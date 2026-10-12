package com.example.mythic_age.client.particle;

import com.example.mythic_age.particle.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public final class ModClientParticles {
    private ModClientParticles() {
    }

    public static void registerClientParticles() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(ModParticles.MYTHIC_FLAME, MythicFlameParticle.Factory::new);
        registry.register(ModParticles.DIVINE_SPARKLE, DivineSparkleParticle.Factory::new);
        registry.register(ModParticles.SHOCKWAVE, ShockwaveParticle.Factory::new);

        // Civilization-themed particles (reuse base factories; custom renderers in future pass)
        registry.register(ModParticles.GREEK_DIVINE_LIGHT, DivineSparkleParticle.Factory::new);
        registry.register(ModParticles.GREEK_LIGHTNING_SPARK, MythicFlameParticle.Factory::new);
        registry.register(ModParticles.EGYPTIAN_SAND_SWIRL, MythicFlameParticle.Factory::new);
        registry.register(ModParticles.EGYPTIAN_SUN_RAY, DivineSparkleParticle.Factory::new);
        registry.register(ModParticles.NORDIC_ICE_SHARD, DivineSparkleParticle.Factory::new);
        registry.register(ModParticles.NORDIC_RUNE_GLOW, DivineSparkleParticle.Factory::new);
        registry.register(ModParticles.CHINESE_CLOUD_WISP, ShockwaveParticle.Factory::new);
        registry.register(ModParticles.CHINESE_DRAGON_TRAIL, MythicFlameParticle.Factory::new);
        registry.register(ModParticles.ATLANTEAN_BUBBLE, DivineSparkleParticle.Factory::new);
        registry.register(ModParticles.ATLANTEAN_PRISM, DivineSparkleParticle.Factory::new);
    }
}
