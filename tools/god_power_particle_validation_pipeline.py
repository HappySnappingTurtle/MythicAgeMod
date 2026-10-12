#!/usr/bin/env python3
"""Static gate for god-power particle matrix and performance controls."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/particle_validation"
REPORT_PATH = REPORT_DIR / "god_power_particle_report.json"
CONFIG = ROOT / "src/main/java/com/example/mythic_age/config/ModConfig.java"
HELPER = ROOT / "src/main/java/com/example/mythic_age/particle/MythicParticleEffects.java"
SPELLBOOK = ROOT / "src/main/java/com/example/mythic_age/item/SpellbookItem.java"
LIGHTNING_MANAGER = ROOT / "src/main/java/com/example/mythic_age/util/LightningStormManager.java"
EARTHQUAKE_MANAGER = ROOT / "src/main/java/com/example/mythic_age/util/EarthquakeManager.java"
WEATHER_MANAGER = ROOT / "src/main/java/com/example/mythic_age/util/GodPowerWeatherManager.java"
MOD_ITEMS = ROOT / "src/main/java/com/example/mythic_age/item/ModItems.java"
LANG_DIR = ROOT / "src/main/resources/assets/mythic_age/lang"
MODEL_DIR = ROOT / "src/main/resources/assets/mythic_age/models/item"

ATLANTEAN_SPELLBOOKS = [
    "tidal_surge",
    "abyssal_current",
    "crystal_barrier",
    "pearl_harvest",
    "orichalcum_forge",
    "siren_song",
    "leviathan_call",
    "abyssal_rift",
    "chrono_stasis",
    "oceanic_wrath",
]

ATLANTEAN_SPELL_TYPES = [
    "TIDAL_SURGE",
    "ABYSSAL_CURRENT",
    "CRYSTAL_BARRIER",
    "PEARL_HARVEST",
    "ORICHALCUM_FORGE",
    "SIREN_SONG",
    "LEVIATHAN_CALL",
    "ABYSSAL_RIFT",
    "CHRONO_STASIS",
    "OCEANIC_WRATH",
]

GOD_POWER_SPELL_TYPES: dict[str, list[str]] = {
    "greek": [
        "LIGHTNING_STORM",
        "RESTORATION",
        "LURE",
        "UNDERWORLD_ARMY",
        "PESTILENCE",
        "CURSE",
        "EARTHQUAKE",
        "CEASEFIRE",
        "BRONZE",
        "UNDERWORLD_PASSAGE",
        "LIGHTNING_STORM_HERA",
        "PLENTY",
    ],
    "chinese": [
        "RAIN_PRAYER",
        "EARTH_GOD_BLESSING",
        "ENLIGHTENMENT",
        "ABUNDANCE",
        "FIVE_THUNDER",
        "CRANE_BLESSING",
        "FLOOD",
        "CALL_TO_ARMS",
        "TAIJI_SHIELD",
        "RECREATION",
    ],
    "nordic": [
        "FENRIR_HOWL",
        "ODIN_EYE",
        "GREAT_HUNT",
        "DWARVEN_MINE",
        "THOR_WRATH",
        "VALHALLA_CALL",
        "FROST",
        "WALKING_WOODS",
        "RAGNAROK",
        "FIMBULWINTER",
    ],
    "egyptian": [
        "RA_RADIANCE",
        "DESERT_STORM",
        "SUN_RAIN",
        "PROSPERITY",
        "EYE_OF_HORUS",
        "ANUBIS_JUDGMENT",
        "LOCUST_SWARM",
        "TORNADO",
        "ISIS_RESURRECTION",
        "METEOR",
    ],
    "atlantean": ATLANTEAN_SPELL_TYPES,
}

GOD_POWER_METHODS: dict[str, list[str]] = {
    "LIGHTNING_STORM": ["castLightningStorm"],
    "RESTORATION": ["castRestoration"],
    "LURE": ["castLure"],
    "UNDERWORLD_ARMY": ["castUnderworldArmy"],
    "PESTILENCE": ["castPestilence"],
    "CURSE": ["castCurse"],
    "EARTHQUAKE": ["castEarthquake"],
    "CEASEFIRE": ["castCeasefire"],
    "BRONZE": ["castBronze"],
    "UNDERWORLD_PASSAGE": ["castUnderworldPassage"],
    "LIGHTNING_STORM_HERA": ["castLightningStormHera"],
    "PLENTY": ["castPlenty"],
    "RAIN_PRAYER": ["castRainPrayer"],
    "EARTH_GOD_BLESSING": ["castEarthGodBlessing"],
    "ENLIGHTENMENT": ["castEnlightenment"],
    "ABUNDANCE": ["castAbundance"],
    "FIVE_THUNDER": ["castFiveThunder"],
    "CRANE_BLESSING": ["castCraneBlessing"],
    "FLOOD": ["castFlood"],
    "CALL_TO_ARMS": ["castCallToArms"],
    "TAIJI_SHIELD": ["castTaijiShield"],
    "RECREATION": ["castRecreation"],
    "FENRIR_HOWL": ["castFenrirHowl"],
    "ODIN_EYE": ["castOdinEye"],
    "GREAT_HUNT": ["castGreatHunt"],
    "DWARVEN_MINE": ["castDwarvenMine"],
    "THOR_WRATH": ["castThorWrath"],
    "VALHALLA_CALL": ["castValhallaCall"],
    "FROST": ["castFrost"],
    "WALKING_WOODS": ["castWalkingWoods"],
    "RAGNAROK": ["castRagnarok"],
    "FIMBULWINTER": ["castFimbulwinter"],
    "RA_RADIANCE": ["castRaRadiance"],
    "DESERT_STORM": ["castDesertStorm"],
    "SUN_RAIN": ["castSunRain"],
    "PROSPERITY": ["castProsperity"],
    "EYE_OF_HORUS": ["castEyeOfHorus"],
    "ANUBIS_JUDGMENT": ["castAnubisJudgment"],
    "LOCUST_SWARM": ["castLocustSwarm"],
    "TORNADO": ["castTornado", "performTornadoEffectAtPosition"],
    "ISIS_RESURRECTION": ["castIsisResurrection"],
    "METEOR": ["castMeteorRain"],
    "TIDAL_SURGE": ["castTidalSurge"],
    "ABYSSAL_CURRENT": ["castAbyssalCurrent"],
    "CRYSTAL_BARRIER": ["castCrystalBarrier"],
    "PEARL_HARVEST": ["castPearlHarvest"],
    "ORICHALCUM_FORGE": ["castOrichalcumForge"],
    "SIREN_SONG": ["castSirenSong"],
    "LEVIATHAN_CALL": ["castLeviathanCall"],
    "ABYSSAL_RIFT": ["castAbyssalRift"],
    "CHRONO_STASIS": ["castChronoStasis"],
    "OCEANIC_WRATH": ["castOceanicWrath"],
}

GOD_POWER_REQUIRED_TOKENS: dict[str, list[str]] = {
    "LIGHTNING_STORM": ["LightningStormManager.getInstance().startLightningStorm"],
    "EARTHQUAKE": ["EarthquakeManager.getInstance().startEarthquake"],
    "FIMBULWINTER": ["GodPowerWeatherManager.getInstance().startFimbulwinter"],
    "OCEANIC_WRATH": ["GodPowerWeatherManager.getInstance().startOceanicWrath"],
}

PARTICLE_HELPER_CALL_RE = re.compile(
    r"MythicParticleEffects\.spawn(?:Ambient|Normal|Major|Ultimate)?\s*\("
)

EXPECTED_CONFIG_TOKENS = [
    "PARTICLES_ENABLED",
    "PARTICLE_DENSITY_MULTIPLIER",
    "PARTICLE_FAR_DISTANCE_MULTIPLIER",
    "PARTICLE_MAX_RENDER_DISTANCE",
    "MAX_PARTICLES_PER_BURST",
]

EXPECTED_HELPER_TOKENS = [
    "public final class MythicParticleEffects",
    "enum Intensity",
    "spawnAmbient(",
    "spawnNormal(",
    "spawnMajor(",
    "spawnUltimate(",
    "scaleParticleCount(",
    "nearestPlayerDistanceSquared(",
    "PARTICLE_DISTANCE_THRESHOLD",
    "MAX_PARTICLES_PER_BURST",
]

POWER_CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "greek_lightning_storm",
        "civilization": "greek",
        "method": "castLightningStorm",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnAmbient(serverWorld, ParticleTypes.CLOUD",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.ELECTRIC_SPARK",
            "LightningStormManager.getInstance().startLightningStorm",
        ],
    },
    {
        "id": "greek_earthquake",
        "civilization": "greek",
        "method": "castEarthquake",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.EXPLOSION_EMITTER",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.CLOUD",
            "EarthquakeManager.getInstance().startEarthquake",
        ],
    },
    {
        "id": "chinese_taiji_shield",
        "civilization": "chinese",
        "method": "castTaijiShield",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.END_ROD",
        ],
    },
    {
        "id": "nordic_fimbulwinter",
        "civilization": "nordic",
        "method": "castFimbulwinter",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.SNOWFLAKE",
        ],
    },
    {
        "id": "egyptian_ra_radiance",
        "civilization": "egyptian",
        "method": "castRaRadiance",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnAmbient(serverWorld, ParticleTypes.END_ROD",
        ],
    },
    {
        "id": "egyptian_isis_resurrection",
        "civilization": "egyptian",
        "method": "castIsisResurrection",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnAmbient(serverWorld, ParticleTypes.TOTEM_OF_UNDYING",
            "MythicParticleEffects.spawnAmbient(serverWorld, ParticleTypes.HEART",
        ],
    },
    {
        "id": "atlantean_tidal_surge",
        "civilization": "atlantean",
        "method": "castTidalSurge",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.SPLASH",
            "MythicParticleEffects.spawnNormal(serverWorld, ParticleTypes.BUBBLE_POP",
            "MythicParticleEffects.spawnMajor(serverWorld, ModParticles.SHOCKWAVE",
        ],
    },
    {
        "id": "atlantean_crystal_barrier",
        "civilization": "atlantean",
        "method": "castCrystalBarrier",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnMajor(serverWorld, ModParticles.DIVINE_SPARKLE",
            "MythicParticleEffects.spawnNormal(serverWorld, ParticleTypes.END_ROD",
        ],
    },
    {
        "id": "atlantean_abyssal_current",
        "civilization": "atlantean",
        "method": "castAbyssalCurrent",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.BUBBLE_POP",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.SPLASH",
            "MythicParticleEffects.spawnNormal(serverWorld, ParticleTypes.NAUTILUS",
        ],
    },
    {
        "id": "atlantean_pearl_harvest",
        "civilization": "atlantean",
        "method": "castPearlHarvest",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.NAUTILUS",
            "MythicParticleEffects.spawnNormal(serverWorld, ParticleTypes.HAPPY_VILLAGER",
            "MythicParticleEffects.spawnNormal(serverWorld, ModParticles.DIVINE_SPARKLE",
        ],
    },
    {
        "id": "atlantean_leviathan_call",
        "civilization": "atlantean",
        "method": "castLeviathanCall",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.NAUTILUS",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.SPLASH",
            "MythicParticleEffects.spawnMajor(serverWorld, ModParticles.SHOCKWAVE",
        ],
    },
    {
        "id": "atlantean_abyssal_rift",
        "civilization": "atlantean",
        "method": "castAbyssalRift",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.SQUID_INK",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.PORTAL",
            "MythicParticleEffects.spawnMajor(serverWorld, ModParticles.SHOCKWAVE",
        ],
    },
    {
        "id": "atlantean_chrono_stasis",
        "civilization": "atlantean",
        "method": "castChronoStasis",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnUltimate(serverWorld, ModParticles.DIVINE_SPARKLE",
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.END_ROD",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.ENCHANT",
        ],
    },
    {
        "id": "atlantean_oceanic_wrath",
        "civilization": "atlantean",
        "method": "castOceanicWrath",
        "path": SPELLBOOK,
        "tokens": [
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.SPLASH",
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.EXPLOSION_EMITTER",
            "MythicParticleEffects.spawnMajor(serverWorld, ModParticles.SHOCKWAVE",
            "MythicParticleEffects.spawnMajor(serverWorld, ModParticles.DIVINE_SPARKLE",
        ],
    },
    {
        "id": "persistent_lightning_storm",
        "civilization": "greek",
        "method": "LightningStormManager",
        "path": LIGHTNING_MANAGER,
        "tokens": [
            "MythicParticleEffects.spawnNormal(serverWorld, ParticleTypes.SOUL",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.CLOUD",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.SWEEP_ATTACK",
        ],
    },
    {
        "id": "persistent_earthquake",
        "civilization": "greek",
        "method": "EarthquakeManager",
        "path": EARTHQUAKE_MANAGER,
        "tokens": [
            "MythicParticleEffects.spawnAmbient(serverWorld, ParticleTypes.EXPLOSION",
            "MythicParticleEffects.spawnUltimate(serverWorld, ParticleTypes.EXPLOSION_EMITTER",
            "MythicParticleEffects.spawnAmbient(serverWorld, ParticleTypes.CRIT",
            "MythicParticleEffects.spawnMajor(serverWorld, ParticleTypes.CLOUD",
        ],
    },
]


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def method_body(text: str, method_name: str) -> str:
    match = re.search(rf"(private|public)\s+(boolean|void)\s+{re.escape(method_name)}\s*\(", text)
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


def spell_switch_case_body(text: str, spell_type: str) -> str:
    match = re.search(rf"case\s+{re.escape(spell_type)}\s*:", text)
    if not match:
        return ""
    next_case = re.search(r"\n\s*(case\s+[A-Z0-9_]+\s*:|default\s*:)", text[match.end():])
    end = match.end() + next_case.start() if next_case else len(text)
    return text[match.start():end]


def helper_call_count(text: str) -> int:
    return len(PARTICLE_HELPER_CALL_RE.findall(text))


def direct_spellbook_particle_calls(text: str) -> list[dict[str, Any]]:
    offenders: list[dict[str, Any]] = []
    for match in re.finditer(r"(?<!MythicParticleEffects\.)\b(?:serverWorld|world)\.spawnParticles\s*\(", text):
        line_no = text.count("\n", 0, match.start()) + 1
        line = text.splitlines()[line_no - 1].strip()
        offenders.append({"line": line_no, "code": line})
    return offenders


def audit() -> tuple[int, dict[str, Any]]:
    failures: list[str] = []
    config_text = read_text(CONFIG)
    helper_text = read_text(HELPER)
    spellbook_text = read_text(SPELLBOOK)
    mod_items_text = read_text(MOD_ITEMS)
    cast_god_power_body = method_body(spellbook_text, "castGodPower")

    config_results = []
    for token in EXPECTED_CONFIG_TOKENS:
        ok = token in config_text
        if not ok:
            failures.append(f"config missing {token}")
        config_results.append({"token": token, "ok": ok})

    helper_results = []
    for token in EXPECTED_HELPER_TOKENS:
        ok = token in helper_text
        if not ok:
            failures.append(f"helper missing {token}")
        helper_results.append({"token": token, "ok": ok})

    contract_results = []
    for contract in POWER_CONTRACTS:
        path = contract["path"]
        text = read_text(path)
        body = method_body(text, contract["method"]) if path == SPELLBOOK else text
        missing = [token for token in contract["tokens"] if token not in body]
        if missing:
            failures.append(f"{contract['id']} missing " + "|".join(missing))
        contract_results.append({
            "id": contract["id"],
            "civilization": contract["civilization"],
            "method": contract["method"],
            "path": str(path.relative_to(ROOT)),
            "missing_tokens": missing,
            "ok": not missing,
        })

    full_matrix_results = []
    full_matrix_failures: list[str] = []
    full_matrix_summary = {
        "expected_spell_count": sum(len(spells) for spells in GOD_POWER_SPELL_TYPES.values()),
        "checked_spell_count": 0,
        "ok_spell_count": 0,
        "by_civilization": {},
    }
    direct_spellbook_calls = direct_spellbook_particle_calls(spellbook_text)
    if direct_spellbook_calls:
        failures.append(f"spellbook has direct spawnParticles calls: {len(direct_spellbook_calls)}")
        full_matrix_failures.append("spellbook direct spawnParticles calls bypass MythicParticleEffects")

    for civilization, spell_types in GOD_POWER_SPELL_TYPES.items():
        civilization_ok_count = 0
        for spell_type in spell_types:
            methods = GOD_POWER_METHODS.get(spell_type, [])
            switch_body = spell_switch_case_body(cast_god_power_body, spell_type)
            method_bodies = {
                method: method_body(spellbook_text, method)
                for method in methods
            }
            combined_body = "\n".join(method_bodies.values())
            missing_methods = [
                method for method, body in method_bodies.items()
                if not body
            ]
            missing_required_tokens = [
                token for token in GOD_POWER_REQUIRED_TOKENS.get(spell_type, [])
                if token not in combined_body
            ]
            registered_ok = re.search(rf"SpellType\.{re.escape(spell_type)}\b", mod_items_text) is not None
            switch_ok = bool(switch_body) and any(f"{method}(" in switch_body for method in methods)
            particle_call_count = helper_call_count(combined_body)
            helper_ok = particle_call_count > 0
            ok = (
                registered_ok
                and switch_ok
                and not missing_methods
                and helper_ok
                and not missing_required_tokens
            )
            if not ok:
                reason_parts = []
                if not registered_ok:
                    reason_parts.append("not registered in ModItems")
                if not switch_ok:
                    reason_parts.append("missing castGodPower switch entry")
                if missing_methods:
                    reason_parts.append("missing method " + ",".join(missing_methods))
                if not helper_ok:
                    reason_parts.append("no MythicParticleEffects call")
                if missing_required_tokens:
                    reason_parts.append("missing required tokens " + ",".join(missing_required_tokens))
                reason = "; ".join(reason_parts)
                full_matrix_failures.append(f"{civilization}.{spell_type}: {reason}")
                failures.append(f"god power particle matrix {civilization}.{spell_type}: {reason}")
            else:
                civilization_ok_count += 1

            full_matrix_results.append({
                "civilization": civilization,
                "spell_type": spell_type,
                "methods": methods,
                "registered_ok": registered_ok,
                "switch_ok": switch_ok,
                "missing_methods": missing_methods,
                "missing_required_tokens": missing_required_tokens,
                "particle_helper_call_count": particle_call_count,
                "ok": ok,
            })
            full_matrix_summary["checked_spell_count"] += 1

        full_matrix_summary["by_civilization"][civilization] = {
            "expected": len(spell_types),
            "ok": civilization_ok_count,
            "complete": civilization_ok_count == len(spell_types),
        }

    full_matrix_summary["ok_spell_count"] = sum(
        1 for result in full_matrix_results
        if result["ok"]
    )
    full_matrix_ok = (
        full_matrix_summary["checked_spell_count"] == full_matrix_summary["expected_spell_count"]
        and full_matrix_summary["ok_spell_count"] == full_matrix_summary["expected_spell_count"]
        and not direct_spellbook_calls
    )

    registered_spellbooks = {
        "greek": len(re.findall(r"SpellType\.(LIGHTNING_STORM|RESTORATION|LURE|UNDERWORLD_ARMY|PESTILENCE|CURSE|EARTHQUAKE|CEASEFIRE|BRONZE|UNDERWORLD_PASSAGE|LIGHTNING_STORM_HERA|PLENTY)", mod_items_text)),
        "chinese": len(re.findall(r"SpellType\.(RAIN_PRAYER|EARTH_GOD_BLESSING|ENLIGHTENMENT|ABUNDANCE|FIVE_THUNDER|CRANE_BLESSING|FLOOD|CALL_TO_ARMS|TAIJI_SHIELD|RECREATION)", mod_items_text)),
        "nordic": len(re.findall(r"SpellType\.(FENRIR_HOWL|ODIN_EYE|GREAT_HUNT|DWARVEN_MINE|THOR_WRATH|VALHALLA_CALL|FROST|WALKING_WOODS|RAGNAROK|FIMBULWINTER)", mod_items_text)),
        "egyptian": len(re.findall(r"SpellType\.(RA_RADIANCE|DESERT_STORM|SUN_RAIN|PROSPERITY|EYE_OF_HORUS|ANUBIS_JUDGMENT|LOCUST_SWARM|TORNADO|ISIS_RESURRECTION|METEOR)", mod_items_text)),
        "atlantean": len(re.findall(r"SpellType\.(" + "|".join(ATLANTEAN_SPELL_TYPES) + r")", mod_items_text)),
        "atlantean_temple_generation": len(re.findall(r"SpellType\.GENERATE_ATLANTEAN_TEMPLE", mod_items_text)),
    }
    if registered_spellbooks["atlantean"] < len(ATLANTEAN_SPELLBOOKS):
        failures.append("atlantean dedicated spellbook matrix incomplete")
    if registered_spellbooks["atlantean_temple_generation"] < 1:
        failures.append("atlantean temple generation spellbook missing")

    atlantean_resource_results = []
    lang_texts = {path.name: read_text(path) for path in sorted(LANG_DIR.glob("*.json"))}
    for spellbook in ATLANTEAN_SPELLBOOKS:
        model_path = MODEL_DIR / f"spellbook_{spellbook}.json"
        model_ok = model_path.exists()
        model_text = read_text(model_path)
        dedicated_texture_id = f"mythic_age:item/spellbook_{spellbook}"
        dedicated_model_ok = dedicated_texture_id in model_text
        texture_path = ROOT / f"src/main/resources/assets/mythic_age/textures/item/spellbook_{spellbook}.png"
        texture_ok = texture_path.exists()
        item_key = f"item.mythic_age.spellbook_{spellbook}"
        spell_key = f"spell.{spellbook}"
        description_key = f"mythic_age.spellbook.{spellbook}.description"
        missing_lang = []
        for lang_name, lang_text in lang_texts.items():
            missing = [
                key for key in (item_key, spell_key, description_key)
                if key not in lang_text
            ]
            if missing:
                missing_lang.append({"lang": lang_name, "missing": missing})
        if not model_ok:
            failures.append(f"atlantean model missing spellbook_{spellbook}.json")
        elif not dedicated_model_ok:
            failures.append(f"atlantean model spellbook_{spellbook}.json does not point at {dedicated_texture_id}")
        if not texture_ok:
            failures.append(f"atlantean texture missing spellbook_{spellbook}.png")
        if missing_lang:
            failures.append(f"atlantean lang missing {spellbook}")
        atlantean_resource_results.append({
            "spellbook": spellbook,
            "model": str(model_path.relative_to(ROOT)),
            "model_ok": model_ok,
            "dedicated_model_ok": dedicated_model_ok,
            "texture": str(texture_path.relative_to(ROOT)),
            "texture_ok": texture_ok,
            "missing_lang": missing_lang,
            "ok": model_ok and dedicated_model_ok and texture_ok and not missing_lang,
        })

    payload = {
        "version": 1,
        "scope": "god_power_particle_matrix_and_performance_gate",
        "ok": not failures,
        "failures": failures,
        "config": config_results,
        "helper": helper_results,
        "contracts": contract_results,
        "full_matrix_ok": full_matrix_ok,
        "full_matrix_summary": full_matrix_summary,
        "full_matrix_failures": full_matrix_failures,
        "god_power_particle_matrix": full_matrix_results,
        "direct_spellbook_particle_calls": direct_spellbook_calls,
        "registered_spellbooks": registered_spellbooks,
        "atlantean_spellbook_resources": atlantean_resource_results,
        "known_gaps": [
            "God-power static particle matrix now covers all 52 non-temple-generation god powers and blocks direct SpellbookItem particle bypasses.",
            "Remaining particle work is final civilization-specific art direction, custom particle atlas polish, gameplay balance, and per-spell playtest acceptance.",
        ],
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
            print("God Power Particle Gate: PASS")
            print(f"report: {REPORT_PATH}")
        else:
            print("God Power Particle Gate: FAIL")
            for failure in payload["failures"]:
                print(f"- {failure}")
        return code
    return 0


if __name__ == "__main__":
    sys.exit(main())
