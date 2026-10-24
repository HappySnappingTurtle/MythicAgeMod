package io.github.happysnappingturtle.mythicage.core.conduct;

/** 善恶值的纯规则（T18 设计文档 §4.1，02 号合同 §1、§2、§5）。 */
public final class ConductRules {
    private ConductRules() {
    }

    /** 三档（消费方各自给阈值，如阿米特 ±20、麒麟 / 穷奇 ±10）。 */
    public enum Band { GOOD, NEUTRAL, EVIL }

    public static int clamp(int score, int min, int max) {
        return Math.max(min, Math.min(max, score));
    }

    /** 向 0 靠近 points 点，不越过 0。 */
    public static int regress(int score, int points) {
        if (score > 0) {
            return Math.max(0, score - points);
        }
        if (score < 0) {
            return Math.min(0, score + points);
        }
        return 0;
    }

    /** 今天已得 granted 点正分、上限 cap 时，这次 delta（> 0）实际能加多少。负值原样返回。 */
    public static int positiveAllowance(int delta, int granted, int cap) {
        if (delta <= 0) {
            return delta;
        }
        return Math.max(0, Math.min(delta, cap - granted));
    }

    /** score ≥ good 为善，≤ −evil 为恶（good、evil 都传正数）。 */
    public static Band band(int score, int good, int evil) {
        if (score >= good) {
            return Band.GOOD;
        }
        if (score <= -evil) {
            return Band.EVIL;
        }
        return Band.NEUTRAL;
    }
}
