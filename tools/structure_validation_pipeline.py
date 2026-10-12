#!/usr/bin/env python3
"""Structure NBT and template pool consistency gate for Mythic Age."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

from minecraft_structure_nbt import palette_block_names, read_root_compound, validate_structure_template


ROOT = Path(__file__).resolve().parents[1]
DATA_PACK_ROOT = ROOT / "src/main/resources/data"
MOD_DATA_ROOT = DATA_PACK_ROOT / "mythic_age"
REPORT_DIR = ROOT / "docs/preview_assets/structure_validation"
REPORT_PATH = REPORT_DIR / "structure_report.json"
WORLDGEN_STRUCTURE_DIR = MOD_DATA_ROOT / "worldgen/structure"
TEMPLATE_POOL_DIR = MOD_DATA_ROOT / "worldgen/template_pool"
STRUCTURES_DIR = MOD_DATA_ROOT / "structures"
MYTHIC_VILLAGE_PROCESSOR = ROOT / "src/main/java/com/example/mythic_age/worldgen/MythicVillageProcessor.java"

CIVILIZATIONS = ["greek", "egyptian", "chinese", "atlantean", "nordic"]
TEMPLE_SIZES = ["small", "medium", "large"]
REQUIRED_BLOCKS_BY_CIVILIZATION = {
    "greek": [
        "mythic_age:temple_greek_bricks",
        "mythic_age:temple_greek_slab",
        "mythic_age:temple_greek_stairs",
        "mythic_age:temple_greek_pillar",
        "mythic_age:temple_greek_relief",
        "mythic_age:relic_pedestal",
        "mythic_age:statue_greek_base",
        "mythic_age:statue_greek_legs",
        "mythic_age:statue_greek_body",
        "mythic_age:statue_greek_head",
    ],
    "egyptian": [
        "mythic_age:temple_egyptian_bricks",
        "mythic_age:temple_egyptian_slab",
        "mythic_age:temple_egyptian_stairs",
        "mythic_age:temple_egyptian_pillar",
        "mythic_age:temple_egyptian_relief",
        "mythic_age:egyptian_statue_pedestal",
        "mythic_age:relic_pedestal",
        "mythic_age:statue_egyptian_base",
        "mythic_age:statue_egyptian_legs",
        "mythic_age:statue_egyptian_body",
        "mythic_age:statue_egyptian_head",
    ],
    "chinese": [
        "mythic_age:temple_chinese_bricks",
        "mythic_age:temple_chinese_slab",
        "mythic_age:temple_chinese_stairs",
        "mythic_age:temple_chinese_pillar",
        "mythic_age:temple_chinese_relief",
        "mythic_age:relic_pedestal",
        "mythic_age:statue_chinese_base",
        "mythic_age:statue_chinese_legs",
        "mythic_age:statue_chinese_body",
        "mythic_age:statue_chinese_head",
    ],
    "atlantean": [
        "mythic_age:temple_atlantean_bricks",
        "mythic_age:temple_atlantean_slab",
        "mythic_age:temple_atlantean_stairs",
        "mythic_age:temple_atlantean_pillar",
        "mythic_age:temple_atlantean_relief",
        "mythic_age:relic_pedestal",
        "mythic_age:statue_atlantean_base",
        "mythic_age:statue_atlantean_legs",
        "mythic_age:statue_atlantean_body",
        "mythic_age:statue_atlantean_head",
    ],
    "nordic": [
        "mythic_age:temple_nordic_bricks",
        "mythic_age:temple_nordic_slab",
        "mythic_age:temple_nordic_stairs",
        "mythic_age:temple_nordic_pillar",
        "mythic_age:temple_nordic_relief",
        "mythic_age:relic_pedestal",
        "mythic_age:statue_nordic_base",
        "mythic_age:statue_nordic_legs",
        "mythic_age:statue_nordic_body",
        "mythic_age:statue_nordic_head",
    ],
}

FUNCTIONAL_POINT_KEYS = ["Bounds", "RelicPedestal", "StatueBase", "Entrance", "GuardianSpawn"]
EXPECTED_DIMENSIONS = {
    "greek": {
        "small": [11, 7, 11],
        "medium": [15, 9, 15],
        "large": [19, 11, 19],
    },
    "egyptian": {
        "small": [11, 7, 11],
        "medium": [15, 9, 15],
        "large": [19, 11, 19],
    },
    "chinese": {
        "small": [13, 9, 13],
        "medium": [17, 11, 17],
        "large": [21, 13, 21],
    },
    "atlantean": {
        "small": [15, 8, 15],
        "medium": [19, 10, 19],
        "large": [23, 12, 23],
    },
    "nordic": {
        "small": [9, 6, 11],
        "medium": [13, 8, 15],
        "large": [17, 10, 19],
    },
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def split_identifier(identifier: str, default_namespace: str = "minecraft") -> tuple[str, str]:
    if ":" in identifier:
        namespace, value = identifier.split(":", 1)
        return namespace, value
    return default_namespace, identifier


def structure_file_for_id(identifier: str) -> Path:
    namespace, value = split_identifier(identifier)
    return DATA_PACK_ROOT / namespace / "structures" / f"{value}.nbt"


def template_pool_file_for_id(identifier: str) -> Path:
    namespace, value = split_identifier(identifier)
    return DATA_PACK_ROOT / namespace / "worldgen/template_pool" / f"{value}.json"


def canonical_temple_structure_path(civilization: str, size: str) -> Path:
    return MOD_DATA_ROOT / "structures/village" / civilization / f"temple_{size}.nbt"


def inspect_structure_nbt(path: Path, civilization: str | None = None, size_name: str | None = None) -> dict[str, Any]:
    errors = validate_structure_template(path)
    size: list[int] | None = None
    block_count = 0
    palette: list[str] = []
    required_blocks: list[str] = []
    missing_required_blocks: list[str] = []
    theme_contract: dict[str, Any] = {
        "ok": False,
        "missing_theme_blocks": [],
        "expected_size": None,
        "actual_size": None,
        "size_ok": False,
        "errors": ["structure did not parse"],
    }

    functional_points: dict[str, Any] = {
        "ok": False,
        "points": {},
        "missing": FUNCTIONAL_POINT_KEYS,
        "errors": ["structure did not parse"],
    }

    if not errors:
        structure = read_root_compound(path)
        raw_size = structure.get("size")
        if isinstance(raw_size, list):
            size = raw_size
        raw_blocks = structure.get("blocks")
        if isinstance(raw_blocks, list):
            block_count = len(raw_blocks)
        palette = palette_block_names(structure)
        if civilization in REQUIRED_BLOCKS_BY_CIVILIZATION:
            required_blocks = REQUIRED_BLOCKS_BY_CIVILIZATION[civilization]
            missing_required_blocks = [block for block in required_blocks if block not in palette]
            errors.extend(f"missing required block in palette: {block}" for block in missing_required_blocks)
        theme_contract = inspect_theme_contract(structure, palette, civilization, size_name)
        errors.extend(f"theme contract: {error}" for error in theme_contract["errors"])
        functional_points = infer_functional_points(structure, palette, civilization)
        errors.extend(f"missing functional point: {point}" for point in functional_points["missing"])

    return {
        "parse_ok": not errors,
        "size": size,
        "block_count": block_count,
        "palette_count": len(palette),
        "required_blocks": required_blocks,
        "missing_required_blocks": missing_required_blocks,
        "theme_contract": theme_contract,
        "functional_points": functional_points,
        "errors": errors,
    }


def block_positions_by_name(structure: dict[str, Any], palette: list[str]) -> dict[str, list[list[int]]]:
    positions: dict[str, list[list[int]]] = {}
    for block in structure.get("blocks", []):
        if not isinstance(block, dict):
            continue
        state_index = block.get("state")
        pos = block.get("pos")
        if not isinstance(state_index, int) or state_index < 0 or state_index >= len(palette):
            continue
        if not isinstance(pos, list) or len(pos) != 3:
            continue
        positions.setdefault(palette[state_index], []).append(pos)
    return positions


def infer_functional_points(structure: dict[str, Any], palette: list[str], civilization: str | None) -> dict[str, Any]:
    size = structure.get("size")
    if not isinstance(size, list) or len(size) != 3:
        return {
            "ok": False,
            "points": {},
            "missing": FUNCTIONAL_POINT_KEYS,
            "errors": ["invalid size"],
        }

    positions = block_positions_by_name(structure, palette)
    relic_positions = positions.get("mythic_age:relic_pedestal", [])
    statue_base_name = f"mythic_age:statue_{civilization}_base" if civilization else ""
    statue_positions = positions.get(statue_base_name, []) if statue_base_name else []
    center_x = size[0] // 2
    center_z = size[2] // 2
    front_z = 0

    points: dict[str, Any] = {
        "Bounds": {
            "min": [0, 0, 0],
            "max": [size[0] - 1, size[1] - 1, size[2] - 1],
            "size": size,
        },
        "Entrance": {
            "pos": [center_x, 1, front_z],
            "facing": "south_to_center",
        },
        "GuardianSpawn": {
            "pos": [center_x, 2, min(size[2] - 2, center_z + 2)],
            "strategy": "derived_from_temple_center",
        },
    }
    if relic_positions:
        points["RelicPedestal"] = {"pos": relic_positions[0], "count": len(relic_positions)}
    if statue_positions:
        points["StatueBase"] = {"pos": statue_positions[0], "count": len(statue_positions)}

    missing = [key for key in FUNCTIONAL_POINT_KEYS if key not in points]
    errors: list[str] = []
    if len(relic_positions) != 1:
        errors.append(f"expected exactly one RelicPedestal, found {len(relic_positions)}")
    if len(statue_positions) != 1:
        errors.append(f"expected exactly one StatueBase, found {len(statue_positions)}")
    if "RelicPedestal" in points and points["RelicPedestal"]["pos"][0] != center_x:
        errors.append("RelicPedestal is not centered on X axis")
    if "StatueBase" in points and points["StatueBase"]["pos"][0] != center_x:
        errors.append("StatueBase is not centered on X axis")

    return {
        "ok": not missing and not errors,
        "points": points,
        "missing": missing,
        "errors": errors,
    }


def inspect_theme_contract(
    structure: dict[str, Any],
    palette: list[str],
    civilization: str | None,
    size_name: str | None,
) -> dict[str, Any]:
    if civilization not in REQUIRED_BLOCKS_BY_CIVILIZATION:
        return {
            "ok": True,
            "civilization": civilization,
            "size": size_name,
            "theme_block_counts": {},
            "missing_theme_blocks": [],
            "expected_size": None,
            "actual_size": structure.get("size"),
            "size_ok": True,
            "errors": [],
        }

    expected_blocks = REQUIRED_BLOCKS_BY_CIVILIZATION[civilization]
    positions = block_positions_by_name(structure, palette)
    theme_block_counts = {block: len(positions.get(block, [])) for block in expected_blocks}
    missing = [block for block, count in theme_block_counts.items() if count == 0]

    actual_size = structure.get("size") if isinstance(structure.get("size"), list) else None
    expected_size = EXPECTED_DIMENSIONS.get(civilization, {}).get(size_name) if size_name else None
    size_ok = expected_size is None or actual_size == expected_size

    errors: list[str] = []
    if missing:
        errors.append("missing civilization theme blocks: " + ", ".join(missing))
    if not size_ok:
        errors.append(f"expected {civilization} {size_name} dimensions {expected_size}, found {actual_size}")

    return {
        "ok": not errors,
        "civilization": civilization,
        "size": size_name,
        "theme_block_counts": theme_block_counts,
        "missing_theme_blocks": missing,
        "expected_size": expected_size,
        "actual_size": actual_size,
        "size_ok": size_ok,
        "errors": errors,
    }


def collect_worldgen_structures(failures: list[str]) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for path in sorted(WORLDGEN_STRUCTURE_DIR.glob("*.json")):
        try:
            payload = load_json(path)
        except json.JSONDecodeError as exc:
            failures.append(f"invalid worldgen structure json: {project_path(path)}: {exc}")
            continue

        start_pool = payload.get("start_pool")
        pool_path = template_pool_file_for_id(start_pool) if isinstance(start_pool, str) else None
        exists = pool_path.exists() if pool_path is not None else False
        if not isinstance(start_pool, str):
            failures.append(f"missing start_pool in worldgen structure: {project_path(path)}")
        elif not exists:
            failures.append(f"missing template pool for {start_pool}: {project_path(pool_path)}")

        entries.append(
            {
                "path": project_path(path),
                "start_pool": start_pool,
                "template_pool_path": project_path(pool_path) if pool_path is not None else None,
                "template_pool_exists": exists,
                "ok": isinstance(start_pool, str) and exists,
            }
        )
    return entries


def template_pool_locations(path: Path, failures: list[str]) -> list[dict[str, Any]]:
    try:
        payload = load_json(path)
    except json.JSONDecodeError as exc:
        failures.append(f"invalid template pool json: {project_path(path)}: {exc}")
        return []

    locations: list[dict[str, Any]] = []
    for index, entry in enumerate(payload.get("elements", [])):
        element = entry.get("element", {}) if isinstance(entry, dict) else {}
        element_type = element.get("element_type")
        location = element.get("location")
        if element_type != "minecraft:single_pool_element":
            continue
        if not isinstance(location, str):
            failures.append(f"missing single_pool_element location in {project_path(path)} element {index}")
            continue

        structure_path = structure_file_for_id(location)
        exists = structure_path.exists()
        if not exists:
            failures.append(f"template pool references missing structure {location}: {project_path(structure_path)}")
        locations.append(
            {
                "location": location,
                "structure_path": project_path(structure_path),
                "exists": exists,
                "ok": exists,
            }
        )
    return locations


def collect_template_pools(failures: list[str]) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for path in sorted(TEMPLATE_POOL_DIR.rglob("*.json")):
        locations = template_pool_locations(path, failures)
        entries.append(
            {
                "path": project_path(path),
                "locations": locations,
                "missing_locations": [location for location in locations if not location["exists"]],
                "ok": all(location["exists"] for location in locations),
            }
        )
    return entries


def collect_canonical_expectations(failures: list[str]) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for civilization in CIVILIZATIONS:
        for size in TEMPLE_SIZES:
            path = canonical_temple_structure_path(civilization, size)
            exists = path.exists()
            nbt = inspect_structure_nbt(path, civilization, size) if exists else None
            if not exists:
                failures.append(f"missing canonical 1.0 temple structure: {project_path(path)}")
            elif nbt and not nbt["parse_ok"]:
                for error in nbt["errors"]:
                    failures.append(f"invalid canonical structure {project_path(path)}: {error}")
            elif nbt and not nbt["theme_contract"]["ok"]:
                for error in nbt["theme_contract"]["errors"]:
                    failures.append(f"invalid theme contract in {project_path(path)}: {error}")
            elif nbt and not nbt["functional_points"]["ok"]:
                for error in nbt["functional_points"]["errors"]:
                    failures.append(f"invalid functional point in {project_path(path)}: {error}")
                for missing in nbt["functional_points"]["missing"]:
                    failures.append(f"missing functional point in {project_path(path)}: {missing}")
            entries.append(
                {
                    "civilization": civilization,
                    "size": size,
                    "path": project_path(path),
                    "exists": exists,
                    "nbt": nbt,
                    "ok": exists and bool(nbt) and nbt["parse_ok"] and nbt["theme_contract"]["ok"] and nbt["functional_points"]["ok"],
                }
            )
    return entries


def collect_code_nbt_paths(failures: list[str], warnings: list[str]) -> list[dict[str, Any]]:
    if not MYTHIC_VILLAGE_PROCESSOR.exists():
        failures.append(f"missing MythicVillageProcessor: {project_path(MYTHIC_VILLAGE_PROCESSOR)}")
        return []

    text = MYTHIC_VILLAGE_PROCESSOR.read_text(encoding="utf-8")
    method_start = text.find("private static String getTempleNBTPath")
    method_end = text.find("private static boolean generateTempleWithCode")
    method_text = text[method_start:method_end] if method_start != -1 and method_end != -1 else text
    case_pattern = re.compile(r"case\s+([A-Z_]+)\s*->\s*(?:\"([^\"]+\.nbt)\"|null)")
    found: dict[str, str | None] = {}
    for match in case_pattern.finditer(method_text):
        civilization, path = match.groups()
        found[civilization.lower()] = path or None
    entries: list[dict[str, Any]] = []
    for civilization in CIVILIZATIONS:
        code_path = found.get(civilization)
        if code_path is None:
            failures.append(f"MythicVillageProcessor has no structure NBT path for {civilization}")
            entries.append(
                {
                    "civilization": civilization,
                    "code_path": None,
                    "file_path": None,
                    "exists": False,
                    "canonical": False,
                    "ok": False,
                }
            )
            continue

        file_path = MOD_DATA_ROOT / code_path
        exists = file_path.exists()
        canonical_prefix = f"structures/village/{civilization}/temple_"
        canonical = code_path.startswith(canonical_prefix)
        if not exists:
            failures.append(f"MythicVillageProcessor references missing {civilization} NBT: {project_path(file_path)}")
        if exists and not canonical:
            warnings.append(f"MythicVillageProcessor {civilization} NBT path is non-canonical: {code_path}")
        nbt = inspect_structure_nbt(file_path, civilization) if exists else None
        if nbt and not nbt["parse_ok"]:
            for error in nbt["errors"]:
                failures.append(f"MythicVillageProcessor references invalid {civilization} NBT: {error}")

        entries.append(
            {
                "civilization": civilization,
                "code_path": code_path,
                "file_path": project_path(file_path),
                "exists": exists,
                "canonical": canonical,
                "nbt": nbt,
                "ok": exists and canonical and bool(nbt) and nbt["parse_ok"],
            }
        )
    return entries


def collect_existing_nbt_files(warnings: list[str]) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    if not STRUCTURES_DIR.exists():
        warnings.append(f"structures directory does not exist: {project_path(STRUCTURES_DIR)}")
        return entries

    for path in sorted(STRUCTURES_DIR.rglob("*.nbt")):
        relative = path.relative_to(STRUCTURES_DIR)
        civilization = relative.parts[1] if len(relative.parts) > 1 and relative.parts[0] == "village" else None
        canonical = (
            len(relative.parts) == 3
            and relative.parts[0] == "village"
            and relative.parts[1] in CIVILIZATIONS
            and relative.name in {f"temple_{size}.nbt" for size in TEMPLE_SIZES}
        )
        nbt = inspect_structure_nbt(path, civilization) if canonical else inspect_structure_nbt(path)
        if not nbt["parse_ok"]:
            warnings.append(f"existing structure has validation errors: {project_path(path)}")
        if not canonical:
            warnings.append(f"existing structure is outside canonical 1.0 path: {project_path(path)}")
        entries.append(
            {
                "path": project_path(path),
                "size_bytes": path.stat().st_size,
                "canonical": canonical,
                "nbt": nbt,
            }
        )
    return entries


def collect_non_data_artifacts(warnings: list[str]) -> list[str]:
    artifacts = [project_path(path) for path in sorted(MOD_DATA_ROOT.rglob(".DS_Store"))]
    for artifact in artifacts:
        warnings.append(f"non-data artifact under data pack root: {artifact}")
    return artifacts


def generate_report() -> tuple[int, dict[str, Any]]:
    failures: list[str] = []
    warnings: list[str] = []
    canonical_expectations = collect_canonical_expectations(failures)
    worldgen_structures = collect_worldgen_structures(failures)
    template_pools = collect_template_pools(failures)
    code_nbt_paths = collect_code_nbt_paths(failures, warnings)
    existing_nbt_files = collect_existing_nbt_files(warnings)
    non_data_artifacts = collect_non_data_artifacts(warnings)

    payload = {
        "version": 1,
        "scope": "structure_nbt_template_pool_consistency",
        "ok": not failures,
        "summary": {
            "civilizations": CIVILIZATIONS,
            "temple_sizes": TEMPLE_SIZES,
            "canonical_expected_count": len(canonical_expectations),
            "canonical_existing_count": sum(1 for item in canonical_expectations if item["exists"]),
            "worldgen_structure_count": len(worldgen_structures),
            "template_pool_count": len(template_pools),
            "template_pool_location_count": sum(len(pool["locations"]) for pool in template_pools),
            "missing_template_pool_locations": sum(len(pool["missing_locations"]) for pool in template_pools),
            "code_nbt_path_count": len(code_nbt_paths),
            "code_nbt_existing_count": sum(1 for item in code_nbt_paths if item["exists"]),
            "existing_nbt_file_count": len(existing_nbt_files),
            "canonical_valid_count": sum(1 for item in canonical_expectations if item["ok"]),
            "theme_contract_checked_count": sum(1 for item in canonical_expectations if item["nbt"] and item["nbt"]["theme_contract"]["ok"]),
            "theme_contract_expected_count": len(canonical_expectations),
            "functional_point_checked_count": sum(1 for item in canonical_expectations if item["nbt"] and item["nbt"]["functional_points"]["ok"]),
            "functional_point_expected_count": len(canonical_expectations),
            "failure_count": len(failures),
            "warning_count": len(warnings),
        },
        "canonical_expectations": canonical_expectations,
        "worldgen_structures": worldgen_structures,
        "template_pools": template_pools,
        "code_nbt_paths": code_nbt_paths,
        "existing_nbt_files": existing_nbt_files,
        "non_data_artifacts": non_data_artifacts,
        "failures": failures,
        "warnings": warnings,
    }

    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return (0 if not failures else 1), payload


def audit() -> int:
    code, payload = generate_report()
    if code == 0:
        print("Structure Validation Gate: PASS")
    else:
        print("Structure Validation Gate: FAIL")
    print(f"report: {REPORT_PATH}")
    summary = payload["summary"]
    print(
        "summary: "
        f"canonical {summary['canonical_existing_count']}/{summary['canonical_expected_count']} present, "
        f"template pool missing locations {summary['missing_template_pool_locations']}, "
        f"code NBT paths {summary['code_nbt_existing_count']}/{summary['code_nbt_path_count']} present, "
        f"failures {summary['failure_count']}, warnings {summary['warning_count']}"
    )
    for failure in payload["failures"][:20]:
        print(f"- {failure}")
    remaining = len(payload["failures"]) - 20
    if remaining > 0:
        print(f"- ... {remaining} more failures in report")
    return code


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["report", "audit", "all"])
    args = parser.parse_args()
    if args.command == "report":
        code, payload = generate_report()
        print(json.dumps(payload, indent=2, ensure_ascii=False))
        return code
    if args.command in {"audit", "all"}:
        return audit()
    return 0


if __name__ == "__main__":
    sys.exit(main())
