package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.DraugrWarriorEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class DraugrWarriorModel extends SinglePartEntityModel<DraugrWarriorEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart helmet;
    private final ModelPart body;
    private final ModelPart cape;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart shield;
    private final ModelPart axe;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public DraugrWarriorModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.helmet = this.head.getChild("helmet");
        this.body = root.getChild("torso");
        this.cape = root.getChild("ragged_cape");
        this.leftArm = root.getChild("shield_arm");
        this.rightArm = root.getChild("axe_arm");
        this.shield = this.leftArm.getChild("round_shield");
        this.axe = this.rightArm.getChild("battle_axe");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.25F, -7.2F, -4.2F, 6.5F, 8.0F, 8.0F),
                ModelTransform.of(0.0F, 7.6F, -1.2F, 0.08F, 0.0F, 0.0F));
        head.addChild("left_eye",
                ModelPartBuilder.create()
                        .uv(34, 0).cuboid(-2.45F, -4.6F, -4.75F, 1.2F, 1.45F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("right_eye",
                ModelPartBuilder.create()
                        .uv(42, 0).cuboid(1.25F, -4.6F, -4.75F, 1.2F, 1.45F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("mouth_line",
                ModelPartBuilder.create()
                        .uv(50, 0).cuboid(-1.6F, -1.7F, -4.8F, 3.2F, 0.65F, 0.75F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData helmet = head.addChild("helmet",
                ModelPartBuilder.create()
                        .uv(0, 18).cuboid(-3.8F, -8.25F, -4.8F, 7.6F, 2.2F, 9.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        helmet.addChild("helmet_crown",
                ModelPartBuilder.create()
                        .uv(38, 18).cuboid(-3.1F, -9.55F, -4.0F, 6.2F, 2.0F, 7.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        helmet.addChild("helmet_nose_guard",
                ModelPartBuilder.create()
                        .uv(68, 18).cuboid(-1.0F, -10.2F, -4.55F, 2.0F, 2.8F, 1.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        helmet.addChild("left_horn",
                ModelPartBuilder.create()
                        .uv(78, 18).cuboid(0.0F, -1.0F, -0.6F, 3.8F, 1.2F, 1.2F)
                        .uv(94, 18).cuboid(3.0F, -1.8F, -0.5F, 1.2F, 1.0F, 1.0F),
                ModelTransform.of(3.6F, -8.2F, -1.8F, -0.06F, -0.16F, -0.36F));
        helmet.addChild("right_horn",
                ModelPartBuilder.create()
                        .uv(78, 24).cuboid(-3.8F, -1.0F, -0.6F, 3.8F, 1.2F, 1.2F)
                        .uv(94, 24).cuboid(-4.2F, -1.8F, -0.5F, 1.2F, 1.0F, 1.0F),
                ModelTransform.of(-3.6F, -8.2F, -1.8F, -0.06F, 0.16F, 0.36F));

        ModelPartData torso = root.addChild("torso",
                ModelPartBuilder.create()
                        .uv(0, 38).cuboid(-4.3F, -0.9F, -2.6F, 8.6F, 12.4F, 5.2F),
                ModelTransform.pivot(0.0F, 8.2F, 0.0F));
        torso.addChild("shoulder_armor",
                ModelPartBuilder.create()
                        .uv(30, 38).cuboid(-4.9F, -1.4F, -3.0F, 9.8F, 3.0F, 6.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        torso.addChild("rib_marks",
                ModelPartBuilder.create()
                        .uv(62, 38).cuboid(-3.0F, 2.0F, -3.35F, 6.0F, 0.55F, 0.8F)
                        .uv(62, 42).cuboid(-2.7F, 4.0F, -3.35F, 5.4F, 0.55F, 0.8F)
                        .uv(62, 46).cuboid(-2.4F, 6.0F, -3.35F, 4.8F, 0.55F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        torso.addChild("belt_buckle",
                ModelPartBuilder.create()
                        .uv(96, 50).cuboid(-4.5F, 9.1F, -3.15F, 9.0F, 1.25F, 0.65F)
                        .uv(112, 50).cuboid(-1.2F, 8.75F, -3.55F, 2.4F, 2.0F, 0.85F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData raggedCape = root.addChild("ragged_cape",
                ModelPartBuilder.create()
                        .uv(74, 38).cuboid(-4.2F, 0.0F, 0.0F, 8.4F, 12.2F, 0.8F),
                ModelTransform.of(0.0F, 7.4F, 2.7F, 0.08F, 0.0F, 0.0F));
        raggedCape.addChild("cape_left_rip",
                ModelPartBuilder.create()
                        .uv(98, 38).cuboid(-4.2F, 10.6F, -0.1F, 2.2F, 3.0F, 0.9F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        raggedCape.addChild("cape_mid_rip",
                ModelPartBuilder.create()
                        .uv(108, 38).cuboid(-0.9F, 10.8F, -0.1F, 2.0F, 2.5F, 0.9F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        raggedCape.addChild("cape_right_rip",
                ModelPartBuilder.create()
                        .uv(118, 38).cuboid(2.0F, 10.3F, -0.1F, 2.2F, 3.4F, 0.9F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData shieldArm = root.addChild("shield_arm",
                ModelPartBuilder.create()
                        .uv(0, 64).cuboid(-1.4F, -1.0F, -1.4F, 2.8F, 10.8F, 2.8F),
                ModelTransform.of(4.8F, 9.0F, -1.0F, -0.35F, 0.0F, -0.18F));
        shieldArm.addChild("left_bracer",
                ModelPartBuilder.create()
                        .uv(14, 64).cuboid(-1.65F, 8.4F, -1.65F, 3.3F, 2.6F, 3.3F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData roundShield = shieldArm.addChild("round_shield",
                ModelPartBuilder.create()
                        .uv(30, 64).cuboid(-4.4F, -4.4F, -0.55F, 8.8F, 8.8F, 1.1F),
                ModelTransform.of(0.8F, 4.8F, -2.2F, 0.0F, -0.18F, 0.12F));
        roundShield.addChild("shield_inner_ring",
                ModelPartBuilder.create()
                        .uv(66, 64).cuboid(-3.0F, -3.0F, -0.75F, 6.0F, 6.0F, 0.65F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        roundShield.addChild("shield_boss",
                ModelPartBuilder.create()
                        .uv(90, 64).cuboid(-0.8F, -0.8F, -0.95F, 1.6F, 1.6F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        roundShield.addChild("shield_crack_a",
                ModelPartBuilder.create()
                        .uv(100, 64).cuboid(-3.45F, -3.65F, -1.15F, 0.55F, 4.6F, 0.35F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        roundShield.addChild("shield_crack_b",
                ModelPartBuilder.create()
                        .uv(106, 64).cuboid(1.35F, -0.7F, -1.15F, 0.55F, 3.75F, 0.35F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData axeArm = root.addChild("axe_arm",
                ModelPartBuilder.create()
                        .uv(0, 82).cuboid(-1.4F, -1.0F, -1.4F, 2.8F, 10.8F, 2.8F),
                ModelTransform.of(-4.8F, 9.0F, -0.8F, -0.45F, 0.0F, 0.18F));
        axeArm.addChild("right_bracer",
                ModelPartBuilder.create()
                        .uv(14, 82).cuboid(-1.65F, 8.4F, -1.65F, 3.3F, 2.6F, 3.3F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData battleAxe = axeArm.addChild("battle_axe",
                ModelPartBuilder.create()
                        .uv(30, 82).cuboid(-0.45F, -8.0F, -0.45F, 0.9F, 16.0F, 0.9F),
                ModelTransform.of(-0.8F, 4.9F, -2.0F, 0.0F, 0.0F, -0.25F));
        battleAxe.addChild("axe_blade",
                ModelPartBuilder.create()
                        .uv(38, 82).cuboid(-4.0F, -8.0F, -0.75F, 3.9F, 4.6F, 1.5F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        battleAxe.addChild("axe_hook",
                ModelPartBuilder.create()
                        .uv(56, 82).cuboid(-5.0F, -6.6F, -0.75F, 1.6F, 2.5F, 1.5F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        battleAxe.addChild("axe_spike",
                ModelPartBuilder.create()
                        .uv(68, 82).cuboid(0.0F, -7.3F, -0.65F, 1.7F, 3.2F, 1.3F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 104).cuboid(-1.4F, 0.0F, -1.4F, 2.8F, 7.6F, 2.8F),
                ModelTransform.pivot(2.0F, 15.7F, 0.0F));
        leftLeg.addChild("left_boot",
                ModelPartBuilder.create()
                        .uv(12, 104).cuboid(-1.8F, 6.8F, -2.3F, 3.6F, 1.5F, 4.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(30, 104).cuboid(-1.4F, 0.0F, -1.4F, 2.8F, 7.6F, 2.8F),
                ModelTransform.pivot(-2.0F, 15.7F, 0.0F));
        rightLeg.addChild("right_boot",
                ModelPartBuilder.create()
                        .uv(42, 104).cuboid(-1.8F, 6.8F, -2.3F, 3.6F, 1.5F, 4.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(DraugrWarriorEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = 0.08F + headPitch * (MathHelper.PI / 180.0F) * 0.48F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.48F;
        this.helmet.roll = MathHelper.sin(animationProgress * 0.08F) * 0.025F;

        float dragStep = MathHelper.sin(limbAngle * 0.58F) * limbDistance;
        float counterDrag = MathHelper.sin(limbAngle * 0.58F + MathHelper.PI) * limbDistance;
        this.leftLeg.pitch = counterDrag * 0.42F;
        this.rightLeg.pitch = dragStep * 0.3F;
        this.leftArm.pitch = -0.35F + MathHelper.sin(animationProgress * 0.06F) * 0.025F;
        this.rightArm.pitch = -0.45F + dragStep * 0.18F;
        this.cape.pitch = 0.08F + MathHelper.sin(animationProgress * 0.07F) * 0.04F;
        this.shield.yaw = -0.18F;
        this.axe.roll = -0.25F;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.45F - 0.35F * swing;
            this.rightArm.yaw = -0.45F * swing;
            this.leftArm.pitch = -0.55F - 0.28F * swing;
            this.body.pitch = -0.08F * swing;
            this.axe.roll = -0.55F - 0.25F * swing;
            this.shield.pitch = -0.18F * swing;
        } else {
            this.body.pitch = 0.0F;
            this.rightArm.yaw = 0.0F;
            this.shield.pitch = 0.0F;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 2.0F) * 0.1F;
        } else {
            this.head.roll = 0.0F;
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
