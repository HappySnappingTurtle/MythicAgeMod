package io.github.happysnappingturtle.mythicage.village;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家进过的文明村庄（T13b 设计文档 §7，SavedData {@code mythic_age_village_visits}）：
 * 玩家 → 维度 → 村庄结构起点区块（{@code ChunkPos.toLong}）。进村标题弹出时记一次；村庄指引跳过这些村庄。
 */
public final class VillageVisits extends SavedData {
    private static final String NAME = "mythic_age_village_visits";
    private final Map<UUID, Map<String, Set<Long>>> visits = new HashMap<>();

    public static VillageVisits get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillageVisits::new, VillageVisits::read, null), NAME);
    }

    /** 记下玩家进了这座村庄（只认有规划数据的文明村庄）；新记下时返回 true。 */
    public static boolean mark(ServerPlayer player, VillageRegistry.Village v) {
        return VillagePlans.get(player.server).byVillage(v.id())
                .map(p -> get(player.server).add(player.getUUID(), p.dim(), p.start()))
                .orElse(false);
    }

    public boolean add(UUID player, ResourceKey<Level> dim, long start) {
        boolean added = visits.computeIfAbsent(player, k -> new HashMap<>())
                .computeIfAbsent(dim.location().toString(), k -> new HashSet<>()).add(start);
        if (added) {
            setDirty();
        }
        return added;
    }

    public Set<Long> visited(UUID player, ResourceKey<Level> dim) {
        return Set.copyOf(visits.getOrDefault(player, Map.of()).getOrDefault(dim.location().toString(), Set.of()));
    }

    /** 只供 GameTest 清场。 */
    public void forget(UUID player) {
        if (visits.remove(player) != null) {
            setDirty();
        }
    }

    public static VillageVisits read(CompoundTag tag, HolderLookup.Provider registries) {
        VillageVisits r = new VillageVisits();
        for (Tag t : tag.getList("players", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            UUID id = c.getUUID("player");
            CompoundTag dims = c.getCompound("dims");
            for (String d : dims.getAllKeys()) {
                Set<Long> set = r.visits.computeIfAbsent(id, k -> new HashMap<>()).computeIfAbsent(d, k -> new HashSet<>());
                for (long l : dims.getLongArray(d)) {
                    set.add(l);
                }
            }
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        visits.forEach((id, dims) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("player", id);
            CompoundTag d = new CompoundTag();
            dims.forEach((k, v) -> d.put(k, new LongArrayTag(v.stream().mapToLong(Long::longValue).toArray())));
            c.put("dims", d);
            list.add(c);
        });
        tag.put("players", list);
        return tag;
    }
}
