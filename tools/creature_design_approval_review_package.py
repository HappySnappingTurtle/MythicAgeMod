#!/usr/bin/env python3
"""Build a single review entry package for all 1.0 creature designs.

This is an approval aid, not an approval tool. It gathers the current design
docs, concept images, visual review boards, production lock state, and B0 V4
candidates into one package so review can happen before any final modeling.
"""

from __future__ import annotations

import argparse
import json
import textwrap
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
DESIGN_FIRST_GATE = ROOT / "docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json"
PRODUCTION_LOCK = ROOT / "docs/preview_assets/creature_design_production_lock/creature_design_production_lock.json"
ART_ROADMAP = ROOT / "docs/preview_assets/creature_art_production_roadmap/creature_art_production_roadmap.json"
OUT_DIR = ROOT / "docs/preview_assets/creature_design_approval_review"
REPORT_PATH = OUT_DIR / "creature_design_approval_review_package.json"
CONTACT_SHEET_PATH = OUT_DIR / "creature_design_approval_contact_sheet.png"
MARKDOWN_PATH = ROOT / "docs/项目管理/MythicAgeMod_1.0生物设计审批总包_2026-06-15.md"

CIVILIZATION_ORDER = ["greek", "egyptian", "nordic", "chinese", "atlantean"]
BATCH_ORDER = [
    "B0_DESIGN_BLOCKER_REDRAW",
    "B1_GREEK_VERTICAL_SLICE_REVIEW",
    "B2_NON_GREEK_DESIGN_APPROVAL",
    "B3_NON_GREEK_BLOCKBENCH_UV_TEXTURE",
    "B4_RUNTIME_SYNC_AND_MOTION_UNLOCK",
]

REVIEW_QUESTIONS = [
    "The silhouette is identifiable without reading the label.",
    "Proportion matches the creature role; details are not achieved by oversized heads.",
    "Eyes, mouth, weapon/wing/tail/multiple-head features have explicit geometry or UV zones.",
    "The design can be built in Blockbench without inventing new major parts.",
    "Texture plan avoids blank-looking faces and random panel noise.",
    "Animation constraints preserve the silhouette and key connections.",
]

BG = (241, 238, 230, 255)
PANEL = (255, 253, 247, 255)
INK = (33, 38, 45, 255)
MUTED = (91, 99, 110, 255)
RED = (154, 63, 55, 255)
AMBER = (156, 103, 43, 255)
GREEN = (51, 112, 76, 255)
LINE = (132, 142, 150, 255)

FONT_PATHS = [
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/STHeiti Light.ttc",
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def font(size: int) -> ImageFont.ImageFont:
    for path in FONT_PATHS:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size=size)
            except OSError:
                pass
    return ImageFont.load_default()


def key_for(civilization: str | None, creature_id: str | None) -> str:
    return f"{civilization}:{creature_id}"


def by_key(items: list[dict[str, Any]], key_field: str | None = None) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for item in items:
        if not isinstance(item, dict):
            continue
        key = item.get(key_field) if key_field else key_for(item.get("civilization"), item.get("id"))
        if isinstance(key, str):
            result[key] = item
    return result


def derived_concept_path(civilization: str, creature_id: str) -> str:
    return f"docs/美术资源/{civilization}_creatures/concepts/{creature_id}_concept.png"


def derived_visual_review_path(civilization: str, creature_id: str) -> str:
    return f"docs/preview_assets/creature_visual_acceptance/visual_review_boards/{civilization}/{creature_id}_visual_review.png"


def batch_sort_key(item: dict[str, Any]) -> tuple[int, int, str]:
    batch = str(item.get("batch") or "")
    civ = str(item.get("civilization") or "")
    batch_index = BATCH_ORDER.index(batch) if batch in BATCH_ORDER else 99
    civ_index = CIVILIZATION_ORDER.index(civ) if civ in CIVILIZATION_ORDER else 99
    # Put Pegasus before Hydra inside B0 because Pegasus is the first user-called-out case this turn sequence.
    creature_order = {"pegasus": "00", "hydra": "01"}.get(str(item.get("id")), str(item.get("id")))
    return (batch_index, civ_index, creature_order)


def latest_candidate_for(lock: dict[str, Any]) -> dict[str, Any]:
    if not isinstance(lock, dict):
        return {}
    latest = lock.get("latest_candidate", {})
    if isinstance(latest, dict) and latest:
        return latest
    legacy = lock.get("v4_candidate", {})
    return legacy if isinstance(legacy, dict) else {}


