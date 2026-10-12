#!/usr/bin/env python3
"""Audit Chinese creature Java runtime model contracts.

The visual pipeline proves design/UV/texture artifacts exist. This gate keeps
the runtime Java models aligned with the Chinese V2 silhouette contracts.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "src/main/java/com/example/mythic_age/client/render/entity/model"
REPORT_PATH = ROOT / "docs/preview_assets/chinese_creatures/runtime_model_quality_report.json"


CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "azure_dragon",
        "model": "AzureDragonModel.java",
        "required_parts": [
            "head", "snout", "left_eye", "right_eye", "mouth_line", "left_brow", "right_brow",
            "left_horn", "right_horn", "left_horn_tip", "right_horn_tip",
            "left_whisker", "right_whisker", "left_whisker_tip", "right_whisker_tip",
            "body_1", "body_1_belly_scale", "body_2", "body_2_belly_scale",
            "body_3", "body_3_belly_scale", "body_4", "body_4_belly_scale",
            "tail", "tail_fin", "tail_fin_left", "tail_fin_right", "tail_tip",
            "dorsal_fin", "dorsal_fin_front", "dorsal_fin_mid", "dorsal_fin_back",
            "left_front_claw", "left_front_talon", "right_front_claw", "right_front_talon",
            "left_back_claw", "left_back_talon", "right_back_claw", "right_back_talon",
        ],
        "required_tokens": [
            ".uv(36, 0).cuboid",  # snout
            ".uv(62, 0).cuboid",  # horn
            ".uv(80, 0).cuboid",  # whisker
            ".uv(116, 8).cuboid",  # mouth line
            ".uv(0, 28).cuboid",  # first body segment
            ".uv(0, 54).cuboid",  # belly scale
            ".uv(0, 78).cuboid",  # dorsal fin
            ".uv(0, 92).cuboid",  # dorsal fin front
            ".uv(62, 66).cuboid",  # talon
            ".uv(58, 92).cuboid",  # tail fin left
            ".uv(90, 78).cuboid",  # tail tip
            "entity.isFlying()",
        ],
        "reason": "dragon head must stay proportionate; eyes, brow, mouth, horns, whiskers, belly scales, talons, tail fin, dorsal fins, segmented body, and flight wave must provide the detail instead of an oversized head",
    },
    {
        "id": "qilin_guardian",
        "model": "QilinGuardianModel.java",
        "required_parts": [
            "head", "snout", "left_eye", "right_eye", "mouth_line",
            "left_antler", "left_antler_branch_a", "left_antler_branch_b", "left_antler_tip",
            "right_antler", "right_antler_branch_a", "right_antler_branch_b", "right_antler_tip",
            "scaled_body", "scale_plate", "scale_plate_front", "scale_plate_mid", "scale_plate_back",
            "left_side_scale_front", "right_side_scale_front", "left_side_scale_mid", "right_side_scale_mid",
            "left_side_scale_back", "right_side_scale_back", "chest_fur",
            "mane", "mane_front", "mane_mid", "mane_back",
            "left_front_leg", "front_left_hoof", "right_front_leg", "front_right_hoof",
            "left_back_leg", "back_left_hoof", "right_back_leg", "back_right_hoof",
            "tail", "tail_plume", "tail_plume_upper", "tail_plume_lower",
        ],
        "required_tokens": [
            ".uv(32, 0).cuboid",  # snout
            ".uv(76, 0).cuboid",  # left antler
            ".uv(82, 0).cuboid",  # antler branch
            ".uv(54, 28).cuboid",  # scale plate
            ".uv(78, 28).cuboid",  # middle scale plate
            ".uv(70, 54).cuboid",  # mane
            ".uv(10, 76).cuboid",  # front hoof
            ".uv(112, 42).cuboid",  # chest fur
            ".uv(22, 98).cuboid",  # tail plume
            ".uv(54, 98).cuboid",  # lower plume
        ],
        "reason": "qilin head must stay horse/deer proportionate; identity must come from branched antlers, eyes, mouth, mane, red-gold scales, hooves, chest fur, and tail plume",
    },
    {
        "id": "jiangshi",
        "model": "JiangshiModel.java",
        "required_parts": [
            "head", "pale_face", "left_eye", "right_eye", "mouth_line",
            "official_hat", "hat_crown", "hat_tail", "left_hat_wing",
            "right_hat_wing", "talisman", "talisman_mark_top",
            "talisman_mark_mid", "talisman_mark_low", "body", "robe_collar",
            "robe", "robe_lower_hem", "robe_front_panel", "left_stiff_arm",
            "left_cuff", "right_stiff_arm", "right_cuff", "left_leg",
            "left_boot", "right_leg", "right_boot",
        ],
        "required_tokens": [
            ".uv(24, 0).cuboid",  # pale face plane
            ".uv(32, 0).cuboid",  # readable eye
            ".uv(44, 0).cuboid",  # mouth line
            ".uv(0, 18).cuboid",  # official hat brim
            ".uv(40, 18).cuboid",  # hat crown
            ".uv(72, 18).cuboid",  # hat tail
            ".uv(90, 18).cuboid",  # talisman paper
            ".uv(100, 18).cuboid",  # talisman red mark
            ".uv(58, 40).cuboid",  # robe shell
            ".uv(92, 40).cuboid",  # robe hem
            ".uv(12, 70).cuboid",  # stiff arm
            ".uv(38, 70).cuboid",  # cuff
            ".uv(42, 92).cuboid",  # boot
            "-1.25F",  # forward stiff-arm pose
        ],
        "reason": "official hat with wings/tail, pale face, talisman paper and red marks, layered robe, cuffs, boots, and stiff forward arms must prevent a generic zombie read",
    },
    {
        "id": "nine_tailed_fox",
        "model": "NineTailedFoxModel.java",
        "required_parts": [
            "fox_body", "shoulder_fur", "chest_fur", "chest_fur_point",
            "head", "snout", "left_eye", "right_eye", "nose", "mouth_line",
            "left_ear", "left_ear_tip", "right_ear", "right_ear_tip",
            "front_left_leg", "front_left_paw", "front_right_leg", "front_right_paw",
            "back_left_leg", "back_left_paw", "back_right_leg", "back_right_paw",
            "tail_1", "tail_2", "tail_3", "tail_4", "tail_5",
            "tail_6", "tail_7", "tail_8", "tail_9",
            "tail_1_tip", "tail_2_tip", "tail_3_tip", "tail_4_tip", "tail_5_tip",
            "tail_6_tip", "tail_7_tip", "tail_8_tip", "tail_9_tip",
        ],
        "required_tokens": [
            "private final ModelPart[] tails = new ModelPart[9]",
            ".uv(30, 0).cuboid",  # fox snout
            ".uv(52, 0).cuboid",  # red eye
            ".uv(64, 0).cuboid",  # nose
            ".uv(82, 0).cuboid",  # ear
            ".uv(74, 30).cuboid",  # chest fur
            ".uv(92, 30).cuboid",  # chest fur point
            "\"tail_9_tip\"",
            "BASE_TAIL_YAW",
            "addTail(root, \"tail_9\"",
        ],
        "reason": "fox face with eyes/nose/mouth, ears, chest fur, paws, nine independently named tails, and nine tail tips must prove the nine-tailed silhouette",
    },
    {
        "id": "stone_lion_guardian",
        "model": "StoneLionGuardianModel.java",
        "required_parts": [
            "stone_body", "shoulder_plate", "stone_plinth",
            "head", "snout", "left_eye", "right_eye", "nose", "left_fang", "right_fang",
            "curled_mane", "mane_left", "mane_right", "mane_bottom", "mane_top_curl",
            "mane_left_curl_a", "mane_right_curl_a", "mane_left_curl_b", "mane_right_curl_b",
            "embroidered_ball", "ball_horizontal_ribbon", "ball_vertical_ribbon", "ball_top_knot",
            "left_front_paw", "left_front_paw_pad", "left_front_claw_a", "left_front_claw_b",
            "left_front_claw_c", "right_front_paw", "right_front_paw_pad", "right_front_claw_a",
            "right_front_claw_b", "right_front_claw_c", "left_back_leg", "left_back_paw",
            "right_back_leg", "right_back_paw", "curled_tail", "tail_ball", "tail_curl",
        ],
        "required_tokens": [
            ".uv(0, 34).cuboid",  # stone body
            ".uv(56, 34).cuboid",  # shoulder plate
            ".uv(88, 34).cuboid",  # stone plinth
            ".uv(40, 0).cuboid",  # snout
            ".uv(70, 0).cuboid",  # left gold eye
            ".uv(82, 0).cuboid",  # nose
            ".uv(90, 0).cuboid",  # fang
            ".uv(0, 12).cuboid",  # mane top
            ".uv(44, 14).cuboid",  # side mane
            ".uv(108, 14).cuboid",  # top mane curl
            ".uv(20, 70).cuboid",  # paw pad
            ".uv(44, 70).cuboid",  # front paw claws
            ".uv(0, 102).cuboid",  # embroidered ball core
            ".uv(20, 102).cuboid",  # ball horizontal ribbon
            ".uv(48, 102).cuboid",  # ball vertical ribbon
            ".uv(86, 102).cuboid",  # curled tail
            ".uv(102, 102).cuboid",  # tail ball
            ".uv(86, 114).cuboid",  # tail curl
        ],
        "reason": "gold eyes, snout, fangs, layered curled mane, stone plinth, thick clawed paws, embroidered ball with ribbons, and curled tail must distinguish the guardian lion from a normal lion",
    },
]


def extract_model_names(source: str) -> set[str]:
    return set(re.findall(r'"([a-z0-9_]+)"', source))


def audit() -> int:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    report: dict[str, Any] = {
        "ok": True,
        "scope": "chinese_runtime_java_models",
        "creatures": [],
        "failures": [],
    }

    for contract in CONTRACTS:
        path = MODEL_DIR / contract["model"]
        item: dict[str, Any] = {
            "id": contract["id"],
            "model": str(path.relative_to(ROOT)),
            "reason": contract["reason"],
            "required_parts": contract["required_parts"],
            "required_tokens": contract.get("required_tokens", []),
            "missing_parts": [],
            "missing_tokens": [],
            "ok": True,
        }
        if not path.exists():
            item["ok"] = False
            item["missing_parts"] = list(contract["required_parts"])
            item["missing_tokens"] = list(contract.get("required_tokens", []))
            report["failures"].append(f"{contract['id']}: missing model file {path}")
        else:
            source = path.read_text(encoding="utf-8")
            names = extract_model_names(source)
            missing_parts = [part for part in contract["required_parts"] if part not in names]
            missing_tokens = [token for token in contract.get("required_tokens", []) if token not in source]
            item["runtime_part_names"] = sorted(names)
            item["missing_parts"] = missing_parts
            item["missing_tokens"] = missing_tokens
            if missing_parts or missing_tokens:
                item["ok"] = False
                for part in missing_parts:
                    report["failures"].append(f"{contract['id']}: missing runtime model part {part}")
                for token in missing_tokens:
                    report["failures"].append(f"{contract['id']}: missing runtime model detail token {token}")
        report["creatures"].append(item)

    report["ok"] = not report["failures"]
    REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    if report["ok"]:
        print("Chinese Runtime Model Quality Gate: PASS")
    else:
        print("Chinese Runtime Model Quality Gate: FAIL")
        for failure in report["failures"]:
            print(f"- {failure}")
    print(f"report: {REPORT_PATH}")
    return 0 if report["ok"] else 1


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"], nargs="?", default="audit")
    parser.parse_args()
    return audit()


if __name__ == "__main__":
    sys.exit(main())
