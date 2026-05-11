package com.example.mythic_age.worldgen;

import com.example.mythic_age.item.GodPowerCertificateItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.*;

/**
 * 生物群系文明类型映射器
 * 
 * 根据生物群系判断该区域可以生成哪些文明类型，
 * 然后通过随机数选择一个文明类型。
 * 
 * 文明类型与生物群系的对应关系：
 * - 希腊：山地、丘陵、针叶林等地形
 * - 埃及：沙漠、恶地等干旱地形
 * - 中国：平原、草甸、热带草原等温带地形
 * - 北欧：雪地、冰原、冻土等寒冷地形
 * - 亚特兰蒂斯：海洋、海滩、蘑菇岛等水域地形
 */
public class BiomeCivilizationMapper {
    
    // 希腊文明适合的生物群系
    private static final Set<String> GREEK_BIOMES = Set.of(
        "minecraft:taiga",
        "minecraft:old_growth_pine_taiga", 
        "minecraft:old_growth_spruce_taiga",
        "minecraft:windswept_hills",
        "minecraft:windswept_forest",
        "minecraft:windswept_gravelly_hills",
        "minecraft:grove",
        "minecraft:jagged_peaks",
        "minecraft:stony_peaks",
        "minecraft:forest",
        "minecraft:birch_forest",
        "minecraft:dark_forest"
    );
    
    // 埃及文明适合的生物群系
    private static final Set<String> EGYPTIAN_BIOMES = Set.of(
        "minecraft:desert",
        "minecraft:badlands",
        "minecraft:eroded_badlands", 
        "minecraft:wooded_badlands",
        "minecraft:warm_ocean"
    );
    
    // 中国文明适合的生物群系
    private static final Set<String> CHINESE_BIOMES = Set.of(
        "minecraft:plains",
        "minecraft:savanna",
        "minecraft:savanna_plateau",
        "minecraft:windswept_savanna", 
        "minecraft:meadow",
        "minecraft:sunflower_plains",
        "minecraft:flower_forest"
    );
    
    // 北欧文明适合的生物群系
    private static final Set<String> NORDIC_BIOMES = Set.of(
        "minecraft:snowy_plains",
        "minecraft:snowy_taiga",
        "minecraft:frozen_river",
        "minecraft:ice_spikes",
        "minecraft:snowy_slopes",
        "minecraft:frozen_peaks",
        "minecraft:snowy_beach"
    );
    
    // 亚特兰蒂斯文明适合的生物群系
    private static final Set<String> ATLANTEAN_BIOMES = Set.of(
        "minecraft:ocean",
        "minecraft:deep_ocean",
        "minecraft:lukewarm_ocean",
        "minecraft:deep_lukewarm_ocean",
        "minecraft:cold_ocean",
        "minecraft:deep_cold_ocean",
        "minecraft:beach",
        "minecraft:stony_shore",
        "minecraft:mushroom_fields",
        "minecraft:river"
    );
    
    // 生物群系到文明类型的映射
    private static final Map<String, List<MythicVillageTypes.VillageType>> BIOME_TO_CIVILIZATIONS = new HashMap<>();
    
    static {
        // 初始化映射关系
        for (String biome : GREEK_BIOMES) {
            BIOME_TO_CIVILIZATIONS.computeIfAbsent(biome, k -> new ArrayList<>())
                .add(MythicVillageTypes.VillageType.GREEK);
        }
        
        for (String biome : EGYPTIAN_BIOMES) {
            BIOME_TO_CIVILIZATIONS.computeIfAbsent(biome, k -> new ArrayList<>())
                .add(MythicVillageTypes.VillageType.EGYPTIAN);
        }
        
        for (String biome : CHINESE_BIOMES) {
            BIOME_TO_CIVILIZATIONS.computeIfAbsent(biome, k -> new ArrayList<>())
                .add(MythicVillageTypes.VillageType.CHINESE);
        }
        
        for (String biome : NORDIC_BIOMES) {
            BIOME_TO_CIVILIZATIONS.computeIfAbsent(biome, k -> new ArrayList<>())
                .add(MythicVillageTypes.VillageType.NORDIC);
        }
        
        for (String biome : ATLANTEAN_BIOMES) {
            BIOME_TO_CIVILIZATIONS.computeIfAbsent(biome, k -> new ArrayList<>())
                .add(MythicVillageTypes.VillageType.ATLANTEAN);
        }
    }
    
