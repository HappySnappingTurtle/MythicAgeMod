#!/usr/bin/env python3
"""希腊精英巢穴（T25a 阶段 ⑤）：独眼巨人洞穴（ST-GR-08）。

依据：
- 《奥德赛》9.181–186：海边高处一个洞，洞口有月桂遮着，羊群夜里关在里面，洞外用石块围出高墙的院子；
- 9.216–223：洞里「筐子堆满奶酪，圈里挤满羊羔和小山羊」，按大小分圈；
- 9.237–243：傍晚赶羊进洞，举起二十二辆四轮车也拉不动的巨石堵住洞口；
- 9.288–293：洞里散着他吃剩的骨头（同伴的遭遇）——只放少量骨块，点到为止；
- 9.319–320：洞里靠着羊圈放着他的大棒（青橄榄木）——大棒在他手上，这里不再放。

坐标：x 向东、y 向上、z 向南；原点是占地西北角、洞底地面上第一层（y = -1 是地面那一层）。
洞口朝南（+Z），由结构按坡向旋转。占地 17 × 29：
  洞身 z 2..19（宽约 11、高 6 的圆拱），洞口隧道 z 20..22（宽 3、高 5，巨人 3.8 格能过），洞外平台 z 23..28。
导出时只写明确给出的格子（keep_terrain）：
  - 洞内的空气（挖空）、地面、陈设都会写；
  - 岩壳（洞身外两圈，石头 / 安山岩 / 凝灰岩 / 圆石）在世界里那格已经是实心时不放（LairPiece 的处理器），
    所以嵌进山里的部分看不出人造的壳，只有山体不够厚的地方（洞口上方）才补成岩拱；
  - 写不到的格子保留原地形。
锚点：entrance（封洞巨石的 3 × 5 格）、sleep、pen、flock、cheese、wine_area、vault、cyclops。
"""
from __future__ import annotations

import math
import random

from buildgen import Building

W = 17          # x 0..16
CX = 8          # 中轴
BACK = 2        # 洞身最里一排
MOUTH = 19      # 洞身最外一排
TUNNEL = (20, 22)
APRON = (23, 28)
H = 6           # 洞身中轴净高（y 0..5）
SHELL = ("minecraft:stone", "minecraft:andesite", "minecraft:tuff", "minecraft:cobblestone", "minecraft:mossy_cobblestone")
# 奶酪架（9.219，阶段 ⑦ 的正式方块；⑤ 时用木桶占位）
CHEESE_RACK = "mythic_age:cheese_rack"
VAULT = "mythic_age:ruin_vault"


def half_width(z: int) -> float:
    """洞身各排的半宽：里面窄、中段最宽、近洞口收口，带一点起伏。"""
    t = (z - BACK) / (MOUTH - BACK)
    return 3.6 + 2.0 * math.sin(math.pi * min(1.0, t * 1.15)) - (0.8 if z >= MOUTH - 1 else 0)


def height_at(x: int, z: int) -> int:
    """圆拱：中轴高 H，往两侧降低。"""
    w = half_width(z)
    d = abs(x - CX) / max(w, 1)
    return max(3, round(H - 2.6 * d * d))


def interior(x: int, y: int, z: int) -> bool:
    if BACK <= z <= MOUTH:
        return abs(x - CX) <= half_width(z) and 0 <= y < height_at(x, z)
    if TUNNEL[0] <= z <= TUNNEL[1]:
        return abs(x - CX) <= 1 and 0 <= y < 5
    return False


