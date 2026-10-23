package io.github.happysnappingturtle.mythicage.gametest;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.advancement.MythEvents;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTempleRequirement;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.rite.RiteKind;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.faith.FaithService;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.rite.PromotionRequirement;
import io.github.happysnappingturtle.mythicage.rite.RiteService;
import io.github.happysnappingturtle.mythicage.rite.RitesView;
import io.github.happysnappingturtle.mythicage.rite.UnlockService;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import io.github.happysnappingturtle.mythicage.statue.PatronStatues;
import io.github.happysnappingturtle.mythicage.statue.StatueTarget;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import io.github.happysnappingturtle.mythicage.trade.TradeView;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
 * T11b：希腊晋升数据、神庙等级条件、次神祠商品、主神像商品、希腊进度（TC-T11b-12～42）。
 * 全部用正式数据包里的希腊数据，不注入测试表；TC-T11b-10、11 在 {@code TempleGameTests} 里随神庙算例一起核对。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GreekPromotionGameTests {
    private static final String BATCH = "greek_promotion";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final Civilization GR = Civilization.GREEK;
    private static final String ALTAR_KIND = "greek:altar";

    private GreekPromotionGameTests() {
    }

    // ───────────── 搭建 ─────────────

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 36; z++) {
                h.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    private record Site(VillageRegistry.Village village, GuardianEntity priest) {
        VillageRegistry.Village current(GameTestHelper h) {
            return VillageRegistry.get(h.getLevel().getServer()).byId(village.id()).orElseThrow();
        }
    }

    private static Site site(GameTestHelper h, String patron, Age stage, Map<Age, List<String>> shrines, int x, int z) {
        BlockPos bell = h.absolutePos(new BlockPos(x, 2, z + 1));
        VillageRegistry.Village v = new VillageRegistry.Village(UUID.randomUUID(), GlobalPos.of(h.getLevel().dimension(), bell),
                GR.id(), patron, VillageRegistry.Origin.VANILLA, null, "plains", -1, stage, shrines, -1, -1);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        GuardianEntity g = GuardianSpawner.spawnNew(h.getLevel(), h.absolutePos(new BlockPos(x, 2, z)), Optional.of(bell), GR);
        if (g == null) {
            throw new GameTestAssertException("祭司没有生成");
        }
        g.setNoAi(true);
        return new Site(v, g);
    }

    private static FakePlayer player(GameTestHelper h, String name, Age age, long favor) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.getInventory().clearContent();
        MythData.get(p).civ(GR).setAge(age);
        MythData.get(p).civ(GR).setFavor(favor);
        return p;
    }

    private static void at(FakePlayer p, Site s) {
        p.moveTo(s.priest().getX() + 1.5, s.priest().getY(), s.priest().getZ());
        TradeService.openGuardian(p, s.priest());
    }

    /** 一座远处的、只写在登记里的希腊神坛（区块不加载，条件只能读登记）。 */
    private static GlobalPos altarEntry(GameTestHelper h, FakePlayer p, int dx, int level) {
        GlobalPos pos = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(new BlockPos(0, 2, 0)).offset(5000 + dx, 0, 0));
        ShrineRegistry reg = ShrineRegistry.get(h.getLevel().getServer());
        reg.add(new ShrineRegistry.Entry(pos, p.getUUID(), p.getGameProfile().getName(), Direction.SOUTH, GR, ALTAR_KIND));
        reg.setLevel(pos, level);
        return pos;
    }

    private static void give(FakePlayer p, net.minecraft.world.item.Item item, int n) {
        p.getInventory().add(new ItemStack(item, n));
    }

    private static void giveId(FakePlayer p, String id, int n) {
        p.getInventory().add(TradeService.stack(id, n));
    }

    private static PlayerMythData.CivState data(FakePlayer p) {
        return MythData.get(p).civ(GR);
    }

    private static RitesView.Option promotion(FakePlayer p) {
        return TradeService.viewForTest(p).orElseThrow().rites().orElseThrow().options().stream()
                .filter(o -> o.kind() == RiteKind.PROMOTION).findFirst().orElseThrow();
    }

    private static String key(net.minecraft.network.chat.Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    private static void cleanup(GameTestHelper h, List<GlobalPos> altars, Site... sites) {
        ShrineRegistry reg = ShrineRegistry.get(h.getLevel().getServer());
        altars.forEach(reg::remove);
        for (Site s : sites) {
            VillageRegistry.get(h.getLevel().getServer()).remove(s.village().bell());
            s.priest().discard();
        }
        TradeService.clearForTest();
    }

    // ───────────── 神庙等级条件（TC-T11b-12～14） ─────────────

    @GameTest(template = T, batch = BATCH)
    public static void temple_requirement_reads_registry(GameTestHelper h) {
        FakePlayer p = player(h, "gp_temple", Age.ARCHAIC, 0);
        JsonObject one = new JsonObject();
        one.addProperty("level", 1);
        JsonObject two = new JsonObject();
        two.addProperty("level", 2);
        PromotionRequirement r = GreekTempleRequirement.INSTANCE;
        PromotionRequirement.Line l = r.check(p, GR, Age.CLASSICAL, null, one);
        h.assertTrue(!l.met() && key(l.label()).equals("mythic_age.rite.line.temple_none"), "没有神坛：" + l);
        GlobalPos a = altarEntry(h, p, 0, 0);
        l = r.check(p, GR, Age.CLASSICAL, null, one);
        h.assertTrue(!l.met() && l.progress().getString().equals("0 / 1"), "0 级：" + l.progress().getString());
        ShrineRegistry reg = ShrineRegistry.get(h.getLevel().getServer());
        reg.setLevel(a, 1);
        h.assertTrue(r.check(p, GR, Age.CLASSICAL, null, one).met(), "1 级满足（神坛区块未加载，只读登记）");
        GlobalPos b = altarEntry(h, p, 100, 3);
        l = r.check(p, GR, Age.HEROIC, null, two);
        h.assertTrue(!l.met() && l.progress().getString().equals("1 / 2"), "远古封顶 1 级：" + l.progress().getString());
        data(p).setAge(Age.CLASSICAL);
        l = r.check(p, GR, Age.HEROIC, null, two);
        h.assertTrue(l.met() && l.progress().getString().equals("2 / 2"), "古典封顶 2 级，取两座里最高：" + l.progress().getString());
        // TC-T11b-14：存档往返；旧存档没有 level 字段
        CompoundTag tag = reg.save(new CompoundTag(), h.getLevel().registryAccess());
        h.assertTrue(ShrineRegistry.load(tag, h.getLevel().registryAccess()).level(b) == 3, "往返后 3 级");
        ListTag list = tag.getList("altars", Tag.TAG_COMPOUND);
        list.forEach(t -> ((CompoundTag) t).remove("level"));
        h.assertTrue(ShrineRegistry.load(tag, h.getLevel().registryAccess()).level(b) == 0, "旧存档缺省 0");
        cleanup(h, List.of(a, b));
        h.succeed();
    }

    // ───────────── 远古 → 古典 → 英雄（TC-T11b-20～25） ─────────────

    private static Set<String> recipeIds(io.github.happysnappingturtle.mythicage.core.rite.UnlockSummary s) {
        return Set.copyOf(s.recipes());
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void archaic_to_heroic(GameTestHelper h) {
        floor(h);
        Site s = site(h, "zeus", Age.ARCHAIC, Map.of(), 4, 4);
        FakePlayer p = player(h, "gp_walk", Age.ARCHAIC, 50);
        give(p, Items.COOKED_BEEF, 8);
        give(p, Items.WHEAT, 32);
        giveId(p, "mythic_age:libation", 4);
        give(p, Items.COPPER_INGOT, 16);
        GlobalPos altar = altarEntry(h, p, 0, 0);
        ShrineRegistry reg = ShrineRegistry.get(h.getLevel().getServer());
        at(p, s);
        // TC-T11b-21：神庙 0 级
        RitesView.Option o = promotion(p);
        h.assertTrue(o.target() == Age.CLASSICAL && !o.ready() && o.lines().size() == 6, "6 行条件、不可举行：" + o.lines());
        RitesView.Line temple = o.lines().get(5);
        h.assertTrue(!temple.met() && temple.progress().getString().equals("0 / 1"), "神庙行 ✘ 0 / 1");
        // TC-T11b-20：1 级后全部满足；候选为宙斯名下的雅典娜、赫尔墨斯
        reg.setLevel(altar, 1);
        o = promotion(p);
        h.assertTrue(o.ready() && o.favor() == 50, "可举行、50 恩宠：" + o.lines());
        h.assertTrue(o.candidates().stream().map(RitesView.Candidate::id).toList().equals(List.of("athena", "hermes")),
                "候选：" + o.candidates());
        RiteService.Outcome r = RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "athena", 50, UUID.randomUUID().toString());
        h.assertTrue(r.reason() == RiteService.Reason.OK, "晋升古典：" + r);
        h.assertTrue(data(p).age() == Age.CLASSICAL && p.getInventory().countItem(Items.COPPER_INGOT) == 0, "古典、供品已扣");
        var classical = UnlockService.summary(p, GR, Age.CLASSICAL);
        h.assertTrue(recipeIds(classical).equals(Set.of("mythic_age:bronze_mix", "mythic_age:ionic_capital",
                "mythic_age:greek_pediment", "mythic_age:greek_statue", "mythic_age:krotala",
                "mythic_age:bronze_helmet", "mythic_age:bronze_chestplate", "mythic_age:bronze_leggings", "mythic_age:bronze_boots",
                "mythic_age:xiphos", "mythic_age:dory", "mythic_age:aspis", "mythic_age:composite_bow", "mythic_age:olive_club")),
                "古典配方（TC-T11b-25，T15 加青铜响板，T17 加青铜装备）：" + recipeIds(classical));
        h.assertTrue(classical.extras().size() == 1 && classical.extras().getFirst().key()
                .equals("mythic_age.unlock.greek.temple_prostylos"), "额外项：中神庙");
        // TC-T11b-22：英雄供品齐、神庙 2 级，但没有凭证
        data(p).setFavor(100);
        give(p, io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.BRONZE_INGOT.get(), 16);
        give(p, Items.GOLD_INGOT, 8);
        give(p, Items.COOKED_MUTTON, 16);
        give(p, Items.HONEY_BOTTLE, 4);
        reg.setLevel(altar, 2);
        at(p, s);
        RitesView.Option toHeroic = promotion(p);
        h.assertTrue(toHeroic.target() == Age.HEROIC && !toHeroic.ready(), "没有凭证不可举行：" + toHeroic.lines()
                + toHeroic.blocking());
        RitesView.Line voucher = toHeroic.lines().stream().filter(x -> key(x.label()).equals("mythic_age.rite.line.voucher"))
                .findFirst().orElseThrow(() -> new GameTestAssertException("没有凭证行：" + toHeroic.lines()));
        h.assertTrue(!voucher.met(), "凭证行 ✘");
        // TC-T11b-23：发一张古典凭证
        io.github.happysnappingturtle.mythicage.rite.TrialService.grant(p, GR, Age.CLASSICAL, 1);
        RitesView.Option heroic = promotion(p);
        h.assertTrue(heroic.ready() && heroic.candidates().stream().map(RitesView.Candidate::id).toList()
                .equals(List.of("apollo", "dionysus")), "可举行，候选阿波罗（未开放）、狄俄尼索斯：" + heroic.candidates());
        r = RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "dionysus", 100, UUID.randomUUID().toString());
        h.assertTrue(r.reason() == RiteService.Reason.OK && data(p).age() == Age.HEROIC
                && data(p).vouchers(Age.CLASSICAL) == 0, "晋升英雄、凭证扣完：" + r);
        var heroicSummary = UnlockService.summary(p, GR, Age.HEROIC);
        h.assertTrue(recipeIds(heroicSummary).equals(Set.of("mythic_age:corinthian_capital"))
                && heroicSummary.extras().getFirst().key().equals("mythic_age.unlock.greek.temple_peripteros"),
                "英雄配方与额外项：" + recipeIds(heroicSummary));
        // TC-T11b-24：神话未开放
        at(p, s);
        RitesView.Option top = promotion(p);
        h.assertTrue(!top.ready() && !top.blocking().isEmpty(), "神话不可举行：" + top.blocking());
        var page = FaithService.build(p).pages().get(GR.ordinal());
        h.assertTrue(page.nextAge() == Age.NONE && page.next().isEmpty(), "信仰面板：下一时代暂未开放");
        cleanup(h, List.of(altar), s);
        h.succeed();
    }

    // ───────────── 商品（TC-T11b-30～34） ─────────────

    private static Optional<TradeView.OfferView> offer(FakePlayer p, String id) {
        return TradeService.viewForTest(p).orElseThrow().offers().stream().filter(o -> o.id().equals(id)).findFirst();
    }

    @GameTest(template = T, batch = BATCH)
    public static void shrine_goods_and_statue(GameTestHelper h) {
        floor(h);
        Site s = site(h, "zeus", Age.CLASSICAL, Map.of(Age.CLASSICAL, List.of("athena")), 4, 4);
        // TC-T11b-30：未供奉雅典娜
        FakePlayer other = player(h, "gp_other", Age.CLASSICAL, 0);
        at(other, s);
        TradeView.OfferView oil = offer(other, "athena_olive_oil").orElseThrow(() -> new GameTestAssertException("没有雅典娜商品"));
        h.assertTrue(oil.lock() == TradeReason.OK && oil.itemPrice().getFirst().getCount() == 3, "原价 3 个绿宝石：" + oil);
        h.assertTrue(offer(other, "hermes_compass").isEmpty(), "本村没有赫尔墨斯祠：不出现");
        h.assertTrue(offer(other, "dionysus_grape").isEmpty(), "没有狄俄尼索斯祠：不出现");
        // TC-T11b-31：供奉雅典娜，物品价九折并按折后数量扣除
        FakePlayer worshipper = player(h, "gp_worship", Age.CLASSICAL, 0);
        data(worshipper).minorGods().put(Age.CLASSICAL, "athena");
        give(worshipper, Items.EMERALD, 10);
        at(worshipper, s);
        h.assertTrue(offer(worshipper, "athena_olive_oil").orElseThrow().itemPrice().getFirst().getCount() == 2
                && offer(worshipper, "athena_shield").orElseThrow().itemPrice().getFirst().getCount() == 4, "3 → 2、5 → 4");
        TradeService.Outcome bought = TradeService.buy(worshipper, "athena_olive_oil", 0, UUID.randomUUID().toString());
        h.assertTrue(bought.reason() == TradeReason.OK && worshipper.getInventory().countItem(Items.EMERALD) == 8
                && bought.given().getCount() == 4, "花 2 个绿宝石买到 4 瓶橄榄油：" + bought);
        // TC-T11b-34：主神像
        FakePlayer fan = player(h, "gp_statue", Age.CLASSICAL, 50);
        at(fan, s);
        h.assertTrue(offer(fan, "patron_statue").orElseThrow().lock() == TradeReason.NEED_APPROVAL, "未认可：列出但锁住");
        h.assertTrue(TradeService.buy(fan, "patron_statue", 30, UUID.randomUUID().toString()).reason() == TradeReason.NEED_APPROVAL,
                "未被宙斯认可：不能买");
        data(fan).approve("zeus", Age.CLASSICAL);
        TradeService.Outcome st = TradeService.buy(fan, "patron_statue", 30, UUID.randomUUID().toString());
        h.assertTrue(st.reason() == TradeReason.OK && new StatueTarget(GR, "zeus").equals(st.given().get(PatronStatues.TARGET.get()))
                && data(fan).favor() == 20, "30 恩宠买到宙斯像：" + st);
        data(fan).setFavor(50);
        h.assertTrue(offer(fan, "patron_statue").orElseThrow().lock() == TradeReason.HAS_STATUE
                && TradeService.buy(fan, "patron_statue", 30, UUID.randomUUID().toString()).reason() == TradeReason.HAS_STATUE,
                "每主神限 1 座");
        cleanup(h, List.of(), s);
        // TC-T11b-32、33：远古村庄（人为放了雅典娜祠）与古典村庄里的狄俄尼索斯祠
        Site archaic = site(h, "zeus", Age.ARCHAIC, Map.of(Age.CLASSICAL, List.of("athena"), Age.HEROIC, List.of("dionysus")), 4, 20);
        at(other, archaic);
        h.assertTrue(offer(other, "athena_olive_oil").orElseThrow().lock() == TradeReason.STAGE_LOCKED, "远古村庄：阶段不足");
        h.assertTrue(offer(other, "dionysus_grape").orElseThrow().lock() == TradeReason.STAGE_LOCKED, "英雄商品：阶段不足");
        cleanup(h, List.of(), archaic);
        h.succeed();
    }

    // ───────────── 进度（TC-T11b-40～42） ─────────────

    /** 正式进度 JSON 解码后，条件对 (事件, 文明, 数值) 是否成立。假玩家的进度是空实现，所以直接问条件。 */
    private static boolean matches(GameTestHelper h, String path, String event, Civilization civ, int value) {
        AdvancementHolder a = h.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
        if (a == null) {
            throw new GameTestAssertException("没有进度 " + path);
        }
        var c = a.value().criteria().get("event");
        return c.triggerInstance() instanceof io.github.happysnappingturtle.mythicage.advancement.MythEventTrigger.Instance i
                && c.trigger() == MythEvents.TRIGGER.get() && i.matches(event, Optional.ofNullable(civ), value);
    }

    @GameTest(template = T, batch = BATCH)
    public static void greek_advancements(GameTestHelper h) {
        // TC-T11b-40：6 条各自的条件
        h.assertTrue(matches(h, "greek/root", MythEvents.VILLAGE_ENTERED, GR, 0), "异乡人");
        h.assertTrue(matches(h, "greek/blessed", MythEvents.BLESSED, GR, 0), "祭司的祝福");
        h.assertTrue(matches(h, "greek/first_favor", MythEvents.FAVOR_GAINED, GR, 3), "第一缕香火");
        h.assertTrue(!matches(h, "greek/temple", MythEvents.TEMPLE_LEVEL, GR, 0)
                && matches(h, "greek/temple", MythEvents.TEMPLE_LEVEL, GR, 1), "柱廊之间：0 级不算、1 级算");
        h.assertTrue(!matches(h, "greek/classical", MythEvents.AGE_REACHED, GR, Age.ARCHAIC.ordinal())
                && matches(h, "greek/classical", MythEvents.AGE_REACHED, GR, Age.CLASSICAL.ordinal()), "古典");
        // TC-T11b-42：直接到英雄，古典也成立
        h.assertTrue(matches(h, "greek/classical", MythEvents.AGE_REACHED, GR, Age.HEROIC.ordinal())
                && matches(h, "greek/heroic", MythEvents.AGE_REACHED, GR, Age.HEROIC.ordinal()), "英雄 ≥ 古典");
        h.assertTrue(!matches(h, "greek/blessed", MythEvents.AGE_REACHED, GR, 9), "事件不同不算");
        // TC-T11b-41：埃及的事件不算
        h.assertTrue(!matches(h, "greek/root", MythEvents.VILLAGE_ENTERED, Civilization.EGYPTIAN, 0)
                && !matches(h, "greek/heroic", MythEvents.AGE_REACHED, Civilization.EGYPTIAN, Age.HEROIC.ordinal()), "埃及不算");
        h.succeed();
    }

    /** 各系统确实发出了事件：祝福、晋升（恩宠与神庙等级的发出点由 T10、T8a 的用例路径覆盖）。 */
    @GameTest(template = T, batch = BATCH)
    public static void events_fired_by_systems(GameTestHelper h) {
        floor(h);
        Site s = site(h, "zeus", Age.ARCHAIC, Map.of(), 4, 4);
        FakePlayer p = player(h, "gp_events", Age.NONE, 0);
        List<MythEvents.Fired> fired = new java.util.concurrent.CopyOnWriteArrayList<>();
        MythEvents.testListener = f -> {
            if (f.player().equals(p.getUUID())) {
                fired.add(f);
            }
        };
        try {
            at(p, s);
            TradeService.Outcome b = TradeService.blessing(p, false);
            h.assertTrue(b.reason() == TradeReason.OK, "祝福：" + b);
            h.assertTrue(fired.contains(new MythEvents.Fired(p.getUUID(), MythEvents.BLESSED, GR, 0))
                    && fired.contains(new MythEvents.Fired(p.getUUID(), MythEvents.AGE_REACHED, GR, Age.ARCHAIC.ordinal())),
                    "祝福发出 blessed 与 age_reached：" + fired);
            data(p).setFavor(50);
            give(p, Items.COOKED_BEEF, 8);
            give(p, Items.WHEAT, 32);
            giveId(p, "mythic_age:libation", 4);
            give(p, Items.COPPER_INGOT, 16);
            GlobalPos altar = altarEntry(h, p, 0, 1);
            at(p, s);
            RiteService.Outcome r = RiteService.perform(p, RiteKind.PROMOTION, Age.NONE, "hermes", 50, UUID.randomUUID().toString());
            h.assertTrue(r.reason() == RiteService.Reason.OK
                    && fired.contains(new MythEvents.Fired(p.getUUID(), MythEvents.AGE_REACHED, GR, Age.CLASSICAL.ordinal())),
                    "晋升发出 age_reached：" + r + " " + fired);
            cleanup(h, List.of(altar), s);
        } finally {
            MythEvents.testListener = null;
        }
        h.succeed();
    }
}
