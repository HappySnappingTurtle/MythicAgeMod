package io.github.happysnappingturtle.mythicage.core.relic;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** TC-GA13-07：每人每村的供奉位各自独立；同一件圣物不能同时在两处。 */
class TempleRelicRulesTest {
    @Test
    void slotsPerVillageAreIndependent() {
        int cap = RelicSlots.offerSlots(Age.CLASSICAL);
        assertEquals(2, cap);
        // 甲村放满 2 件，乙村还是 0 件：乙村照样能放
        assertEquals(TempleRelicRules.Refusal.FULL, TempleRelicRules.check(true, true, false, 2, cap));
        assertEquals(TempleRelicRules.Refusal.NONE, TempleRelicRules.check(true, true, false, 0, cap));
        assertEquals(TempleRelicRules.Refusal.NONE, TempleRelicRules.check(true, true, false, 1, cap));
    }

    @Test
    void sameRelicOnlyOnce() {
        // 已在甲村供奉的那一件，拿去乙村被拒（绑定表里有它）
        assertEquals(TempleRelicRules.Refusal.OCCUPIED, TempleRelicRules.check(true, true, true, 0, 2));
    }

    @Test
    void refusalOrder() {
        assertEquals(TempleRelicRules.Refusal.NOT_DEVOTEE, TempleRelicRules.check(false, false, true, 9, 0));
        assertEquals(TempleRelicRules.Refusal.WRONG_CIV, TempleRelicRules.check(true, false, true, 9, 0));
        assertEquals(TempleRelicRules.Refusal.OCCUPIED, TempleRelicRules.check(true, true, true, 9, 0));
        assertEquals(TempleRelicRules.Refusal.FULL, TempleRelicRules.check(true, true, false, 0, 0));
    }

    @Test
    void capsByAge() {
        assertEquals(1, RelicSlots.offerSlots(Age.ARCHAIC));
        assertEquals(2, RelicSlots.offerSlots(Age.HEROIC));
        assertEquals(3, RelicSlots.offerSlots(Age.MYTHIC));
    }
}
