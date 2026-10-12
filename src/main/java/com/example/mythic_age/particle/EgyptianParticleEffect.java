package com.example.mythic_age.particle;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class EgyptianParticleEffect implements SpellParticleEffect {

    public static final EgyptianParticleEffect INSTANCE = new EgyptianParticleEffect();

    @Override
    public void playCast(ServerWorld world, BlockPos casterPos, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.EGYPTIAN_SUN_RAY,
            casterPos.getX() + 0.5, casterPos.getY() + 2.0, casterPos.getZ() + 0.5,
            12, 0.3, 1.5, 0.3, 0.08);
        MythicParticleEffects.spawnNormal(world, ModParticles.EGYPTIAN_SAND_SWIRL,
            casterPos.getX() + 0.5, casterPos.getY() + 0.5, casterPos.getZ() + 0.5,
            8, 1.0, 0.2, 1.0, 0.05);
    }

    @Override
    public void playActive(ServerWorld world, BlockPos targetPos, int ticksElapsed) {
        if (ticksElapsed % 4 == 0) {
            double angle = (ticksElapsed * 0.15) % (2 * Math.PI);
            double radius = 1.5;
            MythicParticleEffects.spawnAmbient(world, ModParticles.EGYPTIAN_SAND_SWIRL,
                targetPos.getX() + 0.5 + Math.cos(angle) * radius,
                targetPos.getY() + 0.5,
                targetPos.getZ() + 0.5 + Math.sin(angle) * radius,
                2, 0.1, 0.3, 0.1, 0.01);
        }
    }

    @Override
    public void playEnd(ServerWorld world, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.EGYPTIAN_SUN_RAY,
            targetPos.getX() + 0.5, targetPos.getY() + 1.5, targetPos.getZ() + 0.5,
            25, 2.0, 1.5, 2.0, 0.1);
    }
}
