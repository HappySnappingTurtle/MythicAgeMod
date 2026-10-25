package io.github.happysnappingturtle.mythicage.core.village.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文明 JSON 的 {@code quests} 段（T21 设计文档 §10.2）：哪些本职缺料值得发任务、报酬表、贡献权重。
 * 没写的字段用 {@link #DEFAULT}；没有这一段的文明整段用默认值（只发在建项目缺料与口粮不足，修复、护送、救援照常）。
 *
 * @param supplyItems  职业本职缺料里发任务的物品及定量
 * @param prices       每件的绿宝石数（物品或 {@code #组}）；没登记的用 {@code defaultPrice}
 * @param repairPrice  修 1 格的绿宝石数
 * @param escortReward 护送成功的绿宝石定额
 * @param rescueReward 救援的绿宝石定额（D5：默认 0）
 */
public record QuestCivRules(Map<String, Integer> supplyItems, Map<String, Double> prices, double defaultPrice,
                            double repairPrice, int escortReward, int rescueReward, Contribution contribution) {
    /** 贡献权重：交 1 件、修 1 格、护送 1 次、救援 1 次各加多少。 */
    public record Contribution(int item, int repairCell, int escort, int rescue) {
        public static final Contribution DEFAULT = new Contribution(1, 1, 5, 3);
    }

    public static final QuestCivRules DEFAULT = new QuestCivRules(Map.of(), Map.of(), 0.25, 0.125, 3, 0,
            Contribution.DEFAULT);

    public QuestCivRules {
        supplyItems = Map.copyOf(supplyItems);
        prices = Map.copyOf(prices);
    }

    public double price(String key) {
        return prices.getOrDefault(key, defaultPrice);
    }

    public static QuestCivRules parse(JsonObject o) {
        QuestCivRules d = DEFAULT;
        Map<String, Integer> supply = new LinkedHashMap<>();
        if (o.has("supplyItems")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("supplyItems").entrySet()) {
                supply.put(e.getKey(), Math.max(1, e.getValue().getAsInt()));
            }
        }
        Map<String, Double> prices = new LinkedHashMap<>();
        double def = d.defaultPrice();
        if (o.has("prices")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("prices").entrySet()) {
                double v = Math.max(0, e.getValue().getAsDouble());
                if (e.getKey().equals("default")) {
                    def = v;
                } else {
                    prices.put(e.getKey(), v);
                }
            }
        }
        Contribution c = d.contribution();
        if (o.has("contribution")) {
            JsonObject co = o.getAsJsonObject("contribution");
            c = new Contribution(integer(co, "item", c.item()), integer(co, "repairCell", c.repairCell()),
                    integer(co, "escort", c.escort()), integer(co, "rescue", c.rescue()));
        }
        return new QuestCivRules(supply, prices, def,
                o.has("repairPrice") ? Math.max(0, o.get("repairPrice").getAsDouble()) : d.repairPrice(),
                integer(o, "escortReward", d.escortReward()), integer(o, "rescueReward", d.rescueReward()), c);
    }

    private static int integer(JsonObject o, String k, int def) {
        return o.has(k) ? Math.max(0, o.get(k).getAsInt()) : def;
    }
}
