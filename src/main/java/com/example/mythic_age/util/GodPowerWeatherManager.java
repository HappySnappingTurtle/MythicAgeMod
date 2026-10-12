package com.example.mythic_age.util;

import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.world.MythicAgeWorldState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Persistent manager for long-running god powers that primarily control weather.
 */
public class GodPowerWeatherManager {
    private static GodPowerWeatherManager instance;

    private final Map<String, WeatherPowerData> activeWeatherPowers = new HashMap<>();

    private GodPowerWeatherManager() {}

    public static GodPowerWeatherManager getInstance() {
        if (instance == null) {
            instance = new GodPowerWeatherManager();
        }
        return instance;
    }

    public void startFimbulwinter(World world, BlockPos center, String id) {
        startWeatherPower(world, id, WeatherPowerType.FIMBULWINTER, center, 20, 12000, true);
    }

    public void startOceanicWrath(World world, BlockPos center, String id) {
        startWeatherPower(world, id, WeatherPowerType.OCEANIC_WRATH, center, 20, 3600, true);
    }

    private void startWeatherPower(World world, String id, WeatherPowerType type, BlockPos center, int radius, int durationTicks, boolean thunder) {
        MythicAgeWorldState worldState = null;
        if (world instanceof ServerWorld serverWorld) {
            worldState = MythicAgeWorldState.get(serverWorld.getServer());
        }
        activeWeatherPowers.put(
                id,
                new WeatherPowerData(type, world.getRegistryKey().getValue().toString(), center, radius, durationTicks, 0)
        );
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.setWeather(0, durationTicks, true, thunder);
            if (worldState != null) {
                worldState.markDirty();
            }
        }
    }

    public boolean tick(ServerWorld world) {
        boolean dirty = false;
        String dimension = world.getRegistryKey().getValue().toString();
        Iterator<Map.Entry<String, WeatherPowerData>> iterator = activeWeatherPowers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WeatherPowerData> entry = iterator.next();
            WeatherPowerData data = entry.getValue();
            if (!data.dimension.equals(dimension)) {
                continue;
            }

            data.ticksAlive++;
            if (data.ticksAlive >= data.durationTicks) {
                finishWeatherPower(world, data);
                iterator.remove();
                dirty = true;
                continue;
            }

            if (data.ticksAlive % data.type.particleIntervalTicks == 0) {
                spawnAmbientWeatherParticles(world, data);
            }
            dirty = true;
        }
        return dirty;
    }

    public NbtCompound writePersistentState() {
        NbtCompound root = new NbtCompound();
        NbtList powers = new NbtList();
        for (Map.Entry<String, WeatherPowerData> entry : activeWeatherPowers.entrySet()) {
            WeatherPowerData data = entry.getValue();
            NbtCompound power = new NbtCompound();
            power.putString("Id", entry.getKey());
            power.putString("Type", data.type.name());
            power.putString("Dimension", data.dimension);
            power.putLong("Center", data.center.asLong());
            power.putInt("Radius", data.radius);
            power.putInt("DurationTicks", data.durationTicks);
            power.putInt("TicksAlive", data.ticksAlive);
            power.putInt("RemainingTicks", Math.max(0, data.durationTicks - data.ticksAlive));
            powers.add(power);
        }
        root.put("ActiveWeatherPowers", powers);
        return root;
    }

    public void readPersistentState(NbtCompound nbt) {
        activeWeatherPowers.clear();
        NbtList powers = nbt.getList("ActiveWeatherPowers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < powers.size(); i++) {
            NbtCompound power = powers.getCompound(i);
            String id = power.getString("Id");
            String dimension = power.getString("Dimension");
            WeatherPowerType type = WeatherPowerType.fromName(power.getString("Type"));
            if (id.isEmpty() || dimension.isEmpty() || type == null || !power.contains("Center", NbtElement.LONG_TYPE)) {
                continue;
            }
            int durationTicks = power.getInt("DurationTicks");
            if (durationTicks <= 0) {
                durationTicks = type.defaultDurationTicks;
            }
            int ticksAlive = power.contains("TicksAlive", NbtElement.INT_TYPE)
                    ? power.getInt("TicksAlive")
                    : Math.max(0, durationTicks - power.getInt("RemainingTicks"));
            if (ticksAlive >= durationTicks) {
                continue;
            }
            int radius = power.contains("Radius", NbtElement.INT_TYPE) ? power.getInt("Radius") : type.defaultRadius;
            activeWeatherPowers.put(
                    id,
                    new WeatherPowerData(type, dimension, BlockPos.fromLong(power.getLong("Center")), radius, durationTicks, ticksAlive)
            );
        }
    }

    private void spawnAmbientWeatherParticles(ServerWorld world, WeatherPowerData data) {
        if (data.type == WeatherPowerType.FIMBULWINTER) {
            MythicParticleEffects.spawnMajor(world, ParticleTypes.SNOWFLAKE,
                    data.center.getX() + 0.5, data.center.getY() + 4.0, data.center.getZ() + 0.5,
                    28, data.radius, 4.0, data.radius, 0.06);
        } else if (data.type == WeatherPowerType.OCEANIC_WRATH) {
            MythicParticleEffects.spawnMajor(world, ParticleTypes.SPLASH,
                    data.center.getX() + 0.5, data.center.getY() + 2.0, data.center.getZ() + 0.5,
                    34, data.radius, 3.0, data.radius, 0.12);
        }
    }

    private void finishWeatherPower(ServerWorld world, WeatherPowerData data) {
        world.setWeather(6000, 0, false, false);
        if (data.type == WeatherPowerType.FIMBULWINTER) {
            MythicParticleEffects.spawnMajor(world, ParticleTypes.CLOUD,
                    data.center.getX() + 0.5, data.center.getY() + 3.0, data.center.getZ() + 0.5,
                    40, data.radius, 3.0, data.radius, 0.08);
            world.playSound(null, data.center, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.WEATHER, 1.2f, 0.7f);
        } else if (data.type == WeatherPowerType.OCEANIC_WRATH) {
            MythicParticleEffects.spawnMajor(world, ParticleTypes.CLOUD,
                    data.center.getX() + 0.5, data.center.getY() + 2.5, data.center.getZ() + 0.5,
                    36, data.radius, 2.5, data.radius, 0.08);
            world.playSound(null, data.center, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 1.0f, 0.6f);
        }
    }

    private enum WeatherPowerType {
        FIMBULWINTER(20, 12000, 80),
        OCEANIC_WRATH(20, 3600, 60);

        final int defaultRadius;
        final int defaultDurationTicks;
        final int particleIntervalTicks;

        WeatherPowerType(int defaultRadius, int defaultDurationTicks, int particleIntervalTicks) {
            this.defaultRadius = defaultRadius;
            this.defaultDurationTicks = defaultDurationTicks;
            this.particleIntervalTicks = particleIntervalTicks;
        }

        static WeatherPowerType fromName(String name) {
            for (WeatherPowerType value : values()) {
                if (value.name().equals(name)) {
                    return value;
                }
            }
            return null;
        }
    }

    private static class WeatherPowerData {
        final WeatherPowerType type;
        final String dimension;
        final BlockPos center;
        final int radius;
        final int durationTicks;
        int ticksAlive;

        WeatherPowerData(WeatherPowerType type, String dimension, BlockPos center, int radius, int durationTicks, int ticksAlive) {
            this.type = type;
            this.dimension = dimension;
            this.center = center;
            this.radius = radius;
            this.durationTicks = durationTicks;
            this.ticksAlive = ticksAlive;
        }
    }
}
