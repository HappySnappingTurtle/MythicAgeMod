package io.github.happysnappingturtle.mythicage.core.rite;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;

/**
 * 随时代变化的上限（T11a 设计文档 §6.2；总纲 §3.1、§3.6、§3.8）。神坛上限来自配置，由加载层另算。
 */
public final class AgeCaps {
    public static final int MYTHIC_UNITS_BASE = 2;
    public static final int MYTHIC_UNITS_MAX = 5;

    private AgeCaps() {
    }

    /** 全部文明中的最高时代。 */
    public static Age highest(PlayerMythData data) {
        Age best = Age.NONE;
        for (Civilization c : Civilization.values()) {
            if (data.civ(c).age().atLeast(best)) {
                best = data.civ(c).age();
            }
        }
        return best;
    }

    /** 神话单位上限：基础 2，最高时代每高于远古一级 +1，至多 5。 */
    public static int mythicUnits(Age highest) {
        int levels = Math.max(0, highest.ordinal() - Age.ARCHAIC.ordinal());
        return Math.min(MYTHIC_UNITS_MAX, MYTHIC_UNITS_BASE + levels);
    }

    /** 圣物饰品栏：基础 1；任意文明进入英雄 +1，进入神话再 +1。 */
    public static int relicSlots(Age highest) {
        return 1 + (highest.atLeast(Age.HEROIC) ? 1 : 0) + (highest.atLeast(Age.MYTHIC) ? 1 : 0);
    }
}
