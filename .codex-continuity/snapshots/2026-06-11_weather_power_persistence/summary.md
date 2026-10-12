# Continuity Snapshot: Weather God Power Persistence

Timestamp: 2026-06-11T06:35:00Z

## Completed In This Step

- Added `GodPowerWeatherManager` for long-running weather-based god powers.
- Integrated `GodPowerWeatherManager` into `MythicAgeWorldState`.
- Changed Fimbulwinter and Oceanic Wrath spell casts to register persistent weather state instead of only calling `ServerWorld#setWeather`.
- Kept Crystal Barrier as `runtime_only` because current code applies status effects and particles only; it does not create temporary barrier blocks.
- Extended `entity_validation_pipeline.py` to validate `GodPowerWeatherManager` NBT fields.
- Updated the NBT target document:
  - Fimbulwinter: `covered`.
  - Oceanic Wrath: `covered`.
  - Crystal Barrier: `runtime_only`.

## Verification

- `python3 tools/entity_validation_pipeline.py audit` passed.
- `python3 tools/nbt_target_validation_pipeline.py audit` passed.
- `python3 tools/structure_validation_pipeline.py audit` passed with existing warnings.
- `python3 tools/particle_validation_pipeline.py audit` passed.
- `python3 tools/god_power_particle_validation_pipeline.py audit` passed.
- `python3 tools/ui_validation_pipeline.py audit` passed.
- `python3 tools/mythic_age_1_0_gap_audit.py audit` reports not ready but NBT schema owners increased to 13.
- `./gradlew build --offline` passed.

## Current NBT Report

- Static NBT schema owners: 13.
- NBT target rows: 69.
- Covered: 18.
- Required: 3.
- Runtime-only: 48.
- Candidate/undecided: 0.

## Remaining NBT Required Targets

- `MythicAge_Summoner` for summoned/friendly/god-power entities beyond currently covered Walking Tree.
- `GodPowerCertificateManager.CivilizationProductionState`.
- Temple structure functional markers: `GuardianSpawn`, `RelicPedestal`, `StatueBase`, `Entrance`, `Bounds`.

## Next Recommended Step

Implement or at least freeze validation for certificate production state and temple structure functional markers, then decide whether additional summoned entities beyond Walking Tree need owner NBT in 1.0.
