package io.github.happysnappingturtle.mythicage.core.village;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 村庄阶段提升前后的差（TC-T24-24、25）。 */
class StageChangesTest {
    @Test
    void tc24OnlyChanged() {
        Map<String, Integer> before = new LinkedHashMap<>();
        before.put("population", 4);
        before.put("guards", 1);
        before.put("storage", 256);
        Map<String, Integer> after = new LinkedHashMap<>(before);
        after.put("population", 6);
        after.put("relic", 1);
        List<StageChanges.Change> c = StageChanges.diff(before, after);
        assertEquals(List.of(new StageChanges.Change("population", 4, 6), new StageChanges.Change("relic", 0, 1)), c);
    }

    @Test
    void tc25NothingChanged() {
        assertTrue(StageChanges.diff(Map.of("population", 4), Map.of("population", 4)).isEmpty());
        assertTrue(StageChanges.diff(Map.of(), Map.of()).isEmpty());
    }
}
