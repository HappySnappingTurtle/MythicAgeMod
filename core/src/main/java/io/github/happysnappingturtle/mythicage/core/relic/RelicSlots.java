package io.github.happysnappingturtle.mythicage.core.relic;

import io.github.happysnappingturtle.mythicage.core.player.Age;

import java.util.Collection;

/** 槽位数量（总纲 §3.8）。 */
public final class RelicSlots {
    private RelicSlots() {
    }

    /** Curios 圣物槽：基础 1；任意文明进入英雄时代 +1，进入神话时代再 +1，最多 3。 */
    public static int curioSlots(Collection<Age> ages) {
        boolean heroic = false;
        boolean mythic = false;
        for (Age a : ages) {
            heroic |= a.atLeast(Age.HEROIC);
            mythic |= a == Age.MYTHIC;
        }
        return 1 + (heroic ? 1 : 0) + (mythic ? 1 : 0);
    }

    /** 某文明的供奉位：远古 1、古典 2、英雄 2、神话 3；未入门 0（06 号 §0.2 D4）。 */
    public static int offerSlots(Age age) {
        return switch (age) {
            case NONE -> 0;
            case ARCHAIC -> 1;
            case CLASSICAL, HEROIC -> 2;
            case MYTHIC -> 3;
        };
    }
}
