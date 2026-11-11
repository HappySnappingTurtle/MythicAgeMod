package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.equipment.AttackStyle;
import io.github.happysnappingturtle.mythicage.equipment.MeleeWeaponItem;
import io.github.happysnappingturtle.mythicage.equipment.WeaponMotionState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 专属攻击动作的挥击长度（T17 §8.2）：带攻击方式标签的武器按 {@link AttackStyle#swingTicks()} 挥（原版 6 刻），
 * 急迫 / 挖掘疲劳的加减照原版保留。只影响挥手动画的时长，不影响攻击冷却。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingMixin {
    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void mythicAge$styleSwing(CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        InteractionHand hand = self.swingingArm != null ? self.swingingArm : InteractionHand.MAIN_HAND;
        if (self.getItemInHand(hand).getItem() instanceof MeleeWeaponItem) {
            int ticks = WeaponMotionState.current(self, self.getItemInHand(hand)).swingTicks();
            cir.setReturnValue(Math.max(1, ticks + cir.getReturnValue() - 6));
            return;
        }
        AttackStyle.of(self.getItemInHand(hand)).ifPresent(s -> cir.setReturnValue(Math.max(1, s.swingTicks() + cir.getReturnValue() - 6)));
    }
}
