package com.example.mythic_age.particle;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Three-phase particle effect interface for spells.
 * Each spell can bind a SpellParticleEffect to provide civilization-themed visuals.
 */
public interface SpellParticleEffect {

    void playCast(ServerWorld world, BlockPos casterPos, BlockPos targetPos);

    void playActive(ServerWorld world, BlockPos targetPos, int ticksElapsed);

    void playEnd(ServerWorld world, BlockPos targetPos);
}
