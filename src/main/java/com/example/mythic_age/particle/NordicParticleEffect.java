package com.example.mythic_age.particle;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class NordicParticleEffect implements SpellParticleEffect {

    public static final NordicParticleEffect INSTANCE = new NordicParticleEffect();

    @Override
    public void playCast(ServerWorld world, BlockPos casterPos, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.NORDIC_RUNE_GLOW,
            casterPos.getX() + 0.5, casterPos.getY() + 1.0, casterPos.getZ() + 0.5,
            10, 0.8, 0.8, 0.8, 0.05);
        MythicParticleEffects.spawnNormal(world, ModParticles.NORDIC_ICE_SHARD,
            casterPos.getX() + 0.5, casterPos.getY() + 1.5, casterPos.getZ() + 0.5,
            8, 0.5, 1.0, 0.5, 0.1);
    }

    @Override
    public void playActive(ServerWorld world, BlockPos targetPos, int ticksElapsed) {
        if (ticksElapsed % 6 == 0) {
            MythicParticleEffects.spawnAmbient(world, ModParticles.NORDIC_ICE_SHARD,
                targetPos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 3,
                targetPos.getY() + 1.0 + world.random.nextDouble() * 2,
                targetPos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 3,
                2, 0.1, -0.05, 0.1, 0.02);
        }
    }

    @Override
    public void playEnd(ServerWorld world, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.NORDIC_RUNE_GLOW,
            targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
            18, 2.0, 0.5, 2.0, 0.12);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.SNOWFLAKE,
            targetPos.getX() + 0.5, targetPos.getY() + 2.0, targetPos.getZ() + 0.5,
            15, 2.0, 1.0, 2.0, 0.05);
    }
}
