package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.AzureDragonEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class AzureDragonModel extends SinglePartEntityModel<AzureDragonEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftWhisker;
    private final ModelPart rightWhisker;
    private final ModelPart body1;
    private final ModelPart body2;
    private final ModelPart body3;
    private final ModelPart body4;
    private final ModelPart tail;
    private final ModelPart dorsalFin;
    private final ModelPart leftFrontClaw;
    private final ModelPart rightFrontClaw;

    public AzureDragonModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftWhisker = this.head.getChild("left_whisker");
        this.rightWhisker = this.head.getChild("right_whisker");
        this.body1 = root.getChild("body_1");
        this.body2 = root.getChild("body_2");
        this.body3 = root.getChild("body_3");
        this.body4 = root.getChild("body_4");
        this.tail = root.getChild("tail");
        this.dorsalFin = root.getChild("dorsal_fin");
        this.leftFrontClaw = root.getChild("left_front_claw");
        this.rightFrontClaw = root.getChild("right_front_claw");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.25F, -5.1F, -7.7F, 6.5F, 6.4F, 7.9F),
                ModelTransform.pivot(0.0F, 9.0F, -12.0F));
        head.addChild("snout",
                ModelPartBuilder.create().uv(36, 0).cuboid(-2.1F, -2.0F, -11.2F, 4.2F, 3.0F, 3.7F),
                ModelTransform.NONE);
        head.addChild("left_horn",
                ModelPartBuilder.create().uv(62, 0).cuboid(1.8F, -10.5F, -4.0F, 1.2F, 4.7F, 1.5F),
                ModelTransform.NONE);
        head.addChild("right_horn",
                ModelPartBuilder.create().uv(70, 0).cuboid(-3.0F, -10.5F, -4.0F, 1.2F, 4.7F, 1.5F),
                ModelTransform.NONE);
        head.addChild("left_horn_tip",
                ModelPartBuilder.create().uv(62, 12).cuboid(2.1F, -12.6F, -4.2F, 0.8F, 2.3F, 1.2F),
                ModelTransform.NONE);
        head.addChild("right_horn_tip",
                ModelPartBuilder.create().uv(70, 12).cuboid(-2.9F, -12.6F, -4.2F, 0.8F, 2.3F, 1.2F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(118, 0).cuboid(-2.45F, -4.15F, -8.15F, 0.9F, 1.0F, 0.9F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(108, 10).cuboid(1.55F, -4.15F, -8.15F, 0.9F, 1.0F, 0.9F),
                ModelTransform.NONE);
        head.addChild("mouth_line",
                ModelPartBuilder.create().uv(116, 8).cuboid(-2.0F, -1.1F, -11.8F, 4.0F, 0.9F, 1.0F),
                ModelTransform.NONE);
        head.addChild("left_brow",
                ModelPartBuilder.create().uv(108, 20).cuboid(-3.2F, -5.7F, -8.7F, 1.7F, 0.9F, 0.9F),
                ModelTransform.NONE);
        head.addChild("right_brow",
                ModelPartBuilder.create().uv(116, 20).cuboid(1.5F, -5.7F, -8.7F, 1.7F, 0.9F, 0.9F),
                ModelTransform.NONE);

        ModelPartData leftWhisker = head.addChild("left_whisker",
                ModelPartBuilder.create().uv(80, 0).cuboid(0.0F, -0.8F, -0.8F, 6.5F, 1.6F, 1.6F),
                ModelTransform.of(3.5F, -1.4F, -8.4F, 0.0F, -0.18F, 0.15F));
        leftWhisker.addChild("left_whisker_tip",
                ModelPartBuilder.create().uv(80, 10).cuboid(6.0F, -0.55F, -0.55F, 2.8F, 1.1F, 1.1F),
                ModelTransform.NONE);
        ModelPartData rightWhisker = head.addChild("right_whisker",
                ModelPartBuilder.create().uv(98, 0).cuboid(-6.5F, -0.8F, -0.8F, 6.5F, 1.6F, 1.6F),
                ModelTransform.of(-3.5F, -1.4F, -8.4F, 0.0F, 0.18F, -0.15F));
        rightWhisker.addChild("right_whisker_tip",
                ModelPartBuilder.create().uv(98, 10).cuboid(-8.8F, -0.55F, -0.55F, 2.8F, 1.1F, 1.1F),
                ModelTransform.NONE);

        ModelPartData body1 = root.addChild("body_1",
                ModelPartBuilder.create().uv(0, 28).cuboid(-3.2F, -3.0F, 0.0F, 6.4F, 6.0F, 8.0F),
                ModelTransform.pivot(0.0F, 11.0F, -8.0F));
        body1.addChild("body_1_belly_scale",
                ModelPartBuilder.create().uv(0, 54).cuboid(-1.8F, 2.2F, 0.4F, 3.6F, 1.2F, 6.4F),
                ModelTransform.NONE);
        ModelPartData body2 = root.addChild("body_2",
                ModelPartBuilder.create().uv(28, 28).cuboid(-3.0F, -2.8F, 0.0F, 6.0F, 5.6F, 8.0F),
                ModelTransform.pivot(0.0F, 11.0F, 0.0F));
        body2.addChild("body_2_belly_scale",
                ModelPartBuilder.create().uv(22, 54).cuboid(-1.7F, 2.0F, 0.4F, 3.4F, 1.2F, 6.4F),
                ModelTransform.NONE);
        ModelPartData body3 = root.addChild("body_3",
                ModelPartBuilder.create().uv(54, 28).cuboid(-2.8F, -2.6F, 0.0F, 5.6F, 5.2F, 8.0F),
                ModelTransform.pivot(0.0F, 11.0F, 8.0F));
        body3.addChild("body_3_belly_scale",
                ModelPartBuilder.create().uv(44, 54).cuboid(-1.5F, 1.9F, 0.4F, 3.0F, 1.1F, 6.4F),
                ModelTransform.NONE);
        ModelPartData body4 = root.addChild("body_4",
                ModelPartBuilder.create().uv(78, 28).cuboid(-2.5F, -2.4F, 0.0F, 5.0F, 4.8F, 8.0F),
                ModelTransform.pivot(0.0F, 11.0F, 16.0F));
        body4.addChild("body_4_belly_scale",
                ModelPartBuilder.create().uv(0, 68).cuboid(-1.4F, 1.7F, 0.4F, 2.8F, 1.0F, 6.4F),
                ModelTransform.NONE);

        root.addChild("dorsal_fin",
                ModelPartBuilder.create()
                        .uv(0, 78).cuboid(-0.8F, -5.5F, 0.0F, 1.6F, 2.9F, 26.0F),
                ModelTransform.pivot(0.0F, 9.0F, -6.0F));
        root.addChild("dorsal_fin_front",
                ModelPartBuilder.create().uv(0, 92).cuboid(-0.9F, -6.2F, 2.0F, 1.8F, 2.8F, 6.0F),
                ModelTransform.pivot(0.0F, 9.0F, -6.0F));
        root.addChild("dorsal_fin_mid",
                ModelPartBuilder.create().uv(10, 92).cuboid(-0.85F, -6.0F, 10.0F, 1.7F, 2.8F, 8.0F),
                ModelTransform.pivot(0.0F, 9.0F, -6.0F));
        root.addChild("dorsal_fin_back",
                ModelPartBuilder.create().uv(20, 92).cuboid(-0.75F, -5.6F, 20.0F, 1.5F, 2.6F, 8.0F),
                ModelTransform.pivot(0.0F, 9.0F, -6.0F));

        ModelPartData leftFrontClaw = root.addChild("left_front_claw",
                ModelPartBuilder.create().uv(62, 54).cuboid(-0.2F, 0.0F, -1.2F, 3.2F, 4.5F, 3.2F),
                ModelTransform.of(3.0F, 13.0F, -5.0F, 0.25F, 0.0F, -0.15F));
        leftFrontClaw.addChild("left_front_talon",
                ModelPartBuilder.create().uv(62, 66).cuboid(0.5F, 3.8F, -1.7F, 2.6F, 1.0F, 1.0F),
                ModelTransform.NONE);
        ModelPartData rightFrontClaw = root.addChild("right_front_claw",
                ModelPartBuilder.create().uv(78, 54).mirrored().cuboid(-3.0F, 0.0F, -1.2F, 3.2F, 4.5F, 3.2F),
                ModelTransform.of(-3.0F, 13.0F, -5.0F, 0.25F, 0.0F, 0.15F));
        rightFrontClaw.addChild("right_front_talon",
                ModelPartBuilder.create().uv(78, 66).mirrored().cuboid(-3.1F, 3.8F, -1.7F, 2.6F, 1.0F, 1.0F),
                ModelTransform.NONE);
        ModelPartData leftBackClaw = root.addChild("left_back_claw",
                ModelPartBuilder.create().uv(94, 54).cuboid(-0.2F, 0.0F, -1.0F, 3.0F, 4.2F, 3.0F),
                ModelTransform.of(2.5F, 13.0F, 12.0F, 0.18F, 0.0F, -0.12F));
        leftBackClaw.addChild("left_back_talon",
                ModelPartBuilder.create().uv(94, 66).cuboid(0.4F, 3.6F, -1.5F, 2.4F, 1.0F, 1.0F),
                ModelTransform.NONE);
        ModelPartData rightBackClaw = root.addChild("right_back_claw",
                ModelPartBuilder.create().uv(110, 54).mirrored().cuboid(-2.8F, 0.0F, -1.0F, 3.0F, 4.2F, 3.0F),
                ModelTransform.of(-2.5F, 13.0F, 12.0F, 0.18F, 0.0F, 0.12F));
        rightBackClaw.addChild("right_back_talon",
                ModelPartBuilder.create().uv(110, 66).mirrored().cuboid(-2.8F, 3.6F, -1.5F, 2.4F, 1.0F, 1.0F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(36, 78).cuboid(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 12.0F),
                ModelTransform.pivot(0.0F, 11.0F, 24.0F));
        tail.addChild("tail_fin",
                ModelPartBuilder.create().uv(58, 78).cuboid(-4.5F, -0.8F, 8.0F, 9.0F, 1.6F, 8.0F),
                ModelTransform.NONE);
        tail.addChild("tail_fin_left",
                ModelPartBuilder.create().uv(58, 92).cuboid(2.0F, -0.9F, 8.5F, 3.4F, 1.8F, 7.5F),
                ModelTransform.NONE);
        tail.addChild("tail_fin_right",
                ModelPartBuilder.create().uv(76, 92).cuboid(-5.4F, -0.9F, 8.5F, 3.4F, 1.8F, 7.5F),
                ModelTransform.NONE);
        tail.addChild("tail_tip",
                ModelPartBuilder.create().uv(90, 78).cuboid(-1.0F, -1.0F, 12.0F, 2.0F, 2.0F, 4.0F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(AzureDragonEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.6F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.55F;

        float wave = animationProgress * (entity.isFlying() ? 0.18F : 0.09F);
        this.body1.yaw = MathHelper.sin(wave) * 0.10F;
        this.body2.yaw = MathHelper.sin(wave + 0.7F) * 0.16F;
        this.body3.yaw = MathHelper.sin(wave + 1.4F) * 0.18F;
        this.body4.yaw = MathHelper.sin(wave + 2.1F) * 0.2F;
        this.tail.yaw = MathHelper.sin(wave + 2.7F) * 0.24F;
        this.dorsalFin.yaw = this.body3.yaw * 0.35F;

        this.leftWhisker.roll = 0.15F + MathHelper.sin(animationProgress * 0.16F) * 0.08F;
        this.rightWhisker.roll = -0.15F - MathHelper.sin(animationProgress * 0.16F) * 0.08F;
        this.leftFrontClaw.pitch = 0.25F + MathHelper.sin(animationProgress * 0.13F) * 0.08F;
        this.rightFrontClaw.pitch = this.leftFrontClaw.pitch;

        if (entity.handSwingProgress > 0.0F) {
            float strike = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.head.pitch -= 0.35F * strike;
            this.body1.pitch = -0.14F * strike;
        } else {
            this.body1.pitch = 0.0F;
        }
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.1F) * 0.07F;
            this.head.roll = recoil;
            this.body2.roll = -recoil * 0.5F;
            this.body3.roll = recoil * 0.45F;
            this.tail.roll = -recoil * 0.4F;
        } else {
            this.head.roll = 0.0F;
            this.body2.roll = 0.0F;
            this.body3.roll = 0.0F;
            this.tail.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 24.0F);
            this.head.pitch += 0.35F * fall;
            this.body1.pitch = 0.28F * fall;
            this.body2.pitch = 0.2F * fall;
            this.body3.pitch = 0.12F * fall;
            this.body4.roll = 0.45F * fall;
            this.tail.pitch -= 0.25F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
