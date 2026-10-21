package io.github.happysnappingturtle.mythicage.core.village;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 村庄指引（T13b 设计文档 §7）的候选顺序：结构集每个格子算出一个可能的起点区块，按到玩家的距离由近到远排，
 * 去掉半径外的与玩家进过的；搜索按这个顺序逐个确认，第一个确认存在的就是最近的。
 */
public final class GuideCandidates {
    private GuideCandidates() {
    }

    /** 候选起点（区块坐标）与到玩家所在区块的距离平方。 */
    public record Cand(int chunkX, int chunkZ, long distSq) {
        public long key() {
            return ((long) chunkZ << 32) | (chunkX & 0xFFFFFFFFL);
        }
    }

    /**
     * @param px, pz       玩家所在区块
     * @param starts       每个格子的可能起点 {x, z}（区块坐标）
     * @param visited      玩家进过的起点（编码同 {@link Cand#key()}，与原版 ChunkPos.toLong 相同）
     * @param radiusChunks 搜索半径（区块）
     */
    public static List<Cand> order(int px, int pz, List<int[]> starts, Set<Long> visited, int radiusChunks) {
        List<Cand> out = new ArrayList<>();
        long r2 = (long) radiusChunks * radiusChunks;
        for (int[] s : starts) {
            long dx = s[0] - px;
            long dz = s[1] - pz;
            Cand c = new Cand(s[0], s[1], dx * dx + dz * dz);
            if (c.distSq() <= r2 && !visited.contains(c.key())) {
                out.add(c);
            }
        }
        out.sort(Comparator.comparingLong(Cand::distSq).thenComparingInt(Cand::chunkX).thenComparingInt(Cand::chunkZ));
        return out;
    }

    /** 半径内（含边缘）要看的格子范围：返回 {minRegionX, maxRegionX, minRegionZ, maxRegionZ}。 */
    public static int[] regions(int px, int pz, int radiusChunks, int spacing) {
        return new int[]{Math.floorDiv(px - radiusChunks, spacing), Math.floorDiv(px + radiusChunks, spacing),
                Math.floorDiv(pz - radiusChunks, spacing), Math.floorDiv(pz + radiusChunks, spacing)};
    }
}
