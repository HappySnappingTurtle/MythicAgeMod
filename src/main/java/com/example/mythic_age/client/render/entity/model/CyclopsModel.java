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
    private final ModelPart heavyBrow;
    private final ModelPart centralEye;
    private final ModelPart mouth;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart stoneClub;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public CyclopsModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.heavyBrow = this.head.getChild("heavy_brow");
        this.centralEye = this.head.getChild("central_eye");
        this.mouth = this.head.getChild("mouth");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.stoneClub = this.rightArm.getChild("stone_club");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-6.2F, -12.5F, -5.6F, 12.4F, 12.7F, 11.2F),
                ModelTransform.pivot(0.0F, -14.0F, 0.0F)
        );
        head.addChild("heavy_brow",
                ModelPartBuilder.create()
                        .uv(52, 0).cuboid(-5.3F, -8.7F, -6.8F, 10.6F, 1.7F, 1.3F),
                ModelTransform.NONE
        );
        head.addChild("central_eye",
                ModelPartBuilder.create()
                        .uv(80, 0).cuboid(-2.6F, -7.5F, -7.2F, 5.2F, 3.5F, 1.4F),
                ModelTransform.NONE
        );
        head.addChild("central_pupil",
                ModelPartBuilder.create()
                        .uv(98, 0).cuboid(-0.7F, -6.7F, -7.4F, 1.4F, 1.6F, 0.5F),
                ModelTransform.NONE
        );
        head.addChild("nose_bridge",
                ModelPartBuilder.create()
                        .uv(106, 0).cuboid(-1.4F, -4.8F, -6.8F, 2.8F, 3.6F, 1.1F),
                ModelTransform.NONE
        );
        head.addChild("left_nostril",
                ModelPartBuilder.create()
                        .uv(118, 0).cuboid(-1.7F, -2.0F, -7.2F, 1.0F, 0.8F, 0.6F),
                ModelTransform.NONE
        );
        head.addChild("right_nostril",
                ModelPartBuilder.create()
                        .uv(122, 3).cuboid(0.7F, -2.0F, -7.2F, 1.0F, 0.8F, 0.6F),
                ModelTransform.NONE
        );
        head.addChild("mouth",
                ModelPartBuilder.create()
                        .uv(52, 6).cuboid(-3.4F, -1.4F, -6.9F, 6.8F, 1.1F, 1.1F),
                ModelTransform.NONE
        );
        head.addChild("left_tooth",
                ModelPartBuilder.create()
                        .uv(72, 6).cuboid(-2.2F, -0.4F, -7.0F, 1.0F, 1.9F, 0.9F),
                ModelTransform.NONE
        );
        head.addChild("right_tooth",
                ModelPartBuilder.create()
                        .uv(78, 6).cuboid(1.2F, -0.4F, -7.0F, 1.0F, 1.9F, 0.9F),
                ModelTransform.NONE
        );
        head.addChild("left_ear",
                ModelPartBuilder.create()
                        .uv(84, 8).cuboid(5.0F, -9.5F, -2.8F, 2.0F, 4.5F, 3.6F),
                ModelTransform.NONE
        );
        head.addChild("right_ear",
                ModelPartBuilder.create()
                        .uv(116, 8).cuboid(-7.0F, -9.5F, -2.8F, 2.0F, 4.5F, 3.6F),
                ModelTransform.NONE
        );
        head.addChild("right_cheek_scar",
                ModelPartBuilder.create()
                        .uv(100, 19).cuboid(2.8F, -5.0F, -7.0F, 1.9F, 0.6F, 0.8F),
                ModelTransform.NONE
        );

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 28).cuboid(-8.5F, -18.5F, -4.4F, 17.0F, 21.5F, 8.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F)
        );
        body.addChild("chest_plate",
                ModelPartBuilder.create()
                        .uv(56, 28).cuboid(-7.7F, -17.5F, -5.2F, 15.4F, 11.5F, 1.1F),
                ModelTransform.NONE
        );
        body.addChild("left_chest_muscle",
                ModelPartBuilder.create()
                        .uv(94, 28).cuboid(0.8F, -15.4F, -5.5F, 5.4F, 6.6F, 1.1F),
                ModelTransform.NONE
        );
        body.addChild("right_chest_muscle",
                ModelPartBuilder.create()
                        .uv(110, 28).cuboid(-6.2F, -15.4F, -5.5F, 5.4F, 6.6F, 1.1F),
                ModelTransform.NONE
        );
        body.addChild("belly_shadow",
                ModelPartBuilder.create()
                        .uv(56, 44).cuboid(-4.5F, -5.8F, -5.4F, 9.0F, 4.8F, 1.1F),
                ModelTransform.NONE
        );
        body.addChild("waist_belt",
                ModelPartBuilder.create()
                        .uv(80, 44).cuboid(-7.2F, -0.5F, -5.6F, 14.4F, 2.5F, 1.1F),
                ModelTransform.NONE
        );
        body.addChild("loincloth_front",
                ModelPartBuilder.create()
                        .uv(0, 62).cuboid(-3.8F, 1.5F, -5.7F, 7.6F, 6.3F, 1.3F),
                ModelTransform.NONE
        );
        body.addChild("loincloth_back",
                ModelPartBuilder.create()
                        .uv(20, 62).cuboid(-3.8F, 1.5F, 4.4F, 7.6F, 5.7F, 1.3F),
                ModelTransform.NONE
        );

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(70, 62).cuboid(-3.5F, -4.0F, -3.8F, 5.5F, 14.0F, 7.6F),
                ModelTransform.pivot(-8.0F, -12.0F, 0.0F)
        );
        rightArm.addChild("right_forearm",
                ModelPartBuilder.create()
                        .uv(32, 82).cuboid(-3.1F, 9.2F, -4.0F, 5.7F, 14.3F, 8.0F),
                ModelTransform.NONE
        );
        rightArm.addChild("right_fist",
                ModelPartBuilder.create()
                        .uv(94, 82).cuboid(-3.6F, 21.8F, -4.8F, 6.6F, 6.0F, 9.6F),
                ModelTransform.NONE
        );
        rightArm.addChild("right_finger_a",
                ModelPartBuilder.create()
                        .uv(18, 106).cuboid(-2.8F, 26.7F, -5.2F, 1.3F, 2.7F, 1.8F),
                ModelTransform.NONE
        );
        rightArm.addChild("right_finger_b",
                ModelPartBuilder.create()
                        .uv(24, 106).cuboid(-0.7F, 26.8F, -5.4F, 1.3F, 3.0F, 2.0F),
                ModelTransform.NONE
        );
        rightArm.addChild("right_finger_c",
                ModelPartBuilder.create()
                        .uv(30, 106).cuboid(1.4F, 26.7F, -5.2F, 1.3F, 2.7F, 1.8F),
                ModelTransform.NONE
        );
        ModelPartData stoneClub = rightArm.addChild("stone_club",
                ModelPartBuilder.create()
                        .uv(112, 52).cuboid(-1.65F, -24.0F, -1.35F, 3.3F, 42.0F, 2.7F),
                ModelTransform.of(-4.7F, -0.8F, -7.0F, -0.22F, 0.0F, 0.25F)
        );
        stoneClub.addChild("stone_club_head",
                ModelPartBuilder.create()
                        .uv(88, 100).cuboid(-6.9F, -33.0F, -5.8F, 11.8F, 20.5F, 11.6F),
                ModelTransform.NONE
        );
        stoneClub.addChild("stone_club_lump_top",
                ModelPartBuilder.create()
                        .uv(82, 60).cuboid(-4.3F, -38.2F, -4.3F, 7.6F, 6.8F, 8.6F),
                ModelTransform.NONE
        );
        stoneClub.addChild("stone_club_lump_side",
                ModelPartBuilder.create()
                        .uv(82, 72).cuboid(-10.2F, -28.5F, -4.4F, 4.7F, 12.2F, 8.8F),
                ModelTransform.NONE
        );
        stoneClub.addChild("stone_club_lump_back",
                ModelPartBuilder.create()
                        .uv(104, 72).cuboid(3.8F, -25.8F, 3.2F, 4.8F, 10.8F, 4.8F),
                ModelTransform.NONE
        );
        stoneClub.addChild("stone_club_knob",
                ModelPartBuilder.create()
                        .uv(104, 84).cuboid(-2.3F, 16.2F, -3.2F, 5.0F, 6.0F, 6.4F),
                ModelTransform.NONE
        );
        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(40, 62).mirrored().cuboid(-2.0F, -4.0F, -3.8F, 5.5F, 14.0F, 7.6F),
                ModelTransform.pivot(8.0F, -12.0F, 0.0F)
        );
        leftArm.addChild("left_forearm",
                ModelPartBuilder.create()
                        .uv(0, 82).mirrored().cuboid(-2.6F, 9.2F, -4.0F, 5.7F, 14.3F, 8.0F),
                ModelTransform.NONE
        );
        leftArm.addChild("left_fist",
                ModelPartBuilder.create()
                        .uv(64, 82).mirrored().cuboid(-3.0F, 21.8F, -4.8F, 6.6F, 6.0F, 9.6F),
                ModelTransform.NONE
        );
        leftArm.addChild("left_finger_a",
                ModelPartBuilder.create()
                        .uv(0, 106).mirrored().cuboid(-2.7F, 26.7F, -5.2F, 1.3F, 2.7F, 1.8F),
                ModelTransform.NONE
        );
        leftArm.addChild("left_finger_b",
                ModelPartBuilder.create()
                        .uv(6, 106).mirrored().cuboid(-0.6F, 26.8F, -5.4F, 1.3F, 3.0F, 2.0F),
                ModelTransform.NONE
        );
        leftArm.addChild("left_finger_c",
                ModelPartBuilder.create()
                        .uv(12, 106).mirrored().cuboid(1.5F, 26.7F, -5.2F, 1.3F, 2.7F, 1.8F),
                ModelTransform.NONE
        );

        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(26, 110).cuboid(-5.4F, 0.0F, -3.4F, 5.4F, 10.0F, 6.8F),
                ModelTransform.pivot(-0.6F, 3.0F, 0.0F)
        );
        rightLeg.addChild("right_shin",
                ModelPartBuilder.create()
                        .uv(78, 108).cuboid(-5.2F, 9.5F, -3.1F, 4.8F, 9.5F, 6.2F),
                ModelTransform.NONE
        );
        rightLeg.addChild("right_foot",
                ModelPartBuilder.create()
                        .uv(36, 92).cuboid(-6.2F, 17.0F, -5.4F, 6.8F, 4.0F, 9.4F),
                ModelTransform.NONE
        );
        rightLeg.addChild("right_toe_a",
                ModelPartBuilder.create()
                        .uv(90, 92).cuboid(-5.7F, 20.0F, -6.2F, 1.4F, 1.5F, 1.8F),
                ModelTransform.NONE
        );
        rightLeg.addChild("right_toe_b",
                ModelPartBuilder.create()
                        .uv(96, 92).cuboid(-3.5F, 20.0F, -6.5F, 1.4F, 1.8F, 2.1F),
                ModelTransform.NONE
        );
        rightLeg.addChild("right_toe_c",
                ModelPartBuilder.create()
                        .uv(102, 92).cuboid(-1.3F, 20.0F, -6.2F, 1.4F, 1.5F, 1.8F),
                ModelTransform.NONE
        );
        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 110).mirrored().cuboid(0.0F, 0.0F, -3.4F, 5.4F, 10.0F, 6.8F),
                ModelTransform.pivot(0.6F, 3.0F, 0.0F)
        );
        leftLeg.addChild("left_shin",
                ModelPartBuilder.create()
                        .uv(52, 108).mirrored().cuboid(0.4F, 9.5F, -3.1F, 4.8F, 9.5F, 6.2F),
                ModelTransform.NONE
        );
        leftLeg.addChild("left_foot",
                ModelPartBuilder.create()
                        .uv(0, 92).mirrored().cuboid(-0.6F, 17.0F, -5.4F, 6.8F, 4.0F, 9.4F),
                ModelTransform.NONE
        );
        leftLeg.addChild("left_toe_a",
                ModelPartBuilder.create()
                        .uv(72, 92).mirrored().cuboid(-0.1F, 20.0F, -6.2F, 1.4F, 1.5F, 1.8F),
                ModelTransform.NONE
        );
        leftLeg.addChild("left_toe_b",
                ModelPartBuilder.create()
                        .uv(78, 92).mirrored().cuboid(2.1F, 20.0F, -6.5F, 1.4F, 1.8F, 2.1F),
                ModelTransform.NONE
        );
        leftLeg.addChild("left_toe_c",
                ModelPartBuilder.create()
                        .uv(84, 92).mirrored().cuboid(4.3F, 20.0F, -6.2F, 1.4F, 1.5F, 1.8F),
                ModelTransform.NONE
        );

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(CyclopsEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        this.heavyBrow.pitch = 0.0F;
        this.centralEye.pitch = 0.0F;
        this.mouth.pitch = 0.0F;

        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.58F) * 1.0F * limbDistance;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.58F + MathHelper.PI) * 1.0F * limbDistance;
        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.58F + MathHelper.PI) * 0.65F * limbDistance - 0.12F;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.58F) * 0.65F * limbDistance - 0.12F;
        this.rightArm.roll = 0.08F;
        this.leftArm.roll = -0.08F;
        this.stoneClub.roll = 0.12F;
        this.stoneClub.pitch = -0.18F;
        this.body.yaw = MathHelper.sin(limbAngle * 0.29F) * 0.08F * limbDistance;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -2.15F + swing * 0.85F;
            this.leftArm.pitch = -2.05F + swing * 0.5F;
            this.stoneClub.pitch = -0.95F + swing * 0.75F;
            this.body.pitch = swing * 0.18F;
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
