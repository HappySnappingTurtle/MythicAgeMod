package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.power.OwnedSummon;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * 常驻神话单位（T14）：实体实现它，框架（登记、指令、跟随 / 驻守、跨区块）只通过它说话。
 * 每个文明的单位各自实现普攻与特殊攻击。
 */
public interface MythSummon extends OwnedSummon {
    SummonLink link();

    @Override
    default UUID summonOwner() {
        return link().owner;
    }

    @Override
    default boolean permanentSummon() {
        return true;
    }

    /** 战斗单位（天马等侦察单位为 false：不接受攻击指令，「自由攻击」视同跟随）。 */
    boolean combatUnit();

    /** 立即对目标放特殊攻击（传令杖潜行右键）；冷却中、正在放或目标不合适时返回 false。 */
    boolean requestSpecial(LivingEntity target);

    /** 召唤时按单位数值设置属性（生命、普攻、移速……），并把生命回满。 */
    void applyStats(SummonUnits.Stats stats);
}
