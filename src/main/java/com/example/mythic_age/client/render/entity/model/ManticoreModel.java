package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.ManticoreEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class ManticoreModel extends SinglePartEntityModel<ManticoreEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart shoulderRidge;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart noseMouth;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftFang;
    private final ModelPart rightFang;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftCheekSpike;
    private final ModelPart rightCheekSpike;
    private final ModelPart leftWing;
    private final ModelPart leftWingMembraneA;
    private final ModelPart leftWingMembraneB;
    private final ModelPart leftWingBoneFront;
    private final ModelPart leftWingBoneMid;
    private final ModelPart leftWingBoneOuter;
    private final ModelPart leftWingClaw;
    private final ModelPart rightWing;
    private final ModelPart rightWingMembraneA;
    private final ModelPart rightWingMembraneB;
    private final ModelPart rightWingBoneFront;
    private final ModelPart rightWingBoneMid;
    private final ModelPart rightWingBoneOuter;
    private final ModelPart rightWingClaw;
    private final ModelPart tail;
    private final ModelPart tailBase;
    private final ModelPart tailRise;
    private final ModelPart tailCurve;
    private final ModelPart tailHook;
    private final ModelPart tailSting;
    private final ModelPart tailStingTip;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontLeftClaws;
    private final ModelPart frontRightLeg;
    private final ModelPart frontRightClaws;
    private final ModelPart backLeftLeg;
    private final ModelPart backLeftClaws;
    private final ModelPart backRightLeg;
    private final ModelPart backRightClaws;

    public ManticoreModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.shoulderRidge = this.body.getChild("shoulder_ridge");
        this.head = root.getChild("head");
        this.snout = this.head.getChild("snout");
        this.noseMouth = this.head.getChild("nose_mouth");
        this.leftEye = this.head.getChild("left_eye");
        this.rightEye = this.head.getChild("right_eye");
        this.leftFang = this.head.getChild("left_fang");
        this.rightFang = this.head.getChild("right_fang");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftCheekSpike = this.head.getChild("left_cheek_spike");
        this.rightCheekSpike = this.head.getChild("right_cheek_spike");
        this.leftWing = root.getChild("left_wing");
        this.leftWingMembraneA = this.leftWing.getChild("left_wing_membrane_a");
        this.leftWingMembraneB = this.leftWing.getChild("left_wing_membrane_b");
        this.leftWingBoneFront = this.leftWing.getChild("left_wing_bone_front");
        this.leftWingBoneMid = this.leftWing.getChild("left_wing_bone_mid");
        this.leftWingBoneOuter = this.leftWing.getChild("left_wing_bone_outer");
        this.leftWingClaw = this.leftWing.getChild("left_wing_claw");
        this.rightWing = root.getChild("right_wing");
        this.rightWingMembraneA = this.rightWing.getChild("right_wing_membrane_a");
        this.rightWingMembraneB = this.rightWing.getChild("right_wing_membrane_b");
        this.rightWingBoneFront = this.rightWing.getChild("right_wing_bone_front");
        this.rightWingBoneMid = this.rightWing.getChild("right_wing_bone_mid");
        this.rightWingBoneOuter = this.rightWing.getChild("right_wing_bone_outer");
        this.rightWingClaw = this.rightWing.getChild("right_wing_claw");
        this.tail = root.getChild("tail");
        this.tailBase = this.tail.getChild("tail_base");
        this.tailRise = this.tail.getChild("tail_rise");
        this.tailCurve = this.tail.getChild("tail_curve");
        this.tailHook = this.tail.getChild("tail_hook");
        this.tailSting = this.tail.getChild("tail_sting");
        this.tailStingTip = this.tailSting.getChild("tail_sting_tip");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontLeftClaws = this.frontLeftLeg.getChild("front_left_claws");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.frontRightClaws = this.frontRightLeg.getChild("front_right_claws");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backLeftClaws = this.backLeftLeg.getChild("back_left_claws");
        this.backRightLeg = root.getChild("back_right_leg");
        this.backRightClaws = this.backRightLeg.getChild("back_right_claws");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 30).cuboid(-5.7F, -7.4F, -11.0F, 11.4F, 9.8F, 20.8F)
                        .uv(50, 30).cuboid(-4.8F, -8.8F, -10.0F, 9.6F, 2.0F, 6.0F),
                ModelTransform.pivot(0.0F, 14.0F, 1.0F));
        body.addChild("shoulder_ridge",
                ModelPartBuilder.create().uv(58, 38).cuboid(-4.7F, -9.3F, -8.2F, 9.4F, 3.1F, 12.0F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.2F, -5.8F, -7.0F, 8.4F, 7.9F, 8.4F),
                ModelTransform.pivot(0.0F, 9.5F, -8.5F));
        head.addChild("snout",
                ModelPartBuilder.create().uv(30, 0).cuboid(-3.05F, -1.1F, -10.2F, 6.1F, 3.4F, 4.5F),
                ModelTransform.NONE);
        head.addChild("nose_mouth",
                ModelPartBuilder.create().uv(52, 0).cuboid(-2.05F, 0.5F, -10.7F, 4.1F, 1.55F, 1.1F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(112, 0).cuboid(-3.05F, -3.95F, -7.45F, 1.15F, 1.15F, 0.95F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(118, 0).cuboid(1.9F, -3.95F, -7.45F, 1.15F, 1.15F, 0.95F),
                ModelTransform.NONE);
        head.addChild("left_fang",
                ModelPartBuilder.create().uv(112, 6).cuboid(-2.3F, 1.55F, -10.7F, 1.05F, 2.15F, 1.05F),
                ModelTransform.NONE);
        head.addChild("right_fang",
                ModelPartBuilder.create().uv(118, 6).cuboid(1.25F, 1.55F, -10.7F, 1.05F, 2.15F, 1.05F),
                ModelTransform.NONE);
        head.addChild("left_ear",
                ModelPartBuilder.create().uv(64, 0).cuboid(2.8F, -8.2F, -4.05F, 2.0F, 3.8F, 2.2F),
                ModelTransform.NONE);
        head.addChild("right_ear",
                ModelPartBuilder.create().uv(72, 0).cuboid(-4.8F, -8.2F, -4.05F, 2.0F, 3.8F, 2.2F),
                ModelTransform.NONE);
        head.addChild("left_cheek_spike",
                ModelPartBuilder.create().uv(80, 0).cuboid(3.9F, -2.35F, -6.0F, 1.8F, 1.55F, 1.6F),
                ModelTransform.NONE);
        head.addChild("right_cheek_spike",
                ModelPartBuilder.create().uv(86, 0).cuboid(-5.7F, -2.35F, -6.0F, 1.8F, 1.55F, 1.6F),
                ModelTransform.NONE);

        ModelPartData leftWing = root.addChild("left_wing", ModelPartBuilder.create(), ModelTransform.pivot(4.5F, 8.0F, -1.0F));
        leftWing.addChild("left_wing_membrane_a",
                ModelPartBuilder.create().uv(68, 0).cuboid(0.0F, -5.2F, -5.4F, 14.8F, 1.9F, 10.2F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_membrane_b",
                ModelPartBuilder.create().uv(68, 16).cuboid(7.2F, -3.7F, 2.8F, 12.8F, 1.7F, 9.2F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_bone_front",
                ModelPartBuilder.create().uv(0, 92).cuboid(0.0F, -6.0F, -6.0F, 19.0F, 1.6F, 2.0F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_bone_mid",
                ModelPartBuilder.create().uv(40, 92).cuboid(5.0F, -5.2F, -4.2F, 1.5F, 2.2F, 15.2F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_bone_outer",
                ModelPartBuilder.create().uv(48, 92).cuboid(12.2F, -4.6F, -3.2F, 1.5F, 2.3F, 15.2F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_claw",
                ModelPartBuilder.create().uv(58, 92).cuboid(19.0F, -4.4F, 10.0F, 2.0F, 2.4F, 2.2F),
                ModelTransform.NONE);

        ModelPartData rightWing = root.addChild("right_wing", ModelPartBuilder.create(), ModelTransform.pivot(-4.5F, 8.0F, -1.0F));
        rightWing.addChild("right_wing_membrane_a",
                ModelPartBuilder.create().uv(68, 32).mirrored().cuboid(-14.8F, -5.2F, -5.4F, 14.8F, 1.9F, 10.2F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_membrane_b",
                ModelPartBuilder.create().uv(68, 48).mirrored().cuboid(-20.0F, -3.7F, 2.8F, 12.8F, 1.7F, 9.2F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_bone_front",
                ModelPartBuilder.create().uv(0, 98).mirrored().cuboid(-19.0F, -6.0F, -6.0F, 19.0F, 1.6F, 2.0F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_bone_mid",
                ModelPartBuilder.create().uv(40, 98).mirrored().cuboid(-6.5F, -5.2F, -4.2F, 1.5F, 2.2F, 15.2F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_bone_outer",
                ModelPartBuilder.create().uv(48, 98).mirrored().cuboid(-13.7F, -4.6F, -3.2F, 1.5F, 2.3F, 15.2F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_claw",
                ModelPartBuilder.create().uv(58, 98).mirrored().cuboid(-21.0F, -4.4F, 10.0F, 2.0F, 2.4F, 2.2F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 9.5F, 9.0F));
        tail.addChild("tail_base",
                ModelPartBuilder.create().uv(70, 50).cuboid(-1.3F, -2.0F, 0.0F, 2.6F, 2.8F, 10.0F),
                ModelTransform.NONE);
        tail.addChild("tail_rise",
                ModelPartBuilder.create().uv(82, 50).cuboid(-1.2F, -11.5F, 7.5F, 2.4F, 10.1F, 2.9F),
                ModelTransform.NONE);
        tail.addChild("tail_curve",
                ModelPartBuilder.create().uv(94, 50).cuboid(-1.5F, -13.3F, 3.4F, 3.0F, 3.3F, 6.8F),
                ModelTransform.NONE);
        tail.addChild("tail_hook",
                ModelPartBuilder.create().uv(106, 50).cuboid(-1.6F, -15.0F, 0.8F, 3.2F, 3.0F, 4.0F),
                ModelTransform.NONE);
        ModelPartData tailSting = tail.addChild("tail_sting",
                ModelPartBuilder.create().uv(104, 58).cuboid(-2.4F, -16.3F, -0.4F, 4.8F, 3.9F, 4.2F),
                ModelTransform.NONE);
        tailSting.addChild("tail_sting_tip",
                ModelPartBuilder.create().uv(116, 58).cuboid(-1.0F, -15.3F, -2.8F, 2.0F, 2.2F, 2.8F),
                ModelTransform.NONE);

        ModelPartData frontLeftLeg = root.addChild("front_left_leg", ModelPartBuilder.create().uv(0, 60).cuboid(-2.0F, 0.0F, -2.2F, 4.2F, 10.5F, 4.5F), ModelTransform.pivot(3.1F, 14.0F, -5.5F));
        frontLeftLeg.addChild("front_left_claws", ModelPartBuilder.create().uv(0, 78).cuboid(-2.3F, 9.2F, -3.1F, 4.8F, 1.6F, 2.1F), ModelTransform.NONE);
        ModelPartData frontRightLeg = root.addChild("front_right_leg", ModelPartBuilder.create().uv(18, 60).mirrored().cuboid(-2.2F, 0.0F, -2.2F, 4.2F, 10.5F, 4.5F), ModelTransform.pivot(-3.1F, 14.0F, -5.5F));
        frontRightLeg.addChild("front_right_claws", ModelPartBuilder.create().uv(18, 78).mirrored().cuboid(-2.5F, 9.2F, -3.1F, 4.8F, 1.6F, 2.1F), ModelTransform.NONE);
        ModelPartData backLeftLeg = root.addChild("back_left_leg", ModelPartBuilder.create().uv(36, 60).cuboid(-2.0F, 0.0F, -2.0F, 4.2F, 10.5F, 4.5F), ModelTransform.pivot(3.2F, 14.0F, 6.5F));
        backLeftLeg.addChild("back_left_claws", ModelPartBuilder.create().uv(36, 78).cuboid(-2.3F, 9.2F, -3.0F, 4.8F, 1.6F, 2.1F), ModelTransform.NONE);
        ModelPartData backRightLeg = root.addChild("back_right_leg", ModelPartBuilder.create().uv(54, 60).mirrored().cuboid(-2.2F, 0.0F, -2.0F, 4.2F, 10.5F, 4.5F), ModelTransform.pivot(-3.2F, 14.0F, 6.5F));
        backRightLeg.addChild("back_right_claws", ModelPartBuilder.create().uv(54, 78).mirrored().cuboid(-2.5F, 9.2F, -3.0F, 4.8F, 1.6F, 2.1F), ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(ManticoreEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        this.snout.pitch = 0.0F;
        this.noseMouth.pitch = 0.0F;
        this.leftEye.pitch = 0.0F;
        this.rightEye.pitch = 0.0F;
        this.leftFang.pitch = 0.0F;
        this.rightFang.pitch = 0.0F;
        this.leftEar.pitch = MathHelper.sin(animationProgress * 0.11F) * 0.02F;
        this.rightEar.pitch = -this.leftEar.pitch;
        this.leftCheekSpike.pitch = 0.0F;
        this.rightCheekSpike.pitch = 0.0F;

        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.8F) * 1.05F * limbDistance;
        this.backRightLeg.pitch = this.frontLeftLeg.pitch;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.8F + MathHelper.PI) * 1.05F * limbDistance;
        this.backLeftLeg.pitch = this.frontRightLeg.pitch;
        this.frontLeftClaws.pitch = 0.0F;
        this.frontRightClaws.pitch = 0.0F;
        this.backLeftClaws.pitch = 0.0F;
        this.backRightClaws.pitch = 0.0F;

        float wing = MathHelper.sin(animationProgress * 0.22F) * 0.22F;
        this.leftWing.roll = -0.35F - wing;
        this.rightWing.roll = 0.35F + wing;
        resetWingChildren();

        float tailMotion = MathHelper.sin(animationProgress * 0.16F);
        this.tail.pitch = -0.55F + tailMotion * 0.12F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.12F) * 0.1F;
        this.tailBase.pitch = 0.0F;
        this.tailRise.pitch = tailMotion * 0.04F;
        this.tailCurve.pitch = -0.08F + tailMotion * 0.05F;
        this.tailHook.pitch = -0.14F + MathHelper.cos(animationProgress * 0.18F) * 0.04F;
        this.tailSting.pitch = -0.35F + MathHelper.cos(animationProgress * 0.2F) * 0.08F;
        this.tailSting.yaw = MathHelper.sin(animationProgress * 0.2F) * 0.22F;
        this.tailStingTip.pitch = 0.0F;
        this.shoulderRidge.pitch = 0.0F;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.tail.pitch = -1.05F;
            this.tailHook.pitch -= swing * 0.35F;
            this.tailSting.pitch = -1.25F - swing * 0.25F;
            this.head.pitch -= swing * 0.18F;
        }
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.0F) * 0.08F;
            this.head.roll = recoil;
            this.tailSting.yaw -= recoil * 1.4F;
            this.leftWing.roll += recoil;
            this.rightWing.roll += recoil;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.0F * fall;
            this.leftWing.roll = -1.3F * fall;
            this.rightWing.roll = 1.3F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    private void resetWingChildren() {
        this.leftWingMembraneA.pitch = 0.0F;
        this.leftWingMembraneB.pitch = 0.0F;
        this.leftWingBoneFront.pitch = 0.0F;
        this.leftWingBoneMid.pitch = 0.0F;
        this.leftWingBoneOuter.pitch = 0.0F;
        this.leftWingClaw.pitch = 0.0F;
        this.rightWingMembraneA.pitch = 0.0F;
        this.rightWingMembraneB.pitch = 0.0F;
        this.rightWingBoneFront.pitch = 0.0F;
        this.rightWingBoneMid.pitch = 0.0F;
        this.rightWingBoneOuter.pitch = 0.0F;
        this.rightWingClaw.pitch = 0.0F;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
