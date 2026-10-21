package io.github.happysnappingturtle.mythicage.core.summon;

import java.util.Locale;
import java.util.Optional;

/** 三种指令模式（总纲 §3.6、T14 设计文档 §4.1）。 */
public enum SummonMode {
    /** 跟在主人身边；打攻击主人 / 被主人攻击的目标与附近敌对生物。 */
    FOLLOW(16),
    /** 守在驻守点 12 格内；打进入 16 格的敌对生物，追出 24 格就回来。 */
    GUARD(16),
    /** 主动攻击 24 格内的敌对生物。 */
    FREE(24);

    /** 主动索敌半径（格）。 */
    private final int aggroRange;

    SummonMode(int aggroRange) {
        this.aggroRange = aggroRange;
    }

    public int aggroRange() {
        return aggroRange;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<SummonMode> byId(String id) {
        for (SummonMode m : values()) {
            if (m.id().equals(id)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    public static final int GUARD_RADIUS = 12;
    public static final int GUARD_LEASH = 24;
    public static final int FOLLOW_TELEPORT = 24;
}
