#!/usr/bin/env python3
"""Create an executable manual playtest queue for 1.0 god powers.

The gameplay acceptance package already proves static coverage and builds a
manual queue. This tool turns that queue into a release-facing execution plan:
batch order, scenario templates, recording fields, and pass/fail writeback
requirements.
"""

from __future__ import annotations

import argparse
import csv
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
GAMEPLAY_PACKAGE = ROOT / "docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json"
OUT_DIR = ROOT / "docs/preview_assets/god_power_gameplay/manual_playtest_execution"
REPORT_PATH = OUT_DIR / "god_power_manual_playtest_execution_queue.json"
CSV_PATH = OUT_DIR / "god_power_manual_playtest_execution_queue.csv"
LEDGER_TEMPLATE_PATH = OUT_DIR / "god_power_manual_playtest_result_ledger_template.json"
MARKDOWN_PATH = ROOT / "docs/系统设计/MythicAgeMod_1.0_神力手工玩法测试执行队列_2026-06-15.md"

RECORD_FIELDS = [
    "tester",
    "date",
    "minecraft_version",
    "world_seed",
    "game_mode",
    "spell_item",
    "target_case",
    "expected_result",
    "actual_result",
    "cooldown_cost_ok",
    "feedback_ok",
    "particle_ok",
    "audio_ok",
    "cleanup_ok",
    "performance_ok",
    "pass_fail",
    "notes",
    "evidence_path",
]

BASE_SCENARIOS = [
    {
        "id": "valid_near_target",
        "name": "近距离有效目标",
        "expected": "神力成功触发，反馈清楚，冷却和证明消耗正确。",
    },
    {
        "id": "valid_far_target",
        "name": "远距离或边界目标",
        "expected": "射程/范围边界符合设计；失败时不误扣资源。",
    },
    {
        "id": "empty_or_invalid_target",
        "name": "空地/无效目标",
        "expected": "失败路径有提示，不产生实际效果或错误冷却。",
    },
    {
        "id": "cooldown_active",
        "name": "冷却中重复施放",
        "expected": "不能再次施放；提示冷却，资源不重复扣除。",
    },
    {
        "id": "insufficient_certificate",
        "name": "证明不足",
        "expected": "施放失败且提示资源不足，不产生实际效果。",
    },
]

WORLD_EDIT_SCENARIOS = [
    {
        "id": "protected_block_safety",
        "name": "保护/核心方块安全",
        "expected": "不破坏基岩、传送门、神殿核心或不可破坏目标。",
    },
    {
        "id": "chunk_reload_cleanup",
        "name": "离开区块并重载",
        "expected": "持续效果、临时方块、任务或实体不会异常残留。",
    },
    {
        "id": "save_restart_cleanup",
        "name": "保存退出后重进",
        "expected": "跨存档状态符合 NBT 目标表，runtime_only 状态不错误持久化。",
    },
]

MOBILITY_SCENARIOS = [
    {
        "id": "obstructed_destination",
        "name": "传送/位移目标被阻挡",
        "expected": "不会卡墙、窒息或越界；失败时给出反馈。",
    },
    {
        "id": "vertical_or_water_edge",
        "name": "高度差/水边/边界地形",
        "expected": "目标选择和安全落点符合预期。",
    },
]

