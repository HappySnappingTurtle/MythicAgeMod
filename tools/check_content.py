#!/usr/bin/env python3
"""Mythic Age 内容一致性校验（T6 设计文档 §1.1、测试用例文档 §2）。

检查项：
  1. 已实现条目的配方与 08 号设计 json（recipe_contract_examples）一致：类型、输入、数量、产物、时代解锁；
  2. 模型引用的本模组贴图都存在；
  3. zh_cn / en_us / ja_jp 三个语言文件的键完全一致，且每个物品模型都有名称；
  4. 物品与方块贴图是 16×16（带 .mcmeta 的动画贴图允许高度为宽度的整数倍；神力展示方块 block/power/ 允许 32×32；
     实体、主神像、神力表现、粒子、药水效果图标另行规定）；
  5. 原版造型换色：模型引用的换色贴图都在 assets/minecraft/atlases/blocks.json 中登记；
     调色板键与色阶长度一致；换色结果与原版底图、以及同一底图的其他换色之间色差（CIE76 ΔE）≥ 15；
  6. 村庄施工建筑（village_building 里 generated = false）：用到的方块都被文明折算表（village_civ 的 buildCosts）
     或本建筑的 buildCosts 覆盖，折算出的物品都已实现，模板文件存在（T19 设计文档 §1.4 E1）。

用法：
  python3 tools/check_content.py [--root 仓库根目录] [--design 设计 json 路径]
设计 json 不存在时（例如在别的机器上）跳过第 1 项的对比，其余照常。退出码：0 通过，1 有错误。
"""
from __future__ import annotations

import argparse
import json
import re
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
    "#c:dyes/black": "minecraft:black_dye",
    "#c:nuggets/gold": "minecraft:gold_nugget",
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
    "greek_heroic": ("greek", "heroic"),
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
        if rel.parts[0] in ("palettes", "entity", "gui", "statue", "fx", "particle", "mob_effect", "models"):
            continue  # 调色板是 N×1；实体、主神像（GeckoLib 模型）、界面与护甲层（models/armor，64×32）的尺寸由模型 UV / 界面布局决定
        with Image.open(f) as img:
            w, h = img.size
        if rel.parts[:2] in (("block", "power"), ("block", "village")):
            # 神力展示方块（诱兽石等，2026-10-19）与村庄展示方块（陶瓮、瓶堆等，T13b）：展示物件，允许每格 32×32 的细节密度
            if not (w in (16, 32) and h == w):
                report.error(f"贴图 {f.relative_to(paths.root)} 尺寸为 {w}×{h}，神力展示方块应为 16×16 或 32×32")
            continue
        animated = Path(str(f) + ".mcmeta").exists()
        ok = w == 16 and (h == w or (animated and h % w == 0))
        if not ok:
            report.error(f"贴图 {f.relative_to(paths.root)} 尺寸为 {w}×{h}，物品与方块应为 16×16")


# ---------------------------------------------------------------- 圣物卡（T16）

CIVS = ("greek", "egyptian", "norse", "chinese", "atlantean")
RELIC_SLOT_TAG = "data/curios/tags/item/mythic_relic.json"


