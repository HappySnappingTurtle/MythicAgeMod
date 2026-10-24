package io.github.happysnappingturtle.mythicage.core.village.work;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;

/**
 * 工作单的一步：走到目标、播一段动作、结算。动作只是表现，入库只在 SETTLE 一步发生（V1 §4.3 原子结算）；
 * 少数类型要在某段动作结束时改世界（收麦：割完麦子作物回到幼苗，打完谷才入库），由类型的 onActDone 处理。
 *
 * @param anim   动画名（GeckoLib）；MOVE、SETTLE 为空
 * @param ticks  动作时长（刻）；MOVE、SETTLE 为 0
 * @param target MOVE 的目标；为 null 时走到工作单的目标
 * @param carry  这一步手上显示搬着的东西（只是外观）；空串表示空手
 */
public record JobStep(Kind kind, String anim, int ticks, Pos target, String carry) {
    public enum Kind { MOVE, ACT, SETTLE }

    public JobStep(Kind kind, String anim, int ticks) {
        this(kind, anim, ticks, null, "");
    }

    public static JobStep move() {
        return new JobStep(Kind.MOVE, "", 0);
    }

    public static JobStep moveTo(Pos target, String carry) {
        return new JobStep(Kind.MOVE, "", 0, target, carry == null ? "" : carry);
    }

    public static JobStep act(String anim, int ticks) {
        return new JobStep(Kind.ACT, anim, Math.max(1, ticks));
    }

    public static JobStep settle() {
        return new JobStep(Kind.SETTLE, "", 0);
    }
}
