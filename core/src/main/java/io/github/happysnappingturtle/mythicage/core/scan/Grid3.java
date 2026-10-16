package io.github.happysnappingturtle.mythicage.core.scan;

import java.util.BitSet;

/** 扫描体内的布尔位图，局部坐标从 0 开始。索引顺序 y → z → x，与扫描器逐格推进的顺序一致。 */
public final class Grid3 {
    private final int sx;
    private final int sy;
    private final int sz;
    private final BitSet bits;

    public Grid3(int sx, int sy, int sz) {
        if (sx <= 0 || sy <= 0 || sz <= 0) {
            throw new IllegalArgumentException("尺寸必须为正：" + sx + "×" + sy + "×" + sz);
        }
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.bits = new BitSet(sx * sy * sz);
    }

    public int sizeX() {
        return sx;
    }

    public int sizeY() {
        return sy;
    }

    public int sizeZ() {
        return sz;
    }

    public int volume() {
        return sx * sy * sz;
    }

    public boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz;
    }

    public int index(int x, int y, int z) {
        return (y * sz + z) * sx + x;
    }

    /** 越界读取为 false。 */
    public boolean get(int x, int y, int z) {
        return inside(x, y, z) && bits.get(index(x, y, z));
    }

    public void set(int x, int y, int z, boolean value) {
        if (!inside(x, y, z)) {
            throw new IndexOutOfBoundsException(x + "," + y + "," + z);
        }
        bits.set(index(x, y, z), value);
    }

    public void setIndex(int index, boolean value) {
        bits.set(index, value);
    }

    public int cardinality() {
        return bits.cardinality();
    }

    public void clear() {
        bits.clear();
    }

    /** 索引 → [x, y, z]。 */
    public int[] coords(int index) {
        int x = index % sx;
        int rest = index / sx;
        return new int[]{x, rest / sz, rest % sz};
    }
}
