package com.example.mythic_age.particle;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class AtlanteanParticleEffect implements SpellParticleEffect {

    public static final AtlanteanParticleEffect INSTANCE = new AtlanteanParticleEffect();

    @Override
    public void playCast(ServerWorld world, BlockPos casterPos, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.ATLANTEAN_PRISM,
            casterPos.getX() + 0.5, casterPos.getY() + 1.5, casterPos.getZ() + 0.5,
            10, 0.4, 0.8, 0.4, 0.06);
        MythicParticleEffects.spawnNormal(world, ModParticles.ATLANTEAN_BUBBLE,
            casterPos.getX() + 0.5, casterPos.getY() + 0.5, casterPos.getZ() + 0.5,
            12, 0.8, 0.3, 0.8, 0.03);
    }

    @Override
    public void playActive(ServerWorld world, BlockPos targetPos, int ticksElapsed) {
        if (ticksElapsed % 4 == 0) {
            MythicParticleEffects.spawnAmbient(world, ModParticles.ATLANTEAN_BUBBLE,
                targetPos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 2,
                targetPos.getY() + world.random.nextDouble() * 1.5,
                targetPos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 2,
                2, 0.05, 0.1, 0.05, 0.02);
        }
        if (ticksElapsed % 10 == 0) {
            MythicParticleEffects.spawnAmbient(world, ModParticles.ATLANTEAN_PRISM,
                targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
                1, 0.5, 0.5, 0.5, 0.01);
        }
    }

    @Override
    public void playEnd(ServerWorld world, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.ATLANTEAN_PRISM,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            22, 2.0, 1.0, 2.0, 0.12);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.BUBBLE_POP,
            targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5,
            15, 1.5, 1.0, 1.5, 0.05);
    }
}
