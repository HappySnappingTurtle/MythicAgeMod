package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.core.village.gen.BuiltArea;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 村庄的三种空间范围（细化规划 11 号 §9、T18 设计文档 §2），互不混用：
 * <ul>
 *   <li>BUILT 已建范围：生成村庄 = 规划里的建筑盒并集外扩 4 格；原版村庄 = 钟、床、工作站兴趣点各外扩 4 格求并集（D1），
 *       限制在钟 {@link #VANILLA_REACH} 格内，每个 MC 日重算一次</li>
 *   <li>PLANNED 规划地块：生成村庄还没建成的预留地块，不算村庄</li>
 *   <li>RESOURCE 资源区：村民外出采集的登记点（T19 起登记），不算村庄</li>
 *   <li>OUTSIDE</li>
 * </ul>
 * 原版村庄的兴趣点还没算过、钟所在区块也没加载时，结果是 {@code known = false}：善恶判定一律不扣（D8，02 号 §3.4）。
 */
public final class VillageZones {
    public static final int VANILLA_REACH = 64;

    public enum Zone { BUILT, PLANNED, RESOURCE, OUTSIDE }

    public record Hit(Zone zone, Optional<VillageRegistry.Village> village, boolean known) {
        static final Hit OUTSIDE = new Hit(Zone.OUTSIDE, Optional.empty(), true);

        public boolean builtKnown() {
            return known && zone == Zone.BUILT;
        }
    }

    private record Cached(long day, BuiltArea area) {
    }

    /** 原版村庄 id → 兴趣点推出的已建范围。 */
    private static final Map<UUID, Cached> VANILLA = new ConcurrentHashMap<>();
    /** 村庄 id → 资源区盒子。 */
    private static final Map<UUID, List<Box>> RESOURCES = new ConcurrentHashMap<>();
    /** 只供 GameTest：当作边界还没算出来的村庄。 */
    private static final java.util.Set<UUID> UNKNOWN_FOR_TEST = ConcurrentHashMap.newKeySet();

    private VillageZones() {
    }

    public static Hit zoneAt(ServerLevel level, BlockPos pos) {
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        VillagePlans plans = VillagePlans.get(level.getServer());
        int x = pos.getX();
        int z = pos.getZ();
        // 已建：生成村庄
        Optional<UUID> planned = plans.at(level.dimension(), x, z);
        if (planned.isPresent()) {
            Optional<VillagePlans.Plan> plan = plans.byVillage(planned.get());
            if (plan.isPresent() && plan.get().area().contains(x, z)) {
                return new Hit(Zone.BUILT, reg.byId(planned.get()), true);
            }
        }
        // 已建：原版村庄（兴趣点边界）
        boolean unknown = false;
        VillageRegistry.Village unknownVillage = null;
        for (VillageRegistry.Village v : reg.in(level.dimension())) {
            if (v.anchor().kind() == VillageAnchor.Kind.CENTER) {
                continue;
            }
            BlockPos b = v.bell().pos();
            int reach = VANILLA_REACH + BuiltArea.MARGIN;
            if (Math.abs(b.getX() - x) > reach || Math.abs(b.getZ() - z) > reach) {
                continue;
            }
            BuiltArea area = vanillaArea(level, v);
            if (area == null) {
                unknown = true;
                unknownVillage = v;
            } else if (area.contains(x, z)) {
                return new Hit(Zone.BUILT, Optional.of(v), true);
            }
        }
        // 规划地块
        for (VillagePlans.Plan p : plans.all()) {
            if (!p.dim().equals(level.dimension())) {
                continue;
            }
            for (VillagePlan.Plot plot : p.info().plots()) {
                if (plot.box().containsXZ(x, z)) {
                    return new Hit(Zone.PLANNED, reg.byId(p.village()), !unknown);
                }
            }
        }
        // 资源区
        for (Map.Entry<UUID, List<Box>> e : RESOURCES.entrySet()) {
            for (Box b : e.getValue()) {
                if (b.containsXZ(x, z)) {
                    return new Hit(Zone.RESOURCE, reg.byId(e.getKey()), !unknown);
                }
            }
        }
        return unknown ? new Hit(Zone.OUTSIDE, Optional.ofNullable(unknownVillage), false) : Hit.OUTSIDE;
    }

    /** 原版村庄的兴趣点边界：每个 MC 日重算一次；钟所在区块没加载、又没算过时为 null。 */
    @javax.annotation.Nullable
    static BuiltArea vanillaArea(ServerLevel level, VillageRegistry.Village v) {
        if (UNKNOWN_FOR_TEST.contains(v.id())) {
            return null;
        }
        long today = MythDays.today(level.getServer());
        Cached c = VANILLA.get(v.id());
        if (c != null && c.day() == today) {
            return c.area();
        }
        BlockPos bell = v.bell().pos();
        if (!level.isLoaded(bell)) {
            return c != null ? c.area() : null;
        }
        BuiltArea area = scan(level, bell);
        VANILLA.put(v.id(), new Cached(today, area));
        return area;
    }

    /** 已建范围的盒子（已含外扩边距）：生成村庄用规划，原版村庄用兴趣点；算不出来时为空表（T24 村界显示）。 */
    public static List<Box> builtBoxes(ServerLevel level, VillageRegistry.Village v) {
        Optional<VillagePlans.Plan> plan = VillagePlans.get(level.getServer()).byVillage(v.id());
        if (plan.isPresent()) {
            return plan.get().area().boxes();
        }
        BuiltArea area = vanillaArea(level, v);
        return area == null ? List.of() : area.boxes();
    }

    /** 立刻重算（测试与管理用）。 */
    public static BuiltArea refresh(ServerLevel level, VillageRegistry.Village v) {
        BuiltArea area = scan(level, v.bell().pos());
        VANILLA.put(v.id(), new Cached(MythDays.today(level.getServer()), area));
        return area;
    }

    private static BuiltArea scan(ServerLevel level, BlockPos bell) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(point(bell));
        level.getPoiManager().findAll(VillageZones::villagePoi, b -> true, bell, VANILLA_REACH, PoiManager.Occupancy.ANY)
                .forEach(p -> boxes.add(point(p)));
        return new BuiltArea(boxes);
    }

    private static boolean villagePoi(Holder<PoiType> h) {
        return h.is(PoiTypes.MEETING) || h.is(PoiTypes.HOME) || h.is(PoiTypeTags.ACQUIRABLE_JOB_SITE);
    }

    private static Box point(BlockPos p) {
        return new Box(p.getX(), p.getY(), p.getZ(), p.getX(), p.getY(), p.getZ());
    }

    /** 登记一块资源区（T19 的采集点用）。 */
    public static void registerResource(UUID village, Box box) {
        RESOURCES.computeIfAbsent(village, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(box);
    }

    public static void clearResources(UUID village) {
        RESOURCES.remove(village);
    }

    /** 只供 GameTest：把某村庄当作边界还没算出来（钟所在区块没加载过）。 */
    public static void unknownForTest(UUID village, boolean unknown) {
        VANILLA.remove(village);
        if (unknown) {
            UNKNOWN_FOR_TEST.add(village);
        } else {
            UNKNOWN_FOR_TEST.remove(village);
        }
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        VANILLA.clear();
        RESOURCES.clear();
    }
}
