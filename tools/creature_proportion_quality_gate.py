#!/usr/bin/env python3
"""Audit creature head/body proportion and modeled-detail contracts.

This gate prevents a recurring art-direction failure: making heads bigger to
solve readability problems. Readability should come from explicit eyes, mouths,
jaws, horns, weapons, wings, tails, and other named detail parts.
"""

from __future__ import annotations

import ast
import argparse
import importlib.util
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/creature_visual_acceptance"
REPORT_PATH = REPORT_DIR / "creature_proportion_quality_report.json"

PIPELINES = [
    ("greek", ROOT / "tools/greek_creature_visual_pipeline.py", "CREATURES"),
    ("egyptian", ROOT / "tools/egyptian_creature_visual_pipeline.py", "EGYPTIAN_CREATURES"),
    ("chinese", ROOT / "tools/chinese_creature_visual_pipeline.py", "CHINESE_CREATURES"),
    ("nordic", ROOT / "tools/nordic_creature_visual_pipeline.py", "NORDIC_CREATURES"),
    ("atlantean", ROOT / "tools/atlantean_creature_visual_pipeline.py", "ATLANTEAN_CREATURES"),
]

HEAD_NAME_ALIASES = {
    "head",
    "giant_head",
    "dragon_head",
    "jackal_head",
    "wrapped_head",
    "human_head",
    "crystal_head",
    "head_lobe",
    "center_head",
    "left_head",
    "right_head",
    "outer_left_head",
    "outer_right_head",
    "lion_head",
    "goat_head",
    "huge_head",
}

DETAIL_TOKENS = (
    "eye", "pupil", "mouth", "nose", "nostril", "snout", "muzzle", "beak", "jaw",
    "fang", "tooth", "teeth", "horn", "antler", "ear", "brow",
    "whisker", "mandible", "tongue", "face", "gaze", "mane",
    "spike", "chain", "tail", "ball", "claw",
    "bandage", "wrap", "talisman", "mark", "hat", "helmet", "guard",
    "crest", "rune", "glyph", "core", "glow", "rivet", "knuckle",
    "gill", "slit", "rib", "ridge", "beard", "braid", "fin",
)
BODY_TOKENS = (
    "body", "torso", "shell", "ray_body", "wolf_body", "horse_body",
    "lion_body", "falcon_body",
)
HEAD_DETAIL_EXCLUDE_TOKENS = (
    "club_head", "weapon", "headdress", "helmet", "crown", "hat",
    "hair", "mane", "beard", "snake_head", "serpent_head", "crystal",
)
ARCHETYPE_LIMITS = {
    "humanoid": {
        "max_head_height_to_body_height": 0.74,
        "max_head_width_to_body_width": 0.82,
        "min_detail_count": 8,
    },
    "giant_humanoid": {
        "max_head_height_to_body_height": 0.70,
        "max_head_width_to_body_width": 0.78,
        "min_detail_count": 8,
    },
    "quadruped": {
        "max_head_height_to_body_height": 0.86,
        "max_head_width_to_body_width": 0.82,
        "max_head_depth_to_body_depth": 0.50,
        "min_detail_count": 8,
    },
    "equine": {
        "max_head_height_to_body_height": 0.68,
        "max_head_width_to_body_width": 0.52,
        "max_head_depth_to_body_depth": 0.34,
        "min_detail_count": 12,
    },
    "serpentine": {
        "max_head_height_to_body_height": 1.12,
        "max_head_width_to_body_width": 1.05,
        "max_head_depth_to_body_depth": 0.34,
        "min_detail_count": 16,
    },
    "avian": {
        "max_head_height_to_body_height": 0.78,
        "max_head_width_to_body_width": 0.86,
        "max_head_depth_to_body_depth": 0.54,
        "min_detail_count": 8,
    },
    "construct": {
        "max_head_height_to_body_height": 0.72,
        "max_head_width_to_body_width": 0.70,
        "min_detail_count": 8,
    },
}

