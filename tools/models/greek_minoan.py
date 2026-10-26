#!/usr/bin/env python3
"""米诺斯构件（T22 阶段 ④，克诺索斯迷宫正式方块）：米诺斯红柱、双斧门楣、圣角。

形制出处：
  - 红柱：克诺索斯北入口、大阶梯（Grand Staircase）的复原柱——柏木柱身上粗下细（倒置树干），涂赭红，
    柱头是黑色的圆垫（torus）加方顶板，柱脚是一块低矮的圆石础。Evans, The Palace of Minos, Vol. I（1921）
    与 Vol. III（1930）的大阶梯复原；伊拉克利翁考古博物馆的柱础与壁画（「三分神殿」壁画里的红柱黑柱头）。
  - 双斧（labrys）：克诺索斯宫殿石块上的双斧刻记（mason's marks）与阿尔卡洛霍里洞穴出土的金双斧
    （伊拉克利翁考古博物馆）；两片对称的新月形斧刃夹一根竖柄。这里做成刻在白灰门楣上的青铜色浮雕。
  - 圣角（horns of consecration）：克诺索斯南入口复原的大圣角（石灰岩、抹白），宫殿与神龛顶上成排摆放；
    一个矮座上两只向上外翻的角。
模型按「正面朝北（-Z）」绘制，朝向由方块状态旋转。
运行：python3 tools/models/greek_minoan.py && python3 tools/texgen/texgen.py --force <生成的规格>
"""
from __future__ import annotations

from modelgen import box, cross, grid, model, noise, rows, spec, tex

written: list[str] = []


def put(name, g, palette):
    spec(name, rows(g) if isinstance(g[0], list) else g, palette)
    written.append(name)


# ═════════════════════════════ 贴图 ═════════════════════════════

RED = {"s": "#4f160f", "r": "#7d2418", "R": "#9a3020", "l": "#b8432c", "L": "#cf5a3c"}
BLACK = {"b": "#17130f", "B": "#26201b", "g": "#3a322b", "G": "#544940", "w": "#8d7f70"}
GYPSUM = {"h": "#f4efe3", "m": "#e6dfcf", "n": "#d6cdb9", "j": "#bdb29b"}
BRONZE = {"Y": "#f0c873", "y": "#cf9a45", "o": "#9c6a2b", "O": "#5e3d17"}

# 柱身：涂赭红的柏木，竖向木纹（深浅条纹 + 偶尔的节疤），竖向无缝
g = grid()
for x in range(16):
    base = "rRRlRrRRlRRrRlRR"[x]
    for y in range(16):
        c = base
        n = noise(x, y, 7)
        if n > 0.92:
            c = "s" if c != "l" else "r"
        elif n < 0.05:
            c = "L"
        g[y][x] = c
put("minoan_column", g, RED)

# 柱头：黑色圆垫上的横向凹槽带（复原柱头上常见的三道横纹）
put("minoan_capital", [
    "GGGGGGGGGGGGGGGG",
    "gggggggggggggggg",
    "BBBBBBBBBBBBBBBB",
    "bbbbbbbbbbbbbbbb",
    "gggggggggggggggg",
    "BBBBBBBBBBBBBBBB",
    "BBBBBBBBBBBBBBBB",
    "bbbbbbbbbbbbbbbb",
    "GGGGGGGGGGGGGGGG",
    "gggggggggggggggg",
    "BBBBBBBBBBBBBBBB",
    "bbbbbbbbbbbbbbbb",
    "gggggggggggggggg",
    "BBBBBBBBBBBBBBBB",
    "BBBBBBBBBBBBBBBB",
    "bbbbbbbbbbbbbbbb"], BLACK)

# 双斧门楣正面：石膏石底，上下各一道赭红带与刻线，中间一把青铜双斧（两片新月刃 + 竖柄）
LINTEL_PAL = {**GYPSUM, **RED, **BRONZE}


def labrys_cells() -> dict[tuple[int, int], str]:
    """双斧的像素（列 x, 行 y）→ 颜色键；以 7.5 为中线左右对称。刃从柄向外逐渐变高，外缘内凹成新月。"""
    cells: dict[tuple[int, int], str] = {}
    for y in range(3, 14):
        cells[(7, y)] = "o"
        cells[(8, y)] = "O"
    cells[(7, 2)] = "y"
    cells[(8, 2)] = "o"
    for x in range(2, 7):
        d = 6.5 - x                       # 离柄的距离 0.5..4.5
        half = 1.0 + d * 0.78              # 刃半高
        for y in range(3, 13):
            dy = y - 7.5
            if abs(dy) > half:
                continue
            if (x == 2 and abs(dy) < 2.6) or (x == 3 and abs(dy) < 1.1):   # 外缘内凹成新月
                continue
            top = dy < 0 and abs(dy) > half - 1
            bottom = dy > 0 and abs(dy) > half - 1
            k = "Y" if top else ("o" if bottom else "y")
            cells[(x, y)] = k
            m = 15 - x
            cells[(m, y)] = "Y" if top else ("o" if bottom else "y")
    return cells


LABRYS = labrys_cells()
g = grid(fill="m")
for y in range(16):
    for x in range(16):
        n = noise(x, y, 11)
        g[y][x] = "h" if n > 0.85 else ("n" if n < 0.12 else "m")
for x in range(16):
    g[0][x] = "j"
    g[1][x] = "R" if noise(x, 1, 3) > 0.2 else "r"
    g[2][x] = "s"
    g[13][x] = "s"
    g[14][x] = "R" if noise(x, 14, 3) > 0.2 else "r"
    g[15][x] = "j"
