package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;

/** 村庄公共资源在不能真实运行时可补算的 tick 数（T5 设计文档 §4；T19 接入）。 */
public final class PublicCatchUp {
    private PublicCatchUp() {
    }

    public static long ticksToSimulate(long lastRealTick, long now, boolean ownerOnline,
                                       RuntimeConfig.PublicResources cfg) {
        if (cfg.mode() == RuntimeConfig.PublicMode.LOADED_ONLY) {
            return 0;
        }
        if (cfg.requireOwnerOnline() && !ownerOnline) {
            return 0;
        }
        return Math.max(0, Math.min(now - lastRealTick, cfg.maxCatchUpTicks()));
    }
}
