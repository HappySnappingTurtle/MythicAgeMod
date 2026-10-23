package io.github.happysnappingturtle.mythicage.core.wild;

import org.junit.jupiter.api.Test;

import static io.github.happysnappingturtle.mythicage.core.wild.GrabRules.Outcome.FAIL_STUN;
import static io.github.happysnappingturtle.mythicage.core.wild.GrabRules.Outcome.GRAB;
import static io.github.happysnappingturtle.mythicage.core.wild.GrabRules.Outcome.SWIPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** T15 单元测试 TC-T15-07：哈耳庇厄抓举判定。 */
class GrabRulesTest {
    private static final double FAR = Double.POSITIVE_INFINITY;

    @Test
    void tc07_openGroundGrabs() {
        assertEquals(GRAB, GrabRules.contact(false, 100, 0, FAR, 0, 0.6, 1.8), "开阔地、玩家体型");
    }

    @Test
    void tc07_shieldFailsFirst() {
        // 盾优先于一切：免疫期、崖巢旁也是失败眩晕
        assertEquals(FAIL_STUN, GrabRules.contact(true, 100, 0, FAR, 0, 0.6, 1.8));
        assertEquals(FAIL_STUN, GrabRules.contact(true, 100, 500, 10, 9, 0.6, 1.8));
    }

    @Test
    void tc07_immunityBoundary() {
        long grabbedAt = 1000;
        long until = grabbedAt + GrabRules.IMMUNE_TICKS;
        assertEquals(SWIPE, GrabRules.contact(false, grabbedAt + 20, until, FAR, 0, 0.6, 1.8), "1 秒后另一只：拒绝抓举");
        assertEquals(SWIPE, GrabRules.contact(false, until - 1, until, FAR, 0, 0.6, 1.8), "免疫最后一刻");
        assertEquals(GRAB, GrabRules.contact(false, until, until, FAR, 0, 0.6, 1.8), "8 秒整可以再抓");
    }

    @Test
    void tc07_nestAndCliffProtection() {
        assertEquals(SWIPE, GrabRules.contact(false, 0, 0, 24 * 24, 0, 0.6, 1.8), "崖巢 24 格：改爪击");
        assertEquals(GRAB, GrabRules.contact(false, 0, 0, 24.1 * 24.1, 0, 0.6, 1.8), "24 格外");
        assertEquals(SWIPE, GrabRules.contact(false, 0, 0, FAR, 5, 0.6, 1.8), "落差 5 格：改爪击");
        assertEquals(GRAB, GrabRules.contact(false, 0, 0, FAR, 4, 0.6, 1.8), "落差 4 格：仍可抓");
    }

    @Test
    void tc07_tooBigToLift() {
        assertEquals(SWIPE, GrabRules.contact(false, 0, 0, FAR, 0, 1.4, 2.7), "铁傀儡");
        assertEquals(GRAB, GrabRules.contact(false, 0, 0, FAR, 0, 1.0, 2.0), "边界");
    }
}
