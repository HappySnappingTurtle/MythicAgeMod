package com.example.mythic_age.event;

import com.example.mythic_age.worldgen.MythicVillageProcessor;
import com.example.mythic_age.worldgen.MythicVillageTypes;
import com.example.mythic_age.worldgen.VillageManager;
import com.example.mythic_age.worldgen.BiomeCivilizationMapper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 村庄生成事件处理器
 * 改进版本：基于生物群系的智能文明类型分配系统
 * 
 * 实现逻辑：
 * 1. 根据生物群系判断村庄可以生成的文明类型
 * 2. 通过随机数选择一个文明类型分配给村庄
 * 3. 将村庄内所有村民设置为对应的文明类型
 * 
 * 负责监听村庄生成事件，并在合适的时机触发神话村庄的生成。
 * 这个类通过Mixin系统与原版的村庄生成逻辑集成。
 */
public class VillageGenerationEventHandler {
    
    // 存储村庄位置和对应的文明类型（保持向后兼容）
    private static final Map<BlockPos, MythicVillageTypes.VillageType> villageTypeCache = new ConcurrentHashMap<>();
    
    /**
     * 注册事件监听器
     */
    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            System.out.println("[神话村庄] 世界加载完成: " + world.getRegistryKey().getValue());
            // 打印生物群系文明映射调试信息
            BiomeCivilizationMapper.printDebugInfo();
        });
    }
    
    /**
     * 处理结构生成完成事件
     * 改进版本：使用新的村庄管理器和生物群系映射系统
     * 
     * @param world 世界访问器
     * @param pos 结构位置
     * @param structureName 结构名称
     */
    public static void onStructureGenerated(StructureWorldAccess world, BlockPos pos, String structureName) {
        try {
            // 检查是否是村庄结构
            if (isVillageStructure(structureName)) {
                System.out.println("[神话村庄] 检测到村庄结构生成: " + structureName + " 位置: " + pos);
                
                if (world instanceof ServerWorld serverWorld) {
                    // 使用新的村庄管理器处理村庄生成
                    VillageManager.onVillageGenerated(serverWorld, pos);
                    
                    // 获取分配的文明类型
                    MythicVillageTypes.VillageType villageType = VillageManager.getVillageCivilization(pos);
                    
                    if (villageType != null) {
                        System.out.println("[神话村庄] 确定村庄类型: " + villageType.getName());
                        
                        // 缓存村庄类型信息（保持向后兼容）
                        villageTypeCache.put(pos, villageType);
                        
                        // 处理神话村庄生成
                        MythicVillageProcessor.processVillageGeneration(serverWorld, pos, villageType);
                    } else {
                        System.out.println("[神话村庄] 生成普通村庄");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[神话村庄] 处理村庄生成时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取村庄的文明类型
     * 
     * @param pos 村庄位置
     * @return 村庄文明类型，如果是普通村庄则返回null
     */
    public static MythicVillageTypes.VillageType getVillageType(BlockPos pos) {
        // 优先从新的村庄管理器获取
        MythicVillageTypes.VillageType type = VillageManager.getVillageCivilization(pos);
        if (type != null) {
            return type;
        }
        
        // 向后兼容：从旧缓存获取
        return villageTypeCache.get(pos);
    }
    
    /**
     * 检查是否是村庄结构
     * 
     * @param structureName 结构名称
     * @return 是否是村庄结构
     */
    private static boolean isVillageStructure(String structureName) {
        return structureName.contains("village") || 
               structureName.contains("town") || 
               structureName.contains("mythic_age");
    }
}