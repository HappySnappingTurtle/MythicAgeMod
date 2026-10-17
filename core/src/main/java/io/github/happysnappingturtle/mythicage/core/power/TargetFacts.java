package io.github.happysnappingturtle.mythicage.core.power;

/**
 * 分类一个实体所需的事实（服务端从实体收集，core 只做计算）。
 *
 * @param inOwnVillage      村民 / 士兵属于施法者的本村（举行过仪式或自己建立的村庄）
 * @param summonOwnedByCaster 召唤单位的主人是施法者
 * @param summonOwnerTeammate 召唤单位的主人是施法者的队友
 * @param summonOwnerHostile  召唤单位的主人是敌对玩家（PvP 开且不同队）
 * @param petOrLeashed       驯服、拴绳或他人的宠物
 * @param hostile            敌对生物（Enemy 接口）或正在攻击施法者 / 其召唤物
 */
public record TargetFacts(boolean isCaster, boolean isPlayer, boolean teammate, boolean pvpEnabled,
                          boolean isVillager, boolean isSoldier, boolean inOwnVillage,
                          boolean isSummon, boolean summonOwnedByCaster, boolean summonOwnerTeammate,
                          boolean summonOwnerHostile, boolean isGuardian, boolean petOrLeashed,
                          boolean isBoss, boolean isElite, boolean hostile) {

    /** 便于构造的生成器（测试与收集器用）。 */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean isCaster, isPlayer, teammate, pvpEnabled, isVillager, isSoldier, inOwnVillage, isSummon,
                summonOwnedByCaster, summonOwnerTeammate, summonOwnerHostile, isGuardian, petOrLeashed, isBoss, isElite,
                hostile;

        public Builder caster() { isCaster = true; return this; }
        public Builder player(boolean teammate, boolean pvp) { isPlayer = true; this.teammate = teammate; pvpEnabled = pvp; return this; }
        public Builder pvp(boolean pvp) { pvpEnabled = pvp; return this; }
        public Builder villager(boolean own) { isVillager = true; inOwnVillage = own; return this; }
        public Builder soldier(boolean own) { isSoldier = true; inOwnVillage = own; return this; }
        public Builder summon(boolean ownedByCaster, boolean ownerTeammate, boolean ownerHostile) {
            isSummon = true; summonOwnedByCaster = ownedByCaster; summonOwnerTeammate = ownerTeammate;
            summonOwnerHostile = ownerHostile; return this;
        }
        public Builder guardian() { isGuardian = true; return this; }
        public Builder pet() { petOrLeashed = true; return this; }
        public Builder boss() { isBoss = true; return this; }
        public Builder elite() { isElite = true; return this; }
        public Builder hostile() { hostile = true; return this; }

        public TargetFacts build() {
            return new TargetFacts(isCaster, isPlayer, teammate, pvpEnabled, isVillager, isSoldier, inOwnVillage, isSummon,
                    summonOwnedByCaster, summonOwnerTeammate, summonOwnerHostile, isGuardian, petOrLeashed, isBoss, isElite,
                    hostile);
        }
    }
}
