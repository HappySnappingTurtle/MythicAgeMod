#!/usr/bin/env python3
"""希腊村庄第 6 批白模（下）：桥、台地挡土墙与台阶片段（V3-希腊-建筑 §6）。出处见白模篇第 6 批「出处」表。

坐标同 buildgen：x 向东、z 向南，原点为占地西北角，y = 0 是地面上第一层（y = -1 是地表）。
  窄桥 17 × 11（路宽 3）：迈锡尼式（阿卡迪科桥）——多边形巨石干砌，桥身下一个叠涩三角拱洞，两端缓坡上桥。
  宽桥 23 × 13（路宽 5）：希腊化式（埃莱乌特那桥）——大理石方石成层干砌，叠涩三角拱；河宽时中间加桥墩，
        每孔跨 5 格，可按河宽重复。两侧矮护墙是游戏上的让步（原桥不明）。
  台地片段：挡土墙（多边形砌石，高 1–4，顶面压方石）、阶梯街（主街 3 宽 / 巷道 2 宽，台阶贴着挡土墙爬升）、转角。
  台地坡组合 33 × 37：四级台地（每级 3–4 格高），每级前沿一条等高线街道，街后是坡地小屋；
        一条 3 宽的阶梯主街、一条 2 宽的阶梯巷道垂直穿过各级（普里埃内式），顶上一级是橄榄树。

运行：python3 tools/buildings/greek_terrain.py [--world <存档目录>]
"""
from __future__ import annotations

import sys
from pathlib import Path

from buildgen import Building, ROOT, write_datapack
from greek_core import house_small
from greek_farm import olive_tree

OUT = ROOT / "docs/重构/开发/TV/白模/希腊/第6批_主神差异件与地形片段"
BASE = "minecraft:dirt"
STAIRS = "mythic_age:marble_ashlar_stairs"


def place(dst: Building, src: Building, ox, oy, oz):
    """把另一座建筑的体素（含锚点、附带命令）平移后并进来。"""
    for (x, y, z), s in src.blocks.items():
        dst.blocks[(x + ox, y + oy, z + oz)] = s
    for name, pts in src.anchors.items():
        for (x, y, z) in pts:
            dst.anchor(name, x + ox, y + oy, z + oz)
    for cmd in src.commands:
        dst.commands.append(cmd)  # 白模里只有展示框 / 召唤，坐标偏移在调用处处理


# ───────────── 桥 ─────────────

def _river(b: Building, x0, x1, z0, z1, surface=-2, bed=-4):
    """南北流向的河道（白模）：两岸填土、河床、两端封口，水面比地面低 1 格。"""
    b.fill(x0 - 6, bed, z0 - 1, x1 + 6, -2, z1 + 1, BASE)
    for x in range(x0 - 6, x1 + 7):
        for z in range(z0, z1 + 1):
            if x < x0 or x > x1:
                b.set(x, -1, z, "grass")
    b.fill(x0, bed + 1, z0, x1, surface, z1, "minecraft:water")
    b.clear(x0, surface + 1, z0, x1, -1, z1)  # 水面上方掀掉平台草皮


def _corbel_span(b: Building, x0, x1, z0, z1, water_top, deck):
    """叠涩三角拱的一孔：拱洞水面以上一层与河同宽，再往上每层两边各收 1 格（5 → 3 → 1），然后封顶到桥面。"""
    for k in range(0, deck - water_top + 1):
        y = water_top + k
        shrink = max(0, k - 2)
        lo, hi = x0 + shrink, x1 - shrink
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                inside = lo <= x <= hi and y < deck
                if not inside:
                    b.set(x, y, z, "_mason")


