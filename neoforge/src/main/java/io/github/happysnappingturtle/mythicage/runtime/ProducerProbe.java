package io.github.happysnappingturtle.mythicage.runtime;

import io.github.happysnappingturtle.mythicage.core.runtime.ProducerLedger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 产出点探针：回答本区间产出点是否真实运行、主人是否在照看半径内、真实运行时的产出量。
 * 每类产出点（神坛 T8/T10、纪念碑、水晶塔……）注册一个，按 {@link ProducerProbes#type} 分派。
 */
@FunctionalInterface
public interface ProducerProbe {
    record Observation(boolean realRunning, boolean ownerNearby, double realYield) {
        public static final Observation NOT_RUNNING = new Observation(false, false, 0);
    }

    /**
     * @param owner    在线的主人，离线时为 null
     * @param interval 本次结算区间（tick）
     */
    Observation observe(MinecraftServer server, ProducerLedger ledger, ServerPlayer owner, long now, long interval);
}
