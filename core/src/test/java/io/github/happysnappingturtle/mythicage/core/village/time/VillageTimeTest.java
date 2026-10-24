package io.github.happysnappingturtle.mythicage.core.village.time;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.recipe.FuelTable;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T19 单元测试 TC-T19-15～24：作息、历法、配方、燃料、资源点。 */
class VillageTimeTest {
    private static final String GREEK_SCHEDULE = """
            {"segments": [
              {"id": "dawn", "from": 23000, "to": 24000}, {"id": "work", "from": 0, "to": 6000},
              {"id": "lunch", "from": 6000, "to": 6400}, {"id": "work", "from": 6400, "to": 11000},
              {"id": "evening", "from": 11000, "to": 12000}, {"id": "supper", "from": 12000, "to": 13000},
              {"id": "sleep", "from": 13000, "to": 23000}],
             "overlays": [{"id": "ritual", "from": 2000, "to": 9000}]}""";

    private static final String GREEK_CALENDAR = """
            {"yearDays": 12, "periods": [{"id": "sowing", "days": 3}, {"id": "growing", "days": 3},
              {"id": "harvest", "days": 3}, {"id": "vintage", "days": 3}],
             "special": {"5": "oracle", "8": "oracle", "9": "polis_festival", "11": "oracle", "12": "rural_dionysia"}}""";

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void tc15_segments() {
        Schedule s = Schedule.parse(json(GREEK_SCHEDULE));
        assertEquals("work", s.segmentAt(500));
        assertEquals("lunch", s.segmentAt(6200));
        assertEquals("supper", s.segmentAt(12500));
        assertEquals("sleep", s.segmentAt(20000));
        assertTrue(s.in("ritual", 3000));
        assertTrue(s.startsAt("supper", 12000));
    }

    @Test
    void tc16_wrapAroundMidnight() {
        Schedule s = Schedule.parse(json(GREEK_SCHEDULE));
        assertEquals("dawn", s.segmentAt(23500));
        assertEquals("dawn", s.segmentAt(23500 + 24000 * 3));
    }

    @Test
    void scheduleMustCoverDay() {
        assertThrows(IllegalArgumentException.class, () -> Schedule.parse(json(
                "{\"segments\": [{\"id\": \"work\", \"from\": 0, \"to\": 12000}]}")));
    }

    @Test
    void tc17_twelveDayYear() {
        VillageCalendar c = VillageCalendar.parse(json(GREEK_CALENDAR));
        VillageCalendar.Date d1 = c.date(0, 0);
        assertEquals(1, d1.dayOfYear());
        assertEquals("sowing", d1.period());
        assertEquals(1, d1.dayInPeriod());
        assertEquals("growing", c.date(3, 0).period());
        VillageCalendar.Date d9 = c.date(8, 0);
        assertEquals("harvest", d9.period());
        assertEquals(3, d9.dayInPeriod());
        assertTrue(d9.is("polis_festival"));
        assertTrue(c.date(11, 0).is("rural_dionysia"));
        assertEquals(1, c.date(12, 0).dayOfYear(), "12 日一循环");
    }

    @Test
    void tc18_offset() {
        VillageCalendar c = VillageCalendar.parse(json(GREEK_CALENDAR));
        assertEquals(9, c.date(3, 5).dayOfYear());
        assertTrue(c.date(3, 5).is("polis_festival"));
        assertEquals(1, c.daysUntil(2, 5, "polis_festival"));
    }

    @Test
    void tc19_offsetStable() {
        VillageCalendar c = VillageCalendar.parse(json(GREEK_CALENDAR));
        UUID id = UUID.fromString("0b3f6a0e-6c55-4c9e-9a51-2f0d7c3b1e11");
        int o = c.offset(id);
        assertEquals(o, c.offset(UUID.fromString(id.toString())));
        for (int i = 0; i < 200; i++) {
            int x = c.offset(UUID.randomUUID());
            assertTrue(x >= 0 && x < 12);
        }
    }

    @Test
    void tc20_recipeParse() {
        VillageRecipe r = VillageRecipe.parse("fire_tile", "greek", json("""
                {"station": "mythic_age:pottery_kiln", "professions": ["greek:potter"],
                 "inputs": {"mythic_age:raw_roof_tile": 4}, "fuel": 1, "outputs": {"mythic_age:ceramic_component": 4},
                 "stages": [{"anim": "kiln_load", "ticks": 80}, {"anim": "kiln_unload", "ticks": 160}]}"""));
        assertEquals(2, r.stages().size());
        assertEquals(240, r.totalTicks());
        assertEquals("kiln_unload", r.stages().get(1).anim());
        assertEquals(Map.of("mythic_age:raw_roof_tile", 4), r.inputs());
        assertEquals(1, r.fuel());
        assertEquals("archaic", r.stage());
    }

    @Test
    void tc21_fuelUnits() {
        FuelTable f = new FuelTable(Map.of("olive_stick", 4, "oak_log", 1, "charcoal", 1));
        VillageStock s = new VillageStock(100);
        s.deposit("olive_stick", 3);
        assertEquals(Optional.empty(), f.pick(s, 1), "木棍 3 不够 1 单位");
        s.deposit("olive_stick", 1);
        assertEquals(Optional.of(Map.of("olive_stick", 4)), f.pick(s, 1));
        s.deposit("charcoal", 1);
        assertEquals(Optional.of(Map.of("charcoal", 1)), f.pick(s, 1), "优先用整件的燃料");
        assertEquals(2, f.units(s));
    }

    @Test
    void tc22_unattendedRecipe() {
        VillageRecipe r = VillageRecipe.parse("ferment", "greek", json("""
                {"station": "mythic_age:pithos", "inputs": {"mythic_age:grape_must": 1},
                 "outputs": {"mythic_age:wine": 2}, "unattendedDays": 2}"""));
        assertTrue(r.unattended());
        long start = 10;
        assertTrue(11 < start + r.unattendedDays());
        assertTrue(12 >= start + r.unattendedDays());
    }

    @Test
    void tc23_clayRecovers() {
        ResourcePoint p = ResourcePoint.fresh("c", "clay", new Pos(0, 0, 0), 48, 4, 0).take(40);
        assertEquals(8, p.remaining());
        assertEquals(20, p.recover(3).remaining());
        assertEquals(48, p.recover(100).remaining());
        assertEquals(p.recover(3), p.recover(3).recover(3), "同一日不重复恢复");
    }

    @Test
    void tc24_quarryNoRecovery() {
        ResourcePoint p = ResourcePoint.fresh("q", "quarry", new Pos(0, 0, 0), 96, 0, 0).take(200);
        assertEquals(0, p.remaining());
        assertEquals(ResourcePoint.Tier.EMPTY, p.tier());
        assertEquals(0, p.recover(50).remaining());
        assertEquals(ResourcePoint.Tier.FULL, ResourcePoint.fresh("q", "quarry", null, 96, 0, 0).tier());
        assertEquals(ResourcePoint.Tier.HALF, ResourcePoint.fresh("q", "quarry", null, 96, 0, 0).take(40).tier());
        assertEquals(ResourcePoint.Tier.LOW, ResourcePoint.fresh("q", "quarry", null, 96, 0, 0).take(70).tier());
    }
}
