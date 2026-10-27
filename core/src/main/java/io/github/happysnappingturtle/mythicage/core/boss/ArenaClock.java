package io.github.happysnappingturtle.mythicage.core.boss;

/**
 * 竞技场计时（T23 §4.2）：按游戏时间戳记「最后一次有合格玩家在场」。战斗或胜利后连续 {@value #EMPTY_RESET} 刻没人 → 复原；
 * 门开着（OPEN）连续 {@value #OPEN_TIMEOUT} 刻没人进大厅 → 关门。用时间戳而不是逐刻计数：玩家跑远、大厅区块卸载后竞技场不推进，
 * 回来区块重新载入时离开早已超时，第一刻就复原，不会让人回到一场「冻住」的战斗里。
 */
public final class ArenaClock {
    public static final int EMPTY_RESET = 600;
    public static final int OPEN_TIMEOUT = 1200;

    private long lastPresent;

    /** 这一刻（now）是否有人；返回 true 表示已经连续 limit 刻没人。 */
    public boolean tick(boolean anyonePresent, long now, int limit) {
        if (anyonePresent) {
            lastPresent = now;
            return false;
        }
        return now - lastPresent >= limit;
    }

    public long empty(long now) {
        return now - lastPresent;
    }

    public long lastPresent() {
        return lastPresent;
    }

    /** 从 now 起重新计时。 */
    public void reset(long now) {
        lastPresent = now;
    }

    public void restore(long lastPresent) {
        this.lastPresent = lastPresent;
    }
}
