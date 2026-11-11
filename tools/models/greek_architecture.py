#!/usr/bin/env python3
"""希腊建筑套件的模型与贴图规格（T8b 设计文档 §2、§3）。

生成模型 JSON（models/block/）与 texgen 像素规格（tools/texgen/specs/greek/arch_*.json 等）。
模型按「正面朝北（-Z）」绘制；有轴向的方块（爱奥尼柱头、屋脊）由 _z 模型旋转 90° 派生 _x。
运行：python3 tools/models/greek_architecture.py && python3 tools/texgen/texgen.py --force <生成的规格>
"""
from __future__ import annotations

import json
import math

from modelgen import (MARBLE, SPECS, TERRACOTTA, box, cross, grid, model, noise, plane, rotate_y90, rows, spec, tex)

M = dict(MARBLE)
written: list[str] = []


def put(name, g, palette):
    spec(name, rows(g) if isinstance(g[0], list) else g, palette)
    written.append(name)


# ═════════════════════════════ 贴图 ═════════════════════════════

# 柱身凹槽：每道凹槽 4 像素（亮棱、槽面、暗面、槽底），竖向无缝；轻微的石纹噪点
g = grid()
for y in range(16):
    for x in range(16):
        c = "hmnj"[x % 4]
        if c == "m" and noise(x, y, 3) > 0.9:
            c = "n"
        g[y][x] = c
put("marble_column", g, M)

# 柱头、柱础的线脚：横向的亮暗带
put("marble_molding", [
    "hhhhhhhhhhhhhhhh", "mmmmmmmmmmmmmmmm", "mmmmmmmmmmmmmmmm", "nnnnnnnnnnnnnnnn",
    "jjjjjjjjjjjjjjjj", "hhhhhhhhhhhhhhhh", "mmmmmmmmmmmmmmmm", "nnnnnnnnnnnnnnnn",
    "jjjjjjjjjjjjjjjj", "hhhhhhhhhhhhhhhh", "mmmmmmmmmmmmmmmm", "mmmmmmmmmmmmmmmm",
    "nnnnnnnnnnnnnnnn", "jjjjjjjjjjjjjjjj", "hhhhhhhhhhhhhhhh", "nnnnnnnnnnnnnnnn"], M)

# 爱奥尼：卵锚饰（卵形与箭头交替）
put("ionic_echinus", [
    "jjjjjjjjjjjjjjjj",
    "dmhmdmhmdmhmdmhm",
    "jmhnjmhnjmhnjmhn",
    "jmhnjmhnjmhnjmhn",
    "djnjdjnjdjnjdjnj",
    "hhhhhhhhhhhhhhhh",
] + ["mmmmmmmmmmmmmmmm"] * 10, M)

# 爱奥尼涡卷带：两端各一个螺旋，中间是下垂的凹槽（前 6 行为涡卷带，其余为平面）
put("ionic_volute", [
    "hhhhhhhhhhhhhhhh",
    "jjjjjnnnnnnjjjjj",
    "jhmmjmmmmmmjmmhj",
    "jmdhjnmmmmnjhdmj",
    "jmmmjjnnnnjjmmmj",
    "jjjjjjjjjjjjjjjj",
] + ["mmmmmmmmmmmmmmmm"] * 10, M)

# 涡卷的螺旋盘（左上 6×6 有效）：外圈、两圈螺线、中心「眼」
put("ionic_spiral", [
    "njjjjnmmmmmmmmmm",
    "jhhhmjmmmmmmmmmm",
    "jhjjmjmmmmmmmmmm",
    "jhjdmjmmmmmmmmmm",
    "jmmmnjmmmmmmmmmm",
    "njjjjnmmmmmmmmmm",
] + ["mmmmmmmmmmmmmmmm"] * 10, M)

# 爱奥尼侧面的「枕垫」：卷起的垫子，中间一道束带
put("ionic_cushion", [
    "hhhhhhhhhhhhhhhh",
    "nmhhmnnjjnnmhhmn",
    "nmhhmnnjjnnmhhmn",
    "jmhhmjnjjnjmhhmj",
    "jnmmnjnjjnjnmmnj",
    "jjjjjjjjjjjjjjjj",
] + ["mmmmmmmmmmmmmmmm"] * 10, M)

