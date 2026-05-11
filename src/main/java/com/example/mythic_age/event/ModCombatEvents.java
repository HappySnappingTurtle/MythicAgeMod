package com.example.mythic_age.event;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.system.GodPowerCertificateManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 服务器战斗事件。
 */
public final class ModCombatEvents {

    private ModCombatEvents() {
    }

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof PlayerEntity player) {
                GodPowerCertificateManager.getInstance().handleNordicKill(world, player, killedEntity);
            }
        });

        MythicAgeMod.LOGGER.info("Registered Mythic Age combat events");
    }
}
