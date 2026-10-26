package io.github.happysnappingturtle.mythicage.core.village.quest;

import java.util.Objects;
import java.util.UUID;

/**
 * 修复设施（T21 设计文档 §6）：损坏记录与修复核验的纯规则。
 */
public final class RepairRules {
    /** 每座建筑最多记几格损坏（超出只计数，§6.1）。 */
    public static final int MAX_CELLS = 256;

    private RepairRules() {
    }

    /** 放回的方块对不对：方块 id 相同即可（朝向、半砖上下等属性不要求一致，§6.3）。 */
    public static boolean matches(String original, String placed) {
        return original.equals(placed);
    }

    /** 这格这一天是不是已经记过一次损坏：同一格同一 MC 日反复「打掉—放回」，第二次起不再记（§6.3 防刷）。 */
    public static boolean recordAgain(Long lastDamagedDay, long today) {
        return lastDamagedDay == null || lastDamagedDay != today;
    }

    /** 发修复任务的门槛：未修的格数到 {@code minCells}，或损坏里有门、床、工作站（§4.2）。 */
    public static boolean shouldPublish(int unrepaired, boolean keyBlock, int minCells) {
        return unrepaired > 0 && (keyBlock || unrepaired >= minCells);
    }

    /**
     * 这一格修好记不记到这名玩家名下的报酬：不是他自己打掉的（02 号「本人制造损坏不生成同等奖励」），
     * 且他今天在本村的修复报酬还没到上限 {@code dailyCap}（§6.3）。格子照样算修好。
     */
    public static boolean credit(UUID player, UUID cause, int todayCount, int dailyCap) {
        return !Objects.equals(player, cause) && todayCount < dailyCap;
    }

    /** 损坏多久没人修，村民自己修（D3：修复任务的有效期）。 */
    public static boolean selfRepairDue(long damagedDay, long today, int repairDays) {
        return today - damagedDay >= repairDays;
    }
}
