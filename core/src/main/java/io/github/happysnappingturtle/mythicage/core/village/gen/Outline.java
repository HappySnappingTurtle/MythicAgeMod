package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 一组水平矩形（{@link Box} 的 XZ，格子含两端）并集的外轮廓（T24 设计文档 §4.4「显示村界」）：输出单位长度的边，
 * 客户端逐段贴着地表画。重叠的矩形只画外缘，不画内部接缝。
 */
public final class Outline {
    /** 并集面积上限（格）：超出就退回各自画矩形边框，避免巨型村庄卡顿。 */
    public static final int MAX_CELLS = 200_000;

    private Outline() {
    }

    /** 单位边：{@code alongX} 为真时从 (x, z) 到 (x+1, z)，否则从 (x, z) 到 (x, z+1)（方块角坐标）。 */
    public record Edge(int x, int z, boolean alongX) {
    }

    public static List<Edge> union(List<Box> boxes) {
        long area = 0;
        for (Box b : boxes) {
            area += (long) (b.maxX() - b.minX() + 1) * (b.maxZ() - b.minZ() + 1);
        }
        if (area > MAX_CELLS) {
            List<Edge> out = new ArrayList<>();
            boxes.forEach(b -> out.addAll(rect(b)));
            return List.copyOf(out);
        }
        Set<Long> cells = new HashSet<>();
        for (Box b : boxes) {
            for (int x = b.minX(); x <= b.maxX(); x++) {
                for (int z = b.minZ(); z <= b.maxZ(); z++) {
                    cells.add(key(x, z));
                }
            }
        }
        List<Edge> out = new ArrayList<>();
        for (long c : cells) {
            int x = (int) (c >> 32);
            int z = (int) c;
            if (!cells.contains(key(x, z - 1))) {
                out.add(new Edge(x, z, true));
            }
            if (!cells.contains(key(x, z + 1))) {
                out.add(new Edge(x, z + 1, true));
            }
            if (!cells.contains(key(x - 1, z))) {
                out.add(new Edge(x, z, false));
            }
            if (!cells.contains(key(x + 1, z))) {
                out.add(new Edge(x + 1, z, false));
            }
        }
        out.sort(java.util.Comparator.comparingInt(Edge::x).thenComparingInt(Edge::z).thenComparing(Edge::alongX));
        return List.copyOf(out);
    }

    /** 一个矩形的边框（单位边）。 */
    public static List<Edge> rect(Box b) {
        List<Edge> out = new ArrayList<>();
        for (int x = b.minX(); x <= b.maxX(); x++) {
            out.add(new Edge(x, b.minZ(), true));
            out.add(new Edge(x, b.maxZ() + 1, true));
        }
        for (int z = b.minZ(); z <= b.maxZ(); z++) {
            out.add(new Edge(b.minX(), z, false));
            out.add(new Edge(b.maxX() + 1, z, false));
        }
        return out;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
