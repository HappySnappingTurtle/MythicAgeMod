package io.github.happysnappingturtle.mythicage.core.structure;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T22：遗迹选址（单元测试用例文档 TC-T22-15、16）。 */
class SiteRulesTest {
    @Test
    void tc15ReliefBoundary() {
        int[] four = {70, 71, 72, 73, 74, 70, 71, 72, 70};
        int[] five = {70, 71, 72, 73, 75, 70, 71, 72, 70};
        assertEquals(OptionalInt.of(71), SiteRules.floor(four, false, 4), "起伏 4：允许，地面取中位数");
        assertFalse(SiteRules.floor(five, false, 4).isPresent(), "起伏 5：不允许");
    }

    @Test
    void tc16CenterUnderWater() {
        assertTrue(SiteRules.wet(64, 61), "水面 64、水底 61：在水里");
        assertFalse(SiteRules.wet(70, 70), "陆地");
        assertFalse(SiteRules.floor(new int[]{70, 70, 70, 70, 70, 70, 70, 70, 70}, true, 4).isPresent(), "中心在水里不要");
    }
}
