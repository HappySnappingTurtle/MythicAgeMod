#!/usr/bin/env python3
"""Mythic Age 内容一致性校验（T6 设计文档 §1.1、测试用例文档 §2）。

检查项：
  1. 已实现条目的配方与 08 号设计 json（recipe_contract_examples）一致：类型、输入、数量、产物、时代解锁；
  2. 模型引用的本模组贴图都存在；
  3. zh_cn / en_us / ja_jp 三个语言文件的键完全一致，且每个物品模型都有名称；
  4. 物品与方块贴图是 16×16（带 .mcmeta 的动画贴图允许高度为宽度的整数倍；实体贴图另行规定）；
  5. 原版造型换色：模型引用的换色贴图都在 assets/minecraft/atlases/blocks.json 中登记；
     调色板键与色阶长度一致；换色结果与原版底图、以及同一底图的其他换色之间色差（CIE76 ΔE）≥ 15。

用法：
  python3 tools/check_content.py [--root 仓库根目录] [--design 设计 json 路径]
设计 json 不存在时（例如在别的机器上）跳过第 1 项的对比，其余照常。退出码：0 通过，1 有错误。
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from PIL import Image

MOD = "mythic_age"
LOCALES = ("zh_cn", "en_us", "ja_jp")
DEFAULT_ROOT = Path(__file__).resolve().parents[1]
DESIGN_REL = Path("docs/重构/设计/跨文明详细设计/数据/材料物产与配方提案.json")

# 配方中使用的通用标签与设计 json 中原版物品的对应关系（设计写物品，实现用标签以便与其他模组互通）。
TAG_ALIASES = {
    "#c:ingots/copper": "minecraft:copper_ingot",
    "#c:ingots/iron": "minecraft:iron_ingot",
    "#c:ingots/gold": "minecraft:gold_ingot",
    "#c:rods/wooden": "minecraft:stick",
    "#c:strings": "minecraft:string",
    "#c:gems/amethyst": "minecraft:amethyst_shard",
    "#c:dusts/glowstone": "minecraft:glowstone_dust",
    "#c:dyes/red": "minecraft:red_dye",
    "#c:cobblestones": "minecraft:cobblestone",
    "#c:foods/cooked_fish": "design:cooked_fish",
}
# 设计 json 中的「设计别名」输入
WATER_BOTTLE = "design:water_bottle"
# 设计中的配方类型 → 实现中的配方类型
TYPE_MAP = {
    "crafting": {"minecraft:crafting_shaped", "minecraft:crafting_shapeless"},
    "smelting": {"minecraft:smelting"},
    "blasting": {"minecraft:blasting"},
    "stonecutting": {"minecraft:stonecutting"},
}
# 设计中的 unlock 值 → (文明, 时代)；archaic 表示「远古即可，不加限制」
UNLOCKS = {
    "greek_classical": ("greek", "classical"),
    "norse_classical": ("norse", "classical"),
    "archaic": None,
}


class Report:
    def __init__(self):
        self.errors: list[str] = []
        self.notes: list[str] = []

    def error(self, msg: str):
        self.errors.append(msg)

    def note(self, msg: str):
        self.notes.append(msg)


class Paths:
    def __init__(self, root: Path):
        self.root = root
        self.gen = root / "neoforge/src/generated/resources"
        self.main = root / "neoforge/src/main/resources"

    def assets(self):
        return [p / "assets" / MOD for p in (self.gen, self.main)]

    def data(self):
        return [p / "data" for p in (self.gen, self.main)]


# ---------------------------------------------------------------- 标签与配方

def load_item_tags(paths: Paths) -> dict[str, set[str]]:
    """读取实现中的物品标签（只解析本仓库生成或手写的标签文件）。"""
    tags: dict[str, set[str]] = {}
    for data in paths.data():
        if not data.exists():
            continue
        for f in data.glob("*/tags/item/**/*.json"):
            ns = f.relative_to(data).parts[0]
            tag_path = "/".join(f.relative_to(data / ns / "tags" / "item").with_suffix("").parts)
            values = json.loads(f.read_text(encoding="utf-8")).get("values", [])
            tags.setdefault(f"#{ns}:{tag_path}", set()).update(v if isinstance(v, str) else v["id"] for v in values)
    return tags


def ingredient_key(ing, tags: dict[str, set[str]]) -> str:
    if isinstance(ing, list):
        keys = sorted(ingredient_key(i, tags) for i in ing)
        return keys[0] if len(keys) == 1 else "|".join(keys)
    if "item" in ing:
        return ing["item"]
    if "tag" in ing:
        tag = "#" + ing["tag"]
        if tag in TAG_ALIASES:
            return TAG_ALIASES[tag]
        if tag.startswith("#minecraft:"):
            return tag  # 原版标签在原版数据里还有其他成员，本仓库只看得到我们追加的部分，不能折叠
        members = tags.get(tag, set())
        own = sorted(m for m in members if m.startswith(MOD + ":"))
        return own[0] if len(own) == 1 else tag
    if ing.get("type") == "neoforge:components":
        contents = ing.get("components", {}).get("minecraft:potion_contents")
        potion = contents.get("potion") if isinstance(contents, dict) else contents
        if ing.get("items") == "minecraft:potion" and potion == "minecraft:water":
            return WATER_BOTTLE
    return json.dumps(ing, sort_keys=True)


def normalize_recipe(data: dict, tags) -> dict | None:
    unlock = None
    if data.get("type") == f"{MOD}:age_gated":
        unlock = (data["civilization"], data["age"])
        data = data["recipe"]
    rtype = data.get("type")
    inputs: dict[str, int] = {}

    def add(key, n=1):
        inputs[key] = inputs.get(key, 0) + n

    if rtype == "minecraft:crafting_shapeless":
        for ing in data["ingredients"]:
            add(ingredient_key(ing, tags))
    elif rtype == "minecraft:crafting_shaped":
        counts: dict[str, int] = {}
        for row in data["pattern"]:
            for ch in row:
                if ch != " ":
                    counts[ch] = counts.get(ch, 0) + 1
        for ch, n in counts.items():
            add(ingredient_key(data["key"][ch], tags), n)
    elif rtype == f"{MOD}:pressing":
        add(ingredient_key(data["ingredient"], tags), data["count"])
        add(ingredient_key(data["container"], tags))
    elif rtype in ("minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking",
                   "minecraft:stonecutting"):
        add(ingredient_key(data["ingredient"], tags))
    else:
        return None
    result = data["result"]
    out = {result["id"]: result.get("count", 1)}
    return {"type": rtype, "inputs": inputs, "outputs": out, "unlock": unlock}


def load_recipes(paths: Paths, tags) -> dict[str, dict]:
    recipes = {}
    for data in paths.data():
        folder = data / MOD / "recipe"
        if folder.exists():
            for f in sorted(folder.rglob("*.json")):
                norm = normalize_recipe(json.loads(f.read_text(encoding="utf-8")), tags)
                if norm is not None:
                    recipes[f.stem] = norm
    return recipes


def implemented_items(paths: Paths) -> set[str]:
    items = set()
    for assets in paths.assets():
        folder = assets / "models" / "item"
        if folder.exists():
            items.update(f"{MOD}:{f.stem}" for f in folder.glob("*.json"))
    return items


def check_recipes(paths: Paths, design_file: Path, report: Report) -> None:
    if not design_file.exists():
        report.note(f"未找到设计 json {design_file}，跳过配方对比")
        return
    design = json.loads(design_file.read_text(encoding="utf-8"))
    tags = load_item_tags(paths)
    recipes = load_recipes(paths, tags)
    items = implemented_items(paths)
    pending = []
    for d in design.get("recipe_contract_examples", []):
        outs = d.get("outputs", {})
        own_inputs = [i for i in d.get("inputs", {}) if i.startswith(MOD + ":")]
        if not outs or not all(o in items for o in outs) or not all(i in items for i in own_inputs):
            pending.append(d["id"])
            continue
        types = TYPE_MAP.get(d["type"], {d["type"]})
        want_unlock = UNLOCKS.get(d.get("unlock")) if d.get("unlock") in UNLOCKS else None
        candidates = [(rid, r) for rid, r in recipes.items() if r["type"] in types and set(r["outputs"]) == set(outs)]
        if not candidates:
            report.error(f"配方 {d['id']}：没有找到产物为 {outs} 的 {d['type']} 配方")
            continue
        ok = [rid for rid, r in candidates
              if r["inputs"] == d["inputs"] and r["outputs"] == outs and r["unlock"] == want_unlock]
        if not ok:
            got = "; ".join(f"{rid}: 输入 {r['inputs']} 产出 {r['outputs']} 解锁 {r['unlock']}" for rid, r in candidates)
            report.error(f"配方 {d['id']} 与设计不一致：设计 输入 {d['inputs']} 产出 {outs} 解锁 {want_unlock}；实现 {got}")
    if pending:
        report.note(f"设计中尚未实现的配方 {len(pending)} 条（不算失败）")


# ---------------------------------------------------------------- 模型、语言、贴图

RECOLOR_MARK = "_mythic_age_"
# 平均色差阈值：实测锡（蓝灰白镴色）与原版铁锭的 ΔE 约 16 时，两者并排已一眼可辨；
# 低于 15 时开始容易混淆（2026-10-14 预览对比）。
MIN_DELTA_E = 15.0


def load_atlas(paths: Paths) -> dict:
    """合并本模组提供的全部图集文件（blocks、signs）。"""
    sources = []
    folder = paths.main / "assets/minecraft/atlases"
    if folder.exists():
        for f in sorted(folder.glob("*.json")):
            sources += json.loads(f.read_text(encoding="utf-8")).get("sources", [])
    return {"sources": sources}


def recolor_sprites(atlas: dict) -> set[str]:
    out = set()
    for src in atlas.get("sources", []):
        if src.get("type") == "paletted_permutations":
            for base in src["textures"]:
                for perm in src["permutations"]:
                    out.add(f"{base}_{perm}")
    return out


def palette_file(paths: Paths, rl: str) -> Path:
    ns, path = rl.split(":", 1)
    return paths.main / "assets" / ns / "textures" / (path + ".png")


def read_strip(f: Path) -> list[tuple[int, int, int]]:
    with Image.open(f) as img:
        img = img.convert("RGBA")
        flat = getattr(img, "get_flattened_data", None)
        return [p[:3] for p in (flat() if flat else img.getdata())]


def to_lab(c):
    def lin(v):
        v /= 255
        return v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4
    r, g, b = (lin(v) for v in c)
    x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
    y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883

    def f(t):
        return t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    fx, fy, fz = f(x), f(y), f(z)
    return 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)


def delta_e(a: list, b: list) -> float:
    """两条等长色阶逐项色差的平均值（CIE76）。"""
    total = 0.0
    for c1, c2 in zip(a, b):
        l1, l2 = to_lab(c1), to_lab(c2)
        total += sum((l1[i] - l2[i]) ** 2 for i in range(3)) ** 0.5
    return total / max(1, len(a))


def check_recolor(paths: Paths, report: Report) -> None:
    atlas = load_atlas(paths)
    for src in atlas.get("sources", []):
        if src.get("type") != "paletted_permutations":
            continue
        key_f = palette_file(paths, src["palette_key"])
        if not key_f.exists():
            report.error(f"调色板键不存在：{src['palette_key']}")
            continue
        key = read_strip(key_f)
        perms = {}
        for name, rl in src["permutations"].items():
            f = palette_file(paths, rl)
            if not f.exists():
                report.error(f"换色色阶不存在：{rl}")
                continue
            colors = read_strip(f)
            if len(colors) != len(key):
                report.error(f"换色 {name} 的色阶长度 {len(colors)} 与调色板键 {len(key)} 不一致")
                continue
            perms[name] = colors
            d = delta_e(key, colors)
            if d < MIN_DELTA_E:
                report.error(f"换色 {name} 与原版底图 {src['textures'][0]} 太接近（ΔE {d:.1f} < {MIN_DELTA_E}）")
        names = sorted(perms)
        for i in range(len(names)):
            for j in range(i + 1, len(names)):
                d = delta_e(perms[names[i]], perms[names[j]])
                if d < MIN_DELTA_E:
                    report.error(f"换色 {names[i]} 与 {names[j]} 太接近（ΔE {d:.1f} < {MIN_DELTA_E}）")


def texture_file(paths: Paths, ref: str) -> Path | None:
    ns, _, path = ref.partition(":") if ":" in ref else ("minecraft", "", ref)
    if ns != MOD:
        return None
    return paths.main / "assets" / MOD / "textures" / (path + ".png")


def check_models(paths: Paths, report: Report) -> None:
    sprites = recolor_sprites(load_atlas(paths))
    for assets in paths.assets():
        folder = assets / "models"
        if not folder.exists():
            continue
        for f in sorted(folder.rglob("*.json")):
            textures = json.loads(f.read_text(encoding="utf-8")).get("textures", {})
            for key, ref in textures.items():
                if not isinstance(ref, str) or ref.startswith("#"):
                    continue
                if RECOLOR_MARK in ref:
                    if ref not in sprites:
                        report.error(f"模型 {f.relative_to(paths.root)} 引用的换色贴图 {ref} 没有在图集中登记")
                    continue
                tf = texture_file(paths, ref)
                if tf is not None and not tf.exists():
                    report.error(f"模型 {f.relative_to(paths.root)} 的贴图 {key}={ref} 不存在（{tf.relative_to(paths.root)}）")


def check_lang(paths: Paths, report: Report) -> None:
    langs = {}
    for loc in LOCALES:
        files = [a / "lang" / f"{loc}.json" for a in paths.assets() if (a / "lang" / f"{loc}.json").exists()]
        if not files:
            report.error(f"缺少语言文件 {loc}.json")
            langs[loc] = {}
            continue
        merged = {}
        for f in files:
            merged.update(json.loads(f.read_text(encoding="utf-8")))
        langs[loc] = merged
    base = set(langs.get("en_us", {}))
    for loc in LOCALES:
        missing = base - set(langs[loc])
        extra = set(langs[loc]) - base
        for k in sorted(missing):
            report.error(f"{loc} 缺少键 {k}")
        for k in sorted(extra):
            report.error(f"{loc} 多出键 {k}（en_us 中没有）")
        for k, v in langs[loc].items():
            if not str(v).strip():
                report.error(f"{loc} 的 {k} 为空")
    for item in sorted(implemented_items(paths)):
        name = item.split(":")[1]
        keys = (f"item.{MOD}.{name}", f"block.{MOD}.{name}")
        for loc in LOCALES:
            if not any(k in langs[loc] for k in keys):
                report.error(f"{item} 在 {loc} 中没有名称")


def check_textures(paths: Paths, report: Report) -> None:
    folder = paths.main / "assets" / MOD / "textures"
    if not folder.exists():
        return
    for f in sorted(folder.rglob("*.png")):
        rel = f.relative_to(folder)
        if rel.parts[0] in ("palettes", "entity", "gui"):
            continue  # 调色板是 N×1；实体与界面贴图的尺寸由模型 UV / 界面布局决定
        with Image.open(f) as img:
            w, h = img.size
        animated = Path(str(f) + ".mcmeta").exists()
        ok = w == 16 and (h == w or (animated and h % w == 0))
        if not ok:
            report.error(f"贴图 {f.relative_to(paths.root)} 尺寸为 {w}×{h}，物品与方块应为 16×16")


def run(root: Path, design: Path | None) -> Report:
    paths = Paths(root)
    report = Report()
    check_recipes(paths, design if design else root / DESIGN_REL, report)
    check_models(paths, report)
    check_lang(paths, report)
    check_textures(paths, report)
    check_recolor(paths, report)
    return report


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description="Mythic Age 内容一致性校验")
    ap.add_argument("--root", type=Path, default=DEFAULT_ROOT)
    ap.add_argument("--design", type=Path)
    a = ap.parse_args(argv)
    report = run(a.root, a.design)
    for n in report.notes:
        print(f"提示：{n}")
    for e in report.errors:
        print(f"错误：{e}")
    print(f"共 {len(report.errors)} 个错误" if report.errors else "全部通过")
    return 1 if report.errors else 0


if __name__ == "__main__":
    sys.exit(main())