for (x, y), k in LABRYS.items():
    g[y][x] = k
put("labrys_lintel_front", g, LINTEL_PAL)

# 门楣其余面：石膏石，上下沿一道赭红
g = grid(fill="m")
for y in range(16):
    for x in range(16):
        n = noise(x, y, 13)
        g[y][x] = "h" if n > 0.85 else ("n" if n < 0.12 else "m")
for x in range(16):
    g[1][x] = "r"
    g[14][x] = "r"
put("labrys_lintel_side", g, LINTEL_PAL)

# 青铜（浮雕的侧面）
g = grid()
for y in range(16):
    for x in range(16):
        g[y][x] = "y" if noise(x, y, 5) > 0.3 else "o"
put("labrys_bronze", g, BRONZE)

# 圣角：抹白的石灰岩，偶有灰斑与细裂
g = grid()
for y in range(16):
    for x in range(16):
        n = noise(x, y, 17)
        g[y][x] = "h" if n > 0.8 else ("n" if n < 0.15 else ("j" if n < 0.18 else "m"))
put("horns_of_consecration", g, GYPSUM)


# ═════════════════════════════ 模型 ═════════════════════════════

COL = {"particle": tex("minoan_column"), "shaft": tex("minoan_column"), "cap": tex("minoan_capital"),
       "base": tex("smooth_marble")}


def shaft(lo, hi, y0, y1, ends=""):
    return cross(lo, hi, y0, y1, 2, "shaft", faces="nsew" + ends)


def socle():
    """柱脚的低矮圆石础。"""
    return cross(1.5, 14.5, 0, 1.5, 3, "base", comment="石础")


def capital(y0):
    """柱头：颈环、黑色圆垫（两层）、方顶板；y0 为颈环底，顶板顶到 16。"""
    return [
        *cross(2, 14, y0, y0 + 1, 2, "cap", faces="nsewd", comment="颈环"),
        *cross(1, 15, y0 + 1, y0 + 3, 3, "cap", faces="nsewd", comment="圆垫下层"),
        *cross(0.25, 15.75, y0 + 3, y0 + 5, 3.5, "cap", faces="nsewd", comment="圆垫上层"),
        box((0, y0 + 5, 0), (16, 16, 16), "cap", comment="方顶板"),
    ]


# 上粗下细：底段 8→9，中段 10→11，顶段 12 + 柱头；单段是一截矮柱
model("minoan_column_bottom", COL, [*socle(), *shaft(4, 12, 1.5, 9), *shaft(3.5, 12.5, 9, 16)])
model("minoan_column_middle", COL, [*shaft(3, 13, 0, 8), *shaft(2.5, 13.5, 8, 16)])
model("minoan_column_top", COL, [*shaft(2, 14, 0, 9), *capital(9)])
model("minoan_column_single", COL, [*socle(), *shaft(3.5, 12.5, 1.5, 9), *capital(9)])

# 双斧门楣：整格；正面的双斧向外凸出 1 像素（逐列一块，正面贴同一张图的对应列）
LIN = {"particle": tex("labrys_lintel_side"), "front": tex("labrys_lintel_front"), "side": tex("labrys_lintel_side"),
       "bronze": tex("labrys_bronze")}
lintel = [box((0, 0, 0), (16, 16, 16), {"north": "front", "south": "side", "east": "side", "west": "side",
                                       "up": "side", "down": "side"}, comment="门楣")]
for x in range(16):
    ys = sorted(y for (cx, y) in LABRYS if cx == x)
    if not ys:
        continue
    # 一列里可能断开（外缘内凹），按连续段各做一块
    runs, start = [], ys[0]
    for a, b in zip(ys, ys[1:] + [None]):
        if b != a + 1:
            runs.append((start, a))
            start = b
    for top, bot in runs:
        lintel.append(box((x, 16 - (bot + 1), -1), (x + 1, 16 - top, 0),
                          {"north": "front", "east": "bronze", "west": "bronze", "up": "bronze", "down": "bronze",
                           "south": "bronze"}, faces="newud",
                          uv={"north": [x, top, x + 1, bot + 1], "east": [0, 0, 1, bot + 1 - top],
                              "west": [0, 0, 1, bot + 1 - top], "up": [0, 0, 1, 1], "down": [0, 0, 1, 1]},
                          comment="双斧浮雕" if x == 7 else None))
model("labrys_lintel", LIN, lintel)

# 圣角（两角沿东西排开）：矮座、两只向上外翻的角、角根之间的 U 形底
HOR = {"particle": tex("horns_of_consecration"), "s": tex("horns_of_consecration")}
horns = [box((1, 0, 5), (15, 3, 11), "s", comment="矮座"),
         box((4, 3, 6), (12, 4, 10), "s", comment="U 形底")]
for side in (0, 1):
    def m(x0, x1):
        return (x0, x1) if side == 0 else (16 - x1, 16 - x0)
    a, b = m(1.5, 5)
    horns.append(box((a, 3, 6), (b, 7, 10), "s", comment="左角" if side == 0 else "右角"))
    a, b = m(1, 4)
    horns.append(box((a, 7, 6.5), (b, 10, 9.5), "s"))
    a, b = m(0.5, 3)
    horns.append(box((a, 10, 7), (b, 12.5, 9), "s"))
    a, b = m(0, 2)
    horns.append(box((a, 12.5, 7.5), (b, 14, 8.5), "s", comment="角尖"))
model("horns_of_consecration", HOR, horns)

print("规格：", " ".join(f"tools/texgen/specs/greek/{n}.json" for n in written))
