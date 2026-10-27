package io.github.happysnappingturtle.mythicage.core.trade;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T9-01～05：交易表解析与价格计算。 */
class TradeTableTest {
    static TradeTable greek() {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        Path file = root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/trade/greek.json");
        try {
            return TradeTable.parse(Files.readString(file), TradeTable.ItemLookup.ANY);
        } catch (IOException e) {
            throw new IllegalStateException("读不到 " + file, e);
        }
    }

    private static final String BASE = """
            {"schemaVersion":1,"civilization":"greek","offers":[%s]}""";
    private static final String OFFER = """
            {"id":"bolt","category":"god_power","result":{"item":"mythic_age:bolt","count":1},
             "price":{"favor":20},"minAge":"archaic","venues":["guardian","altar"]}""";

    private static void assertPath(String json, String path) {
        assertPath(json, path, TradeTable.ItemLookup.ANY);
    }

    private static void assertPath(String json, String path, TradeTable.ItemLookup items) {
        var e = assertThrows(TradeTable.TradeTableException.class, () -> TradeTable.parse(json, items));
        assertTrue(e.getMessage().startsWith(path + ":"), () -> "应以 " + path + " 开头：" + e.getMessage());
    }

    @Test
    void tc01ParsesDefault() {
        TradeTable t = greek();
        assertEquals(Civilization.GREEK, t.civ());
        assertEquals(28, t.offers().stream().filter(x -> x.category() == TradeOffer.Category.SUPPLIES).count(),
                "奠酒、主神像、20 件次神祠商品（T11b）、村庄指引（T13b）、传令杖（T14）、2 件神谕（T19 §1.10 J6）、阿里阿德涅线团（T23）、"
                        + "克诺索斯地图（T24）");
        TradeOffer map = t.offer("knossos_map").orElseThrow();
        assertEquals(8, map.price().items().get(0).count(), "克诺索斯地图 8 绿宝石（T24 D12）");
        assertEquals(Set.of(TradeOffer.Venue.GUARDIAN), map.venues(), "守护者处");
        TradeOffer thread = t.offer("ariadne_thread").orElseThrow();
        assertEquals(4, thread.price().items().get(0).count(), "线团 4 绿宝石（T23 D2）");
        assertEquals(2, thread.dailyLimit(), "每日 2 个");
        assertEquals(Set.of(TradeOffer.Venue.GUARDIAN), thread.venues(), "守护者处");
        TradeOffer guide = t.offer("village_guide").orElseThrow();
        assertEquals(8, guide.price().items().get(0).count(), "村庄指引 8 绿宝石（T13b §7）");
        assertTrue(t.offers().stream().filter(x -> x.category() == TradeOffer.Category.GOD_POWER)
                .allMatch(x -> x.price().isFavor()), "神力一律恩宠价（T13）");
        TradeOffer o = t.offer("libation").orElseThrow();
        assertEquals(new TradeOffer.ItemAmount("mythic_age:libation", 2), o.result());
        assertEquals(List.of(new TradeOffer.ItemAmount("minecraft:emerald", 1)), o.price().items());
        assertEquals(Age.ARCHAIC, o.minAge());
        assertEquals(Set.of(TradeOffer.Venue.GUARDIAN), o.venues());
        assertEquals(8, o.dailyLimit());
        assertEquals("greek:libation", o.key(Civilization.GREEK));
        assertTrue(t.offersAt(TradeOffer.Venue.STATUE).stream().allMatch(x -> x.category() == TradeOffer.Category.GOD_POWER
                        && !x.patron().isEmpty() || x.category() == TradeOffer.Category.SUMMON),
                "神像前只有主神神力（T13）与天马召唤符（T14 §2.1）");
        TradeOffer centaur = t.offer("summon_centaur").orElseThrow();
        assertTrue("hermes".equals(centaur.minorGod()) && centaur.minAge() == Age.CLASSICAL
                        && centaur.venues().equals(Set.of(TradeOffer.Venue.GUARDIAN)),
                "半人马召唤符：祭司处，古典起，须供奉赫尔墨斯（T14 §2.1）");
        TradeOffer pegasus = t.offer("summon_pegasus").orElseThrow();
        assertTrue(pegasus.patron().isEmpty() && pegasus.price().isFavor() && pegasus.minAge() == Age.ARCHAIC,
                "天马召唤符：全体主神的神像前，恩宠价，远古起（T14 §2.1）");
    }

