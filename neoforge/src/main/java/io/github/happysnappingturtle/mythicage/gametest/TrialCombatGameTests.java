package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.boss.TrialArena;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.GreekKnossos;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.MinotaurBossEntity;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.boss.ArenaPhase;
import io.github.happysnappingturtle.mythicage.core.boss.MoveScheduler.Move;
import io.github.happysnappingturtle.mythicage.core.boss.WallCycle;
import io.github.happysnappingturtle.mythicage.entity.MinotaurEntity;
import io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.HITS;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.Setup;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.boss;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.cleanup;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.inHall;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.open;
import static io.github.happysnappingturtle.mythicage.gametest.TrialArenaGameTests.setup;

/**
 * T23 阶段 ②：战斗（单元测试用例文档 TC-T23-57、60～74）。Boss 出生点 = 大厅局部 (0, −2)；P5 柱在局部 (0, 11)（出生点以南 13 格）、
 * P1 柱在局部 (0, −11)（出生点以北 9 格）。Boss 不自己选招，用例强制出招。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrialCombatGameTests {
    private static final String BATCH = "trial_combat";
    private static final String T = GameTestTemplates.EMPTY_3X3;

    private TrialCombatGameTests() {
    }

    static MinotaurBossEntity at(GameTestHelper h, Setup s, int dz, float yaw) {
        MinotaurBossEntity b = (MinotaurBossEntity) boss(h, s);
        BlockPos o = s.origin();
        b.moveTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5 + dz, yaw, 0);
        b.getNavigation().stop();
        return b;
    }

    static int pillar(Setup s, int dz) {
        return s.arena().pillarAt(s.origin().offset(0, 0, dz));
    }

    static boolean broken(GameTestHelper h, Setup s, int dz) {
        BlockPos base = s.origin().offset(0, 0, dz);
        ServerLevel level = h.getLevel();
        for (int y = 2; y < 7; y++) {
            if (!level.getBlockState(base.above(y)).isAir()) {
                return false;
            }
        }
        return !level.getBlockState(base.above(1)).isAir();
    }

    /** TC-T23-60、61、62、73：撞完整的柱 → 柱碎、硬直 60 刻、窗口内受伤 ×2、不继续冲；20 秒内再撞另一根只碎不硬直。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 500)
    public static void pillar_impact_stuns(GameTestHelper h) {
        Setup s = setup(h, 31);
        ServerPlayer south = inHall(h, s, "gt_p_s", 0, 16);
        ServerPlayer north = inHall(h, s, "gt_p_n", 0, -12);
        open(h, s, south);
        AtomicInteger maxStun = new AtomicInteger();
        AtomicBoolean doubled = new AtomicBoolean();
        AtomicInteger stunAfterSecond = new AtomicInteger();
        AtomicBoolean second = new AtomicBoolean();
        h.runAtTickTime(10, () -> at(h, s, 0, 0).forceMoveForTest(Move.CHARGE, south));
        h.onEachTick(() -> s.arena().boss(h.getLevel()).ifPresent(m -> {
            MinotaurBossEntity b = (MinotaurBossEntity) m;
            if (!second.get()) {
                maxStun.set(Math.max(maxStun.get(), b.stunTicks()));
                if (b.stunTicks() == 50 && !doubled.get()) {
                    float before = b.getHealth();
                    b.hurt(h.getLevel().damageSources().magic(), 10);
                    doubled.set(Math.abs(before - b.getHealth() - 20) < 0.01);
                }
            } else {
                stunAfterSecond.set(Math.max(stunAfterSecond.get(), b.stunTicks()));
            }
        }));
        h.runAtTickTime(150, () -> {
            h.assertTrue(broken(h, s, 13), "P5 柱碎了（断桩在、上面空了）");
            h.assertTrue(s.arena().pillarBroken(pillar(s, 13)), "柱状态记为碎");
            h.assertTrue(maxStun.get() == 60, "硬直 60 刻：" + maxStun.get());
            h.assertTrue(doubled.get(), "硬直中受伤 ×2");
            MinotaurBossEntity b = (MinotaurBossEntity) boss(h, s);
            h.assertTrue(b.stunTicks() == 0 && !b.charge().active(), "硬直结束，不继续冲");
            h.assertFalse(HITS.containsKey(south.getUUID()), "被柱挡住，没冲到柱后的人");
            second.set(true);
            at(h, s, 0, 180).forceMoveForTest(Move.CHARGE, north);
        });
        h.runAtTickTime(260, () -> {
            h.assertTrue(broken(h, s, -9), "P1 柱碎了");
            h.assertTrue(stunAfterSecond.get() == 0, "20 秒内再撞：只碎不硬直");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-63：连冲第一段撞柱 → 第二段取消；没撞上时有第二段。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void double_charge_second_segment(GameTestHelper h) {
        Setup s = setup(h, 32);
        ServerPlayer south = inHall(h, s, "gt_d_s", 0, 16);
        ServerPlayer east = inHall(h, s, "gt_d_e", 8, 0);
        open(h, s, south);
        AtomicInteger maxSegment = new AtomicInteger();
        AtomicBoolean phase2 = new AtomicBoolean();
        h.runAtTickTime(10, () -> at(h, s, 0, 0).forceMoveForTest(Move.DOUBLE_CHARGE, south));
        h.onEachTick(() -> s.arena().boss(h.getLevel()).ifPresent(m -> {
            int seg = ((MinotaurBossEntity) m).segment();
            if (!phase2.get()) {
                maxSegment.set(Math.max(maxSegment.get(), seg));
            } else if (seg == 2) {
                maxSegment.set(2);
            }
        }));
        h.runAtTickTime(160, () -> {
            h.assertTrue(maxSegment.get() == 1, "撞柱后第二段取消：" + maxSegment.get());
            phase2.set(true);
            maxSegment.set(0);
            at(h, s, 0, -90).forceMoveForTest(Move.DOUBLE_CHARGE, east);
        });
        h.runAtTickTime(300, () -> {
            h.assertTrue(maxSegment.get() == 2, "没撞上时有第二段");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-64、65：血量 ≤ 50% → P2（只一次）、怒吼后预警再升 A 组。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void phase_two_raises_walls(GameTestHelper h) {
        Setup s = setup(h, 33);
        ServerPlayer p = inHall(h, s, "gt_p2", 0, 8);
        open(h, s, p);
        AtomicInteger roar = new AtomicInteger();
        h.runAtTickTime(10, () -> {
            MinotaurBossEntity b = (MinotaurBossEntity) boss(h, s);
            b.setHealth(b.getMaxHealth() * 0.4F);
        });
        h.onEachTick(() -> s.arena().boss(h.getLevel())
                .ifPresent(m -> roar.set(Math.max(roar.get(), ((MinotaurBossEntity) m).roarTicks()))));
        h.runAtTickTime(40, () -> {
            h.assertTrue(s.arena().phase() == ArenaPhase.FIGHT_P2, "P2");
            h.assertTrue(s.arena().wallLayers(WallCycle.Group.A) == 0, "怒吼中不升墙");
        });
        h.runAtTickTime(130, () -> {
            h.assertTrue(roar.get() == 40, "怒吼 40 刻：" + roar.get());
            h.assertTrue(allWall(h, s, WallCycle.Group.A, true), "A 组升满");
            h.assertTrue(allWall(h, s, WallCycle.Group.B, false), "B 组没升");
            boss(h, s).setHealth(boss(h, s).getMaxHealth() * 0.2F);
        });
        h.runAtTickTime(140, () -> {
            h.assertTrue(s.arena().phase() == ArenaPhase.FIGHT_P2 && roar.get() == 40, "只切换一次");
            cleanup(h, s);
            h.succeed();
        });
    }

    static boolean allWall(GameTestHelper h, Setup s, WallCycle.Group g, boolean up) {
        Block wall = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mythic_age:plaster"));
        for (BlockPos base : s.arena().wall(g)) {
            for (int y = 0; y < 4; y++) {
                boolean isWall = h.getLevel().getBlockState(base.above(y)).is(wall);
                if (isWall != up) {
                    return false;
                }
            }
        }
        return true;
    }

    static ArmorStand standOn(GameTestHelper h, BlockPos p) {
        ArmorStand stand = EntityType.ARMOR_STAND.create(h.getLevel());
        stand.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        h.getLevel().addFreshEntity(stand);
        return stand;
    }

    /** TC-T23-66：A 组格子里有东西 → 整组不升，走开后升；一直不走 100 刻后取消本次切换。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void walls_postpone_when_occupied(GameTestHelper h) {
        Setup s = setup(h, 34);
        ServerPlayer p = inHall(h, s, "gt_wp", 0, 8);
        open(h, s, p);
        ArmorStand[] stand = new ArmorStand[1];
        h.runAtTickTime(10, () -> {
            stand[0] = standOn(h, s.arena().wall(WallCycle.Group.A).get(3));
            MinotaurBossEntity b = (MinotaurBossEntity) boss(h, s);
            b.setHealth(b.getMaxHealth() * 0.4F);
        });
        h.runAtTickTime(110, () -> {
            h.assertTrue(s.arena().wallLayers(WallCycle.Group.A) == 0, "有人：整组不升");
            h.assertTrue(s.arena().wallCycle().pending().contains(WallCycle.Group.A), "延期中");
        });
        h.runAtTickTime(200, () -> {
            h.assertTrue(s.arena().wallLayers(WallCycle.Group.A) == 0, "一直有人：仍不升");
            h.assertTrue(s.arena().wallCycle().pending().isEmpty() && s.arena().wallCycle().current() == WallCycle.Layout.NONE,
                    "累计 100 刻取消本次切换，保持当前布局");
            stand[0].discard();
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-66（走开后升起）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void walls_rise_after_clear(GameTestHelper h) {
        Setup s = setup(h, 35);
        ServerPlayer p = inHall(h, s, "gt_wr", 0, 8);
        open(h, s, p);
        ArmorStand[] stand = new ArmorStand[1];
        h.runAtTickTime(10, () -> {
            stand[0] = standOn(h, s.arena().wall(WallCycle.Group.A).get(3));
            MinotaurBossEntity b = (MinotaurBossEntity) boss(h, s);
            b.setHealth(b.getMaxHealth() * 0.4F);
        });
        h.runAtTickTime(100, () -> stand[0].discard());
        h.runAtTickTime(150, () -> {
            h.assertTrue(allWall(h, s, WallCycle.Group.A, true), "走开后 A 组升起");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-67：A+B 都升起时大厅仍连通（入口安全点走得到 Boss 出生点与 8 根柱旁）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void hall_connected_with_walls(GameTestHelper h) {
        Setup s = setup(h, 36);
        ServerLevel level = h.getLevel();
        s.arena().setWall(level, WallCycle.Group.A, true);
        s.arena().setWall(level, WallCycle.Group.B, true);
        h.assertTrue(allWall(h, s, WallCycle.Group.A, true) && allWall(h, s, WallCycle.Group.B, true), "两组升满");
        BlockPos from = s.safe();
        h.assertTrue(LabyrinthGameTests.reachable(level, from, s.origin()), "走得到 Boss 出生点");
        for (BlockPos pillar : s.arena().pillars()) {
            h.assertTrue(LabyrinthGameTests.reachable(level, from, pillar), "走得到柱旁 " + pillar);
        }
        s.arena().setWall(level, WallCycle.Group.A, false);
        s.arena().setWall(level, WallCycle.Group.B, false);
        h.assertTrue(allWall(h, s, WallCycle.Group.A, false) && allWall(h, s, WallCycle.Group.B, false), "落下后还原");
        cleanup(h, s);
        h.succeed();
    }

    /** TC-T23-68：升到一半的墙不算「认可的墙」；升满才算。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void half_raised_wall_not_counted(GameTestHelper h) {
        Setup s = setup(h, 37);
        ServerLevel level = h.getLevel();
        TrialArena a = s.arena();
        a.setWallTarget(WallCycle.Group.A, true);
        a.stepWallForTest(level, WallCycle.Group.A);
        a.stepWallForTest(level, WallCycle.Group.A);
        BlockPos cell = a.wall(WallCycle.Group.A).get(0);
        h.assertTrue(a.classify(level, List.of(cell)).kind() == TrialArena.ImpactKind.NONE, "升到一半：不算");
        a.stepWallForTest(level, WallCycle.Group.A);
        a.stepWallForTest(level, WallCycle.Group.A);
        h.assertTrue(a.classify(level, List.of(cell)).kind() == TrialArena.ImpactKind.WALL, "升满：算墙");
        BlockPos pillarCell = a.pillars().get(0).above(3);
        h.assertTrue(a.classify(level, List.of(cell, pillarCell)).kind() == TrialArena.ImpactKind.INTACT_PILLAR, "柱优先");
        a.setWall(level, WallCycle.Group.A, false);
        cleanup(h, s);
        h.succeed();
    }

    /** TC-T23-69：8 柱全碎后撞墙 → 硬直 40 刻。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void wall_stun_after_all_pillars(GameTestHelper h) {
        Setup s = setup(h, 38);
        ServerPlayer north = inHall(h, s, "gt_w_n", 0, -12);
        open(h, s, north);
        AtomicInteger maxStun = new AtomicInteger();
        h.runAtTickTime(10, () -> {
            for (int i = 0; i < 8; i++) {
                s.arena().markPillarBroken(i);
            }
            at(h, s, 0, 180).forceMoveForTest(Move.CHARGE, north);
        });
        h.onEachTick(() -> s.arena().boss(h.getLevel())
                .ifPresent(m -> maxStun.set(Math.max(maxStun.get(), ((MinotaurBossEntity) m).stunTicks()))));
        h.runAtTickTime(120, () -> {
            h.assertTrue(maxStun.get() == 40, "撞墙硬直 40 刻：" + maxStun.get());
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-70：窗口满 12 次后撞柱只碎不硬直；Boss 照样打得死（胜利、开门、开宝库门）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void capped_windows_still_killable(GameTestHelper h) {
        Setup s = setup(h, 39);
        ServerPlayer south = inHall(h, s, "gt_cap", 0, 16);
        open(h, s, south);
        AtomicInteger maxStun = new AtomicInteger();
        h.runAtTickTime(10, () -> {
            s.arena().windows().restore(12, -100000);
            at(h, s, 0, 0).forceMoveForTest(Move.CHARGE, south);
        });
        h.onEachTick(() -> s.arena().boss(h.getLevel())
                .ifPresent(m -> maxStun.set(Math.max(maxStun.get(), ((MinotaurBossEntity) m).stunTicks()))));
        h.runAtTickTime(120, () -> {
            h.assertTrue(broken(h, s, 13), "柱照样碎");
            h.assertTrue(maxStun.get() == 0, "满 12 次不硬直");
            boss(h, s).hurt(h.getLevel().damageSources().playerAttack(south), 10000);
        });
        h.runAtTickTime(130, () -> {
            h.assertTrue(s.arena().phase() == ArenaPhase.VICTORY, "胜利：" + s.arena().phase());
            h.assertTrue(s.arena().vaultOpen(), "宝库门开了");
            h.assertTrue(s.arena().vaultDoor().stream().allMatch(p -> h.getLevel().getBlockState(p).isAir()), "宝库门槽空了");
            h.assertTrue(s.arena().gateOpen(h.getLevel()), "门开着");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-71：掷碎石命中一次、落地不破坏方块。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void throw_rubble_hits_once(GameTestHelper h) {
        Setup s = setup(h, 40);
        ServerPlayer p = inHall(h, s, "gt_throw", 0, 10);
        open(h, s, p);
        ArmorStand[] stand = new ArmorStand[1];
        h.runAtTickTime(10, () -> {
            stand[0] = standOn(h, s.origin().offset(0, 0, 10));
            at(h, s, 0, 0).forceMoveForTest(Move.THROW, p);
        });
        h.runAtTickTime(90, () -> {
            List<Move> hits = HITS.get(stand[0].getUUID());
            h.assertTrue(hits != null && hits.equals(List.of(Move.THROW)), "命中一次：" + hits);
            h.assertFalse(h.getLevel().getBlockState(s.origin().offset(0, -1, 10)).isAir(), "地板没坏");
            stand[0].discard();
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-72：P2 手持线团在大厅里 → 每秒指一次 Boss。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void thread_points_to_boss(GameTestHelper h) {
        Setup s = setup(h, 41);
        ServerPlayer p = inHall(h, s, "gt_point", 0, 8);
        p.setItemInHand(InteractionHand.MAIN_HAND, TrialArenaGameTests.thread());
        open(h, s, p);
        h.runAtTickTime(10, () -> boss(h, s).setHealth(boss(h, s).getMaxHealth() * 0.4F));
        h.runAtTickTime(80, () -> {
            h.assertTrue(s.arena().pointerSendsForTest >= 2, "指过 Boss：" + s.arena().pointerSendsForTest);
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-57：召唤版牛头人冲锋撞大厅柱：柱不碎、竞技场不变（MIN-01、MIN-11）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void summon_minotaur_cannot_break_pillars(GameTestHelper h) {
        Setup s = setup(h, 42);
        MinotaurEntity m = GreekSummons.MINOTAUR.get().create(h.getLevel());
        BlockPos o = s.origin();
        m.moveTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5 + 6, 0, 0);
        h.getLevel().addFreshEntity(m);
        m.startCharge(10);
        h.runAtTickTime(80, () -> {
            h.assertFalse(broken(h, s, 13), "柱完好");
            h.assertTrue(s.arena().phase() == ArenaPhase.IDLE && !s.arena().pillarBroken(pillar(s, 13)), "竞技场不变");
            m.discard();
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-74：复原：碎柱与墙写回；被玩家改过的格子不覆盖。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void reset_restores_pillars_and_walls(GameTestHelper h) {
        Setup s = setup(h, 43);
        ServerLevel level = h.getLevel();
        TrialArena a = s.arena();
        int i = pillar(s, 13);
        a.breakPillar(level, i, net.minecraft.core.Direction.NORTH);
        a.setWall(level, WallCycle.Group.B, true);
        BlockPos changed = s.origin().offset(0, 4, 13);
        level.setBlock(changed, Blocks.GLASS.defaultBlockState(), 2);
        a.reset(level);
        BlockPos base = s.origin().offset(0, 0, 13);
        h.assertTrue(level.getBlockState(changed).is(Blocks.GLASS), "玩家改过的格子不覆盖");
        h.assertFalse(level.getBlockState(base.above(3)).isAir() || level.getBlockState(base.above(6)).isAir(), "柱写回");
        h.assertTrue(level.getBlockState(base.north()).isAir(), "碎块拿掉");
        h.assertTrue(allWall(h, s, WallCycle.Group.B, false), "墙落下");
        h.assertFalse(a.pillarBroken(i), "柱状态清零");
        level.setBlock(changed, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mythic_age:marble_column"))
                .defaultBlockState(), 2);
        cleanup(h, s);
        h.succeed();
    }
}