def check_relics(paths: Paths, report: Report) -> None:
    """圣物卡 relic/<文明>/<id>.json：目录是五文明之一且与 civ 一致；物品已实现、带 curios:mythic_relic 标签、
    三语都有佩戴与供奉说明（item.<物品>.worn / .offered）；两张卡不能指向同一个物品。效果组由游戏加载时校验。"""
    tagged = set()
    for d in paths.data():
        f = d.parent / RELIC_SLOT_TAG
        if f.exists():
            tagged.update(v if isinstance(v, str) else v.get("id") for v in json.loads(f.read_text(encoding="utf-8"))["values"])
    langs = {}
    for loc in LOCALES:
        merged = {}
        for a in paths.assets():
            f = a / "lang" / f"{loc}.json"
            if f.exists():
                merged.update(json.loads(f.read_text(encoding="utf-8")))
        langs[loc] = merged
    items = implemented_items(paths)
    owners: dict[str, str] = {}
    for d in paths.data():
        root = d / MOD / MOD / "relic"
        if not root.exists():
            continue
        for f in sorted(root.glob("*/*.json")):
            card = f"{f.parent.name}:{f.stem}"
            data = json.loads(f.read_text(encoding="utf-8"))
            if f.parent.name not in CIVS:
                report.error(f"圣物卡 {card}：目录 {f.parent.name} 不是五文明之一")
            if data.get("civ") != f.parent.name:
                report.error(f"圣物卡 {card}：civ「{data.get('civ')}」与目录不一致")
            item = data.get("item", "")
            if item not in items:
                report.error(f"圣物卡 {card}：物品 {item} 未实现")
            if item not in tagged:
                report.error(f"圣物卡 {card}：物品 {item} 没有 curios:mythic_relic 标签")
            if item in owners:
                report.error(f"圣物卡 {card}：物品 {item} 已被 {owners[item]} 使用")
            owners.setdefault(item, card)
            name = item.split(":")[-1]
            for loc in LOCALES:
                for mode in ("worn", "offered"):
                    if f"item.{MOD}.{name}.{mode}" not in langs[loc]:
                        report.error(f"圣物卡 {card}：{loc} 缺少 item.{MOD}.{name}.{mode}")


def glob_match(pattern: str, block: str) -> bool:
    """与 core BuildCosts.matches 相同：只支持 *。"""
    import fnmatch
    return fnmatch.fnmatchcase(block, pattern.replace("[", "[[]"))


def check_village_buildings(paths: Paths, report: Report) -> None:
    items = implemented_items(paths)
    for d in paths.data():
        civ_root = d / MOD / MOD / "village_civ"
        bld_root = d / MOD / MOD / "village_building"
        if not bld_root.exists():
            continue
        for civ_dir in sorted(p for p in bld_root.iterdir() if p.is_dir()):
            civ_file = civ_root / f"{civ_dir.name}.json"
            costs = json.loads(civ_file.read_text(encoding="utf-8")).get("buildCosts", {}) if civ_file.exists() else {}
            groups = costs.get("groups", {})
            for key, members in groups.items():
                for it in members:
                    if it.startswith(MOD + ":") and it not in items:
                        report.error(f"折算表 {civ_dir.name} 组 #{key}：物品 {it} 未实现")
            # 奠基村庄的起步住居（T19 §1.13 K7）是生成建筑，但也由居民施工，一样要被折算表覆盖
            civ_data = json.loads(civ_file.read_text(encoding="utf-8")) if civ_file.exists() else {}
            starter = civ_data.get("foundation", {}).get("starter", "")
            for f in sorted(civ_dir.glob("*.json")):
                data = json.loads(f.read_text(encoding="utf-8"))
                if data.get("generated", False) and data.get("id") != starter:
                    continue
                rules = data.get("buildCosts", []) + costs.get("rules", [])
                tpl = data["template"].split(":", 1)
                if not (d / tpl[0] / "structure" / f"{tpl[1]}.nbt").exists():
                    report.error(f"施工建筑 {data['id']}：模板 {data['template']} 不存在")
                for block in data.get("materials", {}):
                    if any(glob_match(p, block) for p in costs.get("free", [])):
                        continue
                    rule = next((r for r in rules if glob_match(r["match"], block)), None)
                    if rule is None:
                        report.error(f"施工建筑 {data['id']}：方块 {block} 没有被折算表覆盖")
                        continue
                    for k in rule["cost"]:
                        if k.startswith("#"):
                            if k[1:] not in groups:
                                report.error(f"施工建筑 {data['id']}：折算用到未定义的物品组 {k}")
                        elif k.startswith(MOD + ":") and k not in items:
                            report.error(f"施工建筑 {data['id']}：折算物品 {k} 未实现")


