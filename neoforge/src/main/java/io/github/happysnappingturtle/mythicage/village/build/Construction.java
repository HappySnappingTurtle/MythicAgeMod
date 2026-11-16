package io.github.happysnappingturtle.mythicage.village.build;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPacing;
import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.runtime.ConstructionBank;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildSegments;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectQueue;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 五文明共用施工协调：项目备料、逐格落块、材料结算、遇阻复查与验收。
 * 工人保留段级工单，但每次提交只尝试一格；服务器配额覆盖蓝图操作及脚手架。
 * 段位图兼容旧消费者，格位图保存部分完成进度。临时标识与脚手架均归项目所有。
 */
public final class Construction {
    public static final int MAX_BUILDERS = 3;
    /** 每个非空气格的锤打时长。 */
    public static final int TICKS_PER_BLOCK = 15;
    public static final int MIN_TICKS = 20;
    public static final int NOTICE_RANGE = 64;
    public static final int NOTICE_LIMIT = 8;
    /** 单格变更：同步客户端，邻接形状由受控修正处理，保留尚未放齐的门/床两半。 */
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public enum Result { PLACED, WAIT, DEFERRED, BLOCKED, SHORT, GONE }

    record Drop(UUID village, long project, int seg) {
    }

    private static final Deque<Drop> QUEUE = new ArrayDeque<>();
    private static final Set<Drop> QUEUED = new LinkedHashSet<>();
    private static final Map<String, BuildSite> SITES = new ConcurrentHashMap<>();
    /** 只供 GameTest：每次落段的结果（村庄 → 最近一次）。 */
    private static final Map<UUID, Result> LAST = new HashMap<>();

    private static final Map<MinecraftServer, BuildPacing.Budget> BUDGETS = new java.util.WeakHashMap<>();
    private static final Map<UUID, Double> SPEEDS = new HashMap<>();
    private static final Map<String, Long> RECHECK = new HashMap<>();

