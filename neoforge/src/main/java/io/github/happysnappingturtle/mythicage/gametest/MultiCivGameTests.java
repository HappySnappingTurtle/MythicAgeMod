package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.guardian.GreekGuardianEntity;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.faith.FaithProfile;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import io.github.happysnappingturtle.mythicage.core.village.Pantheon;
import io.github.happysnappingturtle.mythicage.core.village.VillageRules;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import io.github.happysnappingturtle.mythicage.faith.FaithProfiles;
import io.github.happysnappingturtle.mythicage.faith.GraceSource;
import io.github.happysnappingturtle.mythicage.faith.GraceSources;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianGesture;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.guardian.GuardianTypes;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKinds;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.shrine.ShrineService;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import io.github.happysnappingturtle.mythicage.trade.TradeView;
import io.github.happysnappingturtle.mythicage.village.PantheonLoader;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageResolver;
import io.github.happysnappingturtle.mythicage.village.VillageService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * T10b 多文明通用化（TC-T10b-40～62）。「测试文明」借用埃及的 id（{@link Civilization} 是枚举），
 * 文明档案、祭司类型、交易表、神庙种类都只在本批次里临时注入，批次结束清除；正式内容里没有任何埃及数据。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultiCivGameTests {
    private static final String ON = "multiciv";
    private static final String OFF = "multiciv_off";
    private static final String T = GameTestTemplates.EMPTY_11X11;
    private static final Civilization TC = Civilization.EGYPTIAN;
    private static final Civilization GR = Civilization.GREEK;
    /** 测试文明的神庙种类：只在登记层使用，方块随便选一个原版方块。 */
    private static final ShrineKind TEST_KIND = new ShrineKind("egyptian:test", TC, () -> Blocks.LODESTONE, 4,
            ShrineKind.Hooks.NONE);
    private static final FaithProfile TEST_PROFILE = new FaithProfile(TC, "entity.mythic_age.greek_guardian",
            "block.minecraft.lodestone", "mythic_age.favor.greek", "",
            List.of(new FaithProfile.Gift("minecraft:bread", 2), new FaithProfile.Gift("minecraft:paper", 1)),
            Optional.of(new FaithProfile.Reissue("minecraft:lodestone", TEST_KIND.id())));
    private static final String TEST_TABLE = """
            {"schemaVersion":1,"civilization":"egyptian","offers":[
             {"id":"ankh","category":"supplies","result":{"item":"minecraft:gold_nugget","count":1},
              "price":{"favor":10},"minAge":"archaic","venues":["guardian"],"dailyLimit":3}]}""";

    private MultiCivGameTests() {
    }

    static void install() {
        FaithProfiles.overrideForTest(TC, TEST_PROFILE);
        GuardianTypes.overrideForTest(TC, ModEntities.GREEK_GUARDIAN);
        TradeTableLoader.overrideForTest(TradeTable.parse(TEST_TABLE, TradeTableLoader.ITEMS));
        ShrineKinds.overrideForTest(TEST_KIND);
    }

    static void clear() {
        FaithProfiles.clearTestOverrides();
        GuardianTypes.overrideForTest(TC, null);
        TradeTableLoader.overrideForTest(null);
        ShrineKinds.overrideForTest(null);
        PantheonLoader.overrideForTest(null);
        VillageResolver.overrideBiomesForTest(null);
    }

    @BeforeBatch(batch = ON)
    public static void before(ServerLevel level) {
        install();
    }

    @AfterBatch(batch = ON)
    public static void after(ServerLevel level) {
        clear();
    }

    // ───────────── 搭建 ─────────────

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                h.setBlock(x, 0, z, Blocks.STONE);
            }
        }
    }

    private static BlockPos bell(GameTestHelper h, int x, int z) {
        floor(h);
        BlockPos rel = new BlockPos(x, 1, z);
        h.setBlock(rel, Blocks.BELL);
        return h.absolutePos(rel);
    }

    private static GlobalPos g(GameTestHelper h, BlockPos p) {
        return GlobalPos.of(h.getLevel().dimension(), p);
    }

    private static VillageRegistry.Village village(GameTestHelper h, BlockPos bell, Civilization civ, String patron) {
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), g(h, bell), civ.id(), patron,
                VillageRegistry.Origin.VANILLA, null, "desert", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        return v;
    }

    private static GuardianEntity priest(GameTestHelper h, Civilization civ) {
        floor(h);
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), Optional.empty(), civ);
        if (g == null) {
            throw new GameTestAssertException(civ.id() + " 的祭司没有生成");
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

    private static void expect(GameTestHelper h, TradeService.Outcome o, TradeReason reason) {
        h.assertTrue(o.reason() == reason, "应为 " + reason + "，实际 " + o.reason());
    }

    private static TradeView view(GameTestHelper h, FakePlayer p) {
        return TradeService.viewForTest(p).orElseThrow(() -> new GameTestAssertException("没有交易会话"));
    }

    // ───────────── 神庙登记（TC-T10b-40～43） ─────────────

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = ON)
    public static void legacy_shrine_records_are_greek(GameTestHelper h) {
        CompoundTag c = new CompoundTag();
        c.putString("dim", h.getLevel().dimension().location().toString());
        c.putLong("pos", new BlockPos(1, 2, 3).asLong());
        c.putUUID("owner", UUID.randomUUID());
        c.putString("ownerName", "old");
        c.putString("facing", "south");
        ListTag list = new ListTag();
        list.add(c);
        CompoundTag root = new CompoundTag();
        root.put("altars", list);
        ShrineRegistry r = ShrineRegistry.load(root, h.getLevel().registryAccess());
        ShrineRegistry.Entry e = r.all().getFirst();
        h.assertTrue(e.civ() == GR && e.kind().equals("greek:altar"), "T8a 记录视为希腊神坛：" + e);
        ShrineRegistry back = ShrineRegistry.load(r.save(new CompoundTag(), h.getLevel().registryAccess()),
                h.getLevel().registryAccess());
        h.assertTrue(back.all().equals(r.all()), "保存后再读字段齐全");
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void shrine_limits_and_spacing_are_per_kind(GameTestHelper h) {
        floor(h);
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_kinds"));
        data(p).civ(GR).setAge(Age.ARCHAIC);
        ShrineRegistry reg = ShrineRegistry.get(h.getLevel().getServer());
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        BlockPos near = at.offset(2, 0, 0);
        reg.add(new ShrineRegistry.Entry(g(h, near), p.getUUID(), "ma_kinds", Direction.SOUTH, TC, TEST_KIND.id()));
        try {
            h.assertTrue(reg.countOwned(p.getUUID(), TEST_KIND.id()) == 1, "测试种类 1 座");
            h.assertTrue(reg.countOwned(p.getUUID(), GreekTemple.ALTAR_KIND.id()) == 0, "希腊神坛 0 座");
            Optional<?> err = ShrineService.placementError(p, h.getLevel(), at, Direction.SOUTH, GreekTemple.ALTAR_KIND);
            h.assertTrue(err.isEmpty(), "已有 1 座别的种类：希腊远古上限 1 仍可放，且不受别的种类的间距影响：" + err);
            reg.add(new ShrineRegistry.Entry(g(h, at.offset(-40, 0, 0)), p.getUUID(), "ma_kinds", Direction.SOUTH, GR,
                    GreekTemple.ALTAR_KIND.id()));
            h.assertTrue(ShrineService.placementError(p, h.getLevel(), at, Direction.SOUTH, GreekTemple.ALTAR_KIND)
                    .isPresent(), "希腊神坛已有 1 座：达到远古上限");
        } finally {
            reg.all().stream().filter(e -> e.owner().equals(p.getUUID())).forEach(e -> reg.remove(e.pos()));
        }
        h.succeed();
    }

    // ───────────── 祭司（TC-T10b-45～50） ─────────────

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = ON)
    public static void legacy_priest_records_are_greek(GameTestHelper h) {
        CompoundTag c = new CompoundTag();
        c.putUUID("id", UUID.randomUUID());
        c.putString("dim", h.getLevel().dimension().location().toString());
        c.putLong("home", new BlockPos(1, 2, 3).asLong());
        c.putBoolean("alive", true);
        c.putLong("deathDay", 0);
        c.putUUID("entity", UUID.randomUUID());
        ListTag list = new ListTag();
        list.add(c);
        CompoundTag root = new CompoundTag();
        root.put("guardians", list);
        GuardianRegistry r = GuardianRegistry.read(root, h.getLevel().registryAccess());
        h.assertTrue(r.all().getFirst().civ() == GR, "T9 记录视为希腊");
        GreekGuardianEntity e = new GreekGuardianEntity(ModEntities.GREEK_GUARDIAN.get(), h.getLevel());
        CompoundTag et = new CompoundTag();
        e.saveWithoutId(et);
        et.remove("Civ");
        GreekGuardianEntity back = new GreekGuardianEntity(ModEntities.GREEK_GUARDIAN.get(), h.getLevel());
        back.load(et);
        h.assertTrue(back.civ() == GR, "旧实体没有 Civ：按实体类型视为希腊");
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void test_civ_village_gets_its_own_priest(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        village(h, bell, TC, "ra");
        try {
            int n = GuardianSpawner.trySpawnNear(h.getLevel(), bell);
            h.assertTrue(n == 1, "测试文明村庄生成 1 位祭司，实际 " + n);
            GuardianRegistry.Entry e = GuardianRegistry.get(h.getLevel().getServer()).byBell(h.getLevel().dimension(), bell)
                    .orElseThrow();
            h.assertTrue(e.civ() == TC, "登记的文明：" + e.civ());
            GuardianEntity g = h.getLevel().getEntitiesOfClass(GuardianEntity.class, new AABB(bell).inflate(5)).getFirst();
            h.assertTrue(g.civ() == TC, "实体的文明：" + g.civ());
        } finally {
            VillageRegistry.get(h.getLevel().getServer()).remove(g(h, bell));
        }
        h.succeed();
    }

    @GameTest(template = T, batch = OFF)
    public static void closed_civ_gets_no_priest(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        village(h, bell, TC, "ra");
        try {
            h.assertTrue(!GuardianSpawner.hasGuardian(TC), "没有注入：测试文明未开放");
            h.assertTrue(GuardianSpawner.trySpawnNear(h.getLevel(), bell) == 0, "未开放的文明不生成祭司");
            h.assertTrue(GuardianSpawner.spawnNew(h.getLevel(), bell.offset(2, 0, 0), Optional.empty(), Civilization.NORSE)
                    == null, "没有登记祭司类型的文明直接返回 null");
            var src = h.getLevel().getServer().createCommandSourceStack().withPermission(4).withSuppressedOutput()
                    .withPosition(bell.getCenter());
            int r = h.getLevel().getServer().getCommands().getDispatcher().execute("mythicage guardian spawn egyptian", src);
            h.assertTrue(r == 0, "命令对未开放文明返回 0");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new GameTestAssertException("命令语法：" + e.getMessage());
        } finally {
            VillageRegistry.get(h.getLevel().getServer()).remove(g(h, bell));
        }
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void test_civ_priest_respawns_as_its_civ(GameTestHelper h) {
        GuardianEntity g = priest(h, TC);
        UUID id = g.guardianId();
        g.kill();
        GuardianRegistry reg = GuardianRegistry.get(h.getLevel().getServer());
        h.assertTrue(!reg.get(id).orElseThrow().alive(), "已登记死亡");
        int revived = GuardianSpawner.respawnDue(h.getLevel().getServer(), 0);
        h.assertTrue(revived >= 1, "到期复原");
        GuardianEntity back = h.getLevel().getEntitiesOfClass(GuardianEntity.class,
                new AABB(h.absolutePos(new BlockPos(5, 1, 5))).inflate(5), e -> id.equals(e.guardianId())).getFirst();
        h.assertTrue(back.civ() == TC && reg.get(id).orElseThrow().civ() == TC, "按登记的文明复原");
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void gestures_and_spawn_command(GameTestHelper h) {
        GuardianEntity g = priest(h, GR);
        g.playGesture(GuardianGesture.ANNOUNCE);
        h.assertTrue(g.gesture() == GuardianGesture.ANNOUNCE && g.gestureStart() == g.tickCount, "手势同步字段");
        BlockPos at = h.absolutePos(new BlockPos(2, 1, 2));
        var src = h.getLevel().getServer().createCommandSourceStack().withPermission(4).withSuppressedOutput()
                .withPosition(at.getCenter()).withLevel(h.getLevel());
        try {
            int r = h.getLevel().getServer().getCommands().getDispatcher().execute("mythicage guardian spawn egyptian", src);
            h.assertTrue(r == 1, "注入后命令可生成测试文明祭司");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new GameTestAssertException("命令语法：" + e.getMessage());
        }
        h.assertTrue(h.getLevel().getEntitiesOfClass(GuardianEntity.class, new AABB(at).inflate(2), e -> e.civ() == TC)
                .size() == 1, "命令生成的祭司属于测试文明");
        h.succeed();
    }

    // ───────────── 交易（TC-T10b-51～58） ─────────────

    @GameTest(template = T, batch = ON)
    public static void test_civ_trade_uses_its_own_favor_and_table(GameTestHelper h) {
        GuardianEntity g = priest(h, TC);
        FakePlayer p = player(h, g, "ma_tc_trade");
        data(p).civ(TC).setAge(Age.ARCHAIC);
        data(p).civ(TC).setFavor(30);
        data(p).civ(GR).setAge(Age.ARCHAIC);
        data(p).civ(GR).setFavor(50);
        h.assertTrue(TradeService.openGuardian(p, g), "打开测试文明祭司");
        TradeView v = view(h, p);
        h.assertTrue(v.civ() == TC && v.favor() == 30, "界面是测试文明：" + v.civ() + " " + v.favor());
        h.assertTrue(v.offers().size() == 1 && v.offers().getFirst().id().equals("ankh"), "商品来自测试交易表");
        h.assertTrue(v.labels().shrine().equals("block.minecraft.lodestone"), "文本 key 来自档案");
        var o = TradeService.buy(p, "ankh", 10, UUID.randomUUID().toString());
        expect(h, o, TradeReason.OK);
        h.assertTrue(data(p).civ(TC).favor() == 20 && data(p).civ(GR).favor() == 50, "只扣测试文明的恩宠");
        h.assertTrue(data(p).tradeDaily().view().keySet().stream().anyMatch(k -> k.startsWith("egyptian:")),
                "每日计数带文明前缀：" + data(p).tradeDaily().view().keySet());
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void test_civ_first_blessing_gives_profile_gifts(GameTestHelper h) {
        GuardianEntity g = priest(h, TC);
        FakePlayer p = player(h, g, "ma_tc_bless");
        TradeService.openGuardian(p, g);
        h.assertTrue(view(h, p).dialogue() == TradeView.Dialogue.FIRST, "初见");
        expect(h, TradeService.blessing(p, false), TradeReason.OK);
        h.assertTrue(p.getInventory().countItem(Items.BREAD) == 2 && p.getInventory().countItem(Items.PAPER) == 1,
                "两种赠礼都发放");
        h.assertTrue(data(p).civ(TC).age() == Age.ARCHAIC && data(p).civ(GR).age() == Age.NONE, "只进入测试文明的远古");
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void test_civ_blessing_needs_room_for_all_gifts(GameTestHelper h) {
        GuardianEntity g = priest(h, TC);
        FakePlayer p = player(h, g, "ma_tc_full");
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            p.getInventory().items.set(i, new ItemStack(Blocks.DIRT, 64));
        }
        p.getInventory().items.set(0, ItemStack.EMPTY);
        TradeService.openGuardian(p, g);
        expect(h, TradeService.blessing(p, false), TradeReason.INVENTORY_FULL);
        h.assertTrue(data(p).civ(TC).age() == Age.NONE && p.getInventory().countItem(Items.BREAD) == 0,
                "整单失败：时代不变、没有发放");
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void reissue_is_per_civ(GameTestHelper h) {
        GuardianEntity tc = priest(h, TC);
        FakePlayer p = player(h, tc, "ma_tc_reissue");
        data(p).civ(TC).setAge(Age.ARCHAIC);
        data(p).civ(GR).setAge(Age.ARCHAIC);
        ShrineRegistry reg = ShrineRegistry.get(h.getLevel().getServer());
        GlobalPos greekAltar = g(h, h.absolutePos(new BlockPos(1, 1, 1)).offset(-60, 0, 0));
        reg.add(new ShrineRegistry.Entry(greekAltar, p.getUUID(), "ma_tc_reissue", Direction.SOUTH, GR,
                GreekTemple.ALTAR_KIND.id()));
        try {
            TradeService.openGuardian(p, tc);
            h.assertTrue(view(h, p).dialogue() == TradeView.Dialogue.REISSUE, "测试文明没有神庙：可补领");
            expect(h, TradeService.blessing(p, true), TradeReason.OK);
            h.assertTrue(p.getInventory().countItem(Items.LODESTONE) == 1, "补领档案里的物品");
            GuardianEntity gr = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(2, 1, 8)),
                    Optional.empty(), GR);
            gr.setNoAi(true);
            p.moveTo(gr.getX() + 1, gr.getY(), gr.getZ());
            TradeService.openGuardian(p, gr);
            expect(h, TradeService.blessing(p, true), TradeReason.HAS_SHRINE);
        } finally {
            reg.remove(greekAltar);
        }
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void village_priest_dialogue_only_names_patron(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), g(h, bell), "greek", "zeus",
                VillageRegistry.Origin.VANILLA, null, "plains", -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        try {
            GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), bell.offset(2, 0, 0), Optional.of(bell), GR);
            g.setNoAi(true);
            FakePlayer p = player(h, g, "ma_patron_line");
            data(p).civ(GR).setAge(Age.ARCHAIC);
            TradeService.openGuardian(p, g);
            TradeView view = view(h, p);
            h.assertTrue(view.villagePatron().equals("zeus"), "对话页只带本村主神");
            h.assertTrue(data(p).civ(GR).knownPatrons().contains("zeus"), "结识主神仍记入（图鉴用）");
        } finally {
            VillageRegistry.get(h.getLevel().getServer()).remove(g(h, bell));
        }
        h.succeed();
    }

    // ───────────── 村庄与恩宠来源（TC-T10b-59～62） ─────────────

    @GameTest(template = T, batch = "multiciv_village")
    public static void empty_patron_is_backfilled_once(GameTestHelper h) {
        BlockPos bell = bell(h, 5, 5);
        VillageRegistry.Village v = village(h, bell, TC, "");
        ConfigHolder<VillagesConfig> holder = ModConfigs.villages();
        VillagesConfig original = holder.current();
        try {
            h.assertTrue(VillageService.backfillPatron(h.getLevel(), v).isEmpty(), "没有权重时不补");
            holder.reload("{\"schemaVersion\":2,\"patronWeights\":{\"egyptian\":{\"ra\":40,\"isis\":35,\"set\":25}}}");
            String expected = VillageRules.pick(Map.of("ra", 40, "isis", 35, "set", 25),
                    VillageRules.unit(bell.getX(), bell.getY(), bell.getZ(), VillageRules.SALT_PATRON)).orElseThrow();
            Optional<String> got = VillageService.backfillPatron(h.getLevel(), v);
            h.assertTrue(got.equals(Optional.of(expected)), "补抽结果与登记时就有权重相同：" + got + " / " + expected);
            VillageRegistry.Village now = VillageRegistry.get(h.getLevel().getServer()).byBell(g(h, bell)).orElseThrow();
            h.assertTrue(now.patron().equals(expected), "写入登记");
            h.assertTrue(VillageService.backfillPatron(h.getLevel(), now).isEmpty(), "只补一次");
        } finally {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), original));
            VillageRegistry.get(h.getLevel().getServer()).remove(g(h, bell));
        }
        h.succeed();
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = ON)
    public static void coastal_bonus_matches_t10a(GameTestHelper h) {
        VillagesConfig cfg = ModConfigs.villages().current();
        Map<String, Integer> greek = cfg.patronWeights().get("greek");
        VillageResolver.overrideBiomesForTest((l, b) -> Set.of("#minecraft:is_ocean"));
        try {
            for (int i = 0; i < 40; i++) {
                BlockPos b = new BlockPos(i * 37, 64, -i * 91);
                String want = VillageRules.pick(VillageRules.biomeBonus(greek, Map.of("poseidon", 2.0)),
                        VillageRules.unit(b.getX(), b.getY(), b.getZ(), VillageRules.SALT_PATRON)).orElseThrow();
                String got = VillageService.pickPatron(h.getLevel(), b, "greek", cfg);
                h.assertTrue(want.equals(got), "沿海第 " + i + " 个：" + got + " / " + want);
            }
            VillageResolver.overrideBiomesForTest((l, b) -> Set.of("minecraft:plains"));
            BlockPos b = new BlockPos(5, 64, 5);
            h.assertTrue(VillageService.pickPatron(h.getLevel(), b, "greek", cfg).equals(VillageRules.pick(greek,
                    VillageRules.unit(5, 64, 5, VillageRules.SALT_PATRON)).orElseThrow()), "内陆不加成");
        } finally {
            VillageResolver.overrideBiomesForTest(null);
        }
        h.succeed();
    }

    @GameTest(template = T, batch = ON)
    public static void greek_temple_grace_source_lists_altars(GameTestHelper h) {
        floor(h);
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_grace"));
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos b = new BlockPos(9, 1, 9);
        for (BlockPos rel : List.of(a, b)) {
            h.setBlock(rel, GreekTemple.GREEK_ALTAR.get().defaultBlockState().setValue(GreekAltarBlock.FACING, Direction.SOUTH));
            ShrineService.bindForTest(h.getLevel(), h.absolutePos(rel), p.getUUID(), "ma_grace", Age.ARCHAIC, Direction.SOUTH);
        }
        List<GraceSource> sources = GraceSources.of(GR);
        h.assertTrue(sources.stream().anyMatch(s -> s.id().equals("greek:temple")), "希腊有神庙恩宠来源");
        h.assertTrue(GraceSources.of(Civilization.NORSE).isEmpty(), "未开放的文明没有来源");
        GraceSource temple = sources.stream().filter(s -> s.id().equals("greek:temple")).findFirst().orElseThrow();
        List<GraceSource.Row> rows = temple.rows(p);
        h.assertTrue(rows.size() == 2, "两座神坛两行：" + rows.size());
        h.assertTrue(rows.stream().map(GraceSource.Row::pos).toList()
                .containsAll(List.of(g(h, h.absolutePos(a)), g(h, h.absolutePos(b)))), "行里带坐标");
        h.succeed();
    }

    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = ON)
    public static void duplicate_god_ids_warn(GameTestHelper h) {
        Pantheon p = Pantheon.parse("""
                {"schemaVersion":1,"civilization":"egyptian","patrons":["ra","zeus"],
                 "minorGods":[{"id":"bast","age":"classical","patrons":["ra"]}]}""");
        try {
            PantheonLoader.overrideForTest(p);
            h.assertTrue(PantheonLoader.warnDuplicates().equals(List.of("zeus")), "与希腊重名：zeus");
        } finally {
            PantheonLoader.overrideForTest(null);
        }
        h.assertTrue(PantheonLoader.warnDuplicates().isEmpty(), "正式表没有重名");
        h.succeed();
    }

    /** 防止测试文明的注入漏到其他批次。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = OFF)
    public static void injections_are_cleared(GameTestHelper h) {
        h.assertTrue(FaithProfiles.get(TC).isEmpty() && GuardianTypes.typeFor(TC).isEmpty()
                && ShrineKinds.byId(TEST_KIND.id()).isEmpty() && TradeTableLoader.table(TC).offers().isEmpty(),
                "测试文明的注入已清除");
        h.assertTrue(FaithProfiles.get(GR).isPresent() && GuardianTypes.typeFor(GR).isPresent(), "希腊正式内容在");
        h.succeed();
    }
}
