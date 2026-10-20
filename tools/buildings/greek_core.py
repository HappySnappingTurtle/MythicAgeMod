#!/usr/bin/env python3
"""希腊村庄第 2 批白模：广场、泉屋、坡地小屋、谷仓（V3-希腊-建筑 §2.1、§2.2）。

坐标同 buildgen：x 向东、z 向南，原点为占地西北角、地面一层（y = 0 是地面上第一层）。
  广场 agora 15 × 15：卵石马赛克地面、大理石镶边；中央石座上的聚会钟（村庄中心锚点）；四边石凳；西北角一株橄榄树；
                     四个路口各一根赫尔墨斯界柱（herm）；四角油灯柱。
  泉屋 krēnē 7 × 6：西墙出水口，墙前蓄水池；东面两根多立克柱撑起小山花顶。
  坡地小屋 7 × 9：北侧两间屋（炉灶间 + 卧房）、南侧前院矮墙；屋门朝院子开，临街墙只开高窗。
  谷仓 9 × 7：赭色勒脚 + 泥砖墙、两坡赤陶瓦顶；陶瓮沿墙成排（逻辑库存的货堆显示位）；东侧双开门。

运行：python3 tools/buildings/greek_core.py [--world <存档目录>]
"""
from __future__ import annotations

import sys
from pathlib import Path

from buildgen import Building, ROOT, write_datapack

OUT = ROOT / "docs/重构/开发/TV/白模/希腊/第2批_广场与民居"


def door(b: Building, x, y, z, facing, hinge="left"):
    b.set(x, y, z, "door", facing=facing, half="lower", hinge=hinge, open="false")
    b.set(x, y + 1, z, "door", facing=facing, half="upper", hinge=hinge, open="false")


def bed(b: Building, x, y, z, facing):
    """facing 为床头朝向；床头在 (x, z)，床尾在相反一格。"""
    dx, dz = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}[facing]
    b.set(x, y, z, "bed", facing=facing, part="head")
    b.set(x + dx, y, z + dz, "bed", facing=facing, part="foot")


def house_roof(b: Building, x0, x1, z0, z1, y):
    """民居屋顶：四坡赤陶瓦，四面各挑出 1 格（没有山墙，不会在屋面上露出墙块）。"""
    b.hip_roof(x0 - 1, x1 + 1, z0 - 1, z1 + 1, y)


def agora() -> Building:
    b = Building("greek_agora", "广场（agora）")
    n = 15
    for x in range(n):
        for z in range(n):
            b.set(x, 0, z, "step" if x in (0, n - 1) or z in (0, n - 1) else "mosaic")
    c = n // 2
    # 中央：两级石座 + 聚会钟（村庄中心）
    b.fill(c - 1, 0, c - 1, c + 1, 0, c + 1, "floor")
    b.set(c, 1, c, "step")
    b.set(c, 2, c, "minecraft:bell", attachment="floor", facing="east")
    b.anchor("center", c, 2, c)
    # 四边石凳（靠背朝外）
    for i in (3, 4, 10, 11):
        b.set(i, 1, 1, MA_STAIRS, facing="north")
        b.set(i, 1, n - 2, MA_STAIRS, facing="south")
        b.set(1, 1, i, MA_STAIRS, facing="west")
        b.set(n - 2, 1, i, MA_STAIRS, facing="east")
    # 西北角橄榄树
    tx, tz = 3, 3
    for y in range(1, 4):
        b.set(tx, y, tz, "beam", axis="y")
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for y in (4, 5):
                if abs(dx) + abs(dz) <= (3 if y == 4 else 2):
                    b.set(tx + dx, y, tz + dz, "mythic_age:olive_leaves", persistent="true")
    # 四个路口的赫尔墨斯界柱（方柱 + 顶块）
    for (hx, hz) in ((c, 0), (c, n - 1), (0, c), (n - 1, c)):
        for y in (1, 2):
            b.set(hx, y, hz, "mythic_age:marble_wall")
        b.set(hx, 3, hz, "mythic_age:polished_marble")
    # 四角油灯柱
    for (lx, lz) in ((1, 1), (1, n - 2), (n - 2, 1), (n - 2, n - 2)):
        b.set(lx, 1, lz, "mythic_age:marble_wall")
        b.set(lx, 2, lz, "lamp")
    b.anchor("entrance", c, 0, n - 1)
    return b


def krene() -> Building:
    b = Building("greek_krene", "泉屋（krēnē）")
    # 地坪
    b.fill(0, 0, 0, 6, 0, 5, "step")
    # 西墙（出水墙）与南北矮墙
    b.fill(0, 1, 0, 0, 4, 5, "wall_marble")
    b.set(0, 2, 2, "meander_blue")
    b.set(0, 2, 3, "meander_blue")
    b.set(1, 3, 2, "mythic_age:marble_wall")  # 出水口（正式版：狮头出水口）
    b.set(1, 3, 3, "mythic_age:marble_wall")
    for z in (0, 5):
        b.fill(1, 1, z, 4, 1, z, "wall_marble")
    # 蓄水池：池底下沉一格，池沿是台阶
    b.fill(1, -1, 1, 3, -1, 4, "step")
    b.fill(1, 0, 1, 3, 0, 4, "minecraft:water")
    for z in range(1, 5):
        b.set(4, 0, z, "smooth_slab", type="top")
    # 东面两根柱 + 柱子与西墙之间的额枋
    for z in (0, 5):
        b.column(6, 1, z, 2, "doric")
        for x in range(1, 6):
            b.set(x, 4, z, "wall_marble")
    for z in range(0, 6):
        b.set(6, 5, z, "architrave", facing="east")
        b.set(0, 5, z, "wall_marble")
    for x in range(1, 6):
        b.set(x, 5, 0, "architrave", facing="north")
        b.set(x, 5, 5, "architrave", facing="south")
    b.fill(1, 5, 1, 5, 5, 4, "floor")
    b.gable_roof(0, 5, 0, 5, 6)
    b.pediment_gable(6, 0, 5, 6, "east")
    b.pediment_gable(0, 0, 5, 6, "west")
    b.anchor("water", 4, 0, 2)
    return b


