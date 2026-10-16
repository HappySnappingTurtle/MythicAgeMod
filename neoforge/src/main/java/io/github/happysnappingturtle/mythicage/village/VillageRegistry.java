package io.github.happysnappingturtle.mythicage.village;

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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 村庄登记（T10a 设计文档 §2.1）：以聚会钟为唯一键。主世界 SavedData，维度写在钟的 GlobalPos 里。
 * 文明与主神一经登记永久不变；钟连续 7 个 MC 日不在时注销。
 */
public final class VillageRegistry extends SavedData {
    public static final String NAME = "mythic_age_villages";
    public static final int EXPIRE_DAYS = 7;

    public enum Origin {
        VANILLA, GENERATED, FOUNDED;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Origin byId(String id) {
            for (Origin o : values()) {
                if (o.id().equals(id)) {
                    return o;
                }
            }
            return VANILLA;
        }
    }

    /**
     * @param civ             文明 id；中立为空串
     * @param patron          主神 id；中立或该文明没有主神权重时为空串
     * @param missingSinceDay 钟不在的起始 MC 日；钟在时为 −1
     */
    public record Village(UUID id, GlobalPos bell, String civ, String patron, Origin origin, @Nullable UUID founder,
                          String vanillaType, long missingSinceDay) {
        public boolean neutral() {
            return civ.isEmpty();
        }

        Village withMissingSince(long day) {
            return new Village(id, bell, civ, patron, origin, founder, vanillaType, day);
        }
    }

    private final Map<UUID, Village> byId = new LinkedHashMap<>();
    private final Map<GlobalPos, UUID> byBell = new LinkedHashMap<>();

    public static VillageRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillageRegistry::new, VillageRegistry::read, null), NAME);
    }

    public void add(Village v) {
        Village old = byId.put(v.id(), v);
        if (old != null) {
            byBell.remove(old.bell());
        }
        byBell.put(v.bell(), v.id());
        setDirty();
    }

    public Optional<Village> byBell(GlobalPos bell) {
        UUID id = byBell.get(bell);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public Optional<Village> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public boolean remove(GlobalPos bell) {
        UUID id = byBell.remove(bell);
        if (id != null) {
            byId.remove(id);
            setDirty();
            return true;
        }
        return false;
    }

    void setMissingSince(Village v, long day) {
        if (v.missingSinceDay() != day) {
            add(v.withMissingSince(day));
        }
    }

    public List<Village> all() {
        return new ArrayList<>(byId.values());
    }

    public List<Village> in(ResourceKey<Level> dim) {
        return byId.values().stream().filter(v -> v.bell().dimension().equals(dim)).toList();
    }

    // ───────────── 存取 ─────────────

    public static VillageRegistry read(CompoundTag tag, HolderLookup.Provider registries) {
        VillageRegistry r = new VillageRegistry();
        for (Tag t : tag.getList("villages", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            try {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(c.getString("dim")));
                Village v = new Village(c.getUUID("id"), GlobalPos.of(dim, BlockPos.of(c.getLong("bell"))), c.getString("civ"),
                        c.getString("patron"), Origin.byId(c.getString("origin")),
                        c.hasUUID("founder") ? c.getUUID("founder") : null, c.getString("vanillaType"),
                        c.contains("missingSince") ? c.getLong("missingSince") : -1);
                r.byId.put(v.id(), v);
                r.byBell.put(v.bell(), v.id());
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("跳过损坏的村庄登记：{}（{}）", c, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Village v : byId.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", v.id());
            c.putString("dim", v.bell().dimension().location().toString());
            c.putLong("bell", v.bell().pos().asLong());
            c.putString("civ", v.civ());
            c.putString("patron", v.patron());
            c.putString("origin", v.origin().id());
            if (v.founder() != null) {
                c.putUUID("founder", v.founder());
            }
            c.putString("vanillaType", v.vanillaType());
            if (v.missingSinceDay() >= 0) {
                c.putLong("missingSince", v.missingSinceDay());
            }
            list.add(c);
        }
        tag.put("villages", list);
        return tag;
    }
}
