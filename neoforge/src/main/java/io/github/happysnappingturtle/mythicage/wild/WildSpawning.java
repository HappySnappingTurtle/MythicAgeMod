package io.github.happysnappingturtle.mythicage.wild;

import io.github.happysnappingturtle.mythicage.core.wild.WildCreatureCard;
import io.github.happysnappingturtle.mythicage.core.wild.WildSpawnRules;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

/**
 * 野外神话生物的刷新条件（T15 设计文档 §1.1）：村庄排除——已登记的村庄（钟的位置）与原版村庄的聚集点（钟）都算。
 * 区域密度由各生成入口按 {@link WildSpawnRules#areaKey} 计数。
 */
public final class WildSpawning {
    private WildSpawning() {
    }

    /** pos 离最近村庄中心（登记的村庄钟、原版钟）的距离平方；半径 radius 内没有时为正无穷。 */
    public static double nearestVillageSq(ServerLevel level, BlockPos pos, int radius) {
        double best = Double.POSITIVE_INFINITY;
        for (VillageRegistry.Village v : VillageRegistry.get(level.getServer()).in(level.dimension())) {
            best = Math.min(best, v.bell().pos().distSqr(pos));
        }
        if (radius > 0) {
            PoiManager poi = level.getPoiManager();
            best = Math.min(best, poi.findClosest(t -> t.is(PoiTypes.MEETING), pos, radius, PoiManager.Occupancy.ANY)
                    .map(p -> p.distSqr(pos)).orElse(Double.POSITIVE_INFINITY));
        }
        return best;
    }

    /** 按生物卡判断 pos 能不能生成（村庄排除）。 */
    public static boolean villageAllows(ServerLevel level, BlockPos pos, WildCreatureCard card) {
        return WildSpawnRules.villageAllows(nearestVillageSq(level, pos, card.avoidVillages()), card.avoidVillages());
    }
}
