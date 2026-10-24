package io.github.happysnappingturtle.mythicage.village.resident;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.raid.Raider;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 原版敌对生物把居民当村民攻击（T19 设计文档 §3.3，V1 R5）：僵尸系、灾厄村民系在进入世界时加一个以居民为目标的目标选择，
 * 优先级与它们攻击原版村民的那个相同（3）。
 */
public final class ResidentHostility {
    private ResidentHostility() {
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Zombie z) {
            z.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(z, ResidentEntity.class, false));
        } else if (event.getEntity() instanceof Raider r) {
            r.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(r, ResidentEntity.class, false));
        }
    }
}
