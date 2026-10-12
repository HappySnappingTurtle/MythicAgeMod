#!/usr/bin/env python3
"""Build a design approval package for rejected Greek creature remodels.

This package is deliberately pre-modeling evidence. It gives reviewers a
readable PNG design sheet and an old-vs-new comparison board before the
Blockbench or runtime Java model is touched again.
"""

from __future__ import annotations

import argparse
import json
import textwrap
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/greek_creatures/design_approval"
REPORT_PATH = OUT_DIR / "pegasus_hydra_design_approval_package.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_approval_package_2026-06-15.md"

BG = (242, 238, 229, 255)
PANEL = (255, 253, 247, 255)
INK = (34, 39, 46, 255)
MUTED = (91, 99, 110, 255)
FAIL = (154, 63, 55, 255)
OK = (58, 111, 79, 255)
WHITE = (248, 248, 242, 255)
GREY = (204, 211, 214, 255)
GREEN = (35, 74, 46, 255)
DARK_GREEN = (14, 31, 20, 255)
POISON = (124, 178, 82, 255)

FONT_PATHS = [
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/STHeiti Light.ttc",
]

CREATURES = {
    "pegasus": {
        "display": "Pegasus / 天马",
        "blueprint": "docs/美术资源/greek_creatures/design_blueprints/pegasus_v3_blueprint.md",
        "svg_sheet": "docs/美术资源/greek_creatures/design_sheets/pegasus_v3_design_sheet.svg",
        "png_sheet": "docs/preview_assets/greek_creatures/design_approval/pegasus_v3_design_sheet.png",
        "approval_board": "docs/preview_assets/greek_creatures/design_approval/pegasus_v3_design_approval_board.png",
        "detail_board": "docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_v3_proportion_detail_board.png",
        "detail_board_package": "docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_hydra_detail_design_boards.json",
        "old_preview": "docs/preview_assets/greek_creatures/pegasus/blockbench/side.png",
        "old_preview_2": "docs/preview_assets/greek_creatures/pegasus/blockbench/front.png",
        "must_approve": [
            "Side view reads as a white horse before it reads as a winged unit.",
            "Head is small, elongated, and forward of the chest; never a toy-like cube.",
            "Neck slopes from the withers; no vertical tower neck.",
            "Layered wings attach behind shoulders and do not hide head or rider space.",
            "Pure white texture plan still has eyes, nostrils, mouth, mane, hooves, and grey planes.",
        ],
        "detail_lock": "docs/美术资源/greek_creatures/design_sheets/pegasus_v3_proportion_lock.md",
        "detail_lock_must_approve": [
            "Original-horse-like body proportion is locked before modeling.",
            "Head, muzzle, and neck anchors are approved as small elongated horse anatomy.",
            "Wing root and rider space are approved without hiding the head or neck.",
        ],
        "old_failures": [
            "Old preview has vertical tower head/neck.",
            "Wing reads as flat horizontal strips.",
            "Horse body/neck/head relationship is not established.",
        ],
    },
    "hydra": {
        "display": "Hydra / 九头蛇",
        "blueprint": "docs/美术资源/greek_creatures/design_blueprints/hydra_v3_blueprint.md",
        "svg_sheet": "docs/美术资源/greek_creatures/design_sheets/hydra_v3_design_sheet.svg",
        "png_sheet": "docs/preview_assets/greek_creatures/design_approval/hydra_v3_design_sheet.png",
        "approval_board": "docs/preview_assets/greek_creatures/design_approval/hydra_v3_design_approval_board.png",
        "detail_board": "docs/preview_assets/greek_creatures/design_approval/detail_boards/hydra_v3_topology_detail_board.png",
        "detail_board_package": "docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_hydra_detail_design_boards.json",
        "old_preview": "docs/preview_assets/greek_creatures/hydra/blockbench/three_quarter.png",
        "old_preview_2": "docs/preview_assets/greek_creatures/hydra/blockbench/side.png",
        "must_approve": [
            "Default is nine heads unless the design is explicitly changed before modeling.",
            "Every neck has root, mid, tip, and overlaps a visible shoulder socket.",
            "Body is low and long; mass cannot become a front-facing cube.",
            "Each head has eyes, jaw split, mouth line, fang, and crest or poison gland.",
            "45 degree view shows staggered heads, not a single block wall.",
        ],
        "detail_lock": "docs/美术资源/greek_creatures/design_sheets/hydra_v3_topology_lock.md",
        "detail_lock_must_approve": [
            "Nine named sockets and nine neck roots are approved before modeling.",
            "Each head-neck-body connection is approved as real overlapping volume.",
            "Low long body and tail remain primary mass, not a front block.",
        ],
        "old_failures": [
            "Old preview reads as a blocky head wall.",
            "Head-neck-body connections are not structurally proven.",
            "Front body mass is too dominant for a low long hydra.",
        ],
    },
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def font(size: int, bold: bool = False) -> ImageFont.ImageFont:
    for path in FONT_PATHS:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size=size)
            except OSError:
                pass
    return ImageFont.load_default()


def read_front_matter(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}
    lines = path.read_text(encoding="utf-8").splitlines()
    if not lines or lines[0].strip() != "---":
        return {}
    data: dict[str, str] = {}
    for line in lines[1:]:
        if line.strip() == "---":
            break
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        data[key.strip()] = value.strip().strip('"')
    return data


