#!/usr/bin/env python3
"""Validate the Mythic Age 1.0 NBT persistence target document."""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TARGET_DOC = ROOT / "docs/系统设计/MythicAgeMod_1.0_NBT状态持久化目标_2026-06-11.md"
ENTITY_NBT_REPORT = ROOT / "docs/preview_assets/entity_validation/nbt_schema_report.json"
REPORT_DIR = ROOT / "docs/preview_assets/nbt_target_validation"
REPORT_PATH = REPORT_DIR / "nbt_target_report.json"

ALLOWED_STATUSES = {"required", "runtime_only", "covered", "missing"}
UNDECIDED_STATUSES = {"candidate", "todo", "tbd", "undecided"}

PERSISTENT_TARGET_ASSERTIONS = [
    {"id": "summoner_source", "tokens": ["MythicAge_Summoner"], "allowed_statuses": ["required", "covered"]},
    {"id": "lightning_storm_state", "tokens": ["Lightning Storm"], "allowed_statuses": ["covered"]},
    {"id": "earthquake_state", "tokens": ["Earthquake"], "allowed_statuses": ["covered"]},
    {"id": "fimbulwinter_state", "tokens": ["Fimbulwinter"], "allowed_statuses": ["required", "covered"]},
    {"id": "oceanic_wrath_state", "tokens": ["Oceanic Wrath"], "allowed_statuses": ["required", "covered"]},
    {"id": "certificate_production_state", "tokens": ["CivilizationProductionState"], "allowed_statuses": ["required", "covered"]},
    {"id": "temple_function_markers", "tokens": ["GuardianSpawn", "RelicPedestal", "StatueBase"], "allowed_statuses": ["required", "covered"]},
]

RUNTIME_TARGET_ASSERTIONS = [
    {"id": "special_cooldown_runtime", "tokens": ["MythicAge_SpecialCooldown"]},
    {"id": "pegasus_flight_energy_runtime", "tokens": ["pegasus", "FlightEnergy"]},
    {"id": "underworld_passage_runtime", "tokens": ["Underworld Passage"]},
    {"id": "crystal_barrier_runtime", "tokens": ["Crystal Barrier"]},
    {"id": "abyssal_rift_runtime", "tokens": ["Abyssal Rift"]},
]

