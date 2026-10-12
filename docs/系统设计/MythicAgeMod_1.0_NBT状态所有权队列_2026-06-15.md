# MythicAgeMod 1.0 NBT 状态所有权队列（2026-06-15）

本文档由 `tools/nbt_state_ownership_queue.py audit` 生成。它不把 `runtime_only` 当成缺失，而是把每个长期状态、短期状态和未来升级触发条件登记清楚。

## 总览

- NBT 队列通过：`True`
- NBT 发布验收 ready：`True`
- 状态行数：69
- 已覆盖长期状态：21
- runtime-only 决策：48
- required 待实现：0
- missing 阻断：0
- schema owner 数：14
- 状态统计：`{"covered": 21, "runtime_only": 48}`
- 优先级统计：`{"P0-REGRESSION-GUARD": 21, "P1-RUNTIME-ONLY-DECISION": 48}`
- 领域统计：`{"certificate_economy": 9, "creatures": 44, "god_powers": 10, "structures_blocks": 6}`

## 结论

- 当前 NBT 不是“没做”，而是 1.0 目标表已经把长期状态和短期状态分开：21 项 covered，48 项 runtime_only，required/missing 为 0。
- 30 个普通生物技能冷却、飞行阶段、短 tick 能力大多不应该写长期 NBT；它们只有在变成跨重启真相、经济防刷、召唤归属或长期 boss 阶段时才升级。
- 后续新增玩法必须先改目标表，再实现 NBT/schema/probe，不能先写隐藏状态。

## Schema Owners

| Owner | Scope | Fields | OK | Path |
| --- | --- | --- | --- | --- |
| `WalkingTreeEntity` | `entity` | 6 | `True` | `src/main/java/com/example/mythic_age/entity/WalkingTreeEntity.java` |
| `CustomTempleGuardianEntity` | `entity` | 9 | `True` | `src/main/java/com/example/mythic_age/entity/CustomTempleGuardianEntity.java` |
| `MeteorEntity` | `entity` | 4 | `True` | `src/main/java/com/example/mythic_age/entity/MeteorEntity.java` |
| `VillagerAIEnhancementMixin` | `vanilla_villager_mixin` | 3 | `True` | `src/main/java/com/example/mythic_age/mixin/VillagerAIEnhancementMixin.java` |
| `AnimalLureTowerBlockEntity` | `block_entity` | 3 | `True` | `src/main/java/com/example/mythic_age/block/entity/AnimalLureTowerBlockEntity.java` |
| `AbundanceAltarCoreBlockEntity` | `block_entity` | 1 | `True` | `src/main/java/com/example/mythic_age/block/entity/AbundanceAltarCoreBlockEntity.java` |
| `RelicPedestalBlockEntity` | `block_entity` | 2 | `True` | `src/main/java/com/example/mythic_age/block/entity/RelicPedestalBlockEntity.java` |
| `StatueBaseBlockEntity` | `block_entity` | 3 | `True` | `src/main/java/com/example/mythic_age/block/entity/StatueBaseBlockEntity.java` |
| `EgyptianStatuePedestalBlockEntity` | `block_entity` | 3 | `True` | `src/main/java/com/example/mythic_age/block/entity/EgyptianStatuePedestalBlockEntity.java` |
| `MythicAgeWorldState` | `world_persistent_state` | 5 | `True` | `src/main/java/com/example/mythic_age/world/MythicAgeWorldState.java` |
| `LightningStormManager` | `world_persistent_state_manager` | 7 | `True` | `src/main/java/com/example/mythic_age/util/LightningStormManager.java` |
| `EarthquakeManager` | `world_persistent_state_manager` | 8 | `True` | `src/main/java/com/example/mythic_age/util/EarthquakeManager.java` |
| `GodPowerWeatherManager` | `world_persistent_state_manager` | 9 | `True` | `src/main/java/com/example/mythic_age/util/GodPowerWeatherManager.java` |
| `GodPowerCertificateManager` | `world_persistent_state_manager` | 8 | `True` | `src/main/java/com/example/mythic_age/system/GodPowerCertificateManager.java` |

## 队列

### P0-REGRESSION-GUARD

