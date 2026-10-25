#!/usr/bin/env python3
"""希腊村庄第 5 批白模：英雄、神话阶段建筑（V3-希腊-建筑 §2.3–§2.5）。出处见白模篇第 5 批「出处」表。

坐标同 buildgen：x 向东、z 向南，原点为占地西北角，y = 0 是地面上第一层。
  训练场 palaestra 15 × 15：中央铺沙的方院，四周多立克柱廊；北翼三间：擦油间（elaiothesion，陶瓮）、讲堂（ephebeion，石凳，
         对院敞开）、浴室（loutron，水池与出水口）；西翼拳击沙袋间（korykeion）；东翼撒粉间（konisterion，沙堆）。
  青铜工坊 9 × 11：坡状铸造坑（坑底一端三座泥砖熔炉）、砧与淬火水槽、晾放的黏土铸模；北侧工棚。
  剧场 21 × 17：依坡升起的半圆看台（theatron，7 排）、圆形歌队场（orchestra）中央小祭坛（thymele）、后面的布景楼（skene，
       三扇门、前面一排柱子）、两侧入场通道（parodoi）。白模在平地上，用台阶把看台垫起来；正式生成时要坡地地块。
  体育场 gymnasion 25 × 11：北面有顶的跑道柱廊（xystos，铺沙），南面平行的露天跑道（paradromis），起跑线石条（balbis）。
  大祭坛 13 × 13（2026-10-19 改为帕加马式）：高台基、东面大台阶、台顶 U 形爱奥尼柱廊围着院子，院中祭台燃圣火。
  锻炉 9 × 9：大熔炉（泥砖炉身、炉口、烟囱冒烟）、两座砧、淬火水槽、墙上挂工具；门旁一个放供奉小件与铸模的壁龛。
  神谕所 11 × 11：双柱门廊小神庙，内殿后面隔出禁入的内室（adyton）：三足鼎、肚脐石（omphalos）、月桂；门外沐浴用的泉池。

运行：python3 tools/buildings/greek_heroic.py [--world <存档目录>]
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from buildgen import Building, ROOT, write_datapack
from greek_classical import item_frame

OUT = ROOT / "docs/重构/开发/TV/白模/希腊/第5批_英雄与神话建筑"
SAND = "minecraft:sand"


def colonnade_court(b: Building, x0, z0, x1, z1, y=0, shaft=2):
    """院子四周的柱廊：外墙在 x0/x1/z0/z1，柱子在内圈每 2 格一根，柱廊上有平顶与单坡瓦顶。"""
    for x in range(x0 + 2, x1 - 1, 2):
        b.column(x, y, z0 + 2, shaft, "doric")
        b.column(x, y, z1 - 2, shaft, "doric")
    for z in range(z0 + 4, z1 - 3, 2):
        b.column(x0 + 2, y, z, shaft, "doric")
        b.column(x1 - 2, y, z, shaft, "doric")


def palaestra() -> Building:
    b = Building("greek_palaestra", "训练场（角力场）")
    n = 14
    b.fill(0, -1, 0, n, -1, n, "step")
    b.fill(3, -1, 3, n - 3, -1, n - 3, SAND)
    # 外墙
    b.hollow(0, 0, 0, n, 3, n, "wall_marble")
    b.clear(7, 0, n, 7, 2, n)  # 南门
    # 北翼三间（z 0..3）：隔墙与对院的开口
    b.fill(1, 0, 3, n - 1, 3, 3, "wall_marble")
    for x in (5, 9):
        b.fill(x, 0, 1, x, 3, 2, "wall_marble")
    b.clear(6, 0, 3, 8, 2, 3)  # 讲堂对院敞开
    b.column(6, 0, 3, 2, "doric")
    b.column(8, 0, 3, 2, "doric")
    b.clear(2, 0, 3, 2, 1, 3)  # 擦油间门
    b.clear(12, 0, 3, 12, 1, 3)  # 浴室门
    for x in range(1, 5):
        b.set(x, 0, 1, "pithos")
    b.set(1, 1, 1, "lamp")
    for x in range(6, 9):
        b.set(x, 0, 1, "mythic_age:marble_stairs", facing="north")
    b.anchor("lecture", 7, 0, 2)
    b.fill(10, -1, 1, 13, -1, 2, "floor")
    b.fill(11, -1, 1, 13, -1, 1, "minecraft:water")
    b.set(11, 2, 0, "mythic_age:marble_wall")
    b.set(13, 2, 0, "mythic_age:marble_wall")
    # 西翼拳击沙袋（korykos）：从柱廊顶垂下的沙袋
    b.set(1, 3, 8, "minecraft:chain")
    b.set(1, 2, 8, "minecraft:chain")
    b.set(1, 1, 8, "minecraft:brown_wool")
    # 东翼撒粉间：沙堆
    b.set(n - 1, 0, 8, SAND)
    b.set(n - 1, 0, 9, SAND)
    b.set(n - 1, 1, 8, "minecraft:sand")
    # 内圈柱廊
    for x in range(4, n - 3, 2):
        b.column(x, 0, n - 2, 2, "doric")
    for z in range(5, n - 2, 2):
        b.column(2, 0, z, 2, "doric")
        b.column(n - 2, 0, z, 2, "doric")
    # 柱廊顶：外墙到柱列之间的平顶 + 外圈瓦
    for x in range(1, n):
        for z in range(1, n):
            inner = 3 <= x <= n - 3 and 4 <= z <= n - 3
            if not inner:
                b.set(x, 3, z, "smooth_slab", type="top")
    b.ring_roof(-1, n + 1, -1, n + 1, 3, n - 3, 4, n - 3, 4)
    for (x, z) in ((5, 8), (9, 8), (7, 10)):
        b.anchor("training", x, 0, z)
    return b


def bronze_workshop() -> Building:
    b = Building("greek_bronze_workshop", "青铜工坊")
    b.fill(0, -4, 0, 8, -1, 10, "minecraft:packed_mud")  # 坑壁所在的土层（白模平台很薄，补实）
    # 北侧工棚
    b.fill(0, 0, 0, 8, 2, 0, "mudbrick")
    for x in (0, 4, 8):
        b.fill(x, 0, 3, x, 2, 3, "beam", axis="y")
    b.hip_roof(-1, 9, -1, 4, 3)
    b.set(2, 0, 1, "mythic_age:greek_anvil", facing="east")
    b.set(5, 0, 1, "minecraft:water_cauldron", level=3)  # 淬火水槽
    b.anchor("workstation", 2, 0, 2)
    item_frame(b, 3, 1, 1, "south", "iron_pickaxe")
    # 坡状铸造坑：x 2..6、z 5..9，北浅南深；坑底南端三座熔炉
    for z in range(5, 10):
        depth = 1 + (z - 5) // 2
        for x in range(2, 7):
            for d in range(depth):
                b.set(x, -1 - d, z, "minecraft:air")
            b.set(x, -1 - depth, z, "minecraft:packed_mud")
    for x in (2, 4, 6):
        # 青铜熔炉（两格高，T19 换装：原为高炉替身）
        b.set(x, -3, 9, "mythic_age:bronze_furnace", facing="north", half="lower", lit="true")
        b.set(x, -2, 9, "mythic_age:bronze_furnace", facing="north", half="upper", lit="true")
        b.set(x, -1, 9, "mudbrick")
        b.set(x, 0, 9, "minecraft:campfire", lit="true", signal_fire="false")
    b.anchor("workstation", 4, -2, 8)
    # 晾放的黏土铸模
    for (x, z) in ((7, 5), (7, 6), (1, 6)):
        b.set(x, 0, z, "minecraft:clay")
    b.set(7, 1, 5, "lamp")  # T19：原为饰纹陶罐替身，换成陶油灯
    return b


def theater() -> Building:
    b = Building("greek_theater", "剧场")
    cx, cz = 10, 11  # 歌队场圆心
    r = 4  # 歌队场半径
    # 歌队场：铺石地面、中央小祭坛
    for x in range(21):
        for z in range(17):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.4:
                b.set(x, -1, z, "floor")
    b.set(cx, 0, cz, "floor")
    b.set(cx, 1, cz, "brazier", lit="true")
    b.anchor("orchestra", cx, 0, cz)  # 歌队场中央（节庆演出，T19 §1.10 J5）
    # 看台：圆心北侧的半圆，7 排，每排高 1、深 1，用大理石楼梯当座位；两条竖向台阶走道
    for x in range(21):
        for z in range(0, cz + 1):
            d = math.hypot(x - cx, z - cz)
            row = int(d - r - 0.5)
            if 0 <= row < 7 and d <= r + 7.5 and z <= cz:
                for y in range(0, row):
                    b.set(x, y, z, "step")
                facing = "north" if abs(x - cx) < (cz - z) else ("west" if x < cx else "east")
                b.set(x, row, z, "mythic_age:marble_stairs", facing=facing)
    # 布景楼（skene）：歌队场南侧，三扇门，前面一排柱（proskenion）
    b.fill(3, 0, 15, 17, 3, 16, "wall_marble")
    for x in (6, 10, 14):
        b.clear(x, 0, 15, x, 1, 15)
    for x in range(4, 17, 2):
        b.column(x, 0, 14, 1, "doric")
    b.fill(3, 3, 14, 17, 3, 14, "smooth_slab", type="top")
    b.hip_roof(2, 18, 14, 17, 4)
    # 两侧入场通道（parodoi）：歌队场东西两侧通向外面
    for x in list(range(0, cx - r)) + list(range(cx + r + 1, 21)):
        b.set(x, -1, cz, "floor")
        b.set(x, -1, cz + 1, "floor")
    return b


def gymnasion() -> Building:
    b = Building("greek_gymnasion", "体育场")
    L = 24
    b.fill(0, -1, 0, L, -1, 10, "minecraft:grass_block")
    # 北面有顶的跑道柱廊（xystos）：铺沙，北墙 + 南侧一排柱
    b.fill(0, -1, 0, L, -1, 4, SAND)
    b.fill(0, 0, 0, L, 3, 0, "wall_marble")
    for x in range(0, L + 1, 2):
        b.column(x, 0, 4, 2, "doric")
    for x in range(0, L + 1):
        b.set(x, 4, 4, "architrave", facing="south")
        b.set(x, 4, 0, "wall_marble")
    b.fill(0, 4, 1, L, 4, 3, "smooth_slab", type="top")
    b.gable_roof(0, L, -1, 5, 5)
    b.pediment_gable(L + 1, -1, 5, 5, "east")
    b.pediment_gable(-1, -1, 5, 5, "west")
    # 南面平行的露天跑道（paradromis）：沙地，两侧石边
    b.fill(0, -1, 6, L, -1, 9, SAND)
    b.fill(0, -1, 5, L, -1, 5, "step")
    b.fill(0, -1, 10, L, -1, 10, "step")
    # 起跑线石条（balbis）：两端各一条带槽的石条
    for x in (1, L - 1):
        b.fill(x, -1, 6, x, -1, 9, "floor")
    b.anchor("training", 3, 0, 7)
    # 跑道两端的起跑线石条（节庆赛跑，T19 §1.10 J5）
    b.anchor("track_start", 1, 0, 7)
    b.anchor("track_finish", L - 1, 0, 7)
    return b


def great_altar_into(b: Building, ox=0, oz=0):
    """帕加马式大祭坛（13 × 13，东面上台阶）：高台基四面饰带，东面正中 7 格宽的大台阶，两侧台基如两翼向前伸；
    台顶西、北、南三面爱奥尼柱廊（U 形），围出露天院子，院子中央是燃着圣火、两端带涡卷角的祭台。
    出处：Wikipedia「Pergamon Altar」——高台基、一侧凹进的宽大台阶、台顶 U 形爱奥尼柱廊、柱廊围着的院子里是祭台本身。"""
    n = 12
    h = 4  # 台基高
    def S(x, y, z, *a, **k):
        b.set(ox + x, y, oz + z, *a, **k)
    # 台基：外墙 + 顶面；四面一圈饰带（y = 2）
    for x in range(n + 1):
        for z in range(n + 1):
            edge = x in (0, n) or z in (0, n)
            for y in range(h):
                if edge or y == h - 1:
                    S(x, y, z, "step")
            S(x, h - 1, z, "floor")
    for x in range(n + 1):
        S(x, 2, 0, "frieze", facing="north")
        S(x, 2, n, "frieze", facing="south")
    for z in range(1, n):
        S(0, 2, z, "frieze", facing="west")
        S(n, 2, z, "frieze", facing="east")
    # 东面大台阶：z 3..9，自 x = 12 向西升到台顶（两侧 z 0..2、10..12 是伸出的两翼）
    for i, x in enumerate(range(n, n - h, -1)):
        for z in range(3, 10):
            for y in range(i + 1):
                if y < i:
                    S(x, y, z, "step")
            S(x, i, z, "mythic_age:marble_ashlar_stairs", facing="west")
            for y in range(i + 1, h):
                b.blocks[(ox + x, y, oz + z)] = "minecraft:air"
    # 台顶 U 形柱廊（西、北、南三面外缘），柱高 4；柱顶额枋一圈，柱廊顶平顶
    top = h
    for z in range(0, n + 1, 2):
        b.column(ox + 0, top, oz + z, 2, "ionic", axis="x")
    for x in range(2, n + 1, 2):
        b.column(ox + x, top, oz + 0, 2, "ionic", axis="z")
        b.column(ox + x, top, oz + n, 2, "ionic", axis="z")
    for z in range(0, n + 1):
        S(0, top + 4, z, "architrave", facing="west")
        S(1, top + 4, z, "smooth_slab", type="top")
        S(2, top + 4, z, "smooth_slab", type="top")
    for x in range(1, n + 1):
        for z, f in ((0, "north"), (n, "south")):
            S(x, top + 4, z, "architrave", facing=f)
        for z in (1, 2, n - 1, n - 2):
            S(x, top + 4, z, "smooth_slab", type="top")
    for z in range(0, n + 1):
        S(0, top + 5, z, "cornice", facing="west")
    for x in range(1, n + 1):
        S(x, top + 5, 0, "cornice", facing="north")
        S(x, top + 5, n, "cornice", facing="south")
    # 院子中央的祭台：3 × 2、两层高，两端涡卷角（台阶），顶上三座圣火
    for z in range(5, 8):
        for x in (5, 6):
            S(x, top, z, "floor")
    for x in (5, 6):
        S(x, top + 1, 4, "mythic_age:marble_ashlar_stairs", facing="south")
        S(x, top + 1, 8, "mythic_age:marble_ashlar_stairs", facing="north")
    for z in (5, 6, 7):
        S(5, top + 1, z, "floor")
        S(6, top + 1, z, "brazier", lit="true")
    b.anchor("ritual", ox + 8, top, oz + 6)


def great_altar() -> Building:
    b = Building("greek_great_altar", "大祭坛（帕加马式）")
    great_altar_into(b)
    return b


def forge() -> Building:
    b = Building("greek_forge", "锻炉")
    b.fill(0, -1, 0, 8, -1, 8, "step")
    b.hollow(0, 0, 0, 8, 2, 8, "mudbrick")
    b.fill(0, 0, 0, 8, 0, 8, "socle")
    b.fill(1, 0, 1, 7, 0, 7, "minecraft:air")
    b.clear(8, 0, 3, 8, 2, 5)  # 东面大门洞
    for x in range(0, 9):
        for z in (0, 8):
            b.set(x, 3, z, "beam", axis="x")
    for z in range(1, 8):
        for x in (0, 8):
            b.set(x, 3, z, "beam", axis="z")
    b.hip_roof(-1, 9, -1, 9, 4)
    # 大熔炉：西北角泥砖炉身 + 炉口 + 穿出屋顶的烟囱
    b.fill(1, 0, 1, 3, 2, 3, "mudbrick")
    b.set(3, 0, 2, "mythic_age:bronze_furnace", facing="east", half="lower", lit="true")  # T19 换装：原为高炉替身
    b.set(3, 1, 2, "mythic_age:bronze_furnace", facing="east", half="upper", lit="true")
    for y in range(3, 7):
        b.set(2, y, 2, "mudbrick")
    b.set(2, 7, 2, "minecraft:campfire", lit="true", signal_fire="true")
    b.anchor("workstation", 4, 0, 2)
    for (x, z) in ((5, 3), (5, 6)):
        b.set(x, 0, z, "mythic_age:greek_anvil", facing="north")
    b.anchor("workstation", 6, 0, 4)
    b.set(2, 0, 6, "minecraft:water_cauldron", level=3)
    item_frame(b, 1, 2, 5, "east", "iron_axe")
    item_frame(b, 1, 2, 6, "east", "iron_shovel")
    # 门旁壁龛：工匠供奉的小件与铸模
    b.set(7, 1, 7, "smooth_slab", type="top")
    b.set(7, 2, 7, "lamp")  # T19：原为饰纹陶罐替身，换成陶油灯（供奉的小件）
    b.set(6, 1, 7, "minecraft:clay")
    return b


def oracle() -> Building:
    b = Building("greek_oracle", "神谕所")
    n = 10
    b.fill(0, -1, 0, n, -1, n, "gravel")
    top = b.crepidoma(1, 2, 9, 8, 1)
    # 小神庙：双柱门廊 + 内殿 + 后部内室（adyton）
    b.fill(1, top, 3, 1, top + 3, 7, "wall_marble")
    for z in (3, 7):
        b.fill(1, top, z, 8, top + 3, z, "wall_marble")
    b.fill(6, top, 4, 6, top + 3, 6, "wall_marble")
    b.clear(6, top, 5, 6, top + 2, 5)
    b.fill(3, top, 4, 3, top + 3, 6, "wall_marble")  # 内室隔墙
    b.clear(3, top, 5, 3, top + 1, 5)  # 低矮的门洞（禁入）
    for z in (4, 6):
        b.column(8, top, z, 2, "ionic", axis="z")
    for x in range(1, 9):
        b.set(x, top + 4, 3, "architrave", facing="north")
        b.set(x, top + 4, 7, "architrave", facing="south")
    for z in range(4, 7):
        b.set(8, top + 4, z, "architrave", facing="east")
        b.set(1, top + 4, z, "wall_marble")
    b.fill(2, top + 4, 4, 7, top + 4, 6, "floor")
    b.gable_roof(1, 8, 2, 8, top + 5)
    b.pediment_gable(9, 2, 8, top + 5, "east")
    b.pediment_gable(0, 2, 8, top + 5, "west")
    # 内室：三足鼎、肚脐石（T19 换装：原为炼药台、錾制石英块替身）；月桂盆栽等月桂方块做好再放（原为盆栽杜鹃替身）
    b.set(2, top, 5, "mythic_age:oracle_tripod", facing="east")
    b.set(2, top, 4, "mythic_age:omphalos", facing="east")
    b.anchor("oracle", 4, top, 5)
    # 内殿：神像与油灯
    b.set(4, top, 4, "lamp")
    b.set(4, top, 6, "lamp")
    # 门外：女祭司沐浴的泉池
    b.fill(8, -2, 0, 10, -2, 1, "step")
    b.fill(8, -1, 0, 10, -1, 1, "minecraft:water")
    b.set(10, 0, 0, "mythic_age:marble_wall")
    b.anchor("bath", 7, 0, 1)  # 池边：神谕日问卜前沐浴（T19 §1.10 J6）
    return b


BUILDERS = (palaestra, bronze_workshop, theater, gymnasion, great_altar, forge, oracle)


def main():
    buildings = [f() for f in BUILDERS]
    OUT.mkdir(parents=True, exist_ok=True)
    for b in buildings:
        p = b.layer_plans(OUT / f"{b.name}_逐层.png", cell=16)
        print(f"{b.title}：{len(b.blocks)} 方块，平面图 {p.relative_to(ROOT)}")
    if "--world" in sys.argv:
        world = Path(sys.argv[sys.argv.index("--world") + 1])
        print("数据包：", write_datapack(world, buildings))


if __name__ == "__main__":
    main()
