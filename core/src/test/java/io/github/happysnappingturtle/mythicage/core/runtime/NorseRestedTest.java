package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NorseRestedTest {
    private static final RuntimeConfig F = Fixtures.preset(RuntimeConfig.Preset.F);
    private static final RuntimeConfig E = Fixtures.preset(RuntimeConfig.Preset.E);

    @Test
    void tc_t5_35_restedAccrueAndConsume() {
        double rested = NorseRested.accrue(0, Ticks.DAY, 60, F.favor().offline().rate(), F.norse().rested());
        assertEquals(30, rested, 1e-9);
        var r = NorseRested.consume(rested, 10, F.norse().rested());
        assertEquals(20, r.granted());
        assertEquals(10, r.countedTowardDailyCap());
        assertEquals(20, r.restedRemaining(), 1e-9);
    }

    @Test
    void restedCappedAndBonusLimitedByRemaining() {
        double rested = NorseRested.accrue(0, 100 * Ticks.DAY, 60, 0.5, F.norse().rested());
        assertEquals(60, rested, 1e-9);
        var r = NorseRested.consume(3, 10, F.norse().rested());
        assertEquals(13, r.granted());
        assertEquals(0, r.restedRemaining(), 1e-9);
    }

    @Test
    void tc_t5_36_restedDisabled() {
        assertEquals(0, NorseRested.accrue(0, Ticks.DAY, 60, 0.5, E.norse().rested()), 1e-9);
        var r = NorseRested.consume(30, 10, E.norse().rested());
        assertEquals(10, r.granted());
        assertEquals(10, r.countedTowardDailyCap());
        assertEquals(30, r.restedRemaining(), 1e-9);
    }

    @Test
    void restedWithoutBypassCountsBonus() {
        var cfg = new RuntimeConfig.Rested(true, 2.0, 1.0, false);
        assertEquals(20, NorseRested.consume(30, 10, cfg).countedTowardDailyCap());
    }
}
