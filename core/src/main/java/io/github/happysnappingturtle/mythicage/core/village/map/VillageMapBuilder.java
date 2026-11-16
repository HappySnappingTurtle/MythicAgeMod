package io.github.happysnappingturtle.mythicage.core.village.map;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Outline;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 由村庄规划拼出地图（GA-12 §4）。平台层把规划、施工记录、项目整理成 {@link Input}，这里只做纯计算：
 * 村界并集描边、道路、建筑（升级中的标上进度与目标，新建项目画成在建）、空地块、神庙位置。
 */
public final class VillageMapBuilder {
    private VillageMapBuilder() {
    }

    /** 一座已建成、认得出的建筑。 */
    public record Placed(String building, String god, int tier, Box box) {
    }

    /**
     * 一个进行中的施工项目。
     *
     * @param replaces 升级：被替换的旧建筑包围盒；新建为空
     * @param progress 已放格数 / 总格数的百分比（0–100）
     */
    public record Project(String building, String god, int tier, Box box, int progress, Optional<Box> replaces) {
    }

    /**
     * @param zone      村界：已建范围的外扩包围盒（与「显示村界」同一来源）
     * @param unknown   规划里有、但认不出是哪种建筑的已建包围盒
     * @param freePlots 空地块（未建、没有项目）
     * @param temple    神庙祭坛
     */
    public record Input(Pos center, List<Box> zone, List<VillagePlan.Path> paths, List<Placed> buildings, List<Box> unknown,
                        List<Project> projects, List<VillagePlan.Plot> freePlots, Optional<Pos> temple) {
    }

    public static VillageMapData build(Input in) {
        List<VillageMapData.Segment> border = VillageMapData.segments(Outline.union(in.zone()));
        List<VillageMapData.Road> roads = new ArrayList<>();
        for (VillagePlan.Path p : in.paths()) {
            if (p.points().size() >= 2) {
                roads.add(new VillageMapData.Road(p.width(), p.points().stream()
                        .map(q -> new VillageMapData.Point(q.x(), q.z())).toList()));
            }
        }
        List<VillageMapData.Building> buildings = new ArrayList<>();
        Set<Box> upgraded = new HashSet<>();
        for (Placed b : in.buildings()) {
            Optional<Project> up = in.projects().stream().filter(p -> p.replaces().filter(b.box()::equals).isPresent()).findFirst();
            if (up.isPresent()) {
                Project p = up.get();
                upgraded.add(b.box());
                buildings.add(new VillageMapData.Building(b.building(), b.god(), b.tier(),
                        VillageMapData.Rect.of(b.box()).union(VillageMapData.Rect.of(p.box())), clamp(p.progress()), p.building()));
            } else {
                buildings.add(new VillageMapData.Building(b.building(), b.god(), b.tier(), VillageMapData.Rect.of(b.box()), -1, ""));
            }
        }
        for (Box u : in.unknown()) {
            buildings.add(new VillageMapData.Building("", "", 0, VillageMapData.Rect.of(u), -1, ""));
        }
        for (Project p : in.projects()) {
            // 升级的旧建筑认不出（不在 buildings 里）时，照新建画
            if (p.replaces().isPresent() && upgraded.contains(p.replaces().get())) {
                continue;
            }
            buildings.add(new VillageMapData.Building(p.building(), p.god(), p.tier(), VillageMapData.Rect.of(p.box()),
                    clamp(p.progress()), ""));
        }
        List<VillageMapData.Plot> plots = in.freePlots().stream()
                .map(p -> new VillageMapData.Plot(p.id(), p.kind(), VillageMapData.Rect.of(p.box()))).toList();
        return new VillageMapData(in.center().x(), in.center().z(), border, roads, buildings, plots,
                in.temple().map(t -> new VillageMapData.Point(t.x(), t.z())));
    }

    private static int clamp(int p) {
        return Math.max(0, Math.min(100, p));
    }
}
