package io.github.happysnappingturtle.mythicage.core.boss;

/**
 * Boss 机制窗口（总纲 R1b，T23 §6.4）：冲锋撞柱 / 撞墙时是否硬直、硬直多久、受伤倍率。
 * <ul>
 *   <li>撞完整的柱：{@value #PILLAR_TICKS} 刻；撞墙（已碎的柱、外墙、升起的机关墙）：{@value #WALL_TICKS} 刻，
 *       且只在柱子全碎后才算（还有完整的柱时撞墙只停下）；</li>
 *   <li>两次窗口的起点间隔 ≥ {@value #MIN_INTERVAL} 刻（20 秒），冷却内只停下；全场最多 {@value #CAP} 次；</li>
 *   <li>窗口内受伤 ×{@value #DAMAGE_MULTIPLIER}。</li>
 * </ul>
 */
public final class MechanicWindows {
    public static final int PILLAR_TICKS = 60;
    public static final int WALL_TICKS = 40;
    public static final int MIN_INTERVAL = 400;
    public static final int CAP = 12;
    public static final float DAMAGE_MULTIPLIER = 2.0F;
    /** R1b：单次窗口上限 8 秒、倍率上限 ×2。 */
    public static final int R1B_MAX_TICKS = 160;
    public static final float R1B_MAX_MULTIPLIER = 2.0F;

    public enum Impact { INTACT_PILLAR, WALL }

    /** 撞击结果：stunTicks 为 0 表示只停下。 */
    public record Outcome(int stunTicks, float multiplier) {
        public static final Outcome NONE = new Outcome(0, 1.0F);

        public boolean stunned() {
            return stunTicks > 0;
        }
    }

    private int count;
    private long lastStart = Long.MIN_VALUE / 4;

    public Outcome onImpact(Impact impact, long now, boolean allPillarsBroken) {
        if (impact == Impact.WALL && !allPillarsBroken) {
            return Outcome.NONE;
        }
        if (count >= CAP || now - lastStart < MIN_INTERVAL) {
            return Outcome.NONE;
        }
        count++;
        lastStart = now;
        return new Outcome(impact == Impact.INTACT_PILLAR ? PILLAR_TICKS : WALL_TICKS, DAMAGE_MULTIPLIER);
    }

    public int count() {
        return count;
    }

    public long lastStart() {
        return lastStart;
    }

    public void restore(int count, long lastStart) {
        this.count = count;
        this.lastStart = lastStart;
    }

    public void reset() {
        count = 0;
        lastStart = Long.MIN_VALUE / 4;
    }
}
