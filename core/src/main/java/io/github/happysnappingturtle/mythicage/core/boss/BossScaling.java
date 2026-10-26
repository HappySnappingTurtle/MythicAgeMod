package io.github.happysnappingturtle.mythicage.core.boss;

/**
 * 多人 Boss 血量（总纲 E6，T23 §3）：上限 = 基础 × (1 + 0.5 × (人数 − 1))；人数不足 1 按 1 算。
 * 中途人数变多时按比例提高（当前 / 上限不变），人数变少不降。
 */
public final class BossScaling {
    private BossScaling() {
    }

    public static float maxHealth(float base, int players) {
        int n = Math.max(1, players);
        return base * (1 + 0.5F * (n - 1));
    }

    /** 新上限（只增不减）。 */
    public static float raise(float oldMax, float candidate) {
        return Math.max(oldMax, candidate);
    }

    /** 上限从 oldMax 变成 newMax 时的当前血量（保持比例）。 */
    public static float rescale(float current, float oldMax, float newMax) {
        if (oldMax <= 0) {
            return newMax;
        }
        return current * newMax / oldMax;
    }
}
