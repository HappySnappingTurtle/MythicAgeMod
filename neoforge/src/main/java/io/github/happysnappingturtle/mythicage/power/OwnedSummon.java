package io.github.happysnappingturtle.mythicage.power;

import java.util.UUID;

/**
 * 玩家的召唤单位（T16 的神话单位实现它）：目标矩阵用它区分「自己的常驻召唤单位」与「他人召唤单位」。
 */
public interface OwnedSummon {
    UUID summonOwner();

    /** 常驻（计神话单位上限）还是临时（计临时实体预算）。 */
    boolean permanentSummon();
}
