#!/usr/bin/env python3
"""T15 希腊野外生物的物品贴图（16 × 16）：
  - 半人马鬃弦 centaur_sinew：一卷深棕鬃毛搓成的弦，中间一个结，两端散出发梢。
运行：python3 tools/models/greek_wild.py && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/centaur_sinew.json
"""
from __future__ import annotations

from modelgen import spec

SINEW_PAL = {"o": "#1e140d", "d": "#3b2a1e", "m": "#5a4130", "h": "#8a6a4e", "k": "#c9b48a"}
SINEW = ["................",
         "......oooo......",
         "....oommmmoo....",
         "...omdddddmmo...",
         "..omdo....odmo..",
         "..odo......odo..",
         ".omdo......ohmo.",
         ".odo..okko..odo.",
         ".odo..kkkk..odo.",
         ".omdo.okko.odmo.",
         "..odo......odo..",
         "..omdo....odmo..",
         "...omddddddmo.h.",
         "....oommmmoo.h..",
         "......oooo..h...",
         "................"]


def main():
    spec("centaur_sinew", SINEW, SINEW_PAL, 16, "item/")
    print("centaur_sinew")


if __name__ == "__main__":
    main()