DETAIL_PRESERVING_HEAD_LIMITS = {
    # User feedback 2026-06-15: several creatures read as oversized-head
    # designs. These stricter limits are blockers for the base head block; the
    # lost readability must be recovered through named detail parts and UVs.
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
    },
    "construct": {
        "max_head_height_to_body_height": 0.72,
        "max_head_width_to_body_width": 0.70,
    },
}
CREATURE_ARCHETYPES = {
    "minotaur": "giant_humanoid",
    "cyclops": "giant_humanoid",
    "centaur": "humanoid",
    "medusa": "humanoid",
    "colossus": "construct",
    "pegasus": "equine",
    "anubis_warrior": "humanoid",
    "mummy_soldier": "humanoid",
    "sphinx_sentinel": "quadruped",
    "sun_falcon": "avian",
    "azure_dragon": "serpentine",
    "qilin_guardian": "quadruped",
    "jiangshi": "humanoid",
    "nine_tailed_fox": "quadruped",
    "stone_lion_guardian": "quadruped",
    "frost_giant": "giant_humanoid",
    "draugr_warrior": "humanoid",
    "valkyrie": "humanoid",
    "dwarf_berserker": "giant_humanoid",
    "fenrir_wolf": "quadruped",
    "crystal_leviathan": "serpentine",
    "orichalcum_golem": "construct",
    "siren_priestess": "humanoid",
    "abyssal_ray": "avian",
}
BAD_PROPORTION_WORDS = (
    "大头", "五个大头", "huge_head", "large_head", "large heads",
    "enlarged heads", "head_large_enough", "头必须比旧模型更大",
)
NEGATED_PROPORTION_CONTEXTS = (
    "不能", "禁止", "不得", "避免", "不来自", "不允许", "不靠", "不能靠",
    "过大",
    "异常", "身子大头小", "头部太小", "头部过大",
)

