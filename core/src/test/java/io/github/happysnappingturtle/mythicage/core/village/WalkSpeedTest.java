package io.github.happysnappingturtle.mythicage.core.village;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 米/秒与寻路倍率的换算（TC-GA1-01～07）。 */
class WalkSpeedTest {
    private static final double ATTR = 0.5;

    @Test
    void tc01Zombie() {
        assertEquals(2.28, WalkSpeed.metersPerSecond(0.23, 1.0), 0.02);
    }

    @Test
    void tc02RoundTrip() {
        for (double mps : new double[]{0.5, 2.0, 3.5, 5.0, 9.0}) {
            assertEquals(mps, WalkSpeed.metersPerSecond(ATTR, WalkSpeed.modifier(ATTR, mps)), 1e-9);
        }
    }

    @Test
    void tc03Targets() {
        for (double mps : new double[]{WalkSpeed.WALK, WalkSpeed.RUN, WalkSpeed.FLEE, WalkSpeed.SOLDIER_RUN, WalkSpeed.ERRAND}) {
            double m = WalkSpeed.modifier(ATTR, mps);
            assertTrue(m >= 0.3 && m <= 1.0, mps + " 米/秒的倍率 " + m);
            assertEquals(mps, WalkSpeed.metersPerSecond(ATTR, m), 1e-9);
        }
    }

    @Test
    void tc04OldLinearWritingWasTooFast() {
        assertTrue(WalkSpeed.metersPerSecond(ATTR, 0.55 / 0.5) > 10);
    }

    @Test
    void tc05Zero() {
        assertEquals(0, WalkSpeed.modifier(ATTR, 0));
    }

    @Test
    void tc06Invalid() {
        assertThrows(IllegalArgumentException.class, () -> WalkSpeed.modifier(0, 2));
        assertThrows(IllegalArgumentException.class, () -> WalkSpeed.modifier(-0.5, 2));
        assertThrows(IllegalArgumentException.class, () -> WalkSpeed.modifier(ATTR, -1));
    }

    @Test
    void tc07Monotonic() {
        double walk = WalkSpeed.modifier(ATTR, WalkSpeed.WALK);
        double soldier = WalkSpeed.modifier(ATTR, WalkSpeed.SOLDIER_RUN);
        double run = WalkSpeed.modifier(ATTR, WalkSpeed.RUN);
        assertTrue(walk < soldier && soldier < run);
    }
}
