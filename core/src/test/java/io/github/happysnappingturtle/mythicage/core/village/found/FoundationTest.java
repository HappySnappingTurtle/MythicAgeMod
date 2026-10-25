package io.github.happysnappingturtle.mythicage.core.village.found;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T19 阶段 ⑥：奠基石规则与临时找地（单元测试用例文档 TC-T19-39、39a、39b）。 */
class FoundationTest {
    @Test
    void tc39Limit() {
        assertTrue(FoundationRules.canFound(1, 0, FoundationRules.PER_PLAYER));
        assertFalse(FoundationRules.canFound(2, 0, FoundationRules.PER_PLAYER), "已建 2 个");
        assertFalse(FoundationRules.canFound(1, 1, FoundationRules.PER_PLAYER), "宽限中的也算");
    }

    @Test
    void tc39aGapAndGrace() {
        Box other = new Box(100, 60, 100, 120, 70, 120);
        assertTrue(FoundationRules.tooClose(List.of(other), 140, 110, 32), "离已建范围 20 格");
        assertFalse(FoundationRules.tooClose(List.of(other), 160, 110, 32), "40 格");
        assertEquals(FoundationRules.Grace.ACTIVE, FoundationRules.grace(-1, 10));
        assertEquals(FoundationRules.Grace.RESTORABLE, FoundationRules.grace(10, 12), "拆后 2 日");
        assertEquals(FoundationRules.Grace.EXPIRED, FoundationRules.grace(10, 14), "拆后 4 日");
    }

    /** 平地上找得到；地块不压已建范围、在 8–32 格的环里；水、玩家方块、陡坡、未加载都不行。 */
    @Test
    void tc39bSiteFinder() {
        Box village = new Box(0, 64, 0, 9, 70, 9);
        SiteFinder.Terrain flat = (x, z) -> new SiteFinder.Column(64, false, false);
        Optional<SiteFinder.Site> s = SiteFinder.find(List.of(village), 7, 7, flat);
        assertTrue(s.isPresent());
        double gap = SiteFinder.gapToBuilt(s.get().box(), village);
        assertTrue(gap >= SiteFinder.MIN_RING && gap <= SiteFinder.MAX_RING, "在环里：" + gap);
        assertFalse(s.get().box().intersectsXZ(village.inflateXZ(SiteFinder.MARGIN)));
        assertEquals(64, s.get().ground());
        // 只有东边一片平地，其余都是水
        SiteFinder.Terrain east = (x, z) -> new SiteFinder.Column(64, x < 25 || x > 45, false);
        Optional<SiteFinder.Site> e = SiteFinder.find(List.of(village), 7, 7, east);
        assertTrue(e.isPresent() && e.get().box().minX() >= 25, "只能在东边：" + e);
        // 到处是玩家方块 / 陡坡 / 未加载
        assertTrue(SiteFinder.find(List.of(village), 7, 7, (x, z) -> new SiteFinder.Column(64, false, true)).isEmpty());
        assertTrue(SiteFinder.find(List.of(village), 7, 7, (x, z) -> new SiteFinder.Column(64 + (x + z) % 5, false, false)).isEmpty());
        assertTrue(SiteFinder.find(List.of(village), 7, 7, (x, z) -> null).isEmpty());
        // 高差 2 以内可以，取中位数整平
        Optional<SiteFinder.Site> g = SiteFinder.find(List.of(village), 5, 5, (x, z) -> new SiteFinder.Column(64 + Math.floorMod(x, 3), false, false));
        assertTrue(g.isPresent() && g.get().rise() <= 2);
    }
}
