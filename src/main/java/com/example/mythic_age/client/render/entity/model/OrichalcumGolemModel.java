package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.OrichalcumGolemEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class OrichalcumGolemModel extends SinglePartEntityModel<OrichalcumGolemEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart chestCore;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart backCrystals;
    private final ModelPart crestCrystal;

    public OrichalcumGolemModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.chestCore = this.body.getChild("chest_core");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.backCrystals = root.getChild("back_crystals");
        this.crestCrystal = this.head.getChild("crest_crystal");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 30).cuboid(-6.0F, -17.0F, -4.5F, 12.0F, 19.0F, 9.0F)
                        .uv(44, 30).cuboid(-7.8F, -18.8F, -5.2F, 15.6F, 7.2F, 10.4F)
                        .uv(0, 54).cuboid(-5.4F, -5.0F, -5.4F, 10.8F, 3.8F, 1.2F),
                ModelTransform.pivot(0.0F, 5.0F, 0.0F));
        body.addChild("upper_torso_rivet_left",
                ModelPartBuilder.create().uv(12, 116).cuboid(-5.5F, -12.6F, -5.7F, 1.5F, 1.6F, 1.3F),
                ModelTransform.NONE);
        body.addChild("upper_torso_rivet_right",
                ModelPartBuilder.create().uv(20, 116).cuboid(4.0F, -12.6F, -5.7F, 1.5F, 1.6F, 1.3F),
                ModelTransform.NONE);
        body.addChild("lower_rune_glyph",
                ModelPartBuilder.create().uv(28, 116).cuboid(-1.3F, -2.5F, -5.8F, 2.6F, 2.7F, 1.4F),
                ModelTransform.NONE);

        body.addChild("chest_core",
                ModelPartBuilder.create()
                        .uv(42, 54).cuboid(-3.2F, -10.2F, -6.0F, 6.4F, 7.0F, 1.5F)
                        .uv(62, 54).cuboid(-4.2F, -11.0F, -6.2F, 8.4F, 1.2F, 1.9F)
                        .uv(82, 54).cuboid(-4.2F, -3.0F, -6.2F, 8.4F, 1.2F, 1.9F)
                        .uv(102, 54).cuboid(-4.3F, -9.8F, -6.2F, 1.2F, 6.8F, 1.9F)
                        .uv(112, 54).cuboid(3.1F, -9.8F, -6.2F, 1.2F, 6.8F, 1.9F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        body.addChild("core_vertical_glow",
                ModelPartBuilder.create().uv(38, 116).cuboid(-0.7F, -9.4F, -6.1F, 1.4F, 5.6F, 1.9F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.5F, -9.0F, -4.2F, 9.0F, 8.0F, 8.4F)
                        .uv(42, 0).cuboid(-3.0F, -5.9F, -5.1F, 6.0F, 1.3F, 1.1F)
                        .uv(60, 0).cuboid(-5.2F, -10.0F, -4.5F, 10.4F, 2.4F, 9.0F),
                ModelTransform.pivot(0.0F, -11.0F, 0.0F));

        head.addChild("crest_crystal",
                ModelPartBuilder.create().uv(76, 0).cuboid(-1.4F, -13.2F, -1.2F, 2.8F, 4.4F, 2.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        root.addChild("back_crystals",
                ModelPartBuilder.create()
                        .uv(68, 0).cuboid(-1.5F, -22.0F, 4.0F, 3.0F, 11.5F, 3.2F)
                        .uv(84, 0).cuboid(-5.8F, -20.0F, 4.2F, 3.1F, 8.8F, 2.8F)
                        .uv(100, 0).cuboid(2.7F, -20.0F, 4.2F, 3.1F, 8.8F, 2.8F)
                        .uv(116, 0).cuboid(-1.2F, -12.2F, 4.6F, 2.4F, 6.0F, 2.4F),
                ModelTransform.pivot(0.0F, 5.0F, 0.0F));

        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(0, 82).cuboid(-1.4F, -6.2F, -4.8F, 6.2F, 6.4F, 9.6F)
                        .uv(58, 58).cuboid(-0.2F, -1.2F, -3.4F, 5.2F, 14.8F, 6.8F)
                        .uv(0, 100).cuboid(-0.8F, 8.2F, -4.2F, 6.0F, 6.6F, 8.4F)
                        .uv(64, 100).cuboid(-1.4F, 13.0F, -5.0F, 7.2F, 7.2F, 10.0F),
                ModelTransform.pivot(6.8F, -9.0F, 0.0F));
        leftArm.addChild("left_shoulder_plate",
                ModelPartBuilder.create().uv(0, 82).cuboid(-1.4F, -6.2F, -4.8F, 6.2F, 6.4F, 9.6F),
                ModelTransform.NONE);
        leftArm.addChild("left_shoulder_rivet",
                ModelPartBuilder.create().uv(48, 116).cuboid(1.0F, -4.6F, -5.4F, 1.8F, 1.8F, 1.3F),
                ModelTransform.NONE);
        leftArm.addChild("left_forearm_plate",
                ModelPartBuilder.create().uv(0, 100).cuboid(-0.8F, 8.2F, -4.2F, 6.0F, 6.6F, 8.4F),
                ModelTransform.NONE);
        leftArm.addChild("left_fist",
                ModelPartBuilder.create().uv(64, 100).cuboid(-1.4F, 13.0F, -5.0F, 7.2F, 7.2F, 10.0F),
                ModelTransform.NONE);
        leftArm.addChild("left_fist_knuckle_a",
                ModelPartBuilder.create().uv(64, 116).cuboid(-0.6F, 14.2F, -5.7F, 2.0F, 2.4F, 1.2F),
                ModelTransform.NONE);
        leftArm.addChild("left_fist_knuckle_b",
                ModelPartBuilder.create().uv(72, 116).cuboid(2.2F, 14.2F, -5.7F, 2.0F, 2.4F, 1.2F),
                ModelTransform.NONE);

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(34, 82).cuboid(-4.8F, -6.2F, -4.8F, 6.2F, 6.4F, 9.6F)
                        .uv(86, 58).cuboid(-5.0F, -1.2F, -3.4F, 5.2F, 14.8F, 6.8F)
                        .uv(32, 100).cuboid(-5.2F, 8.2F, -4.2F, 6.0F, 6.6F, 8.4F)
                        .uv(96, 100).cuboid(-5.8F, 13.0F, -5.0F, 7.2F, 7.2F, 10.0F),
                ModelTransform.pivot(-6.8F, -9.0F, 0.0F));
        rightArm.addChild("right_shoulder_plate",
                ModelPartBuilder.create().uv(34, 82).cuboid(-4.8F, -6.2F, -4.8F, 6.2F, 6.4F, 9.6F),
                ModelTransform.NONE);
        rightArm.addChild("right_shoulder_rivet",
                ModelPartBuilder.create().uv(56, 116).cuboid(-2.8F, -4.6F, -5.4F, 1.8F, 1.8F, 1.3F),
                ModelTransform.NONE);
        rightArm.addChild("right_forearm_plate",
                ModelPartBuilder.create().uv(32, 100).cuboid(-5.2F, 8.2F, -4.2F, 6.0F, 6.6F, 8.4F),
                ModelTransform.NONE);
        rightArm.addChild("right_fist",
                ModelPartBuilder.create().uv(96, 100).cuboid(-5.8F, 13.0F, -5.0F, 7.2F, 7.2F, 10.0F),
                ModelTransform.NONE);
        rightArm.addChild("right_fist_knuckle_a",
                ModelPartBuilder.create().uv(80, 116).cuboid(-4.2F, 14.2F, -5.7F, 2.0F, 2.4F, 1.2F),
                ModelTransform.NONE);
        rightArm.addChild("right_fist_knuckle_b",
                ModelPartBuilder.create().uv(88, 116).cuboid(-1.4F, 14.2F, -5.7F, 2.0F, 2.4F, 1.2F),
                ModelTransform.NONE);

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 64).cuboid(-2.0F, 0.0F, -3.0F, 4.4F, 14.8F, 6.0F)
                        .uv(44, 64).cuboid(-2.7F, 6.8F, -3.8F, 5.2F, 3.8F, 1.2F)
                        .uv(72, 64).cuboid(-2.8F, 13.2F, -5.4F, 6.0F, 4.4F, 9.2F),
                ModelTransform.pivot(2.4F, 6.4F, 0.0F));
        leftLeg.addChild("left_knee_plate",
                ModelPartBuilder.create().uv(44, 64).cuboid(-2.7F, 6.8F, -3.8F, 5.2F, 3.8F, 1.2F),
                ModelTransform.NONE);
        leftLeg.addChild("left_heavy_foot",
                ModelPartBuilder.create().uv(72, 64).cuboid(-2.8F, 13.2F, -5.4F, 6.0F, 4.4F, 9.2F),
                ModelTransform.NONE);
        leftLeg.addChild("left_foot_toe_plate",
                ModelPartBuilder.create().uv(96, 116).cuboid(-2.2F, 16.2F, -6.4F, 4.8F, 2.0F, 1.6F),
                ModelTransform.NONE);

        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(22, 64).cuboid(-2.4F, 0.0F, -3.0F, 4.4F, 14.8F, 6.0F)
                        .uv(58, 64).cuboid(-2.5F, 6.8F, -3.8F, 5.2F, 3.8F, 1.2F)
                        .uv(100, 64).cuboid(-3.2F, 13.2F, -5.4F, 6.0F, 4.4F, 9.2F),
                ModelTransform.pivot(-2.4F, 6.4F, 0.0F));
        rightLeg.addChild("right_knee_plate",
                ModelPartBuilder.create().uv(58, 64).cuboid(-2.5F, 6.8F, -3.8F, 5.2F, 3.8F, 1.2F),
                ModelTransform.NONE);
        rightLeg.addChild("right_heavy_foot",
                ModelPartBuilder.create().uv(100, 64).cuboid(-3.2F, 13.2F, -5.4F, 6.0F, 4.4F, 9.2F),
                ModelTransform.NONE);
        rightLeg.addChild("right_foot_toe_plate",
                ModelPartBuilder.create().uv(110, 116).cuboid(-2.6F, 16.2F, -6.4F, 4.8F, 2.0F, 1.6F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(OrichalcumGolemEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.6F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.55F;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.48F) * 0.65F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.48F + MathHelper.PI) * 0.65F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.48F + MathHelper.PI) * 0.38F * limbDistance - 0.12F;
        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.48F) * 0.38F * limbDistance - 0.12F;
        this.leftArm.roll = -0.06F;
        this.rightArm.roll = 0.06F;
        this.body.yaw = MathHelper.sin(limbAngle * 0.24F) * 0.045F * limbDistance;
        this.backCrystals.pitch = this.body.pitch * 0.2F;
        this.crestCrystal.roll = MathHelper.sin(animationProgress * 0.08F) * 0.035F;
        this.chestCore.pitch = MathHelper.sin(animationProgress * 0.06F) * 0.025F;

        if (entity.handSwingProgress > 0.0F) {
            float slam = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.85F - slam * 0.55F;
            this.leftArm.pitch = -1.55F - slam * 0.35F;
            this.body.pitch = -0.16F * slam;
            this.chestCore.pitch -= 0.05F * slam;
        } else {
            this.body.pitch = 0.0F;
        }

        if (entity.hurtTime > 0) {
            float shake = MathHelper.sin(animationProgress * 1.9F) * 0.07F;
            this.head.roll = shake;
            this.body.roll = -shake * 0.35F;
        } else {
            this.head.roll = 0.0F;
            this.body.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 22.0F);
            this.body.roll = 1.1F * fall;
            this.head.pitch += 0.28F * fall;
            this.leftArm.pitch = -0.4F * fall;
            this.rightArm.pitch = -0.4F * fall;
            this.chestCore.pitch -= 0.1F * fall;
            this.backCrystals.pitch += 0.18F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
