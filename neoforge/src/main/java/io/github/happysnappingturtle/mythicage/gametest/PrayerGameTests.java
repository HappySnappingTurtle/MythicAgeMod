package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverEvents;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.GreekPrayer;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayAtAltar;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerRecord;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTempleSource;
import io.github.happysnappingturtle.mythicage.content.greek.temple.VillageTempleSurvey;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerId;
import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import io.github.happysnappingturtle.mythicage.core.runtime.SettlementEngine;
import io.github.happysnappingturtle.mythicage.faith.GraceSource;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.runtime.PlayerActivity;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeDriver;
import io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.shrine.inspect.ShrineSurvey;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.temple.TempleBook;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * 信徒祈祷的集成测试（T10 TC-T10-40～59、GA-4；GA-13 改为村庄神庙）。结构 17×16×36：地面在 y=2，
 * 每个用例自己建一个还没有卫城的希腊小村（0 级神庙，祭坛 = 钟的位置，默认在 (8, 3, 2)），测试玩家是它的供奉者。
 * <p>
 * GA-13 起信徒只去本村神庙，不再在多座神坛间挑选；原来的「神坛选择、同日一主、村外兜底」（TC-T10-42～44）作废，
 * 换成 {@link #prays_only_at_own_village}。走路的用例（真实村民 AI）仍各自单独成批。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PrayerGameTests {
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final BlockPos ALTAR = new BlockPos(8, 3, 2);
    private static final double EPS = 1e-9;

    private PrayerGameTests() {
    }

    // ───────────── 搭建 ─────────────

    private static void ground(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 2, z, Blocks.STONE);
            }
        }
        timeOfDay(h, 3000);
    }

    private static FakePlayer owner(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(8, 3, 6)));
        MythData.get(p).civ(Civilization.GREEK).setAge(Age.ARCHAIC);
        PlayerActivity.markActive(p);
        return p;
    }

    /** 一个还没有卫城的希腊小村：钟（= 0 级神庙的祭坛）在 rel，放一块村庄祭坛当标记；devotees 是供奉者。 */
    record TT(VillageRegistry.Village village, GlobalPos altar) {
        UUID id() {
            return village.id();
        }
    }

    static TT temple(GameTestHelper h, BlockPos rel, FakePlayer... devotees) {
        h.setBlock(rel, GreekVillageBlocks.VILLAGE_ALTAR.get());
        BlockPos abs = h.absolutePos(rel);
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), GlobalPos.of(h.getLevel().dimension(), abs),
                "greek", "zeus", VillageRegistry.Origin.VANILLA, null, "plains", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        VillageTemples.invalidate(v.id());
        MinecraftServer server = h.getLevel().getServer();
        for (FakePlayer p : devotees) {
            VillageTemples.addDevotee(server, v.id(), p.getUUID(), today(h), TempleBook.Via.JOIN);
            TemplePrayer.ensureLedger(server, t(h, v.id()), p.getUUID());
        }
        return new TT(v, t(h, v.id()).altar());
    }

    static VillageTemple t(GameTestHelper h, UUID village) {
        return VillageTemples.of(h.getLevel().getServer(), village)
                .orElseThrow(() -> new GameTestAssertException("村子没有神庙"));
    }

    private static VillageRegistry.Village village(GameTestHelper h, BlockPos absBell, String civ) {
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), GlobalPos.of(h.getLevel().dimension(), absBell),
                civ, civ.equals("greek") ? "zeus" : "", VillageRegistry.Origin.VANILLA, null, "plains", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        return v;
    }

    /** 希腊村庄成员：聚会点指向村庄的钟，家在 home（绝对坐标）。 */
    private static Villager member(GameTestHelper h, BlockPos rel, VillageRegistry.Village village, BlockPos absHome) {
        Villager v = h.spawn(EntityType.VILLAGER, rel);
        v.getBrain().setMemory(MemoryModuleType.MEETING_POINT, village.bell());
        v.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(h.getLevel().dimension(), absHome));
        return v;
    }

    /** 只改当天的时刻，不把日数拨回第 0 天（同组并行的按日入账测试依赖日数）。 */
    private static void timeOfDay(GameTestHelper h, long t) {
        long now = h.getLevel().getDayTime();
        h.getLevel().setDayTime(now - Math.floorMod(now, 24000L) + t);
    }

    private static long today(GameTestHelper h) {
        return MythDays.today(h.getLevel().getServer());
    }

    private static TempleBook book(GameTestHelper h, TT tt) {
        return VillageTemples.book(h.getLevel().getServer(), tt.id());
    }

    static void cleanup(GameTestHelper h, TT... temples) {
        h.killAllEntitiesOfClass(Villager.class);
        h.killAllEntitiesOfClass(io.github.happysnappingturtle.mythicage.guardian.GuardianEntity.class);
        MinecraftServer server = h.getLevel().getServer();
        for (TT tt : temples) {
            for (UUID d : List.copyOf(book(h, tt).devotees())) {
                TemplePrayer.dropLedger(server, tt.id(), d);
            }
            TemplePrayer.resetForTest(tt.id());
            PrayerService.resetForTest(tt.altar());
            VillageRegistry.get(server).remove(tt.village().bell());
            VillageEconomy.get(server).clearForTest(tt.id());
            VillageTemples.invalidate(tt.id());
        }
    }

    private static void cleanupVillages(GameTestHelper h, VillageRegistry.Village... villages) {
        for (VillageRegistry.Village v : villages) {
            VillageRegistry.get(h.getLevel().getServer()).remove(v.bell());
        }
    }

    /** 失败时的诊断：村民状态、行为、预留。 */
    private static String describe(GameTestHelper h, Villager v, TT tt) {
        var brain = v.getBrain();
        return "pos=" + h.relativePos(v.blockPosition()) + " record=" + PrayAtAltar.record(v)
                + " activities=" + brain.getActiveActivities()
                + " walk=" + brain.getMemory(MemoryModuleType.WALK_TARGET).map(t -> h.relativePos(t.getTarget().currentBlockPosition()))
                + " claimants=" + TemplePrayer.claimants(h.getLevel().getServer(), t(h, tt.id()))
                + " slots=" + t(h, tt.id()).prayerSlots().stream().map(h::relativePos).toList()
                + " day=" + h.getLevel().getDayTime() + " believer=" + BelieverEvents.isBeliever(v);
    }

    // ───────────── 注入自检 ─────────────

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = "prayer")
    public static void prayer_behavior_injected(GameTestHelper h) {
        h.assertTrue(GreekPrayer.injected(), "工作包与闲逛包里应有 PrayAtAltar（Mixin）");
        h.succeed();
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = "prayer")
    public static void temple_blocks_block_pathfinding(GameTestHelper h) {
        for (var block : List.of(GreekTemple.GREEK_ALTAR.get(), GreekTemple.GREEK_BRAZIER.get(), GreekTemple.GREEK_STATUE.get(),
                GreekVillageBlocks.VILLAGE_ALTAR.get())) {
            h.assertTrue(!block.defaultBlockState().isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND),
                    block + " 不应被寻路当作可穿过");
        }
        h.succeed();
    }

    // ───────────── 走路与跪拜（TC-T10-40、45） ─────────────

    @GameTest(template = T, batch = "prayer_walk", timeoutTicks = 800)
    public static void believer_walks_and_kneels(GameTestHelper h) {
        ground(h);
        TT tt = temple(h, ALTAR, owner(h, "pr_walk"));
        Villager v = member(h, new BlockPos(8, 3, 14), tt.village(), h.absolutePos(new BlockPos(8, 3, 32)));
        h.assertTrue(BelieverEvents.isBeliever(v), "希腊村庄成员应是信徒");
        h.succeedWhen(() -> {
            PrayerRecord rec = PrayAtAltar.record(v);
            h.assertTrue(rec.praying(), "信徒应走到本村神庙的祈祷位跪拜：" + describe(h, v, tt));
            h.assertTrue(rec.prayedOn(today(h)) && tt.id().equals(rec.lastPrayOwner()) && tt.altar().equals(rec.lastAltar()),
                    "应记下今天在本村神庙祈祷：" + rec);
            TemplePrayer.Stats st = TemplePrayer.stats(h.getLevel().getServer(), t(h, tt.id()), UUID.randomUUID());
            // GA-4：跪满 600 刻才记为信徒并离开，跪拜当中今日信徒还是 0（入账见 believer_prays_once_then_leaves）
            h.assertTrue(st.praying() == 1 && st.tooClose() == 0, "统计：" + st);
            cleanup(h, tt);
        });
    }

    // ───────────── 祈祷位已满（TC-T10-46） ─────────────

    @GameTest(template = T, batch = "prayer_full", timeoutTicks = 900)
    public static void slots_full_third_stays(GameTestHelper h) {
        ground(h);
        TT tt = temple(h, ALTAR, owner(h, "pr_full"));
        BlockPos home = h.absolutePos(new BlockPos(8, 3, 32));
        Villager[] vs = {member(h, new BlockPos(5, 3, 14), tt.village(), home), member(h, new BlockPos(8, 3, 14), tt.village(), home),
                member(h, new BlockPos(11, 3, 14), tt.village(), home)};
        h.succeedWhen(() -> {
            List<UUID> claimants = TemplePrayer.claimants(h.getLevel().getServer(), t(h, tt.id()));
            h.assertTrue(claimants.size() == 2, "0 级（露天祭坛）只有 2 个祈祷位，预留 " + claimants.size());
            long praying = java.util.Arrays.stream(vs).filter(v -> PrayAtAltar.record(v).praying()).count();
            h.assertTrue(praying == 2, "应有 2 人在跪拜，实际 " + praying);
            cleanup(h, tt);
        });
    }

    // ───────────── 零散村民、埃及村民（TC-T10-41） ─────────────

    @GameTest(template = T, batch = "prayer_loner", timeoutTicks = 400)
    public static void loners_and_other_civs_do_not_pray(GameTestHelper h) {
        ground(h);
        TT tt = temple(h, ALTAR, owner(h, "pr_loner"));
        VillageRegistry.Village egypt = village(h, h.absolutePos(new BlockPos(2, 3, 2000)), "egyptian");
        BlockPos home = h.absolutePos(new BlockPos(8, 3, 32));
        Villager loner = h.spawn(EntityType.VILLAGER, new BlockPos(6, 3, 14));
        loner.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(h.getLevel().dimension(), home));
        Villager egyptian = member(h, new BlockPos(10, 3, 14), egypt, home);
        h.assertTrue(!BelieverEvents.isBeliever(loner) && !BelieverEvents.isBeliever(egyptian), "都不是希腊信徒");
        h.runAfterDelay(300, () -> {
            h.assertTrue(TemplePrayer.claimants(h.getLevel().getServer(), t(h, tt.id())).isEmpty(), "不应有人去祈祷");
            cleanupVillages(h, egypt);
            cleanup(h, tt);
            h.succeed();
        });
    }

    // ───────────── 时段（TC-T10-47） ─────────────

    @GameTest(template = T, batch = "prayer_night", timeoutTicks = 1000)
    public static void leaves_when_window_ends(GameTestHelper h) {
        ground(h);
        TT tt = temple(h, ALTAR, owner(h, "pr_night"));
        Villager v = member(h, new BlockPos(8, 3, 14), tt.village(), h.absolutePos(new BlockPos(8, 3, 32)));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(PrayAtAltar.record(v).praying(), "先到祈祷位：" + describe(h, v, tt)))
                .thenExecute(() -> timeOfDay(h, 10000))
                .thenWaitUntil(() -> h.assertTrue(!PrayAtAltar.record(v).praying()
                        && TemplePrayer.claimants(h.getLevel().getServer(), t(h, tt.id())).isEmpty(), "时段外应离开并释放祈祷位"))
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(TemplePrayer.claimants(h.getLevel().getServer(), t(h, tt.id())).isEmpty(), "时段外不再去祈祷");
                    timeOfDay(h, 3000);
                    cleanup(h, tt);
                })
                .thenSucceed();
    }

    // ───────────── 拴绳、乘坐、无家（TC-T10-48） ─────────────

    @GameTest(template = T, batch = "prayer_leash", timeoutTicks = 400)
    public static void leashed_riding_homeless_do_not_pray(GameTestHelper h) {
        ground(h);
        TT tt = temple(h, ALTAR, owner(h, "pr_leash"));
        BlockPos home = h.absolutePos(new BlockPos(8, 3, 32));
        h.setBlock(4, 3, 16, Blocks.OAK_FENCE);
        Villager leashed = member(h, new BlockPos(4, 3, 14), tt.village(), home);
        LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot(h.getLevel(), h.absolutePos(new BlockPos(4, 3, 16)));
        leashed.setLeashedTo(knot, true);
        Villager riding = member(h, new BlockPos(8, 3, 14), tt.village(), home);
        Minecart cart = h.spawn(EntityType.MINECART, new BlockPos(8, 3, 14));
        riding.startRiding(cart, true);
        Villager homeless = member(h, new BlockPos(12, 3, 14), tt.village(), home);
        homeless.getBrain().eraseMemory(MemoryModuleType.HOME);
        h.runAfterDelay(300, () -> {
            List<UUID> c = TemplePrayer.claimants(h.getLevel().getServer(), t(h, tt.id()));
            h.assertTrue(c.isEmpty(), "拴绳、乘坐、无家的信徒都不计入：" + c);
            h.killAllEntitiesOfClass(Minecart.class);
            h.killAllEntitiesOfClass(LeashFenceKnotEntity.class);
            cleanup(h, tt);
            h.succeed();
        });
    }

    // ───────────── 离家太近（TC-T10-49） ─────────────

    @GameTest(template = T, batch = "prayer_close", timeoutTicks = 1600)
    public static void too_close_to_home_not_counted(GameTestHelper h) {
        ground(h);
        TT tt = temple(h, ALTAR, owner(h, "pr_close"));
        Villager v = member(h, new BlockPos(8, 3, 14), tt.village(), tt.altar().pos());
        h.succeedWhen(() -> {
            h.assertTrue(PrayAtAltar.record(v).praying(), "仍会站到祈祷位：" + describe(h, v, tt));
            TemplePrayer.Stats st = TemplePrayer.stats(h.getLevel().getServer(), t(h, tt.id()), UUID.randomUUID());
            h.assertTrue(st.praying() == 1 && st.tooClose() == 1, "面板「离家太近」计 1：" + st);
            h.assertTrue(!PrayAtAltar.record(v).prayedOn(today(h)), "不计入的祈祷不记录");
            h.assertTrue(book(h, tt).believersToday(today(h)) == 0, "不计入的信徒没有产出");
            cleanup(h, tt);
        });
    }

    // ───────────── 只去本村神庙（GA-13，取代 TC-T10-42～44） ─────────────

    /** 让村民立即尝试一次；返回它预留到的祭坛（没有则为 null），然后停下。 */
    private static GlobalPos tryOnce(GameTestHelper h, Villager v, TT... temples) {
        ServerLevel level = h.getLevel();
        PrayAtAltar.resetCheckForTest(v);
        PrayAtAltar b = new PrayAtAltar();
        if (!b.tryStart(level, v, level.getGameTime())) {
            return null;
        }
        GlobalPos chosen = null;
        for (TT tt : temples) {
            if (TemplePrayer.claimants(level.getServer(), t(h, tt.id())).contains(v.getUUID())) {
                chosen = tt.altar();
            }
        }
        b.doStop(level, v, level.getGameTime());
        return chosen;
    }

    @GameTest(template = T, batch = "prayer_choose", timeoutTicks = 300)
    public static void prays_only_at_own_village(GameTestHelper h) {
        ground(h);
        TT a = temple(h, new BlockPos(4, 3, 2), owner(h, "pr_own_a"));
        TT b = temple(h, new BlockPos(12, 3, 33), owner(h, "pr_own_b"));
        Villager v = member(h, new BlockPos(12, 3, 28), a.village(), h.absolutePos(new BlockPos(8, 3, 20)));
        v.setNoAi(true);
        h.succeedWhen(() -> {
            h.assertTrue(a.altar().equals(tryOnce(h, v, a, b)), "甲村成员去甲村神庙（乙村的更近也不去）");
            v.getBrain().setMemory(MemoryModuleType.MEETING_POINT, b.village().bell());
            h.assertTrue(b.altar().equals(tryOnce(h, v, a, b)), "迁到乙村 → 去乙村神庙");
            // 当天已入账就不再去
            PrayAtAltar.record(v).markCredited(today(h));
            h.assertTrue(tryOnce(h, v, a, b) == null, "今天已入账不再去");
            cleanup(h, a, b);
        });
    }

    // ───────────── 产出（TC-T10-50～52；GA-4 按今日信徒） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void yield_open_altar_two(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_yield");
        TT tt = temple(h, ALTAR, p);
        try {
            UUID v1 = UUID.randomUUID();
            h.assertTrue(TemplePrayer.credit(h.getLevel(), tt.id(), v1), "第一名入账");
            h.assertTrue(TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID()), "第二名入账");
            h.assertFalse(TemplePrayer.credit(h.getLevel(), tt.id(), v1), "同一人当天第二次不入账");
            double expected = PrayerFormula.DAILY_BASE * Math.sqrt(2) * 1.0;
            double got = TemplePrayer.pending(tt.id(), p.getUUID());
            h.assertTrue(Math.abs(got - expected) < EPS, "待领产出应为 " + expected + "，实际 " + got);
            TemplePrayer.Stats st = TemplePrayer.stats(h.getLevel().getServer(), t(h, tt.id()), p.getUUID());
            h.assertTrue(st.todayBelievers() == 2 && Math.abs(st.today() - expected) < EPS, "统计：" + st);
            h.succeed();
        } finally {
            cleanup(h, tt);
        }
    }

    /** TC-GA13-05（世界内）：同一供奉者两个村子，今日信徒合计算 √。 */
    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void yield_two_villages_share_sqrt(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_two");
        TT a = temple(h, new BlockPos(4, 3, 2), p);
        TT b = temple(h, new BlockPos(12, 3, 33), p);
        try {
            for (int i = 0; i < 3; i++) {
                TemplePrayer.credit(h.getLevel(), a.id(), UUID.randomUUID());
            }
            TemplePrayer.credit(h.getLevel(), b.id(), UUID.randomUUID());
            double pa = TemplePrayer.pending(a.id(), p.getUUID());
            double pb = TemplePrayer.pending(b.id(), p.getUUID());
            h.assertTrue(Math.abs(pa - PrayerFormula.DAILY_BASE * Math.sqrt(3)) < EPS, "前 3 名在甲村：" + pa);
            h.assertTrue(Math.abs(pa + pb - PrayerFormula.DAILY_BASE * Math.sqrt(4)) < EPS, "合计按 √4，实际 " + (pa + pb));
            h.succeed();
        } finally {
            cleanup(h, a, b);
        }
    }

    @GameTest(template = T, batch = "prayer_rain", timeoutTicks = 200)
    public static void rain_halves_open_altar(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_rain_open");
        TT tt = temple(h, ALTAR, p);
        PrayerService.overrideRainForTest((l, pos) -> true);
        try {
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            h.assertTrue(TemplePrayer.stats(h.getLevel().getServer(), t(h, tt.id()), p.getUUID()).rainHalved(), "面板雨天提示");
            double v = TemplePrayer.pending(tt.id(), p.getUUID());
            double expected = PrayerFormula.DAILY_BASE * Math.sqrt(2) * 1.0 * PrayerFormula.RAIN_FACTOR;
            h.assertTrue(Math.abs(v - expected) < EPS, "露天祭坛下雨减半：应为 " + expected + "，实际 " + v);
            h.succeed();
        } finally {
            PrayerService.overrideRainForTest(null);
            cleanup(h, tt);
        }
    }

    // ───────────── 接入 T5（TC-T10-53～55） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void real_settlement_credits_owner(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_real");
        TT tt = temple(h, ALTAR, p);
        try {
            MinecraftServer server = h.getLevel().getServer();
            RuntimeLedgers ledgers = RuntimeLedgers.get(server);
            ProducerLedger l = ledgers.get(TemplePrayer.ledgerId(tt.id(), p.getUUID()))
                    .orElseThrow(() -> new GameTestAssertException("没有台账"));
            h.assertTrue(l.owner().equals(p.getUUID()) && l.civ() == Civilization.GREEK, "台账主人与文明");
            long now = WorldClock.now(server);
            PlayerActivity.markActive(p);
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            double pending = TemplePrayer.pending(tt.id(), p.getUUID());
            long before = MythData.get(p).civ(Civilization.GREEK).favor();
            l.setLastSettledTick(now - 20);
            SettlementEngine.Settlement s = RuntimeDriver.settle(server, ledgers, l, now, ModConfigs.runtime().current().favor());
            long after = MythData.get(p).civ(Civilization.GREEK).favor();
            h.assertTrue(s.mode() == SettlementEngine.Mode.REAL, "区块加载、主人在线 → REAL，实际 " + s.mode());
            h.assertTrue(s.credited() >= 2 && after - before == s.credited() && TemplePrayer.pending(tt.id(), p.getUUID()) == 0,
                    "待领 " + pending + " 应被取走入账：+" + s.credited() + "，恩宠 " + before + " → " + after);
            h.succeed();
        } finally {
            PlayerActivity.forget(p.getUUID());
            cleanup(h, tt);
        }
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = "prayer")
    public static void unloaded_temple_uses_snapshot(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        FakePlayer p = owner(h, "pr_snap");
        long now = WorldClock.now(server);
        // 不存在的村子：探针报「没在运转」，按快照入账
        ProducerLedger l = new ProducerLedger(TemplePrayer.ledgerId(UUID.randomUUID(), p.getUUID()), p.getUUID(), Civilization.GREEK,
                24, now - 2400);
        l.recordObservation(24);
        RuntimeLedgers ledgers = RuntimeLedgers.get(server);
        ledgers.register(l);
        try {
            SettlementEngine.Settlement s = RuntimeDriver.settle(server, ledgers, l, now, ModConfigs.runtime().current().favor());
            h.assertTrue(s.mode() == SettlementEngine.Mode.SNAPSHOT, "神庙不在运转 → SNAPSHOT，实际 " + s.mode());
            h.assertTrue(s.credited() >= 1, "按快照入账：" + s);
        } finally {
            ledgers.remove(l.id());
            PlayerActivity.forget(p.getUUID());
        }
        h.succeed();
    }

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void favor_full_stops_and_flags(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_full_cap");
        TT tt = temple(h, ALTAR, p);
        try {
            MinecraftServer server = h.getLevel().getServer();
            long cap = MythData.cap(p, Civilization.GREEK);
            MythData.get(p).civ(Civilization.GREEK).setFavor(cap);
            h.assertTrue(TemplePrayer.stats(server, t(h, tt.id()), p.getUUID()).favorFull(), "面板应标记「已满」");
            long now = WorldClock.now(server);
            PlayerActivity.markActive(p);
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            RuntimeLedgers ledgers = RuntimeLedgers.get(server);
            ProducerLedger l = ledgers.get(TemplePrayer.ledgerId(tt.id(), p.getUUID())).orElseThrow();
            l.setLastSettledTick(now - 20);
            RuntimeDriver.settle(server, ledgers, l, now, ModConfigs.runtime().current().favor());
            h.assertTrue(MythData.get(p).civ(Civilization.GREEK).favor() == cap, "恩宠不超过上限");
            h.succeed();
        } finally {
            PlayerActivity.forget(p.getUUID());
            cleanup(h, tt);
        }
    }

    // ───────────── 补货减半（TC-T10-56） ─────────────

    private static Villager farmer(GameTestHelper h, BlockPos rel) {
        Villager v = h.spawn(EntityType.VILLAGER, rel);
        v.setNoAi(true);
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.FARMER));
        h.assertTrue(!v.getOffers().isEmpty(), "前提：农民有交易");
        v.getOffers().get(0).increaseUses();
        // 今天已补过 1 次，且距上次补货已超过 2400 刻
        CompoundTag tag = new CompoundTag();
        v.addAdditionalSaveData(tag);
        tag.putLong("LastRestock", h.getLevel().getGameTime() - 3000);
        tag.putInt("RestocksToday", 1);
        v.readAdditionalSaveData(tag);
        return v;
    }

    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = "prayer_restock")
    public static void prayed_villager_restocks_once(GameTestHelper h) {
        Villager prayed = farmer(h, new BlockPos(2, 1, 2));
        Villager other = farmer(h, new BlockPos(8, 1, 8));
        PrayAtAltar.record(prayed).record(today(h), UUID.randomUUID(), GlobalPos.of(h.getLevel().dimension(), BlockPos.ZERO));
        h.assertTrue(BelieverEvents.prayedToday(prayed) && !BelieverEvents.prayedToday(other), "前提");
        h.assertTrue(!prayed.shouldRestock(), "祈祷过的村民当天第 2 次补货被拒绝");
        h.assertTrue(other.shouldRestock(), "没祈祷的村民可以补第 2 次");
        ConfigHolder<WorldRulesConfig> holder = ModConfigs.worldRules();
        WorldRulesConfig original = holder.current();
        try {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(),
                    original.withGreek(original.greek().withPrayer(new WorldRulesConfig.Prayer(2)))));
            h.assertTrue(prayed.shouldRestock(), "restockLimit = 2 时不减");
        } finally {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), original));
        }
        h.killAllEntitiesOfClass(Villager.class);
        h.succeed();
    }

    // ───────────── 村子没了（TC-T10-57 改） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void removed_village_clears_record(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_remove");
        TT tt = temple(h, ALTAR, p);
        Villager v = member(h, new BlockPos(8, 3, 14), tt.village(), h.absolutePos(new BlockPos(8, 3, 30)));
        v.setNoAi(true);
        try {
            PrayAtAltar.record(v).record(today(h), tt.id(), tt.altar());
            VillageRegistry.get(h.getLevel().getServer()).remove(tt.village().bell());
            VillageTemples.invalidate(tt.id());
            PrayAtAltar.resetCheckForTest(v);
            new PrayAtAltar().tryStart(h.getLevel(), v, h.getLevel().getGameTime());
            h.assertTrue(PrayAtAltar.record(v).lastAltar() == null, "信徒记录里的祭坛应清空");
            h.succeed();
        } finally {
            cleanup(h, tt);
        }
    }

    // ───────────── 存档往返（TC-T10-58） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void save_load_round_trip(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_save");
        TT tt = temple(h, ALTAR, p);
        try {
            PrayerRecord rec = new PrayerRecord();
            rec.record(42, UUID.randomUUID(), tt.altar());
            h.assertTrue(PrayerRecord.load(rec.save()).equals(rec), "祈祷记录往返一致");
            h.assertTrue(PrayerRecord.load(new PrayerRecord().save()).equals(new PrayerRecord()), "空记录往返一致");
            TempleBook b = book(h, tt);
            long today = today(h);
            b.notePrayer(UUID.randomUUID(), today);
            b.notePrayer(UUID.randomUUID(), today - 3);
            b.notePrayer(UUID.randomUUID(), today - 9);
            h.assertTrue(b.believers(today) == 2, "7 日以前的不计");
            ProducerLedger l = RuntimeLedgers.get(h.getLevel().getServer()).get(TemplePrayer.ledgerId(tt.id(), p.getUUID())).orElseThrow();
            h.assertTrue(RuntimeLedgers.read(RuntimeLedgers.write(l)).equals(l), "台账往返一致");
            h.assertTrue(l.id().equals(new ProducerId("village_temple:" + tt.id() + ":" + p.getUUID())), "台账 id 格式");
            h.succeed();
        } finally {
            cleanup(h, tt);
        }
    }

    // ───────────── 信仰面板与检视界面一致（TC-T10-59） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void grace_rows_match_survey(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_rows");
        TT tt = temple(h, ALTAR, p);
        try {
            for (int i = 0; i < 3; i++) {
                TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            }
            MinecraftServer server = h.getLevel().getServer();
            TemplePrayer.Stats st = TemplePrayer.stats(server, t(h, tt.id()), p.getUUID());
            h.assertTrue(st.believers() == 3 && Math.abs(st.estimate() - PrayerFormula.estimate(3, 1.0, 0, 1)) < EPS,
                    "预计日产按近 7 日信徒：" + st);
            ShrineSurvey payload = VillageTempleSurvey.of(p, t(h, tt.id()));
            h.assertTrue(payload.estimate() == st.estimate() && payload.prayer().believers() == st.believers()
                    && payload.prayer().today() == st.today() && payload.prayer().praying() == st.praying()
                    && payload.temple().present() && payload.temple().devotee(), "检视界面与统计一致：" + payload.prayer());
            GraceSource.Row row = GreekTempleSource.INSTANCE.rows(p).stream().filter(r -> r.pos().equals(tt.altar())).findFirst()
                    .orElseThrow(() -> new GameTestAssertException("信仰面板没有这个村子"));
            Object[] args = ((TranslatableContents) row.status().getContents()).getArgs();
            h.assertTrue(row.perDay() == st.estimate() && Integer.valueOf(st.believers()).equals(args[1])
                    && String.format(java.util.Locale.ROOT, "%.1f", st.today()).equals(args[2]),
                    "信仰面板与检视界面一致：" + java.util.Arrays.toString(args));
            // TC-T11a-66：信仰面板的恩宠来源区就是这些行
            Age before = MythData.get(p).civ(Civilization.GREEK).age();
            MythData.get(p).civ(Civilization.GREEK).setAge(Age.CLASSICAL);
            var page = io.github.happysnappingturtle.mythicage.faith.FaithService.build(p).pages().get(Civilization.GREEK.ordinal());
            MythData.get(p).civ(Civilization.GREEK).setAge(before);
            var panelRow = page.sources().stream().filter(r -> r.pos().equals(tt.altar().pos())).findFirst()
                    .orElseThrow(() -> new GameTestAssertException("面板恩宠来源区没有这个村子"));
            h.assertTrue(page.sources().size() == GreekTempleSource.INSTANCE.rows(p).size()
                    && panelRow.note().equals(row.status())
                    && String.format(java.util.Locale.ROOT, "%.1f", row.perDay()).equals(
                    ((TranslatableContents) panelRow.value().getContents()).getArgs()[0]), "面板行与来源一致：" + panelRow);
            h.succeed();
        } finally {
            cleanup(h, tt);
        }
    }

    // ───────────── GA-4：按今日信徒入账与献供（TC-GA4-45、50～58） ─────────────

    /** TC-GA4-45：原版信徒跪满 600 刻入一笔，然后离开，当天不再来。 */
    @GameTest(template = T, batch = "prayer_once", timeoutTicks = 1400)
    public static void believer_prays_once_then_leaves(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_once");
        TT tt = temple(h, ALTAR, p);
        Villager v = member(h, new BlockPos(8, 3, 14), tt.village(), h.absolutePos(new BlockPos(8, 3, 32)));
        h.succeedWhen(() -> {
            long today = today(h);
            PrayerRecord rec = PrayAtAltar.record(v);
            h.assertTrue(rec.creditedOn(today), "跪满 600 刻后记为今天已入账：" + describe(h, v, tt));
            h.assertTrue(!rec.praying(), "入账后离开祈祷位");
            // 入账后 T5 的定时结算可能已经把待领产出转成了恩宠：两边有一边大于 0 即可
            boolean credited = TemplePrayer.pending(tt.id(), p.getUUID()) > 0 || MythData.get(p).civ(Civilization.GREEK).favor() > 0;
            h.assertTrue(book(h, tt).believersToday(today) == 1 && credited,
                    "今日信徒 1、已入账：待领 " + TemplePrayer.pending(tt.id(), p.getUUID()));
            cleanup(h, tt);
        });
    }

    private static ItemStack libation(int n) {
        return new ItemStack(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.LIBATION.get(), n);
    }

    /** TC-GA4-50、51（TC-GA13-24）：供奉者献奠酒：扣 1、记今天；已入账的部分按 +50% 补发，之后的每笔 ×1.5。 */
    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void offering_bonus_whole_day(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_offer");
        TT tt = temple(h, ALTAR, p);
        try {
            long today = today(h);
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            double before = TemplePrayer.pending(tt.id(), p.getUUID());
            ItemStack st = libation(3);
            h.assertTrue(TemplePrayer.offer(p, t(h, tt.id()), st) == PrayerService.OfferResult.OK, "献供成功");
            h.assertTrue(st.getCount() == 2 && book(h, tt).offeredOn(p.getUUID(), today), "扣 1 个、记今天");
            double now = TemplePrayer.pending(tt.id(), p.getUUID());
            h.assertTrue(Math.abs(now - before * 1.5) < EPS, "已入账部分补 50%：" + before + " → " + now);
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            double third = TemplePrayer.pending(tt.id(), p.getUUID()) - now;
            h.assertTrue(Math.abs(third - 1.5 * PrayerFormula.credit(3, 1.0, 1, 0, 0, 1)) < EPS, "第 3 名按 ×1.5：" + third);
            h.assertTrue(Math.abs(TemplePrayer.pending(tt.id(), p.getUUID()) - PrayerFormula.daily(3, 1.0, 0, 0.5, 1)) < EPS,
                    "合计等于献供日产");
            h.assertTrue(TemplePrayer.stats(h.getLevel().getServer(), t(h, tt.id()), p.getUUID()).offered(), "统计显示今日已献");
            h.succeed();
        } finally {
            cleanup(h, tt);
        }
    }

    /** TC-GA4-52～55（TC-GA13-23）：再献、非供奉者、数量不够、不是供品：都不扣物品。 */
    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void offering_refusals(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_offer_no");
        FakePlayer other = owner(h, "pr_offer_other");
        TT tt = temple(h, ALTAR, p);
        try {
            VillageTemple t = t(h, tt.id());
            long today = today(h);
            ItemStack beef = new ItemStack(Items.COOKED_BEEF, 1);
            h.assertTrue(TemplePrayer.offer(p, t, beef) == PrayerService.OfferResult.NOT_ENOUGH && beef.getCount() == 1, "熟牛肉要 2 个");
            ItemStack stone = new ItemStack(Items.STONE, 5);
            h.assertTrue(TemplePrayer.offer(p, t, stone) == PrayerService.OfferResult.NOT_OFFERING && stone.getCount() == 5, "石头不是供品");
            ItemStack mine = libation(1);
            h.assertTrue(TemplePrayer.offer(other, t, mine) == PrayerService.OfferResult.NOT_DEVOTEE && mine.getCount() == 1,
                    "不是供奉者不能献");
            h.assertTrue(TemplePrayer.offer(p, t, mine) == PrayerService.OfferResult.OK && mine.isEmpty(), "供奉者献成");
            ItemStack again = libation(1);
            h.assertTrue(TemplePrayer.offer(p, t, again) == PrayerService.OfferResult.ALREADY && again.getCount() == 1, "当天再献被拒");
            h.assertFalse(book(h, tt).offeredOn(p.getUUID(), today + 1), "TC-GA4-56 第二天可以再献");
            h.succeed();
        } finally {
            cleanup(h, tt);
        }
    }

    /** TC-GA4-57：「今天已献」存进村庄运行数据，重载后保留。 */
    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void offering_survives_reload(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_offer_save");
        TT tt = temple(h, ALTAR, p);
        try {
            long today = today(h);
            book(h, tt).markOffered(p.getUUID(), today);
            TempleBook copy = TempleBook.load(book(h, tt).save(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            h.assertTrue(copy.offeredOn(p.getUUID(), today) && !copy.offeredOn(p.getUUID(), today - 1), "重载后仍是今天已献");
            h.succeed();
        } finally {
            cleanup(h, tt);
        }
    }

    /** TC-GA4-58：服务器倍率 2 → 一名信徒入账翻倍。单独一批：改配置不影响同批其他用例。 */
    @GameTest(template = T, batch = "prayer_mult", timeoutTicks = 200)
    public static void rate_multiplier_scales_credit(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_mult");
        TT tt = temple(h, ALTAR, p);
        ConfigHolder<WorldRulesConfig> holder = ModConfigs.worldRules();
        WorldRulesConfig original = holder.current();
        try {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(),
                    original.withGreek(original.greek().withPrayer(new WorldRulesConfig.Prayer(1, 2.0)))));
            TemplePrayer.credit(h.getLevel(), tt.id(), UUID.randomUUID());
            double got = TemplePrayer.pending(tt.id(), p.getUUID());
            h.assertTrue(Math.abs(got - 2 * PrayerFormula.DAILY_BASE) < EPS, "倍率 2：" + got);
            h.succeed();
        } finally {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), original));
            cleanup(h, tt);
        }
    }
}
