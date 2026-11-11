package io.github.happysnappingturtle.mythicage.equipment;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;

/** 装备机制的事件挂接（T17）。 */
public final class EquipmentEvents {
    private EquipmentEvents() {
    }

    public static void register(IEventBus game) {
        game.addListener(SetBonuses::onServerTickPost);
        game.addListener(EventPriority.LOWEST, WeaponMotionState::onAttack);
        game.addListener(EventPriority.LOWEST, WeaponMotionState::onCritical);
        // 最后看结果：别的处理器取消了格挡就不开窗口
        game.addListener(EventPriority.LOWEST, CounterEvents::onShieldBlock);
        game.addListener(CounterEvents::onIncomingDamage);
        game.addListener(CounterEvents::onServerStopped);
    }
}
