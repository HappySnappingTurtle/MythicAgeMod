package io.github.happysnappingturtle.mythicage.core.rig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RigDefinitionTest {
    private static final String MINOTAUR = """
            {
              "model": "mythic_age:minotaur",
              "collision": {"width": 1.1, "height": 2.5},
              "locators": {
                "axe_grip_l": [10, 12, 1.5], "stone_grip_r": [-10, 12, 1.5],
                "snort": [0, 34, 10], "horn_contact_visual": [0, 39, 4.5]
              },
              "variants": {"boss": {"scale": 1.4, "texture": "mythic_age:textures/entity/minotaur/minotaur_boss.png"}}
            }
            """;
    private static final double EPS = 1e-6;

    private static RigDefinition rig() {
        return RigDefinition.parse(MINOTAUR);
    }

    @Test
    void tc01ParsesMinotaur() {
        RigDefinition r = rig();
        assertEquals("mythic_age:minotaur", r.model());
        assertEquals(1.1, r.width(), EPS);
        assertEquals(2.5, r.height(), EPS);
        assertEquals(4, r.locators().size());
        assertEquals(1.4, r.scaleOf("boss"), EPS);
        assertEquals(1.0, r.scaleOf(null), EPS);
        assertEquals(1.0, r.scaleOf("unknown"), EPS);
    }

    @Test
    void tc02RejectsBadInputWithFieldPath() {
        assertMessage("{\"collision\": {\"width\": 1, \"height\": 2}}", "model");
        assertMessage("{\"model\": \"a:b\"}", "collision");
        assertMessage("{\"model\": \"a:b\", \"collision\": {\"width\": 1}}", "collision.height");
        assertMessage("{\"model\": \"a:b\", \"collision\": {\"width\": 0, \"height\": 2}}", "collision.width");
        assertMessage("{\"model\": \"a:b\", \"collision\": {\"width\": 1, \"height\": 2}, \"locators\": {\"x\": [1, 2]}}", "locators.x");
        assertMessage("{\"model\": \"a:b\", \"collision\": {\"width\": 1, \"height\": 2}, \"locators\": {\"x\": [1, \"a\", 3]}}", "locators.x[1]");
        assertMessage("{\"model\": \"a:b\", \"collision\": {\"width\": 1, \"height\": 2}, \"variants\": {\"boss\": {\"scale\": -1}}}", "variants.boss.scale");
        assertMessage("{\"model\": \"a:b\", \"collision\": {\"width\": 1, \"height\": 2}, \"variants\": {\"boss\": {\"scale\": 0}}}", "variants.boss.scale");
        assertMessage("{oops", "(根)");
    }

    private static void assertMessage(String json, String pathPrefix) {
        RigDefinition.RigParseException e = assertThrows(RigDefinition.RigParseException.class, () -> RigDefinition.parse(json));
        assertTrue(e.getMessage().startsWith(pathPrefix + ":"), () -> "消息应以 " + pathPrefix + " 开头：" + e.getMessage());
    }

    @Test
    void tc03Yaw0RightHandIsWorldMinusX() {
        // 偏航 0°：面朝 +Z，角色左 = +X，右手挂点在 -X 一侧、略偏前
        double[] w = rig().locatorWorld("stone_grip_r", 0, 0, 0, 0, 1.0);
        assertEquals(-10 / 16.0, w[0], EPS);
        assertEquals(12 / 16.0, w[1], EPS);
        assertEquals(1.5 / 16.0, w[2], EPS);
        double[] l = rig().locatorWorld("axe_grip_l", 0, 0, 0, 0, 1.0);
        assertEquals(10 / 16.0, l[0], EPS);
    }

    @Test
    void tc04SnortStaysInFront() {
        RigDefinition r = rig();
        for (double yaw : new double[]{0, 90, 180, -90, 37}) {
            double[] w = r.locatorWorld("snort", 5, 64, -3, yaw, 1.0);
            double fx = -Math.sin(Math.toRadians(yaw));
            double fz = Math.cos(Math.toRadians(yaw));
            double dx = w[0] - 5;
            double dz = w[2] + 3;
            assertEquals(10 / 16.0, dx * fx + dz * fz, EPS, "yaw " + yaw + " 时鼻息应在正前方 10u");
            assertEquals(0, dx * fz - dz * fx, EPS, "yaw " + yaw + " 时鼻息不应偏向两侧");
        }
        // 偏航 90° 面朝 -X：角色左 = +Z
        double[] l = r.locatorWorld("axe_grip_l", 0, 0, 0, 90, 1.0);
        assertEquals(10 / 16.0, l[2], EPS);
        assertEquals(-1.5 / 16.0, l[0], EPS);
    }

    @Test
    void tc05ScaleMultipliesDistanceAndCollision() {
        RigDefinition r = rig();
        double s = r.scaleOf("boss");
        double[] a = r.locatorWorld("horn_contact_visual", 0, 0, 0, 0, 1.0);
        double[] b = r.locatorWorld("horn_contact_visual", 0, 0, 0, 0, s);
        for (int i = 0; i < 3; i++) {
            assertEquals(a[i] * 1.4, b[i], EPS);
        }
        assertEquals(1.54, r.width() * s, 0.001);
        assertEquals(3.5, r.height() * s, 0.001);
    }

    @Test
    void unknownLocatorThrows() {
        assertThrows(IllegalArgumentException.class, () -> rig().locatorWorld("tail", 0, 0, 0, 0, 1));
    }

    @Test
    void headLookDefaultsAndOverrides() {
        assertEquals(new RigDefinition.HeadLook(50, 30), rig().headLook());
        assertEquals(new RigDefinition.HeadLook(30, 20),
                RigDefinition.parse(MINOTAUR.replace("\"collision\":", "\"headLook\": {\"yaw\":30,\"pitch\":20}, \"collision\":")).headLook());
    }

    @Test
    void headLookAllowsPartialOverridesAndZero() {
        assertEquals(new RigDefinition.HeadLook(0, 30),
                RigDefinition.parse(MINOTAUR.replace("\"collision\":", "\"headLook\": {\"yaw\":0}, \"collision\":")).headLook());
        assertEquals(new RigDefinition.HeadLook(50, 20),
                RigDefinition.parse(MINOTAUR.replace("\"collision\":", "\"headLook\": {\"pitch\":20}, \"collision\":")).headLook());
    }

    @Test
    void headLookRejectsInvalidLimits() {
        for (String value : new String[]{"-1", "1e100", "1e400", "\"NaN\"", "null"}) {
            for (String axis : new String[]{"yaw", "pitch"}) {
                String json = MINOTAUR.replace("\"collision\":", "\"headLook\": {\"" + axis + "\":" + value + "}, \"collision\":");
                assertMessage(json, "headLook." + axis);
            }
        }
    }
}
