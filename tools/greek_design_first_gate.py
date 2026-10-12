#!/usr/bin/env python3
"""Design-first gate for Greek creature remodeling.

This gate intentionally runs before Blockbench/model generation. It prevents a
rejected creature from being rebuilt from old V2 assets while its V6 modeling
spec is still waiting for review.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "docs/preview_assets/greek_creatures/design_first_gate.json"

BLOCKED_MODELING_SPECS = {
    "pegasus": ROOT / "docs/美术资源/greek_creatures/design_blueprints/pegasus_v6_modeling_spec_contract.md",
    "hydra": ROOT / "docs/美术资源/greek_creatures/design_blueprints/hydra_v6_modeling_spec_contract.md",
}

REQUIRED_KEYS = {
    "design_status",
    "modeling_allowed",
    "previous_assets_status",
    "approval_sheet",
    "supersedes",
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def read_front_matter(path: Path) -> dict[str, str]:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return {}
    data: dict[str, str] = {}
    for line in lines[1:]:
        if line.strip() == "---":
            break
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        data[key.strip()] = value.strip().strip('"')
    return data


def audit_modeling_spec(creature_id: str, path: Path) -> dict[str, Any]:
    item: dict[str, Any] = {
        "id": creature_id,
        "modeling_spec_contract": project_path(path),
        "exists": path.exists(),
        "front_matter": {},
        "missing_keys": [],
        "approval_sheet": None,
        "approval_sheet_exists": False,
        "design_status": None,
        "modeling_allowed": False,
        "ok_for_modeling": False,
        "next_action": "create_or_review_v6_modeling_spec",
    }
    if not path.exists():
        item["missing_keys"] = sorted(REQUIRED_KEYS)
        return item
    front_matter = read_front_matter(path)
    missing = sorted(REQUIRED_KEYS - set(front_matter))
    design_status = front_matter.get("design_status")
    approval_sheet = front_matter.get("approval_sheet")
    approval_sheet_path = ROOT / approval_sheet if approval_sheet else None
    approval_sheet_exists = bool(approval_sheet_path and approval_sheet_path.exists())
    modeling_allowed = front_matter.get("modeling_allowed", "").lower() == "true"
    ok_for_modeling = (
        not missing
        and design_status == "approved_for_modeling"
        and modeling_allowed
        and front_matter.get("previous_assets_status", "").startswith("rejected")
        and approval_sheet_exists
    )
    item.update({
        "front_matter": front_matter,
        "missing_keys": missing,
        "approval_sheet": approval_sheet,
        "approval_sheet_exists": approval_sheet_exists,
        "design_status": design_status,
        "modeling_allowed": modeling_allowed,
        "ok_for_modeling": ok_for_modeling,
        "next_action": (
            "proceed_to_blockbench_modeling_from_v6_modeling_spec"
            if ok_for_modeling
            else "await_user_approval_of_v6_modeling_spec_before_modeling"
        ),
    })
    return item


def build_report() -> dict[str, Any]:
    creatures = [
        audit_modeling_spec(creature_id, path)
        for creature_id, path in BLOCKED_MODELING_SPECS.items()
    ]
    blocked = [
        item["id"]
        for item in creatures
        if not item["ok_for_modeling"]
    ]
    return {
        "version": 1,
        "scope": "greek_design_first_gate",
        "ok": not blocked,
        "rule": (
            "Pegasus and Hydra V2 assets are rejected. Remodeling is blocked "
            "until the corresponding V6 modeling spec contract and spec sheet are "
            "approved for modeling, and modeling_allowed=true."
        ),
        "blocked_count": len(blocked),
        "blocked": blocked,
        "creatures": creatures,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_report()
        REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        status = "PASS" if payload["ok"] else "BLOCKED"
        print(f"Greek Design-First Gate: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"blocked: {payload['blocked_count']} {payload['blocked']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
