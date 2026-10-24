#!/usr/bin/env python3
"""T17 阶段 ② 希腊武器的手持 3D 模型（短剑、长矛、圆盾、大棒）与物品栏图标。

做法：
- 物品模型 models/item/<名>.json 用 NeoForge 的 separate_transforms：物品栏（gui）显示 16 × 16 平面图标，其余视角显示 3D 模型
  models/item/held/<名>.json（放在子目录，check_content 不把它当物品）。
- 短剑、长矛、大棒先竖着建（刃 / 矛尖 / 棒头朝 +Y，握点在 (8, 2.3, 8)），再把每个元素绕 (8, 8, 8) 转 −45°，
  与原版平面工具贴图的斜放方向、握点一致，于是直接沿用原版 item/handheld 的各视角变换。
- 圆盾按原版盾（实体模型）的坐标系建：盾面中心在原点，正面朝 +Z，臂环在背面；沿用原版 shield / shield_blocking 的视角变换。
- 3D 模型的贴图是 4 × 4 色块表（textures/item/held/held_<名>.png，每块 4 × 4 像素，面取色块内部 2 × 2）。
尺寸按 10 号合同：短剑 22u（刃 14u，最宽处在刃长 2/3）、长矛 32u（握点在杆长 1/3）、圆盾直径 15u、大棒 20u。
运行：python3 tools/models/greek_weapons.py && for n in xiphos dory aspis olive_club; do
      python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/held_$n.json; done
"""
from __future__ import annotations

import json
import math

from modelgen import ROOT, spec

ITEM_MODELS = ROOT / "neoforge/src/main/resources/assets/mythic_age/models/item"
FACES = ("north", "south", "east", "west", "up", "down")