# 科林斯钟面（不透明）：自上而下为螺旋茎（3–5 行）、上排茛苕叶（6–10 行）、下排叶（11–15 行）；叶间是深色钟面
g = grid(fill="j")
for y in range(16):
    for x in range(16):
        if noise(x, y, 5) > 0.8:
            g[y][x] = "k"


def leaf(g, cx, top, bottom, width):
    for y in range(top, bottom + 1):
        t = (y - top) / max(1, bottom - top)
        half = max(0, round(width / 2 * min(1.0, 0.3 + t * 1.3)))
        for x in range(cx - half, cx + half + 1):
            xx = x % 16
            if x == cx:
                g[y][xx] = "n"
            elif x < cx:
                g[y][xx] = "h" if x == cx - half else "m"
            else:
                g[y][xx] = "n" if x == cx + half else "m"
    g[top][(cx - 1) % 16] = "h"  # 叶尖向外卷


for cx in (2, 6, 10, 14):
    leaf(g, cx, 11, 15, 4)
for cx in (0, 4, 8, 12):
    leaf(g, cx, 6, 10, 4)
for cx in (2, 10):  # 螺旋茎（helix）
    for (dx, dy) in ((0, 5), (0, 4), (1, 3), (2, 3), (3, 4), (2, 5)):
        g[dy][(cx + dx) % 16] = "h"
put("corinthian_leaves", g, M)

# 三陇板：两道整槽、两侧半槽；下方一条横带与一排水滴（guttae）
put("triglyph_frieze_triglyph", [
    "hhhhhhhhhhhhhhhh",
    "jjjjjjjjjjjjjjjj",
    "jhmnddhmmnddhmnj",
    "jhmnddhmmnddhmnj",
    "jhmnddhmmnddhmnj",
    "jhmnddhmmnddhmnj",
    "jhmnddhmmnddhmnj",
    "jhmnddhmmnddhmnj",
    "jhmnddhmmnddhmnj",
    "jhmnddhmmnddhmnj",
    "jhmnddhmmnddhmnj",
    "jjjjjjjjjjjjjjjj",
    "hhhhhhhhhhhhhhhh",
    "nnnnnnnnnnnnnnnn",
    "mhjmhjmhjmhjmhjm",
    "mmmmmmmmmmmmmmmm",
], M)

# 陇间板：爱琴蓝底上的浮雕圆盾（原物彩绘，T6 §5.3 的彩绘色）
g = grid(fill="u")
for x in range(16):
    g[0][x] = "h"
    g[1][x] = "j"
    g[15][x] = "n"
for y in range(2, 15):
    g[y][0] = "n"
    g[y][15] = "j"
for y in range(2, 15):
    for x in range(1, 15):
        d = math.hypot(x - 7.5, y - 8.5)
        if d <= 5.2:
            g[y][x] = "m" if d > 4.0 else ("h" if d < 1.2 else "n")
            if d > 4.0 and (x + y) > 16:
                g[y][x] = "j"
        elif d <= 5.8 and (x + y) > 16:
            g[y][x] = "U"
put("triglyph_frieze_metope", g, dict(M, u="#2f6e9e", U="#21547a"))

# 檐口：上半是挑出的檐板（顶部线脚与一道彩绘），下半是齿饰与墙面；底面是齿饰网格
put("marble_cornice_side", [
    "hhhhhhhhhhhhhhhh",
    "uuuuuuuuuuuuuuuu",
    "nnnnnnnnnnnnnnnn",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "nnnnnnnnnnnnnnnn",
    "jjjjjjjjjjjjjjjj",
    "hmdhmdhmdhmdhmdh",
    "hmdhmdhmdhmdhmdh",
    "jkdjkdjkdjkdjkdj",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "nnnnnnnnnnnnnnnn",
], dict(M, u="#2f6e9e"))
put("marble_cornice_bottom", ["hmjdhmjdhmjdhmjd"] * 7 + ["jjjjjjjjjjjjjjjj"] + ["mmmmmmmmmmmmmmmm"] * 8, M)

