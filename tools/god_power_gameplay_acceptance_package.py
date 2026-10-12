#!/usr/bin/env python3
"""Aggregate 1.0 god-power gameplay acceptance evidence.

The package separates static implementation coverage from real playtest
acceptance. A power can have registered code, UI, cooldowns, feedback and
particles while still requiring a human gameplay pass before 1.0.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont

from god_power_particle_validation_pipeline import GOD_POWER_METHODS, GOD_POWER_SPELL_TYPES


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/god_power_gameplay"
REPORT_PATH = REPORT_DIR / "god_power_gameplay_acceptance_package.json"
MARKDOWN_PATH = ROOT / "docs/系统设计/MythicAgeMod_1.0_神力玩法验收包_2026-06-12.md"
REVIEW_BOARD_DIR = REPORT_DIR / "gameplay_review_boards"
REVIEW_BOARD_CONTACT_SHEET = REPORT_DIR / "gameplay_review_board_contact_sheet.png"
RUNTIME_GOD_POWER_PLAYTEST_REPORT = ROOT / "run/visual_probe/god_power_playtest_report.json"

SPELLBOOK = ROOT / "src/main/java/com/example/mythic_age/item/SpellbookItem.java"
MOD_ITEMS = ROOT / "src/main/java/com/example/mythic_age/item/ModItems.java"
GUIDE_SCREEN = ROOT / "src/main/java/com/example/mythic_age/client/gui/GodPowerGuideScreen.java"
LANG_DIR = ROOT / "src/main/resources/assets/mythic_age/lang"
PARTICLE_REPORT = ROOT / "docs/preview_assets/particle_validation/god_power_particle_report.json"
UI_REPORT = ROOT / "docs/preview_assets/ui_validation/ui_report.json"

PRIMARY_LANG = "zh_cn.json"
FEEDBACK_KEYS = [
    "item.mythic_age.spellbook.on_cooldown",
    "item.mythic_age.spellbook.cast_success",
    "item.mythic_age.spellbook.cast_failed",
]
TOOLTIP_KEYS = [
    "mythic_age.spellbook.cooldown",
    "mythic_age.spellbook.type.offensive",
    "mythic_age.spellbook.type.healing",
    "mythic_age.spellbook.type.utility",
    "mythic_age.spellbook.type.summoning",
    "mythic_age.spellbook.type.debuff",
    "mythic_age.spellbook.type.buff",
    "mythic_age.spellbook.type.transportation",
    "mythic_age.spellbook.type.resource",
    "mythic_age.spellbook.type.unknown",
]

WORLD_EDIT_OPERATION_PATTERNS = [
    ("set_block_state", "setBlockState"),
    ("break_block", "breakBlock"),
    ("create_explosion", "createExplosion"),
    ("explosion_entity_or_damage", "Explosion"),
    ("earthquake_manager", "EarthquakeManager"),
    ("meteor_entity", "Meteor"),
]

PLAYTEST_DIMENSION_CRITERIA = {
    "target_selection": [
        "分别测试准星命中、空地、近距、远距、实体和无效目标。",
        "失败目标必须给出清楚反馈，不能误扣冷却或消耗。",
    ],
    "cooldown_cost_balance": [
        "记录冷却、证明消耗、范围、伤害/治疗/召唤数量和持续时间。",
        "同文明同类型神力之间不能出现明显成本和强度倒挂。",
    ],
    "feedback_readability": [
        "粒子启动、持续、命中/结束阶段必须可读。",
        "音效和提示必须说明成功/失败原因，不能遮挡战斗判断。",
    ],
    "effect_scope_and_cleanup": [
        "确认持续效果、召唤物、天气、传送、资源生成或世界编辑会按预期结束。",
        "离开区块、退出世界并重进后不应残留异常实体、任务或方块状态。",
    ],
    "failure_path": [
        "冷却中、证明不足、无目标、不合适地形、无敌/友方/空世界都必须测试。",
        "失败路径不能造成实际效果、错误冷却或错误资源消耗。",
    ],
    "world_edit_safety": [
        "方块修改、爆炸、陨石、地震和天气必须确认范围、性能和存档安全。",
        "不得破坏基岩、传送门、核心结构或其他不可破坏目标。",
    ],
}

PLAYTEST_BATCH_SIZE_BY_RISK = {
    "high": 6,
    "medium": 8,
    "low": 10,
}

RISK_PRIORITY = {
    "high": 1,
    "medium": 2,
    "low": 3,
}

RESAMPLE_LANCZOS = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
FONT_PATHS = (
    "/System/Library/Fonts/STHeiti Light.ttc",
    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
)

CIVILIZATION_COLORS = {
    "greek": (93, 116, 176, 255),
    "egyptian": (181, 132, 54, 255),
    "nordic": (82, 132, 139, 255),
    "chinese": (173, 78, 64, 255),
    "atlantean": (93, 145, 128, 255),
}

CHECK_COLORS = {
    "ok": (63, 126, 82, 255),
    "pending": (176, 112, 45, 255),
    "risk": (158, 64, 58, 255),
    "panel": (247, 244, 235, 255),
    "panel_shadow": (221, 214, 196, 255),
    "ink": (35, 31, 26, 255),
    "muted": (97, 89, 76, 255),
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def runtime_smoke_playtest_status(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {
            "ok": False,
            "path": project_path(path),
            "present": False,
            "total": 0,
            "passed": 0,
            "failed": 0,
            "failed_spells": [],
            "checks_by_spell": {},
        }
    report = load_json(path, {})
    checks_by_spell: dict[str, dict[str, Any]] = {}
    for check in report.get("checks", []):
        if not isinstance(check, dict) or not isinstance(check.get("spell_type"), str):
            continue
        spell_type = check["spell_type"]
        checks_by_spell[spell_type] = {
            "present": True,
            "ok": bool(check.get("ok")),
            "civilization": check.get("civilization"),
            "item_id": check.get("item_id"),
            "action_result": check.get("action_result"),
            "cooldown_set": check.get("cooldown_set"),
            "spawned_entities": check.get("spawned_entities"),
            "duration_ms": check.get("duration_ms"),
            "error": check.get("error", ""),
        }
    failed_spells = [
        check.get("spell_type")
        for check in report.get("checks", [])
        if isinstance(check, dict) and not check.get("ok")
    ]
    return {
        "ok": bool(report.get("failed", 1) == 0 and report.get("total") == report.get("passed")),
        "path": project_path(path),
        "present": True,
        "scope": report.get("scope"),
        "process_id": report.get("process_id"),
        "total": report.get("total", 0),
        "passed": report.get("passed", 0),
        "failed": report.get("failed", 0),
        "failed_spells": failed_spells,
        "checks_by_spell": checks_by_spell,
    }


def load_font(size: int) -> ImageFont.ImageFont:
    for path in FONT_PATHS:
        if Path(path).exists():
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                pass
    return ImageFont.load_default()


def wrap_text(text: str, font: ImageFont.ImageFont, max_width: int) -> list[str]:
    lines: list[str] = []
    current = ""
    for char in text:
        candidate = current + char
        if font.getbbox(candidate)[2] <= max_width or not current:
            current = candidate
        else:
            lines.append(current)
            current = char
    if current:
        lines.append(current)
    return lines


def draw_wrapped(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, font: ImageFont.ImageFont,
                 fill: tuple[int, int, int, int], max_width: int, line_height: int,
                 max_lines: int | None = None) -> int:
    x, y = xy
    lines = wrap_text(text, font, max_width)
    if max_lines is not None and len(lines) > max_lines:
        lines = lines[:max_lines]
        lines[-1] = lines[-1].rstrip("。,.，") + "..."
    for line in lines:
        draw.text((x, y), line, fill=fill, font=font)
        y += line_height
    return y


def gameplay_review_board_path(power: dict[str, Any]) -> Path:
    return REVIEW_BOARD_DIR / str(power["civilization"]) / f"{str(power['spell_type']).lower()}_gameplay_review.png"


def bool_badge_text(value: Any) -> str:
    return "OK" if bool(value) else "MISSING"


def method_signature(text: str, method_name: str) -> re.Match[str] | None:
    return re.search(
        rf"(?:private|public|protected)\s+([\w<>\[\], ?]+)\s+{re.escape(method_name)}\s*\(",
        text,
    )


def method_body(text: str, method_name: str) -> str:
    match = method_signature(text, method_name)
    if not match:
        return ""
    start = text.find("{", match.end())
    if start < 0:
        return ""
    depth = 0
    for index in range(start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[start:index + 1]
    return ""


def method_return_type(text: str, method_name: str) -> str | None:
    match = method_signature(text, method_name)
    if not match:
        return None
    return " ".join(match.group(1).split())


def spell_switch_case_body(text: str, spell_type: str) -> str:
    match = re.search(rf"case\s+{re.escape(spell_type)}\s*:", text)
    if not match:
        return ""
    next_case = re.search(r"\n\s*(case\s+[A-Z0-9_]+\s*:|default\s*:)", text[match.end():])
    end = match.end() + next_case.start() if next_case else len(text)
    return text[match.start():end]


def load_lang_files() -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for path in sorted(LANG_DIR.glob("*.json")):
        try:
            result[path.name] = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            result[path.name] = {"__parse_error__": str(exc)}
    return result


def parse_spellbook_registrations(text: str) -> dict[str, dict[str, Any]]:
    pattern = re.compile(
        r"public\s+static\s+final\s+Item\s+(?P<const>SPELLBOOK_[A-Z0-9_]+)\s*=\s*"
        r"registerItem\(\"(?P<item_id>spellbook_[a-z0-9_]+)\",\s*"
        r"new\s+SpellbookItem\((?P<body>.*?)SpellbookItem\.SpellType\.(?P<spell>[A-Z0-9_]+)\)\);",
        re.S,
    )
    registrations: dict[str, dict[str, Any]] = {}
    for match in pattern.finditer(text):
        raw = match.group(0)
        cooldown_match = re.search(r"\b(\d+)\s*,\s*SpellbookItem\.SpellType\.", raw)
        cooldown_ticks = int(cooldown_match.group(1)) if cooldown_match else None
        spell_type = match.group("spell")
        registrations[spell_type] = {
            "constant": match.group("const"),
            "item_id": match.group("item_id"),
            "cooldown_ticks": cooldown_ticks,
            "cooldown_seconds": round(cooldown_ticks / 20.0, 2) if cooldown_ticks is not None else None,
        }
    return registrations


def parse_guide_costs(text: str) -> dict[str, int]:
    costs: dict[str, int] = {}
    for item_constant, cost in re.findall(r"new\s+GuideSpell\(ModItems\.(SPELLBOOK_[A-Z0-9_]+),\s*(\d+)\)", text):
        costs.setdefault(item_constant, int(cost))
    return costs


def particle_matrix_by_spell(report: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        item.get("spell_type"): item
        for item in report.get("god_power_particle_matrix", [])
        if isinstance(item, dict) and isinstance(item.get("spell_type"), str)
    }


def global_use_contract(spellbook_text: str) -> dict[str, Any]:
    use_body = method_body(spellbook_text, "use")
    sound_body = method_body(spellbook_text, "playSpellCastSound")
    return {
        "server_entry_guard_ok": "if (!world.isClient)" in use_body and "castGodPower(world, user, spellType)" in use_body,
        "cooldown_check_ok": "isCoolingDown(this)" in use_body,
        "cooldown_apply_ok": "getItemCooldownManager().set(this, this.cooldownTicks)" in use_body,
        "success_feedback_ok": 'Text.translatable("item.mythic_age.spellbook.cast_success"' in use_body,
        "failure_feedback_ok": 'Text.translatable("item.mythic_age.spellbook.cast_failed"' in use_body,
        "cooldown_feedback_ok": 'Text.translatable("item.mythic_age.spellbook.on_cooldown"' in use_body,
        "global_success_sound_ok": "playSpellCastSound(world, user.getBlockPos())" in use_body and "world.playSound" in sound_body,
    }


def localization_status(spell_type: str, item_id: str | None, lang_files: dict[str, dict[str, Any]]) -> dict[str, Any]:
    lower = spell_type.lower()
    spell_key = f"spell.{lower}"
    description_key = f"mythic_age.spellbook.{lower}.description"
    item_key = f"item.mythic_age.{item_id}" if item_id else None
    per_lang: dict[str, Any] = {}
    for lang_name, data in lang_files.items():
        if "__parse_error__" in data:
            per_lang[lang_name] = {"parse_ok": False, "missing": ["json_parse_error"], "ok": False}
            continue
        required = [spell_key, description_key, *FEEDBACK_KEYS, *TOOLTIP_KEYS]
        if item_key:
            required.append(item_key)
        missing = [key for key in required if key not in data]
        per_lang[lang_name] = {
            "parse_ok": True,
            "missing": missing,
            "ok": not missing,
        }
    primary = per_lang.get(PRIMARY_LANG, {"ok": False, "missing": ["primary_lang_file_missing"]})
    return {
        "primary_lang": PRIMARY_LANG,
        "primary_lang_ok": bool(primary.get("ok")),
        "primary_lang_missing": primary.get("missing", []),
        "all_lang_ok": all(item.get("ok") for item in per_lang.values()) if per_lang else False,
        "per_lang": per_lang,
        "keys": {
            "item": item_key,
            "spell": spell_key,
            "description": description_key,
        },
    }


def runtime_message_keys(text: str) -> list[str]:
    short_keys = set(re.findall(r'sendSpellMessage\s*\([^,]+,\s*"([^"]+)"', text))
    short_keys.update(re.findall(r'spellMessageText\s*\(\s*"([^"]+)"', text))
    return sorted(f"mythic_age.spellbook.message.{key}" for key in short_keys)


def runtime_message_localization_status(message_keys: list[str], lang_files: dict[str, dict[str, Any]]) -> dict[str, Any]:
    per_lang: dict[str, Any] = {}
    for lang_name, data in lang_files.items():
        if "__parse_error__" in data:
            per_lang[lang_name] = {"parse_ok": False, "missing": ["json_parse_error"], "ok": False}
            continue
        missing = [key for key in message_keys if key not in data]
        per_lang[lang_name] = {
            "parse_ok": True,
            "missing": missing,
            "ok": not missing,
        }
    primary = per_lang.get(PRIMARY_LANG, {"ok": False, "missing": ["primary_lang_file_missing"]})
    return {
        "key_count": len(message_keys),
        "keys": message_keys,
        "primary_lang": PRIMARY_LANG,
        "primary_lang_ok": bool(primary.get("ok")),
        "primary_lang_missing": primary.get("missing", []),
        "all_lang_ok": all(item.get("ok") for item in per_lang.values()) if per_lang else False,
        "per_lang": per_lang,
    }


def infer_targeting(switch_body: str, combined_body: str) -> str:
    body = switch_body + "\n" + combined_body
    if "getTargetPos(world, player)" in body:
        return "target_block_or_crosshair"
    if any(token in body for token in ["getEntitiesByClass", "getOtherEntities", "Box("]):
        return "caster_centered_area"
    if "player.getBlockPos()" in body:
        return "caster_position"
    return "unknown_or_internal"


def infer_effect_scope(combined_body: str) -> list[str]:
    scopes: list[str] = []
    checks = [
        ("damage", ["damage(", "DamageSource", "strikeLightning"]),
        ("buff_or_debuff", ["StatusEffectInstance", "addStatusEffect"]),
        ("summon_or_spawn", ["spawnEntity", "new SkeletonEntity", "new WolfEntity", "new WalkingTreeEntity"]),
        ("world_edit", ["setBlockState", "breakBlock", "removeBlock", "generate", "placeStructure"]),
        ("teleport_or_mobility", ["requestTeleport", "teleport", "setVelocity"]),
        ("weather_or_persistent_manager", ["LightningStormManager", "EarthquakeManager", "GodPowerWeatherManager"]),
        ("resource_generation", ["dropStack", "ItemEntity", "Items.", "Blocks."]),
    ]
    for scope, tokens in checks:
        if any(token in combined_body for token in tokens):
            scopes.append(scope)
    return scopes or ["unknown"]


def world_edit_safety_review(switch_body: str, combined_body: str) -> dict[str, Any]:
    body = switch_body + "\n" + combined_body
    operations = [
        name
        for name, token in WORLD_EDIT_OPERATION_PATTERNS
        if token in body
    ]
    requires_manual_review = bool(operations)
    safeguards = {
        "server_side_guard_present": "world.isClient" in body and "return false" in body,
        "thread_free": "new Thread" not in body and "Thread.sleep" not in body,
        "tick_scheduler_used": "GodPowerTaskScheduler" in body,
        "persistent_manager_used": any(token in body for token in [
            "EarthquakeManager",
            "GodPowerWeatherManager",
            "LightningStormManager",
        ]),
        "preflight_space_or_bounds_check": "canPlace" in body or "getBottomY()" in body or "getTopY()" in body,
        "hard_block_protection": any(token in body for token in [
            "Blocks.BEDROCK",
            "Blocks.OBSIDIAN",
            "Blocks.BARRIER",
            "END_PORTAL_FRAME",
        ]),
        "bounded_area_indicators": "expand(" in body or "Box(" in body or "for (int" in body,
    }
    if not requires_manual_review:
        status = "not_world_edit_or_explosion"
    elif safeguards["thread_free"] and (safeguards["server_side_guard_present"] or "castGodPower" in switch_body):
        status = "static_safety_evidence_recorded_pending_manual_review"
    else:
        status = "static_safety_gap_pending_code_review"
    return {
        "requires_manual_safety_review": requires_manual_review,
        "status": status,
        "operations": operations,
        "safeguards": safeguards,
        "manual_review_focus": [
            "确认范围、持续时间和方块数量不会造成卡顿或存档膨胀。",
            "确认不会破坏基岩、传送门、神殿核心、祭坛核心或其他不应破坏的结构。",
            "确认爆炸、陨石、地震、天气和延迟任务在卸载区块、退出世界、重进世界后不会残留异常。",
        ] if requires_manual_review else [],
    }


def risk_flags(
    return_types: dict[str, str | None],
    switch_body: str,
    combined_body: str,
    localization: dict[str, Any],
    runtime_messages: dict[str, Any],
) -> list[str]:
    flags: list[str] = []
    primary_method_return = next((value for value in return_types.values() if value), None)
    if primary_method_return == "void" and "return true;" in switch_body:
        flags.append("void_cast_always_success")
    if "return false" not in (switch_body + combined_body):
        flags.append("no_explicit_failure_path")
    if "Text.literal" in combined_body or "net.minecraft.text.Text.literal" in combined_body:
        flags.append("literal_player_messages")
    if "new Thread" in combined_body:
        flags.append("threaded_delayed_effect")
    if any(token in combined_body for token in ["createExplosion", "Explosion", "setBlockState", "breakBlock", "EarthquakeManager", "Meteor"]):
        flags.append("large_world_edit_or_explosion")
    if not localization["primary_lang_ok"]:
        flags.append("primary_language_missing_runtime_text")
    if not localization["all_lang_ok"]:
        flags.append("supported_language_release_gap")
    if not runtime_messages["primary_lang_ok"]:
        flags.append("primary_runtime_message_missing")
    if not runtime_messages["all_lang_ok"]:
        flags.append("supported_language_runtime_message_gap")
    return flags


def risk_level(flags: list[str], safety_review: dict[str, Any], effect_scope: list[str], runtime_smoke: dict[str, Any]) -> str:
    high_risk_flags = {
        "large_world_edit_or_explosion",
        "threaded_delayed_effect",
        "void_cast_always_success",
        "no_explicit_failure_path",
    }
    medium_scopes = {
        "summon_or_spawn",
        "weather_or_persistent_manager",
        "teleport_or_mobility",
        "resource_generation",
    }
    if not runtime_smoke.get("ok", False):
        return "high"
    if high_risk_flags.intersection(flags):
        return "high"
    if safety_review.get("requires_manual_safety_review"):
        return "high"
    if medium_scopes.intersection(effect_scope):
        return "medium"
    return "low"


def playtest_dimensions(
    targeting: str,
    effect_scope: list[str],
    localization: dict[str, Any],
    runtime_messages: dict[str, Any],
    flags: list[str],
    safety_review: dict[str, Any],
    runtime_smoke: dict[str, Any],
) -> list[dict[str, Any]]:
    dimensions = [
        {
            "id": "runtime_smoke_cast",
            "status": "auto_passed" if runtime_smoke.get("ok") else "auto_failed",
            "automated": True,
            "focus": "受控 MC 探针能否完成一次服务端施放、设置冷却且无异常。",
            "evidence": {
                "ok": runtime_smoke.get("ok", False),
                "cooldown_set": runtime_smoke.get("cooldown_set"),
                "action_result": runtime_smoke.get("action_result"),
                "duration_ms": runtime_smoke.get("duration_ms"),
                "spawned_entities": runtime_smoke.get("spawned_entities"),
                "error": runtime_smoke.get("error", ""),
            },
        },
        {
            "id": "target_selection",
            "status": "manual_required",
            "automated": False,
            "focus": f"目标模式 `{targeting}` 在准星、空地、实体、远近距离和失败目标下是否直观。",
        },
        {
            "id": "cooldown_cost_balance",
            "status": "manual_required",
            "automated": False,
            "focus": "冷却、证明消耗、影响范围、持续时间、召唤数量或伤害强度是否与同文明神力和战斗节奏匹配。",
        },
        {
            "id": "feedback_readability",
            "status": "manual_required",
            "automated": False,
            "focus": "成功/失败提示、音效、粒子启动/持续/结束阶段是否可读，是否遮挡战斗或误导玩家。",
        },
        {
            "id": "localization_and_ui",
            "status": "auto_passed_pending_visual_check"
            if localization.get("all_lang_ok") and runtime_messages.get("all_lang_ok")
            else "auto_failed",
            "automated": True,
            "focus": "图鉴、tooltip、成功/失败提示在支持语言中是否完整，并在 UI 宽度内可读。",
            "evidence": {
                "tooltip_all_lang_ok": localization.get("all_lang_ok"),
                "runtime_message_all_lang_ok": runtime_messages.get("all_lang_ok"),
            },
        },
        {
            "id": "effect_scope_and_cleanup",
            "status": "manual_required",
            "automated": False,
            "focus": "效果范围、召唤物、天气/持续管理器、传送或资源生成是否按预期结束，离开区块和重进世界后不残留异常。",
            "effect_scope": effect_scope,
        },
        {
            "id": "failure_path",
            "status": "manual_required_high_focus" if "no_explicit_failure_path" in flags or "void_cast_always_success" in flags else "manual_required",
            "automated": False,
            "focus": "无目标、冷却中、证明不足、不适合地形、无敌/友方/空世界等情况下是否给出正确失败反馈。",
        },
        {
            "id": "world_edit_safety",
            "status": "manual_required_high_focus" if safety_review.get("requires_manual_safety_review") else "not_applicable",
            "automated": False,
            "focus": "爆炸、方块修改、陨石、地震、天气和延迟任务是否有边界、性能和存档安全保障。",
            "operations": safety_review.get("operations", []),
        },
    ]
    return dimensions


def manual_playtest_focus(dimensions: list[dict[str, Any]], flags: list[str], safety_review: dict[str, Any]) -> list[str]:
    focus = [
        str(item["focus"])
        for item in dimensions
        if str(item.get("status", "")).startswith("manual_required")
    ]
    if flags:
        focus.insert(0, "优先复核风险标记：" + ", ".join(flags))
    if safety_review.get("manual_review_focus"):
        focus.extend(str(item) for item in safety_review["manual_review_focus"])
    return focus


def playtest_task_priority(power: dict[str, Any]) -> int:
    risk_level_value = str(power.get("playtest_risk_level", "low"))
    priority = RISK_PRIORITY.get(risk_level_value, 4)
    if power.get("world_edit_safety", {}).get("requires_manual_safety_review"):
        priority = min(priority, 1)
    if "void_cast_always_success" in power.get("risk_flags", []):
        priority = min(priority, 1)
    return priority


def minimum_playtest_scenarios(power: dict[str, Any]) -> int:
    scenario_count = 5
    risk_level_value = str(power.get("playtest_risk_level", "low"))
    if risk_level_value == "high":
        scenario_count += 4
    elif risk_level_value == "medium":
        scenario_count += 2
    if power.get("world_edit_safety", {}).get("requires_manual_safety_review"):
        scenario_count += 3
    if "teleport_or_mobility" in power.get("routing", {}).get("effect_scope", []):
        scenario_count += 2
    return scenario_count


def acceptance_criteria_for_power(power: dict[str, Any]) -> list[str]:
    criteria: list[str] = []
    for dimension in power.get("playtest_dimensions", []):
        dimension_id = str(dimension.get("id", ""))
        if str(dimension.get("status", "")).startswith("manual_required"):
            criteria.extend(PLAYTEST_DIMENSION_CRITERIA.get(dimension_id, []))
    seen = set()
    unique = []
    for item in criteria:
        if item not in seen:
            unique.append(item)
            seen.add(item)
    return unique[:10]


def build_manual_playtest_queue(powers: list[dict[str, Any]]) -> list[dict[str, Any]]:
    tasks = []
    for power in powers:
        task = {
            "priority": playtest_task_priority(power),
            "civilization": power["civilization"],
            "spell_type": power["spell_type"],
            "risk_level": power.get("playtest_risk_level"),
            "risk_flags": power.get("risk_flags", []),
            "effect_scope": power.get("routing", {}).get("effect_scope", []),
            "targeting": power.get("routing", {}).get("targeting"),
            "world_edit_operations": power.get("world_edit_safety", {}).get("operations", []),
            "minimum_scenarios": minimum_playtest_scenarios(power),
            "manual_focus": power.get("manual_playtest_focus", [])[:6],
            "acceptance_criteria": acceptance_criteria_for_power(power),
            "runtime_smoke_probe_ok": bool(power.get("runtime_smoke_probe", {}).get("ok")),
            "review_board": power.get("gameplay_review_board", ""),
        }
        tasks.append(task)
    return sorted(
        tasks,
        key=lambda item: (
            item["priority"],
            item["civilization"],
            item["spell_type"],
        ),
    )


def build_manual_playtest_batches(tasks: list[dict[str, Any]]) -> list[dict[str, Any]]:
    batches = []
    grouped: dict[tuple[int, str], list[dict[str, Any]]] = {}
    for task in tasks:
        grouped.setdefault((int(task["priority"]), str(task["risk_level"])), []).append(task)
    for (priority, risk_level_value), group_tasks in sorted(grouped.items()):
        batch_size = PLAYTEST_BATCH_SIZE_BY_RISK.get(risk_level_value, 8)
        for index in range(0, len(group_tasks), batch_size):
            chunk = group_tasks[index:index + batch_size]
            batch_index = index // batch_size + 1
            batches.append({
                "batch_id": f"P{priority}-{risk_level_value}-{batch_index}",
                "priority": priority,
                "risk_level": risk_level_value,
                "task_count": len(chunk),
                "spells": [f"{task['civilization']}:{task['spell_type']}" for task in chunk],
                "total_minimum_scenarios": sum(int(task["minimum_scenarios"]) for task in chunk),
                "world_edit_task_count": sum(1 for task in chunk if task["world_edit_operations"]),
                "acceptance_focus": [
                    "目标选择和失败路径必须覆盖。",
                    "粒子/音效/UI 反馈必须可读。",
                    "高风险批次必须额外记录性能、存档和清理结果。",
                ] if priority == 1 else [
                    "确认手感、平衡和反馈是否符合设计。",
                    "记录任何遮挡、误导或冷却/成本异常。",
                ],
            })
    return batches


def summarize_power(
    civilization: str,
    spell_type: str,
    spellbook_text: str,
    registrations: dict[str, dict[str, Any]],
    guide_costs: dict[str, int],
    particle_by_spell: dict[str, dict[str, Any]],
    lang_files: dict[str, dict[str, Any]],
    global_contract: dict[str, Any],
    runtime_smoke_by_spell: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    cast_body = method_body(spellbook_text, "castGodPower")
    switch_body = spell_switch_case_body(cast_body, spell_type)
    methods = GOD_POWER_METHODS.get(spell_type, [])
    method_bodies = {method: method_body(spellbook_text, method) for method in methods}
    return_types = {method: method_return_type(spellbook_text, method) for method in methods}
    combined_body = "\n".join(body for body in method_bodies.values() if body)
    missing_methods = [method for method, body in method_bodies.items() if not body]
    registration = registrations.get(spell_type, {})
    item_constant = registration.get("constant")
    guide_cost = guide_costs.get(item_constant) if item_constant else None
    particle = particle_by_spell.get(spell_type, {})
    localization = localization_status(spell_type, registration.get("item_id"), lang_files)
    runtime_messages = runtime_message_localization_status(runtime_message_keys(combined_body), lang_files)

    registered_ok = bool(registration)
    route_ok = bool(switch_body) and any(f"{method}(" in switch_body for method in methods)
    guide_cost_ok = guide_cost is not None
    particle_ok = bool(particle.get("ok"))
    global_ok = all(global_contract.values())
    static_contract_ok = (
        registered_ok
        and route_ok
        and not missing_methods
        and guide_cost_ok
        and particle_ok
        and localization["primary_lang_ok"]
        and runtime_messages["primary_lang_ok"]
        and global_ok
    )
    flags = risk_flags(return_types, switch_body, combined_body, localization, runtime_messages)
    safety_review = world_edit_safety_review(switch_body, combined_body)
    targeting = infer_targeting(switch_body, combined_body)
    effect_scope = infer_effect_scope(combined_body)
    runtime_smoke = runtime_smoke_by_spell.get(spell_type, {
        "present": False,
        "ok": False,
        "error": "missing_runtime_smoke_probe_result",
    })
    dimensions = playtest_dimensions(
        targeting,
        effect_scope,
        localization,
        runtime_messages,
        flags,
        safety_review,
        runtime_smoke,
    )
    level = risk_level(flags, safety_review, effect_scope, runtime_smoke)
    return {
        "civilization": civilization,
        "spell_type": spell_type,
        "status": "static_gameplay_contract_passed_pending_playtest" if static_contract_ok else "static_gameplay_contract_failed",
        "static_contract_ok": static_contract_ok,
        "human_playtest_acceptance": "pending",
        "manual_playtest_required": True,
        "registration": {
            "registered_ok": registered_ok,
            **registration,
        },
        "guide": {
            "listed_in_god_power_guide": guide_cost_ok,
            "certificate_cost": guide_cost,
        },
        "routing": {
            "route_ok": route_ok,
            "methods": methods,
            "missing_methods": missing_methods,
            "method_return_types": return_types,
            "targeting": targeting,
            "effect_scope": effect_scope,
        },
        "global_use_contract": global_contract,
        "particles": {
            "particle_matrix_ok": particle_ok,
            "particle_helper_call_count": particle.get("particle_helper_call_count"),
            "source_report_status": particle.get("ok"),
        },
        "localization": localization,
        "runtime_messages": runtime_messages,
        "risk_flags": flags,
        "playtest_risk_level": level,
        "runtime_smoke_probe": runtime_smoke,
        "playtest_dimensions": dimensions,
        "manual_playtest_focus": manual_playtest_focus(dimensions, flags, safety_review),
        "world_edit_safety": safety_review,
        "review_questions": [
            "施法目标选择是否直观，成功/失败反馈是否符合玩家预期？",
            "冷却、证明消耗、范围、持续时间和强度是否与同文明其他神力平衡？",
            "粒子和音效是否让启动、持续、命中/结束阶段可读，而不是遮挡战斗？",
            "服务端执行、延迟效果、世界编辑和召唤物是否不会造成存档或性能风险？",
            "UI 名称、tooltip、成功/失败提示是否在目标语言下完整显示？",
        ],
        "next_action": "manual_playtest_and_balance_review",
    }


def draw_badge(draw: ImageDraw.ImageDraw, xy: tuple[int, int], label: str,
               fill: tuple[int, int, int, int], font: ImageFont.ImageFont) -> None:
    x, y = xy
    text_box = draw.textbbox((0, 0), label, font=font)
    width = text_box[2] - text_box[0] + 22
    height = 28
    draw.rounded_rectangle((x, y, x + width, y + height), radius=6, fill=fill)
    draw.text((x + 11, y + 5), label, fill=(255, 252, 244, 255), font=font)


def draw_panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str,
               title_font: ImageFont.ImageFont) -> None:
    x1, y1, x2, y2 = box
    draw.rounded_rectangle((x1 + 4, y1 + 4, x2 + 4, y2 + 4), radius=8, fill=CHECK_COLORS["panel_shadow"])
    draw.rounded_rectangle(box, radius=8, fill=CHECK_COLORS["panel"])
    draw.text((x1 + 20, y1 + 16), title, fill=CHECK_COLORS["ink"], font=title_font)


def draw_key_value(draw: ImageDraw.ImageDraw, x: int, y: int, key: str, value: str,
                   key_font: ImageFont.ImageFont, value_font: ImageFont.ImageFont,
                   max_width: int) -> int:
    draw.text((x, y), key, fill=CHECK_COLORS["muted"], font=key_font)
    return draw_wrapped(draw, (x + 138, y), value, value_font, CHECK_COLORS["ink"], max_width - 138, 22, 3) + 8


def draw_check_row(draw: ImageDraw.ImageDraw, x: int, y: int, label: str, ok: bool,
                   font: ImageFont.ImageFont) -> int:
    color = CHECK_COLORS["ok"] if ok else CHECK_COLORS["risk"]
    draw.ellipse((x, y + 3, x + 16, y + 19), fill=color)
    draw.text((x + 26, y), f"{label}: {bool_badge_text(ok)}", fill=CHECK_COLORS["ink"], font=font)
    return y + 26


def draw_god_power_review_board(power: dict[str, Any]) -> None:
    out = gameplay_review_board_path(power)
    out.parent.mkdir(parents=True, exist_ok=True)
    width, height = 1280, 1040
    bg = (239, 235, 223, 255)
    image = Image.new("RGBA", (width, height), bg)
    draw = ImageDraw.Draw(image)
    title_font = load_font(42)
    h2_font = load_font(24)
    body_font = load_font(20)
    small_font = load_font(16)
    civilization = str(power["civilization"])
    civ_color = CIVILIZATION_COLORS.get(civilization, (92, 92, 92, 255))
    risk_flags = power["risk_flags"]
    risk_level_value = power.get("playtest_risk_level", "unknown")
    safety = power["world_edit_safety"]
    registration = power["registration"]
    routing = power["routing"]
    particles = power["particles"]
    guide = power["guide"]
    localization = power["localization"]
    runtime_messages = power["runtime_messages"]

    draw.rectangle((0, 0, width, 116), fill=civ_color)
    draw.text((48, 34), str(power["spell_type"]), fill=(255, 252, 244, 255), font=title_font)
    draw.text((48, 80), f"{civilization} god power gameplay review", fill=(245, 239, 225, 255), font=body_font)
    draw_badge(draw, (860, 34), "STATIC PASS" if power["static_contract_ok"] else "STATIC GAP",
               CHECK_COLORS["ok"] if power["static_contract_ok"] else CHECK_COLORS["risk"], small_font)
    risk_badge_color = CHECK_COLORS["risk"] if risk_level_value == "high" else CHECK_COLORS["pending"]
    draw_badge(draw, (1008, 34), f"{risk_level_value.upper()} RISK", risk_badge_color, small_font)

    draw_panel(draw, (48, 146, 608, 554), "Static Contract", h2_font)
    y = 198
    y = draw_key_value(draw, 72, y, "Item", str(registration.get("item_id")), small_font, body_font, 500)
    y = draw_key_value(draw, 72, y, "Cooldown", f"{registration.get('cooldown_seconds')} s", small_font, body_font, 500)
    y = draw_key_value(draw, 72, y, "Certificate", str(guide.get("certificate_cost")), small_font, body_font, 500)
    y = draw_key_value(draw, 72, y, "Targeting", str(routing.get("targeting")), small_font, body_font, 500)
    y = draw_key_value(draw, 72, y, "Effect", ", ".join(routing.get("effect_scope", [])), small_font, body_font, 500)
    y = draw_key_value(draw, 72, y, "Methods", ", ".join(routing.get("methods", [])), small_font, body_font, 500)
    y += 4
    checks = [
        ("registered", registration.get("registered_ok")),
        ("guide listed", guide.get("listed_in_god_power_guide")),
        ("route", routing.get("route_ok")),
        ("particles", particles.get("particle_matrix_ok")),
        ("primary text", localization.get("primary_lang_ok")),
        ("runtime text", runtime_messages.get("primary_lang_ok")),
    ]
    for label, ok in checks:
        y = draw_check_row(draw, 78, y, label, bool(ok), small_font)

    draw_panel(draw, (648, 146, 1232, 554), "Risk And Safety", h2_font)
    y = 198
    risk_text = ", ".join(risk_flags) if risk_flags else "none"
    y = draw_key_value(draw, 672, y, "Risk flags", risk_text, small_font, body_font, 520)
    y = draw_key_value(draw, 672, y, "Risk level", str(risk_level_value), small_font, body_font, 520)
    runtime_probe = power.get("runtime_smoke_probe", {})
    y = draw_key_value(draw, 672, y, "Runtime probe", f"ok={runtime_probe.get('ok')} duration={runtime_probe.get('duration_ms')}ms", small_font, body_font, 520)
    y = draw_key_value(draw, 672, y, "World edit", str(safety.get("requires_manual_safety_review")), small_font, body_font, 520)
    y = draw_key_value(draw, 672, y, "Safety state", str(safety.get("status")), small_font, body_font, 520)
    operations = ", ".join(safety.get("operations", [])) or "none"
    y = draw_key_value(draw, 672, y, "Operations", operations, small_font, body_font, 520)
    y += 6
    if safety.get("requires_manual_safety_review"):
        for label, ok in safety.get("safeguards", {}).items():
            y = draw_check_row(draw, 678, y, label, bool(ok), small_font)
            if y > 520:
                break
    else:
        draw_wrapped(
            draw,
            (678, y),
            "No destructive world-edit or explosion operation was detected by the static source scan.",
            body_font,
            CHECK_COLORS["ink"],
            510,
            26,
            3,
        )

    draw_panel(draw, (48, 590, 608, 960), "Manual Playtest Checklist", h2_font)
    y = 642
    for dimension in power.get("playtest_dimensions", []):
        if not str(dimension.get("status", "")).startswith("manual_required"):
            continue
        y = draw_wrapped(draw, (74, y), "- " + str(dimension.get("focus")), body_font, CHECK_COLORS["ink"], 500, 23, 3)
        y += 7
        if y > 928:
            break

    draw_panel(draw, (648, 590, 1232, 960), "Acceptance Boundary", h2_font)
    y = 642
    boundary = [
        "This board is static evidence only.",
        "It does not prove combat feel, balance, failure paths, performance, or in-game readability.",
        "Next action: manual playtest and balance review.",
    ]
    for line in boundary:
        y = draw_wrapped(draw, (674, y), line, body_font, CHECK_COLORS["ink"], 520, 28, 4)
        y += 10
    if safety.get("manual_review_focus"):
        y += 4
        draw.text((674, y), "World-edit focus", fill=CHECK_COLORS["muted"], font=small_font)
        y += 26
        for focus in safety["manual_review_focus"]:
            y = draw_wrapped(draw, (674, y), "- " + focus, small_font, CHECK_COLORS["ink"], 520, 20, 2)
            y += 4
            if y > 932:
                break

    image.convert("RGB").save(out)


def draw_god_power_review_boards(payload: dict[str, Any]) -> None:
    for power in payload["powers"]:
        draw_god_power_review_board(power)
        path = gameplay_review_board_path(power)
        power["gameplay_review_board"] = project_path(path)
        power["gameplay_review_board_present"] = path.exists()

    cols = 4
    thumb_w, thumb_h = 320, 260
    rows = (len(payload["powers"]) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, max(1, rows) * thumb_h), (236, 231, 218))
    draw = ImageDraw.Draw(sheet)
    small_font = load_font(14)
    for index, power in enumerate(payload["powers"]):
        path = gameplay_review_board_path(power)
        if path.exists():
            thumb = Image.open(path).convert("RGB").resize((thumb_w, thumb_h), RESAMPLE_LANCZOS)
            sheet.paste(thumb, ((index % cols) * thumb_w, (index // cols) * thumb_h))
        x = (index % cols) * thumb_w + 10
        y = (index // cols) * thumb_h + 10
        draw.text((x, y), f"{power['civilization']} / {power['spell_type']}",
                  fill=(32, 28, 24), font=small_font)

    REVIEW_BOARD_CONTACT_SHEET.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(REVIEW_BOARD_CONTACT_SHEET)
    payload["gameplay_review_board_contact_sheet"] = project_path(REVIEW_BOARD_CONTACT_SHEET)
    payload["gameplay_review_board_count"] = sum(
        1 for power in payload["powers"] if power.get("gameplay_review_board_present")
    )


def build_package() -> dict[str, Any]:
    spellbook_text = read_text(SPELLBOOK)
    mod_items_text = read_text(MOD_ITEMS)
    guide_text = read_text(GUIDE_SCREEN)
    particle_report = load_json(PARTICLE_REPORT, {})
    ui_report = load_json(UI_REPORT, {})
    runtime_smoke_playtest = runtime_smoke_playtest_status(RUNTIME_GOD_POWER_PLAYTEST_REPORT)
    registrations = parse_spellbook_registrations(mod_items_text)
    guide_costs = parse_guide_costs(guide_text)
    particle_by_spell = particle_matrix_by_spell(particle_report)
    lang_files = load_lang_files()
    global_contract = global_use_contract(spellbook_text)

    powers: list[dict[str, Any]] = []
    by_civilization: dict[str, Any] = {}
    for civilization, spell_types in GOD_POWER_SPELL_TYPES.items():
        civ_powers = [
            summarize_power(
                civilization,
                spell_type,
                spellbook_text,
                registrations,
                guide_costs,
                particle_by_spell,
                lang_files,
                global_contract,
                runtime_smoke_playtest.get("checks_by_spell", {}),
            )
            for spell_type in spell_types
        ]
        powers.extend(civ_powers)
        by_civilization[civilization] = {
            "spell_count": len(civ_powers),
            "static_contract_ok_count": sum(1 for power in civ_powers if power["static_contract_ok"]),
            "manual_playtest_required_count": sum(1 for power in civ_powers if power["manual_playtest_required"]),
            "high_risk_playtest_count": sum(1 for power in civ_powers if power.get("playtest_risk_level") == "high"),
            "medium_risk_playtest_count": sum(1 for power in civ_powers if power.get("playtest_risk_level") == "medium"),
            "low_risk_playtest_count": sum(1 for power in civ_powers if power.get("playtest_risk_level") == "low"),
            "status_counts": dict(sorted(Counter(power["status"] for power in civ_powers).items())),
        }

    risk_counts = Counter(flag for power in powers for flag in power["risk_flags"])
    risk_level_counts = Counter(power.get("playtest_risk_level", "unknown") for power in powers)
    dimension_status_counts = Counter(
        dimension.get("status", "unknown")
        for power in powers
        for dimension in power.get("playtest_dimensions", [])
    )
    runtime_probe_power_count = sum(1 for power in powers if power.get("runtime_smoke_probe", {}).get("present"))
    runtime_probe_passed_power_count = sum(1 for power in powers if power.get("runtime_smoke_probe", {}).get("ok"))
    high_risk_powers = [
        {
            "civilization": power["civilization"],
            "spell_type": power["spell_type"],
            "risk_flags": power["risk_flags"],
            "effect_scope": power["routing"]["effect_scope"],
            "world_edit_operations": power["world_edit_safety"]["operations"],
            "manual_focus": power["manual_playtest_focus"][:4],
        }
        for power in powers
        if power.get("playtest_risk_level") == "high"
    ]
    world_edit_powers = [
        power
        for power in powers
        if power["world_edit_safety"]["requires_manual_safety_review"]
    ]
    world_edit_status_counts = Counter(power["world_edit_safety"]["status"] for power in world_edit_powers)
    world_edit_operation_counts = Counter(
        operation
        for power in world_edit_powers
        for operation in power["world_edit_safety"]["operations"]
    )
    runtime_message_keys_all = sorted({
        key
        for power in powers
        for key in power["runtime_messages"]["keys"]
    })
    runtime_message_missing_by_lang: dict[str, list[str]] = {}
    for power in powers:
        for lang_name, status in power["runtime_messages"]["per_lang"].items():
            runtime_message_missing_by_lang.setdefault(lang_name, [])
            runtime_message_missing_by_lang[lang_name].extend(status.get("missing", []))
    runtime_message_missing_by_lang = {
        lang_name: sorted(set(missing))
        for lang_name, missing in runtime_message_missing_by_lang.items()
        if missing
    }
    status_counts = Counter(power["status"] for power in powers)
    static_implementation_ok = bool(powers) and all(power["static_contract_ok"] for power in powers)
    gameplay_acceptance_ok = False
    payload = {
        "version": 1,
        "scope": "mythic_age_1_0_god_power_gameplay_acceptance_package",
        "ok": static_implementation_ok,
        "static_implementation_ok": static_implementation_ok,
        "gameplay_acceptance_ok": gameplay_acceptance_ok,
        "manual_playtest_required_count": sum(1 for power in powers if power["manual_playtest_required"]),
        "playtest_risk_level_counts": dict(sorted(risk_level_counts.items())),
        "high_risk_playtest_count": risk_level_counts.get("high", 0),
        "medium_risk_playtest_count": risk_level_counts.get("medium", 0),
        "low_risk_playtest_count": risk_level_counts.get("low", 0),
        "dimension_status_counts": dict(sorted(dimension_status_counts.items())),
        "runtime_smoke_probe_power_count": runtime_probe_power_count,
        "runtime_smoke_probe_passed_power_count": runtime_probe_passed_power_count,
        "high_risk_powers": high_risk_powers,
        "spell_count": len(powers),
        "status_counts": dict(sorted(status_counts.items())),
        "risk_counts": dict(sorted(risk_counts.items())),
        "world_edit_safety_summary": {
            "manual_safety_review_required_count": len(world_edit_powers),
            "status_counts": dict(sorted(world_edit_status_counts.items())),
            "operation_counts": dict(sorted(world_edit_operation_counts.items())),
        },
        "runtime_message_localization_summary": {
            "key_count": len(runtime_message_keys_all),
            "all_lang_ok": not runtime_message_missing_by_lang,
            "missing_by_lang": runtime_message_missing_by_lang,
        },
        "runtime_smoke_playtest_ok": runtime_smoke_playtest["ok"],
        "runtime_smoke_playtest": runtime_smoke_playtest,
        "by_civilization": by_civilization,
        "sources": {
            "spellbook": project_path(SPELLBOOK),
            "mod_items": project_path(MOD_ITEMS),
            "god_power_guide_screen": project_path(GUIDE_SCREEN),
            "particle_report": project_path(PARTICLE_REPORT),
            "ui_report": project_path(UI_REPORT),
            "runtime_smoke_playtest_report": project_path(RUNTIME_GOD_POWER_PLAYTEST_REPORT),
        },
        "upstream_gate_status": {
            "god_power_particle_gate_ok": particle_report.get("ok"),
            "god_power_particle_full_matrix_ok": particle_report.get("full_matrix_ok"),
            "ui_gate_ok": ui_report.get("ok"),
            "ui_scope": ui_report.get("scope"),
        },
        "global_use_contract": global_contract,
        "acceptance_rule": (
            "static_implementation_ok only means every listed power has code/UI/feedback/particle/text coverage. "
            "runtime_smoke_playtest_ok only means every listed spellbook could be cast in a controlled MC runtime probe. "
            "playtest_risk_level only prioritizes manual testing. gameplay_acceptance_ok remains false until each power "
            "is manually played, balanced, and accepted."
        ),
        "powers": powers,
    }
    draw_god_power_review_boards(payload)
    manual_playtest_queue = build_manual_playtest_queue(payload["powers"])
    manual_playtest_batches = build_manual_playtest_batches(manual_playtest_queue)
    payload["manual_playtest_queue_count"] = len(manual_playtest_queue)
    payload["manual_playtest_queue_total_minimum_scenarios"] = sum(
        int(task["minimum_scenarios"]) for task in manual_playtest_queue
    )
    payload["manual_playtest_queue_by_priority"] = dict(sorted(
        Counter(str(task["priority"]) for task in manual_playtest_queue).items(),
        key=lambda item: int(item[0]),
    ))
    payload["manual_playtest_queue_by_risk_level"] = dict(sorted(
        Counter(str(task["risk_level"]) for task in manual_playtest_queue).items()
    ))
    payload["manual_playtest_batch_count"] = len(manual_playtest_batches)
    payload["manual_playtest_batches"] = manual_playtest_batches
    payload["manual_playtest_queue"] = manual_playtest_queue
    return payload


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 神力玩法验收包",
        "",
        "本文档由 `tools/god_power_gameplay_acceptance_package.py audit` 生成。它把 52 个非神殿生成神力的静态实现覆盖和人工玩法验收分开记录：静态通过不等于已经可发布。",
        "",
        "## 总览",
        "",
        f"- 神力数量：{payload['spell_count']}",
        f"- 静态实现契约通过：{payload['static_implementation_ok']}",
        f"- MC 运行期施放探针通过：{payload['runtime_smoke_playtest_ok']}",
        f"- MC 运行期施放探针覆盖：{payload['runtime_smoke_playtest'].get('passed')}/{payload['runtime_smoke_playtest'].get('total')}",
        f"- MC 运行期施放探针报告：`{payload['runtime_smoke_playtest'].get('path')}`",
        f"- 玩法验收通过：{payload['gameplay_acceptance_ok']}",
        f"- 需要人工逐项游玩验收：{payload['manual_playtest_required_count']}",
        f"- 风险等级统计：`{json.dumps(payload['playtest_risk_level_counts'], ensure_ascii=False)}`",
        f"- 高风险玩法测试项：{payload['high_risk_playtest_count']}",
        f"- Runtime smoke 探针覆盖：{payload['runtime_smoke_probe_passed_power_count']}/{payload['spell_count']}",
        f"- 人工玩法测试队列：{payload['manual_playtest_queue_count']}",
        f"- 人工玩法测试批次：{payload['manual_playtest_batch_count']}",
        f"- 人工玩法最低场景预算：{payload['manual_playtest_queue_total_minimum_scenarios']}",
        f"- 人工玩法队列按优先级：`{json.dumps(payload['manual_playtest_queue_by_priority'], ensure_ascii=False)}`",
        f"- 人工玩法队列按风险：`{json.dumps(payload['manual_playtest_queue_by_risk_level'], ensure_ascii=False)}`",
        f"- 测试维度状态统计：`{json.dumps(payload['dimension_status_counts'], ensure_ascii=False)}`",
        f"- 静态玩法 review board：{payload.get('gameplay_review_board_count', 0)} / {payload['spell_count']}",
        f"- Review board 总览图：`{payload.get('gameplay_review_board_contact_sheet', '')}`",
        f"- 状态统计：`{json.dumps(payload['status_counts'], ensure_ascii=False)}`",
        f"- 风险统计：`{json.dumps(payload['risk_counts'], ensure_ascii=False)}`",
        f"- 世界编辑/爆炸安全审查：`{json.dumps(payload['world_edit_safety_summary'], ensure_ascii=False)}`",
        f"- 运行时提示本地化：`{json.dumps(payload['runtime_message_localization_summary'], ensure_ascii=False)}`",
        "",
        "## 文明汇总",
        "",
        "| 文明 | 神力数 | 静态通过 | 待人工玩法验收 | 高风险 | 中风险 | 低风险 | 状态 |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |",
    ]
    for civilization, summary in payload["by_civilization"].items():
        lines.append(
            "| {civilization} | {spell_count} | {static_ok} | {manual} | {high} | {medium} | {low} | `{status}` |".format(
                civilization=civilization,
                spell_count=summary["spell_count"],
                static_ok=summary["static_contract_ok_count"],
                manual=summary["manual_playtest_required_count"],
                high=summary["high_risk_playtest_count"],
                medium=summary["medium_risk_playtest_count"],
                low=summary["low_risk_playtest_count"],
                status=json.dumps(summary["status_counts"], ensure_ascii=False),
            )
        )

    lines.extend([
        "",
        "## 人工玩法测试批次计划",
        "",
        "批次按风险和测试价值排序；P1 优先覆盖世界编辑、爆炸、固定成功路径、持续管理器和高影响范围神力。",
        "",
        "| 批次 | 优先级 | 风险 | 神力数 | 世界编辑项 | 最低场景 | 神力 | 批次验收重点 |",
        "| --- | ---: | --- | ---: | ---: | ---: | --- | --- |",
    ])
    for batch in payload["manual_playtest_batches"]:
        spells = ", ".join(batch["spells"]).replace("|", "/")
        if len(spells) > 180:
            spells = spells[:177] + "..."
        focus = "；".join(batch["acceptance_focus"]).replace("|", "/")
        lines.append(
            "| {batch_id} | {priority} | `{risk}` | {count} | {world_edit} | {scenarios} | {spells} | {focus} |".format(
                batch_id=batch["batch_id"],
                priority=batch["priority"],
                risk=batch["risk_level"],
                count=batch["task_count"],
                world_edit=batch["world_edit_task_count"],
                scenarios=batch["total_minimum_scenarios"],
                spells=spells,
                focus=focus,
            )
        )

    lines.extend([
        "",
        "## 人工玩法测试执行队列",
        "",
        "| 优先级 | 文明 | 神力 | 风险 | 最低场景 | 目标 | 范围 | 世界操作 | 验收要点 |",
        "| ---: | --- | --- | --- | ---: | --- | --- | --- | --- |",
    ])
    for task in payload["manual_playtest_queue"]:
        criteria = "；".join(task["acceptance_criteria"][:4]).replace("|", "/")
        scope = ",".join(task["effect_scope"]) if task["effect_scope"] else "unknown"
        operations = ",".join(task["world_edit_operations"]) if task["world_edit_operations"] else "none"
        lines.append(
            "| {priority} | {civilization} | `{spell}` | `{risk}` | {scenarios} | `{targeting}` | `{scope}` | `{operations}` | {criteria} |".format(
                priority=task["priority"],
                civilization=task["civilization"],
                spell=task["spell_type"],
                risk=task["risk_level"],
                scenarios=task["minimum_scenarios"],
                targeting=task["targeting"],
                scope=scope,
                operations=operations,
                criteria=criteria or "-",
            )
        )

    lines.extend([
        "",
        "## 单项清单",
        "",
        "| 文明 | 神力 | 静态契约 | 风险等级 | Runtime probe | 冷却秒 | 证明成本 | 目标/范围 | 风险 | 首要人工测试重点 | Review board |",
        "| --- | --- | --- | --- | --- | ---: | ---: | --- | --- | --- | --- |",
    ])
    for power in payload["powers"]:
        registration = power["registration"]
        guide = power["guide"]
        runtime_probe = power.get("runtime_smoke_probe", {})
        focus = power.get("manual_playtest_focus", [])
        lines.append(
            "| {civilization} | `{spell}` | `{static}` | `{risk_level}` | `{probe}` | {cooldown} | {cost} | `{targeting}` | `{risks}` | {focus} | `{board}` |".format(
                civilization=power["civilization"],
                spell=power["spell_type"],
                static=power["static_contract_ok"],
                risk_level=power.get("playtest_risk_level"),
                probe=f"ok={runtime_probe.get('ok')} {runtime_probe.get('duration_ms')}ms",
                cooldown=registration.get("cooldown_seconds"),
                cost=guide.get("certificate_cost"),
                targeting=power["routing"]["targeting"],
                risks=",".join(power["risk_flags"]) if power["risk_flags"] else "none",
                focus=(str(focus[0]).replace("|", "/") if focus else "-"),
                board=power.get("gameplay_review_board", ""),
            )
        )

    lines.extend([
        "",
        "## 高风险玩法测试优先级",
        "",
        "| 文明 | 神力 | 风险标记 | 效果范围 | 世界操作 | 测试重点 |",
        "| --- | --- | --- | --- | --- | --- |",
    ])
    for power in payload["high_risk_powers"]:
        lines.append(
            "| {civilization} | `{spell}` | `{risks}` | `{scope}` | `{operations}` | {focus} |".format(
                civilization=power["civilization"],
                spell=power["spell_type"],
                risks=",".join(power["risk_flags"]) if power["risk_flags"] else "none",
                scope=",".join(power["effect_scope"]),
                operations=",".join(power["world_edit_operations"]) if power["world_edit_operations"] else "none",
                focus="；".join(str(item).replace("|", "/") for item in power["manual_focus"]),
            )
        )

    lines.extend([
        "",
        "## 世界编辑/爆炸安全审查",
        "",
        "| 文明 | 神力 | 操作 | 静态安全状态 | 保护证据 |",
        "| --- | --- | --- | --- | --- |",
    ])
    for power in payload["powers"]:
        safety = power["world_edit_safety"]
        if not safety["requires_manual_safety_review"]:
            continue
        safeguards = [
            key
            for key, enabled in safety["safeguards"].items()
            if enabled
        ]
        lines.append(
            "| {civilization} | `{spell}` | `{operations}` | `{status}` | `{safeguards}` |".format(
                civilization=power["civilization"],
                spell=power["spell_type"],
                operations=",".join(safety["operations"]),
                status=safety["status"],
                safeguards=",".join(safeguards) if safeguards else "none",
            )
        )

    lines.extend([
        "",
        "## 验收规则",
        "",
        "- `static_contract_ok=true` 只说明注册、施法路由、冷却/提示、神力图鉴、主语言 tooltip 和粒子矩阵存在。",
        "- `runtime_smoke_playtest_ok=true` 只说明 52 个神力在受控 MC 运行期探针中能完成一次服务端施放。",
        "- `gameplay_acceptance_ok=false` 表示 52 个神力仍未经过逐项实机手感、平衡、失败路径、性能和可读性验收。",
        "- `void_cast_always_success` 表示该神力路由调用 void 方法后固定成功，需要人工确认无目标、无敌人或不可执行场景下是否应该失败。",
        "- 若后续再次出现 `threaded_delayed_effect`，必须迁移到服务端 tick 调度或持久化管理器。",
        "- `large_world_edit_or_explosion` 需要额外检查服务端 tick 安全、存档安全和性能。",
        "- 若 `supported_language_release_gap` 再次出现，发布前必须补齐所有声明支持语言的名称和 tooltip。",
        "",
    ])
    MARKDOWN_PATH.write_text("\n".join(lines), encoding="utf-8")


def audit() -> tuple[int, dict[str, Any]]:
    payload = build_package()
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    write_markdown(payload)
    return (0 if payload["ok"] else 1), payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        code, payload = audit()
        if code == 0:
            print("God Power Gameplay Acceptance Package: STATIC PASS / PLAYTEST PENDING")
            print(f"report: {REPORT_PATH}")
            print(f"markdown: {MARKDOWN_PATH}")
            print(f"manual_playtest_required: {payload['manual_playtest_required_count']}")
        else:
            print("God Power Gameplay Acceptance Package: STATIC FAIL")
            print(f"report: {REPORT_PATH}")
            print(f"markdown: {MARKDOWN_PATH}")
            print(f"status_counts: {payload['status_counts']}")
        return code
    return 0


if __name__ == "__main__":
    sys.exit(main())
