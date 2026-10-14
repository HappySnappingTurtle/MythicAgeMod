#!/usr/bin/env python3
"""Mythic Age 程序化像素贴图生成器（T6 设计文档 §3）。

在目标尺寸（默认 32×32）上逐像素生成贴图，不缩小概念图。输入是 JSON 规格，输出 PNG。
同一份规格（含 seed）每次生成的结果逐像素相同。

用法：
  python3 tools/texgen/texgen.py                 生成 tools/texgen/specs/ 下的全部规格
  python3 tools/texgen/texgen.py specs/greek/tin_ingot.json ...
  python3 tools/texgen/texgen.py --force         允许覆盖内容不同的已有文件
  python3 tools/texgen/texgen.py --preview DIR   另外输出放大 8 倍的预览图与总览图
  python3 tools/texgen/texgen.py --list-templates

另有「原版造型换色」规格（template 为 recolor），见 recolor.py：不生成像素画，而是生成调色板与图集登记。

规格格式：
  {
    "template": "ingot",               模板名，见 TEMPLATES
    "out": "item/tin_ingot",           相对 textures/ 的路径，不含 .png
    "seed": 11,                        随机种子
    "size": 32,                        可选，16 或 32，默认 32
    "palette": {"outline": "#3c4148", ...},   每个模板要求的颜色键见 REQUIRED_COLORS
    "params": {...}                    可选，模板参数
  }
"""
from __future__ import annotations

import argparse
import json
import math
import random
import sys
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

import recolor

ROOT = Path(__file__).resolve().parents[2]
SPEC_DIR = Path(__file__).resolve().parent / "specs"
DEFAULT_OUT = ROOT / "neoforge/src/main/resources/assets/mythic_age/textures"
DEFAULT_ATLAS = ROOT / "neoforge/src/main/resources" / "assets/minecraft/atlases/blocks.json"

RGBA = tuple[int, int, int, int]
CLEAR: RGBA = (0, 0, 0, 0)


class SpecError(Exception):
    """规格错误：带上文件与字段，便于定位。"""


@dataclass
class Spec:
    path: Path
    template: str
    out: str
    seed: int
    size: int
    palette: dict[str, RGBA]
    params: dict

    def color(self, key: str) -> RGBA:
        return self.palette[key]

    def param(self, key: str, default):
        return self.params.get(key, default)


# ---------------------------------------------------------------- 工具函数

def parse_color(text: str, where: str) -> RGBA:
    if not isinstance(text, str) or not text.startswith("#") or len(text) not in (7, 9):
        raise SpecError(f"{where}：颜色应为 #RRGGBB 或 #RRGGBBAA，实际 {text!r}")
    try:
        r, g, b = int(text[1:3], 16), int(text[3:5], 16), int(text[5:7], 16)
        a = int(text[7:9], 16) if len(text) == 9 else 255
    except ValueError as e:
        raise SpecError(f"{where}：颜色格式错误 {text!r}") from e
    return (r, g, b, a)


class Canvas:
    """简单的像素画布：坐标越界自动忽略；wrap=True 时按环面取模（用于可平铺贴图）。"""

    def __init__(self, size: int, wrap: bool = False):
        self.size = size
        self.wrap = wrap
        self.px: list[list[RGBA]] = [[CLEAR] * size for _ in range(size)]

    def inside(self, x: int, y: int) -> bool:
        return 0 <= x < self.size and 0 <= y < self.size

    def get(self, x: int, y: int) -> RGBA:
        if self.wrap:
            return self.px[y % self.size][x % self.size]
        return self.px[y][x] if self.inside(x, y) else CLEAR

    def set(self, x: int, y: int, c: RGBA) -> None:
        if self.wrap:
            self.px[y % self.size][x % self.size] = c
        elif self.inside(x, y):
            self.px[y][x] = c

    def filled(self, x: int, y: int) -> bool:
        return self.get(x, y)[3] > 0

    def to_image(self) -> Image.Image:
        img = Image.new("RGBA", (self.size, self.size))
        img.putdata([c for row in self.px for c in row])
        return img


def mask_canvas(mask: set[tuple[int, int]], size: int, color_of) -> Canvas:
    c = Canvas(size)
    for (x, y) in mask:
        if 0 <= x < size and 0 <= y < size:
            c.set(x, y, color_of(x, y))
    return c


