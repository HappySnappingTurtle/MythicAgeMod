package io.github.happysnappingturtle.mythicage.core.relic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T25a 赫菲斯托斯之砧的经验折扣（TC-T25a-37～39）。 */
class AnvilDiscountTest {
    @Test
    void tc37TenBecomesSeven() {
        assertEquals(7, AnvilDiscount.cost(10, 0.3));
        assertEquals(1, AnvilDiscount.cost(1, 0.3), "至少 1");
        assertEquals(0, AnvilDiscount.cost(0, 0.3));
        assertEquals(21, AnvilDiscount.cost(30, 0.3), "浮点误差不少算");
        assertEquals(10, AnvilDiscount.cost(10, 0), "没戴不折");
    }

    @Test
    void tc38TooExpensiveStays() {
        assertTrue(AnvilDiscount.tooExpensive(45));
        assertEquals(45, AnvilDiscount.cost(45, 0.3), "按折前价判，仍过于昂贵");
        assertEquals(40, AnvilDiscount.cost(40, 0.3));
    }

    @Test
    void tc39JustBelow() {
        assertFalse(AnvilDiscount.tooExpensive(39));
        assertEquals(27, AnvilDiscount.cost(39, 0.3));
    }
}
