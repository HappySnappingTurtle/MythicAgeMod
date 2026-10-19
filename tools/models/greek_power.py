#!/usr/bin/env python3
"""希腊神力方块的模型与贴图规格（T13 阶段 ③）。

生成：
  - 方块模型 JSON：models/block/lure_stone_lower.json、lure_stone_upper.json（诱兽石，GP-GR-02）
  - 像素贴图规格：tools/texgen/specs/greek/lure_stone_*.json（输出到 textures/block/power/）
诱兽石是一块两格高的天然立石（《神话时代》的诱兽石）：扁石板、下宽上窄、略向东倾、顶部东高西低；
下半湿暗、长苔、附藤壶，脚下几块碎石和一丛海草；南北两面刻着褪色的海蓝三叉戟，戟杆从一道卷浪里升起。

精度：贴图 32×32（每像素 2 个贴图像素，与守护神像等展示物件一样高于普通方块），几何以 0.5 像素为单位。
整块石头在 0..31.5 像素高的坐标里建模，按 y=16 切成上下两个方块模型；贴图全部按方块坐标取 UV，
刻纹、裂缝、苔线跨层、跨两格连续。

运行：python3 tools/models/greek_power.py [--preview <合并模型输出路径>]
      && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/lure_stone_*.json
"""
from __future__ import annotations

import json
import math
import sys

from modelgen import box, grid, model, noise, plane, rows, spec, tex

RES = 32  # 贴图边长：每方块像素 2 个贴图像素
FOLDER = "block/power/"  # 神力展示方块的贴图目录：内容校验允许 32×32（普通方块仍是 16×16）

PALETTE = {
    # 石面明暗阶（蓝灰海石）
    "K": "#2b3238",  # 深缝
    "k": "#3f4850",
    "d": "#545f68",
    "m": "#6a767f",
    "s": "#818d95",
    "S": "#97a2a9",
    "h": "#adb7bc",
    "H": "#c4ccd0",
    # 苔
    "o": "#36481f",
    "g": "#4f6a2c",
    "G": "#6c8a38",
    "Y": "#8fa84a",
    # 地衣
    "l": "#a9a874",
    "L": "#cbc690",
    # 藤壶
    "w": "#e6e3d6",
    "W": "#bdb9aa",
    "q": "#8a877b",
    # 刻纹：海蓝填色、磨白、凹槽上沿阴影
    "B": "#4b8d99",
    "b": "#6aa6ae",
    "t": "#24555f",
    # 海草
    "p": "#2f4519",
    "r": "#4c6d24",
    "R": "#6f9033",
}
RAMP = "kdmsShH"


# ───────────── 噪声 ─────────────

def vnoise(x: float, y: float, scale: float, k: int) -> float:
    """平滑值噪声（网格点取 noise，双线性 + smoothstep 插值），0–1。"""
    gx, gy = x / scale, y / scale
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = noise(x0, y0, k) * (1 - sx) + noise(x0 + 1, y0, k) * sx
    b = noise(x0, y0 + 1, k) * (1 - sx) + noise(x0 + 1, y0 + 1, k) * sx
    return a * (1 - sy) + b * sy


def tone(v: float) -> str:
    v = max(0.0, min(0.999, v))
    return RAMP[int(v * len(RAMP))]


# ───────────── 刻纹（贴图像素坐标：ax 0..31 = 方块 x×2，ay 0..63 = 整块石头 y×2，自下而上） ─────────────

def mirror(pts) -> set[tuple[int, int]]:
    """以方块 x=9 为轴左右对称：贴图列 c 与 35−c 对称。"""
    pts = set(pts)
    return pts | {(35 - x, y) for x, y in pts}


def carving() -> set[tuple[int, int]]:
    px: set[tuple[int, int]] = set()
    # 戟杆：2 贴图像素宽，从浪纹里升到横档
    for y in range(16, 46):
        px |= {(17, y), (18, y)}
    # 横档：中间平、两端上翘
    for x in range(13, 18):
        px |= mirror({(x, 44), (x, 45)})
    px |= mirror({(12, 45), (12, 46)})
    # 两侧齿：竖直向上、顶端收尖，中段一对外倒刺
    for y in range(46, 55):
        px |= mirror({(12, y), (13, y)})
    px |= mirror({(12, 55)})
    px |= mirror({(11, 51), (10, 52), (11, 52), (10, 53)})
    # 中齿：比两侧高，顶端是矛头
    for y in range(46, 58):
        px |= {(17, y), (18, y)}
    px |= mirror({(16, 53), (16, 54), (15, 53)})
    px |= {(17, 58), (18, 58)}
    # 卷浪：一条正弦浪线（竖向补齐不断线），每个浪峰下卷一个小涡
    def wave(x):
        return 14 + round(1.6 * math.sin((x - 5) * 2 * math.pi / 8))
    for x in range(5, 31):
        y, prev = wave(x), wave(x - 1)
        px.add((x, y))
        for yy in range(min(y, prev) + 1, max(y, prev)):
            px.add((x, yy))
        if (x - 5) % 8 == 2:
            px |= {(x + 1, y - 2), (x + 2, y - 2)}
    return px


