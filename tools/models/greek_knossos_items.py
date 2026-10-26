#!/usr/bin/env python3
"""克诺索斯玩法方块与物品（T23）：牛首封门、线团祭坛、阿里阿德涅线团。

形制出处：
  - 牛首封门：门板是包青铜箍的深色木门；正中上方一块青铜牌上是米诺斯的牛首——黑色的头、金色的角、白色的鼻吻，
    取自克诺索斯小宫殿出土的牛首形来通杯（Bull's-head rhyton，约前 1450 年，伊拉克利翁考古博物馆）。
  - 线团祭坛：大理石小祭坛，台面上一团金线（阿里阿德涅交给忒修斯的线团；Plutarch, Theseus 19；Catullus 64）。
运行：python3 tools/models/greek_knossos_items.py && python3 tools/texgen/texgen.py --force <生成的规格>
"""
from __future__ import annotations

import math

from modelgen import box, cross, grid, model, noise, rows, spec, tex

written: list[str] = []


def put(name, g, palette, folder="block/"):
    spec(name, rows(g) if isinstance(g[0], list) else g, palette, folder=folder)
    written.append(name)


WOOD = {"a": "#2e2016", "w": "#3d2b1c", "W": "#4f3825", "l": "#62462d"}
BRONZE = {"o": "#5a3818", "b": "#8a5a26", "B": "#b07a36", "h": "#d6a24e"}
BULL = {"K": "#17120f", "G": "#3a302a", "Y": "#ecc565", "y": "#b8902e", "M": "#ebe4d4", "R": "#a8321f"}

# 门板：竖向木板（每块 4 像素，缝线暗），上下两道青铜箍带铆钉
g = grid()
for y in range(16):
    for x in range(16):
        c = "a" if x % 4 == 0 else ("W" if noise(x, y, 21) > 0.35 else "w")
        if x % 4 == 2 and noise(x, y, 23) > 0.85:
            c = "l"
        g[y][x] = c
for y in (2, 3, 12, 13):
    for x in range(16):
        g[y][x] = "B" if y in (2, 12) else "b"
    for x in (1, 5, 9, 13):
        g[y][x] = "h" if y in (2, 12) else "o"
put("bull_gate", g, {**WOOD, **BRONZE})

# 牛首牌：青铜底板上的黑牛头（金角、白鼻吻、红眼）
HEAD = [
    "oooooooooooooooo",
    "oY............Yo",
    "oYy..........yYo",
    "o.Yy........yY.o",
    "o..YyKKKKKKyY..o",
    "o...KKGKKGKK...o",
    "o..KKKKKKKKKK..o",
    "o..KRKKKKKKRK..o",
    "o...KKKKKKKK...o",
    "o...KKKGGKKK...o",
    "o....KKKKKK....o",
    "o....KMMMMK....o",
    "o....MMKKMM....o",
    "o.....MMMM.....o",
    "o..............o",
    "oooooooooooooooo",
]
g = [list(r) for r in HEAD]
for y in range(16):
    for x in range(16):
        if g[y][x] == ".":
            g[y][x] = "h" if noise(x, y, 31) > 0.8 else "B"
put("bull_gate_head", g, {**BRONZE, **BULL})

# 金线（线团祭坛上的线团与物品图标共用的纹理）：斜向缠绕的金线
GOLD = {"d": "#8f6a1f", "g": "#c99a35", "G": "#e8c060", "H": "#fbe7a1"}
g = grid()
for y in range(16):
    for x in range(16):
        t = (x + 2 * y) % 5
        g[y][x] = "H" if t == 0 else ("G" if t in (1, 4) else ("g" if t == 2 else "d"))
put("golden_thread", g, GOLD)

