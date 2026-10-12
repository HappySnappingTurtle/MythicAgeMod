package com.example.mythic_age.world;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.util.EarthquakeManager;
import com.example.mythic_age.util.GodPowerWeatherManager;
import com.example.mythic_age.util.LightningStormManager;
import com.example.mythic_age.system.ChineseCertificateManager;
import com.example.mythic_age.system.GodPowerCertificateManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

/**
 * World-level state for long-running Mythic Age systems that must survive restarts.
 */
public class MythicAgeWorldState extends PersistentState {
    private static final String STORAGE_KEY = MythicAgeMod.MOD_ID + "_world_state";
    private static final int VERSION = 1;

    private final LightningStormManager lightningStormManager = LightningStormManager.getInstance();
    private final EarthquakeManager earthquakeManager = EarthquakeManager.getInstance();
    private final GodPowerWeatherManager godPowerWeatherManager = GodPowerWeatherManager.getInstance();
    private final GodPowerCertificateManager godPowerCertificateManager = GodPowerCertificateManager.getInstance();
    private final ChineseCertificateManager chineseCertificateManager = ChineseCertificateManager.getInstance();

    public MythicAgeWorldState() {
        lightningStormManager.readPersistentState(new NbtCompound());
        earthquakeManager.readPersistentState(new NbtCompound());
        godPowerWeatherManager.readPersistentState(new NbtCompound());
        godPowerCertificateManager.readPersistentState(new NbtCompound());
        chineseCertificateManager.readPersistentState(new NbtCompound());
    }

    public static MythicAgeWorldState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Cannot access overworld persistent state for Mythic Age");
        }
        return overworld.getPersistentStateManager().getOrCreate(
                MythicAgeWorldState::fromNbt,
                MythicAgeWorldState::new,
                STORAGE_KEY
        );
    }

    public static MythicAgeWorldState fromNbt(NbtCompound nbt) {
        MythicAgeWorldState state = new MythicAgeWorldState();
        state.lightningStormManager.readPersistentState(nbt.getCompound("LightningStorms"));
        state.earthquakeManager.readPersistentState(nbt.getCompound("Earthquakes"));
        state.godPowerWeatherManager.readPersistentState(nbt.getCompound("GodPowerWeather"));
        state.godPowerCertificateManager.readPersistentState(nbt.getCompound("GodPowerCertificates"));
        state.chineseCertificateManager.readPersistentState(nbt.getCompound("ChineseCertificates"));
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("Version", VERSION);
        nbt.put("LightningStorms", lightningStormManager.writePersistentState());
        nbt.put("Earthquakes", earthquakeManager.writePersistentState());
        nbt.put("GodPowerWeather", godPowerWeatherManager.writePersistentState());
        nbt.put("GodPowerCertificates", godPowerCertificateManager.writePersistentState());
        nbt.put("ChineseCertificates", chineseCertificateManager.writePersistentState());
        return nbt;
    }

    public void tick(ServerWorld world) {
        boolean dirty = false;
        dirty |= earthquakeManager.tick(world);
        dirty |= lightningStormManager.tick(world);
        dirty |= godPowerWeatherManager.tick(world);
        if (dirty) {
            markDirty();
        }
    }
}
