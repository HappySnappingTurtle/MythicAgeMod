package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.GreekPrayer;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.ResidentPrayer;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.rite.MigrationRules;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillagePopulation;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.ResidentMigration;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * T19 阶段 ④a：移民（TC-T19-100～104、113、115）、祈祷迁移（TC-105、106、114）、性能预算（TC-112）。
 * 移民用希腊的真实数据（阶段人口表在 {@code village_stage/greek.json}）；场地 17 × 16 × 36，地面 y = 1。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageMigrationGameTests {
    private static final String BATCH = "village_migration";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final String GREEK = "greek";

    private VillageMigrationGameTests() {
    }

    private static Pos abs(GameTestHelper h, int x, int y, int z) {
        return VillageWorkGameTests.abs(h, x, y, z);
    }

    /** 希腊村庄：床位锚点 homes 个（沿 z = 30 排开）、村口 (8, 2, 20)、口粮充足；阶段 stage。 */
    static VillageWorkGameTests.TV village(GameTestHelper h, int homes, Age stage, Map<String, List<Pos>> extra, List<Box> extraBuilt) {
        VillageWorkGameTests.floor(h);
        Map<String, List<Pos>> a = new LinkedHashMap<>(extra);
        List<Pos> hs = new ArrayList<>();
        for (int i = 0; i < homes; i++) {
            hs.add(abs(h, 2 + 3 * i, 2, 30));
        }
        a.put("home", hs);
        a.put("entrance", List.of(abs(h, 8, 2, 20)));
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        VillageWorkGameTests.TV tv = VillageWorkGameTests.village(h, GREEK, a,
                new Box(o.getX() + 6, o.getY() + 1, o.getZ(), o.getX() + 10, o.getY() + 4, o.getZ() + 4));
        if (!extraBuilt.isEmpty()) {
            VillagePlans plans = VillagePlans.get(h.getLevel().getServer());
            VillageInfoPiece.Info i = tv.plan().info();
            List<Box> built = new ArrayList<>(i.built());
            built.addAll(extraBuilt);
            plans.updateInfo(tv.id(), new VillageInfoPiece.Info(i.civ(), i.patron(), i.layout(), i.layoutVersion(),
                    i.templateSet(), i.center(), i.anchors(), built, i.plots()));
        }
        // 先以远古接管（接管时会调度一次，不能让它顺手来人），再设阶段
        VillageWorkGameTests.takeOver(h, tv);
        VillageRegistry.get(h.getLevel().getServer()).update(VillageRegistry.get(h.getLevel().getServer()).byId(tv.id())
                .orElseThrow().withStage(stage));
        state(h, tv).stock.putUnchecked("mythic_age:olive_flatbread", 64);
        return new VillageWorkGameTests.TV(VillageRegistry.get(h.getLevel().getServer()).byId(tv.id()).orElseThrow(),
                VillagePlans.get(h.getLevel().getServer()).byVillage(tv.id()).orElseThrow());
    }

    static VillageEconomy.State state(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageEconomy.get(h.getLevel().getServer()).state(tv.id());
    }

    static VillageContext ctx(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
    }

    /** 名册里放一个住在第 i 张床的居民。 */
    static ResidentEntity settler(GameTestHelper h, VillageWorkGameTests.TV tv, String profession, int home) {
        BlockPos bed = h.absolutePos(new BlockPos(2 + 3 * home, 2, 30));
        return io.github.happysnappingturtle.mythicage.village.resident.Residents.spawn(h.getLevel(), tv.village(),
                io.github.happysnappingturtle.mythicage.village.work.VillageData.civ(GREEK).orElseThrow(), profession, bed, bed,
                false).orElseThrow();
    }

    static Optional<ResidentEntity> migrant(GameTestHelper h, VillageWorkGameTests.TV tv) {
        UUID id = state(h, tv).migrant;
        return id == null ? Optional.empty() : Optional.ofNullable(h.getLevel().getEntity(id))
                .filter(e -> e instanceof ResidentEntity).map(e -> (ResidentEntity) e);
    }

    static void cleanup(GameTestHelper h, VillageWorkGameTests.TV tv) {
        migrant(h, tv).ifPresent(ResidentEntity::discard);
        VillagePopulation.setNeedsHousing(tv.id(), false);
        VillageWorkGameTests.cleanup(h, tv);
    }

    /** 陶坊：只登记锚点和包围盒（认建筑只看这两样，不需要真的方块）。 */
    static Map.Entry<Map<String, List<Pos>>, Box> pottery(GameTestHelper h) {
        BuildingSpec s = VillageBuildings.all().get("greek:pottery");
        Pos origin = abs(h, 1, 2, 8);
        BuildPlacement.Placed p = new BuildPlacement.Placed(s.id(), origin, Turn.R0,
                TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), Turn.R0));
        return Map.entry(BuildPlacement.worldAnchors(s, p), p.box());
    }

    // ───────────── 移民 ─────────────

    /** TC-T19-100（验收：从村口走进来、走到空床才登记）+ TC-103（按空缺分职业）+ TC-115（岗位）。 */
    @GameTest(template = T, batch = BATCH)
    public static void migrant_walks_in_then_registers(GameTestHelper h) {
        var pot = pottery(h);
        VillageWorkGameTests.TV tv = village(h, 3, Age.CLASSICAL, pot.getKey(), List.of(pot.getValue()));
        try {
            settler(h, tv, "greek:potter", 0);
            Map<String, Integer> jobs = ResidentMigration.jobs(ctx(h, tv));
            h.assertTrue(jobs.equals(Map.of("greek:potter", 2)), "TC-115 岗位 = 陶坊陶工 2：" + jobs);
            MigrationRules.Decision d = ResidentMigration.step(ctx(h, tv));
            h.assertTrue(d == MigrationRules.Decision.MIGRATE, "该来人了：" + d);
            ResidentEntity m = migrant(h, tv).orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("没有在途移民"));
            BlockPos entrance = h.absolutePos(new BlockPos(8, 2, 20));
            double dist = Math.hypot(m.getX() - entrance.getX() - 0.5, m.getZ() - entrance.getZ() - 0.5);
            h.assertTrue(dist >= 7 && dist <= 13, "在村口外 8–12 格出现：" + dist);
            h.assertTrue(m.inTransit() && !state(h, tv).roster.containsKey(m.getUUID()), "还没进名册");
            h.assertTrue(m.profession().equals("greek:potter"), "TC-103 空缺的陶工岗：" + m.profession());
            h.assertTrue(ResidentMigration.step(ctx(h, tv)) == MigrationRules.Decision.WAITING, "同一时间只来 1 人");
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(m.job().map(j -> j.type().equals(ResidentMigration.ARRIVE)).orElse(false), "在途居民只做「走到床位」");
            BlockPos bed = m.home();
            m.moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5);
            m.tickJobForTest();
            m.tickJobForTest();
            h.assertTrue(!m.inTransit() && state(h, tv).roster.containsKey(m.getUUID()), "走到才登记");
            h.assertTrue(state(h, tv).roster.get(m.getUUID()).home().equals(bed), "床位记进名册");
            h.assertTrue(state(h, tv).migrant == null, "在途记录清空");
            h.assertTrue(ResidentMigration.step(ctx(h, tv)) == MigrationRules.Decision.WAITING, "两日内不再来第二个");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-101：没有空床 → 不来人，村务显示「需要更多住居」。 */
    @GameTest(template = T, batch = BATCH)
    public static void migrant_needs_free_bed(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, 1, Age.CLASSICAL, Map.of(), List.of());
        try {
            settler(h, tv, "greek:laborer", 0);
            MigrationRules.Decision d = ResidentMigration.step(ctx(h, tv));
            h.assertTrue(d == MigrationRules.Decision.NO_BED, "没空床：" + d);
            h.assertTrue(state(h, tv).migrant == null && VillagePopulation.needsHousing(tv.id()), "不生成、提示住居");
            h.assertTrue(ResidentMigration.step(ctx(h, tv)) == MigrationRules.Decision.NO_BED, "再查一次仍是没空床");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-102：在途被杀 → 不登记、在途记录作废；远古村不来人。 */
    @GameTest(template = T, batch = BATCH)
    public static void migrant_killed_on_the_way(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, 3, Age.CLASSICAL, Map.of(), List.of());
        try {
            settler(h, tv, "greek:laborer", 0);
            ResidentMigration.step(ctx(h, tv));
            ResidentEntity m = migrant(h, tv).orElseThrow();
            m.hurt(h.getLevel().damageSources().genericKill(), 1000);
            h.assertTrue(!m.isAlive() && state(h, tv).migrant == null, "作废");
            h.assertTrue(!state(h, tv).roster.containsKey(m.getUUID()), "没进名册");
            VillageRegistry.get(h.getLevel().getServer()).update(VillageRegistry.get(h.getLevel().getServer()).byId(tv.id())
                    .orElseThrow().withStage(Age.ARCHAIC).withPopulation(-1, -1));
            h.assertTrue(ResidentMigration.step(ctx(h, tv)) == MigrationRules.Decision.INACTIVE, "远古不来人");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** TC-T19-113：在途居民存档往返：带着床位与职业，仍在途；TC-104：原版村庄仍按 T11a。 */
    @GameTest(template = T, batch = BATCH)
    public static void migrant_survives_reload(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, 3, Age.CLASSICAL, Map.of(), List.of());
        try {
            settler(h, tv, "greek:laborer", 0);
            ResidentMigration.step(ctx(h, tv));
            ResidentEntity m = migrant(h, tv).orElseThrow();
            CompoundTag tag = new CompoundTag();
            m.saveWithoutId(tag);
            ResidentEntity copy = ModEntities.RESIDENT.get().create(h.getLevel());
            copy.load(tag);
            h.assertTrue(copy.inTransit() && copy.home().equals(m.home()) && copy.profession().equals(m.profession()),
                    "在途、床位、职业都在");
            VillageRegistry.Village vanilla = new VillageRegistry.Village(UUID.randomUUID(), GlobalPos.of(h.getLevel().dimension(),
                    h.absolutePos(new BlockPos(1, 2, 1))), GREEK, "zeus", VillageRegistry.Origin.VANILLA, null, "plains", -1);
            h.assertTrue(!VillagePopulation.residentFramework(vanilla), "原版村庄不归居民框架，仍是 T11a 的原版移民");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    // ───────────── 祈祷 ─────────────

    /** 神坛在 (8, 2, 8) 朝南；床位在 z = 30（离祈祷位 ≥ 8 格）。等扫描出结果再继续。 */
    static void altar(GameTestHelper h, UUID owner) {
        BlockPos a = new BlockPos(8, 2, 8);
        h.setBlock(a, GreekTemple.GREEK_ALTAR.get().defaultBlockState().setValue(GreekAltarBlock.FACING, Direction.SOUTH));
        ShrineService.bindForTest(h.getLevel(), h.absolutePos(a), owner, "tester", Age.ARCHAIC, Direction.SOUTH);
    }

    static boolean scanned(GameTestHelper h) {
        BlockPos a = h.absolutePos(new BlockPos(8, 2, 8));
        return h.getLevel().getBlockEntity(a) instanceof GreekAltarBlockEntity be && be.hasResult()
                && !TempleService.pending(h.getLevel(), a);
    }

    /**
     * TC-T19-105（验收：祈祷迁移）+ 106 + 114：窗口内空闲居民被派去祈祷，到位后和原版信徒一样计入采样（同一公式）；
     * 忙着的居民不抽；一班 2400 刻到点离开，同一天不再派。
     */
    @GameTest(template = T, batch = "village_prayer", timeoutTicks = 400)
    public static void residents_pray_like_believers(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, 3, Age.ARCHAIC, Map.of(), List.of());
        UUID owner = UUID.randomUUID();
        altar(h, owner);
        GlobalPos key = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(8, 2, 8)));
        ResidentEntity pious = settler(h, tv, "greek:farmer", 0);
        ResidentEntity busy = settler(h, tv, "greek:potter", 1);
        ResidentEntity second = settler(h, tv, "greek:laborer", 2);
        h.succeedWhen(() -> {
            h.assertTrue(scanned(h), "等神坛扫描");
            try {
                h.getLevel().setDayTime(3000);
                Job other = new Job(999_999, "leisure", VillageWorkGameTests.abs(h, 8, 2, 2), "busy", "",
                        Job.walkActSettle("chat", 100), Map.of(), Map.of(), java.util.Set.of(), io.github.happysnappingturtle.mythicage.core.village.work.Priority.DECOR,
                        0, false, 2000, busy.getUUID());
                busy.assign(other);
                List<Job> jobs = ResidentPrayer.jobs(ctx(h, tv));
                h.assertTrue(jobs.size() == 2 && jobs.stream().noneMatch(x -> busy.getUUID().equals(x.assignee())),
                        "TC-106 忙着的不抽：" + jobs);
                VillageScheduler.processNowForTest(h.getLevel(), tv.id());
                // 实机发现：派第二个人时续预留只传了自己那一格，把第一个人的预留清掉了
                h.assertTrue(second.job().map(x -> x.type().equals(ResidentPrayer.ID)).orElse(false), "第二个人也派到祈祷");
                List<UUID> holders = PrayerService.claimants(key, h.getLevel().getServer());
                h.assertTrue(holders.contains(pious.getUUID()) && holders.contains(second.getUUID()), "两人的预留都在：" + holders);
                Job j = pious.job().orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("没派到祈祷"));
                h.assertTrue(j.type().equals(ResidentPrayer.ID), "派到祈祷：" + j.type());
                BlockPos slot = new BlockPos(j.target().x(), j.target().y(), j.target().z());
                pious.moveTo(slot.getX() + 0.5, slot.getY(), slot.getZ() + 0.5);
                pious.tickJobForTest();
                pious.tickJobForTest();
                h.assertTrue(PrayerService.claimants(key, h.getLevel().getServer()).contains(pious.getUUID()), "占着祈祷位");
                long today = MythDays.today(h.getLevel().getServer());
                h.assertTrue(pious.getData(GreekPrayer.RECORD).prayedOn(today), "记下今天祈祷过（F7 用）");
                double before = PrayerService.pending(key);
                PrayerService.sampleForTest(h.getLevel().getServer());
                h.assertTrue(PrayerService.pending(key) == before, "GA-4：采样不再入账");
                GreekAltarBlockEntity be = (GreekAltarBlockEntity) h.getLevel().getBlockEntity(key.pos());
                double expect = PrayerFormula.credit(1,
                        TempleService.rules().tier(be.verdict(TempleService.rules()).grantedLevel()).coefficient(), 1, 0, 0, 1);
                for (int i = 0; i < ResidentPrayer.SHIFT + 2 && pious.job().isPresent(); i++) {
                    pious.tickJobForTest();
                }
                double got = PrayerService.pending(key) - before;
                h.assertTrue(Math.abs(got - expect) < 1e-9, "TC-GA4-40 祈祷满 600 刻入一笔，与原版信徒同一公式：" + got + " vs " + expect);
                h.assertTrue(be.believersToday(today) == 1, "记为今日信徒");
                h.assertTrue(pious.job().isEmpty(), "TC-114 一次祈祷到点离开");
                h.assertTrue(!PrayerService.claimants(key, h.getLevel().getServer()).contains(pious.getUUID()), "让出祈祷位");
                h.assertTrue(ResidentPrayer.jobs(ctx(h, tv)).stream().noneMatch(x -> pious.getUUID().equals(x.assignee())),
                        "同一天不再派");
            } finally {
                PrayerService.resetForTest(key);
                cleanup(h, tv);
            }
        });
    }

    // ───────────── 性能 ─────────────

    /** TC-T19-112（验收：性能）：30 名居民的测试村跑 2400 刻，调度与居民执行每刻平均耗时 ≤ 0.5 ms。 */
    @GameTest(template = T, batch = "village_perf", timeoutTicks = 2600)
    public static void thirty_residents_within_budget(GameTestHelper h) {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        h.setBlock(new BlockPos(8, 2, 8), net.minecraft.world.level.block.Blocks.COBBLESTONE);
        h.setBlock(new BlockPos(9, 2, 7), net.minecraft.world.level.block.Blocks.CRAFTING_TABLE);
        a.put("rock", List.of(abs(h, 8, 2, 8)));
        a.put("workstation", List.of(abs(h, 9, 2, 7)));
        VillageWorkGameTests.TV tv = VillageWorkGameTests.village(h, a);
        VillageWorkGameTests.takeOver(h, tv);
        for (int i = 0; i < 30; i++) {
            VillageWorkGameTests.resident(h, tv, 1 + (i % 15), 4 + (i / 15) * 10);
        }
        VillageWorkGameTests.state(h, tv).stock.deposit("minecraft:cobblestone", 40);
        long start = VillageScheduler.runtime(tv.id()).perfNanos();
        h.runAfterDelay(2400, () -> {
            double msPerTick = (VillageScheduler.runtime(tv.id()).perfNanos() - start) / 1e6 / 2400;
            io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.info("TC-T19-112：30 名居民每刻 {} ms", String.format("%.4f", msPerTick));
            VillageWorkGameTests.cleanup(h, tv);
            h.assertTrue(msPerTick <= 0.5, "每刻平均 " + msPerTick + " ms");
            h.succeed();
        });
    }
}
