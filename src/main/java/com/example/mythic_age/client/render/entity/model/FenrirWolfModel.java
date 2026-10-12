package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.FenrirWolfEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class FenrirWolfModel extends SinglePartEntityModel<FenrirWolfEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart backFur;
    private final ModelPart head;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart chainCollar;
    private final ModelPart tail;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public FenrirWolfModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("wolf_body");
        this.backFur = root.getChild("back_fur");
        this.head = root.getChild("wolf_head");
        this.upperJaw = this.head.getChild("upper_jaw");
        this.lowerJaw = this.head.getChild("lower_jaw");
        this.chainCollar = root.getChild("chain_collar");
        this.tail = root.getChild("tail");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild("wolf_body",
                ModelPartBuilder.create()
                        .uv(0, 32).cuboid(-6.4F, -6.4F, -12.0F, 12.8F, 10.2F, 22.5F)
                        .uv(74, 34).cuboid(-5.4F, -7.8F, -9.2F, 10.8F, 2.2F, 14.0F)
                        .uv(0, 58).cuboid(-6.9F, -2.0F, -10.5F, 1.2F, 4.8F, 7.0F)
                        .uv(14, 58).cuboid(5.7F, -2.0F, -10.5F, 1.2F, 4.8F, 7.0F),
                ModelTransform.pivot(0.0F, 13.6F, 1.5F));

        ModelPartData backFur = root.addChild("back_fur",
                ModelPartBuilder.create()
                        .uv(34, 58).cuboid(-1.8F, -8.8F, -10.5F, 3.6F, 3.0F, 5.0F),
                ModelTransform.pivot(0.0F, 13.4F, 1.5F));
        backFur.addChild("back_fur_a",
                ModelPartBuilder.create().uv(34, 58).cuboid(-1.9F, -9.4F, -10.8F, 3.8F, 3.3F, 5.3F),
                ModelTransform.NONE);
        backFur.addChild("back_fur_b",
                ModelPartBuilder.create().uv(52, 58).cuboid(-1.7F, -9.8F, -5.8F, 3.4F, 3.6F, 5.6F),
                ModelTransform.NONE);
        backFur.addChild("back_fur_c",
                ModelPartBuilder.create().uv(70, 58).cuboid(-1.5F, -9.1F, -0.3F, 3.0F, 3.1F, 5.6F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("wolf_head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.55F, -6.3F, -6.75F, 9.1F, 7.9F, 7.95F),
                ModelTransform.of(0.0F, 9.4F, -9.7F, 0.08F, 0.0F, 0.0F));
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(44, 0).cuboid(-3.25F, -4.1F, -7.4F, 1.15F, 1.2F, 0.9F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(50, 0).cuboid(2.1F, -4.1F, -7.4F, 1.15F, 1.2F, 0.9F),
                ModelTransform.NONE);
        head.addChild("left_brow",
                ModelPartBuilder.create().uv(44, 0).cuboid(-3.15F, -4.7F, -7.6F, 1.35F, 1.1F, 0.9F),
                ModelTransform.NONE);
        head.addChild("right_brow",
                ModelPartBuilder.create().uv(50, 0).cuboid(1.8F, -4.7F, -7.6F, 1.35F, 1.1F, 0.9F),
                ModelTransform.NONE);
        head.addChild("left_ear",
                ModelPartBuilder.create().uv(56, 0).cuboid(-4.85F, -8.15F, -1.95F, 1.8F, 3.45F, 2.5F),
                ModelTransform.NONE);
        head.addChild("right_ear",
                ModelPartBuilder.create().uv(68, 0).cuboid(3.05F, -8.15F, -1.95F, 1.8F, 3.45F, 2.5F),
                ModelTransform.NONE);

        head.addChild("upper_jaw",
                ModelPartBuilder.create()
                        .uv(82, 0).cuboid(-2.9F, -2.4F, -12.65F, 5.8F, 3.4F, 5.9F)
                        .uv(112, 0).cuboid(-2.3F, 0.4F, -12.55F, 0.7F, 1.8F, 0.7F)
                        .uv(118, 0).cuboid(1.6F, 0.4F, -12.55F, 0.7F, 1.8F, 0.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData lowerJaw = head.addChild("lower_jaw",
                ModelPartBuilder.create()
                        .uv(82, 12).cuboid(-2.5F, 1.0F, -12.05F, 5.0F, 1.7F, 5.0F)
                        .uv(108, 12).cuboid(-2.0F, 0.2F, -11.95F, 0.65F, 1.2F, 0.65F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        lowerJaw.addChild("left_lower_fang",
                ModelPartBuilder.create().uv(108, 12).cuboid(-2.0F, 0.2F, -11.95F, 0.65F, 2.2F, 0.65F),
                ModelTransform.NONE);
        lowerJaw.addChild("right_lower_fang",
                ModelPartBuilder.create().uv(114, 12).cuboid(1.35F, 0.2F, -11.95F, 0.65F, 2.2F, 0.65F),
                ModelTransform.NONE);

        ModelPartData chainCollar = root.addChild("chain_collar",
                ModelPartBuilder.create()
                        .uv(0, 22).cuboid(-6.0F, -1.2F, -1.0F, 12.0F, 2.0F, 2.0F)
                        .uv(32, 22).cuboid(-6.7F, -1.45F, -3.6F, 1.9F, 2.3F, 2.5F)
                        .uv(42, 22).cuboid(4.8F, -1.45F, -3.6F, 1.9F, 2.3F, 2.5F)
                        .uv(52, 22).cuboid(-4.4F, -1.35F, -4.6F, 2.4F, 2.2F, 1.4F)
                        .uv(64, 22).cuboid(2.0F, -1.35F, -4.6F, 2.4F, 2.2F, 1.4F)
                        .uv(76, 22).cuboid(-1.3F, -1.5F, -5.0F, 2.6F, 2.4F, 1.5F),
                ModelTransform.pivot(0.0F, 10.6F, -6.0F));
        chainCollar.addChild("chain_link_left",
                ModelPartBuilder.create().uv(32, 22).cuboid(-6.8F, -1.6F, -3.8F, 2.0F, 2.4F, 2.6F),
                ModelTransform.NONE);
        chainCollar.addChild("chain_link_right",
                ModelPartBuilder.create().uv(42, 22).cuboid(4.8F, -1.6F, -3.8F, 2.0F, 2.4F, 2.6F),
                ModelTransform.NONE);
        chainCollar.addChild("chain_link_center",
                ModelPartBuilder.create().uv(76, 22).cuboid(-1.4F, -1.8F, -5.2F, 2.8F, 2.6F, 1.7F),
                ModelTransform.NONE);

        ModelPartData frontLeftLeg = root.addChild("front_left_leg",
                ModelPartBuilder.create()
                        .uv(0, 82).cuboid(-1.8F, 0.0F, -1.8F, 3.6F, 11.0F, 3.6F)
                        .uv(16, 82).cuboid(-2.2F, 9.8F, -3.1F, 4.4F, 1.7F, 4.8F),
                ModelTransform.pivot(4.0F, 13.0F, -6.2F));
        frontLeftLeg.addChild("front_left_paw",
                ModelPartBuilder.create().uv(16, 82).cuboid(-2.2F, 9.8F, -3.1F, 4.4F, 1.7F, 4.8F),
                ModelTransform.NONE);
        frontLeftLeg.addChild("front_left_claws",
                ModelPartBuilder.create().uv(0, 120).cuboid(-2.3F, 10.9F, -3.9F, 4.6F, 1.1F, 1.2F),
                ModelTransform.NONE);
        ModelPartData frontRightLeg = root.addChild("front_right_leg",
                ModelPartBuilder.create()
                        .uv(36, 82).cuboid(-1.8F, 0.0F, -1.8F, 3.6F, 11.0F, 3.6F)
                        .uv(52, 82).cuboid(-2.2F, 9.8F, -3.1F, 4.4F, 1.7F, 4.8F),
                ModelTransform.pivot(-4.0F, 13.0F, -6.2F));
        frontRightLeg.addChild("front_right_paw",
                ModelPartBuilder.create().uv(52, 82).cuboid(-2.2F, 9.8F, -3.1F, 4.4F, 1.7F, 4.8F),
                ModelTransform.NONE);
        frontRightLeg.addChild("front_right_claws",
                ModelPartBuilder.create().uv(16, 120).cuboid(-2.3F, 10.9F, -3.9F, 4.6F, 1.1F, 1.2F),
                ModelTransform.NONE);
        ModelPartData backLeftLeg = root.addChild("back_left_leg",
                ModelPartBuilder.create()
                        .uv(72, 82).cuboid(-2.1F, 0.0F, -1.9F, 4.2F, 10.7F, 3.8F)
                        .uv(90, 82).cuboid(-2.5F, 9.6F, -2.9F, 5.0F, 1.7F, 4.6F),
                ModelTransform.pivot(4.2F, 13.2F, 6.4F));
        backLeftLeg.addChild("back_left_paw",
                ModelPartBuilder.create().uv(90, 82).cuboid(-2.5F, 9.6F, -2.9F, 5.0F, 1.7F, 4.6F),
                ModelTransform.NONE);
        backLeftLeg.addChild("back_left_claws",
                ModelPartBuilder.create().uv(32, 120).cuboid(-2.6F, 10.7F, -3.7F, 5.2F, 1.1F, 1.2F),
                ModelTransform.NONE);
        ModelPartData backRightLeg = root.addChild("back_right_leg",
                ModelPartBuilder.create()
                        .uv(72, 100).cuboid(-2.1F, 0.0F, -1.9F, 4.2F, 10.7F, 3.8F)
                        .uv(90, 100).cuboid(-2.5F, 9.6F, -2.9F, 5.0F, 1.7F, 4.6F),
                ModelTransform.pivot(-4.2F, 13.2F, 6.4F));
        backRightLeg.addChild("back_right_paw",
                ModelPartBuilder.create().uv(90, 100).cuboid(-2.5F, 9.6F, -2.9F, 5.0F, 1.7F, 4.6F),
                ModelTransform.NONE);
        backRightLeg.addChild("back_right_claws",
                ModelPartBuilder.create().uv(48, 120).cuboid(-2.6F, 10.7F, -3.7F, 5.2F, 1.1F, 1.2F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(0, 108).cuboid(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 10.8F),
                ModelTransform.of(0.0F, 11.2F, 10.2F, -0.28F, 0.0F, 0.0F));
        tail.addChild("tail_tip",
                ModelPartBuilder.create().uv(28, 108).cuboid(-2.8F, -2.8F, 7.4F, 5.6F, 5.6F, 6.2F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(FenrirWolfEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = 0.08F + headPitch * (MathHelper.PI / 180.0F) * 0.55F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.55F;
        this.chainCollar.yaw = this.head.yaw * 0.18F;

        float stalk = MathHelper.cos(limbAngle * 0.72F) * limbDistance;
        float counterStalk = MathHelper.cos(limbAngle * 0.72F + MathHelper.PI) * limbDistance;
        this.frontLeftLeg.pitch = counterStalk * 0.85F;
        this.backRightLeg.pitch = counterStalk * 0.75F;
        this.frontRightLeg.pitch = stalk * 0.85F;
        this.backLeftLeg.pitch = stalk * 0.75F;
        this.body.pitch = -0.04F;
        this.backFur.pitch = this.body.pitch + MathHelper.sin(animationProgress * 0.08F) * 0.025F;
        this.tail.pitch = -0.28F + MathHelper.sin(animationProgress * 0.11F) * 0.08F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.14F) * 0.16F;
        this.lowerJaw.pitch = MathHelper.sin(animationProgress * 0.08F) * 0.035F;

        if (entity.handSwingProgress > 0.0F) {
            float bite = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.head.pitch -= 0.32F * bite;
            this.upperJaw.pitch = -0.08F * bite;
            this.lowerJaw.pitch = 0.35F * bite;
            this.body.pitch = -0.16F * bite;
        } else {
            this.upperJaw.pitch = 0.0F;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 2.0F) * 0.1F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 22.0F);
            this.body.roll = 1.05F * fall;
            this.head.pitch += 0.35F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
