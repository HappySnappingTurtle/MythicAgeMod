package io.github.happysnappingturtle.mythicage.core.temple;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-T8a-01～11：规则解析与等级判定，算例取自 {@code 希腊自由神庙规则算例.json}。 */
class TempleRulesTest {
    static TempleRules greek() {
        Path root = Path.of(System.getProperty("mythicage.rootDir", ".."));
        Path file = root.resolve("neoforge/src/main/resources/data/mythic_age/mythic_age/temple/greek.json");
        try {
            return TempleRules.parse(Files.readString(file));
        } catch (IOException e) {
            throw new IllegalStateException("读不到 " + file, e);
        }
    }

    /** 算例的 age_max_tier → 主人时代。 */
    private static Age ageForTier(int tier) {
        return switch (tier) {
            case 1 -> Age.ARCHAIC;
            case 2 -> Age.CLASSICAL;
            default -> Age.HEROIC;
        };
    }

    /** 算例输入：柱子围成 9×9（面积 81），全部站在台基上、台基可达、祭坛露天。 */
    static TempleSurvey survey(int columns, int roof, int braziers, int statues, int pediments, int floor) {
        return new TempleSurvey(columns, columns, roof, 81, true, true, braziers, statues, pediments, floor);
    }

    private static int level(int columns, int roof, int braziers, int statues, int pediments, int floor, int ageTier) {
        return greek().evaluate(survey(columns, roof, braziers, statues, pediments, floor), ageForTier(ageTier))
                .grantedLevel();
    }

    @Test
    void tc01ParsesDefault() {
        TempleRules r = greek();
        assertEquals(4, r.tiers().size());
        assertEquals(49, r.roof().need(81));
        assertEquals(6, r.roof().need(4), "面积不足 3×3 时按 9 计算需要数");
        assertEquals(4050, r.scan().volume());
        assertEquals(15, r.scan().sizeX());
        assertEquals(15, r.scan().sizeY());
        assertEquals(18, r.scan().sizeZ());
        assertEquals(new TempleRules.Requirement(4, true, true, false, 1, 0, 0, 0), r.tier(1).require());
        assertTrue(r.tier(2).require().crepidoma());
        assertEquals(Age.CLASSICAL, r.tier(2).age());
    }

    @Test
    void tc02RejectsBadInputWithFieldPath() {
        String base = """
                {"scan":{"halfWidth":7,"back":1,"front":16,"below":2,"above":12},"roof":{"minPercent":60,"minSide":3},"columnMinHeight":3,
                 "tiers":[%s]}""";
        String t0 = "{\"level\":0,\"name\":\"a\",\"age\":\"archaic\",\"prayerSlots\":2,\"coefficient\":1}";
        assertPath(base.formatted(t0 + ",{\"level\":2,\"name\":\"b\",\"age\":\"archaic\",\"prayerSlots\":2,\"coefficient\":1,\"require\":{}}"),
                "tiers[1].level");
        assertPath(base.formatted("{\"level\":0,\"name\":\"a\",\"age\":\"archaic\",\"prayerSlots\":2,\"coefficient\":0}"),
                "tiers[0].coefficient");
        assertPath(base.formatted("{\"level\":0,\"name\":\"a\",\"age\":\"golden\",\"prayerSlots\":2,\"coefficient\":1}"),
                "tiers[0].age");
        assertPath(base.formatted(t0 + ",{\"level\":1,\"name\":\"b\",\"age\":\"archaic\",\"prayerSlots\":4,\"coefficient\":1.5}"),
                "tiers[1].require");
        assertPath(base.formatted(t0).replace("\"minPercent\":60", "\"minPercent\":101"), "roof.minPercent");
        assertPath(base.formatted(t0).replace("\"front\":16", "\"front\":30"), "scan");
        assertPath(base.formatted(""), "tiers");
        assertPath("{oops", "(根)");
    }

    private static void assertPath(String json, String path) {
        TempleRules.TempleRulesException e = assertThrows(TempleRules.TempleRulesException.class, () -> TempleRules.parse(json));
        assertTrue(e.getMessage().startsWith(path + ":"), () -> "应以 " + path + " 开头：" + e.getMessage());
    }

