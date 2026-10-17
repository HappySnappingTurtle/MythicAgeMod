package io.github.happysnappingturtle.mythicage.core.config.world;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code world_rules.json}：方块改变类开关（总纲 R2，决策 C3）。本文件独占这类开关。
 * <p>
 * 通用节不放任何一个文明专属的内容；文明专属的放在以文明 id 命名的节里（T10b 设计文档 §3.4）。
 *
 * @param beneficialConversions 施法者对自有方块做的有益、可逆转化（如神农田）
 * @param mobBlockDamage        生物破坏方块（同时遵守 mobGriefing）
 * @param blockEntityStall      方块实体（如刷怪笼）临时停摆
 * @param recipes               配方规则（T6）
 * @param shrines               神庙种类 id（如 {@code greek:altar}）→ 数量上限、间距与保护（T10b）
 * @param guardians             祭司（T9，T10b 起对所有文明生效）
 * @param greek                 希腊专属：作物生长（T6b）、神庙扫描预算（T8a）、信徒祈祷（T10）
 */
public record WorldRulesConfig(boolean beneficialConversions, boolean mobBlockDamage, boolean blockEntityStall,
                               GodPowers godPowers, Recipes recipes, Map<String, Shrine> shrines,
                               Guardians guardians, Greek greek) {
    public WorldRulesConfig {
        shrines = Map.copyOf(new LinkedHashMap<>(shrines));
    }

    /** 某种神庙的规则；配置里没有这一种时用 {@link Shrine#FALLBACK}。 */
    public Shrine shrine(String kindId) {
        return shrines.getOrDefault(kindId, Shrine.FALLBACK);
    }

    public WorldRulesConfig withRecipes(Recipes r) {
        return new WorldRulesConfig(beneficialConversions, mobBlockDamage, blockEntityStall, godPowers, r, shrines,
                guardians, greek);
    }

    public WorldRulesConfig withShrine(String kindId, Shrine rule) {
        Map<String, Shrine> next = new LinkedHashMap<>(shrines);
        next.put(kindId, rule);
        return new WorldRulesConfig(beneficialConversions, mobBlockDamage, blockEntityStall, godPowers, recipes, next,
                guardians, greek);
    }

    public WorldRulesConfig withGuardians(Guardians g) {
        return new WorldRulesConfig(beneficialConversions, mobBlockDamage, blockEntityStall, godPowers, recipes, shrines,
                g, greek);
    }

    public WorldRulesConfig withGreek(Greek g) {
        return new WorldRulesConfig(beneficialConversions, mobBlockDamage, blockEntityStall, godPowers, recipes, shrines,
                guardians, g);
    }

    /** @param terrainEdits 神力的永久地形破坏，默认关闭（红线 1） */
    /**
     * 神力（T12 设计文档 §2.8）。
     *
     * @param civSharedCooldownTicks 同文明共享短冷却（05 号 §1.1，默认 600 刻 = 30 秒）
     * @param transientEntityCap     全服临时实体上限（R-CAP，默认 96）
     * @param transientPerPlayer     每名玩家临时实体上限（R-CAP，默认 16）
     * @param shieldRadius           神力护盾半径（默认 32 格）
     * @param aimTimeoutTicks        瞄准多久不确认自动取消（D2，默认 200 刻 = 10 秒）
     */
    public record GodPowers(boolean terrainEdits, int civSharedCooldownTicks, int transientEntityCap, int transientPerPlayer,
                            int shieldRadius, int aimTimeoutTicks) {
        public static final GodPowers DEFAULTS = new GodPowers(false, 600, 96, 16, 32, 200);
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
     * 一种神庙（T10b 设计文档 §3）。数量上限按玩家在该神庙所属文明的时代计。
     *
     * @param spacing 同种神庙之间的最小水平切比雪夫距离
     * @param protect 只有主人（或创造模式管理员）能挖掉
     */
    public record Shrine(int limitArchaic, int limitClassical, int limitHeroic, int limitMythic, int spacing,
                         boolean protect) {
        /** 配置里没有的种类：远古 1 座，逐级加到 3，不限间距，受保护。 */
        public static final Shrine FALLBACK = new Shrine(1, 2, 3, 3, 1, true);
        public static final Shrine GREEK_ALTAR = new Shrine(1, 2, 3, 3, 15, true);

        /** 按时代 id（archaic / classical / heroic / mythic）取上限；其他（含 none）为 0。 */
        public int limit(String ageId) {
            return switch (ageId) {
                case "archaic" -> limitArchaic;
                case "classical" -> limitClassical;
                case "heroic" -> limitHeroic;
                case "mythic" -> limitMythic;
                default -> 0;
            };
        }
    }

    /**
     * 祭司（T9 设计文档 §7）。
     *
     * @param spawnInVillages 已登记村庄的中心旁出现本文明的祭司（决策 D1；对所有有祭司的文明生效）
     * @param respawnDays     祭司死亡后在居所复原所需的 MC 日（决策 D4）
     * @param interactRange   交易界面保持打开的最大距离（格）
     */
    public record Guardians(boolean spawnInVillages, int respawnDays, int interactRange) {
        public static final Guardians DEFAULTS = new Guardians(true, 1, 8);
    }

    /** 希腊专属规则。 */
    public record Greek(Growth growth, TempleBudget temple, Prayer prayer) {
        public static final Greek DEFAULTS = new Greek(new Growth(24000, 12000, new BoneMeal(0.5, false)),
                new TempleBudget(2048, 6000), new Prayer(1));

        public Greek withGrowth(Growth g) {
            return new Greek(g, temple, prayer);
        }

        public Greek withTemple(TempleBudget t) {
            return new Greek(growth, t, prayer);
        }

        public Greek withPrayer(Prayer p) {
            return new Greek(growth, temple, p);
        }
    }

    /**
     * 信徒祈祷（T10 设计文档 §7，决策 F7）。
     *
     * @param restockLimit 当天祈祷过的有职业村民每日最多补货几次（原版 2，取 0–2）
     */
    public record Prayer(int restockLimit) {
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
     * 希腊神庙扫描（T8a 设计文档 §7）。
     *
     * @param scanCellsPerTick    全服神庙扫描每刻处理的格数上限
     * @param rescanIntervalTicks 兜底重扫间隔
     */
    public record TempleBudget(int scanCellsPerTick, int rescanIntervalTicks) {
    }
}
