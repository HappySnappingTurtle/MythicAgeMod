package com.example.mythic_age.event;

import com.example.mythic_age.item.ModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;

/**
 * 战利品表修改器类
 * 
 * 负责修改Minecraft原版的战利品表，在各种结构的箱子中添加神话时代Mod的物品。
 * 这个系统确保玩家可以通过探索世界来获得法术书和圣物，而不仅仅依赖创造模式。
 * 
 * 核心功能：
 * 1. 战利品表集成：将Mod物品无缝集成到原版的掉落系统中
 * 2. 平衡性设计：不同稀有度的物品有不同的掉落概率
 * 3. 主题一致性：神话物品出现在合适的结构中
 * 4. 渐进式获取：从常见物品到稀有物品的自然进展
 * 
 * 掉落系统设计：
 * 
 * 第一层 - 村庄箱子 (入门级)：
 * - 目标玩家：新手和早期游戏
 * - 物品类型：基础法术书
 * - 掉落概率：15-25% (相对较高)
 * - 包含物品：动物招引、复原术、闪电风暴
 * 
 * 第二层 - 神殿箱子 (中级)：
 * - 目标玩家：中期探索者
 * - 物品类型：稀有法术书 + 常见圣物
 * - 掉落概率：10-15% (中等)
 * - 包含物品：冥府军团、拉神安卡、阿瑞斯头盔、丰饶角
 * 
 * 第三层 - 地牢箱子 (高级)：
 * - 目标玩家：高级冒险者
 * - 物品类型：史诗圣物 + 稀有法术书
 * - 掉落概率：8-12% (较低)
 * - 包含物品：雷神锤碎片、金羊毛
 * 
 * 平衡性考虑：
 * - 权重系统：不同物品有不同的相对掉落权重
 * - 概率递减：越稀有的物品掉落概率越低
 * - 结构匹配：物品出现在主题相符的结构中
 * - 数量控制：每次最多掉落一个神话物品
 * 
 * 技术实现：
 * - 使用Fabric API的LootTableEvents系统
 * - 动态战利品池创建和注册
 * - 条件化掉落：基于随机概率
 * - 权重分配：控制不同物品的相对稀有度
 * 
 * 结构分类：
 * - 村庄住宅：各种生物群系的村庄房屋
 * - 古代神殿：沙漠金字塔、丛林神庙、要塞
 * - 危险地牢：地牢、废弃矿井、埋藏宝藏、沉船
 * 
 * 未来扩展：
 * - 添加更多结构支持
 * - 实现季节性或事件性掉落
 * - 支持玩家进度相关的掉落
 * - 添加成套物品的概念
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class ModLootTableModifiers {
    // Village chest loot tables
    private static final List<Identifier> VILLAGE_PLAINS_HOUSE_CHEST_IDS = Arrays.asList(
            new Identifier("minecraft", "chests/village/village_plains_house"),
            new Identifier("minecraft", "chests/village/village_desert_house"),
            new Identifier("minecraft", "chests/village/village_savanna_house"),
            new Identifier("minecraft", "chests/village/village_snowy_house"),
            new Identifier("minecraft", "chests/village/village_taiga_house")
    );
    
    // Temple and structure chest loot tables
    private static final List<Identifier> TEMPLE_CHEST_IDS = Arrays.asList(
            new Identifier("minecraft", "chests/desert_pyramid"),
            new Identifier("minecraft", "chests/jungle_temple"),
            new Identifier("minecraft", "chests/stronghold_corridor"),
            new Identifier("minecraft", "chests/stronghold_crossing"),
            new Identifier("minecraft", "chests/stronghold_library")
    );
    
    // Dungeon and rare structure loot tables
    private static final List<Identifier> DUNGEON_CHEST_IDS = Arrays.asList(
            new Identifier("minecraft", "chests/simple_dungeon"),
            new Identifier("minecraft", "chests/abandoned_mineshaft"),
            new Identifier("minecraft", "chests/buried_treasure"),
            new Identifier("minecraft", "chests/shipwreck_treasure")
    );

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            
            // Add spellbooks to village chests (common spells)
            if (source.isBuiltin() && VILLAGE_PLAINS_HOUSE_CHEST_IDS.contains(id)) {
                // Lightning Storm - 15% chance
                LootPool.Builder lightningPool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.15f))
                        .with(ItemEntry.builder(ModItems.SPELLBOOK_LIGHTNING_STORM))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0f)).build());
                tableBuilder.pool(lightningPool.build());
                
                // Restoration - 20% chance (more common healing spell)
                LootPool.Builder restorationPool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.20f))
                        .with(ItemEntry.builder(ModItems.SPELLBOOK_RESTORATION))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0f)).build());
                tableBuilder.pool(restorationPool.build());
                
                // Lure - 25% chance (most common)
                LootPool.Builder lurePool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.25f))
                        .with(ItemEntry.builder(ModItems.SPELLBOOK_LURE))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0f)).build());
                tableBuilder.pool(lurePool.build());
            }
            
            // Add rare spells and relics to temple chests
            if (source.isBuiltin() && TEMPLE_CHEST_IDS.contains(id)) {
                // Underworld Army - 10% chance (rare spell)
                LootPool.Builder underworldPool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.10f))
                        .with(ItemEntry.builder(ModItems.SPELLBOOK_UNDERWORLD_ARMY))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0f)).build());
                tableBuilder.pool(underworldPool.build());
                
                // Common relics - 15% chance
                LootPool.Builder relicPool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.15f))
                        .with(ItemEntry.builder(ModItems.RELIC_ANKH_OF_RA).weight(3))
                        .with(ItemEntry.builder(ModItems.RELIC_HELM_OF_ARES).weight(3))
                        .with(ItemEntry.builder(ModItems.RELIC_CORNUCOPIA).weight(2))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0f)).build());
                tableBuilder.pool(relicPool.build());
            }
            
            // Add epic relics to rare structure chests
            if (source.isBuiltin() && DUNGEON_CHEST_IDS.contains(id)) {
                // Epic relics - 8% chance
                LootPool.Builder epicRelicPool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.08f))
                        .with(ItemEntry.builder(ModItems.RELIC_MJOLNIR_FRAGMENT).weight(1))
                        .with(ItemEntry.builder(ModItems.RELIC_GOLDEN_FLEECE).weight(1))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0f)).build());
                tableBuilder.pool(epicRelicPool.build());
                
                // Also add some common spells to dungeons
                LootPool.Builder dungeonSpellPool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(0.12f))
                        .with(ItemEntry.builder(ModItems.SPELLBOOK_LIGHTNING_STORM).weight(2))
                        .with(ItemEntry.builder(ModItems.SPELLBOOK_RESTORATION).weight(3))
                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0f)).build());
                tableBuilder.pool(dungeonSpellPool.build());
            }
        });
    }
}

