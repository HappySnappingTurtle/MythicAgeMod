package io.github.happysnappingturtle.mythicage.recipe;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 合成时的玩家上下文（T6 设计文档 §7.2）。原版的配方匹配接口拿不到玩家，由 Mixin 在
 * 「合成格计算结果」与「取出合成结果」这两步前后设置和清除。
 * 只在服务端主线程使用；为防异常导致残留，每个服务端 tick 开始时也会清除一次。
 */
public final class CraftingContext {
    private static final ThreadLocal<ServerPlayer> CURRENT = new ThreadLocal<>();

    private CraftingContext() {
    }

    public static void enter(Player player) {
        CURRENT.set(player instanceof ServerPlayer sp ? sp : null);
    }

    public static void exit() {
        CURRENT.remove();
    }

    /** 当前正在合成的玩家；没有玩家（自动合成、客户端）时为 null。 */
    public static ServerPlayer current() {
        return CURRENT.get();
    }
}
