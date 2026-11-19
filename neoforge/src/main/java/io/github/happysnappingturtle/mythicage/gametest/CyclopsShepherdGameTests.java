package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.advancement.MythEvents;
import io.github.happysnappingturtle.mythicage.combat.DamagePipeline;
import io.github.happysnappingturtle.mythicage.content.greek.cyclops.GreekCyclops;
import io.github.happysnappingturtle.mythicage.content.greek.cyclops.ThrownWine;
import io.github.happysnappingturtle.mythicage.content.greek.cyclops.WineBowl;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.combat.DamageZones;
import io.github.happysnappingturtle.mythicage.core.wild.DrunkRules;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import io.github.happysnappingturtle.mythicage.entity.cyclops.ShepherdCyclopsEntity;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.wild.greek.GreekWild;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * T25a 阶段 ⑥：独眼巨人·牧人（TC-T25a-70、74～77、79、80）。场地 17 × 36、地面 y = 1；牧人不绑巢穴（没有家与睡处），
 * 只测他自己的行为。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CyclopsShepherdGameTests {
    private static final String BATCH = "cyclops_shepherd";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private CyclopsShepherdGameTests() {
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static ShepherdCyclopsEntity shepherd(GameTestHelper h, BlockPos at) {
        ShepherdCyclopsEntity s = h.spawn(GreekCyclops.SHEPHERD.get(), at);
        s.setYRot(0);
        s.yBodyRot = 0;
        return s;
    }

    private static Zombie dummy(GameTestHelper h, BlockPos at) {
        Zombie z = h.spawn(EntityType.ZOMBIE, at);
        z.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60));
        z.setNoAi(true);
        z.setPersistenceRequired();
        return z;
    }

    private static void clear(GameTestHelper h, Mob... mobs) {
        for (Mob m : mobs) {
            m.discard();
        }
    }

    /** TC-T25a-70：精英标签生效（雷击按精英 40 点、诅咒对精英无效都看这个标签）；生命 160、伤害 14；伤害乘区按精英算。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 60)
    public static void shepherd_is_elite(GameTestHelper h) {
        floor(h);
        ShepherdCyclopsEntity s = shepherd(h, new BlockPos(8, 2, 8));
        h.assertTrue(TargetFactsCollector.isElite(s) && TargetFactsCollector.isMythic(s), "在 #elite 与 #mythic");
        h.assertTrue(DamagePipeline.targetOf(s) == DamageZones.Target.ELITE, "伤害乘区按精英");
        h.assertTrue(Math.abs(s.getMaxHealth() - 160) < 0.01, "生命 160");
        h.assertTrue(Math.abs(s.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) - 14) < 0.01, "伤害 14");
        clear(h, s);
        h.succeed();
    }

    /** TC-T25a-74（D10）：扔出的酒落地成酒碗（不是掉落物、捡不起）；牧人走过去喝 2 秒，醉倒 8 秒，酒碗喝完消失。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void wine_makes_him_drunk(GameTestHelper h) {
        floor(h);
        ShepherdCyclopsEntity s = shepherd(h, new BlockPos(8, 2, 6));
        ThrownWine wine = new ThrownWine(GreekCyclops.THROWN_WINE.get(), h.getLevel());
        Vec3 drop = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 4, 16)));
        wine.moveTo(drop.x, drop.y, drop.z);
        wine.setDeltaMovement(0, -0.5, 0);
        h.getLevel().addFreshEntity(wine);
        long[] drinkAt = {-1};
        long[] drunkAt = {-1};
        h.onEachTick(() -> {
            long now = h.getTick();
            if (drinkAt[0] < 0 && s.drunkPhase() == DrunkRules.Phase.DRINK) {
                drinkAt[0] = now;
            }
            if (drunkAt[0] < 0 && s.drunkPhase() == DrunkRules.Phase.DRUNK) {
                drunkAt[0] = now;
            }
        });
        h.runAfterDelay(15, () -> {
            List<WineBowl> bowls = h.getLevel().getEntitiesOfClass(WineBowl.class, h.getBounds());
            h.assertTrue(bowls.size() == 1, "酒落地成一只酒碗，实际 " + bowls.size());
            h.assertTrue(!bowls.get(0).isPickable() && h.getLevel().getEntitiesOfClass(ItemEntity.class,
                    bowls.get(0).getBoundingBox().inflate(2)).isEmpty(), "酒碗不是掉落物、点不中");
        });
        h.succeedWhen(() -> {
            h.assertTrue(drunkAt[0] > 0, "应喝醉（喝 " + drinkAt[0] + "）");
            h.assertTrue(drunkAt[0] - drinkAt[0] >= DrunkRules.DRINK_TICKS - 1 && drunkAt[0] - drinkAt[0] <= DrunkRules.DRINK_TICKS + 2,
                    "喝 2 秒，实际 " + (drunkAt[0] - drinkAt[0]));
            h.assertTrue(h.getLevel().getEntitiesOfClass(WineBowl.class, h.getBounds()).isEmpty(),
                    "喝完酒碗消失");
            h.assertTrue(s.animState() == MythAnimState.DRUNK, "仰面醉倒");
            h.assertTrue(h.getTick() - drunkAt[0] >= DrunkRules.DRUNK_TICKS / 2, "醉倒还在");
            h.assertTrue(s.drunk(), "8 秒内还醉着");
            clear(h, s);
        });
    }

    /** TC-T25a-75（D12）：醉倒期间伤害 ×3（窗口区 cyclops_drunk），清醒时 ×1。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void drunk_takes_triple(GameTestHelper h) {
        floor(h);
        ShepherdCyclopsEntity s = shepherd(h, new BlockPos(8, 2, 8));
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_cyc_triple"));
        var src = h.getLevel().damageSources().playerAttack(p);
        DamageZones.Result sober = DamagePipeline.evaluate(s, src, 10);
        h.assertTrue(Math.abs(sober.total() - 10) < 0.01, "清醒时 ×1，实际 " + sober.total());
        WineBowl.place(h.getLevel(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 11))));
        h.succeedWhen(() -> {
            h.assertTrue(s.drunk(), "等他醉倒");
            DamageZones.Result r = DamagePipeline.evaluate(s, src, 10);
            h.assertTrue(Math.abs(r.total() - 30) < 0.01, "醉倒 ×3，实际 " + r.total());
            clear(h, s);
        });
    }

    /** TC-T25a-36 的平台部分：喝酒途中挨打 → 中断、不醉倒，酒碗还在。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 300)
    public static void hit_while_drinking_interrupts(GameTestHelper h) {
        floor(h);
        ShepherdCyclopsEntity s = shepherd(h, new BlockPos(8, 2, 8));
        WineBowl bowl = WineBowl.place(h.getLevel(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 10))));
        AtomicBoolean hit = new AtomicBoolean();
        h.onEachTick(() -> {
            if (!hit.get() && s.drunkPhase() == DrunkRules.Phase.DRINK) {
                hit.set(true);
                s.hurt(h.getLevel().damageSources().generic(), 1);
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(hit.get(), "等他开始喝");
            h.assertTrue(s.drunkPhase() == DrunkRules.Phase.SOBER, "中断后清醒，实际 " + s.drunkPhase());
            h.assertTrue(bowl.isAlive(), "酒碗还在");
            clear(h, s);
            bowl.discard();
        });
    }

    /** TC-T25a-76：投石——举石 1.2 秒（预警）后投出，落点附近的目标受伤，地形不变。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void throws_rock_with_warning(GameTestHelper h) {
        floor(h);
        ShepherdCyclopsEntity s = shepherd(h, new BlockPos(8, 2, 4));
        Zombie z = dummy(h, new BlockPos(8, 2, 18));
        z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        float hp = z.getHealth();
        s.setTarget(z);
        boolean[] lifted = {false};
        h.onEachTick(() -> {
            if (s.animState() == MythAnimState.LIFT_ROCK) {
                lifted[0] = true;
                h.assertTrue(z.getHealth() == hp, "举石预警期间还没砸到");
            }
            s.setTarget(z);
        });
        h.succeedWhen(() -> {
            h.assertTrue(lifted[0], "先举石预警");
            h.assertTrue(hp - z.getHealth() >= ShepherdCyclopsEntity.ROCK_DAMAGE - 0.01, "砸中受 12，实际 " + (hp - z.getHealth()));
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    h.assertBlockPresent(Blocks.STONE, new BlockPos(8 + dx, 1, 18 + dz));
                    h.assertBlockPresent(Blocks.AIR, new BlockPos(8 + dx, 2, 18 + dz));
                }
            }
            clear(h, s, z);
        });
    }

    /** TC-T25a-77（D13、D14）：击杀掉落库克罗普斯之火 2、羊毛 6–10、英雄锻造模板 1。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void drops_fire_wool_template(GameTestHelper h) {
        floor(h);
        ShepherdCyclopsEntity s = shepherd(h, new BlockPos(8, 2, 8));
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_cyc_drop"));
        s.hurt(h.getLevel().damageSources().playerAttack(p), 10000);
        h.runAfterDelay(5, () -> {
            List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(8, 2, 8))).inflate(6));
            int fire = count(items, GreekCyclops.CYCLOPS_FIRE.get());
            int wool = count(items, Items.WHITE_WOOL);
            int tpl = count(items, GreekCyclops.HERO_SMITHING_TEMPLATE.get());
            items.forEach(ItemEntity::discard);
            h.assertTrue(fire == 2, "库克罗普斯之火 2，实际 " + fire);
            h.assertTrue(wool >= 6 && wool <= 10, "羊毛 6–10，实际 " + wool);
            h.assertTrue(tpl == 1, "模板 1，实际 " + tpl);
            h.succeed();
        });
    }

    private static int count(List<ItemEntity> items, Item item) {
        return items.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    /** TC-T25a-79：野生半人马进入 16 格，牧人主动攻击。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void attacks_centaur_in_range(GameTestHelper h) {
        floor(h);
        ShepherdCyclopsEntity s = shepherd(h, new BlockPos(8, 2, 6));
        Mob c = h.spawn(GreekWild.CENTAUR.get(), new BlockPos(8, 2, 18));
        c.setNoAi(true);
        h.succeedWhen(() -> {
            h.assertTrue(s.getTarget() == c, "应以半人马为目标，实际 " + s.getTarget());
            clear(h, s, c);
        });
    }

    /** TC-T25a-80：醉倒期间被玩家击杀 → 进度事件「我的名字叫『无人』」；清醒时击杀不触发。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void nobody_advancement(GameTestHelper h) {
        floor(h);
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_cyc_nobody"));
        FakePlayer q = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_cyc_sober"));
        List<UUID> fired = new java.util.concurrent.CopyOnWriteArrayList<>();
        BiConsumer<ServerPlayer, MythEvents.Fired> l = (pl, f) -> {
            if (ShepherdCyclopsEntity.NOBODY_EVENT.equals(f.event())) {
                fired.add(f.player());
            }
        };
        MythEvents.addListener(l);
        ShepherdCyclopsEntity sober = shepherd(h, new BlockPos(3, 2, 30));
        sober.hurt(h.getLevel().damageSources().playerAttack(q), 10000);
        ShepherdCyclopsEntity s = shepherd(h, new BlockPos(8, 2, 6));
        WineBowl.place(h.getLevel(), Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 9))));
        boolean[] killed = {false};
        h.onEachTick(() -> {
            if (!killed[0] && s.drunk()) {
                killed[0] = true;
                s.hurt(h.getLevel().damageSources().playerAttack(p), 10000);
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(killed[0] && !s.isAlive(), "等他醉倒后击杀");
            MythEvents.removeListener(l);
            h.assertTrue(fired.contains(p.getUUID()), "醉倒时击杀触发进度");
            h.assertTrue(!fired.contains(q.getUUID()), "清醒时击杀不触发");
            h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds()).forEach(ItemEntity::discard);
        });
    }
}
