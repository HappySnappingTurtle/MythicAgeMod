#!/usr/bin/env python3
"""希腊村庄新方块（T13b 阶段 ③，规划篇 §4.2）：模型 JSON 与像素贴图规格。

第 1 批 墙面与货堆：
  - 白灰墙 plaster：泥砖墙外抹的石灰灰泥，略带暖灰、抹刀痕与发丝裂缝（16×16，整块建筑方块）。
  - 赭红墙裙白灰墙 plaster_dado：下部约三分之一刷赭红、分界处一道刻线（奥林索斯民居常见的红色墙裙 / 勒脚区）。
  - 泥砖 mudbrick：日晒泥砖、掺麦秸，一顺一丁错缝（16×16）。
  - 陶瓮 pithos：大腹储物瓮，腹部两道绳纹凸带、肩部两只小耳；三态（空 / 半 / 满：口里看得见的内容 / 封口陶盖）（32×32）。
  - 双耳瓶堆 amphora_stack：三只尖底运输双耳瓶横卧在两根橄榄木托架上，两只在下、一只压在上面（32×32）。
出处见 V3-希腊-建筑（逐层图与白模篇）新方块一节。

精度：整块建筑方块（墙面）16×16，与其它建筑方块一致；展示物件（陶瓮、瓶堆）32×32（诱兽石的教训：展示物件默认 32 密度）。

运行：python3 tools/models/greek_village.py && python3 tools/texgen/texgen.py --force tools/texgen/specs/greek/village_*.json
"""
from __future__ import annotations

import json
import math

from modelgen import MODELS, box, cross, grid, model, noise, rows, spec, tex

FOLDER = "block/village/"  # 村庄展示方块：内容校验允许 16 或 32


def vtex(name: str) -> str:
    return "mythic_age:block/village/" + name


# ───────────── 噪声 ─────────────

def pnoise(x: float, y: float, scale: int, k: int, period: int = 16) -> float:
    """可平铺的平滑值噪声：网格点按 period/scale 取模，贴图左右上下无缝。"""
    n = max(1, period // scale)
    gx, gy = x / scale, y / scale
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)

    def g(a, b):
        return noise(a % n, b % n, k)

    a = g(x0, y0) * (1 - sx) + g(x0 + 1, y0) * sx
    b = g(x0, y0 + 1) * (1 - sx) + g(x0 + 1, y0 + 1) * sx
    return a * (1 - sy) + b * sy


def ramp(v: float, keys: str) -> str:
    v = max(0.0, min(0.999, v))
    return keys[int(v * len(keys))]


# ───────────── 白灰墙 ─────────────

PLASTER = {"c": "#a69b84", "d": "#c9bfa9", "m": "#d8d0bc", "n": "#e3dccb", "h": "#ece6d7", "H": "#f4efe3",
           "s": "#b6ab93"}
DADO = {"R": "#c46a47", "r": "#a8492e", "o": "#8f3b25", "O": "#d4805b", "l": "#7a3a28", "w": "#e3dccb"}


def plaster_rows(k: int = 3, size: int = 16) -> list[list[str]]:
    g = grid(size, size)
    for y in range(size):
        for x in range(size):
            v = 0.55 * pnoise(x, y, 4, k, size) + 0.3 * pnoise(x, y, 2, k + 7, size) + 0.15 * noise(x, y, k + 3)
            g[y][x] = ramp(0.25 + v * 0.75, "dmnhH")
    # 抹刀痕：两段很淡的水平亮条
    for (y, x0, n) in ((4, 2, 4), (11, 9, 5)):
        for x in range(x0, x0 + n):
            if noise(x, y, k + 11) > 0.4:
                g[y][x % size] = "h"
    # 零星砂粒
    for y in range(size):
        for x in range(size):
            if noise(x, y, k + 21) > 0.992:
                g[y][x] = "s"
    return g


def plaster_dado_rows() -> list[list[str]]:
    g = plaster_rows(5)
    for y in range(11, 16):
        for x in range(16):
            v = 0.6 * pnoise(x, y, 4, 13) + 0.4 * noise(x, y, 17)
            g[y][x] = ramp(v, "orRO")
            if y == 15 and noise(x, y, 19) > 0.55:
                g[y][x] = "o"  # 底边脏污
            if noise(x, y, 23) > 0.96:
                g[y][x] = "w"  # 剥落处露出灰泥
    for x in range(16):
        g[11][x] = "l"  # 刻线（墙裙上沿）
    return g


# ───────────── 泥砖 ─────────────

MUD = {"a": "#a77b50", "b": "#9a6f47", "e": "#b58a5c", "q": "#8a6140", "j": "#6b4e34", "J": "#7d5d40",
       "t": "#d2bb7c", "T": "#c1a866"}


