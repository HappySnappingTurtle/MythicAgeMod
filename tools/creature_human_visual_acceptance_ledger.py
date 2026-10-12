#!/usr/bin/env python3
"""Maintain the human visual acceptance ledger for the 1.0 creature roster.

The visual acceptance package is automatic evidence. This ledger is the
release-facing manual decision record: it preserves reviewer fields across
regeneration and makes design rejections explicit blockers instead of treating
them as ordinary pending review.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
PACKAGE_PATH = ROOT / "docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json"
LEDGER_PATH = ROOT / "docs/preview_assets/creature_visual_acceptance/human_visual_acceptance_ledger.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/entity_validation/creature_human_visual_acceptance_ledger.md"

CHECKLIST_KEYS = [
    "scale_and_body_mass",
    "front_silhouette",
    "side_silhouette",
    "three_quarter_depth",
    "face_eye_mouth_readability",
    "signature_feature_readability",
    "texture_blank_or_uv_error",
    "civilization_style",
    "runtime_consistency",
]

LOCKED_DESIGN_REJECTION_DECISION = "blocked_by_design_rejection"
VALID_DECISIONS = {
    "pending_shape_review",
    "pending_user_acceptance",
    "approved",
    "revision_required",
    "design_rewrite_required",
    LOCKED_DESIGN_REJECTION_DECISION,
}
VALID_CHECK_VALUES = {"pending", "pass", "fail", "not_applicable"}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def normalize_checklist(value: Any) -> dict[str, str]:
    existing = value if isinstance(value, dict) else {}
    checklist: dict[str, str] = {}
    for key in CHECKLIST_KEYS:
        candidate = existing.get(key, "pending")
        checklist[key] = candidate if candidate in VALID_CHECK_VALUES else "pending"
    return checklist


def default_decision(creature: dict[str, Any]) -> str:
    status = creature.get("status")
    if status == "design_rejected_requires_v6_modeling_spec_approval":
        return LOCKED_DESIGN_REJECTION_DECISION
    if status == "revision_implemented_pending_user_acceptance":
        return "pending_user_acceptance"
    return "pending_shape_review"


def next_action_for_decision(decision: str, creature: dict[str, Any]) -> str:
    if decision == LOCKED_DESIGN_REJECTION_DECISION:
        return "approve_or_rewrite_v6_modeling_spec_before_modeling"
    if decision == "pending_user_acceptance":
        return "user_review_revision_board_and_accept_or_request_changes"
    if decision == "pending_shape_review":
        return "human_review_visual_board_and_fill_ledger_decision"
    if decision == "revision_required":
        return "revise_model_texture_or_runtime_parts_then_regenerate_visual_package"
    if decision == "design_rewrite_required":
        return "return_to_design_package_before_modeling"
    return "no_visual_action_required"


def preserve_manual_fields(existing: dict[str, Any], creature: dict[str, Any]) -> dict[str, Any]:
    source_status = creature.get("status")
    enforced_decision = default_decision(creature)
    old_decision = existing.get("decision")
    if source_status == "design_rejected_requires_v6_modeling_spec_approval":
        decision = LOCKED_DESIGN_REJECTION_DECISION
    elif old_decision in VALID_DECISIONS and old_decision != LOCKED_DESIGN_REJECTION_DECISION:
        decision = old_decision
    else:
        decision = enforced_decision
    reviewer = existing.get("reviewer") if isinstance(existing.get("reviewer"), str) else ""
    reviewed_at = existing.get("reviewed_at") if isinstance(existing.get("reviewed_at"), str) else ""
    notes = existing.get("notes") if isinstance(existing.get("notes"), str) else ""
    checklist = normalize_checklist(existing.get("checklist"))
    return {
        "decision": decision,
        "reviewer": reviewer,
        "reviewed_at": reviewed_at,
        "notes": notes,
        "checklist": checklist,
    }


def build_entry(creature: dict[str, Any], existing: dict[str, Any]) -> dict[str, Any]:
    manual = preserve_manual_fields(existing, creature)
    artifacts = creature.get("artifacts", {}) if isinstance(creature.get("artifacts"), dict) else {}
    design_rejection = creature.get("design_rejection", {}) if isinstance(creature.get("design_rejection"), dict) else {}
    visual_quality = creature.get("visual_quality_contract", {}) if isinstance(creature.get("visual_quality_contract"), dict) else {}
    runtime_proportion = creature.get("runtime_proportion_summary", {}) if isinstance(creature.get("runtime_proportion_summary"), dict) else {}
    decision = manual["decision"]
    return {
        "civilization": creature.get("civilization"),
        "id": creature.get("id"),
        "entity_id": creature.get("entity_id"),
        "source_status": creature.get("status"),
        "decision": decision,
        "reviewer": manual["reviewer"],
        "reviewed_at": manual["reviewed_at"],
        "notes": manual["notes"],
        "checklist": manual["checklist"],
        "automatic_evidence_ok": bool(creature.get("automatic_evidence_ok")),
        "visual_quality_contract_ok": bool(visual_quality.get("ok")),
        "runtime_proportion_ok": bool(runtime_proportion.get("ok")),
        "detail_first_runtime_ok": bool(runtime_proportion.get("detail_first_readability_ok")),
        "design_rejection": design_rejection,
        "visual_review_board": creature.get("visual_review_board"),
        "design_doc": artifacts.get("design_doc"),
        "concept_art": artifacts.get("concept_art"),
        "bbmodel": artifacts.get("bbmodel"),
        "texture": artifacts.get("texture"),
        "uv_template": artifacts.get("uv_template"),
        "texture_fit_preview": artifacts.get("texture_fit_preview"),
        "blockbench_previews": artifacts.get("blockbench_previews", {}),
        "review_questions": creature.get("review_questions", []),
        "next_action": next_action_for_decision(decision, creature),
    }


def existing_by_key(payload: dict[str, Any]) -> dict[tuple[str, str], dict[str, Any]]:
    result: dict[tuple[str, str], dict[str, Any]] = {}
    for item in payload.get("creatures", []):
        if not isinstance(item, dict):
            continue
        civilization = item.get("civilization")
        creature_id = item.get("id")
        if isinstance(civilization, str) and isinstance(creature_id, str):
            result[(civilization, creature_id)] = item
    return result


def build_ledger() -> dict[str, Any]:
    package = load_json(PACKAGE_PATH, {})
    existing = load_json(LEDGER_PATH, {})
    existing_map = existing_by_key(existing if isinstance(existing, dict) else {})
    creatures = []
    for creature in package.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        civilization = creature.get("civilization")
        creature_id = creature.get("id")
        if not isinstance(civilization, str) or not isinstance(creature_id, str):
            continue
        creatures.append(build_entry(creature, existing_map.get((civilization, creature_id), {})))
    decision_counts = Counter(item["decision"] for item in creatures)
    checklist_fail_count = sum(
        1
        for item in creatures
        for value in item["checklist"].values()
        if value == "fail"
    )
    checklist_pending_count = sum(
        1
        for item in creatures
        for value in item["checklist"].values()
        if value == "pending"
    )
    approved_count = decision_counts.get("approved", 0)
    creature_count = len(creatures)
    ok = (
        creature_count > 0
        and approved_count == creature_count
        and checklist_fail_count == 0
        and checklist_pending_count == 0
    )
    manual_review_required_count = creature_count - approved_count
    return {
        "version": 1,
        "scope": "mythic_age_1_0_human_creature_visual_acceptance_ledger",
        "ok": ok,
        "human_visual_acceptance_ok": ok,
        "source_visual_acceptance_package": project_path(PACKAGE_PATH),
        "creature_count": creature_count,
        "approved_count": approved_count,
        "manual_review_required_count": manual_review_required_count,
        "decision_counts": dict(sorted(decision_counts.items())),
        "blocked_by_design_rejection_count": decision_counts.get(LOCKED_DESIGN_REJECTION_DECISION, 0),
        "pending_shape_review_count": decision_counts.get("pending_shape_review", 0),
        "pending_user_acceptance_count": decision_counts.get("pending_user_acceptance", 0),
        "revision_required_count": decision_counts.get("revision_required", 0),
        "design_rewrite_required_count": decision_counts.get("design_rewrite_required", 0),
        "checklist_pending_count": checklist_pending_count,
        "checklist_fail_count": checklist_fail_count,
        "review_rule": (
            "A creature is accepted for 1.0 visual release only when decision=approved "
            "and every checklist item is pass or not_applicable. Automatic boards are evidence, "
            "not acceptance."
        ),
        "decision_values": sorted(VALID_DECISIONS),
        "checklist_values": sorted(VALID_CHECK_VALUES),
        "checklist_keys": CHECKLIST_KEYS,
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 生物人工视觉验收台账",
        "",
        "本文档由 `tools/creature_human_visual_acceptance_ledger.py audit` 生成。人工字段以 JSON 台账为准，重新生成时会按 `civilization + id` 保留已有人工结论。",
        "",
        "## 总览",
        "",
        f"- 生物数量：{payload['creature_count']}",
        f"- 人工视觉验收通过：{payload['human_visual_acceptance_ok']}",
        f"- 已批准：{payload['approved_count']}",
        f"- 仍需人工处理：{payload['manual_review_required_count']}",
        f"- 因设计拒绝阻断：{payload['blocked_by_design_rejection_count']}",
        f"- 待形体审查：{payload['pending_shape_review_count']}",
        f"- 待用户接受修订：{payload['pending_user_acceptance_count']}",
        f"- 要求返修：{payload['revision_required_count']}",
        f"- 要求重写设计：{payload['design_rewrite_required_count']}",
        f"- checklist 待判定项：{payload['checklist_pending_count']}",
        f"- checklist 失败项：{payload['checklist_fail_count']}",
        f"- 决策统计：`{json.dumps(payload['decision_counts'], ensure_ascii=False)}`",
        "",
        "## 填写规则",
        "",
        "- `approved`：形体、比例、头脸、神话特征、纹理和 runtime 表现都可接受。",
        "- `revision_required`：设计方向可保留，但模型、贴图、比例、细节或 runtime 同步需要返修。",
        "- `design_rewrite_required`：设计本身不成立，必须回到设计包。",
        "- `blocked_by_design_rejection`：设计已被拒绝，禁止建模和验收。",
        "- `pending_shape_review` / `pending_user_acceptance`：尚未形成最终人工结论。",
        "",
        "## 台账",
        "",
        "| 文明 | 生物 | 决策 | 源状态 | Review board | 下一步 |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for item in payload["creatures"]:
        lines.append(
            "| `{civilization}` | `{id}` | `{decision}` | `{status}` | `{board}` | `{next_action}` |".format(
                civilization=item["civilization"],
                id=item["id"],
                decision=item["decision"],
                status=item["source_status"],
                board=item.get("visual_review_board") or "",
                next_action=item["next_action"],
            )
        )
    lines.extend(["", "## 阻断项", ""])
    blockers = [
        item for item in payload["creatures"]
        if item["decision"] != "approved"
    ]
    for item in blockers:
        lines.extend([
            f"### {item['civilization']} / {item['id']}",
            "",
            f"- 决策：`{item['decision']}`",
            f"- 源状态：`{item['source_status']}`",
            f"- Review board：`{item.get('visual_review_board') or ''}`",
            f"- 下一步：`{item['next_action']}`",
        ])
        design_rejection = item.get("design_rejection") or {}
        if design_rejection:
            lines.extend([
                f"- 设计拒绝报告：`{design_rejection.get('rejection_report')}`",
                f"- 设计正确性审查：`{design_rejection.get('design_correctness_audit')}`",
                f"- V6 可建模规格合同：`{design_rejection.get('v6_modeling_spec_contract')}`",
            ])
        lines.append("")
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_ledger()
        LEDGER_PATH.parent.mkdir(parents=True, exist_ok=True)
        LEDGER_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        status = "PASS" if payload["ok"] else "PENDING"
        print(f"Creature Human Visual Acceptance Ledger: {status}")
        print(f"ledger: {LEDGER_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"decisions: {payload['decision_counts']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
