#!/usr/bin/env python3
"""Generate detail design boards for Pegasus and Hydra V3 approval.

These boards sit between the text lock sheets and Blockbench modeling. They
visualize the exact failure points from user review: Pegasus horse proportions
and Hydra head-neck-body connectivity.
"""

from __future__ import annotations

import argparse
import json
import textwrap
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/greek_creatures/design_approval/detail_boards"
REPORT_PATH = OUT_DIR / "pegasus_hydra_detail_design_boards.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_detail_design_boards_2026-06-15.md"

PEGASUS_BOARD = OUT_DIR / "pegasus_v3_proportion_detail_board.png"
HYDRA_BOARD = OUT_DIR / "hydra_v3_topology_detail_board.png"

BG = (242, 238, 229, 255)
PANEL = (255, 253, 247, 255)
INK = (32, 36, 42, 255)
MUTED = (86, 94, 104, 255)
FAIL = (159, 64, 57, 255)
OK = (53, 111, 75, 255)
WHITE = (248, 248, 242, 255)
GREY = (202, 209, 212, 255)
LIGHT_GREY = (231, 235, 236, 255)
GREEN = (35, 74, 46, 255)
DARK_GREEN = (14, 31, 20, 255)
MID_GREEN = (43, 99, 55, 255)
POISON = (124, 178, 82, 255)
YELLOW = (226, 240, 107, 255)

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


def draw_wrapped(draw: ImageDraw.ImageDraw, text: str, xy: tuple[int, int], width: int,
                 fill: tuple[int, int, int, int] = INK, size: int = 24,
                 line_gap: int = 7) -> int:
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
    draw.rounded_rectangle(box, radius=12, fill=PANEL, outline=(127, 136, 144, 255), width=3)
    draw.text((box[0] + 22, box[1] + 16), title, fill=INK, font=font(30))


def arrow(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int],
          color: tuple[int, int, int, int] = FAIL, width: int = 4) -> None:
    draw.line((*start, *end), fill=color, width=width)
    x1, y1 = start
    x2, y2 = end
    dx = 1 if x2 >= x1 else -1
    dy = 1 if y2 >= y1 else -1
    draw.polygon([(x2, y2), (x2 - dx * 18, y2), (x2, y2 - dy * 18)], fill=color)


