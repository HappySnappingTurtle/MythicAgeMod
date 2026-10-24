package io.github.happysnappingturtle.mythicage.conduct;

import io.github.happysnappingturtle.mythicage.village.VillageZones;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;

/** 善恶值与村庄范围的事件挂接（T18）。 */
public final class ConductEventsRegistration {
    private ConductEventsRegistration() {
    }

    public static void register(IEventBus game) {
        // 最后看结果：别的处理器改过的伤害才是实际伤害
        game.addListener(EventPriority.LOWEST, ConductEvents::onDamagePost);
        game.addListener(EventPriority.LOWEST, ConductEvents::onDeath);
        game.addListener(ConductEvents::onConverted);
        game.addListener(ConductEvents::onServerStopped);
        game.addListener(ConductService::onServerTickPost);
        game.addListener(ConductCommand::onRegisterCommands);
        game.addListener(VillageZones::onServerStopped);
    }
}
