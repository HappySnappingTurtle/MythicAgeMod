package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.TidalGuardianEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class TidalGuardianModel extends SinglePartEntityModel<TidalGuardianEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart shell;
    private final ModelPart leftClaw;
    private final ModelPart rightClaw;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;
    private final ModelPart tailFin;
    private final ModelPart crystals;

    public TidalGuardianModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.shell = root.getChild("shell");
        this.leftClaw = root.getChild("left_claw");
        this.rightClaw = root.getChild("right_claw");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
        this.tailFin = root.getChild("tail_fin");
        this.crystals = root.getChild("shell_crystals");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData shell = root.addChild("shell",
                ModelPartBuilder.create()
                        .uv(0, 32).cuboid(-8.0F, -6.0F, -8.0F, 16.0F, 8.0F, 16.0F)
                        .uv(0, 58).cuboid(-7.0F, 1.2F, -7.0F, 14.0F, 2.0F, 14.0F),
                ModelTransform.pivot(0.0F, 14.0F, 0.0F));
        shell.addChild("shell_plate_front",
                ModelPartBuilder.create().uv(54, 32).cuboid(-5.8F, -7.25F, -6.8F, 11.6F, 2.1F, 3.2F),
                ModelTransform.NONE);
        shell.addChild("shell_plate_back",
                ModelPartBuilder.create().uv(86, 32).cuboid(-5.8F, -7.25F, 3.6F, 11.6F, 2.1F, 3.2F),
                ModelTransform.NONE);
        shell.addChild("shell_plate_left",
                ModelPartBuilder.create().uv(54, 46).cuboid(4.2F, -7.35F, -3.6F, 2.8F, 2.0F, 7.2F),
                ModelTransform.NONE);
        shell.addChild("shell_plate_right",
                ModelPartBuilder.create().uv(0, 105).cuboid(-7.0F, -7.35F, -3.6F, 2.8F, 2.0F, 7.2F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.2F, -4.0F, -12.0F, 6.4F, 5.0F, 5.0F)
                        .uv(42, 0).cuboid(-2.0F, -0.1F, -12.8F, 4.0F, 0.9F, 0.9F),
                ModelTransform.pivot(0.0F, 13.4F, 0.0F));
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(28, 0).cuboid(-2.2F, -2.8F, -12.8F, 1.1F, 1.1F, 0.9F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(34, 0).cuboid(1.1F, -2.8F, -12.8F, 1.1F, 1.1F, 0.9F),
                ModelTransform.NONE);
        head.addChild("beak_mouth",
                ModelPartBuilder.create()
                        .uv(42, 4).cuboid(-1.8F, 0.8F, -12.9F, 3.6F, 0.7F, 0.8F),
                ModelTransform.NONE);
        head.addChild("lower_mandible",
                ModelPartBuilder.create()
                        .uv(54, 16).cuboid(-2.5F, 1.1F, -11.9F, 5.0F, 1.0F, 2.4F),
                ModelTransform.NONE);

        ModelPartData leftClaw = root.addChild("left_claw",
                ModelPartBuilder.create()
                        .uv(54, 0).cuboid(0.0F, -2.3F, -3.5F, 5.8F, 5.3F, 7.0F),
                ModelTransform.of(5.8F, 14.0F, -8.5F, 0.0F, -0.18F, 0.08F));
        leftClaw.addChild("left_claw_upper_tip",
                ModelPartBuilder.create().uv(82, 0).cuboid(3.7F, -2.9F, -4.3F, 3.2F, 2.5F, 3.8F),
                ModelTransform.NONE);
        leftClaw.addChild("left_claw_lower_tip",
                ModelPartBuilder.create().uv(22, 92).cuboid(3.7F, 0.9F, -4.3F, 3.2F, 2.4F, 3.8F),
                ModelTransform.NONE);

        ModelPartData rightClaw = root.addChild("right_claw",
                ModelPartBuilder.create()
                        .uv(54, 0).mirrored().cuboid(-5.8F, -2.3F, -3.5F, 5.8F, 5.3F, 7.0F),
                ModelTransform.of(-5.8F, 14.0F, -8.5F, 0.0F, 0.18F, -0.08F));
        rightClaw.addChild("right_claw_upper_tip",
                ModelPartBuilder.create().uv(98, 0).mirrored().cuboid(-6.9F, -2.9F, -4.3F, 3.2F, 2.5F, 3.8F),
                ModelTransform.NONE);
        rightClaw.addChild("right_claw_lower_tip",
                ModelPartBuilder.create().uv(38, 92).mirrored().cuboid(-6.9F, 0.9F, -4.3F, 3.2F, 2.4F, 3.8F),
                ModelTransform.NONE);

        ModelPartData frontLeftLeg = root.addChild("front_left_leg",
                ModelPartBuilder.create()
                        .uv(0, 76).cuboid(-1.2F, -0.6F, -1.5F, 3.3F, 5.0F, 3.0F),
                ModelTransform.pivot(5.2F, 16.0F, -5.0F));
        frontLeftLeg.addChild("front_left_foot_claws",
                ModelPartBuilder.create().uv(16, 76).cuboid(-1.8F, 3.6F, -2.2F, 4.0F, 1.5F, 4.2F),
                ModelTransform.NONE);
        ModelPartData frontRightLeg = root.addChild("front_right_leg",
                ModelPartBuilder.create()
                        .uv(0, 76).mirrored().cuboid(-2.1F, -0.6F, -1.5F, 3.3F, 5.0F, 3.0F),
                ModelTransform.pivot(-5.2F, 16.0F, -5.0F));
        frontRightLeg.addChild("front_right_foot_claws",
                ModelPartBuilder.create().uv(78, 92).cuboid(-2.2F, 3.6F, -2.2F, 4.0F, 1.5F, 4.2F),
                ModelTransform.NONE);
        ModelPartData backLeftLeg = root.addChild("back_left_leg",
                ModelPartBuilder.create()
                        .uv(52, 76).cuboid(-1.2F, -0.6F, -1.5F, 3.3F, 5.0F, 3.0F),
                ModelTransform.pivot(5.2F, 16.0F, 5.0F));
        backLeftLeg.addChild("back_left_foot_claws",
                ModelPartBuilder.create().uv(68, 76).cuboid(-1.8F, 3.6F, -2.0F, 4.0F, 1.5F, 4.0F),
                ModelTransform.NONE);
        ModelPartData backRightLeg = root.addChild("back_right_leg",
                ModelPartBuilder.create()
                        .uv(52, 76).mirrored().cuboid(-2.1F, -0.6F, -1.5F, 3.3F, 5.0F, 3.0F),
                ModelTransform.pivot(-5.2F, 16.0F, 5.0F));
        backRightLeg.addChild("back_right_foot_claws",
                ModelPartBuilder.create().uv(86, 76).mirrored().cuboid(-2.2F, 3.6F, -2.0F, 4.0F, 1.5F, 4.0F),
                ModelTransform.NONE);

        ModelPartData shellCrystals = root.addChild("shell_crystals",
                ModelPartBuilder.create(),
                ModelTransform.pivot(0.0F, 9.0F, 0.0F));
        shellCrystals.addChild("crystal_plate_1",
                ModelPartBuilder.create().uv(64, 58).cuboid(-1.0F, -5.5F, -2.0F, 2.0F, 4.0F, 3.0F),
                ModelTransform.NONE);
        shellCrystals.addChild("crystal_plate_2",
                ModelPartBuilder.create().uv(74, 58).cuboid(-1.0F, -5.0F, 3.0F, 2.0F, 3.8F, 3.0F),
                ModelTransform.NONE);
        shellCrystals.addChild("crystal_plate_3",
                ModelPartBuilder.create().uv(84, 58).cuboid(-4.0F, -4.2F, 0.0F, 2.0F, 3.2F, 2.6F),
                ModelTransform.NONE);
        shellCrystals.addChild("crystal_plate_4",
                ModelPartBuilder.create().uv(94, 58).cuboid(2.0F, -4.2F, 0.0F, 2.0F, 3.2F, 2.6F),
                ModelTransform.NONE);

        ModelPartData tailFin = root.addChild("tail_fin",
                ModelPartBuilder.create()
                        .uv(84, 86).cuboid(-3.5F, -1.0F, 0.0F, 7.0F, 3.0F, 4.0F),
                ModelTransform.of(0.0F, 14.8F, 8.0F, 0.08F, 0.0F, 0.0F));
        tailFin.addChild("tail_fin_tip",
                ModelPartBuilder.create().uv(106, 86).cuboid(-1.1F, -0.4F, 3.4F, 2.2F, 1.8F, 3.2F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(TidalGuardianEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.45F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.55F;
        this.shell.pitch = MathHelper.sin(animationProgress * 0.055F) * 0.018F;
        this.crystals.pitch = this.shell.pitch;

        float crawl = 0.55F * limbDistance;
        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.65F) * crawl;
        this.backRightLeg.pitch = MathHelper.cos(limbAngle * 0.65F) * crawl;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.65F + MathHelper.PI) * crawl;
        this.backLeftLeg.pitch = this.frontRightLeg.pitch;
        this.tailFin.yaw = MathHelper.sin(animationProgress * 0.12F) * 0.1F;

        float clawIdle = MathHelper.sin(animationProgress * 0.1F) * 0.035F;
        this.leftClaw.yaw = -0.18F + clawIdle;
        this.rightClaw.yaw = 0.18F - clawIdle;
        if (entity.handSwingProgress > 0.0F) {
            float pinch = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.leftClaw.yaw = -0.45F + pinch * 0.5F;
            this.rightClaw.yaw = 0.45F - pinch * 0.5F;
            this.shell.pitch -= 0.08F * pinch;
        }

        if (entity.hurtTime > 0) {
            float shake = MathHelper.sin(animationProgress * 2.2F) * 0.06F;
            this.shell.roll = shake;
            this.head.roll = -shake;
        } else {
            this.shell.roll = 0.0F;
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 22.0F);
            this.shell.roll = 1.0F * fall;
            this.head.pitch += 0.25F * fall;
            this.leftClaw.yaw *= 1.0F - fall * 0.5F;
            this.rightClaw.yaw *= 1.0F - fall * 0.5F;
            this.tailFin.yaw *= 1.0F - fall * 0.7F;
            this.crystals.pitch += 0.12F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