def draw_wrapped(draw: ImageDraw.ImageDraw, text: str, xy: tuple[int, int], width: int,
                 fill: tuple[int, int, int, int] = INK, size: int = 24, line_gap: int = 8) -> int:
    x, y = xy
    fnt = font(size)
    avg = max(8, int(size * 0.52))
    chars = max(12, width // avg)
    for paragraph in text.splitlines() or [""]:
        for line in textwrap.wrap(paragraph, width=chars):
            draw.text((x, y), line, fill=fill, font=fnt)
            y += size + line_gap
    return y


def panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str) -> None:
    draw.rounded_rectangle(box, radius=14, fill=PANEL, outline=(125, 134, 143, 255), width=3)
    draw.text((box[0] + 24, box[1] + 18), title, fill=INK, font=font(30))


def draw_pegasus_sheet(path: Path) -> None:
    image = Image.new("RGBA", (1800, 1100), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), "Pegasus V3 design approval sheet", fill=INK, font=font(40))
    draw.text((54, 82), "Approval target: a pure-white horse first, then a flying rideable horse.", fill=MUTED, font=font(25))

    panel(draw, (50, 130, 1120, 610), "Side: horse silhouette first")
    draw.line((115, 555, 1065, 555), fill=(144, 137, 124, 255), width=3)
    draw.rounded_rectangle((330, 365, 775, 485), radius=16, fill=WHITE, outline=INK, width=5)
    draw.rectangle((345, 365, 455, 485), fill=GREY, outline=(92, 102, 111, 255), width=3)
    draw.rectangle((650, 365, 775, 485), fill=GREY, outline=(92, 102, 111, 255), width=3)
    draw.polygon([(405, 350), (500, 275), (545, 325), (470, 405)], fill=WHITE, outline=INK)
    draw.polygon([(485, 275), (595, 235), (675, 265), (630, 320), (545, 325)], fill=WHITE, outline=INK)
    draw.polygon([(665, 248), (840, 260), (1020, 320), (850, 350)], fill=WHITE, outline=INK)
    for offset in [0, 38, 76]:
        draw.polygon([(830 - offset, 350 + offset), (1035 - offset, 320 + offset), (1080 - offset, 356 + offset), (860 - offset, 386 + offset)], fill=(235, 239, 239, 255), outline=(96, 106, 115, 255))
    draw.polygon([(288, 330), (355, 320), (355, 365), (282, 377)], fill=WHITE, outline=INK)
    draw.polygon([(228, 320), (288, 330), (282, 377), (218, 370)], fill=WHITE, outline=INK)
    draw.polygon([(206, 328), (228, 320), (218, 370), (196, 372)], fill=WHITE, outline=INK)
    draw.polygon([(286, 312), (304, 274), (318, 318)], fill=WHITE, outline=INK)
    draw.polygon([(326, 318), (348, 284), (354, 330)], fill=WHITE, outline=INK)
    draw.ellipse((254, 340, 267, 353), fill=(30, 34, 40, 255))
    draw.rectangle((197, 349, 208, 355), fill=(30, 34, 40, 255))
    draw.line((230, 365, 272, 368), fill=(86, 91, 96, 255), width=3)
    draw.polygon([(385, 325), (415, 298), (475, 392), (445, 405)], fill=(226, 230, 231, 255), outline=(96, 106, 115, 255))
    for x in [385, 690]:
        draw.rounded_rectangle((x, 482, x + 50, 555), radius=8, fill=WHITE, outline=INK, width=4)
        draw.rectangle((x - 5, 540, x + 58, 575), fill=GREY, outline=(96, 106, 115, 255), width=3)
    draw.polygon([(775, 390), (900, 360), (875, 425), (778, 450)], fill=(226, 230, 231, 255), outline=(96, 106, 115, 255))
    draw.rounded_rectangle((520, 330, 670, 360), radius=6, fill=(255, 240, 240, 255), outline=FAIL, width=3)
    draw.text((535, 334), "rider space", fill=FAIL, font=font(20))
    draw.line((220, 255, 355, 255), fill=MUTED, width=3)
    draw.text((190, 224), "small elongated horse head", fill=MUTED, font=font(22))
    draw.line((385, 260, 515, 260), fill=MUTED, width=3)
    draw.text((386, 228), "sloped neck from withers", fill=MUTED, font=font(22))

    panel(draw, (1160, 130, 1750, 610), "Front: head visible, wings behind shoulders")
    draw.line((1208, 555, 1700, 555), fill=(144, 137, 124, 255), width=3)
    draw.rounded_rectangle((1355, 365, 1510, 490), radius=14, fill=WHITE, outline=INK, width=5)
    draw.polygon([(1382, 360), (1420, 250), (1453, 250), (1490, 360)], fill=WHITE, outline=INK)
    draw.polygon([(1400, 252), (1412, 210), (1425, 255)], fill=WHITE, outline=INK)
    draw.polygon([(1454, 252), (1472, 210), (1480, 258)], fill=WHITE, outline=INK)
    draw.ellipse((1414, 290, 1426, 302), fill=(30, 34, 40, 255))
    draw.ellipse((1452, 290, 1464, 302), fill=(30, 34, 40, 255))
    draw.rectangle((1405, 325, 1475, 350), fill=GREY, outline=(96, 106, 115, 255), width=2)
    draw.polygon([(1355, 382), (1190, 392), (1225, 445), (1355, 430)], fill=WHITE, outline=INK)
    draw.polygon([(1510, 382), (1675, 392), (1640, 445), (1510, 430)], fill=WHITE, outline=INK)
    for x in [1380, 1460]:
        draw.rounded_rectangle((x, 490, x + 42, 555), radius=7, fill=WHITE, outline=INK, width=4)

    panel(draw, (50, 650, 875, 1040), "45 degree: head, neck, wings, rider space all readable")
    draw.polygon([(260, 820), (615, 790), (740, 875), (340, 930)], fill=WHITE, outline=INK)
    draw.polygon([(260, 820), (380, 805), (455, 905), (340, 930)], fill=GREY, outline=(96, 106, 115, 255))
    draw.polygon([(300, 805), (380, 700), (435, 745), (365, 850)], fill=WHITE, outline=INK)
    draw.polygon([(350, 685), (440, 660), (500, 700), (420, 752)], fill=WHITE, outline=INK)
    draw.polygon([(292, 680), (355, 690), (345, 735), (275, 725)], fill=WHITE, outline=INK)
    draw.ellipse((315, 700, 328, 713), fill=(30, 34, 40, 255))
    draw.polygon([(475, 755), (650, 660), (815, 705), (610, 820)], fill=WHITE, outline=INK)
    for offset in [0, 35, 70]:
        draw.polygon([(610 - offset, 820 + offset), (815 - offset, 705 + offset), (842 - offset, 742 + offset), (640 - offset, 850 + offset)], fill=(235, 239, 239, 255), outline=(96, 106, 115, 255))
    draw.rounded_rectangle((500, 760, 650, 790), radius=6, fill=(255, 240, 240, 255), outline=FAIL, width=3)
    draw.text((515, 764), "rider space", fill=FAIL, font=font(20))
    for x, y in [(355, 915), (640, 895)]:
        draw.rounded_rectangle((x, y, x + 45, 990), radius=8, fill=WHITE, outline=INK, width=4)

    panel(draw, (915, 650, 1750, 1040), "Approval checklist")
    checklist = CREATURES["pegasus"]["must_approve"]
    y = 706
    for item in checklist:
        draw.text((950, y), "□", fill=FAIL, font=font(30))
        y = draw_wrapped(draw, item, (990, y + 2), 690, size=24)
        y += 12
    image.save(path)