COVERED_TARGET_ASSERTIONS = [
    {"id": "walking_tree_lifetime", "tokens": ["walking_tree", "walkingTreeLifetimeTicks"]},
    {"id": "statue_base_storage", "tokens": ["StatueBaseBlockEntity", "StoredCertificates"]},
    {"id": "egyptian_statue_pedestal", "tokens": ["EgyptianStatuePedestalBlockEntity", "productionTick"]},
    {"id": "canonical_structure_nbt", "tokens": ["canonical NBT", "DataVersion"]},
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def is_separator_row(cells: list[str]) -> bool:
    return all(re.fullmatch(r":?-{3,}:?", cell.strip()) for cell in cells if cell.strip())


def split_markdown_row(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def status_in_cells(cells: list[str]) -> tuple[str | None, int | None, list[str]]:
    exact_statuses: list[str] = []
    for index, cell in enumerate(cells):
        match = re.fullmatch(r"`([A-Za-z_]+)`", cell.strip())
        if not match:
            continue
        status = match.group(1)
        if status in ALLOWED_STATUSES or status in UNDECIDED_STATUSES:
            exact_statuses.append(status)
            return status, index, exact_statuses
    return None, None, exact_statuses


def parse_target_rows(text: str) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    rows: list[dict[str, Any]] = []
    unknown_status_rows: list[dict[str, Any]] = []
    section = ""

    for line_number, line in enumerate(text.splitlines(), 1):
        if line.startswith("## "):
            section = line.lstrip("#").strip()
            continue

        stripped = line.strip()
        if not stripped.startswith("|"):
            continue

        cells = split_markdown_row(stripped)
        if len(cells) < 4 or is_separator_row(cells):
            continue

        status, status_column, _ = status_in_cells(cells)
        if status is None:
            continue

        entry = {
            "line": line_number,
            "section": section,
            "cells": cells,
            "status": status,
            "status_column": status_column,
            "raw": stripped,
        }
        if status not in ALLOWED_STATUSES:
            unknown_status_rows.append(entry)
        rows.append(entry)

    return rows, unknown_status_rows


def compact_entry(entry: dict[str, Any]) -> dict[str, Any]:
    cells = entry["cells"]
    status_column = entry["status_column"]
    left = cells[:status_column]
    right = cells[status_column + 1 :]
    owner = left[0] if left else ""
    field = left[1] if len(left) > 1 else left[0] if left else ""
    return {
        "line": entry["line"],
        "section": entry["section"],
        "owner_or_system": owner,
        "field": field,
        "status": entry["status"],
        "type_or_scope": left[2] if len(left) > 2 else "",
        "note": right[-1] if right else "",
        "raw": entry["raw"],
    }


def assertion_result(assertion: dict[str, Any], rows: list[dict[str, Any]], expected_status: str) -> dict[str, Any]:
    tokens = assertion["tokens"]
    matches = [
        row for row in rows
        if row["status"] == expected_status and all(token in row["raw"] for token in tokens)
    ]
    return {
        "id": assertion["id"],
        "expected_status": expected_status,
        "tokens": tokens,
        "matched": bool(matches),
        "lines": [row["line"] for row in matches],
    }


def status_set_assertion_result(assertion: dict[str, Any], rows: list[dict[str, Any]]) -> dict[str, Any]:
    tokens = assertion["tokens"]
    allowed_statuses = assertion["allowed_statuses"]
    matches = [
        row for row in rows
        if row["status"] in allowed_statuses and all(token in row["raw"] for token in tokens)
    ]
    return {
        "id": assertion["id"],
        "allowed_statuses": allowed_statuses,
        "tokens": tokens,
        "matched": bool(matches),
        "lines": [row["line"] for row in matches],
        "matched_statuses": sorted({row["status"] for row in matches}),
    }


def implemented_schema_summary() -> dict[str, Any]:
    data = load_json(ENTITY_NBT_REPORT, {})
    schemas = data.get("schemas", []) if isinstance(data, dict) else []
    owners = [
        schema.get("owner")
        for schema in schemas
        if isinstance(schema, dict) and isinstance(schema.get("owner"), str)
    ]
    return {
        "path": project_path(ENTITY_NBT_REPORT),
        "exists": ENTITY_NBT_REPORT.exists(),
        "owner_count": len(owners),
        "owners": owners,
    }


def build_report() -> dict[str, Any]:
    text = read_text(TARGET_DOC)
    rows, unknown_status_rows = parse_target_rows(text)
    status_counts = Counter(row["status"] for row in rows)
    for status in sorted(ALLOWED_STATUSES | UNDECIDED_STATUSES):
        status_counts.setdefault(status, 0)

    persistent_results = [
        status_set_assertion_result(assertion, rows)
        for assertion in PERSISTENT_TARGET_ASSERTIONS
    ]
    runtime_results = [
        assertion_result(assertion, rows, "runtime_only")
        for assertion in RUNTIME_TARGET_ASSERTIONS
    ]
    covered_results = [
        assertion_result(assertion, rows, "covered")
        for assertion in COVERED_TARGET_ASSERTIONS
    ]

    undecided_rows = [
        row for row in rows
        if row["status"] in UNDECIDED_STATUSES or "`candidate`" in row["raw"]
    ]
    missing_rows = [row for row in rows if row["status"] == "missing"]
    failures: list[str] = []
    if not TARGET_DOC.exists():
        failures.append(f"missing target doc: {project_path(TARGET_DOC)}")
    if not rows:
        failures.append("no NBT target table rows parsed")
    if undecided_rows:
        failures.append(f"undecided target rows remain: {len(undecided_rows)}")
    if unknown_status_rows:
        failures.append(f"unknown status rows found: {len(unknown_status_rows)}")
    if missing_rows:
        failures.append(f"explicit missing target rows found: {len(missing_rows)}")

    for result in persistent_results + runtime_results + covered_results:
        if not result["matched"]:
            expected = result.get("expected_status") or "/".join(result.get("allowed_statuses", []))
            failures.append(f"missing {expected} assertion: {result['id']}")

    target_gate_ok = not failures
    required_targets = [compact_entry(row) for row in rows if row["status"] == "required"]

    return {
        "version": 1,
        "scope": "mythic_age_1_0_nbt_target_validation",
        "ok": target_gate_ok,
        "target_gate_ok": target_gate_ok,
        "release_ready": False,
        "source": project_path(TARGET_DOC),
        "row_count": len(rows),
        "status_counts": dict(sorted(status_counts.items())),
        "undecided_row_count": len(undecided_rows),
        "unknown_status_row_count": len(unknown_status_rows),
        "missing_row_count": len(missing_rows),
        "persistent_target_assertions": persistent_results,
        "runtime_only_assertions": runtime_results,
        "covered_assertions": covered_results,
        "required_targets": required_targets,
        "required_targets_pending_implementation": required_targets,
        "implemented_schema": implemented_schema_summary(),
        "failures": failures,
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
        print(f"Mythic Age NBT Target Gate: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"rows: {payload['row_count']}")
        print(f"status_counts: {payload['status_counts']}")
        if payload["failures"]:
            for failure in payload["failures"]:
                print(f"failure: {failure}", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
