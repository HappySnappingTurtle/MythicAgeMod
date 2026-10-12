package com.example.mythic_age.particle;

import com.example.mythic_age.MythicAgeMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {
    public static final DefaultParticleType MYTHIC_FLAME = register("mythic_flame");
    public static final DefaultParticleType DIVINE_SPARKLE = register("divine_sparkle");
    public static final DefaultParticleType SHOCKWAVE = register("shockwave");

    // Civilization-themed particles
    public static final DefaultParticleType GREEK_DIVINE_LIGHT = register("greek_divine_light");
    public static final DefaultParticleType GREEK_LIGHTNING_SPARK = register("greek_lightning_spark");
    public static final DefaultParticleType EGYPTIAN_SAND_SWIRL = register("egyptian_sand_swirl");
    public static final DefaultParticleType EGYPTIAN_SUN_RAY = register("egyptian_sun_ray");
    public static final DefaultParticleType NORDIC_ICE_SHARD = register("nordic_ice_shard");
    public static final DefaultParticleType NORDIC_RUNE_GLOW = register("nordic_rune_glow");
    public static final DefaultParticleType CHINESE_CLOUD_WISP = register("chinese_cloud_wisp");
    public static final DefaultParticleType CHINESE_DRAGON_TRAIL = register("chinese_dragon_trail");
    public static final DefaultParticleType ATLANTEAN_BUBBLE = register("atlantean_bubble");
    public static final DefaultParticleType ATLANTEAN_PRISM = register("atlantean_prism");

    private ModParticles() {
    }

    public static void registerModParticles() {
        MythicAgeMod.LOGGER.info("Registering Mythic Age particles");
    }

    private static DefaultParticleType register(String name) {
        return Registry.register(
                Registries.PARTICLE_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, name),
                FabricParticleTypes.simple()
        );
    }
}
