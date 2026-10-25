package io.github.happysnappingturtle.mythicage.village.quest;

import io.github.happysnappingturtle.mythicage.core.village.quest.Quest;
import io.github.happysnappingturtle.mythicage.core.village.quest.QuestState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * NPC 任务存档（T21 设计文档 §11）：每村一本——任务表、已用编号、每人在本村的贡献、备料进展记录（发布条件用）、
 * 每人看过的最大任务编号与当天是否提示过新任务。和 {@code VillageEconomy} 分开存。
 */
public final class VillageQuests extends SavedData {
    public static final String NAME = "mythic_age_village_quests";

    /**
     * 村里能产的料的备料进展：上次看到的缺口、缺口最近一次变小的 MC 日（§4.2）。
     */
    public record Track(int missing, long progressDay) {
    }

    public static final class Book {
        public final UUID village;
        public int next = 1;
        public final List<Quest> quests = new ArrayList<>();
        public final Map<UUID, Integer> contribution = new LinkedHashMap<>();
        public final Map<String, Track> tracks = new LinkedHashMap<>();
        public final Map<UUID, Integer> seen = new LinkedHashMap<>();
        public final Map<UUID, Long> hinted = new LinkedHashMap<>();
        public long lastScanDay = -1;

        Book(UUID village) {
            this.village = village;
        }

        public Optional<Quest> byId(String id) {
            return quests.stream().filter(q -> q.id().equals(id)).findFirst();
        }

        public int contribution(UUID player) {
            return contribution.getOrDefault(player, 0);
        }
    }

    private final Map<UUID, Book> books = new LinkedHashMap<>();

    public static VillageQuests get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillageQuests::new, VillageQuests::read, null), NAME);
    }

    public Book book(UUID village) {
        setDirty();
        return books.computeIfAbsent(village, Book::new);
    }

    public Optional<Book> peek(UUID village) {
        return Optional.ofNullable(books.get(village));
    }

    public void remove(UUID village) {
        if (books.remove(village) != null) {
            setDirty();
        }
    }

    // ───────────── 存取 ─────────────

    public static VillageQuests read(CompoundTag tag, HolderLookup.Provider registries) {
        VillageQuests out = new VillageQuests();
        for (Tag t : tag.getList("villages", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Book b = new Book(c.getUUID("village"));
            b.next = Math.max(1, c.getInt("next"));
            b.lastScanDay = c.contains("lastScanDay") ? c.getLong("lastScanDay") : -1;
            for (Tag qt : c.getList("quests", Tag.TAG_COMPOUND)) {
                b.quests.add(readQuest((CompoundTag) qt));
            }
            readInts(c.getCompound("contribution"), b.contribution);
            readInts(c.getCompound("seen"), b.seen);
            CompoundTag h = c.getCompound("hinted");
            for (String k : h.getAllKeys()) {
                b.hinted.put(UUID.fromString(k), h.getLong(k));
            }
            CompoundTag tr = c.getCompound("tracks");
            for (String k : tr.getAllKeys()) {
                CompoundTag x = tr.getCompound(k);
                b.tracks.put(k, new Track(x.getInt("missing"), x.getLong("day")));
            }
            out.books.put(b.village, b);
        }
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Book b : books.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("village", b.village);
            c.putInt("next", b.next);
            c.putLong("lastScanDay", b.lastScanDay);
            ListTag qs = new ListTag();
            b.quests.forEach(q -> qs.add(writeQuest(q)));
            c.put("quests", qs);
            c.put("contribution", writeInts(b.contribution));
            c.put("seen", writeInts(b.seen));
            CompoundTag h = new CompoundTag();
            b.hinted.forEach((k, v) -> h.putLong(k.toString(), v));
            c.put("hinted", h);
            CompoundTag tr = new CompoundTag();
            b.tracks.forEach((k, v) -> {
                CompoundTag x = new CompoundTag();
                x.putInt("missing", v.missing());
                x.putLong("day", v.progressDay());
                tr.put(k, x);
            });
            c.put("tracks", tr);
            list.add(c);
        }
        tag.put("villages", list);
        return tag;
    }

    static CompoundTag writeQuest(Quest q) {
        CompoundTag t = new CompoundTag();
        t.putInt("number", q.number);
        t.putString("type", q.type);
        t.putString("source", q.source);
        t.putString("target", q.target);
        t.putString("scope", q.scope.id());
        t.putLong("issued", q.issued);
        t.putInt("need", q.need);
        t.putInt("done", q.done);
        t.putLong("deadline", q.deadline);
        t.putString("state", q.state.id());
        t.putString("reason", q.reason);
        t.putLong("endedDay", q.endedDay);
        t.putLong("pausedSince", q.pausedSince);
        t.putBoolean("filled", q.filled);
        CompoundTag prices = new CompoundTag();
        q.prices.forEach(prices::putDouble);
        t.put("prices", prices);
        CompoundTag owed = new CompoundTag();
        q.owed.forEach((k, v) -> owed.putDouble(k.toString(), v));
        t.put("owed", owed);
        t.put("contrib", writeInts(q.contrib));
        t.put("accepted", uuids(q.accepted));
        t.put("settled", uuids(q.settled));
        return t;
    }

    static Quest readQuest(CompoundTag t) {
        Quest q = new Quest(t.getInt("number"), t.getString("type"), t.getString("source"), t.getString("target"),
                Quest.Scope.byId(t.getString("scope")), t.getLong("issued"), t.getInt("need"), t.getLong("deadline"));
        q.done = t.getInt("done");
        q.state = QuestState.byId(t.getString("state"));
        q.reason = t.getString("reason");
        q.endedDay = t.getLong("endedDay");
        q.pausedSince = t.getLong("pausedSince");
        q.filled = t.getBoolean("filled");
        CompoundTag prices = t.getCompound("prices");
        prices.getAllKeys().forEach(k -> q.prices.put(k, prices.getDouble(k)));
        CompoundTag owed = t.getCompound("owed");
        owed.getAllKeys().forEach(k -> q.owed.put(UUID.fromString(k), owed.getDouble(k)));
        readInts(t.getCompound("contrib"), q.contrib);
        for (Tag x : t.getList("accepted", Tag.TAG_STRING)) {
            q.accepted.add(UUID.fromString(x.getAsString()));
        }
        for (Tag x : t.getList("settled", Tag.TAG_STRING)) {
            q.settled.add(UUID.fromString(x.getAsString()));
        }
        return q;
    }

    private static ListTag uuids(Iterable<UUID> ids) {
        ListTag l = new ListTag();
        ids.forEach(u -> l.add(StringTag.valueOf(u.toString())));
        return l;
    }

    private static CompoundTag writeInts(Map<UUID, Integer> m) {
        CompoundTag t = new CompoundTag();
        m.forEach((k, v) -> t.putInt(k.toString(), v));
        return t;
    }

    private static void readInts(CompoundTag t, Map<UUID, Integer> into) {
        t.getAllKeys().forEach(k -> into.put(UUID.fromString(k), t.getInt(k)));
    }

    /** 只供 GameTest：写 NBT 再读回（模拟重启）。 */
    public static VillageQuests roundTripForTest(VillageQuests q, HolderLookup.Provider registries) {
        return read(q.save(new CompoundTag(), registries), registries);
    }

    /** 只供 GameTest：用读回来的数据替换某个村庄的任务本。 */
    public void replaceForTest(UUID village, VillageQuests from) {
        from.peek(village).ifPresent(b -> books.put(village, b));
        setDirty();
    }
}
