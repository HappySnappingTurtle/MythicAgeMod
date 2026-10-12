# Stone Lion Guardian Polish Summary - 2026-06-10

## Current Objective

Continue MythicAgeMod 1.0 development across code, models, actions, textures, UI, art, particles, and NBT. This slice continued the high-detail creature polish phase by improving Chinese `Stone Lion Guardian / 石狮守卫` readability.

## Completed Work

- Audited the Stone Lion Guardian design package, Chinese visual pipeline, Java runtime model, and runtime model quality gate.
- Strengthened `tools/chinese_creature_visual_pipeline.py` for `stone_lion_guardian`:
  - Expanded the design parts from a generic stone lion body into named stone-body, head, snout, gold eyes, nose, fangs, layered curled mane, paw pads, claws, embroidered ball, ball ribbons, stone plinth, and curled tail parts.
  - Replaced the simple gray quadruped geometry with explicit Blockbench-style geometry for the stone plinth, broad head, forward snout, readable eyes/fangs, mane curls, heavy paws, ribboned ball, and tail curl.
  - Added Stone Lion texture decoration so gold eyes, red embroidered ball/ribbons, pale fangs, dark nose/claws, carved mane grooves, cracks, and gray stone planes are distinct.
- Updated `src/main/java/com/example/mythic_age/client/render/entity/model/StoneLionGuardianModel.java`:
  - Split previously anonymous cuboids into named runtime parts for shoulder plate, plinth, snout, eyes, nose, fangs, side/bottom/top mane curls, paw pads, individual claws, back paws, ball ribbons, top knot, tail ball, and tail curl.
  - Preserved the existing heavy-step, head, mane, tail, ball, hurt, and death animation behavior.
- Strengthened `tools/chinese_runtime_model_quality_gate.py`:
  - Stone Lion Guardian now requires 40 named runtime parts and 18 detail UV tokens.
  - The gate will fail if the model regresses to a normal lion or a generic gray quadruped.

## Important Decisions

- No Minecraft client or in-game screenshot validation was run, respecting the user's instruction that in-game validation is currently not needed.
- Visual testing was used only to verify the design implementation, not to invent the creature shape by trial and error.
- This is a high-detail polish pass, not final hand-authored release art.

## Files Touched Or Generated

- `tools/chinese_creature_visual_pipeline.py`
- `tools/chinese_runtime_model_quality_gate.py`
- `src/main/java/com/example/mythic_age/client/render/entity/model/StoneLionGuardianModel.java`
- Regenerated Chinese visual artifacts under `docs/美术资源/chinese_creatures/`, `docs/美术资源/blockbench/chinese_creatures/`, `docs/preview_assets/chinese_creatures/`, and Chinese entity textures under `src/main/resources/assets/mythic_age/textures/entity/`.

## Verification

- `python3 tools/chinese_creature_visual_pipeline.py all` PASS.
- Visual inspection opened `docs/preview_assets/chinese_creatures/blockbench_validation_contact_sheet.png`; Stone Lion Guardian now has readable gold eyes, snout, fangs, layered mane curls, gray stone crack detail, red embroidered ball/ribbons, clawed paws, plinth, and curled tail.
- `python3 tools/chinese_runtime_model_quality_gate.py audit` PASS.
- `python3 tools/chinese_creature_visual_pipeline.py audit` PASS.
- `python3 tools/entity_validation_pipeline.py all` PASS, including entity validation and NBT reports.
- Resource JSON validation with `jq empty` over Chinese Blockbench/design/preview assets and `src/main/resources/assets/mythic_age` PASS.
- `./gradlew build --offline` first failed in sandbox on Gradle wrapper `.lck` permission, then PASS with approved external wrapper-cache access.

## Open Questions Or Blockers

- Stone Lion Guardian is improved but not final art.
- No in-game visual validation is currently expected unless the user asks to re-enable MC screenshots.
- Other creatures still need comparable high-detail passes, especially earlier Greek models criticized for insufficient mythological silhouette and facial/feature detail.

## Next Concrete Action

Continue high-detail polish with the next low-readability creature. Strong next targets are Greek `Cyclops`, Greek `Pegasus`, Greek `Hydra`, or broader Greek re-polish to address the user's explicit feedback.
