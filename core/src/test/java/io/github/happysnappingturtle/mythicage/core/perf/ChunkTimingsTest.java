package io.github.happysnappingturtle.mythicage.core.perf;

import org.junit.jupiter.api.Test;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkTimingsTest {
    @Test
    void generatedDenominatorIncludesZeroWorkChunksWithoutDuplicates() {
        ChunkTimings s = new ChunkTimings();
        s.add("busy", 6000);
        s.add("notLoadedInWindow", 999_000);
        s.generated("busy");
        s.generated("empty");
        s.generated("empty");
        assertEquals(2, s.generatedCount());
        assertEquals(502.5, s.amortizedMicros(), "为相邻未载入区块做的工作也必须计入总成本，(6 + 999) / 2");
    }

    @Test
    void sumsPiecesWithinChunkRatherThanCountingEachPiece() {
        ChunkTimings s = new ChunkTimings();
        s.add("overworld:0,0", 1000);
        s.add("overworld:0,0", 2000);
        s.add("nether:0,0", 5000);
        assertEquals(2, s.summary().samples());
        assertEquals(4, s.summary().avgMicros());
        assertEquals(5, s.summary().maxMicros());
    }

    @Test
    void supportsConcurrentWorkersAndEmptyReport() {
        ChunkTimings s = new ChunkTimings();
        assertEquals(PerfStats.Summary.EMPTY, s.summary());
        IntStream.range(0, 1000).parallel().forEach(i -> s.add("same", 1000));
        assertEquals(1000, s.summary().avgMicros());
    }
}
