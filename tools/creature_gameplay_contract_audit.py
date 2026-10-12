#!/usr/bin/env python3
"""Audit creature gameplay/code contracts for the MythicAgeMod 1.0 roster.

The visual gates answer "does it look right?" This gate answers a different
question: does every 1.0 creature have a gameplay/code loop that can be
released once art and manual acceptance are complete?
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs/preview_assets/creature_gameplay_contract"
REPORT_PATH = OUT_DIR / "creature_gameplay_contract.json"
MARKDOWN_PATH = ROOT / "docs/系统设计/MythicAgeMod_1.0生物玩法闭环契约_2026-06-15.md"

SRC = ROOT / "src/main/java/com/example/mythic_age"
ENTITY_DIR = SRC / "entity"
MODEL_DIR = SRC / "client/render/entity/model"
RENDERER_DIR = SRC / "client/render/entity"
MOD_ENTITIES = ENTITY_DIR / "ModEntities.java"
MOD_ITEMS = SRC / "item/ModItems.java"
CLIENT_INIT = SRC / "MythicAgeModClient.java"
SOUNDS_JSON = ROOT / "src/main/resources/assets/mythic_age/sounds.json"
TEXTURE_ROOT = ROOT / "src/main/resources/assets/mythic_age/textures/entity"
ITEM_MODEL_ROOT = ROOT / "src/main/resources/assets/mythic_age/models/item"
LANG_ROOT = ROOT / "src/main/resources/assets/mythic_age/lang"

CIVILIZATIONS = ["greek", "egyptian", "nordic", "chinese", "atlantean"]
MANIFESTS = {
    civ: ROOT / f"docs/美术资源/{civ}_creatures/visual_manifest.json"
    for civ in CIVILIZATIONS
}
CREATURE_DESIGN_FIRST_GATE = ROOT / "docs/preview_assets/creature_design_first_gate/creature_design_first_gate.json"
HUMAN_VISUAL_LEDGER = ROOT / "docs/preview_assets/creature_visual_acceptance/human_visual_acceptance_ledger.json"
MOTION_REWORK_QUEUE = ROOT / "docs/preview_assets/creature_motion_rework_queue/creature_motion_rework_queue.json"

CREATURE_OVERRIDES = {
    "nemean_lion": {"class": "NemeanLion"},
    "nine_tailed_fox": {"class": "NineTailedFox"},
}

BASE_REQUIRED_SOUND_EVENTS = ["ambient", "hurt", "death", "attack", "special"]
SOUND_EVENT_OVERRIDES = {
    "minotaur": ["ambient", "hurt", "death", "step", "charge"],
    "pegasus": ["ambient", "hurt", "death", "fly", "special"],
}


def project_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def snake_to_pascal(value: str) -> str:
    if value in CREATURE_OVERRIDES:
        return CREATURE_OVERRIDES[value]["class"]
    return "".join(part.capitalize() for part in value.split("_"))


def creature_roster() -> list[dict[str, Any]]:
    roster: list[dict[str, Any]] = []
    for civ, manifest_path in MANIFESTS.items():
        manifest = load_json(manifest_path, {})
        for item in manifest.get("creatures", []) if isinstance(manifest, dict) else []:
            if not isinstance(item, dict):
                continue
            creature_id = item.get("id")
            if isinstance(creature_id, str):
                roster.append({
                    "civilization": civ,
                    "id": creature_id,
                    "entity_id": item.get("entity_id"),
                    "display": item.get("display", creature_id),
                })
    return roster


def design_status_map() -> dict[tuple[str, str], dict[str, Any]]:
    payload = load_json(CREATURE_DESIGN_FIRST_GATE, {})
    result: dict[tuple[str, str], dict[str, Any]] = {}
    for item in payload.get("creatures", []) if isinstance(payload, dict) else []:
        if not isinstance(item, dict):
            continue
        civ = item.get("civilization")
        cid = item.get("id")
        if isinstance(civ, str) and isinstance(cid, str):
            result[(civ, cid)] = item
    return result


def visual_ledger_map() -> dict[tuple[str, str], dict[str, Any]]:
    payload = load_json(HUMAN_VISUAL_LEDGER, {})
    result: dict[tuple[str, str], dict[str, Any]] = {}
    for item in payload.get("creatures", []) if isinstance(payload, dict) else []:
        if not isinstance(item, dict):
            continue
        civ = item.get("civilization")
        cid = item.get("id")
        if isinstance(civ, str) and isinstance(cid, str):
            result[(civ, cid)] = item
    return result


def motion_blocked_set() -> set[tuple[str, str]]:
    payload = load_json(MOTION_REWORK_QUEUE, {})
    result: set[tuple[str, str]] = set()
    for item in payload.get("tasks", []) if isinstance(payload, dict) else []:
        if not isinstance(item, dict):
            continue
        civ = item.get("civilization")
        cid = item.get("creature_id") or item.get("id")
        state = item.get("state")
        if isinstance(civ, str) and isinstance(cid, str) and state == "blocked_by_art_or_design":
            result.add((civ, cid))
    return result


def sound_keys() -> set[str]:
    data = load_json(SOUNDS_JSON, {})
    return set(data.keys()) if isinstance(data, dict) else set()


def texture_path_for(creature_id: str) -> Path:
    nested = TEXTURE_ROOT / creature_id / f"{creature_id}.png"
    if nested.exists():
        return nested
    return TEXTURE_ROOT / f"{creature_id}.png"


def has_pattern(text: str, pattern: str) -> bool:
    return re.search(pattern, text, flags=re.MULTILINE | re.DOTALL) is not None


def audit_creature(
    creature: dict[str, Any],
    texts: dict[str, str],
    sounds: set[str],
    designs: dict[tuple[str, str], dict[str, Any]],
    visual_decisions: dict[tuple[str, str], dict[str, Any]],
    motion_blocked: set[tuple[str, str]],
) -> dict[str, Any]:
    cid = creature["id"]
    civ = creature["civilization"]
    class_base = snake_to_pascal(cid)
    constant = cid.upper()
    entity_file = ENTITY_DIR / f"{class_base}Entity.java"
    goal_file = ENTITY_DIR / "ai/goal" / f"{class_base}ChargeAttackGoal.java"
    model_file = MODEL_DIR / f"{class_base}Model.java"
    renderer_file = RENDERER_DIR / f"{class_base}EntityRenderer.java"
    texture_file = texture_path_for(cid)
    spawn_egg_model = ITEM_MODEL_ROOT / f"{cid}_spawn_egg.json"
    entity_text = read_text(entity_file)
    goal_text = read_text(goal_file)
    behavior_text = entity_text + "\n" + goal_text
    renderer_text = read_text(renderer_file)
    expected_sounds = SOUND_EVENT_OVERRIDES.get(cid, BASE_REQUIRED_SOUND_EVENTS)

    hostile_like = "AnimalEntity" not in entity_text
    expected_target_goal = hostile_like
    expected_special = cid != "pegasus"

    checks = {
        "entity_java_exists": entity_file.exists(),
        "registered_entity_type": f'"{cid}"' in texts["mod_entities"] and f"EntityType<{class_base}Entity>" in texts["mod_entities"],
        "attribute_factory_exists": has_pattern(entity_text, rf"create{class_base}Attributes\s*\("),
        "attributes_registered": f"FabricDefaultAttributeRegistry.register({constant}," in texts["mod_entities"],
        "natural_spawn_registered": f"BiomeModifications.addSpawn(" in texts["mod_entities"] and f"\n                {constant}," in texts["mod_entities"],
        "spawn_restriction_registered": f"SpawnRestriction.register({constant}," in texts["mod_entities"],
        "goals_initialized": "protected void initGoals()" in entity_text and "goalSelector.add" in entity_text,
        "target_goals_initialized": (not expected_target_goal) or "targetSelector.add" in entity_text,
        "drop_loot_overridden": "dropLoot(" in entity_text,
        "sound_overrides_or_role_sounds": all(f"entity.{cid}.{event}" in sounds for event in expected_sounds),
        "renderer_registered": f"EntityRendererRegistry.register(ModEntities.{constant}," in texts["client_init"],
        "model_layer_registered": f"EntityModelLayerRegistry.registerModelLayer(ModModelLayers.{constant}," in texts["client_init"],
        "renderer_file_exists": renderer_file.exists(),
        "model_file_exists": model_file.exists(),
        "texture_exists": texture_file.exists(),
        "spawn_egg_item_registered": f"{constant}_SPAWN_EGG" in texts["mod_items"] and f'"{cid}_spawn_egg"' in texts["mod_items"],
        "spawn_egg_item_model_exists": spawn_egg_model.exists(),
        "lang_en_spawn_egg": f"item.mythic_age.{cid}_spawn_egg" in texts["lang_en"],
        "lang_zh_spawn_egg": f"item.mythic_age.{cid}_spawn_egg" in texts["lang_zh"],
        "lang_ja_spawn_egg": f"item.mythic_age.{cid}_spawn_egg" in texts["lang_ja"],
    }

    role_checks: dict[str, bool] = {}
    if cid == "pegasus":
        role_checks = {
            "autonomous_flight_code": "tickAutonomousFlight" in entity_text and "GENERIC_FLYING_SPEED" in entity_text,
            "rider_flight_code": "travelWithRider" in entity_text and "startRiding" in entity_text,
            "fall_damage_disabled": "handleFallDamage" in entity_text and "return false" in entity_text,
            "flight_particles": "spawnFlightParticles" in entity_text,
        }
    else:
        role_checks = {
            "special_cooldown_or_unique_goal": (
                "canUseSpecial()" in behavior_text
                or "setSpecialCooldown" in behavior_text
                or "playSpecialSound" in behavior_text
                or "ChargeAttackGoal" in behavior_text
            ),
            "special_effect_or_particle": (
                "spawnParticles" in behavior_text
                or "StatusEffectInstance" in behavior_text
                or "ParticleTypes" in behavior_text
                or "damageAndKnockback" in behavior_text
                or "takeKnockback" in behavior_text
            ),
        }
    checks.update(role_checks)

    static_failures = [key for key, ok in checks.items() if not ok]
    static_ok = not static_failures

    design = designs.get((civ, cid), {})
    visual = visual_decisions.get((civ, cid), {})
    design_status = design.get("status")
    visual_decision = visual.get("decision")
    motion_design_blocked = (civ, cid) in motion_blocked

    release_blockers: list[str] = []
    if static_failures:
        release_blockers.append("static_code_contract_incomplete")
    if design_status != "approved_for_modeling":
        release_blockers.append(f"design_not_approved:{design_status}")
    if visual_decision != "approved":
        release_blockers.append(f"human_visual_not_approved:{visual_decision}")
    if motion_design_blocked:
        release_blockers.append("motion_blocked_by_art_or_design")
    if cid in {"pegasus", "hydra"}:
        release_blockers.append("current_design_rejected_v6_spec_pending")

    return {
        "civilization": civ,
        "id": cid,
        "display": creature.get("display"),
        "entity_id": creature.get("entity_id"),
        "class_base": class_base,
        "constant": constant,
        "expected_sound_events": expected_sounds,
        "paths": {
            "entity_java": project_path(entity_file),
            "goal_java": project_path(goal_file) if goal_file.exists() else None,
            "model_java": project_path(model_file),
            "renderer_java": project_path(renderer_file),
            "texture": project_path(texture_file),
            "spawn_egg_model": project_path(spawn_egg_model),
        },
        "checks": checks,
        "static_code_contract_ok": static_ok,
        "static_failures": static_failures,
        "design_status": design_status,
        "design_approved": design_status == "approved_for_modeling",
        "human_visual_decision": visual_decision,
        "human_visual_approved": visual_decision == "approved",
        "motion_blocked_by_art_or_design": motion_design_blocked,
        "release_gameplay_ready": not release_blockers,
        "release_blockers": release_blockers,
        "notes": [
            "Static code/resource evidence is necessary but not sufficient for 1.0 release.",
            "Design, visual, motion, and manual gameplay acceptance must still pass.",
        ],
    }


def build_report() -> dict[str, Any]:
    roster = creature_roster()
    texts = {
        "mod_entities": read_text(MOD_ENTITIES),
        "mod_items": read_text(MOD_ITEMS),
        "client_init": read_text(CLIENT_INIT),
        "lang_en": read_text(LANG_ROOT / "en_us.json"),
        "lang_zh": read_text(LANG_ROOT / "zh_cn.json"),
        "lang_ja": read_text(LANG_ROOT / "ja_jp.json"),
    }
    designs = design_status_map()
    visual_decisions = visual_ledger_map()
    motion_blocked = motion_blocked_set()
    keys = sound_keys()
    creatures = [
        audit_creature(creature, texts, keys, designs, visual_decisions, motion_blocked)
        for creature in roster
    ]

    static_fail_counter: Counter[str] = Counter()
    release_blocker_counter: Counter[str] = Counter()
    for item in creatures:
        static_fail_counter.update(item["static_failures"])
        release_blocker_counter.update(item["release_blockers"])

    static_ok_count = sum(1 for item in creatures if item["static_code_contract_ok"])
    release_ready_count = sum(1 for item in creatures if item["release_gameplay_ready"])
    by_civilization: dict[str, dict[str, Any]] = {}
    for civ in CIVILIZATIONS:
        civ_items = [item for item in creatures if item["civilization"] == civ]
        by_civilization[civ] = {
            "creature_count": len(civ_items),
            "static_code_contract_ok_count": sum(1 for item in civ_items if item["static_code_contract_ok"]),
            "release_gameplay_ready_count": sum(1 for item in civ_items if item["release_gameplay_ready"]),
            "static_failures": dict(Counter(failure for item in civ_items for failure in item["static_failures"])),
            "release_blockers": dict(Counter(blocker for item in civ_items for blocker in item["release_blockers"])),
        }

    return {
        "version": 1,
        "scope": "mythic_age_1_0_creature_gameplay_code_contract",
        "ok": release_ready_count == len(creatures) and bool(creatures),
        "static_code_contract_ok": static_ok_count == len(creatures) and bool(creatures),
        "creature_count": len(creatures),
        "static_code_contract_ok_count": static_ok_count,
        "release_gameplay_ready_count": release_ready_count,
        "static_failure_counts": dict(sorted(static_fail_counter.items())),
        "release_blocker_counts": dict(sorted(release_blocker_counter.items())),
        "by_civilization": by_civilization,
        "rule": (
            "A creature is 1.0 gameplay-ready only when static code/resource contracts, "
            "design approval, human visual approval, and motion readiness all pass."
        ),
        "creatures": creatures,
    }


def write_markdown(payload: dict[str, Any]) -> None:
    lines = [
        "# MythicAgeMod 1.0 生物玩法闭环契约（2026-06-15）",
        "",
        "本文档由 `tools/creature_gameplay_contract_audit.py audit` 生成。它检查 30 个 1.0 生物是否不仅有模型/纹理，还具备代码玩法闭环：注册、属性、自然生成、AI、特殊能力、掉落、声音、渲染、模型、纹理、刷怪蛋和发布验收依赖。",
        "",
        "## 总览",
        "",
        f"- 发布玩法闭环通过：`{payload['ok']}`",
        f"- 静态代码契约通过：`{payload['static_code_contract_ok']}`",
        f"- 生物数量：{payload['creature_count']}",
        f"- 静态代码契约通过数：{payload['static_code_contract_ok_count']}",
        f"- 发布玩法 ready 数：{payload['release_gameplay_ready_count']}",
        f"- 静态失败统计：`{json.dumps(payload['static_failure_counts'], ensure_ascii=False)}`",
        f"- 发布阻断统计：`{json.dumps(payload['release_blocker_counts'], ensure_ascii=False)}`",
        f"- 判定规则：{payload['rule']}",
        "",
        "## 文明摘要",
        "",
        "| 文明 | 生物数 | 静态通过 | 发布 ready | 静态失败 | 发布阻断 |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for civ, item in payload["by_civilization"].items():
        lines.append(
            "| `{civ}` | {count} | {static_ok} | {release_ready} | `{static_fail}` | `{release_block}` |".format(
                civ=civ,
                count=item["creature_count"],
                static_ok=item["static_code_contract_ok_count"],
                release_ready=item["release_gameplay_ready_count"],
                static_fail=json.dumps(item["static_failures"], ensure_ascii=False),
                release_block=json.dumps(item["release_blockers"], ensure_ascii=False),
            )
        )
    lines.extend([
        "",
        "## 生物矩阵",
        "",
        "| 文明 | 生物 | 静态代码 | 发布 ready | 设计状态 | 视觉结论 | 静态失败 | 发布阻断 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ])
    for item in payload["creatures"]:
        lines.append(
            "| `{civ}` | `{cid}` | `{static}` | `{ready}` | `{design}` | `{visual}` | `{failures}` | `{blockers}` |".format(
                civ=item["civilization"],
                cid=item["id"],
                static=item["static_code_contract_ok"],
                ready=item["release_gameplay_ready"],
                design=item["design_status"],
                visual=item["human_visual_decision"],
                failures=json.dumps(item["static_failures"], ensure_ascii=False),
                blockers=json.dumps(item["release_blockers"], ensure_ascii=False),
            )
        )
    lines.extend(["", "## 需要处理的静态缺口", ""])
    for item in payload["creatures"]:
        if item["static_code_contract_ok"]:
            continue
        lines.extend([
            f"### {item['civilization']} / {item['id']}",
            "",
            f"- 静态失败：`{json.dumps(item['static_failures'], ensure_ascii=False)}`",
            f"- 路径：`{json.dumps(item['paths'], ensure_ascii=False)}`",
            "",
        ])
    lines.extend([
        "## 退出标准",
        "",
        "- 30 个生物 `static_code_contract_ok=true`。",
        "- 30 个生物设计门禁为 `approved_for_modeling`。",
        "- 30 个生物人工视觉结论为 `approved`。",
        "- 动作/动画队列中不再有 art/design 阻断，并完成必要连续帧/clip 验收。",
        "- Pegasus/Hydra 必须先签收 V6 可建模规格，再完成模型和动作重建；不能用当前 V2 代码功能抵消视觉设计失败。",
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
        print(f"Creature Gameplay Contract: {status}")
        print(f"report: {REPORT_PATH}")
        print(f"markdown: {MARKDOWN_PATH}")
        print(f"static_code_contract_ok: {payload['static_code_contract_ok']}")
        print(f"static_ok: {payload['static_code_contract_ok_count']}/{payload['creature_count']}")
        print(f"release_ready: {payload['release_gameplay_ready_count']}/{payload['creature_count']}")
        print(f"static_failure_counts: {payload['static_failure_counts']}")
        print(f"release_blocker_counts: {payload['release_blocker_counts']}")
        return 0 if payload["ok"] else 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