    @Test
    void tc01bFavorOfferAndVenues() {
        TradeTable t = TradeTable.parse(BASE.formatted(OFFER), TradeTable.ItemLookup.ANY);
        TradeOffer o = t.offers().getFirst();
        assertTrue(o.price().isFavor());
        assertEquals(20, o.price().favor());
        assertEquals(0, o.dailyLimit(), "省略 dailyLimit 为不限");
        assertEquals(1, t.offersAt(TradeOffer.Venue.STATUE).size());
    }

    @Test
    void tc02FieldErrorsCarryPath() {
        assertPath(BASE.formatted(OFFER + "," + OFFER), "offers[1].id");
        assertPath(BASE.formatted(OFFER.replace("god_power", "weapon")), "offers[0].category");
        assertPath(BASE.formatted(OFFER.replace("{\"favor\":20}",
                "{\"favor\":20,\"items\":[{\"item\":\"minecraft:emerald\",\"count\":1}]}")), "offers[0].price");
        assertPath(BASE.formatted(OFFER.replace("{\"favor\":20}", "{}")), "offers[0].price");
        assertPath(BASE.formatted(OFFER.replace("\"favor\":20", "\"favor\":0")), "offers[0].price.favor");
        String four = "{\"items\":[{\"item\":\"a:a\",\"count\":1},{\"item\":\"a:b\",\"count\":1},"
                + "{\"item\":\"a:c\",\"count\":1},{\"item\":\"a:d\",\"count\":1}]}";
        assertPath(BASE.formatted(OFFER.replace("{\"favor\":20}", four)), "offers[0].price.items");
        assertPath(BASE.formatted(OFFER.replace("[\"guardian\",\"altar\"]", "[]")), "offers[0].venues");
        assertPath(BASE.formatted(OFFER.replace("[\"guardian\",\"altar\"]", "[\"market\"]")), "offers[0].venues[0]");
        assertPath(BASE.formatted(OFFER.replace("\"archaic\"", "\"golden\"")), "offers[0].minAge");
        assertPath(BASE.formatted(OFFER.replace("\"archaic\"", "\"none\"")), "offers[0].minAge");
        assertPath(BASE.formatted(OFFER.replace("\"count\":1}", "\"count\":0}")), "offers[0].result.count");
        assertPath(BASE.formatted(OFFER.replace("\"bolt\",\"category\"", "\"Bolt!\",\"category\"")), "offers[0].id");
    }

    @Test
    void tc02bItemsCheckedAgainstRegistry() {
        TradeTable.ItemLookup only = id -> id.equals("mythic_age:bolt") ? OptionalInt.of(16) : OptionalInt.empty();
        assertPath(BASE.formatted(OFFER.replace("mythic_age:bolt", "mythic_age:nothing")), "offers[0].result.item", only);
        assertPath(BASE.formatted(OFFER.replace("\"count\":1}", "\"count\":17}")), "offers[0].result.count", only);
    }

    @Test
    void tc03SchemaVersionAndUnknownFields() {
        assertPath("{\"civilization\":\"greek\",\"offers\":[]}", "schemaVersion");
        assertPath(BASE.formatted("").replace("\"schemaVersion\":1", "\"schemaVersion\":2"), "schemaVersion");
        assertPath(BASE.formatted("").replace("\"offers\"", "\"extra\":1,\"offers\""), "extra");
        assertPath(BASE.formatted(OFFER.replace("\"minAge\"", "\"stock\":3,\"minAge\"")), "offers[0].stock");
        assertPath("{oops", "(根)");
    }

    @Test
    void tc04PriceDiscountsMultiply() {
        assertEquals(20, PriceCalculator.favor(20, List.of()));
        assertEquals(18, PriceCalculator.favor(20, List.of(0.1)));
        assertEquals(15, PriceCalculator.favor(20, List.of(0.1, 0.2)), "20×0.9×0.8 = 14.4 向上取整");
    }

    @Test
    void tc05PriceFloorIsOne() {
        assertEquals(1, PriceCalculator.favor(1, List.of(0.5)));
        assertTrue(PriceCalculator.favor(10, List.of(0.6, 0.6, 0.9)) >= 1);
        assertEquals(1, PriceCalculator.favor(10, List.of(1.5, 1.5)), "越界折扣截断到 0.95");
        assertThrows(IllegalArgumentException.class, () -> PriceCalculator.favor(0, List.of()));
    }
}