def swatches(name: str, cells: dict[str, str]) -> dict[str, list[float]]:
    keys = list(cells)
    rows = ["".join(keys[min(len(keys) - 1, (py // 4) * 4 + px // 4)] if (py // 4) * 4 + px // 4 < len(keys) else keys[0]
                    for px in range(16)) for py in range(16)]
    spec("held_" + name, rows, cells, 16, "item/held/")
    return {k: [(i % 4) * 4 + 1, (i // 4) * 4 + 1, (i % 4) * 4 + 3, (i // 4) * 4 + 3] for i, k in enumerate(keys)}


def el(frm, to, color, uv, rot=None, comment=None):
    e = {}
    if comment:
        e["__comment"] = comment
    e["from"] = [round(v, 3) for v in frm]
    e["to"] = [round(v, 3) for v in to]
    if rot:
        e["rotation"] = rot
    e["faces"] = {f: {"texture": "#0", "uv": uv[color]} for f in FACES}
    return e


def tilt(elements):
    """竖着建好的元素绕 (8, 8, 8) 转 −45°（刃尖朝右上），对齐原版平面工具的斜放。"""
    for e in elements:
        e["rotation"] = {"angle": -45, "axis": "z", "origin": [8, 8, 8]}
    return elements


def write(path, model):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def item(name: str, comment: str, overrides=None):
    """物品模型：gui 用平面图标，其余用 held/<名>。"""
    m = {"__comment": comment, "loader": "neoforge:separate_transforms",
         "base": {"parent": f"mythic_age:item/held/{name}"},
         "perspectives": {"gui": {"parent": "minecraft:item/generated", "textures": {"layer0": f"mythic_age:item/{name}"}}}}
    if overrides:
        m["overrides"] = overrides
    write(ITEM_MODELS / f"{name}.json", m)


# 原版 item/handheld + item/generated 的视角变换。不能直接 parent 它们：根是 builtin/generated，会忽略 elements。
HANDHELD_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]}}


def held(name: str, elements, parent=None, display=None, extra=None):
    m = {} if parent is None else {"parent": parent}
    m["display"] = dict(HANDHELD_DISPLAY)
    if display:
        m["display"].update(display)
    tex = f"mythic_age:item/held/held_{name.removesuffix('_blocking')}"
    m |= {"textures": {"0": tex, "particle": tex},
         "elements": elements}
    if extra:
        m.update(extra)
    write(ITEM_MODELS / "held" / f"{name}.json", m)


# ───────────────────────────── 希腊短剑 Xiphos ─────────────────────────────

def xiphos():
    uv = swatches("xiphos", {"b": "#b07a36", "B": "#d9a24a", "h": "#f6d68a", "d": "#6e4518",
                             "g": "#2a1e16", "G": "#8a5a24", "p": "#a8742e"})
    e = [el((7.0, -1.5, 7.0), (9.0, -0.5, 9.0), "p", uv, comment="圆盘柄头"),
         el((7.5, -0.5, 7.5), (8.5, 4.0, 8.5), "g", uv, comment="黑木握柄"),
         el((5.5, 4.0, 7.25), (10.5, 5.0, 8.75), "G", uv, comment="十字短护手"),
         el((7.0, 5.0, 7.6), (9.0, 9.0, 8.4), "b", uv, comment="刃根"),
         el((6.6, 9.0, 7.6), (9.4, 15.5, 8.4), "B", uv, comment="叶形刃最宽段（刃长 2/3 处）"),
         el((7.2, 15.5, 7.6), (8.8, 18.0, 8.4), "b", uv, comment="收尖"),
         el((7.7, 18.0, 7.65), (8.3, 19.5, 8.35), "b", uv),
         el((7.85, 5.0, 7.5), (8.15, 17.5, 8.5), "h", uv, comment="刃脊高光")]
    held("xiphos", tilt(e))
    item("xiphos", "希腊短剑（T17）：物品栏用平面图标，手持用 held/xiphos 3D 模型")


# ───────────────────────────── 重装长矛 Dory ─────────────────────────────

def dory():
    uv = swatches("dory", {"w": "#c9a876", "W": "#a68052", "l": "#5a3a24", "b": "#c98f3e", "B": "#e8c27a",
                           "d": "#8a5a24"})
    e = [el((7.7, -9.4, 7.7), (8.3, -8.4, 8.3), "d", uv, comment="尾刺 sauroter 尖"),
         el((7.4, -8.4, 7.4), (8.6, -5.0, 8.6), "d", uv, comment="尾刺 sauroter"),
         el((7.5, -5.0, 7.5), (8.5, 18.0, 8.5), "w", uv, comment="白蜡木杆（握点在杆长 1/3）"),
         el((7.35, 0.8, 7.35), (8.65, 3.8, 8.65), "l", uv, comment="握把皮缠"),
         el((7.3, 18.0, 7.3), (8.7, 19.5, 8.7), "d", uv, comment="矛头銎口"),
         el((7.0, 19.5, 7.7), (9.0, 20.5, 8.3), "b", uv, comment="叶形矛尖"),
         el((6.7, 20.5, 7.7), (9.3, 22.0, 8.3), "b", uv),
         el((7.3, 22.0, 7.7), (8.7, 23.2, 8.3), "b", uv),
         el((7.7, 23.2, 7.75), (8.3, 24.2, 8.25), "b", uv),
         el((7.85, 19.5, 7.6), (8.15, 23.5, 8.4), "B", uv, comment="矛脊高光")]
    held("dory", tilt(e), display={
        # 32u 比原版工具长一倍：第一人称缩小，免得占满画面
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 2.0, 1.13], "scale": [0.5, 0.5, 0.5]},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 2.0, 1.13], "scale": [0.5, 0.5, 0.5]}})
    item("dory", "重装长矛（T17）：物品栏用平面图标，手持用 held/dory 3D 模型")


# ───────────────────────────── 橄榄木大棒 ─────────────────────────────

def club():
    uv = swatches("olive_club", {"o": "#bfa470", "O": "#d8c08a", "w": "#8f7448", "k": "#4e3c24", "d": "#6b5434"})
    e = [el((7.3, -2.0, 7.3), (8.7, 6.0, 8.7), "w", uv, comment="削细的握端"),
         el((7.0, 6.0, 7.0), (9.0, 10.0, 9.0), "o", uv, comment="渐粗"),
         el((6.6, 10.0, 6.6), (9.4, 14.0, 9.4), "o", uv),
         el((6.2, 14.0, 6.2), (9.8, 17.5, 9.8), "o", uv, comment="粗端（打击端在粗头 6u 以内）"),
         el((6.4, 17.5, 6.4), (9.6, 18.0, 9.6), "O", uv, comment="端面"),
         el((9.4, 11.0, 7.6), (10.2, 12.0, 8.6), "k", uv, comment="节疤 ×3"),
         el((5.8, 13.5, 7.4), (6.6, 14.5, 8.4), "k", uv),
         el((7.6, 15.0, 9.8), (8.6, 16.0, 10.6), "k", uv),
         el((6.9, 7.0, 8.95), (7.3, 13.5, 9.1), "d", uv, comment="树皮纹")]
    held("olive_club", tilt(e))
    item("olive_club", "橄榄木大棒（T17）：物品栏用平面图标，手持用 held/olive_club 3D 模型")


