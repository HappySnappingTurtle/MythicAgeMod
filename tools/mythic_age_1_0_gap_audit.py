#!/usr/bin/env python3
"""Aggregate current Mythic Age 1.0 readiness evidence and known gaps."""

from __future__ import annotations

import argparse
import html
import json
import os
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/1_0_gap_audit"
REPORT_PATH = REPORT_DIR / "gap_report.json"
DASHBOARD_PATH = REPORT_DIR / "release_acceptance_dashboard.html"
REQUIREMENT_TRACE_REPORT = ROOT / "docs/preview_assets/1_0_requirement_trace/mythic_age_1_0_requirement_trace.json"
REQUIREMENT_TRACE_MARKDOWN = ROOT / "docs/项目管理/MythicAgeMod_1.0需求追踪与开发缺口_2026-06-15.md"

CIVILIZATIONS = ["greek", "egyptian", "nordic", "chinese", "atlantean"]
CREATURE_MANIFESTS = {
    civ: ROOT / f"docs/美术资源/{civ}_creatures/visual_manifest.json"
    for civ in CIVILIZATIONS
}
VISUAL_REPORTS = {
    civ: ROOT / f"docs/preview_assets/{civ}_creatures/visual_report.json"
    for civ in CIVILIZATIONS
}
RUNTIME_REPORTS = {
    civ: ROOT / f"docs/preview_assets/{civ}_creatures/runtime_model_quality_report.json"
    for civ in CIVILIZATIONS
}

