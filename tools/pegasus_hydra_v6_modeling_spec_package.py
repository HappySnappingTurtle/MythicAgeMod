#!/usr/bin/env python3
"""Build V6 pre-modeling specs for Pegasus and Hydra.

V5 fixed the design direction, but it was still too much like an approval
sketch. V6 is stricter: it translates the user feedback into Blockbench-scale
proportion boxes, required pivots/groups, UV zones, and runtime rebuild gates.
It remains locked until human approval is recorded.
"""

from __future__ import annotations

import argparse
import json
import textwrap
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/greek_creatures/design_approval/v6"
REPORT_PATH = OUT_DIR / "pegasus_hydra_v6_modeling_spec_package.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v6_modeling_spec_package_2026-06-15.md"

CONTRACTS = {
    "pegasus": ROOT / "docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md",
    "hydra": ROOT / "docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md",
}
SHEETS = {
    "pegasus": OUT_DIR / "pegasus_v6_modeling_spec_sheet.png",
    "hydra": OUT_DIR / "hydra_v6_modeling_spec_sheet.png",
}

BG = (241, 238, 230, 255)
PANEL = (255, 253, 247, 255)
INK = (31, 35, 40, 255)
MUTED = (91, 99, 110, 255)
WARN = (154, 70, 48, 255)
LINE = (126, 136, 145, 255)
GRID = (210, 205, 195, 255)
WHITE = (248, 248, 242, 255)
WARM_WHITE = (232, 236, 232, 255)
GREY = (174, 184, 190, 255)
DARK = (28, 32, 36, 255)
GREEN = (42, 91, 48, 255)
DARK_GREEN = (14, 34, 20, 255)
POISON = (151, 198, 83, 255)
MOUTH = (61, 30, 38, 255)
FANG = (244, 238, 219, 255)

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


def panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str) -> None:
    draw.rounded_rectangle(box, radius=8, fill=PANEL, outline=LINE, width=3)
    draw.text((box[0] + 22, box[1] + 16), title, fill=INK, font=font(30))


def wrap_lines(text: str, width: int, size: int) -> list[str]:
    chars = max(14, width // max(7, int(size * 0.52)))
    return textwrap.wrap(text, width=chars)


def wrapped(draw: ImageDraw.ImageDraw, text: str, xy: tuple[int, int], width: int, *, size: int = 22, fill=INK) -> int:
    x, y = xy
    for line in wrap_lines(text, width, size):
        draw.text((x, y), line, fill=fill, font=font(size))
        y += size + 7
    return y


def grid(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], step: int = 40) -> None:
    x1, y1, x2, y2 = box
    for x in range(x1, x2 + 1, step):
        draw.line((x, y1, x, y2), fill=GRID, width=1)
    for y in range(y1, y2 + 1, step):
        draw.line((x1, y, x2, y), fill=GRID, width=1)


def label(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, *, fill=WARN, size: int = 19) -> None:
    x, y = xy
    pad = 5
    bbox = draw.textbbox((x, y), text, font=font(size))
    draw.rounded_rectangle(
        (bbox[0] - pad, bbox[1] - pad, bbox[2] + pad, bbox[3] + pad),
        radius=4,
        fill=(255, 245, 236, 255),
        outline=fill,
        width=2,
    )
    draw.text((x, y), text, fill=fill, font=font(size))


