package io.github.happysnappingturtle.mythicage.core.rite;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeReason;
import io.github.happysnappingturtle.mythicage.core.trade.TradeRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T11a 数据解析、兑换地点、成功界面清单与阶段效果（TC-T11a-23～39b）。 */
class RiteDataTest {
    static final Civilization C = Civilization.GREEK;

    // ───────────── 数据解析（TC-T11a-23～27） ─────────────

    @Test
    void tc23_promotionTable() {
        List<String> w = new ArrayList<>();
        PromotionTable t = PromotionTable.parse(RiteRulesTest.TABLE.replace("\"trial\":\"classical\"}",
                        "\"trial\":\"classical\",\"extraUnlocks\":[{\"icon\":\"minecraft:anvil\",\"key\":\"k.forge\"}]}"),
                id -> OptionalInt.of(64), x -> x.startsWith("test:"), w);
        assertTrue(w.isEmpty(), w::toString);
        PromotionTable.AgeEntry c = t.entry(Age.CLASSICAL).orElseThrow();
        assertEquals(25, c.favor());
        assertEquals(List.of(new TradeOffer.ItemAmount("minecraft:cobblestone", 2)), c.offerings());
        assertEquals(Age.NONE, c.trial());
        assertEquals("test:switch", c.requirements().getFirst().type());
        assertEquals(Age.CLASSICAL, t.entry(Age.HEROIC).orElseThrow().trial());
        assertEquals(List.of(new PromotionTable.ExtraUnlock("minecraft:anvil", "k.forge")), t.entry(Age.HEROIC).orElseThrow().extraUnlocks());
        assertEquals(Age.CLASSICAL, t.trialTierOf("mythic_age:test_boss").orElseThrow());
        assertTrue(t.trialTierOf("mythic_age:other").isEmpty());
        assertTrue(t.entry(Age.MYTHIC).isEmpty());
    }

    @Test
    void tc24_civilizationMismatchOrBadVersion() {
        assertThrows(PromotionTable.PromotionTableException.class, () -> PromotionTable.parse(
                "{\"schemaVersion\":1,\"civilization\":\"elves\"}", id -> OptionalInt.of(64), x -> true, new ArrayList<>()));
        assertThrows(PromotionTable.PromotionTableException.class, () -> PromotionTable.parse(
                "{\"schemaVersion\":2,\"civilization\":\"greek\"}", id -> OptionalInt.of(64), x -> true, new ArrayList<>()));
    }

