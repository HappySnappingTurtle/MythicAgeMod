package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;

/** 方案 F 的离线储备（T5 设计文档 §5）：按 {@code dailyRate × rate} 累积，上限 {@code maxBufferDays × dailyRate}。 */
public final class OfflineBuffer {
    private OfflineBuffer() {
    }

    public static double accrue(double buffer, long ticks, double dailyRate, RuntimeConfig.Offline cfg) {
        if (cfg.rate() <= 0 || ticks <= 0 || dailyRate <= 0) {
            return buffer;
        }
        double cap = cfg.maxBufferDays() * dailyRate;
        if (buffer >= cap) {
            return buffer;
        }
        return Math.min(cap, buffer + Ticks.perDay(dailyRate * cfg.rate(), ticks));
    }
}
