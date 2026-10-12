package com.example.mythic_age.item;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Mod物品组注册类
 * 
 * 负责创建和管理神话时代Mod在创造模式物品栏中的自定义标签页。
 * 物品组提供了一个统一的地方来展示Mod的所有内容，方便玩家在创造模式中查找和使用。
 * 
 * 功能特性：
 * - 创建专属的"神话时代"标签页
 * - 有序组织所有Mod物品和方块
 * - 使用标志性的拉神安卡作为标签页图标
 * - 支持多语言的标签页名称
 * 
 * 物品组织结构：
 * 1. 神力法术书区域：
 *    - 12种神力法术书，按神祇重要性和功能分类
 * 
 * 2. 神话圣物区域：
 *    - 5种传说级神话圣物，按稀有度和文明排序
 * 
 * 3. 神力证明区域：
 *    - 5种神话背景的神力证明，作为货币系统
 * 
 * 4. 神话村庄建筑区域：
 *    - 5种神殿方块和5种神像方块
 *    - 圣物基座等功能方块
 * 
 * 5. 实体相关区域：
 *    - 神话生物和特殊村民的怪物蛋
 * 
 * 设计考虑：
 * - 按功能类型分组，便于查找
 * - 法术书在前，圣物次之，证明和建筑在中间，怪物蛋最后
 * - 使用拉神安卡作为图标，代表古代神话主题
 * - 标签页名称支持国际化
 * 
 * 技术实现：
 * - 使用Fabric API的FabricItemGroup构建器
 * - 静态注册确保在Mod加载时创建
 * - 通过entries回调动态添加物品
 * - 完整的注册表集成
 * 
 * @author MythicAge Development Team
 * @version 2.0.0
 * @since 1.20.1
 */
public class ModItemGroups {
    
