#!/usr/bin/env python3
"""Audit the static 1.0 god-power certificate economy contract."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/certificate_economy"
REPORT_PATH = REPORT_DIR / "certificate_economy_report.json"
DOC_PATH = ROOT / "docs/系统设计/MythicAgeMod_1.0_五文明证书经济矩阵_2026-06-12.md"

GUIDE_SCREEN = ROOT / "src/main/java/com/example/mythic_age/client/gui/GodPowerGuideScreen.java"
CUSTOM_GUARDIAN = ROOT / "src/main/java/com/example/mythic_age/entity/CustomTempleGuardianEntity.java"
TEMPLE_GUARDIAN = ROOT / "src/main/java/com/example/mythic_age/entity/TempleGuardianEntity.java"
STATUE_BLOCK = ROOT / "src/main/java/com/example/mythic_age/block/StatueBlock.java"
CERTIFICATE_MANAGER = ROOT / "src/main/java/com/example/mythic_age/system/GodPowerCertificateManager.java"
CERTIFICATE_GENERATION_MANAGER = ROOT / "src/main/java/com/example/mythic_age/system/CertificateGenerationManager.java"
STATUE_BASE_ENTITY = ROOT / "src/main/java/com/example/mythic_age/block/entity/StatueBaseBlockEntity.java"
CUSTOM_GUARDIAN_TRADE_SCREEN = ROOT / "src/main/java/com/example/mythic_age/client/gui/CustomTempleGuardianTradeScreen.java"
LANG_ZH = ROOT / "src/main/resources/assets/mythic_age/lang/zh_cn.json"
LANG_EN = ROOT / "src/main/resources/assets/mythic_age/lang/en_us.json"

CIVILIZATIONS = ["greek", "egyptian", "chinese", "nordic", "atlantean"]
ENUM_BY_CIV = {
    "greek": "GREEK",
    "egyptian": "EGYPTIAN",
    "chinese": "CHINESE",
    "nordic": "NORDIC",
    "atlantean": "ATLANTEAN",
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_text(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def extract_switch_case(text: str, enum_name: str) -> str:
    marker = f"case {enum_name} ->"
    start = text.find(marker)
    if start < 0:
        return ""
    brace_start = text.find("{", start)
    if brace_start < 0:
        return ""
    depth = 0
    for index in range(brace_start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[brace_start + 1:index]
    return ""


def guide_spells(text: str, civ: str) -> dict[str, int]:
    method_match = re.search(
        rf"static GuideCivilization {re.escape(civ)}\(\) \{{(?P<body>.*?)\n        \}}\n\n        static GuideCivilization",
        text,
        flags=re.DOTALL,
    )
    if not method_match:
        if civ == "atlantean":
            method_match = re.search(
                r"static GuideCivilization atlantean\(\) \{(?P<body>.*?)\n        \}\n\n        static GuideCivilization general",
                text,
                flags=re.DOTALL,
            )
        if not method_match:
            return {}
    body = method_match.group("body")
    spells: dict[str, int] = {}
    for item, cost in re.findall(r"new GuideSpell\(ModItems\.(SPELLBOOK_[A-Z0-9_]+),\s*(\d+)\)", body):
        spells[item] = int(cost)
    return spells


def guardian_trades(text: str, civ: str) -> dict[str, int]:
    case_body = extract_switch_case(text, ENUM_BY_CIV[civ])
    trades: dict[str, int] = {}
    for count, item in re.findall(
        r"safeAddTrade\(certificate,\s*(\d+),\s*ModItems\.(SPELLBOOK_[A-Z0-9_]+)",
        case_body,
    ):
        trades[item] = int(count)
    return trades


def source_contracts(texts: dict[str, str]) -> dict[str, Any]:
    custom_guardian = texts["custom_guardian"]
    certificate_manager = texts["certificate_manager"]
    certificate_generation = texts["certificate_generation"]
    mythic_world_state = texts["statue_base_entity"]
    return {
        "greek_temple_villager_generation": (
            "generateCertificatesForNearbyVillagers" in custom_guardian
            and "mythologyType != GodPowerCertificateItem.MythologyType.GREEK" in custom_guardian
            and "generateCertificatesForTemple" in certificate_generation
        ),
        "generic_temple_generation_supports_five_civilizations": all(
            token in certificate_generation
            for token in [
                "case GREEK",
                "case EGYPTIAN",
                "case CHINESE",
                "case ATLANTEAN",
                "case NORDIC",
            ]
        ),
        "egyptian_statue_generation": (
            "tickEgyptianStatues" in certificate_manager
            and "EgyptianStatuePedestalBlockEntity" in texts["egyptian_statue_entity"]
        ),
        "chinese_population_generation_method": "handleChinesePopulationGeneration" in certificate_manager,
        "nordic_kill_generation_method": "handleNordicKill" in certificate_manager,
        "nordic_combat_event_registered": "handleNordicKill" in texts["combat_events"],
        "atlantean_prismarine_guardian_trade": (
            "Items.PRISMARINE_SHARD" in custom_guardian
            and "GOD_POWER_CERTIFICATE_ATLANTEAN" in custom_guardian
        ),
        "statue_base_certificate_nbt": all(
            token in mythic_world_state
            for token in ["StoredCertificates", "LastUpdateTime", "MythologyType"]
        ),
        "custom_guardian_trade_cooldown_nbt": all(
            token in custom_guardian
            for token in ["tradeCooldowns", "putLong", "getLong"]
        ),
    }


def build_report() -> dict[str, Any]:
    texts = {
        "guide": load_text(GUIDE_SCREEN),
        "custom_guardian": load_text(CUSTOM_GUARDIAN),
        "temple_guardian": load_text(TEMPLE_GUARDIAN),
        "statue_block": load_text(STATUE_BLOCK),
        "certificate_manager": load_text(CERTIFICATE_MANAGER),
        "certificate_generation": load_text(CERTIFICATE_GENERATION_MANAGER),
        "statue_base_entity": load_text(STATUE_BASE_ENTITY),
        "trade_screen": load_text(CUSTOM_GUARDIAN_TRADE_SCREEN),
        "lang_zh": load_text(LANG_ZH),
        "lang_en": load_text(LANG_EN),
        "combat_events": load_text(ROOT / "src/main/java/com/example/mythic_age/event/ModCombatEvents.java"),
        "egyptian_statue_entity": load_text(ROOT / "src/main/java/com/example/mythic_age/block/entity/EgyptianStatuePedestalBlockEntity.java"),
    }

    civilizations: dict[str, Any] = {}
    total_missing: list[dict[str, Any]] = []
    price_mismatches: list[dict[str, Any]] = []
    for civ in CIVILIZATIONS:
        guide = guide_spells(texts["guide"], civ)
        custom = guardian_trades(texts["custom_guardian"], civ)
        temple = guardian_trades(texts["temple_guardian"], civ)
        missing_custom = sorted(set(guide) - set(custom))
        extra_custom = sorted(set(custom) - set(guide))
        mismatches = []
        for item in sorted(set(guide) & set(custom)):
            if guide[item] != custom[item]:
                mismatches.append({
                    "spellbook": item,
                    "guide_cost": guide[item],
                    "custom_guardian_cost": custom[item],
                })
        for item in missing_custom:
            total_missing.append({"civilization": civ, "spellbook": item})
        for mismatch in mismatches:
            price_mismatches.append({"civilization": civ, **mismatch})
        civilizations[civ] = {
            "guide_spell_count": len(guide),
            "custom_guardian_spell_trade_count": len(custom),
            "temple_guardian_spell_trade_count": len(temple),
            "missing_from_custom_guardian": missing_custom,
            "extra_custom_guardian_spell_trades": extra_custom,
            "price_mismatches": mismatches,
            "guide_spells": guide,
            "custom_guardian_spells": custom,
            "temple_guardian_spells": temple,
            "custom_guardian_matches_guide": not missing_custom and not extra_custom and not mismatches,
        }

    source = source_contracts(texts)
    legacy_statue_block_greek_only = (
        "mythologyType == GodPowerCertificateItem.MythologyType.GREEK" in texts["statue_block"]
        and "menu.coming_soon" in texts["statue_block"]
    )
    language_source_keys = {
        "zh": {
            civ: f"gui.mythic_age.god_power_guide.acquisition.{civ}" in texts["lang_zh"]
            for civ in CIVILIZATIONS
        },
        "en": {
            civ: f"gui.mythic_age.god_power_guide.acquisition.{civ}" in texts["lang_en"]
            for civ in CIVILIZATIONS
        },
    }

    blockers = []
    if total_missing:
        blockers.append({
            "id": "guardian_trade_matrix_incomplete",
            "severity": "P1",
            "details": total_missing,
        })
    if price_mismatches:
        blockers.append({
            "id": "guide_and_guardian_prices_mismatch",
            "severity": "P1",
            "details": price_mismatches,
        })
    if legacy_statue_block_greek_only:
        blockers.append({
            "id": "statue_block_legacy_greek_only_exchange",
            "severity": "P2",
            "details": project_path(STATUE_BLOCK),
        })
    if not source["chinese_population_generation_method"]:
        blockers.append({
            "id": "chinese_certificate_source_missing",
            "severity": "P1",
            "details": project_path(CERTIFICATE_MANAGER),
        })
    if not source["atlantean_prismarine_guardian_trade"]:
        blockers.append({
            "id": "atlantean_certificate_source_missing",
            "severity": "P1",
            "details": project_path(CUSTOM_GUARDIAN),
        })

    return {
        "version": 1,
        "scope": "certificate_economy_static_audit",
        "ok": not blockers,
        "static_contract_ok": True,
        "playtest_required": True,
        "documentation": project_path(DOC_PATH),
        "reports": {
            "certificate_economy_report": project_path(REPORT_PATH),
        },
        "evidence": {
            "guide_screen": project_path(GUIDE_SCREEN),
            "custom_guardian": project_path(CUSTOM_GUARDIAN),
            "temple_guardian": project_path(TEMPLE_GUARDIAN),
            "statue_block": project_path(STATUE_BLOCK),
            "certificate_manager": project_path(CERTIFICATE_MANAGER),
            "certificate_generation_manager": project_path(CERTIFICATE_GENERATION_MANAGER),
            "statue_base_entity": project_path(STATUE_BASE_ENTITY),
            "trade_screen": project_path(CUSTOM_GUARDIAN_TRADE_SCREEN),
            "lang_zh": project_path(LANG_ZH),
            "lang_en": project_path(LANG_EN),
        },
        "summary": {
            "civilization_count": len(CIVILIZATIONS),
            "guide_spell_total": sum(data["guide_spell_count"] for data in civilizations.values()),
            "custom_guardian_spell_trade_total": sum(data["custom_guardian_spell_trade_count"] for data in civilizations.values()),
            "missing_custom_guardian_trade_count": len(total_missing),
            "price_mismatch_count": len(price_mismatches),
            "legacy_statue_block_greek_only": legacy_statue_block_greek_only,
            "source_contracts": source,
        },
        "civilizations": civilizations,
        "language_source_keys": language_source_keys,
        "blockers": blockers,
        "recommended_next_actions": [
            "Add missing Egyptian, Chinese, and Nordic spellbook trades to CustomTempleGuardianEntity.",
            "Align GodPowerGuideScreen costs with actual guardian trade costs.",
            "Decide whether StatueBlock is legacy display-only or upgrade it to a five-civilization exchange entry.",
            "Confirm Chinese population generation call sites or add a village production tick/event.",
            "Freeze Atlantean certificate source as prismarine trade or implement ocean-ruin exploration with persistent anti-farm state.",
            "Extend runtime NBT restart validation to certificate storage, guardian trade cooldowns, and civilization production state.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()

    if args.command == "audit":
        payload = build_report()
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print("Certificate Economy Audit:", "OK" if payload["ok"] else "NOT READY")
        print(f"report: {REPORT_PATH}")
        print(f"missing_custom_guardian_trade_count: {payload['summary']['missing_custom_guardian_trade_count']}")
        print(f"price_mismatch_count: {payload['summary']['price_mismatch_count']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
