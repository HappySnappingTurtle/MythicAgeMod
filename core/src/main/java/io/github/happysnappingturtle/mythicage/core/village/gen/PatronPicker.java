package io.github.happysnappingturtle.mythicage.core.village.gen;

import io.github.happysnappingturtle.mythicage.core.config.villages.VillagesConfig;
import io.github.happysnappingturtle.mythicage.core.village.VillageRules;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生成时抽主神（V0 W6）：规则与 {@code VillageService.pickPatron} 相同——权重 × 群系加成（中心与 8 个方向采样），
 * 坐标哈希用聚会钟的位置，所以日后登记时补抽也会得到同一个结果。
 */
public final class PatronPicker {
    private PatronPicker() {
    }

    public static String pick(VillagesConfig cfg, String civ, BiomeView biomes, int x, int y, int z) {
        Map<String, Integer> pw = cfg.patronWeights().get(civ);
        if (pw == null || pw.isEmpty()) {
            return "";
        }
        Map<String, Double> matched = new LinkedHashMap<>();
        cfg.biomeBonus(civ).forEach((patron, b) -> {
            if (matches(biomes, x, z, b)) {
                matched.put(patron, b.multiplier());
            }
        });
        return VillageRules.pick(VillageRules.biomeBonus(pw, matched), VillageRules.unit(x, y, z, VillageRules.SALT_PATRON))
                .orElse("");
    }

    static boolean matches(BiomeView biomes, int x, int z, VillagesConfig.BiomeBonus b) {
        int r = b.radius();
        int d = (int) Math.round(r * 0.7071);
        int[][] dirs = {{0, 0}, {r, 0}, {-r, 0}, {0, r}, {0, -r}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
        for (int[] off : dirs) {
            for (String id : b.biomes()) {
                if (biomes.matches(id, x + off[0], z + off[1])) {
                    return true;
                }
            }
        }
        return false;
    }
}
