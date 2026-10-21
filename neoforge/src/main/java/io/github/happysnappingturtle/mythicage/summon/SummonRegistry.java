package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import io.github.happysnappingturtle.mythicage.core.summon.SummonUnitCard;
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
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 召唤登记（T14 设计文档 §3.1，SavedData {@code mythic_age_summons}）：每个常驻神话单位一条，是归属、模式、
 * 驻守点、重生状态的唯一真相；实体只是它在世界里的化身（实体载入时与登记对照，多余的自删）。
 */
public final class SummonRegistry extends SavedData {
    private static final String NAME = "mythic_age_summons";

    /** 一个常驻神话单位。可变字段只在主线程改，改完调用 {@link #setDirty()}。 */
    public static final class Unit {
        public final UUID unitId;
        public final UUID owner;
        /** 单位卡键，如 {@code greek:minotaur}。 */
        public final String card;
        public SummonMode mode = SummonMode.FOLLOW;
        @Nullable
        public GlobalPos guard;
        @Nullable
        public GlobalPos last;
        public boolean respawning;
        public long respawnAt;
        /** 当前实体 UUID（重生后换新实体，unitId 不变）。 */
        @Nullable
        public UUID entity;

        public Unit(UUID unitId, UUID owner, String card) {
            this.unitId = unitId;
            this.owner = owner;
            this.card = card;
        }

        public int cost() {
            return SummonUnits.card(card).map(SummonUnitCard::cost).orElse(1);
        }
    }

    private final Map<UUID, Unit> units = new LinkedHashMap<>();

    public static SummonRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SummonRegistry::new, SummonRegistry::read, null), NAME);
    }

    public Unit add(UUID unitId, UUID owner, String card) {
        Unit u = new Unit(unitId, owner, card);
        units.put(unitId, u);
        setDirty();
        return u;
    }

    public Optional<Unit> get(UUID unitId) {
        return Optional.ofNullable(units.get(unitId));
    }

    public boolean remove(UUID unitId) {
        boolean removed = units.remove(unitId) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<Unit> byOwner(UUID owner) {
        List<Unit> out = new ArrayList<>();
        for (Unit u : units.values()) {
            if (u.owner.equals(owner)) {
                out.add(u);
            }
        }
        return out;
    }

    public List<Unit> all() {
        return List.copyOf(units.values());
    }

    /** 主人的占用：存活与等待重生的常驻单位的 cost 之和。 */
    public int occupied(UUID owner) {
        int n = 0;
        for (Unit u : byOwner(owner)) {
            n += u.cost();
        }
        return n;
    }

    // ───────────── 存档 ─────────────

    private static CompoundTag pos(GlobalPos p) {
        CompoundTag t = new CompoundTag();
        t.putString("dim", p.dimension().location().toString());
        t.putLong("pos", p.pos().asLong());
        return t;
    }

    @Nullable
    private static GlobalPos pos(CompoundTag t) {
        if (!t.contains("dim")) {
            return null;
        }
        ResourceKey<net.minecraft.world.level.Level> dim = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(t.getString("dim")));
        return GlobalPos.of(dim, BlockPos.of(t.getLong("pos")));
    }

    public static SummonRegistry read(CompoundTag tag, HolderLookup.Provider registries) {
        SummonRegistry r = new SummonRegistry();
        for (Tag t : tag.getList("units", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Unit u = new Unit(c.getUUID("unit"), c.getUUID("owner"), c.getString("card"));
            u.mode = SummonMode.byId(c.getString("mode")).orElse(SummonMode.FOLLOW);
            u.guard = c.contains("guard") ? pos(c.getCompound("guard")) : null;
            u.last = c.contains("last") ? pos(c.getCompound("last")) : null;
            u.respawning = c.getBoolean("respawning");
            u.respawnAt = c.getLong("respawnAt");
            u.entity = c.hasUUID("entity") ? c.getUUID("entity") : null;
            r.units.put(u.unitId, u);
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Unit u : units.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("unit", u.unitId);
            c.putUUID("owner", u.owner);
            c.putString("card", u.card);
            c.putString("mode", u.mode.id());
            if (u.guard != null) {
                c.put("guard", pos(u.guard));
            }
            if (u.last != null) {
                c.put("last", pos(u.last));
            }
            c.putBoolean("respawning", u.respawning);
            c.putLong("respawnAt", u.respawnAt);
            if (u.entity != null) {
                c.putUUID("entity", u.entity);
            }
            list.add(c);
        }
        tag.put("units", list);
        return tag;
    }
}
