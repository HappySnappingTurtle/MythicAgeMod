package io.github.happysnappingturtle.mythicage.core.village.migrate;

import java.util.Map;

/**
 * 新移民的职业（T19 设计文档 §10.2、§1.6 H2）：纯函数。各职业「岗位 − 现有人数」取缺口最大的（同缺口按职业 id），
 * 都没缺口就是基础劳力。岗位来自已建建筑的 {@code jobs}（未开放阶段的职业由调用方去掉）。
 */
public final class MigrationPlanner {
    private MigrationPlanner() {
    }

    public static String profession(Map<String, Integer> jobs, Map<String, Integer> have, String fallback) {
        String best = null;
        int bestGap = 0;
        for (Map.Entry<String, Integer> e : new java.util.TreeMap<>(jobs).entrySet()) {
            int gap = e.getValue() - have.getOrDefault(e.getKey(), 0);
            if (gap > bestGap) {
                bestGap = gap;
                best = e.getKey();
            }
        }
        return best != null ? best : fallback;
    }
}
