package io.github.happysnappingturtle.mythicage.core.village;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 村庄归属的纯规则（T10a 设计文档 §2）：用坐标哈希做确定性加权抽取——同一世界同一位置，重复登记结果一致。
 * 权重表按键排序后抽取，不依赖 Map 的迭代顺序。
 */
public final class VillageRules {
    private VillageRules() {
    }

    /** 坐标 + 用途盐 → [0, 1) 的确定性值（splitmix64）。 */
    public static double unit(long x, long y, long z, long salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ salt * 0xD6E8FEB86659FD93L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** 按权重抽取一个键；表为空或总权重为 0 时为空。 */
    public static Optional<String> pick(Map<String, Integer> weights, double unit) {
        List<String> keys = new ArrayList<>(weights.keySet());
        keys.sort(String::compareTo);
        long total = 0;
        for (String k : keys) {
            total += Math.max(0, weights.get(k));
        }
        if (total <= 0) {
            return Optional.empty();
        }
        double target = unit * total;
        long acc = 0;
        for (String k : keys) {
            acc += Math.max(0, weights.get(k));
            if (target < acc) {
                return Optional.of(k);
            }
        }
        return Optional.of(keys.getLast());
    }

    /**
     * 群系加成（T10b 设计文档 §7）：命中的主神权重乘以各自的倍率（向下取整，至少 1）；
     * 不在权重表里的主神忽略。
     */
    public static Map<String, Integer> biomeBonus(Map<String, Integer> weights, Map<String, Double> matched) {
        if (matched.isEmpty()) {
            return weights;
        }
        Map<String, Integer> out = new LinkedHashMap<>(weights);
        matched.forEach((patron, mult) -> {
            Integer w = weights.get(patron);
            if (w != null) {
                out.put(patron, Math.max(1, (int) Math.floor(w * mult)));
            }
        });
        return out;
    }

    /** 文明抽取与主神抽取用不同的盐，避免两者相关。 */
    public static final long SALT_CIV = 0x43_49_56L;
    public static final long SALT_PATRON = 0x50_41_54L;
}
