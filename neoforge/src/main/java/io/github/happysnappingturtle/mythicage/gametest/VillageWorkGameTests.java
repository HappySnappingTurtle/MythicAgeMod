package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.OnlinePlayers;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageAffairs;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.resident.Residents;
import io.github.happysnappingturtle.mythicage.village.work.PointEvents;
import io.github.happysnappingturtle.mythicage.village.work.VillageData;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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
 * T19 阶段 ① 居民框架（TC-T19-40～53）。用测试文明 {@code test}：一个职业、一种采集点（石堆 → 圆石）、一条配方（圆石 2 → 石砖 1，工作台），
 * 证明框架不依赖希腊。场地 17 × 16 × 36，地面 y = 1；村庄的已建范围覆盖整个场地。
 * 真实运行由 {@link VillageScheduler#forceRunningForTest} 指定；居民的走路与动作用 {@link ResidentEntity#fastForwardForTest} 跳过。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageWorkGameTests {
    private static final String BATCH = "village_work";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    static final String CIV = "test";
    static final String WORKER = "test:worker";
    private static final String COBBLE = "minecraft:cobblestone";
    private static final String BRICKS = "minecraft:stone_bricks";
    private static final String BREAD = "minecraft:bread";

    private VillageWorkGameTests() {
    }

    // ───────────── 搭建 ─────────────

    static CivVillageData testCiv() {
        CivVillageData d = CivVillageData.parse(CIV, JsonParser.parseString("""
                {"schedule": {"segments": [{"id": "work", "from": 0, "to": 24000}]},
                 "storage": {"base": 64, "unit": 0},
                 "rations": {"floor": 0, "portions": {"minecraft:bread": 1.0}},
                 "points": {"rock": {"outputs": {"minecraft:cobblestone": 2}, "take": 2, "max": 10, "recover": 0,
                                     "anim": "dig", "ticks": 10, "perDay": 2}},
                 "residents": {"fallback": "test:worker", "male": ["Tester"], "female": ["Testa"]}}""").getAsJsonObject());
        VillageRecipe brick = VillageRecipe.parse("test:brick", CIV, JsonParser.parseString("""
                {"station": "minecraft:crafting_table", "professions": ["test:worker"],
                 "inputs": {"minecraft:cobblestone": 2}, "outputs": {"minecraft:stone_bricks": 1},
                 "stages": [{"anim": "work", "ticks": 20}]}""").getAsJsonObject());
        return d.withProfessions(Map.of(WORKER, new CivVillageData.Profession(WORKER, java.util.Set.of("gather_point", "craft"),
                "archaic", false)), Map.of(brick.id(), brick));
    }

    static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    static Pos abs(GameTestHelper h, int x, int y, int z) {
        BlockPos b = h.absolutePos(new BlockPos(x, y, z));
        return new Pos(b.getX(), b.getY(), b.getZ());
    }

    record TV(VillageRegistry.Village village, VillagePlans.Plan plan) {
        UUID id() {
            return village.id();
        }
    }

    /** 测试文明的生成村庄：中心 (8, 2, 2)；锚点按相对坐标给；已建范围覆盖整个场地。 */
    static TV village(GameTestHelper h, Map<String, List<Pos>> anchors) {
        VillageData.overrideForTest(CIV, testCiv());
        floor(h);
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        return village(h, CIV, anchors, new Box(o.getX(), o.getY(), o.getZ(), o.getX() + 16, o.getY() + 15, o.getZ() + 35));
    }

    /** 任意文明的生成村庄（不铺地面）：中心 (8, 2, 2)。 */
    static TV village(GameTestHelper h, String civ, Map<String, List<Pos>> anchors, Box built) {
        Pos center = abs(h, 8, 2, 2);
        Map<String, List<Pos>> a = new LinkedHashMap<>(anchors);
        // 调用方给了广场锚点就用它（游行的集合点；村中心仍是场地边上的钟）
        a.putIfAbsent("center", List.of(center));
        VillageInfoPiece.Info info = new VillageInfoPiece.Info(civ, "zeus", "test", 1, "test", center, a, List.of(built), List.of());
        UUID id = UUID.randomUUID();
        VillageRegistry.Village v = new VillageRegistry.Village(id, GlobalPos.of(h.getLevel().dimension(),
                VillageInfoPiece.block(center)), civ, "zeus", VillageRegistry.Origin.GENERATED, null, "", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        VillagePlans.Plan p = new VillagePlans.Plan(id, h.getLevel().dimension(), "test", h.getLevel().getRandom().nextLong(),
                info, true);
        VillagePlans.get(h.getLevel().getServer()).put(p);
        VillageScheduler.forceRunningForTest(id, true);
        return new TV(v, p);
    }

    static ResidentEntity resident(GameTestHelper h, TV tv, int x, int z) {
        BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
        return Residents.spawn(h.getLevel(), tv.village(), testCiv(), WORKER, at, at, false).orElseThrow();
    }

    static ResidentEntity resident(GameTestHelper h, TV tv, String profession, int x, int z) {
        BlockPos at = h.absolutePos(new BlockPos(x, 2, z));
        return Residents.spawn(h.getLevel(), tv.village(), VillageData.civ(tv.village().civ()).orElseThrow(), profession, at, at,
                false).orElseThrow();
    }

    static VillageEconomy.State state(GameTestHelper h, TV tv) {
        return VillageEconomy.get(h.getLevel().getServer()).state(tv.id());
    }

    static void cleanup(GameTestHelper h, TV tv) {
        // 只清本村的：同一批的用例并排放着，按范围清会删掉隔壁用例的居民
        for (ResidentEntity r : h.getLevel().getEntitiesOfClass(ResidentEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(64),
                r -> r.village().map(tv.id()::equals).orElse(false))) {
            r.discard();
        }
        VillagePlans.get(h.getLevel().getServer()).remove(tv.id());
        VillageRegistry.get(h.getLevel().getServer()).remove(tv.village().bell());
        VillageEconomy.get(h.getLevel().getServer()).clearForTest(tv.id());
        io.github.happysnappingturtle.mythicage.village.quest.VillageQuests.get(h.getLevel().getServer()).remove(tv.id());
        io.github.happysnappingturtle.mythicage.village.quest.QuestService.clearForTest(tv.id());
        VillageScheduler.clearForTest(tv.id());
    }

    private static FakePlayer player(GameTestHelper h, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8, 2, 34))));
        OnlinePlayers.forTest(p, true);
        return p;
    }

    /** 石堆在 (8, 2, 8)、工作台在 (9, 2, 7)；居民在 (8, 2, 6) 旁边就能够到。 */
    private static Map<String, List<Pos>> workshop(GameTestHelper h, boolean rock, boolean station) {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        if (rock) {
            h.setBlock(new BlockPos(8, 2, 8), Blocks.COBBLESTONE);
            a.put("rock", List.of(abs(h, 8, 2, 8)));
        }
        if (station) {
            h.setBlock(new BlockPos(9, 2, 7), Blocks.CRAFTING_TABLE);
            a.put("workstation", List.of(abs(h, 9, 2, 7)));
        }
        return a;
    }

    /** 先接管一次（发库存容量、登记采集点），再往村库里放东西。 */
    static void takeOver(GameTestHelper h, TV tv) {
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
    }

    // ───────────── 用例 ─────────────

    /** TC-T19-40：居民身份存档往返，名册里有同一份。 */
    @GameTest(template = T, batch = BATCH)
    public static void resident_identity_roundtrip(GameTestHelper h) {
        TV tv = village(h, Map.of());
        try {
            ResidentEntity r = resident(h, tv, 8, 6);
            CompoundTag tag = new CompoundTag();
            r.saveWithoutId(tag);
            ResidentEntity copy = ModEntities.RESIDENT.get().create(h.getLevel());
            h.assertTrue(copy != null, "能创建");
            copy.load(tag);
            h.assertTrue(copy.village().equals(r.village()), "村庄一致");
            h.assertTrue(r.home() != null && r.home().equals(copy.home()), "床位一致");
            h.assertTrue(copy.profession().equals(WORKER) && copy.civ().equals(CIV), "职业、文明一致");
            h.assertTrue(copy.female() == r.female() && copy.palette() == r.palette(), "外观一致");
            h.assertTrue(copy.getCustomName() != null && copy.getCustomName().getString().equals(r.getCustomName().getString()),
                    "名字一致");
            VillageEconomy.Member m = state(h, tv).roster.get(r.getUUID());
            h.assertTrue(m != null && m.profession().equals(WORKER) && m.home().equals(r.home()), "名册一致");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-41：测试文明跑通「采集 → 加工 → 入库」，库存守恒。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 400)
    public static void test_civ_gather_then_craft(GameTestHelper h) {
        TV tv = village(h, workshop(h, true, true));
        ResidentEntity r = resident(h, tv, 8, 6);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.succeedWhen(() -> {
            r.fastForwardForTest();
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(s.stock.count(BRICKS) >= 1, "做出了石砖：" + s.stock.items() + " 工作单 " + r.job()
                    + " 空闲 " + r.idle() + " 进行中 " + VillageScheduler.runtime(tv.id()).activeJobs() + " 刻 "
                    + h.getLevel().getServer().overworld().getGameTime() + " 上次规划 " + VillageScheduler.runtime(tv.id()).lastPlan()
                    + " 规划数 " + VillagePlans.get(h.getLevel().getServer()).all().size() + " "
                    + VillageScheduler.runtime(tv.id()).lastStats());
            ResourcePoint p = s.points.get("rock_0");
            int mined = (p.max() - p.remaining());
            int accounted = s.stock.count(COBBLE) + s.stock.escrowed(COBBLE) + 2 * s.stock.count(BRICKS);
            h.assertTrue(mined == accounted, "守恒：采了 " + mined + "，库存折合 " + accounted);
            cleanup(h, tv);
        });
    }

    /** TC-T19-42：加工到一半拆掉工作站：作废，托管退回，不产出。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void station_removed_mid_craft(GameTestHelper h) {
        TV tv = village(h, workshop(h, false, true));
                ResidentEntity r = resident(h, tv, 8, 6);
        takeOver(h, tv);
        state(h, tv).stock.deposit(COBBLE, 2);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().map(j -> j.type().equals("craft")).orElse(false), "派了加工单");
        h.assertTrue(state(h, tv).stock.count(COBBLE) == 0 && state(h, tv).stock.escrowed(COBBLE) == 2, "圆石已托管");
        h.setBlock(new BlockPos(9, 2, 7), Blocks.AIR);
        r.fastForwardForTest();
        h.succeedWhen(() -> {
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(r.job().isEmpty(), "工作单结束");
            h.assertTrue(s.stock.count(COBBLE) == 2 && s.stock.escrowed(COBBLE) == 0, "托管退回：" + s.stock.items());
            h.assertTrue(s.stock.count(BRICKS) == 0, "不产出");
            cleanup(h, tv);
        });
    }

    /** TC-T19-43：加工到一半居民被杀：托管退回，名册除名。 */
    @GameTest(template = T, batch = BATCH)
    public static void killed_mid_craft(GameTestHelper h) {
        TV tv = village(h, workshop(h, false, true));
        try {
                        ResidentEntity r = resident(h, tv, 8, 6);
            takeOver(h, tv);
        state(h, tv).stock.deposit(COBBLE, 2);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().isPresent(), "派了单");
            r.kill();
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(s.stock.count(COBBLE) == 2 && s.stock.escrowed(COBBLE) == 0, "托管退回");
            h.assertTrue(!s.roster.containsKey(r.getUUID()), "名册除名");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-44：走不到的目标：超时失败、托管退回；同一目标 3 次后当日不再派。 */
    @GameTest(template = T, batch = BATCH)
    public static void unreachable_target_gives_up(GameTestHelper h) {
        Map<String, List<Pos>> a = new LinkedHashMap<>();
        h.setBlock(new BlockPos(8, 2, 30), Blocks.COBBLESTONE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = 2; y <= 4; y++) {
                    if (dx != 0 || dz != 0 || y > 2) {
                        h.setBlock(new BlockPos(8 + dx, y, 30 + dz), Blocks.GLASS);
                    }
                }
            }
        }
        a.put("rock", List.of(abs(h, 8, 2, 30)));
        TV tv = village(h, a);
        try {
            ResidentEntity r = resident(h, tv, 8, 6);
            for (int i = 0; i < 3; i++) {
                VillageScheduler.processNowForTest(h.getLevel(), tv.id());
                h.assertTrue(r.job().isPresent(), "第 " + (i + 1) + " 次派单");
                r.ageJobForTest(Job.DEFAULT_TIMEOUT + 10);
                r.tickJobForTest();
                h.assertTrue(r.job().isEmpty(), "超时作废");
            }
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().isEmpty(), "3 次失败后当日不再派");
            h.assertTrue(VillageScheduler.runtime(tv.id()).failures().count("point:rock_0",
                    MythDays.today(h.getLevel().getServer())) == 3, "失败计数 3");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-45：敌对生物靠近：居民放下工作回屋（托管退回）；尸壳把居民当目标。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void hostile_makes_resident_flee(GameTestHelper h) {
        TV tv = village(h, workshop(h, false, true));
                ResidentEntity r = resident(h, tv, 8, 6);
        takeOver(h, tv);
        state(h, tv).stock.deposit(COBBLE, 2);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
        h.assertTrue(r.job().isPresent(), "派了单");
        Husk husk = h.spawn(EntityType.HUSK, new BlockPos(8, 2, 12));
        h.succeedWhen(() -> {
            h.assertTrue(r.job().isEmpty(), "放下工作");
            h.assertTrue(state(h, tv).stock.count(COBBLE) == 2, "托管退回");
            h.assertTrue(targetsResidents(husk), "尸壳有以居民为目标的目标选择");
            husk.discard();
            cleanup(h, tv);
        });
    }

    /** 目标选择里有没有「找最近的居民」（不依赖难度与视线，只看有没有挂上）。 */
    static boolean targetsResidents(net.minecraft.world.entity.Mob mob) {
        try {
            java.lang.reflect.Field f = net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal.class
                    .getDeclaredField("targetType");
            f.setAccessible(true);
            for (var g : mob.targetSelector.getAvailableGoals()) {
                if (g.getGoal() instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<?> t
                        && f.get(t) == ResidentEntity.class) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** TC-T19-46：玩家打居民：善恶值 −2（村民类别）。 */
    @GameTest(template = T, batch = BATCH)
    public static void hitting_resident_costs_conduct(GameTestHelper h) {
        TV tv = village(h, Map.of());
        FakePlayer p = player(h, "t19_hit");
        try {
            ResidentEntity r = resident(h, tv, 8, 6);
            int before = MythData.get(p).conductScore();
            r.hurt(h.getLevel().damageSources().playerAttack(p), 1);
            h.assertTrue(MythData.get(p).conductScore() == before - 2, "善恶 −2：" + MythData.get(p).conductScore());
        } finally {
            OnlinePlayers.forTest(p, false);
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-47：玩家在采集点上挖掘，与 NPC 共用余量。 */
    @GameTest(template = T, batch = BATCH)
    public static void player_mining_shares_point(GameTestHelper h) {
        TV tv = village(h, workshop(h, true, false));
        try {
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(state(h, tv).points.get("rock_0").remaining() == 10, "登记余量 10");
            h.assertTrue(PointEvents.consumeAt(h.getLevel(), h.absolutePos(new BlockPos(8, 2, 8))), "命中采集点");
            h.assertTrue(state(h, tv).points.get("rock_0").remaining() == 8, "余量 −2");
            h.assertTrue(!PointEvents.consumeAt(h.getLevel(), h.absolutePos(new BlockPos(5, 2, 8))), "别处不算");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-48：采集点所在区块没加载：不派单。 */
    @GameTest(template = T, batch = BATCH)
    public static void unloaded_point_not_dispatched(GameTestHelper h) {
        BlockPos far = h.absolutePos(new BlockPos(8, 2, 8)).offset(200000, 0, 200000);
        TV tv = village(h, Map.of("rock", List.of(new Pos(far.getX(), far.getY(), far.getZ()))));
        try {
            ResidentEntity r = resident(h, tv, 8, 6);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(!h.getLevel().isLoaded(far), "确实没加载");
            h.assertTrue(r.job().isEmpty(), "不派单");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-49：离线补算：未运行 → 离开 2 日 → 回来补算一次；再处理一次不重复。 */
    @GameTest(template = T, batch = BATCH)
    public static void catch_up_once(GameTestHelper h) {
        TV tv = village(h, workshop(h, true, false));
        try {
            ResidentEntity r = resident(h, tv, 8, 6);
            r.discard();
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(s.initialized && s.roster.size() == 1, "已接管，名册 1 人");
            VillageScheduler.forceRunningForTest(tv.id(), false);
            h.assertTrue(!VillageScheduler.processNowForTest(h.getLevel(), tv.id()), "未运行");
            s.lastRealTick = WorldClock.now(h.getLevel().getServer()) - 48000;
            VillageScheduler.forceRunningForTest(tv.id(), true);
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(s.stock.count(COBBLE) == 8, "2 日 × 2 次 × 2 块 = 8：" + s.stock.items() + " 容量 " + s.stock.capacity()
                    + " 采集点 " + s.points + " 记录 " + s.log.stream().map(e -> e.key() + e.args()).toList()
                    + " lastReal " + s.lastRealTick + " now " + WorldClock.now(h.getLevel().getServer()));
            h.assertTrue(s.points.get("rock_0").remaining() == 2, "余量 10 → 2");
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(s.stock.count(COBBLE) == 8, "不重复补算");
            h.assertTrue(s.log.stream().anyMatch(e -> e.key().equals("mythic_age.village.log.catch_up")), "写了记录");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-50：重启：读档时工作单托管退回，实体重载丢掉工作单，不重复产出。 */
    @GameTest(template = T, batch = BATCH)
    public static void restart_releases_job_escrow(GameTestHelper h) {
        TV tv = village(h, workshop(h, false, true));
        try {
                        ResidentEntity r = resident(h, tv, 8, 6);
            takeOver(h, tv);
        state(h, tv).stock.deposit(COBBLE, 2);
        VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            h.assertTrue(r.job().isPresent() && state(h, tv).stock.escrowed(COBBLE) == 2, "托管中");
            VillageEconomy eco = VillageEconomy.get(h.getLevel().getServer());
            VillageEconomy reread = VillageEconomy.roundTripForTest(eco, h.getLevel().registryAccess());
            eco.replaceForTest(tv.id(), reread);
            h.assertTrue(state(h, tv).stock.count(COBBLE) == 2 && state(h, tv).stock.escrowed(COBBLE) == 0, "读档退回托管");
            r.discard();
            h.assertTrue(state(h, tv).stock.count(COBBLE) == 2 && state(h, tv).stock.count(BRICKS) == 0, "不重复、不产出");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-51：捐赠：容量剩 5、捐 8 → 收 5 退 3；善恶不变。 */
    @GameTest(template = T, batch = BATCH)
    public static void donation_respects_capacity(GameTestHelper h) {
        TV tv = village(h, Map.of());
        FakePlayer p = player(h, "t19_donor");
        try {
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            VillageEconomy.State s = state(h, tv);
            s.stock.deposit("minecraft:dirt", 59);
            int conduct = MythData.get(p).conductScore();
            ItemStack stack = new ItemStack(Items.COBBLESTONE, 8);
            int got = VillageAffairs.donateForTest(p, tv.id(), stack);
            h.assertTrue(got == 5 && stack.getCount() == 3, "收 5 退 3：" + got + " / " + stack.getCount());
            h.assertTrue(s.seenBy.contains(p.getUUID()), "记了贡献");
            h.assertTrue(MythData.get(p).conductScore() == conduct, "善恶不变");
        } finally {
            OnlinePlayers.forTest(p, false);
            cleanup(h, tv);
        }
        h.succeed();
    }

    /** TC-T19-52（D3）：已建范围里的无业原版村民换成居民；有职业的不动。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void vanilla_jobless_converted(GameTestHelper h) {
        TV tv = village(h, Map.of("home", List.of(abs(h, 4, 2, 10), abs(h, 5, 2, 10))));
        Villager farmer = EntityType.VILLAGER.create(h.getLevel());
        farmer.setVillagerData(farmer.getVillagerData().setType(VillagerType.PLAINS).setProfession(VillagerProfession.FARMER));
        BlockPos fp = h.absolutePos(new BlockPos(12, 2, 10));
        farmer.moveTo(fp.getX() + 0.5, fp.getY(), fp.getZ() + 0.5);
        farmer.setNoAi(true);
        h.getLevel().addFreshEntity(farmer);
        Villager jobless = h.spawn(EntityType.VILLAGER, new BlockPos(4, 2, 12));
        jobless.setNoAi(true);
        h.succeedWhen(() -> {
            h.assertTrue(!jobless.isAlive() || jobless.isRemoved(), "无业村民被换掉");
            h.assertTrue(farmer.isAlive() && !farmer.isRemoved(), "农夫不动");
            VillageEconomy.State s = state(h, tv);
            h.assertTrue(s.roster.size() == 1, "名册 1 人：" + s.roster.size());
            farmer.discard();
            cleanup(h, tv);
        });
    }

    /** TC-T19-53：晚饭扣口粮，同一 MC 日只扣一次。 */
    @GameTest(template = T, batch = BATCH)
    public static void rations_once_per_day(GameTestHelper h) {
        TV tv = village(h, Map.of());
        try {
            List<ResidentEntity> rs = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                rs.add(resident(h, tv, 6 + i, 6));
            }
            VillageScheduler.processNowForTest(h.getLevel(), tv.id());
            VillageEconomy.State s = state(h, tv);
            s.stock.deposit(BREAD, 10);
            VillageScheduler.supperForTest(h.getLevel(), tv.id());
            h.assertTrue(s.stock.count(BREAD) == 7, "3 人扣 3 份：" + s.stock.count(BREAD));
            VillageScheduler.supperForTest(h.getLevel(), tv.id());
            h.assertTrue(s.stock.count(BREAD) == 7, "同一天不再扣");
        } finally {
            cleanup(h, tv);
        }
        h.succeed();
    }
}