def review_questions_for(lock: dict[str, Any]) -> list[str]:
    latest = latest_candidate_for(lock)
    questions = latest.get("must_approve_before_modeling", []) if isinstance(latest, dict) else []
    if questions:
        return [str(item) for item in questions]
    return REVIEW_QUESTIONS


def build_report() -> dict[str, Any]:
    design_gate = load_json(DESIGN_FIRST_GATE, {})
    production_lock = load_json(PRODUCTION_LOCK, {})
    roadmap = load_json(ART_ROADMAP, {})
    design_by_key = by_key(design_gate.get("creatures", []))
    lock_by_key = by_key(production_lock.get("locks", []), "key")
    roadmap_by_key = by_key(roadmap.get("creatures", []), "key")

    creatures: list[dict[str, Any]] = []
    for key, design in design_by_key.items():
        civilization = str(design.get("civilization"))
        creature_id = str(design.get("id"))
        lock = lock_by_key.get(key, {})
        road = roadmap_by_key.get(key, {})
        front = design.get("front_matter", {}) if isinstance(design.get("front_matter"), dict) else {}
        latest_candidate = latest_candidate_for(lock)
        concept_art = derived_concept_path(civilization, creature_id)
        visual_review = derived_visual_review_path(civilization, creature_id)
        display = front.get("creature_name") or creature_id
        batch = road.get("batch") or lock.get("batch")
        lock_level = lock.get("lock_level") or "unknown"
        creatures.append({
            "key": key,
            "civilization": civilization,
            "id": creature_id,
            "display": display,
            "entity_id": design.get("entity_id"),
            "batch": batch,
            "priority": road.get("priority") or lock.get("priority"),
            "lock_level": lock_level,
            "design_status": design.get("status"),
            "approved_for_modeling": bool(design.get("approved_for_modeling")),
            "human_decision": road.get("human_decision") or lock.get("human_decision"),
            "design_doc": design.get("design_doc"),
            "concept_art": concept_art,
            "concept_art_exists": (ROOT / concept_art).exists(),
            "visual_review_board": visual_review,
            "visual_review_board_exists": (ROOT / visual_review).exists(),
            "latest_candidate_contract": latest_candidate.get("contract"),
            "latest_candidate_sheet": latest_candidate.get("candidate_sheet"),
            "latest_candidate_sheet_exists": bool(latest_candidate.get("candidate_sheet")) and (ROOT / str(latest_candidate.get("candidate_sheet"))).exists(),
            "v4_candidate_contract": latest_candidate.get("contract"),
            "v4_candidate_sheet": latest_candidate.get("candidate_sheet"),
            "v4_candidate_sheet_exists": bool(latest_candidate.get("candidate_sheet")) and (ROOT / str(latest_candidate.get("candidate_sheet"))).exists(),
            "approval_failures": design.get("approval_failures", []),
            "review_questions": review_questions_for(lock),
            "approval_writeback_required": [
                "design_status=approved_for_modeling",
                "design_sheet_status=approved_for_modeling",
                "design_approval_status=approved_for_modeling",
                "design_correctness_verdict=approved_for_modeling",
                "design_detail_sheet_status=approved_for_modeling",
                "design_detail_board_status=approved_for_modeling",
                "modeling_allowed=true",
            ],
            "allowed_now": lock.get("allowed_steps", []),
            "locked_steps": lock.get("locked_steps", []),
            "next_action": lock.get("next_action") or design.get("next_action"),
            "must_not": lock.get("must_not", design.get("must_not", [])),
        })

    creatures.sort(key=batch_sort_key)
    batch_counts = Counter(item["batch"] for item in creatures)
    civilization_counts = Counter(item["civilization"] for item in creatures)
    lock_counts = Counter(item["lock_level"] for item in creatures)
    missing_assets = [
        {
            "key": item["key"],
            "concept_art_exists": item["concept_art_exists"],
            "visual_review_board_exists": item["visual_review_board_exists"],
        }
        for item in creatures
        if not item["concept_art_exists"] or not item["visual_review_board_exists"]
    ]
    payload = {
        "version": 1,
        "scope": "mythic_age_1_0_creature_design_approval_review_package",
        "ok": False,
        "status": "review_package_ready_designs_not_approved",
        "rule": (
            "This package only organizes design review. It does not approve modeling. "
            "Production remains locked until the design-first gate records approved_for_modeling=true."
        ),
        "source_design_first_gate": project_path(DESIGN_FIRST_GATE),
        "source_production_lock": project_path(PRODUCTION_LOCK),
        "source_art_roadmap": project_path(ART_ROADMAP),
        "contact_sheet": project_path(CONTACT_SHEET_PATH),
        "creature_count": len(creatures),
        "approved_for_modeling_count": sum(1 for item in creatures if item["approved_for_modeling"]),
        "pending_review_count": sum(1 for item in creatures if not item["approved_for_modeling"]),
        "batch_counts": dict(sorted(batch_counts.items())),
        "civilization_counts": dict(sorted(civilization_counts.items())),
        "lock_counts": dict(sorted(lock_counts.items())),
        "missing_asset_count": len(missing_assets),
        "missing_assets": missing_assets,
        "review_order": BATCH_ORDER,
        "creatures": creatures,
    }
    draw_contact_sheet(payload)
    return payload


