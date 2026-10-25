package io.github.happysnappingturtle.mythicage.core.village.quest;

/**
 * 缺料供给（T21 设计文档 §5）：交多少、报酬怎么算。纯函数。
 */
public final class SupplyRules {
    private SupplyRules() {
    }

    /** 这次收多少：min(背包里有的, 还差的, 村库剩余容量)，不多收（§5.1）。 */
    public static int deliverable(int remaining, int have, int free) {
        return Math.max(0, Math.min(remaining, Math.min(have, free)));
    }

    /**
     * 口粮任务：还差 {@code remainingPortions} 份口粮、这种食物每件 {@code portion} 份时，最多收几件
     * （向上取整：最后一件可以补过头一点，不让玩家交不完）。
     */
    public static int foodUnits(double remainingPortions, double portion) {
        if (remainingPortions <= 0 || portion <= 0) {
            return 0;
        }
        return (int) Math.ceil(remainingPortions / portion - 1.0E-9);
    }

    /** 结算的绿宝石：累计的小数向上取整；没交东西（0）不给。 */
    public static int payout(double owed) {
        if (owed <= 1.0E-9) {
            return 0;
        }
        return (int) Math.ceil(owed - 1.0E-9);
    }
}
