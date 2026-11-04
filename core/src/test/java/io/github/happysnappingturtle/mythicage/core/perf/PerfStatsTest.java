package io.github.happysnappingturtle.mythicage.core.perf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 每刻耗时统计（TC-T24-20～23）。 */
class PerfStatsTest {
    @Test
    void tc20AvgP95Max() {
        PerfStats s = new PerfStats(100);
        for (int i = 1; i <= 100; i++) {
            s.add("village", i * 1000L);
            s.endTick();
        }
        PerfStats.Summary v = s.summary("village");
        assertEquals(50.5, v.avgMicros(), 1e-9);
        assertEquals(95, v.p95Micros(), 1e-9);
        assertEquals(100, v.maxMicros(), 1e-9);
        assertEquals(100, v.samples());
    }

    @Test
    void tc21Empty() {
        PerfStats s = new PerfStats(10);
        assertEquals(PerfStats.Summary.EMPTY, s.summary("village"), "没有样本：不除零");
        assertThrows(IllegalArgumentException.class, () -> new PerfStats(0));
    }

    @Test
    void tc22WindowRolls() {
        PerfStats s = new PerfStats(4);
        for (long v : new long[]{100_000, 100_000, 1000, 1000, 1000, 1000}) {
            s.add("arena", v);
            s.endTick();
        }
        assertEquals(1, s.summary("arena").maxMicros(), 1e-9, "只统计最近 4 刻");
        assertEquals(4, s.summary("arena").samples());
    }

    @Test
    void tc23CategoriesSeparateAndTotal() {
        PerfStats s = new PerfStats(10);
        s.add("village", 3000);
        s.add("temple", 1000);
        s.add("village", 1000);
        s.endTick();
        s.add("temple", 2000);
        s.endTick();
        assertEquals(2, s.summary("village").avgMicros(), 1e-9, "第二刻没跑记 0：(4 + 0) / 2");
        assertEquals(1.5, s.summary("temple").avgMicros(), 1e-9);
        assertEquals(3.5, s.summary(PerfStats.TOTAL).avgMicros(), 1e-9, "合计 (5 + 2) / 2");
        assertEquals(List.of("village", "temple"), s.categories());
    }
}
