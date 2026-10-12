#!/usr/bin/env python3
"""Audit Nordic creature Java runtime model contracts.

The visual pipeline proves the design/UV/texture artifacts exist. This gate
keeps the runtime Java models aligned with the Nordic V2 silhouette contracts.
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
REPORT_PATH = ROOT / "docs/preview_assets/nordic_creatures/runtime_model_quality_report.json"


CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "frost_giant",
        "model": "FrostGiantModel.java",
        "required_parts": [
            "head", "heavy_brow", "left_eye", "right_eye", "nose_bridge", "mouth_line",
            "left_brow_ice", "right_brow_ice", "ice_beard", "left_beard_strand",
            "center_beard_strand", "right_beard_strand", "body", "fur_pelt", "rune_chest",
            "left_shoulder_ice", "right_shoulder_ice", "left_arm", "left_bracer_ice",
            "right_arm", "right_bracer_ice", "frost_club", "frost_club_head",
            "club_crack_a", "club_crack_b", "club_ice_cap", "left_leg", "left_knee_ice",
            "right_leg", "right_knee_ice", "left_boot", "right_boot", "left_boot_spikes",
            "right_boot_spikes", "back_spikes", "back_left_spike", "back_right_spike",
        ],
        "required_tokens": [
            ".uv(44, 0).cuboid",  # heavy brow
            ".uv(62, 0).cuboid",  # glowing eye detail
            ".uv(58, 12).cuboid",  # nose bridge
            ".uv(70, 12).cuboid",  # mouth line
            ".uv(52, 28).cuboid",  # fur pelt
            ".uv(88, 28).cuboid",  # chest rune
            ".uv(88, 58).cuboid",  # frost club head
            ".uv(100, 58).cuboid",  # club crack
            ".uv(0, 108).cuboid",  # shoulder ice
            ".uv(40, 120).cuboid",  # boot spikes
        ],
        "reason": "giant height, ice beard, back spikes, chest rune, and oversized frost club must define the frost giant",
    },
    {
        "id": "draugr_warrior",
        "model": "DraugrWarriorModel.java",
        "required_parts": [
            "head", "left_eye", "right_eye", "mouth_line", "helmet", "helmet_crown",
            "helmet_nose_guard", "left_horn", "right_horn", "torso", "shoulder_armor",
            "rib_marks", "belt_buckle", "ragged_cape", "cape_left_rip", "cape_mid_rip",
            "cape_right_rip", "shield_arm", "left_bracer", "round_shield",
            "shield_inner_ring", "shield_boss", "shield_crack_a", "shield_crack_b",
            "axe_arm", "right_bracer", "battle_axe", "axe_blade", "axe_hook",
            "axe_spike", "left_leg", "left_boot", "right_leg", "right_boot",
        ],
        "required_tokens": [
            ".uv(34, 0).cuboid",  # hollow eye
            ".uv(68, 18).cuboid",  # helmet nose guard
            ".uv(62, 38).cuboid",  # rib marks
            ".uv(112, 50).cuboid",  # belt buckle
            ".uv(74, 38).cuboid",  # ragged cape body
            ".uv(30, 64).cuboid",  # round shield
            ".uv(100, 64).cuboid",  # shield crack
            ".uv(38, 82).cuboid",  # axe blade
            ".uv(68, 82).cuboid",  # axe spike
            ".uv(42, 104).cuboid",  # boot
        ],
        "reason": "helmet crown/nose guard, hollow eyes, rib marks, ragged cape, cracked round shield, bracers, boots, and battle axe sub-shapes must prevent a generic zombie read",
    },
    {
        "id": "valkyrie",
        "model": "ValkyrieModel.java",
        "required_parts": [
            "head", "left_eye", "right_eye", "mouth_line", "winged_helmet", "helmet_crest",
            "left_helmet_wing", "right_helmet_wing", "left_helmet_feather_tip",
            "right_helmet_feather_tip", "armor", "armor_plate", "cape", "cape_left_rip",
            "cape_right_rip", "left_wing", "left_wing_primary_a", "left_wing_primary_b",
            "left_wing_primary_c", "right_wing", "right_wing_primary_a", "right_wing_primary_b",
            "right_wing_primary_c", "left_arm", "left_bracer", "spear_arm", "spear_bracer",
            "spear", "spear_blade", "spear_counterweight", "left_leg", "left_boot",
            "right_leg", "right_boot",
        ],
        "required_tokens": [
            ".uv(44, 16).cuboid",  # helmet wing
            ".uv(0, 58).cuboid",  # wing base
            ".uv(0, 70).cuboid",  # feather strip
            ".uv(54, 88).cuboid",  # spear shaft
            ".uv(62, 88).cuboid",  # spear blade
            ".uv(12, 90).cuboid",  # bracer
            ".uv(112, 36).cuboid",  # cape rip
            "entity.isFlying()",
        ],
        "reason": "winged helmet, segmented wings, cape, spear, and flight animation must define the valkyrie",
    },
    {
        "id": "fenrir_wolf",
        "model": "FenrirWolfModel.java",
        "required_parts": [
            "wolf_body", "back_fur", "back_fur_a", "back_fur_b", "back_fur_c",
            "wolf_head", "left_eye", "right_eye", "left_brow", "right_brow",
            "left_ear", "right_ear", "upper_jaw", "lower_jaw", "left_lower_fang",
            "right_lower_fang", "chain_collar", "chain_link_left", "chain_link_right",
            "chain_link_center", "tail", "tail_tip", "front_left_leg", "front_left_paw",
            "front_left_claws", "front_right_leg", "front_right_paw", "front_right_claws",
            "back_left_leg", "back_left_paw", "back_left_claws", "back_right_leg",
            "back_right_paw", "back_right_claws",
        ],
        "required_tokens": [
            ".uv(44, 0).cuboid",  # red eye/detail
            ".uv(82, 0).cuboid",  # upper jaw
            ".uv(82, 12).cuboid",  # lower jaw
            ".uv(0, 22).cuboid",  # chain collar
            ".uv(32, 22).cuboid",  # chain link
            ".uv(0, 120).cuboid",  # claw strip
            ".uv(0, 108).cuboid",  # tail
            ".uv(28, 108).cuboid",  # tail tip
        ],
        "reason": "giant wolf body, red eye, separated jaws, chain collar, back fur, and tail must distinguish Fenrir from a normal wolf",
    },
    {
        "id": "dwarf_berserker",
        "model": "DwarfBerserkerModel.java",
        "required_parts": [
            "head", "nose", "left_eye", "right_eye", "mouth_line", "helmet",
            "helmet_crown", "helmet_nose_guard", "left_rim", "right_rim",
            "beard", "beard_center", "left_braid", "right_braid", "body",
            "shoulder_armor", "chest_panel", "belt", "belt_buckle",
            "cloak_pack", "cloak_roll", "left_arm", "left_bracer", "right_arm",
            "right_bracer", "left_axe", "left_axe_blade", "left_axe_hook",
            "left_axe_counterweight", "right_axe", "right_axe_blade",
            "right_axe_hook", "right_axe_counterweight", "left_leg", "left_boot",
            "right_leg", "right_boot",
        ],
        "required_tokens": [
            ".uv(9, 15).cuboid",  # readable eye
            ".uv(22, 15).cuboid",  # mouth line
            ".uv(72, 0).cuboid",  # helmet crown
            ".uv(100, 0).cuboid",  # helmet nose guard
            ".uv(66, 10).cuboid",  # beard mass
            ".uv(102, 10).cuboid",  # beard braid
            ".uv(72, 32).cuboid",  # chest panel
            ".uv(34, 46).cuboid",  # left axe
            ".uv(42, 46).cuboid",  # left axe blade
            ".uv(52, 58).cuboid",  # right axe
            ".uv(60, 58).cuboid",  # right axe blade
            ".uv(102, 24).cuboid",  # belt buckle
            ".uv(42, 82).cuboid",  # boot
            "this.leftBraid.roll",
            "this.rightBraid.roll",
        ],
        "reason": "short wide body, readable face, round helmet, large beard with moving braids, belt, boots, and split dual axes must define the dwarf berserker",
    },
]


def extract_model_names(source: str) -> set[str]:
    return set(re.findall(r'"([a-z0-9_]+)"', source))


def audit() -> int:
    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    report: dict[str, Any] = {
        "ok": True,
        "scope": "nordic_runtime_java_models",
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
        print("Nordic Runtime Model Quality Gate: PASS")
    else:
        print("Nordic Runtime Model Quality Gate: FAIL")
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
