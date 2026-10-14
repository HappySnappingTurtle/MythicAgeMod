package io.github.happysnappingturtle.mythicage.core.config.world;

/**
 * {@code world_rules.json}：方块改变类开关（总纲 R2，决策 C3）。本文件独占这类开关。
 *
 * @param beneficialConversions 施法者对自有方块做的有益、可逆转化（如神农田）
 * @param mobBlockDamage        生物破坏方块（同时遵守 mobGriefing）
 * @param blockEntityStall      方块实体（如刷怪笼）临时停摆
 * @param recipes               配方规则（T6）
 */
public record WorldRulesConfig(boolean beneficialConversions, boolean mobBlockDamage, boolean blockEntityStall,
                               GodPowers godPowers, Recipes recipes) {

    /** @param terrainEdits 神力的永久地形破坏，默认关闭（红线 1） */
    public record GodPowers(boolean terrainEdits) {
    }

    public record Recipes(AgeGated ageGated) {
    }

    /**
     * 时代受限配方（T6 设计文档 §7）。
     *
     * @param enabled         false 时所有受限配方当作普通配方
     * @param allowAutomation true 时放行没有玩家的自动合成（合成器、其他模组的自动合成）
     */
    public record AgeGated(boolean enabled, boolean allowAutomation) {
    }
}
