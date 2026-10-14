package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.server.level.ServerPlayer;

/** 恩宠上限的圣物加成（总纲 §3.1、§3.8）。T16 圣物系统实现；在此之前恒为 0。 */
@FunctionalInterface
public interface CapBonusProvider {
    long capBonus(ServerPlayer player, Civilization civ);

    /** 当前生效的提供者；T16 启动时替换。 */
    final class Holder {
        private static volatile CapBonusProvider current = (player, civ) -> 0;

        private Holder() {
        }

        public static CapBonusProvider get() {
            return current;
        }

        public static void set(CapBonusProvider provider) {
            current = provider;
        }
    }
}
