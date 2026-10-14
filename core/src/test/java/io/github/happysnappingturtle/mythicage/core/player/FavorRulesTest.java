package io.github.happysnappingturtle.mythicage.core.player;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FavorRulesTest {

    @Test
    void tc_t4_01_baseCapsPerAge() {
        assertEquals(0, FavorRules.cap(Age.NONE, 0));
        assertEquals(100, FavorRules.cap(Age.ARCHAIC, 0));
        assertEquals(150, FavorRules.cap(Age.CLASSICAL, 0));
        assertEquals(200, FavorRules.cap(Age.HEROIC, 0));
        assertEquals(300, FavorRules.cap(Age.MYTHIC, 0));
    }

    @Test
    void tc_t4_02_capBonus() {
        assertEquals(120, FavorRules.cap(Age.ARCHAIC, 20));
        assertEquals(100, FavorRules.cap(Age.ARCHAIC, -5));
    }

    @Test
    void tc_t4_03_addClampsToCap() {
        assertEquals(100, FavorRules.add(90, 20, 100));
    }

    @Test
    void tc_t4_04_addAtCapIsUnchanged() {
        assertEquals(100, FavorRules.add(100, 5, 100));
    }

    @Test
    void tc_t4_05_aboveCapIsKeptButDoesNotGrow() {
        assertEquals(130, FavorRules.add(130, 5, 100));
    }

    @Test
    void tc_t4_06_growthResumesBelowCap() {
        long afterSpend = FavorRules.spend(130, 40).orElseThrow();
        assertEquals(90, afterSpend);
        assertEquals(100, FavorRules.add(afterSpend, 30, 100));
    }

    @Test
    void tc_t4_07_noOverflow() {
        assertEquals(Long.MAX_VALUE, FavorRules.add(Long.MAX_VALUE - 1, 10, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, FavorRules.cap(Age.MYTHIC, Long.MAX_VALUE));
    }

    @Test
    void tc_t4_08_spend() {
        assertEquals(OptionalLong.of(0), FavorRules.spend(50, 50));
        assertEquals(OptionalLong.empty(), FavorRules.spend(49, 50));
    }

    @Test
    void tc_t4_09_nonPositiveAmountsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FavorRules.add(0, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> FavorRules.add(0, -1, 100));
        assertThrows(IllegalArgumentException.class, () -> FavorRules.spend(10, 0));
        assertThrows(IllegalArgumentException.class, () -> FavorRules.spend(10, -3));
    }

    @Test
    void tc_t4_10_noFavorBeforeFirstContact() {
        assertEquals(0, FavorRules.add(0, 10, FavorRules.cap(Age.NONE, 0)));
    }
}