| Domain | Owner/System | Field | Status | Phase |
| --- | --- | --- | --- | --- |
| `creatures` | ``MythicAge_Summoner`` | `uuid` | `covered` | `keep_schema_and_restart_evidence_green` |
| `creatures` | ``walking_tree`` | ``walkingTreeVariant`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `creatures` | ``walking_tree`` | ``walkingTreeLifetimeTicks`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `creatures` | ``walking_tree`` | ``walkingTreeSummoner`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `creatures` | ``walking_tree`` | ``guardPosX/Y/Z`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `god_powers` | `Lightning Storm` | ``dimension`, `center`, `remainingTicks`, `lightningTimer`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `god_powers` | `Earthquake` | ``dimension`, `center`, `remainingTicks`, `intervalTimer`, `yaw`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `god_powers` | `Walking Woods` | ``summoner`, `treeIds`, `expiresAt`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `god_powers` | `Fimbulwinter` | ``dimension`, `center`, `radius`, `remainingTicks`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `god_powers` | `Oceanic Wrath` | ``dimension`, `center`, `radius`, `remainingTicks`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `certificate_economy` | ``StatueBaseBlockEntity`` | ``StoredCertificates`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `certificate_economy` | ``StatueBaseBlockEntity`` | ``LastUpdateTime`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `certificate_economy` | ``CustomTempleGuardianEntity`` | ``tradeCooldowns`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `certificate_economy` | ``CustomTempleGuardianEntity`` | ``tradeOffers`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `certificate_economy` | ``GodPowerCertificateManager`` | ``CivilizationProductionState`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `structures_blocks` | ``RelicPedestalBlockEntity`` | ``RelicItem`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `structures_blocks` | ``RelicPedestalBlockEntity`` | ``IsActiveTemple`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `structures_blocks` | ``EgyptianStatuePedestalBlockEntity`` | ``hasStatue`, `productionTick`, `statueType`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `structures_blocks` | ``AbundanceAltarCoreBlockEntity`` | ``NextGenerationTime`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `structures_blocks` | `神殿结构功能点` | ``GuardianSpawn`, `RelicPedestal`, `StatueBase`, `Entrance`, `Bounds`` | `covered` | `keep_schema_and_restart_evidence_green` |
| `structures_blocks` | `canonical NBT` | ``size`, `palette`, `blocks`, `entities`, `DataVersion`` | `covered` | `keep_schema_and_restart_evidence_green` |

### P1-RUNTIME-ONLY-DECISION

| Domain | Owner/System | Field | Status | Phase |
| --- | --- | --- | --- | --- |
| `creatures` | ``MythicAge_Variant`` | `string` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``MythicAge_SpecialCooldown`` | `int` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``MythicAge_AbilityPhase`` | `string` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``MythicAge_LastAbilityGameTime`` | `long` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``MythicAge_SourceCivilization`` | `string` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``minotaur`` | ``ChargeCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``cyclops`` | ``StoneThrowCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``centaur`` | ``RangedVolleyCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``hydra`` | ``ActiveHeadCount`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``hydra`` | ``RegenerationTicks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``medusa`` | ``PetrifiedTargets`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``nemean_lion`` | ``PounceCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``manticore`` | ``VenomCharges`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``chimera`` | ``FlameBreathCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``colossus`` | ``CoreHeat`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``pegasus`` | ``IsTamed`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``pegasus`` | ``Owner`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``pegasus`` | ``FlightEnergy`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``anubis_warrior`` | ``JudgementMarkTicks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``scarab_guardian`` | ``Burrowed`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``mummy_soldier`` | ``CurseStacks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``sphinx_sentinel`` | ``RiddleCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``sun_falcon`` | ``SolarCharge`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``frost_giant`` | ``FrostQuakeCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``draugr_warrior`` | ``ShieldBashCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``valkyrie`` | ``SpearDiveCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``fenrir_wolf`` | ``DreadHowlCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``dwarf_berserker`` | ``BerserkTicks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``azure_dragon`` | ``DragonBreathCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``azure_dragon`` | ``FlightPhase`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``qilin_guardian`` | ``BlessingCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``jiangshi`` | ``TalismanBindTicks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``nine_tailed_fox`` | ``GlamourCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``stone_lion_guardian`` | ``StoneSuppressionCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``crystal_leviathan`` | ``PrismSurgeCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``orichalcum_golem`` | ``CorePulseCharge`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``siren_priestess`` | ``HymnTicks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``tidal_guardian`` | ``ClawBreakerCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `creatures` | ``abyssal_ray`` | ``AbyssalStingCooldown`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `god_powers` | `Ceasefire` | ``affectedEntities`, `remainingTicks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `god_powers` | `Bronze` | ``affectedEntities`, `remainingTicks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `god_powers` | `Underworld Passage` | ``portalA`, `portalB`, `expiresAt`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `god_powers` | `Crystal Barrier` | ``affectedEntities`, `remainingTicks`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `god_powers` | `Abyssal Rift` | ``center`, `remainingTicks`, `owner`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `certificate_economy` | ``GodPowerCertificateItem`` | ``MythologyType`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `certificate_economy` | ``GodPowerCertificateManager`` | ``EgyptianStatueRuntimeIndex`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `certificate_economy` | `神力书 item` | ``UnlockedBy`, `Charges`, `BoundCivilization`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |
| `certificate_economy` | `圣物 item` | ``RelicPower`, `BoundOwner`, `DurabilityState`` | `runtime_only` | `document_decision_and_watch_upgrade_trigger` |

