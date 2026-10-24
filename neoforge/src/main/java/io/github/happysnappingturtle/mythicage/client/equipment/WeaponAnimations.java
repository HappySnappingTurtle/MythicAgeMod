package io.github.happysnappingturtle.mythicage.client.equipment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.equipment.AttackStyle;
import io.github.happysnappingturtle.mythicage.equipment.DrawBowItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 专属攻击动作的曲线（T17 设计文档 §8.2）：第三人称改模型的手臂，第一人称改手持物的变换，两边用同一组分段。
 * 自变量 t 是原版的挥击进度（0 → 1），挥击时长按 {@link AttackStyle#swingTicks()}。
 * 模型坐标：手臂 xRot 为负 = 向前上方抬起，−π/2 是水平前伸；z 为负 = 向前。
 */
public final class WeaponAnimations {
    private WeaponAnimations() {
    }

    static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** 0 → 1 → 0 的「出手再收回」：前 split 出手，余下收回。 */
    static float outAndBack(float t, float split) {
        return t < split ? smooth(t / split) : 1 - smooth((t - split) / (1 - split));
    }

    /** 举盾时越过盾上沿下刺：另一只手正举着盾。 */
    static boolean overShield(LivingEntity e, InteractionHand attackHand) {
        return e.isBlocking() && e.getUsedItemHand() != attackHand;
    }

    // ───────────── 第三人称 ─────────────

    /** 返回 true 表示已处理（取消原版横挥）。 */
    public static boolean thirdPerson(HumanoidModel<?> m, LivingEntity e) {
        if (m.attackTime <= 0) {
            return false;
        }
        InteractionHand hand = e.swingingArm != null ? e.swingingArm : InteractionHand.MAIN_HAND;
        Optional<AttackStyle> style = AttackStyle.of(e.getItemInHand(hand));
        if (style.isEmpty()) {
            return false;
        }
        HumanoidArm side = hand == InteractionHand.MAIN_HAND ? e.getMainArm() : e.getMainArm().getOpposite();
        ModelPart arm = side == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
        float k = side == HumanoidArm.RIGHT ? 1 : -1;
        float t = m.attackTime;
        float base = arm.xRot;
        switch (style.get()) {
            case STAB -> {
                // 手持物固定在手上：手臂只多抬一点，主要靠整条手臂前送，剑尖始终朝前
                float p = outAndBack(t, 0.4F);
                arm.xRot = Mth.lerp(p, base, base - 0.2F);
                arm.yRot = Mth.lerp(p, arm.yRot, -0.12F * k);
                arm.z -= 4 * p;
            }
            case THRUST -> {
                if (overShield(e, hand)) {
                    // 手举到肩高（0–0.3），矛尖斜向前上越过盾沿 → 前送下压（0.3–0.55）→ 收回
                    if (t < 0.3F) {
                        arm.xRot = Mth.lerp(smooth(t / 0.3F), base, -1.3F);
                    } else if (t < 0.55F) {
                        float d = smooth((t - 0.3F) / 0.25F);
                        arm.xRot = Mth.lerp(d, -1.3F, -1.0F);
                        arm.z -= 5 * d;
                    } else {
                        float r = smooth((t - 0.55F) / 0.45F);
                        arm.xRot = Mth.lerp(r, -1.0F, base);
                        arm.z -= 5 * (1 - r);
                    }
                } else {
                    float p = outAndBack(t, 0.5F);
                    twist(m, 0.3F * k * p);
                    arm.xRot = Mth.lerp(p, base, base - 0.2F);
                    arm.yRot += m.body.yRot;
                    arm.z -= 6 * p;
                }
            }
            case SMASH -> {
                // 慢举过头、棒身后垂（0–0.45）→ 快速砸到身前（0.45–0.65）→ 慢慢收回肩头
                if (t < 0.45F) {
                    arm.xRot = Mth.lerp(smooth(t / 0.45F), base, -2.9F);
                } else if (t < 0.65F) {
                    float d = (t - 0.45F) / 0.2F;
                    arm.xRot = Mth.lerp(d * d, -2.9F, -0.4F);
                    twist(m, -0.15F * k * d);
                } else {
                    float r = smooth((t - 0.65F) / 0.35F);
                    arm.xRot = Mth.lerp(r, -0.4F, base);
                    twist(m, -0.15F * k * (1 - r));
                }
            }
        }
        return true;
    }

    /** 躯干绕竖轴拧转，并按原版的做法把两肩跟着挪（setupAttackAnimation 里的那几行）。 */
    private static void twist(HumanoidModel<?> m, float yRot) {
        m.body.yRot = yRot;
        m.rightArm.z = Mth.sin(yRot) * 5.0F;
        m.rightArm.x = -Mth.cos(yRot) * 5.0F;
        m.leftArm.z = -Mth.sin(yRot) * 5.0F;
        m.leftArm.x = Mth.cos(yRot) * 5.0F;
    }

    // ───────────── 第一人称 ─────────────

    /**
     * 手持物的第一人称变换（{@code IClientItemExtensions.applyForgeHandTransform}）。返回 true 表示已处理。
     * 正在使用这只手的物品时（举盾、拉弓）交回原版，复合弓除外（按自己的满弓刻数画拉弓）。
     */
    public static boolean firstPerson(PoseStack ps, LivingEntity player, HumanoidArm arm, ItemStack stack, float partialTick,
                                      float equip, float swing) {
        int k = arm == HumanoidArm.RIGHT ? 1 : -1;
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        boolean using = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
        if (stack.getItem() instanceof DrawBowItem bow) {
            if (!using) {
                return false;
            }
            bowDraw(ps, player, stack, k, partialTick, equip, bow.fullTicks());
            return true;
        }
        Optional<AttackStyle> style = AttackStyle.of(stack);
        if (style.isEmpty() || using) {
            return false;
        }
        // 原版出手后按攻击冷却把手持物沉下 0.6（equip）；慢武器沉得久会挡住动作，这里只沉三分之一
        ps.translate(k * 0.56F, -0.52F + equip * -0.2F, -0.72F);
        float t = swing;
        // 第一人称镜头离手很近：前送、下压都要小，手持物不能移出画面（x 轴转角为负 = 尖端向前下）
        switch (style.get()) {
            case STAB -> {
                float p = outAndBack(t, 0.4F);
                ps.translate(-k * 0.05F * p, 0.02F * p, -0.25F * p);
                ps.mulPose(Axis.XP.rotationDegrees(-20 * p));
            }
            case THRUST -> {
                if (overShield(player, hand)) {
                    if (t < 0.3F) {
                        float q = smooth(t / 0.3F);
                        ps.translate(-k * 0.1F * q, 0.2F * q, 0);
                        ps.mulPose(Axis.XP.rotationDegrees(15 * q));
                    } else if (t < 0.55F) {
                        float d = smooth((t - 0.3F) / 0.25F);
                        ps.translate(-k * 0.1F, 0.2F - 0.2F * d, -0.35F * d);
                        ps.mulPose(Axis.XP.rotationDegrees(15 - 75 * d));
                    } else {
                        float r = 1 - smooth((t - 0.55F) / 0.45F);
                        ps.translate(-k * 0.1F * r, 0, -0.35F * r);
                        ps.mulPose(Axis.XP.rotationDegrees(-60 * r));
                    }
                } else {
                    float p = outAndBack(t, 0.5F);
                    ps.translate(-k * 0.05F * p, 0, -0.3F * p);
                    ps.mulPose(Axis.XP.rotationDegrees(-30 * p));
                }
            }
            case SMASH -> {
                if (t < 0.45F) {
                    float q = smooth(t / 0.45F);
                    ps.translate(0, 0.15F * q, 0.05F * q);
                    ps.mulPose(Axis.XP.rotationDegrees(35 * q));
                } else if (t < 0.65F) {
                    float d = (t - 0.45F) / 0.2F;
                    d *= d;
                    ps.translate(0, 0.15F - 0.25F * d, 0.05F - 0.2F * d);
                    ps.mulPose(Axis.XP.rotationDegrees(35 - 75 * d));
                } else {
                    float r = 1 - smooth((t - 0.65F) / 0.35F);
                    ps.translate(0, -0.1F * r, -0.15F * r);
                    ps.mulPose(Axis.XP.rotationDegrees(-40 * r));
                }
            }
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
