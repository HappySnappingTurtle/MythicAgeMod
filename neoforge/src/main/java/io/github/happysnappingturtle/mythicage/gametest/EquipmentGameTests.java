package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.content.LangKeys;
import io.github.happysnappingturtle.mythicage.core.equipment.WeaponMotion;
import io.github.happysnappingturtle.mythicage.equipment.MeleeWeaponItem;
import io.github.happysnappingturtle.mythicage.equipment.WeaponFeedback;
import io.github.happysnappingturtle.mythicage.equipment.WeaponMotionState;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.Consumer;
import io.github.happysnappingturtle.mythicage.content.greek.equipment.GreekEquipment;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.equipment.DrawRules;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.equipment.CounterEvents;
import io.github.happysnappingturtle.mythicage.equipment.EquipmentTags;
import io.github.happysnappingturtle.mythicage.equipment.SetBonuses;
import io.github.happysnappingturtle.mythicage.equipment.ShieldAttack;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import io.github.happysnappingturtle.mythicage.summon.greek.SummonPegasusEntity;
import io.github.happysnappingturtle.mythicage.wild.greek.GreekWild;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * T17 青铜装备与希腊基础武器（TC-T17-20～58）。
 * FakePlayer 不 tick：装备的属性修饰不会自动挂上，用 {@link #equip} 手动挂；攻击冷却、举盾时长用反射设定。
 * 方阵半径 4 格，各用例的实体都放在自己的结构里。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private static final String BATCH = "equipment";
    private static final String T = GameTestTemplates.EMPTY_17X16X20;
    private static final BlockPos AT = new BlockPos(8, 2, 8);

    private EquipmentGameTests() {
    }

    // ───────────── 搭建 ─────────────

    private static FakePlayer player(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(AT)));
        p.getAbilities().instabuild = false;
        return p;
    }

    /** 穿上并手动挂上装备的属性修饰（真实玩家由每刻的装备检测完成）。 */
    private static void equip(FakePlayer p, EquipmentSlot slot, ItemLike item) {
        ItemStack old = p.getItemBySlot(slot);
        old.forEachModifier(slot, (attr, mod) -> {
            AttributeInstance a = p.getAttribute(attr);
            if (a != null) {
                a.removeModifier(mod.id());
            }
        });
        ItemStack s = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        p.setItemSlot(slot, s);
        s.forEachModifier(slot, (attr, mod) -> {
            AttributeInstance a = p.getAttribute(attr);
            if (a != null) {
                a.addOrUpdateTransientModifier(mod);
            }
        });
    }

    private static void fullBronze(FakePlayer p) {
        equip(p, EquipmentSlot.HEAD, GreekEquipment.BRONZE_HELMET.get());
        equip(p, EquipmentSlot.CHEST, GreekEquipment.BRONZE_CHESTPLATE.get());
        equip(p, EquipmentSlot.LEGS, GreekEquipment.BRONZE_LEGGINGS.get());
        equip(p, EquipmentSlot.FEET, GreekEquipment.BRONZE_BOOTS.get());
    }

    private static double phalanx(FakePlayer p) {
        SetBonuses.refresh(p);
        AttributeModifier m = p.getAttribute(Attributes.ARMOR).getModifier(SetBonuses.Phalanx.ID);
        return m == null ? 0 : m.amount();
    }

    private static <T extends LivingEntity> T still(T e) {
        if (e instanceof net.minecraft.world.entity.Mob m) {
            m.setNoAi(true);
        }
        return e;
    }

    private static void setField(Object o, String name, int v) {
        try {
            Class<?> c = o.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    f.setInt(o, v);
                    return;
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            throw new GameTestAssertException("找不到字段 " + name);
        } catch (IllegalAccessException e) {
            throw new GameTestAssertException("写字段 " + name + " 失败：" + e);
        }
    }

    /** 副手举盾满 10 刻（isBlocking 要求 ≥ 5 刻）。 */
    private static void raiseShield(LivingEntity e, ItemLike shield) {
        e.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(shield));
        e.startUsingItem(InteractionHand.OFF_HAND);
        setField(e, "useItemRemaining", e.getUseItem().getUseDuration(e) - 10);
    }

    private static void fullStrength(FakePlayer p) {
        setField(p, "attackStrengthTicker", 100);
    }

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        h.assertTrue(Math.abs(expected - actual) < 1e-3, what + "：应为 " + expected + "，实际 " + actual);
    }

    private static float hit(LivingEntity e, DamageSource s, float amount) {
        e.setHealth(e.getMaxHealth());
        float before = e.getHealth();
        e.invulnerableTime = 0;
        e.hurt(s, amount);
        return before - e.getHealth();
    }

    // ───────────── 3.1 护甲与方阵 ─────────────

    /** TC-T17-20：四件青铜甲护甲 17、韧性 4；每件耐久 248/360/338/293。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc20_bronze_armor(GameTestHelper h) {
        FakePlayer p = player(h, "eq_armor");
        fullBronze(p);
        close(h, 17, p.getAttributeValue(Attributes.ARMOR), "护甲");
        close(h, 4, p.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "韧性");
        int[] dur = {248, 360, 338, 293};
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getItemBySlot(slots[i]);
            h.assertTrue(s.getMaxDamage() == dur[i], slots[i] + " 耐久应为 " + dur[i] + "，实际 " + s.getMaxDamage());
            h.assertTrue(s.is(GreekEquipment.SET_BRONZE), "带青铜套装标签");
        }
        h.succeed();
    }

    /** TC-T17-21：只穿 3 件不触发；穿上第 4 件后刷新一次 → +1。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc21_phalanx_needs_full_set(GameTestHelper h) {
        FakePlayer p = player(h, "eq_three");
        equip(p, EquipmentSlot.HEAD, GreekEquipment.BRONZE_HELMET.get());
        equip(p, EquipmentSlot.CHEST, GreekEquipment.BRONZE_CHESTPLATE.get());
        equip(p, EquipmentSlot.LEGS, GreekEquipment.BRONZE_LEGGINGS.get());
        equip(p, EquipmentSlot.OFFHAND, GreekEquipment.ASPIS.get());
        still(h.spawn(EntityType.IRON_GOLEM, 10, 2, 8));
        close(h, 0, phalanx(p), "3 件");
        equip(p, EquipmentSlot.FEET, GreekEquipment.BRONZE_BOOTS.get());
        close(h, 1, phalanx(p), "4 件");
        h.killAllEntitiesOfClass(IronGolem.class);
        h.succeed();
    }

    /** TC-T17-22 / 26 / 27：5 个铁傀儡封顶 +3；移走 2 个仍 +3，再移走 1 个 +2；反复刷新只有一条修饰；5 格外不计。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc22_phalanx_cap(GameTestHelper h) {
        FakePlayer p = player(h, "eq_cap");
        fullBronze(p);
        equip(p, EquipmentSlot.MAINHAND, GreekEquipment.ASPIS.get());
        List<IronGolem> golems = new java.util.ArrayList<>();
        int[][] spots = {{10, 8}, {6, 8}, {8, 10}, {8, 6}, {10, 10}};
        for (int[] s : spots) {
            golems.add(still(h.spawn(EntityType.IRON_GOLEM, s[0], 2, s[1])));
        }
        still(h.spawn(EntityType.IRON_GOLEM, 14, 2, 8));
        close(h, 3, phalanx(p), "5 个铁傀儡（另有 1 个在 6 格外）");
        golems.get(0).discard();
        golems.get(1).discard();
        close(h, 3, phalanx(p), "剩 3 个");
        golems.get(2).discard();
        close(h, 2, phalanx(p), "剩 2 个");
        for (int i = 0; i < 5; i++) {
            SetBonuses.refresh(p);
        }
        long n = p.getAttribute(Attributes.ARMOR).getModifiers().stream().filter(m -> m.id().equals(SetBonuses.Phalanx.ID)).count();
        h.assertTrue(n == 1, "方阵修饰只有一条，实际 " + n);
        h.killAllEntitiesOfClass(IronGolem.class);
        h.succeed();
    }

    /** TC-T17-23：只有村民和驯服的狼 → +0。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc23_villagers_and_pets_not_allies(GameTestHelper h) {
        FakePlayer p = player(h, "eq_pets");
        fullBronze(p);
        equip(p, EquipmentSlot.OFFHAND, GreekEquipment.ASPIS.get());
        still(h.spawn(EntityType.VILLAGER, 10, 2, 8));
        Wolf wolf = still(h.spawn(EntityType.WOLF, 6, 2, 8));
        wolf.tame(p);
        close(h, 0, phalanx(p), "村民与狼");
        h.killAllEntitiesOfClass(Villager.class);
        h.killAllEntitiesOfClass(Wolf.class);
        h.succeed();
    }

    /** TC-T17-24：自己的召唤单位计入、别人的召唤单位不计；同队玩家算友方，非同队玩家不算（FakePlayer 不能放进世界，队伍只查 isAlly）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void tc24_summons_and_team(GameTestHelper h) {
        FakePlayer p = player(h, "eq_team");
        FakePlayer mate = player(h, "eq_mate");
        FakePlayer stranger = player(h, "eq_stranger");
        var board = h.getLevel().getScoreboard();
        PlayerTeam team = board.addPlayerTeam("eq_" + UUID.randomUUID().toString().substring(0, 8));
        board.addPlayerToTeam(p.getScoreboardName(), team);
        board.addPlayerToTeam(mate.getScoreboardName(), team);
        try {
            h.assertTrue(SetBonuses.Phalanx.isAlly(p, mate), "同队玩家是友方");
            h.assertFalse(SetBonuses.Phalanx.isAlly(p, stranger), "非同队玩家不是友方");
            fullBronze(p);
            equip(p, EquipmentSlot.OFFHAND, GreekEquipment.ASPIS.get());
            SummonService.Result mine = SummonService.summon(p, SummonUnits.card("greek:pegasus").orElseThrow(),
                    Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 10))));
            SummonService.Result theirs = SummonService.summon(stranger, SummonUnits.card("greek:pegasus").orElseThrow(),
                    Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 6))));
            h.assertTrue(mine == SummonService.Result.OK && theirs == SummonService.Result.OK, "召唤天马应成功");
            close(h, 1, phalanx(p), "只算自己的天马");
        } finally {
            board.removePlayerTeam(team);
            h.getLevel().getEntitiesOfClass(SummonPegasusEntity.class, new AABB(h.absolutePos(AT)).inflate(20)).forEach(e -> e.discard());
        }
        h.succeed();
    }

    /** TC-T17-25：盾放回背包 → 修饰移除。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc25_no_shield_removes(GameTestHelper h) {
        FakePlayer p = player(h, "eq_noshield");
        fullBronze(p);
        equip(p, EquipmentSlot.OFFHAND, GreekEquipment.ASPIS.get());
        still(h.spawn(EntityType.IRON_GOLEM, 10, 2, 8));
        close(h, 1, phalanx(p), "拿着盾");
        equip(p, EquipmentSlot.OFFHAND, Items.AIR);
        p.getInventory().setItem(9, new ItemStack(GreekEquipment.ASPIS.get()));
        close(h, 0, phalanx(p), "盾在背包里");
        h.killAllEntitiesOfClass(IronGolem.class);
        h.succeed();
    }

    // ───────────── 3.2 武器 ─────────────

    /** TC-T17-30 / 31 / 35：面板数值；长矛触及只挂主手；耐久与数值表一致。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc30_weapon_stats(GameTestHelper h) {
        Object[][] rows = {{GreekEquipment.XIPHOS.get(), 6.0, 1.8, 0.0, 375}, {GreekEquipment.DORY.get(), 7.0, 1.1, 1.0, 375},
                {GreekEquipment.OLIVE_CLUB.get(), 7.0, 0.9, 0.0, 200}};
        for (Object[] r : rows) {
            FakePlayer p = player(h, "eq_stats");
            equip(p, EquipmentSlot.MAINHAND, (ItemLike) r[0]);
            close(h, (double) r[1], p.getAttributeValue(Attributes.ATTACK_DAMAGE), r[0] + " 伤害");
            close(h, (double) r[2], p.getAttributeValue(Attributes.ATTACK_SPEED), r[0] + " 攻速");
            close(h, 3.0 + (double) r[3], p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), r[0] + " 触及");
            h.assertTrue(new ItemStack((ItemLike) r[0]).getMaxDamage() == (int) r[4], r[0] + " 耐久");
        }
        ItemStack dory = new ItemStack(GreekEquipment.DORY.get());
        dory.getAttributeModifiers().modifiers().forEach(e -> h.assertTrue(e.slot() == EquipmentSlotGroup.MAINHAND,
                "长矛的属性只挂主手：" + e.attribute()));
        h.assertTrue(new ItemStack(GreekEquipment.ASPIS.get()).getMaxDamage() == 500, "圆盾耐久 500");
        h.assertTrue(new ItemStack(GreekEquipment.COMPOSITE_BOW.get()).getMaxDamage() == 384, "复合弓耐久 384");
        h.succeed();
    }

    /** TC-T17-32 / 33 / 34：短剑、大棒不横扫；钝器标签；大棒不能当燃料。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc32_sweep_blunt_fuel(GameTestHelper h) {
        ItemStack xiphos = new ItemStack(GreekEquipment.XIPHOS.get());
        ItemStack club = new ItemStack(GreekEquipment.OLIVE_CLUB.get());
        h.assertFalse(xiphos.canPerformAction(ItemAbilities.SWORD_SWEEP), "短剑不横扫");
        h.assertFalse(club.canPerformAction(ItemAbilities.SWORD_SWEEP), "大棒不横扫");
        h.assertTrue(new ItemStack(Items.IRON_SWORD).canPerformAction(ItemAbilities.SWORD_SWEEP), "对照：铁剑横扫");
        h.assertTrue(club.is(EquipmentTags.BLUNT) && !xiphos.is(EquipmentTags.BLUNT), "钝器标签");
        h.assertTrue(club.getBurnTime(RecipeType.SMELTING) <= 0, "大棒不是燃料");
        h.succeed();
    }

    // ───────────── 3.3 圆盾反击 ─────────────

    /** TC-T17-40 / 41 / 42 / 44 / 45：+2、第 11 刻不加、只加一次、原版盾不开窗口、弓箭不吃。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc40_counter_window(GameTestHelper h) {
        FakePlayer p = player(h, "eq_counter");
        Zombie z = still(h.spawn(EntityType.ZOMBIE, 10, 2, 8));
        Pig pig = still(h.spawn(EntityType.PIG, 6, 2, 8));
        DamageSource incoming = h.getLevel().damageSources().mobAttack(z);
        DamageSource mine = h.getLevel().damageSources().playerAttack(p);
        long now = h.getLevel().getGameTime();
        ItemStack aspis = new ItemStack(GreekEquipment.ASPIS.get());
        h.assertTrue(CounterEvents.blocked(p, incoming, aspis, now), "圆盾格挡 → 开窗口");
        close(h, 5, hit(pig, mine, 3), "窗口内第一次 +2");
        close(h, 3, hit(pig, mine, 3), "第二次不加");
        h.assertTrue(CounterEvents.blocked(p, incoming, aspis, now - 11), "窗口从 11 刻前开始");
        close(h, 2, hit(pig, mine, 2), "第 11 刻不加");
        h.assertFalse(CounterEvents.blocked(p, incoming, new ItemStack(Items.SHIELD), now), "原版盾不开窗口");
        CounterEvents.blocked(p, incoming, aspis, now);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), p, new ItemStack(Items.ARROW), null);
        close(h, 2, hit(pig, h.getLevel().damageSources().arrow(arrow, p), 2), "弓箭不吃加成");
        h.killAllEntitiesOfClass(Zombie.class);
        h.killAllEntitiesOfClass(Pig.class);
        h.succeed();
    }

    /** TC-T17-43：盾被斧子破掉的那次不开窗口（D6）。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc43_axe_break_no_window(GameTestHelper h) {
        FakePlayer p = player(h, "eq_axe");
        Zombie z = still(h.spawn(EntityType.ZOMBIE, 10, 2, 8));
        z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        h.assertFalse(CounterEvents.blocked(p, h.getLevel().damageSources().mobAttack(z), new ItemStack(GreekEquipment.ASPIS.get()),
                h.getLevel().getGameTime()), "破盾不开窗口");
        h.assertFalse(CounterEvents.isOpen(p), "没有窗口");
        h.killAllEntitiesOfClass(Zombie.class);
        h.succeed();
    }

    // ───────────── 3.4 长矛举盾刺击 ─────────────

    private static FakePlayer spearman(GameTestHelper h, String name) {
        FakePlayer p = player(h, name);
        equip(p, EquipmentSlot.MAINHAND, GreekEquipment.DORY.get());
        raiseShield(p, GreekEquipment.ASPIS.get());
        fullStrength(p);
        return p;
    }

    /** TC-T17-50：刺击 4.2，仍在格挡，冷却重置。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc50_thrust(GameTestHelper h) {
        FakePlayer p = spearman(h, "eq_thrust");
        Pig pig = still(h.spawn(EntityType.PIG, 8, 2, 11));
        float before = pig.getHealth();
        h.assertTrue(ShieldAttack.thrust(p, pig), "应出手");
        close(h, 4.2, before - pig.getHealth(), "刺击伤害");
        h.assertTrue(p.isBlocking(), "仍在格挡");
        h.assertTrue(p.getAttackStrengthScale(0.5F) < ShieldAttack.MIN_STRENGTH, "冷却已重置");
        h.killAllEntitiesOfClass(Pig.class);
        h.succeed();
    }

    /** TC-T17-51 / 52 / 53：副手不是盾、没举盾、主手不是长矛、冷却未满、超出触及、视线被挡 → 不出手。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc51_thrust_rejected(GameTestHelper h) {
        Pig pig = still(h.spawn(EntityType.PIG, 8, 2, 11));
        FakePlayer noShield = player(h, "eq_r1");
        equip(noShield, EquipmentSlot.MAINHAND, GreekEquipment.DORY.get());
        fullStrength(noShield);
        h.assertFalse(ShieldAttack.thrust(noShield, pig), "副手没有盾");
        FakePlayer down = player(h, "eq_r2");
        equip(down, EquipmentSlot.MAINHAND, GreekEquipment.DORY.get());
        down.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(GreekEquipment.ASPIS.get()));
        fullStrength(down);
        h.assertFalse(ShieldAttack.thrust(down, pig), "盾没举起");
        FakePlayer sword = player(h, "eq_r3");
        equip(sword, EquipmentSlot.MAINHAND, GreekEquipment.XIPHOS.get());
        raiseShield(sword, GreekEquipment.ASPIS.get());
        fullStrength(sword);
        h.assertFalse(ShieldAttack.thrust(sword, pig), "主手不是长矛");
        FakePlayer tired = spearman(h, "eq_r4");
        tired.resetAttackStrengthTicker();
        h.assertFalse(ShieldAttack.thrust(tired, pig), "冷却未满");
        Pig far = still(h.spawn(EntityType.PIG, 8, 2, 16));
        h.assertFalse(ShieldAttack.thrust(spearman(h, "eq_r5"), far), "超出触及（8 格）");
        h.setBlock(new BlockPos(8, 3, 10), Blocks.STONE);
        h.setBlock(new BlockPos(8, 2, 10), Blocks.STONE);
        h.assertFalse(ShieldAttack.thrust(spearman(h, "eq_r6"), pig), "视线被挡");
        close(h, pig.getMaxHealth(), pig.getHealth(), "猪没受伤");
        h.killAllEntitiesOfClass(Pig.class);
        h.succeed();
    }

    /** TC-T17-54：刺击不消耗反击窗口；之后的普通攻击才吃 +2。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc54_thrust_keeps_counter(GameTestHelper h) {
        FakePlayer p = spearman(h, "eq_mix");
        Zombie z = still(h.spawn(EntityType.ZOMBIE, 10, 2, 8));
        Pig pig = still(h.spawn(EntityType.PIG, 8, 2, 11));
        CounterEvents.blocked(p, h.getLevel().damageSources().mobAttack(z), new ItemStack(GreekEquipment.ASPIS.get()),
                h.getLevel().getGameTime());
        float before = pig.getHealth();
        ShieldAttack.thrust(p, pig);
        close(h, 4.2, before - pig.getHealth(), "刺击没有 +2");
        h.assertTrue(CounterEvents.isOpen(p), "窗口仍开着");
        close(h, 5, hit(pig, h.getLevel().damageSources().playerAttack(p), 3), "普通攻击 +2");
        h.killAllEntitiesOfClass(Zombie.class);
        h.killAllEntitiesOfClass(Pig.class);
        h.succeed();
    }

    /** TC-T17-55：刺击打在举盾的目标上照常被挡（这里用举盾的僵尸）。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc55_thrust_blocked_by_shield(GameTestHelper h) {
        FakePlayer p = spearman(h, "eq_vs_shield");
        Zombie z = still(h.spawn(EntityType.ZOMBIE, 8, 2, 11));
        z.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        z.setYHeadRot(z.getYRot());
        z.yBodyRot = z.getYRot();
        raiseShield(z, Items.SHIELD);
        float before = z.getHealth();
        ShieldAttack.thrust(p, z);
        close(h, 0, before - z.getHealth(), "被盾挡下");
        h.killAllEntitiesOfClass(Zombie.class);
        h.succeed();
    }

    // ───────────── 3.5 复合弓与配方 ─────────────

    private static double arrowSpeed(GameTestHelper h, FakePlayer p, ItemStack bow, int ticks) {
        h.killAllEntitiesOfClass(AbstractArrow.class);
        p.getInventory().add(new ItemStack(Items.ARROW, 4));
        bow.getItem().releaseUsing(bow, h.getLevel(), p, bow.getUseDuration(p) - ticks);
        List<AbstractArrow> arrows = h.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(p.blockPosition()).inflate(4));
        h.assertTrue(arrows.size() == 1, "应射出 1 支箭，实际 " + arrows.size());
        double v = arrows.get(0).getDeltaMovement().length();
        arrows.get(0).discard();
        return v;
    }

    /** TC-T17-56：复合弓 18 刻 = 原版弓 20 刻；9 刻 = 原版 10 刻（箭速按力度 × 3，散布误差 2% 内）。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc56_composite_bow_draw(GameTestHelper h) {
        FakePlayer p = player(h, "eq_bow");
        double full = arrowSpeed(h, p, new ItemStack(GreekEquipment.COMPOSITE_BOW.get()), 18);
        double half = arrowSpeed(h, p, new ItemStack(GreekEquipment.COMPOSITE_BOW.get()), 9);
        double vanillaFull = 3.0 * DrawRules.power(20, 20);
        double vanillaHalf = 3.0 * DrawRules.power(10, 20);
        h.assertTrue(Math.abs(full - vanillaFull) / vanillaFull < 0.02, "18 刻箭速 " + full + "，原版满弓 " + vanillaFull);
        h.assertTrue(Math.abs(half - vanillaHalf) / vanillaHalf < 0.02, "9 刻箭速 " + half + "，原版 10 刻 " + vanillaHalf);
        h.succeed();
    }

    private static ItemStack craft(GameTestHelper h, Age age, ItemLike... grid) {
        FakePlayer p = player(h, "eq_craft");
        MythData.get(p).civ(Civilization.GREEK).setAge(age);
        CraftingMenu menu = new CraftingMenu(0, p.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(AT)));
        try {
            for (int i = 0; i < 9; i++) {
                menu.getSlot(i + 1).set(grid[i] == Items.AIR ? ItemStack.EMPTY : new ItemStack(grid[i]));
            }
            return menu.getSlot(0).getItem().copy();
        } finally {
            menu.removed(p);
            p.getInventory().clearContent();
        }
    }

    /** TC-T17-57 / 58：复合弓缺鬃弦不成；青铜装备远古不能合成、古典能。 */
    @GameTest(template = T, batch = BATCH)
    public static void tc57_recipes(GameTestHelper h) {
        h.setBlock(AT, Blocks.CRAFTING_TABLE);
        ItemLike B = io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.BRONZE_INGOT.get();
        ItemLike P = Items.OAK_PLANKS;
        ItemLike S = Items.STICK;
        ItemLike C = GreekWild.CENTAUR_SINEW.get();
        ItemLike T = Items.STRING;
        ItemLike A = Items.AIR;
        h.assertTrue(craft(h, Age.CLASSICAL, P, S, C, B, A, T, P, S, C).is(GreekEquipment.COMPOSITE_BOW.get()), "复合弓配方");
        h.assertTrue(craft(h, Age.CLASSICAL, P, S, T, B, A, T, P, S, T).isEmpty(), "鬃弦换成线不成");
        h.assertTrue(craft(h, Age.ARCHAIC, A, B, A, A, B, A, A, S, A).isEmpty(), "远古不能合成短剑");
        h.assertTrue(craft(h, Age.CLASSICAL, A, B, A, A, B, A, A, S, A).is(GreekEquipment.XIPHOS.get()), "古典能合成短剑");
        h.assertTrue(craft(h, Age.CLASSICAL, B, A, B, B, B, B, B, B, B).is(GreekEquipment.BRONZE_CHESTPLATE.get()), "青铜胸甲 8 锭");
        h.succeed();
    }
    // ───────────── GA-8：通用动作与真实攻击反馈 ─────────────

    /** TC-GA8-20：移除僵尸固有护甲以测面板 6；旁边目标放进原版横扫范围。 */
    @GameTest(template = T, batch = BATCH)
    public static void ga8_tc20_xiphos_damage_no_sweep(GameTestHelper h) {
        FakePlayer p = player(h, "ga8_damage");
        equip(p, EquipmentSlot.MAINHAND, GreekEquipment.XIPHOS.get());
        fullStrength(p);
        p.setOnGround(true);
        Zombie target = still(h.spawn(EntityType.ZOMBIE, 8, 2, 10));
        Zombie beside = still(h.spawn(EntityType.ZOMBIE, 9, 2, 10));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        float before = target.getHealth();
        float besideBefore = beside.getHealth();
        p.attack(target);
        close(h, 6, before - target.getHealth(), "短剑普通伤害");
        close(h, besideBefore, beside.getHealth(), "旁边僵尸不得被横扫");
        h.assertFalse(p.getMainHandItem().canPerformAction(ItemAbilities.SWORD_SWEEP), "不开放横扫能力");
        close(h, 1.8, p.getAttributeValue(Attributes.ATTACK_SPEED), "攻速不变");
        close(h, 375, p.getMainHandItem().getMaxDamage(), "耐久不变");
        h.killAllEntitiesOfClass(Zombie.class);
        h.succeed();
    }

    /** TC-GA8-21：监听真实 Level 播放事件，不能只测动作到声音的静态映射。 */
    @GameTest(template = T, batch = BATCH)
    public static void ga8_tc21_hit_sounds(GameTestHelper h) {
        FakePlayer p = player(h, "ga8_sounds");
        equip(p, EquipmentSlot.MAINHAND, GreekEquipment.XIPHOS.get());
        Zombie target = still(h.spawn(EntityType.ZOMBIE, 8, 2, 10));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        var played = new ArrayList<String>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == h.getLevel() && event.getSound() != null
                    && event.getPosition().distanceToSqr(target.position()) < 0.01) {
                played.add(event.getSound().value().getLocation().toString());
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            fullStrength(p);
            p.setOnGround(true);
            p.attack(target);
            h.assertTrue(played.stream().filter(id -> id.equals("mythic_age:" + WeaponFeedback.THRUST)).count() == 1,
                    "普通命中必须且只播放一次前刺音效：" + played);
            h.assertFalse(played.contains("mythic_age:" + WeaponFeedback.CLEAVE), "普通命中不播下劈");
            played.clear();
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            fullStrength(p);
            p.setOnGround(false);
            p.fallDistance = 1;
            p.attack(target);
            close(h, 9, target.getMaxHealth() - target.getHealth(), "原版暴击倍率仍是 1.5");
            h.assertTrue(played.stream().filter(id -> id.equals("mythic_age:" + WeaponFeedback.CLEAVE)).count() == 1,
                    "暴击必须且只播放一次下劈音效：" + played);
            h.assertFalse(played.contains("mythic_age:" + WeaponFeedback.THRUST), "暴击不播前刺");
            p.setOnGround(true);
            p.fallDistance = 0;
            p.resetAttackStrengthTicker();
            h.assertTrue(WeaponMotionState.current(p, p.getMainHandItem()) == WeaponMotion.CLEAVE,
                    "本次动作锁定，不随落地或冷却清零切回前刺");
            played.clear();
            target.setInvulnerable(true);
            fullStrength(p);
            p.attack(target);
            h.assertFalse(played.contains("mythic_age:" + WeaponFeedback.THRUST)
                    || played.contains("mythic_age:" + WeaponFeedback.CLEAVE), "伤害失败不能播放命中反馈");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.killAllEntitiesOfClass(Zombie.class);
        h.succeed();
    }

    /** TC-GA8-22：注册、sounds.json 字幕声明及 runData 生成的三语资源都存在。 */
    @GameTest(template = T, batch = BATCH)
    public static void ga8_tc22_sound_subtitles(GameTestHelper h) throws Exception {
        try (var in = EquipmentGameTests.class.getResourceAsStream("/assets/mythic_age/sounds.json")) {
            h.assertTrue(in != null, "sounds.json 必须存在");
            var sounds = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String id : List.of(WeaponFeedback.THRUST, WeaponFeedback.CLEAVE, WeaponFeedback.MISS)) {
                h.assertTrue(PowerRegistries.sound(id).getLocation().getPath().equals(id), "音效已注册：" + id);
                String key = sounds.getAsJsonObject(id).get("subtitle").getAsString();
                h.assertTrue(key.equals("subtitles.mythic_age." + id), "字幕键匹配");
                h.assertTrue(LangKeys.all().containsKey(key), "三语源登记");
                for (String locale : List.of("zh_cn", "en_us", "ja_jp")) {
                    try (var lang = EquipmentGameTests.class.getResourceAsStream("/assets/mythic_age/lang/" + locale + ".json")) {
                        h.assertTrue(lang != null, "生成语言文件存在：" + locale);
                        var values = JsonParser.parseReader(new InputStreamReader(lang, StandardCharsets.UTF_8)).getAsJsonObject();
                        h.assertTrue(values.has(key) && !values.get(key).getAsString().isBlank(), locale + " 有字幕：" + key);
                        h.assertTrue(values.get(key).getAsString().equals(LangKeys.all().get(key).get(locale)), "生成结果与登记一致");
                    }
                }
            }
        }
        h.succeed();
    }

    /** TC-GA8-23：长矛动作改前刺，伤害、速度、触及与盾后刺击倍率均不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void ga8_tc23_dory_motion_stats(GameTestHelper h) {
        FakePlayer p = player(h, "ga8_dory");
        double baseReach = p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        equip(p, EquipmentSlot.MAINHAND, GreekEquipment.DORY.get());
        var stats = ((MeleeWeaponItem) p.getMainHandItem().getItem()).stats();
        h.assertTrue(stats.motion() == WeaponMotion.THRUST, "长矛配置前刺");
        h.assertTrue(stats.critMotion() == WeaponMotion.SLASH, "缺省 critMotion 为 slash");
        close(h, 7, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "面板伤害不变");
        close(h, 1.1, p.getAttributeValue(Attributes.ATTACK_SPEED), "攻速不变");
        close(h, baseReach + 1, p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), "触及 +1 不变");
        close(h, 375, p.getMainHandItem().getMaxDamage(), "耐久不变");
        close(h, 0.6, stats.thrustMultiplier(), "盾后刺击倍率不变");
        Zombie target = still(h.spawn(EntityType.ZOMBIE, 8, 2, 10));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        fullStrength(p);
        p.setOnGround(true);
        float before = target.getHealth();
        p.attack(target);
        close(h, 7, before - target.getHealth(), "实际普通伤害不变");
        h.killAllEntitiesOfClass(Zombie.class);
        h.succeed();
    }
}
