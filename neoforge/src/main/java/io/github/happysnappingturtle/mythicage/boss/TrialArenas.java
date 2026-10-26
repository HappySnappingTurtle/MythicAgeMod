package io.github.happysnappingturtle.mythicage.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 全部试炼竞技场实例（T23 §4.1）：世界存档数据 {@value #NAME}，按实例 id 存。只有非 IDLE 的实例每刻推进（通常 0–1 个）。
 * 实例 id = 维度 | Boss 出生点坐标（同一处结构固定不变）。
 */
public final class TrialArenas extends SavedData {
    public static final String NAME = "mythic_age_trial_arenas";

    private final Map<String, TrialArena> arenas = new LinkedHashMap<>();
    private static TrialArenas current;

    public static TrialArenas get(MinecraftServer server) {
        current = server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TrialArenas::new, TrialArenas::read, null), NAME);
        return current;
    }

    /** 实例状态变了：下次存档写盘。 */
    static void markDirty() {
        if (current != null) {
            current.setDirty();
        }
    }

    public static String idFor(ServerLevel level, BlockPos bossOrigin) {
        return level.dimension().location() + "|" + bossOrigin.getX() + "," + bossOrigin.getY() + "," + bossOrigin.getZ();
    }

    public void add(TrialArena a) {
        if (arenas.putIfAbsent(a.id(), a) == null) {
            setDirty();
        }
    }

    public Optional<TrialArena> byId(String id) {
        return Optional.ofNullable(arenas.get(id));
    }

    public Collection<TrialArena> all() {
        return arenas.values();
    }

    /** 这一格受哪个竞技场保护。 */
    public Optional<TrialArena> protecting(ServerLevel level, BlockPos p) {
        for (TrialArena a : arenas.values()) {
            if (a.dimension().equals(level.dimension()) && a.protects(p)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public Optional<TrialArena> byGate(ServerLevel level, BlockPos p) {
        for (TrialArena a : arenas.values()) {
            if (a.dimension().equals(level.dimension()) && a.isGate(p)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public Optional<TrialArena> hallOf(Entity e) {
        for (TrialArena a : arenas.values()) {
            if (a.inHall(e)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    /** 实体所在的迷宫范围（回程线用）。 */
    public Optional<TrialArena> regionOf(Entity e) {
        BlockPos p = e.blockPosition();
        for (TrialArena a : arenas.values()) {
            if (a.dimension().equals(e.level().dimension()) && a.region().isInside(p)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public void tick(MinecraftServer server) {
        for (TrialArena a : List.copyOf(arenas.values())) {
            if (a.phase() == io.github.happysnappingturtle.mythicage.core.boss.ArenaPhase.IDLE) {
                continue;
            }
            ServerLevel level = server.getLevel(a.dimension());
            if (level != null && level.isLoaded(a.bossOrigin())) {
                a.tick(level);
            }
        }
    }

    static TrialArenas read(CompoundTag tag, HolderLookup.Provider registries) {
        TrialArenas s = new TrialArenas();
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag t : tag.getList("arenas", Tag.TAG_COMPOUND)) {
            TrialArena.load((CompoundTag) t, blocks).ifPresent(a -> s.arenas.put(a.id(), a));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag l = new ListTag();
        arenas.values().forEach(a -> l.add(a.save()));
        tag.put("arenas", l);
        return tag;
    }

    /** 只供 GameTest。 */
    public void forgetForTest(String id) {
        if (arenas.remove(id) != null) {
            setDirty();
        }
    }

    /** 只供 GameTest：把一个实例存档往返（模拟重启；只动这一个，同批并行的其他用例不受影响）。 */
    public void roundTripForTest(HolderLookup.Provider registries, String id) {
        TrialArena a = arenas.get(id);
        if (a != null) {
            TrialArena.load(a.save(), registries.lookupOrThrow(Registries.BLOCK)).ifPresent(copy -> arenas.put(id, copy));
        }
    }

    public static List<TrialArena> activeForTest(MinecraftServer server) {
        List<TrialArena> out = new ArrayList<>();
        get(server).all().forEach(out::add);
        return out;
    }
}
