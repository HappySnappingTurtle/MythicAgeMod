package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.recipe.VillageRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 工作站（T19 设计文档 §6.1，V1 §5.2）。建筑锚点通常是站位，工作站方块在它旁边：按文明数据的 {@code stations}
 * 在锚点周围找配方用到的工作站方块。另有两种不占方块的「虚拟工作站」写在配方的 station 里：
 * <ul>
 *   <li>{@code anchor:<锚点名>}：在这类锚点上做（如谷仓管事位锯木板）；</li>
 *   <li>{@code point:<采集点种类>}：在采集点上做（如采石点粗打多边形砌石）。</li>
 * </ul>
 * 一个工作站方块同时只服务一名居民（目标键按方块坐标）。
 *
 * @param stand 居民站的位置
 * @param block 工作站方块的位置（虚拟工作站与站位相同）
 * @param id    工作站 id（方块 id，或 anchor: / point: 开头的虚拟 id）
 */
public final class Workstations {
    public record Station(Pos stand, BlockPos block, String id) {
        public String key() {
            return "station:" + block.asLong() + ":" + id;
        }
    }

    public static final String ANCHOR = "anchor:";
    public static final String POINT = "point:";

    private Workstations() {
    }

    /** 配方用到的全部方块工作站 id。 */
    static Set<String> blockStations(CivVillageData data) {
        Set<String> out = new HashSet<>();
        for (VillageRecipe r : data.recipes().values()) {
            if (!r.station().startsWith(ANCHOR) && !r.station().startsWith(POINT)) {
                out.add(r.station());
            }
        }
        return out;
    }

    /** 扫一遍（只看已加载的格子）。 */
    public static List<Station> scan(VillageContext ctx) {
        List<Station> out = new ArrayList<>();
        Set<Long> taken = new LinkedHashSet<>();
        Set<String> known = blockStations(ctx.data());
        for (CivVillageData.StationSearch s : ctx.data().stations()) {
            Set<String> want = s.blocks().isEmpty() ? known : s.blocks();
            for (Pos a : ctx.anchors(s.anchor())) {
                BlockPos ap = VillageContext.block(a);
                if (!ctx.loaded(ap)) {
                    continue;
                }
                BlockPos found = null;
                String id = null;
                int bestD = Integer.MAX_VALUE;
                // 在半径内找离锚点最近（曼哈顿距离）的工作站；同距离取 y 低的（高炉、陶窑取下半格）
                for (BlockPos p : BlockPos.betweenClosed(ap.offset(-s.radius(), -1, -s.radius()), ap.offset(s.radius(), 1, s.radius()))) {
                    if (taken.contains(p.asLong()) || !ctx.loaded(p)) {
                        continue;
                    }
                    String bid = id(ctx.level().getBlockState(p));
                    if (!want.contains(bid)) {
                        continue;
                    }
                    int d = Math.abs(p.getX() - ap.getX()) + Math.abs(p.getY() - ap.getY()) * 2 + Math.abs(p.getZ() - ap.getZ());
                    if (d < bestD || (d == bestD && found != null && p.getY() < found.getY())) {
                        bestD = d;
                        found = p.immutable();
                        id = bid;
                    }
                }
                if (found != null) {
                    taken.add(found.asLong());
                    out.add(new Station(a, found.immutable(), id));
                }
            }
        }
        // 虚拟工作站：配方里写了 anchor:<名> 的，每个这类锚点一个
        Set<String> anchorKinds = new LinkedHashSet<>();
        for (VillageRecipe r : ctx.data().recipes().values()) {
            if (r.station().startsWith(ANCHOR)) {
                anchorKinds.add(r.station().substring(ANCHOR.length()));
            }
        }
        for (String kind : anchorKinds) {
            for (Pos a : ctx.anchors(kind)) {
                out.add(new Station(a, VillageContext.block(a), ANCHOR + kind));
            }
        }
        return out;
    }

    static String id(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
    }

    /** 工作站方块还在不在（虚拟工作站总是在）。 */
    static boolean present(VillageContext ctx, BlockPos block, String id) {
        if (id.startsWith(ANCHOR) || id.startsWith(POINT)) {
            return true;
        }
        return ctx.loaded(block) && id(ctx.level().getBlockState(block)).equals(id);
    }
}
