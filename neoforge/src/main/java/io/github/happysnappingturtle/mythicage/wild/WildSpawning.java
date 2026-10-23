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
 * 区域密度：随结构生成的生物按 {@link #areaSiteAllows} 只在每个区域挑出的区块里放结构。
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

    /**
     * 地物生成时用：只查村庄登记（钟），不查兴趣点——地物在生成线程上跑，查兴趣点会同步加载区块、可能卡死。
     * 没有生物卡时按敌对默认 64 格。
     */
    public static boolean villageAllowsDuringGen(ServerLevel level, BlockPos pos, @javax.annotation.Nullable WildCreatureCard card) {
        int avoid = card != null ? card.avoidVillages() : 64;
        return WildSpawnRules.villageAllows(nearestVillageSq(level, pos, 0), avoid);
    }

    /**
     * 小结构（营地、鸟巢）的区域密度：只有这一区域按世界种子挑出的区块才放（{@link WildSpawnRules#isAreaSite}）。
     * 生物卡没读到时按 fallbackArea、每区域 1 处。
     */
    public static boolean areaSiteAllows(net.minecraft.world.level.WorldGenLevel level, BlockPos origin, String key, int fallbackArea) {
        WildCreatureCard card = WildCreatures.card(key).orElse(null);
        int area = card != null ? card.densityArea() : fallbackArea;
        int max = card != null ? card.densityMax() : 1;
        return WildSpawnRules.isAreaSite(level.getSeed(), key, origin.getX() >> 4, origin.getZ() >> 4, area, max);
    }

    /**
     * 自然刷新的条件（生物卡 spawn = natural）：非和平难度、方块可站（原版 Mob 规则，不要求黑暗）、村庄排除、
     * 区域密度（这一 area × area 区域里已加载的同类不超过 max × 群上限）。命令、刷怪蛋等非自然方式不受限。
     */
    public static <T extends net.minecraft.world.entity.Mob> net.minecraft.world.entity.SpawnPlacements.SpawnPredicate<T> natural(String key) {
        return (type, level, reason, pos, random) -> {
            if (reason != net.minecraft.world.entity.MobSpawnType.NATURAL && reason != net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) {
                return true;
            }
            WildCreatureCard card = WildCreatures.card(key).orElse(null);
            if (card == null || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL
                    || !net.minecraft.world.entity.Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
                return false;
            }
            ServerLevel sl = level.getLevel();
            return villageAllows(sl, pos, card) && WildSpawnRules.densityAllows(countInArea(sl, type, pos, card.densityArea()),
                    card.densityMax() * card.maxGroup());
        };
    }

    /** pos 所在 area × area 区域里已加载的 type 实体数。 */
    public static int countInArea(ServerLevel level, net.minecraft.world.entity.EntityType<?> type, BlockPos pos, int area) {
        int x0 = Math.floorDiv(pos.getX(), area) * area;
        int z0 = Math.floorDiv(pos.getZ(), area) * area;
        var box = new net.minecraft.world.phys.AABB(x0, level.getMinBuildHeight(), z0, x0 + area, level.getMaxBuildHeight(), z0 + area);
        return level.getEntities(type, box, e -> e.isAlive()).size();
    }
}
