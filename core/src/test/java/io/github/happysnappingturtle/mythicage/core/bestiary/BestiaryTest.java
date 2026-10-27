package io.github.happysnappingturtle.mythicage.core.bestiary;

import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.bestiary.BestiaryRules.Tier;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 图鉴点亮（TC-T24-11～16）。 */
class BestiaryTest {
    @Test
    void tc11RaiseCanSkip() {
        Set<String> d = new LinkedHashSet<>();
        assertEquals(List.of(Tier.SIGHTED), BestiaryRules.raise(d, "greek/harpy", Tier.SIGHTED));
        assertEquals(List.of(Tier.ENGAGED, Tier.DEFEATED), BestiaryRules.raise(d, "greek/harpy", Tier.DEFEATED), "交战被跳过时一并补上");
        assertEquals(Tier.DEFEATED, BestiaryRules.tier(d, "greek/harpy"));
        Set<String> e = new LinkedHashSet<>();
        BestiaryRules.raise(e, "greek/karkinos", Tier.ENGAGED);
        assertTrue(e.contains("bestiary:greek/karkinos:sighted") && e.contains("bestiary:greek/karkinos:engaged"));
    }

    @Test
    void tc12NeverLowers() {
        Set<String> d = new LinkedHashSet<>();
        BestiaryRules.raise(d, "greek/minotaur", Tier.DEFEATED);
        assertTrue(BestiaryRules.raise(d, "greek/minotaur", Tier.SIGHTED).isEmpty(), "已到更高级：不再授予");
        assertEquals(Tier.DEFEATED, BestiaryRules.tier(d, "greek/minotaur"));
        assertEquals(Tier.NONE, BestiaryRules.tier(d, "greek/harpy"));
    }

    @Test
    void tc13SightEdge() {
        assertTrue(BestiaryRules.inSight(24 * 24));
        assertFalse(BestiaryRules.inSight(24.01 * 24.01));
    }

    @Test
    void tc14NearestEight() {
        List<Integer> dist = List.of(30, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        List<Integer> picked = BestiaryRules.nearest(dist, x -> x * x);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), picked, "超出 24 格的不算，最近的 8 只");
    }

    @Test
    void tc15SharedEntry() {
        BestiaryTable t = BestiaryTable.parse(JsonParser.parseString("""
                {"entries": {"greek/minotaur": {"entities": ["mythic_age:minotaur", "mythic_age:summon_minotaur", "mythic_age:minotaur_boss"]},
                             "greek/harpy": {"entities": ["mythic_age:harpy"]}}}""").getAsJsonObject(), "t");
        assertEquals("greek/minotaur", t.entryOf("mythic_age:summon_minotaur").orElseThrow());
        assertEquals("greek/minotaur", t.entryOf("mythic_age:minotaur_boss").orElseThrow());
        assertTrue(t.entryOf("minecraft:cow").isEmpty());
        assertEquals(List.of("greek/minotaur", "greek/harpy"), t.entries());
    }

    @Test
    void tc16BadTables() {
        assertThrows(IllegalArgumentException.class, () -> BestiaryTable.parse(JsonParser.parseString("""
                {"entries": {"greek/a": {"entities": ["x:y"]}, "greek/b": {"entities": ["x:y"]}}}""").getAsJsonObject(), "t"),
                "实体重复");
        assertThrows(IllegalArgumentException.class, () -> BestiaryTable.parse(JsonParser.parseString("""
                {"entries": {"greek/a": {"entities": []}}}""").getAsJsonObject(), "t"), "没有实体");
        assertThrows(IllegalArgumentException.class, () -> BestiaryTable.parse(JsonParser.parseString("""
                {"entries": {"Minotaur": {"entities": ["x:y"]}}}""").getAsJsonObject(), "t"), "条目名不合法");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> BestiaryTable.merge(List.of(
                BestiaryTable.parse(JsonParser.parseString("{\"entries\": {\"greek/a\": {\"entities\": [\"x:y\"]}}}").getAsJsonObject(), "a"),
                BestiaryTable.parse(JsonParser.parseString("{\"entries\": {\"norse/b\": {\"entities\": [\"x:y\"]}}}").getAsJsonObject(), "b"))));
        assertTrue(e.getMessage().contains("x:y"));
    }
}
