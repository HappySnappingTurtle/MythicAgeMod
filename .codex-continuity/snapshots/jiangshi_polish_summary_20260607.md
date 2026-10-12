# Jiangshi Polish Summary - 2026-06-07

## Current Objective

Continue MythicAgeMod 1.0 development across code, models, actions, textures, UI, art, particles, and NBT. This slice continued the high-detail creature polish phase by improving Chinese `Jiangshi / 僵尸` readability.

## Completed Work

- Audited the Jiangshi design package, Chinese visual pipeline, Java runtime model, and runtime model quality gate.
- Strengthened `tools/chinese_creature_visual_pipeline.py` for `jiangshi`:
  - Expanded the design parts to include pale face, eyes, mouth, official hat, hat wings, hat tail, talisman paper, red talisman marks, robe body, robe front panel, robe collar, sleeves, cuffs, stiff arms, legs, and black boots.
  - Replaced simple geometry with explicit Blockbench-style geometry for face, hat, talisman, robe layers, forward sleeve arms, cuffs, and boots.
  - Added a Chinese texture decoration hook for Jiangshi so pale face, black hat, yellow talisman, red marks, dark teal robe, cuffs, and boots use distinct readable colors.
- Updated `src/main/java/com/example/mythic_age/client/render/entity/model/JiangshiModel.java`:
  - Split previously anonymous cuboids into named runtime parts: `pale_face`, `left_eye`, `right_eye`, `mouth_line`, `hat_crown`, `hat_tail`, `left_hat_wing`, `right_hat_wing`, `talisman_mark_top`, `talisman_mark_mid`, `talisman_mark_low`, `robe_collar`, `robe_lower_hem`, `robe_front_panel`, `left_cuff`, `right_cuff`, `left_boot`, and `right_boot`.
  - Preserved existing stiff-arm and hopping animation behavior.
- Strengthened `tools/chinese_runtime_model_quality_gate.py`:
  - Jiangshi now requires 27 named runtime parts and additional detail UV tokens.
  - The gate will fail if Jiangshi regresses to a generic zombie silhouette.

## Important Decisions

- No Minecraft client or in-game screenshot validation was run, respecting the user's instruction that in-game validation is currently not needed.
- The work follows the design-first workflow: clarify the Blockbench source first, synchronize Java second, and use tests only to validate execution.
- This is a high-detail polish pass, not final hand-authored release art.

## Files Touched Or Generated

- `tools/chinese_creature_visual_pipeline.py`
- `tools/chinese_runtime_model_quality_gate.py`
- `src/main/java/com/example/mythic_age/client/render/entity/model/JiangshiModel.java`
- Regenerated Chinese visual artifacts under `docs/美术资源/chinese_creatures/`, `docs/美术资源/blockbench/chinese_creatures/`, `docs/preview_assets/chinese_creatures/`, and Chinese entity textures under `src/main/resources/assets/mythic_age/textures/entity/`.

## Verification

- `python3 tools/chinese_creature_visual_pipeline.py all` initially failed on `robe_lower_hem` UV overflow and too-thin `mouth_line`/talisman mark regions; UV and geometry were corrected, then PASSed.
- Visual inspection opened `docs/preview_assets/chinese_creatures/blockbench_validation_contact_sheet.png`; Jiangshi now has readable black official hat, yellow talisman with red marks, pale face, forward sleeve arms, dark teal robe, cuffs, and black boots.
- `python3 tools/chinese_runtime_model_quality_gate.py audit` PASS.
- `python3 tools/entity_validation_pipeline.py all` PASS, including entity validation and NBT reports.
- Resource JSON validation with `jq empty` over Chinese Blockbench/design/preview assets and `src/main/resources/assets/mythic_age` PASS.
- `./gradlew build --offline` first failed in sandbox on Gradle wrapper `.lck` permission, then PASS with approved external wrapper-cache access.

## Open Questions Or Blockers

- Jiangshi is improved but not final art.
- No in-game visual validation is currently expected unless the user asks to re-enable MC screenshots.
- Other creatures still need comparable high-detail passes.

## Next Concrete Action

Continue high-detail polish with another low-readability creature. Good next targets are Chinese `Nine-tailed Fox`, Chinese `Stone Lion Guardian`, or the larger Greek re-polish pass.
