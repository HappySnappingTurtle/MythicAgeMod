package io.github.happysnappingturtle.mythicage.core.rite;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.village.Pantheon;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T11a 仪式规则（TC-T11a-01～22）。主神 a、b；古典次神 x(a,b)、y(a)、z(b，未开放)；英雄次神 h(a,b)。 */
class RiteRulesTest {
    static final Civilization C = Civilization.GREEK;
    static final String PANTHEON = """
            {"schemaVersion":1,"civilization":"greek","patrons":["a","b"],"minorGods":[
             {"id":"x","age":"classical","patrons":["a","b"],"available":true},
             {"id":"y","age":"classical","patrons":["a"],"available":true},
             {"id":"z","age":"classical","patrons":["b"],"available":false},
             {"id":"h","age":"heroic","patrons":["a","b"],"available":true}]}""";
    static final String TABLE = """
            {"schemaVersion":1,"civilization":"greek","ages":{
             "classical":{"favor":25,"offerings":[{"item":"minecraft:cobblestone","count":2}],"requirements":[{"type":"test:switch"}]},
             "heroic":{"favor":75,"offerings":[{"item":"minecraft:gold_ingot","count":1}],"trial":"classical"}},
             "trials":{"classical":["mythic_age:test_boss"]}}""";
    static final Pantheon P = Pantheon.parse(PANTHEON);
    static final PromotionTable T = PromotionTable.parse(TABLE, id -> OptionalInt.of(64), t -> t.startsWith("test:"),
            new ArrayList<>());
    static final UUID V1 = UUID.randomUUID();

    static PlayerMythData.CivState player(Age age, long favor) {
        PlayerMythData d = new PlayerMythData();
        d.civ(C).setAge(age);
        d.civ(C).setFavor(favor);
        return d.civ(C);
    }

    static RiteRules.Village village(String patron, Age stage) {
        return new RiteRules.Village(V1, "greek", patron, stage, new EnumMap<>(Age.class));
    }

    static RiteRules.Plan plan(PlayerMythData.CivState s, RiteRules.Village v, RiteKind kind, Age age, String god) {
        return RiteRules.plan(s, C, v, P, T, kind, age, god, 10);
    }

    static Set<String> keys(List<RiteRules.Issue> issues) {
        return Set.copyOf(issues.stream().map(RiteRules.Issue::key).toList());
    }

    @Test
    void tc01_promotionToClassical() {
        PlayerMythData.CivState s = player(Age.ARCHAIC, 30);
        RiteRules.Village v = village("a", Age.ARCHAIC);
        RiteRules.Plan p = plan(s, v, RiteKind.PROMOTION, Age.NONE, "x");
        assertTrue(p.issues().isEmpty(), p.issues()::toString);
        assertEquals(Age.CLASSICAL, p.target());
        assertEquals(new RiteRules.Cost(25, List.of(new TradeOffer.ItemAmount("minecraft:cobblestone", 2)), Age.NONE, 0), p.cost());
        assertEquals(Set.of(RequirementScope.PLAYER, RequirementScope.VILLAGE), p.scopes());
        RiteRules.applyToPlayer(s, p, p.cost(), v, 10);
        assertEquals(Age.CLASSICAL, s.age());
        assertEquals(5, s.favor());
        assertEquals("x", s.minorGods().get(Age.CLASSICAL));
        assertEquals(Age.CLASSICAL, s.approval("a"));
        assertEquals(new PlayerMythData.RiteMark(Age.CLASSICAL, true), s.riteVillages().get(V1));
        assertEquals(List.of(Age.CLASSICAL), s.pendingUnlocks());
        assertEquals(new RiteRules.VillageChange(Age.CLASSICAL, Age.CLASSICAL, "x"), RiteRules.villageChange(p, v));
    }

    @Test
    void tc02_candidatesFollowVillagePatron() {
        assertEquals(List.of(new RiteRules.Candidate("x", true), new RiteRules.Candidate("y", true)),
                RiteRules.candidates(P, "a", Age.CLASSICAL));
        assertEquals(List.of(new RiteRules.Candidate("x", true), new RiteRules.Candidate("z", false)),
                RiteRules.candidates(P, "b", Age.CLASSICAL));
    }

