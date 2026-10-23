package io.github.happysnappingturtle.mythicage.core.relic;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 效果组（06 号 §0.4）：不同名但效果相同的圣物归入同一组，同组取最大值、不相加。
 * 不同组之间：百分比增益加算到同一乘区，百分比减伤乘算。属性修饰按效果组使用固定 id。
 * 这里登记已经有代码实现的组；圣物卡只能引用这些组。
 */
public final class EffectGroups {
    /** 数值的含义，决定合并与校验方式。 */
    public enum Kind {
        /** 固定值（恩宠上限 +50、生命 +4）。 */
        FLAT,
        /** 百分比增益（0–1），不同组相加。 */
        PERCENT_BONUS,
        /** 百分比减伤（0–1），不同组相乘。 */
        PERCENT_REDUCTION
    }

    public static final String MELEE_TAKEN_SELF = "melee_damage_taken.self";
    public static final String MELEE_TAKEN_AREA_ALLIES = "melee_damage_taken.area_allies";
    public static final String MAX_HEALTH_SELF_FLAT = "max_health.self_flat";
    public static final String MAX_HEALTH_BELIEVER_VILLAGER = "max_health.believer_villager";

    private static final Map<String, Kind> KNOWN = new LinkedHashMap<>();

    static {
        for (Civilization c : Civilization.values()) {
            KNOWN.put(favorCap(c), Kind.FLAT);
            KNOWN.put(prayerR(c), Kind.PERCENT_BONUS);
        }
        KNOWN.put(MELEE_TAKEN_SELF, Kind.PERCENT_REDUCTION);
        KNOWN.put(MELEE_TAKEN_AREA_ALLIES, Kind.PERCENT_REDUCTION);
        KNOWN.put(MAX_HEALTH_SELF_FLAT, Kind.FLAT);
        KNOWN.put(MAX_HEALTH_BELIEVER_VILLAGER, Kind.PERCENT_BONUS);
    }

    private EffectGroups() {
    }

    public static String favorCap(Civilization civ) {
        return "favor_cap." + civ.id();
    }

    /** 祈祷公式里的 R（希腊用；其他文明的同名组留给各自的产出公式）。 */
    public static String prayerR(Civilization civ) {
        return "prayer.R." + civ.id();
    }

    public static Optional<Kind> kindOf(String group) {
        return Optional.ofNullable(KNOWN.get(group));
    }

    /** 一条效果：组 id 与数值。 */
    public record Effect(String group, double value) {
    }

    /** 合并：同组取最大值。 */
    public static Map<String, Double> merge(Collection<Effect> effects) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Effect e : effects) {
            out.merge(e.group(), e.value(), Math::max);
        }
        return out;
    }

    /** 若干组的百分比增益之和（同一乘区）。 */
    public static double bonus(Map<String, Double> merged, Collection<String> groups) {
        double sum = 0;
        for (String g : groups) {
            sum += merged.getOrDefault(g, 0.0);
        }
        return sum;
    }

    /** 若干百分比减伤合成后剩下的伤害倍率：∏(1 − r)。 */
    public static double remaining(Collection<Double> reductions) {
        double m = 1;
        for (double r : reductions) {
            m *= 1 - Math.max(0, Math.min(1, r));
        }
        return m;
    }
}
