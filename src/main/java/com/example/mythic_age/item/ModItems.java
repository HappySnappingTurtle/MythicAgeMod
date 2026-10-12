package com.example.mythic_age.item;

import com.example.mythic_age.MythicAgeMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.item.Item;

/**
 * 神话时代 Mod 物品注册类
 * 
 * 负责注册和管理Mod中的所有物品，包括法术书、圣物等核心游戏内容。
 * 所有物品都在此类中定义为静态常量，并通过registerItem方法注册到游戏中。
 * 
 * 包含的物品类型：
 * 1. 神力法术书系统 (4种)：
 *    - 闪电风暴法术书 (宙斯) - 范围攻击法术
 *    - 复原术法术书 (雅典娜) - 治疗法术
 *    - 动物招引法术书 (波塞冬) - 辅助法术
 *    - 冥府军团法术书 (哈迪斯) - 召唤法术
 * 
 * 2. 神话圣物系统 (5种)：
 *    - 拉神安卡 - 埃及神话，再生效果
 *    - 阿瑞斯头盔 - 希腊神话，力量效果
 *    - 雷神锤碎片 - 北欧神话，急迫效果
 *    - 金羊毛 - 希腊神话，幸运和经验效果
 *    - 丰饶角 - 罗马神话，饱食和食物生成效果
 * 
 * 每个物品都有特定的稀有度、堆叠限制和功能特性。
 * 法术书具有冷却机制，圣物具有三层效果系统。
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class ModItems {

    // ============ 神力法术书系统 ============
    
    /** 
     * 闪电风暴法术书 (宙斯神力)
     * 稀有度：不常见 | 冷却：6秒 | 范围：5x5区域闪电攻击
     * 在目标位置召唤强力的闪电风暴，对区域内的敌对生物造成大量伤害
     */
    public static final Item SPELLBOOK_LIGHTNING_STORM = registerItem("spellbook_lightning_storm",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             120, SpellbookItem.SpellType.LIGHTNING_STORM));
    
    /** 
     * 复原术法术书 (雅典娜神力)
     * 稀有度：不常见 | 冷却：9秒 | 效果：治疗自己和附近玩家
     * 治疗施法者和10格范围内的所有玩家，恢复10点生命值(5颗心)
     */
    public static final Item SPELLBOOK_RESTORATION = registerItem("spellbook_restoration",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             180, SpellbookItem.SpellType.RESTORATION));
    
    /** 
     * 动物招引法术书 (波塞冬神力)
     * 稀有度：常见 | 冷却：5秒 | 效果：吸引附近的被动动物
     * 将30格范围内的被动动物吸引到指定位置，便于驯养和管理
     */
    public static final Item SPELLBOOK_LURE = registerItem("spellbook_lure",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.COMMON), 
                             100, SpellbookItem.SpellType.LURE));
    
    /** 
     * 冥府军团法术书 (哈迪斯神力)
     * 稀有度：稀有 | 冷却：15秒 | 效果：召唤友方骷髅战士
     * 在周围召唤3-5个友方骷髅战士，它们不会攻击召唤者，持续战斗直到死亡
     */
    public static final Item SPELLBOOK_UNDERWORLD_ARMY = registerItem("spellbook_underworld_army",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             300, SpellbookItem.SpellType.UNDERWORLD_ARMY));

    // ============ 新增希腊神明神力法术书 ============
    
    /** 
     * 瘟疫法术书 (阿瑞斯神力)
     * 稀有度：不常见 | 冷却：8秒 | 效果：削弱敌对生物
     * 对目标区域内的敌对生物施加虚弱、缓慢和中毒效果
     */
    public static final Item SPELLBOOK_PESTILENCE = registerItem("spellbook_pestilence",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             160, SpellbookItem.SpellType.PESTILENCE));
    
    /** 
     * 诅咒法术书 (阿芙洛狄忒神力)
     * 稀有度：稀有 | 冷却：12秒 | 效果：将敌人变成猪
     * 将目标区域内受伤的敌对生物变成无害的猪
     */
    public static final Item SPELLBOOK_CURSE = registerItem("spellbook_curse",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             240, SpellbookItem.SpellType.CURSE));
    
    /** 
     * 地震法术书 (阿尔忒弥斯神力)
     * 稀有度：史诗 | 冷却：20秒 | 效果：大范围破坏建筑
     * 在目标区域制造地震，破坏大范围内的方块和建筑
     */
    public static final Item SPELLBOOK_EARTHQUAKE = registerItem("spellbook_earthquake",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             400, SpellbookItem.SpellType.EARTHQUAKE));
    
    /** 
     * 停火法术书 (赫尔墨斯神力)
     * 稀有度：不常见 | 冷却：25秒 | 效果：暂时停止战斗
     * 对大范围内的敌对生物施加虚弱和缓慢效果，并清除它们的攻击目标
     */
    public static final Item SPELLBOOK_CEASEFIRE = registerItem("spellbook_ceasefire",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             500, SpellbookItem.SpellType.CEASEFIRE));
    
    /** 
     * 青铜法术书 (狄俄尼索斯神力)
     * 稀有度：不常见 | 冷却：10秒 | 效果：增强护甲和力量
     * 为自己和附近的盟友提供抗性和力量效果，持续90秒
     */
    public static final Item SPELLBOOK_BRONZE = registerItem("spellbook_bronze",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             200, SpellbookItem.SpellType.BRONZE));
    
    /** 
     * 冥界通道法术书 (阿波罗神力)
     * 稀有度：稀有 | 冷却：15秒 | 效果：瞬间传送
     * 将施法者传送到目标位置，创造传送门特效
     */
    public static final Item SPELLBOOK_UNDERWORLD_PASSAGE = registerItem("spellbook_underworld_passage",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             300, SpellbookItem.SpellType.UNDERWORLD_PASSAGE));
    
    /** 
     * 赫拉雷暴法术书 (赫拉神力)
     * 稀有度：史诗 | 冷却：18秒 | 效果：增强版闪电风暴
     * 比宙斯的闪电风暴更强力，覆盖7x7区域，闪电密度更高
     */
    public static final Item SPELLBOOK_LIGHTNING_STORM_HERA = registerItem("spellbook_lightning_storm_hera",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             360, SpellbookItem.SpellType.LIGHTNING_STORM_HERA));
    
    /** 
     * 丰饶法术书 (赫菲斯托斯神力)
     * 稀有度：稀有 | 冷却：30秒 | 效果：生成资源
     * 在目标位置生成各种珍贵的资源方块
     */
    public static final Item SPELLBOOK_PLENTY = registerItem("spellbook_plenty",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             600, SpellbookItem.SpellType.PLENTY));

    // ============ 中国神力法术书系统 ============
    
    /** 
     * 祈雨术法术书 (中国神力)
     * 稀有度：不常见 | 冷却：8秒 | 效果：召唤龙王降雨
     * 召唤龙王降雨，促进农作物生长，熄灭火焰
     */
    public static final Item SPELLBOOK_RAIN_PRAYER = registerItem("spellbook_rain_prayer",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             160, SpellbookItem.SpellType.RAIN_PRAYER));
    
    /** 
     * 土地神佑法术书 (中国神力)
     * 稀有度：不常见 | 冷却：10秒 | 效果：获得土地神庇护
     * 获得土地神庇护，提升资源采集效率
     */
    public static final Item SPELLBOOK_EARTH_GOD_BLESSING = registerItem("spellbook_earth_god_blessing",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             200, SpellbookItem.SpellType.EARTH_GOD_BLESSING));
    
    /** 
     * 五雷正法法术书 (中国神力)
     * 稀有度：稀有 | 冷却：12秒 | 效果：召唤五行雷电
     * 召唤五行雷电，对敌人造成毁灭性打击
     */
    public static final Item SPELLBOOK_FIVE_THUNDER = registerItem("spellbook_five_thunder",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             240, SpellbookItem.SpellType.FIVE_THUNDER));
    
    /** 
     * 启迪法术书 (中国神力)
     * 稀有度：不常见 | 冷却：6秒 | 效果：开启智慧之门
     * 开启智慧之门，大幅提升经验获取速度
     */
    public static final Item SPELLBOOK_ENLIGHTENMENT = registerItem("spellbook_enlightenment",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             120, SpellbookItem.SpellType.ENLIGHTENMENT));
    
    /** 
     * 丰收法术书 (中国神力)
     * 稀有度：不常见 | 冷却：10秒 | 效果：祈求丰收之神恩赐
     * 祈求丰收之神的恩赐，使农作物产量翻倍
     */
    public static final Item SPELLBOOK_ABUNDANCE = registerItem("spellbook_abundance",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             200, SpellbookItem.SpellType.ABUNDANCE));
    
    /** 
     * 仙鹤祈福法术书 (中国神力)
     * 稀有度：稀有 | 冷却：15秒 | 效果：仙鹤降临带来祝福
     * 仙鹤降临带来祝福，恢复生命值并获得飞行能力
     */
    public static final Item SPELLBOOK_CRANE_BLESSING = registerItem("spellbook_crane_blessing",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             300, SpellbookItem.SpellType.CRANE_BLESSING));
    
    /** 
     * 洪水法术书 (中国神力)
     * 稀有度：史诗 | 冷却：20秒 | 效果：引发滔天洪水
     * 引发滔天洪水，冲刷大地上的一切敌对生物
     */
    public static final Item SPELLBOOK_FLOOD = registerItem("spellbook_flood",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             400, SpellbookItem.SpellType.FLOOD));
    
    /** 
     * 召唤守护者法术书 (中国神力)
     * 稀有度：史诗 | 冷却：18秒 | 效果：召唤强大守护者
     * 召唤强大的守护者为你而战，忠诚护主直至战斗结束
     */
    public static final Item SPELLBOOK_CALL_TO_ARMS = registerItem("spellbook_call_to_arms",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             360, SpellbookItem.SpellType.CALL_TO_ARMS));
    
    /** 
     * 太极护盾法术书 (中国神力)
     * 稀有度：史诗 | 冷却：25秒 | 效果：凝聚太极之力护盾
     * 凝聚太极之力形成护盾，吸收伤害并反弹给攻击者
     */
    public static final Item SPELLBOOK_TAIJI_SHIELD = registerItem("spellbook_taiji_shield",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             500, SpellbookItem.SpellType.TAIJI_SHIELD));
    
    /** 
     * 再造法术书 (中国神力)
     * 稀有度：传奇 | 冷却：30秒 | 效果：逆转时空重塑环境
     * 逆转时空，重塑周围环境，化腐朽为神奇的终极力量
     */
    public static final Item SPELLBOOK_RECREATION = registerItem("spellbook_recreation",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             600, SpellbookItem.SpellType.RECREATION));

    // ============ 埃及神力法术书系统 ============
    
    /** 
     * 拉神光辉法术书 (埃及神力)
     * 稀有度：稀有 | 冷却：10秒 | 效果：召唤太阳神的光辉
     * 召唤太阳神的光辉，治疗友军并伤害亡灵
     */
    public static final Item SPELLBOOK_RA_RADIANCE = registerItem("spellbook_ra_radiance",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             200, SpellbookItem.SpellType.RA_RADIANCE));
    
    /** 
     * 沙漠风暴法术书 (埃及神力)
     * 稀有度：不常见 | 冷却：8秒 | 效果：召唤沙漠风暴
     * 召唤沙漠风暴，阻挡视线并造成持续伤害
     */
    public static final Item SPELLBOOK_DESERT_STORM = registerItem("spellbook_desert_storm",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             160, SpellbookItem.SpellType.DESERT_STORM));
    
    /** 
     * 荷鲁斯之眼法术书 (埃及神力)
     * 稀有度：稀有 | 冷却：15秒 | 效果：获得全知之眼
     * 获得荷鲁斯之眼的洞察力，探测敌人和宝藏
     */
    public static final Item SPELLBOOK_EYE_OF_HORUS = registerItem("spellbook_eye_of_horus",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             300, SpellbookItem.SpellType.EYE_OF_HORUS));
    
    /** 
     * 太阳雨法术书 (埃及神力)
     * 稀有度：不常见 | 冷却：8秒 | 效果：降下金色太阳雨
     * 降下金色的太阳雨，治愈友军并加速植物生长
     */
    public static final Item SPELLBOOK_SUN_RAIN = registerItem("spellbook_sun_rain",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             160, SpellbookItem.SpellType.SUN_RAIN));
    
    /** 
     * 繁荣法术书 (埃及神力)
     * 稀有度：不常见 | 冷却：12秒 | 效果：获得繁荣之神眷顾
     * 获得繁荣之神的眷顾，大幅增加贸易收益和宝物发现几率
     */
    public static final Item SPELLBOOK_PROSPERITY = registerItem("spellbook_prosperity",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             240, SpellbookItem.SpellType.PROSPERITY));
    
    /** 
     * 阿努比斯审判法术书 (埃及神力)
     * 稀有度：稀有 | 冷却：18秒 | 效果：召唤死神审判
     * 召唤死神阿努比斯进行审判，对邪恶生物造成致命打击
     */
    public static final Item SPELLBOOK_ANUBIS_JUDGMENT = registerItem("spellbook_anubis_judgment",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             360, SpellbookItem.SpellType.ANUBIS_JUDGMENT));
    
    /** 
     * 蝗虫群法术书 (埃及神力)
     * 稀有度：史诗 | 冷却：20秒 | 效果：释放蝗虫群
     * 释放蝗虫群，吞噬敌人的装备和建筑，带来毁灭性打击
     */
    public static final Item SPELLBOOK_LOCUST_SWARM = registerItem("spellbook_locust_swarm",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             400, SpellbookItem.SpellType.LOCUST_SWARM));
    
    /** 
     * 龙卷风法术书 (埃及神力)
     * 稀有度：史诗 | 冷却：22秒 | 效果：召唤强力龙卷风
     * 召唤强力龙卷风，将敌人卷入空中并造成大范围破坏
     */
    public static final Item SPELLBOOK_TORNADO = registerItem("spellbook_tornado",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             440, SpellbookItem.SpellType.TORNADO));
    
    /** 
     * 伊西斯复活术法术书 (埃及神力)
     * 稀有度：史诗 | 冷却：25秒 | 效果：借助女神力量复活
     * 借助伊西斯女神的力量，复活死去的生物为己所用
     */
    public static final Item SPELLBOOK_ISIS_RESURRECTION = registerItem("spellbook_isis_resurrection",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             500, SpellbookItem.SpellType.ISIS_RESURRECTION));
    
    /** 
     * 陨石雨法术书 (埃及神力)
     * 稀有度：传奇 | 冷却：30秒 | 效果：召唤陨石雨
     * 从天空召唤陨石雨，对大范围区域造成毁天灭地的伤害
     */
    public static final Item SPELLBOOK_METEOR = registerItem("spellbook_meteor",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             600, SpellbookItem.SpellType.METEOR));

    // ============ 北欧神力法术书系统 ============
    
    /** 
     * 芬里尔之嚎法术书 (北欧神力)
     * 稀有度：稀有 | 冷却：12秒 | 效果：发出巨狼长嚎
     * 发出巨狼长嚎，恐吓敌人并召唤狼群
     */
    public static final Item SPELLBOOK_FENRIR_HOWL = registerItem("spellbook_fenrir_howl",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             240, SpellbookItem.SpellType.FENRIR_HOWL));
    
    /** 
     * 奥丁之眼法术书 (北欧神力)
     * 稀有度：史诗 | 冷却：18秒 | 效果：获得全知之眼
     * 获得全知之眼，洞察战场信息
     */
    public static final Item SPELLBOOK_ODIN_EYE = registerItem("spellbook_odin_eye",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             360, SpellbookItem.SpellType.ODIN_EYE));
    
    /** 
     * 雷神之怒法术书 (北欧神力)
     * 稀有度：史诗 | 冷却：15秒 | 效果：挥舞雷神锤
     * 挥舞雷神锤，召唤毁灭性雷暴
     */
    public static final Item SPELLBOOK_THOR_WRATH = registerItem("spellbook_thor_wrath",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             300, SpellbookItem.SpellType.THOR_WRATH));
    
    /** 
     * 巨鹰法术书 (北欧神力)
     * 稀有度：不常见 | 冷却：10秒 | 效果：召唤巨鹰狩猎
     * 召唤巨鹰进行狩猎，从高空俯冲攻击敌人
     */
    public static final Item SPELLBOOK_GREAT_HUNT = registerItem("spellbook_great_hunt",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             200, SpellbookItem.SpellType.GREAT_HUNT));
    
    /** 
     * 矮人矿坑法术书 (北欧神力)
     * 稀有度：不常见 | 冷却：12秒 | 效果：开启矮人秘密矿坑
     * 开启矮人的秘密矿坑，获得珍贵的矿物资源
     */
    public static final Item SPELLBOOK_DWARVEN_MINE = registerItem("spellbook_dwarven_mine",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON), 
                             240, SpellbookItem.SpellType.DWARVEN_MINE));
    
    /** 
     * 瓦尔哈拉召唤法术书 (北欧神力)
     * 稀有度：稀有 | 冷却：18秒 | 效果：召唤英灵战士
     * 召唤瓦尔哈拉的英灵战士，与你并肩作战至死不渝
     */
    public static final Item SPELLBOOK_VALHALLA_CALL = registerItem("spellbook_valhalla_call",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE), 
                             360, SpellbookItem.SpellType.VALHALLA_CALL));
    
    /** 
     * 霜冻法术书 (北欧神力)
     * 稀有度：史诗 | 冷却：16秒 | 效果：释放极地寒霜
     * 释放极地寒霜，冰封敌人并造成冰霜伤害
     */
    public static final Item SPELLBOOK_FROST = registerItem("spellbook_frost",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             320, SpellbookItem.SpellType.FROST));
    
    /** 
     * 行走的森林法术书 (北欧神力)
     * 稀有度：史诗 | 冷却：20秒 | 效果：唤醒森林力量
     * 唤醒森林的力量，让树木行走并攻击入侵者
     */
    public static final Item SPELLBOOK_WALKING_WOODS = registerItem("spellbook_walking_woods",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             400, SpellbookItem.SpellType.WALKING_WOODS));
    
    /** 
     * 诸神黄昏法术书 (北欧神力)
     * 稀有度：史诗 | 冷却：25秒 | 效果：引发诸神黄昏
     * 引发诸神黄昏，带来世界末日般的毁灭力量
     */
    public static final Item SPELLBOOK_RAGNAROK = registerItem("spellbook_ragnarok",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             500, SpellbookItem.SpellType.RAGNAROK));
    
    /** 
     * 芬布尔之冬法术书 (北欧神力)
     * 稀有度：传奇 | 冷却：30秒 | 效果：召唤永恒严寒
     * 召唤芬布尔之冬，用永恒的严寒冰封整个世界
     */
    public static final Item SPELLBOOK_FIMBULWINTER = registerItem("spellbook_fimbulwinter",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             600, SpellbookItem.SpellType.FIMBULWINTER));

    // ============ 亚特兰蒂斯神力法术书系统 ============

    /**
     * 潮汐奔流法术书 (亚特兰蒂斯神力)
     * 稀有度：不常见 | 冷却：9秒 | 效果：水流冲击并减速敌人
     * 在目标区域释放潮汐，伤害敌对生物并用海浪打乱阵型
     */
    public static final Item SPELLBOOK_TIDAL_SURGE = registerItem("spellbook_tidal_surge",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON),
                             180, SpellbookItem.SpellType.TIDAL_SURGE));

    /**
     * 深流跃迁法术书 (亚特兰蒂斯神力)
     * 稀有度：不常见 | 冷却：10秒 | 效果：短距潮流位移并提供水下机动
     * 让施法者沿暗潮跃迁到目标位置，并获得短暂水下行动优势
     */
    public static final Item SPELLBOOK_ABYSSAL_CURRENT = registerItem("spellbook_abyssal_current",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON),
                             200, SpellbookItem.SpellType.ABYSSAL_CURRENT));

    /**
     * 水晶屏障法术书 (亚特兰蒂斯神力)
     * 稀有度：不常见 | 冷却：11秒 | 效果：为盟友提供屏障和水下适应
     * 召唤青蓝水晶护盾，为周围玩家提供吸收、抗性和水下呼吸
     */
    public static final Item SPELLBOOK_CRYSTAL_BARRIER = registerItem("spellbook_crystal_barrier",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON),
                             220, SpellbookItem.SpellType.CRYSTAL_BARRIER));

    /**
     * 珍珠丰收法术书 (亚特兰蒂斯神力)
     * 稀有度：稀有 | 冷却：13秒 | 效果：召回海底贡藏和幸运增益
     * 在目标区域凝聚珍珠、鹦鹉螺壳与海晶资源
     */
    public static final Item SPELLBOOK_PEARL_HARVEST = registerItem("spellbook_pearl_harvest",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE),
                             260, SpellbookItem.SpellType.PEARL_HARVEST));

    /**
     * 山铜熔炉法术书 (亚特兰蒂斯神力)
     * 稀有度：稀有 | 冷却：14秒 | 效果：生成海晶与金属并强化采集
     * 点亮失落熔炉，为施法者提供采集增益并产出山铜风格资源
     */
    public static final Item SPELLBOOK_ORICHALCUM_FORGE = registerItem("spellbook_orichalcum_forge",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE),
                             280, SpellbookItem.SpellType.ORICHALCUM_FORGE));

    /**
     * 海妖之歌法术书 (亚特兰蒂斯神力)
     * 稀有度：稀有 | 冷却：16秒 | 效果：控制敌人并治愈盟友
     * 释放海妖圣歌，让敌人迟缓虚弱，同时为盟友带来短暂恢复
     */
    public static final Item SPELLBOOK_SIREN_SONG = registerItem("spellbook_siren_song",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE),
                             320, SpellbookItem.SpellType.SIREN_SONG));

    /**
     * 利维坦召唤法术书 (亚特兰蒂斯神力)
     * 稀有度：史诗 | 冷却：20秒 | 效果：召来利维坦幻影压制敌群
     * 用深海巨兽的幻影震慑敌人，造成伤害、发光和拉扯
     */
    public static final Item SPELLBOOK_LEVIATHAN_CALL = registerItem("spellbook_leviathan_call",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC),
                             400, SpellbookItem.SpellType.LEVIATHAN_CALL));

    /**
     * 深渊裂隙法术书 (亚特兰蒂斯神力)
     * 稀有度：史诗 | 冷却：22秒 | 效果：拉扯并伤害敌人
     * 在目标区域打开深渊裂隙，将敌人拖向中心并施加枯竭伤害
     */
    public static final Item SPELLBOOK_ABYSSAL_RIFT = registerItem("spellbook_abyssal_rift",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC),
                             440, SpellbookItem.SpellType.ABYSSAL_RIFT));

    /**
     * 时滞静域法术书 (亚特兰蒂斯神力)
     * 稀有度：史诗 | 冷却：27秒 | 效果：大范围冻结战场节奏
     * 以失落科技停滞敌人行动，为施法者创造战术窗口
     */
    public static final Item SPELLBOOK_CHRONO_STASIS = registerItem("spellbook_chrono_stasis",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC),
                             540, SpellbookItem.SpellType.CHRONO_STASIS));

    /**
     * 海洋震怒法术书 (亚特兰蒂斯神力)
     * 稀有度：史诗 | 冷却：30秒 | 效果：召唤终极海洋风暴
     * 引发海洋风暴、雷电和震波，大范围压制敌人并强化施法者
     */
    public static final Item SPELLBOOK_OCEANIC_WRATH = registerItem("spellbook_oceanic_wrath",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC),
                             600, SpellbookItem.SpellType.OCEANIC_WRATH));

    // ============ 神话圣物系统 ============
    
    /** 
     * 拉神安卡 (埃及神话圣物)
     * 稀有度：稀有 | 主题：生命与治愈
     * 手持效果：再生 I | 神殿效果：5格范围持续再生
     */
    public static final Item RELIC_ANKH_OF_RA = registerItem("relic_ankh_of_ra",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));
            
    /** 
     * 阿瑞斯头盔 (希腊神话圣物)
     * 稀有度：稀有 | 主题：战争与力量
     * 手持效果：力量 I | 神殿效果：5格范围战斗力量
     */
    public static final Item RELIC_HELM_OF_ARES = registerItem("relic_helm_of_ares",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));
            
    /** 
     * 雷神锤碎片 (北欧神话圣物)
     * 稀有度：史诗 | 主题：雷电与力量
     * 手持效果：急迫 I | 神殿效果：8格范围急迫和抗性 | 特殊：雷暴天气闪电特效
     */
    public static final Item RELIC_MJOLNIR_FRAGMENT = registerItem("relic_mjolnir_fragment",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
            
    /** 
     * 伊阿宋的金羊毛 (希腊神话圣物)
     * 稀有度：史诗 | 主题：财富与好运
     * 手持效果：幸运 I + 速度 I | 被动效果：每10秒获得经验
     * 神殿效果：6格范围幸运、村庄英雄效果，每5秒给予经验
     */
    public static final Item RELIC_GOLDEN_FLEECE = registerItem("relic_golden_fleece",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
            
    /** 
     * 丰饶角 (罗马神话圣物)
     * 稀有度：稀有 | 主题：丰收与富饶
     * 手持效果：饱和 I + 缓慢恢复饥饿 | 被动效果：每30秒生成面包
     * 神殿效果：7格范围饱和效果，每15秒在周围生成食物
     */
    public static final Item RELIC_CORNUCOPIA = registerItem("relic_cornucopia",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    // ============ 中国神话圣物 ============
    
    /** 
     * 昆仑镜 (中国神话圣物)
     * 稀有度：史诗 | 主题：洞察与预知
     * 手持效果：夜视 + 发光 | 被动效果：每20秒获得经验
     * 神殿效果：10格范围内所有玩家获得夜视和发光效果
     */
    public static final Item RELIC_KUNLUN_MIRROR = registerItem("relic_kunlun_mirror",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
            
    /** 
     * 女娲石 (中国神话圣物)
     * 稀有度：传奇 | 主题：创造与修复
     * 手持效果：再生 II + 抗性 I | 被动效果：每30秒修复装备耐久
     * 神殿效果：15格范围内持续治愈所有友好生物
     */
    public static final Item RELIC_NUWA_STONE = registerItem("relic_nuwa_stone",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
            
    /** 
     * 神农鼎 (中国神话圣物)
     * 稀有度：稀有 | 主题：炼制与治愈
     * 手持效果：饱和 I + 抗毒 | 被动效果：每25秒生成药水材料
     * 神殿效果：8格范围内清除负面效果并提供抗毒
     */
    public static final Item RELIC_SHENNONG_CAULDRON = registerItem("relic_shennong_cauldron",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));
            
    /** 
     * 后羿弓 (中国神话圣物)
     * 稀有度：史诗 | 主题：精准与力量
     * 手持效果：力量 I + 急迫 I | 被动效果：弓箭伤害提升
     * 神殿效果：12格范围内提供力量和急迫效果
     */
    public static final Item RELIC_HOUYI_BOW = registerItem("relic_houyi_bow",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));

    // ============ 埃及神话圣物 ============
    
    /** 
     * 荷鲁斯之眼 (埃及神话圣物)
     * 稀有度：史诗 | 主题：保护与洞察
     * 手持效果：夜视 + 抗性 I | 被动效果：每15秒获得经验
     * 神殿效果：10格范围内提供保护和夜视效果
     */
    public static final Item RELIC_EYE_OF_HORUS = registerItem("relic_eye_of_horus",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
            
    /** 
     * 伊西斯的安卡 (埃及神话圣物)
     * 稀有度：传奇 | 主题：生命与复活
     * 手持效果：再生 II + 生命提升 I | 被动效果：死亡时有概率复活
     * 神殿效果：15格范围内持续治愈并提供生命提升
     */
    public static final Item RELIC_ANKH_OF_ISIS = registerItem("relic_ankh_of_isis",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
            
    /** 
     * 阿努比斯权杖 (埃及神话圣物)
     * 稀有度：史诗 | 主题：死亡与审判
     * 手持效果：力量 I + 凋零抗性 | 被动效果：击杀敌人获得额外经验
     * 神殿效果：12格范围内对亡灵生物造成额外伤害
     */
    public static final Item RELIC_ANUBIS_STAFF = registerItem("relic_anubis_staff",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
            
    /** 
     * 拉神圣甲虫 (埃及神话圣物)
     * 稀有度：稀有 | 主题：太阳与重生
     * 手持效果：发光 + 火焰保护 I | 被动效果：白天时获得速度提升
     * 神殿效果：8格范围内提供发光和火焰保护
     */
    public static final Item RELIC_SCARAB_OF_RA = registerItem("relic_scarab_of_ra",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    /** 
     * 奥丁之眼 (北欧神话圣物)
     * 稀有度：史诗 | 主题：智慧与预知
     * 手持效果：夜视 + 水下呼吸 | 被动效果：每10秒获得经验
     * 神殿效果：12格范围内提供智慧效果（经验获取翻倍）
     */
    public static final Item RELIC_ODIN_EYE = registerItem("relic_odin_eye",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
            
    /** 
     * 芙蕾雅项链 (北欧神话圣物)
     * 稀有度：稀有 | 主题：美丽与魅力
     * 手持效果：幸运 I + 魅力（村民交易折扣）| 被动效果：每20秒生成宝石
     * 神殿效果：10格范围内提供幸运效果
     */
    public static final Item RELIC_FREYJA_NECKLACE = registerItem("relic_freyja_necklace",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));
            
    /** 
     * 洛基面具 (北欧神话圣物)
     * 稀有度：史诗 | 主题：变化与诡计
     * 手持效果：隐身 I + 速度 I | 被动效果：潜行时消耗更少饥饿
     * 神殿效果：8格范围内提供隐身和速度效果
     */
    public static final Item RELIC_LOKI_MASK = registerItem("relic_loki_mask",
            new RelicItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));

    // ============ 神力证明系统 ============
    
    /** 
     * 希腊神力证明
     * 稀有度：不常见 | 堆叠：64 | 用途：在希腊神像处兑换希腊神明法术书
     * 代表获得了希腊奥林匹斯众神的认可，可用于兑换希腊神明的神力法术书
     */
    public static final Item GOD_POWER_CERTIFICATE_GREEK = registerItem("god_power_certificate_greek",
            new GodPowerCertificateItem(new FabricItemSettings().maxCount(64).rarity(Rarity.UNCOMMON), 
                                       GodPowerCertificateItem.MythologyType.GREEK));
    
    /** 
     * 埃及神力证明
     * 稀有度：不常见 | 堆叠：64 | 用途：在埃及神像处兑换埃及神明法术书
     * 代表获得了埃及法老王朝神明的祝福，可用于兑换埃及神明的神力法术书
     */
    public static final Item GOD_POWER_CERTIFICATE_EGYPTIAN = registerItem("god_power_certificate_egyptian",
            new GodPowerCertificateItem(new FabricItemSettings().maxCount(64).rarity(Rarity.UNCOMMON), 
                                       GodPowerCertificateItem.MythologyType.EGYPTIAN));
    
    /** 
     * 中国神力证明
     * 稀有度：不常见 | 堆叠：64 | 用途：在中国神像处兑换中国神明法术书
     * 代表获得了东方仙神的印证，可用于兑换中国神话的神力法术书
     */
    public static final Item GOD_POWER_CERTIFICATE_CHINESE = registerItem("god_power_certificate_chinese",
            new GodPowerCertificateItem(new FabricItemSettings().maxCount(64).rarity(Rarity.UNCOMMON), 
                                       GodPowerCertificateItem.MythologyType.CHINESE));
    
    /** 
     * 亚特兰蒂斯神力证明
     * 稀有度：不常见 | 堆叠：64 | 用途：在亚特兰蒂斯神像处兑换亚特兰蒂斯法术书
     * 代表获得了古代亚特兰蒂斯文明的智慧结晶，可用于兑换亚特兰蒂斯的神力法术书
     */
    public static final Item GOD_POWER_CERTIFICATE_ATLANTEAN = registerItem("god_power_certificate_atlantean",
            new GodPowerCertificateItem(new FabricItemSettings().maxCount(64).rarity(Rarity.UNCOMMON), 
                                       GodPowerCertificateItem.MythologyType.ATLANTEAN));
    
    /** 
     * 北欧神力证明
     * 稀有度：不常见 | 堆叠：64 | 用途：在北欧神像处兑换北欧神明法术书
     * 代表获得了阿萨神族的战士荣耀，可用于兑换北欧神话的神力法术书
     */
    public static final Item GOD_POWER_CERTIFICATE_NORDIC = registerItem("god_power_certificate_nordic",
            new GodPowerCertificateItem(new FabricItemSettings().maxCount(64).rarity(Rarity.UNCOMMON), 
                                       GodPowerCertificateItem.MythologyType.NORDIC));

    // ============ 埃及神像系统 ============
    
    /** 
     * 埃及奴隶神像 (1级)
     * 稀有度：常见 | 价值：最低 | 产出：最少神力证明
     * 简单的铜制雕像，朴素造型，适合初级神殿
     */
    public static final Item EGYPTIAN_STATUE_SLAVE = registerItem("egyptian_statue_slave",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.COMMON)));
    
    /** 
     * 埃及市民神像 (2级)
     * 稀有度：常见 | 价值：较低 | 产出：少量神力证明
     * 银制装饰雕像，中等精细度，代表普通民众
     */
    public static final Item EGYPTIAN_STATUE_CITIZEN = registerItem("egyptian_statue_citizen",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.COMMON)));
    
    /** 
     * 埃及士兵神像 (3级)
     * 稀有度：不常见 | 价值：中等 | 产出：中等神力证明
     * 青铜盔甲雕像，军事风格，展现武士精神
     */
    public static final Item EGYPTIAN_STATUE_SOLDIER = registerItem("egyptian_statue_soldier",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON)));
    
    /** 
     * 埃及王后神像 (4级)
     * 稀有度：稀有 | 价值：较高 | 产出：较多神力证明
     * 金色头饰雕像，华丽女性造型，彰显王室威严
     */
    public static final Item EGYPTIAN_STATUE_QUEEN = registerItem("egyptian_statue_queen",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));
    
    /** 
     * 埃及国王神像 (5级)
     * 稀有度：稀有 | 价值：高 | 产出：大量神力证明
     * 法老面具雕像，威严男性造型，象征王权神授
     */
    public static final Item EGYPTIAN_STATUE_KING = registerItem("egyptian_statue_king",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));
    
    /** 
     * 埃及王室夫妇神像 (6级)
     * 稀有度：史诗 | 价值：很高 | 产出：大量神力证明
     * 双人法老雕像，展现王室和谐，象征国家繁荣
     */
    public static final Item EGYPTIAN_STATUE_ROYAL_COUPLE = registerItem("egyptian_statue_royal_couple",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));
    
    /** 
     * 埃及法老神像 (7级)
     * 稀有度：史诗 | 价值：最高 | 产出：最多神力证明
     * 华丽黄金法老雕像，最高级别造型，代表神圣王权
     */
    public static final Item EGYPTIAN_STATUE_PHARAOH = registerItem("egyptian_statue_pharaoh",
            new Item(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));

    // ============ 神力指南系统 ============
    
    /** 
     * 神力指南
     * 稀有度：不常见 | 堆叠：1 | 用途：查看神力系统详细信息
     * 神力系统的完整说明书，包含所有神力相关功能的详细介绍
     */
    public static final Item GOD_POWER_GUIDE = registerItem("god_power_guide",
            new GodPowerGuideItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON)));

    // ============ 怪物蛋系统 ============
    
    /** 
     * 米诺陶怪物蛋
     * 稀有度：稀有 | 堆叠：64 | 用途：生成米诺陶实体
     * 希腊神话中的牛头人身怪物，强大的敌对生物，具有冲锋攻击能力
     */
    public static final Item MINOTAUR_SPAWN_EGG = registerItem("minotaur_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.MINOTAUR, 
                                               0x8B4513, 0xD2691E, // 棕色主体，橙棕色斑点
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item CYCLOPS_SPAWN_EGG = registerItem("cyclops_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CYCLOPS,
                                               0x6B6157, 0xD7C39A,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item CENTAUR_SPAWN_EGG = registerItem("centaur_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CENTAUR,
                                               0x8B5A2B, 0xD9B382,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item HYDRA_SPAWN_EGG = registerItem("hydra_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.HYDRA,
                                               0x234A2E, 0x78B356,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item MEDUSA_SPAWN_EGG = registerItem("medusa_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.MEDUSA,
                                               0x2C5B46, 0xB8B8B8,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item NEMEAN_LION_SPAWN_EGG = registerItem("nemean_lion_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.NEMEAN_LION,
                                               0xC89032, 0xF3D27A,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item MANTICORE_SPAWN_EGG = registerItem("manticore_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.MANTICORE,
                                               0x8B3E3A, 0x2F5B3F,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item CHIMERA_SPAWN_EGG = registerItem("chimera_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CHIMERA,
                                               0x4A2C22, 0xD65A31,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item COLOSSUS_SPAWN_EGG = registerItem("colossus_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.COLOSSUS,
                                               0x7B5A3A, 0x2DBA9D,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item PEGASUS_SPAWN_EGG = registerItem("pegasus_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.PEGASUS,
                                               0xF7F7F0, 0xCFEAFF,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item ANUBIS_WARRIOR_SPAWN_EGG = registerItem("anubis_warrior_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.ANUBIS_WARRIOR,
                                               0x3B2B22, 0xD8B45C,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item SCARAB_GUARDIAN_SPAWN_EGG = registerItem("scarab_guardian_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.SCARAB_GUARDIAN,
                                               0x1F4A3F, 0xD2A94B,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item MUMMY_SOLDIER_SPAWN_EGG = registerItem("mummy_soldier_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.MUMMY_SOLDIER,
                                               0xB8A77E, 0x4A3F2B,
                                               new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    public static final Item SPHINX_SENTINEL_SPAWN_EGG = registerItem("sphinx_sentinel_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.SPHINX_SENTINEL,
                                               0xC79A4A, 0x315F7D,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item SUN_FALCON_SPAWN_EGG = registerItem("sun_falcon_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.SUN_FALCON,
                                               0x8F4B22, 0xF2C14E,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item FROST_GIANT_SPAWN_EGG = registerItem("frost_giant_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.FROST_GIANT,
                                               0x8FB8D8, 0xE6F6FF,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item DRAUGR_WARRIOR_SPAWN_EGG = registerItem("draugr_warrior_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.DRAUGR_WARRIOR,
                                               0x4F5A5B, 0x9AA7A8,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item VALKYRIE_SPAWN_EGG = registerItem("valkyrie_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.VALKYRIE,
                                               0xD8D7CF, 0xD7B65A,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item FENRIR_WOLF_SPAWN_EGG = registerItem("fenrir_wolf_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.FENRIR_WOLF,
                                               0x1D2028, 0xB02A2A,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item DWARF_BERSERKER_SPAWN_EGG = registerItem("dwarf_berserker_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.DWARF_BERSERKER,
                                               0x7B4C2A, 0xC5A15C,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item AZURE_DRAGON_SPAWN_EGG = registerItem("azure_dragon_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.AZURE_DRAGON,
                                               0x2F8F8A, 0xD8B65A,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item QILIN_GUARDIAN_SPAWN_EGG = registerItem("qilin_guardian_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.QILIN_GUARDIAN,
                                               0xB84A36, 0xD9B84A,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item JIANGSHI_SPAWN_EGG = registerItem("jiangshi_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.JIANGSHI,
                                               0x46605F, 0xD6B64A,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item NINE_TAILED_FOX_SPAWN_EGG = registerItem("nine_tailed_fox_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.NINE_TAILED_FOX,
                                               0xF0D2A2, 0xE84A5F,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item STONE_LION_GUARDIAN_SPAWN_EGG = registerItem("stone_lion_guardian_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.STONE_LION_GUARDIAN,
                                               0x8F8A7A, 0xB93A32,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item CRYSTAL_LEVIATHAN_SPAWN_EGG = registerItem("crystal_leviathan_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CRYSTAL_LEVIATHAN,
                                               0x1B6F7A, 0x72E6F2,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item ORICHALCUM_GOLEM_SPAWN_EGG = registerItem("orichalcum_golem_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.ORICHALCUM_GOLEM,
                                               0x9A6C38, 0x36D6C9,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item SIREN_PRIESTESS_SPAWN_EGG = registerItem("siren_priestess_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.SIREN_PRIESTESS,
                                               0x2C8F9E, 0xD7F7FF,
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    public static final Item TIDAL_GUARDIAN_SPAWN_EGG = registerItem("tidal_guardian_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.TIDAL_GUARDIAN,
                                               0x2F5D62, 0x48D1CC,
                                               new FabricItemSettings().rarity(Rarity.EPIC)));

    public static final Item ABYSSAL_RAY_SPAWN_EGG = registerItem("abyssal_ray_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.ABYSSAL_RAY,
                                               0x183F5A, 0x7DE7FF,
                                               new FabricItemSettings().rarity(Rarity.RARE)));
    
    /** 
     * 希腊神殿守护者刷怪蛋
     * 稀有度：不常见 | 堆叠：64 | 颜色主题：蓝色（智慧）+ 金色（神圣）
     * 希腊神话村庄中的神殿守护者，提供希腊神力相关服务
     */
    public static final Item TEMPLE_GUARDIAN_GREEK_SPAWN_EGG = registerItem("temple_guardian_greek_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.TEMPLE_GUARDIAN_GREEK, 
                                               0x4169E1, 0xFFD700, // 蓝色主体，金色斑点
                                               new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    /** 
     * 埃及神殿守护者刷怪蛋
     * 稀有度：不常见 | 堆叠：64 | 颜色主题：金色（沙漠）+ 青绿色（尼罗河）
     * 埃及神话村庄中的神殿守护者，提供埃及神力相关服务
     */
    public static final Item TEMPLE_GUARDIAN_EGYPTIAN_SPAWN_EGG = registerItem("temple_guardian_egyptian_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.TEMPLE_GUARDIAN_EGYPTIAN, 
                                               0xDAA520, 0x20B2AA, // 金色主体，青绿色斑点
                                               new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    /** 
     * 中国神殿守护者刷怪蛋
     * 稀有度：不常见 | 堆叠：64 | 颜色主题：红色（吉祥）+ 金色（富贵）
     * 中国神话村庄中的神殿守护者，提供中国神力相关服务
     */
    public static final Item TEMPLE_GUARDIAN_CHINESE_SPAWN_EGG = registerItem("temple_guardian_chinese_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.TEMPLE_GUARDIAN_CHINESE, 
                                               0xDC143C, 0xFFD700, // 红色主体，金色斑点
                                               new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    /** 
     * 亚特兰蒂斯神殿守护者刷怪蛋
     * 稀有度：不常见 | 堆叠：64 | 颜色主题：青蓝色（海洋）+ 薄荷绿（水晶）
     * 亚特兰蒂斯神话村庄中的神殿守护者，提供亚特兰蒂斯神力相关服务
     */
    public static final Item TEMPLE_GUARDIAN_ATLANTEAN_SPAWN_EGG = registerItem("temple_guardian_atlantean_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.TEMPLE_GUARDIAN_ATLANTEAN, 
                                               0x00CED1, 0x98FB98, // 青蓝色主体，薄荷绿斑点
                                               new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    /** 
     * 北欧神殿守护者刷怪蛋
     * 稀有度：不常见 | 堆叠：64 | 颜色主题：灰色（石头）+ 银色（钢铁）
     * 北欧神话村庄中的神殿守护者，提供北欧神力相关服务
     */
    public static final Item TEMPLE_GUARDIAN_NORDIC_SPAWN_EGG = registerItem("temple_guardian_nordic_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.TEMPLE_GUARDIAN_NORDIC, 
                                               0x708090, 0xC0C0C0, // 灰色主体，银色斑点
                                               new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    /** 
     * 通用神殿守护者刷怪蛋（保留兼容性）
     * @deprecated 使用特定文明的神殿守护者刷怪蛋
     */
    @Deprecated
    public static final Item TEMPLE_GUARDIAN_SPAWN_EGG = registerItem("temple_guardian_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.TEMPLE_GUARDIAN, 
                                               0x4169E1, 0xFFD700, // 蓝色主体，金色斑点
                                               new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    // ============ 自定义神殿守护者刷怪蛋系统 ============
    
    /** 
     * 自定义希腊神殿守护者刷怪蛋
     * 稀有度：稀有 | 堆叠：64 | 颜色主题：深蓝色（海洋）+ 白色（大理石）
     * 使用完全自定义交易系统的希腊神殿守护者，不受原版村民限制
     */
    public static final Item CUSTOM_TEMPLE_GUARDIAN_GREEK_SPAWN_EGG = registerItem("custom_temple_guardian_greek_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CUSTOM_TEMPLE_GUARDIAN_GREEK, 
                                               0x191970, 0xF5F5DC, // 深蓝色主体，米白色斑点
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    /** 
     * 自定义埃及神殿守护者刷怪蛋
     * 稀有度：稀有 | 堆叠：64 | 颜色主题：沙黄色（沙漠）+ 深金色（法老）
     * 使用完全自定义交易系统的埃及神殿守护者，不受原版村民限制
     */
    public static final Item CUSTOM_TEMPLE_GUARDIAN_EGYPTIAN_SPAWN_EGG = registerItem("custom_temple_guardian_egyptian_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CUSTOM_TEMPLE_GUARDIAN_EGYPTIAN, 
                                               0xF4A460, 0xB8860B, // 沙黄色主体，深金色斑点
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    /** 
     * 自定义中国神殿守护者刷怪蛋
     * 稀有度：稀有 | 堆叠：64 | 颜色主题：深红色（传统）+ 黄色（皇家）
     * 使用完全自定义交易系统的中国神殿守护者，不受原版村民限制
     */
    public static final Item CUSTOM_TEMPLE_GUARDIAN_CHINESE_SPAWN_EGG = registerItem("custom_temple_guardian_chinese_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CUSTOM_TEMPLE_GUARDIAN_CHINESE, 
                                               0x8B0000, 0xFFD700, // 深红色主体，金色斑点
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    /** 
     * 自定义亚特兰蒂斯神殿守护者刷怪蛋
     * 稀有度：稀有 | 堆叠：64 | 颜色主题：深青色（深海）+ 青绿色（水晶）
     * 使用完全自定义交易系统的亚特兰蒂斯神殿守护者，不受原版村民限制
     */
    public static final Item CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN_SPAWN_EGG = registerItem("custom_temple_guardian_atlantean_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CUSTOM_TEMPLE_GUARDIAN_ATLANTEAN, 
                                               0x008B8B, 0x40E0D0, // 深青色主体，青绿色斑点
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    /** 
     * 自定义北欧神殿守护者刷怪蛋
     * 稀有度：稀有 | 堆叠：64 | 颜色主题：深灰色（钢铁）+ 亮银色（武器）
     * 使用完全自定义交易系统的北欧神殿守护者，不受原版村民限制
     */
    public static final Item CUSTOM_TEMPLE_GUARDIAN_NORDIC_SPAWN_EGG = registerItem("custom_temple_guardian_nordic_spawn_egg",
            new net.minecraft.item.SpawnEggItem(com.example.mythic_age.entity.ModEntities.CUSTOM_TEMPLE_GUARDIAN_NORDIC, 
                                               0x2F4F4F, 0xC0C0C0, // 深灰色主体，银色斑点
                                               new FabricItemSettings().rarity(Rarity.RARE)));

    // ============ 神殿生成法术书系统 (测试用) ============
    
    /** 
     * 埃及神殿生成法术书 (测试用)
     * 稀有度：史诗 | 冷却：1秒 | 效果：生成埃及神殿
     * 在玩家前方生成完整的埃及风格神殿，包含守护者
     */
    public static final Item SPELLBOOK_GENERATE_EGYPTIAN_TEMPLE = registerItem("spellbook_generate_egyptian_temple",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             20, SpellbookItem.SpellType.GENERATE_EGYPTIAN_TEMPLE));
    
    /** 
     * 中国神殿生成法术书 (测试用)
     * 稀有度：史诗 | 冷却：1秒 | 效果：生成中国神殿
     * 在玩家前方生成完整的中国风格神殿，包含守护者
     */
    public static final Item SPELLBOOK_GENERATE_CHINESE_TEMPLE = registerItem("spellbook_generate_chinese_temple",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             20, SpellbookItem.SpellType.GENERATE_CHINESE_TEMPLE));
    
    /** 
     * 亚特兰蒂斯神殿生成法术书 (测试用)
     * 稀有度：史诗 | 冷却：1秒 | 效果：生成亚特兰蒂斯神殿
     * 在玩家前方生成完整的亚特兰蒂斯风格神殿，包含守护者和发光效果
     */
    public static final Item SPELLBOOK_GENERATE_ATLANTEAN_TEMPLE = registerItem("spellbook_generate_atlantean_temple",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             20, SpellbookItem.SpellType.GENERATE_ATLANTEAN_TEMPLE));
    
    /** 
     * 北欧神殿生成法术书 (测试用)
     * 稀有度：史诗 | 冷却：1秒 | 效果：生成北欧神殿
     * 在玩家前方生成完整的北欧风格神殿，包含守护者
     */
    public static final Item SPELLBOOK_GENERATE_NORDIC_TEMPLE = registerItem("spellbook_generate_nordic_temple",
            new SpellbookItem(new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC), 
                             20, SpellbookItem.SpellType.GENERATE_NORDIC_TEMPLE));

    // ============ 辅助方法 ============
    
    /**
     * 物品注册辅助方法
     * 
     * 将物品注册到Minecraft的物品注册表中，使用Mod的命名空间。
     * 每个物品都会获得一个唯一的标识符，格式为 "mythic_age:物品名称"
     * 
     * @param name 物品的注册名称，必须是小写字母和下划线
     * @param item 要注册的物品实例
     * @return 注册后的物品实例
     */
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MythicAgeMod.MOD_ID, name), item);
    }

    /**
     * 物品注册主方法
     * 
     * 在Mod初始化时调用，负责将所有定义的物品注册到游戏中。
     * 由于使用了静态初始化器，所有物品在类加载时就会自动注册。
     * 
     * 注册的物品包括：
     * - 16种神力法术书：基础4种 + 高级8种 + 神殿生成4种
     * - 5种神话圣物：拉神安卡、阿瑞斯头盔、雷神锤碎片、金羊毛、丰饶角
     * - 5种神力证明：希腊、埃及、中国、亚特兰蒂斯、北欧
     * - 多种刷怪蛋和建筑方块
     * 
     * 所有物品都会在创造模式物品栏中可用，并可通过战利品表在世界中获得。
     */
    public static void registerModItems() {
        MythicAgeMod.LOGGER.info("Registering Mod Items for " + MythicAgeMod.MOD_ID);
        // 静态初始化器会在类加载时自动处理物品注册
        // 这里主要用于日志记录和确保类被正确加载
        MythicAgeMod.LOGGER.info("Successfully registered " + 
            "56 spellbooks (12 Greek + 4 Temple Generation + 10 Chinese + 10 Egyptian + 10 Nordic + 10 Atlantean), 5 relics, 5 god power certificates, and multiple spawn eggs for Mythic Age Mod");
    }
}
