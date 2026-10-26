#!/usr/bin/env python3
"""克诺索斯迷宫（T22 阶段 ③，ST-GR-04）：地下迷宫的模块、中心大厅、宝库房、地表宫殿残迹。

形制参考：克诺索斯宫殿（伊文思复原）——白灰墙、赭红墙裙与壁画带、石膏石（gypsum）铺地、木梁天花，
上粗下细的红色柏木柱，牛角形的圣角顶饰（horns of consecration），双斧（labrys）标志；迷宫的传说与
「双斧之宫」（labyrinthos 一说来自 labrys）相连。出处：Evans, The Palace of Minos（1921–1935）；
伊拉克利翁考古博物馆的圣角与双斧展品。

坐标：x 向东、y 向上、z 向南。每个迷宫格 8 × 8，高 11（y -2..8）：地板两层（y -2、-1），通道净高 4（y 0..3），
房间净高 5（y 0..4），其余实心（天然就是外壳，洞穴、含水层进不来）。
通道 3 格宽，沿格子中线（局部 2..4）；开口按 N/E/S/W 位掩码（1/2/4/8）。
大厅按 BO-GR-01 的 JSON 坐标：局部 -15..15、地面 y = -1、穹顶底面 y = 7、柱高 7（顶到穹顶）、8 根柱、A/B 墙槽位。
"""
from __future__ import annotations

import json
from pathlib import Path

from buildgen import Building, ROOT

CELL = 8
Y0, Y1 = -2, 8          # 模块的竖向范围（含）
N, E, S, W = 1, 2, 4, 8
DIRS = {N: (0, -1), E: (1, 0), S: (0, 1), W: (-1, 0)}
FACING = {N: "north", E: "east", S: "south", W: "west"}

ARENA = ROOT / "neoforge/src/main/resources/data/mythic_age/mythic_age/arena/greek_knossos.json"  # 复制自 BO-GR-01（T23 运行时也读它）

# 材料（米诺斯宫殿：白灰墙、赭红墙裙、石膏石地面、木梁天花；实心部分是凝灰岩）
FILLER = "minecraft:tuff"
WALL = "mythic_age:plaster"
DADO = "mythic_age:plaster_dado"
FLOOR = "mythic_age:smooth_marble"
CEILING = "mythic_age:olive_planks"
LAMP = "mythic_age:greek_oil_lamp"
# 米诺斯构件（阶段 ④ 的正式方块，模型见 tools/models/greek_minoan.py）
MINOAN_COLUMN = "mythic_age:minoan_column"
LABRYS = "mythic_age:labrys_lintel"
HORNS = "mythic_age:horns_of_consecration"


def column_part(i: int, h: int) -> str:
    """红柱第 i 段（自下而上，共 h 段）的状态：结构放置不会按邻格更新 part，模板里要写对。"""
    part = "single" if h == 1 else ("bottom" if i == 0 else ("top" if i == h - 1 else "middle"))
    return f"{MINOAN_COLUMN}[part={part}]"


