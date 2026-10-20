package io.github.happysnappingturtle.mythicage.core.village.gen;

import io.github.happysnappingturtle.mythicage.core.village.VillageRules;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 文明选择（V0 §3.3）：按顺序判断，第一条满足的生效；只做群系采样，不读方块。 */
public final class CivSelector {
    public static final String RIVER = "#minecraft:is_river";
    private static final long SALT = 0x52_55_4C_45L;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private CivSelector() {
    }

    /**
     * @param withTemplate 已有正式模板的文明
     * @param chances      配置里的概率（键见 {@link CivRule#chanceKey()}）
     */
    public static Optional<String> select(List<CivRule> rules, BiomeView biomes, int x, int z, Set<String> withTemplate,
                                          Map<String, Double> chances) {
        for (int i = 0; i < rules.size(); i++) {
            CivRule r = rules.get(i);
            if (!r.requiresTemplate().isEmpty() && !withTemplate.contains(r.requiresTemplate())) {
                continue;
            }
            if (!r.region().isEmpty() && !biomes.matches(r.region(), x, z)) {
                continue;
            }
            if (r.notRegions().stream().anyMatch(n -> biomes.matches(n, x, z))) {
                continue;
            }
            if (r.nearRiver() > 0 && !nearRiver(biomes, x, z, r.nearRiver())) {
                continue;
            }
            double p = r.chanceKey().isEmpty() ? r.chance() : chances.getOrDefault(r.chanceKey(), r.chance());
            if (p < 1.0 && VillageRules.unit(x, 0, z, SALT + i) >= p) {
                continue;
            }
            return Optional.of(r.civ());
        }
        return Optional.empty();
    }

    static boolean nearRiver(BiomeView biomes, int x, int z, int r) {
        if (biomes.matches(RIVER, x, z)) {
            return true;
        }
        for (int[] d : DIRS) {
            if (biomes.matches(RIVER, x + d[0] * r, z + d[1] * r)) {
                return true;
            }
        }
        return false;
    }
}
