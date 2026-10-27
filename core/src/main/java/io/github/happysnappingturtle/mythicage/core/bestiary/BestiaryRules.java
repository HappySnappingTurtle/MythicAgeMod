package io.github.happysnappingturtle.mythicage.core.bestiary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * 图鉴三级点亮（T24 设计文档 §6.3，01 号 §12.2）：目击 → 交战 → 击败 / 驯服，只升不降。
 * 记在玩家数据的 {@code discoveries} 里：{@code bestiary:<条目>:<级别>}，每到一级就加一条（加到高级时低级也补上），
 * 只增不删，所以不用改存档格式。
 */
public final class BestiaryRules {
    public static final String PREFIX = "bestiary:";
    /** 目击距离（格）。 */
    public static final double SIGHT_RANGE = 24;
    /** 每名玩家每次抽查最多处理几只。 */
    public static final int SIGHT_PER_CHECK = 8;

    private BestiaryRules() {
    }

    public enum Tier {
        NONE, SIGHTED, ENGAGED, DEFEATED;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static String key(String entry, Tier tier) {
        return PREFIX + entry + ":" + tier.id();
    }

    /** 这名玩家在这个条目上到了哪一级。 */
    public static Tier tier(Set<String> discoveries, String entry) {
        for (int i = Tier.values().length - 1; i > 0; i--) {
            Tier t = Tier.values()[i];
            if (discoveries.contains(key(entry, t))) {
                return t;
            }
        }
        return Tier.NONE;
    }

    /**
     * 记一次事件：把 event 及以下各级都补上。返回新到达的级别（从低到高）；已到或更高时返回空表。
     */
    public static List<Tier> raise(Set<String> discoveries, String entry, Tier event) {
        List<Tier> fresh = new ArrayList<>();
        for (int i = 1; i <= event.ordinal(); i++) {
            Tier t = Tier.values()[i];
            if (discoveries.add(key(entry, t))) {
                fresh.add(t);
            }
        }
        return fresh;
    }

    /** 在不在目击距离内（含边界）。 */
    public static boolean inSight(double distanceSq) {
        return distanceSq <= SIGHT_RANGE * SIGHT_RANGE;
    }

    /** 一次抽查：距离内的候选按远近取最近的 {@value #SIGHT_PER_CHECK} 个。 */
    public static <T> List<T> nearest(List<T> candidates, ToDoubleFunction<T> distanceSq) {
        return candidates.stream().filter(c -> inSight(distanceSq.applyAsDouble(c)))
                .sorted(Comparator.comparingDouble(distanceSq)).limit(SIGHT_PER_CHECK).toList();
    }
}
