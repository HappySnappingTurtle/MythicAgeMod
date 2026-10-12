package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.AbyssalRayEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class AbyssalRayModel extends SinglePartEntityModel<AbyssalRayEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart headLobe;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWingTip;
    private final ModelPart tail;
    private final ModelPart tailSpine;
    private final ModelPart glowBelly;

    public AbyssalRayModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("ray_body");
        this.headLobe = this.body.getChild("head_lobe");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftWingTip = this.leftWing.getChild("left_wing_tip");
        this.rightWingTip = this.rightWing.getChild("right_wing_tip");
        this.tail = root.getChild("tail");
        this.tailSpine = this.tail.getChild("tail_spine");
        this.glowBelly = root.getChild("glow_belly");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("ray_body",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-5.5F, -2.0F, -7.0F, 11.0F, 3.6F, 14.0F)
                        .uv(82, 0).cuboid(-1.8F, 1.2F, -4.0F, 3.6F, 1.0F, 8.0F),
                ModelTransform.pivot(0.0F, 13.4F, -1.0F));
        body.addChild("back_ridge",
                ModelPartBuilder.create().uv(44, 0).cuboid(-3.8F, -2.7F, -5.0F, 7.6F, 1.2F, 8.8F),
                ModelTransform.NONE);

        ModelPartData headLobe = body.addChild("head_lobe",
                ModelPartBuilder.create()
                        .uv(0, 22).cuboid(-3.7F, -1.7F, -4.6F, 7.4F, 3.2F, 5.0F),
                ModelTransform.pivot(0.0F, -0.1F, -6.6F));
        headLobe.addChild("left_eye",
                ModelPartBuilder.create()
                        .uv(26, 22).cuboid(-3.2F, -1.95F, -4.95F, 1.3F, 1.1F, 0.7F),
                ModelTransform.NONE);
        headLobe.addChild("right_eye",
                ModelPartBuilder.create()
                        .uv(32, 22).cuboid(1.9F, -1.95F, -4.95F, 1.3F, 1.1F, 0.7F),
                ModelTransform.NONE);
        headLobe.addChild("mouth_slit",
                ModelPartBuilder.create()
                        .uv(38, 22).cuboid(-1.4F, 0.9F, -5.0F, 2.8F, 0.7F, 0.9F),
                ModelTransform.NONE);
        headLobe.addChild("left_gill_mark",
                ModelPartBuilder.create().uv(18, 98).cuboid(-4.0F, -0.45F, -2.4F, 0.7F, 0.6F, 2.6F),
                ModelTransform.NONE);
        headLobe.addChild("right_gill_mark",
                ModelPartBuilder.create().uv(26, 98).cuboid(3.3F, -0.45F, -2.4F, 0.7F, 0.6F, 2.6F),
                ModelTransform.NONE);

        ModelPartData leftWing = root.addChild("left_wing",
                ModelPartBuilder.create()
                        .uv(0, 38).cuboid(0.0F, -0.9F, -6.4F, 11.5F, 1.8F, 13.0F)
                        .uv(50, 38).cuboid(8.8F, -0.65F, -3.9F, 4.8F, 1.3F, 8.8F),
                ModelTransform.of(4.6F, 13.7F, -0.8F, 0.0F, -0.08F, 0.08F));
        leftWing.addChild("left_wing_tip",
                ModelPartBuilder.create().uv(84, 38).cuboid(0.0F, -0.45F, -2.6F, 5.4F, 0.9F, 5.2F),
                ModelTransform.of(12.8F, 0.0F, 0.6F, 0.0F, -0.16F, -0.14F));
        leftWing.addChild("left_wing_rib_a",
                ModelPartBuilder.create().uv(34, 98).cuboid(1.0F, -1.05F, -5.8F, 0.7F, 0.6F, 11.4F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_rib_b",
                ModelPartBuilder.create().uv(42, 98).cuboid(4.8F, -1.05F, -5.2F, 0.7F, 0.6F, 10.2F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_rib_c",
                ModelPartBuilder.create().uv(50, 98).cuboid(8.4F, -1.05F, -4.2F, 0.7F, 0.6F, 8.3F),
                ModelTransform.NONE);

        ModelPartData rightWing = root.addChild("right_wing",
                ModelPartBuilder.create()
                        .uv(0, 58).cuboid(-11.5F, -0.9F, -6.4F, 11.5F, 1.8F, 13.0F)
                        .uv(50, 58).cuboid(-13.6F, -0.65F, -3.9F, 4.8F, 1.3F, 8.8F),
                ModelTransform.of(-4.6F, 13.7F, -0.8F, 0.0F, 0.08F, -0.08F));
        rightWing.addChild("right_wing_tip",
                ModelPartBuilder.create().uv(84, 58).cuboid(-5.4F, -0.45F, -2.6F, 5.4F, 0.9F, 5.2F),
                ModelTransform.of(-12.8F, 0.0F, 0.6F, 0.0F, 0.16F, 0.14F));
        rightWing.addChild("right_wing_rib_a",
                ModelPartBuilder.create().uv(34, 106).mirrored().cuboid(-1.7F, -1.05F, -5.8F, 0.7F, 0.6F, 11.4F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_rib_b",
                ModelPartBuilder.create().uv(42, 106).mirrored().cuboid(-5.5F, -1.05F, -5.2F, 0.7F, 0.6F, 10.2F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_rib_c",
                ModelPartBuilder.create().uv(50, 106).mirrored().cuboid(-9.1F, -1.05F, -4.2F, 0.7F, 0.6F, 8.3F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(0, 78).cuboid(-1.0F, -0.75F, 0.0F, 2.0F, 1.5F, 13.0F)
                        .uv(24, 78).cuboid(-0.6F, -0.45F, 10.0F, 1.2F, 0.9F, 8.0F),
                ModelTransform.pivot(0.0F, 13.6F, 6.0F));
        tail.addChild("tail_spine",
                ModelPartBuilder.create()
                        .uv(48, 78).cuboid(-0.45F, -0.45F, 15.5F, 0.9F, 0.9F, 5.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        tail.addChild("tail_barb_left",
                ModelPartBuilder.create().uv(58, 78).cuboid(-1.8F, -0.35F, 19.2F, 1.2F, 0.7F, 2.7F),
                ModelTransform.NONE);
        tail.addChild("tail_barb_right",
                ModelPartBuilder.create().uv(70, 78).cuboid(0.6F, -0.35F, 19.2F, 1.2F, 0.7F, 2.7F),
                ModelTransform.NONE);

        ModelPartData glowBelly = root.addChild("glow_belly",
                ModelPartBuilder.create()
                        .uv(76, 78).cuboid(-3.4F, -0.3F, -4.6F, 6.8F, 0.6F, 8.8F),
                ModelTransform.pivot(0.0F, 16.0F, -0.8F));
        glowBelly.addChild("glow_rune_front",
                ModelPartBuilder.create().uv(0, 98).cuboid(-1.0F, -0.5F, -5.6F, 2.0F, 0.55F, 2.0F),
                ModelTransform.NONE);
        glowBelly.addChild("glow_rune_center",
                ModelPartBuilder.create().uv(8, 98).cuboid(-1.2F, -0.5F, -1.1F, 2.4F, 0.55F, 2.2F),
                ModelTransform.NONE);
        glowBelly.addChild("glow_rune_back",
                ModelPartBuilder.create().uv(0, 106).cuboid(-0.9F, -0.5F, 3.1F, 1.8F, 0.55F, 1.9F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(AbyssalRayEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.body.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.18F;
        this.body.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.16F;
        this.glowBelly.pitch = this.body.pitch * 0.45F;
        this.glowBelly.yaw = this.body.yaw * 0.45F;

        float glidePulse = MathHelper.sin(animationProgress * (entity.isGliding() ? 0.18F : 0.11F));
        float wingLift = 0.1F + glidePulse * 0.12F;
        this.leftWing.roll = 0.08F + wingLift;
        this.rightWing.roll = -0.08F - wingLift;
        this.leftWingTip.roll = -0.14F + glidePulse * 0.08F;
        this.rightWingTip.roll = 0.14F - glidePulse * 0.08F;

        float tailWave = animationProgress * 0.17F;
        this.tail.yaw = MathHelper.sin(tailWave) * 0.22F;
        this.tail.pitch = MathHelper.cos(tailWave * 0.7F) * 0.05F;
        this.tailSpine.yaw = MathHelper.sin(tailWave + 0.7F) * 0.18F;
        this.headLobe.pitch = MathHelper.sin(animationProgress * 0.09F) * 0.03F;

        if (entity.handSwingProgress > 0.0F) {
            float strike = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.tail.yaw += 0.52F * strike;
            this.tailSpine.yaw += 0.42F * strike;
            this.leftWing.roll += 0.12F * strike;
            this.rightWing.roll -= 0.12F * strike;
        }
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.2F) * 0.07F;
            this.body.roll = recoil;
            this.leftWing.roll += recoil;
            this.rightWing.roll += recoil;
            this.tail.roll = -recoil * 0.7F;
        } else {
            this.body.roll = 0.0F;
            this.tail.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 0.9F * fall;
            this.body.pitch += 0.22F * fall;
            this.leftWing.roll = -0.55F * fall;
            this.rightWing.roll = 0.55F * fall;
            this.leftWingTip.roll *= 1.0F - fall * 0.7F;
            this.rightWingTip.roll *= 1.0F - fall * 0.7F;
            this.tail.pitch -= 0.3F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
