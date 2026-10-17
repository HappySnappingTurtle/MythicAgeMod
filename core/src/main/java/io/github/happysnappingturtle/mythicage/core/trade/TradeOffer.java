package io.github.happysnappingturtle.mythicage.core.trade;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 交易表中的一条商品（T9 设计文档 §4）。价格是恩宠或物品，二选一。
 *
 * @param dailyLimit       每个玩家每 MC 日的购买次数；0 表示不限
 * @param minorGod         非空时只对供奉该次神的玩家出售（T10a / 17 号 §7.3）
 * @param minorGodDiscount 次神 → 折扣：玩家供奉该次神时打折
 * @param patron           该商品对应的主神（如雷击 = zeus）；空表示无
 * @param patronDiscount   在主神为 patron 的村庄的祭司处购买时的折扣（Q1：九折）
 */
public record TradeOffer(String id, Category category, ItemAmount result, Price price, Age minAge, Set<Venue> venues,
                         int dailyLimit, String minorGod, java.util.Map<String, Double> minorGodDiscount, String patron,
                         double patronDiscount) {
    public TradeOffer {
        Objects.requireNonNull(id);
        venues = Set.copyOf(venues);
        minorGod = minorGod == null ? "" : minorGod;
        minorGodDiscount = java.util.Map.copyOf(minorGodDiscount);
        patron = patron == null ? "" : patron;
    }

    /** T9 的写法：没有次神与主神字段。 */
    public TradeOffer(String id, Category category, ItemAmount result, Price price, Age minAge, Set<Venue> venues,
                      int dailyLimit) {
        this(id, category, result, price, minAge, venues, dailyLimit, "", java.util.Map.of(), "", 0);
    }

    /** 收据与限购用的全局键：{@code <文明>:<id>}。 */
    public String key(Civilization civ) {
        return civ.id() + ":" + id;
    }

    public boolean limited() {
        return dailyLimit > 0;
    }

    /** 界面分组。 */
    public enum Category {
        GOD_POWER, SUMMON, BLUEPRINT, SUPPLIES;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Optional<Category> byId(String id) {
            for (Category c : values()) {
                if (c.id().equals(id)) {
                    return Optional.of(c);
                }
            }
            return Optional.empty();
        }
    }

    /** 出售地点：守护者本人，或玩家自己的神坛。 */
    /**
     * 兑换地点（T11a 修订 1）：村庄祭司、玩家自己的主神像。T9 的 {@code altar}（玩家神坛）已由主神像取代，
     * 数据里的 {@code altar} 读作 {@code statue}。
     */
    public enum Venue {
        GUARDIAN, STATUE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Optional<Venue> byId(String id) {
            if ("altar".equals(id)) {
                return Optional.of(STATUE);
            }
            for (Venue v : values()) {
                if (v.id().equals(id)) {
                    return Optional.of(v);
                }
            }
            return Optional.empty();
        }
    }

    /** 物品 id 与数量。 */
    public record ItemAmount(String item, int count) {
        public ItemAmount {
            Objects.requireNonNull(item);
            if (count < 1) {
                throw new IllegalArgumentException("数量必须为正：" + count);
            }
        }
    }

    /** 价格：favor > 0 时为恩宠价，否则为物品价（items 非空）。 */
    public record Price(long favor, List<ItemAmount> items) {
        public Price {
            items = List.copyOf(items);
            if ((favor > 0) == !items.isEmpty()) {
                throw new IllegalArgumentException("恩宠价与物品价必须二选一");
            }
        }

        public static Price ofFavor(long favor) {
            return new Price(favor, List.of());
        }

        public static Price ofItems(List<ItemAmount> items) {
            return new Price(0, items);
        }

        public boolean isFavor() {
            return favor > 0;
        }
    }
}