CREATURE_SPEC: dict[str, dict[str, Any]] = {
    "minotaur": {
        "max_head_height_to_body_height": 0.70,
        "max_head_width_to_body_width": 0.80,
        "required_detail_tokens": ["eye", "muzzle", "nostril", "mouth", "horn", "ear"],
        "min_detail_count": 12,
    },
    "hydra": {
        "head_names": ["center_head", "left_head", "right_head", "outer_left_head", "outer_right_head"],
        "body_names": ["body", "shoulder_saddle"],
        "max_single_head_to_body_depth": 0.40,
        "max_single_head_width_to_body_width": 0.92,
        "required_detail_tokens": ["neck_bridge", "body_overlap", "head_overlap", "eye", "jaw", "fang", "horn", "poison_gland"],
        "min_detail_count": 30,
        "require_fan_wider_than_body": 2.2,
        "required_overlap_pairs": [
            ["center_neck_body_overlap", "center_neck_bridge"],
            ["left_neck_body_overlap", "left_neck_bridge"],
            ["right_neck_body_overlap", "right_neck_bridge"],
            ["outer_left_neck_body_overlap", "outer_left_neck_bridge"],
            ["outer_right_neck_body_overlap", "outer_right_neck_bridge"],
            ["center_neck_head_overlap", "center_head"],
            ["left_neck_head_overlap", "left_head"],
            ["right_neck_head_overlap", "right_head"],
            ["outer_left_neck_head_overlap", "outer_left_head"],
            ["outer_right_neck_head_overlap", "outer_right_head"],
        ],
    },
    "pegasus": {
        "head_names": ["head"],
        "body_names": ["body", "chest"],
        "archetype": "equine",
        "max_head_height_to_body_height": 0.63,
        "min_head_height_to_body_height": 0.45,
        "max_head_width_to_body_width": 0.45,
        "min_head_width_to_body_width": 0.32,
        "max_head_depth_to_body_depth": 0.34,
        "required_detail_tokens": ["eye", "ear", "snout", "nostril", "mouth", "mane", "wing", "feather"],
        "min_detail_count": 12,
        "required_forward_structure_pairs": [
            ["neck", "head"],
            ["head", "snout"],
        ],
    },
    "medusa": {
        "max_head_width_to_body_width": 0.78,
        "required_detail_tokens": ["eye", "pupil", "mouth", "snake", "fang", "tongue", "tail"],
        "min_detail_count": 18,
    },
    "mummy_soldier": {
        "required_detail_tokens": ["eye", "bandage", "mouth", "spear"],
        "min_detail_count": 8,
    },
    "anubis_warrior": {
        "body_names": ["torso", "gold_collar", "linen_skirt"],
        "max_head_width_to_body_width": 0.74,
        "required_detail_tokens": ["eye", "snout", "ear", "nostril", "mouth", "collar", "khopesh"],
        "min_detail_count": 8,
    },
    "sphinx_sentinel": {
        "max_head_height_to_body_height": 0.92,
        "required_detail_tokens": ["eye", "face", "headdress", "mouth", "tail"],
        "min_detail_count": 8,
    },
    "sun_falcon": {
        "max_head_height_to_body_height": 0.70,
        "max_head_width_to_body_width": 0.72,
        "max_head_depth_to_body_depth": 0.54,
        "required_detail_tokens": ["eye", "beak", "brow", "sun_disk", "wing"],
        "min_detail_count": 8,
    },
    "jiangshi": {
        "required_detail_tokens": ["eye", "mouth", "talisman", "hat", "cuff"],
        "min_detail_count": 8,
    },
    "draugr_warrior": {
        "body_names": ["torso", "shoulder_armor"],
        "required_detail_tokens": ["eye", "mouth", "helmet", "horn", "shield", "axe"],
        "min_detail_count": 8,
    },
    "valkyrie": {
        "body_names": ["body", "armor_plate"],
        "max_head_width_to_body_width": 0.72,
        "required_detail_tokens": ["eye", "mouth", "helmet", "wing", "spear"],
        "min_detail_count": 8,
    },
    "dwarf_berserker": {
        "required_detail_tokens": ["eye", "nose", "mouth", "beard", "braid", "axe"],
        "min_detail_count": 8,
    },
    "crystal_leviathan": {
        "max_head_height_to_body_height": 0.92,
        "max_head_width_to_body_width": 0.92,
        "required_detail_tokens": ["eye", "jaw", "mouth", "gill", "crystal", "fin", "tail"],
        "min_detail_count": 16,
    },
    "orichalcum_golem": {
        "required_detail_tokens": ["eye", "core", "rivet", "knuckle", "crystal"],
        "min_detail_count": 8,
    },
    "siren_priestess": {
        "max_head_height_to_body_height": 0.74,
        "max_head_width_to_body_width": 0.82,
        "required_detail_tokens": ["eye", "mouth", "fin", "staff", "tail"],
        "min_detail_count": 8,
    },
    "abyssal_ray": {
        "max_head_height_to_body_height": 0.94,
        "required_detail_tokens": ["eye", "mouth", "gill", "wing", "rib", "tail", "glow"],
        "min_detail_count": 8,
    },
    "nemean_lion": {
        "body_names": ["body", "shoulder", "chest_mass"],
        "max_head_height_to_body_height": 0.80,
        "max_head_width_to_body_width": 0.78,
        "max_head_depth_to_body_depth": 0.55,
        "min_detail_count": 9,
    },
    "manticore": {
        "body_names": ["body", "chest", "shoulder_ridge"],
        "max_head_height_to_body_height": 0.80,
        "max_head_width_to_body_width": 0.76,
        "max_head_depth_to_body_depth": 0.50,
        "min_detail_count": 10,
    },
    "nine_tailed_fox": {
        "max_head_height_to_body_height": 0.82,
        "max_head_width_to_body_width": 0.72,
        "max_head_depth_to_body_depth": 0.38,
        "min_detail_count": 8,
    },
    "stone_lion_guardian": {
        "max_head_height_to_body_height": 0.72,
        "max_head_width_to_body_width": 0.74,
        "max_head_depth_to_body_depth": 0.46,
        "min_detail_count": 10,
    },
    "fenrir_wolf": {
        "head_names": ["wolf_head"],
        "body_names": ["wolf_body", "shoulder_hump"],
        "max_head_height_to_body_height": 0.84,
        "max_head_width_to_body_width": 0.76,
        "max_head_depth_to_body_depth": 0.48,
        "required_detail_tokens": ["eye", "jaw", "fang", "ear", "chain", "tail"],
        "min_detail_count": 12,
    },
    "frost_giant": {
        "head_names": ["giant_head"],
        "body_names": ["torso"],
        "max_head_height_to_body_height": 0.85,
    },
    "azure_dragon": {
        "head_names": ["dragon_head"],
        "body_names": ["body_1", "body_2", "body_3", "body_4"],
        "max_head_height_to_body_height": 1.12,
        "max_head_width_to_body_width": 1.05,
        "max_head_depth_to_body_depth": 0.34,
        "required_detail_tokens": [
            "eye", "mouth", "brow", "horn", "whisker", "belly_scale",
            "dorsal_fin", "tail_fin", "talon",
        ],
        "min_detail_count": 20,
    },
    "qilin_guardian": {
        "head_names": ["head"],
        "body_names": ["body"],
        "max_head_height_to_body_height": 0.78,
        "max_head_width_to_body_width": 0.68,
        "max_head_depth_to_body_depth": 0.34,
        "required_detail_tokens": ["eye", "mouth", "antler", "mane", "scale", "hoof", "tail"],
        "min_detail_count": 14,
    },
}


