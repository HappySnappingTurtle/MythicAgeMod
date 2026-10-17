package io.github.happysnappingturtle.mythicage.core.power;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;

/**
 * 神力冷却（T12 设计文档 §2.4；05 号 §1.1）：同文明共享短冷却 + 各神力自己的长冷却，同时计。
 * 时刻都用世界时钟（单调的游戏刻），存在玩家数据里。
 */
public final class CooldownRules {
    private CooldownRules() {
    }

    /** 还要等多少刻；0 = 可以施放。 */
    public static long remaining(PlayerMythData data, Civilization civ, String powerId, long now) {
        long until = Math.max(data.civ(civ).powerCooldownUntil(), data.powerCooldowns().getOrDefault(powerId, 0L));
        return Math.max(0, until - now);
    }

    /** 剩余秒数（向上取整）。 */
    public static long remainingSeconds(long ticks) {
        return (ticks + 19) / 20;
    }

    /** 提交后写入：该文明全部神力 shared 刻、本神力 own 刻。只会延长，不会缩短已有冷却。 */
    public static void apply(PlayerMythData data, Civilization civ, String powerId, long now, long shared, long own) {
        PlayerMythData.CivState s = data.civ(civ);
        s.setPowerCooldownUntil(Math.max(s.powerCooldownUntil(), now + shared));
        long prev = data.powerCooldowns().getOrDefault(powerId, 0L);
        data.powerCooldowns().put(powerId, Math.max(prev, now + own));
    }
}
