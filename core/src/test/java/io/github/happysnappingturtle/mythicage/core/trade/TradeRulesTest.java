package io.github.happysnappingturtle.mythicage.core.trade;

import io.github.happysnappingturtle.mythicage.core.guardian.GuardianRules;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.DailyLedger;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;
import io.github.happysnappingturtle.mythicage.core.player.tx.Transactions;
import io.github.happysnappingturtle.mythicage.core.player.tx.TxSteps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T9-06～19：交易判定、每日限购、背包预留、事务与守护者规则。 */
class TradeRulesTest {
    private static final TradeOffer.Venue G = TradeOffer.Venue.GUARDIAN;

    private static TradeOffer favorOffer(Age minAge, int limit, Set<TradeOffer.Venue> venues) {
        return new TradeOffer("bolt", TradeOffer.Category.GOD_POWER, new TradeOffer.ItemAmount("mythic_age:bolt", 1),
                TradeOffer.Price.ofFavor(20), minAge, venues, limit);
    }

    private static PlayerMythData player(Age age, long favor) {
        PlayerMythData d = new PlayerMythData();
        d.civ(GREEK).setAge(age);
        d.civ(GREEK).setFavor(favor);
        return d;
    }

    private static TradeReason check(PlayerMythData d, TradeOffer o, TradeOffer.Venue venue, long expected) {
        return TradeRules.check(d, GREEK, o, ctx(venue), 20, expected, 3);
    }

    private static TradeRules.VenueContext ctx(TradeOffer.Venue venue) {
        return venue == G ? TradeRules.VenueContext.guardian(Age.ARCHAIC, "", Set.of())
                : TradeRules.VenueContext.statue("", Set.of());
    }

    @Test
    void tc06AgeLocked() {
        TradeOffer classical = favorOffer(Age.CLASSICAL, 0, Set.of(G));
        assertEquals(TradeReason.AGE_LOCKED, check(player(Age.NONE, 99), classical, G, 20));
        assertEquals(TradeReason.AGE_LOCKED, check(player(Age.ARCHAIC, 99), classical, G, 20));
    }

    @Test
    void tc07ExactAgePasses() {
        assertEquals(TradeReason.OK, check(player(Age.CLASSICAL, 99), favorOffer(Age.CLASSICAL, 0, Set.of(G)), G, 20));
    }

    @Test
    void tc08PriceChanged() {
        assertEquals(TradeReason.PRICE_CHANGED, check(player(Age.ARCHAIC, 99), favorOffer(Age.ARCHAIC, 0, Set.of(G)), G, 18));
    }

    @Test
    void tc09FavorBoundary() {
        TradeOffer o = favorOffer(Age.ARCHAIC, 0, Set.of(G));
        assertEquals(TradeReason.OK, check(player(Age.ARCHAIC, 20), o, G, 20));
        assertEquals(TradeReason.NOT_ENOUGH_FAVOR, check(player(Age.ARCHAIC, 19), o, G, 20));
    }

    @Test
    void tc10WrongVenue() {
        assertEquals(TradeReason.WRONG_VENUE,
                check(player(Age.ARCHAIC, 99), favorOffer(Age.ARCHAIC, 0, Set.of(G)), TradeOffer.Venue.STATUE, 20));
    }

    @Test
    void tc11DailyLimitRollsOverNextDay() {
        PlayerMythData d = player(Age.ARCHAIC, 999);
        TradeOffer o = favorOffer(Age.ARCHAIC, 8, Set.of(G));
        for (int i = 0; i < 8; i++) {
            assertEquals(TradeReason.OK, check(d, o, G, 20), "第 " + (i + 1) + " 次");
            d.tradeDaily().record(o.key(GREEK), 3);
        }
        assertEquals(TradeReason.DAILY_LIMIT, check(d, o, G, 20));
        assertEquals(0, TradeRules.remainingToday(d, GREEK, o, 3));
        assertEquals(TradeReason.OK, TradeRules.check(d, GREEK, o, ctx(G), 20, 20, 4), "次日旧日期视为 0");
        assertEquals(8, TradeRules.remainingToday(d, GREEK, o, 4));
        assertEquals(-1, TradeRules.remainingToday(d, GREEK, favorOffer(Age.ARCHAIC, 0, Set.of(G)), 4));
    }

    @Test
    void tc12LedgerKeepsNewest64() {
        DailyLedger l = new DailyLedger();
        for (int i = 0; i < DailyLedger.CAPACITY + 5; i++) {
            l.record("k" + i, 1);
        }
        assertEquals(DailyLedger.CAPACITY, l.view().size());
        assertEquals(0, l.count("k0", 1), "最旧的被淘汰");
        assertEquals(1, l.count("k" + (DailyLedger.CAPACITY + 4), 1));
    }

