package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.HydraEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class HydraModel extends SinglePartEntityModel<HydraEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftNeck;
    private final ModelPart centerNeck;
    private final ModelPart rightNeck;
    private final ModelPart leftHead;
    private final ModelPart centerHead;
    private final ModelPart rightHead;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;
    private final ModelPart tail;

    public HydraModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftNeck = root.getChild("left_neck");
        this.centerNeck = root.getChild("center_neck");
        this.rightNeck = root.getChild("right_neck");
        this.leftHead = this.leftNeck.getChild("left_head");
        this.centerHead = this.centerNeck.getChild("center_head");
        this.rightHead = this.rightNeck.getChild("right_head");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
        this.tail = this.body.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 34).cuboid(-9.0F, -6.0F, -13.0F, 18.0F, 12.0F, 26.0F)
                        .uv(0, 72).cuboid(-7.0F, -8.0F, -10.0F, 14.0F, 3.0F, 18.0F),
                ModelTransform.pivot(0.0F, 12.0F, 2.0F)
        );
        body.addChild("tail",
                ModelPartBuilder.create()
                        .uv(88, 48).cuboid(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 18.0F)
                        .uv(88, 70).cuboid(-1.5F, -1.5F, 17.0F, 3.0F, 3.0F, 8.0F),
                ModelTransform.of(0.0F, 0.5F, 12.0F, 0.18F, 0.0F, 0.0F)
        );

        addNeck(root, "center_neck", "center_head", 0.0F, 7.0F, -9.0F, 0.0F);
        addNeck(root, "left_neck", "left_head", 4.6F, 8.0F, -8.0F, 0.32F);
        addNeck(root, "right_neck", "right_head", -4.6F, 8.0F, -8.0F, -0.32F);
        addLeg(root, "front_left_leg", 6.5F, 17.0F, -7.5F, false);
        addLeg(root, "front_right_leg", -6.5F, 17.0F, -7.5F, true);
        addLeg(root, "back_left_leg", 6.5F, 17.0F, 10.0F, false);
        addLeg(root, "back_right_leg", -6.5F, 17.0F, 10.0F, true);

        return TexturedModelData.of(modelData, 128, 128);
    }

    private static void addNeck(ModelPartData root, String neckName, String headName, float x, float y, float z, float yaw) {
        ModelPartData neck = root.addChild(neckName,
                ModelPartBuilder.create().uv(64, 0).cuboid(-2.0F, -12.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                ModelTransform.of(x, y, z, -0.55F, yaw, 0.0F)
        );
        neck.addChild(headName,
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.5F, -5.0F, -5.0F, 7.0F, 6.0F, 8.0F)
                        .uv(30, 0).cuboid(-2.5F, -1.0F, -8.0F, 5.0F, 3.0F, 3.0F)
                        .uv(46, 0).cuboid(-2.8F, -7.0F, -2.0F, 2.0F, 3.0F, 2.0F)
                        .uv(46, 0).mirrored().cuboid(0.8F, -7.0F, -2.0F, 2.0F, 3.0F, 2.0F),
                ModelTransform.pivot(0.0F, -12.0F, -1.5F)
        );
    }

    private static void addLeg(ModelPartData root, String name, float x, float y, float z, boolean mirrored) {
        root.addChild(name,
                ModelPartBuilder.create()
                        .uv(64, 72).mirrored(mirrored).cuboid(-2.2F, 0.0F, -2.5F, 4.0F, 7.0F, 5.0F)
                        .uv(82, 72).mirrored(mirrored).cuboid(-2.8F, 6.0F, -4.0F, 5.0F, 2.0F, 6.0F),
                ModelTransform.pivot(x, y, z)
        );
    }

    @Override
    public void setAngles(HydraEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        float headYawRad = headYaw * (MathHelper.PI / 180.0F);
        float headPitchRad = headPitch * (MathHelper.PI / 180.0F);

        this.centerNeck.yaw = headYawRad * 0.35F;
        this.leftNeck.yaw = 0.32F + headYawRad * 0.25F + MathHelper.sin(animationProgress * 0.09F) * 0.08F;
        this.rightNeck.yaw = -0.32F + headYawRad * 0.25F - MathHelper.sin(animationProgress * 0.1F) * 0.08F;
        this.centerHead.pitch = headPitchRad * 0.45F + MathHelper.sin(animationProgress * 0.12F) * 0.04F;
        this.leftHead.pitch = headPitchRad * 0.35F + MathHelper.sin(animationProgress * 0.11F + 1.4F) * 0.06F;
        this.rightHead.pitch = headPitchRad * 0.35F + MathHelper.sin(animationProgress * 0.1F + 2.2F) * 0.06F;

        float crawl = 0.75F * limbDistance;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.7F) * crawl;
        this.backLeftLeg.pitch = MathHelper.cos(limbAngle * 0.7F) * crawl;
        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.7F + MathHelper.PI) * crawl;
        this.backRightLeg.pitch = MathHelper.cos(limbAngle * 0.7F + MathHelper.PI) * crawl;
        this.body.yaw = MathHelper.sin(limbAngle * 0.35F) * 0.06F * limbDistance;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.12F) * 0.25F + MathHelper.sin(limbAngle * 0.35F) * 0.18F * limbDistance;

        if (entity.handSwingProgress > 0.0F) {
            float strike = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.centerNeck.pitch = -0.75F + strike * 0.35F;
            this.leftNeck.pitch = -0.65F + strike * 0.25F;
            this.rightNeck.pitch = -0.65F + strike * 0.25F;
        } else {
            this.centerNeck.pitch = -0.55F;
            this.leftNeck.pitch = -0.55F;
            this.rightNeck.pitch = -0.55F;
        }

        if (entity.hurtTime > 0) {
            float shake = MathHelper.sin(animationProgress * 2.5F) * 0.08F;
            this.centerHead.roll = shake;
            this.leftHead.roll = -shake;
            this.rightHead.roll = shake * 0.7F;
        } else {
            this.centerHead.roll = 0.0F;
            this.leftHead.roll = 0.0F;
            this.rightHead.roll = 0.0F;
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.2F * fall;
            this.centerNeck.pitch = 0.25F * fall;
            this.leftNeck.pitch = 0.25F * fall;
            this.rightNeck.pitch = 0.25F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