def bridge(name, title, road, spans, masonry, wall):
    """road：路宽；spans：孔数（每孔 5 格，孔间 1 格桥墩）。"""
    river_w = spans * 5 + (spans - 1)
    rx0 = 6
    rx1 = rx0 + river_w - 1
    zc0, zc1 = 0, road + 5  # 河道沿 z 方向的展示长度
    bz0 = (zc1 - road) // 2  # 桥身（含护墙）起止
    bz1 = bz0 + road + 1
    b = Building(name, title)
    _river(b, rx0, rx1, zc0, zc1)
    deck = 2  # 桥面方块在 y = 2（比两岸高 3 格），护墙在 y = 3
    # 拱孔与桥墩
    for s in range(spans):
        sx0 = rx0 + s * 6
        _corbel_span(b, sx0, sx0 + 4, bz0, bz1, -3, deck)
        if s:
            b.fill(sx0 - 1, -4, bz0, sx0 - 1, deck, bz1, "_mason")
    # 桥台：两岸实砌，往外用台阶缓坡接到地面
    for (xa, xb) in ((rx0 - 3, rx0 - 1), (rx1 + 1, rx1 + 3)):
        b.fill(xa, -3, bz0, xb, deck, bz1, "_mason")
    for z in range(bz0 + 1, bz1):
        for (xs, xo, facing) in ((rx0 - 4, rx0 - 5, "east"), (rx1 + 4, rx1 + 5, "west")):
            b.set(xs, 0, z, "_mason")
            b.set(xs, 1, z, STAIRS, facing=facing)
            b.set(xo, 0, z, STAIRS, facing=facing)
    # 桥面：碎石路面，两侧矮护墙
    for x in range(rx0 - 3, rx1 + 4):
        for z in range(bz0 + 1, bz1):
            b.set(x, deck, z, "gravel" if (x + z) % 3 else masonry)
        b.set(x, deck + 1, bz0, wall)
        b.set(x, deck + 1, bz1, wall)
    for (p, s) in list(b.blocks.items()):
        if s == "_mason":
            b.blocks[p] = B_STATE[masonry]
    b.anchor("crossing", rx0 - 6, 0, (bz0 + bz1) // 2)
    b.anchor("crossing", rx1 + 6, 0, (bz0 + bz1) // 2)
    return b


B_STATE = {"polygonal": "mythic_age:polygonal_marble", "step": "mythic_age:marble_ashlar"}


def bridge_narrow() -> Building:
    return bridge("greek_bridge_narrow", "窄桥（路宽 3，迈锡尼式）", 3, 1, "polygonal", "mythic_age:polygonal_marble_wall")


def bridge_wide() -> Building:
    return bridge("greek_bridge_wide", "宽桥（路宽 5，两孔，希腊化式）", 5, 2, "step", "mythic_age:marble_ashlar_wall")


# ───────────── 台地 ─────────────

def retaining_wall(b: Building, x0, x1, z, low, high, parapet=True, gaps=()):
    """挡土墙：在 z 这一行从 low+1 砌到 high（多边形砌石），顶面压方石；高 ≥ 3 时墙顶加矮护墙（gaps 处留口）。"""
    for x in range(x0, x1 + 1):
        for y in range(low + 1, high):
            b.set(x, y, z, "polygonal")
        b.set(x, high, z, "step")
        if parapet and high - low >= 3 and x not in gaps:
            b.set(x, high + 1, z, "polygonal_wall")


def stair_run(b: Building, x0, x1, z_wall, low, high):
    """贴着挡土墙南面往上爬的台阶：从下一级地面起，每格升 1，最后一级与上一级地面齐平；台阶下实砌。"""
    h = high - low
    for j in range(1, h + 1):
        z = z_wall + (h - j + 1)
        for x in range(x0, x1 + 1):
            for y in range(low + 1, low + j):
                b.set(x, y, z, "step")
            b.set(x, low + j, z, STAIRS, facing="north")
    # 两侧的颊墙（台阶两边的挡墙）
    for x in (x0 - 1, x1 + 1):
        for j in range(1, h + 1):
            z = z_wall + (h - j + 1)
            for y in range(low + 1, low + j + 1):
                b.set(x, y, z, "polygonal")


def terrace_pieces() -> Building:
    """单独的片段样张：西段 4 格高挡土墙 + 3 宽阶梯；东段 2 格高挡土墙 + 2 宽阶梯；两段之间的转角。"""
    b = Building("greek_terrace_pieces", "台地片段（挡土墙、阶梯、转角）")
    for (x0, x1, s) in ((0, 11, 3), (12, 21, 1)):
        b.fill(x0, 0, 0, x1, s - 1, 4, BASE)
        b.fill(x0, s, 0, x1, s, 4, "grass")
        b.fill(x0, -1, 5, x1, -1, 10, "grass")
    retaining_wall(b, 0, 11, 5, -1, 3, gaps=(3, 4, 5))
    stair_run(b, 3, 5, 5, -1, 3)
    retaining_wall(b, 12, 21, 5, -1, 1, gaps=(16, 17))
    stair_run(b, 16, 17, 5, -1, 1)
    # 转角：西段台地的东沿折向北，挡住高出东段的 2 格
    for z in range(0, 6):
        b.set(12, 2, z, "polygonal")
        b.set(12, 3, z, "step")
    return b


def terrace_hill() -> Building:
    """台地坡组合：自南往北四级，地面高度 -1 / 3 / 6 / 9。"""
    b = Building("greek_terrace_hill", "台地坡组合（四级台地 + 阶梯街 + 坡地小屋）")
    X = 32
    # (北界 z, 南界 z, 地面高度)：每级南沿 3 格是等高线街道，后面是小屋
    levels = [(0, 5, 9), (6, 17, 6), (18, 29, 3), (30, 36, -1)]
    main_x = (14, 16)  # 3 宽阶梯主街
    alley_x = (29, 30)  # 2 宽阶梯巷道
    for (z0, z1, s) in levels:
        if s >= 0:
            b.fill(0, 0, z0, X, s - 1, z1, BASE)
        for x in range(0, X + 1):
            for z in range(z0, z1 + 1):
                street = z >= z1 - 2 and s < 9
                b.set(x, s, z, "gravel" if street and (x + z) % 4 else "polygonal" if street else "grass")
    # 挡土墙与阶梯：每级的南沿是墙，下面一级贴墙爬上来
    for i in range(len(levels) - 1):
        z0, z1, s = levels[i]
        low = levels[i + 1][2]
        retaining_wall(b, 0, X, z1, low, s, gaps=tuple(range(main_x[0], main_x[1] + 1)) + alley_x)
        stair_run(b, main_x[0], main_x[1], z1, low, s)
        stair_run(b, alley_x[0], alley_x[1], z1, low, s)
    # 阶梯街穿过每级的屋后部分：铺方石
    for (z0, z1, s) in levels[1:3]:
        for z in range(z0, z1 - 2):
            for x in range(main_x[0], main_x[1] + 1):
                if b.blocks.get((x, s, z)) == "minecraft:grass_block":
                    b.set(x, s, z, "polygonal")
            for x in alley_x:
                if b.blocks.get((x, s, z)) == "minecraft:grass_block":
                    b.set(x, s, z, "polygonal")
    # 坡地小屋：每级两到三座，门朝南开向街
    house = house_small()
    for (z0, z1, s) in levels[1:3]:
        for hx in (2, 21):
            place(b, house, hx, s + 1, z0)
    # 顶级：橄榄树
    top = Building("_t", "")
    for i, x in enumerate((4, 11, 20, 26)):
        olive_tree(top, x, 2 + i % 2, i)
    place(b, top, 0, 10, 0)
    return b


BUILDERS = (bridge_narrow, bridge_wide, terrace_pieces, terrace_hill)


def main():
    buildings = [f() for f in BUILDERS]
    OUT.mkdir(parents=True, exist_ok=True)
    for b in buildings:
        p = b.layer_plans(OUT / f"{b.name}_逐层.png", cell=12 if b.name == "greek_terrace_hill" else 16)
        print(f"{b.title}：{len(b.blocks)} 方块，平面图 {p.relative_to(ROOT)}")
    if "--world" in sys.argv:
        world = Path(sys.argv[sys.argv.index("--world") + 1])
        print("数据包：", write_datapack(world, buildings))


if __name__ == "__main__":
    main()
