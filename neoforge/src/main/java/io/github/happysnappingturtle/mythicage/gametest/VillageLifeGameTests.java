package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.village.GreekFestivalItems;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.core.village.life.OracleRules;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianGesture;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.ResidentTrade;
import io.github.happysnappingturtle.mythicage.village.economy.ResidentTradeView;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.life.OracleService;
import io.github.happysnappingturtle.mythicage.village.life.PathBudget;
import io.github.happysnappingturtle.mythicage.village.life.VillageLife;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.work.BasicJobTypes;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
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

import static io.github.happysnappingturtle.mythicage.gametest.VillageWorkGameTests.abs;

/**
 * T19 阶段 ⑤：希腊生活与节庆（单元测试用例文档 TC-T19-120～134）。真实的希腊数据（{@code village_civ/greek.json} 的 {@code life}）。
 * 历法日用村庄的 {@code calendarShift} 拨，不动全服的日数；需要真实走路的（游行、接力）各自一个批次，并逐刻推进当天的时刻。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageLifeGameTests {
    private static final String BATCH = "village_life";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final String LABORER = "greek:laborer";
    private static final String GREEK = "greek";

    private VillageLifeGameTests() {
    }

    // ───────────── 搭建 ─────────────

    static VillageWorkGameTests.TV village(GameTestHelper h, Age stage, Map<String, List<Pos>> extra) {
        return VillageMigrationGameTests.village(h, 0, stage, extra, List.of());
    }

    static VillageEconomy.State state(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageEconomy.get(h.getLevel().getServer()).state(tv.id());
    }

    /** 换主神（测试村默认宙斯）。 */
    static VillageWorkGameTests.TV patron(GameTestHelper h, VillageWorkGameTests.TV tv, String god) {
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        VillageRegistry.Village v = reg.byId(tv.id()).orElseThrow();
        reg.update(new VillageRegistry.Village(v.id(), v.bell(), v.civ(), god, v.origin(), v.founder(), v.vanillaType(),
                v.missingSinceDay(), v.stage(), v.shrines(), v.baseline(), v.lastMigrationDay()));
        return new VillageWorkGameTests.TV(reg.byId(tv.id()).orElseThrow(), tv.plan());
    }

    /** 把本村历法拨到一年中的第 {@code doy} 日。 */
    static void day(GameTestHelper h, VillageWorkGameTests.TV tv, int doy) {
        VillageContext c = VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
        state(h, tv).calendarShift += doy - c.date().dayOfYear();
    }

    /** 当天的某一刻的上下文（不动世界时钟）。 */
    static VillageContext at(GameTestHelper h, VillageWorkGameTests.TV tv, int tod) {
        VillageContext c = VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
        return new VillageContext(c.level(), c.village(), c.plan(), c.state(), c.data(), c.rt(), c.behavior(), c.now(), tod,
                c.data().schedule().segmentAt(tod), c.today(), c.date(), c.residents());
    }

    /** 名册里的居民：床在 (1 + 2i, 2, 33)，人也站在床位上。 */
    static List<ResidentEntity> members(GameTestHelper h, VillageWorkGameTests.TV tv, String profession, int n) {
        List<ResidentEntity> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockPos home = h.absolutePos(new BlockPos(1 + 2 * (i % 8), 2, 33 - 2 * (i / 8)));
            out.add(Residents.spawn(h.getLevel(), tv.village(), VillageData.civ(GREEK).orElseThrow(), profession, home, home, false)
                    .orElseThrow());
        }
        return out;
    }

    static List<Job> candidates(String type, VillageContext ctx) {
        return JobType.REGISTRY.get(type).candidates(ctx);
    }

    static GuardianEntity priest(GameTestHelper h, VillageWorkGameTests.TV tv, int x, int z) {
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(x, 2, z)),
                Optional.of(tv.village().bell().pos()), Civilization.GREEK);
        h.assertTrue(g != null, "祭司生成");
        return g;
    }

    static void cleanup(GameTestHelper h, VillageWorkGameTests.TV tv, GuardianEntity... priests) {
        for (GuardianEntity g : priests) {
            if (g.guardianId() != null) {
                GuardianRegistry.get(h.getLevel().getServer()).remove(g.guardianId());
            }
            g.discard();
        }
        VillageWorkGameTests.cleanup(h, tv);
    }

    static Map<String, List<Pos>> anchors(Object... kv) {
        Map<String, List<Pos>> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            @SuppressWarnings("unchecked")
            List<Pos> l = kv[i + 1] instanceof Pos p ? List.of(p) : (List<Pos>) kv[i + 1];
            m.put((String) kv[i], l);
        }
        return m;
    }

    /**
     * 测试里逐刻推进当天的时刻（GameTest 服务器不走昼夜）。返回还原器：结束时把时钟拨回原处，
     * 否则后面批次的用例会停在夜里（只在工作段派单的用例就派不出单）。
     */
    static Runnable clock(GameTestHelper h, int startTod) {
        long orig = h.getLevel().getDayTime();
        long base = orig - Math.floorMod(orig, 24000L) + startTod;
        long t0 = h.getLevel().getGameTime();
        boolean[] done = {false};
        h.getLevel().setDayTime(base);
        h.onEachTick(() -> {
            if (!done[0]) {
                h.getLevel().setDayTime(base + (h.getLevel().getGameTime() - t0));
            }
        });
        return () -> {
            done[0] = true;
            h.getLevel().setDayTime(orig);
        };
    }

    // ───────────── 生活 ─────────────

    /** TC-T19-120：早上与傍晚 1–3 人去泉屋打水（不超过人口 1/3、女性优先）；装饰优先级（有生产需要时让位）；上午中段不派。 */
    @GameTest(template = T, batch = BATCH)
    public static void water_fetch_windows(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, anchors("water", abs(h, 8, 2, 12)));
        List<ResidentEntity> rs = members(h, tv, LABORER, 9);
        try {
            for (int tod : new int[]{200, 11100}) {
                List<Job> w = candidates(VillageLife.LIFE, at(h, tv, tod)).stream().filter(j -> j.ref().startsWith("life:water:")).toList();
                h.assertTrue(w.size() >= 1 && w.size() <= 3, "刻 " + tod + " 打水 " + w.size() + " 人");
                h.assertTrue(w.stream().allMatch(j -> j.priority() == Priority.DECOR), "装饰优先级");
                Job j = w.get(0);
                h.assertTrue(j.steps().stream().anyMatch(s -> s.kind() == JobStep.Kind.ACT && s.anim().equals("water_fill")), "装水");
                h.assertTrue(j.steps().stream().anyMatch(s -> "hydria".equals(s.carry())), "头顶水罐回家");
                boolean anyFemale = rs.stream().anyMatch(ResidentEntity::female);
                ResidentEntity first = rs.stream().filter(r -> r.getUUID().equals(j.assignee())).findFirst().orElseThrow();
                h.assertTrue(!anyFemale || first.female(), "女性优先");
            }
            h.assertTrue(candidates(VillageLife.LIFE, at(h, tv, 4000)).stream().noneMatch(j -> j.ref().startsWith("life:water:")),
                    "上午中段不打水");
            // 派出去就算这一段做过了：同一段不再重复派同一人
            VillageContext c = at(h, tv, 300);
            List<Job> w = candidates(VillageLife.LIFE, c).stream().filter(j -> j.ref().startsWith("life:water:")).toList();
            w.forEach(j -> JobType.REGISTRY.get(VillageLife.LIFE).escrow(c, j));
            h.assertTrue(candidates(VillageLife.LIFE, at(h, tv, 400)).stream().noneMatch(j -> j.ref().startsWith("life:water:")),
                    "人数够了不再派");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-128：黎明、晚饭在家先奠酒再吃（不扣物品）；不在家的不派（先回家）；午饭原地吃。 */
    @GameTest(template = T, batch = BATCH)
    public static void meals_at_home(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        List<ResidentEntity> rs = members(h, tv, LABORER, 2);
        try {
            ResidentEntity away = rs.get(1);
            away.teleportTo(away.getX(), away.getY(), away.getZ() - 12);
            int bread = state(h, tv).stock.count("mythic_age:olive_flatbread");
            for (int tod : new int[]{23500, 12300}) {
                List<Job> m = candidates(VillageLife.MEAL, at(h, tv, tod));
                h.assertTrue(m.size() == 1 && m.get(0).assignee().equals(rs.get(0).getUUID()), "只有在家的人吃：" + m.size());
                List<String> anims = m.get(0).steps().stream().filter(s -> s.kind() == JobStep.Kind.ACT).map(JobStep::anim).toList();
                h.assertTrue(anims.equals(List.of("libation_home", "eat")), "先奠酒再吃：" + anims);
                h.assertTrue(!m.get(0).needsWalk(), "在家里，不走路");
                h.assertTrue(candidates(BasicJobTypes.GO_HOME, at(h, tv, tod)).stream()
                        .anyMatch(j -> away.getUUID().equals(j.assignee())), "不在家的先回家");
            }
            List<Job> lunch = candidates(VillageLife.MEAL, at(h, tv, 6100));
            h.assertTrue(lunch.size() == 2, "午饭两人都在原地吃：" + lunch.size());
            h.assertTrue(state(h, tv).stock.count("mythic_age:olive_flatbread") == bread, "三餐表现不扣物品");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-121：祭司每天上午到村庄祭坛奠酒一次（依次去每个祭坛）；同一天不再去。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void priest_morning_libation(GameTestHelper h) {
        h.setBlock(new BlockPos(8, 2, 14), Blocks.STONE_BRICKS);
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, anchors("ritual", abs(h, 8, 2, 14)));
        GuardianEntity g = priest(h, tv, 8, 20);
        VillageContext early = at(h, tv, 800);
        VillageLife.tick(early);
        h.assertTrue(g.errand() == null, "1000 刻之前不去");
        VillageLife.tick(at(h, tv, 1200));
        h.assertTrue(g.errand() != null && g.errand().tag().equals("libation"), "上午去奠酒");
        h.assertTrue(state(h, tv).dailyCount(early.today(), "priest:libation") == 1, "记一次");
        g.arriveForTest();
        h.succeedWhen(() -> {
            h.assertTrue(g.gesture() == GuardianGesture.LIBATION_POUR, "在祭坛奠酒：" + g.gesture());
            g.endErrand();
            VillageLife.tick(at(h, tv, 2000));
            h.assertTrue(g.errand() == null, "同一天不再去");
            cleanup(h, tv, g);
        });
    }

    /** TC-T19-132：宙斯村的祭司清早去圣橡听橡；傍晚下雨时聚到柱廊下，没有柱廊就不聚。 */
    @GameTest(template = T, batch = BATCH)
    public static void zeus_listen_and_rain_meet(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, anchors("sacred_tree", abs(h, 3, 2, 14)));
        GuardianEntity g = priest(h, tv, 8, 20);
        members(h, tv, LABORER, 3);
        try {
            VillageLife.tick(at(h, tv, 400));
            h.assertTrue(g.errand() != null && g.errand().tag().equals("listen"), "清早听橡");
            VillageContext c = at(h, tv, 11200);
            h.assertTrue(BasicJobTypes.Leisure.place(c, true).isEmpty(), "没有柱廊：下雨不聚");
            h.assertTrue(BasicJobTypes.Leisure.place(c, false).isPresent(), "不下雨在广场");
            var info = io.github.happysnappingturtle.mythicage.village.VillagePlans.get(h.getLevel().getServer()).byVillage(tv.id())
                    .orElseThrow().info();
            Map<String, List<Pos>> a = new LinkedHashMap<>(info.anchors());
            a.put("merchant", List.of(abs(h, 14, 2, 10)));
            io.github.happysnappingturtle.mythicage.village.VillagePlans.get(h.getLevel().getServer()).updateInfo(tv.id(),
                    new io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece.Info(info.civ(), info.patron(), info.layout(),
                            info.layoutVersion(), info.templateSet(), info.center(), a, info.built(), info.plots()));
            VillageContext c2 = at(h, tv, 11200);
            h.assertTrue(BasicJobTypes.Leisure.place(c2, true).map(p -> p.equals(h.absolutePos(new BlockPos(14, 2, 10)))).orElse(false),
                    "下雨聚到柱廊");
            h.assertTrue(BasicJobTypes.Leisure.jobs(c2, true).stream().noneMatch(Job::outdoor), "柱廊下不算户外");
            h.succeed();
        } finally {
            cleanup(h, tv, g);
        }
    }

    /** TC-T19-126：哈迪斯村的亡灵日傍晚全村去墓园；平日最多 1 人。 */
    @GameTest(template = T, batch = BATCH)
    public static void hades_genesia(GameTestHelper h) {
        VillageWorkGameTests.TV tv = patron(h, village(h, Age.ARCHAIC,
                anchors("grave_visit", List.of(abs(h, 2, 2, 14), abs(h, 4, 2, 14), abs(h, 6, 2, 14)))), "hades");
        members(h, tv, LABORER, 5);
        try {
            day(h, tv, 2);
            List<Job> g = candidates(VillageLife.LIFE, at(h, tv, 11100)).stream().filter(j -> j.ref().startsWith("life:genesia:")).toList();
            h.assertTrue(g.size() == 5, "亡灵日全村去墓园：" + g.size());
            h.assertTrue(g.stream().allMatch(j -> j.steps().stream().anyMatch(s -> s.anim().equals("grave_tend"))), "系丝带、摆供品");
            h.assertTrue(g.stream().allMatch(j -> j.steps().get(0).carry().equals("ribbon")), "带着丝带");
            h.assertTrue(candidates(VillageLife.LIFE, at(h, tv, 5000)).stream().noneMatch(j -> j.ref().startsWith("life:genesia:")),
                    "白天不去");
            day(h, tv, 3);
            long n = candidates(VillageLife.LIFE, at(h, tv, 11100)).stream().filter(j -> j.ref().startsWith("life:")
                    && (j.ref().contains("genesia") || j.ref().contains("grave"))).count();
            h.assertTrue(n <= 1, "平日最多 1 人扫墓：" + n);
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-127：波塞冬临海村的劳力打鱼，每日结算不超过 8 次；别的主神不打鱼。 */
    @GameTest(template = T, batch = BATCH)
    public static void poseidon_fishing_cap(GameTestHelper h) {
        VillageWorkGameTests.TV tv = patron(h, village(h, Age.ARCHAIC,
                anchors("berth", List.of(abs(h, 3, 2, 12), abs(h, 13, 2, 12)))), "poseidon");
        List<ResidentEntity> rs = members(h, tv, LABORER, 2);
        try {
            JobType fish = JobType.REGISTRY.get(VillageLife.FISH);
            int settled = 0;
            for (int i = 0; i < 20; i++) {
                VillageContext c = at(h, tv, 1000);
                List<Job> js = fish.candidates(c);
                if (js.isEmpty()) {
                    break;
                }
                Job j = js.get(0);
                h.assertTrue(j.professions().contains(LABORER) && j.outdoor(), "劳力的户外活");
                fish.onActDone(c, rs.get(0), j, 2);
                h.assertTrue(fish.settle(c, rs.get(0), j), "渔获入库");
                settled++;
            }
            int fishes = state(h, tv).stock.count("minecraft:cod") + state(h, tv).stock.count("minecraft:salmon");
            h.assertTrue(settled == 8 && fishes == 8, "每日 8 次：结算 " + settled + "、渔获 " + fishes);
            VillageWorkGameTests.TV zeus = patron(h, tv, "zeus");
            h.assertTrue(fish.candidates(at(h, zeus, 1000)).isEmpty(), "非波塞冬村不打鱼");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    // ───────────── 节庆 ─────────────

    /** TC-T19-122：古典阶段的城邦节不举行（英雄起）；口粮 1 份。 */
    @GameTest(template = T, batch = BATCH)
    public static void classical_no_festival(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        members(h, tv, LABORER, 3);
        try {
            day(h, tv, 9);
            VillageContext c = at(h, tv, 1000);
            h.assertTrue(c.date().is("polis_festival"), "历法上是城邦节");
            h.assertTrue(VillageLife.festivalToday(c).isEmpty(), "古典不过节");
            h.assertTrue(candidates(VillageLife.FESTIVAL, c).isEmpty(), "没有节庆单");
            h.assertTrue(VillageLife.rations(c) == 1, "口粮 1 份");
            Job work = new Job(1, "gather_point", abs(h, 1, 2, 1), "x", "", List.of(JobStep.settle()), Map.of(), Map.of(), Set.of(),
                    Priority.SHORTAGE, 1, true, 100);
            h.assertTrue(VillageLife.filter(c, work).isPresent(), "照常干活");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-130：节庆日 0–13000 生产让位：口粮链、出窑、祈祷（游行时段除外）照常；宴饮时段不回家吃饭；13000 后恢复。 */
    @GameTest(template = T, batch = BATCH)
    public static void festival_production_yields(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.HEROIC, Map.of());
        try {
            day(h, tv, 9);
            java.util.function.BiFunction<String, Priority, Job> job = (type, p) -> new Job(1, type, abs(h, 1, 2, 1), "x", "",
                    List.of(JobStep.settle()), Map.of(), Map.of(), Set.of(), p, 1, false, 100);
            VillageContext c = at(h, tv, 4000);
            h.assertTrue(VillageLife.filter(c, job.apply("gather_point", Priority.SHORTAGE)).isEmpty(), "采集让位");
            h.assertTrue(VillageLife.filter(c, job.apply("build", Priority.ACTIVE_BUILD)).isEmpty(), "施工不派新段");
            h.assertTrue(VillageLife.filter(c, job.apply("craft", Priority.FOOD_SAFETY)).isPresent(), "口粮链照常");
            h.assertTrue(VillageLife.filter(c, job.apply(BasicJobTypes.COLLECT, Priority.SHORTAGE)).isPresent(), "出窑照常");
            h.assertTrue(VillageLife.filter(c, job.apply("pray", Priority.SHORTAGE)).isPresent(), "祈祷照常");
            h.assertTrue(VillageLife.filter(at(h, tv, 1000), job.apply("pray", Priority.SHORTAGE)).isEmpty(), "游行时段不祈祷");
            h.assertTrue(VillageLife.filter(at(h, tv, 12200), job.apply(VillageLife.MEAL, Priority.ROUTINE)).isEmpty(), "宴饮不回家吃饭");
            h.assertTrue(VillageLife.filter(at(h, tv, 13500), job.apply("gather_point", Priority.SHORTAGE)).isPresent(), "13000 后恢复");
            day(h, tv, 10);
            h.assertTrue(VillageLife.filter(at(h, tv, 4000), job.apply("gather_point", Priority.SHORTAGE)).isPresent(), "平日照常");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-129：乡村酒神节——有剧场：3 人演出、观众坐看台；没有剧场：广场跳舞；宴饮每 4 人 1 瓶葡萄酒，不足有多少用多少。 */
    @GameTest(template = T, batch = BATCH)
    public static void dionysia_theater_and_feast(GameTestHelper h) {
        VillageWorkGameTests.TV plain = village(h, Age.HEROIC, Map.of());
        members(h, plain, LABORER, 6);
        try {
            day(h, plain, 12);
            List<Job> dance = candidates(VillageLife.FESTIVAL, at(h, plain, 6500));
            h.assertTrue(dance.size() == 6 && dance.stream().allMatch(j -> j.steps().get(1).anim().equals("dance")),
                    "没有剧场：广场跳舞：" + dance.size());
        } finally {
            VillageWorkGameTests.cleanup(h, plain);
        }
        // 剧场：歌队场在 (8, 2, 16)，看台是半径 6 的半圈楼梯
        BlockPos orch = new BlockPos(8, 2, 16);
        for (int k = 0; k < 12; k++) {
            double a = Math.PI + k * Math.PI / 11;
            BlockPos st = orch.offset((int) Math.round(Math.cos(a) * 6), 0, (int) Math.round(Math.sin(a) * 6));
            h.setBlock(st, Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        }
        VillageWorkGameTests.TV tv = village(h, Age.HEROIC, anchors("orchestra", abs(h, 8, 2, 16)));
        members(h, tv, LABORER, 6);
        try {
            day(h, tv, 12);
            List<Job> show = candidates(VillageLife.FESTIVAL, at(h, tv, 6500));
            long actors = show.stream().filter(j -> j.steps().get(1).anim().equals("perform")).count();
            long watchers = show.stream().filter(j -> j.steps().get(1).anim().equals("sit_watch")).count();
            h.assertTrue(actors == 3 && watchers == 3, "演员 " + actors + "、观众 " + watchers);
            for (Job j : show.stream().filter(j -> j.steps().get(1).anim().equals("sit_watch")).toList()) {
                BlockPos seat = VillageContext.block(j.steps().get(0).target()).below();
                h.assertTrue(h.getLevel().getBlockState(seat).getBlock() instanceof StairBlock, "坐在看台上：" + seat);
            }
            // 宴饮：6 人要 2 瓶，库里只有 1 瓶
            state(h, tv).stock.putUnchecked("mythic_age:wine", 1);
            VillageLife.tick(at(h, tv, 11200));
            h.assertTrue(state(h, tv).stock.count("mythic_age:wine") == 0, "葡萄酒有多少用多少");
            VillageLife.tick(at(h, tv, 11800));
            h.assertTrue(state(h, tv).dailyCount(at(h, tv, 11800).today(), "feast:rural_dionysia") == 1, "只消耗一次");
            h.assertTrue(VillageLife.rations(at(h, tv, 12000)) == 2, "节庆口粮 ×2");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    /** TC-T19-133：城邦节当天集市商人卖泛雅典娜奖瓶，每村限 2；平日不出现；右键倒出橄榄油 4。 */
    @GameTest(template = T, batch = BATCH)
    public static void festival_goods(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.HEROIC, Map.of());
        List<ResidentEntity> merchants = members(h, tv, "greek:merchant", 2);
        FakePlayer p = VillageTradeGameTests.player(h, 30);
        try {
            day(h, tv, 10);
            h.assertTrue(ResidentTrade.view(p, merchants.get(0)).orElseThrow().rows().stream().noneMatch(r -> r.id().equals("prize_amphora")),
                    "平日没有奖瓶");
            day(h, tv, 9);
            h.assertTrue(ResidentTrade.deal(p, merchants.get(0), "prize_amphora") == ResidentTradeView.State.OK, "第 1 件");
            h.assertTrue(ResidentTrade.deal(p, merchants.get(0), "prize_amphora") == ResidentTradeView.State.OK, "第 2 件");
            h.assertTrue(ResidentTrade.deal(p, merchants.get(1), "prize_amphora") == ResidentTradeView.State.DAILY_DONE,
                    "全村限 2：另一个商人也不卖了");
            int amph = VillageTradeGameTests.count(p, GreekFestivalItems.PANATHENAIC_AMPHORA.get());
            h.assertTrue(amph == 2, "拿到 2 件：" + amph);
            int oil = VillageTradeGameTests.count(p, io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture.OLIVE_OIL.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GreekFestivalItems.PANATHENAIC_AMPHORA.get()));
            GreekFestivalItems.PANATHENAIC_AMPHORA.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
            int oil2 = VillageTradeGameTests.count(p, io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture.OLIVE_OIL.get());
            h.assertTrue(oil2 - oil == 4, "倒出橄榄油 4：" + (oil2 - oil));
            h.succeed();
        } finally {
            OnlinePlayers.forTest(p, false);
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    // ───────────── 神谕 ─────────────

    /**
     * TC-T19-125 + 134：神谕日开（祭司坐诊、右键祭司是神谕场所、能求铅板）/ 平日关并说下一个神谕日 / 耕种期闭门；
     * 铅板上写着答案；每样每天 1 次。
     */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void oracle_days(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.MYTHIC, anchors("oracle", abs(h, 8, 2, 14)));
        BlockPos o = h.absolutePos(new BlockPos(8, 2, 14));
        state(h, tv).constructed.add(new VillageEconomy.Built("greek:oracle", new Pos(o.getX() - 4, o.getY() - 1, o.getZ() - 5), Turn.R0,
                new Box(o.getX() - 4, o.getY() - 1, o.getZ() - 5, o.getX() + 6, o.getY() + 8, o.getZ() + 5), "", "", 0));
        GuardianEntity g = priest(h, tv, 8, 20);
        FakePlayer p = VillageTradeGameTests.player(h, 0);
        var server = h.getLevel().getServer();
        day(h, tv, 6);
        OracleRules.Status closed = OracleService.status(server, tv.village()).orElseThrow();
        h.assertTrue(closed.state() == OracleRules.State.CLOSED && closed.nextDay() == 8 && closed.daysUntil() == 2,
                "平日关着，下一个第 8 日还有 2 日：" + closed);
        h.assertTrue(OracleService.oracleAt(h.getLevel(), o.above()).isPresent(), "右键神谕所的方块能找到所属村庄");
        h.assertTrue(OracleService.oracleAt(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 34))).isEmpty(), "别处不算");
        day(h, tv, 2);
        h.assertTrue(OracleService.status(server, tv.village()).orElseThrow().state() == OracleRules.State.WINTER, "耕种期闭门");
        day(h, tv, 5);
        h.assertTrue(OracleService.status(server, tv.village()).orElseThrow().state() == OracleRules.State.OPEN, "神谕日开");
        VillageLife.tick(at(h, tv, 2000));
        h.assertTrue(g.errand() != null && g.errand().tag().equals(OracleService.DUTY), "祭司去神谕所");
        g.arriveForTest();
        h.succeedWhen(() -> {
            h.assertTrue(OracleService.seated(g) && g.gesture() == GuardianGesture.ORACLE_SIT, "坐诊");
            TradeService.openGuardian(p, g);
            h.assertTrue(TradeService.session(p).map(s -> s.venue() == TradeOffer.Venue.ORACLE).orElse(false), "神谕场所");
            var table = TradeTableLoader.table(Civilization.GREEK);
            for (String id : List.of(OracleService.STORM, OracleService.CREATURE)) {
                TradeOffer offer = table.offer(id).orElseThrow();
                h.assertTrue(offer.venues().equals(Set.of(TradeOffer.Venue.ORACLE)) && offer.dailyLimit() == 1, "只在神谕所、每天 1 次");
                h.assertTrue(io.github.happysnappingturtle.mythicage.core.trade.TradeRules.visible(offer,
                        TradeService.context(p, TradeService.session(p).orElseThrow())), "神谕场所看得到");
                ItemStack tablet = OracleService.answer(p, id);
                h.assertTrue(tablet.is(GreekFestivalItems.ORACLE_TABLET.get()), "神谕铅板");
                var lore = tablet.get(DataComponents.LORE);
                h.assertTrue(lore != null && !lore.lines().isEmpty(), "写着答案");
            }
            h.assertTrue(table.offer("village_guide").map(x -> !x.venues().contains(TradeOffer.Venue.ORACLE)).orElse(true),
                    "祭司平时的商品不在神谕所");
            TradeService.close(p);
            OnlinePlayers.forTest(p, false);
            cleanup(h, tv, g);
        });
    }

    // ───────────── 需要真实走路的（各自一批） ─────────────

    /**
     * TC-T19-123 + 124：英雄阶段的城邦节——居民在广场排队，按排放行走到祭坛；领头者到达时消耗葡萄酒 1（有油也先用酒）；
     * 当天口粮 ×2；放行错开，每刻新寻路不超过预算。
     */
    @GameTest(template = T, batch = "village_life_procession", timeoutTicks = 1600)
    public static void polis_procession(GameTestHelper h) {
        h.setBlock(new BlockPos(8, 2, 26), Blocks.STONE_BRICKS);
        VillageWorkGameTests.TV tv = village(h, Age.HEROIC, anchors("ritual", abs(h, 8, 2, 26), "guardian", abs(h, 8, 2, 28)));
        List<ResidentEntity> rs = members(h, tv, LABORER, 8);
        VillageEconomy.State s = state(h, tv);
        s.stock.putUnchecked("mythic_age:wine", 1);
        s.stock.putUnchecked("mythic_age:olive_oil", 1);
        day(h, tv, 9);
        PathBudget.clearPeakForTest();
        Runnable restore = clock(h, 10);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        List<Job> proc = VillageScheduler.runtime(tv.id()).activeJobs().stream().filter(j -> j.ref().contains("|procession|")).toList();
        h.assertTrue(proc.size() >= 6, "游行出发：" + proc.size());
        Map<Integer, Integer> perRelease = new LinkedHashMap<>();
        proc.forEach(j -> perRelease.merge(Integer.parseInt(j.ref().split("\\|")[3]), 1, Integer::sum));
        h.assertTrue(perRelease.size() >= 3 && perRelease.values().stream().allMatch(n -> n <= 2), "按排放行：" + perRelease);
        Job leader = proc.stream().filter(j -> j.ref().split("\\|")[2].equals("0")).findFirst().orElseThrow();
        h.assertTrue(leader.steps().stream().anyMatch(st -> "kanoun".equals(st.carry())), "领头者头顶祭篮");
        h.succeedWhen(() -> {
            h.assertTrue(PathBudget.peakForTest() <= PathBudget.PER_TICK, "每刻寻路 " + PathBudget.peakForTest());
            long today = VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow().today();
            h.assertTrue(s.dailyCount(today, "offer:polis_festival") == 1, "领头者到了祭坛");
            h.assertTrue(s.stock.count("mythic_age:wine") == 0 && s.stock.count("mythic_age:olive_oil") == 1, "先用葡萄酒");
            BlockPos altar = h.absolutePos(new BlockPos(8, 2, 26));
            long near = rs.stream().filter(r -> r.distanceToSqr(altar.getX() + 0.5, altar.getY(), altar.getZ() + 0.5) < 8 * 8).count();
            h.assertTrue(near >= 6, "游行走到祭坛前：" + near);
            int bread = s.stock.count("mythic_age:olive_flatbread");
            VillageScheduler.supperForTest(h.getLevel(), tv.id());
            h.assertTrue(bread - s.stock.count("mythic_age:olive_flatbread") == 16, "口粮 ×2：8 人吃 16 份");
            restore.run();
            VillageWorkGameTests.cleanup(h, tv);
        });
    }

    /** TC-T19-131：城邦节前夜的火炬接力：4 棒依次跑完，最后一棒到达时祭坛点燃。 */
    @GameTest(template = T, batch = "village_life_relay", timeoutTicks = 1600)
    public static void polis_torch_relay(GameTestHelper h) {
        h.setBlock(new BlockPos(8, 2, 34), Blocks.STONE_BRICKS);
        VillageWorkGameTests.TV tv = village(h, Age.HEROIC, anchors("ritual", abs(h, 8, 2, 34)));
        members(h, tv, LABORER, 5);
        VillageEconomy.State s = state(h, tv);
        day(h, tv, 8);
        Runnable restore = clock(h, 12600);
        h.succeedWhen(() -> {
            VillageContext c = VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
            h.assertTrue(s.dailyCount(c.today(), "relay:polis_festival") == 4, "4 棒跑完：" + s.dailyCount(c.today(), "relay:polis_festival"));
            h.assertTrue(s.dailyCount(c.today(), "lit:polis_festival") == 1, "祭坛点燃");
            restore.run();
            VillageWorkGameTests.cleanup(h, tv);
        });
    }
}
