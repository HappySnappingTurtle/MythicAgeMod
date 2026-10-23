package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.relic.RelicBinding;
import io.github.happysnappingturtle.mythicage.core.relic.RelicState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.Optional;

/**
 * 玩家神话数据与 NBT 的互转（T4 设计文档 §4）。根标签带 {@code format} 版本号。
 * 读取时遇到未知文明、非法时代或负数恩宠，只跳过或修正那一项并记录警告，不让整份数据作废。
 * 格式 2（T11a）：各文明加主神认可、Boss 凭证、改奉日、仪式村庄、待弹成功界面，根加试炼击败次数；
 * 格式 1 的存档缺这些字段，读作空。
 * 格式 3（T12）：各文明加神力共享冷却到期刻，根加各神力长冷却到期刻；旧档缺省为 0。
 * 格式 4（T16）：根加圣物绑定表 {@code relics}；旧档没有这个键，读作空表。
 */
public final class PlayerDataNbt {
    public static final int FORMAT = 4;

    private PlayerDataNbt() {
    }

    public static CompoundTag write(PlayerMythData data) {
        CompoundTag root = new CompoundTag();
        root.putInt("format", FORMAT);
        CompoundTag civs = new CompoundTag();
        data.civsView().forEach((civ, state) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("favor", state.favor());
            c.putString("age", state.age().id());
            c.put("knownPatrons", strings(state.knownPatrons()));
            CompoundTag gods = new CompoundTag();
            state.minorGods().forEach((a, g) -> gods.putString(a.id(), g));
            c.put("minorGods", gods);
            CompoundTag approvals = new CompoundTag();
            state.approvals().forEach((patron, a) -> approvals.putString(patron, a.id()));
            c.put("approvals", approvals);
            CompoundTag vouchers = new CompoundTag();
            state.trialVouchers().forEach((a, n) -> vouchers.putInt(a.id(), n));
            c.put("trialVouchers", vouchers);
            c.putLong("rededicatedDay", state.rededicatedDay());
            ListTag villages = new ListTag();
            state.riteVillages().forEach((id, mark) -> {
                CompoundTag t = new CompoundTag();
                t.putUUID("id", id);
                t.putString("age", mark.highest().id());
                t.putBoolean("promoted", mark.promotedHere());
                villages.add(t);
            });
            c.put("riteVillages", villages);
            ListTag pending = new ListTag();
            state.pendingUnlocks().forEach(a -> pending.add(StringTag.valueOf(a.id())));
            c.put("pendingUnlocks", pending);
            c.putLong("powerCooldownUntil", state.powerCooldownUntil());
            civs.put(civ.id(), c);
        });
        root.put("civs", civs);
        root.put("firstKills", strings(data.firstKills()));
        CompoundTag trialKills = new CompoundTag();
        data.trialKills().forEach(trialKills::putInt);
        root.put("trialKills", trialKills);
        CompoundTag cooldowns = new CompoundTag();
        data.powerCooldowns().forEach(cooldowns::putLong);
        root.put("powerCooldowns", cooldowns);
        root.put("discoveries", strings(data.discoveries()));
        root.put("blueprints", strings(data.blueprints()));
        CompoundTag forge = new CompoundTag();
        data.forgeRecords().forEach((id, record) -> forge.putInt(id, record.generation()));
        root.put("forge", forge);
        CompoundTag conduct = new CompoundTag();
        conduct.putInt("score", data.conductScore());
        ListTag recent = new ListTag();
        for (PlayerMythData.ConductEntry e : data.recentConduct()) {
            CompoundTag t = new CompoundTag();
            t.putString("reason", e.reason());
            t.putInt("delta", e.delta());
            t.putLong("time", e.gameTime());
            recent.add(t);
        }
        conduct.put("recent", recent);
        root.put("conduct", conduct);
        CompoundTag receipts = new CompoundTag();
        receipts.put("unique", strings(data.receipts().list(ReceiptBook.Scope.UNIQUE)));
        receipts.put("request", strings(data.receipts().list(ReceiptBook.Scope.REQUEST)));
        root.put("receipts", receipts);
        CompoundTag daily = new CompoundTag();
        data.tradeDaily().view().forEach((key, c) -> {
            CompoundTag t = new CompoundTag();
            t.putLong("day", c.day());
            t.putInt("count", c.count());
            daily.put(key, t);
        });
        root.put("tradeDaily", daily);
        ListTag relics = new ListTag();
        data.relics().view().forEach((id, b) -> {
            CompoundTag t = new CompoundTag();
            t.putString("id", id);
            t.putString("state", b.state().name());
            t.putUUID("instance", b.instance());
            t.putString("civ", b.civ().id());
            t.putString("where", b.where());
            relics.add(t);
        });
        root.put("relics", relics);
        root.putLong("version", data.dataVersion());
        return root;
    }

    public static PlayerMythData read(CompoundTag root) {
        PlayerMythData data = new PlayerMythData();
        int format = root.getInt("format");
        if (format > FORMAT) {
            MythicAge.LOGGER.warn("[player data] 存档格式 {} 高于当前版本 {}，尽力读取", format, FORMAT);
        }
        CompoundTag civs = root.getCompound("civs");
        for (String key : civs.getAllKeys()) {
            Optional<Civilization> civ = Civilization.byId(key);
            if (civ.isEmpty()) {
                MythicAge.LOGGER.warn("[player data] 跳过未知文明：{}", key);
                continue;
            }
            CompoundTag c = civs.getCompound(key);
            long favor = c.getLong("favor");
            if (favor < 0) {
                MythicAge.LOGGER.warn("[player data] {} 恩宠为负数（{}），按 0 读取", key, favor);
            }
            data.civ(civ.get()).setFavor(favor);
            String ageId = c.getString("age");
            Optional<Age> age = Age.byId(ageId);
            if (age.isPresent()) {
                data.civ(civ.get()).setAge(age.get());
            } else if (!ageId.isEmpty()) {
                MythicAge.LOGGER.warn("[player data] {} 时代非法（{}），按未接触处理", key, ageId);
            }
            readStrings(c, "knownPatrons", data.civ(civ.get()).knownPatrons());
            CompoundTag gods = c.getCompound("minorGods");
            for (String a : gods.getAllKeys()) {
                Age.byId(a).ifPresent(godAge -> data.civ(civ.get()).minorGods().put(godAge, gods.getString(a)));
            }
            readRites(c, data.civ(civ.get()));
            data.civ(civ.get()).setPowerCooldownUntil(c.getLong("powerCooldownUntil"));
        }
        readStrings(root, "firstKills", data.firstKills());
        CompoundTag cooldowns = root.getCompound("powerCooldowns");
        for (String id : cooldowns.getAllKeys()) {
            if (cooldowns.getLong(id) > 0) {
                data.powerCooldowns().put(id, cooldowns.getLong(id));
            }
        }
        CompoundTag trialKills = root.getCompound("trialKills");
        for (String boss : trialKills.getAllKeys()) {
            if (trialKills.getInt(boss) > 0) {
                data.trialKills().put(boss, trialKills.getInt(boss));
            }
        }
        readStrings(root, "discoveries", data.discoveries());
        readStrings(root, "blueprints", data.blueprints());
        CompoundTag forge = root.getCompound("forge");
        for (String id : forge.getAllKeys()) {
            int generation = forge.getInt(id);
            if (generation >= 1) {
                data.forgeRecords().put(id, new PlayerMythData.ForgeRecord(generation));
            }
        }
        CompoundTag conduct = root.getCompound("conduct");
        data.setConductScore(conduct.getInt("score"));
        for (Tag t : conduct.getList("recent", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            data.addConductEntry(new PlayerMythData.ConductEntry(e.getString("reason"), e.getInt("delta"), e.getLong("time")));
        }
        CompoundTag receipts = root.getCompound("receipts");
        for (Tag t : receipts.getList("unique", Tag.TAG_STRING)) {
            data.receipts().record(ReceiptBook.Scope.UNIQUE, t.getAsString());
        }
        for (Tag t : receipts.getList("request", Tag.TAG_STRING)) {
            data.receipts().record(ReceiptBook.Scope.REQUEST, t.getAsString());
        }
        CompoundTag daily = root.getCompound("tradeDaily");
        for (String key : daily.getAllKeys()) {
            CompoundTag t = daily.getCompound(key);
            data.tradeDaily().restore(key, new io.github.happysnappingturtle.mythicage.core.player.DailyLedger.DailyCount(
                    t.getLong("day"), t.getInt("count")));
        }
        readRelics(root, data);
        data.setDataVersion(root.getLong("version"));
        return data;
    }

    /** 格式 4 的圣物绑定表（T16）；不合法的条目跳过并记录警告。 */
    private static void readRelics(CompoundTag root, PlayerMythData data) {
        for (Tag t : root.getList("relics", Tag.TAG_COMPOUND)) {
            CompoundTag r = (CompoundTag) t;
            try {
                RelicState state = RelicState.valueOf(r.getString("state"));
                Civilization civ = Civilization.byId(r.getString("civ")).orElseThrow();
                data.relics().restore(r.getString("id"), new RelicBinding(state, r.getUUID("instance"), civ, r.getString("where")));
            } catch (RuntimeException e) {
                MythicAge.LOGGER.warn("[player data] 跳过不合法的圣物绑定：{}", r);
            }
        }
    }

    /** 格式 2 的仪式字段（T11a）；格式 1 没有这些键，全部读作空。 */
    private static void readRites(CompoundTag c, PlayerMythData.CivState state) {
        CompoundTag approvals = c.getCompound("approvals");
        for (String patron : approvals.getAllKeys()) {
            Age.byId(approvals.getString(patron)).ifPresent(a -> state.approve(patron, a));
        }
        CompoundTag vouchers = c.getCompound("trialVouchers");
        for (String a : vouchers.getAllKeys()) {
            Age.byId(a).ifPresent(tier -> state.setVouchers(tier, vouchers.getInt(a)));
        }
        state.setRededicatedDay(c.contains("rededicatedDay") ? c.getLong("rededicatedDay") : -1);
        for (Tag t : c.getList("riteVillages", Tag.TAG_COMPOUND)) {
            CompoundTag v = (CompoundTag) t;
            if (v.hasUUID("id")) {
                state.riteVillages().put(v.getUUID("id"), new PlayerMythData.RiteMark(
                        Age.byId(v.getString("age")).orElse(Age.NONE), v.getBoolean("promoted")));
            }
        }
        for (Tag t : c.getList("pendingUnlocks", Tag.TAG_STRING)) {
            Age.byId(t.getAsString()).filter(a -> a != Age.NONE).ifPresent(state.pendingUnlocks()::add);
        }
    }

    private static ListTag strings(Collection<String> values) {
        ListTag list = new ListTag();
        values.forEach(v -> list.add(StringTag.valueOf(v)));
        return list;
    }

    private static void readStrings(CompoundTag root, String key, Collection<String> target) {
        for (Tag t : root.getList(key, Tag.TAG_STRING)) {
            target.add(t.getAsString());
        }
    }
}
