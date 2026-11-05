package io.github.happysnappingturtle.mythicage.content.greek.prayer;

import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;
import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.runtime.PlayerActivity;
import io.github.happysnappingturtle.mythicage.runtime.ProducerProbe;
import io.github.happysnappingturtle.mythicage.runtime.ProducerProbes;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleFunction;

/**
 * 信徒祈祷的产出（T10 设计文档 §5；GA-4 改为按今日信徒计）：信徒祈祷满 {@link PrayerFormula#PRAY_TICKS} 刻时
 * 按这位主人今天的信徒总数入一笔（√n 递减）到这座神坛的「待领产出」；T5 的结算通过探针 {@value #PROBE_TYPE} 取走。
 * 主人当天在神坛献供后，这座神坛当天的入账按供品表的加成提高。神坛有主人时登记台账
 * {@code greek_temple:<维度>:<x,y,z>}，拆除时注销。
 * <p>
 * 采样只遍历有祈祷位预留的神坛，不扫实体；祈祷中的村民自己报到（{@link PrayAtAltar}）。
 */
public final class PrayerService {
    public static final String PROBE_TYPE = "greek_temple";
    public static final int SAMPLE_INTERVAL = 20;
    /** 主人在神坛这个半径内算「在照看」（T5 照看期）。 */
    public static final int OWNER_NEARBY = 64;
    /** 「祈祷位走不到」提示保留的时长。 */
    static final int UNREACHABLE_MEMORY = 1200;
    /** 两次采样间隔的上限：服务器卡顿后补的时长不超过它。 */
    private static final int MAX_SAMPLE_GAP = 100;

    static final PrayerSlots SLOTS = new PrayerSlots();
    private static final Map<GlobalPos, Double> PENDING = new HashMap<>();
    /** 神坛 → {MC 日序号, 当日已得}。 */
    private static final Map<GlobalPos, double[]> TODAY = new HashMap<>();
    /** 神坛 → 最近一次有村民放弃（走不到祈祷位）的 gameTime。 */
    private static final Map<GlobalPos, Long> UNREACHABLE = new HashMap<>();
    /** 圣物加成 R（T16 接入；在此之前为 0）。 */
    private static volatile ToDoubleFunction<UUID> relicBonus = owner -> 0;
    private static long lastSample = Long.MIN_VALUE;
    /** 「此处在下雨」的判定；GameTest 结构上方有遮挡，测试时替换。 */
    private static volatile BiPredicate<Level, BlockPos> raining = Level::isRainingAt;

    private PrayerService() {
    }

    public static void init() {
        ProducerProbes.register(PROBE_TYPE, PrayerService::observe);
    }

    /** T16 圣物系统设置 R 的来源。 */
    public static void setRelicBonus(ToDoubleFunction<UUID> provider) {
        relicBonus = provider;
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SLOTS.clear();
        PENDING.clear();
        TODAY.clear();
        TODAY_BASE.clear();
        UNREACHABLE.clear();
        lastSample = Long.MIN_VALUE;
    }

    // ───────────── 台账 ─────────────

    public static ProducerId ledgerId(GlobalPos altar) {
        BlockPos p = altar.pos();
        return new ProducerId(PROBE_TYPE + ":" + altar.dimension().location() + ":" + p.getX() + "," + p.getY() + "," + p.getZ());
    }

