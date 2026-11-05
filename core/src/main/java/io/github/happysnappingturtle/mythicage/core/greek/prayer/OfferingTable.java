package io.github.happysnappingturtle.mythicage.core.greek.prayer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 供奉的供品表（GA-4 设计文档 §2.5），来自数据包 {@code data/<ns>/mythic_age/offering/<文明>.json}。
 * 整份不可用（版本、加成不对）时抛出；单项不可用时跳过并记入 warnings。
 *
 * @param bonus 献过供后当天这座神坛的祈祷恩宠加成 O（0–2）
 * @param items 物品 id → 一次要献的数量与献祭类别
 */
public record OfferingTable(double bonus, Map<String, Offering> items) {
    public static final int SCHEMA_VERSION = 1;
    public static final double MAX_BONUS = 2;
    public static final OfferingTable EMPTY = new OfferingTable(0, Map.of());

    /** @param kind 献祭类别（spondai、thysia、pelanos、aparchai），只用于显示与说明 */
    public record Offering(String item, int count, String kind) {
    }

    public OfferingTable {
        items = Map.copyOf(items);
    }

    public static final class OfferingTableException extends RuntimeException {
        public OfferingTableException(String message) {
            super(message);
        }
    }

    public Optional<Offering> of(String itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    public static OfferingTable parse(String json, List<String> warnings) {
        JsonElement el = JsonParser.parseString(json);
        if (!el.isJsonObject()) {
            throw new OfferingTableException("供品表应为对象");
        }
        JsonObject root = el.getAsJsonObject();
        if (!root.has("schemaVersion") || root.get("schemaVersion").getAsInt() != SCHEMA_VERSION) {
            throw new OfferingTableException("schemaVersion 应为 " + SCHEMA_VERSION);
        }
        double bonus = root.has("bonus") ? root.get("bonus").getAsDouble() : -1;
        if (Double.isNaN(bonus) || bonus < 0 || bonus > MAX_BONUS) {
            throw new OfferingTableException("bonus 应在 0–" + MAX_BONUS + "：" + bonus);
        }
        Map<String, Offering> items = new LinkedHashMap<>();
        if (root.has("items") && root.get("items").isJsonArray()) {
            int i = 0;
            for (JsonElement e : root.getAsJsonArray("items")) {
                String at = "items[" + i++ + "]";
                if (!e.isJsonObject() || !e.getAsJsonObject().has("item")) {
                    warnings.add(at + "：缺少 item，已跳过");
                    continue;
                }
                JsonObject o = e.getAsJsonObject();
                String item = o.get("item").getAsString();
                int count = o.has("count") ? o.get("count").getAsInt() : 1;
                if (count < 1) {
                    warnings.add(at + "：count 应 ≥ 1，已跳过 " + item);
                    continue;
                }
                if (items.containsKey(item)) {
                    warnings.add(at + "：重复的 " + item + "，保留第一条");
                    continue;
                }
                items.put(item, new Offering(item, count, o.has("kind") ? o.get("kind").getAsString() : ""));
            }
        }
        return new OfferingTable(bonus, items);
    }
}
