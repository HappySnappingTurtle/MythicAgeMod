package io.github.happysnappingturtle.mythicage.core.trade;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;

/**
 * 交易的纯判定（T9 设计文档 §5 第 1 步，背包与物品价格除外）。按固定顺序检查，返回第一个不满足的原因：
 * 地点 → 时代 → 次神 → 价格是否变化 → 每日限购 → 恩宠。
 */
public final class TradeRules {
    private TradeRules() {
    }

    /**
     * @param price         按当前折扣算出的恩宠价（物品价商品传 0）
     * @param expectedPrice 客户端界面上显示的价格
     */
    public static TradeReason check(PlayerMythData data, Civilization civ, TradeOffer offer, TradeOffer.Venue venue,
                                    long price, long expectedPrice, long today) {
        if (!offer.venues().contains(venue)) {
            return TradeReason.WRONG_VENUE;
        }
        if (!data.civ(civ).age().atLeast(offer.minAge())) {
            return TradeReason.AGE_LOCKED;
        }
        if (!offer.minorGod().isEmpty() && !data.civ(civ).worships(offer.minorGod())) {
            return TradeReason.MINOR_GOD_LOCKED;
        }
        if (price != expectedPrice) {
            return TradeReason.PRICE_CHANGED;
        }
        if (offer.limited() && data.tradeDaily().count(offer.key(civ), today) >= offer.dailyLimit()) {
            return TradeReason.DAILY_LIMIT;
        }
        if (offer.price().isFavor() && data.civ(civ).favor() < price) {
            return TradeReason.NOT_ENOUGH_FAVOR;
        }
        return TradeReason.OK;
    }

    /**
     * 该玩家在某村庄祭司处购买此商品适用的折扣：供奉的次神折扣 + 村庄主神折扣（T10a）。
     *
     * @param villagePatron 卖家所在村庄的主神；不在村庄或祭坛兑换时为空
     */
    public static java.util.List<Double> discounts(PlayerMythData data, Civilization civ, TradeOffer offer, String villagePatron) {
        java.util.List<Double> out = new java.util.ArrayList<>();
        offer.minorGodDiscount().forEach((god, d) -> {
            if (data.civ(civ).worships(god)) {
                out.add(d);
            }
        });
        if (!offer.patron().isEmpty() && offer.patron().equals(villagePatron) && offer.patronDiscount() > 0) {
            out.add(offer.patronDiscount());
        }
        return out;
    }

    /** 今日剩余次数；不限购时为 −1。 */
    public static int remainingToday(PlayerMythData data, Civilization civ, TradeOffer offer, long today) {
        return offer.limited() ? Math.max(0, offer.dailyLimit() - data.tradeDaily().count(offer.key(civ), today)) : -1;
    }
}
