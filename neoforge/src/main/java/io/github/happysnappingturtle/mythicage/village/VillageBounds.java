package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Outline;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 「显示村界」的数据（T24 设计文档 §4.4）：已建村界（决定村内动物伤害是否计入善恶）、未建的规划地块、被占的地块。
 * 三类在客户端用不同线型画，不只靠颜色。
 */
public final class VillageBounds {
    private VillageBounds() {
    }

    public static Optional<AffairsPayloads.Bounds> build(MinecraftServer server, UUID village) {
        Optional<VillageRegistry.Village> vo = VillageRegistry.get(server).byId(village);
        if (vo.isEmpty()) {
            return Optional.empty();
        }
        VillageRegistry.Village v = vo.get();
        ServerLevel level = server.getLevel(v.bell().dimension());
        if (level == null) {
            return Optional.empty();
        }
        List<Outline.Edge> built = Outline.union(VillageZones.builtBoxes(level, v));
        List<int[]> planned = new ArrayList<>();
        List<int[]> occupied = new ArrayList<>();
        Optional<VillagePlans.Plan> plan = VillagePlans.get(server).byVillage(village);
        if (plan.isPresent()) {
            VillageEconomy.State s = VillageEconomy.get(server).state(village);
            String blockedPlot = s.activeProject().filter(p -> p.state == BuildProject.State.BLOCKED).map(p -> p.plot).orElse("");
            for (VillagePlan.Plot plot : plan.get().info().plots()) {
                Box b = plot.box();
                int[] r = {b.minX(), b.minZ(), b.maxX(), b.maxZ()};
                if (plot.id().equals(blockedPlot)) {
                    occupied.add(r);
                } else if (!s.builtPlots.contains(plot.id())) {
                    planned.add(r);
                }
            }
        }
        return Optional.of(new AffairsPayloads.Bounds(built, List.copyOf(planned), List.copyOf(occupied)));
    }
}
