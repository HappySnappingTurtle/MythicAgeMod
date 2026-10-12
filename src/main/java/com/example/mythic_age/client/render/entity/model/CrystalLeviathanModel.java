package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.CrystalLeviathanEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class CrystalLeviathanModel extends SinglePartEntityModel<CrystalLeviathanEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftMandibleFin;
    private final ModelPart rightMandibleFin;
    private final ModelPart body1;
    private final ModelPart body2;
    private final ModelPart body3;
    private final ModelPart body4;
    private final ModelPart leftSideFin;
    private final ModelPart rightSideFin;
    private final ModelPart tail;
    private final ModelPart tailFan;

    public CrystalLeviathanModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftMandibleFin = this.head.getChild("left_mandible_fin");
        this.rightMandibleFin = this.head.getChild("right_mandible_fin");
        this.body1 = root.getChild("body_1");
        this.body2 = root.getChild("body_2");
        this.body3 = root.getChild("body_3");
        this.body4 = root.getChild("body_4");
        this.leftSideFin = root.getChild("left_side_fin");
        this.rightSideFin = root.getChild("right_side_fin");
        this.tail = root.getChild("tail");
        this.tailFan = this.tail.getChild("tail_fan");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.5F, -5.2F, -8.0F, 7.0F, 6.4F, 8.0F),
                ModelTransform.pivot(0.0F, 9.5F, -12.5F));
        head.addChild("snout",
                ModelPartBuilder.create().uv(42, 0).cuboid(-2.6F, -2.2F, -12.0F, 5.2F, 3.2F, 4.0F),
                ModelTransform.NONE);
        head.addChild("lower_jaw",
                ModelPartBuilder.create().uv(72, 0).cuboid(-2.4F, 0.55F, -11.2F, 4.8F, 1.45F, 4.4F),
                ModelTransform.NONE);
        head.addChild("left_prism_eye",
                ModelPartBuilder.create().uv(96, 0).cuboid(-2.85F, -3.85F, -8.45F, 1.2F, 1.25F, 1.0F),
                ModelTransform.NONE);
        head.addChild("right_prism_eye",
                ModelPartBuilder.create().uv(104, 0).cuboid(1.65F, -3.85F, -8.45F, 1.2F, 1.25F, 1.0F),
                ModelTransform.NONE);
        head.addChild("head_crown_crystal",
                ModelPartBuilder.create().uv(116, 8).cuboid(-1.1F, -7.7F, -5.0F, 2.2F, 2.5F, 2.6F),
                ModelTransform.NONE);
        head.addChild("mouth_line",
                ModelPartBuilder.create().uv(112, 16).cuboid(-2.05F, 1.45F, -12.2F, 4.1F, 0.75F, 1.0F),
                ModelTransform.NONE);
        head.addChild("left_gill_slits",
                ModelPartBuilder.create().uv(102, 12).cuboid(3.8F, -2.2F, -7.9F, 1.0F, 3.2F, 4.1F),
                ModelTransform.NONE);
        head.addChild("right_gill_slits",
                ModelPartBuilder.create().uv(114, 12).cuboid(-4.8F, -2.2F, -7.9F, 1.0F, 3.2F, 4.1F),
                ModelTransform.NONE);

        head.addChild("left_mandible_fin",
                ModelPartBuilder.create().uv(112, 0).cuboid(0.0F, -1.1F, -1.4F, 4.2F, 2.2F, 2.8F),
                ModelTransform.of(3.6F, -0.1F, -9.6F, 0.1F, -0.24F, 0.12F));
        head.addChild("right_mandible_fin",
                ModelPartBuilder.create().uv(0, 26).cuboid(-4.2F, -1.1F, -1.4F, 4.2F, 2.2F, 2.8F),
                ModelTransform.of(-3.6F, -0.1F, -9.6F, 0.1F, 0.24F, -0.12F));

        ModelPartData body1 = root.addChild("body_1",
                ModelPartBuilder.create().uv(22, 26).cuboid(-4.6F, -4.2F, 0.0F, 9.2F, 8.4F, 8.0F),
                ModelTransform.pivot(0.0F, 11.0F, -8.0F));
        body1.addChild("body_1_glow_scale",
                ModelPartBuilder.create().uv(0, 96).cuboid(-2.1F, 3.2F, 1.0F, 4.2F, 1.5F, 5.8F),
                ModelTransform.NONE);
        ModelPartData body2 = root.addChild("body_2",
                ModelPartBuilder.create().uv(54, 26).cuboid(-4.2F, -3.9F, 0.0F, 8.4F, 7.8F, 8.0F),
                ModelTransform.pivot(0.0F, 11.0F, 0.0F));
        body2.addChild("body_2_glow_scale",
                ModelPartBuilder.create().uv(18, 96).cuboid(-1.9F, 2.9F, 1.0F, 3.8F, 1.4F, 5.8F),
                ModelTransform.NONE);
        ModelPartData body3 = root.addChild("body_3",
                ModelPartBuilder.create().uv(84, 26).cuboid(-3.8F, -3.5F, 0.0F, 7.6F, 7.0F, 8.0F),
                ModelTransform.pivot(0.0F, 11.0F, 8.0F));
        body3.addChild("body_3_glow_scale",
                ModelPartBuilder.create().uv(36, 96).cuboid(-1.7F, 2.6F, 1.0F, 3.4F, 1.3F, 5.8F),
                ModelTransform.NONE);
        ModelPartData body4 = root.addChild("body_4",
                ModelPartBuilder.create().uv(0, 54).cuboid(-3.2F, -3.1F, 0.0F, 6.4F, 6.2F, 8.0F),
                ModelTransform.pivot(0.0F, 11.0F, 16.0F));
        body4.addChild("body_4_glow_scale",
                ModelPartBuilder.create().uv(54, 96).cuboid(-1.4F, 2.3F, 1.0F, 2.8F, 1.2F, 5.8F),
                ModelTransform.NONE);

        root.addChild("dorsal_crystal_1",
                ModelPartBuilder.create().uv(58, 54).cuboid(-1.2F, -4.2F, -1.5F, 2.4F, 4.2F, 3.0F),
                ModelTransform.of(0.0F, 7.0F, -3.0F, -0.1F, 0.0F, 0.0F));
        root.addChild("dorsal_crystal_2",
                ModelPartBuilder.create().uv(70, 54).cuboid(-1.1F, -4.8F, -1.5F, 2.2F, 4.8F, 3.0F),
                ModelTransform.of(0.0F, 7.0F, 5.0F, 0.08F, 0.0F, 0.0F));
        root.addChild("dorsal_crystal_3",
                ModelPartBuilder.create().uv(82, 54).cuboid(-1.0F, -4.3F, -1.5F, 2.0F, 4.3F, 3.0F),
                ModelTransform.of(0.0F, 7.0F, 13.0F, -0.06F, 0.0F, 0.0F));
        root.addChild("dorsal_crystal_4",
                ModelPartBuilder.create().uv(112, 54).cuboid(-0.9F, -4.0F, -1.5F, 1.8F, 4.0F, 3.0F),
                ModelTransform.of(0.0F, 7.0F, 21.0F, 0.04F, 0.0F, 0.0F));

        ModelPartData leftSideFin = root.addChild("left_side_fin",
                ModelPartBuilder.create().uv(94, 54).cuboid(0.0F, -1.2F, -4.0F, 5.5F, 2.5F, 8.0F),
                ModelTransform.of(4.0F, 11.0F, 3.0F, 0.0F, 0.0F, 0.12F));
        leftSideFin.addChild("left_side_fin_rib",
                ModelPartBuilder.create().uv(94, 68).cuboid(1.0F, -0.3F, -3.0F, 4.5F, 1.1F, 1.4F),
                ModelTransform.NONE);
        ModelPartData rightSideFin = root.addChild("right_side_fin",
                ModelPartBuilder.create().uv(0, 82).cuboid(-5.5F, -1.2F, -4.0F, 5.5F, 2.5F, 8.0F),
                ModelTransform.of(-4.0F, 11.0F, 3.0F, 0.0F, 0.0F, -0.12F));
        rightSideFin.addChild("right_side_fin_rib",
                ModelPartBuilder.create().uv(108, 68).cuboid(-5.5F, -0.3F, -3.0F, 4.5F, 1.1F, 1.4F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create().uv(24, 82).cuboid(-2.4F, -2.4F, 0.0F, 4.8F, 4.8F, 12.0F),
                ModelTransform.pivot(0.0F, 11.0F, 24.0F));
        tail.addChild("tail_fan",
                ModelPartBuilder.create().uv(50, 82).cuboid(-6.5F, -1.0F, 8.0F, 13.0F, 2.0F, 8.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        tail.addChild("tail_fan_left_rib",
                ModelPartBuilder.create().uv(50, 96).cuboid(0.8F, -1.3F, 9.0F, 5.0F, 2.6F, 1.5F),
                ModelTransform.NONE);
        tail.addChild("tail_fan_right_rib",
                ModelPartBuilder.create().uv(68, 96).cuboid(-5.8F, -1.3F, 9.0F, 5.0F, 2.6F, 1.5F),
                ModelTransform.NONE);
        tail.addChild("tail_tip_crystal",
                ModelPartBuilder.create().uv(88, 96).cuboid(-1.0F, -1.0F, 16.0F, 2.0F, 2.0F, 4.0F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(CrystalLeviathanEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.55F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.5F;

        float wave = animationProgress * (entity.isGliding() ? 0.16F : 0.08F);
        this.body1.yaw = MathHelper.sin(wave) * 0.08F;
        this.body2.yaw = MathHelper.sin(wave + 0.7F) * 0.14F;
        this.body3.yaw = MathHelper.sin(wave + 1.4F) * 0.17F;
        this.body4.yaw = MathHelper.sin(wave + 2.1F) * 0.2F;
        this.tail.yaw = MathHelper.sin(wave + 2.8F) * 0.26F;
        this.tailFan.roll = MathHelper.sin(animationProgress * 0.12F) * 0.09F;

        float finPulse = MathHelper.sin(animationProgress * 0.18F) * 0.08F;
        this.leftSideFin.roll = 0.12F + finPulse;
        this.rightSideFin.roll = -0.12F - finPulse;
        this.leftMandibleFin.roll = 0.12F + finPulse * 0.7F;
        this.rightMandibleFin.roll = -0.12F - finPulse * 0.7F;

        if (entity.handSwingProgress > 0.0F) {
            float surge = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.head.pitch -= 0.34F * surge;
            this.body1.pitch = -0.12F * surge;
        } else {
            this.body1.pitch = 0.0F;
        }
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.0F) * 0.07F;
            this.head.roll = recoil;
            this.body2.roll = -recoil * 0.45F;
            this.body3.roll = recoil * 0.4F;
            this.tail.roll = -recoil * 0.35F;
        } else {
            this.head.roll = 0.0F;
            this.body2.roll = 0.0F;
            this.body3.roll = 0.0F;
            this.tail.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 24.0F);
            this.head.pitch += 0.28F * fall;
            this.body1.pitch = 0.24F * fall;
            this.body2.pitch = 0.18F * fall;
            this.body3.pitch = 0.12F * fall;
            this.body4.roll = 0.4F * fall;
            this.tail.pitch -= 0.28F * fall;
            this.tailFan.roll *= 1.0F - fall * 0.5F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
