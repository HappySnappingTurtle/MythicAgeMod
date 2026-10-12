package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.JiangshiEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class JiangshiModel extends SinglePartEntityModel<JiangshiEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart talisman;
    private final ModelPart body;
    private final ModelPart robe;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public JiangshiModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.hat = this.head.getChild("official_hat");
        this.talisman = this.head.getChild("talisman");
        this.body = root.getChild("body");
        this.robe = root.getChild("robe");
        this.leftArm = root.getChild("left_stiff_arm");
        this.rightArm = root.getChild("right_stiff_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.1F, -7.0F, -3.55F, 6.2F, 8.0F, 7.1F),
                ModelTransform.pivot(0.0F, 7.5F, -1.0F));
        head.addChild("pale_face",
                ModelPartBuilder.create()
                        .uv(24, 0).cuboid(-2.6F, -5.9F, -4.2F, 5.2F, 4.8F, 0.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("left_eye",
                ModelPartBuilder.create()
                        .uv(32, 0).cuboid(-2.2F, -4.8F, -4.4F, 1.1F, 1.1F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("right_eye",
                ModelPartBuilder.create()
                        .uv(38, 0).cuboid(1.1F, -4.8F, -4.4F, 1.1F, 1.1F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("mouth_line",
                ModelPartBuilder.create()
                        .uv(44, 0).cuboid(-1.45F, -2.2F, -4.45F, 2.9F, 0.9F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData officialHat = head.addChild("official_hat",
                ModelPartBuilder.create()
                        .uv(0, 18).cuboid(-4.0F, -2.0F, -4.4F, 8.0F, 2.2F, 8.8F),
                ModelTransform.pivot(0.0F, -7.0F, 0.0F));
        officialHat.addChild("hat_crown",
                ModelPartBuilder.create()
                        .uv(40, 18).cuboid(-3.1F, -5.0F, -3.6F, 6.2F, 3.2F, 7.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        officialHat.addChild("hat_tail",
                ModelPartBuilder.create()
                        .uv(72, 18).cuboid(-1.2F, -6.4F, 2.6F, 2.4F, 2.4F, 3.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        officialHat.addChild("left_hat_wing",
                ModelPartBuilder.create()
                        .uv(82, 18).cuboid(3.55F, -1.4F, -1.4F, 2.9F, 1.6F, 2.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        officialHat.addChild("right_hat_wing",
                ModelPartBuilder.create()
                        .uv(82, 24).cuboid(-6.45F, -1.4F, -1.4F, 2.9F, 1.6F, 2.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData talisman = head.addChild("talisman",
                ModelPartBuilder.create()
                        .uv(90, 18).cuboid(-1.35F, -5.4F, -4.6F, 2.7F, 5.9F, 0.55F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        talisman.addChild("talisman_mark_top",
                ModelPartBuilder.create()
                        .uv(100, 18).cuboid(-0.95F, -4.9F, -5.1F, 1.9F, 0.75F, 0.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        talisman.addChild("talisman_mark_mid",
                ModelPartBuilder.create()
                        .uv(100, 22).cuboid(-0.95F, -3.55F, -5.1F, 1.9F, 0.75F, 0.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        talisman.addChild("talisman_mark_low",
                ModelPartBuilder.create()
                        .uv(100, 26).cuboid(-0.95F, -2.2F, -5.1F, 1.9F, 0.75F, 0.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 40).cuboid(-4.2F, -0.5F, -2.3F, 8.4F, 12.0F, 4.6F),
                ModelTransform.pivot(0.0F, 8.0F, 0.0F));
        body.addChild("robe_collar",
                ModelPartBuilder.create()
                        .uv(28, 40).cuboid(-3.6F, -0.8F, -2.6F, 7.2F, 2.0F, 5.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData robe = root.addChild("robe",
                ModelPartBuilder.create()
                        .uv(58, 40).cuboid(-4.8F, -0.2F, -2.8F, 9.6F, 13.0F, 5.6F),
                ModelTransform.pivot(0.0F, 8.4F, 0.0F));
        robe.addChild("robe_lower_hem",
                ModelPartBuilder.create()
                        .uv(92, 40).cuboid(-5.2F, 8.6F, -3.0F, 10.4F, 3.0F, 6.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        robe.addChild("robe_front_panel",
                ModelPartBuilder.create()
                        .uv(0, 70).cuboid(-1.0F, 0.0F, -3.25F, 2.0F, 12.0F, 0.6F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData leftArm = root.addChild("left_stiff_arm",
                ModelPartBuilder.create()
                        .uv(12, 70).cuboid(-1.3F, -1.3F, -0.8F, 2.6F, 2.6F, 9.8F),
                ModelTransform.of(4.7F, 9.2F, -3.1F, -1.25F, -0.08F, -0.05F));
        leftArm.addChild("left_cuff",
                ModelPartBuilder.create()
                        .uv(38, 70).cuboid(-1.6F, -1.6F, 7.4F, 3.2F, 3.2F, 2.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData rightArm = root.addChild("right_stiff_arm",
                ModelPartBuilder.create()
                        .uv(54, 70).cuboid(-1.3F, -1.3F, -0.8F, 2.6F, 2.6F, 9.8F),
                ModelTransform.of(-4.7F, 9.2F, -3.1F, -1.25F, 0.08F, 0.05F));
        rightArm.addChild("right_cuff",
                ModelPartBuilder.create()
                        .uv(80, 70).cuboid(-1.6F, -1.6F, 7.4F, 3.2F, 3.2F, 2.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 92).cuboid(-1.4F, 0.0F, -1.4F, 2.8F, 7.2F, 2.8F),
                ModelTransform.pivot(2.0F, 16.2F, 0.0F));
        leftLeg.addChild("left_boot",
                ModelPartBuilder.create()
                        .uv(12, 92).cuboid(-1.7F, 6.4F, -1.9F, 3.4F, 1.4F, 3.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(30, 92).cuboid(-1.4F, 0.0F, -1.4F, 2.8F, 7.2F, 2.8F),
                ModelTransform.pivot(-2.0F, 16.2F, 0.0F));
        rightLeg.addChild("right_boot",
                ModelPartBuilder.create()
                        .uv(42, 92).cuboid(-1.7F, 6.4F, -1.9F, 3.4F, 1.4F, 3.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(JiangshiEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.45F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.45F;
        this.hat.roll = MathHelper.sin(animationProgress * 0.08F) * 0.035F;
        this.talisman.pitch = MathHelper.sin(animationProgress * 0.12F) * 0.035F;

        float hop = MathHelper.abs(MathHelper.sin(limbAngle * 0.9F)) * limbDistance;
        this.body.pitch = 0.04F + hop * 0.08F;
        this.robe.pitch = this.body.pitch;
        this.leftArm.pitch = -1.25F - hop * 0.12F;
        this.rightArm.pitch = -1.25F - hop * 0.12F;
        this.leftArm.roll = -0.05F + MathHelper.sin(animationProgress * 0.1F) * 0.02F;
        this.rightArm.roll = 0.05F - MathHelper.sin(animationProgress * 0.1F) * 0.02F;
        this.leftLeg.pitch = hop * 0.35F;
        this.rightLeg.pitch = hop * 0.35F;

        if (entity.handSwingProgress > 0.0F) {
            float shove = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.leftArm.pitch -= 0.35F * shove;
            this.rightArm.pitch -= 0.35F * shove;
            this.head.pitch += 0.1F * shove;
        }
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.2F) * 0.08F;
            this.head.roll = recoil;
            this.body.roll = recoil * 0.45F;
            this.talisman.roll = -recoil * 0.7F;
        } else {
            this.head.roll = 0.0F;
            this.body.roll = 0.0F;
            this.talisman.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 0.95F * fall;
            this.head.pitch += 0.25F * fall;
            this.leftArm.pitch = -0.45F * fall;
            this.rightArm.pitch = -0.45F * fall;
            this.talisman.pitch += 0.2F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
