package com.example.mythic_age.worldgen;

import com.example.mythic_age.entity.CivilizedVillager;
import com.example.mythic_age.item.GodPowerCertificateItem;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 村庄管理器
 * 
 * 负责管理村庄的文明类型分配和村民文明类型设置。
 * 实现逻辑：
 * 1. 根据生物群系判断村庄可以生成的文明类型
 * 2. 通过随机数选择一个文明类型分配给村庄
 * 3. 将村庄内所有村民设置为对应的文明类型
 */
public class VillageManager {
    
    // 村庄位置到文明类型的映射
    private static final Map<BlockPos, MythicVillageTypes.VillageType> villageTypeMap = new ConcurrentHashMap<>();
    
    // 村庄搜索半径
    private static final int VILLAGE_SEARCH_RADIUS = 128;
    
    // 村民搜索半径
    private static final int VILLAGER_SEARCH_RADIUS = 64;
    
    /**
     * 为村庄分配文明类型
     * 
     * @param world 服务器世界
     * @param villageCenter 村庄中心位置
     * @return 分配的文明类型，如果该位置不适合生成神话村庄则返回null
     */
    public static MythicVillageTypes.VillageType assignVillageCivilization(ServerWorld world, BlockPos villageCenter) {
        try {
            // 获取村庄中心的生物群系
            var biome = world.getBiome(villageCenter);
            
            // 根据生物群系获取可能的文明类型
            List<MythicVillageTypes.VillageType> possibleCivilizations = 
                BiomeCivilizationMapper.getPossibleCivilizations(biome);
            
            if (possibleCivilizations.isEmpty()) {
                System.out.println("[村庄管理器] 位置 " + villageCenter + " 的生物群系不支持神话文明");
                return null;
            }
            
            // 使用村庄坐标作为随机种子，确保同一位置总是生成相同的文明类型
            long seed = villageCenter.getX() * 31L + villageCenter.getZ() * 17L;
            Random random = new Random(seed);
            
            // 10%概率生成神话村庄
            if (random.nextDouble() >= 0.1) {
                System.out.println("[村庄管理器] 位置 " + villageCenter + " 随机决定生成普通村庄");
                return null;
            }
            
            // 从可能的文明类型中随机选择一个
            MythicVillageTypes.VillageType selectedCivilization = 
                possibleCivilizations.get(random.nextInt(possibleCivilizations.size()));
            
            // 缓存村庄文明类型
            villageTypeMap.put(villageCenter, selectedCivilization);
            
            System.out.println("[村庄管理器] 为位置 " + villageCenter + " 分配文明类型: " + 
                selectedCivilization.getName() + " (生物群系: " + biome.getKey().map(key -> key.getValue().toString()).orElse("unknown") + ")");
            
            return selectedCivilization;
            
        } catch (Exception e) {
            System.err.println("[村庄管理器] 分配村庄文明类型时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 为村庄内的所有村民设置文明类型
     * 
     * @param world 服务器世界
     * @param villageCenter 村庄中心位置
     * @param civilizationType 文明类型
     */
    public static void setVillagersCivilization(ServerWorld world, BlockPos villageCenter, MythicVillageTypes.VillageType civilizationType) {
        try {
            // 搜索村庄范围内的所有村民
            Box searchBox = new Box(villageCenter).expand(VILLAGER_SEARCH_RADIUS);
            List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, searchBox, entity -> true);
            
            int civilizedCount = 0;
            for (VillagerEntity villager : villagers) {
                if (villager instanceof CivilizedVillager civilizedVillager) {
                    // 设置村民的文明类型
                    civilizedVillager.mythicAge$setCivilization(civilizationType.getMythologyType());
                    civilizedCount++;
                    
                    System.out.println("[村庄管理器] 为村民设置文明类型: " + civilizationType.getName() + 
                        " (位置: " + villager.getBlockPos() + ")");
                }
            }
            
            System.out.println("[村庄管理器] 在村庄 " + villageCenter + " 为 " + civilizedCount + 
                " 个村民设置了 " + civilizationType.getName() + " 文明类型");
            
        } catch (Exception e) {
            System.err.println("[村庄管理器] 设置村民文明类型时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取村庄的文明类型
     * 
     * @param villageCenter 村庄中心位置
     * @return 村庄的文明类型，如果未找到则返回null
     */
    public static MythicVillageTypes.VillageType getVillageCivilization(BlockPos villageCenter) {
        return villageTypeMap.get(villageCenter);
    }
    
    /**
     * 查找最近的村庄文明类型
     * 
     * @param pos 搜索起始位置
     * @return 最近的村庄文明类型，如果未找到则返回null
     */
    public static MythicVillageTypes.VillageType findNearestVillageCivilization(BlockPos pos) {
        MythicVillageTypes.VillageType nearestType = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Map.Entry<BlockPos, MythicVillageTypes.VillageType> entry : villageTypeMap.entrySet()) {
            double distance = pos.getSquaredDistance(entry.getKey());
            if (distance < nearestDistance && distance <= VILLAGE_SEARCH_RADIUS * VILLAGE_SEARCH_RADIUS) {
                nearestDistance = distance;
                nearestType = entry.getValue();
            }
        }
        
        return nearestType;
    }
    
    /**
     * 处理村庄生成完成事件
     * 
     * @param world 服务器世界
     * @param villageCenter 村庄中心位置
     */
    public static void onVillageGenerated(ServerWorld world, BlockPos villageCenter) {
        try {
            // 为村庄分配文明类型
            MythicVillageTypes.VillageType civilizationType = assignVillageCivilization(world, villageCenter);
            
            if (civilizationType != null) {
                // 延迟设置村民文明类型，确保村民已经生成
                world.getServer().execute(() -> {
                    try {
                        Thread.sleep(1000); // 等待1秒确保村民生成完成
                        setVillagersCivilization(world, villageCenter, civilizationType);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
        } catch (Exception e) {
            System.err.println("[村庄管理器] 处理村庄生成事件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 为单个村民设置文明类型（基于位置查找最近的村庄）
     * 
     * @param villager 村民实体
     */
    public static void setVillagerCivilizationByLocation(VillagerEntity villager) {
        if (!(villager instanceof CivilizedVillager civilizedVillager)) {
            return;
        }
        
        // 如果已经设置过文明类型，则不重复设置
        if (civilizedVillager.mythicAge$getCivilization() != null) {
            return;
        }
        
        try {
            BlockPos villagerPos = villager.getBlockPos();
            MythicVillageTypes.VillageType nearestCivilization = findNearestVillageCivilization(villagerPos);
            
            if (nearestCivilization != null) {
                civilizedVillager.mythicAge$setCivilization(nearestCivilization.getMythologyType());
                System.out.println("[村庄管理器] 为村民设置文明类型: " + nearestCivilization.getName() + 
                    " (位置: " + villagerPos + ")");
            } else {
                // 如果没有找到附近的神话村庄，设置为默认的希腊文明
                civilizedVillager.mythicAge$setCivilization(GodPowerCertificateItem.MythologyType.GREEK);
                System.out.println("[村庄管理器] 村民位置 " + villagerPos + " 附近没有神话村庄，设置为默认希腊文明");
            }
            
        } catch (Exception e) {
            System.err.println("[村庄管理器] 为村民设置文明类型时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清理缓存（用于调试）
     */
    public static void clearCache() {
        villageTypeMap.clear();
        System.out.println("[村庄管理器] 已清理村庄文明类型缓存");
    }
    
    /**
     * 获取缓存的村庄数量（用于调试）
     */
    public static int getCachedVillageCount() {
        return villageTypeMap.size();
    }
    
    /**
     * 打印调试信息
     */
    public static void printDebugInfo() {
        System.out.println("[村庄管理器] 当前缓存的村庄:");
        for (Map.Entry<BlockPos, MythicVillageTypes.VillageType> entry : villageTypeMap.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue().getName());
        }
    }
}