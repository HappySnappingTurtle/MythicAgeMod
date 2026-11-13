package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildCosts;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectQueue;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.build.VillageProjects;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.quest.VillageQuests;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GA-11 项目选择与升级（TC-GA11-13、20～28）：希腊真实数据。场地里放一座远古坡地小屋（原点 (1, 1, 1)）与一座谷仓（原点 (1, 1, 24)），
 * 村庄是古典阶段；点名、队列、原地升级都走正式代码。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageProjectGameTests {
    private static final String BATCH = "village_projects";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final String HOUSE = "greek:house_small", HOUSE_II = "greek:house_small_ii";
    private static final String GRANARY = "greek:granary", GRANARY_II = "greek:granary_ii";

    private VillageProjectGameTests() {
    }

    record Site(VillageWorkGameTests.TV tv, BuildPlacement.Placed house, BuildPlacement.Placed granary, BuildPlacement.Placed house2,
                List<FakePlayer> players) {
    }

    private static BuildPlacement.Placed place(GameTestHelper h, String id, int x, int y, int z) {
        BuildingSpec s = VillageBuildings.all().get(id);
        BlockPos o = h.absolutePos(new BlockPos(x, y, z));
        StructureTemplate t = h.getLevel().getServer().getStructureManager().get(ResourceLocation.parse(s.template())).orElseThrow();
        t.placeInWorld(h.getLevel(), o, o, new StructurePlaceSettings(), h.getLevel().getRandom(), 2);
        Pos origin = new Pos(o.getX(), o.getY(), o.getZ());
        return new BuildPlacement.Placed(s.id(), origin, Turn.R0, TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), Turn.R0));
    }

    /** 古典希腊村：生成的小屋与谷仓（锚点、包围盒照生成时的写法），口粮充足；可选一块空的住居地块（给内院宅）。 */
    static Site village(GameTestHelper h) {
        return village(h, false);
    }

    /** @param twoHouses 再放一座小屋（原点 (1, 1, 12)） */
    static Site village(GameTestHelper h, boolean twoHouses) {
        VillageWorkGameTests.floor(h);
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        BuildPlacement.Placed house = place(h, HOUSE, 1, 1, 1);
        BuildPlacement.Placed granary = place(h, GRANARY, 1, 1, 24);
        BuildPlacement.Placed house2 = twoHouses ? place(h, HOUSE, 1, 1, 12) : null;
        Map<String, List<Pos>> anchors = new LinkedHashMap<>();
        BuildPlacement.worldAnchors(specs.get(HOUSE), house).forEach((k, v) -> anchors.computeIfAbsent(k, x -> new ArrayList<>()).addAll(v));
        BuildPlacement.worldAnchors(specs.get(GRANARY), granary).forEach((k, v) -> anchors.computeIfAbsent(k, x -> new ArrayList<>()).addAll(v));
        if (house2 != null) {
            BuildPlacement.worldAnchors(specs.get(HOUSE), house2).forEach((k, v) -> anchors.computeIfAbsent(k, x -> new ArrayList<>()).addAll(v));
        }
        Pos center = VillageWorkGameTests.abs(h, 14, 2, 14);
        anchors.put("center", List.of(center));
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        // 整个场地算村界（玩家站在哪都在村里）；规划里再记两座建筑各自的包围盒（升级按它认出旧建筑）
        Box all = new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 35);
        List<Box> built = new ArrayList<>(List.of(all, house.box(), granary.box()));
        if (house2 != null) {
            built.add(house2.box());
        }
        VillageInfoPiece.Info info = new VillageInfoPiece.Info("greek", "", "test", 1, "test", center, anchors, built, List.of());
        UUID id = UUID.randomUUID();
        VillageRegistry.Village v = new VillageRegistry.Village(id, GlobalPos.of(h.getLevel().dimension(), VillageInfoPiece.block(center)),
                "greek", "", VillageRegistry.Origin.GENERATED, null, "", -1).withStage(Age.CLASSICAL);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        VillagePlans.get(h.getLevel().getServer()).put(new VillagePlans.Plan(id, h.getLevel().dimension(), "test", 1L, info, true));
        VillageScheduler.forceRunningForTest(id, true);
        VillageWorkGameTests.TV tv = new VillageWorkGameTests.TV(v, VillagePlans.get(h.getLevel().getServer()).byVillage(id).orElseThrow());
        VillageEconomy.State s = VillageWorkGameTests.state(h, tv);
        s.initialized = true;
        s.stock.setCapacity(100000);
        s.stock.putUnchecked("mythic_age:olive_flatbread", 40);
        return new Site(tv, house, granary, house2, new ArrayList<>());
    }

    static VillageContext ctx(GameTestHelper h, Site site) {
        return VillageScheduler.contextForTest(h.getLevel(), site.tv().id()).orElseThrow();
    }

    static FakePlayer player(GameTestHelper h, Site site, int contribution) {
        FakePlayer p = VillageBuildGameTests.player(h, "Nominator" + contribution, h.absolutePos(new BlockPos(14, 2, 14)));
        VillageQuests.get(h.getLevel().getServer()).book(site.tv().id()).contribute(p.getUUID(), "test", contribution, 0);
        site.players().add(p);
        return p;
    }

    /** 开一个升级项目并备齐料。 */
    static BuildProject upgrade(GameTestHelper h, Site site, String building, String old, BuildPlacement.Placed at) {
        VillageContext ctx = ctx(h, site);
        BuildProject p = Construction.start(ctx, new ProjectPicker.Pick(building, "", "", old, at.origin())).orElseThrow();
        stock(ctx, p);
        Construction.tick(ctx);
        return p;
    }

    static void stock(VillageContext ctx, BuildProject p) {
        BuildCosts costs = ctx.data().buildCosts();
        p.bill().forEach((k, n) -> ctx.state().stock.putUnchecked(costs.representative(k), n));
    }

    static void cleanup(GameTestHelper h, Site site) {
        Construction.clearForTest(site.tv().id());
        VillageWorkGameTests.cleanup(h, site.tv());
        site.players().forEach(p -> io.github.happysnappingturtle.mythicage.player.OnlinePlayers.forTest(p, false));
    }

    /**
     * 按 BuildJob 的阶段规则推进一格：只在第一个没放完的阶段（拆 / 放 × 平常 / 最后）里找段落块，不跨阶段。
     * 依附在后一阶段的格上的格如果留在前一阶段，这里会一直卡住（GA-11 实机：阁楼油灯与陶瓮）。
     */
    static void phasedStep(GameTestHelper h, Site site, BuildProject p) {
        VillageContext ctx = ctx(h, site);
        Construction.tick(ctx);
        if (p.state != BuildProject.State.BUILDING) {
            return;
        }
        var buildSite = Construction.site(ctx, p).orElseThrow();
        io.github.happysnappingturtle.mythicage.core.village.build.BuildSegments.Kind phase = null;
        Boolean late = null;
        for (var seg : buildSite.segments) {
            if (p.placed.get(seg.index())) {
                continue;
            }
            boolean isLate = seg.cells().stream().anyMatch(io.github.happysnappingturtle.mythicage.core.village.build.BuildSegments.Cell::last);
            if (phase == null) {
                phase = seg.kind();
                late = isLate;
            }
            if (phase != seg.kind() || late != isLate) {
                return;
            }
            var r = Construction.drop(ctx, p, seg.index());
            if (r == Construction.Result.PLACED || r == Construction.Result.BLOCKED || r == Construction.Result.SHORT) {
                return;
            }
        }
    }

    /** 诊断：还没放的格（前 6 个）：段类型、方块、世界里现在是什么。 */
    static String left(GameTestHelper h, Site site, BuildProject p) {
        var siteOpt = Construction.site(ctx(h, site), p);
        if (siteOpt.isEmpty()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (var seg : siteOpt.get().segments) {
            for (int i = 0; i < seg.cells().size() && n < 6; i++) {
                if (!p.cellPlaced(seg.index(), i)) {
                    var c = seg.cells().get(i);
                    sb.append(seg.kind()).append(' ').append(c.block()).append('@').append(c.pos()).append(" now=")
                            .append(h.getLevel().getBlockState(VillageInfoPiece.block(c.pos()))).append("; ");
                    n++;
                }
            }
        }
        return sb.toString();
    }

    static Pos rel(BuildPlacement.Placed at, int x, int y, int z) {
        return TemplateTransform.toWorld(at.origin(), new Pos(x, y, z), at.turn());
    }

    // ───────────── 用例 ─────────────

    /** TC-GA11-20、13：贡献 64 点名谷仓升级 → 进「下一个」位、村里玩家收到提示、发展记录一行；存档往返一致。 */
    @GameTest(template = T, batch = BATCH)
    public static void nominate_with_enough_contribution(GameTestHelper h) {
        Site site = village(h);
        try {
            VillageContext ctx = ctx(h, site);
            upgrade(h, site, HOUSE_II, HOUSE, site.house());
            FakePlayer p = player(h, site, 64);
            var out = VillageProjects.nominate(p, ctx, GRANARY_II);
            h.assertTrue(out.result() == VillageProjects.Result.OK, "点名成功：" + out);
            VillageEconomy.State s = ctx.state();
            h.assertTrue(s.nextProject != null && s.nextProject.key().equals(GRANARY_II) && s.nextProject.player().equals(p.getUUID()),
                    "进「下一个」位：" + s.nextProject);
            h.assertTrue(s.activeProject().map(x -> x.building.equals(HOUSE_II)).orElse(false), "当前项目不变");
            var said = VillageProjects.lastAnnounceForTest(site.tv().id()).orElseThrow();
            h.assertTrue(said.to().contains(p.getUUID()), "村界内的玩家收到提示");
            h.assertTrue(s.log.stream().anyMatch(l -> l.key().equals("mythic_age.village.log.project_nominated")), "发展记录");
            List<AffairsView.Line> lines = VillageAffairs.projectLines(s);
            h.assertTrue(lines.stream().anyMatch(l -> l.key().equals("mythic_age.village.project.next")), "村务里有「下一个」一行：" + lines);
            // TC-GA11-13：存档往返
            VillageEconomy back = VillageEconomy.roundTripForTest(VillageEconomy.get(h.getLevel().getServer()), h.getLevel().registryAccess());
            ProjectQueue.Next n = back.peek(site.tv().id()).orElseThrow().nextProject;
            h.assertTrue(s.nextProject.equals(n), "存档往返一致：" + n);
            // 清空
            h.assertTrue(VillageProjects.clear(p, ctx).result() == VillageProjects.Result.CLEARED && s.nextProject == null, "清空");
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    /** TC-GA11-21：贡献 10 → 拒绝，提示还差多少。 */
    @GameTest(template = T, batch = BATCH)
    public static void nominate_low_contribution(GameTestHelper h) {
        Site site = village(h);
        try {
            FakePlayer p = player(h, site, 10);
            var out = VillageProjects.nominate(p, ctx(h, site), GRANARY_II);
            h.assertTrue(out.result() == VillageProjects.Result.LOW_CONTRIBUTION && out.need() == 64 && out.have() == 10, "拒绝：" + out);
            h.assertTrue(ctx(h, site).state().nextProject == null && ctx(h, site).state().activeProject().isEmpty(), "什么都没变");
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    /** TC-GA11-22：当前项目开工后不能换：点名正在盖的那一项被拒绝，当前项目不动。 */
    @GameTest(template = T, batch = BATCH)
    public static void nominate_current_rejected(GameTestHelper h) {
        Site site = village(h);
        try {
            BuildProject p = upgrade(h, site, GRANARY_II, GRANARY, site.granary());
            FakePlayer pl = player(h, site, 100);
            var out = VillageProjects.nominate(pl, ctx(h, site), GRANARY_II);
            h.assertTrue(out.result() == VillageProjects.Result.CURRENT, "正在盖的不能再点：" + out);
            h.assertTrue(ctx(h, site).state().activeProject().orElseThrow() == p && p.active(), "当前项目不动");
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    /**
     * TC-GA11-23、24、26：小屋升级施工中点名谷仓升级；施工期间一层的床一直在（床格两级相同，不拆不换）；
     * 落成后床位锚点 2 → 4、旧建筑登记换成新的；随即开工点名的谷仓升级。
     */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 6000)
    public static void house_upgrade_then_next(GameTestHelper h) {
        Site site = village(h);
        VillageContext ctx = ctx(h, site);
        BuildProject p = upgrade(h, site, HOUSE_II, HOUSE, site.house());
        h.assertTrue(p.state == BuildProject.State.BUILDING, "料齐开工：" + p.state + " " + p.missing());
        FakePlayer pl = player(h, site, 64);
        h.assertTrue(VillageProjects.nominate(pl, ctx, GRANARY_II).result() == VillageProjects.Result.OK, "点名谷仓升级");
        BuildingSpec small = VillageBuildings.all().get(HOUSE);
        List<BlockPos> beds = new ArrayList<>();
        BuildPlacement.worldAnchors(small, site.house()).get("home").forEach(a -> beds.add(VillageInfoPiece.block(a)));
        h.onEachTick(() -> {
            for (BlockPos b : beds) {
                h.assertTrue(h.getLevel().getBlockState(b).getBlock() instanceof BedBlock, "施工期间一层的床一直在：" + b);
            }
        });
        h.startSequence().thenWaitUntil(() -> {
            phasedStep(h, site, p);
            h.assertTrue(!p.active(), "逐块升级中：" + p.state + " " + BuildProject.count(p.placedCells) + " blocked=" + p.blockedCells
                    + " missing=" + p.missingCells + " left=" + left(h, site, p));
        }).thenExecute(() -> {
            try {
                VillageEconomy.State s = ctx.state();
                h.assertTrue(s.constructed.stream().anyMatch(b -> b.building().equals(HOUSE_II)), "记下两层小屋");
                VillagePlans.Plan plan = VillagePlans.get(h.getLevel().getServer()).byVillage(site.tv().id()).orElseThrow();
                h.assertTrue(plan.anchors("home").size() == 4, "床位 2 → 4：" + plan.anchors("home"));
                h.assertTrue(!plan.info().built().contains(site.house().box()), "旧包围盒去掉");
                BlockPos upstairs = VillageInfoPiece.block(rel(site.house(), 3, 5, 3));
                h.assertTrue(h.getLevel().getBlockState(upstairs).getBlock() instanceof BedBlock, "二层的床：" + upstairs);
                h.assertTrue(s.nextProject == null, "「下一个」位清空");
                h.assertTrue(s.activeProject().map(x -> x.building.equals(GRANARY_II)).orElse(false), "随即开工点名的谷仓升级："
                        + s.activeProject().map(x -> x.building));
                h.assertTrue(VillageProjects.lastAnnounceForTest(site.tv().id()).map(a -> a.message().getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                        && tc.getKey().equals("mythic_age.village.project.nominated_started")).orElse(false), "开工提示");
                h.assertTrue(Construction.input(ctx(h, site)).map(in -> ProjectPicker.candidates(in).stream()
                        .noneMatch(c -> c.key().equals(HOUSE_II))).orElse(false), "唯一一座小屋升级完，不再列出小屋升级");
                h.succeed();
            } finally {
                cleanup(h, site);
            }
        });
    }

    /** TC-GA11-25：谷仓升级落成 → 仓储单位 +1（容量 + 一个仓储单位）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 6000)
    public static void granary_upgrade_adds_storage(GameTestHelper h) {
        Site site = village(h);
        VillageContext ctx = ctx(h, site);
        VillageScheduler.processNowForTest(h.getLevel(), site.tv().id());
        int before = ctx.state().stock.capacity();
        // 调度时村子可能已经在黎明自己选了项目：先撤掉，下面手动开
        ctx.state().activeProject().ifPresent(x -> Construction.cancel(ctx, x));
        BuildProject p = upgrade(h, site, GRANARY_II, GRANARY, site.granary());
        h.startSequence().thenWaitUntil(() -> {
            phasedStep(h, site, p);
            h.assertTrue(!p.active(), "逐块升级中（按阶段）：" + p.state + " " + BuildProject.count(p.placedCells) + " blocked=" + p.blockedCells
                    + " missing=" + p.missingCells + " bill=" + p.missing() + " left=" + left(h, site, p));
        }).thenExecute(() -> {
            try {
                VillageScheduler.processNowForTest(h.getLevel(), site.tv().id());
                int after = ctx.state().stock.capacity();
                h.assertTrue(after - before == ctx.data().storageUnit(), "仓储 +1 单位：" + before + " → " + after);
                VillagePlans.Plan plan = VillagePlans.get(h.getLevel().getServer()).byVillage(site.tv().id()).orElseThrow();
                h.assertTrue(plan.anchors("storage_keeper").size() == 1, "管事位仍是一个");
                h.succeed();
            } finally {
                cleanup(h, site);
            }
        });
    }

    /** TC-GA11-27：命令 projects 列出候选；nominate 点名、nominate clear 清空。 */
    @GameTest(template = T, batch = BATCH)
    public static void commands_projects_and_nominate(GameTestHelper h) {
        Site site = village(h);
        try {
            VillageContext ctx = ctx(h, site);
            upgrade(h, site, HOUSE_II, HOUSE, site.house());
            FakePlayer p = player(h, site, 64);
            CommandSourceStack src = p.createCommandSourceStack().withPermission(2);
            var d = h.getLevel().getServer().getCommands().getDispatcher();
            int n = d.execute("mythicage village projects", src);
            List<VillageProjects.View> list = VillageProjects.candidates(ctx);
            h.assertTrue(n == list.size() && n > 0, "列出候选：" + n);
            int idx = -1;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).candidate().key().equals(GRANARY_II)) {
                    idx = i + 1;
                }
            }
            h.assertTrue(idx > 0, "候选里有谷仓升级");
            h.assertTrue(list.stream().noneMatch(v -> v.candidate().key().equals(HOUSE_II)), "正在升级的小屋不再列出");
            VillageProjects.View granary = list.get(idx - 1);
            h.assertTrue(!granary.materials().isEmpty(), "升级有差额材料表");
            h.assertTrue(d.execute("mythicage village nominate " + idx, src) == 1, "nominate 成功");
            h.assertTrue(ctx.state().nextProject != null && ctx.state().nextProject.key().equals(GRANARY_II), "点名进位");
            h.assertTrue(d.execute("mythicage village nominate clear", src) == 1 && ctx.state().nextProject == null, "nominate clear");
            h.succeed();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new net.minecraft.gametest.framework.GameTestAssertException(e.getMessage());
        } finally {
            cleanup(h, site);
        }
    }

    /** 村子闲着时点名：马上开工；升级的材料表是差额（比新建整座少）。 */
    @GameTest(template = T, batch = BATCH)
    public static void nominate_idle_starts_now(GameTestHelper h) {
        Site site = village(h);
        try {
            VillageContext ctx = ctx(h, site);
            FakePlayer p = player(h, site, 64);
            h.assertTrue(VillageProjects.nominate(p, ctx, HOUSE_II).result() == VillageProjects.Result.OK, "点名");
            var active = ctx.state().activeProject().orElseThrow();
            h.assertTrue(active.building.equals(HOUSE_II) && active.oldOrigin.equals(site.house().origin()), "马上开工，换的是这座小屋");
            h.assertTrue(ctx.state().nextProject == null, "「下一个」位已用掉");
            int diff = active.bill().values().stream().mapToInt(Integer::intValue).sum();
            BuildingSpec ii = VillageBuildings.all().get(HOUSE_II);
            int whole = io.github.happysnappingturtle.mythicage.village.build.BuildSite.create(h.getLevel(), ctx.data().buildCosts(), ii,
                    new BuildPlacement.Placed(ii.id(), new Pos(0, 0, 0), Turn.R0, TemplateTransform.worldBox(new Pos(0, 0, 0),
                            ii.sizeX(), ii.sizeY(), ii.sizeZ(), Turn.R0))).map(x -> BuildCosts.ceil(x.totalCost()).values().stream()
                    .mapToInt(Integer::intValue).sum()).orElse(0);
            h.assertTrue(diff > 0 && diff < whole, "差额 " + diff + " < 整座 " + whole);
            h.succeed();
        } finally {
            cleanup(h, site);
        }
    }

    /**
     * 两座小屋：第一座升级中，点名「小屋升级」（住居不是唯一建筑，照常能点）；第一座落成后接着升级的是第二座，
     * 不会把刚升级完的第一座又当成旧建筑（落成后用新的规划）。
     */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 6000)
    public static void second_house_upgrade_follows(GameTestHelper h) {
        Site site = village(h, true);
        BuildProject p = upgrade(h, site, HOUSE_II, HOUSE, site.house());
        FakePlayer pl = player(h, site, 64);
        var out = VillageProjects.nominate(pl, ctx(h, site), HOUSE_II);
        h.assertTrue(out.result() == VillageProjects.Result.OK, "另一座小屋照常能点：" + out);
        h.startSequence().thenWaitUntil(() -> {
            VillageBuildGameTests.step(h, site.tv(), p);
            h.assertTrue(!p.active(), "逐块升级中：" + p.state + " " + BuildProject.count(p.placedCells) + " left=" + left(h, site, p));
        }).thenExecute(() -> {
            try {
                var next = ctx(h, site).state().activeProject().orElseThrow();
                h.assertTrue(next.building.equals(HOUSE_II) && next.oldOrigin.equals(site.house2().origin()),
                        "接着升级第二座：" + next.oldOrigin + " / " + site.house2().origin());
                h.succeed();
            } finally {
                cleanup(h, site);
            }
        });
    }
}
