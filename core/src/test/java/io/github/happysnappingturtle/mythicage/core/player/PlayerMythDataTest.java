package io.github.happysnappingturtle.mythicage.core.player;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMythDataTest {

    @Test
    void tc_t4_30_newPlayer() {
        PlayerMythData d = new PlayerMythData();
        for (Civilization c : Civilization.values()) {
            assertEquals(Age.NONE, d.civ(c).age());
            assertEquals(0, d.civ(c).favor());
        }
        assertTrue(d.firstKills().isEmpty());
        assertTrue(d.blueprints().isEmpty());
        assertTrue(d.forgeRecords().isEmpty());
        assertTrue(d.recentConduct().isEmpty());
    }

    @Test
    void tc_t4_31_setAgeAffectsOnlyThatCivilization() {
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.GREEK).setAge(Age.CLASSICAL);
        assertEquals(Age.CLASSICAL, d.civ(Civilization.GREEK).age());
        assertEquals(Age.NONE, d.civ(Civilization.EGYPTIAN).age());
    }

    @Test
    void tc_t4_32_downgradeDoesNotTruncateFavor() {
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.GREEK).setAge(Age.MYTHIC);
        d.civ(Civilization.GREEK).setFavor(250);
        d.civ(Civilization.GREEK).setAge(Age.ARCHAIC);
        assertEquals(250, d.civ(Civilization.GREEK).favor());
    }

    @Test
    void tc_t4_33_recentConductKeepsLast20() {
        PlayerMythData d = new PlayerMythData();
        for (int i = 0; i < 25; i++) {
            d.addConductEntry(new PlayerMythData.ConductEntry("e" + i, -1, i));
        }
        List<PlayerMythData.ConductEntry> recent = d.recentConduct();
        assertEquals(PlayerMythData.RECENT_CONDUCT_LIMIT, recent.size());
        assertEquals("e5", recent.get(0).reason());
        assertEquals("e24", recent.get(19).reason());
    }

    @Test
    void tc_t4_34_reforgeIncrementsGeneration() {
        PlayerMythData d = new PlayerMythData();
        assertEquals(1, d.recordForge("harpe").generation());
        assertEquals(2, d.recordForge("harpe").generation());
        assertEquals(2, d.forgeRecords().get("harpe").generation());
    }

    @Test
    void tc_t4_35_copyIsDeep() {
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.GREEK).setFavor(40);
        d.blueprints().add("harpe");
        d.receipts().record(ReceiptBook.Scope.UNIQUE, "first-kill:minotaur");
        d.addConductEntry(new PlayerMythData.ConductEntry("x", 1, 0));
        PlayerMythData copy = d.copy();
        assertEquals(d, copy);
        copy.civ(Civilization.GREEK).setFavor(99);
        copy.blueprints().add("aegis");
        copy.receipts().record(ReceiptBook.Scope.UNIQUE, "other");
        assertEquals(40, d.civ(Civilization.GREEK).favor());
        assertEquals(List.of("harpe"), List.copyOf(d.blueprints()));
        assertEquals(1, d.receipts().size(ReceiptBook.Scope.UNIQUE));
        assertNotEquals(d, copy);
    }

    @Test
    void negativeFavorIsStoredAsZero() {
        PlayerMythData d = new PlayerMythData();
        d.civ(Civilization.NORSE).setFavor(-5);
        assertEquals(0, d.civ(Civilization.NORSE).favor());
    }

    @Test
    void idsRoundTrip() {
        for (Civilization c : Civilization.values()) {
            assertEquals(c, Civilization.byId(c.id()).orElseThrow());
        }
        for (Age a : Age.values()) {
            assertEquals(a, Age.byId(a.id()).orElseThrow());
        }
        assertTrue(Civilization.byId("atlantis2").isEmpty());
    }
}
