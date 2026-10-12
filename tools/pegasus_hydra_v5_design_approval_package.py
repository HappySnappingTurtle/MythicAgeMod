#!/usr/bin/env python3
"""Build V5 approval-ready candidate design sheets for Pegasus and Hydra.

V4 made the constraints stricter, but it was still mostly geometric. V5 keeps
modeling locked and creates a clearer pre-modeling design contract: the sheets
must be reviewed as creature design drawings before any Blockbench work resumes.
"""

from __future__ import annotations

import argparse
import json
import textwrap
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/greek_creatures/design_approval/v5"
REPORT_PATH = OUT_DIR / "pegasus_hydra_v5_design_approval_package.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v5_design_approval_package_2026-06-15.md"

CONTRACTS = {
    "pegasus": ROOT / "docs/美术资源/greek_creatures/design_blueprints/pegasus_v5_approval_candidate_contract.md",
    "hydra": ROOT / "docs/美术资源/greek_creatures/design_blueprints/hydra_v5_approval_candidate_contract.md",
}
SHEETS = {
    "pegasus": OUT_DIR / "pegasus_v5_approval_candidate_sheet.png",
    "hydra": OUT_DIR / "hydra_v5_approval_candidate_sheet.png",
}

BG = (241, 238, 230, 255)
PANEL = (255, 253, 247, 255)
INK = (33, 38, 45, 255)
MUTED = (91, 99, 110, 255)
WARN = (154, 70, 48, 255)
GREEN = (38, 82, 48, 255)
DARK_GREEN = (14, 34, 20, 255)
POISON = (151, 198, 83, 255)
WHITE = (249, 249, 244, 255)
WARM_WHITE = (238, 240, 235, 255)
GREY = (185, 194, 198, 255)
DARK = (31, 35, 40, 255)
MOUTH = (61, 30, 38, 255)
FANG = (244, 238, 219, 255)
GRID = (207, 201, 190, 255)

FONT_PATHS = [
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/STHeiti Light.ttc",
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def font(size: int) -> ImageFont.ImageFont:
    for path in FONT_PATHS:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size=size)
            except OSError:
                pass
    return ImageFont.load_default()


def wrapped(draw: ImageDraw.ImageDraw, text: str, xy: tuple[int, int], width: int, *, size: int = 24, fill=INK) -> int:
    x, y = xy
    fnt = font(size)
    chars = max(12, width // max(8, int(size * 0.52)))
    for line in textwrap.wrap(text, width=chars):
        draw.text((x, y), line, fill=fill, font=fnt)
        y += size + 8
    return y


def panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str) -> None:
    draw.rounded_rectangle(box, radius=10, fill=PANEL, outline=(126, 136, 145, 255), width=3)
    draw.text((box[0] + 24, box[1] + 18), title, fill=INK, font=font(31))


def grid(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], step: int = 50) -> None:
    x1, y1, x2, y2 = box
    for x in range(x1, x2 + 1, step):
        draw.line((x, y1, x, y2), fill=GRID, width=1)
    for y in range(y1, y2 + 1, step):
        draw.line((x1, y, x2, y), fill=GRID, width=1)


def dim(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], text: str) -> None:
    draw.line((*start, *end), fill=WARN, width=3)
    for x, y in [start, end]:
        draw.ellipse((x - 5, y - 5, x + 5, y + 5), fill=WARN)
    mx, my = (start[0] + end[0]) // 2, (start[1] + end[1]) // 2
    draw.rounded_rectangle((mx + 8, my - 20, mx + 8 + len(text) * 12, my + 14), radius=5, fill=(255, 245, 236, 255), outline=WARN)
    draw.text((mx + 14, my - 18), text, fill=WARN, font=font(20))


