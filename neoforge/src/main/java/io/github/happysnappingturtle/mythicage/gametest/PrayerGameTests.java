package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverEvents;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.GreekPrayer;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayAtAltar;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerRecord;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.content.greek.temple.AltarSurveyPayload;
import io.github.happysnappingturtle.mythicage.content.greek.temple.BrazierBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTempleSource;
import io.github.happysnappingturtle.mythicage.content.greek.temple.TempleService;
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
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * T10 信徒祈祷的集成测试（TC-T10-40～59）。结构 17×16×36：地面在 y=2，神坛默认在 (8, 3, 2) 朝南。
 * <p>
 * 走路的用例（真实村民 AI）各自单独成批，避免一个测试的信徒被相邻测试的神坛吸走；
 * 产出用例直接放预留（{@link PrayerService#putClaimForTest}）并手动采样，在同一刻里读结果。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PrayerGameTests {
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final BlockPos ALTAR = new BlockPos(8, 3, 2);
    private static final double PER_SAMPLE = PrayerFormula.BASE_PER_MINUTE / PrayerFormula.TICKS_PER_MINUTE
            * PrayerService.SAMPLE_INTERVAL;
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

    private static GlobalPos altar(GameTestHelper h, BlockPos rel, FakePlayer owner) {
        h.setBlock(rel, GreekTemple.GREEK_ALTAR.get().defaultBlockState().setValue(GreekAltarBlock.FACING, Direction.SOUTH));
        BlockPos abs = h.absolutePos(rel);
        ShrineService.bindForTest(h.getLevel(), abs, owner.getUUID(), owner.getGameProfile().getName(), Age.ARCHAIC,
                Direction.SOUTH);
        return GlobalPos.of(h.getLevel().dimension(), abs);
    }

    private static GreekAltarBlockEntity be(GameTestHelper h, GlobalPos key) {
        if (h.getLevel().getBlockEntity(key.pos()) instanceof GreekAltarBlockEntity be) {
            return be;
        }
        throw new GameTestAssertException("没有神坛方块实体 " + key.pos());
    }

    private static void assertScanned(GameTestHelper h, GlobalPos... keys) {
        for (GlobalPos k : keys) {
            h.assertTrue(be(h, k).hasResult() && !TempleService.pending(h.getLevel(), k.pos()), "扫描尚未完成");
        }
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

    private static void cleanup(GameTestHelper h, List<VillageRegistry.Village> villages, GlobalPos... altars) {
        h.killAllEntitiesOfClass(Villager.class);
        h.killAllEntitiesOfClass(io.github.happysnappingturtle.mythicage.guardian.GuardianEntity.class);
        for (VillageRegistry.Village v : villages) {
            VillageRegistry.get(h.getLevel().getServer()).remove(v.bell());
        }
        for (GlobalPos a : altars) {
            PrayerService.resetForTest(a);
        }
    }

    /**
     * 一个希腊村庄。钟只写进登记、放在 2000 格外（成员关系只看聚会点记忆）：失败的用例来不及清理时，
     * 残留的村庄登记也不会被其他测试的「附近村庄」查询碰到。神坛因此走「村外 48 格内」的兜底。
     */
    private static VillageRegistry.Village greekVillage(GameTestHelper h) {
        return village(h, h.absolutePos(new BlockPos(2, 3, 2000)), "greek");
    }

    /** 失败时的诊断：村民状态、行为、预留。 */
    private static String describe(GameTestHelper h, Villager v, GlobalPos a) {
        var brain = v.getBrain();
        return "pos=" + h.relativePos(v.blockPosition()) + " running=" + PrayAtAltar.record(v)
                + " flags=" + PrayAtAltar.record(v).praying() + " activities=" + brain.getActiveActivities()
                + " walk=" + brain.getMemory(MemoryModuleType.WALK_TARGET).map(t -> h.relativePos(t.getTarget().currentBlockPosition()))
                + " stuck=" + brain.getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
                + " running=" + brain.getRunningBehaviors().stream().map(Object::toString).toList()
                + " claimants=" + PrayerService.claimants(a, h.getLevel().getServer())
                + " slots=" + be(h, a).prayerSlots(TempleService.rules()).stream().map(h::relativePos).toList()
                + " day=" + h.getLevel().getDayTime()
                + " ABS origin=" + h.absolutePos(BlockPos.ZERO) + " altar=" + a.pos() + " v=" + v.blockPosition()
                + " slotsAbs=" + be(h, a).prayerSlots(TempleService.rules())
                + " path=" + (v.getNavigation().getPath() == null ? "null" : v.getNavigation().getPath().getTarget()
                + " done=" + v.getNavigation().isDone()) + " exact=" + v.position()
                + " nodes=" + (v.getNavigation().getPath() == null ? "" : java.util.stream.IntStream.range(0,
                v.getNavigation().getPath().getNodeCount()).mapToObj(i -> v.getNavigation().getPath().getNode(i).asBlockPos()
                + "/" + v.getNavigation().getPath().getNode(i).type).toList() + " next=" + v.getNavigation().getPath().getNextNodeIndex())
                + " stuckNav=" + v.getNavigation().isStuck() + " now=" + h.getLevel().getGameTime()
                + " home=" + brain.getMemory(MemoryModuleType.HOME) + " meet=" + brain.getMemory(MemoryModuleType.MEETING_POINT)
                + " believer=" + BelieverEvents.isBeliever(v) + " unreachable=" + PrayerService.stats(be(h, a), TempleService.rules());
    }

    // ───────────── 注入自检 ─────────────

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = "prayer")
    public static void prayer_behavior_injected(GameTestHelper h) {
        h.assertTrue(GreekPrayer.injected(), "工作包与闲逛包里应有 PrayAtAltar（Mixin）");
        h.succeed();
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = "prayer")
    public static void temple_blocks_block_pathfinding(GameTestHelper h) {
        for (var block : List.of(GreekTemple.GREEK_ALTAR.get(), GreekTemple.GREEK_BRAZIER.get(), GreekTemple.GREEK_STATUE.get())) {
            h.assertTrue(!block.defaultBlockState().isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND),
                    block + " 不应被寻路当作可穿过");
        }
        h.succeed();
    }

    // ───────────── 走路与跪拜（TC-T10-40、45） ─────────────

    @GameTest(template = T, batch = "prayer_walk", timeoutTicks = 800)
    public static void believer_walks_and_kneels(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_walk");
        GlobalPos a = altar(h, ALTAR, p);
        VillageRegistry.Village village = greekVillage(h);
        Villager v = member(h, new BlockPos(8, 3, 14), village, h.absolutePos(new BlockPos(8, 3, 32)));
        h.assertTrue(BelieverEvents.isBeliever(v), "希腊村庄成员应是信徒");
        h.succeedWhen(() -> {
            assertScanned(h, a);
            PrayerRecord rec = PrayAtAltar.record(v);
            h.assertTrue(rec.praying(), "信徒应走到祈祷位跪拜：" + describe(h, v, a));
            h.assertTrue(rec.prayedOn(today(h)) && p.getUUID().equals(rec.lastPrayOwner()) && a.equals(rec.lastAltar()),
                    "应记下今天为这位主人在这座神坛祈祷：" + rec);
            PrayerService.Stats st = PrayerService.stats(be(h, a), TempleService.rules());
            h.assertTrue(st.praying() == 1 && st.believers() == 1 && st.tooClose() == 0, "统计：" + st);
            cleanup(h, List.of(village), a);
        });
    }

    // ───────────── 祈祷位已满（TC-T10-46） ─────────────

    @GameTest(template = T, batch = "prayer_full", timeoutTicks = 900)
    public static void slots_full_third_stays(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_full"));
        VillageRegistry.Village village = greekVillage(h);
        BlockPos home = h.absolutePos(new BlockPos(8, 3, 32));
        Villager[] vs = {member(h, new BlockPos(5, 3, 14), village, home), member(h, new BlockPos(8, 3, 14), village, home),
                member(h, new BlockPos(11, 3, 14), village, home)};
        h.succeedWhen(() -> {
            assertScanned(h, a);
            List<UUID> claimants = PrayerService.claimants(a, h.getLevel().getServer());
            h.assertTrue(claimants.size() == 2, "露天祭坛只有 2 个祈祷位，预留 " + claimants.size());
            long praying = java.util.Arrays.stream(vs).filter(v -> PrayAtAltar.record(v).praying()).count();
            h.assertTrue(praying == 2, "应有 2 人在跪拜，实际 " + praying);
            cleanup(h, List.of(village), a);
        });
    }

    // ───────────── 零散村民、埃及村民（TC-T10-41） ─────────────

    @GameTest(template = T, batch = "prayer_loner", timeoutTicks = 400)
    public static void loners_and_other_civs_do_not_pray(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_loner"));
        VillageRegistry.Village egypt = village(h, h.absolutePos(new BlockPos(2, 3, 2000)), "egyptian");
        BlockPos home = h.absolutePos(new BlockPos(8, 3, 32));
        Villager loner = h.spawn(EntityType.VILLAGER, new BlockPos(6, 3, 14));
        loner.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(h.getLevel().dimension(), home));
        Villager egyptian = member(h, new BlockPos(10, 3, 14), egypt, home);
        h.assertTrue(!BelieverEvents.isBeliever(loner) && !BelieverEvents.isBeliever(egyptian), "都不是希腊信徒");
        h.runAfterDelay(300, () -> {
            assertScanned(h, a);
            h.assertTrue(PrayerService.claimants(a, h.getLevel().getServer()).isEmpty(), "不应有人去祈祷");
            cleanup(h, List.of(egypt), a);
            h.succeed();
        });
    }

    // ───────────── 时段（TC-T10-47） ─────────────

    @GameTest(template = T, batch = "prayer_night", timeoutTicks = 1000)
    public static void leaves_when_window_ends(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_night"));
        VillageRegistry.Village village = greekVillage(h);
        Villager v = member(h, new BlockPos(8, 3, 14), village, h.absolutePos(new BlockPos(8, 3, 32)));
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(PrayAtAltar.record(v).praying(), "先到祈祷位：" + describe(h, v, a)))
                .thenExecute(() -> timeOfDay(h, 10000))
                .thenWaitUntil(() -> h.assertTrue(!PrayAtAltar.record(v).praying()
                        && PrayerService.claimants(a, h.getLevel().getServer()).isEmpty(), "时段外应离开并释放祈祷位"))
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(PrayerService.claimants(a, h.getLevel().getServer()).isEmpty(), "时段外不再去祈祷");
                    timeOfDay(h, 3000);
                    cleanup(h, List.of(village), a);
                })
                .thenSucceed();
    }

    // ───────────── 拴绳、乘坐、无家（TC-T10-48） ─────────────

    @GameTest(template = T, batch = "prayer_leash", timeoutTicks = 400)
    public static void leashed_riding_homeless_do_not_pray(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_leash"));
        VillageRegistry.Village village = greekVillage(h);
        BlockPos home = h.absolutePos(new BlockPos(8, 3, 32));
        h.setBlock(4, 3, 16, Blocks.OAK_FENCE);
        Villager leashed = member(h, new BlockPos(4, 3, 14), village, home);
        LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot(h.getLevel(), h.absolutePos(new BlockPos(4, 3, 16)));
        leashed.setLeashedTo(knot, true);
        Villager riding = member(h, new BlockPos(8, 3, 14), village, home);
        Minecart cart = h.spawn(EntityType.MINECART, new BlockPos(8, 3, 14));
        riding.startRiding(cart, true);
        Villager homeless = member(h, new BlockPos(12, 3, 14), village, home);
        homeless.getBrain().eraseMemory(MemoryModuleType.HOME);
        h.runAfterDelay(300, () -> {
            assertScanned(h, a);
            h.assertTrue(PrayerService.claimants(a, h.getLevel().getServer()).isEmpty(),
                    "拴绳、乘坐、无家的信徒都不计入：" + PrayerService.claimants(a, h.getLevel().getServer()));
            h.killAllEntitiesOfClass(Minecart.class);
            h.killAllEntitiesOfClass(LeashFenceKnotEntity.class);
            cleanup(h, List.of(village), a);
            h.succeed();
        });
    }

    // ───────────── 离家太近（TC-T10-49） ─────────────

    // 超时 800 → 1600：整套并行时神坛扫描与村民出发偶尔偏晚，走 20 格还没到就超时（2026-10-16，8 轮里 2 次）
    @GameTest(template = T, batch = "prayer_close", timeoutTicks = 1600)
    public static void too_close_to_home_not_counted(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_close"));
        VillageRegistry.Village village = greekVillage(h);
        Villager v = member(h, new BlockPos(8, 3, 14), village, a.pos());
        h.succeedWhen(() -> {
            assertScanned(h, a);
            h.assertTrue(PrayAtAltar.record(v).praying(), "仍会站到祈祷位：" + describe(h, v, a));
            PrayerService.Stats st = PrayerService.stats(be(h, a), TempleService.rules());
            h.assertTrue(st.praying() == 1 && st.tooClose() == 1, "面板「离家太近」计 1：" + st);
            h.assertTrue(!PrayAtAltar.record(v).prayedOn(today(h)), "不计入的祈祷不记录");
            PrayerService.resetForTest(a);
            PrayerService.putClaimForTest(a, v.getUUID(), ALTAR, false, h.getLevel().getGameTime());
            PrayerService.sampleForTest(h.getLevel().getServer());
            h.assertTrue(PrayerService.pending(a) == 0, "不计入的信徒没有产出");
            cleanup(h, List.of(village), a);
        });
    }

    // ───────────── 神坛选择、同日一主、迁村（TC-T10-42～44） ─────────────

    /** 让村民立即尝试一次；返回它预留到的神坛（没有则为 null），然后停下。 */
    private static GlobalPos tryOnce(GameTestHelper h, Villager v, GlobalPos... altars) {
        ServerLevel level = h.getLevel();
        PrayAtAltar.resetCheckForTest(v);
        PrayAtAltar b = new PrayAtAltar();
        if (!b.tryStart(level, v, level.getGameTime())) {
            return null;
        }
        GlobalPos chosen = null;
        for (GlobalPos a : altars) {
            if (PrayerService.claimants(a, level.getServer()).contains(v.getUUID())) {
                chosen = a;
            }
        }
        b.doStop(level, v, level.getGameTime());
        return chosen;
    }

    @GameTest(template = T, batch = "prayer_choose", timeoutTicks = 300)
    public static void altar_choice_and_one_owner_per_day(GameTestHelper h) {
        ground(h);
        FakePlayer ownerA = owner(h, "pr_choose_a");
        FakePlayer ownerB = owner(h, "pr_choose_b");
        GlobalPos a = altar(h, new BlockPos(4, 3, 2), ownerA);
        GlobalPos b = altar(h, new BlockPos(12, 3, 33), ownerB);
        // 村 1 的钟在 A 北 40 格（A 在村内、B 不在）；村 2 的钟在 B 南 40 格（B 在村内、A 不在）
        VillageRegistry.Village v1 = village(h, a.pos().offset(0, 0, -40), "greek");
        VillageRegistry.Village v2 = village(h, b.pos().offset(0, 0, 40), "greek");
        BlockPos home = h.absolutePos(new BlockPos(8, 3, 20));
        Villager v = member(h, new BlockPos(4, 3, 8), v1, home);
        v.setNoAi(true);
        h.succeedWhen(() -> {
            assertScanned(h, a, b);
            h.assertTrue(a.equals(tryOnce(h, v, a, b)), "村 1 成员：选村内的 A");
            // TC-T10-42：今天已为 B 的主人祈祷过 → 只能去 B（即使 A 更近且在村内）
            PrayAtAltar.record(v).record(today(h), ownerB.getUUID(), b);
            h.assertTrue(tryOnce(h, v, a, b) == null, "A 不是今天的主人，村内又没有 B 的神坛 → 不去");
            v.getBrain().setMemory(MemoryModuleType.MEETING_POINT, v2.bell());
            h.assertTrue(b.equals(tryOnce(h, v, a, b)), "TC-T10-44：迁到村 2 → 按新村选村内的 B");
            // TC-T10-43：第二天可以为另一位主人祈祷
            v.getBrain().setMemory(MemoryModuleType.MEETING_POINT, v1.bell());
            MythDays.advanceForTest(1);
            try {
                h.assertTrue(a.equals(tryOnce(h, v, a, b)), "次日可以为 A 的主人祈祷");
            } finally {
                MythDays.advanceForTest(-1);
            }
            cleanup(h, List.of(v1, v2), a, b);
        });
    }

    @GameTest(template = T, batch = "prayer_choose", timeoutTicks = 300)
    public static void fallback_outside_village(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_fallback"));
        // 钟远在 200 格外：村内没有神坛 → 去村外 48 格内最近的
        VillageRegistry.Village far = village(h, a.pos().offset(200, 0, 0), "greek");
        Villager v = member(h, new BlockPos(8, 3, 14), far, h.absolutePos(new BlockPos(8, 3, 30)));
        v.setNoAi(true);
        h.succeedWhen(() -> {
            assertScanned(h, a);
            h.assertTrue(a.equals(tryOnce(h, v, a)), "村内无神坛时用村外 48 格内的");
            cleanup(h, List.of(far), a);
        });
    }

    // ───────────── 产出（TC-T10-50～52） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void yield_open_altar_two(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_yield"));
        h.succeedWhen(() -> {
            assertScanned(h, a);
            long now = h.getLevel().getGameTime();
            PrayerService.resetForTest(a);
            PrayerService.putClaimForTest(a, UUID.randomUUID(), ALTAR.east(), true, now);
            PrayerService.putClaimForTest(a, UUID.randomUUID(), ALTAR.west(), true, now);
            PrayerService.sampleForTest(h.getLevel().getServer());
            double expected = PER_SAMPLE * Math.sqrt(2) * 1.0;
            h.assertTrue(Math.abs(PrayerService.pending(a) - expected) < EPS,
                    "待领产出应为 " + expected + "，实际 " + PrayerService.pending(a));
            PrayerService.Stats st = PrayerService.stats(be(h, a), TempleService.rules());
            h.assertTrue(st.praying() == 2 && Math.abs(st.today() - expected) < EPS, "统计：" + st);
            PrayerService.resetForTest(a);
        });
    }

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void yield_two_altars_share_sqrt(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_two");
        GlobalPos a = altar(h, new BlockPos(4, 3, 2), p);
        GlobalPos b = altar(h, new BlockPos(12, 3, 33), p);
        h.succeedWhen(() -> {
            assertScanned(h, a, b);
            long now = h.getLevel().getGameTime();
            PrayerService.resetForTest(a);
            PrayerService.resetForTest(b);
            for (int i = 0; i < 3; i++) {
                PrayerService.putClaimForTest(a, UUID.randomUUID(), a.pos().offset(i, 0, 1), true, now);
            }
            PrayerService.putClaimForTest(b, UUID.randomUUID(), b.pos().north(), true, now);
            PrayerService.sampleForTest(h.getLevel().getServer());
            double pa = PrayerService.pending(a);
            double pb = PrayerService.pending(b);
            h.assertTrue(Math.abs(pa / pb - 3) < 1e-6, "份额应为 3:1，实际 " + pa + " : " + pb);
            h.assertTrue(Math.abs(pa + pb - PER_SAMPLE * Math.sqrt(4)) < EPS, "合计按 √4，实际 " + (pa + pb));
            PrayerService.resetForTest(a);
            PrayerService.resetForTest(b);
        });
    }

    /** 下雨只在本刻内生效，不影响同时运行的其他测试。 */
    private static double sampleInRain(GameTestHelper h, GlobalPos a) {
        Level level = h.getLevel();
        long now = level.getGameTime();
        PrayerService.resetForTest(a);
        PrayerService.putClaimForTest(a, UUID.randomUUID(), a.pos().east(), true, now);
        PrayerService.putClaimForTest(a, UUID.randomUUID(), a.pos().west(), true, now);
        PrayerService.overrideRainForTest((l, pos) -> true);
        try {
            PrayerService.sampleForTest(h.getLevel().getServer());
            h.assertTrue(PrayerService.stats(be(h, a), TempleService.rules()).rainHalved()
                    == (be(h, a).verdict(TempleService.rules()).grantedLevel() == 0), "面板雨天提示");
        } finally {
            PrayerService.overrideRainForTest(null);
        }
        double v = PrayerService.pending(a);
        PrayerService.resetForTest(a);
        return v;
    }

    @GameTest(template = T, batch = "prayer_rain", timeoutTicks = 200)
    public static void rain_halves_open_altar(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_rain_open"));
        h.succeedWhen(() -> {
            assertScanned(h, a);
            double v = sampleInRain(h, a);
            double expected = PER_SAMPLE * Math.sqrt(2) * 1.0 * PrayerFormula.RAIN_FACTOR;
            h.assertTrue(Math.abs(v - expected) < EPS, "露天祭坛下雨减半：应为 " + expected + "，实际 " + v);
        });
    }

    @GameTest(template = T, batch = "prayer_rain", timeoutTicks = 200)
    public static void rain_spares_small_temple(GameTestHelper h) {
        ground(h);
        // 小神庙：4 根柱、柱顶整片屋顶、1 个点燃的圣火盆
        for (int[] c : new int[][]{{4, 6}, {12, 6}, {4, 14}, {12, 14}}) {
            for (int y = 3; y <= 5; y++) {
                h.setBlock(c[0], y, c[1], GreekTemple.MARBLE_PILLAR.get());
            }
        }
        for (int x = 4; x <= 12; x++) {
            for (int z = 6; z <= 14; z++) {
                h.setBlock(x, 6, z, GreekMaterials.SMOOTH_MARBLE.get());
            }
        }
        h.setBlock(6, 3, 12, GreekTemple.GREEK_BRAZIER.get().defaultBlockState().setValue(BrazierBlock.LIT, true));
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_rain_temple"));
        h.succeedWhen(() -> {
            assertScanned(h, a);
            h.assertTrue(be(h, a).verdict(TempleService.rules()).grantedLevel() == 1, "前提：认定为小神庙");
            double v = sampleInRain(h, a);
            double expected = PER_SAMPLE * Math.sqrt(2) * 1.5;
            h.assertTrue(Math.abs(v - expected) < EPS, "小神庙下雨不减：应为 " + expected + "，实际 " + v);
        });
    }

    // ───────────── 接入 T5（TC-T10-53～55） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void real_settlement_credits_owner(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_real");
        GlobalPos a = altar(h, ALTAR, p);
        h.succeedWhen(() -> {
            assertScanned(h, a);
            MinecraftServer server = h.getLevel().getServer();
            RuntimeLedgers ledgers = RuntimeLedgers.get(server);
            ProducerLedger l = ledgers.get(PrayerService.ledgerId(a)).orElseThrow(() -> new GameTestAssertException("没有台账"));
            h.assertTrue(l.owner().equals(p.getUUID()) && l.civ() == Civilization.GREEK, "台账主人与文明");
            long now = WorldClock.now(server);
            PlayerActivity.markActive(p);
            PrayerService.resetForTest(a);
            PrayerService.putClaimForTest(a, UUID.randomUUID(), ALTAR.east(), true, now);
            PrayerService.putClaimForTest(a, UUID.randomUUID(), ALTAR.west(), true, now);
            for (int i = 0; i < 100; i++) {
                PrayerService.sampleForTest(server);
            }
            double pending = PrayerService.pending(a);
            long before = MythData.get(p).civ(Civilization.GREEK).favor();
            l.setLastSettledTick(now - 20);
            SettlementEngine.Settlement s = RuntimeDriver.settle(server, ledgers, l, now, ModConfigs.runtime().current().favor());
            long after = MythData.get(p).civ(Civilization.GREEK).favor();
            h.assertTrue(s.mode() == SettlementEngine.Mode.REAL, "区块加载、主人在线 → REAL，实际 " + s.mode());
            h.assertTrue(s.credited() >= 2 && after - before == s.credited() && PrayerService.pending(a) == 0,
                    "待领 " + pending + " 应被取走入账：+" + s.credited() + "，恩宠 " + before + " → " + after);
            PrayerService.resetForTest(a);
            PlayerActivity.forget(p.getUUID());
        });
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = "prayer")
    public static void unloaded_altar_uses_snapshot(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        FakePlayer p = owner(h, "pr_snap");
        long now = WorldClock.now(server);
        GlobalPos far = GlobalPos.of(Level.OVERWORLD, new BlockPos(3_000_000, 64, 3_000_000 + (int) (h.getTick() % 1000)));
        ProducerLedger l = new ProducerLedger(PrayerService.ledgerId(far), p.getUUID(), Civilization.GREEK, 24, now - 2400);
        l.recordObservation(24);
        RuntimeLedgers ledgers = RuntimeLedgers.get(server);
        ledgers.register(l);
        try {
            SettlementEngine.Settlement s = RuntimeDriver.settle(server, ledgers, l, now, ModConfigs.runtime().current().favor());
            h.assertTrue(s.mode() == SettlementEngine.Mode.SNAPSHOT, "区块未加载 → SNAPSHOT，实际 " + s.mode());
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
        GlobalPos a = altar(h, ALTAR, p);
        h.succeedWhen(() -> {
            assertScanned(h, a);
            MinecraftServer server = h.getLevel().getServer();
            long cap = MythData.cap(p, Civilization.GREEK);
            MythData.get(p).civ(Civilization.GREEK).setFavor(cap);
            h.assertTrue(PrayerService.stats(be(h, a), TempleService.rules()).favorFull(), "面板应标记「已满」");
            long now = WorldClock.now(server);
            PlayerActivity.markActive(p);
            PrayerService.resetForTest(a);
            PrayerService.putClaimForTest(a, UUID.randomUUID(), ALTAR.east(), true, now);
            for (int i = 0; i < 100; i++) {
                PrayerService.sampleForTest(server);
            }
            RuntimeLedgers ledgers = RuntimeLedgers.get(server);
            ProducerLedger l = ledgers.get(PrayerService.ledgerId(a)).orElseThrow();
            l.setLastSettledTick(now - 20);
            RuntimeDriver.settle(server, ledgers, l, now, ModConfigs.runtime().current().favor());
            h.assertTrue(MythData.get(p).civ(Civilization.GREEK).favor() == cap, "恩宠不超过上限");
            PrayerService.resetForTest(a);
            PlayerActivity.forget(p.getUUID());
        });
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

    // ───────────── 神坛被拆（TC-T10-57） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void removed_altar_unregisters(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_remove");
        GlobalPos a = altar(h, ALTAR, p);
        VillageRegistry.Village village = greekVillage(h);
        Villager v = member(h, new BlockPos(8, 3, 14), village, h.absolutePos(new BlockPos(8, 3, 30)));
        v.setNoAi(true);
        h.succeedWhen(() -> {
            assertScanned(h, a);
            MinecraftServer server = h.getLevel().getServer();
            h.assertTrue(RuntimeLedgers.get(server).get(PrayerService.ledgerId(a)).isPresent(), "前提：有台账");
            PrayAtAltar.record(v).record(today(h), p.getUUID(), a);
            h.setBlock(ALTAR, Blocks.AIR);
            h.assertTrue(RuntimeLedgers.get(server).get(PrayerService.ledgerId(a)).isEmpty(), "台账应注销");
            tryOnce(h, v, a);
            h.assertTrue(PrayAtAltar.record(v).lastAltar() == null, "信徒记录里的神坛应清空");
            h.assertTrue(BelieverEvents.isBeliever(v), "仍是信徒");
            cleanup(h, List.of(village), a);
        });
    }

    // ───────────── 存档往返（TC-T10-58） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void save_load_round_trip(GameTestHelper h) {
        ground(h);
        GlobalPos a = altar(h, ALTAR, owner(h, "pr_save"));
        h.succeedWhen(() -> {
            assertScanned(h, a);
            PrayerRecord rec = new PrayerRecord();
            rec.record(42, UUID.randomUUID(), a);
            h.assertTrue(PrayerRecord.load(rec.save()).equals(rec), "祈祷记录往返一致");
            h.assertTrue(PrayerRecord.load(new PrayerRecord().save()).equals(new PrayerRecord()), "空记录往返一致");

            GreekAltarBlockEntity be = be(h, a);
            long today = today(h);
            be.notePrayer(UUID.randomUUID(), today);
            be.notePrayer(UUID.randomUUID(), today - 3);
            be.notePrayer(UUID.randomUUID(), today - 9);
            h.assertTrue(be.believers(today) == 2, "7 日以前的不计");
            CompoundTag tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
            GreekAltarBlockEntity copy = new GreekAltarBlockEntity(be.getBlockPos(), be.getBlockState());
            copy.loadWithComponents(tag, h.getLevel().registryAccess());
            h.assertTrue(copy.recentPrayers().equals(be.recentPrayers()), "近 7 日信徒往返一致");

            ProducerLedger l = RuntimeLedgers.get(h.getLevel().getServer()).get(PrayerService.ledgerId(a)).orElseThrow();
            h.assertTrue(RuntimeLedgers.read(RuntimeLedgers.write(l)).equals(l), "台账往返一致");
            h.assertTrue(l.id().equals(new ProducerId("greek_temple:" + a.dimension().location() + ":"
                    + a.pos().getX() + "," + a.pos().getY() + "," + a.pos().getZ())), "台账 id 格式");
        });
    }

    // ───────────── 信仰面板与检视界面一致（TC-T10-59） ─────────────

    @GameTest(template = T, batch = "prayer", timeoutTicks = 200)
    public static void grace_rows_match_survey(GameTestHelper h) {
        ground(h);
        FakePlayer p = owner(h, "pr_rows");
        GlobalPos a = altar(h, ALTAR, p);
        h.succeedWhen(() -> {
            assertScanned(h, a);
            GreekAltarBlockEntity be = be(h, a);
            long today = today(h);
            for (int i = 0; i < 3; i++) {
                be.notePrayer(UUID.randomUUID(), today);
            }
            long now = h.getLevel().getGameTime();
            PrayerService.resetForTest(a);
            PrayerService.putClaimForTest(a, UUID.randomUUID(), ALTAR.east(), true, now);
            PrayerService.sampleForTest(h.getLevel().getServer());
            PrayerService.Stats st = PrayerService.stats(be, TempleService.rules());
            h.assertTrue(Math.abs(st.estimate() - PrayerFormula.estimate(3, 2, 1.0, 0)) < EPS, "预计日产按信徒 3、祈祷位 2：" + st);
            AltarSurveyPayload payload = AltarSurveyPayload.of(be, p, TempleService.rules());
            h.assertTrue(payload.estimate() == st.estimate() && payload.prayer().believers() == st.believers()
                    && payload.prayer().today() == st.today() && payload.prayer().praying() == st.praying(),
                    "检视界面与统计一致：" + payload.prayer());
            GraceSource.Row row = GreekTempleSource.INSTANCE.rows(p).stream().filter(r -> r.pos().equals(a)).findFirst()
                    .orElseThrow(() -> new GameTestAssertException("信仰面板没有这座神坛"));
            Object[] args = ((TranslatableContents) row.status().getContents()).getArgs();
            h.assertTrue(row.perDay() == st.estimate() && Integer.valueOf(st.believers()).equals(args[1])
                    && String.format(java.util.Locale.ROOT, "%.1f", st.today()).equals(args[2]),
                    "信仰面板与检视界面一致：" + java.util.Arrays.toString(args));
            // TC-T11a-66：信仰面板的恩宠来源区就是这些行
            io.github.happysnappingturtle.mythicage.core.player.Age before = MythData.get(p).civ(Civilization.GREEK).age();
            MythData.get(p).civ(Civilization.GREEK).setAge(io.github.happysnappingturtle.mythicage.core.player.Age.CLASSICAL);
            var page = io.github.happysnappingturtle.mythicage.faith.FaithService.build(p).pages().get(Civilization.GREEK.ordinal());
            MythData.get(p).civ(Civilization.GREEK).setAge(before);
            var panelRow = page.sources().stream().filter(r -> r.pos().equals(a.pos())).findFirst()
                    .orElseThrow(() -> new GameTestAssertException("面板恩宠来源区没有这座神坛"));
            h.assertTrue(page.sources().size() == GreekTempleSource.INSTANCE.rows(p).size()
                    && panelRow.note().equals(row.status())
                    && String.format(java.util.Locale.ROOT, "%.1f", row.perDay()).equals(
                    ((TranslatableContents) panelRow.value().getContents()).getArgs()[0]), "面板行与来源一致：" + panelRow);
            PrayerService.resetForTest(a);
        });
    }
}
