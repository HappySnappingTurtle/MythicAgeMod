package io.github.happysnappingturtle.mythicage.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 「最近被玩家攻击」的记录（T20 §6.2：士兵最后一击时清掉，原版就按非玩家击杀处理）。 */
@Mixin(LivingEntity.class)
public interface LivingEntityHurtByPlayerAccessor {
    @Accessor("lastHurtByPlayer")
    void mythicAge$setLastHurtByPlayer(Player player);

    @Accessor("lastHurtByPlayerTime")
    void mythicAge$setLastHurtByPlayerTime(int ticks);
}
