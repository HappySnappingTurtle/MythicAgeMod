package io.github.happysnappingturtle.mythicage.boss;

/**
 * 试炼竞技场里的 Boss（T23 §4）：实体记住自己属于哪个竞技场实例；竞技场在阶段切换、拴绳传回时通知它。
 */
public interface TrialBoss {
    /** 竞技场实例 id；未绑定为空字符串。 */
    String arenaId();

    void bindArena(String id);

    /** 被拴绳传回出生点：清掉当前招式。 */
    default void onLeashed() {
    }

    /** 竞技场进入阶段二（血量首次 ≤ 50%）。 */
    default void onPhaseTwo() {
    }

    /** 阶段二的开场（怒吼）结束了，竞技场可以开始机关。 */
    default boolean phaseTwoReady() {
        return true;
    }

    /** 当前受伤倍率（机制窗口内 ×2）。 */
    default float damageTakenMultiplier() {
        return 1.0F;
    }
}