def run(root: Path, design: Path | None) -> Report:
    paths = Paths(root)
    report = Report()
    check_recipes(paths, design if design else root / DESIGN_REL, report)
    check_models(paths, report)
    check_lang(paths, report)
    check_textures(paths, report)
    check_recolor(paths, report)
    check_relics(paths, report)
    check_village_buildings(paths, report)
    check_village_anims(paths, report)
    check_village_dialogue(paths, report)
    check_equipment(root, report)
    check_rigs(root, report)
    check_warning_fx(root, report)
    check_handbook(paths, report)
    check_untranslated(paths, report)
    return report


CJK = re.compile(r"[\u4e00-\u9fff]")
# 日文与中文同形的词（汉字相同）属于正常情况：只提示含中文专用字形或整句相同的值，交给 codex 核对（T24 §6.5，TC-T24-93）
# 2026-10-28 人工核对：日语本来同形的汉字术语/方向/单位，按键放行而非放宽整类检测。
JA_SAME_OK: set[str] = {
    "block.mythic_age.greek_statue",
    "item.mythic_age.patron_statue",
    "item.mythic_age.patron_statue.of",
    "mythic_age.age.classical",
    "mythic_age.age.heroic",
    "mythic_age.age.none",
    "mythic_age.age_unlocked.powers",
    "mythic_age.altar.row.statues",
    "mythic_age.civ.norse",
    "mythic_age.dir.north",
    "mythic_age.dir.south",
    "mythic_age.dir.west",
    "mythic_age.direction.n",
    "mythic_age.direction.s",
    "mythic_age.direction.w",
    "mythic_age.faith.tab",
    "mythic_age.faith.title",
    "mythic_age.jei.pressing_time",
    "mythic_age.profession.greek.potter",
    "mythic_age.quest.reward.conduct",
    "mythic_age.trade.category.god_power",
}


def check_untranslated(paths: Paths, report: Report) -> None:
    """en_us / ja_jp 的值与 zh_cn 字面相同且含汉字：多半没翻，列成提示。"""
    files = {}
    for lang in ("zh_cn", "en_us", "ja_jp"):
        for a in paths.assets():
            f = a / f"lang/{lang}.json"
            if f.exists():
                files.setdefault(lang, {}).update(json.loads(f.read_text(encoding="utf-8")))
    zh = files.get("zh_cn", {})
    for lang in ("en_us", "ja_jp"):
        same = [k for k, v in files.get(lang, {}).items()
                if zh.get(k) == v and CJK.search(v) and not (lang == "ja_jp" and k in JA_SAME_OK)]
        if same:
            report.notes.append(f"{lang} 有 {len(same)} 条与 zh_cn 相同且含汉字（可能没翻）：" + "、".join(sorted(same)[:8])
                                + ("……" if len(same) > 8 else ""))


# 《神话手札》（T24 §6.5）：三语条目一一对应、页数与锁页一致；引用的物品、实体、进度都存在。
HANDBOOK_REL = "patchouli_books/handbook"
HANDBOOK_LANGS = ("zh_cn", "en_us", "ja_jp")
EXTERNAL_NS = ("minecraft", "patchouli")


def handbook_files(book: Path, lang: str) -> dict[str, dict]:
    d = book / lang
    if not d.is_dir():
        return {}
    return {str(f.relative_to(d)): json.loads(f.read_text(encoding="utf-8")) for f in sorted(d.rglob("*.json"))}


def item_ids(paths: Paths) -> set[str]:
    """本模组有物品形态的 id：有物品模型的（只有方块、没有物品的不算，Patchouli 图标会失败）。"""
    out: set[str] = set()
    for a in paths.assets():
        d = a / "models/item"
        if d.is_dir():
            out.update(f"{MOD}:{f.stem}" for f in d.glob("*.json"))
    return out


