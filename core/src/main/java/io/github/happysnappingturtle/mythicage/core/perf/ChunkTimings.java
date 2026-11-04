package io.github.happysnappingturtle.mythicage.core.perf;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** 世界生成可跨线程；同一区块的规划与落块耗时相加，不能把每个结构件当成一个区块。 */
public final class ChunkTimings {
    private static final int LIMIT = 65536;
    private final Map<String, Long> nanos = new HashMap<>();
    private final Set<String> generated = new HashSet<>();
    private int dropped;

    public synchronized void add(String chunk, long elapsed) {
        if (!nanos.containsKey(chunk) && nanos.size() >= LIMIT) {
            dropped++;
            return;
        }
        nanos.merge(chunk, Math.max(0, elapsed), Long::sum);
    }

    public synchronized PerfStats.Summary summary() {
        if (nanos.isEmpty()) {
            return PerfStats.Summary.EMPTY;
        }
        PerfStats stats = new PerfStats(nanos.size());
        for (long value : nanos.values()) {
            stats.add("chunk", value);
            stats.endTick();
        }
        return stats.summary("chunk");
    }

    /** 单独统计新生成并载入的区块；没有本模组结构的区块也必须进入总分母。 */
    public synchronized void generated(String chunk) {
        if (!generated.contains(chunk) && generated.size() >= LIMIT) {
            dropped++;
            return;
        }
        generated.add(chunk);
    }

    public synchronized int generatedCount() {
        return generated.size();
    }

    /** 窗口内全部结构工作折算到新生成载入区块；不能丢掉为相邻未载入区块做的规划成本。 */
    public synchronized double amortizedMicros() {
        if (generated.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (long value : nanos.values()) {
            total += value;
        }
        return total / generated.size() / 1000.0;
    }

    public synchronized int dropped() {
        return dropped;
    }
}
