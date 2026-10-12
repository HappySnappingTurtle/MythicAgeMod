package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.FrostGiantEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class FrostGiantModel extends SinglePartEntityModel<FrostGiantEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart beard;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart club;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart spikes;

    public FrostGiantModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.beard = this.head.getChild("ice_beard");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.club = this.rightArm.getChild("frost_club");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.spikes = root.getChild("back_spikes");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 28).cuboid(-7.5F, -18.0F, -4.0F, 15.0F, 18.0F, 8.0F),
                ModelTransform.pivot(0.0F, 6.0F, 0.0F));
        body.addChild("fur_pelt",
                ModelPartBuilder.create().uv(52, 28).cuboid(-8.3F, -18.5F, -4.8F, 16.6F, 6.5F, 9.6F),
                ModelTransform.NONE);
        body.addChild("rune_chest",
                ModelPartBuilder.create().uv(88, 28).cuboid(-3.5F, -13.5F, -5.2F, 7.0F, 4.5F, 1.0F),
                ModelTransform.NONE);
        body.addChild("left_shoulder_ice",
                ModelPartBuilder.create().uv(0, 108).cuboid(3.6F, -20.0F, -5.5F, 5.2F, 4.0F, 11.0F),
                ModelTransform.NONE);
        body.addChild("right_shoulder_ice",
                ModelPartBuilder.create().uv(26, 108).cuboid(-8.8F, -20.0F, -5.5F, 5.2F, 4.0F, 11.0F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-5.5F, -11.0F, -5.0F, 11.0F, 11.0F, 10.0F),
                ModelTransform.pivot(0.0F, -10.0F, 0.0F));
        head.addChild("heavy_brow",
                ModelPartBuilder.create().uv(44, 0).cuboid(-4.6F, -7.8F, -6.0F, 9.2F, 1.4F, 1.0F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(62, 0).cuboid(-3.5F, -6.3F, -6.3F, 1.4F, 1.3F, 1.0F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(70, 0).cuboid(2.1F, -6.3F, -6.3F, 1.4F, 1.3F, 1.0F),
                ModelTransform.NONE);
        head.addChild("nose_bridge",
                ModelPartBuilder.create().uv(58, 12).cuboid(-0.8F, -5.8F, -6.6F, 1.6F, 3.0F, 1.2F),
                ModelTransform.NONE);
        head.addChild("mouth_line",
                ModelPartBuilder.create().uv(70, 12).cuboid(-2.6F, -2.2F, -6.5F, 5.2F, 1.0F, 1.0F),
                ModelTransform.NONE);
        head.addChild("left_brow_ice",
                ModelPartBuilder.create().uv(82, 12).cuboid(-4.2F, -8.4F, -6.4F, 3.2F, 1.8F, 1.1F),
                ModelTransform.NONE);
        head.addChild("right_brow_ice",
                ModelPartBuilder.create().uv(96, 12).cuboid(1.0F, -8.4F, -6.4F, 3.2F, 1.8F, 1.1F),
                ModelTransform.NONE);

        ModelPartData beard = head.addChild("ice_beard",
                ModelPartBuilder.create()
                        .uv(78, 0).cuboid(-4.0F, -3.0F, -6.0F, 8.0F, 7.8F, 1.5F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        beard.addChild("left_beard_strand",
                ModelPartBuilder.create().uv(78, 0).cuboid(-4.2F, -2.6F, -6.5F, 2.0F, 9.1F, 1.4F),
                ModelTransform.NONE);
        beard.addChild("center_beard_strand",
                ModelPartBuilder.create().uv(100, 0).cuboid(-1.2F, -2.2F, -6.8F, 2.4F, 9.7F, 1.6F),
                ModelTransform.NONE);
        beard.addChild("right_beard_strand",
                ModelPartBuilder.create().uv(110, 0).cuboid(2.2F, -2.6F, -6.5F, 2.0F, 9.1F, 1.4F),
                ModelTransform.NONE);

        ModelPartData backSpikes = root.addChild("back_spikes",
                ModelPartBuilder.create()
                        .uv(104, 0).cuboid(-1.0F, -18.8F, 3.8F, 2.0F, 14.8F, 3.0F),
                ModelTransform.pivot(0.0F, 6.0F, 0.0F));
        backSpikes.addChild("back_left_spike",
                ModelPartBuilder.create().uv(114, 0).cuboid(-4.5F, -16.8F, 3.9F, 2.0F, 9.8F, 2.5F),
                ModelTransform.NONE);
        backSpikes.addChild("back_right_spike",
                ModelPartBuilder.create().uv(114, 18).cuboid(2.5F, -16.8F, 3.9F, 2.0F, 9.8F, 2.5F),
                ModelTransform.NONE);

        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(0, 58).cuboid(-0.3F, -3.0F, -3.4F, 7.3F, 23.0F, 6.8F),
                ModelTransform.pivot(7.2F, 5.0F, 0.0F));
        leftArm.addChild("left_bracer_ice",
                ModelPartBuilder.create().uv(52, 108).cuboid(-0.6F, 12.0F, -4.0F, 8.1F, 4.0F, 8.0F),
                ModelTransform.NONE);

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(32, 58).cuboid(-7.0F, -3.0F, -3.4F, 7.3F, 23.0F, 6.8F),
                ModelTransform.pivot(-7.2F, 5.0F, 0.0F));
        rightArm.addChild("right_bracer_ice",
                ModelPartBuilder.create().uv(80, 108).cuboid(-7.5F, 12.0F, -4.0F, 8.1F, 4.0F, 8.0F),
                ModelTransform.NONE);

        ModelPartData club = rightArm.addChild("frost_club",
                ModelPartBuilder.create()
                        .uv(64, 58).cuboid(-1.4F, -1.0F, -1.4F, 2.8F, 25.0F, 2.8F),
                ModelTransform.pivot(-2.5F, 11.0F, -0.6F));
        club.addChild("frost_club_head",
                ModelPartBuilder.create().uv(88, 58).cuboid(-4.6F, -7.0F, -4.6F, 7.8F, 15.0F, 9.2F),
                ModelTransform.NONE);
        club.addChild("club_crack_a",
                ModelPartBuilder.create().uv(100, 58).cuboid(-4.9F, -3.2F, -4.9F, 8.4F, 1.4F, 1.1F),
                ModelTransform.NONE);
        club.addChild("club_crack_b",
                ModelPartBuilder.create().uv(100, 64).cuboid(-4.6F, 3.2F, 3.8F, 7.7F, 1.4F, 1.2F),
                ModelTransform.NONE);
        club.addChild("club_ice_cap",
                ModelPartBuilder.create().uv(96, 72).cuboid(-3.2F, -10.2F, -3.2F, 5.2F, 3.4F, 6.4F),
                ModelTransform.NONE);

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 88).cuboid(-2.6F, 0.0F, -3.2F, 5.2F, 17.0F, 6.4F)
                        .uv(50, 88).cuboid(-3.3F, 14.0F, -5.4F, 6.5F, 4.0F, 9.2F),
                ModelTransform.pivot(3.4F, 6.0F, 0.0F));
        leftLeg.addChild("left_knee_ice",
                ModelPartBuilder.create().uv(0, 120).cuboid(-3.0F, 7.0F, -3.8F, 5.9F, 3.8F, 1.2F),
                ModelTransform.NONE);
        leftLeg.addChild("left_boot",
                ModelPartBuilder.create().uv(50, 88).cuboid(-3.3F, 14.0F, -5.4F, 6.5F, 4.0F, 9.2F),
                ModelTransform.NONE);
        leftLeg.addChild("left_boot_spikes",
                ModelPartBuilder.create().uv(40, 120).cuboid(-3.4F, 16.0F, -6.2F, 7.0F, 2.8F, 1.4F),
                ModelTransform.NONE);

        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(24, 88).cuboid(-2.6F, 0.0F, -3.2F, 5.2F, 17.0F, 6.4F)
                        .uv(80, 88).cuboid(-3.2F, 14.0F, -5.4F, 6.5F, 4.0F, 9.2F),
                ModelTransform.pivot(-3.4F, 6.0F, 0.0F));
        rightLeg.addChild("right_knee_ice",
                ModelPartBuilder.create().uv(20, 120).cuboid(-2.9F, 7.0F, -3.8F, 5.9F, 3.8F, 1.2F),
                ModelTransform.NONE);
        rightLeg.addChild("right_boot",
                ModelPartBuilder.create().uv(80, 88).cuboid(-3.2F, 14.0F, -5.4F, 6.5F, 4.0F, 9.2F),
                ModelTransform.NONE);
        rightLeg.addChild("right_boot_spikes",
                ModelPartBuilder.create().uv(62, 120).cuboid(-3.6F, 16.0F, -6.2F, 7.0F, 2.8F, 1.4F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(FrostGiantEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.75F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.65F;
        this.beard.pitch = MathHelper.sin(animationProgress * 0.07F) * 0.035F;

        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.58F) * 0.75F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.58F + MathHelper.PI) * 0.75F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.58F + MathHelper.PI) * 0.55F * limbDistance;
        this.rightArm.pitch = -0.22F + MathHelper.cos(limbAngle * 0.58F) * 0.35F * limbDistance;
        this.rightArm.roll = -0.1F;
        this.club.pitch = -0.35F;
        this.spikes.pitch = this.body.pitch * 0.25F;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.7F - swing * 0.8F;
            this.body.pitch = -0.18F * swing;
            this.club.pitch = -0.9F;
        } else {
            this.body.pitch = 0.0F;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 1.8F) * 0.08F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 22.0F);
            this.body.roll = 1.1F * fall;
            this.head.pitch += 0.35F * fall;
            this.rightArm.pitch = -0.65F * fall;
            this.leftArm.pitch = -0.25F * fall;
            this.club.pitch = -0.15F * fall;
        } else if (entity.handSwingProgress <= 0.0F) {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
