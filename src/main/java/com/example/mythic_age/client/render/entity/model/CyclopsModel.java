package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.CyclopsEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class CyclopsModel extends SinglePartEntityModel<CyclopsEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public CyclopsModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-5.0F, -11.0F, -5.0F, 10.0F, 10.0F, 10.0F)
                        .uv(40, 0).cuboid(-2.0F, -7.0F, -5.8F, 4.0F, 3.0F, 1.0F)
                        .uv(0, 20).cuboid(-4.0F, -1.5F, -6.0F, 8.0F, 2.0F, 2.0F),
                ModelTransform.pivot(0.0F, -6.0F, 0.0F)
        );

        root.addChild("body",
                ModelPartBuilder.create()
                        .uv(40, 10).cuboid(-7.0F, -4.0F, -4.0F, 14.0F, 18.0F, 8.0F)
                        .uv(0, 42).cuboid(-6.0F, -3.0F, -4.8F, 12.0F, 13.0F, 1.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );

        root.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(84, 0).cuboid(-5.0F, -2.0F, -3.0F, 6.0F, 22.0F, 6.0F)
                        .uv(84, 28).cuboid(-5.5F, 16.0F, -3.5F, 7.0F, 5.0F, 7.0F),
                ModelTransform.pivot(-8.0F, -1.0F, 0.0F)
        );
        root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(84, 0).mirrored().cuboid(-1.0F, -2.0F, -3.0F, 6.0F, 22.0F, 6.0F)
                        .uv(84, 28).mirrored().cuboid(-1.5F, 16.0F, -3.5F, 7.0F, 5.0F, 7.0F),
                ModelTransform.pivot(8.0F, -1.0F, 0.0F)
        );

        root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(0, 60).cuboid(-3.2F, 0.0F, -3.0F, 6.0F, 16.0F, 6.0F)
                        .uv(24, 60).cuboid(-3.8F, 13.5F, -4.5F, 7.0F, 3.0F, 8.0F),
                ModelTransform.pivot(-3.5F, 8.0F, 0.0F)
        );
        root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 60).mirrored().cuboid(-2.8F, 0.0F, -3.0F, 6.0F, 16.0F, 6.0F)
                        .uv(24, 60).mirrored().cuboid(-3.2F, 13.5F, -4.5F, 7.0F, 3.0F, 8.0F),
                ModelTransform.pivot(3.5F, 8.0F, 0.0F)
        );

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(CyclopsEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);

        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.58F) * 1.0F * limbDistance;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.58F + MathHelper.PI) * 1.0F * limbDistance;
        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.58F + MathHelper.PI) * 0.65F * limbDistance - 0.12F;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.58F) * 0.65F * limbDistance - 0.12F;
        this.rightArm.roll = 0.08F;
        this.leftArm.roll = -0.08F;
        this.body.yaw = MathHelper.sin(limbAngle * 0.29F) * 0.08F * limbDistance;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -2.15F + swing * 0.45F;
            this.leftArm.pitch = -2.05F + swing * 0.35F;
            this.body.pitch = swing * 0.12F;
        } else {
            this.body.pitch = 0.0F;
        }

        if (entity.hurtTime > 0) {
            float shake = MathHelper.sin(animationProgress * 2.2F) * 0.08F;
            this.head.roll = shake;
            this.body.roll = -shake * 0.5F;
        } else {
            this.head.roll = 0.0F;
            this.body.roll = 0.0F;
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.pitch = 0.7F * fall;
            this.head.pitch += 0.5F * fall;
            this.leftArm.pitch = 0.8F * fall;
            this.rightArm.pitch = 0.8F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