    /**
     * 神话时代物品组
     * 
     * 创造模式中的专属标签页，包含所有神话时代Mod的物品和方块。
     * 使用拉神安卡作为标签页图标，象征古代神话的神秘力量。
     * 
     * 物品排列顺序：
     * 1. 神力法术书 - 按神祇重要性和功能分类
     * 2. 神话圣物 - 按稀有度和文明排序  
     * 3. 神力证明 - 四种神话背景的货币系统
     * 4. 神话建筑 - 神殿、神像和功能方块
     * 5. 实体相关 - 怪物蛋等
     */
    public static final ItemGroup MYTHIC_AGE_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(MythicAgeMod.MOD_ID, "mythic_age"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.mythic_age"))
                    .icon(() -> new ItemStack(ModItems.RELIC_ANKH_OF_RA))
                    .entries((displayContext, entries) -> {
                        // ============ 神力法术书区域 ============
                        // 基础神力法术书 - 按神祇重要性排序
                        entries.add(ModItems.SPELLBOOK_LIGHTNING_STORM);    // 宙斯 - 众神之王
                        entries.add(ModItems.SPELLBOOK_RESTORATION);        // 雅典娜 - 智慧女神
                        entries.add(ModItems.SPELLBOOK_LURE);               // 波塞冬 - 海神
                        entries.add(ModItems.SPELLBOOK_UNDERWORLD_ARMY);    // 哈迪斯 - 冥王
                        
                        // 高级神力法术书 - 按功能分类
                        entries.add(ModItems.SPELLBOOK_PESTILENCE);         // 阿波罗 - 瘟疫箭
                        entries.add(ModItems.SPELLBOOK_CURSE);              // 赫卡忒 - 诅咒变形
                        entries.add(ModItems.SPELLBOOK_EARTHQUAKE);         // 波塞冬 - 地震术
                        entries.add(ModItems.SPELLBOOK_CEASEFIRE);          // 伊瑞涅 - 和平术
                        entries.add(ModItems.SPELLBOOK_BRONZE);             // 狄俄尼索斯 - 青铜护甲
                        entries.add(ModItems.SPELLBOOK_UNDERWORLD_PASSAGE); // 阿波罗 - 冥界通道
                        entries.add(ModItems.SPELLBOOK_LIGHTNING_STORM_HERA); // 赫拉 - 强化雷暴
                        entries.add(ModItems.SPELLBOOK_PLENTY);             // 赫菲斯托斯 - 丰饶术
                        
                        // 中国神力法术书 - 按重要性排序
                        entries.add(ModItems.SPELLBOOK_RAIN_PRAYER);        // 祈雨术 - 龙王神力
                        entries.add(ModItems.SPELLBOOK_EARTH_GOD_BLESSING); // 土地神佑 - 土地神神力
                        entries.add(ModItems.SPELLBOOK_ENLIGHTENMENT);      // 启迪 - 文昌帝君神力
                        entries.add(ModItems.SPELLBOOK_ABUNDANCE);          // 丰收 - 后土神力
                        entries.add(ModItems.SPELLBOOK_FIVE_THUNDER);       // 五雷正法 - 雷公电母神力
                        entries.add(ModItems.SPELLBOOK_CRANE_BLESSING);     // 仙鹤祈福 - 仙鹤神力
                        entries.add(ModItems.SPELLBOOK_FLOOD);              // 洪水 - 共工神力
                        entries.add(ModItems.SPELLBOOK_CALL_TO_ARMS);       // 召唤守护者 - 门神神力
                        entries.add(ModItems.SPELLBOOK_TAIJI_SHIELD);       // 太极护盾 - 太极神力
                        entries.add(ModItems.SPELLBOOK_RECREATION);         // 再造 - 女娲神力
                        
                        // 埃及神力法术书 - 按重要性排序
                        entries.add(ModItems.SPELLBOOK_RA_RADIANCE);        // 拉神光辉 - 太阳神神力
                        entries.add(ModItems.SPELLBOOK_DESERT_STORM);       // 沙漠风暴 - 塞特神力
                        entries.add(ModItems.SPELLBOOK_SUN_RAIN);           // 太阳雨 - 拉神神力
                        entries.add(ModItems.SPELLBOOK_PROSPERITY);         // 繁荣 - 哈托尔神力
                        entries.add(ModItems.SPELLBOOK_EYE_OF_HORUS);       // 荷鲁斯之眼 - 荷鲁斯神力
                        entries.add(ModItems.SPELLBOOK_ANUBIS_JUDGMENT);    // 阿努比斯审判 - 阿努比斯神力
                        entries.add(ModItems.SPELLBOOK_LOCUST_SWARM);       // 蝗虫群 - 塞特神力
                        entries.add(ModItems.SPELLBOOK_TORNADO);            // 龙卷风 - 塞特神力
                        entries.add(ModItems.SPELLBOOK_ISIS_RESURRECTION);  // 伊西斯复活术 - 伊西斯神力
                        entries.add(ModItems.SPELLBOOK_METEOR);             // 陨石雨 - 拉神神力
                        
                        // 北欧神力法术书 - 按重要性排序
                        entries.add(ModItems.SPELLBOOK_FENRIR_HOWL);        // 芬里尔之嚎 - 巨狼神力
                        entries.add(ModItems.SPELLBOOK_ODIN_EYE);           // 奥丁之眼 - 众神之父神力
                        entries.add(ModItems.SPELLBOOK_GREAT_HUNT);         // 巨鹰 - 巨鹰神力
                        entries.add(ModItems.SPELLBOOK_DWARVEN_MINE);       // 矮人矿坑 - 矮人神力
                        entries.add(ModItems.SPELLBOOK_THOR_WRATH);         // 雷神之怒 - 雷神神力
                        entries.add(ModItems.SPELLBOOK_VALHALLA_CALL);      // 瓦尔哈拉召唤 - 奥丁神力
                        entries.add(ModItems.SPELLBOOK_FROST);              // 霜冻 - 约顿海姆神力
                        entries.add(ModItems.SPELLBOOK_WALKING_WOODS);      // 行走的森林 - 世界树神力
                        entries.add(ModItems.SPELLBOOK_RAGNAROK);           // 诸神黄昏 - 终极神力
                        entries.add(ModItems.SPELLBOOK_FIMBULWINTER);       // 芬布尔之冬 - 终极神力

                        // 亚特兰蒂斯神力法术书 - 按从小神力到终极神力排序
                        entries.add(ModItems.SPELLBOOK_TIDAL_SURGE);        // 潮汐奔流 - 水流攻击
                        entries.add(ModItems.SPELLBOOK_ABYSSAL_CURRENT);    // 深流跃迁 - 机动位移
                        entries.add(ModItems.SPELLBOOK_CRYSTAL_BARRIER);    // 水晶屏障 - 防御护盾
                        entries.add(ModItems.SPELLBOOK_PEARL_HARVEST);      // 珍珠丰收 - 海底资源
                        entries.add(ModItems.SPELLBOOK_ORICHALCUM_FORGE);   // 山铜熔炉 - 资源与强化
                        entries.add(ModItems.SPELLBOOK_SIREN_SONG);         // 海妖之歌 - 控制与恢复
                        entries.add(ModItems.SPELLBOOK_LEVIATHAN_CALL);     // 利维坦召唤 - 幻影压制
                        entries.add(ModItems.SPELLBOOK_ABYSSAL_RIFT);       // 深渊裂隙 - 拉扯伤害
                        entries.add(ModItems.SPELLBOOK_CHRONO_STASIS);      // 时滞静域 - 终极控制
                        entries.add(ModItems.SPELLBOOK_OCEANIC_WRATH);      // 海洋震怒 - 终极风暴
                        
                        // 神殿生成法术书 (测试用)
                        entries.add(ModItems.SPELLBOOK_GENERATE_EGYPTIAN_TEMPLE);  // 埃及神殿生成
                        entries.add(ModItems.SPELLBOOK_GENERATE_CHINESE_TEMPLE);   // 中国神殿生成
                        entries.add(ModItems.SPELLBOOK_GENERATE_ATLANTEAN_TEMPLE); // 亚特兰蒂斯神殿生成
                        entries.add(ModItems.SPELLBOOK_GENERATE_NORDIC_TEMPLE);    // 北欧神殿生成
                        
                        // ============ 神话圣物区域 ============
                        // 按文明和稀有度排序
                        
                        // === 中国神话圣物 ===
                        entries.add(ModItems.RELIC_SHENNONG_CAULDRON);      // 中国 - 稀有 - 神农鼎
                        entries.add(ModItems.RELIC_KUNLUN_MIRROR);          // 中国 - 史诗 - 昆仑镜
                        entries.add(ModItems.RELIC_HOUYI_BOW);              // 中国 - 史诗 - 后羿弓
                        entries.add(ModItems.RELIC_NUWA_STONE);             // 中国 - 史诗 - 女娲石
                        
                        // === 埃及神话圣物 ===
                        entries.add(ModItems.RELIC_ANKH_OF_RA);             // 埃及 - 稀有 - 拉神安卡
                        entries.add(ModItems.RELIC_SCARAB_OF_RA);           // 埃及 - 稀有 - 拉神圣甲虫
                        entries.add(ModItems.RELIC_EYE_OF_HORUS);           // 埃及 - 史诗 - 荷鲁斯之眼
                        entries.add(ModItems.RELIC_ANUBIS_STAFF);           // 埃及 - 史诗 - 阿努比斯权杖
                        entries.add(ModItems.RELIC_ANKH_OF_ISIS);           // 埃及 - 史诗 - 伊西斯的安卡
                        
                        // === 希腊神话圣物 ===
                        entries.add(ModItems.RELIC_HELM_OF_ARES);           // 希腊 - 稀有 - 阿瑞斯头盔
                        entries.add(ModItems.RELIC_GOLDEN_FLEECE);          // 希腊 - 史诗 - 金羊毛
                        
                        // === 罗马神话圣物 ===
                        entries.add(ModItems.RELIC_CORNUCOPIA);             // 罗马 - 稀有 - 丰饶角
                        
                        // === 北欧神话圣物 ===
                        entries.add(ModItems.RELIC_FREYJA_NECKLACE);        // 北欧 - 稀有 - 芙蕾雅项链
                        entries.add(ModItems.RELIC_ODIN_EYE);               // 北欧 - 史诗 - 奥丁之眼
                        entries.add(ModItems.RELIC_LOKI_MASK);              // 北欧 - 史诗 - 洛基面具
                        entries.add(ModItems.RELIC_MJOLNIR_FRAGMENT);       // 北欧 - 史诗 - 雷神锤碎片
                        
                        // ============ 神力证明区域 ============
                        // 五种神话背景的货币系统
                        entries.add(ModItems.GOD_POWER_CERTIFICATE_GREEK);     // 希腊神力证明
                        entries.add(ModItems.GOD_POWER_CERTIFICATE_EGYPTIAN);  // 埃及神力证明
                        entries.add(ModItems.GOD_POWER_CERTIFICATE_CHINESE);   // 中国神力证明
                        entries.add(ModItems.GOD_POWER_CERTIFICATE_ATLANTEAN); // 亚特兰蒂斯神力证明
                        entries.add(ModItems.GOD_POWER_CERTIFICATE_NORDIC);    // 北欧神力证明
                        
                        // ============ 希腊神庙建筑区域 ============
                        // 希腊神庙系列方块 - 按建筑用途排序
                        entries.add(ModBlocks.TEMPLE_GREEK_BRICKS);     // 希腊神庙砖
                        entries.add(ModBlocks.TEMPLE_GREEK_STAIRS);     // 希腊神庙台阶
                        entries.add(ModBlocks.TEMPLE_GREEK_SLAB);       // 希腊神庙半砖
                        entries.add(ModBlocks.TEMPLE_GREEK_PILLAR);     // 希腊神庙柱子
                        entries.add(ModBlocks.TEMPLE_GREEK_RELIEF);     // 希腊神庙浮雕
                        
                        // ============ 埃及神殿建筑区域 ============
                        // 埃及神殿系列方块 - 按建筑用途排序
                        entries.add(ModBlocks.TEMPLE_EGYPTIAN_BRICKS);  // 埃及神殿砖
                        entries.add(ModBlocks.TEMPLE_EGYPTIAN_STAIRS);  // 埃及神殿台阶
                        entries.add(ModBlocks.TEMPLE_EGYPTIAN_SLAB);    // 埃及神殿半砖
                        entries.add(ModBlocks.TEMPLE_EGYPTIAN_PILLAR);  // 埃及神殿柱子
                        entries.add(ModBlocks.TEMPLE_EGYPTIAN_RELIEF);  // 埃及神殿浮雕
                        
                        // ============ 中国神殿建筑区域 ============
                        // 中国神殿系列方块 - 按建筑用途排序
                        entries.add(ModBlocks.TEMPLE_CHINESE_BRICKS);   // 中国神殿砖
                        entries.add(ModBlocks.TEMPLE_CHINESE_STAIRS);   // 中国神殿台阶
                        entries.add(ModBlocks.TEMPLE_CHINESE_SLAB);     // 中国神殿半砖
                        entries.add(ModBlocks.TEMPLE_CHINESE_PILLAR);   // 中国神殿柱子
                        entries.add(ModBlocks.TEMPLE_CHINESE_RELIEF);   // 中国神殿浮雕
                        
                        // ============ 亚特兰蒂斯神殿建筑区域 ============
                        // 亚特兰蒂斯神殿系列方块 - 按建筑用途排序
                        entries.add(ModBlocks.TEMPLE_ATLANTEAN_BRICKS); // 亚特兰蒂斯神殿砖
                        entries.add(ModBlocks.TEMPLE_ATLANTEAN_STAIRS); // 亚特兰蒂斯神殿台阶
                        entries.add(ModBlocks.TEMPLE_ATLANTEAN_SLAB);   // 亚特兰蒂斯神殿半砖
                        entries.add(ModBlocks.TEMPLE_ATLANTEAN_PILLAR); // 亚特兰蒂斯神殿柱子
                        entries.add(ModBlocks.TEMPLE_ATLANTEAN_RELIEF); // 亚特兰蒂斯神殿浮雕
                        
                        // ============ 北欧神殿建筑区域 ============
                        // 北欧神殿系列方块 - 按建筑用途排序
                        entries.add(ModBlocks.TEMPLE_NORDIC_BRICKS);    // 北欧神殿砖
                        entries.add(ModBlocks.TEMPLE_NORDIC_STAIRS);    // 北欧神殿台阶
                        entries.add(ModBlocks.TEMPLE_NORDIC_SLAB);      // 北欧神殿半砖
                        entries.add(ModBlocks.TEMPLE_NORDIC_PILLAR);    // 北欧神殿柱子
                        entries.add(ModBlocks.TEMPLE_NORDIC_RELIEF);    // 北欧神殿浮雕
                        
                        // ============ 神话村庄建筑区域 ============
                        // 神殿方块 - 按神话背景排序
                        entries.add(ModBlocks.TEMPLE_EGYPTIAN);     // 埃及神殿
                        entries.add(ModBlocks.TEMPLE_CHINESE);      // 中国神殿
                        entries.add(ModBlocks.TEMPLE_ATLANTEAN);    // 亚特兰蒂斯神殿
                        entries.add(ModBlocks.TEMPLE_NORDIC);       // 北欧神殿
                        
                        // 神像方块 - 按神话背景排序
                        entries.add(ModBlocks.STATUE_GREEK);        // 希腊神像
                        entries.add(ModBlocks.STATUE_EGYPTIAN);     // 埃及神像
                        entries.add(ModBlocks.STATUE_CHINESE);      // 中国神像
                        entries.add(ModBlocks.STATUE_ATLANTEAN);    // 亚特兰蒂斯神像
                        entries.add(ModBlocks.STATUE_NORDIC);       // 北欧神像
                        
                        // ============ 多方块神像结构部分 ============
                        
                        // === 希腊神像多方块结构 ===
                        entries.add(ModBlocks.STATUE_GREEK_BASE);   // 希腊神像基座
                        entries.add(ModBlocks.STATUE_GREEK_LEGS);   // 希腊神像腿部
                        entries.add(ModBlocks.STATUE_GREEK_BODY);   // 希腊神像身体
                        entries.add(ModBlocks.STATUE_GREEK_HEAD);   // 希腊神像头部
                        
                        // === 埃及神像多方块结构 ===
                        entries.add(ModBlocks.STATUE_EGYPTIAN_BASE);  // 埃及神像基座
                        entries.add(ModBlocks.STATUE_EGYPTIAN_LEGS);  // 埃及神像腿部
                        entries.add(ModBlocks.STATUE_EGYPTIAN_BODY);  // 埃及神像身体
                        entries.add(ModBlocks.STATUE_EGYPTIAN_HEAD);  // 埃及神像头部
                        
                        // === 中国神像多方块结构 ===
                        entries.add(ModBlocks.STATUE_CHINESE_BASE);   // 中国神像基座
                        entries.add(ModBlocks.STATUE_CHINESE_LEGS);   // 中国神像腿部
                        entries.add(ModBlocks.STATUE_CHINESE_BODY);   // 中国神像身体
                        entries.add(ModBlocks.STATUE_CHINESE_HEAD);   // 中国神像头部
                        
                        // === 亚特兰蒂斯神像多方块结构 ===
                        entries.add(ModBlocks.STATUE_ATLANTEAN_BASE); // 亚特兰蒂斯神像基座
                        entries.add(ModBlocks.STATUE_ATLANTEAN_LEGS); // 亚特兰蒂斯神像腿部
                        entries.add(ModBlocks.STATUE_ATLANTEAN_BODY); // 亚特兰蒂斯神像身体
                        entries.add(ModBlocks.STATUE_ATLANTEAN_HEAD); // 亚特兰蒂斯神像头部
                        
                        // === 北欧神像多方块结构 ===
                        entries.add(ModBlocks.STATUE_NORDIC_BASE);    // 北欧神像基座
                        entries.add(ModBlocks.STATUE_NORDIC_LEGS);    // 北欧神像腿部
                        entries.add(ModBlocks.STATUE_NORDIC_BODY);    // 北欧神像身体
                        entries.add(ModBlocks.STATUE_NORDIC_HEAD);    // 北欧神像头部
                        
                        // 功能方块
                        entries.add(ModBlocks.RELIC_PEDESTAL);      // 圣物基座
                        
                        // ============ 埃及神像系统 ============
                        // 埃及神像基座
                        entries.add(ModBlocks.EGYPTIAN_STATUE_PEDESTAL);  // 埃及神像基座
                        
                        // 埃及神像物品 - 按等级排序
                        entries.add(ModItems.EGYPTIAN_STATUE_SLAVE);        // 1级：奴隶神像
                        entries.add(ModItems.EGYPTIAN_STATUE_CITIZEN);      // 2级：市民神像
                        entries.add(ModItems.EGYPTIAN_STATUE_SOLDIER);      // 3级：士兵神像
                        entries.add(ModItems.EGYPTIAN_STATUE_QUEEN);        // 4级：王后神像
                        entries.add(ModItems.EGYPTIAN_STATUE_KING);         // 5级：国王神像
                        entries.add(ModItems.EGYPTIAN_STATUE_ROYAL_COUPLE); // 6级：王室夫妇神像
                        entries.add(ModItems.EGYPTIAN_STATUE_PHARAOH);      // 7级：法老神像
                        
                        // 丰饶祭坛相关方块
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_CORE);        // 祭坛核心
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_PILLAR);      // 祭坛柱子
                        entries.add(ModBlocks.MEAT_PILE);                   // 肉堆
                        entries.add(ModBlocks.WOOD_PILE);                   // 木堆
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_BRICK);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_H);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_EW);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_NS);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_V);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_CORNER);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_WALL);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_BOTTOM);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_TOP);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_STAIRS);
                        entries.add(ModBlocks.ABUNDANCE_ALTAR_SLAB);
                        entries.add(ModBlocks.ABUNDANCE_STORAGE_MINERAL);   // 储藏室(矿物)
                        entries.add(ModBlocks.ABUNDANCE_STORAGE_FOOD);      // 储藏室(食物)
                        entries.add(ModBlocks.ABUNDANCE_STORAGE_WOOD);      // 储藏室(木材)
                        
                        // 动物招引塔
                        entries.add(ModBlocks.ANIMAL_LURE_TOWER);           // 动物招引塔
                        
                        // ============ 实体相关区域 ============
                        // 怪物蛋 - 按实体类型排序
                        entries.add(ModItems.MINOTAUR_SPAWN_EGG);                    // 米诺陶怪物蛋
                        entries.add(ModItems.CYCLOPS_SPAWN_EGG);                     // 独眼巨人怪物蛋
                        entries.add(ModItems.CENTAUR_SPAWN_EGG);                     // 半人马怪物蛋
                        entries.add(ModItems.HYDRA_SPAWN_EGG);                       // 九头蛇怪物蛋
                        entries.add(ModItems.MEDUSA_SPAWN_EGG);                      // 美杜莎怪物蛋
                        entries.add(ModItems.NEMEAN_LION_SPAWN_EGG);                 // 尼米亚狮怪物蛋
                        entries.add(ModItems.MANTICORE_SPAWN_EGG);                   // 蝎尾狮怪物蛋
                        entries.add(ModItems.CHIMERA_SPAWN_EGG);                     // 奇美拉怪物蛋
                        entries.add(ModItems.COLOSSUS_SPAWN_EGG);                    // 青铜巨像怪物蛋
                        entries.add(ModItems.PEGASUS_SPAWN_EGG);                     // 天马刷怪蛋
                        entries.add(ModItems.ANUBIS_WARRIOR_SPAWN_EGG);              // 阿努比斯战士刷怪蛋
                        entries.add(ModItems.SCARAB_GUARDIAN_SPAWN_EGG);             // 圣甲虫守卫刷怪蛋
                        entries.add(ModItems.MUMMY_SOLDIER_SPAWN_EGG);               // 木乃伊士兵刷怪蛋
                        entries.add(ModItems.SPHINX_SENTINEL_SPAWN_EGG);             // 狮身人面守卫刷怪蛋
                        entries.add(ModItems.SUN_FALCON_SPAWN_EGG);                  // 太阳隼刷怪蛋
                        entries.add(ModItems.FROST_GIANT_SPAWN_EGG);                 // 霜巨人刷怪蛋
                        entries.add(ModItems.DRAUGR_WARRIOR_SPAWN_EGG);              // 亡灵战士刷怪蛋
                        entries.add(ModItems.VALKYRIE_SPAWN_EGG);                    // 女武神刷怪蛋
                        entries.add(ModItems.FENRIR_WOLF_SPAWN_EGG);                 // 芬里尔巨狼刷怪蛋
                        entries.add(ModItems.DWARF_BERSERKER_SPAWN_EGG);             // 矮人狂战士刷怪蛋
                        entries.add(ModItems.AZURE_DRAGON_SPAWN_EGG);                // 青龙刷怪蛋
                        entries.add(ModItems.QILIN_GUARDIAN_SPAWN_EGG);              // 麒麟守护者刷怪蛋
                        entries.add(ModItems.JIANGSHI_SPAWN_EGG);                    // 僵尸刷怪蛋
                        entries.add(ModItems.NINE_TAILED_FOX_SPAWN_EGG);             // 九尾狐刷怪蛋
                        entries.add(ModItems.STONE_LION_GUARDIAN_SPAWN_EGG);         // 石狮守卫刷怪蛋
                        entries.add(ModItems.CRYSTAL_LEVIATHAN_SPAWN_EGG);           // 水晶利维坦刷怪蛋
                        entries.add(ModItems.ORICHALCUM_GOLEM_SPAWN_EGG);            // 山铜魔像刷怪蛋
                        entries.add(ModItems.SIREN_PRIESTESS_SPAWN_EGG);             // 海妖祭司刷怪蛋
                        entries.add(ModItems.TIDAL_GUARDIAN_SPAWN_EGG);              // 潮汐守卫刷怪蛋
                        entries.add(ModItems.ABYSSAL_RAY_SPAWN_EGG);                 // 深渊鳐刷怪蛋
                        
                        // 神殿守护者刷怪蛋 - 按文明排序
                        entries.add(ModItems.TEMPLE_GUARDIAN_GREEK_SPAWN_EGG);       // 希腊神殿守护者刷怪蛋
                        entries.add(ModItems.TEMPLE_GUARDIAN_EGYPTIAN_SPAWN_EGG);    // 埃及神殿守护者刷怪蛋
                        entries.add(ModItems.TEMPLE_GUARDIAN_CHINESE_SPAWN_EGG);     // 中国神殿守护者刷怪蛋
                        entries.add(ModItems.TEMPLE_GUARDIAN_ATLANTEAN_SPAWN_EGG);   // 亚特兰蒂斯神殿守护者刷怪蛋
                        entries.add(ModItems.TEMPLE_GUARDIAN_NORDIC_SPAWN_EGG);      // 北欧神殿守护者刷怪蛋
                        
                        // 自定义神殿守护者刷怪蛋 - 按文明排序（不受原版村民限制）
                        entries.add(ModItems.CUSTOM_TEMPLE_GUARDIAN_GREEK_SPAWN_EGG);       // 自定义希腊神殿守护者刷怪蛋
                        entries.add(ModItems.CUSTOM_TEMPLE_GUARDIAN_EGYPTIAN_SPAWN_EGG);    // 自定义埃及神殿守护者刷怪蛋
                        entries.add(ModItems.CUSTOM_TEMPLE_GUARDIAN_CHINESE_SPAWN_EGG);     // 自定义中国神殿守护者刷怪蛋
                        entries.add(ModItems.CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN_SPAWN_EGG);   // 自定义亚特兰蒂斯神殿守护者刷怪蛋
                        entries.add(ModItems.CUSTOM_TEMPLE_GUARDIAN_NORDIC_SPAWN_EGG);      // 自定义北欧神殿守护者刷怪蛋
                        
                        // 神力指南系统
                        entries.add(ModItems.GOD_POWER_GUIDE);                       // 神力指南
                    })
                    .build());

    /**
     * 物品组注册方法
     * 
     * 在Mod初始化时调用，确保自定义物品组被正确注册到游戏中。
     * 由于使用静态初始化器，物品组在类加载时就会自动创建和注册。
     * 
     * 注册过程：
     * 1. 创建物品组实例
     * 2. 设置显示名称和图标
     * 3. 定义包含的物品列表
     * 4. 注册到游戏的物品组注册表
     * 
     * 包含的内容统计：
     * - 12种神力法术书
     * - 5种神话圣物
     * - 5种神力证明
     * - 7种埃及神像物品
     * - 25种神殿建筑方块（5系列 × 5类型）
     * - 5种单一神殿方块
     * - 5种神像方块
     * - 1种埃及神像基座
     * - 1种圣物基座
     * - 19种丰饶祭坛方块
     * - 1种动物招引塔
     * - 2种怪物蛋
     * 总计：88种物品/方块
     */
    public static void registerItemGroups() {
        MythicAgeMod.LOGGER.info("Registering Item Groups for " + MythicAgeMod.MOD_ID);
        MythicAgeMod.LOGGER.info("Created Mythic Age creative tab with comprehensive building system:");
        MythicAgeMod.LOGGER.info("- 12 spellbooks, 5 relics, 5 certificates");
        MythicAgeMod.LOGGER.info("- 7 Egyptian statue items, 1 statue pedestal");
        MythicAgeMod.LOGGER.info("- 25 temple building blocks (5 civilizations × 5 types)");
        MythicAgeMod.LOGGER.info("- 5 simple temple blocks, 5 statue blocks");
        MythicAgeMod.LOGGER.info("- 19 abundance altar blocks, 1 lure tower");
        MythicAgeMod.LOGGER.info("- 2 spawn eggs - Total: 88 items");
    }
}
