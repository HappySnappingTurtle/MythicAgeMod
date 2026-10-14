package io.github.happysnappingturtle.mythicage.core.player;

import java.util.OptionalLong;

/** 恩宠规则（T4 设计文档 §2.1），纯函数。 */
public final class FavorRules {
    private FavorRules() {
    }

    /** 上限 = 时代基础上限 + 圣物加成（负加成按 0 处理）。 */
    public static long cap(Age age, long capBonus) {
        return saturatedAdd(age.baseCap(), Math.max(0, capBonus));
    }

    /**
     * 增加恩宠。当前值已达到或超过上限时保持不变（卸下圣物后超出的部分保留、但不再增长，总纲 §3.1）。
     *
     * @throws IllegalArgumentException amount ≤ 0
     */
    public static long add(long current, long amount, long cap) {
        if (amount <= 0) {
            throw new IllegalArgumentException("增加的恩宠必须为正数：" + amount);
        }
        if (current >= cap) {
            return current;
        }
        return Math.min(cap, saturatedAdd(current, amount));
    }

    /**
     * 扣除恩宠。
     *
     * @return 扣除后的值；余额不足时返回 empty
     * @throws IllegalArgumentException amount ≤ 0
     */
    public static OptionalLong spend(long current, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("扣除的恩宠必须为正数：" + amount);
        }
        return amount <= current ? OptionalLong.of(current - amount) : OptionalLong.empty();
    }

    private static long saturatedAdd(long a, long b) {
        long r = a + b;
        return ((a ^ r) & (b ^ r)) < 0 ? Long.MAX_VALUE : r;
    }
}
