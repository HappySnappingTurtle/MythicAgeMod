package io.github.happysnappingturtle.mythicage.core.conduct;

import io.github.happysnappingturtle.mythicage.core.conduct.ConductDisplay.Tier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 善恶显示分档（TC-T24-17、18）。 */
class ConductDisplayTest {
    @Test
    void tc17TierEdges() {
        // 范围 −100…+100：正直 ≥ 15、仁德 ≥ 50；负半边对称
        assertEquals(Tier.ORDINARY, ConductDisplay.tier(0, -100, 100));
        assertEquals(Tier.ORDINARY, ConductDisplay.tier(14, -100, 100));
        assertEquals(Tier.UPRIGHT, ConductDisplay.tier(15, -100, 100));
        assertEquals(Tier.UPRIGHT, ConductDisplay.tier(49, -100, 100));
        assertEquals(Tier.VIRTUOUS, ConductDisplay.tier(50, -100, 100));
        assertEquals(Tier.ORDINARY, ConductDisplay.tier(-14, -100, 100));
        assertEquals(Tier.DISREPUTABLE, ConductDisplay.tier(-15, -100, 100));
        assertEquals(Tier.INFAMOUS, ConductDisplay.tier(-50, -100, 100));
        assertEquals(Tier.INFAMOUS, ConductDisplay.tier(-100, -100, 100));
        // 不对称范围：各半边按自己的长度
        assertEquals(Tier.VIRTUOUS, ConductDisplay.tier(10, -100, 20));
        assertEquals(Tier.DISREPUTABLE, ConductDisplay.tier(-15, -100, 20));
        // 极小范围也有正直档（至少 1）
        assertEquals(Tier.VIRTUOUS, ConductDisplay.tier(1, -1, 1));
    }

    @Test
    void tc18NewestFirstLimited() {
        List<Integer> oldToNew = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        List<Integer> r = ConductDisplay.newestFirst(oldToNew, ConductDisplay.RECENT);
        assertEquals(10, r.size());
        assertEquals(15, r.get(0));
        assertEquals(6, r.get(9));
        assertEquals(List.of(), ConductDisplay.newestFirst(List.of(), 10));
    }
}
