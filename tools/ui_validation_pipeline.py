#!/usr/bin/env python3
"""Static UI gate for Mythic Age 1.0 interface work."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/ui_validation"
REPORT_PATH = REPORT_DIR / "ui_report.json"
TRADE_SCREEN = ROOT / "src/main/java/com/example/mythic_age/client/gui/CustomTempleGuardianTradeScreen.java"
GUIDE_SCREEN = ROOT / "src/main/java/com/example/mythic_age/client/gui/GodPowerGuideScreen.java"
GUIDE_ITEM = ROOT / "src/main/java/com/example/mythic_age/item/GodPowerGuideItem.java"
NETWORKING = ROOT / "src/main/java/com/example/mythic_age/network/ModNetworking.java"
NETWORK_CLIENT = ROOT / "src/main/java/com/example/mythic_age/network/ModNetworkClient.java"
GUARDIAN_ENTITY = ROOT / "src/main/java/com/example/mythic_age/entity/CustomTempleGuardianEntity.java"
GUI_TEXTURE = ROOT / "src/main/resources/assets/mythic_age/textures/gui/temple_guardian_trade.png"
LANG_FILES = [
    ROOT / "src/main/resources/assets/mythic_age/lang/en_us.json",
    ROOT / "src/main/resources/assets/mythic_age/lang/zh_cn.json",
    ROOT / "src/main/resources/assets/mythic_age/lang/ja_jp.json",
]


UI_CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "custom_texture",
        "path": TRADE_SCREEN,
        "tokens": [
            '"textures/gui/temple_guardian_trade.png"',
            "context.drawTexture(TEXTURE",
        ],
    },
    {
        "id": "trade_list_navigation",
        "path": TRADE_SCREEN,
        "tokens": [
            "VISIBLE_ROWS",
            "scrollOffset",
            "mouseScrolled(",
            "selectTrade(",
            "renderTradeList(",
        ],
    },
    {
        "id": "trade_detail_status",
        "path": TRADE_SCREEN,
        "tokens": [
            "renderTradeDetails(",
            "countPlayerItems(player)",
            "statusText(player, guardian)",
            "getTradeCooldownSeconds(output)",
            "executeButton.active",
        ],
    },
    {
        "id": "hover_tooltips",
        "path": TRADE_SCREEN,
        "tokens": [
            "Tooltip.of(",
            "drawItemTooltip(",
            "renderHoveredItemTooltip(",
        ],
    },
    {
        "id": "translatable_text",
        "path": TRADE_SCREEN,
        "tokens": [
            'Text.translatable("gui.mythic_age.temple_guardian_trade.title")',
            "mythic_age.civilization.greek",
            "gui.mythic_age.temple_guardian_trade.inventory",
            "gui.mythic_age.temple_guardian_trade.cooldown",
        ],
    },
    {
        "id": "guardian_cooldown_read_api",
        "path": GUARDIAN_ENTITY,
        "tokens": [
            "public boolean hasCooldown(ItemStack output)",
            "public long getTradeCooldownSeconds(ItemStack output)",
            "tradeCooldowns.getOrDefault",
        ],
    },
    {
        "id": "god_power_guide_screen_tabs",
        "path": GUIDE_SCREEN,
        "tokens": [
            "class GodPowerGuideScreen extends Screen",
            "GuideTab.OVERVIEW",
            "GuideTab.SPELLS",
            "GuideTab.TEMPLE_STATUS",
            "GuideTab.CERTIFICATES",
            "renderTempleStatus(",
            "renderCertificates(",
            "countPlayerItems(",
        ],
    },
    {
        "id": "god_power_guide_civilization_catalog",
        "path": GUIDE_SCREEN,
        "tokens": [
            "static GuideCivilization greek()",
            "static GuideCivilization egyptian()",
            "static GuideCivilization chinese()",
            "static GuideCivilization nordic()",
            "static GuideCivilization atlantean()",
            "SPELLBOOK_TIDAL_SURGE",
            "GOD_POWER_CERTIFICATE_ATLANTEAN",
        ],
    },
    {
        "id": "god_power_guide_network_entrypoint",
        "path": NETWORKING,
        "tokens": [
            "OPEN_GOD_POWER_GUIDE_SCREEN",
            '"open_god_power_guide_screen"',
        ],
    },
    {
        "id": "god_power_guide_client_receiver",
        "path": NETWORK_CLIENT,
        "tokens": [
            "OPEN_GOD_POWER_GUIDE_SCREEN",
            "new GodPowerGuideScreen(civilization, client.player)",
        ],
    },
    {
        "id": "god_power_guide_item_opens_screen_without_replacing_item",
        "path": GUIDE_ITEM,
        "tokens": [
            "ServerPlayNetworking.send",
            "ModNetworking.OPEN_GOD_POWER_GUIDE_SCREEN",
            'return "general";',
        ],
    },
]

LANG_KEYS = [
    "gui.mythic_age.temple_guardian_trade.title",
    "gui.mythic_age.temple_guardian_trade.execute",
    "gui.mythic_age.temple_guardian_trade.close",
    "gui.mythic_age.temple_guardian_trade.previous",
    "gui.mythic_age.temple_guardian_trade.next",
    "gui.mythic_age.temple_guardian_trade.inventory",
    "gui.mythic_age.temple_guardian_trade.cooldown",
    "gui.mythic_age.temple_guardian_trade.status.available",
    "gui.mythic_age.temple_guardian_trade.status.insufficient",
    "gui.mythic_age.temple_guardian_trade.status.cooldown",
    "gui.mythic_age.temple_guardian_trade.status.unavailable",
    "mythic_age.civilization.greek",
    "mythic_age.civilization.egyptian",
    "mythic_age.civilization.chinese",
    "mythic_age.civilization.atlantean",
    "mythic_age.civilization.nordic",
    "gui.mythic_age.god_power_guide.title",
    "gui.mythic_age.god_power_guide.civilization",
    "gui.mythic_age.god_power_guide.general_civilization",
    "gui.mythic_age.god_power_guide.close",
    "gui.mythic_age.god_power_guide.previous_page",
    "gui.mythic_age.god_power_guide.next_page",
    "gui.mythic_age.god_power_guide.tab.overview",
    "gui.mythic_age.god_power_guide.tab.overview.tooltip",
    "gui.mythic_age.god_power_guide.tab.spells",
    "gui.mythic_age.god_power_guide.tab.spells.tooltip",
    "gui.mythic_age.god_power_guide.tab.temple_status",
    "gui.mythic_age.god_power_guide.tab.temple_status.tooltip",
    "gui.mythic_age.god_power_guide.tab.certificates",
    "gui.mythic_age.god_power_guide.tab.certificates.tooltip",
    "gui.mythic_age.god_power_guide.overview.title",
    "gui.mythic_age.god_power_guide.acquisition.title",
    "gui.mythic_age.god_power_guide.spell_count",
    "gui.mythic_age.god_power_guide.spells.title",
    "gui.mythic_age.god_power_guide.cost",
    "gui.mythic_age.god_power_guide.page",
    "gui.mythic_age.god_power_guide.temple_status.title",
    "gui.mythic_age.god_power_guide.temple_status.guardian",
    "gui.mythic_age.god_power_guide.temple_status.certificate_source",
    "gui.mythic_age.god_power_guide.temple_status.trade_screen",
    "gui.mythic_age.god_power_guide.temple_status.statue_or_focus",
    "gui.mythic_age.god_power_guide.certificates.title",
    "gui.mythic_age.god_power_guide.certificates.balance",
    "gui.mythic_age.god_power_guide.certificates.hint",
    "gui.mythic_age.god_power_guide.progress.title",
    "gui.mythic_age.god_power_guide.progress.affordable",
    "gui.mythic_age.god_power_guide.overview.general",
    "gui.mythic_age.god_power_guide.acquisition.general",
    "gui.mythic_age.god_power_guide.overview.greek",
    "gui.mythic_age.god_power_guide.acquisition.greek",
    "gui.mythic_age.god_power_guide.overview.egyptian",
    "gui.mythic_age.god_power_guide.acquisition.egyptian",
    "gui.mythic_age.god_power_guide.overview.chinese",
    "gui.mythic_age.god_power_guide.acquisition.chinese",
    "gui.mythic_age.god_power_guide.overview.nordic",
    "gui.mythic_age.god_power_guide.acquisition.nordic",
    "gui.mythic_age.god_power_guide.overview.atlantean",
    "gui.mythic_age.god_power_guide.acquisition.atlantean",
]


def audit() -> tuple[int, dict[str, Any]]:
    failures: list[str] = []
    contracts: list[dict[str, Any]] = []

    if not GUI_TEXTURE.exists():
        failures.append(f"missing gui texture: {GUI_TEXTURE.relative_to(ROOT)}")

    for contract in UI_CONTRACTS:
        path = contract["path"]
        missing_tokens: list[str] = []
        if not path.exists():
            failures.append(f"{contract['id']}: missing source {path.relative_to(ROOT)}")
            text = ""
        else:
            text = path.read_text(encoding="utf-8")
            missing_tokens = [token for token in contract["tokens"] if token not in text]
            if missing_tokens:
                failures.append(f"{contract['id']}: missing tokens " + "|".join(missing_tokens))
        contracts.append({
            "id": contract["id"],
            "path": str(path.relative_to(ROOT)),
            "missing_tokens": missing_tokens,
            "ok": not missing_tokens and path.exists(),
        })

    lang_results: list[dict[str, Any]] = []
    for lang_file in LANG_FILES:
        if not lang_file.exists():
            missing = LANG_KEYS
            failures.append(f"missing lang file: {lang_file.relative_to(ROOT)}")
            text = ""
        else:
            text = lang_file.read_text(encoding="utf-8")
            missing = [key for key in LANG_KEYS if f'"{key}"' not in text]
            if missing:
                failures.append(f"{lang_file.relative_to(ROOT)} missing lang keys " + "|".join(missing))
        lang_results.append({
            "path": str(lang_file.relative_to(ROOT)),
            "missing_keys": missing,
            "ok": not missing and lang_file.exists(),
        })

    payload = {
        "version": 1,
        "scope": "static_guardian_trade_and_god_power_guide_ui",
        "ok": not failures,
        "failures": failures,
        "texture": {
            "path": str(GUI_TEXTURE.relative_to(ROOT)),
            "exists": GUI_TEXTURE.exists(),
        },
        "contracts": contracts,
        "lang": lang_results,
    }
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return (0 if not failures else 1), payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        code, payload = audit()
        if code == 0:
            print("UI Validation Gate: PASS")
            print(f"report: {REPORT_PATH}")
        else:
            print("UI Validation Gate: FAIL")
            for failure in payload["failures"]:
                print(f"- {failure}")
        return code
    return 0


if __name__ == "__main__":
    sys.exit(main())