    @Test
    void tc03_unavailableOrForeignGod() {
        assertEquals(Set.of("god_unavailable"), keys(plan(player(Age.ARCHAIC, 99), village("b", Age.ARCHAIC),
                RiteKind.PROMOTION, Age.NONE, "z").issues()));
        assertEquals(Set.of("not_candidate"), keys(plan(player(Age.ARCHAIC, 99), village("b", Age.ARCHAIC),
                RiteKind.PROMOTION, Age.NONE, "y").issues()));
        assertEquals(Set.of("choose_god"), keys(plan(player(Age.ARCHAIC, 99), village("a", Age.ARCHAIC),
                RiteKind.PROMOTION, Age.NONE, "").issues()));
    }

    @Test
    void tc04_allResourceIssuesListed() {
        RiteRules.Plan p = plan(player(Age.ARCHAIC, 24), village("a", Age.ARCHAIC), RiteKind.PROMOTION, Age.NONE, "x");
        List<RiteRules.Issue> res = RiteRules.resources(p.cost(), 24, Map.of("minecraft:cobblestone", 1), 0);
        assertEquals(List.of(RiteRules.Issue.of("favor", 24L, 25L), RiteRules.Issue.of("offering", "minecraft:cobblestone", 1, 2)), res);
    }

    @Test
    void tc05_onlyOneAgeAtATime() {
        // 远古玩家只能升古典：目标由玩家时代决定，没有「直接到英雄」的入口
        assertEquals(Age.CLASSICAL, plan(player(Age.ARCHAIC, 999), village("a", Age.HEROIC), RiteKind.PROMOTION, Age.HEROIC, "x").target());
        assertEquals(Set.of("max_age"), keys(plan(player(Age.MYTHIC, 999), village("a", Age.ARCHAIC), RiteKind.PROMOTION, Age.NONE, "x").issues()));
        assertEquals(Set.of("not_blessed"), keys(plan(player(Age.NONE, 999), village("a", Age.ARCHAIC), RiteKind.PROMOTION, Age.NONE, "x").issues()));
    }

    @Test
    void tc06_heroicNeedsVoucher() {
        PlayerMythData.CivState s = player(Age.CLASSICAL, 99);
        RiteRules.Plan p = plan(s, village("a", Age.CLASSICAL), RiteKind.PROMOTION, Age.NONE, "h");
        assertEquals(new RiteRules.Cost(75, List.of(new TradeOffer.ItemAmount("minecraft:gold_ingot", 1)), Age.CLASSICAL, 1), p.cost());
        assertEquals(Set.of("voucher"), keys(RiteRules.resources(p.cost(), 99, Map.of("minecraft:gold_ingot", 1), 0)));
        assertTrue(RiteRules.resources(p.cost(), 99, Map.of("minecraft:gold_ingot", 1), 1).isEmpty());
        s.setVouchers(Age.CLASSICAL, 1);
        RiteRules.applyToPlayer(s, p, p.cost(), village("a", Age.CLASSICAL), 10);
        assertEquals(0, s.vouchers(Age.CLASSICAL));
        assertTrue(s.trialVouchers().isEmpty());
    }

    @Test
    void tc07_promotionInHigherVillageKeepsStage() {
        RiteRules.Plan p = plan(player(Age.ARCHAIC, 99), village("a", Age.HEROIC), RiteKind.PROMOTION, Age.NONE, "y");
        assertEquals(new RiteRules.VillageChange(Age.HEROIC, Age.CLASSICAL, "y"), RiteRules.villageChange(p, village("a", Age.HEROIC)));
    }

    @Test
    void tc08_consecrationNeedsClassical() {
        assertEquals(Set.of("need_classical"), keys(plan(player(Age.ARCHAIC, 99), village("a", Age.ARCHAIC),
                RiteKind.CONSECRATION, Age.NONE, "x").issues()));
    }

