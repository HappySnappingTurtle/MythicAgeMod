#!/usr/bin/env python3
"""T16 希腊圣物的物品图标（16 × 16，阶段 ① 用；阶段 ② 换 32 × 32 正式图标并加供奉小模型）。
配色按 06 号卡片：
  - 瑞亚之冠 rheia_crown：低矮月桂冠环，前方一个大叶簇；白石底托、青铜叶脉箍、橄榄绿叶（3 色）。
  - 阿喀琉斯之甲 armor_of_achilles：胸甲正面与肩扣；暗铜甲片、皮革棕肩扣、象牙白边饰（3 色）。
  - 皮格马利翁雕像 pygmalion_statue：修长的站立人像与基座；白石、浅金发带与基座镶边（2 色）。
运行：python3 tools/models/greek_relics.py && for n in rheia_crown armor_of_achilles pygmalion_statue; do
      python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/$n.json; done
"""
from __future__ import annotations

from modelgen import spec

CROWN_PAL = {"o": "#2a2a1c", "w": "#d9d4c4", "W": "#f2efe4", "s": "#a9a392",
             "b": "#8a5a2a", "B": "#c9883a", "g": "#4f6b2a", "G": "#7a9a3c", "L": "#a6c25a"}
CROWN = ["................",
         "................",
         "......oooo......",
         ".....oGLLGo.....",
         "....oGLGGLGo....",
         "...ooGGBBGGoo...",
         "..oGgoGBBGogGo..",
         ".oGgo.oggo.ogGo.",
         ".oGo........oGo.",
         ".ogBo......oBgo.",
         "..oGgo....ogGo..",
         "..obBGooooGBbo..",
         "...owWWWWWWWwo..",
         "....oswwwwwso...",
         ".....oooooooo...",
         "................"]

ARMOR_PAL = {"o": "#1f140c", "d": "#5a3a1e", "m": "#8a5a2c", "h": "#b07a3c", "k": "#d9a85e",
             "l": "#5a3a24", "L": "#7a5434", "i": "#e8dfc4"}
ARMOR = ["................",
         "..oooo....oooo..",
         ".oLLlo....olLLo.",
         ".olloooooooollo.",
         ".oiimmhkkhmmiio.",
         "..odmhkhhkhmdo..",
         "..omhkmmmmkhmo..",
         "..omhmddddmhmo..",
         "..odmhmmmmhmdo..",
         "..odmmhkkhmmdo..",
         "..oddmhhhhmddo..",
         "..odddmmmmdddo..",
         "..oiiiiiiiiiio..",
         "...odmdmmdmdo...",
         "....oooooooo....",
         "................"]

STATUE_PAL = {"o": "#2a2620", "s": "#9a9486", "w": "#d6d1c2", "W": "#f4f1e8", "y": "#c9a64a", "Y": "#ead27a"}
STATUE = [".......oo.......",
          "......oWwo......",
          "......oyYo......",
          "......oWwo......",
          ".......oo.......",
          "......oWwo......",
          ".....oWWwso.....",
          ".....owWwso.....",
          "......oWwo......",
          "......oWso......",
          "......oWso......",
          "......oWso......",
          ".....oWwwso.....",
          "...ooyYYYYyoo...",
          "...owWWWWWwso...",
          "...oooooooooo..."]


def main():
    spec("rheia_crown", CROWN, CROWN_PAL, 16, "item/")
    spec("armor_of_achilles", ARMOR, ARMOR_PAL, 16, "item/")
    spec("pygmalion_statue", STATUE, STATUE_PAL, 16, "item/")
    print("rheia_crown armor_of_achilles pygmalion_statue")


if __name__ == "__main__":
    main()
