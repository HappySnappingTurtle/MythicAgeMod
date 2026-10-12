#!/usr/bin/env python3
"""Trace the original 1.0 goal to current evidence, gaps, and next work.

This report is intentionally user-requirement-facing. The gap audit aggregates
technical evidence; this tool maps that evidence back to the requested product
surface: code, five civilizations, models, actions, textures, UI, art, VFX,
audio, structures, and NBT.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
GAP_REPORT = ROOT / "docs/preview_assets/1_0_gap_audit/gap_report.json"
CREATURE_GAMEPLAY_CONTRACT = ROOT / "docs/preview_assets/creature_gameplay_contract/creature_gameplay_contract.json"
CREATURE_GAMEPLAY_CONTRACT_MD = ROOT / "docs/系统设计/MythicAgeMod_1.0生物玩法闭环契约_2026-06-15.md"
CREATURE_ART_PRODUCTION_ROADMAP = ROOT / "docs/preview_assets/creature_art_production_roadmap/creature_art_production_roadmap.json"
CREATURE_ART_PRODUCTION_ROADMAP_MD = ROOT / "docs/项目管理/MythicAgeMod_1.0生物美术生产路线图_2026-06-15.md"
CREATURE_DESIGN_PRODUCTION_LOCK = ROOT / "docs/preview_assets/creature_design_production_lock/creature_design_production_lock.json"
CREATURE_DESIGN_PRODUCTION_LOCK_MD = ROOT / "docs/项目管理/MythicAgeMod_1.0生物设计生产锁_2026-06-15.md"
CREATURE_DESIGN_APPROVAL_REVIEW = ROOT / "docs/preview_assets/creature_design_approval_review/creature_design_approval_review_package.json"
CREATURE_DESIGN_APPROVAL_REVIEW_MD = ROOT / "docs/项目管理/MythicAgeMod_1.0生物设计审批总包_2026-06-15.md"
OUT_DIR = ROOT / "docs/preview_assets/1_0_requirement_trace"
REPORT_PATH = OUT_DIR / "mythic_age_1_0_requirement_trace.json"
MARKDOWN_PATH = ROOT / "docs/项目管理/MythicAgeMod_1.0需求追踪与开发缺口_2026-06-15.md"

PASSING_STATUSES = {"verified", "verified_with_watchlist"}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def as_int(value: Any) -> int:
    if isinstance(value, bool):
        return int(value)
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return int(value)
    return 0


def req(
    *,
    requirement_id: str,
    user_requirement: str,
    status: str,
    current_evidence: list[str],
    missing_or_weak_evidence: list[str],
    next_actions: list[str],
    authoritative_evidence: list[str],
    priority: str,
    release_relevant: bool = True,
) -> dict[str, Any]:
    return {
        "id": requirement_id,
        "priority": priority,
        "status": status,
        "release_relevant": release_relevant,
        "user_requirement": user_requirement,
        "current_evidence": current_evidence,
        "missing_or_weak_evidence": missing_or_weak_evidence,
        "next_actions": next_actions,
        "authoritative_evidence": authoritative_evidence,
        "done_for_1_0": status in PASSING_STATUSES,
    }


def build_report() -> dict[str, Any]:
    gap = load_json(GAP_REPORT, {})
    creature_gameplay = load_json(CREATURE_GAMEPLAY_CONTRACT, {})
    summary = gap.get("summary", {}) if isinstance(gap, dict) else {}
    evidence = gap.get("evidence", {}) if isinstance(gap, dict) else {}
    release_blockers = gap.get("release_blocking_gaps", []) if isinstance(gap, dict) else []
    release_blocker_ids = [
        str(item.get("id"))
        for item in release_blockers
        if isinstance(item, dict)
    ]

    creature_count = as_int(summary.get("civilization_creature_count"))
    design_approved = as_int(summary.get("creature_design_first_approved_count"))
    design_total = as_int(summary.get("creature_design_first_creature_count"))
    non_greek_upgrade_count = as_int(summary.get("creature_art_rework_queue_non_greek_upgrade_count"))
    god_power_manual = as_int(summary.get("god_power_gameplay_manual_playtest_required_count"))
    motion_missing = as_int(summary.get("creature_action_motion_sequence_missing_mode_count"))
    motion_required = as_int(summary.get("creature_action_motion_sequence_required_mode_count"))
    motion_captured = as_int(summary.get("creature_action_motion_sequence_captured_required_mode_count"))
    audio_queue = as_int(summary.get("creature_audio_manual_listening_queue_count"))
    nbt_required = as_int(summary.get("nbt_target_required_count"))
    nbt_runtime_only = as_int(summary.get("nbt_target_runtime_only_count"))

    requirements = [
        req(
            requirement_id="scope_five_civilizations",
            priority="P0",
            status=(
                "static_pass_runtime_pending"
                if len(summary.get("civilizations", [])) == 5 and creature_count == 30
                else "blocked"
            ),
            user_requirement="1.0 必须是五文明基础可玩版本，不是只有希腊生物。",
            current_evidence=[
                f"civilizations={summary.get('civilizations')}",
                f"civilization_creature_count={creature_count}",
                f"creature_roster={summary.get('civilization_creature_roster')}",
                f"structure canonical={summary.get('structure_canonical_existing')}/{summary.get('structure_canonical_expected')}",
                f"god power review boards={summary.get('god_power_gameplay_review_board_count')}",
            ],
            missing_or_weak_evidence=[
                "五文明内容虽然有静态资源和注册证据，但人工玩法、美术、动作、粒子和结构实机验收未闭环。",
                f"当前 release blockers={release_blocker_ids}",
            ],
            next_actions=[
                "保留五文明 30 生物、52 神力、15 canonical 神殿作为 1.0 范围，不再缩成希腊-only。",
                "每个文明按同一套设计、模型、动作、纹理、UI/反馈、粒子、NBT 退出标准推进。",
            ],
            authoritative_evidence=[
                project_path(GAP_REPORT),
                evidence.get("creature_design_first_gate_report", ""),
                evidence.get("structure_report", ""),
                evidence.get("god_power_gameplay_acceptance_report", ""),
            ],
        ),
        req(
            requirement_id="code_gameplay_systems",
            priority="P1",
            status=(
                "manual_acceptance_pending"
                if summary.get("certificate_economy_ok")
                and summary.get("god_power_gameplay_static_implementation_ok")
                else "blocked"
            ),
            user_requirement="代码与玩法要形成五文明闭环：实体、AI、神力、证书经济、守护者交易和失败路径。",
            current_evidence=[
                f"entity_manifest_count={summary.get('entity_manifest_count')}",
                f"certificate_economy_ok={summary.get('certificate_economy_ok')}",
                f"guide_spell_total={summary.get('certificate_guide_spell_total')}",
                f"custom_guardian_spell_trade_total={summary.get('certificate_custom_guardian_spell_trade_total')}",
                f"god_power_static_implementation_ok={summary.get('god_power_gameplay_static_implementation_ok')}",
                f"god_power_runtime_smoke={summary.get('god_power_runtime_smoke_probe_passed_power_count')}/{summary.get('god_power_runtime_smoke_probe_power_count')}",
                f"god_power_manual_execution={summary.get('god_power_manual_playtest_execution_status')} "
                f"batches={summary.get('god_power_manual_playtest_execution_batch_count')} "
                f"scenarios={summary.get('god_power_manual_playtest_execution_scenario_total')}",
                f"creature_gameplay_static_contract={creature_gameplay.get('static_code_contract_ok')} ({creature_gameplay.get('static_code_contract_ok_count')}/{creature_gameplay.get('creature_count')})",
                f"creature_gameplay_release_ready={creature_gameplay.get('release_gameplay_ready_count')}/{creature_gameplay.get('creature_count')}",
            ],
            missing_or_weak_evidence=[
                f"{god_power_manual} 个神力仍需人工玩法验收。",
                "30 生物静态代码闭环已经可审计，但设计/视觉/动作发布依赖未过，不能算玩法发布 ready。",
                "证书经济通过静态矩阵，但实机频率、防刷和价格平衡还未作为发布验收闭环。",
            ],
            next_actions=[
                "先验收 18 个高风险神力的性能、失败路径、回滚/降级和跨存档语义。",
                "按神力手工玩法测试执行队列逐批记录 52 个神力的场景结果。",
                "为 30 生物建立生成/召唤/掉落/AI 玩法复查表。",
                "用实机交易流程验证证书余额、冷却、防刷和 UI 提示。",
            ],
            authoritative_evidence=[
                evidence.get("god_power_gameplay_acceptance_report", ""),
                evidence.get("god_power_manual_playtest_execution_report", ""),
                evidence.get("god_power_manual_playtest_execution_markdown", ""),
                evidence.get("god_power_manual_playtest_execution_csv", ""),
                evidence.get("certificate_economy_report", ""),
                evidence.get("entity_manifest", ""),
                project_path(CREATURE_GAMEPLAY_CONTRACT),
                project_path(CREATURE_GAMEPLAY_CONTRACT_MD),
            ],
        ),
        req(
            requirement_id="creature_design_models_art",
            priority="P0",
            status="blocked",
            user_requirement="模型与美术必须达到 1.0 质量，不能只有不好看的希腊生物；天马和九头蛇不能继续沿用畸形设计。",
            current_evidence=[
                f"design approvals={design_approved}/{design_total}",
                f"design_status_counts={summary.get('creature_design_first_status_counts')}",
                f"Pegasus/Hydra verdict={summary.get('pegasus_hydra_design_correctness_verdict')}",
                f"Pegasus/Hydra V6 modeling spec={summary.get('pegasus_hydra_v6_modeling_spec_status')} "
                f"verdict={summary.get('pegasus_hydra_v6_modeling_spec_verdict')} "
                f"modeling_allowed={summary.get('pegasus_hydra_v6_modeling_allowed')}",
                f"creature_gameplay_static_contract={creature_gameplay.get('static_code_contract_ok')} ({creature_gameplay.get('static_code_contract_ok_count')}/{creature_gameplay.get('creature_count')})",
                f"art_rework_unresolved={summary.get('creature_art_rework_queue_unresolved_count')}",
                f"art_rework_priorities={summary.get('creature_art_rework_queue_priority_counts')}",
                f"art_production_batches={summary.get('creature_art_production_batch_counts')}",
                f"art_production_blocked={summary.get('creature_art_production_blocked_creature_count')}",
                f"design_production_lock={summary.get('creature_design_production_lock_status')} "
                f"locked={summary.get('creature_design_production_locked_count')} "
                f"unlocked={summary.get('creature_design_production_unlocked_count')}",
                f"design_approval_review={summary.get('creature_design_approval_review_status')} "
                f"pending={summary.get('creature_design_approval_review_pending_count')} "
                f"batches={summary.get('creature_design_approval_review_batch_counts')}",
                f"human_visual_decisions={summary.get('creature_human_visual_acceptance_decision_counts')}",
            ],
            missing_or_weak_evidence=[
                "30 个生物当前没有任何一个正式 approved_for_modeling。",
                "Pegasus/Hydra 当前设计被拒绝，V6 可建模规格已生成但未签收，modeling_allowed=false。",
                "生产锁已阻止 30 生物进入最终 Blockbench、UV、纹理、Java runtime 和动作生产。",
                "非希腊 20 生物仍在 P1 美术升级队列，不能算最终 1.0 资产。",
                "人工视觉台账仍未批准 30 生物的形体、比例、神话特征和纹理表现。",
            ],
            next_actions=[
                "按生物美术生产路线图执行 B0/B1/B2/B3/B4 批次，禁止跳过设计签收。",
                "使用生物设计审批总包按 B0/B1/B2 顺序做人工设计审查。",
                "先签收 Pegasus/Hydra V6 可建模规格：天马以原版成年马比例为主体，九头蛇以九头 socket 和真实颈身连接为硬约束。",
                "按设计包重建被拒绝生物，再同步 Blockbench、UV、纹理和 Java runtime model。",
                "逐文明完成 30 生物人工视觉验收，失败项回设计包而不是测试阶段试错。",
            ],
            authoritative_evidence=[
                evidence.get("creature_design_first_gate_report", ""),
                evidence.get("pegasus_hydra_design_correctness_report", ""),
                evidence.get("pegasus_hydra_v6_modeling_spec_report", ""),
                evidence.get("pegasus_hydra_v6_modeling_spec_markdown", ""),
                evidence.get("creature_art_rework_queue_report", ""),
                project_path(CREATURE_ART_PRODUCTION_ROADMAP),
                project_path(CREATURE_ART_PRODUCTION_ROADMAP_MD),
                project_path(CREATURE_DESIGN_PRODUCTION_LOCK),
                project_path(CREATURE_DESIGN_PRODUCTION_LOCK_MD),
                project_path(CREATURE_DESIGN_APPROVAL_REVIEW),
                project_path(CREATURE_DESIGN_APPROVAL_REVIEW_MD),
                evidence.get("creature_human_visual_acceptance_ledger", ""),
                project_path(CREATURE_GAMEPLAY_CONTRACT),
            ],
        ),
        req(
            requirement_id="non_greek_creature_completion",
            priority="P1",
            status="manual_acceptance_pending",
            user_requirement="埃及、北欧、中国、亚特兰蒂斯不能只是占位，必须有各自文明生物成品级模型和美术。",
            current_evidence=[
                f"non_greek_art_upgrade_count={non_greek_upgrade_count}",
                f"art_production_batches={summary.get('creature_art_production_batch_counts')}",
                f"design_production_lock_counts={summary.get('creature_design_production_step_lock_counts')}",
                f"runtime_reports={evidence.get('runtime_reports')}",
                f"visual_reports={evidence.get('visual_reports')}",
            ],
            missing_or_weak_evidence=[
                "非希腊 20 生物有设计包、预览和 runtime 契约，但缺正式设计签收和人工视觉通过。",
                "文明材质、轮廓、武器/符号、神话特征仍需从工程首版提升到成品级。",
            ],
            next_actions=[
                "先把 20 个非希腊生物推进 B2 设计批准，再进入 B3 Blockbench/UV/纹理生产。",
                "按 Egyptian/Nordic/Chinese/Atlantean 四批做设计签收和美术重做。",
                "每批要求概念图、Blockbench 三视图、UV、贴图、runtime 模型和人工结论一致。",
            ],
            authoritative_evidence=[
                evidence.get("creature_art_rework_queue_report", ""),
                project_path(CREATURE_ART_PRODUCTION_ROADMAP),
                project_path(CREATURE_ART_PRODUCTION_ROADMAP_MD),
                project_path(CREATURE_DESIGN_PRODUCTION_LOCK),
                evidence.get("creature_visual_acceptance_report", ""),
            ],
        ),
        req(
            requirement_id="textures_uv_materials",
            priority="P0",
            status="manual_acceptance_pending",
            user_requirement="纹理必须 UV 驱动，眼、嘴、角、翼、尾、符号等关键细节不能空白、错位或随机。",
            current_evidence=[
                f"visual_quality_contract_ok={summary.get('creature_visual_quality_contract_ok')}",
                f"visual_quality_failures={summary.get('creature_visual_quality_contract_fail_count')}",
                f"manual_visual_review_required={summary.get('creature_visual_manual_review_required_count')}",
                f"texture/UV evidence report={evidence.get('creature_visual_acceptance_report')}",
            ],
            missing_or_weak_evidence=[
                "自动 texture-fit 只能检查空白/越界，不能证明眼嘴和神话特征美术正确。",
                "纯白、深色和大面积单色资产仍需人工检查结构阴影和材质层次。",
                "Pegasus/Hydra 设计未通过前，其纹理不能作为最终依据。",
            ],
            next_actions=[
                "每个生物保留 UV 模板和关键面清单，纹理绘制必须绑定设计包。",
                "人工验收重点检查白色空白感、深色细节丢失、面部五官、翼羽、尾刺、多头连接阴影。",
            ],
            authoritative_evidence=[
                evidence.get("creature_visual_acceptance_report", ""),
                evidence.get("creature_design_first_gate_report", ""),
            ],
        ),
        req(
            requirement_id="actions_animation_motion",
            priority="P1",
            status="blocked",
            user_requirement="动作要覆盖待机、移动、攻击、特殊、受击、死亡，以及飞行/骑乘/多头等特殊动作。",
            current_evidence=[
                f"required motion modes covered={motion_captured}/{motion_required}",
                f"missing_motion_modes={motion_missing}",
                f"motion_rework_queued={summary.get('creature_motion_rework_queued_task_count')}",
                f"motion_rework_blocked={summary.get('creature_motion_rework_blocked_task_count')}",
                f"blocked_creatures={summary.get('creature_motion_rework_blocked_creatures')}",
                f"minimum_frame_budget={summary.get('creature_motion_rework_minimum_frame_budget')}",
            ],
            missing_or_weak_evidence=[
                "138 项动作模式缺 MC 连续帧/clip 证据。",
                "Pegasus 飞行/骑乘飞行和 Hydra 九头错相动作被设计/美术阻断。",
                "静态 setAngles 或 storyboard 不等于发布动作验收。",
            ],
            next_actions=[
                "先等设计阻断生物按 V6 签收规格完成模型重建，再捕获其飞行/多头动作。",
                "按 16 批 motion capture 计划补足连续帧、触发条件和人工 motion review note。",
            ],
            authoritative_evidence=[
                evidence.get("creature_action_acceptance_report", ""),
                evidence.get("creature_motion_rework_queue_report", ""),
            ],
        ),
        req(
            requirement_id="ui_player_guidance",
            priority="P1",
            status="static_pass_runtime_pending",
            user_requirement="UI 要让玩家看懂文明、证书、神力、交易、冷却、失败原因和进度。",
            current_evidence=[
                f"ui_static_gate_ok={summary.get('ui_static_gate_ok')}",
                f"ui_guide_scope_ok={summary.get('ui_guide_scope_ok')}",
            ],
            missing_or_weak_evidence=[
                "守护者交易 UI 和神力图鉴有静态 gate，但缺真实分辨率、GUI scale、长文本和多语言验收。",
                "神殿状态、证书余额、失败原因、文明主题反馈还需要实机走查。",
            ],
            next_actions=[
                "用多个 GUI scale/分辨率走查交易 UI 与神力图鉴。",
                "冻结 UI 文案后做多语言和长文本不重叠检查。",
            ],
            authoritative_evidence=[
                evidence.get("ui_report", ""),
            ],
        ),
        req(
            requirement_id="particles_vfx",
            priority="P1",
            status="manual_acceptance_pending",
            user_requirement="粒子效果要覆盖神力和生物技能，具有文明主题、三段式反馈和性能降级。",
            current_evidence=[
                f"particle_static_gate_ok={summary.get('particle_static_gate_ok')}",
                f"god_power_particle_full_matrix_ok={summary.get('god_power_particle_full_matrix_ok')}",
                f"manual_playtest_required={god_power_manual}",
                f"manual_execution_scenarios={summary.get('god_power_manual_playtest_execution_scenario_total')}",
            ],
            missing_or_weak_evidence=[
                "52 神力静态粒子矩阵通过，但实机可读性和压力/降级未完成。",
                "生物技能粒子还要验证不遮挡眼、嘴、武器、翼根、尾刺和多头结构。",
            ],
            next_actions=[
                "按神力批次记录 cast、active/impact、end 三段截图/结论。",
                "高风险大范围粒子先做帧率和粒子数量降级策略验证。",
            ],
            authoritative_evidence=[
                evidence.get("particle_report", ""),
                evidence.get("god_power_particle_report", ""),
                evidence.get("god_power_gameplay_acceptance_report", ""),
                evidence.get("god_power_manual_playtest_execution_report", ""),
            ],
        ),
        req(
            requirement_id="art_direction_structures_world",
            priority="P1",
            status="manual_acceptance_pending",
            user_requirement="美术不仅是生物，还包括五文明建筑、方块、物品、UI 和粒子的统一视觉语言。",
            current_evidence=[
                f"m1_reference_docs_ok={summary.get('m1_reference_docs_ok')}",
                f"structure_quality_gate_ok={summary.get('structure_quality_gate_ok')}",
                f"structures={summary.get('structure_canonical_existing')}/{summary.get('structure_canonical_expected')}",
                f"theme_contracts={summary.get('structure_theme_contract_checked')}/{summary.get('structure_theme_contract_expected')}",
            ],
            missing_or_weak_evidence=[
                "结构 gate 是静态质量，仍缺 15 个 canonical 神殿在地形中的建筑审美和功能点位验收。",
                "物品、方块、UI、粒子和生物之间的文明主题一致性仍需人工 art-final 审查。",
            ],
            next_actions=[
                "做 15 个神殿 small/medium/large 实机加载审美验收。",
                "按文明对齐材质、色彩、符号、建筑轮廓、UI 装饰和粒子语言。",
            ],
            authoritative_evidence=[
                evidence.get("structure_report", ""),
                "docs/项目管理/MythicAgeMod_1.0重整开发总纲_2026-06-15.md",
            ],
        ),
        req(
            requirement_id="audio_feedback",
            priority="P1",
            status="manual_acceptance_pending",
            user_requirement="音效和反馈要支撑生物、神力、UI、证书和战斗读秒，不只技术可播放。",
            current_evidence=[
                f"audio_technical_gate_ok={summary.get('audio_technical_gate_ok')}",
                f"creature_audio_events={summary.get('creature_audio_audition_event_count')}",
                f"manual_listening_queue={audio_queue}",
                f"final_mix_candidates={summary.get('creature_audio_final_mix_candidate_count')}",
                f"missing_tools={summary.get('creature_audio_tooling_missing_tools')}",
            ],
            missing_or_weak_evidence=[
                "149 个生物音频事件仍需人工听感验收。",
                "21 个 final-mix candidate 需最终通过/替换结论。",
                "响度/容器工具或等价 MCP 能力仍需补齐。",
            ],
            next_actions=[
                "先补响度/容器检测能力，再按 15 批完成试听和人工结论。",
                "补神力 cast/impact/end、交易成功/失败、证书提示等 UI/玩法音效审计。",
            ],
            authoritative_evidence=[
                evidence.get("creature_audio_final_mix_report", ""),
                evidence.get("audio_report", ""),
            ],
        ),
        req(
            requirement_id="nbt_persistence",
            priority="P1",
            status=(
                "verified_with_watchlist"
                if summary.get("nbt_release_acceptance_package_ok")
                and summary.get("nbt_real_restart_cycle_ok")
                and summary.get("nbt_state_ownership_queue_ok")
                and nbt_required == 0
                else "blocked"
            ),
            user_requirement="NBT 结构要正确覆盖长期状态；新增持久化状态必须有 schema、probe 和真实重启回归。",
            current_evidence=[
                f"nbt_release_acceptance_package_ok={summary.get('nbt_release_acceptance_package_ok')}",
                f"nbt_real_restart_cycle_ok={summary.get('nbt_real_restart_cycle_ok')}",
                f"nbt_state_ownership_status_counts={summary.get('nbt_state_ownership_status_counts')}",
                f"required={nbt_required}",
                f"runtime_only={nbt_runtime_only}",
            ],
            missing_or_weak_evidence=[
                "当前 required/missing 为 0，因此不是当前 P0 blocker。",
                f"{nbt_runtime_only} 个 runtime_only 状态是明确决策，不代表未来长期状态自动完成。",
                "任一新增长期神力、交易防刷、结构状态或实体状态都必须先升级目标表。",
            ],
            next_actions=[
                "保留 NBT 发布验收包和真实重启周期作为回归 gate。",
                "开发新长期状态时先改 NBT 目标表，再补 schema、round-trip、restart contract 和真实重启验证。",
            ],
            authoritative_evidence=[
                evidence.get("nbt_release_acceptance_report", ""),
                evidence.get("nbt_state_ownership_queue_report", ""),
                evidence.get("nbt_target_report", ""),
                evidence.get("nbt_restart_contract_report", ""),
            ],
        ),
        req(
            requirement_id="release_management",
            priority="P0",
            status="blocked" if release_blockers else "verified",
            user_requirement="1.0 大版本要有统一的发布口径、回归命令、玩家指南和退出标准。",
            current_evidence=[
                f"ready_for_1_0={gap.get('ready_for_1_0')}",
                f"release_blocking_gaps={release_blocker_ids}",
                f"release_hygiene_ok={summary.get('release_hygiene_ok')}",
                f"scope_matrix=docs/项目管理/MythicAgeMod_1.0模块范围与缺口矩阵_2026-06-15.md",
            ],
            missing_or_weak_evidence=[
                "release blocking gaps 未清零。",
                "玩家指南、CHANGELOG、发布说明需要在玩法/UI/美术最终稳定后再冻结。",
            ],
            next_actions=[
                "每完成一个波次后重跑 gap audit、scope matrix、requirement trace 和 Gradle build。",
                "只有 release blockers=0 且所有需求追踪项 done_for_1_0=true，才能进入 1.0 候选整理。",
            ],
            authoritative_evidence=[
                project_path(GAP_REPORT),
                "docs/项目管理/MythicAgeMod_1.0模块范围与缺口矩阵_2026-06-15.md",
            ],
        ),
    ]

    status_counts = Counter(item["status"] for item in requirements)
    open_requirements = [
        item["id"]
        for item in requirements
        if item["status"] not in PASSING_STATUSES
    ]
    blockers = [
        item["id"]
        for item in requirements
        if item["status"] == "blocked"
    ]
    ok = not open_requirements

    return {
        "version": 1,
        "scope": "mythic_age_1_0_user_requirement_trace",
        "source_gap_report": project_path(GAP_REPORT),
        "ok": ok,
        "ready_for_1_0": bool(gap.get("ready_for_1_0")) and ok,
        "rule": (
            "A 1.0 requirement is done only when current authoritative evidence "
            "proves the requested product surface is complete, not merely registered or scaffolded."
        ),
        "requirement_count": len(requirements),
        "status_counts": dict(sorted(status_counts.items())),
        "open_requirement_count": len(open_requirements),
        "blocked_requirement_count": len(blockers),
        "open_requirements": open_requirements,
        "blocked_requirements": blockers,
        "release_blocking_gaps": release_blocker_ids,
        "requirements": requirements,
        "development_waves": [
            "W0: Pegasus/Hydra V6 可建模规格审查，冻结设计先行规则。",
            "W1: 30 生物设计签收，Greek 视觉 vertical slice 作为质量样板。",
            "W2: 非希腊 20 生物美术升级和人工视觉验收。",
            "W3: 138 项动作连续帧/clip 补齐并做人工 motion 验收。",
            "W4: 52 神力玩法、粒子、音效和证书消耗验收。",
            "W5: UI、结构、世界生成、NBT 回归和玩家指南。",
            "W6: release blockers 清零后进入 1.0 候选整理。",
        ],
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 需求追踪与开发缺口（2026-06-15）",
        "",
        "本文档由 `tools/mythic_age_1_0_requirement_trace.py audit` 生成。它把用户原始 1.0 目标映射到当前证据、缺口和下一步，防止只看单个 gate 就误判完成。",
        "",
        "## 总览",
        "",
        f"- 需求追踪通过：`{payload['ok']}`",
        f"- 1.0 就绪：`{payload['ready_for_1_0']}`",
        f"- 需求数量：{payload['requirement_count']}",
        f"- 未完成需求：{payload['open_requirement_count']}",
        f"- 阻断需求：{payload['blocked_requirement_count']}",
        f"- 状态统计：`{json.dumps(payload['status_counts'], ensure_ascii=False)}`",
        f"- release blockers：`{json.dumps(payload['release_blocking_gaps'], ensure_ascii=False)}`",
        f"- 判定规则：{payload['rule']}",
        "",
        "## 需求矩阵",
        "",
        "| ID | 优先级 | 状态 | 用户目标 |",
        "| --- | --- | --- | --- |",
    ]
    for item in payload["requirements"]:
        lines.append(
            "| `{id}` | `{priority}` | `{status}` | {requirement} |".format(
                id=item["id"],
                priority=item["priority"],
                status=item["status"],
                requirement=item["user_requirement"].replace("|", "/"),
            )
        )

    lines.extend(["", "## 详细追踪", ""])
    for item in payload["requirements"]:
        lines.extend([
            f"### {item['id']}",
            "",
            f"- 优先级：`{item['priority']}`",
            f"- 状态：`{item['status']}`",
            f"- 是否满足 1.0：`{item['done_for_1_0']}`",
            f"- 用户目标：{item['user_requirement']}",
            "",
            "当前证据：",
        ])
        for value in item["current_evidence"]:
            lines.append(f"- {value}")
        lines.append("")
        lines.append("缺口或弱证据：")
        for value in item["missing_or_weak_evidence"]:
            lines.append(f"- {value}")
        lines.append("")
        lines.append("下一步：")
        for value in item["next_actions"]:
            lines.append(f"- {value}")
        lines.append("")
        lines.append("权威证据：")
        for value in item["authoritative_evidence"]:
            if value:
                lines.append(f"- `{value}`")
        lines.append("")

    lines.extend([
        "## 开发波次",
        "",
    ])
    for wave in payload["development_waves"]:
        lines.append(f"- {wave}")
    lines.extend([
        "",
        "## 退出口径",
        "",
        "- 本报告所有需求项必须为 `verified` 或 `verified_with_watchlist`。",
        "- `tools/mythic_age_1_0_gap_audit.py audit` 必须显示 `ready_for_1_0=true` 且 release blockers 为 0。",
        "- `tools/mythic_age_1_0_scope_matrix.py audit` 必须显示所有模块满足退出 gate。",
        "- `./gradlew build --offline` 必须通过。",
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
        print(f"MythicAgeMod 1.0 Requirement Trace: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"status_counts: {payload['status_counts']}")
        print(f"open_requirements: {payload['open_requirement_count']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
