package io.github.happysnappingturtle.mythicage.core.greek;

import io.github.happysnappingturtle.mythicage.core.greek.temple.TempleRules;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.rite.PromotionTable;
import io.github.happysnappingturtle.mythicage.core.rite.VillageStageTable;
import io.github.happysnappingturtle.mythicage.core.trade.PriceCalculator;
import io.github.happysnappingturtle.mythicage.core.trade.TradeOffer;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import io.github.happysnappingturtle.mythicage.core.village.Pantheon;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T11b：正式希腊数据的解析与交叉校验、物品价折扣（TC-T11b-01～05）。 */
class GreekPromotionDataTest {
    private static String read(String rel) {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        Path file = root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/" + rel);
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException("读不到 " + file, e);
        }
    }

    static PromotionTable promotion(List<String> warnings) {
        return PromotionTable.parse(read("promotion/greek.json"), id -> OptionalInt.of(64),
                type -> type.equals("greek:temple_level"), warnings);
    }

    @Test
    void tc01_promotionTable() {
        List<String> warnings = new ArrayList<>();
        PromotionTable t = promotion(warnings);
        assertEquals(List.of(), warnings);
        PromotionTable.AgeEntry c = t.entry(Age.CLASSICAL).orElseThrow();
        assertEquals(50, c.favor());
        assertEquals(List.of(new TradeOffer.ItemAmount("minecraft:cooked_beef", 8), new TradeOffer.ItemAmount("minecraft:wheat", 32),
                new TradeOffer.ItemAmount("mythic_age:libation", 4), new TradeOffer.ItemAmount("minecraft:copper_ingot", 16)),
                c.offerings());
        assertEquals("greek:temple_level", c.requirements().getFirst().type());
        assertEquals(1, c.requirements().getFirst().params().get("level").getAsInt());
        assertEquals(Age.NONE, c.trial());
        PromotionTable.AgeEntry hero = t.entry(Age.HEROIC).orElseThrow();
        assertEquals(100, hero.favor());
        assertEquals(Age.CLASSICAL, hero.trial());
        assertEquals(2, hero.requirements().getFirst().params().get("level").getAsInt());
        assertTrue(t.entry(Age.MYTHIC).isEmpty(), "神话不写（D6）");
        assertEquals(Map.of(Age.CLASSICAL, List.of("mythic_age:minotaur")), t.trials());
    }

    @Test
    void tc02_villageStages() {
        List<String> warnings = new ArrayList<>();
        VillageStageTable s = VillageStageTable.parse(read("village_stage/greek.json"), warnings);
        assertEquals(List.of(), warnings);
        assertEquals(new VillageStageTable.Stage(4, 1, 2, List.of()), s.stage(Age.CLASSICAL));
        assertEquals(new VillageStageTable.Stage(6, 2, 2, List.of()), s.stage(Age.HEROIC));
        assertEquals(new VillageStageTable.Stage(8, 3, 2, List.of()), s.stage(Age.MYTHIC));
        assertEquals(20, s.populationTarget(10, Age.HEROIC));
    }

    @Test
    void tc03_tradeTableMatchesPantheon() {
        TradeTable t = TradeTable.parse(read("trade/greek.json"), TradeTable.ItemLookup.ANY);
        Pantheon p = Pantheon.parse(read("pantheon/greek.json"));
        Map<String, Pantheon.MinorGod> gods = p.minorGods().stream().collect(Collectors.toMap(Pantheon.MinorGod::id, g -> g));
        List<TradeOffer> shrine = t.offers().stream().filter(o -> !o.minorGod().isEmpty()).toList();
        assertEquals(20, shrine.size());
        for (TradeOffer o : shrine) {
            Pantheon.MinorGod g = gods.get(o.minorGod());
            assertTrue(g != null && g.available(), o.id() + "：次神须存在且已开放");
            assertEquals(g.age(), o.minAge(), o.id() + "：时代与次神一致");
            assertFalse(o.price().isFavor(), o.id() + "：绿宝石价（D1）");
            assertTrue(o.price().items().stream().allMatch(a -> a.item().equals("minecraft:emerald") && a.count() >= 3),
                    o.id() + "：每项 ≥3 个绿宝石");
            assertEquals(Map.of(o.minorGod(), 0.1), o.minorGodDiscount(), o.id());
            assertEquals(4, o.dailyLimit());
        }
        TradeOffer statue = t.offer("patron_statue").orElseThrow();
        assertTrue(statue.price().isFavor() && statue.price().favor() == 30);
    }

    @Test
    void tc04_templeLevelReachableBeforeNeeded() {
        TempleRules rules = TempleRules.parse(read("temple/greek.json"));
        PromotionTable t = promotion(new ArrayList<>());
        for (Age target : List.of(Age.CLASSICAL, Age.HEROIC)) {
            int need = t.entry(target).orElseThrow().requirements().getFirst().params().get("level").getAsInt();
            Age before = Age.values()[target.ordinal() - 1];
            assertTrue(rules.ageCap(before) >= need, target + " 要求的 " + need + " 级在 " + before + " 就能认定");
        }
    }

    @Test
    void tc05_itemPriceDiscount() {
        List<Double> ten = List.of(0.1);
        assertEquals(2, PriceCalculator.items(3, ten));
        assertEquals(3, PriceCalculator.items(4, ten));
        assertEquals(5, PriceCalculator.items(6, ten));
        assertEquals(1, PriceCalculator.items(1, ten));
        assertEquals(9, PriceCalculator.items(10, ten), "10 × 0.9 不因浮点误差变 8");
        assertEquals(5, PriceCalculator.items(5, List.of()));
        assertEquals(4, PriceCalculator.items(6, List.of(0.1, 0.2)), "6 × 0.9 × 0.8 = 4.32 → 4");
    }
}
