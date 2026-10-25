package io.github.happysnappingturtle.mythicage.village.soldier;

import io.github.happysnappingturtle.mythicage.conduct.Responsibility;
import io.github.happysnappingturtle.mythicage.mixin.LivingEntityHurtByPlayerAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/**
 * 士兵击杀的归因 ×0（T20 设计文档 §6，01 号 §0.1）：
 * <ul>
 *   <li>恩宠、善恶由 {@link Responsibility} 的「士兵，无责任人」保证（这里不用管）；</li>
 *   <li>士兵最后一击的生物：清掉「最近被玩家攻击」的记录——原版就按非玩家击杀掉落：没有经验、没有玩家击杀专属战利品；</li>
 *   <li>打上 {@link #TAG}，供首杀记录检查（T23：士兵击杀的精英与 Boss 不写首杀）。</li>
 * </ul>
 */
public final class SoldierAttribution {
    public static final String TAG = "mythic_age:killed_by_soldier";

    private SoldierAttribution() {
    }

    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (e.isCanceled() || victim.level().isClientSide()) {
            return;
        }
        if (Responsibility.of(e.getSource()).via() != Responsibility.Via.SOLDIER) {
            return;
        }
        victim.getPersistentData().putBoolean(TAG, true);
        LivingEntityHurtByPlayerAccessor acc = (LivingEntityHurtByPlayerAccessor) victim;
        acc.mythicAge$setLastHurtByPlayer(null);
        acc.mythicAge$setLastHurtByPlayerTime(0);
    }

    public static void onExperienceDrop(LivingExperienceDropEvent e) {
        if (killedBySoldier(e.getEntity())) {
            e.setDroppedExperience(0);
        }
    }

    /** 这只生物是不是被士兵打死的（首杀记录用）。 */
    public static boolean killedBySoldier(Entity e) {
        return e != null && e.getPersistentData().getBoolean(TAG);
    }
}
