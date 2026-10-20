package io.github.happysnappingturtle.mythicage.core.village.gen;

/** 闭区间包围盒（含两端）。 */
public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public Box {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("包围盒最小角大于最大角：" + minX + "," + minY + "," + minZ + " / "
                    + maxX + "," + maxY + "," + maxZ);
        }
    }

    public static Box of(Pos a, Pos b) {
        return new Box(Math.min(a.x(), b.x()), Math.min(a.y(), b.y()), Math.min(a.z(), b.z()),
                Math.max(a.x(), b.x()), Math.max(a.y(), b.y()), Math.max(a.z(), b.z()));
    }

    /** 水平方向（x、z）外扩 n 格，高度不变。 */
    public Box inflateXZ(int n) {
        return new Box(minX - n, minY, minZ - n, maxX + n, maxY, maxZ + n);
    }

    public boolean containsXZ(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean intersectsXZ(Box o) {
        return minX <= o.maxX && maxX >= o.minX && minZ <= o.maxZ && maxZ >= o.minZ;
    }

    public int sizeX() {
        return maxX - minX + 1;
    }

    public int sizeZ() {
        return maxZ - minZ + 1;
    }

    public int centerX() {
        return Math.floorDiv(minX + maxX, 2);
    }

    public int centerZ() {
        return Math.floorDiv(minZ + maxZ, 2);
    }

    /** 到 (x, z) 的水平最远距离的平方（判断是否超出中心 128 格用）。 */
    public long farthestDistSq(int x, int z) {
        long dx = Math.max(Math.abs((long) minX - x), Math.abs((long) maxX - x));
        long dz = Math.max(Math.abs((long) minZ - z), Math.abs((long) maxZ - z));
        return dx * dx + dz * dz;
    }
}
