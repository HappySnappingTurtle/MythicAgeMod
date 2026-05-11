package com.example.mythic_age;

import com.example.mythic_age.block.ModBlocks;
import com.example.mythic_age.block.entity.ModBlockEntities;
import com.example.mythic_age.entity.ModEntities;
import com.example.mythic_age.entity.ModEntityTypes;
import com.example.mythic_age.event.ModCombatEvents;
import com.example.mythic_age.event.ModLootTableModifiers;
import com.example.mythic_age.event.VillageGenerationEventHandler;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.item.ModItemGroups;
import com.example.mythic_age.network.ModNetworking;
import com.example.mythic_age.sound.ModSounds;
import com.example.mythic_age.system.GodPowerCertificateManager;
import com.example.mythic_age.util.EarthquakeManager;
import com.example.mythic_age.util.LightningStormManager;
import com.example.mythic_age.worldgen.MythicVillageTypes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 神话时代 Mod 主类
 * 
 * 这是神话时代Mod的核心初始化类，负责整个Mod的服务器端初始化工作。
 * 该Mod基于微软《神话时代》游戏，为Minecraft带来古代神话的神力法术和圣物系统。
 * 
 * 主要功能：
 * - 注册所有的物品（法术书、圣物等）
 * - 注册所有的方块（圣物基座等）
 * - 注册所有的实体（米诺陶等神话生物）
 * - 注册音效系统
 * - 注册方块实体（圣物基座实体等）
 * - 注册物品组（创造模式标签页）
 * - 修改战利品表（在各种结构中添加神话物品）
 * 
 * 包含的系统：
 * 1. 神力法术书系统 - 4种不同文明的神力法术
 * 2. 圣物系统 - 5种传说中的神话圣物，具有三层效果系统
 * 3. 神殿多方块结构 - 可建造的神殿，激活圣物效果
 * 4. 神话生物系统 - 米诺陶等传说生物
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class MythicAgeMod implements ModInitializer {
    /** Mod的唯一标识符，用于所有资源的命名空间 */
    public static final String MOD_ID = "mythic_age";
    
    /** 日志记录器，用于输出Mod的运行信息和调试信息 */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Mod初始化方法
     * 
     * 在服务器端和客户端都会调用此方法进行Mod的初始化。
     * 按照特定顺序注册所有的游戏内容，确保依赖关系正确。
     * 
     * 初始化顺序：
     * 1. 物品注册 - 法术书和圣物
     * 2. 方块注册 - 圣物基座等功能方块
     * 3. 方块实体注册 - 带有逻辑的方块实体
     * 4. 实体注册 - 神话生物
     * 5. 音效注册 - 法术和效果音效
     * 6. 物品组注册 - 创造模式标签页
     * 7. 战利品表修改 - 在世界结构中添加神话物品
     */
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Mythic Age Mod");

        // 注册物品：法术书、圣物等核心物品
        ModItems.registerModItems();
        
        // 注册方块：圣物基座等功能性方块
        ModBlocks.registerModBlocks();
        
        // 注册方块实体：具有特殊逻辑的方块实体
        ModBlockEntities.registerBlockEntities();
        
        // 注册实体：米诺陶等神话生物
        ModEntities.registerModEntities();
        
        // 注册实体类型：陨石等特殊实体
        ModEntityTypes.registerEntityTypes();
        
        // 注册音效：法术效果、环境音效等
        ModSounds.registerModSounds();
        
        // 注册物品组：创造模式标签页分类
        ModItemGroups.registerItemGroups();

        // 修改战利品表：在各种结构中添加神话物品的掉落
        ModLootTableModifiers.modifyLootTables();

        // 注册战斗事件：北欧神力证明由击杀怪物或其他玩家产出
        ModCombatEvents.register();
        
        // 初始化神话村庄系统
        MythicVillageTypes.initialize();
        
        // 注册村庄生成事件处理器
        VillageGenerationEventHandler.register();
        
        // 注册网络数据包
        ModNetworking.registerNetworkPackets();
        
        // 注册服务器tick事件：用于更新法术效果管理器
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            if (!world.isClient()) {
                EarthquakeManager.getInstance().tick(world);
                LightningStormManager.getInstance().tick(world);
                GodPowerCertificateManager.getInstance().tickEgyptianStatues(world);
            }
        });
        
        LOGGER.info("Mythic Age Mod initialization completed successfully");
    }
}