def project_path(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(ROOT))
    except ValueError:
        return str(path.resolve())


def load_creatures(path: Path, variable: str) -> list[dict[str, Any]]:
    spec = importlib.util.spec_from_file_location(f"creature_proportion_{path.stem}", path)
    if spec is not None and spec.loader is not None:
        module = importlib.util.module_from_spec(spec)
        try:
            spec.loader.exec_module(module)
            creatures = getattr(module, variable, None)
            if isinstance(creatures, list):
                return creatures
        except ModuleNotFoundError:
            pass

    tree = ast.parse(path.read_text(encoding="utf-8"))
    literals: dict[str, Any] = {}
    for node in tree.body:
        if isinstance(node, ast.AnnAssign) and getattr(node.target, "id", None) == variable:
            literals[variable] = ast.literal_eval(node.value)
        if isinstance(node, ast.Assign):
            for target in node.targets:
                if getattr(target, "id", None) == variable:
                    literals[variable] = ast.literal_eval(node.value)
        if isinstance(node, ast.AnnAssign) and isinstance(getattr(node.target, "id", None), str):
            name = node.target.id
            if name.endswith("_1_0_DETAIL_REFINEMENTS"):
                literals[name] = ast.literal_eval(node.value)
        if isinstance(node, ast.Assign):
            for target in node.targets:
                name = getattr(target, "id", None)
                if isinstance(name, str) and name.endswith("_1_0_DETAIL_REFINEMENTS"):
                    literals[name] = ast.literal_eval(node.value)

    creatures = literals.get(variable)
    if isinstance(creatures, list):
        for refinement in [
            value for key, value in literals.items()
            if key.endswith("_1_0_DETAIL_REFINEMENTS") and isinstance(value, dict)
        ]:
            for creature in creatures:
                item = refinement.get(creature.get("id"), {})
                if not isinstance(item, dict):
                    continue
                existing_parts = set(creature.get("parts", []))
                for part in item.get("parts", []):
                    if part not in existing_parts:
                        creature.setdefault("parts", []).append(part)
                        existing_parts.add(part)
                existing_geometry = {row[0] for row in creature.get("geometry", [])}
                for row in item.get("geometry", []):
                    if row[0] not in existing_geometry:
                        creature.setdefault("geometry", []).append(row)
                        existing_geometry.add(row[0])
        return creatures
    raise RuntimeError(f"Cannot find {variable} in {path}")


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
    return {"width": max_x - min_x, "height": max_y - min_y, "depth": max_z - min_z}


def row_map(rows: list[list[Any]]) -> dict[str, list[Any]]:
    return {str(row[0]): row for row in rows}


def row_extents(row: list[Any]) -> dict[str, float]:
    x1, y1, z1, x2, y2, z2 = row_bounds(row)
    return {
        "min_x": min(x1, x2),
        "max_x": max(x1, x2),
        "min_y": min(y1, y2),
        "max_y": max(y1, y2),
        "min_z": min(z1, z2),
        "max_z": max(z1, z2),
    }


