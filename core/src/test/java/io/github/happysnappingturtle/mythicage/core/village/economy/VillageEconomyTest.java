package io.github.happysnappingturtle.mythicage.core.village.economy;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T19 单元测试 TC-T19-01～06（库存与托管）、25～31（离线补算）。 */
class VillageEconomyTest {
    private static final String CLAY = "clay_ball", RAW = "raw_tile", PART = "component", STICK = "olive_stick",
            BREAD = "flatbread";

    @Test
    void tc01_depositRespectsCapacity() {
        VillageStock s = new VillageStock(10);
        s.deposit(CLAY, 8);
        assertEquals(2, s.deposit(CLAY, 5));
        assertEquals(10, s.count(CLAY));
    }

    @Test
    void tc02_reserveAllOrNothing() {
        VillageStock s = new VillageStock(100);
        s.deposit(CLAY, 3);
        assertFalse(s.reserve("job:1", Map.of(CLAY, 4)));
        assertEquals(3, s.count(CLAY));
        assertFalse(s.hasEscrow("job:1"));
    }

    @Test
    void tc03_releaseIgnoresCapacity() {
        VillageStock s = new VillageStock(10);
        s.deposit(CLAY, 10);
        assertTrue(s.reserve("job:1", Map.of(CLAY, 4)));
        s.deposit(RAW, 4);
        assertEquals(0, s.free());
        s.release("job:1");
        assertEquals(10, s.count(CLAY), "退回不受容量限制");
        assertEquals(14, s.total());
    }

    @Test
    void tc04_consumeConserves() {
        VillageStock s = new VillageStock(100);
        s.deposit(CLAY, 10);
        s.reserve("job:1", Map.of(CLAY, 4));
        assertEquals(10, s.count(CLAY) + s.escrowed(CLAY), "托管期间总量守恒");
        assertEquals(Map.of(CLAY, 4), s.consume("job:1"));
        assertEquals(6, s.count(CLAY));
        assertEquals(0, s.escrowed(CLAY));
        assertEquals(Map.of(), s.consume("job:1"), "同一托管不能消耗两次");
    }

    @Test
    void tc05_rationFloorLimitsSale() {
        VillageStock s = new VillageStock(100);
        s.deposit(BREAD, 30);
        Rations r = new Rations(Map.of(BREAD, 1.0), 24);
        assertEquals(6, r.sellable(s, BREAD));
        assertEquals(6, s.take(BREAD, 10, s.count(BREAD) - r.sellable(s, BREAD)));
        assertEquals(24, s.count(BREAD));
    }

    @Test
    void tc06_reloadReleasesJobEscrowsOnly() {
        VillageStock s = new VillageStock(100);
        s.deposit(CLAY, 20);
        s.reserve("job:1", Map.of(CLAY, 2));
        s.reserve("job:2", Map.of(CLAY, 2));
        s.reserve("job:3", Map.of(CLAY, 2));
        s.reserve("project:p1", Map.of(CLAY, 5));
        assertEquals(3, s.releaseJobEscrows());
        assertEquals(15, s.count(CLAY));
        assertTrue(s.hasEscrow("project:p1"));
        assertEquals(5, s.escrowed(CLAY));
    }

    @Test
    void consumePartial() {
        VillageStock s = new VillageStock(100);
        s.deposit(CLAY, 10);
        s.reserve("project:p", Map.of(CLAY, 10));
        assertEquals(4, s.consumePart("project:p", CLAY, 4));
        assertEquals(6, s.escrowed(CLAY));
        assertEquals(6, s.consumePart("project:p", CLAY, 99));
        assertFalse(s.hasEscrow("project:p"));
    }

    @Test
    void rationsConsumeAndShortage() {
        VillageStock s = new VillageStock(100);
        s.deposit(BREAD, 3);
        s.deposit("travel_meal", 2);
        Rations r = new Rations(Map.of(BREAD, 1.0, "travel_meal", 1.5), 0);
        assertEquals(6.0, r.total(s), 1e-9);
        assertEquals(4.0, r.consume(s, 10), 1e-9);
        assertEquals(0, s.total());
    }

    // ───────────── 离线补算 ─────────────

    private static Map<String, ResourcePoint> clayPit(int remaining) {
        return new LinkedHashMap<>(Map.of("clay_0", new ResourcePoint("clay_0", "clay", new Pos(0, 0, 0), remaining, 48, 4, 0)));
    }

    private static CatchUpPlanner.Producer digger(double perDay) {
        return new CatchUpPlanner.Producer("dig", Map.of(), Map.of(CLAY, 4), perDay, "clay_0", 4);
    }

