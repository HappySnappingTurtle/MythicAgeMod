package io.github.happysnappingturtle.mythicage.core.config.villages;

import java.util.List;
import java.util.Map;

/**
 * {@code villages.json}：聚落归属（T10a 设计文档 §8；17 号定案 §4.3；T10b 设计文档 §7 升为第 2 版）。
 *
 * @param vanillaVillages        原版村庄是否自动归属文明
 * @param vanillaTypeToCiv       原版村庄类型（plains / desert / savanna / taiga / snowy）→ 文明权重表；单一文明写成 {civ: 1}
 * @param replaceVanillaInRegion 文明村庄结构上线后，在该文明主群系里取代原版村庄生成（T13b 起生效）
 * @param patronWeights          文明 → 主神权重表；没有条目的文明，村庄登记时主神为空，有了条目后补抽
 * @param patronBiomeBonus       文明 → 主神 → 群系加成：村庄中心附近有指定群系时该主神权重乘以倍率
 * @param titleOnEnter           进入村庄时显示标题
 */
public record VillagesConfig(Mode vanillaVillages, Map<String, Map<String, Integer>> vanillaTypeToCiv,
                             boolean replaceVanillaInRegion, Map<String, Map<String, Integer>> patronWeights,
                             Map<String, Map<String, BiomeBonus>> patronBiomeBonus, boolean titleOnEnter) {
    public VillagesConfig {
        vanillaTypeToCiv = Map.copyOf(vanillaTypeToCiv);
        patronWeights = Map.copyOf(patronWeights);
        patronBiomeBonus = Map.copyOf(patronBiomeBonus);
    }

    public enum Mode { AFFILIATE, NEUTRAL }

    /**
     * @param biomes     群系 id（{@code minecraft:beach}）或群系标签（{@code #minecraft:is_ocean}）
     * @param radius     采样半径：中心与 8 个方向各取一点
     * @param multiplier 命中时的权重倍率
     */
    public record BiomeBonus(List<String> biomes, int radius, double multiplier) {
        public BiomeBonus {
            biomes = List.copyOf(biomes);
        }
    }

    /** 某文明的群系加成；没有时为空表。 */
    public Map<String, BiomeBonus> biomeBonus(String civ) {
        return patronBiomeBonus.getOrDefault(civ, Map.of());
    }

    /** 原版村庄类型。 */
    public static final List<String> VANILLA_TYPES = List.of("plains", "desert", "savanna", "taiga", "snowy");
}
