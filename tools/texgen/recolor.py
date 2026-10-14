"""原版造型换色（T6 设计文档 §3.2）。

不复制原版贴图：只读取原版贴图里用到的几种颜色，按亮度排成「调色板键」，再配上我们自己的色阶，
写出两张 N×1 的调色板 PNG，并在 assets/minecraft/atlases/blocks.json 中登记一个
paletted_permutations 图集来源。游戏在加载资源时用玩家本机的原版贴图实时换色。

生成的贴图名（模型里引用的名字）是 `<原版贴图>_mythic_age_<id>`，
例如 `minecraft:item/iron_ingot_mythic_age_tin_ingot`；Java 侧用 `Recolor.sprite(base, id)` 得到同样的名字。

规格格式（tools/texgen/specs/**.json）：
  {
    "template": "recolor",
    "id": "tin_ingot",                       换色结果的名字（全局唯一，小写字母、数字、下划线）
    "base": "minecraft:item/iron_ingot",     原版贴图
    "keep": "minecraft:block/stone",         可选：这张贴图里出现的颜色保持不变（用于矿石保留石头底）
    "ramp": ["#4f5762", "#76808c", "#dfe5ea"] 由暗到亮的锚点颜色，按需要插值成 N 种
  }
"""
from __future__ import annotations

import io
import json
import os
import re
import zipfile
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ATLAS_REL = Path("assets/minecraft/atlases/blocks.json")
PALETTE_DIR = "palettes"
PREFIX = "mythic_age_"


class RecolorError(Exception):
    pass


@dataclass
class RecolorSpec:
    path: Path
    id: str
    base: str
    keep: str | None
    ramp: list[tuple[int, int, int]]

    @property
    def permutation(self) -> str:
        return PREFIX + self.id

    @property
    def sprite(self) -> str:
        return f"{self.base}_{self.permutation}"

    @property
    def key_name(self) -> str:
        """调色板键的文件名：同一张原版贴图（以及相同的 keep）共用一个键。"""
        name = flat(self.base)
        if self.keep:
            name += "__keep_" + flat(self.keep)
        return name


def flat(rl: str) -> str:
    return rl.replace(":", "_").replace("/", "_")


def parse_hex(text: str, where: str) -> tuple[int, int, int]:
    if not isinstance(text, str) or not re.fullmatch(r"#[0-9a-fA-F]{6}", text):
        raise RecolorError(f"{where}：颜色应为 #RRGGBB，实际 {text!r}")
    return tuple(int(text[i:i + 2], 16) for i in (1, 3, 5))


