package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.power.ThrowRules;

/**
 * 投掷型神力的飞行（T13 设计文档 §3.1，D2）：提交时已消耗；服务端按 {@link ThrowRules} 算飞行刻数，到点执行落地效果。
 * 客户端收到同一次施放的 {@code COMMIT} 后按同一公式画投掷物从施法者手里飞到落点（不同步实体）。
 */
public final class PowerThrow {
    private PowerThrow() {
    }

    /** 飞行刻数（从施法者眼睛到落点）。 */
    public static int flightTicks(CastContext ctx) {
        return ThrowRules.flightTicks(ctx.caster().getEyePosition().distanceTo(ctx.origin()));
    }

    /** 飞行结束时在服务端调用 {@code onLand}。 */
    public static void launch(CastContext ctx, Runnable onLand) {
        int ticks = flightTicks(ctx);
        PowerTasks.schedule((server, age) -> {
            if (age < ticks) {
                return true;
            }
            onLand.run();
            return false;
        });
    }
}
