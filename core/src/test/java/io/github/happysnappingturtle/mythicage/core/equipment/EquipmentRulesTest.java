package io.github.happysnappingturtle.mythicage.core.equipment;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T17 单元测试 TC-T17-01～09：方阵、反击窗口、拉弓、装备数值表。 */
class EquipmentRulesTest {
    @Test
    void tc01_phalanxCap() {
        assertEquals(0, PhalanxRules.bonus(0, 1, 3));
        assertEquals(1, PhalanxRules.bonus(1, 1, 3));
        assertEquals(3, PhalanxRules.bonus(3, 1, 3));
        assertEquals(3, PhalanxRules.bonus(5, 1, 3), "封顶 +3");
    }

    @Test
    void tc02_counterWindowEdge() {
        CounterWindow w = new CounterWindow();
        w.open(100, 10);
        assertTrue(w.consume(110), "第 10 刻仍在窗口内");
        CounterWindow late = new CounterWindow();
        late.open(100, 10);
        assertFalse(late.consume(111), "第 11 刻不加");
    }

    @Test
    void tc03_counterOnlyOnce() {
        CounterWindow w = new CounterWindow();
        w.open(100, 10);
        assertTrue(w.consume(103));
        assertFalse(w.consume(105), "窗口内第二次不加");
    }

    @Test
    void tc04_reopen() {
        CounterWindow w = new CounterWindow();
        w.open(100, 10);
        w.consume(101);
        w.open(200, 10);
        assertFalse(w.consume(199), "新窗口之前的时刻不算");
        assertTrue(w.consume(210));
        assertFalse(new CounterWindow().consume(0), "从没格挡过");
    }

    @Test
    void tc05_drawPower() {
        assertEquals(0f, DrawRules.power(0, 18));
        assertEquals(1f, DrawRules.power(18, 18), 1e-6);
        assertEquals(1f, DrawRules.power(30, 18), 1e-6);
        assertEquals(DrawRules.power(20, DrawRules.VANILLA_FULL_TICKS), DrawRules.power(18, 18), 1e-6,
                "18 刻满弓 = 原版 20 刻满弓");
        assertEquals(DrawRules.power(10, 20), DrawRules.power(9, 18), 1e-6, "拉到一半也按比例");
    }

    @Test
    void tc06_parseErrors() {
        assertMessage("points", () -> EquipmentTable.parse("{\"armor\":{\"x\":{\"durability\":[1,1,1,1]}}}"));
        assertMessage("4 个", () -> EquipmentTable.parse("{\"armor\":{\"x\":{\"points\":[1,2,3],\"durability\":[1,1,1,1]}}}"));
        assertMessage("damage", () -> EquipmentTable.parse("{\"weapons\":{\"x\":{\"damage\":-1,\"speed\":1,\"durability\":5}}}"));
        assertMessage("speed", () -> EquipmentTable.parse("{\"weapons\":{\"x\":{\"damage\":1,\"speed\":0,\"durability\":5}}}"));
        assertMessage("durability", () -> EquipmentTable.parse("{\"weapons\":{\"x\":{\"damage\":1,\"speed\":1}}}"));
    }

    private static void assertMessage(String part, org.junit.jupiter.api.function.Executable e) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, e);
        assertTrue(ex.getMessage().contains(part), "消息应指出 " + part + "：" + ex.getMessage());
    }

    @Test
    void tc06_realTable() throws Exception {
        Path root = Path.of(System.getProperty("mythicage.rootDir", "."));
        EquipmentTable t = EquipmentTable.parse(Files.readString(
                root.resolve("neoforge/src/main/resources/mythic_age/equipment/greek.json")));
        EquipmentTable.ArmorSet bronze = t.armor().get("bronze");
        assertArrayEquals(new int[]{2, 7, 5, 3}, bronze.points());
        assertEquals(17, bronze.total());
        assertFalse(t.weapon("xiphos").orElseThrow().sweep());
        assertFalse(t.weapon("dory").orElseThrow().sweep(), "没写 sweep 时默认不横扫");
        assertTrue(t.weapon("olive_club").orElseThrow().blunt());
        assertEquals(18, t.weapon("composite_bow").orElseThrow().drawTicks());
        assertEquals(4, t.sets().get("phalanx").pieces());
    }

    @Test
    void tc07_panelToModifier() {
        assertEquals(5.0, EquipmentTable.damageModifier(6), 1e-9);
        assertEquals(-2.2, EquipmentTable.speedModifier(1.8), 1e-9);
    }

    @Test
    void tc08_bronzeDurability() {
        assertArrayEquals(new int[]{248, 360, 338, 293}, EquipmentTable.durability(22.5));
    }

    @Test
    void tc09_xiphosCooldown() {
        assertEquals(11, (int) (20 / 1.8));
    }
}