ENTITY_MANIFEST = ROOT / "docs/美术资源/entity_validation/entity_manifest.json"
NBT_REPORT = ROOT / "docs/preview_assets/entity_validation/nbt_schema_report.json"
NBT_TARGET_REPORT = ROOT / "docs/preview_assets/nbt_target_validation/nbt_target_report.json"
NBT_ROUNDTRIP_REPORT = ROOT / "docs/preview_assets/nbt_roundtrip_validation/nbt_roundtrip_report.json"
NBT_RESTART_CONTRACT_REPORT = ROOT / "docs/preview_assets/nbt_roundtrip_validation/nbt_restart_contract_report.json"
NBT_RELEASE_ACCEPTANCE_REPORT = ROOT / "docs/preview_assets/nbt_roundtrip_validation/nbt_release_acceptance_package.json"
NBT_STATE_OWNERSHIP_QUEUE_REPORT = ROOT / "docs/preview_assets/nbt_state_ownership/nbt_state_ownership_queue.json"
RUNTIME_NBT_REPORT = ROOT / "run/visual_probe/entity_nbt_report.json"
RUNTIME_NBT_RESTART_REPORT = ROOT / "run/visual_probe/nbt_restart_report.json"
RUNTIME_NBT_RESTART_PREPARE_REPORT = ROOT / "run/visual_probe/nbt_restart_prepare_report.json"
RUNTIME_NBT_RESTART_VERIFY_REPORT = ROOT / "run/visual_probe/nbt_restart_verify_report.json"
ACTION_PARTICLE_REPORT = ROOT / "docs/preview_assets/entity_validation/action_particle_report.json"
STRUCTURE_REPORT = ROOT / "docs/preview_assets/structure_validation/structure_report.json"
UI_REPORT = ROOT / "docs/preview_assets/ui_validation/ui_report.json"
PARTICLE_REPORT = ROOT / "docs/preview_assets/particle_validation/particle_report.json"
GOD_POWER_PARTICLE_REPORT = ROOT / "docs/preview_assets/particle_validation/god_power_particle_report.json"
GOD_POWER_GAMEPLAY_ACCEPTANCE_REPORT = ROOT / "docs/preview_assets/god_power_gameplay/god_power_gameplay_acceptance_package.json"
GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT = ROOT / "docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.json"
GOD_POWER_MANUAL_PLAYTEST_EXECUTION_MARKDOWN = ROOT / "docs/系统设计/MythicAgeMod_1.0_神力手工玩法测试执行队列_2026-06-15.md"
GOD_POWER_MANUAL_PLAYTEST_EXECUTION_CSV = ROOT / "docs/preview_assets/god_power_gameplay/manual_playtest_execution/god_power_manual_playtest_execution_queue.csv"
AUDIO_REPORT = ROOT / "docs/preview_assets/audio_validation/audio_report.json"
CREATURE_AUDIO_FINAL_MIX_REPORT = ROOT / "docs/preview_assets/audio_validation/creature_audio_final_mix_acceptance_package.json"
GREEK_SHAPE_ACCEPTANCE_REPORT = ROOT / "docs/preview_assets/greek_creatures/shape_acceptance_report.json"
CREATURE_VISUAL_ACCEPTANCE_REPORT = ROOT / "docs/preview_assets/creature_visual_acceptance/creature_visual_acceptance_package.json"
CREATURE_HUMAN_VISUAL_ACCEPTANCE_LEDGER = ROOT / "docs/preview_assets/creature_visual_acceptance/human_visual_acceptance_ledger.json"
CREATURE_RUNTIME_PROPORTION_QUALITY_REPORT = ROOT / "docs/preview_assets/creature_visual_acceptance/creature_runtime_proportion_quality_report.json"
CREATURE_DESIGN_FIRST_GATE_REPORT = ROOT / "docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json"
PEGASUS_HYDRA_DESIGN_CORRECTNESS_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_correctness/pegasus_hydra_design_correctness_gate.json"
PEGASUS_HYDRA_V6_MODELING_SPEC_REPORT = ROOT / "docs/preview_assets/greek_creatures/design_approval/v6/pegasus_hydra_v6_modeling_spec_package.json"
PEGASUS_HYDRA_V6_MODELING_SPEC_MARKDOWN = ROOT / "docs/美术资源/greek_creatures/design_review/pegasus_hydra_v6_modeling_spec_package_2026-06-15.md"
CREATURE_ART_REWORK_QUEUE_REPORT = ROOT / "docs/preview_assets/creature_art_rework_queue/creature_art_rework_queue.json"
CREATURE_ART_PRODUCTION_ROADMAP_REPORT = ROOT / "docs/preview_assets/creature_art_production_roadmap/creature_art_production_roadmap.json"
CREATURE_ART_PRODUCTION_ROADMAP_MARKDOWN = ROOT / "docs/项目管理/MythicAgeMod_1.0生物美术生产路线图_2026-06-15.md"
CREATURE_DESIGN_PRODUCTION_LOCK_REPORT = ROOT / "docs/preview_assets/creature_design_production_lock/creature_design_production_lock.json"
CREATURE_DESIGN_PRODUCTION_LOCK_MARKDOWN = ROOT / "docs/项目管理/MythicAgeMod_1.0生物设计生产锁_2026-06-15.md"
CREATURE_DESIGN_APPROVAL_REVIEW_REPORT = ROOT / "docs/preview_assets/creature_design_approval_review/creature_design_approval_review_package.json"
CREATURE_DESIGN_APPROVAL_REVIEW_MARKDOWN = ROOT / "docs/项目管理/MythicAgeMod_1.0生物设计审批总包_2026-06-15.md"
CREATURE_DESIGN_APPROVAL_REVIEW_CONTACT_SHEET = ROOT / "docs/preview_assets/creature_design_approval_review/creature_design_approval_contact_sheet.png"
CREATURE_ACTION_ACCEPTANCE_REPORT = ROOT / "docs/preview_assets/creature_action_acceptance/creature_action_acceptance_package.json"
CREATURE_MOTION_REWORK_QUEUE_REPORT = ROOT / "docs/preview_assets/creature_motion_rework_queue/creature_motion_rework_queue.json"
CREATURE_GAMEPLAY_CONTRACT_REPORT = ROOT / "docs/preview_assets/creature_gameplay_contract/creature_gameplay_contract.json"
CREATURE_GAMEPLAY_CONTRACT_MARKDOWN = ROOT / "docs/系统设计/MythicAgeMod_1.0生物玩法闭环契约_2026-06-15.md"
CERTIFICATE_ECONOMY_REPORT = ROOT / "docs/preview_assets/certificate_economy/certificate_economy_report.json"
SOUNDS_JSON = ROOT / "src/main/resources/assets/mythic_age/sounds.json"
FABRIC_MOD_JSON = ROOT / "src/main/resources/fabric.mod.json"
GRADLE_PROPERTIES = ROOT / "gradle.properties"
DOCS_README = ROOT / "docs/README.md"
HISTORICAL_STATUS_REPORT = ROOT / "docs/项目管理/PROJECT_STATUS_FINAL.md"
ONE_DOT_ZERO_OVERVIEW = ROOT / "docs/项目管理/MythicAgeMod_1.0当前缺口与开发总纲_2026-06-11.md"
CONTENT_MATRIX = ROOT / "docs/项目管理/MythicAgeMod_1.0五文明内容矩阵_2026-06-11.md"
ART_BIBLE = ROOT / "docs/美术资源/MythicAgeMod_1.0美术Bible_2026-06-11.md"
NBT_TARGETS = ROOT / "docs/系统设计/MythicAgeMod_1.0_NBT状态持久化目标_2026-06-11.md"
EXECUTION_ROADMAP = ROOT / "docs/项目管理/MythicAgeMod_1.0开发执行路线图_2026-06-12.md"
REBASELINE_OVERVIEW = ROOT / "docs/项目管理/MythicAgeMod_1.0重整开发总纲_2026-06-15.md"
TASK_LEDGER = ROOT / "docs/项目管理/MythicAgeMod_1.0开发任务台账_2026-06-15.md"
CERTIFICATE_ECONOMY_MATRIX = ROOT / "docs/系统设计/MythicAgeMod_1.0_五文明证书经济矩阵_2026-06-12.md"
M1_REFERENCE_DOCS = {
    "rebaseline_overview": REBASELINE_OVERVIEW,
    "task_ledger": TASK_LEDGER,
    "overview": ONE_DOT_ZERO_OVERVIEW,
    "content_matrix": CONTENT_MATRIX,
    "art_bible": ART_BIBLE,
    "nbt_targets": NBT_TARGETS,
    "execution_roadmap": EXECUTION_ROADMAP,
    "certificate_economy_matrix": CERTIFICATE_ECONOMY_MATRIX,
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def load_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def audit_reference_doc(path: Path, required_tokens: list[str]) -> dict[str, Any]:
    text = load_text(path)
    missing = [token for token in required_tokens if token not in text]
    return {
        "path": project_path(path),
        "exists": path.exists(),
        "missing_tokens": missing,
        "ok": path.exists() and not missing,
    }


def reference_doc_status() -> dict[str, Any]:
    required_tokens = {
        "rebaseline_overview": ["工程骨架", "设计拒绝", "设计审批包", "代码", "模型", "动作", "纹理", "UI", "美术", "粒子", "NBT"],
        "task_ledger": ["P0-ART-001", "P1-ACT-001", "P1-GP-001", "P1-AUD-001", "P1-UI-001", "P1-FX-001", "P1-NBT-001", "design_approval_status", "退出标准"],
        "overview": ["代码", "模型", "动作", "纹理", "UI", "美术", "粒子", "NBT"],
        "content_matrix": ["Greek", "Egyptian", "Nordic", "Chinese", "Atlantean", "音效", "NBT"],
        "art_bible": ["Greek", "Egyptian", "Nordic", "Chinese", "Atlantean", "粒子", "UI"],
        "nbt_targets": ["required", "runtime_only", "covered", "神力持续状态", "证书", "结构"],
        "execution_roadmap": ["代码", "模型", "动作", "纹理", "UI", "美术", "粒子", "NBT", "NOT READY"],
        "certificate_economy_matrix": ["Greek", "Egyptian", "Chinese", "Nordic", "Atlantean", "证书", "NBT", "守护者交易"],
    }
    docs = {
        key: audit_reference_doc(path, required_tokens[key])
        for key, path in M1_REFERENCE_DOCS.items()
    }
    return {
        "ok": all(doc["ok"] for doc in docs.values()),
        "docs": docs,
    }


def gradle_property_value(text: str, key: str) -> str | None:
    for line in text.splitlines():
        if line.strip().startswith("#") or "=" not in line:
            continue
        candidate_key, value = line.split("=", 1)
        if candidate_key.strip() == key:
            return value.strip()
    return None


def release_hygiene_status(fabric_mod: dict[str, Any]) -> dict[str, Any]:
    gradle_text = load_text(GRADLE_PROPERTIES)
    readme_text = load_text(DOCS_README)
    historical_text = load_text(HISTORICAL_STATUS_REPORT)
    fabric_version = fabric_mod.get("version")
    gradle_version = gradle_property_value(gradle_text, "mod_version")
    expected_jar = f"build/libs/mythic_age-{gradle_version}.jar" if gradle_version else None
    stale_tokens = ["95%完成状态", "接近发布就绪", "可以开始Beta版本发布", "代码实现、美术资源和文档工作已100%完成"]
    stale_hits = [
        token for token in stale_tokens
        if token in readme_text or token in historical_text
    ]
    checks = [
        {
            "id": "fabric_and_gradle_version_match",
            "ok": bool(fabric_version) and fabric_version == gradle_version,
            "evidence": f"fabric.mod.json={fabric_version}; gradle.properties={gradle_version}",
        },
        {
            "id": "readme_declares_development_snapshot",
            "ok": "`0.1.0` 开发快照" in readme_text and "不是 1.0 发布候选" in readme_text,
            "evidence": project_path(DOCS_README),
        },
        {
            "id": "readme_jar_matches_current_version",
            "ok": bool(expected_jar) and expected_jar in readme_text and "mythic_age-1.0.0.jar" not in readme_text,
            "evidence": expected_jar,
        },
        {
            "id": "historical_status_report_archived",
            "ok": "历史状态报告归档" in historical_text and "不能作为发布就绪依据" in historical_text,
            "evidence": project_path(HISTORICAL_STATUS_REPORT),
        },
        {
            "id": "no_stale_optimistic_release_claims",
            "ok": not stale_hits,
            "evidence": stale_hits,
        },
    ]
    return {
        "ok": all(check["ok"] for check in checks),
        "fabric_version": fabric_version,
        "gradle_mod_version": gradle_version,
        "expected_jar": expected_jar,
        "checks": checks,
    }


def nbt_target_status(nbt_target_report: dict[str, Any] | None = None) -> dict[str, Any]:
    if isinstance(nbt_target_report, dict) and nbt_target_report:
        status_counts = Counter(nbt_target_report.get("status_counts", {}))
        for status in ["required", "runtime_only", "covered", "missing", "candidate"]:
            status_counts.setdefault(status, 0)
        return {
            "path": project_path(NBT_TARGETS),
            "exists": NBT_TARGETS.exists(),
            "status_token_counts": status_counts,
            "required_sections": [],
            "missing_sections": [],
            "ok": bool(nbt_target_report.get("target_gate_ok")),
        }

    text = load_text(NBT_TARGETS)
    status_pattern = re.compile(r"\|\s*`(required|runtime_only|covered|missing|candidate)`\s*\|")
    statuses = Counter(status_pattern.findall(text))
    for status in ["required", "runtime_only", "covered", "missing", "candidate"]:
        statuses.setdefault(status, 0)
    required_sections = [
        "生物状态目标",
        "神力持续状态目标",
        "证书、交易和经济状态",
        "方块实体和结构状态",
        "验证扩展计划",
    ]
    missing_sections = [section for section in required_sections if section not in text]
    return {
        "path": project_path(NBT_TARGETS),
        "exists": NBT_TARGETS.exists(),
        "status_token_counts": statuses,
        "required_sections": required_sections,
        "missing_sections": missing_sections,
        "ok": NBT_TARGETS.exists() and not missing_sections and statuses["candidate"] == 0 and statuses["missing"] == 0 and statuses["covered"] > 0,
    }


def creature_ids_from_manifest(path: Path) -> list[str]:
    data = load_json(path, [])
    creatures = data if isinstance(data, list) else data.get("creatures", [])
    ids: list[str] = []
    for creature in creatures:
        if isinstance(creature, dict):
            value = creature.get("id") or creature.get("name")
            if isinstance(value, str):
                ids.append(value)
    return ids


def visual_status(path: Path) -> dict[str, Any]:
    data = load_json(path, {})
    creatures = data.get("creatures", []) if isinstance(data, dict) else []
    statuses = Counter(
        creature.get("status", "unknown")
        for creature in creatures
        if isinstance(creature, dict)
    )
    pending = sum(
        count for status, count in statuses.items()
        if "pending_human_shape_review" in status or "requires_human" in status
    )
    return {
        "path": project_path(path),
        "exists": path.exists(),
        "creature_count": len(creatures),
        "statuses": dict(sorted(statuses.items())),
        "pending_human_shape_review_count": pending,
    }


def runtime_status(path: Path) -> dict[str, Any]:
    data = load_json(path, {})
    creatures = data.get("creatures", []) if isinstance(data, dict) else []
    failures = data.get("failures", []) if isinstance(data, dict) else []
    return {
        "path": project_path(path),
        "exists": path.exists(),
        "ok": bool(data.get("ok")) if isinstance(data, dict) else False,
        "scope": data.get("scope") if isinstance(data, dict) else None,
        "creature_count": len(creatures),
        "failure_count": len(failures),
    }


def runtime_nbt_report_status(path: Path) -> dict[str, Any]:
    data = load_json(path, {})
    failed = data.get("failed") if isinstance(data, dict) else None
    passed = data.get("passed") if isinstance(data, dict) else None
    total = data.get("total") if isinstance(data, dict) else None
    return {
        "path": project_path(path),
        "exists": path.exists(),
        "scope": data.get("scope") if isinstance(data, dict) else None,
        "phase": data.get("phase") if isinstance(data, dict) else None,
        "total": total,
        "passed": passed,
        "failed": failed,
        "ok": path.exists() and failed == 0 and total is not None and passed == total,
    }


def sound_coverage(creature_ids: list[str]) -> dict[str, Any]:
    sounds = load_json(SOUNDS_JSON, {})
    keys = sorted(sounds.keys()) if isinstance(sounds, dict) else []
    by_entity: dict[str, list[str]] = defaultdict(list)
    for key in keys:
        if not key.startswith("entity."):
            continue
        parts = key.split(".")
        if len(parts) >= 3:
            by_entity[parts[1]].append(parts[2])

    required = ["ambient", "hurt", "death", "attack", "special"]
    coverage = []
    missing_any = []
    for creature_id in creature_ids:
        events = sorted(set(by_entity.get(creature_id, [])))
        missing = [event for event in required if event not in events]
        if creature_id == "pegasus" and "attack" in missing:
            missing.remove("attack")
        if creature_id == "minotaur" and "special" in missing and "charge" in events:
            missing.remove("special")
        coverage.append({
            "id": creature_id,
            "events": events,
            "missing_recommended_events": missing,
            "ok_for_1_0": not missing,
        })
        if missing:
            missing_any.append(creature_id)

    return {
        "path": project_path(SOUNDS_JSON),
        "registered_entity_sound_keys": len(keys),
        "creature_sound_coverage": coverage,
        "creatures_missing_recommended_sounds": missing_any,
    }


def build_report() -> dict[str, Any]:
    entity_manifest = load_json(ENTITY_MANIFEST, {})
    nbt_report = load_json(NBT_REPORT, {})
    nbt_target_report = load_json(NBT_TARGET_REPORT, {})
    nbt_roundtrip_report = load_json(NBT_ROUNDTRIP_REPORT, {})
    nbt_restart_contract_report = load_json(NBT_RESTART_CONTRACT_REPORT, {})
    nbt_release_acceptance = load_json(NBT_RELEASE_ACCEPTANCE_REPORT, {})
    nbt_state_ownership_queue = load_json(NBT_STATE_OWNERSHIP_QUEUE_REPORT, {})
    runtime_nbt_report = runtime_nbt_report_status(RUNTIME_NBT_REPORT)
    runtime_nbt_restart_report = runtime_nbt_report_status(RUNTIME_NBT_RESTART_REPORT)
    runtime_nbt_restart_prepare_report = runtime_nbt_report_status(RUNTIME_NBT_RESTART_PREPARE_REPORT)
    runtime_nbt_restart_verify_report = runtime_nbt_report_status(RUNTIME_NBT_RESTART_VERIFY_REPORT)
    action_report = load_json(ACTION_PARTICLE_REPORT, {})
    structure_report = load_json(STRUCTURE_REPORT, {})
    ui_report = load_json(UI_REPORT, {})
    particle_report = load_json(PARTICLE_REPORT, {})
    god_particle_report = load_json(GOD_POWER_PARTICLE_REPORT, {})
    god_power_gameplay_acceptance = load_json(GOD_POWER_GAMEPLAY_ACCEPTANCE_REPORT, {})
    god_power_manual_playtest_execution = load_json(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT, {})
    audio_report = load_json(AUDIO_REPORT, {})
    creature_audio_final_mix = load_json(CREATURE_AUDIO_FINAL_MIX_REPORT, {})
    greek_shape_report = load_json(GREEK_SHAPE_ACCEPTANCE_REPORT, {})
    creature_visual_acceptance = load_json(CREATURE_VISUAL_ACCEPTANCE_REPORT, {})
    creature_human_visual_ledger = load_json(CREATURE_HUMAN_VISUAL_ACCEPTANCE_LEDGER, {})
    creature_runtime_proportion_quality = load_json(CREATURE_RUNTIME_PROPORTION_QUALITY_REPORT, {})
    creature_design_first_gate = load_json(CREATURE_DESIGN_FIRST_GATE_REPORT, {})
    pegasus_hydra_design_correctness = load_json(PEGASUS_HYDRA_DESIGN_CORRECTNESS_REPORT, {})
    pegasus_hydra_v6_modeling_spec = load_json(PEGASUS_HYDRA_V6_MODELING_SPEC_REPORT, {})
    creature_art_rework_queue = load_json(CREATURE_ART_REWORK_QUEUE_REPORT, {})
    creature_art_production_roadmap = load_json(CREATURE_ART_PRODUCTION_ROADMAP_REPORT, {})
    creature_design_production_lock = load_json(CREATURE_DESIGN_PRODUCTION_LOCK_REPORT, {})
    creature_design_approval_review = load_json(CREATURE_DESIGN_APPROVAL_REVIEW_REPORT, {})
    creature_action_acceptance = load_json(CREATURE_ACTION_ACCEPTANCE_REPORT, {})
    creature_motion_rework_queue = load_json(CREATURE_MOTION_REWORK_QUEUE_REPORT, {})
    creature_gameplay_contract = load_json(CREATURE_GAMEPLAY_CONTRACT_REPORT, {})
    certificate_economy = load_json(CERTIFICATE_ECONOMY_REPORT, {})
    requirement_trace = load_json(REQUIREMENT_TRACE_REPORT, {})
    fabric_mod = load_json(FABRIC_MOD_JSON, {})

    creature_roster = {
        civ: creature_ids_from_manifest(path)
        for civ, path in CREATURE_MANIFESTS.items()
    }
    all_civilization_creatures = [
        creature_id
        for ids in creature_roster.values()
        for creature_id in ids
    ]

    visual = {civ: visual_status(path) for civ, path in VISUAL_REPORTS.items()}
    runtime = {civ: runtime_status(path) for civ, path in RUNTIME_REPORTS.items()}
    sounds = sound_coverage(all_civilization_creatures + ["walking_tree"])

    entity_entries = entity_manifest.get("entities", []) if isinstance(entity_manifest, dict) else []
    entity_nbt_modes = Counter(
        entry.get("nbt", "unknown")
        for entry in entity_entries
        if isinstance(entry, dict)
    )
    nbt_schemas = nbt_report.get("schemas", []) if isinstance(nbt_report, dict) else []
    nbt_schema_owners = [
        schema.get("owner")
        for schema in nbt_schemas
        if isinstance(schema, dict)
    ]

    structure_summary = structure_report.get("summary", {}) if isinstance(structure_report, dict) else {}
    structure_warnings = structure_report.get("warnings", []) if isinstance(structure_report, dict) else []
    god_power_known_gaps = god_particle_report.get("known_gaps", []) if isinstance(god_particle_report, dict) else []
    god_power_full_matrix_ok = bool(god_particle_report.get("full_matrix_ok")) if isinstance(god_particle_report, dict) else False
    registered_spellbooks = god_particle_report.get("registered_spellbooks", {}) if isinstance(god_particle_report, dict) else {}
    atlantean_resources = god_particle_report.get("atlantean_spellbook_resources", []) if isinstance(god_particle_report, dict) else []
    atlantean_resource_ok = bool(atlantean_resources) and all(
        isinstance(item, dict) and item.get("ok")
        for item in atlantean_resources
    )
    references = reference_doc_status()
    release_hygiene = release_hygiene_status(fabric_mod)
    nbt_targets = nbt_target_status(nbt_target_report)
    ui_scope = ui_report.get("scope")
    ui_ok = ui_report.get("ok")
    ui_guide_scope_ok = ui_ok and ui_scope == "static_guardian_trade_and_god_power_guide_ui"
    structure_quality_ok = (
        structure_report.get("ok")
        and not structure_warnings
        and structure_summary.get("canonical_valid_count") == structure_summary.get("canonical_expected_count")
        and structure_summary.get("functional_point_checked_count") == structure_summary.get("functional_point_expected_count")
        and structure_summary.get("theme_contract_checked_count") == structure_summary.get("theme_contract_expected_count")
    )
    nbt_real_restart_cycle_ok = bool(runtime_nbt_restart_verify_report["ok"])
    visual_ledger_exists = CREATURE_HUMAN_VISUAL_ACCEPTANCE_LEDGER.exists()
    visual_acceptance_ok = bool(creature_human_visual_ledger.get("human_visual_acceptance_ok")) if visual_ledger_exists else bool(creature_visual_acceptance.get("human_visual_acceptance_ok"))
    visual_manual_review_required = creature_human_visual_ledger.get("manual_review_required_count") if visual_ledger_exists else creature_visual_acceptance.get("manual_review_required_count")
    visual_ledger_decision_counts = creature_human_visual_ledger.get("decision_counts", {}) if isinstance(creature_human_visual_ledger, dict) else {}
    visual_ledger_blocked_by_design = creature_human_visual_ledger.get("blocked_by_design_rejection_count") if isinstance(creature_human_visual_ledger, dict) else None
    visual_ledger_pending_shape = creature_human_visual_ledger.get("pending_shape_review_count") if isinstance(creature_human_visual_ledger, dict) else None
    visual_ledger_pending_user_acceptance = creature_human_visual_ledger.get("pending_user_acceptance_count") if isinstance(creature_human_visual_ledger, dict) else None
    visual_ledger_approved = creature_human_visual_ledger.get("approved_count") if isinstance(creature_human_visual_ledger, dict) else None
    visual_ledger_checklist_pending = creature_human_visual_ledger.get("checklist_pending_count") if isinstance(creature_human_visual_ledger, dict) else None
    visual_package_ok = bool(creature_visual_acceptance.get("ok"))
    visual_quality_contract_ok = bool(creature_visual_acceptance.get("visual_quality_contract_ok"))
    visual_quality_contract_fail_count = creature_visual_acceptance.get("visual_quality_contract_fail_count")
    visual_design_rejection_count = creature_visual_acceptance.get("design_rejection_blocker_count")
    visual_design_rejection_blockers = creature_visual_acceptance.get("design_rejection_blockers", [])
    visual_quality_contract_blockers = creature_visual_acceptance.get("visual_quality_contract_blockers", [])
    visual_quality_status_counts = creature_visual_acceptance.get("quality_status_counts")
    visual_revision_pending_user_acceptance = creature_visual_acceptance.get("revision_pending_user_acceptance_count")
    visual_pending_human_shape_review = creature_visual_acceptance.get("pending_human_shape_review_count")
    visual_review_board_count = creature_visual_acceptance.get("visual_review_board_count")
    visual_review_board_contact_sheet = creature_visual_acceptance.get("visual_review_board_contact_sheet")
    visual_runtime_proportion_ok = bool(creature_visual_acceptance.get("runtime_proportion_quality_gate_ok"))
    visual_runtime_proportion_blocker_count = creature_visual_acceptance.get("runtime_proportion_quality_blocker_count")
    visual_proportion_detail_first_ok = bool(creature_visual_acceptance.get("proportion_detail_first_readability_ok"))
    visual_proportion_detail_first_count = creature_visual_acceptance.get("proportion_detail_first_readability_count")
    visual_runtime_detail_first_ok = bool(creature_visual_acceptance.get("runtime_detail_first_readability_ok"))
    visual_runtime_detail_first_count = creature_visual_acceptance.get("runtime_detail_first_readability_count")
    creature_design_first_gate_ok = bool(creature_design_first_gate.get("ok"))
    creature_design_first_approved_count = creature_design_first_gate.get("approved_count")
    creature_design_first_creature_count = creature_design_first_gate.get("creature_count")
    creature_design_first_status_counts = creature_design_first_gate.get("status_counts", {})
    creature_design_first_manual_required_count = creature_design_first_gate.get("manual_design_approval_required_count")
    creature_design_first_blocked_count = creature_design_first_gate.get("blocked_count")
    pegasus_hydra_design_correctness_ok = bool(pegasus_hydra_design_correctness.get("ok"))
    pegasus_hydra_design_correctness_verdict = pegasus_hydra_design_correctness.get("verdict")
    pegasus_hydra_design_correctness_blocked = pegasus_hydra_design_correctness.get("blocked", [])
    pegasus_hydra_design_correctness_blocked_count = pegasus_hydra_design_correctness.get("blocked_count")
    pegasus_hydra_v6_modeling_spec_status = pegasus_hydra_v6_modeling_spec.get("status")
    pegasus_hydra_v6_modeling_spec_verdict = pegasus_hydra_v6_modeling_spec.get("verdict")
    pegasus_hydra_v6_modeling_spec_blocked_count = pegasus_hydra_v6_modeling_spec.get("blocked_count")
    pegasus_hydra_v6_modeling_allowed = pegasus_hydra_v6_modeling_spec.get("modeling_allowed")
    creature_art_rework_queue_ok = bool(creature_art_rework_queue.get("ok"))
    creature_art_rework_queue_unresolved_count = creature_art_rework_queue.get("unresolved_count")
    creature_art_rework_queue_priority_counts = creature_art_rework_queue.get("priority_counts", {})
    creature_art_rework_queue_non_greek_upgrade_count = creature_art_rework_queue.get("non_greek_upgrade_count")
    creature_art_production_roadmap_ok = bool(creature_art_production_roadmap.get("ok"))
    creature_art_production_batch_counts = creature_art_production_roadmap.get("batch_counts", {})
    creature_art_production_step_status_counts = creature_art_production_roadmap.get("step_status_counts", {})
    creature_art_production_blocked_count = creature_art_production_roadmap.get("blocked_creature_count")
    creature_art_production_design_blocker_count = creature_art_production_roadmap.get("design_blocker_count")
    creature_design_production_lock_ok = bool(creature_design_production_lock.get("ok"))
    creature_design_production_lock_status = creature_design_production_lock.get("status")
    creature_design_production_locked_count = creature_design_production_lock.get("locked_count")
    creature_design_production_unlocked_count = creature_design_production_lock.get("unlocked_count")
    creature_design_production_hard_lock_count = creature_design_production_lock.get("hard_lock_rejected_design_count")
    creature_design_production_approval_lock_count = creature_design_production_lock.get("approval_lock_manual_review_required_count")
    creature_design_production_step_lock_counts = creature_design_production_lock.get("step_lock_counts", {})
    creature_design_approval_review_status = creature_design_approval_review.get("status")
    creature_design_approval_review_pending_count = creature_design_approval_review.get("pending_review_count")
    creature_design_approval_review_batch_counts = creature_design_approval_review.get("batch_counts", {})
    creature_design_approval_review_missing_asset_count = creature_design_approval_review.get("missing_asset_count")
    action_acceptance_ok = bool(creature_action_acceptance.get("ok"))
    action_manual_review_required = creature_action_acceptance.get("manual_review_required_count")
    action_human_motion_acceptance_ok = bool(creature_action_acceptance.get("human_motion_acceptance_ok"))
    action_storyboard_count = creature_action_acceptance.get("motion_storyboard_count")
    action_storyboard_contact_sheet = creature_action_acceptance.get("motion_storyboard_contact_sheet")
    action_review_board_count = creature_action_acceptance.get("motion_review_board_count")
    action_review_board_contact_sheet = creature_action_acceptance.get("motion_review_board_contact_sheet")
    action_motion_sequence_ok = bool(creature_action_acceptance.get("motion_sequence_evidence_ok"))
    action_motion_sequence_ok_count = creature_action_acceptance.get("motion_sequence_evidence_ok_count")
    action_motion_sequence_full_mode_ok = bool(creature_action_acceptance.get("motion_sequence_full_mode_coverage_ok"))
    action_motion_sequence_full_mode_ok_count = creature_action_acceptance.get("motion_sequence_full_mode_coverage_ok_count")
    action_motion_sequence_required_mode_count = creature_action_acceptance.get("motion_sequence_required_mode_count")
    action_motion_sequence_captured_required_mode_count = creature_action_acceptance.get("motion_sequence_captured_required_mode_count")
    action_motion_sequence_missing_mode_count = creature_action_acceptance.get("motion_sequence_missing_mode_count")
    action_motion_sequence_frame_count = creature_action_acceptance.get("motion_sequence_frame_count")
    action_motion_sequence_manifest = creature_action_acceptance.get("motion_sequence_manifest")
    action_motion_capture_queue_count = creature_action_acceptance.get("motion_capture_queue_count")
    action_motion_capture_queue_by_mode = creature_action_acceptance.get("motion_capture_queue_by_mode", {})
    action_motion_capture_queue_by_civilization = creature_action_acceptance.get("motion_capture_queue_by_civilization", {})
    action_motion_capture_queue_by_priority = creature_action_acceptance.get("motion_capture_queue_by_priority", {})
    action_motion_capture_total_minimum_frames = creature_action_acceptance.get("motion_capture_queue_total_minimum_frames")
    action_motion_capture_plan_batch_count = creature_action_acceptance.get("motion_capture_plan_batch_count")
    creature_motion_rework_queue_ok = bool(creature_motion_rework_queue.get("ok"))
    creature_motion_rework_queued_task_count = creature_motion_rework_queue.get("queued_task_count")
    creature_motion_rework_blocked_task_count = creature_motion_rework_queue.get("blocked_task_count")
    creature_motion_rework_blocked_creatures = creature_motion_rework_queue.get("blocked_creatures", [])
    creature_motion_rework_task_state_counts = creature_motion_rework_queue.get("task_state_counts", {})
    creature_motion_rework_art_priority_counts = creature_motion_rework_queue.get("task_art_priority_counts", {})
    creature_motion_rework_minimum_frame_budget = creature_motion_rework_queue.get("minimum_frame_budget")
    creature_gameplay_contract_ok = bool(creature_gameplay_contract.get("ok"))
    creature_gameplay_static_contract_ok = bool(creature_gameplay_contract.get("static_code_contract_ok"))
    creature_gameplay_static_ok_count = creature_gameplay_contract.get("static_code_contract_ok_count")
    creature_gameplay_release_ready_count = creature_gameplay_contract.get("release_gameplay_ready_count")
    creature_gameplay_creature_count = creature_gameplay_contract.get("creature_count")
    creature_gameplay_static_failure_counts = creature_gameplay_contract.get("static_failure_counts", {})
    creature_gameplay_release_blocker_counts = creature_gameplay_contract.get("release_blocker_counts", {})
    god_power_gameplay_package_ok = bool(god_power_gameplay_acceptance.get("ok"))
    god_power_gameplay_acceptance_ok = bool(god_power_gameplay_acceptance.get("gameplay_acceptance_ok"))
    god_power_manual_playtest_required = god_power_gameplay_acceptance.get("manual_playtest_required_count")
    god_power_review_board_count = god_power_gameplay_acceptance.get("gameplay_review_board_count")
    god_power_review_board_contact_sheet = god_power_gameplay_acceptance.get("gameplay_review_board_contact_sheet")
    god_power_runtime_smoke = god_power_gameplay_acceptance.get("runtime_smoke_playtest", {})
    god_power_runtime_smoke_ok = bool(god_power_gameplay_acceptance.get("runtime_smoke_playtest_ok"))
    god_power_runtime_smoke_probe_passed = god_power_gameplay_acceptance.get("runtime_smoke_probe_passed_power_count")
    god_power_runtime_smoke_probe_total = god_power_gameplay_acceptance.get("runtime_smoke_probe_power_count")
    god_power_playtest_risk_counts = god_power_gameplay_acceptance.get("playtest_risk_level_counts", {})
    god_power_high_risk_playtest_count = god_power_gameplay_acceptance.get("high_risk_playtest_count")
    god_power_dimension_status_counts = god_power_gameplay_acceptance.get("dimension_status_counts", {})
    god_power_manual_queue_count = god_power_gameplay_acceptance.get("manual_playtest_queue_count")
    god_power_manual_batch_count = god_power_gameplay_acceptance.get("manual_playtest_batch_count")
    god_power_manual_minimum_scenarios = god_power_gameplay_acceptance.get("manual_playtest_queue_total_minimum_scenarios")
    god_power_manual_queue_by_priority = god_power_gameplay_acceptance.get("manual_playtest_queue_by_priority", {})
    god_power_manual_queue_by_risk = god_power_gameplay_acceptance.get("manual_playtest_queue_by_risk_level", {})
    god_power_manual_execution_status = god_power_manual_playtest_execution.get("status")
    god_power_manual_execution_batch_count = god_power_manual_playtest_execution.get("batch_count")
    god_power_manual_execution_scenario_total = god_power_manual_playtest_execution.get("scenario_template_total")
    god_power_manual_execution_world_edit_count = god_power_manual_playtest_execution.get("world_edit_task_count")
    god_power_manual_execution_risk_counts = god_power_manual_playtest_execution.get("risk_counts", {})
    nbt_release_review_board = nbt_release_acceptance.get("review_board")
    nbt_state_ownership_ok = bool(nbt_state_ownership_queue.get("ok"))
    nbt_state_ownership_status_counts = nbt_state_ownership_queue.get("status_counts", {})
    nbt_state_ownership_priority_counts = nbt_state_ownership_queue.get("priority_counts", {})
    nbt_state_ownership_covered_count = nbt_state_ownership_queue.get("covered_count")
    nbt_state_ownership_runtime_only_count = nbt_state_ownership_queue.get("runtime_only_count")
    audio_review_board_count = creature_audio_final_mix.get("final_mix_review_board_count")
    audio_review_board_contact_sheet = creature_audio_final_mix.get("final_mix_review_board_contact_sheet")
    audio_audition_index = creature_audio_final_mix.get("audition_index")
    audio_audition_event_count = creature_audio_final_mix.get("audition_event_count")
    audio_tooling_status = creature_audio_final_mix.get("audio_tooling_status", {})
    if not isinstance(audio_tooling_status, dict):
        audio_tooling_status = {}
    audio_tooling_missing = audio_tooling_status.get("missing_tools", [])
    audio_loudness_metrics_available = bool(audio_tooling_status.get("loudness_metrics_available"))
    audio_final_mix_tooling_blocker = bool(creature_audio_final_mix.get("final_mix_tooling_blocker"))
    audio_ogg_container_metrics_ok = bool(creature_audio_final_mix.get("ogg_container_metrics_ok"))
    audio_ogg_container_metrics_event_count = creature_audio_final_mix.get("ogg_container_metrics_event_count")
    audio_ogg_container_metrics_failure_count = creature_audio_final_mix.get("ogg_container_metrics_failure_count")
    audio_manual_listening_queue_count = creature_audio_final_mix.get("manual_listening_queue_count")
    audio_manual_listening_batch_count = creature_audio_final_mix.get("manual_listening_batch_count")
    audio_manual_listening_total_duration_seconds = creature_audio_final_mix.get("manual_listening_total_duration_seconds")
    audio_manual_listening_queue_by_priority = creature_audio_final_mix.get("manual_listening_queue_by_priority", {})
    audio_manual_listening_queue_by_event = creature_audio_final_mix.get("manual_listening_queue_by_event", {})
    audio_candidate_listening_queue_count = creature_audio_final_mix.get("candidate_listening_queue_count")
    audio_replacement_listening_queue_count = creature_audio_final_mix.get("replacement_listening_queue_count")
    audio_candidate_listening_queue_by_event = creature_audio_final_mix.get("candidate_listening_queue_by_event", {})
    certificate_economy_ok = bool(certificate_economy.get("ok"))
    certificate_economy_summary = certificate_economy.get("summary", {}) if isinstance(certificate_economy, dict) else {}
    certificate_missing_guardian_trade_count = certificate_economy_summary.get("missing_custom_guardian_trade_count")
    certificate_price_mismatch_count = certificate_economy_summary.get("price_mismatch_count")
    certificate_guide_spell_total = certificate_economy_summary.get("guide_spell_total")
    certificate_custom_guardian_spell_trade_total = certificate_economy_summary.get("custom_guardian_spell_trade_total")
    certificate_legacy_statue_block_greek_only = certificate_economy_summary.get("legacy_statue_block_greek_only")
    requirement_trace_ok = bool(requirement_trace.get("ok"))
    requirement_trace_ready = bool(requirement_trace.get("ready_for_1_0"))
    requirement_trace_status_counts = requirement_trace.get("status_counts", {})
    requirement_trace_open_count = requirement_trace.get("open_requirement_count")
    requirement_trace_blocked_count = requirement_trace.get("blocked_requirement_count")
    requirement_trace_open_requirements = requirement_trace.get("open_requirements", [])
    requirement_trace_blocked_requirements = requirement_trace.get("blocked_requirements", [])

    release_blocking_gaps = [
        {
            "id": "human_visual_acceptance_not_done",
            "severity": "P0",
            "module": "models_textures_art",
            "evidence": (
                f"full visual acceptance package ok={visual_package_ok}; "
                f"visual quality contract ok={visual_quality_contract_ok}; "
                f"visual quality contract failures={visual_quality_contract_fail_count}; "
                f"design rejection blockers={visual_design_rejection_count}; "
                f"design rejection blocker list={visual_design_rejection_blockers}; "
                f"visual quality contract blockers={visual_quality_contract_blockers}; "
                f"runtime Java head/body proportion ok={visual_runtime_proportion_ok}; "
                f"runtime Java head/body proportion blockers={visual_runtime_proportion_blocker_count}; "
                f"design/Blockbench detail-first shape ok={visual_proportion_detail_first_ok} "
                f"({visual_proportion_detail_first_count}/{creature_visual_acceptance.get('creature_count')}); "
                f"runtime Java detail-first shape ok={visual_runtime_detail_first_ok} "
                f"({visual_runtime_detail_first_count}/{creature_visual_acceptance.get('creature_count')}); "
                f"full creature design-first gate ok={creature_design_first_gate_ok}; "
                f"full creature design approvals={creature_design_first_approved_count}/{creature_design_first_creature_count}; "
                f"full creature design status counts={creature_design_first_status_counts}; "
                f"full creature manual design approvals required={creature_design_first_manual_required_count}; "
                f"full creature design blockers={creature_design_first_blocked_count}; "
                f"full creature design-first report={project_path(CREATURE_DESIGN_FIRST_GATE_REPORT)}; "
                f"Pegasus/Hydra design correctness gate ok={pegasus_hydra_design_correctness_ok}; "
                f"Pegasus/Hydra design correctness verdict={pegasus_hydra_design_correctness_verdict}; "
                f"Pegasus/Hydra design correctness blocked={pegasus_hydra_design_correctness_blocked}; "
                f"Pegasus/Hydra design correctness report={project_path(PEGASUS_HYDRA_DESIGN_CORRECTNESS_REPORT)}; "
                f"Pegasus/Hydra V6 modeling spec status={pegasus_hydra_v6_modeling_spec_status}; "
                f"Pegasus/Hydra V6 modeling spec verdict={pegasus_hydra_v6_modeling_spec_verdict}; "
                f"Pegasus/Hydra V6 modeling allowed={pegasus_hydra_v6_modeling_allowed}; "
                f"Pegasus/Hydra V6 modeling spec blocked={pegasus_hydra_v6_modeling_spec_blocked_count}; "
                f"Pegasus/Hydra V6 modeling spec report={project_path(PEGASUS_HYDRA_V6_MODELING_SPEC_REPORT)}; "
                f"Pegasus/Hydra V6 modeling spec markdown={project_path(PEGASUS_HYDRA_V6_MODELING_SPEC_MARKDOWN)}; "
                f"creature art rework queue ok={creature_art_rework_queue_ok}; "
                f"creature art rework unresolved={creature_art_rework_queue_unresolved_count}; "
                f"creature art rework priority counts={creature_art_rework_queue_priority_counts}; "
                f"non-Greek art upgrade count={creature_art_rework_queue_non_greek_upgrade_count}; "
                f"art production roadmap ok={creature_art_production_roadmap_ok}; "
                f"art production batches={creature_art_production_batch_counts}; "
                f"art production blocked creatures={creature_art_production_blocked_count}; "
                f"art production design blockers={creature_art_production_design_blocker_count}; "
                f"design production lock ok={creature_design_production_lock_ok}; "
                f"design production lock status={creature_design_production_lock_status}; "
                f"design production locked={creature_design_production_locked_count}; "
                f"design production unlocked={creature_design_production_unlocked_count}; "
                f"design production hard locks={creature_design_production_hard_lock_count}; "
                f"design production approval locks={creature_design_production_approval_lock_count}; "
                f"design production step locks={creature_design_production_step_lock_counts}; "
                f"design production lock report={project_path(CREATURE_DESIGN_PRODUCTION_LOCK_REPORT)}; "
                f"design approval review package status={creature_design_approval_review_status}; "
                f"design approval review pending={creature_design_approval_review_pending_count}; "
                f"design approval review batches={creature_design_approval_review_batch_counts}; "
                f"design approval review missing assets={creature_design_approval_review_missing_asset_count}; "
                f"design approval review package={project_path(CREATURE_DESIGN_APPROVAL_REVIEW_REPORT)}; "
                f"quality status counts={visual_quality_status_counts}; "
                f"creatures in visual package={creature_visual_acceptance.get('creature_count')}; "
                f"manual review required={visual_manual_review_required}; "
                f"human visual ledger exists={visual_ledger_exists}; "
                f"human visual ledger decisions={visual_ledger_decision_counts}; "
                f"human visual ledger approved={visual_ledger_approved}; "
                f"human visual ledger blocked by design={visual_ledger_blocked_by_design}; "
                f"human visual ledger pending shape={visual_ledger_pending_shape}; "
                f"human visual ledger pending user acceptance={visual_ledger_pending_user_acceptance}; "
                f"human visual ledger checklist pending={visual_ledger_checklist_pending}; "
                f"visual review boards={visual_review_board_count}; "
                f"review board contact sheet={visual_review_board_contact_sheet}; "
                f"human visual acceptance ok={visual_acceptance_ok}; "
                f"Greek shape mechanical gate ok={greek_shape_report.get('mechanical_gate_ok')}; "
                f"shape acceptance ok={greek_shape_report.get('shape_acceptance_ok')}; "
                f"Greek shape revisions required={greek_shape_report.get('shape_revision_required')}; "
                f"Greek revisions pending user acceptance={greek_shape_report.get('revision_pending_user_acceptance_count')}; "
                f"package revisions pending user acceptance={visual_revision_pending_user_acceptance}; "
                f"package pending human shape review={visual_pending_human_shape_review}."
            ),
            "required_1_0_state": "30 个生物不仅要有设计包、概念图、Blockbench 三视图、UV、贴图贴合和 runtime model gate，还必须有正式设计先行签收。设计拒绝项必须先签收 Pegasus/Hydra V6 可建模规格，并让 Pegasus/Hydra 设计正确性 gate 通过。其余生成型设计包也需要人工批准为 approved_for_modeling 后，模型、纹理、动作和 runtime 证据才可作为 1.0 正式验收依据。",
        },
        {
            "id": "creature_motion_acceptance_not_done",
            "severity": "P1",
            "module": "actions",
            "evidence": (
                f"creature action package ok={action_acceptance_ok}; "
                f"creatures in action package={creature_action_acceptance.get('creature_count')}; "
                f"manual review required={action_manual_review_required}; "
                f"motion storyboards={action_storyboard_count}; "
                f"storyboard contact sheet={action_storyboard_contact_sheet}; "
                f"motion review boards={action_review_board_count}; "
                f"review board contact sheet={action_review_board_contact_sheet}; "
                f"MC motion sequence evidence ok={action_motion_sequence_ok}; "
                f"MC motion sequence coverage={action_motion_sequence_ok_count}/{creature_action_acceptance.get('creature_count')}; "
                f"MC full-mode sequence ok={action_motion_sequence_full_mode_ok}; "
                f"MC full-mode coverage={action_motion_sequence_full_mode_ok_count}/{creature_action_acceptance.get('creature_count')}; "
                f"MC required motion modes covered={action_motion_sequence_captured_required_mode_count}/{action_motion_sequence_required_mode_count}; "
                f"MC missing motion mode items={action_motion_sequence_missing_mode_count}; "
                f"MC motion capture queue items={action_motion_capture_queue_count}; "
                f"MC motion capture queue by mode={action_motion_capture_queue_by_mode}; "
                f"MC motion capture queue by priority={action_motion_capture_queue_by_priority}; "
                f"MC motion capture plan batches={action_motion_capture_plan_batch_count}; "
                f"MC motion capture minimum frame budget={action_motion_capture_total_minimum_frames}; "
                f"creature motion rework queue ok={creature_motion_rework_queue_ok}; "
                f"creature motion rework queued tasks={creature_motion_rework_queued_task_count}; "
                f"creature motion rework blocked tasks={creature_motion_rework_blocked_task_count}; "
                f"creature motion rework blocked creatures={creature_motion_rework_blocked_creatures}; "
                f"creature motion rework task states={creature_motion_rework_task_state_counts}; "
                f"creature motion rework art priorities={creature_motion_rework_art_priority_counts}; "
                f"creature motion rework minimum frame budget={creature_motion_rework_minimum_frame_budget}; "
                f"creature motion rework report={project_path(CREATURE_MOTION_REWORK_QUEUE_REPORT)}; "
                f"MC motion sequence frames={action_motion_sequence_frame_count}; "
                f"MC motion sequence manifest={action_motion_sequence_manifest}; "
                f"human motion acceptance ok={action_human_motion_acceptance_ok}; "
                f"status_counts={creature_action_acceptance.get('status_counts')}."
            ),
            "required_1_0_state": "30 个生物的 idle、移动、攻击/特殊、受击/死亡、飞行/滑游/骑乘和多部件联动已有源码证据、静态 storyboard 和 motion review board；发布前需要 MC 连续帧/clip 覆盖每个必需动作模式，并人工确认动作节奏、关键部件参与和粒子遮挡符合设计包。",
        },
        {
            "id": "creature_audio_art_final_pending",
            "severity": "P1",
            "module": "audio_feedback",
            "evidence": (
                f"audio technical gate ok={audio_report.get('technical_ok')}; "
                f"subtitle ok={audio_report.get('subtitle_ok')}; "
                f"art direction ok={audio_report.get('art_direction_ok')}; "
                f"audio final-mix package ok={creature_audio_final_mix.get('ok')}; "
                f"OGG container metrics ok={audio_ogg_container_metrics_ok}; "
                f"OGG container metrics events={audio_ogg_container_metrics_event_count}; "
                f"OGG container metric failures={audio_ogg_container_metrics_failure_count}; "
                f"loudness metrics available={audio_loudness_metrics_available}; "
                f"final mix tooling blocker={audio_final_mix_tooling_blocker}; "
                f"missing audio tools={audio_tooling_missing}; "
                f"replacement required={creature_audio_final_mix.get('replacement_required_count')}; "
                f"final mix candidates={creature_audio_final_mix.get('final_mix_candidate_count')}; "
                f"candidate listening required={creature_audio_final_mix.get('candidate_listening_required_count')}; "
                f"manual listening queue items={audio_manual_listening_queue_count}; "
                f"manual listening batches={audio_manual_listening_batch_count}; "
                f"manual listening total duration seconds={audio_manual_listening_total_duration_seconds}; "
                f"manual listening queue by priority={audio_manual_listening_queue_by_priority}; "
                f"manual listening queue by event={audio_manual_listening_queue_by_event}; "
                f"candidate listening queue items={audio_candidate_listening_queue_count}; "
                f"replacement listening queue items={audio_replacement_listening_queue_count}; "
                f"candidate queue by event={audio_candidate_listening_queue_by_event}; "
                f"review boards={audio_review_board_count}; "
                f"review board contact sheet={audio_review_board_contact_sheet}; "
                f"audition index={audio_audition_index}; "
                f"audition events={audio_audition_event_count}; "
                f"final mix candidate creature count={audio_report.get('final_mix_candidate_count')}; "
                f"procedural first-pass creature count={audio_report.get('procedural_first_pass_count')}; "
                f"technical placeholder repair count={audio_report.get('technical_placeholder_repair_count')}; "
                f"art_final_ok={audio_report.get('art_final_ok')}."
            ),
            "required_1_0_state": "每个 1.0 生物已有技术可播放音效、字幕、音效方向、最终混音 review board 和本地试听入口；占位/程序首版资源需至少替换为最终混音候选，并在发布前完成响度、循环、战斗可读性和人工听感验收。",
        },
    ]
    if not nbt_real_restart_cycle_ok:
        release_blocking_gaps.insert(1, {
            "id": "nbt_runtime_roundtrip_missing",
            "severity": "P0",
            "module": "nbt_persistence",
            "evidence": (
                f"static NBT schema covers {len(nbt_schemas)} owners; "
                f"entity manifest NBT modes: {dict(entity_nbt_modes)}; "
                f"target doc ok={nbt_targets['ok']}; "
                f"target gate ok={nbt_target_report.get('target_gate_ok')} "
                f"required={nbt_target_report.get('status_counts', {}).get('required')} "
                f"runtime_only={nbt_target_report.get('status_counts', {}).get('runtime_only')}; "
                f"source roundtrip probe ok={nbt_roundtrip_report.get('ok')}; "
                f"restart contract probe ok={nbt_restart_contract_report.get('ok')} "
                f"coverage={nbt_restart_contract_report.get('coverage')}; "
                f"NBT release package static ok={nbt_release_acceptance.get('static_contract_ok')} "
                f"runtime entity ok={nbt_release_acceptance.get('runtime_entity_probe_ok')} "
                f"real restart ok={nbt_release_acceptance.get('real_restart_cycle_ok')}; "
                f"release review board={nbt_release_review_board}; "
                f"runtime entity probe ok={runtime_nbt_report['ok']}; "
                f"runtime restart semantic probe ok={runtime_nbt_restart_report['ok']}; "
                f"restart-cycle prepare ok={runtime_nbt_restart_prepare_report['ok']}; "
                f"restart-cycle verify ok={runtime_nbt_restart_verify_report['ok']}."
            ),
            "required_1_0_state": "源码级 round-trip、源码级重启合约、运行中 restart 语义探针只能证明 NBT 读写路径可用；发布前必须完成 prepare -> 保存退出 -> 重启同一世界 -> verify 的真实 MC 存档重启周期报告。",
        })
    if not release_hygiene["ok"]:
        release_blocking_gaps.append({
            "id": "release_version_and_docs_not_aligned",
            "severity": "P2",
            "module": "release_management",
            "evidence": f"release hygiene checks={release_hygiene['checks']}",
            "required_1_0_state": "发布前统一版本号、README、旧项目状态文档和变更日志，避免把原型误称 1.0 完成。",
        })
    if not certificate_economy_ok:
        release_blocking_gaps.insert(3, {
            "id": "certificate_economy_not_closed",
            "severity": "P1",
            "module": "code_ui_nbt_gameplay",
            "evidence": (
                f"certificate economy ok={certificate_economy_ok}; "
                f"guide spell total={certificate_guide_spell_total}; "
                f"custom guardian spell trades={certificate_custom_guardian_spell_trade_total}; "
                f"missing custom guardian trades={certificate_missing_guardian_trade_count}; "
                f"price mismatches={certificate_price_mismatch_count}; "
                f"legacy Greek-only StatueBlock={certificate_legacy_statue_block_greek_only}; "
                f"report={project_path(CERTIFICATE_ECONOMY_REPORT)}."
            ),
            "required_1_0_state": "五文明证书来源、守护者交易、神力图鉴价格、神像/基座存储、交易冷却和 NBT 防刷状态必须一致；52 个 1.0 神力都要有证书消耗入口或明确不走证书经济的理由。",
        })
    if registered_spellbooks.get("atlantean", 0) < 10 or not atlantean_resource_ok:
        release_blocking_gaps.insert(3, {
            "id": "atlantean_spell_matrix_mvp_only",
            "severity": "P1",
            "module": "god_powers",
            "evidence": (
                f"registered spellbooks: {registered_spellbooks}; "
                f"atlantean_resource_ok={atlantean_resource_ok}; "
                f"known gaps: {god_power_known_gaps}"
            ),
            "required_1_0_state": "亚特兰蒂斯神力从 MVP 冻结到 1.0 10 神力矩阵，并补独立图标、粒子和逐项玩法验收。",
        })
    if not god_power_gameplay_acceptance_ok:
        release_blocking_gaps.insert(3, {
            "id": "god_power_playtest_acceptance_not_done",
            "severity": "P1",
            "module": "god_powers",
            "evidence": (
                f"gameplay package ok={god_power_gameplay_package_ok}; "
                f"spell count={god_power_gameplay_acceptance.get('spell_count')}; "
                f"manual playtest required={god_power_manual_playtest_required}; "
                f"gameplay review boards={god_power_review_board_count}; "
                f"review board contact sheet={god_power_review_board_contact_sheet}; "
                f"runtime smoke playtest ok={god_power_runtime_smoke_ok}; "
                f"runtime smoke passed={god_power_runtime_smoke.get('passed')}/{god_power_runtime_smoke.get('total')}; "
                f"per-power runtime smoke probe passed={god_power_runtime_smoke_probe_passed}/{god_power_runtime_smoke_probe_total}; "
                f"gameplay acceptance ok={god_power_gameplay_acceptance_ok}; "
                f"status_counts={god_power_gameplay_acceptance.get('status_counts')}; "
                f"risk_counts={god_power_gameplay_acceptance.get('risk_counts')}; "
                f"playtest_risk_level_counts={god_power_playtest_risk_counts}; "
                f"high_risk_playtest_count={god_power_high_risk_playtest_count}; "
                f"manual playtest queue items={god_power_manual_queue_count}; "
                f"manual playtest batches={god_power_manual_batch_count}; "
                f"manual playtest minimum scenarios={god_power_manual_minimum_scenarios}; "
                f"manual playtest queue by priority={god_power_manual_queue_by_priority}; "
                f"manual playtest queue by risk={god_power_manual_queue_by_risk}; "
                f"manual execution status={god_power_manual_execution_status}; "
                f"manual execution batches={god_power_manual_execution_batch_count}; "
                f"manual execution scenarios={god_power_manual_execution_scenario_total}; "
                f"manual execution world edit tasks={god_power_manual_execution_world_edit_count}; "
                f"manual execution risk counts={god_power_manual_execution_risk_counts}; "
                f"manual execution report={project_path(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT)}; "
                f"dimension_status_counts={god_power_dimension_status_counts}."
            ),
            "required_1_0_state": "52 个非神殿生成神力不仅要注册、进图鉴、带冷却/反馈、粒子和静态玩法 review board，还必须逐项完成人工玩法、平衡、失败路径、性能和实机可读性验收。",
        })
    if not structure_quality_ok:
        release_blocking_gaps.insert(3, {
            "id": "structure_quality_not_art_final",
            "severity": "P1",
            "module": "structures_nbt",
            "evidence": (
                f"structure gate ok={structure_report.get('ok')}; "
                f"warnings={structure_warnings}; "
                f"theme_contract={structure_summary.get('theme_contract_checked_count')}/"
                f"{structure_summary.get('theme_contract_expected_count')}; "
                f"functional_points={structure_summary.get('functional_point_checked_count')}/"
                f"{structure_summary.get('functional_point_expected_count')}."
            ),
            "required_1_0_state": "15 个 canonical 神殿 NBT 不只可读，还要保持数据包无非 canonical 污染，并通过文明主题材料、尺寸、功能点位、守护者/圣物/神像布局验收。",
        })
    if not god_power_full_matrix_ok:
        release_blocking_gaps.insert(3, {
            "id": "particle_matrix_partial",
            "severity": "P1",
            "module": "particles",
            "evidence": (
                f"custom particle count={len(particle_report.get('particles', [])) if isinstance(particle_report, dict) else 0}; "
                f"god-power full_matrix_ok={god_power_full_matrix_ok}; "
                f"full_matrix_failures={god_particle_report.get('full_matrix_failures') if isinstance(god_particle_report, dict) else None}; "
                f"known gaps={god_power_known_gaps}"
            ),
            "required_1_0_state": "每个神力和核心生物技能具备启动、持续、命中/结束三段文明主题粒子，且受性能配置控制。",
        })
    if not ui_guide_scope_ok:
        release_blocking_gaps.insert(4, {
            "id": "ui_scope_only_guardian_trade",
            "severity": "P1",
            "module": "ui",
            "evidence": f"UI gate scope={ui_scope} ok={ui_ok}.",
            "required_1_0_state": "补神力图鉴/文明进度/神殿状态/证书余额等 1.0 UI，并做分辨率和多语言验收。",
        })

    module_gaps = {
        "code": [
            "五文明实体和神力入口已大量存在，52 个神力已有静态玩法验收包，但仍需逐项人工玩法和平衡验收。",
            f"证书经济已有静态矩阵和审计报告，但当前 guide spell total={certificate_guide_spell_total}, "
            f"custom guardian spell trades={certificate_custom_guardian_spell_trade_total}, "
            f"missing guardian trades={certificate_missing_guardian_trade_count}, price mismatches={certificate_price_mismatch_count}。",
        ],
        "models": [
            "五文明 runtime model gate 均通过，但它是部件/token 契约，不是最终审美验收。",
            "希腊作为 vertical slice 已做 V2 精修，仍需用户人工确认三视角设计图级质量。",
        ],
        "actions": [
            f"action/particle static contracts checked={action_report.get('contracts_checked')}; "
            f"creature action acceptance package ok={action_acceptance_ok}; "
            f"human motion acceptance ok={action_human_motion_acceptance_ok}; "
            "仍缺动作预览、受击/死亡/飞行/骑乘/多头联动的人工验收。",
        ],
        "textures": [
            "UV/texture fit 自动检查能发现空白和越界，但关键面表现、材质语言和程序化占位感仍需人工 polish。",
        ],
        "ui": [
            "守护者交易 UI 和神力图鉴 UI 已有静态 gate；仍需发布前做真实分辨率与手柄/键鼠可用性走查。",
        ],
        "art": [
            "需要全局美术 bible 和五文明视觉板，避免生物、建筑、UI、粒子各自漂移。",
        ],
        "particles": [
            "自定义粒子链路存在，神力性能辅助存在；神力静态矩阵已要求全 52 个非神殿生成神力经过统一粒子辅助层，但最终粒子美术和玩法可读性仍需人工验收。",
        ],
        "audio": [
            "30 个 1.0 生物已有技术可播放音效、三语字幕和音效方向；非希腊 20 个仍是程序化首版，需要最终文明主题混音。",
        ],
        "nbt": [
            "静态 NBT schema、目标表和源码级重启契约探针已覆盖 1.0 关键长期状态；仍需真实 MC 存档重启实测。",
        ],
        "structures": [
            "canonical 15/15 结构存在且可读，旧结构和数据包污染已清理；文明主题材料、尺寸和功能点位契约已进入 structure gate，仍需最终人工建筑审美验收。",
        ],
    }

    return {
        "version": 1,
        "scope": "mythic_age_1_0_gap_audit",
        "ready_for_1_0": False,
        "summary": {
            "civilizations": CIVILIZATIONS,
            "civilization_creature_roster": creature_roster,
            "civilization_creature_count": sum(len(ids) for ids in creature_roster.values()),
            "entity_manifest_count": entity_manifest.get("entity_count"),
            "nbt_schema_owner_count": len(nbt_schemas),
            "structure_canonical_existing": structure_summary.get("canonical_existing_count"),
            "structure_canonical_expected": structure_summary.get("canonical_expected_count"),
            "structure_functional_point_checked": structure_summary.get("functional_point_checked_count"),
            "structure_functional_point_expected": structure_summary.get("functional_point_expected_count"),
            "structure_theme_contract_checked": structure_summary.get("theme_contract_checked_count"),
            "structure_theme_contract_expected": structure_summary.get("theme_contract_expected_count"),
            "structure_quality_gate_ok": structure_quality_ok,
            "ui_static_gate_ok": ui_report.get("ok"),
            "ui_guide_scope_ok": ui_guide_scope_ok,
            "particle_static_gate_ok": particle_report.get("ok"),
            "god_power_particle_gate_ok": god_particle_report.get("ok"),
            "god_power_particle_full_matrix_ok": god_power_full_matrix_ok,
            "god_power_gameplay_package_ok": god_power_gameplay_package_ok,
            "god_power_gameplay_static_implementation_ok": god_power_gameplay_acceptance.get("static_implementation_ok"),
            "god_power_gameplay_acceptance_ok": god_power_gameplay_acceptance_ok,
            "god_power_gameplay_manual_playtest_required_count": god_power_manual_playtest_required,
            "god_power_runtime_smoke_playtest_ok": god_power_runtime_smoke_ok,
            "god_power_runtime_smoke_playtest_passed": god_power_runtime_smoke.get("passed"),
            "god_power_runtime_smoke_playtest_total": god_power_runtime_smoke.get("total"),
            "god_power_runtime_smoke_probe_passed_power_count": god_power_runtime_smoke_probe_passed,
            "god_power_runtime_smoke_probe_power_count": god_power_runtime_smoke_probe_total,
            "god_power_runtime_smoke_playtest_report": god_power_runtime_smoke.get("path"),
            "god_power_gameplay_risk_counts": god_power_gameplay_acceptance.get("risk_counts"),
            "god_power_playtest_risk_level_counts": god_power_playtest_risk_counts,
            "god_power_high_risk_playtest_count": god_power_high_risk_playtest_count,
            "god_power_playtest_dimension_status_counts": god_power_dimension_status_counts,
            "god_power_manual_playtest_queue_count": god_power_manual_queue_count,
            "god_power_manual_playtest_batch_count": god_power_manual_batch_count,
            "god_power_manual_playtest_queue_total_minimum_scenarios": god_power_manual_minimum_scenarios,
            "god_power_manual_playtest_queue_by_priority": god_power_manual_queue_by_priority,
            "god_power_manual_playtest_queue_by_risk_level": god_power_manual_queue_by_risk,
            "god_power_manual_playtest_execution_status": god_power_manual_execution_status,
            "god_power_manual_playtest_execution_batch_count": god_power_manual_execution_batch_count,
            "god_power_manual_playtest_execution_scenario_total": god_power_manual_execution_scenario_total,
            "god_power_manual_playtest_execution_world_edit_task_count": god_power_manual_execution_world_edit_count,
            "god_power_manual_playtest_execution_risk_counts": god_power_manual_execution_risk_counts,
            "god_power_manual_playtest_execution_report": project_path(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT),
            "god_power_manual_playtest_execution_markdown": project_path(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_MARKDOWN),
            "god_power_manual_playtest_execution_csv": project_path(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_CSV),
            "god_power_gameplay_review_board_count": god_power_review_board_count,
            "god_power_gameplay_review_board_contact_sheet": god_power_review_board_contact_sheet,
            "certificate_economy_ok": certificate_economy_ok,
            "certificate_guide_spell_total": certificate_guide_spell_total,
            "certificate_custom_guardian_spell_trade_total": certificate_custom_guardian_spell_trade_total,
            "certificate_missing_custom_guardian_trade_count": certificate_missing_guardian_trade_count,
            "certificate_price_mismatch_count": certificate_price_mismatch_count,
            "certificate_legacy_statue_block_greek_only": certificate_legacy_statue_block_greek_only,
            "atlantean_resource_ok": atlantean_resource_ok,
            "audio_technical_gate_ok": audio_report.get("technical_ok"),
            "audio_subtitle_gate_ok": audio_report.get("subtitle_ok"),
            "audio_art_direction_gate_ok": audio_report.get("art_direction_ok"),
            "audio_art_final_ok": audio_report.get("art_final_ok"),
            "audio_art_final_pending_count": audio_report.get("art_final_pending_count"),
            "audio_final_mix_candidate_count": audio_report.get("final_mix_candidate_count"),
            "audio_procedural_first_pass_count": audio_report.get("procedural_first_pass_count"),
            "audio_technical_placeholder_repair_count": audio_report.get("technical_placeholder_repair_count"),
            "creature_audio_final_mix_package_ok": creature_audio_final_mix.get("ok"),
            "creature_audio_final_mix_acceptance_ok": creature_audio_final_mix.get("final_mix_acceptance_ok"),
            "creature_audio_ogg_container_metrics_ok": audio_ogg_container_metrics_ok,
            "creature_audio_ogg_container_metrics_event_count": audio_ogg_container_metrics_event_count,
            "creature_audio_ogg_container_metrics_failure_count": audio_ogg_container_metrics_failure_count,
            "creature_audio_loudness_metrics_available": audio_loudness_metrics_available,
            "creature_audio_final_mix_tooling_blocker": audio_final_mix_tooling_blocker,
            "creature_audio_tooling_missing_tools": audio_tooling_missing,
            "creature_audio_replacement_required_count": creature_audio_final_mix.get("replacement_required_count"),
            "creature_audio_final_mix_candidate_count": creature_audio_final_mix.get("final_mix_candidate_count"),
            "creature_audio_candidate_listening_required_count": creature_audio_final_mix.get("candidate_listening_required_count"),
            "creature_audio_manual_listening_queue_count": audio_manual_listening_queue_count,
            "creature_audio_manual_listening_batch_count": audio_manual_listening_batch_count,
            "creature_audio_manual_listening_total_duration_seconds": audio_manual_listening_total_duration_seconds,
            "creature_audio_manual_listening_queue_by_priority": audio_manual_listening_queue_by_priority,
            "creature_audio_manual_listening_queue_by_event": audio_manual_listening_queue_by_event,
            "creature_audio_candidate_listening_queue_count": audio_candidate_listening_queue_count,
            "creature_audio_replacement_listening_queue_count": audio_replacement_listening_queue_count,
            "creature_audio_candidate_listening_queue_by_event": audio_candidate_listening_queue_by_event,
            "creature_audio_review_board_count": audio_review_board_count,
            "creature_audio_review_board_contact_sheet": audio_review_board_contact_sheet,
            "creature_audio_audition_index": audio_audition_index,
            "creature_audio_audition_event_count": audio_audition_event_count,
            "creature_visual_acceptance_package_ok": visual_package_ok,
            "creature_visual_quality_contract_ok": visual_quality_contract_ok,
            "creature_visual_quality_contract_fail_count": visual_quality_contract_fail_count,
            "creature_visual_design_rejection_blocker_count": visual_design_rejection_count,
            "creature_visual_design_rejection_blockers": visual_design_rejection_blockers,
            "creature_visual_quality_contract_blockers": visual_quality_contract_blockers,
            "creature_runtime_proportion_quality_gate_ok": visual_runtime_proportion_ok,
            "creature_runtime_proportion_quality_blocker_count": visual_runtime_proportion_blocker_count,
            "creature_proportion_detail_first_readability_ok": visual_proportion_detail_first_ok,
            "creature_proportion_detail_first_readability_count": visual_proportion_detail_first_count,
            "creature_runtime_detail_first_readability_ok": visual_runtime_detail_first_ok,
            "creature_runtime_detail_first_readability_count": visual_runtime_detail_first_count,
            "creature_design_first_gate_ok": creature_design_first_gate_ok,
            "creature_design_first_approved_count": creature_design_first_approved_count,
            "creature_design_first_creature_count": creature_design_first_creature_count,
            "creature_design_first_status_counts": creature_design_first_status_counts,
            "creature_design_first_manual_design_approval_required_count": creature_design_first_manual_required_count,
            "creature_design_first_blocked_count": creature_design_first_blocked_count,
            "creature_design_first_gate_report": project_path(CREATURE_DESIGN_FIRST_GATE_REPORT),
            "pegasus_hydra_design_correctness_gate_ok": pegasus_hydra_design_correctness_ok,
            "pegasus_hydra_design_correctness_verdict": pegasus_hydra_design_correctness_verdict,
            "pegasus_hydra_design_correctness_blocked_count": pegasus_hydra_design_correctness_blocked_count,
            "pegasus_hydra_design_correctness_blocked": pegasus_hydra_design_correctness_blocked,
            "pegasus_hydra_design_correctness_report": project_path(PEGASUS_HYDRA_DESIGN_CORRECTNESS_REPORT),
            "pegasus_hydra_v6_modeling_spec_status": pegasus_hydra_v6_modeling_spec_status,
            "pegasus_hydra_v6_modeling_spec_verdict": pegasus_hydra_v6_modeling_spec_verdict,
            "pegasus_hydra_v6_modeling_spec_blocked_count": pegasus_hydra_v6_modeling_spec_blocked_count,
            "pegasus_hydra_v6_modeling_allowed": pegasus_hydra_v6_modeling_allowed,
            "pegasus_hydra_v6_modeling_spec_report": project_path(PEGASUS_HYDRA_V6_MODELING_SPEC_REPORT),
            "pegasus_hydra_v6_modeling_spec_markdown": project_path(PEGASUS_HYDRA_V6_MODELING_SPEC_MARKDOWN),
            "creature_art_rework_queue_ok": creature_art_rework_queue_ok,
            "creature_art_rework_queue_unresolved_count": creature_art_rework_queue_unresolved_count,
            "creature_art_rework_queue_priority_counts": creature_art_rework_queue_priority_counts,
            "creature_art_rework_queue_non_greek_upgrade_count": creature_art_rework_queue_non_greek_upgrade_count,
            "creature_art_rework_queue_report": project_path(CREATURE_ART_REWORK_QUEUE_REPORT),
            "creature_art_production_roadmap_ok": creature_art_production_roadmap_ok,
            "creature_art_production_batch_counts": creature_art_production_batch_counts,
            "creature_art_production_step_status_counts": creature_art_production_step_status_counts,
            "creature_art_production_blocked_creature_count": creature_art_production_blocked_count,
            "creature_art_production_design_blocker_count": creature_art_production_design_blocker_count,
            "creature_art_production_roadmap_report": project_path(CREATURE_ART_PRODUCTION_ROADMAP_REPORT),
            "creature_art_production_roadmap_markdown": project_path(CREATURE_ART_PRODUCTION_ROADMAP_MARKDOWN),
            "creature_design_production_lock_ok": creature_design_production_lock_ok,
            "creature_design_production_lock_status": creature_design_production_lock_status,
            "creature_design_production_locked_count": creature_design_production_locked_count,
            "creature_design_production_unlocked_count": creature_design_production_unlocked_count,
            "creature_design_production_hard_lock_count": creature_design_production_hard_lock_count,
            "creature_design_production_approval_lock_count": creature_design_production_approval_lock_count,
            "creature_design_production_step_lock_counts": creature_design_production_step_lock_counts,
            "creature_design_production_lock_report": project_path(CREATURE_DESIGN_PRODUCTION_LOCK_REPORT),
            "creature_design_production_lock_markdown": project_path(CREATURE_DESIGN_PRODUCTION_LOCK_MARKDOWN),
            "creature_design_approval_review_status": creature_design_approval_review_status,
            "creature_design_approval_review_pending_count": creature_design_approval_review_pending_count,
            "creature_design_approval_review_batch_counts": creature_design_approval_review_batch_counts,
            "creature_design_approval_review_missing_asset_count": creature_design_approval_review_missing_asset_count,
            "creature_design_approval_review_report": project_path(CREATURE_DESIGN_APPROVAL_REVIEW_REPORT),
            "creature_design_approval_review_markdown": project_path(CREATURE_DESIGN_APPROVAL_REVIEW_MARKDOWN),
            "creature_design_approval_review_contact_sheet": project_path(CREATURE_DESIGN_APPROVAL_REVIEW_CONTACT_SHEET),
            "creature_visual_quality_status_counts": visual_quality_status_counts,
            "creature_visual_acceptance_count": creature_visual_acceptance.get("creature_count"),
            "creature_visual_manual_review_required_count": visual_manual_review_required,
            "creature_human_visual_acceptance_ledger_exists": visual_ledger_exists,
            "creature_human_visual_acceptance_ledger": project_path(CREATURE_HUMAN_VISUAL_ACCEPTANCE_LEDGER),
            "creature_human_visual_acceptance_decision_counts": visual_ledger_decision_counts,
            "creature_human_visual_acceptance_approved_count": visual_ledger_approved,
            "creature_human_visual_acceptance_blocked_by_design_count": visual_ledger_blocked_by_design,
            "creature_human_visual_acceptance_pending_shape_review_count": visual_ledger_pending_shape,
            "creature_human_visual_acceptance_pending_user_acceptance_count": visual_ledger_pending_user_acceptance,
            "creature_human_visual_acceptance_checklist_pending_count": visual_ledger_checklist_pending,
            "creature_visual_review_board_count": visual_review_board_count,
            "creature_visual_review_board_contact_sheet": visual_review_board_contact_sheet,
            "creature_visual_revision_pending_user_acceptance_count": visual_revision_pending_user_acceptance,
            "creature_visual_pending_human_shape_review_count": visual_pending_human_shape_review,
            "creature_human_visual_acceptance_ok": visual_acceptance_ok,
            "creature_action_acceptance_package_ok": action_acceptance_ok,
            "creature_action_acceptance_count": creature_action_acceptance.get("creature_count"),
            "creature_action_manual_review_required_count": action_manual_review_required,
            "creature_action_storyboard_count": action_storyboard_count,
            "creature_action_storyboard_contact_sheet": action_storyboard_contact_sheet,
            "creature_action_review_board_count": action_review_board_count,
            "creature_action_review_board_contact_sheet": action_review_board_contact_sheet,
            "creature_action_motion_sequence_evidence_ok": action_motion_sequence_ok,
            "creature_action_motion_sequence_evidence_ok_count": action_motion_sequence_ok_count,
            "creature_action_motion_sequence_full_mode_coverage_ok": action_motion_sequence_full_mode_ok,
            "creature_action_motion_sequence_full_mode_coverage_ok_count": action_motion_sequence_full_mode_ok_count,
            "creature_action_motion_sequence_required_mode_count": action_motion_sequence_required_mode_count,
            "creature_action_motion_sequence_captured_required_mode_count": action_motion_sequence_captured_required_mode_count,
            "creature_action_motion_sequence_missing_mode_count": action_motion_sequence_missing_mode_count,
            "creature_action_motion_sequence_frame_count": action_motion_sequence_frame_count,
            "creature_action_motion_sequence_manifest": action_motion_sequence_manifest,
            "creature_action_motion_capture_queue_count": action_motion_capture_queue_count,
            "creature_action_motion_capture_queue_by_mode": action_motion_capture_queue_by_mode,
            "creature_action_motion_capture_queue_by_civilization": action_motion_capture_queue_by_civilization,
            "creature_action_motion_capture_queue_by_priority": action_motion_capture_queue_by_priority,
            "creature_action_motion_capture_queue_total_minimum_frames": action_motion_capture_total_minimum_frames,
            "creature_action_motion_capture_plan_batch_count": action_motion_capture_plan_batch_count,
            "creature_motion_rework_queue_ok": creature_motion_rework_queue_ok,
            "creature_motion_rework_queued_task_count": creature_motion_rework_queued_task_count,
            "creature_motion_rework_blocked_task_count": creature_motion_rework_blocked_task_count,
            "creature_motion_rework_blocked_creatures": creature_motion_rework_blocked_creatures,
            "creature_motion_rework_task_state_counts": creature_motion_rework_task_state_counts,
            "creature_motion_rework_art_priority_counts": creature_motion_rework_art_priority_counts,
            "creature_motion_rework_minimum_frame_budget": creature_motion_rework_minimum_frame_budget,
            "creature_motion_rework_queue_report": project_path(CREATURE_MOTION_REWORK_QUEUE_REPORT),
            "creature_gameplay_contract_ok": creature_gameplay_contract_ok,
            "creature_gameplay_static_contract_ok": creature_gameplay_static_contract_ok,
            "creature_gameplay_static_contract_ok_count": creature_gameplay_static_ok_count,
            "creature_gameplay_release_ready_count": creature_gameplay_release_ready_count,
            "creature_gameplay_creature_count": creature_gameplay_creature_count,
            "creature_gameplay_static_failure_counts": creature_gameplay_static_failure_counts,
            "creature_gameplay_release_blocker_counts": creature_gameplay_release_blocker_counts,
            "creature_gameplay_contract_report": project_path(CREATURE_GAMEPLAY_CONTRACT_REPORT),
            "creature_gameplay_contract_markdown": project_path(CREATURE_GAMEPLAY_CONTRACT_MARKDOWN),
            "creature_human_motion_acceptance_ok": action_human_motion_acceptance_ok,
            "greek_shape_mechanical_gate_ok": greek_shape_report.get("mechanical_gate_ok"),
            "greek_shape_acceptance_ok": greek_shape_report.get("shape_acceptance_ok"),
            "greek_shape_revision_required_count": greek_shape_report.get("shape_revision_required_count"),
            "greek_shape_revision_pending_user_acceptance_count": greek_shape_report.get("revision_pending_user_acceptance_count"),
            "greek_shape_pending_human_review_count": greek_shape_report.get("pending_human_review_count"),
            "m1_reference_docs_ok": references["ok"],
            "release_hygiene_ok": release_hygiene["ok"],
            "requirement_trace_ok": requirement_trace_ok,
            "requirement_trace_ready_for_1_0": requirement_trace_ready,
            "requirement_trace_status_counts": requirement_trace_status_counts,
            "requirement_trace_open_requirement_count": requirement_trace_open_count,
            "requirement_trace_blocked_requirement_count": requirement_trace_blocked_count,
            "requirement_trace_open_requirements": requirement_trace_open_requirements,
            "requirement_trace_blocked_requirements": requirement_trace_blocked_requirements,
            "requirement_trace_report": project_path(REQUIREMENT_TRACE_REPORT),
            "requirement_trace_markdown": project_path(REQUIREMENT_TRACE_MARKDOWN),
            "nbt_target_doc_ok": nbt_targets["ok"],
            "nbt_target_gate_ok": nbt_target_report.get("target_gate_ok"),
            "nbt_roundtrip_source_probe_ok": nbt_roundtrip_report.get("ok"),
            "nbt_restart_contract_probe_ok": nbt_restart_contract_report.get("ok"),
            "nbt_release_acceptance_package_ok": nbt_release_acceptance.get("ok"),
            "nbt_release_static_contract_ok": nbt_release_acceptance.get("static_contract_ok"),
            "nbt_release_runtime_entity_probe_ok": nbt_release_acceptance.get("runtime_entity_probe_ok"),
            "nbt_release_real_restart_cycle_ok": nbt_release_acceptance.get("real_restart_cycle_ok"),
            "nbt_release_review_board": nbt_release_review_board,
            "nbt_state_ownership_queue_ok": nbt_state_ownership_ok,
            "nbt_state_ownership_status_counts": nbt_state_ownership_status_counts,
            "nbt_state_ownership_priority_counts": nbt_state_ownership_priority_counts,
            "nbt_state_ownership_covered_count": nbt_state_ownership_covered_count,
            "nbt_state_ownership_runtime_only_count": nbt_state_ownership_runtime_only_count,
            "nbt_state_ownership_report": project_path(NBT_STATE_OWNERSHIP_QUEUE_REPORT),
            "nbt_runtime_entity_probe_ok": runtime_nbt_report["ok"],
            "nbt_runtime_restart_semantic_probe_ok": runtime_nbt_restart_report["ok"],
            "nbt_runtime_restart_prepare_ok": runtime_nbt_restart_prepare_report["ok"],
            "nbt_runtime_restart_verify_ok": runtime_nbt_restart_verify_report["ok"],
            "nbt_real_restart_cycle_ok": nbt_real_restart_cycle_ok,
            "nbt_target_required_count": nbt_target_report.get("status_counts", {}).get("required"),
            "nbt_target_runtime_only_count": nbt_target_report.get("status_counts", {}).get("runtime_only"),
            "nbt_target_candidate_count": nbt_target_report.get("status_counts", {}).get("candidate", 0),
        },
        "evidence": {
            "entity_manifest": project_path(ENTITY_MANIFEST),
            "nbt_schema_report": project_path(NBT_REPORT),
            "nbt_target_report": project_path(NBT_TARGET_REPORT),
            "nbt_roundtrip_report": project_path(NBT_ROUNDTRIP_REPORT),
            "nbt_restart_contract_report": project_path(NBT_RESTART_CONTRACT_REPORT),
            "nbt_release_acceptance_report": project_path(NBT_RELEASE_ACCEPTANCE_REPORT),
            "nbt_state_ownership_queue_report": project_path(NBT_STATE_OWNERSHIP_QUEUE_REPORT),
            "runtime_nbt_report": project_path(RUNTIME_NBT_REPORT),
            "runtime_nbt_restart_report": project_path(RUNTIME_NBT_RESTART_REPORT),
            "runtime_nbt_restart_prepare_report": project_path(RUNTIME_NBT_RESTART_PREPARE_REPORT),
            "runtime_nbt_restart_verify_report": project_path(RUNTIME_NBT_RESTART_VERIFY_REPORT),
            "action_particle_report": project_path(ACTION_PARTICLE_REPORT),
            "creature_action_acceptance_report": project_path(CREATURE_ACTION_ACCEPTANCE_REPORT),
            "creature_motion_rework_queue_report": project_path(CREATURE_MOTION_REWORK_QUEUE_REPORT),
            "creature_gameplay_contract_report": project_path(CREATURE_GAMEPLAY_CONTRACT_REPORT),
            "creature_gameplay_contract_markdown": project_path(CREATURE_GAMEPLAY_CONTRACT_MARKDOWN),
            "structure_report": project_path(STRUCTURE_REPORT),
            "ui_report": project_path(UI_REPORT),
            "particle_report": project_path(PARTICLE_REPORT),
            "god_power_particle_report": project_path(GOD_POWER_PARTICLE_REPORT),
            "god_power_gameplay_acceptance_report": project_path(GOD_POWER_GAMEPLAY_ACCEPTANCE_REPORT),
            "god_power_manual_playtest_execution_report": project_path(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_REPORT),
            "god_power_manual_playtest_execution_markdown": project_path(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_MARKDOWN),
            "god_power_manual_playtest_execution_csv": project_path(GOD_POWER_MANUAL_PLAYTEST_EXECUTION_CSV),
            "certificate_economy_report": project_path(CERTIFICATE_ECONOMY_REPORT),
            "audio_report": project_path(AUDIO_REPORT),
            "creature_audio_final_mix_report": project_path(CREATURE_AUDIO_FINAL_MIX_REPORT),
            "creature_visual_acceptance_report": project_path(CREATURE_VISUAL_ACCEPTANCE_REPORT),
            "creature_human_visual_acceptance_ledger": project_path(CREATURE_HUMAN_VISUAL_ACCEPTANCE_LEDGER),
            "creature_runtime_proportion_quality_report": project_path(CREATURE_RUNTIME_PROPORTION_QUALITY_REPORT),
            "creature_design_first_gate_report": project_path(CREATURE_DESIGN_FIRST_GATE_REPORT),
            "pegasus_hydra_design_correctness_report": project_path(PEGASUS_HYDRA_DESIGN_CORRECTNESS_REPORT),
            "pegasus_hydra_v6_modeling_spec_report": project_path(PEGASUS_HYDRA_V6_MODELING_SPEC_REPORT),
            "pegasus_hydra_v6_modeling_spec_markdown": project_path(PEGASUS_HYDRA_V6_MODELING_SPEC_MARKDOWN),
            "creature_art_rework_queue_report": project_path(CREATURE_ART_REWORK_QUEUE_REPORT),
            "creature_art_production_roadmap_report": project_path(CREATURE_ART_PRODUCTION_ROADMAP_REPORT),
            "creature_art_production_roadmap_markdown": project_path(CREATURE_ART_PRODUCTION_ROADMAP_MARKDOWN),
            "creature_design_production_lock_report": project_path(CREATURE_DESIGN_PRODUCTION_LOCK_REPORT),
            "creature_design_production_lock_markdown": project_path(CREATURE_DESIGN_PRODUCTION_LOCK_MARKDOWN),
            "creature_design_approval_review_report": project_path(CREATURE_DESIGN_APPROVAL_REVIEW_REPORT),
            "creature_design_approval_review_markdown": project_path(CREATURE_DESIGN_APPROVAL_REVIEW_MARKDOWN),
            "creature_design_approval_review_contact_sheet": project_path(CREATURE_DESIGN_APPROVAL_REVIEW_CONTACT_SHEET),
            "requirement_trace_report": project_path(REQUIREMENT_TRACE_REPORT),
            "requirement_trace_markdown": project_path(REQUIREMENT_TRACE_MARKDOWN),
            "greek_shape_acceptance_report": project_path(GREEK_SHAPE_ACCEPTANCE_REPORT),
            "visual_reports": {civ: visual[civ]["path"] for civ in CIVILIZATIONS},
            "runtime_reports": {civ: runtime[civ]["path"] for civ in CIVILIZATIONS},
        },
        "visual_gate_status": visual,
        "creature_visual_acceptance": creature_visual_acceptance,
        "creature_human_visual_acceptance_ledger": creature_human_visual_ledger,
        "creature_runtime_proportion_quality": creature_runtime_proportion_quality,
        "creature_design_first_gate": creature_design_first_gate,
        "pegasus_hydra_design_correctness": pegasus_hydra_design_correctness,
        "pegasus_hydra_v6_modeling_spec": pegasus_hydra_v6_modeling_spec,
        "creature_art_rework_queue": creature_art_rework_queue,
        "creature_art_production_roadmap": creature_art_production_roadmap,
        "creature_design_production_lock": creature_design_production_lock,
        "creature_design_approval_review": creature_design_approval_review,
        "creature_action_acceptance": creature_action_acceptance,
        "creature_motion_rework_queue": creature_motion_rework_queue,
        "creature_gameplay_contract": creature_gameplay_contract,
        "greek_shape_acceptance": greek_shape_report,
        "runtime_model_gate_status": runtime,
        "nbt": {
            "schemas_checked": nbt_report.get("schemas_checked"),
            "schema_owners": nbt_schema_owners,
            "entity_nbt_modes": dict(entity_nbt_modes),
            "target_gate": nbt_target_report,
            "roundtrip_source_probe": nbt_roundtrip_report,
            "restart_contract_probe": nbt_restart_contract_report,
            "release_acceptance_package": nbt_release_acceptance,
            "state_ownership_queue": nbt_state_ownership_queue,
            "runtime_entity_probe": runtime_nbt_report,
            "runtime_restart_semantic_probe": runtime_nbt_restart_report,
            "runtime_restart_prepare": runtime_nbt_restart_prepare_report,
            "runtime_restart_verify": runtime_nbt_restart_verify_report,
        },
        "structures": {
            "summary": structure_summary,
            "warnings": structure_warnings,
        },
        "ui": ui_report,
        "particles": {
            "custom_particle_gate": particle_report,
            "god_power_particle_gate": god_particle_report,
        },
        "god_power_gameplay_acceptance": god_power_gameplay_acceptance,
        "god_power_manual_playtest_execution": god_power_manual_playtest_execution,
        "certificate_economy": certificate_economy,
        "audio": sounds,
        "audio_gate": audio_report,
        "creature_audio_final_mix": creature_audio_final_mix,
        "m1_reference_docs": references,
        "release_hygiene": release_hygiene,
        "requirement_trace": requirement_trace,
        "nbt_targets": nbt_targets,
        "release_blocking_gaps": release_blocking_gaps,
        "module_gaps": module_gaps,
        "recommended_next_milestones": [
            "M0: 用本报告冻结当前状态口径，停止引用旧的发布就绪结论。",
            "M1: 先冻结五文明 1.0 内容矩阵、美术 bible、NBT schema 目标和 UI 范围。",
            "M2: 以希腊 10 生物为 1.0 质量样板做人工视觉验收和动作 polish。",
            "M3: 扩展 NBT/结构/资源审计到物品、交易、神殿状态和数据包污染。",
            "M4: 横向补齐非希腊生物音效、动作人工验收、粒子和玩法实测。",
            "M5: 对五文明 52 个非神殿生成神力做玩法、粒子、音效和证书消耗验收。",
            "M6: 统一版本、README、玩家指南、CHANGELOG 和 release checklist。",
        ],
    }


def dashboard_relative_path(project_relative_path: str | None) -> str | None:
    if not project_relative_path:
        return None
    return os.path.relpath(ROOT / project_relative_path, DASHBOARD_PATH.parent).replace(os.sep, "/")


def html_escape(value: Any) -> str:
    return html.escape(str(value), quote=True)


def html_link(project_relative_path: str | None, label: str | None = None) -> str:
    if not project_relative_path:
        return '<span class="muted">missing</span>'
    href = dashboard_relative_path(project_relative_path)
    text = label or project_relative_path
    return f'<a href="{html_escape(href)}">{html_escape(text)}</a>'


def yes_no(value: Any) -> str:
    return "PASS" if bool(value) else "PENDING"


def status_class(value: Any) -> str:
    return "pass" if bool(value) else "pending"


def metric_card(label: str, value: Any, note: str = "") -> str:
    note_html = f'<div class="note">{html_escape(note)}</div>' if note else ""
    return (
        '<section class="metric">'
        f'<span>{html_escape(label)}</span>'
        f'<strong>{html_escape(value)}</strong>'
        f'{note_html}'
        '</section>'
    )


def evidence_card(title: str, status: Any, body: str, links: list[tuple[str | None, str]]) -> str:
    links_html = "\n".join(
        f"<li>{html_link(path, label)}</li>"
        for path, label in links
        if path
    )
    if not links_html:
        links_html = '<li><span class="muted">no linked artifact</span></li>'
    return (
        '<section class="card">'
        '<div class="card-title-row">'
        f'<h2>{html_escape(title)}</h2>'
        f'<span class="pill {status_class(status)}">{yes_no(status)}</span>'
        '</div>'
        f'<p>{html_escape(body)}</p>'
        f'<ul>{links_html}</ul>'
        '</section>'
    )


def write_dashboard(payload: dict[str, Any]) -> None:
    summary = payload["summary"]
    evidence = payload["evidence"]
    blockers = payload["release_blocking_gaps"]
    dashboard_project_path = project_path(DASHBOARD_PATH)
    payload["dashboard"] = dashboard_project_path
    payload["summary"]["release_acceptance_dashboard"] = dashboard_project_path
    payload["evidence"]["release_acceptance_dashboard"] = dashboard_project_path

    blocker_cards = "\n".join(
        '<section class="blocker">'
        '<div class="card-title-row">'
        f'<h2>{html_escape(gap["id"])}</h2>'
        f'<span class="pill severity">{html_escape(gap["severity"])}</span>'
        '</div>'
        f'<div class="module">{html_escape(gap["module"])}</div>'
        f'<p>{html_escape(gap["required_1_0_state"])}</p>'
        f'<details><summary>Evidence</summary><pre>{html_escape(gap["evidence"])}</pre></details>'
        '</section>'
        for gap in blockers
    )

    metrics = "\n".join([
        metric_card("Civilizations", len(summary["civilizations"]), ", ".join(summary["civilizations"])),
        metric_card("Creatures", summary["civilization_creature_count"], "Five civilization 1.0 roster"),
        metric_card("Visual Boards", summary["creature_visual_review_board_count"], "Static visual review, no MC launch"),
        metric_card("Motion Boards", summary["creature_action_review_board_count"], "Static action storyboard and review boards"),
        metric_card("Motion Sequences", f"{summary['creature_action_motion_sequence_evidence_ok_count']}/{summary['creature_action_acceptance_count']}", "MC frame sequence coverage"),
        metric_card("Audio Events", summary["creature_audio_audition_event_count"], "Local audition page entries"),
        metric_card("God Powers", summary["god_power_gameplay_review_board_count"], "Static gameplay review boards"),
        metric_card("Certificate Trades", f"{summary['certificate_custom_guardian_spell_trade_total']}/{summary['certificate_guide_spell_total']}", "Guardian trades vs guide spell matrix"),
        metric_card("NBT Owners", summary["nbt_schema_owner_count"], "Static schema owners"),
        metric_card("Canonical Structures", f"{summary['structure_canonical_existing']}/{summary['structure_canonical_expected']}", "Structure gate"),
    ])

    evidence_cards = "\n".join([
        evidence_card(
            "Creature Visual Acceptance",
            summary["creature_human_visual_acceptance_ok"],
            (
                f"{summary['creature_visual_acceptance_count']} creatures have static visual review boards. "
                f"Quality contract failures: {summary['creature_visual_quality_contract_fail_count']}; "
                f"design rejections: {summary['creature_visual_design_rejection_blocker_count']}; "
                f"human ledger decisions: {html_escape(str(summary['creature_human_visual_acceptance_decision_counts']))}; "
                f"detail-first design/runtime: {summary['creature_proportion_detail_first_readability_count']}/"
                f"{summary['creature_visual_acceptance_count']} and {summary['creature_runtime_detail_first_readability_count']}/"
                f"{summary['creature_visual_acceptance_count']}; "
                f"manual review required: {summary['creature_visual_manual_review_required_count']}."
            ),
            [
                (summary["creature_visual_review_board_contact_sheet"], "visual review contact sheet"),
                (summary["creature_human_visual_acceptance_ledger"], "human visual acceptance ledger"),
                (evidence["creature_visual_acceptance_report"], "visual acceptance JSON"),
                (evidence["greek_shape_acceptance_report"], "Greek shape acceptance JSON"),
            ],
        ),
        evidence_card(
            "Creature Motion Acceptance",
            summary["creature_human_motion_acceptance_ok"],
            (
                f"{summary['creature_action_acceptance_count']} creatures have static motion evidence. "
                f"Storyboards: {summary['creature_action_storyboard_count']}; "
                f"MC sequence coverage: {summary['creature_action_motion_sequence_evidence_ok_count']}/{summary['creature_action_acceptance_count']} "
                f"({summary['creature_action_motion_sequence_frame_count']} frames); "
                f"full-mode coverage: {summary['creature_action_motion_sequence_full_mode_coverage_ok_count']}/{summary['creature_action_acceptance_count']}; "
                f"required modes covered: {summary['creature_action_motion_sequence_captured_required_mode_count']}/{summary['creature_action_motion_sequence_required_mode_count']}; "
                f"capture queue: {summary['creature_action_motion_capture_queue_count']} "
                f"{html_escape(str(summary['creature_action_motion_capture_queue_by_mode']))}; "
                f"batches: {summary['creature_action_motion_capture_plan_batch_count']}; "
                f"minimum frames: {summary['creature_action_motion_capture_queue_total_minimum_frames']}; "
                f"manual review required: {summary['creature_action_manual_review_required_count']}."
            ),
            [
                (summary["creature_action_review_board_contact_sheet"], "motion review contact sheet"),
                (summary["creature_action_storyboard_contact_sheet"], "motion storyboard contact sheet"),
                (summary["creature_action_motion_sequence_manifest"], "motion sequence manifest"),
                (evidence["creature_action_acceptance_report"], "action acceptance JSON"),
            ],
        ),
        evidence_card(
            "Creature Audio Final Mix",
            summary["creature_audio_final_mix_acceptance_ok"],
            (
                f"{summary['creature_audio_audition_event_count']} audio events are indexed for local listening. "
                f"OGG container metrics: {summary['creature_audio_ogg_container_metrics_event_count']} events, "
                f"failures {summary['creature_audio_ogg_container_metrics_failure_count']}; "
                f"loudness metrics available: {summary['creature_audio_loudness_metrics_available']}; "
                f"missing tools: {html_escape(str(summary['creature_audio_tooling_missing_tools']))}; "
                f"Replacement required: {summary['creature_audio_replacement_required_count']}; "
                f"candidates pending listening: {summary['creature_audio_candidate_listening_required_count']}; "
                f"listening batches: {summary['creature_audio_manual_listening_batch_count']}; "
                f"listening duration: {summary['creature_audio_manual_listening_total_duration_seconds']}s; "
                f"candidate event queue: {summary['creature_audio_candidate_listening_queue_count']} "
                f"{html_escape(str(summary['creature_audio_candidate_listening_queue_by_event']))}."
            ),
            [
                (summary["creature_audio_audition_index"], "local audio audition index"),
                (summary["creature_audio_review_board_contact_sheet"], "final mix review contact sheet"),
                (evidence["creature_audio_final_mix_report"], "final mix acceptance JSON"),
                (evidence["audio_report"], "audio technical gate JSON"),
            ],
        ),
        evidence_card(
            "God Power Gameplay",
            summary["god_power_gameplay_acceptance_ok"],
            (
                f"{summary['god_power_gameplay_review_board_count']} non-temple-spawn god powers have static gameplay boards. "
                f"Runtime smoke playtest: {summary['god_power_runtime_smoke_playtest_passed']}/"
                f"{summary['god_power_runtime_smoke_playtest_total']}. "
                f"Per-power probes: {summary['god_power_runtime_smoke_probe_passed_power_count']}/"
                f"{summary['god_power_runtime_smoke_probe_power_count']}; "
                f"high-risk playtests: {summary['god_power_high_risk_playtest_count']}; "
                f"risk levels: {summary['god_power_playtest_risk_level_counts']}. "
                f"manual queue: {summary['god_power_manual_playtest_queue_count']} powers, "
                f"{summary['god_power_manual_playtest_batch_count']} batches, "
                f"{summary['god_power_manual_playtest_queue_total_minimum_scenarios']} minimum scenarios. "
                f"Manual playtest required: {summary['god_power_gameplay_manual_playtest_required_count']}."
            ),
            [
                (summary["god_power_gameplay_review_board_contact_sheet"], "gameplay review contact sheet"),
                (summary["god_power_runtime_smoke_playtest_report"], "runtime smoke playtest report"),
                (evidence["god_power_gameplay_acceptance_report"], "gameplay acceptance JSON"),
                (evidence["god_power_particle_report"], "god-power particle JSON"),
            ],
        ),
        evidence_card(
            "Certificate Economy",
            summary["certificate_economy_ok"],
            (
                f"Guide spell matrix: {summary['certificate_guide_spell_total']}; "
                f"custom guardian spell trades: {summary['certificate_custom_guardian_spell_trade_total']}; "
                f"missing trades: {summary['certificate_missing_custom_guardian_trade_count']}; "
                f"price mismatches: {summary['certificate_price_mismatch_count']}."
            ),
            [
                (evidence["certificate_economy_report"], "certificate economy JSON"),
                (payload["m1_reference_docs"]["docs"]["certificate_economy_matrix"]["path"], "certificate economy matrix"),
            ],
        ),
        evidence_card(
            "NBT Persistence",
            summary["nbt_real_restart_cycle_ok"],
            (
                f"Static contract ok: {summary['nbt_release_static_contract_ok']}; "
                f"runtime entity probe ok: {summary['nbt_release_runtime_entity_probe_ok']}; "
                f"real restart cycle ok: {summary['nbt_release_real_restart_cycle_ok']}."
            ),
            [
                (summary["nbt_release_review_board"], "NBT release review board"),
                (evidence["nbt_release_acceptance_report"], "NBT release acceptance JSON"),
                (evidence["nbt_target_report"], "NBT target gate JSON"),
                (evidence["nbt_restart_contract_report"], "NBT restart contract JSON"),
            ],
        ),
        evidence_card(
            "Structures, UI, Particles",
            (
                summary["structure_quality_gate_ok"]
                and summary["ui_static_gate_ok"]
                and summary["particle_static_gate_ok"]
                and summary["god_power_particle_full_matrix_ok"]
            ),
            (
                f"Structures: {summary['structure_theme_contract_checked']}/{summary['structure_theme_contract_expected']} theme contracts. "
                f"UI static gate: {summary['ui_static_gate_ok']}; particle full matrix: {summary['god_power_particle_full_matrix_ok']}."
            ),
            [
                (evidence["structure_report"], "structure report JSON"),
                (evidence["ui_report"], "UI report JSON"),
                (evidence["particle_report"], "custom particle report JSON"),
                (evidence["god_power_particle_report"], "god-power particle report JSON"),
            ],
        ),
    ])

    roster_rows = "\n".join(
        "<tr>"
        f"<th>{html_escape(civ)}</th>"
        f"<td>{html_escape(', '.join(ids))}</td>"
        "</tr>"
        for civ, ids in summary["civilization_creature_roster"].items()
    )

    gate_rows = "\n".join(
        "<tr>"
        f"<th>{html_escape(label)}</th>"
        f'<td><span class="pill {status_class(value)}">{yes_no(value)}</span></td>'
        "</tr>"
        for label, value in [
            ("Reference docs", summary["m1_reference_docs_ok"]),
            ("Release hygiene", summary["release_hygiene_ok"]),
            ("Structure quality", summary["structure_quality_gate_ok"]),
            ("UI static guide scope", summary["ui_guide_scope_ok"]),
            ("Particle static gate", summary["particle_static_gate_ok"]),
            ("God power particle full matrix", summary["god_power_particle_full_matrix_ok"]),
            ("Creature visual human acceptance", summary["creature_human_visual_acceptance_ok"]),
            ("Creature motion human acceptance", summary["creature_human_motion_acceptance_ok"]),
            ("Creature MC motion sequence evidence", summary["creature_action_motion_sequence_evidence_ok"]),
            ("Creature MC full-mode sequence coverage", summary["creature_action_motion_sequence_full_mode_coverage_ok"]),
            ("Creature audio final mix", summary["creature_audio_final_mix_acceptance_ok"]),
            ("Certificate economy", summary["certificate_economy_ok"]),
            ("God power gameplay acceptance", summary["god_power_gameplay_acceptance_ok"]),
            ("NBT real restart cycle", summary["nbt_real_restart_cycle_ok"]),
        ]
    )

    doc_links = "\n".join(
        f"<li>{html_link(doc['path'], key)}</li>"
        for key, doc in payload["m1_reference_docs"]["docs"].items()
    )
    visual_report_links = "\n".join(
        f"<li>{html_link(path, civ + ' visual report')}</li>"
        for civ, path in evidence["visual_reports"].items()
    )
    runtime_report_links = "\n".join(
        f"<li>{html_link(path, civ + ' runtime model report')}</li>"
        for civ, path in evidence["runtime_reports"].items()
    )
    milestone_items = "\n".join(
        f"<li>{html_escape(item)}</li>"
        for item in payload["recommended_next_milestones"]
    )

    html_text = f"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Mythic Age 1.0 Release Acceptance Dashboard</title>
  <style>
    :root {{
      color-scheme: light;
      --ink: #20242b;
      --muted: #667080;
      --line: #d9dee8;
      --paper: #f7f8fb;
      --panel: #ffffff;
      --pending: #a13d16;
      --pass: #1f7a4d;
      --accent: #284f8f;
      --soft: #edf2f9;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      background: var(--paper);
      color: var(--ink);
      line-height: 1.55;
    }}
    header {{
      padding: 32px 40px 24px;
      background: #ffffff;
      border-bottom: 1px solid var(--line);
    }}
    main {{ padding: 28px 40px 48px; }}
    h1 {{ margin: 0 0 8px; font-size: clamp(28px, 4vw, 46px); letter-spacing: 0; }}
    h2 {{ margin: 0; font-size: 18px; letter-spacing: 0; }}
    h3 {{ margin: 32px 0 14px; font-size: 20px; letter-spacing: 0; }}
    p {{ margin: 8px 0 0; }}
    a {{ color: var(--accent); text-decoration-thickness: 1px; text-underline-offset: 3px; }}
    table {{ width: 100%; border-collapse: collapse; background: var(--panel); border: 1px solid var(--line); }}
    th, td {{ padding: 10px 12px; border-bottom: 1px solid var(--line); text-align: left; vertical-align: top; }}
    th {{ width: 220px; color: #384050; background: #f1f4f9; }}
    pre {{ white-space: pre-wrap; overflow-wrap: anywhere; padding: 12px; background: #f4f6fa; border: 1px solid var(--line); border-radius: 8px; }}
    .subhead {{ color: var(--muted); max-width: 1120px; }}
    .warning {{
      margin-top: 18px;
      padding: 12px 14px;
      border: 1px solid #efc1a6;
      background: #fff4ed;
      color: #6d2d12;
      border-radius: 8px;
      max-width: 1120px;
    }}
    .metrics {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 12px;
    }}
    .metric, .card, .blocker {{
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 8px;
      padding: 16px;
    }}
    .metric span {{ display: block; color: var(--muted); font-size: 13px; }}
    .metric strong {{ display: block; margin-top: 4px; font-size: 28px; letter-spacing: 0; }}
    .note {{ color: var(--muted); font-size: 12px; margin-top: 4px; }}
    .grid {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 14px;
    }}
    .card-title-row {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }}
    .pill {{
      display: inline-block;
      min-width: 78px;
      text-align: center;
      padding: 3px 8px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0;
    }}
    .pill.pass {{ color: #0f5633; background: #dff2e8; }}
    .pill.pending {{ color: #7e2e10; background: #ffe6d8; }}
    .pill.severity {{ color: #222; background: #e8edf7; }}
    .blockers {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
      gap: 14px;
    }}
    .blocker {{ border-color: #e8b18d; }}
    .module {{ color: var(--muted); margin-top: 4px; font-size: 13px; }}
    .muted {{ color: var(--muted); }}
    .two-col {{
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      gap: 16px;
    }}
    @media (max-width: 860px) {{
      header, main {{ padding-left: 18px; padding-right: 18px; }}
      .two-col {{ grid-template-columns: 1fr; }}
      th {{ width: 140px; }}
    }}
  </style>
</head>
<body>
  <header>
    <h1>Mythic Age 1.0 Release Acceptance Dashboard</h1>
    <p class="subhead">Generated from <code>{html_escape(project_path(REPORT_PATH))}</code>. Current result: <strong>NOT READY</strong>; release-blocking gaps: <strong>{len(blockers)}</strong>.</p>
    <div class="warning">本页面只汇总静态证据和人工验收入口；它不代表 MC 客户端实机测试已通过，也没有启动游戏。</div>
  </header>
  <main>
    <h3>Top Metrics</h3>
    <div class="metrics">{metrics}</div>

    <h3>Release Blockers</h3>
    <div class="blockers">{blocker_cards}</div>

    <h3>Evidence Boards</h3>
    <div class="grid">{evidence_cards}</div>

    <div class="two-col">
      <section>
        <h3>Gate Status</h3>
        <table>{gate_rows}</table>
      </section>
      <section>
        <h3>1.0 Creature Roster</h3>
        <table>{roster_rows}</table>
      </section>
    </div>

    <div class="two-col">
      <section>
        <h3>Reference Docs</h3>
        <ul>{doc_links}</ul>
      </section>
      <section>
        <h3>Per-Civilization Reports</h3>
        <ul>{visual_report_links}{runtime_report_links}</ul>
      </section>
    </div>

    <h3>Recommended Milestones</h3>
    <ol>{milestone_items}</ol>
  </main>
</body>
</html>
"""
    DASHBOARD_PATH.write_text(html_text, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()

    if args.command == "audit":
        payload = build_report()
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        write_dashboard(payload)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print("Mythic Age 1.0 Gap Audit: NOT READY")
        print(f"report: {REPORT_PATH}")
        print(f"dashboard: {DASHBOARD_PATH}")
        print(f"release_blocking_gaps: {len(payload['release_blocking_gaps'])}")
        print(f"civilization_creatures: {payload['summary']['civilization_creature_count']}")
        print(f"nbt_schema_owners: {payload['summary']['nbt_schema_owner_count']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
