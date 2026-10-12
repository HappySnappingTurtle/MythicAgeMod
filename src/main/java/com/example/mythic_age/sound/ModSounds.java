package com.example.mythic_age.sound;

import com.example.mythic_age.MythicAgeMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    // Minotaur Sounds
    public static final SoundEvent ENTITY_MINOTAUR_AMBIENT = registerSoundEvent("entity.minotaur.ambient");
    public static final SoundEvent ENTITY_MINOTAUR_HURT = registerSoundEvent("entity.minotaur.hurt");
    public static final SoundEvent ENTITY_MINOTAUR_DEATH = registerSoundEvent("entity.minotaur.death");
    public static final SoundEvent ENTITY_MINOTAUR_STEP = registerSoundEvent("entity.minotaur.step");
    public static final SoundEvent ENTITY_MINOTAUR_CHARGE = registerSoundEvent("entity.minotaur.charge"); // For charge attack

    public static final SoundEvent ENTITY_CYCLOPS_AMBIENT = registerSoundEvent("entity.cyclops.ambient");
    public static final SoundEvent ENTITY_CYCLOPS_HURT = registerSoundEvent("entity.cyclops.hurt");
    public static final SoundEvent ENTITY_CYCLOPS_DEATH = registerSoundEvent("entity.cyclops.death");
    public static final SoundEvent ENTITY_CYCLOPS_STEP = registerSoundEvent("entity.cyclops.step");
    public static final SoundEvent ENTITY_CYCLOPS_ATTACK = registerSoundEvent("entity.cyclops.attack");
    public static final SoundEvent ENTITY_CYCLOPS_SPECIAL = registerSoundEvent("entity.cyclops.special");

    public static final SoundEvent ENTITY_CENTAUR_AMBIENT = registerSoundEvent("entity.centaur.ambient");
    public static final SoundEvent ENTITY_CENTAUR_HURT = registerSoundEvent("entity.centaur.hurt");
    public static final SoundEvent ENTITY_CENTAUR_DEATH = registerSoundEvent("entity.centaur.death");
    public static final SoundEvent ENTITY_CENTAUR_STEP = registerSoundEvent("entity.centaur.step");
    public static final SoundEvent ENTITY_CENTAUR_ATTACK = registerSoundEvent("entity.centaur.attack");
    public static final SoundEvent ENTITY_CENTAUR_SPECIAL = registerSoundEvent("entity.centaur.special");

    public static final SoundEvent ENTITY_HYDRA_AMBIENT = registerSoundEvent("entity.hydra.ambient");
    public static final SoundEvent ENTITY_HYDRA_HURT = registerSoundEvent("entity.hydra.hurt");
    public static final SoundEvent ENTITY_HYDRA_DEATH = registerSoundEvent("entity.hydra.death");
    public static final SoundEvent ENTITY_HYDRA_STEP = registerSoundEvent("entity.hydra.step");
    public static final SoundEvent ENTITY_HYDRA_ATTACK = registerSoundEvent("entity.hydra.attack");
    public static final SoundEvent ENTITY_HYDRA_SPECIAL = registerSoundEvent("entity.hydra.special");

    public static final SoundEvent ENTITY_MEDUSA_AMBIENT = registerSoundEvent("entity.medusa.ambient");
    public static final SoundEvent ENTITY_MEDUSA_HURT = registerSoundEvent("entity.medusa.hurt");
    public static final SoundEvent ENTITY_MEDUSA_DEATH = registerSoundEvent("entity.medusa.death");
    public static final SoundEvent ENTITY_MEDUSA_STEP = registerSoundEvent("entity.medusa.step");
    public static final SoundEvent ENTITY_MEDUSA_ATTACK = registerSoundEvent("entity.medusa.attack");
    public static final SoundEvent ENTITY_MEDUSA_SPECIAL = registerSoundEvent("entity.medusa.special");

    public static final SoundEvent ENTITY_NEMEAN_LION_AMBIENT = registerSoundEvent("entity.nemean_lion.ambient");
    public static final SoundEvent ENTITY_NEMEAN_LION_HURT = registerSoundEvent("entity.nemean_lion.hurt");
    public static final SoundEvent ENTITY_NEMEAN_LION_DEATH = registerSoundEvent("entity.nemean_lion.death");
    public static final SoundEvent ENTITY_NEMEAN_LION_STEP = registerSoundEvent("entity.nemean_lion.step");
    public static final SoundEvent ENTITY_NEMEAN_LION_ATTACK = registerSoundEvent("entity.nemean_lion.attack");
    public static final SoundEvent ENTITY_NEMEAN_LION_SPECIAL = registerSoundEvent("entity.nemean_lion.special");

    public static final SoundEvent ENTITY_MANTICORE_AMBIENT = registerSoundEvent("entity.manticore.ambient");
    public static final SoundEvent ENTITY_MANTICORE_HURT = registerSoundEvent("entity.manticore.hurt");
    public static final SoundEvent ENTITY_MANTICORE_DEATH = registerSoundEvent("entity.manticore.death");
    public static final SoundEvent ENTITY_MANTICORE_STEP = registerSoundEvent("entity.manticore.step");
    public static final SoundEvent ENTITY_MANTICORE_ATTACK = registerSoundEvent("entity.manticore.attack");
    public static final SoundEvent ENTITY_MANTICORE_SPECIAL = registerSoundEvent("entity.manticore.special");

    public static final SoundEvent ENTITY_CHIMERA_AMBIENT = registerSoundEvent("entity.chimera.ambient");
    public static final SoundEvent ENTITY_CHIMERA_HURT = registerSoundEvent("entity.chimera.hurt");
    public static final SoundEvent ENTITY_CHIMERA_DEATH = registerSoundEvent("entity.chimera.death");
    public static final SoundEvent ENTITY_CHIMERA_STEP = registerSoundEvent("entity.chimera.step");
    public static final SoundEvent ENTITY_CHIMERA_ATTACK = registerSoundEvent("entity.chimera.attack");
    public static final SoundEvent ENTITY_CHIMERA_SPECIAL = registerSoundEvent("entity.chimera.special");

    public static final SoundEvent ENTITY_COLOSSUS_AMBIENT = registerSoundEvent("entity.colossus.ambient");
    public static final SoundEvent ENTITY_COLOSSUS_HURT = registerSoundEvent("entity.colossus.hurt");
    public static final SoundEvent ENTITY_COLOSSUS_DEATH = registerSoundEvent("entity.colossus.death");
    public static final SoundEvent ENTITY_COLOSSUS_STEP = registerSoundEvent("entity.colossus.step");
    public static final SoundEvent ENTITY_COLOSSUS_ATTACK = registerSoundEvent("entity.colossus.attack");
    public static final SoundEvent ENTITY_COLOSSUS_SPECIAL = registerSoundEvent("entity.colossus.special");

    public static final SoundEvent ENTITY_PEGASUS_AMBIENT = registerSoundEvent("entity.pegasus.ambient");
    public static final SoundEvent ENTITY_PEGASUS_HURT = registerSoundEvent("entity.pegasus.hurt");
    public static final SoundEvent ENTITY_PEGASUS_DEATH = registerSoundEvent("entity.pegasus.death");
    public static final SoundEvent ENTITY_PEGASUS_STEP = registerSoundEvent("entity.pegasus.step");
    public static final SoundEvent ENTITY_PEGASUS_FLY = registerSoundEvent("entity.pegasus.fly");
    public static final SoundEvent ENTITY_PEGASUS_SPECIAL = registerSoundEvent("entity.pegasus.special");

    // Non-Greek Myth Creature Sounds
    // anubis_warrior
    public static final SoundEvent ENTITY_ANUBIS_WARRIOR_AMBIENT = registerSoundEvent("entity.anubis_warrior.ambient");
    public static final SoundEvent ENTITY_ANUBIS_WARRIOR_HURT = registerSoundEvent("entity.anubis_warrior.hurt");
    public static final SoundEvent ENTITY_ANUBIS_WARRIOR_DEATH = registerSoundEvent("entity.anubis_warrior.death");
    public static final SoundEvent ENTITY_ANUBIS_WARRIOR_ATTACK = registerSoundEvent("entity.anubis_warrior.attack");
    public static final SoundEvent ENTITY_ANUBIS_WARRIOR_SPECIAL = registerSoundEvent("entity.anubis_warrior.special");

    // scarab_guardian
    public static final SoundEvent ENTITY_SCARAB_GUARDIAN_AMBIENT = registerSoundEvent("entity.scarab_guardian.ambient");
    public static final SoundEvent ENTITY_SCARAB_GUARDIAN_HURT = registerSoundEvent("entity.scarab_guardian.hurt");
    public static final SoundEvent ENTITY_SCARAB_GUARDIAN_DEATH = registerSoundEvent("entity.scarab_guardian.death");
    public static final SoundEvent ENTITY_SCARAB_GUARDIAN_ATTACK = registerSoundEvent("entity.scarab_guardian.attack");
    public static final SoundEvent ENTITY_SCARAB_GUARDIAN_SPECIAL = registerSoundEvent("entity.scarab_guardian.special");

    // mummy_soldier
    public static final SoundEvent ENTITY_MUMMY_SOLDIER_AMBIENT = registerSoundEvent("entity.mummy_soldier.ambient");
    public static final SoundEvent ENTITY_MUMMY_SOLDIER_HURT = registerSoundEvent("entity.mummy_soldier.hurt");
    public static final SoundEvent ENTITY_MUMMY_SOLDIER_DEATH = registerSoundEvent("entity.mummy_soldier.death");
    public static final SoundEvent ENTITY_MUMMY_SOLDIER_ATTACK = registerSoundEvent("entity.mummy_soldier.attack");
    public static final SoundEvent ENTITY_MUMMY_SOLDIER_SPECIAL = registerSoundEvent("entity.mummy_soldier.special");

    // sphinx_sentinel
    public static final SoundEvent ENTITY_SPHINX_SENTINEL_AMBIENT = registerSoundEvent("entity.sphinx_sentinel.ambient");
    public static final SoundEvent ENTITY_SPHINX_SENTINEL_HURT = registerSoundEvent("entity.sphinx_sentinel.hurt");
    public static final SoundEvent ENTITY_SPHINX_SENTINEL_DEATH = registerSoundEvent("entity.sphinx_sentinel.death");
    public static final SoundEvent ENTITY_SPHINX_SENTINEL_ATTACK = registerSoundEvent("entity.sphinx_sentinel.attack");
    public static final SoundEvent ENTITY_SPHINX_SENTINEL_SPECIAL = registerSoundEvent("entity.sphinx_sentinel.special");

    // sun_falcon
    public static final SoundEvent ENTITY_SUN_FALCON_AMBIENT = registerSoundEvent("entity.sun_falcon.ambient");
    public static final SoundEvent ENTITY_SUN_FALCON_HURT = registerSoundEvent("entity.sun_falcon.hurt");
    public static final SoundEvent ENTITY_SUN_FALCON_DEATH = registerSoundEvent("entity.sun_falcon.death");
    public static final SoundEvent ENTITY_SUN_FALCON_ATTACK = registerSoundEvent("entity.sun_falcon.attack");
    public static final SoundEvent ENTITY_SUN_FALCON_SPECIAL = registerSoundEvent("entity.sun_falcon.special");

    // frost_giant
    public static final SoundEvent ENTITY_FROST_GIANT_AMBIENT = registerSoundEvent("entity.frost_giant.ambient");
    public static final SoundEvent ENTITY_FROST_GIANT_HURT = registerSoundEvent("entity.frost_giant.hurt");
    public static final SoundEvent ENTITY_FROST_GIANT_DEATH = registerSoundEvent("entity.frost_giant.death");
    public static final SoundEvent ENTITY_FROST_GIANT_ATTACK = registerSoundEvent("entity.frost_giant.attack");
    public static final SoundEvent ENTITY_FROST_GIANT_SPECIAL = registerSoundEvent("entity.frost_giant.special");

    // draugr_warrior
    public static final SoundEvent ENTITY_DRAUGR_WARRIOR_AMBIENT = registerSoundEvent("entity.draugr_warrior.ambient");
    public static final SoundEvent ENTITY_DRAUGR_WARRIOR_HURT = registerSoundEvent("entity.draugr_warrior.hurt");
    public static final SoundEvent ENTITY_DRAUGR_WARRIOR_DEATH = registerSoundEvent("entity.draugr_warrior.death");
    public static final SoundEvent ENTITY_DRAUGR_WARRIOR_ATTACK = registerSoundEvent("entity.draugr_warrior.attack");
    public static final SoundEvent ENTITY_DRAUGR_WARRIOR_SPECIAL = registerSoundEvent("entity.draugr_warrior.special");

    // valkyrie
    public static final SoundEvent ENTITY_VALKYRIE_AMBIENT = registerSoundEvent("entity.valkyrie.ambient");
    public static final SoundEvent ENTITY_VALKYRIE_HURT = registerSoundEvent("entity.valkyrie.hurt");
    public static final SoundEvent ENTITY_VALKYRIE_DEATH = registerSoundEvent("entity.valkyrie.death");
    public static final SoundEvent ENTITY_VALKYRIE_ATTACK = registerSoundEvent("entity.valkyrie.attack");
    public static final SoundEvent ENTITY_VALKYRIE_SPECIAL = registerSoundEvent("entity.valkyrie.special");

    // fenrir_wolf
    public static final SoundEvent ENTITY_FENRIR_WOLF_AMBIENT = registerSoundEvent("entity.fenrir_wolf.ambient");
    public static final SoundEvent ENTITY_FENRIR_WOLF_HURT = registerSoundEvent("entity.fenrir_wolf.hurt");
    public static final SoundEvent ENTITY_FENRIR_WOLF_DEATH = registerSoundEvent("entity.fenrir_wolf.death");
    public static final SoundEvent ENTITY_FENRIR_WOLF_ATTACK = registerSoundEvent("entity.fenrir_wolf.attack");
    public static final SoundEvent ENTITY_FENRIR_WOLF_SPECIAL = registerSoundEvent("entity.fenrir_wolf.special");

    // dwarf_berserker
    public static final SoundEvent ENTITY_DWARF_BERSERKER_AMBIENT = registerSoundEvent("entity.dwarf_berserker.ambient");
    public static final SoundEvent ENTITY_DWARF_BERSERKER_HURT = registerSoundEvent("entity.dwarf_berserker.hurt");
    public static final SoundEvent ENTITY_DWARF_BERSERKER_DEATH = registerSoundEvent("entity.dwarf_berserker.death");
    public static final SoundEvent ENTITY_DWARF_BERSERKER_ATTACK = registerSoundEvent("entity.dwarf_berserker.attack");
    public static final SoundEvent ENTITY_DWARF_BERSERKER_SPECIAL = registerSoundEvent("entity.dwarf_berserker.special");

    // azure_dragon
    public static final SoundEvent ENTITY_AZURE_DRAGON_AMBIENT = registerSoundEvent("entity.azure_dragon.ambient");
    public static final SoundEvent ENTITY_AZURE_DRAGON_HURT = registerSoundEvent("entity.azure_dragon.hurt");
    public static final SoundEvent ENTITY_AZURE_DRAGON_DEATH = registerSoundEvent("entity.azure_dragon.death");
    public static final SoundEvent ENTITY_AZURE_DRAGON_ATTACK = registerSoundEvent("entity.azure_dragon.attack");
    public static final SoundEvent ENTITY_AZURE_DRAGON_SPECIAL = registerSoundEvent("entity.azure_dragon.special");

    // qilin_guardian
    public static final SoundEvent ENTITY_QILIN_GUARDIAN_AMBIENT = registerSoundEvent("entity.qilin_guardian.ambient");
    public static final SoundEvent ENTITY_QILIN_GUARDIAN_HURT = registerSoundEvent("entity.qilin_guardian.hurt");
    public static final SoundEvent ENTITY_QILIN_GUARDIAN_DEATH = registerSoundEvent("entity.qilin_guardian.death");
    public static final SoundEvent ENTITY_QILIN_GUARDIAN_ATTACK = registerSoundEvent("entity.qilin_guardian.attack");
    public static final SoundEvent ENTITY_QILIN_GUARDIAN_SPECIAL = registerSoundEvent("entity.qilin_guardian.special");

    // jiangshi
    public static final SoundEvent ENTITY_JIANGSHI_AMBIENT = registerSoundEvent("entity.jiangshi.ambient");
    public static final SoundEvent ENTITY_JIANGSHI_HURT = registerSoundEvent("entity.jiangshi.hurt");
    public static final SoundEvent ENTITY_JIANGSHI_DEATH = registerSoundEvent("entity.jiangshi.death");
    public static final SoundEvent ENTITY_JIANGSHI_ATTACK = registerSoundEvent("entity.jiangshi.attack");
    public static final SoundEvent ENTITY_JIANGSHI_SPECIAL = registerSoundEvent("entity.jiangshi.special");

    // nine_tailed_fox
    public static final SoundEvent ENTITY_NINE_TAILED_FOX_AMBIENT = registerSoundEvent("entity.nine_tailed_fox.ambient");
    public static final SoundEvent ENTITY_NINE_TAILED_FOX_HURT = registerSoundEvent("entity.nine_tailed_fox.hurt");
    public static final SoundEvent ENTITY_NINE_TAILED_FOX_DEATH = registerSoundEvent("entity.nine_tailed_fox.death");
    public static final SoundEvent ENTITY_NINE_TAILED_FOX_ATTACK = registerSoundEvent("entity.nine_tailed_fox.attack");
    public static final SoundEvent ENTITY_NINE_TAILED_FOX_SPECIAL = registerSoundEvent("entity.nine_tailed_fox.special");

    // stone_lion_guardian
    public static final SoundEvent ENTITY_STONE_LION_GUARDIAN_AMBIENT = registerSoundEvent("entity.stone_lion_guardian.ambient");
    public static final SoundEvent ENTITY_STONE_LION_GUARDIAN_HURT = registerSoundEvent("entity.stone_lion_guardian.hurt");
    public static final SoundEvent ENTITY_STONE_LION_GUARDIAN_DEATH = registerSoundEvent("entity.stone_lion_guardian.death");
    public static final SoundEvent ENTITY_STONE_LION_GUARDIAN_ATTACK = registerSoundEvent("entity.stone_lion_guardian.attack");
    public static final SoundEvent ENTITY_STONE_LION_GUARDIAN_SPECIAL = registerSoundEvent("entity.stone_lion_guardian.special");

    // crystal_leviathan
    public static final SoundEvent ENTITY_CRYSTAL_LEVIATHAN_AMBIENT = registerSoundEvent("entity.crystal_leviathan.ambient");
    public static final SoundEvent ENTITY_CRYSTAL_LEVIATHAN_HURT = registerSoundEvent("entity.crystal_leviathan.hurt");
    public static final SoundEvent ENTITY_CRYSTAL_LEVIATHAN_DEATH = registerSoundEvent("entity.crystal_leviathan.death");
    public static final SoundEvent ENTITY_CRYSTAL_LEVIATHAN_ATTACK = registerSoundEvent("entity.crystal_leviathan.attack");
    public static final SoundEvent ENTITY_CRYSTAL_LEVIATHAN_SPECIAL = registerSoundEvent("entity.crystal_leviathan.special");

    // orichalcum_golem
    public static final SoundEvent ENTITY_ORICHALCUM_GOLEM_AMBIENT = registerSoundEvent("entity.orichalcum_golem.ambient");
    public static final SoundEvent ENTITY_ORICHALCUM_GOLEM_HURT = registerSoundEvent("entity.orichalcum_golem.hurt");
    public static final SoundEvent ENTITY_ORICHALCUM_GOLEM_DEATH = registerSoundEvent("entity.orichalcum_golem.death");
    public static final SoundEvent ENTITY_ORICHALCUM_GOLEM_ATTACK = registerSoundEvent("entity.orichalcum_golem.attack");
    public static final SoundEvent ENTITY_ORICHALCUM_GOLEM_SPECIAL = registerSoundEvent("entity.orichalcum_golem.special");

    // siren_priestess
    public static final SoundEvent ENTITY_SIREN_PRIESTESS_AMBIENT = registerSoundEvent("entity.siren_priestess.ambient");
    public static final SoundEvent ENTITY_SIREN_PRIESTESS_HURT = registerSoundEvent("entity.siren_priestess.hurt");
    public static final SoundEvent ENTITY_SIREN_PRIESTESS_DEATH = registerSoundEvent("entity.siren_priestess.death");
    public static final SoundEvent ENTITY_SIREN_PRIESTESS_ATTACK = registerSoundEvent("entity.siren_priestess.attack");
    public static final SoundEvent ENTITY_SIREN_PRIESTESS_SPECIAL = registerSoundEvent("entity.siren_priestess.special");

    // tidal_guardian
    public static final SoundEvent ENTITY_TIDAL_GUARDIAN_AMBIENT = registerSoundEvent("entity.tidal_guardian.ambient");
    public static final SoundEvent ENTITY_TIDAL_GUARDIAN_HURT = registerSoundEvent("entity.tidal_guardian.hurt");
    public static final SoundEvent ENTITY_TIDAL_GUARDIAN_DEATH = registerSoundEvent("entity.tidal_guardian.death");
    public static final SoundEvent ENTITY_TIDAL_GUARDIAN_ATTACK = registerSoundEvent("entity.tidal_guardian.attack");
    public static final SoundEvent ENTITY_TIDAL_GUARDIAN_SPECIAL = registerSoundEvent("entity.tidal_guardian.special");

    // abyssal_ray
    public static final SoundEvent ENTITY_ABYSSAL_RAY_AMBIENT = registerSoundEvent("entity.abyssal_ray.ambient");
    public static final SoundEvent ENTITY_ABYSSAL_RAY_HURT = registerSoundEvent("entity.abyssal_ray.hurt");
    public static final SoundEvent ENTITY_ABYSSAL_RAY_DEATH = registerSoundEvent("entity.abyssal_ray.death");
    public static final SoundEvent ENTITY_ABYSSAL_RAY_ATTACK = registerSoundEvent("entity.abyssal_ray.attack");
    public static final SoundEvent ENTITY_ABYSSAL_RAY_SPECIAL = registerSoundEvent("entity.abyssal_ray.special");

    // Meteor Sounds
    public static final SoundEvent ENTITY_METEOR_FALLING = registerSoundEvent("entity.meteor.falling");
    public static final SoundEvent ENTITY_METEOR_WARNING = registerSoundEvent("entity.meteor.warning");
    public static final SoundEvent ENTITY_METEOR_IMPACT = registerSoundEvent("entity.meteor.impact");


    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(MythicAgeMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerModSounds() {
        MythicAgeMod.LOGGER.info("Registering Mod Sounds for " + MythicAgeMod.MOD_ID);
        // This method is called to ensure the static initializer runs.
    }
}
