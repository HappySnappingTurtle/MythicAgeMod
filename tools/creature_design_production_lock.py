#!/usr/bin/env python3
"""Lock creature art production until design approval is explicit.

Design-first reports describe what is missing. This lock turns that state into
an explicit production policy for downstream tools: unapproved creatures cannot
advance model, UV, texture, runtime Java model, visual ledger, or motion gates.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DESIGN_FIRST_GATE = ROOT / "docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json"
ART_PRODUCTION_ROADMAP = ROOT / "docs/preview_assets/creature_art_production_roadmap/creature_art_production_roadmap.json"
V4_CANDIDATE_PACKAGE = ROOT / "docs/preview_assets/greek_creatures/design_approval/v4/pegasus_hydra_v4_candidate_package.json"
V5_CANDIDATE_PACKAGE = ROOT / "docs/preview_assets/greek_creatures/design_approval/v5/pegasus_hydra_v5_design_approval_package.json"
V6_CANDIDATE_PACKAGE = ROOT / "docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json"
DESIGN_CORRECTNESS_GATE = ROOT / "docs/preview_assets/greek_creatures/design_correctness/pegasus_hydra_design_correctness_gate.json"
DESIGN_APPROVAL_REVIEW_PACKAGE = ROOT / "docs/preview_assets/creature_design_approval_review/creature_design_approval_review_package.json"
DESIGN_APPROVAL_REVIEW_MARKDOWN = ROOT / "docs/项目管理/MythicAgeMod_1.0生物设计审批总包_2026-06-15.md"
DESIGN_APPROVAL_REVIEW_CONTACT_SHEET = ROOT / "docs/preview_assets/creature_design_approval_review/creature_design_approval_contact_sheet.png"
OUT_DIR = ROOT / "docs/preview_assets/creature_design_production_lock"
REPORT_PATH = OUT_DIR / "creature_design_production_lock.json"
MARKDOWN_PATH = ROOT / "docs/项目管理/MythicAgeMod_1.0生物设计生产锁_2026-06-15.md"

LOCKED_STEPS = [
    "blockbench_model_built_or_polished",
    "uv_template_locked",
    "texture_painted_from_uv",
    "blockbench_front_side_three_quarter_review",
    "java_runtime_model_synced",
    "visual_ledger_approved",
    "motion_capture_unblocked",
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def creature_key(item: dict[str, Any]) -> str:
    return f"{item.get('civilization')}:{item.get('id')}"


def candidate_map(payload: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for creature in payload.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        creature_id = creature.get("id")
        if isinstance(creature_id, str):
            result[f"greek:{creature_id}"] = creature
    return result


def roadmap_map(payload: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for creature in payload.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        key = creature.get("key")
        if isinstance(key, str):
            result[key] = creature
    return result


def lock_level(design_status: str, priority: str | None) -> str:
    if design_status.startswith("blocked") or priority == "P0-DESIGN-BLOCKER":
        return "hard_lock_rejected_design"
    return "approval_lock_manual_review_required"


def next_action_for(level: str, candidate: dict[str, Any] | None) -> str:
    if level == "hard_lock_rejected_design":
        if candidate:
            return "review_latest_candidate_contract_and_sheet_before_any_modeling"
        return "redraw_and_approve_design_before_any_modeling"
    return "human_review_design_package_and_record_explicit_approval"


def build_report() -> dict[str, Any]:
    design_gate = load_json(DESIGN_FIRST_GATE, {})
    roadmap = load_json(ART_PRODUCTION_ROADMAP, {})
    v4_candidates = candidate_map(load_json(V4_CANDIDATE_PACKAGE, {}))
    v5_candidates = candidate_map(load_json(V5_CANDIDATE_PACKAGE, {}))
    v6_candidates = candidate_map(load_json(V6_CANDIDATE_PACKAGE, {}))
    roadmap_by_key = roadmap_map(roadmap)
    correctness = load_json(DESIGN_CORRECTNESS_GATE, {})

    locks: list[dict[str, Any]] = []
    for creature in design_gate.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        key = creature_key(creature)
        approved = bool(creature.get("approved_for_modeling"))
        roadmap_item = roadmap_by_key.get(key, {})
        priority = roadmap_item.get("priority") if isinstance(roadmap_item, dict) else None
        design_status = str(creature.get("status", "unknown"))
        level = "unlocked" if approved else lock_level(design_status, str(priority) if priority else None)
        candidate = v6_candidates.get(key) or v5_candidates.get(key) or v4_candidates.get(key)
        locked_steps = [] if approved else LOCKED_STEPS
        locks.append({
            "key": key,
            "civilization": creature.get("civilization"),
            "id": creature.get("id"),
            "approved_for_modeling": approved,
            "design_status": design_status,
            "lock_level": level,
            "batch": roadmap_item.get("batch") if isinstance(roadmap_item, dict) else None,
            "priority": priority,
            "human_decision": roadmap_item.get("human_decision") if isinstance(roadmap_item, dict) else None,
            "locked_steps": locked_steps,
            "allowed_steps": [
                "design_review",
                "design_contract_revision",
                "candidate_sheet_revision",
                "approval_record_update",
            ] if not approved else LOCKED_STEPS,
            "design_doc": creature.get("design_doc"),
            "approval_failures": creature.get("approval_failures", []),
            "latest_candidate": {
                "status": candidate.get("status"),
                "contract": candidate.get("contract"),
                "candidate_sheet": candidate.get("candidate_sheet"),
                "candidate_design_verdict": candidate.get("candidate_design_verdict"),
                "supersedes": candidate.get("supersedes"),
                "must_approve_before_modeling": candidate.get("must_approve_before_modeling", []),
            } if candidate else {},
            "v4_candidate": {
                "status": candidate.get("status"),
                "contract": candidate.get("contract"),
                "candidate_sheet": candidate.get("candidate_sheet"),
                "must_approve_before_modeling": candidate.get("must_approve_before_modeling", []),
            } if candidate else {},
            "next_action": next_action_for(level, candidate),
            "must_not": [
                "Do not generate or polish final .bbmodel from this design.",
                "Do not lock final UV or paint final texture from this design.",
                "Do not sync Java runtime model from this design.",
                "Do not capture or accept final motion evidence from this design.",
                "Do not treat generated previews, runtime screenshots, or static code contract as design approval.",
            ] if not approved else [],
        })
    locks.sort(key=lambda item: (item["lock_level"], str(item["civilization"]), str(item["id"])))

    lock_counts = Counter(item["lock_level"] for item in locks)
    locked_creatures = [item["key"] for item in locks if item["lock_level"] != "unlocked"]
    hard_locked = [item["key"] for item in locks if item["lock_level"] == "hard_lock_rejected_design"]
    approval_locked = [item["key"] for item in locks if item["lock_level"] == "approval_lock_manual_review_required"]
    step_lock_counts = {
        step: sum(1 for item in locks if step in item["locked_steps"])
        for step in LOCKED_STEPS
    }
    ok = not locked_creatures and bool(locks)
    return {
        "version": 1,
        "scope": "mythic_age_1_0_creature_design_production_lock",
        "ok": ok,
        "status": "unlocked" if ok else "locked",
        "rule": (
            "No creature may advance final Blockbench, UV, texture, runtime Java model, visual ledger, "
            "or motion capture until approved_for_modeling=true is recorded in the design-first gate."
        ),
        "source_design_first_gate": project_path(DESIGN_FIRST_GATE),
        "source_art_production_roadmap": project_path(ART_PRODUCTION_ROADMAP),
        "source_design_correctness_gate": project_path(DESIGN_CORRECTNESS_GATE),
        "source_v4_candidate_package": project_path(V4_CANDIDATE_PACKAGE),
        "source_v5_candidate_package": project_path(V5_CANDIDATE_PACKAGE),
        "source_v6_candidate_package": project_path(V6_CANDIDATE_PACKAGE),
        "design_approval_review_package": project_path(DESIGN_APPROVAL_REVIEW_PACKAGE),
        "design_approval_review_markdown": project_path(DESIGN_APPROVAL_REVIEW_MARKDOWN),
        "design_approval_review_contact_sheet": project_path(DESIGN_APPROVAL_REVIEW_CONTACT_SHEET),
        "creature_count": len(locks),
        "locked_count": len(locked_creatures),
        "unlocked_count": len(locks) - len(locked_creatures),
        "hard_lock_rejected_design_count": len(hard_locked),
        "approval_lock_manual_review_required_count": len(approval_locked),
        "lock_counts": dict(sorted(lock_counts.items())),
        "step_lock_counts": step_lock_counts,
        "hard_locked": hard_locked,
        "approval_locked": approval_locked,
        "design_correctness_verdict": correctness.get("verdict"),
        "locks": locks,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 生物设计生产锁（2026-06-15）",
        "",
        "本文档由 `tools/creature_design_production_lock.py audit` 生成。它把设计先行原则转成生产准入锁：设计未批准时，不允许进入最终模型、UV、纹理、Java runtime、视觉台账或动作捕获。",
        "",
        "## 总览",
        "",
        f"- 生产锁状态：`{payload['status']}`",
        f"- 生物数量：{payload['creature_count']}",
        f"- 已解锁：{payload['unlocked_count']}",
        f"- 锁定：{payload['locked_count']}",
        f"- 设计拒绝硬锁：{payload['hard_lock_rejected_design_count']}",
        f"- 人工审批锁：{payload['approval_lock_manual_review_required_count']}",
        f"- 锁统计：`{json.dumps(payload['lock_counts'], ensure_ascii=False)}`",
        f"- 工序锁统计：`{json.dumps(payload['step_lock_counts'], ensure_ascii=False)}`",
        f"- 判定规则：{payload['rule']}",
        f"- 设计审批总包：`{payload['design_approval_review_markdown']}`",
        f"- 设计审批联系图：`{payload['design_approval_review_contact_sheet']}`",
        "",
        "## 锁定清单",
        "",
        "| 生物 | 锁级别 | 批次 | 锁定工序 | 下一步 |",
        "| --- | --- | --- | --- | --- |",
    ]
    for item in payload["locks"]:
        lines.append(
            "| `{key}` | `{level}` | `{batch}` | `{steps}` | `{next}` |".format(
                key=item["key"],
                level=item["lock_level"],
                batch=item["batch"],
                steps=", ".join(item["locked_steps"]) or "-",
                next=item["next_action"],
            )
        )

    lines.extend(["", "## 硬锁详情", ""])
    for item in payload["locks"]:
        if item["lock_level"] != "hard_lock_rejected_design":
            continue
        lines.extend([
            f"### {item['key']}",
            "",
            f"- 设计状态：`{item['design_status']}`",
            f"- 最新候选契约：`{item['latest_candidate'].get('contract')}`",
            f"- 最新候选图：`{item['latest_candidate'].get('candidate_sheet')}`",
            "- 必须先确认：",
        ])
        for check in item["latest_candidate"].get("must_approve_before_modeling", []):
            lines.append(f"  - {check}")
        lines.extend(["", "- 禁止事项："])
        for rule in item["must_not"]:
            lines.append(f"  - {rule}")
        lines.append("")

    lines.extend([
        "## 退出条件",
        "",
        "- 对每个生物，设计包 front matter 必须明确 `approved_for_modeling`。",
        "- 对每个生物，`modeling_allowed=true` 必须与设计状态一致。",
        "- Pegasus/Hydra 必须先通过专门设计正确性 gate。",
        "- 生产锁 `ok=true` 后，才能进入最终 Blockbench/UV/纹理/Java runtime/动作验收。",
    ])
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_report()
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        status = "PASS" if payload["ok"] else "LOCKED"
        print(f"Creature Design Production Lock: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"lock_counts: {payload['lock_counts']}")
        print(f"locked_count: {payload['locked_count']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
