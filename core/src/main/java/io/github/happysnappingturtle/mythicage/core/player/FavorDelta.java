package io.github.happysnappingturtle.mythicage.core.player;

/**
 * 恩宠 HUD 的「最近变化」（T24 §5.4，12 号 §4「最近变化、来源；没有改变时不刷屏」）：窗口内的连续变化合并成一条，
 * 超出窗口重新开始。来源取最近一次带来源的变化。
 *
 * @param delta  合并后的变化
 * @param at     最近一次变化的时刻（毫秒）
 * @param source 来源语言键；未知为空串
 */
public record FavorDelta(long delta, long at, String source) {
    public static final FavorDelta NONE = new FavorDelta(0, Long.MIN_VALUE / 2, "");
    /** 合并窗口（毫秒）。 */
    public static final long WINDOW = 3000;

    /** 记一次变化：距上次不超过窗口就累加（来源为空时沿用旧来源），否则重开。0 变化不改动。 */
    public FavorDelta add(long change, long now, String src) {
        if (change == 0) {
            return this;
        }
        if (now - at <= WINDOW) {
            return new FavorDelta(delta + change, now, src.isEmpty() ? source : src);
        }
        return new FavorDelta(change, now, src);
    }

    /** 只补来源（来源包与数值包先后不定）：窗口内才补。 */
    public FavorDelta withSource(String src, long now) {
        return now - at <= WINDOW ? new FavorDelta(delta, at, src) : this;
    }

    /** 此刻是否还该显示（距最近变化不超过 showMillis，且合并后不为 0）。 */
    public boolean visible(long now, long showMillis) {
        return delta != 0 && now - at <= showMillis;
    }
}
