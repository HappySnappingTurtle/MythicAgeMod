package io.github.happysnappingturtle.mythicage.core.power;

/**
 * 雷击的结算（T13 设计文档 §2.1；05 号 GP-GR-01）：最大生命 ≤60 的普通生物即死；精英、Boss 固定 40 点；
 * 敌对玩家（PvP）12 点。即死阈值读最大生命而不是当前生命。
 */
public final class BoltRules {
    public static final float KILL_MAX_HEALTH = 60;
    public static final float DAMAGE = 40;
    public static final float PVP_DAMAGE = 12;

    /** 结算结果：{@code kill} 为真时直接杀死，否则造成 {@code damage} 点。 */
    public record Outcome(boolean kill, float damage) {
    }

    private BoltRules() {
    }

    public static Outcome resolve(float maxHealth, boolean boss, boolean elite, boolean player) {
        if (player) {
            return new Outcome(false, PVP_DAMAGE);
        }
        if (boss || elite) {
            return new Outcome(false, DAMAGE);
        }
        return maxHealth <= KILL_MAX_HEALTH ? new Outcome(true, 0) : new Outcome(false, DAMAGE);
    }
}
