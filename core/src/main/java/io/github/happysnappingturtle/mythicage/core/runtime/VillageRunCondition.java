package io.github.happysnappingturtle.mythicage.core.runtime;

/**
 * 村庄是否「真实运行」：所在区块处于实体刻，且最近的玩家在半径内（含边界）。
 * 1.21.1 强加载区块中 128 格内没有玩家时作物不生长（03 号调研 §1），所以只看加载不够。
 */
public final class VillageRunCondition {
    private VillageRunCondition() {
    }

    public static boolean realRunning(boolean entityTicking, double nearestPlayerDistance, int realRunRadius) {
        return entityTicking && nearestPlayerDistance <= realRunRadius;
    }
}