def house_small() -> Building:
    b = Building("greek_house_small", "坡地小屋")
    x0, x1, z0, z1 = 0, 6, 0, 5  # 屋身
    b.fill(x0, -1, z0, x1, -1, 8, "step")  # 地基垫层（坡地时由台地工序接上）
    b.hollow(x0, 0, z0, x1, 0, z1, "socle")
    b.hollow(x0, 1, z0, x1, 2, z1, "plaster")
    b.fill(x0 + 1, -1, z0 + 1, x1 - 1, -1, z1 - 1, "mythic_age:polygonal_marble")
    # 隔墙：西间卧房、东间炉灶间，中间门洞
    b.fill(3, 0, z0 + 1, 3, 2, z1 - 1, "plaster")
    b.clear(3, 0, 2, 3, 1, 2)
    # 梁圈 + 顶棚
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            b.set(x, 3, z, "beam", axis="x")
    for z in range(z0 + 1, z1):
        for x in (x0, x1):
            b.set(x, 3, z, "beam", axis="z")
    b.fill(x0 + 1, 3, z0 + 1, x1 - 1, 3, z1 - 1, "plank_slab", type="top")
    house_roof(b, x0, x1, z0, z1, 4)
    # 屋门朝南开向前院；临街（北）墙只开一扇高窗
    b.clear(4, 0, z1, 4, 1, z1)
    door(b, 4, 0, z1, "north")
    b.set(1, 2, z0, "shutter", facing="north", half="top", open="true")
    b.set(x1, 2, 3, "shutter", facing="east", half="top", open="true")
    # 前院：矮墙（勒脚 + 一层白灰），院门在南墙正中
    for x in range(x0, x1 + 1):
        b.set(x, 0, 8, "socle")
        b.set(x, 1, 8, "plaster")
    for z in (6, 7):
        for x in (x0, x1):
            b.set(x, 0, z, "socle")
            b.set(x, 1, z, "plaster")
    b.clear(3, 0, 8, 3, 1, 8)  # 院门位（正式版用栅栏门）
    # 室内
    bed(b, 1, 0, 1, "north")
    bed(b, 2, 0, 1, "north")
    b.set(5, 0, 1, "minecraft:campfire", lit="true")  # 炉灶
    b.set(5, 0, 4, "pithos")
    b.set(5, 1, 4, "lamp")  # 油灯放在陶瓮上
    b.set(1, 0, 6, "pithos")
    b.set(5, 0, 7, "pithos")
    b.anchor("home", 1, 0, 2)
    b.anchor("home", 2, 0, 2)
    b.anchor("entrance", 3, 0, 8)
    return b


def granary() -> Building:
    b = Building("greek_granary", "谷仓")
    x0, x1, z0, z1 = 0, 8, 0, 6
    b.fill(x0, -1, z0, x1, -1, z1, "step")
    b.hollow(x0, 0, z0, x1, 0, z1, "socle")
    b.hollow(x0, 1, z0, x1, 3, z1, "mudbrick")
    b.fill(x0 + 1, 0, z0 + 1, x1 - 1, 0, z1 - 1, "minecraft:air")
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            b.set(x, 4, z, "beam", axis="x")
    for z in range(z0 + 1, z1):
        for x in (x0, x1):
            b.set(x, 4, z, "beam", axis="z")
    b.fill(x0 + 1, 4, z0 + 1, x1 - 1, 4, z1 - 1, "plank_slab", type="top")
    house_roof(b, x0, x1, z0, z1, 5)
    # 东侧双开门
    b.clear(x1, 0, 2, x1, 2, 4)
    door(b, x1, 0, 3, "west", hinge="left")
    door(b, x1, 0, 4, "west", hinge="right")
    b.set(x1, 0, 2, "socle")
    b.set(x1, 1, 2, "mudbrick")
    b.set(x1, 2, 2, "mudbrick")
    # 陶瓮沿南北墙成排（货堆显示位）
    for x in range(1, x1):
        for z in (1, z1 - 1):
            b.set(x, 0, z, "pithos")
            b.anchor("goods", x, 0, z)
    b.set(1, 0, 3, "pithos")
    b.set(1, 1, 3, "pithos")
    # 高窗
    for x in (2, 6):
        b.set(x, 3, z0, "shutter", facing="north", half="top", open="true")
        b.set(x, 3, z1, "shutter", facing="south", half="top", open="true")
    b.set(1, 2, 3, "lamp")  # 油灯放在叠起的陶瓮上
    b.anchor("storage_keeper", 6, 0, 3)
    return b


MA_STAIRS = "mythic_age:marble_stairs"
BUILDERS = (agora, krene, house_small, granary)


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
