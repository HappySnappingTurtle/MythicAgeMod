#!/usr/bin/env python3
"""T17 阶段 ① 希腊青铜装备的临时贴图（阶段 ② 换成正式图标、手持 3D 模型与 3D 护甲）。

- 护甲件图标、短剑、护甲两层贴图：把原版铁制贴图按亮度映射到青铜色阶（低饱和暖铜）。
- 复合弓：原版弓的四张贴图，弦（浅灰）改成半人马鬃的浅褐，木色压暗。
- 长矛、圆盾、大棒：16 × 16 像素图。
原版贴图从 NeoForge 的 client-extra 资源包里读：python3 tools/models/greek_equipment.py <解压后的 minecraft 资源目录>
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEX = ROOT / "neoforge/src/main/resources/assets/mythic_age/textures"
BRONZE = [(0.0, (38, 22, 10)), (0.35, (110, 66, 26)), (0.6, (168, 116, 46)), (0.82, (217, 162, 74)), (1.0, (246, 214, 138))]


def ramp(t: float, stops) -> tuple[int, int, int]:
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if t <= b:
            k = 0 if b == a else (t - a) / (b - a)
            return tuple(round(ca[i] + (cb[i] - ca[i]) * k) for i in range(3))
    return stops[-1][1]


def bronze(src: Path, dst: Path, keep=lambda rgb: False) -> None:
    im = Image.open(src).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0 or keep((r, g, b)):
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            px[x, y] = (*ramp(lum, BRONZE), a)
    dst.parent.mkdir(parents=True, exist_ok=True)
    im.save(dst)


def is_wood(rgb) -> bool:
    r, g, b = rgb
    return r > g + 12 and g > b  # 原版剑柄的棕色木头不改


def bow(src: Path, dst: Path) -> None:
    im = Image.open(src).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if abs(r - g) < 12 and abs(g - b) < 12 and r > 120:  # 弦：浅灰 → 鬃弦浅褐
                px[x, y] = (196, 160, 112, a)
            else:  # 木：压暗，偏角黄
                px[x, y] = (int(r * 0.75 + 20), int(g * 0.7 + 10), int(b * 0.55), a)
    dst.parent.mkdir(parents=True, exist_ok=True)
    im.save(dst)


def pixels(name: str, rows: list[str], pal: dict[str, str]) -> None:
    im = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                h = pal[c].lstrip("#")
                im.putpixel((x, y), (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255))
    im.save(TEX / "item" / f"{name}.png")


DORY = ["..............Bh",
        ".............BkB",
        "............BkB.",
        "...........BBB..",
        "..........wW....",
        ".........wW.....",
        "........wW......",
        ".......wW.......",
        "......wW........",
        ".....wW.........",
        "....wW..........",
        "...wW...........",
        "..wW............",
        ".BB.............",
        "Bb..............",
        "................"]
DORY_PAL = {"B": "#8a5a24", "b": "#5a3a14", "h": "#f6d68a", "k": "#d9a24a", "w": "#a68052", "W": "#cfae7e"}

ASPIS = ["....oooooooo....",
         "..ooBBBBBBBBoo..",
         ".oBhhhBBBBBBBBo.",
         ".oBhBBBBBBBBBBo.",
         "oBhBBBkkkkBBBBBo",
         "oBhBBkBBBBkBBBBo",
         "oBBBkBBddBBkBBBo",
         "oBBBkBdddBBkBBBo",
         "oBBBkBBddBBkBBdo",
         "oBBBBkBBBBkBBBdo",
         "oBBBBBkkkkBBBBdo",
         ".oBBBBBBBBBBBdo.",
         ".oBBBBBBBBBBddo.",
         "..ooBBBBBBddoo..",
         "....oooooooo....",
         "................"]
ASPIS_PAL = {"o": "#3a2410", "B": "#b07a36", "h": "#e8c27a", "k": "#2a1a10", "d": "#7a4e20"}

CLUB = ["...........oo...",
        "..........oWWo..",
        ".........oWwWko.",
        "........oWwkWWo.",
        ".......oWwWWwo..",
        "......oWwWkwo...",
        ".....oWwWwo.....",
        "....owWwo.......",
        "...owwo.........",
        "..owwo..........",
        ".owwo...........",
        "owwo............",
        "ooo.............",
        "................",
        "................",
        "................"]
CLUB_PAL = {"o": "#3b2a1a", "W": "#bfa470", "w": "#8f7448", "k": "#5a4428"}


def main(mc: Path) -> None:
    item = mc / "assets/minecraft/textures/item"
    armor = mc / "assets/minecraft/textures/models/armor"
    for part in ("helmet", "chestplate", "leggings", "boots"):
        bronze(item / f"iron_{part}.png", TEX / "item" / f"bronze_{part}.png")
    bronze(item / "iron_sword.png", TEX / "item" / "xiphos.png", keep=is_wood)
    for layer in (1, 2):
        bronze(armor / f"iron_layer_{layer}.png", TEX / "models/armor" / f"bronze_layer_{layer}.png")
    bow(item / "bow.png", TEX / "item" / "composite_bow.png")
    for i in range(3):
        bow(item / f"bow_pulling_{i}.png", TEX / "item" / f"composite_bow_pulling_{i}.png")
    pixels("dory", DORY, DORY_PAL)
    pixels("aspis", ASPIS, ASPIS_PAL)
    pixels("olive_club", CLUB, CLUB_PAL)
    print("ok")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
