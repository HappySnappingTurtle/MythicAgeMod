# Minotaur polish summary 2026-06-11

## Current objective

Continue MythicAgeMod 1.0 development. The active goal remains incomplete: the mod still needs broad 1.0 work across code, models, animation, textures, UI, art, particles, NBT/runtime validation, and multiple civilizations.

## Completed work in this slice

- Selected Minotaur after Nemean Lion because it had a recognizable base but still risked reading as a generic humanoid with a bull head and simple axe.
- Strengthened the Minotaur design contract in `tools/greek_creature_visual_pipeline.py`:
  - explicit bull face: muzzle, heavy brow, left/right eyes, left/right nostrils, mouth, horns, ears
  - stronger humanoid torso: chest harness, left/right chest muscles, waist belt, loincloth
  - split arms and hands: forearms, fists, fingers
  - split labrys axe: handle, left blade, right blade, center socket, counterweight
  - split hooves: left/right hoof and hoof claw
  - stricter UV plan, texture detail rules, modeling musts, rejection triggers, and V2 freeze text
- Added Minotaur-specific texture semantics:
  - pale horn gradients
  - dark eyes, nostrils, brow, and mouth line
  - leather harness, belt, loincloth, chest shading
  - dark fingers, hooves, hoof claws
  - metal axe blades, wooden handle, socket, and counterweight
- Replaced `MinotaurModel.java` with a high-detail runtime model:
  - 42 required runtime parts
  - explicit bull face, body harness/muscle/belt/loincloth, hand/finger parts, hoof/claw parts, and multi-part labrys axe
  - charge animation lowers the head and tilts the labrys while keeping the weapon visible
- Strengthened `tools/greek_runtime_model_quality_gate.py` for Minotaur:
  - required parts expanded from 15 coarse parts to 42 detailed parts
  - required tokens now cover nostrils, ears, chest muscles, waist/loincloth, forearms, fingers, axe subparts, hoof claws, and charge animation hooks
- Regenerated Minotaur design package, `.bbmodel`, UV template, texture, preview images, reports, and Greek contact sheets.

## Visual review

- Front preview now shows the bull muzzle, eyes, nostrils, mouth, large horns, chest/waist detail, hooves, and labrys.
- Side preview shows the forward bull muzzle and tall humanoid body; axe readability is strongest from front and three-quarter views.
- Three-quarter preview reads as a large bull-headed humanoid with weapon and hooves rather than a plain humanoid.
- No MC in-game validation was run because the user explicitly said in-game validation is not needed for this stage.

## Files touched or generated

- `tools/greek_creature_visual_pipeline.py`
- `tools/greek_runtime_model_quality_gate.py`
- `src/main/java/com/example/mythic_age/client/render/entity/model/MinotaurModel.java`
- `docs/美术资源/greek_creatures/design/minotaur.md`
- `docs/美术资源/blockbench/greek_creatures/minotaur.bbmodel`
- `src/main/resources/assets/mythic_age/textures/entity/minotaur/minotaur.png`
- `docs/preview_assets/greek_creatures/minotaur/blockbench/front.png`
- `docs/preview_assets/greek_creatures/minotaur/blockbench/side.png`
- `docs/preview_assets/greek_creatures/minotaur/blockbench/three_quarter.png`
- `docs/preview_assets/greek_creatures/runtime_model_quality_report.json`
- `docs/preview_assets/greek_creatures/texture_fit_report.json`

## Verification

- `python3 tools/greek_creature_visual_pipeline.py all` passed.
- Manual preview inspection completed for Minotaur front/side/three-quarter renders.
- `python3 tools/greek_runtime_model_quality_gate.py audit` passed.
- Minotaur runtime report:
  - `ok: true`
  - `required_parts_count: 42`
  - `required_tokens_count: 19`
  - `missing_parts: []`
  - `missing_tokens: []`
- Minotaur texture fit report:
  - `ok: true`
  - `elements_checked: 41`
  - `failures: []`
- `python3 tools/entity_validation_pipeline.py all` passed.
- JSON resource validation over Greek/design/preview/assets passed with `jq empty`.
- `python3 -m json.tool docs/preview_assets/entity_validation/nbt_schema_report.json` passed.
- `./gradlew build --offline` initially failed in sandbox on the external Gradle wrapper lock file, then passed with approved escalation:
  - `BUILD SUCCESSFUL in 12s`
  - 4 actionable tasks executed

## Open work

- The active 1.0 goal is not complete.
- Remaining Greek polish target is Centaur, plus follow-up checks for Colossus/Medusa if needed.
- Other civilizations still need the same design-first and runtime-gated treatment plus broader gameplay, UI, particles, structures, items, spells, and NBT validation.

## Next concrete action

Continue with Centaur next. It currently has a horse body and bow, but should be pushed toward a more readable human-horse hybrid with human face/torso, horse chest/body, four legs/hooves, bow, string, quiver or warrior details, and clearer animation hooks.
