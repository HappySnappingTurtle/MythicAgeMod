package io.github.happysnappingturtle.mythicage.core.village.life;

import io.github.happysnappingturtle.mythicage.core.village.time.VillageCalendar;

/**
 * 神谕所开闭（T19 设计文档 §1.10 J6，V3-希腊-居民 §5.6）：神谕日开；闭门的期（希腊：耕种期，冬季阿波罗离开德尔斐）闭门；
 * 其余日子关着并告诉玩家下一个神谕日是第几日、还有几日。
 */
public final class OracleRules {
    private OracleRules() {
    }

    public enum State { OPEN, CLOSED, WINTER }

    /** @param nextDay 下一个神谕日是一年中的第几日（今天开着时是今天之后的下一个）；{@code daysUntil} 还有几日。没有为 −1。 */
    public record Status(State state, int nextDay, int daysUntil) {
    }

    public static Status status(VillageCalendar cal, int offset, long mcDay, String tag, String closedPeriod) {
        VillageCalendar.Date d = cal.date(mcDay, offset);
        State s = !closedPeriod.isEmpty() && closedPeriod.equals(d.period()) ? State.WINTER
                : d.is(tag) ? State.OPEN : State.CLOSED;
        for (int i = 1; i <= cal.yearDays(); i++) {
            VillageCalendar.Date n = cal.date(mcDay + i, offset);
            if (n.is(tag)) {
                return new Status(s, n.dayOfYear(), i);
            }
        }
        return new Status(s, -1, -1);
    }
}