SUMMON_SCENARIOS = [
    {
        "id": "spawn_cap_or_cleanup",
        "name": "召唤上限与清理",
        "expected": "召唤数量、持续时间、阵营和消失逻辑符合设计。",
    }
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def scenario_templates(task: dict[str, Any]) -> list[dict[str, str]]:
    scenarios = list(BASE_SCENARIOS)
    effect_scope = set(task.get("effect_scope", []))
    if task.get("world_edit_operations"):
        scenarios.extend(WORLD_EDIT_SCENARIOS)
    if "teleport_or_mobility" in effect_scope:
        scenarios.extend(MOBILITY_SCENARIOS)
    if "summon_or_spawn" in effect_scope:
        scenarios.extend(SUMMON_SCENARIOS)
    # Keep minimum_scenarios authoritative by adding generic balance repeats.
    minimum = int(task.get("minimum_scenarios") or len(scenarios))
    counter = 1
    while len(scenarios) < minimum:
        scenarios.append({
            "id": f"balance_repeat_{counter}",
            "name": f"战斗节奏/平衡复测 {counter}",
            "expected": "记录冷却、消耗、强度、反馈和同文明同类型神力的相对平衡。",
        })
        counter += 1
    return scenarios[:minimum]


def task_key(task: dict[str, Any]) -> str:
    return f"{task.get('civilization')}:{task.get('spell_type')}"


def build_report() -> dict[str, Any]:
    package = load_json(GAMEPLAY_PACKAGE, {})
    tasks = package.get("manual_playtest_queue", []) if isinstance(package, dict) else []
    batches = package.get("manual_playtest_batches", []) if isinstance(package, dict) else []
    task_map = {task_key(task): task for task in tasks if isinstance(task, dict)}

    execution_tasks: list[dict[str, Any]] = []
    for task in tasks:
        if not isinstance(task, dict):
            continue
        scenarios = scenario_templates(task)
        execution_tasks.append({
            "key": task_key(task),
            "priority": task.get("priority"),
            "civilization": task.get("civilization"),
            "spell_type": task.get("spell_type"),
            "risk_level": task.get("risk_level"),
            "risk_flags": task.get("risk_flags", []),
            "targeting": task.get("targeting"),
            "effect_scope": task.get("effect_scope", []),
            "world_edit_operations": task.get("world_edit_operations", []),
            "minimum_scenarios": task.get("minimum_scenarios"),
            "scenario_template_count": len(scenarios),
            "scenario_templates": scenarios,
            "manual_focus": task.get("manual_focus", []),
            "acceptance_criteria": task.get("acceptance_criteria", []),
            "runtime_smoke_probe_ok": bool(task.get("runtime_smoke_probe_ok")),
            "review_board": task.get("review_board", ""),
            "record_fields": RECORD_FIELDS,
            "writeback_required": [
                "human_playtest_acceptance=approved",
                "manual_playtest_required=false",
                "playtest_notes=<batch evidence path>",
                "balance_review=approved",
            ],
        })

    execution_batches: list[dict[str, Any]] = []
    for batch in batches:
        if not isinstance(batch, dict):
            continue
        spells = [str(item) for item in batch.get("spells", [])]
        batch_tasks = [task_map[spell] for spell in spells if spell in task_map]
        execution_batches.append({
            "batch_id": batch.get("batch_id"),
            "priority": batch.get("priority"),
            "risk_level": batch.get("risk_level"),
            "task_count": batch.get("task_count"),
            "spells": spells,
            "total_minimum_scenarios": batch.get("total_minimum_scenarios"),
            "world_edit_task_count": batch.get("world_edit_task_count"),
            "acceptance_focus": batch.get("acceptance_focus", []),
            "record_fields": RECORD_FIELDS,
            "execution_steps": [
                "Create or load the standard 1.0 playtest world.",
                "Spawn required target entities, empty target zones, protected blocks, and resource/certificate states.",
                "Run every scenario template for each spell in the batch.",
                "Record pass/fail, screenshots or video paths, performance notes, cleanup/restart notes, and balance notes.",
                "Do not mark a spell approved unless all mandatory scenario rows pass or a design/code issue is filed.",
            ],
            "batch_task_keys": [task_key(task) for task in batch_tasks],
        })

    risk_counts = Counter(str(task.get("risk_level")) for task in execution_tasks)
    priority_counts = Counter(str(task.get("priority")) for task in execution_tasks)
    civ_counts = Counter(str(task.get("civilization")) for task in execution_tasks)
    world_edit_count = sum(1 for task in execution_tasks if task.get("world_edit_operations"))
    scenario_total = sum(int(task.get("scenario_template_count") or 0) for task in execution_tasks)
    return {
        "version": 1,
        "scope": "mythic_age_1_0_god_power_manual_playtest_execution_queue",
        "ok": False,
        "status": "manual_playtest_not_done",
        "rule": (
            "Runtime smoke and static implementation are not gameplay acceptance. "
            "Each spell must have manual scenario rows recorded and approved before 1.0."
        ),
        "source_gameplay_acceptance_package": project_path(GAMEPLAY_PACKAGE),
        "spell_count": len(execution_tasks),
        "batch_count": len(execution_batches),
        "scenario_template_total": scenario_total,
        "minimum_scenario_budget_from_source": package.get("manual_playtest_queue_total_minimum_scenarios"),
        "risk_counts": dict(sorted(risk_counts.items())),
        "priority_counts": dict(sorted(priority_counts.items())),
        "civilization_counts": dict(sorted(civ_counts.items())),
        "world_edit_task_count": world_edit_count,
        "runtime_smoke_all_ok": all(task.get("runtime_smoke_probe_ok") for task in execution_tasks),
        "record_fields": RECORD_FIELDS,
        "csv_queue": project_path(CSV_PATH),
        "markdown": project_path(MARKDOWN_PATH),
        "result_ledger_template": project_path(LEDGER_TEMPLATE_PATH),
        "batches": execution_batches,
        "tasks": execution_tasks,
    }


def write_csv(payload: dict[str, Any]) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    with CSV_PATH.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=[
                "batch_id",
                "priority",
                "risk_level",
                "civilization",
                "spell_type",
                "scenario_id",
                "scenario_name",
                "expected",
                *RECORD_FIELDS,
            ],
        )
        writer.writeheader()
        batch_for_spell: dict[str, str] = {}
        for batch in payload["batches"]:
            for spell in batch.get("spells", []):
                batch_for_spell[str(spell)] = str(batch.get("batch_id"))
        for task in payload["tasks"]:
            for scenario in task["scenario_templates"]:
                writer.writerow({
                    "batch_id": batch_for_spell.get(task["key"], ""),
                    "priority": task["priority"],
                    "risk_level": task["risk_level"],
                    "civilization": task["civilization"],
                    "spell_type": task["spell_type"],
                    "scenario_id": scenario["id"],
                    "scenario_name": scenario["name"],
                    "expected": scenario["expected"],
                    **{field: "" for field in RECORD_FIELDS},
                })


