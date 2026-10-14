package io.github.happysnappingturtle.mythicage.core.runtime;

import io.github.happysnappingturtle.mythicage.core.config.runtime.RuntimeConfig;

/**
 * 私人恩宠结算（T5 设计文档 §3.1）。把 {@code (lastSettledTick, now]} 这一段归类为
 * 真实运行 / 快照运行 / 离线储备 / 无产出，算出应入账的整数恩宠，小数部分留到下次。
 * 纯函数：时钟、主人活跃、真实运行与否都由调用方传入。
 */
public final class SettlementEngine {
    /** 判断产出是否攒满整数时的容差，抵消浮点累加误差。 */
    private static final double EPSILON = 1e-9;

    public enum Mode { REAL, SNAPSHOT, OFFLINE, NONE }

    /**
     * @param realRunning 产出点所在区块本区间在真实运行
     * @param ownerActive 主人活跃（在线且未挂机）
     * @param ownerNearby 主人在产出点照看半径内（用于照看期）
     * @param realYield   真实运行期间产出点上报的产出
     */
    public record Conditions(boolean realRunning, boolean ownerActive, boolean ownerNearby, double realYield) {
        public static Conditions real(double yield, boolean ownerNearby) {
            return new Conditions(true, true, ownerNearby, yield);
        }

        public static Conditions away(boolean ownerActive) {
            return new Conditions(false, ownerActive, false, 0);
        }
    }

    /**
     * @param credited 应入账给主人的整数恩宠（含方案 C 补算与方案 F 储备兑现）
     * @param warning  非空表示异常（例如时钟回退），本次未结算
     */
    public record Settlement(Mode mode, long interval, long credited, String warning) {
        static Settlement skipped(String warning) {
            return new Settlement(Mode.NONE, 0, 0, warning);
        }
    }

    private SettlementEngine() {
    }

    /** 快照是否可用：未失效，且在照看期内（0 表示不限）。 */
    public static boolean snapshotUsable(ProducerLedger l, long now, RuntimeConfig.Snapshot cfg) {
        return l.snapshotValid()
                && (cfg.maxTicksSinceVisit() == 0 || now - l.lastVisitTick() <= cfg.maxTicksSinceVisit());
    }

    /** 快照日产：快照产率 × rateFactor × 未验证折扣。 */
    public static double snapshotDailyRate(ProducerLedger l, RuntimeConfig.Snapshot cfg) {
        double discount = l.verified() ? 1.0 : cfg.unverifiedFactor();
        return l.snapshotRatePerDay() * cfg.rateFactor() * discount;
    }

    public static Settlement settle(ProducerLedger l, long now, Conditions c, RuntimeConfig.Favor cfg) {
        long prev = l.lastSettledTick();
        if (now < prev) {
            return Settlement.skipped("时钟回退：now=" + now + " < lastSettledTick=" + prev + "（" + l.id() + "）");
        }
        long interval = now - prev;
        if (interval == 0) {
            return Settlement.skipped("");
        }
        RuntimeConfig.Snapshot snap = cfg.snapshot();
        trackVisit(l, prev, now, c, snap);

        boolean real = c.realRunning() && c.ownerActive();
        boolean usable = snapshotUsable(l, now, snap);
        double produced = 0;
        Mode mode = Mode.NONE;
        switch (cfg.mode()) {
            case ONLINE_SNAPSHOT -> {
                if (real) {
                    mode = Mode.REAL;
                    produced = Math.max(0, c.realYield());
                } else if (c.ownerActive() && usable) {
                    mode = Mode.SNAPSHOT;
                    produced = Ticks.perDay(snapshotDailyRate(l, snap), interval);
                } else if (!c.ownerActive() && cfg.offline().rate() > 0 && l.snapshotValid()) {
                    mode = Mode.OFFLINE;
                    l.setOfflineBuffer(OfflineBuffer.accrue(l.offlineBuffer(), interval,
                            snapshotDailyRate(l, snap), cfg.offline()));
                }
                // 方案 F：恢复活跃且快照可用时一次性兑现储备；rate 为 0 时储备冻结保留（03 号 §13.3）
                if (c.ownerActive() && usable && cfg.offline().rate() > 0 && l.offlineBuffer() > 0) {
                    produced += l.offlineBuffer();
                    l.setOfflineBuffer(0);
                }
            }
            case LOADED_ONLY -> {
                if (real) {
                    mode = Mode.REAL;
                    produced = Math.max(0, c.realYield());
                }
            }
            case CAPPED_CATCHUP -> {
                long maxSim = cfg.catchUp().maxSimulationTicks();
                if (real) {
                    mode = Mode.REAL;
                    produced = Math.max(0, c.realYield());
                    if (l.awayActiveTicks() > 0 && l.snapshotValid()) {
                        produced += Ticks.perDay(snapshotDailyRate(l, snap), Math.min(l.awayActiveTicks(), maxSim));
                    }
                    l.setAwayActiveTicks(0);
                } else if (c.ownerActive()) {
                    l.setAwayActiveTicks(Math.min(maxSim, l.awayActiveTicks() + interval));
                }
            }
        }
        trackVerification(l, prev, now, mode, c.realYield(), snap);

        double total = l.carry() + produced;
        long credited = (long) Math.floor(total + EPSILON);
        l.setCarry(total - credited);
        l.setLastSettledTick(now);
        return new Settlement(mode, interval, credited, "");
    }

    /** 照看：真实运行且主人在半径内连续停留 ≥ visitMinTicks。 */
    private static void trackVisit(ProducerLedger l, long prev, long now, Conditions c, RuntimeConfig.Snapshot snap) {
        if (!(c.realRunning() && c.ownerNearby())) {
            l.setNearbySinceTick(ProducerLedger.NONE);
            return;
        }
        if (l.nearbySinceTick() == ProducerLedger.NONE) {
            l.setNearbySinceTick(prev);
        }
        if (now - l.nearbySinceTick() >= snap.visitMinTicks()) {
            l.setLastVisitTick(now);
        }
    }

    /** 完整观测日：真实运行连续 ≥ verifyTicks 时，用实测日产率校正快照。 */
    private static void trackVerification(ProducerLedger l, long prev, long now, Mode mode, double yield,
                                          RuntimeConfig.Snapshot snap) {
        if (mode != Mode.REAL) {
            l.setRealStreak(ProducerLedger.NONE, 0);
            return;
        }
        long start = l.realStreakStartTick() == ProducerLedger.NONE ? prev : l.realStreakStartTick();
        double streakYield = (l.realStreakStartTick() == ProducerLedger.NONE ? 0 : l.realStreakYield())
                + Math.max(0, yield);
        long span = now - start;
        if (snap.verifyTicks() > 0 && span >= snap.verifyTicks()) {
            l.recordObservation(streakYield * Ticks.DAY / span);
            l.setRealStreak(now, 0);
        } else {
            l.setRealStreak(start, streakYield);
        }
    }
}
