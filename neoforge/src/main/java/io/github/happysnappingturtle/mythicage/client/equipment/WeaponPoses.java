package io.github.happysnappingturtle.mythicage.client.equipment;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/**
 * 平时的手持姿势（T17 §8.2）：NeoForge 的 ArmPose 扩展，由 META-INF/enumextensions.json 引用下面三个 proxy。
 * 保留一半原版的走路摆臂，看起来不僵。
 */
public final class WeaponPoses {
    @SuppressWarnings("unused") // 由 enumextensions.json 引用
    public static final EnumProxy<HumanoidModel.ArmPose> STAB_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) (model, entity, arm) -> {
                ModelPart a = arm(model, arm);
                a.xRot = a.xRot * 0.5F - 0.45F;
                a.yRot = 0;
            });
    @SuppressWarnings("unused")
    public static final EnumProxy<HumanoidModel.ArmPose> THRUST_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) (model, entity, arm) -> {
                ModelPart a = arm(model, arm);
                a.xRot = a.xRot * 0.3F - 0.3F;
                a.yRot = 0;
            });
    @SuppressWarnings("unused")
    public static final EnumProxy<HumanoidModel.ArmPose> SMASH_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) (model, entity, arm) -> {
                ModelPart a = arm(model, arm);
                a.xRot = a.xRot * 0.2F - 1.45F;
                a.yRot = 0;
            });

    /** GA-8 通用动态姿势；动作与暴击选择来自装备数据。 */
    @SuppressWarnings("unused")
    public static final EnumProxy<HumanoidModel.ArmPose> MOTION_PROXY = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) WeaponAnimations::pose);

    public static HumanoidModel.ArmPose motion() {
        return MOTION_PROXY.getValue();
    }

    private WeaponPoses() {
    }

    private static ModelPart arm(HumanoidModel<?> model, HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
    }

    public static HumanoidModel.ArmPose stab() {
        return STAB_PROXY.getValue();
    }

    public static HumanoidModel.ArmPose thrust() {
        return THRUST_PROXY.getValue();
    }

    public static HumanoidModel.ArmPose smash() {
        return SMASH_PROXY.getValue();
    }
}
