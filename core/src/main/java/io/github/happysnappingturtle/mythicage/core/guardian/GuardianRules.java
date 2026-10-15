package io.github.happysnappingturtle.mythicage.core.guardian;

/**
 * 守护者的纯规则（T9 设计文档 §6、§7）。MC 日序号 = 世界时钟 / 24000。
 */
public final class GuardianRules {
    public static final long TICKS_PER_DAY = 24000;

    private GuardianRules() {
    }

    public static long day(long worldClockTicks) {
        return Math.floorDiv(worldClockTicks, TICKS_PER_DAY);
    }

    /** 补领祭坛（决策 D3）：名下没有已放置的祭坛，背包里也没有。每日次数由收据控制。 */
    public static boolean canReissue(long ownedAltars, boolean altarInInventory) {
        return ownedAltars == 0 && !altarInInventory;
    }

    /** 复原（决策 D4）：死亡后满 respawnDays 个 MC 日。 */
    public static boolean shouldRespawn(long deathDay, long today, int respawnDays) {
        return today >= deathDay + respawnDays;
    }

    /** 补领收据：每 MC 日一张。 */
    public static String reissueReceipt(String civId, long day) {
        return civId + ":altar_reissue:" + day;
    }

    /** 初见收据：每个玩家每个文明一张（UNIQUE）。 */
    public static String blessingReceipt(String civId) {
        return civId + ":first_blessing";
    }
}
