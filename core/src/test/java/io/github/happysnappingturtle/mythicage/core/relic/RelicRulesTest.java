package io.github.happysnappingturtle.mythicage.core.relic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.PrayerFormula;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.FavorRules;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T16 单元测试 TC-T16-01～17：圣物绑定表、槽位、效果组、减伤、圣物卡。 */
class RelicRulesTest {
    private static final String A = "greek:rheia_crown";
    private static final String B = "greek:pygmalion_statue";
    private static final UUID I1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID I2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static RelicBinding worn(UUID i) {
        return new RelicBinding(RelicState.WORN, i, Civilization.GREEK, RelicBinding.curioWhere(0));
    }

    private static RelicBinding offered(UUID i, Civilization civ) {
        return new RelicBinding(RelicState.OFFERED, i, civ, RelicBinding.shrineWhere("minecraft:overworld", 1, 64, 2));
    }

    @Test
    void tc01_curioSlots() {
        assertEquals(1, RelicSlots.curioSlots(List.of(Age.NONE, Age.NONE)));
        assertEquals(1, RelicSlots.curioSlots(List.of(Age.CLASSICAL, Age.NONE)));
        assertEquals(2, RelicSlots.curioSlots(List.of(Age.HEROIC, Age.ARCHAIC)));
        assertEquals(3, RelicSlots.curioSlots(List.of(Age.HEROIC, Age.MYTHIC)));
        assertEquals(3, RelicSlots.curioSlots(List.of(Age.MYTHIC)));
    }

    @Test
    void tc02_offerSlots() {
        assertEquals(0, RelicSlots.offerSlots(Age.NONE));
        assertEquals(1, RelicSlots.offerSlots(Age.ARCHAIC));
        assertEquals(2, RelicSlots.offerSlots(Age.CLASSICAL));
        assertEquals(2, RelicSlots.offerSlots(Age.HEROIC));
        assertEquals(3, RelicSlots.offerSlots(Age.MYTHIC));
    }

    @Test
    void tc03_bindWorn() {
        RelicBook book = new RelicBook();
        assertTrue(book.bind(A, worn(I1)).ok());
        assertEquals(RelicState.WORN, book.state(A));
        assertEquals(RelicState.INACTIVE, book.state(B));
    }

    @Test
    void tc04_uniquenessByRelicId() {
        RelicBook book = new RelicBook();
        book.bind(A, worn(I1));
        RelicBook.BindResult sameInstance = book.bind(A, offered(I1, Civilization.GREEK));
        RelicBook.BindResult otherInstance = book.bind(A, offered(I2, Civilization.GREEK));
        assertFalse(sameInstance.ok());
        assertFalse(otherInstance.ok());
        assertEquals(RelicState.WORN, otherInstance.occupiedBy().orElseThrow().state(), "返回当前位置");
        assertEquals(worn(I1), book.get(A).orElseThrow(), "表不变");
    }

    @Test
    void tc05_mustPassThroughInactive() {
        RelicBook book = new RelicBook();
        book.bind(A, offered(I1, Civilization.GREEK));
        assertFalse(book.bind(A, worn(I1)).ok());
        assertTrue(book.release(A, I1));
        assertTrue(book.bind(A, worn(I1)).ok());
    }

    @Test
    void tc06_releaseNeedsSameInstance() {
        RelicBook book = new RelicBook();
        book.bind(A, worn(I1));
        assertFalse(book.release(A, I2));
        assertEquals(RelicState.WORN, book.state(A));
    }

    @Test
    void tc07_playersIndependent() {
        RelicBook p1 = new RelicBook();
        RelicBook p2 = new RelicBook();
        assertTrue(p1.bind(A, worn(I1)).ok());
        assertTrue(p2.bind(A, worn(I2)).ok());
    }

    @Test
    void tc08_offerUsageByCiv() {
        RelicBook book = new RelicBook();
        book.bind(A, offered(I1, Civilization.GREEK));
        book.bind("egyptian:ankh_of_ra", offered(I2, Civilization.EGYPTIAN));
        book.bind(B, worn(UUID.randomUUID()));
        assertEquals(1, book.used(RelicState.OFFERED, Civilization.GREEK));
        assertEquals(1, book.used(RelicState.OFFERED, Civilization.EGYPTIAN));
    }

    @Test
    void tc09_offerCapacityBoundary() {
        RelicBook book = new RelicBook();
        book.bind(A, offered(I1, Civilization.GREEK));
        int used = book.used(RelicState.OFFERED, Civilization.GREEK);
        assertFalse(used < RelicSlots.offerSlots(Age.ARCHAIC), "远古 1 / 1 已满");
        assertTrue(used < RelicSlots.offerSlots(Age.CLASSICAL), "古典 1 / 2 可放");
        book.bind(B, offered(I2, Civilization.GREEK));
        assertFalse(book.used(RelicState.OFFERED, Civilization.GREEK) < RelicSlots.offerSlots(Age.CLASSICAL), "2 / 2 已满");
    }

    @Test
    void tc10_effectGroupMerge() {
        Map<String, Double> sameGroup = EffectGroups.merge(List.of(
                new EffectGroups.Effect("prayer.R.greek", 0.10), new EffectGroups.Effect("prayer.R.greek", 0.15)));
        assertEquals(0.15, sameGroup.get("prayer.R.greek"), 1e-9, "同组取最大值");
        Map<String, Double> two = Map.of("g1", 0.10, "g2", 0.05);
        assertEquals(0.15, EffectGroups.bonus(two, List.of("g1", "g2")), 1e-9, "不同组增益相加");
        assertEquals(0.218, 1 - EffectGroups.remaining(List.of(0.08, 0.15)), 1e-9, "不同组减伤相乘");
    }

