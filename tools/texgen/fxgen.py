#!/usr/bin/env python3
"""Mythic Age 神力表现贴图生成器（T13 设计文档 §1.3）。

生成三类贴图到 neoforge/src/main/resources/assets/mythic_age/textures/：
  fx/          贴地发光圈（128×128，圈在外缘，中心透明）与模型叠层流光（64×64，可平铺，黑底用于加色混合）；
  particle/    粒子帧（8×8 或 16×16 像素画）；
  mob_effect/  药水效果图标（18×18）。
同一份代码每次生成的结果逐像素相同（固定随机种子）。

用法：
  python3 tools/texgen/fxgen.py              生成全部
  python3 tools/texgen/fxgen.py --preview D  另外输出放大预览到目录 D
"""
from __future__ import annotations

import argparse
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "neoforge/src/main/resources/assets/mythic_age/textures"


def hexc(s: str, a: int = 255) -> tuple[int, int, int, int]:
    s = s.lstrip("#")
    return int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a


def smooth(edge0: float, edge1: float, x: float) -> float:
    t = max(0.0, min(1.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3 - 2 * t)


def band(r: float, center: float, half: float, soft: float) -> float:
    """r 处于 [center-half, center+half] 内为 1，外侧 soft 宽度内平滑衰减。"""
    d = abs(r - center)
    return 1.0 - smooth(half, half + soft, d)


def blend(dst: tuple, src: tuple) -> tuple:
    """src over dst（RGBA 0–255）。"""
    sa = src[3] / 255
    da = dst[3] / 255
    oa = sa + da * (1 - sa)
    if oa <= 0:
        return 0, 0, 0, 0
    c = [int((src[i] * sa + dst[i] * da * (1 - sa)) / oa) for i in range(3)]
    return c[0], c[1], c[2], int(oa * 255)


class Canvas:
    def __init__(self, size: int, bg=(0, 0, 0, 0)):
        self.size = size
        self.img = Image.new("RGBA", (size, size), bg)
        self.px = self.img.load()

    def put(self, x: int, y: int, c: tuple) -> None:
        if 0 <= x < self.size and 0 <= y < self.size and c[3] > 0:
            self.px[x, y] = blend(self.px[x, y], c)

    def polar(self, fn) -> None:
        """对每个像素按 (r, θ) 取色；r 以半边长为 1。"""
        s = self.size
        for y in range(s):
            for x in range(s):
                dx = (x + 0.5) / s * 2 - 1
                dy = (y + 0.5) / s * 2 - 1
                c = fn(math.hypot(dx, dy), math.atan2(dy, dx))
                if c is not None:
                    self.put(x, y, c)


def with_alpha(rgb: str, a: float) -> tuple:
    r, g, b, _ = hexc(rgb)
    return r, g, b, max(0, min(255, int(a * 255)))


# ───────────────────────────── 贴地圈 ─────────────────────────────

def bolt_ring() -> Image.Image:
    """雷击落点环：外圈亮环 + 内圈细环 + 12 道指向圆心的锯齿光芒（白色，渲染时调金白）。"""
    c = Canvas(128)

    def fn(r, th):
        a = 0.95 * band(r, 0.9, 0.04, 0.04) + 0.7 * band(r, 0.62, 0.012, 0.02)
        # 光芒：12 个三角，底在外圈内侧、尖向圆心
        k = (th / (2 * math.pi) * 12) % 1.0
        width = 0.5 * smooth(0.62, 0.86, r)
        if 0.62 < r < 0.86 and abs(k - 0.5) < width * 0.35:
            a = max(a, 0.75)
        # 外缘柔光
        a = max(a, 0.35 * band(r, 0.9, 0.06, 0.08))
        return None if a <= 0.01 else with_alpha("#ffffff", a)

    c.polar(fn)
    return c.img


def soft_ring(rgb: str, center: float, half: float, glow: float, fill: float) -> Image.Image:
    c = Canvas(128)

    def fn(r, th):
        a = 0.95 * band(r, center, half, 0.03) + glow * band(r, center, half, 0.18) + fill * (1 - smooth(0.0, center, r)) * 0.4
        return None if a <= 0.01 else with_alpha(rgb, min(1, a))

    c.polar(fn)
    return c.img


def olive_ring() -> Image.Image:
    """复原的叶环：细圈 + 沿圈交错排列的 28 片橄榄叶（叶尖顺时针）。"""
    c = Canvas(128)
    c.polar(lambda r, th: None if band(r, 0.92, 0.014, 0.018) < 0.02 else with_alpha("#e7f2c4", 0.95 * band(r, 0.92, 0.014, 0.018)))
    n = 28
    for i in range(n):
        th = i / n * 2 * math.pi
        rr = 0.84 if i % 2 == 0 else 0.97
        cx = 64 + math.cos(th) * rr * 60
        cy = 64 + math.sin(th) * rr * 60
        ang = th + math.pi / 2 + (0.35 if i % 2 == 0 else -0.35)
        dark = i % 2 == 0
        for y in range(128):
            for x in range(128):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                u = dx * math.cos(ang) + dy * math.sin(ang)
                v = -dx * math.sin(ang) + dy * math.cos(ang)
                # 叶形：长 6、宽 2.2 的尖椭圆
                if (u / 8.0) ** 2 + (v / (3.2 * (1 - abs(u) / 9.5))) ** 2 <= 1 if abs(u) < 9.5 else False:
                    col = "#6f9a32" if dark else "#a9cc5a"
                    if v < -0.9:
                        col = "#c8e08a"
                    c.put(x, y, with_alpha(col, 0.95))
    return c.img


def bronze_ring() -> Image.Image:
    """青铜圈：24 块青铜甲片围成的环 + 内侧细线（青铜色，自带颜色）。"""
    c = Canvas(128)

    def fn(r, th):
        k = (th / (2 * math.pi) * 24) % 1.0
        seg = 0.08 < k < 0.92
        a = 0.0
        col = "#d58a3e"
        if seg and 0.86 <= r <= 0.97:
            a = 0.95
            if r > 0.94:
                col = "#f2bd78"
            elif r < 0.885:
                col = "#8f5524"
        a = max(a, 0.8 * band(r, 0.8, 0.008, 0.012))
        if a <= 0.01:
            return None
        if 0.78 < r < 0.82:
            col = "#e9a95e"
        return with_alpha(col, a)

    c.polar(fn)
    return c.img


def bronze_swirl() -> Image.Image:
    """青铜流光：可平铺的斜向亮纹（灰白，黑底），渲染时以加色混合并调成青铜色。"""
    s = 64
    img = Image.new("RGBA", (s, s), (0, 0, 0, 255))
    px = img.load()
    rnd = random.Random(808)
    phases = [rnd.random() * 2 * math.pi for _ in range(3)]
    for y in range(s):
        for x in range(s):
            # 方向 (1, 2) 的条纹，周期整除 64 保证平铺
            t = (x + 2 * y) / s * 2 * math.pi
            v = 0.55 * (0.5 + 0.5 * math.sin(t * 2 + phases[0]))
            v += 0.3 * (0.5 + 0.5 * math.sin(t * 5 + phases[1] + math.sin(x / s * 2 * math.pi) * 1.5))
            v += 0.15 * (0.5 + 0.5 * math.sin(y / s * 2 * math.pi * 3 + phases[2]))
            v = v ** 2.2
            g = int(min(1, v) * 255)
            px[x, y] = (g, g, g, 255)
    return img


def bronze_plate() -> Image.Image:
    """青铜外壳：可平铺的锤打青铜面（不透明，渲染时按亮度给透明度），分片的亮边与暗缝。"""
    s = 64
    img = Image.new("RGBA", (s, s), (0, 0, 0, 255))
    px = img.load()
    rnd = random.Random(909)
    # 8×8 的甲片格，每片随机明暗，片缘一圈亮边、片缝一线暗
    tone = [[rnd.uniform(-0.12, 0.12) for _ in range(8)] for _ in range(8)]
    base = (0.80, 0.46, 0.18)
    for y in range(s):
        for x in range(s):
            cx, cy = x // 8, y // 8
            lx, ly = x % 8, y % 8
            v = 0.78 + tone[cy][cx] + rnd.uniform(-0.05, 0.05)
            if lx == 0 or ly == 0:
                v = 0.42
            elif lx == 1 or ly == 1:
                v = 1.18
            r, g, b = (min(1.0, c * v + (0.12 if v > 1.1 else 0)) for c in base)
            px[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    return img


# ───────────────────────────── 像素画 ─────────────────────────────

def pixels(rows: list[str], palette: dict[str, tuple]) -> Image.Image:
    size = len(rows)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        assert len(row) == size, (row, size)
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = palette[ch]
    return img


SPARK = {"w": hexc("#ffffff"), "y": hexc("#fff1a8"), "g": hexc("#ffd34d", 200), "o": hexc("#ffb43a", 120)}
SPARKS = [
    ["........", "...g....", "...y....", ".gywyg..", "...y....", "...g....", "........", "........"],
    ["........", ".o...o..", "..g.g...", "...w....", "..g.g...", ".o...o..", "........", "........"],
    ["........", "........", "...o....", "..oyo...", "...o....", "........", "........", "........"],
]


def cloud(seed: int) -> Image.Image:
    """雷云团（32×32）：5–6 团软边圆叠成的乌云，底部深蓝灰、顶部略亮，边缘柔和半透明。"""
    rnd = random.Random(seed)
    n = 32
    c = Canvas(n)
    blobs = [(16 + rnd.uniform(-7, 7), 16 + rnd.uniform(-3.5, 3.5), rnd.uniform(6.0, 9.5)) for _ in range(6)]
    top = (0x8a, 0x8f, 0xa0)
    bottom = (0x2c, 0x30, 0x3c)
    for y in range(n):
        for x in range(n):
            a = 0.0
            for bx, by, br in blobs:
                d = math.hypot(x + 0.5 - bx, y + 0.5 - by)
                a = max(a, 1 - smooth(br * 0.35, br, d))
            if a <= 0.02:
                continue
            t = smooth(0.2, 0.85, (y + 0.5) / n)
            col = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
            c.put(x, y, (col[0], col[1], col[2], int(min(1, a) * 0.95 * 255)))
    return c.img


def athena_sigil() -> Image.Image:
    """雅典娜纹章（128×128，自带颜色）：外双圈 + 回纹带 + 内圈 + 橄榄环 + 中心八芒星，淡金与橄榄绿。"""
    c = Canvas(128)
    gold = "#f3e3a2"
    green = "#b8d46a"
    # 回纹单元：沿角度 6 格、沿半径 5 格的钥匙纹
    key = ["######", ".....#", "####.#", "#....#", "#.####"]
    units = 36

    def fn(r, th):
        a = 0.0
        col = gold
        a = max(a, 0.95 * band(r, 0.965, 0.012, 0.012), 0.8 * band(r, 0.93, 0.008, 0.01))
        if 0.80 <= r <= 0.905:
            u = (th / (2 * math.pi) % 1.0) * units * 6
            v = (0.905 - r) / 0.105 * 5
            if key[min(4, int(v))][int(u) % 6] == "#":
                a = max(a, 0.9)
        a = max(a, 0.85 * band(r, 0.775, 0.008, 0.01))
        # 中心八芒星
        k = abs(math.cos(4 * th))
        if r < 0.16 + 0.12 * k ** 6:
            a = max(a, 0.9)
        a = max(a, 0.5 * band(r, 0.3, 0.006, 0.01))
        if a <= 0.01:
            return None
        return with_alpha(col, a)

    c.polar(fn)
    # 橄榄环：两段弧，叶片顺弧排列，底部留缺口打结
    for side in (-1, 1):
        for i in range(11):
            t = (i + 0.5) / 11
            th = math.pi / 2 + side * (0.25 + t * (math.pi - 0.45))
            for flip in (-1, 1):
                rr = 0.56 + flip * 0.06
                cx = 64 + math.cos(th) * rr * 60
                cy = 64 + math.sin(th) * rr * 60
                ang = th + math.pi / 2 * side + flip * 0.5 * side
                for y in range(int(cy) - 6, int(cy) + 7):
                    for x in range(int(cx) - 6, int(cx) + 7):
                        dx = x + 0.5 - cx
                        dy = y + 0.5 - cy
                        u = dx * math.cos(ang) + dy * math.sin(ang)
                        v = -dx * math.sin(ang) + dy * math.cos(ang)
                        if abs(u) < 5.5 and (u / 5.0) ** 2 + (v / (1.9 * (1 - abs(u) / 5.5))) ** 2 <= 1:
                            c.put(x, y, with_alpha(green if v > -0.5 else "#e2f0b4", 0.9))
    return c.img


MOTE = {"w": hexc("#f4ffe6"), "g": hexc("#b6ec8c", 200), "d": hexc("#7fcf5a", 110)}
MOTES = [
    ["........", "........", "...d....", "..dgd...", "..gwg...", "...g....", "........", "........"],
    ["........", "...d....", "...g....", ".dgwgd..", "...g....", "...d....", "........", "........"],
]
LEAF = {"l": hexc("#a9cc5a"), "d": hexc("#5f8a2d"), "h": hexc("#d4e89a"), "s": hexc("#6b4f2a")}
LEAVES = [
    ["........", "......l.", ".....lh.", "....ldl.", "...ldl..", "..ldl...", ".sd.....", "........"],
    ["........", "........", "..llll..", ".lhhhdl.", "slldddl.", "..llll..", "........", "........"],
    ["........", "...l....", "..lhl...", "..ldl...", "..ldl...", "...d....", "...s....", "........"],
]
SEA_MOTE = {"w": hexc("#eafcff"), "g": hexc("#7fe0e6", 200), "d": hexc("#3aa6b8", 110)}
RUNE_MOTE = {"w": hexc("#d6ffe6"), "g": hexc("#3fd08a", 210), "d": hexc("#1d7a48", 130)}
RUNE_MOTES = [
    ["........", "........", "...d....", "..dgd...", "...d....", "........", "........", "........"],
    ["........", "...d....", "...g....", ".dgwgd..", "...g....", "...d....", "........", "........"],
]
FLAKE = {"h": hexc("#ffd9a0"), "b": hexc("#e0913f"), "d": hexc("#9a5a22")}
FLAKES = [
    ["........", "........", "...hb...", "..hbbd..", "...bd...", "........", "........", "........"],
    ["........", "........", "....h...", "...bb...", "..bd....", "........", "........", "........"],
]
ICON = {
    "o": hexc("#2f4a1c"), "g": hexc("#8fd16a"), "G": hexc("#5aa83a"), "w": hexc("#ffffff"), "y": hexc("#effcc8"),
    "k": hexc("#2b2b30"), "m": hexc("#6d7078"), "M": hexc("#9aa0aa"), "h": hexc("#d6dbe2"),
}
CLEANSE = [
    "................",
    ".....oooooo.....",
    "...ooggggggoo...",
    "..oggGGGGGGggo..",
    "..ogGG....GGgo..",
    ".ogGG...w..GGgo.",
    ".ogG....w...Ggo.",
    ".ogG..wwywww.Go.",
    ".ogG....w...Ggo.",
    ".ogGG...w..GGgo.",
    "..ogGG....GGgo..",
    "..oggGGGGGGggo..",
    "...ooggggggoo...",
    ".....oooooo.....",
    "................",
    "................",
]
REPAIR = [
    "................",
    "................",
    "................",
    "..kkkkkkkkkkk...",
    ".khhhhhhhhhMMk..",
    "kkMMMMMMMMMMMk..",
    ".kkkkMMMMMMkk...",
    "....kMMMMMk.....",
    ".....kMMMk......",
    ".....kmMmk......",
    "....kmmmmmk.....",
    "...kkkkkkkkk....",
    "...kmmmmmmmk....",
    "...kkkkkkkkk....",
    "................",
    "................",
]
EFFECT = {"b": hexc("#4a2a12"), "h": hexc("#f6c27a"), "H": hexc("#d08a3c"), "m": hexc("#9a5d26"), "w": hexc("#ffe2a8"),
          "B": hexc("#ffefc8")}
BRONZE_ICON = [
    "..................",
    "..................",
    "......bbbbbb......",
    "....bbhhhHHHbb....",
    "...bhhhHHHHHmmb...",
    "...bhhHHHHHHmmb...",
    "..bhhHHHwwHHHmmb..",
    "..bhHHHwBBwHHmmb..",
    "..bhHHwBBBBwHmmb..",
    "..bHHHwBBBBwHmmb..",
    "..bHHHHwBBwHHmmb..",
    "..bHHHHHwwHHmmmb..",
    "...bHHHHHHHmmmb...",
    "...bmmmmmmmmmmb...",
    "....bbmmmmmmbb....",
    "......bbbbbb......",
    "..................",
    "..................",
]


def plague_seal() -> Image.Image:
    """瘟疫病印（128）：暗红的斑驳外环 + 一圈向内的尖刺纹 + 内圈细线，带病黄色斑点（自带颜色）。"""
    rnd = random.Random(606)
    c = Canvas(128)
    blots = [(rnd.uniform(0, 2 * math.pi), rnd.uniform(0.86, 0.94), rnd.uniform(0.012, 0.022)) for _ in range(30)]

    def fn(r, th):
        a = 0.0
        col = "#7a1a16"
        a = max(a, 0.9 * band(r, 0.92, 0.018, 0.02))
        for bt, br, bs in blots:
            d = math.hypot(r * math.cos(th) - br * math.cos(bt), r * math.sin(th) - br * math.sin(bt))
            if d < bs:
                a = max(a, 0.95)
                col = "#c9b44a" if bs > 0.019 else "#5a1210"
        k = (th / (2 * math.pi) * 18) % 1.0
        if 0.6 < r < 0.86 and abs(k - 0.5) < 0.18 * smooth(0.6, 0.86, r):
            a = max(a, 0.7)
        a = max(a, 0.6 * band(r, 0.58, 0.008, 0.012))
        a = max(a, 0.18 * (1 - smooth(0.0, 0.9, r)))
        return None if a <= 0.01 else with_alpha(col, a)

    c.polar(fn)
    return c.img


def plague_band() -> Image.Image:
    """瘟疫边界带（64×16，沿圆周平铺）：暗红主带 + 指向圈内的尖刺 + 病黄细线，上下边缘柔和。"""
    w, h = 64, 16
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            v = (y + 0.5) / h
            a = band(v, 0.62, 0.12, 0.08) * 0.95
            # 尖刺：每 16 像素一个三角，从主带指向内侧（v 小的一侧）
            k = (x % 16 + 0.5) / 16
            spike = 0.5 - abs(k - 0.5)
            if 0.2 < v < 0.55 and (0.55 - v) < spike * 0.7:
                a = max(a, 0.85)
            col = "#7a1a16"
            if abs(v - 0.62) < 0.03:
                col = "#c9b44a"
            if a > 0.01:
                r, g, b, _ = hexc(col)
                px[x, y] = (r, g, b, int(a * 255))
    return img


def curse_ring() -> Image.Image:
    """诅咒符圈（128）：玫瑰粉双圈 + 12 朵五瓣小花 + 内圈心形连环（自带颜色）。"""
    c = Canvas(128)

    def fn(r, th):
        a = max(0.95 * band(r, 0.95, 0.012, 0.014), 0.8 * band(r, 0.78, 0.008, 0.012))
        # 心形连环：沿 r≈0.66 的波浪线
        wave = 0.66 + 0.035 * math.sin(th * 24)
        a = max(a, 0.75 * band(r, wave, 0.008, 0.01))
        a = max(a, 0.15 * (1 - smooth(0.0, 0.8, r)))
        return None if a <= 0.01 else with_alpha("#f39bc0", a)

    c.polar(fn)
    for i in range(12):
        th = i / 12 * 2 * math.pi
        cx = 64 + math.cos(th) * 0.865 * 60
        cy = 64 + math.sin(th) * 0.865 * 60
        for y in range(int(cy) - 6, int(cy) + 7):
            for x in range(int(cx) - 6, int(cx) + 7):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                rr = math.hypot(dx, dy)
                ang = math.atan2(dy, dx)
                petal = 3.2 + 1.8 * abs(math.cos(2.5 * ang))
                if rr < 1.3:
                    c.put(x, y, with_alpha("#ffe08a", 1))
                elif rr < petal:
                    c.put(x, y, with_alpha("#ff7fb0" if rr > petal - 1.2 else "#ffc2da", 0.95))
    return c.img


def lure_band() -> Image.Image:
    """诱兽石的贝纹海浪带（64×16，沿圆周平铺）：一道卷浪线（浪峰朝外）+ 浪峰下的白沫点 + 内侧淡海蓝的余波，
    上下边缘柔和（自带颜色）。v 小的一侧在圈内。"""
    w, h = 64, 16
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            v = (y + 0.5) / h
            k = (x + 0.5) / 16 * 2 * math.pi
            crest = 0.62 + 0.13 * math.sin(k)  # 浪线
            a = 0.95 * band(v, crest, 0.045, 0.06)
            col = "#58b9c2"
            if band(v, crest + 0.05, 0.02, 0.02) > 0.5 and math.sin(k) > 0.55:
                col = "#e8fbff"  # 浪峰白沫
                a = max(a, 0.9)
            a = max(a, 0.35 * band(v, 0.35, 0.12, 0.14))  # 内侧余波
            if a > 0.01:
                r, g, b, _ = hexc(col)
                px[x, y] = (r, g, b, int(a * 255))
    return img


def sentinel_rune() -> Image.Image:
    """哨兵站位符（64）：方形双框 + 框间回纹 + 中央冥府双叉戟，暗绿发光（自带颜色）。"""
    n = 64
    c = Canvas(n)
    core = with_alpha("#9dffc8", 1)
    line = with_alpha("#3fd08a", 0.95)
    glow = with_alpha("#2e8a58", 0.45)

    def rect(x0, y0, x1, y1, col):
        for y in range(y0, y1):
            for x in range(x0, x1):
                c.put(x, y, col)

    # 外框、内框（各 2 像素）与一圈柔光
    for inset, col in ((3, glow), (4, line), (13, line)):
        t = 2 if col is line else 4
        rect(inset, inset, n - inset, inset + t, col)
        rect(inset, n - inset - t, n - inset, n - inset, col)
        rect(inset, inset, inset + t, n - inset, col)
        rect(n - inset - t, inset, n - inset, n - inset, col)
    # 框间回纹：每 8 像素一个「卍」形小钩，沿四边排布
    for i in range(8, n - 8, 8):
        for (x, y) in ((i, 8), (i, n - 10), (8, i), (n - 10, i)):
            rect(x, y, x + 3, y + 1, line)
            rect(x + 2, y, x + 3, y + 3, line)
    # 中央双叉戟：杆、横档、两齿
    rect(31, 26, 33, 46, core)
    rect(25, 26, 39, 28, core)
    rect(25, 18, 27, 28, core)
    rect(37, 18, 39, 28, core)
    for (x, y) in ((24, 20), (39, 20)):
        rect(x, y, x + 1, y + 2, line)
    return c.img


def soft_puff(seed: int, top: str, bottom: str, n: int = 16) -> Image.Image:
    """软边烟团（疫雾、玫瑰烟）。"""
    rnd = random.Random(seed)
    c = Canvas(n)
    blobs = [(n / 2 + rnd.uniform(-n * 0.18, n * 0.18), n / 2 + rnd.uniform(-n * 0.12, n * 0.12), rnd.uniform(n * 0.25, n * 0.38))
             for _ in range(4)]
    t0 = hexc(top)
    t1 = hexc(bottom)
    for y in range(n):
        for x in range(n):
            a = 0.0
            for bx, by, br in blobs:
                a = max(a, 1 - smooth(br * 0.3, br, math.hypot(x + 0.5 - bx, y + 0.5 - by)))
            if a <= 0.02:
                continue
            t = (y + 0.5) / n
            col = tuple(int(t0[i] + (t1[i] - t0[i]) * t) for i in range(3))
            c.put(x, y, (col[0], col[1], col[2], int(a * 230)))
    return c.img


PMARK = {"o": hexc("#2a0a08"), "r": hexc("#a3241c"), "R": hexc("#d6463a"), "y": hexc("#d8cf5a")}
PLAGUE_MARK = [
    "........",
    "...o....",
    "..oRo...",
    "..oro...",
    ".orrro..",
    ".oryro..",
    "..ooo...",
    "........",
]
SEAL_ICON = {"o": hexc("#20140f"), "g": hexc("#5c5f66"), "G": hexc("#8a8e96"), "r": hexc("#d23a2e"), "R": hexc("#ff6a55")}
SEAL = [
    "................",
    "..oooooooooooo..",
    "..oGgGgGgGgGgo..",
    ".Rr..........rR.",
    "..rR..g..g..Rr..",
    "..oGrRg..gRrGo..",
    "..og.rRggRr.go..",
    "..oG..rRRr..Go..",
    "..og..rRRr..go..",
    "..oG.rRggRr.Go..",
    "..ogrRg..gRrgo..",
    "..rR..g..g..Rr..",
    ".Rr..........rR.",
    "..oGgGgGgGgGgo..",
    "..oooooooooooo..",
    "................",
]
IMMUNE_ICON = {"o": hexc("#2b2f3a"), "s": hexc("#c9d2e0"), "S": hexc("#ffffff"), "b": hexc("#6f8fbf"), "y": hexc("#f2d27a")}
IMMUNE = [
    "................",
    "....oooooooo....",
    "...osSSSSSSso...",
    "...osSbbbbSso...",
    "...osbbyybbso...",
    "...osbyyyybso...",
    "...osbbyybbso...",
    "...osbbbbbbso...",
    "....osbbbbso....",
    "....osbbbbso....",
    ".....osbbso.....",
    "......osso......",
    ".......oo.......",
    "................",
    "................",
    "................",
]


def cursed_pig() -> Image.Image:
    """被诅咒的猪（64×32，原版猪模型的贴图尺寸与方块 UV 排布）：玫瑰粉皮、稀疏金斑、深粉鼻、金色眼眸。"""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(707)
    skin = [hexc("#e8899f"), hexc("#e8899f"), hexc("#e8899f"), hexc("#df7f96"), hexc("#ef94a9")]
    spot = hexc("#e9c25c")

    def box(u, v, w, h, d):
        # 顶、底、四个侧面
        for (x0, y0, ww, hh) in ((u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h),
                                 (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h)):
            for y in range(y0, y0 + hh):
                for x in range(x0, x0 + ww):
                    px[x, y] = spot if rnd.random() < 0.035 else skin[rnd.randrange(len(skin))]

    box(0, 0, 8, 8, 8)      # 头
    box(16, 16, 4, 3, 1)    # 鼻
    box(28, 8, 10, 16, 8)   # 身
    box(0, 16, 4, 6, 4)     # 腿
    # 脸（头的正面 8..15 × 8..15）：金色眼眸
    for (x, y, c) in ((9, 11, "#ffffff"), (10, 11, "#c48a1a"), (13, 11, "#c48a1a"), (14, 11, "#ffffff")):
        px[x, y] = hexc(c)
    # 鼻子正面（17..20 × 17..19）：深粉 + 鼻孔
    for y in range(17, 20):
        for x in range(17, 21):
            px[x, y] = hexc("#c45a78")
    px[17, 18] = hexc("#6a2236")
    px[20, 18] = hexc("#6a2236")
    return img


def all_textures() -> dict[str, Image.Image]:
    out: dict[str, Image.Image] = {
        "fx/bolt_ring": bolt_ring(),
        "fx/olive_ring": olive_ring(),
        "fx/heal_ring": soft_ring("#e8ffd0", 0.82, 0.02, 0.45, 0.25),
        "fx/bronze_ring": bronze_ring(),
        "fx/bronze_swirl": bronze_swirl(),
        "fx/bronze_plate": bronze_plate(),
        "fx/athena_sigil": athena_sigil(),
        "fx/plague_seal": plague_seal(),
        "fx/plague_band": plague_band(),
        "fx/curse_ring": curse_ring(),
        "fx/lure_band": lure_band(),
        "fx/sentinel_rune": sentinel_rune(),
        "particle/plague_mark": pixels(PLAGUE_MARK, PMARK),
        "particle/seal": pixels(SEAL, SEAL_ICON),
        "particle/immune": pixels(IMMUNE, IMMUNE_ICON),
        "entity/cursed_pig": cursed_pig(),
        "mob_effect/bronze_body": pixels(BRONZE_ICON, EFFECT),
        "particle/cleanse": pixels(CLEANSE, ICON),
        "particle/repair": pixels(REPAIR, ICON),
    }
    for i, rows in enumerate(SPARKS):
        out[f"particle/spark_{i}"] = pixels(rows, SPARK)
    for i in range(4):
        out[f"particle/storm_cloud_{i}"] = cloud(100 + i)
    for i, rows in enumerate(MOTES):
        out[f"particle/heal_mote_{i}"] = pixels(rows, MOTE)
    for i, rows in enumerate(LEAVES):
        out[f"particle/olive_leaf_{i}"] = pixels(rows, LEAF)
    for i in range(3):
        out[f"particle/plague_mist_{i}"] = soft_puff(300 + i, "#7d6a3a", "#4a1c18")
        out[f"particle/rose_puff_{i}"] = soft_puff(400 + i, "#ffd3e4", "#e97aa6")
    for i, rows in enumerate(FLAKES):
        out[f"particle/bronze_flake_{i}"] = pixels(rows, FLAKE)
    for i, rows in enumerate(MOTES):
        out[f"particle/sea_mote_{i}"] = pixels(rows, SEA_MOTE)
    for i, rows in enumerate(RUNE_MOTES):
        out[f"particle/rune_mote_{i}"] = pixels(rows, RUNE_MOTE)
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    args = ap.parse_args()
    textures = all_textures()
    for name, img in textures.items():
        path = TEX / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
        print(f"wrote {path.relative_to(ROOT)}  {img.width}×{img.height}")
    if args.preview:
        d = Path(args.preview)
        d.mkdir(parents=True, exist_ok=True)
        for name, img in textures.items():
            scale = max(1, 256 // img.width)
            bg = Image.new("RGBA", img.size, (40, 44, 52, 255))
            bg.alpha_composite(img)
            bg.resize((img.width * scale, img.height * scale), Image.NEAREST).save(d / (name.replace("/", "__") + ".png"))


if __name__ == "__main__":
    main()
