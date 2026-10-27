package io.github.happysnappingturtle.mythicage.core.village.gen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 村界轮廓（TC-T24-26～28）。 */
class OutlineTest {
    @Test
    void tc26SingleBoxPerimeter() {
        List<Outline.Edge> e = Outline.union(List.of(new Box(0, 64, 0, 2, 64, 1)));
        assertEquals(2 * (3 + 2), e.size(), "3×2 的矩形周长 10");
        assertTrue(e.contains(new Outline.Edge(0, 0, true)));
        assertTrue(e.contains(new Outline.Edge(3, 1, false)), "东边在 x = 3");
    }

    @Test
    void tc27OverlapHasNoInnerSeam() {
        // 两个 2×2 重叠一列：并集 3×2，周长 10；内部接缝 x = 1、x = 2 不出现竖边
        List<Outline.Edge> e = Outline.union(List.of(new Box(0, 0, 0, 1, 0, 1), new Box(1, 0, 0, 2, 0, 1)));
        assertEquals(10, e.size());
        assertFalse(e.contains(new Outline.Edge(1, 0, false)));
        assertFalse(e.contains(new Outline.Edge(2, 0, false)));
        // 互不相接的两块各自一圈
        assertEquals(8, Outline.union(List.of(new Box(0, 0, 0, 0, 0, 0), new Box(5, 0, 5, 5, 0, 5))).size());
    }

    @Test
    void tc28HugeFallsBackToRects() {
        Box big = new Box(0, 0, 0, 999, 0, 999);
        List<Outline.Edge> e = Outline.union(List.of(big));
        assertEquals(4000, e.size(), "超过面积上限退回画边框");
        assertTrue(Outline.union(List.of()).isEmpty());
    }
}