def draw_hydra_sheet(path: Path) -> None:
    image = Image.new("RGBA", (1800, 1100), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), "Hydra V3 design approval sheet", fill=INK, font=font(40))
    draw.text((54, 82), "Approval target: low long body, nine connected necks, no blocky head wall.", fill=MUTED, font=font(25))

    panel(draw, (50, 130, 1120, 610), "45 degree: nine heads connect to visible sockets")
    draw.line((115, 555, 1065, 555), fill=(144, 137, 124, 255), width=3)
    draw.polygon([(285, 390), (720, 350), (965, 400), (990, 480), (675, 520), (245, 480)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(282, 445), (710, 425), (945, 460), (670, 510), (235, 475)], fill=(61, 106, 54, 255), outline=DARK_GREEN)
    draw.polygon([(270, 455), (130, 495), (90, 480), (235, 412)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(940, 435), (1080, 450), (985, 490)], fill=GREEN, outline=DARK_GREEN)
    for x in [350, 730]:
        draw.rounded_rectangle((x, 500, x + 62, 585), radius=9, fill=GREEN, outline=DARK_GREEN, width=4)
    sockets = [(390, 360), (440, 352), (490, 344), (545, 338), (602, 338), (660, 345), (715, 355), (770, 372), (820, 395)]
    for x, y in sockets:
        draw.rectangle((x, y, x + 42, y + 34), fill=POISON, outline=DARK_GREEN, width=3)
    head_points = [(230, 245), (285, 172), (370, 128), (500, 102), (675, 102), (825, 145), (930, 205), (1000, 285), (1035, 382)]
    for idx, ((sx, sy), (hx, hy)) in enumerate(zip(sockets, head_points)):
        midx = (sx + hx) // 2
        midy = min(sy, hy) - 55 - (idx % 3) * 12
        draw.line((sx + 21, sy + 4, midx, midy, hx, hy), fill=DARK_GREEN, width=38, joint="curve")
        draw.line((sx + 21, sy + 4, midx, midy, hx, hy), fill=(31, 90, 49, 255), width=30, joint="curve")
    for hx, hy in head_points:
        draw.polygon([(hx - 40, hy - 25), (hx + 20, hy - 32), (hx + 65, hy), (hx + 20, hy + 35), (hx - 38, hy + 28)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
        draw.polygon([(hx - 20, hy + 28), (hx + 30, hy + 36), (hx + 5, hy + 58)], fill=(23, 34, 25, 255), outline=DARK_GREEN)
        draw.ellipse((hx + 10, hy - 8, hx + 22, hy + 4), fill=(226, 240, 107, 255), outline=DARK_GREEN)
        draw.polygon([(hx + 32, hy + 30), (hx + 44, hy + 33), (hx + 37, hy + 50)], fill=(238, 231, 206, 255), outline=DARK_GREEN)
        draw.polygon([(hx - 10, hy - 38), (hx + 5, hy - 65), (hx + 18, hy - 35)], fill=POISON, outline=DARK_GREEN)
    draw.rounded_rectangle((410, 565, 775, 595), radius=6, fill=(255, 243, 223, 255), outline=(169, 109, 38, 255), width=3)
    draw.text((425, 569), "all neck roots overlap sockets", fill=(126, 76, 25, 255), font=font(22))

    panel(draw, (1160, 130, 1750, 610), "Side: low body and long tail")
    draw.line((1208, 555, 1700, 555), fill=(144, 137, 124, 255), width=3)
    draw.polygon([(1220, 430), (1510, 410), (1690, 444), (1510, 485), (1220, 478)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1510, 444), (1730, 456), (1580, 495)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1225, 450), (1160, 470), (1190, 425)], fill=GREEN, outline=DARK_GREEN)
    for x in [1290, 1468]:
        draw.rounded_rectangle((x, 478, x + 48, 558), radius=8, fill=GREEN, outline=DARK_GREEN, width=4)
    for sx, sy, hx, hy in [(1290, 430, 1180, 250), (1360, 425, 1300, 220), (1430, 420, 1600, 265)]:
        draw.line((sx, sy, (sx + hx) // 2, hy - 55, hx, hy), fill=DARK_GREEN, width=34)
        draw.line((sx, sy, (sx + hx) // 2, hy - 55, hx, hy), fill=(31, 90, 49, 255), width=26)
        draw.polygon([(hx - 42, hy - 24), (hx + 18, hy - 30), (hx + 62, hy), (hx + 20, hy + 32), (hx - 40, hy + 26)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
        draw.ellipse((hx + 10, hy - 6, hx + 22, hy + 6), fill=(226, 240, 107, 255), outline=DARK_GREEN)
    draw.text((1210, 575), "body height stays below one third of total silhouette", fill=MUTED, font=font(22))

    panel(draw, (50, 650, 875, 1040), "Front: fan, not a head wall")
    draw.polygon([(260, 820), (610, 800), (740, 880), (590, 945), (270, 925), (145, 875)], fill=GREEN, outline=DARK_GREEN)
    roots = [(250, 790), (305, 760), (365, 738), (430, 725), (495, 725), (560, 738), (620, 760), (675, 790)]
    heads = [(125, 700), (210, 640), (295, 585), (390, 535), (525, 535), (655, 585), (760, 650), (820, 725)]
    for sx, sy in roots:
        draw.rectangle((sx, sy, sx + 40, sy + 30), fill=POISON, outline=DARK_GREEN, width=3)
    for (sx, sy), (hx, hy) in zip(roots, heads):
        draw.line((sx + 20, sy, (sx + hx) // 2, hy - 30, hx, hy), fill=DARK_GREEN, width=34)
        draw.line((sx + 20, sy, (sx + hx) // 2, hy - 30, hx, hy), fill=(31, 90, 49, 255), width=26)
        draw.polygon([(hx - 34, hy - 20), (hx + 15, hy - 25), (hx + 50, hy), (hx + 16, hy + 28), (hx - 34, hy + 22)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
        draw.ellipse((hx + 7, hy - 5, hx + 18, hy + 6), fill=(226, 240, 107, 255), outline=DARK_GREEN)
    draw.text((105, 985), "front fan keeps gaps between heads and visible neck roots", fill=MUTED, font=font(22))

    panel(draw, (915, 650, 1750, 1040), "Approval checklist")
    checklist = CREATURES["hydra"]["must_approve"]
    y = 706
    for item in checklist:
        draw.text((950, y), "□", fill=FAIL, font=font(30))
        y = draw_wrapped(draw, item, (990, y + 2), 690, size=24)
        y += 12
    image.save(path)


def fit_image(path: Path, size: tuple[int, int]) -> Image.Image:
    try:
        image = Image.open(path).convert("RGBA")
    except FileNotFoundError:
        image = Image.new("RGBA", size, (245, 220, 220, 255))
        draw = ImageDraw.Draw(image)
        draw.text((20, 20), f"Missing: {project_path(path)}", fill=FAIL, font=font(22))
        return image
    image.thumbnail(size, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", size, (250, 247, 239, 255))
    canvas.alpha_composite(image, ((size[0] - image.width) // 2, (size[1] - image.height) // 2))
    return canvas


def draw_approval_board(creature_id: str, cfg: dict[str, Any]) -> None:
    board_path = ROOT / cfg["approval_board"]
    png_sheet_path = ROOT / cfg["png_sheet"]
    image = Image.new("RGBA", (1800, 1250), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), f"{cfg['display']} design approval board", fill=INK, font=font(40))
    draw.text((54, 82), "Left: rejected V2 evidence. Right: V3 candidate design to approve before modeling.", fill=MUTED, font=font(25))

    panel(draw, (50, 130, 865, 705), "Rejected V2 previews")
    old1 = fit_image(ROOT / cfg["old_preview"], (760, 245))
    old2 = fit_image(ROOT / cfg["old_preview_2"], (760, 245))
    image.alpha_composite(old1, (78, 200))
    image.alpha_composite(old2, (78, 455))
    y = 735
    draw.text((80, y), "Why V2 is rejected", fill=FAIL, font=font(28))
    y += 44
    for item in cfg["old_failures"]:
        draw.text((92, y), "✗", fill=FAIL, font=font(28))
        y = draw_wrapped(draw, item, (128, y + 3), 700, fill=INK, size=23)
        y += 8

    panel(draw, (920, 130, 1750, 705), "V3 candidate design")
    sheet = fit_image(png_sheet_path, (760, 520))
    image.alpha_composite(sheet, (955, 180))
    y = 735
    draw.text((950, y), "Approval must verify", fill=OK, font=font(28))
    y += 44
    for item in cfg["must_approve"]:
        draw.text((962, y), "□", fill=OK, font=font(28))
        y = draw_wrapped(draw, item, (1000, y + 3), 700, fill=INK, size=23)
        y += 8

    draw.rounded_rectangle((50, 1170, 1750, 1220), radius=10, fill=(255, 239, 238, 255), outline=FAIL, width=3)
    draw.text((78, 1182), "Current decision: APPROVAL PENDING. Modeling remains blocked until blueprint and design sheet are approved.", fill=FAIL, font=font(26))
    image.save(board_path)


def draw_pegasus_sheet_safe(path: Path) -> None:
    image = Image.new("RGBA", (1800, 1200), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), "Pegasus V3 design approval sheet", fill=INK, font=font(40))
    draw.text((54, 82), "Approval target: pure-white horse proportions first; wings and riding flight second.", fill=MUTED, font=font(25))

    panel(draw, (50, 130, 1150, 570), "Side view: horse silhouette first")
    draw.line((110, 530, 1090, 530), fill=(144, 137, 124, 255), width=3)
    draw.rounded_rectangle((390, 355, 760, 450), radius=14, fill=WHITE, outline=INK, width=5)
    draw.rectangle((405, 355, 500, 450), fill=GREY, outline=(92, 102, 111, 255), width=3)
    draw.rectangle((655, 355, 760, 450), fill=GREY, outline=(92, 102, 111, 255), width=3)
    draw.polygon([(455, 342), (530, 284), (575, 330), (520, 390)], fill=WHITE, outline=INK)
    draw.polygon([(520, 286), (610, 250), (675, 275), (640, 325), (575, 330)], fill=WHITE, outline=INK)
    draw.polygon([(645, 270), (820, 280), (1040, 340), (835, 370)], fill=WHITE, outline=INK)
    for offset in [0, 34, 68]:
        draw.polygon([(820 - offset, 372 + offset), (1040 - offset, 340 + offset), (1082 - offset, 374 + offset), (842 - offset, 405 + offset)], fill=(235, 239, 239, 255), outline=(96, 106, 115, 255))
    draw.polygon([(315, 325), (382, 318), (382, 360), (308, 370)], fill=WHITE, outline=INK)
    draw.polygon([(248, 317), (315, 325), (308, 370), (238, 365)], fill=WHITE, outline=INK)
    draw.polygon([(225, 325), (248, 317), (238, 365), (214, 368)], fill=WHITE, outline=INK)
    draw.polygon([(310, 310), (327, 275), (342, 316)], fill=WHITE, outline=INK)
    draw.polygon([(350, 314), (370, 282), (378, 326)], fill=WHITE, outline=INK)
    draw.ellipse((278, 336, 291, 349), fill=(30, 34, 40, 255))
    draw.rectangle((216, 345, 228, 352), fill=(30, 34, 40, 255))
    draw.line((248, 360, 288, 363), fill=(86, 91, 96, 255), width=3)
    draw.polygon([(435, 324), (465, 300), (522, 390), (492, 405)], fill=(226, 230, 231, 255), outline=(96, 106, 115, 255))
    for x in [440, 685]:
        draw.rounded_rectangle((x, 448, x + 48, 526), radius=8, fill=WHITE, outline=INK, width=4)
        draw.rectangle((x - 5, 512, x + 56, 545), fill=GREY, outline=(96, 106, 115, 255), width=3)
    draw.polygon([(760, 382), (900, 350), (875, 410), (764, 438)], fill=(226, 230, 231, 255), outline=(96, 106, 115, 255))
    draw.rounded_rectangle((555, 323, 705, 353), radius=6, fill=(255, 240, 240, 255), outline=FAIL, width=3)
    draw.text((570, 327), "rider space", fill=FAIL, font=font(20))
    draw.text((145, 230), "small elongated head", fill=MUTED, font=font(22))
    draw.line((205, 260, 360, 260), fill=MUTED, width=3)
    draw.text((420, 228), "sloped neck from withers", fill=MUTED, font=font(22))
    draw.line((475, 260, 590, 260), fill=MUTED, width=3)

    panel(draw, (1190, 130, 1750, 570), "Front view: head stays visible")
    draw.line((1240, 530, 1700, 530), fill=(144, 137, 124, 255), width=3)
    draw.rounded_rectangle((1390, 360, 1510, 465), radius=12, fill=WHITE, outline=INK, width=5)
    draw.polygon([(1410, 358), (1440, 255), (1468, 255), (1492, 358)], fill=WHITE, outline=INK)
    draw.polygon([(1425, 258), (1437, 220), (1450, 260)], fill=WHITE, outline=INK)
    draw.polygon([(1468, 258), (1484, 220), (1492, 263)], fill=WHITE, outline=INK)
    draw.ellipse((1435, 295, 1447, 307), fill=(30, 34, 40, 255))
    draw.ellipse((1470, 295, 1482, 307), fill=(30, 34, 40, 255))
    draw.rectangle((1428, 328, 1485, 350), fill=GREY, outline=(96, 106, 115, 255), width=2)
    draw.polygon([(1390, 385), (1248, 395), (1282, 442), (1390, 426)], fill=WHITE, outline=INK)
    draw.polygon([(1510, 385), (1652, 395), (1618, 442), (1510, 426)], fill=WHITE, outline=INK)
    for x in [1410, 1470]:
        draw.rounded_rectangle((x, 465, x + 38, 530), radius=7, fill=WHITE, outline=INK, width=4)

    panel(draw, (50, 620, 880, 1130), "45 degree: head, neck, wings, rider space")
    draw.polygon([(275, 825), (600, 795), (725, 870), (350, 930)], fill=WHITE, outline=INK)
    draw.polygon([(275, 825), (385, 810), (455, 905), (350, 930)], fill=GREY, outline=(96, 106, 115, 255))
    draw.polygon([(310, 810), (390, 715), (440, 755), (370, 850)], fill=WHITE, outline=INK)
    draw.polygon([(350, 700), (430, 678), (485, 715), (415, 760)], fill=WHITE, outline=INK)
    draw.polygon([(290, 697), (350, 705), (342, 745), (272, 735)], fill=WHITE, outline=INK)
    draw.ellipse((314, 714, 327, 727), fill=(30, 34, 40, 255))
    draw.polygon([(475, 770), (645, 685), (810, 728), (610, 835)], fill=WHITE, outline=INK)
    for offset in [0, 34, 68]:
        draw.polygon([(610 - offset, 835 + offset), (810 - offset, 728 + offset), (838 - offset, 762 + offset), (640 - offset, 864 + offset)], fill=(235, 239, 239, 255), outline=(96, 106, 115, 255))
    draw.rounded_rectangle((505, 775, 650, 805), radius=6, fill=(255, 240, 240, 255), outline=FAIL, width=3)
    draw.text((520, 779), "rider space", fill=FAIL, font=font(20))
    for x, y in [(365, 925), (630, 900)]:
        draw.rounded_rectangle((x, y, x + 42, 1010), radius=8, fill=WHITE, outline=INK, width=4)

    panel(draw, (920, 620, 1750, 1130), "Approval checklist")
    y = 684
    for item in CREATURES["pegasus"]["must_approve"]:
        draw.text((955, y), "□", fill=FAIL, font=font(26))
        y = draw_wrapped(draw, item, (992, y + 2), 690, size=23)
        y += 8
    image.save(path)


def draw_hydra_sheet_safe(path: Path) -> None:
    image = Image.new("RGBA", (1800, 1200), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), "Hydra V3 design approval sheet", fill=INK, font=font(40))
    draw.text((54, 82), "Approval target: low long body, nine connected necks, no blocky head wall.", fill=MUTED, font=font(25))

    panel(draw, (50, 130, 1150, 620), "45 degree: nine heads connect to visible sockets")
    draw.line((110, 570, 1090, 570), fill=(144, 137, 124, 255), width=3)
    draw.polygon([(275, 430), (710, 395), (950, 445), (975, 510), (670, 545), (235, 512)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(275, 470), (700, 452), (930, 486), (665, 535), (230, 500)], fill=(61, 106, 54, 255), outline=DARK_GREEN)
    draw.polygon([(260, 486), (128, 528), (90, 512), (230, 450)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(930, 470), (1080, 485), (980, 528)], fill=GREEN, outline=DARK_GREEN)
    sockets = [(390, 400), (440, 392), (490, 384), (545, 378), (602, 378), (660, 385), (715, 395), (770, 412), (820, 435)]
    heads = [(235, 275), (300, 235), (385, 208), (505, 190), (660, 190), (805, 220), (915, 270), (990, 335), (1030, 420)]
    for x, y in sockets:
        draw.rectangle((x, y, x + 42, y + 34), fill=POISON, outline=DARK_GREEN, width=3)
    for idx, ((sx, sy), (hx, hy)) in enumerate(zip(sockets, heads)):
        midx = (sx + hx) // 2
        midy = min(sy, hy) - 35 - (idx % 2) * 12
        draw.line((sx + 21, sy + 4, midx, midy, hx, hy), fill=DARK_GREEN, width=32)
        draw.line((sx + 21, sy + 4, midx, midy, hx, hy), fill=(31, 90, 49, 255), width=24)
    for hx, hy in heads:
        draw.polygon([(hx - 34, hy - 20), (hx + 18, hy - 27), (hx + 56, hy), (hx + 18, hy + 29), (hx - 34, hy + 23)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
        draw.polygon([(hx - 18, hy + 24), (hx + 28, hy + 32), (hx + 6, hy + 48)], fill=(23, 34, 25, 255), outline=DARK_GREEN)
        draw.ellipse((hx + 9, hy - 7, hx + 20, hy + 4), fill=(226, 240, 107, 255), outline=DARK_GREEN)
        draw.polygon([(hx + 29, hy + 25), (hx + 38, hy + 28), (hx + 33, hy + 43)], fill=(238, 231, 206, 255), outline=DARK_GREEN)
        draw.polygon([(hx - 8, hy - 30), (hx + 6, hy - 54), (hx + 18, hy - 28)], fill=POISON, outline=DARK_GREEN)
    for x in [350, 730]:
        draw.rounded_rectangle((x, 525, x + 58, 600), radius=8, fill=GREEN, outline=DARK_GREEN, width=4)
    draw.rounded_rectangle((420, 585, 780, 612), radius=6, fill=(255, 243, 223, 255), outline=(169, 109, 38, 255), width=3)
    draw.text((435, 588), "all neck roots overlap sockets", fill=(126, 76, 25, 255), font=font(20))

    panel(draw, (1190, 130, 1750, 620), "Side view: low body and long tail")
    draw.line((1240, 570, 1700, 570), fill=(144, 137, 124, 255), width=3)
    draw.polygon([(1230, 440), (1500, 420), (1685, 452), (1500, 492), (1230, 485)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1500, 452), (1728, 462), (1585, 505)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1235, 462), (1165, 485), (1200, 435)], fill=GREEN, outline=DARK_GREEN)
    for sx, sy, hx, hy in [(1295, 438, 1185, 295), (1375, 432, 1318, 270), (1450, 428, 1600, 305)]:
        draw.line((sx, sy, (sx + hx) // 2, hy - 35, hx, hy), fill=DARK_GREEN, width=30)
        draw.line((sx, sy, (sx + hx) // 2, hy - 35, hx, hy), fill=(31, 90, 49, 255), width=22)
        draw.polygon([(hx - 35, hy - 20), (hx + 16, hy - 25), (hx + 55, hy), (hx + 18, hy + 28), (hx - 35, hy + 22)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
        draw.ellipse((hx + 8, hy - 5, hx + 18, hy + 5), fill=(226, 240, 107, 255), outline=DARK_GREEN)
    for x in [1300, 1480]:
        draw.rounded_rectangle((x, 485, x + 46, 565), radius=8, fill=GREEN, outline=DARK_GREEN, width=4)
    draw.text((1225, 590), "body height stays below one third of total silhouette", fill=MUTED, font=font(22))

    panel(draw, (50, 670, 880, 1130), "Front view: fan, not a head wall")
    draw.polygon([(260, 890), (610, 870), (730, 940), (590, 1000), (270, 980), (150, 930)], fill=GREEN, outline=DARK_GREEN)
    roots = [(250, 852), (305, 825), (365, 806), (430, 795), (495, 795), (560, 806), (620, 825), (675, 852)]
    heads = [(125, 775), (210, 732), (300, 695), (392, 665), (525, 665), (655, 695), (755, 740), (820, 790)]
    for sx, sy in roots:
        draw.rectangle((sx, sy, sx + 40, sy + 30), fill=POISON, outline=DARK_GREEN, width=3)
    for (sx, sy), (hx, hy) in zip(roots, heads):
        draw.line((sx + 20, sy, (sx + hx) // 2, hy - 25, hx, hy), fill=DARK_GREEN, width=30)
        draw.line((sx + 20, sy, (sx + hx) // 2, hy - 25, hx, hy), fill=(31, 90, 49, 255), width=22)
        draw.polygon([(hx - 30, hy - 18), (hx + 14, hy - 23), (hx + 45, hy), (hx + 15, hy + 25), (hx - 30, hy + 20)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
        draw.ellipse((hx + 6, hy - 5, hx + 16, hy + 5), fill=(226, 240, 107, 255), outline=DARK_GREEN)
    draw.text((105, 1050), "front fan keeps gaps between heads and visible neck roots", fill=MUTED, font=font(22))

    panel(draw, (920, 670, 1750, 1130), "Approval checklist")
    y = 734
    for item in CREATURES["hydra"]["must_approve"]:
        draw.text((955, y), "□", fill=FAIL, font=font(26))
        y = draw_wrapped(draw, item, (992, y + 2), 690, size=23)
        y += 8
    image.save(path)


def build_report() -> dict[str, Any]:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    draw_pegasus_sheet_safe(ROOT / CREATURES["pegasus"]["png_sheet"])
    draw_hydra_sheet_safe(ROOT / CREATURES["hydra"]["png_sheet"])
    for creature_id, cfg in CREATURES.items():
        draw_approval_board(creature_id, cfg)

    creatures: list[dict[str, Any]] = []
    for creature_id, cfg in CREATURES.items():
        blueprint_path = ROOT / cfg["blueprint"]
        front = read_front_matter(blueprint_path)
        design_status = front.get("design_status")
        sheet_status = front.get("design_sheet_status")
        correctness_verdict = front.get("design_correctness_verdict")
        detail_sheet = front.get("design_detail_sheet")
        detail_sheet_status = front.get("design_detail_sheet_status")
        detail_board = front.get("design_detail_board")
        detail_board_status = front.get("design_detail_board_status")
        modeling_allowed = front.get("modeling_allowed", "").lower() == "true"
        approved = (
            design_status == "approved_for_modeling"
            and sheet_status == "approved_for_modeling"
            and correctness_verdict == "approved_for_modeling"
            and detail_sheet_status == "approved_for_modeling"
            and detail_board_status == "approved_for_modeling"
            and modeling_allowed
        )
        creatures.append({
            "id": creature_id,
            "display": cfg["display"],
            "status": "approved_for_modeling" if approved else "approval_pending",
            "approved_for_modeling": approved,
            "blueprint": cfg["blueprint"],
            "svg_design_sheet": cfg["svg_sheet"],
            "png_design_sheet": cfg["png_sheet"],
            "approval_board": cfg["approval_board"],
            "detail_lock": cfg["detail_lock"],
            "detail_lock_exists": (ROOT / cfg["detail_lock"]).exists(),
            "detail_board": cfg["detail_board"],
            "detail_board_exists": (ROOT / cfg["detail_board"]).exists(),
            "detail_board_package": cfg["detail_board_package"],
            "detail_board_package_exists": (ROOT / cfg["detail_board_package"]).exists(),
            "detail_lock_must_approve": cfg["detail_lock_must_approve"],
            "old_preview": cfg["old_preview"],
            "old_preview_2": cfg["old_preview_2"],
            "design_status": design_status,
            "design_sheet_status": sheet_status,
            "design_correctness_verdict": correctness_verdict,
            "design_detail_sheet": detail_sheet,
            "design_detail_sheet_status": detail_sheet_status,
            "design_detail_board": detail_board,
            "design_detail_board_status": detail_board_status,
            "modeling_allowed": modeling_allowed,
            "old_failures": cfg["old_failures"],
            "must_approve": cfg["must_approve"],
            "next_action": (
                "proceed_to_v3_modeling"
                if approved
                else "user_review_blueprint_detail_lock_png_sheet_and_approval_board"
            ),
        })
    blocked = [item["id"] for item in creatures if not item["approved_for_modeling"]]
    return {
        "version": 1,
        "scope": "pegasus_hydra_design_approval_package",
        "ok": not blocked,
        "status": "approved_for_modeling" if not blocked else "approval_pending",
        "blocked_count": len(blocked),
        "blocked": blocked,
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# Pegasus / Hydra V3 设计审批包（2026-06-15）",
        "",
        "本文档用于建模前审批。它不表示设计已通过；只有用户确认蓝图、比例/拓扑锁定表和设计图后，才允许重新进入 Blockbench、纹理、动作和 Java runtime。",
        "",
        f"- 当前状态：`{payload['status']}`",
        f"- 阻断数量：{payload['blocked_count']}",
        "",
        "## 审批项",
        "",
        "| 生物 | 状态 | 蓝图 | 细化锁定表 | 细化设计板 | PNG 设计图 | 旧/新对照板 | 下一步 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in payload["creatures"]:
        lines.append(
            "| `{id}` | `{status}` | `{blueprint}` | `{detail}` | `{detail_board}` | `{sheet}` | `{board}` | `{next}` |".format(
                id=item["id"],
                status=item["status"],
                blueprint=item["blueprint"],
                detail=item["detail_lock"],
                detail_board=item["detail_board"],
                sheet=item["png_design_sheet"],
                board=item["approval_board"],
                next=item["next_action"],
            )
        )
    lines.extend(["", "## 设计图预览", ""])
    for item in payload["creatures"]:
        lines.extend([
            f"### {item['display']}",
            "",
            f"![{item['id']} design sheet](../../preview_assets/greek_creatures/design_approval/{item['id']}_v3_design_sheet.png)",
            "",
            f"![{item['id']} approval board](../../preview_assets/greek_creatures/design_approval/{item['id']}_v3_design_approval_board.png)",
            "",
            f"- 设计正确性结论：`{item['design_correctness_verdict']}`",
            f"- 细化锁定状态：`{item['design_detail_sheet_status']}`",
            f"- 细化设计板状态：`{item['design_detail_board_status']}`",
            f"- 细化锁定表：`{item['detail_lock']}`",
            f"- 细化设计板：`{item['detail_board']}`",
            "",
            f"![{item['id']} detail board](../../preview_assets/greek_creatures/design_approval/detail_boards/{item['id']}_v3_{'proportion' if item['id'] == 'pegasus' else 'topology'}_detail_board.png)",
            "",
            "必须确认：",
            "",
        ])
        for check in item["must_approve"]:
            lines.append(f"- [ ] {check}")
        lines.extend(["", "细化锁定必须确认：", ""])
        for check in item["detail_lock_must_approve"]:
            lines.append(f"- [ ] {check}")
        lines.extend(["", "旧设计拒绝原因：", ""])
        for failure in item["old_failures"]:
            lines.append(f"- {failure}")
        lines.append("")
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
        status = "PASS" if payload["ok"] else "APPROVAL PENDING"
        print(f"Pegasus/Hydra Design Approval Package: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"blocked: {payload['blocked_count']} {payload['blocked']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
