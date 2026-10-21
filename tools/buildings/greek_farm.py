#!/usr/bin/env python3
"""希腊村庄第 3 批白模：橄榄园、葡萄园、麦田与打谷场、陶坊、羊圈（V3-希腊-建筑 §2.2、§2.3）。

坐标同 buildgen：x 向东、z 向南，原点为占地西北角，y = 0 是地面上第一层（y = -1 为地面层本身，可替换成耕地、路面）。
  橄榄园 15 × 15：干砌石矮墙围起，8 株橄榄树（矮粗的树干、分杈、扁平的树冠，株间留空隙，挂果）；东南角压榨棚（橄榄压榨器）。
  葡萄园 11 × 13：四行三格高葡萄架（结果状态），行间留 2 格走道；矮木栅栏围边。
  麦田与打谷场 15 × 11：西侧 8 × 11 麦田（成熟小麦、中间一条水渠）；东侧圆形打谷场（石砌边、铺石地面）与草垛。
  陶坊 9 × 9：北侧带四坡顶的工棚（陶轮、黏土池、晾坯架），院里一座泥砖窑（前有炉口、顶上冒烟），矮墙院门朝南。
  羊圈 9 × 9：干砌石矮墙、南侧木门；北侧单坡棚子、草垛、水槽；3 只羊。
锚点：fruit_tree（采收点）、trellis、crop、thresh、workstation（工作站）、clay、pen（牲畜）。

运行：python3 tools/buildings/greek_farm.py [--world <存档目录>]
"""
from __future__ import annotations

import sys
from pathlib import Path

from buildgen import Building, ROOT, write_datapack

OUT = ROOT / "docs/重构/开发/TV/白模/希腊/第3批_农园与作坊"
WALL = "mythic_age:polygonal_marble_wall"  # 干砌石矮墙（白模用多边形砌石墙）


def olive_tree(b: Building, x, z, seed):
    """一株老橄榄树：两格粗短树干，第 3 格向两侧分杈，树冠扁平宽大（半径 2–3，两层），挂果。"""
    b.set(x, 0, z, "beam", axis="y")
    b.set(x, 1, z, "beam", axis="y")
    dx, dz = ((1, 0), (0, 1), (-1, 0), (0, -1))[seed % 4]
    b.set(x + dx, 2, z + dz, "beam", axis="x" if dx else "z")
    b.set(x - dx, 2, z - dz, "beam", axis="x" if dx else "z")
    b.set(x, 2, z, "beam", axis="y")
    leaves = "mythic_age:olive_leaves"
    # 树冠：下层半径 2 的菱形（每株缺 1–2 个角，形状不一）、上层半径 1；株与株之间留出空隙
    missing = {((2, 0), (0, -2)), ((-2, 0),), ((0, 2), (2, 0)), ((0, -2),)}
    skip = list(missing)[seed % 4]
    for ox in range(-2, 3):
        for oz in range(-2, 3):
            r = abs(ox) + abs(oz)
            if r <= 2 and (ox, oz) not in skip:
                b.set(x + ox, 3, z + oz, leaves, persistent="true", fruit=2 if (ox + oz + seed) % 3 == 0 else 0)
            if r <= 1:
                b.set(x + ox, 4, z + oz, leaves, persistent="true", fruit=2 if (ox * oz + seed) % 4 == 0 else 0)
    b.anchor("fruit_tree", x, 0, z)


def boundary(b: Building, x0, z0, x1, z1, block, gate=None):
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            b.set(x, 0, z, block)
    for z in range(z0, z1 + 1):
        for x in (x0, x1):
            b.set(x, 0, z, block)
    if gate:
        gx, gz = gate
        b.clear(gx, 0, gz, gx, 0, gz)


def olive_grove() -> Building:
    b = Building("greek_olive_grove", "橄榄园")
    b.fill(0, -1, 0, 14, -1, 14, "minecraft:coarse_dirt")
    for (x, z) in ((3, 3), (8, 3), (13, 3), (3, 8), (8, 8), (13, 8), (3, 13), (8, 13)):
        b.fill(x - 1, -1, z - 1, x + 1, -1, z + 1, "minecraft:grass_block")
    boundary(b, 0, 0, 14, 14, WALL, gate=(7, 14))
    trees = [(3, 3), (8, 3), (13, 3), (3, 8), (8, 8), (13, 8), (3, 13), (8, 13)]
    for i, (x, z) in enumerate(trees):
        olive_tree(b, x, z, i)
    # 东南角压榨棚：四根橄榄木柱 + 木板顶，棚下压榨器和陶瓮
    for (x, z) in ((11, 11), (13, 11), (11, 13), (13, 13)):
        b.fill(x, 0, z, x, 2, z, "fence")
    b.fill(10, 3, 10, 14, 3, 14, "plank_slab", type="bottom")
    b.fill(11, -1, 11, 13, -1, 13, "step")
    b.set(12, 0, 12, "mythic_age:olive_press", facing="north")
    b.set(11, 0, 12, "pithos")
    b.set(13, 0, 12, "pithos")
    b.anchor("workstation", 12, 0, 11)
    return b


def vineyard() -> Building:
    b = Building("greek_vineyard", "葡萄园")
    b.fill(0, -1, 0, 10, -1, 12, "minecraft:coarse_dirt")
    for z in (2, 5, 8, 11):
        b.fill(1, -1, z, 9, -1, z, "minecraft:grass_block")
    boundary(b, 0, 0, 10, 12, "fence", gate=(5, 12))
    trellis = "mythic_age:grape_trellis"
    for z in (2, 5, 8):
        for x in range(1, 10):
            b.set(x, 0, z, trellis, stage=4, axis="x", part="bottom")
            b.set(x, 1, z, trellis, stage=4, axis="x", part="middle")
            b.set(x, 2, z, trellis, stage=4, axis="x", part="top")
            b.anchor("trellis", x, 0, z + 1)
    b.set(9, 0, 10, "pithos")
    b.set(8, 0, 10, "pithos")
    return b