def registered_ids(paths: Paths) -> set[str]:
    """本模组已注册的物品、方块、实体 id：取自 zh_cn 语言文件的名称键。"""
    out: set[str] = set()
    for a in paths.assets():
        f = a / "lang/zh_cn.json"
        if f.exists():
            for k in json.loads(f.read_text(encoding="utf-8")):
                parts = k.split(".")
                if len(parts) == 3 and parts[0] in ("item", "block", "entity") and parts[1] == MOD:
                    out.add(f"{MOD}:{parts[2]}")
    return out


def advancement_ids(paths: Paths) -> set[str]:
    out: set[str] = set()
    for data in paths.data():
        base = data / MOD / "advancement"
        if base.is_dir():
            for f in base.rglob("*.json"):
                out.add(f"{MOD}:" + "/".join(f.relative_to(base).with_suffix("").parts))
    return out


def check_handbook(paths: Paths, report: Report) -> None:
    book = paths.main / "assets" / MOD / HANDBOOK_REL
    if not book.is_dir():
        return
    langs = {lang: handbook_files(book, lang) for lang in HANDBOOK_LANGS}
    base = langs[HANDBOOK_LANGS[0]]
    ids = registered_ids(paths)
    advs = advancement_ids(paths)

    items = item_ids(paths)

    def known(ref: str, need_item: bool = True) -> bool:
        ref = ref.split("{")[0].split("#")[0]
        ns = ref.split(":")[0] if ":" in ref else "minecraft"
        return ns in EXTERNAL_NS or (ref in items if need_item else ref in ids)

    for lang, files in langs.items():
        for name in sorted(set(base) ^ set(files)):
            report.errors.append(f"手札：{lang} 与 {HANDBOOK_LANGS[0]} 的文件不一致：{name}")
        for name, o in files.items():
            where = f"手札 {lang}/{name}"
            for key in ("icon",):
                if key in o and not known(o[key]):
                    report.errors.append(f"{where}：图标 {o[key]} 不存在")
            if "advancement" in o and o["advancement"] not in advs:
                report.errors.append(f"{where}：锁条目的进度 {o['advancement']} 不存在")
            for i, page in enumerate(o.get("pages", [])):
                for key in ("item", "entity"):
                    if key in page and not known(page[key], need_item=key == "item"):
                        report.errors.append(f"{where} 第 {i + 1} 页：{key} {page[key]} 不存在")
                if "advancement" in page and page["advancement"] not in advs:
                    report.errors.append(f"{where} 第 {i + 1} 页：锁页的进度 {page['advancement']} 不存在")
                # 手札文字经 I18n.get 走 String.format：单个 % 会让整页变成「Format error」，要写成 %%
                if "%" in page.get("text", "").replace("%%", ""):
                    report.errors.append(f"{where} 第 {i + 1} 页：单个 % 要写成 %%（否则整页显示 Format error）")
            ref = base.get(name)
            if ref is not None and lang != HANDBOOK_LANGS[0]:
                if len(ref.get("pages", [])) != len(o.get("pages", [])):
                    report.errors.append(f"{where}：页数与 {HANDBOOK_LANGS[0]} 不同")
                elif [p.get("advancement") for p in ref.get("pages", [])] != [p.get("advancement") for p in o.get("pages", [])]:
                    report.errors.append(f"{where}：锁页与 {HANDBOOK_LANGS[0]} 不同")
                if ref.get("advancement") != o.get("advancement"):
                    report.errors.append(f"{where}：锁条目与 {HANDBOOK_LANGS[0]} 不同")


# 战斗预警所在的代码（T24 §7.1）：这些文件发出的预警粒子、预警声不能被「装饰粒子」客户端选项减掉，
# 所以不许引用 ClientFx（装饰特效开关）。新增 Boss / 竞技场时把文件加进来。
WARNING_FX_DIRS = [
    "neoforge/src/main/java/io/github/happysnappingturtle/mythicage/boss",
    "neoforge/src/main/java/io/github/happysnappingturtle/mythicage/content/greek/knossos",
]