    @Test
    void tc13InventoryReservation() {
        List<InventoryPlan.Slot> empty = java.util.Collections.nCopies(36, InventoryPlan.Slot.empty());
        assertTrue(new InventoryPlan(empty).insert("libation", 2, 16));

        java.util.ArrayList<InventoryPlan.Slot> full = new java.util.ArrayList<>(
                java.util.Collections.nCopies(36, new InventoryPlan.Slot("stone", 64, 64)));
        full.set(5, new InventoryPlan.Slot("libation", 15, 16));
        assertFalse(new InventoryPlan(full).insert("libation", 2, 16), "只差 1 个满堆，放不下 2 个");
        assertTrue(new InventoryPlan(full).insert("libation", 1, 16));

        full.set(9, new InventoryPlan.Slot("emerald", 1, 64));
        InventoryPlan plan = new InventoryPlan(full);
        assertTrue(plan.remove("emerald", 1), "先扣价");
        assertTrue(plan.insert("libation", 2, 16), "扣价腾出的格子可以放产物");
        assertFalse(new InventoryPlan(full).remove("emerald", 2), "物品不足");
    }

    @Test
    void tc14RequireAgeExactly() {
        assertTrue(TxSteps.requireAgeExactly(GREEK, Age.NONE).check(player(Age.NONE, 0)).isEmpty());
        assertTrue(TxSteps.requireAgeExactly(GREEK, Age.NONE).check(player(Age.ARCHAIC, 0)).isPresent());
    }

    private static Transactions.Request blessing() {
        return Transactions.Request.unique(GuardianRules.blessingReceipt("greek"),
                TxSteps.requireAgeExactly(GREEK, Age.NONE), TxSteps.setAge(GREEK, Age.ARCHAIC));
    }

    @Test
    void tc15BlessingOncePerPlayer() {
        PlayerMythData d = player(Age.NONE, 0);
        assertEquals(Transactions.Status.COMMITTED, Transactions.execute(d, blessing()).status());
        assertEquals(Age.ARCHAIC, d.civ(GREEK).age());
        assertEquals(0, d.civ(GREEK).favor(), "A1：不送启动恩宠");
        long v = d.dataVersion();
        assertEquals(Transactions.Status.DUPLICATE, Transactions.execute(d, blessing()).status());
        assertEquals(v, d.dataVersion());
        assertTrue(d.receipts().contains(ReceiptBook.Scope.UNIQUE, "greek:first_blessing"));
    }

    private static Transactions.Request buy(String requestId, int limit) {
        return Transactions.Request.of("trade:" + requestId, TxSteps.requireAge(GREEK, Age.ARCHAIC),
                TxSteps.spendFavor(GREEK, 20), TxSteps.recordDaily("greek:bolt", 3, limit));
    }

    @Test
    void tc16DuplicateRequestSettlesOnce() {
        PlayerMythData d = player(Age.ARCHAIC, 50);
        assertEquals(Transactions.Status.COMMITTED, Transactions.execute(d, buy("r1", 0)).status());
        assertEquals(Transactions.Status.DUPLICATE, Transactions.execute(d, buy("r1", 0)).status());
        assertEquals(30, d.civ(GREEK).favor());
        assertEquals(1, d.tradeDaily().count("greek:bolt", 3));
    }

    @Test
    void tc17AllChecksBeforeApply() {
        PlayerMythData d = player(Age.ARCHAIC, 50);
        d.tradeDaily().record("greek:bolt", 3);
        Transactions.Result r = Transactions.execute(d, buy("r2", 1));
        assertEquals(Transactions.Status.REJECTED, r.status());
        assertEquals(50, d.civ(GREEK).favor(), "限购拒绝时恩宠不变");
    }

    @Test
    void tc18Reissue() {
        assertTrue(GuardianRules.canReissue(0, false));
        assertFalse(GuardianRules.canReissue(1, false));
        assertFalse(GuardianRules.canReissue(0, true));
        assertEquals("greek:altar_reissue:7", GuardianRules.reissueReceipt("greek", 7));
    }

    @Test
    void tc19Respawn() {
        assertFalse(GuardianRules.shouldRespawn(5, 5, 1));
        assertTrue(GuardianRules.shouldRespawn(5, 6, 1));
        assertTrue(GuardianRules.shouldRespawn(5, 5, 0));
        assertEquals(1, GuardianRules.day(24000));
        assertEquals(0, GuardianRules.day(23999));
    }

    @Test
    void ledgerSurvivesCopyAndEquals() {
        PlayerMythData d = player(Age.ARCHAIC, 1);
        d.tradeDaily().record("greek:libation", 2);
        PlayerMythData c = d.copy();
        assertEquals(d, c);
        c.tradeDaily().record("greek:libation", 2);
        assertFalse(d.equals(c));
    }
}