## Runtime-only 升级触发器

- `creatures` / ``MythicAge_Variant`` / `string`：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``MythicAge_SpecialCooldown`` / `int`：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``MythicAge_AbilityPhase`` / `string`：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``MythicAge_LastAbilityGameTime`` / `long`：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``MythicAge_SourceCivilization`` / `string`：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``minotaur`` / ``ChargeCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``cyclops`` / ``StoneThrowCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``centaur`` / ``RangedVolleyCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``hydra`` / ``ActiveHeadCount``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``hydra`` / ``RegenerationTicks``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``medusa`` / ``PetrifiedTargets``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``nemean_lion`` / ``PounceCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``manticore`` / ``VenomCharges``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``chimera`` / ``FlameBreathCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``colossus`` / ``CoreHeat``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``pegasus`` / ``IsTamed``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``pegasus`` / ``Owner``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``pegasus`` / ``FlightEnergy``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``anubis_warrior`` / ``JudgementMarkTicks``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``scarab_guardian`` / ``Burrowed``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``mummy_soldier`` / ``CurseStacks``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``sphinx_sentinel`` / ``RiddleCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``sun_falcon`` / ``SolarCharge``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``frost_giant`` / ``FrostQuakeCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``draugr_warrior`` / ``ShieldBashCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``valkyrie`` / ``SpearDiveCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``fenrir_wolf`` / ``DreadHowlCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``dwarf_berserker`` / ``BerserkTicks``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``azure_dragon`` / ``DragonBreathCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``azure_dragon`` / ``FlightPhase``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``qilin_guardian`` / ``BlessingCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``jiangshi`` / ``TalismanBindTicks``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``nine_tailed_fox`` / ``GlamourCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``stone_lion_guardian`` / ``StoneSuppressionCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``crystal_leviathan`` / ``PrismSurgeCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``orichalcum_golem`` / ``CorePulseCharge``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``siren_priestess`` / ``HymnTicks``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``tidal_guardian`` / ``ClawBreakerCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `creatures` / ``abyssal_ray`` / ``AbyssalStingCooldown``：当该状态需要跨卸载/重启保留、影响经济防刷、召唤归属或长期 boss 阶段时，升级为 required。
- `god_powers` / `Ceasefire` / ``affectedEntities`, `remainingTicks``：当持续神力不再能由状态效果或当前 tick 重建时，升级为 required。
- `god_powers` / `Bronze` / ``affectedEntities`, `remainingTicks``：当持续神力不再能由状态效果或当前 tick 重建时，升级为 required。
- `god_powers` / `Underworld Passage` / ``portalA`, `portalB`, `expiresAt``：当持续神力不再能由状态效果或当前 tick 重建时，升级为 required。
- `god_powers` / `Crystal Barrier` / ``affectedEntities`, `remainingTicks``：当持续神力不再能由状态效果或当前 tick 重建时，升级为 required。
- `god_powers` / `Abyssal Rift` / ``center`, `remainingTicks`, `owner``：当神力产生可持续实体、方块、区域、传送门或跨重启影响时，升级为 world persistent state。
- `certificate_economy` / ``GodPowerCertificateItem`` / ``MythologyType``：当物品获得绑定、次数、耐久、交易来源或防刷语义时，升级为 item component 或 world persistent state。
- `certificate_economy` / ``GodPowerCertificateManager`` / ``EgyptianStatueRuntimeIndex``：当物品获得绑定、次数、耐久、交易来源或防刷语义时，升级为 item component 或 world persistent state。
- `certificate_economy` / `神力书 item` / ``UnlockedBy`, `Charges`, `BoundCivilization``：当物品获得绑定、次数、耐久、交易来源或防刷语义时，升级为 item component 或 world persistent state。
- `certificate_economy` / `圣物 item` / ``RelicPower`, `BoundOwner`, `DurabilityState``：当物品获得绑定、次数、耐久、交易来源或防刷语义时，升级为 item component 或 world persistent state。

## 回归规则

- `tools/nbt_target_validation_pipeline.py audit` 必须通过。
- `tools/nbt_release_acceptance_package.py audit` 必须通过。
- 任何从 runtime-only 升级为 required 的项，必须同时补 schema、round-trip/restart probe 和默认值策略。
- release 文档中的 next_actions 是回归守护动作，不代表 NBT 当前未完成。
