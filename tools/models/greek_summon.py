#!/usr/bin/env python3
"""T14 希腊召唤的物品贴图（16 × 16）：
  - 召唤符 summon_token_<单位>：赤陶还愿板（pinax）上的黑绘单位纹样，四角穿绳孔；
  - 传令杖 kerykeion：赫尔墨斯的金杖，两条蛇交缠、杖顶一对小翅（斜放）。
运行：python3 tools/models/greek_summon.py && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/summon_*.json tools/texgen/specs/greek/kerykeion.json
"""
from __future__ import annotations

from modelgen import spec

TABLET = {"o": "#3a1d10", "R": "#d2834e", "r": "#b4602f", "s": "#8e4521", "k": "#1e120c", "h": "#e7a873", "w": "#f1d3a8"}

# 黑绘牛头：一对向上弯的角、向两侧伸的耳朵、宽额、浅色鼻吻；板子一圈深色描边，左上受光
MINOTAUR = ["................",
            ".oooooooooooooo.",
            ".ohhRRRRRRRRRro.",
            ".ohkRRRRRRRRkro.",
            ".oRkRRRRRRRRkro.",
            ".oRRkRRRRRRkRro.",
            ".okkRkkkkkkRkko.",
            ".oRkkkwkkwkkkro.",
            ".oRRRkkkkkkRRro.",
            ".oRRRkkkkkkRRro.",
            ".oRRRRkkkkRRRro.",
            ".oRRRRksskRRRso.",
            ".oRRRRRkkRRRRso.",
            ".orrrrrrrrrrsso.",
            ".oooooooooooooo.",
            "................"]

KERY = {"o": "#3b2412", "g": "#c8902a", "G": "#e8bf52", "H": "#fbe7a0", "s": "#4d6e3b", "S": "#7aa05c",
        "w": "#f4efe3", "W": "#c9c2b1"}
KERYKEION = ["...........ww...",
             "..........wWGww.",
             "...........GHW..",
             "..........sGSs..",
             ".........S.Gg.s.",
             "..........sGs...",
             ".........sGgS...",
             "........S.Gg.s..",
             ".........sGs....",
             "........sGgS....",
             ".......S.Gg.....",
             "........Gg......",
             ".......Gg.......",
             "......Gg........",
             ".....og.........",
             "....oo.........."]


def main():
    spec("summon_token_minotaur", MINOTAUR, TABLET, 16, "item/")
    spec("kerykeion", KERYKEION, KERY, 16, "item/")
    print("summon_token_minotaur、kerykeion")


if __name__ == "__main__":
    main()