class Solid:
    """先按「实心 / 挖空」画格子，最后按挨着的空格给每个实心格选材料（墙、地、天花、填充）。"""

    def __init__(self, w: int, d: int, y0: int = Y0, y1: int = Y1):
        self.w, self.d, self.y0, self.y1 = w, d, y0, y1
        self.air: set[tuple[int, int, int]] = set()
        self.fixed: dict[tuple[int, int, int], str] = {}

    def carve(self, x0, y0, z0, x1, y1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if 0 <= x < self.w and 0 <= z < self.d and self.y0 <= y <= self.y1:
                        self.air.add((x, y, z))

    def put(self, x, y, z, state):
        """在挖空的地方放东西（灯、柱、床……），或强制某个实心格的材料。"""
        self.fixed[(x, y, z)] = state

    def build(self, name, title) -> Building:
        b = Building(name, title)
        for x in range(self.w):
            for z in range(self.d):
                for y in range(self.y0, self.y1 + 1):
                    p = (x, y, z)
                    if p in self.fixed:
                        b.blocks[p] = self.fixed[p]
                    elif p in self.air:
                        b.blocks[p] = "minecraft:air"
                    else:
                        b.blocks[p] = self.material(x, y, z)
        return b

    def material(self, x, y, z):
        if (x, y + 1, z) in self.air:
            return FLOOR
        if (x, y - 1, z) in self.air:
            return CEILING
        if any((x + dx, y, z + dz) in self.air for dx, dz in DIRS.values()):
            return DADO if y == 0 else WALL
        return FILLER


def arms(s: Solid, mask: int, ox=0, oz=0, height=3):
    """格子中心 3 × 3 加上朝各开口的 3 格宽通道。"""
    s.carve(ox + 2, 0, oz + 2, ox + 4, height, oz + 4)
    if mask & N:
        s.carve(ox + 2, 0, oz + 0, ox + 4, height, oz + 1)
    if mask & S:
        s.carve(ox + 2, 0, oz + 5, ox + 4, height, oz + 7)
    if mask & W:
        s.carve(ox + 0, 0, oz + 2, ox + 1, height, oz + 4)
    if mask & E:
        s.carve(ox + 5, 0, oz + 2, ox + 7, height, oz + 4)


LIGHT = "minecraft:shroomlight"  # 天花嵌灯（迷宫里不刷怪，灯只为看得见路）


def corridor(mask: int) -> Building:
    s = Solid(CELL, CELL)
    arms(s, mask)
    s.put(3, 4, 3, LIGHT)
    return s.build(f"knossos_corridor_{mask}", f"迷宫回廊（开口 {mask}）")


def room(kind: str, door: int) -> Building:
    """死胡同上的房间（净高 5，局部 1..6）：rest 休整点、colonnade 教学柱廊、storage 储物间。"""
    s = Solid(CELL, CELL)
    s.carve(1, 0, 1, 6, 4, 6)
    arms(s, door, height=3)
    s.put(3, 5, 3, LIGHT)
    b_anchor = []
    if kind == "rest":
        s.put(5, 0, 1, "mythic_age:greek_brazier[lit=true]")
        s.put(1, 0, 5, "minecraft:white_bed[facing=north,part=foot]")
        s.put(1, 0, 4, "minecraft:white_bed[facing=north,part=head]")
        s.put(6, 0, 6, "minecraft:barrel[facing=up]")
        b_anchor = [("safe_return", 3, 0, 3)]
    elif kind == "colonnade":
        # 三根柱子一排（让玩家在打 Boss 之前见过「冲锋撞柱」的场面）：中间那根断了、碎块在地上
        for z, h in ((2, 3), (5, 3)):
            s.put(4, 0, z, "mythic_age:marble_column_base")
            for i in range(h):
                s.put(4, 1 + i, z, "mythic_age:marble_column")
            s.put(4, 1 + h, z, "mythic_age:doric_capital")
        s.put(4, 0, 3, "mythic_age:marble_column_base")
        s.put(3, 0, 4, "mythic_age:marble_rubble[facing=east]")
        s.put(5, 0, 4, "mythic_age:marble_rubble[facing=north]")
        s.put(1, 0, 1, LAMP)
        b_anchor = [("quest", 2, 0, 3)]
    elif kind == "storage":
        s.put(1, 0, 1, "minecraft:chest[facing=south]")
        s.put(6, 0, 1, "mythic_age:pithos")
        s.put(6, 0, 2, "mythic_age:pithos")
        s.put(1, 0, 6, "mythic_age:pithos")
        s.put(6, 0, 6, LAMP)
        b_anchor = [("loot", 1, 0, 1)]
    b = s.build(f"knossos_{kind}_{door}", f"迷宫{ {'rest': '休整点', 'colonnade': '教学柱廊', 'storage': '储物间'}[kind] }（门 {door}）")
    for name, x, y, z in b_anchor:
        b.anchor(name, x, y, z)
    return b


ANTE_W, ANTE_E, ANTE_S3, ANTE_S4 = 1, 2, 4, 8


def antechamber(mask: int) -> Building:
    """牛首门前室：两格（16 × 8）并成一间，北墙中间 5 格是通往大厅的门（T23 在这里放牛首门）。
    开口：西（左格的西）、东（右格的东）、左格的南、右格的南。"""
    s = Solid(2 * CELL, CELL)
    s.carve(1, 0, 1, 14, 4, 6)
    s.carve(6, 0, 0, 10, 4, 0)                     # 北门，接大厅南门（世界 x 30..34）
    if mask & ANTE_W:
        s.carve(0, 0, 2, 0, 3, 4)
    if mask & ANTE_E:
        s.carve(15, 0, 2, 15, 3, 4)
    if mask & ANTE_S3:
        s.carve(2, 0, 7, 4, 3, 7)
    if mask & ANTE_S4:
        s.carve(10, 0, 7, 12, 3, 7)
    # 门两侧的红柱，北墙上门两边各一块双斧门楣（朝南，对着前室）
    for x in (5, 11):
        for y in range(0, 5):
            s.put(x, y, 1, column_part(y, 5))
    for x in (3, 13):
        s.put(x, 2, 0, f"{LABRYS}[facing=south]")
    s.put(1, 0, 6, LAMP)
    s.put(14, 0, 6, LAMP)
    b = s.build(f"knossos_antechamber_{mask}", f"牛首门前室（开口 {mask}）")
    b.anchor("gate", 8, 0, 0)
    return b


def hall_voxels() -> Solid:
    """中心大厅：32 × 32（局部 -15..15 放在 1..31，第 0 行 / 列是实心），穹顶底面 y = 7。"""
    data = json.loads(ARENA.read_text(encoding="utf-8"))
    s = Solid(32, 32, Y0, Y1)
    off = 16  # 局部 0 → 体素 16（大厅占 1..31）
    floor = {(x + off, z + off) for x, z in data["floor_cells"]}
    for (x, z) in floor:
        s.carve(x, 0, z, x, 6, z)
    # 南门（局部 z = 15 那一圈与再外一行），北门封着（宝库门槽，T23 打开）
    s.carve(off - 2, 0, 31, off + 2, 4, 31)
    for p in data["pillars"]:
        x, z = p["xz"][0] + off, p["xz"][1] + off
        # 柱子顶到穹顶（底面 y = 7）：柱础 + 5 段柱身 + 柱头（用户审阅：原来 5 格高够不着天花板）
        s.put(x, 0, z, "mythic_age:marble_column_base")
        for y in range(1, 6):
            s.put(x, y, z, "mythic_age:marble_column")
        s.put(x, 6, z, "mythic_age:doric_capital")
    for g in data["wall_groups"]:
        for x, z in g["cells"]:
            s.put(x + off, -1, z + off, "mythic_age:blue_meander_marble")  # 升降墙槽位（T23）
    # 天光：穹顶上 8 处嵌灯
    for x, z in ((-8, 0), (8, 0), (0, -8), (0, 8), (-5, -5), (5, -5), (-5, 5), (5, 5),
                 (-12, -4), (-12, 4), (12, -4), (12, 4), (-4, -12), (4, -12), (-4, 12), (4, 12)):
        s.put(x + off, 7, z + off, "minecraft:shroomlight")
    s.put(off, -1, off + 13, "mythic_age:pebble_mosaic")  # 入口安全点
    return s


def hall_quarters() -> list[Building]:
    s = hall_voxels()
    whole = s.build("knossos_hall", "中心大厅")
    data = json.loads(ARENA.read_text(encoding="utf-8"))
    out = []
    for qx, qz, name in ((0, 0, "nw"), (16, 0, "ne"), (0, 16, "sw"), (16, 16, "se")):
        b = Building(f"knossos_hall_{name}", f"中心大厅（{name}）")
        for (x, y, z), st in whole.blocks.items():
            if qx <= x < qx + 16 and qz <= z < qz + 16:
                b.blocks[(x - qx, y, z - qz)] = st
        out.append(b)
    # 锚点写在西北片上（相对大厅原点），放置时按整厅换算
    out[0].anchor("boss_origin", data["boss_spawn"][0] + 16, 0, data["boss_spawn"][1] + 16)
    out[0].anchor("safe_return", data["entry"][0] + 16, 0, data["entry"][1] + 16)
    for p in data["pillars"]:
        out[0].anchor("pillar", p["xz"][0] + 16, 0, p["xz"][1] + 16)
    for g in data["wall_groups"]:
        for x, z in g["cells"]:
            out[0].anchor("wall_" + g["id"].lower(), x + 16, 0, z + 16)
    out[0].anchor("vault_door", 16, 0, 1)
    return out


def vault_room() -> Building:
    """宝库房：两格（16 × 8），在大厅北面；只从大厅北墙中间（封着的门槽）进。"""
    s = Solid(2 * CELL, CELL)
    s.carve(1, 0, 1, 14, 4, 6)
    s.carve(7, 0, 7, 9, 3, 7)  # 通向大厅北门槽的一段
    s.put(8, 0, 2, "mythic_age:ruin_vault[facing=south]")
    for x in (3, 13):
        s.put(x, 0, 1, "minecraft:chest[facing=south]")
    for x in (1, 14):
        s.put(x, 0, 1, "mythic_age:pithos")
        s.put(x, 0, 6, LAMP)
    for x in (5, 11):
        for y in range(0, 5):
            s.put(x, y, 1, column_part(y, 5))
    s.put(8, 2, 0, f"{LABRYS}[facing=south]")  # 宝库后墙上的双斧
    b = s.build("knossos_vault", "迷宫宝库房")
    b.anchor("vault", 8, 0, 2)
    b.anchor("loot", 3, 0, 1)
    b.anchor("loot", 13, 0, 1)
    return b


# 地表残迹里留给下行台阶（dromos）出口的槽：x 10..12、z 9..13（3 宽 5 长），铺地不写，两侧矮护墙
CUT_X, CUT_Z, CUT_LEN = 10, 9, 5


def surface_ruin() -> Building:
    """地表的米诺斯宫殿残迹（23 × 23）：中庭里是下行台阶的出口（台阶由程序挖，从迷宫入口格一路向南升到地面），
    出口两侧矮护墙；北面一道双斧门（两根红柱 + 门楣双斧），残墙上立着圣角；东侧一段宽缓台阶。
    y = 0 是地面上第一层，y = -1 是铺地（出口槽不铺）。"""
    b = Building("knossos_surface", "克诺索斯地表宫殿残迹")
    w = 23
    cut = {(x, z) for x in range(CUT_X, CUT_X + 3) for z in range(CUT_Z, CUT_Z + CUT_LEN)}
    for x in range(w):
        for z in range(w):
            if (x, z) not in cut:
                b.set(x, -1, z, "mythic_age:smooth_marble" if (x + z) % 5 else "mythic_age:polygonal_marble")
    for z in range(CUT_Z, CUT_Z + CUT_LEN - 1):
        for x in (CUT_X - 1, CUT_X + 3):
            b.set(x, 0, z, "mythic_age:polygonal_marble_wall")
    for x in range(CUT_X - 1, CUT_X + 4):
        b.set(x, 0, CUT_Z - 1, "mythic_age:polygonal_marble_wall")
    # 残墙：西、南两面，高低不一
    heights = [3, 3, 2, 1, 0, 1, 2, 3, 3, 2, 1, 1, 0, 0, 1, 2, 3, 3, 2, 1, 1, 2, 3]
    for i, h in enumerate(heights):
        for y in range(h):
            b.set(0, y, i, DADO if y == 0 else WALL)
            b.set(i, y, w - 1, DADO if y == 0 else WALL)
    # 北面的双斧门：两根红柱（4 高）+ 门楣（正中一块双斧门楣，朝外）
    for x in (9, 13):
        for y in range(4):
            b.set(x, y, 1, column_part(y, 4))
    for x in range(9, 14):
        b.set(x, 4, 1, "mythic_age:smooth_marble")
    b.set(11, 4, 1, LABRYS, facing="north")
    # 圣角：立在残墙高处与门楣上（远处看得见）；两角顺着墙的走向排开
    for x, y, z, f in ((0, 3, 1, "east"), (0, 3, 8, "east"), (17, 3, 22, "north"), (10, 5, 1, "north"),
                       (12, 5, 1, "north")):
        b.set(x, y, z, HORNS, facing=f)
    # 东侧宽缓台阶（剧场式台阶的残段）
    for i in range(4):
        for z in range(4, 18):
            b.set(w - 1 - i, i // 2, z, "mythic_age:marble_ashlar_stairs" if i % 2 == 0 else "mythic_age:marble_ashlar",
                  **({"facing": "west"} if i % 2 == 0 else {}))
    b.anchor("entrance", CUT_X + 1, 0, CUT_Z + CUT_LEN)
    b.anchor("cut", CUT_X, 0, CUT_Z)
    return b


CORRIDORS = [corridor(m) for m in range(1, 16)]
ROOMS = [room(k, d) for k in ("rest", "colonnade", "storage") for d in (N, E, S, W)]
ANTECHAMBERS = [antechamber(m) for m in range(16)]


def all_buildings() -> list[Building]:
    return CORRIDORS + ROOMS + ANTECHAMBERS + hall_quarters() + [vault_room(), surface_ruin()]
