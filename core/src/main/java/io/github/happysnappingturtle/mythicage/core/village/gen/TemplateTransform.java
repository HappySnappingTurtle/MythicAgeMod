package io.github.happysnappingturtle.mythicage.core.village.gen;

/**
 * 模板放置的坐标换算（T13b §1）：MC 绕模板原点旋转，旋转后包围盒的最小角不再是原点。
 * 规划器给出「旋转后包围盒最小角放在哪」，这里反算模板原点，并把锚点、门换到世界坐标。
 */
public final class TemplateTransform {
    private TemplateTransform() {
    }

    /** 旋转后包围盒最小角相对模板原点的偏移（只有 x、z）。 */
    public static Pos minOffset(int sizeX, int sizeZ, Turn t) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (int x : new int[]{0, sizeX - 1}) {
            for (int z : new int[]{0, sizeZ - 1}) {
                minX = Math.min(minX, t.rx(x, z));
                minZ = Math.min(minZ, t.rz(x, z));
            }
        }
        return new Pos(minX, 0, minZ);
    }

    /** 旋转后的水平尺寸 (宽 x, 深 z)。 */
    public static int[] rotatedSize(int sizeX, int sizeZ, Turn t) {
        return t == Turn.R90 || t == Turn.R270 ? new int[]{sizeZ, sizeX} : new int[]{sizeX, sizeZ};
    }

    /** 想让旋转后包围盒的最小角落在 boxMin，模板原点应放的位置。 */
    public static Pos originFor(Pos boxMin, int sizeX, int sizeZ, Turn t) {
        Pos off = minOffset(sizeX, sizeZ, t);
        return new Pos(boxMin.x() - off.x(), boxMin.y(), boxMin.z() - off.z());
    }

    /** 模板里的相对坐标 → 世界坐标。 */
    public static Pos toWorld(Pos origin, Pos rel, Turn t) {
        return origin.plus(t.apply(rel));
    }

    /** 放置后的世界包围盒。 */
    public static Box worldBox(Pos origin, int sizeX, int sizeY, int sizeZ, Turn t) {
        return Box.of(toWorld(origin, new Pos(0, 0, 0), t), toWorld(origin, new Pos(sizeX - 1, sizeY - 1, sizeZ - 1), t));
    }
}
