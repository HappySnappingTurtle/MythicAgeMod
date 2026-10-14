package io.github.happysnappingturtle.mythicage.core.config.conduct;

/**
 * {@code conduct.json}：善恶值（决策 D1）。数值来自
 * {@code docs/重构/设计/跨文明详细设计/02_善恶值事件与NPC任务合同.md}。
 * <p>
 * 刻意没有「村外动物伤害」字段：村外动物伤害记 0 是设计硬边界，不可配置。
 *
 * @param incidentWindowTicks 同一责任人、同一受害者的事件窗口（去重与升级用）
 * @param selfDefenseExempt   合法自卫不计善恶
 * @param pvpCountsAsConduct  合法 PvP 是否计入善恶
 */
public record ConductConfig(boolean enabled, Range range, int initial, Regression regression, int positiveDailyCap,
                           int incidentWindowTicks, Events events, boolean selfDefenseExempt,
                           boolean pvpCountsAsConduct) {

    public record Range(int min, int max) {
    }

    /** 每累计 intervalOnlineTicks 个在线 tick，向 0 靠近 points 点（不跨零）。 */
    public record Regression(int points, long intervalOnlineTicks) {
    }

    /**
     * 事件分值。负面事件 ≤0，正面事件 ≥0；「Total」是同一事件致死时的总罚分（含已扣的伤害分）。
     */
    public record Events(int harmVillager, int killVillagerTotal,
                         int harmGuardian, int killGuardianTotal,
                         int harmDefender, int killDefenderTotal,
                         int harmAnimalInVillage, int killAnimalInVillageTotal,
                         int cureZombieVillager, int rescueResident, int rescueEscortQuest) {
    }
}
