#!/usr/bin/env python3
"""Build a stricter V4 design candidate package for Pegasus and Hydra.

The package is deliberately pre-modeling evidence. It answers the current
design-correctness problem before any Blockbench or Java model work continues:

* Pegasus must first be a correctly proportioned pure-white horse.
* Hydra must first be a low long body with nine physically connected necks.

The generated contracts remain candidate material until the user explicitly
approves them. This script therefore exits non-zero while approval is pending.
"""

from __future__ import annotations

import argparse
import json
import textwrap
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/greek_creatures/design_approval/v4"
REPORT_PATH = OUT_DIR / "pegasus_hydra_v4_candidate_package.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v4_candidate_package_2026-06-15.md"

BG = (241, 238, 230, 255)
PANEL = (255, 253, 247, 255)
INK = (33, 38, 45, 255)
MUTED = (90, 99, 110, 255)
WARNING = (156, 74, 50, 255)
OK = (43, 111, 73, 255)
WHITE = (248, 248, 242, 255)
LIGHT_GREY = (207, 214, 217, 255)
MID_GREY = (158, 168, 173, 255)
GREEN = (39, 83, 48, 255)
DARK_GREEN = (14, 34, 20, 255)
SCALE_GREEN = (62, 119, 56, 255)
POISON = (149, 198, 81, 255)
MOUTH = (54, 26, 32, 255)
FANG = (238, 232, 210, 255)

FONT_PATHS = [
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/STHeiti Light.ttc",
]

CONTRACTS = {
    "pegasus": ROOT / "docs/美术资源/greek_creatures/design_blueprints/pegasus_v4_candidate_modeling_contract.md",
    "hydra": ROOT / "docs/美术资源/greek_creatures/design_blueprints/hydra_v4_candidate_modeling_contract.md",
}

SHEETS = {
    "pegasus": OUT_DIR / "pegasus_v4_final_candidate_sheet.png",
    "hydra": OUT_DIR / "hydra_v4_final_candidate_sheet.png",
}


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


def draw_wrapped(
    draw: ImageDraw.ImageDraw,
    text: str,
    xy: tuple[int, int],
    width: int,
    *,
    fill: tuple[int, int, int, int] = INK,
    size: int = 24,
    line_gap: int = 8,
) -> int:
    x, y = xy
    fnt = font(size)
    avg = max(7, int(size * 0.52))
    chars = max(10, width // avg)
    for paragraph in text.splitlines() or [""]:
        for line in textwrap.wrap(paragraph, width=chars):
            draw.text((x, y), line, fill=fill, font=fnt)
            y += size + line_gap
    return y


def panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str) -> None:
    draw.rounded_rectangle(box, radius=14, fill=PANEL, outline=(128, 138, 146, 255), width=3)
    draw.text((box[0] + 24, box[1] + 18), title, fill=INK, font=font(30))


