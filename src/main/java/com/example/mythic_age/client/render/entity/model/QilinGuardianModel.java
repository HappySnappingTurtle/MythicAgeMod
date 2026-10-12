package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.QilinGuardianEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class QilinGuardianModel extends SinglePartEntityModel<QilinGuardianEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftAntler;
    private final ModelPart rightAntler;
    private final ModelPart body;
    private final ModelPart mane;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;
    private final ModelPart tailPlume;

    public QilinGuardianModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftAntler = this.head.getChild("left_antler");
        this.rightAntler = this.head.getChild("right_antler");
        this.body = root.getChild("scaled_body");
        this.mane = root.getChild("mane");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftBackLeg = root.getChild("left_back_leg");
        this.rightBackLeg = root.getChild("right_back_leg");
        this.tail = root.getChild("tail");
        this.tailPlume = this.tail.getChild("tail_plume");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.5F, -4.8F, -6.5F, 7.0F, 6.6F, 6.4F),
                ModelTransform.pivot(0.0F, 9.0F, -8.0F));
        head.addChild("snout",
                ModelPartBuilder.create().uv(32, 0).cuboid(-2.35F, -2.1F, -10.0F, 4.7F, 3.1F, 3.8F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(56, 0).cuboid(-2.85F, -2.95F, -7.05F, 1.05F, 1.15F, 0.8F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(62, 0).cuboid(1.8F, -2.95F, -7.05F, 1.05F, 1.15F, 0.8F),
                ModelTransform.NONE);
        head.addChild("mouth_line",
                ModelPartBuilder.create().uv(68, 0).cuboid(-1.25F, 0.2F, -10.35F, 2.5F, 0.65F, 0.9F),
                ModelTransform.NONE);

        ModelPartData leftAntler = head.addChild("left_antler",
                ModelPartBuilder.create()
                        .uv(76, 0).cuboid(-0.5F, -7.0F, -0.5F, 1.0F, 7.2F, 1.0F),
                ModelTransform.of(2.3F, -4.5F, -4.2F, -0.12F, -0.2F, 0.2F));
        leftAntler.addChild("left_antler_branch_a",
                ModelPartBuilder.create().uv(82, 0).cuboid(0.0F, -6.5F, -0.4F, 3.2F, 0.8F, 0.8F),
                ModelTransform.NONE);
        leftAntler.addChild("left_antler_branch_b",
                ModelPartBuilder.create().uv(96, 0).cuboid(0.2F, -4.5F, -0.4F, 2.5F, 0.75F, 0.75F),
                ModelTransform.NONE);
        leftAntler.addChild("left_antler_tip",
                ModelPartBuilder.create().uv(108, 0).cuboid(2.4F, -7.0F, -0.35F, 0.8F, 2.2F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData rightAntler = head.addChild("right_antler",
                ModelPartBuilder.create()
                        .uv(76, 10).cuboid(-0.5F, -7.0F, -0.5F, 1.0F, 7.2F, 1.0F),
                ModelTransform.of(-2.3F, -4.5F, -4.2F, -0.12F, 0.2F, -0.2F));
        rightAntler.addChild("right_antler_branch_a",
                ModelPartBuilder.create().uv(82, 10).cuboid(-3.2F, -6.5F, -0.4F, 3.2F, 0.8F, 0.8F),
                ModelTransform.NONE);
        rightAntler.addChild("right_antler_branch_b",
                ModelPartBuilder.create().uv(96, 10).cuboid(-2.7F, -4.5F, -0.4F, 2.5F, 0.75F, 0.75F),
                ModelTransform.NONE);
        rightAntler.addChild("right_antler_tip",
                ModelPartBuilder.create().uv(108, 10).cuboid(-3.2F, -7.0F, -0.35F, 0.8F, 2.2F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData scaledBody = root.addChild("scaled_body",
                ModelPartBuilder.create()
                        .uv(0, 28).cuboid(-5.3F, -4.4F, -10.4F, 10.6F, 8.8F, 20.8F),
                ModelTransform.pivot(0.0F, 12.0F, 0.0F));
        scaledBody.addChild("scale_plate",
                ModelPartBuilder.create().uv(54, 28).cuboid(-4.0F, -5.4F, -8.2F, 8.0F, 1.6F, 16.0F),
                ModelTransform.NONE);
        scaledBody.addChild("scale_plate_front",
                ModelPartBuilder.create().uv(56, 28).cuboid(-4.2F, -5.8F, -9.8F, 8.4F, 2.1F, 5.8F),
                ModelTransform.NONE);
        scaledBody.addChild("scale_plate_mid",
                ModelPartBuilder.create().uv(78, 28).cuboid(-4.0F, -5.6F, -3.8F, 8.0F, 2.0F, 6.4F),
                ModelTransform.NONE);
        scaledBody.addChild("scale_plate_back",
                ModelPartBuilder.create().uv(98, 28).cuboid(-3.8F, -5.4F, 2.8F, 7.6F, 1.9F, 6.0F),
                ModelTransform.NONE);
        scaledBody.addChild("left_side_scale_front",
                ModelPartBuilder.create().uv(0, 54).cuboid(-5.4F, -2.4F, -6.6F, 1.0F, 4.8F, 3.2F),
                ModelTransform.NONE);
        scaledBody.addChild("right_side_scale_front",
                ModelPartBuilder.create().uv(10, 54).cuboid(4.4F, -2.4F, -6.6F, 1.0F, 4.8F, 3.2F),
                ModelTransform.NONE);
        scaledBody.addChild("left_side_scale_mid",
                ModelPartBuilder.create().uv(20, 54).cuboid(-5.4F, -2.4F, -1.8F, 1.0F, 4.8F, 3.2F),
                ModelTransform.NONE);
        scaledBody.addChild("right_side_scale_mid",
                ModelPartBuilder.create().uv(30, 54).cuboid(4.4F, -2.4F, -1.8F, 1.0F, 4.8F, 3.2F),
                ModelTransform.NONE);
        scaledBody.addChild("left_side_scale_back",
                ModelPartBuilder.create().uv(40, 54).cuboid(-5.4F, -2.4F, 3.0F, 1.0F, 4.8F, 3.2F),
                ModelTransform.NONE);
        scaledBody.addChild("right_side_scale_back",
                ModelPartBuilder.create().uv(50, 54).cuboid(4.4F, -2.4F, 3.0F, 1.0F, 4.8F, 3.2F),
                ModelTransform.NONE);
        scaledBody.addChild("chest_fur",
                ModelPartBuilder.create().uv(112, 42).cuboid(-3.0F, -3.0F, -10.8F, 6.0F, 5.4F, 1.6F),
                ModelTransform.NONE);

        ModelPartData mane = root.addChild("mane",
                ModelPartBuilder.create(),
                ModelTransform.pivot(0.0F, 8.0F, 0.0F));
        mane.addChild("mane_front",
                ModelPartBuilder.create().uv(70, 54).cuboid(-1.35F, -4.8F, -7.5F, 2.7F, 5.2F, 5.2F),
                ModelTransform.NONE);
        mane.addChild("mane_mid",
                ModelPartBuilder.create().uv(84, 54).cuboid(-1.25F, -5.8F, -3.2F, 2.5F, 5.6F, 5.2F),
                ModelTransform.NONE);
        mane.addChild("mane_back",
                ModelPartBuilder.create().uv(98, 54).cuboid(-1.15F, -5.2F, 1.2F, 2.3F, 4.8F, 5.5F),
                ModelTransform.NONE);

        ModelPartData leftFrontLeg = root.addChild("left_front_leg",
                ModelPartBuilder.create().uv(0, 76).cuboid(-1.2F, 0.0F, -1.2F, 2.4F, 8.0F, 2.4F),
                ModelTransform.pivot(3.1F, 15.0F, -5.5F));
        leftFrontLeg.addChild("front_left_hoof",
                ModelPartBuilder.create().uv(10, 76).cuboid(-1.5F, 7.2F, -1.5F, 3.0F, 1.5F, 3.0F),
                ModelTransform.NONE);
        ModelPartData rightFrontLeg = root.addChild("right_front_leg",
                ModelPartBuilder.create().uv(24, 76).cuboid(-1.2F, 0.0F, -1.2F, 2.4F, 8.0F, 2.4F),
                ModelTransform.pivot(-3.1F, 15.0F, -5.5F));
        rightFrontLeg.addChild("front_right_hoof",
                ModelPartBuilder.create().uv(34, 76).cuboid(-1.5F, 7.2F, -1.5F, 3.0F, 1.5F, 3.0F),
                ModelTransform.NONE);
        ModelPartData leftBackLeg = root.addChild("left_back_leg",
                ModelPartBuilder.create().uv(48, 76).cuboid(-1.3F, 0.0F, -1.25F, 2.6F, 8.0F, 2.5F),
                ModelTransform.pivot(3.2F, 15.0F, 5.0F));
        leftBackLeg.addChild("back_left_hoof",
                ModelPartBuilder.create().uv(60, 76).cuboid(-1.6F, 7.2F, -1.55F, 3.2F, 1.5F, 3.1F),
                ModelTransform.NONE);
        ModelPartData rightBackLeg = root.addChild("right_back_leg",
                ModelPartBuilder.create().uv(74, 76).cuboid(-1.3F, 0.0F, -1.25F, 2.6F, 8.0F, 2.5F),
                ModelTransform.pivot(-3.2F, 15.0F, 5.0F));
        rightBackLeg.addChild("back_right_hoof",
                ModelPartBuilder.create().uv(86, 76).cuboid(-1.6F, 7.2F, -1.55F, 3.2F, 1.5F, 3.1F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create().uv(0, 98).cuboid(-1.2F, -1.2F, 0.0F, 2.4F, 2.4F, 7.0F),
                ModelTransform.of(0.0F, 10.5F, 7.0F, -0.28F, 0.0F, 0.0F));
        tail.addChild("tail_plume",
                ModelPartBuilder.create()
                        .uv(22, 98).cuboid(-3.4F, -2.4F, 5.0F, 6.8F, 4.8F, 5.0F)
                        .uv(54, 98).cuboid(-2.4F, -3.0F, 7.0F, 4.8F, 6.0F, 3.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        tail.addChild("tail_plume_upper",
                ModelPartBuilder.create().uv(22, 98).cuboid(-3.2F, -4.6F, 8.0F, 6.4F, 3.0F, 4.0F),
                ModelTransform.NONE);
        tail.addChild("tail_plume_lower",
                ModelPartBuilder.create().uv(54, 98).cuboid(-2.7F, 1.6F, 7.8F, 5.4F, 3.2F, 4.0F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(QilinGuardianEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.55F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.5F;

        float walk = MathHelper.sin(limbAngle * 0.6662F) * limbDistance;
        float counterWalk = MathHelper.sin(limbAngle * 0.6662F + MathHelper.PI) * limbDistance;
        this.leftFrontLeg.pitch = counterWalk * 0.7F;
        this.rightFrontLeg.pitch = walk * 0.7F;
        this.leftBackLeg.pitch = walk * 0.65F;
        this.rightBackLeg.pitch = counterWalk * 0.65F;

        this.body.roll = MathHelper.sin(animationProgress * 0.08F) * 0.025F;
        this.mane.pitch = MathHelper.sin(animationProgress * 0.1F) * 0.03F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.13F) * 0.18F;
        this.tailPlume.roll = MathHelper.cos(animationProgress * 0.11F) * 0.12F;
        this.leftAntler.roll = 0.2F + MathHelper.sin(animationProgress * 0.07F) * 0.02F;
        this.rightAntler.roll = -0.2F - MathHelper.sin(animationProgress * 0.07F) * 0.02F;

        if (entity.handSwingProgress > 0.0F) {
            float stomp = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.head.pitch -= 0.18F * stomp;
            this.leftFrontLeg.pitch -= 0.75F * stomp;
            this.rightFrontLeg.pitch -= 0.75F * stomp;
            this.mane.pitch -= 0.08F * stomp;
        }
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.0F) * 0.07F;
            this.head.roll = recoil;
            this.body.roll += recoil * 0.5F;
            this.tail.yaw -= recoil;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.0F * fall;
            this.head.pitch += 0.25F * fall;
            this.mane.pitch += 0.15F * fall;
            this.tail.yaw *= 1.0F - fall * 0.5F;
            this.tailPlume.roll *= 1.0F - fall * 0.5F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
