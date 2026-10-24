package io.github.happysnappingturtle.mythicage.core.village.work;

/**
 * 工作单的一步：走到目标、播一段动作、结算。动作只是表现，结算只在 SETTLE 一步发生（V1 §4.3 原子结算）。
 *
 * @param anim  动画名（GeckoLib）；MOVE、SETTLE 为空
 * @param ticks 动作时长（刻）；MOVE、SETTLE 为 0
 */
public record JobStep(Kind kind, String anim, int ticks) {
    public enum Kind { MOVE, ACT, SETTLE }

    public static JobStep move() {
        return new JobStep(Kind.MOVE, "", 0);
    }

    public static JobStep act(String anim, int ticks) {
        return new JobStep(Kind.ACT, anim, Math.max(1, ticks));
    }

    public static JobStep settle() {
        return new JobStep(Kind.SETTLE, "", 0);
    }
}