def check_warning_fx(root: Path, report: Report) -> None:
    for rel in WARNING_FX_DIRS:
        d = root / rel
        if not d.is_dir():
            continue
        for f in sorted(d.rglob("*.java")):
            if "ClientFx" in f.read_text(encoding="utf-8"):
                report.errors.append(f"战斗预警代码引用了装饰特效开关 ClientFx：{f.relative_to(root)}")


def check_village_dialogue(paths: Paths, report: Report) -> None:
    """有缺料条件（shortWhen）的居民职业，三语都要有「本职缺料」与「委托」两句（T21 阶段 ④：缺料时有对应委托就说「委托」，
    没有就说「本职缺料」；少了键，游戏里只显示键名）。键：mythic_age.village.say.<文明>.<职业>.short / .quest。"""
    langs = {}
    for loc in LOCALES:
        merged = {}
        for a in paths.assets():
            f = a / "lang" / f"{loc}.json"
            if f.exists():
                merged.update(json.loads(f.read_text(encoding="utf-8")))
        langs[loc] = merged
    for d in paths.data():
        for f in sorted(d.glob(f"{MOD}/{MOD}/village_profession/*/*.json")):
            try:
                prof = json.loads(f.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                continue
            w = prof.get("shortWhen") or {}
            if not (w.get("items") or w.get("food") or w.get("point")):
                continue
            civ, name = f.parent.name, f.stem
            for suffix in ("short", "quest"):
                key = f"{MOD}.village.say.{civ}.{name}.{suffix}"
                for loc in LOCALES:
                    if key not in langs[loc]:
                        report.error(f"{f.relative_to(d)}：缺 {loc} 的对话 {key}")


def check_village_anims(paths: Paths, report: Report) -> None:
    """村庄数据里写的居民动作名（配方工序、采集点、兵种动作表）都要在居民动画文件里（T20：配方写了不存在的动作，
    游戏里只会悄悄退成通用动作，看不出来）。"""
    anim_file = paths.main / "assets" / MOD / "animations/entity/resident.animation.json"
    if not anim_file.exists():
        report.note("没有居民动画文件，跳过动作名校验")
        return
    have = set(json.loads(anim_file.read_text(encoding="utf-8")).get("animations", {}))
    refs: list[tuple[str, str]] = []

    def walk(node, where: str):
        if isinstance(node, dict):
            for k, v in node.items():
                if k == "anim" and isinstance(v, str) and v:
                    refs.append((v, where))
                elif k == "anims" and isinstance(v, dict):
                    refs.extend((x, where) for x in v.values() if isinstance(x, str) and x)
                else:
                    walk(v, where)
        elif isinstance(node, list):
            for v in node:
                walk(v, where)

    for d in paths.data():
        for f in sorted(d.glob(f"{MOD}/{MOD}/village_*/**/*.json")):
            try:
                walk(json.loads(f.read_text(encoding="utf-8")), str(f.relative_to(d)))
            except json.JSONDecodeError:
                continue
    for name, where in refs:
        if name not in have:
            report.error(f"{where}：居民动作 {name} 不在 resident.animation.json 里")


def check_equipment(root: Path, report: Report) -> None:
    """装备数值表与 10 号合同 json（T17），见 tools/check_equipment.py。"""
    import check_equipment as ce  # noqa: E402
    if not (root / ce.CONTRACT).exists():
        report.note("没有 10 号合同 json，跳过装备数值校验")
        return
    errors, notes = ce.check(root)
    for e in errors:
        report.error(e)
    for n in notes:
        report.note(n)


def check_rigs(root: Path, report: Report) -> None:
    """实体资源（T7）：骨骼、定位点、UV、动画与导出哈希，见 tools/blockbench/check_rig.py。"""
    sys.path.insert(0, str(Path(__file__).resolve().parent / "blockbench"))
    import check_rig  # noqa: E402
    if not (root / check_rig.RIGS[0]["params"]).exists():
        report.note("没有实体参数文件，跳过实体资源校验")
        return
    for e in check_rig.run(root):
        report.error(e)


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
