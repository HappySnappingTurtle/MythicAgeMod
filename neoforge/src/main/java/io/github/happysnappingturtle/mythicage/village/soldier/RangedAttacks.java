package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程兵种的出手（T20 设计文档 §8.2）：按弹药物品找到投射物的执行器（各文明注册；希腊投矛在阶段 ②）。
 * 没有执行器的远程兵种退为护送。
 */
public final class RangedAttacks {
    /** 一种弹药的执行：返回 true 表示这一刻已经处理（走位、出手或后撤）。 */
    public interface Shooter {
        boolean tick(ResidentEntity soldier, SoldierBrain brain, SoldierOrders orders, LivingEntity target, long now);
    }

    private static final Map<String, Shooter> BY_AMMO = new ConcurrentHashMap<>();

    private RangedAttacks() {
    }

    public static void register(String ammoItem, Shooter s) {
        BY_AMMO.put(ammoItem, s);
    }

    static boolean tick(ResidentEntity soldier, SoldierBrain brain, SoldierOrders o, LivingEntity target, long now) {
        if (o.profile().ammo() == null) {
            return false;
        }
        for (var item : o.profile().ammo().items()) {
            Shooter s = BY_AMMO.get(item.item());
            if (s != null) {
                return s.tick(soldier, brain, o, target, now);
            }
        }
        return false;
    }
}
