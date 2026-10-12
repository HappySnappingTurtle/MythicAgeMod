#!/usr/bin/env python3
"""Greek creature visual acceptance gate.

The existing Blockbench gate proves that assets exist, textures are mapped, and
runtime model parts are present. This gate records the higher-level creature
shape acceptance state so user-reported silhouette failures cannot be hidden by
token-based checks.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

from greek_creature_visual_pipeline import CREATURES, PREVIEW_DIR, REPORT_PATH as VISUAL_REPORT_PATH, TEXTURE_FIT_REPORT_PATH


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/greek_creatures"
REPORT_PATH = REPORT_DIR / "shape_acceptance_report.json"

USER_FEEDBACK = {
    "hydra": [
        "用户明确指出九头蛇曾经过于臃肿、身子大头小。",
        "用户复测指出九头蛇头和身体曾经没有连接上。",
        "必须回到设计阶段确认头数、头-颈-肩连接、低伏长身和每头眼/嘴/獠牙/角结构。",
        "V6 默认按显性 9 头设计；如改为 5 个主战头，必须先在设计规格阶段确认，不能由模型试错决定。",
    ],
    "cyclops": [
        "用户明确指出独眼巨人应是巨大人形生物，并使用巨大的石棒作为武器。",
        "必须验证单眼面部、人形躯干、粗臂短腿、手指脚趾和大石棒轮廓。",
    ],
    "pegasus": [
        "用户明确指出天马必须是纯白色，且当前完全看不出头部。",
        "用户复测指出天马头部曾经太大，必须参考原版马的细长头颈比例。",
        "必须验证白马头、长颈、深色眼点、耳/鼻口、分层翅膀、自主飞行和骑乘飞行可读性。",
    ],
}

DESIGN_REJECTIONS = {
    "hydra": {
        "status": "design_rejected_requires_v6_modeling_spec_approval",
        "rejection_report": "docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md",
        "v6_modeling_spec_contract": "docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md",
        "reason": (
            "用户复审判定当前九头蛇设计已畸形；旧概念图和 Blockbench 预览没有建立正确的"
            "头-颈-肩-身体连接关系，不能继续作为建模依据。"
        ),
    },
    "pegasus": {
        "status": "design_rejected_requires_v6_modeling_spec_approval",
        "rejection_report": "docs/美术资源/greek_creatures/design_review/pegasus_hydra_design_rejection_2026-06-15.md",
        "v6_modeling_spec_contract": "docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md",
        "reason": (
            "用户复审判定当前天马设计已畸形；旧概念图和 Blockbench 预览是竖直塔状头颈，"
            "不符合原版马比例和天马飞行坐骑设计。"
        ),
    },
}

IMPLEMENTED_REVISIONS = {
    "cyclops": [
        "已改为巨大人形比例，保留单眼、面部、手指脚趾、胸腰结构。",
        "已将武器改为右侧/肩侧巨大石棒，含握柄、不规则石块头、裂纹和凸块。",
    ],
}

PENDING_REVIEW_NOTE = "当前只通过自动 Blockbench/UV/token 验证，仍需要用户或美术负责人对三视图做最终形态确认。"


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def row_bounds(row: list[Any]) -> tuple[float, float, float, float, float, float]:
    _name, _group, frm, to, _uv = row
    return (
        float(frm[0]), float(frm[1]), float(frm[2]),
        float(to[0]), float(to[1]), float(to[2]),
    )


def extent(rows: list[list[Any]], names: set[str]) -> dict[str, float]:
    selected = [row_bounds(row) for row in rows if row[0] in names]
    if not selected:
        return {"width": 0.0, "height": 0.0, "depth": 0.0}
    min_x = min(item[0] for item in selected)
    min_y = min(item[1] for item in selected)
    min_z = min(item[2] for item in selected)
    max_x = max(item[3] for item in selected)
    max_y = max(item[4] for item in selected)
    max_z = max(item[5] for item in selected)
    return {
        "width": max_x - min_x,
        "height": max_y - min_y,
        "depth": max_z - min_z,
    }


def part_names(creature: dict[str, Any]) -> set[str]:
    return {row[0] for row in creature.get("geometry", [])}


def bounds_by_name(rows: list[list[Any]]) -> dict[str, tuple[float, float, float, float, float, float]]:
    return {row[0]: row_bounds(row) for row in rows}


def boxes_touch_or_overlap(
    a: tuple[float, float, float, float, float, float],
    b: tuple[float, float, float, float, float, float],
    tolerance: float = 0.25,
) -> bool:
    return (
        a[0] <= b[3] + tolerance and a[3] + tolerance >= b[0]
        and a[1] <= b[4] + tolerance and a[4] + tolerance >= b[1]
        and a[2] <= b[5] + tolerance and a[5] + tolerance >= b[2]
    )


def check_required_parts(creature: dict[str, Any], required: list[str]) -> tuple[bool, list[str]]:
    names = part_names(creature)
    missing = [name for name in required if name not in names]
    return not missing, missing


def mechanical_shape_checks(creature: dict[str, Any]) -> dict[str, Any]:
    cid = creature["id"]
    rows = creature.get("geometry", [])
    names = part_names(creature)
    checks: list[dict[str, Any]] = []

    def add(name: str, ok: bool, detail: dict[str, Any] | None = None) -> None:
        checks.append({"name": name, "ok": ok, "detail": detail or {}})

    if cid == "hydra":
        v3_head_names = {f"head_{index:02d}_core" for index in range(1, 10)}
        v3_socket_names = {f"neck_socket_{index:02d}" for index in range(1, 10)}
        v3_neck_roots = {f"head_{index:02d}_neck_root" for index in range(1, 10)}
        v3_neck_mids = {f"head_{index:02d}_neck_mid" for index in range(1, 10)}
        v3_neck_tips = {f"head_{index:02d}_neck_tip" for index in range(1, 10)}
        v3_head_extent = extent(rows, v3_head_names)
        center_head_extent = extent(rows, {"head_01_core"})
        body_extent = extent(rows, {"body"})
        shoulder_extent = extent(rows, {"shoulder_saddle"})
        bounds = bounds_by_name(rows)
        ok, missing = check_required_parts(creature, sorted(v3_head_names))
        add("v3_nine_named_proportional_heads", ok, {
            "missing": missing,
            "head_extent": v3_head_extent,
            "note": "Current V2 five-head assets cannot satisfy the V3 Hydra design contract.",
        })
        ok, missing = check_required_parts(creature, sorted(v3_socket_names | v3_neck_roots | v3_neck_mids | v3_neck_tips | {"shoulder_saddle"}))
        add("v3_nine_sockets_and_three_segment_necks_present", ok, {"missing": missing})
        root_contact_failures = []
        shoulder = bounds.get("shoulder_saddle")
        for name in sorted(v3_neck_roots):
            if shoulder is None or name not in bounds or not boxes_touch_or_overlap(bounds[name], shoulder, tolerance=0.35):
                root_contact_failures.append(name)
        add("v3_neck_roots_touch_shoulder_saddle", not root_contact_failures, {
            "failed_roots": root_contact_failures,
            "rule": "Every V3 hydra neck_root must touch or overlap the shoulder_saddle within tolerance.",
        })
        neck_segment_failures = []
        for index in range(1, 10):
            root_name = f"head_{index:02d}_neck_root"
            mid_name = f"head_{index:02d}_neck_mid"
            tip_name = f"head_{index:02d}_neck_tip"
            head_name = f"head_{index:02d}_core"
            for first, second in ((root_name, mid_name), (mid_name, tip_name), (tip_name, head_name)):
                if first not in bounds or second not in bounds or not boxes_touch_or_overlap(bounds[first], bounds[second], tolerance=0.35):
                    neck_segment_failures.append({"from": first, "to": second})
        add("v3_neck_segments_touch_matching_heads", not neck_segment_failures, {
            "failed_pairs": neck_segment_failures,
        })
        add("v3_head_fan_wider_than_body", v3_head_extent["width"] >= max(1.0, body_extent["width"] * 2.2), {
            "head_fan_width": v3_head_extent["width"],
            "body_width": body_extent["width"],
        })
        add("center_head_not_oversized", center_head_extent["width"] <= max(1.0, body_extent["depth"] * 0.38)
            and center_head_extent["height"] <= max(1.0, body_extent["depth"] * 0.38), {
            "center_head": center_head_extent,
            "body": body_extent,
            "max_ratio_to_body_depth": 0.38,
        })
        add("low_slender_body", body_extent["height"] <= 7.0 and body_extent["depth"] >= 25.0, {
            "body": body_extent,
            "shoulder_saddle": shoulder_extent,
            "note": "Shoulder saddle is the neck-root platform and is not counted as body bulk.",
        })
        required_face = []
        for index in range(1, 10):
            required_face.extend([
                f"head_{index:02d}_left_eye",
                f"head_{index:02d}_right_eye",
                f"head_{index:02d}_upper_jaw",
                f"head_{index:02d}_lower_jaw",
                f"head_{index:02d}_left_fang",
                f"head_{index:02d}_right_fang",
            ])
        ok, missing = check_required_parts(creature, required_face)
        add("v3_eyes_jaws_and_fangs_on_all_nine_heads", ok, {"missing": missing})

    elif cid == "cyclops":
        head_extent = extent(rows, {"head"})
        eye_extent = extent(rows, {"central_eye"})
        club_extent = extent(rows, {"stone_club_head", "stone_club_lump_top", "stone_club_lump_side", "stone_club_lump_back"})
        ok, missing = check_required_parts(creature, [
            "central_eye", "central_pupil", "nose_bridge", "left_nostril", "right_nostril",
            "mouth", "left_tooth", "right_tooth", "left_ear", "right_ear",
            "stone_club_handle", "stone_club_head", "stone_club_lump_top", "stone_club_lump_side",
            "stone_club_lump_back", "stone_club_knob",
            "left_finger_a", "left_finger_b", "left_finger_c",
            "right_finger_a", "right_finger_b", "right_finger_c",
            "left_toe_a", "left_toe_b", "left_toe_c",
            "right_toe_a", "right_toe_b", "right_toe_c",
        ])
        add("human_giant_face_weapon_digits", ok, {"missing": missing})
        add("single_eye_occupies_face", 0.35 <= (eye_extent["width"] / max(1.0, head_extent["width"])) <= 0.48, {
            "eye_width": eye_extent["width"],
            "head_width": head_extent["width"],
            "ratio": eye_extent["width"] / max(1.0, head_extent["width"]),
        })
        add("giant_stone_club_head", club_extent["height"] >= 14.0 and club_extent["width"] >= 8.0, club_extent)

    elif cid == "pegasus":
        head_extent = extent(rows, {"head", "snout", "nose_mouth", "left_eye", "right_eye", "left_ear", "right_ear"})
        head_core_extent = extent(rows, {"head", "snout"})
        head_main_extent = extent(rows, {"head"})
        wing_extent = extent(rows, {
            "left_wing_primary", "left_wing_secondary", "left_wing_coverts",
            "right_wing_primary", "right_wing_secondary", "right_wing_coverts",
            "left_feather_a", "left_feather_b", "left_feather_c", "left_feather_d", "left_feather_e",
            "right_feather_a", "right_feather_b", "right_feather_c", "right_feather_d", "right_feather_e",
        })
        body_extent = extent(rows, {"body", "chest"})
        ok, missing = check_required_parts(creature, [
            "head", "snout", "nose_mouth", "left_eye", "right_eye", "left_ear", "right_ear",
            "neck", "neck_front_plane", "mane", "mane_crest", "saddle_space",
            "left_wing_shoulder", "left_wing_primary", "left_wing_secondary", "left_wing_coverts",
            "right_wing_shoulder", "right_wing_primary", "right_wing_secondary", "right_wing_coverts",
            "left_feather_a", "left_feather_b", "left_feather_c", "left_feather_d", "left_feather_e",
            "right_feather_a", "right_feather_b", "right_feather_c", "right_feather_d", "right_feather_e",
        ])
        add("readable_horse_head_neck_wings", ok, {"missing": missing})
        add("horse_head_proportionate_not_oversized", 0.45 <= (head_core_extent["height"] / max(1.0, body_extent["height"])) <= 1.45, {
            "head_core_height": head_core_extent["height"],
            "head_with_ears_and_face": head_extent,
            "body_height": body_extent["height"],
            "ratio": head_core_extent["height"] / max(1.0, body_extent["height"]),
        })
        add("horse_head_width_matches_body_ratio", head_main_extent["width"] <= body_extent["width"] * 0.52
            and head_core_extent["width"] <= body_extent["width"] * 0.58, {
            "head_main_width": head_main_extent["width"],
            "head_core_width": head_core_extent["width"],
            "body_width": body_extent["width"],
            "max_main_ratio": 0.52,
            "max_core_ratio": 0.58,
            "note": "Pegasus should read like a horse: details come from eyes, ears, nostrils, mouth, mane and snout, not a toy-like oversized head block.",
        })
        add("horse_head_depth_is_long_not_bulky", head_core_extent["depth"] <= body_extent["depth"] * 0.58, {
            "head_core_depth": head_core_extent["depth"],
            "body_depth": body_extent["depth"],
            "max_ratio": 0.58,
        })
        add("wide_layered_wings", wing_extent["width"] >= body_extent["width"] * 4.5, {
            "wing_width": wing_extent["width"],
            "body_width": body_extent["width"],
        })

    elif cid == "minotaur":
        head_extent = extent(rows, {"head", "muzzle", "left_horn", "right_horn"})
        body_extent = extent(rows, {"torso"})
        weapon_extent = extent(rows, {"labrys_handle", "labrys_left_blade", "labrys_right_blade", "labrys_center_socket", "labrys_counterweight"})
        ok, missing = check_required_parts(creature, [
            "head", "muzzle", "heavy_brow", "left_eye", "right_eye", "left_nostril", "right_nostril", "mouth",
            "left_horn", "right_horn", "left_ear", "right_ear",
            "left_finger_a", "left_finger_b", "right_finger_a", "right_finger_b",
            "left_hoof", "right_hoof", "left_hoof_claw", "right_hoof_claw",
            "labrys_handle", "labrys_left_blade", "labrys_right_blade", "labrys_center_socket", "labrys_counterweight",
        ])
        add("bull_head_face_hooves_and_labrys_parts", ok, {"missing": missing})
        add("horned_head_wider_than_torso", head_extent["width"] >= body_extent["width"] * 1.25, {
            "head_width": head_extent["width"],
            "torso_width": body_extent["width"],
        })
        add("labrys_reads_as_large_double_axe", weapon_extent["height"] >= body_extent["height"] * 1.5 and weapon_extent["width"] >= 10.0, {
            "weapon": weapon_extent,
            "torso": body_extent,
        })

    elif cid == "centaur":
        horse_extent = extent(rows, {"horse_body", "horse_chest", "horse_rump"})
        human_extent = extent(rows, {"human_torso", "human_shoulders", "head"})
        bow_extent = extent(rows, {"bow_upper", "bow_lower", "bow_grip", "bow_string", "arrow", "arrow_tip"})
        ok, missing = check_required_parts(creature, [
            "horse_body", "horse_chest", "horse_rump", "human_waist", "human_torso", "human_shoulders",
            "head", "left_eye", "right_eye", "nose_bridge", "mouth", "left_ear", "right_ear",
            "bow_upper", "bow_lower", "bow_grip", "bow_string", "arrow", "arrow_tip",
            "quiver", "quiver_strap", "quiver_arrow_a", "quiver_arrow_b",
            "front_left_leg", "front_left_hoof", "front_right_leg", "front_right_hoof",
            "back_left_leg", "back_left_hoof", "back_right_leg", "back_right_hoof",
        ])
        add("human_horse_archer_parts", ok, {"missing": missing})
        add("horse_body_longer_than_human_torso", horse_extent["depth"] >= human_extent["height"] * 1.4, {
            "horse": horse_extent,
            "human": human_extent,
        })
        add("bow_is_tall_readable_archer_signal", bow_extent["height"] >= 20.0, bow_extent)

    elif cid == "medusa":
        snake_heads = {
            "front_snake_head", "left_snake_head", "right_snake_head",
            "back_snake_head", "left_outer_snake_head", "right_outer_snake_head",
        }
        snake_face = [
            "front_snake_left_eye", "front_snake_right_eye", "front_snake_left_fang", "front_snake_right_fang", "front_snake_tongue",
            "left_snake_left_eye", "left_snake_right_eye", "left_snake_left_fang", "left_snake_right_fang", "left_snake_tongue",
            "right_snake_left_eye", "right_snake_right_eye", "right_snake_left_fang", "right_snake_right_fang", "right_snake_tongue",
            "back_snake_left_eye", "back_snake_right_eye", "back_snake_left_fang", "back_snake_right_fang", "back_snake_tongue",
            "left_outer_snake_left_eye", "left_outer_snake_right_eye", "left_outer_snake_left_fang", "left_outer_snake_right_fang", "left_outer_snake_tongue",
            "right_outer_snake_left_eye", "right_outer_snake_right_eye", "right_outer_snake_left_fang", "right_outer_snake_right_fang", "right_outer_snake_tongue",
        ]
        tail_extent = extent(rows, {"tail_base", "tail_mid", "tail_coil_outer", "tail_coil_inner", "tail_tip", "tail_tip_spike"})
        torso_extent = extent(rows, {"torso"})
        ok, missing = check_required_parts(creature, [
            "face_plate", "left_eye", "right_eye", "left_pupil", "right_pupil", "stone_gaze_aura", "nose_bridge", "mouth",
            *sorted(snake_heads),
            *snake_face,
            "tail_base", "tail_mid", "tail_coil_outer", "tail_coil_inner", "tail_tip", "tail_tip_spike",
        ])
        add("stone_gaze_six_snake_hair_and_serpent_tail_parts", ok, {"missing": missing})
        add("six_named_snake_heads", len(snake_heads.intersection(names)) == 6, {
            "present": sorted(snake_heads.intersection(names)),
        })
        add("serpent_tail_taller_than_torso", tail_extent["height"] >= torso_extent["height"] * 2.0, {
            "tail": tail_extent,
            "torso": torso_extent,
        })

    elif cid == "nemean_lion":
        mane_extent = extent(rows, {"mane_core", "mane_top", "mane_bottom", "mane_left", "mane_right", "mane_outer_top", "mane_outer_left", "mane_outer_right", "mane_chin"})
        head_extent = extent(rows, {"head", "snout"})
        body_extent = extent(rows, {"body", "shoulder", "chest_mass"})
        ok, missing = check_required_parts(creature, [
            "body", "shoulder", "chest_mass", "back_armor_ridge",
            "head", "snout", "nose_mouth", "left_eye", "right_eye", "left_fang", "right_fang",
            "mane_core", "mane_top", "mane_bottom", "mane_left", "mane_right", "mane_outer_top", "mane_outer_left", "mane_outer_right", "mane_chin",
            "front_left_claws", "front_right_claws", "back_left_claws", "back_right_claws", "tail", "tail_tuft_core",
        ])
        add("gold_lion_mane_face_claws_and_tail_parts", ok, {"missing": missing})
        add("oversized_mane_frames_head", mane_extent["width"] >= head_extent["width"] * 2.0 and mane_extent["height"] >= head_extent["height"] * 2.0, {
            "mane": mane_extent,
            "head": head_extent,
        })
        add("lion_body_longer_than_tall", body_extent["depth"] >= body_extent["height"] * 1.5, body_extent)

    elif cid == "manticore":
        wing_extent = extent(rows, {
            "left_wing_membrane_a", "left_wing_membrane_b", "left_wing_bone_front", "left_wing_bone_mid", "left_wing_bone_outer", "left_wing_claw",
            "right_wing_membrane_a", "right_wing_membrane_b", "right_wing_bone_front", "right_wing_bone_mid", "right_wing_bone_outer", "right_wing_claw",
        })
        body_extent = extent(rows, {"body", "chest"})
        tail_extent = extent(rows, {"tail_base", "tail_rise", "tail_curve", "tail_hook", "tail_sting", "tail_sting_tip"})
        ok, missing = check_required_parts(creature, [
            "head", "snout", "nose_mouth", "left_eye", "right_eye", "left_fang", "right_fang",
            "left_cheek_spike", "right_cheek_spike",
            "left_wing_membrane_a", "left_wing_membrane_b", "left_wing_bone_front", "left_wing_bone_mid", "left_wing_bone_outer", "left_wing_claw",
            "right_wing_membrane_a", "right_wing_membrane_b", "right_wing_bone_front", "right_wing_bone_mid", "right_wing_bone_outer", "right_wing_claw",
            "tail_base", "tail_rise", "tail_curve", "tail_hook", "tail_sting", "tail_sting_tip",
            "front_left_claws", "front_right_claws", "back_left_claws", "back_right_claws",
        ])
        add("human_lion_face_bat_wings_scorpion_tail_parts", ok, {"missing": missing})
        add("wide_wing_span", wing_extent["width"] >= body_extent["width"] * 3.0, {
            "wings": wing_extent,
            "body": body_extent,
        })
        add("raised_stinging_tail", tail_extent["height"] >= body_extent["height"] * 1.3, {
            "tail": tail_extent,
            "body": body_extent,
        })

    elif cid == "chimera":
        lion_extent = extent(rows, {"lion_head", "lion_snout", "lion_mane_core", "lion_mane_top", "lion_mane_left", "lion_mane_right"})
        goat_extent = extent(rows, {"goat_neck", "goat_head", "goat_snout", "goat_left_horn", "goat_right_horn", "goat_beard"})
        serpent_extent = extent(rows, {"tail_base", "tail_rise", "tail_tip", "serpent_head", "serpent_upper_jaw", "serpent_lower_jaw"})
        wing_extent = extent(rows, {"left_wing_membrane", "left_wing_bone", "left_wing_claw", "right_wing_membrane", "right_wing_bone", "right_wing_claw"})
        ok, missing = check_required_parts(creature, [
            "lion_head", "lion_mane_core", "lion_snout", "lion_left_eye", "lion_right_eye", "lion_left_fang", "lion_right_fang",
            "goat_neck", "goat_head", "goat_snout", "goat_left_eye", "goat_right_eye", "goat_left_horn", "goat_right_horn", "goat_beard",
            "serpent_head", "serpent_upper_jaw", "serpent_lower_jaw", "serpent_left_eye", "serpent_right_eye", "snake_tongue",
            "left_wing_membrane", "left_wing_bone", "left_wing_claw", "right_wing_membrane", "right_wing_bone", "right_wing_claw",
            "front_left_claws", "front_right_claws", "back_left_claws", "back_right_claws",
        ])
        add("lion_goat_serpent_wings_parts", ok, {"missing": missing})
        add("three_creature_heads_readable", lion_extent["height"] >= 12.0 and goat_extent["height"] >= 10.0 and serpent_extent["depth"] >= 16.0, {
            "lion": lion_extent,
            "goat": goat_extent,
            "serpent": serpent_extent,
        })
        add("wing_span_supports_fire_monster_silhouette", wing_extent["width"] >= 30.0, wing_extent)

    elif cid == "colossus":
        body_extent = extent(rows, {"torso_core", "chest_plate_upper", "chest_plate_lower"})
        head_extent = extent(rows, {"head", "face_mask", "eye_slit", "mouth_groove", "crown_band", "crown_center_spike", "crown_spike_left", "crown_spike_right"})
        limb_extent = extent(rows, {"left_arm", "left_forearm", "right_arm", "right_forearm", "left_leg", "left_shin_plate", "right_leg", "right_shin_plate"})
        ok, missing = check_required_parts(creature, [
            "torso_core", "chest_plate_upper", "chest_plate_lower", "chest_center_gem", "left_chest_seam", "right_chest_seam", "back_plate", "bronze_belt",
            "head", "face_mask", "eye_slit", "mouth_groove", "left_cheek_plate", "right_cheek_plate", "crown_band", "crown_center_spike", "crown_spike_left", "crown_spike_right",
            "left_shoulder_plate", "right_shoulder_plate", "left_elbow_ring", "right_elbow_ring", "left_knee_ring", "right_knee_ring",
            "left_finger_a", "left_finger_b", "right_finger_a", "right_finger_b", "left_toe_plate", "right_toe_plate",
        ])
        add("bronze_plate_face_gem_crown_limb_parts", ok, {"missing": missing})
        add("colossus_torso_larger_than_head", body_extent["height"] >= head_extent["height"] * 1.4 and body_extent["width"] >= head_extent["width"] * 1.6, {
            "body": body_extent,
            "head": head_extent,
        })
        add("monumental_limb_span", limb_extent["height"] >= 25.0 and limb_extent["width"] >= body_extent["width"] * 0.9, {
            "limbs": limb_extent,
            "body": body_extent,
        })

    else:
        add("automatic_shape_metrics_not_defined", False, {
            "note": "Creature-specific shape metrics must be defined before this creature can pass visual acceptance.",
        })

    return {
        "mechanical_contract_ok": all(item["ok"] for item in checks),
        "checks": checks,
    }


def build_report() -> dict[str, Any]:
    visual_report = load_json(VISUAL_REPORT_PATH, {})
    texture_fit_report = load_json(TEXTURE_FIT_REPORT_PATH, {})
    visual_by_id = {item.get("id"): item for item in visual_report.get("creatures", []) if isinstance(item, dict)}
    fit_by_id = {item.get("id"): item for item in texture_fit_report.get("creatures", []) if isinstance(item, dict)}

    creatures: list[dict[str, Any]] = []
    failures: list[str] = []
    design_rejected: list[str] = []
    shape_revision_required: list[str] = []
    revision_pending_user_acceptance: list[str] = []
    pending_human_review: list[str] = []

    for creature in CREATURES:
        cid = creature["id"]
        visual = visual_by_id.get(cid, {})
        fit = fit_by_id.get(cid, {})
        metrics = mechanical_shape_checks(creature)
        blockbench_ready = all(
            (ROOT / path).exists()
            for path in visual.get("artifacts", {}).get("blockbench_previews", {}).values()
        )
        texture_ok = bool(fit.get("ok", False))
        user_feedback = USER_FEEDBACK.get(cid, [])
        design_rejection = DESIGN_REJECTIONS.get(cid)
        revision_notes = IMPLEMENTED_REVISIONS.get(cid, [])
        if design_rejection:
            shape_status = "design_rejected_requires_v6_modeling_spec_approval"
        elif user_feedback and revision_notes and metrics["mechanical_contract_ok"] and blockbench_ready and texture_ok:
            shape_status = "revision_implemented_pending_user_acceptance"
        elif user_feedback:
            shape_status = "requires_shape_revision"
        else:
            shape_status = "pending_human_shape_review"
        if shape_status == "design_rejected_requires_v6_modeling_spec_approval":
            design_rejected.append(cid)
        elif shape_status == "requires_shape_revision":
            shape_revision_required.append(cid)
        elif shape_status == "revision_implemented_pending_user_acceptance":
            revision_pending_user_acceptance.append(cid)
        else:
            pending_human_review.append(cid)

        if not blockbench_ready:
            failures.append(f"{cid}: missing Blockbench previews")
        if not texture_ok:
            failures.append(f"{cid}: texture fit failed")
        if not metrics["mechanical_contract_ok"]:
            failures.append(f"{cid}: mechanical shape contract failed")

        creatures.append({
            "id": cid,
            "entity_id": creature["entity_id"],
            "shape_status": shape_status,
            "user_feedback": user_feedback,
            "design_rejection": design_rejection or {},
            "implemented_revision": revision_notes,
            "mechanical_contract_ok": metrics["mechanical_contract_ok"],
            "mechanical_checks": metrics["checks"],
            "blockbench_ready": blockbench_ready,
            "texture_fit_ok": texture_ok,
            "next_action": (
                "await_v6_modeling_spec_approval_before_modeling"
                if shape_status == "design_rejected_requires_v6_modeling_spec_approval"
                else "revise_design_and_model_then_regenerate_previews"
                if shape_status == "requires_shape_revision"
                else "await_user_acceptance_of_revision"
                if shape_status == "revision_implemented_pending_user_acceptance"
                else "await_human_shape_acceptance"
            ),
        })

    mechanical_gate_ok = not failures
    shape_acceptance_ok = (
        not design_rejected
        and not shape_revision_required
        and not revision_pending_user_acceptance
        and not pending_human_review
    )
    return {
        "version": 1,
        "scope": "greek_creature_shape_acceptance",
        "mechanical_gate_ok": mechanical_gate_ok,
        "shape_acceptance_ok": shape_acceptance_ok,
        "ok": mechanical_gate_ok and shape_acceptance_ok,
        "note": PENDING_REVIEW_NOTE,
        "source_visual_report": project_path(VISUAL_REPORT_PATH),
        "source_texture_fit_report": project_path(TEXTURE_FIT_REPORT_PATH),
        "design_rejected_count": len(design_rejected),
        "design_rejected": design_rejected,
        "shape_revision_required_count": len(shape_revision_required),
        "shape_revision_required": shape_revision_required,
        "revision_pending_user_acceptance_count": len(revision_pending_user_acceptance),
        "revision_pending_user_acceptance": revision_pending_user_acceptance,
        "pending_human_review_count": len(pending_human_review),
        "pending_human_review": pending_human_review,
        "failures": failures,
        "creatures": creatures,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_report()
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        mechanical = "PASS" if payload["mechanical_gate_ok"] else "FAIL"
        acceptance = "PASS" if payload["shape_acceptance_ok"] else "PENDING"
        print(f"Greek Visual Acceptance Gate: mechanical={mechanical}, shape_acceptance={acceptance}")
        print(f"report: {REPORT_PATH}")
        print(f"design_rejected: {payload['design_rejected_count']}")
        print(f"shape_revision_required: {payload['shape_revision_required_count']}")
        print(f"pending_human_review: {payload['pending_human_review_count']}")
        if payload["failures"]:
            for failure in payload["failures"]:
                print(f"failure: {failure}", file=sys.stderr)
            return 1
        if payload["design_rejected_count"] > 0:
            for cid in payload["design_rejected"]:
                print(f"design rejected: {cid}", file=sys.stderr)
            return 1
        return 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