def mudbrick_rows() -> list[list[str]]:
    g = grid(16, 16)
    for y in range(16):
        course = y // 4
        offset = 0 if course % 2 == 0 else 4
        for x in range(16):
            bx = (x + offset) % 16
            brick = (bx // 8, course)
            if y % 4 == 3 or bx % 8 == 7:
                g[y][x] = "j" if noise(x, y, 31) > 0.3 else "J"  # 泥浆缝
                continue
            base = "abe"[int(noise(*brick, 37) * 3)]
            v = noise(x, y, 41)
            g[y][x] = "q" if (y % 4 == 2 and v > 0.5) else (base if v > 0.18 else "q")
    # 麦秸：两像素长的浅色短划，只在砖面上
    for y in range(16):
        for x in range(15):
            if g[y][x] in "abe" and g[y][x + 1] in "abe" and noise(x, y, 43) > 0.9:
                g[y][x] = "t"
                g[y][x + 1] = "T"
    return g


# ───────────── 八角截面（正方体 + 绕轴转 45° 的同尺寸正方体） ─────────────

def octa_y(cx, cz, r, y0, y1, t, faces="nsewud", comment=None) -> list:
    """竖直的八角柱（外接半径约 r）：十字两臂（宽 r）+ 两条转 ±45° 的细长块正好填满四个缺角，
    顶点在 (±r, ±r/2)、(±r/2, ±r)。"""
    h = r / 2
    L, W = 1.5 * r / math.sqrt(2), h / math.sqrt(2)
    els = [box((cx - r, y0, cz - h), (cx + r, y1, cz + h), t, faces, comment=comment),
           box((cx - h, y0, cz - r), (cx + h, y1, cz + r), t, faces)]
    for ang in (45, -45):
        els.append(box((cx - L, y0, cz - W), (cx + L, y1, cz + W), t, faces,
                       rotation={"angle": ang, "axis": "y", "origin": [cx, y0, cz]}))
    return els


def octa_x(x0, x1, cy, cz, r, t, comment=None) -> list:
    """沿 x 轴的八角柱（横卧的瓶身），截面同 {@link octa_y}。"""
    h = r / 2
    L, W = 1.5 * r / math.sqrt(2), h / math.sqrt(2)
    els = [box((x0, cy - h, cz - r), (x1, cy + h, cz + r), t, comment=comment),
           box((x0, cy - r, cz - h), (x1, cy + r, cz + h), t)]
    for ang in (45, -45):
        els.append(box((x0, cy - W, cz - L), (x1, cy + W, cz + L), t,
                       rotation={"angle": ang, "axis": "x", "origin": [x0, cy, cz]}))
    return els


# ───────────── 陶瓮（32×32） ─────────────

CLAY = {"k": "#4a2416", "d": "#6b3622", "m": "#8a4a2e", "s": "#a3603d", "S": "#b8744c", "h": "#ca8a5f",
        "H": "#dba276", "x": "#2a1810", "X": "#3d2418", "g": "#c9a24f", "G": "#e0bf6a", "y": "#a8822f",
        "c": "#8a7258", "C": "#b19a7c", "w": "#d6c7a8"}


def clay_rows(k: int, light: float = 0.0) -> list[list[str]]:
    """陶面：平整的烧成色，大团的冷暖起伏 + 很淡的轮制细纹；越往下越暗（底部烟熏），零星砂粒。"""
    g = grid(32, 32)
    for y in range(32):
        for x in range(32):
            v = 0.45 + 0.18 * (pnoise(x, y, 16, k, 32) - 0.5) + 0.22 * (1 - y / 31) + light
            if y % 6 == 3:
                v -= 0.08
            g[y][x] = ramp(v, "dmsShH")
    return g


def rope_rows() -> list[list[str]]:
    """绳纹凸带：一股股斜向麻绳（周期 8 像素，向右上倾）：股心高光 → 股面 → 背光 → 股间深缝。"""
    g = grid(32, 32)
    shade = "HhhSsmdk"
    for y in range(32):
        for x in range(32):
            g[y][x] = shade[(x + y) % 8]
    return g


def mouth_rows(kind: str) -> list[list[str]]:
    """瓮口（口沿里面的圆口，贴在口沿顶面上）：空——黑洞；半——谷物；满——封口陶盖与十字麻绳。外圈是口沿内壁的阴影。"""
    g = grid(32, 32)
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - 15.5, y - 15.5)
            edge = d > 13
            if kind == "empty":
                g[y][x] = "d" if edge else ("x" if d < 10 else "X")
            elif kind == "half":
                v = noise(x // 2, y // 2, 71)
                g[y][x] = "d" if edge else ("G" if v > 0.6 else "g" if v > 0.25 else "y")
            else:
                g[y][x] = "m" if edge else ("h" if noise(x, y, 75) > 0.2 else "S")
    if kind == "full":
        for i in range(3, 29):
            for c in (15, 16):
                g[i][c] = "C" if i % 3 else "c"
                g[c][i] = "C" if i % 3 else "c"
        g[15][15] = g[16][16] = "w"
    return g


def pithos_elements(fill: str) -> list:
    c = 8
    els = []
    for (h, y0, y1, t, name) in ((2.4, 0, 1, "body", "圈足"), (4.2, 1, 3, "body", "下腹"), (5.3, 3, 4.6, "body", "腹下沿"),
                                 (6.0, 4.6, 10.6, "body", "鼓腹"), (6.45, 5.8, 7.0, "rope", "绳纹凸带（下）"),
                                 (6.45, 8.6, 9.8, "rope", "绳纹凸带（上）"), (5.0, 10.6, 12, "body", "肩"),
                                 (3.8, 12, 13, "body", "肩上"), (2.6, 13, 14.6, "body", "颈")):
        els += octa_y(c, c, h, y0, y1, t, comment=name)
    els += octa_y(c, c, 3.7, 14.6, 15.8, "body", comment="口沿")
    els.append(box((c - 2.6, 15.8, c - 2.6), (c + 2.6, 15.81, c + 2.6), "mouth", faces="u", comment="瓮口（空 / 半 / 满）"))
    els.append(box((1.4, 11, 7.1), (2.6, 13, 8.9), "body", comment="西耳"))
    els.append(box((13.4, 11, 7.1), (14.6, 13, 8.9), "body", comment="东耳"))
    return els


# ───────────── 双耳瓶堆（32×32） ─────────────

def hcyl(x0, x1, cy, cz, r, t, inset=None, comment=None) -> list:
    """沿 x 轴的「十」字截面圆柱（双耳瓶横卧）。"""
    i = inset if inset is not None else r * 0.45
    return [box((x0, cy - r + i, cz - r), (x1, cy + r - i, cz + r), t, comment=comment),
            box((x0, cy - r, cz - r + i), (x1, cy + r, cz + r - i), t)]


def amphora(cy, cz, flip=False, tag="") -> list:
    """一只横卧的尖底双耳瓶：瓶足（尖）→ 下腹收分 → 鼓腹 → 肩 → 颈 → 口沿，颈肩之间一对环耳。flip 时瓶口朝西。"""
    # 运输双耳瓶的轮廓：肩部最宽，往下长长地收成尖足（分四段收分），短颈、厚口沿
    parts = [(0.3, 1.5, 0.55, "瓶足"), (1.5, 3.0, 1.2, "下腹·一"), (3.0, 5.0, 1.8, "下腹·二"), (5.0, 7.8, 2.35, "下腹·三"),
             (7.8, 11.0, 2.8, "肩腹"), (11.0, 12.2, 2.1, "肩"), (12.2, 14.2, 1.0, "颈"), (14.2, 15.2, 1.35, "口沿")]
    els = []
    for (a, b, r, name) in parts:
        x0, x1 = (16 - b, 16 - a) if flip else (a, b)
        els += octa_x(x0, x1, cy, cz, r, "amph", comment=f"{tag}{name}")
    # 环耳：上下各一，从颈部伸出、折向肩部（一段竖、一段横）
    for dy in (1, -1):
        def fx(a, b):
            return (16 - b, 16 - a) if flip else (a, b)
        a0, a1 = fx(12.6, 13.6)
        b0, b1 = fx(11.0, 13.6)
        ya, yb = sorted((cy + dy * 1.1, cy + dy * 2.7))
        yc, yd = sorted((cy + dy * 2.7, cy + dy * 2.0))
        els.append(box((a0, ya, cz - 0.5), (a1, yb, cz + 0.5), "amph", comment=f"{tag}耳"))
        els.append(box((b0, yc, cz - 0.5), (b1, yd, cz + 0.5), "amph"))
    return els


def amphora_elements() -> list:
    els = [box((3, 0, 0.5), (4.6, 1.0, 15.5), "wood", comment="托架（西）"),
           box((10.8, 0, 0.5), (12.4, 1.0, 15.5), "wood", comment="托架（东）")]
    els += amphora(3.6, 3.6, tag="前下·")
    els += amphora(3.6, 12.4, tag="后下·")
    els += amphora(8.6, 8.0, flip=True, tag="上·")
    return els


AMPH = {"k": "#5e321c", "d": "#7f4429", "m": "#9e5c39", "s": "#b8744e", "S": "#c98b64", "h": "#d9a27c",
        "H": "#e6b994", "r": "#3e2014"}


def amphora_rows() -> list[list[str]]:
    """运输双耳瓶：平滑的浅橙陶面，只有大团冷暖起伏（转 45° 的块上细纹会被映射乱，所以不画轮制纹）。"""
    g = grid(32, 32)
    for y in range(32):
        for x in range(32):
            v = 0.5 + 0.22 * (pnoise(x, y, 16, 91, 32) - 0.5) + 0.06 * (noise(x, y, 92) - 0.5)
            g[y][x] = ramp(v, "dmsShH")
    return g


# ───────────── 第 2 批：工作站（32×32，均按朝北绘制，方块状态按朝向旋转） ─────────────

WOOD = {"k": "#3d2c1c", "d": "#574029", "m": "#6e5335", "s": "#86683f", "S": "#9c7b4b", "h": "#b39158", "H": "#c8a86c"}
WETCLAY = {"d": "#6e6252", "m": "#857761", "s": "#9a8b72", "S": "#ad9e83", "h": "#c0b195", "H": "#d2c4a8", "k": "#564c40"}
DAUB = {"k": "#3a2418", "d": "#6b4430", "m": "#87583c", "s": "#a06d4a", "S": "#b5825b", "h": "#c7976d", "H": "#d7ab82",
        "z": "#2b2420", "Z": "#433732"}
FIRE = {"k": "#5a1a08", "d": "#a1300c", "m": "#d24e10", "s": "#ee7a1c", "S": "#f8a531", "h": "#ffd05a", "H": "#fff1a8"}
SOOT = {"k": "#141110", "d": "#221c19", "m": "#2f2723", "s": "#3d332d", "S": "#4a3e36", "h": "#58493f", "H": "#66554a"}
STONE = {"k": "#8f8676", "d": "#a99f8b", "m": "#c4bba8", "s": "#d6cebd", "S": "#e0d9ca", "h": "#ebe5d8", "H": "#f6f2ea"}
METAL = {"k": "#1e1d1f", "d": "#2f2d30", "m": "#423f42", "s": "#565256", "S": "#6b666a", "h": "#857f82", "H": "#a8a2a3"}
LEATHER = {"k": "#2c1a10", "d": "#45291a", "m": "#5d3823", "s": "#74472d", "S": "#8a5737", "h": "#9f6a46", "H": "#b47f58",
           "t": "#c9b48a"}
MUST = {"k": "#1e0a1a", "d": "#33102b", "m": "#4a1840", "s": "#622254", "S": "#7a2d68", "h": "#93407e", "H": "#ad5a95",
        "g": "#3f5a24", "G": "#5b7a32"}


def smooth_rows(k: int, scale: int = 16, amp: float = 0.25, base: float = 0.5, grain=None, keys="dmsShH") -> list[list[str]]:
    """通用的平滑材质：大团明暗；grain = (轴, 周期, 深度) 时加一组纹路（木纹、刻痕）。"""
    g = grid(32, 32)
    for y in range(32):
        for x in range(32):
            v = base + amp * (pnoise(x, y, scale, k, 32) - 0.5) + 0.05 * (noise(x, y, k + 1) - 0.5)
            if grain:
                axis, period, depth = grain
                c = x if axis == "x" else y
                wob = int(2 * pnoise(y if axis == "x" else x, c, 8, k + 2, 32))
                if (c + wob) % period == 0:
                    v -= depth
            g[y][x] = ramp(v, keys)
    return g


def wood_rows(k=101):
    return smooth_rows(k, 16, 0.22, 0.5, ("x", 5, 0.18))


def fire_rows():
    """火口：中心最亮，往外由黄到橙到暗红。"""
    g = grid(32, 32)
    for y in range(32):
        for x in range(32):
            d = math.hypot((x - 15.5) / 15, (y - 22) / 12)
            v = 1.0 - d * 0.7 + 0.15 * (noise(x // 2, y // 2, 111) - 0.5)
            g[y][x] = ramp(v, "kdmsShH")
    return g


def daub_rows(k=121, soot_mouth=True):
    """陶窑 / 熔炉的抹泥外壁：赭褐色、几道干裂缝，炉口上方熏黑。"""
    g = smooth_rows(k, 16, 0.26, 0.52)
    for (x0, y0, n) in ((7, 6, 5), (23, 18, 4)):
        x = x0
        for y in range(y0, y0 + n):
            g[y][x % 32] = "k"
            x += 1 if noise(x, y, k + 3) > 0.6 else 0
    if soot_mouth:
        for y in range(0, 32):
            for x in range(32):
                if abs(x - 15.5) < 8 - y / 6 and y < 20 and noise(x, y, k + 5) > 0.35:
                    g[y][x] = "z" if y > 10 else "Z"
    return g


def stone_rows(k=131):
    """石作台：大理石方石，表面一道道齿凿痕。"""
    return smooth_rows(k, 16, 0.2, 0.55, ("y", 3, 0.1))


def metal_rows(k=141):
    """砧：深灰金属，顶面受锤处磨亮。"""
    g = smooth_rows(k, 8, 0.3, 0.45)
    for y in range(10, 22):
        for x in range(8, 24):
            if noise(x, y, k + 1) > 0.35:
                g[y][x] = "h" if noise(x, y, k + 2) > 0.5 else "S"
    return g


def leather_rows(k=151):
    """风箱：山羊皮囊，褶皱 + 一道缝线。"""
    g = smooth_rows(k, 8, 0.32, 0.5, ("y", 6, 0.16))
    for x in range(32):
        g[16][x] = "t" if x % 3 else "k"
    return g


def must_rows(k=161):
    """葡萄浆：深紫液面，浮着几簇压破的果皮与葡萄梗。"""
    g = smooth_rows(k, 8, 0.3, 0.45)
    for y in range(32):
        for x in range(32):
            if noise(x // 3, y // 3, k + 4) > 0.82:
                g[y][x] = "H" if noise(x, y, k + 5) > 0.5 else "h"
            elif noise(x // 2, y // 2, k + 6) > 0.95:
                g[y][x] = "G" if noise(x, y, k + 7) > 0.5 else "g"
    return g


def ring_y(cx, cz, r, y0, y1, t, comment=None):
    return octa_y(cx, cz, r, y0, y1, t, comment=comment)


def wheel_elements():
    """陶轮：插在地上的锥形轮座 → 厚重的轮盘（八角，直径约 0.9 m）→ 盘心一团湿泥正在拉成罐坯。"""
    els = []
    els += ring_y(8, 8, 2.0, 0, 1.4, "wood", comment="轴窝（地面）")
    els += ring_y(8, 8, 3.2, 1.4, 3.2, "wood", comment="锥形轮座·下")
    els += ring_y(8, 8, 4.8, 3.2, 4.6, "wood", comment="锥形轮座·上")
    els += ring_y(8, 8, 7.4, 4.6, 6.6, "wood", comment="轮盘")
    els += ring_y(8, 8, 3.4, 6.6, 7.8, "clay", comment="罐坯·底")
    els += ring_y(8, 8, 4.2, 7.8, 10.4, "clay", comment="罐坯·腹")
    els += ring_y(8, 8, 2.8, 10.4, 11.8, "clay", comment="罐坯·颈")
    els += ring_y(8, 8, 3.4, 11.8, 12.4, "clay", comment="罐坯·口")
    els.append(box((0.4, 6.6, 7.3), (3.4, 7.3, 8.7), "wood", comment="转轮用的木棍"))
    return els


def kiln_elements(lit):
    """陶窑（两格高）：圆形升焰窑——下部燃烧室、上部装坯室收成穹顶、顶上出烟口；
    正面（北）下方进火口，上方装窑门。"""
    els = []
    els += ring_y(8, 8, 7.7, 0, 2.5, "daub", comment="窑脚（外扩）")
    els += ring_y(8, 8, 7.4, 2.5, 10, "daub", comment="燃烧室")
    els += ring_y(8, 8, 7.2, 10, 17, "daub", comment="装坯室")
    els += ring_y(8, 8, 6.6, 17, 20, "daub", comment="穹顶·1")
    els += ring_y(8, 8, 5.7, 20, 22.6, "daub", comment="穹顶·2")
    els += ring_y(8, 8, 4.5, 22.6, 24.8, "daub", comment="穹顶·3")
    els += ring_y(8, 8, 3.0, 24.8, 26.6, "daub", comment="穹顶·4")
    els += ring_y(8, 8, 1.9, 26.6, 28.4, "daub", comment="出烟口")
    els.append(box((5.2, 0, -0.1), (10.8, 6.4, 1.0), "fire" if lit else "soot", faces="n", comment="进火口（生火时发亮）"))
    els.append(box((4.2, 0, 0.0), (11.8, 7.4, 1.0), "daub", faces="ewu", comment="进火口拱框"))
    els.append(box((5.8, 12, 0.6), (10.2, 16.8, 1.4), "soot", faces="n", comment="装窑门（封泥）"))
    els.append(box((5.2, 11.4, 0.7), (10.8, 12, 1.5), "daub", faces="nu", comment="窑门门槛"))
    els.append(box((7.0, 28.4, 7.0), (9.0, 28.42, 9.0), "fire" if lit else "soot", faces="u", comment="烟口内"))
    return els


def bench_elements():
    """石作台：一块方石当台子，上面立着凿了一半的多立克柱头毛坯（柱身一截 + 圆盘形柱头 + 方顶板）；
    木槌与尖凿搁在台面上，旁边几块石屑。"""
    els = [box((0.5, 0, 1), (15.5, 8.5, 15), "stone", comment="方石台"),
           *ring_y(7, 8, 3.6, 8.5, 11.4, "stone", comment="柱身一截"),
           *ring_y(7, 8, 5.0, 11.4, 13.4, "stone", comment="圆盘形柱头（echinus）"),
           box((1.6, 13.4, 2.6), (12.4, 15.6, 13.4), "stone", comment="方顶板（abacus）"),
           box((12.0, 8.5, 3.6), (15.0, 10.6, 6.6), "wood", comment="木槌头"),
           box((13.0, 8.5, 6.6), (14.0, 9.2, 12.4), "wood", comment="木槌柄"),
           box((12.6, 8.5, 12.6), (15.2, 9.0, 13.3), "metal", comment="尖凿"),
           box((1.4, 8.5, 13.2), (2.8, 9.2, 14.4), "stone", comment="石屑"),
           box((12.0, 8.5, 13.8), (12.9, 9.0, 14.6), "stone")]
    return els


def furnace_elements(lit):
    """青铜熔炉（两格高）：抹泥竖炉，往上收成高烟囱；正面炉口里是熔铜的坩埚（生火时发亮）；
    背后一对山羊皮风箱接陶管（柏林铸造场杯：工人在炉后拉风箱）。"""
    els = []
    els += ring_y(8, 7.4, 6.8, 0, 10, "daub", comment="炉身")
    els += ring_y(8, 7.4, 5.6, 10, 14, "daub", comment="炉肩")
    els += ring_y(8, 7.4, 4.2, 14, 17, "daub", comment="烟囱根")
    els += ring_y(8, 7.4, 3.0, 17, 29, "daub", comment="烟囱")
    els += ring_y(8, 7.4, 3.6, 29, 31, "daub", comment="烟囱口沿")
    els.append(box((5.4, 1.4, 0.5), (10.6, 7.0, 1.4), "fire" if lit else "soot", faces="n", comment="炉口（坩埚）"))
    els.append(box((4.4, 0.8, 0.6), (11.6, 8.0, 1.4), "daub", faces="ewu", comment="炉口拱框"))
    els.append(box((6.8, 30.98, 6.2), (9.2, 31, 8.6), "fire" if lit else "soot", faces="u", comment="烟囱口"))
    for (x0, name) in ((1.6, "西风箱"), (9.2, "东风箱")):
        els.append(box((x0, 0.4, 12.6), (x0 + 5.2, 4.4, 16.0), "leather", comment=name))
        els.append(box((x0 + 1.8, 4.4, 15.2), (x0 + 3.4, 5.0, 16.0), "wood", comment=name + "把手"))
        els.append(box((x0 + 2.0, 1.4, 11.4), (x0 + 3.2, 2.6, 12.6), "clay", comment=name + "陶管"))
    return els


def anvil_elements():
    """希腊式砧：埋在粗木墩里的方砧（没有砧角）；一把锻锤横在砧上，一把火钳靠在木墩边。"""
    els = []
    els += ring_y(8, 8, 6.6, 0, 9.5, "wood", comment="木墩")
    els += [box((4.4, 9.5, 5.0), (11.6, 13.4, 11.0), "metal", comment="砧身"),
            box((3.6, 13.4, 4.2), (12.4, 14.6, 11.8), "metal", comment="砧面"),
            box((6.0, 14.6, 7.0), (10.0, 16.0, 9.0), "metal", comment="锤头"),
            box((10.0, 14.9, 7.6), (15.4, 15.7, 8.4), "wood", comment="锤柄"),
            box((13.4, 0, 1.4), (14.0, 11.0, 2.0), "metal", comment="火钳（一股）",
                rotation={"angle": -22.5, "axis": "z", "origin": [13.7, 0, 1.7]}),
            box((14.2, 0, 1.4), (14.8, 11.0, 2.0), "metal", comment="火钳（另一股）",
                rotation={"angle": -22.5, "axis": "z", "origin": [14.5, 0, 1.7]})]
    return els


def vat_elements():
    """踩葡萄池（lenos，两格长）：石砌浅池，四壁半格高（内侧染着紫色浆迹），池里是紫色葡萄浆；
    正面（北）一根出水口伸出池外。整体坐标 z 0–32（前格 0–16、后格 16–32），由 split 切成两格。"""
    els = [box((0.5, 0, 0.5), (15.5, 1.5, 31.5), "stone", comment="池底"),
           box((0.5, 1.5, 0.5), (15.5, 8.6, 2), "stone", faces="nsewd", comment="北壁"),
           box((0.5, 1.5, 30), (15.5, 8.6, 31.5), "stone", faces="nsewd", comment="南壁"),
           box((0.5, 1.5, 2), (2, 8.6, 30), "stone", faces="nsewd", comment="西壁"),
           box((14, 1.5, 2), (15.5, 8.6, 30), "stone", faces="nsewd", comment="东壁"),
           box((0.5, 8.6, 0.5), (15.5, 8.6, 16), "rim", faces="u", comment="池沿顶面·前（染着浆迹）"),
           box((0.5, 8.6, 16), (15.5, 8.6, 31.5), "rim_back", faces="u", comment="池沿顶面·后"),
           box((2, 1.5, 2), (14, 5.5, 30), "must", faces="u", comment="葡萄浆"),
           box((6.4, 2.0, -2.8), (9.6, 3.8, 0.5), "stone", comment="出水口"),
           box((7.0, 3.4, -2.8), (9.0, 3.42, 0.5), "must", faces="u", comment="出水口里的浆")]
    return els


# ───────────── 多格方块：按格切开 ─────────────

FACE_OF = {("y", 0): "up", ("y", 1): "down", ("z", 0): "south", ("z", 1): "north"}


def split(elements, axis, at=16.0):
    """把跨两格的模型按 axis = at 切成两份：返回 (前一格, 后一格)，后一格坐标平移 -16。
    切口处的面朝向另一格、看不见，去掉。绕其它轴旋转的元素不能被切（会错位），断言保证。"""
    i = {"x": 0, "y": 1, "z": 2}[axis]
    first, second = [], []
    for el in elements:
        lo, hi = el["from"][i], el["to"][i]
        rot = el.get("rotation")
        if rot and rot["axis"] != axis and lo < at < hi:
            assert axis == "y" and rot["axis"] == "y", f"不能切开带旋转的元素：{el.get('__comment')}"
        if hi <= at:
            first.append(el)
            continue
        if lo >= at:
            second.append(shift(el, i, -16))
            continue
        a, b = json.loads(json.dumps(el)), json.loads(json.dumps(el))
        a["to"][i] = at
        a["faces"].pop(FACE_OF[(axis, 0)], None)
        b["from"][i] = at
        b["faces"].pop(FACE_OF[(axis, 1)], None)
        b.pop("__comment", None)
        first.append(a)
        second.append(shift(b, i, -16))
    return first, second


def shift(el, i, d):
    e = json.loads(json.dumps(el))
    e["from"][i] = round(e["from"][i] + d, 4)
    e["to"][i] = round(e["to"][i] + d, 4)
    if "rotation" in e:
        e["rotation"]["origin"][i] = round(e["rotation"]["origin"][i] + d, 4)
    return e


# 两格方块的物品模型（整体缩小显示，同神像）
TALL_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0], "scale": [0.4, 0.4, 0.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, -4, 0], "scale": [0.42, 0.42, 0.42]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}
LONG_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [1.5, 0, -1.5], "scale": [0.45, 0.45, 0.45]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, -2], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 4], "scale": [0.45, 0.45, 0.45]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, -1], "scale": [0.3, 0.3, 0.3]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, -2], "scale": [0.3, 0.3, 0.3]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, -2], "scale": [0.3, 0.3, 0.3]}}


