package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.NemeanLionEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class NemeanLionModel extends SinglePartEntityModel<NemeanLionEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart shoulder;
    private final ModelPart backArmorRidge;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart noseMouth;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftFang;
    private final ModelPart rightFang;
    private final ModelPart mane;
    private final ModelPart maneCore;
    private final ModelPart maneTop;
    private final ModelPart maneBottom;
    private final ModelPart maneLeft;
    private final ModelPart maneRight;
    private final ModelPart maneOuterTop;
    private final ModelPart maneOuterLeft;
    private final ModelPart maneOuterRight;
    private final ModelPart maneChin;
    private final ModelPart tail;
    private final ModelPart tailTuftCore;
    private final ModelPart tailTuftTop;
    private final ModelPart tailTuftBottom;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontLeftPaw;
    private final ModelPart frontLeftClaws;
    private final ModelPart frontRightLeg;
    private final ModelPart frontRightPaw;
    private final ModelPart frontRightClaws;
    private final ModelPart backLeftLeg;
    private final ModelPart backLeftPaw;
    private final ModelPart backLeftClaws;
    private final ModelPart backRightLeg;
    private final ModelPart backRightPaw;
    private final ModelPart backRightClaws;

    public NemeanLionModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.shoulder = this.body.getChild("shoulder");
        this.backArmorRidge = this.body.getChild("back_armor_ridge");
        this.head = root.getChild("head");
        this.snout = this.head.getChild("snout");
        this.noseMouth = this.head.getChild("nose_mouth");
        this.leftEye = this.head.getChild("left_eye");
        this.rightEye = this.head.getChild("right_eye");
        this.leftFang = this.head.getChild("left_fang");
        this.rightFang = this.head.getChild("right_fang");
        this.mane = root.getChild("mane");
        this.maneCore = this.mane.getChild("mane_core");
        this.maneTop = this.mane.getChild("mane_top");
        this.maneBottom = this.mane.getChild("mane_bottom");
        this.maneLeft = this.mane.getChild("mane_left");
        this.maneRight = this.mane.getChild("mane_right");
        this.maneOuterTop = this.mane.getChild("mane_outer_top");
        this.maneOuterLeft = this.mane.getChild("mane_outer_left");
        this.maneOuterRight = this.mane.getChild("mane_outer_right");
        this.maneChin = this.mane.getChild("mane_chin");
        this.tail = root.getChild("tail");
        this.tailTuftCore = this.tail.getChild("tail_tuft_core");
        this.tailTuftTop = this.tail.getChild("tail_tuft_top");
        this.tailTuftBottom = this.tail.getChild("tail_tuft_bottom");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontLeftPaw = this.frontLeftLeg.getChild("front_left_paw");
        this.frontLeftClaws = this.frontLeftLeg.getChild("front_left_claws");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.frontRightPaw = this.frontRightLeg.getChild("front_right_paw");
        this.frontRightClaws = this.frontRightLeg.getChild("front_right_claws");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backLeftPaw = this.backLeftLeg.getChild("back_left_paw");
        this.backLeftClaws = this.backLeftLeg.getChild("back_left_claws");
        this.backRightLeg = root.getChild("back_right_leg");
        this.backRightPaw = this.backRightLeg.getChild("back_right_paw");
        this.backRightClaws = this.backRightLeg.getChild("back_right_claws");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 28).cuboid(-6.0F, -7.4F, -11.8F, 12.0F, 10.4F, 21.6F)
                        .uv(0, 90).cuboid(-5.2F, -8.8F, -12.8F, 10.4F, 9.6F, 6.4F),
                ModelTransform.pivot(0.0F, 14.0F, 2.0F));
        body.addChild("shoulder",
                ModelPartBuilder.create().uv(60, 32).cuboid(-5.5F, -9.6F, -9.4F, 11.0F, 4.5F, 12.2F),
                ModelTransform.NONE);
        body.addChild("back_armor_ridge",
                ModelPartBuilder.create().uv(72, 90).cuboid(-1.1F, -10.8F, -8.0F, 2.2F, 2.0F, 14.5F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.55F, -6.3F, -8.0F, 9.1F, 8.4F, 9.2F),
                ModelTransform.pivot(0.0F, 9.0F, -9.0F));
        head.addChild("snout",
                ModelPartBuilder.create().uv(34, 0).cuboid(-3.0F, -1.2F, -11.2F, 6.0F, 3.4F, 4.5F),
                ModelTransform.NONE);
        head.addChild("nose_mouth",
                ModelPartBuilder.create().uv(56, 0).cuboid(-2.0F, 0.45F, -11.8F, 4.0F, 1.55F, 1.2F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(108, 0).cuboid(-3.15F, -4.1F, -8.35F, 1.2F, 1.15F, 0.95F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(114, 0).cuboid(1.95F, -4.1F, -8.35F, 1.2F, 1.15F, 0.95F),
                ModelTransform.NONE);
        head.addChild("left_fang",
                ModelPartBuilder.create().uv(108, 6).cuboid(-2.3F, 1.55F, -11.7F, 1.1F, 2.1F, 1.1F),
                ModelTransform.NONE);
        head.addChild("right_fang",
                ModelPartBuilder.create().uv(114, 6).cuboid(1.2F, 1.55F, -11.7F, 1.1F, 2.1F, 1.1F),
                ModelTransform.NONE);

        ModelPartData mane = root.addChild("mane", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 10.0F, -5.5F));
        mane.addChild("mane_core",
                ModelPartBuilder.create().uv(54, 0).cuboid(-7.8F, -10.2F, -6.4F, 15.6F, 14.4F, 9.3F),
                ModelTransform.NONE);
        mane.addChild("mane_top",
                ModelPartBuilder.create().uv(88, 0).cuboid(-6.0F, -12.8F, -5.4F, 12.0F, 4.4F, 7.4F),
                ModelTransform.NONE);
        mane.addChild("mane_bottom",
                ModelPartBuilder.create().uv(84, 20).cuboid(-6.2F, 2.8F, -4.4F, 12.4F, 4.8F, 7.0F),
                ModelTransform.NONE);
        mane.addChild("mane_left",
                ModelPartBuilder.create().uv(56, 18).cuboid(-10.0F, -6.9F, -5.0F, 3.2F, 10.1F, 7.6F),
                ModelTransform.NONE);
        mane.addChild("mane_right",
                ModelPartBuilder.create().uv(70, 18).cuboid(6.8F, -6.9F, -5.0F, 3.2F, 10.1F, 7.6F),
                ModelTransform.NONE);
        mane.addChild("mane_outer_top",
                ModelPartBuilder.create().uv(0, 106).cuboid(-4.8F, -15.0F, -4.4F, 9.6F, 2.6F, 6.0F),
                ModelTransform.NONE);
        mane.addChild("mane_outer_left",
                ModelPartBuilder.create().uv(24, 106).cuboid(-12.0F, -5.8F, -4.4F, 2.4F, 8.4F, 6.2F),
                ModelTransform.NONE);
        mane.addChild("mane_outer_right",
                ModelPartBuilder.create().uv(42, 106).cuboid(9.6F, -5.8F, -4.4F, 2.4F, 8.4F, 6.2F),
                ModelTransform.NONE);
        mane.addChild("mane_chin",
                ModelPartBuilder.create().uv(60, 106).cuboid(-4.8F, 6.8F, -3.8F, 9.6F, 3.0F, 5.6F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create().uv(70, 54).cuboid(-1.0F, -1.4F, 0.0F, 2.0F, 2.8F, 12.8F),
                ModelTransform.pivot(0.0F, 11.0F, 10.0F));
        tail.addChild("tail_tuft_core",
                ModelPartBuilder.create().uv(98, 54).cuboid(-2.9F, -3.1F, 11.0F, 5.8F, 6.2F, 5.4F),
                ModelTransform.NONE);
        tail.addChild("tail_tuft_top",
                ModelPartBuilder.create().uv(98, 68).cuboid(-2.1F, -4.4F, 11.8F, 4.2F, 1.7F, 3.8F),
                ModelTransform.NONE);
        tail.addChild("tail_tuft_bottom",
                ModelPartBuilder.create().uv(104, 74).cuboid(-2.1F, 2.7F, 11.8F, 4.2F, 1.7F, 3.8F),
                ModelTransform.NONE);

        ModelPartData frontLeftLeg = root.addChild("front_left_leg", ModelPartBuilder.create().uv(0, 60).cuboid(-2.1F, 0.0F, -2.1F, 4.6F, 11.2F, 4.7F), ModelTransform.pivot(3.2F, 13.0F, -6.0F));
        frontLeftLeg.addChild("front_left_paw", ModelPartBuilder.create().uv(36, 60).cuboid(-2.7F, 9.2F, -5.0F, 5.5F, 2.9F, 5.8F), ModelTransform.NONE);
        frontLeftLeg.addChild("front_left_claws", ModelPartBuilder.create().uv(92, 12).cuboid(-2.9F, 10.7F, -6.2F, 5.9F, 1.6F, 1.6F), ModelTransform.NONE);
        ModelPartData frontRightLeg = root.addChild("front_right_leg", ModelPartBuilder.create().uv(18, 60).mirrored().cuboid(-2.5F, 0.0F, -2.1F, 4.6F, 11.2F, 4.7F), ModelTransform.pivot(-3.2F, 13.0F, -6.0F));
        frontRightLeg.addChild("front_right_paw", ModelPartBuilder.create().uv(58, 60).mirrored().cuboid(-2.8F, 9.2F, -5.0F, 5.5F, 2.9F, 5.8F), ModelTransform.NONE);
        frontRightLeg.addChild("front_right_claws", ModelPartBuilder.create().uv(92, 18).mirrored().cuboid(-3.0F, 10.7F, -6.2F, 5.9F, 1.6F, 1.6F), ModelTransform.NONE);
        ModelPartData backLeftLeg = root.addChild("back_left_leg", ModelPartBuilder.create().uv(0, 74).cuboid(-2.3F, 0.0F, -2.2F, 4.6F, 11.0F, 4.9F), ModelTransform.pivot(3.4F, 13.0F, 7.0F));
        backLeftLeg.addChild("back_left_paw", ModelPartBuilder.create().uv(36, 76).cuboid(-2.7F, 9.0F, -2.6F, 5.2F, 3.0F, 5.8F), ModelTransform.NONE);
        backLeftLeg.addChild("back_left_claws", ModelPartBuilder.create().uv(92, 24).cuboid(-2.9F, 10.5F, -3.7F, 5.6F, 1.6F, 1.6F), ModelTransform.NONE);
        ModelPartData backRightLeg = root.addChild("back_right_leg", ModelPartBuilder.create().uv(18, 74).mirrored().cuboid(-2.3F, 0.0F, -2.2F, 4.6F, 11.0F, 4.9F), ModelTransform.pivot(-3.4F, 13.0F, 7.0F));
        backRightLeg.addChild("back_right_paw", ModelPartBuilder.create().uv(58, 76).mirrored().cuboid(-2.5F, 9.0F, -2.6F, 5.2F, 3.0F, 5.8F), ModelTransform.NONE);
        backRightLeg.addChild("back_right_claws", ModelPartBuilder.create().uv(92, 30).mirrored().cuboid(-2.7F, 10.5F, -3.7F, 5.6F, 1.6F, 1.6F), ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(NemeanLionEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        this.snout.pitch = 0.0F;
        this.noseMouth.pitch = 0.0F;
        this.leftEye.pitch = 0.0F;
        this.rightEye.pitch = 0.0F;
        this.leftFang.pitch = 0.0F;
        this.rightFang.pitch = 0.0F;
        this.mane.pitch = this.head.pitch * 0.45F;
        this.mane.yaw = this.head.yaw * 0.35F;
        resetManeLayers();
        this.shoulder.pitch = 0.0F;
        this.backArmorRidge.pitch = 0.0F;

        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.75F) * 1.15F * limbDistance;
        this.backRightLeg.pitch = this.frontLeftLeg.pitch;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.75F + MathHelper.PI) * 1.15F * limbDistance;
        this.backLeftLeg.pitch = this.frontRightLeg.pitch;
        resetPaws();

        this.tail.pitch = 0.35F + MathHelper.sin(animationProgress * 0.12F) * 0.12F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.18F) * 0.18F;
        this.tailTuftCore.pitch = MathHelper.sin(animationProgress * 0.16F) * 0.05F;
        this.tailTuftTop.pitch = 0.0F;
        this.tailTuftBottom.pitch = 0.0F;

        float pounce = entity.handSwingProgress > 0.0F ? MathHelper.sin(entity.handSwingProgress * MathHelper.PI) : 0.0F;
        this.body.pitch = -0.25F * pounce;
        this.frontLeftPaw.pitch -= 0.18F * pounce;
        this.frontRightPaw.pitch -= 0.18F * pounce;
        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 2.0F) * 0.1F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.1F * fall;
            this.head.pitch += 0.45F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    private void resetManeLayers() {
        this.maneCore.pitch = 0.0F;
        this.maneTop.pitch = 0.0F;
        this.maneBottom.pitch = 0.0F;
        this.maneLeft.yaw = 0.0F;
        this.maneRight.yaw = 0.0F;
        this.maneOuterTop.pitch = 0.0F;
        this.maneOuterLeft.yaw = 0.0F;
        this.maneOuterRight.yaw = 0.0F;
        this.maneChin.pitch = 0.0F;
    }

    private void resetPaws() {
        this.frontLeftPaw.pitch = 0.0F;
        this.frontLeftClaws.pitch = 0.0F;
        this.frontRightPaw.pitch = 0.0F;
        this.frontRightClaws.pitch = 0.0F;
        this.backLeftPaw.pitch = 0.0F;
        this.backLeftClaws.pitch = 0.0F;
        this.backRightPaw.pitch = 0.0F;
        this.backRightClaws.pitch = 0.0F;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