def draw_horse_side(draw: ImageDraw.ImageDraw, ox: int, oy: int, scale: float = 1.0) -> None:
    s = scale
    def p(x: float, y: float) -> tuple[int, int]:
        return (ox + int(x * s), oy + int(y * s))

    # Body built around adult horse proportions: long barrel, separate chest/rump.
    draw.polygon([p(160, 250), p(285, 210), p(540, 220), p(665, 265), p(620, 370), p(230, 370)], fill=WHITE, outline=INK)
    draw.polygon([p(185, 275), p(305, 245), p(300, 370), p(180, 370)], fill=WARM_WHITE, outline=GREY)
    draw.polygon([p(545, 230), p(665, 265), p(620, 370), p(535, 355)], fill=WARM_WHITE, outline=GREY)
    draw.line((p(265, 306), p(590, 315)), fill=GREY, width=max(2, int(4 * s)))

    # Sloped neck and small elongated horse head.
    draw.polygon([p(210, 245), p(105, 135), p(145, 105), p(270, 225)], fill=WHITE, outline=INK)
    draw.polygon([p(110, 120), p(15, 115), p(-50, 148), p(20, 178), p(118, 170)], fill=WHITE, outline=INK)
    draw.polygon([p(-50, 148), p(-118, 157), p(-118, 188), p(20, 178)], fill=WHITE, outline=INK)
    draw.polygon([p(35, 110), p(50, 55), p(69, 114)], fill=WHITE, outline=INK)
    draw.polygon([p(88, 110), p(112, 64), p(120, 125)], fill=WHITE, outline=INK)
    draw.ellipse((*p(38, 140), *p(53, 155)), fill=DARK)
    draw.rectangle((*p(-98, 168), *p(-82, 176)), fill=DARK)
    draw.line((*p(-35, 188), *p(30, 190)), fill=(80, 88, 95, 255), width=max(2, int(4 * s)))
    for i in range(5):
        draw.polygon([p(135 + i * 18, 104 + i * 21), p(158 + i * 19, 124 + i * 20), p(142 + i * 17, 138 + i * 22)], fill=GREY, outline=(120, 130, 138, 255))

    # Wings sit behind shoulder and expose rider slot.
    draw.ellipse((*p(280, 224), *p(350, 295)), fill=GREY, outline=INK, width=3)
    draw.polygon([p(335, 225), p(540, 70), p(780, 140), p(590, 262)], fill=WHITE, outline=INK)
    for i in range(7):
        draw.polygon(
            [p(520 - i * 26, 250 + i * 14), p(775 - i * 25, 145 + i * 14), p(825 - i * 23, 177 + i * 14), p(560 - i * 25, 292 + i * 14)],
            fill=(231, 236, 236, 255),
            outline=(102, 113, 120, 255),
        )
    draw.rounded_rectangle((*p(325, 217), *p(490, 252)), radius=6, fill=(255, 241, 238, 255), outline=WARN, width=2)
    draw.text(p(342, 224), "rider slot", fill=WARN, font=font(max(14, int(20 * s))))

    # Legs: four slimmer horse legs, not pillars.
    for x, knee in [(205, 455), (305, 440), (520, 442), (610, 458)]:
        draw.polygon([p(x, 360), p(x + 48, 360), p(x + 40, knee), p(x + 15, knee)], fill=WHITE, outline=INK)
        draw.polygon([p(x + 14, knee), p(x + 42, knee), p(x + 38, 555), p(x + 5, 555)], fill=WHITE, outline=INK)
        draw.polygon([p(x - 5, 555), p(x + 48, 555), p(x + 65, 585), p(x - 18, 585)], fill=GREY, outline=(105, 115, 122, 255))

    draw.polygon([p(650, 300), p(800, 335), p(760, 445), p(630, 365)], fill=GREY, outline=(105, 115, 122, 255))


