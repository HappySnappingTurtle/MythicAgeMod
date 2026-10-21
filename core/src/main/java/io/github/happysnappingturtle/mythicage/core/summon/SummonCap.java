package io.github.happysnappingturtle.mythicage.core.summon;

import io.github.happysnappingturtle.mythicage.core.player.Age;

import java.util.Collection;

/**
 * 神话单位上限（总纲 §3.6、T14 设计文档 §2.2、D1）：基础 2，玩家在任意文明达到的最高时代从远古起每升一级 +1，
 * 硬上限 5，任何效果都不能突破。上限只算常驻单位（含等待重生的）。
 */
public final class SummonCap {
    public static final int BASE = 2;
    public static final int HARD_CAP = 5;

    private SummonCap() {
    }

    /** 远古 2、古典 3、英雄 4、神话 5；尚未进入任何时代也是 2。 */
    public static int cap(Age highest) {
        int bonus = Math.max(0, highest.ordinal() - Age.ARCHAIC.ordinal());
        return Math.min(HARD_CAP, BASE + bonus);
    }

    /** 各文明时代中的最高一个。 */
    public static Age highest(Collection<Age> ages) {
        Age best = Age.NONE;
        for (Age a : ages) {
            if (a.ordinal() > best.ordinal()) {
                best = a;
            }
        }
        return best;
    }

    /** 再召一个 cost 的单位是否不超上限。 */
    public static boolean fits(int occupied, int cost, int cap) {
        return cost > 0 && occupied + cost <= Math.min(cap, HARD_CAP);
    }
}
