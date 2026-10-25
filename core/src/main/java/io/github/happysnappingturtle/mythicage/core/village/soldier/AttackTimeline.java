package io.github.happysnappingturtle.mythicage.core.village.soldier;

/**
 * 一次出手的阶段（T20 设计文档 §8.1，02 号卡 §3）：前摇 → 有效 → 恢复 → 冷却 → 可再次出手。
 * 时间 {@code t} 是从出手开始的刻数（第 0 刻是前摇第一刻）。
 */
public final class AttackTimeline {
    private AttackTimeline() {
    }

    public enum Phase { WINDUP, ACTIVE, RECOVER, COOLDOWN, READY }

    public static Phase phase(SoldierProfile.Timeline tl, int t) {
        if (t < 0) {
            return Phase.READY;
        }
        if (t < tl.windup()) {
            return Phase.WINDUP;
        }
        if (t < tl.windup() + tl.active()) {
            return Phase.ACTIVE;
        }
        if (t < tl.windup() + tl.active() + tl.recover()) {
            return Phase.RECOVER;
        }
        return t < tl.interval() ? Phase.COOLDOWN : Phase.READY;
    }

    /** 朝向锁定：前摇的最后 {@code lock} 刻与有效窗口里不再转向（不在命中时突然转身）。 */
    public static boolean locked(SoldierProfile.Timeline tl, int t) {
        return t >= tl.windup() - tl.lock() && t < tl.windup() + tl.active();
    }

    /** 动作在第几刻结束（恢复结束）。 */
    public static int animationEnd(SoldierProfile.Timeline tl) {
        return tl.windup() + tl.active() + tl.recover();
    }
}
