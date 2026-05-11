package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.MedusaEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class MedusaModel extends SinglePartEntityModel<MedusaEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart tail;
    private final ModelPart snakes;

    public MedusaModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.tail = root.getChild("tail");
        this.snakes = this.head.getChild("snakes");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F)
                        .uv(28, 0).cuboid(-4.0F, -8.0F, -3.0F, 8.0F, 2.0F, 6.0F),
                ModelTransform.pivot(0.0F, 3.0F, 0.0F)
        );
        ModelPartData snakes = head.addChild("snakes",
                ModelPartBuilder.create()
                        .uv(56, 0).cuboid(-5.2F, -9.0F, -1.0F, 2.0F, 8.0F, 2.0F)
                        .uv(56, 0).cuboid(3.2F, -9.0F, -1.0F, 2.0F, 8.0F, 2.0F)
                        .uv(64, 0).cuboid(-1.0F, -10.0F, -5.0F, 2.0F, 9.0F, 2.0F)
                        .uv(72, 0).cuboid(-1.0F, -10.0F, 3.0F, 2.0F, 9.0F, 2.0F),
                ModelTransform.NONE
        );
        snakes.addChild("snake_crown",
                ModelPartBuilder.create()
                        .uv(80, 0).cuboid(-4.5F, -11.5F, -1.0F, 9.0F, 2.0F, 2.0F),
                ModelTransform.NONE
        );

        root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 18).cuboid(-4.0F, 0.0F, -2.4F, 8.0F, 11.0F, 5.0F)
                        .uv(26, 18).cuboid(-4.5F, 1.0F, -3.0F, 9.0F, 6.0F, 1.0F),
                ModelTransform.pivot(0.0F, 3.0F, 0.0F)
        );
        root.addChild("right_arm",
                ModelPartBuilder.create().uv(48, 18).cuboid(-2.0F, -1.0F, -1.8F, 3.0F, 12.0F, 4.0F),
                ModelTransform.pivot(-4.5F, 4.0F, 0.0F)
        );
        root.addChild("left_arm",
                ModelPartBuilder.create().uv(48, 18).mirrored().cuboid(-1.0F, -1.0F, -1.8F, 3.0F, 12.0F, 4.0F),
                ModelTransform.pivot(4.5F, 4.0F, 0.0F)
        );
        root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(0, 40).cuboid(-3.2F, 0.0F, -3.0F, 6.4F, 11.0F, 6.0F)
                        .uv(26, 40).cuboid(-2.3F, 9.0F, -2.2F, 4.6F, 8.0F, 4.4F)
                        .uv(46, 40).cuboid(-1.5F, 15.0F, -1.5F, 3.0F, 7.0F, 3.0F),
                ModelTransform.pivot(0.0F, 13.0F, 0.0F)
        );

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(MedusaEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        this.body.yaw = MathHelper.sin(limbAngle * 0.45F) * 0.08F * limbDistance;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.18F) * 0.18F + MathHelper.sin(limbAngle * 0.55F) * 0.25F * limbDistance;
        this.tail.pitch = MathHelper.sin(limbAngle * 0.35F) * 0.06F * limbDistance;

        this.snakes.pitch = MathHelper.sin(animationProgress * 0.16F) * 0.06F;
        this.snakes.yaw = MathHelper.cos(animationProgress * 0.13F) * 0.08F;
        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 0.8F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 0.8F * limbDistance;
        this.rightArm.roll = 0.08F;
        this.leftArm.roll = -0.08F;

        if (entity.handSwingProgress > 0.0F) {
            float cast = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.4F + cast * 0.35F;
            this.leftArm.pitch = -1.35F + cast * 0.3F;
            this.head.pitch -= cast * 0.2F;
            this.snakes.pitch -= cast * 0.25F;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 2.8F) * 0.08F;
        } else {
            this.head.roll = 0.0F;
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.pitch = 0.75F * fall;
            this.tail.pitch = 0.45F * fall;
            this.head.pitch += 0.45F * fall;
        } else {
            this.body.pitch = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
