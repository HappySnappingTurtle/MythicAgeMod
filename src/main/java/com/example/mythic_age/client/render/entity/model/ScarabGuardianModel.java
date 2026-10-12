package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.ScarabGuardianEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class ScarabGuardianModel extends SinglePartEntityModel<ScarabGuardianEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart horn;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftMiddleLeg;
    private final ModelPart rightMiddleLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;

    public ScarabGuardianModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.horn = this.head.getChild("horn");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftMiddleLeg = root.getChild("left_middle_leg");
        this.rightMiddleLeg = root.getChild("right_middle_leg");
        this.leftBackLeg = root.getChild("left_back_leg");
        this.rightBackLeg = root.getChild("right_back_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 20).cuboid(-8.0F, -5.0F, -9.0F, 16.0F, 8.0F, 18.0F),
                ModelTransform.pivot(0.0F, 18.0F, 2.0F));
        body.addChild("gold_carapace",
                ModelPartBuilder.create().uv(68, 20).cuboid(-6.2F, -7.0F, -6.6F, 12.4F, 3.2F, 12.8F),
                ModelTransform.NONE);
        body.addChild("carapace_ridge",
                ModelPartBuilder.create().uv(68, 38).cuboid(-1.0F, -7.4F, -5.8F, 2.0F, 3.7F, 11.6F),
                ModelTransform.NONE);
        body.addChild("left_shell_plate",
                ModelPartBuilder.create().uv(82, 38).cuboid(2.0F, -6.8F, -5.8F, 3.8F, 2.4F, 11.4F),
                ModelTransform.NONE);
        body.addChild("right_shell_plate",
                ModelPartBuilder.create().uv(100, 38).cuboid(-5.8F, -6.8F, -5.8F, 3.8F, 2.4F, 11.4F),
                ModelTransform.NONE);
        body.addChild("rear_shell_plate",
                ModelPartBuilder.create().uv(76, 54).cuboid(-4.5F, -6.5F, 4.8F, 9.0F, 2.3F, 3.6F),
                ModelTransform.NONE);
        body.addChild("scarab_sigil",
                ModelPartBuilder.create().uv(98, 54).cuboid(-2.0F, -7.5F, -2.8F, 4.0F, 3.5F, 5.6F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-5.0F, -4.0F, -5.5F, 10.0F, 5.5F, 6.5F),
                ModelTransform.pivot(0.0F, 18.0F, -8.5F));
        head.addChild("left_mandible",
                ModelPartBuilder.create().uv(34, 0).cuboid(2.5F, -1.6F, -6.1F, 2.5F, 2.1F, 2.0F),
                ModelTransform.NONE);
        head.addChild("right_mandible",
                ModelPartBuilder.create().uv(44, 0).cuboid(-5.0F, -1.6F, -6.1F, 2.5F, 2.1F, 2.0F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(54, 0).cuboid(-3.7F, -2.8F, -5.9F, 1.0F, 1.0F, 0.8F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(60, 0).cuboid(2.7F, -2.8F, -5.9F, 1.0F, 1.0F, 0.8F),
                ModelTransform.NONE);

        ModelPartData horn = head.addChild("horn",
                ModelPartBuilder.create()
                        .uv(78, 0).cuboid(-1.2F, -1.2F, -7.5F, 2.4F, 2.4F, 8.0F),
                ModelTransform.pivot(0.0F, -2.0F, -5.5F));
        horn.addChild("horn_tip",
                ModelPartBuilder.create().uv(90, 0).cuboid(-0.8F, -0.8F, -10.2F, 1.6F, 1.6F, 3.0F),
                ModelTransform.NONE);

        ModelPartData leftFrontLeg = root.addChild("left_front_leg",
                ModelPartBuilder.create().uv(0, 52).cuboid(0.0F, -0.8F, -1.2F, 9.5F, 2.0F, 2.4F),
                ModelTransform.of(5.5F, 20.4F, -5.4F, 0.0F, 0.0F, 0.35F));
        leftFrontLeg.addChild("front_left_claw",
                ModelPartBuilder.create().uv(0, 74).cuboid(9.1F, -0.6F, -1.4F, 2.3F, 1.6F, 2.8F),
                ModelTransform.NONE);
        ModelPartData rightFrontLeg = root.addChild("right_front_leg",
                ModelPartBuilder.create().uv(0, 52).mirrored().cuboid(-9.5F, -0.8F, -1.2F, 9.5F, 2.0F, 2.4F),
                ModelTransform.of(-5.5F, 20.4F, -5.4F, 0.0F, 0.0F, -0.35F));
        rightFrontLeg.addChild("front_right_claw",
                ModelPartBuilder.create().uv(12, 74).cuboid(-11.4F, -0.6F, -1.4F, 2.3F, 1.6F, 2.8F),
                ModelTransform.NONE);
        ModelPartData leftMiddleLeg = root.addChild("left_middle_leg",
                ModelPartBuilder.create().uv(24, 52).cuboid(0.0F, -0.8F, -1.2F, 10.5F, 2.0F, 2.4F),
                ModelTransform.pivot(6.0F, 20.6F, 0.8F));
        leftMiddleLeg.addChild("mid_left_claw",
                ModelPartBuilder.create().uv(24, 74).cuboid(10.1F, -0.6F, -1.4F, 2.3F, 1.6F, 2.8F),
                ModelTransform.NONE);
        ModelPartData rightMiddleLeg = root.addChild("right_middle_leg",
                ModelPartBuilder.create().uv(24, 52).mirrored().cuboid(-10.5F, -0.8F, -1.2F, 10.5F, 2.0F, 2.4F),
                ModelTransform.pivot(-6.0F, 20.6F, 0.8F));
        rightMiddleLeg.addChild("mid_right_claw",
                ModelPartBuilder.create().uv(36, 74).cuboid(-12.4F, -0.6F, -1.4F, 2.3F, 1.6F, 2.8F),
                ModelTransform.NONE);
        ModelPartData leftBackLeg = root.addChild("left_back_leg",
                ModelPartBuilder.create().uv(52, 52).cuboid(0.0F, -0.8F, -1.2F, 9.2F, 2.0F, 2.4F),
                ModelTransform.of(5.5F, 20.4F, 6.8F, 0.0F, 0.0F, -0.28F));
        leftBackLeg.addChild("back_left_claw",
                ModelPartBuilder.create().uv(48, 74).cuboid(8.8F, -0.6F, -1.5F, 2.2F, 1.6F, 3.0F),
                ModelTransform.NONE);
        ModelPartData rightBackLeg = root.addChild("right_back_leg",
                ModelPartBuilder.create().uv(52, 52).mirrored().cuboid(-9.2F, -0.8F, -1.2F, 9.2F, 2.0F, 2.4F),
                ModelTransform.of(-5.5F, 20.4F, 6.8F, 0.0F, 0.0F, 0.28F));
        rightBackLeg.addChild("back_right_claw",
                ModelPartBuilder.create().uv(60, 74).cuboid(-11.0F, -0.6F, -1.5F, 2.2F, 1.6F, 3.0F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(ScarabGuardianEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.4F;
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.35F;
        this.horn.pitch = entity.handSwingProgress > 0.0F ? -0.28F : 0.0F;

        float scuttle = MathHelper.cos(limbAngle * 1.15F) * 0.45F * limbDistance;
        this.leftFrontLeg.yaw = -0.35F + scuttle;
        this.rightBackLeg.yaw = 0.28F + scuttle;
        this.rightFrontLeg.yaw = 0.35F - scuttle;
        this.leftBackLeg.yaw = -0.28F - scuttle;
        this.leftMiddleLeg.yaw = -scuttle * 0.7F;
        this.rightMiddleLeg.yaw = scuttle * 0.7F;
        this.body.pitch = entity.hurtTime > 0 ? MathHelper.sin(animationProgress * 1.8F) * 0.05F : 0.0F;
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.0F * fall;
            this.head.pitch += 0.25F * fall;
            this.horn.pitch -= 0.35F * fall;
            this.leftFrontLeg.yaw *= 0.45F;
            this.rightFrontLeg.yaw *= 0.45F;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
