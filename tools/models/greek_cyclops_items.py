#!/usr/bin/env python3
"""T25a 阶段 ⑥ 物品贴图（16 × 16），阶段 ⑦ / ⑧ 给用户看图确认：
  - 马洛涅烈酒 maronean_wine：一只鼓胀的山羊皮酒袋，袋口扎绳、口上渗出一滴深红的酒
    （《奥德赛》9.196：奥德修斯「带了一皮袋黑色的甜酒」；9.212 皮袋）；
  - 库克罗普斯之火 cyclops_fire：赤陶钵托着一团橙白的火苗，钵身刻一只圆眼（设计文档 §8；《神谱》139–146 库克罗普斯为宙斯锻雷电）；
  - 英雄锻造模板 hero_smithing_template：照原版锻造模板的板形，青铜板面上浮雕一个狮头（英雄装备从涅墨亚狮子起步，T25b / T25c）。
运行：python3 tools/models/greek_cyclops_items.py && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/maronean_wine.json \
      tools/texgen/specs/greek/cyclops_fire.json tools/texgen/specs/greek/hero_smithing_template.json
"""
from __future__ import annotations

from modelgen import spec

# 山羊皮：浅褐皮面、深褐描边与皱褶；扎口的绳；深红酒滴
WINESKIN_PAL = {"o": "#3b2414", "s": "#8a5a34", "S": "#a8754a", "h": "#c99a68", "d": "#6b4426",
                "c": "#d8c08a", "r": "#7a1426", "R": "#a8203a"}
WINESKIN = ["................",
            "........cc......",
            ".......occo.....",
            "........dd......",
            ".......osSo.....",
            "......osSShoo...",
            ".....osSShhSSo..",
            "....osSShhSSSso.",
            "...osSSShSSSSso.",
            "...osSSSSSSSdso.",
            "..osSSSSSSSdsso.",
            "..osSSSSSSdssoo.",
            "..osSSSSSdssso..",
            "...ossddssssoR..",
            "....oooooooo.r..",
            "................"]

# 赤陶钵 + 火苗；钵身一只刻出的圆眼（白圈、黑瞳）
FIRE_PAL = {"o": "#3a1d10", "R": "#c56b3a", "r": "#a2532a", "s": "#7e3d1d", "w": "#f2e3c6", "k": "#1e120c",
            "y": "#ffe9a0", "Y": "#fff8dc", "f": "#ffb43c", "F": "#ff7a1a", "e": "#d9431a"}
FIRE = ["................",
        ".......Y........",
        "......yY........",
        "......yYy.......",
        ".....fyYyf......",
        ".....fyYYf......",
        "....FfyYyfF.....",
        "....FffyyfFe....",
        "...eFFffffFFe...",
        ".ooooooooooooo..",
        ".oRRRRwwwRRRro..",
        "..oRRwwkwwRrro..",
        "..oRRRwwwRRrso..",
        "...orrrrrrrso...",
        "....oooooooo....",
        "................"]

# 锻造模板：深色板边、青铜板面、浮雕狮头（鬃毛一圈、两眼、鼻吻）
TEMPLATE_PAL = {"o": "#2a1e14", "b": "#8c5a24", "B": "#b07a34", "h": "#d6a456", "d": "#5e3c18",
                "m": "#6e4a1c", "M": "#c48a3a", "e": "#1e140c"}
TEMPLATE = ["................",
            "..oooooooooooo..",
            ".ohhBBBBBBBBBBdo",
            ".ohBBmmmmmmBBBdo",
            ".oBBmMMMMMMmBBdo",
            ".oBmMMhMMhMMmBdo",
            ".oBmMMeMMeMMmBdo",
            ".oBmMMMMMMMMmBdo",
            ".oBmMMMddMMMmBdo",
            ".oBBmMMddMMmBBdo",
            ".oBBBmmMMmmBBBdo",
            ".oBBBBBmmBBBBBdo",
            ".oBBBBBBBBBBBBdo",
            ".obddddddddddddo",
            "..oooooooooooooo",
            "................"]


def main():
    spec("maronean_wine", WINESKIN, WINESKIN_PAL, 16, "item/")
    spec("cyclops_fire", FIRE, FIRE_PAL, 16, "item/")
    spec("hero_smithing_template", TEMPLATE, TEMPLATE_PAL, 16, "item/")
    print("maronean_wine、cyclops_fire、hero_smithing_template")


if __name__ == "__main__":
    main()
