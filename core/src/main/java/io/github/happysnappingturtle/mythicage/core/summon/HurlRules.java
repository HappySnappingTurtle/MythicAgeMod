package io.github.happysnappingturtle.mythicage.core.summon;

import io.github.happysnappingturtle.mythicage.core.wild.GrabRules;

/**
 * 独眼巨人召唤的特殊攻击「抓投」（SU-GR-04，T25a §3）：伸手 {@value #WINDUP_TICKS} 刻 → 抓起一个非神话、非 Boss 的敌人
 * → 向前扔 {@value #DISTANCE} 格；被扔者 {@value #HIT_DAMAGE} 伤害，落点 {@value #SPLASH_RADIUS} 格内溅射 {@value #SPLASH_DAMAGE}。
 * 神话生物、Boss 抓空（D6、D7，冷却只算 {@value #MISS_COOLDOWN} 刻）；玩家只击退（D8）。抓取免疫与体型上限沿用 {@link GrabRules}。
 */
public final class HurlRules {
    public static final int WINDUP_TICKS = 20;
    public static final double DISTANCE = 8.0;
    public static final float HIT_DAMAGE = 12.0F;
    public static final float SPLASH_DAMAGE = 6.0F;
    public static final double SPLASH_RADIUS = 2.0;
    public static final int COOLDOWN = 400;
    public static final int MISS_COOLDOWN = 100;
    /** 沿抛线往回找空位的步长（格）。 */
    public static final double STEP = 0.5;

    public enum Outcome {
        /** 抓起扔出。 */
        GRAB,
        /** 伸手抓空、挥一下（神话生物、Boss、抓取免疫或体型太大）。 */
        MISS,
        /** 玩家：只击退，不抓。 */
        KNOCKBACK
    }

    private HurlRules() {
    }

    /**
     * @param mythic      目标在 {@code #mythic_age:mythic}（含精英、召唤、Boss）
     * @param boss        目标是 Boss
     * @param player      目标是玩家（PvP）
     * @param immuneUntil 目标的抓取免疫截止刻
     */
    public static Outcome decide(boolean mythic, boolean boss, boolean player, long now, long immuneUntil, double width,
                                 double height) {
        if (mythic || boss) {
            return Outcome.MISS;
        }
        if (player) {
            return Outcome.KNOCKBACK;
        }
        if (now < immuneUntil || width > GrabRules.MAX_WIDTH || height > GrabRules.MAX_HEIGHT) {
            return Outcome.MISS;
        }
        return Outcome.GRAB;
    }

    public static int cooldown(Outcome o) {
        return o == Outcome.MISS ? MISS_COOLDOWN : COOLDOWN;
    }

    /** 某个距离上能不能落（目标的碰撞箱在那里不压方块、脚下也不是空的由平台层判）。 */
    @FunctionalInterface
    public interface Clear {
        boolean at(double distance);
    }

    /** 落点：从 {@link #DISTANCE} 沿抛线往回找第一个能落的位置；一直找不到就放在原地（0）。 */
    public static double landing(Clear clear) {
        for (double d = DISTANCE; d > 0; d -= STEP) {
            if (clear.at(d)) {
                return d;
            }
        }
        return 0;
    }
}
