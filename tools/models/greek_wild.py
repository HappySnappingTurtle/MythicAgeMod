#!/usr/bin/env python3
"""T15 希腊野外生物的物品贴图（16 × 16）：
  - 半人马鬃弦 centaur_sinew：一卷深棕鬃毛搓成的弦，中间一个结，两端散出发梢。
  - 青铜羽毛 bronze_feather：斜放的一根青铜羽，深色羽轴、亮色前缘，羽根是一截细管。
  - 青铜响板 krotala：两片长条青铜拍板，下端用皮绳连在一起（V 形张开）。
  - 插地青铜羽 stuck_bronze_feather（方块，十字模型）：一根竖插的青铜羽，羽根没入地里。
  - 风暴之羽 storm_feather：一根宽大的炭黑飞羽，浅灰横斑，羽轴浅色，前缘一道蓝白闪电光。
  - 巨蟹甲壳 crab_shell：一块前宽后窄的橄榄青铜绿头胸甲，前侧缘锯齿，甲面深斑与亮点。
运行：python3 tools/models/greek_wild.py && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/<名>.json
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


BRONZE_PAL = {"o": "#3a2410", "d": "#6e4518", "m": "#a8742e", "h": "#d9a24a", "k": "#f6d68a", "s": "#4e3112",
              "l": "#6b4426", "L": "#8a5a32"}
FEATHER = ["................",
           "............oo..",
           "...........okho.",
           "..........ohksmo",
           ".........ohksmdo",
           "........ohksmdo.",
           ".......ohksmdo..",
           "......ohksmdo...",
           ".....ohksmdo....",
           "....ohksmdo.....",
           "...ohksmdo......",
           "...oksmdo.......",
           "..osodo.........",
           ".os.oo..........",
           "os..............",
           "o..............."]
KROTALA = ["................",
           "..oo.......oo...",
           ".ohko.....okho..",
           ".ohmo.....omho..",
           ".ohmdo...odmho..",
           "..ohmo...omho...",
           "..ohmdo.odmho...",
           "...ohmo.omho....",
           "...ohmdodmho....",
           "....ohmmmho.....",
           "....oommmoo.....",
           ".....olllo......",
           ".....oLLLo......",
           "....olLLLlo.....",
           ".....ooooo......",
           "................"]
STUCK = ["................",
         ".......oo.......",
         "......okho......",
         ".....ohksmo.....",
         ".....ohksmo.....",
         "....ohhksmdo....",
         "....ohhksmdo....",
         "....ohhksmdo....",
         "....ohhksmdo....",
         ".....ohksdo.....",
         ".....ohksdo.....",
         "......oksdo.....",
         ".......so.......",
         ".......so.......",
         ".......so.......",
         ".......so......."]

STORM_PAL = {"o": "#15161b", "d": "#2c2e35", "m": "#4c515b", "b": "#9ea4ac", "s": "#c8ccd2", "l": "#9fd8ff",
             "L": "#e6f6ff"}
STORM = ["................",
         "...........oLo..",
         "..........olmdo.",
         ".........olmbdo.",
         "........olmmddo.",
         ".......olbbsddo.",
         "......olmmsddo..",
         ".....olmmsdbdo..",
         "....olbbsdddo...",
         "....olmsddbo....",
         "...olmsdddo.....",
         "...olsbbdo......",
         "..olsddoo.......",
         "..oso.oo........",
         ".os.............",
         "os.............."]

SHELL_PAL = {"o": "#1f2614", "d": "#3b4526", "m": "#556236", "h": "#77854a", "k": "#a3b06a", "t": "#2a221c"}
SHELL = ["................",
         "................",
         "..o.o.oooo.o.o..",
         ".otohommmmohoto.",
         ".odmmhmmmmhmmdo.",
         "odmmdmmhkmmdmmdo",
         "odmhmmdmmmmhmmdo",
         "odmmmmmdmmmmmhdo",
         ".odmmhmmmdmmmdo.",
         ".odmmmmmmmmhmdo.",
         "..odmmdmmmmmdo..",
         "...oddmmmmddo...",
         "....ooddddoo....",
         "......oooo......",
         "................",
         "................"]


def main():
    spec("centaur_sinew", SINEW, SINEW_PAL, 16, "item/")
    spec("bronze_feather", FEATHER, BRONZE_PAL, 16, "item/")
    spec("krotala", KROTALA, BRONZE_PAL, 16, "item/")
    spec("stuck_bronze_feather", STUCK, BRONZE_PAL, 16, "block/")
    spec("storm_feather", STORM, STORM_PAL, 16, "item/")
    spec("crab_shell", SHELL, SHELL_PAL, 16, "item/")
    print("centaur_sinew bronze_feather krotala stuck_bronze_feather storm_feather crab_shell")


if __name__ == "__main__":
    main()
