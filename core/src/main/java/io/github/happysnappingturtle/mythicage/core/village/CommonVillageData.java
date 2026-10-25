package io.github.happysnappingturtle.mythicage.core.village;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Map;

/**
 * 五文明共用的基础采集（T20 设计文档 §10.6，D5）：{@code village_civ/_common.json} 里的 {@code woodland}、{@code points}、
 * {@code naturalPoints} 合并进每个文明的数据，在解析成 {@link CivVillageData} 之前按 JSON 合并。
 * <ul>
 *   <li>{@code woodland}：逐字段合并，文明写了的字段覆盖共用的；文明写 {@code null} 就关掉；</li>
 *   <li>{@code points}、{@code naturalPoints}：逐条合并，文明写了同名条目整条覆盖；写 {@code null} 就去掉这一条；</li>
 *   <li>职业名单里的占位符 {@code @fallback} 换成文明自己的基础劳力职业（{@code residents.fallback}，默认 {@code <文明>:laborer}）。</li>
 * </ul>
 */
public final class CommonVillageData {
    /** 共用数据的文件名（不是一个文明）。 */
    public static final String FILE = "_common";
    public static final String FALLBACK = "@fallback";
    private static final List<String> MAPS = List.of("points", "naturalPoints");

    private CommonVillageData() {
    }

    public static JsonObject merge(JsonObject common, JsonObject civ, String civId) {
        JsonObject out = civ.deepCopy();
        if (common == null) {
            return out;
        }
        String fallback = civ.has("residents") && civ.getAsJsonObject("residents").has("fallback")
                ? civ.getAsJsonObject("residents").get("fallback").getAsString() : civId + ":laborer";
        if (common.has("woodland") && common.get("woodland").isJsonObject()) {
            if (!civ.has("woodland")) {
                out.add("woodland", common.getAsJsonObject("woodland").deepCopy());
            } else if (civ.get("woodland").isJsonObject()) {
                JsonObject w = common.getAsJsonObject("woodland").deepCopy();
                for (Map.Entry<String, JsonElement> e : civ.getAsJsonObject("woodland").entrySet()) {
                    w.add(e.getKey(), e.getValue());
                }
                out.add("woodland", w);
            }
        }
        if (out.has("woodland") && out.get("woodland").isJsonNull()) {
            out.remove("woodland");
        }
        for (String key : MAPS) {
            JsonObject merged = new JsonObject();
            if (common.has(key)) {
                for (Map.Entry<String, JsonElement> e : common.getAsJsonObject(key).entrySet()) {
                    merged.add(e.getKey(), e.getValue().deepCopy());
                }
            }
            if (civ.has(key)) {
                for (Map.Entry<String, JsonElement> e : civ.getAsJsonObject(key).entrySet()) {
                    if (e.getValue().isJsonNull()) {
                        merged.remove(e.getKey());
                    } else {
                        merged.add(e.getKey(), e.getValue().deepCopy());
                    }
                }
            }
            if (merged.isEmpty()) {
                out.remove(key);
            } else {
                out.add(key, merged);
            }
        }
        replaceFallback(out, fallback);
        return out;
    }

    /** 把 {@code professions} 数组里的 {@code @fallback} 换成文明的劳力职业。 */
    private static void replaceFallback(JsonObject o, String fallback) {
        if (o.has("woodland") && o.get("woodland").isJsonObject()) {
            replaceIn(o.getAsJsonObject("woodland"), fallback);
        }
        for (String key : MAPS) {
            if (o.has(key)) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(key).entrySet()) {
                    if (e.getValue().isJsonObject()) {
                        replaceIn(e.getValue().getAsJsonObject(), fallback);
                    }
                }
            }
        }
    }

    private static void replaceIn(JsonObject o, String fallback) {
        if (!o.has("professions") || !o.get("professions").isJsonArray()) {
            return;
        }
        JsonArray arr = new JsonArray();
        for (JsonElement e : o.getAsJsonArray("professions")) {
            arr.add(FALLBACK.equals(e.getAsString()) ? new JsonPrimitive(fallback) : e);
        }
        o.add("professions", arr);
    }
}