def draw_dimension(draw: ImageDraw.ImageDraw, a: tuple[int, int], b: tuple[int, int], label: str) -> None:
    draw.line((a[0], a[1], b[0], b[1]), fill=WARNING, width=3)
    draw.ellipse((a[0] - 4, a[1] - 4, a[0] + 4, a[1] + 4), fill=WARNING)
    draw.ellipse((b[0] - 4, b[1] - 4, b[0] + 4, b[1] + 4), fill=WARNING)
    mid = ((a[0] + b[0]) // 2, (a[1] + b[1]) // 2)
    draw.text((mid[0] + 10, mid[1] - 18), label, fill=WARNING, font=font(21))


def draw_pegasus_sheet(path: Path) -> None:
    image = Image.new("RGBA", (2200, 1500), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 38), "Pegasus V4 final candidate design contract", fill=INK, font=font(42))
    draw.text(
        (54, 88),
        "Not approved for modeling. Proves horse proportions, visible head detail, shoulder wings, and rideable flight space.",
        fill=MUTED,
        font=font(25),
    )

    panel(draw, (50, 140, 1420, 690), "Side view: adult horse body first, wings second")
    draw.line((115, 635, 1370, 635), fill=(143, 137, 126, 255), width=3)

    # Horse body: long horizontal barrel with distinct chest and rump.
    draw.rounded_rectangle((540, 420, 1025, 555), radius=18, fill=WHITE, outline=INK, width=5)
    draw.rectangle((560, 420, 690, 555), fill=LIGHT_GREY, outline=(96, 106, 115, 255), width=3)
    draw.rectangle((900, 420, 1025, 555), fill=LIGHT_GREY, outline=(96, 106, 115, 255), width=3)
    draw.polygon([(560, 405), (655, 372), (815, 383), (905, 415), (560, 425)], fill=(242, 244, 241, 255), outline=INK)
    draw.rounded_rectangle((720, 384, 900, 418), radius=8, fill=(255, 240, 238, 255), outline=WARNING, width=3)
    draw.text((736, 390), "rider slot clear", fill=WARNING, font=font(20))

    # Sloped neck and small elongated horse head.
    draw.polygon([(565, 402), (465, 315), (510, 285), (625, 414)], fill=WHITE, outline=INK)
    draw.polygon([(470, 296), (350, 290), (270, 318), (360, 355), (475, 342)], fill=WHITE, outline=INK)
    draw.polygon([(275, 318), (210, 332), (205, 370), (356, 358)], fill=WHITE, outline=INK)
    draw.polygon([(372, 285), (392, 238), (412, 290)], fill=WHITE, outline=INK)
    draw.polygon([(438, 287), (466, 245), (476, 300)], fill=WHITE, outline=INK)
    draw.ellipse((322, 321, 338, 337), fill=(25, 29, 34, 255))
    draw.rectangle((224, 348, 238, 356), fill=(25, 29, 34, 255))
    draw.line((292, 365, 352, 368), fill=(76, 84, 91, 255), width=4)
    draw.polygon([(535, 365), (572, 340), (633, 420), (598, 438)], fill=LIGHT_GREY, outline=(96, 106, 115, 255))

    # Wing root and layered feathers.
    draw.polygon([(720, 394), (930, 270), (1215, 350), (940, 445)], fill=(252, 252, 247, 255), outline=INK)
    for i in range(7):
        base_x = 905 - i * 34
        base_y = 438 + i * 18
        draw.polygon(
            [(base_x, base_y), (1210 - i * 34, 350 + i * 18), (1266 - i * 31, 382 + i * 18), (942 - i * 34, base_y + 44)],
            fill=(235, 239, 239, 255),
            outline=(96, 106, 115, 255),
        )
    draw.ellipse((700, 382, 770, 445), fill=LIGHT_GREY, outline=INK, width=4)

    # Legs and tail.
    for x, bend in [(585, -35), (705, -15), (880, 25), (980, 45)]:
        draw.rounded_rectangle((x, 548, x + 54, 635), radius=9, fill=WHITE, outline=INK, width=4)
        draw.polygon([(x + 7, 635), (x + 60, 635), (x + 72 + bend // 6, 666), (x - 8, 666)], fill=MID_GREY, outline=(96, 106, 115, 255))
    draw.polygon([(1020, 450), (1215, 405), (1168, 510), (1024, 532)], fill=LIGHT_GREY, outline=(96, 106, 115, 255))

    draw_dimension(draw, (540, 590), (1025, 590), "body length target")
    draw_dimension(draw, (270, 256), (475, 256), "small elongated head")
    draw_dimension(draw, (585, 360), (475, 305), "sloped neck")
    draw_dimension(draw, (720, 350), (720, 445), "wing root behind shoulder")

    panel(draw, (1460, 140, 2150, 690), "Front view: head not hidden, wings behind shoulders")
    draw.line((1508, 635, 2100, 635), fill=(143, 137, 126, 255), width=3)
    draw.rounded_rectangle((1695, 420, 1875, 550), radius=15, fill=WHITE, outline=INK, width=5)
    draw.polygon([(1740, 420), (1770, 288), (1810, 288), (1845, 420)], fill=WHITE, outline=INK)
    draw.polygon([(1755, 288), (1772, 235), (1790, 290)], fill=WHITE, outline=INK)
    draw.polygon([(1810, 288), (1830, 235), (1842, 292)], fill=WHITE, outline=INK)
    draw.ellipse((1767, 335, 1782, 350), fill=(25, 29, 34, 255))
    draw.ellipse((1814, 335, 1829, 350), fill=(25, 29, 34, 255))
    draw.rectangle((1760, 382, 1836, 407), fill=LIGHT_GREY, outline=(96, 106, 115, 255), width=3)
    draw.polygon([(1695, 455), (1518, 468), (1560, 530), (1695, 500)], fill=WHITE, outline=INK)
    draw.polygon([(1875, 455), (2052, 468), (2010, 530), (1875, 500)], fill=WHITE, outline=INK)
    for x in [1728, 1810]:
        draw.rounded_rectangle((x, 550, x + 48, 635), radius=8, fill=WHITE, outline=INK, width=4)
    draw.text((1550, 584), "wings do not cover face", fill=WARNING, font=font(22))

    panel(draw, (50, 740, 1100, 1430), "45 degree: readable head, neck, wing root, rider slot")
    draw.line((110, 1365, 1040, 1365), fill=(143, 137, 126, 255), width=3)
    draw.polygon([(360, 1045), (790, 1000), (955, 1092), (510, 1190)], fill=WHITE, outline=INK)
    draw.polygon([(362, 1045), (500, 1022), (580, 1174), (510, 1190)], fill=LIGHT_GREY, outline=(96, 106, 115, 255))
    draw.polygon([(430, 1020), (545, 890), (612, 942), (500, 1090)], fill=WHITE, outline=INK)
    draw.polygon([(500, 878), (632, 838), (720, 890), (612, 950)], fill=WHITE, outline=INK)
    draw.polygon([(388, 875), (505, 880), (494, 944), (360, 930)], fill=WHITE, outline=INK)
    draw.ellipse((435, 902, 452, 919), fill=(25, 29, 34, 255))
    draw.rectangle((374, 910, 388, 918), fill=(25, 29, 34, 255))
    draw.line((410, 932, 468, 938), fill=(76, 84, 91, 255), width=4)
    draw.polygon([(600, 980), (815, 805), (1040, 875), (770, 1095)], fill=WHITE, outline=INK)
    for i in range(7):
        draw.polygon(
            [(760 - i * 35, 1090 + i * 22), (1040 - i * 35, 875 + i * 22), (1088 - i * 32, 915 + i * 22), (802 - i * 35, 1130 + i * 22)],
            fill=(235, 239, 239, 255),
            outline=(96, 106, 115, 255),
        )
    draw.rounded_rectangle((650, 955, 830, 992), radius=7, fill=(255, 240, 238, 255), outline=WARNING, width=3)
    draw.text((666, 961), "rider slot", fill=WARNING, font=font(21))
    for x, y in [(465, 1182), (760, 1138)]:
        draw.rounded_rectangle((x, y, x + 50, y + 128), radius=9, fill=WHITE, outline=INK, width=4)
        draw.rectangle((x - 7, y + 110, x + 62, y + 146), fill=MID_GREY, outline=(96, 106, 115, 255), width=3)

    panel(draw, (1145, 740, 2150, 1430), "Hard gates before Blockbench")
    checks = [
        "Side silhouette reads as adult white horse before winged unit.",
        "Head height is 45-60% of chest height; head width is 35-45% of body width.",
        "Muzzle is longer than half the head core; eyes, nostrils, and mouth line have fixed UV zones.",
        "Neck starts at withers and slopes forward; no vertical tower neck.",
        "Wing root sits behind shoulder; at least 6 primary feathers per side.",
        "Rider slot remains clear during self-flight and ridden flight.",
        "Pure white texture uses grey planes, not blank/transparent-looking faces.",
    ]
    y = 805
    for item in checks:
        draw.text((1190, y), "□", fill=WARNING, font=font(28))
        y = draw_wrapped(draw, item, (1230, y + 3), 840, size=24)
        y += 14
    draw.rounded_rectangle((1190, 1328, 2085, 1385), radius=10, fill=(255, 240, 238, 255), outline=WARNING, width=3)
    draw.text((1210, 1341), "Decision: candidate only. modeling_allowed=false until user approval.", fill=WARNING, font=font(24))

    image.save(path)


def draw_snake_head(draw: ImageDraw.ImageDraw, hx: int, hy: int, scale: float = 1.0) -> None:
    s = scale
    pts = [
        (hx - int(44 * s), hy - int(24 * s)),
        (hx + int(20 * s), hy - int(32 * s)),
        (hx + int(70 * s), hy),
        (hx + int(20 * s), hy + int(34 * s)),
        (hx - int(44 * s), hy + int(26 * s)),
    ]
    draw.polygon(pts, fill=SCALE_GREEN, outline=DARK_GREEN)
    draw.polygon(
        [(hx - int(20 * s), hy + int(28 * s)), (hx + int(34 * s), hy + int(38 * s)), (hx + int(8 * s), hy + int(58 * s))],
        fill=MOUTH,
        outline=DARK_GREEN,
    )
    draw.ellipse(
        (hx + int(10 * s), hy - int(9 * s), hx + int(24 * s), hy + int(5 * s)),
        fill=(230, 240, 98, 255),
        outline=DARK_GREEN,
    )
    draw.polygon(
        [(hx + int(34 * s), hy + int(28 * s)), (hx + int(48 * s), hy + int(32 * s)), (hx + int(40 * s), hy + int(52 * s))],
        fill=FANG,
        outline=DARK_GREEN,
    )
    draw.polygon(
        [(hx - int(10 * s), hy - int(35 * s)), (hx + int(6 * s), hy - int(66 * s)), (hx + int(22 * s), hy - int(34 * s))],
        fill=POISON,
        outline=DARK_GREEN,
    )


def draw_neck(draw: ImageDraw.ImageDraw, start: tuple[int, int], mid: tuple[int, int], head: tuple[int, int], *, width: int) -> None:
    draw.line((start[0], start[1], mid[0], mid[1], head[0], head[1]), fill=DARK_GREEN, width=width)
    draw.line((start[0], start[1], mid[0], mid[1], head[0], head[1]), fill=(35, 96, 51, 255), width=max(4, width - 10))
    # Segment rings make root/mid/tip readable.
    for point in [start, mid, head]:
        draw.ellipse((point[0] - 10, point[1] - 10, point[0] + 10, point[1] + 10), fill=POISON, outline=DARK_GREEN, width=2)


def draw_hydra_sheet(path: Path) -> None:
    image = Image.new("RGBA", (2200, 1500), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 38), "Hydra V4 final candidate design contract", fill=INK, font=font(42))
    draw.text(
        (54, 88),
        "Not approved for modeling. Proves nine sockets, connected neck roots, low long body, and per-head face detail.",
        fill=MUTED,
        font=font(25),
    )

    panel(draw, (50, 140, 1440, 770), "45 degree topology")
    draw.line((110, 715, 1390, 715), fill=(143, 137, 126, 255), width=3)
    draw.polygon([(310, 522), (855, 465), (1205, 535), (1265, 640), (820, 705), (260, 650)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(315, 585), (845, 553), (1180, 600), (825, 680), (260, 638)], fill=(54, 101, 49, 255), outline=DARK_GREEN)
    draw.polygon([(300, 610), (105, 678), (72, 655), (260, 548)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1190, 580), (1385, 600), (1268, 662)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(540, 482), (772, 430), (1040, 485), (996, 545), (600, 550)], fill=(46, 98, 51, 255), outline=DARK_GREEN)

    sockets = [
        (470, 492), (535, 478), (605, 466), (680, 455), (760, 455),
        (840, 466), (915, 482), (985, 508), (1040, 545),
    ]
    heads = [
        (295, 335), (388, 275), (505, 235), (650, 205), (825, 205),
        (985, 250), (1110, 320), (1192, 410), (1218, 520),
    ]
    for i, (sx, sy) in enumerate(sockets, start=1):
        draw.rounded_rectangle((sx - 26, sy - 16, sx + 36, sy + 28), radius=8, fill=POISON, outline=DARK_GREEN, width=3)
        draw.text((sx - 11, sy - 12), f"{i}", fill=DARK_GREEN, font=font(19))
    for i, ((sx, sy), (hx, hy)) in enumerate(zip(sockets, heads), start=1):
        mid = ((sx + hx) // 2, min(sy, hy) - 46 - (i % 3) * 13)
        draw_neck(draw, (sx + 5, sy + 6), mid, (hx, hy), width=36 if i == 5 else 32)
    for hx, hy in heads:
        draw_snake_head(draw, hx, hy, scale=1.0)
    for x in [390, 860]:
        draw.rounded_rectangle((x, 675, x + 70, 758), radius=9, fill=GREEN, outline=DARK_GREEN, width=4)
    for idx, x in enumerate(range(570, 1030, 65)):
        draw.polygon([(x, 454 - idx % 2 * 8), (x + 25, 420 - idx % 2 * 10), (x + 48, 460 - idx % 2 * 8)], fill=POISON, outline=DARK_GREEN)
    draw.rounded_rectangle((510, 708, 965, 748), radius=8, fill=(255, 243, 224, 255), outline=WARNING, width=3)
    draw.text((530, 716), "9 visible sockets; neck_root overlaps shoulder_saddle", fill=WARNING, font=font(23))

    panel(draw, (1480, 140, 2150, 770), "Side view: body is low and long, not bulky")
    draw.line((1530, 715, 2100, 715), fill=(143, 137, 126, 255), width=3)
    draw.polygon([(1545, 552), (1855, 525), (2075, 565), (1840, 635), (1540, 625)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1845, 572), (2130, 588), (1920, 668)], fill=GREEN, outline=DARK_GREEN)
    draw.polygon([(1548, 583), (1465, 622), (1505, 548)], fill=GREEN, outline=DARK_GREEN)
    side_necks = [
        (1612, 544, 1495, 375),
        (1695, 535, 1650, 330),
        (1795, 528, 1970, 365),
    ]
    for sx, sy, hx, hy in side_necks:
        draw_neck(draw, (sx, sy), ((sx + hx) // 2, hy - 46), (hx, hy), width=30)
        draw_snake_head(draw, hx, hy, scale=0.9)
    for x in [1630, 1850]:
        draw.rounded_rectangle((x, 625, x + 54, 713), radius=8, fill=GREEN, outline=DARK_GREEN, width=4)
    draw_dimension(draw, (1545, 676), (2075, 676), "3-4x body height")
    draw_dimension(draw, (1515, 525), (1515, 625), "low body")
    draw.text((1538, 742), "main body height <= 32% of full silhouette", fill=WARNING, font=font(22))

    panel(draw, (50, 820, 1085, 1430), "Front fan")
    draw.polygon([(325, 1180), (730, 1135), (910, 1245), (710, 1350), (330, 1325), (180, 1240)], fill=GREEN, outline=DARK_GREEN)
    front_roots = [
        (300, 1135), (370, 1100), (455, 1075), (555, 1060), (655, 1060),
        (755, 1075), (840, 1100), (910, 1135), (955, 1180),
    ]
    front_heads = [
        (145, 1015), (255, 925), (370, 870), (500, 830), (650, 820),
        (800, 855), (930, 920), (1025, 1010), (1030, 1130),
    ]
    for i, (sx, sy) in enumerate(front_roots, start=1):
        draw.rounded_rectangle((sx - 22, sy - 14, sx + 34, sy + 24), radius=7, fill=POISON, outline=DARK_GREEN, width=3)
        draw.text((sx - 7, sy - 10), f"{i}", fill=DARK_GREEN, font=font(18))
    for i, ((sx, sy), (hx, hy)) in enumerate(zip(front_roots, front_heads), start=1):
        mid = ((sx + hx) // 2, hy - 18 - (i % 2) * 22)
        draw_neck(draw, (sx + 5, sy), mid, (hx, hy), width=29)
    for hx, hy in front_heads:
        draw_snake_head(draw, hx, hy, scale=0.82)
    draw.text((160, 1372), "Each head has its own neck path and visible socket; gaps stay readable.", fill=MUTED, font=font(23))

    panel(draw, (1135, 820, 2150, 1430), "Hard gates before Blockbench")
    checks = [
        "Default head count is exactly nine unless the design package is explicitly revised.",
        "Every neck group has neck_root, neck_mid, neck_tip, head_core, upper_jaw, lower_jaw, eyes, fangs.",
        "neck_root overlaps its named socket and shoulder_saddle; neck_tip overlaps head_core.",
        "Body is long and low; height comes from neck fan, not a bulky torso.",
        "Each head reads as a wedge snake head with eye, mouth line, fang, horn or poison gland.",
        "No head wall: 45 degree and front views keep staggered gaps.",
        "Details come from geometry and UV, not from enlarging heads.",
    ]
    y = 885
    for item in checks:
        draw.text((1180, y), "□", fill=WARNING, font=font(28))
        y = draw_wrapped(draw, item, (1220, y + 3), 855, size=24)
        y += 14
    draw.rounded_rectangle((1180, 1370, 2085, 1424), radius=10, fill=(255, 240, 238, 255), outline=WARNING, width=3)
    draw.text((1200, 1382), "Decision: candidate only. modeling_allowed=false until user approval.", fill=WARNING, font=font(24))

    image.save(path)


def pegasus_contract() -> str:
    return """---
creature_id: pegasus
creature_name: Pegasus / 天马
design_version: v4_candidate
design_status: candidate_needs_user_review
modeling_allowed: false
previous_v2_assets_status: rejected_malformed
previous_v3_status: direction_correct_but_detail_insufficient
candidate_sheet: docs/preview_assets/greek_creatures/design_approval/v4/pegasus_v4_final_candidate_sheet.png
design_gate_required: true
---

# Pegasus / 天马 V4 候选建模契约

## 当前结论

当前天马设计未正确通过。旧 V2/当前模型已经畸形，不能继续在其上微调；V3 只给出方向，不能作为直接建模依据。V4 仍然是候选契约，必须由用户确认后才允许进入 Blockbench。

## 设计目标

先是一匹比例成立的成年白马，再是有双翼、能自主飞行、能载玩家飞行的神话坐骑。识别顺序固定为：白马身体 -> 小而细长的马头 -> 斜向颈部 -> 肩后双翼 -> 骑乘飞行空间。

## 硬比例

- 碰撞盒目标：`1.7w x 2.1h`。
- 视觉高度：`2.15` 到 `2.25` 格。
- 主躯干长度为躯干高度的 `1.9` 到 `2.2` 倍。
- 头部宽度为身体宽度的 `0.35` 到 `0.45`。
- 头部高度为胸腔高度的 `0.45` 到 `0.60`。
- 吻部长度为头部核心长度的 `0.55` 到 `0.75`。
- 翼根位于肩胛后方，不能长在腹部、背中部或臀部。
- 玩家骑乘点位于背中段，不能遮住头颈、翼根或主要羽片。

## 正确造型要求

- 侧面必须先读成成年马，不能是大头方块、白色鸟、白色鹿或玩具马。
- 头部必须小、细长、在胸腔前方；眼、鼻孔、嘴线和耳朵靠部件与 UV 表达，不靠放大头部。
- 颈部从鬐甲/肩胛向前上方斜出，至少 `neck_lower`、`neck_upper` 两段，禁止竖直塔状颈。
- 每侧翅膀至少包含 `shoulder_joint`、`upper_wing`、`secondary_wing`、`coverts`、`primary_feathers_01_06`。
- 纯白不是空白：白色大面必须有浅灰分面、鼻口线、鬃毛线、马蹄和羽片边界。

## Blockbench 层级

- `root/body/body_barrel`
- `root/body/chest`
- `root/body/rump`
- `root/body/withers`
- `root/body/saddle_space`
- `root/head_neck/neck_lower`
- `root/head_neck/neck_upper`
- `root/head_neck/mane_strip`
- `root/head_neck/head_core`
- `root/head_neck/muzzle`
- `root/head_neck/left_eye`
- `root/head_neck/right_eye`
- `root/head_neck/left_nostril`
- `root/head_neck/right_nostril`
- `root/head_neck/mouth_line`
- `root/head_neck/left_ear`
- `root/head_neck/right_ear`
- `root/wings/left_wing/shoulder_joint`
- `root/wings/left_wing/upper_wing`
- `root/wings/left_wing/secondary_wing`
- `root/wings/left_wing/coverts`
- `root/wings/left_wing/primary_feathers_01_06`
- `root/wings/right_wing/shoulder_joint`
- `root/wings/right_wing/upper_wing`
- `root/wings/right_wing/secondary_wing`
- `root/wings/right_wing/coverts`
- `root/wings/right_wing/primary_feathers_01_06`
- `root/legs/front_left/upper_leg/lower_leg/hoof`
- `root/legs/front_right/upper_leg/lower_leg/hoof`
- `root/legs/back_left/upper_leg/lower_leg/hoof`
- `root/legs/back_right/upper_leg/lower_leg/hoof`
- `root/tail/tail_base`
- `root/tail/tail_plume_segments`

## UV 与纹理

- `head_front`: 双眼、鼻梁、鼻孔、嘴线固定在马头面，不允许漂到颈部或翼面。
- `muzzle_side`: 细长吻部侧面，必须帮助侧面读成马脸。
- `neck_side`: 浅灰颈部肌理和鬃毛根，不允许整块白柱。
- `body_side`: 暖白主身体与浅灰胸/臀/腹分面。
- `wing_top`: 翼肩、次翼、覆羽。
- `primary_feathers`: 每侧至少六片主羽，每片有灰色边线。
- `legs_hooves`: 腿部关节和马蹄。
- `tail_mane`: 白到浅灰的鬃尾分段。

## 动画约束

- 自主飞行：翼肩驱动，上翼、次翼、主羽分层拍动，马体保持水平。
- 骑乘飞行：`saddle_space` 稳定，玩家位置不挡头颈和翼根，翼片不穿玩家。
- 待机/行走：头颈小幅呼吸和马步，不允许像人形大头单位。

## 设计否决条件

- 头太大，或靠放大头部换取眼嘴细节。
- 头太小或被翼遮挡，导致看不出头部。
- 颈部竖直堆高。
- 身体短胖，不像马。
- 翅膀只是白条或平板。
- 纯白贴图渲染后像透明/空白面。

## 建模许可

当前为 `candidate_needs_user_review`，`modeling_allowed=false`。用户确认本契约和 V4 图后，才能把状态改为 `approved_for_modeling` 并进入 `.bbmodel`、UV、纹理和 Java runtime 同步。
"""


def hydra_contract() -> str:
    return """---
creature_id: hydra
creature_name: Hydra / 九头蛇
design_version: v4_candidate
design_status: candidate_needs_user_review
modeling_allowed: false
previous_v2_assets_status: rejected_disconnected
previous_v3_status: topology_direction_but_detail_insufficient
candidate_sheet: docs/preview_assets/greek_creatures/design_approval/v4/hydra_v4_final_candidate_sheet.png
design_gate_required: true
---

# Hydra / 九头蛇 V4 候选建模契约

## 当前结论

当前九头蛇设计未正确通过。旧 V2/当前模型存在头颈身体断开、身体臃肿和头部比例错误问题；V3 只说明拓扑方向，仍不足以直接建模。V4 仍然是候选契约，必须由用户确认后才允许进入 Blockbench。

## 设计目标

低伏长身的沼泽蛇怪，默认九头，九条颈从同一肩带/背甲区域生长。压迫感来自长身、长尾、九头扇形错层、毒性鳞片和每个蛇头的面部细节，不来自臃肿身体或巨大方头。

## 硬比例与拓扑

- 碰撞盒目标：`3.2w x 2.8h`。
- 视觉高度：`2.8` 到 `3.1` 格。
- 默认头数：`9`，除非设计包先被改写并重新审批。
- 主身体长度为身体高度的 `3.0` 到 `4.0` 倍。
- 主身体高度不超过整体视觉高度的 `0.32`。
- 每个头宽约为肩带总宽的 `0.12` 到 `0.18`。
- 中央头可略大，但不得超过相邻头的 `1.18` 倍。
- 每条颈必须有 `neck_root`、`neck_mid`、`neck_tip` 三段。
- `neck_root` 必须与对应 `neck_socket` 和 `shoulder_saddle` 几何重叠。
- `neck_tip` 必须与 `head_core` 几何重叠。

## 正确造型要求

- 45 度必须同时看到九头错层、九个 socket、低伏身体、长尾和背脊鳞。
- 正面必须是扇形九头，有间隙，不是一堵头墙。
- 侧面必须是低长身体和长尾，不是胖蜥蜴或大箱体。
- 每个头是楔形蛇头，必须有眼、上颚、下颚、嘴线、獠牙和角/毒腺之一。
- 细节必须来自独立部件和 UV，不允许通过放大头部解决。

## Blockbench 层级

- `root/body/slender_body`
- `root/body/chest_keel`
- `root/body/shoulder_saddle`
- `root/body/neck_socket_01`
- `root/body/neck_socket_02`
- `root/body/neck_socket_03`
- `root/body/neck_socket_04`
- `root/body/neck_socket_05`
- `root/body/neck_socket_06`
- `root/body/neck_socket_07`
- `root/body/neck_socket_08`
- `root/body/neck_socket_09`
- `root/body/spine_crests_01_08`
- `root/body/tail_base`
- `root/body/tail_mid`
- `root/body/tail_tip`
- `root/legs/front_left`
- `root/legs/front_right`
- `root/legs/back_left`
- `root/legs/back_right`
- `root/necks/head_01_center/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_02_inner_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_03_inner_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_04_mid_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_05_mid_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_06_outer_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_07_outer_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_08_rear_left/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`
- `root/necks/head_09_rear_right/neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horns_or_poison_glands`

## UV 与纹理

- `head_front_01_09`: 每个头固定眼、眉、上颚、下颚、嘴线、獠牙位置。
- `jaw_side_01_09`: 蛇头楔形侧面和嘴角。
- `neck_scale_01_09`: 每条颈的鳞片方向必须顺着颈段。
- `neck_socket_band`: 九个颈根 socket 与肩带阴影，用于证明颈从身体长出。
- `body_side`: 深绿低伏身体，不允许无细节深绿大块。
- `spine_crests`: 从肩到尾连续的背脊鳞。
- `tail`: 尾巴逐段变细的鳞片。
- `legs_claws`: 四肢、爪和腹部阴影。

## 动画约束

- 待机：九头错相摆动，身体低伏不膨胀。
- 攻击：中央头和左右内侧头前探，外侧头维持扇形威胁。
- 任何动画状态下，`neck_root` 与 socket、`neck_tip` 与 head_core 不得断开。

## 设计否决条件

- 只有五头却没有先改设计包。
- 头和身体没有实体连接。
- 多头像插在背上的柱子。
- 身体臃肿、正面大箱体化。
- 头被放大到破坏整体比例。
- 头太小且缺少眼、嘴、獠牙等神话特征。
- 45 度视角糊成头墙。

## 建模许可

当前为 `candidate_needs_user_review`，`modeling_allowed=false`。用户确认本契约和 V4 图后，才能把状态改为 `approved_for_modeling` 并进入 `.bbmodel`、UV、纹理和 Java runtime 同步。
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
            "current_design_correct": False,
            "candidate_design_correct": None,
            "contract": project_path(CONTRACTS["pegasus"]),
            "candidate_sheet": project_path(SHEETS["pegasus"]),
            "user_problem_answered": [
                "head_must_not_be_oversized",
                "must_follow_adult_horse_proportion",
                "must_keep_visible_head_muzzle_eyes_nostrils_mouth",
                "must_support_self_flight_and_ridden_flight",
            ],
            "must_approve_before_modeling": [
                "side silhouette reads as adult white horse",
                "head and muzzle ratios are accepted",
                "wing root and rider slot are accepted",
                "pure-white texture plan has fixed face and feather UV zones",
            ],
        },
        {
            "id": "hydra",
            "display": "Hydra / 九头蛇",
            "status": "candidate_needs_user_review",
            "approved_for_modeling": False,
            "modeling_allowed": False,
            "current_design_correct": False,
            "candidate_design_correct": None,
            "contract": project_path(CONTRACTS["hydra"]),
            "candidate_sheet": project_path(SHEETS["hydra"]),
            "user_problem_answered": [
                "head_neck_body_connections_are_required",
                "default_head_count_is_nine",
                "body_must_be_low_and_long",
                "head_details_must_not_depend_on_oversized_heads",
            ],
            "must_approve_before_modeling": [
                "nine sockets and nine neck roots are accepted",
                "each neck root overlaps shoulder saddle",
                "each neck tip overlaps head core",
                "front and 45 degree views keep gaps instead of a head wall",
            ],
        },
    ]
    return {
        "version": 1,
        "scope": "pegasus_hydra_v4_design_candidate_package",
        "ok": False,
        "status": "candidate_needs_user_review",
        "verdict": "not_approved_for_modeling",
        "rule": (
            "This package is a design candidate only. It strengthens the design-first gate, "
            "but modeling remains blocked until the user approves the V4 contracts and sheets."
        ),
        "blocked_count": len(creatures),
        "blocked": [item["id"] for item in creatures],
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# Pegasus / Hydra V4 候选设计包（2026-06-15）",
        "",
        "本文档回答当前问题：天马和九头蛇的现有设计不正确，不能继续在畸形模型上调参。V4 是建模前候选契约，不是批准结果。",
        "",
        f"- 当前状态：`{payload['status']}`",
        f"- 建模允许：`{payload['ok']}`",
        f"- 结论：`{payload['verdict']}`",
        "",
        "## 审批材料",
        "",
        "| 生物 | V4 候选契约 | V4 候选设计图 | 当前结论 |",
        "| --- | --- | --- | --- |",
    ]
    for item in payload["creatures"]:
        lines.append(
            "| {display} | `{contract}` | `{sheet}` | `{status}` |".format(
                display=item["display"],
                contract=item["contract"],
                sheet=item["candidate_sheet"],
                status=item["status"],
            )
        )
    lines.extend(["", "## 设计图预览", ""])
    for item in payload["creatures"]:
        lines.extend([
            f"### {item['display']}",
            "",
            f"![{item['id']} V4 candidate sheet](../../preview_assets/greek_creatures/design_approval/v4/{item['id']}_v4_final_candidate_sheet.png)",
            "",
            "该候选图必须由用户确认后才允许进入建模。它当前只证明“下一轮设计应如何约束”，不证明最终模型已经正确。",
            "",
            "必须先确认：",
            "",
        ])
        for check in item["must_approve_before_modeling"]:
            lines.append(f"- [ ] {check}")
        lines.extend(["", "已针对用户反馈新增的硬约束：", ""])
        for answer in item["user_problem_answered"]:
            lines.append(f"- `{answer}`")
        lines.append("")
    lines.extend([
        "## 建模门禁",
        "",
        "- 当前 V2 模型与贴图不得继续作为修复依据。",
        "- V3 只作为方向参考，不作为直接建模依据。",
        "- V4 契约和设计图经用户确认后，才能把对应蓝图改为 `approved_for_modeling` 与 `modeling_allowed=true`。",
        "- 未批准前，不允许生成新的最终 `.bbmodel`、最终纹理、动作或 Java runtime 同步。",
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
        print("Pegasus/Hydra V4 Design Candidate Package: OPEN")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"status: {payload['status']}")
        print(f"blocked: {payload['blocked_count']} {payload['blocked']}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
