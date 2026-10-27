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

    /**
     * 建筑里一格损坏（§6.1）：原方块状态、方块 id、责任玩家（无主为 null）、记下的 MC 日、是否已转给村民自修（D3）。
     */
    public static final class Cell {
        public final long pos;
        public final net.minecraft.world.level.block.state.BlockState state;
        public final String block;
        @org.jetbrains.annotations.Nullable
        public final UUID cause;
        public final long day;
        public boolean self;

        public Cell(long pos, net.minecraft.world.level.block.state.BlockState state, @org.jetbrains.annotations.Nullable UUID cause,
                    long day) {
            this.pos = pos;
            this.state = state;
            this.block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            this.cause = cause;
            this.day = day;
        }
    }

    /** 一座建筑（建筑盒）的损坏：未修的格、超出上限的计数、每格最近一次记损坏的日子（防同日反复拆修）。 */
    public static final class Damage {
        public final String key;
        public final io.github.happysnappingturtle.mythicage.core.village.gen.Box box;
        public final Map<Long, Cell> cells = new LinkedHashMap<>();
        public int overflow;
        public final Map<Long, Long> lastDay = new LinkedHashMap<>();

        public Damage(io.github.happysnappingturtle.mythicage.core.village.gen.Box box) {
            this.box = box;
            this.key = key(box);
        }

        public static String key(io.github.happysnappingturtle.mythicage.core.village.gen.Box b) {
            return b.minX() + "," + b.minY() + "," + b.minZ() + "," + b.maxX() + "," + b.maxY() + "," + b.maxZ();
        }
    }

    public static final class Book {
        public final UUID village;
        public int next = 1;
        public final List<Quest> quests = new ArrayList<>();
        public final Map<UUID, Integer> contribution = new LinkedHashMap<>();
        /** 每人最近的贡献来源（T24 §3.3，只给界面看）。 */
        public final io.github.happysnappingturtle.mythicage.core.village.quest.ContributionLog contributionLog =
                new io.github.happysnappingturtle.mythicage.core.village.quest.ContributionLog();
        public final Map<String, Track> tracks = new LinkedHashMap<>();
        public final Map<UUID, Integer> seen = new LinkedHashMap<>();
        public final Map<UUID, Long> hinted = new LinkedHashMap<>();
        public long lastScanDay = -1;
        public final Map<String, Damage> damage = new LinkedHashMap<>();
        /** 玩家放进建筑盒的方块（被打掉不算损坏，§6.2）；最多 {@value #PLAYER_BLOCKS}，超出丢掉最早的。 */
        public final java.util.LinkedHashSet<Long> playerBlocks = new java.util.LinkedHashSet<>();
        /** 每人当天在本村的修复报酬格数（§6.3 上限）：日子与格数。 */
        public final Map<UUID, Long> repairDay = new LinkedHashMap<>();
        public final Map<UUID, Integer> repairCount = new LinkedHashMap<>();

        Book(UUID village) {
            this.village = village;
        }

        public Optional<Quest> byId(String id) {
            return quests.stream().filter(q -> q.id().equals(id)).findFirst();
        }

        public int contribution(UUID player) {
            return contribution.getOrDefault(player, 0);
        }

        /** 记一笔贡献：累计数与来源记录一起写（T24 §3.3）。 */
        public void contribute(UUID player, String reason, int points, long day) {
            if (points <= 0) {
                return;
            }
            contribution.merge(player, points, Integer::sum);
            contributionLog.add(player, new io.github.happysnappingturtle.mythicage.core.village.quest.ContributionLog.Event(reason, points, day));
        }

        public void addPlayerBlock(long pos) {
            playerBlocks.remove(pos);
            playerBlocks.add(pos);
            while (playerBlocks.size() > PLAYER_BLOCKS) {
                playerBlocks.remove(playerBlocks.iterator().next());
            }
        }

        /** 这名玩家今天在本村已拿修复报酬的格数。 */
        public int repairedToday(UUID player, long today) {
            return repairDay.getOrDefault(player, -1L) == today ? repairCount.getOrDefault(player, 0) : 0;
        }

        public void addRepaired(UUID player, long today) {
            if (repairDay.getOrDefault(player, -1L) != today) {
                repairDay.put(player, today);
                repairCount.put(player, 0);
            }
            repairCount.merge(player, 1, Integer::sum);
        }
    }

    public static final int PLAYER_BLOCKS = 1024;

    private final Map<UUID, Book> books = new LinkedHashMap<>();

    public static VillageQuests get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillageQuests::new, VillageQuests::read, null), NAME);
    }

    public Book book(UUID village) {
        setDirty();
        return books.computeIfAbsent(village, Book::new);
    }

    /** 全部任务簿（只读遍历）。 */
    public java.util.Collection<Book> all() {
        return java.util.Collections.unmodifiableCollection(books.values());
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
            CompoundTag cl = c.getCompound("contributionLog");
            for (String k : cl.getAllKeys()) {
                List<io.github.happysnappingturtle.mythicage.core.village.quest.ContributionLog.Event> evs = new ArrayList<>();
                for (Tag et : cl.getList(k, Tag.TAG_COMPOUND)) {
                    CompoundTag e = (CompoundTag) et;
                    evs.add(new io.github.happysnappingturtle.mythicage.core.village.quest.ContributionLog.Event(
                            e.getString("reason"), e.getInt("points"), e.getLong("day")));
                }
                b.contributionLog.restore(UUID.fromString(k), evs);
            }
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
            net.minecraft.core.HolderGetter<net.minecraft.world.level.block.Block> blocks =
                    registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK);
            for (Tag dt : c.getList("damage", Tag.TAG_COMPOUND)) {
                CompoundTag d = (CompoundTag) dt;
                int[] bx = d.getIntArray("box");
                if (bx.length != 6) {
                    continue;
                }
                Damage dmg = new Damage(new io.github.happysnappingturtle.mythicage.core.village.gen.Box(bx[0], bx[1], bx[2], bx[3], bx[4], bx[5]));
                dmg.overflow = d.getInt("overflow");
                for (Tag ct : d.getList("cells", Tag.TAG_COMPOUND)) {
                    CompoundTag x = (CompoundTag) ct;
                    Cell cell = new Cell(x.getLong("pos"), net.minecraft.nbt.NbtUtils.readBlockState(blocks, x.getCompound("state")),
                            x.hasUUID("cause") ? x.getUUID("cause") : null, x.getLong("day"));
                    cell.self = x.getBoolean("self");
                    dmg.cells.put(cell.pos, cell);
                }
                CompoundTag ld = d.getCompound("lastDay");
                ld.getAllKeys().forEach(k -> dmg.lastDay.put(Long.parseLong(k), ld.getLong(k)));
                b.damage.put(dmg.key, dmg);
            }
            for (long pb : c.getLongArray("playerBlocks")) {
                b.playerBlocks.add(pb);
            }
            CompoundTag rd = c.getCompound("repairDay");
            rd.getAllKeys().forEach(k -> b.repairDay.put(UUID.fromString(k), rd.getLong(k)));
            readInts(c.getCompound("repairCount"), b.repairCount);
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
            CompoundTag cl = new CompoundTag();
            b.contributionLog.all().forEach((k, evs) -> {
                ListTag l = new ListTag();
                for (var e : evs) {
                    CompoundTag x = new CompoundTag();
                    x.putString("reason", e.reason());
                    x.putInt("points", e.points());
                    x.putLong("day", e.day());
                    l.add(x);
                }
                cl.put(k.toString(), l);
            });
            c.put("contributionLog", cl);
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
            ListTag damage = new ListTag();
            for (Damage dmg : b.damage.values()) {
                CompoundTag d = new CompoundTag();
                var bx = dmg.box;
                d.putIntArray("box", new int[]{bx.minX(), bx.minY(), bx.minZ(), bx.maxX(), bx.maxY(), bx.maxZ()});
                d.putInt("overflow", dmg.overflow);
                ListTag cells = new ListTag();
                for (Cell cell : dmg.cells.values()) {
                    CompoundTag x = new CompoundTag();
                    x.putLong("pos", cell.pos);
                    x.put("state", net.minecraft.nbt.NbtUtils.writeBlockState(cell.state));
                    if (cell.cause != null) {
                        x.putUUID("cause", cell.cause);
                    }
                    x.putLong("day", cell.day);
                    x.putBoolean("self", cell.self);
                    cells.add(x);
                }
                d.put("cells", cells);
                CompoundTag ld = new CompoundTag();
                dmg.lastDay.forEach((k, v) -> ld.putLong(Long.toString(k), v));
                d.put("lastDay", ld);
                damage.add(d);
            }
            c.put("damage", damage);
            c.putLongArray("playerBlocks", b.playerBlocks.stream().mapToLong(Long::longValue).toArray());
            CompoundTag rd = new CompoundTag();
            b.repairDay.forEach((k, v) -> rd.putLong(k.toString(), v));
            c.put("repairDay", rd);
            c.put("repairCount", writeInts(b.repairCount));
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
        t.putString("label", q.label);
        CompoundTag vars = new CompoundTag();
        q.vars.forEach(vars::putLong);
        t.put("vars", vars);
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
        q.label = t.getString("label");
        CompoundTag vars = t.getCompound("vars");
        vars.getAllKeys().forEach(k -> q.vars.put(k, vars.getLong(k)));
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
