package io.github.happysnappingturtle.mythicage.core.growth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowthRulesTest {
    @Test
    void chanceMatchesExpectedDuration() {
        // 每阶段 12000 刻：约 8.8 次随机刻推进一次
        assertEquals(0.11378, GrowthRules.chancePerRandomTick(12000), 1e-4);
        assertEquals(1.0, GrowthRules.chancePerRandomTick(100), "短于随机刻间隔时封顶为 1");
        assertThrows(IllegalArgumentException.class, () -> GrowthRules.chancePerRandomTick(0));
    }

    @Test
    void tc_t6b_63_tickGuardCountsOncePerTick() {
        TickGuard guard = new TickGuard();
        int counted = 0;
        for (int i = 0; i < 10; i++) {
            if (guard.firstCallAt(100)) {
                counted++;
            }
        }
        assertEquals(1, counted);
        assertTrue(guard.firstCallAt(101));
        assertFalse(guard.firstCallAt(101));
    }
}
