package io.github.happysnappingturtle.mythicage.core.equipment;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** GA-8 装备数据、曲线与原版暴击前提，完全不依赖客户端。 */
class WeaponMotionsTest {
    private static EquipmentTable.Weapon weapon(String fields) {
        return EquipmentTable.parse("{\"weapons\":{\"test\":{\"durability\":375" + fields + "}}}").weapon("test").orElseThrow();
    }

    @Test
    void tc01_motionAndCritMotion() throws Exception {
        var weapon = weapon(",\"motion\":\"thrust\",\"critMotion\":\"cleave\"");
        assertEquals(WeaponMotion.THRUST, weapon.motion());
        assertEquals(WeaponMotion.CLEAVE, weapon.critMotion());
        Path root = Path.of(System.getProperty("mythicage.rootDir"));
        var actual = EquipmentTable.parse(Files.readString(root.resolve("neoforge/src/main/resources/mythic_age/equipment/greek.json")));
        assertEquals(WeaponMotion.THRUST, actual.weapon("xiphos").orElseThrow().motion());
        assertEquals(WeaponMotion.CLEAVE, actual.weapon("xiphos").orElseThrow().critMotion());
        assertEquals(WeaponMotion.THRUST, actual.weapon("dory").orElseThrow().motion());
    }

    @Test
    void tc02_missingDefaultsToSlash() {
        assertEquals(WeaponMotion.SLASH, weapon("").motion());
        assertEquals(WeaponMotion.SLASH, weapon("").critMotion());
        assertEquals(WeaponMotion.SLASH, weapon(",\"motion\":\"thrust\"").critMotion(), "两个可选字段独立默认");
        assertEquals(WeaponMotion.CLEAVE, weapon(",\"motion\":\"cleave\"").motion());
        assertEquals(WeaponMotion.SLASH, weapon(",\"motion\":\"slash\"").motion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"stab\"", "\"THRUST\"", "\"\"", "null", "17", "true", "{}", "[]"})
    void tc03_badMotionWarnsAndFallsBack(String invalid) {
        var warnings = new ArrayList<String>();
        var root = JsonParser.parseString("{\"weapons\":{\"test\":{\"durability\":1,\"motion\":" + invalid
                + ",\"critMotion\":" + invalid + "}}}").getAsJsonObject();
        var weapon = EquipmentTable.parse(root, warnings::add).weapon("test").orElseThrow();
        assertEquals(WeaponMotion.SLASH, weapon.motion());
        assertEquals(WeaponMotion.SLASH, weapon.critMotion());
        assertEquals(2, warnings.size());
        assertTrue(warnings.getFirst().contains("test") && warnings.getFirst().contains("motion"));
        assertTrue(warnings.get(1).contains("critMotion") && warnings.get(1).contains("slash"));
    }

    @Test
    void tc03_missingDoesNotWarn() {
        var warnings = new ArrayList<String>();
        EquipmentTable.parse(JsonParser.parseString("{\"weapons\":{\"test\":{\"durability\":1}}}").getAsJsonObject(), warnings::add);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void tc04_thrustKeyframesAndMonotonicSegments() {
        assertEquals(0, WeaponMotions.thrust(0), 1e-6);
        assertEquals(-0.10, WeaponMotions.thrust(0.3F), 1e-6);
        assertEquals(0.45, WeaponMotions.thrust(0.6F), 1e-6);
        assertEquals(0, WeaponMotions.thrust(1), 1e-6);
        for (int i = 1; i <= 100; i++) {
            float before = WeaponMotions.thrust((i - 1) / 100F);
            float after = WeaponMotions.thrust(i / 100F);
            assertTrue(i <= 30 || i > 60 ? after <= before + 1e-6 : after >= before - 1e-6, "分段单调：" + i);
        }
        assertEquals(0, WeaponMotions.thrust(-1), 1e-6);
        assertEquals(0, WeaponMotions.thrust(2), 1e-6);
        assertEquals(0, WeaponMotions.thrust(Float.NaN), 1e-6);
        assertEquals(5, WeaponMotion.THRUST.swingTicks(), "约 0.25 秒");
    }

    @Test
    void tc04_cleaveRaisesCutsAndReturns() {
        assertEquals(0, WeaponMotions.cleave(0), 1e-5);
        assertEquals(100, WeaponMotions.cleave(0.35F), 1e-5);
        assertEquals(-55, WeaponMotions.cleave(0.65F), 1e-5);
        assertEquals(0, WeaponMotions.cleave(1), 1e-5);
        for (int i = 1; i <= 100; i++) {
            float before = WeaponMotions.cleave((i - 1) / 100F);
            float after = WeaponMotions.cleave(i / 100F);
            assertTrue(i <= 35 || i > 65 ? after >= before - 1e-5 : after <= before + 1e-5);
        }
        assertEquals(0, WeaponMotions.envelope(0));
        assertEquals(1, WeaponMotions.envelope(0.3F));
        assertEquals(1, WeaponMotions.envelope(0.6F));
        assertEquals(0, WeaponMotions.envelope(1));
        assertEquals(6, WeaponMotion.CLEAVE.swingTicks(), "约 0.3 秒");
    }

    @Test
    void tc05_vanillaCriticalConditions() {
        assertTrue(critical(1, false, false, false, false, false, false, 1, true));
        assertFalse(critical(0, false, false, false, false, false, false, 1, true), "上升阶段");
        assertFalse(critical(1, true, false, false, false, false, false, 1, true), "落地");
        assertFalse(critical(1, false, true, false, false, false, false, 1, true), "攀爬");
        assertFalse(critical(1, false, false, true, false, false, false, 1, true), "水中");
        assertFalse(critical(1, false, false, false, true, false, false, 1, true), "失明");
        assertFalse(critical(1, false, false, false, false, true, false, 1, true), "骑乘");
        assertFalse(critical(1, false, false, false, false, false, true, 1, true), "疾跑");
        assertFalse(critical(1, false, false, false, false, false, false, 0.5F, true), "冷却未满");
        assertFalse(critical(1, false, false, false, false, false, false, 0.9F, true), "原版严格大于 0.9");
        assertTrue(critical(1, false, false, false, false, false, false, 0.9001F, true));
        assertFalse(critical(1, false, false, false, false, false, false, 1, false), "挥空或非活体目标");
    }

    private static boolean critical(float fall, boolean ground, boolean climb, boolean water, boolean blind,
                                    boolean passenger, boolean sprint, float strength, boolean living) {
        return CriticalHitRules.isCritical(fall, ground, climb, water, blind, passenger, sprint, strength, living);
    }
}
