package io.github.happysnappingturtle.mythicage.core.greek.temple;

import io.github.happysnappingturtle.mythicage.core.scan.Grid3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 屋顶、露天祭坛、台基（修订 01）与祈祷位（TC-T8a-40～45）。
 * 扫描体 15 × 15 × 18：神坛在 (7, 2, 1)，站立高度 y = 2（地面方块在 y = 1），神庙在 +z 方向。
 */
class TempleGeometryTest {
    private static final TempleRules.Scan SCAN = new TempleRules.Scan(7, 1, 16, 2, 12);
    private static final TempleRules.Roof ROOF = new TempleRules.Roof(60, 3);
    private final TempleGeometry geo = new TempleGeometry(SCAN);

    private Grid3 lastColumns;

    /** 四根柱子围成 9×9（x 3–11、z 5–13），柱脚在 baseY，高 3。 */
    private ColumnCounter.Result corners(int baseY) {
        Grid3 col = geo.newGrid();
        lastColumns = col;
        for (int[] c : new int[][]{{3, 5}, {11, 5}, {3, 13}, {11, 13}}) {
            for (int y = baseY; y < baseY + 3; y++) {
                col.set(c[0], y, c[1], true);
            }
        }
        return ColumnCounter.count(col, 3);
    }

    /** 柱围矩形上方 baseY+3 处按行铺 n 格屋顶。 */
    private Grid3 roofCover(int n, int y) {
        Grid3 g = geo.newGrid();
        int i = 0;
        for (int z = 5; z <= 13 && i < n; z++) {
            for (int x = 3; x <= 11 && i < n; x++, i++) {
                g.set(x, y, z, true);
            }
        }
        return g;
    }

    @Test
    void roofOverColumnBox() {
        var box = TempleGeometry.columnBox(corners(2));
        assertEquals(9, box.width());
        assertEquals(9, box.depth());
        TempleGeometry.Roof r = geo.roof(roofCover(49, 5), lastColumns, box, ROOF);
        assertEquals(49, r.covered());
        assertEquals(81, r.area());
        assertTrue(ROOF.satisfied(r.covered(), r.area()));
        assertFalse(ROOF.satisfied(geo.roof(roofCover(48, 5), lastColumns, box, ROOF).covered(), 81));
    }

    @Test
    void platformBelowColumnsIsNotRoof() {
        // 柱脚在 y=4（台基 3 层：y=1..3），台基本身不算屋顶
        Grid3 cover = geo.newGrid();
        for (int z = 5; z <= 13; z++) {
            for (int x = 3; x <= 11; x++) {
                for (int y = 1; y <= 3; y++) {
                    cover.set(x, y, z, true);
                }
            }
        }
        assertEquals(0, geo.roof(cover, geo.newGrid(), TempleGeometry.columnBox(corners(4)), ROOF).covered());
    }

    @Test
    void columnShaftIsNotRoof() {
        var box = TempleGeometry.columnBox(corners(2));
        Grid3 cover = geo.newGrid();
        for (int[] c : new int[][]{{3, 5}, {11, 5}, {3, 13}, {11, 13}}) {
            for (int y = 2; y < 5; y++) {
                cover.set(c[0], y, c[1], true);
            }
        }
        assertEquals(0, geo.roof(cover, lastColumns, box, ROOF).covered(), "柱身是完整方块，但不算屋顶");
    }

    @Test
    void columnsInALineEncloseNothing() {
        Grid3 col = geo.newGrid();
        for (int x = 3; x <= 11; x += 2) {
            for (int y = 2; y < 5; y++) {
                col.set(x, y, 6, true);
            }
        }
        var box = TempleGeometry.columnBox(ColumnCounter.count(col, 3));
        assertEquals(0, geo.roof(roofCover(81, 5), col, box, ROOF).area(), "一排柱子没有围出空间");
    }

    @Test
    void openAltar() {
        Grid3 cover = geo.newGrid();
        assertTrue(geo.openAltar(cover));
        assertNull(geo.altarCover(cover));
        cover.set(7, 12, 1, true);
        assertFalse(geo.openAltar(cover));
        cover.set(7, 2, 1, true);
        cover.set(7, 12, 1, false);
        assertTrue(geo.openAltar(cover), "神坛本身不算遮盖");
    }

    @Test
    void raisedColumns() {
        assertEquals(0, geo.raisedColumns(corners(2)));
        assertEquals(4, geo.raisedColumns(corners(3)));
    }

    // ───────────── 站立、台基、祈祷位 ─────────────

    /** 站立高度 y=2；以 (3..11, 5..13) 为底建 h 层台基；stairs 为 true 时正面（z=4、3…）逐级向外铺台阶。 */
    private Grid3[] terrain(int h, boolean stairs) {
        Grid3 top = geo.newGrid();
        Grid3 pass = geo.newGrid();
        for (int z = 0; z < 18; z++) {
            for (int x = 0; x < 15; x++) {
                int floor = 2; // 站立高度（地面方块在 floor - 1）
                if (x >= 3 && x <= 11 && z >= 5 && z <= 13) {
                    floor = 2 + h;
                } else if (stairs && x >= 6 && x <= 8 && z < 5 && z >= 5 - h) {
                    floor = 2 + h - (5 - z); // z=4 低一级，z=3 再低一级……
                }
                for (int y = 0; y < floor; y++) {
                    top.set(x, y, z, y == floor - 1);
                }
                for (int y = floor; y < 15; y++) {
                    pass.set(x, y, z, true);
                }
            }
        }
        pass.set(7, 2, 1, false);
        return new Grid3[]{top, pass};
    }

