package io.github.happysnappingturtle.mythicage.core.village.life;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.time.VillageCalendar;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T19 阶段 ⑤：生活与节庆的纯逻辑（单元测试用例文档 TC-T19-19a–19f）。 */
class LifeTest {
    static CivVillageData greek() throws IOException {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        JsonObject o = JsonParser.parseString(Files.readString(
                root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/village_civ/greek.json"))).getAsJsonObject();
        return CivVillageData.parse("greek", o);
    }

    @Test
    void tc19aParseGreekLife() throws IOException {
        LifeData l = greek().life();
        assertEquals(List.of(new LifeData.Act("libation_home", 40), new LifeData.Act("eat", 100)), l.meals().dawn());
        assertEquals(1, l.meals().lunch().size());
        assertTrue(l.activities().size() >= 5);
        LifeData.Activity water = l.activities().stream().filter(a -> a.id().equals("water")).findFirst().orElseThrow();
        assertEquals("water", water.anchor());
        assertEquals(2, water.windows().size());
        assertEquals(1, water.min());
        assertEquals(3, water.max());
        assertEquals("hydria", water.carryHome());
        assertEquals("female", water.prefer());
        assertEquals("merchant", l.meet().shelter());
        assertTrue(l.priest().size() >= 3);
        assertTrue(l.priest().stream().anyMatch(p -> p.id().equals("libation") && p.all() && p.at() == 1000));
        LifeData.Festival polis = l.festivals().get("polis_festival");
        assertEquals("heroic", polis.minAge());
        assertEquals(2.0, polis.rations());
        assertEquals(List.of("relay", "procession", "games", "gather"), polis.phases().stream().map(LifeData.Phase::kind).toList());
        assertTrue(polis.phases().get(0).eve());
        assertEquals(List.of("mythic_age:wine", "mythic_age:olive_oil"), polis.phases().get(1).offering());
        LifeData.Phase feast = l.festivals().get("rural_dionysia").phases().get(2);
        assertEquals("mythic_age:wine", feast.consume());
        assertEquals(4, feast.per());
        assertEquals(8, l.fishing().daily());
        assertTrue(l.fishing().enabled());
        assertEquals("sowing", l.oracle().closedPeriod());
        JsonObject bad = JsonParser.parseString("{\"festivals\":{\"x\":{\"phases\":[{\"kind\":\"juggle\"}]}}}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> LifeData.parse(bad));
    }

    @Test
    void tc19bPhases() throws IOException {
        CivVillageData d = greek();
        LifeData l = d.life();
        VillageCalendar c = d.calendar();
        // 偏移 0：mcDay 8 是第 9 日（城邦节），mcDay 7 是前夜
        assertEquals("relay", FestivalRules.at(l, c, 0, 7, 13500, Age.HEROIC).orElseThrow().phase().kind());
        assertTrue(FestivalRules.at(l, c, 0, 7, 13500, Age.HEROIC).orElseThrow().eve());
        assertEquals("procession", FestivalRules.at(l, c, 0, 8, 1000, Age.HEROIC).orElseThrow().phase().kind());
        assertEquals("games", FestivalRules.at(l, c, 0, 8, 7000, Age.HEROIC).orElseThrow().phase().kind());
        assertEquals("gather", FestivalRules.at(l, c, 0, 8, 12000, Age.HEROIC).orElseThrow().phase().kind());
        assertTrue(FestivalRules.at(l, c, 0, 8, 4000, Age.HEROIC).isEmpty());
        assertTrue(FestivalRules.at(l, c, 0, 8, 1000, Age.CLASSICAL).isEmpty());
        assertTrue(FestivalRules.at(l, c, 0, 7, 13500, Age.CLASSICAL).isEmpty());
        assertEquals(2.0, FestivalRules.rations(l, c, 0, 8, Age.HEROIC));
        assertEquals(1.0, FestivalRules.rations(l, c, 0, 8, Age.CLASSICAL));
        assertEquals(1.0, FestivalRules.rations(l, c, 0, 7, Age.HEROIC));
        // 乡村酒神节：第 12 日
        assertEquals("rural_dionysia", FestivalRules.on(l, c, 0, 11, Age.MYTHIC).orElseThrow().id());
    }

    @Test
    void tc19cRelease() {
        List<Integer> r = FestivalRules.release(13, 2, 20);
        assertEquals(13, r.size());
        assertEquals(0, r.get(0));
        assertEquals(0, r.get(1));
        assertEquals(20, r.get(2));
        assertEquals(120, r.get(12));
        assertEquals(7, r.stream().distinct().count());
        for (int t : r) {
            assertTrue(r.stream().filter(x -> x == t).count() <= 2);
        }
    }

    @Test
    void tc19dConsumption() {
        assertEquals(2, FestivalRules.feast(13, 4, 2));
        assertEquals(4, FestivalRules.feast(13, 4, 10));
        assertEquals(0, FestivalRules.feast(13, 4, 0));
        assertEquals(0, FestivalRules.feast(0, 4, 5));
        List<String> off = List.of("mythic_age:wine", "mythic_age:olive_oil");
        assertEquals("mythic_age:wine", FestivalRules.offering(off, Map.of("mythic_age:wine", 1, "mythic_age:olive_oil", 3)::get).orElseThrow());
        assertEquals("mythic_age:olive_oil", FestivalRules.offering(off, i -> i.endsWith("oil") ? 1 : 0).orElseThrow());
        assertTrue(FestivalRules.offering(off, i -> 0).isEmpty());
    }

    @Test
    void tc19eOracle() throws IOException {
        VillageCalendar c = greek().calendar();
        OracleRules.Status s5 = OracleRules.status(c, 0, 4, "oracle", "sowing");
        assertEquals(OracleRules.State.OPEN, s5.state());
        assertEquals(8, s5.nextDay());
        OracleRules.Status s6 = OracleRules.status(c, 0, 5, "oracle", "sowing");
        assertEquals(OracleRules.State.CLOSED, s6.state());
        assertEquals(8, s6.nextDay());
        assertEquals(2, s6.daysUntil());
        OracleRules.Status s2 = OracleRules.status(c, 0, 1, "oracle", "sowing");
        assertEquals(OracleRules.State.WINTER, s2.state());
        assertEquals(5, s2.nextDay());
        OracleRules.Status s12 = OracleRules.status(c, 0, 11, "oracle", "sowing");
        assertEquals(OracleRules.State.CLOSED, s12.state());
        assertEquals(5, s12.nextDay());
        assertEquals(5, s12.daysUntil());
    }

    @Test
    void tc19fWaterPeople() throws IOException {
        LifeData.Activity water = greek().life().activities().stream().filter(a -> a.id().equals("water")).findFirst().orElseThrow();
        UUID v = UUID.fromString("00000000-0000-0000-0000-000000000042");
        for (long day = 0; day < 30; day++) {
            long seed = LifeRules.seed(v, day, 0, "water");
            int n = LifeRules.people(water, 10, 1, seed);
            assertTrue(n >= 1 && n <= 3, "人数 " + n);
            assertEquals(n, LifeRules.people(water, 10, 1, LifeRules.seed(v, day, 0, "water")));
            assertEquals(1, LifeRules.people(water, 2, 1, seed));
            assertEquals(0, LifeRules.people(water, 0, 1, seed));
            assertEquals(0, LifeRules.people(water, 10, 0, seed));
        }
        LifeData.Activity grave = greek().life().activities().stream().filter(a -> a.id().equals("grave")).findFirst().orElseThrow();
        int days = 0;
        for (long day = 0; day < 400; day++) {
            days += LifeRules.people(grave, 10, 3, LifeRules.seed(v, day, 11000, "grave"));
        }
        assertTrue(days > 60 && days < 140, "25% 概率：400 日里 " + days + " 日有人扫墓");
        assertFalse(LifeRules.unit(123) < 0);
    }
}