# 赤陶瓦：盖瓦（圆凸、亮）与板瓦（平）交替，顺坡每 8 行一道搭接阴影
T = dict(TERRACOTTA)
g = grid()
for y in range(16):
    for x in range(16):
        c = "sRro"[x % 4]
        if y % 8 == 7 and c == "o":
            c = "s"
        elif y % 8 == 0 and c in "Rr":
            c = "R"
        if c == "o" and noise(x, y, 7) > 0.85:
            c = "r"
        g[y][x] = c
put("terracotta_roof_tiles", g, T)
put("terracotta_roof_side", ["RRRRRRRRRRRRRRRR", "rrrrrrrrrrrrrrrr", "oooooooooooooooo", "ssssssssssssssss"] * 4, T)
g = grid()
for y in range(16):
    for x in range(16):
        g[y][x] = "s" if noise(x, y, 11) > 0.3 else "o"
put("terracotta_roof_under", g, T)
put("terracotta_ridge_cap", ["RRRRsRRRRsRRRRsR", "rrrrsrrrrsrrrrsr", "rrrrsrrrrsrrrrsr", "oooosoooosoooos o".replace(" ", "")[:16],
                             "ssssssssssssssss"] + ["rrrrsrrrrsrrrrsr"] * 11, T)

# 瓦当：赤陶底、黑彩的棕榈叶纹立板，两块并排（下 9 行为可见区域）
put("terracotta_antefix", [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "...r.r....r.r...",
    "..rbrbr..rbrbr..",
    ".rbrbrbrrbrbrbr.",
    ".rbbbbbrrbbbbbr.",
    ".rrbbbrrrrbbbrr.",
    "..rrbrr..rrbrr..",
    "..rRRRr..rRRRr..",
    "..rrrrr..rrrrr..",
    "..sssss..sssss..",
], dict(T))

# 卵石马赛克：32×32 的一幅（深色卵石底、白色回纹边框、中央白色八瓣圆花），切成四个象限
P = {"K": "#2b2826", "k": "#3a3633", "W": "#e8e4da", "w": "#d4cec2", "g": "#9a948a"}
big = [["K"] * 32 for _ in range(32)]


def white(x, y):
    big[y][x] = "W"


# 边框：内缩 1 的白色回纹带（每 8 像素一个「回」）
for i in range(32):
    for t in (1, 30):
        white(i, t)
        white(t, i)
key = ["11111110", "10000010", "10111010", "10100010", "10111110"]
for side in range(4):
    for i in range(3, 29):
        for j, row in enumerate(key[:4]):
            if row[i % 8] == "1":
                x, y = (i, 3 + j)
                pts = {0: (x, y), 1: (31 - x, 31 - y), 2: (y, 31 - x), 3: (31 - y, x)}[side]
                white(*pts)
# 中央圆框与八瓣花
for y in range(32):
    for x in range(32):
        d = math.hypot(x - 15.5, y - 15.5)
        a = math.atan2(y - 15.5, x - 15.5)
        if 8.6 <= d <= 9.6:
            white(x, y)
        elif d <= 7.4 and d >= 1.5 and math.cos(8 * a) > 0.35 - d * 0.03:
            white(x, y)
        elif d < 1.5:
            big[y][x] = "w"
# 卵石质感：每个像素按噪点取明暗，少量灰缝
for y in range(32):
    for x in range(32):
        n = noise(x, y, 19)
        if big[y][x] == "K":
            big[y][x] = "k" if n > 0.6 else "K"
        elif big[y][x] == "W":
            big[y][x] = "w" if n > 0.65 else "W"
        if n > 0.93:
            big[y][x] = "g"
for q, (ox, oy) in enumerate([(0, 0), (16, 0), (0, 16), (16, 16)]):
    put(f"pebble_mosaic_q{q}", ["".join(big[oy + y][ox:ox + 16]) for y in range(16)], P)