CARVE = carving()


def near_carving(ax: int, ay: int) -> bool:
    return any((ax + dx, ay + dy) in CARVE for dx in (-1, 0, 1) for dy in (-1, 0, 1))


# ───────────── 裂缝与藤壶 ─────────────

def cracks(k: int) -> set[tuple[int, int]]:
    """几道以竖向为主、略带折线的裂缝（立石的竖向石理）。"""
    out: set[tuple[int, int]] = set()
    starts = ((4, 60, 14), (26, 50, 18), (9, 36, 12), (28, 26, 10), (22, 9, 8), (6, 20, 9), (14, 63, 6))
    for i, (x, y, n) in enumerate(starts):
        fx = float(x)
        for step in range(n):
            out.add((round(fx), y - step))
            fx += (noise(i, step, k) - 0.5) * 1.2
            if noise(i, step, k + 1) > 0.85:  # 偶尔横折一下
                out.add((round(fx) + 1, y - step))
    return out


BARNACLES = ((5, 5), (24, 7), (12, 10), (28, 3), (8, 15))


def barnacle_px() -> dict[tuple[int, int], str]:
    """藤壶：2×2 的小丘，左上亮、右下灰（开口），右下投一像素阴影。"""
    out = {}
    for cx, cy in BARNACLES:
        out[(cx, cy + 1)] = "w"
        out[(cx + 1, cy + 1)] = "W"
        out[(cx, cy)] = "W"
        out[(cx + 1, cy)] = "q"
        out[(cx + 2, cy)] = "k"
        out[(cx + 1, cy - 1)] = "k"
    return out


BARN = barnacle_px()


# ───────────── 石面 ─────────────

def mossy(ax: int, ay: int, k: int) -> bool:
    return ay < 30 and vnoise(ax, ay, 4, k + 5) + 0.15 * noise(ax, ay, k + 6) < 0.6 - ay * 0.024


def stone_px(ax: int, ay: int, k: int, crack: set) -> str:
    """整块石头的石面像素：大块明暗 + 细颗粒，下湿暗上亮，两道横向层理，裂缝带受光下沿，苔、地衣、藤壶。"""
    v = 0.55 * vnoise(ax, ay, 7, k) + 0.35 * vnoise(ax, ay, 3, k + 1) + 0.1 * noise(ax, ay, k + 2)
    v = (v - 0.5) * 1.6 + 0.5
    v += 0.18 * (ay / 63 - 0.45)
    if ay < 10:
        v -= 0.2 * (1 - ay / 10)
    strata = 23 + 2.5 * math.sin(ax / 5 + k)
    if abs(ay - strata) < 0.6 or abs(ay - strata - 17) < 0.6:
        v -= 0.16
    c = tone(v)
    if (ax, ay) in crack:
        c = "K"
    elif (ax - 1, ay + 1) in crack or (ax, ay + 1) in crack:
        c = "h" if v > 0.3 else "S"
    # 苔：成簇，越低越密；簇的上沿亮、下沿暗；也爬进下部的裂缝
    if mossy(ax, ay, k) or ((ax, ay) in crack and ay < 20):
        shade = noise(ax, ay, k + 7)
        if not mossy(ax, ay + 1, k) and shade > 0.55:
            c = "Y"
        elif not mossy(ax, ay - 1, k):
            c = "o"
        else:
            c = "G" if shade > 0.6 else "g"
    # 地衣：上半零星成团的圆斑
    lich = vnoise(ax, ay, 3, k + 8)
    if ay >= 30 and lich > 0.83:
        c = "L" if lich > 0.88 else "l"
    return BARN.get((ax, ay), c)


