package io.github.happysnappingturtle.mythicage.core.boss;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 试炼竞技场的阶段（T23 §4.2）：IDLE（门关着）→ OPEN（门开了、Boss 未出）→ FIGHT_P1 → FIGHT_P2 → VICTORY → RESETTING → IDLE。
 * 战斗中空场、OPEN 超时都回到 RESETTING / IDLE。
 */
public enum ArenaPhase {
    IDLE, OPEN, FIGHT_P1, FIGHT_P2, VICTORY, RESETTING;

    private static final Map<ArenaPhase, Set<ArenaPhase>> NEXT = Map.of(
            IDLE, EnumSet.of(OPEN),
            OPEN, EnumSet.of(FIGHT_P1, IDLE),
            FIGHT_P1, EnumSet.of(FIGHT_P2, VICTORY, RESETTING),
            FIGHT_P2, EnumSet.of(VICTORY, RESETTING),
            VICTORY, EnumSet.of(RESETTING),
            RESETTING, EnumSet.of(IDLE));

    public boolean fighting() {
        return this == FIGHT_P1 || this == FIGHT_P2;
    }

    public boolean canGo(ArenaPhase to) {
        return NEXT.get(this).contains(to);
    }

    /** 合法就返回 to，否则抛异常。 */
    public ArenaPhase go(ArenaPhase to) {
        if (!canGo(to)) {
            throw new IllegalStateException("竞技场阶段不能从 " + this + " 到 " + to);
        }
        return to;
    }
}