    public static double speed(UUID village) { return SPEEDS.getOrDefault(village, 1.0); }
    public static void speed(UUID village, double value) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Invalid construction speed");
        SPEEDS.put(village, value);
    }

    static boolean takeBudget(VillageContext ctx) {
        return BUDGETS.computeIfAbsent(ctx.level().getServer(), k -> new BuildPacing.Budget())
                .take(ctx.level().getServer().getTickCount(), limit(ctx));
    }

    static int limit(VillageContext ctx) {
        return Math.min(ctx.data().construction().globalPerTick(),
                io.github.happysnappingturtle.mythicage.village.work.VillageData.constructionLimit());
    }

    private Construction() {
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        QUEUE.clear();
        QUEUED.clear();
        SITES.clear();
        LAST.clear();
        BUDGETS.clear(); SPEEDS.clear(); RECHECK.clear();
        BuildJob.clear(); BuildSite.clearAttachments(); VillageProjects.clearCache();
    }

    static String key(UUID village, long project) {
        return village + ":" + project;
    }

    // ───────────── 工地 ─────────────

    /** 项目的工地（缓存；重启后按蓝图重新算）。 */
    public static Optional<BuildSite> site(VillageContext ctx, BuildProject p) {
        String k = key(ctx.village().id(), p.id);
        BuildSite s = SITES.get(k);
        if (s != null) {
            return Optional.of(s);
        }
        BuildingSpec spec = VillageBuildings.all().get(p.building);
        if (spec == null) {
            return Optional.empty();
        }
        BuildPlacement.Placed at = placed(spec, p.origin, p.turn);
        Optional<BuildSite> made;
        if (p.replaces.isEmpty()) {
            made = BuildSite.create(ctx.level(), ctx.data().buildCosts(), spec, at, p.tripSize, p.layoutVersion == 0);
        } else {
            BuildingSpec old = VillageBuildings.all().get(p.replaces);
            if (old == null || p.oldOrigin == null) {
                return Optional.empty();
            }
            made = BuildSite.replace(ctx.level(), ctx.data().buildCosts(), spec, at, old, placed(old, p.oldOrigin, p.oldTurn), p.tripSize, p.layoutVersion);
        }
        made.ifPresent(m -> {
            if (p.segments != 0 && p.segments != m.segments.size()) {
                MythicAge.LOGGER.warn("村庄 {} 的施工项目 {}：蓝图段数变了（{} → {}），按新的继续", ctx.village().id(), p.id,
                        p.segments, m.segments.size());
                p.segments = m.segments.size();
            }
            p.migrateCells(m.segments);
            SITES.put(k, m);
        });
        return made;
    }

    public static BuildPlacement.Placed placed(BuildingSpec s, Pos origin, io.github.happysnappingturtle.mythicage.core.village.gen.Turn t) {
        return new BuildPlacement.Placed(s.id(), origin, t, TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), t));
    }

    // ───────────── 选项目 ─────────────

    /** 每 MC 日开始调用一次（§8.2）。返回新开的项目。 */
    public static Optional<BuildProject> daily(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        boolean busy = s.activeProject().isPresent();
        // 奠基村庄还没有住居：第一个项目固定是起步住居（远古也开工，§1.13 K7）
        String starter = ctx.data().foundation().starter();
        if (ctx.village().origin() == io.github.happysnappingturtle.mythicage.village.VillageRegistry.Origin.FOUNDED
                && ctx.anchors("home").isEmpty() && !busy && !ctx.data().rations().belowFloor(s.stock) && !starter.isEmpty()
                && specs.containsKey(starter)) {
            return AdHocSites.start(ctx, new ProjectPicker.Pick(starter, "", "", ""));
        }
        return advance(ctx, true);
    }

    /**
     * 按项目队列开下一个项目（GA-11 §2.2）：黎明、当前项目落成或取消、玩家点名时调用。
     * 「下一个」位空着、黎明、没有能放下的预留地块时，照 T19 临时找地。
     */
    public static Optional<BuildProject> advance(VillageContext current, boolean dawn) {
        // 落成刚改过规划（旧建筑的包围盒、锚点）：取一份新的上下文，免得把刚升级完的那座又当成旧建筑
        VillageContext ctx = VillageScheduler.contextOf(current.level(), current.village().id()).orElse(current);
        Optional<ProjectPicker.Input> in = input(ctx);
        if (in.isEmpty()) {
            return Optional.empty();
        }
        VillageEconomy.State s = ctx.state();
        ProjectQueue.Decision d = ProjectQueue.decide(in.get(), Optional.ofNullable(s.nextProject), dawn);
        ProjectQueue.Next next = s.nextProject;
        if (d.dropNext()) {
            s.nextProject = null;
            VillageEconomy.get(ctx.level().getServer()).setDirty();
            if (!d.fromNext() && next != null) {
                VillageProjects.dropped(ctx, next, d.dropReason());
            }
        }
        if (d.pick().isPresent()) {
            Optional<BuildProject> p = start(ctx, d.pick().get());
            if (d.fromNext() && next != null) {
                if (p.isPresent()) {
                    VillageProjects.started(ctx, next);
                } else {
                    VillageProjects.dropped(ctx, next, ProjectQueue.GONE);
                }
            }
            if (p.isPresent()) {
                ctx.rt().setNeedsSpace("");
                return p;
            }
        }
        if (!dawn || in.get().busy() || s.nextProject != null) {
            return Optional.empty();
        }
        // 想建、但没有能放下它的预留地块：临时找地（奠基、立原版村庄的一切项目，生成村庄的体育场等）
        return ProjectPicker.wanted(in.get()).flatMap(want -> AdHocSites.start(ctx, want));
    }

    /** 选项目的输入（GA-11：黎明、落成、点名、候选列表共用）。本文明没有施工建筑时为空。 */
    public static Optional<ProjectPicker.Input> input(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        String civ = ctx.village().civ();
        List<BuildingSpec> candidates = specs.values().stream()
                .filter(b -> b.civ().equals(civ) && !b.generated() && b.patron().isEmpty()).toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Set<String> built = builtIds(s, specs);
        Set<String> gods = new LinkedHashSet<>();
        s.constructed.forEach(b -> {
            if (!b.god().isEmpty()) {
                gods.add(b.god());
            }
        });
        List<ProjectPicker.ShrineRequest> shrines = new ArrayList<>();
        new TreeMap<>(ctx.village().shrines()).forEach((age, list) -> list.forEach(g -> shrines.add(new ProjectPicker.ShrineRequest(age, g))));
        Set<String> satisfied = new LinkedHashSet<>();
        ctx.plan().info().anchors().forEach((k, v) -> {
            if (!v.isEmpty()) {
                satisfied.add("anchor:" + k);
            }
        });
        built.forEach(b -> satisfied.add("building:" + b));
        Set<String> inProgress = new LinkedHashSet<>();
        s.projects.stream().filter(BuildProject::active).forEach(p -> {
            BuildingSpec spec = specs.get(p.building);
            inProgress.add(ProjectPicker.key(spec == null ? p.building : spec.civ() + ":" + spec.shortBase(), p.god));
        });
        boolean busy = s.activeProject().isPresent();
        boolean hungry = ctx.data().rations().belowFloor(s.stock);
        return Optional.of(new ProjectPicker.Input(ctx.village().stage(), busy, hungry, candidates, freePlots(ctx), built,
                gods, shrines, satisfied, existing(ctx, candidates, specs), inProgress, fit(ctx, specs)));
    }
    /** 本村已有的施工建筑 id（含去掉主神后缀的基础 id）。 */
    static Set<String> builtIds(VillageEconomy.State s, Map<String, BuildingSpec> specs) {
        Set<String> out = new LinkedHashSet<>();
        for (VillageEconomy.Built b : s.constructed) {
            out.add(b.building());
            BuildingSpec spec = specs.get(b.building());
            if (spec != null) {
                out.add(spec.civ() + ":" + spec.shortBase());
            }
        }
        return out;
    }

    /** 空地块：未建成、没被玩家占用、没有进行中的项目。 */
    static List<ProjectPicker.Plot> freePlots(VillageContext ctx) {
        VillageEconomy.State s = ctx.state();
        Map<String, BlockPos> blocked = VillagePlans.get(ctx.level().getServer()).blocked(ctx.village().id());
        Set<String> busy = new LinkedHashSet<>();
        s.projects.stream().filter(BuildProject::active).forEach(p -> busy.add(p.plot));
        List<ProjectPicker.Plot> out = new ArrayList<>();
        for (VillagePlan.Plot pl : ctx.plan().info().plots()) {
            if (s.builtPlots.contains(pl.id()) || blocked.containsKey(pl.id()) || busy.contains(pl.id())
                    || !groundOk(ctx.level(), pl.box())) {
                continue;
            }
            out.add(new ProjectPicker.Plot(pl.id(), pl.kind(), Age.byId(pl.stage()).orElse(Age.MYTHIC), pl.box().sizeX(),
                    pl.box().sizeZ(), pl.front()));
        }
        return out;
    }

    /**
     * 地块的地面能不能盖房：逐列（每 2 格）看地面下一格是实心方块、地面这一格不是水；九成以上合格才算。
     * 没加载的地块不算（等加载了再选）。生成时有的地块落在了湖面上（实机 ma_t19b），这样的地块留给阶段 ⑥ 的临时找地。
     */
    public static boolean groundOk(ServerLevel level, Box b) {
        int ok = 0;
        int all = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = b.minX(); x <= b.maxX(); x += 2) {
            for (int z = b.minZ(); z <= b.maxZ(); z += 2) {
                if (!level.isLoaded(m.set(x, b.minY(), z))) {
                    return false;
                }
                all++;
                BlockPos under = new BlockPos(x, b.minY() - 1, z);
                BlockState below = level.getBlockState(under);
                BlockState at = level.getBlockState(m);
                if (below.isCollisionShapeFullBlock(level, under) && below.getFluidState().isEmpty() && at.getFluidState().isEmpty()) {
                    ok++;
                }
            }
        }
        return all > 0 && ok * 10 >= all * 9;
    }

    /** 能被原地替换的旧建筑 id：施工建成的直接查；生成的按锚点反推（§1.4 E2）。 */
    static Set<String> replaceable(VillageContext ctx, List<BuildingSpec> candidates, Map<String, BuildingSpec> specs) {
        Set<String> out = new LinkedHashSet<>();
        existing(ctx, candidates, specs).forEach(e -> out.add(e.building()));
        return out;
    }

    /**
     * 能被原地升级的旧建筑，每座一行（GA-11）：施工建成的直接取位置；生成的按锚点找出全部（同种的住居有好几座）。
     * 正在被进行中的项目替换的那一座不算。
     */
    static List<ProjectPicker.Existing> existing(VillageContext ctx, List<BuildingSpec> candidates, Map<String, BuildingSpec> specs) {
        Set<String> prefixes = new LinkedHashSet<>();
        candidates.forEach(c -> prefixes.addAll(c.replace()));
        List<ProjectPicker.Existing> out = new ArrayList<>();
        if (prefixes.isEmpty()) {
            return out;
        }
        Set<Pos> replacing = new java.util.HashSet<>();
        ctx.state().projects.stream().filter(p -> p.active() && p.oldOrigin != null).forEach(p -> replacing.add(p.oldOrigin));
        for (VillageEconomy.Built b : ctx.state().constructed) {
            if (prefixes.stream().anyMatch(b.building()::startsWith) && !replacing.contains(b.origin())) {
                out.add(new ProjectPicker.Existing(b.building(), b.placed()));
            }
        }
        for (BuildingSpec s : specs.values()) {
            if (s.generated() && prefixes.stream().anyMatch(s.id()::startsWith)) {
                for (BuildPlacement.Placed at : BuildPlacement.locateAll(s, ctx.plan().info().anchors(), ctx.plan().info().built())) {
                    if (!replacing.contains(at.origin())) {
                        out.add(new ProjectPicker.Existing(s.id(), at));
                    }
                }
            }
        }
        return out;
    }

    /** 原位判断（GA-11 §2.4）：多出来的格子不压别的建筑与地块，地面不是水。 */
    static ProjectPicker.Fit fit(VillageContext ctx, Map<String, BuildingSpec> specs) {
        List<Box> others = ctx.plan().info().built();
        List<io.github.happysnappingturtle.mythicage.core.village.build.UpgradeFit.PlotArea> plots = ctx.plan().info().plots().stream()
                .map(p -> new io.github.happysnappingturtle.mythicage.core.village.build.UpgradeFit.PlotArea(p.kind(), p.box())).toList();
        ServerLevel level = ctx.level();
        return (next, old) -> {
            BuildingSpec oldSpec = specs.get(old.building());
            if (oldSpec == null || old.at() == null) {
                return "";
            }
            return io.github.happysnappingturtle.mythicage.core.village.build.UpgradeFit.check(next, oldSpec, old.at(), others, plots,
                    p -> {
                        BlockPos at = new BlockPos(p.x(), p.y(), p.z());
                        return level.isLoaded(at) && (!level.getFluidState(at).isEmpty() || !level.getFluidState(at.below()).isEmpty());
                    });
        };
    }

    /** 开一个项目（选项目、开发命令、GameTest）：算位置、切段、账单，进入备料。 */
    public static Optional<BuildProject> start(VillageContext ctx, ProjectPicker.Pick pick) {
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        BuildingSpec base = specs.get(pick.building());
        if (base == null) {
            return Optional.empty();
        }
        BuildingSpec spec = BuildingSpec.forPatron(specs, base.civ(), base.shortBase(), ctx.village().patron());
        VillageEconomy.State s = ctx.state();
        BuildPlacement.Placed at;
        BuildPlacement.Placed old = null;
        if (!pick.replaces().isEmpty()) {
            BuildingSpec oldSpec = specs.get(pick.replaces());
            if (oldSpec == null) {
                return Optional.empty();
            }
            Pos want = pick.replaceAt();
            old = s.constructed.stream().filter(b -> b.building().equals(pick.replaces()) && (want == null || b.origin().equals(want)))
                    .map(VillageEconomy.Built::placed).findFirst()
                    .or(() -> BuildPlacement.locateAll(oldSpec, ctx.plan().info().anchors(), ctx.plan().info().built()).stream()
                            .filter(x -> want == null || x.origin().equals(want)).findFirst())
                    .orElse(null);
            if (old == null) {
                return Optional.empty();
            }
            at = BuildPlacement.replacing(spec, oldSpec, old);
        } else {
            Optional<VillagePlan.Plot> plot = ctx.plan().info().plots().stream().filter(p -> p.id().equals(pick.plot())).findFirst();
            if (plot.isEmpty()) {
                return Optional.empty();
            }
            at = BuildPlacement.onPlot(spec, plot.get().box(), plot.get().front());
        }
        BuildProject p = new BuildProject(s.nextProjectId++, spec.id(), pick.plot(), pick.god(), pick.replaces(), at.origin(),
                at.turn(), old == null ? null : old.origin(), old == null ? null : old.turn(), ctx.today());
        p.tripSize = ctx.data().construction().tripSize();
        Optional<BuildSite> site = site(ctx, p);
        if (site.isEmpty()) {
            return Optional.empty();
        }
        p.segments = site.get().segments.size();
        io.github.happysnappingturtle.mythicage.core.village.build.BuildCosts.add(p.planned, site.get().totalCost());
        s.projects.add(p);
        s.log(ctx.today(), "mythic_age.village.log.project", nameArg(spec, p.god));
        VillageEconomy.get(ctx.level().getServer()).setDirty();
        return Optional.of(p);
    }

    // ───────────── 每次调度 ─────────────

    /** 每次调度时调用：备料、恢复停工、开工。 */
    public static void tick(VillageContext ctx) {
        BuildPerformance.begin();
        try { tickMeasured(ctx); } finally { BuildPerformance.end(); }
    }

    private static void tickMeasured(VillageContext ctx) {
        for (BuildProject p : List.copyOf(ctx.state().projects)) {
            if (!p.active()) {
                continue;
            }
            Optional<BuildSite> site = site(ctx, p);
            if (site.isEmpty()) {
                continue;
            }
            p.stockUp(ctx.state().stock, site.get().costs);
            if (p.state == BuildProject.State.STOCKING && p.missing().isEmpty()) p.state = BuildProject.State.BUILDING;
            // Missing cells are reconsidered after stock arrives; blocked cells only every 600 ticks.
            for (var entry : new ArrayList<>(p.missingCells.entrySet())) {
                var seg = site.get().segments.get(entry.getKey());
                for (int i = entry.getValue().nextSetBit(0); i >= 0; i = entry.getValue().nextSetBit(i + 1)) {
                    if (p.canPay(site.get().costs.cost(seg.cells().get(i).block()))) BuildProject.mark(p.missingCells, seg.index(), i, false);
                }
            }
            String id = key(ctx.village().id(), p.id);
            long now = ctx.level().getGameTime();
            if (now >= RECHECK.getOrDefault(id, 0L)) {
                RECHECK.put(id, now + 600);
                for (var entry : new ArrayList<>(p.blockedCells.entrySet())) {
                    var seg = site.get().segments.get(entry.getKey());
                    for (int i = entry.getValue().nextSetBit(0); i >= 0; i = entry.getValue().nextSetBit(i + 1)) {
                        var cell = seg.cells().get(i);
                        var at = VillageContext.block(cell.pos());
                        if (ctx.loaded(at) && acceptable(ctx.level().getBlockState(at), site.get().state(seg, cell), cell,
                                seg.kind() == BuildSegments.Kind.PLACE)) BuildProject.mark(p.blockedCells, seg.index(), i, false);
                    }
                }
            }
            if (p.state == BuildProject.State.BLOCKED && hasWork(p, site.get())) p.resume();
            var trunks = p.cleanupCells;
            if (!trunks.isEmpty()) {
                Pos pending = trunks.iterator().next();
                BlockPos cut = VillageContext.block(pending);
                if (ctx.loaded(cut) && BUDGETS.computeIfAbsent(ctx.level().getServer(), k -> new BuildPacing.Budget())
                        .take(ctx.level().getServer().getTickCount(), limit(ctx))) {
                    if (ctx.level().getBlockState(cut).is(BlockTags.LOGS) && !PlayerConstructionEdits.contains(ctx.level(), cut))
                        ctx.level().setBlock(cut, Blocks.AIR.defaultBlockState(), FLAGS);
                    trunks.remove(pending);
                    VillageEconomy.get(ctx.level().getServer()).setDirty();
                }
            }
            SiteMarkers.ensure(ctx, p, site.get());
            if (p.complete()) finish(ctx, p, site.get());
            else exhausted(ctx, p, site.get());
        }
    }

    /** 回到真实运行时：有进行中的项目就按离开时长累积施工储备（T5 方案 E）。 */
    public static void accrue(VillageContext ctx, long awayTicks) {
        if (ctx.state().activeProject().isPresent()) {
            ctx.state().constructionBank = ConstructionBank.accrue(ctx.state().constructionBank, awayTicks, config());
        }
    }

    static RuntimeConfig.Construction config() {
        return ModConfigs.runtime().current().village().construction();
    }

    /** 当前单格用时；储备只在真正完成格子后扣除。 */
    static int ticksFor(VillageContext ctx, BuildSegments.Segment seg) {
        return BuildPacing.ticks(ctx.data().construction(), ctx.state().constructionBank, speed(ctx.village().id()));
    }

    static int nextCell(BuildProject project, BuildSegments.Segment segment) {
        var blocked = project.blockedCells.get(segment.index());
        var missing = project.missingCells.get(segment.index());
        for (int i = 0; i < segment.cells().size(); i++) {
            if (!project.cellPlaced(segment.index(), i)
                    && (blocked == null || !blocked.get(i))
                    && (missing == null || !missing.get(i))) return i;
        }
        return -1;
    }

    static boolean hasWork(BuildProject p, BuildSite site) {
        return site.segments.stream().anyMatch(s -> nextCell(p, s) >= 0);
    }

    static boolean ready(VillageContext ctx, BuildProject p, BuildSite site, BuildSegments.Segment segment) {
        var blocked = p.blockedCells.get(segment.index()); var missing = p.missingCells.get(segment.index());
        for (int i = 0; i < segment.cells().size(); i++) {
            if (p.cellPlaced(segment.index(), i) || (blocked != null && blocked.get(i)) || (missing != null && missing.get(i))) continue;
            var cell = segment.cells().get(i); var at = VillageContext.block(cell.pos());
            if (ctx.loaded(at) && (segment.kind() == BuildSegments.Kind.DEMOLISH || cell.air() || site.supported(ctx.level(), at, site.state(segment,cell)))) return true;
        }
        return false;
    }

    /** 开发诊断：还没放、也没记被挡或缺料的前几格（段类型、方块、世界里现在是什么、放不放得住）。 */
    public static String stuck(VillageContext ctx, BuildProject p, int limit) {
        Optional<BuildSite> so = site(ctx, p);
        if (so.isEmpty()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (var seg : so.get().segments) {
            for (int i = 0; i < seg.cells().size() && n < limit; i++) {
                if (p.cellPlaced(seg.index(), i)) continue;
                var bl = p.blockedCells.get(seg.index());
                var mi = p.missingCells.get(seg.index());
                if ((bl != null && bl.get(i)) || (mi != null && mi.get(i))) continue;
                var c = seg.cells().get(i);
                BlockPos at = VillageContext.block(c.pos());
                boolean place = seg.kind() == BuildSegments.Kind.PLACE;
                sb.append(" | ").append(seg.index()).append(place ? " 放 " : " 拆 ").append(c.block()).append(" @").append(at.toShortString())
                        .append(" 现 ").append(BuildSite.id(ctx.level().getBlockState(at)))
                        .append(place && !c.air() ? (so.get().supported(ctx.level(), at, so.get().state(seg, c)) ? " 撑得住" : " 撑不住") : "")
                        .append(c.last() ? " 最后" : "");
                n++;
            }
        }
        return sb.toString();
    }

    static void exhausted(VillageContext ctx, BuildProject p, BuildSite site) {
        if (!p.blockedCells.isEmpty() && site.segments.stream().noneMatch(s -> ready(ctx,p,site,s))) {
            var entry = p.blockedCells.entrySet().iterator().next();
            p.block(site.segments.get(entry.getKey()).cells().get(entry.getValue().nextSetBit(0)).pos(),
                    "unreachable".equals(p.blockedReason) ? "unreachable" : "player_block");
        }
    }

    // ───────────── 落段 ─────────────

    public static void queue(UUID village, long project, int seg) {
        Drop d = new Drop(village, project, seg);
        if (QUEUED.add(d)) {
            QUEUE.add(d);
        }
    }

    public static boolean queued(UUID village, long project, int seg) {
        return QUEUED.contains(new Drop(village, project, seg));
    }

    /** 兼容已排队的请求：每次只尝试一格，统一受全服配额限制。 */
    public static void drain(MinecraftServer server) {
        int tries = QUEUE.size();
        while (tries-- > 0) {
            Drop d = QUEUE.poll();
            if (d == null) {
                return;
            }
            Optional<Result> r = dropNow(server, d);
            if (r.isPresent() && r.get() == Result.WAIT) {
                QUEUE.add(d);
                return;
            }
            QUEUED.remove(d);
            if (r.isPresent() && r.get() == Result.PLACED) {
                return;
            }
        }
    }

    static Optional<Result> dropNow(MinecraftServer server, Drop d) {
        Optional<VillagePlans.Plan> plan = VillagePlans.get(server).byVillage(d.village());
        ServerLevel level = plan.map(p -> server.getLevel(p.dim())).orElse(null);
        if (level == null) {
            return Optional.empty();
        }
        Optional<VillageContext> ctx = VillageScheduler.contextOf(level, d.village());
        if (ctx.isEmpty()) {
            return Optional.empty();
        }
        Optional<BuildProject> p = ctx.get().state().projects.stream().filter(x -> x.id == d.project()).findFirst();
        if (p.isEmpty()) {
            return Optional.empty();
        }
        Result r = drop(ctx.get(), p.get(), d.seg());
        LAST.put(d.village(), r);
        return Optional.of(r);
    }

    /** One attempt changes at most one cell. Direct command/test calls share the server budget. */
    public static Result drop(VillageContext ctx, BuildProject p, int segIdx) {
        Optional<BuildSite> so = site(ctx, p);
        if (so.isEmpty() || p.state != BuildProject.State.BUILDING || segIdx < 0 || segIdx >= p.segments) return Result.GONE;
        var seg = so.get().segments.get(segIdx);
        int cell = nextCell(p, seg);
        if (cell < 0) { exhausted(ctx, p, so.get()); return Result.GONE; }
        return dropCell(ctx, p, segIdx, cell);
    }

    public static Result dropCell(VillageContext ctx, BuildProject p, int segIdx, int cellIdx) {
        BuildPerformance.begin();
        try { return dropMeasured(ctx, p, segIdx, cellIdx); } finally { BuildPerformance.end(); }
    }

    private static Result dropMeasured(VillageContext ctx, BuildProject p, int segIdx, int cellIdx) {
        if (!VillageScheduler.isRunning(ctx)) return Result.WAIT;
        BuildSite site = site(ctx, p).orElseThrow();
        if (p.state != BuildProject.State.BUILDING || p.cellPlaced(segIdx, cellIdx)) return Result.GONE;
        var seg = site.segments.get(segIdx);
        var cell = seg.cells().get(cellIdx);
        var single = new BuildSegments.Segment(segIdx, seg.kind(), List.of(cell));
        BlockPos at = VillageContext.block(cell.pos());
        ServerLevel level = ctx.level();
        if (!level.isLoaded(at)) return Result.WAIT;
        boolean place = seg.kind() == BuildSegments.Kind.PLACE;
        if (place && playerInside(level, single)) return Result.WAIT;
        BlockState current = level.getBlockState(at);
        BlockState target = place ? site.state(seg, cell) : Blocks.AIR.defaultBlockState();
        boolean unprovenTerrain = place && cell.expect().isEmpty() && current != target && !current.isAir()
                && !current.canBeReplaced() && !current.is(BlockTags.LEAVES) && !current.is(BlockTags.FLOWERS)
                && !current.is(BlockTags.SAPLINGS) && !naturalGround(current) && !current.is(CLEARABLE)
                && !PlayerConstructionEdits.knownTerrain(level, at);
        if (unprovenTerrain || (!current.isAir() && PlayerConstructionEdits.contains(level, at))
                || (!acceptable(current, target, cell, place) && !site.oldTerrain(at, current))) {
            BuildProject.mark(p.blockedCells, segIdx, cellIdx, true);
            exhausted(ctx, p, site);
            VillageEconomy.get(level.getServer()).setDirty();
            return Result.BLOCKED;
        }
        if (place && !cell.air() && !site.supported(level, at, target)) return Result.DEFERRED;
        Map<String, Double> cost = place ? site.costs.cost(cell.block()) : Map.of();
        if (place && !p.canPay(cost)) {
            BuildProject.mark(p.missingCells, segIdx, cellIdx, true);
            exhausted(ctx, p, site);
            VillageEconomy.get(level.getServer()).setDirty();
            return Result.SHORT;
        }
        var budget = BUDGETS.computeIfAbsent(level.getServer(), k -> new BuildPacing.Budget());
        if (!budget.take(level.getServer().getTickCount(), limit(ctx))) return Result.WAIT;
        if (current != target && !level.setBlock(at, target, FLAGS)) return Result.WAIT;
        // The world mutation succeeded (or was already correct); only now commit this cell's material cost.
        if (place) p.pay(ctx.state().stock, cost);
        if (!current.isAir() && ((!place && BuildSite.id(current).equals(cell.block()))
                || (place && !cell.expect().isEmpty() && BuildSite.id(current).equals(cell.expect())))) {
            p.refund(ctx.state().stock, site.costs, site.costs.cost(BuildSite.id(current)));
        }
        if (current.is(BlockTags.LOGS) && cell.expect().isEmpty()) {
            BlockPos above = new BlockPos(at.getX(), Math.max(at.getY(), site.box().maxY()) + 1, at.getZ());
            for (int i = 0; i < 24 && level.getBlockState(above).is(BlockTags.LOGS); i++, above = above.above()) {
                if (PlayerConstructionEdits.contains(level, above) || !PlayerConstructionEdits.knownTerrain(level, above)) break;
                p.cleanupCells.add(VillageContext.pos(above));
            }
        }
        if (place) pushMobs(level, single);
        fixShapes(level, List.of(at));
        BlockState effect = place ? target : current;
        if (!effect.isAir()) level.playSound(null, at, place ? effect.getSoundType().getPlaceSound() : effect.getSoundType().getBreakSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!effect.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, effect),
                at.getX() + .5, at.getY() + .5, at.getZ() + .5, 4, .2, .2, .2, .02);
        p.placedCell(seg, cellIdx);
        site.stands.clear();
        VillageEconomy.get(level.getServer()).setDirty();
        if (p.complete()) finish(ctx, p, site);
        else exhausted(ctx, p, site);
        return Result.PLACED;
    }

    /** 这一格能不能动工：已是目标、旧建筑里该有的旧方块、或自然方块（草、雪、树叶、泥土……）。 */
    static boolean acceptable(BlockState cur, BlockState target, BuildSegments.Cell c, boolean place) {
        if (cur.isAir()) {
            return true;
        }
        String id = BuildSite.id(cur);
        if (place) {
            return (!c.air() && cur.is(target.getBlock())) || (!c.expect().isEmpty() && id.equals(c.expect())) || clearable(cur);
        }
        return id.equals(c.block()) || clearable(cur);
    }

    /**
     * 施工截断了一棵天然的树：把建筑上方剩下的那截树干一并清掉（树叶没了原木支撑会自然凋落），
     * 免得一截树干和树冠穿出屋顶。只清建筑包围盒以上、与这一格同一列的连续原木。
     */
    static void clearTrunkAbove(ServerLevel level, BlockPos cut, Box box) {
        BlockPos p = new BlockPos(cut.getX(), Math.max(cut.getY(), box.maxY()) + 1, cut.getZ());
        for (int i = 0; i < 24 && level.getBlockState(p).is(BlockTags.LOGS); i++, p = p.above()) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** 自然方块与地块界石：施工时直接清掉（与 PlotWatch 的「不算占用」一致，另加天然石头、矿石、砂砾、雪冰等山体地形）。 */
    /** 村子自己放的、施工时可以直接拆掉的方块（地块界石等）。 */
    static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> CLEARABLE = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mythic_age", "construction_clearable"));

    public static boolean clearable(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.DIRT) || s.is(BlockTags.LOGS) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(BlockTags.ICE) || s.is(Blocks.GRAVEL) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND) || s.is(Blocks.CLAY)
                || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW) || s.is(CLEARABLE)
                || s.is(net.neoforged.neoforge.common.Tags.Blocks.ORES) || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.MUD)
                || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.CALCITE) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE);
    }

    /**
     * 天然地面：没有来源记录的老区块里也照 T19 的规则允许替换（地基几乎都要替换这些）。
     * 原木、木板这类玩家常拿来盖房子的方块不在内，老区块里仍按保守原则当成被挡。
     */
    static boolean naturalGround(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND) || s.is(Blocks.CLAY) || s.is(Blocks.MUD) || s.is(BlockTags.TERRACOTTA)
                || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW) || s.is(BlockTags.ICE) || s.is(Blocks.MOSS_BLOCK)
                || s.is(Blocks.CALCITE) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE)
                || s.is(net.neoforged.neoforge.common.Tags.Blocks.ORES);
    }

    static boolean playerInside(ServerLevel level, BuildSegments.Segment seg) {
        for (Player pl : io.github.happysnappingturtle.mythicage.player.OnlinePlayers.in(level)) {
            if (pl.isSpectator()) {
                continue;
            }
            AABB box = pl.getBoundingBox();
            for (BuildSegments.Cell c : seg.cells()) {
                if (!c.air() && box.intersects(new AABB(VillageContext.block(c.pos())))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 段里放下的实心格上站着的生物：挪到上面能站的地方。 */
    static void pushMobs(ServerLevel level, BuildSegments.Segment seg) {
        for (BuildSegments.Cell c : seg.cells()) {
            if (c.air()) {
                continue;
            }
            BlockPos at = VillageContext.block(c.pos());
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at), x -> !(x instanceof Player))) {
                BlockPos up = at.above();
                for (int i = 0; i < 8 && !BuildSite.standable(level, up); i++) {
                    up = up.above();
                }
                e.teleportTo(e.getX(), up.getY(), e.getZ());
            }
        }
    }

    /** 放下后给这一段与相邻的方块做一次形状修正（栅栏、墙连接）；门、床等两格方块跳过（另一半可能还没放）。 */
    static void fixShapes(ServerLevel level, List<BlockPos> changed) {
        Set<BlockPos> touch = new LinkedHashSet<>();
        for (BlockPos p : changed) {
            touch.add(p);
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                touch.add(p.relative(d));
            }
        }
        for (BlockPos p : touch) {
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || s.hasProperty(BlockStateProperties.BED_PART)) {
                continue;
            }
            BlockState u = Block.updateFromNeighbourShapes(s, level, p);
            if (u != s) {
                level.setBlock(p, u, FLAGS);
            }
        }
    }

    static void effects(ServerLevel level, BuildSite site, BuildSegments.Segment seg) {
        Pos c = seg.center();
        BlockState sample = seg.cells().stream().filter(x -> !x.air()).findFirst().map(x -> site.state(seg, x))
                .orElse(Blocks.STONE.defaultBlockState());
        level.playSound(null, new BlockPos(c.x(), c.y(), c.z()), sample.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        int n = 0;
        for (BuildSegments.Cell x : seg.cells()) {
            if (x.air() || n++ >= 4) {
                continue;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, site.state(seg, x)), x.pos().x() + 0.5,
                    x.pos().y() + 0.5, x.pos().z() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
        }
    }

    // ───────────── 验收与落成 ─────────────

    /** 全部落完：抽查关键格，不一致就修补，否则落成。 */
    static void finish(VillageContext ctx, BuildProject p, BuildSite site) {
        if (!p.cleanupCells.isEmpty()) return;
        Map<Integer, Map<String, Double>> bad = inspect(ctx, site);
        if (!bad.isEmpty()) {
            p.unplace(bad);
            ctx.state().log(ctx.today(), "mythic_age.village.log.project_repair", nameArg(site.spec, p.god), String.valueOf(bad.size()));
            VillageEconomy.get(ctx.level().getServer()).setDirty();
            return;
        }
        complete(ctx, p, site);
    }

    /** 抽查：门、床、工作站、神像，加每 16 格抽 1 格。返回不一致的段 → 段的成本。 */
    static Map<Integer, Map<String, Double>> inspect(VillageContext ctx, BuildSite site) {
        Set<String> stations = new LinkedHashSet<>();
        for (VillageRecipe r : ctx.data().recipes().values()) {
            if (!r.station().startsWith("anchor:") && !r.station().startsWith("point:")) {
                stations.add(r.station());
            }
        }
        Map<Integer, Map<String, Double>> bad = new LinkedHashMap<>();
        for (BuildSegments.Segment seg : site.segments) {
            if (seg.kind() != BuildSegments.Kind.PLACE) {
                continue;
            }
            for (BuildSegments.Cell c : seg.cells()) {
                if (c.air()) {
                    continue;
                }
                BlockState target = site.state(seg, c);
                boolean key = target.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                        || target.hasProperty(BlockStateProperties.BED_PART) || stations.contains(c.block())
                        || c.last() || c.index() % 16 == 0;
                BlockPos at = VillageContext.block(c.pos());
                if (key && ctx.loaded(at) && !ctx.level().getBlockState(at).is(target.getBlock())) {
                    bad.put(seg.index(), site.cost(seg.index()));
                    break;
                }
            }
        }
        return bad;
    }

    static void complete(VillageContext ctx, BuildProject p, BuildSite site) {
        VillageEconomy.State s = ctx.state();
        MinecraftServer server = ctx.level().getServer();
        SiteMarkers.clear(ctx, p);
        p.state = BuildProject.State.DONE;
        s.projects.remove(p);
        SITES.remove(key(ctx.village().id(), p.id));
        BuildingSpec spec = site.spec;
        VillageInfoPiece.Info info = ctx.plan().info();
        Map<String, List<Pos>> anchors = new LinkedHashMap<>();
        info.anchors().forEach((k, v) -> anchors.put(k, new ArrayList<>(v)));
        List<Box> built = new ArrayList<>(info.built());
        if (!p.replaces.isEmpty()) {
            BuildingSpec old = VillageBuildings.all().get(p.replaces);
            if (old != null && p.oldOrigin != null) {
                BuildPlacement.Placed oldAt = placed(old, p.oldOrigin, p.oldTurn);
                BuildPlacement.worldAnchors(old, oldAt).forEach((k, v) -> {
                    List<Pos> l = anchors.get(k);
                    if (l != null) {
                        l.removeAll(v);
                    }
                });
                built.remove(oldAt.box());
            }
            s.constructed.removeIf(b -> b.building().equals(p.replaces) && (p.oldOrigin == null || b.origin().equals(p.oldOrigin)));
        }
        BuildPlacement.worldAnchors(spec, site.at).forEach((k, v) -> anchors.computeIfAbsent(k, x -> new ArrayList<>()).addAll(v));
        built.add(site.box());
        VillagePlans.get(server).updateInfo(ctx.village().id(), new VillageInfoPiece.Info(info.civ(), info.patron(), info.layout(),
                info.layoutVersion(), info.templateSet(), info.center(), anchors, built, info.plots()));
        s.constructed.add(new VillageEconomy.Built(spec.id(), site.at.origin(), site.at.turn(), site.box(), p.plot, p.god,
                ctx.today()));
        io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.afterConstruction(server, ctx.village().id());
        if (!p.plot.isEmpty()) {
            s.builtPlots.add(p.plot);
        }
        if (!p.replaces.isEmpty()) {
            moveGuardian(ctx, spec, site.at);
        }
        ctx.rt().invalidateStations();
        s.log(ctx.today(), "mythic_age.village.log.project_done", nameArg(spec, p.god));
        BuildNotices.built(ctx, spec, p.god);
        doorAccess(ctx, spec, site.at, built);
        VillageEconomy.get(server).setDirty();
        advance(ctx, false);
    }

    /**
     * 落成时的门前检查（GA-14 D2）：原地升级沿用旧位置，门可能被坡地整地的挡土墙堵着。门前要台阶就一次修好（只拆自然方块与村子
     * 自己砌的挡土墙；玩家放的方块、别的建筑占地里的格一格都不碰，有一格拆不了就整条不修），写一行发展记录。返回修了几级。
     */
    public static int doorAccess(VillageContext ctx, BuildingSpec spec, BuildPlacement.Placed at, List<Box> built) {
        ServerLevel level = ctx.level();
        var door = io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.door(spec, at.origin(), at.turn());
        var cols = io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.corridor(at.box(), door,
                io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.MAX_STEPS);
        for (var c : cols) {
            if (!level.isLoaded(new BlockPos(c.x(), door.pos().y(), c.z()))) {
                return 0;
            }
        }
        List<Box> others = built.stream().filter(b -> !b.equals(at.box()) && !b.containsXZ(door.pos().x(), door.pos().z())).toList();
        java.util.function.Predicate<BlockPos> can = q -> io.github.happysnappingturtle.mythicage.structure.TerrainOps.doorClearable(level.getBlockState(q))
                && !PlayerConstructionEdits.contains(level, q) && others.stream().noneMatch(b -> b.containsXZ(q.getX(), q.getZ()));
        int need = io.github.happysnappingturtle.mythicage.structure.TerrainOps.doorSteps(level, null, cols, door.pos().y(), can, true);
        if (need < 0) {
            ctx.state().log(ctx.today(), "mythic_age.village.log.door_blocked", nameArg(spec, ""));
            return 0;
        }
        if (need > 0) {
            io.github.happysnappingturtle.mythicage.structure.TerrainOps.doorSteps(level, null, cols, door.pos().y(), can, false);
            ctx.state().log(ctx.today(), "mythic_age.village.log.door_steps", nameArg(spec, ""));
        }
        return need;
    }

    /** 神庙扩建后，祭司（守护者）的居所跟着神像站位走。 */
    static void moveGuardian(VillageContext ctx, BuildingSpec spec, BuildPlacement.Placed at) {
        List<Pos> g = BuildPlacement.worldAnchors(spec, at).getOrDefault("guardian", List.of());
        if (g.isEmpty()) {
            return;
        }
        BlockPos home = VillageContext.block(g.get(0));
        GuardianRegistry reg = GuardianRegistry.get(ctx.level().getServer());
        reg.byBell(ctx.level().dimension(), ctx.center()).ifPresent(e -> {
            reg.put(new GuardianRegistry.Entry(e.guardianId(), GlobalPos.of(ctx.level().dimension(), home), e.bell(), e.alive(),
                    e.deathDay(), e.entityUuid(), e.civ()));
            if (ctx.level().getEntity(e.entityUuid()) instanceof GuardianEntity ge) {
                ge.assign(e.guardianId(), home);
                ge.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            }
        });
    }

    /** 取消（管理命令）：只退托管里剩下的料。 */
    public static Map<String, Integer> cancel(VillageContext ctx, BuildProject p) {
        SiteMarkers.clear(ctx, p);
        Map<String, Integer> back = p.cancel(ctx.state().stock);
        ctx.state().projects.remove(p);
        SITES.remove(key(ctx.village().id(), p.id));
        ctx.state().log(ctx.today(), "mythic_age.village.log.project_cancelled", p.building);
        VillageEconomy.get(ctx.level().getServer()).setDirty();
        advance(ctx, false);
        return back;
    }

    // ───────────── 名称 ─────────────

    /** 建筑名的语言键（主神变体用基础名）。 */
    public static String nameKey(BuildingSpec spec) {
        return "mythic_age.building." + spec.civ() + "." + spec.shortBase();
    }

    /** 发展记录里的建筑名参数：通用祠写成「某神的祠」。 */
    public static String nameArg(BuildingSpec spec, String god) {
        return "*".equals(spec.god()) && !god.isEmpty() ? "@mythic_age.god." + god : "@" + nameKey(spec);
    }

    public static Component name(BuildingSpec spec, String god) {
        return "*".equals(spec.god()) && !god.isEmpty()
                ? Component.translatable("mythic_age.building.shrine_of", Component.translatable("mythic_age.god." + god))
                : Component.translatable(nameKey(spec));
    }

    /** 开发命令：这座替换类建筑能替换的旧建筑。 */
    public static Optional<String> replaceableForCommand(VillageContext ctx, BuildingSpec spec) {
        return replaceable(ctx, List.of(spec), VillageBuildings.all()).stream()
                .filter(id -> spec.replace().stream().anyMatch(id::startsWith)).findFirst();
    }

    /** 开发命令：第一块放得下这座建筑的空地块。 */
    public static Optional<String> firstPlotFor(VillageContext ctx, BuildingSpec spec) {
        return freePlots(ctx).stream().filter(p -> spec.plotKinds().contains(p.kind())
                && BuildPlacement.fits(spec.sizeX(), spec.sizeZ(), BuildPlacement.turnFor(spec.doorFacing(), p.front()), p.w(), p.d()))
                .map(ProjectPicker.Plot::id).findFirst();
    }

    // ───────────── 测试 ─────────────

    /** 只供 GameTest：最近一次从队列落段的结果。 */
    public static Optional<Result> lastForTest(UUID village) {
        return Optional.ofNullable(LAST.get(village));
    }

    public static void clearForTest(UUID village) {
        SITES.keySet().removeIf(k -> k.startsWith(village + ":"));
        QUEUE.removeIf(d -> d.village().equals(village));
        QUEUED.removeIf(d -> d.village().equals(village));
        LAST.remove(village);
        SPEEDS.remove(village);
        RECHECK.keySet().removeIf(k -> k.startsWith(village + ":"));
    }

    static void notifyPlayers(ServerLevel level, BlockPos center, Component msg) {
        for (ServerPlayer pl : io.github.happysnappingturtle.mythicage.player.OnlinePlayers.in(level)) {
            if (pl.blockPosition().closerThan(center, NOTICE_RANGE)) {
                pl.displayClientMessage(msg, true);
            }
        }
    }
}
