package io.github.happysnappingturtle.mythicage.core.wild;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T15 单元测试 TC-T15-01～05。 */
class WildRulesTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void tc01_parse() {
        WildCreatureCard c = WildCreatureCard.parse("greek", "centaur", json("""
                {"entity": "mythic_age:centaur", "attitude": "neutral", "spawn": "structure",
                 "biomes": ["#mythic_age:region/greek/arcadia"], "group": [3, 5], "density": {"area": 256, "max": 1}}"""));
        assertEquals("greek:centaur", c.key());
        assertEquals(WildCreatureCard.Attitude.NEUTRAL, c.attitude());
        assertEquals("mythic_age:region/greek/arcadia", c.biomes().get(0));
        assertEquals(3, c.minGroup());
        assertThrows(IllegalArgumentException.class, () -> WildCreatureCard.parse("greek", "x", json("""
                {"attitude": "neutral", "spawn": "structure"}""")), "缺 entity");
        assertThrows(IllegalArgumentException.class, () -> WildCreatureCard.parse("greek", "x", json("""
                {"entity": "a:b", "attitude": "neutral", "spawn": "structure", "group": [5, 3]}""")), "群下限大于上限");
        assertThrows(IllegalArgumentException.class, () -> WildCreatureCard.parse("greek", "x", json("""
                {"entity": "a:b", "attitude": "grumpy", "spawn": "structure"}""")), "态度写错");
        assertThrows(IllegalArgumentException.class, () -> WildCreatureCard.parse("greek", "x", json("""
                {"entity": "a:b", "attitude": "hostile", "spawn": "natural"}""")), "自然刷新却没有权重与群系");
    }

    @Test
    void tc02_villageAvoidDefaults() {
        WildCreatureCard hostile = WildCreatureCard.parse("greek", "h", json("""
                {"entity": "a:b", "attitude": "hostile", "spawn": "natural", "weight": 5, "biomes": ["a:b"]}"""));
        WildCreatureCard neutral = WildCreatureCard.parse("greek", "n", json("""
                {"entity": "a:b", "attitude": "neutral", "spawn": "structure"}"""));
        assertEquals(64, hostile.avoidVillages());
        assertEquals(0, neutral.avoidVillages());
        assertFalse(WildSpawnRules.villageAllows(64 * 64, 64), "正好 64 格：不生成");
        assertTrue(WildSpawnRules.villageAllows(65 * 65, 64));
        assertTrue(WildSpawnRules.villageAllows(Double.POSITIVE_INFINITY, 64), "没有村庄");
        assertTrue(WildSpawnRules.villageAllows(1, 0), "中立不排除");
    }

    @Test
    void tc03_density() {
        assertFalse(WildSpawnRules.densityAllows(1, 1));
        assertTrue(WildSpawnRules.densityAllows(0, 1));
        assertEquals(WildSpawnRules.areaKey(10, 10, 256), WildSpawnRules.areaKey(255, 200, 256), "同一区域");
        assertNotEquals(WildSpawnRules.areaKey(255, 0, 256), WildSpawnRules.areaKey(256, 0, 256), "相邻区域");
        assertNotEquals(WildSpawnRules.areaKey(-1, 0, 256), WildSpawnRules.areaKey(0, 0, 256), "负坐标另算一个区域");
    }

    /** TC-T15-06：小结构落点——每个区域恰好 1 个候选区块（max 1），至多 max 个；同种子结果不变；负坐标区域也一样。 */
    @Test
    void tc06_areaSite() {
        for (int[] area : new int[][]{{0, 0}, {-1, -1}, {3, -2}}) {
            int ones = 0;
            int twos = 0;
            for (int x = 0; x < 24; x++) {
                for (int z = 0; z < 24; z++) {
                    int cx = area[0] * 24 + x;
                    int cz = area[1] * 24 + z;
                    if (WildSpawnRules.isAreaSite(42L, "greek:stymphalian_bird", cx, cz, 384, 1)) {
                        ones++;
                    }
                    if (WildSpawnRules.isAreaSite(42L, "greek:stymphalian_bird", cx, cz, 384, 2)) {
                        twos++;
                    }
                }
            }
            assertEquals(1, ones, "max 1：区域里恰好 1 个候选区块");
            assertTrue(twos >= 1 && twos <= 2, "max 2：1–2 个，实际 " + twos);
        }
        // 同种子同结果；换种子或换生物，候选区块会变（不要求每次都变，只要 10 个区域里有变的）
        int moved = 0;
        for (int a = 0; a < 10; a++) {
            int site42 = -1;
            int site7 = -1;
            for (int i = 0; i < 16 * 16; i++) {
                int cx = a * 16 + i % 16;
                int cz = i / 16;
                if (WildSpawnRules.isAreaSite(42L, "greek:centaur", cx, cz, 256, 1)) {
                    assertTrue(WildSpawnRules.isAreaSite(42L, "greek:centaur", cx, cz, 256, 1), "可重复");
                    site42 = i;
                }
                if (WildSpawnRules.isAreaSite(7L, "greek:centaur", cx, cz, 256, 1)) {
                    site7 = i;
                }
            }
            if (site42 != site7) {
                moved++;
            }
        }
        assertTrue(moved > 0, "换种子后候选区块应有变化");
    }

    @Test
    void tc04_groupAggro() {
        assertTrue(GroupAggro.alertsGroup(GroupAggro.Source.PLAYER));
        assertTrue(GroupAggro.alertsGroup(GroupAggro.Source.PLAYER_SUMMON));
        assertTrue(GroupAggro.alertsGroup(GroupAggro.Source.PLAYER_PROJECTILE));
        assertFalse(GroupAggro.alertsGroup(GroupAggro.Source.OTHER_MOB));
        assertTrue(GroupAggro.retaliates(GroupAggro.Source.OTHER_MOB));
    }

    @Test
    void tc05_frontalArmor() {
        // 朝向 0° = 面朝 +Z
        assertEquals(0.2, WildCombatRules.frontalMultiplier(0, 0, 5, 120, 0.8), 1e-9, "正前方");
        double r59 = Math.toRadians(59);
        assertEquals(0.2, WildCombatRules.frontalMultiplier(0, -Math.sin(r59) * 5, Math.cos(r59) * 5, 120, 0.8), 1e-9, "59°");
        double r61 = Math.toRadians(61);
        assertEquals(1.0, WildCombatRules.frontalMultiplier(0, Math.sin(r61) * 5, Math.cos(r61) * 5, 120, 0.8), 1e-9, "61°");
        assertEquals(1.0, WildCombatRules.frontalMultiplier(0, 0, -5, 120, 0.8), 1e-9, "正后方");
        assertEquals(0.2, WildCombatRules.frontalMultiplier(90, -5, 0, 120, 0.8), 1e-9, "朝向 90° = 面朝 −X");
    }
}