def draw_pegasus_board(path: Path) -> None:
    image = Image.new("RGBA", (1900, 1400), BG)
    draw = ImageDraw.Draw(image)
    draw.text((56, 38), "Pegasus V3 proportion detail board", fill=INK, font=font(44))
    draw.text((56, 88), "Purpose: lock original-horse-like proportions before any Blockbench rebuild.", fill=MUTED, font=font(26))

    panel(draw, (50, 135, 1250, 760), "Side proportion lock: horse first, wings second")
    draw.line((110, 700, 1190, 700), fill=(139, 132, 119, 255), width=3)

    body_x, body_y, body_w, body_h = 465, 455, 440, 125
    draw.rounded_rectangle((body_x, body_y, body_x + body_w, body_y + body_h), radius=14, fill=WHITE, outline=INK, width=5)
    draw.rectangle((body_x + 18, body_y, body_x + 128, body_y + body_h), fill=GREY, outline=(92, 102, 111, 255), width=3)
    draw.rectangle((body_x + 318, body_y, body_x + body_w, body_y + body_h), fill=GREY, outline=(92, 102, 111, 255), width=3)
    draw.text((body_x + 130, body_y + 47), "body_barrel", fill=MUTED, font=font(22))
    draw.line((body_x, body_y - 28, body_x + body_w, body_y - 28), fill=OK, width=4)
    draw.rounded_rectangle((body_x + 505, body_y + 16, body_x + 880, body_y + 52),
                           radius=5, fill=(240, 248, 238, 255), outline=OK, width=2)
    draw.text((body_x + 518, body_y + 21), "body length = 1.8-2.2x torso height", fill=OK, font=font(21))

    withers = (body_x + 90, body_y - 8)
    neck_mid = (body_x + 18, body_y - 105)
    head_back = (body_x - 165, body_y - 125)
    head_front = (body_x - 310, body_y - 108)
    draw.ellipse((withers[0] - 8, withers[1] - 8, withers[0] + 8, withers[1] + 8), fill=FAIL)
    draw.text((withers[0] - 88, withers[1] + 24), "withers_anchor", fill=FAIL, font=font(20))
    draw.polygon([withers, neck_mid, (neck_mid[0] + 45, neck_mid[1] + 48), (withers[0] + 48, withers[1] + 20)],
                 fill=WHITE, outline=INK)
    draw.polygon([(neck_mid[0] - 15, neck_mid[1] + 3), (head_back[0], head_back[1]),
                  (head_back[0] + 40, head_back[1] + 52), (neck_mid[0] + 28, neck_mid[1] + 45)],
                 fill=WHITE, outline=INK)
    draw.polygon([(head_back[0] - 10, head_back[1] - 25), (head_back[0] + 70, head_back[1] - 35),
                  (head_back[0] + 118, head_back[1] + 7), (head_back[0] + 70, head_back[1] + 50),
                  (head_back[0] - 10, head_back[1] + 42)],
                 fill=WHITE, outline=INK)
    draw.polygon([(head_front[0], head_front[1] - 16), (head_back[0], head_back[1] - 12),
                  (head_back[0] - 10, head_back[1] + 35), (head_front[0] - 12, head_front[1] + 24)],
                 fill=WHITE, outline=INK)
    draw.polygon([(head_front[0] - 12, head_front[1] - 8), (head_front[0] - 43, head_front[1] + 5),
                  (head_front[0] - 18, head_front[1] + 20)], fill=WHITE, outline=INK)
    draw.ellipse((head_back[0] + 68, head_back[1] - 7, head_back[0] + 82, head_back[1] + 7), fill=(28, 31, 36, 255))
    draw.rectangle((head_front[0] - 30, head_front[1] + 5, head_front[0] - 20, head_front[1] + 12), fill=(28, 31, 36, 255))
    draw.line((head_front[0] + 2, head_front[1] + 25, head_back[0] + 45, head_back[1] + 30), fill=(85, 90, 96, 255), width=3)
    draw.polygon([(head_back[0] + 50, head_back[1] - 38), (head_back[0] + 66, head_back[1] - 80), (head_back[0] + 80, head_back[1] - 36)], fill=WHITE, outline=INK)
    draw.polygon([(head_back[0] + 95, head_back[1] - 30), (head_back[0] + 120, head_back[1] - 70), (head_back[0] + 120, head_back[1] - 22)], fill=WHITE, outline=INK)
    draw.text((110, 275), "muzzle length = 0.55-0.75x head core", fill=OK, font=font(22))
    draw.line((190, 308, head_back[0] + 100, head_back[1] - 45), fill=OK, width=3)
    draw.rounded_rectangle((112, 385, 505, 420), radius=5, fill=(240, 248, 238, 255), outline=OK, width=2)
    draw.text((126, 390), "head width = 0.35-0.45x body width", fill=OK, font=font(21))

    wing_root = (body_x + 135, body_y + 8)
    draw.ellipse((wing_root[0] - 9, wing_root[1] - 9, wing_root[0] + 9, wing_root[1] + 9), fill=FAIL)
    draw.text((wing_root[0] + 10, wing_root[1] - 52), "wing root behind shoulder", fill=FAIL, font=font(21))
    draw.polygon([wing_root, (body_x + 325, body_y - 125), (body_x + 640, body_y - 72), (body_x + 355, body_y + 10)],
                 fill=WHITE, outline=INK)
    for offset in [0, 36, 72, 108]:
        draw.polygon([(body_x + 350 - offset, body_y + 12 + offset),
                      (body_x + 640 - offset, body_y - 72 + offset),
                      (body_x + 682 - offset, body_y - 32 + offset),
                      (body_x + 382 - offset, body_y + 47 + offset)],
                     fill=LIGHT_GREY, outline=(96, 106, 115, 255))
    draw.rounded_rectangle((body_x + 165, body_y - 47, body_x + 330, body_y - 14), radius=5, fill=(255, 240, 240, 255), outline=FAIL, width=3)
    draw.text((body_x + 182, body_y - 43), "rider space", fill=FAIL, font=font(20))
    arrow(draw, (body_x + 250, body_y - 14), (body_x + 250, body_y + 18), FAIL)

    for x in [body_x + 50, body_x + 345]:
        draw.rounded_rectangle((x, body_y + body_h - 2, x + 52, 700), radius=8, fill=WHITE, outline=INK, width=4)
        draw.rectangle((x - 6, 678, x + 60, 720), fill=GREY, outline=(96, 106, 115, 255), width=3)
    draw.text((body_x + 28, 730), "legs: upper/lower/hoof, not four blank pillars", fill=MUTED, font=font(20))

    panel(draw, (1285, 135, 1848, 760), "Head / neck construction detail")
    y = 190
    for text in [
        "1. Head center must sit forward of the chest, not above the back.",
        "2. Neck is two or three sloped segments from withers_anchor.",
        "3. Muzzle is a real cuboid/mesh section, not only a texture line.",
        "4. Eye, nostril, mouth, ear, mane strip and hoof details are required.",
        "5. Do not enlarge the head to make facial details readable.",
        "6. Pure white material must still show grey planes and UV detail.",
    ]:
        y = draw_wrapped(draw, "- " + text, (1315, y), 500, fill=INK, size=23)
        y += 12
    draw.rounded_rectangle((1315, 650, 1815, 715), radius=8, fill=(255, 239, 238, 255), outline=FAIL, width=3)
    draw.text((1340, 668), "Board status: DRAFT / REQUIRES USER APPROVAL", fill=FAIL, font=font(23))

    panel(draw, (50, 805, 920, 1318), "Front / rider clearance")
    draw.line((110, 1268, 850, 1268), fill=(139, 132, 119, 255), width=3)
    draw.rounded_rectangle((405, 1050, 560, 1190), radius=14, fill=WHITE, outline=INK, width=5)
    draw.polygon([(428, 1045), (470, 880), (515, 880), (548, 1045)], fill=WHITE, outline=INK)
    draw.polygon([(452, 885), (468, 820), (488, 888)], fill=WHITE, outline=INK)
    draw.polygon([(514, 885), (538, 824), (546, 892)], fill=WHITE, outline=INK)
    draw.ellipse((464, 940, 478, 954), fill=(28, 31, 36, 255))
    draw.ellipse((508, 940, 522, 954), fill=(28, 31, 36, 255))
    draw.rectangle((454, 990, 522, 1016), fill=GREY, outline=(96, 106, 115, 255), width=2)
    draw.polygon([(405, 1068), (160, 1085), (210, 1160), (405, 1122)], fill=WHITE, outline=INK)
    draw.polygon([(560, 1068), (805, 1085), (755, 1160), (560, 1122)], fill=WHITE, outline=INK)
    draw.rounded_rectangle((330, 1018, 635, 1048), radius=5, fill=(255, 240, 240, 255), outline=FAIL, width=2)
    draw.text((350, 1021), "rider must not hide head or wing roots", fill=FAIL, font=font(20))
    draw.text((150, 1225), "front must show head, two eyes, ears, narrow chest, and wing roots", fill=MUTED, font=font(22))

    panel(draw, (960, 805, 1848, 1318), "Approval checklist")
    y = 870
    for text in [
        "Side silhouette reads as a normal white horse before reading as a winged unit.",
        "Head is small, elongated and forward of chest; no toy cube head.",
        "withers_anchor, neck_lower, neck_upper and head_core are connected by real volume.",
        "Wings attach behind shoulder and show primary feather separation.",
        "Rider space stays on the back and does not hide the head, neck or wing root.",
        "Texture plan has grey structure planes, eye, nostril, mouth, mane, hoof detail.",
    ]:
        draw.text((1000, y), "□", fill=OK, font=font(27))
        y = draw_wrapped(draw, text, (1038, y + 1), 750, fill=INK, size=23)
        y += 15

    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def draw_snake_head_connection(draw: ImageDraw.ImageDraw, origin: tuple[int, int], label: str) -> None:
    x, y = origin
    draw.rectangle((x, y + 220, x + 120, y + 270), fill=POISON, outline=DARK_GREEN, width=4)
    draw.text((x - 6, y + 276), "neck_socket", fill=MUTED, font=font(18))
    draw.line((x + 60, y + 222, x + 120, y + 150, x + 178, y + 105), fill=DARK_GREEN, width=50)
    draw.line((x + 60, y + 222, x + 120, y + 150, x + 178, y + 105), fill=MID_GREEN, width=36)
    draw.line((x + 178, y + 105, x + 250, y + 80), fill=DARK_GREEN, width=46)
    draw.line((x + 178, y + 105, x + 250, y + 80), fill=MID_GREEN, width=32)
    draw.polygon([(x + 220, y + 45), (x + 310, y + 35), (x + 385, y + 82),
                  (x + 315, y + 132), (x + 220, y + 120)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
    draw.polygon([(x + 238, y + 118), (x + 315, y + 138), (x + 278, y + 168)], fill=(23, 34, 25, 255), outline=DARK_GREEN)
    draw.ellipse((x + 294, y + 70, x + 312, y + 88), fill=YELLOW, outline=DARK_GREEN)
    draw.line((x + 305, y + 112, x + 360, y + 100), fill=(16, 26, 18, 255), width=4)
    draw.polygon([(x + 330, y + 122), (x + 346, y + 125), (x + 338, y + 152)], fill=(238, 231, 206, 255), outline=DARK_GREEN)
    draw.polygon([(x + 258, y + 40), (x + 282, y), (x + 295, y + 42)], fill=POISON, outline=DARK_GREEN)
    draw.text((x, y), label, fill=INK, font=font(23))
    draw.text((x + 20, y + 190), "neck_root overlaps socket", fill=FAIL, font=font(19))
    draw.text((x + 132, y + 145), "neck_mid", fill=MUTED, font=font(18))
    draw.text((x + 225, y + 145), "neck_tip overlaps head_core", fill=FAIL, font=font(18))


def draw_hydra_board(path: Path) -> None:
    image = Image.new("RGBA", (1900, 1400), BG)
    draw = ImageDraw.Draw(image)
    draw.text((56, 38), "Hydra V3 topology detail board", fill=INK, font=font(44))
    draw.text((56, 88), "Purpose: prove nine head-neck-body connections before any Blockbench rebuild.", fill=MUTED, font=font(26))

    panel(draw, (50, 135, 1280, 780), "45 degree topology: 9 sockets and connected necks")
    draw.line((105, 720, 1218, 720), fill=(139, 132, 119, 255), width=3)
    body = [(300, 512), (780, 470), (1085, 532), (1120, 625), (760, 690), (245, 640)]
    draw.polygon(body, fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(300, 575), (770, 552), (1058, 600), (760, 675), (245, 630)], fill=(61, 106, 54, 255), outline=DARK_GREEN)
    draw.polygon([(290, 590), (135, 642), (90, 625), (255, 540)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1060, 585), (1235, 606), (1120, 660)], fill=GREEN, outline=DARK_GREEN)
    for x in [410, 850]:
        draw.rounded_rectangle((x, 668, x + 62, 760), radius=9, fill=GREEN, outline=DARK_GREEN, width=4)

    sockets = [
        (420, 482), (480, 470), (545, 460), (615, 452), (690, 452),
        (765, 462), (835, 478), (900, 505), (955, 540),
    ]
    heads = [
        (240, 360), (325, 285), (450, 240), (610, 215), (765, 218),
        (930, 255), (1045, 325), (1120, 410), (1135, 520),
    ]
    for idx, (x, y) in enumerate(sockets, start=1):
        draw.rectangle((x, y, x + 48, y + 38), fill=POISON, outline=DARK_GREEN, width=3)
        draw.text((x + 7, y + 7), f"{idx:02d}", fill=(17, 30, 20, 255), font=font(18))
    for idx, ((sx, sy), (hx, hy)) in enumerate(zip(sockets, heads), start=1):
        midx = (sx + hx) // 2
        midy = min(sy, hy) - 40 - (idx % 3) * 10
        draw.line((sx + 24, sy + 4, midx, midy, hx, hy), fill=DARK_GREEN, width=36)
        draw.line((sx + 24, sy + 4, midx, midy, hx, hy), fill=MID_GREEN, width=26)
    for idx, (hx, hy) in enumerate(heads, start=1):
        draw.polygon([(hx - 38, hy - 24), (hx + 20, hy - 30), (hx + 65, hy),
                      (hx + 20, hy + 34), (hx - 38, hy + 27)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
        draw.polygon([(hx - 20, hy + 27), (hx + 30, hy + 38), (hx + 6, hy + 58)], fill=(23, 34, 25, 255), outline=DARK_GREEN)
        draw.ellipse((hx + 10, hy - 8, hx + 22, hy + 4), fill=YELLOW, outline=DARK_GREEN)
        draw.polygon([(hx + 32, hy + 30), (hx + 45, hy + 33), (hx + 38, hy + 52)], fill=(238, 231, 206, 255), outline=DARK_GREEN)
        draw.text((hx - 14, hy - 62), f"H{idx:02d}", fill=INK, font=font(18))
    draw.rounded_rectangle((360, 730, 970, 765), radius=6, fill=(255, 243, 223, 255), outline=(169, 109, 38, 255), width=3)
    draw.text((378, 735), "each neck_root must overlap its numbered socket", fill=(126, 76, 25, 255), font=font(21))

    panel(draw, (1315, 135, 1848, 780), "Single head connection rule")
    draw_snake_head_connection(draw, (1360, 245), "Required per head")
    y = 555
    for text in [
        "neck_root overlaps neck_socket",
        "neck_mid creates visible curve",
        "neck_tip overlaps head_core",
        "upper_jaw / lower_jaw split is modeled",
        "eye, mouth line, fang, crest/poison gland are visible",
    ]:
        draw.text((1360, y), "□", fill=OK, font=font(25))
        y = draw_wrapped(draw, text, (1396, y + 1), 390, fill=INK, size=21)
        y += 10

    panel(draw, (50, 820, 930, 1320), "Front fan: visible gaps and sockets")
    draw.polygon([(300, 1100), (620, 1065), (785, 1145), (625, 1220), (285, 1195), (145, 1130)], fill=GREEN, outline=DARK_GREEN)
    roots = [(250, 1080), (310, 1048), (375, 1028), (445, 1015), (520, 1015), (595, 1028), (660, 1050), (720, 1082)]
    heads = [(130, 998), (225, 940), (325, 892), (430, 862), (575, 862), (710, 895), (825, 955), (875, 1030)]
    for sx, sy in roots:
        draw.rectangle((sx, sy, sx + 42, sy + 31), fill=POISON, outline=DARK_GREEN, width=3)
    for (sx, sy), (hx, hy) in zip(roots, heads):
        draw.line((sx + 21, sy, (sx + hx) // 2, hy - 28, hx, hy), fill=DARK_GREEN, width=32)
        draw.line((sx + 21, sy, (sx + hx) // 2, hy - 28, hx, hy), fill=MID_GREEN, width=23)
        draw.polygon([(hx - 31, hy - 19), (hx + 14, hy - 24), (hx + 50, hy),
                      (hx + 16, hy + 27), (hx - 31, hy + 21)], fill=(45, 111, 57, 255), outline=DARK_GREEN)
        draw.ellipse((hx + 7, hy - 5, hx + 17, hy + 5), fill=YELLOW, outline=DARK_GREEN)
    draw.text((110, 1265), "front fan keeps visible gaps between heads and visible neck roots", fill=MUTED, font=font(22))

    panel(draw, (965, 820, 1848, 1320), "Approval checklist")
    y = 880
    for text in [
        "Default V3 has nine heads; any five-head simplification must be a design change.",
        "All nine sockets are named and visible on shoulder_saddle.",
        "Every head group has neck_root, neck_mid, neck_tip and head_core overlap.",
        "Body is low and long; body mass must not become a front-facing cube.",
        "Each head has eyes, jaw split, mouth line, fang and crest/poison gland.",
        "45 degree view shows staggered heads, not a single block wall.",
        "Animation must preserve neck-root and head-neck contact in all frames.",
    ]:
        draw.text((1000, y), "□", fill=OK, font=font(27))
        y = draw_wrapped(draw, text, (1038, y + 1), 750, fill=INK, size=23)
        y += 12
    draw.rounded_rectangle((1000, 1235, 1810, 1298), radius=8, fill=(255, 239, 238, 255), outline=FAIL, width=3)
    draw.text((1030, 1254), "Board status: DRAFT / REQUIRES USER APPROVAL", fill=FAIL, font=font(23))

    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def build_report() -> dict[str, Any]:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    draw_pegasus_board(PEGASUS_BOARD)
    draw_hydra_board(HYDRA_BOARD)
    creatures = [
        {
            "id": "pegasus",
            "display": "Pegasus / 天马",
            "status": "draft_requires_user_approval",
            "detail_board": project_path(PEGASUS_BOARD),
            "detail_lock": "docs/美术资源/greek_creatures/design_sheets/pegasus_v3_proportion_lock.md",
            "blueprint": "docs/美术资源/greek_creatures/design_blueprints/pegasus_v3_blueprint.md",
            "must_approve": [
                "horse-like body length and chest/rump mass",
                "small elongated head and real muzzle",
                "sloped withers-to-head neck segments",
                "wing root behind shoulder and rider clearance",
                "white material still has visible structure detail",
            ],
        },
        {
            "id": "hydra",
            "display": "Hydra / 九头蛇",
            "status": "draft_requires_user_approval",
            "detail_board": project_path(HYDRA_BOARD),
            "detail_lock": "docs/美术资源/greek_creatures/design_sheets/hydra_v3_topology_lock.md",
            "blueprint": "docs/美术资源/greek_creatures/design_blueprints/hydra_v3_blueprint.md",
            "must_approve": [
                "nine heads remain the 1.0 default",
                "nine numbered sockets are visible on shoulder_saddle",
                "each neck_root overlaps socket and each neck_tip overlaps head_core",
                "body remains low and long with a long tail",
                "each head keeps eye, jaw split, mouth line, fang and crest/poison gland detail",
            ],
        },
    ]
    return {
        "version": 1,
        "scope": "pegasus_hydra_v3_detail_design_boards",
        "ok": False,
        "status": "draft_requires_user_approval",
        "rule": (
            "These boards are pre-modeling evidence. They do not unlock modeling until "
            "the user approves the blueprint, detail lock sheet, detail board, and approval board."
        ),
        "blocked": ["pegasus", "hydra"],
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# Pegasus / Hydra V3 细化设计板（2026-06-15）",
        "",
        "本文档由 `tools/pegasus_hydra_detail_design_boards.py audit` 生成。它补足文字锁定表和粗略三视图之间的缺口，但仍不表示设计已批准。",
        "",
        f"- 当前状态：`{payload['status']}`",
        f"- 规则：{payload['rule']}",
        "",
        "## 细化板",
        "",
    ]
    for item in payload["creatures"]:
        lines.extend([
            f"### {item['display']}",
            "",
            f"- 状态：`{item['status']}`",
            f"- 蓝图：`{item['blueprint']}`",
            f"- 锁定表：`{item['detail_lock']}`",
            f"- 细化板：`{item['detail_board']}`",
            "",
            f"![{item['id']} detail board](../../preview_assets/greek_creatures/design_approval/detail_boards/{item['id']}_v3_{'proportion' if item['id'] == 'pegasus' else 'topology'}_detail_board.png)",
            "",
            "必须确认：",
            "",
        ])
        for check in item["must_approve"]:
            lines.append(f"- [ ] {check}")
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
        print("Pegasus/Hydra Detail Design Boards: APPROVAL PENDING")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"blocked: {payload['blocked']}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