    static Optional<GlobalPos> altarOf(ProducerId id) {
        String v = id.value();
        if (!v.startsWith(PROBE_TYPE + ":")) {
            return Optional.empty();
        }
        String rest = v.substring(PROBE_TYPE.length() + 1);
        int cut = rest.lastIndexOf(':');
        ResourceLocation dim = cut < 0 ? null : ResourceLocation.tryParse(rest.substring(0, cut));
        String[] xyz = cut < 0 ? new String[0] : rest.substring(cut + 1).split(",");
        if (dim == null || xyz.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim),
                    new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]))));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** 神坛有了主人（放置、加载时）：确保台账存在且主人一致，并更新估算日产。 */
    public static void onBound(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof GreekAltarBlockEntity be) || be.owner().isEmpty()) {
            return;
        }
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        RuntimeLedgers ledgers = RuntimeLedgers.get(level.getServer());
        ProducerId id = ledgerId(key);
        Optional<ProducerLedger> existing = ledgers.get(id);
        if (existing.isEmpty() || !existing.get().owner().equals(be.owner().get())) {
            ledgers.register(new ProducerLedger(id, be.owner().get(), Civilization.GREEK, estimate(be, TempleService.rules()),
                    WorldClock.now(level.getServer())));
        } else {
            refreshEstimate(be);
        }
    }

    /** 神坛被拆：注销台账，放掉祈祷位与未领产出。 */
    public static void onRemoved(ServerLevel level, GlobalPos key) {
        RuntimeLedgers.get(level.getServer()).remove(ledgerId(key));
        SLOTS.releaseAll(key);
        PENDING.remove(key);
        TODAY.remove(key);
        TODAY_BASE.remove(key);
        UNREACHABLE.remove(key);
    }

    /** 信徒数或神庙等级变化后更新台账的估算日产（快照用）。 */
    public static void refreshEstimate(GreekAltarBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level)) {
            return;
        }
        GlobalPos key = GlobalPos.of(level.dimension(), be.getBlockPos());
        RuntimeLedgers ledgers = RuntimeLedgers.get(level.getServer());
        ledgers.get(ledgerId(key)).ifPresent(l -> {
            double rate = estimate(be, TempleService.rules());
            if (Double.compare(rate, l.estimatedRatePerDay()) != 0) {
                l.setEstimatedRatePerDay(rate);
                ledgers.setDirty();
            }
        });
    }

    // ───────────── 采样 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = WorldClock.now(server);
        if (now % SAMPLE_INTERVAL != 0) {
            return;
        }
        long dt = lastSample == Long.MIN_VALUE || now <= lastSample ? SAMPLE_INTERVAL
                : Math.min(MAX_SAMPLE_GAP, now - lastSample);
        lastSample = now;
        sample(server, now, dt);
    }

    /** 只供 GameTest：立刻按 Δt = 20 刻采样一次（不影响正常采样节奏）。 */
    public static void sampleForTest(MinecraftServer server) {
        sample(server, WorldClock.now(server), SAMPLE_INTERVAL);
    }

    /** GA-4：采样只清理失效神坛的预留，不再入账（入账改在信徒祈祷满 {@link PrayerFormula#PRAY_TICKS} 刻时）。 */
    private static void sample(MinecraftServer server, long now, long dt) {
        ShrineRegistry reg = ShrineRegistry.get(server);
        for (GlobalPos key : SLOTS.altars(now)) {
            ServerLevel level = server.getLevel(key.dimension());
            if (reg.get(key).isEmpty() || level == null || !level.isLoaded(key.pos())
                    || !(level.getBlockEntity(key.pos()) instanceof GreekAltarBlockEntity)) {
                SLOTS.releaseAll(key);
            }
        }
    }

    // ───────────── 入账（GA-4） ─────────────

    /** 神坛 → {MC 日序号, 当日不含供奉的入账}：当天献供时按它补发。 */
    private static final Map<GlobalPos, double[]> TODAY_BASE = new HashMap<>();

    static double multiplier() {
        return io.github.happysnappingturtle.mythicage.config.ModConfigs.worldRules().current().greek().prayer().rateMultiplier();
    }

    /** 主人今天各希腊神坛合计的信徒数（只数已载入的神坛）。 */
    static int believersToday(MinecraftServer server, UUID owner, long today) {
        int n = 0;
        for (ShrineRegistry.Entry e : ShrineRegistry.get(server).ownedBy(owner, Civilization.GREEK)) {
            GreekAltarBlockEntity be = altarAt(server, e.pos());
            if (be != null) {
                n += be.believersToday(today);
            }
        }
        return n;
    }

    /**
     * 一名信徒在这座神坛祈祷满 {@link PrayerFormula#PRAY_TICKS} 刻：记为今日信徒并入一笔（GA-4 设计文档 §2.2）。
     * 今天已在这里计入过、神坛无主或不是希腊神坛时什么都不做，返回 false。
     */
    public static boolean credit(ServerLevel level, GlobalPos key, UUID villager) {
        MinecraftServer server = level.getServer();
        GreekAltarBlockEntity be = altarAt(server, key);
        if (be == null || be.owner().isEmpty()) {
            return false;
        }
        long today = MythDays.today(server);
        if (be.prayedHereToday(villager, today)) {
            return false;
        }
        if (be.notePrayer(villager, today)) {
            refreshEstimate(be);
        }
        UUID owner = be.owner().get();
        int n = believersToday(server, owner, today);
        TempleRules rules = TempleService.rules();
        double t = tier(be, rules).coefficient();
        double w = weather(level, be, rules);
        double r = relicBonus.applyAsDouble(owner);
        double m = multiplier();
        double base = PrayerFormula.credit(n, t, w, r, 0, m);
        double gain = PrayerFormula.credit(n, t, w, r, offering(be, today), m);
        PENDING.merge(key, gain, Double::sum);
        addToday(TODAY, key, today, gain);
        addToday(TODAY_BASE, key, today, base);
        return true;
    }

    private static void addToday(Map<GlobalPos, double[]> map, GlobalPos key, long today, double v) {
        double[] t = map.computeIfAbsent(key, k -> new double[]{today, 0});
        if (t[0] != today) {
            t[0] = today;
            t[1] = 0;
        }
        t[1] += v;
    }

    private static double todayOf(Map<GlobalPos, double[]> map, GlobalPos key, long today) {
        double[] t = map.get(key);
        return t != null && t[0] == today ? t[1] : 0;
    }

    /** 当天这座神坛的供奉加成 O。 */
    static double offering(GreekAltarBlockEntity be, long today) {
        return be.offeredOn(today) ? OfferingLoader.greek().bonus() : 0;
    }

    // ───────────── 供奉（GA-4） ─────────────

    public enum OfferResult { OK, NOT_OFFERING, NOT_ENOUGH, NOT_OWNER, ALREADY }

    /**
     * 主人手持供品献给这座神坛：扣物品、记今天已献、把今天已入账的部分按加成补一笔（GA-4 设计文档 §2.5）。
     * 不成功时不扣物品。
     */
    public static OfferResult offer(ServerPlayer player, GreekAltarBlockEntity be, ItemStack stack) {
        var table = OfferingLoader.greek();
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var offering = table.of(id);
        if (offering.isEmpty()) {
            return OfferResult.NOT_OFFERING;
        }
        if (be.owner().filter(player.getUUID()::equals).isEmpty()) {
            return OfferResult.NOT_OWNER;
        }
        MinecraftServer server = player.server;
        long today = MythDays.today(server);
        if (be.offeredOn(today)) {
            return OfferResult.ALREADY;
        }
        if (stack.getCount() < offering.get().count()) {
            return OfferResult.NOT_ENOUGH;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(offering.get().count());
        }
        be.markOffered(today);
        GlobalPos key = GlobalPos.of(be.getLevel().dimension(), be.getBlockPos());
        double back = todayOf(TODAY_BASE, key, today) * table.bonus();
        if (back > 0) {
            PENDING.merge(key, back, Double::sum);
            addToday(TODAY, key, today, back);
        }
        return OfferResult.OK;
    }

    static TempleRules.Tier tier(GreekAltarBlockEntity be, TempleRules rules) {
        return rules.tier(be.verdict(rules).grantedLevel());
    }

    /** 天气系数 W：露天祭坛（认定 0 级）且此处在下雨 → 0.5；有顶的神庙不受影响。 */
    static double weather(Level level, GreekAltarBlockEntity be, TempleRules rules) {
        return be.verdict(rules).grantedLevel() == 0 && raining.test(level, be.getBlockPos().above())
                ? PrayerFormula.RAIN_FACTOR : 1;
    }

    /** 未领产出（不清零）。 */
    public static double pending(GlobalPos altar) {
        return PENDING.getOrDefault(altar, 0.0);
    }

    static double takePending(GlobalPos altar) {
        Double v = PENDING.remove(altar);
        return v == null ? 0 : v;
    }

    // ───────────── 探针 ─────────────

    private static ProducerProbe.Observation observe(MinecraftServer server, ProducerLedger ledger, ServerPlayer owner,
                                                     long now, long interval) {
        Optional<GlobalPos> key = altarOf(ledger.id());
        if (key.isEmpty()) {
            return ProducerProbe.Observation.NOT_RUNNING;
        }
        double yield = takePending(key.get());
        ServerLevel level = server.getLevel(key.get().dimension());
        BlockPos pos = key.get().pos();
        if (level == null || !ProducerProbes.chunkRunning(level, pos)
                || !(level.getBlockEntity(pos) instanceof GreekAltarBlockEntity)) {
            return ProducerProbe.Observation.NOT_RUNNING;
        }
        boolean nearby = owner != null && owner.level().dimension().equals(key.get().dimension())
                && owner.blockPosition().closerThan(pos, OWNER_NEARBY);
        return new ProducerProbe.Observation(true, nearby, yield);
    }

    // ───────────── 统计（检视界面与信仰面板共用） ─────────────

    /**
     * 一座神坛的祈祷统计。
     *
     * @param praying     正在祈祷（已到达祈祷位）的人数，含离家太近的
     * @param believers   近 7 日信徒
     * @param today       今日已得（按采样分配，未扣恩宠上限）
     * @param estimate    预计日产（快照公式）
     * @param rainHalved  露天祭坛下雨，产出减半
     * @param tooClose    站在祈祷位上但离家太近、不计入的人数
     * @param unreachable 最近有村民走不到祈祷位
     * @param favorFull   主人的希腊恩宠已满（只在主人在线时可知）
     */
    public record Stats(int praying, int believers, double today, double estimate, boolean rainHalved, int tooClose,
                        boolean unreachable, boolean favorFull, int todayBelievers, boolean offered) {
        public static final Stats EMPTY = new Stats(0, 0, 0, 0, false, 0, false, false, 0, false);
    }

    public static Stats stats(GreekAltarBlockEntity be, TempleRules rules) {
        if (!(be.getLevel() instanceof ServerLevel level)) {
            return Stats.EMPTY;
        }
        MinecraftServer server = level.getServer();
        GlobalPos key = GlobalPos.of(level.dimension(), be.getBlockPos());
        long now = WorldClock.now(server);
        long today = MythDays.today(server);
        List<PrayerSlots.Claim> live = SLOTS.live(key, now);
        int praying = (int) live.stream().filter(PrayerSlots.Claim::arrived).count();
        int tooClose = (int) live.stream().filter(PrayerSlots.Claim::tooClose).count();
        double[] t = TODAY.get(key);
        double todayYield = t != null && t[0] == today ? t[1] : 0;
        Long gaveUp = UNREACHABLE.get(key);
        boolean full = be.owner().map(id -> PlayerActivity.onlinePlayer(server, id))
                .map(p -> MythData.get(p).civ(Civilization.GREEK).favor() >= MythData.cap(p, Civilization.GREEK))
                .orElse(false);
        return new Stats(praying, be.believers(today), todayYield, estimate(be, rules),
                weather(level, be, rules) < 1, tooClose, gaveUp != null && now - gaveUp < UNREACHABLE_MEMORY, full,
                be.believersToday(today), be.offeredOn(today));
    }

    /** 预计日产：3.5 × √min(近 7 日信徒, 24) × T × (1 + R) × M（GA-4：不再受祈祷位限制，不含供奉）。 */
    public static double estimate(GreekAltarBlockEntity be, TempleRules rules) {
        long today = be.getLevel() instanceof ServerLevel level ? MythDays.today(level.getServer()) : 0;
        double r = be.owner().map(relicBonus::applyAsDouble).orElse(0.0);
        return PrayerFormula.estimate(be.believers(today), tier(be, rules).coefficient(), r, multiplier());
    }

    static void markUnreachable(GlobalPos altar, long now) {
        UNREACHABLE.put(altar, now);
    }

    /** 只供 GameTest：直接放一条已到达的预留（不经过村民 AI）。 */
    public static void putClaimForTest(GlobalPos altar, UUID villager, BlockPos slot, boolean counted, long now) {
        SLOTS.put(altar, new PrayerSlots.Claim(villager, slot, now + 100_000, true, counted, !counted));
    }

    /** 只供 GameTest：替换下雨判定；传 null 恢复。 */
    public static void overrideRainForTest(BiPredicate<Level, BlockPos> f) {
        raining = f == null ? Level::isRainingAt : f;
    }

    /** 只供 GameTest：清掉某座神坛的预留与未领产出。 */
    public static void resetForTest(GlobalPos altar) {
        SLOTS.releaseAll(altar);
        PENDING.remove(altar);
        TODAY.remove(altar);
        TODAY_BASE.remove(altar);
        UNREACHABLE.remove(altar);
    }

    /** 被祈祷位预留了的村民（测试、调试命令）。 */
    public static List<UUID> claimants(GlobalPos altar, MinecraftServer server) {
        return SLOTS.live(altar, WorldClock.now(server)).stream().map(PrayerSlots.Claim::villager).toList();
    }

    static GreekAltarBlockEntity altarAt(MinecraftServer server, GlobalPos key) {
        ServerLevel level = server.getLevel(key.dimension());
        return level != null && level.isLoaded(key.pos()) && level.getBlockEntity(key.pos()) instanceof GreekAltarBlockEntity be
                ? be : null;
    }

    static boolean isGreekAltar(MinecraftServer server, GlobalPos key) {
        return ShrineRegistry.get(server).get(key).map(e -> e.kind().equals(GreekTemple.ALTAR_KIND.id())).orElse(false);
    }
}