def edge_pixels(mask: set[tuple[int, int]]) -> set[tuple[int, int]]:
    """mask 中与 mask 外（4 邻域）相邻的像素。"""
    out = set()
    for (x, y) in mask:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in mask:
                out.add((x, y))
                break
    return out


def polygon_mask(points: list[tuple[float, float]], size: int) -> set[tuple[int, int]]:
    """像素中心落在多边形内的像素（偶奇规则），不做抗锯齿。"""
    mask = set()
    n = len(points)
    for y in range(size):
        cy = y + 0.5
        for x in range(size):
            cx = x + 0.5
            inside = False
            for i in range(n):
                x1, y1 = points[i]
                x2, y2 = points[(i + 1) % n]
                if (y1 > cy) != (y2 > cy):
                    t = (cy - y1) / (y2 - y1)
                    if cx < x1 + t * (x2 - x1):
                        inside = not inside
            if inside:
                mask.add((x, y))
    return mask


def ellipse_mask(cx: float, cy: float, rx: float, ry: float, size: int) -> set[tuple[int, int]]:
    mask = set()
    for y in range(size):
        for x in range(size):
            dx = (x + 0.5 - cx) / rx
            dy = (y + 0.5 - cy) / ry
            if dx * dx + dy * dy <= 1.0:
                mask.add((x, y))
    return mask


def clip_border(mask: set[tuple[int, int]], size: int, border: int = 1) -> set[tuple[int, int]]:
    """物品图标四边留出透明边。"""
    return {(x, y) for (x, y) in mask if border <= x < size - border and border <= y < size - border}