# ═════════════════════════════ 模型 ═════════════════════════════

COL = {"particle": tex("smooth_marble"), "flute": tex("marble_column"), "end": tex("marble_pillar_top"),
       "smooth": tex("smooth_marble"), "molding": tex("marble_molding")}


def shaft(lo, hi, y0=0, y1=16, ends="ud"):
    """柱身一段：十字截面（四角切去 2），侧面贴凹槽，端面贴圆截面。"""
    t = {"north": "flute", "south": "flute", "east": "flute", "west": "flute", "up": "end", "down": "end"}
    return cross(lo, hi, y0, y1, 2, t, faces="nsew" + ends)


# 柱身：底段、中段、单段截面 12；顶段截面 11（收分）
model("marble_column_single", COL, shaft(2, 14))
model("marble_column_bottom", COL, shaft(2, 14, ends="d"))
model("marble_column_middle", COL, shaft(2, 14, ends=""))
model("marble_column_top", COL, shaft(2.5, 13.5, ends="u"))

# 柱础（阿提卡式）：方底板、下圆环、凹槽、上圆环，上接一截柱身
model("marble_column_base", COL, [
    box((0, 0, 0), (16, 3, 16), "smooth", comment="方底板"),
    *cross(1, 15, 3, 6, 2.5, "molding", comment="下圆环"),
    *cross(2.5, 13.5, 6, 7, 2, "smooth", faces="nsew"),
    *cross(1.5, 14.5, 7, 9, 2.5, "molding", comment="上圆环"),
    *shaft(2, 14, 9, 16, ends=""),
])

# 多立克柱头：一截柱身、颈环、两层外挑的圆垫（echinus）、方顶板（abacus）
model("doric_capital", COL, [
    *shaft(2.5, 13.5, 0, 7, ends="d"),
    *cross(2.25, 13.75, 7, 8, 2, "molding", faces="nsew", comment="颈环"),
    *cross(1.5, 14.5, 8, 10, 2.5, "molding", comment="圆垫下层"),
    *cross(0.5, 15.5, 10, 12, 3, "molding", comment="圆垫上层"),
    box((0, 12, 0), (16, 16, 16), "smooth", comment="方顶板"),
])

# 爱奥尼柱头（_z：涡卷面朝南北）
ION = dict(COL, echinus=tex("ionic_echinus"), volute=tex("ionic_volute"), cushion=tex("ionic_cushion"),
           spiral=tex("ionic_spiral"))
ionic = [
    *shaft(2.5, 13.5, 0, 6, ends="d"),
    *cross(1.5, 14.5, 6, 9, 2, "echinus", faces="nsew", uv={k: [0, 0, 16, 6] for k in ("north", "south", "east", "west")}),
    box((-1, 8, 3), (17, 14, 13), {"north": "volute", "south": "volute", "east": "cushion", "west": "cushion",
                                   "up": "smooth", "down": "smooth"},
        uv={"north": [0, 0, 16, 6], "south": [0, 0, 16, 6], "east": [0, 0, 16, 6], "west": [0, 0, 16, 6]},
        comment="涡卷带（两侧各挑出 1 像素）"),
    box((1, 14, 1), (15, 16, 15), "smooth", comment="方顶板"),
]
for x0 in (-1.5, 11.5):
    for (z0, z1, face) in ((2, 3, "n"), (13, 14, "s")):
        ionic.append(box((x0, 8, z0), (x0 + 6, 14, z1), {"north": "spiral", "south": "spiral", "east": "smooth",
                                                         "west": "smooth", "up": "smooth", "down": "smooth"},
                         faces=face + "ewud", uv={"north": [0, 0, 6, 6], "south": [0, 0, 6, 6]},
                         comment="螺旋盘" if x0 < 0 and face == "n" else None))
model("ionic_capital_z", ION, ionic)
model("ionic_capital_x", ION, rotate_y90(ionic))

