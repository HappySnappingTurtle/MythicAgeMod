package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.runtime.PublicCatchUp;
import io.github.happysnappingturtle.mythicage.core.runtime.VillageRunCondition;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.economy.CatchUpPlanner;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.core.village.time.Schedule;
import io.github.happysnappingturtle.mythicage.core.village.work.FailureBook;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.Scheduler;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.behavior.VillageBehavior;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 村庄调度驱动（T19 设计文档 §5.3、§7、§9、§14）：每村一个调度器，统一决定谁去做什么，居民实体只执行（V1 R2）。
 * <ul>
 *   <li>全服每刻最多处理 {@link #VILLAGES_PER_TICK} 个村庄（轮转），每村每 {@link #PLAN_INTERVAL} 刻规划一次；</li>
 *   <li>只在村庄<b>真实运行</b>时规划（{@link VillageRunCondition}）；从未运行回到运行时做一次离线补算（≤ 3 个 MC 日）；</li>
 *   <li>每 MC 日开始：资源点恢复、行为模块 onDayStart；晚饭段开始时扣口粮（同一 MC 日只扣一次）；</li>
 *   <li>派单前托管材料；结算成功消耗托管，失败或打断退回托管。</li>
 * </ul>
 */
public final class VillageScheduler {
    public static final int PLAN_INTERVAL = 20;
    public static final int VILLAGES_PER_TICK = 4;
    /** 全服每刻新发起的居民寻路上限（V1 §11）。 */
    public static final int NEW_PATHS_PER_TICK = 8;
    public static final double ALARM_RADIUS = 12;
    public static final int ALARM_TICKS = 200;
    /** 离开超过这么久才算「回来」，触发补算。 */
    public static final long CATCH_UP_MIN_TICKS = 200;

    /** 每村的内存状态（不存档）。 */
    public static final class Runtime {
        final FailureBook failures = new FailureBook();
        final Map<UUID, Job> active = new LinkedHashMap<>();
        /** 工作单途中的中间产物（收麦：割下来、还没打谷的麦子），工作单 id → 物品。作废时丢掉。 */
        final Map<Long, Map<String, Integer>> pending = new HashMap<>();
        /** 工作站缓存（每 MC 日或村庄重新运行时刷新）。 */
        List<Workstations.Station> stations = List.of();
        long stationsDay = Long.MIN_VALUE;
        long stationsTick = Long.MIN_VALUE / 2;
        long lastPlan = Long.MIN_VALUE / 2;
        long alarmUntil = Long.MIN_VALUE / 2;
        /** 警戒场次（T20 §5.5）：从平静进入警戒时 +1，警戒持续中不变。 */
        long alertSerial;
        boolean wasRunning;
        Boolean forceRunning;
        /** 性能统计（§1.6 H6，GameTest 用）：调度与居民执行累计耗时（纳秒）。 */
        long perfNanos;

        public long perfNanos() {
            return perfNanos;
        }

        /** 临时找地没找到地方的建筑（村务界面显示「需要空地」，§1.13 K7）；空串表示没有。 */
        String needsSpace = "";

        public String needsSpace() {
            return needsSpace;
        }

        public void setNeedsSpace(String building) {
            needsSpace = building == null ? "" : building;
        }

        /** 生活活动当天做过的（键 → MC 日；纯表现，不存档，§1.10）。 */
        final Map<String, Long> lifeDone = new HashMap<>();

        public boolean done(String key, long day) {
            Long d = lifeDone.get(key);
            return d != null && d == day;
        }

        public void markDone(String key, long day) {
            if (lifeDone.size() > 512) {
                lifeDone.values().removeIf(d -> d != day);
            }
            lifeDone.put(key, day);
        }

        /** 当天以 {@code prefix} 开头的记录有几条。 */
        public int countDone(String prefix, long day) {
            int n = 0;
            for (Map.Entry<String, Long> e : lifeDone.entrySet()) {
                if (e.getValue() == day && e.getKey().startsWith(prefix)) {
                    n++;
                }
            }
            return n;
        }

        /** 本村的岗位（职业 → 人数）与算出它的 MC 日（§1.6 H1，每日重算）。 */
        Map<String, Integer> jobs = Map.of();
        long jobsDay = Long.MIN_VALUE;
        private long jobIds = 1;

        public Map<Long, Map<String, Integer>> pending() {
            return pending;
        }

        public List<Workstations.Station> stations() {
            return stations;
        }

        public long nextJobId() {
            return jobIds++;
        }

        public List<Job> activeJobs() {
            return List.copyOf(active.values());
        }

        public boolean alarmed(long now) {
            return now < alarmUntil;
        }

        public long alertSerial() {
            return alertSerial;
        }

        /** 进入或延长警戒；返回此刻的警戒场次。 */
        long alarm(long now) {
            if (now >= alarmUntil) {
                alertSerial++;
            }
            alarmUntil = now + ALARM_TICKS;
            return alertSerial;
        }

        public FailureBook failures() {
            return failures;
        }

        public Map<String, Integer> jobsCache(long day) {
            return jobsDay == day ? jobs : null;
        }

        public void putJobs(long day, Map<String, Integer> j) {
            jobs = Map.copyOf(j);
            jobsDay = day;
        }

        /** 下一次调度重扫工作站（施工落成后）。 */
        public void invalidateStations() {
            stationsDay = Long.MIN_VALUE;
        }

        public long lastPlan() {
            return lastPlan;
        }

        /** 最近一次规划的统计（调试与 GameTest 用）：居民数、空闲数、候选数、派出数。 */
        public String lastStats() {
            return lastStats;
        }

        String lastStats = "";
        /** 各类工作单的完成 / 失败 / 作废次数（调试命令看）。 */
        final Map<String, int[]> counts = new LinkedHashMap<>();

        public String counts() {
            StringBuilder b = new StringBuilder();
            counts.forEach((k, v) -> b.append(k).append(' ').append(v[0]).append('/').append(v[1]).append('/').append(v[2]).append("  "));
            return b.toString();
        }

        void count(String type, int which) {
            counts.computeIfAbsent(type, k -> new int[3])[which]++;
        }
    }

    private static final Map<UUID, Runtime> RUNTIMES = new HashMap<>();
    /** 已加载的居民：村庄 → 居民。 */
    private static final Map<UUID, Set<ResidentEntity>> LOADED = new HashMap<>();
    private static int cursor;
    private static int newPathsThisTick;

    private VillageScheduler() {
    }

    public static Runtime runtime(UUID village) {
        return RUNTIMES.computeIfAbsent(village, k -> new Runtime());
    }

    // ───────────── 事件 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        tick(event.getServer());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        RUNTIMES.clear();
        LOADED.clear();
        Residents.clearQueue();
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ResidentEntity r) {
            r.village().ifPresent(v -> LOADED.computeIfAbsent(v, k -> new LinkedHashSet<>()).add(r));
        } else if (event.getLevel() instanceof ServerLevel sl
                && event.getEntity() instanceof net.minecraft.world.entity.npc.Villager villager) {
            Residents.maybeQueueConversion(sl, villager);
        }
    }

    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ResidentEntity r) {
            r.village().ifPresent(v -> {
                Set<ResidentEntity> s = LOADED.get(v);
                if (s != null) {
                    s.remove(r);
                }
            });
        }
    }

    public static List<ResidentEntity> loaded(UUID village) {
        Set<ResidentEntity> s = LOADED.get(village);
        if (s == null) {
            return List.of();
        }
        return s.stream().filter(r -> r.isAlive() && !r.isRemoved()).toList();
    }

    // ───────────── 主循环 ─────────────

    static void tick(MinecraftServer server) {
        newPathsThisTick = 0;
        io.github.happysnappingturtle.mythicage.village.life.PathBudget.reset();
        Residents.drainQueue(server);
        Construction.drain(server);
        List<VillagePlans.Plan> plans = VillagePlans.get(server).all();
        if (plans.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        int processed = 0;
        for (int i = 0; i < plans.size() && processed < VILLAGES_PER_TICK; i++) {
            VillagePlans.Plan p = plans.get(Math.floorMod(cursor + i, plans.size()));
            Runtime rt = runtime(p.village());
            if (now - rt.lastPlan < PLAN_INTERVAL) {
                continue;
            }
            rt.lastPlan = now;
            processed++;
            ServerLevel level = server.getLevel(p.dim());
            if (level != null) {
                try {
                    process(level, p);
                } catch (RuntimeException e) {
                    MythicAge.LOGGER.error("村庄 {} 调度出错", p.village(), e);
                }
            }
        }
        cursor = Math.floorMod(cursor + Math.max(1, processed), plans.size());
    }

    /** 处理一座村庄（GameTest 也直接调用）。返回是否真实运行。 */
    public static boolean process(ServerLevel level, VillagePlans.Plan plan) {
        long t0 = System.nanoTime();
        try {
            return processInner(level, plan);
        } finally {
            runtime(plan.village()).perfNanos += System.nanoTime() - t0;
        }
    }

    /** 居民执行工作单的耗时记到村庄上（{@code ResidentJobGoal} 调用）。 */
    public static void addPerf(UUID village, long nanos) {
        Runtime rt = RUNTIMES.get(village);
        if (rt != null) {
            rt.perfNanos += nanos;
        }
    }

    private static boolean processInner(ServerLevel level, VillagePlans.Plan plan) {
        MinecraftServer server = level.getServer();
        Optional<VillageRegistry.Village> vo = VillageRegistry.get(server).byId(plan.village());
        if (vo.isEmpty()) {
            return false;
        }
        VillageRegistry.Village village = vo.get();
        Optional<CivVillageData> data = VillageData.civ(village.civ());
        if (data.isEmpty()) {
            return false;
        }
        Runtime rt = runtime(village.id());
        VillageEconomy.State state = VillageEconomy.get(server).state(village.id());
        long now = WorldClock.now(server);
        boolean running = running(level, village, rt);
        if (!running) {
            rt.wasRunning = false;
            return false;
        }
        VillageContext ctx = context(level, village, plan, state, data.get(), rt, now);
        initialize(ctx);
        if (!rt.wasRunning && state.lastRealTick != VillageEconomy.NEVER && now - state.lastRealTick >= CATCH_UP_MIN_TICKS) {
            catchUp(ctx, now - state.lastRealTick);
            Construction.accrue(ctx, now - state.lastRealTick);
        }
        rt.wasRunning = true;
        state.lastRealTick = now;
        refreshStations(ctx);
        dayStart(ctx);
        rations(ctx);
        ResidentMigration.step(ctx);
        Construction.tick(ctx);
        io.github.happysnappingturtle.mythicage.village.life.VillageLife.tick(ctx);
        ctx.behavior().tick(ctx);
        io.github.happysnappingturtle.mythicage.village.soldier.Soldiers.plan(ctx);
        io.github.happysnappingturtle.mythicage.village.quest.QuestService.tick(ctx);
        sleepAndWake(ctx);
        plan(ctx);
        return true;
    }

    static VillageContext context(ServerLevel level, VillageRegistry.Village village, VillagePlans.Plan plan,
                                  VillageEconomy.State state, CivVillageData data, Runtime rt, long now) {
        int tod = Schedule.timeOfDay(level.getDayTime());
        long today = MythDays.today(level.getServer());
        int offset = VillageData.offset(data, state);
        return new VillageContext(level, village, plan, state, data, rt, VillageBehavior.byId(data.behavior()), now, tod,
                data.schedule().segmentAt(tod), today, data.calendar().date(today, offset), loaded(village.id()));
    }

    /** 当前（或测试指定的）真实运行状态。 */
    public static boolean isRunning(VillageContext ctx) {
        return running(ctx.level(), ctx.village(), ctx.rt());
    }

    static boolean running(ServerLevel level, VillageRegistry.Village village, Runtime rt) {
        if (rt.forceRunning != null) {
            return rt.forceRunning;
        }
        BlockPos c = village.bell().pos();
        if (!level.isLoaded(c)) {
            return false;
        }
        double nearest = Double.MAX_VALUE;
        for (Player p : level.players()) {
            if (!p.isSpectator()) {
                nearest = Math.min(nearest, Math.sqrt(p.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5)));
            }
        }
        RuntimeConfig.PublicResources cfg = ModConfigs.runtime().current().village().publicResources();
        return VillageRunCondition.realRunning(level.isPositionEntityTicking(c), nearest, cfg.realRunRadius());
    }

    /** 接管：S0 库存、登记采集点（只登记一次）；容量每次重算。 */
    static void initialize(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        s.stock.setCapacity(ctx.data().capacity(storageUnits(ctx)));
        if (s.initialized) {
            return;
        }
        anchoredPoints(ctx);
        ctx.data().initialStock().forEach(s.stock::deposit);
        natural(ctx);
        s.initialized = true;
        s.lastDay = ctx.today();
        s.log(ctx.today(), "mythic_age.village.log.founded");
    }

    /**
     * 仓储单位：每个谷仓管事位算 1（生成的谷仓）；施工建成的建筑按 {@code provides.storage} 算（大仓库 2），
     * 它自己带来的管事位不重复计（T19 §1.1 的过渡算法到阶段 ③ 改成这样）。
     */
    static int storageUnits(VillageContext ctx) {
        int units = ctx.anchors(Residents.KEEPER_ANCHOR).size();
        for (VillageEconomy.Built b : ctx.state().constructed) {
            io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec spec =
                    io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings.all().get(b.building());
            if (spec != null) {
                units += spec.provides().getOrDefault("storage", 0) - spec.anchors(Residents.KEEPER_ANCHOR).size();
            }
        }
        return Math.max(0, units);
    }

    /** 按建筑锚点登记采集点（已有的不动；数据里新加的种类在下一个 MC 日补上）。 */
    static void anchoredPoints(VillageContext ctx) {
        for (CivVillageData.PointKind k : ctx.data().points().values()) {
            List<Pos> at = ctx.anchors(k.anchor());
            for (int i = 0; i < at.size(); i++) {
                String id = k.id() + "_" + i;
                ctx.state().points.putIfAbsent(id, ResourcePoint.fresh(id, k.id(), at.get(i), k.max(), k.recover(), ctx.today()));
            }
        }
    }

    /** 天然资源：补登记林地、长回来的树恢复、缺采石点就去找（都只看已加载的格子）。 */
    static void natural(VillageContext ctx) {
        NaturalResources.refreshWoodland(ctx);
        NaturalResources.registerWoodland(ctx);
        for (CivVillageData.NaturalPoint np : ctx.data().naturalPoints().values()) {
            NaturalResources.registerNatural(ctx, np);
        }
    }

    /** 工作站缓存：每 MC 日、或缓存为空时每 200 刻重扫一次。 */
    static void refreshStations(VillageContext ctx) {
        Runtime rt = ctx.rt();
        if (rt.stationsDay != ctx.today() || (rt.stations.isEmpty() && ctx.now() - rt.stationsTick >= 200)) {
            rt.stations = Workstations.scan(ctx);
            rt.stationsDay = ctx.today();
            rt.stationsTick = ctx.now();
        }
    }

    static void dayStart(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        if (s.lastDay == ctx.today()) {
            return;
        }
        s.lastDay = ctx.today();
        s.points.replaceAll((id, p) -> p.recover(ctx.today()));
        anchoredPoints(ctx);
        natural(ctx);
        ctx.behavior().onDayStart(ctx);
        Construction.daily(ctx);
        io.github.happysnappingturtle.mythicage.village.soldier.Soldiers.dayStart(ctx);
        io.github.happysnappingturtle.mythicage.village.quest.QuestService.dayStart(ctx);
    }

    /** 晚饭段：每人 1 份（行为模块可在节庆日改为 2 份，见 {@link #rationsPerPerson}）。同一 MC 日只扣一次。 */
    static void rations(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        if (!Schedule.SUPPER.equals(ctx.segment()) || s.rationsDay == ctx.today() || s.roster.isEmpty()) {
            return;
        }
        s.rationsDay = ctx.today();
        double shortage = ctx.data().rations().consume(s.stock, s.roster.size() * rationsPerPerson(ctx));
        if (shortage > 0) {
            s.log(ctx.today(), "mythic_age.village.log.hungry", String.valueOf((int) Math.ceil(shortage)));
        }
    }

    /** 节庆日 ×2（§9.3、§1.10 J5）。 */
    static double rationsPerPerson(VillageContext ctx) {
        return io.github.happysnappingturtle.mythicage.village.life.VillageLife.rations(ctx);
    }

    static void sleepAndWake(VillageContext ctx) {
        boolean night = Schedule.SLEEP.equals(ctx.segment());
        for (ResidentEntity r : ctx.residents()) {
            if (r.soldierBusy()) {
                continue;
            }
            if (night && r.idle()) {
                r.trySleepAtHome();
            } else if (!night && r.resting()) {
                r.wakeUp();
            }
        }
    }

    // ───────────── 派单 ─────────────

    static void plan(VillageContext ctx) {
        Runtime rt = ctx.rt();
        // 已不在的居民：放掉他们的工作单记录
        Set<UUID> present = new LinkedHashSet<>();
        ctx.residents().forEach(r -> present.add(r.getUUID()));
        rt.active.keySet().removeIf(id -> !present.contains(id));
        for (ResidentEntity r : ctx.residents()) {
            if (alarmNear(r)) {
                rt.alarm(ctx.now());
                break;
            }
        }
        List<Scheduler.ResidentView> views = new ArrayList<>();
        Map<UUID, ResidentEntity> byId = new HashMap<>();
        for (ResidentEntity r : ctx.residents()) {
            if (r.inTransit()) {
                // 在途移民只做一件事：走到自己的床位（§1.6 H4），不进调度器
                if (r.idle() && newPathsThisTick < NEW_PATHS_PER_TICK) {
                    Job arrive = ResidentMigration.arriveJob(ctx, r);
                    r.assign(arrive);
                    rt.active.put(r.getUUID(), arrive);
                    newPathsThisTick++;
                }
                continue;
            }
            if (r.soldierBusy()) {
                // 值勤、补觉、接敌中的士兵由士兵 AI 接管（T20 §5.7）：不派单，警戒时也不派回屋
                continue;
            }
            byId.put(r.getUUID(), r);
            views.add(new Scheduler.ResidentView(r.getUUID(), r.profession(), VillageContext.pos(r.blockPosition()),
                    r.home() == null ? VillageContext.pos(r.blockPosition()) : VillageContext.pos(r.home()), r.idle(),
                    r.walking()));
        }
        if (views.stream().noneMatch(Scheduler.ResidentView::idle)) {
            rt.lastStats = "residents=" + views.size() + " idle=0";
            return;
        }
        List<Job> candidates = new ArrayList<>();
        for (JobType t : JobType.REGISTRY.values()) {
            candidates.addAll(t.candidates(ctx));
        }
        candidates.addAll(ctx.behavior().jobs(ctx));
        Set<String> busy = new LinkedHashSet<>();
        rt.active.values().forEach(j -> busy.add(j.targetKey()));
        Scheduler.Snapshot snap = Scheduler.Snapshot.of(views, candidates)
                .withAlarm(rt.alarmed(ctx.now()))
                .withRaining(ctx.raining())
                .withFoodShort(ctx.data().rations().belowFloor(ctx.state().stock))
                .withFailures(rt.failures, ctx.today())
                .withBusy(busy)
                .withMaxWalking(io.github.happysnappingturtle.mythicage.village.life.VillageLife.maxWalking(ctx))
                .withBehavior((r, j) -> ctx.behavior().available(ctx, r, j),
                        j -> io.github.happysnappingturtle.mythicage.village.life.VillageLife.filter(ctx, j)
                                .flatMap(k -> ctx.behavior().modify(ctx, k)));
        List<Scheduler.Assignment> plan = Scheduler.plan(snap);
        int assigned = 0;
        for (Scheduler.Assignment a : plan) {
            ResidentEntity r = byId.get(a.resident());
            Job j = a.job();
            if (r == null || (j.needsWalk() && newPathsThisTick >= NEW_PATHS_PER_TICK)) {
                continue;
            }
            JobType type = JobType.REGISTRY.get(j.type());
            Optional<Map<String, Integer>> escrow = type == null ? Optional.of(Map.of()) : type.escrow(ctx, j);
            if (escrow.isEmpty() || !ctx.state().stock.reserve(j.escrowId(), escrow.get())) {
                continue;
            }
            j = io.github.happysnappingturtle.mythicage.village.build.BuildJob.withBankedTicks(ctx, j);
            r.assign(j);
            rt.active.put(r.getUUID(), j);
            assigned++;
            if (j.needsWalk()) {
                newPathsThisTick++;
            }
        }
        rt.lastStats = "residents=" + views.size() + " candidates=" + candidates.size() + " planned=" + plan.size()
                + " assigned=" + assigned + " alarm=" + rt.alarmed(ctx.now()) + " segment=" + ctx.segment();
    }

    private static boolean alarmNear(ResidentEntity r) {
        return !r.level().getEntitiesOfClass(LivingEntity.class, r.getBoundingBox().inflate(ALARM_RADIUS),
                e -> e instanceof Enemy && e.isAlive()).isEmpty();
    }

    /** 居民走完最后一步：按类型结算；成功消耗托管，失败退回并记目标失败。 */
    public static void onJobSettle(ServerLevel level, ResidentEntity resident, Job job) {
        Optional<VillageContext> ctx = contextOf(level, resident);
        if (ctx.isEmpty()) {
            return;
        }
        ctx.get().rt().active.remove(resident.getUUID());
        JobType type = JobType.REGISTRY.get(job.type());
        boolean ok = type == null || type.settle(ctx.get(), resident, job);
        ctx.get().rt().pending.remove(job.id());
        VillageEconomy.State s = ctx.get().state();
        ctx.get().rt().count(job.type(), ok ? 0 : 1);
        if (ok) {
            s.stock.consume(job.escrowId());
        } else {
            s.stock.release(job.escrowId());
            ctx.get().rt().failures.fail(job.targetKey(), ctx.get().today());
        }
        VillageEconomy.get(level.getServer()).setDirty();
    }

    /** 动作进行中的每一刻：交给类型（祈祷按刻报到）。 */
    public static void onActTick(ServerLevel level, ResidentEntity resident, Job job, int step, int ticks) {
        JobType type = JobType.REGISTRY.get(job.type());
        if (type != null) {
            type.onActTick(level, resident, job, step, ticks);
        }
    }

    /** 走路中每 20 刻：交给类型（祈祷续预留）。 */
    public static void onWalkTick(ServerLevel level, ResidentEntity resident, Job job, int step) {
        JobType type = JobType.REGISTRY.get(job.type());
        if (type != null) {
            type.onWalkTick(level, resident, job, step);
        }
    }

    /** 一段动作做完：交给类型处理中间效果。 */
    public static void onActDone(ServerLevel level, ResidentEntity resident, Job job, int step) {
        JobType type = JobType.REGISTRY.get(job.type());
        if (type != null) {
            contextOf(level, resident).ifPresent(ctx -> type.onActDone(ctx, resident, job, step));
        }
    }

    /** 工作单被打断：托管退回，不产出。 */
    public static void onJobAborted(ServerLevel level, ResidentEntity resident, Job job, boolean countFailure) {
        resident.village().ifPresent(v -> {
            Runtime rt = runtime(v);
            rt.active.remove(resident.getUUID());
            rt.count(job.type(), 2);
            JobType type = JobType.REGISTRY.get(job.type());
            if (type != null) {
                type.onAbort(rt, job);
            }
            rt.pending.remove(job.id());
            VillageEconomy.get(level.getServer()).peek(v).ifPresent(s -> s.stock.release(job.escrowId()));
            VillageEconomy.get(level.getServer()).setDirty();
            if (countFailure) {
                rt.failures.fail(job.targetKey(), MythDays.today(level.getServer()));
            }
        });
    }

    public static void onResidentDied(ServerLevel level, ResidentEntity resident) {
        resident.village().ifPresent(v -> VillageEconomy.get(level.getServer()).peek(v).ifPresent(s -> {
            s.roster.remove(resident.getUUID());
            io.github.happysnappingturtle.mythicage.village.soldier.Soldiers.onDied(s, resident);
            ResidentMigration.onDied(s, resident, MythDays.today(level.getServer()));
            io.github.happysnappingturtle.mythicage.village.quest.QuestService.onResidentDied(level.getServer(), v, resident.getUUID(),
                    MythDays.today(level.getServer()));
            VillageEconomy.get(level.getServer()).setDirty();
        }));
    }

    public static void onInteract(ServerPlayer player, ResidentEntity resident) {
        VillageAffairs.onResidentInteract(player, resident);
    }

    /** 村庄此刻的上下文（施工落段用）。 */
    public static Optional<VillageContext> contextOf(ServerLevel level, UUID village) {
        return contextForTest(level, village);
    }

    static Optional<VillageContext> contextOf(ServerLevel level, ResidentEntity resident) {
        Optional<UUID> v = resident.village();
        if (v.isEmpty()) {
            return Optional.empty();
        }
        MinecraftServer server = level.getServer();
        Optional<VillageRegistry.Village> village = VillageRegistry.get(server).byId(v.get());
        Optional<VillagePlans.Plan> plan = VillagePlans.get(server).byVillage(v.get());
        if (village.isEmpty() || plan.isEmpty()) {
            return Optional.empty();
        }
        Optional<CivVillageData> data = VillageData.civ(village.get().civ());
        if (data.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(context(level, village.get(), plan.get(), VillageEconomy.get(server).state(v.get()), data.get(),
                runtime(v.get()), WorldClock.now(server)));
    }

    // ───────────── 离线补算 ─────────────

    /** 回到真实运行时的一次补算（T19 设计文档 §7）。 */
    static void catchUp(VillageContext ctx, long awayTicks) {
        RuntimeConfig.PublicResources cfg = ModConfigs.runtime().current().village().publicResources();
        long ticks = PublicCatchUp.ticksToSimulate(ctx.now() - awayTicks, ctx.now(), true, cfg);
        if (ticks <= 0) {
            return;
        }
        VillageEconomy.State s = ctx.state();
        CatchUpPlanner.Result r = CatchUpPlanner.plan(s.stock, s.points, producers(ctx), ticks / 24000.0,
                cfg.maxCatchUpTicks() / 24000.0, ctx.today(), s.roster.size(), 1, ctx.data().rations());
        s.points.clear();
        s.points.putAll(r.points());
        // 记录写收得最多的一样（物品名由客户端翻译）和种数，不写物品 id（T24 界面文本）
        String days = String.format(java.util.Locale.ROOT, "%.1f", r.days());
        r.gained().entrySet().stream().max(Map.Entry.comparingByValue()).ifPresentOrElse(
                top -> s.log(ctx.today(), "mythic_age.village.log.catch_up_items", days,
                        "@" + io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs.itemKeyOf(top.getKey()),
                        String.valueOf(top.getValue()), String.valueOf(r.gained().size())),
                () -> s.log(ctx.today(), "mythic_age.village.log.catch_up_none", days));
        MythicAge.LOGGER.debug("村庄 {} 离线补算 {} 日：{}", ctx.village().id(), r.days(), r.gained());
    }

    /** 岗位的日产率：采集点、作物、工作站配方。只算名册里有人能做的。 */
    static List<CatchUpPlanner.Producer> producers(VillageContext ctx) {
        Set<String> professions = new LinkedHashSet<>();
        ctx.state().roster.values().forEach(m -> professions.add(m.profession()));
        List<CatchUpPlanner.Producer> out = new ArrayList<>();
        for (ResourcePoint p : ctx.state().points.values()) {
            CivVillageData.PointKind k = ctx.data().points().get(p.kind());
            if (k != null && staffed(k.professions(), professions)) {
                out.add(new CatchUpPlanner.Producer("point:" + p.id(), Map.of(), k.outputs(), k.perDay(), p.id(), k.take()));
            }
        }
        for (CivVillageData.CropKind k : ctx.data().crops().values()) {
            int n = ctx.anchors(k.id()).size();
            if (n > 0 && staffed(k.professions(), professions)) {
                out.add(new CatchUpPlanner.Producer("crop:" + k.id(), Map.of(), k.expected(), n * k.perAnchorPerDay(), null, 0));
            }
        }
        for (CivVillageData.FruitKind k : ctx.data().fruits().values()) {
            int n = ctx.anchors(k.id()).size();
            if (n > 0 && staffed(k.professions(), professions)) {
                out.add(new CatchUpPlanner.Producer("fruit:" + k.id(), Map.of(), k.expected(), n * k.perAnchorPerDay(), null, 0));
            }
        }
        CivVillageData.Woodland w = ctx.data().woodland();
        if (w != null && staffed(w.professions(), professions)) {
            long trees = ctx.state().points.values().stream()
                    .filter(p -> p.kind().equals(NaturalResources.WOODLAND) && p.remaining() > 0).count();
            String log = woodlandLog(ctx);
            if (trees > 0 && log != null) {
                // 补算不改方块：按「每日能伐几棵 × 每棵几块原木」估，不超过登记的株数
                out.add(new CatchUpPlanner.Producer("woodland", Map.of(), Map.of(log, w.logsPerTree()),
                        Math.min(trees, w.perDay()), null, 0));
            }
        }
        Map<String, Integer> stationsByKind = new LinkedHashMap<>();
        for (Workstations.Station st : BasicJobTypes.stations(ctx)) {
            stationsByKind.merge(st.id(), 1, Integer::sum);
        }
        String fuelItem = ctx.data().fuel().perUnit().entrySet().stream()
                .max(java.util.Comparator.comparingInt(e -> ctx.state().stock.count(e.getKey()) / Math.max(1, e.getValue())))
                .map(Map.Entry::getKey).orElse(null);
        // 能开工的原料：库存里有的，加上采集、收割、采果、伐木会产出的
        Set<String> available = new LinkedHashSet<>();
        ctx.state().stock.items().forEach((k, v) -> {
            if (v > 0) {
                available.add(k);
            }
        });
        out.forEach(pr -> available.addAll(pr.outputs().keySet()));
        List<CatchUpPlanner.Producer> recipes = new ArrayList<>();
        for (Map.Entry<String, Integer> st : stationsByKind.entrySet()) {
            List<VillageRecipe> rs = ctx.data().recipes().values().stream()
                    .filter(r -> r.station().equals(st.getKey()) && !r.unattended() && BasicJobTypes.stageOpen(ctx, r.stage())
                            && staffed(r.professions(), professions)
                            && (available.containsAll(r.inputs().keySet()) || ctx.data().recipes().values().stream()
                                    .anyMatch(o -> o != r && r.inputs().keySet().stream().anyMatch(o.outputs()::containsKey))))
                    .toList();
            for (VillageRecipe r : rs) {
                Map<String, Integer> in = new LinkedHashMap<>(r.inputs());
                if (r.fuel() > 0) {
                    if (fuelItem == null) {
                        continue;
                    }
                    in.merge(fuelItem, r.fuel() * ctx.data().fuel().perUnit().get(fuelItem), Integer::sum);
                }
                double perDay = Math.min(8, 11000 * 0.6 / Math.max(20, r.totalTicks())) * st.getValue() / rs.size();
                Map<String, Integer> caps = r.stockTarget() > 0 ? Map.of(r.mainOutput(), r.stockTarget()) : Map.of();
                recipes.add(new CatchUpPlanner.Producer("recipe:" + r.id(), in, r.outputs(), perDay, null, 0, caps));
            }
        }
        // 口粮先做：同一种原料（橄榄）先给烤饼，再给压榨（V1 §3：恢复口粮优先于其他生产）
        recipes.sort(java.util.Comparator.comparingInt(pr -> pr.outputs().keySet().stream()
                .anyMatch(ctx.data().rations()::isFood) ? 0 : 1));
        out.addAll(recipes);
        return out;
    }

    /** 登记林地里最常见的原木（补算产物用）。 */
    static String woodlandLog(VillageContext ctx) {
        Map<String, Integer> count = new LinkedHashMap<>();
        for (ResourcePoint p : ctx.state().points.values()) {
            BlockPos b = VillageContext.block(p.pos());
            if (p.kind().equals(NaturalResources.WOODLAND) && ctx.loaded(b)) {
                var st = ctx.level().getBlockState(b);
                if (st.is(net.minecraft.tags.BlockTags.LOGS)) {
                    count.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getBlock().asItem()).toString(), 1,
                            Integer::sum);
                }
            }
        }
        return count.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private static boolean staffed(Set<String> need, Set<String> have) {
        return need.isEmpty() ? !have.isEmpty() : need.stream().anyMatch(have::contains);
    }

    // ───────────── 测试 ─────────────

    /** 士兵发现敌人：进入或延长警戒（T20 §5.5）。返回警戒场次。 */
    public static long raiseAlarm(UUID village, long now) {
        return runtime(village).alarm(now);
    }

    /** 只供 GameTest：让村庄警戒到某一刻（对话优先级）。 */
    public static void alarmForTest(UUID village, long until) {
        runtime(village).alarmUntil = until;
    }

    /** 只供 GameTest：指定村庄是否真实运行（null 恢复按世界判断）。 */
    public static void forceRunningForTest(UUID village, Boolean running) {
        runtime(village).forceRunning = running;
    }

    /** 只供 GameTest：立刻处理一次（不等轮转）。 */
    public static boolean processNowForTest(ServerLevel level, UUID village) {
        Optional<VillagePlans.Plan> p = VillagePlans.get(level.getServer()).byVillage(village);
        runtime(village).lastPlan = level.getServer().overworld().getGameTime();
        newPathsThisTick = 0;
        return p.isPresent() && process(level, p.get());
    }

    /** 只供 GameTest：当前的村庄上下文。 */
    public static Optional<VillageContext> contextForTest(ServerLevel level, UUID village) {
        Optional<VillagePlans.Plan> p = VillagePlans.get(level.getServer()).byVillage(village);
        Optional<VillageRegistry.Village> v = VillageRegistry.get(level.getServer()).byId(village);
        Optional<CivVillageData> d = v.flatMap(x -> VillageData.civ(x.civ()));
        if (p.isEmpty() || d.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(context(level, v.get(), p.get(), VillageEconomy.get(level.getServer()).state(village), d.get(),
                runtime(village), WorldClock.now(level.getServer())));
    }

    /** 只供 GameTest（TC-T19-53）：按晚饭段扣一次口粮。 */
    public static void supperForTest(ServerLevel level, UUID village) {
        VillagePlans.Plan p = VillagePlans.get(level.getServer()).byVillage(village).orElseThrow();
        VillageRegistry.Village v = VillageRegistry.get(level.getServer()).byId(village).orElseThrow();
        CivVillageData data = VillageData.civ(v.civ()).orElseThrow();
        VillageContext c = context(level, v, p, VillageEconomy.get(level.getServer()).state(village), data, runtime(village),
                WorldClock.now(level.getServer()));
        rations(new VillageContext(c.level(), c.village(), c.plan(), c.state(), c.data(), c.rt(), c.behavior(), c.now(),
                12000, Schedule.SUPPER, c.today(), c.date(), c.residents()));
    }

    public static void clearForTest(UUID village) {
        RUNTIMES.remove(village);
        LOADED.remove(village);
    }
}
