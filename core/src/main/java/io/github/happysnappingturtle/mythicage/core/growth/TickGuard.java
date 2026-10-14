package io.github.happysnappingturtle.mythicage.core.growth;

/**
 * 刻守卫（T6b 设计文档 §10.2-4）：方块实体在同一个游戏刻里被调用多次（例如其他模组的刻加速器）时只计一次，
 * 使产出与进度只跟随游戏时间，不跟随调用次数。
 */
public final class TickGuard {
    private long lastTick = Long.MIN_VALUE;

    /** 这一刻是否第一次调用；是则记下并返回 true。 */
    public boolean firstCallAt(long gameTime) {
        if (gameTime == lastTick) {
            return false;
        }
        lastTick = gameTime;
        return true;
    }
}
