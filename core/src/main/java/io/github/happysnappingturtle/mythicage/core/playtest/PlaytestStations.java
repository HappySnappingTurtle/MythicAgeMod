package io.github.happysnappingturtle.mythicage.core.playtest;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.rite.PromotionTable;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;

import java.util.List;
import java.util.Optional;

/**
 * 快速验证的 8 个站点（GA-3 设计文档 §2）。纯数据：每站进站时把玩家准备成什么状态、站在哪里。
 * 「看什么」的文字在语言文件 {@code mythic_age.playtest.station.<id>.watch}。
 */
public final class PlaytestStations {
    /** 站在哪里。 */
    public enum Spot {
        /** 村界外约 16 格，面朝村庄。 */
        VILLAGE_EDGE,
        /** 村庄祭司旁。 */
        VILLAGE_PRIEST,
        /** 村庄管事旁。 */
        VILLAGE_KEEPER,
        /** 迷宫入口（结构起点上方）。 */
        LABYRINTH_ENTRANCE,
        /** 牛首门外（同 {@code /mythicage arena tp <n> gate}）。 */
        ARENA_GATE;

        public boolean village() {
            return this == VILLAGE_EDGE || this == VILLAGE_PRIEST || this == VILLAGE_KEEPER;
        }
    }

    /** 晋升供品给多少。 */
    public enum Offerings {
        NONE,
        /** 古典供品的前一半（向上取整）。 */
        HALF_CLASSICAL,
        /** 英雄供品全部。 */
        ALL_HEROIC
    }

    public record Give(String item, int count) {
    }

    /**
     * 一站的准备规格。
     *
     * @param age         希腊时代；NONE 表示未受祝福
     * @param god         晋升到古典时选的次神；空串表示不选
     * @param favor       恩宠（在时代定好之后加，受上限约束）
     * @param altarGift   受祝福时是否发祭坛（首次见面赠礼）
     * @param temple      预先搭好的神庙等级；-1 表示不搭
     * @param items       另给的物品
     * @param offerings   晋升供品
     * @param quest       村里要有一件委托
     * @param firstKill   已结算一次弥诺陶洛斯首杀
     * @param handbook    补发手札
     * @param dayTime     设定的时间（取模 24000）；-1 表示不改
     */
    public record Spec(Age age, String god, long favor, boolean altarGift, int temple, List<Give> items,
                       Offerings offerings, boolean quest, boolean firstKill, boolean handbook, Spot spot, long dayTime) {
        public Spec {
            items = List.copyOf(items);
        }
    }

    public record Station(int number, String id, int minutes, Spec spec) {
        public String watchKey() {
            return "mythic_age.playtest.station." + id + ".watch";
        }

        public String nameKey() {
            return "mythic_age.playtest.station." + id;
        }
    }

    /** 一级神庙的材料：4 根 3 高的柱子、9×9 屋顶、火盆、点火用具，留些余量。 */
    public static final List<Give> TEMPLE_MATERIALS = List.of(
            new Give("mythic_age:marble_pillar", 16),
            new Give("mythic_age:smooth_marble", 128),
            new Give("mythic_age:greek_brazier", 2),
            new Give("minecraft:flint_and_steel", 1));

    /** 迷宫与 Boss 的战斗装备。 */
    public static final List<Give> COMBAT = List.of(
            new Give("mythic_age:bronze_helmet", 1),
            new Give("mythic_age:bronze_chestplate", 1),
            new Give("mythic_age:bronze_leggings", 1),
            new Give("mythic_age:bronze_boots", 1),
            new Give("mythic_age:xiphos", 1),
            new Give("mythic_age:aspis", 1),
            new Give("mythic_age:greek_travel_meal", 8));

    public static final String ATHENA = "athena";
    private static final long MORNING = 1000;
    private static final long DAY = 3000;

    public static final List<Station> ALL = List.of(
            new Station(1, "start", 5, new Spec(Age.NONE, "", 0, false, -1, List.of(), Offerings.NONE,
                    false, false, true, Spot.VILLAGE_EDGE, MORNING)),
            new Station(2, "temple", 10, new Spec(Age.ARCHAIC, "", 0, true, -1, TEMPLE_MATERIALS, Offerings.NONE,
                    false, false, false, Spot.VILLAGE_PRIEST, MORNING)),
            new Station(3, "powers", 10, new Spec(Age.CLASSICAL, ATHENA, 150, false, 1, List.of(), Offerings.NONE,
                    false, false, false, Spot.VILLAGE_PRIEST, DAY)),
            new Station(4, "village", 5, new Spec(Age.ARCHAIC, "", 0, false, -1, List.of(), Offerings.NONE,
                    true, false, false, Spot.VILLAGE_KEEPER, DAY)),
            new Station(5, "rite", 5, new Spec(Age.ARCHAIC, "", 50, false, 1, List.of(), Offerings.HALF_CLASSICAL,
                    false, false, false, Spot.VILLAGE_PRIEST, DAY)),
            new Station(6, "labyrinth", 10, new Spec(Age.CLASSICAL, ATHENA, 150, false, -1, COMBAT, Offerings.NONE,
                    false, false, false, Spot.LABYRINTH_ENTRANCE, DAY)),
            new Station(7, "boss", 10, new Spec(Age.CLASSICAL, ATHENA, 150, false, -1, concat(COMBAT, List.of(
                    new Give("mythic_age:ariadne_thread", 1), new Give("mythic_age:gp_gr_restoration", 1))),
                    Offerings.NONE, false, false, false, Spot.ARENA_GATE, DAY)),
            new Station(8, "hero", 5, new Spec(Age.CLASSICAL, ATHENA, 100, false, 2, List.of(), Offerings.ALL_HEROIC,
                    false, true, false, Spot.VILLAGE_PRIEST, DAY)));

    private PlaytestStations() {
    }

    private static List<Give> concat(List<Give> a, List<Give> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }

    /** 按 id 查（区分大小写，不去空白）。 */
    public static Optional<Station> byId(String id) {
        return ALL.stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public static int totalMinutes() {
        return ALL.stream().mapToInt(Station::minutes).sum();
    }

    /** 本站要给的晋升供品，按晋升表算；表里没有那一级时为空。 */
    public static List<Give> offerings(Offerings plan, PromotionTable table) {
        return switch (plan) {
            case NONE -> List.of();
            case HALF_CLASSICAL -> table.entry(Age.CLASSICAL).map(e -> {
                List<TradeOffer.ItemAmount> all = e.offerings();
                return all.subList(0, (all.size() + 1) / 2).stream().map(PlaytestStations::give).toList();
            }).orElse(List.of());
            case ALL_HEROIC -> table.entry(Age.HEROIC)
                    .map(e -> e.offerings().stream().map(PlaytestStations::give).toList()).orElse(List.of());
        };
    }

    private static Give give(TradeOffer.ItemAmount a) {
        return new Give(a.item(), a.count());
    }
}
