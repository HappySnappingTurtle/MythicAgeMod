package io.github.happysnappingturtle.mythicage.core.config.runtime;

/**
 * {@code runtime.json}：运行时钟、离场与离线结算（决策 A2，方案 E）。
 * 字段含义见 {@code docs/重构/设计/跨文明详细设计/03_A2运行时钟与补结算提案.md} §13。
 */
public record RuntimeConfig(Preset preset, Favor favor, Norse norse, Village village) {

    public enum Preset { A, B, C, E, F }

    public enum FavorMode { ONLINE_SNAPSHOT, CAPPED_CATCHUP, LOADED_ONLY }

    public enum OfflineClock { SERVER_TICKS, WALL_CLOCK }

    public enum PublicMode { LOGICAL_WHEN_NOT_REAL, LOADED_ONLY }

    public record Favor(FavorMode mode, Idle idle, Snapshot snapshot, Offline offline, CatchUp catchUp) {
    }

    /** 挂机判定：超过宽限期按下线处理。 */
    public record Idle(boolean enabled, int graceTicks) {
    }

    /** 产能快照：照看期、未验证折扣等。 */
    public record Snapshot(double rateFactor, long maxTicksSinceVisit, long visitMinTicks, long verifyTicks,
                           double unverifiedFactor) {
    }

    /** 下线期间的储备（方案 F 使用；E 中 rate 为 0）。 */
    public record Offline(double rate, double maxBufferDays, OfflineClock clock) {
    }

    /** 方案 C 的补算上限。 */
    public record CatchUp(long maxSimulationTicks) {
    }

    public record Norse(Rested rested) {
    }

    /** 北欧「战意」储备（方案 F 使用）。 */
    public record Rested(boolean enabled, double multiplier, double maxBufferDays, boolean bypassDailyCap) {
    }

    public record Village(ForceLoad forceLoad, PublicResources publicResources, Construction construction,
                          Population population) {
    }

    public record ForceLoad(boolean enabled) {
    }

    public record PublicResources(PublicMode mode, int realRunRadius, boolean requireOwnerOnline, long maxCatchUpTicks,
                                  double rateFactor) {
    }

    /** 施工：离开期间不落块，只积累储备。simulateWhenUnloaded 恒为 false。 */
    public record Construction(boolean simulateWhenUnloaded, long bankMaxTicks, double bankSpendMultiplier) {
    }

    /** 人口不离线模拟。simulateWhenUnloaded 恒为 false。 */
    public record Population(boolean simulateWhenUnloaded) {
    }
}
