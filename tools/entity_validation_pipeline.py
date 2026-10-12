#!/usr/bin/env python3
"""Whole-entity visual scope and NBT schema gate for Mythic Age."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "docs/preview_assets/entity_validation"
MANIFEST_PATH = ROOT / "docs/美术资源/entity_validation/entity_manifest.json"
NBT_REPORT_PATH = REPORT_DIR / "nbt_schema_report.json"
ACTION_PARTICLE_REPORT_PATH = REPORT_DIR / "action_particle_report.json"
MC_NBT_REPORT_PATH = ROOT / "run/visual_probe/entity_nbt_report.json"
MOD_ENTITIES = ROOT / "src/main/java/com/example/mythic_age/entity/ModEntities.java"
MOD_ENTITY_TYPES = ROOT / "src/main/java/com/example/mythic_age/entity/ModEntityTypes.java"
SRC_ROOT = ROOT / "src/main/java/com/example/mythic_age"
GREEK_VISUAL_MANIFEST = ROOT / "docs/美术资源/greek_creatures/visual_manifest.json"
EGYPTIAN_VISUAL_MANIFEST = ROOT / "docs/美术资源/egyptian_creatures/visual_manifest.json"
NORDIC_VISUAL_MANIFEST = ROOT / "docs/美术资源/nordic_creatures/visual_manifest.json"
CHINESE_VISUAL_MANIFEST = ROOT / "docs/美术资源/chinese_creatures/visual_manifest.json"
ATLANTEAN_VISUAL_MANIFEST = ROOT / "docs/美术资源/atlantean_creatures/visual_manifest.json"
GREEK_RUNTIME_MODEL_REPORT = ROOT / "docs/preview_assets/greek_creatures/runtime_model_quality_report.json"
EGYPTIAN_RUNTIME_MODEL_REPORT = ROOT / "docs/preview_assets/egyptian_creatures/runtime_model_quality_report.json"
NORDIC_RUNTIME_MODEL_REPORT = ROOT / "docs/preview_assets/nordic_creatures/runtime_model_quality_report.json"
CHINESE_RUNTIME_MODEL_REPORT = ROOT / "docs/preview_assets/chinese_creatures/runtime_model_quality_report.json"
ATLANTEAN_RUNTIME_MODEL_REPORT = ROOT / "docs/preview_assets/atlantean_creatures/runtime_model_quality_report.json"

VISUAL_GATE_MANIFESTS = {
    "greek_design_gate": GREEK_VISUAL_MANIFEST,
    "egyptian_design_gate": EGYPTIAN_VISUAL_MANIFEST,
    "nordic_design_gate": NORDIC_VISUAL_MANIFEST,
    "chinese_design_gate": CHINESE_VISUAL_MANIFEST,
    "atlantean_design_gate": ATLANTEAN_VISUAL_MANIFEST,
}


CUSTOM_ENTITIES: list[dict[str, Any]] = [
    {"id": "minotaur", "entity_id": "mythic_age:minotaur", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "cyclops", "entity_id": "mythic_age:cyclops", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "centaur", "entity_id": "mythic_age:centaur", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "hydra", "entity_id": "mythic_age:hydra", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "medusa", "entity_id": "mythic_age:medusa", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "nemean_lion", "entity_id": "mythic_age:nemean_lion", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "manticore", "entity_id": "mythic_age:manticore", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "chimera", "entity_id": "mythic_age:chimera", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "colossus", "entity_id": "mythic_age:colossus", "kind": "creature", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "pegasus", "entity_id": "mythic_age:pegasus", "kind": "creature_mount", "visual_gate": "greek_design_gate", "nbt": "vanilla_only"},
    {"id": "anubis_warrior", "entity_id": "mythic_age:anubis_warrior", "kind": "egyptian_creature", "visual_gate": "egyptian_design_gate", "nbt": "vanilla_only"},
    {"id": "scarab_guardian", "entity_id": "mythic_age:scarab_guardian", "kind": "egyptian_creature", "visual_gate": "egyptian_design_gate", "nbt": "vanilla_only"},
    {"id": "mummy_soldier", "entity_id": "mythic_age:mummy_soldier", "kind": "egyptian_creature", "visual_gate": "egyptian_design_gate", "nbt": "vanilla_only"},
    {"id": "sphinx_sentinel", "entity_id": "mythic_age:sphinx_sentinel", "kind": "egyptian_creature", "visual_gate": "egyptian_design_gate", "nbt": "vanilla_only"},
    {"id": "sun_falcon", "entity_id": "mythic_age:sun_falcon", "kind": "egyptian_flying_creature", "visual_gate": "egyptian_design_gate", "nbt": "vanilla_only"},
    {"id": "frost_giant", "entity_id": "mythic_age:frost_giant", "kind": "nordic_creature", "visual_gate": "nordic_design_gate", "nbt": "vanilla_only"},
    {"id": "draugr_warrior", "entity_id": "mythic_age:draugr_warrior", "kind": "nordic_creature", "visual_gate": "nordic_design_gate", "nbt": "vanilla_only"},
    {"id": "valkyrie", "entity_id": "mythic_age:valkyrie", "kind": "nordic_flying_creature", "visual_gate": "nordic_design_gate", "nbt": "vanilla_only"},
    {"id": "fenrir_wolf", "entity_id": "mythic_age:fenrir_wolf", "kind": "nordic_creature", "visual_gate": "nordic_design_gate", "nbt": "vanilla_only"},
    {"id": "dwarf_berserker", "entity_id": "mythic_age:dwarf_berserker", "kind": "nordic_creature", "visual_gate": "nordic_design_gate", "nbt": "vanilla_only"},
    {"id": "azure_dragon", "entity_id": "mythic_age:azure_dragon", "kind": "chinese_flying_creature", "visual_gate": "chinese_design_gate", "nbt": "vanilla_only"},
    {"id": "qilin_guardian", "entity_id": "mythic_age:qilin_guardian", "kind": "chinese_creature", "visual_gate": "chinese_design_gate", "nbt": "vanilla_only"},
    {"id": "jiangshi", "entity_id": "mythic_age:jiangshi", "kind": "chinese_creature", "visual_gate": "chinese_design_gate", "nbt": "vanilla_only"},
    {"id": "nine_tailed_fox", "entity_id": "mythic_age:nine_tailed_fox", "kind": "chinese_creature", "visual_gate": "chinese_design_gate", "nbt": "vanilla_only"},
    {"id": "stone_lion_guardian", "entity_id": "mythic_age:stone_lion_guardian", "kind": "chinese_creature", "visual_gate": "chinese_design_gate", "nbt": "vanilla_only"},
    {"id": "crystal_leviathan", "entity_id": "mythic_age:crystal_leviathan", "kind": "atlantean_creature", "visual_gate": "atlantean_design_gate", "nbt": "vanilla_only"},
    {"id": "orichalcum_golem", "entity_id": "mythic_age:orichalcum_golem", "kind": "atlantean_creature", "visual_gate": "atlantean_design_gate", "nbt": "vanilla_only"},
    {"id": "siren_priestess", "entity_id": "mythic_age:siren_priestess", "kind": "atlantean_creature", "visual_gate": "atlantean_design_gate", "nbt": "vanilla_only"},
    {"id": "tidal_guardian", "entity_id": "mythic_age:tidal_guardian", "kind": "atlantean_creature", "visual_gate": "atlantean_design_gate", "nbt": "vanilla_only"},
    {"id": "abyssal_ray", "entity_id": "mythic_age:abyssal_ray", "kind": "atlantean_creature", "visual_gate": "atlantean_design_gate", "nbt": "vanilla_only"},
    {"id": "walking_tree", "entity_id": "mythic_age:walking_tree", "kind": "summoned_creature", "visual_gate": "existing_blockbench_gate", "nbt": "custom_entity"},
    {"id": "temple_guardian", "entity_id": "mythic_age:temple_guardian", "kind": "guardian", "visual_gate": "renderer_smoke", "nbt": "vanilla_villager_plus_logic"},
    {"id": "temple_guardian_greek", "entity_id": "mythic_age:temple_guardian_greek", "kind": "guardian", "visual_gate": "renderer_smoke", "nbt": "vanilla_villager_plus_logic"},
    {"id": "temple_guardian_egyptian", "entity_id": "mythic_age:temple_guardian_egyptian", "kind": "guardian", "visual_gate": "renderer_smoke", "nbt": "vanilla_villager_plus_logic"},
    {"id": "temple_guardian_chinese", "entity_id": "mythic_age:temple_guardian_chinese", "kind": "guardian", "visual_gate": "renderer_smoke", "nbt": "vanilla_villager_plus_logic"},
    {"id": "temple_guardian_atlantean", "entity_id": "mythic_age:temple_guardian_atlantean", "kind": "guardian", "visual_gate": "renderer_smoke", "nbt": "vanilla_villager_plus_logic"},
    {"id": "temple_guardian_nordic", "entity_id": "mythic_age:temple_guardian_nordic", "kind": "guardian", "visual_gate": "renderer_smoke", "nbt": "vanilla_villager_plus_logic"},
    {"id": "custom_temple_guardian_greek", "entity_id": "mythic_age:custom_temple_guardian_greek", "kind": "custom_guardian", "visual_gate": "renderer_smoke", "nbt": "custom_entity"},
    {"id": "custom_temple_guardian_egyptian", "entity_id": "mythic_age:custom_temple_guardian_egyptian", "kind": "custom_guardian", "visual_gate": "renderer_smoke", "nbt": "custom_entity"},
    {"id": "custom_temple_guardian_chinese", "entity_id": "mythic_age:custom_temple_guardian_chinese", "kind": "custom_guardian", "visual_gate": "renderer_smoke", "nbt": "custom_entity"},
    {"id": "custom_temple_guardian_atlantean", "entity_id": "mythic_age:custom_temple_guardian_atlantean", "kind": "custom_guardian", "visual_gate": "renderer_smoke", "nbt": "custom_entity"},
    {"id": "custom_temple_guardian_nordic", "entity_id": "mythic_age:custom_temple_guardian_nordic", "kind": "custom_guardian", "visual_gate": "renderer_smoke", "nbt": "custom_entity"},
    {"id": "meteor", "entity_id": "mythic_age:meteor", "kind": "projectile_effect", "visual_gate": "runtime_effect", "nbt": "custom_entity"},
]

NBT_SCHEMAS: list[dict[str, Any]] = [
    {
        "owner": "WalkingTreeEntity",
        "path": "src/main/java/com/example/mythic_age/entity/WalkingTreeEntity.java",
        "scope": "entity",
        "required_write_read": {
            "walkingTreeVariant": "string",
            "walkingTreeLifetimeTicks": "int",
            "walkingTreeSummoner": "uuid_optional",
            "guardPosX": "int_optional",
            "guardPosY": "int_optional",
            "guardPosZ": "int_optional",
        },
    },
    {
        "owner": "CustomTempleGuardianEntity",
        "path": "src/main/java/com/example/mythic_age/entity/CustomTempleGuardianEntity.java",
        "scope": "entity",
        "required_write_read": {
            "mythologyType": "string_write_only_constructor_final",
            "certificateGenerationTimer": "int",
            "hasInitializedTrades": "boolean",
            "templePosX": "int_optional",
            "templePosY": "int_optional",
            "templePosZ": "int_optional",
            "tradeOffers": "compound_optional",
            "tradeCount": "int_optional",
            "tradeCooldowns": "compound_optional",
        },
    },
    {
        "owner": "MeteorEntity",
        "path": "src/main/java/com/example/mythic_age/entity/MeteorEntity.java",
        "scope": "entity",
        "required_write_read": {
            "Life": "int",
            "ExplosionPower": "float",
            "PlayedFallingSound": "boolean",
            "PlayedWarningSound": "boolean",
        },
    },
    {
        "owner": "VillagerAIEnhancementMixin",
        "path": "src/main/java/com/example/mythic_age/mixin/VillagerAIEnhancementMixin.java",
        "scope": "vanilla_villager_mixin",
        "required_write_read": {
            "MythicAge_Civilization": "string_optional",
            "MythicAge_CivilizationInitialized": "boolean",
            "MythicAge_AIInitialized": "boolean",
        },
    },
    {
        "owner": "AnimalLureTowerBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/AnimalLureTowerBlockEntity.java",
        "scope": "block_entity",
        "required_write_read": {
            "ticksAlive": "int",
            "lureTimer": "int",
            "grassTimer": "int",
        },
    },
    {
        "owner": "AbundanceAltarCoreBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/AbundanceAltarCoreBlockEntity.java",
        "scope": "block_entity",
        "required_write_read": {
            "NextGenerationTime": "long",
        },
    },
    {
        "owner": "RelicPedestalBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/RelicPedestalBlockEntity.java",
        "scope": "block_entity",
        "required_write_read": {
            "RelicItem": "compound_optional",
            "IsActiveTemple": "boolean",
        },
    },
    {
        "owner": "StatueBaseBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/StatueBaseBlockEntity.java",
        "scope": "block_entity",
        "required_write_read": {
            "StoredCertificates": "int",
            "LastUpdateTime": "long",
            "MythologyType": "string_write_only_constructor_final",
        },
    },
    {
        "owner": "EgyptianStatuePedestalBlockEntity",
        "path": "src/main/java/com/example/mythic_age/block/entity/EgyptianStatuePedestalBlockEntity.java",
        "scope": "block_entity",
        "required_write_read": {
            "hasStatue": "boolean",
            "productionTick": "int",
            "statueType": "string_optional",
        },
    },
    {
        "owner": "MythicAgeWorldState",
        "path": "src/main/java/com/example/mythic_age/world/MythicAgeWorldState.java",
        "scope": "world_persistent_state",
        "required_write_read": {
            "Version": "int_write_only_schema_version",
            "LightningStorms": "compound",
            "Earthquakes": "compound",
            "GodPowerWeather": "compound",
            "GodPowerCertificates": "compound",
        },
    },
    {
        "owner": "LightningStormManager",
        "path": "src/main/java/com/example/mythic_age/util/LightningStormManager.java",
        "scope": "world_persistent_state_manager",
        "required_write_read": {
            "ActiveStorms": "list",
            "Id": "string",
            "Dimension": "string",
            "Center": "long",
            "TicksAlive": "int",
            "RemainingTicks": "int_backcompat",
            "LightningTimer": "int",
        },
    },
    {
        "owner": "EarthquakeManager",
        "path": "src/main/java/com/example/mythic_age/util/EarthquakeManager.java",
        "scope": "world_persistent_state_manager",
        "required_write_read": {
            "ActiveEarthquakes": "list",
            "Id": "string",
            "Dimension": "string",
            "Epicenter": "long",
            "TicksAlive": "int",
            "RemainingTicks": "int_backcompat",
            "IntervalTimer": "int",
            "PlayerYaw": "float",
        },
    },
    {
        "owner": "GodPowerWeatherManager",
        "path": "src/main/java/com/example/mythic_age/util/GodPowerWeatherManager.java",
        "scope": "world_persistent_state_manager",
        "required_write_read": {
            "ActiveWeatherPowers": "list",
            "Id": "string",
            "Type": "string",
            "Dimension": "string",
            "Center": "long",
            "Radius": "int",
            "DurationTicks": "int",
            "TicksAlive": "int",
            "RemainingTicks": "int_backcompat",
        },
    },
    {
        "owner": "GodPowerCertificateManager",
        "path": "src/main/java/com/example/mythic_age/system/GodPowerCertificateManager.java",
        "scope": "world_persistent_state_manager",
        "required_write_read": {
            "CivilizationProductionState": "list",
            "Key": "string",
            "SourceType": "string",
            "Civilization": "string",
            "Dimension": "string",
            "SourcePos": "long",
            "LastGenerationTime": "long",
            "GeneratedAmount": "int",
        },
    },
]


def special_action_contract(
    creature_id: str,
    class_name: str,
    ability: str,
    required_particles: list[str],
    civilization: str,
) -> dict[str, Any]:
    return {
        "id": creature_id,
        "entity_id": f"mythic_age:{creature_id}",
        "civilization": civilization,
        "kind": "special_combat",
        "paths": [f"src/main/java/com/example/mythic_age/entity/{class_name}.java"],
        "ability": ability,
        "required_tokens": [
            f"private void {ability}(",
            "canUseSpecial()",
            "setSpecialCooldown(",
            "playSpecialSound(",
            "spawnParticles(",
        ],
        "required_particles": required_particles,
    }


ACTION_PARTICLE_CONTRACTS: list[dict[str, Any]] = [
    {
        "id": "minotaur",
        "entity_id": "mythic_age:minotaur",
        "civilization": "greek",
        "kind": "charge_goal",
        "paths": [
            "src/main/java/com/example/mythic_age/entity/MinotaurEntity.java",
            "src/main/java/com/example/mythic_age/entity/ai/goal/MinotaurChargeAttackGoal.java",
        ],
        "ability": "MinotaurChargeAttackGoal",
        "required_tokens": [
            "new MinotaurChargeAttackGoal",
            "CHARGE_COOLDOWN_TICKS",
            "ModSounds.ENTITY_MINOTAUR_CHARGE",
            "serverWorld.spawnParticles(",
            "this.minotaur.tryAttack(this.target)",
        ],
        "required_particles": ["CLOUD", "CRIT"],
    },
    special_action_contract("cyclops", "CyclopsEntity", "tryStoneThrow", ["POOF", "CRIT"], "greek"),
    {
        "id": "centaur",
        "entity_id": "mythic_age:centaur",
        "civilization": "greek",
        "kind": "ranged_projectile",
        "paths": ["src/main/java/com/example/mythic_age/entity/CentaurEntity.java"],
        "ability": "rangedArrowShot",
        "required_tokens": [
            "implements RangedAttackMob",
            "ProjectileAttackGoal",
            "spawnEntity(arrow)",
            "playSpecialSound(",
            "spawnParticles(",
        ],
        "required_particles": ["CRIT"],
    },
    special_action_contract("hydra", "HydraEntity", "tryPoisonSweep", ["SNEEZE", "SWEEP_ATTACK"], "greek"),
    special_action_contract("medusa", "MedusaEntity", "tryPetrifyingGaze", ["WHITE_ASH", "ENCHANT"], "greek"),
    special_action_contract("nemean_lion", "NemeanLionEntity", "tryPounce", ["CLOUD", "CRIT"], "greek"),
    special_action_contract("manticore", "ManticoreEntity", "tryTailSting", ["SNEEZE", "CRIT"], "greek"),
    special_action_contract("chimera", "ChimeraEntity", "tryFlameBreath", ["FLAME", "SMOKE"], "greek"),
    special_action_contract("colossus", "ColossusEntity", "tryStomp", ["POOF", "CRIT"], "greek"),
    {
        "id": "pegasus",
        "entity_id": "mythic_age:pegasus",
        "civilization": "greek",
        "kind": "rideable_flight",
        "paths": ["src/main/java/com/example/mythic_age/entity/PegasusEntity.java"],
        "ability": "mountedAndAutonomousFlight",
        "required_tokens": [
            "setNoGravity(true)",
            "tickAutonomousFlight(",
            "travelWithRider(",
            "player.startRiding(this)",
            "isFlying()",
            "ModSounds.ENTITY_PEGASUS_FLY",
            "spawnParticles(",
        ],
        "required_particles": ["CLOUD", "END_ROD"],
    },
    special_action_contract("anubis_warrior", "AnubisWarriorEntity", "tryJudgementMark", ["SOUL", "ENCHANT"], "egyptian"),
    special_action_contract("scarab_guardian", "ScarabGuardianEntity", "tryHornRush", ["CRIT", "CLOUD"], "egyptian"),
    special_action_contract("mummy_soldier", "MummySoldierEntity", "trySpearThrust", ["CRIT"], "egyptian"),
    special_action_contract("sphinx_sentinel", "SphinxSentinelEntity", "tryRiddleGaze", ["ENCHANT", "SOUL"], "egyptian"),
    {
        "id": "sun_falcon",
        "entity_id": "mythic_age:sun_falcon",
        "civilization": "egyptian",
        "kind": "flying_special_combat",
        "paths": ["src/main/java/com/example/mythic_age/entity/SunFalconEntity.java"],
        "ability": "trySolarDive",
        "required_tokens": [
            "setNoGravity(true)",
            "tickFlight(",
            "private void trySolarDive(",
            "canUseSpecial()",
            "setSpecialCooldown(",
            "playSpecialSound(",
            "spawnParticles(",
            "isFlying()",
        ],
        "required_particles": ["ENCHANT", "CRIT"],
    },
    special_action_contract("frost_giant", "FrostGiantEntity", "tryFrostQuake", ["SNOWFLAKE", "CRIT"], "nordic"),
    special_action_contract("draugr_warrior", "DraugrWarriorEntity", "tryShieldBash", ["CRIT", "SOUL", "BLOCK"], "nordic"),
    {
        "id": "valkyrie",
        "entity_id": "mythic_age:valkyrie",
        "civilization": "nordic",
        "kind": "flying_special_combat",
        "paths": ["src/main/java/com/example/mythic_age/entity/ValkyrieEntity.java"],
        "ability": "trySpearDive",
        "required_tokens": [
            "setNoGravity(true)",
            "tickWingedFlight(",
            "private void trySpearDive(",
            "canUseSpecial()",
            "setSpecialCooldown(",
            "playSpecialSound(",
            "spawnParticles(",
            "isFlying()",
        ],
        "required_particles": ["END_ROD", "CRIT", "ENCHANT"],
    },
    special_action_contract("fenrir_wolf", "FenrirWolfEntity", "tryDreadHowl", ["SOUL", "SMOKE", "CRIT"], "nordic"),
    special_action_contract("dwarf_berserker", "DwarfBerserkerEntity", "tryBerserkFlurry", ["CRIT", "ENCHANT"], "nordic"),
    {
        "id": "azure_dragon",
        "entity_id": "mythic_age:azure_dragon",
        "civilization": "chinese",
        "kind": "flying_special_combat",
        "paths": ["src/main/java/com/example/mythic_age/entity/AzureDragonEntity.java"],
        "ability": "tryDragonBreath",
        "required_tokens": [
            "setNoGravity(true)",
            "tickSerpentineFlight(",
            "private void tryDragonBreath(",
            "canUseSpecial()",
            "setSpecialCooldown(",
            "playSpecialSound(",
            "spawnParticles(",
            "isFlying()",
        ],
        "required_particles": ["DRAGON_BREATH", "ENCHANT"],
    },
    special_action_contract("qilin_guardian", "QilinGuardianEntity", "tryAuspiciousStomp", ["ENCHANT", "HAPPY_VILLAGER", "CRIT"], "chinese"),
    special_action_contract("jiangshi", "JiangshiEntity", "tryTalismanBind", ["ENCHANT", "SOUL", "CRIT"], "chinese"),
    special_action_contract("nine_tailed_fox", "NineTailedFoxEntity", "tryTailGlamour", ["HEART", "ENCHANT", "WITCH"], "chinese"),
    special_action_contract("stone_lion_guardian", "StoneLionGuardianEntity", "tryStonePawSuppression", ["BLOCK", "CRIT", "ENCHANT"], "chinese"),
    {
        "id": "walking_tree",
        "entity_id": "mythic_age:walking_tree",
        "civilization": "nordic",
        "kind": "summoned_guard_action_state",
        "paths": ["src/main/java/com/example/mythic_age/entity/WalkingTreeEntity.java"],
        "ability": "guardAreaAndAttackAnimation",
        "required_tokens": [
            "GuardAreaGoal",
            "ATTACK_ANIMATION_TICKS",
            "getAttackAnimationProgress(",
            "writeCustomDataToNbt(",
            "readCustomDataFromNbt(",
            "tryAttack(",
        ],
        "required_particles": [],
    },
    {
        "id": "crystal_leviathan",
        "entity_id": "mythic_age:crystal_leviathan",
        "civilization": "atlantean",
        "kind": "gliding_special_combat",
        "paths": ["src/main/java/com/example/mythic_age/entity/CrystalLeviathanEntity.java"],
        "ability": "tryPrismSurge",
        "required_tokens": [
            "setNoGravity(true)",
            "tickTidalGlide(",
            "private void tryPrismSurge(",
            "canUseSpecial()",
            "setSpecialCooldown(",
            "playSpecialSound(",
            "spawnParticles(",
            "isGliding()",
        ],
        "required_particles": ["BUBBLE_POP", "SPLASH", "ENCHANT"],
    },
    special_action_contract("orichalcum_golem", "OrichalcumGolemEntity", "tryCorePulse", ["ENCHANT", "CRIT", "BUBBLE_POP"], "atlantean"),
    {
        "id": "siren_priestess",
        "entity_id": "mythic_age:siren_priestess",
        "civilization": "atlantean",
        "kind": "drifting_special_combat",
        "paths": ["src/main/java/com/example/mythic_age/entity/SirenPriestessEntity.java"],
        "ability": "tryAbyssalHymn",
        "required_tokens": [
            "setNoGravity(true)",
            "tickSirenDrift(",
            "private void tryAbyssalHymn(",
            "canUseSpecial()",
            "setSpecialCooldown(",
            "playSpecialSound(",
            "spawnParticles(",
            "isDrifting()",
        ],
        "required_particles": ["ENCHANT", "BUBBLE_POP", "SPLASH"],
    },
    special_action_contract("tidal_guardian", "TidalGuardianEntity", "tryClawBreaker", ["SPLASH", "BUBBLE_POP", "ENCHANT"], "atlantean"),
    {
        "id": "abyssal_ray",
        "entity_id": "mythic_age:abyssal_ray",
        "civilization": "atlantean",
        "kind": "gliding_special_combat",
        "paths": ["src/main/java/com/example/mythic_age/entity/AbyssalRayEntity.java"],
        "ability": "tryTailSting",
        "required_tokens": [
            "setNoGravity(true)",
            "tickAbyssalGlide(",
            "private void tryTailSting(",
            "canUseSpecial()",
            "setSpecialCooldown(",
            "playSpecialSound(",
            "spawnParticles(",
            "isGliding()",
        ],
        "required_particles": ["CRIT", "BUBBLE_POP", "ENCHANT"],
    },
]


def write_manifest() -> None:
    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "version": 1,
        "scope": "all_custom_entities_and_nbt_structures",
        "visual_views": ["front", "side", "three_quarter"],
        "entity_count": len(CUSTOM_ENTITIES),
        "entities": CUSTOM_ENTITIES,
        "nbt_schema_report": str(NBT_REPORT_PATH.relative_to(ROOT)),
        "action_particle_report": str(ACTION_PARTICLE_REPORT_PATH.relative_to(ROOT)),
        "mc_runtime_nbt_report": str(MC_NBT_REPORT_PATH.relative_to(ROOT)),
        "visual_gate_manifests": {
            key: str(path.relative_to(ROOT)) for key, path in VISUAL_GATE_MANIFESTS.items()
        },
    }
    MANIFEST_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def source_text(schema: dict[str, Any]) -> str:
    return (ROOT / schema["path"]).read_text(encoding="utf-8")


def has_put(text: str, key: str) -> bool:
    return re.search(rf"\.put(?:String|Int|Long|Boolean|Float|Uuid)?\(\"{re.escape(key)}\"", text) is not None


def has_get_or_contains(text: str, key: str) -> bool:
    patterns = [
        rf"\.get(?:String|Int|Long|Boolean|Float|Uuid|Compound|List)?\(\"{re.escape(key)}\"",
        rf"\.contains(?:Uuid)?\(\"{re.escape(key)}\"",
    ]
    return any(re.search(pattern, text) for pattern in patterns)


def audit_nbt_schema() -> tuple[int, dict[str, Any]]:
    failures: list[str] = []
    results: list[dict[str, Any]] = []
    for schema in NBT_SCHEMAS:
        text = source_text(schema)
        fields: list[dict[str, Any]] = []
        for key, kind in schema["required_write_read"].items():
            written = has_put(text, key)
            read = has_get_or_contains(text, key)
            write_only = "write_only" in kind
            optional = "optional" in kind
            ok = written and (read or write_only)
            if not ok:
                failures.append(f"{schema['owner']}: NBT field {key} write/read mismatch")
            fields.append({
                "key": key,
                "kind": kind,
                "written": written,
                "read_or_checked": read,
                "optional": optional,
                "write_only_by_design": write_only,
                "ok": ok,
            })
        results.append({
            "owner": schema["owner"],
            "scope": schema["scope"],
            "path": schema["path"],
            "fields": fields,
            "ok": all(field["ok"] for field in fields),
        })
    payload = {
        "version": 1,
        "scope": "static_nbt_schema_symmetry",
        "schemas_checked": len(NBT_SCHEMAS),
        "ok": not failures,
        "failures": failures,
        "schemas": results,
    }
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    NBT_REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return (0 if not failures else 1), payload


def audit_action_particle_contracts() -> tuple[int, dict[str, Any]]:
    failures: list[str] = []
    results: list[dict[str, Any]] = []
    for contract in ACTION_PARTICLE_CONTRACTS:
        missing_paths: list[str] = []
        texts: list[str] = []
        for relative_path in contract["paths"]:
            path = ROOT / relative_path
            if not path.exists():
                missing_paths.append(relative_path)
                continue
            texts.append(path.read_text(encoding="utf-8"))

        source = "\n".join(texts)
        missing_tokens = [
            token for token in contract.get("required_tokens", [])
            if token not in source
        ]
        missing_particles = [
            particle for particle in contract.get("required_particles", [])
            if f"ParticleTypes.{particle}" not in source
        ]
        ok = not missing_paths and not missing_tokens and not missing_particles
        if not ok:
            details: list[str] = []
            if missing_paths:
                details.append("missing_paths=" + "|".join(missing_paths))
            if missing_tokens:
                details.append("missing_tokens=" + "|".join(missing_tokens))
            if missing_particles:
                details.append("missing_particles=" + "|".join(missing_particles))
            failures.append(f"{contract['entity_id']}: " + "; ".join(details))
        results.append({
            "id": contract["id"],
            "entity_id": contract["entity_id"],
            "civilization": contract["civilization"],
            "kind": contract["kind"],
            "ability": contract["ability"],
            "paths": contract["paths"],
            "required_particles": contract.get("required_particles", []),
            "missing_paths": missing_paths,
            "missing_tokens": missing_tokens,
            "missing_particles": missing_particles,
            "ok": ok,
        })

    custom_creature_ids = {
        entity["id"] for entity in CUSTOM_ENTITIES
        if entity["kind"] in {
            "creature",
            "creature_mount",
            "egyptian_creature",
            "egyptian_flying_creature",
            "nordic_creature",
            "nordic_flying_creature",
            "chinese_creature",
            "chinese_flying_creature",
            "atlantean_creature",
            "summoned_creature",
        }
    }
    contract_ids = {contract["id"] for contract in ACTION_PARTICLE_CONTRACTS}
    missing_contracts = sorted(custom_creature_ids - contract_ids)
    if missing_contracts:
        failures.append("missing action/particle contracts for creatures: " + "|".join(missing_contracts))

    payload = {
        "version": 1,
        "scope": "static_creature_action_and_particle_contracts",
        "contracts_checked": len(ACTION_PARTICLE_CONTRACTS),
        "creature_contract_coverage": {
            "expected_creatures": len(custom_creature_ids),
            "covered_creatures": len(custom_creature_ids & contract_ids),
            "missing_contracts": missing_contracts,
        },
        "ok": not failures,
        "failures": failures,
        "contracts": results,
    }
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    ACTION_PARTICLE_REPORT_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return (0 if not failures else 1), payload


def audit_entity_registration() -> tuple[int, list[str]]:
    failures: list[str] = []
    sources = "\n".join(path.read_text(encoding="utf-8") for path in (MOD_ENTITIES, MOD_ENTITY_TYPES))
    for entity in CUSTOM_ENTITIES:
        path = entity["entity_id"].split(":", 1)[1]
        if f'"{path}"' not in sources:
            failures.append(f"missing registry entry for {entity['entity_id']}")
    return (0 if not failures else 1), failures


def audit_visual_gates() -> tuple[int, list[str]]:
    failures: list[str] = []
    cache: dict[str, dict[str, Any]] = {}
    for entity in CUSTOM_ENTITIES:
        gate = entity["visual_gate"]
        manifest_path = VISUAL_GATE_MANIFESTS.get(gate)
        if manifest_path is None:
            continue
        if not manifest_path.exists():
            failures.append(f"{entity['entity_id']}: missing visual gate manifest {manifest_path}")
            continue
        if gate not in cache:
            cache[gate] = json.loads(manifest_path.read_text(encoding="utf-8"))
        creatures = cache[gate].get("creatures", [])
        match = next((creature for creature in creatures if creature.get("entity_id") == entity["entity_id"]), None)
        if match is None:
            failures.append(f"{entity['entity_id']}: not present in {gate} manifest")
            continue
        artifact_keys = ("design_doc", "bbmodel", "texture", "uv_template")
        for key in artifact_keys:
            artifact = match.get(key)
            if not artifact or not (ROOT / artifact).exists():
                failures.append(f"{entity['entity_id']}: missing {key} artifact from {gate}")
        previews = match.get("blockbench_previews", {})
        for view in ("front", "side", "three_quarter"):
            preview = previews.get(view)
            if not preview or not (ROOT / preview).exists():
                failures.append(f"{entity['entity_id']}: missing {view} blockbench preview from {gate}")
    return (0 if not failures else 1), failures


def audit_runtime_model_reports() -> tuple[int, list[str]]:
    failures: list[str] = []
    required_reports = {
        "greek_runtime_model_quality": GREEK_RUNTIME_MODEL_REPORT,
        "egyptian_runtime_model_quality": EGYPTIAN_RUNTIME_MODEL_REPORT,
        "nordic_runtime_model_quality": NORDIC_RUNTIME_MODEL_REPORT,
        "chinese_runtime_model_quality": CHINESE_RUNTIME_MODEL_REPORT,
        "atlantean_runtime_model_quality": ATLANTEAN_RUNTIME_MODEL_REPORT,
    }
    for name, path in required_reports.items():
        if not path.exists():
            failures.append(f"missing runtime model quality report {name}: {path}")
            continue
        payload = json.loads(path.read_text(encoding="utf-8"))
        if not payload.get("ok", False):
            failures.append(f"runtime model quality report has failures {name}: {path}")
    return (0 if not failures else 1), failures


def audit() -> int:
    failures: list[str] = []
    if not MANIFEST_PATH.exists():
        failures.append(f"Missing manifest: {MANIFEST_PATH}")
    code, registry_failures = audit_entity_registration()
    failures.extend(registry_failures)
    visual_code, visual_failures = audit_visual_gates()
    failures.extend(visual_failures)
    runtime_code, runtime_failures = audit_runtime_model_reports()
    failures.extend(runtime_failures)
    nbt_code, _payload = audit_nbt_schema()
    if nbt_code != 0:
        failures.append(f"NBT schema report has failures: {NBT_REPORT_PATH}")
    action_code, _action_payload = audit_action_particle_contracts()
    if action_code != 0:
        failures.append(f"Action/particle report has failures: {ACTION_PARTICLE_REPORT_PATH}")
    if failures:
        print("Entity Validation Gate: FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("Entity Validation Gate: PASS")
    print(f"manifest: {MANIFEST_PATH}")
    print(f"nbt_schema_report: {NBT_REPORT_PATH}")
    print(f"action_particle_report: {ACTION_PARTICLE_REPORT_PATH}")
    print(f"mc_runtime_nbt_report: {MC_NBT_REPORT_PATH}")
    return 0


def generate_all() -> int:
    write_manifest()
    _code, _payload = audit_nbt_schema()
    _action_code, _action_payload = audit_action_particle_contracts()
    return audit()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["all", "manifest", "nbt", "actions", "audit"])
    args = parser.parse_args()
    if args.command == "all":
        return generate_all()
    if args.command == "manifest":
        write_manifest()
        return 0
    if args.command == "nbt":
        code, payload = audit_nbt_schema()
        print(json.dumps(payload, indent=2, ensure_ascii=False))
        return code
    if args.command == "actions":
        code, payload = audit_action_particle_contracts()
        print(json.dumps(payload, indent=2, ensure_ascii=False))
        return code
    if args.command == "audit":
        return audit()
    return 0


if __name__ == "__main__":
    sys.exit(main())