    /**
     * 根据生物群系获取可能的文明类型列表
     * 
     * @param biome 生物群系
     * @return 可能的文明类型列表，如果该生物群系不支持任何文明则返回空列表
     */
    public static List<MythicVillageTypes.VillageType> getPossibleCivilizations(RegistryEntry<Biome> biome) {
        if (biome == null || !biome.getKey().isPresent()) {
            return Collections.emptyList();
        }
        
        Identifier biomeId = biome.getKey().get().getValue();
        String biomeName = biomeId.toString();
        
        return BIOME_TO_CIVILIZATIONS.getOrDefault(biomeName, Collections.emptyList());
    }
    
    /**
     * 根据生物群系名称获取可能的文明类型列表
     * 
     * @param biomeName 生物群系名称
     * @return 可能的文明类型列表
     */
    public static List<MythicVillageTypes.VillageType> getPossibleCivilizations(String biomeName) {
        return BIOME_TO_CIVILIZATIONS.getOrDefault(biomeName, Collections.emptyList());
    }
    
    /**
     * 根据生物群系随机选择一个文明类型
     * 
     * @param biome 生物群系
     * @param random 随机数生成器
     * @return 随机选择的文明类型，如果该生物群系不支持任何文明则返回null
     */
    public static MythicVillageTypes.VillageType getRandomCivilization(RegistryEntry<Biome> biome, Random random) {
        List<MythicVillageTypes.VillageType> possibleCivilizations = getPossibleCivilizations(biome);
        
        if (possibleCivilizations.isEmpty()) {
            return null;
        }
        
        return possibleCivilizations.get(random.nextInt(possibleCivilizations.size()));
    }
    
    /**
     * 根据生物群系名称随机选择一个文明类型
     * 
     * @param biomeName 生物群系名称
     * @param random 随机数生成器
     * @return 随机选择的文明类型，如果该生物群系不支持任何文明则返回null
     */
    public static MythicVillageTypes.VillageType getRandomCivilization(String biomeName, Random random) {
        List<MythicVillageTypes.VillageType> possibleCivilizations = getPossibleCivilizations(biomeName);
        
        if (possibleCivilizations.isEmpty()) {
            return null;
        }
        
        return possibleCivilizations.get(random.nextInt(possibleCivilizations.size()));
    }
    
    /**
     * 检查生物群系是否支持指定的文明类型
     * 
     * @param biome 生物群系
     * @param civilizationType 文明类型
     * @return 是否支持
     */
    public static boolean supportsCivilization(RegistryEntry<Biome> biome, MythicVillageTypes.VillageType civilizationType) {
        List<MythicVillageTypes.VillageType> possibleCivilizations = getPossibleCivilizations(biome);
        return possibleCivilizations.contains(civilizationType);
    }
    
    /**
     * 获取所有支持神话文明的生物群系
     * 
     * @return 支持神话文明的生物群系集合
     */
    public static Set<String> getAllSupportedBiomes() {
        return BIOME_TO_CIVILIZATIONS.keySet();
    }
    
    /**
     * 打印调试信息
     */
    public static void printDebugInfo() {
        System.out.println("[生物群系文明映射] 支持的生物群系和文明类型:");
        for (Map.Entry<String, List<MythicVillageTypes.VillageType>> entry : BIOME_TO_CIVILIZATIONS.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
    }
}