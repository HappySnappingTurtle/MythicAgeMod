package io.github.happysnappingturtle.mythicage.core.village;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.village.VillageBlockers.Class;
import io.github.happysnappingturtle.mythicage.core.village.VillageBlockers.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T24 受阻三分类（TC-T24-01～06）。 */
class VillageBlockersTest {
    static final JsonObject COMMON = JsonParser.parseString("""
            {"woodland": {"radius": 48, "max": 24, "professions": ["@fallback"]},
             "points": {"stone": {"outputs": {"minecraft:cobblestone": 2}, "professions": ["@fallback"]},
                        "gravel": {"outputs": {"minecraft:flint": 1}, "professions": ["@fallback"]}}}""").getAsJsonObject();

    static CivVillageData civ() {
        JsonObject own = JsonParser.parseString("""
                {"buildCosts": {"groups": {"logs": ["minecraft:oak_log", "mythic_age:olive_log"],
                                           "planks": ["minecraft:oak_planks"]}}}""").getAsJsonObject();
        return CivVillageData.parse("g", CommonVillageData.merge(COMMON, own, "g"));
    }

    @Test
    void tc01SelfResolvingKinds() {
        for (Kind k : List.of(Kind.FOOD_LOW, Kind.POINT_DEPLETED, Kind.STOCK_FULL, Kind.NEEDS_HOUSING)) {
            assertEquals(Class.SELF_RESOLVING, VillageBlockers.classify(k, false), k.name());
        }
    }

    @Test
    void tc02BasicGatherIsSelfResolving() {
        CivVillageData d = civ();
        for (String item : List.of("minecraft:cobblestone", "minecraft:flint", "#logs", "minecraft:oak_log")) {
            assertTrue(d.producible(item), item);
            assertEquals(Class.SELF_RESOLVING, VillageBlockers.classify(Kind.MISSING_MATERIAL, d.producible(item)), item);
        }
    }

    @Test
    void tc03RareMaterial() {
        CivVillageData d = civ();
        assertFalse(d.producible("mythic_age:bronze_ingot"));
        assertEquals(Class.RARE_MATERIAL, VillageBlockers.classify(Kind.MISSING_MATERIAL, false));
    }

    @Test
    void tc04MixedProjectSplits() {
        CivVillageData d = civ();
        Map<String, Integer> missing = Map.of("minecraft:cobblestone", 12, "mythic_age:bronze_ingot", 4);
        long rare = missing.keySet().stream()
                .filter(k -> VillageBlockers.classify(Kind.MISSING_MATERIAL, d.producible(k)) == Class.RARE_MATERIAL).count();
        assertEquals(1, rare);
        // 木板没有配方也没有采集点：组里没有可自产的成员就算稀有
        assertFalse(d.producible("#planks"));
    }

    @Test
    void tc05Occupied() {
        assertEquals(Class.OCCUPIED, VillageBlockers.classify(Kind.PROJECT_BLOCKED, true));
        assertEquals(Class.OCCUPIED, VillageBlockers.classify(Kind.NEEDS_SPACE, false));
    }

    @Test
    void tc06NoSourcesConfigured() {
        CivVillageData bare = CivVillageData.parse("x", new JsonObject());
        assertFalse(bare.producible("minecraft:cobblestone"));
        assertFalse(bare.producible("#logs"), "组不存在时不抛异常");
        assertEquals(Class.RARE_MATERIAL, VillageBlockers.classify(Kind.MISSING_MATERIAL, bare.producible("minecraft:cobblestone")));
    }
}
