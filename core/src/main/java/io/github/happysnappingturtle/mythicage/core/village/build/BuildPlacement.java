package io.github.happysnappingturtle.mythicage.core.village.build;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 施工建筑放在哪（T19 设计文档 §1.4 E2）：
 * <ul>
 *   <li>地块：门朝地块正面；旋转后的模板居中放在地块上，每边最多比地块多出 {@link #APRON} 格（白模的围裙地基）；</li>
 *   <li>原地替换：新原点 = 旧原点 + 旋转后的（新 frame − 旧 frame）；</li>
 *   <li>生成村庄的规划没存「哪座是哪座」，按旧建筑唯一的锚点反推它的原点与转向（包围盒要和规划里的某个已建包围盒完全相同）。</li>
 * </ul>
 */
public final class BuildPlacement {
    public static final int APRON = 1;

    /** 一座建筑在世界里的位置。 */
    public record Placed(String building, Pos origin, Turn turn, Box box) {
    }

    private BuildPlacement() {
    }

    public static Turn turnFor(String doorFacing, String plotFront) {
        return Turn.between(doorFacing, plotFront);
    }

    /** 旋转后的模板能不能放进 w × d（x × z）的地块。 */
    public static boolean fits(int sizeX, int sizeZ, Turn t, int w, int d) {
        int[] r = TemplateTransform.rotatedSize(sizeX, sizeZ, t);
        return r[0] <= w + 2 * APRON && r[1] <= d + 2 * APRON;
    }

    /** 放到地块上：plot 的 y 是整平后地面上第一格。 */
    public static Placed onPlot(BuildingSpec s, Box plot, String plotFront) {
        Turn t = turnFor(s.doorFacing(), plotFront);
        int[] r = TemplateTransform.rotatedSize(s.sizeX(), s.sizeZ(), t);
        int minX = plot.minX() + Math.floorDiv(plot.sizeX() - r[0], 2);
        int minZ = plot.minZ() + Math.floorDiv(plot.sizeZ() - r[1], 2);
        Pos origin = TemplateTransform.originFor(new Pos(minX, plot.minY() - s.floorY(), minZ), s.sizeX(), s.sizeZ(), t);
        return new Placed(s.id(), origin, t, TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), t));
    }

    /** 原地替换：与旧建筑同一个白模坐标系、同一转向。 */
    public static Placed replacing(BuildingSpec next, BuildingSpec old, Placed at) {
        Pos d = new Pos(next.frame().x() - old.frame().x(), next.frame().y() - old.frame().y(),
                next.frame().z() - old.frame().z());
        Pos origin = at.origin().plus(at.turn().apply(d));
        return new Placed(next.id(), origin, at.turn(),
                TemplateTransform.worldBox(origin, next.sizeX(), next.sizeY(), next.sizeZ(), at.turn()));
    }

    /**
     * 反推生成村庄里某座建筑的位置：取建筑数据里只有一个点的锚点，对规划里同名锚点的每个位置、每种转向算出原点与包围盒，
     * 与某个已建包围盒完全相同就是它。
     */
    public static Optional<Placed> locate(BuildingSpec s, Map<String, List<Pos>> worldAnchors, List<Box> built) {
        for (Map.Entry<String, List<Pos>> a : s.anchors().entrySet()) {
            if (a.getValue().size() != 1) {
                continue;
            }
            Pos rel = a.getValue().get(0);
            for (Pos w : worldAnchors.getOrDefault(a.getKey(), List.of())) {
                for (Turn t : Turn.values()) {
                    Pos r = t.apply(rel);
                    Pos origin = new Pos(w.x() - r.x(), w.y() - r.y(), w.z() - r.z());
                    Box box = TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), t);
                    if (built.contains(box)) {
                        return Optional.of(new Placed(s.id(), origin, t, box));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** 同 {@link #locate}，但找出全部（同一种建筑可能有好几座，如坡地小屋）。 */
    public static List<Placed> locateAll(BuildingSpec s, Map<String, List<Pos>> worldAnchors, List<Box> built) {
        List<Placed> out = new java.util.ArrayList<>();
        java.util.Set<Box> seen = new java.util.HashSet<>();
        for (Map.Entry<String, List<Pos>> a : s.anchors().entrySet()) {
            if (a.getValue().size() != 1) {
                continue;
            }
            Pos rel = a.getValue().get(0);
            for (Pos w : worldAnchors.getOrDefault(a.getKey(), List.of())) {
                for (Turn t : Turn.values()) {
                    Pos r = t.apply(rel);
                    Pos origin = new Pos(w.x() - r.x(), w.y() - r.y(), w.z() - r.z());
                    Box box = TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), t);
                    if (built.contains(box) && seen.add(box)) {
                        out.add(new Placed(s.id(), origin, t, box));
                    }
                }
            }
            return out;
        }
        return out;
    }

    /** 建筑的锚点换到世界坐标。 */
    public static Map<String, List<Pos>> worldAnchors(BuildingSpec s, Placed p) {
        Map<String, List<Pos>> out = new java.util.LinkedHashMap<>();
        s.anchors().forEach((k, v) -> out.put(k, v.stream().map(rel -> TemplateTransform.toWorld(p.origin(), rel, p.turn())).toList()));
        return out;
    }
}