    private Grid3 reach(Grid3[] t) {
        return geo.reachable(geo.standable(t[0], t[1]));
    }

    @Test
    void platformOneHighIsReachable() {
        assertTrue(geo.platformReachable(reach(terrain(1, false)), TempleGeometry.columnBox(corners(3))));
    }

    @Test
    void platformThreeHighNeedsStairs() {
        var box = TempleGeometry.columnBox(corners(5));
        assertFalse(geo.platformReachable(reach(terrain(3, false)), box), "3 格高、没有台阶走不上去");
        assertTrue(geo.platformReachable(reach(terrain(3, true)), box), "有正面台阶就能走上去");
    }

    @Test
    void reachableFloorCountsPlatformTop() {
        Grid3[] t = terrain(1, false);
        Grid3 floor = geo.newGrid();
        for (int z = 5; z <= 13; z++) {
            for (int x = 3; x <= 11; x++) {
                floor.set(x, 2, z, true);
            }
        }
        floor.set(0, 13, 0, true);
        assertEquals(81, geo.reachableFloor(floor, reach(t)), "台基顶面算，够不着的高处不算");
    }

    private List<int[]> slots(Grid3[] t, int limit) {
        return geo.prayerSlots(reach(t), limit);
    }

    private static void assertSpaced(List<int[]> slots) {
        for (int i = 0; i < slots.size(); i++) {
            int[] a = slots.get(i);
            int d = Math.max(Math.abs(a[0] - 7), Math.abs(a[2] - 1));
            assertTrue(d >= 1 && d <= 4, "距离应在 1–4");
            for (int j = i + 1; j < slots.size(); j++) {
                int[] b = slots.get(j);
                assertTrue(Math.max(Math.abs(a[0] - b[0]), Math.abs(a[2] - b[2])) >= 2, "祈祷位不应相邻");
            }
        }
    }

    @Test
    void tc40OpenAltarTwoSlots() {
        List<int[]> s = slots(terrain(0, false), 2);
        assertEquals(2, s.size());
        assertSpaced(s);
    }

    @Test
    void tc41TenSlots() {
        List<int[]> s = slots(terrain(0, false), 10);
        assertEquals(10, s.size());
        assertSpaced(s);
    }

    @Test
    void tc42WalledInIsUnreachable() {
        Grid3[] t = terrain(0, false);
        for (int x = 1; x <= 13; x++) {
            for (int y = 2; y <= 4; y++) {
                t[1].set(x, y, 0, false);
                t[1].set(x, y, 7, false);
            }
        }
        for (int z = 0; z <= 7; z++) {
            for (int y = 2; y <= 4; y++) {
                t[1].set(1, y, z, false);
                t[1].set(13, y, z, false);
            }
        }
        assertEquals(0, slots(t, 4).size());
    }

    @Test
    void tc43LowCeilingNotStandable() {
        Grid3[] t = terrain(0, false);
        for (int z = 0; z < 18; z++) {
            for (int x = 0; x < 15; x++) {
                t[1].set(x, 3, z, false);
            }
        }
        assertEquals(0, slots(t, 4).size());
    }

    @Test
    void tc45Deterministic() {
        List<int[]> a = slots(terrain(0, false), 7);
        List<int[]> b = slots(terrain(0, false), 7);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(List.of(a.get(i)[0], a.get(i)[1], a.get(i)[2]), List.of(b.get(i)[0], b.get(i)[1], b.get(i)[2]));
        }
    }

    /** 修订 02：2 格高台基 = 整格一级 + 外圈半格踏步，逐级可走（每步脚底高差 ≤ 1）。 */
    @Test
    void halfStepRingMakesPlatformReachable() {
        Grid3 top2 = geo.newGrid();
        Grid3 pass2 = geo.newGrid();
        Grid3 half2 = geo.newGrid();
        // 地面脚底 2 → 外圈半砖 2.5 → 内圈整格 + 半砖 3.5 → 柱座两整格 4
        for (int z = 0; z < 18; z++) {
            for (int x = 0; x < 15; x++) {
                boolean platform = x >= 4 && x <= 10 && z >= 6 && z <= 12;
                boolean inner = !platform && x >= 3 && x <= 11 && z >= 5 && z <= 13;
                boolean outer = !platform && !inner && x >= 2 && x <= 12 && z >= 4 && z <= 14;
                top2.set(x, 1, z, true);
                int air = 2;
                if (platform) {
                    top2.set(x, 2, z, true);
                    top2.set(x, 3, z, true);
                    air = 4;
                } else if (inner) {
                    top2.set(x, 2, z, true);
                    half2.set(x, 3, z, true);
                    air = 4;
                } else if (outer) {
                    half2.set(x, 2, z, true);
                    air = 3;
                }
                for (int y = air; y < 15; y++) {
                    pass2.set(x, y, z, true);
                }
            }
        }
        var box2 = TempleGeometry.columnBox(corners(4));
        assertFalse(geo.platformReachable(geo.reachable(geo.standable(top2, pass2)), box2),
                "不认半格踏步时走不上去");
        assertTrue(geo.platformReachable(geo.reachable(geo.standable(top2, pass2, half2), half2), box2),
                "整格 + 半格踏步逐级上去");
    }
}