def extents_intersect(first: dict[str, float], second: dict[str, float]) -> bool:
    return (
        min(first["max_x"], second["max_x"]) > max(first["min_x"], second["min_x"])
        and min(first["max_y"], second["max_y"]) > max(first["min_y"], second["min_y"])
        and min(first["max_z"], second["max_z"]) > max(first["min_z"], second["min_z"])
    )


def rows_intersect(rows_by_name: dict[str, list[Any]], first: str, second: str) -> bool:
    if first not in rows_by_name or second not in rows_by_name:
        return False
    return extents_intersect(row_extents(rows_by_name[first]), row_extents(rows_by_name[second]))


def row_center_z(rows_by_name: dict[str, list[Any]], name: str) -> float | None:
    row = rows_by_name.get(name)
    if row is None:
        return None
    bounds = row_extents(row)
    return (bounds["min_z"] + bounds["max_z"]) * 0.5


def infer_head_names(creature: dict[str, Any]) -> set[str]:
    names = {row[0] for row in creature.get("geometry", [])}
    inferred = set()
    for name in names:
        lower = name.lower()
        if name in HEAD_NAME_ALIASES:
            inferred.add(name)
            continue
        if not lower.endswith("_head"):
            continue
        if any(token in lower for token in HEAD_DETAIL_EXCLUDE_TOKENS):
            continue
        inferred.add(name)
    return inferred


def infer_body_names(creature: dict[str, Any]) -> set[str]:
    names = {row[0] for row in creature.get("geometry", [])}
    body_names = set()
    for name in names:
        lower = name.lower()
        if any(token == lower or token in lower for token in BODY_TOKENS):
            if "snake_body" not in lower and "hair" not in lower and "mandible" not in lower:
                body_names.add(name)
    return body_names


def text_fields(creature: dict[str, Any]) -> list[str]:
    fields: list[str] = []
    for key in (
            "scale_note", "texture_detail", "animation", "silhouette",
            "signature_features", "modeling_musts", "reject_if", "v2_design_freeze",
            "parts", "uv_plan",
    ):
        value = creature.get(key)
        if isinstance(value, str):
            fields.append(value)
        elif isinstance(value, dict):
            fields.extend(str(item) for item in value.values())
        elif isinstance(value, list):
            fields.extend(str(item) for item in value)
    return fields


def bad_proportion_terms(creature: dict[str, Any]) -> list[str]:
    bad_terms: list[str] = []
    for field in text_fields(creature):
        field_lower = field.lower()
        if any(context.lower() in field_lower for context in NEGATED_PROPORTION_CONTEXTS):
            continue
        for token in BAD_PROPORTION_WORDS:
            if token.lower() in field_lower and token not in bad_terms:
                bad_terms.append(token)
    return sorted(bad_terms)