def load_recolor_spec(path: Path, data: dict) -> RecolorSpec:
    for key in ("id", "base", "ramp"):
        if key not in data:
            raise RecolorError(f"{path}：缺少字段 {key}")
    rid = data["id"]
    if not isinstance(rid, str) or not re.fullmatch(r"[a-z0-9_]+", rid):
        raise RecolorError(f"{path}：id 只能包含小写字母、数字和下划线，实际 {rid!r}")
    for key in ("base", "keep"):
        v = data.get(key)
        if v is not None and (not isinstance(v, str) or not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", v)):
            raise RecolorError(f"{path}：{key} 应为 命名空间:路径，实际 {v!r}")
    ramp = data["ramp"]
    if not isinstance(ramp, list) or len(ramp) < 2:
        raise RecolorError(f"{path}：ramp 至少需要 2 个锚点颜色")
    return RecolorSpec(path, rid, data["base"], data.get("keep"),
                       [parse_hex(c, f"{path} ramp[{i}]") for i, c in enumerate(ramp)])


class VanillaAssets:
    """读取原版贴图：优先用环境变量 MYTHICAGE_VANILLA_DIR 指定的目录（测试用），否则用 MDG 下载的资源 jar。"""

    def __init__(self, directory: Path | None = None):
        env = os.environ.get("MYTHICAGE_VANILLA_DIR")
        self.directory = directory or (Path(env) if env else None)
        self._zip = None

    def _jar(self) -> zipfile.ZipFile:
        if self._zip is None:
            jars = sorted((ROOT / "neoforge/build/moddev/artifacts").glob("*client-extra*.jar"))
            if not jars:
                raise RecolorError("找不到原版资源 jar；先运行一次 ./gradlew :neoforge:compileJava 让 MDG 下载")
            self._zip = zipfile.ZipFile(jars[-1])
        return self._zip

    def texture(self, rl: str) -> Image.Image:
        ns, path = rl.split(":", 1)
        rel = f"assets/{ns}/textures/{path}.png"
        if self.directory is not None:
            f = self.directory / rel
            if not f.exists():
                raise RecolorError(f"原版贴图不存在：{rl}（{f}）")
            return Image.open(f).convert("RGBA")
        try:
            data = self._jar().read(rel)
        except KeyError as e:
            raise RecolorError(f"原版贴图不存在：{rl}") from e
        return Image.open(io.BytesIO(data)).convert("RGBA")


def luminance(c) -> float:
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def opaque_colors(img: Image.Image) -> set[tuple[int, int, int]]:
    flat_data = getattr(img, "get_flattened_data", None)
    px = flat_data() if flat_data else img.getdata()
    return {p[:3] for p in px if p[3] > 0}


def key_colors(spec: RecolorSpec, vanilla: VanillaAssets) -> list[tuple[int, int, int]]:
    """原版贴图中要换色的颜色，按亮度由暗到亮排序（亮度相同时按 RGB 排，保证确定性）。"""
    colors = opaque_colors(vanilla.texture(spec.base))
    if spec.keep:
        colors -= opaque_colors(vanilla.texture(spec.keep))
    if not colors:
        raise RecolorError(f"{spec.path}：{spec.base} 去掉 keep 之后没有可换的颜色")
    return sorted(colors, key=lambda c: (luminance(c), c))


def interpolate(anchors: list[tuple[int, int, int]], n: int) -> list[tuple[int, int, int]]:
    if n == 1:
        return [anchors[-1]]
    out = []
    for i in range(n):
        t = i / (n - 1) * (len(anchors) - 1)
        j = min(int(t), len(anchors) - 2)
        f = t - j
        out.append(tuple(round(anchors[j][k] * (1 - f) + anchors[j + 1][k] * f) for k in range(3)))
    return out


def strip(colors) -> Image.Image:
    img = Image.new("RGBA", (len(colors), 1))
    img.putdata([c + (255,) for c in colors])
    return img


@dataclass
class RecolorOutput:
    keys: dict[str, Image.Image]          # 键名 → 调色板键
    palettes: dict[str, Image.Image]      # id → 我们的色阶
    atlas: dict                           # blocks.json 内容
    previews: dict[str, Image.Image]      # sprite → 换色后的效果（本地预览用，不写入资源）


def build(specs: list[RecolorSpec], vanilla: VanillaAssets) -> RecolorOutput:
    ids = {}
    for s in specs:
        if s.id in ids:
            raise RecolorError(f"换色 id 重复：{s.id}（{ids[s.id]} 与 {s.path}）")
        ids[s.id] = s.path
    keys: dict[str, Image.Image] = {}
    palettes: dict[str, Image.Image] = {}
    previews: dict[str, Image.Image] = {}
    groups: dict[tuple[str, str], dict[str, str]] = {}
    for s in sorted(specs, key=lambda x: x.id):
        kc = key_colors(s, vanilla)
        target = interpolate(s.ramp, len(kc))
        keys[s.key_name] = strip(kc)
        palettes[s.id] = strip(target)
        groups.setdefault((s.base, s.key_name), {})[s.permutation] = f"mythic_age:{PALETTE_DIR}/{s.id}"
        previews[s.sprite] = apply(vanilla.texture(s.base), dict(zip(kc, target)))
    sources = [{
        "type": "paletted_permutations",
        "textures": [base],
        "palette_key": f"mythic_age:{PALETTE_DIR}/key/{key}",
        "permutations": dict(sorted(perms.items())),
    } for (base, key), perms in sorted(groups.items())]
    return RecolorOutput(keys, palettes, {"sources": sources}, previews)


def apply(img: Image.Image, mapping: dict) -> Image.Image:
    """与游戏中 paletted_permutations 相同的换色规则：命中的颜色替换，其余不变，透明度保留。"""
    flat_data = getattr(img, "get_flattened_data", None)
    px = list(flat_data() if flat_data else img.getdata())
    out = Image.new("RGBA", img.size)
    out.putdata([p if p[3] == 0 or p[:3] not in mapping else mapping[p[:3]] + (p[3],) for p in px])
    return out


def atlas_text(atlas: dict) -> str:
    return json.dumps(atlas, indent=2, ensure_ascii=False) + "\n"
