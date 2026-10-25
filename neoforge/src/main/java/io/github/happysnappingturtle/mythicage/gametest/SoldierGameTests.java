package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.conduct.ConductEvents;
import io.github.happysnappingturtle.mythicage.conduct.Responsibility;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.power.TargetCategory;
import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.CommonVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.core.village.soldier.DutyRules;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierProfile;
import io.github.happysnappingturtle.mythicage.core.village.soldier.SoldierTargetRules;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierAttribution;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierRecord;
import io.github.happysnappingturtle.mythicage.village.soldier.SoldierTargets;
import io.github.happysnappingturtle.mythicage.village.soldier.Soldiers;
import io.github.happysnappingturtle.mythicage.village.soldier.VillageThreats;
import io.github.happysnappingturtle.mythicage.village.work.NaturalResources;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * T20 阶段 ① 士兵框架（TC-T20-40～69）。用测试文明 {@code soldiertest}：一个劳力职业、一个近战兵种 {@code soldiertest:guard}
 * （生命 24、伤害 4、触及 2.5、无盾，兵装用原版皮甲当物品），岗位只有村钟（{@code center}），证明框架不依赖希腊。
 * 场地 17 × 16 × 36，地面 y = 1；村钟在 (8, 2, 2)；村庄的已建范围覆盖整个场地。
 * 测试文明的班次是全天（{@code [0, 0)}），所以士兵一直在岗；看换班的用例另设班次并在结束时拨回时刻。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoldierGameTests {
    private static final String BATCH = "soldier";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    static final String CIV = "soldiertest";
    /** 默认昼夜班的测试文明（看换班的用例用）。 */
    static final String CIV_TIME = "soldiertime";
    /** 合并了共用基础采集的测试文明（两个用例各一份）。 */
    static final String CIV_COMMON = "soldiercommon", CIV_HIDES = "soldierhides";
    static final String WORKER = "soldiertest:worker";
    static final String GUARD = "soldiertest:guard";
    static final String FULL = "minecraft:leather_chestplate";
    static final String SIMPLE = "minecraft:leather_helmet";
    static final String BROKEN = "minecraft:leather";

    private SoldierGameTests() {
    }

    // ───────────── 搭建 ─────────────

    static final String ALL_DAY = "{\"day\": [0, 0], \"night\": [0, 0], \"nightSleep\": [0, 0]}";
    static final String DEFAULT_SHIFTS = "{\"day\": [23000, 12000], \"night\": [12000, 23000], \"nightSleep\": [23000, 6000]}";

    static CivVillageData civ(String shifts) {
        CivVillageData d = CivVillageData.parse(CIV, JsonParser.parseString("""
                {"schedule": {"segments": [{"id": "work", "from": 0, "to": 24000}]},
                 "storage": {"base": 256, "unit": 0},
                 "points": {"rock": {"outputs": {"minecraft:cobblestone": 2}, "take": 2, "max": 40, "recover": 0,
                                     "anim": "dig", "ticks": 10, "perDay": 2}},
                 "residents": {"fallback": "soldiertest:worker", "keeper": "soldiertest:worker", "male": ["Tester"], "female": ["Testa"]},
                 "soldiers": {"order": ["soldiertest:guard"], "posts": ["center"], "minLaborers": 1, "shifts": %s}}"""
                .formatted(shifts)).getAsJsonObject());
        CivVillageData.Profession guard = CivVillageData.parseProfession(GUARD, JsonParser.parseString("""
                {"jobs": ["gather_point"], "stage": "archaic",
                 "soldier": {"kind": "melee", "health": 24, "armor": 2, "damage": 4,
                   "timeline": {"windup": 12, "lock": 4, "active": 3, "recover": 9, "interval": 32},
                   "melee": {"reach": 2.5, "radius": 0.25, "height": 1.15, "halfHeight": 0.5},
                   "kits": {"full": {"item": "minecraft:leather_chestplate"},
                            "simple": {"item": "minecraft:leather_helmet", "armor": -2, "damage": -1},
                            "broken": "minecraft:leather"},
                   "durability": 60}}""").getAsJsonObject());
        return d.withProfessions(Map.of(WORKER, new CivVillageData.Profession(WORKER, Set.of("gather_point"), "archaic", false),
                GUARD, guard), Map.of());
    }

    static VillageWorkGameTests.TV village(GameTestHelper h) {
        return village(h, ALL_DAY, null);
    }

    static VillageWorkGameTests.TV village(GameTestHelper h, String shifts, Box built) {
        // 每种班次一个文明 id：同一批并行的用例会共用同一份测试文明数据
        String civ = shifts.equals(ALL_DAY) ? CIV : CIV_TIME;
        VillageData.overrideForTest(civ, civ(shifts));
        VillageWorkGameTests.floor(h);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        Box b = built != null ? built : new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 35);
        VillageWorkGameTests.TV tv = VillageWorkGameTests.village(h, civ, Map.of(), b);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        return tv;
    }

    static ResidentEntity spawn(GameTestHelper h, VillageWorkGameTests.TV tv, String prof, int x, int z, boolean keeper) {
        BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
        return Residents.spawn(h.getLevel(), tv.village(), VillageData.civ(tv.village().civ()).orElseThrow(), prof, at, at, keeper)
                .orElseThrow();
    }

    /** {@code n} 名劳力（第一名是谷仓管事），床位排在 x = 2…，z = 10。 */
    static List<ResidentEntity> workers(GameTestHelper h, VillageWorkGameTests.TV tv, int n) {
        List<ResidentEntity> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(spawn(h, tv, WORKER, 2 + i, 10, i == 0));
        }
        return out;
    }

    static VillageContext ctx(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
    }

    static VillageEconomy.State state(GameTestHelper h, VillageWorkGameTests.TV tv) {
        return VillageWorkGameTests.state(h, tv);
    }

    /** 直接转岗一名（不看上限，按村库扣兵装；没有兵装就先放一套）。 */
    static ResidentEntity enlist(GameTestHelper h, VillageWorkGameTests.TV tv, ResidentEntity r, SoldierProfile.Tier tier) {
        VillageContext c = ctx(h, tv);
        SoldierProfile p = c.data().soldier(GUARD).orElseThrow();
        state(h, tv).stock.deposit(p.kits().item(tier), 1);
        h.assertTrue(Soldiers.enlist(c, r, GUARD, p, tier), "转岗成功");
        return r;
    }

    static Husk husk(GameTestHelper h, double x, double z, boolean noAi) {
        Husk m = EntityType.HUSK.create(h.getLevel());
        m.moveTo(h.absoluteVec(new Vec3(x, 2, z)));
        m.setNoAi(noAi);
        m.setPersistenceRequired();
        h.getLevel().addFreshEntity(m);
        return m;
    }

    static FakePlayer player(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 20))));
        OnlinePlayers.forTest(p, true);
        return p;
    }

    static void cleanup(GameTestHelper h, VillageWorkGameTests.TV tv, Entity... extra) {
        for (Entity e : extra) {
            if (e != null && !e.isRemoved()) {
                e.discard();
            }
        }
        VillageThreats.clearForTest(tv.id());
        // 不清掉测试文明的数据：同一批并行的用例共用它
        VillageWorkGameTests.cleanup(h, tv);
    }

    // ───────────── 转岗、降回、存档、阵亡（TC-40～45） ─────────────

    /** TC-T20-40：人口 6、村库 1 套兵装，日开始转 1 名劳力：同一 UUID、名册职业改了、库存 −1、属性挂上、生命按比例。 */
    @GameTest(template = T, batch = BATCH)
    public static void convert_on_day_start(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            List<ResidentEntity> ws = workers(h, tv, 6);
            VillageEconomy.State s = state(h, tv);
            s.stock.deposit(FULL, 1);
            // 床位离岗位（村钟 (8, 2, 2)）最近的是 x = 7 的那名
            ResidentEntity nearest = ws.get(5);
            nearest.setHealth(10);
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(s.soldiers.size() == 1, "转了 1 名：" + s.soldiers);
            UUID id = s.soldiers.keySet().iterator().next();
            h.assertTrue(id.equals(nearest.getUUID()), "选了床位离岗位最近的劳力");
            ResidentEntity r = nearest;
            h.assertTrue(r.isSoldier() && r.profession().equals(GUARD) && s.roster.get(id).profession().equals(GUARD), "职业改了");
            h.assertTrue(s.stock.count(FULL) == 0, "兵装从村库扣了");
            h.assertTrue(r.getMaxHealth() == 24 && r.getArmorValue() == 2, "属性：" + r.getMaxHealth() + " / " + r.getArmorValue());
            h.assertTrue(r.kitTier().equals("full"), "同步了兵装档");
            h.assertTrue(Math.abs(r.getHealth() - 12) < 0.01, "生命按比例 10/20 → 12/24：" + r.getHealth());
            ResidentEntity r2 = enlist(h, tv, ws.get(1), SoldierProfile.Tier.SIMPLE);
            h.assertTrue(r2.getArmorValue() == 0, "简易兵装护甲 −2");
            h.assertTrue(!ws.get(0).isSoldier(), "管事不转");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-41：村库没有兵装，不转岗。 */
    @GameTest(template = T, batch = BATCH)
    public static void no_kit_no_convert(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            workers(h, tv, 6);
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(state(h, tv).soldiers.isEmpty(), "没有兵装不转岗");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-42：上限 2、兵装 2 套，每个日开始只转 1 名。 */
    @GameTest(template = T, batch = BATCH)
    public static void one_per_day(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            workers(h, tv, 8);
            VillageEconomy.State s = state(h, tv);
            s.stock.deposit(SIMPLE, 2);
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(s.soldiers.size() == 1, "第一日 1 名");
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(s.soldiers.size() == 2, "第二日 2 名");
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(s.soldiers.size() == 2 && s.stock.count(SIMPLE) == 0, "满了不再转");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-43：士兵 2、人口降到 4（上限 1）：降回最晚转岗的，属性摘掉，兵装退回村库。 */
    @GameTest(template = T, batch = BATCH)
    public static void demote_when_over_cap(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            List<ResidentEntity> ws = workers(h, tv, 8);
            VillageEconomy.State s = state(h, tv);
            ResidentEntity a = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            ResidentEntity b = enlist(h, tv, ws.get(2), SoldierProfile.Tier.FULL);
            for (int i = 4; i < 8; i++) {
                s.roster.remove(ws.get(i).getUUID());
                ws.get(i).discard();
            }
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(a.isSoldier() && !b.isSoldier(), "降回最晚的那名");
            h.assertTrue(b.profession().equals(WORKER) && s.roster.get(b.getUUID()).profession().equals(WORKER), "职业回到劳力");
            h.assertTrue(b.getMaxHealth() == 20 && b.getArmorValue() == 0, "属性摘掉了");
            h.assertTrue(s.stock.count(FULL) == 1, "兵装退回村库（磨损 0，原样）");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-44：存档往返——士兵表、库存、实体的兵装档都不变，不会再扣一次兵装（GUARD-08）。 */
    @GameTest(template = T, batch = BATCH)
    public static void save_roundtrip_no_duplicate(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            List<ResidentEntity> ws = workers(h, tv, 4);
            ResidentEntity r = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            VillageEconomy eco = VillageEconomy.get(h.getLevel().getServer());
            CompoundTag tag = eco.save(new CompoundTag(), h.getLevel().registryAccess());
            VillageEconomy back = VillageEconomy.read(tag, h.getLevel().registryAccess());
            VillageEconomy.State s2 = back.peek(tv.id()).orElseThrow();
            SoldierRecord rec = s2.soldiers.get(r.getUUID());
            h.assertTrue(rec != null && rec.tier() == SoldierProfile.Tier.FULL && rec.profession().equals(GUARD), "士兵行读回：" + rec);
            h.assertTrue(s2.stock.count(FULL) == 0, "库存没多出兵装");
            CompoundTag et = new CompoundTag();
            r.saveWithoutId(et);
            ResidentEntity copy = io.github.happysnappingturtle.mythicage.entity.ModEntities.RESIDENT.get().create(h.getLevel());
            copy.load(et);
            h.assertTrue(copy.isSoldier() && copy.kitTier().equals("full") && copy.getMaxHealth() == 24, "实体读回：" + copy.kitTier());
            // 读档后的日开始不再扣兵装、不重复登记
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(state(h, tv).soldiers.size() == 1 && state(h, tv).stock.count(FULL) == 0, "不重复");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-45：阵亡——名册 −1；完整兵装变破损兵装入库、简易报废；地上没有掉落物。 */
    @GameTest(template = T, batch = BATCH)
    public static void death_returns_broken_kit(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            List<ResidentEntity> ws = workers(h, tv, 6);
            VillageEconomy.State s = state(h, tv);
            ResidentEntity full = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            ResidentEntity simple = enlist(h, tv, ws.get(2), SoldierProfile.Tier.SIMPLE);
            int before = s.roster.size();
            full.kill();
            simple.kill();
            h.assertTrue(s.roster.size() == before - 2 && s.soldiers.isEmpty(), "名册与士兵表都移除");
            h.assertTrue(s.stock.count(BROKEN) == 1 && s.stock.count(SIMPLE) == 0, "完整 → 破损 1，简易报废");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(full.blockPosition()).inflate(4)).isEmpty(),
                    "不掉落物品");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    // ───────────── 接敌、拴绳、友方、墙（TC-46～53） ─────────────

    /** TC-T20-46：在岗士兵发现村内 10 格的敌人：报警、走过去打死；居民没受伤（GUARD-01）。 */
    @GameTest(template = T, batch = "soldier_fight_a", timeoutTicks = 800)
    public static void guard_kills_hostile(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        g.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 3))));
        Husk m = husk(h, 8.5, 13.5, true);
        float[] residentHp = {ws.get(0).getHealth()};
        h.succeedWhen(() -> {
            h.assertTrue(!m.isAlive(), "等士兵打死敌人：" + g.soldierBrain().debug() + " 位 " + g.position() + " 敌 " + m.position()
                    + " 生命 " + m.getHealth());
            h.assertTrue(VillageScheduler.runtime(tv.id()).alertSerial() >= 1, "报过警");
            h.assertTrue(ws.get(0).getHealth() >= residentHp[0], "居民没受伤");
            cleanup(h, tv, m);
        });
    }

    /** TC-T20-47：离岗 20 格（村内）的敌人不接敌。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void far_hostile_ignored(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        Husk m = husk(h, 8.5, 23.5, true);
        h.runAfterDelay(80, () -> {
            h.assertTrue(g.soldierBrain().target() == null, "20 格外不接敌：" + g.soldierBrain().target());
            cleanup(h, tv, m);
            h.succeed();
        });
    }

    /** TC-T20-48：追击超过离岗 24 格：放弃、走回岗位（GUARD-07）。 */
    @GameTest(template = T, batch = "soldier_fight_b", timeoutTicks = 800)
    public static void chase_leash_returns(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        Husk m = husk(h, 8.5, 14.5, true);
        m.setInvulnerable(true);
        boolean[] moved = {false};
        BlockPos post = tv.village().bell().pos();
        h.onEachTick(() -> {
            if (!moved[0] && g.soldierBrain().target() == m) {
                moved[0] = true;
                m.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 29))));
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(moved[0], "先盯上");
            h.assertTrue(g.soldierBrain().target() == null, "超出 24 格放弃");
            h.assertTrue(g.blockPosition().distManhattan(post) <= 8, "回到岗位附近：" + g.blockPosition() + " 岗 " + post);
            cleanup(h, tv, m);
        });
    }

    /** TC-T20-49：目标走出村庄已建范围：放弃追击。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void target_leaves_village(GameTestHelper h) {
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        VillageWorkGameTests.TV tv = village(h, ALL_DAY, new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 14));
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        Husk m = husk(h, 8.5, 12.5, true);
        m.setInvulnerable(true);
        boolean[] moved = {false};
        h.onEachTick(() -> {
            if (!moved[0] && g.soldierBrain().target() == m) {
                moved[0] = true;
                // 已建范围外扩 4 格到 z = 18；挪到 z = 22（离岗 20 格，没超拴绳，但出村了）
                m.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 22))));
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(moved[0], "先盯上：" + g.soldierBrain().debug() + " 敌在村里 " + SoldierTargets.inVillage(h.getLevel(),
                    m.blockPosition(), tv.id()) + " 敌 " + m.blockPosition().toShortString() + " 钟 " + tv.village().bell().pos().toShortString());
            h.assertTrue(g.soldierBrain().target() == null, "出村放弃");
            cleanup(h, tv, m);
        });
    }

    /** TC-T20-50：居民站在矛线上：士兵收手换位，居民不掉血（GUARD-02）。 */
    @GameTest(template = T, batch = "soldier_fight_c", timeoutTicks = 300)
    public static void ally_in_line_withholds(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        ResidentEntity ward = ws.get(2);
        // 都用玻璃围住：士兵在 (8,2,5)，居民 (8,2,6)，敌人 (8,2,7)；居民不能动
        g.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 5))));
        ward.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 6))));
        ward.setNoAi(true);
        for (int z = 4; z <= 8; z++) {
            h.setBlock(new BlockPos(7, 2, z), Blocks.GLASS);
            h.setBlock(new BlockPos(9, 2, z), Blocks.GLASS);
            h.setBlock(new BlockPos(7, 3, z), Blocks.GLASS);
            h.setBlock(new BlockPos(9, 3, z), Blocks.GLASS);
        }
        h.setBlock(new BlockPos(8, 2, 4), Blocks.GLASS);
        h.setBlock(new BlockPos(8, 3, 4), Blocks.GLASS);
        Husk m = husk(h, 8.5, 7.5, true);
        float hp = ward.getHealth();
        // 居民挡在矛线上时收手；士兵可以从居民身边挤过去（居民之间不碰撞），换到居民前面再出手——这也不算穿人刺击
        h.succeedWhen(() -> {
            h.assertTrue(ward.getHealth() >= hp, "居民没被刺：" + g.soldierBrain().debug());
            h.assertTrue(g.soldierBrain().withholds() >= 1, "挡线时收过手：" + g.soldierBrain().debug());
            cleanup(h, tv, m);
        });
    }

    /** TC-T20-51：矛线被石墙截断：刺不到（GUARD-03）。 */
    @GameTest(template = T, batch = "soldier_fight_d", timeoutTicks = 300)
    public static void wall_blocks_thrust(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        g.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 5))));
        // 敌人关在石头笼子里；士兵面前那一格是下半砖：视线（眼高 1.62）从砖上越过，矛（离地 1.15）被砖挡住
        for (int x = 7; x <= 9; x++) {
            for (int z = 6; z <= 8; z++) {
                if (x == 8 && z == 7) {
                    continue;
                }
                if (x == 8 && z == 6) {
                    h.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                    h.setBlock(new BlockPos(x, 3, z), Blocks.STONE_SLAB);
                    continue;
                }
                h.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                h.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        Husk m = husk(h, 8.5, 7.5, true);
        g.soldierBrain().targetForTest(m, SoldierTargetRules.Decision.ENGAGE);
        h.runAfterDelay(200, () -> {
            h.assertTrue(m.getHealth() >= m.getMaxHealth(), "墙挡住了矛：" + m.getHealth() + " 最后伤害来自 " + m.getLastHurtByMob()
                    + " 士兵 " + g.soldierBrain().debug() + " 位 " + g.position());
            cleanup(h, tv, m);
            h.succeed();
        });
    }

    /** TC-T20-52：两个敌人叠在胶囊里：一次出手只伤一个、只伤一次（GUARD-04）。 */
    @GameTest(template = T, batch = "soldier_fight_e", timeoutTicks = 300)
    public static void one_hit_per_attack(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        g.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 5))));
        Husk a = husk(h, 8.5, 6.9, true);
        Husk b = husk(h, 8.5, 7.6, true);
        g.soldierBrain().targetForTest(a, SoldierTargetRules.Decision.ENGAGE);
        long[] first = {-1};
        h.onEachTick(() -> {
            if (first[0] < 0 && (a.getHealth() < a.getMaxHealth() || b.getHealth() < b.getMaxHealth())) {
                first[0] = h.getTick();
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(first[0] >= 0, "等第一刀");
            h.assertTrue(h.getTick() >= first[0] + 5, "有效窗口过完");
            float da = a.getMaxHealth() - a.getHealth();
            float db = b.getMaxHealth() - b.getHealth();
            h.assertTrue((da > 0) != (db > 0), "只伤了一个：" + da + " / " + db);
            h.assertTrue(Math.max(da, db) < 5, "只伤一次：" + Math.max(da, db));
            cleanup(h, tv, a, b);
        });
    }

    /** TC-T20-53：生命不到 30% 后撤到床位；回到 60% 前不回岗。 */
    @GameTest(template = T, batch = "soldier_fight_f", timeoutTicks = 400)
    public static void retreat_when_low(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        Husk m = husk(h, 8.5, 9.5, true);
        m.setInvulnerable(true);
        g.setHealth(6);
        h.succeedWhen(() -> {
            h.assertTrue(g.soldierBrain().retreating(), "后撤中");
            h.assertTrue(g.reached(g.home()), "到了床位：" + g.blockPosition() + " 床 " + g.home());
            cleanup(h, tv, m);
        });
    }

    // ───────────── 调度、逃跑、无兵装（TC-54～56） ─────────────

    /** TC-T20-54：警戒时值勤士兵不派回屋，普通居民回屋。 */
    @GameTest(template = T, batch = BATCH)
    public static void alarm_skips_duty_soldiers(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            List<ResidentEntity> ws = workers(h, tv, 4);
            ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(g.soldierBusy(), "在岗：" + g.soldierBrain().orders());
            VillageScheduler.alarmForTest(tv.id(), h.getLevel().getGameTime() + 400);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(g.job().isEmpty(), "值勤士兵没有回屋单：" + g.job());
            h.assertTrue(ws.get(2).job().map(j -> j.type().equals("go_home")).orElse(false), "普通居民回屋");
            h.assertTrue(!g.idle(), "值勤中不算空闲（不会被抽去祈祷、生产）");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-55：有兵装的士兵 12 格内有敌人也不逃（不回屋）。 */
    @GameTest(template = T, batch = "soldier_fight_g", timeoutTicks = 200)
    public static void soldier_does_not_flee(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        g.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 4))));
        Husk m = husk(h, 8.5, 12.5, true);
        m.setInvulnerable(true);
        h.runAfterDelay(80, () -> {
            h.assertTrue(g.soldierBrain().target() == m, "盯上了");
            h.assertTrue(g.distanceToSqr(m) < Vec3.atBottomCenterOf(g.home()).distanceToSqr(m.position()), "朝敌人去，不是回屋");
            cleanup(h, tv, m);
            h.succeed();
        });
    }

    /** TC-T20-56：无兵装模式：不接敌，站到敌人与居民之间（护送）。 */
    @GameTest(template = T, batch = "soldier_fight_h", timeoutTicks = 300)
    public static void no_kit_escorts(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        VillageContext c = ctx(h, tv);
        SoldierProfile p = c.data().soldier(GUARD).orElseThrow();
        state(h, tv).soldiers.put(g.getUUID(), state(h, tv).soldiers.get(g.getUUID()).withTier(SoldierProfile.Tier.NONE, 0));
        Soldiers.apply(g, p, SoldierProfile.Tier.NONE);
        ResidentEntity ward = ws.get(2);
        ward.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 6))));
        ward.setNoAi(true);
        for (ResidentEntity w : ws) {
            if (w != g && w != ward) {
                w.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 33))));
                w.setNoAi(true);
            }
        }
        Husk m = husk(h, 8.5, 12.5, true);
        h.succeedWhen(() -> {
            h.assertTrue(g.soldierBrain().target() == m && g.soldierBrain().decision() == SoldierTargetRules.Decision.ESCORT,
                    "护送：" + g.soldierBrain().decision());
            double gz = g.getZ() - ward.getZ();
            h.assertTrue(gz > 0.5 && gz < 2.5 && Math.abs(g.getX() - ward.getX()) < 1.2, "站在居民与敌人之间：" + g.position());
            h.assertTrue(m.getHealth() >= m.getMaxHealth(), "没出手");
            cleanup(h, tv, m);
        });
    }

    // ───────────── 归因、善恶、目标分类（TC-57～60） ─────────────

    /** TC-T20-57：士兵打死玩家打过的敌人：玩家不得经验、无责任人、打上「士兵击杀」标记（TC-T18-35 后半）。 */
    @GameTest(template = T, batch = "soldier_fight_i", timeoutTicks = 800)
    public static void soldier_kill_attribution(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        List<ResidentEntity> ws = workers(h, tv, 4);
        ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
        g.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 6))));
        FakePlayer p = player(h, "soldier_attr");
        Husk m = husk(h, 8.5, 8.5, true);
        m.hurt(h.getLevel().damageSources().playerAttack(p), 1);
        Responsibility.Blame blame = Responsibility.of(g.damageSources().mobAttack(g));
        h.assertTrue(blame.player().isEmpty() && blame.via() == Responsibility.Via.SOLDIER, "士兵的伤害没有责任人：" + blame);
        BlockPos where = m.blockPosition();
        h.succeedWhen(() -> {
            h.assertTrue(!m.isAlive(), "等士兵打死");
            h.assertTrue(SoldierAttribution.killedBySoldier(m), "打了士兵击杀标记");
            h.assertTrue(h.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(where).inflate(6)).isEmpty(), "没有经验球");
            OnlinePlayers.forTest(p, false);
            cleanup(h, tv);
        });
    }

    /** TC-T20-58：伤害士兵按「防卫者」、普通居民按「村民」；士兵打居民没有责任人。 */
    @GameTest(template = T, batch = BATCH)
    public static void conduct_categories(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            List<ResidentEntity> ws = workers(h, tv, 4);
            ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            h.assertTrue(ConductEvents.category(h.getLevel(), g) == ConductEvents.Category.DEFENDER, "士兵 = 防卫者");
            h.assertTrue(ConductEvents.category(h.getLevel(), ws.get(2)) == ConductEvents.Category.VILLAGER, "居民 = 村民");
            h.assertTrue(ConductEvents.judge(h.getLevel(), ws.get(2), g.damageSources().mobAttack(g), false) == 0, "士兵打居民不改任何人的善恶");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-59、60：目标分类——本村居民 / 士兵、他村居民；召唤单位不以他们为目标；神力卡里的 own_soldier 反应能作用到士兵。 */
    @GameTest(template = T, batch = BATCH)
    public static void target_classification(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        try {
            List<ResidentEntity> ws = workers(h, tv, 4);
            ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            FakePlayer p = player(h, "soldier_cls");
            Set<UUID> own = Set.of(tv.id());
            h.assertTrue(TargetClassifier.classify(TargetFactsCollector.collect(p, g, own)) == TargetCategory.OWN_SOLDIER, "本村士兵");
            h.assertTrue(TargetClassifier.classify(TargetFactsCollector.collect(p, ws.get(2), own)) == TargetCategory.OWN_VILLAGER,
                    "本村居民");
            h.assertTrue(TargetClassifier.classify(TargetFactsCollector.collect(p, ws.get(2), Set.of())) == TargetCategory.OTHER_VILLAGE,
                    "他村居民");
            h.assertTrue(!io.github.happysnappingturtle.mythicage.core.summon.SummonTargetRules.canAttack(
                    TargetFactsCollector.collect(p, g, own), true), "召唤单位不打本村士兵");
            OnlinePlayers.forTest(p, false);
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    // ───────────── 玩家、动物、中立（TC-61～63） ─────────────

    /** TC-T20-61、62：PvP 关时对打居民的玩家只护送；PvP 开时 30 秒内反击，31 秒后解除；创造模式不管。 */
    @GameTest(template = T, batch = "soldier_pvp")
    public static void player_rules_follow_pvp(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        boolean pvp = h.getLevel().getServer().isPvpAllowed();
        try {
            List<ResidentEntity> ws = workers(h, tv, 4);
            ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            FakePlayer p = player(h, "soldier_pvp");
            long now = h.getLevel().getGameTime();
            VillageThreats.note(tv.id(), p.getUUID(), now);
            h.getLevel().getServer().setPvpAllowed(false);
            h.assertTrue(SoldierTargetRules.decide(SoldierTargets.facts(g, p, tv.id(), 2.5, now)) == SoldierTargetRules.Decision.ESCORT,
                    "PvP 关：护送");
            h.getLevel().getServer().setPvpAllowed(true);
            h.assertTrue(SoldierTargetRules.decide(SoldierTargets.facts(g, p, tv.id(), 2.5, now + 29 * 20)) == SoldierTargetRules.Decision.ENGAGE,
                    "PvP 开：29 秒内反击");
            h.assertTrue(SoldierTargetRules.decide(SoldierTargets.facts(g, p, tv.id(), 2.5, now + 31 * 20)) == SoldierTargetRules.Decision.IGNORE,
                    "31 秒后解除");
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            h.assertTrue(SoldierTargetRules.decide(SoldierTargets.facts(g, p, tv.id(), 2.5, now)) == SoldierTargetRules.Decision.IGNORE,
                    "创造模式不管");
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            OnlinePlayers.forTest(p, false);
        } finally {
            h.getLevel().getServer().setPvpAllowed(pvp);
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-63：动物永远不打（咬了居民也只护送）；中立生物（僵尸猪灵）打过居民 100 刻内接敌，之后停手。 */
    @GameTest(template = T, batch = BATCH)
    public static void animals_and_neutrals(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        Pig pig = EntityType.PIG.create(h.getLevel());
        net.minecraft.world.entity.monster.ZombifiedPiglin wolf = EntityType.ZOMBIFIED_PIGLIN.create(h.getLevel());
        try {
            List<ResidentEntity> ws = workers(h, tv, 4);
            ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            long now = h.getLevel().getGameTime();
            VillageThreats.note(tv.id(), pig.getUUID(), now);
            h.assertTrue(SoldierTargetRules.decide(SoldierTargets.facts(g, pig, tv.id(), 2.5, now)) == SoldierTargetRules.Decision.ESCORT,
                    "动物咬了居民：只护送，不打");
            h.assertTrue(SoldierTargetRules.decide(SoldierTargets.facts(g, pig, tv.id(), 2.5, now + 200)) == SoldierTargetRules.Decision.IGNORE,
                    "平时的动物不管");
            VillageThreats.note(tv.id(), wolf.getUUID(), now);
            h.assertTrue(SoldierTargetRules.decide(SoldierTargets.facts(g, wolf, tv.id(), 2.5, now + 100)) == SoldierTargetRules.Decision.ENGAGE,
                    "中立生物打过居民：接敌");
            h.assertTrue(SoldierTargetRules.decide(SoldierTargets.facts(g, wolf, tv.id(), 2.5, now + 101)) == SoldierTargetRules.Decision.IGNORE,
                    "停手 100 刻后不再打");
        } finally {
            cleanup(h, tv, pig, wolf);
        }
        h.succeed();
    }

    // ───────────── 班次（TC-64～66，改世界时刻，单独一批） ─────────────

    /** TC-T20-64：2 名士兵，dayTime 1000 昼班在岗、夜班在睡；18000 互换。结束时拨回时刻。 */
    @GameTest(template = T, batch = "soldier_time")
    public static void shifts_swap(GameTestHelper h) {
        long t0 = h.getLevel().getDayTime();
        VillageWorkGameTests.TV tv = village(h, DEFAULT_SHIFTS, null);
        try {
            List<ResidentEntity> ws = workers(h, tv, 5);
            ResidentEntity a = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            ResidentEntity b = enlist(h, tv, ws.get(2), SoldierProfile.Tier.FULL);
            h.getLevel().setDayTime(1000);
            Soldiers.plan(ctx(h, tv));
            h.assertTrue(a.soldierBrain().orders().state() == DutyRules.State.ON_DUTY, "1000：昼班在岗");
            h.assertTrue(b.soldierBrain().orders().state() == DutyRules.State.SHIFT_SLEEP, "1000：夜班补觉");
            h.getLevel().setDayTime(18000);
            Soldiers.plan(ctx(h, tv));
            h.assertTrue(a.soldierBrain().orders().state() == DutyRules.State.OFF_DUTY, "18000：昼班下班");
            h.assertTrue(b.soldierBrain().orders().state() == DutyRules.State.ON_DUTY, "18000：夜班在岗");
        } finally {
            h.getLevel().setDayTime(t0);
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-65、66：不值勤的士兵按职业接单（采石），上班时工作单作废；值勤中不算空闲（祈祷、节庆都抽不到）。 */
    @GameTest(template = T, batch = "soldier_time")
    public static void off_duty_works_then_reports(GameTestHelper h) {
        long t0 = h.getLevel().getDayTime();
        VillageWorkGameTests.TV tv = village(h, DEFAULT_SHIFTS, null);
        try {
            List<ResidentEntity> ws = workers(h, tv, 4);
            ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            // 只留士兵一个人能接单：其余劳力都设成在途
            h.setBlock(new BlockPos(8, 2, 8), Blocks.COBBLESTONE);
            VillageEconomy.State s = state(h, tv);
            s.points.put("rock_0", ResourcePoint.fresh("rock_0", "rock", VillageWorkGameTests.abs(h, 8, 2, 8), 40, 0, 0));
            for (ResidentEntity w : ws) {
                if (w != g) {
                    w.setMigrant(true, 0);
                }
            }
            h.getLevel().setDayTime(13000);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(g.soldierBrain().orders().state() == DutyRules.State.OFF_DUTY && g.idle() || g.job().isPresent(),
                    "不值勤：空闲或已接单");
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(g.job().map(j -> j.type().equals("gather_point")).orElse(false), "接了采石单：" + g.job());
            h.getLevel().setDayTime(1000);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(g.job().isEmpty() && g.soldierBusy() && !g.idle(), "上班：工作单作废、不再空闲");
            h.assertTrue(s.stock.escrows().isEmpty(), "托管退回");
        } finally {
            h.getLevel().setDayTime(t0);
            cleanup(h, tv);
        }
        h.succeed();
    }

    // ───────────── 配置、原版村庄（TC-68、69） ─────────────

    /** TC-T20-68：关掉士兵（soldiers.enabled = false）：不转岗，已有士兵降回。 */
    @GameTest(template = T, batch = "soldier_config")
    public static void disabled_demotes_all(GameTestHelper h) {
        VillageWorkGameTests.TV tv = village(h);
        ConfigHolder<VillagesConfig> holder = ModConfigs.villages();
        VillagesConfig original = holder.current();
        try {
            List<ResidentEntity> ws = workers(h, tv, 8);
            ResidentEntity g = enlist(h, tv, ws.get(1), SoldierProfile.Tier.FULL);
            state(h, tv).stock.deposit(FULL, 2);
            holder.reload("{\"schemaVersion\":3,\"soldiers\":{\"enabled\":false}}");
            Soldiers.dayStart(ctx(h, tv));
            h.assertTrue(!g.isSoldier() && state(h, tv).soldiers.isEmpty(), "降回了");
            h.assertTrue(state(h, tv).stock.count(FULL) == 3, "兵装退回、也没有新转岗");
        } finally {
            holder.reload(io.github.happysnappingturtle.mythicage.core.config.ConfigLoader.toPrettyJson(holder.schema(), original));
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-69：原版村庄（没有规划、不走居民框架）：调度器不处理，也就没有士兵（D9）。 */
    @GameTest(template = T, batch = BATCH)
    public static void vanilla_village_has_no_soldiers(GameTestHelper h) {
        UUID id = UUID.randomUUID();
        VillageRegistry.Village v = new VillageRegistry.Village(id, GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(3, 2, 3))),
                "greek", "zeus", VillageRegistry.Origin.VANILLA, null, "plains", -1);
        h.assertTrue(!io.github.happysnappingturtle.mythicage.village.VillagePopulation.residentFramework(v), "原版村庄不走居民框架");
        h.assertTrue(VillageScheduler.contextForTest(h.getLevel(), id).isEmpty(), "没有规划就没有调度上下文");
        h.succeed();
    }

    // ───────────── 共用基础采集（TC-69a～69e） ─────────────

    /** 共用数据（{@code village_civ/_common.json}）原文。 */
    static com.google.gson.JsonObject common(GameTestHelper h) {
        try (var in = h.getLevel().getServer().getResourceManager().getResourceOrThrow(ResourceLocation.fromNamespaceAndPath(
                MythicAgeIds.MOD_ID, "mythic_age/village_civ/_common.json")).open()) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** TC-T20-69a、b：采石面、砾石滩——找到露出的方块就登记；劳力采一次村库入圆石 / 燧石，余量按日恢复。希腊数据里都有。 */
    @GameTest(template = T, batch = BATCH)
    public static void common_stone_and_gravel(GameTestHelper h) {
        CivVillageData greek = VillageData.civ("greek").orElseThrow();
        h.assertTrue(greek.points().containsKey("stone") && greek.points().containsKey("gravel") && greek.points().containsKey("hides"),
                "希腊合并了共用采集：" + greek.points().keySet());
        h.assertTrue(greek.points().get("stone").professions().equals(Set.of("greek:laborer")), "@fallback 换成希腊劳力");
        h.assertTrue(greek.woodland() != null && greek.woodland().max() == 24, "林地挪进了共用数据，数值不变");
        // 用合并了共用数据的测试文明登记：已建范围只盖场地前半，砾石放在后半
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        Box built = new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 10);
        CivVillageData merged = CivVillageData.parse(CIV_COMMON, CommonVillageData.merge(common(h), JsonParser.parseString(
                "{\"residents\": {\"fallback\": \"soldiertest:worker\"}}").getAsJsonObject(), CIV_COMMON));
        VillageData.overrideForTest(CIV_COMMON, merged.withProfessions(civ(ALL_DAY).professions(), Map.of()));
        VillageWorkGameTests.floor(h);
        VillageWorkGameTests.TV tv = VillageWorkGameTests.village(h, CIV_COMMON, Map.of(), built);
        try {
            h.setBlock(new BlockPos(8, 2, 28), Blocks.GRAVEL);
            CivVillageData.NaturalPoint np = merged.naturalPoints().get("gravel");
            CivVillageData.NaturalPoint near = new CivVillageData.NaturalPoint(np.kind(), np.block(), 30, false);
            h.assertTrue(NaturalResources.registerNaturalForTest(h.getLevel(), tv.id(), near), "登记了砾石滩");
            ResourcePoint g = state(h, tv).points.get("gravel_0");
            // 别的用例可能在附近放过砾石露头：只要求登记在一块露出的砾石上
            h.assertTrue(g != null && h.getLevel().getBlockState(new BlockPos(g.pos().x(), g.pos().y(), g.pos().z())).is(Blocks.GRAVEL),
                    "在砾石上：" + state(h, tv).points);
            h.assertTrue(merged.points().get("gravel").outputs().equals(Map.of("minecraft:flint", 1)), "筛得燧石");
            h.assertTrue(merged.points().get("stone").outputs().equals(Map.of("minecraft:cobblestone", 2)), "采得圆石");
            ResourcePoint used = g.take(1);
            h.assertTrue(used.recover(g.lastDay() + 1).remaining() == Math.min(g.max(), used.remaining() + 2), "每日恢复 2");
        } finally {
            h.setBlock(new BlockPos(8, 2, 28), Blocks.AIR);
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T20-69e：剥皮鞣革登记在畜栏锚点上：有畜栏才有；一次取 2、每日恢复 1，所以两天一张皮革。 */
    @GameTest(template = T, batch = BATCH)
    public static void common_hides_on_pen(GameTestHelper h) {
        CivVillageData.PointKind k = VillageData.civ("greek").orElseThrow().points().get("hides");
        // 不登记天然点（采石面、砾石滩找不到时会在附近放露头，可能放进隔壁场地）
        h.assertTrue(k != null && k.anchor().equals("pen") && k.take() == 2 && k.max() == 2 && k.recover() == 1
                && k.outputs().equals(Map.of("minecraft:leather", 1)), "鞣革数据：" + k);
        CivVillageData merged = CivVillageData.parse(CIV_HIDES, CommonVillageData.merge(common(h), JsonParser.parseString(
                "{\"residents\": {\"fallback\": \"soldiertest:worker\"}, \"woodland\": null,"
                        + " \"naturalPoints\": {\"stone\": null, \"gravel\": null}}").getAsJsonObject(), CIV_HIDES));
        VillageData.overrideForTest(CIV_HIDES, merged.withProfessions(civ(ALL_DAY).professions(), Map.of()));
        VillageWorkGameTests.floor(h);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        Map<String, List<Pos>> anchors = new LinkedHashMap<>();
        anchors.put("pen", List.of(VillageWorkGameTests.abs(h, 4, 2, 20)));
        VillageWorkGameTests.TV tv = VillageWorkGameTests.village(h, CIV_HIDES, anchors,
                new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 35));
        VillageWorkGameTests.TV none = null;
        try {
            VillageWorkGameTests.takeOver(h, tv);
            h.assertTrue(state(h, tv).points.containsKey("hides_0"), "有畜栏：登记了鞣革");
            none = VillageWorkGameTests.village(h, CIV_HIDES, Map.of(), new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15,
                    o.getZ() + 35));
            VillageWorkGameTests.takeOver(h, none);
            h.assertTrue(state(h, none).points.keySet().stream().noneMatch(x -> x.startsWith("hides")), "没有畜栏：没有鞣革");
        } finally {
            if (none != null) {
                VillageWorkGameTests.cleanup(h, none);
            }
            cleanup(h, tv);
        }
        h.succeed();
    }
}
