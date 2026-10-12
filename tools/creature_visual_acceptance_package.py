#!/usr/bin/env python3
"""Aggregate 1.0 creature visual acceptance evidence.

The creature visual pipelines prove that design packages, Blockbench files,
textures, UV templates, and previews exist. This package is the human review
handoff: it keeps the design-first evidence in one place and records exactly
which creatures still need shape acceptance.
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
CIVILIZATIONS = ["greek", "egyptian", "nordic", "chinese", "atlantean"]
REPORT_DIR = ROOT / "docs/preview_assets/creature_visual_acceptance"
REPORT_PATH = REPORT_DIR / "creature_visual_acceptance_package.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/entity_validation/creature_visual_acceptance_package.md"
REVIEW_BOARD_DIR = REPORT_DIR / "visual_review_boards"
REVIEW_BOARD_CONTACT_SHEET = REPORT_DIR / "visual_review_board_contact_sheet.png"
PROPORTION_QUALITY_REPORT = REPORT_DIR / "creature_proportion_quality_report.json"
RUNTIME_PROPORTION_QUALITY_REPORT = REPORT_DIR / "creature_runtime_proportion_quality_report.json"

PREVIEW_NAMES = ["front", "side", "three_quarter"]
MIN_SIGNATURE_FEATURES = 3
MIN_REJECT_IF_RULES = 3
MIN_DESIGN_REQUIRED_PARTS = 10
MIN_RUNTIME_DETAIL_EVIDENCE = 12
MIN_BBMODEL_ELEMENTS_1_0 = 24
MIN_BBMODEL_DETAIL_ELEMENTS_1_0 = 8
MIN_RUNTIME_NAMED_PARTS_1_0 = 18
MIN_RUNTIME_DETAIL_TOKENS_1_0 = 8
FOCAL_DETAIL_TOKENS = [
    "eye",
    "face",
    "mouth",
    "jaw",
    "muzzle",
    "beak",
    "brow",
    "nostril",
    "fang",
    "horn",
    "mask",
    "core",
    "gem",
    "crystal",
    "sun_disk",
    "mane",
    "snake",
    "head",
]
STRUCTURE_DETAIL_TOKENS = [
    "wing",
    "tail",
    "horn",
    "weapon",
    "spear",
    "axe",
    "staff",
    "shield",
    "claw",
    "hoof",
    "leg",
    "arm",
    "fin",
    "blade",
    "fang",
    "snake",
    "mane",
    "chain",
    "club",
    "stone",
    "crystal",
    "head",
]
BBMODEL_DETAIL_TOKENS = sorted(set(FOCAL_DETAIL_TOKENS + STRUCTURE_DETAIL_TOKENS + [
    "antler",
    "bandage",
    "beard",
    "belly",
    "boot",
    "bracer",
    "buckle",
    "carapace",
    "cheek",
    "collar",
    "crown",
    "ear",
    "feather",
    "hoof",
    "jaw",
    "mandible",
    "nose",
    "nostril",
    "paw",
    "pelt",
    "plate",
    "pupil",
    "ridge",
    "rune",
    "scale",
    "scute",
    "shell",
    "snout",
    "spike",
    "talisman",
    "tooth",
    "teeth",
    "whisker",
]))


GREEK_USER_FEEDBACK = {
    "hydra": [
        "用户指出旧九头蛇过于臃肿、身子大头小；验收必须看九个比例合理的蛇头、长颈前探、低伏瘦长身体。",
        "每个头都要能看到眼睛、嘴、獠牙、角/毒腺，不允许只靠纹理暗示。",
        "用户复审判定当前九头蛇已经畸形；旧 V2 产物不能继续作为建模依据，必须先确认 V6 可建模规格。",
    ],
    "cyclops": [
        "用户指出独眼巨人应是巨大人形生物，并使用巨大的石棒作为武器。",
        "验收必须确认单眼面部、粗壮人形、手指脚趾和巨大石棒轮廓。",
    ],
    "pegasus": [
        "用户指出天马必须纯白，并且旧模型完全看不出头部。",
        "验收必须确认白马头、耳、鼻口、深色眼点、鬃毛、分层翅膀、自主飞行和骑乘飞行可读性。",
        "用户复审判定当前天马已经畸形；旧 V2 产物不能继续作为建模依据，必须先确认 V6 可建模规格。",
    ],
}

GREEK_DESIGN_REJECTIONS = {
    "hydra": {
        "status": "design_rejected_requires_v6_modeling_spec_approval",
        "rejection_report": "docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md",
        "design_correctness_audit": "docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md",
        "v6_modeling_spec_contract": "docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md",
        "v6_modeling_spec_sheet": "docs/preview_assets/greek_creatures/design_approval/v6/hydra_v6_modeling_spec_sheet.png",
        "next_action": "await_v6_modeling_spec_approval_before_modeling",
    },
    "pegasus": {
        "status": "design_rejected_requires_v6_modeling_spec_approval",
        "rejection_report": "docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md",
        "design_correctness_audit": "docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_correctness_audit_2026-06-15.md",
        "v6_modeling_spec_contract": "docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md",
        "v6_modeling_spec_sheet": "docs/preview_assets/greek_creatures/design_approval/v6/pegasus_v6_modeling_spec_sheet.png",
        "next_action": "await_v6_modeling_spec_approval_before_modeling",
    },
}

GREEK_REVISIONS = {
    "cyclops": "revision_implemented_pending_user_acceptance",
}

DEFAULT_ACCEPTANCE_PROMPTS = [
    "正面是否能看清眼睛、嘴和最重要神话特征？",
    "侧面是否能看清体态、翼、尾、武器或多头结构？",
    "45 度是否比正/侧更能展示层次？",
    "与玩家相比是否有正确体量？",
    "纹理是否有空白、透明破面或关键面错位？",
    "是否能一眼归属到对应文明？",
]

RESAMPLE_LANCZOS = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
FONT_PATHS = (
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
)
CIVILIZATION_COLORS = {
    "greek": (93, 116, 176, 255),
    "egyptian": (181, 132, 54, 255),
    "nordic": (82, 132, 139, 255),
    "chinese": (173, 78, 64, 255),
    "atlantean": (93, 145, 128, 255),
}
COLORS = {
    "bg": (239, 235, 223, 255),
    "panel": (248, 245, 237, 255),
    "panel_shadow": (220, 213, 196, 255),
    "ink": (35, 31, 26, 255),
    "muted": (96, 88, 75, 255),
    "ok": (63, 126, 82, 255),
    "pending": (176, 112, 45, 255),
    "fail": (158, 64, 58, 255),
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def load_font(size: int) -> ImageFont.ImageFont:
    for path in FONT_PATHS:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                pass
    return ImageFont.load_default()


def wrap_text(text: str, font: ImageFont.ImageFont, max_width: int) -> list[str]:
    lines: list[str] = []
    current = ""
    for char in text:
        candidate = current + char
        if font.getbbox(candidate)[2] <= max_width or not current:
            current = candidate
        else:
            lines.append(current)
            current = char
    if current:
        lines.append(current)
    return lines


def draw_wrapped(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, font: ImageFont.ImageFont,
                 fill: tuple[int, int, int, int], max_width: int, line_height: int,
                 max_lines: int | None = None) -> int:
    x, y = xy
    lines = wrap_text(text, font, max_width)
    if max_lines is not None and len(lines) > max_lines:
        lines = lines[:max_lines]
        lines[-1] = lines[-1].rstrip("。,.，") + "..."
    for line in lines:
        draw.text((x, y), line, fill=fill, font=font)
        y += line_height
    return y


def draw_panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str,
               title_font: ImageFont.ImageFont) -> None:
    x1, y1, x2, y2 = box
    draw.rounded_rectangle((x1 + 4, y1 + 4, x2 + 4, y2 + 4), radius=8, fill=COLORS["panel_shadow"])
    draw.rounded_rectangle(box, radius=8, fill=COLORS["panel"])
    draw.text((x1 + 20, y1 + 14), title, fill=COLORS["ink"], font=title_font)


def review_board_path(creature: dict[str, Any]) -> Path:
    return REVIEW_BOARD_DIR / str(creature["civilization"]) / f"{creature['id']}_visual_review.png"


def paste_fit(canvas: Image.Image, path_text: str | None, box: tuple[int, int, int, int],
              draw: ImageDraw.ImageDraw, label: str, font: ImageFont.ImageFont) -> None:
    x1, y1, x2, y2 = box
    draw.rectangle(box, fill=(229, 224, 211, 255))
    draw.text((x1 + 10, y1 + 8), label, fill=COLORS["muted"], font=font)
    if not path_text:
        draw.text((x1 + 10, y1 + 40), "missing path", fill=COLORS["fail"], font=font)
        return
    path = ROOT / path_text
    if not path.exists():
        draw.text((x1 + 10, y1 + 40), "missing file", fill=COLORS["fail"], font=font)
        return
    image = Image.open(path).convert("RGBA")
    available_w = x2 - x1 - 16
    available_h = y2 - y1 - 40
    scale = min(available_w / image.width, available_h / image.height)
    size = (max(1, int(image.width * scale)), max(1, int(image.height * scale)))
    resized = image.resize(size, RESAMPLE_LANCZOS)
    px = x1 + (x2 - x1 - size[0]) // 2
    py = y1 + 34 + (available_h - size[1]) // 2
    canvas.alpha_composite(resized, (px, py))


def draw_status_dot(draw: ImageDraw.ImageDraw, x: int, y: int, label: str, ok: bool,
                    font: ImageFont.ImageFont, pending: bool = False) -> int:
    color = COLORS["ok"] if ok else (COLORS["pending"] if pending else COLORS["fail"])
    status = "OK" if ok else ("PENDING" if pending else "MISSING")
    draw.ellipse((x, y + 4, x + 16, y + 20), fill=color)
    draw.text((x + 26, y), f"{label}: {status}", fill=COLORS["ink"], font=font)
    return y + 27


def path_exists(path_text: str | None) -> bool:
    if not path_text:
        return False
    return (ROOT / path_text).exists()


def artifact_presence(artifacts: dict[str, Any]) -> dict[str, Any]:
    preview_paths = artifacts.get("blockbench_previews", {}) if isinstance(artifacts.get("blockbench_previews"), dict) else {}
    checks = {
        "design_doc": path_exists(artifacts.get("design_doc")),
        "concept_art": path_exists(artifacts.get("concept_art")),
        "bbmodel": path_exists(artifacts.get("bbmodel")),
        "texture": path_exists(artifacts.get("texture")),
        "uv_template": path_exists(artifacts.get("uv_template")),
        "texture_fit_preview": path_exists(artifacts.get("texture_fit_preview")),
        "blockbench_front": path_exists(preview_paths.get("front")),
        "blockbench_side": path_exists(preview_paths.get("side")),
        "blockbench_three_quarter": path_exists(preview_paths.get("three_quarter")),
    }
    return {
        "checks": checks,
        "ok": all(checks.values()),
    }


def bbmodel_detail_metrics(artifacts: dict[str, Any]) -> dict[str, Any]:
    """Measure actual Blockbench element density for 1.0 art readiness."""
    path_text = artifacts.get("bbmodel")
    metrics: dict[str, Any] = {
        "path": path_text,
        "exists": path_exists(path_text),
        "element_count": 0,
        "detail_element_count": 0,
        "outliner_group_count": 0,
        "texture_count": 0,
        "detail_element_sample": [],
        "low_detail_reason": [],
    }
    if not path_text or not metrics["exists"]:
        metrics["low_detail_reason"].append("missing_bbmodel")
        return metrics
    try:
        data = load_json(ROOT / path_text, {})
    except json.JSONDecodeError:
        metrics["low_detail_reason"].append("invalid_bbmodel_json")
        return metrics
    elements = data.get("elements", [])
    outliner = data.get("outliner", [])
    textures = data.get("textures", [])
    names = [
        str(element.get("name", ""))
        for element in elements
        if isinstance(element, dict)
    ]
    detail_names = [
        name
        for name in names
        if any(token in name.lower() for token in BBMODEL_DETAIL_TOKENS)
    ]
    metrics.update({
        "element_count": len(names),
        "detail_element_count": len(detail_names),
        "outliner_group_count": len(outliner) if isinstance(outliner, list) else 0,
        "texture_count": len(textures) if isinstance(textures, list) else 0,
        "detail_element_sample": detail_names[:20],
    })
    if metrics["element_count"] < MIN_BBMODEL_ELEMENTS_1_0:
        metrics["low_detail_reason"].append("bbmodel_element_count_below_1_0_threshold")
    if metrics["detail_element_count"] < MIN_BBMODEL_DETAIL_ELEMENTS_1_0:
        metrics["low_detail_reason"].append("bbmodel_detail_element_count_below_1_0_threshold")
    return metrics


def has_token(values: list[str], tokens: list[str]) -> bool:
    text = " ".join(str(value).lower() for value in values)
    return any(token in text for token in tokens)


def visual_quality_contract(creature: dict[str, Any],
                            artifacts: dict[str, Any],
                            artifact_status: dict[str, Any],
                            runtime: dict[str, Any],
                            greek_shape: dict[str, Any] | None) -> dict[str, Any]:
    """Static contract for the design-first visual standard.

    This does not claim the creature is artistically accepted. It checks that
    the design package and runtime model expose enough named structure for a
    human reviewer to judge the actual mythical silhouette instead of a vague
    low-detail placeholder.
    """
    design_target = creature.get("design_target", {})
    silhouette = design_target.get("silhouette", {})
    signature_features = design_target.get("signature_features", [])
    reject_if = design_target.get("reject_if", [])
    design_required_parts = design_target.get("required_parts", [])
    texture_fit = creature.get("texture_fit_summary", {})
    runtime_required_parts = runtime.get("required_parts", [])
    runtime_required_tokens = runtime.get("required_tokens", [])
    runtime_missing_parts = runtime.get("missing_parts", [])
    preview_paths = artifacts.get("blockbench_previews", {}) if isinstance(artifacts.get("blockbench_previews"), dict) else {}
    bbmodel_metrics = bbmodel_detail_metrics(artifacts)
    all_declared_parts = [
        str(part)
        for part in list(design_required_parts or []) + list(runtime_required_parts or [])
    ]
    checks = {
        "target_box_declared": bool(design_target.get("target_box")),
        "visual_height_declared": design_target.get("visual_height") is not None,
        "three_view_silhouette_declared": (
            isinstance(silhouette, dict)
            and all(bool(silhouette.get(key)) for key in ["front", "side"])
            and bool(silhouette.get("three_quarter") or silhouette.get("back"))
        ),
        "signature_features_detailed": (
            isinstance(signature_features, list)
            and len(signature_features) >= MIN_SIGNATURE_FEATURES
        ),
        "reject_if_rules_detailed": (
            isinstance(reject_if, list)
            and len(reject_if) >= MIN_REJECT_IF_RULES
        ),
        "design_required_parts_detailed": (
            isinstance(design_required_parts, list)
            and len(design_required_parts) >= MIN_DESIGN_REQUIRED_PARTS
        ),
        "runtime_detail_evidence_detailed": (
            isinstance(runtime_required_parts, list)
            and isinstance(runtime_required_tokens, list)
            and len(runtime_required_parts) + len(runtime_required_tokens) >= MIN_RUNTIME_DETAIL_EVIDENCE
        ),
        "bbmodel_element_density_1_0": (
            bbmodel_metrics["exists"]
            and bbmodel_metrics["element_count"] >= MIN_BBMODEL_ELEMENTS_1_0
        ),
        "bbmodel_detail_elements_1_0": (
            bbmodel_metrics["exists"]
            and bbmodel_metrics["detail_element_count"] >= MIN_BBMODEL_DETAIL_ELEMENTS_1_0
        ),
        "runtime_named_parts_1_0": (
            isinstance(runtime_required_parts, list)
            and len(runtime_required_parts) >= MIN_RUNTIME_NAMED_PARTS_1_0
        ),
        "runtime_detail_tokens_1_0": (
            isinstance(runtime_required_tokens, list)
            and len(runtime_required_tokens) >= MIN_RUNTIME_DETAIL_TOKENS_1_0
        ),
        "runtime_no_missing_parts": not runtime_missing_parts,
        "focal_detail_named": has_token(all_declared_parts, FOCAL_DETAIL_TOKENS),
        "signature_structure_named": has_token(all_declared_parts, STRUCTURE_DETAIL_TOKENS),
        "preview_triad_complete": all(path_exists(preview_paths.get(name)) for name in PREVIEW_NAMES),
        "texture_fit_ok": bool(texture_fit.get("ok")) and not texture_fit.get("failures"),
        "artifact_presence_ok": bool(artifact_status.get("ok")),
    }
    if greek_shape:
        checks["greek_mechanical_contract_ok"] = bool(greek_shape.get("mechanical_contract_ok"))
    failures = [key for key, ok in checks.items() if not ok]
    return {
        "ok": not failures,
        "status": "quality_contract_passed" if not failures else "needs_detail_contract_refinement",
        "checks": checks,
        "failures": failures,
        "metrics": {
            "signature_feature_count": len(signature_features) if isinstance(signature_features, list) else 0,
            "reject_if_count": len(reject_if) if isinstance(reject_if, list) else 0,
            "design_required_part_count": len(design_required_parts) if isinstance(design_required_parts, list) else 0,
            "runtime_required_part_count": len(runtime_required_parts) if isinstance(runtime_required_parts, list) else 0,
            "runtime_required_token_count": len(runtime_required_tokens) if isinstance(runtime_required_tokens, list) else 0,
            "runtime_detail_evidence_count": (
                (len(runtime_required_parts) if isinstance(runtime_required_parts, list) else 0)
                + (len(runtime_required_tokens) if isinstance(runtime_required_tokens, list) else 0)
            ),
            "texture_fit_elements_checked": texture_fit.get("elements_checked"),
            "runtime_missing_part_count": len(runtime_missing_parts) if isinstance(runtime_missing_parts, list) else 0,
            "bbmodel_element_count": bbmodel_metrics["element_count"],
            "bbmodel_detail_element_count": bbmodel_metrics["detail_element_count"],
            "bbmodel_outliner_group_count": bbmodel_metrics["outliner_group_count"],
            "bbmodel_texture_count": bbmodel_metrics["texture_count"],
            "bbmodel_low_detail_reason": bbmodel_metrics["low_detail_reason"],
            "bbmodel_detail_element_sample": bbmodel_metrics["detail_element_sample"],
        },
        "thresholds": {
            "min_signature_features": MIN_SIGNATURE_FEATURES,
            "min_reject_if_rules": MIN_REJECT_IF_RULES,
            "min_design_required_parts": MIN_DESIGN_REQUIRED_PARTS,
            "min_runtime_detail_evidence": MIN_RUNTIME_DETAIL_EVIDENCE,
            "min_bbmodel_elements_1_0": MIN_BBMODEL_ELEMENTS_1_0,
            "min_bbmodel_detail_elements_1_0": MIN_BBMODEL_DETAIL_ELEMENTS_1_0,
            "min_runtime_named_parts_1_0": MIN_RUNTIME_NAMED_PARTS_1_0,
            "min_runtime_detail_tokens_1_0": MIN_RUNTIME_DETAIL_TOKENS_1_0,
        },
    }


def runtime_by_id(civilization: str) -> dict[str, dict[str, Any]]:
    data = load_json(ROOT / f"docs/preview_assets/{civilization}_creatures/runtime_model_quality_report.json", {})
    return {
        item.get("id"): item
        for item in data.get("creatures", [])
        if isinstance(item, dict) and isinstance(item.get("id"), str)
    }


def greek_shape_by_id() -> dict[str, dict[str, Any]]:
    data = load_json(ROOT / "docs/preview_assets/greek_creatures/shape_acceptance_report.json", {})
    return {
        item.get("id"): item
        for item in data.get("creatures", [])
        if isinstance(item, dict) and isinstance(item.get("id"), str)
    }


def proportion_quality_by_key() -> dict[tuple[str, str], dict[str, Any]]:
    data = load_json(PROPORTION_QUALITY_REPORT, {})
    return {
        (item.get("civilization"), item.get("id")): item
        for item in data.get("creatures", [])
        if isinstance(item, dict)
        and isinstance(item.get("civilization"), str)
        and isinstance(item.get("id"), str)
    }


def runtime_proportion_quality_by_key() -> dict[tuple[str, str], dict[str, Any]]:
    data = load_json(RUNTIME_PROPORTION_QUALITY_REPORT, {})
    return {
        (item.get("civilization"), item.get("id")): item
        for item in data.get("creatures", [])
        if isinstance(item, dict)
        and isinstance(item.get("civilization"), str)
        and isinstance(item.get("id"), str)
    }


def runtime_proportion_summary(runtime_proportion_quality: dict[str, Any]) -> dict[str, Any]:
    ratios = runtime_proportion_quality.get("ratios", {})
    visual_ratios = runtime_proportion_quality.get("visual_ratios", {})
    detail_first = runtime_proportion_quality.get("detail_first_readability", {})
    return {
        "ok": bool(runtime_proportion_quality.get("ok", False)),
        "detail_first_readability_ok": bool(runtime_proportion_quality.get("detail_first_readability_ok", False)),
        "head_ratios": {
            "height_to_body_height": ratios.get("height_to_body_height"),
            "width_to_body_width": ratios.get("width_to_body_width"),
            "depth_to_body_depth": ratios.get("depth_to_body_depth"),
        },
        "visual_head_ratios": {
            "height_to_body_height": visual_ratios.get("height_to_body_height"),
            "width_to_body_width": visual_ratios.get("width_to_body_width"),
            "depth_to_body_depth": visual_ratios.get("depth_to_body_depth"),
        },
        "detail_count": runtime_proportion_quality.get("detail_count"),
        "required_detail_tokens": detail_first.get("required_detail_tokens", []),
        "failed_proportion_checks": detail_first.get("failed_proportion_checks", []),
        "failed_detail_checks": detail_first.get("failed_detail_checks", []),
        "failures": runtime_proportion_quality.get("failures", []),
        "rule": "Head scale and visual face volume must stay body-proportionate; detail must come from named eyes, mouth, jaw, mane, horn, wing, tail, or weapon parts.",
    }


def creature_acceptance_status(civilization: str, creature_id: str) -> str:
    if civilization == "greek" and creature_id in GREEK_DESIGN_REJECTIONS:
        return GREEK_DESIGN_REJECTIONS[creature_id]["status"]
    if civilization == "greek" and creature_id in GREEK_REVISIONS:
        return GREEK_REVISIONS[creature_id]
    return "pending_human_shape_review"


def review_questions(creature: dict[str, Any]) -> list[str]:
    design_target = creature.get("design_target", {})
    questions = list(DEFAULT_ACCEPTANCE_PROMPTS)
    signature_features = design_target.get("signature_features", [])
    if isinstance(signature_features, list) and signature_features:
        questions.append("核心识别特征是否全部实现：" + "；".join(str(item) for item in signature_features[:4]))
    reject_if = design_target.get("reject_if", [])
    if isinstance(reject_if, list) and reject_if:
        questions.append("是否避开返工触发条件：" + "；".join(str(item) for item in reject_if[:4]))
    return questions


def summarize_creature(civilization: str, creature: dict[str, Any],
                       runtime: dict[str, Any], greek_shape: dict[str, Any] | None,
                       proportion_quality: dict[str, Any] | None,
                       runtime_proportion_quality: dict[str, Any] | None) -> dict[str, Any]:
    creature_id = creature["id"]
    artifacts = creature.get("artifacts", {})
    artifact_status = artifact_presence(artifacts)
    pipeline_checks = creature.get("checks", {})
    design_target = creature.get("design_target", {})
    runtime_ok = bool(runtime.get("ok"))
    texture_fit = creature.get("texture_fit_summary", {})
    status = creature_acceptance_status(civilization, creature_id)
    quality_contract = visual_quality_contract(creature, artifacts, artifact_status, runtime, greek_shape)
    proportion_quality = proportion_quality or {}
    proportion_ok = bool(proportion_quality.get("ok", False))
    runtime_proportion_quality = runtime_proportion_quality or {}
    runtime_proportion_ok = bool(runtime_proportion_quality.get("ok", False))
    runtime_proportion = runtime_proportion_summary(runtime_proportion_quality)
    design_rejection = (
        GREEK_DESIGN_REJECTIONS.get(creature_id)
        if civilization == "greek"
        else None
    )
    next_action = (
        design_rejection["next_action"]
        if design_rejection
        else "await_user_acceptance_of_revision"
        if status == "revision_implemented_pending_user_acceptance"
        else "await_human_shape_acceptance"
    )
    gate_ok = (
        artifact_status["ok"]
        and all(bool(pipeline_checks.get(key)) for key in [
            "design_doc_complete",
            "concept_art_present",
            "bbmodel_present",
            "uv_template_present",
            "blockbench_previews_present",
            "texture_fit_preview_present",
            "no_blank_or_transparent_key_uv_regions",
            "texture_model_fit_passed",
        ])
        and runtime_ok
        and proportion_ok
        and runtime_proportion_ok
    )
    return {
        "id": creature_id,
        "civilization": civilization,
        "entity_id": creature.get("entity_id"),
        "status": status,
        "automatic_evidence_ok": gate_ok,
        "human_acceptance": "pending",
        "manual_review_required": True,
        "user_feedback": GREEK_USER_FEEDBACK.get(creature_id, []),
        "design_rejection": design_rejection or {},
        "design_target": {
            "target_box": design_target.get("target_box"),
            "visual_height": design_target.get("visual_height"),
            "silhouette": design_target.get("silhouette", {}),
            "signature_features": design_target.get("signature_features", []),
            "reject_if": design_target.get("reject_if", []),
        },
        "artifacts": artifacts,
        "artifact_presence": artifact_status,
        "pipeline_checks": pipeline_checks,
        "visual_quality_contract": quality_contract,
        "proportion_quality_gate": proportion_quality,
        "runtime_proportion_quality_gate": runtime_proportion_quality,
        "runtime_proportion_summary": runtime_proportion,
        "texture_fit_summary": {
            "ok": texture_fit.get("ok"),
            "elements_checked": texture_fit.get("elements_checked"),
            "average_opaque_ratio": texture_fit.get("average_opaque_ratio"),
            "average_color_variation": texture_fit.get("average_color_variation"),
            "failures": texture_fit.get("failures", []),
        },
        "runtime_model_gate": {
            "ok": runtime_ok,
            "model": runtime.get("model"),
            "missing_parts": runtime.get("missing_parts", []),
            "missing_tokens": runtime.get("missing_tokens", []),
            "required_parts": runtime.get("required_parts", []),
        },
        "greek_shape_gate": greek_shape or {},
        "review_questions": review_questions(creature),
        "next_action": next_action,
    }


def draw_visual_review_board(creature: dict[str, Any]) -> None:
    out = review_board_path(creature)
    out.parent.mkdir(parents=True, exist_ok=True)
    width, height = 1600, 1440
    canvas = Image.new("RGBA", (width, height), COLORS["bg"])
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(42)
    h2_font = load_font(24)
    body_font = load_font(19)
    small_font = load_font(15)
    civ = creature["civilization"]
    header_color = CIVILIZATION_COLORS.get(civ, (92, 92, 92, 255))
    artifacts = creature["artifacts"]
    previews = artifacts.get("blockbench_previews", {})
    contract = creature["visual_quality_contract"]
    design = creature["design_target"]
    texture_fit = creature["texture_fit_summary"]
    runtime_proportion = creature.get("runtime_proportion_summary", {})

    draw.rectangle((0, 0, width, 112), fill=header_color)
    draw.text((48, 30), f"{civ.upper()} / {creature['id']}", fill=(255, 252, 244, 255), font=title_font)
    draw.text((48, 76), "creature visual review board - human acceptance pending",
              fill=(245, 239, 225, 255), font=body_font)
    y_badge = 34
    draw_status_dot(draw, 1110, y_badge, "auto evidence", bool(creature["automatic_evidence_ok"]), small_font)
    draw_status_dot(draw, 1110, y_badge + 28, "human acceptance", False, small_font, pending=True)

    draw_panel(draw, (48, 140, 604, 516), "Concept", h2_font)
    paste_fit(canvas, artifacts.get("concept_art"), (72, 192, 580, 492), draw, "concept art", small_font)

    draw_panel(draw, (636, 140, 1552, 516), "Blockbench Three-View", h2_font)
    paste_fit(canvas, previews.get("front"), (660, 192, 948, 492), draw, "front", small_font)
    paste_fit(canvas, previews.get("side"), (968, 192, 1256, 492), draw, "side", small_font)
    paste_fit(canvas, previews.get("three_quarter"), (1276, 192, 1528, 492), draw, "three-quarter", small_font)

    draw_panel(draw, (48, 550, 604, 914), "Texture And Scale", h2_font)
    paste_fit(canvas, artifacts.get("texture_fit_preview"), (72, 604, 308, 884), draw, "texture fit", small_font)
    y = 612
    y = draw_wrapped(draw, (332, y), f"target box: {design.get('target_box')}", body_font, COLORS["ink"], 240, 24, 2)
    y = draw_wrapped(draw, (332, y + 6), f"visual height: {design.get('visual_height')}", body_font, COLORS["ink"], 240, 24, 2)
    y = draw_wrapped(draw, (332, y + 6), f"texture fit ok: {texture_fit.get('ok')}", body_font, COLORS["ink"], 240, 24, 2)
    y = draw_wrapped(draw, (332, y + 6), f"checked elements: {texture_fit.get('elements_checked')}", body_font, COLORS["ink"], 240, 24, 2)
    visual_ratios = runtime_proportion.get("visual_head_ratios", {})
    ratio_text = "visual head h/w/d: {h}/{w}/{d}".format(
        h=visual_ratios.get("height_to_body_height"),
        w=visual_ratios.get("width_to_body_width"),
        d=visual_ratios.get("depth_to_body_depth"),
    )
    y = draw_wrapped(draw, (332, y + 6), ratio_text, small_font, COLORS["ink"], 240, 20, 2)
    y = draw_wrapped(draw, (332, y + 4), f"runtime detail parts: {runtime_proportion.get('detail_count')}", small_font, COLORS["ink"], 240, 20, 1)
    y += 8
    y = draw_status_dot(draw, 336, y, "quality contract", bool(contract["ok"]), small_font)
    y = draw_status_dot(draw, 336, y, "head/body proportion", bool(runtime_proportion.get("ok")), small_font)
    y = draw_status_dot(draw, 336, y, "detail-first shape", bool(runtime_proportion.get("detail_first_readability_ok")), small_font)
    y = draw_status_dot(draw, 336, y, "revision/user review", False, small_font, pending=True)
    if creature.get("user_feedback"):
        y += 8
        draw.text((336, y), "User feedback focus", fill=COLORS["muted"], font=small_font)
        y += 24
        for item in creature["user_feedback"]:
            y = draw_wrapped(draw, (336, y), "- " + item, small_font, COLORS["ink"], 240, 18, 2)
            y += 3

    draw_panel(draw, (636, 550, 1552, 914), "Design Acceptance Targets", h2_font)
    y = 604
    features = design.get("signature_features") or []
    reject_if = design.get("reject_if") or []
    draw.text((660, y), "Signature features", fill=COLORS["muted"], font=small_font)
    y += 24
    for item in features[:5]:
        y = draw_wrapped(draw, (660, y), "- " + str(item), body_font, COLORS["ink"], 860, 24, 2)
        y += 4
        if y > 760:
            break
    y += 8
    draw.text((660, y), "Reject / rework if", fill=COLORS["muted"], font=small_font)
    y += 24
    for item in reject_if[:4]:
        y = draw_wrapped(draw, (660, y), "- " + str(item), body_font, COLORS["fail"], 860, 24, 2)
        y += 4
        if y > 888:
            break

    draw_panel(draw, (48, 948, 1552, 1368), "Manual Visual Review Checklist", h2_font)
    y = 1000
    questions = creature["review_questions"]
    left_x, right_x = 72, 820
    for index, question in enumerate(questions[:8]):
        x = left_x if index < 4 else right_x
        local_y = y + (index % 4) * 72
        draw_wrapped(draw, (x, local_y), "- " + question, body_font, COLORS["ink"], 700, 23, 3)
    draw_wrapped(
        draw,
        (72, 1340),
        "Boundary: this board prepares human acceptance. It does not mark the creature as artistically accepted.",
        small_font,
        COLORS["muted"],
        1420,
        18,
        1,
    )

    canvas.convert("RGB").save(out)


def draw_visual_review_boards(payload: dict[str, Any]) -> None:
    for creature in payload["creatures"]:
        draw_visual_review_board(creature)
        path = review_board_path(creature)
        creature["visual_review_board"] = project_path(path)
        creature["visual_review_board_present"] = path.exists()

    cols = 5
    thumb_w, thumb_h = 320, 256
    rows = (len(payload["creatures"]) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, max(1, rows) * thumb_h), (236, 231, 218))
    draw = ImageDraw.Draw(sheet)
    small_font = load_font(14)
    for index, creature in enumerate(payload["creatures"]):
        path = review_board_path(creature)
        if path.exists():
            thumb = Image.open(path).convert("RGB").resize((thumb_w, thumb_h), RESAMPLE_LANCZOS)
            sheet.paste(thumb, ((index % cols) * thumb_w, (index // cols) * thumb_h))
        x = (index % cols) * thumb_w + 8
        y = (index // cols) * thumb_h + 8
        draw.text((x, y), f"{creature['civilization']} / {creature['id']}", fill=(32, 28, 24), font=small_font)
    REVIEW_BOARD_CONTACT_SHEET.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(REVIEW_BOARD_CONTACT_SHEET)
    payload["visual_review_board_contact_sheet"] = project_path(REVIEW_BOARD_CONTACT_SHEET)
    payload["visual_review_board_count"] = sum(
        1 for creature in payload["creatures"] if creature.get("visual_review_board_present")
    )


def build_package() -> dict[str, Any]:
    greek_shapes = greek_shape_by_id()
    proportion_quality_report = load_json(PROPORTION_QUALITY_REPORT, {})
    proportion_quality_map = proportion_quality_by_key()
    runtime_proportion_quality_report = load_json(RUNTIME_PROPORTION_QUALITY_REPORT, {})
    runtime_proportion_quality_map = runtime_proportion_quality_by_key()
    creatures: list[dict[str, Any]] = []
    per_civ: dict[str, Any] = {}
    for civilization in CIVILIZATIONS:
        visual_report_path = ROOT / f"docs/preview_assets/{civilization}_creatures/visual_report.json"
        visual_report = load_json(visual_report_path, {})
        runtime_map = runtime_by_id(civilization)
        civ_creatures = []
        for creature in visual_report.get("creatures", []):
            if not isinstance(creature, dict) or not isinstance(creature.get("id"), str):
                continue
            item = summarize_creature(
                civilization,
                creature,
                runtime_map.get(creature["id"], {}),
                greek_shapes.get(creature["id"]) if civilization == "greek" else None,
                proportion_quality_map.get((civilization, creature["id"])),
                runtime_proportion_quality_map.get((civilization, creature["id"])),
            )
            creatures.append(item)
            civ_creatures.append(item)
        status_counts = Counter(item["status"] for item in civ_creatures)
        quality_status_counts = Counter(item["visual_quality_contract"]["status"] for item in civ_creatures)
        per_civ[civilization] = {
            "visual_report": project_path(visual_report_path),
            "contact_sheet": visual_report.get("contact_sheet"),
            "concept_contact_sheet": visual_report.get("concept_contact_sheet"),
            "creature_count": len(civ_creatures),
            "automatic_evidence_ok_count": sum(1 for item in civ_creatures if item["automatic_evidence_ok"]),
            "visual_quality_contract_ok_count": sum(1 for item in civ_creatures if item["visual_quality_contract"]["ok"]),
            "status_counts": dict(sorted(status_counts.items())),
            "quality_status_counts": dict(sorted(quality_status_counts.items())),
        }

    global_status = Counter(item["status"] for item in creatures)
    global_quality_status = Counter(item["visual_quality_contract"]["status"] for item in creatures)
    design_rejection_blockers = [
        {
            "civilization": item["civilization"],
            "id": item["id"],
            "status": item["status"],
            "rejection_report": item.get("design_rejection", {}).get("rejection_report"),
            "design_correctness_audit": item.get("design_rejection", {}).get("design_correctness_audit"),
            "v6_modeling_spec_contract": item.get("design_rejection", {}).get("v6_modeling_spec_contract"),
            "v6_modeling_spec_sheet": item.get("design_rejection", {}).get("v6_modeling_spec_sheet"),
            "next_action": item["next_action"],
        }
        for item in creatures
        if item["status"] == "design_rejected_requires_v6_modeling_spec_approval"
    ]
    quality_blockers = [
        {
            "civilization": item["civilization"],
            "id": item["id"],
            "status": item["status"],
            "quality_status": item["visual_quality_contract"]["status"],
            "failed_checks": item["visual_quality_contract"]["failures"],
            "metrics": item["visual_quality_contract"]["metrics"],
            "next_action": (
                "expand_design_package_and_named_model_parts_before_human_acceptance"
                if not item["visual_quality_contract"]["ok"]
                else item["next_action"]
            ),
        }
        for item in creatures
        if not item["visual_quality_contract"]["ok"]
    ]
    proportion_blockers = [
        {
            "civilization": item["civilization"],
            "id": item["id"],
            "failed_checks": item.get("proportion_quality_gate", {}).get("failures", []),
            "next_action": "fix_head_body_proportion_or_add_named_face_detail_before_visual_acceptance",
        }
        for item in creatures
        if not item.get("proportion_quality_gate", {}).get("ok", False)
    ]
    proportion_warnings = [
        {
            "civilization": item["civilization"],
            "id": item["id"],
            "warning_checks": item.get("proportion_quality_gate", {}).get("warnings", []),
            "archetype": item.get("proportion_quality_gate", {}).get("archetype"),
            "ratios": item.get("proportion_quality_gate", {}).get("single_head_ratios", []),
            "next_action": "manual_shape_review_should_check_head_body_ratio_and_named_detail_density",
        }
        for item in creatures
        if item.get("proportion_quality_gate", {}).get("warnings")
    ]
    automatic_evidence_package_ok = all(item["automatic_evidence_ok"] for item in creatures)
    visual_quality_contract_ok = all(item["visual_quality_contract"]["ok"] for item in creatures)
    proportion_quality_gate_ok = (
        bool(proportion_quality_report.get("ok"))
        and len(proportion_blockers) == 0
        and len(proportion_quality_map) >= len(creatures)
    )
    proportion_detail_first_ok = (
        bool(proportion_quality_report.get("detail_first_readability_ok"))
        and len(proportion_quality_map) >= len(creatures)
    )
    runtime_proportion_blockers = [
        {
            "civilization": item["civilization"],
            "id": item["id"],
            "failed_checks": item.get("runtime_proportion_quality_gate", {}).get("failures", []),
            "ratios": item.get("runtime_proportion_quality_gate", {}).get("ratios", {}),
            "visual_ratios": item.get("runtime_proportion_quality_gate", {}).get("visual_ratios", {}),
            "detail_count": item.get("runtime_proportion_quality_gate", {}).get("detail_count"),
            "next_action": "fix_runtime_java_model_head_body_proportion_before_visual_acceptance",
        }
        for item in creatures
        if not item.get("runtime_proportion_quality_gate", {}).get("ok", False)
    ]
    runtime_proportion_quality_gate_ok = (
        bool(runtime_proportion_quality_report.get("ok"))
        and len(runtime_proportion_blockers) == 0
        and len(runtime_proportion_quality_map) >= len(creatures)
    )
    runtime_detail_first_ok = (
        bool(runtime_proportion_quality_report.get("detail_first_readability_ok"))
        and len(runtime_proportion_quality_map) >= len(creatures)
    )
    payload = {
        "version": 1,
        "scope": "mythic_age_1_0_creature_visual_acceptance_package",
        "ok": (
            automatic_evidence_package_ok
            and proportion_quality_gate_ok
            and proportion_detail_first_ok
            and runtime_proportion_quality_gate_ok
            and runtime_detail_first_ok
            and visual_quality_contract_ok
            and not design_rejection_blockers
        ),
        "automatic_evidence_package_ok": automatic_evidence_package_ok,
        "proportion_quality_gate_ok": proportion_quality_gate_ok,
        "proportion_quality_report": project_path(PROPORTION_QUALITY_REPORT),
        "proportion_quality_creature_count": len(proportion_quality_map),
        "proportion_quality_blocker_count": len(proportion_blockers),
        "proportion_quality_warning_count": len(proportion_warnings),
        "proportion_quality_blockers": proportion_blockers,
        "proportion_quality_warnings": proportion_warnings,
        "proportion_detail_first_readability_ok": proportion_detail_first_ok,
        "proportion_detail_first_readability_count": proportion_quality_report.get("detail_first_readability_count"),
        "runtime_proportion_quality_gate_ok": runtime_proportion_quality_gate_ok,
        "runtime_proportion_quality_report": project_path(RUNTIME_PROPORTION_QUALITY_REPORT),
        "runtime_proportion_quality_creature_count": len(runtime_proportion_quality_map),
        "runtime_proportion_quality_blocker_count": len(runtime_proportion_blockers),
        "runtime_proportion_quality_blockers": runtime_proportion_blockers,
        "runtime_detail_first_readability_ok": runtime_detail_first_ok,
        "runtime_detail_first_readability_count": runtime_proportion_quality_report.get("detail_first_readability_count"),
        "visual_quality_contract_ok": visual_quality_contract_ok,
        "visual_quality_contract_ok_count": sum(1 for item in creatures if item["visual_quality_contract"]["ok"]),
        "visual_quality_contract_fail_count": len(quality_blockers),
        "visual_quality_contract_blockers": quality_blockers,
        "design_rejection_blocker_count": len(design_rejection_blockers),
        "design_rejection_blockers": design_rejection_blockers,
        "human_visual_acceptance_ok": False,
        "manual_review_required_count": sum(1 for item in creatures if item["manual_review_required"]),
        "revision_pending_user_acceptance_count": sum(1 for item in creatures if item["status"] == "revision_implemented_pending_user_acceptance"),
        "pending_human_shape_review_count": sum(1 for item in creatures if item["status"] == "pending_human_shape_review"),
        "creature_count": len(creatures),
        "civilizations": CIVILIZATIONS,
        "status_counts": dict(sorted(global_status.items())),
        "quality_status_counts": dict(sorted(global_quality_status.items())),
        "review_rule": "Automatic evidence proves the design-first package is ready for review; it does not replace human shape acceptance.",
        "visual_quality_rule": (
            "1.0 creatures must declare target scale, three-view silhouette, signature features, reject-if rules, "
            "at least 10 design parts, at least 24 Blockbench elements, at least 8 named Blockbench detail elements, "
            "at least 18 runtime named parts, at least 8 runtime detail UV tokens, focal details, signature structures, "
            "Blockbench preview triad, and UV texture-fit evidence before human acceptance."
        ),
        "proportion_quality_rule": (
            "Heads must remain proportionate to the creature body archetype; readability must come from explicit "
            "eyes, mouth, jaws, horns, mane, weapon, wing, tail, and UV detail, not abnormal head enlargement. "
            "Creature-specific/user-feedback checks are blockers; broad archetype checks are review warnings."
        ),
        "detail_first_rule": (
            "A proportionate head is required but not sufficient: each creature must preserve enough named face "
            "and signature parts that reducing head scale does not reduce eye, mouth, snout, horn, wing, tail, "
            "weapon, or mythic-feature readability."
        ),
        "runtime_proportion_quality_rule": (
            "Runtime Java model cuboids must match the same head/body proportion rule, including visual face volume "
            "from snout/jaw/mouth/brow/cheek parts; Blockbench/design evidence is not enough if the in-game model "
            "still renders an oversized or under-detailed head."
        ),
        "art_bible": "docs/美术资源/MythicAgeMod_1.0美术Bible_2026-06-11.md",
        "per_civilization": per_civ,
        "creatures": creatures,
    }
    draw_visual_review_boards(payload)
    return payload


def write_markdown(payload: dict[str, Any]) -> None:
    lines: list[str] = [
        "# MythicAgeMod 1.0 生物视觉人工验收包",
        "",
        "本文档由 `tools/creature_visual_acceptance_package.py audit` 生成。自动证据只证明设计包、Blockbench、UV、贴图、预览和 runtime model gate 已准备好；它不替代人工审美验收。",
        "",
        "## 总览",
        "",
        f"- 生物数量：{payload['creature_count']}",
        f"- 自动证据通过：{payload['ok']}",
        f"- 自动证据包通过：{payload['automatic_evidence_package_ok']}",
        f"- 头身比例质量门通过：{payload['proportion_quality_gate_ok']}",
        f"- 设计/Blockbench 细节优先门通过：{payload['proportion_detail_first_readability_ok']} ({payload['proportion_detail_first_readability_count']}/{payload['creature_count']})",
        f"- 头身比例质量门待补数量：{payload['proportion_quality_blocker_count']}",
        f"- 头身比例/细节审美预警数量：{payload['proportion_quality_warning_count']}",
        f"- 运行时 Java 头身比例门通过：{payload['runtime_proportion_quality_gate_ok']}",
        f"- 运行时 Java 细节优先门通过：{payload['runtime_detail_first_readability_ok']} ({payload['runtime_detail_first_readability_count']}/{payload['creature_count']})",
        f"- 运行时 Java 头身比例待补数量：{payload['runtime_proportion_quality_blocker_count']}",
        f"- 1.0 视觉质量契约通过：{payload['visual_quality_contract_ok']}",
        f"- 视觉质量契约通过数量：{payload['visual_quality_contract_ok_count']}/{payload['creature_count']}",
        f"- 视觉质量契约待补数量：{payload['visual_quality_contract_fail_count']}",
        f"- 设计拒绝阻断数量：{payload['design_rejection_blocker_count']}",
        f"- 人工视觉验收通过：{payload['human_visual_acceptance_ok']}",
        f"- 需要人工验收：{payload['manual_review_required_count']}",
        f"- 视觉 review board：{payload.get('visual_review_board_count', 0)} / {payload['creature_count']}",
        f"- Review board 总览图：`{payload.get('visual_review_board_contact_sheet', '')}`",
        f"- Greek 返修待用户接受：{payload['revision_pending_user_acceptance_count']}",
        f"- 待人工形体审查：{payload['pending_human_shape_review_count']}",
        f"- 状态统计：`{json.dumps(payload['status_counts'], ensure_ascii=False)}`",
        f"- 质量契约统计：`{json.dumps(payload['quality_status_counts'], ensure_ascii=False)}`",
        f"- 质量规则：{payload['visual_quality_rule']}",
        f"- 比例规则：{payload['proportion_quality_rule']}",
        f"- 细节优先规则：{payload['detail_first_rule']}",
        f"- 运行时比例规则：{payload['runtime_proportion_quality_rule']}",
        "",
        "## 文明验收材料",
        "",
    ]
    for civilization in CIVILIZATIONS:
        civ = payload["per_civilization"][civilization]
        lines.extend([
            f"### {civilization}",
            "",
            f"- Visual report：`{civ['visual_report']}`",
            f"- Blockbench contact sheet：`{civ.get('contact_sheet')}`",
            f"- Concept contact sheet：`{civ.get('concept_contact_sheet')}`",
            f"- 生物数量：{civ['creature_count']}",
            f"- 自动证据通过数量：{civ['automatic_evidence_ok_count']}",
            f"- 视觉质量契约通过数量：{civ['visual_quality_contract_ok_count']}",
            f"- 状态统计：`{json.dumps(civ['status_counts'], ensure_ascii=False)}`",
            f"- 质量契约统计：`{json.dumps(civ['quality_status_counts'], ensure_ascii=False)}`",
            "",
        ])

    lines.extend([
        "## 1.0 视觉质量契约清单",
        "",
        "| 文明 | 生物 | 质量契约 | 设计部件 | BB 元素 | BB 细节 | Runtime 部件 | Runtime 细节 | 失败项 | 下一步 |",
        "| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |",
    ])
    for creature in payload["creatures"]:
        contract = creature["visual_quality_contract"]
        metrics = contract["metrics"]
        failures = "；".join(contract["failures"]) or "-"
        next_action = (
            "先补设计拆解和命名部件"
            if not contract["ok"]
            else creature["next_action"]
        )
        lines.append(
            "| {civilization} | `{id}` | `{status}` | {design_parts} | {bbmodel_elements} | {bbmodel_details} | {runtime_parts} | {runtime_tokens} | {failures} | {next_action} |".format(
                civilization=creature["civilization"],
                id=creature["id"],
                status=contract["status"],
                design_parts=metrics["design_required_part_count"],
                bbmodel_elements=metrics["bbmodel_element_count"],
                bbmodel_details=metrics["bbmodel_detail_element_count"],
                runtime_parts=metrics["runtime_detail_evidence_count"],
                runtime_tokens=metrics["runtime_required_token_count"],
                failures=failures.replace("|", "/"),
                next_action=next_action,
            )
        )

    lines.extend([
        "",
        "## 头身比例质量门",
        "",
        f"- 报告：`{payload['proportion_quality_report']}`",
        f"- 通过：{payload['proportion_quality_gate_ok']}",
        f"- 覆盖生物：{payload['proportion_quality_creature_count']}/{payload['creature_count']}",
        f"- 待补数量：{payload['proportion_quality_blocker_count']}",
        f"- 审美预警数量：{payload['proportion_quality_warning_count']}",
        f"- 细节优先通过：{payload['proportion_detail_first_readability_ok']} ({payload['proportion_detail_first_readability_count']}/{payload['creature_count']})",
        "- 规则：头部必须符合身体比例，神话特征可读性来自独立眼、嘴、颌、角、鬃毛、武器、翼、尾和固定 UV 细节，而不是异常放大头部。",
        "",
    ])
    if payload["proportion_quality_warnings"]:
        lines.extend([
            "### 头身比例/细节预警清单",
            "",
            "| 文明 | 生物 | 类型 | 预警项 | 头部比例摘要 | 下一步 |",
            "| --- | --- | --- | --- | --- | --- |",
        ])
        for warning in payload["proportion_quality_warnings"]:
            ratio_bits = []
            for ratio in warning.get("ratios", [])[:2]:
                ratio_bits.append(
                    "{name}: h={height}, w={width}, d={depth}".format(
                        name=ratio.get("name"),
                        height=ratio.get("height_to_body_height"),
                        width=ratio.get("width_to_body_width"),
                        depth=ratio.get("depth_to_body_depth"),
                    )
                )
            lines.append(
                "| {civilization} | `{id}` | `{archetype}` | {checks} | {ratios} | {next_action} |".format(
                    civilization=warning["civilization"],
                    id=warning["id"],
                    archetype=warning.get("archetype"),
                    checks="；".join(warning.get("warning_checks", [])).replace("|", "/"),
                    ratios="；".join(ratio_bits).replace("|", "/") or "-",
                    next_action=warning["next_action"],
                )
            )
        lines.append("")

    if payload["design_rejection_blockers"]:
        lines.extend([
            "## 设计拒绝阻断项",
            "",
            "| 文明 | 生物 | 状态 | 冻结记录 | 正确性审查 | V6 规格合同 | V6 规格图 | 下一步 |",
            "| --- | --- | --- | --- | --- | --- | --- | --- |",
        ])
        for blocker in payload["design_rejection_blockers"]:
            lines.append(
                "| {civilization} | `{id}` | `{status}` | `{rejection}` | `{audit}` | `{blueprint}` | `{sheet}` | `{next_action}` |".format(
                    civilization=blocker["civilization"],
                    id=blocker["id"],
                    status=blocker["status"],
                    rejection=blocker.get("rejection_report"),
                    audit=blocker.get("design_correctness_audit"),
                    blueprint=blocker.get("v6_modeling_spec_contract"),
                    sheet=blocker.get("v6_modeling_spec_sheet"),
                    next_action=blocker["next_action"],
                )
            )
        lines.append("")

    lines.extend([
        "## 运行时 Java 头身比例门",
        "",
        f"- 报告：`{payload['runtime_proportion_quality_report']}`",
        f"- 通过：{payload['runtime_proportion_quality_gate_ok']}",
        f"- 覆盖生物：{payload['runtime_proportion_quality_creature_count']}/{payload['creature_count']}",
        f"- 待补数量：{payload['runtime_proportion_quality_blocker_count']}",
        f"- 细节优先通过：{payload['runtime_detail_first_readability_ok']} ({payload['runtime_detail_first_readability_count']}/{payload['creature_count']})",
        "- 规则：游戏内 Java 模型也必须符合头身比例；不能只让 Blockbench/设计包正确而实机仍保留旧大头模型。",
        "",
    ])
    if payload["runtime_proportion_quality_blockers"]:
        lines.extend([
            "### 运行时比例待补清单",
            "",
            "| 文明 | 生物 | 失败项 | 比例摘要 | 下一步 |",
            "| --- | --- | --- | --- | --- |",
        ])
        for blocker in payload["runtime_proportion_quality_blockers"]:
            ratios = blocker.get("ratios", {})
            visual_ratios = blocker.get("visual_ratios", {})
            ratio_text = "h={height}, w={width}, d={depth}".format(
                height=ratios.get("height_to_body_height"),
                width=ratios.get("width_to_body_width"),
                depth=ratios.get("depth_to_body_depth"),
            )
            visual_ratio_text = "visual h={height}, w={width}, d={depth}; detail={detail}".format(
                height=visual_ratios.get("height_to_body_height"),
                width=visual_ratios.get("width_to_body_width"),
                depth=visual_ratios.get("depth_to_body_depth"),
                detail=blocker.get("detail_count"),
            )
            lines.append(
                "| {civilization} | `{id}` | {checks} | {ratios} | {next_action} |".format(
                    civilization=blocker["civilization"],
                    id=blocker["id"],
                    checks="；".join(blocker.get("failed_checks", [])).replace("|", "/"),
                    ratios=(ratio_text + "; " + visual_ratio_text).replace("|", "/"),
                    next_action=blocker["next_action"],
                )
            )
        lines.append("")

    lines.extend([
        "## 单体清单",
        "",
        "| 文明 | 生物 | 状态 | 自动证据 | 设计比例门 | 运行时比例门 | 细节优先 | 视觉头部比例 h/w/d | 细节数 | 质量契约 | 下一步 | Review board | 关键验收点 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | ---: | --- | --- | --- | --- |",
    ])
    for creature in payload["creatures"]:
        features = creature["design_target"].get("signature_features") or []
        feature_text = "；".join(str(item) for item in features[:2])
        if not feature_text:
            silhouette = creature["design_target"].get("silhouette") or {}
            feature_text = "；".join(str(value) for value in list(silhouette.values())[:2])
        runtime_proportion = creature.get("runtime_proportion_summary", {})
        visual_ratios = runtime_proportion.get("visual_head_ratios", {})
        visual_ratio_text = "{h}/{w}/{d}".format(
            h=visual_ratios.get("height_to_body_height"),
            w=visual_ratios.get("width_to_body_width"),
            d=visual_ratios.get("depth_to_body_depth"),
        )
        lines.append(
            "| {civilization} | `{id}` | `{status}` | `{evidence}` | `{proportion}` | `{runtime_proportion}` | `{detail_first}` | `{visual_ratio}` | {detail_count} | `{quality}` | `{next_action}` | `{board}` | {features} |".format(
                civilization=creature["civilization"],
                id=creature["id"],
                status=creature["status"],
                evidence=creature["automatic_evidence_ok"],
                proportion=creature.get("proportion_quality_gate", {}).get("ok"),
                runtime_proportion=creature.get("runtime_proportion_quality_gate", {}).get("ok"),
                detail_first=runtime_proportion.get("detail_first_readability_ok"),
                visual_ratio=visual_ratio_text,
                detail_count=runtime_proportion.get("detail_count"),
                quality=creature["visual_quality_contract"]["ok"],
                next_action=creature["next_action"],
                board=creature.get("visual_review_board", ""),
                features=feature_text.replace("|", "/"),
            )
        )

    lines.extend([
        "",
        "## 验收规则",
        "",
        "- 先看概念图和设计包，再看 Blockbench 三视图；测试只验证设计是否落地，不负责临场重做造型。",
        "- 每个生物至少看 `front`、`side`、`three_quarter` 三张 Blockbench 预览和 `texture_fit.png`。",
        "- 有用户反馈且仍是修订待确认的 Greek 项：`cyclops`。",
        "- `pegasus` 和 `hydra` 当前不是“待确认修订”，而是 `design_rejected_requires_v6_modeling_spec_approval`；必须先签收 V6 可建模规格，才能重新建模。",
        "- 非 Greek 20 个生物虽然自动证据齐全，但仍停在 `pending_human_shape_review`，不能视为最终美术完成。",
        "- `needs_detail_contract_refinement` 表示该生物还只是流程证据齐全，不满足 1.0 模型精细度；必须补结构件、五官/武器/翼/尾/鳞片等细节件和 runtime 命名部件后再进入人工签收。",
        "",
    ])
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_package()
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        status = "PASS" if payload["ok"] else "FAIL"
        print(f"Mythic Age Creature Visual Acceptance Package: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"creatures: {payload['creature_count']}, manual_review_required: {payload['manual_review_required_count']}")
        if not payload["ok"]:
            return 1
        return 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
