package com.example.mythic_age.particle;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class GreekParticleEffect implements SpellParticleEffect {

    public static final GreekParticleEffect INSTANCE = new GreekParticleEffect();

    @Override
    public void playCast(ServerWorld world, BlockPos casterPos, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.GREEK_DIVINE_LIGHT,
            casterPos.getX() + 0.5, casterPos.getY() + 1.5, casterPos.getZ() + 0.5,
            15, 0.5, 1.0, 0.5, 0.1);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.ENCHANT,
            casterPos.getX() + 0.5, casterPos.getY() + 2.0, casterPos.getZ() + 0.5,
            10, 0.3, 0.5, 0.3, 0.05);
    }

    @Override
    public void playActive(ServerWorld world, BlockPos targetPos, int ticksElapsed) {
        if (ticksElapsed % 5 == 0) {
            MythicParticleEffects.spawnAmbient(world, ModParticles.GREEK_LIGHTNING_SPARK,
                targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
                3, 1.0, 0.5, 1.0, 0.02);
        }
    }

    @Override
    public void playEnd(ServerWorld world, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.GREEK_DIVINE_LIGHT,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            20, 1.5, 1.0, 1.5, 0.15);
    }
}
