#!/usr/bin/env python3
"""Generate the 1.0 creature motion rework and capture queue.

This queue combines the action acceptance package with the art rework queue.
It prevents motion capture from proceeding on creatures whose model/design is
already blocked, especially Pegasus and Hydra.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
ACTION_PACKAGE = ROOT / "docs/preview_assets/creature_action_acceptance/creature_action_acceptance_package.json"
ART_REWORK_QUEUE = ROOT / "docs/preview_assets/creature_art_rework_queue/creature_art_rework_queue.json"
OUT_DIR = ROOT / "docs/preview_assets/creature_motion_rework_queue"
REPORT_PATH = OUT_DIR / "creature_motion_rework_queue.json"
MARKDOWN_PATH = ROOT / "docs/美术资源/entity_validation/creature_motion_rework_queue_2026-06-15.md"

MODE_LABELS = {
    "locomotion": "移动/步态",
    "attack_or_special": "攻击/特殊",
    "flight_or_glide": "自主飞行/滑翔",
    "riding_fly": "骑乘飞行",
    "swim_or_drift": "游动/漂移",
    "signature_multi_part_motion": "标志部件联动",
    "hurt": "受击",
    "death": "死亡",
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def creature_key(civilization: str, creature_id: str) -> str:
    return f"{civilization}:{creature_id}"


def index_art_queue(payload: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for item in payload.get("queue", []):
        if not isinstance(item, dict):
            continue
        civilization = item.get("civilization")
        creature_id = item.get("id")
        if isinstance(civilization, str) and isinstance(creature_id, str):
            result[creature_key(civilization, creature_id)] = item
    return result


def index_action_creatures(payload: dict[str, Any]) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for item in payload.get("creatures", []):
        if not isinstance(item, dict):
            continue
        civilization = item.get("civilization")
        creature_id = item.get("id")
        if isinstance(civilization, str) and isinstance(creature_id, str):
            result[creature_key(civilization, creature_id)] = item
    return result


def art_dependency_state(art_item: dict[str, Any] | None) -> str:
    if not art_item:
        return "unknown_art_queue_missing"
    priority = art_item.get("priority")
    if priority == "P0-DESIGN-BLOCKER":
        return "blocked_until_design_and_model_rebuild"
    if priority == "P0-USER-ACCEPTANCE":
        return "blocked_until_user_visual_acceptance_or_rework"
    if priority in {"P0-GREEK-REVIEW", "P1-CIV-ART-UPGRADE"}:
        return "wait_for_visual_review_before_final_motion_acceptance"
    return "unknown_art_priority"


def task_state(task: dict[str, Any], art_item: dict[str, Any] | None) -> str:
    dependency = art_dependency_state(art_item)
    if dependency.startswith("blocked"):
        return "blocked_by_art_or_design"
    if dependency.startswith("wait"):
        return "capture_allowed_but_final_acceptance_waits_for_art"
    return "ready_for_motion_capture"


def dependency_note(task: dict[str, Any], art_item: dict[str, Any] | None) -> str:
    creature_id = task.get("creature_id")
    mode = task.get("motion_mode")
    dependency = art_dependency_state(art_item)
    if creature_id == "pegasus" and dependency == "blocked_until_design_and_model_rebuild":
        return "天马所有动作必须等 V6 原版成年马式马体、非大头头颈、翼根和骑乘空间规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。"
    if creature_id == "hydra" and dependency == "blocked_until_design_and_model_rebuild":
        return "九头蛇所有动作必须等 V6 九头 socket、颈根、头颈重叠和低伏长身规格签收并重建模型后再捕获；旧模型动作证据不可作为 1.0 依据。"
    if creature_id == "pegasus" and mode in {"flight_or_glide", "riding_fly"}:
        return "天马飞行动作必须等 V6 马体、头颈、翼根和骑乘空间规格签收并重建模型后再捕获。"
    if dependency == "blocked_until_user_visual_acceptance_or_rework":
        return "当前视觉修订仍需用户接受；动作最终验收等视觉结论稳定。"
    if dependency == "wait_for_visual_review_before_final_motion_acceptance":
        return "可作为捕获队列保留，但最终动作通过必须等人工视觉验收或返修完成。"
    return "动作捕获可按当前队列执行。"


def build_task(task: dict[str, Any], art_item: dict[str, Any] | None) -> dict[str, Any]:
    state = task_state(task, art_item)
    return {
        "key": creature_key(str(task.get("civilization")), str(task.get("creature_id"))),
        "civilization": task.get("civilization"),
        "creature_id": task.get("creature_id"),
        "entity_id": task.get("entity_id"),
        "motion_mode": task.get("motion_mode"),
        "motion_label": MODE_LABELS.get(str(task.get("motion_mode")), str(task.get("motion_mode"))),
        "priority": task.get("priority"),
        "state": state,
        "required_view": task.get("required_view"),
        "minimum_frames": task.get("minimum_frames"),
        "frame_interval_ticks": task.get("frame_interval_ticks"),
        "trigger_hint": task.get("trigger_hint"),
        "acceptance_criteria": task.get("acceptance_criteria", []),
        "required_evidence_types": task.get("required_evidence_types", []),
        "art_priority": art_item.get("priority") if art_item else None,
        "art_phase": art_item.get("phase") if art_item else None,
        "art_human_decision": art_item.get("human_decision") if art_item else None,
        "art_dependency_state": art_dependency_state(art_item),
        "dependency_note": dependency_note(task, art_item),
        "exit_gate": exit_gate_for(state, task, art_item),
    }


def exit_gate_for(state: str, task: dict[str, Any], art_item: dict[str, Any] | None) -> list[str]:
    gates = [
        "gui-free frame sequence captured",
        "motion_sequence_manifest entry exists",
        "human motion review note approves this mode",
    ]
    if state == "blocked_by_art_or_design":
        return [
            "art/design blocker resolved first",
            "Blockbench/runtime model rebuilt or accepted",
            *gates,
        ]
    if state == "capture_allowed_but_final_acceptance_waits_for_art":
        return [
            "visual/art review accepted or rework completed",
            *gates,
        ]
    return gates


def creature_motion_summary(action_creature: dict[str, Any], art_item: dict[str, Any] | None, tasks: list[dict[str, Any]]) -> dict[str, Any]:
    missing_modes = action_creature.get("motion_sequence_missing_modes", [])
    captured_modes = action_creature.get("motion_sequence_captured_modes", [])
    return {
        "key": creature_key(str(action_creature.get("civilization")), str(action_creature.get("id"))),
        "civilization": action_creature.get("civilization"),
        "id": action_creature.get("id"),
        "entity_id": action_creature.get("entity_id"),
        "art_priority": art_item.get("priority") if art_item else None,
        "art_dependency_state": art_dependency_state(art_item),
        "motion_status": action_creature.get("status"),
        "human_motion_acceptance": action_creature.get("human_motion_acceptance"),
        "required_modes": action_creature.get("motion_sequence_required_modes", []),
        "captured_modes": captured_modes,
        "missing_modes": missing_modes,
        "missing_mode_count": len(missing_modes),
        "captured_required_mode_count": action_creature.get("motion_sequence_captured_required_mode_count"),
        "required_mode_count": action_creature.get("motion_sequence_required_mode_count"),
        "queued_task_count": len(tasks),
        "blocked_task_count": sum(1 for task in tasks if task["state"] == "blocked_by_art_or_design"),
        "minimum_frame_budget": sum(int(task.get("minimum_frames") or 0) for task in tasks),
        "motion_review_board": action_creature.get("motion_review_board"),
        "motion_storyboard": action_creature.get("motion_storyboard"),
        "animation_constraints": action_creature.get("animation_constraints"),
    }


def build_report() -> dict[str, Any]:
    action = load_json(ACTION_PACKAGE, {})
    art = load_json(ART_REWORK_QUEUE, {})
    art_map = index_art_queue(art)
    action_map = index_action_creatures(action)
    tasks = [
        build_task(task, art_map.get(creature_key(str(task.get("civilization")), str(task.get("creature_id")))))
        for task in action.get("motion_capture_queue", [])
        if isinstance(task, dict)
    ]
    tasks_by_key: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for task in tasks:
        tasks_by_key[task["key"]].append(task)
    creature_summaries = [
        creature_motion_summary(action_creature, art_map.get(key), tasks_by_key.get(key, []))
        for key, action_creature in sorted(action_map.items())
    ]
    task_state_counts = Counter(task["state"] for task in tasks)
    task_mode_counts = Counter(str(task["motion_mode"]) for task in tasks)
    task_civ_counts = Counter(str(task["civilization"]) for task in tasks)
    task_art_priority_counts = Counter(str(task["art_priority"]) for task in tasks)
    blocked_tasks = [task for task in tasks if task["state"] == "blocked_by_art_or_design"]
    blocked_creatures = sorted({task["key"] for task in blocked_tasks})
    ok = (
        not tasks
        and bool(action.get("human_motion_acceptance_ok"))
        and bool(art.get("ok"))
    )
    return {
        "version": 1,
        "scope": "mythic_age_1_0_creature_motion_rework_queue",
        "ok": ok,
        "source_action_package": project_path(ACTION_PACKAGE),
        "source_art_rework_queue": project_path(ART_REWORK_QUEUE),
        "creature_count": len(creature_summaries),
        "queued_task_count": len(tasks),
        "blocked_task_count": len(blocked_tasks),
        "blocked_creatures": blocked_creatures,
        "capture_allowed_waiting_art_count": task_state_counts.get("capture_allowed_but_final_acceptance_waits_for_art", 0),
        "ready_for_capture_count": task_state_counts.get("ready_for_motion_capture", 0),
        "minimum_frame_budget": sum(int(task.get("minimum_frames") or 0) for task in tasks),
        "task_state_counts": dict(sorted(task_state_counts.items())),
        "task_mode_counts": dict(sorted(task_mode_counts.items())),
        "task_civilization_counts": dict(sorted(task_civ_counts.items())),
        "task_art_priority_counts": dict(sorted(task_art_priority_counts.items())),
        "action_package_summary": {
            "motion_sequence_required_mode_count": action.get("motion_sequence_required_mode_count"),
            "motion_sequence_captured_required_mode_count": action.get("motion_sequence_captured_required_mode_count"),
            "motion_sequence_missing_mode_count": action.get("motion_sequence_missing_mode_count"),
            "motion_capture_plan_batch_count": action.get("motion_capture_plan_batch_count"),
            "human_motion_acceptance_ok": action.get("human_motion_acceptance_ok"),
        },
        "rule": (
            "Motion evidence cannot be final while the creature art/model is rejected or pending. "
            "Design-blocked creatures must be rebuilt before MC motion capture is accepted."
        ),
        "creatures": creature_summaries,
        "tasks": tasks,
        "source_batches": action.get("motion_capture_plan_batches", []),
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 生物动作/动画重做队列（2026-06-15）",
        "",
        "本文档由 `tools/creature_motion_rework_queue.py audit` 生成。它把动作验收包和美术重做队列合并，防止在未签收或畸形模型上做最终动作捕获。",
        "",
        "## 总览",
        "",
        f"- 队列通过：`{payload['ok']}`",
        f"- 生物数量：{payload['creature_count']}",
        f"- 待捕获任务：{payload['queued_task_count']}",
        f"- 因美术/设计阻断的动作任务：{payload['blocked_task_count']}",
        f"- 阻断生物：`{json.dumps(payload['blocked_creatures'], ensure_ascii=False)}`",
        f"- 可捕获但最终验收等待美术：{payload['capture_allowed_waiting_art_count']}",
        f"- 最低帧预算：{payload['minimum_frame_budget']}",
        f"- 任务状态统计：`{json.dumps(payload['task_state_counts'], ensure_ascii=False)}`",
        f"- 动作模式统计：`{json.dumps(payload['task_mode_counts'], ensure_ascii=False)}`",
        f"- 文明统计：`{json.dumps(payload['task_civilization_counts'], ensure_ascii=False)}`",
        f"- 美术优先级统计：`{json.dumps(payload['task_art_priority_counts'], ensure_ascii=False)}`",
        "",
        "## 关键结论",
        "",
        "- 现有动作静态合同通过不等于 1.0 动作完成；当前仍缺 138 个 MC 连续帧动作模式。",
        "- `pegasus` 和 `hydra` 的动作任务必须等设计/模型重建后才可作为最终证据。",
        "- 其他生物可以保留捕获队列，但最终动作验收必须等视觉/模型人工结论稳定。",
        "",
        "## 按生物汇总",
        "",
        "| 文明 | 生物 | 美术依赖 | 缺失模式 | 队列数 | 阻断数 | 最低帧 |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for creature in payload["creatures"]:
        lines.append(
            "| {civ} | `{cid}` | `{dep}` | `{missing}` | {queued} | {blocked} | {frames} |".format(
                civ=creature["civilization"],
                cid=creature["id"],
                dep=creature["art_dependency_state"],
                missing=", ".join(creature["missing_modes"]),
                queued=creature["queued_task_count"],
                blocked=creature["blocked_task_count"],
                frames=creature["minimum_frame_budget"],
            )
        )
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for task in payload["tasks"]:
        grouped[task["state"]].append(task)
    lines.extend(["", "## 队列任务", ""])
    for state in ["blocked_by_art_or_design", "capture_allowed_but_final_acceptance_waits_for_art", "ready_for_motion_capture"]:
        items = grouped.get(state, [])
        if not items:
            continue
        lines.extend([f"### {state}", "", "| 优先级 | 文明 | 生物 | 模式 | 视角 | 帧数 | 依赖说明 |", "| --- | --- | --- | --- | --- | --- | --- |"])
        for task in items:
            lines.append(
                f"| {task['priority']} | {task['civilization']} | `{task['creature_id']}` | `{task['motion_mode']}` | `{task['required_view']}` | {task['minimum_frames']} | {task['dependency_note']} |"
            )
        lines.append("")
    lines.extend([
        "## 退出规则",
        "",
        "- 先完成美术/模型设计 gate，再把动作连续帧作为最终验收。",
        "- 每个动作模式必须有无菜单 GUI 的连续帧、manifest 记录和人工 motion review note。",
        "- 天马必须分别验证自主飞行和骑乘飞行，且玩家不遮挡头颈/翼根。",
        "- 九头蛇必须在 idle/移动/攻击/受击/死亡中保持每条颈和头的实体连接。",
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
        print(f"Creature Motion Rework Queue: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"queued: {payload['queued_task_count']}, blocked: {payload['blocked_task_count']}")
        print(f"task_state_counts: {payload['task_state_counts']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