    @Test
    void tc11_meleeTruthTable() {
        assertTrue(DamageRules.isMelee(true, true, false, false, false), "生物直接攻击");
        assertTrue(DamageRules.isMelee(true, true, false, false, false), "玩家横扫（玩家攻击伤害源）");
        assertFalse(DamageRules.isMelee(false, false, true, false, false), "箭");
        assertFalse(DamageRules.isMelee(false, false, true, false, false), "三叉戟");
        assertFalse(DamageRules.isMelee(true, true, false, true, false), "苦力怕爆炸");
        assertFalse(DamageRules.isMelee(true, true, false, false, true), "荆棘");
        assertFalse(DamageRules.isMelee(false, false, false, false, false), "凋零效果（无直接实体）");
    }

    @Test
    void tc12_reduceOrder() {
        assertEquals(7.2f, DamageRules.reduce(10, List.of(0.08), 2), 1e-5, "先乘 −8%，再减 2");
        assertEquals(0f, DamageRules.reduce(1, List.of(0.08), 2), 1e-5, "固定值大于剩余伤害时为 0");
        assertEquals(9.2f, DamageRules.reduce(10, List.of(0.08), 0), 1e-5);
    }

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void tc13_cardParse() {
        RelicCard c = RelicCard.parse("greek", "rheia_crown", json("""
                {"item":"mythic_age:rheia_crown","civ":"greek",
                 "worn":[{"group":"favor_cap.greek","value":50}],
                 "offered":[{"group":"prayer.R.greek","value":0.15}]}"""));
        assertEquals("greek:rheia_crown", c.id());
        assertEquals(50, c.value(RelicState.WORN, "favor_cap.greek"));
        assertEquals(0.15, c.value(RelicState.OFFERED, "prayer.R.greek"), 1e-9);
        assertEquals(0, c.value(RelicState.OFFERED, "favor_cap.greek"));

        assertMessage("item", () -> RelicCard.parse("greek", "x", json("{\"civ\":\"greek\"}")));
        assertMessage("civ", () -> RelicCard.parse("greek", "x", json("{\"item\":\"a:b\",\"civ\":\"grek\"}")));
        assertMessage("效果组", () -> RelicCard.parse("greek", "x", json(
                "{\"item\":\"a:b\",\"civ\":\"greek\",\"worn\":[{\"group\":\"nope\",\"value\":1}]}")));
        assertMessage("为负", () -> RelicCard.parse("greek", "x", json(
                "{\"item\":\"a:b\",\"civ\":\"greek\",\"worn\":[{\"group\":\"favor_cap.greek\",\"value\":-1}]}")));
        assertMessage("百分比", () -> RelicCard.parse("greek", "x", json(
                "{\"item\":\"a:b\",\"civ\":\"greek\",\"offered\":[{\"group\":\"prayer.R.greek\",\"value\":1.5}]}")));
        assertMessage("未知文明", () -> RelicCard.parse("lemuria", "x", json("{\"item\":\"a:b\",\"civ\":\"lemuria\"}")));
    }

    private static void assertMessage(String part, org.junit.jupiter.api.function.Executable e) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, e);
        assertTrue(ex.getMessage().contains(part), "消息应指出 " + part + "：" + ex.getMessage());
    }

    @Test
    void tc14_duplicateItem() {
        RelicCard a = new RelicCard("greek:a", "mythic_age:x", Civilization.GREEK, List.of(), List.of());
        RelicCard b = new RelicCard("greek:b", "mythic_age:x", Civilization.GREEK, List.of(), List.of());
        java.util.List<String> errors = new java.util.ArrayList<>();
        Map<String, RelicCard> out = RelicCard.dedupe(List.of(a, b), errors::add);
        assertEquals(List.of("greek:a"), List.copyOf(out.keySet()));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("greek:b"));
    }

    @Test
    void tc15_bookCopyAndEquals() {
        PlayerMythData d = new PlayerMythData();
        d.relics().bind(A, worn(I1));
        d.relics().bind(B, offered(I2, Civilization.GREEK));
        PlayerMythData c = d.copy();
        assertEquals(d, c);
        c.relics().remove(A);
        assertNotEquals(d, c, "深拷贝");
        assertEquals(RelicState.WORN, d.relics().state(A));
    }

    @Test
    void tc16_rheiaCapExcessKept() {
        long withCrown = FavorRules.cap(Age.MYTHIC, 50);
        assertEquals(350, withCrown);
        long cap = FavorRules.cap(Age.MYTHIC, 0);
        assertEquals(330, FavorRules.add(330, 10, cap), "卸下后保留、不再增长");
        long after = FavorRules.spend(330, 40).orElseThrow();
        assertEquals(290, after);
        assertEquals(300, FavorRules.add(after, 20, cap));
    }

    @Test
    void tc17_rClamp() {
        assertEquals(0.15, PrayerFormula.clampR(0.15 + 0.10), 1e-9);
        assertEquals(0.15, PrayerFormula.clampR(0.15), 1e-9);
    }
}
