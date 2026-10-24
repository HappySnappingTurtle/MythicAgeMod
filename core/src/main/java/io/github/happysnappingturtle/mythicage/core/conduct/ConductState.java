package io.github.happysnappingturtle.mythicage.core.conduct;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 善恶值的辅助状态（T18 设计文档 §4.2），挂在玩家数据上：事件账本、在线刻累计、已领治愈奖励的村民、当日已得正分。
 * 分数本身仍是 {@code PlayerMythData.conductScore}。
 */
public final class ConductState {
    public static final int CURED_LIMIT = 256;

    private final IncidentLedger incidents = new IncidentLedger();
    private long onlineTicks;
    private final Set<UUID> cured = new LinkedHashSet<>();
    private long positiveDay = -1;
    private int positiveGranted;

    public IncidentLedger incidents() {
        return incidents;
    }

    public long onlineTicks() {
        return onlineTicks;
    }

    public void setOnlineTicks(long ticks) {
        this.onlineTicks = ticks;
    }

    /** 第一次领这个村民的治愈奖励时返回 true 并记下。 */
    public boolean markCured(UUID villager) {
        if (cured.contains(villager)) {
            return false;
        }
        cured.add(villager);
        while (cured.size() > CURED_LIMIT) {
            cured.remove(cured.iterator().next());
        }
        return true;
    }

    public Set<UUID> cured() {
        return Collections.unmodifiableSet(cured);
    }

    /** 某 MC 日已得的正分（换日后为 0）。 */
    public int positiveGranted(long today) {
        return positiveDay == today ? positiveGranted : 0;
    }

    public void addPositive(long today, int points) {
        if (positiveDay != today) {
            positiveDay = today;
            positiveGranted = 0;
        }
        positiveGranted += points;
    }

    public long positiveDay() {
        return positiveDay;
    }

    /** 读档用。 */
    public void restore(long onlineTicks, Set<UUID> cured, long positiveDay, int positiveGranted) {
        this.onlineTicks = onlineTicks;
        this.cured.clear();
        this.cured.addAll(cured);
        this.positiveDay = positiveDay;
        this.positiveGranted = positiveGranted;
    }

    public void copyFrom(ConductState o) {
        incidents.copyFrom(o.incidents);
        restore(o.onlineTicks, o.cured, o.positiveDay, o.positiveGranted);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ConductState s && incidents.equals(s.incidents) && onlineTicks == s.onlineTicks
                && cured.equals(s.cured) && positiveDay == s.positiveDay && positiveGranted == s.positiveGranted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidents, onlineTicks, cured, positiveDay, positiveGranted);
    }
}
