package io.github.happysnappingturtle.mythicage.playtest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 快速验证的世界数据（GA-3 设计文档 §4.3，SavedData {@code mythic_age_playtest}，存在主世界）：
 * 固定下来的试玩村庄、迷宫入口、神庙位置，每名玩家第一次进站前的神话数据快照，以及做完的站。
 */
public final class PlaytestState extends SavedData {
    private static final String NAME = "mythic_age_playtest";

    @Nullable
    BlockPos villageCenter;
    @Nullable
    BlockPos labyrinthEntrance;
    @Nullable
    BlockPos templeSite;
    private final Map<UUID, CompoundTag> snapshots = new HashMap<>();
    private final Map<UUID, Set<String>> done = new HashMap<>();

    public static PlaytestState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PlaytestState::new, PlaytestState::read, null), NAME);
    }

    void setVillage(BlockPos center) {
        villageCenter = center.immutable();
        templeSite = null;
        setDirty();
    }

    void setLabyrinth(BlockPos entrance) {
        labyrinthEntrance = entrance.immutable();
        setDirty();
    }

    void setTempleSite(BlockPos site) {
        templeSite = site.immutable();
        setDirty();
    }

    void resetAnchors() {
        villageCenter = null;
        labyrinthEntrance = null;
        templeSite = null;
        setDirty();
    }

    /** 只在第一次进站时存；返回是否新存。 */
    boolean snapshotIfAbsent(UUID player, CompoundTag data) {
        if (snapshots.containsKey(player)) {
            return false;
        }
        snapshots.put(player, data.copy());
        setDirty();
        return true;
    }

    Optional<CompoundTag> takeSnapshot(UUID player) {
        CompoundTag t = snapshots.remove(player);
        if (t != null) {
            setDirty();
        }
        return Optional.ofNullable(t);
    }

    /** 只供 GameTest 清场。 */
    public void takeSnapshotForTest(UUID player) {
        takeSnapshot(player);
    }

    public boolean hasSnapshot(UUID player) {
        return snapshots.containsKey(player);
    }

    void markDone(UUID player, String station) {
        if (done.computeIfAbsent(player, k -> new HashSet<>()).add(station)) {
            setDirty();
        }
    }

    Set<String> done(UUID player) {
        return Set.copyOf(done.getOrDefault(player, Set.of()));
    }

    public static PlaytestState read(CompoundTag tag, HolderLookup.Provider registries) {
        PlaytestState s = new PlaytestState();
        s.villageCenter = NbtUtils.readBlockPos(tag, "village").orElse(null);
        s.labyrinthEntrance = NbtUtils.readBlockPos(tag, "labyrinth").orElse(null);
        s.templeSite = NbtUtils.readBlockPos(tag, "temple").orElse(null);
        for (Tag t : tag.getList("players", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            UUID id = c.getUUID("player");
            if (c.contains("snapshot", Tag.TAG_COMPOUND)) {
                s.snapshots.put(id, c.getCompound("snapshot"));
            }
            Set<String> d = new HashSet<>();
            for (Tag x : c.getList("done", Tag.TAG_STRING)) {
                d.add(x.getAsString());
            }
            if (!d.isEmpty()) {
                s.done.put(id, d);
            }
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (villageCenter != null) {
            tag.put("village", NbtUtils.writeBlockPos(villageCenter));
        }
        if (labyrinthEntrance != null) {
            tag.put("labyrinth", NbtUtils.writeBlockPos(labyrinthEntrance));
        }
        if (templeSite != null) {
            tag.put("temple", NbtUtils.writeBlockPos(templeSite));
        }
        Set<UUID> ids = new HashSet<>(snapshots.keySet());
        ids.addAll(done.keySet());
        ListTag players = new ListTag();
        for (UUID id : ids) {
            CompoundTag c = new CompoundTag();
            c.putUUID("player", id);
            if (snapshots.containsKey(id)) {
                c.put("snapshot", snapshots.get(id));
            }
            ListTag d = new ListTag();
            done.getOrDefault(id, Set.of()).stream().sorted().forEach(x -> d.add(StringTag.valueOf(x)));
            c.put("done", d);
            players.add(c);
        }
        tag.put("players", players);
        return tag;
    }
}