    @Test
    void tc09_consecrationApprovedHalfPrice() {
        PlayerMythData.CivState s = player(Age.CLASSICAL, 99);
        s.minorGods().put(Age.CLASSICAL, "x");
        s.approve("a", Age.CLASSICAL);
        RiteRules.Village v = village("a", Age.ARCHAIC);
        RiteRules.Plan p = plan(s, v, RiteKind.CONSECRATION, Age.NONE, "y");
        assertTrue(p.issues().isEmpty(), p.issues()::toString);
        assertTrue(p.approved());
        assertEquals(13, p.cost().favor());
        assertEquals(0, p.cost().vouchers());
        assertEquals(Set.of(RequirementScope.VILLAGE), p.scopes());
        RiteRules.applyToPlayer(s, p, p.cost(), v, 10);
        assertEquals("x", s.minorGods().get(Age.CLASSICAL), "立祀不改变玩家自己的次神");
        assertEquals(new RiteRules.VillageChange(Age.CLASSICAL, Age.CLASSICAL, "y"), RiteRules.villageChange(p, v));
        assertEquals(new PlayerMythData.RiteMark(Age.CLASSICAL, false), s.riteVillages().get(V1));
        assertTrue(s.pendingUnlocks().isEmpty());
    }

    @Test
    void tc10_consecrationOtherPatronFullPrice() {
        PlayerMythData.CivState s = player(Age.CLASSICAL, 99);
        s.approve("a", Age.CLASSICAL);
        RiteRules.Plan p = plan(s, village("b", Age.ARCHAIC), RiteKind.CONSECRATION, Age.NONE, "x");
        assertFalse(p.approved());
        assertEquals(25, p.cost().favor());
        RiteRules.applyToPlayer(s, p, p.cost(), village("b", Age.ARCHAIC), 10);
        assertEquals(Age.CLASSICAL, s.approval("b"));
    }

    @Test
    void tc11_consecrationHeroicOtherPatronNeedsVoucher() {
        PlayerMythData.CivState s = player(Age.HEROIC, 999);
        s.approve("a", Age.HEROIC);
        RiteRules.Plan other = plan(s, village("b", Age.CLASSICAL), RiteKind.CONSECRATION, Age.NONE, "h");
        assertEquals(1, other.cost().vouchers());
        assertEquals(Age.CLASSICAL, other.cost().voucherTier());
        RiteRules.Plan same = plan(s, village("a", Age.CLASSICAL), RiteKind.CONSECRATION, Age.NONE, "h");
        assertEquals(0, same.cost().vouchers());
        assertEquals(38, same.cost().favor());
    }

    @Test
    void tc12_consecrationCannotExceedPlayerAge() {
        assertEquals(Set.of("village_caught_up"), keys(plan(player(Age.CLASSICAL, 99), village("a", Age.CLASSICAL),
                RiteKind.CONSECRATION, Age.NONE, "h").issues()));
    }

    @Test
    void tc13_secondPlayerSeesNewTarget() {
        // 别人已把村庄立祀到古典：目标变为英雄，超过这名古典玩家的时代
        RiteRules.Plan p = plan(player(Age.CLASSICAL, 99), village("a", Age.CLASSICAL), RiteKind.CONSECRATION, Age.NONE, "x");
        assertEquals(Age.HEROIC, p.target());
        assertTrue(keys(p.issues()).contains("village_caught_up"));
    }

    @Test
    void tc14_rededicateSameGod() {
        PlayerMythData.CivState s = player(Age.CLASSICAL, 99);
        s.minorGods().put(Age.CLASSICAL, "x");
        assertEquals(Set.of("same_god"), keys(plan(s, village("a", Age.CLASSICAL), RiteKind.REDEDICATION, Age.CLASSICAL, "x").issues()));
    }

