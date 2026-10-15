package io.github.happysnappingturtle.mythicage.core.config.world;

/**
 * {@code world_rules.json}：方块改变类开关（总纲 R2，决策 C3）。本文件独占这类开关。
 *
 * @param beneficialConversions 施法者对自有方块做的有益、可逆转化（如神农田）
 * @param mobBlockDamage        生物破坏方块（同时遵守 mobGriefing）
 * @param blockEntityStall      方块实体（如刷怪笼）临时停摆
 * @param recipes               配方规则（T6）
 * @param growth                作物与果树的生长（T6b，含骨粉等催化效果的规则）
 * @param temples               神坛与神庙判定（T8a）
 */
public record WorldRulesConfig(boolean beneficialConversions, boolean mobBlockDamage, boolean blockEntityStall,
                               GodPowers godPowers, Recipes recipes, Growth growth, Temples temples) {

    /** 兼容旧调用：神庙规则用默认值。 */
    public WorldRulesConfig(boolean beneficialConversions, boolean mobBlockDamage, boolean blockEntityStall,
                            GodPowers godPowers, Recipes recipes, Growth growth) {
        this(beneficialConversions, mobBlockDamage, blockEntityStall, godPowers, recipes, growth, Temples.DEFAULTS);
    }

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

    /**
     * 生长（T6b 设计文档 §10.3）。
     *
     * @param oliveFruitTicks 橄榄树叶从无果到熟果的期望时长（按默认随机刻速度换算）
     * @param grapeStageTicks 葡萄架每个阶段的期望时长
     */
    public record Growth(long oliveFruitTicks, long grapeStageTicks, BoneMeal boneMeal) {
    }

    /**
     * @param grapeChance 骨粉使葡萄架推进 1 阶的几率
     * @param oliveLeaves 橄榄树叶是否接受骨粉（默认否：橄榄是奠酒原料，防止自动化无上限刷）
     */
    public record BoneMeal(double grapeChance, boolean oliveLeaves) {
    }

    /**
     * 神坛与神庙判定（T8a 设计文档 §7）。
     *
     * @param altarLimitArchaic   各时代每位玩家可拥有的希腊神坛数
     * @param altarSpacing        任意两座神坛的最小水平切比雪夫距离（15 时扫描体不重叠）
     * @param protectAltar        只有主人（或创造模式管理员）能挖掉神坛
     * @param scanCellsPerTick    全服神庙扫描每刻处理的格数上限
     * @param rescanIntervalTicks 兜底重扫间隔
     */
    public record Temples(int altarLimitArchaic, int altarLimitClassical, int altarLimitHeroic, int altarLimitMythic,
                          int altarSpacing, boolean protectAltar, int scanCellsPerTick, int rescanIntervalTicks) {
        public static final Temples DEFAULTS = new Temples(1, 2, 3, 3, 15, true, 2048, 6000);

        /** 按时代 id（archaic / classical / heroic / mythic）取上限；其他（含 none）为 0。 */
        public int altarLimit(String ageId) {
            return switch (ageId) {
                case "archaic" -> altarLimitArchaic;
                case "classical" -> altarLimitClassical;
                case "heroic" -> altarLimitHeroic;
                case "mythic" -> altarLimitMythic;
                default -> 0;
            };
        }
    }
}
