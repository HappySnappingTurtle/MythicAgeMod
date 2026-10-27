package io.github.happysnappingturtle.mythicage.core.village;

/**
 * 村庄受阻的三分类（T24 设计文档 §4.3，12 号 §4「自主会解决 / 需要稀有材料 / 被占地」要区分，
 * 不把整屏都写成捐物请求）。五文明通用：「能否自产」看文明数据（{@link CivVillageData#producible}）。
 */
public final class VillageBlockers {
    private VillageBlockers() {
    }

    /** 村务界面已有的缺口种类。 */
    public enum Kind { FOOD_LOW, POINT_DEPLETED, STOCK_FULL, NEEDS_HOUSING, MISSING_MATERIAL, PROJECT_BLOCKED, NEEDS_SPACE }

    public enum Class {
        SELF_RESOLVING, RARE_MATERIAL, OCCUPIED;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** @param producible 只对 MISSING_MATERIAL 有意义：本村有没有这种料的来源 */
    public static Class classify(Kind kind, boolean producible) {
        return switch (kind) {
            case FOOD_LOW, POINT_DEPLETED, STOCK_FULL, NEEDS_HOUSING -> Class.SELF_RESOLVING;
            case MISSING_MATERIAL -> producible ? Class.SELF_RESOLVING : Class.RARE_MATERIAL;
            case PROJECT_BLOCKED, NEEDS_SPACE -> Class.OCCUPIED;
        };
    }
}
