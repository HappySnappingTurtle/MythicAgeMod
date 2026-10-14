package io.github.happysnappingturtle.mythicage.core.config;

import com.google.gson.JsonObject;

import java.util.List;

/**
 * 一份配置文件的结构定义。每份配置一个实现。
 *
 * @param <T> 不可变的配置值类型（record 树），必须正确实现 equals
 */
public interface ConfigSchema<T> {
    /** 文件名（不含扩展名），例如 {@code runtime}。 */
    String name();

    /** 当前 schema 版本，写在文件的 {@code schemaVersion} 字段。 */
    int currentVersion();

    /** 全部字段的默认值。 */
    T defaults();

    /**
     * 选出本次读取的基准值（每个字段缺失或不合法时回退到它）。默认就是 {@link #defaults()}；
     * 支持预设的配置（如 runtime.json 的 {@code preset}）在这里读取预设键并返回对应基准。
     */
    default T base(ObjectReader root, List<ConfigIssue> issues) {
        return defaults();
    }

    /** 按 schema 读取全部字段。不合法的字段回退到 {@code base} 中的对应值。 */
    T read(ObjectReader root, T base);

    /** 把配置值写成 JSON（用于生成默认文件和 {@code show} 命令）；不含 schemaVersion。 */
    JsonObject toJson(T value);
}
