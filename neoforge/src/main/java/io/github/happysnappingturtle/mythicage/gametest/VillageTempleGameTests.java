package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.temple.TempleBook;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GA-13 神庙归村庄：村庄神庙的解析、供奉者、祈祷入账、献供（测试文档 §2，批次 {@value #BATCH}）。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageTempleGameTests {
    static final String BATCH = "village_temple";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;

    private VillageTempleGameTests() {
    }

    /** 放了一座卫城的希腊村子。 */
    record TempleVillage(VillageWorkGameTests.TV tv, BuildPlacement.Placed at, BlockPos origin) {
        UUID id() {
            return tv.id();
        }
    }

    /** 在场地上放一座卫城（模板比场地宽，四周的屏障先拆掉）并登记一个希腊村子，村中心在祭坛东边 6 格。 */
    static TempleVillage village(GameTestHelper h, String buildingId, Age stage) {
        BuildingSpec spec = VillageBuildings.all().get(buildingId);
        BlockPos o = h.absolutePos(new BlockPos(6, 2, 2));
        for (int x = -6; x < 32; x++) {
            for (int z = -2; z < 30; z++) {
                h.getLevel().setBlockAndUpdate(o.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 20; y++) {
                    if (h.getLevel().getBlockState(o.offset(x, y, z)).is(Blocks.BARRIER)) {
                        h.getLevel().setBlockAndUpdate(o.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        StructureTemplate t = h.getLevel().getServer().getStructureManager().get(ResourceLocation.parse(spec.template())).orElseThrow();
        t.placeInWorld(h.getLevel(), o, o, new StructurePlaceSettings(), h.getLevel().getRandom(), 2);
        Pos origin = new Pos(o.getX(), o.getY(), o.getZ());
        BuildPlacement.Placed at = new BuildPlacement.Placed(spec.id(), origin, Turn.R0,
                TemplateTransform.worldBox(origin, spec.sizeX(), spec.sizeY(), spec.sizeZ(), Turn.R0));
        Map<String, List<Pos>> anchors = new LinkedHashMap<>(BuildPlacement.worldAnchors(spec, at));
        Pos center = anchors.get("ritual").get(0).offset(6, 0, 0);
        anchors.put("center", List.of(center));
        VillageInfoPiece.Info info = new VillageInfoPiece.Info("greek", "", "test", 1, "test", center, anchors,
                List.of(at.box()), List.of());
        UUID id = UUID.randomUUID();
        VillageRegistry.Village v = new VillageRegistry.Village(id, GlobalPos.of(h.getLevel().dimension(), VillageInfoPiece.block(center)),
                "greek", "", VillageRegistry.Origin.GENERATED, null, "", -1).withStage(stage);
        VillageRegistry.get(h.getLevel().getServer()).add(v);
        VillagePlans.get(h.getLevel().getServer()).put(new VillagePlans.Plan(id, h.getLevel().dimension(), "test", 1L, info, true));
        VillageScheduler.forceRunningForTest(id, true);
        VillageWorkGameTests.TV tv = new VillageWorkGameTests.TV(v, VillagePlans.get(h.getLevel().getServer()).byVillage(id).orElseThrow());
        VillageEconomy.State s = VillageEconomy.get(h.getLevel().getServer()).state(id);
        s.initialized = true;
        if (!spec.generated()) {
            // 施工建成的卫城：记进施工记录（生成的按锚点反推）
            s.constructed.add(new VillageEconomy.Built(spec.id(), origin, Turn.R0, at.box(), "", "", 0));
        }
        VillageTemples.invalidate(id);
        return new TempleVillage(tv, at, o);
    }

    static void cleanup(GameTestHelper h, TempleVillage tv) {
        VillageTemples.forceIntegrityForTest(tv.id(), -1);
        VillageWorkGameTests.cleanup(h, tv.tv());
        VillageTemples.invalidate(tv.id());
        // 卫城比场地宽：场地外的部分拆掉，免得干扰后面复用这块地的用例
        for (BlockPos q : BlockPos.betweenClosed(tv.origin().offset(-6, -1, -2), tv.origin().offset(32, 20, 30))) {
            if (!h.getLevel().getBlockState(q).isAir()) {
                h.getLevel().setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    // ───────────── 阶段 ①：解析 ─────────────

    /** TC-GA13-01（世界内）：远古卫城 → 1 级、系数 1.5；祭坛是模板里那块村庄祭坛；有祈祷位；完好度打折（TC-GA13-02）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void temple_resolves_from_acropolis(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        try {
            VillageTemple t = VillageTemples.of(h.getLevel().getServer(), tv.id()).orElseThrow();
            h.assertTrue(t.level() == 1, "远古卫城 1 级，实际 " + t.level());
            h.assertTrue(Math.abs(t.coefficient() - 1.5) < 1e-9, "系数 1.5，实际 " + t.coefficient());
            h.assertTrue(h.getLevel().getBlockState(t.altar().pos()).is(GreekVillageBlocks.VILLAGE_ALTAR.get()),
                    "祭坛是村庄祭坛方块：" + t.altar().pos() + " " + h.getLevel().getBlockState(t.altar().pos()));
            h.assertTrue(tv.at().box().containsXZ(t.center().getX(), t.center().getZ()), "48 格圆心在卫城范围里");
            h.assertTrue(!t.prayerSlots().isEmpty() && t.prayerSlots().size() <= t.slotLimit(),
                    "有祈祷位：" + t.prayerSlots().size() + "/" + t.slotLimit());
            h.assertTrue(VillageTemples.byAltar(h.getLevel().getServer(), t.altar()).isPresent(), "按祭坛位置能找回这个村子");
            VillageTemples.forceIntegrityForTest(tv.id(), 0.6);
            VillageTemple worn = VillageTemples.of(h.getLevel().getServer(), tv.id()).orElseThrow();
            h.assertTrue(Math.abs(worn.coefficient() - 0.9) < 1e-9, "完好度 0.6 → 系数 0.9，实际 " + worn.coefficient());
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** 施工建成的中神庙 → 2 级、系数 2.0。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void temple_resolves_classical(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_classical", Age.CLASSICAL);
        try {
            VillageTemple t = VillageTemples.of(h.getLevel().getServer(), tv.id()).orElseThrow();
            h.assertTrue(t.level() == 2 && Math.abs(t.coefficient() - 2.0) < 1e-9, "中神庙 2 级 / 2.0，实际 " + t.level() + " / " + t.coefficient());
            h.assertTrue(h.getLevel().getBlockState(t.altar().pos()).is(GreekVillageBlocks.VILLAGE_ALTAR.get()), "祭坛是村庄祭坛方块");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    /** 神庙簿随村庄运行数据存档：供奉者、信徒、献供、完好度。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void temple_book_round_trip(GameTestHelper h) {
        VillageEconomy e = new VillageEconomy();
        UUID v = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID villager = UUID.randomUUID();
        TempleBook b = e.state(v).temple;
        b.addDevotee(a, 5, TempleBook.Via.BLESSING);
        b.notePrayer(villager, 7);
        b.markOffered(a, 7);
        b.setIntegrity(0.75);
        TempleBook r = VillageEconomy.roundTripForTest(e, h.getLevel().registryAccess()).state(v).temple;
        h.assertTrue(r.isDevotee(a) && r.devoteeView().get(a).via() == TempleBook.Via.BLESSING && r.devoteeView().get(a).since() == 5,
                "供奉者与方式");
        h.assertTrue(r.prayedToday(villager, 7) && r.believersToday(7) == 1, "信徒");
        h.assertTrue(r.offeredOn(a, 7) && !r.offeredOn(a, 8), "献供日");
        h.assertTrue(Math.abs(r.integrity() - 0.75) < 1e-9, "完好度");
        h.succeed();
    }

    // ───────────── 阶段 ②：供奉者、祈祷入账、献供、晋升条件 ─────────────

    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h, String name, Age age) {
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
        p.getInventory().clearContent();
        io.github.happysnappingturtle.mythicage.player.MythData.get(p).civ(io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK)
                .setAge(age);
        return p;
    }

    private static io.github.happysnappingturtle.mythicage.guardian.GuardianEntity guardian(GameTestHelper h, TempleVillage tv) {
        BlockPos at = tv.tv().village().bell().pos();
        var g = io.github.happysnappingturtle.mythicage.guardian.GuardianSpawner.spawnNew(h.getLevel(), at,
                java.util.Optional.of(at), io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK);
        if (g == null) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("祭司没有生成");
        }
        g.setNoAi(true);
        return g;
    }

    private static void cleanupDevotees(GameTestHelper h, TempleVillage tv) {
        var server = h.getLevel().getServer();
        for (UUID d : List.copyOf(VillageTemples.book(server, tv.id()).devotees())) {
            io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.dropLedger(server, tv.id(), d);
        }
        io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.resetForTest(tv.id());
        h.killAllEntitiesOfClass(io.github.happysnappingturtle.mythicage.guardian.GuardianEntity.class);
    }

    /** TC-GA13-20：在本村守护者处受祝福 → 成为这个村子的供奉者，背包里没有祭坛；已在别处受过祝福的人可以「在这里供奉」。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void blessing_makes_devotee(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        try {
            var g = guardian(h, tv);
            var p = player(h, "tp_bless", Age.NONE);
            p.moveTo(g.getX() + 2, g.getY(), g.getZ());
            io.github.happysnappingturtle.mythicage.trade.TradeService.openGuardian(p, g);
            var o = io.github.happysnappingturtle.mythicage.trade.TradeService.blessing(p, false);
            h.assertTrue(o.reason() == io.github.happysnappingturtle.mythicage.core.trade.TradeReason.OK, "受祝福：" + o.reason());
            TempleBook book = VillageTemples.book(server, tv.id());
            h.assertTrue(book.isDevotee(p.getUUID()) && book.devoteeView().get(p.getUUID()).via() == TempleBook.Via.BLESSING,
                    "成为供奉者（祝福）");
            h.assertTrue(p.getInventory().countItem(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_ALTAR.get().asItem()) == 0,
                    "不发祭坛");
            h.assertTrue(io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers.get(server)
                    .get(io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.ledgerId(tv.id(), p.getUUID())).isPresent(),
                    "开好台账");
            // 在别处受过祝福的人：对话是「在这里供奉」
            var q = player(h, "tp_join", Age.ARCHAIC);
            q.moveTo(g.getX() - 2, g.getY(), g.getZ());
            io.github.happysnappingturtle.mythicage.trade.TradeService.openGuardian(q, g);
            h.assertTrue(io.github.happysnappingturtle.mythicage.trade.TradeService.viewForTest(q).orElseThrow().dialogue()
                    == io.github.happysnappingturtle.mythicage.trade.TradeView.Dialogue.JOIN, "对话：在这里供奉");
            var j = io.github.happysnappingturtle.mythicage.trade.TradeService.blessing(q, true);
            h.assertTrue(j.reason() == io.github.happysnappingturtle.mythicage.core.trade.TradeReason.OK && j.given().isEmpty()
                    && book.isDevotee(q.getUUID()) && book.devoteeView().get(q.getUUID()).via() == TempleBook.Via.JOIN, "加入：" + j.reason());
            h.assertTrue(io.github.happysnappingturtle.mythicage.trade.TradeService.blessing(q, true).reason()
                    == io.github.happysnappingturtle.mythicage.core.trade.TradeReason.ALREADY_DEVOTEE, "再点：已经是供奉者");
            h.assertTrue(io.github.happysnappingturtle.mythicage.trade.TradeService.viewForTest(q).orElseThrow().dialogue()
                    == io.github.happysnappingturtle.mythicage.trade.TradeView.Dialogue.NONE, "加入后对话恢复普通问候");
            h.succeed();
        } finally {
            cleanupDevotees(h, tv);
            cleanup(h, tv);
        }
    }

    /** TC-GA13-21、22：远古卫城（系数 1.5）的一次祈祷给两位供奉者各入一笔；献供只加献的那一位（TC-GA13-06）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void prayer_credits_each_devotee(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        try {
            var a = player(h, "tp_a", Age.ARCHAIC);
            var b = player(h, "tp_b", Age.ARCHAIC);
            long today = io.github.happysnappingturtle.mythicage.guardian.MythDays.today(server);
            VillageTemples.addDevotee(server, tv.id(), a.getUUID(), today, TempleBook.Via.JOIN);
            VillageTemples.addDevotee(server, tv.id(), b.getUUID(), today, TempleBook.Via.JOIN);
            VillageTemple t = VillageTemples.of(server, tv.id()).orElseThrow();
            var lib = new net.minecraft.world.item.ItemStack(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.LIBATION.get());
            h.assertTrue(io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.offer(a, t, lib)
                    == io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService.OfferResult.OK, "甲献供");
            h.assertTrue(io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.credit(h.getLevel(), tv.id(), UUID.randomUUID()),
                    "一次祈祷入账");
            double base = io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula.DAILY_BASE * 1.5;
            double pa = io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.pending(tv.id(), a.getUUID());
            double pb = io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.pending(tv.id(), b.getUUID());
            h.assertTrue(Math.abs(pb - base) < 1e-9, "乙按系数 1.5 全额：" + pb);
            h.assertTrue(Math.abs(pa - base * 1.5) < 1e-9, "甲献过供 ×1.5：" + pa);
            // 有顶的神庙下雨不减
            io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService.overrideRainForTest((l, pos) -> true);
            try {
                io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.credit(h.getLevel(), tv.id(), UUID.randomUUID());
            } finally {
                io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService.overrideRainForTest(null);
            }
            double pb2 = io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.pending(tv.id(), b.getUUID()) - pb;
            double second = io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula.credit(2, 1.5, 1, 0, 0, 1);
            h.assertTrue(Math.abs(pb2 - second) < 1e-9, "小神庙下雨不减：" + pb2 + " / " + second);
            h.succeed();
        } finally {
            cleanupDevotees(h, tv);
            cleanup(h, tv);
        }
    }

    /** TC-GA13-23、27：右键神庙祭坛献供（非供奉者不扣物品）；村里别处的村庄祭坛没有这些交互。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void altar_click_offers_only_at_temple(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        try {
            VillageTemple t = VillageTemples.of(server, tv.id()).orElseThrow();
            var p = player(h, "tp_click", Age.ARCHAIC);
            var lib = new net.minecraft.world.item.ItemStack(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.LIBATION.get(), 2);
            var stranger = io.github.happysnappingturtle.mythicage.content.greek.temple.VillageAltarUse.useItemOn(lib, h.getLevel(),
                    t.altar().pos(), p);
            h.assertTrue(stranger == net.minecraft.world.ItemInteractionResult.CONSUME && lib.getCount() == 2, "非供奉者：提示、不扣");
            long today = io.github.happysnappingturtle.mythicage.guardian.MythDays.today(server);
            VillageTemples.addDevotee(server, tv.id(), p.getUUID(), today, TempleBook.Via.JOIN);
            io.github.happysnappingturtle.mythicage.content.greek.temple.VillageAltarUse.useItemOn(lib, h.getLevel(), t.altar().pos(), p);
            h.assertTrue(lib.getCount() == 1 && VillageTemples.book(server, tv.id()).offeredOn(p.getUUID(), today), "供奉者献供成功");
            // 场地角上另放一块村庄祭坛：只是装饰
            BlockPos deco = h.absolutePos(new BlockPos(1, 2, 33));
            h.getLevel().setBlockAndUpdate(deco, GreekVillageBlocks.VILLAGE_ALTAR.get().defaultBlockState());
            var lib2 = new net.minecraft.world.item.ItemStack(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.LIBATION.get());
            var r = io.github.happysnappingturtle.mythicage.content.greek.temple.VillageAltarUse.useItemOn(lib2, h.getLevel(), deco, p);
            h.assertTrue(r == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && lib2.getCount() == 1,
                    "装饰用的村庄祭坛不收供品：" + r);
            h.assertTrue(!io.github.happysnappingturtle.mythicage.content.greek.temple.VillageAltarUse.useWithoutItem(h.getLevel(), deco, p),
                    "装饰用的村庄祭坛不开检视");
            h.succeed();
        } finally {
            cleanupDevotees(h, tv);
            cleanup(h, tv);
        }
    }

    private static com.google.gson.JsonObject level(int n) {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("level", n);
        return o;
    }

    /** TC-GA13-29：晋升条件看仪式所在村：远古卫城不够 2 级；中神庙够；完好度 0.7 不够。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void promotion_reads_village_temple(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.CLASSICAL);
        try {
            var p = player(h, "tp_promo", Age.CLASSICAL);
            var req = io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTempleRequirement.INSTANCE;
            var gr = io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;
            h.assertTrue(!req.check(p, gr, Age.HEROIC, tv.tv().village(), level(2)).met(), "远古卫城：英雄要求中神庙，不满足");
            h.assertTrue(req.check(p, gr, Age.CLASSICAL, tv.tv().village(), level(1)).met(), "远古卫城：1 级满足");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void promotion_needs_intact_temple(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_classical", Age.CLASSICAL);
        var server = h.getLevel().getServer();
        try {
            var p = player(h, "tp_intact", Age.CLASSICAL);
            var req = io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTempleRequirement.INSTANCE;
            var gr = io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;
            h.assertTrue(req.check(p, gr, Age.HEROIC, tv.tv().village(), level(2)).met(), "中神庙完好：满足");
            VillageTemples.forceIntegrityForTest(tv.id(), 0.7);
            h.assertTrue(!req.check(p, gr, Age.HEROIC, tv.tv().village(), level(2)).met(), "完好度 70%：不满足");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    // ───────────── 阶段 ③：圣物供奉 ─────────────

    private static net.minecraft.world.item.ItemStack armor() {
        return new net.minecraft.world.item.ItemStack(io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics.ARMOR_OF_ACHILLES.get());
    }

    private static net.minecraft.world.item.ItemStack crown() {
        return new net.minecraft.world.item.ItemStack(io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics.RHEIA_CROWN.get());
    }

    private static net.minecraft.world.item.ItemStack statue() {
        return new net.minecraft.world.item.ItemStack(io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics.PYGMALION_STATUE.get());
    }

    private static float hit(net.minecraft.world.entity.LivingEntity e, net.minecraft.world.damagesource.DamageSource s) {
        e.setHealth(e.getMaxHealth());
        float before = e.getHealth();
        e.invulnerableTime = 0;
        e.hurt(s, 10);
        return before - e.getHealth();
    }

    /** 手持圣物潜行右键神庙祭坛；返回手里是否空了（= 放进去了）。 */
    private static boolean offerRelic(GameTestHelper h, net.neoforged.neoforge.common.util.FakePlayer p, VillageTemple t,
                                      net.minecraft.world.item.ItemStack s) {
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, s);
        p.setShiftKeyDown(true);
        io.github.happysnappingturtle.mythicage.content.greek.temple.VillageAltarUse.useItemOn(p.getMainHandItem(), h.getLevel(),
                t.altar().pos(), p);
        p.setShiftKeyDown(false);
        boolean in = p.getMainHandItem().isEmpty();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        return in;
    }

    private static net.neoforged.neoforge.common.util.FakePlayer devotee(GameTestHelper h, TempleVillage tv, String name, Age age) {
        var p = player(h, name, age);
        p.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(tv.tv().village().bell().pos()));
        io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(p, true);
        VillageTemples.addDevotee(h.getLevel().getServer(), tv.id(), p.getUUID(),
                io.github.happysnappingturtle.mythicage.guardian.MythDays.today(h.getLevel().getServer()), TempleBook.Via.JOIN);
        return p;
    }

    /** TC-GA13-25、26：潜行右键放进自己的供奉位；效果以卫城为圆心 48 格（外面不减）；取回后效果消失、物品回背包。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void relic_offer_and_retrieve(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        var p = devotee(h, tv, "tr_offer", Age.ARCHAIC);
        var z = h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, 1, 2, 33);
        z.setNoAi(true);
        var melee = h.getLevel().damageSources().mobAttack(z);
        var near = h.spawn(net.minecraft.world.entity.EntityType.VILLAGER, 2, 2, 30);
        near.setNoAi(true);
        var far = net.minecraft.world.entity.EntityType.VILLAGER.create(h.getLevel());
        try {
            VillageTemple t = VillageTemples.of(server, tv.id()).orElseThrow();
            net.minecraft.world.phys.Vec3 c = net.minecraft.world.phys.Vec3.atCenterOf(t.center());
            far.moveTo(c.x, c.y + 49, c.z);
            far.setNoAi(true);
            h.getLevel().addFreshEntity(far);
            h.assertTrue(Math.abs(hit(near, melee) - 10) < 1e-3, "前提：没供奉时不减");
            h.assertTrue(offerRelic(h, p, t, armor()), "放进供奉位");
            var b = io.github.happysnappingturtle.mythicage.player.MythData.get(p).relics().get("greek:armor_of_achilles").orElseThrow();
            h.assertTrue(b.state() == io.github.happysnappingturtle.mythicage.core.relic.RelicState.OFFERED
                    && b.where().equals(io.github.happysnappingturtle.mythicage.relic.TempleRelics.where(tv.id())), "绑定记在这个村子：" + b);
            h.assertTrue(VillageTemples.book(server, tv.id()).relics(p.getUUID()).size() == 1, "神庙簿里有这一件");
            h.assertTrue(Math.abs(hit(near, melee) - 8.5) < 1e-3, "卫城 48 格内的村民 −15%：" + hit(near, melee));
            var resident = io.github.happysnappingturtle.mythicage.village.resident.Residents.spawn(h.getLevel(), tv.tv().village(),
                    io.github.happysnappingturtle.mythicage.village.work.VillageData.civ("greek").orElseThrow(), "greek:laborer",
                    h.absolutePos(new BlockPos(4, 2, 30)), h.absolutePos(new BlockPos(4, 2, 30)), false).orElseThrow();
            resident.setNoAi(true);
            h.assertTrue(Math.abs(hit(resident, melee) - 8.5) < 1e-3, "本作居民也算友方 −15%：" + hit(resident, melee));
            h.assertTrue(Math.abs(hit(far, melee) - 10) < 1e-3, "49 格外不减");
            io.github.happysnappingturtle.mythicage.relic.RelicService.reconcile(p);
            h.assertTrue(io.github.happysnappingturtle.mythicage.player.MythData.get(p).relics().get("greek:armor_of_achilles").isPresent(),
                    "对账后绑定还在");
            // 取回（检视界面的「取出」走同一入口）
            h.assertTrue(io.github.happysnappingturtle.mythicage.relic.RelicService.retrieve(p, h.getLevel(), t.altar().pos(), 0), "取出");
            h.assertTrue(p.getInventory().countItem(io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics.ARMOR_OF_ACHILLES.get()) == 1
                    && io.github.happysnappingturtle.mythicage.player.MythData.get(p).relics().get("greek:armor_of_achilles").isEmpty(),
                    "回到背包、解绑");
            h.assertTrue(Math.abs(hit(near, melee) - 10) < 1e-3, "取回后效果消失");
            h.succeed();
        } finally {
            far.discard();
            h.killAllEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class);
            h.killAllEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class);
            h.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
            io.github.happysnappingturtle.mythicage.relic.OfferedIndex.removeVillage(tv.id());
            io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(p, false);
            cleanupDevotees(h, tv);
            cleanup(h, tv);
        }
    }

    /** 皮格马利翁雕像供奉在村庄神庙：48 格内本村居民的生命上限 ×1.4（居民也是信徒）。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void statue_blesses_residents(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        var p = devotee(h, tv, "tr_statue", Age.ARCHAIC);
        var resident = io.github.happysnappingturtle.mythicage.village.resident.Residents.spawn(h.getLevel(), tv.tv().village(),
                io.github.happysnappingturtle.mythicage.village.work.VillageData.civ("greek").orElseThrow(), "greek:laborer",
                h.absolutePos(new BlockPos(4, 2, 30)), h.absolutePos(new BlockPos(4, 2, 30)), false).orElseThrow();
        resident.setNoAi(true);
        float base = resident.getMaxHealth();
        VillageTemple t = VillageTemples.of(server, tv.id()).orElseThrow();
        h.assertTrue(offerRelic(h, p, t, statue()), "供奉雕像");
        h.runAfterDelay(25, () -> {
            try {
                h.assertTrue(Math.abs(resident.getMaxHealth() - base * 1.4F) < 1e-3, "居民生命上限 ×1.4：" + base + " → " + resident.getMaxHealth());
                h.succeed();
            } finally {
                io.github.happysnappingturtle.mythicage.relic.OfferedIndex.removeVillage(tv.id());
                io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(p, false);
                cleanupDevotees(h, tv);
                cleanup(h, tv);
            }
        });
    }

    /** TC-GA13-07（世界内）：古典玩家在甲村 2 个位、乙村 2 个位，各自独立；同一件不能同时在两处；非供奉者放不进。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void relic_slots_per_village(GameTestHelper h) {
        TempleVillage a = village(h, "greek:acropolis_archaic", Age.CLASSICAL);
        var server = h.getLevel().getServer();
        // 乙村：没有卫城的小村，神庙在钟的位置（场地角上）
        BlockPos bellB = h.absolutePos(new BlockPos(1, 2, 33));
        var vb = new io.github.happysnappingturtle.mythicage.village.VillageRegistry.Village(UUID.randomUUID(),
                GlobalPos.of(h.getLevel().dimension(), bellB), "greek", "zeus",
                io.github.happysnappingturtle.mythicage.village.VillageRegistry.Origin.VANILLA, null, "plains", -1);
        io.github.happysnappingturtle.mythicage.village.VillageRegistry.get(server).add(vb);
        var p = devotee(h, a, "tr_slots", Age.CLASSICAL);
        var stranger = player(h, "tr_stranger", Age.CLASSICAL);
        io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(stranger, true);
        try {
            VillageTemple ta = VillageTemples.of(server, a.id()).orElseThrow();
            VillageTemple tb = VillageTemples.of(server, vb.id()).orElseThrow();
            h.assertTrue(!offerRelic(h, stranger, ta, armor()), "不是供奉者放不进");
            h.assertTrue(offerRelic(h, p, ta, armor()) && offerRelic(h, p, ta, crown()), "甲村放 2 件");
            h.assertTrue(!offerRelic(h, p, ta, statue()), "甲村第 3 件：满了");
            VillageTemples.addDevotee(server, vb.id(), p.getUUID(), 0, TempleBook.Via.JOIN);
            h.assertTrue(offerRelic(h, p, tb, statue()), "乙村另有自己的供奉位");
            var held = armor();
            h.assertTrue(io.github.happysnappingturtle.mythicage.relic.TempleRelics.offerError(p, tb,
                    io.github.happysnappingturtle.mythicage.relic.RelicCards.of(held).orElseThrow(), held).isPresent(),
                    "同一件（阿喀琉斯之甲）已在甲村，乙村拒绝");
            h.succeed();
        } finally {
            h.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
            io.github.happysnappingturtle.mythicage.relic.OfferedIndex.removeVillage(a.id());
            io.github.happysnappingturtle.mythicage.relic.OfferedIndex.removeVillage(vb.id());
            io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(p, false);
            io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(stranger, false);
            io.github.happysnappingturtle.mythicage.village.VillageRegistry.get(server).remove(vb.bell());
            VillageEconomy.get(server).clearForTest(vb.id());
            VillageTemples.invalidate(vb.id());
            cleanupDevotees(h, a);
            cleanup(h, a);
        }
    }

    /** TC-GA13-33：退出供奉：不再入账、台账注销、圣物退回并解绑；重新加入后恢复入账。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void leave_devotion(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        var p = devotee(h, tv, "tr_leave", Age.ARCHAIC);
        try {
            VillageTemple t = VillageTemples.of(server, tv.id()).orElseThrow();
            io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.ensureLedger(server, t, p.getUUID());
            h.assertTrue(offerRelic(h, p, t, crown()), "先供奉一件");
            h.assertTrue(io.github.happysnappingturtle.mythicage.content.greek.temple.TempleMembership.leave(p, tv.id()), "退出");
            h.assertTrue(!VillageTemples.book(server, tv.id()).isDevotee(p.getUUID()), "不再是供奉者");
            h.assertTrue(p.getInventory().countItem(io.github.happysnappingturtle.mythicage.relic.greek.GreekRelics.RHEIA_CROWN.get()) == 1
                    && io.github.happysnappingturtle.mythicage.player.MythData.get(p).relics().get("greek:rheia_crown").isEmpty(),
                    "圣物退回背包、解绑");
            h.assertTrue(io.github.happysnappingturtle.mythicage.runtime.RuntimeLedgers.get(server)
                    .get(io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.ledgerId(tv.id(), p.getUUID())).isEmpty(),
                    "台账注销");
            io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.credit(h.getLevel(), tv.id(), UUID.randomUUID());
            h.assertTrue(io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.pending(tv.id(), p.getUUID()) == 0,
                    "退出后不入账");
            h.assertTrue(!io.github.happysnappingturtle.mythicage.content.greek.temple.TempleMembership.leave(p, tv.id()), "再退：不是供奉者");
            VillageTemples.addDevotee(server, tv.id(), p.getUUID(), 0, TempleBook.Via.JOIN);
            io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.credit(h.getLevel(), tv.id(), UUID.randomUUID());
            h.assertTrue(io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer.pending(tv.id(), p.getUUID()) > 0,
                    "重新加入后恢复入账");
            h.succeed();
        } finally {
            h.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
            io.github.happysnappingturtle.mythicage.relic.OfferedIndex.removeVillage(tv.id());
            io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(p, false);
            cleanupDevotees(h, tv);
            cleanup(h, tv);
        }
    }

    // ───────────── 阶段 ④：完好度 ─────────────

    /** TC-GA13-28：拆掉卫城 4 格柱子：记进损坏、完好度按比例下降、系数跟着降；修好后恢复。 */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void integrity_follows_damage(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        try {
            BuildingSpec spec = VillageBuildings.all().get("greek:acropolis_archaic");
            int total = io.github.happysnappingturtle.mythicage.content.greek.temple.TempleIntegrity.total(server, spec);
            h.assertTrue(total > 0, "模板里有关键方块：" + total);
            VillageTemple t = VillageTemples.of(server, tv.id()).orElseThrow();
            h.assertTrue(Math.abs(t.integrity() - 1) < 1e-9, "刚放好：完好");
            java.util.List<BlockPos> pillars = new java.util.ArrayList<>();
            var b = tv.at().box();
            for (BlockPos q : BlockPos.betweenClosed(new BlockPos(b.minX(), b.minY(), b.minZ()), new BlockPos(b.maxX(), b.maxY(), b.maxZ()))) {
                if (pillars.size() < 4 && h.getLevel().getBlockState(q).is(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_COLUMN)) {
                    pillars.add(q.immutable());
                }
            }
            h.assertTrue(pillars.size() == 4, "找到 4 格柱子：" + pillars.size());
            java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> was = new java.util.LinkedHashMap<>();
            for (BlockPos q : pillars) {
                var st = h.getLevel().getBlockState(q);
                was.put(q, st);
                io.github.happysnappingturtle.mythicage.village.quest.BuildingDamage.record(h.getLevel(), q, st, UUID.randomUUID());
                h.getLevel().setBlock(q, Blocks.AIR.defaultBlockState(), 3);
            }
            VillageTemple worn = VillageTemples.of(server, tv.id()).orElseThrow();
            double expect = (total - 4) / (double) total;
            h.assertTrue(Math.abs(worn.integrity() - expect) < 1e-9, "完好度 " + expect + "，实际 " + worn.integrity());
            h.assertTrue(Math.abs(worn.coefficient() - 1.5 * expect) < 1e-9, "系数按比例降：" + worn.coefficient());
            var damage = io.github.happysnappingturtle.mythicage.village.quest.VillageQuests.get(server).peek(tv.id())
                    .map(x -> x.damage.get(io.github.happysnappingturtle.mythicage.village.quest.VillageQuests.Damage.key(b))).orElse(null);
            h.assertTrue(damage != null && damage.cells.size() == 4, "进了修缮流程的损坏记录（发委托、到期村民自修）");
            // 修好（同村民自修：放回原方块、去掉损坏格）
            was.forEach((q, st) -> {
                h.getLevel().setBlock(q, st, 3);
                damage.cells.remove(q.asLong());
            });
            VillageTemples.invalidate(tv.id());
            h.assertTrue(Math.abs(VillageTemples.of(server, tv.id()).orElseThrow().integrity() - 1) < 1e-9, "修完恢复");
            h.succeed();
        } finally {
            cleanup(h, tv);
        }
    }

    // ───────────── 阶段 ⑤：快速验证的神庙准备 ─────────────

    /** TC-GA13-34：「神庙扩建」站：村子推进到古典、开工中神庙（替换远古卫城）、材料齐、施工 ×8。 */
    @GameTest(template = T, batch = "village_temple_build", timeoutTicks = 200)
    public static void playtest_upgrade_starts(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        try {
            VillageEconomy.get(server).state(tv.id()).stock.setCapacity(100000);
            var p = io.github.happysnappingturtle.mythicage.playtest.PlaytestTemple.startUpgrade(h.getLevel(), tv.id(),
                    io.github.happysnappingturtle.mythicage.playtest.PlaytestTemple.UPGRADE_SPEED);
            h.assertTrue(p.isPresent(), "开工");
            h.assertTrue(p.get().building.startsWith("greek:acropolis_classical") && p.get().replaces.startsWith("greek:acropolis_archaic"),
                    "中神庙替换远古卫城：" + p.get().building + " ← " + p.get().replaces);
            h.assertTrue(p.get().state == io.github.happysnappingturtle.mythicage.core.village.build.BuildProject.State.BUILDING,
                    "材料齐、开始施工：" + p.get().state + " 缺 " + p.get().missing());
            h.assertTrue(io.github.happysnappingturtle.mythicage.village.VillageRegistry.get(server).byId(tv.id()).orElseThrow().stage() == Age.CLASSICAL,
                    "村子进入古典");
            h.assertTrue(io.github.happysnappingturtle.mythicage.village.build.Construction.speed(tv.id())
                    == io.github.happysnappingturtle.mythicage.playtest.PlaytestTemple.UPGRADE_SPEED, "施工加速");
            h.succeed();
        } finally {
            io.github.happysnappingturtle.mythicage.village.build.Construction.speed(tv.id(), 1);
            io.github.happysnappingturtle.mythicage.village.build.Construction.clearForTest(tv.id());
            cleanup(h, tv);
        }
    }

    /** TC-GA13-30、34：要 2 级的站：开工中神庙后每刻直接逐格落下，落成后系数 1.5 → 2.0，祭坛跟着新卫城走。 */
    @GameTest(template = T, batch = "village_temple_build", timeoutTicks = 12000)
    public static void playtest_ensure_level_builds(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        VillageEconomy.get(server).state(tv.id()).stock.setCapacity(100000);
        h.assertTrue(Math.abs(VillageTemples.of(server, tv.id()).orElseThrow().coefficient() - 1.5) < 1e-9, "前提：小神庙 1.5");
        h.assertTrue(io.github.happysnappingturtle.mythicage.playtest.PlaytestTemple.startUpgrade(h.getLevel(), tv.id(), 1).isPresent(), "开工");
        h.startSequence().thenWaitUntil(() -> {
            io.github.happysnappingturtle.mythicage.playtest.PlaytestTemple.advance(h.getLevel(), tv.id());
            h.assertTrue(io.github.happysnappingturtle.mythicage.playtest.PlaytestTemple.level(h.getLevel(), tv.id()) >= 2, "扩建中");
        }).thenExecute(() -> {
            try {
                VillageTemple t = VillageTemples.of(server, tv.id()).orElseThrow();
                h.assertTrue(t.level() == 2 && Math.abs(t.coefficient() - 2.0) < 1e-9, "2 级、系数 2.0：" + t.level() + " / " + t.coefficient());
                h.assertTrue(h.getLevel().getBlockState(t.altar().pos()).is(GreekVillageBlocks.VILLAGE_ALTAR.get()), "祭坛是新卫城的那块");
            } finally {
                io.github.happysnappingturtle.mythicage.village.build.Construction.clearForTest(tv.id());
                cleanup(h, tv);
            }
        }).thenSucceed();
    }

    // ───────────── 阶段 ⑤：旧存档迁移 ─────────────

    /**
     * TC-GA13-31：旧存档里玩家祭坛在村里、供奉了 1 件圣物：加载（绑定）后变成装饰——登记注销、主人清空、方块还在；
     * 圣物挪进主人在这个村子的供奉位，主人成了这里的供奉者；对账后绑定改到村子。另一位只在登记里有旧祭坛的玩家上线时也成为供奉者，只做一次。
     */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void legacy_altar_migrates(GameTestHelper h) {
        TempleVillage tv = village(h, "greek:acropolis_archaic", Age.ARCHAIC);
        var server = h.getLevel().getServer();
        var owner = player(h, "tl_owner", Age.ARCHAIC);
        io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(owner, true);
        BlockPos rel = new BlockPos(2, 2, 32);
        BlockPos abs = h.absolutePos(rel);
        GlobalPos key = GlobalPos.of(h.getLevel().dimension(), abs);
        var other = player(h, "tl_other", Age.ARCHAIC);
        GlobalPos far = GlobalPos.of(h.getLevel().dimension(), tv.tv().village().bell().pos().offset(20, 0, 0));
        try {
            h.getLevel().setBlockAndUpdate(abs, io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_ALTAR.get()
                    .defaultBlockState());
            var be = (io.github.happysnappingturtle.mythicage.content.greek.temple.GreekAltarBlockEntity) h.getLevel().getBlockEntity(abs);
            var crown = crown();
            UUID inst = io.github.happysnappingturtle.mythicage.relic.RelicItems.ensureInstance(crown);
            be.relics().add(crown);
            io.github.happysnappingturtle.mythicage.player.MythData.get(owner).relics().bind("greek:rheia_crown",
                    new io.github.happysnappingturtle.mythicage.core.relic.RelicBinding(io.github.happysnappingturtle.mythicage.core.relic.RelicState.OFFERED,
                            inst, io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK,
                            io.github.happysnappingturtle.mythicage.core.relic.RelicBinding.shrineWhere(key.dimension().location().toString(),
                                    abs.getX(), abs.getY(), abs.getZ())));
            // 旧存档读档：方块实体带主人，走绑定路径（同加载时）
            io.github.happysnappingturtle.mythicage.shrine.ShrineService.bindForTest(h.getLevel(), abs, owner.getUUID(), "tl_owner",
                    Age.ARCHAIC, net.minecraft.core.Direction.SOUTH);
            h.assertTrue(h.getLevel().getBlockState(abs).is(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_ALTAR.get()),
                    "方块还在（成了装饰）");
            h.assertTrue(io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry.get(server).get(key).isEmpty(), "神坛登记注销");
            h.assertTrue(be.owner().isEmpty() && be.relics().isEmpty(), "方块实体清掉主人与圣物");
            TempleBook book = VillageTemples.book(server, tv.id());
            h.assertTrue(book.isDevotee(owner.getUUID()) && book.devoteeView().get(owner.getUUID()).via() == TempleBook.Via.MIGRATION,
                    "主人成了这里的供奉者（迁移）");
            h.assertTrue(book.relics(owner.getUUID()).size() == 1, "圣物进了主人在这个村子的供奉位");
            io.github.happysnappingturtle.mythicage.relic.RelicService.reconcile(owner);
            var b = io.github.happysnappingturtle.mythicage.player.MythData.get(owner).relics().get("greek:rheia_crown").orElseThrow();
            h.assertTrue(b.where().equals(io.github.happysnappingturtle.mythicage.relic.TempleRelics.where(tv.id())) && b.instance().equals(inst),
                    "对账后绑定改到村子：" + b);
            // 只在登记里有旧祭坛（区块没加载）的玩家：上线时成为供奉者
            io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry.get(server).add(new io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry.Entry(
                    far, other.getUUID(), "tl_other", net.minecraft.core.Direction.SOUTH,
                    io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK, "greek:altar"));
            io.github.happysnappingturtle.mythicage.content.greek.temple.LegacyAltars.onLogin(other);
            h.assertTrue(book.isDevotee(other.getUUID()), "上线后成为最近村子的供奉者");
            h.assertTrue(io.github.happysnappingturtle.mythicage.player.MythData.get(other).receipts().contains(
                    io.github.happysnappingturtle.mythicage.core.player.ReceiptBook.Scope.UNIQUE,
                    io.github.happysnappingturtle.mythicage.content.greek.temple.LegacyAltars.RECEIPT), "只提示一次（回执）");
            h.succeed();
        } finally {
            io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry.get(server).remove(far);
            io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry.get(server).remove(key);
            io.github.happysnappingturtle.mythicage.relic.OfferedIndex.removeVillage(tv.id());
            io.github.happysnappingturtle.mythicage.relic.RelicService.onlineForTest(owner, false);
            cleanupDevotees(h, tv);
            cleanup(h, tv);
        }
    }

    /** TC-GA13-32：新放下的旧祭坛（背包里的旧物品）只是装饰：不登记、不会被当成「无主神坛」拆掉。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH, timeoutTicks = 100)
    public static void old_altar_is_decoration(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 1, 1);
        h.setBlock(rel, io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_ALTAR.get());
        GlobalPos key = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(rel));
        h.runAfterDelay(10, () -> {
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(rel)).is(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_ALTAR.get()),
                    "没有被移除");
            h.assertTrue(io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry.get(h.getLevel().getServer()).get(key).isEmpty(), "不登记");
            h.succeed();
        });
    }
}
