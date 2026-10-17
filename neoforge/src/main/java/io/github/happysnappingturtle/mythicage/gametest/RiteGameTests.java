package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.faith.FaithProfile;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.rite.PromotionTable;
import io.github.happysnappingturtle.mythicage.core.rite.RequirementScope;
import io.github.happysnappingturtle.mythicage.core.rite.RiteKind;
import io.github.happysnappingturtle.mythicage.core.rite.VillageStageTable;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import io.github.happysnappingturtle.mythicage.core.village.Pantheon;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.faith.FaithProfiles;
import io.github.happysnappingturtle.mythicage.faith.FaithService;
import io.github.happysnappingturtle.mythicage.faith.FaithView;
import io.github.happysnappingturtle.mythicage.faith.GraceSource;
import io.github.happysnappingturtle.mythicage.faith.GraceSources;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.guardian.GuardianTypes;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.PlayerDataNbt;
import io.github.happysnappingturtle.mythicage.rite.AgeUnlockedPayload;
import io.github.happysnappingturtle.mythicage.rite.PromotionRequirement;
import io.github.happysnappingturtle.mythicage.rite.PromotionRequirements;
import io.github.happysnappingturtle.mythicage.rite.RiteHooks;
import io.github.happysnappingturtle.mythicage.rite.RiteService;
import io.github.happysnappingturtle.mythicage.rite.RiteTables;
import io.github.happysnappingturtle.mythicage.rite.RitesView;
import io.github.happysnappingturtle.mythicage.rite.TrialService;
import io.github.happysnappingturtle.mythicage.rite.UnlockService;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.statue.PatronStatues;
import io.github.happysnappingturtle.mythicage.statue.StatueCoreBlockEntity;
import io.github.happysnappingturtle.mythicage.statue.StatueTarget;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import io.github.happysnappingturtle.mythicage.trade.TradeView;
import io.github.happysnappingturtle.mythicage.village.PantheonLoader;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * T11a 阶段 ①② 的集成测试：晋升、立祀、改奉、Boss 凭证、兑换地点、主神像、信仰面板（TC-T11a-40～68、90～99）。
 * 测试文明借用埃及的 id，主神表、晋升表、阶段表、交易表与两个可开关的建筑条件只在本批次注入、批次结束清除。
 * 主神 a、b；古典次神 x(a,b)、y(a)、z(b，未开放)；英雄次神 h(a,b)。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RiteGameTests {
    private static final String BATCH = "rite";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final Civilization TC = Civilization.EGYPTIAN;
    private static final Civilization GR = Civilization.GREEK;

    static final AtomicBoolean PLAYER_SWITCH = new AtomicBoolean(true);
    static final AtomicBoolean VILLAGE_SWITCH = new AtomicBoolean(true);

    private static PromotionRequirement requirement(String type, RequirementScope scope, AtomicBoolean on) {
        return new PromotionRequirement() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public RequirementScope scope() {
                return scope;
            }

            @Override
            public Line check(net.minecraft.server.level.ServerPlayer player, Civilization civ, Age target,
                              VillageRegistry.Village village, JsonObject params) {
                return new Line(Component.literal(type), on.get(), Component.literal(on.get() ? "1 / 1" : "0 / 1"));
            }
        };
    }

    private static final String PANTHEON = """
            {"schemaVersion":1,"civilization":"egyptian","patrons":["a","b"],"minorGods":[
             {"id":"x","age":"classical","patrons":["a","b"],"available":true},
             {"id":"y","age":"classical","patrons":["a"],"available":true},
             {"id":"z","age":"classical","patrons":["b"],"available":false},
             {"id":"h","age":"heroic","patrons":["a","b"],"available":true}]}""";
    private static final String PROMOTION = """
            {"schemaVersion":1,"civilization":"egyptian","ages":{
             "classical":{"favor":30,"offerings":[{"item":"minecraft:cobblestone","count":2}],
                          "requirements":[{"type":"test:switch"},{"type":"test:village_switch"}],
                          "extraUnlocks":[{"icon":"minecraft:anvil","key":"mythic_age.age_unlocked.top"}]},
             "heroic":{"favor":60,"offerings":[{"item":"minecraft:gold_ingot","count":1}],"trial":"classical"}},
             "trials":{"classical":["minecraft:zombie"]}}""";
    private static final String STAGES = """
            {"schemaVersion":1,"civilization":"egyptian","stages":{
             "classical":{"population":4,"guards":1,"migrationDays":2},"heroic":{"population":6,"guards":2}}}""";
    private static final String TABLE = """
            {"schemaVersion":1,"civilization":"egyptian","offers":[
             {"id":"bolt_a","category":"god_power","result":{"item":"minecraft:paper","count":1},"price":{"favor":10},
              "minAge":"archaic","venues":["guardian","statue"],"patron":"a","patronDiscount":0.1},
             {"id":"heal_x","category":"god_power","result":{"item":"minecraft:book","count":1},"price":{"favor":10},
              "minAge":"classical","venues":["guardian","statue"],"minorGod":"x","minorGodDiscount":{"x":0.1}},
             {"id":"shield_x","category":"supplies","result":{"item":"minecraft:shield","count":1},"price":{"favor":10},
              "minAge":"classical","venues":["guardian"],"minorGod":"x","minorGodDiscount":{"x":0.1}},
             {"id":"market","category":"supplies","result":{"item":"minecraft:bread","count":1},"price":{"favor":5},
              "minAge":"classical","venues":["guardian"]},
             {"id":"statue","category":"supplies","result":{"item":"mythic_age:patron_statue","count":1},"price":{"favor":5},
              "minAge":"archaic","venues":["guardian"]}]}""";
    private static final FaithProfile PROFILE = new FaithProfile(TC, "entity.mythic_age.greek_guardian",
            "block.minecraft.lodestone", "mythic_age.favor.greek", "", List.of(),
            Optional.empty());

    private RiteGameTests() {
    }

    static PromotionTable promotion(Civilization civ, String json) {
        return PromotionTable.parse(json.replace("\"egyptian\"", "\"" + civ.id() + "\""), id -> OptionalInt.of(64),
                PromotionRequirements::known, new ArrayList<>());
    }

    @BeforeBatch(batch = BATCH)
    public static void before(ServerLevel level) {
        PromotionRequirements.overrideForTest(requirement("test:switch", RequirementScope.PLAYER, PLAYER_SWITCH));
        PromotionRequirements.overrideForTest(requirement("test:village_switch", RequirementScope.VILLAGE, VILLAGE_SWITCH));
        FaithProfiles.overrideForTest(TC, PROFILE);
        GuardianTypes.overrideForTest(TC, ModEntities.GREEK_GUARDIAN);
        PantheonLoader.overrideForTest(Pantheon.parse(PANTHEON));
        RiteTables.overridePromotionForTest(promotion(TC, PROMOTION));
        RiteTables.overridePromotionForTest(promotion(GR, PROMOTION));
        RiteTables.overrideStagesForTest(VillageStageTable.parse(STAGES, new ArrayList<>()));
        TradeTableLoader.overrideForTest(TradeTable.parse(TABLE, TradeTableLoader.ITEMS));
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        PromotionRequirements.overrideForTest(null);
        FaithProfiles.clearTestOverrides();
        GuardianTypes.overrideForTest(TC, null);
        PantheonLoader.overrideForTest(null);
        RiteTables.overridePromotionForTest(null);
        RiteTables.overrideStagesForTest(null);
        TradeTableLoader.overrideForTest(null);
        GraceSources.overrideForTest(TC, null);
        PLAYER_SWITCH.set(true);
        VILLAGE_SWITCH.set(true);
    }

    // ───────────── 搭建 ─────────────

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    /** 一个测试文明村庄（钟只写进登记）与它的祭司（站在 (x, 2, z)）。 */
    private record Site(VillageRegistry.Village village, GuardianEntity priest) {
        VillageRegistry.Village current(GameTestHelper h) {
            return VillageRegistry.get(h.getLevel().getServer()).byId(village.id()).orElseThrow();
        }
    }

    private static Site site(GameTestHelper h, Civilization civ, String patron, Age stage, int x, int z) {
        BlockPos bell = h.absolutePos(new BlockPos(x, 2, z + 1));
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), GlobalPos.of(h.getLevel().dimension(), bell),
                civ.id(), patron, VillageRegistry.Origin.VANILLA, null, "plains", -1, stage, Map.of(), -1, -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(x, 2, z)), Optional.of(bell), civ);
        if (g == null) {
            throw new GameTestAssertException("祭司没有生成");
        }
        g.setNoAi(true);
        return new Site(v, g);
    }

    private static FakePlayer player(GameTestHelper h, String name, Civilization civ, Age age, long favor) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.getInventory().clearContent();
        MythData.get(p).civ(civ).setAge(age);
        MythData.get(p).civ(civ).setFavor(favor);
        return p;
    }

    private static void at(FakePlayer p, Site s) {
        p.moveTo(s.priest().getX() + 1.5, s.priest().getY(), s.priest().getZ());
        TradeService.openGuardian(p, s.priest());
    }

    private static RiteService.Outcome rite(GameTestHelper h, FakePlayer p, Site s, Civilization civ, RiteKind kind, Age age,
                                            String god) {
        at(p, s);
        long favor = RiteService.evaluate(p, civ, s.current(h), kind, age, god).cost().favor();
        return RiteService.perform(p, kind, age, god, favor, UUID.randomUUID().toString());
    }

    private static PlayerMythData.CivState data(FakePlayer p, Civilization civ) {
        return MythData.get(p).civ(civ);
    }

    private static void expect(GameTestHelper h, RiteService.Outcome o, RiteService.Reason r) {
        h.assertTrue(o.reason() == r, "应为 " + r + "，实际 " + o.reason() + " " + o.issues());
    }

    private static int count(FakePlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    private static void cleanup(GameTestHelper h, Site... sites) {
        for (Site s : sites) {
            VillageRegistry.get(h.getLevel().getServer()).remove(s.village().bell());
            s.priest().discard();
        }
        TradeService.clearForTest();
    }

    // ───────────── 晋升（TC-T11a-40～44） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void promotion_happy_path(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        FakePlayer p = player(h, "rt_promo", TC, Age.ARCHAIC, 40);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        at(p, s);
        TradeView view = TradeService.viewForTest(p).orElseThrow();
        RitesView.Option opt = view.rites().orElseThrow().options().getFirst();
        h.assertTrue(opt.kind() == RiteKind.PROMOTION && opt.target() == Age.CLASSICAL && opt.ready() && opt.favor() == 30,
                "晋升页：古典、可举行、30 恩宠：" + opt);
        h.assertTrue(opt.candidates().stream().map(RitesView.Candidate::id).toList().equals(List.of("x", "y")), "候选 x、y");
        h.assertTrue(opt.lines().size() == 4, "恩宠、供品、两条建筑条件：" + opt.lines());
        String req = UUID.randomUUID().toString();
        RiteService.Outcome o = RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "x", 30, req);
        expect(h, o, RiteService.Reason.OK);
        PlayerMythData.CivState d = data(p, TC);
        h.assertTrue(d.age() == Age.CLASSICAL && "x".equals(d.minorGods().get(Age.CLASSICAL)) && d.favor() == 10,
                "玩家：古典、供奉 x、余 10 恩宠：" + d.age() + " " + d.minorGods() + " " + d.favor());
        h.assertTrue(d.approval("a") == Age.CLASSICAL, "主神 a 认可古典");
        h.assertTrue(d.riteVillages().get(s.village().id()).promotedHere(), "记为晋升地");
        h.assertTrue(d.pendingUnlocks().isEmpty(), "成功界面已弹出并清空");
        h.assertTrue(count(p, Items.COBBLESTONE) == 0, "供品已扣");
        VillageRegistry.Village v = s.current(h);
        h.assertTrue(v.stage() == Age.CLASSICAL && v.hasShrine("x"), "村庄古典、立 x 祠：" + v);
        // TC-T11a-41：同一请求再发一次
        expect(h, RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "x", 30, req), RiteService.Reason.DUPLICATE);
        h.assertTrue(data(p, TC).favor() == 10, "不重复扣恩宠");
        cleanup(h, s);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void promotion_rejections(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        FakePlayer p = player(h, "rt_reject", TC, Age.ARCHAIC, 40);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 1));
        at(p, s);
        // TC-T11a-42：供品不全
        RiteService.Outcome o = RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "x", 30, UUID.randomUUID().toString());
        expect(h, o, RiteService.Reason.BLOCKED);
        h.assertTrue(data(p, TC).age() == Age.ARCHAIC && data(p, TC).favor() == 40 && count(p, Items.COBBLESTONE) == 1
                && s.current(h).stage() == Age.ARCHAIC, "什么都不变");
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 1));
        // TC-T11a-43：期望恩宠不符
        expect(h, RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "x", 29, UUID.randomUUID().toString()),
                RiteService.Reason.PRICE_CHANGED);
        // TC-T11a-03：不在候选 / 未开放
        expect(h, RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "z", 30, UUID.randomUUID().toString()),
                RiteService.Reason.BLOCKED);
        // TC-T11a-44：祭司离开
        s.priest().discard();
        expect(h, RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "x", 30, UUID.randomUUID().toString()),
                RiteService.Reason.NPC_GONE);
        h.assertTrue(data(p, TC).favor() == 40 && count(p, Items.COBBLESTONE) == 2, "仍然什么都不变");
        cleanup(h, s);
        h.succeed();
    }

    // ───────────── 立祀（TC-T11a-45～47） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void consecration_half_and_full(GameTestHelper h) {
        floor(h);
        Site v1 = site(h, TC, "a", Age.ARCHAIC, 3, 3);
        Site v2 = site(h, TC, "a", Age.ARCHAIC, 13, 3);
        Site v3 = site(h, TC, "b", Age.ARCHAIC, 8, 20);
        FakePlayer p = player(h, "rt_consecrate", TC, Age.ARCHAIC, 150);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 6));
        expect(h, rite(h, p, v1, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.OK);
        long f = data(p, TC).favor();
        expect(h, rite(h, p, v2, TC, RiteKind.CONSECRATION, Age.NONE, "y"), RiteService.Reason.OK);
        h.assertTrue(f - data(p, TC).favor() == 15, "同一主神已认可：半价 15，实际 " + (f - data(p, TC).favor()));
        f = data(p, TC).favor();
        expect(h, rite(h, p, v3, TC, RiteKind.CONSECRATION, Age.NONE, "x"), RiteService.Reason.OK);
        h.assertTrue(f - data(p, TC).favor() == 30, "不同主神：全价 30");
        h.assertTrue(v2.current(h).stage() == Age.CLASSICAL && v2.current(h).hasShrine("y")
                && v3.current(h).stage() == Age.CLASSICAL, "两村都升古典并立祠");
        h.assertTrue("x".equals(data(p, TC).minorGods().get(Age.CLASSICAL)), "立祀不改变自己的次神");
        h.assertTrue(data(p, TC).approval("b") == Age.CLASSICAL, "主神 b 也认可了");
        h.assertTrue(data(p, TC).pendingUnlocks().isEmpty() && data(p, TC).age() == Age.CLASSICAL, "立祀不弹成功界面、不改时代");
        cleanup(h, v1, v2, v3);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void multiplayer_stage_and_shrines(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        FakePlayer a = player(h, "rt_mp_a", TC, Age.ARCHAIC, 40);
        FakePlayer b = player(h, "rt_mp_b", TC, Age.ARCHAIC, 40);
        a.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        b.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        expect(h, rite(h, a, s, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.OK);
        expect(h, rite(h, b, s, TC, RiteKind.PROMOTION, Age.NONE, "y"), RiteService.Reason.OK);
        VillageRegistry.Village v = s.current(h);
        h.assertTrue(v.stage() == Age.CLASSICAL && v.shrines().get(Age.CLASSICAL).equals(List.of("x", "y")),
                "阶段古典、两座祠：" + v.shrines());
        h.assertTrue("x".equals(data(a, TC).minorGods().get(Age.CLASSICAL)) && "y".equals(data(b, TC).minorGods().get(Age.CLASSICAL)),
                "两人各自的次神");
        // TC-T11a-47：村庄已古典，古典玩家 C 再来立祀 → 超过自己的时代
        FakePlayer c = player(h, "rt_mp_c", TC, Age.CLASSICAL, 99);
        c.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        expect(h, rite(h, c, s, TC, RiteKind.CONSECRATION, Age.NONE, "x"), RiteService.Reason.BLOCKED);
        h.assertTrue(data(c, TC).approvals().isEmpty() && data(c, TC).favor() == 99, "C 没有被扣、没有认可");
        cleanup(h, s);
        h.succeed();
    }

    // ───────────── 改奉、凭证、建筑条件、钩子（TC-T11a-48～51） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void rededication_cooldown(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        FakePlayer p = player(h, "rt_rededicate", TC, Age.CLASSICAL, 99);
        data(p, TC).minorGods().put(Age.CLASSICAL, "x");
        expect(h, rite(h, p, s, TC, RiteKind.REDEDICATION, Age.CLASSICAL, "y"), RiteService.Reason.OK);
        h.assertTrue("y".equals(data(p, TC).minorGods().get(Age.CLASSICAL)) && data(p, TC).favor() == 84,
                "改为 y，扣半价 15：" + data(p, TC).favor());
        h.assertTrue(s.current(h).stage() == Age.ARCHAIC && s.current(h).hasShrine("y"), "村庄阶段不变，立 y 祠");
        expect(h, rite(h, p, s, TC, RiteKind.REDEDICATION, Age.CLASSICAL, "x"), RiteService.Reason.BLOCKED);
        MythDays.advanceForTest(3);
        try {
            expect(h, rite(h, p, s, TC, RiteKind.REDEDICATION, Age.CLASSICAL, "x"), RiteService.Reason.OK);
        } finally {
            MythDays.advanceForTest(-3);
        }
        cleanup(h, s);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void trial_voucher_for_heroic(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.CLASSICAL, 4, 4);
        FakePlayer p = player(h, "rt_trial", TC, Age.CLASSICAL, 99);
        p.getInventory().add(new ItemStack(Items.GOLD_INGOT, 2));
        expect(h, rite(h, p, s, TC, RiteKind.PROMOTION, Age.NONE, "h"), RiteService.Reason.BLOCKED);
        h.assertTrue(TrialService.recordKill(p, "minecraft:zombie").contains(TC), "僵尸是测试文明的古典试炼");
        TrialService.recordKill(p, "minecraft:zombie");
        PlayerMythData d = MythData.get(p);
        h.assertTrue(d.civ(TC).vouchers(Age.CLASSICAL) == 2 && d.trialKills().get("minecraft:zombie") == 2
                && d.firstKills().contains("minecraft:zombie"), "两次击败：凭证 2、计数 2、首杀记一次");
        h.assertTrue(TrialService.recordKill(p, "minecraft:skeleton").isEmpty(), "不是试炼 Boss 的不给凭证");
        expect(h, rite(h, p, s, TC, RiteKind.PROMOTION, Age.NONE, "h"), RiteService.Reason.OK);
        h.assertTrue(data(p, TC).age() == Age.HEROIC && data(p, TC).vouchers(Age.CLASSICAL) == 1, "英雄晋升消耗 1 张凭证");
        cleanup(h, s);
        h.succeed();
    }

    @GameTest(template = T, batch = "rite_switch")
    public static void building_requirements(GameTestHelper h) {
        floor(h);
        RiteGameTests.before(h.getLevel());
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        Site s2 = site(h, TC, "b", Age.ARCHAIC, 12, 20);
        FakePlayer p = player(h, "rt_switch", TC, Age.ARCHAIC, 99);
        FakePlayer q = player(h, "rt_switch_q", TC, Age.CLASSICAL, 99);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        q.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        try {
            PLAYER_SWITCH.set(false);
            expect(h, rite(h, p, s, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.BLOCKED);
            expect(h, rite(h, q, s2, TC, RiteKind.CONSECRATION, Age.NONE, "x"), RiteService.Reason.OK);
            PLAYER_SWITCH.set(true);
            VILLAGE_SWITCH.set(false);
            expect(h, rite(h, p, s, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.BLOCKED);
            VILLAGE_SWITCH.set(true);
            expect(h, rite(h, p, s, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.OK);
        } finally {
            PLAYER_SWITCH.set(true);
            VILLAGE_SWITCH.set(true);
            cleanup(h, s, s2);
            RiteGameTests.after(h.getLevel());
        }
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void after_promotion_hook(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        Site s2 = site(h, TC, "b", Age.ARCHAIC, 12, 20);
        FakePlayer p = player(h, "rt_hook", TC, Age.ARCHAIC, 99);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 4));
        AtomicInteger calls = new AtomicInteger();
        RiteHooks.AfterPromotion counter = (pl, civ, age, god, v) -> {
            if (pl == p) {
                calls.incrementAndGet();
            }
        };
        RiteHooks.AfterPromotion broken = (pl, civ, age, god, v) -> {
            if (pl == p) {
                throw new IllegalStateException("测试钩子故意出错");
            }
        };
        RiteHooks.afterPromotion(counter);
        RiteHooks.afterPromotion(broken);
        try {
            expect(h, rite(h, p, s, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.OK);
            expect(h, rite(h, p, s2, TC, RiteKind.CONSECRATION, Age.NONE, "x"), RiteService.Reason.OK);
            h.assertTrue(calls.get() == 1, "晋升调用 1 次、立祀不调用，实际 " + calls.get());
            h.assertTrue(data(p, TC).age() == Age.CLASSICAL, "钩子出错不回滚仪式");
        } finally {
            RiteHooks.removeForTest(counter);
            RiteHooks.removeForTest(broken);
            cleanup(h, s, s2);
        }
        h.succeed();
    }

    // ───────────── 村庄不对、神坛刷新、希腊（TC-T11a-52～54） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void wrong_or_missing_village(GameTestHelper h) {
        floor(h);
        GuardianEntity loner = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(4, 2, 4)), Optional.empty(), TC);
        loner.setNoAi(true);
        FakePlayer p = player(h, "rt_loner", TC, Age.ARCHAIC, 99);
        p.moveTo(loner.getX() + 1.5, loner.getY(), loner.getZ());
        TradeService.openGuardian(p, loner);
        h.assertTrue(TradeService.viewForTest(p).orElseThrow().rites().isEmpty(), "不属于村庄的祭司没有晋升页");
        Site greek = site(h, GR, "zeus", Age.ARCHAIC, 12, 20);
        GuardianEntity tcPriestInGreek = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(13, 2, 20)),
                Optional.of(greek.village().bell().pos()), TC);
        tcPriestInGreek.setNoAi(true);
        p.moveTo(tcPriestInGreek.getX() + 1.5, tcPriestInGreek.getY(), tcPriestInGreek.getZ());
        TradeService.openGuardian(p, tcPriestInGreek);
        RitesView.Option opt = TradeService.viewForTest(p).orElseThrow().rites().orElseThrow().options().getFirst();
        h.assertTrue(!opt.ready() && !opt.blocking().isEmpty(), "其他文明的村庄不能举行：" + opt.blocking());
        loner.discard();
        tcPriestInGreek.discard();
        cleanup(h, greek);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void shrines_refresh_after_age_change(GameTestHelper h) {
        floor(h);
        BlockPos rel = new BlockPos(8, 2, 2);
        h.setBlock(rel, GreekTemple.GREEK_ALTAR.get().defaultBlockState().setValue(GreekAltarBlock.FACING, Direction.SOUTH));
        FakePlayer p = player(h, "rt_refresh", GR, Age.ARCHAIC, 0);
        BlockPos abs = h.absolutePos(rel);
        ShrineService.bindForTest(h.getLevel(), abs, p.getUUID(), "rt_refresh", Age.ARCHAIC, Direction.SOUTH);
        data(p, GR).setAge(Age.CLASSICAL);
        RiteService.refreshShrines(p, GR);
        h.assertTrue(h.getLevel().getBlockEntity(abs) instanceof GreekAltarBlockEntity be && be.ownerAge() == Age.CLASSICAL,
                "神坛缓存的主人时代立即更新");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void greek_promotion_not_hardcoded(GameTestHelper h) {
        floor(h);
        // 希腊用真实主神表（宙斯：雅典娜、赫尔墨斯）与注入的测试晋升表
        Site s = site(h, GR, "zeus", Age.ARCHAIC, 4, 4);
        FakePlayer p = player(h, "rt_greek", GR, Age.ARCHAIC, 40);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        at(p, s);
        List<String> cands = TradeService.viewForTest(p).orElseThrow().rites().orElseThrow().options().getFirst()
                .candidates().stream().map(RitesView.Candidate::id).toList();
        h.assertTrue(cands.equals(List.of("athena", "hermes")), "宙斯之村的古典候选：" + cands);
        expect(h, rite(h, p, s, GR, RiteKind.PROMOTION, Age.NONE, "athena"), RiteService.Reason.OK);
        h.assertTrue(data(p, GR).age() == Age.CLASSICAL && s.current(h).hasShrine("athena"), "希腊晋升走通");
        cleanup(h, s);
        h.succeed();
    }

    // ───────────── 兑换与视图（TC-T11a-55～58） ─────────────

    private static Optional<TradeView.OfferView> offer(TradeView v, String id) {
        return v.offers().stream().filter(o -> o.id().equals(id)).findFirst();
    }

    @GameTest(template = T, batch = BATCH)
    public static void venue_rules_in_villages(GameTestHelper h) {
        floor(h);
        Site a = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        Site b = site(h, TC, "b", Age.CLASSICAL, 12, 20);
        VillageRegistry.get(h.getLevel().getServer()).update(b.village().withRite(Age.CLASSICAL, Age.CLASSICAL, "x"));
        FakePlayer p = player(h, "rt_venue", TC, Age.ARCHAIC, 99);
        at(p, a);
        TradeView va = TradeService.viewForTest(p).orElseThrow();
        h.assertTrue(offer(va, "bolt_a").isPresent() && offer(va, "bolt_a").get().favorPrice() == 9, "a 村卖 a 的远古神力并九折");
        h.assertTrue(offer(va, "heal_x").isEmpty() && offer(va, "shield_x").isEmpty(), "没有 x 祠：不列 x 的商品");
        h.assertTrue(offer(va, "market").orElseThrow().lock() == TradeReason.STAGE_LOCKED, "远古村庄的古典集市商品锁定");
        at(p, b);
        TradeView vb = TradeService.viewForTest(p).orElseThrow();
        h.assertTrue(offer(vb, "bolt_a").isEmpty(), "b 村不卖 a 的神力");
        h.assertTrue(offer(vb, "heal_x").orElseThrow().lock() == TradeReason.AGE_LOCKED, "x 的神力：远古玩家时代不够");
        h.assertTrue(offer(vb, "shield_x").orElseThrow().lock() == TradeReason.OK
                && offer(vb, "shield_x").get().favorPrice() == 10, "x 祠商品对未供奉者可买、不打折");
        h.assertTrue(offer(vb, "market").orElseThrow().lock() == TradeReason.OK, "古典村庄：远古玩家也能买古典集市商品");
        TradeService.Outcome bought = TradeService.buy(p, "market", 5, UUID.randomUUID().toString());
        h.assertTrue(bought.ok(), "买到：" + bought.reason());
        data(p, TC).setAge(Age.CLASSICAL);
        data(p, TC).minorGods().put(Age.CLASSICAL, "x");
        TradeView vb2 = TradeService.viewForTest(p).orElseThrow();
        h.assertTrue(offer(vb2, "heal_x").orElseThrow().lock() == TradeReason.OK && offer(vb2, "heal_x").get().favorPrice() == 9,
                "供奉 x 的古典玩家在 x 祠村庄九折");
        cleanup(h, a, b);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void views_round_trip(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        FakePlayer p = player(h, "rt_codec", TC, Age.ARCHAIC, 40);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        at(p, s);
        TradeView v = TradeService.viewForTest(p).orElseThrow();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        TradeView.STREAM_CODEC.encode(buf, v);
        TradeView back = TradeView.STREAM_CODEC.decode(buf);
        h.assertTrue(back.rites().isPresent() && back.rites().get().options().size() == v.rites().get().options().size()
                && back.offers().size() == v.offers().size(), "TradeView（含晋升页）往返");
        // 成功界面：清单与编解码
        expect(h, rite(h, p, s, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.OK);
        data(p, TC).pendingUnlocks().add(Age.CLASSICAL);
        List<AgeUnlockedPayload> sent = UnlockService.flush(p);
        h.assertTrue(sent.size() == 1 && data(p, TC).pendingUnlocks().isEmpty(), "补弹一次后清空");
        AgeUnlockedPayload u = sent.getFirst();
        h.assertTrue(u.powers().stream().anyMatch(st -> st.is(Items.BOOK)) && u.powers().stream().noneMatch(st -> st.is(Items.PAPER)),
                "古典神力只列所选次神 x 的：" + u.powers());
        h.assertTrue(u.caps().stream().anyMatch(c -> c.key().equals("favor_cap") && c.before() == 100 && c.after() == 150),
                "恩宠上限 100 → 150：" + u.caps());
        h.assertTrue(u.extras().size() == 1 && u.next().isPresent() && u.next().get().favor() == 60, "额外项与下一时代预告");
        RegistryFriendlyByteBuf ub = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        AgeUnlockedPayload.STREAM_CODEC.encode(ub, u);
        AgeUnlockedPayload ub2 = AgeUnlockedPayload.STREAM_CODEC.decode(ub);
        h.assertTrue(ub2.caps().equals(u.caps()) && ub2.powers().size() == u.powers().size() && ub2.next().isPresent(), "往返一致");
        cleanup(h, s);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void village_stage_command(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        BlockPos bell = s.village().bell().pos();
        h.getLevel().getServer().getCommands().performPrefixedCommand(h.getLevel().getServer().createCommandSourceStack()
                .withLevel(h.getLevel()).withPermission(4).withSuppressedOutput(),
                "mythicage village stage " + bell.getX() + " " + bell.getY() + " " + bell.getZ() + " heroic");
        h.assertTrue(s.current(h).stage() == Age.HEROIC, "命令设定村庄阶段：" + s.current(h).stage());
        cleanup(h, s);
        h.succeed();
    }

    // ───────────── 存档（TC-T11a-60～62） ─────────────

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void persistence(GameTestHelper h) {
        PlayerMythData d = new PlayerMythData();
        PlayerMythData.CivState s = d.civ(TC);
        s.setAge(Age.HEROIC);
        s.minorGods().put(Age.CLASSICAL, "x");
        s.approve("a", Age.HEROIC);
        s.setVouchers(Age.CLASSICAL, 2);
        s.setRededicatedDay(7);
        s.markRiteVillage(UUID.randomUUID(), Age.CLASSICAL, true);
        s.pendingUnlocks().add(Age.HEROIC);
        d.trialKills().put("minecraft:zombie", 3);
        PlayerMythData back = PlayerDataNbt.read(PlayerDataNbt.write(d));
        h.assertTrue(back.equals(d), "格式 2 往返一致");
        CompoundTag old = PlayerDataNbt.write(new PlayerMythData());
        old.putInt("format", 1);
        old.remove("trialKills");
        h.assertTrue(PlayerDataNbt.read(old).civ(TC).approvals().isEmpty(), "格式 1 读出新字段为空");
        // 村庄登记
        VillageRegistry reg = new VillageRegistry();
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(),
                GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 2, 3)), "egyptian", "a", VillageRegistry.Origin.VANILLA, null,
                "plains", -1).withRite(Age.CLASSICAL, Age.CLASSICAL, "x").withPopulation(5, 12);
        reg.add(v);
        CompoundTag tag = reg.save(new CompoundTag(), h.getLevel().registryAccess());
        VillageRegistry.Village v2 = VillageRegistry.read(tag, h.getLevel().registryAccess()).byId(v.id()).orElseThrow();
        h.assertTrue(v2.equals(v), "村庄新字段往返：" + v2);
        CompoundTag legacy = tag.getList("villages", 10).getCompound(0);
        legacy.remove("stage");
        legacy.remove("shrines");
        legacy.remove("baseline");
        legacy.remove("lastMigration");
        h.assertTrue(VillageRegistry.read(tag, h.getLevel().registryAccess()).byId(v.id()).orElseThrow().stage() == Age.ARCHAIC,
                "旧登记：有文明 → 远古");
        h.succeed();
    }

    // ───────────── 信仰面板（TC-T11a-63～68） ─────────────

    private static FaithView.Page page(FaithView v, Civilization civ) {
        return v.pages().get(civ.ordinal());
    }

    @GameTest(template = T, batch = BATCH)
    public static void faith_panel_pages(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        FakePlayer p = player(h, "rt_faith", TC, Age.ARCHAIC, 40);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 2));
        // TC-T11a-64：远古时，下一次是古典——恩宠、供品、玩家范围的条件；村庄范围的只给一行提示
        FaithView.Page before = page(FaithService.build(p), TC);
        h.assertTrue(before.nextAge() == Age.CLASSICAL && before.next().size() == 4 && before.next().get(0).met()
                && before.next().get(1).met() && before.next().get(2).label().getString().equals("test:switch"),
                "远古 → 古典的条件行：" + before.next());
        expect(h, rite(h, p, s, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.OK);
        FaithView v = FaithService.build(p);
        // TC-T11a-63：页签状态
        FaithView.Page tc = page(v, TC);
        h.assertTrue(tc.status() == FaithView.Status.CONTACTED, "测试文明已接触");
        h.assertTrue(page(v, GR).status() == FaithView.Status.UNCONTACTED && !page(v, GR).hint().isEmpty(),
                "希腊未接触，只有线索：" + page(v, GR));
        h.assertTrue(page(v, Civilization.NORSE).status() == FaithView.Status.UNOPENED, "北欧没有档案：未开放");
        // TC-T11a-64：通用区
        h.assertTrue(tc.age() == Age.CLASSICAL && tc.favor() == 10 && tc.cap() == MythData.cap(p, TC)
                && tc.minorGods().equals(List.of("x", "", "")), "时代、恩宠、次神：" + tc);
        h.assertTrue(tc.approvals().equals(List.of(new FaithView.Approval("a", Age.CLASSICAL), new FaithView.Approval("b", Age.NONE))),
                "主神认可按主神表逐位：" + tc.approvals());
        h.assertTrue(tc.nextAge() == Age.HEROIC && tc.next().size() == 3 && tc.next().stream().noneMatch(FaithView.Line::met)
                && tc.next().get(0).progress().getString().equals("10 / 60") && tc.next().get(1).icon().is(Items.GOLD_INGOT)
                && tc.next().get(2).progress().getString().equals("0 / 1"), "古典 → 英雄：恩宠、金锭、凭证：" + tc.next());
        // TC-T11a-67：顶栏按全部文明的最高时代
        h.assertTrue(v.mythicUnits() == 3 && v.relicSlots() == 1, "古典：神话单位 3、圣物栏 1：" + v.mythicUnits() + " " + v.relicSlots());
        data(p, GR).setAge(Age.HEROIC);
        FaithView v2 = FaithService.build(p);
        h.assertTrue(v2.mythicUnits() == 4 && v2.relicSlots() == 2, "希腊英雄：4、2");
        h.assertTrue(page(v2, GR).status() == FaithView.Status.CONTACTED && page(v2, GR).nextAge() == Age.NONE
                && page(v2, GR).next().isEmpty(), "晋升表没有神话：不显示下一次");
        cleanup(h, s);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void faith_panel_villages_and_codec(GameTestHelper h) {
        floor(h);
        Site v1 = site(h, TC, "a", Age.ARCHAIC, 3, 3);
        Site v2 = site(h, TC, "a", Age.ARCHAIC, 13, 3);
        Site v3 = site(h, TC, "b", Age.ARCHAIC, 8, 20);
        FakePlayer p = player(h, "rt_faith_v", TC, Age.ARCHAIC, 150);
        p.getInventory().add(new ItemStack(Items.COBBLESTONE, 6));
        expect(h, rite(h, p, v1, TC, RiteKind.PROMOTION, Age.NONE, "x"), RiteService.Reason.OK);
        expect(h, rite(h, p, v2, TC, RiteKind.CONSECRATION, Age.NONE, "y"), RiteService.Reason.OK);
        // 测试来源：v3 有神坛；另一行指向 v1（应去重）
        GraceSource src = new GraceSource() {
            @Override
            public Civilization civ() {
                return TC;
            }

            @Override
            public String id() {
                return TC.id() + ":test";
            }

            @Override
            public List<Row> rows(net.minecraft.server.level.ServerPlayer viewer) {
                return List.of(new Row(Component.literal("s3"), v3.village().bell(), Component.literal("ok"), 2.5,
                                Optional.of(v3.village().id())),
                        new Row(Component.literal("s1"), v1.village().bell(), Component.literal("ok"), 1.0,
                                Optional.of(v1.village().id())));
            }
        };
        GraceSources.overrideForTest(TC, List.of(src));
        try {
            // TC-T11a-65：你的村庄
            FaithView v = FaithService.build(p);
            FaithView.Page tc = page(v, TC);
            h.assertTrue(tc.villages().stream().map(FaithView.Row::pos).toList().equals(List.of(
                    v1.village().bell().pos(), v2.village().bell().pos(), v3.village().bell().pos())), "三个村庄、不重复：" + tc.villages());
            List<String> notes = tc.villages().stream()
                    .map(r -> ((net.minecraft.network.chat.contents.TranslatableContents) r.note().getContents()).getKey()).toList();
            h.assertTrue(notes.equals(List.of("mythic_age.faith.village.promoted", "mythic_age.faith.village.rite",
                    "mythic_age.faith.village.shrine")), "晋升 / 立祀 / 神坛：" + notes);
            h.assertTrue(tc.sources().size() == 2 && tc.sources().get(0).pos().equals(v3.village().bell().pos()),
                    "恩宠来源区按来源列出");
            // TC-T11a-68：编解码往返
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            FaithView.STREAM_CODEC.encode(buf, v);
            FaithView back = FaithView.STREAM_CODEC.decode(buf);
            FaithView.Page b = page(back, TC);
            h.assertTrue(back.mythicUnits() == v.mythicUnits() && back.relicSlots() == v.relicSlots()
                    && back.pages().size() == v.pages().size()
                    && back.pages().stream().map(FaithView.Page::status).toList()
                    .equals(v.pages().stream().map(FaithView.Page::status).toList()), "顶栏与页签往返");
            h.assertTrue(b.age() == tc.age() && b.favor() == tc.favor() && b.cap() == tc.cap() && b.favorKey().equals(tc.favorKey())
                    && b.minorGods().equals(tc.minorGods()) && b.approvals().equals(tc.approvals()) && b.nextAge() == tc.nextAge()
                    && b.next().size() == tc.next().size() && b.next().get(1).icon().is(Items.GOLD_INGOT)
                    && b.villages().stream().map(FaithView.Row::pos).toList().equals(tc.villages().stream().map(FaithView.Row::pos).toList())
                    && b.sources().size() == tc.sources().size(), "已接触页往返一致");
            // 村庄注销后：「村庄已消失」
            VillageRegistry.get(h.getLevel().getServer()).remove(v2.village().bell());
            FaithView.Row gone = page(FaithService.build(p), TC).villages().get(1);
            h.assertTrue(!gone.hasPos() && ((net.minecraft.network.chat.contents.TranslatableContents) gone.name().getContents())
                    .getKey().equals("mythic_age.faith.village_gone"), "村庄已消失：" + gone);
        } finally {
            GraceSources.overrideForTest(TC, null);
        }
        cleanup(h, v1, v2, v3);
        h.succeed();
    }

    // ───────────── 主神像（TC-T11a-90～99） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void statue_purchase_needs_approval(GameTestHelper h) {
        floor(h);
        Site s = site(h, TC, "a", Age.ARCHAIC, 4, 4);
        FakePlayer p = player(h, "rt_statue_buy", TC, Age.CLASSICAL, 99);
        at(p, s);
        h.assertTrue(offer(TradeService.viewForTest(p).orElseThrow(), "statue").orElseThrow().lock() == TradeReason.NEED_APPROVAL,
                "未被 a 认可：锁定");
        data(p, TC).approve("a", Age.CLASSICAL);
        TradeService.Outcome o = TradeService.buy(p, "statue", 5, UUID.randomUUID().toString());
        h.assertTrue(o.ok() && new StatueTarget(TC, "a").equals(o.given().get(PatronStatues.TARGET.get())),
                "买到带主神 a 的神像：" + o.reason());
        h.assertTrue(offer(TradeService.viewForTest(p).orElseThrow(), "statue").orElseThrow().lock() == TradeReason.HAS_STATUE,
                "已有一座：不能再买");
        cleanup(h, s);
        h.succeed();
    }

    private static BlockPos placeStatue(GameTestHelper h, FakePlayer p, BlockPos rel, Direction facing) {
        PatronStatues.PlaceResult r = PatronStatues.place(p, h.getLevel(), new StatueTarget(TC, "a"), h.absolutePos(rel), facing);
        h.assertTrue(r == PatronStatues.PlaceResult.OK, "放置：" + r);
        return h.absolutePos(rel.above());
    }

    @GameTest(template = T, batch = BATCH)
    public static void statue_placement_and_facings(GameTestHelper h) {
        floor(h);
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int i = 0; i < dirs.length; i++) {
            FakePlayer p = player(h, "rt_statue_dir" + i, TC, Age.CLASSICAL, 0);
            BlockPos core = placeStatue(h, p, new BlockPos(4 + (i % 2) * 7, 2, 4 + (i / 2) * 10), dirs[i]);
            h.assertTrue(h.getLevel().getBlockEntity(core) instanceof StatueCoreBlockEntity be && be.intact()
                            && be.owner().orElseThrow().equals(p.getUUID()) && be.parts().size() == 9 + 2,
                    "朝向 " + dirs[i] + "：核心、9 块底座、2 个部件");
            h.assertTrue(h.getLevel().getBlockState(core).getValue(io.github.happysnappingturtle.mythicage.statue.StatueCoreBlock.FACING)
                    == dirs[i], "核心朝向 " + dirs[i]);
            h.assertTrue(h.getLevel().getBlockState(core.above()).is(PatronStatues.PART.get())
                    && h.getLevel().getBlockState(core.above(2)).is(PatronStatues.PART.get()), "核心上方两格是部件");
            h.assertTrue(ShrineRegistry.get(h.getLevel().getServer()).countOwned(p.getUUID(), PatronStatues.kindId(TC, "a")) == 1, "已登记");
            PatronStatues.PlaceResult again = PatronStatues.place(p, h.getLevel(), new StatueTarget(TC, "a"),
                    h.absolutePos(new BlockPos(8, 2, 30)), dirs[i]);
            h.assertTrue(again == PatronStatues.PlaceResult.LIMIT, "每位主神 1 座：" + again);
        }
        FakePlayer q = player(h, "rt_statue_space", TC, Age.CLASSICAL, 0);
        h.setBlock(new BlockPos(8, 4, 25), Blocks.STONE);
        h.assertTrue(PatronStatues.place(q, h.getLevel(), new StatueTarget(TC, "a"), h.absolutePos(new BlockPos(8, 2, 25)),
                Direction.NORTH) == PatronStatues.PlaceResult.NO_SPACE, "范围内有实心方块：空间不足");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void statue_integrity_and_trade(GameTestHelper h) {
        floor(h);
        FakePlayer p = player(h, "rt_statue_use", TC, Age.CLASSICAL, 99);
        data(p, TC).minorGods().put(Age.CLASSICAL, "x");
        BlockPos core = placeStatue(h, p, new BlockPos(8, 2, 8), Direction.NORTH);
        p.moveTo(core.getX() + 0.5, core.getY(), core.getZ() + 2.5);
        h.assertTrue(TradeService.openStatue(p, core), "主人在完整的神像前打开兑换");
        TradeView v = TradeService.viewForTest(p).orElseThrow();
        h.assertTrue(v.venue() == TradeOffer.Venue.STATUE && offer(v, "bolt_a").isPresent() && offer(v, "heal_x").isPresent()
                && offer(v, "heal_x").get().favorPrice() == 10 && offer(v, "market").isEmpty() && offer(v, "statue").isEmpty(),
                "主神像：a 的神力、x 的神力（不打次神折扣），不卖补给");
        // 拆 1 块底座 → 受损
        BlockPos base = core.below().east();
        h.getLevel().setBlockAndUpdate(base, Blocks.AIR.defaultBlockState());
        // 部件被替换成别的方块
        h.getLevel().setBlockAndUpdate(core.above(2), Blocks.AIR.defaultBlockState());
        h.runAfterDelay(2, () -> {
            StatueCoreBlockEntity be = (StatueCoreBlockEntity) h.getLevel().getBlockEntity(core);
            h.assertTrue(be != null && !be.intact() && be.missing().equals(List.of(base)), "缺 1 块底座：" + (be == null ? null : be.missing()));
            h.assertTrue(h.getLevel().getBlockState(core.above(2)).is(PatronStatues.PART.get()), "部件由核心补回");
            h.assertTrue(!TradeService.openStatue(p, core), "受损不能兑换");
            h.getLevel().setBlockAndUpdate(base, be.parts().stream().filter(pt -> core.offset(pt.offset()).equals(base))
                    .findFirst().orElseThrow().state());
            h.runAfterDelay(2, () -> {
                h.assertTrue(((StatueCoreBlockEntity) h.getLevel().getBlockEntity(core)).intact(), "放回同种方块即修复");
                h.succeed();
            });
        });
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void statue_protection_and_removal(GameTestHelper h) {
        floor(h);
        FakePlayer owner = player(h, "rt_statue_owner", TC, Age.CLASSICAL, 0);
        FakePlayer other = player(h, "rt_statue_other", TC, Age.CLASSICAL, 0);
        BlockPos core = placeStatue(h, owner, new BlockPos(8, 2, 8), Direction.NORTH);
        BlockPos base = core.below().west();
        h.assertTrue(!other.gameMode.destroyBlock(base) && !other.gameMode.destroyBlock(core.above())
                && !other.gameMode.destroyBlock(core), "非主人拆底座、部件、核心都被拒绝");
        h.getLevel().explode(null, core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 2.5, 3.0f, Level.ExplosionInteraction.TNT);
        h.assertTrue(!h.getLevel().getBlockState(base).isAir() && h.getLevel().getBlockState(core).is(PatronStatues.CORE.get()),
                "爆炸不破坏神像");
        // 存档往返
        StatueCoreBlockEntity be = (StatueCoreBlockEntity) h.getLevel().getBlockEntity(core);
        CompoundTag tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
        StatueCoreBlockEntity copy = new StatueCoreBlockEntity(core, be.getBlockState());
        copy.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(copy.parts().equals(be.parts()) && copy.patron().equals("a") && copy.civ() == TC, "核心存档往返");
        h.assertTrue(owner.gameMode.destroyBlock(core), "主人拆核心");
        h.assertTrue(h.getLevel().getBlockState(core.above()).isAir() && h.getLevel().getBlockState(core.above(2)).isAir()
                && !h.getLevel().getBlockState(base).isAir(), "部件消失、底座保留");
        h.assertTrue(ShrineRegistry.get(h.getLevel().getServer()).countOwned(owner.getUUID(), PatronStatues.kindId(TC, "a")) == 0,
                "登记注销");
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(core).inflate(3));
        h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(PatronStatues.ITEM.get())
                && new StatueTarget(TC, "a").equals(e.getItem().get(PatronStatues.TARGET.get()))), "道具返还");
        drops.forEach(ItemEntity::discard);
        h.succeed();
    }
}
