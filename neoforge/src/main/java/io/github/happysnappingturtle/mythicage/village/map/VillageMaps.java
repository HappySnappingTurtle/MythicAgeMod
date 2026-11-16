package io.github.happysnappingturtle.mythicage.village.map;

import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.core.village.map.VillageMapBuilder;
import io.github.happysnappingturtle.mythicage.core.village.map.VillageMapData;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageZones;
import io.github.happysnappingturtle.mythicage.village.build.BuildSite;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import io.github.happysnappingturtle.mythicage.village.gen.VillageRegistration;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 村庄地图的服务端生成（GA-12 §4）：按村庄规划、施工记录与进行中的项目整理成 {@link VillageMapBuilder.Input}，
 * 再交给 core 拼图。<b>不读世界方块</b>。建筑的身份：施工建成的看 {@code constructed}，生成时就有的按锚点认
 * （同 GA-11 找可升级的旧建筑）；认不出的照「建筑」画。
 */
public final class VillageMaps {
    private VillageMaps() {
    }

    public static Optional<VillageMapData> build(ServerLevel level, UUID village) {
        Optional<VillageContext> ctx = VillageScheduler.contextOf(level, village);
        return ctx.map(VillageMaps::build);
    }

    public static VillageMapData build(VillageContext ctx) {
        ServerLevel level = ctx.level();
        VillageRegistry.Village v = ctx.village();
        VillagePlans.Plan plan = ctx.plan();
        VillageEconomy.State s = ctx.state();
        Map<String, BuildingSpec> specs = VillageBuildings.all();
        List<Box> raw = plan.info().built();
        Set<Box> known = new HashSet<>();
        List<VillageMapBuilder.Placed> placed = new ArrayList<>();
        for (VillageEconomy.Built b : s.constructed) {
            if (known.add(b.box())) {
                placed.add(new VillageMapBuilder.Placed(b.building(), b.god(), tier(b.building(), specs), b.box()));
            }
        }
        for (BuildingSpec spec : specs.values()) {
            if (!spec.generated() || !spec.civ().equals(v.civ())) {
                continue;
            }
            for (BuildPlacement.Placed at : BuildPlacement.locateAll(spec, plan.info().anchors(), raw)) {
                if (known.add(at.box())) {
                    placed.add(new VillageMapBuilder.Placed(spec.id(), "", tier(spec.id(), specs), at.box()));
                }
            }
        }
        List<Box> unknown = raw.stream().filter(b -> !known.contains(b)).toList();
        List<VillageMapBuilder.Project> projects = new ArrayList<>();
        Set<String> busyPlots = new HashSet<>();
        for (BuildProject p : s.projects) {
            if (!p.active()) {
                continue;
            }
            BuildingSpec spec = specs.get(p.building);
            if (spec == null) {
                continue;
            }
            busyPlots.add(p.plot);
            Box box = Construction.placed(spec, p.origin, p.turn).box();
            Optional<Box> replaces = Optional.empty();
            if (!p.replaces.isEmpty() && p.oldOrigin != null) {
                BuildingSpec old = specs.get(p.replaces);
                if (old != null) {
                    replaces = Optional.of(Construction.placed(old, p.oldOrigin, p.oldTurn).box());
                }
            }
            projects.add(new VillageMapBuilder.Project(p.building, p.god, tier(p.building, specs), box, progress(ctx, p), replaces));
        }
        List<VillagePlan.Plot> free = plan.info().plots().stream()
                .filter(pl -> !s.builtPlots.contains(pl.id()) && !busyPlots.contains(pl.id())).toList();
        Optional<Pos> temple = VillageTemples.of(level.getServer(), v)
                .map(t -> VillageContext.pos(t.altar().pos()));
        VillageMapData data = VillageMapBuilder.build(new VillageMapBuilder.Input(plan.info().center(),
                VillageZones.builtBoxes(level, v), roads(level, plan), placed, unknown, projects, free, temple));
        return data.fit(VillageMapData.MAX_BYTES);
    }

    /** 施工进度百分比：已放格数 / 全部格数；备料中为 0。 */
    public static int progress(VillageContext ctx, BuildProject p) {
        Optional<BuildSite> site = Construction.site(ctx, p);
        if (site.isEmpty()) {
            return 0;
        }
        int total = site.get().segments.stream().mapToInt(seg -> seg.cells().size()).sum();
        return total <= 0 ? 0 : (int) Math.min(100, BuildProject.count(p.placedCells) * 100L / total);
    }

    /**
     * 建筑等级：名字以 {@code _ii}、{@code _iii} 结尾的为 2、3 级；卫城按时代（远古 1、古典 2、英雄 3）；
     * 有别的建筑能原地替换它的为 1 级；其余没有等级链，为 0。
     */
    public static int tier(String building, Map<String, BuildingSpec> specs) {
        BuildingSpec spec = specs.get(building);
        String base = spec == null ? building : spec.civ() + ":" + spec.shortBase();
        if (base.endsWith("_iii")) {
            return 3;
        }
        if (base.endsWith("_ii")) {
            return 2;
        }
        if (base.contains(":acropolis_")) {
            // 主神变体（acropolis_archaic_zeus）的规格没写 patron，shortBase 去不掉后缀：按前缀认
            String age = base.substring(base.indexOf(":acropolis_") + ":acropolis_".length());
            return age.startsWith("archaic") ? 1 : age.startsWith("classical") ? 2 : age.startsWith("heroic") ? 3 : 0;
        }
        for (BuildingSpec o : specs.values()) {
            if (o.replace().stream().anyMatch(building::startsWith)) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * 道路：登记时存下的；老村子没存过的，在结构起点区块已加载时从道路片段补读一次。奠基村庄没有结构，记成没有路。
     */
    static List<VillagePlan.Path> roads(ServerLevel level, VillagePlans.Plan plan) {
        VillagePlans plans = VillagePlans.get(level.getServer());
        Optional<List<VillagePlan.Path>> known = plans.roads(plan.village());
        if (known.isPresent()) {
            return known.get();
        }
        if (plan.structure().isEmpty()) {
            plans.setRoads(plan.village(), List.of());
            return List.of();
        }
        ChunkPos cp = new ChunkPos(plan.start());
        LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
        if (chunk == null) {
            return List.of();
        }
        Pos center = plan.info().center();
        for (StructureStart start : chunk.getAllStarts().values()) {
            if (!start.isValid()) {
                continue;
            }
            boolean ours = start.getPieces().stream().anyMatch(pc -> pc instanceof VillageInfoPiece info
                    && info.info().center().equals(center));
            if (ours) {
                List<VillagePlan.Path> paths = VillageRegistration.roads(start);
                plans.setRoads(plan.village(), paths);
                return paths;
            }
        }
        plans.setRoads(plan.village(), List.of());
        return List.of();
    }
}
