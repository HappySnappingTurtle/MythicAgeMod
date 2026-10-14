package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;

/**
 * 施工储备（T5 设计文档 §4）：离开期间不落块，只积累储备 tick；
 * 回来真实运行时每 tick 额外推进 {@code multiplier − 1} 个施工 tick，直到储备用完。
 */
public final class ConstructionBank {

    /** @param extraTicks 本次额外推进的施工 tick；remaining 剩余储备 */
    public record SpendResult(long extraTicks, long remaining) {
    }

    private ConstructionBank() {
    }

    public static long accrue(long bank, long awayTicks, RuntimeConfig.Construction cfg) {
        if (awayTicks <= 0 || bank >= cfg.bankMaxTicks()) {
            return bank;
        }
        return Math.min(cfg.bankMaxTicks(), bank + Math.min(awayTicks, cfg.bankMaxTicks()));
    }

    public static SpendResult spend(long bank, long realTicks, RuntimeConfig.Construction cfg) {
        if (bank <= 0 || realTicks <= 0 || cfg.bankSpendMultiplier() <= 1) {
            return new SpendResult(0, Math.max(0, bank));
        }
        long extra = (long) Math.min(bank, Math.floor(realTicks * (cfg.bankSpendMultiplier() - 1)));
        return new SpendResult(extra, bank - extra);
    }
}