def draw_pegasus_sheet(path: Path) -> None:
    image = Image.new("RGBA", (2400, 1600), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), "Pegasus V5 approval candidate: adult white horse first", fill=INK, font=font(42))
    draw.text((54, 88), "Candidate only. Not approved for modeling until human review records approved_for_modeling=true.", fill=MUTED, font=font(24))

    panel(draw, (48, 140, 1500, 840), "Side design: normal horse proportion, small readable head")
    grid(draw, (90, 210, 1455, 790), 50)
    draw.line((95, 765, 1455, 765), fill=(145, 137, 124, 255), width=3)
    draw_horse_side(draw, 270, 180, 1.0)
    dim(draw, (430, 615), (935, 615), "long body")
    dim(draw, (160, 255), (390, 255), "small elongated head")
    dim(draw, (448, 382), (385, 300), "sloped neck")
    dim(draw, (575, 430), (615, 285), "wing root")

    panel(draw, (1540, 140, 2320, 840), "Front design: head visible, wings behind shoulders")
    draw.line((1595, 765, 2265, 765), fill=(145, 137, 124, 255), width=3)
    # front chest, head, ears
    draw.rounded_rectangle((1835, 425, 2035, 575), radius=16, fill=WHITE, outline=INK, width=5)
    draw.polygon([(1878, 425), (1910, 260), (1968, 260), (2005, 425)], fill=WHITE, outline=INK)
    draw.polygon([(1895, 260), (1913, 195), (1936, 262)], fill=WHITE, outline=INK)
    draw.polygon([(1962, 260), (1990, 200), (1998, 270)], fill=WHITE, outline=INK)
    draw.ellipse((1910, 330, 1928, 348), fill=DARK)
    draw.ellipse((1969, 330, 1987, 348), fill=DARK)
    draw.rectangle((1912, 382, 1986, 410), fill=WARM_WHITE, outline=GREY, width=3)
    draw.line((1930, 412, 1975, 414), fill=(80, 88, 95, 255), width=3)
    draw.polygon([(1835, 468), (1615, 495), (1670, 585), (1835, 540)], fill=WHITE, outline=INK)
    draw.polygon([(2035, 468), (2255, 495), (2200, 585), (2035, 540)], fill=WHITE, outline=INK)
    for i in range(5):
        draw.line((1645 + i * 40, 525 + i * 8, 1835, 495 + i * 10), fill=GREY, width=3)
        draw.line((2035, 495 + i * 10, 2225 - i * 40, 525 + i * 8), fill=GREY, width=3)
    for x in [1870, 1965]:
        draw.rounded_rectangle((x, 575, x + 52, 765), radius=8, fill=WHITE, outline=INK, width=4)
        draw.rectangle((x - 8, 720, x + 64, 765), fill=GREY, outline=(105, 115, 122, 255), width=3)
    draw.text((1625, 795), "Reject if wings cover face or rider slot blocks wing roots.", fill=WARN, font=font(23))

    panel(draw, (48, 890, 1220, 1530), "45 degree design: head, rider slot, feather layers all readable")
    draw_horse_side(draw, 285, 940, 0.72)
    draw.rounded_rectangle((720, 1120, 880, 1160), radius=6, fill=(255, 241, 238, 255), outline=WARN, width=2)
    draw.text((738, 1127), "clear rider", fill=WARN, font=font(19))

    panel(draw, (1260, 890, 2320, 1530), "Approval checklist")
    y = 960
    checks = [
        "The side silhouette reads as an adult white horse before it reads as a winged unit.",
        "Head is small and elongated; facial details use geometry/UV, not oversized head scale.",
        "Neck slopes forward from withers and connects visibly to the chest and head.",
        "Wing roots sit behind shoulders; each side has upper wing, secondary wing, coverts, and at least 6 primary feathers.",
        "Pure white coat has grey planes, nostrils, mouth line, hoof, mane, and feather borders; no blank-looking faces.",
        "Self-flight and ridden-flight animation must keep saddle_space stable and wings clear of the player.",
    ]
    for item in checks:
        draw.text((1305, y), "□", fill=WARN, font=font(28))
        y = wrapped(draw, item, (1345, y + 2), 890, size=24)
        y += 16
    draw.rounded_rectangle((1305, 1450, 2245, 1504), radius=9, fill=(255, 241, 238, 255), outline=WARN, width=3)
    draw.text((1325, 1462), "Decision required: approve, request redraw, or change the design contract.", fill=WARN, font=font(24))
    image.save(path)


def snake_head(draw: ImageDraw.ImageDraw, hx: int, hy: int, scale: float = 1.0, flip: bool = False) -> None:
    s = scale
    direction = -1 if flip else 1
    pts = [
        (hx - direction * int(48 * s), hy - int(26 * s)),
        (hx + direction * int(22 * s), hy - int(36 * s)),
        (hx + direction * int(82 * s), hy),
        (hx + direction * int(24 * s), hy + int(38 * s)),
        (hx - direction * int(48 * s), hy + int(26 * s)),
    ]
    draw.polygon(pts, fill=(65, 123, 58, 255), outline=DARK_GREEN)
    draw.ellipse((hx + direction * int(14 * s) - 7, hy - int(11 * s), hx + direction * int(14 * s) + 7, hy + int(3 * s)), fill=(230, 240, 98, 255), outline=DARK_GREEN)
    draw.polygon([(hx + direction * int(28 * s), hy + int(28 * s)), (hx + direction * int(48 * s), hy + int(32 * s)), (hx + direction * int(38 * s), hy + int(56 * s))], fill=FANG, outline=DARK_GREEN)
    draw.polygon([(hx - direction * int(18 * s), hy + int(28 * s)), (hx + direction * int(35 * s), hy + int(40 * s)), (hx + direction * int(6 * s), hy + int(62 * s))], fill=MOUTH, outline=DARK_GREEN)
    draw.polygon([(hx - direction * int(9 * s), hy - int(40 * s)), (hx + direction * int(8 * s), hy - int(72 * s)), (hx + direction * int(25 * s), hy - int(38 * s))], fill=POISON, outline=DARK_GREEN)