def draw_horse_spec(draw: ImageDraw.ImageDraw, ox: int, oy: int, scale: float = 1.0) -> None:
    s = scale

    def p(x: float, y: float) -> tuple[int, int]:
        return (ox + int(x * s), oy + int(y * s))

    # Adult horse first: long barrel, separate chest/rump, small head, sloped neck.
    draw.polygon([p(180, 285), p(310, 245), p(590, 250), p(700, 300), p(650, 390), p(220, 390)], fill=WHITE, outline=INK, width=3)
    draw.polygon([p(200, 310), p(320, 270), p(315, 390), p(205, 390)], fill=WARM_WHITE, outline=GREY, width=2)
    draw.polygon([p(570, 260), p(700, 300), p(650, 390), p(560, 378)], fill=WARM_WHITE, outline=GREY, width=2)
    draw.line((*p(260, 333), *p(620, 340)), fill=GREY, width=4)

    draw.polygon([p(238, 282), p(135, 150), p(178, 122), p(306, 260)], fill=WHITE, outline=INK, width=3)
    draw.polygon([p(138, 148), p(36, 142), p(-34, 178), p(40, 212), p(150, 202)], fill=WHITE, outline=INK, width=3)
    draw.polygon([p(-34, 178), p(-112, 186), p(-112, 218), p(40, 212)], fill=WHITE, outline=INK, width=3)
    draw.polygon([p(65, 136), p(80, 78), p(100, 143)], fill=WHITE, outline=INK, width=2)
    draw.polygon([p(115, 137), p(145, 87), p(150, 152)], fill=WHITE, outline=INK, width=2)
    draw.ellipse((*p(68, 174), *p(83, 190)), fill=DARK)
    draw.rectangle((*p(-92, 198), *p(-75, 207)), fill=DARK)
    draw.line((*p(-28, 218), *p(42, 222)), fill=(82, 90, 96, 255), width=4)
    for i in range(6):
        draw.polygon([p(160 + i * 20, 122 + i * 21), p(183 + i * 19, 144 + i * 19), p(165 + i * 18, 160 + i * 21)], fill=GREY, outline=(112, 122, 130, 255))

    # Wings start behind shoulder, not on neck/head.
    draw.ellipse((*p(322, 254), *p(390, 322)), fill=GREY, outline=INK, width=3)
    draw.polygon([p(365, 255), p(580, 78), p(845, 150), p(630, 295)], fill=WHITE, outline=INK, width=3)
    for i in range(7):
        draw.polygon(
            [p(570 - i * 24, 280 + i * 13), p(835 - i * 22, 156 + i * 13), p(890 - i * 22, 188 + i * 13), p(610 - i * 23, 322 + i * 13)],
            fill=(229, 235, 235, 255),
            outline=(102, 113, 120, 255),
        )
    label(draw, p(385, 260), "wing_root pivot: shoulder+2z")
    label(draw, p(348, 230), "saddle_space clear")

    for x, knee in [(235, 475), (340, 458), (560, 460), (650, 480)]:
        draw.polygon([p(x, 385), p(x + 48, 385), p(x + 40, knee), p(x + 14, knee)], fill=WHITE, outline=INK, width=2)
        draw.polygon([p(x + 13, knee), p(x + 42, knee), p(x + 38, 600), p(x + 4, 600)], fill=WHITE, outline=INK, width=2)
        draw.polygon([p(x - 7, 600), p(x + 50, 600), p(x + 65, 630), p(x - 20, 630)], fill=GREY, outline=(105, 115, 122, 255), width=2)
    draw.polygon([p(675, 330), p(825, 365), p(785, 475), p(650, 390)], fill=GREY, outline=(105, 115, 122, 255), width=2)


