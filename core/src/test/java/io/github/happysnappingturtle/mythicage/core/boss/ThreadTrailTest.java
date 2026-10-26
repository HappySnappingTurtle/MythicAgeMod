package io.github.happysnappingturtle.mythicage.core.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T23：回程线（TC-T23-29～33）。 */
class ThreadTrailTest {
    @Test
    void tc29Step() {
        ThreadTrail t = new ThreadTrail();
        t.add(0, 64, 0);
        t.add(1.9, 64, 0);
        assertEquals(1, t.size(), "1.9 格不记");
        t.add(2.0, 64, 0);
        assertEquals(2, t.size(), "2.0 格记一个");
    }

    @Test
    void tc30Gap() {
        ThreadTrail t = new ThreadTrail();
        t.add(0, 64, 0);
        t.add(7, 64, 0);
        var ps = t.points();
        for (int i = 1; i < ps.size(); i++) {
            assertTrue(ps.get(i).dist(ps.get(i - 1)) <= ThreadTrail.MAX_GAP + 1e-9, "相邻点 ≤ 3");
        }
        assertEquals(7, ps.get(ps.size() - 1).x(), 1e-9);
    }

    @Test
    void tc31Cap() {
        ThreadTrail t = new ThreadTrail();
        for (int i = 0; i < 401; i++) {
            t.add(i * 2.0, 64, 0);
        }
        assertEquals(400, t.size());
        assertEquals(2.0, t.points().get(0).x(), 1e-9, "丢最早的");
        assertEquals(401, t.end());
    }

    @Test
    void tc32Backtrack() {
        ThreadTrail t = new ThreadTrail();
        for (int i = 0; i < 20; i++) {
            t.add(i * 2.0, 64, 0);
        }
        int v = t.version();
        t.add(9 * 2.0 + 0.4, 64, 0.3);   // 从第 20 个点一下回到第 10 个点（下标 9）附近
        assertEquals(10, t.size(), "截到第 10 个点");
        assertEquals(v + 1, t.version(), "截断时版本加一");
    }

    @Test
    void tc33Since() {
        ThreadTrail t = new ThreadTrail();
        for (int i = 0; i < 5; i++) {
            t.add(i * 2.0, 64, 0);
        }
        assertEquals(2, t.since(3).size());
        assertTrue(t.since(5).isEmpty());
        assertEquals(5, t.since(0).size());
    }
}
