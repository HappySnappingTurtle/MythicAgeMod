package io.github.happysnappingturtle.mythicage.core.wild;

/**
 * 奥德修斯之计（EL-GR-02，T25a §4.3；《奥德赛》9.196–211、9.371–374）：独眼巨人·牧人闻到 {@value #NOTICE_RANGE} 格内的马洛涅烈酒
 * → 走过去 → 喝 {@value #DRINK_TICKS} 刻 → 醉倒 {@value #DRUNK_TICKS} 刻（期间受伤 ×{@value #DRUNK_MULTIPLIER}，D12，T25.1 窗口区）
 * → 醒；醒后 {@value #SOBER_TICKS} 刻内对酒没兴趣（D11）。喝酒途中挨打就中断，不醉倒，酒碗还在。
 */
public final class DrunkRules {
    public static final double NOTICE_RANGE = 16.0;
    public static final int DRINK_TICKS = 40;
    public static final int DRUNK_TICKS = 160;
    public static final int SOBER_TICKS = 600;
    public static final double DRUNK_MULTIPLIER = 3.0;

    public enum Phase {
        SOBER,
        /** 走向酒碗。 */
        APPROACH,
        DRINK,
        DRUNK
    }

    /**
     * @param since  进入当前阶段的刻
     * @param wokeAt 上次醒来的刻（从没醉过为 {@link Long#MIN_VALUE} / 4）
     */
    public record State(Phase phase, long since, long wokeAt) {
        public static final State FRESH = new State(Phase.SOBER, 0, Long.MIN_VALUE / 4);
    }

    private DrunkRules() {
    }

    /** 闻到酒：清醒、过了醒后的冷淡期、在范围内 → 走过去。 */
    public static State notice(State s, long now, double distSq) {
        if (s.phase() == Phase.SOBER && now - s.wokeAt() >= SOBER_TICKS && distSq <= NOTICE_RANGE * NOTICE_RANGE) {
            return new State(Phase.APPROACH, now, s.wokeAt());
        }
        return s;
    }

    /** 走到酒碗边：开始喝。 */
    public static State arrive(State s, long now) {
        return s.phase() == Phase.APPROACH ? new State(Phase.DRINK, now, s.wokeAt()) : s;
    }

    /** 酒碗没了（消失或被别人喝完）：走去的半路放弃。 */
    public static State bowlGone(State s, long now) {
        return s.phase() == Phase.APPROACH ? new State(Phase.SOBER, now, s.wokeAt()) : s;
    }

    /** 每刻推进：喝满 → 醉倒；醉够 → 醒。 */
    public static State tick(State s, long now) {
        return switch (s.phase()) {
            case DRINK -> now - s.since() >= DRINK_TICKS ? new State(Phase.DRUNK, now, s.wokeAt()) : s;
            case DRUNK -> now - s.since() >= DRUNK_TICKS ? new State(Phase.SOBER, now, now) : s;
            default -> s;
        };
    }

    /** 挨打：走去或喝酒途中中断（不醉倒、不算醒过，酒碗留着）；醉倒中照睡。 */
    public static State hurt(State s, long now) {
        return s.phase() == Phase.APPROACH || s.phase() == Phase.DRINK ? new State(Phase.SOBER, now, s.wokeAt()) : s;
    }

    /** 喝完了吗（这一刻刚醉倒，平台层据此移走酒碗）。 */
    public static boolean finishedDrinking(State before, State after) {
        return before.phase() == Phase.DRINK && after.phase() == Phase.DRUNK;
    }

    /** 当前受伤倍率（窗口区的值）：醉倒 ×3，其余 ×1。 */
    public static double damageWindow(State s) {
        return s.phase() == Phase.DRUNK ? DRUNK_MULTIPLIER : 1.0;
    }
}