def draw_hydra_spec(draw: ImageDraw.ImageDraw, ox: int, oy: int, scale: float = 1.0) -> None:
    s = scale

    def p(x: float, y: float) -> tuple[int, int]:
        return (ox + int(x * s), oy + int(y * s))

    draw.polygon([p(220, 445), p(610, 388), p(1015, 435), p(1165, 535), p(690, 620), p(165, 565)], fill=GREEN, outline=DARK_GREEN, width=4)
    draw.polygon([p(220, 505), p(610, 465), p(1010, 505), p(700, 585), p(175, 555)], fill=(49, 101, 50, 255), outline=DARK_GREEN, width=3)
    draw.polygon([p(205, 505), p(45, 598), p(15, 555), p(190, 455)], fill=GREEN, outline=DARK_GREEN, width=3)
    draw.polygon([p(1120, 515), p(1370, 540), p(1188, 620)], fill=GREEN, outline=DARK_GREEN, width=3)
    draw.polygon([p(465, 395), p(620, 352), p(785, 392), p(760, 472), p(490, 470)], fill=(44, 94, 47, 255), outline=DARK_GREEN, width=3)

    sockets = [(345, 435), (425, 414), (515, 396), (612, 386), (715, 386), (815, 400), (905, 425), (988, 470), (1055, 535)]
    heads = [(170, 305), (285, 245), (420, 210), (565, 185), (710, 185), (860, 210), (990, 260), (1090, 350), (1120, 465)]
    for i, (sx, sy) in enumerate(sockets, start=1):
        draw.rounded_rectangle((*p(sx - 30, sy - 18), *p(sx + 36, sy + 28)), radius=7, fill=POISON, outline=DARK_GREEN, width=3)
        draw.text(p(sx - 8, sy - 11), f"{i}", fill=DARK_GREEN, font=font(max(14, int(18 * s))))
    for i, ((sx, sy), (hx, hy)) in enumerate(zip(sockets, heads), start=1):
        mid = ((sx + hx) // 2, min(sy, hy) - 48 - (i % 3) * 18)
        for width, color in [(31, DARK_GREEN), (20, (42, 108, 55, 255))]:
            draw.line((*p(sx + 3, sy + 7), *p(mid[0], mid[1]), *p(hx - 22, hy + 18)), fill=color, width=max(4, int(width * s)))
        for px, py in [(sx + 3, sy + 7), mid, (hx - 22, hy + 18)]:
            draw.ellipse((*p(px - 10, py - 10), *p(px + 10, py + 10)), fill=POISON, outline=DARK_GREEN, width=2)
    for i, (hx, hy) in enumerate(heads, start=1):
        snake_head(draw, p(hx, hy), scale=s * 0.72)
        draw.text(p(hx - 22, hy - 70), f"H{i}", fill=DARK_GREEN, font=font(max(14, int(18 * s))))
    for x in range(500, 1010, 70):
        draw.polygon([p(x, 390), p(x + 28, 344), p(x + 56, 398)], fill=POISON, outline=DARK_GREEN)
    label(draw, p(410, 596), "low body, long tail; no bulky torso")
    label(draw, p(392, 465), "9 named sockets on shoulder_saddle")


def snake_head(draw: ImageDraw.ImageDraw, center: tuple[int, int], *, scale: float = 1.0) -> None:
    hx, hy = center
    s = scale
    pts = [
        (hx - int(48 * s), hy - int(25 * s)),
        (hx + int(24 * s), hy - int(35 * s)),
        (hx + int(82 * s), hy),
        (hx + int(24 * s), hy + int(36 * s)),
        (hx - int(48 * s), hy + int(25 * s)),
    ]
    draw.polygon(pts, fill=(68, 126, 58, 255), outline=DARK_GREEN)
    draw.ellipse((hx + int(12 * s) - 6, hy - int(10 * s), hx + int(12 * s) + 6, hy + int(3 * s)), fill=(232, 240, 98, 255), outline=DARK_GREEN)
    draw.polygon([(hx - int(14 * s), hy + int(27 * s)), (hx + int(32 * s), hy + int(39 * s)), (hx + int(4 * s), hy + int(62 * s))], fill=MOUTH, outline=DARK_GREEN)
    draw.polygon([(hx + int(27 * s), hy + int(26 * s)), (hx + int(47 * s), hy + int(31 * s)), (hx + int(36 * s), hy + int(56 * s))], fill=FANG, outline=DARK_GREEN)
    draw.polygon([(hx - int(8 * s), hy - int(38 * s)), (hx + int(8 * s), hy - int(70 * s)), (hx + int(25 * s), hy - int(37 * s))], fill=POISON, outline=DARK_GREEN)


def draw_spec_table(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], rows: list[tuple[str, str]], *, size: int = 20) -> None:
    x1, y1, x2, _ = box
    y = y1
    for key, value in rows:
        draw.text((x1, y), key, fill=INK, font=font(size))
        y = wrapped(draw, value, (x1 + 260, y), x2 - x1 - 270, size=size, fill=MUTED)
        y += 8


