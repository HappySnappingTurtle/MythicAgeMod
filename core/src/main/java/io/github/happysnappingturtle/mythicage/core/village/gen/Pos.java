package io.github.happysnappingturtle.mythicage.core.village.gen;

/** 方块坐标（core 不依赖 MC 的 BlockPos）。 */
public record Pos(int x, int y, int z) {
    public Pos offset(int dx, int dy, int dz) {
        return new Pos(x + dx, y + dy, z + dz);
    }

    public Pos plus(Pos o) {
        return new Pos(x + o.x, y + o.y, z + o.z);
    }

    public long horizontalDistSq(int px, int pz) {
        long dx = x - px;
        long dz = z - pz;
        return dx * dx + dz * dz;
    }
}
