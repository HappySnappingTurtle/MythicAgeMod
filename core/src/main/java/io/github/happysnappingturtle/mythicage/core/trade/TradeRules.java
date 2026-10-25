package io.github.happysnappingturtle.mythicage.core.trade;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 交易的纯判定（T9 设计文档 §5 第 1 步，背包与物品价格除外；T11a 设计文档 §5 加入兑换地点）。
 * <ul>
 *   <li>神力、召唤：本村主神（或此像主神）的远古神力；次神神力要求本村立了该次神的祠（或该次神属于此像主神），
 *       并且玩家时代够、供奉该次神；</li>
 *   <li>其他商品：只在村庄祭司处，看本村阶段（不看玩家时代）；带次神的只在立了该次神祠的村庄出售，对所有人。</li>
 * </ul>
 */
public final class TradeRules {
    private TradeRules() {
    }

    /**
     * 兑换上下文。
     *
     * @param villageStage 本村阶段（主神像处为 NONE）
     * @param patron       本村主神 / 此像的主神
     * @param gods         祭司处：本村立过祠的次神；主神像处：此像主神名下的次神
     */
    public record VenueContext(TradeOffer.Venue venue, Age villageStage, String patron, Set<String> gods) {
        public VenueContext {
            gods = Set.copyOf(gods);
        }

        public static VenueContext guardian(Age stage, String patron, Set<String> shrineGods) {
            return new VenueContext(TradeOffer.Venue.GUARDIAN, stage, patron, shrineGods);
        }

        /** 神谕所（与祭司处同一村庄的阶段与次神祠）。 */
        public static VenueContext oracle(Age stage, String patron, Set<String> shrineGods) {
            return new VenueContext(TradeOffer.Venue.ORACLE, stage, patron, shrineGods);
        }

        public static VenueContext statue(String patron, Set<String> patronGods) {
            return new VenueContext(TradeOffer.Venue.STATUE, Age.NONE, patron, patronGods);
        }
    }

    static boolean power(TradeOffer o) {
        return o.category() == TradeOffer.Category.GOD_POWER || o.category() == TradeOffer.Category.SUMMON;
    }

    /** 商品是否在此地点出现（不出现的不列在界面上；出现但条件不够的显示为锁定）。 */
    public static boolean visible(TradeOffer o, VenueContext ctx) {
        if (!o.venues().contains(ctx.venue())) {
            return false;
        }
        if (power(o)) {
            return (o.patron().isEmpty() || o.patron().equals(ctx.patron()))
                    && (o.minorGod().isEmpty() || ctx.gods().contains(o.minorGod()));
        }
        return ctx.venue() != TradeOffer.Venue.STATUE && (o.minorGod().isEmpty() || ctx.gods().contains(o.minorGod()));
    }

    /**
     * 按固定顺序检查，返回第一个不满足的原因：地点 → 主神 / 次神祠 → 时代或本村阶段 → 次神供奉 → 价格是否变化
     * → 每日限购 → 恩宠。
     *
     * @param price         按当前折扣算出的恩宠价（物品价商品传 0）
     * @param expectedPrice 客户端界面上显示的价格
     */
    public static TradeReason check(PlayerMythData data, Civilization civ, TradeOffer offer, VenueContext ctx,
                                    long price, long expectedPrice, long today) {
        if (!offer.venues().contains(ctx.venue())) {
            return TradeReason.WRONG_VENUE;
        }
        if (power(offer)) {
            if (!offer.patron().isEmpty() && !offer.patron().equals(ctx.patron())) {
                return TradeReason.WRONG_PATRON;
            }
            if (!offer.minorGod().isEmpty() && !ctx.gods().contains(offer.minorGod())) {
                return ctx.venue() == TradeOffer.Venue.GUARDIAN ? TradeReason.NO_SHRINE : TradeReason.WRONG_PATRON;
            }
            if (!data.civ(civ).age().atLeast(offer.minAge())) {
                return TradeReason.AGE_LOCKED;
            }
            if (!offer.minorGod().isEmpty() && !data.civ(civ).worships(offer.minorGod())) {
                return TradeReason.MINOR_GOD_LOCKED;
            }
        } else {
            if (ctx.venue() == TradeOffer.Venue.STATUE) {
                return TradeReason.WRONG_VENUE;
            }
            if (!offer.minorGod().isEmpty() && !ctx.gods().contains(offer.minorGod())) {
                return TradeReason.NO_SHRINE;
            }
            if (!ctx.villageStage().atLeast(offer.minAge())) {
                return TradeReason.STAGE_LOCKED;
            }
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
     * 适用的折扣：主神折扣在本村主神的村庄或该主神的像处生效；次神折扣——神力与召唤只在立了该次神祠的村庄、
     * 其他商品只要玩家供奉该次神（能出现就说明本村有祠）。
     */
    public static List<Double> discounts(PlayerMythData data, Civilization civ, TradeOffer offer, VenueContext ctx) {
        List<Double> out = new ArrayList<>();
        boolean power = power(offer);
        offer.minorGodDiscount().forEach((god, d) -> {
            if (!data.civ(civ).worships(god)) {
                return;
            }
            if (!power || (ctx.venue() == TradeOffer.Venue.GUARDIAN && ctx.gods().contains(god))) {
                out.add(d);
            }
        });
        if (!offer.patron().isEmpty() && offer.patron().equals(ctx.patron()) && offer.patronDiscount() > 0) {
            out.add(offer.patronDiscount());
        }
        return out;
    }

    /** 今日剩余次数；不限购时为 −1。 */
    public static int remainingToday(PlayerMythData data, Civilization civ, TradeOffer offer, long today) {
        return offer.limited() ? Math.max(0, offer.dailyLimit() - data.tradeDaily().count(offer.key(civ), today)) : -1;
    }
}
