package com.example.mythic_age.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class TempleGuardianModel<T extends LivingEntity> extends SinglePartEntityModel<T> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public TempleGuardianModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                .uv(54, 0).cuboid(-2.0F, -1.0F, -4.8F, 4.0F, 4.0F, 1.0F),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );
        head.addChild("circlet",
            ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.8F, -4.0F, 8.0F, 1.0F, 8.0F),
            ModelTransform.NONE
        );
        head.addChild("hood",
            ModelPartBuilder.create().uv(0, 16).cuboid(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, new Dilation(0.2F)),
            ModelTransform.NONE
        );

        root.addChild("body",
            ModelPartBuilder.create().uv(36, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );
        root.addChild("robe",
            ModelPartBuilder.create().uv(0, 37).cuboid(-4.5F, 0.0F, -2.5F, 9.0F, 22.0F, 5.0F),
            ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );
        root.addChild("belt",
            ModelPartBuilder.create().uv(28, 50).cuboid(-5.0F, 9.0F, -3.0F, 10.0F, 3.0F, 6.0F),
            ModelTransform.NONE
        );

        root.addChild("right_arm",
            ModelPartBuilder.create().uv(44, 32).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            ModelTransform.pivot(-5.0F, 2.0F, 0.0F)
        );
        root.addChild("left_arm",
            ModelPartBuilder.create().uv(44, 32).mirrored().cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            ModelTransform.pivot(5.0F, 2.0F, 0.0F)
        );
        root.addChild("right_leg",
            ModelPartBuilder.create().uv(28, 34).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            ModelTransform.pivot(-1.9F, 12.0F, 0.0F)
        );
        root.addChild("left_leg",
            ModelPartBuilder.create().uv(28, 34).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            ModelTransform.pivot(1.9F, 12.0F, 0.0F)
        );

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.body.yaw = 0.0F;

        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 0.8F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 0.8F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 0.65F * limbDistance;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 0.65F * limbDistance;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
