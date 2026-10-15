package io.github.happysnappingturtle.mythicage.core.anim;

/**
 * 冲锋时间线（T7 设计文档 §6.3，卡片 §6）：蓄力 20 刻（第 10 刻起锁向）→ 冲刺（至多 20 刻，可被碰撞提前结束）
 * → 收势 16 刻 → 空闲。纯状态机，不知道实体与世界；T23 的 Boss 直接复用。
 *
 * <p>用法：{@link #start(int)} 后每个服务端刻先读 {@link #phase()} 执行本刻行为，再调用 {@link #advance()}。
 */
public final class ChargeTimeline {
    public static final int WINDUP_TICKS = 20;
    public static final int LOCK_TURN_AT = 10;
    public static final int MAX_RUN_TICKS = 20;
    public static final int RECOVER_TICKS = 16;

    public enum Phase {
        NONE, WINDUP, RUN, RECOVER
    }

    private Phase phase = Phase.NONE;
    private int tick;
    private int runTicks;

    /** 按距离与速度算冲刺刻数，限制在 [0, {@link #MAX_RUN_TICKS}]。 */
    public static int runTicksFor(double distanceBlocks, double blocksPerTick) {
        if (distanceBlocks <= 0) {
            return 0;
        }
        if (blocksPerTick <= 0) {
            throw new IllegalArgumentException("冲刺速度必须为正：" + blocksPerTick);
        }
        return (int) Math.min(MAX_RUN_TICKS, Math.ceil(distanceBlocks / blocksPerTick - 1e-9));
    }

    /** 开始一次冲锋（进行中的冲锋会被重置）。 */
    public void start(int plannedRunTicks) {
        if (plannedRunTicks < 0) {
            throw new IllegalArgumentException("冲刺刻数不能为负：" + plannedRunTicks);
        }
        runTicks = Math.min(MAX_RUN_TICKS, plannedRunTicks);
        phase = Phase.WINDUP;
        tick = 0;
    }

    /** 推进一刻，返回推进后的阶段。 */
    public Phase advance() {
        if (phase == Phase.NONE) {
            return phase;
        }
        tick++;
        switch (phase) {
            case WINDUP -> {
                if (tick >= WINDUP_TICKS) {
                    enter(runTicks > 0 ? Phase.RUN : Phase.RECOVER);
                }
            }
            case RUN -> {
                if (tick >= runTicks) {
                    enter(Phase.RECOVER);
                }
            }
            case RECOVER -> {
                if (tick >= RECOVER_TICKS) {
                    enter(Phase.NONE);
                }
            }
            default -> {
            }
        }
        return phase;
    }

    /** 冲刺中撞到障碍：立即进入收势。其他阶段无效果。 */
    public void stopRun() {
        if (phase == Phase.RUN) {
            enter(Phase.RECOVER);
        }
    }

    /** 立即结束（目标消失、实体被移除等）。 */
    public void cancel() {
        enter(Phase.NONE);
    }

    private void enter(Phase next) {
        phase = next;
        tick = 0;
    }

    public Phase phase() {
        return phase;
    }

    public int tickInPhase() {
        return tick;
    }

    public boolean active() {
        return phase != Phase.NONE;
    }

    /** 只有冲刺阶段发生位移。 */
    public boolean moving() {
        return phase == Phase.RUN;
    }

    /** 蓄力前半段可以转向对准目标；第 {@link #LOCK_TURN_AT} 刻起锁向，直到收势结束。 */
    public boolean canTurn() {
        return phase == Phase.NONE || (phase == Phase.WINDUP && tick < LOCK_TURN_AT);
    }
}
