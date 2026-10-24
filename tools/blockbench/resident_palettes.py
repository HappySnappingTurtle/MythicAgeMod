#!/usr/bin/env python3
"""居民贴图的男女 × 服色变体（T19 阶段 ①，V3-希腊-居民 §8：服色从 4–6 种土色里随机）。

输入：build_resident.js 导出的 resident.png 与材质遮罩 art/blockbench/greek/resident/resident_mask.png
     （红 = 皮肤，绿 = 男装希顿，蓝 = 女装长袍，黄 = 披巾）。
输出：textures/entity/resident/resident_m<0-5>.png、resident_f<0-5>.png。
换色按「目标色 / 原底色」逐通道缩放，保留贴图上的明暗、布纹与袍边。
颜色取古希腊常见的染料：本色羊毛、漂白、茜草红、菘蓝、番红花黄、黄褐与橄榄色（出处见居民白模卡）。
运行：python3 tools/blockbench/resident_palettes.py
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "neoforge/src/main/resources/assets/mythic_age/textures/entity/resident"
MASK = ROOT / "art/blockbench/greek/resident/resident_mask.png"

BASE = {"skin": "#AE7656", "tunic": "#D6C7A6", "dress": "#D2AC58", "shawl": "#7C3A2F"}
KEYS = {(255, 0, 0): "skin", (0, 255, 0): "tunic", (0, 0, 255): "dress", (255, 255, 0): "shawl"}
SKIN_F = "#C79270"
TUNICS = ["#D6C7A6", "#E6DFCC", "#8C3B2B", "#7C8C9E", "#A39A6C", "#C49A5A"]
DRESSES = ["#D2AC58", "#E6DFCC", "#A84A3A", "#6F86A6", "#9FAA7C", "#C4B08C"]
SHAWLS = ["#7C3A2F", "#3F5A7A", "#8A6A2E", "#7C3A2F", "#5E4632", "#4E5E3A"]


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def recolor(src, mask, targets):
    out = src.copy()
    sp, mp, op = src.load(), mask.load(), out.load()
    ratio = {}
    for fam, tgt in targets.items():
        b = rgb(BASE[fam])
        t = rgb(tgt)
        ratio[fam] = tuple(t[i] / b[i] for i in range(3))
    for y in range(src.height):
        for x in range(src.width):
            m = mp[x, y]
            fam = KEYS.get(m[:3]) if m[3] > 0 else None
            if fam is None or fam not in ratio:
                continue
            r, g, b, a = sp[x, y]
            k = ratio[fam]
            op[x, y] = (min(255, round(r * k[0])), min(255, round(g * k[1])), min(255, round(b * k[2])), a)
    return out


def main():
    src = Image.open(TEX / "resident.png").convert("RGBA")
    mask = Image.open(MASK).convert("RGBA")
    for i in range(6):
        recolor(src, mask, {"tunic": TUNICS[i]}).save(TEX / f"resident_m{i}.png")
        recolor(src, mask, {"skin": SKIN_F, "dress": DRESSES[i], "shawl": SHAWLS[i]}).save(TEX / f"resident_f{i}.png")
    print("变体 12 张 →", TEX)


if __name__ == "__main__":
    main()
