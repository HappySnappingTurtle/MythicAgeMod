package io.github.happysnappingturtle.mythicage.core.power;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 召唤管理「临时效果」读的账（TC-T24-29a／b）。 */
class PowerBookkeepingViewTest {
    static final UUID A = new UUID(0, 1);
    static final UUID B = new UUID(0, 2);

    @Test
    void transientEntriesPerOwnerInOrder() {
        TransientBudget budget = new TransientBudget();
        budget.register(A, List.of("a1", "a2"), 0, 100, 16, 96);
        budget.register(B, List.of("b1"), 0, 100, 16, 96);
        budget.register(A, List.of("a3"), 10, 200, 16, 96);
        List<TransientBudget.Entry> mine = budget.entries(A);
        assertEquals(List.of("a1", "a2", "a3"), mine.stream().map(TransientBudget.Entry::id).toList());
        assertEquals(210, mine.get(2).expiresAt());
        assertTrue(budget.entries(new UUID(9, 9)).isEmpty());
    }

    @Test
    void productGroupsPerOwner() {
        ProductGroups g = new ProductGroups();
        g.replace(A, "mythic_age:sentinel", List.of("s1", "s2"));
        g.replace(A, "mythic_age:lure", List.of("l1"));
        g.replace(B, "mythic_age:lure", List.of("x"));
        Map<String, List<String>> mine = g.groups(A);
        assertEquals(List.of("mythic_age:lure", "mythic_age:sentinel"), List.copyOf(mine.keySet()), "按神力 id 排序");
        assertEquals(List.of("s1", "s2"), mine.get("mythic_age:sentinel"));
        g.forget("l1");
        assertEquals(1, g.groups(A).size(), "空组不列");
    }
}
