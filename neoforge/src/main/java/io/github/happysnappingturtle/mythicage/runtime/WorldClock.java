package io.github.happysnappingturtle.mythicage.runtime;

import net.minecraft.server.MinecraftServer;

/**
 * 世界时钟（T5 设计文档 §2）：主世界的 gameTime。服务器真实推进、单调递增；
 * {@code /time set} 与睡觉跳夜只改 dayTime，不影响它。
 */
public final class WorldClock {
    private WorldClock() {
    }

    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }
}
