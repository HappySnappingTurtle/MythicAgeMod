package io.github.happysnappingturtle.mythicage.core.power;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 诱兽石的候选挑选（T13 设计文档 §4.1；05 号 GP-GR-02）：同时最多吸引 {@link #CAP} 只；马和驴优先，其余按距离由近到远。
 * 已经在被吸引的动物继续算在数内；满额后不再挑新的（卡片验证 1）。
 */
public final class LureRules {
    public static final int CAP = 24;
    public static final int RADIUS = 48;
    /** 同时最多产鸡数与间隔（3 分钟 / 30 秒）。 */
    public static final int MAX_CHICKENS = 6;
    public static final int CHICKEN_INTERVAL = 600;
    /** 吸引的刷新间隔与候选缓存刷新间隔。 */
    public static final int PULL_INTERVAL = 40;
    public static final int CACHE_INTERVAL = 200;
    /** 走到石头这么近就停下。 */
    public static final double STOP_DISTANCE = 3;

    /**
     * 一个候选。
     *
     * @param equine 马或驴（优先）
     */
    public record Candidate<T>(T id, boolean equine, double distance) {
    }

    private LureRules() {
    }

    /**
     * 本轮新加入吸引的动物。
     *
     * @param already 已在被吸引的数量
     */
    public static <T> List<T> pick(List<Candidate<T>> candidates, int already, int cap) {
        int room = Math.max(0, cap - already);
        List<Candidate<T>> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparing((Candidate<T> c) -> !c.equine()).thenComparingDouble(Candidate::distance));
        List<T> out = new ArrayList<>();
        for (int i = 0; i < sorted.size() && out.size() < room; i++) {
            out.add(sorted.get(i).id());
        }
        return out;
    }

    /** 从放置起经过 elapsed 刻时，按时钟应当已经产过几只鸡（第 30 秒产第 1 只，封顶 6）。 */
    public static int chickensDue(long elapsed) {
        return (int) Math.min(MAX_CHICKENS, Math.max(0, elapsed / CHICKEN_INTERVAL));
    }
}