def side(half: int, carve: bool, k: int) -> list[str]:
    """侧面贴图（half 0 = 下半格、1 = 上半格）。行号 r 对应 ay = (RES−1−r) + RES×half。"""
    g = grid(RES, RES)
    crack = cracks(k)
    for r in range(RES):
        for x in range(RES):
            ay = (RES - 1 - r) + RES * half
            if carve and (x, ay) in CARVE:
                # 凹刻：上沿一像素在阴影里，其余填褪色海蓝，少数磨白
                c = "t" if (x, ay + 1) not in CARVE else "b" if noise(x, ay, k + 9) > 0.85 else "B"
            elif carve and near_carving(x, ay):
                # 刻纹周围一圈素面；刻槽下方外侧一像素受光
                c = "h" if (x, ay + 1) in CARVE else "S" if noise(x, ay, k) > 0.5 else "s"
                if ay < 20 and c != "h":
                    c = "m"
            else:
                c = stone_px(x, ay, k, crack)
                if carve and 9 <= x <= 26 and 40 <= ay <= 60 and c in "lL":
                    c = "s"  # 戟头齿间不长地衣：会读成第四根齿
            g[r][x] = c
    return rows(g)


def top(moss: bool) -> list[str]:
    """顶面：上层是带地衣的亮石面，下层台面与碎石顶是苔面（苔簇之间露石）。"""
    g = grid(RES, RES)
    for y in range(RES):
        for x in range(RES):
            v = 0.6 * vnoise(x, y, 5, 61) + 0.4 * noise(x, y, 62)
            if moss:
                if vnoise(x, y, 4, 63) < 0.72:
                    g[y][x] = "Y" if v > 0.7 else "G" if v > 0.45 else "g" if v > 0.2 else "o"
                else:
                    g[y][x] = tone(v * 0.6 + 0.2)
            else:
                c = tone(v * 0.7 + 0.35)
                if vnoise(x, y, 3, 64) > 0.86:
                    c = "L" if v > 0.5 else "l"
                g[y][x] = c
    return rows(g)


