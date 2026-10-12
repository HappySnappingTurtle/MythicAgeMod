# Commands

| Purpose | Command | Notes |
| --- | --- | --- |
| Build the mod offline | `./gradlew build --offline` | Requires access to the local Gradle wrapper/cache. If sandbox blocks the wrapper lock file, rerun with escalated permission. |
| Generate all Greek visual design artifacts | `python3 tools/greek_creature_visual_pipeline.py all` | Regenerates design docs, concept art, `.bbmodel`, textures, UV templates, texture-fit report, Blockbench previews, contact sheets, manifest, and visual report. |
| Audit Greek visual gate only | `python3 tools/greek_creature_visual_pipeline.py audit` | Checks design docs, concept art, `.bbmodel`, UV bounds, texture opacity/variation, Blockbench previews, and entity dimensions. |
| Audit entity/NBT validation | `python3 tools/entity_validation_pipeline.py audit` | Checks entity validation manifest, NBT schema report, and runtime NBT report file generated earlier. |
| Resume continuity context | `python3 /Users/hongyuwu/.codex/skills/codex-continuity/scripts/continuity.py resume --project-root "$PWD"` | First command for a future Codex run after context loss. Read the files it prints. |
| Record a new continuity summary | `python3 /Users/hongyuwu/.codex/skills/codex-continuity/scripts/continuity.py record-compression --project-root "$PWD" --summary-file /path/to/summary.md --trigger manual-update` | Append-only; do not rewrite old compression records. |
| Optional MC visual probe | `JAVA_HOME=/Users/hongyuwu/Library/Java/JavaVirtualMachines/ms-17.0.15/Contents/Home JAVA_TOOL_OPTIONS='-Dmythic.visualProbe=true -Dmythic.visualProbePort=47620 -Dmythic.visualProbeWorldBase64=5paw55qE5LiW55WM' ./gradlew runClient` | Current user instruction says not to run MC validation. Keep this only as a known future command if the user asks. |
