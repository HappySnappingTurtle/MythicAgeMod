#!/usr/bin/env python3
"""希腊神庙方块的模型与贴图规格生成器（T8a 设计文档 §2）。

生成：
  - 方块模型 JSON：neoforge/src/main/resources/assets/mythic_age/models/block/
      greek_altar、greek_brazier(_lit)、greek_statue_lower/_upper/_item、greek_pediment_single/left/middle/right
  - 像素贴图规格：tools/texgen/specs/greek/temple_*.json（再由 texgen.py 生成 PNG）
模型都按「正面朝北（-Z）」绘制，方块状态按 facing 旋转。

运行：python3 tools/models/greek_temple.py && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/temple_*.json
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODELS = ROOT / "neoforge/src/main/resources/assets/mythic_age/models/block"
SPECS = ROOT / "tools/texgen/specs/greek"

MARBLE = {"h": "#f6f2ea", "m": "#ebe5d8", "n": "#e0d9ca", "j": "#c4bba8", "k": "#a99f8b"}


def tex(name: str) -> str:
    return "mythic_age:block/" + name


def spec(name: str, rows: list[str], palette: dict[str, str]) -> None:
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
    data = {"template": "pixels", "out": "block/" + name, "size": 16, "palette": palette,
            "params": {"kind": "block", "rows": rows}}
    (SPECS / f"{name}.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def face(t: str, uv=None, cull=None, tint=None) -> dict:
    f = {"texture": "#" + t}
    if uv is not None:
        f["uv"] = uv
    if cull:
        f["cullface"] = cull
    return f


def box(frm, to, t, faces="nsewud", comment=None, uv=None, rotation=None, shade=True) -> dict:
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    el = {}
    if comment:
        el["__comment"] = comment
    el["from"] = list(frm)
    el["to"] = list(to)
    if rotation:
        el["rotation"] = rotation
    if not shade:
        el["shade"] = False
    el["faces"] = {}
    for c in faces:
        n = names[c]
        el["faces"][n] = face(t if isinstance(t, str) else t[n], uv[n] if isinstance(uv, dict) and n in uv else None)
    return el


def model(name: str, textures: dict, elements: list, parent="minecraft:block/block", render_type=None, display=None):
    m = {"parent": parent, "textures": textures, "elements": elements}
    if render_type:
        m["render_type"] = render_type
    if display:
        m["display"] = display
    (MODELS / f"{name}.json").write_text(json.dumps(m, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


# ───────────────────────────── 大理石柱 ─────────────────────────────
# 侧面：四道凹槽（每道 4 像素：亮边、槽面、暗面、槽底），竖向无缝平铺
spec("marble_pillar", [
    "hmnjhmnjhmnjhmnj",
    "hmnjhmnjhmnjhmnj",
    "hmnjhmnjhmnjhmnj",
    "hmnjhmmjhmnjhmnj",
    "hmnjhmnjhmnjhmnj",
    "hmnjhmnjhmnkhmnj",
    "hmnjhmnjhmnjhmnj",
    "hmnkhmnjhmnjhmnj",
    "hmnjhmnjhmnjhmnj",
    "hmnjhmnjhmnjhmmj",
    "hmnjhmnjhmnjhmnj",
    "hmnjhmnjhmnjhmnj",
    "hmnjhmmjhmnjhmnj",
    "hmnjhmnjhmnjhmnj",
    "hmnjhmnjhnnjhmnj",
    "hmnjhmnjhmnjhmnj",
], MARBLE)
# 端面：圆形柱截面，外缘一圈凹槽的锯齿
spec("marble_pillar_top", [
    "jjjjjjjjjjjjjjjj",
    "jjjjjhjhjhjjjjjj",
    "jjjhnmmmmmmnhjjj",
    "jjhmmmmmmmmmmhjj",
    "jhmmmmnnnnmmmmhj",
    "jnmmmnmmmmnmmmnj",
    "jhmmnmmhhmmnmmhj",
    "jnmmnmhmmhmnmmnj",
    "jhmmnmhmmhmnmmhj",
    "jnmmnmmhhmmnmmnj",
    "jhmmmnmmmmnmmmhj",
    "jjmmmmnnnnmmmmjj",
    "jjhmmmmmmmmmmhjj",
    "jjjhnmmmmmmnhjjj",
    "jjjjjhjhjhjjjjjj",
    "jjjjjjjjjjjjjjjj",
], MARBLE)

# ───────────────────────────── 奥林匹斯祭坛 ─────────────────────────────
# 台身侧面（按方块坐标自动取 UV：台身占 v 4–13）：上缘阴影、赤陶回纹带、花环与牛头饰、下缘阴影
ALTAR_PAL = dict(MARBLE, r="#a9492a", R="#c8643b", g="#7d8a4a", G="#9fae62")
spec("greek_altar_side", [
    "hhhhhhhhhhhhhhhh",
    "mmmmmmmmmmmmmmmm",
    "nnnnnnnnnnnnnnnn",
    "jjjjjjjjjjjjjjjj",
    "kkkkkkkkkkkkkkkk",
    "mRRRRRRRRRRRRRRm",
    "mRrrrRrrrRrrrRRm",
    "mRrRrRrRrRrRrRRm",
    "mRRRrRRRrRRRrRRm",
    "mmmmmmmmmmmmmmmm",
    "mmmGmmmmmmmmGmmm",
    "mmgmGmmhhmmGmgmm",
    "mmmmmgGnnGgmmmmm",
    "jmmmmmmmmmmmmmmj",
    "kkkkkkkkkkkkkkkk",
    "jjjjjjjjjjjjjjjj",
], ALTAR_PAL)
# 顶面：大理石沿、浅火槽（灰烬与余烬）
spec("greek_altar_top", [
    "hhhhhhhhhhhhhhhh",
    "hmmmmmmmmmmmmmmj",
    "hmmmmmmmmmmmmmmj",
    "hmmjjjjjjjjjjmmj",
    "hmmjAAaAAAaAjmmj",
    "hmmjAaAAeAAAjmmj",
    "hmmjAAAaAAaAjmmj",
    "hmmjAeAAAAAAjmmj",
    "hmmjAAAAaAeAjmmj",
    "hmmjaAAAAAAAjmmj",
    "hmmjAAeAAaAAjmmj",
    "hmmjAAAAAAAajmmj",
    "hmmhhhhhhhhhhmmj",
    "hmmmmmmmmmmmmmmj",
    "hmmmmmmmmmmmmmmj",
    "jjjjjjjjjjjjjjjj",
], dict(MARBLE, A="#4a403a", a="#2f2925", e="#d8641c"))

model("greek_altar", {"particle": tex("smooth_marble"), "base": tex("marble_ashlar"),
                      "body": tex("greek_altar_side"), "top": tex("greek_altar_top"),
                      "rim": tex("smooth_marble")}, [
    box((0, 0, 0), (16, 3, 16), "base", comment="台基"),
    box((1, 3, 1), (15, 12, 15), "body", faces="nsew", comment="台身（回纹与花环）"),
    box((0, 12, 0), (16, 15, 16), {"north": "rim", "south": "rim", "east": "rim", "west": "rim",
                                   "up": "top", "down": "rim"}, comment="顶沿与火槽"),
    # 四角的小角饰（祭坛常见的「角」）
    *[box(c0, c1, "rim", faces="nsewu", comment="角饰" if i == 0 else None,
          uv={"north": [0, 0, 3, 2], "south": [0, 0, 3, 2], "east": [0, 0, 3, 2], "west": [0, 0, 3, 2],
              "up": [0, 0, 3, 3]})
      for i, (c0, c1) in enumerate([((0, 15, 0), (3, 17, 3)), ((13, 15, 0), (16, 17, 3)),
                                    ((0, 15, 13), (3, 17, 16)), ((13, 15, 13), (16, 17, 16))])],
])

# ───────────────────────────── 基础圣火盆 ─────────────────────────────
# 一张图集：上半为石碗外壁（圆石台阶色），左下为木腿，右下为炭（熄灭 / 点燃两版）；四腿木架
BRAZIER_PAL = {"s": "#8f8f8f", "S": "#a8a8a8", "d": "#6b6b6b", "D": "#555555", "w": "#6b4a2b", "W": "#8a6238",
               "c": "#2a2522", "C": "#3d3530", "x": "#1a1715"}
spec("greek_brazier", [
    "SSSSSSSSSSSSSSSS",
    "SsSssSsssSssSsSs",
    "sssdsssssdssssds",
    "ssssssdssssssdss",
    "dsssdssssssdssss",
    "DDDDDDDDDDDDDDDD",
    "sSssssSsssSssSss",
    "dddddddddddddddd",
    "WwcCccCcCcccCccC",
    "WwCcCccCcccCcCcc",
    "WwccCcCccCcccCcC",
    "WwCcccCcCcCccCcc",
    "WwcCcCccccCcCccC",
    "WwccCccCcCccCcxc",
    "WwCcxcCcccCcccCc",
    "WwcCccCcxCcCcCcc",
], BRAZIER_PAL)
spec("greek_brazier_lit", [
    "SSSSSSSSSSSSSSSS",
    "SsSssSsssSssSsSs",
    "sssdsssssdssssds",
    "ssssssdssssssdss",
    "dsssdssssssdssss",
    "DDDDDDDDDDDDDDDD",
    "sSssssSsssSssSss",
    "dddddddddddddddd",
    "WwcRcYcRcYcRcYcR",
    "WwRcYRcYRcYRcRcY",
    "WwcYRcRcYcRcYcRc",
    "WwRcRYcRcYRcRcYc",
    "WwYcRcYcRcRcYcRc",
    "WwcRcYcRYcRcRcYc",
    "WwRcYcRcRcYcRcRc",
    "WwcYcRcYcRcRcYcR",
], dict(BRAZIER_PAL, R="#d8641c", Y="#ffcc4a"))

BOWL = [0, 0, 16, 7]
LEG = [0, 8, 2, 16]
COAL = [2, 8, 16, 16]


def brazier_elements(lit: bool) -> list:
    els = []
    # 四腿木架：腿在盆身四角正下方，从地面一直顶到盆身底面（y=10），底部有外伸的脚垫；
    # 四根横撑连接相邻两腿，两端都插进腿里（用户 2026-10-15：原三脚架正面只看到两条腿、且没连上盆）
    leg = {k: LEG for k in ("north", "south", "east", "west")}
    lo, hi = 3.5, 11.0  # 两条腿的起点（腿宽 1.5）
    for i, (x, z) in enumerate([(lo, lo), (hi, lo), (lo, hi), (hi, hi)]):
        els.append(box((x, 0, z), (x + 1.5, 10, z + 1.5), "b", faces="nsewd", comment="木腿" if i == 0 else None,
                       uv=dict(leg, down=[0, 8, 2, 10])))
        fx = x - 0.5 if x == lo else x
        fz = z - 0.5 if z == lo else z
        els.append(box((fx, 0, fz), (fx + 2, 1, fz + 2), "b", faces="nsewu", comment="脚垫" if i == 0 else None,
                       uv={k: [0, 8, 2, 9] for k in ("north", "south", "east", "west", "up")}))
    bar = {k: [0, 8, 2, 9] for k in ("north", "south", "east", "west", "up", "down")}
    y0, y1 = 4.0, 5.0
    els += [box((lo + 1.5, y0, lo + 0.25), (hi, y1, lo + 1.25), "b", comment="横撑（两端插入腿中）", uv=bar),
            box((lo + 1.5, y0, hi + 0.25), (hi, y1, hi + 1.25), "b", uv=bar),
            box((lo + 0.25, y0, lo + 1.5), (lo + 1.25, y1, hi), "b", uv=bar),
            box((hi + 0.25, y0, lo + 1.5), (hi + 1.25, y1, hi), "b", uv=bar)]
    # 石碗：碗底、碗身、外翻碗沿（四面墙，中空）
    els.append(box((5, 9, 5), (11, 10, 11), "b", faces="nsewd", comment="碗底",
                   uv={k: [0, 5, 16, 7] for k in ("north", "south", "east", "west", "down")}))
    els.append(box((3, 10, 3), (13, 12, 13), "b", faces="nsewd", comment="碗身",
                   uv={k: [0, 3, 16, 5] for k in ("north", "south", "east", "west", "down")}))
    rim = {k: [0, 0, 16, 2] for k in ("north", "south", "east", "west", "up", "down")}
    els += [
        box((2, 12, 2), (14, 14, 3), "b", comment="碗沿", uv=rim),
        box((2, 12, 13), (14, 14, 14), "b", uv=rim),
        box((2, 12, 3), (3, 14, 13), "b", uv=rim),
        box((13, 12, 3), (14, 14, 13), "b", uv=rim),
    ]
    els.append(box((3, 12, 3), (13, 13, 13), "b", faces="u", comment="炭", uv={"up": COAL}))
    if lit:
        # 两片交叉的火焰（原版营火火焰贴图，带动画）
        els.append({"__comment": "火焰", "from": [3, 13, 8], "to": [13, 24, 8], "shade": False,
                    "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]},
                    "faces": {"north": face("fire", [0, 0, 16, 16]), "south": face("fire", [0, 0, 16, 16])}})
        els.append({"from": [8, 13, 3], "to": [8, 24, 13], "shade": False,
                    "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]},
                    "faces": {"east": face("fire", [0, 0, 16, 16]), "west": face("fire", [0, 0, 16, 16])}})
    return els


model("greek_brazier", {"particle": tex("greek_brazier"), "b": tex("greek_brazier")}, brazier_elements(False),
      render_type="minecraft:cutout")
model("greek_brazier_lit", {"particle": tex("greek_brazier_lit"), "b": tex("greek_brazier_lit"),
                            "fire": "minecraft:block/campfire_fire"}, brazier_elements(True),
      render_type="minecraft:cutout")

# ───────────────────────────── 神像（雅典娜：头盔、长矛、圆盾） ─────────────────────────────
STATUE_PAL = dict(MARBLE, b="#b08d3e", B="#d2ad55", o="#7a5f26")
# 长袍：竖向褶皱
spec("greek_statue_robe", [
    "mmhmnjmmhmnmjhmn",
    "mmhmnjmmhmnmjhmn",
    "mmhmnjmmhmnmjhmn",
    "mmhmnjmhmmnmjhmn",
    "mmhmnjmhmmnmjhmn",
    "mhmmnjmhmmnjmhmn",
    "mhmmnjmhmnmjmhmn",
    "mhmnmjhmmnmjmhmn",
    "mhmnmjhmmnmjhmmn",
    "hmmnmjhmnmmjhmmn",
    "hmmnmjhmnmjmhmnm",
    "hmnmmjhmnmjmhmnm",
    "hmnmjmhmnmjhmmnm",
    "hmnmjmhnmmjhmnmj",
    "nnnnjnnnnnjnnnnj",
    "jjjjjjjjjjjjjjjj",
], STATUE_PAL)
# 脸（正面 4×4 落在 u 6–10、v 4–8；按方块坐标取 UV）：眼窝、鼻梁阴影、唇线
spec("greek_statue_face", [
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmjhhjmmmmmm",
    "mmmmmmhnnhmmmmmm",
    "mmmmmmhjnhmmmmmm",
    "mmmmmmmnnmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
], STATUE_PAL)
# 圆盾正面：青铜盾面（与头盔、长矛同色）、亮边外缘，中心是大理石的戈耳工头浮雕
spec("greek_statue_shield", [
    "....oooooooo....",
    "..ooBBBBBBBBoo..",
    ".oBBbbbbbbbbBBo.",
    ".oBbbbbbbbbbbBo.",
    "oBbbbbmmmmbbbbBo",
    "oBbbbmhmmhmbbbBo",
    "oBbbmhjmmjhmbbBo",
    "oBbbmmmnnmmmbbBo",
    "oBbbmmjnnjmmbbBo",
    "oBbbmjmjjmjmbbBo",
    "oBbbbmmmmmmbbbBo",
    "oBbbbbmmmmbbbbBo",
    ".oBbbbbbbbbbbBo.",
    ".oBBbbbbbbbbBBo.",
    "..ooBBBBBBBBoo..",
    "....oooooooo....",
], STATUE_PAL)
spec("greek_statue_bronze", [
    "BBBBBBBBBBBBBBBB",
    "BbbbbbbbbbbbbbbB",
    "BbBbbbbbbbbbBbbo",
    "Bbbbbbbbbbbbbbbo",
    "BbbbbBbbbbbbbbbo",
    "Bbbbbbbbbbbbbbbo",
    "Bbbbbbbbbbbbbbbo",
    "BbbbbbbbbBbbbbbo",
    "Bbbbbbbbbbbbbbbo",
    "Bbbbbbbbbbbbbbbo",
    "BbbBbbbbbbbbbbbo",
    "Bbbbbbbbbbbbbbbo",
    "Bbbbbbbbbbbbbbbo",
    "BbbbbbbbbbbBbbbo",
    "Bbbbbbbbbbbbbbbo",
    "Booooooooooooooo",
], STATUE_PAL)

STATUE_TEX = {"particle": tex("smooth_marble"), "base": tex("marble_ashlar"), "robe": tex("greek_statue_robe"),
              "skin": tex("smooth_marble"), "face": tex("greek_statue_face"), "shield": tex("greek_statue_shield"),
              "bronze": tex("greek_statue_bronze")}


def statue_lower() -> list:
    return [
        box((1, 0, 1), (15, 3, 15), "base", comment="基座"),
        box((2, 3, 2), (14, 4, 14), "skin", comment="基座顶沿"),
        box((4, 4, 5), (12, 16, 11), "robe", faces="nsewu", comment="长袍下摆（直垂的褶皱）"),
        box((3.5, 4, 5.5), (12.5, 6, 10.5), "robe", faces="nsew", comment="袍脚外展"),
        box((5, 4, 4), (7, 5, 5), "skin", faces="nsewu", comment="脚尖"),
        box((9, 4, 4), (11, 5, 5), "skin", faces="nsewu"),
        # 左手（角色左 = 西 = -X）持盾：盾立在身侧，下缘落在基座上
        box((1.5, 4, 3), (2.5, 16, 13), {"north": "shield", "south": "shield", "east": "shield", "west": "shield",
                                         "up": "shield", "down": "shield"}, comment="圆盾（下半）",
            uv={"west": [3, 0, 13, 12], "east": [3, 0, 13, 12], "north": [7, 0, 8, 12], "south": [7, 0, 8, 12],
                "up": [7, 2, 8, 12], "down": [7, 2, 8, 12]}),
        # 右手（东 = +X）长矛：矛杆从基座立起
        box((13, 3, 7.5), (14, 16, 8.5), "bronze", comment="矛杆（下段）"),
    ]


def statue_upper() -> list:
    return [
        box((4.5, 0, 5.5), (11.5, 6, 10.5), "robe", faces="nsewd", comment="上身（披肩褶皱）"),
        box((4, 5, 5), (12, 7, 11), "skin", faces="nsewud", comment="肩"),
        box((7, 7, 6.5), (9, 8, 9.5), "skin", faces="nsew", comment="颈"),
        box((6, 8, 6), (10, 12, 10), {"north": "face", "south": "skin", "east": "skin", "west": "skin",
                                      "up": "skin", "down": "skin"}, comment="头"),
        box((5.5, 11, 5.5), (10.5, 13, 10.5), "bronze", faces="nsewud", comment="头盔"),
        box((7.5, 13, 5), (8.5, 15.5, 11), "bronze", faces="nsewu", comment="盔冠"),
        box((5.5, 10, 9.5), (10.5, 12, 10.5), "bronze", faces="nsew", comment="护颈"),
        # 左臂垂下扶盾
        box((2.5, 0, 6.5), (4.5, 6, 9.5), "skin", faces="nsewd", comment="左臂"),
        box((1.5, 0, 3), (2.5, 4, 13), {"north": "shield", "south": "shield", "east": "shield", "west": "shield",
                                        "up": "shield", "down": "shield"}, comment="圆盾（上半）",
            uv={"west": [3, 12, 13, 16], "east": [3, 12, 13, 16], "north": [7, 12, 8, 16], "south": [7, 12, 8, 16],
                "up": [7, 2, 8, 12], "down": [7, 2, 8, 12]}),
        # 右臂上举握矛
        box((11.5, 3, 6.5), (13.5, 9, 9.5), "skin", faces="nsewud", comment="右臂（上举）"),
        box((12.5, 7, 7), (14.5, 9, 9), "skin", faces="nsewu", comment="右拳"),
        box((13, 0, 7.5), (14, 13, 8.5), "bronze", faces="nsewd", comment="矛杆（上段）"),
        box((12.75, 13, 7.25), (14.25, 16, 8.75), "bronze", faces="nsewu", comment="矛头"),
    ]


model("greek_statue_lower", STATUE_TEX, statue_lower(), render_type="minecraft:cutout")
model("greek_statue_upper", STATUE_TEX, statue_upper(), render_type="minecraft:cutout")
upper_shifted = []
for el in statue_upper():
    el = json.loads(json.dumps(el))
    el["from"][1] += 16
    el["to"][1] += 16
    upper_shifted.append(el)
model("greek_statue_item", STATUE_TEX, statue_lower() + upper_shifted, render_type="minecraft:cutout", display={
    "gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [0.42, 0.42, 0.42]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, -4, 0], "scale": [0.45, 0.45, 0.45]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
})

# ───────────────────────────── 三角楣石 ─────────────────────────────
# 山花面：蓝底，正中一尊披袍立像的浮雕（每块一尊，排成一组群像；两端只露出斜坡内的部分）
PED_PAL = dict(MARBLE, u="#3f6aa8", U="#35598f", r="#a9492a", R="#c8643b")
spec("greek_pediment", [
    "uuuuuuuuuuuuuuuu",
    "uuuuuuuhhuuuuuuu",
    "uuuuuuhmmjuuuuuu",
    "uuuuuuhmmjuuuuuu",
    "uuuuuuunjuuuuuuu",
    "uuuuuhhmmmjuuuuu",
    "uuuuhmhmmnmjuuuu",
    "uuuuhmumnmujuuuu",
    "uuuuuuhmnmjuuuuu",
    "uuuuuuhmnmjuuuuu",
    "uuuuuhmmnmmjuuuu",
    "uuuuuhmnmnmjuuuu",
    "uuuuhmmnmnmmjuuu",
    "uuuuhmnmmmnmjuuu",
    "UUUUjjjjjjjjjUUU",
    "UUUUUUUUUUUUUUUU",
], PED_PAL)
# 斜檐与横檐的线脚：亮边、赤陶色涂饰带、暗边
spec("greek_pediment_cornice", [
    "hhhhhhhhhhhhhhhh",
    "mmmmmmmmmmmmmmmm",
    "RRRRRRRRRRRRRRRR",
    "rrrrrrrrrrrrrrrr",
    "nnnnnnnnnnnnnnnn",
    "jjjjjjjjjjjjjjjj",
    "hjhjhjhjhjhjhjhj",
    "hjhjhjhjhjhjhjhj",
    "jjjjjjjjjjjjjjjj",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "jjjjjjjjjjjjjjjj",
], PED_PAL)
PED_TEX = {"particle": tex("smooth_marble"), "front": tex("greek_pediment"), "side": tex("smooth_marble"),
           "cornice": tex("greek_pediment_cornice")}
RAKE = {k: [0, 0, 16, 2] for k in ("north", "south", "east", "west", "up", "down")}
DENTIL = {"north": [0, 4, 16, 9], "south": [0, 9, 16, 14], "east": [0, 4, 1, 9], "west": [0, 4, 1, 9],
          "up": [0, 9, 16, 10], "down": [0, 9, 16, 10]}


def pediment(shape: str, bottom: bool) -> list:
    """缓坡 1:2（T8a 修订 02）。正面朝北；left 系列外侧在 x=16、坡向 x=0 升高，right 系列相反。
    斜面用 2 像素一级的阶梯填充（方块模型只能转 22.5° 的倍数，做不出 1:2 的光滑斜面），
    顶上一条 1.5 像素的斜檐线脚，正面突出 1 像素。"""
    def height(xc: float) -> float:
        return {
            "left": (16 - xc) / 2, "left_high": 8 + (16 - xc) / 2,
            "right": xc / 2, "right_high": 8 + xc / 2,
            "single": 4 - abs(xc - 8) / 2, "middle": 16,
        }[shape]

    faces = {"north": "front", "south": "side", "east": "side", "west": "side", "up": "side", "down": "side"}
    els = []
    if shape == "middle":
        els.append(box((0, 0, 1), (16, 16, 16), faces, comment="山花面"))
    else:
        for k in range(8):
            x0, x1 = k * 2, k * 2 + 2
            h = round(height(x0 + 1) * 2) / 2
            if h <= 0:
                continue
            els.append(box((x0, 0, 1), (x1, h, 16), faces, comment="斜面填充" if k == 0 else None))
            top = min(h + 1.5, 16)
            if top > h:
                els.append(box((x0, h, 0), (x1, top, 16), "cornice",
                               uv={n: [x0, 0, x1, top - h] for n in ("north", "south", "east", "west", "up", "down")},
                               comment="斜檐线脚" if k == 0 else None))
        if shape == "single":
            els.append(box((7, 4, 0), (9, 7, 3), "cornice", comment="顶端饰",
                           uv={k: [0, 0, 2, 3] for k in ("north", "south", "east", "west", "up", "down")}))
    if bottom:
        els.append(box((0, 0, -1), (16, 2, 1), "cornice", uv={k: [0, 4, 16, 6] if k in ("north", "south") else
                       [0, 4, 2, 6] for k in ("north", "south", "east", "west", "up", "down")},
                       comment="底部齿饰横檐"))
    return els


for shape in ("single", "left", "left_high", "middle", "right_high", "right"):
    for bottom in (False, True):
        model(f"greek_pediment_{shape}" + ("_bottom" if bottom else ""), PED_TEX, pediment(shape, bottom))

print("模型与贴图规格已生成")
