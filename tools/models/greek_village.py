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
    print("第 1 批：plaster、plaster_dado、mudbrick、pithos_{empty,half,full}、amphora_stack")


if __name__ == "__main__":
    main()
