package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 已建范围（V0 §5.1、W4）：已建建筑包围盒的并集，水平各外扩 {@link #MARGIN} 格；用于「在不在村里」。
 */
public final class BuiltArea {
    public static final int MARGIN = 4;

    private final List<Box> boxes;

    public BuiltArea(List<Box> raw) {
        this.boxes = raw.stream().map(b -> b.inflateXZ(MARGIN)).toList();
    }

    public List<Box> boxes() {
        return boxes;
    }

    public boolean contains(int x, int z) {
        return within(x, z, 0);
    }

    /** 在已建范围再外扩 extra 格之内（离开判定用）。 */
    public boolean within(int x, int z, int extra) {
        for (Box b : boxes) {
            if (b.inflateXZ(extra).containsXZ(x, z)) {
                return true;
            }
        }
        return false;
    }

    /** 覆盖到的全部区块（区块坐标打包成 long，与 MC 的 ChunkPos.asLong 相同）。 */
    public Set<Long> chunks() {
        Set<Long> out = new LinkedHashSet<>();
        for (Box b : boxes) {
            for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
                for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
                    out.add(chunkKey(cx, cz));
                }
            }
        }
        return out;
    }

    public static long chunkKey(int cx, int cz) {
        return (long) cx & 0xFFFFFFFFL | ((long) cz & 0xFFFFFFFFL) << 32;
    }
}
