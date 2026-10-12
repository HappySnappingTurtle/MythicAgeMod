# Changelog

## [1.0.0] - 2026-06-17

### Added
- 30 mythological creatures across 5 civilizations (Greek 10, Egyptian 5, Nordic 5, Chinese 5, Atlantean 5)
- 52 god power spells with civilization-themed particle effects
- 4 temple generation spells (Egyptian, Chinese, Atlantean, Nordic)
- 5 civilization certificate economy systems (Greek mineral exchange, Egyptian statue tiers, Nordic monster drops, Chinese population + trade + labor, Atlantean crystal harvesting)
- Chinese certificate manager with trade interception (VillagerTradeMixin) and labor cooldown tracking
- 15 canonical temple structures with guardian NPCs
- Custom temple guardian trading UI
- God power guide UI
- SpellRegistry with 56 registered spells and pure registry dispatch
- 5 civilization particle effect classes (Greek/Egyptian/Nordic/Chinese/Atlantean)
- 13 custom particle types with textures
- 7 AI goal classes for creature signature abilities (Hydra poison sweep, Medusa stone gaze, Cyclops stone throw, Chimera flame breath, Nemean Lion pounce, Fenrir howl buff, Azure Dragon breath)
- NBT persistence for certificate cooldowns and world state
- Full i18n support (English, Chinese, Japanese)

### Technical
- SpellbookItem refactored from 4334 to 170 lines via SpellRegistry architecture
- AbstractSpell base class with getParticleEffect() auto-selecting civilization particles
- MythicParticleEffects utility with distance-based scaling and intensity tiers
- Creature models with full bone hierarchies and animations (151 animations total)
- Pegasus V4: 2-segment diagonal neck, V4 proportional horse body
- Hydra V4: 9-head fan layout, 3-segment neck paths, low elongated body
