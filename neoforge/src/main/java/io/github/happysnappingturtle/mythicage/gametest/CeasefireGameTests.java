package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.power.CeasefirePower;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.power.PowerSpec;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeRules;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.PowerService;
import io.github.happysnappingturtle.mythicage.power.PowerSpecLoader;
import io.github.happysnappingturtle.mythicage.power.PowerZones;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.UUID;

/**
 * T25a 停火（TC-T25a-50～58）。停火半径 64 会盖到同批相邻的场地，所以单独一批，并在本批把半径改成
 * {@value #TEST_RADIUS}、持续改成 {@value #TEST_TICKS} 刻（正式 64 / 600）。施法者站在 (8, 2, 6)。
 * 除 TC-50 走正式的施放流程外，其余直接执行神力（好在同一场 Boss 战里连放两次，不受冷却限制）。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CeasefireGameTests {
    private static final String BATCH = "ceasefire";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    static final int TEST_RADIUS = 6;
    static final int TEST_TICKS = 240;

    private CeasefireGameTests() {
    }

    @BeforeBatch(batch = BATCH)
    public static void before(ServerLevel level) {
        PowerSpec s = PowerSpecLoader.get(GreekPowers.CEASEFIRE).orElseThrow();
        PowerSpecLoader.overrideForTest(new PowerSpec(s.id(), s.card(), s.civ(), s.form(), s.castTicks(), s.cooldownTicks(), s.range(),
                s.profile(), s.selfCentered(), s.large(), s.matrix(), TEST_TICKS, TEST_RADIUS));
        TargetFactsCollector.bossForTest(EntityType.HUSK);
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        endAll(level);
        PowerSpecLoader.overrideForTest(null);
        TargetFactsCollector.bossForTest(null);
        PowerService.clearForTest();
    }

    // ───────────── 搭建 ─────────────

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    private static PowerGameTests.Vulnerable player(GameTestHelper h, String name, int x, int z) {
        PowerGameTests.Vulnerable p = new PowerGameTests.Vulnerable(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        p.getInventory().clearContent();
        // 新建的服务端玩家有 60 刻出生保护，假玩家不走 tick，直接清零（同 GreekPowerGameTests.bolt_pvp）
        try {
            var f = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(p, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }

    private static <M extends Mob> M mob(GameTestHelper h, EntityType<M> type, int x, int z) {
        M m = TestMobs.tag(h.spawn(type, new BlockPos(x, 2, z)));
        m.setNoAi(true);
        return m;
    }

    /** 直接执行停火（不走吟唱与冷却）；返回 castId。 */
    private static String cast(GameTestHelper h, PowerGameTests.Vulnerable p) {
        PowerSpec spec = PowerSpecLoader.get(GreekPowers.CEASEFIRE).orElseThrow();
        String castId = "ceasefire_test_" + UUID.randomUUID();
        new CeasefirePower().execute(new CastContext(p, h.getLevel(), spec, castId, null, null, "", p.position(), null, null, 0,
                h.getLevel().getGameTime()));
        return castId;
    }

    private static boolean active(GameTestHelper h, LivingEntity e) {
        return CeasefirePower.activeAt(h.getLevel(), e.position());
    }

    /** 打一下，返回扣了多少血。 */
    private static float hit(LivingEntity victim, net.minecraft.world.damagesource.DamageSource s, float amount) {
        victim.invulnerableTime = 0;
        float before = victim.getHealth();
        victim.hurt(s, amount);
        return before - victim.getHealth();
    }

    /** 只清本场地附近的休战区（同批的用例并行跑，不能清别人的）。 */
    private static void endHere(GameTestHelper h) {
        Vec3 c = h.absoluteVec(new Vec3(8.5, 2, 6.5));
        for (PowerZones.Zone z : PowerZones.all(h.getLevel().getServer())) {
            if (z.powerId().equals(GreekPowers.CEASEFIRE) && z.center().distanceTo(c) < 16) {
                PowerZones.remove(h.getLevel().getServer(), z.castId());
            }
        }
    }

    private static void endAll(ServerLevel level) {
        for (PowerZones.Zone z : PowerZones.all(level.getServer())) {
            if (z.powerId().equals(GreekPowers.CEASEFIRE)) {
                PowerZones.remove(level.getServer(), z.castId());
            }
        }
    }

    private static boolean fast(LivingEntity p) {
        var a = p.getAttribute(Attributes.MOVEMENT_SPEED);
        return a != null && a.getModifiers().stream().anyMatch(m -> m.id().getPath().equals("ceasefire_speed"));
    }

    // ───────────── 用例 ─────────────

    /** TC-T25a-50：正式施放后，僵尸打玩家无伤、锁不住目标；区外的僵尸打区内的玩家也拦（D18）；施法者移速 +30%。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 120)
    public static void blocks_and_disengages(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_cast", 8, 6);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GreekPowers.CEASEFIRE_ITEM.get()));
        Zombie z = mob(h, EntityType.ZOMBIE, 8, 9);
        Zombie far = mob(h, EntityType.ZOMBIE, 8, 20);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        PowerService.use(p, InteractionHand.MAIN_HAND);
        h.runAfterDelay(50, () -> {
            try {
                h.assertTrue(active(h, p), "施放后休战区生效");
                h.assertTrue(fast(p), "施法者移速 +30%");
                z.setTarget(p);
                h.assertTrue(z.getTarget() == null, "僵尸锁不住目标");
                h.assertTrue(hit(p, h.getLevel().damageSources().mobAttack(z), 4) == 0, "区内僵尸打玩家无伤");
                h.assertTrue(hit(p, h.getLevel().damageSources().mobAttack(far), 4) == 0, "区外僵尸打区内玩家也拦");
                h.assertTrue(hit(far, h.getLevel().damageSources().mobAttack(z), 4) == 0, "区内打区外也拦");
                h.succeed();
            } finally {
                endHere(h);
            }
        });
    }

    /** TC-T25a-51：队友挥砍僵尸 → 休战对所有人结束，这一击照常；结束方式是破约（碎裂）；施法者移速撤掉。 */
    @GameTest(template = T, batch = BATCH)
    public static void attack_breaks_truce(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_owner", 8, 6);
        PowerGameTests.Vulnerable ally = player(h, "cf_ally", 9, 7);
        Zombie z = mob(h, EntityType.ZOMBIE, 8, 9);
        try {
            String castId = cast(h, p);
            h.assertTrue(hit(z, h.getLevel().damageSources().playerAttack(ally), 4) == 0, "破约前：伤不到");
            h.assertTrue(CommonHooks.onPlayerAttackTarget(ally, z), "挥砍照常发生");
            h.assertFalse(active(h, p), "休战结束");
            h.assertTrue(hit(z, h.getLevel().damageSources().playerAttack(ally), 4) > 0, "这一击照常结算");
            h.assertTrue(CeasefirePower.endedForTest(castId).orElse("").equals("break"), "破约结束");
            h.assertFalse(fast(p), "施法者移速撤掉");
            h.succeed();
        } finally {
            endHere(h);
        }
    }

    /** 区内射出投射物算破约。 */
    @GameTest(template = T, batch = BATCH)
    public static void projectile_breaks_truce(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_archer", 8, 6);
        try {
            cast(h, p);
            Arrow arrow = new Arrow(h.getLevel(), p, new ItemStack(Items.ARROW), null);
            arrow.setPos(p.getX(), p.getEyeY(), p.getZ());
            h.getLevel().addFreshEntity(arrow);
            h.assertFalse(active(h, p), "射出箭即破约");
            arrow.discard();
            h.succeed();
        } finally {
            endHere(h);
        }
    }

    /** TC-T25a-52：环境伤害照常。 */
    @GameTest(template = T, batch = BATCH)
    public static void environment_passes(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_lava", 8, 6);
        try {
            cast(h, p);
            h.assertTrue(hit(p, h.getLevel().damageSources().lava(), 4) > 0, "岩浆照常");
            h.assertTrue(hit(p, h.getLevel().damageSources().fall(), 2) > 0, "摔落照常");
            h.assertTrue(active(h, p), "环境伤害不算破约");
            h.succeed();
        } finally {
            endHere(h);
        }
    }

    /** TC-T25a-53：Boss（本批把尸壳当 Boss）第一次：停手 8 秒，双方互不伤害；8 秒后放出，Boss 再打不算破约。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 260)
    public static void boss_held_then_released(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_boss", 8, 6);
        Husk boss = mob(h, EntityType.HUSK, 8, 9);
        cast(h, p);
        h.assertTrue(CeasefirePower.heldForTest(boss, h.getLevel()), "Boss 停手");
        h.assertTrue(hit(p, h.getLevel().damageSources().mobAttack(boss), 4) == 0, "Boss 伤不到人");
        h.assertTrue(hit(boss, h.getLevel().damageSources().playerAttack(p), 4) == 0, "人伤不到 Boss");
        h.runAfterDelay(170, () -> {
            try {
                h.assertFalse(CeasefirePower.heldForTest(boss, h.getLevel()), "8 秒后放出");
                h.assertTrue(hit(p, h.getLevel().damageSources().mobAttack(boss), 4) > 0, "Boss 照常打人");
                h.assertTrue(active(h, p), "Boss 再攻击不算破约");
                h.assertTrue(hit(boss, h.getLevel().damageSources().playerAttack(p), 4) > 0, "参与者也照常打 Boss");
                h.succeed();
            } finally {
                endHere(h);
            }
        });
    }

    /** TC-T25a-54：同一场第二次（第一次的 8 秒停手已过）：Boss 不受影响；场边的小怪仍休战。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 260)
    public static void boss_second_cast(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_boss2", 8, 6);
        Husk boss = mob(h, EntityType.HUSK, 8, 9);
        Zombie minion = mob(h, EntityType.ZOMBIE, 6, 8);
        cast(h, p);
        h.assertTrue(CeasefirePower.heldForTest(boss, h.getLevel()), "第一次停手");
        h.runAfterDelay(170, () -> {
            try {
                endHere(h);
                cast(h, p);
                h.assertFalse(CeasefirePower.heldForTest(boss, h.getLevel()), "第二次 Boss 不停手");
                h.assertTrue(hit(boss, h.getLevel().damageSources().playerAttack(p), 4) > 0, "Boss 照常挨打");
                h.assertTrue(hit(minion, h.getLevel().damageSources().playerAttack(p), 4) == 0, "场边小怪仍受保护");
                h.succeed();
            } finally {
                endHere(h);
            }
        });
    }

    /** TC-T25a-55（D4）：区内刷怪笼、自然生成判失败；刷怪蛋放行；动物不拦。 */
    @GameTest(template = T, batch = BATCH)
    public static void spawns_blocked(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_spawn", 8, 6);
        try {
            cast(h, p);
            Vec3 at = h.absoluteVec(new Vec3(8.5, 2, 8.5));
            Zombie z = EntityType.ZOMBIE.create(h.getLevel());
            z.moveTo(at);
            for (MobSpawnType type : new MobSpawnType[]{MobSpawnType.SPAWNER, MobSpawnType.NATURAL, MobSpawnType.TRIAL_SPAWNER}) {
                var e = new MobSpawnEvent.PositionCheck(z, h.getLevel(), type, null);
                CeasefirePower.onSpawnCheck(e);
                h.assertTrue(e.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL, type + " 被拦");
            }
            var egg = new MobSpawnEvent.PositionCheck(z, h.getLevel(), MobSpawnType.SPAWN_EGG, null);
            CeasefirePower.onSpawnCheck(egg);
            h.assertTrue(egg.getResult() != MobSpawnEvent.PositionCheck.Result.FAIL, "刷怪蛋放行");
            var pig = EntityType.PIG.create(h.getLevel());
            pig.moveTo(at);
            var natural = new MobSpawnEvent.PositionCheck(pig, h.getLevel(), MobSpawnType.NATURAL, null);
            CeasefirePower.onSpawnCheck(natural);
            h.assertTrue(natural.getResult() != MobSpawnEvent.PositionCheck.Result.FAIL, "动物不拦");
            h.assertTrue(io.github.happysnappingturtle.mythicage.power.SpawnerStall.stalled(h.getLevel(), BlockPos.containing(at)),
                    "区内刷怪器停摆");
            z.discard();
            pig.discard();
            h.succeed();
        } finally {
            endHere(h);
        }
    }

    /** TC-T25a-56：存档往返后休战区还在，剩余时间不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void zone_persists(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_save", 8, 6);
        try {
            String castId = cast(h, p);
            long until = PowerZones.all(h.getLevel().getServer()).stream().filter(z -> z.castId().equals(castId)).findFirst()
                    .orElseThrow().until();
            PowerZones.roundTripForTest(h.getLevel().getServer());
            var back = PowerZones.all(h.getLevel().getServer()).stream().filter(z -> z.castId().equals(castId)).findFirst();
            h.assertTrue(back.isPresent() && back.get().until() == until, "往返后还在、到期刻不变");
            h.assertTrue(active(h, p), "照常生效");
            h.succeed();
        } finally {
            endHere(h);
        }
    }

    /** TC-T25a-57：到期区域消失、淡出（不是碎裂），施法者移速撤掉。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = TEST_TICKS + 80)
    public static void expires_and_fades(GameTestHelper h) {
        floor(h);
        PowerGameTests.Vulnerable p = player(h, "cf_expire", 8, 6);
        String castId = cast(h, p);
        h.runAfterDelay(TEST_TICKS + 40, () -> {
            try {
                h.assertFalse(active(h, p), "到期消失");
                h.assertTrue(CeasefirePower.endedForTest(castId).orElse("").equals("fade"), "淡出");
                h.assertFalse(fast(p), "施法者移速撤掉");
                h.succeed();
            } finally {
                endHere(h);
            }
        });
    }

    /** TC-T25a-58：赫尔墨斯祠所在村、古典时代、38 恩宠可买；没有赫尔墨斯祠看不到。 */
    @GameTest(template = T, batch = BATCH)
    public static void offer(GameTestHelper h) {
        TradeOffer o = TradeTableLoader.table(Civilization.GREEK).offers().stream().filter(x -> x.id().equals("gp_gr_ceasefire"))
                .findFirst().orElseThrow();
        h.assertTrue(o.price().favor() == 38, "38 恩宠：" + o.price());
        h.assertTrue(TradeRules.visible(o, TradeRules.VenueContext.guardian(Age.CLASSICAL, "zeus", Set.of("hermes"))), "赫尔墨斯祠可见");
        h.assertFalse(TradeRules.visible(o, TradeRules.VenueContext.guardian(Age.CLASSICAL, "zeus", Set.of("athena"))), "雅典娜祠不卖");
        h.succeed();
    }
}
