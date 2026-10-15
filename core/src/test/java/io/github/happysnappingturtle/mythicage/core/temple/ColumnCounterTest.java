package io.github.happysnappingturtle.mythicage.core.temple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** TC-T8a-20～26。 */
class ColumnCounterTest {
    private static final String[] FOUR = {"#...#", ".....", ".....", ".....", "#...#"};
    private static final String[] ONE = {".....", ".....", "..#..", ".....", "....."};
    private static final String[] NONE = {".....", ".....", ".....", ".....", "....."};

    private static ColumnCounter.Result count(String[]... layers) {
        return ColumnCounter.count(GridArt.of(layers), 3);
    }

    @Test
    void tc20FourSeparateColumns() {
        assertEquals(4, count(GridArt.repeat(FOUR, 3)).count());
    }

    @Test
    void tc21TallColumnCountsOnce() {
        assertEquals(1, count(GridArt.repeat(ONE, 6)).count());
    }

    @Test
    void tc22ShortColumnListed() {
        ColumnCounter.Result r = count(ONE, ONE, NONE);
        assertEquals(0, r.count());
        assertEquals(1, r.groups().size());
        assertFalse(r.groups().getFirst().counted());
        assertEquals(2, r.groups().getFirst().longestRun());
    }

    @Test
    void tc23ClusterCountsOnce() {
        String[] cluster = {".....", ".##..", ".##..", ".....", "....."};
        assertEquals(1, count(GridArt.repeat(cluster, 3)).count());
    }

    @Test
    void tc24BrokenColumnSplits() {
        // 下段 2、空 1、上段 3：两组，只有上段合格
        ColumnCounter.Result r = count(ONE, ONE, NONE, ONE, ONE, ONE);
        assertEquals(1, r.count());
        assertEquals(2, r.groups().size());
    }

    @Test
    void tc25LShapeIsOneColumn() {
        String[] arm = {".....", ".....", "..###", ".....", "....."};
        assertEquals(1, count(ONE, ONE, arm).count());
    }

    @Test
    void tc26OnlyInsideVolumeCounts() {
        // 扫描体只有 2 层：再高的柱在体内也只有 2 段
        assertEquals(0, count(ONE, ONE).count());
    }
}
