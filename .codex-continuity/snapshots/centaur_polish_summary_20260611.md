# Centaur Polish Summary - 2026-06-11

## Scope
- Continued the Greek creature visual-quality pass with `centaur`.
- User instruction still stands: design first, no further MC client validation unless explicitly requested.
- Goal remains active for MythicAgeMod 1.0 planning and broader creature/asset/runtime completion.

## Design Package
- Updated `docs/美术资源/greek_creatures/design/centaur.md` through `tools/greek_creature_visual_pipeline.py`.
- Target box is now `1.9w x 3.0h`; visual height is `3.15`.
- Design contract now requires a forward human archer torso, readable head face, bow/string/arrow, back quiver, long horse body, tail, and four separate hooves.
- Fixed design hierarchy and UV plan so face, hair, equipment, horse body, and hooves occupy named UV islands.

## Visual Pipeline
- Updated `tools/greek_creature_visual_pipeline.py`:
  - Centaur geometry expanded to 49 named elements.
  - Added independent `left_eye`, `right_eye`, `nose_bridge`, `mouth`, ears, hair, mane, arms, hands, fingers, bow limbs, bow grip, bow string, arrow, arrow tip, quiver, quiver strap, quiver arrows, tail, and four hooves.
  - Added Centaur-specific texture decoration rules so the final texture is UV-driven, not a generic random panel.
  - Fixed a UV overflow in `quiver_arrow_b`.
- Regenerated `.bbmodel`, UV template, final texture, Blockbench previews, visual reports, and contact sheet.

## Runtime Model
- Rewrote `src/main/java/com/example/mythic_age/client/render/entity/model/CentaurModel.java`.
- Runtime model now exposes the same design-critical parts:
  - horse body/chest/rump/shoulder/belly
  - human torso/waist/shoulders/chest strap
  - head, eyes, nose, mouth, ears, hair
  - arms/forearms/hands/fingers
  - bow, bow string, arrow, arrow tip
  - quiver, strap, quiver arrows
  - tail/tuft and four separate hooves
- Animation now keeps horse gait, human torso stability, face tracking, tail sway, and bow-draw motion.
- Updated `src/main/java/com/example/mythic_age/entity/ModEntities.java` Centaur dimensions from `1.7f x 2.8f` to `1.9f x 3.0f`.

## Quality Gate
- Updated `tools/greek_runtime_model_quality_gate.py` Centaur contract from a coarse 14-part check to a strict high-detail archer-centaur contract with required UV/detail tokens.

## Verification
- `python3 tools/greek_creature_visual_pipeline.py all` passed.
- `python3 tools/greek_runtime_model_quality_gate.py audit` passed.
- `python3 tools/entity_validation_pipeline.py all` passed, including NBT schema and runtime NBT report generation.
- JSON validation passed:
  - `find docs/美术资源/blockbench/greek_creatures docs/美术资源/greek_creatures docs/preview_assets/greek_creatures src/main/resources/assets/mythic_age -name '*.json' -print0 | xargs -0 -n 1 jq empty`
- Sandbox Gradle build failed only on external wrapper lock permission:
  - `/Users/hongyuwu/project/gradle-repository/wrapper/dists/gradle-8.2-bin/.../gradle-8.2-bin.zip.lck`
- Escalated `./gradlew build --offline` passed.

## Visual Check
- Reviewed:
  - `docs/preview_assets/greek_creatures/centaur/blockbench/front.png`
  - `docs/preview_assets/greek_creatures/centaur/blockbench/side.png`
  - `docs/preview_assets/greek_creatures/centaur/blockbench/three_quarter.png`
  - `src/main/resources/assets/mythic_age/textures/entity/centaur/centaur.png`
- Front reads as human archer torso with eyes/nose/mouth, bow, and separated hooves.
- Side reads as long horse body, forward human torso, bow, quiver, and tail.
- Texture has no obvious transparent blank islands.

## Next Work
- Continue Greek polish with remaining lower-detail creatures, likely `colossus` or `medusa`, then consolidate the wider 1.0 plan across civilizations, NBT, UI, particles, animation, and structure/resource gates.
