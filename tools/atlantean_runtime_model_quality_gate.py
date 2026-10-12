#!/usr/bin/env python3
"""Audit Atlantean creature Java runtime model contracts.

The visual pipeline proves design/UV/texture artifacts exist. This gate keeps
the runtime Java models aligned with the Atlantean V2 silhouette contracts.
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
REPORT_PATH = ROOT / "docs/preview_assets/atlantean_creatures/runtime_model_quality_report.json"


CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "crystal_leviathan",
        "model": "CrystalLeviathanModel.java",
        "required_parts": [
            "head", "snout", "lower_jaw", "left_prism_eye", "right_prism_eye",
            "head_crown_crystal", "mouth_line", "left_gill_slits", "right_gill_slits",
            "left_mandible_fin", "right_mandible_fin", "body_1", "body_1_glow_scale",
            "body_2", "body_2_glow_scale", "body_3", "body_3_glow_scale",
            "body_4", "body_4_glow_scale", "dorsal_crystal_1",
            "dorsal_crystal_2", "dorsal_crystal_3", "left_side_fin",
            "right_side_fin", "dorsal_crystal_4", "left_side_fin_rib", "right_side_fin_rib",
            "tail", "tail_fan", "tail_fan_left_rib", "tail_fan_right_rib", "tail_tip_crystal",
        ],
        "required_tokens": [
            ".uv(42, 0).cuboid",  # snout
            ".uv(96, 0).cuboid",  # prism eye
            ".uv(116, 8).cuboid",  # head crown crystal
            ".uv(112, 16).cuboid",  # mouth line
            ".uv(0, 96).cuboid",  # glow scale
            ".uv(58, 54).cuboid",  # dorsal crystal
            ".uv(94, 54).cuboid",  # side fin
            ".uv(94, 68).cuboid",  # fin rib
            ".uv(50, 82).cuboid",  # tail fan
            ".uv(88, 96).cuboid",  # tail crystal
        ],
        "reason": "wide sea-beast head, prism eyes, segmented body, dorsal crystals, side fins, and tail fan must prevent a generic serpent read",
    },
    {
        "id": "orichalcum_golem",
        "model": "OrichalcumGolemModel.java",
        "required_parts": [
            "head", "crest_crystal", "body", "chest_core", "upper_torso_rivet_left",
            "upper_torso_rivet_right", "lower_rune_glyph", "core_vertical_glow",
            "left_shoulder_plate", "left_shoulder_rivet", "right_shoulder_plate", "right_shoulder_rivet",
            "left_arm", "left_forearm_plate", "left_fist", "left_fist_knuckle_a", "left_fist_knuckle_b",
            "right_arm", "right_forearm_plate", "right_fist", "right_fist_knuckle_a", "right_fist_knuckle_b",
            "left_leg", "left_knee_plate", "left_heavy_foot", "left_foot_toe_plate",
            "right_leg", "right_knee_plate", "right_heavy_foot", "right_foot_toe_plate",
            "back_crystals",
        ],
        "required_tokens": [
            ".uv(0, 30).cuboid",  # heavy body
            ".uv(42, 54).cuboid",  # enlarged chest core
            ".uv(62, 54).cuboid",  # core frame
            ".uv(42, 0).cuboid",  # eye slit
            ".uv(76, 0).cuboid",  # crest crystal
            ".uv(68, 0).cuboid",  # back crystal array
            ".uv(0, 82).cuboid",  # shoulder plate
            ".uv(0, 100).cuboid",  # forearm plate
            ".uv(64, 100).cuboid",  # giant fist
            ".uv(44, 64).cuboid",  # knee plate
            ".uv(72, 64).cuboid",  # heavy foot
            ".uv(12, 116).cuboid",  # rivets
            ".uv(38, 116).cuboid",  # core vertical glow
            ".uv(64, 116).cuboid",  # fist knuckle
            ".uv(96, 116).cuboid",  # toe plate
        ],
        "reason": "orichalcum armor, framed glowing core, eye slit, crest crystal, shoulder plates, oversized fists, knee armor, heavy feet, and back crystals must distinguish the golem from an iron golem",
    },
    {
        "id": "siren_priestess",
        "model": "SirenPriestessModel.java",
        "required_parts": [
            "head", "left_eye", "right_eye", "mouth_line", "fin_crown", "crown_spire",
            "left_hair_fin", "right_hair_fin", "left_ear_fin", "right_ear_fin",
            "body", "wave_chest_ornament", "waist_shell_belt", "left_arm", "left_wrist_fin",
            "right_arm", "right_wrist_fin", "crystal_staff", "staff_crystal", "staff_top_spire",
            "staff_rune_band", "tail_base", "tail_scale_band_1", "tail_scale_band_2",
            "tail_scale_band_3", "tail_tip", "tail_tip_scale_band", "tail_fin",
            "tail_fin_left_edge", "tail_fin_right_edge",
        ],
        "required_tokens": [
            ".uv(28, 0).cuboid",  # eye
            ".uv(40, 0).cuboid",  # mouth line
            ".uv(42, 0).cuboid",  # fin crown
            ".uv(66, 0).cuboid",  # crown spire base
            ".uv(76, 0).cuboid",  # side hair fin
            ".uv(90, 0).cuboid",  # staff
            ".uv(26, 34).cuboid",  # tail base
            ".uv(20, 54).cuboid",  # tail scale band
            ".uv(74, 34).cuboid",  # tail fin
            ".uv(96, 12).cuboid",  # ear fin
            ".uv(112, 12).cuboid",  # wrist fin
            ".uv(90, 22).cuboid",  # staff rune band
            ".uv(58, 54).cuboid",  # third scale band
        ],
        "reason": "face, mouth, fin crown, side hair fins, staff, wave chest ornament, and fish-tail scale bands must prevent a generic witch or humanoid read",
    },
    {
        "id": "tidal_guardian",
        "model": "TidalGuardianModel.java",
        "required_parts": [
            "head", "left_eye", "right_eye", "beak_mouth", "lower_mandible",
            "shell", "shell_plate_front", "shell_plate_left", "shell_plate_right", "shell_plate_back",
            "left_claw", "left_claw_upper_tip", "left_claw_lower_tip",
            "right_claw", "right_claw_upper_tip", "right_claw_lower_tip",
            "front_left_leg", "front_left_foot_claws", "front_right_leg", "front_right_foot_claws",
            "back_left_leg", "back_left_foot_claws", "back_right_leg", "back_right_foot_claws",
            "shell_crystals", "crystal_plate_1", "crystal_plate_2", "crystal_plate_3", "crystal_plate_4",
            "tail_fin", "tail_fin_tip",
        ],
        "required_tokens": [
            ".uv(0, 32).cuboid",  # wide shell
            ".uv(54, 0).cuboid",  # claw
            ".uv(64, 58).cuboid",  # shell crystals
            ".uv(84, 86).cuboid",  # tail fin
            ".uv(28, 0).cuboid",  # eye
            ".uv(42, 4).cuboid",  # beak mouth
            ".uv(54, 16).cuboid",  # lower mandible
            ".uv(22, 92).cuboid",  # claw tip
            ".uv(78, 92).cuboid",  # foot claws
            ".uv(84, 58).cuboid",  # extra crystal plate
            ".uv(106, 86).cuboid",  # tail fin tip
        ],
        "reason": "wide shell, large crab claws, short legs, shell crystals, and tail fin must distinguish the tidal guardian from a turtle",
    },
    {
        "id": "abyssal_ray",
        "model": "AbyssalRayModel.java",
        "required_parts": [
            "ray_body", "back_ridge", "head_lobe", "left_eye", "right_eye", "mouth_slit",
            "left_gill_mark", "right_gill_mark",
            "left_wing", "left_wing_rib_a", "left_wing_rib_b", "left_wing_rib_c",
            "right_wing", "right_wing_rib_a", "right_wing_rib_b", "right_wing_rib_c",
            "left_wing_tip", "right_wing_tip", "tail", "tail_spine", "tail_barb_left", "tail_barb_right",
            "glow_belly", "glow_rune_front", "glow_rune_center", "glow_rune_back",
        ],
        "required_tokens": [
            ".uv(0, 0).cuboid",  # ray body
            ".uv(26, 22).cuboid",  # left eye
            ".uv(32, 22).cuboid",  # right eye
            ".uv(38, 22).cuboid",  # mouth slit
            ".uv(0, 38).cuboid",  # left wing
            ".uv(0, 58).cuboid",  # right wing
            ".uv(48, 78).cuboid",  # tail spine
            ".uv(76, 78).cuboid",  # glowing belly
            ".uv(34, 98).cuboid",  # wing rib
            ".uv(0, 98).cuboid",  # glow rune
            ".uv(58, 78).cuboid",  # tail barb
        ],
        "reason": "flat body, wide wings, wing tips, glowing belly, and long tail spine must prevent a generic fish read",
    },
]


def extract_model_names(source: str) -> set[str]:
    return set(re.findall(r'"([a-z0-9_]+)"', source))


def audit() -> int:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    report: dict[str, Any] = {
        "ok": True,
        "scope": "atlantean_runtime_java_models",
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
        print("Atlantean Runtime Model Quality Gate: PASS")
    else:
        print("Atlantean Runtime Model Quality Gate: FAIL")
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
