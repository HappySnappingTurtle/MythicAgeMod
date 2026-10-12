# Decisions

| Date | Decision | Rationale | Consequences |
| --- | --- | --- | --- |
| 2026-05-26 | Use a design-first gate for Greek creature work. | User identified that models were being built without sufficiently explicit mythological design intent. | Update concept/design source before model or texture work; tests validate design implementation rather than inventing the design. |
| 2026-05-26 | Treat generated `.bbmodel`, design docs, concept PNGs, UV templates, textures, and reports as outputs of `tools/greek_creature_visual_pipeline.py`. | Prevent generated artifacts and source-of-truth definitions from drifting. | Durable changes should start in the pipeline, then regenerate. |
| 2026-05-26 | Skip MC in-game screenshot validation for the current round. | User explicitly said it is not needed. | Do not launch `runClient` or visual probe unless user asks again. Build, visual pipeline audit, and entity/NBT audit are enough for this continuity state. |
| 2026-05-26 | Old MC screenshots are stale for newly edited models. | Hydra/Cyclops/Pegasus and other Java models changed after the old screenshots. | If MC proof is later requested, re-capture screenshots with the probe; do not cite old screenshots as current proof. |

<!-- CODEX-CONTINUITY:AUTO:START -->
## Latest Recorded Decisions (2026-06-16T02:00:24Z)

- Session: session-20260526T065453Z-e77e4a09
- Compression: cmp-20260616T020024Z-decd1135

- Current Pegasus and Hydra models are rejected as modeling bases; V6 specs are review materials, not approval.
- Tests validate approved design implementation and must not be used for shape trial-and-error.
<!-- CODEX-CONTINUITY:AUTO:END -->
