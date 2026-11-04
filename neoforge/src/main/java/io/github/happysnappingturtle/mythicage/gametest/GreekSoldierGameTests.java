package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.temple.BrazierBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.village.soldier.BrazierSignal;
import io.github.happysnappingturtle.mythicage.content.greek.village.soldier.GreekSoldiers;
import io.github.happysnappingturtle.mythicage.content.greek.village.soldier.JavelinEntity;
import io.github.happysnappingturtle.mythicage.content.greek.village.soldier.PrayerGuard;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.core.village.soldier.DutyRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.KitRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.life.VillageLife;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierBrain;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierRecord;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierTrain;
import io.github.happysnappingturtle.mythicage.village.soldier.Soldiers;
import io.github.happysnappingturtle.mythicage.village.soldier.VillageLandmarks;
import io.github.happysnappingturtle.mythicage.village.soldier.VillageThreats;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * T20 阶段 ② 希腊两兵种（TC-T20-70～99）：用正式的希腊数据。场地 17 × 16 × 36，地面 y = 1，村钟 (8, 2, 2)，
 * 谷仓管事位 (2, 2, 4)；村庄已建范围覆盖整个场地。士兵是否在岗用 {@link Soldiers#forceStateForTest} 指定（不改世界时钟）。
 * 接管前先占住采石面、砾石滩、大理石采石点（余量 0），免得在附近放露头。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GreekSoldierGameTests {
    private static final String BATCH = "greek_soldier";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    static final String GREEK = "greek";
    static final String HOPLITE = "greek:hoplite", JAVELINEER = "greek:javelineer", LABORER = "greek:laborer";
    static final String JAVELIN = "mythic_age:javelin", ROUGH = "mythic_age:rough_javelin";

    private GreekSoldierGameTests() {
    }

    // ───────────── 搭建 ─────────────

    static VillageWorkGameTests.TV village(GameTestHelper h, Age stage, Map<String, List<Pos>> extra) {
        VillageWorkGameTests.floor(h);
        Map<String, List<Pos>> a = new LinkedHashMap<>(extra);
        a.putIfAbsent("storage_keeper", List.of(VillageWorkGameTests.abs(h, 2, 2, 4)));
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        VillageWorkGameTests.TV tv = VillageWorkGameTests.village(h, GREEK, a,
                new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 35));
        VillageEconomy.State s = state(h, tv);
        for (String k : List.of("stone", "gravel", "quarry")) {
            s.points.put(k + "_0", new ResourcePoint(k + "_0", k, VillageWorkGameTests.abs(h, 0, 1, 35), 0, 0, 0, 0));
        }
        s.soldierSeeded = true;
        VillageWorkGameTests.takeOver(h, tv);
        VillageRegistry reg = VillageRegistry.get(h.getLevel().getServer());
        reg.update(reg.byId(tv.id()).orElseThrow().withStage(stage));
        return new VillageWorkGameTests.TV(reg.byId(tv.id()).orElseThrow(), VillagePlans.get(h.getLevel().getServer()).byVillage(tv.id())
                .orElseThrow());
    }

    static VillageEconomy.State state(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageWorkGameTests.state(h, tv);
    }

    static VillageContext ctx(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
    }

    static VillageContext at(GameTestHelper h, VillageWorkGameTests.TV tv, int tod) {
        VillageContext c = ctx(h, tv);
        return new VillageContext(c.level(), c.village(), c.plan(), c.state(), c.data(), c.rt(), c.behavior(), c.now(), tod,
                c.data().schedule().segmentAt(tod), c.today(), c.date(), c.residents());
    }

    static ResidentEntity spawn(GameTestHelper h, VillageWorkGameTests.TV tv, String prof, int x, int z, boolean keeper) {
        BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
        return Residents.spawn(h.getLevel(), tv.village(), VillageData.civ(GREEK).orElseThrow(), prof, at, at, keeper).orElseThrow();
    }

    static List<ResidentEntity> laborers(GameTestHelper h, VillageWorkGameTests.TV tv, int n) {
        List<ResidentEntity> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ResidentEntity r = spawn(h, tv, LABORER, 1 + (i % 15), 33 - 2 * (i / 15), i == 0);
            r.setFemale(false);
            out.add(r);
        }
        return out;
    }

    static ResidentEntity enlist(GameTestHelper h, VillageWorkGameTests.TV tv, ResidentEntity r, String prof, SoldierProfile.Tier t) {
        VillageContext c = ctx(h, tv);
        SoldierProfile p = c.data().soldier(prof).orElseThrow();
        state(h, tv).stock.deposit(p.kits().item(t), 1);
        h.assertTrue(Soldiers.enlist(c, r, prof, p, t), "转岗成功");
        return r;
    }

    static void ammo(GameTestHelper h, VillageWorkGameTests.TV tv, ResidentEntity r, Map<String, Integer> a) {
        VillageEconomy.State s = state(h, tv);
        s.soldiers.put(r.getUUID(), s.soldiers.get(r.getUUID()).withAmmo(a));
    }

    static Husk husk(GameTestHelper h, double x, double z, boolean noAi) {
        Husk m = EntityType.HUSK.create(h.getLevel());
        m.moveTo(h.absoluteVec(new Vec3(x, 2, z)));
        m.setNoAi(noAi);
        m.setPersistenceRequired();
        h.getLevel().addFreshEntity(m);
        return m;
    }

    /** 希腊祭坛（朝南），登记进神坛表。 */
    static GlobalPos altar(GameTestHelper h, int x, int z) {
        BlockPos rel = new BlockPos(x, 2, z);
        h.setBlock(rel, GreekTemple.GREEK_ALTAR.get().defaultBlockState().setValue(GreekAltarBlock.FACING, Direction.SOUTH));
        BlockPos abs = h.absolutePos(rel);
        FakePlayer owner = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "gs_altar"));
        ShrineService.bindForTest(h.getLevel(), abs, owner.getUUID(), "gs_altar", Age.ARCHAIC, Direction.SOUTH);
        return GlobalPos.of(h.getLevel().dimension(), abs);
    }

    static void cleanup(GameTestHelper h, VillageWorkGameTests.TV tv, Object... extra) {
        for (Object e : extra) {
            if (e instanceof Entity en && !en.isRemoved()) {
                en.discard();
            } else if (e instanceof GlobalPos g) {
                ShrineRegistry.get(h.getLevel().getServer()).remove(g);
                h.getLevel().removeBlock(g.pos(), false);
            }
        }
        Soldiers.forceStateForTest(tv.id(), null);
        Soldiers.clearForTest(tv.id());
        BrazierSignal.clearForTest(tv.id());
        JavelinEntity.clearForTest(tv.id());
        VillageLandmarks.invalidate(tv.id());
        VillageThreats.clearForTest(tv.id());
        for (JavelinEntity j : h.getLevel().getEntitiesOfClass(JavelinEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(BlockPos.ZERO))
                .inflate(48), j -> tv.id().equals(j.ammoVillage()))) {
            j.discard();
        }
        VillageWorkGameTests.cleanup(h, tv);
    }

    // ───────────── 初始卫兵、补发（TC-70、71） ─────────────

    /** TC-T20-70：生成村庄有 1 名盾矛卫兵，穿简易兵装；S0 库存没有被扣兵装。 */
    @GameTest(template = T, batch = BATCH)
    public static void initial_hoplite(GameTestHelper h) {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        List<Pos> homes = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            homes.add(VillageWorkGameTests.abs(h, 2 + 2 * i, 2, 30));
        }
        a.put("home", homes);
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, a);
        try {
            int n = Residents.spawnInitial(h.getLevel(), tv.village(), tv.plan(), VillageData.civ(GREEK).orElseThrow());
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(n == 6, "生成 6 人");
            List<SoldierRecord> soldiers = new ArrayList<>(s.soldiers.values());
            h.assertTrue(soldiers.size() == 1 && soldiers.get(0).profession().equals(HOPLITE)
                    && soldiers.get(0).tier() == SoldierProfile.Tier.SIMPLE, "1 名盾矛卫兵、简易兵装：" + soldiers);
            ResidentEntity g = VillageScheduler.loaded(tv.id()).stream().filter(ResidentEntity::isSoldier).findFirst().orElseThrow();
            h.assertTrue(g.getMaxHealth() == 28 && g.getArmorValue() == 4, "生命 28、护甲 6 − 2：" + g.getMaxHealth() + "/" + g.getArmorValue());
            h.assertTrue(s.stock.count("mythic_age:hoplite_kit_simple") == 0, "S0 库存没有扣兵装");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-71：已有存档的生成村庄（人口 ≥ 4、没有士兵）第一次处理时补发 1 套简易兵装，当天就转出一名卫兵；只补一次。 */
    @GameTest(template = T, batch = BATCH)
    public static void seed_kit_once(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        try {
            laborers(h, tv, 6);
            VillageEconomy.State s = state(h, tv);
            s.soldierSeeded = false;
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(s.soldierSeeded, "记下补发过");
            h.assertTrue(s.soldiers.size() == 1 && s.soldiers.values().iterator().next().tier() == SoldierProfile.Tier.SIMPLE,
                    "补发的那套当天就穿上了");
            h.assertTrue(s.log.stream().anyMatch(l -> l.key().equals("mythic_age.village.log.kit_seeded")), "村务记录");
            int before = s.stock.count("mythic_age:hoplite_kit_simple");
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(s.stock.count("mythic_age:hoplite_kit_simple") == before, "不再补");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    // ───────────── 举盾（TC-72～74） ─────────────

    /** 一名朝 +Z 站着、举盾的卫兵（不跑 AI，朝向不变）。 */
    static ResidentEntity shieldGuard(GameTestHelper h, VillageWorkGameTests.TV tv) {
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity g = enlist(h, tv, ls.get(1), HOPLITE, SoldierProfile.Tier.FULL);
        g.moveTo(h.absoluteVec(new Vec3(8.5, 2, 10.5)));
        g.setNoAi(true);
        g.setYRot(0);
        g.yBodyRot = 0;
        Soldiers.forceStateForTest(tv.id(), DutyRules.State.ON_DUTY);
        Soldiers.plan(ctx(h, tv));
        g.soldierBrain().blockForTest(true);
        return g;
    }

    /** TC-T20-72、73：举盾时正前方的伤害 ×0.4，背后全额；举盾 40 刻自动放下、15 刻内不再举（core TC-39e 已覆盖时序）。 */
    @GameTest(template = T, batch = BATCH)
    public static void shield_front_and_back(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        Husk front = husk(h, 8.5, 12.5, true);
        Husk back = husk(h, 8.5, 8.5, true);
        try {
            ResidentEntity g = shieldGuard(h, tv);
            h.assertTrue(g.soldierBrain().blockingEffective(), "举盾生效");
            float hp = g.getHealth();
            g.hurt(h.getLevel().damageSources().mobAttack(front), 10);
            float frontLoss = hp - g.getHealth();
            g.invulnerableTime = 0;
            hp = g.getHealth();
            g.hurt(h.getLevel().damageSources().mobAttack(back), 10);
            float backLoss = hp - g.getHealth();
            // 盾的倍率在护甲之前：正前方 10 × 0.4 = 4 再过护甲，背后 10 直接过护甲
            float armor = g.getArmorValue();
            float tough = (float) g.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
            DamageSource src = h.getLevel().damageSources().mobAttack(front);
            float wantFront = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(g, 4, src, armor, tough);
            float wantBack = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(g, 10, src, armor, tough);
            h.assertTrue(Math.abs(frontLoss - wantFront) < 0.01F && Math.abs(backLoss - wantBack) < 0.01F,
                    "正前 ×0.4、背后全额（都再过护甲）：" + frontLoss + "/" + wantFront + "，" + backLoss + "/" + wantBack);
        } finally {
            cleanup(h, tv, front, back);
        }
        h.succeed();
    }

    /** TC-T20-74：骷髅从正前方射箭：伤害 ×0.4，兵装耐久 −1。 */
    @GameTest(template = T, batch = BATCH)
    public static void shield_blocks_arrow_and_wears(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        Skeleton sk = EntityType.SKELETON.create(h.getLevel());
        sk.moveTo(h.absoluteVec(new Vec3(8.5, 2, 18.5)));
        sk.setNoAi(true);
        h.getLevel().addFreshEntity(sk);
        Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
        arrow.moveTo(h.absoluteVec(new Vec3(8.5, 3, 12)));
        try {
            ResidentEntity g = shieldGuard(h, tv);
            float hp = g.getHealth();
            DamageSource src = h.getLevel().damageSources().arrow(arrow, sk);
            g.hurt(src, 10);
            float loss = hp - g.getHealth();
            h.assertTrue(loss > 0 && loss < 5, "挡住了大半：" + loss);
            h.assertTrue(state(h, tv).soldiers.get(g.getUUID()).wear() == 1, "兵装耐久 −1");
        } finally {
            cleanup(h, tv, sk, arrow);
        }
        h.succeed();
    }

    // ───────────── 护祷拦截（TC-75～80） ─────────────

    /** TC-T20-75：扫描范围内祈祷的居民被盯上：在岗卫兵跑到两者连线上（离居民 ≤ 1.5 格）、面向威胁举盾。 */
    @GameTest(template = T, batch = "greek_soldier_guard_a", timeoutTicks = 400)
    public static void prayer_guard_intercepts(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        GlobalPos alt = altar(h, 8, 8);
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity g = enlist(h, tv, ls.get(1), HOPLITE, SoldierProfile.Tier.FULL);
        // 卫兵从居民侧面出发：从敌人那头绕过来时，路过敌人身边会顺手刺它；从居民正后方出发要挤过居民身子，偶尔卡住（偶发失败）
        g.moveTo(h.absoluteVec(new Vec3(12.5, 2, 12.5)));
        ResidentEntity ward = ls.get(2);
        ward.moveTo(h.absoluteVec(new Vec3(8.5, 2, 12.5)));
        ward.setNoAi(true);
        Husk m = husk(h, 8.5, 21.5, true);
        m.setTarget(ward);
        Soldiers.forceStateForTest(tv.id(), DutyRules.State.ON_DUTY);
        h.onEachTick(() -> PrayerGuard.prayingForTest(ward.getUUID(), h.getLevel().getGameTime()));
        h.succeedWhen(() -> {
            h.assertTrue(g.soldierBrain().ward() == ward, "在护他：" + g.soldierBrain().debug() + " 结束原因 "
                    + PrayerGuard.reasonForTest(g.getUUID()));
            double dz = g.getZ() - ward.getZ();
            h.assertTrue(dz > 0.5 && dz < 2.0 && Math.abs(g.getX() - ward.getX()) < 1.0, "站在居民与威胁之间：" + h.relativeVec(g.position())
                    + " 居民 " + h.relativeVec(ward.position()) + " 威胁 " + h.relativeVec(m.position()) + " 导航完 " + g.getNavigation().isDone()
                    + " 动作 " + g.anim() + " | " + g.soldierBrain().debug());
            h.assertTrue(g.soldierBrain().blocking(), "举盾");
            cleanup(h, tv, m, alt);
        });
    }

    /** TC-T20-76：居民在扫描范围外：不拦截。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void prayer_guard_outside_area(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        GlobalPos alt = altar(h, 8, 26);
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity g = enlist(h, tv, ls.get(1), HOPLITE, SoldierProfile.Tier.FULL);
        // 卫兵从居民这一侧出发（从敌人那头绕过来时，路过敌人身边会顺手刺它）
        g.moveTo(h.absoluteVec(new Vec3(8.5, 2, 4.5)));
        ResidentEntity ward = ls.get(2);
        ward.moveTo(h.absoluteVec(new Vec3(8.5, 2, 12.5)));
        ward.setNoAi(true);
        Husk m = husk(h, 8.5, 24.5, true);
        m.setTarget(ward);
        Soldiers.forceStateForTest(tv.id(), DutyRules.State.ON_DUTY);
        h.onEachTick(() -> PrayerGuard.prayingForTest(ward.getUUID(), h.getLevel().getGameTime()));
        h.runAfterDelay(80, () -> {
            h.assertTrue(g.soldierBrain().ward() == null, "范围外不拦截：" + g.soldierBrain().debug());
            cleanup(h, tv, m, alt);
            h.succeed();
        });
    }

    /** TC-T20-77：两名居民同时被盯上、只有 1 名卫兵：只护 1 名。 */
    @GameTest(template = T, batch = "greek_soldier_guard_b", timeoutTicks = 300)
    public static void prayer_guard_one_at_a_time(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        GlobalPos alt = altar(h, 8, 8);
        List<ResidentEntity> ls = laborers(h, tv, 6);
        ResidentEntity g = enlist(h, tv, ls.get(1), HOPLITE, SoldierProfile.Tier.FULL);
        g.moveTo(h.absoluteVec(new Vec3(8.5, 2, 4.5)));
        ResidentEntity w1 = ls.get(2);
        ResidentEntity w2 = ls.get(3);
        w1.moveTo(h.absoluteVec(new Vec3(5.5, 2, 12.5)));
        w2.moveTo(h.absoluteVec(new Vec3(11.5, 2, 12.5)));
        w1.setNoAi(true);
        w2.setNoAi(true);
        Husk m1 = husk(h, 5.5, 21.5, true);
        Husk m2 = husk(h, 11.5, 21.5, true);
        m1.setTarget(w1);
        m2.setTarget(w2);
        Soldiers.forceStateForTest(tv.id(), DutyRules.State.ON_DUTY);
        h.onEachTick(() -> {
            PrayerGuard.prayingForTest(w1.getUUID(), h.getLevel().getGameTime());
            PrayerGuard.prayingForTest(w2.getUUID(), h.getLevel().getGameTime());
        });
        h.succeedWhen(() -> {
            h.assertTrue(g.soldierBrain().ward() != null, "在护一名");
            boolean a = SoldierBrain.guardOf(w1.getUUID()).isPresent();
            boolean b = SoldierBrain.guardOf(w2.getUUID()).isPresent();
            h.assertTrue(a != b, "只护 1 名");
            cleanup(h, tv, m1, m2, alt);
        });
    }

    /** TC-T20-78：拦截中，骷髅从卫兵正前方射被护的居民：×0.4；射手在背后不挡。 */
    @GameTest(template = T, batch = BATCH)
    public static void prayer_guard_covers_projectiles(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        Skeleton front = EntityType.SKELETON.create(h.getLevel());
        front.moveTo(h.absoluteVec(new Vec3(8.5, 2, 20.5)));
        Skeleton back = EntityType.SKELETON.create(h.getLevel());
        back.moveTo(h.absoluteVec(new Vec3(8.5, 2, 2.5)));
        Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
        arrow.moveTo(h.absoluteVec(new Vec3(8.5, 3, 9)));
        Husk m = husk(h, 8.5, 18.5, true);
        try {
            ResidentEntity g = shieldGuard(h, tv);
            ResidentEntity ward = VillageScheduler.loaded(tv.id()).stream().filter(r -> !r.isSoldier()).findFirst().orElseThrow();
            ward.moveTo(h.absoluteVec(new Vec3(8.5, 2, 9)));
            ward.setNoAi(true);
            g.soldierBrain().guard(ward, m);
            g.soldierBrain().blockForTest(true);
            float f = Soldiers.wardMultiplier(ward, h.getLevel().damageSources().arrow(arrow, front));
            float b = Soldiers.wardMultiplier(ward, h.getLevel().damageSources().arrow(arrow, back));
            h.assertTrue(Math.abs(f - 0.4F) < 1e-4, "正前方射来 ×0.4：" + f);
            h.assertTrue(b == 1.0F, "背后射来不挡：" + b);
        } finally {
            cleanup(h, tv, front, back, arrow, m);
        }
        h.succeed();
    }

    /** TC-T20-79：拦截中居民走出扫描范围：卫兵立即恢复普通值勤。 */
    @GameTest(template = T, batch = "greek_soldier_guard_c", timeoutTicks = 400)
    public static void prayer_guard_ends_outside(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.ARCHAIC, Map.of());
        GlobalPos alt = altar(h, 8, 8);
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity g = enlist(h, tv, ls.get(1), HOPLITE, SoldierProfile.Tier.FULL);
        // 卫兵从居民这一侧出发（从敌人那头绕过来时，路过敌人身边会顺手刺它）
        g.moveTo(h.absoluteVec(new Vec3(8.5, 2, 4.5)));
        ResidentEntity ward = ls.get(2);
        ward.moveTo(h.absoluteVec(new Vec3(8.5, 2, 12.5)));
        ward.setNoAi(true);
        Husk m = husk(h, 8.5, 21.5, true);
        m.setTarget(ward);
        Soldiers.forceStateForTest(tv.id(), DutyRules.State.ON_DUTY);
        boolean[] moved = {false};
        h.onEachTick(() -> {
            PrayerGuard.prayingForTest(ward.getUUID(), h.getLevel().getGameTime());
            if (!moved[0] && g.soldierBrain().ward() == ward) {
                moved[0] = true;
                ward.moveTo(h.absoluteVec(new Vec3(8.5, 2, 4.5)));
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(moved[0], "先护上");
            h.assertTrue(g.soldierBrain().ward() == null, "出范围就不护了");
            cleanup(h, tv, m, alt);
        });
    }

    // ───────────── 投矛（TC-81～87、99） ─────────────

    static ResidentEntity javelineer(GameTestHelper h, VillageWorkGameTests.TV tv, List<ResidentEntity> ls, int x, int z) {
        ResidentEntity j = enlist(h, tv, ls.get(1), JAVELINEER, SoldierProfile.Tier.FULL);
        j.moveTo(h.absoluteVec(new Vec3(x + 0.5, 2, z + 0.5)));
        Soldiers.forceStateForTest(tv.id(), DutyRules.State.ON_DUTY);
        return j;
    }

    /** TC-T20-81：8 格外的敌人：前摇后投出、命中造成伤害；身上投矛 6 → 5。 */
    @GameTest(template = T, batch = "greek_soldier_throw_a", timeoutTicks = 400)
    public static void javelin_throw_hits(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity j = javelineer(h, tv, ls, 8, 4);
        ammo(h, tv, j, Map.of(JAVELIN, 6));
        Husk m = husk(h, 8.5, 12.5, true);
        h.succeedWhen(() -> {
            h.assertTrue(m.getHealth() < m.getMaxHealth(), "命中：" + j.soldierBrain().debug());
            h.assertTrue(state(h, tv).soldiers.get(j.getUUID()).ammo().getOrDefault(JAVELIN, 0) <= 5, "用掉了投矛");
            cleanup(h, tv, m);
        });
    }

    /** TC-T20-82：敌人在 3 格内：先后撤拉开到 ≥ 4 格。 */
    @GameTest(template = T, batch = "greek_soldier_throw_b", timeoutTicks = 300)
    public static void javelin_backs_off_when_close(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity j = javelineer(h, tv, ls, 8, 10);
        ammo(h, tv, j, Map.of(JAVELIN, 6));
        Husk m = husk(h, 8.5, 12.5, true);
        m.setInvulnerable(true);
        h.succeedWhen(() -> {
            h.assertTrue(j.soldierBrain().target() == m, "盯上了");
            h.assertTrue(j.distanceTo(m) >= 4, "拉开距离：" + j.distanceTo(m) + "；" + j.soldierBrain().debug());
            cleanup(h, tv, m);
        });
    }

    /** TC-GA1-15：尸壳追着走（约 2.3 米/秒）：投矛兵按跑步速度后撤，仍能拉开到 ≥ 4 格。 */
    @GameTest(template = T, batch = "greek_soldier_throw_b", timeoutTicks = 300)
    public static void javelin_outruns_chasing_husk(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity j = javelineer(h, tv, ls, 8, 10);
        ammo(h, tv, j, Map.of(JAVELIN, 6));
        Husk m = husk(h, 8.5, 12.5, false);
        m.setInvulnerable(true);
        m.setTarget(j);
        h.succeedWhen(() -> {
            h.assertTrue(j.soldierBrain().target() == m, "盯上了");
            h.assertTrue(j.distanceTo(m) >= 4, "拉开距离：" + j.distanceTo(m) + "；" + j.soldierBrain().debug());
            cleanup(h, tv, m);
        });
    }

    /** TC-T20-83：居民站在投矛线上：不投、换位；居民不受伤。 */
    @GameTest(template = T, batch = "greek_soldier_throw_c", timeoutTicks = 300)
    public static void javelin_line_blocked(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity j = javelineer(h, tv, ls, 8, 4);
        ammo(h, tv, j, Map.of(JAVELIN, 6));
        ResidentEntity ward = ls.get(2);
        ward.moveTo(h.absoluteVec(new Vec3(8.5, 2, 8.5)));
        ward.setNoAi(true);
        for (int z = 3; z <= 13; z++) {
            h.setBlock(new BlockPos(7, 2, z), Blocks.GLASS);
            h.setBlock(new BlockPos(9, 2, z), Blocks.GLASS);
            h.setBlock(new BlockPos(7, 3, z), Blocks.GLASS);
            h.setBlock(new BlockPos(9, 3, z), Blocks.GLASS);
        }
        h.setBlock(new BlockPos(8, 2, 3), Blocks.GLASS);
        h.setBlock(new BlockPos(8, 3, 3), Blocks.GLASS);
        Husk m = husk(h, 8.5, 12.5, true);
        float hp = ward.getHealth();
        h.succeedWhen(() -> {
            h.assertTrue(ward.getHealth() >= hp, "居民没受伤");
            h.assertTrue(j.soldierBrain().withholds() >= 1, "挡线时没投：" + j.soldierBrain().debug());
            cleanup(h, tv, m);
        });
    }

    /** TC-T20-84：身上没有投矛、村库有：不在警戒时回谷仓补到 6，先拿完好的；只有粗投矛时伤害 −1。 */
    @GameTest(template = T, batch = "greek_soldier_throw_d", timeoutTicks = 400)
    public static void javelin_resupply(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity j = javelineer(h, tv, ls, 8, 12);
        VillageEconomy.State s = state(h, tv);
        s.stock.deposit(JAVELIN, 4);
        s.stock.deposit(ROUGH, 10);
        h.succeedWhen(() -> {
            Map<String, Integer> a = s.soldiers.get(j.getUUID()).ammo();
            h.assertTrue(a.getOrDefault(JAVELIN, 0) == 4 && a.getOrDefault(ROUGH, 0) == 2, "补到 6：先 4 支完好、再 2 支粗的：" + a);
            h.assertTrue(s.stock.count(JAVELIN) == 0 && s.stock.count(ROUGH) == 8, "村库扣了");
            JavelinEntity rough = GreekSoldiers.JAVELIN.get().create(h.getLevel());
            SoldierProfile p = VillageData.civ(GREEK).orElseThrow().soldier(JAVELINEER).orElseThrow();
            h.assertTrue(p.damage(SoldierProfile.Tier.FULL) + p.ammo().items().get(1).damage() == 4, "粗投矛伤害 5 − 1");
            rough.discard();
            cleanup(h, tv);
        });
    }

    /** 一支插在 (x, 2, z) 的投矛。 */
    static JavelinEntity landed(GameTestHelper h, VillageWorkGameTests.TV tv, double x, double z) {
        JavelinEntity e = GreekSoldiers.JAVELIN.get().create(h.getLevel());
        e.moveTo(h.absoluteVec(new Vec3(x, 2.6, z)));
        e.setDeltaMovement(0, -0.5, 0);
        e.configure(tv.id(), JAVELIN, 5, 0.05, 0.8, 1200);
        h.getLevel().addFreshEntity(e);
        return e;
    }

    /** TC-T20-85：回收：村内落地的投矛警戒结束后捡回，按 UUID 定种子 60% 回库；村外的落地即损耗。 */
    @GameTest(template = T, batch = "greek_soldier_throw_e", timeoutTicks = 600)
    public static void javelin_recovery(GameTestHelper h) {
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        // 只让场地前半算村里：后半落的投矛是村外
        VillagePlans plans = VillagePlans.get(h.getLevel().getServer());
        var info = tv.plan().info();
        plans.updateInfo(tv.id(), new io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece.Info(info.civ(), info.patron(),
                info.layout(), info.layoutVersion(), info.templateSet(), info.center(), info.anchors(),
                List.of(new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 20)), info.plots()));
        List<ResidentEntity> ls = laborers(h, tv, 5);
        ResidentEntity j = javelineer(h, tv, ls, 8, 4);
        ammo(h, tv, j, Map.of(JAVELIN, 6));
        List<JavelinEntity> in = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            in.add(landed(h, tv, 3.5 + 2 * i, 10.5));
        }
        JavelinEntity out = landed(h, tv, 8.5, 31.5);
        int[] want = {0};
        in.forEach(e -> want[0] += Soldiers.intact(e.getUUID(), 0.6) ? 1 : 0);
        h.succeedWhen(() -> {
            h.assertTrue(out.isRemoved(), "村外那支落地即损耗");
            h.assertTrue(in.stream().allMatch(Entity::isRemoved), "都捡了：" + j.soldierBrain().debug());
            h.assertTrue(state(h, tv).stock.count(JAVELIN) == want[0], "按种子回库 " + want[0] + "：" + state(h, tv).stock.count(JAVELIN));
            cleanup(h, tv);
        });
    }

    /** TC-T20-86、87：投矛存档往返不变；玩家碰到也捡不起来。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void javelin_persists_and_not_pickable(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        JavelinEntity e = landed(h, tv, 8.5, 10.5);
        h.runAfterDelay(10, () -> {
            h.assertTrue(e.landed(), "落地了");
            CompoundTag tag = new CompoundTag();
            e.saveWithoutId(tag);
            JavelinEntity copy = GreekSoldiers.JAVELIN.get().create(h.getLevel());
            copy.load(tag);
            h.assertTrue(copy.landed() && tv.id().equals(copy.ammoVillage()) && copy.ammoItem().equals(JAVELIN), "读回一致");
            FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "gs_touch"));
            int before = p.getInventory().countItem(io.github.happysnappingturtle.mythicage.content.greek.village.soldier.GreekSoldierItems.JAVELIN.get());
            e.playerTouch(p);
            h.assertTrue(!e.isRemoved() && p.getInventory().countItem(
                    io.github.happysnappingturtle.mythicage.content.greek.village.soldier.GreekSoldierItems.JAVELIN.get()) == before, "捡不起来");
            copy.discard();
            cleanup(h, tv, e);
            h.succeed();
        });
    }

    /** TC-T20-99：同一村同时插地的投矛最多 12 支，多出的最早那支移除。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void javelin_village_cap(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        List<JavelinEntity> all = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            all.add(landed(h, tv, 1.5 + i, 14.5));
        }
        h.runAfterDelay(5, () -> {
            h.assertTrue(all.get(0).isRemoved(), "最早那支移除了");
            h.assertTrue(all.stream().filter(e -> !e.isRemoved()).count() == 12, "剩 12 支");
            cleanup(h, tv);
            h.succeed();
        });
    }

    // ───────────── 圣火烽号（TC-67、88～91） ─────────────

    /** 点着的圣火盆。 */
    static void brazier(GameTestHelper h, int x, int z, boolean lit) {
        h.setBlock(new BlockPos(x, 2, z), GreekTemple.GREEK_BRAZIER.get().defaultBlockState().setValue(BrazierBlock.LIT, lit));
    }

    /** TC-T20-67、88：警戒开始，投矛兵跑到 24 格内点着的圣火盆扬火；全村在岗士兵得知威胁（卫兵去接敌）。地标索引找得到模板里的圣火盆。 */
    @GameTest(template = T, batch = "greek_soldier_signal_a", timeoutTicks = 600)
    public static void brazier_signal_rallies(GameTestHelper h) {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        a.put("entrance", List.of(VillageWorkGameTests.abs(h, 8, 2, 30)));
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, a);
        brazier(h, 14, 8, true);
        h.assertTrue(VillageLandmarks.find(h.getLevel(), tv.id(), BrazierSignal.LANDMARK).contains(h.absolutePos(new BlockPos(14, 2, 8))),
                "地标索引找到了圣火盆");
        List<ResidentEntity> ls = laborers(h, tv, 8);
        ResidentEntity j = javelineer(h, tv, ls, 8, 4);
        ammo(h, tv, j, Map.of(JAVELIN, 6));
        ResidentEntity hop = enlist(h, tv, ls.get(2), HOPLITE, SoldierProfile.Tier.FULL);
        // 卫兵守村口（第一个岗位）；敌人离村钟 13 格（投矛兵能看见），离村口 17 格（卫兵自己不会接敌）
        hop.moveTo(h.absoluteVec(new Vec3(8.5, 2, 30.5)));
        Husk m = husk(h, 2.5, 13.5, true);
        m.setInvulnerable(true);
        h.succeedWhen(() -> {
            long[] done = BrazierSignal.lastForTest(tv.id());
            h.assertTrue(done != null && done[1] == 1, "扬了火：" + j.soldierBrain().debug());
            h.assertTrue(hop.soldierBrain().target() == m, "卫兵得知威胁：" + hop.soldierBrain().debug());
            cleanup(h, tv, m);
        });
    }

    /** TC-T20-89：同一场警戒里不再扬第二次。 */
    @GameTest(template = T, batch = BATCH)
    public static void brazier_signal_once_per_alert(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        brazier(h, 14, 8, true);
        try {
            List<ResidentEntity> ls = laborers(h, tv, 6);
            ResidentEntity j = javelineer(h, tv, ls, 8, 4);
            Soldiers.plan(ctx(h, tv));
            Husk m = husk(h, 8.5, 12.5, true);
            long serial = VillageScheduler.raiseAlarm(tv.id(), h.getLevel().getGameTime());
            Soldiers.broadcastAlert(h.getLevel(), tv.id(), serial, m);
            h.assertTrue(BrazierSignal.taskedForTest(j.getUUID()), "第一次：派了扬火");
            BrazierSignal.clearForTest(tv.id());
            Soldiers.broadcastAlert(h.getLevel(), tv.id(), serial, m);
            h.assertTrue(!BrazierSignal.taskedForTest(j.getUUID()), "同一场不再派");
            m.discard();
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-90：圣火盆没点着：退化为呼喊。 */
    @GameTest(template = T, batch = BATCH)
    public static void brazier_unlit_shouts(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        brazier(h, 14, 8, false);
        try {
            List<ResidentEntity> ls = laborers(h, tv, 6);
            ResidentEntity j = javelineer(h, tv, ls, 8, 4);
            Soldiers.plan(ctx(h, tv));
            Husk m = husk(h, 8.5, 12.5, true);
            long serial = VillageScheduler.raiseAlarm(tv.id(), h.getLevel().getGameTime());
            Soldiers.broadcastAlert(h.getLevel(), tv.id(), serial, m);
            h.assertTrue(!BrazierSignal.taskedForTest(j.getUUID()), "没有点着的圣火盆就不跑");
            long[] done = BrazierSignal.lastForTest(tv.id());
            h.assertTrue(done != null && done[1] == 0, "呼喊");
            m.discard();
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-91：跑去途中圣火盆被浇灭：到了之后读到没点着，退化为呼喊。 */
    @GameTest(template = T, batch = "greek_soldier_signal_b", timeoutTicks = 300)
    public static void brazier_doused_on_the_way(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        brazier(h, 14, 20, true);
        List<ResidentEntity> ls = laborers(h, tv, 6);
        ResidentEntity j = javelineer(h, tv, ls, 8, 4);
        Soldiers.plan(ctx(h, tv));
        Husk m = husk(h, 2.5, 12.5, true);
        m.setInvulnerable(true);
        long serial = VillageScheduler.raiseAlarm(tv.id(), h.getLevel().getGameTime());
        Soldiers.broadcastAlert(h.getLevel(), tv.id(), serial, m);
        h.assertTrue(BrazierSignal.taskedForTest(j.getUUID()), "派了扬火");
        brazier(h, 14, 20, false);
        h.succeedWhen(() -> {
            long[] done = BrazierSignal.lastForTest(tv.id());
            h.assertTrue(done != null && done[1] == 0, "浇灭了就呼喊");
            cleanup(h, tv, m);
        });
    }

    // ───────────── 配方、上限、训练、节庆（TC-92～97） ─────────────

    /** TC-T20-92、93：兵装、投矛、修复的配方数据；修复是完整原材料的一半。 */
    @GameTest(template = T, batch = BATCH)
    public static void soldier_recipes(GameTestHelper h) {
        CivVillageData d = VillageData.civ(GREEK).orElseThrow();
        VillageRecipe full = d.recipes().get("greek:kit_hoplite");
        VillageRecipe simple = d.recipes().get("greek:kit_hoplite_simple");
        VillageRecipe repair = d.recipes().get("greek:repair_hoplite_kit");
        VillageRecipe rough = d.recipes().get("greek:rough_javelins");
        h.assertTrue(full != null && full.professions().contains("greek:bronzesmith") && full.stage().equals("heroic"), "完整兵装：青铜工匠、英雄");
        h.assertTrue(simple != null && simple.inputs().containsKey("minecraft:cobblestone") && simple.professions().contains(LABORER),
                "简易兵装用圆石（D5）");
        h.assertTrue(rough != null && rough.inputs().containsKey("minecraft:flint"), "粗投矛用燧石（D5）");
        Map<String, Integer> half = KitRules.repairCost(full.inputs());
        Map<String, Integer> rin = new LinkedHashMap<>(repair.inputs());
        rin.remove("mythic_age:hoplite_kit_broken");
        h.assertTrue(rin.equals(half), "修复 = 一半原材料：" + rin + " vs " + half);
        h.assertTrue(full.stockTarget() == 1 && d.recipes().get("greek:javelins").stockTarget() == 12, "备货目标");
        h.succeed();
    }

    /** TC-T20-94：古典人口 12、上限 4：依次转出 2 名卫兵、1 名投矛兵…；远古时不出现投矛兵。 */
    @GameTest(template = T, batch = BATCH)
    public static void classical_mix(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.CLASSICAL, Map.of());
        try {
            laborers(h, tv, 12);
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(Soldiers.cap(ctx(h, tv)) == 4, "上限 3 + 古典 1：" + Soldiers.cap(ctx(h, tv)));
            s.stock.deposit("mythic_age:hoplite_kit_simple", 3);
            s.stock.deposit("mythic_age:javelineer_kit_simple", 3);
            for (int i = 0; i < 4; i++) {
                Soldiers.dayStart(ctx(h, tv));
            }
            Map<String, Integer> n = new LinkedHashMap<>();
            s.soldiers.values().forEach(r -> n.merge(r.profession(), 1, Integer::sum));
            h.assertTrue(n.getOrDefault(HOPLITE, 0) == 2 && n.getOrDefault(JAVELINEER, 0) == 2, "2 卫兵 + 2 投矛兵：" + n);
        } finally {
            cleanup(h, tv);
        }
        VillageWorkGameTests.TV tv2 = village(h, Age.ARCHAIC, Map.of());
        try {
            laborers(h, tv2, 12);
            state(h, tv2).stock.deposit("mythic_age:hoplite_kit_simple", 3);
            state(h, tv2).stock.deposit("mythic_age:javelineer_kit_simple", 3);
            for (int i = 0; i < 4; i++) {
                Soldiers.dayStart(ctx(h, tv2));
            }
            h.assertTrue(state(h, tv2).soldiers.values().stream().allMatch(r -> r.profession().equals(HOPLITE))
                    && state(h, tv2).soldiers.size() == 3, "远古只有卫兵（上限 3）：" + state(h, tv2).soldiers.values());
        } finally {
            cleanup(h, tv2);
        }
        h.succeed();
    }

    /** TC-T20-95：有训练锚点时，工作段的偶数段排训练单（只给兵种）；奇数段不排；没有训练锚点不排。 */
    @GameTest(template = T, batch = BATCH)
    public static void training_jobs(GameTestHelper h) {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        a.put("training", List.of(VillageWorkGameTests.abs(h, 12, 2, 26)));
        VillageWorkGameTests.TV tv = village(h, Age.HEROIC, a);
        try {
            List<Job> jobs = JobType.REGISTRY.get(SoldierTrain.ID).candidates(at(h, tv, 1000));
            h.assertTrue(jobs.size() == 1 && jobs.get(0).allows(HOPLITE) && !jobs.get(0).allows(LABORER), "训练单只给兵种：" + jobs);
            h.assertTrue(JobType.REGISTRY.get(SoldierTrain.ID).candidates(at(h, tv, 3000)).isEmpty(), "奇数段不训练（帮农务）");
        } finally {
            cleanup(h, tv);
        }
        VillageWorkGameTests.TV none = village(h, Age.HEROIC, Map.of());
        try {
            h.assertTrue(JobType.REGISTRY.get(SoldierTrain.ID).candidates(at(h, none, 1000)).isEmpty(), "没有训练场不训练");
        } finally {
            cleanup(h, none);
        }
        h.succeed();
    }

    /** TC-T20-96、97：城邦节竞技段，有训练锚点、2 名不值勤士兵：两人角力；值勤的那名不参加。 */
    @GameTest(template = T, batch = BATCH)
    public static void festival_wrestling(GameTestHelper h) {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        a.put("training", List.of(VillageWorkGameTests.abs(h, 12, 2, 26)));
        VillageWorkGameTests.TV tv = village(h, Age.HEROIC, a);
        try {
            List<ResidentEntity> ls = laborers(h, tv, 10);
            ResidentEntity s1 = enlist(h, tv, ls.get(1), HOPLITE, SoldierProfile.Tier.FULL);
            ResidentEntity s2 = enlist(h, tv, ls.get(2), HOPLITE, SoldierProfile.Tier.FULL);
            ResidentEntity s3 = enlist(h, tv, ls.get(3), HOPLITE, SoldierProfile.Tier.FULL);
            VillageContext c0 = ctx(h, tv);
            state(h, tv).calendarShift += 9 - c0.date().dayOfYear();
            VillageContext c = at(h, tv, 6500);
            h.assertTrue(c.date().is("polis_festival"), "历法上是城邦节");
            // 6500：1 名昼班在岗，2 名夜班不值勤（补觉到 6000 为止）
            Soldiers.plan(c);
            List<Job> jobs = JobType.REGISTRY.get(VillageLife.FESTIVAL).candidates(c);
            List<UUID> wrestlers = jobs.stream().filter(j -> j.ref().contains("|wrestle|")).map(Job::assignee).toList();
            h.assertTrue(wrestlers.size() == 2, "两人角力：" + jobs.stream().map(Job::ref).toList());
            ResidentEntity onDuty = java.util.stream.Stream.of(s1, s2, s3).filter(ResidentEntity::soldierBusy).findFirst().orElseThrow();
            h.assertTrue(!wrestlers.contains(onDuty.getUUID()), "值勤的不参加");
            h.assertTrue(jobs.stream().noneMatch(j -> onDuty.getUUID().equals(j.assignee())), "值勤的也不去看");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-98：30 人村庄 + 6 名士兵（3 名在岗）运行 200 刻：每刻平均耗时在 T19 的预算内（≤ 0.5 毫秒）。 */
    @GameTest(template = T, batch = "greek_soldier_perf", timeoutTicks = 400)
    public static void perf_with_soldiers(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h, Age.HEROIC, Map.of());
        List<ResidentEntity> ls = laborers(h, tv, 30);
        for (int i = 1; i <= 6; i++) {
            enlist(h, tv, ls.get(i), i % 2 == 0 ? HOPLITE : JAVELINEER, SoldierProfile.Tier.FULL);
        }
        long start = VillageScheduler.runtime(tv.id()).perfNanos();
        long t0 = h.getLevel().getGameTime();
        h.runAfterDelay(200, () -> {
            long ticks = h.getLevel().getGameTime() - t0;
            double perTickMs = (VillageScheduler.runtime(tv.id()).perfNanos() - start) / 1.0e6 / Math.max(1, ticks);
            io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.info("TC-T20-98：30 人 + 6 名士兵每刻 {} ms", String.format("%.4f", perTickMs));
            h.assertTrue(perTickMs <= 0.5, "每刻 " + String.format("%.3f", perTickMs) + " ms");
            cleanup(h, tv);
            h.succeed();
        });
    }
}