def write_ledger_template(payload: dict[str, Any]) -> None:
    batch_for_spell: dict[str, str] = {}
    for batch in payload["batches"]:
        for spell in batch.get("spells", []):
            batch_for_spell[str(spell)] = str(batch.get("batch_id"))
    rows: list[dict[str, Any]] = []
    for task in payload["tasks"]:
        for scenario in task["scenario_templates"]:
            rows.append({
                "batch_id": batch_for_spell.get(task["key"], ""),
                "spell_key": task["key"],
                "civilization": task["civilization"],
                "spell_type": task["spell_type"],
                "risk_level": task["risk_level"],
                "scenario_id": scenario["id"],
                "scenario_name": scenario["name"],
                "expected_result": scenario["expected"],
                **{field: "" for field in RECORD_FIELDS},
                "approval_status": "not_run",
                "issue_id": "",
            })
    ledger = {
        "version": 1,
        "scope": "mythic_age_1_0_god_power_manual_playtest_result_ledger_template",
        "status": "template_not_executed",
        "rule": "Copy or fill this ledger during manual playtest. A spell is accepted only when every required row is pass and evidence_path is populated.",
        "source_queue": project_path(REPORT_PATH),
        "source_csv": project_path(CSV_PATH),
        "row_count": len(rows),
        "required_pass_count": len(rows),
        "filled_pass_count": 0,
        "failed_count": 0,
        "not_run_count": len(rows),
        "rows": rows,
    }
    LEDGER_TEMPLATE_PATH.write_text(json.dumps(ledger, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 神力手工玩法测试执行队列（2026-06-15）",
        "",
        "本文档由 `tools/god_power_manual_playtest_execution_queue.py audit` 生成。它把已有神力玩法验收包中的 52 个待测神力整理成可执行记录队列。",
        "",
        "## 总览",
        "",
        f"- 状态：`{payload['status']}`",
        f"- 神力数量：{payload['spell_count']}",
        f"- 批次数量：{payload['batch_count']}",
        f"- 场景模板总数：{payload['scenario_template_total']}",
        f"- 源最低场景预算：{payload['minimum_scenario_budget_from_source']}",
        f"- 风险统计：`{json.dumps(payload['risk_counts'], ensure_ascii=False)}`",
        f"- 优先级统计：`{json.dumps(payload['priority_counts'], ensure_ascii=False)}`",
        f"- 文明统计：`{json.dumps(payload['civilization_counts'], ensure_ascii=False)}`",
        f"- 世界编辑/爆炸相关神力：{payload['world_edit_task_count']}",
        f"- runtime smoke 全通过：`{payload['runtime_smoke_all_ok']}`",
        f"- CSV 执行表：`{payload['csv_queue']}`",
        f"- 结果台账模板：`{payload['result_ledger_template']}`",
        "",
        "## 记录字段",
        "",
    ]
    for field in payload["record_fields"]:
        lines.append(f"- `{field}`")
    lines.extend([
        "",
        "## 批次执行顺序",
        "",
        "| 批次 | 优先级 | 风险 | 神力数 | 最低场景 | 世界编辑项 | 神力 | 执行重点 |",
        "| --- | ---: | --- | ---: | ---: | ---: | --- | --- |",
    ])
    for batch in payload["batches"]:
        spells = ", ".join(batch["spells"]).replace("|", "/")
        if len(spells) > 180:
            spells = spells[:177] + "..."
        focus = "；".join(batch["acceptance_focus"]).replace("|", "/")
        lines.append(
            "| {batch} | {priority} | `{risk}` | {count} | {scenarios} | {world_edit} | {spells} | {focus} |".format(
                batch=batch["batch_id"],
                priority=batch["priority"],
                risk=batch["risk_level"],
                count=batch["task_count"],
                scenarios=batch["total_minimum_scenarios"],
                world_edit=batch["world_edit_task_count"],
                spells=spells,
                focus=focus,
            )
        )

    lines.extend(["", "## 批次勾选单", ""])
    for batch in payload["batches"]:
        lines.extend([
            f"### {batch['batch_id']}",
            "",
            f"- [ ] 创建/加载 1.0 标准测试世界。",
            f"- [ ] 准备本批次资源、证明不足状态、冷却状态、目标实体和空目标区域。",
            f"- [ ] 执行本批次 {batch['task_count']} 个神力，共 {batch['total_minimum_scenarios']} 个最低场景行。",
            f"- [ ] 对 {batch['world_edit_task_count']} 个世界编辑/爆炸类神力执行区块重载、保存退出重进和清理验证。",
            f"- [ ] 填写 CSV 或 JSON 台账中的 `pass_fail`、`actual_result`、`evidence_path` 和备注。",
            f"- [ ] 失败项已登记 issue 或回到代码/设计/UI/粒子/音效/NBT 对应步骤。",
            "",
        ])

    lines.extend(["", "## 单神力执行模板", ""])
    for task in payload["tasks"]:
        lines.extend([
            f"### {task['key']}",
            "",
            f"- 优先级：`{task['priority']}`",
            f"- 风险：`{task['risk_level']}`",
            f"- 目标模式：`{task['targeting']}`",
            f"- 作用范围：`{json.dumps(task['effect_scope'], ensure_ascii=False)}`",
            f"- 世界操作：`{json.dumps(task['world_edit_operations'], ensure_ascii=False)}`",
            f"- review board：`{task['review_board']}`",
            "",
            "| 场景 | 预期 |",
            "| --- | --- |",
        ])
        for scenario in task["scenario_templates"]:
            lines.append(f"| `{scenario['id']}` {scenario['name']} | {scenario['expected'].replace('|', '/')} |")
        lines.extend(["", "验收要点："])
        for criterion in task["acceptance_criteria"][:8]:
            lines.append(f"- {criterion}")
        lines.append("")
    lines.extend([
        "## 通过口径",
        "",
        "- 每个场景行必须有 pass/fail、实际结果、证据路径和备注。",
        "- 高风险神力必须额外记录性能、存档重启/区块重载和清理结果。",
        "- 任何失败都不能直接豁免；必须回到代码、设计、UI、粒子、音效或 NBT 对应问题单。",
        "- 全部神力通过并回写后，`god_power_playtest_acceptance_not_done` 才能关闭。",
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
        write_csv(payload)
        write_ledger_template(payload)
        write_markdown(payload)
        print("God Power Manual Playtest Execution Queue: OPEN")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"csv: {CSV_PATH}")
        print(f"ledger_template: {LEDGER_TEMPLATE_PATH}")
        print(f"batches: {payload['batch_count']}")
        print(f"scenarios: {payload['scenario_template_total']}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
