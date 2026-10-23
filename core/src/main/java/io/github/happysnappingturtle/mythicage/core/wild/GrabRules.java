package io.github.happysnappingturtle.mythicage.core.wild;

/**
 * 哈耳庇厄俯冲接触目标时的判定（01_希腊 W2、T15 设计文档 §4、§9 D3；总纲 R1 免疫）。纯计算，世界查询由调用方完成。
 * <ol>
 *   <li>目标正面举盾挡住 → 失败，哈耳庇厄坠地眩晕；</li>
 *   <li>目标还在抓举免疫期（上次被抓后 8 秒）、离崖巢 24 格内、附近 6 格内有 &gt; 4 格的落差、或体型太大 → 改为爪击 + 击退；</li>
 *   <li>否则抓起：升高 ≤ 3 格，20 刻后松手。</li>
 * </ol>
 */
public final class GrabRules {
    public enum Outcome { GRAB, SWIPE, FAIL_STUN }

    /** 被抓后免疫抓举的刻数（8 秒）。同一目标 1 秒内被另一只抓也落在这段免疫里。 */
    public static final int IMMUNE_TICKS = 160;
    /** 抓住后多少刻松手。 */
    public static final int HOLD_TICKS = 20;
    /** 最多把目标提高多少格。 */
    public static final double MAX_LIFT = 3.0;
    /** 崖巢多少格内不抓举。 */
    public static final double NEST_SAFE = 24.0;
    /** 目标附近多少格内查落差、落差超过多少格不抓举。 */
    public static final int CLIFF_RANGE = 6;
    public static final int CLIFF_DROP = 4;
    /** 松手时给目标记上的已下落距离：再摔 3 格总计 6 → 原版摔落伤害 3（1.5 心），穿摔落保护的靴子照常减免。 */
    public static final float RELEASE_FALL = 3.0F;
    /** 能抓起的体型上限（格）。 */
    public static final double MAX_WIDTH = 1.0;
    public static final double MAX_HEIGHT = 2.0;

    private GrabRules() {
    }

    /**
     * @param shieldBlocked 目标正举着盾、且盾挡住了来自哈耳庇厄方向的攻击
     * @param immuneUntil   目标的抓举免疫截止刻（没有为 0）
     * @param nestDistSq    目标离这只哈耳庇厄的崖巢的距离平方（没有巢时传 {@link Double#POSITIVE_INFINITY}）
     * @param maxDropNearby 目标脚下及周围 {@link #CLIFF_RANGE} 格内的最大落差（格）
     */
    public static Outcome contact(boolean shieldBlocked, long now, long immuneUntil, double nestDistSq, int maxDropNearby,
                                  double targetWidth, double targetHeight) {
        if (shieldBlocked) {
            return Outcome.FAIL_STUN;
        }
        if (now < immuneUntil || nestDistSq <= NEST_SAFE * NEST_SAFE || maxDropNearby > CLIFF_DROP
                || targetWidth > MAX_WIDTH || targetHeight > MAX_HEIGHT) {
            return Outcome.SWIPE;
        }
        return Outcome.GRAB;
    }
}
