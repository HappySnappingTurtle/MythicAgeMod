#!/usr/bin/env python3
"""Build the 1.0 creature art production roadmap.

The rework queue says what is unresolved. This roadmap says how to execute it
without falling back into trial-and-error modeling: every creature moves through
design approval, Blockbench rebuild/polish, UV, texture, runtime sync, and then
motion unlock gates.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
ART_REWORK_QUEUE = ROOT / "docs/preview_assets/creature_art_rework_queue/creature_art_rework_queue.json"
DESIGN_FIRST_GATE = ROOT / "docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json"
MOTION_REWORK_QUEUE = ROOT / "docs/preview_assets/creature_motion_rework_queue/creature_motion_rework_queue.json"
V4_CANDIDATE_PACKAGE_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_approval/v4/pegasus_hydra_v4_candidate_package.json"
V4_CANDIDATE_MARKDOWN = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v4_candidate_package_2026-06-15.md"
V5_CANDIDATE_PACKAGE_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_approval/v5/pegasus_hydra_v5_design_approval_package.json"
V5_CANDIDATE_MARKDOWN = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v5_design_approval_package_2026-06-15.md"
V6_CANDIDATE_PACKAGE_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json"
V6_CANDIDATE_MARKDOWN = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v6_modeling_spec_package_2026-06-15.md"
OUT_DIR = ROOT / "docs/preview_assets/creature_art_production_roadmap"
REPORT_PATH = OUT_DIR / "creature_art_production_roadmap.json"
MARKDOWN_PATH = ROOT / "docs/项目管理/MythicAgeMod_1.0生物美术生产路线图_2026-06-15.md"

BATCH_ORDER = [
    "B0_DESIGN_BLOCKER_REDRAW",
    "B1_GREEK_VERTICAL_SLICE_REVIEW",
    "B2_NON_GREEK_DESIGN_APPROVAL",
    "B3_NON_GREEK_BLOCKBENCH_UV_TEXTURE",
    "B4_RUNTIME_SYNC_AND_MOTION_UNLOCK",
]

CIVILIZATION_ORDER = ["greek", "egyptian", "nordic", "chinese", "atlantean"]

PIPELINE_STEPS = [
    "design_package_approved",
    "final_design_sheet_approved",
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


def key_for(item: dict[str, Any]) -> str:
    return f"{item.get('civilization')}:{item.get('id')}"


def motion_blocked_keys(payload: dict[str, Any]) -> set[str]:
    keys: set[str] = set()
    for creature in payload.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        if creature.get("blocked_task_count", 0):
            keys.add(f"{creature.get('civilization')}:{creature.get('id')}")
    return keys


def design_status_map(payload: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for creature in payload.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        result[f"{creature.get('civilization')}:{creature.get('id')}"] = creature
    return result


def candidate_map(payload: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for creature in payload.get("creatures", []):
        if not isinstance(creature, dict):
            continue
        creature_id = creature.get("id")
        if isinstance(creature_id, str):
            result[f"greek:{creature_id}"] = creature
    return result


def batch_for(item: dict[str, Any]) -> str:
    priority = item.get("priority")
    civilization = item.get("civilization")
    if priority == "P0-DESIGN-BLOCKER":
        return "B0_DESIGN_BLOCKER_REDRAW"
    if priority in {"P0-GREEK-REVIEW", "P0-USER-ACCEPTANCE"}:
        return "B1_GREEK_VERTICAL_SLICE_REVIEW"
    if civilization != "greek":
        return "B2_NON_GREEK_DESIGN_APPROVAL"
    return "B4_RUNTIME_SYNC_AND_MOTION_UNLOCK"


def initial_step_statuses(item: dict[str, Any], design: dict[str, Any], motion_blocked: bool) -> dict[str, str]:
    priority = item.get("priority")
    human_decision = item.get("human_decision")
    design_approved = bool(design.get("approved_for_modeling"))
    statuses = {step: "pending" for step in PIPELINE_STEPS}

    if priority == "P0-DESIGN-BLOCKER":
        statuses["design_package_approved"] = "blocked"
        statuses["final_design_sheet_approved"] = "blocked"
        statuses["motion_capture_unblocked"] = "blocked"
        return statuses

    if design_approved:
        statuses["design_package_approved"] = "done"
        statuses["final_design_sheet_approved"] = "done"
    else:
        statuses["design_package_approved"] = "pending_manual_approval"
        statuses["final_design_sheet_approved"] = "pending_manual_approval"

    if human_decision == "approved":
        statuses["visual_ledger_approved"] = "done"
    elif human_decision == "pending_user_acceptance":
        statuses["visual_ledger_approved"] = "pending_user_acceptance"
    else:
        statuses["visual_ledger_approved"] = "pending_shape_review"

    statuses["motion_capture_unblocked"] = "blocked_by_art_or_design" if motion_blocked else "waiting_for_visual_approval"
    return statuses


def creature_sort_key(item: dict[str, Any]) -> tuple[int, int, str]:
    batch = str(item.get("batch"))
    civ = str(item.get("civilization"))
    civ_index = CIVILIZATION_ORDER.index(civ) if civ in CIVILIZATION_ORDER else 99
    return (BATCH_ORDER.index(batch) if batch in BATCH_ORDER else 99, civ_index, str(item.get("id")))


def build_report() -> dict[str, Any]:
    art = load_json(ART_REWORK_QUEUE, {})
    design_first = load_json(DESIGN_FIRST_GATE, {})
    motion = load_json(MOTION_REWORK_QUEUE, {})
    v4_package = load_json(V4_CANDIDATE_PACKAGE_REPORT, {})
    v5_package = load_json(V5_CANDIDATE_PACKAGE_REPORT, {})
    v6_package = load_json(V6_CANDIDATE_PACKAGE_REPORT, {})
    design_map = design_status_map(design_first)
    legacy_candidate_map = candidate_map(v4_package)
    v5_candidate_map = candidate_map(v5_package)
    latest_candidate_map = candidate_map(v6_package)
    blocked_motion = motion_blocked_keys(motion)

    creatures: list[dict[str, Any]] = []
    for item in art.get("queue", []):
        if not isinstance(item, dict):
            continue
        key = key_for(item)
        design = design_map.get(key, {})
        latest_candidate = latest_candidate_map.get(key) or v5_candidate_map.get(key) or legacy_candidate_map.get(key, {})
        motion_blocked = key in blocked_motion
        batch = batch_for(item)
        steps = initial_step_statuses(item, design, motion_blocked)
        blocked_steps = [step for step, value in steps.items() if value.startswith("blocked")]
        pending_steps = [step for step, value in steps.items() if value.startswith("pending") or value.startswith("waiting")]
        creatures.append({
            "key": key,
            "civilization": item.get("civilization"),
            "id": item.get("id"),
            "batch": batch,
            "priority": item.get("priority"),
            "human_decision": item.get("human_decision"),
            "design_status": design.get("status"),
            "approved_for_modeling": bool(design.get("approved_for_modeling")),
            "motion_blocked_by_art_or_design": motion_blocked,
            "pipeline_steps": steps,
            "blocked_steps": blocked_steps,
            "pending_steps": pending_steps,
            "art_focus": item.get("art_focus", []),
            "work_items": item.get("work_items", []),
            "must_not": item.get("must_not", []),
            "exit_gate": item.get("exit_gate", []),
            "evidence": item.get("evidence", {}),
            "latest_candidate_evidence": {
                "status": latest_candidate.get("status"),
                "approved_for_modeling": bool(latest_candidate.get("approved_for_modeling")),
                "contract": latest_candidate.get("contract"),
                "candidate_sheet": latest_candidate.get("candidate_sheet"),
                "candidate_design_verdict": latest_candidate.get("candidate_design_verdict"),
                "supersedes": latest_candidate.get("supersedes"),
                "must_approve_before_modeling": latest_candidate.get("must_approve_before_modeling", []),
            } if latest_candidate else {},
            "v4_candidate_evidence": {
                "status": latest_candidate.get("status"),
                "approved_for_modeling": bool(latest_candidate.get("approved_for_modeling")),
                "contract": latest_candidate.get("contract"),
                "candidate_sheet": latest_candidate.get("candidate_sheet"),
                "must_approve_before_modeling": latest_candidate.get("must_approve_before_modeling", []),
            } if latest_candidate else {},
        })
    creatures.sort(key=creature_sort_key)

    batch_counts = Counter(item["batch"] for item in creatures)
    priority_counts = Counter(item["priority"] for item in creatures)
    step_status_counts: dict[str, dict[str, int]] = {}
    for step in PIPELINE_STEPS:
        step_status_counts[step] = dict(sorted(Counter(item["pipeline_steps"][step] for item in creatures).items()))
    blocked_count = sum(1 for item in creatures if item["blocked_steps"])
    design_blocker_count = sum(1 for item in creatures if item["batch"] == "B0_DESIGN_BLOCKER_REDRAW")
    approved_count = sum(1 for item in creatures if item["human_decision"] == "approved")

    return {
        "version": 1,
        "scope": "mythic_age_1_0_creature_art_production_roadmap",
        "ok": approved_count == len(creatures) and blocked_count == 0 and bool(creatures),
        "source_art_rework_queue": project_path(ART_REWORK_QUEUE),
        "source_design_first_gate": project_path(DESIGN_FIRST_GATE),
        "source_motion_rework_queue": project_path(MOTION_REWORK_QUEUE),
        "b0_v4_candidate_package_status": v4_package.get("status", "missing"),
        "b0_v4_candidate_package_report": project_path(V4_CANDIDATE_PACKAGE_REPORT),
        "b0_v4_candidate_markdown": project_path(V4_CANDIDATE_MARKDOWN),
        "b0_v5_candidate_package_status": v5_package.get("status", "missing"),
        "b0_v5_candidate_package_report": project_path(V5_CANDIDATE_PACKAGE_REPORT),
        "b0_v5_candidate_markdown": project_path(V5_CANDIDATE_MARKDOWN),
        "b0_v6_candidate_package_status": v6_package.get("status", "missing"),
        "b0_v6_candidate_package_report": project_path(V6_CANDIDATE_PACKAGE_REPORT),
        "b0_v6_candidate_markdown": project_path(V6_CANDIDATE_MARKDOWN),
        "creature_count": len(creatures),
        "approved_count": approved_count,
        "blocked_creature_count": blocked_count,
        "design_blocker_count": design_blocker_count,
        "batch_counts": dict(sorted(batch_counts.items())),
        "priority_counts": dict(sorted(priority_counts.items())),
        "step_status_counts": step_status_counts,
        "pipeline_steps": PIPELINE_STEPS,
        "rule": (
            "Testing may verify design implementation only after design_package_approved and final_design_sheet_approved. "
            "Pegasus/Hydra stay in B0 until their design gate passes; motion capture remains blocked for art/design-blocked creatures."
        ),
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 生物美术生产路线图（2026-06-15）",
        "",
        "本文档由 `tools/creature_art_production_roadmap.py audit` 生成。它把生物美术重做队列拆成可执行生产批次，确保先设计签收，再建模、UV、纹理、runtime 同步和动作捕获。",
        "",
        "## 总览",
        "",
        f"- 路线图通过：`{payload['ok']}`",
        f"- 生物数量：{payload['creature_count']}",
        f"- 已人工批准：{payload['approved_count']}",
        f"- 被阻断生物：{payload['blocked_creature_count']}",
        f"- 设计阻断生物：{payload['design_blocker_count']}",
        f"- 批次统计：`{json.dumps(payload['batch_counts'], ensure_ascii=False)}`",
        f"- 优先级统计：`{json.dumps(payload['priority_counts'], ensure_ascii=False)}`",
        "",
        "## 批次",
        "",
        "- `B0_DESIGN_BLOCKER_REDRAW`：Pegasus/Hydra 这类设计已拒绝生物，禁止建模，先重画/签收设计。",
        "- `B1_GREEK_VERTICAL_SLICE_REVIEW`：希腊切片人工形体复审，决定 polish、局部返修或回设计。",
        "- `B2_NON_GREEK_DESIGN_APPROVAL`：非希腊 20 生物先补正式设计签收。",
        "- `B3_NON_GREEK_BLOCKBENCH_UV_TEXTURE`：非希腊通过设计后进入模型/UV/纹理生产。",
        "- `B4_RUNTIME_SYNC_AND_MOTION_UNLOCK`：视觉批准后同步 Java runtime 并解锁动作捕获。",
        "",
        "## B0 候选设计材料",
        "",
        f"- V6 可建模规格包状态：`{payload['b0_v6_candidate_package_status']}`",
        f"- V6 可建模规格包报告：`{payload['b0_v6_candidate_package_report']}`",
        f"- V6 可建模规格包说明：`{payload['b0_v6_candidate_markdown']}`",
        f"- V5 历史候选包：`{payload['b0_v5_candidate_package_report']}`",
        f"- V4 历史候选包：`{payload['b0_v4_candidate_package_report']}`",
        "- 候选包只用于用户审查和设计签收；存在候选图不代表可建模。",
        "",
        "## 工序状态统计",
        "",
        "| 工序 | 状态统计 |",
        "| --- | --- |",
    ]
    for step, counts in payload["step_status_counts"].items():
        lines.append(f"| `{step}` | `{json.dumps(counts, ensure_ascii=False)}` |")

    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in payload["creatures"]:
        grouped[item["batch"]].append(item)
    lines.extend(["", "## 生物批次清单", ""])
    for batch in BATCH_ORDER:
        items = grouped.get(batch, [])
        if not items:
            continue
        lines.extend([f"### {batch}", "", "| 文明 | 生物 | 优先级 | 人工决策 | 设计状态 | 阻断工序 | 下一步 |", "| --- | --- | --- | --- | --- | --- | --- |"])
        for item in items:
            next_step = item["blocked_steps"][0] if item["blocked_steps"] else (item["pending_steps"][0] if item["pending_steps"] else "ready_for_next_gate")
            lines.append(
                "| {civ} | `{cid}` | `{priority}` | `{decision}` | `{design}` | `{blocked}` | `{next_step}` |".format(
                    civ=item["civilization"],
                    cid=item["id"],
                    priority=item["priority"],
                    decision=item["human_decision"],
                    design=item["design_status"],
                    blocked=", ".join(item["blocked_steps"]) or "-",
                    next_step=next_step,
                )
            )
        lines.append("")

    lines.extend(["## 执行纪律", ""])
    lines.extend([
        "- 测试只验证设计是否落地，不负责发明造型。",
        "- `design_package_approved` 与 `final_design_sheet_approved` 未通过时，不允许生成最终 `.bbmodel`、纹理、动作或 runtime model。",
        "- 任何生物不得用大头弥补眼、嘴、牙、角、吻部、武器、翼、尾或多头细节。",
        "- 纹理必须从 UV 计划绘制，不能用随机面板当最终资源。",
        "- 动作捕获只在视觉 ledger approved 后进入最终验收；被设计阻断的动作证据不作为 1.0 发布依据。",
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
        status = "PASS" if payload["ok"] else "OPEN"
        print(f"Creature Art Production Roadmap: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"batch_counts: {payload['batch_counts']}")
        print(f"blocked_creatures: {payload['blocked_creature_count']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
