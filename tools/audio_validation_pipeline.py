#!/usr/bin/env python3
"""Validate creature audio coverage for Mythic Age 1.0."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/audio_validation"
REPORT_PATH = REPORT_DIR / "audio_report.json"
SOUNDS_JSON = ROOT / "src/main/resources/assets/mythic_age/sounds.json"
MOD_SOUNDS = ROOT / "src/main/java/com/example/mythic_age/sound/ModSounds.java"
ENTITY_DIR = ROOT / "src/main/java/com/example/mythic_age/entity"
AUDIO_ART_DIRECTION = ROOT / "docs/美术资源/audio/creature_audio_art_direction_2026-06-12.json"
AUDIO_PLACEHOLDER_REPAIR_REPORT = REPORT_DIR / "audio_placeholder_repair_report.json"
AUDIO_FINAL_MIX_CANDIDATE_REPORT = REPORT_DIR / "creature_final_mix_candidate_audio_report.json"
LANG_DIR = ROOT / "src/main/resources/assets/mythic_age/lang"

CIVILIZATION_MANIFESTS = {
    "greek": ROOT / "docs/美术资源/greek_creatures/visual_manifest.json",
    "egyptian": ROOT / "docs/美术资源/egyptian_creatures/visual_manifest.json",
    "nordic": ROOT / "docs/美术资源/nordic_creatures/visual_manifest.json",
    "chinese": ROOT / "docs/美术资源/chinese_creatures/visual_manifest.json",
    "atlantean": ROOT / "docs/美术资源/atlantean_creatures/visual_manifest.json",
}

ENTITY_CLASSES = {
    "minotaur": "MinotaurEntity",
    "cyclops": "CyclopsEntity",
    "centaur": "CentaurEntity",
    "hydra": "HydraEntity",
    "medusa": "MedusaEntity",
    "nemean_lion": "NemeanLionEntity",
    "manticore": "ManticoreEntity",
    "chimera": "ChimeraEntity",
    "colossus": "ColossusEntity",
    "pegasus": "PegasusEntity",
    "anubis_warrior": "AnubisWarriorEntity",
    "scarab_guardian": "ScarabGuardianEntity",
    "mummy_soldier": "MummySoldierEntity",
    "sphinx_sentinel": "SphinxSentinelEntity",
    "sun_falcon": "SunFalconEntity",
    "frost_giant": "FrostGiantEntity",
    "draugr_warrior": "DraugrWarriorEntity",
    "valkyrie": "ValkyrieEntity",
    "fenrir_wolf": "FenrirWolfEntity",
    "dwarf_berserker": "DwarfBerserkerEntity",
    "azure_dragon": "AzureDragonEntity",
    "qilin_guardian": "QilinGuardianEntity",
    "jiangshi": "JiangshiEntity",
    "nine_tailed_fox": "NineTailedFoxEntity",
    "stone_lion_guardian": "StoneLionGuardianEntity",
    "crystal_leviathan": "CrystalLeviathanEntity",
    "orichalcum_golem": "OrichalcumGolemEntity",
    "siren_priestess": "SirenPriestessEntity",
    "tidal_guardian": "TidalGuardianEntity",
    "abyssal_ray": "AbyssalRayEntity",
}

EXTRA_REFERENCE_SOURCES = {
    ("minotaur", "charge"): [
        ROOT / "src/main/java/com/example/mythic_age/entity/ai/goal/MinotaurChargeAttackGoal.java",
    ],
}

DEFAULT_EVENTS = ["ambient", "hurt", "death", "attack", "special"]
EVENT_OVERRIDES = {
    "minotaur": ["ambient", "hurt", "death", "charge"],
    "pegasus": ["ambient", "hurt", "death", "fly", "special"],
}
PROCEDURAL_FIRST_PASS_CREATURES = {
    "anubis_warrior",
    "scarab_guardian",
    "mummy_soldier",
    "sphinx_sentinel",
    "sun_falcon",
    "frost_giant",
    "draugr_warrior",
    "valkyrie",
    "fenrir_wolf",
    "dwarf_berserker",
    "azure_dragon",
    "qilin_guardian",
    "jiangshi",
    "nine_tailed_fox",
    "stone_lion_guardian",
    "crystal_leviathan",
    "orichalcum_golem",
    "siren_priestess",
    "tidal_guardian",
    "abyssal_ray",
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def creature_ids(civilization: str, path: Path) -> list[str]:
    payload = load_json(path, {})
    creatures = payload.get("creatures", []) if isinstance(payload, dict) else []
    ids: list[str] = []
    for creature in creatures:
        if isinstance(creature, dict) and isinstance(creature.get("id"), str):
            ids.append(creature["id"])
    return ids


def required_events(creature_id: str) -> list[str]:
    return EVENT_OVERRIDES.get(creature_id, DEFAULT_EVENTS)


def sound_file_status(sound_ref: str) -> dict[str, Any]:
    namespace = "mythic_age"
    path = sound_ref
    if ":" in sound_ref:
        namespace, path = sound_ref.split(":", 1)
    audio_path = ROOT / f"src/main/resources/assets/{namespace}/sounds/{path}.ogg"
    if not audio_path.exists():
        return {
            "path": project_path(audio_path),
            "exists": False,
            "non_empty": False,
            "looks_like_ogg_vorbis": False,
            "ok": False,
        }
    header = audio_path.read_bytes()[:96]
    looks_like_ogg_vorbis = header.startswith(b"OggS") and b"vorbis" in header
    return {
        "path": project_path(audio_path),
        "exists": True,
        "non_empty": audio_path.stat().st_size > 0,
        "looks_like_ogg_vorbis": looks_like_ogg_vorbis,
        "ok": audio_path.stat().st_size > 0 and looks_like_ogg_vorbis,
    }


def sounds_for_event(sounds_payload: dict[str, Any], key: str) -> list[str]:
    entry = sounds_payload.get(key, {})
    raw_sounds = entry.get("sounds", []) if isinstance(entry, dict) else []
    sounds: list[str] = []
    for raw in raw_sounds:
        if isinstance(raw, str):
            sounds.append(raw)
        elif isinstance(raw, dict) and isinstance(raw.get("name"), str):
            sounds.append(raw["name"])
    return sounds


def subtitle_for_event(sounds_payload: dict[str, Any], key: str) -> str | None:
    entry = sounds_payload.get(key, {})
    if isinstance(entry, dict) and isinstance(entry.get("subtitle"), str):
        return entry["subtitle"]
    return None


def constant_name(creature_id: str, event: str) -> str:
    return f"ENTITY_{creature_id.upper()}_{event.upper()}"


def entity_uses_mod_sound(creature_id: str, event: str, source: str) -> bool:
    const = f"ModSounds.{constant_name(creature_id, event)}"
    extra_sources = EXTRA_REFERENCE_SOURCES.get((creature_id, event), [])
    extra_text = "\n".join(path.read_text(encoding="utf-8") for path in extra_sources if path.exists())
    if event in {"ambient", "hurt", "death", "attack", "charge", "fly", "special"}:
        return const in source or const in extra_text
    return False


def build_report() -> dict[str, Any]:
    sounds_payload = load_json(SOUNDS_JSON, {})
    art_direction = load_json(AUDIO_ART_DIRECTION, {})
    placeholder_repair_report = load_json(AUDIO_PLACEHOLDER_REPAIR_REPORT, {})
    placeholder_repair_creatures = {
        creature_id
        for creature_id in placeholder_repair_report.get("generated_creatures", [])
        if isinstance(creature_id, str)
    }
    final_mix_candidate_report = load_json(AUDIO_FINAL_MIX_CANDIDATE_REPORT, {})
    final_mix_candidate_creatures = {
        creature_id
        for creature_id in final_mix_candidate_report.get("generated_creatures", [])
        if isinstance(creature_id, str)
    } if final_mix_candidate_report.get("ok") else set()
    art_creatures = art_direction.get("creatures", {}) if isinstance(art_direction, dict) else {}
    lang_payloads = {
        path.stem: load_json(path, {})
        for path in sorted(LANG_DIR.glob("*.json"))
    }
    mod_sounds_text = MOD_SOUNDS.read_text(encoding="utf-8") if MOD_SOUNDS.exists() else ""
    failures: list[str] = []
    art_direction_failures: list[str] = []
    subtitle_failures: list[str] = []
    civilizations: dict[str, Any] = {}
    creature_results: list[dict[str, Any]] = []

    for civilization, manifest in CIVILIZATION_MANIFESTS.items():
        ids = creature_ids(civilization, manifest)
        civilizations[civilization] = {
            "manifest": project_path(manifest),
            "creatures": ids,
        }
        for creature_id in ids:
            source_path = ENTITY_DIR / f"{ENTITY_CLASSES[creature_id]}.java"
            source = source_path.read_text(encoding="utf-8") if source_path.exists() else ""
            events: list[dict[str, Any]] = []
            for event in required_events(creature_id):
                key = f"entity.{creature_id}.{event}"
                const = constant_name(creature_id, event)
                subtitle = subtitle_for_event(sounds_payload, key)
                registered_in_code = f'registerSoundEvent("{key}")' in mod_sounds_text
                declared_in_json = key in sounds_payload
                sound_refs = sounds_for_event(sounds_payload, key)
                file_statuses = [sound_file_status(sound_ref) for sound_ref in sound_refs]
                files_exist = bool(file_statuses) and all(status["exists"] for status in file_statuses)
                playable_files = bool(file_statuses) and all(status["ok"] for status in file_statuses)
                referenced_by_entity = entity_uses_mod_sound(creature_id, event, source)
                subtitle_lang_missing = [
                    lang for lang, payload in lang_payloads.items()
                    if not subtitle or subtitle not in payload
                ]
                subtitle_ok = bool(subtitle) and not subtitle_lang_missing
                ok = registered_in_code and declared_in_json and playable_files and referenced_by_entity and subtitle_ok
                if not ok:
                    failures.append(f"{creature_id}.{event}: audio contract incomplete")
                if not subtitle_ok:
                    subtitle_failures.append(f"{creature_id}.{event}: missing subtitle lang {subtitle_lang_missing}")
                events.append({
                    "event": event,
                    "sound_key": key,
                    "constant": const,
                    "subtitle": subtitle,
                    "subtitle_lang_missing": subtitle_lang_missing,
                    "registered_in_code": registered_in_code,
                    "declared_in_json": declared_in_json,
                    "sound_refs": sound_refs,
                    "file_statuses": file_statuses,
                    "files_exist": files_exist,
                    "playable_files": playable_files,
                    "referenced_by_entity": referenced_by_entity,
                    "subtitle_ok": subtitle_ok,
                    "ok": ok,
                })
            creature_direction = art_creatures.get(creature_id, {}) if isinstance(art_creatures, dict) else {}
            direction_event_intents = creature_direction.get("event_intents", {}) if isinstance(creature_direction, dict) else {}
            direction_missing = []
            if not creature_direction:
                direction_missing.append("creature direction")
            for token in ["civilization", "final_asset_status", "sonic_identity", "acceptance_criteria"]:
                if not creature_direction.get(token):
                    direction_missing.append(token)
            for event in required_events(creature_id):
                if not direction_event_intents.get(event):
                    direction_missing.append(f"event_intents.{event}")
            if direction_missing:
                art_direction_failures.append(f"{creature_id}: missing " + ",".join(direction_missing))
            creature_ok = all(event["ok"] for event in events)
            if creature_id in final_mix_candidate_creatures:
                asset_class = "final_mix_candidate_pending_listening"
            elif creature_id in placeholder_repair_creatures:
                asset_class = "technical_placeholder_repair"
            elif creature_id in PROCEDURAL_FIRST_PASS_CREATURES:
                asset_class = "procedural_first_pass"
            else:
                asset_class = "authored_or_existing"
            creature_results.append({
                "id": creature_id,
                "civilization": civilization,
                "entity_source": project_path(source_path),
                "events": events,
                "art_direction": {
                    "exists": bool(creature_direction),
                    "path": project_path(AUDIO_ART_DIRECTION),
                    "missing": direction_missing,
                    "ok": not direction_missing,
                    "final_asset_status": creature_direction.get("final_asset_status"),
                },
                "asset_class": asset_class,
                "technical_ok": creature_ok,
                "art_final_ok": asset_class == "authored_or_existing" and creature_ok,
            })

    technical_ok = not failures
    art_direction_ok = not art_direction_failures
    subtitle_ok = not subtitle_failures
    art_final_pending = [
        creature["id"]
        for creature in creature_results
        if creature["asset_class"] != "authored_or_existing" and creature["technical_ok"]
    ]
    procedural_first_pass_pending = [
        creature["id"]
        for creature in creature_results
        if creature["asset_class"] == "procedural_first_pass" and creature["technical_ok"]
    ]
    technical_placeholder_repair_pending = [
        creature["id"]
        for creature in creature_results
        if creature["asset_class"] == "technical_placeholder_repair" and creature["technical_ok"]
    ]
    final_mix_candidate_pending = [
        creature["id"]
        for creature in creature_results
        if creature["asset_class"] == "final_mix_candidate_pending_listening" and creature["technical_ok"]
    ]
    return {
        "version": 1,
        "scope": "mythic_age_1_0_creature_audio_validation",
        "technical_ok": technical_ok,
        "ok": technical_ok and subtitle_ok and art_direction_ok,
        "art_direction_ok": art_direction_ok,
        "subtitle_ok": subtitle_ok,
        "art_final_ok": technical_ok and not art_final_pending,
        "art_direction": {
            "path": project_path(AUDIO_ART_DIRECTION),
            "scope": art_direction.get("scope") if isinstance(art_direction, dict) else None,
            "status": art_direction.get("status") if isinstance(art_direction, dict) else None,
            "creature_count": len(art_creatures) if isinstance(art_creatures, dict) else 0,
            "final_acceptance_shared": art_direction.get("final_acceptance_shared", []) if isinstance(art_direction, dict) else [],
            "failures": art_direction_failures,
        },
        "subtitle_failures": subtitle_failures,
        "civilizations": civilizations,
        "creature_count": len(creature_results),
        "event_count": sum(len(creature["events"]) for creature in creature_results),
        "art_final_pending_count": len(art_final_pending),
        "final_mix_candidate_count": len(final_mix_candidate_pending),
        "procedural_first_pass_count": len(procedural_first_pass_pending),
        "technical_placeholder_repair_count": len(technical_placeholder_repair_pending),
        "final_mix_candidate_report": {
            "path": project_path(AUDIO_FINAL_MIX_CANDIDATE_REPORT),
            "ok": final_mix_candidate_report.get("ok"),
            "generated_count": final_mix_candidate_report.get("generated_count"),
            "generated_creatures": sorted(final_mix_candidate_pending),
        },
        "placeholder_repair_report": {
            "path": project_path(AUDIO_PLACEHOLDER_REPAIR_REPORT),
            "ok": placeholder_repair_report.get("ok"),
            "generated_count": placeholder_repair_report.get("generated_count"),
            "generated_creatures": sorted(technical_placeholder_repair_pending),
        },
        "art_final_pending_creatures": art_final_pending,
        "failures": failures,
        "creatures": creature_results,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["audit"])
    args = parser.parse_args()
    if args.command == "audit":
        payload = build_report()
        REPORT_DIR.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        status = "PASS" if payload["ok"] else "FAIL"
        print(f"Mythic Age Audio Validation Gate: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"creatures: {payload['creature_count']}, events: {payload['event_count']}")
        if payload["failures"] or payload["art_direction"]["failures"]:
            print(f"failures: {len(payload['failures'])}")
            for failure in payload["failures"][:20]:
                print(f"failure: {failure}", file=sys.stderr)
            for failure in payload["art_direction"]["failures"][:20]:
                print(f"failure: {failure}", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
