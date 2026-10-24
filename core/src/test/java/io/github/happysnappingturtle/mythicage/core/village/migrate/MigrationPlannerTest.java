package io.github.happysnappingturtle.mythicage.core.village.migrate;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** T19 单元测试 TC-T19-38：新移民按空缺岗位分职业。 */
class MigrationPlannerTest {
    @Test
    void tc38_fillsLargestGapThenLaborer() {
        Map<String, Integer> jobs = Map.of("greek:potter", 2, "greek:mason", 1);
        assertEquals("greek:mason", MigrationPlanner.profession(jobs, Map.of("greek:potter", 1), "greek:laborer"),
                "陶坊 2 岗有 1 人、石作坊 1 岗有 0 人：同缺口按 id，石作匠在前");
        assertEquals("greek:potter", MigrationPlanner.profession(Map.of("greek:potter", 3, "greek:mason", 1),
                Map.of("greek:potter", 1), "greek:laborer"), "缺口大的先补");
        assertEquals("greek:laborer", MigrationPlanner.profession(jobs, Map.of("greek:potter", 2, "greek:mason", 1),
                "greek:laborer"), "全满是劳力");
        assertEquals("greek:laborer", MigrationPlanner.profession(Map.of(), Map.of(), "greek:laborer"));
    }
}
