package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.AnubisWarriorEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class AnubisWarriorModel extends SinglePartEntityModel<AnubisWarriorEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart khopesh;

    public AnubisWarriorModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.khopesh = this.rightArm.getChild("khopesh");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 24).cuboid(-4.0F, -10.0F, -2.2F, 8.0F, 12.0F, 4.4F)
                        .uv(28, 24).cuboid(-4.8F, -10.8F, -3.0F, 9.6F, 3.2F, 1.0F)
                        .uv(54, 24).cuboid(-4.2F, -0.8F, -2.6F, 8.4F, 6.4F, 5.2F),
                ModelTransform.pivot(0.0F, 8.0F, 0.0F));
        body.addChild("gold_collar",
                ModelPartBuilder.create().uv(28, 32).cuboid(-4.5F, -9.4F, 2.1F, 9.0F, 2.0F, 0.8F),
                ModelTransform.NONE);
        body.addChild("chest_scarab",
                ModelPartBuilder.create().uv(96, 8).cuboid(-1.7F, -8.8F, -3.15F, 3.4F, 3.5F, 0.9F),
                ModelTransform.NONE);
        body.addChild("collar_center_gem",
                ModelPartBuilder.create().uv(110, 8).cuboid(-1.1F, -10.9F, -3.25F, 2.2F, 2.3F, 1.0F),
                ModelTransform.NONE);
        body.addChild("linen_skirt",
                ModelPartBuilder.create().uv(54, 34).cuboid(-4.4F, -0.8F, -3.0F, 8.8F, 6.5F, 0.9F),
                ModelTransform.NONE);
        body.addChild("belt_buckle",
                ModelPartBuilder.create().uv(118, 8).cuboid(-1.2F, -0.8F, -3.05F, 2.4F, 1.8F, 0.9F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.25F, -8.0F, -3.05F, 6.5F, 8.0F, 6.1F),
                ModelTransform.pivot(0.0F, 0.8F, -0.8F));
        head.addChild("long_snout",
                ModelPartBuilder.create().uv(34, 0).cuboid(-2.0F, -4.6F, -8.0F, 4.0F, 3.5F, 5.0F),
                ModelTransform.NONE);
        head.addChild("left_ear",
                ModelPartBuilder.create().uv(58, 0).cuboid(-3.05F, -12.6F, -1.65F, 1.55F, 5.4F, 2.25F),
                ModelTransform.NONE);
        head.addChild("right_ear",
                ModelPartBuilder.create().uv(66, 0).cuboid(1.5F, -12.6F, -1.65F, 1.55F, 5.4F, 2.25F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(76, 0).cuboid(-2.1F, -5.35F, -3.4F, 0.95F, 1.0F, 0.6F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(82, 0).cuboid(1.15F, -5.35F, -3.4F, 0.95F, 1.0F, 0.6F),
                ModelTransform.NONE);
        head.addChild("mouth_line",
                ModelPartBuilder.create().uv(76, 6).cuboid(-1.25F, -2.2F, -8.35F, 2.5F, 0.8F, 0.7F),
                ModelTransform.NONE);
        head.addChild("left_nostril",
                ModelPartBuilder.create().uv(88, 0).cuboid(-1.15F, -2.9F, -8.5F, 0.75F, 0.8F, 0.7F),
                ModelTransform.NONE);
        head.addChild("right_nostril",
                ModelPartBuilder.create().uv(92, 0).cuboid(0.4F, -2.9F, -8.5F, 0.75F, 0.8F, 0.7F),
                ModelTransform.NONE);
        head.addChild("forehead_uraeus",
                ModelPartBuilder.create().uv(96, 0).cuboid(-0.7F, -8.3F, -3.8F, 1.4F, 2.1F, 0.8F),
                ModelTransform.NONE);

        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(84, 0).cuboid(-0.2F, -1.0F, -1.7F, 2.4F, 12.6F, 3.4F)
                        .uv(84, 18).cuboid(-0.4F, 7.4F, -2.0F, 2.8F, 2.0F, 4.0F),
                ModelTransform.pivot(4.1F, 7.0F, 0.0F));
        leftArm.addChild("left_bracer",
                ModelPartBuilder.create().uv(84, 18).cuboid(-0.5F, 6.8F, -2.1F, 3.0F, 2.4F, 4.2F),
                ModelTransform.NONE);

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(96, 0).cuboid(-2.2F, -1.0F, -1.7F, 2.4F, 12.6F, 3.4F)
                        .uv(96, 18).cuboid(-2.4F, 7.4F, -2.0F, 2.8F, 2.0F, 4.0F),
                ModelTransform.pivot(-4.1F, 7.0F, 0.0F));
        rightArm.addChild("right_bracer",
                ModelPartBuilder.create().uv(98, 18).cuboid(-2.5F, 6.8F, -2.1F, 3.0F, 2.4F, 4.2F),
                ModelTransform.NONE);

        ModelPartData khopesh = rightArm.addChild("khopesh",
                ModelPartBuilder.create()
                        .uv(108, 0).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 9.0F, 1.0F),
                ModelTransform.pivot(-1.2F, 8.6F, -0.8F));
        khopesh.addChild("khopesh_blade",
                ModelPartBuilder.create().uv(108, 16).cuboid(-1.6F, -5.8F, -0.6F, 3.2F, 5.8F, 1.2F),
                ModelTransform.NONE);
        khopesh.addChild("khopesh_hook",
                ModelPartBuilder.create().uv(118, 16).cuboid(-0.9F, -7.7F, -0.6F, 2.2F, 2.2F, 1.2F),
                ModelTransform.NONE);
        khopesh.addChild("khopesh_counterweight",
                ModelPartBuilder.create().uv(118, 22).cuboid(-0.7F, 8.0F, -0.6F, 1.6F, 2.2F, 1.2F),
                ModelTransform.NONE);

        root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 48).cuboid(-1.7F, 0.0F, -1.6F, 3.3F, 11.0F, 3.2F)
                        .uv(28, 48).cuboid(-2.0F, 10.1F, -3.0F, 4.0F, 1.5F, 3.2F),
                ModelTransform.pivot(2.0F, 12.4F, 0.0F));

        root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(14, 48).cuboid(-1.6F, 0.0F, -1.6F, 3.3F, 11.0F, 3.2F)
                        .uv(46, 48).cuboid(-2.0F, 10.1F, -3.0F, 4.0F, 1.5F, 3.2F),
                ModelTransform.pivot(-2.0F, 12.4F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(AnubisWarriorEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        float headPitchRadians = headPitch * (MathHelper.PI / 180.0F);
        float headYawRadians = headYaw * (MathHelper.PI / 180.0F);
        this.head.pitch = headPitchRadians;
        this.head.yaw = headYawRadians;
        this.head.roll = 0.0F;

        float walk = MathHelper.cos(limbAngle * 0.6662F);
        this.leftLeg.pitch = walk * 1.0F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 1.0F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 0.7F * limbDistance;
        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 0.7F * limbDistance;
        this.leftArm.roll = 0.08F + MathHelper.sin(animationProgress * 0.08F) * 0.04F;
        this.rightArm.roll = -0.08F;
        this.khopesh.pitch = -0.45F;
        this.khopesh.roll = -0.12F;
        this.body.yaw = 0.0F;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.45F + swing * -0.65F;
            this.rightArm.yaw = -0.25F + swing * 0.35F;
            this.body.yaw = -0.12F * swing;
            this.khopesh.pitch = -0.9F;
            this.khopesh.roll = -0.35F;
        } else {
            this.rightArm.yaw = 0.0F;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 1.8F) * 0.08F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.0F * fall;
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
