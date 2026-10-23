package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.shrine.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 各文明的圣物锚点（19 号 U1、D1）：哪种神庙可以供奉、供奉范围怎么算、谁算这个文明的信徒。
 * 通用层只规定「玩家 × 文明」供奉位数量；放在哪里、范围多大由这里决定。
 */
public interface RelicAnchor {
    Civilization civ();

    /** 这种神庙能不能供奉。 */
    boolean accepts(ShrineKind kind);

    /** 目标是否在供奉范围内（神庙所在区块已加载时才会被调用）。 */
    boolean inRange(ServerLevel level, BlockPos anchor, Entity target);

    /** 范围搜索用的外包半径（格）：{@link #inRange} 为真的目标都在神庙这么远以内。 */
    double searchRadius();

    /** 是否是本文明的信徒村民（皮格马利翁雕像等「信徒」类效果用）。 */
    boolean believer(Villager villager);

    final class Registry {
        private static final Map<Civilization, RelicAnchor> ANCHORS = new EnumMap<>(Civilization.class);

        private Registry() {
        }

        public static synchronized void register(RelicAnchor anchor) {
            if (ANCHORS.putIfAbsent(anchor.civ(), anchor) != null) {
                throw new IllegalStateException("重复的圣物锚点：" + anchor.civ().id());
            }
        }

        public static Optional<RelicAnchor> of(Civilization civ) {
            return Optional.ofNullable(ANCHORS.get(civ));
        }

        /** 某种神庙对应的锚点（不接受供奉时为空）。 */
        public static Optional<RelicAnchor> forKind(ShrineKind kind) {
            return of(kind.civ()).filter(a -> a.accepts(kind));
        }
    }
}
