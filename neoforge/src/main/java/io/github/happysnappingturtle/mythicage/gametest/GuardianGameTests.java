package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.guardian.GreekGuardianEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.player.PlayerDataNbt;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * T9 守护者与兑换的集成测试（TC-T9-40～62）。交易请求直接调用 {@link TradeService}，绕过网络层；
 * 测试商品通过 {@link TradeTableLoader#overrideForTest} 注入，批次结束后恢复。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuardianGameTests {
    private static final String BATCH = "guardian";
    private static final String T = GameTestTemplates.EMPTY_11X11;
    private static final Civilization GR = Civilization.GREEK;
    /** 正式表的奠酒 + 两件恩宠测试商品（产物用原版物品，避免依赖 T13 的神力道具）。 */
    private static final String TEST_TABLE = """
            {"schemaVersion":1,"civilization":"greek","offers":[
             {"id":"libation","category":"supplies","result":{"item":"mythic_age:libation","count":2},
              "price":{"items":[{"item":"minecraft:emerald","count":1}]},"minAge":"archaic","venues":["guardian"],"dailyLimit":8},
             {"id":"test_bolt","category":"god_power","result":{"item":"minecraft:paper","count":1},
              "price":{"favor":20},"minAge":"archaic","venues":["guardian"]},
             {"id":"test_classical","category":"summon","result":{"item":"minecraft:book","count":1},
              "price":{"favor":10},"minAge":"classical","venues":["guardian","altar"]}]}""";

    private GuardianGameTests() {
    }

    @BeforeBatch(batch = BATCH)
    public static void before(ServerLevel level) {
        TradeTableLoader.overrideForTest(TradeTable.parse(TEST_TABLE, TradeTableLoader.ITEMS));
    }

    @AfterBatch(batch = BATCH)
    public static void after(ServerLevel level) {
        TradeTableLoader.overrideForTest(null);
    }

    // ───────────── 搭建 ─────────────

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                h.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    private static GuardianEntity guardian(GameTestHelper h) {
        floor(h);
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), Optional.empty(), Civilization.GREEK);
        if (g == null) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("祭司没有生成");
        }
        g.setNoAi(true);
        return g;
    }

    private static FakePlayer player(GameTestHelper h, GuardianEntity g, String name) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.getInventory().clearContent();
        p.moveTo(g.getX() + 2, g.getY(), g.getZ());
        return p;
    }

    private static PlayerMythData data(FakePlayer p) {
        return MythData.get(p);
    }

    private static FakePlayer blessed(GameTestHelper h, GuardianEntity g, String name, long favor) {
        FakePlayer p = player(h, g, name);
        data(p).civ(GR).setAge(Age.ARCHAIC);
        data(p).civ(GR).setFavor(favor);
        TradeService.openGuardian(p, g);
        return p;
    }

    private static int count(FakePlayer p, net.minecraft.world.item.Item item) {
        return p.getInventory().countItem(item);
    }

    private static void fill(FakePlayer p) {
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            p.getInventory().items.set(i, new ItemStack(Blocks.DIRT, 64));
        }
    }

    private static TradeService.Outcome buy(FakePlayer p, String offer, long price) {
        return TradeService.buy(p, offer, price, UUID.randomUUID().toString());
    }

    private static void expect(GameTestHelper h, TradeService.Outcome o, TradeReason reason) {
        h.assertTrue(o.reason() == reason, "应为 " + reason + "，实际 " + o.reason());
    }

    // ───────────── 初见（TC-T9-40～43） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void first_blessing_gives_altar_and_archaic(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = player(h, g, "ma_bless");
        TradeService.openGuardian(p, g);
        expect(h, TradeService.blessing(p, false), TradeReason.OK);
        h.assertTrue(data(p).civ(GR).age() == Age.ARCHAIC, "进入远古");
        h.assertTrue(count(p, GreekTemple.GREEK_ALTAR.get().asItem()) == 1, "得到 1 座祭坛");
        h.assertTrue(data(p).civ(GR).favor() == 0, "A1：不送恩宠");
        // TC-T9-41：重复请求
        expect(h, TradeService.blessing(p, false), TradeReason.DUPLICATE);
        h.assertTrue(count(p, GreekTemple.GREEK_ALTAR.get().asItem()) == 1, "重复请求不再给祭坛");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void first_blessing_needs_room(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = player(h, g, "ma_full");
        fill(p);
        TradeService.openGuardian(p, g);
        expect(h, TradeService.blessing(p, false), TradeReason.INVENTORY_FULL);
        h.assertTrue(data(p).civ(GR).age() == Age.NONE, "满背包时不提交");
        p.getInventory().items.set(0, ItemStack.EMPTY);
        expect(h, TradeService.blessing(p, false), TradeReason.OK);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void two_players_bless_independently(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer a = player(h, g, "ma_a");
        FakePlayer b = player(h, g, "ma_b");
        TradeService.openGuardian(a, g);
        TradeService.openGuardian(b, g);
        expect(h, TradeService.blessing(a, false), TradeReason.OK);
        expect(h, TradeService.blessing(b, false), TradeReason.OK);
        h.assertTrue(count(a, GreekTemple.GREEK_ALTAR.get().asItem()) == 1
                && count(b, GreekTemple.GREEK_ALTAR.get().asItem()) == 1, "各得 1 座");
        h.succeed();
    }

    // ───────────── 交易（TC-T9-44～54） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void favor_trade(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = blessed(h, g, "ma_favor", 30);
        TradeService.Outcome o = buy(p, "test_bolt", 20);
        expect(h, o, TradeReason.OK);
        h.assertTrue(data(p).civ(GR).favor() == 10, "恩宠 −20");
        h.assertTrue(count(p, Items.PAPER) == 1, "产物入包");
        // TC-T9-45：恩宠不足
        TradeService.Outcome poor = buy(p, "test_bolt", 20);
        expect(h, poor, TradeReason.NOT_ENOUGH_FAVOR);
        h.assertTrue(poor.arg() == 10, "还差 10，实际 " + poor.arg());
        h.assertTrue(count(p, Items.PAPER) == 1 && data(p).civ(GR).favor() == 10, "不足时不变");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void age_locked_trade(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = blessed(h, g, "ma_age", 99);
        expect(h, buy(p, "test_classical", 10), TradeReason.AGE_LOCKED);
        h.assertTrue(data(p).civ(GR).favor() == 99, "恩宠不变");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void full_inventory_trade(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = blessed(h, g, "ma_full2", 50);
        fill(p);
        int receipts = data(p).receipts().size(io.github.happysnappingturtle.mythicage.core.player.ReceiptBook.Scope.REQUEST);
        expect(h, buy(p, "test_bolt", 20), TradeReason.INVENTORY_FULL);
        h.assertTrue(data(p).civ(GR).favor() == 50, "恩宠不变");
        h.assertTrue(data(p).receipts().size(io.github.happysnappingturtle.mythicage.core.player.ReceiptBook.Scope.REQUEST)
                == receipts, "不写收据");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void item_price_trade_and_daily_limit(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = blessed(h, g, "ma_libation", 0);
        p.getInventory().add(new ItemStack(Items.EMERALD, 20));
        expect(h, buy(p, "libation", 0), TradeReason.OK);
        h.assertTrue(count(p, Items.EMERALD) == 19, "绿宝石 −1");
        h.assertTrue(count(p, io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.LIBATION.get()) == 2, "奠酒 +2");
        for (int i = 1; i < 8; i++) {
            expect(h, buy(p, "libation", 0), TradeReason.OK);
        }
        expect(h, buy(p, "libation", 0), TradeReason.DAILY_LIMIT);
        MythDays.advanceForTest(1);
        try {
            expect(h, buy(p, "libation", 0), TradeReason.OK);
        } finally {
            MythDays.advanceForTest(-1);
        }
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void item_price_missing(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = blessed(h, g, "ma_noemerald", 0);
        expect(h, buy(p, "libation", 0), TradeReason.NOT_ENOUGH_ITEMS);
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void duplicate_request_settles_once(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = blessed(h, g, "ma_dup", 60);
        String id = UUID.randomUUID().toString();
        expect(h, TradeService.buy(p, "test_bolt", 20, id), TradeReason.OK);
        expect(h, TradeService.buy(p, "test_bolt", 20, id), TradeReason.DUPLICATE);
        h.assertTrue(data(p).civ(GR).favor() == 40 && count(p, Items.PAPER) == 1, "只结算一次");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void npc_gone_when_far_or_dead(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = blessed(h, g, "ma_gone", 60);
        g.teleportTo(g.getX() + 20, g.getY(), g.getZ());
        expect(h, buy(p, "test_bolt", 20), TradeReason.NPC_GONE);
        h.assertFalse(TradeService.hasSession(p), "会话已关闭");
        g.teleportTo(p.getX() + 1, p.getY(), p.getZ());
        TradeService.openGuardian(p, g);
        g.kill();
        expect(h, buy(p, "test_bolt", 20), TradeReason.NPC_GONE);
        h.assertTrue(data(p).civ(GR).favor() == 60, "恩宠不变");
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void unknown_offer_and_price_changed(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = blessed(h, g, "ma_hot", 60);
        expect(h, buy(p, "removed_offer", 20), TradeReason.UNKNOWN_OFFER);
        TradeService.Outcome o = buy(p, "test_bolt", 25);
        expect(h, o, TradeReason.PRICE_CHANGED);
        h.assertTrue(o.arg() == 20, "下发新价格 20，实际 " + o.arg());
        h.succeed();
    }

    // ───────────── 补领（TC-T9-55～56） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void reissue_once_per_day(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = player(h, g, "ma_reissue");
        TradeService.openGuardian(p, g);
        expect(h, TradeService.blessing(p, true), TradeReason.NOT_BLESSED);
        expect(h, TradeService.blessing(p, false), TradeReason.OK);
        expect(h, TradeService.blessing(p, true), TradeReason.HAS_SHRINE);
        p.getInventory().clearContent();
        expect(h, TradeService.blessing(p, true), TradeReason.OK);
        p.getInventory().clearContent();
        expect(h, TradeService.blessing(p, true), TradeReason.REISSUED_TODAY);
        MythDays.advanceForTest(1);
        try {
            expect(h, TradeService.blessing(p, true), TradeReason.OK);
        } finally {
            MythDays.advanceForTest(-1);
        }
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH)
    public static void reissue_refused_with_placed_altar(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        FakePlayer p = player(h, g, "ma_placed");
        TradeService.openGuardian(p, g);
        expect(h, TradeService.blessing(p, false), TradeReason.OK);
        p.getInventory().clearContent();
        BlockPos rel = new BlockPos(1, 1, 1);
        h.setBlock(rel, GreekTemple.GREEK_ALTAR.get().defaultBlockState().setValue(GreekAltarBlock.FACING, Direction.SOUTH));
        ShrineService.bindForTest(h.getLevel(), h.absolutePos(rel), p.getUUID(), "ma_placed", Age.ARCHAIC, net.minecraft.core.Direction.SOUTH);
        expect(h, TradeService.blessing(p, true), TradeReason.HAS_SHRINE);
        h.succeed();
    }

    // ───────────── 村庄出现（TC-T9-57～58） ─────────────

    @GameTest(template = T, batch = "guardian_village")
    public static void village_bell_gets_one_priest(GameTestHelper h) {
        floor(h);
        BlockPos rel = new BlockPos(5, 1, 5);
        h.setBlock(rel, Blocks.BELL);
        BlockPos bell = h.absolutePos(rel);
        h.runAfterDelay(2, () -> {
            // T10a：祭司只在已登记的希腊村庄生成；测试结构里放不下原版村庄，注入「这个钟属于平原村庄」
            io.github.happysnappingturtle.mythicage.village.VillageResolver.overrideTypeForTest(
                    (l, b) -> b.equals(bell) ? Optional.of("plains") : Optional.empty());
            try {
                GuardianSpawner.spawnForArea(h.getLevel(), bell);
                GuardianSpawner.spawnForArea(h.getLevel(), bell);
            } finally {
                io.github.happysnappingturtle.mythicage.village.VillageResolver.overrideTypeForTest(null);
            }
            Optional<GuardianRegistry.Entry> e = GuardianRegistry.get(h.getLevel().getServer()).byBell(h.getLevel().dimension(), bell);
            h.assertTrue(e.isPresent(), "钟旁应登记 1 位祭司");
            long near = h.getLevel().getEntitiesOfClass(GuardianEntity.class,
                    new net.minecraft.world.phys.AABB(bell).inflate(4)).size();
            h.assertTrue(near == 1, "钟旁只有 1 位祭司，实际 " + near);
            h.succeed();
        });
    }

    @GameTest(template = T, batch = "guardian_village_off")
    public static void village_spawn_respects_civ_and_config(GameTestHelper h) {
        floor(h);
        BlockPos rel = new BlockPos(5, 1, 5);
        h.setBlock(rel, Blocks.BELL);
        BlockPos bell = h.absolutePos(rel);
        ConfigHolder<WorldRulesConfig> holder = ModConfigs.worldRules();
        WorldRulesConfig original = holder.current();
        h.runAfterDelay(2, () -> {
            try {
                setGuardians(holder, original, new WorldRulesConfig.Guardians(false, 1, 8));
                GuardianSpawner.spawnForArea(h.getLevel(), bell);
                h.assertTrue(GuardianRegistry.get(h.getLevel().getServer()).byBell(h.getLevel().dimension(), bell).isEmpty(),
                        "配置关闭时不生成");
                setGuardians(holder, original, original.guardians());
                // 沙漠村庄 → 埃及（尚未实现）：登记但不生成希腊祭司
                io.github.happysnappingturtle.mythicage.village.VillageResolver.overrideTypeForTest(
                        (l, b) -> b.equals(bell) ? Optional.of("desert") : Optional.empty());
                GuardianSpawner.trySpawnNear(h.getLevel(), bell);
                h.assertTrue(GuardianRegistry.get(h.getLevel().getServer()).byBell(h.getLevel().dimension(), bell).isEmpty(),
                        "非希腊村庄的钟不生成祭司");
            } finally {
                io.github.happysnappingturtle.mythicage.village.VillageResolver.overrideTypeForTest(null);
                setGuardians(holder, original, original.guardians());
            }
            h.succeed();
        });
    }

    private static void setGuardians(ConfigHolder<WorldRulesConfig> holder, WorldRulesConfig base, WorldRulesConfig.Guardians g) {
        WorldRulesConfig next = base.withGuardians(g);
        holder.reload(ConfigLoader.toPrettyJson(holder.schema(), next));
    }

    // ───────────── 死亡、复原、去重、居所（TC-T9-59～61） ─────────────

    @GameTest(template = T, batch = "guardian_respawn")
    public static void killed_priest_returns_next_day(GameTestHelper h) {
        GuardianEntity g = guardian(h);
        UUID id = g.guardianId();
        MinecraftServerRef server = new MinecraftServerRef(h);
        boolean[] killedEvent = {false};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (io.github.happysnappingturtle.mythicage.guardian.GuardianEvent.Killed e) -> {
                    if (id.equals(e.guardian().guardianId())) {
                        killedEvent[0] = true;
                    }
                });
        g.kill();
        GuardianRegistry reg = GuardianRegistry.get(server.get());
        h.assertTrue(!reg.get(id).orElseThrow().alive(), "登记为已故");
        h.assertTrue(killedEvent[0], "发出 GuardianKilled 事件");
        GuardianSpawner.respawnDue(server.get(), 1);
        h.assertTrue(!reg.get(id).orElseThrow().alive(), "当天不复原");
        MythDays.advanceForTest(1);
        try {
            GuardianSpawner.respawnDue(server.get(), 1);
        } finally {
            MythDays.advanceForTest(-1);
        }
        GuardianRegistry.Entry e = reg.get(id).orElseThrow();
        h.assertTrue(e.alive(), "次日复原");
        Entity revived = h.getLevel().getEntity(e.entityUuid());
        h.assertTrue(revived instanceof GuardianEntity rg && id.equals(rg.guardianId()), "身份不变");
        // TC-T9-60：同 guardianId 的旧副本不能再进入世界
        GuardianEntity copy = new GreekGuardianEntity(
                io.github.happysnappingturtle.mythicage.entity.ModEntities.GREEK_GUARDIAN.get(), h.getLevel());
        copy.assign(id, e.home().pos());
        copy.moveTo(revived.getX(), revived.getY(), revived.getZ());
        h.assertFalse(h.getLevel().addFreshEntity(copy), "旧副本应被拒绝");
        h.succeed();
    }

    /** 让 lambda 里拿得到服务端。 */
    private record MinecraftServerRef(GameTestHelper h) {
        net.minecraft.server.MinecraftServer get() {
            return h.getLevel().getServer();
        }
    }

    @GameTest(template = GameTestTemplates.EMPTY_17X16X36, batch = "guardian_home", timeoutTicks = 600)
    public static void priest_walks_back_home(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(8, 1, 4)), Optional.empty(), Civilization.GREEK);
        h.assertTrue(g != null, "生成祭司");
        BlockPos home = g.home();
        BlockPos far = h.absolutePos(new BlockPos(8, 1, 30));
        g.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        h.succeedWhen(() -> h.assertTrue(g.blockPosition().distManhattan(home) <= 16,
                "应走回居所 16 格内，当前距离 " + g.blockPosition().distManhattan(home)));
    }

    // ───────────── 存档（TC-T9-62） ─────────────

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void registry_entity_and_ledger_round_trip(GameTestHelper h) {
        GuardianRegistry reg = new GuardianRegistry();
        UUID id = UUID.randomUUID();
        reg.put(new GuardianRegistry.Entry(id, net.minecraft.core.GlobalPos.of(h.getLevel().dimension(), new BlockPos(1, 2, 3)),
                Optional.of(new BlockPos(4, 5, 6)), false, 7, UUID.randomUUID(), Civilization.GREEK));
        CompoundTag tag = reg.save(new CompoundTag(), h.getLevel().registryAccess());
        GuardianRegistry back = GuardianRegistry.read(tag, h.getLevel().registryAccess());
        h.assertTrue(back.get(id).equals(reg.get(id)), "注册表往返一致");

        GuardianEntity g = new GreekGuardianEntity(
                io.github.happysnappingturtle.mythicage.entity.ModEntities.GREEK_GUARDIAN.get(), h.getLevel());
        g.assign(id, new BlockPos(9, 8, 7));
        CompoundTag et = new CompoundTag();
        g.saveWithoutId(et);
        GuardianEntity g2 = new GreekGuardianEntity(
                io.github.happysnappingturtle.mythicage.entity.ModEntities.GREEK_GUARDIAN.get(), h.getLevel());
        g2.load(et);
        h.assertTrue(id.equals(g2.guardianId()) && new BlockPos(9, 8, 7).equals(g2.home()), "实体身份与居所往返一致");

        PlayerMythData d = new PlayerMythData();
        d.tradeDaily().record("greek:libation", 12);
        d.tradeDaily().record("greek:libation", 12);
        h.assertTrue(PlayerDataNbt.read(PlayerDataNbt.write(d)).equals(d), "限购台账往返一致");
        h.succeed();
    }
}