    @Test
    void tc03to09WorkedExamples() {
        assertEquals(0, level(0, 0, 0, 0, 0, 0, 1), "GR-L0");
        assertEquals(1, level(4, 49, 1, 0, 0, 20, 1), "GR-L1");
        assertEquals(2, level(8, 60, 2, 1, 2, 40, 2), "GR-L2");
        assertEquals(3, level(14, 70, 4, 1, 4, 80, 3), "GR-L3");
        assertEquals(0, level(4, 48, 1, 0, 0, 20, 1), "GR-BAD-ROOF");
        assertEquals(0, level(8, 70, 0, 1, 2, 50, 2), "GR-BAD-FIRE");
        assertEquals(1, level(14, 70, 4, 1, 4, 80, 1), "GR-AGE-CAP");
    }

    @Test
    void tc07RoofMissingIsReported() {
        TempleVerdict v = greek().evaluate(survey(4, 48, 1, 0, 0, 20), Age.ARCHAIC);
        assertEquals(java.util.List.of(new TempleVerdict.Missing("roof", 48, 49)), v.missing());
    }

    @Test
    void tc08FireMissingIsReported() {
        TempleVerdict v = greek().evaluate(survey(8, 70, 0, 1, 2, 50), Age.CLASSICAL);
        assertEquals(0, v.structuralLevel());
        assertTrue(v.missing().contains(new TempleVerdict.Missing("litBraziers", 0, 1)));
    }

    @Test
    void tc09AgeCapReportsRequiredAge() {
        TempleVerdict v = greek().evaluate(survey(14, 70, 4, 1, 4, 80), Age.ARCHAIC);
        assertEquals(3, v.structuralLevel());
        assertEquals(1, v.grantedLevel());
        assertEquals(Optional.of(Age.CLASSICAL), v.ageRequired());
        assertTrue(v.missing().isEmpty(), "结构已是最高级，没有结构缺失项");
    }

    @Test
    void tc10NoneAgeStillOpenAltar() {
        TempleVerdict v = greek().evaluate(survey(4, 49, 1, 0, 0, 0), Age.NONE);
        assertEquals(1, v.structuralLevel());
        assertEquals(0, v.grantedLevel());
        assertEquals(Optional.of(Age.ARCHAIC), v.ageRequired());
    }

    @Test
    void tc11ExactThresholdsPass() {
        assertEquals(2, level(8, 49, 2, 1, 2, 40, 2));
        assertEquals(1, level(7, 49, 2, 1, 2, 40, 2), "7 柱只到 1 级");
    }

    @Test
    void rev01CoveredAltarFallsToZero() {
        TempleSurvey s = new TempleSurvey(8, 8, 60, 81, false, true, 2, 1, 2, 40);
        TempleVerdict v = greek().evaluate(s, Age.CLASSICAL);
        assertEquals(0, v.grantedLevel());
        assertTrue(v.missing().contains(new TempleVerdict.Missing("openAltar", 0, 1)));
    }

    @Test
    void rev01ClassicalNeedsCrepidoma() {
        TempleSurvey ground = new TempleSurvey(8, 0, 60, 81, true, false, 2, 1, 2, 40);
        TempleVerdict v = greek().evaluate(ground, Age.CLASSICAL);
        assertEquals(1, v.grantedLevel(), "柱子贴地只到小神庙");
        assertTrue(v.missing().contains(new TempleVerdict.Missing("raisedColumns", 0, 8)));
        assertTrue(v.missing().contains(new TempleVerdict.Missing("platformAccess", 0, 1)));
        TempleSurvey noStairs = new TempleSurvey(8, 8, 60, 81, true, false, 2, 1, 2, 40);
        assertEquals(1, greek().evaluate(noStairs, Age.CLASSICAL).grantedLevel(), "台基走不上去也不行");
    }

    @Test
    void rev01RoofNeedsEnclosedArea() {
        TempleRules.Roof roof = greek().roof();
        assertTrue(!roof.satisfied(8, 0), "柱子没有围出 3×3 时不合格");
        assertTrue(roof.satisfied(9, 15));
        assertTrue(!roof.satisfied(8, 15));
    }
}
