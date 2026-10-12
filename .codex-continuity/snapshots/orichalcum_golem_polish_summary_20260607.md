# Orichalcum Golem Polish Summary - 2026-06-07

## Current Objective

Continue MythicAgeMod 1.0 development across code, models, actions, textures, UI, art, particles, and NBT. After all five civilization creature sets gained V2/runtime model gates, this slice began the high-detail art polish phase by improving Orichalcum Golem, the weakest Atlantean silhouette.

## Completed Work

- Audited `Orichalcum Golem / 山铜魔像` and confirmed the previous version still read too much like a generic blocky humanoid.
- Strengthened `tools/atlantean_creature_visual_pipeline.py` for `orichalcum_golem`:
  - Added detailed parts: `core_frame`, `shoulder_plates`, `back_crystal_array`, `forearm_plates`, `giant_fists`, `knee_plates`, and `heavy_feet`.
  - Expanded geometry from a simple body/arms/legs layout into a more layered construct silhouette.
  - Fixed UV overflow found by the visual audit on `right_fist` and `right_heavy_foot`.
- Updated `src/main/java/com/example/mythic_age/client/render/entity/model/OrichalcumGolemModel.java`:
  - Added `chest_core` as a child of body.
  - Added `crest_crystal` as a child of head.
  - Enlarged and framed the chest core.
  - Added stronger shoulder plates, back crystal array, forearm armor, oversized fists, knee plates, and heavy feet.
  - Added subtle animation hooks for crest crystal and chest core.
- Strengthened `tools/atlantean_runtime_model_quality_gate.py` so the golem runtime contract now requires the new detail tokens and runtime part names.

## Important Decisions

- No Minecraft client or in-game screenshot validation was run, respecting the user's instruction that in-game validation is currently not needed.
- This is a real polish pass, not just a gate tweak: Blockbench contact sheet now shows clearer core, shoulder armor, back crystals, giant fists, knee armor, and heavy feet.
- The model is improved but still not final hand-authored 1.0 art.

## Files Touched Or Generated

- `tools/atlantean_creature_visual_pipeline.py`
- `tools/atlantean_runtime_model_quality_gate.py`
- `src/main/java/com/example/mythic_age/client/render/entity/model/OrichalcumGolemModel.java`
- Regenerated Atlantean visual artifacts under `docs/美术资源/atlantean_creatures/`, `docs/美术资源/blockbench/atlantean_creatures/`, `docs/preview_assets/atlantean_creatures/`, and `src/main/resources/assets/mythic_age/textures/entity/`.

## Verification

- `python3 tools/atlantean_creature_visual_pipeline.py all` initially failed on UV overflow for `right_fist` and `right_heavy_foot`; fixed UV layout and reran.
- `python3 tools/atlantean_creature_visual_pipeline.py all` PASS after fix.
- Visual inspection opened `docs/preview_assets/atlantean_creatures/blockbench_validation_contact_sheet.png`; Orichalcum Golem now reads as a more layered construct silhouette.
- `python3 tools/atlantean_runtime_model_quality_gate.py audit` PASS.
- `python3 tools/atlantean_creature_visual_pipeline.py audit` PASS.
- `python3 tools/entity_validation_pipeline.py all` PASS.
- JSON validation over Atlantean Blockbench/design/preview/assets and resource JSON passed with `jq empty`.
- `./gradlew build --offline` first failed in sandbox on Gradle wrapper `.lck` permission, then PASS with approved external wrapper-cache access.

## Open Questions Or Blockers

- Orichalcum Golem is improved but not final art.
- Next weakest art targets remain:
  - Siren Priestess face/fin-crown readability.
  - Nordic Draugr Warrior and Dwarf Berserker blockiness.
  - Chinese Jiangshi face/talisman readability.
  - Nine-tailed Fox tail separation and Stone Lion sculptural mane.

## Next Concrete Action

Continue the high-detail polish phase. A good next target is Siren Priestess because it has a clear design contract but weak face/fin-crown readability in the current contact sheet.
