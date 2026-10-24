package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildCosts;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildSegments;
import io.github.happysnappingturtle.mythicage.core.village.build.ProjectPicker;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.build.Blueprint;
import io.github.happysnappingturtle.mythicage.village.build.BuildJob;
import io.github.happysnappingturtle.mythicage.village.build.BuildNotices;
import io.github.happysnappingturtle.mythicage.village.build.BuildSite;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * T19 阶段 ③ 施工（TC-T19-80～93）。测试文明 {@code buildtest}：一座 3 × 3 × 3 的小屋（石地面、石墙、一扇门，25 个方块 → 2 段），
 * 石头折算成石砖、门折算成橡木板；模板在第一个用例里从世界里拓下来。神庙扩建（TC-86）用希腊的真实数据，单独一批。
 * 场地 17 × 16 × 36，地面 y = 1；小屋地块在 (6..8, 2, 10..12)，正面朝北。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageBuildGameTests {
    private static final String BATCH = "village_build";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    static final String CIV = "buildtest";
    static final String HUT = "buildtest:hut";
    static final String SHRINE = "buildtest:shrine_athena";
    static final String HUT_TEMPLATE = "mythic_age:village/buildtest/hut";
    private static final String BRICKS = "minecraft:stone_bricks", PLANKS = "minecraft:oak_planks";

    private VillageBuildGameTests() {
    }

    // ───────────── 搭建 ─────────────

    static CivVillageData civ() {
        CivVillageData d = CivVillageData.parse(CIV, JsonParser.parseString("""
                {"schedule": {"segments": [{"id": "work", "from": 0, "to": 24000}]},
                 "storage": {"base": 64, "unit": 16},
                 "rations": {"floor": 0, "portions": {"minecraft:bread": 1.0}},
                 "residents": {"fallback": "buildtest:worker", "male": ["Tekton"], "female": ["Tektaina"]},
                 "buildCosts": {"free": ["minecraft:air"],
                                "rules": [{"match": "minecraft:stone", "cost": {"minecraft:stone_bricks": 1}},
                                          {"match": "minecraft:oak_door", "cost": {"minecraft:oak_planks": 1}}]}}""")
                .getAsJsonObject());
        VillageRecipe brick = VillageRecipe.parse("buildtest:brick", CIV, JsonParser.parseString("""
                {"station": "minecraft:crafting_table", "inputs": {"minecraft:cobblestone": 2},
                 "outputs": {"minecraft:stone_bricks": 1}, "stages": [{"anim": "work", "ticks": 20}]}""").getAsJsonObject());
        return d.withProfessions(Map.of("buildtest:worker", new CivVillageData.Profession("buildtest:worker",
                Set.of("craft", "build"), "archaic", false)), Map.of(brick.id(), brick));
    }

    static BuildingSpec spec(String id, String god, String plotKind) {
        return BuildingSpec.parse(JsonParser.parseString("""
                {"id": "%s", "civ": "buildtest", "template": "%s", "size": [3, 3, 3], "floor_y": 1, "stage": "classical",
                 "generated": false, "plot_kinds": ["%s"], "priority": 10, "unique": false, "god": "%s",
                 "provides": {"storage": 2}, "anchors": {"home": [[1, 1, 1]]},
                 "door": {"pos": [1, 1, 0], "facing": "north"}}""".formatted(id, HUT_TEMPLATE, plotKind, god)).getAsJsonObject());
    }

    /** 小屋模板：在场地角上搭一座、拓下来、拆掉（模板管理器缓存后，后面的用例直接用）。 */
    static void ensureHut(GameTestHelper h) {
        VillageData.overrideForTest(CIV, civ());
        VillageBuildings.putForTest(HUT, spec(HUT, "", "hut"));
        VillageBuildings.putForTest(SHRINE, spec(SHRINE, "athena", "shrine"));
        StructureTemplateManager m = h.getLevel().getServer().getStructureManager();
        if (m.get(ResourceLocation.parse(HUT_TEMPLATE)).isPresent()) {
            return;
        }
        BlockPos o = new BlockPos(12, 2, 30);
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                h.setBlock(o.offset(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 2; y++) {
                    boolean wall = x != 1 || z != 1;
                    h.setBlock(o.offset(x, y, z), wall ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        h.setBlock(o.offset(1, 1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        h.setBlock(o.offset(1, 2, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        StructureTemplate t = m.getOrCreate(ResourceLocation.parse(HUT_TEMPLATE));
        t.fillFromWorld(h.getLevel(), h.absolutePos(o), new Vec3i(3, 3, 3), false, Blocks.STRUCTURE_VOID);
        Blueprint.clear();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                for (int y = 0; y <= 2; y++) {
                    h.setBlock(o.offset(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    static Box absBox(GameTestHelper h, int x0, int y, int z0, int x1, int z1) {
        BlockPos a = h.absolutePos(new BlockPos(x0, y, z0));
        BlockPos b = h.absolutePos(new BlockPos(x1, y, z1));
        return Box.of(new Pos(a.getX(), a.getY(), a.getZ()), new Pos(b.getX(), b.getY(), b.getZ()));
    }

    /** 测试文明的村庄：远古阶段（不会自己选项目），一块小屋地块、一块祠地块；已建范围覆盖整个场地。 */
    static VillageWorkGameTests.TV village(GameTestHelper h) {
        ensureHut(h);
        VillageWorkGameTests.floor(h);
        Pos center = VillageWorkGameTests.abs(h, 8, 2, 2);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        Box built = new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 5);
        Map<String, List<Pos>> anchors = new LinkedHashMap<>();
        anchors.put("center", List.of(center));
        List<VillagePlan.Plot> plots = List.of(
                new VillagePlan.Plot("classical_hut_1", "hut", "classical", absBox(h, 6, 2, 10, 8, 12), "north"),
                new VillagePlan.Plot("classical_shrine_1", "shrine", "classical", absBox(h, 6, 2, 20, 8, 22), "north"));
        VillageInfoPiece.Info info = new VillageInfoPiece.Info(CIV, "zeus", "test", 1, "test", center, anchors, List.of(built), plots);
        UUID id = UUID.randomUUID();
        VillageRegistry.Village v = new VillageRegistry.Village(id, GlobalPos.of(h.getLevel().dimension(),
                VillageInfoPiece.block(center)), CIV, "zeus", VillageRegistry.Origin.GENERATED, null, "", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        VillagePlans.Plan p = new VillagePlans.Plan(id, h.getLevel().dimension(), "test", h.getLevel().getRandom().nextLong(),
                info, true);
        VillagePlans.get(h.getLevel().getServer()).put(p);
        VillageScheduler.forceRunningForTest(id, true);
        VillageWorkGameTests.TV tv = new VillageWorkGameTests.TV(v, p);
        VillageWorkGameTests.takeOver(h, tv);
        return tv;
    }

    static VillageContext ctx(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
    }

    static VillageEconomy.State state(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageEconomy.get(h.getLevel().getServer()).state(tv.id());
    }

    /** 开一座小屋并备齐料（石砖 23、橡木板 2；多给的留在库里）。 */
    static BuildProject hut(GameTestHelper h, VillageWorkGameTests.TV tv, int bricks, int planks) {
        VillageEconomy.State s = state(h, tv);
        s.stock.deposit(BRICKS, bricks);
        s.stock.deposit(PLANKS, planks);
        BuildProject p = Construction.start(ctx(h, tv), new ProjectPicker.Pick(HUT, "classical_hut_1", "", "")).orElseThrow();
        Construction.tick(ctx(h, tv));
        return p;
    }

    static void cleanup(GameTestHelper h, VillageWorkGameTests.TV tv) {
        Construction.clearForTest(tv.id());
        VillageWorkGameTests.cleanup(h, tv);
    }

    static BlockPos rel(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    /** 小屋的门（下半）在 (7, 2, 10)。 */
    static boolean doorAt(GameTestHelper h) {
        return h.getLevel().getBlockState(rel(h, 7, 2, 10)).is(Blocks.OAK_DOOR)
                && h.getLevel().getBlockState(rel(h, 7, 3, 10)).is(Blocks.OAK_DOOR);
    }

    static FakePlayer player(GameTestHelper h, String name, BlockPos at) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(Vec3.atBottomCenterOf(at));
        OnlinePlayers.forTest(p, true);
        return p;
    }

    // ───────────── 用例 ─────────────

    /** TC-T19-80（验收：测试文明跑通一次施工）+ TC-T19-92：备料 → 居民锤打 → 落段 → 验收 → 地块 built、范围并入、容量按 provides。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_full_cycle(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            BuildProject p = hut(h, tv, 30, 4);
            h.assertTrue(p.state == BuildProject.State.BUILDING, "料齐开工：" + p.state + " 缺 " + p.missing());
            h.assertTrue(p.segments == 2, "25 个方块切成 2 段：" + p.segments);
            ResidentEntity r = VillageWorkGameTests.resident(h, tv, "buildtest:worker", 8, 6);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().map(j -> j.type().equals(BuildJob.ID)).orElse(false), "居民领到施工单：" + r.job());
            r.fastForwardForTest();
            r.tickJobForTest();
            h.assertTrue(Construction.queued(tv.id(), p.id, 0), "锤完进落段队列");
            h.assertTrue(!p.placed.get(0), "落段前不算已放");
            Construction.drain(h.getLevel().getServer());
            h.assertTrue(p.placed.get(0) && doorAt(h), "第一段落下（含门的两半）");
            h.assertTrue(Construction.drop(ctx(h, tv), p, 1) == Construction.Result.PLACED, "第二段");
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(s.projects.isEmpty() && s.constructed.stream().anyMatch(b -> b.building().equals(HUT)), "落成");
            h.assertTrue(s.builtPlots.contains("classical_hut_1"), "地块 built");
            h.assertTrue(s.stock.count(BRICKS) == 7 && s.stock.count(PLANKS) == 2, "正好扣掉账单：" + s.stock.items());
            VillagePlans.Plan plan = VillagePlans.get(h.getLevel().getServer()).byVillage(tv.id()).orElseThrow();
            Box box = s.constructed.get(0).box();
            h.assertTrue(plan.info().built().contains(box), "已建范围并入");
            BlockPos home = rel(h, 7, 2, 11);
            h.assertTrue(plan.anchors("home").contains(new Pos(home.getX(), home.getY(), home.getZ())), "锚点生效：" + plan.anchors("home"));
            h.assertTrue(VillagePlans.get(h.getLevel().getServer()).at(h.getLevel().dimension(), home.getX(), home.getZ()).isPresent(),
                    "空间索引看得到");
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(s.stock.capacity() == 64 + 16 * 2, "TC-92 容量按 provides.storage：" + s.stock.capacity());
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-81：缺料时停在备料，缺口显示在村务里，缺的料会被生产补上。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_short_of_materials(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            h.setBlock(new BlockPos(9, 2, 7), Blocks.CRAFTING_TABLE);
            VillageEconomy.State s = state(h, tv);
            s.stock.deposit("minecraft:cobblestone", 10);
            BuildProject p = hut(h, tv, 5, 2);
            h.assertTrue(p.state == BuildProject.State.STOCKING, "缺料：" + p.state);
            h.assertTrue(p.missing().equals(Map.of(BRICKS, 18)), "缺 18 块石砖：" + p.missing());
            h.assertTrue(s.stock.count(BRICKS) == 0 && s.stock.escrowed(BRICKS) == 5, "已有的先托管");
            List<AffairsView.Line> lines = VillageAffairs.projectLines(s);
            h.assertTrue(lines.stream().anyMatch(l -> l.key().equals("mythic_age.village.project.missing")), "村务显示缺口：" + lines);
            VillageWorkGameTests.TV tv2 = tv;
            Map<String, List<Pos>> a = new LinkedHashMap<>(VillagePlans.get(h.getLevel().getServer()).byVillage(tv2.id()).orElseThrow().info().anchors());
            a.put("workstation", List.of(VillageWorkGameTests.abs(h, 9, 2, 7)));
            VillagePlans.Plan plan = VillagePlans.get(h.getLevel().getServer()).byVillage(tv.id()).orElseThrow();
            VillageInfoPiece.Info i = plan.info();
            VillagePlans.get(h.getLevel().getServer()).updateInfo(tv.id(), new VillageInfoPiece.Info(i.civ(), i.patron(), i.layout(),
                    i.layoutVersion(), i.templateSet(), i.center(), a, i.built(), i.plots()));
            VillageScheduler.runtime(tv.id()).invalidateStations();
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            List<Job> crafts = JobType.REGISTRY.get("craft").candidates(ctx(h, tv));
            h.assertTrue(crafts.stream().anyMatch(j -> j.ref().startsWith("buildtest:brick")), "缺的石砖变成生产需求：" + crafts);
            s.stock.deposit(BRICKS, 18);
            Construction.tick(ctx(h, tv));
            h.assertTrue(p.state == BuildProject.State.BUILDING, "补齐后开工");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-82 / 83（验收：挡路停工不拆）：段内有玩家的方块 → 停工记坐标、方块不动；拿走后恢复。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_blocked_then_resumes(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            BuildProject p = hut(h, tv, 30, 4);
            h.setBlock(new BlockPos(6, 2, 11), Blocks.OAK_PLANKS);
            Construction.Result r = Construction.drop(ctx(h, tv), p, 0);
            BlockPos at = rel(h, 6, 2, 11);
            h.assertTrue(r == Construction.Result.BLOCKED && p.state == BuildProject.State.BLOCKED, "停工：" + r);
            h.assertTrue(p.blockedAt.equals(new Pos(at.getX(), at.getY(), at.getZ())), "记坐标：" + p.blockedAt);
            h.assertTrue(h.getLevel().getBlockState(at).is(Blocks.OAK_PLANKS), "玩家方块不拆");
            h.assertTrue(!doorAt(h) && !p.placed.get(0), "整段没落");
            h.assertTrue(state(h, tv).stock.escrowed(BRICKS) == 23, "料没扣");
            Construction.tick(ctx(h, tv));
            h.assertTrue(p.state == BuildProject.State.BLOCKED, "还挡着就不恢复");
            h.setBlock(new BlockPos(6, 2, 11), Blocks.AIR);
            Construction.tick(ctx(h, tv));
            h.assertTrue(p.state == BuildProject.State.BUILDING, "拿走后恢复");
            h.assertTrue(Construction.drop(ctx(h, tv), p, 0) == Construction.Result.PLACED && doorAt(h), "复工落段");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-84：施工中重启（存档往返、工地缓存清空）：已放的段不重放，托管不重复。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_survives_restart(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            BuildProject p = hut(h, tv, 30, 4);
            h.assertTrue(Construction.drop(ctx(h, tv), p, 0) == Construction.Result.PLACED, "先落一段");
            VillageEconomy eco = VillageEconomy.get(h.getLevel().getServer());
            int escBefore = eco.state(tv.id()).stock.escrowed(BRICKS);
            int stockBefore = eco.state(tv.id()).stock.count(BRICKS);
            eco.replaceForTest(tv.id(), VillageEconomy.roundTripForTest(eco, h.getLevel().registryAccess()));
            Construction.clearForTest(tv.id());
            VillageEconomy.State s = eco.state(tv.id());
            BuildProject q = s.projects.get(0);
            h.assertTrue(q.id == p.id && q.placed.get(0) && q.state == BuildProject.State.BUILDING, "按项目 id 恢复");
            h.assertTrue(s.stock.escrowed(BRICKS) == escBefore && s.stock.count(BRICKS) == stockBefore, "托管不重复、不丢");
            Construction.tick(ctx(h, tv));
            h.assertTrue(s.stock.escrowed(BRICKS) == escBefore, "再备料也不多锁");
            h.assertTrue(Construction.drop(ctx(h, tv), q, 0) == Construction.Result.GONE, "已放的段不重放");
            h.assertTrue(Construction.drop(ctx(h, tv), q, 1) == Construction.Result.PLACED, "续上第二段");
            h.assertTrue(s.projects.isEmpty() && s.stock.count(BRICKS) == 7, "落成、账单正好：" + s.stock.items());
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-85：未运行时不落块；回来后累积施工储备，派单时锤打时长按倍率缩短、储备扣掉省下的刻数。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_offline_bank(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            BuildProject p = hut(h, tv, 30, 4);
            VillageScheduler.forceRunningForTest(tv.id(), false);
            h.assertTrue(!VillageScheduler.processNowForTest(h.getLevel(), tv.id()), "未运行");
            h.assertTrue(!doorAt(h) && p.placed.isEmpty(), "不落块");
            VillageEconomy.State s = state(h, tv);
            s.lastRealTick = WorldClock.now(h.getLevel().getServer()) - 5000;
            VillageScheduler.forceRunningForTest(tv.id(), true);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(s.constructionBank >= 4900 && s.constructionBank <= 5000, "累积储备：" + s.constructionBank);
            long bank = s.constructionBank;
            Job j = JobType.REGISTRY.get(BuildJob.ID).candidates(ctx(h, tv)).get(0);
            int base = j.steps().get(1).ticks();
            Job fast = BuildJob.withBankedTicks(ctx(h, tv), j);
            int ticks = fast.steps().get(1).ticks();
            h.assertTrue(base == 160 && ticks == 80, "按倍率 2 缩短：" + base + " → " + ticks);
            h.assertTrue(s.constructionBank == bank - 80, "储备扣掉省下的刻数：" + s.constructionBank);
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-87：古典立祀登记了雅典娜 → 选项目时插队，建在古典 shrine 地块。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_shrine_for_registered_god(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
            reg.update(tv.village().withStage(Age.CLASSICAL).withRite(Age.CLASSICAL, Age.CLASSICAL, "athena"));
            Optional<BuildProject> p = Construction.daily(ctx(h, tv));
            h.assertTrue(p.isPresent() && p.get().building.equals(SHRINE), "先建雅典娜祠：" + p.map(x -> x.building));
            h.assertTrue(p.get().plot.equals("classical_shrine_1") && p.get().god.equals("athena"), "建在古典 shrine 地块");
            h.assertTrue(Construction.daily(ctx(h, tv)).isEmpty(), "每村同时一个项目");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-88：落成提示发给 64 格内的玩家；seenBy 里不在场的玩家下次进村补发；文字不含恩宠、时代。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_notice(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        FakePlayer near = player(h, "near", rel(h, 8, 2, 30));
        FakePlayer far = player(h, "far", rel(h, 8, 2, 30).offset(300, 0, 0));
        try {
            VillageEconomy.State s = state(h, tv);
            s.seenBy.add(far.getUUID());
            BuildProject p = hut(h, tv, 30, 4);
            Construction.drop(ctx(h, tv), p, 0);
            Construction.drop(ctx(h, tv), p, 1);
            h.assertTrue(BuildNotices.sentForTest(s, HUT, near.getUUID()), "在场的收到");
            h.assertTrue(!BuildNotices.sentForTest(s, HUT, far.getUUID()), "不在场的还没收到");
            h.assertTrue(BuildNotices.onEnter(far, tv.village()) == 1 && BuildNotices.sentForTest(s, HUT, far.getUUID()), "进村补发");
            h.assertTrue(BuildNotices.onEnter(far, tv.village()) == 0, "不重复补发");
            var msg = BuildNotices.message(tv.village(), VillageBuildings.all().get(HUT), "");
            String key = ((TranslatableContents) msg.getContents()).getKey();
            h.assertTrue(key.equals("mythic_age.village.built"), "提示语言键：" + key);
            String zh = io.github.happysnappingturtle.mythicage.content.LangKeys.all().get(key).zh();
            h.assertTrue(zh != null && !zh.contains("恩宠") && !zh.contains("时代"), "不含恩宠、时代：" + zh);
            h.succeed();
        } finally {
            OnlinePlayers.forTest(near, false);
            OnlinePlayers.forTest(far, false);
            cleanup(h, tv);
        }
    }

    /** TC-T19-89：导出的古典 / 英雄 / 神话模板都能读到，尺寸与建筑数据一致，账单非空、方块都被折算表覆盖。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void exported_templates_load(GameTestHelper h) {
        CivVillageData greek = VillageData.civ("greek").orElseThrow();
        StructureTemplateManager m = h.getLevel().getServer().getStructureManager();
        int n = 0;
        for (BuildingSpec s : VillageBuildings.all().values()) {
            if (s.generated() || !s.civ().equals("greek")) {
                continue;
            }
            n++;
            Optional<StructureTemplate> t = m.get(ResourceLocation.parse(s.template()));
            h.assertTrue(t.isPresent(), "模板读得到：" + s.template());
            Vec3i size = t.get().getSize();
            h.assertTrue(size.getX() == s.sizeX() && size.getY() == s.sizeY() && size.getZ() == s.sizeZ(),
                    s.id() + " 尺寸一致：" + size);
            List<Blueprint.Cell> cells = Blueprint.load(h.getLevel(), s.template()).orElseThrow();
            BuildCosts costs = BuildSite.costsFor(greek.buildCosts(), s);
            List<String> blocks = new ArrayList<>();
            for (Blueprint.Cell c : cells) {
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(c.state().getBlock()).toString();
                h.assertTrue(costs.covers(id), s.id() + " 的 " + id + " 没有折算");
                blocks.add(id);
            }
            h.assertTrue(!costs.sum(blocks).isEmpty(), s.id() + " 账单非空");
        }
        h.assertTrue(n >= 20, "古典起的施工建筑：" + n);
        h.succeed();
    }

    /** TC-T19-90：验收发现门被拆了 → 含门的段标回未放、账单增加、回到备料；补料后重放并落成。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_inspection_repairs(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            BuildProject p = hut(h, tv, 40, 6);
            Construction.drop(ctx(h, tv), p, 0);
            h.setBlock(new BlockPos(7, 3, 10), Blocks.AIR);
            h.setBlock(new BlockPos(7, 2, 10), Blocks.AIR);
            Construction.drop(ctx(h, tv), p, 1);
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(s.projects.contains(p) && p.state == BuildProject.State.STOCKING, "回到备料：" + p.state);
            h.assertTrue(!p.placed.get(0) && p.placed.get(1), "含门的段标回未放");
            h.assertTrue(p.bill().get(BRICKS) == 23 + 16 - 2 && p.bill().get(PLANKS) == 4, "账单追加第一段的成本：" + p.bill());
            Construction.tick(ctx(h, tv));
            h.assertTrue(p.state == BuildProject.State.BUILDING, "库里还有料：" + p.missing() + " " + s.stock.items());
            h.assertTrue(Construction.drop(ctx(h, tv), p, 0) == Construction.Result.PLACED && doorAt(h), "重放");
            h.assertTrue(s.projects.isEmpty(), "落成");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-91：段内站着玩家 → 这一刻不落；离开后再落。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_waits_for_player(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        FakePlayer pl = player(h, "inside", rel(h, 6, 3, 12));
        try {
            BuildProject p = hut(h, tv, 30, 4);
            Construction.drop(ctx(h, tv), p, 0);
            h.assertTrue(Construction.drop(ctx(h, tv), p, 1) == Construction.Result.WAIT && !p.placed.get(1), "有人站着就等");
            pl.moveTo(Vec3.atBottomCenterOf(rel(h, 8, 2, 30)));
            h.assertTrue(Construction.drop(ctx(h, tv), p, 1) == Construction.Result.PLACED, "离开后落下");
            h.succeed();
        } finally {
            OnlinePlayers.forTest(pl, false);
            cleanup(h, tv);
        }
    }

    /** TC-T19-93：锤到一半被打断 → 段没落、料没扣、不进队列；下一次重新派单。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_interrupted(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            BuildProject p = hut(h, tv, 30, 4);
            ResidentEntity r = VillageWorkGameTests.resident(h, tv, "buildtest:worker", 8, 6);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().map(j -> j.type().equals(BuildJob.ID)).orElse(false), "领到施工单");
            r.abortJob(false);
            h.assertTrue(p.placed.isEmpty() && !Construction.queued(tv.id(), p.id, 0), "没落、没进队列");
            h.assertTrue(state(h, tv).stock.escrowed(BRICKS) == 23, "料没扣");
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().map(j -> j.type().equals(BuildJob.ID)).orElse(false), "重新派单");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-94：工地里有一棵天然的树：模板范围内的树干按空气格清掉，建筑上方剩下的那截也清掉。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_clears_cut_trunk(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            for (int y = 2; y <= 7; y++) {
                h.setBlock(new BlockPos(7, y, 11), Blocks.OAK_LOG);
            }
            BuildProject p = hut(h, tv, 30, 4);
            Construction.drop(ctx(h, tv), p, 0);
            Construction.drop(ctx(h, tv), p, 1);
            for (int y = 2; y <= 7; y++) {
                h.assertTrue(h.getLevel().getBlockState(rel(h, 7, y, 11)).isAir(), "y = " + y + " 的树干清掉了");
            }
            h.assertTrue(state(h, tv).projects.isEmpty(), "落成");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-95：地块地面是水（生成时预留在了湖面上）→ 不选它；换成实地后才选。 */
    @GameTest(template = T, batch = BATCH)
    public static void build_skips_wet_plot(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            VillageRegistry.get(h.getLevel().getServer()).update(tv.village().withStage(Age.CLASSICAL));
            for (int x = 6; x <= 8; x++) {
                for (int z = 10; z <= 12; z++) {
                    h.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
                }
            }
            Box plot = absBox(h, 6, 2, 10, 8, 12);
            h.assertTrue(!Construction.groundOk(h.getLevel(), plot), "水面不算实地");
            h.assertTrue(Construction.daily(ctx(h, tv)).isEmpty(), "不选水面上的地块");
            for (int x = 6; x <= 8; x++) {
                for (int z = 10; z <= 12; z++) {
                    h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                }
            }
            h.assertTrue(Construction.daily(ctx(h, tv)).map(x -> x.building.equals(HUT)).orElse(false), "实地上才建");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    // ───────────── 神庙扩建（希腊真实数据，单独一批：模板比场地宽） ─────────────

    /**
     * TC-T19-86：远古小神庙 → 中神庙（原地替换）：选项目认出旧神庙；先拆后放，神像旁的段排在最后；拆下的方块折算回村库；
     * 祭司居所跟着神像走；建成后 T8a 认定 2 级（前柱式）。
     */
    @GameTest(template = T, batch = "village_build_temple", timeoutTicks = 1200)
    public static void temple_replace_to_classical(GameTestHelper h) {
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        BuildingSpec archaic = specs.get("greek:acropolis_archaic");
        BlockPos o = rel(h, 6, 2, 2);
        for (int x = -6; x < 30; x++) {
            for (int z = -2; z < 30; z++) {
                h.getLevel().setBlockAndUpdate(o.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                // 模板比场地宽：拆掉场地四周的屏障，否则会被当成别人的方块停工
                for (int y = 0; y < 20; y++) {
                    if (h.getLevel().getBlockState(o.offset(x, y, z)).is(Blocks.BARRIER)) {
                        h.getLevel().setBlockAndUpdate(o.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        StructureTemplate t = h.getLevel().getServer().getStructureManager().get(ResourceLocation.parse(archaic.template())).orElseThrow();
        t.placeInWorld(h.getLevel(), o, o, new StructurePlaceSettings(), h.getLevel().getRandom(), 2);
        Pos origin = new Pos(o.getX(), o.getY(), o.getZ());
        BuildPlacement.Placed at = new BuildPlacement.Placed(archaic.id(), origin, Turn.R0,
                TemplateTransform.worldBox(origin, archaic.sizeX(), archaic.sizeY(), archaic.sizeZ(), Turn.R0));
        Map<String, List<Pos>> anchors = new LinkedHashMap<>(BuildPlacement.worldAnchors(archaic, at));
        Pos ritual = anchors.get("ritual").get(0);
        Pos center = ritual.offset(6, 0, 0);
        anchors.put("center", List.of(center));
        VillageInfoPiece.Info info = new VillageInfoPiece.Info("greek", "", "test", 1, "test", center, anchors,
                List.of(at.box()), List.of());
        UUID id = UUID.randomUUID();
        VillageRegistry.Village v = new VillageRegistry.Village(id, GlobalPos.of(h.getLevel().dimension(), VillageInfoPiece.block(center)),
                "greek", "", VillageRegistry.Origin.GENERATED, null, "", -1).withStage(Age.CLASSICAL);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        VillagePlans.get(h.getLevel().getServer()).put(new VillagePlans.Plan(id, h.getLevel().dimension(), "test", 1L, info, true));
        VillageScheduler.forceRunningForTest(id, true);
        VillageWorkGameTests.TV tv = new VillageWorkGameTests.TV(v, VillagePlans.get(h.getLevel().getServer()).byVillage(id).orElseThrow());
        VillageEconomy.State s = state(h, tv);
        s.initialized = true;
        s.stock.setCapacity(100000);
        s.stock.putUnchecked("mythic_age:olive_flatbread", 40);
        VillageContext ctx = ctx(h, tv);
        BuildProject p = Construction.daily(ctx).orElse(null);
        h.assertTrue(p != null && p.building.equals("greek:acropolis_classical") && p.replaces.equals(archaic.id()),
                "认出旧神庙并选中中神庙：" + (p == null ? null : p.building));
        BuildCosts costs = ctx.data().buildCosts();
        p.bill().forEach((k, n) -> s.stock.putUnchecked(costs.representative(k), n));
        Construction.tick(ctx(h, tv));
        h.assertTrue(p.state == BuildProject.State.BUILDING, "料齐开工");
        BuildSite site = Construction.site(ctx(h, tv), p).orElseThrow();
        h.assertTrue(site.segments.get(0).kind() == BuildSegments.Kind.DEMOLISH, "先拆");
        BuildSegments.Segment last = site.segments.get(site.segments.size() - 1);
        h.assertTrue(last.cells().stream().allMatch(BuildSegments.Cell::last), "神像、祭司站位、仪式位附近的段在最后");
        for (int i = 0; i < p.segments; i++) {
            Construction.Result r = Construction.drop(ctx(h, tv), p, i);
            h.assertTrue(r == Construction.Result.PLACED, "第 " + i + " 段：" + r + " " + p.blockedAt + " " + p.missing());
        }
        h.assertTrue(s.projects.isEmpty(), "落成（验收通过）");
        h.assertTrue(!p.returnedGiven.isEmpty(), "拆下的方块折算回村库：" + p.returnedGiven);
        h.assertTrue(s.constructed.stream().anyMatch(b -> b.building().equals("greek:acropolis_classical")), "记下中神庙");
        VillagePlans.Plan plan = VillagePlans.get(h.getLevel().getServer()).byVillage(id).orElseThrow();
        BuildingSpec classical = specs.get("greek:acropolis_classical");
        Pos guardian = BuildPlacement.worldAnchors(classical, BuildPlacement.replacing(classical, archaic, at)).get("guardian").get(0);
        h.assertTrue(plan.anchors("guardian").equals(List.of(guardian)), "神像站位换成新的：" + plan.anchors("guardian"));
        h.assertTrue(!plan.info().built().contains(at.box()), "旧包围盒去掉");
        // T8a：在仪式位换上玩家的神坛，扫描认定等级
        BlockPos altar = VillageInfoPiece.block(ritual);
        h.getLevel().setBlockAndUpdate(altar, GreekTemple.GREEK_ALTAR.get().defaultBlockState()
                .setValue(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock.FACING, Direction.WEST));
        ShrineService.bindForTest(h.getLevel(), altar, UUID.randomUUID(), "tester", Age.CLASSICAL, Direction.WEST);
        h.succeedWhen(() -> {
            if (!(h.getLevel().getBlockEntity(altar) instanceof GreekAltarBlockEntity be) || !be.hasResult()
                    || TempleService.pending(h.getLevel(), altar)) {
                throw new net.minecraft.gametest.framework.GameTestAssertException("扫描尚未完成");
            }
            int level = be.verdict(TempleService.rules()).grantedLevel();
            h.assertTrue(level == 2, "中神庙满足 T8a 2 级，实际 " + level + "，统计 " + be.survey());
            VillageWorkGameTests.cleanup(h, tv);
            Construction.clearForTest(id);
            // 神庙比场地宽：场地外建的部分要拆掉，否则会留在世界里干扰后面复用这块地的用例（如找露头大理石）
            for (BlockPos q : BlockPos.betweenClosed(o.offset(-6, -1, -2), o.offset(30, 20, 30))) {
                if (!h.getLevel().getBlockState(q).isAir()) {
                    h.getLevel().setBlock(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
        });
    }
}
