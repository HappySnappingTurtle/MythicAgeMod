package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 兵种独有机制的挂钩（T20 设计文档 §2.2）：各文明按 id 注册（兵种数据的 {@code ability}），框架在这几个时机调用。
 * 希腊示例：护祷拦截（{@code greek:prayer_guard}）、圣火烽号（{@code greek:brazier_signal}）。
 */
public interface SoldierAbility {
    Map<String, SoldierAbility> REGISTRY = new ConcurrentHashMap<>();

    static void register(String id, SoldierAbility a) {
        REGISTRY.put(id, a);
    }

    static Optional<SoldierAbility> of(String id) {
        return id == null || id.isEmpty() ? Optional.empty() : Optional.ofNullable(REGISTRY.get(id));
    }

    /** 本村进入一场新的警戒（每场只调用一次，{@code serial} 是警戒场次）。 */
    default void onAlert(ResidentEntity soldier, SoldierBrain brain, long serial) {
    }

    /**
     * 值勤（或接敌）中的每一刻、在框架自己的行为之前调用；返回 true 表示这一刻由它接管移动与动作（框架这一刻不做别的）。
     */
    default boolean tick(ResidentEntity soldier, SoldierBrain brain) {
        return false;
    }

    /** 本村居民即将受伤（护祷拦截的投射物掩护）：返回伤害倍率。 */
    default float protectedHurt(ResidentEntity soldier, SoldierBrain brain, ResidentEntity victim, DamageSource source) {
        return 1.0F;
    }
}
