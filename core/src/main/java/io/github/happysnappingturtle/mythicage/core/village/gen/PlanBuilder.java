package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规划器的共用工具：把一座建筑放到地形上（取地面高度、判泡水与高差）、收集锚点。
 * 放不下时抛 {@link Rejected}，由规划器换方案。
 */
public final class PlanBuilder {
    /** 一座建筑占地内允许的最大地表高差（超过就算这块地不能用；阶段 ② 的整平工序再放宽）。 */
    public static final int MAX_FOOTPRINT_RELIEF = 6;

    public static final class Rejected extends RuntimeException {
        public Rejected(String reason) {
            super(reason, null, false, false);
        }
    }

    private final TerrainSampler terrain;
    private final List<VillagePlan.Placement> placed = new ArrayList<>();
    private final Map<String, List<Pos>> anchors = new LinkedHashMap<>();

    public PlanBuilder(TerrainSampler terrain) {
        this.terrain = terrain;
    }

    /** 地面高度：占地四角与中心的地表中位数（地表上第一格）。 */
    public int groundY(int minX, int minZ, int sizeX, int sizeZ) {
        int maxX = minX + sizeX - 1;
        int maxZ = minZ + sizeZ - 1;
        int[][] pts = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}, {(minX + maxX) / 2, (minZ + maxZ) / 2}};
        int[] hs = new int[pts.length];
        for (int i = 0; i < pts.length; i++) {
            if (terrain.water(pts[i][0], pts[i][1])) {
                throw new Rejected("泡水");
            }
            hs[i] = terrain.surface(pts[i][0], pts[i][1]);
        }
        Arrays.sort(hs);
        if (hs[hs.length - 1] - hs[0] > MAX_FOOTPRINT_RELIEF) {
            throw new Rejected("高差过大");
        }
        return hs[hs.length / 2];
    }

    /** 把建筑旋转后包围盒的最小角放在 (minX, minZ)，地面高度自动取。返回放置结果。 */
    public VillagePlan.Placement place(BuildingSpec spec, int minX, int minZ, Turn turn) {
        int[] size = TemplateTransform.rotatedSize(spec.sizeX(), spec.sizeZ(), turn);
        int ground = groundY(minX, minZ, size[0], size[1]);
        Pos origin = TemplateTransform.originFor(new Pos(minX, ground - spec.floorY(), minZ), spec.sizeX(), spec.sizeZ(), turn);
        Box box = TemplateTransform.worldBox(origin, spec.sizeX(), spec.sizeY(), spec.sizeZ(), turn);
        VillagePlan.Placement p = new VillagePlan.Placement(spec.id(), spec.template(), origin, turn, box);
        placed.add(p);
        spec.anchors().forEach((name, pts) -> pts.forEach(rel ->
                anchor(name, TemplateTransform.toWorld(origin, rel, turn))));
        return p;
    }

    public void anchor(String name, Pos p) {
        anchors.computeIfAbsent(name, k -> new ArrayList<>()).add(p);
    }

    public int surface(int x, int z) {
        return terrain.surface(x, z);
    }

    public VillagePlan build(String civ, String patron, String layout, int version) {
        List<Pos> center = anchors.getOrDefault("center", List.of());
        if (center.isEmpty()) {
            throw new Rejected("缺件：锚点 center");
        }
        return new VillagePlan(civ, patron, layout, version, center.get(0), placed, anchors);
    }
}