    @Test
    void tc15_rededicate() {
        PlayerMythData.CivState s = player(Age.CLASSICAL, 99);
        s.minorGods().put(Age.CLASSICAL, "x");
        RiteRules.Village v = village("a", Age.ARCHAIC);
        RiteRules.Plan p = plan(s, v, RiteKind.REDEDICATION, Age.CLASSICAL, "y");
        assertTrue(p.issues().isEmpty(), p.issues()::toString);
        assertEquals(new RiteRules.Cost(13, List.of(), Age.NONE, 0), p.cost());
        assertTrue(p.scopes().isEmpty());
        RiteRules.applyToPlayer(s, p, p.cost(), v, 10);
        assertEquals("y", s.minorGods().get(Age.CLASSICAL));
        assertEquals(10, s.rededicatedDay());
        assertEquals(new RiteRules.VillageChange(Age.ARCHAIC, Age.CLASSICAL, "y"), RiteRules.villageChange(p, v),
                "改奉不推进村庄阶段");
    }

    @Test
    void tc16_rededicationCooldown() {
        PlayerMythData.CivState s = player(Age.CLASSICAL, 99);
        s.minorGods().put(Age.CLASSICAL, "x");
        RiteRules.Village v = village("a", Age.CLASSICAL);
        for (int[] c : new int[][]{{10, 0}, {12, 0}, {13, 1}}) {
            s.setRededicatedDay(10);
            RiteRules.Plan p = RiteRules.plan(s, C, v, P, T, RiteKind.REDEDICATION, Age.CLASSICAL, "y", c[0]);
            assertEquals(c[1] == 1, p.issues().isEmpty(), "第 " + (c[0] - 10) + " 天：" + p.issues());
        }
    }

    @Test
    void tc17_rededicateUnreachedAge() {
        assertTrue(keys(plan(player(Age.CLASSICAL, 99), village("a", Age.CLASSICAL), RiteKind.REDEDICATION, Age.HEROIC, "h")
                .issues()).contains("age_not_reached"));
    }

    @Test
    void tc18_wrongVillage() {
        PlayerMythData.CivState s = player(Age.ARCHAIC, 99);
        assertEquals(Set.of("wrong_village"), keys(RiteRules.plan(s, C, new RiteRules.Village(V1, "", "", Age.NONE, Map.of()),
                P, T, RiteKind.PROMOTION, Age.NONE, "x", 0).issues()));
        assertEquals(Set.of("wrong_village"), keys(RiteRules.plan(s, C, new RiteRules.Village(V1, "greek", "", Age.ARCHAIC, Map.of()),
                P, T, RiteKind.PROMOTION, Age.NONE, "x", 0).issues()));
        assertEquals(Set.of("wrong_village"), keys(RiteRules.plan(s, C, new RiteRules.Village(V1, "egyptian", "a", Age.ARCHAIC, Map.of()),
                P, T, RiteKind.PROMOTION, Age.NONE, "x", 0).issues()));
    }

    @Test
    void tc19_halfRoundsUp() {
        assertEquals(13, RiteRules.half(25));
        assertEquals(38, RiteRules.half(75));
        assertEquals(0, RiteRules.half(0));
    }

    @Test
    void tc21_missingAgeNotOpen() {
        PlayerMythData.CivState s = player(Age.HEROIC, 999);
        assertEquals(Set.of("not_open", "no_candidate"), keys(plan(s, village("a", Age.HEROIC), RiteKind.PROMOTION, Age.NONE, "").issues()));
    }

    @Test
    void tc22_patronCountNotFixed() {
        Pantheon four = Pantheon.parse("""
                {"schemaVersion":1,"civilization":"greek","patrons":["p1","p2","p3","p4"],"minorGods":[
                 {"id":"g","age":"classical","patrons":["p4"],"available":true}]}""");
        RiteRules.Plan p = RiteRules.plan(player(Age.ARCHAIC, 99), C, village("p4", Age.ARCHAIC), four, T,
                RiteKind.PROMOTION, Age.NONE, "g", 0);
        assertTrue(p.issues().isEmpty(), p.issues()::toString);
        assertEquals(List.of(), RiteRules.candidates(four, "p1", Age.CLASSICAL));
    }
}
