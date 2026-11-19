#!/usr/bin/env python3
"""T25a 阶段 ⑦：独眼巨人洞穴的方块与酒碗外观（⑧ 给用户看图确认）。
  - 奶酪架 cheese_rack（方块，朝向放置者）：两层粗木框，隔板是柳条编的筐底，上下层各摆圆奶酪（整块、切开的、带蜡皮的）。
    出处：《奥德赛》9.219「编织的架子上压满了奶酪」（τάρσοι μὲν τυρῶν βρῖθον）；9.246–247 凝乳放进编织的篮子里控水。
  - 酒碗 wine_bowl（物品模型，只给地上的酒碗实体显示用）：常春藤木的碗，碗里一层深红的酒。
    出处：《奥德赛》9.346 奥德修斯捧给独眼巨人的「一碗」是常春藤木碗（κισσύβιον）。
贴图是 4 × 4 的色块表（大色块、不加噪声，和圣物供奉模型同一做法），面的 UV 取色块内部。
运行：python3 tools/models/greek_cyclops_blocks.py && python3 tools/texgen/texgen.py --force \
      tools/texgen/specs/greek/cheese_rack.json tools/texgen/specs/greek/wine_bowl.json
"""
from __future__ import annotations

import json

from modelgen import MODELS, ROOT, spec

ITEM_MODELS = ROOT / "neoforge/src/main/resources/assets/mythic_age/models/item"


def swatches(name: str, folder: str, cells: dict[str, str]) -> dict[str, list[float]]:
    keys = list(cells)
    assert len(keys) <= 16, name
    rows = []
    for py in range(16):
        row = ""
        for px in range(16):
            i = (py // 4) * 4 + px // 4
            row += keys[i] if i < len(keys) else keys[0]
        rows.append(row)
    spec(name, rows, cells, 16, folder)
    return {k: [(i % 4) * 4 + 1, (i // 4) * 4 + 1, (i % 4) * 4 + 3, (i // 4) * 4 + 3] for i, k in enumerate(keys)}


def el(frm, to, c, uv, comment=None, rot=None):
    e = {}
    if comment:
        e["__comment"] = comment
    e["from"] = [round(v, 3) for v in frm]
    e["to"] = [round(v, 3) for v in to]
    if rot:
        e["rotation"] = rot
    e["faces"] = {f: {"texture": "#0", "uv": uv[c]} for f in ("north", "south", "east", "west", "up", "down")}
    return e


def write(path, textures, elements, parent="minecraft:block/block", display=None):
    m = {"parent": parent, "textures": textures, "elements": elements}
    if display:
        m["display"] = display
    path.write_text(json.dumps(m, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def cheese(x, y, z, r, uv, cut=False):
    """一块圆奶酪：十字截面近似圆（两块交叉），上面一层蜡皮色；cut 时切掉一角露出奶酪芯。"""
    h = 3.0
    out = [el((x - r, y, z - r + 1), (x + r, y + h, z + r - 1), "c", uv, comment="圆奶酪"),
           el((x - r + 1, y, z - r), (x + r - 1, y + h, z + r), "c", uv),
           el((x - r + 1, y + h, z - r + 1), (x + r - 1, y + h + 0.3, z + r - 1), "C", uv)]
    if cut:
        out.append(el((x, y + 0.01, z - r + 0.6), (x + r - 0.6, y + h + 0.31, z), "f", uv, comment="切口露出的奶酪芯"))
    return out


def cheese_rack():
    uv = swatches("cheese_rack", "block/", {"w": "#5e4228", "W": "#7c5a38", "d": "#3e2a18", "k": "#b08e52", "K": "#cfae6c",
                                             "c": "#e9b84a", "C": "#c98a2a", "f": "#f8e7a0"})
    e = []
    # 四根立柱（粗木）
    for (x, z) in ((1, 3), (13, 3), (1, 11), (13, 11)):
        e.append(el((x, 0, z), (x + 2, 15, z + 2), "w", uv, comment="立柱" if (x, z) == (1, 3) else None))
    # 两层横档与柳条筐底
    for y, label in ((1.0, "下层"), (8.0, "上层")):
        e.append(el((1, y, 3.5), (15, y + 1, 4.5), "W", uv, comment=label + "横档"))
        e.append(el((1, y, 11.5), (15, y + 1, 12.5), "W", uv))
        e.append(el((2, y + 0.5, 4.5), (14, y + 1.2, 11.5), "k", uv, comment=label + "柳条筐底"))
        for i, xx in enumerate((3, 6, 9, 12)):
            e.append(el((xx, y + 1.2, 4.5), (xx + 1, y + 1.4, 11.5), "K", uv, comment="编条" if i == 0 else None))
    # 顶上的横梁（挑出一点）
    e.append(el((0.5, 15, 3), (15.5, 16, 5), "d", uv, comment="顶梁"))
    e.append(el((0.5, 15, 11), (15.5, 16, 13), "d", uv))
    # 奶酪：每层两大块（上层一块切开），蜡黄色、厚 3u，远看也认得出
    e += cheese(5.0, 2.4, 8, 3.2, uv) + cheese(11.0, 2.4, 8, 3.0, uv)
    e += cheese(5.0, 9.4, 8, 3.0, uv, cut=True) + cheese(11.0, 9.4, 8, 3.2, uv)
    write(MODELS / "cheese_rack.json", {"0": "mythic_age:block/cheese_rack", "particle": "mythic_age:block/cheese_rack"}, e)


def wine_bowl():
    uv = swatches("wine_bowl", "item/", {"w": "#6a5a32", "W": "#8a7644", "g": "#4a5a28", "r": "#5a0f1e", "R": "#8a1a2e"})
    e = [el((5, 0, 5), (11, 1, 11), "w", uv, comment="碗底"),
         el((3, 1, 4), (13, 3.5, 5), "W", uv, comment="碗壁"),
         el((3, 1, 11), (13, 3.5, 12), "W", uv),
         el((3, 1, 5), (4, 3.5, 11), "w", uv),
         el((12, 1, 5), (13, 3.5, 11), "w", uv),
         el((4, 1, 5), (12, 2.8, 11), "r", uv, comment="酒"),
         el((5, 2.8, 6), (9, 2.85, 8), "R", uv, comment="酒面的反光"),
         el((3.2, 3.5, 4.2), (12.8, 3.8, 4.6), "g", uv, comment="碗沿刻的常春藤（一圈深绿）"),
         el((3.2, 3.5, 11.4), (12.8, 3.8, 11.8), "g", uv)]
    display = {"ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [1.0, 1.0, 1.0]},
               "gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [0.9, 0.9, 0.9]},
               # 地上的酒碗实体用 FIXED 平放着画（WineBowlRenderer），碗底贴地
               "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]}}
    write(ITEM_MODELS / "wine_bowl.json", {"0": "mythic_age:item/wine_bowl", "particle": "mythic_age:item/wine_bowl"}, e,
          display=display)


def main():
    cheese_rack()
    wine_bowl()
    print("cheese_rack wine_bowl")


if __name__ == "__main__":
    main()
