package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import io.github.happysnappingturtle.mythicage.core.village.temple.TempleCredit;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.runtime.PlayerActivity;
import io.github.happysnappingturtle.mythicage.runtime.ProducerProbe;
import io.github.happysnappingturtle.mythicage.runtime.ProducerProbes;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.temple.TempleBook;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 村庄神庙的祈祷入账与献供（GA-13 §2.3、§2.4）：信徒在本村神庙的祭坛前祈祷满 {@link PrayerFormula#PRAY_TICKS} 刻，
 * 这个村子的每位供奉者各入一笔（{@link TempleCredit}）；台账按「村 × 供奉者」登记 {@code village_temple:<村>:<玩家>}，
 * T5 的结算通过探针 {@value #PROBE_TYPE} 取走待领产出。献供按「人 × 村 × 日」记在 {@link TempleBook}。
 * 祈祷位预留与玩家神坛共用 {@link PrayerService#SLOTS}（按祭坛坐标分开）。
 */
public final class TemplePrayer {
    public static final String PROBE_TYPE = "village_temple";

    /** 「村|玩家」→ 待领产出。 */
    private static final Map<String, Double> PENDING = new HashMap<>();
    /** 「村|玩家」→ {MC 日, 当日已得}。 */
    private static final Map<String, double[]> TODAY = new HashMap<>();
    /** 「村|玩家」→ {MC 日, 当日不含献供的入账}：当天献供时按它补发。 */
    private static final Map<String, double[]> TODAY_BASE = new HashMap<>();
    /** 村 → 最近一次有信徒走不到祈祷位的 gameTime。 */
    private static final Map<UUID, Long> UNREACHABLE = new HashMap<>();

    private TemplePrayer() {
    }

    public static void init() {
        ProducerProbes.register(PROBE_TYPE, TemplePrayer::observe);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        TODAY.clear();
        TODAY_BASE.clear();
        UNREACHABLE.clear();
    }

    static String key(UUID village, UUID player) {
        return village + "|" + player;
    }

    // ───────────── 台账 ─────────────

    public static ProducerId ledgerId(UUID village, UUID player) {
        return new ProducerId(PROBE_TYPE + ":" + village + ":" + player);
    }

    /** 台账 id → {村, 玩家}；格式不符为空。 */
    static Optional<UUID[]> parse(ProducerId id) {
        String v = id.value();
        if (!v.startsWith(PROBE_TYPE + ":")) {
            return Optional.empty();
        }
        String[] p = v.substring(PROBE_TYPE.length() + 1).split(":");
        if (p.length != 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(new UUID[]{UUID.fromString(p[0]), UUID.fromString(p[1])});
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** 确保这位供奉者在这个村子有台账，并更新估算日产（快照用）。 */
    public static void ensureLedger(MinecraftServer server, VillageTemple t, UUID player) {
        RuntimeLedgers ledgers = RuntimeLedgers.get(server);
        ProducerId id = ledgerId(t.village(), player);
        double rate = estimate(server, t, player);
        Optional<ProducerLedger> existing = ledgers.get(id);
        if (existing.isEmpty()) {
            ledgers.register(new ProducerLedger(id, player, civ(t), rate, WorldClock.now(server)));
        } else if (Double.compare(rate, existing.get().estimatedRatePerDay()) != 0) {
            existing.get().setEstimatedRatePerDay(rate);
            ledgers.setDirty();
        }
    }

    /** 不再供奉：注销台账、放掉待领产出。 */
    public static void dropLedger(MinecraftServer server, UUID village, UUID player) {
        RuntimeLedgers.get(server).remove(ledgerId(village, player));
        String k = key(village, player);
        PENDING.remove(k);
        TODAY.remove(k);
        TODAY_BASE.remove(k);
    }

    private static Civilization civ(VillageTemple t) {
        return Civilization.byId(t.civ()).orElse(Civilization.GREEK);
    }

    // ───────────── 入账 ─────────────

    /**
     * 一名信徒在本村神庙祈祷满 600 刻：记为今日信徒，给每位供奉者各入一笔。今天已在这里计入过时返回 false。
     * 没有供奉者也记信徒（村民照常祈祷，只是没人入账）。
     */
    public static boolean credit(ServerLevel level, UUID village, UUID villager) {
        MinecraftServer server = level.getServer();
        Optional<VillageTemple> temple = VillageTemples.of(server, village);
        if (temple.isEmpty()) {
            return false;
        }
        VillageTemple t = temple.get();
        TempleBook book = VillageTemples.book(server, village);
        long today = MythDays.today(server);
        if (book.prayedToday(villager, today)) {
            return false;
        }
        book.notePrayer(villager, today);
        VillageTemples.changed(server);
        double weather = t.openAir() && PrayerService.raining(level, t.altar().pos().above()) ? PrayerFormula.RAIN_FACTOR : 1;
        TempleCredit.Inputs in = new TempleCredit.Inputs(t.coefficient(), weather, PrayerService.multiplier(),
                OfferingLoader.greek().bonus());
        Map<UUID, TempleCredit.Share> shares = TempleCredit.split(book.devotees(),
                d -> VillageTemples.believersToday(server, d, today), PrayerService::relicR,
                d -> book.offeredOn(d, today) ? 1 : 0, in);
        shares.forEach((d, s) -> {
            String k = key(village, d);
            if (s.gain() > 0) {
                PENDING.merge(k, s.gain(), Double::sum);
            }
            addToday(TODAY, k, today, s.gain());
            addToday(TODAY_BASE, k, today, s.base());
            ensureLedger(server, t, d);
        });
        return true;
    }

    private static void addToday(Map<String, double[]> map, String key, long today, double v) {
        double[] t = map.computeIfAbsent(key, k -> new double[]{today, 0});
        if (t[0] != today) {
            t[0] = today;
            t[1] = 0;
        }
        t[1] += v;
    }

    private static double todayOf(Map<String, double[]> map, String key, long today) {
        double[] t = map.get(key);
        return t != null && t[0] == today ? t[1] : 0;
    }

    public static double pending(UUID village, UUID player) {
        return PENDING.getOrDefault(key(village, player), 0.0);
    }

    static double takePending(UUID village, UUID player) {
        Double v = PENDING.remove(key(village, player));
        return v == null ? 0 : v;
    }

    /** 这位供奉者今天在这个村子已得（含献供补发）。 */
    public static double today(MinecraftServer server, UUID village, UUID player) {
        return todayOf(TODAY, key(village, player), MythDays.today(server));
    }

    // ───────────── 献供 ─────────────

    /**
     * 供奉者手持供品献给本村神庙：扣物品、记今天已献、把今天已入账的部分按加成补一笔。不成功时不扣物品。
     * 非供奉者返回 {@link PrayerService.OfferResult#NOT_DEVOTEE}。
     */
    public static PrayerService.OfferResult offer(ServerPlayer player, VillageTemple t, ItemStack stack) {
        var table = OfferingLoader.greek();
        var offering = table.of(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (offering.isEmpty()) {
            return PrayerService.OfferResult.NOT_OFFERING;
        }
        MinecraftServer server = player.server;
        TempleBook book = VillageTemples.book(server, t.village());
        if (!book.isDevotee(player.getUUID())) {
            return PrayerService.OfferResult.NOT_DEVOTEE;
        }
        long today = MythDays.today(server);
        if (book.offeredOn(player.getUUID(), today)) {
            return PrayerService.OfferResult.ALREADY;
        }
        if (stack.getCount() < offering.get().count()) {
            return PrayerService.OfferResult.NOT_ENOUGH;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(offering.get().count());
        }
        book.markOffered(player.getUUID(), today);
        VillageTemples.changed(server);
        String k = key(t.village(), player.getUUID());
        double back = todayOf(TODAY_BASE, k, today) * table.bonus();
        if (back > 0) {
            PENDING.merge(k, back, Double::sum);
            addToday(TODAY, k, today, back);
        }
        return PrayerService.OfferResult.OK;
    }

    // ───────────── 探针 ─────────────

    private static ProducerProbe.Observation observe(MinecraftServer server, ProducerLedger ledger, ServerPlayer owner,
                                                     long now, long interval) {
        Optional<UUID[]> ids = parse(ledger.id());
        if (ids.isEmpty()) {
            return ProducerProbe.Observation.NOT_RUNNING;
        }
        double yield = takePending(ids.get()[0], ids.get()[1]);
        Optional<VillageTemple> t = VillageTemples.of(server, ids.get()[0]);
        if (t.isEmpty()) {
            return ProducerProbe.Observation.NOT_RUNNING;
        }
        GlobalPos altar = t.get().altar();
        ServerLevel level = server.getLevel(altar.dimension());
        if (level == null || !ProducerProbes.chunkRunning(level, altar.pos())) {
            return ProducerProbe.Observation.NOT_RUNNING;
        }
        boolean nearby = owner != null && owner.level().dimension().equals(altar.dimension())
                && owner.blockPosition().closerThan(altar.pos(), PrayerService.OWNER_NEARBY);
        return new ProducerProbe.Observation(true, nearby, yield);
    }

    // ───────────── 统计 ─────────────

    /** 预计日产：3.5 × √min(近 7 日信徒, 24) × T × (1 + R) × M（不含献供）。 */
    public static double estimate(MinecraftServer server, VillageTemple t, UUID player) {
        TempleBook book = VillageTemples.book(server, t.village());
        return PrayerFormula.estimate(book.believers(MythDays.today(server)), t.coefficient(), PrayerService.relicR(player),
                PrayerService.multiplier());
    }

    /**
     * 一个村子神庙对某位玩家的统计（检视界面、信仰面板共用）。
     *
     * @param devotee    这位玩家是这里的供奉者
     * @param devotees   这里的供奉者人数
     * @param praying    正在祈祷（已到达祈祷位）的人数
     * @param believers  近 7 日信徒
     * @param today      这位玩家今天在这里已得
     * @param favorFull  这位玩家的希腊恩宠已满
     */
    public record Stats(boolean devotee, int devotees, int praying, int believers, int todayBelievers, double today,
                        double estimate, boolean offered, boolean rainHalved, int tooClose, boolean unreachable,
                        boolean favorFull) {
    }

    public static Stats stats(MinecraftServer server, VillageTemple t, UUID viewer) {
        TempleBook book = VillageTemples.book(server, t.village());
        long now = WorldClock.now(server);
        long today = MythDays.today(server);
        List<PrayerSlots.Claim> live = PrayerService.SLOTS.live(t.altar(), now);
        int praying = (int) live.stream().filter(PrayerSlots.Claim::arrived).count();
        int tooClose = (int) live.stream().filter(PrayerSlots.Claim::tooClose).count();
        ServerLevel level = server.getLevel(t.altar().dimension());
        boolean rain = level != null && t.openAir() && PrayerService.raining(level, t.altar().pos().above());
        Long gaveUp = UNREACHABLE.get(t.village());
        ServerPlayer p = PlayerActivity.onlinePlayer(server, viewer);
        boolean full = p != null && MythData.get(p).civ(Civilization.GREEK).favor() >= MythData.cap(p, Civilization.GREEK);
        return new Stats(book.isDevotee(viewer), book.devotees().size(), praying, book.believers(today),
                book.believersToday(today), todayOf(TODAY, key(t.village(), viewer), today), estimate(server, t, viewer),
                book.offeredOn(viewer, today), rain, tooClose, gaveUp != null && now - gaveUp < PrayerService.UNREACHABLE_MEMORY,
                full);
    }

    static void markUnreachable(UUID village, long now) {
        UNREACHABLE.put(village, now);
    }

    /** 祭坛还是这个村子的神庙祭坛（神庙扩建后祭坛会挪位置）。 */
    static boolean isTempleAltar(MinecraftServer server, UUID village, GlobalPos altar) {
        return VillageTemples.of(server, village).map(t -> t.altar().equals(altar)).orElse(false);
    }

    /** 某村民所属希腊村子的神庙。 */
    static Optional<VillageTemple> templeOf(MinecraftServer server, VillageRegistry.Village v) {
        return "greek".equals(v.civ()) ? VillageTemples.of(server, v) : Optional.empty();
    }

    /** 只供 GameTest：清掉某村的待领、当日记录。 */
    public static void resetForTest(UUID village) {
        PENDING.keySet().removeIf(k -> k.startsWith(village + "|"));
        TODAY.keySet().removeIf(k -> k.startsWith(village + "|"));
        TODAY_BASE.keySet().removeIf(k -> k.startsWith(village + "|"));
        UNREACHABLE.remove(village);
    }

    /** 被祈祷位预留了的信徒（测试、调试命令）。 */
    public static List<UUID> claimants(MinecraftServer server, VillageTemple t) {
        return PrayerService.SLOTS.live(t.altar(), WorldClock.now(server)).stream().map(PrayerSlots.Claim::villager).toList();
    }
}
