package io.github.happysnappingturtle.mythicage.mixin.client;

import io.github.happysnappingturtle.mythicage.client.equipment.WeaponAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 第三人称的专属攻击动作（T17 §8.2）：攻击手上的武器配置了非 slash 动作时，换掉原版的横挥。 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelAttackMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void mythicAge$motionLean(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                     float netHeadYaw, float headPitch, CallbackInfo ci) {
        WeaponAnimations.lean((HumanoidModel<?>) (Object) this, entity);
    }

    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void mythicAge$styleAttack(T entity, float ageInTicks, CallbackInfo ci) {
        if (WeaponAnimations.thirdPerson((HumanoidModel<?>) (Object) this, entity)) {
            ci.cancel();
        }
    }
}
