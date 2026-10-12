package com.example.mythic_age.particle;

import com.example.mythic_age.config.ModConfig;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class MythicParticleEffects {
    public enum Intensity {
        AMBIENT(0.55D),
        NORMAL(1.0D),
        MAJOR(1.35D),
        ULTIMATE(1.65D);

        private final double multiplier;

        Intensity(double multiplier) {
            this.multiplier = multiplier;
        }
    }

    private MythicParticleEffects() {
    }

    public static <T extends ParticleEffect> int spawnAmbient(
            ServerWorld world,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
        return spawn(world, particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, Intensity.AMBIENT);
    }

    public static <T extends ParticleEffect> int spawnNormal(
            ServerWorld world,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
        return spawn(world, particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, Intensity.NORMAL);
    }

    public static <T extends ParticleEffect> int spawnMajor(
            ServerWorld world,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
        return spawn(world, particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, Intensity.MAJOR);
    }

    public static <T extends ParticleEffect> int spawnUltimate(
            ServerWorld world,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
        return spawn(world, particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, Intensity.ULTIMATE);
    }

    public static <T extends ParticleEffect> int spawn(
            ServerWorld world,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed,
            Intensity intensity
    ) {
        int scaledCount = scaleParticleCount(world, x, y, z, count, intensity);
        if (scaledCount <= 0) {
            return 0;
        }
        return world.spawnParticles(particle, x, y, z, scaledCount, offsetX, offsetY, offsetZ, speed);
    }

    public static int scaleParticleCount(
            ServerWorld world,
            double x,
            double y,
            double z,
            int requestedCount,
            Intensity intensity
    ) {
        if (!ModConfig.PARTICLES_ENABLED || requestedCount <= 0 || ModConfig.PARTICLE_DENSITY_MULTIPLIER <= 0.0D) {
            return 0;
        }

        double nearestDistanceSquared = nearestPlayerDistanceSquared(world, x, y, z);
        double maxDistance = ModConfig.PARTICLE_MAX_RENDER_DISTANCE;
        if (nearestDistanceSquared == Double.MAX_VALUE || nearestDistanceSquared > maxDistance * maxDistance) {
            return 0;
        }

        double distanceMultiplier = nearestDistanceSquared > ModConfig.PARTICLE_DISTANCE_THRESHOLD * ModConfig.PARTICLE_DISTANCE_THRESHOLD
                ? ModConfig.PARTICLE_FAR_DISTANCE_MULTIPLIER
                : 1.0D;
        double factor = ModConfig.PARTICLE_DENSITY_MULTIPLIER * distanceMultiplier * intensity.multiplier;
        if (requestedCount == 1 && factor < 1.0D && world.random.nextDouble() > factor) {
            return 0;
        }

        int scaledCount = (int) Math.ceil(requestedCount * factor);
        return Math.max(1, Math.min(ModConfig.MAX_PARTICLES_PER_BURST, scaledCount));
    }

    private static double nearestPlayerDistanceSquared(ServerWorld world, double x, double y, double z) {
        double nearest = Double.MAX_VALUE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            double dx = player.getX() - x;
            double dy = player.getY() - y;
            double dz = player.getZ() - z;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared < nearest) {
                nearest = distanceSquared;
            }
        }
        return nearest;
    }
}
