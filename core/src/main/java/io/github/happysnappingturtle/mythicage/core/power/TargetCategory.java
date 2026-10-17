package io.github.happysnappingturtle.mythicage.core.power;

import java.util.Locale;
import java.util.Optional;

/**
 * 目标矩阵的 12 列（05 号 §5）。JSON 里用小写 id。
 */
public enum TargetCategory {
    CASTER, OWN_VILLAGER, OWN_SOLDIER, OTHER_VILLAGE, OWN_SUMMON, OTHER_SUMMON, GUARDIAN, NEUTRAL_ANIMAL, HOSTILE, ELITE,
    BOSS, ENEMY_PLAYER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TargetCategory> byId(String id) {
        for (TargetCategory c : values()) {
            if (c.id().equals(id)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
