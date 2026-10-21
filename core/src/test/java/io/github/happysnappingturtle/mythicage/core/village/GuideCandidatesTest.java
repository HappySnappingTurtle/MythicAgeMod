package io.github.happysnappingturtle.mythicage.core.village;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** TC-T13b-72d：村庄指引的候选顺序。 */
class GuideCandidatesTest {
    @Test
    void ordersByDistanceAndSkipsVisitedAndFar() {
        List<int[]> starts = List.of(new int[]{30, 0}, new int[]{5, 5}, new int[]{-10, 2}, new int[]{200, 0}, new int[]{0, -8});
        long visited = new GuideCandidates.Cand(5, 5, 0).key();
        List<GuideCandidates.Cand> c = GuideCandidates.order(0, 0, starts, Set.of(visited), 100);
        assertEquals(3, c.size(), "进过的 (5,5) 与 100 区块外的 (200,0) 不算");
        assertEquals(0, c.get(0).chunkX());
        assertEquals(-8, c.get(0).chunkZ());
        assertEquals(-10, c.get(1).chunkX());
        assertEquals(30, c.get(2).chunkX());
    }

    @Test
    void keyMatchesVanillaChunkPosEncoding() {
        // 原版 ChunkPos.asLong(x, z) = (x & 0xFFFFFFFFL) | (z & 0xFFFFFFFFL) << 32
        assertEquals((-3L & 0xFFFFFFFFL) | (7L << 32), new GuideCandidates.Cand(-3, 7, 0).key());
        assertEquals((5L) | ((-2L & 0xFFFFFFFFL) << 32), new GuideCandidates.Cand(5, -2, 0).key());
    }

    @Test
    void regionsCoverRadius() {
        assertArrayEquals(new int[]{-5, 4, -5, 4}, GuideCandidates.regions(0, 0, 100, 24));
        assertArrayEquals(new int[]{0, 0, 0, 1}, GuideCandidates.regions(10, 30, 10, 24));
    }
}
