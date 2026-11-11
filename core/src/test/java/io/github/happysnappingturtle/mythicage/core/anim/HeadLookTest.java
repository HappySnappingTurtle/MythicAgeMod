package io.github.happysnappingturtle.mythicage.core.anim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeadLookTest {
    @Test
    void tc01ZeroYaw() {
        assertEquals(0, HeadLook.yaw(0, 50));
    }

    @Test
    void tc02PositiveFullTurn() {
        assertEquals(-2, HeadLook.yaw(358, 50));
    }

    @Test
    void tc03NegativeFullTurn() {
        assertEquals(2, HeadLook.yaw(-358, 50));
    }

    @Test
    void tc04WrapBeforeClamp() {
        assertEquals(-50, HeadLook.yaw(200, 50));
    }

    @Test
    void tc05YawWithinAndAboveLimit() {
        assertEquals(40, HeadLook.yaw(40, 50));
        assertEquals(50, HeadLook.yaw(70, 50));
    }

    @Test
    void tc06MultipleTurns() {
        assertEquals(0.5F, HeadLook.yaw(720.5F, 50));
    }

    @Test
    void tc07PitchWithinAndBelowLimit() {
        assertEquals(-30, HeadLook.pitch(-60, 30));
        assertEquals(10, HeadLook.pitch(10, 30));
    }

    @Test
    void tc08NanYawIsZero() {
        assertEquals(0, HeadLook.yaw(Float.NaN, 50));
    }

    @Test
    void halfTurnUsesPositiveEndpoint() {
        // 文档要求 (−180, 180]，不能直接照搬原版的 [−180, 180)。
        for (float angle : new float[]{-540, -180, 180, 540}) {
            assertEquals(180, HeadLook.yaw(angle, 180));
            assertEquals(50, HeadLook.yaw(angle, 50));
        }
    }

    @Test
    void negativeYawAndPositivePitchAreClamped() {
        assertEquals(-50, HeadLook.yaw(-70, 50));
        assertEquals(30, HeadLook.pitch(60, 30));
        assertEquals(0, HeadLook.yaw(40, 0));
        assertEquals(0, HeadLook.pitch(20, 0));
    }

    @Test
    void nonFiniteAnglesAreZero() {
        for (float angle : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertEquals(0, HeadLook.yaw(angle, 50));
            assertEquals(0, HeadLook.pitch(angle, 30));
        }
    }
}