# ───────────────────────────── 青铜圆盾 Aspis ─────────────────────────────

SHIELD_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [10, 6, 12], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-10, 2, -10], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [10, 0, -10], "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
    "fixed": {"rotation": [0, 180, 0], "translation": [-4.5, 4.5, -5], "scale": [0.55, 0.55, 0.55]},
    "ground": {"rotation": [0, 0, 0], "translation": [2, 4, 2], "scale": [0.25, 0.25, 0.25]}}
BLOCKING_DISPLAY = {
    "thirdperson_righthand": {"rotation": [45, 155, 0], "translation": [-3.49, 11, -2], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [45, 155, 0], "translation": [11.51, 7, 2.5], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, 180, -5], "translation": [-15, 5, -11], "scale": [1.25, 1.25, 1.25]},
    "firstperson_lefthand": {"rotation": [0, 180, -5], "translation": [5, 5, -11], "scale": [1.25, 1.25, 1.25]},
    "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]}}


def disc(radius, z0, z1, color, uv, comment=None, step=1.0):
    """用 1u 高的横条拼出圆盘（中心在原点）。"""
    out = []
    n = int(round(radius * 2 / step))
    for i in range(n):
        y0 = -radius + i * step
        yc = y0 + step / 2
        w = math.sqrt(max(0.0, radius * radius - yc * yc))
        out.append(el((-w, y0, z0), (w, y0 + step, z1), color, uv, comment=comment if i == 0 else None))
    return out


def aspis():
    uv = swatches("aspis", {"r": "#7a4e20", "b": "#b07a36", "h": "#e2b56a", "k": "#2a1a10", "w": "#6b4a2b",
                            "l": "#5a3a24"})
    e = (disc(7.5, 0.0, 1.0, "w", uv, comment="木芯背板")
         + disc(7.5, 1.0, 1.6, "r", uv, comment="盾缘（暗铜）")
         + disc(6.5, 1.6, 2.1, "b", uv, comment="凸面青铜盾面")
         + disc(3.0, 2.1, 2.3, "k", uv, comment="黑色绘纹圈")
         + disc(2.4, 2.3, 2.8, "h", uv, comment="中心凸起"))
    e += [el((-1.6, -1.2, -2.2), (1.6, 1.2, 0.0), "l", uv, comment="中央臂环 porpax"),
          el((5.0, -1.0, -1.6), (6.0, 1.0, 0.0), "l", uv, comment="边缘手握 antilabe")]
    held("aspis", e, display=SHIELD_DISPLAY, extra={"gui_light": "front"})
    held("aspis_blocking", e, display=BLOCKING_DISPLAY, extra={"gui_light": "front"})
    item("aspis", "青铜圆盾（T17）：物品栏用平面图标；手持用 held/aspis，举盾时换 held/aspis_blocking（原版盾的视角变换）",
         overrides=[{"predicate": {"blocking": 1}, "model": "mythic_age:item/held/aspis_blocking"}])


XIPHOS_ICON = ["...........oo...",
               "..........ohko..",
               ".........ohbko..",
               "........ohBbo...",
               ".......ohBbo....",
               "......ohBbo.....",
               ".....ohbbo......",
               "..o.ohbbo.......",
               "..oohbbo........",
               "...oGgo.........",
               "..oGooGo........",
               ".oGo.oGGo.......",
               "oppo...oo.......",
               "ppo.............",
               "oo..............",
               "................"]
XIPHOS_PAL = {"o": "#2a1a10", "h": "#f6d68a", "B": "#e8c27a", "b": "#c98f3e", "k": "#8a5a24", "G": "#8a5a24",
              "g": "#2a1e16", "p": "#a8742e"}


def main():
    xiphos()
    dory()
    club()
    aspis()
    spec("xiphos", XIPHOS_ICON, XIPHOS_PAL, 16, "item/")
    print("xiphos dory aspis olive_club")


if __name__ == "__main__":
    main()
