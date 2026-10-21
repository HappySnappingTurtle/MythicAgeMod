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
    HAS_SHRINE,
    REISSUED_TODAY,
    /** 本村阶段不够（T11a）。 */
    STAGE_LOCKED,
    /** 本村没有该次神的祠（T11a）。 */
    NO_SHRINE,
    /** 此主神像不卖该主神 / 次神的商品（T11a）。 */
    WRONG_PATRON,
    /** 主神像：尚未被本村主神认可到古典（T11a）。 */
    NEED_APPROVAL,
    /** 主神像：已有这位主神的像（T11a，每位主神 1 座）。 */
    HAS_STATUE,
    /** 村庄指引：祭司正在回想附近的村庄（T13b，搜索在后台进行，找到才成交）。 */
    SEARCHING,
    /** 村庄指引：搜索半径内没有你没去过的同文明村庄（T13b，不扣钱）。 */
    NO_VILLAGE_NEARBY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String langKey() {
        return "mythic_age.trade.result." + id();
    }
}
