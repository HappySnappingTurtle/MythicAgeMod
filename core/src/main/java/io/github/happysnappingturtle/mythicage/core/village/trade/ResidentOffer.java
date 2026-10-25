package io.github.happysnappingturtle.mythicage.core.village.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.player.Age;

import java.util.ArrayList;
import java.util.List;

/**
 * 居民的一条交易（T19 设计文档 §11.3、§1.8 I1）：职业数据 {@code village_profession/<civ>/<职业>.json} 的 {@code trades}。
 *
 * @param sell     true = 居民卖给玩家（从村库出货）；false = 居民收购（收进村库）
 * @param count    一份的件数
 * @param price    一份的绿宝石数（卖：玩家付；收：居民付）
 * @param daily    这名居民每 MC 日最多做几份
 * @param minAge   村庄阶段
 * @param minorGod 次神特色商品：只在立过该次神祠的村庄出现；空串表示普通商品
 * @param imported 集市商人的杂货（村里不产）：不走村库，只受每日额度
 * @param festival 节庆商品：只在这个节日当天出现（T19 §1.10 J5）；空串表示平日商品
 * @param villageCap 每村每个节日最多卖几份（0 = 不限，只看每日额度）
 */
public record ResidentOffer(String id, boolean sell, String item, int count, int price, int daily, Age minAge, String minorGod,
                            boolean imported, String festival, int villageCap) {
    public ResidentOffer(String id, boolean sell, String item, int count, int price, int daily, Age minAge, String minorGod,
                         boolean imported) {
        this(id, sell, item, count, price, daily, minAge, minorGod, imported, "", 0);
    }

    public static List<ResidentOffer> parse(JsonObject trades) {
        List<ResidentOffer> out = new ArrayList<>();
        for (String side : new String[]{"sell", "buy"}) {
            if (!trades.has(side)) {
                continue;
            }
            JsonArray a = trades.getAsJsonArray(side);
            for (JsonElement e : a) {
                JsonObject o = e.getAsJsonObject();
                out.add(new ResidentOffer(o.get("id").getAsString(), side.equals("sell"), o.get("item").getAsString(),
                        o.has("count") ? o.get("count").getAsInt() : 1, o.get("price").getAsInt(),
                        o.has("daily") ? o.get("daily").getAsInt() : 8,
                        o.has("minAge") ? Age.byId(o.get("minAge").getAsString()).orElse(Age.ARCHAIC) : Age.ARCHAIC,
                        o.has("minorGod") ? o.get("minorGod").getAsString() : "",
                        o.has("source") && o.get("source").getAsString().equals("import"),
                        o.has("festival") ? o.get("festival").getAsString() : "",
                        o.has("villageCap") ? o.get("villageCap").getAsInt() : 0));
            }
        }
        return List.copyOf(out);
    }
}
