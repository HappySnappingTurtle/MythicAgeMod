package io.github.happysnappingturtle.mythicage.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按键读取一个 JSON 对象的字段：校验类型与范围，不合法时回退到给定默认值并记录问题。
 * <p>
 * 读取过的键会被记录；{@link #reportUnknownKeys()} 把没有被读取的键报告为未知字段。
 * 越界时回退到默认值而不是截断到边界，避免悄悄变成服主没有写过的值。
 */
public final class ObjectReader {
    private final JsonObject json;
    private final String path;
    private final List<ConfigIssue> issues;
    private final Set<String> consumed = new HashSet<>();
    private final List<ObjectReader> children = new ArrayList<>();

    ObjectReader(JsonObject json, String path, List<ConfigIssue> issues) {
        this.json = json;
        this.path = path;
        this.issues = issues;
    }

    /** 以给定 JSON 对象为根创建读取器。 */
    public static ObjectReader root(JsonObject json, List<ConfigIssue> issues) {
        return new ObjectReader(json, "", issues);
    }

    /** 读取子对象。缺失时返回空读取器（所有字段取默认值）；不是对象时告警后同样返回空读取器。 */
    public ObjectReader object(String key) {
        JsonElement element = take(key);
        JsonObject child = null;
        if (element != null) {
            if (element.isJsonObject()) {
                child = element.getAsJsonObject();
            } else {
                warn(key, "应为对象，已整体使用默认值");
            }
        }
        ObjectReader reader = new ObjectReader(child, pathOf(key), issues);
        children.add(reader);
        return reader;
    }

    public boolean bool(String key, boolean fallback) {
        JsonElement element = take(key);
        if (element == null) {
            return fallback;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        warn(key, "应为 true 或 false，已使用默认值 " + fallback);
        return fallback;
    }

    /** 锁定字段：只能是 {@code locked}，写了其他值时强制改回并告警。 */
    public boolean lockedBool(String key, boolean locked) {
        JsonElement element = take(key);
        if (element == null) {
            return locked;
        }
        if (!(element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) || element.getAsBoolean() != locked) {
            warn(key, "该字段只能为 " + locked + "，已强制为 " + locked);
        }
        return locked;
    }

    public int intIn(String key, int fallback, int min, int max) {
        return (int) longIn(key, fallback, min, max);
    }

    public long longIn(String key, long fallback, long min, long max) {
        JsonElement element = take(key);
        if (element == null) {
            return fallback;
        }
        BigDecimal number = numberOf(element);
        if (number == null || number.stripTrailingZeros().scale() > 0) {
            warn(key, "应为整数，已使用默认值 " + fallback);
            return fallback;
        }
        if (number.compareTo(BigDecimal.valueOf(min)) < 0 || number.compareTo(BigDecimal.valueOf(max)) > 0) {
            warn(key, "超出范围 [" + min + ", " + max + "]，已使用默认值 " + fallback);
            return fallback;
        }
        return number.longValueExact();
    }

    public double doubleIn(String key, double fallback, double min, double max) {
        JsonElement element = take(key);
        if (element == null) {
            return fallback;
        }
        BigDecimal number = numberOf(element);
        if (number == null) {
            warn(key, "应为数字，已使用默认值 " + fallback);
            return fallback;
        }
        double value = number.doubleValue();
        if (Double.isNaN(value) || value < min || value > max) {
            warn(key, "超出范围 [" + min + ", " + max + "]，已使用默认值 " + fallback);
            return fallback;
        }
        return value;
    }

    /** 读取枚举：{@code names} 是 JSON 中的合法写法到枚举值的映射。 */
    public <E> E choice(String key, E fallback, Map<String, E> names) {
        JsonElement element = take(key);
        if (element == null) {
            return fallback;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            E value = names.get(element.getAsString());
            if (value != null) {
                return value;
            }
        }
        warn(key, "不是合法值，可选 " + names.keySet().stream().sorted().toList() + "，已使用默认值");
        return fallback;
    }

    /** 本对象当前有哪些键（不标记为已读；用于读取「键 → 值」表）。缺失对象时为空。 */
    public java.util.List<String> keys() {
        return json == null ? java.util.List.of() : java.util.List.copyOf(json.keySet());
    }

    /** 该键存在且是 JSON 对象。 */
    public boolean isObject(String key) {
        JsonElement e = peek(key);
        return e != null && e.isJsonObject();
    }

    /** 读取非空字符串；缺失或类型不对时告警并返回 fallback。 */
    public String string(String key, String fallback) {
        JsonElement element = take(key);
        if (element == null) {
            return fallback;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && !element.getAsString().isEmpty()) {
            return element.getAsString();
        }
        warn(key, "应为非空字符串，已使用默认值 " + fallback);
        return fallback;
    }

    /** 记录一条与本对象某个键相关的告警（供 schema 做跨字段校验）。 */
    public void warn(String key, String message) {
        issues.add(ConfigIssue.warn(pathOf(key), message));
    }

    /** 递归报告所有没有被读取的键。 */
    public void reportUnknownKeys() {
        if (json != null) {
            for (String key : json.keySet()) {
                if (!consumed.contains(key)) {
                    warn(key, "未知字段，已忽略");
                }
            }
        }
        for (ObjectReader child : children) {
            child.reportUnknownKeys();
        }
    }

    /** 标记某个键已被处理（例如 schemaVersion、preset），不再报告为未知字段。 */
    void markConsumed(String key) {
        consumed.add(key);
    }

    JsonElement peek(String key) {
        return json == null ? null : json.get(key);
    }

    private JsonElement take(String key) {
        consumed.add(key);
        if (json == null) {
            return null;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? null : element;
    }

    private static BigDecimal numberOf(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            return null;
        }
        try {
            return primitive.getAsBigDecimal();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String pathOf(String key) {
        return path.isEmpty() ? key : path + "." + key;
    }
}
