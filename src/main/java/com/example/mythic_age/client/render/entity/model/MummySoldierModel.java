package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.MummySoldierEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class MummySoldierModel extends SinglePartEntityModel<MummySoldierEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart spear;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public MummySoldierModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.spear = this.rightArm.getChild("spear");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-2.8F, -8.0F, -3.65F, 5.6F, 8.0F, 7.3F),
                ModelTransform.pivot(0.0F, 1.0F, 0.0F));
        head.addChild("head_bandage_upper",
                ModelPartBuilder.create().uv(34, 0).cuboid(-3.05F, -6.9F, -4.0F, 6.1F, 1.2F, 7.8F),
                ModelTransform.NONE);
        head.addChild("head_bandage_lower",
                ModelPartBuilder.create().uv(34, 10).cuboid(-2.95F, -3.2F, -4.0F, 5.9F, 1.0F, 7.8F),
                ModelTransform.NONE);
        head.addChild("head_bandage_mid",
                ModelPartBuilder.create().uv(34, 16).cuboid(-2.85F, -4.5F, -4.15F, 5.7F, 0.9F, 1.0F),
                ModelTransform.NONE);
        head.addChild("head_bandage_diagonal",
                ModelPartBuilder.create().uv(54, 16).cuboid(-2.3F, -7.4F, -4.6F, 1.65F, 7.0F, 1.0F),
                ModelTransform.NONE);
        head.addChild("mouth_shadow",
                ModelPartBuilder.create().uv(66, 16).cuboid(-1.45F, -2.1F, -4.6F, 2.9F, 0.9F, 1.0F),
                ModelTransform.NONE);
        head.addChild("left_cheek_wrap",
                ModelPartBuilder.create().uv(76, 16).cuboid(-2.45F, -3.5F, -4.25F, 1.1F, 2.3F, 1.0F),
                ModelTransform.NONE);
        head.addChild("right_cheek_wrap",
                ModelPartBuilder.create().uv(84, 16).cuboid(1.35F, -3.5F, -4.25F, 1.1F, 2.3F, 1.0F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(86, 0).cuboid(-2.05F, -5.2F, -4.15F, 1.1F, 1.4F, 0.8F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(92, 0).cuboid(0.95F, -5.2F, -4.15F, 1.1F, 1.4F, 0.8F),
                ModelTransform.NONE);

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 24).cuboid(-4.0F, -10.0F, -2.1F, 8.0F, 12.0F, 4.2F),
                ModelTransform.pivot(0.0F, 8.0F, 0.0F));
        body.addChild("bandage_chest",
                ModelPartBuilder.create().uv(28, 24).cuboid(-4.4F, -8.5F, -2.5F, 8.8F, 1.1F, 5.0F),
                ModelTransform.NONE);
        body.addChild("bandage_mid",
                ModelPartBuilder.create().uv(28, 30).cuboid(-4.3F, -5.5F, -2.5F, 8.6F, 1.1F, 5.0F),
                ModelTransform.NONE);
        body.addChild("bandage_low",
                ModelPartBuilder.create().uv(28, 36).cuboid(-4.2F, -1.8F, -2.4F, 8.4F, 1.2F, 4.8F),
                ModelTransform.NONE);
        body.addChild("waist_wrap",
                ModelPartBuilder.create().uv(92, 16).cuboid(-4.1F, 0.2F, -2.55F, 8.2F, 1.6F, 0.9F),
                ModelTransform.NONE);

        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(60, 20).cuboid(-0.2F, -1.0F, -1.5F, 2.4F, 12.2F, 3.0F),
                ModelTransform.pivot(4.0F, 7.2F, 0.0F));
        leftArm.addChild("left_arm_bandage_upper",
                ModelPartBuilder.create().uv(60, 36).cuboid(-0.5F, 2.2F, -1.8F, 3.0F, 1.0F, 3.6F),
                ModelTransform.NONE);
        leftArm.addChild("left_arm_bandage_lower",
                ModelPartBuilder.create().uv(60, 42).cuboid(-0.5F, 6.6F, -1.8F, 3.0F, 1.0F, 3.6F),
                ModelTransform.NONE);

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(74, 20).cuboid(-2.2F, -1.0F, -1.5F, 2.4F, 12.2F, 3.0F),
                ModelTransform.pivot(-4.0F, 7.2F, 0.0F));
        rightArm.addChild("right_arm_bandage_upper",
                ModelPartBuilder.create().uv(74, 36).cuboid(-2.5F, 2.2F, -1.8F, 3.0F, 1.0F, 3.6F),
                ModelTransform.NONE);
        rightArm.addChild("right_arm_bandage_lower",
                ModelPartBuilder.create().uv(74, 42).cuboid(-2.5F, 6.6F, -1.8F, 3.0F, 1.0F, 3.6F),
                ModelTransform.NONE);

        ModelPartData spear = rightArm.addChild("spear",
                ModelPartBuilder.create()
                        .uv(96, 0).cuboid(-0.5F, -7.0F, -0.5F, 1.0F, 18.0F, 1.0F),
                ModelTransform.pivot(-1.0F, 7.0F, -1.0F));
        spear.addChild("spear_tip",
                ModelPartBuilder.create().uv(102, 0).cuboid(-1.5F, -10.0F, -0.6F, 3.0F, 3.2F, 1.2F),
                ModelTransform.NONE);
        spear.addChild("spear_tassel",
                ModelPartBuilder.create().uv(112, 0).cuboid(-1.8F, -8.8F, -1.2F, 3.6F, 1.6F, 2.4F),
                ModelTransform.NONE);

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 48).cuboid(-1.6F, 0.0F, -1.5F, 3.2F, 11.0F, 3.0F)
                        .uv(28, 48).cuboid(-1.9F, 3.8F, -1.8F, 3.8F, 1.0F, 3.6F)
                        .uv(28, 54).cuboid(-1.9F, 8.0F, -1.8F, 3.8F, 1.0F, 3.6F),
                ModelTransform.pivot(2.0F, 12.5F, 0.0F));
        leftLeg.addChild("left_leg_bandage",
                ModelPartBuilder.create().uv(28, 48).cuboid(-1.9F, 7.0F, -1.8F, 3.8F, 1.2F, 3.6F),
                ModelTransform.NONE);

        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(14, 48).cuboid(-1.6F, 0.0F, -1.5F, 3.2F, 11.0F, 3.0F)
                        .uv(42, 48).cuboid(-1.9F, 3.8F, -1.8F, 3.8F, 1.0F, 3.6F)
                        .uv(42, 54).cuboid(-1.9F, 8.0F, -1.8F, 3.8F, 1.0F, 3.6F),
                ModelTransform.pivot(-2.0F, 12.5F, 0.0F));
        rightLeg.addChild("right_leg_bandage",
                ModelPartBuilder.create().uv(42, 48).cuboid(-1.9F, 7.0F, -1.8F, 3.8F, 1.2F, 3.6F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(MummySoldierEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) + MathHelper.sin(animationProgress * 0.08F) * 0.04F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        this.body.pitch = 0.08F;

        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.52F) * 0.55F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.52F + MathHelper.PI) * 0.55F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.52F + MathHelper.PI) * 0.45F * limbDistance - 0.18F;
        this.rightArm.pitch = -0.75F + MathHelper.cos(limbAngle * 0.52F) * 0.28F * limbDistance;
        this.rightArm.yaw = -0.12F;
        this.spear.pitch = -0.32F;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.45F - swing * 0.75F;
            this.rightArm.yaw = -0.32F + swing * 0.18F;
            this.spear.pitch = -0.92F;
        }
        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 1.8F) * 0.08F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.0F * fall;
            this.head.pitch += 0.28F * fall;
            this.leftArm.pitch = -0.15F * fall;
            this.rightArm.pitch = -0.55F * fall;
            this.spear.pitch = -0.15F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
