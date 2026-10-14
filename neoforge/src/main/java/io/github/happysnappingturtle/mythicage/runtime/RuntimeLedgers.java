package io.github.happysnappingturtle.mythicage.runtime;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 全部私人恩宠台账（T5 设计文档 §6），世界级 SavedData {@code mythic_age_runtime}，挂在主世界。
 * 读取时跳过损坏的条目并记录 WARN，不让整份数据作废。
 */
public final class RuntimeLedgers extends SavedData {
    public static final String NAME = "mythic_age_runtime";
    private static final int FORMAT = 1;

    private final Map<ProducerId, ProducerLedger> ledgers = new LinkedHashMap<>();

    public static RuntimeLedgers get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RuntimeLedgers::new, RuntimeLedgers::load, null), NAME);
    }

    /** 登记产出点；同 id 已存在时替换（例如神坛重建）。 */
    public ProducerLedger register(ProducerLedger ledger) {
        ledgers.put(ledger.id(), ledger);
        setDirty();
        return ledger;
    }

    public Optional<ProducerLedger> get(ProducerId id) {
        return Optional.ofNullable(ledgers.get(id));
    }

    public boolean remove(ProducerId id) {
        boolean removed = ledgers.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /** 拆除、核对失败、规则升级：快照不可再用，台账保留（重建后由产出点重新校验）。 */
    public boolean invalidate(ProducerId id) {
        ProducerLedger l = ledgers.get(id);
        if (l == null) {
            return false;
        }
        l.invalidate();
        setDirty();
        return true;
    }

    public List<ProducerLedger> byOwner(UUID owner) {
        List<ProducerLedger> out = new ArrayList<>();
        for (ProducerLedger l : ledgers.values()) {
            if (l.owner().equals(owner)) {
                out.add(l);
            }
        }
        return out;
    }

    public Collection<ProducerLedger> all() {
        return Collections.unmodifiableCollection(ledgers.values());
    }

    public List<ProducerId> ids() {
        return new ArrayList<>(ledgers.keySet());
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("format", FORMAT);
        ListTag list = new ListTag();
        for (ProducerLedger l : ledgers.values()) {
            list.add(write(l));
        }
        tag.put("ledgers", list);
        return tag;
    }

    public static RuntimeLedgers load(CompoundTag tag, HolderLookup.Provider registries) {
        RuntimeLedgers data = new RuntimeLedgers();
        ListTag list = tag.getList("ledgers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                ProducerLedger l = read(t);
                data.ledgers.put(l.id(), l);
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("Skipping broken runtime ledger #{} ({}): {}", i, e.getMessage(), t);
            }
        }
        return data;
    }

    public static CompoundTag write(ProducerLedger l) {
        CompoundTag t = new CompoundTag();
        t.putString("id", l.id().value());
        t.putUUID("owner", l.owner());
        t.putString("civ", l.civ().id());
        t.putDouble("estimatedRate", l.estimatedRatePerDay());
        t.putDouble("snapshotRate", l.snapshotRatePerDay());
        t.putBoolean("verified", l.verified());
        t.putBoolean("snapshotValid", l.snapshotValid());
        t.putLong("lastSettled", l.lastSettledTick());
        t.putLong("lastVisit", l.lastVisitTick());
        t.putDouble("carry", l.carry());
        t.putDouble("offlineBuffer", l.offlineBuffer());
        t.putLong("nearbySince", l.nearbySinceTick());
        t.putLong("realStreakStart", l.realStreakStartTick());
        t.putDouble("realStreakYield", l.realStreakYield());
        t.putLong("awayActive", l.awayActiveTicks());
        return t;
    }

    /** @throws RuntimeException id、owner 或文明缺失或非法 */
    public static ProducerLedger read(CompoundTag t) {
        ProducerId id = new ProducerId(t.getString("id"));
        if (!t.hasUUID("owner")) {
            throw new IllegalArgumentException("缺少 owner");
        }
        Civilization civ = Civilization.byId(t.getString("civ"))
                .orElseThrow(() -> new IllegalArgumentException("未知文明：" + t.getString("civ")));
        ProducerLedger l = new ProducerLedger(id, t.getUUID("owner"), civ, t.getDouble("estimatedRate"),
                t.getLong("lastSettled"));
        l.setSnapshotRatePerDay(t.getDouble("snapshotRate"));
        l.setVerified(t.getBoolean("verified"));
        l.setSnapshotValid(!t.contains("snapshotValid") || t.getBoolean("snapshotValid"));
        l.setLastVisitTick(t.getLong("lastVisit"));
        l.setCarry(t.getDouble("carry"));
        l.setOfflineBuffer(t.getDouble("offlineBuffer"));
        l.setNearbySinceTick(t.contains("nearbySince") ? t.getLong("nearbySince") : ProducerLedger.NONE);
        l.setRealStreak(t.contains("realStreakStart") ? t.getLong("realStreakStart") : ProducerLedger.NONE,
                t.getDouble("realStreakYield"));
        l.setAwayActiveTicks(t.getLong("awayActive"));
        return l;
    }
}
