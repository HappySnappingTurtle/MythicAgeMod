package io.github.happysnappingturtle.mythicage.core.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * 把配置文本按 schema 解析为配置值。规则见 T3 设计文档 §3。
 */
public final class ConfigLoader {
    public static final String SCHEMA_VERSION_KEY = "schemaVersion";

    private ConfigLoader() {
    }

    /**
     * @param lastValid 整份文件不可用时返回的值；为 null 时返回默认值
     */
    public static <T> ConfigLoadResult<T> load(ConfigSchema<T> schema, String text, T lastValid) {
        List<ConfigIssue> issues = new ArrayList<>();
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                throw new JsonParseException("顶层应为 JSON 对象");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            issues.add(ConfigIssue.error("", "整份文件无法解析：" + e.getMessage()
                    + (lastValid != null ? "；已保留上一次有效配置" : "；已使用默认值")));
            return new ConfigLoadResult<>(lastValid != null ? lastValid : schema.defaults(), issues, true);
        }

        ObjectReader reader = ObjectReader.root(root, issues);
        reader.markConsumed(SCHEMA_VERSION_KEY);
        readVersion(schema, root.get(SCHEMA_VERSION_KEY), issues);

        T base = schema.base(reader, issues);
        T value = schema.read(reader, base);
        reader.reportUnknownKeys();
        return new ConfigLoadResult<>(value, issues, false);
    }

    /** 默认文件内容：schemaVersion 加默认值，带缩进。 */
    public static <T> String defaultFileContent(ConfigSchema<T> schema) {
        return toPrettyJson(schema, schema.defaults());
    }

    /** 当前值的规范化 JSON（带 schemaVersion），用于 show 命令和往返测试。 */
    public static <T> String toPrettyJson(ConfigSchema<T> schema, T value) {
        JsonObject out = new JsonObject();
        out.addProperty(SCHEMA_VERSION_KEY, schema.currentVersion());
        schema.toJson(value).entrySet().forEach(e -> out.add(e.getKey(), e.getValue()));
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(out) + "\n";
    }

    private static void readVersion(ConfigSchema<?> schema, JsonElement element, List<ConfigIssue> issues) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (!(element.isJsonPrimitive() && ((JsonPrimitive) element).isNumber())) {
            issues.add(ConfigIssue.warn(SCHEMA_VERSION_KEY, "应为整数，按版本 1 读取"));
            return;
        }
        int version = element.getAsInt();
        if (version > schema.currentVersion()) {
            issues.add(ConfigIssue.warn(SCHEMA_VERSION_KEY, "来自更新版本（" + version + "）的配置，按当前版本 "
                    + schema.currentVersion() + " 尽力读取"));
        }
        // 低于当前版本时执行迁移；第 1 版尚无迁移。
    }
}
