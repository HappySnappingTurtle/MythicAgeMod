package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.DwarfBerserkerEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class DwarfBerserkerModel extends SinglePartEntityModel<DwarfBerserkerEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart helmet;
    private final ModelPart beard;
    private final ModelPart leftBraid;
    private final ModelPart rightBraid;
    private final ModelPart body;
    private final ModelPart cloakPack;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftAxe;
    private final ModelPart rightAxe;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public DwarfBerserkerModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.helmet = this.head.getChild("helmet");
        this.beard = this.head.getChild("beard");
        this.leftBraid = this.beard.getChild("left_braid");
        this.rightBraid = this.beard.getChild("right_braid");
        this.body = root.getChild("body");
        this.cloakPack = root.getChild("cloak_pack");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftAxe = this.leftArm.getChild("left_axe");
        this.rightAxe = this.rightArm.getChild("right_axe");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.85F, -6.2F, -3.65F, 7.7F, 6.2F, 7.3F),
                ModelTransform.of(0.0F, 9.4F, -0.8F, 0.06F, 0.0F, 0.0F));
        head.addChild("nose",
                ModelPartBuilder.create()
                        .uv(0, 15).cuboid(-1.0F, -3.3F, -4.55F, 2.0F, 1.0F, 0.85F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("left_eye",
                ModelPartBuilder.create()
                        .uv(9, 15).cuboid(-2.65F, -4.2F, -4.3F, 1.1F, 1.05F, 0.85F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("right_eye",
                ModelPartBuilder.create()
                        .uv(15, 15).cuboid(1.55F, -4.2F, -4.3F, 1.1F, 1.05F, 0.85F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("mouth_line",
                ModelPartBuilder.create()
                        .uv(22, 15).cuboid(-1.55F, -1.65F, -4.75F, 3.1F, 1.15F, 0.95F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData helmet = head.addChild("helmet",
                ModelPartBuilder.create()
                        .uv(36, 0).cuboid(-4.3F, -7.4F, -4.1F, 8.6F, 2.6F, 8.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        helmet.addChild("helmet_crown",
                ModelPartBuilder.create()
                        .uv(72, 0).cuboid(-3.4F, -8.5F, -3.35F, 6.8F, 1.6F, 6.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        helmet.addChild("helmet_nose_guard",
                ModelPartBuilder.create()
                        .uv(100, 0).cuboid(-0.75F, -8.2F, -4.75F, 1.5F, 4.1F, 0.85F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        helmet.addChild("left_rim",
                ModelPartBuilder.create()
                        .uv(106, 0).cuboid(0.0F, -0.55F, -0.55F, 4.2F, 1.1F, 1.1F),
                ModelTransform.of(4.1F, -6.25F, -1.35F, 0.0F, 0.0F, -0.24F));
        helmet.addChild("right_rim",
                ModelPartBuilder.create()
                        .uv(106, 4).cuboid(-4.2F, -0.55F, -0.55F, 4.2F, 1.1F, 1.1F),
                ModelTransform.of(-4.1F, -6.25F, -1.35F, 0.0F, 0.0F, 0.24F));

        ModelPartData beard = head.addChild("beard",
                ModelPartBuilder.create()
                        .uv(66, 10).cuboid(-3.7F, -2.1F, -4.5F, 7.4F, 6.8F, 1.6F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        beard.addChild("beard_center",
                ModelPartBuilder.create()
                        .uv(92, 10).cuboid(-1.15F, 2.4F, -4.9F, 2.3F, 4.1F, 1.25F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        beard.addChild("left_braid",
                ModelPartBuilder.create()
                        .uv(102, 10).cuboid(-1.0F, 0.0F, -0.55F, 2.0F, 5.4F, 1.1F)
                        .uv(110, 10).cuboid(-0.75F, 5.0F, -0.48F, 1.5F, 1.2F, 0.95F),
                ModelTransform.of(2.5F, 2.1F, -4.85F, 0.0F, 0.0F, -0.12F));
        beard.addChild("right_braid",
                ModelPartBuilder.create()
                        .uv(102, 18).cuboid(-1.0F, 0.0F, -0.55F, 2.0F, 5.4F, 1.1F)
                        .uv(110, 18).cuboid(-0.75F, 5.0F, -0.48F, 1.5F, 1.2F, 0.95F),
                ModelTransform.of(-2.5F, 2.1F, -4.85F, 0.0F, 0.0F, 0.12F));

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 24).cuboid(-5.3F, -1.1F, -3.0F, 10.6F, 9.9F, 6.0F),
                ModelTransform.pivot(0.0F, 10.5F, 0.0F));
        body.addChild("shoulder_armor",
                ModelPartBuilder.create()
                        .uv(36, 24).cuboid(-5.85F, -1.55F, -3.45F, 11.7F, 2.5F, 6.9F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        body.addChild("chest_panel",
                ModelPartBuilder.create()
                        .uv(72, 32).cuboid(-4.2F, 1.1F, -3.7F, 8.4F, 4.4F, 0.85F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        body.addChild("belt",
                ModelPartBuilder.create()
                        .uv(70, 24).cuboid(-5.7F, 5.65F, -3.55F, 11.4F, 2.0F, 7.1F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        body.addChild("belt_buckle",
                ModelPartBuilder.create()
                        .uv(102, 24).cuboid(-1.2F, 5.25F, -4.05F, 2.4F, 2.8F, 0.85F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData cloakPack = root.addChild("cloak_pack",
                ModelPartBuilder.create()
                        .uv(0, 38).cuboid(-4.4F, 0.0F, 0.0F, 8.8F, 7.0F, 1.15F),
                ModelTransform.of(0.0F, 9.65F, 3.0F, 0.08F, 0.0F, 0.0F));
        cloakPack.addChild("cloak_roll",
                ModelPartBuilder.create()
                        .uv(24, 38).cuboid(-3.1F, 1.2F, 0.55F, 6.2F, 4.4F, 1.35F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(0, 50).cuboid(-0.45F, -1.0F, -1.6F, 3.0F, 9.7F, 3.2F),
                ModelTransform.of(5.05F, 10.0F, -0.7F, -0.32F, 0.0F, -0.23F));
        leftArm.addChild("left_bracer",
                ModelPartBuilder.create()
                        .uv(14, 50).cuboid(-0.65F, 7.6F, -1.85F, 3.4F, 2.4F, 3.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData leftAxe = leftArm.addChild("left_axe",
                ModelPartBuilder.create()
                        .uv(34, 46).cuboid(-0.42F, -8.3F, -0.42F, 0.84F, 15.2F, 0.84F),
                ModelTransform.of(1.75F, 5.2F, -2.05F, 0.0F, 0.0F, 0.23F));
        leftAxe.addChild("left_axe_blade",
                ModelPartBuilder.create()
                        .uv(42, 46).cuboid(0.05F, -8.2F, -0.68F, 3.9F, 4.6F, 1.36F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        leftAxe.addChild("left_axe_hook",
                ModelPartBuilder.create()
                        .uv(58, 46).cuboid(2.95F, -6.9F, -0.6F, 1.6F, 2.4F, 1.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        leftAxe.addChild("left_axe_counterweight",
                ModelPartBuilder.create()
                        .uv(68, 46).cuboid(-2.5F, -7.6F, -0.6F, 2.2F, 3.5F, 1.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(0, 64).cuboid(-2.55F, -1.0F, -1.6F, 3.0F, 9.7F, 3.2F),
                ModelTransform.of(-5.05F, 10.0F, -0.7F, -0.32F, 0.0F, 0.23F));
        rightArm.addChild("right_bracer",
                ModelPartBuilder.create()
                        .uv(14, 64).cuboid(-2.75F, 7.6F, -1.85F, 3.4F, 2.4F, 3.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData rightAxe = rightArm.addChild("right_axe",
                ModelPartBuilder.create()
                        .uv(52, 58).cuboid(-0.42F, -8.3F, -0.42F, 0.84F, 15.2F, 0.84F),
                ModelTransform.of(-1.75F, 5.2F, -2.05F, 0.0F, 0.0F, -0.23F));
        rightAxe.addChild("right_axe_blade",
                ModelPartBuilder.create()
                        .uv(60, 58).cuboid(-3.95F, -8.2F, -0.68F, 3.9F, 4.6F, 1.36F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        rightAxe.addChild("right_axe_hook",
                ModelPartBuilder.create()
                        .uv(76, 58).cuboid(-4.55F, -6.9F, -0.6F, 1.6F, 2.4F, 1.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        rightAxe.addChild("right_axe_counterweight",
                ModelPartBuilder.create()
                        .uv(86, 58).cuboid(0.3F, -7.6F, -0.6F, 2.2F, 3.5F, 1.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 82).cuboid(-1.35F, 0.0F, -1.45F, 2.7F, 5.9F, 2.9F),
                ModelTransform.pivot(2.2F, 17.25F, 0.0F));
        leftLeg.addChild("left_boot",
                ModelPartBuilder.create()
                        .uv(12, 82).cuboid(-1.85F, 5.1F, -2.65F, 3.7F, 1.65F, 4.3F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(30, 82).cuboid(-1.35F, 0.0F, -1.45F, 2.7F, 5.9F, 2.9F),
                ModelTransform.pivot(-2.2F, 17.25F, 0.0F));
        rightLeg.addChild("right_boot",
                ModelPartBuilder.create()
                        .uv(42, 82).cuboid(-1.85F, 5.1F, -2.65F, 3.7F, 1.65F, 4.3F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(DwarfBerserkerEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = 0.06F + headPitch * (MathHelper.PI / 180.0F) * 0.55F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.52F;
        this.helmet.roll = MathHelper.sin(animationProgress * 0.11F) * 0.025F;
        this.beard.pitch = 0.04F + MathHelper.sin(animationProgress * 0.12F) * 0.045F;
        this.leftBraid.roll = -0.12F + MathHelper.sin(animationProgress * 0.13F) * 0.035F;
        this.rightBraid.roll = 0.12F - MathHelper.sin(animationProgress * 0.13F) * 0.035F;

        float quickStep = MathHelper.sin(limbAngle * 1.15F) * limbDistance;
        float counterStep = MathHelper.sin(limbAngle * 1.15F + MathHelper.PI) * limbDistance;
        this.leftLeg.pitch = quickStep * 0.75F;
        this.rightLeg.pitch = counterStep * 0.75F;
        this.leftArm.pitch = -0.42F + counterStep * 0.45F;
        this.rightArm.pitch = -0.42F + quickStep * 0.45F;
        this.leftArm.roll = -0.23F;
        this.rightArm.roll = 0.23F;
        this.leftAxe.roll = 0.23F;
        this.rightAxe.roll = -0.23F;
        this.cloakPack.pitch = 0.08F + MathHelper.sin(animationProgress * 0.09F) * 0.035F;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.body.pitch = -0.16F * swing;
            this.leftArm.pitch = -1.15F - 0.55F * swing;
            this.rightArm.pitch = -1.35F + 0.35F * swing;
            this.leftArm.yaw = 0.32F * swing;
            this.rightArm.yaw = -0.32F * swing;
            this.leftAxe.roll = 0.55F + 0.28F * swing;
            this.rightAxe.roll = -0.55F - 0.28F * swing;
            this.beard.pitch += 0.12F * swing;
            this.leftBraid.roll -= 0.06F * swing;
            this.rightBraid.roll += 0.06F * swing;
        } else {
            this.body.pitch = 0.0F;
            this.leftArm.yaw = 0.0F;
            this.rightArm.yaw = 0.0F;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 2.2F) * 0.1F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 0.82F * fall;
            this.head.pitch += 0.2F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
