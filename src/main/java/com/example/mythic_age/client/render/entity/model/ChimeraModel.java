package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.ChimeraEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class ChimeraModel extends SinglePartEntityModel<ChimeraEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart shoulderFireCrest;
    private final ModelPart lionHead;
    private final ModelPart lionMane;
    private final ModelPart lionSnout;
    private final ModelPart lionNoseMouth;
    private final ModelPart lionLeftEye;
    private final ModelPart lionRightEye;
    private final ModelPart lionLeftFang;
    private final ModelPart lionRightFang;
    private final ModelPart goatNeck;
    private final ModelPart goatHead;
    private final ModelPart goatSnout;
    private final ModelPart goatLeftEye;
    private final ModelPart goatRightEye;
    private final ModelPart goatLeftHorn;
    private final ModelPart goatRightHorn;
    private final ModelPart goatBeard;
    private final ModelPart tailBase;
    private final ModelPart tailRise;
    private final ModelPart tailTip;
    private final ModelPart serpentHead;
    private final ModelPart serpentUpperJaw;
    private final ModelPart serpentLowerJaw;
    private final ModelPart serpentLeftEye;
    private final ModelPart serpentRightEye;
    private final ModelPart snakeTongue;
    private final ModelPart leftWing;
    private final ModelPart leftWingMembrane;
    private final ModelPart leftWingBone;
    private final ModelPart leftWingClaw;
    private final ModelPart rightWing;
    private final ModelPart rightWingMembrane;
    private final ModelPart rightWingBone;
    private final ModelPart rightWingClaw;
    private final ModelPart tail;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontLeftClaws;
    private final ModelPart frontRightLeg;
    private final ModelPart frontRightClaws;
    private final ModelPart backLeftLeg;
    private final ModelPart backLeftClaws;
    private final ModelPart backRightLeg;
    private final ModelPart backRightClaws;

    public ChimeraModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.shoulderFireCrest = this.body.getChild("shoulder_fire_crest");
        this.lionHead = root.getChild("lion_head");
        this.lionMane = this.lionHead.getChild("lion_mane");
        this.lionSnout = this.lionHead.getChild("lion_snout");
        this.lionNoseMouth = this.lionHead.getChild("lion_nose_mouth");
        this.lionLeftEye = this.lionHead.getChild("lion_left_eye");
        this.lionRightEye = this.lionHead.getChild("lion_right_eye");
        this.lionLeftFang = this.lionHead.getChild("lion_left_fang");
        this.lionRightFang = this.lionHead.getChild("lion_right_fang");
        this.goatNeck = root.getChild("goat_neck");
        this.goatHead = root.getChild("goat_head");
        this.goatSnout = this.goatHead.getChild("goat_snout");
        this.goatLeftEye = this.goatHead.getChild("goat_left_eye");
        this.goatRightEye = this.goatHead.getChild("goat_right_eye");
        this.goatLeftHorn = this.goatHead.getChild("goat_left_horn");
        this.goatRightHorn = this.goatHead.getChild("goat_right_horn");
        this.goatBeard = this.goatHead.getChild("goat_beard");
        this.tail = root.getChild("tail");
        this.tailBase = this.tail.getChild("tail_base");
        this.tailRise = this.tail.getChild("tail_rise");
        this.tailTip = this.tail.getChild("tail_tip");
        this.serpentHead = this.tail.getChild("serpent_head");
        this.serpentUpperJaw = this.serpentHead.getChild("serpent_upper_jaw");
        this.serpentLowerJaw = this.serpentHead.getChild("serpent_lower_jaw");
        this.serpentLeftEye = this.serpentHead.getChild("serpent_left_eye");
        this.serpentRightEye = this.serpentHead.getChild("serpent_right_eye");
        this.snakeTongue = this.serpentHead.getChild("snake_tongue");
        this.leftWing = root.getChild("left_wing");
        this.leftWingMembrane = this.leftWing.getChild("left_wing_membrane");
        this.leftWingBone = this.leftWing.getChild("left_wing_bone");
        this.leftWingClaw = this.leftWing.getChild("left_wing_claw");
        this.rightWing = root.getChild("right_wing");
        this.rightWingMembrane = this.rightWing.getChild("right_wing_membrane");
        this.rightWingBone = this.rightWing.getChild("right_wing_bone");
        this.rightWingClaw = this.rightWing.getChild("right_wing_claw");
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
                        .uv(0, 32).cuboid(-6.8F, -8.2F, -12.0F, 13.6F, 11.4F, 22.8F)
                        .uv(74, 32).cuboid(-5.4F, -10.0F, -11.0F, 10.8F, 2.4F, 7.2F),
                ModelTransform.pivot(0.0F, 13.0F, 1.5F));
        body.addChild("shoulder_fire_crest",
                ModelPartBuilder.create().uv(74, 44).cuboid(-1.0F, -13.0F, -8.0F, 2.0F, 5.0F, 13.4F),
                ModelTransform.NONE);
        ModelPartData lionHead = root.addChild("lion_head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-5.2F, -6.3F, -7.8F, 10.4F, 9.1F, 9.4F),
                ModelTransform.pivot(0.0F, 8.0F, -9.0F));
        lionHead.addChild("lion_mane",
                ModelPartBuilder.create()
                        .uv(42, 0).cuboid(-6.8F, -8.4F, -7.0F, 13.6F, 13.2F, 4.6F)
                        .uv(80, 0).cuboid(-5.6F, -10.8F, -6.3F, 11.2F, 3.4F, 3.9F)
                        .uv(112, 0).cuboid(-8.3F, -6.5F, -5.9F, 3.0F, 10.3F, 3.2F)
                        .uv(112, 16).cuboid(5.3F, -6.5F, -5.9F, 3.0F, 10.3F, 3.2F),
                ModelTransform.NONE);
        lionHead.addChild("lion_snout",
                ModelPartBuilder.create()
                        .uv(80, 9).cuboid(-3.4F, -1.1F, -10.8F, 6.8F, 3.8F, 4.7F),
                ModelTransform.NONE);
        lionHead.addChild("lion_nose_mouth",
                ModelPartBuilder.create().uv(104, 16).cuboid(-2.3F, 0.6F, -11.3F, 4.6F, 1.6F, 1.1F),
                ModelTransform.NONE);
        lionHead.addChild("lion_left_eye",
                ModelPartBuilder.create().uv(104, 0).cuboid(-3.7F, -4.1F, -8.3F, 1.3F, 1.1F, 1.1F),
                ModelTransform.NONE);
        lionHead.addChild("lion_right_eye",
                ModelPartBuilder.create().uv(112, 0).cuboid(2.4F, -4.1F, -8.3F, 1.3F, 1.1F, 1.1F),
                ModelTransform.NONE);
        lionHead.addChild("lion_left_fang",
                ModelPartBuilder.create().uv(104, 5).cuboid(-2.5F, 1.8F, -10.9F, 1.1F, 2.3F, 1.1F),
                ModelTransform.NONE);
        lionHead.addChild("lion_right_fang",
                ModelPartBuilder.create().uv(110, 5).cuboid(1.4F, 1.8F, -10.9F, 1.1F, 2.3F, 1.1F),
                ModelTransform.NONE);
        root.addChild("goat_neck",
                ModelPartBuilder.create().uv(58, 20).cuboid(-1.5F, -3.0F, -1.8F, 2.7F, 6.0F, 3.5F),
                ModelTransform.of(5.2F, 8.3F, -5.0F, 0.0F, -0.35F, 0.0F));
        ModelPartData goatHead = root.addChild("goat_head",
                ModelPartBuilder.create()
                        .uv(58, 0).cuboid(-3.1F, -6.7F, -4.6F, 6.1F, 7.9F, 7.2F),
                ModelTransform.of(7.6F, 7.2F, -5.7F, 0.0F, -0.45F, 0.0F));
        goatHead.addChild("goat_snout",
                ModelPartBuilder.create().uv(88, 8).cuboid(-1.4F, -2.2F, -7.8F, 4.3F, 3.4F, 3.6F),
                ModelTransform.NONE);
        goatHead.addChild("goat_left_eye",
                ModelPartBuilder.create().uv(106, 9).cuboid(-2.5F, -4.3F, -5.2F, 1.1F, 1.1F, 1.0F),
                ModelTransform.NONE);
        goatHead.addChild("goat_right_eye",
                ModelPartBuilder.create().uv(112, 9).cuboid(1.4F, -4.3F, -5.2F, 1.1F, 1.1F, 1.0F),
                ModelTransform.NONE);
        goatHead.addChild("goat_left_horn",
                ModelPartBuilder.create().uv(88, 0).cuboid(-3.0F, -11.2F, -3.8F, 1.9F, 5.4F, 2.1F),
                ModelTransform.NONE);
        goatHead.addChild("goat_right_horn",
                ModelPartBuilder.create().uv(98, 0).cuboid(2.0F, -11.2F, -3.8F, 1.9F, 5.4F, 2.1F),
                ModelTransform.NONE);
        goatHead.addChild("goat_beard",
                ModelPartBuilder.create()
                        .uv(106, 20).cuboid(-0.4F, 0.8F, -6.9F, 3.0F, 4.0F, 2.7F),
                ModelTransform.NONE);
        ModelPartData tail = root.addChild("tail", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 10.0F, 10.5F));
        tail.addChild("tail_base",
                ModelPartBuilder.create().uv(80, 68).cuboid(-1.4F, -1.5F, 0.0F, 2.8F, 2.5F, 9.0F),
                ModelTransform.NONE);
        tail.addChild("tail_rise",
                ModelPartBuilder.create().uv(104, 68).cuboid(-1.2F, -6.0F, 6.6F, 2.4F, 5.0F, 3.0F),
                ModelTransform.NONE);
        tail.addChild("tail_tip",
                ModelPartBuilder.create().uv(116, 68).cuboid(-0.9F, -5.6F, 9.0F, 1.8F, 3.2F, 3.2F),
                ModelTransform.NONE);
        ModelPartData serpentHead = tail.addChild("serpent_head",
                ModelPartBuilder.create()
                        .uv(80, 80).cuboid(-2.8F, -5.1F, 8.9F, 5.6F, 4.7F, 6.3F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, -0.35F, 0.0F, 0.0F));
        serpentHead.addChild("serpent_upper_jaw",
                ModelPartBuilder.create().uv(106, 80).cuboid(-2.2F, -4.0F, 14.3F, 4.4F, 1.9F, 3.2F),
                ModelTransform.NONE);
        serpentHead.addChild("serpent_lower_jaw",
                ModelPartBuilder.create().uv(106, 86).cuboid(-2.0F, -1.0F, 13.7F, 4.0F, 2.1F, 3.2F),
                ModelTransform.NONE);
        serpentHead.addChild("serpent_left_eye",
                ModelPartBuilder.create().uv(122, 80).cuboid(-2.1F, -4.2F, 13.3F, 1.1F, 1.2F, 1.0F),
                ModelTransform.NONE);
        serpentHead.addChild("serpent_right_eye",
                ModelPartBuilder.create().uv(122, 84).cuboid(1.0F, -4.2F, 13.3F, 1.1F, 1.2F, 1.0F),
                ModelTransform.NONE);
        serpentHead.addChild("snake_tongue",
                ModelPartBuilder.create()
                        .uv(112, 88).cuboid(-0.4F, -2.1F, 17.0F, 0.8F, 0.8F, 3.5F),
                ModelTransform.NONE);
        ModelPartData leftWing = root.addChild("left_wing", ModelPartBuilder.create(),
                ModelTransform.pivot(5.0F, 6.5F, -1.0F));
        leftWing.addChild("left_wing_membrane",
                ModelPartBuilder.create().uv(0, 92).cuboid(0.0F, -5.0F, -5.5F, 18.5F, 2.3F, 14.7F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_bone",
                ModelPartBuilder.create().uv(68, 92).cuboid(0.0F, -5.8F, -6.2F, 19.5F, 1.6F, 1.8F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_claw",
                ModelPartBuilder.create().uv(112, 94).cuboid(18.2F, -6.2F, -4.8F, 2.4F, 2.4F, 2.0F),
                ModelTransform.NONE);
        ModelPartData rightWing = root.addChild("right_wing", ModelPartBuilder.create(),
                ModelTransform.pivot(-5.0F, 6.5F, -1.0F));
        rightWing.addChild("right_wing_membrane",
                ModelPartBuilder.create().uv(0, 110).mirrored().cuboid(-18.5F, -5.0F, -5.5F, 18.5F, 2.3F, 14.7F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_bone",
                ModelPartBuilder.create().uv(68, 98).mirrored().cuboid(-19.5F, -5.8F, -6.2F, 19.5F, 1.6F, 1.8F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_claw",
                ModelPartBuilder.create().uv(112, 100).mirrored().cuboid(-20.6F, -6.2F, -4.8F, 2.4F, 2.4F, 2.0F),
                ModelTransform.NONE);
        ModelPartData frontLeftLeg = root.addChild("front_left_leg", ModelPartBuilder.create().uv(0, 68).cuboid(-2.0F, 0.0F, -2.2F, 4.3F, 11.2F, 4.8F), ModelTransform.pivot(3.8F, 13.0F, -6.2F));
        frontLeftLeg.addChild("front_left_claws", ModelPartBuilder.create().uv(0, 86).cuboid(-2.3F, 10.0F, -3.6F, 4.9F, 1.4F, 2.0F), ModelTransform.NONE);
        ModelPartData frontRightLeg = root.addChild("front_right_leg", ModelPartBuilder.create().uv(20, 68).mirrored().cuboid(-2.3F, 0.0F, -2.2F, 4.3F, 11.2F, 4.8F), ModelTransform.pivot(-3.8F, 13.0F, -6.2F));
        frontRightLeg.addChild("front_right_claws", ModelPartBuilder.create().uv(16, 86).mirrored().cuboid(-2.6F, 10.0F, -3.6F, 4.9F, 1.4F, 2.0F), ModelTransform.NONE);
        ModelPartData backLeftLeg = root.addChild("back_left_leg", ModelPartBuilder.create().uv(40, 68).cuboid(-2.1F, 0.0F, -2.0F, 4.7F, 11.2F, 4.8F), ModelTransform.pivot(3.9F, 13.0F, 7.0F));
        backLeftLeg.addChild("back_left_claws", ModelPartBuilder.create().uv(32, 86).cuboid(-2.4F, 10.0F, -3.2F, 5.2F, 1.4F, 2.0F), ModelTransform.NONE);
        ModelPartData backRightLeg = root.addChild("back_right_leg", ModelPartBuilder.create().uv(60, 68).mirrored().cuboid(-2.6F, 0.0F, -2.0F, 4.7F, 11.2F, 4.8F), ModelTransform.pivot(-3.9F, 13.0F, 7.0F));
        backRightLeg.addChild("back_right_claws", ModelPartBuilder.create().uv(48, 86).mirrored().cuboid(-2.8F, 10.0F, -3.2F, 5.2F, 1.4F, 2.0F), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(ChimeraEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.lionHead.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.lionHead.yaw = headYaw * (MathHelper.PI / 180.0F);
        this.lionMane.pitch = 0.0F;
        this.lionSnout.pitch = 0.0F;
        this.lionNoseMouth.pitch = 0.0F;
        this.lionLeftEye.pitch = 0.0F;
        this.lionRightEye.pitch = 0.0F;
        this.lionLeftFang.pitch = 0.0F;
        this.lionRightFang.pitch = 0.0F;
        this.goatNeck.pitch = this.lionHead.pitch * 0.2F + MathHelper.sin(animationProgress * 0.1F) * 0.03F;
        this.goatNeck.yaw = this.lionHead.yaw * 0.2F - 0.18F;
        this.goatHead.pitch = this.lionHead.pitch * 0.45F + MathHelper.sin(animationProgress * 0.1F) * 0.06F;
        this.goatHead.yaw = this.lionHead.yaw * 0.35F - 0.25F;
        this.goatSnout.pitch = 0.0F;
        this.goatLeftEye.pitch = 0.0F;
        this.goatRightEye.pitch = 0.0F;
        this.goatLeftHorn.pitch = 0.0F;
        this.goatRightHorn.pitch = 0.0F;
        this.goatBeard.pitch = MathHelper.sin(animationProgress * 0.13F) * 0.03F;
        this.tailBase.pitch = 0.0F;
        this.tailRise.pitch = MathHelper.sin(animationProgress * 0.12F) * 0.04F;
        this.tailTip.pitch = MathHelper.cos(animationProgress * 0.16F) * 0.05F;
        this.serpentHead.pitch = this.lionHead.pitch * 0.35F + MathHelper.cos(animationProgress * 0.18F) * 0.12F;
        this.serpentHead.yaw = this.lionHead.yaw * 0.35F + 0.35F + MathHelper.sin(animationProgress * 0.16F) * 0.16F;
        this.serpentUpperJaw.pitch = MathHelper.sin(animationProgress * 0.18F) * 0.03F;
        this.serpentLowerJaw.pitch = MathHelper.sin(animationProgress * 0.18F + MathHelper.PI) * 0.05F;
        this.serpentLeftEye.pitch = 0.0F;
        this.serpentRightEye.pitch = 0.0F;
        this.snakeTongue.pitch = MathHelper.sin(animationProgress * 0.24F) * 0.08F;
        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.72F) * 1.0F * limbDistance;
        this.frontLeftClaws.pitch = 0.0F;
        this.backRightLeg.pitch = this.frontLeftLeg.pitch;
        this.backRightClaws.pitch = 0.0F;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.72F + MathHelper.PI) * 1.0F * limbDistance;
        this.frontRightClaws.pitch = 0.0F;
        this.backLeftLeg.pitch = this.frontRightLeg.pitch;
        this.backLeftClaws.pitch = 0.0F;
        float wing = MathHelper.sin(animationProgress * 0.2F) * 0.18F;
        this.leftWing.roll = -0.28F - wing;
        this.rightWing.roll = 0.28F + wing;
        this.leftWingMembrane.pitch = 0.0F;
        this.leftWingBone.pitch = 0.0F;
        this.leftWingClaw.pitch = 0.0F;
        this.rightWingMembrane.pitch = 0.0F;
        this.rightWingBone.pitch = 0.0F;
        this.rightWingClaw.pitch = 0.0F;
        this.shoulderFireCrest.pitch = 0.0F;
        this.tail.pitch = -0.35F + MathHelper.sin(animationProgress * 0.14F) * 0.1F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.12F) * 0.2F;
        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.lionHead.pitch -= swing * 0.45F;
            this.goatHead.pitch -= swing * 0.25F;
            this.serpentHead.pitch -= swing * 0.3F;
            this.serpentLowerJaw.pitch += swing * 0.24F;
        }
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.1F) * 0.07F;
            this.lionHead.roll = recoil;
            this.goatHead.roll = -recoil * 0.7F;
            this.serpentHead.roll = recoil * 1.1F;
            this.tail.yaw += recoil;
        } else {
            this.lionHead.roll = 0.0F;
            this.goatHead.roll = 0.0F;
            this.serpentHead.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.0F * fall;
            this.lionHead.pitch += 0.4F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
