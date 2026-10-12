#!/usr/bin/env python3
"""Audit Egyptian creature Java runtime model contracts.

The visual pipeline proves design/UV/texture artifacts exist. This gate proves
the runtime Java models still expose the named parts that carry the Egyptian
silhouette contracts.
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
REPORT_PATH = ROOT / "docs/preview_assets/egyptian_creatures/runtime_model_quality_report.json"


CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "anubis_warrior",
        "model": "AnubisWarriorModel.java",
        "required_parts": [
            "head", "long_snout", "mouth_line", "left_nostril", "right_nostril",
            "left_ear", "right_ear", "left_eye", "right_eye", "forehead_uraeus",
            "body", "gold_collar", "collar_center_gem", "chest_scarab", "linen_skirt",
            "belt_buckle", "left_arm", "left_bracer", "right_arm", "right_bracer",
            "left_leg", "right_leg", "khopesh", "khopesh_blade", "khopesh_hook",
            "khopesh_counterweight",
        ],
        "required_tokens": [
            ".uv(34, 0).cuboid",  # long snout
            ".uv(58, 0).cuboid",  # left tall ear
            ".uv(66, 0).cuboid",  # right tall ear
            ".uv(76, 0).cuboid",  # eye detail
            ".uv(88, 0).cuboid",  # nostril detail
            ".uv(96, 0).cuboid",  # uraeus brow ornament
            ".uv(28, 24).cuboid",  # gold collar
            ".uv(96, 8).cuboid",  # chest scarab
            ".uv(118, 16).cuboid",  # khopesh hook
            ".uv(118, 22).cuboid",  # khopesh counterweight
        ],
        "reason": "jackal head, long snout/ears, gold collar, limbs, and khopesh must define the Anubis guard silhouette",
    },
    {
        "id": "scarab_guardian",
        "model": "ScarabGuardianModel.java",
        "required_parts": [
            "body", "gold_carapace", "carapace_ridge", "left_shell_plate",
            "right_shell_plate", "rear_shell_plate", "scarab_sigil", "head", "horn",
            "horn_tip", "left_mandible", "right_mandible", "left_eye", "right_eye",
            "left_front_leg", "right_front_leg", "left_middle_leg", "right_middle_leg", "left_back_leg", "right_back_leg",
            "front_left_claw", "front_right_claw", "mid_left_claw", "mid_right_claw",
            "back_left_claw", "back_right_claw",
        ],
        "required_tokens": [
            ".uv(68, 20).cuboid",  # raised carapace
            ".uv(68, 38).cuboid",  # central scarab ridge
            ".uv(82, 38).cuboid",  # shell side plate
            ".uv(98, 54).cuboid",  # scarab sigil
            ".uv(34, 0).cuboid",  # left mandible
            ".uv(54, 0).cuboid",  # left eye detail
            ".uv(78, 0).cuboid",  # horn
            ".uv(0, 74).cuboid",  # claw detail
        ],
        "reason": "low shell, gold/raised carapace, front horn, and six separated legs must distinguish the scarab from a spider",
    },
    {
        "id": "mummy_soldier",
        "model": "MummySoldierModel.java",
        "required_parts": [
            "head", "head_bandage_upper", "head_bandage_mid", "head_bandage_lower",
            "head_bandage_diagonal", "mouth_shadow", "left_cheek_wrap", "right_cheek_wrap",
            "left_eye", "right_eye", "body", "bandage_chest", "bandage_mid", "bandage_low",
            "waist_wrap", "left_arm", "left_arm_bandage_upper", "left_arm_bandage_lower",
            "right_arm", "right_arm_bandage_upper", "right_arm_bandage_lower", "spear",
            "spear_tip", "spear_tassel", "left_leg", "left_leg_bandage", "right_leg",
            "right_leg_bandage",
        ],
        "required_tokens": [
            ".uv(34, 0).cuboid",  # head bandage wrap
            ".uv(34, 10).cuboid",  # second head bandage wrap
            ".uv(34, 16).cuboid",  # mid head bandage
            ".uv(54, 16).cuboid",  # diagonal head bandage
            ".uv(86, 0).cuboid",  # dark eye detail
            ".uv(28, 24).cuboid",  # chest bandage wrap
            ".uv(28, 30).cuboid",  # mid body bandage
            ".uv(92, 16).cuboid",  # waist wrap
            ".uv(60, 36).cuboid",  # arm bandage wrap
            ".uv(102, 0).cuboid",  # spear tip
            ".uv(112, 0).cuboid",  # spear tassel
        ],
        "reason": "wrapped head/body/limbs, dark eyes, and independent spear must carry the mummy soldier identity",
    },
    {
        "id": "sphinx_sentinel",
        "model": "SphinxSentinelModel.java",
        "required_parts": [
            "body", "chest", "shoulder_scarab", "head", "human_face", "headdress",
            "headdress_left_panel", "headdress_right_panel", "headdress_top_band",
            "headdress_brow", "left_eye", "right_eye", "left_pupil", "right_pupil",
            "nose_bridge", "mouth_line", "chin_beard", "tail", "tail_tuft",
            "front_left_leg", "front_right_leg", "back_left_leg", "back_right_leg",
            "front_left_paw", "front_right_paw", "back_left_paw", "back_right_paw",
            "front_left_claws", "front_right_claws",
        ],
        "required_tokens": [
            ".uv(34, 0).cuboid",  # human mouth/nose plane
            ".uv(92, 0).cuboid",  # human eye detail
            ".uv(104, 0).cuboid",  # nose bridge
            ".uv(112, 0).cuboid",  # mouth line
            ".uv(52, 0).cuboid",  # pharaoh headdress top
            ".uv(52, 12).cuboid",  # headdress side panel
            ".uv(116, 0).cuboid",  # pupil detail
            ".uv(116, 8).cuboid",  # chin beard
            ".uv(0, 82).cuboid",  # claw detail
            ".uv(76, 60).cuboid",  # heavy chest/front body
        ],
        "reason": "human face, pharaoh headdress, long lion body, paws, and tail must carry the sphinx silhouette",
    },
    {
        "id": "sun_falcon",
        "model": "SunFalconModel.java",
        "required_parts": [
            "head", "beak", "beak_tip", "left_eye", "right_eye", "left_brow",
            "right_brow", "sun_disk", "sun_disk_top_ray", "sun_disk_bottom_ray",
            "sun_disk_left_ray", "sun_disk_right_ray", "body", "belly",
            "left_wing", "left_primary_feather_a", "left_primary_feather_b",
            "left_primary_feather_c", "right_wing", "right_primary_feather_a",
            "right_primary_feather_b", "right_primary_feather_c", "left_talon",
            "right_talon", "tail", "tail_tip",
        ],
        "required_tokens": [
            ".uv(26, 0).cuboid",  # beak
            ".uv(40, 0).cuboid",  # beak tip
            ".uv(88, 0).cuboid",  # eye detail
            ".uv(44, 0).cuboid",  # sun disk
            ".uv(66, 0).cuboid",  # sun disk ray
            ".uv(88, 8).cuboid",  # horizontal sun ray
            ".uv(0, 60).cuboid",  # left feather strip
            ".uv(38, 60).mirrored().cuboid",  # right feather strip
            ".uv(78, 62).cuboid",  # talon
            ".uv(92, 34).cuboid",  # tail tip
        ],
        "reason": "sun disk, falcon beak/eyes, segmented wings, and tail must prove this is a solar flying creature",
    },
]


def extract_model_names(source: str) -> set[str]:
    return set(re.findall(r'"([a-z0-9_]+)"', source))


def audit() -> int:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    report: dict[str, Any] = {
        "ok": True,
        "scope": "egyptian_runtime_java_models",
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
        print("Egyptian Runtime Model Quality Gate: PASS")
    else:
        print("Egyptian Runtime Model Quality Gate: FAIL")
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
