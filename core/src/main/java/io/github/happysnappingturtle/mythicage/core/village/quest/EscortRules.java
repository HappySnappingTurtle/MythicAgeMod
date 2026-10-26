package io.github.happysnappingturtle.mythicage.core.village.quest;

/**
 * 护送在途移民（T21 设计文档 §7）：在场比例与半径。纯函数。
 *
 * <p>在场按刻数累计（服务器每 20 刻采样一次，每次把这段时间整段记为在场或不在场），所以「采样 10 次在场 6 次」与
 * 「200 刻里在场 120 刻」是同一回事。
 */
public final class EscortRules {
    private EscortRules() {
    }

    /** 距离（平方）在半径内：恰好等于半径也算在场（TC-T21-32）。 */
    public static boolean within(double distSq, int radius) {
        return distSq <= (double) radius * radius;
    }

    /** 接取以来在场的刻数够不够比例 {@code presence}（02 号：一路陪着）；一刻都没记到的不算。 */
    public static boolean enough(long nearTicks, long totalTicks, double presence) {
        return totalTicks > 0 && nearTicks >= presence * totalTicks - 1.0E-9;
    }
}
