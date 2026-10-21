package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonUnitCard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 召唤管理的入口（T14 设计文档 §7）。阶段 ① 先在聊天里列出单位；阶段 ② 换成管理界面。
 */
public final class SummonScreens {
    private SummonScreens() {
    }

    public static void open(ServerPlayer p) {
        var units = SummonRegistry.get(p.server).byOwner(p.getUUID());
        p.sendSystemMessage(Component.translatable("mythic_age.summon.summary", SummonService.occupied(p), SummonService.cap(p)));
        for (SummonRegistry.Unit u : units) {
            Component name = SummonUnits.card(u.card).map(SummonUnitCard::entity)
                    .map(id -> Component.translatable("entity." + id.replace(':', '.')))
                    .orElse(Component.literal(u.card));
            Entity e = SummonService.find(p.server, u);
            Component where = u.respawning
                    ? Component.translatable("mythic_age.summon.where_respawning",
                    Math.max(0, (u.respawnAt - p.server.overworld().getGameTime()) / 20))
                    : e == null ? Component.translatable("mythic_age.summon.where_unloaded")
                    : e.level() != p.level() ? Component.translatable("mythic_age.summon.where_dimension",
                    e.level().dimension().location().toString())
                    : Component.translatable("mythic_age.summon.where_distance", (int) e.distanceTo(p));
            p.sendSystemMessage(Component.translatable("mythic_age.summon.line", name,
                    Component.translatable("mythic_age.summon.mode." + u.mode.id()), where));
        }
    }
}
