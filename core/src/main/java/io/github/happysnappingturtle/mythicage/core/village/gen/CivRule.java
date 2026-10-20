package io.github.happysnappingturtle.mythicage.core.village.gen;

import java.util.List;

/**
 * 文明选择表的一条（V0 §3.3）。条件全部满足才命中：
 *
 * @param civ              命中时出的文明
 * @param region           中心所在群系须属于该标签 / id；空串不限
 * @param notRegions       中心群系不能属于这些标签 / id
 * @param nearRiver        &gt; 0 时，中心或周围 8 个方向这么远处至少一处是河流群系
 * @param chance           命中后的概率；配置键 {@code chanceKey} 不为空时以配置值为准
 * @param chanceKey        概率读哪个配置项（如 {@code chineseRiverChance}）；空串用 {@code chance}
 * @param requiresTemplate 该文明须已有正式模板，否则整条跳过；空串不要求
 */
public record CivRule(String civ, String region, List<String> notRegions, int nearRiver, double chance,
                      String chanceKey, String requiresTemplate) {
    public CivRule {
        notRegions = List.copyOf(notRegions);
    }

    public static CivRule simple(String civ, String region) {
        return new CivRule(civ, region, List.of(), 0, 1.0, "", "");
    }
}