def rim_rows(back=False):
    """池沿顶面：外圈石面，内圈靠池的一侧染着紫色浆迹（贴图中间透明）。两格长的池子前后各一张：
    前格的南边、后格的北边是两格相接处，不画池沿。"""
    g = stone_rows(197 if back else 191)
    for y in range(32):
        for x in range(32):
            edge = min(x, 31 - x, (31 - y) if back else y)
            if edge >= 3:
                g[y][x] = "."
            elif edge == 2:
                g[y][x] = "v" if noise(x // 2, y // 2, 193) > 0.35 else "w"
    return g


# ───────────── 输出 ─────────────

def main():
    # 墙面（16×16，放在 block/village/ 下）
    spec("village_plaster", rows(plaster_rows()), PLASTER, 16, FOLDER)
    spec("village_plaster_dado", rows(plaster_dado_rows()), {**PLASTER, **DADO}, 16, FOLDER)
    spec("village_mudbrick", rows(mudbrick_rows()), MUD, 16, FOLDER)
    # 陶瓮与瓶堆（32×32）
    spec("village_pithos_body", rows(clay_rows(73)), CLAY, 32, FOLDER)
    spec("village_pithos_rope", rows(rope_rows()), CLAY, 32, FOLDER)
    for kind in ("empty", "half", "full"):
        spec(f"village_pithos_mouth_{kind}", rows(mouth_rows(kind)), CLAY, 32, FOLDER)
    spec("village_amphora", rows(amphora_rows()), AMPH, 32, FOLDER)

    for name, t in (("plaster", "village_plaster"), ("mudbrick", "village_mudbrick")):
        (MODELS / f"{name}.json").write_text(json.dumps(
            {"parent": "minecraft:block/cube_all", "textures": {"all": vtex(t)}}, indent=2) + "\n", encoding="utf-8")
    (MODELS / "plaster_dado.json").write_text(json.dumps(
        {"parent": "minecraft:block/cube_bottom_top",
         "textures": {"side": vtex("village_plaster_dado"), "top": vtex("village_plaster"),
                      "bottom": vtex("village_plaster")}}, indent=2) + "\n", encoding="utf-8")
    for fill in ("empty", "half", "full"):
        pt = {"particle": vtex("village_pithos_body"), "body": vtex("village_pithos_body"),
              "rope": vtex("village_pithos_rope"), "mouth": vtex(f"village_pithos_mouth_{fill}")}
        model(f"pithos_{fill}", pt, pithos_elements(fill))
    model("amphora_stack", {"particle": vtex("village_amphora"), "amph": vtex("village_amphora"),
                            "wood": "minecraft:block/jungle_planks_mythic_age_olive_planks"}, amphora_elements())
    # 第 2 批：工作站
    spec("village_wood", rows(wood_rows()), WOOD, 32, FOLDER)
    spec("village_wetclay", rows(smooth_rows(171, 8, 0.25, 0.5, ("y", 4, 0.12))), WETCLAY, 32, FOLDER)
    spec("village_daub", rows(daub_rows(soot_mouth=False)), DAUB, 32, FOLDER)
    spec("village_fire", rows(fire_rows()), FIRE, 32, FOLDER)
    spec("village_soot", rows(smooth_rows(181, 8, 0.3, 0.4)), SOOT, 32, FOLDER)
    spec("village_stone", rows(stone_rows()), STONE, 32, FOLDER)
    spec("village_metal", rows(metal_rows()), METAL, 32, FOLDER)
    spec("village_leather", rows(leather_rows()), LEATHER, 32, FOLDER)
    spec("village_must", rows(must_rows()), MUST, 32, FOLDER)
    w = {"wood": vtex("village_wood"), "clay": vtex("village_wetclay"), "daub": vtex("village_daub"),
         "fire": vtex("village_fire"), "soot": vtex("village_soot"), "stone": vtex("village_stone"),
         "metal": vtex("village_metal"), "leather": vtex("village_leather"), "must": vtex("village_must")}

    def ws(particle, keys):
        return {"particle": w[particle], **{k: w[k] for k in keys}}

    model("pottery_wheel", ws("wood", ("wood", "clay")), wheel_elements())
    for lit in (False, True):
        sfx = "_lit" if lit else ""
        for name, tex_keys, els in (("pottery_kiln", ("daub", "fire", "soot"), kiln_elements(lit)),
                                    ("bronze_furnace", ("daub", "fire", "soot", "leather", "clay", "wood"),
                                     furnace_elements(lit))):
            lower, upper = split(els, "y")
            model(f"{name}_lower{sfx}", ws("daub", tex_keys), lower)
            model(f"{name}_upper{sfx}", ws("daub", tex_keys), upper)
            if not lit:
                model(f"{name}_item", ws("daub", tex_keys), els, display=TALL_DISPLAY)
    model("mason_bench", ws("stone", ("stone", "wood", "metal")), bench_elements())
    model("greek_anvil", ws("metal", ("metal", "wood")), anvil_elements())
    spec("village_vat_rim", rows(rim_rows()), {**STONE, "v": "#6b2a5c", "w": "#8a4a78"}, 32, FOLDER)
    spec("village_vat_rim_back", rows(rim_rows(back=True)), {**STONE, "v": "#6b2a5c", "w": "#8a4a78"}, 32, FOLDER)
    vt = {**ws("stone", ("stone", "must")), "rim": vtex("village_vat_rim"), "rim_back": vtex("village_vat_rim_back")}
    front, back = split(vat_elements(), "z")
    model("treading_vat_front", vt, front, render_type="minecraft:cutout")
    model("treading_vat_back", vt, back, render_type="minecraft:cutout")
    model("treading_vat_item", vt, vat_elements(), render_type="minecraft:cutout", display=LONG_DISPLAY)
    print("第 1 批：plaster、plaster_dado、mudbrick、pithos_{empty,half,full}、amphora_stack")
    print("第 2 批：pottery_wheel、pottery_kiln_{lower,upper}[_lit]、mason_bench、bronze_furnace_{lower,upper}[_lit]、greek_anvil、treading_vat_{front,back}")


if __name__ == "__main__":
    main()
