package io.github.happysnappingturtle.mythicage.core.structure;

import io.github.happysnappingturtle.mythicage.core.structure.MazePlanner.Cell;
import io.github.happysnappingturtle.mythicage.core.structure.MazePlanner.Dir;
import io.github.happysnappingturtle.mythicage.core.structure.MazePlanner.Plan;
import io.github.happysnappingturtle.mythicage.core.structure.MazePlanner.Room;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T22：迷宫网格规划（单元测试用例文档 TC-T22-01～08）。 */
class MazePlannerTest {
    static Set<Cell> reachable(Plan p) {
        Set<Cell> seen = new HashSet<>();
        ArrayDeque<Cell> q = new ArrayDeque<>();
        q.add(MazePlanner.ENTRANCE);
        seen.add(MazePlanner.ENTRANCE);
        while (!q.isEmpty()) {
            Cell c = q.poll();
            for (Dir d : p.openings(c)) {
                Cell n = c.step(d);
                if (p.open().containsKey(n) && seen.add(n)) {
                    q.add(n);
                }
            }
        }
        return seen;
    }

    static void assertValid(Plan p) {
        assertEquals(45 + 1, p.open().size(), "46 格（45 个节点，前室两格）");
        assertEquals(p.open().keySet(), reachable(p), "TC-01：全部从入口连通");
        assertTrue(p.segments() >= MazePlanner.PATH_MIN && p.segments() <= MazePlanner.PATH_MAX, "TC-02：主路径 8–12 段：" + p.segments());
        // TC-05：开口成对、不通向大厅与宝库
        for (Map.Entry<Cell, Set<Dir>> e : p.open().entrySet()) {
            for (Dir d : e.getValue()) {
                Cell n = e.getKey().step(d);
                if (e.getKey().equals(MazePlanner.ENTRANCE) && d == Dir.S) {
                    continue;
                }
                assertTrue(MazePlanner.node(n), "TC-05：不通向大厅或宝库：" + e.getKey() + d);
                assertTrue(p.openings(n).contains(d.opposite()), "TC-07：开口成对：" + e.getKey() + d);
            }
        }
        // TC-06：特殊房间
        long rest = p.rooms().values().stream().filter(r -> r == Room.REST).count();
        long col = p.rooms().values().stream().filter(r -> r == Room.COLONNADE).count();
        long store = p.rooms().values().stream().filter(r -> r == Room.STORAGE).count();
        assertEquals(1, rest, "休整点 1");
        assertEquals(1, col, "教学柱廊 1");
        assertTrue(store >= 0 && store <= 2, "储物间 0–2：" + store);
        p.rooms().forEach((c, r) -> {
            if (r == Room.REST || r == Room.COLONNADE || r == Room.STORAGE) {
                assertEquals(1, p.openings(c).size(), "特殊房间在死胡同上：" + c);
                assertFalse(p.mainPath().contains(c), "不在主路径上：" + c);
            }
        });
        assertTrue(p.openings(MazePlanner.ENTRANCE).contains(Dir.S), "入口朝南通往阶梯井");
    }

    @Test
    void tc01to07ManySeeds() {
        int fallbacks = 0;
        for (long seed = 0; seed < 1000; seed++) {
            Plan p = MazePlanner.plan(seed);
            assertValid(p);
            if (p.fallback()) {
                fallbacks++;
            }
        }
        assertTrue(fallbacks <= 1, "TC-03：1000 个种子里几乎不用固定布局：" + fallbacks);
    }

    @Test
    void tc04FallbackIsValid() {
        Plan f = MazePlanner.fallback();
        assertNotNull(f);
        assertTrue(f.fallback());
        assertValid(f);
        // 每次尝试都有界：主路径回溯有步数上限，返回空或合格布局，不会卡住
        for (long seed = 0; seed < 200; seed++) {
            Plan p = MazePlanner.tryPlan(new Random(seed), false);
            if (p != null) {
                assertValid(p);
            }
        }
    }

    @Test
    void tc08Deterministic() {
        assertEquals(MazePlanner.plan(42), MazePlanner.plan(42), "同种子同迷宫");
        assertFalse(MazePlanner.plan(42).open().equals(MazePlanner.plan(43).open()), "不同种子不同");
    }
}
