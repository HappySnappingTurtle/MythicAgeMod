package io.github.happysnappingturtle.mythicage.faith;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 信仰面板的「本文明特色」区块（T11a 设计文档 §7.3；18 号 §2.2）：可选，各文明自己提供
 * （如埃及法老权杖、亚特兰蒂斯英雄化单位、中华三皇赐福）。T11a 只建接口，没有实现者。
 */
public interface CivFeatureView {
    Civilization civ();

    /** 该玩家的若干行。 */
    List<Component> rows(ServerPlayer viewer);

    /** 特色区块注册表。 */
    final class Registry {
        private static final List<CivFeatureView> ALL = new ArrayList<>();

        private Registry() {
        }

        public static synchronized void register(CivFeatureView view) {
            ALL.add(view);
        }

        public static synchronized List<CivFeatureView> of(Civilization civ) {
            return ALL.stream().filter(v -> v.civ() == civ).toList();
        }
    }
}
