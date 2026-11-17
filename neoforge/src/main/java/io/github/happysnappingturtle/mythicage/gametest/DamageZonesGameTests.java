package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.combat.DamagePipeline;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.combat.DamageZones;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.relic.DamageRules;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.relic.RelicEffects;
import io.github.happysnappingturtle.mythicage.relic.RelicItems;
import io.github.happysnappingturtle.mythicage.relic.RelicService;
import io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * T25.1 伤害乘区（TC-T25.1-21、30～32 与神力分类）。举盾刺击、弥诺陶洛斯硬直、卡尔基诺斯的回归（TC-20、22、23）由原来的
 * {@code EquipmentGameTests.tc40/tc54}、{@code TrialCombatGameTests.pillar_impact_stuns}、{@code WildGameTests.karkinos_*}
 * 走完整受伤流程核对扣血，收拢后照过即是同一个数。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DamageZonesGameTests {
    private static final String BATCH = "damage_zones";
    private static final String T = GameTestTemplates.EMPTY_17X16X20;

    private DamageZonesGameTests() {
    }

    private static FakePlayer player(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(h.absoluteVec(new Vec3(8.5, 2, 6.5)));
        return p;
    }

    private static Pig pig(GameTestHelper h, int x, int z) {
        Pig pig = h.spawn(EntityType.PIG, x, 2, z);
        pig.setNoAi(true);
        return pig;
    }

    /** 打一下，返回扣了多少血。 */
    private static float hit(Pig pig, DamageSource s, float amount) {
        pig.invulnerableTime = 0;
        float before = pig.getHealth();
        pig.hurt(s, amount);
        return before - pig.getHealth();
    }

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        h.assertTrue(Math.abs(expected - actual) < 1e-3, what + "：应为 " + expected + "，实际 " + actual);
    }

    /** TC-T25.1-21：戴阿喀琉斯之甲被僵尸近战打：管线的结果与收拢前的公式（DamageRules.reduce）同一个数；弓箭不减。 */
    @GameTest(template = T, batch = BATCH)
    public static void relic_melee_reduction_same(GameTestHelper h) {
        FakePlayer p = player(h, "dz_relic");
        MythData.get(p).civ(Civilization.GREEK).setAge(Age.ARCHAIC);
        RelicService.onlineForTest(p, true);
        Zombie z = h.spawn(EntityType.ZOMBIE, 10, 2, 8);
        z.setNoAi(true);
        try {
            var slots = CuriosApi.getCuriosInventory(p).flatMap(i -> i.getStacksHandler(RelicItems.SLOT)).orElseThrow();
            slots.getStacks().setStackInSlot(0, new ItemStack(GreekRelics.ARMOR_OF_ACHILLES.get()));
            RelicService.reconcile(p);
            DamageSource melee = h.getLevel().damageSources().mobAttack(z);
            List<Double> r = RelicEffects.meleeReductions(p, melee);
            h.assertTrue(!r.isEmpty(), "阿喀琉斯之甲有近战减伤");
            float old = DamageRules.reduce(10, r, 0);
            close(h, old, DamagePipeline.evaluate(p, melee, 10).total(), "与收拢前相同");
            var arrow = new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), z, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
            close(h, 10, DamagePipeline.evaluate(p, h.getLevel().damageSources().arrow(arrow, z), 10).total(), "弓箭不减");
            h.succeed();
        } finally {
            RelicService.onlineForTest(p, false);
            z.discard();
        }
    }

    /** TC-T25.1-30：判定拦下（停火这类）的伤害归零、不进乘区。 */
    @GameTest(template = T, batch = BATCH)
    public static void gate_zeroes_damage(GameTestHelper h) {
        FakePlayer p = player(h, "dz_gate");
        Pig pig = pig(h, 6, 8);
        DamagePipeline.Gate gate = hit -> hit.attacker() == p;
        DamagePipeline.Provider boost = (hit, out) -> {
            if (hit.attacker() == p) {
                out.add(DamageZones.Mod.of(DamageZones.Zone.WINDOW, "dz_test", 2));
            }
        };
        DamagePipeline.registerForTest(gate, boost);
        try {
            close(h, 0, hit(pig, h.getLevel().damageSources().playerAttack(p), 4), "被拦下");
            DamagePipeline.unregisterForTest(gate, null);
            close(h, 8, hit(pig, h.getLevel().damageSources().playerAttack(p), 4), "拿掉判定后照常进乘区 ×2");
            h.succeed();
        } finally {
            DamagePipeline.unregisterForTest(gate, boost);
            pig.discard();
        }
    }

    /** 另一个模组在更早的优先级把伤害 ×2（只对登记的目标）。 */
    private static final Set<UUID> DOUBLED = ConcurrentHashMap.newKeySet();
    private static volatile boolean otherModRegistered;

    private static void otherMod(LivingIncomingDamageEvent e) {
        if (DOUBLED.contains(e.getEntity().getUUID())) {
            e.setAmount(e.getAmount() * 2);
        }
    }

    /** TC-T25.1-31：别的模组先改了伤害，本作的乘区在它的结果上算，不覆盖。 */
    @GameTest(template = T, batch = BATCH)
    public static void other_mod_first(GameTestHelper h) {
        if (!otherModRegistered) {
            otherModRegistered = true;
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, DamageZonesGameTests::otherMod);
        }
        FakePlayer p = player(h, "dz_other");
        Pig pig = pig(h, 6, 8);
        DOUBLED.add(pig.getUUID());
        DamagePipeline.Provider half = (hit, out) -> {
            if (hit.victim() == pig) {
                out.add(DamageZones.Mod.of(DamageZones.Zone.RESIST, "dz_test", 0.25));
            }
        };
        DamagePipeline.registerForTest(null, half);
        try {
            close(h, 6, hit(pig, h.getLevel().damageSources().playerAttack(p), 4), "4 × 2（别的模组）× 0.75（本作抗性）");
            h.succeed();
        } finally {
            DamagePipeline.unregisterForTest(null, half);
            DOUBLED.remove(pig.getUUID());
            pig.discard();
        }
    }

    /** TC-T25.1-32：打开明细后打一下，攻击者收到一行；关掉后不再记。 */
    @GameTest(template = T, batch = BATCH)
    public static void debug_breakdown(GameTestHelper h) {
        FakePlayer p = player(h, "dz_debug");
        Pig pig = pig(h, 6, 8);
        DamagePipeline.Provider gear = (hit, out) -> {
            if (hit.attacker() == p) {
                out.add(DamageZones.Mod.of(DamageZones.Zone.GEAR, "dz_test", 0.25));
            }
        };
        DamagePipeline.registerForTest(null, gear);
        try {
            DamagePipeline.debug(p.getUUID(), true);
            close(h, 5, hit(pig, h.getLevel().damageSources().playerAttack(p), 4), "4 × 1.25");
            var line = DamagePipeline.lastDebugForTest(p.getUUID());
            h.assertTrue(line != null && line.getString().contains("×1.25") && line.getString().contains("5.0"), "收到明细：" + line);
            DamagePipeline.debug(p.getUUID(), false);
            hit(pig, h.getLevel().damageSources().playerAttack(p), 1);
            h.assertTrue(DamagePipeline.lastDebugForTest(p.getUUID()) == null, "关掉后不再记");
            h.succeed();
        } finally {
            DamagePipeline.debug(p.getUUID(), false);
            DamagePipeline.unregisterForTest(null, gear);
            pig.discard();
        }
    }

    /** D6：雷击的施法者是玩家，但按神力算，不吃装备区；同一名玩家普通攻击照吃。 */
    @GameTest(template = T, batch = BATCH)
    public static void power_skips_gear(GameTestHelper h) {
        FakePlayer p = player(h, "dz_power");
        Pig pig = pig(h, 6, 8);
        DamagePipeline.Provider gear = (hit, out) -> {
            if (hit.attacker() == p) {
                out.add(DamageZones.Mod.of(DamageZones.Zone.GEAR, "dz_test", 0.25));
            }
        };
        DamagePipeline.registerForTest(null, gear);
        try {
            DamageSource bolt = h.getLevel().damageSources().source(GreekPowers.DIVINE_BOLT, p);
            h.assertTrue(DamagePipeline.hit(pig, bolt).kind() == DamageZones.Source.POWER, "雷击按神力分类");
            close(h, 4, hit(pig, bolt, 4), "神力不吃装备区");
            close(h, 5, hit(pig, h.getLevel().damageSources().playerAttack(p), 4), "普通攻击 ×1.25");
            h.succeed();
        } finally {
            DamagePipeline.unregisterForTest(null, gear);
            pig.discard();
        }
    }
}
