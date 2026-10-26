#!/usr/bin/env python3
"""希腊遗迹（T22）：废弃神殿（ST-GR-02）。

形制参考：多立克式围柱神庙的残迹——奥林匹亚的赫拉神庙（柱子高矮不一，部分只剩柱础与一两段柱身）、
帕埃斯图姆与塞利努斯（倒塌的柱段成串躺在台基旁，柱头与檐部碎块散在地上）；希腊神庙正门朝东，
祭坛在正门前的露天里（T8a 已按此做）。内殿（naos）里是神像，后殿（opisthodomos）在古代常用作存放
供品与财物的库房（帕特农神庙的后殿就存放雅典的公共财物），所以宝库放在后殿。

坐标：x 向东、y 向上、z 向南；原点是占地西北角、地面上第一层（y = -1 是地面那一层，前庭铺地写在 y = -1）。
占地 31 × 21：神庙台基 x 0..23，前庭 x 24..30（正门朝东）。
流程（09 合同 ST-GR-02）：远处倒塌的柱廊 → 前庭三尊哨兵 → 内殿残像 → 后殿宝库 → 北侧门或原路离开。
"""
from __future__ import annotations

import random

from buildgen import Building

W_X, W_Z = 31, 21
CZ = 10  # 中轴（z）

# 正式方块（T22 阶段 ②；白模时用圆石台阶、木桶代替）：碎石、残缺神像、宝库
RUBBLE = "mythic_age:marble_rubble"
STATUE = "mythic_age:broken_greek_statue"
VAULT = "mythic_age:ruin_vault"
FACINGS = ("north", "east", "south", "west")

# 每根柱子剩多高：-1 = 完全没了（只剩台面），0 = 只剩柱础，1..3 = 柱础 + 几段柱身，5 = 完整（柱础 + 3 段 + 柱头）
# 东立面（x = 22）只有 z = 6、14 两根完整：挡住前庭哨兵看向内殿门的斜线（合同：前庭至少 2 根完整柱子）
COLUMNS = {
    # 东立面
    (22, 2): 1, (22, 6): 5, (22, 10): -1, (22, 14): 5, (22, 18): 2,
    # 北侧
    (2, 2): 2, (6, 2): 5, (10, 2): 5, (14, 2): 0, (18, 2): 5,
    # 南侧
    (2, 18): 5, (6, 18): 2, (10, 18): 5, (14, 18): 1, (18, 18): 0,
    # 西立面
    (2, 6): 3, (2, 10): 5, (2, 14): 0,
}

# 内殿外墙每格剩多高（y 2 起算的格数，0 = 塌到地面）；后殿那段（x 5..9）完整，屋顶也在
NORTH_WALL = [5, 5, 5, 5, 5, 4, 3, 2, 1, 1, 2, 3, 3, 4, 5]          # x 5..19
SOUTH_WALL = [5, 5, 5, 5, 5, 4, 3, 3, 4, 2, 0, 1, 3, 4, 5]          # x 5..19（x = 15 塌出一个缺口）
EAST_WALL = [3, 4, 5, 5, 0, 0, 0, 5, 5, 4, 3]                      # z 5..15（z 9..11 是门）


def column(b: Building, x: int, z: int, keep: int, top: int):
    if keep < 0:
        return
    b.set(x, top, z, "col_base")
    for i in range(min(keep, 3)):
        b.set(x, top + 1 + i, z, "col_shaft")
    if keep >= 5:
        b.set(x, top + 4, z, "doric")


def fallen_drum(b: Building, x: int, y: int, z: int, length: int, axis: str):
    """倒地的柱身：横躺的大理石柱段。"""
    for i in range(length):
        if axis == "x":
            b.set(x + i, y, z, "mythic_age:marble_pillar", axis="x")
        else:
            b.set(x, y, z + i, "mythic_age:marble_pillar", axis="z")


