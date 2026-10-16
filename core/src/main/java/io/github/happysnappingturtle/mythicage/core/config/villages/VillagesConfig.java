package io.github.happysnappingturtle.mythicage.core.config.villages;

import java.util.Map;

/**
 * {@code villages.json}：聚落归属（T10a 设计文档 §8；17 号定案 §4.3）。
 *
 * @param vanillaVillages       原版村庄是否自动归属文明
 * @param vanillaTypeToCiv      原版村庄类型（plains / desert / savanna / taiga / snowy）→ 文明权重表；单一文明写成 {civ: 1}
 * @param replaceVanillaInRegion 文明村庄结构上线后，在该文明主群系里取代原版村庄生成（T13b 起生效）
 * @param patronWeights         文明 → 主神权重表；没有条目的文明，村庄登记时主神为空
 * @param coastalPoseidonBonus  沿海村庄的波塞冬权重倍率
 * @param titleOnEnter          进入村庄时显示标题
 */
public record VillagesConfig(Mode vanillaVillages, Map<String, Map<String, Integer>> vanillaTypeToCiv,
                             boolean replaceVanillaInRegion, Map<String, Map<String, Integer>> patronWeights,
                             double coastalPoseidonBonus, boolean titleOnEnter) {
    public VillagesConfig {
        vanillaTypeToCiv = Map.copyOf(vanillaTypeToCiv);
        patronWeights = Map.copyOf(patronWeights);
    }

    public enum Mode { AFFILIATE, NEUTRAL }

    /** 原版村庄类型。 */
    public static final java.util.List<String> VANILLA_TYPES = java.util.List.of("plains", "desert", "savanna", "taiga", "snowy");
}
