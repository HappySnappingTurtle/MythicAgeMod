package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collection;
import java.util.Optional;

/**
 * 玩家神话数据与 NBT 的互转（T4 设计文档 §4）。根标签带 {@code format} 版本号。
 * 读取时遇到未知文明、非法时代或负数恩宠，只跳过或修正那一项并记录警告，不让整份数据作废。
 */
public final class PlayerDataNbt {
    public static final int FORMAT = 1;

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
            civs.put(civ.id(), c);
        });
        root.put("civs", civs);
        root.put("firstKills", strings(data.firstKills()));
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
        }
        readStrings(root, "firstKills", data.firstKills());
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
        data.setDataVersion(root.getLong("version"));
        return data;
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
