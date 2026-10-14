package io.github.happysnappingturtle.mythicage.core.config.world;

/**
 * {@code world_rules.json}：方块改变类开关（总纲 R2，决策 C3）。本文件独占这类开关。
 *
 * @param beneficialConversions 施法者对自有方块做的有益、可逆转化（如神农田）
 * @param mobBlockDamage        生物破坏方块（同时遵守 mobGriefing）
 * @param blockEntityStall      方块实体（如刷怪笼）临时停摆
 */
public record WorldRulesConfig(boolean beneficialConversions, boolean mobBlockDamage, boolean blockEntityStall,
                               GodPowers godPowers) {

    /** @param terrainEdits 神力的永久地形破坏，默认关闭（红线 1） */
    public record GodPowers(boolean terrainEdits) {
    }
}