def abandoned_temple() -> Building:
    b = Building("greek_abandoned_temple", "废弃神殿（ST-GR-02）")
    rnd = random.Random(2202)
    top = b.crepidoma(0, 0, 23, 20, 2)  # 台面 y = 2；台面方块 x 1..22、z 1..19
    # 台阶残缺：东、南两面最下一级缺几块，露出地面
    for x, z in ((3, 20), (4, 20), (11, 20), (17, 20), (23, 4), (23, 16), (23, 17)):
        b.blocks[(x, 0, z)] = "minecraft:air"
    # 正门前的踏步（z 8..12）：前庭地面 → 最下一级 → 台面，不用跳
    for z in range(CZ - 2, CZ + 3):
        b.set(24, 0, z, "step_stair", facing="west")
        b.set(23, 1, z, "step_stair", facing="west")
    # ── 围柱 ──
    for (x, z), keep in COLUMNS.items():
        column(b, x, z, keep, top)
    # 额枋只留在还立着的柱子之间（中间那根倒了的，上面的额枋也一起塌了）：
    # 东面两根完整柱各顶着一截断掉的额枋；北侧 x = 6、10 两根相邻的完整柱之间架着一整段，上面还有一截檐口
    for z in (5, 6, 7, 13, 14, 15):
        b.set(22, top + 5, z, "architrave", facing="east")
    for x in range(6, 11):
        b.set(x, top + 5, 2, "architrave", facing="north")
    for x in range(6, 9):
        b.set(x, top + 6, 2, "cornice", facing="north")
    # ── 内殿与后殿 ──
    x0, x1, z0, z1 = 5, 19, 5, 15
    for i, h in enumerate(NORTH_WALL):
        for y in range(h):
            b.set(x0 + i, top + y, z0, "wall_marble")
    for i, h in enumerate(SOUTH_WALL):
        for y in range(h):
            b.set(x0 + i, top + y, z1, "wall_marble")
    for i, h in enumerate(EAST_WALL):
        for y in range(h):
            b.set(x1, top + y, z0 + i, "wall_marble")
    for z in range(z0, z1 + 1):
        for y in range(5):
            b.set(x0, top + y, z, "wall_marble")   # 西墙（后殿背墙）完整
            b.set(9, top + y, z, "wall_marble")    # 后殿与内殿的隔墙
    # 隔墙中间一道门（内殿 → 后殿），北墙后殿一段开侧门
    for y in (top, top + 1):
        b.blocks[(9, y, CZ)] = "minecraft:air"
        b.blocks[(7, y, z0)] = "minecraft:air"
    # 后殿顶：大理石板天花 + 一角残存的赤陶瓦
    b.fill(x0, top + 5, z0, 9, top + 5, z1, "smooth_slab", type="bottom")
    for x in range(x0, 10):
        for z in range(z0, 10):
            b.set(x, top + 6, z, "roof_slab", type="bottom")
    # 内殿：神像在西端（面朝东门），台座 1 格
    b.set(11, top, CZ, "floor")
    b.set(11, top + 1, CZ, STATUE, half="lower", facing="east")
    b.set(11, top + 2, CZ, STATUE, half="upper", facing="east")
    b.anchor("statue", 11, top + 1, CZ)
    # 后殿：宝库靠西墙正中，两侧各一个普通箱子
    b.set(6, top, CZ, VAULT, facing="east")
    b.anchor("vault", 6, top, CZ)
    for z in (7, 13):
        b.set(6, top, z, "minecraft:chest", facing="east")
        b.anchor("loot", 6, top, z)
    b.anchor("exit", 7, top, z0 - 2)
    # ── 倒塌物 ──
    fallen_drum(b, 9, top, 17, 3, "x")        # 南侧回廊：一根倒下的柱子（留出 z = 16 一格通道）
    fallen_drum(b, 15, top, 3, 2, "x")        # 北侧回廊：两段柱身
    fallen_drum(b, 24, 0, 1, 3, "z")          # 前庭北角：东北角那根倒向前庭
    b.set(24, 0, 4, "doric")                  # 摔下来的柱头
    fallen_drum(b, 27, 0, 18, 3, "x")         # 前庭南边
    # 内殿地面的碎块与塌下来的瓦（不挡门、不挡神像前）
    for _ in range(26):
        x, z = rnd.randint(10, 18), rnd.randint(6, 14)
        if (x, z) in ((11, CZ), (10, CZ), (18, CZ), (18, CZ - 1), (18, CZ + 1)) or abs(z - CZ) <= 1:
            continue
        kind = rnd.choice([RUBBLE, RUBBLE, "roof_slab", "smooth_slab"])
        if kind == RUBBLE:
            b.set(x, top, z, RUBBLE, facing=rnd.choice(FACINGS))
        else:
            b.set(x, top, z, kind, type="bottom")
    # 围柱回廊与台基上的碎块
    for _ in range(18):
        x, z = rnd.randint(1, 22), rnd.choice([1, 19, rnd.randint(1, 19)])
        if (x, top, z) in b.blocks or 5 <= x <= 19 and 5 <= z <= 15:
            continue
        b.set(x, top, z, RUBBLE, facing=rnd.choice(FACINGS))
    # 青苔与杂草：台基上零星的苔藓地毯
    for _ in range(30):
        x, z = rnd.randint(1, 22), rnd.randint(1, 19)
        if (x, top, z) not in b.blocks and not (5 <= x <= 19 and 5 <= z <= 15):
            b.set(x, top, z, "minecraft:moss_carpet")
    # ── 前庭（x 24..30）：残缺的多边形石铺地、露天祭坛残迹、三尊哨兵的基座 ──
    for x in range(24, W_X):
        for z in range(0, W_Z):
            r = rnd.random()
            b.set(x, -1, z, "polygonal" if r < 0.55 else "gravel" if r < 0.7 else "grass")
    b.fill(27, 0, 9, 27, 0, 11, "floor")      # 祭坛：只剩台座与一块残板
    b.set(27, 1, 10, "smooth_slab", type="bottom")
    for (x, z) in ((26, 4), (26, 16), (29, 10)):
        b.set(x, 0, z, "floor")
        b.anchor("guardian", x, 1, z)
    b.anchor("entrance", W_X - 1, 0, CZ)
    return b


ALL = [abandoned_temple]
