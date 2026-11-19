package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.summon.HurlRules;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.summon.SummonRegistry;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.summon.SummonTargets;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons;
import io.github.happysnappingturtle.mythicage.summon.greek.SummonCyclopsEntity;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import io.github.happysnappingturtle.mythicage.wild.greek.GreekWild;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T25a 召唤独眼巨人（TC-T25a-60～65）。场地 17 × 36，地面在 y = 1；独眼巨人放在 z = 6、面朝 +Z，靶子都在 +Z 方向。
 * 靶子是不会动的僵尸（防晒、NoAI），抓起与飞行都由独眼巨人逐刻摆放，NoAI 不影响。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CyclopsSummonGameTests {
    private static final String BATCH = "summon_cyclops";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private CyclopsSummonGameTests() {
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static FakePlayer owner(GameTestHelper h, String name, Age age, BlockPos at) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        MythData.get(p).civ(Civilization.GREEK).setAge(age);
        Vec3 v = Vec3.atBottomCenterOf(h.absolutePos(at));
        p.moveTo(v.x, v.y, v.z, 0, 0);
        SummonTargets.ownerForTest(p, true);
        return p;
    }

    private static SummonCyclopsEntity summon(GameTestHelper h, FakePlayer p, BlockPos at) {
        SummonService.Result r = SummonService.summon(p, SummonUnits.card("greek:cyclops").orElseThrow(),
                Vec3.atBottomCenterOf(h.absolutePos(at)));
        h.assertTrue(r == SummonService.Result.OK, "召唤独眼巨人应成功，实际 " + r);
        SummonCyclopsEntity c = units(h, p).get(0);
        c.setYRot(0);
        c.yBodyRot = 0;
        c.yHeadRot = 0;
        return c;
    }

    private static List<SummonCyclopsEntity> units(GameTestHelper h, FakePlayer p) {
        return h.getLevel().getEntitiesOfClass(SummonCyclopsEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(80),
                e -> e.isAlive() && p.getUUID().equals(e.link().owner));
    }

    private static void cleanup(GameTestHelper h, FakePlayer... players) {
        for (FakePlayer p : players) {
            SummonRegistry reg = SummonRegistry.get(h.getLevel().getServer());
            for (SummonRegistry.Unit u : reg.byOwner(p.getUUID())) {
                SummonService.dismiss(h.getLevel().getServer(), u.unitId);
            }
            SummonTargets.ownerForTest(p, false);
        }
    }

    private static Zombie dummy(GameTestHelper h, BlockPos at) {
        Zombie z = h.spawn(EntityType.ZOMBIE, at);
        z.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60));
        z.setPersistenceRequired();
        z.setNoAi(true);
        // 僵尸自带 2 点护甲，会把 6 减成 5.9；清零后按设计伤害核对
        z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        return z;
    }

    /** TC-T25a-60：占 1 位、生命 110；远古上限 2，第三个拒绝且召唤符不消耗。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void cyclops_slot_and_cap(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_cyc_cap", Age.ARCHAIC, new BlockPos(8, 2, 2));
        try {
            SummonCyclopsEntity c = summon(h, p, new BlockPos(5, 2, 8));
            h.assertTrue(SummonService.occupied(p) == 1, "占 1 位，实际 " + SummonService.occupied(p));
            h.assertTrue(Math.abs(c.getMaxHealth() - 110) < 0.01 && Math.abs(c.getHealth() - 110) < 0.01,
                    "生命 110，实际 " + c.getHealth() + "/" + c.getMaxHealth());
            h.assertTrue(Math.abs(c.getBbWidth() - 1.8) < 0.01 && Math.abs(c.getBbHeight() - 3.8) < 0.01, "碰撞 1.8 × 3.8");
            h.assertTrue(SummonService.summon(p, SummonUnits.card("greek:cyclops").orElseThrow(),
                    Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(11, 2, 8)))) == SummonService.Result.OK, "第 2 个");
            ItemStack stack = new ItemStack(GreekSummons.CYCLOPS_TOKEN.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var r = GreekSummons.CYCLOPS_TOKEN.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(!r.getResult().consumesAction() && stack.getCount() == 1, "已满：不吟唱、不消耗");
            GreekSummons.CYCLOPS_TOKEN.get().finishUsingItem(stack, h.getLevel(), p);
            h.assertTrue(stack.getCount() == 1 && units(h, p).size() == 2, "已满时吟唱完也不召唤");
        } finally {
            cleanup(h, p);
        }
        h.succeed();
    }

    /** TC-T25a-61、64：传令杖点名（潜行右键走的同一入口）→ 1 秒前摇后抓起 → 扔出约 8 格；被扔者受 12，落点 2 格内另一只受 6。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void cyclops_hurls_zombie(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_cyc_hurl", Age.CLASSICAL, new BlockPos(8, 2, 1));
        SummonCyclopsEntity c = summon(h, p, new BlockPos(8, 2, 6));
        Zombie z = dummy(h, new BlockPos(8, 2, 9));
        Zombie near = dummy(h, new BlockPos(9, 2, 14));
        float hp = z.getHealth();
        float hpNear = near.getHealth();
        Vec3 start = c.position();
        h.assertTrue(SummonService.commandSpecial(p, z) == 1, "1 个单位放出抓投");
        h.assertTrue(c.hurling(), "进入抓投");
        h.runAfterDelay(HurlRules.WINDUP_TICKS - 4, () -> h.assertTrue(!z.isPassenger(), "前摇未完不应抓起"));
        h.runAfterDelay(HurlRules.WINDUP_TICKS + 3, () -> h.assertTrue(z.getVehicle() == c, "1 秒前摇后应被抓起"));
        h.runAfterDelay(80, () -> {
            try {
                h.assertTrue(!z.isPassenger() && c.flying() == null, "应已扔出并落地");
                double d = Math.hypot(z.getX() - start.x, z.getZ() - start.z);
                h.assertTrue(d > 6.5 && d < 9.0, "应扔出约 8 格，实际 " + d);
                h.assertTrue(hp - z.getHealth() >= HurlRules.HIT_DAMAGE - 0.01, "被扔者受 12，实际 " + (hp - z.getHealth()));
                h.assertTrue(hpNear - near.getHealth() >= HurlRules.SPLASH_DAMAGE - 0.01,
                        "落点旁的受 6，实际 " + (hpNear - near.getHealth()));
                h.assertTrue(!c.requestSpecial(z), "冷却中不能再放");
                long left = c.hurlReadyAt() - h.getLevel().getGameTime();
                h.assertTrue(left > HurlRules.COOLDOWN - 100, "冷却按 20 秒算，剩 " + left);
            } finally {
                cleanup(h, p);
            }
            h.succeed();
        });
    }

    /** TC-T25a-62：抛线前方 5 格处有一堵墙——落在墙前的空位，不卡进方块。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void cyclops_hurl_stops_before_wall(GameTestHelper h) {
        floor(h);
        for (int x = 0; x < 17; x++) {
            for (int y = 2; y <= 6; y++) {
                h.setBlock(new BlockPos(x, y, 12), Blocks.STONE);
            }
        }
        FakePlayer p = owner(h, "ma_cyc_wall", Age.CLASSICAL, new BlockPos(8, 2, 1));
        SummonCyclopsEntity c = summon(h, p, new BlockPos(8, 2, 6));
        Zombie z = dummy(h, new BlockPos(8, 2, 9));
        double wallZ = h.absolutePos(new BlockPos(8, 2, 12)).getZ();
        h.assertTrue(SummonService.commandSpecial(p, z) == 1, "放出抓投");
        h.runAfterDelay(80, () -> {
            try {
                h.assertTrue(!z.isPassenger() && c.flying() == null, "应已落地");
                h.assertTrue(z.getBoundingBox().maxZ <= wallZ + 0.01, "应落在墙前，实际 z " + z.getZ() + "，墙 " + wallZ);
                h.assertTrue(h.getLevel().noCollision(z, z.getBoundingBox().deflate(0.01)), "不应卡进方块");
            } finally {
                cleanup(h, p);
            }
            h.succeed();
        });
    }

    /** TC-T25a-63：对神话生物（哈耳庇厄）与 Boss 抓空——不抓起，冷却只算 5 秒。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void cyclops_misses_mythic_and_boss(GameTestHelper h) {
        floor(h);
        TargetFactsCollector.bossForTest(EntityType.HUSK);
        FakePlayer p = owner(h, "ma_cyc_miss", Age.CLASSICAL, new BlockPos(2, 2, 1));
        FakePlayer q = owner(h, "ma_cyc_miss2", Age.CLASSICAL, new BlockPos(14, 2, 1));
        SummonCyclopsEntity a = summon(h, p, new BlockPos(4, 2, 6));
        SummonCyclopsEntity b = summon(h, q, new BlockPos(13, 2, 6));
        Mob harpy = GreekWild.HARPY.get().create(h.getLevel());
        BlockPos hp = h.absolutePos(new BlockPos(4, 2, 9));
        harpy.moveTo(hp.getX() + 0.5, hp.getY(), hp.getZ() + 0.5);
        harpy.setNoAi(true);
        harpy.setPersistenceRequired();
        h.getLevel().addFreshEntity(harpy);
        Mob boss = h.spawn(EntityType.HUSK, new BlockPos(13, 2, 9));
        boss.setNoAi(true);
        h.assertTrue(TargetFactsCollector.isMythic(harpy), "哈耳庇厄算神话生物");
        h.assertTrue(a.requestSpecial(harpy), "对哈耳庇厄也会出手");
        h.assertTrue(b.requestSpecial(boss), "对 Boss 也会出手");
        h.runAfterDelay(HurlRules.WINDUP_TICKS + 6, () -> {
            try {
                h.assertTrue(!harpy.isPassenger() && !boss.isPassenger(), "不应抓起神话生物与 Boss");
                h.assertTrue(!a.hurling() && !b.hurling(), "抓空后结束");
                long now = h.getLevel().getGameTime();
                long la = a.hurlReadyAt() - now;
                long lb = b.hurlReadyAt() - now;
                h.assertTrue(la > 0 && la <= HurlRules.MISS_COOLDOWN && lb > 0 && lb <= HurlRules.MISS_COOLDOWN,
                        "抓空冷却 5 秒，实际 " + la + " / " + lb);
            } finally {
                TargetFactsCollector.bossForTest(null);
                harpy.discard();
                cleanup(h, p, q);
            }
            h.succeed();
        });
    }

    /**
     * TC-T25a-65：独眼巨人召唤符是阿瑞斯的商品（祭司处、古典起、恩宠 24）；没供奉阿瑞斯、本村没有阿瑞斯祠时买不到。
     */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void cyclops_token_is_ares_offer(GameTestHelper h) {
        floor(h);
        TradeOffer offer = TradeTableLoader.table(Civilization.GREEK).offer("summon_cyclops").orElseThrow();
        h.assertTrue(offer.category() == TradeOffer.Category.SUMMON && offer.minorGod().equals("ares")
                && offer.minAge() == Age.CLASSICAL && offer.price().favor() == 24
                && offer.result().item().equals("mythic_age:summon_token_cyclops"), "商品条件");
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(8, 2, 8)), Optional.empty(),
                Civilization.GREEK);
        h.assertTrue(g != null, "祭司");
        g.setNoAi(true);
        FakePlayer p = owner(h, "ma_cyc_trade", Age.CLASSICAL, new BlockPos(10, 2, 8));
        p.getInventory().clearContent();
        MythData.get(p).civ(Civilization.GREEK).setFavor(40);
        TradeService.openGuardian(p, g);
        var o = TradeService.buy(p, "summon_cyclops", 24, UUID.randomUUID().toString());
        h.assertTrue(o.reason() != TradeReason.OK && p.getInventory().countItem(GreekSummons.CYCLOPS_TOKEN.get()) == 0,
                "没有阿瑞斯祠时买不到，实际 " + o.reason());
        TradeService.close(p);
        g.discard();
        SummonTargets.ownerForTest(p, false);
        h.succeed();
    }
}
