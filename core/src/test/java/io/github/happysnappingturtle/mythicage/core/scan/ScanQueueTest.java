package io.github.happysnappingturtle.mythicage.core.scan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T8a-50～53。 */
class ScanQueueTest {
    private record Call(String key, int start, int count) {
    }

    @Test
    void tc50TwoTicksFor2700() {
        ScanQueue<String> q = new ScanQueue<>();
        q.submit("a", 2700);
        assertTrue(q.tick(2048, (k, s, c) -> { }).isEmpty());
        assertEquals(List.of("a"), q.tick(2048, (k, s, c) -> { }));
    }

    @Test
    void tc51Dedupe() {
        ScanQueue<String> q = new ScanQueue<>();
        q.submit("a", 2700);
        q.submit("a", 2700);
        q.submit("a", 2700);
        assertEquals(1, q.size());
    }

    @Test
    void tc52DirtyDuringScanRestarts() {
        ScanQueue<String> q = new ScanQueue<>();
        List<Call> calls = new ArrayList<>();
        q.submit("a", 2700);
        q.tick(2048, (k, s, c) -> calls.add(new Call(k, s, c)));
        q.submit("a", 2700);
        q.tick(2048, (k, s, c) -> calls.add(new Call(k, s, c)));
        List<String> done = q.tick(2048, (k, s, c) -> calls.add(new Call(k, s, c)));
        assertEquals(List.of(new Call("a", 0, 2048), new Call("a", 0, 2048), new Call("a", 2048, 652)), calls);
        assertEquals(List.of("a"), done);
    }

    @Test
    void tc53BudgetSharedInOrder() {
        ScanQueue<String> q = new ScanQueue<>();
        q.submit("a", 2700);
        q.submit("b", 2700);
        q.submit("c", 2700);
        List<String> order = new ArrayList<>();
        for (int t = 0; t < 10 && q.size() > 0; t++) {
            int[] used = {0};
            order.addAll(q.tick(2048, (k, s, c) -> used[0] += c));
            assertTrue(used[0] <= 2048, "每刻不超过预算");
        }
        assertEquals(List.of("a", "b", "c"), order);
    }
}
