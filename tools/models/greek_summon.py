#!/usr/bin/env python3
"""T14 希腊召唤的物品贴图（16 × 16）：
  - 召唤符 summon_token_<单位>：赤陶还愿板（pinax）上的黑绘单位纹样，四角穿绳孔；
  - 传令杖 kerykeion：赫尔墨斯的金杖，两条蛇交缠、杖顶一对小翅（斜放）。
运行：python3 tools/models/greek_summon.py && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/summon_*.json tools/texgen/specs/greek/kerykeion.json
（T25a 加独眼巨人召唤符）
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

# 黑绘天马（科林斯银币式：面朝左，卷翼自背上竖起、翼梢向前钩）
_PEG = [".......kkk..",
        "......k...k.",
        "..k..kk...k.",
        ".kk..kkk.k..",
        "kwkk.kkkkk..",
        "k..kkkkkk...",
        "....kkkkkkkk",
        "....kkkkkkk.",
        "....k.k.k.k.",
        "....k.k.k..k",
        "...k..k.k...",
        "............"]
PEGASUS = ["................", ".oooooooooooooo."]
for i, row in enumerate(_PEG):
    inner = row.replace(".", "R")
    if i == 0:
        inner = "hh" + inner[2:]
    if i == len(_PEG) - 1:
        inner = "rrrrrrrrrrss"
    PEGASUS.append(".o" + inner + "o.")
PEGASUS += [".oooooooooooooo.", "................"]

# 黑绘半人马弓手（面朝左：人上身竖直、左手持弓在身前，马身向右、四条马腿）
_CEN = ["..k.kk......",
        ".k..kk......",
        ".k.kkk......",
        "k.kkkk......",
        ".k..kk......",
        ".k..kkkkkkk.",
        "..k.kkkkkkkk",
        "....kkkkkkk.",
        "....k.k.k.k.",
        "....k.k.k..k",
        "...k..k.k...",
        "............"]
CENTAUR = ["................", ".oooooooooooooo."]
for i, row in enumerate(_CEN):
    inner = row.replace(".", "R")
    if i == 0:
        inner = "hh" + inner[2:]
    if i == len(_CEN) - 1:
        inner = "rrrrrrrrrrss"
    CENTAUR.append(".o" + inner + "o.")
CENTAUR += [".oooooooooooooo.", "................"]

# 黑绘独眼巨人头像（正面）：蓬发、额心一只刻白的圆眼、露齿、浓须；右侧竖一根带树疤的大棒
_CYC = ["..kkkkkk..kk",
        ".kkkkkkkk.kk",
        ".kkwwwwkk.kk",
        ".kwwkkwwk..k",
        ".kwkkkkwk.kk",
        ".kwwkkwwk..k",
        ".kkwwwwkk..k",
        ".kkkkkkkk.kk",
        ".kkwwwwkk..k",
        "..kkkkkk...k",
        "...kkkk....k",
        "............"]
CYCLOPS = ["................", ".oooooooooooooo."]
for i, row in enumerate(_CYC):
    inner = row.replace(".", "R")
    if i == 0:
        inner = "hh" + inner[2:]
    if i == len(_CYC) - 1:
        inner = "rrrrrrrrrrss"
    CYCLOPS.append(".o" + inner + "o.")
CYCLOPS += [".oooooooooooooo.", "................"]

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
    spec("summon_token_pegasus", PEGASUS, TABLET, 16, "item/")
    spec("summon_token_centaur", CENTAUR, TABLET, 16, "item/")
    spec("summon_token_cyclops", CYCLOPS, TABLET, 16, "item/")
    spec("kerykeion", KERYKEION, KERY, 16, "item/")
    print("summon_token_minotaur、summon_token_pegasus、summon_token_centaur、summon_token_cyclops、kerykeion")


if __name__ == "__main__":
    main()
