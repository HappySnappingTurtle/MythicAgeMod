package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.ValkyrieEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class ValkyrieModel extends SinglePartEntityModel<ValkyrieEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart helmet;
    private final ModelPart body;
    private final ModelPart cape;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart spear;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ValkyrieModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.helmet = this.head.getChild("winged_helmet");
        this.body = root.getChild("armor");
        this.cape = root.getChild("cape");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("spear_arm");
        this.spear = this.rightArm.getChild("spear");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.0F, -6.8F, -3.35F, 6.0F, 7.2F, 6.6F)
                        .uv(30, 0).cuboid(-2.1F, -3.8F, -3.8F, 0.85F, 1.05F, 0.7F)
                        .uv(36, 0).cuboid(1.25F, -3.8F, -3.8F, 0.85F, 1.05F, 0.7F),
                ModelTransform.pivot(0.0F, 7.2F, -0.6F));
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(30, 0).cuboid(-2.1F, -3.8F, -3.8F, 0.85F, 1.05F, 0.7F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(36, 0).cuboid(1.25F, -3.8F, -3.8F, 0.85F, 1.05F, 0.7F),
                ModelTransform.NONE);
        head.addChild("mouth_line",
                ModelPartBuilder.create().uv(42, 0).cuboid(-1.05F, -1.2F, -3.85F, 2.1F, 0.55F, 0.7F),
                ModelTransform.NONE);

        ModelPartData helmet = head.addChild("winged_helmet",
                ModelPartBuilder.create()
                        .uv(0, 16).cuboid(-3.35F, -7.7F, -3.8F, 6.7F, 2.1F, 7.3F)
                        .uv(34, 16).cuboid(-0.85F, -9.0F, -3.85F, 1.7F, 2.4F, 1.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        helmet.addChild("helmet_crest",
                ModelPartBuilder.create().uv(34, 16).cuboid(-0.85F, -9.0F, -3.85F, 1.7F, 2.4F, 1.2F),
                ModelTransform.NONE);
        helmet.addChild("left_helmet_wing",
                ModelPartBuilder.create()
                        .uv(44, 16).cuboid(0.0F, -2.0F, -0.45F, 4.2F, 1.1F, 0.9F)
                        .uv(62, 16).cuboid(2.6F, -3.1F, -0.42F, 2.5F, 1.0F, 0.84F),
                ModelTransform.of(3.2F, -6.7F, -0.9F, -0.08F, -0.16F, -0.36F));
        helmet.addChild("right_helmet_wing",
                ModelPartBuilder.create()
                        .uv(44, 22).cuboid(-4.2F, -2.0F, -0.45F, 4.2F, 1.1F, 0.9F)
                        .uv(62, 22).cuboid(-5.1F, -3.1F, -0.42F, 2.5F, 1.0F, 0.84F),
                ModelTransform.of(-3.2F, -6.7F, -0.9F, -0.08F, 0.16F, 0.36F));
        helmet.addChild("left_helmet_feather_tip",
                ModelPartBuilder.create().uv(62, 16).cuboid(6.2F, -10.2F, -1.2F, 2.8F, 3.2F, 2.4F),
                ModelTransform.NONE);
        helmet.addChild("right_helmet_feather_tip",
                ModelPartBuilder.create().uv(62, 22).cuboid(-9.0F, -10.2F, -1.2F, 2.8F, 3.2F, 2.4F),
                ModelTransform.NONE);

        ModelPartData armor = root.addChild("armor",
                ModelPartBuilder.create()
                        .uv(0, 36).cuboid(-4.1F, -0.8F, -2.4F, 8.2F, 12.0F, 4.8F)
                        .uv(30, 36).cuboid(-4.9F, -1.4F, -2.9F, 9.8F, 3.0F, 5.8F)
                        .uv(60, 36).cuboid(-1.4F, -0.8F, -3.2F, 2.8F, 10.8F, 0.9F)
                        .uv(72, 36).cuboid(-3.4F, 5.6F, -3.0F, 6.8F, 1.4F, 0.75F),
                ModelTransform.pivot(0.0F, 8.0F, 0.0F));
        armor.addChild("armor_plate",
                ModelPartBuilder.create().uv(30, 36).cuboid(-4.9F, -1.4F, -2.9F, 9.8F, 3.0F, 5.8F),
                ModelTransform.NONE);

        ModelPartData cape = root.addChild("cape",
                ModelPartBuilder.create()
                        .uv(88, 36).cuboid(-4.0F, 0.0F, 0.0F, 8.0F, 13.0F, 0.8F)
                        .uv(112, 36).cuboid(-3.8F, 11.0F, -0.1F, 2.2F, 3.5F, 0.9F),
                ModelTransform.of(0.0F, 7.2F, 2.5F, 0.12F, 0.0F, 0.0F));
        cape.addChild("cape_left_rip",
                ModelPartBuilder.create().uv(112, 36).cuboid(-3.8F, 11.0F, -0.1F, 2.2F, 3.5F, 0.9F),
                ModelTransform.NONE);
        cape.addChild("cape_right_rip",
                ModelPartBuilder.create().uv(122, 36).cuboid(1.5F, 10.7F, -0.1F, 2.3F, 3.8F, 0.9F),
                ModelTransform.NONE);

        ModelPartData leftWing = root.addChild("left_wing",
                ModelPartBuilder.create()
                        .uv(0, 58).cuboid(0.0F, -1.0F, -1.1F, 8.5F, 1.4F, 5.8F)
                        .uv(32, 58).cuboid(6.6F, -0.5F, 2.7F, 7.8F, 1.2F, 5.4F)
                        .uv(0, 70).cuboid(2.0F, 0.4F, 3.4F, 1.8F, 1.0F, 6.6F),
                ModelTransform.of(3.7F, 9.2F, 1.7F, 0.1F, 0.0F, 0.28F));
        leftWing.addChild("left_wing_primary_a",
                ModelPartBuilder.create().uv(0, 70).cuboid(2.0F, 0.4F, 3.4F, 1.8F, 1.0F, 6.6F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_primary_b",
                ModelPartBuilder.create().uv(10, 70).cuboid(5.4F, 0.4F, 4.0F, 1.8F, 1.0F, 7.2F),
                ModelTransform.NONE);
        leftWing.addChild("left_wing_primary_c",
                ModelPartBuilder.create().uv(20, 70).cuboid(8.8F, 0.4F, 4.5F, 1.8F, 1.0F, 7.8F),
                ModelTransform.NONE);
        ModelPartData rightWing = root.addChild("right_wing",
                ModelPartBuilder.create()
                        .uv(0, 58).mirrored().cuboid(-8.5F, -1.0F, -1.1F, 8.5F, 1.4F, 5.8F)
                        .uv(32, 58).mirrored().cuboid(-14.4F, -0.5F, 2.7F, 7.8F, 1.2F, 5.4F)
                        .uv(38, 70).mirrored().cuboid(-3.8F, 0.4F, 3.4F, 1.8F, 1.0F, 6.6F),
                ModelTransform.of(-3.7F, 9.2F, 1.7F, 0.1F, 0.0F, -0.28F));
        rightWing.addChild("right_wing_primary_a",
                ModelPartBuilder.create().uv(38, 70).mirrored().cuboid(-3.8F, 0.4F, 3.4F, 1.8F, 1.0F, 6.6F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_primary_b",
                ModelPartBuilder.create().uv(48, 70).mirrored().cuboid(-7.2F, 0.4F, 4.0F, 1.8F, 1.0F, 7.2F),
                ModelTransform.NONE);
        rightWing.addChild("right_wing_primary_c",
                ModelPartBuilder.create().uv(58, 70).mirrored().cuboid(-10.6F, 0.4F, 4.5F, 1.8F, 1.0F, 7.8F),
                ModelTransform.NONE);

        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(0, 90).cuboid(-1.2F, -1.0F, -1.2F, 2.4F, 10.5F, 2.4F)
                        .uv(12, 90).cuboid(-1.45F, 8.4F, -1.45F, 2.9F, 2.4F, 2.9F),
                ModelTransform.of(4.7F, 8.8F, -0.5F, -0.22F, 0.0F, -0.12F));
        leftArm.addChild("left_bracer",
                ModelPartBuilder.create().uv(12, 90).cuboid(-1.45F, 8.4F, -1.45F, 2.9F, 2.4F, 2.9F),
                ModelTransform.NONE);
        ModelPartData spearArm = root.addChild("spear_arm",
                ModelPartBuilder.create()
                        .uv(26, 90).cuboid(-1.2F, -1.0F, -1.2F, 2.4F, 10.5F, 2.4F)
                        .uv(38, 90).cuboid(-1.45F, 8.4F, -1.45F, 2.9F, 2.4F, 2.9F),
                ModelTransform.of(-4.7F, 8.8F, -0.7F, -0.55F, 0.0F, 0.14F));
        spearArm.addChild("spear_bracer",
                ModelPartBuilder.create().uv(38, 90).cuboid(-1.45F, 8.4F, -1.45F, 2.9F, 2.4F, 2.9F),
                ModelTransform.NONE);
        ModelPartData spear = spearArm.addChild("spear",
                ModelPartBuilder.create()
                        .uv(54, 88).cuboid(-0.35F, -12.0F, -0.35F, 0.7F, 24.0F, 0.7F),
                ModelTransform.of(-0.6F, 4.5F, -2.0F, 0.0F, 0.0F, -0.12F));
        spear.addChild("spear_blade",
                ModelPartBuilder.create().uv(62, 88).cuboid(-1.2F, -16.2F, -0.45F, 2.4F, 4.6F, 0.9F),
                ModelTransform.NONE);
        spear.addChild("spear_counterweight",
                ModelPartBuilder.create().uv(82, 88).cuboid(-1.1F, 9.5F, -0.45F, 2.2F, 2.0F, 0.9F),
                ModelTransform.NONE);

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 110).cuboid(-1.25F, 0.0F, -1.25F, 2.5F, 7.6F, 2.5F)
                        .uv(12, 110).cuboid(-1.55F, 6.8F, -2.0F, 3.1F, 1.5F, 3.6F),
                ModelTransform.pivot(1.8F, 15.7F, 0.0F));
        leftLeg.addChild("left_boot",
                ModelPartBuilder.create().uv(12, 110).cuboid(-1.55F, 6.8F, -2.0F, 3.1F, 1.5F, 3.6F),
                ModelTransform.NONE);
        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(28, 110).cuboid(-1.25F, 0.0F, -1.25F, 2.5F, 7.6F, 2.5F)
                        .uv(40, 110).cuboid(-1.55F, 6.8F, -2.0F, 3.1F, 1.5F, 3.6F),
                ModelTransform.pivot(-1.8F, 15.7F, 0.0F));
        rightLeg.addChild("right_boot",
                ModelPartBuilder.create().uv(40, 110).cuboid(-1.55F, 6.8F, -2.0F, 3.1F, 1.5F, 3.6F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(ValkyrieEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.55F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.55F;
        this.helmet.roll = MathHelper.sin(animationProgress * 0.08F) * 0.025F;

        float flapSpeed = entity.isFlying() ? 0.64F : 0.22F;
        float flapAmount = entity.isFlying() ? 0.9F : 0.24F;
        float flap = MathHelper.sin(animationProgress * flapSpeed) * flapAmount;
        this.leftWing.roll = 0.22F + flap;
        this.rightWing.roll = -0.22F - flap;
        this.leftWing.pitch = 0.1F + MathHelper.cos(animationProgress * flapSpeed) * 0.12F;
        this.rightWing.pitch = this.leftWing.pitch;
        this.cape.pitch = 0.12F + MathHelper.sin(animationProgress * 0.1F) * 0.06F;

        float drift = MathHelper.sin(animationProgress * 0.1F) * 0.05F;
        this.leftArm.pitch = -0.22F + drift;
        this.rightArm.pitch = -0.55F - drift;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.45F) * 0.28F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.45F + MathHelper.PI) * 0.28F * limbDistance;
        this.spear.roll = -0.12F;

        if (entity.handSwingProgress > 0.0F) {
            float thrust = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.35F - 0.28F * thrust;
            this.rightArm.yaw = -0.32F * thrust;
            this.body.pitch = -0.12F * thrust;
            this.spear.pitch = -0.35F * thrust;
            this.leftWing.roll += 0.2F * thrust;
            this.rightWing.roll -= 0.2F * thrust;
        } else {
            this.rightArm.yaw = 0.0F;
            this.body.pitch = 0.0F;
            this.spear.pitch = 0.0F;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 1.8F) * 0.08F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 0.95F * fall;
            this.head.pitch += 0.32F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