def audit_creature(civilization: str, creature: dict[str, Any]) -> dict[str, Any]:
    cid = creature["id"]
    explicit_spec = CREATURE_SPEC.get(cid, {})
    explicit_spec_keys = set(explicit_spec.keys())
    spec = dict(explicit_spec)
    archetype = str(spec.get("archetype") or CREATURE_ARCHETYPES.get(cid, "generic"))
    archetype_limits = ARCHETYPE_LIMITS.get(archetype, {})
    for key, value in archetype_limits.items():
        spec.setdefault(key, value)
    rows = creature.get("geometry", [])
    rows_by_name = row_map(rows)
    head_names = set(spec.get("head_names") or infer_head_names(creature))
    body_names = set(spec.get("body_names") or infer_body_names(creature))
    head_extent = extent(rows, head_names)
    body_extent = extent(rows, body_names)
    single_head_ratios = []
    for head_name in sorted(head_names):
        item_extent = extent(rows, {head_name})
        single_head_ratios.append({
            "name": head_name,
            "extent": item_extent,
            "height_to_body_height": round(item_extent["height"] / max(1.0, body_extent["height"]), 4),
            "width_to_body_width": round(item_extent["width"] / max(1.0, body_extent["width"]), 4),
            "depth_to_body_depth": round(item_extent["depth"] / max(1.0, body_extent["depth"]), 4),
        })

    detail_names = [
        row[0] for row in rows
        if any(token in row[0].lower() for token in DETAIL_TOKENS)
        and row[0] not in head_names
    ]
    all_part_names = [row[0].lower() for row in rows]
    bad_words = bad_proportion_terms(creature)
    if any("huge_head" == name for name in head_names):
        bad_words.append("huge_head_part_name")

    checks: list[dict[str, Any]] = []

    def add(name: str, ok: bool, detail: dict[str, Any], severity: str = "blocker") -> None:
        checks.append({"name": name, "ok": ok, "severity": severity, "detail": detail})

    def explicit_or_warning(*keys: str) -> str:
        return "blocker" if any(key in explicit_spec_keys for key in keys) else "warning"

    add("head_and_body_geometry_present", bool(head_names and body_names), {
        "head_names": sorted(head_names),
        "body_names": sorted(body_names),
        "archetype": archetype,
    })
    min_detail_count = int(spec.get("min_detail_count", 3))
    add("modeled_face_detail_present", len(detail_names) >= min_detail_count, {
        "detail_count": len(detail_names),
        "min_detail_count": min_detail_count,
        "sample": sorted(detail_names)[:20],
    }, explicit_or_warning("min_detail_count", "required_detail_tokens"))
    required_detail_tokens = list(spec.get("required_detail_tokens", []))
    if required_detail_tokens:
        missing_tokens = [
            token for token in required_detail_tokens
            if not any(token in name for name in all_part_names)
        ]
        add("required_identity_detail_tokens_present", not missing_tokens, {
            "required_tokens": required_detail_tokens,
            "missing_tokens": missing_tokens,
        })
    add("no_art_direction_terms_that_reward_oversized_heads", not bad_words, {
        "bad_terms": bad_words,
    })

    max_height = float(spec.get("max_head_height_to_body_height", 1.55))
    min_height = float(spec.get("min_head_height_to_body_height", 0.25))
    if len(head_names) == 1 and body_extent["height"] > 0:
        ratios = single_head_ratios[0]
        height_ratio = ratios["height_to_body_height"]
        add("single_head_height_proportionate", min_height <= height_ratio <= max_height, {
            "ratio": height_ratio,
            "min": min_height,
            "max": max_height,
        }, explicit_or_warning("max_head_height_to_body_height", "min_head_height_to_body_height"))
        max_width = float(spec.get("max_head_width_to_body_width", 1.0))
        min_width = float(spec.get("min_head_width_to_body_width", 0.0))
        width_ratio = ratios["width_to_body_width"]
        add("single_head_width_proportionate", min_width <= width_ratio <= max_width, {
            "ratio": width_ratio,
            "min": min_width,
            "max": max_width,
            "source": "creature_spec" if "max_head_width_to_body_width" in spec else "default",
        }, explicit_or_warning("max_head_width_to_body_width", "min_head_width_to_body_width"))
        if "max_head_height_to_body_height" not in spec:
            add("single_head_default_height_not_oversized", height_ratio <= 1.1, {
                "ratio": height_ratio,
                "max": 1.1,
                "source": "default",
            })
        if "max_head_depth_to_body_depth" in spec:
            max_depth = float(spec["max_head_depth_to_body_depth"])
            depth_ratio = ratios["depth_to_body_depth"]
            add("single_head_depth_proportionate", depth_ratio <= max_depth, {
                "ratio": depth_ratio,
                "max": max_depth,
            }, explicit_or_warning("max_head_depth_to_body_depth"))
        detail_preserving_limits = dict(DETAIL_PRESERVING_HEAD_LIMITS.get(archetype, {}))
        detail_preserving_limits.update(spec.get("detail_preserving_head_limits", {}))
        if detail_preserving_limits:
            if "max_head_height_to_body_height" in detail_preserving_limits:
                detail_height_max = float(detail_preserving_limits["max_head_height_to_body_height"])
                add("detail_preserving_head_height_not_oversized", height_ratio <= detail_height_max, {
                    "ratio": height_ratio,
                    "max": detail_height_max,
                    "archetype": archetype,
                    "rule": "Do not enlarge the base head to carry facial readability; move precision into eyes, mouth, snout, ears, horns, mane, weapons, wings, tail, and UV detail.",
                })
            if "max_head_width_to_body_width" in detail_preserving_limits:
                detail_width_max = float(detail_preserving_limits["max_head_width_to_body_width"])
                add("detail_preserving_head_width_not_oversized", width_ratio <= detail_width_max, {
                    "ratio": width_ratio,
                    "max": detail_width_max,
                    "archetype": archetype,
                    "rule": "A head that approaches the body width is not acceptable unless the archetype explicitly requires it; use independent detail geometry instead of a larger head block.",
                })
            if "max_head_depth_to_body_depth" in detail_preserving_limits:
                detail_depth_max = float(detail_preserving_limits["max_head_depth_to_body_depth"])
                depth_ratio = ratios["depth_to_body_depth"]
                add("detail_preserving_head_depth_not_oversized", depth_ratio <= detail_depth_max, {
                    "ratio": depth_ratio,
                    "max": detail_depth_max,
                    "archetype": archetype,
                    "rule": "The head/snout depth must stay anatomical for the body; detail precision must come from named muzzle/nostril/mouth parts and UV placement.",
                })

    if spec.get("max_single_head_to_body_depth") and body_extent["depth"] > 0:
        max_depth_ratio = float(spec["max_single_head_to_body_depth"])
        worst = max(item["depth_to_body_depth"] for item in single_head_ratios) if single_head_ratios else 0.0
        add("multi_head_single_head_not_oversized", worst <= max_depth_ratio, {
            "worst_depth_to_body_depth": worst,
            "max": max_depth_ratio,
        })
    if spec.get("max_single_head_width_to_body_width") and body_extent["width"] > 0:
        max_width_ratio = float(spec["max_single_head_width_to_body_width"])
        worst = max(item["width_to_body_width"] for item in single_head_ratios) if single_head_ratios else 0.0
        add("multi_head_single_head_width_not_oversized", worst <= max_width_ratio, {
            "worst_width_to_body_width": worst,
            "max": max_width_ratio,
        })

    if spec.get("require_fan_wider_than_body") and body_extent["width"] > 0:
        min_fan_ratio = float(spec["require_fan_wider_than_body"])
        fan_ratio = round(head_extent["width"] / max(1.0, body_extent["width"]), 4)
        add("multi_head_fan_width_readable", fan_ratio >= min_fan_ratio, {
            "fan_width_to_body_width": fan_ratio,
            "min": min_fan_ratio,
        })

    overlap_pairs = list(spec.get("required_overlap_pairs", []))
    if overlap_pairs:
        overlap_results = [
            {
                "first": first,
                "second": second,
                "ok": rows_intersect(rows_by_name, first, second),
            }
            for first, second in overlap_pairs
        ]
        missing_overlaps = [
            f"{item['first']}<->{item['second']}"
            for item in overlap_results
            if not item["ok"]
        ]
        add("modeled_connection_overlap_intersects", not missing_overlaps, {
            "pairs": overlap_results,
            "missing": missing_overlaps,
            "rule": "Connection elements must geometrically overlap adjacent structure; named parts alone do not prove the head/neck/body is connected.",
        })
    forward_pairs = list(spec.get("required_forward_structure_pairs", []))
    if forward_pairs:
        forward_results = []
        for rear, front in forward_pairs:
            rear_z = row_center_z(rows_by_name, rear)
            front_z = row_center_z(rows_by_name, front)
            forward_results.append({
                "rear": rear,
                "front": front,
                "rear_center_z": rear_z,
                "front_center_z": front_z,
                "ok": rear_z is not None and front_z is not None and front_z < rear_z,
            })
        missing_forward = [
            f"{item['rear']}->{item['front']}"
            for item in forward_results
            if not item["ok"]
        ]
        add("modeled_forward_head_neck_structure_present", not missing_forward, {
            "pairs": forward_results,
            "missing": missing_forward,
            "rule": "Equine heads must read as a forward neck-to-head-to-snout chain, not a vertical tower.",
        })

    proportion_check_terms = (
        "head_height", "head_width", "head_depth",
        "single_head", "multi_head", "fan_width",
    )
    failed_proportion_checks = [
        check["name"]
        for check in checks
        if any(term in check["name"] for term in proportion_check_terms)
        and not check["ok"]
        and check.get("severity") == "blocker"
    ]
    failed_detail_checks = [
        check["name"]
        for check in checks
        if check["name"] in {
            "modeled_face_detail_present",
            "required_identity_detail_tokens_present",
            "no_art_direction_terms_that_reward_oversized_heads",
            "modeled_connection_overlap_intersects",
            "modeled_forward_head_neck_structure_present",
        }
        and not check["ok"]
        and check.get("severity") == "blocker"
    ]
    detail_first_readability_ok = not failed_proportion_checks and not failed_detail_checks
    add("detail_first_readability_not_head_scale", detail_first_readability_ok, {
        "rule": "Creature identity must be carried by named eyes, mouth, snout/jaw, horns, mane, wings, weapon, tail, scales, or UV detail; head size must stay inside body-archetype limits.",
        "failed_proportion_checks": failed_proportion_checks,
        "failed_detail_checks": failed_detail_checks,
        "detail_count": len(detail_names),
        "required_detail_tokens": required_detail_tokens,
    })

    failures = [check["name"] for check in checks if not check["ok"] and check.get("severity") == "blocker"]
    warnings = [check["name"] for check in checks if not check["ok"] and check.get("severity") == "warning"]
    return {
        "id": cid,
        "civilization": civilization,
        "entity_id": creature.get("entity_id"),
        "ok": not failures,
        "failures": failures,
        "warnings": warnings,
        "head_extent": head_extent,
        "body_extent": body_extent,
        "single_head_ratios": single_head_ratios,
        "archetype": archetype,
        "detail_count": len(detail_names),
        "detail_first_readability_ok": detail_first_readability_ok,
        "detail_first_readability": {
            "failed_proportion_checks": failed_proportion_checks,
            "failed_detail_checks": failed_detail_checks,
            "required_detail_tokens": required_detail_tokens,
            "detail_count": len(detail_names),
            "detail_sample": sorted(detail_names)[:20],
        },
        "checks": checks,
    }