def draw_pegasus_sheet(path: Path) -> None:
    image = Image.new("RGBA", (2400, 1600), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), "Pegasus V6 modeling spec: adult horse proportions locked", fill=INK, font=font(42))
    draw.text((54, 88), "Still locked. This sheet is precise enough to review before Blockbench, not permission to model.", fill=MUTED, font=font(24))

    panel(draw, (48, 140, 1508, 890), "Blockbench side spec: horse first, winged second")
    grid(draw, (90, 215, 1465, 835), 40)
    draw.line((95, 815, 1465, 815), fill=(145, 137, 124, 255), width=3)
    draw_horse_spec(draw, 300, 170, 0.95)
    label(draw, (170, 270), "head width <= 0.42 body width")
    label(draw, (178, 730), "body length 21-23 BB units")
    label(draw, (520, 338), "neck diagonal, never vertical")

    panel(draw, (1545, 140, 2320, 890), "Build dimensions and pivots")
    draw_spec_table(draw, (1585, 215, 2285, 835), [
        ("body_barrel", "10.5w x 7.2h x 22.0l, pivot (0, 13, 2); long horse barrel is the anchor."),
        ("head_core", "3.2w x 3.8h x 4.4l; smaller than current, with separate muzzle 2.0w x 2.0h x 6.0l."),
        ("neck", "two sloped segments from withers to head; no single vertical column."),
        ("wings", "root pivots behind shoulders at body z -3 to -1; upper, secondary, coverts, six primary feathers per side."),
        ("saddle_space", "visible but understated; does not cover wing root or neck."),
        ("texture", "white coat uses grey planes, fixed eye/nostril/mouth/mane/hoof/feather UV zones."),
    ])

    panel(draw, (48, 930, 1110, 1530), "Front rejection checks")
    draw.rounded_rectangle((495, 1140, 655, 1275), radius=10, fill=WHITE, outline=INK, width=5)
    draw.polygon([(530, 1140), (555, 1000), (600, 1000), (628, 1140)], fill=WHITE, outline=INK, width=3)
    draw.polygon([(548, 1000), (565, 940), (586, 1002)], fill=WHITE, outline=INK, width=2)
    draw.polygon([(600, 1000), (630, 945), (638, 1010)], fill=WHITE, outline=INK, width=2)
    draw.ellipse((558, 1070, 574, 1086), fill=DARK)
    draw.ellipse((602, 1070, 618, 1086), fill=DARK)
    draw.polygon([(495, 1192), (250, 1228), (300, 1315), (495, 1260)], fill=WHITE, outline=INK, width=3)
    draw.polygon([(655, 1192), (900, 1228), (850, 1315), (655, 1260)], fill=WHITE, outline=INK, width=3)
    for text, y in [
        ("Reject if face is hidden by wings.", 1365),
        ("Reject if head is oversized to show eyes.", 1405),
        ("Reject if rider slot overlaps wing roots.", 1445),
    ]:
        draw.text((110, y), text, fill=WARN, font=font(24))

    panel(draw, (1145, 930, 2320, 1530), "Approval before modeling")
    checks = [
        "Side silhouette reads as an adult Minecraft horse derivative before reading as a winged unit.",
        "Head is smaller than current implementation; facial detail comes from muzzle, eye, nostril, mouth UV/parts.",
        "Neck has lower/upper sloped segments and visible withers connection.",
        "Wing roots are behind shoulders; six primary feathers remain readable in front, side, and 45-degree views.",
        "Flight and ridden-flight animations must preserve saddle_space and never clip through head/neck.",
    ]
    y = 1005
    for item in checks:
        draw.text((1195, y), "□", fill=WARN, font=font(28))
        y = wrapped(draw, item, (1238, y + 2), 990, size=24)
        y += 18
    image.save(path)