def fit_image(path: Path, size: tuple[int, int]) -> Image.Image:
    canvas = Image.new("RGBA", size, (247, 244, 236, 255))
    try:
        image = Image.open(path).convert("RGBA")
    except FileNotFoundError:
        draw = ImageDraw.Draw(canvas)
        draw.rectangle((0, 0, size[0] - 1, size[1] - 1), outline=RED, width=3)
        draw.text((16, 16), "Missing image", fill=RED, font=font(22))
        return canvas
    image.thumbnail(size, Image.Resampling.LANCZOS)
    canvas.alpha_composite(image, ((size[0] - image.width) // 2, (size[1] - image.height) // 2))
    return canvas


def draw_wrapped(draw: ImageDraw.ImageDraw, text: str, xy: tuple[int, int], width: int, *, size: int = 22, fill: tuple[int, int, int, int] = INK) -> int:
    x, y = xy
    chars = max(12, width // max(7, int(size * 0.52)))
    for line in textwrap.wrap(text, width=chars):
        draw.text((x, y), line, fill=fill, font=font(size))
        y += size + 6
    return y


def draw_contact_sheet(payload: dict[str, Any]) -> None:
    creatures = payload["creatures"]
    cols = 5
    card_w = 410
    card_h = 415
    gap = 24
    margin = 54
    header_h = 170
    rows = (len(creatures) + cols - 1) // cols
    width = margin * 2 + cols * card_w + (cols - 1) * gap
    height = header_h + rows * card_h + (rows - 1) * gap + margin
    image = Image.new("RGBA", (width, height), BG)
    draw = ImageDraw.Draw(image)
    draw.text((margin, 38), "MythicAgeMod 1.0 Creature Design Approval Review", fill=INK, font=font(42))
    draw.text(
        (margin, 92),
        (
            f"30 creatures. Approved: {payload['approved_for_modeling_count']}. "
            f"Pending: {payload['pending_review_count']}. "
            f"Locks: {payload['lock_counts']}."
        ),
        fill=MUTED,
        font=font(25),
    )
    draw.text((margin, 126), "Review only: this sheet does not unlock modeling.", fill=RED, font=font(24))

    for idx, item in enumerate(creatures):
        row = idx // cols
        col = idx % cols
        x = margin + col * (card_w + gap)
        y = header_h + row * (card_h + gap)
        lock_level = str(item["lock_level"])
        accent = RED if lock_level == "hard_lock_rejected_design" else AMBER
        draw.rounded_rectangle((x, y, x + card_w, y + card_h), radius=10, fill=PANEL, outline=accent, width=4)
        draw.rectangle((x, y, x + card_w, y + 46), fill=accent)
        draw.text((x + 14, y + 10), item["key"], fill=(255, 255, 255, 255), font=font(22))
        image_path = item.get("latest_candidate_sheet") if item.get("latest_candidate_sheet") else item.get("concept_art")
        thumb = fit_image(ROOT / str(image_path), (card_w - 28, 185))
        image.alpha_composite(thumb, (x + 14, y + 58))
        draw.text((x + 14, y + 254), str(item["batch"]), fill=INK, font=font(20))
        draw.text((x + 14, y + 282), lock_level, fill=accent, font=font(19))
        y2 = draw_wrapped(draw, str(item["next_action"]), (x + 14, y + 314), card_w - 28, size=18, fill=MUTED)
        if item.get("visual_review_board_exists"):
            draw.text((x + 14, min(y + card_h - 36, y2 + 8)), "visual board: yes", fill=GREEN, font=font(18))
        else:
            draw.text((x + 14, min(y + card_h - 36, y2 + 8)), "visual board: missing", fill=RED, font=font(18))
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    image.save(CONTACT_SHEET_PATH)


def write_markdown(payload: dict[str, Any]) -> None:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in payload["creatures"]:
        grouped[str(item["batch"])].append(item)

    lines = [
        "# MythicAgeMod 1.0 生物设计审批总包（2026-06-15）",
        "",
        "本文档由 `tools/creature_design_approval_review_package.py audit` 生成。它是设计审批入口，不是批准结果。生产锁仍然有效，任何生物未写入 `approved_for_modeling=true` 前，都不得进入最终模型、UV、纹理、Java runtime 或动作验收。",
        "",
        "## 总览",
        "",
        f"- 审批包状态：`{payload['status']}`",
        f"- 生物数量：{payload['creature_count']}",
        f"- 已批准建模：{payload['approved_for_modeling_count']}",
        f"- 待审批：{payload['pending_review_count']}",
        f"- 批次统计：`{json.dumps(payload['batch_counts'], ensure_ascii=False)}`",
        f"- 文明统计：`{json.dumps(payload['civilization_counts'], ensure_ascii=False)}`",
        f"- 锁统计：`{json.dumps(payload['lock_counts'], ensure_ascii=False)}`",
        f"- 缺失审查图片：{payload['missing_asset_count']}",
        f"- 联系图：`{payload['contact_sheet']}`",
        "",
        f"![creature design approval contact sheet](../preview_assets/creature_design_approval_review/creature_design_approval_contact_sheet.png)",
        "",
        "## 审批规则",
        "",
        "- 测试不负责发明造型，只验证设计是否落地。",
        "- 审批只发生在设计包、候选图、视觉审查板都被人工确认之后。",
        "- 审批通过后，必须显式写回设计 front matter：`design_status=approved_for_modeling`、`modeling_allowed=true` 等字段。",
        "- 未批准前，不能生成最终 `.bbmodel`、最终 UV、最终纹理、Java runtime 同步或最终动作证据。",
        "",
        "## 批次清单",
        "",
    ]
    for batch in BATCH_ORDER:
        items = grouped.get(batch, [])
        if not items:
            continue
        lines.extend([
            f"### {batch}",
            "",
            "| 生物 | 锁级别 | 设计包 | 主要审查图 | 下一步 |",
            "| --- | --- | --- | --- | --- |",
        ])
        for item in items:
            primary_image = item["latest_candidate_sheet"] or item["concept_art"]
            lines.append(
                "| `{key}` | `{lock}` | `{doc}` | `{image}` | `{next}` |".format(
                    key=item["key"],
                    lock=item["lock_level"],
                    doc=item["design_doc"],
                    image=primary_image,
                    next=item["next_action"],
                )
            )
        lines.append("")

    lines.extend(["## 审查问题", ""])
    for item in payload["creatures"]:
        lines.extend([
            f"### {item['key']}",
            "",
            f"- 批次：`{item['batch']}`",
            f"- 锁级别：`{item['lock_level']}`",
            f"- 设计包：`{item['design_doc']}`",
            f"- 概念图：`{item['concept_art']}`",
            f"- 视觉审查板：`{item['visual_review_board']}`",
        ])
        if item["latest_candidate_contract"]:
            lines.append(f"- 最新候选契约：`{item['latest_candidate_contract']}`")
        if item["latest_candidate_sheet"]:
            lines.append(f"- 最新候选图：`{item['latest_candidate_sheet']}`")
        lines.extend(["", "必须确认："])
        for question in item["review_questions"]:
            lines.append(f"- [ ] {question}")
        lines.extend(["", "审批通过后必须写回："])
        for field in item["approval_writeback_required"]:
            lines.append(f"- `{field}`")
        lines.append("")

    lines.extend([
        "## 退出口径",
        "",
        "- 30 个生物全部通过设计审批后，生产锁才可能解除。",
        "- Pegasus/Hydra 必须先解除 `hard_lock_rejected_design`。",
        "- 非希腊 20 生物必须先完成设计审批，再进入 B3 模型/UV/纹理生产。",
        "- 本审批总包通过不等于 1.0 完成；后续还需要模型、动作、纹理、UI、粒子、音效、NBT 和实机验收闭环。",
    ])
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_report()
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        print("Creature Design Approval Review Package: OPEN")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"contact_sheet: {CONTACT_SHEET_PATH}")
        print(f"batch_counts: {payload['batch_counts']}")
        print(f"lock_counts: {payload['lock_counts']}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