    @Test
    void tc25_badEntriesSkipped() {
        List<String> w = new ArrayList<>();
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            many.append("{\"item\":\"minecraft:item").append(i).append("\",\"count\":1},");
        }
        String json = """
                {"schemaVersion":1,"civilization":"greek","extra":1,"ages":{
                 "archaic":{"favor":1},
                 "classical":{"favor":10,"bogus":true,"offerings":[%s{"item":"minecraft:missing","count":1}]}}}"""
                .formatted(many);
        PromotionTable t = PromotionTable.parse(json, id -> id.equals("minecraft:missing") ? OptionalInt.empty() : OptionalInt.of(64),
                x -> true, w);
        assertEquals(6, t.entry(Age.CLASSICAL).orElseThrow().offerings().size());
        assertFalse(t.entry(Age.CLASSICAL).orElseThrow().broken());
        assertTrue(t.entry(Age.ARCHAIC).isEmpty());
        assertEquals(5, w.size(), w::toString);
    }

    @Test
    void tc26_badTrialOrRequirementBreaksAge() {
        List<String> w = new ArrayList<>();
        PromotionTable t = PromotionTable.parse("""
                {"schemaVersion":1,"civilization":"greek","ages":{
                 "classical":{"favor":10,"trial":"mythic"},
                 "heroic":{"favor":10,"requirements":[{"type":"greek:unknown"}]}}}""",
                id -> OptionalInt.of(64), x -> false, w);
        assertTrue(t.entry(Age.CLASSICAL).orElseThrow().broken());
        assertTrue(t.entry(Age.HEROIC).orElseThrow().broken());
        PlayerMythData d = new PlayerMythData();
        d.civ(C).setAge(Age.ARCHAIC);
        RiteRules.Plan p = RiteRules.plan(d.civ(C), C, RiteRulesTest.village("a", Age.ARCHAIC), RiteRulesTest.P, t,
                RiteKind.PROMOTION, Age.NONE, "x", 0);
        assertEquals(Set.of("data_error"), RiteRulesTest.keys(p.issues()));
    }

    @Test
    void tc27_stageTable() {
        List<String> w = new ArrayList<>();
        VillageStageTable s = VillageStageTable.parse("""
                {"schemaVersion":1,"civilization":"greek","stages":{
                 "classical":{"population":4,"guards":1,"migrationDays":2,"preview":["k.market"]},
                 "heroic":{"population":-3,"guards":2},
                 "bogus":{}}}""", w);
        assertEquals(new VillageStageTable.Stage(4, 1, 2, List.of("k.market")), s.stage(Age.CLASSICAL));
        assertEquals(0, s.stage(Age.HEROIC).population());
        assertEquals(VillageStageTable.Stage.NONE, s.stage(Age.MYTHIC));
        assertEquals(2, w.size(), w::toString);
    }

    // ───────────── 兑换地点与交易规则（TC-T11a-28～34） ─────────────

    static final EnumSet<TradeOffer.Venue> BOTH = EnumSet.of(TradeOffer.Venue.GUARDIAN, TradeOffer.Venue.STATUE);

    static TradeOffer offer(String id, TradeOffer.Category cat, Age age, String patron, String god, double godDiscount) {
        return new TradeOffer(id, cat, new TradeOffer.ItemAmount("mythic_age:" + id, 1), TradeOffer.Price.ofFavor(20), age,
                cat == TradeOffer.Category.SUPPLIES ? EnumSet.of(TradeOffer.Venue.GUARDIAN) : BOTH, 0, god,
                god.isEmpty() || godDiscount == 0 ? Map.of() : Map.of(god, godDiscount), patron, patron.isEmpty() ? 0 : 0.1);
    }

    static PlayerMythData player(Age age, String god) {
        PlayerMythData d = new PlayerMythData();
        d.civ(C).setAge(age);
        d.civ(C).setFavor(999);
        if (!god.isEmpty()) {
            d.civ(C).minorGods().put(Age.CLASSICAL, god);
        }
        return d;
    }

    static TradeReason check(PlayerMythData d, TradeOffer o, TradeRules.VenueContext ctx) {
        return TradeRules.check(d, C, o, ctx, 20, 20, 0);
    }

    static TradeRules.VenueContext g(String patron, Age stage, String... shrines) {
        return TradeRules.VenueContext.guardian(stage, patron, Set.of(shrines));
    }

    @Test
    void tc28_patronPowerOnlyInItsVillage() {
        TradeOffer bolt = offer("bolt", TradeOffer.Category.GOD_POWER, Age.ARCHAIC, "a", "", 0);
        PlayerMythData d = player(Age.ARCHAIC, "");
        assertTrue(TradeRules.visible(bolt, g("a", Age.ARCHAIC)));
        assertEquals(TradeReason.OK, check(d, bolt, g("a", Age.ARCHAIC)));
        assertEquals(List.of(0.1), TradeRules.discounts(d, C, bolt, g("a", Age.ARCHAIC)));
        assertFalse(TradeRules.visible(bolt, g("b", Age.ARCHAIC)));
        assertEquals(TradeReason.WRONG_PATRON, check(d, bolt, g("b", Age.ARCHAIC)));
    }

    @Test
    void tc29_minorPowerNeedsShrine() {
        TradeOffer heal = offer("heal", TradeOffer.Category.GOD_POWER, Age.CLASSICAL, "", "x", 0.1);
        PlayerMythData d = player(Age.CLASSICAL, "x");
        assertEquals(TradeReason.OK, check(d, heal, g("a", Age.ARCHAIC, "x")));
        assertEquals(List.of(0.1), TradeRules.discounts(d, C, heal, g("a", Age.ARCHAIC, "x")));
        assertFalse(TradeRules.visible(heal, g("a", Age.CLASSICAL)));
        assertEquals(TradeReason.NO_SHRINE, check(d, heal, g("a", Age.CLASSICAL)));
    }

    @Test
    void tc30_minorPowerNeedsWorship() {
        TradeOffer heal = offer("heal", TradeOffer.Category.GOD_POWER, Age.CLASSICAL, "", "x", 0.1);
        assertTrue(TradeRules.visible(heal, g("a", Age.CLASSICAL, "x")), "立了祠的村庄对不供奉者也列出（显示锁定）");
        assertEquals(TradeReason.MINOR_GOD_LOCKED, check(player(Age.CLASSICAL, "y"), heal, g("a", Age.CLASSICAL, "x")));
        assertEquals(TradeReason.AGE_LOCKED, check(player(Age.ARCHAIC, ""), heal, g("a", Age.CLASSICAL, "x")));
    }

    @Test
    void tc31_statue() {
        TradeRules.VenueContext statueA = TradeRules.VenueContext.statue("a", Set.of("x", "y"));
        TradeOffer boltA = offer("bolt", TradeOffer.Category.GOD_POWER, Age.ARCHAIC, "a", "", 0);
        TradeOffer lureB = offer("lure", TradeOffer.Category.GOD_POWER, Age.ARCHAIC, "b", "", 0);
        TradeOffer healX = offer("heal", TradeOffer.Category.GOD_POWER, Age.CLASSICAL, "", "x", 0.1);
        TradeOffer plagueZ = offer("plague", TradeOffer.Category.GOD_POWER, Age.CLASSICAL, "", "z", 0.1);
        PlayerMythData d = player(Age.CLASSICAL, "x");
        assertEquals(TradeReason.OK, check(d, boltA, statueA));
        assertFalse(TradeRules.visible(lureB, statueA));
        assertEquals(TradeReason.OK, check(d, healX, statueA));
        assertEquals(List.of(), TradeRules.discounts(d, C, healX, statueA), "主神像处不打次神折扣");
        assertFalse(TradeRules.visible(plagueZ, statueA));
        assertEquals(TradeReason.WRONG_PATRON, check(player(Age.CLASSICAL, "z"), plagueZ, statueA));
    }

    @Test
    void tc32_suppliesFollowVillageStage() {
        TradeOffer market = offer("market", TradeOffer.Category.SUPPLIES, Age.CLASSICAL, "", "", 0);
        assertEquals(TradeReason.OK, check(player(Age.ARCHAIC, ""), market, g("a", Age.CLASSICAL)));
        assertEquals(TradeReason.STAGE_LOCKED, check(player(Age.CLASSICAL, ""), market, g("a", Age.ARCHAIC)));
        assertFalse(TradeRules.visible(market, TradeRules.VenueContext.statue("a", Set.of())));
    }

    @Test
    void tc33_shrineGoods() {
        TradeOffer shield = offer("shield", TradeOffer.Category.SUPPLIES, Age.CLASSICAL, "", "x", 0.1);
        assertEquals(TradeReason.NO_SHRINE, check(player(Age.CLASSICAL, "x"), shield, g("a", Age.CLASSICAL)));
        PlayerMythData other = player(Age.ARCHAIC, "");
        assertEquals(TradeReason.OK, check(other, shield, g("a", Age.CLASSICAL, "x")));
        assertEquals(List.of(), TradeRules.discounts(other, C, shield, g("a", Age.CLASSICAL, "x")));
        assertEquals(List.of(0.1), TradeRules.discounts(player(Age.CLASSICAL, "x"), C, shield, g("a", Age.CLASSICAL, "x")));
    }

    @Test
    void tc34_libationAndAltarAlias() {
        TradeOffer libation = offer("libation", TradeOffer.Category.SUPPLIES, Age.ARCHAIC, "", "", 0);
        assertEquals(TradeReason.OK, check(player(Age.ARCHAIC, ""), libation, g("a", Age.ARCHAIC)));
        assertEquals(TradeOffer.Venue.STATUE, TradeOffer.Venue.byId("altar").orElseThrow());
    }

    // ───────────── 成功界面清单与上限（TC-T11a-35～38） ─────────────

    @Test
    void tc35_summaryPicksChosenGod() {
        List<TradeOffer> offers = List.of(
                offer("heal", TradeOffer.Category.GOD_POWER, Age.CLASSICAL, "", "x", 0),
                offer("truce", TradeOffer.Category.GOD_POWER, Age.CLASSICAL, "", "y", 0),
                offer("cyclops", TradeOffer.Category.SUMMON, Age.CLASSICAL, "", "", 0),
                offer("bronze", TradeOffer.Category.GOD_POWER, Age.HEROIC, "", "", 0));
        UnlockSummary s = UnlockSummary.of(C, Age.CLASSICAL, "x", offers,
                List.of(new UnlockSummary.GatedRecipe("mythic_age:bronze_mix", C, Age.CLASSICAL),
                        new UnlockSummary.GatedRecipe("mythic_age:bronze_mix", C, Age.CLASSICAL),
                        new UnlockSummary.GatedRecipe("mythic_age:other", Civilization.NORSE, Age.CLASSICAL)),
                List.of(new UnlockSummary.CapChange("favor_cap", 100, 150), new UnlockSummary.CapChange("relic_slots", 1, 1)),
                List.of());
        assertEquals(List.of("mythic_age:heal"), s.powers());
        assertEquals(List.of("mythic_age:cyclops"), s.summons());
        assertEquals(List.of("mythic_age:bronze_mix"), s.recipes());
        assertEquals(List.of(new UnlockSummary.CapChange("favor_cap", 100, 150)), s.caps());
    }

    @Test
    void tc36_capsArchaicToClassical() {
        assertEquals(100, Age.ARCHAIC.baseCap());
        assertEquals(150, Age.CLASSICAL.baseCap());
        assertEquals(2, AgeCaps.mythicUnits(Age.ARCHAIC));
        assertEquals(3, AgeCaps.mythicUnits(Age.CLASSICAL));
        assertEquals(5, AgeCaps.mythicUnits(Age.MYTHIC));
        assertEquals(2, AgeCaps.mythicUnits(Age.NONE));
        assertEquals(1, AgeCaps.relicSlots(Age.CLASSICAL));
    }

    @Test
    void tc37_relicSlotsUseHighestAcrossCivs() {
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.NORSE).setAge(Age.HEROIC);
        d.civ(C).setAge(Age.CLASSICAL);
        Age before = AgeCaps.highest(d);
        d.civ(C).setAge(Age.HEROIC);
        assertEquals(AgeCaps.relicSlots(before), AgeCaps.relicSlots(AgeCaps.highest(d)));
        assertEquals(2, AgeCaps.relicSlots(before));
    }

    @Test
    void tc38_mergeAcrossAges() {
        UnlockSummary a = new UnlockSummary(C, Age.CLASSICAL, "x", List.of("p1"), List.of(), List.of("r1"),
                List.of(new UnlockSummary.CapChange("favor_cap", 100, 150)), List.of());
        UnlockSummary b = new UnlockSummary(C, Age.HEROIC, "h", List.of("p2"), List.of("s2"), List.of("r1", "r2"),
                List.of(new UnlockSummary.CapChange("favor_cap", 150, 200)), List.of());
        UnlockSummary m = UnlockSummary.merge(List.of(a, b));
        assertEquals(Age.HEROIC, m.age());
        assertEquals(List.of("p1", "p2"), m.powers());
        assertEquals(List.of("r1", "r2"), m.recipes());
        assertEquals(List.of(new UnlockSummary.CapChange("favor_cap", 100, 200)), m.caps());
    }

    // ───────────── 阶段效果（TC-T11a-39b） ─────────────

    @Test
    void tc39b_populationTargetAndGuards() {
        VillageStageTable s = VillageStageTable.parse("""
                {"schemaVersion":1,"civilization":"greek","stages":{
                 "classical":{"population":4,"guards":1},"heroic":{"population":6,"guards":2},
                 "mythic":{"population":8,"guards":3}}}""", new ArrayList<>());
        assertEquals(6, s.populationTarget(6, Age.ARCHAIC));
        assertEquals(10, s.populationTarget(6, Age.CLASSICAL));
        assertEquals(16, s.populationTarget(6, Age.HEROIC));
        assertEquals(24, s.populationTarget(6, Age.MYTHIC));
        assertEquals(0 + 1, s.guardCap(3, Age.CLASSICAL));
        assertEquals(1 + 1, s.guardCap(4, Age.CLASSICAL));
        assertEquals(2 + 2, s.guardCap(8, Age.HEROIC));
        assertEquals(6 + 3, s.guardCap(40, Age.MYTHIC));
    }
}
