package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.MinotaurEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class MinotaurModel extends SinglePartEntityModel<MinotaurEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public MinotaurModel(ModelPart root) {
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

        ModelPartData head = root.addChild("head",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-4.5F, -10.0F, -4.5F, 9.0F, 10.0F, 9.0F)
                .uv(0, 20).cuboid(-2.5F, -4.0F, -8.0F, 5.0F, 4.0F, 4.0F)
                .uv(18, 20).cuboid(-3.5F, -5.0F, -6.2F, 7.0F, 1.0F, 2.0F),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );
        head.addChild("left_horn",
            ModelPartBuilder.create().uv(36, 20).cuboid(0.4F, -13.0F, -1.8F, 3.0F, 6.0F, 4.0F),
            ModelTransform.NONE
        );
        head.addChild("right_horn",
            ModelPartBuilder.create().uv(36, 20).mirrored().cuboid(-3.4F, -13.0F, -1.8F, 3.0F, 6.0F, 4.0F),
            ModelTransform.NONE
        );

        root.addChild("body",
            ModelPartBuilder.create()
                .uv(40, 0).cuboid(-5.5F, 0.0F, -3.0F, 11.0F, 13.0F, 6.0F)
                .uv(0, 28).cuboid(-4.5F, 1.0F, -3.8F, 9.0F, 10.0F, 1.0F),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        ).addChild("tail",
            ModelPartBuilder.create().uv(0, 40).cuboid(-1.0F, 0.0F, 0.0F, 2.0F, 10.0F, 2.0F),
            ModelTransform.of(0.0F, 2.0F, 3.0F, 0.35F, 0.0F, 0.0F)
        );

        root.addChild("right_arm",
            ModelPartBuilder.create().uv(68, 0).cuboid(-4.0F, -2.5F, -2.5F, 5.0F, 15.0F, 5.0F),
            ModelTransform.pivot(-6.0F, 2.0F, 0.0F)
        );
        root.addChild("left_arm",
            ModelPartBuilder.create().uv(68, 0).mirrored().cuboid(-1.0F, -2.5F, -2.5F, 5.0F, 15.0F, 5.0F),
            ModelTransform.pivot(6.0F, 2.0F, 0.0F)
        );

        root.addChild("right_leg",
            ModelPartBuilder.create().uv(88, 0).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F)
                .uv(88, 18).cuboid(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 6.0F),
            ModelTransform.pivot(-2.8F, 12.0F, 0.0F)
        );
        root.addChild("left_leg",
            ModelPartBuilder.create().uv(88, 0).mirrored().cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F)
                .uv(88, 18).mirrored().cuboid(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 6.0F),
            ModelTransform.pivot(2.8F, 12.0F, 0.0F)
        );

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(MinotaurEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);

        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 1.35F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.35F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 1.4F * limbDistance;

        this.rightArm.roll = 0.08F;
        this.leftArm.roll = -0.08F;
        this.body.yaw = MathHelper.sin(limbAngle * 0.3331F) * 0.08F * limbDistance;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
