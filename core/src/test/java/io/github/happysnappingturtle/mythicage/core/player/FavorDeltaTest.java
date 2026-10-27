package io.github.happysnappingturtle.mythicage.core.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 恩宠 HUD 的最近变化合并（TC-T24-61 的规则部分）。 */
class FavorDeltaTest {
    @Test
    void mergesWithinWindow() {
        FavorDelta d = FavorDelta.NONE.add(2, 1000, "mythic_age.favor.source.greek_temple");
        d = d.add(3, 3000, "");
        assertEquals(5, d.delta());
        assertEquals("mythic_age.favor.source.greek_temple", d.source(), "没来源的沿用旧来源");
    }

    @Test
    void restartsAfterWindow() {
        FavorDelta d = FavorDelta.NONE.add(2, 1000, "a").add(4, 1000 + FavorDelta.WINDOW + 1, "b");
        assertEquals(4, d.delta());
        assertEquals("b", d.source());
    }

    @Test
    void zeroChangeIgnoredAndVisibility() {
        FavorDelta d = FavorDelta.NONE.add(0, 1000, "x");
        assertEquals(FavorDelta.NONE, d);
        assertFalse(d.visible(1000, 5000), "没有变化不显示");
        FavorDelta up = d.add(5, 1000, "").add(-5, 1500, "");
        assertFalse(up.visible(1600, 5000), "一加一减合并为 0 不显示");
        assertTrue(FavorDelta.NONE.add(1, 1000, "").visible(5999, 5000));
        assertFalse(FavorDelta.NONE.add(1, 1000, "").visible(6001, 5000));
    }

    @Test
    void lateSource() {
        FavorDelta d = FavorDelta.NONE.add(3, 1000, "").withSource("s", 1200);
        assertEquals("s", d.source());
        assertEquals("", FavorDelta.NONE.add(3, 1000, "").withSource("s", 1000 + FavorDelta.WINDOW + 1).source(), "过期的来源不补");
    }
}
