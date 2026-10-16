package io.github.happysnappingturtle.mythicage.core.greek.prayer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T10 信徒祈祷的 core 用例（TC-T10-01～14）。 */
class PrayerFormulaTest {
    private static final double PER_TICK = PrayerFormula.BASE_PER_MINUTE / PrayerFormula.TICKS_PER_MINUTE;

    private static PrayerFormula.Altar altar(int n, double t) {
        return new PrayerFormula.Altar(n, t, 1);
    }

    private static double sum(double[] a) {
        double s = 0;
        for (double v : a) {
            s += v;
        }
        return s;
    }

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

    @Test
    void tc02_splitByHeadcount() {
        double[] out = PrayerFormula.distribute(List.of(altar(3, 1.5), altar(1, 1.5)), 0, 20);
        assertEquals(3.0, out[0] / out[1], 1e-9);
        assertEquals(PER_TICK * 20 * Math.sqrt(4) * 1.5, sum(out), 1e-12);
    }

    @Test
    void tc03_splitByTier() {
        double[] out = PrayerFormula.distribute(List.of(altar(3, 3.0), altar(1, 1.5)), 0, 20);
        double tBar = (3 * 3.0 + 1 * 1.5) / 4;
        assertEquals(PER_TICK * 20 * Math.sqrt(4) * tBar, sum(out), 1e-12);
        assertEquals(3 * 3.0 / (1 * 1.5), out[0] / out[1], 1e-9);
    }

    @Test
    void tc04_nobodyPraying() {
        assertArrayEquals(new double[]{0, 0}, PrayerFormula.distribute(List.of(altar(0, 1), altar(0, 3)), 0, 20));
        assertArrayEquals(new double[0], PrayerFormula.distribute(List.of(), 0, 20));
        assertEquals(0, PrayerFormula.daily(0, 1, 0));
    }

    @Test
    void tc05_capAt24() {
        double[] out = PrayerFormula.distribute(List.of(altar(20, 1), altar(10, 1)), 0, 20);
        assertEquals(PER_TICK * 20 * Math.sqrt(24), sum(out), 1e-12);
        assertEquals(2.0, out[0] / out[1], 1e-9);
    }

    @Test
    void tc06_rainOnlyHitsOpenAltar() {
        double[] dry = PrayerFormula.distribute(List.of(altar(2, 1.0), altar(2, 1.5)), 0, 20);
        double[] wet = PrayerFormula.distribute(List.of(new PrayerFormula.Altar(2, 1.0, PrayerFormula.RAIN_FACTOR),
                altar(2, 1.5)), 0, 20);
        assertEquals(dry[0] / 2, wet[0], 1e-12);
        assertEquals(dry[1], wet[1], 1e-12);
    }

    @Test
    void tc07_relicBonusClamped() {
        double base = sum(PrayerFormula.distribute(List.of(altar(4, 1)), 0, 20));
        assertEquals(base * 1.15, sum(PrayerFormula.distribute(List.of(altar(4, 1)), 0.15, 20)), 1e-12);
        assertEquals(base * 1.15, sum(PrayerFormula.distribute(List.of(altar(4, 1)), 0.4, 20)), 1e-12);
        assertEquals(base, sum(PrayerFormula.distribute(List.of(altar(4, 1)), -1, 20)), 1e-12);
    }

    @Test
    void tc08_samplesAddUpToDaily() {
        double total = 0;
        for (int i = 0; i < 350; i++) {
            total += sum(PrayerFormula.distribute(List.of(altar(2, 1.0)), 0, 20));
        }
        assertEquals(PrayerFormula.daily(2, 1.0, 0), total, 1e-9);
        assertEquals(4.9, total, 0.1);
    }

    @Test
    void tc09_snapshotEstimate() {
        assertEquals(10.5, PrayerFormula.estimate(9, 4, 1.5, 0), 1e-9);
        assertEquals(0, PrayerFormula.estimate(0, 4, 1.5, 0), 1e-9);
        assertEquals(PrayerFormula.daily(24, 3, 0), PrayerFormula.estimate(40, 30, 3, 0), 1e-9);
    }

    @Test
    void tc10_pilgrimageDistance() {
        assertFalse(BelieverRules.pilgrimageOk(0, 0, 7.9, 0));
        assertTrue(BelieverRules.pilgrimageOk(0, 0, 8.0, 0));
        assertTrue(BelieverRules.pilgrimageOk(0, 0, 6, 6));
    }

    @Test
    void tc11_oneOwnerPerDay() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(BelieverRules.canPrayFor(-1, null, 5, a));
        assertTrue(BelieverRules.canPrayFor(5, a, 5, a));
        assertFalse(BelieverRules.canPrayFor(5, a, 5, b));
        assertTrue(BelieverRules.canPrayFor(5, a, 6, b));
    }

    @Test
    void tc12_altarOrder() {
        UUID o = UUID.randomUUID();
        var near = new BelieverRules.Candidate<>("in5", o, true, 5);
        var far = new BelieverRules.Candidate<>("in12", o, true, 12);
        var outside = new BelieverRules.Candidate<>("out3", o, false, 3);
        assertEquals(List.of("in5", "in12"),
                BelieverRules.order(List.of(far, outside, near)).stream().map(BelieverRules.Candidate::altar).toList());
        var tooFar = new BelieverRules.Candidate<>("out60", o, false, 60);
        assertEquals(List.of("out3"),
                BelieverRules.order(List.of(tooFar, outside)).stream().map(BelieverRules.Candidate::altar).toList());
        assertTrue(BelieverRules.order(List.<BelieverRules.Candidate<String>>of()).isEmpty());
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
