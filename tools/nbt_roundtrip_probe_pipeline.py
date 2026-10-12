#!/usr/bin/env python3
"""Source-level NBT round-trip probe for Mythic Age world persistent state.

This is intentionally not an MC client/server launcher. It checks whether the
long-lived systems that moved into MythicAgeWorldState have symmetric write/read
keys and whether their mutation entry points mark the persistent state dirty.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/nbt_roundtrip_validation"
REPORT_PATH = REPORT_DIR / "nbt_roundtrip_report.json"
WORLD_STATE_PATH = ROOT / "src/main/java/com/example/mythic_age/world/MythicAgeWorldState.java"

SYSTEMS: list[dict[str, Any]] = [
    {
        "id": "lightning_storms",
        "owner": "LightningStormManager",
        "path": "src/main/java/com/example/mythic_age/util/LightningStormManager.java",
        "world_key": "LightningStorms",
        "list_key": "ActiveStorms",
        "map_field": "activeStorms",
        "tick_call": "lightningStormManager.tick(world)",
        "constructor_clear": "lightningStormManager.readPersistentState(new NbtCompound())",
        "mutation_methods": ["startLightningStorm"],
        "record_keys": ["Id", "Dimension", "Center", "TicksAlive", "RemainingTicks", "LightningTimer"],
    },
    {
        "id": "earthquakes",
        "owner": "EarthquakeManager",
        "path": "src/main/java/com/example/mythic_age/util/EarthquakeManager.java",
        "world_key": "Earthquakes",
        "list_key": "ActiveEarthquakes",
        "map_field": "activeEarthquakes",
        "tick_call": "earthquakeManager.tick(world)",
        "constructor_clear": "earthquakeManager.readPersistentState(new NbtCompound())",
        "mutation_methods": ["startEarthquake"],
        "record_keys": ["Id", "Dimension", "Epicenter", "TicksAlive", "RemainingTicks", "IntervalTimer", "PlayerYaw"],
    },
    {
        "id": "god_power_weather",
        "owner": "GodPowerWeatherManager",
        "path": "src/main/java/com/example/mythic_age/util/GodPowerWeatherManager.java",
        "world_key": "GodPowerWeather",
        "list_key": "ActiveWeatherPowers",
        "map_field": "activeWeatherPowers",
        "tick_call": "godPowerWeatherManager.tick(world)",
        "constructor_clear": "godPowerWeatherManager.readPersistentState(new NbtCompound())",
        "mutation_methods": ["startWeatherPower"],
        "record_keys": ["Id", "Type", "Dimension", "Center", "Radius", "DurationTicks", "TicksAlive", "RemainingTicks"],
    },
    {
        "id": "god_power_certificates",
        "owner": "GodPowerCertificateManager",
        "path": "src/main/java/com/example/mythic_age/system/GodPowerCertificateManager.java",
        "world_key": "GodPowerCertificates",
        "list_key": "CivilizationProductionState",
        "map_field": "civilizationProductionState",
        "tick_call": None,
        "constructor_clear": "godPowerCertificateManager.readPersistentState(new NbtCompound())",
        "mutation_methods": ["recordCivilizationProduction"],
        "read_methods_requiring_state": ["canGenerateCivilizationProduction"],
        "record_keys": ["Key", "SourceType", "Civilization", "Dimension", "SourcePos", "LastGenerationTime", "GeneratedAmount"],
    },
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def read_text(relative_or_abs: str | Path) -> str:
    path = Path(relative_or_abs)
    if not path.is_absolute():
        path = ROOT / path
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def has_put(text: str, key: str) -> bool:
    return re.search(rf"\.put(?:String|Int|Long|Boolean|Float|Uuid)?\(\"{re.escape(key)}\"", text) is not None


def has_get_or_contains(text: str, key: str) -> bool:
    patterns = [
        rf"\.get(?:String|Int|Long|Boolean|Float|Uuid|Compound|List)?\(\"{re.escape(key)}\"",
        rf"\.contains(?:Uuid)?\(\"{re.escape(key)}\"",
    ]
    return any(re.search(pattern, text) for pattern in patterns)


def extract_method(text: str, method_name: str) -> str:
    match = re.search(
        rf"(?:public|private|protected)\s+[A-Za-z0-9_<>, ?\[\]]+\s+{re.escape(method_name)}\s*\(",
        text,
    )
    if not match:
        return ""

    brace_start = text.find("{", match.end())
    if brace_start == -1:
        return ""

    depth = 0
    for index in range(brace_start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return text[brace_start:index + 1]
    return ""


def check_world_state(world_text: str) -> dict[str, Any]:
    failures: list[str] = []
    systems: list[dict[str, Any]] = []
    for system in SYSTEMS:
        world_key = system["world_key"]
        written = has_put(world_text, world_key)
        read = has_get_or_contains(world_text, world_key)
        constructor_clear = system["constructor_clear"] in world_text
        tick_call = True if system["tick_call"] is None else system["tick_call"] in world_text
        ok = written and read and constructor_clear and tick_call
        if not ok:
            failures.append(f"MythicAgeWorldState missing contract for {world_key}")
        systems.append({
            "id": system["id"],
            "world_key": world_key,
            "written": written,
            "read": read,
            "constructor_clear": constructor_clear,
            "tick_call_present": tick_call,
            "ok": ok,
        })

    tick_method = extract_method(world_text, "tick")
    dirty_aggregation_ok = "boolean dirty = false" in tick_method and "markDirty()" in tick_method
    if not dirty_aggregation_ok:
        failures.append("MythicAgeWorldState.tick does not aggregate dirty manager ticks before markDirty")

    return {
        "path": project_path(WORLD_STATE_PATH),
        "exists": WORLD_STATE_PATH.exists(),
        "dirty_aggregation_ok": dirty_aggregation_ok,
        "systems": systems,
        "ok": not failures,
        "failures": failures,
    }


def check_manager(system: dict[str, Any]) -> dict[str, Any]:
    path = ROOT / system["path"]
    text = read_text(path)
    failures: list[str] = []
    key_results: list[dict[str, Any]] = []

    if not path.exists():
        return {
            "id": system["id"],
            "owner": system["owner"],
            "path": system["path"],
            "exists": False,
            "ok": False,
            "failures": [f"missing manager source: {system['path']}"],
        }

    list_key = system["list_key"]
    root_written = has_put(text, list_key)
    root_read = has_get_or_contains(text, list_key)
    if not root_written or not root_read:
        failures.append(f"{system['owner']} root list {list_key} is not write/read symmetric")

    for key in system["record_keys"]:
        written = has_put(text, key)
        read = has_get_or_contains(text, key)
        ok = written and read
        if not ok:
            failures.append(f"{system['owner']} record key {key} is not write/read symmetric")
        key_results.append({
            "key": key,
            "written": written,
            "read_or_checked": read,
            "ok": ok,
        })

    mutation_results: list[dict[str, Any]] = []
    for method_name in system.get("mutation_methods", []):
        method = extract_method(text, method_name)
        state_get = "MythicAgeWorldState.get" in method
        mark_dirty = ".markDirty()" in method
        map_put_token = f"{system['map_field']}.put"
        map_put = map_put_token in method
        state_get_before_put = state_get and map_put and method.find("MythicAgeWorldState.get") < method.find(map_put_token)
        ok = state_get and mark_dirty and map_put and state_get_before_put
        if not ok:
            failures.append(f"{system['owner']}.{method_name} does not load state before mutation and mark dirty")
        mutation_results.append({
            "method": method_name,
            "found": bool(method),
            "state_get": state_get,
            "map_put": map_put,
            "state_get_before_put": state_get_before_put,
            "mark_dirty": mark_dirty,
            "ok": ok,
        })

    read_results: list[dict[str, Any]] = []
    for method_name in system.get("read_methods_requiring_state", []):
        method = extract_method(text, method_name)
        state_get = "MythicAgeWorldState.get" in method
        map_get = f"{system['map_field']}.get" in method
        state_get_before_read = state_get and map_get and method.find("MythicAgeWorldState.get") < method.find(f"{system['map_field']}.get")
        ok = state_get and map_get and state_get_before_read
        if not ok:
            failures.append(f"{system['owner']}.{method_name} does not load state before reading persisted map")
        read_results.append({
            "method": method_name,
            "found": bool(method),
            "state_get": state_get,
            "map_get": map_get,
            "state_get_before_read": state_get_before_read,
            "ok": ok,
        })

    expired_filter_present = (
        "ticksAlive >= " in text
        or "durationTicks" in text and "ticksAlive >= durationTicks" in text
        or system["id"] == "god_power_certificates"
    )
    if not expired_filter_present:
        failures.append(f"{system['owner']} does not filter expired restored records")

    return {
        "id": system["id"],
        "owner": system["owner"],
        "path": system["path"],
        "exists": True,
        "list_key": {
            "key": list_key,
            "written": root_written,
            "read_or_checked": root_read,
            "ok": root_written and root_read,
        },
        "record_keys": key_results,
        "mutation_methods": mutation_results,
        "read_methods_requiring_state": read_results,
        "expired_filter_present": expired_filter_present,
        "ok": not failures,
        "failures": failures,
    }


def build_report() -> dict[str, Any]:
    world_text = read_text(WORLD_STATE_PATH)
    world_state = check_world_state(world_text)
    managers = [check_manager(system) for system in SYSTEMS]
    failures = list(world_state["failures"])
    for manager in managers:
        failures.extend(manager.get("failures", []))

    return {
        "version": 1,
        "scope": "source_level_nbt_roundtrip_probe",
        "ok": not failures,
        "probe_type": "source_static_contract",
        "mc_restart_probe": "not_run_by_request",
        "limitations": [
            "Does not launch Minecraft or deserialize live world saves.",
            "Checks source-level symmetry and dirty-state wiring for long-lived NBT contracts.",
        ],
        "world_state": world_state,
        "managers": managers,
        "failures": failures,
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
        print(f"Mythic Age NBT Roundtrip Source Probe: {status}")
        print(f"report: {REPORT_PATH}")
        if payload["failures"]:
            for failure in payload["failures"]:
                print(f"failure: {failure}", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
