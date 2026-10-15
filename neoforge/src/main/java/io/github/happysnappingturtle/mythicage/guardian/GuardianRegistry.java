package io.github.happysnappingturtle.mythicage.guardian;

import io.github.happysnappingturtle.mythicage.MythicAge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 守护者注册表（T9 设计文档 §7、§10）：guardianId → 居所、所属聚会钟、生死与当前实体。
 * 存在主世界的 SavedData 中，维度写在每条记录里。以本表为准去重与复原。
 */
public final class GuardianRegistry extends SavedData {
    public static final String NAME = "mythic_age_guardians";

    /**
     * @param bell       所属村庄的聚会钟；刷怪蛋或命令生成的祭司没有
     * @param entityUuid 当前（或最后一个）实体的 UUID；复原后换新
     */
    public record Entry(UUID guardianId, GlobalPos home, Optional<BlockPos> bell, boolean alive, long deathDay,
                        UUID entityUuid) {
        Entry dead(long day) {
            return new Entry(guardianId, home, bell, false, day, entityUuid);
        }

        Entry revived(UUID entity) {
            return new Entry(guardianId, home, bell, true, 0, entity);
        }
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public static GuardianRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GuardianRegistry::new, GuardianRegistry::read, null), NAME);
    }

    public void put(Entry e) {
        entries.put(e.guardianId(), e);
        setDirty();
    }

    public Optional<Entry> get(UUID guardianId) {
        return Optional.ofNullable(entries.get(guardianId));
    }

    public Optional<Entry> byBell(ResourceKey<Level> dim, BlockPos bell) {
        return entries.values().stream()
                .filter(e -> e.home().dimension().equals(dim) && e.bell().map(bell::equals).orElse(false))
                .findFirst();
    }

    public void markDead(UUID guardianId, long day) {
        Entry e = entries.get(guardianId);
        if (e != null && e.alive()) {
            entries.put(guardianId, e.dead(day));
            setDirty();
        }
    }

    public void markAlive(UUID guardianId, UUID entityUuid) {
        Entry e = entries.get(guardianId);
        if (e != null) {
            entries.put(guardianId, e.revived(entityUuid));
            setDirty();
        }
    }

    public void remove(UUID guardianId) {
        if (entries.remove(guardianId) != null) {
            setDirty();
        }
    }

    public List<Entry> all() {
        return new ArrayList<>(entries.values());
    }

    // ───────────── 存取 ─────────────

    public static GuardianRegistry read(CompoundTag tag, HolderLookup.Provider registries) {
        GuardianRegistry r = new GuardianRegistry();
        for (Tag t : tag.getList("guardians", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            try {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim")));
                Optional<BlockPos> bell = c.contains("bell") ? Optional.of(BlockPos.of(c.getLong("bell"))) : Optional.empty();
                Entry e = new Entry(c.getUUID("id"), GlobalPos.of(dim, BlockPos.of(c.getLong("home"))), bell,
                        c.getBoolean("alive"), c.getLong("deathDay"), c.getUUID("entity"));
                r.entries.put(e.guardianId(), e);
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的守护者登记：{}（{}）", c, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.guardianId());
            c.putString("dim", e.home().dimension().location().toString());
            c.putLong("home", e.home().pos().asLong());
            e.bell().ifPresent(b -> c.putLong("bell", b.asLong()));
            c.putBoolean("alive", e.alive());
            c.putLong("deathDay", e.deathDay());
            c.putUUID("entity", e.entityUuid());
            list.add(c);
        }
        tag.put("guardians", list);
        return tag;
    }
}
