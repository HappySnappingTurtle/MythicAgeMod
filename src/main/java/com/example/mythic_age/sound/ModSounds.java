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
