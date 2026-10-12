Current objective
- Continue MythicAgeMod 1.0 development across code, models, actions, textures, UI, art, particles, and NBT. This slice continued the design-first Greek creature V2 correction after the user said in-game validation is no longer needed.

Completed work
- Audited the remaining Greek V2 visual gap and confirmed only Cyclops, Hydra, and Pegasus previously had `v2_design_freeze` markers.
- Added V2 design-freeze requirements for Centaur, Nemean Lion, Manticore, and Chimera in `tools/greek_creature_visual_pipeline.py`.
- Regenerated design packages, concept modeling-contract images, `.bbmodel` files, UV templates, runtime textures, texture-fit previews, Blockbench previews, contact sheets, visual manifest, and visual report for all 9 Greek pipeline creatures.
- Updated Centaur Java runtime model with explicit `human_shoulders`, readable forward head/face, `bow_string`, `tail_tuft`, stronger horse chest/body, and four-leg silhouette.
- Updated Nemean Lion Java runtime model with explicit radial mane parts: `mane_top`, `mane_left`, `mane_right`, `mane_bottom`, thicker shoulder/chest, fangs, paws, claws, and tail tuft.
- Updated Manticore Java runtime model with segmented wing membranes/bones and a raised multi-segment scorpion tail ending in a distinct poison sting.
- Updated Chimera Java runtime model with stronger lion head/mane/snout/eyes, larger horned goat head, `goat_beard`, `serpent_jaw`, snake tongue, wing/fire silhouette, and raised serpent tail structure.
- Strengthened `tools/greek_runtime_model_quality_gate.py` so Centaur, Nemean Lion, Manticore, and Chimera contracts now check the new V2 runtime part names.
- Updated `docs/项目管理/MythicAgeMod_1.0大版本开发规划_2026-06-05.md` to record the second Greek V2 batch, clarify that the visual pipeline covers 9 Greek myth creatures while `minotaur` still needs separate design-first coverage, and record the current decision not to run MC validation.

Important decisions
- No Minecraft client or in-game screenshot validation was run, respecting the user's latest instruction.
- The work remains programmatic Blockbench-grade correction, not final hand-authored creature art. `visual_report.json` still marks the V2 creatures as `blockbench_passed_pending_human_shape_review`.
- For small parts, the UV/texture-fit gate should remain strict. When `centaur:arrow` failed as visually blank, the fix was to add arrow shaft/head texture detail, not weaken the gate.

Files touched or inspected
- `tools/greek_creature_visual_pipeline.py`
- `tools/greek_runtime_model_quality_gate.py`
- `src/main/java/com/example/mythic_age/client/render/entity/model/CentaurModel.java`
- `src/main/java/com/example/mythic_age/client/render/entity/model/NemeanLionModel.java`
- `src/main/java/com/example/mythic_age/client/render/entity/model/ManticoreModel.java`
- `src/main/java/com/example/mythic_age/client/render/entity/model/ChimeraModel.java`
- `docs/项目管理/MythicAgeMod_1.0大版本开发规划_2026-06-05.md`
- Generated Greek visual artifacts under `docs/美术资源/greek_creatures/`, `docs/美术资源/blockbench/greek_creatures/`, `docs/preview_assets/greek_creatures/`, and `src/main/resources/assets/mythic_age/textures/entity/`.

Verification
- `python3 tools/greek_creature_visual_pipeline.py all` PASS after fixing UV overflow and adding arrow texture detail.
- Visual inspection opened `docs/preview_assets/greek_creatures/blockbench_validation_contact_sheet.png`; the second batch now has clearer mythological silhouettes, but still needs human art review for final quality.
- `python3 tools/greek_runtime_model_quality_gate.py audit` PASS.
- `python3 tools/greek_creature_visual_pipeline.py audit` PASS.
- `python3 tools/entity_validation_pipeline.py all` PASS, including NBT schema/action-particle reports.
- `python3 tools/particle_validation_pipeline.py audit` PASS.
- `python3 tools/god_power_particle_validation_pipeline.py audit` PASS.
- `python3 tools/ui_validation_pipeline.py audit` PASS.
- `python3 tools/structure_validation_pipeline.py audit` PASS with 4 existing warnings: old Greek small temple path/validation warning and `.DS_Store` artifacts under data paths.
- `find docs/美术资源/blockbench/greek_creatures docs/美术资源/greek_creatures src/main/resources/assets/mythic_age -name '*.json' -print0 | xargs -0 -n 1 jq empty` PASS.
- `./gradlew build --offline` first failed in sandbox on Gradle wrapper `.lck` permission, then PASS with approved external wrapper-cache access.

Open questions or blockers
- Human art review is still required for all Greek creatures before declaring 1.0 visual quality.
- `medusa` and `colossus` are not V2-remodeled in this slice, though they still pass existing gates.
- `minotaur` is listed among current Greek entities but is not yet part of the Greek design-first visual pipeline and should be added or audited separately.
- Structure gate warnings are non-failing but should be cleaned before 1.0 packaging.

Next concrete action
- Either complete V2 design-first coverage for Medusa, Colossus, and Minotaur, or move to the next 1.0 slice: non-Greek civilization creature rosters and final art/animation pipeline under the same design-first gate.
