package io.github.happysnappingturtle.mythicage.core.village.quest;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 任务的生命周期（T21 设计文档 §3.2，02 号 §6）：
 * <pre>
 * OPEN ⇄ PAUSED
 * OPEN → COMPLETE / INVALID / EXPIRED / FAILED；PAUSED → INVALID / EXPIRED / FAILED
 * COMPLETE / INVALID / EXPIRED → SETTLED（每名贡献者都结算了）
 * SETTLED、FAILED 是终态
 * </pre>
 * 暂停时不能直接完成：先恢复。
 */
public enum QuestState {
    OPEN, PAUSED, COMPLETE, INVALID, EXPIRED, FAILED, SETTLED;

    private static final Map<QuestState, Set<QuestState>> NEXT = Map.of(
            OPEN, EnumSet.of(PAUSED, COMPLETE, INVALID, EXPIRED, FAILED),
            PAUSED, EnumSet.of(OPEN, INVALID, EXPIRED, FAILED),
            COMPLETE, EnumSet.of(SETTLED),
            INVALID, EnumSet.of(SETTLED),
            EXPIRED, EnumSet.of(SETTLED),
            FAILED, EnumSet.noneOf(QuestState.class),
            SETTLED, EnumSet.noneOf(QuestState.class));

    public boolean canMoveTo(QuestState to) {
        return NEXT.get(this).contains(to);
    }

    /** 还在进行（占接取名额、能交付）：OPEN 或 PAUSED。 */
    public boolean active() {
        return this == OPEN || this == PAUSED;
    }

    /** 已结束、等着给贡献者结算：COMPLETE、INVALID、EXPIRED。 */
    public boolean ended() {
        return this == COMPLETE || this == INVALID || this == EXPIRED;
    }

    /** 终态：不会再变。 */
    public boolean terminal() {
        return this == SETTLED || this == FAILED;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static QuestState byId(String id) {
        for (QuestState s : values()) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return OPEN;
    }
}