# 物品图标：一团金线，右下拖出一段线头
g = grid()
cx, cy, r = 7.0, 7.5, 5.6
for y in range(16):
    for x in range(16):
        dx, dy = x + 0.5 - cx, y + 0.5 - cy
        d = math.hypot(dx, dy)
        if d <= r:
            t = int((x * 0.8 + y * 1.6 + (3 if dx * dy > 0 else 0)) % 5)
            c = "H" if t == 0 else ("G" if t in (1, 4) else ("g" if t == 2 else "d"))
            if d > r - 1.0:
                c = "d" if dx + dy > 0 else "g"
            g[y][x] = c
for i, (x, y) in enumerate([(11, 11), (12, 12), (13, 12), (14, 13), (14, 14), (15, 15)]):
    g[y][x] = "G" if i % 2 == 0 else "g"
g[4][5] = "H"
g[5][4] = "H"
put("ariadne_thread", g, GOLD, folder="item/")


# 牛角（英雄装备部件）：一只弯角，根部深褐、往尖端渐成乳白
HORN = {"o": "#3b2a1e", "d": "#6b4a30", "m": "#a07a52", "l": "#d9c4a0", "w": "#f2e8d2"}


def horn_cells():
    """弯角：沿一条从左下到右上的弧，宽度从 4 收到 1。"""
    cells = {}
    for t in range(0, 23):
        u = t / 22
        cx = 3 + 10 * u
        cy = 13 - 11 * u + 4 * math.sin(u * math.pi) * 0.6
        w = 2.2 - 1.7 * u
        for x in range(16):
            for y in range(16):
                if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= w:
                    k = "d" if u < 0.25 else ("m" if u < 0.55 else ("l" if u < 0.8 else "w"))
                    cells.setdefault((x, y), k)
    return cells


def draw_horn(extra=None):
    g = grid()
    cells = horn_cells()
    for (x, y), k in cells.items():
        g[y][x] = k
    for (x, y) in cells:
        for dx, dy in ((1, 0), (0, 1)):
            if (x + dx, y + dy) not in cells and 0 <= x + dx < 16 and 0 <= y + dy < 16 and g[y + dy][x + dx] == ".":
                g[y + dy][x + dx] = "o"
    if extra:
        extra(g)
    return g


put("bull_horn", draw_horn(), HORN, folder="item/")


# 弥诺陶洛斯之角（神之信物）：更大更黑的角，根部一道青铜箍与一截红绳
def token_extra(g):
    for (x, y) in ((3, 12), (4, 12), (4, 11), (3, 11), (5, 12)):
        g[y][x] = "B"
    for (x, y) in ((2, 13), (2, 14), (1, 15)):
        g[y][x] = "R"


TOKEN = {**HORN, "d": "#2a1d14", "m": "#5a4030", "l": "#c9b38c", "B": "#d6a24e", "R": "#a8321f"}
put("minotaur_horn", draw_horn(token_extra), TOKEN, folder="item/")


# ═════════════════════════════ 模型 ═════════════════════════════

model("bull_gate", {"particle": tex("bull_gate"), "all": tex("bull_gate")},
      [box((0, 0, 0), (16, 16, 16), "all")])
model("bull_gate_head", {"particle": tex("bull_gate"), "head": tex("bull_gate_head"), "side": tex("bull_gate")},
      [box((0, 0, 0), (16, 16, 16), {"north": "head", "south": "head", "east": "side", "west": "side", "up": "side",
                                     "down": "side"})])

ALT = {"particle": tex("smooth_marble"), "s": tex("smooth_marble"), "c": tex("marble_column"),
       "m": tex("marble_molding"), "t": tex("golden_thread")}
model("thread_altar", ALT, [
    box((1, 0, 1), (15, 3, 15), "s", comment="台座"),
    *cross(3, 13, 3, 10, 2, "c", faces="nsew"),
    box((2, 10, 2), (14, 12, 14), "m", comment="台面"),
    *cross(5.5, 10.5, 12, 15.5, 1.2, "t", comment="线团"),
])

print("规格：", " ".join(f"tools/texgen/specs/greek/{n}.json" for n in written))
