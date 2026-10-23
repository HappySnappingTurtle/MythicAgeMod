package io.github.happysnappingturtle.mythicage.relic.greek;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverEvents;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.relic.RelicAnchor;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

/**
 * 希腊的圣物锚点：奥林匹斯祭坛；供奉范围 = 同维度、到神坛中心的三维距离 ≤ 48 格（01_希腊 §9，D6）；
 * 信徒 = 已登记希腊村庄里的成年村民（D5）。
 */
public final class GreekRelicAnchor implements RelicAnchor {
    public static final double RADIUS = 48;

    @Override
    public Civilization civ() {
        return Civilization.GREEK;
    }

    @Override
    public boolean accepts(ShrineKind kind) {
        return kind.id().equals(GreekTemple.ALTAR_KIND.id());
    }

    @Override
    public boolean inRange(ServerLevel level, BlockPos anchor, Entity target) {
        return target.level() == level && target.position().distanceToSqr(Vec3.atCenterOf(anchor)) <= RADIUS * RADIUS;
    }

    @Override
    public double searchRadius() {
        return RADIUS;
    }

    @Override
    public boolean believer(Villager villager) {
        return BelieverEvents.isBeliever(villager);
    }
}
