package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;

/**
 * 北欧「战意」（03 号 §7；T5 设计文档 §5）：下线或挂机期间按 {@code 日上限 × offline.rate} 累积，
 * 上限 {@code maxBufferDays × 日上限}；战斗获得恩宠时按 multiplier 加成，加成从战意中扣除。
 */
public final class NorseRested {

    /**
     * @param granted       实际获得的恩宠（基础 + 加成）
     * @param countedTowardDailyCap 计入战斗日上限的部分
     * @param restedRemaining 剩余战意
     */
    public record Result(long granted, long countedTowardDailyCap, double restedRemaining) {
    }

    private NorseRested() {
    }

    /** 关闭时原样返回（储备冻结保留，03 号 §13.3）。 */
    public static double accrue(double rested, long ticks, double dailyCap, double offlineRate,
                                RuntimeConfig.Rested cfg) {
        if (!cfg.enabled() || offlineRate <= 0 || ticks <= 0 || dailyCap <= 0) {
            return rested;
        }
        double cap = cfg.maxBufferDays() * dailyCap;
        if (rested >= cap) {
            return rested;
        }
        return Math.min(cap, rested + Ticks.perDay(dailyCap * offlineRate, ticks));
    }

    /** 一次战斗获得 base 点恩宠时的兑现。 */
    public static Result consume(double rested, long base, RuntimeConfig.Rested cfg) {
        if (!cfg.enabled() || base <= 0 || cfg.multiplier() <= 1 || rested < 1) {
            return new Result(base, base, rested);
        }
        long bonus = (long) Math.min(Math.floor(base * (cfg.multiplier() - 1)), Math.floor(rested));
        long granted = base + bonus;
        return new Result(granted, cfg.bypassDailyCap() ? base : granted, rested - bonus);
    }
}