    @Test
    void tc25_atMostThreeDays() {
        VillageStock s = new VillageStock(1000);
        CatchUpPlanner.Result r = CatchUpPlanner.plan(s, Map.of(), List.of(
                new CatchUpPlanner.Producer("p", Map.of(), Map.of(BREAD, 2), 1, null, 0)), 5, 3, 5, 0, 1, Rations.none());
        assertEquals(3, r.days(), 1e-9);
        assertEquals(6, s.count(BREAD));
    }

    @Test
    void tc26_limitedByInputs() {
        VillageStock s = new VillageStock(1000);
        s.deposit(CLAY, 4);
        CatchUpPlanner.plan(s, Map.of(), List.of(
                new CatchUpPlanner.Producer("shape", Map.of(CLAY, 4), Map.of(RAW, 4), 2, null, 0)), 1, 3, 1, 0, 1,
                Rations.none());
        assertEquals(4, s.count(RAW), "日产 8，黏土只够 4");
    }

    @Test
    void tc27_limitedByPoint() {
        VillageStock s = new VillageStock(1000);
        Map<String, ResourcePoint> pts = new LinkedHashMap<>(Map.of("q", new ResourcePoint("q", "quarry", new Pos(0, 0, 0), 5, 96, 0, 0)));
        CatchUpPlanner.Result r = CatchUpPlanner.plan(s, pts, List.of(
                new CatchUpPlanner.Producer("quarry", Map.of(), Map.of("marble", 1), 8, "q", 1)), 1, 3, 1, 0, 1, Rations.none());
        assertEquals(5, s.count("marble"));
        assertEquals(0, r.points().get("q").remaining());
    }

    @Test
    void tc28_limitedByCapacity() {
        VillageStock s = new VillageStock(10);
        CatchUpPlanner.Result r = CatchUpPlanner.plan(s, Map.of(), List.of(
                new CatchUpPlanner.Producer("p", Map.of(), Map.of(BREAD, 20), 1, null, 0)), 1, 3, 1, 0, 1, Rations.none());
        assertEquals(10, s.count(BREAD));
        assertEquals(10, r.discarded().get(BREAD));
    }

    @Test
    void tc29_chainUpstreamFirst() {
        VillageStock s = new VillageStock(1000);
        s.deposit(STICK, 40);
        // 故意把下游放在前面
        List<CatchUpPlanner.Producer> ps = List.of(
                new CatchUpPlanner.Producer("fire", Map.of(RAW, 4, STICK, 4), Map.of(PART, 4), 3, null, 0),
                new CatchUpPlanner.Producer("shape", Map.of(CLAY, 4), Map.of(RAW, 4), 3, null, 0),
                digger(3));
        CatchUpPlanner.plan(s, clayPit(48), ps, 1, 3, 1, 0, 1, Rations.none());
        assertEquals(12, s.count(PART));
        assertEquals(0, s.count(RAW), "不凭空多出陶坯");
        assertEquals(0, s.count(CLAY));
    }

    @Test
    void tc29b_pointRecoversWhileAway() {
        VillageStock s = new VillageStock(1000);
        CatchUpPlanner.Result r = CatchUpPlanner.plan(s, clayPit(0), List.of(digger(10)), 2, 3, 2, 0, 1, Rations.none());
        assertEquals(8, s.count(CLAY), "离开 2 日恢复 8");
        assertEquals(0, r.points().get("clay_0").remaining());
    }

    @Test
    void tc30_rationsShortNotNegative() {
        VillageStock s = new VillageStock(1000);
        s.deposit(BREAD, 20);
        CatchUpPlanner.Result r = CatchUpPlanner.plan(s, Map.of(), List.of(), 3, 3, 3, 10, 1,
                new Rations(Map.of(BREAD, 1.0), 24));
        assertEquals(0, s.count(BREAD));
        assertEquals(10, r.rationShort(), 1e-9);
    }

    @Test
    void tc31_zeroDaysNoop() {
        VillageStock s = new VillageStock(1000);
        CatchUpPlanner.Result r = CatchUpPlanner.plan(s, Map.of(), List.of(
                new CatchUpPlanner.Producer("p", Map.of(), Map.of(BREAD, 2), 1, null, 0)), 0, 3, 0, 5, 1,
                new Rations(Map.of(BREAD, 1.0), 0));
        assertEquals(0, s.total());
        assertEquals(0, r.rationShort(), 1e-9);
    }
}
