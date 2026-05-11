package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.CentaurEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class CentaurModel extends SinglePartEntityModel<CentaurEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart horseBody;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;
    private final ModelPart bow;
    private final ModelPart tail;

    public CentaurModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.torso = root.getChild("torso");
        this.horseBody = root.getChild("horse_body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
        this.bow = this.leftArm.getChild("bow");
        this.tail = this.horseBody.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild("horse_body",
                ModelPartBuilder.create()
                        .uv(0, 34).cuboid(-5.5F, -5.0F, -11.0F, 11.0F, 10.0F, 22.0F)
                        .uv(66, 34).cuboid(-4.0F, -6.0F, -13.0F, 8.0F, 7.0F, 5.0F),
                ModelTransform.pivot(0.0F, 11.0F, 2.0F)
        ).addChild("tail",
                ModelPartBuilder.create().uv(96, 34).cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 11.0F, 2.0F),
                ModelTransform.of(0.0F, -3.5F, 11.0F, 0.65F, 0.0F, 0.0F)
        );

        root.addChild("torso",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.0F, -12.0F, -2.5F, 8.0F, 12.0F, 5.0F)
                        .uv(26, 0).cuboid(-3.6F, -11.0F, -3.0F, 7.2F, 6.0F, 1.0F),
                ModelTransform.pivot(0.0F, 7.0F, -7.0F)
        );
        root.addChild("head",
                ModelPartBuilder.create()
                        .uv(42, 0).cuboid(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F)
                        .uv(66, 0).cuboid(-2.5F, -9.0F, -2.0F, 5.0F, 2.0F, 4.0F),
                ModelTransform.pivot(0.0F, -5.0F, -7.0F)
        );
        root.addChild("right_arm",
                ModelPartBuilder.create().uv(0, 18).cuboid(-2.0F, -1.0F, -2.0F, 3.0F, 13.0F, 4.0F),
                ModelTransform.pivot(-4.4F, -3.5F, -7.0F)
        );
        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create().uv(0, 18).mirrored().cuboid(-1.0F, -1.0F, -2.0F, 3.0F, 13.0F, 4.0F),
                ModelTransform.pivot(4.4F, -3.5F, -7.0F)
        );
        leftArm.addChild("bow",
                ModelPartBuilder.create()
                        .uv(84, 0).cuboid(1.2F, 1.0F, -6.0F, 1.0F, 15.0F, 1.0F)
                        .uv(90, 0).cuboid(1.2F, 1.0F, -5.8F, 1.0F, 1.0F, 8.0F)
                        .uv(90, 10).cuboid(1.2F, 15.0F, -5.8F, 1.0F, 1.0F, 8.0F),
                ModelTransform.NONE
        );

        addHorseLeg(root, "front_left_leg", 3.7F, 15.0F, -6.5F, false);
        addHorseLeg(root, "front_right_leg", -3.7F, 15.0F, -6.5F, true);
        addHorseLeg(root, "back_left_leg", 3.7F, 15.0F, 10.0F, false);
        addHorseLeg(root, "back_right_leg", -3.7F, 15.0F, 10.0F, true);

        return TexturedModelData.of(modelData, 128, 128);
    }

    private static void addHorseLeg(ModelPartData root, String name, float x, float y, float z, boolean mirrored) {
        ModelPartBuilder builder = ModelPartBuilder.create()
                .uv(66, 48).mirrored(mirrored).cuboid(-1.4F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F)
                .uv(78, 48).mirrored(mirrored).cuboid(-1.8F, 8.0F, -2.0F, 4.0F, 2.0F, 4.0F);
        root.addChild(name, builder, ModelTransform.pivot(x, y, z));
    }

    @Override
    public void setAngles(CentaurEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);

        float gait = 1.2F * limbDistance;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.78F) * gait;
        this.backLeftLeg.pitch = MathHelper.cos(limbAngle * 0.78F) * gait;
        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.78F + MathHelper.PI) * gait;
        this.backRightLeg.pitch = MathHelper.cos(limbAngle * 0.78F + MathHelper.PI) * gait;
        this.horseBody.pitch = MathHelper.sin(limbAngle * 0.39F) * 0.03F * limbDistance;
        this.tail.pitch = 0.65F + MathHelper.sin(animationProgress * 0.15F) * 0.12F;

        this.rightArm.pitch = -0.45F;
        this.leftArm.pitch = -0.35F;
        this.rightArm.roll = 0.18F;
        this.leftArm.roll = -0.25F;
        this.bow.visible = true;

        if (entity.handSwingProgress > 0.0F) {
            float draw = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.45F + draw * 0.35F;
            this.leftArm.pitch = -1.25F;
            this.leftArm.yaw = -0.25F;
            this.rightArm.yaw = 0.45F;
        } else {
            this.leftArm.yaw = 0.0F;
            this.rightArm.yaw = 0.0F;
        }

        if (entity.hurtTime > 0) {
            this.torso.roll = MathHelper.sin(animationProgress * 2.4F) * 0.07F;
        } else {
            this.torso.roll = 0.0F;
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.horseBody.roll = 1.15F * fall;
            this.torso.pitch = 0.7F * fall;
            this.head.pitch += 0.5F * fall;
        } else {
            this.horseBody.roll = 0.0F;
            this.torso.pitch = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
