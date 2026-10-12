package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.PegasusEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class PegasusModel extends SinglePartEntityModel<PegasusEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftNostril;
    private final ModelPart rightNostril;
    private final ModelPart neckLower;
    private final ModelPart neckUpper;
    private final ModelPart mane;
    private final ModelPart maneCrest;
    private final ModelPart body;
    private final ModelPart saddleSpace;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftWingFeathers;
    private final ModelPart rightWingFeathers;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public PegasusModel(ModelPart root) {
        this.root = root;
        this.neckLower = root.getChild("neck_lower");
        this.neckUpper = this.neckLower.getChild("neck_upper");
        this.head = this.neckUpper.getChild("head");
        this.snout = this.head.getChild("snout");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEye = this.head.getChild("left_eye");
        this.rightEye = this.head.getChild("right_eye");
        this.leftNostril = this.head.getChild("left_nostril");
        this.rightNostril = this.head.getChild("right_nostril");
        this.mane = this.neckUpper.getChild("mane");
        this.maneCrest = this.neckUpper.getChild("mane_crest");
        this.body = root.getChild("body");
        this.saddleSpace = this.body.getChild("saddle_space");
        this.tail = root.getChild("tail");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftWingFeathers = this.leftWing.getChild("left_wing_feathers");
        this.rightWingFeathers = this.rightWing.getChild("right_wing_feathers");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 32).cuboid(-5.25F, -6.4F, -8.8F, 10.5F, 8.8F, 17.6F),
                ModelTransform.pivot(0.0F, 13.0F, 2.0F));
        body.addChild("chest",
                ModelPartBuilder.create()
                        .uv(64, 34).cuboid(-4.7F, -7.8F, -9.8F, 9.4F, 6.2F, 5.6F),
                ModelTransform.NONE);
        body.addChild("saddle_space",
                ModelPartBuilder.create()
                        .uv(88, 36).cuboid(-3.8F, -8.0F, -3.5F, 7.6F, 1.6F, 8.0F),
                ModelTransform.NONE);
        body.addChild("left_shoulder_blend",
                ModelPartBuilder.create()
                        .uv(106, 36).cuboid(3.8F, -7.4F, -7.0F, 2.0F, 4.0F, 5.5F),
                ModelTransform.NONE);
        body.addChild("right_shoulder_blend",
                ModelPartBuilder.create()
                        .uv(108, 36).cuboid(-5.8F, -7.4F, -7.0F, 2.0F, 4.0F, 5.5F),
                ModelTransform.NONE);

        ModelPartData neckLower = root.addChild("neck_lower",
                ModelPartBuilder.create()
                        .uv(44, 0).cuboid(-2.2F, -7.0F, -2.0F, 4.4F, 7.0F, 4.0F)
                        .uv(92, 0).cuboid(-1.5F, -6.2F, -2.8F, 3.0F, 5.4F, 1.1F),
                ModelTransform.of(0.0F, 8.0F, -7.5F, 0.35F, 0.0F, 0.0F));

        ModelPartData neckUpper = neckLower.addChild("neck_upper",
                ModelPartBuilder.create()
                        .uv(44, 12).cuboid(-2.0F, -5.5F, -1.8F, 4.0F, 6.0F, 3.6F)
                        .uv(92, 8).cuboid(-1.3F, -4.8F, -2.4F, 2.6F, 4.4F, 1.0F),
                ModelTransform.of(0.0F, -6.5F, -0.5F, 0.20F, 0.0F, 0.0F));
        neckUpper.addChild("mane",
                ModelPartBuilder.create()
                        .uv(100, 0).cuboid(-1.1F, -5.8F, 0.1F, 2.2F, 5.6F, 2.4F),
                ModelTransform.NONE);
        neckUpper.addChild("mane_crest",
                ModelPartBuilder.create()
                        .uv(110, 12).cuboid(-0.7F, -6.3F, -0.35F, 1.4F, 4.2F, 1.0F),
                ModelTransform.NONE);

        ModelPartData head = neckUpper.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-2.0F, -3.5F, -4.5F, 4.0F, 4.2F, 5.0F)
                        .uv(84, 0).cuboid(-2.35F, -2.0F, -4.2F, 0.42F, 1.96F, 2.8F)
                        .uv(90, 0).cuboid(1.93F, -2.0F, -4.2F, 0.42F, 1.96F, 2.8F),
                ModelTransform.of(0.0F, -5.2F, -1.5F, -0.16F, 0.0F, 0.0F));
        head.addChild("snout",
                ModelPartBuilder.create()
                        .uv(28, 0).cuboid(-1.0F, -1.5F, -8.0F, 2.0F, 2.0F, 3.5F),
                ModelTransform.NONE);
        head.addChild("nose_bridge",
                ModelPartBuilder.create()
                        .uv(78, 0).cuboid(-0.35F, -2.5F, -8.3F, 0.7F, 1.6F, 0.8F),
                ModelTransform.NONE);
        head.addChild("left_cheek_shadow",
                ModelPartBuilder.create()
                        .uv(72, 0).cuboid(-2.35F, -1.6F, -4.3F, 0.42F, 1.86F, 2.72F),
                ModelTransform.NONE);
        head.addChild("right_cheek_shadow",
                ModelPartBuilder.create()
                        .uv(82, 0).cuboid(1.93F, -1.6F, -4.3F, 0.42F, 1.86F, 2.72F),
                ModelTransform.NONE);
        head.addChild("nose_mouth",
                ModelPartBuilder.create()
                        .uv(112, 4).cuboid(-0.82F, -0.06F, -8.4F, 1.64F, 0.52F, 0.7F),
                ModelTransform.NONE);
        head.addChild("left_nostril",
                ModelPartBuilder.create()
                        .uv(104, 4).cuboid(-0.72F, -0.55F, -8.48F, 0.48F, 0.58F, 0.64F),
                ModelTransform.NONE);
        head.addChild("right_nostril",
                ModelPartBuilder.create()
                        .uv(108, 4).cuboid(0.24F, -0.55F, -8.48F, 0.48F, 0.58F, 0.64F),
                ModelTransform.NONE);
        head.addChild("left_ear",
                ModelPartBuilder.create()
                        .uv(92, 16).cuboid(0.9F, -6.6F, -2.4F, 0.82F, 3.0F, 0.95F),
                ModelTransform.NONE);
        head.addChild("right_ear",
                ModelPartBuilder.create()
                        .uv(100, 16).cuboid(-1.72F, -6.6F, -2.4F, 0.82F, 3.0F, 0.95F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create()
                        .uv(112, 0).cuboid(-2.25F, -2.7F, -4.95F, 0.6F, 0.7F, 0.52F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create()
                        .uv(118, 0).cuboid(1.65F, -2.7F, -4.95F, 0.6F, 0.7F, 0.52F),
                ModelTransform.NONE);

        ModelPartData leftWing = root.addChild("left_wing",
                ModelPartBuilder.create()
                        .uv(0, 64).cuboid(0.0F, -5.0F, -5.4F, 7.2F, 4.0F, 8.6F)
                        .uv(28, 64).cuboid(5.2F, -4.7F, -4.4F, 21.0F, 2.4F, 12.2F)
                        .uv(60, 64).cuboid(13.5F, -3.1F, 6.2F, 18.0F, 2.0F, 8.0F)
                        .uv(80, 64).cuboid(2.8F, -2.5F, -3.8F, 12.7F, 1.7F, 10.2F),
                ModelTransform.of(4.8F, 7.0F, -2.0F, 0.0F, 0.0F, 0.26F));
        leftWing.addChild("left_wing_feathers",
                ModelPartBuilder.create()
                        .uv(0, 78).cuboid(4.0F, 0.0F, 7.2F, 3.0F, 1.2F, 8.3F)
                        .uv(12, 78).cuboid(7.8F, 0.0F, 8.0F, 3.0F, 1.2F, 9.2F)
                        .uv(24, 78).cuboid(11.6F, 0.0F, 9.0F, 3.0F, 1.2F, 9.8F)
                        .uv(36, 78).cuboid(15.4F, 0.0F, 10.0F, 3.0F, 1.2F, 10.0F)
                        .uv(48, 78).cuboid(19.2F, 0.0F, 10.8F, 3.0F, 1.2F, 8.8F)
                        .uv(0, 92).cuboid(23.0F, 0.0F, 11.2F, 3.0F, 1.2F, 7.5F),
                ModelTransform.NONE);

        ModelPartData rightWing = root.addChild("right_wing",
                ModelPartBuilder.create()
                        .uv(54, 64).mirrored().cuboid(-7.2F, -5.0F, -5.4F, 7.2F, 4.0F, 8.6F)
                        .uv(66, 106).mirrored().cuboid(-26.2F, -4.7F, -4.4F, 21.0F, 2.4F, 12.2F)
                        .uv(20, 106).mirrored().cuboid(-31.5F, -3.1F, 6.2F, 18.0F, 2.0F, 8.0F)
                        .uv(80, 104).mirrored().cuboid(-15.5F, -2.5F, -3.8F, 12.7F, 1.7F, 10.2F),
                ModelTransform.of(-4.8F, 7.0F, -2.0F, 0.0F, 0.0F, -0.26F));
        rightWing.addChild("right_wing_feathers",
                ModelPartBuilder.create()
                        .uv(60, 78).mirrored().cuboid(-7.0F, 0.0F, 7.2F, 3.0F, 1.2F, 8.3F)
                        .uv(72, 78).mirrored().cuboid(-10.8F, 0.0F, 8.0F, 3.0F, 1.2F, 9.2F)
                        .uv(84, 78).mirrored().cuboid(-14.6F, 0.0F, 9.0F, 3.0F, 1.2F, 9.8F)
                        .uv(96, 78).mirrored().cuboid(-18.4F, 0.0F, 10.0F, 3.0F, 1.2F, 10.0F)
                        .uv(104, 78).mirrored().cuboid(-22.2F, 0.0F, 10.8F, 3.0F, 1.2F, 8.8F)
                        .uv(0, 92).mirrored().cuboid(-26.0F, 0.0F, 11.2F, 3.0F, 1.2F, 7.5F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(64, 52).cuboid(-1.2F, -1.0F, 0.0F, 2.4F, 2.2F, 10.0F)
                        .uv(94, 52).cuboid(-3.1F, -2.7F, 8.0F, 6.2F, 6.0F, 7.4F),
                ModelTransform.pivot(0.0F, 10.0F, 10.5F));
        tail.addChild("tail_highlight",
                ModelPartBuilder.create()
                        .uv(108, 52).cuboid(-1.0F, -3.0F, 10.5F, 2.0F, 1.2F, 5.5F),
                ModelTransform.NONE);

        root.addChild("front_left_leg",
                ModelPartBuilder.create()
                        .uv(0, 88).cuboid(-1.6F, 0.0F, -1.6F, 3.3F, 12.0F, 3.3F)
                        .uv(28, 88).cuboid(-2.0F, 10.2F, -2.5F, 4.0F, 2.3F, 4.7F),
                ModelTransform.pivot(3.2F, 12.0F, -5.5F));

        root.addChild("front_right_leg",
                ModelPartBuilder.create()
                        .uv(14, 88).mirrored().cuboid(-1.7F, 0.0F, -1.6F, 3.3F, 12.0F, 3.3F)
                        .uv(42, 88).mirrored().cuboid(-2.0F, 10.2F, -2.5F, 4.0F, 2.3F, 4.7F),
                ModelTransform.pivot(-3.2F, 12.0F, -5.5F));

        root.addChild("back_left_leg",
                ModelPartBuilder.create()
                        .uv(56, 88).cuboid(-1.8F, 0.0F, -1.8F, 3.7F, 12.0F, 3.7F)
                        .uv(88, 88).cuboid(-2.2F, 10.2F, -2.5F, 4.3F, 2.3F, 5.0F),
                ModelTransform.pivot(3.4F, 12.0F, 7.0F));

        root.addChild("back_right_leg",
                ModelPartBuilder.create()
                        .uv(72, 88).mirrored().cuboid(-1.9F, 0.0F, -1.8F, 3.7F, 12.0F, 3.7F)
                        .uv(104, 88).mirrored().cuboid(-2.1F, 10.2F, -2.5F, 4.3F, 2.3F, 5.0F),
                ModelTransform.pivot(-3.4F, 12.0F, 7.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(PegasusEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.55F - 0.04F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.55F;
        this.snout.pitch = 0.0F;
        float earFlick = MathHelper.sin(animationProgress * 0.09F) * 0.025F;
        this.leftEar.pitch = earFlick;
        this.rightEar.pitch = -earFlick * 0.8F;
        this.leftEye.pitch = 0.0F;
        this.rightEye.pitch = 0.0F;
        this.leftNostril.pitch = 0.0F;
        this.rightNostril.pitch = 0.0F;
        this.neckLower.pitch = 0.35F + this.head.pitch * 0.14F;
        this.neckLower.yaw = this.head.yaw * 0.21F;
        this.neckUpper.pitch = 0.20F + this.head.pitch * 0.14F;
        this.neckUpper.yaw = this.head.yaw * 0.21F;
        this.mane.pitch = MathHelper.sin(animationProgress * 0.1F) * 0.035F;
        this.maneCrest.pitch = this.mane.pitch * 1.2F;
        this.saddleSpace.visible = true;

        float step = MathHelper.cos(limbAngle * 0.72F) * 0.9F * limbDistance;
        this.frontLeftLeg.pitch = step;
        this.backRightLeg.pitch = step;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.72F + MathHelper.PI) * 0.9F * limbDistance;
        this.backLeftLeg.pitch = this.frontRightLeg.pitch;

        float flapSpeed = entity.isFlying() ? 0.62F : 0.22F;
        float flapAmount = entity.isFlying() ? 0.7F : 0.18F;
        float flap = MathHelper.sin(animationProgress * flapSpeed) * flapAmount;
        this.leftWing.roll = 0.25F + flap;
        this.rightWing.roll = -0.25F - flap;
        this.leftWing.pitch = -0.12F + MathHelper.cos(animationProgress * flapSpeed) * 0.12F;
        this.rightWing.pitch = this.leftWing.pitch;
        this.leftWingFeathers.pitch = MathHelper.cos(animationProgress * flapSpeed + 0.6F) * 0.06F;
        this.rightWingFeathers.pitch = this.leftWingFeathers.pitch;
        this.leftWingFeathers.roll = flap * 0.08F;
        this.rightWingFeathers.roll = -flap * 0.08F;

        this.tail.pitch = 0.18F + MathHelper.sin(animationProgress * 0.12F) * 0.08F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.16F) * 0.14F;
        this.body.pitch = entity.hasPassengers() ? -0.06F : 0.0F;

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 2.0F) * 0.1F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.05F * fall;
            this.neckLower.pitch += 0.18F * fall;
            this.neckUpper.pitch += 0.17F * fall;
            this.head.pitch += 0.2F * fall;
            this.leftWing.roll = -0.95F * fall;
            this.rightWing.roll = 0.95F * fall;
            this.tail.pitch -= 0.35F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
