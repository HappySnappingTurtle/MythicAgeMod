package io.github.happysnappingturtle.mythicage.core.greek.prayer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T10 信徒祈祷的 core 用例（TC-T10-01、10～14）与 GA-4 按今日信徒入账（TC-GA4-01～10）。 */
class PrayerFormulaTest {
    @Test
    void tc01_dailyTable() {
        // 01_希腊 §3.3 算例表：n、T̄、R → 日产
        double[][] rows = {
                {2, 1.0, 0, 4.9}, {4, 1.5, 0, 10.5}, {7, 2.0, 0, 18.5}, {11, 1.82, 0, 21.1},
                {17, 2.59, 0.15, 42.9}, {20, 3.0, 0.15, 54.0}, {24, 3.0, 0.15, 59.1}};
        for (double[] r : rows) {
            assertEquals(r[3], PrayerFormula.daily((int) r[0], r[1], r[2]), 0.1, "n=" + r[0]);
        }
    }

    // ───── GA-4：按今日信徒入账（TC-GA4-01～10，取代原 TC-T10-02～09 的按刻分配） ─────

    private static double sumCredits(int from, int to, double tier) {
        double s = 0;
        for (int n = from; n <= to; n++) {
            s += PrayerFormula.credit(n, tier, 1, 0, 0, 1);
        }
        return s;
    }

    @Test
    void ga4_01_firstBeliever() {
        assertEquals(3.5 * 1.5, PrayerFormula.credit(1, 1.5, 1, 0, 0, 1), 1e-12);
    }

    @Test
    void ga4_02_creditsTelescopeToDaily() {
        assertEquals(3.5 * 1.5 * Math.sqrt(8), sumCredits(1, 8, 1.5), 1e-9);
        assertEquals(PrayerFormula.daily(8, 1.5, 0), sumCredits(1, 8, 1.5), 1e-9);
        assertEquals(14.85, sumCredits(1, 8, 1.5), 0.01);
    }

    @Test
    void ga4_03_capAt24() {
        assertTrue(PrayerFormula.credit(24, 1, 1, 0, 0, 1) > 0);
        assertEquals(0, PrayerFormula.credit(25, 1, 1, 0, 0, 1));
        assertEquals(0, PrayerFormula.credit(30, 1, 1, 0, 0, 1));
        assertEquals(3.5 * Math.sqrt(24), sumCredits(1, 30, 1), 1e-9);
    }

    @Test
    void ga4_04_rainHalves() {
        assertEquals(PrayerFormula.credit(3, 1, 1, 0, 0, 1) / 2,
                PrayerFormula.credit(3, 1, PrayerFormula.RAIN_FACTOR, 0, 0, 1), 1e-12);
    }

    @Test
    void ga4_05_offering() {
        assertEquals(PrayerFormula.credit(3, 1.5, 1, 0, 0, 1) * 1.5, PrayerFormula.credit(3, 1.5, 1, 0, 0.5, 1), 1e-12);
        assertEquals(PrayerFormula.credit(3, 1.5, 1, 0, 0, 1), PrayerFormula.credit(3, 1.5, 1, 0, -1, 1), 1e-12);
    }

    @Test
    void ga4_06_relicClamped() {
        double base = PrayerFormula.credit(4, 1, 1, 0, 0, 1);
        assertEquals(base * 1.15, PrayerFormula.credit(4, 1, 1, 0.3, 0, 1), 1e-12);
        assertEquals(base, PrayerFormula.credit(4, 1, 1, -1, 0, 1), 1e-12);
    }

    @Test
    void ga4_07_multiplier() {
        assertEquals(PrayerFormula.credit(1, 1.5, 1, 0, 0, 1) * 2, PrayerFormula.credit(1, 1.5, 1, 0, 0, 2), 1e-12);
        assertEquals(0, PrayerFormula.credit(1, 1.5, 1, 0, 0, 0));
    }

    @Test
    void ga4_08_badInput() {
        assertEquals(0, PrayerFormula.credit(0, 1.5, 1, 0, 0, 1));
        assertEquals(0, PrayerFormula.credit(-3, 1.5, 1, 0, 0, 1));
        assertFalse(Double.isNaN(PrayerFormula.credit(-3, 1.5, 1, 0, 0, 1)));
        assertEquals(0, PrayerFormula.daily(0, 1, 0));
    }

    @Test
    void ga4_09_dailyWithOffering() {
        assertEquals(22.27, PrayerFormula.daily(8, 1.5, 0, 0.5, 1), 0.01);
    }

    @Test
    void ga4_10_estimateIgnoresSlots() {
        assertEquals(PrayerFormula.daily(8, 1.5, 0), PrayerFormula.estimate(8, 1.5, 0, 1), 1e-9);
        assertEquals(0, PrayerFormula.estimate(0, 1.5, 0, 1), 1e-9);
        assertEquals(PrayerFormula.daily(24, 3, 0), PrayerFormula.estimate(40, 3, 0, 1), 1e-9);
    }

    @Test
    void tc10_pilgrimageDistance() {
        assertFalse(BelieverRules.pilgrimageOk(0, 0, 7.9, 0));
        assertTrue(BelieverRules.pilgrimageOk(0, 0, 8.0, 0));
        assertTrue(BelieverRules.pilgrimageOk(0, 0, 6, 6));
    }

    @Test
    void tc13_prayerWindow() {
        assertFalse(PrayerFormula.inWindow(1999));
        assertTrue(PrayerFormula.inWindow(2000));
        assertTrue(PrayerFormula.inWindow(8999));
        assertFalse(PrayerFormula.inWindow(9000));
        assertTrue(PrayerFormula.inWindow(24000 + 3000));
        assertTrue(PrayerFormula.inWindow(-21000));
        assertFalse(PrayerFormula.inWindow(-24000));
    }

    @Test
    void tc14_restockLimit() {
        assertEquals(1, BelieverRules.restockLimit(true, 1));
        assertEquals(2, BelieverRules.restockLimit(false, 1));
        assertEquals(0, BelieverRules.restockLimit(true, 0));
        assertEquals(2, BelieverRules.restockLimit(true, 2));
        assertEquals(2, BelieverRules.restockLimit(true, 5));
        long now = 100_000;
        // 上限 2 与原版一致
        assertTrue(BelieverRules.restockAllowed(0, 2, now, now));
        assertFalse(BelieverRules.restockAllowed(1, 2, now, now - 2400));
        assertTrue(BelieverRules.restockAllowed(1, 2, now, now - 2401));
        assertFalse(BelieverRules.restockAllowed(2, 2, now, 0));
        // 祈祷过：上限 1
        assertTrue(BelieverRules.restockAllowed(0, 1, now, now));
        assertFalse(BelieverRules.restockAllowed(1, 1, now, 0));
        assertFalse(BelieverRules.restockAllowed(0, 0, now, 0));
    }
}
