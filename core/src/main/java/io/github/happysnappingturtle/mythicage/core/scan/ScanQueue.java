package io.github.happysnappingturtle.mythicage.core.scan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全服神庙扫描队列（T8a 设计文档 §3.5）：每刻最多处理 budget 格，一个任务可以跨刻完成；
 * 同一键重复入队只保留一个任务；任务进行中再次置脏时，本轮作废、从头开始，
 * 避免用半新半旧的数据下结论。纯逻辑，扫描本身由 {@link Worker} 完成。
 */
public final class ScanQueue<K> {
    /** 扫描执行者。start 为 0 时表示新一轮开始，应清空上一轮的中间结果。 */
    public interface Worker<K> {
        void scan(K key, int start, int count);
    }

    private static final class Job {
        final int total;
        int progress;

        Job(int total) {
            this.total = total;
        }
    }

    private final Map<K, Job> jobs = new LinkedHashMap<>();

    /** 入队或置脏。已在队列中时：进度归零（如已开始），位置不变。 */
    public void submit(K key, int totalCells) {
        if (totalCells <= 0) {
            throw new IllegalArgumentException("格数必须为正：" + totalCells);
        }
        Job job = jobs.get(key);
        if (job == null || job.total != totalCells) {
            jobs.put(key, new Job(totalCells));
        } else {
            job.progress = 0;
        }
    }

    public void cancel(K key) {
        jobs.remove(key);
    }

    public boolean contains(K key) {
        return jobs.containsKey(key);
    }

    public int size() {
        return jobs.size();
    }

    /**
     * 推进一刻。
     *
     * @return 本刻完成的键（按完成顺序）
     */
    public List<K> tick(int budget, Worker<K> worker) {
        if (budget <= 0) {
            throw new IllegalArgumentException("预算必须为正：" + budget);
        }
        List<K> done = new ArrayList<>();
        var it = jobs.entrySet().iterator();
        while (budget > 0 && it.hasNext()) {
            var e = it.next();
            Job job = e.getValue();
            int n = Math.min(budget, job.total - job.progress);
            worker.scan(e.getKey(), job.progress, n);
            job.progress += n;
            budget -= n;
            if (job.progress >= job.total) {
                it.remove();
                done.add(e.getKey());
            }
        }
        return done;
    }
}