# 科林斯柱头：上大下小的倒钟形（三层逐渐外扩），钟面画两排茛苕叶与螺旋茎；四角小涡卷托起方顶板
COR = dict(COL, leaves=tex("corinthian_leaves"))
lv = lambda v0, v1: {k: [0, v0, 16, v1] for k in ("north", "south", "east", "west")}
cor = [
    *cross(2, 14, 0, 5, 2, "leaves", faces="nsewd", uv=lv(11, 16)),
    *cross(1.25, 14.75, 5, 10, 2.5, "leaves", faces="nsewd", uv=lv(6, 11)),
    *cross(0.5, 15.5, 10, 13, 3, "leaves", faces="nsewd", uv=lv(3, 6)),
    box((0.5, 13, 0.5), (15.5, 14, 15.5), "molding", comment="顶板下沿"),
    box((0, 14, 0), (16, 16, 16), "smooth", comment="方顶板"),
]
for (x, z) in ((-0.5, -0.5), (13.5, -0.5), (-0.5, 13.5), (13.5, 13.5)):
    cor.append(box((x, 11, z), (x + 3, 14, z + 3), "molding", comment="角涡卷" if x < 0 and z < 0 else None))
model("corinthian_capital", COR, cor)

# 额枋：三道逐层挑出的横带（三重额枋）与顶部束带
model("marble_architrave", COL, [
    box((0, 0, 3), (16, 16, 16), "smooth", faces="sewud", comment="主体"),
    box((0, 0, 2), (16, 5, 3), "smooth", faces="newd", comment="下带"),
    box((0, 5, 1), (16, 10, 3), "smooth", faces="newd", comment="中带"),
    box((0, 10, 0.5), (16, 14, 3), "smooth", faces="newd", comment="上带"),
    box((0, 14, 0), (16, 16, 3), "molding", faces="newud", comment="束带",
        uv={"north": [0, 0, 16, 2], "east": [0, 0, 3, 2], "west": [0, 0, 3, 2]}),
])

# 三陇板饰带：整格，正面贴三陇板或陇间板
for alt in ("triglyph", "metope"):
    model(f"triglyph_frieze_{alt}", {"particle": tex("smooth_marble"), "front": tex(f"triglyph_frieze_{alt}"),
                                     "smooth": tex("smooth_marble")},
          [box((0, 0, 0), (16, 16, 16), {"north": "front", "south": "smooth", "east": "smooth", "west": "smooth",
                                         "up": "smooth", "down": "smooth"})])

# 屋脊（_z：脊线沿 z）：下半赤陶瓦，上面两层逐渐收窄的脊瓦
RID = {"particle": tex("terracotta_roof_tiles"), "tiles": tex("terracotta_roof_tiles"), "cap": tex("terracotta_ridge_cap"),
       "under": tex("terracotta_roof_under")}
ridge = [
    box((0, 0, 0), (16, 8, 16), {"north": "under", "south": "under", "east": "under", "west": "under", "up": "tiles",
                                 "down": "under"}, comment="瓦面"),
    box((3, 8, 0), (13, 11, 16), "cap", faces="nsewu", comment="脊瓦"),
    box((5, 11, 0), (11, 13, 16), "cap", faces="nsewu"),
]
model("terracotta_ridge_z", RID, ridge)
model("terracotta_ridge_x", RID, rotate_y90(ridge))

# 瓦当：贴在屋檐边缘的立板（正面朝北）与一条压边
model("terracotta_antefix", {"particle": tex("terracotta_roof_tiles"), "face": tex("terracotta_antefix"),
                             "under": tex("terracotta_roof_under")}, [
    plane((0, 0, 15), (16, 9, 15), "face", "north", uv=[0, 7, 16, 16], comment="棕榈纹立板"),
    box((0, 0, 14), (16, 1, 16), "under", comment="压边"),
], render_type="minecraft:cutout")

# 卵石马赛克：四个象限各一个整格模型
for q in range(4):
    m = {"parent": "minecraft:block/cube_all", "textures": {"all": tex(f"pebble_mosaic_q{q}")}}
    from modelgen import MODELS
    (MODELS / f"pebble_mosaic_q{q}.json").write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8")

print("规格：", " ".join(f"tools/texgen/specs/greek/{n}.json" for n in written))
