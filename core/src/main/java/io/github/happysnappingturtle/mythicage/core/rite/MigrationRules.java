package io.github.happysnappingturtle.mythicage.core.rite;

import io.github.happysnappingturtle.mythicage.core.player.Age;

/**
 * 村庄吸纳移民的判定（T11a 设计文档 §8.1；D9）：古典起、附近有玩家、成员未到目标、有空床、间隔已到。
 * 不离线模拟（总纲 A2）：没人在附近就不吸纳，也不补算。
 */
public final class MigrationRules {
    private MigrationRules() {
    }

    public enum Decision {
        /** 生成一名移民。 */
        MIGRATE,
        /** 远古或中立村庄：不吸纳。 */
        INACTIVE,
        /** 附近没有玩家。 */
        NO_PLAYER,
        /** 成员已达目标。 */
        FULL,
        /** 成员未到目标但没有空床：「需要更多住居」。 */
        NO_BED,
        /** 距上次吸纳不足间隔天数。 */
        WAITING
    }

    /**
     * @param lastMigrationDay 上次吸纳的日子；−1 = 从未
     * @param intervalDays     间隔（≥ 1）
     */
    public record Input(Age stage, int members, int target, int freeBeds, long today, long lastMigrationDay,
                        int intervalDays, boolean playerNear) {
    }

    public static Decision decide(Input in) {
        if (!in.stage().atLeast(Age.CLASSICAL)) {
            return Decision.INACTIVE;
        }
        if (!in.playerNear()) {
            return Decision.NO_PLAYER;
        }
        if (in.members() >= in.target()) {
            return Decision.FULL;
        }
        if (in.freeBeds() <= 0) {
            return Decision.NO_BED;
        }
        if (in.lastMigrationDay() >= 0 && in.today() - in.lastMigrationDay() < Math.max(1, in.intervalDays())) {
            return Decision.WAITING;
        }
        return Decision.MIGRATE;
    }
}