def draw_hydra_sheet(path: Path) -> None:
    image = Image.new("RGBA", (2400, 1600), BG)
    draw = ImageDraw.Draw(image)
    draw.text((54, 36), "Hydra V6 modeling spec: nine connected heads locked", fill=INK, font=font(42))
    draw.text((54, 88), "Still locked. Current five-head Blockbench/Java models are rejection evidence only.", fill=MUTED, font=font(24))

    panel(draw, (48, 140, 1560, 910), "Blockbench topology spec: every head grows from the body")
    grid(draw, (90, 215, 1515, 855), 40)
    draw.line((95, 835, 1515, 835), fill=(145, 137, 124, 255), width=3)
    draw_hydra_spec(draw, 105, 185, 0.96)

    panel(draw, (1590, 140, 2320, 910), "Build dimensions and topology")
    draw_spec_table(draw, (1628, 215, 2288, 855), [
        ("required heads", "exactly 9 visible heads unless the design package is revised before modeling."),
        ("slender_body", "8w x 4h x 36l; visual body height <= 32% of full silhouette."),
        ("shoulder_saddle", "wide low saddle with neck_socket_01..09; every neck_root overlaps its socket."),
        ("neck path", "each head group has neck_root -> neck_mid -> neck_tip -> head_core; root and tip overlaps are geometry, not comments."),
        ("head detail", "each head has upper/lower jaw, eye, mouth line, fangs, horn or poison gland; no detail by oversized heads."),
        ("runtime", "Java arrays must become 9 neck/head entries and animation must preserve root/socket and tip/head overlaps."),
    ])

    panel(draw, (48, 950, 1130, 1530), "Head connection rejection checks")
    draw.polygon([(300, 1330), (760, 1270), (950, 1380), (735, 1485), (330, 1460)], fill=GREEN, outline=DARK_GREEN, width=4)
    roots = [(310, 1292), (390, 1265), (485, 1245), (585, 1235), (685, 1235), (785, 1248), (880, 1275), (960, 1320), (995, 1380)]
    heads = [(160, 1215), (275, 1130), (410, 1080), (555, 1040), (700, 1038), (845, 1080), (980, 1145), (1050, 1240), (1055, 1350)]
    for i, ((sx, sy), (hx, hy)) in enumerate(zip(roots, heads), start=1):
        draw.rounded_rectangle((sx - 22, sy - 12, sx + 26, sy + 20), radius=5, fill=POISON, outline=DARK_GREEN, width=2)
        draw.line((sx, sy, (sx + hx) // 2, hy - 25, hx - 18, hy + 14), fill=DARK_GREEN, width=16)
        draw.line((sx, sy, (sx + hx) // 2, hy - 25, hx - 18, hy + 14), fill=(46, 110, 56, 255), width=9)
        snake_head(draw, (hx, hy), scale=0.62)
    draw.rounded_rectangle((82, 1390, 660, 1515), radius=8, fill=(255, 253, 247, 235), outline=WARN, width=2)
    for text, y in [
        ("Reject if any head floats above the saddle.", 1405),
        ("Reject if 9 heads collapse into one wall.", 1440),
        ("Reject if body bulk hides neck roots.", 1475),
    ]:
        draw.text((105, y), text, fill=WARN, font=font(23))

    panel(draw, (1160, 950, 2320, 1530), "Approval before modeling")
    checks = [
        "Blockbench group list contains body/slender_body/shoulder_saddle/neck_socket_01..09.",
        "Each head_01..09 contains neck_root, neck_mid, neck_tip, head_core, jaws, eyes, fangs, and horn or poison gland.",
        "Side view remains low and long; torso is not allowed to become the main visual mass.",
        "Front and 45-degree views show gaps between heads and nine distinct neck roots.",
        "Java runtime rebuild uses nine arrays and rejects the current five-head runtime model.",
    ]
    y = 1025
    for item in checks:
        draw.text((1210, y), "□", fill=WARN, font=font(28))
        y = wrapped(draw, item, (1255, y + 2), 980, size=24)
        y += 18
    image.save(path)


def pegasus_contract() -> str:
    return """---
creature_id: pegasus
creature_name: Pegasus / 天马
design_version: v6_modeling_spec
design_status: modeling_spec_ready_for_user_review
modeling_allowed: false
previous_assets_status: rejected_malformed
approval_sheet: docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png
supersedes: v5_approval_candidate
---

# Pegasus / 天马 V6 可建模规格合同

## 结论

当前天马 `.bbmodel`、贴图预览和 Java runtime 都不能继续作为修复基础。V6 不是建模许可，而是建模前必须签收的规格：先成立成年白马，再成立纯白飞行坐骑。

## Blockbench 尺寸锁

- `body_barrel`: 约 `10.5w x 7.2h x 22.0l`，视觉锚点是成年马横向躯干。
- `head_core`: 约 `3.2w x 3.8h x 4.4l`，头宽不超过身体宽度 `0.42`。
- `muzzle`: 约 `2.0w x 2.0h x 6.0l`，吻部必须前伸，不能用大方头代替马脸。
- `neck_lower` / `neck_upper`: 两段斜向连接鬐甲和头部，禁止单根竖直颈柱。
- `wing_root`: 位于肩后，不能盖住头颈或骑乘点。
- `primary_feathers_01_06`: 每侧至少 6 片可读主羽。

## 必须存在的 group

- `root/body/body_barrel/chest/rump/withers/saddle_space`
- `root/head_neck/neck_lower/neck_upper/head_core/muzzle/eyes/nostrils/mouth_line/ears/mane_strip`
- `root/wings/left_wing/shoulder_joint/upper_wing/secondary_wing/coverts/primary_feathers_01_06`
- `root/wings/right_wing/shoulder_joint/upper_wing/secondary_wing/coverts/primary_feathers_01_06`
- `root/legs/front_left/front_right/back_left/back_right/hooves`
- `root/tail/tail_base/tail_plume_segments`

## UV 与纹理锁

- 纯白身体不得是空白面；`body_side` 必须有浅灰分面。
- `head_front` 必须固定眼、鼻孔、鼻梁、嘴线 UV。
- `mane_strip`、`hoof_band`、`feather_edges` 必须独立分区。
- 禁止随机白灰面板作为最终贴图。

## Runtime 重建要求

- 当前 `PegasusModel.java` 不能作为比例源。
- 新 runtime 必须从 V6 `.bbmodel` 同步；保留自主飞行和骑乘飞行，但重新校验 `saddle_space` 与 `wing_root` 不互相遮挡。

## 否决条件

- 头部过大、头看不见、头颈像竖直白柱。
- 侧面不能先读成成年白马。
- 翼面像水平白板或遮挡马头。
- 骑乘位压住翼根。
"""


def hydra_contract() -> str:
    return """---
creature_id: hydra
creature_name: Hydra / 九头蛇
design_version: v6_modeling_spec
design_status: modeling_spec_ready_for_user_review
modeling_allowed: false
previous_assets_status: rejected_wrong_head_count_and_disconnected
approval_sheet: docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png
supersedes: v5_approval_candidate
---

# Hydra / 九头蛇 V6 可建模规格合同

## 结论

当前九头蛇 `.bbmodel` 和 Java runtime 都是五头实现，不满足九头蛇设计。V6 固定为九头、九个 socket、九条连续颈路径；任何减少头数都必须先改设计合同。

## Blockbench 拓扑锁

- 默认头数：`9`。
- `slender_body`: 约 `8w x 4h x 36l`，低伏长身，身体高度不超过整体视觉高度 `0.32`。
- `shoulder_saddle`: 必须承载 `neck_socket_01` 到 `neck_socket_09`。
- 每个 `neck_root` 必须与对应 `neck_socket` 几何重叠。
- 每条颈必须有 `neck_root -> neck_mid -> neck_tip -> head_core`。
- 每个 `neck_tip` 必须与 `head_core` 几何重叠。

## 必须存在的 group

- `root/body/slender_body/chest_keel/shoulder_saddle/neck_socket_01_09/spine_crests_01_08/tail_base/tail_mid/tail_tip`
- `root/necks/head_01_front_left` 到 `root/necks/head_09_rear_right`
- 每个 head group 必须含 `neck_root/neck_mid/neck_tip/head_core/upper_jaw/lower_jaw/left_eye/right_eye/left_fang/right_fang/horn_or_poison_gland`

## UV 与纹理锁

- `head_front_01` 到 `head_front_09` 独立分区，眼/嘴/牙不能靠随机纹理暗示。
- `neck_scale_01` 到 `neck_scale_09` 必须带方向性鳞片。
- `neck_socket_band` 必须在肩带处形成阴影，证明脖子从身体长出。

## Runtime 重建要求

- 当前 `HydraModel.java` 的 5 个 `HEAD_NAMES` / `NECK_NAMES` 必须作废。
- 新 runtime 必须有 9 个 neck/head 数组项，动画保持 root/socket 与 tip/head overlap。

## 否决条件

- 少于九头且没有先改设计包。
- 任一头漂浮、任一颈根不接 shoulder_saddle。
- 头墙化、身体臃肿、头靠放大获得细节。
- 单头缺眼、嘴、颌、牙、角/毒腺。
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
            "status": "modeling_spec_ready_for_user_review",
            "approved_for_modeling": False,
            "modeling_allowed": False,
            "contract": project_path(CONTRACTS["pegasus"]),
            "candidate_sheet": project_path(SHEETS["pegasus"]),
            "supersedes": "v5_approval_candidate",
            "design_decision_required": "approve_spec_or_request_redraw_before_modeling",
            "candidate_design_verdict": "modeling_spec_concrete_but_unapproved",
            "user_problem_answered": [
                "head_must_be_smaller_than_current_and_horse_like",
                "must_reference_adult_minecraft_horse_proportions_before_wings",
                "flight_and_ridden_flight_space_locked_before_modeling",
                "details_must_come_from_geometry_and_uv_not_head_scale",
            ],
            "must_approve_before_modeling": [
                "adult horse side silhouette and body length are accepted",
                "head_core and muzzle dimensions are accepted as small horse head",
                "two-segment sloped neck and visible withers connection are accepted",
                "wing roots, saddle_space, and six feather layers are accepted",
                "white UV zones include eyes, nostrils, mouth, mane, hooves, and feather edges",
            ],
        },
        {
            "id": "hydra",
            "display": "Hydra / 九头蛇",
            "status": "modeling_spec_ready_for_user_review",
            "approved_for_modeling": False,
            "modeling_allowed": False,
            "contract": project_path(CONTRACTS["hydra"]),
            "candidate_sheet": project_path(SHEETS["hydra"]),
            "supersedes": "v5_approval_candidate",
            "design_decision_required": "approve_spec_or_request_redraw_before_modeling",
            "candidate_design_verdict": "modeling_spec_concrete_but_unapproved",
            "user_problem_answered": [
                "current_five_head_blockbench_and_java_models_are_rejected",
                "nine_heads_nine_sockets_nine_connected_neck_paths_locked",
                "each_head_must_connect_to_body_via_socket_and_overlap_geometry",
                "low_long_body_and_head_detail_locked_without_oversized_heads",
            ],
            "must_approve_before_modeling": [
                "exactly nine heads and nine sockets are accepted",
                "each neck path has root, mid, tip, and head_core overlap",
                "low long body and shoulder_saddle socket band are accepted",
                "each head has jaw, eye, mouth, fang, and horn or poison gland",
                "Java runtime rebuild from five heads to nine heads is required",
            ],
        },
    ]
    return {
        "version": 1,
        "scope": "pegasus_hydra_v6_modeling_spec_package",
        "ok": False,
        "status": "modeling_spec_ready_for_user_review",
        "verdict": "not_approved_for_modeling",
        "modeling_allowed": False,
        "rule": (
            "V6 is a concrete pre-modeling specification, not production approval. "
            "Modeling remains blocked until these specs are explicitly approved and design front matter is updated."
        ),
        "blocked_count": len(creatures),
        "blocked": [item["id"] for item in creatures],
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# Pegasus / Hydra V6 可建模规格包（2026-06-15）",
        "",
        "V6 把用户指出的天马头大、九头蛇断颈和大头化问题转成建模前硬规格。它不是建模放行结果；只有用户确认并写回 `approved_for_modeling` 后，才能开始 `.bbmodel`、UV、贴图和 Java runtime 重建。",
        "",
        f"- 状态：`{payload['status']}`",
        f"- 结论：`{payload['verdict']}`",
        f"- 建模允许：`{payload['ok']}`",
        "",
        "## 审批材料",
        "",
        "| 生物 | V6 合同 | V6 规格图 | 当前结论 |",
        "| --- | --- | --- | --- |",
    ]
    for item in payload["creatures"]:
        lines.append(f"| {item['display']} | `{item['contract']}` | `{item['candidate_sheet']}` | `{item['candidate_design_verdict']}` |")
    lines.extend(["", "## 规格图", ""])
    for item in payload["creatures"]:
        lines.extend([
            f"### {item['display']}",
            "",
            f"![{item['id']} V6 modeling spec](../../preview_assets/greek_creatures/design_approval/v6/{item['id']}_v6_modeling_spec_sheet.png)",
            "",
            "必须先确认：",
            "",
        ])
        for check in item["must_approve_before_modeling"]:
            lines.append(f"- [ ] {check}")
        lines.extend(["", "本轮已锁定：", ""])
        for answer in item["user_problem_answered"]:
            lines.append(f"- `{answer}`")
        lines.append("")
    lines.extend([
        "## 禁止事项",
        "",
        "- 不允许基于当前 V2 `.bbmodel` 或当前 Java runtime 微调天马/九头蛇。",
        "- 不允许用放大头部换取眼睛、嘴、獠牙、吻部、角或毒腺细节。",
        "- 不允许把 V6 规格图当成自动建模许可；它必须先被用户签收。",
        "- 不允许测试阶段重新发明造型；测试只验证已批准设计是否落地。",
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
        print("Pegasus/Hydra V6 Modeling Spec Package: OPEN")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"status: {payload['status']}")
        print(f"blocked: {payload['blocked_count']} {payload['blocked']}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
