package com.example.mythic_age.particle;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ChineseParticleEffect implements SpellParticleEffect {

    public static final ChineseParticleEffect INSTANCE = new ChineseParticleEffect();

    @Override
    public void playCast(ServerWorld world, BlockPos casterPos, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.CHINESE_CLOUD_WISP,
            casterPos.getX() + 0.5, casterPos.getY() + 1.5, casterPos.getZ() + 0.5,
            12, 0.6, 1.0, 0.6, 0.04);
        MythicParticleEffects.spawnNormal(world, ModParticles.CHINESE_DRAGON_TRAIL,
            casterPos.getX() + 0.5, casterPos.getY() + 1.0, casterPos.getZ() + 0.5,
            6, 0.3, 0.5, 0.3, 0.08);
    }

    @Override
    public void playActive(ServerWorld world, BlockPos targetPos, int ticksElapsed) {
        if (ticksElapsed % 5 == 0) {
            double angle = (ticksElapsed * 0.2) % (2 * Math.PI);
            double radius = 1.0 + Math.sin(ticksElapsed * 0.1) * 0.5;
            MythicParticleEffects.spawnAmbient(world, ModParticles.CHINESE_CLOUD_WISP,
                targetPos.getX() + 0.5 + Math.cos(angle) * radius,
                targetPos.getY() + 1.0 + Math.sin(ticksElapsed * 0.05) * 0.5,
                targetPos.getZ() + 0.5 + Math.sin(angle) * radius,
                2, 0.1, 0.2, 0.1, 0.01);
        }
    }

    @Override
    public void playEnd(ServerWorld world, BlockPos targetPos) {
        MythicParticleEffects.spawnMajor(world, ModParticles.CHINESE_DRAGON_TRAIL,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            20, 1.5, 1.5, 1.5, 0.1);
        MythicParticleEffects.spawnNormal(world, ModParticles.CHINESE_CLOUD_WISP,
            targetPos.getX() + 0.5, targetPos.getY() + 2.0, targetPos.getZ() + 0.5,
            10, 2.0, 0.5, 2.0, 0.03);
    }
}
