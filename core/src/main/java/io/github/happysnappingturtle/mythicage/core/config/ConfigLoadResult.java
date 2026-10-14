package io.github.happysnappingturtle.mythicage.core.config;

import java.util.List;

/**
 * 一次加载的结果。
 *
 * @param value        可以直接使用的配置值（永远不为 null）
 * @param issues       发现的问题，按发现顺序
 * @param usedFallback true 表示整份文件不可用，value 是上一次有效配置或默认值
 */
public record ConfigLoadResult<T>(T value, List<ConfigIssue> issues, boolean usedFallback) {
    public ConfigLoadResult {
        issues = List.copyOf(issues);
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(i -> i.severity() == ConfigIssue.Severity.ERROR);
    }
}
