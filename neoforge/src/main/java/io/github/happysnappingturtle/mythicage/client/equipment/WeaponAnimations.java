package io.github.happysnappingturtle.mythicage.client.equipment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.core.equipment.WeaponMotion;
import io.github.happysnappingturtle.mythicage.core.equipment.WeaponMotions;
import io.github.happysnappingturtle.mythicage.equipment.MeleeWeaponItem;
import io.github.happysnappingturtle.mythicage.equipment.WeaponMotionState;
import io.github.happysnappingturtle.mythicage.equipment.DrawBowItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


/**
 * 专属攻击动作的曲线（T17 设计文档 §8.2）：第三人称改模型的手臂，第一人称改手持物的变换，两边用同一组分段。
 * 自变量 t 是原版的挥击进度（0 → 1），挥击时长按 {@link WeaponMotion#swingTicks()}。
 * 模型坐标：手臂 xRot 为负 = 向前上方抬起，−π/2 是水平前伸；z 为负 = 向前。
 */
public final class WeaponAnimations {
    private WeaponAnimations() {
    }

    /** 对应手的动作；副手举盾时仍只改变攻击手。 */
    private static InteractionHand hand(LivingEntity entity, HumanoidArm side) {
        return side == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    /** ArmPose 回调：每帧只使用 core 曲线，不重新判断暴击。 */
    public static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm side) {
        InteractionHand hand = hand(entity, side);
        ItemStack stack = entity.getItemInHand(hand);
        WeaponMotion motion = WeaponMotionState.current(entity, stack);
        ModelPart arm = side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float k = side == HumanoidArm.RIGHT ? 1 : -1;
        float t = entity.swingingArm == hand ? model.attackTime : 0;
        float base = arm.xRot * 0.3F - 0.45F;
        arm.xRot = base;
        arm.yRot = 0;
        if (t <= 0 || motion == WeaponMotion.SLASH) {
            return;
        }
        float envelope = WeaponMotions.envelope(t);
        if (motion == WeaponMotion.THRUST) {
            float distance = WeaponMotions.thrust(t);
            // 前伸持械手臂，再沿朝向直送；举盾时从盾上沿送出，副手角度不动。
            boolean overShield = entity.isBlocking() && entity.getUsedItemHand() != hand;
            arm.xRot = Mth.lerp(envelope, base, overShield ? -1.2F : -55 * Mth.DEG_TO_RAD);
            arm.yRot = -0.10F * k * envelope;
            arm.z -= distance * 12;
        } else {
            arm.xRot = base - WeaponMotions.cleave(t) * Mth.DEG_TO_RAD * 1.35F;
            arm.yRot = -0.08F * k * envelope;
        }
    }

    /** 复用既有 Mixin，只阻止原版横挥覆盖自定义 ArmPose；未配动作的武器仍用原版。 */
    public static boolean thirdPerson(HumanoidModel<?> model, LivingEntity entity) {
        if (!customAttack(model, entity)) {
            return false;
        }
        InteractionHand hand = entity.swingingArm != null ? entity.swingingArm : InteractionHand.MAIN_HAND;
        HumanoidArm side = hand == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
        HumanoidModel.ArmPose pose = side == HumanoidArm.RIGHT ? model.rightArmPose : model.leftArmPose;
        // 原版举盾时只执行盾手的 ArmPose；非玩家模型也未必查询客户端物品扩展，补上攻击手。
        if (entity.isUsingItem() || pose != WeaponPoses.motion()) {
            pose(model, entity, side);
        }
        return true;
    }

    private static boolean customAttack(HumanoidModel<?> model, LivingEntity entity) {
        InteractionHand hand = entity.swingingArm != null ? entity.swingingArm : InteractionHand.MAIN_HAND;
        return model.attackTime > 0 && entity.getItemInHand(hand).getItem() instanceof MeleeWeaponItem
                && WeaponMotionState.current(entity, entity.getItemInHand(hand)) != WeaponMotion.SLASH;
    }

    /** 原版在 ArmPose 后重置 body.xRot，所以前倾必须在 setupAnim 末尾追加。 */
    public static void lean(HumanoidModel<?> model, LivingEntity entity) {
        InteractionHand hand = entity.swingingArm != null ? entity.swingingArm : InteractionHand.MAIN_HAND;
        if (customAttack(model, entity) && WeaponMotionState.current(entity, entity.getItemInHand(hand)) == WeaponMotion.THRUST) {
            model.body.xRot += Math.max(0, WeaponMotions.thrust(model.attackTime)) * 0.22F;
        }
    }

    /** applyForgeHandTransform：按原版挥击进度取曲线，使用中交回原版（复合弓除外）。 */
    public static boolean firstPerson(PoseStack ps, LivingEntity player, HumanoidArm arm, ItemStack stack, float partialTick,
                                      float equip, float swing) {
        int k = arm == HumanoidArm.RIGHT ? 1 : -1;
        InteractionHand hand = hand(player, arm);
        boolean using = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
        if (stack.getItem() instanceof DrawBowItem bow) {
            if (!using) {
                return false;
            }
            bowDraw(ps, player, stack, k, partialTick, equip, bow.fullTicks());
            return true;
        }
        if (!(stack.getItem() instanceof MeleeWeaponItem) || using) {
            return false;
        }
        WeaponMotion motion = WeaponMotionState.current(player, stack);
        if (motion == WeaponMotion.SLASH) {
            return false;
        }
        ps.translate(k * 0.56F, -0.52F - equip * 0.2F, -0.72F);
        if (motion == WeaponMotion.THRUST) {
            // 持械模型尖端原本向前上 25°，绕 X 转 -65° 后沿视线；前推阶段保持角度。
            ps.translate(0, 0, -WeaponMotions.thrust(swing));
            ps.mulPose(Axis.XP.rotationDegrees(-65 * WeaponMotions.envelope(swing)));
        } else {
            float angle = WeaponMotions.cleave(swing);
            ps.translate(0, Math.max(0, angle) * 0.0025F, -Math.max(0, -angle) * 0.003F);
            ps.mulPose(Axis.XP.rotationDegrees(angle));
        }
        return true;
    }

    /** 原版第一人称拉弓（ItemInHandRenderer 的 BOW 分支），满弓刻数换成这把弓自己的。 */
    private static void bowDraw(PoseStack ps, LivingEntity player, ItemStack stack, int k, float partialTick, float equip,
                                int fullTicks) {
        ps.translate(k * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        ps.translate(k * -0.2785682F, 0.18344387F, 0.15731531F);
        ps.mulPose(Axis.XP.rotationDegrees(-13.935F));
        ps.mulPose(Axis.YP.rotationDegrees(k * 35.3F));
        ps.mulPose(Axis.ZP.rotationDegrees(k * -9.785F));
        float used = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
        float f = used / fullTicks;
        f = Math.min(1.0F, (f * f + f * 2.0F) / 3.0F);
        if (f > 0.1F) {
            float shake = Mth.sin((used - 0.1F) * 1.3F) * (f - 0.1F);
            ps.translate(0, shake * 0.004F, 0);
        }
        ps.translate(0, 0, f * 0.04F);
        ps.scale(1.0F, 1.0F, 1.0F + f * 0.2F);
        ps.mulPose(Axis.YN.rotationDegrees(k * 45.0F));
    }
}
