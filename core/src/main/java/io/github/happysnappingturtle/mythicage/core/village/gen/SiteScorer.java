package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 选址评分（V0 §3.4）：在候选区块中心周围取 9 个候选中心（中心 + 8 方向 32 格），每个候选取 3 × 3 个
 * 地表采样（间距 8），按配置打分取最高；有水或全部低于下限就放弃。每个候选点共 81 × 2 次高度采样（D5 预算内）。
 */
public final class SiteScorer {
    public static final int RING = 32;
    public static final int STEP = 8;

    /**
     * @param flatness   平坦度权重
     * @param highGround 局部高点权重（候选比所有候选的平均高出 8 格得满分）
     * @param minScore   下限
     * @param maxRelief  候选内高差达到这个值时平坦度为 0
     */
    public record Config(double flatness, double highGround, double minScore, int maxRelief) {
        public static final Config DEFAULT = new Config(1.0, 0.3, 0.35, 12);
    }

    /** 选中的中心：y 是地表上第一格（候选内 9 个采样的中位数）。 */
    public record Site(int x, int y, int z, double score) {
    }

    private SiteScorer() {
    }

    public static Optional<Site> best(int cx, int cz, TerrainSampler t, Config cfg) {
        List<int[]> cands = new ArrayList<>();
        cands.add(new int[]{cx, cz});
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    cands.add(new int[]{cx + dx * RING, cz + dz * RING});
                }
            }
        }
        List<double[]> stats = new ArrayList<>(); // x, z, mean, relief, median, ok
        double sumMean = 0;
        int okCount = 0;
        for (int[] c : cands) {
            int[] hs = new int[9];
            boolean wet = false;
            int i = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = c[0] + dx * STEP;
                    int z = c[1] + dz * STEP;
                    int s = t.surface(x, z);
                    wet |= s > t.floor(x, z);
                    hs[i++] = s;
                }
            }
            java.util.Arrays.sort(hs);
            double mean = java.util.Arrays.stream(hs).average().orElse(0);
            stats.add(new double[]{c[0], c[1], mean, hs[8] - hs[0], hs[4], wet ? 0 : 1});
            if (!wet) {
                sumMean += mean;
                okCount++;
            }
        }
        if (okCount == 0) {
            return Optional.empty();
        }
        double globalMean = sumMean / okCount;
        return stats.stream().filter(s -> s[5] > 0).map(s -> {
            double flat = 1.0 - Math.min(1.0, s[3] / cfg.maxRelief());
            double high = Math.max(-1.0, Math.min(1.0, (s[2] - globalMean) / 8.0));
            double score = cfg.flatness() * flat + cfg.highGround() * high;
            return new Site((int) s[0], (int) s[4], (int) s[1], score);
        }).filter(s -> s.score() >= cfg.minScore()).max(Comparator.comparingDouble(Site::score)
                .thenComparing(s -> -(Math.abs(s.x() - cx) + Math.abs(s.z() - cz))));
    }
}
