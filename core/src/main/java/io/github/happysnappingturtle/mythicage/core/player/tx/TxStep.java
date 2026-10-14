package io.github.happysnappingturtle.mythicage.core.player.tx;

import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;

import java.util.Optional;

/**
 * 事务中的一个步骤（T4 设计文档 §3）。所有可能失败的条件都放在 {@link #check}；
 * {@link #apply} 只在全部步骤 check 通过后调用，并且不允许失败。
 * 涉及物品的步骤由加载器层实现同一接口：check 检查背包容量（预留），apply 放入物品。
 */
public interface TxStep {
    /** @return 失败原因；可以执行时返回 empty。不得修改任何状态。 */
    Optional<String> check(PlayerMythData data);

    void apply(PlayerMythData data);
}
