package io.github.happysnappingturtle.mythicage.core.village.trade;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.economy.Rations;
import io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T19 单元测试 TC-T19-109、116～118：居民交易的折扣、额度、口粮底线、杂货。 */
class ResidentTradeTest {
    static final ResidentTradeRules RULES = new ResidentTradeRules(0.9, Map.of("hermes", 0.95), "hera");

    static ResidentOffer sell(String id, String item, int count, int price, int daily, Age age, String god, boolean imported) {
        return new ResidentOffer(id, true, item, count, price, daily, age, god, imported);
    }

    @Test
    void tc116_minorGodVisibilityAndDiscounts() {
        ResidentOffer oil = sell("oil", "mythic_age:olive_oil", 2, 10, 4, Age.CLASSICAL, "athena", false);
        assertTrue(ResidentTradeRules.visible(oil, Age.CLASSICAL, Set.of("athena")));
        assertFalse(ResidentTradeRules.visible(oil, Age.CLASSICAL, Set.of("ares")), "没有祠不出现");
        assertFalse(ResidentTradeRules.visible(oil, Age.ARCHAIC, Set.of("athena")), "阶段没到不出现");
        assertEquals(10, RULES.price(oil, Set.of()));
        assertEquals(9, RULES.price(oil, Set.of("athena")), "供奉者九折");
        assertEquals(9, RULES.price(oil, Set.of("athena", "hermes")), "10 × (1 − 0.855) = 1.45 → 便宜 1");
        ResidentOffer cheap = sell("c", "mythic_age:olive_oil", 3, 2, 4, Age.CLASSICAL, "athena", false);
        assertEquals(1, RULES.price(cheap, Set.of("athena")), "原价 2：有折扣至少便宜 1（原版英雄折扣惯例）");
        assertEquals(2, RULES.price(cheap, Set.of()), "不是供奉者原价");
        ResidentOffer big = sell("b", "minecraft:map", 1, 20, 1, Age.ARCHAIC, "", true);
        assertEquals(19, RULES.price(big, Set.of("hermes")), "20 × 5% = 1");
        ResidentOffer plain = sell("flat", "mythic_age:olive_flatbread", 6, 1, 8, Age.ARCHAIC, "", false);
        assertEquals(1, RULES.price(plain, Set.of("hermes")), "至少 1");
        ResidentOffer buy = new ResidentOffer("clay", false, "minecraft:clay_ball", 16, 1, 8, Age.ARCHAIC, "", false);
        assertEquals(1, RULES.price(buy, Set.of("hermes", "athena")), "收购价不打折");
    }

    @Test
    void tc109_f7HalvesDailyUnlessHera() {
        ResidentOffer o = sell("o", "mythic_age:olive", 12, 1, 8, Age.ARCHAIC, "", false);
        assertEquals(8, RULES.dailyCap(o, false, Set.of()));
        assertEquals(4, RULES.dailyCap(o, true, Set.of()), "当天祈祷过减半");
        assertEquals(8, RULES.dailyCap(o, true, Set.of("hera")), "供奉赫拉不减半");
        assertEquals(1, RULES.dailyCap(sell("x", "i", 1, 1, 1, Age.ARCHAIC, "", false), true, Set.of()), "至少 1");
    }

    @Test
    void tc117_foodSoldDownToFloor() {
        Rations rations = new Rations(Map.of("mythic_age:olive_flatbread", 1.0), 24);
        VillageStock stock = new VillageStock(100);
        stock.deposit("mythic_age:olive_flatbread", 30);
        ResidentOffer bread = sell("flat", "mythic_age:olive_flatbread", 6, 1, 8, Age.ARCHAIC, "", false);
        assertEquals(1, ResidentTradeRules.stockShares(bread, stock, rations));
        stock.take("mythic_age:olive_flatbread", 6, 0);
        assertEquals(0, ResidentTradeRules.stockShares(bread, stock, rations), "第 2 份缺货");
        ResidentOffer olive = sell("olive", "mythic_age:olive", 12, 1, 8, Age.ARCHAIC, "", false);
        stock.deposit("mythic_age:olive", 25);
        assertEquals(2, ResidentTradeRules.stockShares(olive, stock, rations), "非口粮按库存");
    }

    @Test
    void tc118_importIgnoresStock() {
        ResidentOffer map = sell("map", "minecraft:map", 1, 3, 2, Age.CLASSICAL, "hermes", true);
        assertEquals(Integer.MAX_VALUE, ResidentTradeRules.stockShares(map, new VillageStock(0), Rations.none()));
        ResidentOffer buy = new ResidentOffer("clay", false, "minecraft:clay_ball", 16, 1, 8, Age.ARCHAIC, "", false);
        VillageStock full = new VillageStock(10);
        assertFalse(ResidentTradeRules.canAccept(buy, full), "装不下拒收");
        assertTrue(ResidentTradeRules.canAccept(buy, new VillageStock(100)));
    }
}