def rubble() -> list[str]:
    """碎石：一块块暗湿卵石（错位排布，每块左上亮、右下暗），石缝深，顶部生苔。"""
    g = grid(RES, RES)
    for y in range(RES):
        for x in range(RES):
            ox = (x + (y // 6) * 4) % 8
            oy = y % 6
            v = 0.45 + 0.25 * vnoise(x, y, 3, 71) - 0.06 * (ox + oy) / 6
            c = "K" if ox == 7 or oy == 5 else tone(v + (0.12 if ox == 0 or oy == 0 else 0))
            if y < 3 and noise(x, y, 72) > 0.25:
                c = "g" if noise(x, y, 73) > 0.4 else "o"
            g[y][x] = c
    return rows(g)


def kelp() -> list[str]:
    """海草：五根叶片，画在贴图第 10..21 列、第 16..31 行（方块单位 5..11、8..16），弯向两边、上端变细；
    透明处不渲染。"""
    g = grid(RES, RES)
    for x0, height, sway in ((11, 16, 0.9), (13, 13, -0.7), (15, 15, 0.5), (18, 12, -0.9), (20, 14, 0.6)):
        for i in range(height):
            x = x0 + round(sway * math.sin(i / height * math.pi) * 2.2)
            width = 2 if i < height * 0.55 else 1
            for dx in range(width):
                if 10 <= x + dx < 22:
                    g[RES - 1 - i][x + dx] = "p" if i < 3 else "R" if dx == 0 and i % 3 == 1 else "r"
    return rows(g)


def barnacle() -> list[str]:
    """藤壶小方块：灰白粗糙面。"""
    g = grid(RES, RES)
    for y in range(RES):
        for x in range(RES):
            n = noise(x, y, 13)
            g[y][x] = "W" if n > 0.65 else "q" if n < 0.08 else "w"
    return rows(g)


# ───────────── 几何（方块像素，0.5 精度） ─────────────

# 轮廓关键点 (y, x0, x1, z0, z1)：东西宽、南北薄，向东倾，顶部东高西低
PROFILE = (
    (0, 2, 14, 3, 13),
    (5, 2, 14, 3, 13),
    (10, 2.5, 13.5, 3.5, 12.5),
    (15, 3, 13.5, 3.5, 12),
    (20, 3.5, 13.5, 4, 12),
    (24, 4.5, 13, 4.5, 11.5),
    (27, 5, 12.5, 5, 11),
    (29, 6, 12, 5.5, 10.5),
    (30.5, 7.5, 12, 6, 10),
    (31.5, 9, 11.5, 7, 9),
)
TOP = 31.5


def q(v: float) -> float:
    return round(v * 2) / 2


def profile(y: float) -> tuple:
    for (ya, *a), (yb, *b) in zip(PROFILE, PROFILE[1:]):
        if ya <= y <= yb:
            f = (y - ya) / (yb - ya)
            return tuple(q(p + (r - p) * f) for p, r in zip(a, b))
    return tuple(PROFILE[-1][1:])


def stone_boxes() -> list:
    """三组长块（A 东西长、B 南北长、C 补角）各自按错开的高度分段，段高 2.5–3 像素，每段按轮廓取宽、外缘随机鼓出
    0.5，读作粗糙石面而不是一层层台阶。倒角下半 1.5、上半 1、最窄处 0.5。"""
    out = []
    groups = (("A", "东西长块", 0.0, 3.0, TOP - 1), ("B", "南北长块", 1.5, 3.0, TOP), ("C", "中心块", 0.5, 2.5, TOP - 0.5))
    for gi, (g, name, phase, step, end) in enumerate(groups):
        cuts = [0.0] + [phase + i * step for i in range(1, 20) if phase + i * step < end - 1] + [end]
        for si, (ya, yb) in enumerate(zip(cuts, cuts[1:])):
            x0, x1, z0, z1 = profile(ya)
            b = 1.5 if ya < 16 else 1.0
            if x1 - x0 < 5:
                b = 0.5

            def bump(kk):  # 每段外缘 0 或 +0.5 的起伏
                return 0.5 if noise(si, gi, 80 + kk) > 0.55 else 0.0
            if g == "A":
                f, t = (x0 - bump(1), ya, z0 + b), (x1 + bump(2), yb, z1 - b)
            elif g == "B":
                f, t = (x0 + b, ya, z0 - bump(3)), (x1 - b, yb, z1 + bump(4))
            else:
                f, t = (x0 + b / 2, ya, z0 + b / 2), (x1 - b / 2, yb, z1 - b / 2)
            if t[0] - f[0] >= 1 and t[2] - f[2] >= 1:
                out.append((f, t, f"石体{name} y{ya}–{yb}"))
    return out


# 不规则凸起与崩口外翻：(from, to)
BULGES = (
    ((1.5, 7, 6), (2.5, 11.5, 10)),       # 西侧鼓包
    ((6, 8.5, 12.5), (10, 12, 13.5)),     # 南面鼓包
    ((10, 14, 3), (12.5, 17, 4)),         # 北面崩口外翻
    ((13, 19.5, 7), (14, 23.5, 10)),      # 东肩
    ((3.5, 21, 4), (5.5, 22.5, 5)),       # 北面小瘤
    ((11, 3, 12.5), (13, 5, 13.5)),       # 南面根部瘤
)
BARNACLE_CUBES = (
    ((1.5, 2, 7), (2.5, 3, 8)),
    ((13.5, 1.5, 8), (14.5, 2.5, 9)),
    ((5, 2, 12.5), (6, 3, 13.5)),
    ((10, 4, 2.5), (11, 5, 3.5)),
)
RUBBLE = (
    ((0.5, 0, 10), (3.5, 1, 13.5)),
    ((1, 0, 13.5), (2.5, 0.5, 15)),
    ((12, 0, 0.5), (15, 1.5, 3.5)),
    ((14.5, 0, 3.5), (15.5, 0.5, 5)),
    ((13, 0, 11), (15.5, 1, 13.5)),
    ((1, 0, 1.5), (2.5, 0.5, 3)),
    ((6, 0, 14), (7.5, 0.5, 15)),
)


def face_uv(face: str, frm, to, half: int) -> list[float]:
    """方块坐标 UV（与原版自动 UV 相同，但北面不镜像，使南北两面的刻纹落在同一列）。"""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    oy = 16 * half
    v0, v1 = 16 - (y1 - oy), 16 - (y0 - oy)
    return {"north": [x0, v0, x1, v1], "south": [x0, v0, x1, v1],
            "west": [z0, v0, z1, v1], "east": [16 - z1, v0, 16 - z0, v1],
            "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}[face]


def stone_box(frm, to, comment):
    """石体块（整块坐标）。返回 [(half, element)]，跨 y=16 时切成两段。"""
    out = []
    for half in (0, 1):
        lo, hi = max(frm[1], 16 * half), min(to[1], 16 * half + 16)
        if hi <= lo:
            continue
        f = (frm[0], lo, frm[2])
        t = (to[0], hi, to[2])
        h = "upper" if half else "lower"
        faces = {}
        uv = {}
        for n in ("north", "south", "east", "west", "up", "down"):
            if n in ("north", "south"):
                faces[n] = "front_" + h
            elif n in ("east", "west"):
                faces[n] = "side_" + h
            elif n == "up":
                faces[n] = "moss" if hi <= 13 else "top"
            else:
                faces[n] = "rubble"
            uv[n] = face_uv(n, f, t, half)
        # 底面只有悬空时才看得见；切分处的上下面互相遮住，省掉
        fs = "nsew" + ("u" if hi == to[1] else "") + ("d" if lo == frm[1] and lo > 0 else "")
        el = box((f[0], lo - 16 * half, f[2]), (t[0], hi - 16 * half, t[2]), faces, fs, uv=uv, comment=comment)
        out.append((half, el))
    return out


def separate(boxes: list) -> list:
    """消除同向共面重叠：两块的同一朝向面落在同一平面且有重叠面积时（z-fighting），把后一块的那一面向内收 0.02
    （底面除外）。boxes 为 [(from, to, 注释)]，坐标是整块石头的方块坐标。"""
    out = [(list(f), list(t), c) for f, t, c in boxes]
    for i in range(len(out)):
        for j in range(i):
            fi, ti, _ = out[i]
            fj, tj, _ = out[j]
            for axis in range(3):
                o = [k for k in range(3) if k != axis]
                if any(min(ti[k], tj[k]) - max(fi[k], fj[k]) <= 0 for k in o):
                    continue
                if fi[axis] == fj[axis] and axis != 1:  # 底面不动：贴地的块不能悬空
                    fi[axis] += 0.02
                if ti[axis] == tj[axis]:
                    ti[axis] -= 0.02
    return [(tuple(f), tuple(t), c) for f, t, c in out]


def elements() -> tuple[list, list]:
    halves: tuple[list, list] = ([], [])
    boxes = stone_boxes() + [(f, t, "凸起") for f, t in BULGES]
    for f, t, comment in separate(boxes):
        for half, el in stone_box(f, t, comment):
            halves[half].append(el)
    low = halves[0]
    for f, t in BARNACLE_CUBES:
        low.append(box(f, t, "barnacle", "nsewu", uv={k: [2, 2, 4, 4] for k in
                                                       ("north", "south", "east", "west", "up")}, comment="藤壶"))
    for f, t in RUBBLE:
        low.append(box(f, t, {"north": "rubble", "south": "rubble", "east": "rubble", "west": "rubble",
                              "up": "moss"}, "nsewu", comment="碎石"))
    # 海草两丛：交叉面片，贴图按 1:1 取第 5..11 像素列（窄丛取中间 4 列）
    for cx, cz, w, u0 in ((14, 9, 6, 5), (2.5, 4.5, 4, 6)):
        for angle in (45, -45):
            low.append(plane((cx - w / 2, 0, cz), (cx + w / 2, 8, cz), "kelp", "north",
                             uv=[u0, 8, u0 + w, 16], comment="海草",
                             rotation={"origin": [cx, 0, cz], "axis": "y", "angle": angle}))
    return halves


def main():
    spec("lure_stone_lower_side", side(0, False, 51), PALETTE, RES, FOLDER)
    spec("lure_stone_upper_side", side(1, False, 51), PALETTE, RES, FOLDER)
    spec("lure_stone_lower_front", side(0, True, 57), PALETTE, RES, FOLDER)
    spec("lure_stone_upper_front", side(1, True, 57), PALETTE, RES, FOLDER)
    spec("lure_stone_top", top(False), PALETTE, RES, FOLDER)
    spec("lure_stone_moss", top(True), PALETTE, RES, FOLDER)
    spec("lure_stone_rubble", rubble(), PALETTE, RES, FOLDER)
    spec("lure_stone_kelp", kelp(), PALETTE, RES, FOLDER)
    spec("lure_stone_barnacle", barnacle(), PALETTE, RES, FOLDER)
    t = {"particle": tex("power/lure_stone_upper_side"),
         "side_lower": tex("power/lure_stone_lower_side"), "side_upper": tex("power/lure_stone_upper_side"),
         "front_lower": tex("power/lure_stone_lower_front"), "front_upper": tex("power/lure_stone_upper_front"),
         "top": tex("power/lure_stone_top"), "moss": tex("power/lure_stone_moss"), "rubble": tex("power/lure_stone_rubble"),
         "kelp": tex("power/lure_stone_kelp"), "barnacle": tex("power/lure_stone_barnacle")}
    low, up = elements()
    model("lure_stone_lower", t, low, render_type="minecraft:cutout")
    model("lure_stone_upper", t, up, render_type="minecraft:cutout")
    if "--preview" in sys.argv:
        # 审阅用：上下两段合成一个模型（上段抬高 16），供 Blockbench 预览
        merged = json.loads(json.dumps(low))
        for el in json.loads(json.dumps(up)):
            el["from"][1] += 16
            el["to"][1] += 16
            if "rotation" in el:
                el["rotation"]["origin"][1] += 16
            merged.append(el)
        path = sys.argv[sys.argv.index("--preview") + 1]
        with open(path, "w", encoding="utf-8") as f:
            json.dump({"textures": t, "elements": merged}, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
