package io.github.happysnappingturtle.mythicage.core.village.soldier;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 士兵名额与转岗（T20 设计文档 §4.1–§4.3，01 号 §0.2）：纯函数。
 */
public final class SoldierRoster {
    /** 人口达到这个数时上限至少 1（01 号 §0.2）。 */
    public static final int MIN_POPULATION = 4;

    private SoldierRoster() {
    }

    /**
     * 上限 = min(硬上限, ⌊人口 × 比例⌋，人口 ≥ 4 时至少 1) + 当前阶段加成。硬上限 0 表示关闭（阶段加成也不加）。
     */
    public static int cap(int population, int stageBonus, int hardCap, double ratio) {
        if (hardCap <= 0) {
            return 0;
        }
        int base = Math.min(hardCap, (int) Math.floor(Math.max(0, population) * Math.max(0, ratio) + 1.0E-9));
        if (population >= MIN_POPULATION && ratio > 0) {
            base = Math.max(1, base);
        }
        return base + Math.max(0, stageBonus);
    }

    /** 远程配额 ⌊上限 / 2⌋（D3：上限只有 1 时那一名一定是近战）。 */
    public static int rangedQuota(int cap) {
        return Math.max(0, cap) / 2;
    }

    /** 一个兵种：职业 id、是否远程、此刻是否开放。 */
    public record Branch(String profession, boolean ranged, boolean open) {
    }

    /**
     * 下一名该补哪个兵种：先补近战到「上限 − 远程配额」，再补远程到远程配额；远程都没开放时近战补满上限。
     * 同类兵种有多个时按 {@code order} 的顺序取第一个开放的。没有空缺返回 empty。
     *
     * @param have 现有各兵种人数
     */
    public static Optional<String> next(List<Branch> order, Map<String, Integer> have, int cap) {
        int total = have.values().stream().mapToInt(Integer::intValue).sum();
        if (total >= cap) {
            return Optional.empty();
        }
        boolean rangedOpen = order.stream().anyMatch(b -> b.ranged() && b.open());
        int quota = rangedOpen ? rangedQuota(cap) : 0;
        int melee = 0;
        int ranged = 0;
        for (Branch b : order) {
            int n = have.getOrDefault(b.profession(), 0);
            if (b.ranged()) {
                ranged += n;
            } else {
                melee += n;
            }
        }
        boolean wantRanged = melee >= cap - quota && ranged < quota;
        for (Branch b : order) {
            if (b.open() && b.ranged() == wantRanged) {
                return Optional.of(b.profession());
            }
        }
        // 想要的那类没有开放的（比如只有远程兵种）：退而取任一开放的
        return order.stream().filter(Branch::open).map(Branch::profession).findFirst();
    }

    /**
     * 转岗候选：{@code distance} 是床位到第一个岗位的距离。
     */
    public record Candidate(UUID id, String profession, boolean keeper, boolean inTransit, double distance) {
    }

    /**
     * 选一名转岗的人：只从基础劳力里选，不选谷仓管事、不选在途移民；转完后劳力（含管事）至少留 {@code minLaborers} 人。
     * 床位离岗位最近的优先，距离相同按 UUID。
     */
    public static Optional<UUID> pickConvert(List<Candidate> roster, String fallback, int minLaborers) {
        long laborers = roster.stream().filter(c -> c.profession().equals(fallback) && !c.inTransit()).count();
        if (laborers - 1 < minLaborers) {
            return Optional.empty();
        }
        return roster.stream()
                .filter(c -> c.profession().equals(fallback) && !c.keeper() && !c.inTransit())
                .min(Comparator.comparingDouble(Candidate::distance).thenComparing(Candidate::id))
                .map(Candidate::id);
    }

    /** 名册里的一名士兵：转岗的先后序号（越大越晚）。 */
    public record Enlisted(UUID id, String profession, long order) {
    }

    /**
     * 多出来的士兵（士兵数 &gt; max(上限, keepFloor)）：最晚转岗的先降回。返回要降回的人，按降回顺序。
     *
     * @param keepFloor 初始卫兵的保底（设计 §4.4：上限为 0 时也不降回生成时的那一名；关闭士兵时传 0）
     */
    public static List<UUID> pickDemote(List<Enlisted> soldiers, int cap, int keepFloor) {
        int keep = Math.max(cap, keepFloor);
        if (soldiers.size() <= keep) {
            return List.of();
        }
        return soldiers.stream().sorted(Comparator.comparingLong(Enlisted::order).reversed().thenComparing(Enlisted::id))
                .limit(soldiers.size() - keep).map(Enlisted::id).toList();
    }

    /** 穿哪一档：村库有完整的就完整，否则简易，都没有为 NONE。 */
    public static SoldierProfile.Tier choose(int fullInStock, int simpleInStock) {
        if (fullInStock > 0) {
            return SoldierProfile.Tier.FULL;
        }
        return simpleInStock > 0 ? SoldierProfile.Tier.SIMPLE : SoldierProfile.Tier.NONE;
    }

    /** 该不该换装：穿简易或没有、村库里有更好的。 */
    public static boolean upgrade(SoldierProfile.Tier wearing, int fullInStock, int simpleInStock) {
        return switch (wearing) {
            case FULL -> false;
            case SIMPLE -> fullInStock > 0;
            case NONE -> fullInStock > 0 || simpleInStock > 0;
        };
    }
}
