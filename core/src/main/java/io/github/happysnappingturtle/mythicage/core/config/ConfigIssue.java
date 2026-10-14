package io.github.happysnappingturtle.mythicage.core.config;

/**
 * 加载或热更配置时发现的一个问题。
 *
 * @param path 出问题的键路径，例如 {@code favor.idle.graceTicks}；整份文件的问题用空字符串
 */
public record ConfigIssue(Severity severity, String path, String message) {
    public enum Severity {
        /** 某个字段不合法，已回退到默认值；其余字段照常生效。 */
        WARN,
        /** 整份文件不可用，已保留上一次有效配置（或使用默认值）。 */
        ERROR
    }

    public static ConfigIssue warn(String path, String message) {
        return new ConfigIssue(Severity.WARN, path, message);
    }

    public static ConfigIssue error(String path, String message) {
        return new ConfigIssue(Severity.ERROR, path, message);
    }

    @Override
    public String toString() {
        return severity + (path.isEmpty() ? "" : " [" + path + "]") + ": " + message;
    }
}
