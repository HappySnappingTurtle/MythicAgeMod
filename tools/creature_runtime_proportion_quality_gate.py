#!/usr/bin/env python3
"""Audit runtime Java creature model head/body proportions.

The design pipelines and Blockbench assets can pass while an older Java model
still renders with oversized heads in-game. This gate parses named cuboids from
the runtime model source and checks that head readability comes from named
details instead of abnormal head enlargement.
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
REPORT_DIR = ROOT / "docs/preview_assets/creature_visual_acceptance"
REPORT_PATH = REPORT_DIR / "creature_runtime_proportion_quality_report.json"

DETAIL_TOKENS = (
    "eye", "pupil", "mouth", "nose", "nostril", "snout", "muzzle", "beak",
    "jaw", "fang", "tooth", "teeth", "horn", "antler", "ear", "brow",
    "whisker", "mandible", "tongue", "face", "mane", "bandage", "wrap",
    "talisman", "hat", "helmet", "guard", "crest", "rune", "glyph", "core",
    "glow", "rivet", "knuckle", "gill", "slit", "rib", "ridge", "beard",
    "braid", "fin", "wing", "feather", "tail", "shield", "axe", "spear",
    "staff", "club", "claw", "hoof", "paw", "crystal", "scale",
)

VISUAL_HEAD_VOLUME_TOKENS = (
    "face", "snout", "muzzle", "nose", "nostril", "mouth", "jaw",
    "mandible", "beak", "brow", "cheek",
)

VISUAL_HEAD_VOLUME_EXCLUDE_TOKENS = (
    "eye", "pupil", "ear", "horn", "antler", "fang", "tooth", "teeth",
    "tongue", "mane", "hair", "beard", "whisker", "fin", "crest",
    "helmet", "hat", "crown", "headdress", "club", "weapon", "staff",
    "wing", "tail", "claw", "hoof", "paw", "scale", "rune", "glyph",
    "crystal", "core", "glow", "spike", "scar",
)

DETAIL_PRESERVING_RUNTIME_LIMITS = {
    # User feedback 2026-06-15: the runtime model must not use a large head
    # block to compensate for missing face/detail precision. These limits are
    # stricter than some older per-creature contracts and become blockers.
    "humanoid": {
        "max_head_height_to_body_height": 0.70,
        "max_head_width_to_body_width": 0.76,
    },
    "giant_humanoid": {
        "max_head_height_to_body_height": 0.68,
        "max_head_width_to_body_width": 0.74,
    },
    "equine": {
        "max_head_height_to_body_height": 0.63,
        "max_head_width_to_body_width": 0.45,
        "max_head_depth_to_body_depth": 0.34,
        "max_visual_head_width_to_body_width": 0.42,
    },
    "construct": {
        "max_head_height_to_body_height": 0.72,
        "max_head_width_to_body_width": 0.70,
    },
}

CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "minotaur",
        "civilization": "greek",
        "archetype": "giant_humanoid",
        "model": "MinotaurModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.70,
        "max_head_width_to_body_width": 0.80,
        "max_visual_head_height_to_body_height": 0.72,
        "max_visual_head_width_to_body_width": 0.86,
        "required_detail_tokens": ["eye", "muzzle", "nostril", "mouth", "horn", "ear"],
        "min_detail_count": 12,
    },
    {
        "id": "cyclops",
        "civilization": "greek",
        "archetype": "giant_humanoid",
        "model": "CyclopsModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.70,
        "max_head_width_to_body_width": 0.78,
        "max_visual_head_height_to_body_height": 0.72,
        "max_visual_head_width_to_body_width": 0.80,
        "required_detail_tokens": ["central_eye", "pupil", "brow", "mouth", "club"],
        "min_detail_count": 12,
    },
    {
        "id": "centaur",
        "civilization": "greek",
        "archetype": "humanoid",
        "model": "CentaurModel.java",
        "head_parts": ["head"],
        "body_parts": ["horse_body"],
        "max_head_height_to_body_height": 0.90,
        "max_head_width_to_body_width": 0.82,
        "max_visual_head_height_to_body_height": 0.76,
        "max_visual_head_width_to_body_width": 0.58,
        "required_detail_tokens": ["eye", "ear", "mouth", "mane", "hoof", "tail"],
        "min_detail_count": 12,
    },
    {
        "id": "hydra",
        "civilization": "greek",
        "model": "HydraModel.java",
        "design_contract_status": "v3_nine_head_required_before_modeling",
        "required_head_count": 9,
        "required_neck_count": 9,
        "head_parts": [
            "head_01_center", "head_02_inner_left", "head_03_inner_right",
            "head_04_mid_left", "head_05_mid_right",
            "head_06_outer_left", "head_07_outer_right",
            "head_08_rear_left", "head_09_rear_right",
        ],
        "neck_parts": [
            "neck_01_center", "neck_02_inner_left", "neck_03_inner_right",
            "neck_04_mid_left", "neck_05_mid_right",
            "neck_06_outer_left", "neck_07_outer_right",
            "neck_08_rear_left", "neck_09_rear_right",
        ],
        "dynamic_head_factory": "addNeck",
        "dynamic_head_base_extent": {"width": 9.0, "height": 8.5, "depth": 8.8},
        "dynamic_visual_head_base_extent": {"width": 10.4, "height": 12.5, "depth": 13.5},
        "body_extent_mode": "local",
        "connection_parts": [
            "neck_socket_01", "neck_socket_02", "neck_socket_03",
            "neck_socket_04", "neck_socket_05", "neck_socket_06",
            "neck_socket_07", "neck_socket_08", "neck_socket_09",
            "head_01_center_body_overlap", "head_02_inner_left_body_overlap",
            "head_03_inner_right_body_overlap", "head_04_mid_left_body_overlap",
            "head_05_mid_right_body_overlap", "head_06_outer_left_body_overlap",
            "head_07_outer_right_body_overlap", "head_08_rear_left_body_overlap",
            "head_09_rear_right_body_overlap",
        ],
        "body_parts": [
            "body", "slender_body", "chest_keel", "shoulder_saddle",
        ],
        "max_single_head_width_to_body_width": 0.68,
        "max_single_head_depth_to_body_depth": 0.42,
        "max_single_visual_head_width_to_body_width": 0.72,
        "max_single_visual_head_depth_to_body_depth": 0.46,
        "required_detail_tokens": ["neck_root", "neck_mid", "neck_tip", "body_overlap", "head_overlap", "eye", "jaw", "fang", "horn", "poison_gland"],
        "min_detail_count": 30,
        "required_parent_child_links": [
            ["body", "neck_01_center"],
            ["body", "neck_02_inner_left"],
            ["body", "neck_03_inner_right"],
            ["body", "neck_04_mid_left"],
            ["body", "neck_05_mid_right"],
            ["body", "neck_06_outer_left"],
            ["body", "neck_07_outer_right"],
            ["body", "neck_08_rear_left"],
            ["body", "neck_09_rear_right"],
            ["neck_01_center", "head_01_center"],
            ["neck_02_inner_left", "head_02_inner_left"],
            ["neck_03_inner_right", "head_03_inner_right"],
            ["neck_04_mid_left", "head_04_mid_left"],
            ["neck_05_mid_right", "head_05_mid_right"],
            ["neck_06_outer_left", "head_06_outer_left"],
            ["neck_07_outer_right", "head_07_outer_right"],
            ["neck_08_rear_left", "head_08_rear_left"],
            ["neck_09_rear_right", "head_09_rear_right"],
        ],
        "required_overlap_pairs": [
            ["head_01_center_body_overlap", "neck_socket_01"],
            ["head_02_inner_left_body_overlap", "neck_socket_02"],
            ["head_03_inner_right_body_overlap", "neck_socket_03"],
            ["head_04_mid_left_body_overlap", "neck_socket_04"],
            ["head_05_mid_right_body_overlap", "neck_socket_05"],
            ["head_06_outer_left_body_overlap", "neck_socket_06"],
            ["head_07_outer_right_body_overlap", "neck_socket_07"],
            ["head_08_rear_left_body_overlap", "neck_socket_08"],
            ["head_09_rear_right_body_overlap", "neck_socket_09"],
            ["head_01_center_head_overlap", "head_01_center"],
            ["head_02_inner_left_head_overlap", "head_02_inner_left"],
            ["head_03_inner_right_head_overlap", "head_03_inner_right"],
            ["head_04_mid_left_head_overlap", "head_04_mid_left"],
            ["head_05_mid_right_head_overlap", "head_05_mid_right"],
            ["head_06_outer_left_head_overlap", "head_06_outer_left"],
            ["head_07_outer_right_head_overlap", "head_07_outer_right"],
            ["head_08_rear_left_head_overlap", "head_08_rear_left"],
            ["head_09_rear_right_head_overlap", "head_09_rear_right"],
        ],
    },
    {
        "id": "medusa",
        "civilization": "greek",
        "archetype": "humanoid",
        "model": "MedusaModel.java",
        "head_parts": ["head"],
        "body_parts": ["torso"],
        "max_head_height_to_body_height": 0.88,
        "max_head_width_to_body_width": 0.78,
        "max_visual_head_height_to_body_height": 0.88,
        "max_visual_head_width_to_body_width": 0.82,
        "required_detail_tokens": ["eye", "pupil", "mouth", "snake", "fang", "tongue", "tail"],
        "min_detail_count": 18,
    },
    {
        "id": "nemean_lion",
        "civilization": "greek",
        "model": "NemeanLionModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.80,
        "max_head_width_to_body_width": 0.78,
        "max_head_depth_to_body_depth": 0.48,
        "max_visual_head_height_to_body_height": 0.80,
        "max_visual_head_width_to_body_width": 0.80,
        "max_visual_head_depth_to_body_depth": 0.64,
        "required_detail_tokens": ["eye", "snout", "mouth", "fang", "mane", "claw", "tail"],
        "min_detail_count": 14,
    },
    {
        "id": "manticore",
        "civilization": "greek",
        "model": "ManticoreModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.80,
        "max_head_width_to_body_width": 0.76,
        "max_head_depth_to_body_depth": 0.48,
        "max_visual_head_height_to_body_height": 0.80,
        "max_visual_head_width_to_body_width": 0.78,
        "max_visual_head_depth_to_body_depth": 0.64,
        "required_detail_tokens": ["eye", "snout", "mouth", "fang", "wing", "tail", "sting"],
        "min_detail_count": 16,
    },
    {
        "id": "chimera",
        "civilization": "greek",
        "model": "ChimeraModel.java",
        "head_parts": ["lion_head", "goat_head"],
        "body_parts": ["body"],
        "max_single_head_width_to_body_width": 0.88,
        "max_single_head_depth_to_body_depth": 0.58,
        "max_single_visual_head_width_to_body_width": 0.88,
        "max_single_visual_head_depth_to_body_depth": 0.64,
        "required_detail_tokens": ["lion_left_eye", "lion_snout", "goat_left_eye", "goat_snout", "horn", "wing", "tail"],
        "min_detail_count": 18,
    },
    {
        "id": "colossus",
        "civilization": "greek",
        "archetype": "construct",
        "model": "ColossusModel.java",
        "head_parts": ["head"],
        "body_parts": ["torso_core"],
        "max_head_height_to_body_height": 0.76,
        "max_head_width_to_body_width": 0.72,
        "max_visual_head_height_to_body_height": 0.76,
        "max_visual_head_width_to_body_width": 0.72,
        "required_detail_tokens": ["eye", "mouth", "face", "core"],
        "min_detail_count": 8,
    },
    {
        "id": "pegasus",
        "civilization": "greek",
        "archetype": "equine",
        "model": "PegasusModel.java",
        "head_parts": ["head"],
        "body_parts": ["body", "chest"],
        "body_extent_mode": "local",
        "min_head_height_to_body_height": 0.42,
        "min_head_width_to_body_width": 0.28,
        "max_head_height_to_body_height": 0.68,
        "max_head_width_to_body_width": 0.45,
        "max_head_depth_to_body_depth": 0.36,
        "min_visual_head_height_to_body_height": 0.48,
        "max_visual_head_height_to_body_height": 0.62,
        "max_visual_head_width_to_body_width": 0.42,
        "max_visual_head_depth_to_body_depth": 0.55,
        "required_detail_tokens": ["eye", "ear", "snout", "nostril", "mouth", "mane", "wing", "feather"],
        "min_detail_count": 14,
        "required_parent_child_links": [
            ["neck", "head"],
            ["head", "snout"],
            ["head", "left_eye"],
            ["head", "right_eye"],
            ["head", "left_ear"],
            ["head", "right_ear"],
            ["neck", "mane"],
            ["neck", "mane_crest"],
        ],
    },
    {
        "id": "anubis_warrior",
        "civilization": "egyptian",
        "archetype": "humanoid",
        "model": "AnubisWarriorModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.86,
        "max_head_width_to_body_width": 0.74,
        "max_visual_head_width_to_body_width": 0.76,
        "required_detail_tokens": ["eye", "snout", "ear", "nostril", "mouth", "khopesh"],
        "min_detail_count": 8,
    },
    {
        "id": "scarab_guardian",
        "civilization": "egyptian",
        "model": "ScarabGuardianModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_width_to_body_width": 0.72,
        "max_head_depth_to_body_depth": 0.64,
        "min_detail_count": 8,
    },
    {
        "id": "mummy_soldier",
        "civilization": "egyptian",
        "archetype": "humanoid",
        "model": "MummySoldierModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.74,
        "max_head_width_to_body_width": 0.82,
        "min_detail_count": 8,
    },
    {
        "id": "sphinx_sentinel",
        "civilization": "egyptian",
        "model": "SphinxSentinelModel.java",
        "head_parts": ["head"],
        "body_parts": ["body", "chest"],
        "max_head_height_to_body_height": 0.95,
        "max_head_width_to_body_width": 0.94,
        "min_detail_count": 10,
    },
    {
        "id": "sun_falcon",
        "civilization": "egyptian",
        "model": "SunFalconModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.68,
        "max_head_width_to_body_width": 0.72,
        "max_head_depth_to_body_depth": 0.54,
        "min_detail_count": 8,
    },
    {
        "id": "azure_dragon",
        "civilization": "chinese",
        "model": "AzureDragonModel.java",
        "head_parts": ["head"],
        "body_parts": ["body_1", "body_2", "body_3", "body_4"],
        "max_head_height_to_body_height": 1.15,
        "max_head_width_to_body_width": 1.05,
        "max_head_depth_to_body_depth": 0.36,
        "min_detail_count": 20,
    },
    {
        "id": "qilin_guardian",
        "civilization": "chinese",
        "model": "QilinGuardianModel.java",
        "head_parts": ["head"],
        "body_parts": ["scaled_body"],
        "max_head_height_to_body_height": 0.78,
        "max_head_width_to_body_width": 0.68,
        "max_head_depth_to_body_depth": 0.34,
        "min_detail_count": 14,
    },
    {
        "id": "jiangshi",
        "civilization": "chinese",
        "archetype": "humanoid",
        "model": "JiangshiModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.76,
        "max_head_width_to_body_width": 0.84,
        "min_detail_count": 8,
    },
    {
        "id": "nine_tailed_fox",
        "civilization": "chinese",
        "model": "NineTailedFoxModel.java",
        "head_parts": ["head"],
        "body_parts": ["fox_body"],
        "max_head_height_to_body_height": 0.82,
        "max_head_width_to_body_width": 0.72,
        "max_head_depth_to_body_depth": 0.38,
        "min_detail_count": 8,
    },
    {
        "id": "stone_lion_guardian",
        "civilization": "chinese",
        "model": "StoneLionGuardianModel.java",
        "head_parts": ["head"],
        "body_parts": ["stone_body"],
        "max_head_height_to_body_height": 0.72,
        "max_head_width_to_body_width": 0.74,
        "max_head_depth_to_body_depth": 0.46,
        "min_detail_count": 10,
    },
    {
        "id": "frost_giant",
        "civilization": "nordic",
        "archetype": "giant_humanoid",
        "model": "FrostGiantModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.86,
        "max_head_width_to_body_width": 0.82,
        "min_detail_count": 10,
    },
    {
        "id": "draugr_warrior",
        "civilization": "nordic",
        "archetype": "humanoid",
        "model": "DraugrWarriorModel.java",
        "head_parts": ["head"],
        "body_parts": ["torso"],
        "max_head_height_to_body_height": 0.74,
        "max_head_width_to_body_width": 0.82,
        "min_detail_count": 8,
    },
    {
        "id": "valkyrie",
        "civilization": "nordic",
        "archetype": "humanoid",
        "model": "ValkyrieModel.java",
        "head_parts": ["head"],
        "body_parts": ["armor"],
        "max_head_height_to_body_height": 0.82,
        "max_head_width_to_body_width": 0.72,
        "max_visual_head_width_to_body_width": 0.74,
        "required_detail_tokens": ["eye", "mouth", "helmet", "wing", "spear", "cape"],
        "min_detail_count": 8,
    },
    {
        "id": "fenrir_wolf",
        "civilization": "nordic",
        "model": "FenrirWolfModel.java",
        "head_parts": ["wolf_head"],
        "body_parts": ["wolf_body"],
        "max_head_height_to_body_height": 0.84,
        "max_head_width_to_body_width": 0.76,
        "max_head_depth_to_body_depth": 0.50,
        "required_detail_tokens": ["eye", "jaw", "fang", "ear", "chain", "tail"],
        "min_detail_count": 12,
    },
    {
        "id": "dwarf_berserker",
        "civilization": "nordic",
        "archetype": "giant_humanoid",
        "model": "DwarfBerserkerModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.70,
        "max_head_width_to_body_width": 0.78,
        "min_detail_count": 12,
    },
    {
        "id": "crystal_leviathan",
        "civilization": "atlantean",
        "model": "CrystalLeviathanModel.java",
        "head_parts": ["head"],
        "body_parts": ["body_1", "body_2", "body_3", "body_4"],
        "max_head_height_to_body_height": 0.90,
        "max_head_width_to_body_width": 0.90,
        "max_head_depth_to_body_depth": 0.38,
        "min_detail_count": 16,
    },
    {
        "id": "orichalcum_golem",
        "civilization": "atlantean",
        "archetype": "construct",
        "model": "OrichalcumGolemModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.76,
        "max_head_width_to_body_width": 0.74,
        "min_detail_count": 8,
    },
    {
        "id": "siren_priestess",
        "civilization": "atlantean",
        "archetype": "humanoid",
        "model": "SirenPriestessModel.java",
        "head_parts": ["head"],
        "body_parts": ["body"],
        "max_head_height_to_body_height": 0.74,
        "max_head_width_to_body_width": 0.82,
        "min_detail_count": 8,
    },
    {
        "id": "tidal_guardian",
        "civilization": "atlantean",
        "model": "TidalGuardianModel.java",
        "head_parts": ["head"],
        "body_parts": ["shell"],
        "max_head_width_to_body_width": 0.65,
        "max_head_depth_to_body_depth": 0.60,
        "min_detail_count": 8,
    },
    {
        "id": "abyssal_ray",
        "civilization": "atlantean",
        "model": "AbyssalRayModel.java",
        "head_parts": ["head_lobe"],
        "body_parts": ["ray_body"],
        "max_head_height_to_body_height": 0.96,
        "max_head_width_to_body_width": 0.82,
        "max_head_depth_to_body_depth": 0.62,
        "min_detail_count": 8,
    },
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def source_for_model(model: str) -> str:
    path = MODEL_DIR / model
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def find_part_block(source: str, part_name: str) -> str:
    token = f'addChild("{part_name}"'
    index = source.find(token)
    if index < 0:
        return ""
    next_add = source.find('.addChild("', index + len(token))
    if next_add < 0:
        next_add = source.find('return TexturedModelData', index)
    if next_add < 0:
        next_add = len(source)
    return source[index:next_add]


def broad_part_block(source: str, part_name: str) -> str:
    token = f'addChild("{part_name}"'
    index = source.find(token)
    if index < 0:
        return ""
    end_markers = [
        source.find("\n        ModelPartData ", index + len(token)),
        source.find("\n        root.addChild(", index + len(token)),
        source.find("\n        body.addChild(", index + len(token)),
        source.find("\n        addNeck(", index + len(token)),
        source.find("\n        addLeg(", index + len(token)),
        source.find("\n        return TexturedModelData", index + len(token)),
    ]
    valid = [marker for marker in end_markers if marker > index]
    end = min(valid) if valid else len(source)
    return source[index:end]


def parent_block(source: str, parent_name: str) -> str:
    if parent_name == "root":
        return source
    return broad_part_block(source, parent_name)


def parent_has_child(source: str, parent_name: str, child_name: str) -> bool:
    if parent_name == "body" and child_name.endswith("_neck"):
        dynamic_pattern = rf"addNeck\(\s*body,\s*\"{re.escape(child_name)}\""
        if re.search(dynamic_pattern, source):
            return True
    if parent_name.endswith("_neck") and child_name.endswith("_head"):
        dynamic_pattern = (
            rf"addNeck\(\s*body,\s*\"{re.escape(parent_name)}\",\s*"
            rf"\"{re.escape(child_name)}\""
        )
        if re.search(dynamic_pattern, source):
            return True
    direct_variable_pattern = rf"\b{re.escape(parent_name)}\.addChild\(\s*\"{re.escape(child_name)}\""
    if re.search(direct_variable_pattern, source):
        return True
    block = parent_block(source, parent_name)
    if not block:
        return False
    return f'addChild("{child_name}"' in block


def parse_cuboids(block: str) -> list[dict[str, float]]:
    cuboids: list[dict[str, float]] = []
    pattern = re.compile(
        r"cuboid\(\s*"
        r"([-+]?\d+(?:\.\d+)?)F?,\s*([-+]?\d+(?:\.\d+)?)F?,\s*([-+]?\d+(?:\.\d+)?)F?,\s*"
        r"([-+]?\d+(?:\.\d+)?)F?,\s*([-+]?\d+(?:\.\d+)?)F?,\s*([-+]?\d+(?:\.\d+)?)F?",
        re.MULTILINE,
    )
    for match in pattern.finditer(block):
        x, y, z, width, height, depth = (float(value) for value in match.groups())
        cuboids.append({
            "x1": x,
            "y1": y,
            "z1": z,
            "x2": x + width,
            "y2": y + height,
            "z2": z + depth,
            "width": width,
            "height": height,
            "depth": depth,
        })
    return cuboids


def cuboid_intersection_volume(a: dict[str, float], b: dict[str, float]) -> float:
    dx = min(a["x2"], b["x2"]) - max(a["x1"], b["x1"])
    dy = min(a["y2"], b["y2"]) - max(a["y1"], b["y1"])
    dz = min(a["z2"], b["z2"]) - max(a["z1"], b["z1"])
    if dx <= 0.0 or dy <= 0.0 or dz <= 0.0:
        return 0.0
    return dx * dy * dz


def dynamic_generated_part_block(source: str, part_name: str) -> str:
    if part_name.endswith("_base_socket"):
        token = 'neck.addChild(neckName + "_base_socket"'
    elif part_name.endswith("_body_overlap"):
        token = 'neck.addChild(neckName + "_body_overlap"'
    elif part_name.endswith("_head_overlap"):
        token = 'neck.addChild(neckName + "_head_overlap"'
    elif part_name.endswith("_throat_ridge"):
        token = 'neck.addChild(neckName + "_throat_ridge"'
    elif part_name.endswith("_head"):
        token = "neck.addChild(headName"
    else:
        return ""
    index = source.find(token)
    if index < 0:
        return ""
    next_child = source.find("\n        head.addChild(", index + len(token))
    next_neck_child = source.find("\n        neck.addChild(", index + len(token))
    next_method = source.find("\n    private static", index + len(token))
    valid = [marker for marker in (next_child, next_neck_child, next_method) if marker > index]
    end = min(valid) if valid else len(source)
    return source[index:end]


def dynamic_head_scale(source: str, head_name: str) -> float | None:
    for call in re.finditer(
        r"addNeck\(\s*body,\s*\"(?P<neck>[^\"]+)\",\s*\"(?P<head>[^\"]+)\"(?P<args>.*?)\);",
        source,
        re.DOTALL,
    ):
        if call.group("head") != head_name:
            continue
        numbers = [
            float(value)
            for value in re.findall(r"([-+]?\d+(?:\.\d+)?)F", call.group("args"))
        ]
        return numbers[-1] if numbers else 1.0
    return None


def dynamic_head_cuboids(source: str, head_name: str) -> list[dict[str, float]]:
    scale = dynamic_head_scale(source, head_name)
    if scale is None:
        return []
    # Matches HydraModel.addNeck(): the generated head block is scaled by
    # headScale and then pivoted into the neck's local space.
    cuboid = {
        "x1": -4.5 * scale,
        "y1": -7.5 * scale,
        "z1": -5.7 * scale,
        "x2": 4.5 * scale,
        "y2": (-7.5 + 8.5) * scale,
        "z2": (-5.7 + 8.8) * scale,
        "width": 9.0 * scale,
        "height": 8.5 * scale,
        "depth": 8.8 * scale,
    }
    return apply_offset([cuboid], (0.0, -15.2, -2.6))


def local_cuboids_for_part(source: str, part_name: str) -> list[dict[str, float]]:
    if part_name.endswith("_head"):
        dynamic = dynamic_head_cuboids(source, part_name)
        if dynamic:
            return dynamic
    block = dynamic_generated_part_block(source, part_name)
    if block:
        parsed = parse_cuboids(block)
        if parsed and part_name.endswith("_head"):
            return apply_offset(parsed, parse_transform_offset(block))
        return parsed
    return parse_cuboids(find_part_block(source, part_name))


def parts_intersect_locally(source: str, first: str, second: str) -> bool:
    first_cuboids = local_cuboids_for_part(source, first)
    second_cuboids = local_cuboids_for_part(source, second)
    return any(
        cuboid_intersection_volume(a, b) > 0.0
        for a in first_cuboids
        for b in second_cuboids
    )


def parse_transform_offset(block: str) -> tuple[float, float, float]:
    match = re.search(
        r"ModelTransform\.(?:pivot|of)\(\s*"
        r"([-+]?\d+(?:\.\d+)?)F?,\s*([-+]?\d+(?:\.\d+)?)F?,\s*([-+]?\d+(?:\.\d+)?)F?",
        block,
        re.MULTILINE,
    )
    if not match:
        return (0.0, 0.0, 0.0)
    return tuple(float(value) for value in match.groups())


def apply_offset(cuboids: list[dict[str, float]], offset: tuple[float, float, float]) -> list[dict[str, float]]:
    ox, oy, oz = offset
    adjusted: list[dict[str, float]] = []
    for cuboid in cuboids:
        adjusted.append({
            **cuboid,
            "x1": cuboid["x1"] + ox,
            "y1": cuboid["y1"] + oy,
            "z1": cuboid["z1"] + oz,
            "x2": cuboid["x2"] + ox,
            "y2": cuboid["y2"] + oy,
            "z2": cuboid["z2"] + oz,
        })
    return adjusted


def extent_from_cuboids(cuboids: list[dict[str, float]], found_parts: list[str]) -> dict[str, Any]:
    if not cuboids:
        return {
            "found_parts": found_parts,
            "cuboid_count": 0,
            "width": 0.0,
            "height": 0.0,
            "depth": 0.0,
        }
    min_x = min(item["x1"] for item in cuboids)
    min_y = min(item["y1"] for item in cuboids)
    min_z = min(item["z1"] for item in cuboids)
    max_x = max(item["x2"] for item in cuboids)
    max_y = max(item["y2"] for item in cuboids)
    max_z = max(item["z2"] for item in cuboids)
    return {
        "found_parts": found_parts,
        "cuboid_count": len(cuboids),
        "width": round(max_x - min_x, 4),
        "height": round(max_y - min_y, 4),
        "depth": round(max_z - min_z, 4),
    }


def dynamic_head_extents(source: str, contract: dict[str, Any], base_key: str = "dynamic_head_base_extent") -> dict[str, dict[str, float]]:
    if contract.get("dynamic_head_factory") != "addNeck":
        return {}
    base = contract.get(base_key, {})
    if not base:
        return {}
    calls = re.finditer(
        r"addNeck\(\s*body,\s*\"(?P<neck>[^\"]+)\",\s*\"(?P<head>[^\"]+)\"(?P<args>.*?)\);",
        source,
        re.DOTALL,
    )
    extents: dict[str, dict[str, float]] = {}
    for call in calls:
        numbers = [
            float(value)
            for value in re.findall(r"([-+]?\d+(?:\.\d+)?)F", call.group("args"))
        ]
        scale = numbers[-1] if numbers else 1.0
        extents[call.group("head")] = {
            "width": round(float(base.get("width", 0.0)) * scale, 4),
            "height": round(float(base.get("height", 0.0)) * scale, 4),
            "depth": round(float(base.get("depth", 0.0)) * scale, 4),
        }
    return extents


def dynamic_add_neck_calls(source: str) -> list[dict[str, str]]:
    calls: list[dict[str, str]] = []
    for call in re.finditer(
        r"addNeck\(\s*body,\s*\"(?P<neck>[^\"]+)\",\s*\"(?P<head>[^\"]+)\"(?P<args>.*?)\);",
        source,
        re.DOTALL,
    ):
        calls.append({
            "neck": call.group("neck"),
            "head": call.group("head"),
        })
    return calls


def extent_for_parts(source: str, part_names: list[str], coordinate_mode: str = "global") -> dict[str, Any]:
    cuboids: list[dict[str, float]] = []
    found_parts: list[str] = []
    for part_name in part_names:
        block = find_part_block(source, part_name)
        parsed = parse_cuboids(block)
        if coordinate_mode == "global":
            parsed = apply_offset(parsed, parse_transform_offset(block))
        if parsed:
            found_parts.append(part_name)
            cuboids.extend(parsed)
    return extent_from_cuboids(cuboids, found_parts)


def local_extent_for_parts(source: str, part_names: list[str]) -> dict[str, Any]:
    cuboids: list[dict[str, float]] = []
    found_parts: list[str] = []
    for part_name in part_names:
        parsed = parse_cuboids(find_part_block(source, part_name))
        if parsed:
            found_parts.append(part_name)
            cuboids.extend(parsed)
    return extent_from_cuboids(cuboids, found_parts)


def merged_extent(extents: list[dict[str, Any]]) -> dict[str, Any]:
    present = [extent for extent in extents if extent.get("cuboid_count", 0) > 0]
    if not present:
        return {
            "found_parts": [],
            "cuboid_count": 0,
            "width": 0.0,
            "height": 0.0,
            "depth": 0.0,
        }
    return {
        "found_parts": sorted({
            part
            for extent in present
            for part in extent.get("found_parts", [])
        }),
        "cuboid_count": sum(int(extent.get("cuboid_count", 0)) for extent in present),
        "width": round(max(float(extent.get("width", 0.0)) for extent in present), 4),
        "height": round(max(float(extent.get("height", 0.0)) for extent in present), 4),
        "depth": round(max(float(extent.get("depth", 0.0)) for extent in present), 4),
    }


def runtime_names(source: str) -> list[str]:
    return sorted(set(re.findall(r'"([a-z0-9_]+)"', source)))


def expanded_runtime_names(source: str, head_parts: list[str]) -> list[str]:
    names = runtime_names(source)
    for suffix in re.findall(r"(?:headName|neckName)\s*\+\s*\"(_[a-z0-9_]+)\"", source):
        for head_part in head_parts:
            names.append(f"{head_part}{suffix}")
    return sorted(set(names))


def detail_names(source: str, head_parts: list[str], body_parts: list[str]) -> list[str]:
    names = expanded_runtime_names(source, head_parts)
    excluded = set(head_parts + body_parts)
    return [
        name for name in names
        if name not in excluded and any(token in name for token in DETAIL_TOKENS)
    ]


def visual_head_part_names(source: str, head_parts: list[str], body_parts: list[str]) -> list[str]:
    excluded = set(head_parts + body_parts)
    names = []
    for name in runtime_names(source):
        if name in excluded:
            continue
        lower = name.lower()
        if any(token in lower for token in VISUAL_HEAD_VOLUME_EXCLUDE_TOKENS):
            continue
        if any(token in lower for token in VISUAL_HEAD_VOLUME_TOKENS):
            names.append(name)
    return sorted(names)


def visual_part_names_for_head(head_part: str, visual_part_names: list[str]) -> list[str]:
    if head_part == "head":
        return visual_part_names
    prefix = head_part.removesuffix("_head")
    if prefix and prefix != head_part:
        return [name for name in visual_part_names if name.startswith(prefix + "_")]
    return [name for name in visual_part_names if name.startswith(head_part + "_")]


def visual_extent_for_head(source: str, head_part: str, visual_part_names: list[str]) -> dict[str, Any]:
    child_names = visual_part_names_for_head(head_part, visual_part_names)
    return local_extent_for_parts(source, [head_part] + child_names)


def visual_extent_for_heads(source: str, head_parts: list[str], visual_part_names: list[str]) -> dict[str, Any]:
    return merged_extent([
        visual_extent_for_head(source, head_part, visual_part_names)
        for head_part in head_parts
    ])


def ratio(value: float, denominator: float) -> float:
    if denominator <= 0:
        return 0.0
    return round(value / denominator, 4)


def audit_contract(contract: dict[str, Any]) -> dict[str, Any]:
    source = source_for_model(contract["model"])
    model_path = MODEL_DIR / contract["model"]
    head_parts = list(contract["head_parts"])
    body_parts = list(contract["body_parts"])
    head_extent_mode = str(contract.get("head_extent_mode", "global"))
    body_extent_mode = str(contract.get("body_extent_mode", "global"))
    dynamic_extents = dynamic_head_extents(source, contract)
    dynamic_visual_extents = dynamic_head_extents(source, contract, "dynamic_visual_head_base_extent")
    dynamic_neck_calls = dynamic_add_neck_calls(source)
    if dynamic_extents:
        found_dynamic = [part for part in head_parts if part in dynamic_extents]
        if found_dynamic:
            head_extent = {
                "found_parts": found_dynamic,
                "cuboid_count": len(found_dynamic),
                "width": round(max(dynamic_extents[part]["width"] for part in found_dynamic), 4),
                "height": round(max(dynamic_extents[part]["height"] for part in found_dynamic), 4),
                "depth": round(max(dynamic_extents[part]["depth"] for part in found_dynamic), 4),
            }
        else:
            head_extent = extent_for_parts(source, head_parts, head_extent_mode)
    else:
        head_extent = extent_for_parts(source, head_parts, head_extent_mode)
    visual_part_names = visual_head_part_names(source, head_parts, body_parts)
    if dynamic_visual_extents:
        visual_head_extent = merged_extent([head_extent, {
            "found_parts": list(dynamic_visual_extents),
            "cuboid_count": len(dynamic_visual_extents),
            "width": round(max((dynamic_visual_extents[part]["width"] for part in dynamic_visual_extents), default=0.0), 4),
            "height": round(max((dynamic_visual_extents[part]["height"] for part in dynamic_visual_extents), default=0.0), 4),
            "depth": round(max((dynamic_visual_extents[part]["depth"] for part in dynamic_visual_extents), default=0.0), 4),
        }])
    else:
        visual_head_extent = visual_extent_for_heads(source, head_parts, visual_part_names)
    body_extent = extent_for_parts(source, body_parts, body_extent_mode)
    details = detail_names(source, head_parts, body_parts)
    all_names = expanded_runtime_names(source, head_parts)
    checks: list[dict[str, Any]] = []

    def add(name: str, ok: bool, detail: dict[str, Any]) -> None:
        checks.append({"name": name, "ok": ok, "detail": detail})

    add("model_file_present", bool(source), {"model": project_path(model_path)})
    add("head_parts_present", set(head_parts).issubset(set(head_extent["found_parts"])), {
        "required": head_parts,
        "found": head_extent["found_parts"],
    })
    add("body_parts_present", set(body_parts).issubset(set(body_extent["found_parts"])), {
        "required": body_parts,
        "found": body_extent["found_parts"],
    })
    if "required_head_count" in contract:
        required_head_count = int(contract["required_head_count"])
        actual_heads = sorted({call["head"] for call in dynamic_neck_calls})
        add("required_head_count_matches_design_contract", len(actual_heads) == required_head_count, {
            "required": required_head_count,
            "actual": len(actual_heads),
            "actual_heads": actual_heads,
            "rule": "Hydra V3 is a nine-head design; an older five-head runtime model is rejected even if its remaining parts have mechanical proportions.",
        })
    if "required_neck_count" in contract:
        required_neck_count = int(contract["required_neck_count"])
        actual_necks = sorted({call["neck"] for call in dynamic_neck_calls})
        add("required_neck_count_matches_design_contract", len(actual_necks) == required_neck_count, {
            "required": required_neck_count,
            "actual": len(actual_necks),
            "actual_necks": actual_necks,
            "rule": "Each Hydra head must have its own connected neck root/mid/tip chain.",
        })
    add("runtime_detail_density_present", len(details) >= int(contract.get("min_detail_count", 8)), {
        "detail_count": len(details),
        "min_detail_count": int(contract.get("min_detail_count", 8)),
        "sample": details[:24],
    })
    required_detail_tokens = list(contract.get("required_detail_tokens", []))
    if required_detail_tokens:
        missing_tokens = [
            token for token in required_detail_tokens
            if not any(token in name for name in all_names)
        ]
        add("runtime_required_identity_detail_tokens_present", not missing_tokens, {
            "required_tokens": required_detail_tokens,
            "missing_tokens": missing_tokens,
        })
    connection_parts = list(contract.get("connection_parts", []))
    if connection_parts:
        found_connection_parts = [
            part for part in connection_parts
            if extent_for_parts(source, [part]).get("cuboid_count", 0) > 0
        ]
        missing_connection_parts = [
            part for part in connection_parts
            if part not in found_connection_parts
        ]
        add("head_body_connection_parts_present", not missing_connection_parts, {
            "required": connection_parts,
            "found": found_connection_parts,
            "missing": missing_connection_parts,
        })
    parent_child_links = list(contract.get("required_parent_child_links", []))
    if parent_child_links:
        link_results = [
            {
                "parent": parent,
                "child": child,
                "ok": parent_has_child(source, parent, child),
            }
            for parent, child in parent_child_links
        ]
        missing_links = [
            f"{item['parent']}->{item['child']}"
            for item in link_results
            if not item["ok"]
        ]
        add("runtime_parent_child_structure_present", not missing_links, {
            "links": link_results,
            "missing": missing_links,
        })
    overlap_pairs = list(contract.get("required_overlap_pairs", []))
    if overlap_pairs:
        overlap_results = [
            {
                "first": first,
                "second": second,
                "ok": parts_intersect_locally(source, first, second),
            }
            for first, second in overlap_pairs
        ]
        missing_overlaps = [
            f"{item['first']}<->{item['second']}"
            for item in overlap_results
            if not item["ok"]
        ]
        add("runtime_connection_overlap_intersects", not missing_overlaps, {
            "pairs": overlap_results,
            "missing": missing_overlaps,
            "rule": "Connection cuboids must geometrically intersect their adjacent neck/body or neck/head part in local model space; naming alone is not enough.",
        })

    ratios = {
        "height_to_body_height": ratio(head_extent["height"], body_extent["height"]),
        "width_to_body_width": ratio(head_extent["width"], body_extent["width"]),
        "depth_to_body_depth": ratio(head_extent["depth"], body_extent["depth"]),
    }
    visual_ratios = {
        "height_to_body_height": ratio(visual_head_extent["height"], body_extent["height"]),
        "width_to_body_width": ratio(visual_head_extent["width"], body_extent["width"]),
        "depth_to_body_depth": ratio(visual_head_extent["depth"], body_extent["depth"]),
    }
    if "max_head_height_to_body_height" in contract:
        add("head_height_runtime_proportionate", ratios["height_to_body_height"] <= float(contract["max_head_height_to_body_height"]), {
            "ratio": ratios["height_to_body_height"],
            "max": float(contract["max_head_height_to_body_height"]),
        })
    if "min_head_height_to_body_height" in contract:
        add("head_height_runtime_readable_without_overscale", ratios["height_to_body_height"] >= float(contract["min_head_height_to_body_height"]), {
            "ratio": ratios["height_to_body_height"],
            "min": float(contract["min_head_height_to_body_height"]),
        })
    if "max_head_width_to_body_width" in contract:
        add("head_width_runtime_proportionate", ratios["width_to_body_width"] <= float(contract["max_head_width_to_body_width"]), {
            "ratio": ratios["width_to_body_width"],
            "max": float(contract["max_head_width_to_body_width"]),
        })
    if "min_head_width_to_body_width" in contract:
        add("head_width_runtime_readable_without_overscale", ratios["width_to_body_width"] >= float(contract["min_head_width_to_body_width"]), {
            "ratio": ratios["width_to_body_width"],
            "min": float(contract["min_head_width_to_body_width"]),
        })
    if "max_head_depth_to_body_depth" in contract:
        add("head_depth_runtime_proportionate", ratios["depth_to_body_depth"] <= float(contract["max_head_depth_to_body_depth"]), {
            "ratio": ratios["depth_to_body_depth"],
            "max": float(contract["max_head_depth_to_body_depth"]),
        })
    if "max_visual_head_height_to_body_height" in contract:
        add("visual_head_height_runtime_proportionate", visual_ratios["height_to_body_height"] <= float(contract["max_visual_head_height_to_body_height"]), {
            "ratio": visual_ratios["height_to_body_height"],
            "max": float(contract["max_visual_head_height_to_body_height"]),
            "visual_parts": visual_part_names,
        })
    if "min_visual_head_height_to_body_height" in contract:
        add("visual_head_height_runtime_readable_without_overscale", visual_ratios["height_to_body_height"] >= float(contract["min_visual_head_height_to_body_height"]), {
            "ratio": visual_ratios["height_to_body_height"],
            "min": float(contract["min_visual_head_height_to_body_height"]),
            "visual_parts": visual_part_names,
        })
    if "max_visual_head_width_to_body_width" in contract:
        add("visual_head_width_runtime_proportionate", visual_ratios["width_to_body_width"] <= float(contract["max_visual_head_width_to_body_width"]), {
            "ratio": visual_ratios["width_to_body_width"],
            "max": float(contract["max_visual_head_width_to_body_width"]),
            "visual_parts": visual_part_names,
        })
    if "max_visual_head_depth_to_body_depth" in contract:
        add("visual_head_depth_runtime_proportionate", visual_ratios["depth_to_body_depth"] <= float(contract["max_visual_head_depth_to_body_depth"]), {
            "ratio": visual_ratios["depth_to_body_depth"],
            "max": float(contract["max_visual_head_depth_to_body_depth"]),
            "visual_parts": visual_part_names,
        })

    archetype = str(contract.get("archetype", "generic"))
    detail_preserving_limits = dict(DETAIL_PRESERVING_RUNTIME_LIMITS.get(archetype, {}))
    detail_preserving_limits.update(contract.get("detail_preserving_head_limits", {}))
    if detail_preserving_limits:
        if "max_head_height_to_body_height" in detail_preserving_limits:
            max_height = float(detail_preserving_limits["max_head_height_to_body_height"])
            add("detail_preserving_head_height_runtime_not_oversized", ratios["height_to_body_height"] <= max_height, {
                "ratio": ratios["height_to_body_height"],
                "max": max_height,
                "archetype": archetype,
                "rule": "Runtime readability must not be recovered by making the base head too large; preserve face precision with named cuboids and UVs.",
            })
        if "max_head_width_to_body_width" in detail_preserving_limits:
            max_width = float(detail_preserving_limits["max_head_width_to_body_width"])
            add("detail_preserving_head_width_runtime_not_oversized", ratios["width_to_body_width"] <= max_width, {
                "ratio": ratios["width_to_body_width"],
                "max": max_width,
                "archetype": archetype,
                "rule": "The base head block must match body proportion; eyes, mouth, snout, ears, horns, mane, weapon, wings, tail, and UV detail carry identity.",
            })
        if "max_head_depth_to_body_depth" in detail_preserving_limits:
            max_depth = float(detail_preserving_limits["max_head_depth_to_body_depth"])
            add("detail_preserving_head_depth_runtime_not_oversized", ratios["depth_to_body_depth"] <= max_depth, {
                "ratio": ratios["depth_to_body_depth"],
                "max": max_depth,
                "archetype": archetype,
                "rule": "Head/snout depth must stay anatomical for the body; do not use a deep head block as a detail substitute.",
            })
        if "max_visual_head_width_to_body_width" in detail_preserving_limits:
            max_visual_width = float(detail_preserving_limits["max_visual_head_width_to_body_width"])
            add("detail_preserving_visual_head_width_runtime_not_oversized", visual_ratios["width_to_body_width"] <= max_visual_width, {
                "ratio": visual_ratios["width_to_body_width"],
                "max": max_visual_width,
                "archetype": archetype,
                "visual_parts": visual_part_names,
            })

    if (
            "max_single_head_width_to_body_width" in contract
            or "max_single_head_depth_to_body_depth" in contract
            or "max_single_visual_head_width_to_body_width" in contract
            or "max_single_visual_head_depth_to_body_depth" in contract
    ):
        single_ratios = []
        for head_part in head_parts:
            single_extent = dynamic_extents.get(head_part) or extent_for_parts(source, [head_part])
            single_visual_extent = (
                dynamic_visual_extents.get(head_part)
                or visual_extent_for_head(source, head_part, visual_part_names)
                or single_extent
            )
            single_ratios.append({
                "name": head_part,
                "width_to_body_width": ratio(single_extent["width"], body_extent["width"]),
                "depth_to_body_depth": ratio(single_extent["depth"], body_extent["depth"]),
                "height_to_body_height": ratio(single_extent["height"], body_extent["height"]),
                "visual_width_to_body_width": ratio(single_visual_extent["width"], body_extent["width"]),
                "visual_depth_to_body_depth": ratio(single_visual_extent["depth"], body_extent["depth"]),
                "visual_height_to_body_height": ratio(single_visual_extent["height"], body_extent["height"]),
            })
        if "max_single_head_width_to_body_width" in contract:
            worst = max((item["width_to_body_width"] for item in single_ratios), default=0.0)
            add("single_head_width_runtime_proportionate", worst <= float(contract["max_single_head_width_to_body_width"]), {
                "worst": worst,
                "max": float(contract["max_single_head_width_to_body_width"]),
                "single_head_ratios": single_ratios,
            })
        if "max_single_head_depth_to_body_depth" in contract:
            worst = max((item["depth_to_body_depth"] for item in single_ratios), default=0.0)
            add("single_head_depth_runtime_proportionate", worst <= float(contract["max_single_head_depth_to_body_depth"]), {
                "worst": worst,
                "max": float(contract["max_single_head_depth_to_body_depth"]),
                "single_head_ratios": single_ratios,
            })
        if "max_single_visual_head_width_to_body_width" in contract:
            worst = max((item["visual_width_to_body_width"] for item in single_ratios), default=0.0)
            add("single_visual_head_width_runtime_proportionate", worst <= float(contract["max_single_visual_head_width_to_body_width"]), {
                "worst": worst,
                "max": float(contract["max_single_visual_head_width_to_body_width"]),
                "single_head_ratios": single_ratios,
            })
        if "max_single_visual_head_depth_to_body_depth" in contract:
            worst = max((item["visual_depth_to_body_depth"] for item in single_ratios), default=0.0)
            add("single_visual_head_depth_runtime_proportionate", worst <= float(contract["max_single_visual_head_depth_to_body_depth"]), {
                "worst": worst,
                "max": float(contract["max_single_visual_head_depth_to_body_depth"]),
                "single_head_ratios": single_ratios,
            })

    proportion_check_terms = (
        "head_height", "head_width", "head_depth",
        "visual_head", "single_head", "single_visual_head",
    )
    failed_proportion_checks = [
        check["name"]
        for check in checks
        if any(term in check["name"] for term in proportion_check_terms)
        and not check["ok"]
    ]
    failed_detail_checks = [
        check["name"]
        for check in checks
        if check["name"] in {
            "runtime_detail_density_present",
            "runtime_required_identity_detail_tokens_present",
            "head_body_connection_parts_present",
            "runtime_parent_child_structure_present",
            "runtime_connection_overlap_intersects",
        }
        and not check["ok"]
    ]
    detail_first_readability_ok = not failed_proportion_checks and not failed_detail_checks
    add("runtime_detail_first_readability_not_head_scale", detail_first_readability_ok, {
        "rule": "Runtime readability must come from named cuboids for eyes, mouth, snout/jaw, horns, mane, wings, weapon, tail, scale, and connection parts; head volume must remain body-proportionate, and shrinking a head must not remove those details.",
        "failed_proportion_checks": failed_proportion_checks,
        "failed_detail_checks": failed_detail_checks,
        "detail_count": len(details),
        "required_detail_tokens": required_detail_tokens,
    })

    failures = [check["name"] for check in checks if not check["ok"]]
    return {
        "id": contract["id"],
        "civilization": contract["civilization"],
        "model": project_path(model_path),
        "archetype": archetype,
        "ok": not failures,
        "failures": failures,
        "head_parts": head_parts,
        "body_parts": body_parts,
        "head_extent": head_extent,
        "visual_head_extent": visual_head_extent,
        "body_extent": body_extent,
        "extent_modes": {
            "head": head_extent_mode,
            "body": body_extent_mode,
        },
        "ratios": ratios,
        "visual_ratios": visual_ratios,
        "detail_count": len(details),
        "detail_sample": details[:24],
        "detail_first_readability_ok": detail_first_readability_ok,
        "detail_first_readability": {
            "failed_proportion_checks": failed_proportion_checks,
            "failed_detail_checks": failed_detail_checks,
            "required_detail_tokens": required_detail_tokens,
            "detail_count": len(details),
            "detail_sample": details[:24],
        },
        "checks": checks,
    }


def build_report() -> dict[str, Any]:
    creatures = [audit_contract(contract) for contract in CONTRACTS]
    blockers = [
        f"{item['civilization']}:{item['id']}:{failure}"
        for item in creatures
        for failure in item["failures"]
    ]
    return {
        "version": 1,
        "scope": "runtime_java_model_head_body_proportion_quality",
        "ok": not blockers,
        "creature_count": len(creatures),
        "blocker_count": len(blockers),
        "detail_first_readability_ok": all(item["detail_first_readability_ok"] for item in creatures),
        "detail_first_readability_count": sum(1 for item in creatures if item["detail_first_readability_ok"]),
        "blockers": blockers,
        "rule": "Runtime Java models must preserve body-proportionate heads; identity must come from named eye/mouth/horn/wing/tail/weapon details rather than oversized head geometry.",
        "detail_first_rule": "Runtime models may not recover readability by growing the head, and may not lose detail while correcting proportions: named face and signature cuboids must carry the detail.",
        "report_path": project_path(REPORT_PATH),
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
        status = "PASS" if payload["ok"] else "FAIL"
        print(f"Creature Runtime Proportion Quality Gate: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"creatures: {payload['creature_count']}, blockers: {payload['blocker_count']}")
        for blocker in payload["blockers"][:40]:
            print(f"blocker: {blocker}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