def periodic_noise(size: int, cells: int, rng: random.Random) -> list[list[float]]:
    """可平铺的值噪声，取值 0..1：在 cells×cells 的环面网格上做双线性插值。"""
    grid = [[rng.random() for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * size for _ in range(size)]
    for y in range(size):
        gy = y * cells / size
        y0 = int(gy) % cells
        y1 = (y0 + 1) % cells
        ty = gy - int(gy)
        ty = ty * ty * (3 - 2 * ty)
        for x in range(size):
            gx = x * cells / size
            x0 = int(gx) % cells
            x1 = (x0 + 1) % cells
            tx = gx - int(gx)
            tx = tx * tx * (3 - 2 * tx)
            a = grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx
            b = grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx
            out[y][x] = a * (1 - ty) + b * ty
    return out


def fractal_noise(size: int, rng: random.Random, octaves=((4, 0.6), (8, 0.3), (16, 0.1))) -> list[list[float]]:
    layers = [(periodic_noise(size, cells, rng), w) for cells, w in octaves if cells <= size]
    total = sum(w for _, w in layers)
    return [[sum(layer[y][x] * w for layer, w in layers) / total for x in range(size)] for y in range(size)]


def pick_shade(value: float, shades: list[RGBA], thresholds: list[float]) -> RGBA:
    for t, c in zip(thresholds, shades):
        if value < t:
            return c
    return shades[-1]


def shade_by_light(x: int, y: int, cx: float, cy: float, r: float) -> float:
    """以左上方为光源的球面明暗，返回 -1（暗）..1（亮）。"""
    dx, dy = (x + 0.5 - cx) / max(r, 1), (y + 0.5 - cy) / max(r, 1)
    return max(-1.0, min(1.0, -(dx + dy) * 0.7))


# ---------------------------------------------------------------- 模板

def t_ore_overlay(spec: Spec) -> Canvas:
    """矿粒叠层：透明底上的若干矿粒簇，簇内左上亮、右下有暗边（叠在原版石头上使用）。"""
    s, rng = spec.size, random.Random(spec.seed)
    clusters = spec.param("clusters", 6)
    lo, hi = spec.param("clusterSize", [4, 9])
    canvas = Canvas(s, wrap=True)
    taken: set[tuple[int, int]] = set()
    for _ in range(clusters):
        for _attempt in range(30):
            cx, cy = rng.randrange(s), rng.randrange(s)
            if all(((cx - x) % s) ** 2 + ((cy - y) % s) ** 2 > 16 for (x, y) in taken) or not taken:
                break
        cluster = {(cx, cy)}
        target = rng.randint(lo, hi)
        compact = spec.param("compactness", 0.7)
        while len(cluster) < target:
            # 候选为簇的 4 邻域；按「已有邻居数」加权，compactness 越高簇越紧凑（团块而不是细线）
            cands = sorted({((x + dx) % s, (y + dy) % s) for (x, y) in cluster
                            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))} - cluster)
            weights = []
            for (x, y) in cands:
                n = sum(((x + dx) % s, (y + dy) % s) in cluster for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                weights.append(1 + compact * 4 * (n - 1))
            cluster.add(rng.choices(cands, weights=weights)[0])
        taken |= cluster
        for (x, y) in cluster:
            below = ((x + 1) % s, y) not in cluster or (x, (y + 1) % s) not in cluster
            above = ((x - 1) % s, y) not in cluster or (x, (y - 1) % s) not in cluster
            if above and not below:
                c = spec.color("light")
            elif below and not above:
                c = spec.color("dark")
            else:
                c = spec.color("mid")
            canvas.set(x, y, c)
        # 右下侧的阴影像素，让矿粒从石头中「凸出来」
        for (x, y) in cluster:
            for dx, dy in ((1, 1), (1, 0), (0, 1)):
                p = ((x + dx) % s, (y + dy) % s)
                if p not in taken and not canvas.filled(*p):
                    canvas.set(*p, spec.color("shadow"))
        # 每簇一个高光点
        hx, hy = min(cluster, key=lambda p: (p[0] + p[1], p))
        canvas.set(hx, hy, spec.color("highlight"))
    return canvas


def ingot_mask(s: int):
    k = s / 32
    top = [(5 * k, 15 * k), (19 * k, 8 * k), (27 * k, 11 * k), (13 * k, 18 * k)]
    front = [(13 * k, 18 * k), (27 * k, 11 * k), (27 * k, 16 * k), (13 * k, 24 * k)]
    end = [(5 * k, 15 * k), (13 * k, 18 * k), (13 * k, 24 * k), (5 * k, 20 * k)]
    return polygon_mask(top, s), polygon_mask(front, s), polygon_mask(end, s)


def t_ingot(spec: Spec) -> Canvas:
    """斜放的锭：顶面亮、正面中、端面暗，外缘描边；可选角部斑点（accent）用于区分金属。"""
    s, rng = spec.size, random.Random(spec.seed)
    top, front, end = ingot_mask(s)
    front -= top
    end -= top | front
    shape = clip_border(top | front | end, s)
    canvas = Canvas(s)
    for p in top:
        canvas.set(*p, spec.color("light"))
    for p in front:
        canvas.set(*p, spec.color("mid"))
    for p in end:
        canvas.set(*p, spec.color("dark"))
    # 顶面与正面交界处的高光线
    for (x, y) in top:
        if (x, y + 1) in front:
            canvas.set(x, y, spec.color("highlight"))
    for p in edge_pixels(shape):
        canvas.set(*p, spec.color("outline"))
    if "accent" in spec.palette:
        spots = spec.param("accentSpots", 4)
        inner = sorted(top - edge_pixels(shape))
        for p in rng.sample(inner, min(spots, len(inner))):
            canvas.set(*p, spec.color("accent"))
    return canvas


def blob_mask(spec: Spec, rng: random.Random, lumps: int, cx: float, cy: float, spread: float,
              rmin: float, rmax: float) -> tuple[set[tuple[int, int]], list[tuple[float, float, float]]]:
    s = spec.size
    circles = []
    mask: set[tuple[int, int]] = set()
    base = rng.random() * math.tau
    for i in range(lumps):
        # 除中心块外，其余块大致均匀地分布在四周，避免拉成一长条
        ang = base + (i - 1) * math.tau / max(1, lumps - 1) + rng.uniform(-0.4, 0.4)
        dist = 0 if i == 0 else rng.uniform(spread * 0.5, spread)
        r = rng.uniform(rmin, rmax) * s / 32
        ccx, ccy = cx + math.cos(ang) * dist * s / 32, cy + math.sin(ang) * dist * s / 32
        circles.append((ccx, ccy, r))
        mask |= ellipse_mask(ccx, ccy, r, r * 0.9, s)
    return clip_border(mask, s), circles


def t_raw_chunk(spec: Spec) -> Canvas:
    """粗矿：几个团块连成的不规则矿块，边缘有缺口，左上受光；可选矿粒点（accent）。"""
    s, rng = spec.size, random.Random(spec.seed)
    mask, circles = blob_mask(spec, rng, spec.param("lumps", 4), s / 2, s / 2 + 1,
                              spec.param("spread", 6.0), 5.5, 7.5)
    # 边缘侵蚀：随机去掉一部分边缘像素，避免轮廓过圆
    erode = spec.param("erode", 0.15)
    for p in sorted(edge_pixels(mask)):
        if rng.random() < erode:
            mask.discard(p)
    canvas = Canvas(s)
    for (x, y) in mask:
        cx, cy, r = min(circles, key=lambda c: (x + 0.5 - c[0]) ** 2 + (y + 0.5 - c[1]) ** 2 - c[2] ** 2)
        v = shade_by_light(x, y, cx, cy, r) + rng.uniform(-0.25, 0.25)
        c = spec.color("light") if v > 0.3 else spec.color("mid") if v > -0.25 else spec.color("dark")
        canvas.set(x, y, c)
    if "accent" in spec.palette:
        inner = sorted(mask - edge_pixels(mask))
        for p in rng.sample(inner, min(spec.param("accentSpots", 6), len(inner))):
            canvas.set(*p, spec.color("accent"))
    for p in edge_pixels(mask):
        canvas.set(*p, spec.color("outline"))
    return canvas


def t_granular(spec: Spec) -> Canvas:
    """颗粒堆（混合物、粉末）：半椭圆土堆，两组颗粒颜色按比例混合，左上亮。"""
    s, rng = spec.size, random.Random(spec.seed)
    ratio = spec.param("ratio", 0.25)
    mound = {(x, y) for (x, y) in ellipse_mask(s / 2, s * 0.72, s * 0.40, s * 0.38, s) if y < s * 0.82}
    mound = clip_border(mound, s, 2)
    canvas = Canvas(s)
    for (x, y) in mound:
        v = shade_by_light(x, y, s / 2, s * 0.6, s * 0.4) + rng.uniform(-0.35, 0.35)
        group = "b" if rng.random() < ratio else "a"
        key = f"{group}_light" if v > 0.3 else f"{group}_mid" if v > -0.35 else f"{group}_dark"
        canvas.set(x, y, spec.color(key))
    for p in edge_pixels(mound):
        canvas.set(*p, spec.color("outline"))
    return canvas


def stone_base(spec: Spec, rng: random.Random) -> Canvas:
    s = spec.size
    noise = fractal_noise(s, rng)
    shades = [spec.color("dark"), spec.color("mid"), spec.color("light")]
    th = spec.param("thresholds", [0.38, 0.62])
    canvas = Canvas(s, wrap=True)
    for y in range(s):
        for x in range(s):
            canvas.set(x, y, pick_shade(noise[y][x], shades, th))
    return canvas


def add_veins(canvas: Canvas, spec: Spec, rng: random.Random) -> None:
    """平滑的细纹理线：沿一个缓慢变化的方向前进，偶尔断开。"""
    s = canvas.size
    for _ in range(spec.param("veins", 2)):
        x, y = rng.uniform(0, s), rng.uniform(0, s)
        angle = rng.uniform(0.35, 1.2) * rng.choice((1, -1))
        for _step in range(int(s * rng.uniform(0.6, 1.0))):
            if rng.random() > 0.12:
                canvas.set(int(x), int(y), spec.color("vein"))
            angle += rng.uniform(-0.25, 0.25)
            x += math.cos(angle)
            y += math.sin(abs(angle)) * 0.9 + 0.1


def t_stone(spec: Spec) -> Canvas:
    """可平铺石材面：三档明暗的低频噪声 + 稀疏纹理线（vein）。"""
    rng = random.Random(spec.seed)
    canvas = stone_base(spec, rng)
    if "vein" in spec.palette:
        add_veins(canvas, spec, rng)
    return canvas


def t_polished(spec: Spec) -> Canvas:
    """磨制石材：更平整的底色，四边一圈斜面（上左亮、下右暗）。"""
    s, rng = spec.size, random.Random(spec.seed)
    noise = fractal_noise(s, rng, ((4, 0.8), (8, 0.2)))
    canvas = Canvas(s)
    for y in range(s):
        for x in range(s):
            canvas.set(x, y, spec.color("light") if noise[y][x] > spec.param("speckle", 0.72) else spec.color("mid"))
    for i in range(s):
        canvas.set(i, 0, spec.color("highlight"))
        canvas.set(0, i, spec.color("highlight"))
        canvas.set(i, s - 1, spec.color("dark"))
        canvas.set(s - 1, i, spec.color("dark"))
    canvas.set(0, s - 1, spec.color("mid"))
    canvas.set(s - 1, 0, spec.color("mid"))
    return canvas


def t_bricks(spec: Spec) -> Canvas:
    """砖：错缝砌法，砖缝为 mortar 色；每块砖上左一像素亮边，明暗略有差异。可平铺。"""
    s, rng = spec.size, random.Random(spec.seed)
    rows = spec.param("rows", 4)
    cols = spec.param("cols", 2)
    bh, bw = s // rows, s // cols
    noise = fractal_noise(s, rng, ((8, 0.7), (16, 0.3)))
    canvas = Canvas(s, wrap=True)
    for r in range(rows):
        offset = (bw // 2) * (r % 2)
        for col in range(cols):
            tone = rng.uniform(-0.12, 0.12)
            x0, y0 = col * bw + offset, r * bh
            for y in range(y0, y0 + bh):
                for x in range(x0, x0 + bw):
                    lx, ly = x - x0, y - y0
                    if ly == bh - 1 or lx == bw - 1:
                        c = spec.color("mortar")
                    elif ly == 0 or lx == 0:
                        c = spec.color("light")
                    else:
                        v = noise[y % s][x % s] + tone
                        c = spec.color("dark") if v < 0.36 else spec.color("mid")
                    canvas.set(x, y, c)
    return canvas


def t_voronoi_masonry(spec: Spec) -> Canvas:
    """多边形砌石：环面上的 Voronoi 分块（四边可平铺），石块之间是 1 像素的缝；
    每块石头的明暗略有不同，左上边缘受光、右下边缘背光。"""
    s, rng = spec.size, random.Random(spec.seed)
    n = spec.param("stones", 6)
    seeds = [(rng.uniform(0, s), rng.uniform(0, s)) for _ in range(n)]
    tones = [rng.choice(("mid", "mid", "light", "dark")) for _ in range(n)]

    def nearest(x, y):
        best = None
        for i, (sx, sy) in enumerate(seeds):
            dx = min(abs(x + 0.5 - sx), s - abs(x + 0.5 - sx))
            dy = min(abs(y + 0.5 - sy), s - abs(y + 0.5 - sy))
            d = dx * dx + dy * dy
            if best is None or d < best[0]:
                best = (d, i)
        return best[1]

    owner = [[nearest(x, y) for x in range(s)] for y in range(s)]
    canvas = Canvas(s, wrap=True)
    for y in range(s):
        for x in range(s):
            o = owner[y][x]
            right, down = owner[y][(x + 1) % s], owner[(y + 1) % s][x]
            left, up = owner[y][(x - 1) % s], owner[(y - 1) % s][x]
            if right != o or down != o:
                c = spec.color("joint")
            elif left != o or up != o:
                c = spec.color("highlight")
            else:
                c = spec.color(tones[o])
            canvas.set(x, y, c)
    return canvas


def t_bottle(spec: Spec) -> Canvas:
    """细颈陶瓶：圆腹、细颈、瓶口与塞子；腹部一道纹饰带（band）。"""
    s = spec.size
    k = s / 32
    body = ellipse_mask(16 * k, 20 * k, 8 * k, 8.5 * k, s)
    neck = polygon_mask([(13.5 * k, 6 * k), (18.5 * k, 6 * k), (18.5 * k, 13 * k), (13.5 * k, 13 * k)], s)
    lip = polygon_mask([(12 * k, 5 * k), (20 * k, 5 * k), (20 * k, 7 * k), (12 * k, 7 * k)], s)
    stopper = polygon_mask([(14 * k, 2 * k), (18 * k, 2 * k), (18 * k, 5 * k), (14 * k, 5 * k)], s)
    shape = clip_border(body | neck | lip | stopper, s)
    canvas = Canvas(s)
    for (x, y) in shape:
        if (x, y) in stopper:
            c = spec.color("stopper")
        else:
            v = shade_by_light(x, y, 16 * k, 18 * k, 9 * k)
            c = spec.color("light") if v > 0.35 else spec.color("mid") if v > -0.35 else spec.color("dark")
            if (x, y) in body and abs(y + 0.5 - 21 * k) < 1.2 * k:
                c = spec.color("band")
        canvas.set(x, y, c)
    for p in edge_pixels(shape):
        canvas.set(*p, spec.color("outline"))
    return canvas


def t_tile(spec: Spec) -> Canvas:
    """瓦片 / 陶片：两片叠放的弧形瓦，横向条纹表现弧度。"""
    s, rng = spec.size, random.Random(spec.seed)
    k = s / 32
    shape: set[tuple[int, int]] = set()
    pieces = []
    for (ox, oy) in ((4, 6), (11, 13)):
        poly = [((ox + 2) * k, oy * k), ((ox + 17) * k, (oy + 1) * k), ((ox + 15) * k, (oy + 12) * k),
                (ox * k, (oy + 11) * k)]
        m = clip_border(polygon_mask(poly, s), s)
        pieces.append((m, ox, oy))
    canvas = Canvas(s)
    for m, ox, oy in pieces:
        for (x, y) in m:
            band = int((x - ox * k) / (3 * k)) % 3
            c = (spec.color("light"), spec.color("mid"), spec.color("dark"))[band]
            if rng.random() < 0.06:
                c = spec.color("dark")
            canvas.set(x, y, c)
        for p in edge_pixels(m):
            canvas.set(*p, spec.color("outline"))
        shape |= m
    return canvas


def t_plate(spec: Spec) -> Canvas:
    """浅圆盘（俯视略斜）：外沿、内底，左上受光。"""
    s = spec.size
    k = s / 32
    outer = clip_border(ellipse_mask(16 * k, 17 * k, 13 * k, 9 * k, s), s)
    inner = ellipse_mask(16 * k, 16.5 * k, 9.5 * k, 6 * k, s)
    canvas = Canvas(s)
    for (x, y) in outer:
        if (x, y) in inner:
            v = shade_by_light(x, y, 16 * k, 16.5 * k, 9 * k)
            c = spec.color("dark") if v > 0.3 else spec.color("mid")
        else:
            c = spec.color("light") if y < 17 * k else spec.color("mid")
        canvas.set(x, y, c)
    for p in edge_pixels(inner):
        if p[1] < 16.5 * k:
            canvas.set(*p, spec.color("dark"))
    for p in edge_pixels(outer):
        canvas.set(*p, spec.color("outline"))
    return canvas


def t_pixels(spec: Spec) -> Canvas:
    """手绘像素：rows 为 size 行字符串，每个字符是调色板里的一个键，「.」为透明。"""
    canvas = Canvas(spec.size)
    for y, row in enumerate(spec.params["rows"]):
        for x, ch in enumerate(row):
            if ch != ".":
                canvas.set(x, y, spec.color(ch))
    return canvas


TEMPLATES = {
    "pixels": t_pixels,
    "ore_overlay": t_ore_overlay,
    "ingot": t_ingot,
    "raw_chunk": t_raw_chunk,
    "granular": t_granular,
    "stone": t_stone,
    "polished": t_polished,
    "bricks": t_bricks,
    "bottle": t_bottle,
    "voronoi_masonry": t_voronoi_masonry,
    "tile": t_tile,
    "plate": t_plate,
}

REQUIRED_COLORS = {
    "pixels": [],
    "ore_overlay": ["light", "mid", "dark", "shadow", "highlight"],
    "ingot": ["outline", "dark", "mid", "light", "highlight"],
    "raw_chunk": ["outline", "dark", "mid", "light"],
    "granular": ["outline", "a_dark", "a_mid", "a_light", "b_dark", "b_mid", "b_light"],
    "stone": ["dark", "mid", "light"],
    "polished": ["dark", "mid", "light", "highlight"],
    "bricks": ["mortar", "dark", "mid", "light"],
    "bottle": ["outline", "dark", "mid", "light", "band", "stopper"],
    "voronoi_masonry": ["joint", "dark", "mid", "light", "highlight"],
    "tile": ["outline", "dark", "mid", "light"],
    "plate": ["outline", "dark", "mid", "light"],
}

# 物品类模板：四边必须留透明边（pixels 由规格的 params.kind 决定，见 check_content）
ITEM_TEMPLATES = {"ingot", "raw_chunk", "granular", "bottle", "tile", "plate"}
# 方块面模板：必须四边可平铺
TILING_TEMPLATES = {"stone", "bricks", "voronoi_masonry"}


# ---------------------------------------------------------------- 读取与输出

def load_spec(path: Path) -> Spec:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as e:
        raise SpecError(f"{path}：无法读取 JSON：{e}") from e
    if data.get("template") == "pixels":
        data.setdefault("seed", 0)
    for key in ("template", "out", "seed", "palette"):
        if key not in data:
            raise SpecError(f"{path}：缺少字段 {key}")
    template = data["template"]
    if template not in TEMPLATES:
        raise SpecError(f"{path}：未知模板 {template!r}，可选 {sorted(TEMPLATES)}")
    if not isinstance(data["seed"], int):
        raise SpecError(f"{path}：seed 应为整数")
    size = data.get("size", 32)
    if size not in (16, 32):
        raise SpecError(f"{path}：size 只能是 16 或 32")
    out = data["out"]
    if not isinstance(out, str) or not out or out.startswith("/") or ".." in out or out.endswith(".png"):
        raise SpecError(f"{path}：out 应为 textures/ 下的相对路径且不带 .png，实际 {out!r}")
    if not isinstance(data["palette"], dict):
        raise SpecError(f"{path}：palette 应为对象")
    palette = {k: parse_color(v, f"{path} palette.{k}") for k, v in data["palette"].items()}
    missing = [k for k in REQUIRED_COLORS[template] if k not in palette]
    if missing:
        raise SpecError(f"{path}：模板 {template} 缺少颜色 {missing}")
    params = data.get("params", {})
    if not isinstance(params, dict):
        raise SpecError(f"{path}：params 应为对象")
    if template == "pixels":
        check_pixels(path, palette, params, size)
    return Spec(path, template, out, data["seed"], size, palette, params)


def check_pixels(path: Path, palette: dict, params: dict, size: int) -> None:
    rows = params.get("rows")
    if not isinstance(rows, list) or len(rows) != size:
        raise SpecError(f"{path}：pixels 模板需要 params.rows，共 {size} 行")
    for k in palette:
        if len(k) != 1 or k == ".":
            raise SpecError(f"{path}：pixels 的调色板键必须是单个字符且不能是「.」，实际 {k!r}")
    for y, row in enumerate(rows):
        if not isinstance(row, str) or len(row) != size:
            raise SpecError(f"{path}：rows[{y}] 应为 {size} 个字符")
        for x, ch in enumerate(row):
            if ch != "." and ch not in palette:
                raise SpecError(f"{path}：rows[{y}][{x}] 的字符 {ch!r} 不在调色板中")


def render(spec: Spec) -> Image.Image:
    return TEMPLATES[spec.template](spec).to_image()


def write_png(img: Image.Image, target: Path, force: bool) -> str:
    """返回 'created' / 'unchanged' / 'overwritten'；内容不同且未 force 时抛出 SpecError。"""
    if target.exists():
        with Image.open(target) as old:
            same = old.convert("RGBA").tobytes() == img.tobytes() and old.size == img.size
        if same:
            return "unchanged"
        if not force:
            raise SpecError(f"{target} 已存在且内容不同；确认要覆盖时加 --force")
        img.save(target)
        return "overwritten"
    target.parent.mkdir(parents=True, exist_ok=True)
    img.save(target)
    return "created"


def save_preview(images: list[tuple[str, Image.Image]], out_dir: Path, scale: int = 8) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    tiles = []
    for name, img in images:
        big = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
        bg = Image.new("RGBA", big.size, (120, 120, 120, 255))
        bg.alpha_composite(big)
        bg.save(out_dir / (name.replace("/", "__") + ".png"))
        tiles.append(bg)
    if tiles:
        w = max(t.width for t in tiles)
        h = max(t.height for t in tiles)
        cols = min(4, len(tiles))
        rows = (len(tiles) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * (w + 8), rows * (h + 8)), (60, 60, 60, 255))
        for i, t in enumerate(tiles):
            sheet.paste(t, ((i % cols) * (w + 8), (i // cols) * (h + 8)))
        sheet.save(out_dir / "_sheet.png")


def collect_specs(args: list[str], spec_dir: Path = SPEC_DIR) -> list[Path]:
    if not args:
        return sorted(spec_dir.rglob("*.json"))
    paths = []
    for a in args:
        p = Path(a)
        if not p.is_absolute() and not p.exists():
            p = Path(__file__).resolve().parent / a
        paths.extend(sorted(p.rglob("*.json")) if p.is_dir() else [p])
    return paths


def is_recolor(path: Path) -> bool:
    try:
        return json.loads(path.read_text(encoding="utf-8")).get("template") == "recolor"
    except (OSError, json.JSONDecodeError, AttributeError):
        return False


def write_recolor(out_root: Path, atlas_file: Path, spec_dir: Path, vanilla) -> dict:
    """换色输出总是由规格目录中的全部 recolor 规格重建（图集文件只能有一份）。"""
    specs = []
    for p in sorted(spec_dir.rglob("*.json")):
        if is_recolor(p):
            try:
                specs.append(recolor.load_recolor_spec(p, json.loads(p.read_text(encoding="utf-8"))))
            except recolor.RecolorError as e:
                raise SpecError(str(e)) from e
    try:
        result = recolor.build(specs, vanilla)
    except recolor.RecolorError as e:
        raise SpecError(str(e)) from e
    palette_dir = out_root / recolor.PALETTE_DIR
    wanted = {palette_dir / f"{i}.png": img for i, img in result.palettes.items()}
    wanted.update({palette_dir / "key" / f"{k}.png": img for k, img in result.keys.items()})
    if palette_dir.exists():
        for old in palette_dir.rglob("*.png"):
            if old not in wanted:
                old.unlink()
                print(f"removed     {old.relative_to(out_root)}")
    for target, img in sorted(wanted.items()):
        status = write_png(img, target, force=True)
        if status != "unchanged":
            print(f"{status:11s} {target.relative_to(out_root)}")
    text = recolor.atlas_text(result.atlas)
    if specs or atlas_file.exists():
        if not atlas_file.exists() or atlas_file.read_text(encoding="utf-8") != text:
            atlas_file.parent.mkdir(parents=True, exist_ok=True)
            atlas_file.write_text(text, encoding="utf-8")
            print(f"updated     {atlas_file.name}（{len(specs)} 个换色）")
    return result.previews


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Mythic Age 程序化像素贴图生成器")
    ap.add_argument("specs", nargs="*", help="只生成这些像素规格；换色规格总是全部重建")
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    ap.add_argument("--atlas", type=Path, default=DEFAULT_ATLAS)
    ap.add_argument("--spec-dir", type=Path, default=SPEC_DIR)
    ap.add_argument("--vanilla", type=Path, help="原版资源目录（默认读取 MDG 下载的资源 jar）")
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--preview", type=Path)
    ap.add_argument("--list-templates", action="store_true")
    a = ap.parse_args(argv)
    if a.list_templates:
        for name in sorted(TEMPLATES):
            print(f"{name}: {', '.join(REQUIRED_COLORS[name])}")
        print("recolor: 见 tools/texgen/recolor.py 文件头")
        return 0
    try:
        paths = [p for p in collect_specs(a.specs, a.spec_dir) if not is_recolor(p)]
        specs = [load_spec(p) for p in paths]
        rendered = [(s, render(s)) for s in specs]
        previews = write_recolor(a.out, a.atlas, a.spec_dir, recolor.VanillaAssets(a.vanilla))
        for s, img in rendered:
            status = write_png(img, a.out / (s.out + ".png"), a.force)
            print(f"{status:11s} {s.out}.png  ({s.template}, seed {s.seed})")
    except SpecError as e:
        print(f"错误：{e}", file=sys.stderr)
        return 2
    if a.preview:
        save_preview([(s.out, img) for s, img in rendered] + sorted(previews.items()), a.preview)
    return 0


if __name__ == "__main__":
    sys.exit(main())
