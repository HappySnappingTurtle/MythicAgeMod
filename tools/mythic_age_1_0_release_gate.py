#!/usr/bin/env python3
"""Run and summarize the MythicAgeMod 1.0 release gate.

This is an orchestrator for the evidence already produced by narrower gates.
Some gates intentionally return non-zero while work remains OPEN/LOCKED. This
script distinguishes expected product blockers from command failures.
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/1_0_release_gate"
REPORT_PATH = OUT_DIR / "mythic_age_1_0_release_gate.json"
MARKDOWN_PATH = ROOT / "docs/项目管理/MythicAgeMod_1.0统一发布门禁_2026-06-15.md"

GAP_REPORT = ROOT / "docs/preview_assets/1_0_gap_audit/gap_report.json"
TRACE_REPORT = ROOT / "docs/preview_assets/1_0_requirement_trace/mythic_age_1_0_requirement_trace.json"
SCOPE_REPORT = ROOT / "docs/preview_assets/1_0_scope_matrix/mythic_age_1_0_scope_matrix.json"
PRODUCTION_LOCK_REPORT = ROOT / "docs/preview_assets/creature_design_production_lock/creature_design_production_lock.json"
DESIGN_APPROVAL_REVIEW_REPORT = ROOT / "docs/preview_assets/creature_design_approval_review/creature_design_approval_review_package.json"
NBT_TARGET_REPORT = ROOT / "docs/preview_assets/nbt_target_validation/nbt_target_report.json"
NBT_RELEASE_REPORT = ROOT / "docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json"
NBT_OWNERSHIP_REPORT = ROOT / "docs/preview_assets/nbt_state_ownership/nbt_state_ownership_queue.json"
GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT = ROOT / "docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.json"

BUNDLED_PYTHON = Path.home() / ".cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3"
PYTHON = os.environ.get("MYTHIC_AGE_PYTHON") or (str(BUNDLED_PYTHON) if BUNDLED_PYTHON.exists() else sys.executable)

COMMANDS = [
    {
        "id": "creature_design_approval_review",
        "cmd": [PYTHON, "-B", "tools/creature_design_approval_review_package.py", "audit"],
        "expected_codes": [1],
        "expected_state": "OPEN while 30 creature designs await approval",
        "report": DESIGN_APPROVAL_REVIEW_REPORT,
    },
    {
        "id": "creature_design_production_lock",
        "cmd": [PYTHON, "-B", "tools/creature_design_production_lock.py", "audit"],
        "expected_codes": [1],
        "expected_state": "LOCKED while no creature is approved_for_modeling",
        "report": PRODUCTION_LOCK_REPORT,
    },
    {
        "id": "nbt_target_validation",
        "cmd": [PYTHON, "-B", "tools/nbt_target_validation_pipeline.py", "audit"],
        "expected_codes": [0],
        "expected_state": "PASS",
        "report": NBT_TARGET_REPORT,
    },
    {
        "id": "nbt_release_acceptance",
        "cmd": [PYTHON, "-B", "tools/nbt_release_acceptance_package.py", "audit"],
        "expected_codes": [0],
        "expected_state": "PASS",
        "report": NBT_RELEASE_REPORT,
    },
    {
        "id": "nbt_state_ownership",
        "cmd": [PYTHON, "-B", "tools/nbt_state_ownership_queue.py", "audit"],
        "expected_codes": [0],
        "expected_state": "PASS",
        "report": NBT_OWNERSHIP_REPORT,
    },
    {
        "id": "god_power_manual_playtest_execution",
        "cmd": [PYTHON, "-B", "tools/god_power_manual_playtest_execution_queue.py", "audit"],
        "expected_codes": [1],
        "expected_state": "OPEN while 52 god powers await manual scenario playtests",
        "report": GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT,
    },
    {
        "id": "gap_audit",
        "cmd": [PYTHON, "-B", "tools/mythic_age_1_0_gap_audit.py", "audit"],
        "expected_codes": [0],
        "expected_state": "NOT READY with release blockers",
        "report": GAP_REPORT,
    },
    {
        "id": "requirement_trace",
        "cmd": [PYTHON, "-B", "tools/mythic_age_1_0_requirement_trace.py", "audit"],
        "expected_codes": [1],
        "expected_state": "OPEN while 1.0 requirements remain incomplete",
        "report": TRACE_REPORT,
    },
    {
        "id": "scope_matrix",
        "cmd": [PYTHON, "-B", "tools/mythic_age_1_0_scope_matrix.py", "audit"],
        "expected_codes": [1],
        "expected_state": "NOT READY while modules remain blocked/pending",
        "report": SCOPE_REPORT,
    },
    {
        "id": "gradle_build_offline",
        "cmd": ["./gradlew", "build", "--offline"],
        "expected_codes": [0],
        "expected_state": "external direct build gate; run outside this Python subprocess",
        "report": None,
        "skip_by_default": True,
        "skip_reason": (
            "Gradle wrapper lock files live outside the workspace sandbox when launched from a Python subprocess. "
            "Run ./gradlew build --offline directly as the build gate."
        ),
    },
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def run_command(spec: dict[str, Any], *, include_build: bool) -> dict[str, Any]:
    if spec.get("skip_by_default") and not include_build:
        return {
            "id": spec["id"],
            "cmd": spec["cmd"],
            "returncode": None,
            "expected_codes": spec["expected_codes"],
            "expected_state": spec["expected_state"],
            "command_ok": True,
            "skipped": True,
            "skip_reason": spec.get("skip_reason"),
            "report": project_path(spec["report"]) if spec.get("report") else None,
            "report_exists": bool(spec.get("report") and Path(spec["report"]).exists()),
            "output_tail": "",
        }
    proc = subprocess.run(
        spec["cmd"],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )
    output = proc.stdout or ""
    return {
        "id": spec["id"],
        "cmd": spec["cmd"],
        "returncode": proc.returncode,
        "expected_codes": spec["expected_codes"],
        "expected_state": spec["expected_state"],
        "command_ok": proc.returncode in spec["expected_codes"],
        "skipped": False,
        "skip_reason": None,
        "report": project_path(spec["report"]) if spec.get("report") else None,
        "report_exists": bool(spec.get("report") and Path(spec["report"]).exists()),
        "output_tail": "\n".join(output.splitlines()[-18:]),
    }


def build_summary(command_results: list[dict[str, Any]]) -> dict[str, Any]:
    gap = load_json(GAP_REPORT, {})
    trace = load_json(TRACE_REPORT, {})
    scope = load_json(SCOPE_REPORT, {})
    lock = load_json(PRODUCTION_LOCK_REPORT, {})
    review = load_json(DESIGN_APPROVAL_REVIEW_REPORT, {})
    nbt_target = load_json(NBT_TARGET_REPORT, {})
    nbt_release = load_json(NBT_RELEASE_REPORT, {})
    nbt_ownership = load_json(NBT_OWNERSHIP_REPORT, {})
    god_power_manual_execution = load_json(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT, {})

    gap_summary = gap.get("summary", {}) if isinstance(gap, dict) else {}
    release_blockers = [
        item.get("id")
        for item in gap.get("release_blocking_gaps", [])
        if isinstance(item, dict)
    ] if isinstance(gap, dict) else []

    return {
        "commands_ok": all(item["command_ok"] for item in command_results),
        "skipped_command_count": sum(1 for item in command_results if item.get("skipped")),
        "skipped_commands": [item["id"] for item in command_results if item.get("skipped")],
        "ready_for_1_0": bool(gap.get("ready_for_1_0")) and bool(trace.get("ready_for_1_0")) and not release_blockers,
        "release_blocking_gaps": release_blockers,
        "release_blocking_gap_count": len(release_blockers),
        "requirement_trace_status_counts": trace.get("status_counts"),
        "requirement_trace_open_requirement_count": trace.get("open_requirement_count"),
        "scope_module_status_counts": scope.get("module_status_counts"),
        "creature_design_lock_status": lock.get("status"),
        "creature_design_locked_count": lock.get("locked_count"),
        "creature_design_unlocked_count": lock.get("unlocked_count"),
        "creature_design_approval_review_status": review.get("status"),
        "creature_design_approval_pending_count": review.get("pending_review_count"),
        "creature_design_approval_batch_counts": review.get("batch_counts"),
        "creature_design_approval_missing_asset_count": review.get("missing_asset_count"),
        "civilization_count": len(gap_summary.get("civilizations", []) or []),
        "civilization_creature_count": gap_summary.get("civilization_creature_count"),
        "god_power_manual_playtest_required_count": gap_summary.get("god_power_gameplay_manual_playtest_required_count"),
        "god_power_manual_playtest_execution_status": god_power_manual_execution.get("status"),
        "god_power_manual_playtest_execution_batch_count": god_power_manual_execution.get("batch_count"),
        "god_power_manual_playtest_execution_scenario_total": god_power_manual_execution.get("scenario_template_total"),
        "god_power_manual_playtest_execution_world_edit_task_count": god_power_manual_execution.get("world_edit_task_count"),
        "creature_motion_missing_mode_count": gap_summary.get("creature_action_motion_sequence_missing_mode_count"),
        "creature_audio_manual_listening_queue_count": gap_summary.get("creature_audio_manual_listening_queue_count"),
        "nbt_target_gate_ok": bool(nbt_target.get("target_gate_ok")),
        "nbt_release_acceptance_ok": bool(nbt_release.get("ok")),
        "nbt_state_ownership_ok": bool(nbt_ownership.get("ok")),
    }


def build_report(*, include_build: bool) -> dict[str, Any]:
    command_results = [run_command(spec, include_build=include_build) for spec in COMMANDS]
    summary = build_summary(command_results)
    ok = bool(summary["commands_ok"]) and bool(summary["ready_for_1_0"])
    return {
        "version": 1,
        "scope": "mythic_age_1_0_unified_release_gate",
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "ok": ok,
        "status": "READY" if ok else "NOT_READY",
        "include_build": include_build,
        "rule": (
            "The release gate is green only when command execution is healthy and the current 1.0 "
            "gap/trace/scope evidence proves ready_for_1_0=true with no release blockers."
        ),
        "summary": summary,
        "commands": command_results,
        "evidence": {
            "gap_report": project_path(GAP_REPORT),
            "requirement_trace_report": project_path(TRACE_REPORT),
            "scope_matrix_report": project_path(SCOPE_REPORT),
            "creature_design_production_lock_report": project_path(PRODUCTION_LOCK_REPORT),
            "creature_design_approval_review_report": project_path(DESIGN_APPROVAL_REVIEW_REPORT),
            "nbt_target_report": project_path(NBT_TARGET_REPORT),
            "nbt_release_acceptance_report": project_path(NBT_RELEASE_REPORT),
            "nbt_state_ownership_report": project_path(NBT_OWNERSHIP_REPORT),
            "god_power_manual_playtest_execution_report": project_path(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT),
        },
        "next_work_order": [
            "B0: Review Pegasus/Hydra V6 modeling spec contracts and sheets; keep modeling locked until approved.",
            "B1: Review remaining Greek vertical slice designs and record explicit approved_for_modeling decisions.",
            "B2: Review Egyptian/Nordic/Chinese/Atlantean design packages before any final Blockbench/UV/texture work.",
            "After design unlock: rebuild/polish models, lock UVs, paint UV-driven textures, sync Java runtime, then capture motion.",
            "Parallel release work: complete 52 god-power manual playtests, 138 motion mode captures, audio listening/final mix, UI/particle/structure acceptance.",
        ],
    }


def write_markdown(payload: dict[str, Any]) -> None:
    summary = payload["summary"]
    lines = [
        "# MythicAgeMod 1.0 统一发布门禁（2026-06-15）",
        "",
        "本文档由 `tools/mythic_age_1_0_release_gate.py audit` 生成。它统一执行当前 1.0 关键 gate，并区分“命令失败”和“产品仍未就绪”。",
        "",
        "## 总览",
        "",
        f"- 发布门禁状态：`{payload['status']}`",
        f"- 命令执行健康：`{summary['commands_ok']}`",
        f"- 跳过命令：`{json.dumps(summary['skipped_commands'], ensure_ascii=False)}`",
        f"- 1.0 就绪：`{summary['ready_for_1_0']}`",
        f"- release blockers：`{json.dumps(summary['release_blocking_gaps'], ensure_ascii=False)}`",
        f"- 需求状态：`{json.dumps(summary['requirement_trace_status_counts'], ensure_ascii=False)}`",
        f"- 模块状态：`{json.dumps(summary['scope_module_status_counts'], ensure_ascii=False)}`",
        f"- 生物设计锁：`{summary['creature_design_lock_status']}`，locked={summary['creature_design_locked_count']}，unlocked={summary['creature_design_unlocked_count']}",
        f"- 生物审批包：`{summary['creature_design_approval_review_status']}`，pending={summary['creature_design_approval_pending_count']}，batches=`{json.dumps(summary['creature_design_approval_batch_counts'], ensure_ascii=False)}`",
        f"- 神力手工测试：`{summary['god_power_manual_playtest_execution_status']}`，batches={summary['god_power_manual_playtest_execution_batch_count']}，scenarios={summary['god_power_manual_playtest_execution_scenario_total']}，world_edit={summary['god_power_manual_playtest_execution_world_edit_task_count']}",
        f"- NBT：target={summary['nbt_target_gate_ok']}，release={summary['nbt_release_acceptance_ok']}，ownership={summary['nbt_state_ownership_ok']}",
        "",
        "## 命令结果",
        "",
        "| Gate | 结果 | 跳过 | 返回码 | 预期状态 | 报告 |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for item in payload["commands"]:
        lines.append(
            "| `{id}` | `{ok}` | `{skipped}` | `{code}` | {state} | `{report}` |".format(
                id=item["id"],
                ok=item["command_ok"],
                skipped=item.get("skipped", False),
                code=item["returncode"],
                state=item["expected_state"].replace("|", "/"),
                report=item["report"] or "-",
            )
        )

    lines.extend(["", "## 下一步顺序", ""])
    for item in payload["next_work_order"]:
        lines.append(f"- {item}")

    lines.extend(["", "## 失败输出尾部", ""])
    for item in payload["commands"]:
        if item["command_ok"]:
            continue
        lines.extend([
            f"### {item['id']}",
            "",
            "```text",
            item["output_tail"],
            "```",
            "",
        ])

    lines.extend([
        "## 证据",
        "",
    ])
    for key, value in payload["evidence"].items():
        lines.append(f"- `{key}`: `{value}`")
    MARKDOWN_PATH.parent.mkdir(parents=True, exist_ok=True)
    MARKDOWN_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    parser.add_argument(
        "--include-build",
        action="store_true",
        help="Run ./gradlew build --offline inside the Python subprocess. Default skips it; run Gradle directly instead.",
    )
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_report(include_build=args.include_build)
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_markdown(payload)
        print(f"MythicAgeMod 1.0 Release Gate: {payload['status']}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"commands_ok: {payload['summary']['commands_ok']}")
        print(f"release_blockers: {payload['summary']['release_blocking_gaps']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
