package io.github.happysnappingturtle.mythicage.core.village.trade;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.economy.Rations;
import io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 居民交易的规则（T19 设计文档 §11.3、§1.8 I2–I5）：纯函数。文明级配置在 {@code village_civ/<civ>.json} 的 {@code trade}。
 *
 * @param devoteeDiscount 次神特色商品对供奉者的折扣（0.9 = 九折）
 * @param godDiscounts    某位次神的供奉者在所有居民处的折扣（赫尔墨斯 0.95）
 * @param f7ExemptGod     供奉这位次神的玩家不受 F7 减半（赫拉）
 */
public record ResidentTradeRules(double devoteeDiscount, Map<String, Double> godDiscounts, String f7ExemptGod) {
    public ResidentTradeRules {
        godDiscounts = Map.copyOf(godDiscounts);
    }

    public static ResidentTradeRules none() {
        return new ResidentTradeRules(1, Map.of(), "");
    }

    public static ResidentTradeRules parse(JsonObject o) {
        Map<String, Double> gods = new LinkedHashMap<>();
        if (o.has("godDiscounts")) {
            o.getAsJsonObject("godDiscounts").entrySet().forEach(e -> gods.put(e.getKey(), e.getValue().getAsDouble()));
        }
        return new ResidentTradeRules(o.has("devoteeDiscount") ? o.get("devoteeDiscount").getAsDouble() : 1, gods,
                o.has("f7ExemptGod") ? o.get("f7ExemptGod").getAsString() : "");
    }

    /** 这条商品此刻在这个村庄出不出现：阶段已到；次神商品要本村立过该次神祠。 */
    public static boolean visible(ResidentOffer o, Age villageStage, Set<String> shrineGods) {
        return visible(o, villageStage, shrineGods, "");
    }

    /** {@code festivalToday}：今天的节日（空串 = 不过节）；节庆商品只在当天出现。 */
    public static boolean visible(ResidentOffer o, Age villageStage, Set<String> shrineGods, String festivalToday) {
        return villageStage.atLeast(o.minAge()) && (o.minorGod().isEmpty() || shrineGods.contains(o.minorGod()))
                && (o.festival().isEmpty() || o.festival().equals(festivalToday));
    }

    /**
     * 玩家实付的绿宝石：卖出的商品按供奉的次神打折，折扣连乘；按原版村民「村庄英雄」折扣的惯例，原价 ≥ 2 时
     * 只要有折扣就至少便宜 1 颗（居民的价都只有几颗绿宝石，按比例取整等于没打折），最低 1 颗。收购价不打折。
     */
    public int price(ResidentOffer o, Set<String> playerGods) {
        if (!o.sell()) {
            return o.price();
        }
        double f = 1;
        if (!o.minorGod().isEmpty() && playerGods.contains(o.minorGod())) {
            f *= devoteeDiscount;
        }
        for (String g : playerGods) {
            f *= godDiscounts.getOrDefault(g, 1.0);
        }
        if (f >= 1 - 1e-9 || o.price() < 2) {
            return o.price();
        }
        int off = Math.max(1, (int) Math.round(o.price() * (1 - f)));
        return Math.max(1, o.price() - off);
    }

    /** 今天这名居民这条商品最多做几份：当天祈祷过减半（向下取整，至少 1，F7），供奉豁免次神的玩家不减。 */
    public int dailyCap(ResidentOffer o, boolean prayedToday, Set<String> playerGods) {
        if (!prayedToday || (!f7ExemptGod.isEmpty() && playerGods.contains(f7ExemptGod))) {
            return o.daily();
        }
        return Math.max(1, o.daily() / 2);
    }

    /** 村库还能卖出几份：杂货不看库存；口粮卖到底线为止。 */
    public static int stockShares(ResidentOffer o, VillageStock stock, Rations rations) {
        if (o.imported()) {
            return Integer.MAX_VALUE;
        }
        return rations.sellable(stock, o.item()) / Math.max(1, o.count());
    }

    /** 村库收不收得下一份收购。 */
    public static boolean canAccept(ResidentOffer o, VillageStock stock) {
        return stock.free() >= o.count();
    }
}
