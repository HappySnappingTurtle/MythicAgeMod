package io.github.happysnappingturtle.mythicage.core.village.economy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 口粮（T19 设计文档 §9.3，V3-希腊-居民 §4.7）：按文明数据把食物折算成「份」。
 * 每人每天晚饭扣 1 份（节庆 2 份）；不够时不饿死，只记缺粮。卖给玩家时要保住口粮底线。
 *
 * @param portions 物品 → 每件折几份
 * @param floor    口粮底线（份），低于它暂停新项目、不能卖口粮
 */
public record Rations(Map<String, Double> portions, int floor) {
    public Rations {
        portions = Map.copyOf(portions);
    }

    public static Rations none() {
        return new Rations(Map.of(), 0);
    }

    public boolean isFood(String item) {
        return portions.containsKey(item);
    }

    /** 库存里一共有几份口粮。 */
    public double total(VillageStock stock) {
        double t = 0;
        for (Map.Entry<String, Double> e : portions.entrySet()) {
            t += stock.count(e.getKey()) * e.getValue();
        }
        return t;
    }

    public boolean belowFloor(VillageStock stock) {
        return total(stock) < floor;
    }

    /** 这件食物最多能卖几件而不跌破底线；不是口粮的物品返回库存数。 */
    public int sellable(VillageStock stock, String item) {
        Double v = portions.get(item);
        if (v == null || v <= 0) {
            return stock.count(item);
        }
        double spare = total(stock) - floor;
        if (spare <= 0) {
            return 0;
        }
        return (int) Math.min(stock.count(item), Math.floor(spare / v + 1e-9));
    }

    /**
     * 扣 {@code need} 份口粮：先扣存量最多的食物。返回没扣到的份数（缺口，≥ 0）。
     * 一件食物不拆开：剩下不足 1 件的零头按 1 件扣（多扣的份数不找零）。
     */
    public double consume(VillageStock stock, double need) {
        double left = need;
        List<Map.Entry<String, Double>> order = new ArrayList<>(portions.entrySet());
        order.sort((a, b) -> Double.compare(stock.count(b.getKey()) * b.getValue(), stock.count(a.getKey()) * a.getValue()));
        for (Map.Entry<String, Double> e : order) {
            if (left <= 1e-9) {
                break;
            }
            double v = e.getValue();
            if (v <= 0) {
                continue;
            }
            int want = (int) Math.ceil(left / v - 1e-9);
            int got = stock.take(e.getKey(), want, 0);
            left -= got * v;
        }
        return Math.max(0, left);
    }

    public static Rations parse(com.google.gson.JsonObject o) {
        Map<String, Double> p = new LinkedHashMap<>();
        if (o.has("portions")) {
            o.getAsJsonObject("portions").entrySet().forEach(e -> p.put(e.getKey(), e.getValue().getAsDouble()));
        }
        return new Rations(p, o.has("floor") ? o.get("floor").getAsInt() : 0);
    }
}
