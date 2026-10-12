#!/usr/bin/env python3
"""Generate the MythicAgeMod 1.0 module scope and gap matrix.

This matrix is intentionally release-facing. It summarizes the whole 1.0
product surface instead of focusing only on Greek creature modeling.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
GAP_REPORT = ROOT / "docs/preview_assets/1_0_gap_audit/gap_report.json"
OUT_DIR = ROOT / "docs/preview_assets/1_0_scope_matrix"
REPORT_PATH = OUT_DIR / "mythic_age_1_0_scope_matrix.json"
MARKDOWN_PATH = ROOT / "docs/项目管理/MythicAgeMod_1.0模块范围与缺口矩阵_2026-06-15.md"


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def status(ok: bool, blockers: int = 0, pending: int = 0) -> str:
    if ok and blockers == 0 and pending == 0:
        return "verified"
    if blockers > 0:
        return "blocked"
    if pending > 0:
        return "manual_acceptance_pending"
    return "static_pass_runtime_pending"


def evidence_path(gap: dict[str, Any], key: str) -> str:
    value = gap.get("evidence", {}).get(key)
    return value if isinstance(value, str) else ""


def build_report() -> dict[str, Any]:
    gap = load_json(GAP_REPORT, {})
    summary = gap.get("summary", {}) if isinstance(gap, dict) else {}
    evidence = gap.get("evidence", {}) if isinstance(gap, dict) else {}
    release_blockers = gap.get("release_blocking_gaps", []) if isinstance(gap, dict) else []
    blocker_ids = [item.get("id") for item in release_blockers if isinstance(item, dict)]
    requirement_trace_counts = summary.get("requirement_trace_status_counts", {})
    if not isinstance(requirement_trace_counts, dict):
        requirement_trace_counts = {}

    visual_decisions = summary.get("creature_human_visual_acceptance_decision_counts", {})
    if not isinstance(visual_decisions, dict):
        visual_decisions = {}

    modules = [
        {
            "id": "release_management",
            "name": "版本与发布管理",
            "status": status(bool(summary.get("release_hygiene_ok")), pending=len(release_blockers)),
            "current_state": (
                f"1.0 ready={gap.get('ready_for_1_0')}; release blockers={len(release_blockers)}; "
                f"blockers={blocker_ids}; "
                f"requirement trace ok={summary.get('requirement_trace_ok')}; "
                f"requirement statuses={requirement_trace_counts}; "
                f"open requirements={summary.get('requirement_trace_open_requirement_count')}; "
                f"blocked requirements={summary.get('requirement_trace_blocked_requirements')}"
            ),
            "must_finish": [
                "release blocking gaps 清零",
                "需求追踪表全部变为 verified 或 verified_with_watchlist",
                "版本号、README、玩家指南、CHANGELOG 和 dashboard 对齐",
                "所有模块 gate 与人工验收均有可复验记录",
            ],
            "evidence": [
                project_path(GAP_REPORT),
                summary.get("release_acceptance_dashboard", ""),
                summary.get("requirement_trace_markdown", ""),
            ],
            "exit_gate": "tools/mythic_age_1_0_gap_audit.py audit 返回 ready_for_1_0=true，requirement trace 通过，且 ./gradlew build --offline 通过。",
        },
        {
            "id": "code_gameplay",
            "name": "代码与玩法",
            "status": status(
                bool(summary.get("certificate_economy_ok"))
                and bool(summary.get("god_power_gameplay_static_implementation_ok")),
                pending=int(summary.get("god_power_gameplay_manual_playtest_required_count") or 0),
            ),
            "current_state": (
                f"entity_manifest={summary.get('entity_manifest_count')}; "
                f"creatures={summary.get('civilization_creature_count')}; "
                f"god powers={summary.get('god_power_gameplay_review_board_count')}; "
                f"manual playtests={summary.get('god_power_gameplay_manual_playtest_required_count')}; "
                f"min scenarios={summary.get('god_power_manual_playtest_queue_total_minimum_scenarios')}; "
                f"manual execution={summary.get('god_power_manual_playtest_execution_status')} "
                f"batches={summary.get('god_power_manual_playtest_execution_batch_count')} "
                f"scenarios={summary.get('god_power_manual_playtest_execution_scenario_total')}; "
                f"certificate economy ok={summary.get('certificate_economy_ok')}; "
                f"creature gameplay static={summary.get('creature_gameplay_static_contract_ok')} "
                f"({summary.get('creature_gameplay_static_contract_ok_count')}/"
                f"{summary.get('creature_gameplay_creature_count')}); "
                f"creature gameplay release ready={summary.get('creature_gameplay_release_ready_count')}/"
                f"{summary.get('creature_gameplay_creature_count')}; "
                f"creature gameplay blockers={summary.get('creature_gameplay_release_blocker_counts')}"
            ),
            "must_finish": [
                "52 个神力完成人工玩法、目标选择、失败路径、冷却、消耗和性能验收",
                "按神力手工玩法测试执行队列填完 CSV 场景记录并回写通过结论",
                "18 个高风险神力优先做压力、回滚/降级和跨存档语义确认",
                "30 生物静态玩法闭环已可审计；发布前仍需设计、视觉、动作和实机行为验收全部通过",
                "证书经济从静态链路进入实机频率、防刷和价格平衡",
            ],
            "evidence": [
                evidence.get("god_power_gameplay_acceptance_report", ""),
                evidence.get("god_power_manual_playtest_execution_report", ""),
                evidence.get("god_power_manual_playtest_execution_markdown", ""),
                evidence.get("god_power_manual_playtest_execution_csv", ""),
                evidence.get("certificate_economy_report", ""),
                evidence.get("creature_gameplay_contract_report", ""),
                evidence.get("creature_gameplay_contract_markdown", ""),
            ],
            "exit_gate": "god_power_gameplay_acceptance_ok=true，certificate_economy_ok=true，且 creature_gameplay_contract ok=true。",
        },
        {
            "id": "models_art",
            "name": "模型与美术",
            "status": status(
                bool(summary.get("creature_human_visual_acceptance_ok")),
                blockers=int(summary.get("creature_visual_design_rejection_blocker_count") or 0),
                pending=int(summary.get("creature_visual_manual_review_required_count") or 0),
            ),
            "current_state": (
                f"design approvals={summary.get('creature_design_first_approved_count')}/"
                f"{summary.get('creature_design_first_creature_count')}; "
                f"design states={summary.get('creature_design_first_status_counts')}; "
                f"visual boards={summary.get('creature_visual_review_board_count')}/"
                f"{summary.get('creature_visual_acceptance_count')}; "
                f"quality blockers={summary.get('creature_visual_quality_contract_fail_count')}; "
                f"design rejections={summary.get('creature_visual_design_rejection_blocker_count')}; "
                f"Pegasus/Hydra design correctness={summary.get('pegasus_hydra_design_correctness_verdict')}; "
                f"Pegasus/Hydra V6 modeling spec={summary.get('pegasus_hydra_v6_modeling_spec_status')} "
                f"modeling_allowed={summary.get('pegasus_hydra_v6_modeling_allowed')}; "
                f"art rework unresolved={summary.get('creature_art_rework_queue_unresolved_count')}; "
                f"art rework priorities={summary.get('creature_art_rework_queue_priority_counts')}; "
                f"art production batches={summary.get('creature_art_production_batch_counts')}; "
                f"art production blocked={summary.get('creature_art_production_blocked_creature_count')}; "
                f"art production design blockers={summary.get('creature_art_production_design_blocker_count')}; "
                f"design production lock={summary.get('creature_design_production_lock_status')} "
                f"locked={summary.get('creature_design_production_locked_count')} "
                f"unlocked={summary.get('creature_design_production_unlocked_count')} "
                f"hard={summary.get('creature_design_production_hard_lock_count')} "
                f"approval={summary.get('creature_design_production_approval_lock_count')}; "
                f"design approval review={summary.get('creature_design_approval_review_status')} "
                f"pending={summary.get('creature_design_approval_review_pending_count')} "
                f"batches={summary.get('creature_design_approval_review_batch_counts')}; "
                f"human decisions={visual_decisions}"
            ),
            "must_finish": [
                "30 个生物先补正式设计先行签收，不能把生成型设计包当作批准",
                "按生物设计审批总包完成 B0/B1/B2 审查顺序和写回字段",
                "生产锁解除前，不允许进入最终 Blockbench、UV、纹理、Java runtime 或动作验收",
                "按生物美术生产路线图执行 B0/B1/B2/B3/B4 批次",
                "Pegasus/Hydra 先审查并签收 V6 可建模规格，再重建模型",
                "Greek 10 生物人工视觉复审并形成通过/返修/重写设计结论",
                "非希腊 20 生物按文明重做或精修为 1.0 成品级美术",
                "所有模型禁止大头补细节，必须用部件层级和 UV 细节表达神话身份",
            ],
            "evidence": [
                evidence.get("creature_design_first_gate_report", ""),
                evidence.get("creature_visual_acceptance_report", ""),
                evidence.get("creature_human_visual_acceptance_ledger", ""),
                evidence.get("pegasus_hydra_design_correctness_report", ""),
                evidence.get("pegasus_hydra_v6_modeling_spec_report", ""),
                evidence.get("pegasus_hydra_v6_modeling_spec_markdown", ""),
                evidence.get("creature_art_rework_queue_report", ""),
                evidence.get("creature_art_production_roadmap_report", ""),
                evidence.get("creature_art_production_roadmap_markdown", ""),
                evidence.get("creature_design_production_lock_report", ""),
                evidence.get("creature_design_production_lock_markdown", ""),
                evidence.get("creature_design_approval_review_report", ""),
                evidence.get("creature_design_approval_review_markdown", ""),
                evidence.get("creature_design_approval_review_contact_sheet", ""),
                evidence.get("greek_shape_acceptance_report", ""),
                "docs/preview_assets/greek_creatures/design_approval/detail_boards/pegasus_hydra_detail_design_boards.json",
            ],
            "exit_gate": "greek_design_first_gate、creature_visual_acceptance_package、creature_human_visual_acceptance_ledger 全部通过。",
        },
        {
            "id": "textures_uv",
            "name": "纹理与 UV",
            "status": status(
                bool(summary.get("creature_visual_quality_contract_ok"))
                and int(summary.get("creature_visual_quality_contract_fail_count") or 0) == 0,
                pending=int(summary.get("creature_visual_manual_review_required_count") or 0),
            ),
            "current_state": (
                f"texture/UV quality contract ok={summary.get('creature_visual_quality_contract_ok')}; "
                f"failures={summary.get('creature_visual_quality_contract_fail_count')}; "
                f"manual review required={summary.get('creature_visual_manual_review_required_count')}"
            ),
            "must_finish": [
                "每个生物关键面固定 UV：眼、嘴、牙、角、翼、尾、武器、符号",
                "纯白、深色或大面积色块必须有结构阴影，不能像空白/透明破面",
                "禁止通用随机面板纹理作为最终资源",
                "Blockbench 三视图和 texture-fit preview 由人工验收确认",
            ],
            "evidence": [
                evidence.get("creature_visual_acceptance_report", ""),
            ],
            "exit_gate": "texture-fit、UV 空白检查、人工视觉台账全部通过。",
        },
        {
            "id": "actions_animation",
            "name": "动作与动画",
            "status": status(
                bool(summary.get("creature_human_motion_acceptance_ok")),
                pending=int(summary.get("creature_action_motion_sequence_missing_mode_count") or 0),
            ),
            "current_state": (
                f"captured modes={summary.get('creature_action_motion_sequence_captured_required_mode_count')}/"
                f"{summary.get('creature_action_motion_sequence_required_mode_count')}; "
                f"missing={summary.get('creature_action_motion_sequence_missing_mode_count')}; "
                f"frames={summary.get('creature_action_motion_sequence_frame_count')}; "
                f"capture queue={summary.get('creature_action_motion_capture_queue_count')}; "
                f"batches={summary.get('creature_action_motion_capture_plan_batch_count')}; "
                f"rework queued={summary.get('creature_motion_rework_queued_task_count')}; "
                f"blocked by art/design={summary.get('creature_motion_rework_blocked_task_count')}; "
                f"blocked creatures={summary.get('creature_motion_rework_blocked_creatures')}; "
                f"rework states={summary.get('creature_motion_rework_task_state_counts')}"
            ),
            "must_finish": [
                "补齐 138 项缺失动作模式",
                "Pegasus 需要自主飞行与骑乘飞行，且等设计通过后重做",
                "Hydra 需要 V6 九头错相 idle/attack/hurt，且任何动作不断颈；旧五头动作证据不作为 1.0 依据",
                "每个生物 idle、移动、攻击/特殊、受击、死亡有人工结论",
            ],
            "evidence": [
                evidence.get("creature_action_acceptance_report", ""),
                evidence.get("creature_motion_rework_queue_report", ""),
                summary.get("creature_action_motion_sequence_manifest", ""),
            ],
            "exit_gate": "creature_human_motion_acceptance_ok=true 且 full-mode sequence coverage 通过。",
        },
        {
            "id": "ui_player_guidance",
            "name": "UI 与玩家引导",
            "status": status(bool(summary.get("ui_static_gate_ok")), pending=1),
            "current_state": (
                f"UI static ok={summary.get('ui_static_gate_ok')}; "
                f"guide scope ok={summary.get('ui_guide_scope_ok')}"
            ),
            "must_finish": [
                "守护者交易 UI 实机验收：文明、余额、价格、材料、冷却、失败原因",
                "神力图鉴 UI 实机验收：效果、消耗、冷却、来源、风险和状态",
                "GUI scale、长文本、多语言、低分辨率不重叠不截断",
                "神殿/证书/神像状态给玩家清楚反馈",
            ],
            "evidence": [
                evidence.get("ui_report", ""),
            ],
            "exit_gate": "UI 静态 gate 加实机分辨率/多语言验收通过。",
        },
        {
            "id": "particles_vfx",
            "name": "粒子与特效",
            "status": status(
                bool(summary.get("particle_static_gate_ok"))
                and bool(summary.get("god_power_particle_full_matrix_ok")),
                pending=52,
            ),
            "current_state": (
                f"particle static={summary.get('particle_static_gate_ok')}; "
                f"god power full matrix={summary.get('god_power_particle_full_matrix_ok')}; "
                f"gameplay manual playtests={summary.get('god_power_gameplay_manual_playtest_required_count')}; "
                f"manual execution scenarios={summary.get('god_power_manual_playtest_execution_scenario_total')}"
            ),
            "must_finish": [
                "52 神力 cast、active/impact、end 三段式实机可读性验收",
                "高风险大范围效果做性能压力和降级策略验证",
                "生物技能粒子不遮挡眼、嘴、武器、翼根、尾刺和多头结构",
                "粒子主题必须符合对应文明美术语言",
            ],
            "evidence": [
                evidence.get("particle_report", ""),
                evidence.get("god_power_particle_report", ""),
                evidence.get("god_power_gameplay_acceptance_report", ""),
                evidence.get("god_power_manual_playtest_execution_report", ""),
            ],
            "exit_gate": "god_power_particle_full_matrix_ok=true 且玩法/压力验收完成。",
        },
        {
            "id": "audio_feedback",
            "name": "音效与反馈",
            "status": status(
                bool(summary.get("creature_audio_final_mix_acceptance_ok")),
                pending=int(summary.get("creature_audio_manual_listening_queue_count") or 0),
            ),
            "current_state": (
                f"events={summary.get('creature_audio_audition_event_count')}; "
                f"manual listening={summary.get('creature_audio_manual_listening_queue_count')}; "
                f"final mix candidates={summary.get('creature_audio_final_mix_candidate_count')}; "
                f"loudness metrics available={summary.get('creature_audio_loudness_metrics_available')}; "
                f"missing tools={summary.get('creature_audio_tooling_missing_tools')}"
            ),
            "must_finish": [
                "149 个生物音频事件完成听感验收",
                "21 个 final-mix candidate 生物有最终通过/替换结论",
                "补齐响度/容器工具链或等价 MCP 能力",
                "神力和 UI 音效补 cast/impact/end、交易成功/失败、证书提示",
            ],
            "evidence": [
                evidence.get("creature_audio_final_mix_report", ""),
                evidence.get("audio_report", ""),
            ],
            "exit_gate": "creature_audio_final_mix_acceptance_ok=true，响度/听感报告完整。",
        },
        {
            "id": "nbt_persistence",
            "name": "NBT 与持久化",
            "status": status(bool(summary.get("nbt_real_restart_cycle_ok")), pending=int(summary.get("nbt_target_runtime_only_count") or 0)),
            "current_state": (
                f"schema owners={summary.get('nbt_schema_owner_count')}; "
                f"real restart cycle={summary.get('nbt_real_restart_cycle_ok')}; "
                f"required={summary.get('nbt_target_required_count')}; "
                f"runtime_only={summary.get('nbt_target_runtime_only_count')}; "
                f"state ownership={summary.get('nbt_state_ownership_status_counts')}"
            ),
            "must_finish": [
                "当前 NBT release package 作为回归保留",
                "任一新增长期状态先升为 required，再补 schema、round-trip 和真实重启验证",
                "复查持续神力、证书经济、结构/神殿状态的跨存档语义",
                "不要把 runtime_only 状态误判为发布持久化完成",
            ],
            "evidence": [
                evidence.get("nbt_release_acceptance_report", ""),
                evidence.get("nbt_state_ownership_queue_report", ""),
                evidence.get("nbt_target_report", ""),
                evidence.get("nbt_restart_contract_report", ""),
            ],
            "exit_gate": "nbt_release_acceptance_package.py audit 通过，并且新增 required 状态全部有重启回归。",
        },
        {
            "id": "structures_worldgen",
            "name": "结构与世界生成",
            "status": status(bool(summary.get("structure_quality_gate_ok")), pending=1),
            "current_state": (
                f"canonical structures={summary.get('structure_canonical_existing')}/"
                f"{summary.get('structure_canonical_expected')}; "
                f"theme contracts={summary.get('structure_theme_contract_checked')}/"
                f"{summary.get('structure_theme_contract_expected')}; "
                f"functional points={summary.get('structure_functional_point_checked')}/"
                f"{summary.get('structure_functional_point_expected')}"
            ),
            "must_finish": [
                "15 个 canonical 神殿实机加载并做建筑审美验收",
                "文明材料、体量、入口、守护者、祭坛、奖励点位可读",
                "世界生成频率、稀有度和玩家进程匹配",
                "数据包内无非 canonical 结构污染",
            ],
            "evidence": [
                evidence.get("structure_report", ""),
            ],
            "exit_gate": "structure_validation 通过，并有实机建筑审美/功能点位结论。",
        },
    ]

    status_counts: dict[str, int] = {}
    for module in modules:
        status_counts[module["status"]] = status_counts.get(module["status"], 0) + 1

    return {
        "version": 1,
        "scope": "mythic_age_1_0_module_scope_and_gap_matrix",
        "source_gap_report": project_path(GAP_REPORT),
        "ready_for_1_0": bool(gap.get("ready_for_1_0")),
        "release_blocking_gap_count": len(release_blockers),
        "release_blocking_gaps": blocker_ids,
        "module_count": len(modules),
        "module_status_counts": status_counts,
        "rule": "1.0 is complete only when every module is verified and the gap audit reports ready_for_1_0=true.",
        "modules": modules,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 模块范围与缺口矩阵（2026-06-15）",
        "",
        "本文档由 `tools/mythic_age_1_0_scope_matrix.py audit` 生成。它把 1.0 从“希腊生物继续补模型”扩展回完整发布范围：代码、模型、动作、纹理、UI、美术、粒子、音效、NBT、结构和世界生成。",
        "",
        "## 总览",
        "",
        f"- 1.0 就绪：`{payload['ready_for_1_0']}`",
        f"- release blocking gaps：{payload['release_blocking_gap_count']}",
        f"- blocking gap IDs：`{json.dumps(payload['release_blocking_gaps'], ensure_ascii=False)}`",
        f"- 模块数量：{payload['module_count']}",
        f"- 模块状态统计：`{json.dumps(payload['module_status_counts'], ensure_ascii=False)}`",
        f"- 判定规则：{payload['rule']}",
        "",
        "## 模块矩阵",
        "",
        "| 模块 | 状态 | 当前证据摘要 | 退出 gate |",
        "| --- | --- | --- | --- |",
    ]
    for module in payload["modules"]:
        lines.append(
            "| `{id}` {name} | `{status}` | {current} | {exit_gate} |".format(
                id=module["id"],
                name=module["name"],
                status=module["status"],
                current=module["current_state"].replace("|", "/"),
                exit_gate=module["exit_gate"].replace("|", "/"),
            )
        )
    lines.extend(["", "## 详细缺口", ""])
    for module in payload["modules"]:
        lines.extend([
            f"### {module['name']} (`{module['id']}`)",
            "",
            f"- 状态：`{module['status']}`",
            f"- 当前证据：{module['current_state']}",
            "- 必须完成：",
            "",
        ])
        for item in module["must_finish"]:
            lines.append(f"  - {item}")
        lines.extend(["", "- 证据：", ""])
        for item in module["evidence"]:
            if item:
                lines.append(f"  - `{item}`")
        lines.extend(["", f"- 退出 gate：{module['exit_gate']}", ""])
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
        status_text = "READY" if payload["ready_for_1_0"] else "NOT READY"
        print(f"MythicAgeMod 1.0 Scope Matrix: {status_text}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"module_status_counts: {payload['module_status_counts']}")
        return 0 if payload["ready_for_1_0"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
