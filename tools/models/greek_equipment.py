#!/usr/bin/env python3
"""T17 阶段 ① 希腊青铜装备的临时贴图（阶段 ② 换成正式图标、手持 3D 模型与 3D 护甲）。

- 护腿、靴的图标：把原版铁制贴图按亮度映射到青铜色阶（低饱和暖铜）。
- 复合弓：原版弓的四张贴图，弦改成半人马鬃的浅褐，弓臂分角片黄与深木两层。
- 长矛、圆盾、大棒：16 × 16 像素图（短剑图标在 greek_weapons.py）。
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


HORN = (216, 180, 106)
HORN_D = (176, 138, 72)
WOOD = (90, 58, 32)
WOOD_D = (58, 36, 20)
SINEW = (196, 160, 112)


def bow(src: Path, dst: Path) -> None:
    """复合弓（10 号合同：角黄 / 深木 / 浅褐鬃弦）：原版弓的轮廓，弦（浅灰）改成鬃弦浅褐，
    弓臂按亮度分两层：亮的是内侧角片（角黄），暗的是外侧筋木（深木）；箭（拉弓时）照原版不动。"""
    im = Image.open(src).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            sat = max(r, g, b) - min(r, g, b)
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if sat < 18 and lum > 110:  # 弦
                px[x, y] = (*SINEW, a)
            elif sat < 18:  # 箭杆、箭头的灰（拉弓贴图里），保持
                continue
            elif lum > 105:
                px[x, y] = (*HORN, a)
            elif lum > 80:
                px[x, y] = (*HORN_D, a)
            elif lum > 55:
                px[x, y] = (*WOOD, a)
            else:
                px[x, y] = (*WOOD_D, a)
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


HELMET = ["................",
          "..rRrRrRrRrRr...",
          "..rrrrrrrrrrr...",
          "......oo........",
          "...ooBBBBoo.....",
          "..oBhBBBBBBo....",
          "..oBhBBBBBBBo...",
          "..oBBBBBBBBBo...",
          "..oB..BB..BBo...",
          "..oBB.BB.BBBo...",
          "..oBBB..BBBdo...",
          "..oBBB..BBdo....",
          "...oBB..Bdo.....",
          "....oo..oo......",
          "................",
          "................"]
HELMET_PAL = {"o": "#3a2410", "B": "#b07a36", "h": "#e8c27a", "d": "#7a4e20", "r": "#8e3a24", "R": "#b0583a"}

CHEST = ["................",
         "..ll........ll..",
         ".olllo....ollllo",
         ".oBBBBooooBBBBo.",
         ".oBhhhBBBBhhhBo.",
         "..oBBdBBBBdBBo..",
         "..oBBBdBBdBBBo..",
         "..oBBBBdBBBBBo..",
         "..oBBhBdBhBBBo..",
         "..oBBBBdBBBBdo..",
         "...oBBBdBBBdo...",
         "...ollllllllo...",
         "...wlwlwlwlwl...",
         "...wlwlwlwlwl...",
         "...wlwlwlwlwl...",
         "................"]
CHEST_PAL = {"o": "#3a2410", "B": "#b07a36", "h": "#e8c27a", "d": "#7a4e20", "l": "#5a3a24", "w": "#d8cfb4"}


def main(mc: Path) -> None:
    item = mc / "assets/minecraft/textures/item"
    for part in ("leggings", "boots"):
        bronze(item / f"iron_{part}.png", TEX / "item" / f"bronze_{part}.png")
    pixels("bronze_helmet", HELMET, HELMET_PAL)  # 科林斯盔：横冠、T 形开口
    pixels("bronze_chestplate", CHEST, CHEST_PAL)  # 肌肉胸甲 + 垂条
    # 护甲两层贴图改由 greek_armor.py 生成（阶段 ②，64 × 64，带 3D 部件）
    bow(item / "bow.png", TEX / "item" / "composite_bow.png")
    for i in range(3):
        bow(item / f"bow_pulling_{i}.png", TEX / "item" / f"composite_bow_pulling_{i}.png")
    pixels("dory", DORY, DORY_PAL)
    pixels("aspis", ASPIS, ASPIS_PAL)
    pixels("olive_club", CLUB, CLUB_PAL)
    print("ok")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
