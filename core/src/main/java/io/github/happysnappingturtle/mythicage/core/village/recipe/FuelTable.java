package io.github.happysnappingturtle.mythicage.core.village.recipe;

import io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 燃料单位（V3-希腊-居民 §4.1）：例如橄榄木棍 4 件 = 原木 1 件 = 木炭 1 件 = 1 单位。
 * 配方的 {@code fuel} 按单位要求；托管时优先用「一件就够一单位」的燃料，再用零碎的木棍。
 *
 * @param perUnit 物品 → 几件凑 1 单位
 */
public record FuelTable(Map<String, Integer> perUnit) {
    public FuelTable {
        perUnit = Map.copyOf(perUnit);
    }

    public static FuelTable none() {
        return new FuelTable(Map.of());
    }

    /** 从库存里挑出凑够 {@code units} 单位的燃料（不扣库存）；凑不够返回 empty。 */
    public Optional<Map<String, Integer>> pick(VillageStock stock, int units) {
        if (units <= 0) {
            return Optional.of(Map.of());
        }
        List<Map.Entry<String, Integer>> order = new ArrayList<>(perUnit.entrySet());
        order.sort(Map.Entry.comparingByValue());
        Map<String, Integer> take = new LinkedHashMap<>();
        int left = units;
        for (Map.Entry<String, Integer> e : order) {
            if (left == 0) {
                break;
            }
            int per = Math.max(1, e.getValue());
            int canUnits = stock.count(e.getKey()) / per;
            int use = Math.min(canUnits, left);
            if (use > 0) {
                take.put(e.getKey(), use * per);
                left -= use;
            }
        }
        return left == 0 ? Optional.of(take) : Optional.empty();
    }

    /** 库存折合多少燃料单位。 */
    public int units(VillageStock stock) {
        int u = 0;
        for (Map.Entry<String, Integer> e : perUnit.entrySet()) {
            u += stock.count(e.getKey()) / Math.max(1, e.getValue());
        }
        return u;
    }
}
