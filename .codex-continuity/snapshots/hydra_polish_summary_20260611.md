# Hydra polish summary 2026-06-11

## Current objective

Continue MythicAgeMod 1.0 development from the design-first visual acceptance plan. The long-running goal is still active: the mod must grow beyond weak Greek-only prototypes into a 1.0 scope covering code, models, animation, textures, UI, art, particles, NBT/runtime validation, and multiple civilizations.

## Completed work in this slice

- Reworked Hydra as the next high-priority Greek creature polish target after Cyclops and Pegasus.
- Strengthened `tools/greek_creature_visual_pipeline.py` Hydra design contract:
  - low slender body instead of bulky torso
  - five independent necks and large readable heads as the current MC-readable Hydra representation
  - explicit per-head brows, eyes, upper/lower jaws, mouth lines, fangs, horns, poison glands
  - shoulder saddle, spine crests, long tail, tail tip, scale bands, and toxic-green accent plan
- Regenerated `docs/美术资源/greek_creatures/design/hydra.md`, Blockbench-style outputs, preview assets, and `src/main/resources/assets/mythic_age/textures/entity/hydra/hydra.png`.
- Rebuilt `HydraModel.java` so the runtime Java model has explicit named child parts instead of vague combined cuboids:
  - `shoulder_saddle`
  - `spine_crest_a/b/c`
  - `tail`, `tail_mid`, `tail_tip`
  - five necks and five heads
  - per-head throat ridge, upper/lower jaw, brow, left/right eyes, fangs, horns, and poison glands
- Upgraded `tools/greek_runtime_model_quality_gate.py` so it can expand dynamic model names from `HEAD_NAMES`, `NECK_NAMES`, and expressions like `headName + "_left_eye"`.
- Strengthened the Hydra runtime gate to require 82 named runtime parts and 18 implementation tokens.

## Important decisions

- No MC in-game validation was run because the user explicitly said in-game validation is not needed for this stage.
- The Hydra still uses five heads rather than literal nine heads for current MC readability and animation feasibility; this remains documented as the frozen current representation rather than final premium art.
- Tests are now used to verify the design contract, not to invent the creature shape by trial and error.

## Files touched or generated

- `tools/greek_creature_visual_pipeline.py`
- `tools/greek_runtime_model_quality_gate.py`
- `src/main/java/com/example/mythic_age/client/render/entity/model/HydraModel.java`
- `src/main/resources/assets/mythic_age/textures/entity/hydra/hydra.png`
- `docs/美术资源/greek_creatures/design/hydra.md`
- `docs/preview_assets/greek_creatures/runtime_model_quality_report.json`
- Greek preview and Blockbench-style generated assets under `docs/preview_assets/greek_creatures/` and `docs/美术资源/blockbench/greek_creatures/`

## Verification

- `python3 tools/greek_creature_visual_pipeline.py all` passed.
- `python3 tools/greek_creature_visual_pipeline.py audit` passed.
- `python3 tools/greek_runtime_model_quality_gate.py audit` passed.
- Hydra runtime report shows `ok: true`, `missing_parts: []`, `missing_tokens: []`, `required_parts_count: 82`, `required_tokens_count: 18`.
- `python3 tools/entity_validation_pipeline.py all` passed for manifest, NBT schema report, action/particle report, and MC runtime NBT report.
- JSON resource validation with `jq empty` passed over Greek/design/preview/assets JSON.
- `python3 -m json.tool docs/preview_assets/entity_validation/nbt_schema_report.json` passed.
- `python3 -m json.tool run/visual_probe/entity_nbt_report.json` passed.
- `./gradlew build --offline` initially failed inside sandbox on the external Gradle wrapper lock file, then passed with approved escalation:
  - `BUILD SUCCESSFUL in 12s`
  - 4 actionable tasks executed

## Open work

- The full 1.0 objective is not complete.
- Remaining Greek creatures still need design-first high-detail runtime gates and model/texture polish, especially Chimera, Manticore, Nemean Lion, Minotaur, Centaur, and possibly Colossus.
- Other civilizations need the same level of runtime visual gates, entity/NBT verification, particles, UI, item/spell integration, and art checks.

## Next concrete action

Polish Chimera next because it depends on a distinct three-creature silhouette: lion head/mane, goat head/horns/beard, serpent tail/head/jaw/tongue, wings, paws/claws, and body segmentation. Strengthen the design package, UV/texture semantics, Java model named parts, runtime gate, and local validation without MC screenshot validation.
