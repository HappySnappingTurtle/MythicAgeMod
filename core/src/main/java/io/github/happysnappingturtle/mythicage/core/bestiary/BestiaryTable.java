package io.github.happysnappingturtle.mythicage.core.bestiary;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 图鉴点亮表 {@code bestiary/<文明>.json}（T24 §6.3）：哪些实体类型对应哪个条目。召唤版、野外版、Boss 版的同一种神话生物
 * 共用一个条目。格式：{@code {"entries": {"greek/minotaur": {"entities": ["mythic_age:minotaur", ...]}}}}。
 */
public final class BestiaryTable {
    private final Map<String, String> byEntity;
    private final List<String> entries;

    private BestiaryTable(Map<String, String> byEntity, List<String> entries) {
        this.byEntity = Map.copyOf(byEntity);
        this.entries = List.copyOf(entries);
    }

    public static BestiaryTable empty() {
        return new BestiaryTable(Map.of(), List.of());
    }

    /** 读表；条目名不合法、实体重复、条目没有实体时抛出带位置的异常。 */
    public static BestiaryTable parse(JsonObject root, String source) {
        if (!root.has("entries") || !root.get("entries").isJsonObject()) {
            throw new IllegalArgumentException(source + "：缺少 entries");
        }
        Map<String, String> byEntity = new LinkedHashMap<>();
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("entries").entrySet()) {
            String entry = e.getKey();
            if (!entry.matches("[a-z0-9_]+/[a-z0-9_]+")) {
                throw new IllegalArgumentException(source + "：条目名「" + entry + "」应为 <文明>/<名字>");
            }
            JsonObject o = e.getValue().getAsJsonObject();
            if (!o.has("entities") || o.getAsJsonArray("entities").isEmpty()) {
                throw new IllegalArgumentException(source + "：条目 " + entry + " 没有实体");
            }
            for (JsonElement x : o.getAsJsonArray("entities")) {
                String id = x.getAsString();
                String old = byEntity.put(id, entry);
                if (old != null) {
                    throw new IllegalArgumentException(source + "：实体 " + id + " 同时属于 " + old + " 和 " + entry);
                }
            }
            entries.add(entry);
        }
        return new BestiaryTable(byEntity, entries);
    }

    /** 合并几张表（各文明一张）；实体重复时抛出。 */
    public static BestiaryTable merge(List<BestiaryTable> tables) {
        Map<String, String> byEntity = new LinkedHashMap<>();
        List<String> entries = new ArrayList<>();
        for (BestiaryTable t : tables) {
            t.byEntity.forEach((k, v) -> {
                String old = byEntity.put(k, v);
                if (old != null && !old.equals(v)) {
                    throw new IllegalArgumentException("实体 " + k + " 同时属于 " + old + " 和 " + v);
                }
            });
            entries.addAll(t.entries);
        }
        return new BestiaryTable(byEntity, entries);
    }

    public Optional<String> entryOf(String entityType) {
        return Optional.ofNullable(byEntity.get(entityType));
    }

    public List<String> entries() {
        return entries;
    }

    public Map<String, String> byEntity() {
        return byEntity;
    }
}
