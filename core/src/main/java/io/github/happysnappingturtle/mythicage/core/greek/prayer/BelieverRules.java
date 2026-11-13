package io.github.happysnappingturtle.mythicage.core.greek.prayer;

/**
 * 信徒的纯判定（T10 设计文档 §4、§6、§7）：朝圣距离、补货上限。GA-13 起信徒只去本村神庙，
 * 原来的神坛选择顺序与「同一天只为一位主人祈祷」随玩家祭坛一起去掉。
 */
public final class BelieverRules {
    /** 朝圣距离：家到祈祷位的水平距离不小于它才计入。 */
    public static final double PILGRIMAGE_MIN = 8;
    /** 原版每日补货上限与两次补货的最小间隔。 */
    public static final int VANILLA_RESTOCKS = 2;
    public static final long RESTOCK_GAP_TICKS = 2400;

    private BelieverRules() {
    }

    /** 朝圣距离：家到祈祷位的水平距离 ≥ 8 格。 */
    public static boolean pilgrimageOk(double homeX, double homeZ, double slotX, double slotZ) {
        double dx = homeX - slotX;
        double dz = homeZ - slotZ;
        return dx * dx + dz * dz >= PILGRIMAGE_MIN * PILGRIMAGE_MIN;
    }

    /** 今天的补货次数上限：当天祈祷过的有职业村民用配置值（默认 1），否则原版的 2。 */
    public static int restockLimit(boolean prayedToday, int configuredLimit) {
        return prayedToday ? Math.max(0, Math.min(VANILLA_RESTOCKS, configuredLimit)) : VANILLA_RESTOCKS;
    }

    /**
     * 是否允许再补货一次。上限为 2 时与原版 {@code Villager#allowedToRestock} 完全一致：
     * 当天第一次随时可补，之后要距上次补货超过 2400 刻。
     */
    public static boolean restockAllowed(int restocksToday, int limit, long gameTime, long lastRestockGameTime) {
        return restocksToday < limit
                && (restocksToday == 0 || gameTime > lastRestockGameTime + RESTOCK_GAP_TICKS);
    }
}