def cyclops_cave() -> Building:
    b = Building("greek_cyclops_cave", "独眼巨人洞穴（ST-GR-08）")
    rnd = random.Random(2508)
    cells = [(x, y, z) for x in range(W) for y in range(0, 8) for z in range(0, TUNNEL[1] + 1) if interior(x, y, z)]
    inside = set(cells)
    # 岩壳：洞内空气外两圈（含地面下一层与洞顶）
    for (x, y, z) in cells:
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                for dz in range(-2, 3):
                    p = (x + dx, y + dy, z + dz)
                    if p in inside or p in b.blocks or not (0 <= p[0] < W) or p[2] < 0 or p[2] > TUNNEL[1] or p[1] < -2:
                        continue
                    if p[1] == -1:
                        continue  # 地面层另写
                    r = rnd.random()
                    b.set(*p, SHELL[0] if r < 0.45 else SHELL[1] if r < 0.7 else SHELL[2] if r < 0.85
                          else SHELL[3] if r < 0.95 else SHELL[4])
    # 洞内：先全部挖空
    for p in cells:
        b.set(*p, "minecraft:air")
    # 地面（y = -1）：洞内踩实的泥土与砾石、羊圈里的干草碎屑
    for x in range(W):
        for z in range(0, TUNNEL[1] + 1):
            if interior(x, 0, z):
                r = rnd.random()
                b.set(x, -1, z, "minecraft:coarse_dirt" if r < 0.5 else "minecraft:packed_mud" if r < 0.75
                      else "minecraft:gravel" if r < 0.9 else "minecraft:rooted_dirt")
    # ── 洞外平台（z 23..28）：踩出来的空地，上方清空 5 格（削掉坡上的土包），外缘留草 ──
    for z in range(APRON[0], APRON[1] + 1):
        reach = 4 + (z - APRON[0]) // 2
        for x in range(CX - reach, CX + reach + 1):
            if not 0 <= x < W:
                continue
            edge = abs(x - CX) == reach or z == APRON[1]
            b.set(x, -1, z, "minecraft:grass_block" if edge or rnd.random() < 0.35 else "minecraft:coarse_dirt")
            for y in range(0, 5):
                b.set(x, y, z, "minecraft:air")
    # 洞外石墙围的院子（9.184–186）：平台两侧各一段矮石墙，留出中间的路
    for z in range(APRON[0] + 1, APRON[1]):
        for x in (CX - 6, CX + 6):
            if 0 <= x < W:
                b.set(x, 0, z, "minecraft:cobblestone_wall" if rnd.random() < 0.8 else "minecraft:mossy_cobblestone_wall")
    # 洞口的炊烟：洞口外一侧的篝火（干草垫底，烟柱更高，远处看得见）
    b.set(CX + 3, -1, APRON[0] + 1, "minecraft:hay_block", axis="y")
    b.set(CX + 3, 0, APRON[0] + 1, "minecraft:campfire", lit="true", facing="south")
    # 洞口附近的月桂丛（9.183「洞口有月桂遮着」）
    for (x, z) in ((CX - 3, APRON[0]), (CX + 2, APRON[0] + 3)):
        b.set(x, 0, z, "mythic_age:laurel_leaves", persistent="true")

    # ── 洞内陈设 ──
    # 羊圈（9.219–222）：西侧一圈栅栏，按大小隔成两格
    for z in range(8, 15):
        b.set(CX - 4, 0, z, "minecraft:spruce_fence")
    for x in range(CX - 6, CX - 3):
        b.set(x, 0, 8, "minecraft:spruce_fence")
        b.set(x, 0, 14, "minecraft:spruce_fence")
    b.set(CX - 6, 0, 11, "minecraft:spruce_fence")
    b.set(CX - 5, 0, 11, "minecraft:spruce_fence")
    b.set(CX - 4, 0, 11, "minecraft:spruce_fence_gate", facing="east")
    for (x, z) in ((CX - 6, 9), (CX - 5, 13)):
        b.set(x, 0, z, "minecraft:hay_block", axis="x")
    b.anchor("pen", CX - 5, 0, 10)
    # 奶酪架（9.219）：东侧靠墙一排
    for z in (7, 9, 11):
        b.set(CX + 4, 0, z, CHEESE_RACK, facing="west", alt="false")
        b.anchor("cheese", CX + 4, 0, z)
    # 挤奶的桶与盛奶的器皿（9.222–223）
    b.set(CX + 3, 0, 13, "minecraft:cauldron")
    b.set(CX + 4, 0, 14, "minecraft:decorated_pot", facing="west")
    # 洞内的火塘（9.251 他生起火来）：贴着东墙，不放在走道上——巨人白天踩到火堆上，寻路把那一格当危险格，夜里就走不回睡处
    b.set(CX + 3, 0, 16, "minecraft:campfire", lit="true", facing="west")
    # 照明（用户 11-18：洞里太暗）。洞底睡处旁墙角再生一堆火（同样贴墙、不挡走道）；
    # 离地 2 格藏几块隐形光源（minecraft:light，亮度 12，没有碰撞），沿中轴每 4 格一块，羊圈、宝库上方各一块，
    # 让火光够不到的地方也看得清，又不多出看得见的器物——独眼巨人不用油灯（9.106–115 不耕种、无器具）。
    b.set(CX - 3, 0, 3, "minecraft:campfire", lit="true", facing="east")
    for z in (3, 7, 11, 15, 19):
        b.set(CX, 2, z, "minecraft:light", level="12")
    for (x, z) in ((CX - 4, 10), (CX + 3, 4)):   # 羊圈上方、宝库旁
        b.set(x, 2, z, "minecraft:light", level="12")
    # 睡处：洞底铺羊毛（羊皮褥子）
    for x in range(CX - 2, CX + 3):
        for z in (3, 4):
            b.set(x, 0, z, "minecraft:white_carpet")
    b.anchor("sleep", CX, 0, 5)
    # 宝库（赫菲斯托斯之砧）：洞底东角
    b.set(CX + 4, 0, 4, VAULT, facing="west")
    b.anchor("vault", CX + 4, 0, 4)
    # 吃剩的骨头（9.288–293）：两三块，点到为止
    for (x, z) in ((CX - 3, 5), (CX + 3, 17)):
        b.set(x, 0, z, "minecraft:bone_block", axis=rnd.choice(["x", "z"]))
    # 喝酒的空地、巨人生成点
    b.anchor("wine_area", CX, 0, 13)
    b.anchor("cyclops", CX, 0, 10)
    # 封洞巨石的位置：隧道最外一排（z = 22）3 × 5
    for x in range(CX - 1, CX + 2):
        for y in range(0, 5):
            b.anchor("entrance", x, y, TUNNEL[1])
    # 洞外放牧的羊群刷点
    b.anchor("flock", CX, 0, APRON[0] + 3)
    return b


ALL = [cyclops_cave]