def wheat_field() -> Building:
    b = Building("greek_wheat_field", "麦田与打谷场")
    # 西侧麦田 8 × 11，中间一条水渠（z = 5）
    for x in range(0, 8):
        for z in range(0, 11):
            if z == 5:
                b.set(x, -1, z, "minecraft:water")
            else:
                b.set(x, -1, z, "minecraft:farmland", moisture=7)
                b.set(x, 0, z, "minecraft:wheat", age=7)
                b.anchor("crop", x, 0, z)
    # 东侧圆形打谷场：直径 7（x 8..14、z 2..8），石砌边、铺石地面
    cx, cz, r = 11, 5, 3.5
    for x in range(8, 15):
        for z in range(1, 10):
            d = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
            if d <= r - 0.6:
                b.set(x, -1, z, "mythic_age:threshing_floor")
            elif d <= r + 0.4:
                b.set(x, -1, z, "step")
                b.set(x, 0, z, "smooth_slab", type="bottom")
    b.anchor("thresh", cx, 0, cz)
    for (x, z) in ((14, 0), (14, 10), (13, 10)):
        b.set(x, 0, z, "minecraft:hay_block", axis="y")
    b.set(14, 1, 0, "minecraft:hay_block", axis="x")
    return b


def pottery() -> Building:
    b = Building("greek_pottery", "陶坊")
    b.fill(0, -1, 0, 8, -1, 8, "minecraft:packed_mud")
    # 院墙（勒脚 + 一层白灰），院门朝南
    for x in range(0, 9):
        for z in (0, 8):
            b.set(x, 0, z, "socle")
            b.set(x, 1, z, "plaster")
    for z in range(0, 9):
        for x in (0, 8):
            b.set(x, 0, z, "socle")
            b.set(x, 1, z, "plaster")
    b.clear(4, 0, 8, 4, 1, 8)
    # 北侧工棚：北墙到 3 格高，南面敞开由木柱撑起，四坡瓦顶
    b.fill(1, 0, 1, 7, 2, 1, "plaster")
    for x in (1, 4, 7):
        b.fill(x, 0, 3, x, 2, 3, "beam", axis="y")
    for x in range(0, 9):
        b.set(x, 3, 1, "beam", axis="x")
        b.set(x, 3, 3, "beam", axis="x")
    b.fill(1, 3, 2, 7, 3, 2, "plank_slab", type="top")
    b.hip_roof(-1, 9, 0, 4, 4)
    # 棚下：陶轮、黏土池、晾坯架
    b.set(3, 0, 2, "mythic_age:pottery_wheel", facing="south")
    b.anchor("workstation", 3, 0, 3)
    b.fill(5, -1, 2, 6, -1, 2, "minecraft:clay")
    b.set(5, 0, 2, "smooth_slab", type="bottom")
    b.anchor("clay", 5, 0, 3)
    for x in (1, 2):
        b.set(x, 1, 2, "plank_slab", type="top")
        b.set(x, 2, 2, "pithos")
    # 院里的陶窑（两格高、正在烧），旁边一堆柴
    b.set(6, 0, 6, "mythic_age:pottery_kiln", facing="south", half="lower", lit="true")
    b.set(6, 1, 6, "mythic_age:pottery_kiln", facing="south", half="upper", lit="true")
    b.set(7, 0, 5, "beam", axis="x")
    b.set(7, 0, 6, "beam", axis="x")
    b.anchor("workstation", 6, 0, 7)
    # 院里成品陶罐
    for (x, z) in ((1, 5), (2, 6), (1, 7)):
        b.set(x, 0, z, "pithos")
    return b


def sheep_pen() -> Building:
    b = Building("greek_pen", "羊圈")
    boundary(b, 0, 0, 8, 8, WALL)
    b.clear(4, 0, 8, 4, 0, 8)
    b.set(4, 0, 8, "mythic_age:olive_fence_gate", facing="south")
    # 北侧单坡棚
    for x in (1, 4, 7):
        b.fill(x, 0, 3, x, 1, 3, "fence")
    b.fill(1, 2, 1, 7, 2, 3, "plank_slab", type="bottom")
    b.fill(1, 3, 1, 7, 3, 1, "plank_slab", type="bottom")
    b.set(1, 0, 1, "minecraft:hay_block", axis="y")
    b.set(2, 0, 1, "minecraft:hay_block", axis="y")
    b.set(7, 0, 6, "minecraft:water_cauldron", level=3)
    b.anchor("pen", 4, 0, 5)
    for (x, z) in ((3, 5), (5, 6), (2, 4)):
        b.commands.append(f"summon sheep ~{x + 0.5} ~0 ~{z + 0.5} {{NoAI:1b,PersistenceRequired:1b}}")
    return b


BUILDERS = (olive_grove, vineyard, wheat_field, pottery, sheep_pen)


def main():
    buildings = [f() for f in BUILDERS]
    OUT.mkdir(parents=True, exist_ok=True)
    for b in buildings:
        p = b.layer_plans(OUT / f"{b.name}_逐层.png", cell=18)
        print(f"{b.title}：{len(b.blocks)} 方块，平面图 {p.relative_to(ROOT)}")
    if "--world" in sys.argv:
        world = Path(sys.argv[sys.argv.index("--world") + 1])
        print("数据包：", write_datapack(world, buildings))


if __name__ == "__main__":
    main()
