package io.github.happysnappingturtle.mythicage.core.trade;

import java.util.Locale;

/** 交易与祝福请求的结果原因码（T9 设计文档 §5）。界面按 {@link #langKey()} 显示。 */
public enum TradeReason {
    OK,
    DUPLICATE,
    NPC_GONE,
    UNKNOWN_OFFER,
    WRONG_VENUE,
    AGE_LOCKED,
    MINOR_GOD_LOCKED,
    PRICE_CHANGED,
    DAILY_LIMIT,
    NOT_ENOUGH_FAVOR,
    NOT_ENOUGH_ITEMS,
    INVENTORY_FULL,
    ALREADY_BLESSED,
    NOT_BLESSED,
    HAS_ALTAR,
    REISSUED_TODAY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String langKey() {
        return "mythic_age.trade.result." + id();
    }
}
