#!/usr/bin/env python3
"""希腊村庄第 6 批白模（上）：主神差异件（V3-希腊-建筑 §5）。出处见白模篇第 6 批「出处」表。

坐标同 buildgen：x 向东、z 向南，原点为占地西北角，y = 0 是地面上第一层（y = -1 是地表）。
  宙斯·多多那圣橡 17 × 17：老橡树居中，树下一圈首尾相接的铜鼎（三足鼎上的大鼎，敲一只全圈共鸣）；
        跟村庄阶段走（2026-10-19 用户定）：远古只有圣橡 + 铜鼎圈（greek_zeus_oak_archaic），
        古典起加上前 4 世纪那道南面开门的矮围墙与门柱火盆（greek_zeus_oak）。
  波塞冬·码头与渔船棚 19 × 21（临海）：驳岸、伸进海里的石堤（泊位）、成对的船棚（后墙封闭、中间一排柱子、
        两坡顶，船台斜着伸进水里，中线是木质龙骨槽）；一条渔船拖上船台、一条泊在水里。
  波塞冬·海神泉 11 × 9（不临海）：泉屋扩大——三个出水口的蓄水池、爱奥尼前廊；前廊北端地面留三个孔
        （三叉戟印，孔下是盐泉），正上方屋顶留天窗。
  哈迪斯·柏树墓园 21 × 16：路边墓园——一排柏树、一条路，路南三块家族墓园（peribolos）：临路的高墙（黑石勒脚 +
        大理石方石）顶上立墓碑：小神庙式墓碑（naiskos）、高墓碑与大理石油瓶、封土墓。

运行：python3 tools/buildings/greek_patron.py [--world <存档目录>]
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from buildgen import Building, ROOT, write_datapack

OUT = ROOT / "docs/重构/开发/TV/白模/希腊/第6批_主神差异件与地形片段"
SEA = "minecraft:water"
BASE = "minecraft:dirt"  # 白模用的填土 / 海床（高空平台下面是空的，水要自己围住）


def _free(b: Building, x, y, z):
    return (x, y, z) not in b.blocks


# ───────────── 宙斯 ─────────────

def zeus_oak(walled=True) -> Building:
    b = Building("greek_zeus_oak" if walled else "greek_zeus_oak_archaic",
                 "宙斯·多多那圣橡（古典起）" if walled else "宙斯·多多那圣橡（远古）")
    n, c = 16, 8
    # 地面：树下踩实的粗土
    for x in range(1, n):
        for z in range(1, n):
            if math.hypot(x - c, z - c) <= 7.2:
                b.set(x, -1, z, "minecraft:coarse_dirt")
    # 矮围墙（前 4 世纪取代铜鼎圈的那道墙），只有半人高，从外面看得见铜鼎；南门两侧门柱上放火盆
    if walled:
        for i in range(0, n + 1):
            for (x, z) in ((i, 0), (i, n), (0, i), (n, i)):
                b.set(x, 0, z, "polygonal_wall")
        b.clear(c - 1, 0, n, c + 1, 0, n)
        for x in (c - 2, c + 2):
            b.fill(x, 0, n, x, 1, n, "polygonal")
            b.set(x, 2, n, "brazier")
    # 树干：根部加粗（十字形 + 斜向根），主干粗两格到 y 6，往上单根，四面分杈
    for (dx, dz) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (1, 1)):
        for y in (0, 1, 2):
            b.set(c + dx, y, c + dz, "minecraft:oak_wood", axis="y")
    for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2), (-1, -1), (1, -1), (-1, 1)):
        b.set(c + dx, 0, c + dz, "minecraft:oak_wood", axis="y")
    for y in range(3, 10):
        b.set(c, y, c, "minecraft:oak_log", axis="y")
    for y in range(3, 7):
        b.set(c + 1, y, c, "minecraft:oak_log", axis="y")
        b.set(c, y, c + 1, "minecraft:oak_log", axis="y")
    for (dx, dz, y) in ((-1, 0, 6), (-2, 0, 6), (-3, 0, 7), (-4, 0, 7), (2, 0, 6), (3, 0, 7), (4, 0, 8),
                        (0, -1, 7), (0, -2, 7), (0, -3, 8), (0, 2, 7), (0, 3, 7), (0, 4, 8),
                        (-1, -1, 8), (-2, -2, 8), (-3, -3, 9), (2, 2, 8), (3, 3, 9), (2, -2, 9), (-2, 2, 9)):
        b.set(c + dx, y, c + dz, "minecraft:oak_log", axis="x" if dz == 0 else "z" if dx == 0 else "y")
    # 树冠：宽而低的伞形（半径 6.5），层间半径不同，用散列挖掉边缘一些格子让轮廓不规整
    radius = {6: 3.5, 7: 5.3, 8: 6.4, 9: 6.6, 10: 5.6, 11: 4.0, 12: 2.0}
    for y, r in radius.items():
        for x in range(c - 7, c + 8):
            for z in range(c - 7, c + 8):
                d = math.hypot(x - c, z - c)
                if d <= r and _free(b, x, y, z) and not (d > r - 1.3 and (x * 7 + z * 13 + y * 5) % 4 == 0):
                    b.set(x, y, z, "minecraft:oak_leaves", persistent="true")
    # 铜鼎圈：半径 6 的一圈首尾相接的三足铜鼎，南面留口
    for x in range(1, n):
        for z in range(1, n):
            d = math.hypot(x - c, z - c)
            if 5.5 <= d < 6.5 and not (z > c and abs(x - c) <= 1):
                b.set(x, 0, z, "mythic_age:bronze_tripod", facing="north")
    b.anchor("sacred_tree", c, 0, c + 3)
    b.anchor("sacred_tree", c, 0, c - 3)
    return b


# ───────────── 波塞冬 ─────────────

def _gable_roof_z(b: Building, x0, x1, z0, z1, y):
    """南北走向的两坡屋顶（屋脊沿 z），1:2 缓坡。"""
    width = x1 - x0 + 1
    half = (width - 1) / 2
    for x in range(x0, x1 + 1):
        d = min(x - x0, x1 - x)
        for z in range(z0, z1 + 1):
            if width % 2 == 1 and d == half:
                b.set(x, y + d // 2, z, "ridge", axis="z")
            else:
                b.set(x, y + d // 2, z, "roof_slab", type="bottom" if d % 2 == 0 else "top")


def poseidon_harbor(natural=False) -> Building:
    """natural=True：正式模板用，不带白模的海床、填土与围边，海水用世界里真实的海。"""
    b = Building("greek_poseidon_harbor", "波塞冬·码头与渔船棚")
    X, SHORE, Z = 18, 8, 20  # 宽 0..18；z < 8 是陆地，z ≥ 8 是海
    surface = -2  # 水面比地面低 1 格，驳岸高出水面
    if not natural:
        _white_base(b, X, SHORE, Z, surface)
    _harbor_body(b, X, SHORE, Z, surface)
    return b


def _white_base(b: Building, X, SHORE, Z, surface):
    """白模底座：海床与陆地填土（高空平台下面是空的，水要自己围住）。"""
    b.fill(-1, -5, -1, X + 1, -5, Z + 1, BASE)
    b.fill(0, -4, 0, X, -2, SHORE - 1, BASE)
    b.fill(0, -1, 0, X, -1, SHORE - 1, "grass")
    for x in range(-1, X + 2):
        b.fill(x, -4, Z + 1, x, -1, Z + 1, BASE)
    for z in range(SHORE, Z + 1):
        b.fill(-1, -4, z, -1, -1, z, BASE)
        b.fill(X + 1, -4, z, X + 1, -1, z, BASE)
    for x in range(-1, X + 2):  # 白模底座的围边顶面铺草，看起来像岸
        b.set(x, -1, Z + 1, "grass")
    for z in range(SHORE, Z + 1):
        b.set(-1, -1, z, "grass")
        b.set(X + 1, -1, z, "grass")
    b.fill(0, -4, SHORE, X, surface, Z, SEA)
    b.clear(0, surface + 1, SHORE, X, -1, Z)  # 海面上方掀掉平台草皮


def _harbor_body(b: Building, X, SHORE, Z, surface):
    # 驳岸：大理石方石砌到水底，顶面与地面平
    b.fill(0, -4, SHORE, X, -1, SHORE, "step")
    # 石堤：从岸边伸进海里，两侧系船石，堤头是泊位
    b.fill(13, -4, SHORE, 15, -1, 18, "step")
    for z in (10, 13, 16):
        for x in (13, 15):
            b.set(x, 0, z, "mythic_age:marble_wall")
    b.anchor("berth", 16, surface, 17)
    # 船棚：两条船台成对，后墙封闭，中间一排柱子，两侧实墙，共用一个两坡顶，前端伸到水上
    x0, mid, x1, back, front = 0, 6, 12, 0, 10
    for slip in ((x0 + 1, mid - 1), (mid + 1, x1 - 1)):
        for z in range(back + 1, 13):
            top = -1 - (z - 1) // 3  # 船台每 3 格降 1 格，伸进水里
            for x in range(slip[0], slip[1] + 1):
                b.fill(x, -4, z, x, top - 1, z, BASE if z < SHORE else "step")
                keel = x == (slip[0] + slip[1]) // 2
                b.set(x, top, z, "planks" if keel else "floor")
                for y in range(top + 1, 0):
                    b.set(x, y, z, SEA if y <= surface else "minecraft:air")
    # 中间的石台（船台之间的走道，一直伸到水里），柱子立在上面
    b.fill(mid, -4, back, mid, -1, 12, "step")
    for z in range(back + 1, front + 1, 2):
        b.column(mid, 0, z, 2, "doric")
    # 两侧实墙 + 后墙
    for x in (x0, x1):
        b.fill(x, -4, back, x, 3, front, "wall_marble")
    b.fill(x0, -1, back, x1, 3, back, "wall_marble")
    # 梁圈与屋顶
    for x in range(x0, x1 + 1):
        b.set(x, 4, back, "beam", axis="x")
        b.set(x, 4, front, "beam", axis="x")
    for z in range(back + 1, front):
        b.set(mid, 4, z, "beam", axis="z")
    _gable_roof_z(b, x0 - 1, x1 + 1, back, front, 5)
    # 岸上：陶瓮（鱼货）、网架用栅栏
    for x in (8, 9, 10):
        b.set(x + 6, 0, 2, "pithos")
    for x in range(14, 18):
        b.set(x, 0, 5, "fence")
    b.anchor("workstation", 3, 0, 2)
    b.anchor("haul", 14, 0, 4)
    # 渔船：一条拖上船台，一条泊在水里，一条靠在石堤（白模用原版木船）
    b.commands += [
        'summon minecraft:boat ~3.5 ~-1 ~4.5 {Type:"oak",Rotation:[0f,0f]}',
        'summon minecraft:boat ~9.5 ~-1 ~11.5 {Type:"oak",Rotation:[0f,0f]}',
        'summon minecraft:boat ~16.8 ~-1 ~14.5 {Type:"spruce",Rotation:[0f,0f]}',
    ]


def poseidon_spring() -> Building:
    b = Building("greek_poseidon_spring", "波塞冬·海神泉")
    W, D = 9, 8  # 前廊柱子在 x = 9，进深 z 0..8
    b.fill(0, 0, 0, W, 0, D, "step")
    # 西墙（出水墙）：回纹饰带 + 三个出水口
    b.fill(0, 1, 0, 0, 4, D, "wall_marble")
    for z in range(1, D):
        b.set(0, 2, z, "meander_blue")
    for z in (2, 4, 6):
        b.set(0, 3, z, "mythic_age:lion_spout", facing="east")  # 狮头出水口砌在出水墙里，朝池子
    # 南北墙围住水池间，前廊两侧敞开
    for z in (0, D):
        b.fill(1, 1, z, 5, 4, z, "wall_marble")
    # 蓄水池：池底下沉一格，池沿台面
    b.fill(1, -1, 1, 4, -1, D - 1, "step")
    b.fill(1, 0, 1, 4, 0, D - 1, "minecraft:water")
    for z in range(1, D):
        b.set(5, 0, z, "smooth_slab", type="top")
    # 前廊：五根爱奥尼柱（厄瑞克忒翁北廊是爱奥尼式）
    for z in range(0, D + 1, 2):
        b.column(W, 1, z, 2, "ionic", axis="z")
    for z in range(0, D + 1):
        b.set(W, 5, z, "architrave", facing="east")
        b.set(0, 5, z, "wall_marble")
    for x in range(1, W):
        b.set(x, 5, 0, "architrave", facing="north")
        b.set(x, 5, D, "architrave", facing="south")
    b.fill(1, 5, 1, W - 1, 5, D - 1, "floor")
    b.gable_roof(0, W - 1, 0, D, 6)
    b.pediment_gable(W, 0, D, 6, "east")
    b.pediment_gable(0, 0, D, 6, "west")
    # 三叉戟印：前廊北端地面三个分开的孔（三齿呈 V 形），孔下是盐泉；正上方的顶棚与屋顶留天窗
    for (x, z) in ((6, 2), (7, 1), (8, 2)):
        b.set(x, -2, z, "step")
        b.set(x, -1, z, "minecraft:water")
        b.set(x, 0, z, "minecraft:air")
    for x in (6, 7, 8):
        for y in range(5, 8):
            if (x, y, 1) in b.blocks and y > 4:
                b.blocks[(x, y, 1)] = "minecraft:air"
    b.anchor("water", 5, 1, 4)
    b.anchor("ritual", 7, 1, 2)
    return b


# ───────────── 哈迪斯 ─────────────

def cypress(b: Building, x, z, h, y0=0):
    """地中海柏树：细高的深色树柱，树干在中间，顶端收尖。"""
    for y in range(y0, y0 + h - 2):
        b.set(x, y, z, "mythic_age:cypress_log", axis="y")
    for y in range(y0 + 1, y0 + h + 1):
        k = y - y0
        r = 0 if k >= h - 1 else 1
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                full = k in range(3, h - 3) and (k + dx + dz) % 3 == 0
                if (abs(dx) + abs(dz) <= 1 or full) and _free(b, x + dx, y, z + dz):
                    b.set(x + dx, y, z + dz, "mythic_age:cypress_leaves", persistent="true")


def hades_cemetery() -> Building:
    b = Building("greek_hades_cemetery", "哈迪斯·柏树墓园")
    X = 20
    # 路北一排柏树，路宽 3（z 2..4），路南三块家族墓园（z 5..14）
    for x in (1, 6, 11, 16):
        cypress(b, x, 0, 9 + x % 3)
    for x in range(0, X + 1):
        for z in (2, 3, 4):
            b.set(x, -1, z, "minecraft:dirt_path" if z != 3 or x % 4 else "gravel")
    plots = ((0, "naiskos"), (7, "stele"), (14, "tumulus"))
    for a, kind in plots:
        e = a + 6
        # 墓园地面、侧墙与后墙（多边形砌石矮墙）
        b.fill(a + 1, -1, 6, e - 1, -1, 13, "minecraft:coarse_dirt")
        for z in range(6, 15):
            for y in (0, 1):
                b.set(a, y, z, "polygonal_wall")
                b.set(e, y, z, "polygonal_wall")
        for x in range(a, e + 1):
            for y in (0, 1):
                b.set(x, y, 14, "polygonal_wall")
        b.clear(e, 0, 12, e, 1, 12)  # 侧门
        # 临路高墙：黑石勒脚 + 大理石方石，顶上压檐
        for x in range(a, e + 1):
            b.set(x, 0, 5, "minecraft:polished_blackstone_bricks")
            b.fill(x, 1, 5, x, 2, 5, "step")
            b.set(x, 3, 5, "mythic_age:marble_ashlar_slab", type="bottom")
        # 墙顶的墓碑
        if kind == "naiskos":
            for x in (a + 2, a + 4):
                b.fill(x, 3, 5, x, 4, 5, "mythic_age:marble_wall")
            b.set(a + 3, 3, 5, "step")
            b.set(a + 3, 4, 5, "mythic_age:grave_relief", facing="north")  # 浮雕面朝路
            for x in range(a + 2, a + 5):
                b.set(x, 5, 5, "pediment", facing="north")
            b.set(a + 3, 6, 5, "pediment", facing="north")
        elif kind == "stele":
            b.fill(a + 3, 3, 5, a + 3, 5, 5, "mythic_age:marble_pillar")
            b.set(a + 3, 6, 5, "mythic_age:polished_marble_slab", type="bottom")
            for x in (a + 1, a + 5):
                b.set(x, 3, 5, "mythic_age:marble_lekythos", facing="north")
        # 园内：低矮墓碑两排、供奉的小陶瓶与油灯；后角一株柏树
        if kind == "tumulus":
            cx, cz = a + 3, 10
            for y, r in ((0, 3.2), (1, 2.3), (2, 1.2)):
                for x in range(a + 1, e):
                    for z in range(6, 14):
                        if math.hypot(x - cx, z - cz) <= r:
                            b.set(x, y, z, "grass" if y == 2 or math.hypot(x - cx, z - cz) > r - 1 else BASE)
            b.set(cx, 3, cz, "mythic_age:marble_wall")
            b.set(cx, 0, cz - 4, "minecraft:flower_pot")
        else:
            for z in (8, 11):
                for x in (a + 2, a + 4):
                    b.set(x, 0, z, "mythic_age:marble_wall")
                b.set(a + 3, 0, z, "minecraft:flower_pot")
            b.set(a + 1, 0, 7, "lamp")
            cypress(b, e - 1, 13, 8)
        b.anchor("grave_visit", a + 3, 0, 7)
    return b


BUILDERS = (lambda: zeus_oak(False), zeus_oak, poseidon_harbor, poseidon_spring, hades_cemetery)


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
