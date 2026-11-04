package io.github.happysnappingturtle.mythicage.core.village.soldier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 后撤一步的距离与刻数（TC-GA1-08、09）。 */
class BackstepTest {
    @Test
    void tc08StandingTarget() {
        Backstep b = Backstep.plan(3, 5, 0, 8).orElseThrow();
        assertEquals(3, b.distance(), 1e-9);
        assertEquals(12 + Backstep.ACCEL_TICKS, b.ticks());
    }

    @Test
    void tc08ChasingHusk() {
        // 尸壳约 2.28 米/秒追：要多跑一段才能拉开 3 格
        Backstep b = Backstep.plan(3, 5, 2.28, 8).orElseThrow();
        assertEquals(5 * 3 / (5 - 2.28), b.distance(), 1e-9);
        assertTrue(b.ticks() > 20);
    }

    @Test
    void tc08CappedByMaxDistance() {
        assertEquals(8, Backstep.plan(3, 5, 4.5, 8).orElseThrow().distance(), 1e-9);
    }

    @Test
    void tc09CannotOutrun() {
        assertTrue(Backstep.plan(3, 5, 5, 8).isEmpty());
        assertTrue(Backstep.plan(3, 5, 5.6, 8).isEmpty());
    }

    @Test
    void tc09Invalid() {
        assertTrue(Backstep.plan(0, 5, 0, 8).isEmpty());
        assertTrue(Backstep.plan(3, 0, 0, 8).isEmpty());
        assertTrue(Backstep.plan(3, 5, -1, 8).isEmpty());
        assertTrue(Backstep.plan(3, 5, 0, 0).isEmpty());
    }
}
