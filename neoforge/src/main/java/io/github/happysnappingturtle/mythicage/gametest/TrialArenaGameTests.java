package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.boss.TrialArena;
import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.BullGateBlock;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.GreekKnossos;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.MinotaurBossEntity;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.ThreadAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.ThreadTrails;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.boss.ArenaPhase;
import io.github.happysnappingturtle.mythicage.core.boss.MoveScheduler.Move;
import io.github.happysnappingturtle.mythicage.power.TempBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * T23 阶段 ①：试炼竞技场框架与入场（单元测试用例文档 TC-T23-40～58）。每个用例在远处槽位摆一座迷宫（{@link LabyrinthGameTests#place}），
 * 调登记处理建立竞技场实例，强制加载大厅一带的区块让竞技场与 Boss 每刻推进。假玩家放进 {@link TrialArena#TEST_PLAYERS}；
 * 假玩家不会受伤，命中用 {@link MinotaurBossEntity#onHitForTest} 记录。Boss 不自己选招，用例里强制出招。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrialArenaGameTests {
    private static final String BATCH = "trial_arena";
    private static final String T = GameTestTemplates.EMPTY_3X3;
    static final Map<UUID, List<Move>> HITS = new ConcurrentHashMap<>();

    static {
        MinotaurBossEntity.onHitForTest = (m, id) -> HITS.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(m);
    }

    private TrialArenaGameTests() {
    }

    record Setup(LabyrinthGameTests.Placed placed, TrialArena arena, List<ChunkPos> forced, List<ServerPlayer> players) {
        BlockPos origin() {
            return arena.bossOrigin();
        }

        /** 大厅入口安全点（局部 (0, 13)）。 */
        BlockPos safe() {
            return arena.center().south(13);
        }
    }

    static Setup setup(GameTestHelper h, long seed) {
        MinotaurBossEntity.pauseAllForTest = true;
        ServerLevel level = h.getLevel();
        LabyrinthGameTests.Placed p = LabyrinthGameTests.place(h, seed, false);
        GreekKnossos.register(level, p.info());
        BlockPos o = LabyrinthGameTests.one(p, "boss_origin");
        TrialArena a = TrialArenas.get(level.getServer()).byId(TrialArenas.idFor(level, o))
                .orElseThrow(() -> new GameTestAssertException("没有建立竞技场实例"));
        List<ChunkPos> forced = new ArrayList<>();
        for (int cx = (o.getX() - 24) >> 4; cx <= (o.getX() + 24) >> 4; cx++) {
            for (int cz = (o.getZ() - 24) >> 4; cz <= (o.getZ() + 28) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                forced.add(new ChunkPos(cx, cz));
            }
        }
        return new Setup(p, a, forced, new ArrayList<>());
    }

    static ServerPlayer player(GameTestHelper h, Setup s, String name, double x, double y, double z) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.setGameMode(GameType.SURVIVAL);
        p.moveTo(x, y, z, 0, 0);
        TrialArena.TEST_PLAYERS.add(p);
        s.players().add(p);
        return p;
    }

    static ServerPlayer inHall(GameTestHelper h, Setup s, String name, int dx, int dz) {
        BlockPos o = s.origin();
        return player(h, s, name, o.getX() + 0.5 + dx, o.getY(), o.getZ() + 0.5 + dz);
    }

    static void cleanup(GameTestHelper h, Setup s) {
        ServerLevel level = h.getLevel();
        s.arena().boss(level).ifPresent(Mob::discard);
        TrialArena.TEST_PLAYERS.removeAll(s.players());
        TrialArenas.get(level.getServer()).forgetForTest(s.arena().id());
        for (ChunkPos c : s.forced()) {
            level.setChunkForced(c.x, c.z, false);
        }
        LabyrinthGameTests.cleanup(level, s.placed());
    }

    static ItemStack thread() {
        return new ItemStack(GreekKnossos.ARIADNE_THREAD.get());
    }

    static void open(GameTestHelper h, Setup s, ServerPlayer p) {
        s.arena().useGate(h.getLevel(), p, thread());
    }

    static Mob boss(GameTestHelper h, Setup s) {
        return s.arena().boss(h.getLevel()).orElseThrow(() -> new GameTestAssertException("Boss 没有出现"));
    }

    static boolean gateClosed(GameTestHelper h, Setup s) {
        return s.arena().gate().stream().allMatch(p -> h.getLevel().getBlockState(p).getBlock() instanceof BullGateBlock);
    }

    /** TC-T23-40：登记后入口有线团祭坛、门洞是 25 格牛首封门（一格牛首）、竞技场 IDLE。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void arena_registers(GameTestHelper h) {
        Setup s = setup(h, 11);
        ServerLevel level = h.getLevel();
        BlockPos t = LabyrinthGameTests.one(s.placed(), "thread");
        h.assertTrue(level.getBlockState(t).is(GreekKnossos.THREAD_ALTAR.get()), "线团祭坛在入口格中心");
        h.assertTrue(s.arena().gate().size() == 25 && gateClosed(h, s), "门洞 25 格都是牛首封门");
        long heads = s.arena().gate().stream().filter(p -> level.getBlockState(p).getValue(BullGateBlock.HEAD)).count();
        h.assertTrue(heads == 1, "一格牛首：" + heads);
        h.assertTrue(s.arena().phase() == ArenaPhase.IDLE, "IDLE");
        h.assertTrue(s.arena().pillars().size() == 8 && s.arena().vaultDoor().size() == 24, "8 柱、宝库门 3×4×2");
        cleanup(h, s);
        h.succeed();
    }

    /** TC-T23-41：祭坛：背包没线团给一个，有了就不给。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void altar_gives_one_thread(GameTestHelper h) {
        Setup s = setup(h, 12);
        ServerPlayer p = player(h, s, "gt_thread", s.origin().getX(), s.origin().getY(), s.origin().getZ() + 60);
        h.assertTrue(ThreadAltarBlock.take(p), "第一次给");
        h.assertFalse(ThreadAltarBlock.take(p), "已有就不给");
        int n = p.getInventory().countItem(GreekKnossos.ARIADNE_THREAD.get());
        h.assertTrue(n == 1, "只有 1 个：" + n);
        cleanup(h, s);
        h.succeed();
    }

    /** TC-T23-42、43：空手开不了；持线团打开、线团不消耗。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void gate_needs_thread(GameTestHelper h) {
        Setup s = setup(h, 13);
        ServerPlayer p = player(h, s, "gt_gate", s.origin().getX(), s.origin().getY(), s.arena().gate().get(0).getZ() + 2);
        s.arena().useGate(h.getLevel(), p, ItemStack.EMPTY);
        h.assertTrue(s.arena().phase() == ArenaPhase.IDLE && gateClosed(h, s), "空手不开");
        ItemStack th = thread();
        s.arena().useGate(h.getLevel(), p, th);
        h.assertTrue(s.arena().phase() == ArenaPhase.OPEN, "OPEN");
        h.assertTrue(s.arena().gate().stream().allMatch(q -> h.getLevel().getBlockState(q).isAir()), "门洞变空");
        h.assertTrue(th.getCount() == 1, "线团不消耗（D1）");
        cleanup(h, s);
        h.succeed();
    }

    /** TC-T23-44、45、54：有人进大厅 → Boss 出现、320 血、FIGHT_P1、血条；门洞里有东西时不关门，清掉后关上。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void fight_starts_and_gate_closes(GameTestHelper h) {
        Setup s = setup(h, 14);
        ServerLevel level = h.getLevel();
        ServerPlayer p = inHall(h, s, "gt_enter", 0, 6);
        open(h, s, p);
        BlockPos g = s.arena().gate().get(2);
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
        level.addFreshEntity(stand);
        h.runAtTickTime(10, () -> {
            Mob b = boss(h, s);
            h.assertTrue(s.arena().phase() == ArenaPhase.FIGHT_P1, "FIGHT_P1：" + s.arena().phase());
            h.assertTrue(Math.abs(b.getMaxHealth() - 320) < 0.01 && Math.abs(b.getHealth() - 320) < 0.01, "320 血：" + b.getMaxHealth());
            h.assertTrue(b.blockPosition().closerThan(s.origin(), 2), "在出生点");
            h.assertTrue(s.arena().barForTest().map(bar -> bar.getPlayers().contains(p)).orElse(false), "血条");
        });
        h.runAtTickTime(80, () -> {
            h.assertFalse(gateClosed(h, s), "门洞里有盔甲架：不关门");
            stand.discard();
        });
        h.runAtTickTime(110, () -> {
            h.assertTrue(gateClosed(h, s), "清掉后关门");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-46：两人开战 480；第三人进来 640 且比例不变；有人走不降。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void health_scales_with_players(GameTestHelper h) {
        Setup s = setup(h, 15);
        ServerPlayer a = inHall(h, s, "gt_s1", -3, 6);
        inHall(h, s, "gt_s2", 3, 6);
        open(h, s, a);
        h.runAtTickTime(10, () -> {
            Mob b = boss(h, s);
            h.assertTrue(Math.abs(b.getMaxHealth() - 480) < 0.01, "两人 480：" + b.getMaxHealth());
            b.setHealth(360);
            inHall(h, s, "gt_s3", 0, 9);
        });
        h.runAtTickTime(20, () -> {
            Mob b = boss(h, s);
            h.assertTrue(Math.abs(b.getMaxHealth() - 640) < 0.01, "三人 640：" + b.getMaxHealth());
            h.assertTrue(Math.abs(b.getHealth() - 480) < 0.5, "比例 75% 不变：" + b.getHealth());
            a.moveTo(a.getX(), a.getY(), s.origin().getZ() + 60);
        });
        h.runAtTickTime(30, () -> {
            h.assertTrue(Math.abs(boss(h, s).getMaxHealth() - 640) < 0.01, "有人走不降");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-47、48：战中大厅一侧右键门开 60 刻；Boss 出了大厅被传回。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void exit_gate_and_leash(GameTestHelper h) {
        Setup s = setup(h, 16);
        ServerPlayer p = inHall(h, s, "gt_exit", 0, 8);
        open(h, s, p);
        h.runAtTickTime(70, () -> {
            h.assertTrue(gateClosed(h, s), "开战后关门");
            s.arena().useGate(h.getLevel(), p, ItemStack.EMPTY);
            h.assertFalse(gateClosed(h, s), "大厅一侧空手也能开门撤离");
            Mob b = boss(h, s);
            BlockPos out = s.arena().gate().get(2).south(3);
            b.teleportTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5);
        });
        h.runAtTickTime(75, () -> h.assertTrue(boss(h, s).blockPosition().closerThan(s.origin(), 3), "Boss 被传回出生点"));
        h.runAtTickTime(150, () -> {
            h.assertTrue(gateClosed(h, s), "60 刻后关门");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-49：大厅空 600 刻 → 复原：Boss 消失、门关上、IDLE。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 900)
    public static void empty_arena_resets(GameTestHelper h) {
        Setup s = setup(h, 17);
        ServerPlayer p = inHall(h, s, "gt_leave", 0, 8);
        open(h, s, p);
        h.runAtTickTime(10, () -> {
            boss(h, s);
            p.moveTo(p.getX(), p.getY(), s.origin().getZ() + 60);
        });
        h.runAtTickTime(500, () -> h.assertTrue(s.arena().phase() == ArenaPhase.FIGHT_P1, "599 刻内不复原"));
        h.runAtTickTime(640, () -> {
            h.assertTrue(s.arena().phase() == ArenaPhase.IDLE, "复原到 IDLE：" + s.arena().phase());
            h.assertTrue(s.arena().boss(h.getLevel()).isEmpty(), "Boss 消失");
            h.assertTrue(gateClosed(h, s), "门关上");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** T24 专服回归：重连位置就是大厅内，不能先刷新 lastPresent 而跳过已过期战斗。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void reconnect_inside_expired_arena_resets(GameTestHelper h) {
        Setup s = setup(h, 99);
        ServerPlayer p = inHall(h, s, "gt_reconnect_inside", 0, 8);
        open(h, s, p);
        h.runAtTickTime(10, () -> {
            boss(h, s);
            s.arena().awayForTest(h.getLevel().getGameTime(), 5000);
            // 保留玩家在大厅里，模拟重新加载第一刻就已在场的登录位置。
        });
        h.runAtTickTime(14, () -> {
            h.assertTrue(s.arena().phase() == ArenaPhase.IDLE, "超时后在大厅内重连也复原");
            h.assertTrue(s.arena().boss(h.getLevel()).isEmpty(), "旧 Boss 消失而非结算胜利");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-49（补）：玩家跑远、大厅区块卸载期间计时不推进——回来第一刻按离开时长复原；过期的 Boss 实体自行消失。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void long_absence_resets_and_stale_boss_vanishes(GameTestHelper h) {
        Setup s = setup(h, 22);
        ServerLevel level = h.getLevel();
        ServerPlayer p = inHall(h, s, "gt_away", 0, 8);
        open(h, s, p);
        Mob[] stale = new Mob[1];
        h.runAtTickTime(10, () -> {
            boss(h, s);
            p.moveTo(p.getX(), p.getY(), s.origin().getZ() + 60);
            s.arena().awayForTest(level.getGameTime(), 5000);
        });
        h.runAtTickTime(14, () -> {
            h.assertTrue(s.arena().phase() == ArenaPhase.IDLE, "离开早已超时：立即复原");
            // 模拟区块卸载时没被清掉、之后才载入的旧 Boss
            Mob m = GreekKnossos.MINOTAUR_BOSS.get().create(level);
            ((MinotaurBossEntity) m).bindArena(s.arena().id());
            m.moveTo(s.origin().getX() + 0.5, s.origin().getY(), s.origin().getZ() + 0.5);
            level.addFreshEntity(m);
            stale[0] = m;
        });
        h.runAtTickTime(60, () -> {
            h.assertTrue(stale[0].isRemoved(), "过期的 Boss 自行消失");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-50、51、52：挖不动、炸不坏；战中不能放方块；神力临时方块被拒。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void arena_is_protected(GameTestHelper h) {
        Setup s = setup(h, 18);
        ServerLevel level = h.getLevel();
        ServerPlayer p = inHall(h, s, "gt_dig", 0, 8);
        BlockPos floor = s.origin().south(4).below();
        p.gameMode.destroyBlock(floor);
        h.assertFalse(level.getBlockState(floor).isAir(), "挖不动地板");
        BlockPos pillar = s.arena().pillars().get(0).above(2);
        p.gameMode.destroyBlock(pillar);
        h.assertFalse(level.getBlockState(pillar).isAir(), "挖不动柱");
        level.explode(null, floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5, 4, Level.ExplosionInteraction.TNT);
        h.assertFalse(level.getBlockState(floor).isAir(), "炸不坏");
        h.assertTrue(TempBlocks.place(level, floor.above(), Blocks.STONE.defaultBlockState(), 100, p.getUUID(), false)
                == TempBlocks.Result.REFUSED_PROTECTED, "神力临时方块被拒");
        BlockPos at = floor.above();
        h.assertFalse(place(level, p, at), "IDLE 时可以放");
        open(h, s, p);
        h.runAtTickTime(10, () -> {
            h.assertTrue(place(level, p, at.east()), "战中不能放");
            cleanup(h, s);
            h.succeed();
        });
    }

    /** 发一次放方块事件，返回是否被取消。 */
    static boolean place(ServerLevel level, ServerPlayer p, BlockPos at) {
        BlockEvent.EntityPlaceEvent e = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, at),
                Blocks.STONE.defaultBlockState(), p);
        NeoForge.EVENT_BUS.post(e);
        return e.isCanceled();
    }

    /** TC-T23-53：持线团在迷宫里走记点；没有线团、或在迷宫外不记。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void thread_trail_records(GameTestHelper h) {
        Setup s = setup(h, 19);
        BoundingBox r = s.arena().region();
        ServerPlayer p = player(h, s, "gt_trail", r.minX() + 4.5, s.origin().getY(), r.minZ() + 4.5);
        ThreadTrails.clearForTest(p.getUUID());
        for (int i = 0; i < 15; i++) {
            p.moveTo(r.minX() + 4.5 + i * 2, s.origin().getY(), r.minZ() + 4.5);
            ThreadTrails.tick(p);
        }
        h.assertTrue(ThreadTrails.pointsForTest(p.getUUID()) == 0, "没有线团不记");
        p.getInventory().add(thread());
        for (int i = 0; i < 15; i++) {
            p.moveTo(r.minX() + 4.5 + i * 2, s.origin().getY(), r.minZ() + 8.5);
            ThreadTrails.tick(p);
        }
        int n = ThreadTrails.pointsForTest(p.getUUID());
        h.assertTrue(n >= 14, "走 30 格记 ≥ 14 个点：" + n);
        p.moveTo(r.maxX() + 50, s.origin().getY(), r.maxZ() + 50);
        ThreadTrails.tick(p);
        h.assertTrue(ThreadTrails.pointsForTest(p.getUUID()) == n, "迷宫外不记，线保留");
        ThreadTrails.clearForTest(p.getUUID());
        cleanup(h, s);
        h.succeed();
    }

    /** TC-T23-55：斧劈只打前方扇形里、视线通的人，每人一次。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void axe_hits_front_only(GameTestHelper h) {
        Setup s = setup(h, 20);
        ServerLevel level = h.getLevel();
        ServerPlayer front = inHall(h, s, "gt_front", 0, 3);
        ServerPlayer behind = inHall(h, s, "gt_behind", 0, -3);
        ServerPlayer walled = inHall(h, s, "gt_walled", 2, 3);
        open(h, s, front);
        h.runAtTickTime(10, () -> {
            MinotaurBossEntity b = (MinotaurBossEntity) boss(h, s);
            BlockPos o = s.origin();
            for (int dy = 0; dy < 3; dy++) {
                level.setBlock(o.offset(1, dy, 2), Blocks.STONE.defaultBlockState(), 2);
            }
            b.moveTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5, 0, 0);
            b.forceMoveForTest(Move.AXE, front);
        });
        h.runAtTickTime(60, () -> {
            h.assertTrue(List.of(Move.AXE).equals(HITS.get(front.getUUID())), "前方挨一次：" + HITS.get(front.getUUID()));
            h.assertTrue(!HITS.containsKey(behind.getUUID()), "身后不挨");
            h.assertTrue(!HITS.containsKey(walled.getUUID()), "墙后不挨");
            for (int dy = 0; dy < 3; dy++) {
                level.setBlock(s.origin().offset(1, dy, 2), Blocks.AIR.defaultBlockState(), 2);
            }
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-56：冲锋路径上的两人各挨一次 16。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void charge_hits_each_once(GameTestHelper h) {
        Setup s = setup(h, 21);
        ServerPlayer near = inHall(h, s, "gt_c1", 0, 6);
        ServerPlayer far = inHall(h, s, "gt_c2", 0, 10);
        open(h, s, near);
        h.runAtTickTime(10, () -> {
            MinotaurBossEntity b = (MinotaurBossEntity) boss(h, s);
            BlockPos o = s.origin();
            b.moveTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5, 0, 0);
            b.forceMoveForTest(Move.CHARGE, far);
        });
        h.runAtTickTime(90, () -> {
            h.assertTrue(List.of(Move.CHARGE).equals(HITS.get(near.getUUID())), "近的挨一次：" + HITS.get(near.getUUID()));
            h.assertTrue(List.of(Move.CHARGE).equals(HITS.get(far.getUUID())), "远的挨一次：" + HITS.get(far.getUUID()));
            cleanup(h, s);
            h.succeed();
        });
    }

    /** TC-T23-58：Boss 在 #mythic_age:boss 里（神力按 Boss 处理）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void boss_is_tagged(GameTestHelper h) {
        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "boss"));
        h.assertTrue(GreekKnossos.MINOTAUR_BOSS.get().is(tag), "#mythic_age:boss");
        h.succeed();
    }
}
