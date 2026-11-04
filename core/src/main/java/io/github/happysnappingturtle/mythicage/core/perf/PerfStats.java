package io.github.happysnappingturtle.mythicage.core.perf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 每刻耗时统计（T24 设计文档 §9.1）：各分类在一刻里的耗时先累加，刻末收进环形窗口；
 * 汇总给出窗口内每刻耗时的平均、p95、最大（微秒），以及所有分类相加的「模组合计」。
 * 只在主线程用，不加锁。
 */
public final class PerfStats {
    public static final String TOTAL = "total";

    /** @param samples 窗口里有几刻 */
    public record Summary(double avgMicros, double p95Micros, double maxMicros, int samples) {
        public static final Summary EMPTY = new Summary(0, 0, 0, 0);
    }

    private final int window;
    private final Map<String, long[]> rings = new LinkedHashMap<>();
    private final Map<String, Long> current = new LinkedHashMap<>();
    private int ticks;

    public PerfStats(int window) {
        if (window <= 0) {
            throw new IllegalArgumentException("窗口应为正数：" + window);
        }
        this.window = window;
    }

    /** 记一段耗时（纳秒），累加到这一刻。 */
    public void add(String category, long nanos) {
        current.merge(category, Math.max(0, nanos), Long::sum);
    }

    /** 刻末：每个见过的分类收一个样本（这一刻没跑的记 0），合计另收一个。 */
    public void endTick() {
        long sum = 0;
        for (String c : current.keySet()) {
            rings.computeIfAbsent(c, k -> new long[window]);
        }
        int slot = ticks % window;
        for (Map.Entry<String, long[]> e : rings.entrySet()) {
            if (e.getKey().equals(TOTAL)) {
                continue;
            }
            long v = current.getOrDefault(e.getKey(), 0L);
            e.getValue()[slot] = v;
            sum += v;
        }
        rings.computeIfAbsent(TOTAL, k -> new long[window])[slot] = sum;
        current.clear();
        ticks++;
    }

    public int ticks() {
        return ticks;
    }

    public Summary summary(String category) {
        long[] r = rings.get(category);
        int n = Math.min(ticks, window);
        if (r == null || n == 0) {
            return Summary.EMPTY;
        }
        long[] v = Arrays.copyOf(r, n);
        Arrays.sort(v);
        double sum = 0;
        for (long x : v) {
            sum += x;
        }
        int p95 = Math.min(n - 1, (int) Math.ceil(n * 0.95) - 1);
        return new Summary(sum / n / 1000.0, v[p95] / 1000.0, v[n - 1] / 1000.0, n);
    }

    /** 各分类（不含合计），按平均耗时从高到低。 */
    public List<String> categories() {
        List<String> out = new ArrayList<>(rings.keySet());
        out.remove(TOTAL);
        out.sort((a, b) -> Double.compare(summary(b).avgMicros(), summary(a).avgMicros()));
        return out;
    }

    public void reset() {
        rings.clear();
        current.clear();
        ticks = 0;
    }
}