def neck(draw: ImageDraw.ImageDraw, root: tuple[int, int], mid: tuple[int, int], tip: tuple[int, int], width: int = 30) -> None:
    draw.line((*root, *mid, *tip), fill=DARK_GREEN, width=width)
    draw.line((*root, *mid, *tip), fill=(36, 98, 52, 255), width=max(5, width - 10))
    for point, label in [(root, "root"), (mid, "mid"), (tip, "tip")]:
        x, y = point
        draw.ellipse((x - 11, y - 11, x + 11, y + 11), fill=POISON, outline=DARK_GREEN, width=2)
        if label == "root":
            draw.text((x - 16, y + 14), "socket", fill=DARK_GREEN, font=font(14))


def draw_hydra_sheet(path: Path) -> None:
    image = Image.new("RGBA", (2400, 1600), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), "Hydra V5 approval candidate: nine connected heads, low long body", fill=INK, font=font(42))
    draw.text((54, 88), "Candidate only. Current five-head/disconnected model is rejected and may not be repaired in place.", fill=MUTED, font=font(24))

    panel(draw, (48, 140, 1515, 880), "45 degree: nine sockets, nine connected neck paths")
    grid(draw, (90, 235, 1470, 825), 50)
    draw.line((96, 815, 1470, 815), fill=(145, 137, 124, 255), width=3)
    draw.polygon([(330, 570), (820, 505), (1230, 565), (1325, 675), (845, 770), (275, 705)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(330, 625), (820, 585), (1220, 635), (850, 740), (280, 692)], fill=(50, 101, 50, 255), outline=DARK_GREEN)
    draw.polygon([(300, 660), (120, 750), (82, 710), (270, 595)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1230, 625), (1455, 660), (1300, 745)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(565, 525), (820, 465), (1080, 530), (1010, 605), (610, 605)], fill=(45, 96, 49, 255), outline=DARK_GREEN)
    sockets = [(455, 548), (535, 532), (625, 515), (725, 505), (830, 505), (935, 518), (1030, 542), (1115, 585), (1180, 645)]
    heads = [(270, 415), (365, 345), (500, 300), (660, 270), (840, 268), (1010, 300), (1145, 365), (1240, 465), (1260, 575)]
    for i, (sx, sy) in enumerate(sockets, start=1):
        draw.rounded_rectangle((sx - 30, sy - 18, sx + 36, sy + 30), radius=8, fill=POISON, outline=DARK_GREEN, width=3)
        draw.text((sx - 11, sy - 11), f"{i}", fill=DARK_GREEN, font=font(18))
    for i, ((sx, sy), (hx, hy)) in enumerate(zip(sockets, heads), start=1):
        mid = ((sx + hx) // 2, min(sy, hy) - 55 - (i % 3) * 18)
        neck(draw, (sx + 4, sy + 8), mid, (hx - 34 if i < 5 else hx - 20, hy + 18), width=34 if i == 5 else 30)
    for i, (hx, hy) in enumerate(heads, start=1):
        snake_head(draw, hx, hy, scale=0.92, flip=False)
        draw.text((hx - 28, max(220, hy - 88)), f"H{i}", fill=DARK_GREEN, font=font(18))
    for x in [450, 900]:
        draw.rounded_rectangle((x, 735, x + 70, 820), radius=9, fill=GREEN, outline=DARK_GREEN, width=4)
    for x in range(600, 1110, 70):
        draw.polygon([(x, 504), (x + 28, 460), (x + 55, 512)], fill=POISON, outline=DARK_GREEN)
    draw.rounded_rectangle((500, 775, 1030, 818), radius=8, fill=(255, 243, 224, 255), outline=WARN, width=3)
    draw.text((520, 785), "Every neck_root overlaps a numbered socket on shoulder_saddle", fill=WARN, font=font(23))

    panel(draw, (1560, 140, 2320, 880), "Side design: low body, long tail, heads not inflated")
    draw.line((1615, 815, 2265, 815), fill=(145, 137, 124, 255), width=3)
    draw.polygon([(1660, 610), (1965, 575), (2210, 625), (1985, 700), (1655, 690)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(2200, 638), (2350, 675), (2185, 722)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1660, 660), (1510, 725), (1575, 622)], fill=GREEN, outline=DARK_GREEN)
    side = [(1715, 595, 1600, 445), (1810, 580, 1785, 380), (1940, 580, 2120, 430)]
    for sx, sy, hx, hy in side:
        neck(draw, (sx, sy), ((sx + hx) // 2, hy - 55), (hx - 24, hy + 12), width=28)
        snake_head(draw, hx, hy, scale=0.8)
    for x in [1740, 1970]:
        draw.rounded_rectangle((x, 690, x + 56, 815), radius=8, fill=GREEN, outline=DARK_GREEN, width=4)
    dim(draw, (1660, 748), (2210, 748), "3-4x body height")
    dim(draw, (1630, 610), (1630, 690), "low body <= 32% full silhouette")

    panel(draw, (48, 930, 1150, 1530), "Front fan: nine heads with gaps")
    draw.polygon([(330, 1310), (725, 1258), (950, 1360), (735, 1485), (330, 1458), (180, 1385)], fill=GREEN, outline=DARK_GREEN)
    roots = [(300, 1268), (380, 1235), (470, 1212), (570, 1198), (675, 1198), (780, 1212), (875, 1238), (955, 1275), (1005, 1330)]
    heads2 = [(145, 1188), (260, 1108), (380, 1055), (520, 1020), (675, 1010), (830, 1040), (970, 1100), (1052, 1195), (1058, 1305)]
    for i, (sx, sy) in enumerate(roots, start=1):
        draw.rounded_rectangle((sx - 24, sy - 16, sx + 32, sy + 24), radius=7, fill=POISON, outline=DARK_GREEN, width=3)
        draw.text((sx - 8, sy - 10), f"{i}", fill=DARK_GREEN, font=font(16))
    for i, ((sx, sy), (hx, hy)) in enumerate(zip(roots, heads2), start=1):
        neck(draw, (sx, sy), ((sx + hx) // 2, hy - 28 - (i % 2) * 22), (hx - 22, hy + 15), width=27)
    for hx, hy in heads2:
        snake_head(draw, hx, hy, scale=0.76)
    draw.text((145, 1472), "Reject if heads merge into one wall or any head lacks eye/mouth/fang detail.", fill=WARN, font=font(23))

    panel(draw, (1190, 930, 2320, 1530), "Approval checklist")
    y = 995
    checks = [
        "Exactly nine heads are visible unless the design package is formally revised first.",
        "Every head has a continuous neck_root -> neck_mid -> neck_tip -> head_core path.",
        "Each neck_root overlaps a named socket on shoulder_saddle; no floating or pasted-on heads.",
        "Body is low and long; torso mass does not replace multi-head silhouette.",
        "Each head has wedge shape, eye, mouth line, upper/lower jaw, fang, and horn or poison gland.",
        "Animation must preserve root/socket and tip/head overlap in idle, walk, attack, and special states.",
    ]
    for item in checks:
        draw.text((1235, y), "□", fill=WARN, font=font(28))
        y = wrapped(draw, item, (1275, y + 2), 940, size=24)
        y += 16
    draw.rounded_rectangle((1235, 1450, 2245, 1504), radius=9, fill=(255, 241, 238, 255), outline=WARN, width=3)
    draw.text((1255, 1462), "Decision required: approve, request redraw, or change the design contract.", fill=WARN, font=font(24))
    image.save(path)


def pegasus_contract() -> str:
    return """---
creature_id: pegasus
creature_name: Pegasus / 天马
design_version: v5_approval_candidate
design_status: candidate_needs_user_review
modeling_allowed: false
previous_assets_status: rejected_malformed
approval_sheet: docs/preview_assets/greek_creatures/design_approval/v5/pegasus_v5_approval_candidate_sheet.png
---

# Pegasus / 天马 V5 审批候选设计合同

## 结论

当前模型和旧设计均不正确。V5 不是建模许可，而是新的可审批设计图和合同。只有用户明确签收后，才能把对应设计包改为 `approved_for_modeling` 与 `modeling_allowed=true`。

## 造型目标

先是一匹正常成年白马，再是长出双翼的神话坐骑。识别顺序固定为：成年马体 -> 小而细长马头 -> 斜向颈部 -> 肩后双翼 -> 可载人飞行空间。

## 关键比例

- 主躯干长度为躯干高度的 `1.9` 到 `2.2` 倍。
- 头宽为身体宽度的 `0.35` 到 `0.45`，不允许大头化。
- 头高为胸腔高度的 `0.45` 到 `0.60`。
- 吻部长度大于头核心长度的 `0.55`。
- 颈部必须从鬐甲/肩胛斜向前上方伸出。
- 翼根必须在肩后；骑乘点不得遮挡头颈或翼根。

## Blockbench 结构

- `root/body/body_barrel/chest/rump/withers/saddle_space`
- `root/head_neck/neck_lower/neck_upper/mane_strip/head_core/muzzle/eyes/nostrils/mouth_line/ears`
- `root/wings/left_wing` 和 `root/wings/right_wing`，每侧含 `shoulder_joint/upper_wing/secondary_wing/coverts/primary_feathers_01_06`
- `root/legs/front_left/front_right/back_left/back_right/hoof`
- `root/tail/tail_base/tail_plume_segments`

## 纹理与 UV

纯白不是空白。头、鼻孔、嘴线、眼、鬃毛、马蹄、羽片边缘必须有固定 UV 区和浅灰分面。禁止使用随机白灰面板作为最终纹理。

## 否决条件

- 头太大、头看不见、颈部竖直、身体短胖。
- 翅膀像白条或平板，缺少羽片层级。
- 骑乘点挡住翼根或头颈。
- 白色贴图在 Blockbench 或 MC 中像透明/空白面。
"""


def hydra_contract() -> str:
    return """---
creature_id: hydra
creature_name: Hydra / 九头蛇
design_version: v5_approval_candidate
design_status: candidate_needs_user_review
modeling_allowed: false
previous_assets_status: rejected_wrong_head_count_and_disconnected
approval_sheet: docs/preview_assets/greek_creatures/design_approval/v5/hydra_v5_approval_candidate_sheet.png
---

# Hydra / 九头蛇 V5 审批候选设计合同

## 结论

当前五头/头墙式模型不正确，不能继续修。V5 重新定义九头蛇的可建模设计：低伏长身、默认九头、九个 socket、九条连续颈路径和每个头的面部细节。

## 造型目标

压迫感来自低伏长身、长尾、九头错层、颈根连接和毒性鳞片，不来自臃肿身体或大头堆叠。

## 关键比例与拓扑

- 默认头数：`9`。
- 主身体长度为身体高度的 `3.0` 到 `4.0` 倍。
- 主身体高度不超过整体视觉高度的 `0.32`。
- 每个头宽为肩带宽度的 `0.12` 到 `0.18`。
- 每个头必须有 `neck_root -> neck_mid -> neck_tip -> head_core` 连续路径。
- 每个 `neck_root` 必须与命名 `neck_socket` 和 `shoulder_saddle` 几何重叠。
- 每个 `neck_tip` 必须与 `head_core` 几何重叠。

## Blockbench 结构

- `root/body/slender_body/chest_keel/shoulder_saddle/neck_socket_01_09/spine_crests_01_08/tail_base/tail_mid/tail_tip`
- `root/necks/head_01_center` 到 `head_09_rear_right`
- 每个 head group 必须含 `neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`

## 纹理与 UV

每个头都有独立 `head_front_01_09`、`jaw_side_01_09` 和 `neck_scale_01_09` 纹理区。肩带必须有 `neck_socket_band` 阴影区，用于证明颈从身体长出。

## 否决条件

- 少于九头但没有先改设计包。
- 头和身体悬空或只靠命名暗示连接。
- 多头像插在背上的柱子。
- 身体臃肿、头墙化、单头缺少眼嘴獠牙等特征。
"""


def build_report() -> dict[str, Any]:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    CONTRACTS["pegasus"].parent.mkdir(parents=True, exist_ok=True)
    draw_pegasus_sheet(SHEETS["pegasus"])
    draw_hydra_sheet(SHEETS["hydra"])
    CONTRACTS["pegasus"].write_text(pegasus_contract(), encoding="utf-8")
    CONTRACTS["hydra"].write_text(hydra_contract(), encoding="utf-8")

    creatures = [
        {
            "id": "pegasus",
            "display": "Pegasus / 天马",
            "status": "candidate_needs_user_review",
            "approved_for_modeling": False,
            "modeling_allowed": False,
            "contract": project_path(CONTRACTS["pegasus"]),
            "candidate_sheet": project_path(SHEETS["pegasus"]),
            "supersedes": "v4_candidate_geometry_contract",
            "design_decision_required": "approve_or_request_redraw_before_modeling",
            "user_problem_answered": [
                "head_must_not_be_oversized",
                "must_reference_adult_horse_body_proportions",
                "head_neck_wing_root_rider_slot_must_be_visible_before_modeling",
                "self_flight_and_ridden_flight_must_share_clear_saddle_space",
            ],
            "must_approve_before_modeling": [
                "side view reads as adult pure-white horse first",
                "head is small elongated horse head and not hidden by wings",
                "neck slopes from withers and does not become a vertical tower",
                "wing roots sit behind shoulders with layered feathers",
                "pure-white texture plan has fixed face, hoof, mane, and feather UV zones",
            ],
        },
        {
            "id": "hydra",
            "display": "Hydra / 九头蛇",
            "status": "candidate_needs_user_review",
            "approved_for_modeling": False,
            "modeling_allowed": False,
            "contract": project_path(CONTRACTS["hydra"]),
            "candidate_sheet": project_path(SHEETS["hydra"]),
            "supersedes": "v4_candidate_geometry_contract",
            "design_decision_required": "approve_or_request_redraw_before_modeling",
            "user_problem_answered": [
                "current_five_head_model_is_rejected",
                "default_head_count_is_nine",
                "each_head_must_connect_to_body_via_neck_root_socket",
                "body_must_be_low_long_not_bulky",
            ],
            "must_approve_before_modeling": [
                "exactly nine heads are visible unless design package is revised",
                "nine sockets and nine neck roots are accepted",
                "each neck path connects root, mid, tip, and head core",
                "side view reads as low long body with long tail",
                "each head has eye, mouth line, jaw, fang, and horn or poison gland",
            ],
        },
    ]
    return {
        "version": 1,
        "scope": "pegasus_hydra_v5_design_approval_package",
        "ok": False,
        "status": "candidate_needs_user_review",
        "verdict": "not_approved_for_modeling",
        "rule": (
            "V5 is approval-ready design evidence, not production approval. "
            "Modeling stays blocked until the user approves these sheets/contracts."
        ),
        "blocked_count": len(creatures),
        "blocked": [item["id"] for item in creatures],
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# Pegasus / Hydra V5 设计审批候选包（2026-06-15）",
        "",
        "V5 用于纠正当前天马和九头蛇畸形问题。它是设计审批材料，不是建模放行结果。",
        "",
        f"- 状态：`{payload['status']}`",
        f"- 结论：`{payload['verdict']}`",
        f"- 建模允许：`{payload['ok']}`",
        "",
        "## 审批材料",
        "",
        "| 生物 | V5 合同 | V5 设计图 | 审批结论 |",
        "| --- | --- | --- | --- |",
    ]
    for item in payload["creatures"]:
        lines.append(f"| {item['display']} | `{item['contract']}` | `{item['candidate_sheet']}` | `{item['status']}` |")
    lines.extend(["", "## 设计图", ""])
    for item in payload["creatures"]:
        lines.extend([
            f"### {item['display']}",
            "",
            f"![{item['id']} V5 approval candidate](../../preview_assets/greek_creatures/design_approval/v5/{item['id']}_v5_approval_candidate_sheet.png)",
            "",
            "必须先确认：",
            "",
        ])
        for check in item["must_approve_before_modeling"]:
            lines.append(f"- [ ] {check}")
        lines.extend(["", "本轮针对用户反馈锁定：", ""])
        for answer in item["user_problem_answered"]:
            lines.append(f"- `{answer}`")
        lines.append("")
    lines.extend([
        "## 禁止事项",
        "",
        "- 不允许基于当前 `.bbmodel` 修天马或九头蛇。",
        "- 不允许用放大头部换取眼睛、嘴、獠牙等细节。",
        "- 不允许测试阶段重新发明造型；测试只能验证已批准设计是否落地。",
        "- 未审批前，不允许进入最终 Blockbench、UV、纹理、Java runtime 或动作同步。",
    ])
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_report()
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        print("Pegasus/Hydra V5 Design Approval Package: OPEN")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"status: {payload['status']}")
        print(f"blocked: {payload['blocked_count']} {payload['blocked']}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
