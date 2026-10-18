package io.github.happysnappingturtle.mythicage.core.power;

/**
 * 敌我分类与目标矩阵取值（T12 设计文档 §2.2；05 号 §5、总纲 §3.10）。
 * <p>
 * 框架只决定「这个实体落在哪一列」和「这一列对本神力的反应键」；反应键由各神力的效果代码解释。
 * 不论卡片怎么写，以下情形一律是 {@link #NONE}（不可选、不受影响），失控型神力除外（决策 G3）：
 * PvP 关时的玩家、队友、队友的召唤单位、非敌对玩家的召唤单位、驯服 / 拴绳 / 他人宠物（卡片写 {@code pets} 键时除外）。
 * 友方入口（{@code ALLY_AREA}）例外：队友取「施法者」列、队友的召唤单位取「自己的常驻召唤单位」列（T13 §1.5）。
 */
public final class TargetClassifier {
    public static final String NONE = "none";
    /** 卡片给「中立动物」写这个键时，宠物与拴绳动物也受影响（如环境型神力）。 */
    public static final String PETS = "pets";

    private TargetClassifier() {
    }

    /** 按优先级取第一个匹配的列。 */
    public static TargetCategory classify(TargetFacts f) {
        if (f.isCaster()) {
            return TargetCategory.CASTER;
        }
        if (f.isBoss()) {
            return TargetCategory.BOSS;
        }
        if (f.isElite()) {
            return TargetCategory.ELITE;
        }
        if (f.isPlayer()) {
            return TargetCategory.ENEMY_PLAYER;
        }
        if (f.isGuardian()) {
            return TargetCategory.GUARDIAN;
        }
        if (f.isSummon()) {
            return f.summonOwnedByCaster() ? TargetCategory.OWN_SUMMON : TargetCategory.OTHER_SUMMON;
        }
        if (f.isSoldier()) {
            return f.inOwnVillage() ? TargetCategory.OWN_SOLDIER : TargetCategory.OTHER_VILLAGE;
        }
        if (f.isVillager()) {
            return f.inOwnVillage() ? TargetCategory.OWN_VILLAGER : TargetCategory.OTHER_VILLAGE;
        }
        if (f.hostile()) {
            return TargetCategory.HOSTILE;
        }
        return TargetCategory.NEUTRAL_ANIMAL;
    }

    /** 本神力对这个实体的反应键；{@link #NONE} 表示不可选、不受影响。 */
    public static String reaction(PowerSpec spec, TargetFacts f) {
        TargetCategory c = classify(f);
        String key = spec.reaction(c);
        if (spec.profile() == TargetProfile.FREE_FOR_ALL) {
            return key;
        }
        if (spec.profile() == TargetProfile.ALLY_AREA) {
            // 友方入口（T13 设计文档 §1.5）：队友按施法者、队友的召唤单位按自己的召唤单位
            if (c == TargetCategory.ENEMY_PLAYER && f.teammate()) {
                return spec.reaction(TargetCategory.CASTER);
            }
            if (c == TargetCategory.OTHER_SUMMON && f.summonOwnerTeammate()) {
                return spec.reaction(TargetCategory.OWN_SUMMON);
            }
        }
        switch (c) {
            case ENEMY_PLAYER -> {
                if (!f.pvpEnabled() || f.teammate()) {
                    return NONE;
                }
            }
            case OTHER_SUMMON -> {
                if (f.summonOwnerTeammate() || !f.pvpEnabled() || !f.summonOwnerHostile()) {
                    return NONE;
                }
            }
            case NEUTRAL_ANIMAL -> {
                if (f.petOrLeashed() && !PETS.equals(key)) {
                    return NONE;
                }
            }
            default -> {
            }
        }
        return key;
    }
}
