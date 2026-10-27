package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.content.greek.power.LurePower;
import io.github.happysnappingturtle.mythicage.content.greek.power.LureStoneBlock;
import io.github.happysnappingturtle.mythicage.content.greek.power.SentinelEntity;
import io.github.happysnappingturtle.mythicage.content.greek.power.SentinelPower;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.power.PowerReason;
import io.github.happysnappingturtle.mythicage.core.power.PowerSpec;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.power.PowerService;
import io.github.happysnappingturtle.mythicage.power.PowerSpecLoader;
import io.github.happysnappingturtle.mythicage.power.PowerZones;
import io.github.happysnappingturtle.mythicage.power.TransientEntities;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * T13 阶段 ③：诱兽石（TC-T13-60～65）、哨兵石像（TC-T13-66～72）。
 * 测试施法者是假玩家，不在玩家列表里，所以哨兵按「主人离线」规则只打实现 Enemy 的怪；
 * 「队友挡在射线上」用村民代替队友（同属友方，见实现记录）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LureSentinelGameTests {
    private static final String BATCH = "greek_power3";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    /** 诱兽石在本批次只持续 400 刻（正式 3600）。 */
    static final int LURE_TEST_TICKS = 400;

    private LureSentinelGameTests() {
    }

    /** 本批次施放出来的哨兵只活这么久（正式 1–3 个 MC 日），免得整套测试期间一直朝相邻结构里的怪放箭。 */
    static final long SENTINEL_TEST_LIFE = 400;

    @BeforeBatch(batch = BATCH)
    public static void before(ServerLevel level) {
        SentinelPower.lifetimeOverrideForTest(SENTINEL_TEST_LIFE);
        PowerSpec s = PowerSpecLoader.get(GreekPowers.LURE).orElseThrow();
        PowerSpecLoader.overrideForTest(new PowerSpec(s.id(), s.card(), s.civ(), s.form(), s.castTicks(), s.cooldownTicks(),
                s.range(), s.profile(), s.selfCentered(), s.large(), s.matrix(), LURE_TEST_TICKS, s.radius()));
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        PowerSpecLoader.overrideForTest(null);
        SentinelPower.lifetimeOverrideForTest(null);
        PowerService.clearForTest();
        TestMobs.clear(level, e -> e instanceof SentinelEntity);
    }

    // ───────────── 搭建 ─────────────

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    /**
     * 地面加一圈 3 格高的屏障墙：诱兽石会把动物拉来拉去，动物跑出结构会挤到相邻测试（并行运行）里，
     * 挡住那边村民的路。
     */
    private static void pen(GameTestHelper h) {
        floor(h);
        for (int y = 2; y <= 4; y++) {
            for (int x = 0; x < 17; x++) {
                h.setBlock(x, y, 0, Blocks.BARRIER);
                h.setBlock(x, y, 35, Blocks.BARRIER);
            }
            for (int z = 0; z < 36; z++) {
                h.setBlock(0, y, z, Blocks.BARRIER);
                h.setBlock(16, y, z, Blocks.BARRIER);
            }
        }
    }

    /** 测试结束：移除本结构里的哨兵与箭。 */
    private static void clearSentinels(GameTestHelper h) {
        h.getLevel().getEntitiesOfClass(SentinelEntity.class, h.getBounds()).forEach(e -> e.discard());
        h.getLevel().getEntitiesOfClass(Arrow.class, h.getBounds().inflate(8)).forEach(e -> e.discard());
    }

    /** 测试结束：移除本结构里的动物与鸡（结构外不动）。 */
    private static void clearAnimals(GameTestHelper h) {
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, h.getBounds()).forEach(e -> e.discard());
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.WaterAnimal.class, h.getBounds()).forEach(e -> e.discard());
    }

    private static PowerGameTests.Vulnerable caster(GameTestHelper h, String name, Item power, int count) {
        PowerGameTests.Vulnerable p = new PowerGameTests.Vulnerable(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        BlockPos at = h.absolutePos(new BlockPos(8, 2, 4));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        p.getInventory().clearContent();
        p.getInventory().selected = 0;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(power, count));
        return p;
    }

    /** 让施法者看向结构里某个地面格子的顶面。 */
    private static void lookAtFloor(GameTestHelper h, Player p, int x, int z) {
        BlockPos b = h.absolutePos(new BlockPos(x, 1, z));
        p.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(b.getX() + 0.5, b.getY() + 1.0, b.getZ() + 0.5));
    }

    private static String cast(Player p) {
        PowerService.use((ServerPlayer) p, InteractionHand.MAIN_HAND);
        String id = PowerService.sessionForTest((ServerPlayer) p).orElseThrow().castId();
        PowerService.use((ServerPlayer) p, InteractionHand.MAIN_HAND);
        return id;
    }

    private static boolean committed(Player p) {
        return PowerService.lastResultForTest((ServerPlayer) p).orElse(null) == PowerReason.COMMITTED;
    }

    private static void resetCooldowns(Player p) {
        MythData.get((ServerPlayer) p).civ(Civilization.GREEK).setPowerCooldownUntil(0);
        MythData.get((ServerPlayer) p).powerCooldowns().clear();
    }

    private static List<PowerZones.Zone> zonesOf(GameTestHelper h, String castId) {
        return PowerZones.all(h.getLevel().getServer()).stream().filter(z -> z.castId().equals(castId)).toList();
    }

    private static <T extends Mob> T animal(GameTestHelper h, EntityType<T> type, int x, int z) {
        return TestMobs.tag(h.spawn(type, new BlockPos(x, 2, z)));
    }

    /** 施放诱兽石（石头放在 (x, 2, z)），提交后调用 then。 */
    private static void lure(GameTestHelper h, TestTimeline tl, Player p, int x, int z,
                             java.util.function.Consumer<String> then) {
        // 放置型道具的施放距离是原版交互距离（4.5 格）：先走到石头南边 2 格
        BlockPos stand = h.absolutePos(new BlockPos(x, 2, z - 2));
        p.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
        lookAtFloor(h, p, x, z);
        String castId = cast(p);
        tl.when(() -> committed(p) && !zonesOf(h, castId).isEmpty(), () -> then.accept(castId));
    }

    /** (x, 2, z) 是诱兽石下半、(x, 3, z) 是上半。 */
    private static boolean stoneAt(GameTestHelper h, int x, int z) {
        BlockState lower = h.getBlockState(new BlockPos(x, 2, z));
        BlockState upper = h.getBlockState(new BlockPos(x, 3, z));
        return lower.is(GreekPowers.LURE_STONE.get()) && lower.getValue(LureStoneBlock.HALF) == DoubleBlockHalf.LOWER
                && upper.is(GreekPowers.LURE_STONE.get()) && upper.getValue(LureStoneBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    // ───────────── 诱兽石 ─────────────

    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void lure_needs_two_blocks_of_space(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        h.setBlock(new BlockPos(8, 3, 10), Blocks.STONE);
        PowerGameTests.Vulnerable p = caster(h, "lurelow", GreekPowers.LURE_ITEM.get(), 1);
        BlockPos stand = h.absolutePos(new BlockPos(8, 2, 8));
        p.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
        lookAtFloor(h, p, 8, 10);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        PowerReason r = PowerService.use(p, InteractionHand.MAIN_HAND);
        h.assertTrue(r == PowerReason.PLACEMENT_INVALID, "头顶只有 1 格：确认时就失败：" + r);
        tl.later(40, () -> {
            h.assertTrue(!committed(p) && h.getBlockState(new BlockPos(8, 2, 10)).isAir(), "不提交、不放石头");
            h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "道具不消耗");
            h.setBlock(new BlockPos(8, 3, 10), Blocks.AIR);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void lure_pulls_at_most_24(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        PowerGameTests.Vulnerable p = caster(h, "lure24", GreekPowers.LURE_ITEM.get(), 1);
        List<Cow> cows = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            cows.add(animal(h, EntityType.COW, 1 + (i % 6) * 3, 20 + (i / 6) * 3));
        }
        lure(h, tl, p, 8, 10, castId -> {
            h.assertTrue(stoneAt(h, 8, 10), "立起两格高的诱兽石");
            LurePower.pullForTest(h.getLevel(), castId);
            h.assertTrue(LurePower.pulledForTest(castId) == 24, "最多 24 只：" + LurePower.pulledForTest(castId));
            Vec3 stone = h.absoluteVec(new Vec3(8.5, 2, 10.5));
            // 只看本结构里被这块石头吸引的牛（相邻测试结构的动物也可能排进 24 只）
            List<Cow> mine = cows.stream().filter(c -> LurePower.luresOnForTest(c).contains(castId)).toList();
            h.assertTrue(mine.size() >= 10, "本结构的牛大多被吸引：" + mine.size());
            long nearBefore = mine.stream().filter(c -> c.position().distanceTo(stone) <= 6).count();
            tl.later(120, () -> {
                // 二十几头牛挤在石头周围会互相推开，所以看「走到石头 6 格内的头数」而不是平均距离
                long nearAfter = mine.stream().filter(c -> c.position().distanceTo(stone) <= 6).count();
                h.assertTrue(nearBefore == 0 && nearAfter >= 4, "被吸引的牛走到石头旁：" + nearBefore + " → " + nearAfter);
                LurePower.pullForTest(h.getLevel(), castId);
                h.assertTrue(LurePower.pulledForTest(castId) == 24, "满额后不再挑新的");
                clearAnimals(h);
            h.succeed();
            });
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void lure_skips_leashed_and_tamed(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        PowerGameTests.Vulnerable p = caster(h, "lurefree", GreekPowers.LURE_ITEM.get(), 1);
        h.setBlock(new BlockPos(3, 2, 24), Blocks.OAK_FENCE);
        Horse leashed = animal(h, EntityType.HORSE, 4, 24);
        leashed.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(h.getLevel(), h.absolutePos(new BlockPos(3, 2, 24))), true);
        Wolf wolf = animal(h, EntityType.WOLF, 12, 24);
        wolf.setTame(true, false);
        wolf.setOwnerUUID(UUID.randomUUID());
        Horse wild = animal(h, EntityType.HORSE, 8, 28);
        lure(h, tl, p, 8, 10, castId -> {
            LurePower.pullForTest(h.getLevel(), castId);
            int n = LurePower.pulledWithinForTest(h.getLevel(), castId, h.getBounds());
            h.assertTrue(n == 1 && LurePower.luresOnForTest(wild).contains(castId), "只吸引野马：" + n);
            h.assertTrue(!LurePower.luresOnForTest(leashed).contains(castId) && !LurePower.luresOnForTest(wolf).contains(castId),
                    "拴绳的马、他人驯服的狼不吸引");
            // 野马从 17.5 格外起步、走路有快有慢：给 100 刻（原来 60 刻偶发不够，2026-10-26）
            tl.later(100, () -> {
                h.assertTrue(leashed.isLeashed(), "拴绳的马仍拴着");
                h.assertTrue(wild.position().distanceTo(h.absoluteVec(new Vec3(8.5, 2, 10.5))) < 17.5, "野马朝石头走");
                clearAnimals(h);
            h.succeed();
            });
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void lure_near_water_draws_fish_only(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        for (int x = 9; x <= 14; x++) {
            for (int z = 8; z <= 14; z++) {
                h.setBlock(x, 0, z, Blocks.STONE);
                h.setBlock(x, 1, z, Blocks.WATER);
                h.setBlock(x, 2, z, Blocks.WATER);
            }
        }
        PowerGameTests.Vulnerable p = caster(h, "lurewater", GreekPowers.LURE_ITEM.get(), 1);
        animal(h, EntityType.COD, 12, 11);
        animal(h, EntityType.SQUID, 12, 13);
        Cow cow = animal(h, EntityType.COW, 4, 20);
        lure(h, tl, p, 8, 10, castId -> {
            LurePower.pullForTest(h.getLevel(), castId);
            int n = LurePower.pulledWithinForTest(h.getLevel(), castId, h.getBounds());
            h.assertTrue(n == 2 && !LurePower.luresOnForTest(cow).contains(castId), "鱼与鱿鱼被吸引，岸上的牛不受影响：" + n);
            clearAnimals(h);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void lure_chickens_capped_and_untagged(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        PowerGameTests.Vulnerable p = caster(h, "lurechick", GreekPowers.LURE_ITEM.get(), 1);
        lure(h, tl, p, 8, 10, castId -> {
            LurePower.advanceChickensForTest(h.getLevel(), castId, 3600);
            LurePower.advanceChickensForTest(h.getLevel(), castId, 9999);
            AABB box = new AABB(h.absolutePos(new BlockPos(8, 2, 10))).inflate(3);
            List<Chicken> chickens = h.getLevel().getEntitiesOfClass(Chicken.class, box);
            h.assertTrue(chickens.size() == 6, "3 分钟内 6 只，不超过：" + chickens.size());
            h.assertTrue(chickens.stream().noneMatch(c -> c.getPersistentData().contains("mythic_age:power")), "鸡不带标记");
            chickens.forEach(c -> c.discard());
            clearAnimals(h);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void lure_second_placement_replaces(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        PowerGameTests.Vulnerable p = caster(h, "lure2", GreekPowers.LURE_ITEM.get(), 2);
        Cow cow = animal(h, EntityType.COW, 3, 25);
        lure(h, tl, p, 6, 10, first -> {
            LurePower.pullForTest(h.getLevel(), first);
            h.assertTrue(LurePower.luresOnForTest(cow).contains(first), "第一块吸引牛");
            resetCooldowns(p);
            lure(h, tl, p, 11, 12, second -> {
                h.assertTrue(h.getBlockState(new BlockPos(6, 2, 10)).isAir() && h.getBlockState(new BlockPos(6, 3, 10)).isAir(),
                        "旧石两格立即复原");
                h.assertTrue(stoneAt(h, 11, 12), "新石生效");
                h.assertTrue(zonesOf(h, first).isEmpty() && LurePower.pulledForTest(first) == 0, "旧区域与吸引撤销");
                h.assertTrue(!zonesOf(h, second).isEmpty(), "新区域");
                clearAnimals(h);
            h.succeed();
            });
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void lure_expiry_restores_and_releases(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        PowerGameTests.Vulnerable p = caster(h, "lureend", GreekPowers.LURE_ITEM.get(), 1);
        Cow cow = animal(h, EntityType.COW, 3, 25);
        lure(h, tl, p, 8, 10, castId -> {
            LurePower.pullForTest(h.getLevel(), castId);
            h.assertTrue(LurePower.luresOnForTest(cow).contains(castId), "牛被加上吸引目标");
            PowerZones.Zone z = zonesOf(h, castId).getFirst();
            long now = WorldClock.now(h.getLevel().getServer());
            PowerZones.update(h.getLevel().getServer(), new PowerZones.Zone(z.castId(), z.kind(), z.powerId(), z.dim(),
                    z.center(), z.radius(), now, z.owner(), z.marks()));
            PowerZones.tickForTest(h.getLevel().getServer());
            h.assertTrue(h.getBlockState(new BlockPos(8, 2, 10)).isAir() && h.getBlockState(new BlockPos(8, 3, 10)).isAir(),
                    "到期：两格复原");
            h.assertTrue(!LurePower.luresOnForTest(cow).contains(castId) && LurePower.pulledForTest(castId) == 0,
                    "吸引目标从动物身上移除");
            clearAnimals(h);
            h.succeed();
        });
    }

    // ───────────── 哨兵石像 ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void sentinel_lifetime_by_own_shrine(GameTestHelper h) {
        PowerGameTests.Vulnerable p = caster(h, "sentlife", GreekPowers.SENTINEL_ITEM.get(), 1);
        ShrineRegistry reg = ShrineRegistry.get(h.getLevel().getServer());
        Vec3 at = h.absoluteVec(new Vec3(8.5, 2, 20.5));
        BlockPos base = BlockPos.containing(at);
        GlobalPos own31 = GlobalPos.of(h.getLevel().dimension(), base.offset(31, 0, 0));
        GlobalPos own33 = GlobalPos.of(h.getLevel().dimension(), base.offset(-33, 0, 0));
        GlobalPos other10 = GlobalPos.of(h.getLevel().dimension(), base.offset(0, 0, 10));
        try {
            reg.add(new ShrineRegistry.Entry(own33, p.getUUID(), "sentlife", Direction.SOUTH, Civilization.GREEK, "greek:altar"));
            reg.add(new ShrineRegistry.Entry(other10, UUID.randomUUID(), "other", Direction.SOUTH, Civilization.GREEK, "greek:altar"));
            h.assertTrue(SentinelPower.lifetimeFor(p, h.getLevel(), at) == 24000, "自己的神坛 33 格、别人的 10 格 → 1 日");
            reg.add(new ShrineRegistry.Entry(own31, p.getUUID(), "sentlife", Direction.SOUTH, Civilization.GREEK, "greek:altar"));
            h.assertTrue(SentinelPower.lifetimeFor(p, h.getLevel(), at) == 72000, "自己的神坛 31 格 → 3 日");
        } finally {
            reg.remove(own31);
            reg.remove(own33);
            reg.remove(other10);
        }
        h.succeed();
    }

    private static List<SentinelEntity> sentinels(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(SentinelEntity.class, h.getBounds(), s -> s.isAlive());
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void sentinel_group_replaces_old(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        PowerGameTests.Vulnerable p = caster(h, "sentgrp", GreekPowers.SENTINEL_ITEM.get(), 2);
        lookAtFloor(h, p, 8, 12);
        cast(p);
        tl.later(50, () -> {
            h.assertTrue(committed(p), "第一组已提交");
            List<SentinelEntity> first = sentinels(h);
            h.assertTrue(first.size() == 4, "第一组 4 尊：" + first.size());
            resetCooldowns(p);
            lookAtFloor(h, p, 8, 18);
            cast(p);
            tl.later(50, () -> {
                h.assertTrue(first.stream().allMatch(SentinelEntity::retiring) || first.stream().noneMatch(SentinelEntity::isAlive),
                        "旧组进入碎化");
                tl.later(60, () -> {
                    h.assertTrue(first.stream().noneMatch(SentinelEntity::isAlive), "旧组 4 尊移除");
                    h.assertTrue(TransientEntities.productsForTest(p.getUUID(), GreekPowers.SENTINEL).size() == 4, "新组 4 尊");
                    h.assertTrue(sentinels(h).size() == 4, "世界里剩 4 尊");
                    clearSentinels(h);
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void sentinel_needs_four_spots(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        for (int y = 0; y <= 7; y++) {
            h.setBlock(10, y, 14, Blocks.STONE);
        }
        PowerGameTests.Vulnerable p = caster(h, "sent3", GreekPowers.SENTINEL_ITEM.get(), 1);
        lookAtFloor(h, p, 8, 12);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        PowerReason r = PowerService.use(p, InteractionHand.MAIN_HAND);
        h.assertTrue(r == PowerReason.PLACEMENT_INVALID, "确认时就失败：" + r);
        tl.later(60, () -> {
            h.assertTrue(!committed(p), "只有 3 个站位：不提交");
            h.assertTrue(p.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "道具不消耗");
            h.assertTrue(sentinels(h).isEmpty(), "没有石像");
            clearSentinels(h);
            h.succeed();
        });
    }

    /** 直接在结构里立一尊哨兵（主人是不在线的随机 UUID）。 */
    private static SentinelEntity sentinel(GameTestHelper h, int x, int z, UUID owner, long life) {
        SentinelEntity s = GreekPowers.SENTINEL_ENTITY.get().create(h.getLevel());
        BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
        s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        s.setup(owner, WorldClock.now(h.getLevel().getServer()) + life);
        h.getLevel().addFreshEntity(s);
        return s;
    }

    private static int arrowsNear(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(Arrow.class, h.getBounds()).size();
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void sentinel_holds_fire_when_friend_in_line(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        sentinel(h, 8, 10, UUID.randomUUID(), SENTINEL_TEST_LIFE);
        Mob villager = TestMobs.tag(h.spawn(EntityType.VILLAGER, new BlockPos(8, 2, 14)));
        villager.setNoAi(true);
        Mob spider = TestMobs.tag(h.spawn(EntityType.SPIDER, new BlockPos(8, 2, 19)));
        spider.setNoAi(true);
        tl.later(80, () -> {
            h.assertTrue(spider.getHealth() == spider.getMaxHealth() && arrowsNear(h) == 0, "友方挡在射线上：不射");
            villager.discard();
            tl.later(80, () -> {
                h.assertTrue(spider.getHealth() <= spider.getMaxHealth() - 4 + 0.01, "让开后射击，每箭 4 点：" + spider.getHealth());
                clearSentinels(h);
            h.succeed();
            });
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void sentinel_never_shoots_friends(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        sentinel(h, 8, 10, UUID.randomUUID(), SENTINEL_TEST_LIFE);
        Mob villager = TestMobs.tag(h.spawn(EntityType.VILLAGER, new BlockPos(5, 2, 16)));
        villager.setNoAi(true);
        Wolf wolf = TestMobs.tag(h.spawn(EntityType.WOLF, new BlockPos(11, 2, 16)));
        wolf.setNoAi(true);
        wolf.setTame(true, false);
        wolf.setOwnerUUID(UUID.randomUUID());
        tl.later(100, () -> {
            h.assertTrue(villager.getHealth() == villager.getMaxHealth() && wolf.getHealth() == wolf.getMaxHealth()
                    && arrowsNear(h) == 0, "村民、驯服的狼不射");
            clearSentinels(h);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void sentinel_expires_and_crumbles(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        SentinelEntity s = sentinel(h, 8, 10, UUID.randomUUID(), 5);
        tl.later(15, () -> h.assertTrue(s.retiring(), "到期进入碎化"));
        tl.later(60, () -> {
            h.assertTrue(!s.isAlive(), "碎化后移除");
            SentinelEntity old = GreekPowers.SENTINEL_ENTITY.get().create(h.getLevel());
            old.setup(UUID.randomUUID(), WorldClock.now(h.getLevel().getServer()) - 1);
            h.assertTrue(SentinelPower.rejectOnLoad(old, WorldClock.now(h.getLevel().getServer())), "载入时已过期：不加入");
            SentinelEntity fresh = GreekPowers.SENTINEL_ENTITY.get().create(h.getLevel());
            fresh.setup(UUID.randomUUID(), WorldClock.now(h.getLevel().getServer()) + 100);
            h.assertTrue(!SentinelPower.rejectOnLoad(fresh, WorldClock.now(h.getLevel().getServer())), "没过期：照常加入");
            clearSentinels(h);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void restoration_heals_sentinel(GameTestHelper h) {
        TestTimeline tl = new TestTimeline(h);
        pen(h);
        PowerGameTests.Vulnerable p = caster(h, "sentheal", GreekPowers.RESTORATION_ITEM.get(), 1);
        SentinelEntity s = sentinel(h, 8, 8, p.getUUID(), SENTINEL_TEST_LIFE);
        s.setHealth(10);
        cast(p);
        tl.later(150, () -> {
            h.assertTrue(committed(p), "复原已提交");
            h.assertTrue(s.getHealth() >= s.getMaxHealth() - 0.01, "回满：" + s.getHealth());
            clearSentinels(h);
            h.succeed();
        });
    }
}
