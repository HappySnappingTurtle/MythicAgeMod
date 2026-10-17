package io.github.happysnappingturtle.mythicage.core.power;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T12 阶段 ②：神力护盾、临时实体预算、产物组（TC-T12-40～46）。 */
class PowerLimitsTest {
    static List<String> ids(String prefix, int n) {
        return IntStream.range(0, n).mapToObj(i -> prefix + i).toList();
    }

    @Test
    void tc40_shield() {
        ShieldPool pool = new ShieldPool();
        pool.add(new ShieldPool.Shield("overworld", 0, 0, 32, 200));
        assertEquals(100, pool.remaining("overworld", 31, 0, 100));
        assertEquals(0, pool.remaining("overworld", 33, 0, 100));
        assertEquals(0, pool.remaining("the_nether", 0, 0, 100), "不同维度");
        assertEquals(0, pool.remaining("overworld", 0, 0, 200), "到期");
        assertEquals(0, pool.size(), "到期后清掉");
    }

    @Test
    void tc41_overlapTakesLongest() {
        ShieldPool pool = new ShieldPool();
        pool.add(new ShieldPool.Shield("o", 0, 0, 32, 200));
        pool.add(new ShieldPool.Shield("o", 20, 0, 32, 500));
        assertEquals(400, pool.remaining("o", 10, 0, 100));
    }

    @Test
    void tc42_personalCap() {
        TransientBudget b = new TransientBudget();
        UUID a = UUID.randomUUID();
        assertEquals(List.of(), b.register(a, ids("a", 16), 0, 100, 16, 96));
        assertEquals(List.of("a0"), b.register(a, List.of("x"), 1, 100, 16, 96));
        assertEquals(16, b.count(a));
    }

    @Test
    void tc43_globalCap() {
        TransientBudget b = new TransientBudget();
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID p = UUID.randomUUID();
            players.add(p);
            b.register(p, ids("p" + i + "_", 9), i, 1000, 16, 96);
        }
        assertEquals(90, b.count());
        List<String> ev = b.register(players.get(9), ids("new", 7), 20, 1000, 16, 96);
        assertEquals(List.of("p0_0"), ev, "淘汰全服最早的 1 个（不论属于谁）");
        assertEquals(96, b.count());
    }

    @Test
    void tc44_wholeBatchSpawns() {
        TransientBudget b = new TransientBudget();
        UUID a = UUID.randomUUID();
        b.register(a, ids("old", 14), 0, 100, 16, 96);
        List<String> ev = b.register(a, ids("new", 5), 1, 100, 16, 96);
        assertEquals(List.of("old0", "old1", "old2"), ev);
        assertTrue(ids("new", 5).stream().allMatch(b::contains), "本批全部生成");
    }

    @Test
    void tc45_lifetimeOver300s() {
        TransientBudget b = new TransientBudget();
        assertThrows(IllegalArgumentException.class, () -> b.register(UUID.randomUUID(), List.of("x"), 0, 6001, 16, 96));
    }

    @Test
    void expire() {
        TransientBudget b = new TransientBudget();
        UUID a = UUID.randomUUID();
        b.register(a, List.of("x"), 0, 100, 16, 96);
        assertEquals(List.of(), b.expire(99));
        assertEquals(List.of("x"), b.expire(100));
        assertEquals(0, b.count());
    }

    @Test
    void tc46_productGroups() {
        ProductGroups g = new ProductGroups();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertEquals(List.of(), g.replace(a, "sentinel", List.of("s1", "s2")));
        assertEquals(List.of(), g.replace(a, "lure", List.of("l1")), "不同神力互不影响");
        assertEquals(List.of(), g.replace(b, "sentinel", List.of("t1")), "不同玩家互不影响");
        assertEquals(List.of("s1", "s2"), g.replace(a, "sentinel", List.of("s3")));
        g.forget("s3");
        assertEquals(List.of(), g.group(a, "sentinel"));
    }
}
