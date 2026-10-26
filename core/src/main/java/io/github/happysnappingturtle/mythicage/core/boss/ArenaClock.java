package io.github.happysnappingturtle.mythicage.core.boss;

/**
 * 竞技场计时（T23 §4.2）：战斗或胜利后大厅里连续 {@value #EMPTY_RESET} 刻没有合格玩家 → 复原；
 * 门开着（OPEN）连续 {@value #OPEN_TIMEOUT} 刻没人进大厅 → 关门。有人回来就清零。
 */
public final class ArenaClock {
    public static final int EMPTY_RESET = 600;
    public static final int OPEN_TIMEOUT = 1200;

    private int empty;

    /** 推进一刻；返回 true 表示到时（limit 刻）。 */
    public boolean tick(boolean anyonePresent, int limit) {
        if (anyonePresent) {
            empty = 0;
            return false;
        }
        empty++;
        return empty >= limit;
    }

    public int empty() {
        return empty;
    }

    public void reset() {
        empty = 0;
    }

    public void restore(int empty) {
        this.empty = Math.max(0, empty);
    }
}