def build_report() -> dict[str, Any]:
    creatures = []
    for civilization, path, variable in PIPELINES:
        for creature in load_creatures(path, variable):
            creatures.append(audit_creature(civilization, creature))
    blockers = [
        f"{item['civilization']}:{item['id']}:{failure}"
        for item in creatures
        for failure in item["failures"]
    ]
    warnings = [
        f"{item['civilization']}:{item['id']}:{warning}"
        for item in creatures
        for warning in item.get("warnings", [])
    ]
    return {
        "version": 1,
        "scope": "all_creature_proportion_and_detail_quality",
        "ok": not blockers,
        "creature_count": len(creatures),
        "blocker_count": len(blockers),
        "warning_count": len(warnings),
        "detail_first_readability_ok": all(item["detail_first_readability_ok"] for item in creatures),
        "detail_first_readability_count": sum(1 for item in creatures if item["detail_first_readability_ok"]),
        "blockers": blockers,
        "warnings": warnings,
        "rule": "Heads must remain proportionate to body archetype; readability must come from modeled detail parts, not abnormal head enlargement. Creature-specific/user-feedback rules are blockers; broad archetype thresholds are review warnings.",
        "detail_first_rule": "A proportionate head is required but not enough by itself: each creature must preserve named face and signature detail so readability is not recovered by enlarging the head, and shrinking a head may not remove eyes, mouth, snout, ear, horn, mane, wing, weapon, tail, or UV precision.",
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
        print(f"Creature Proportion Quality Gate: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"creatures: {payload['creature_count']}, blockers: {payload['blocker_count']}, warnings: {payload['warning_count']}")
        for blocker in payload["blockers"][:40]:
            print(f"blocker: {blocker}")
        for warning in payload["warnings"][:20]:
            print(f"warning: {warning}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
