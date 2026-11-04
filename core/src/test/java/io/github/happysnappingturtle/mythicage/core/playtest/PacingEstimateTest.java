package io.github.happysnappingturtle.mythicage.core.playtest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 节奏估算（TC-GA3-30～35）。 */
class PacingEstimateTest {
    /** 与 temple/greek.json 相同：{祈祷位, 系数}。 */
    static final double[][] LEVELS = {{2, 1.0}, {4, 1.5}, {7, 2.0}, {10, 3.0}};

    @Test
    void tc30Days() {
        // 3.5 × √4 × 1 = 7/天
        assertEquals(50 / 7.0, PacingEstimate.days(50, 4, 1.0, 0).orElseThrow(), 1e-9);
    }

    @Test
    void tc31AlreadyMet() {
        assertEquals(0, PacingEstimate.days(0, 4, 1.0, 0).orElseThrow());
        assertEquals(0, PacingEstimate.days(-3, 0, 1.0, 0).orElseThrow());
    }

    @Test
    void tc32NoBelievers() {
        assertTrue(PacingEstimate.days(50, 0, 1.5, 0).isEmpty());
    }

    @Test
    void tc33Cap() {
        assertEquals(PacingEstimate.days(50, 24, 1.0, 0).orElseThrow(), PacingEstimate.days(50, 30, 1.0, 0).orElseThrow(), 1e-9);
    }

    @Test
    void tc34Minutes() {
        List<PacingEstimate.Row> rows = PacingEstimate.table(50, new int[]{4}, new int[]{1}, LEVELS);
        PacingEstimate.Row r = rows.getFirst();
        assertEquals(r.days() * 20, r.minutes(), 1e-9);
        // 一级神庙、4 人：3.5 × 2 × 1.5 = 10.5/天
        assertEquals(10.5, r.daily(), 1e-9);
    }

    @Test
    void tc35Table() {
        List<PacingEstimate.Row> rows = PacingEstimate.table(50, new int[]{2, 4, 8}, new int[]{1, 2}, LEVELS);
        assertEquals(6, rows.size());
        // 一级神庙只有 4 个祈祷位：8 人按 4 计
        assertEquals(4, rows.get(2).believers());
        assertTrue(rows.get(0).days() > rows.get(1).days());
        assertTrue(rows.get(3).days() > rows.get(4).days() && rows.get(4).days() > rows.get(5).days());
    }
}
