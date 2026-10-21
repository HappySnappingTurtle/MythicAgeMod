package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import io.github.happysnappingturtle.mythicage.core.summon.SummonUnitCard;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import io.github.happysnappingturtle.mythicage.summon.SummonRegistry;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.summon.SummonTargets;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons;
import io.github.happysnappingturtle.mythicage.summon.greek.SummonMinotaurEntity;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
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
 * T14 召唤框架（TC-T14-20～61）。场地 17 × 36，地面在 y = 1；主人是 FakePlayer，用测试钩子登记成可查到的主人。
 * 每个用例结束删掉自己的登记与实体。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SummonGameTests {
    private static final String BATCH = "summon";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private SummonGameTests() {
    }

    // ───────────── 搭建 ─────────────

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

    private static SummonUnitCard minotaurCard() {
        return SummonUnits.card("greek:minotaur").orElseThrow();
    }

    private static SummonMinotaurEntity summon(GameTestHelper h, FakePlayer p, BlockPos at) {
        SummonService.Result r = SummonService.summon(p, minotaurCard(), Vec3.atBottomCenterOf(h.absolutePos(at)));
        h.assertTrue(r == SummonService.Result.OK, "召唤应成功，实际 " + r);
        return units(h, p).get(0);
    }

    private static List<SummonMinotaurEntity> units(GameTestHelper h, FakePlayer p) {
        return h.getLevel().getEntitiesOfClass(SummonMinotaurEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(160),
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

    private static Zombie zombie(GameTestHelper h, BlockPos at) {
        Zombie z = h.spawn(EntityType.ZOMBIE, at);
        z.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60));
        z.setPersistenceRequired();
        return z;
    }

    // ───────────── 3.1 召唤与上限 ─────────────

    /** TC-T14-20、23：用召唤符召出单位，召唤符消耗；谁用谁是主人。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void token_summons_for_user(GameTestHelper h) {
        floor(h);
        FakePlayer a = owner(h, "ma_summon_a", Age.CLASSICAL, new BlockPos(8, 2, 4));
        FakePlayer b = owner(h, "ma_summon_b", Age.CLASSICAL, new BlockPos(8, 2, 20));
        try {
            ItemStack stack = new ItemStack(GreekSummons.MINOTAUR_TOKEN.get(), 2);
            GreekSummons.MINOTAUR_TOKEN.get().finishUsingItem(stack, h.getLevel(), a);
            h.assertTrue(stack.getCount() == 1, "召唤符应消耗 1 张");
            h.assertTrue(units(h, a).size() == 1, "A 应有 1 个单位");
            GreekSummons.MINOTAUR_TOKEN.get().finishUsingItem(stack, h.getLevel(), b);
            h.assertTrue(units(h, b).size() == 1 && units(h, a).size() == 1, "B 用了召唤符，单位归 B");
            SummonRegistry.Unit u = SummonRegistry.get(h.getLevel().getServer()).byOwner(b.getUUID()).get(0);
            h.assertTrue(u.card.equals("greek:minotaur") && u.mode == SummonMode.FOLLOW, "登记内容");
        } finally {
            cleanup(h, a, b);
        }
        h.succeed();
    }

    /** TC-T14-21：远古上限 2，第三个拒绝且召唤符不消耗。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void cap_refuses_and_keeps_token(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_cap", Age.ARCHAIC, new BlockPos(8, 2, 4));
        try {
            summon(h, p, new BlockPos(6, 2, 8));
            h.assertTrue(SummonService.summon(p, minotaurCard(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(10, 2, 8))))
                    == SummonService.Result.OK, "第 2 个");
            h.assertTrue(SummonService.cap(p) == 2 && SummonService.occupied(p) == 2, "上限 2、占用 2");
            ItemStack stack = new ItemStack(GreekSummons.MINOTAUR_TOKEN.get());
            p.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var r = GreekSummons.MINOTAUR_TOKEN.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(!r.getResult().consumesAction() && stack.getCount() == 1, "已满：不吟唱、不消耗");
            GreekSummons.MINOTAUR_TOKEN.get().finishUsingItem(stack, h.getLevel(), p);
            h.assertTrue(stack.getCount() == 1 && units(h, p).size() == 2, "已满时吟唱完也不召唤");
        } finally {
            cleanup(h, p);
        }
        h.succeed();
    }

    // ───────────── 3.2 指令与行为 ─────────────

    /** TC-T14-30、31：跟随走近；主人瞬移到远处，单位瞬移过去。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void follows_and_teleports(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_follow", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 5));
        Vec3 near = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 18)));
        p.moveTo(near.x, near.y, near.z);
        h.runAfterDelay(120, () -> {
            double d = m.distanceTo(p);
            if (d > 8.5) {
                cleanup(h, p);
                h.fail("跟随后应在 8 格内，实际 " + d);
                return;
            }
            BlockPos far = h.absolutePos(new BlockPos(8, 2, 34));
            p.moveTo(far.getX() + 0.5, far.getY(), far.getZ() + 40.5);
            h.getLevel().setBlockAndUpdate(BlockPos.containing(p.position()).below(), Blocks.STONE.defaultBlockState());
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    h.getLevel().setBlockAndUpdate(BlockPos.containing(p.position()).offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                }
            }
        });
        h.runAfterDelay(200, () -> {
            double d = m.distanceTo(p);
            cleanup(h, p);
            h.assertTrue(d < 5, "主人瞬移 40 格外后单位应瞬移过去，实际 " + d);
            h.succeed();
        });
    }

    /** TC-T14-32、33：传令杖驻守；僵尸进入 16 格被打；单位留在驻守点附近。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 600)
    public static void guard_point_and_defend(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_guard", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 5));
        BlockPos post = h.absolutePos(new BlockPos(8, 2, 10));
        h.assertTrue(SummonService.commandGuard(p, post) == 1, "1 个单位改为驻守");
        h.assertTrue(m.link().mode == SummonMode.GUARD, "模式为驻守");
        p.moveTo(p.getX(), p.getY(), p.getZ() - 60);
        Zombie z = zombie(h, new BlockPos(8, 2, 22));
        float hp = z.getHealth();
        h.succeedWhen(() -> {
            h.assertTrue(z.getHealth() < hp || !z.isAlive(), "驻守单位应攻击进入范围的僵尸");
            h.assertTrue(m.blockPosition().distSqr(post) < 24 * 24, "不离开驻守点 24 格");
            cleanup(h, p);
        });
    }

    /** TC-T14-34、35、36：集火合法目标；对牛、村民下令被拒；主动索敌不选牛与村民。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void command_target_rules(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_rules", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 5));
        Cow cow = h.spawn(EntityType.COW, new BlockPos(6, 2, 9));
        Villager v = h.spawn(EntityType.VILLAGER, new BlockPos(10, 2, 9));
        h.assertTrue(SummonService.commandAttack(p, cow) == -1, "对牛下令应被拒");
        h.assertTrue(SummonService.commandAttack(p, v) == -1, "对村民下令应被拒");
        h.runAfterDelay(60, () -> {
            h.assertTrue(m.getTarget() == null, "附近只有牛和村民时不应有目标，实际 " + m.getTarget());
            Zombie z = zombie(h, new BlockPos(8, 2, 28));
            h.assertTrue(SummonService.commandAttack(p, z) == 1, "集火僵尸");
            h.assertTrue(m.getTarget() == z, "目标应为僵尸");
            cleanup(h, p);
            h.succeed();
        });
    }

    /** TC-T14-37、38：潜行右键放顶飞——普通生物被顶起并脱战；Boss 只击退。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void toss_lifts_and_disengages(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_toss", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 6));
        Zombie z = zombie(h, new BlockPos(8, 2, 9));
        h.assertTrue(SummonService.commandSpecial(p, z) == 1, "1 个单位放出顶飞");
        h.assertTrue(m.tossing(), "进入刨地");
        double[] maxVy = {0};
        boolean[] untargeted = {false};
        h.onEachTick(() -> {
            maxVy[0] = Math.max(maxVy[0], z.getDeltaMovement().y);
        });
        h.runAfterDelay(60, () -> {
            untargeted[0] = z.getTarget() == null || !SummonService.disengaged(z);
            h.assertTrue(maxVy[0] > 0.5, "应被顶起，最大垂直速度 " + maxVy[0]);
            h.assertTrue(SummonService.disengaged(z) || z.getTarget() == null, "顶飞后 3 秒内脱战");
            h.assertTrue(!m.requestSpecial(z), "冷却中不能再放");
            cleanup(h, p);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void toss_only_knocks_back_boss(GameTestHelper h) {
        floor(h);
        TargetFactsCollector.bossForTest(EntityType.HUSK);
        FakePlayer p = owner(h, "ma_summon_boss", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 6));
        var boss = h.spawn(EntityType.HUSK, new BlockPos(8, 2, 9));
        h.assertTrue(SummonService.commandSpecial(p, boss) == 1, "对 Boss 也能放");
        double[] maxVy = {0};
        h.onEachTick(() -> maxVy[0] = Math.max(maxVy[0], boss.getDeltaMovement().y));
        h.runAfterDelay(60, () -> {
            TargetFactsCollector.bossForTest(null);
            cleanup(h, p);
            h.assertTrue(maxVy[0] < 0.5, "Boss 不应被顶起，最大垂直速度 " + maxVy[0]);
            h.assertTrue(!SummonService.disengaged(boss), "Boss 不脱战");
            h.succeed();
        });
    }

    // ───────────── 3.3 跨区块与上线 ─────────────

    /** TC-T14-41：同一单位的第二个实体载入时被移除。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void duplicate_entity_removed(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_dup", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 6));
        SummonMinotaurEntity copy = GreekSummons.MINOTAUR.get().create(h.getLevel());
        copy.link().owner = p.getUUID();
        copy.link().unitId = m.link().unitId;
        copy.link().card = m.link().card;
        BlockPos at = h.absolutePos(new BlockPos(10, 2, 6));
        copy.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        h.getLevel().addFreshEntity(copy);
        h.runAfterDelay(2, () -> {
            int n = units(h, p).size();
            cleanup(h, p);
            h.assertTrue(n == 1, "复制的实体应被移除，实际 " + n);
            h.succeed();
        });
    }

    /** TC-T14-42：主人「上线」在远处，跟随单位被拉回身边。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void login_pulls_followers(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_login", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 6));
        BlockPos far = h.absolutePos(new BlockPos(8, 1, 3)).offset(0, 0, -70);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                h.getLevel().setBlockAndUpdate(far.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
            }
        }
        p.moveTo(far.getX() + 0.5, far.getY() + 1, far.getZ() + 0.5);
        SummonService.pullFollowers(p);
        double d = m.distanceTo(p);
        cleanup(h, p);
        h.assertTrue(d < 5, "上线后跟随单位应在身边，实际 " + d);
        h.succeed();
    }

    // ───────────── 3.4 死亡、重生、遣散 ─────────────

    /** TC-T14-50：阵亡释放上限、不重生。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void death_releases_slot(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_death", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 6));
        m.kill();
        h.runAfterDelay(2, () -> {
            int occ = SummonService.occupied(p);
            cleanup(h, p);
            h.assertTrue(occ == 0, "阵亡后占用应为 0，实际 " + occ);
            h.succeed();
        });
    }

    /** TC-T14-51、52：侦察单位阵亡后等待重生（仍占位）；没有神坛不重生，有了神坛在旁边重生，unitId 不变。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void scout_respawns_at_shrine(GameTestHelper h) {
        floor(h);
        SummonUnits.putForTest(new SummonUnitCard("greek", "test_scout", "mythic_age:summon_minotaur", 1, 1, false,
                "mythic_age:summon_token_minotaur"));
        FakePlayer p = owner(h, "ma_summon_scout", Age.CLASSICAL, new BlockPos(8, 2, 3));
        var card = SummonUnits.card("greek:test_scout").orElseThrow();
        h.assertTrue(SummonService.summon(p, card, Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 6))))
                == SummonService.Result.OK, "召唤");
        SummonRegistry reg = SummonRegistry.get(h.getLevel().getServer());
        SummonRegistry.Unit u = reg.byOwner(p.getUUID()).get(0);
        UUID unitId = u.unitId;
        units(h, p).get(0).kill();
        h.runAfterDelay(2, () -> {
            h.assertTrue(u.respawning && SummonService.occupied(p) == 1, "等待重生且仍占位");
            u.respawnAt = h.getLevel().getGameTime();
            SummonService.tickRespawns(h.getLevel().getServer());
            h.assertTrue(u.respawning, "没有神坛不重生");
            GlobalPos altar = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(8, 2, 14)));
            ShrineRegistry shrines = ShrineRegistry.get(h.getLevel().getServer());
            shrines.add(new ShrineRegistry.Entry(altar, p.getUUID(), "ma_summon_scout", Direction.SOUTH, Civilization.GREEK,
                    "greek:altar"));
            SummonService.tickRespawns(h.getLevel().getServer());
            shrines.remove(altar);
            boolean back = !u.respawning && units(h, p).size() == 1 && units(h, p).get(0).link().unitId.equals(unitId);
            cleanup(h, p);
            h.assertTrue(back, "有神坛后应在旁边重生，unitId 不变");
            h.succeed();
        });
    }

    /** TC-T14-53：遣散。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void dismiss_frees_slot(GameTestHelper h) {
        floor(h);
        FakePlayer p = owner(h, "ma_summon_dismiss", Age.CLASSICAL, new BlockPos(8, 2, 3));
        SummonMinotaurEntity m = summon(h, p, new BlockPos(8, 2, 6));
        h.assertTrue(SummonService.dismiss(h.getLevel().getServer(), m.link().unitId), "遣散");
        h.runAfterDelay(1, () -> {
            boolean gone = units(h, p).isEmpty() && SummonService.occupied(p) == 0;
            cleanup(h, p);
            h.assertTrue(gone, "实体消失、占用归零");
            h.succeed();
        });
    }

    // ───────────── 3.5 存档与交易 ─────────────

    /** TC-T14-60：登记写盘再读回。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void registry_roundtrip(GameTestHelper h) {
        SummonRegistry r = new SummonRegistry();
        UUID unit = UUID.randomUUID();
        SummonRegistry.Unit u = r.add(unit, UUID.randomUUID(), "greek:minotaur");
        u.mode = SummonMode.GUARD;
        u.guard = GlobalPos.of(h.getLevel().dimension(), new BlockPos(1, 2, 3));
        u.respawning = true;
        u.respawnAt = 12345L;
        var tag = r.save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
        SummonRegistry back = SummonRegistry.read(tag, h.getLevel().registryAccess());
        SummonRegistry.Unit b = back.get(unit).orElseThrow();
        h.assertTrue(b.mode == SummonMode.GUARD && b.guard.equals(u.guard) && b.respawning && b.respawnAt == 12345L
                && b.owner.equals(u.owner), "字段一致");
        h.succeed();
    }

    /**
     * TC-T14-61：弥诺陶洛斯召唤符是雅典娜的商品（祭司处、古典起、恩宠 18）；没供奉雅典娜买不到。
     * 次神商品还要求本村有该次神的祠（T11a），测试场没有村庄，所以只核对「买不到」与商品条件。
     */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void minotaur_token_is_athena_offer(GameTestHelper h) {
        floor(h);
        var offer = io.github.happysnappingturtle.mythicage.trade.TradeTableLoader.table(Civilization.GREEK)
                .offer("summon_minotaur").orElseThrow();
        h.assertTrue(offer.category() == io.github.happysnappingturtle.mythicage.core.trade.TradeOffer.Category.SUMMON
                && offer.minorGod().equals("athena") && offer.minAge() == Age.CLASSICAL && offer.price().favor() == 18
                && offer.result().item().equals("mythic_age:summon_token_minotaur"), "商品条件");
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(8, 2, 8)), Optional.empty(),
                Civilization.GREEK);
        h.assertTrue(g != null, "祭司");
        g.setNoAi(true);
        FakePlayer p = owner(h, "ma_summon_trade", Age.CLASSICAL, new BlockPos(10, 2, 8));
        p.getInventory().clearContent();
        MythData.get(p).civ(Civilization.GREEK).setFavor(40);
        TradeService.openGuardian(p, g);
        var o = TradeService.buy(p, "summon_minotaur", 18, UUID.randomUUID().toString());
        h.assertTrue(o.reason() != TradeReason.OK && p.getInventory().countItem(GreekSummons.MINOTAUR_TOKEN.get()) == 0,
                "没供奉雅典娜、本村没有雅典娜祠时买不到，实际 " + o.reason());
        TradeService.close(p);
        g.discard();
        SummonTargets.ownerForTest(p, false);
        h.succeed();
    }
}
