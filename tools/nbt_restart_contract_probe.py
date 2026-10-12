#!/usr/bin/env python3
"""Restart-oriented NBT contract probe for entity and block-entity state.

This does not launch Minecraft. It statically verifies the save/reload contract
for state that must survive a server save and restart: custom guardian trades,
trade cooldowns, civilization identity, temple positions, and key block entity
storage fields.
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
REPORT_PATH = REPORT_DIR / "nbt_restart_contract_report.json"
VALIDATION_COMMAND = ROOT / "src/main/java/com/example/mythic_age/validation/ValidationCommand.java"
VISUAL_PROBE_SERVER = ROOT / "src/main/java/com/example/mythic_age/client/visual/VisualProbeServer.java"
MC_VISUAL_PROBE_MCP = ROOT / "tools/mc_visual_probe_mcp.py"


ENTITY_CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "custom_temple_guardian",
        "owner": "CustomTempleGuardianEntity",
        "path": "src/main/java/com/example/mythic_age/entity/CustomTempleGuardianEntity.java",
        "write_method": "writeCustomDataToNbt",
        "read_method": "readCustomDataFromNbt",
        "keys": [
            "mythologyType",
            "certificateGenerationTimer",
            "hasInitializedTrades",
            "templePosX",
            "templePosY",
            "templePosZ",
            "tradeOffers",
            "tradeCount",
            "tradeCooldowns",
        ],
        "required_tokens": [
            "GodPowerCertificateItem.MythologyType.valueOf",
            "refreshCivilizationName()",
            "tradeOffers.clear()",
            "ItemStack.fromNbt",
            "tradeCooldowns.clear()",
        ],
        "mutation_tokens": [
            "tradeCooldowns.put",
            "this.getWorld().getTime() + MYTHIC_CREATURE_TRADE_COOLDOWN",
        ],
    },
]


BLOCK_ENTITY_CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "statue_base",
        "owner": "StatueBaseBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/StatueBaseBlockEntity.java",
        "write_method": "writeNbt",
        "read_method": "readNbt",
        "keys": ["StoredCertificates", "LastUpdateTime"],
        "mutation_tokens": ["markDirty()"],
    },
    {
        "id": "relic_pedestal",
        "owner": "RelicPedestalBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/RelicPedestalBlockEntity.java",
        "write_method": "writeNbt",
        "read_method": "readNbt",
        "keys": ["RelicItem", "IsActiveTemple"],
        "required_tokens": ["ItemStack.fromNbt", "relicStack.writeNbt"],
        "mutation_tokens": ["markDirty()"],
    },
    {
        "id": "egyptian_statue_pedestal",
        "owner": "EgyptianStatuePedestalBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/EgyptianStatuePedestalBlockEntity.java",
        "write_method": "writeNbt",
        "read_method": "readNbt",
        "keys": ["hasStatue", "productionTick", "statueType"],
        "required_tokens": ["EgyptianStatueType.values()", "type.getName().equals(typeName)"],
        "mutation_tokens": ["markDirty()"],
    },
    {
        "id": "abundance_altar_core",
        "owner": "AbundanceAltarCoreBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/AbundanceAltarCoreBlockEntity.java",
        "write_method": "writeNbt",
        "read_method": "readNbt",
        "keys": ["NextGenerationTime"],
        "mutation_tokens": ["nextGenerationTime", "markDirty()"],
    },
    {
        "id": "animal_lure_tower",
        "owner": "AnimalLureTowerBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/AnimalLureTowerBlockEntity.java",
        "write_method": "writeNbt",
        "read_method": "readNbt",
        "keys": [],
        "status": "runtime_only_no_persistent_fields",
        "required_tokens": ["super.readNbt(nbt)", "super.writeNbt(nbt)"],
    },
]


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


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


def has_write(method: str, key: str) -> bool:
    return re.search(rf"\.put(?:String|Int|Long|Boolean)?\(\"{re.escape(key)}\"", method) is not None


def has_read(method: str, key: str) -> bool:
    patterns = [
        rf"\.get(?:String|Int|Long|Boolean|Compound)?\(\"{re.escape(key)}\"",
        rf"\.contains\(\"{re.escape(key)}\"",
    ]
    return any(re.search(pattern, method) for pattern in patterns)


def check_contract(contract: dict[str, Any]) -> dict[str, Any]:
    path = ROOT / contract["path"]
    text = read_text(path)
    write_method = extract_method(text, contract["write_method"])
    read_method = extract_method(text, contract["read_method"])
    failures: list[str] = []

    if not path.exists():
        return {
            "id": contract["id"],
            "owner": contract["owner"],
            "path": contract["path"],
            "exists": False,
            "ok": False,
            "failures": [f"missing source: {contract['path']}"],
        }

    if not write_method:
        failures.append(f"{contract['owner']} missing {contract['write_method']}")
    if not read_method:
        failures.append(f"{contract['owner']} missing {contract['read_method']}")

    key_results = []
    for key in contract.get("keys", []):
        written = has_write(write_method, key)
        read = has_read(read_method, key)
        ok = written and read
        if not ok:
            failures.append(f"{contract['owner']} key {key} is not write/read symmetric")
        key_results.append({
            "key": key,
            "written": written,
            "read_or_checked": read,
            "ok": ok,
        })

    required_tokens = []
    for token in contract.get("required_tokens", []):
        present = token in text
        if not present:
            failures.append(f"{contract['owner']} missing restart token: {token}")
        required_tokens.append({"token": token, "present": present})

    mutation_tokens = []
    for token in contract.get("mutation_tokens", []):
        present = token in text
        if not present:
            failures.append(f"{contract['owner']} missing mutation/dirty token: {token}")
        mutation_tokens.append({"token": token, "present": present})

    return {
        "id": contract["id"],
        "owner": contract["owner"],
        "path": contract["path"],
        "exists": True,
        "status": contract.get("status", "persistent_contract"),
        "write_method_found": bool(write_method),
        "read_method_found": bool(read_method),
        "keys": key_results,
        "required_tokens": required_tokens,
        "mutation_tokens": mutation_tokens,
        "ok": not failures,
        "failures": failures,
    }


def check_runtime_probe_entrypoints() -> dict[str, Any]:
    validation_text = read_text(VALIDATION_COMMAND)
    visual_probe_text = read_text(VISUAL_PROBE_SERVER)
    mcp_text = read_text(MC_VISUAL_PROBE_MCP)
    checks = [
        {
            "id": "server_command_registered",
            "path": project_path(VALIDATION_COMMAND),
            "ok": "CommandManager.literal(\"nbt_restart\")" in validation_text,
        },
        {
            "id": "runtime_report_writer_present",
            "path": project_path(VALIDATION_COMMAND),
            "ok": "writeRestartNbtReport" in validation_text and "RESTART_REPORT_FILE" in validation_text,
        },
        {
            "id": "guardian_runtime_roundtrip_present",
            "path": project_path(VALIDATION_COMMAND),
            "ok": "validateCustomGuardianRestart" in validation_text and "tradeCooldowns" in validation_text,
        },
        {
            "id": "block_entity_runtime_roundtrip_present",
            "path": project_path(VALIDATION_COMMAND),
            "ok": (
                "validateStatueBaseRestart" in validation_text
                and "validateRelicPedestalRestart" in validation_text
                and "validateEgyptianStatuePedestalRestart" in validation_text
                and "validateAbundanceAltarRestart" in validation_text
            ),
        },
        {
            "id": "visual_probe_http_endpoint_present",
            "path": project_path(VISUAL_PROBE_SERVER),
            "ok": "/validate_nbt_restart" in visual_probe_text and "handleValidateNbtRestart" in visual_probe_text,
        },
        {
            "id": "true_restart_cycle_commands_present",
            "path": project_path(VALIDATION_COMMAND),
            "ok": (
                "nbt_restart_prepare" in validation_text
                and "nbt_restart_verify" in validation_text
                and "writeRestartCyclePrepareReport" in validation_text
                and "writeRestartCycleVerifyReport" in validation_text
            ),
        },
        {
            "id": "mcp_restart_cycle_tools_present",
            "path": project_path(MC_VISUAL_PROBE_MCP),
            "ok": (
                "run_runtime_nbt_restart_validation" in mcp_text
                and "run_runtime_nbt_restart_prepare" in mcp_text
                and "run_runtime_nbt_restart_verify" in mcp_text
            ),
        },
    ]
    failures = [
        f"missing runtime restart probe entrypoint: {check['id']}"
        for check in checks
        if not check["ok"]
    ]
    return {
        "ok": not failures,
        "checks": checks,
        "failures": failures,
    }


def build_report() -> dict[str, Any]:
    entities = [check_contract(contract) for contract in ENTITY_CONTRACTS]
    block_entities = [check_contract(contract) for contract in BLOCK_ENTITY_CONTRACTS]
    runtime_entrypoints = check_runtime_probe_entrypoints()
    failures = [
        failure
        for section in (entities, block_entities)
        for item in section
        for failure in item.get("failures", [])
    ]
    failures.extend(runtime_entrypoints["failures"])
    return {
        "version": 1,
        "scope": "restart_semantic_nbt_contract_probe",
        "ok": not failures,
        "probe_type": "source_static_restart_contract",
        "mc_restart_probe": "not_run_by_request",
        "coverage": {
            "entities": len(entities),
            "block_entities": len(block_entities),
            "persistent_block_entities": sum(1 for item in block_entities if item.get("status") == "persistent_contract"),
            "runtime_entrypoints": len(runtime_entrypoints["checks"]),
        },
        "entities": entities,
        "block_entities": block_entities,
        "runtime_probe_entrypoints": runtime_entrypoints,
        "limitations": [
            "Does not launch Minecraft or deserialize a live saved world.",
            "Checks the source contracts that make live save/restart recovery possible.",
        ],
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
        print(f"Mythic Age NBT Restart Contract Probe: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"entities: {payload['coverage']['entities']}, block_entities: {payload['coverage']['block_entities']}")
        if payload["failures"]:
            for failure in payload["failures"]:
                print(f"failure: {failure}", file=sys.stderr)
            return 1
        return 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
