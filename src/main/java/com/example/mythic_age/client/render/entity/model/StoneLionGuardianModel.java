package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.StoneLionGuardianEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class StoneLionGuardianModel extends SinglePartEntityModel<StoneLionGuardianEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart mane;
    private final ModelPart body;
    private final ModelPart ball;
    private final ModelPart leftFrontPaw;
    private final ModelPart rightFrontPaw;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;

    public StoneLionGuardianModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.mane = this.head.getChild("curled_mane");
        this.body = root.getChild("stone_body");
        this.ball = root.getChild("embroidered_ball");
        this.leftFrontPaw = root.getChild("left_front_paw");
        this.rightFrontPaw = root.getChild("right_front_paw");
        this.leftBackLeg = root.getChild("left_back_leg");
        this.rightBackLeg = root.getChild("right_back_leg");
        this.tail = root.getChild("curled_tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData stoneBody = root.addChild("stone_body",
                ModelPartBuilder.create()
                        .uv(0, 34).cuboid(-6.4F, -6.6F, -8.5F, 12.8F, 10.8F, 15.8F),
                ModelTransform.pivot(0.0F, 14.2F, 1.2F));
        stoneBody.addChild("shoulder_plate",
                ModelPartBuilder.create()
                        .uv(56, 34).cuboid(-5.1F, -7.1F, -5.2F, 10.2F, 2.2F, 9.5F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        stoneBody.addChild("stone_plinth",
                ModelPartBuilder.create()
                        .uv(88, 34).cuboid(-5.4F, 0.8F, -7.3F, 10.8F, 3.8F, 13.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.65F, -4.9F, -5.7F, 9.3F, 7.4F, 7.0F),
                ModelTransform.pivot(0.0F, 9.2F, -7.0F));
        head.addChild("snout",
                ModelPartBuilder.create()
                        .uv(40, 0).cuboid(-3.05F, -1.45F, -9.3F, 6.1F, 3.3F, 3.9F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("left_eye",
                ModelPartBuilder.create()
                        .uv(70, 0).cuboid(-3.35F, -3.0F, -6.25F, 1.1F, 1.1F, 0.75F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("right_eye",
                ModelPartBuilder.create()
                        .uv(76, 0).cuboid(2.25F, -3.0F, -6.25F, 1.1F, 1.1F, 0.75F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("nose",
                ModelPartBuilder.create()
                        .uv(82, 0).cuboid(-0.9F, -0.35F, -9.7F, 1.8F, 1.0F, 0.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("left_fang",
                ModelPartBuilder.create()
                        .uv(90, 0).cuboid(-2.6F, 1.6F, -9.9F, 1.2F, 1.6F, 0.9F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("right_fang",
                ModelPartBuilder.create()
                        .uv(96, 0).cuboid(1.4F, 1.6F, -9.9F, 1.2F, 1.6F, 0.9F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData curledMane = head.addChild("curled_mane",
                ModelPartBuilder.create()
                        .uv(0, 12).cuboid(-6.1F, -6.9F, -4.4F, 12.2F, 2.8F, 6.6F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        curledMane.addChild("mane_left",
                ModelPartBuilder.create()
                        .uv(44, 14).cuboid(-6.7F, -4.5F, -4.7F, 2.5F, 7.6F, 6.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        curledMane.addChild("mane_right",
                ModelPartBuilder.create()
                        .uv(60, 14).cuboid(4.2F, -4.5F, -4.7F, 2.5F, 7.6F, 6.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        curledMane.addChild("mane_bottom",
                ModelPartBuilder.create()
                        .uv(76, 14).cuboid(-4.4F, 1.9F, -4.4F, 8.8F, 2.9F, 6.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        curledMane.addChild("mane_top_curl",
                ModelPartBuilder.create()
                        .uv(108, 14).cuboid(-1.4F, -8.8F, -4.0F, 2.8F, 2.6F, 2.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        curledMane.addChild("mane_left_curl_a",
                ModelPartBuilder.create()
                        .uv(0, 24).cuboid(-5.8F, -6.0F, -5.5F, 2.7F, 2.7F, 2.3F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        curledMane.addChild("mane_right_curl_a",
                ModelPartBuilder.create()
                        .uv(12, 24).cuboid(3.1F, -6.0F, -5.5F, 2.7F, 2.7F, 2.3F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        curledMane.addChild("mane_left_curl_b",
                ModelPartBuilder.create()
                        .uv(24, 24).cuboid(-6.2F, -1.7F, -5.6F, 2.4F, 2.4F, 2.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        curledMane.addChild("mane_right_curl_b",
                ModelPartBuilder.create()
                        .uv(36, 24).cuboid(3.8F, -1.7F, -5.6F, 2.4F, 2.4F, 2.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData leftFrontPaw = root.addChild("left_front_paw",
                ModelPartBuilder.create()
                        .uv(0, 70).cuboid(-2.2F, -0.8F, -2.5F, 4.4F, 5.8F, 4.8F),
                ModelTransform.pivot(3.8F, 16.7F, -4.8F));
        leftFrontPaw.addChild("left_front_paw_pad",
                ModelPartBuilder.create()
                        .uv(20, 70).cuboid(-2.8F, 3.8F, -3.5F, 5.6F, 2.0F, 5.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        leftFrontPaw.addChild("left_front_claw_a",
                ModelPartBuilder.create()
                        .uv(44, 70).cuboid(-2.4F, 5.1F, -4.1F, 1.0F, 0.9F, 1.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        leftFrontPaw.addChild("left_front_claw_b",
                ModelPartBuilder.create()
                        .uv(50, 70).cuboid(-0.5F, 5.1F, -4.2F, 1.0F, 0.9F, 1.5F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        leftFrontPaw.addChild("left_front_claw_c",
                ModelPartBuilder.create()
                        .uv(56, 70).cuboid(1.4F, 5.1F, -4.1F, 1.0F, 0.9F, 1.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData rightFrontPaw = root.addChild("right_front_paw",
                ModelPartBuilder.create()
                        .uv(0, 84).cuboid(-2.2F, -0.8F, -2.5F, 4.4F, 5.8F, 4.8F),
                ModelTransform.pivot(-3.8F, 16.7F, -4.8F));
        rightFrontPaw.addChild("right_front_paw_pad",
                ModelPartBuilder.create()
                        .uv(20, 84).cuboid(-2.8F, 3.8F, -3.5F, 5.6F, 2.0F, 5.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        rightFrontPaw.addChild("right_front_claw_a",
                ModelPartBuilder.create()
                        .uv(44, 84).cuboid(-2.4F, 5.1F, -4.1F, 1.0F, 0.9F, 1.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        rightFrontPaw.addChild("right_front_claw_b",
                ModelPartBuilder.create()
                        .uv(50, 84).cuboid(-0.5F, 5.1F, -4.2F, 1.0F, 0.9F, 1.5F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        rightFrontPaw.addChild("right_front_claw_c",
                ModelPartBuilder.create()
                        .uv(56, 84).cuboid(1.4F, 5.1F, -4.1F, 1.0F, 0.9F, 1.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData leftBackLeg = root.addChild("left_back_leg",
                ModelPartBuilder.create()
                        .uv(68, 70).cuboid(-2.2F, -1.0F, -2.0F, 4.4F, 6.0F, 4.0F),
                ModelTransform.pivot(4.0F, 16.7F, 4.3F));
        leftBackLeg.addChild("left_back_paw",
                ModelPartBuilder.create()
                        .uv(88, 70).cuboid(-2.6F, 3.9F, -2.5F, 5.2F, 1.8F, 4.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData rightBackLeg = root.addChild("right_back_leg",
                ModelPartBuilder.create()
                        .uv(68, 84).cuboid(-2.2F, -1.0F, -2.0F, 4.4F, 6.0F, 4.0F),
                ModelTransform.pivot(-4.0F, 16.7F, 4.3F));
        rightBackLeg.addChild("right_back_paw",
                ModelPartBuilder.create()
                        .uv(88, 84).cuboid(-2.6F, 3.9F, -2.5F, 5.2F, 1.8F, 4.8F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData ball = root.addChild("embroidered_ball",
                ModelPartBuilder.create()
                        .uv(0, 102).cuboid(-2.3F, -2.3F, -2.3F, 4.6F, 4.6F, 4.6F),
                ModelTransform.pivot(0.0F, 20.0F, -7.2F));
        ball.addChild("ball_horizontal_ribbon",
                ModelPartBuilder.create()
                        .uv(20, 102).cuboid(-2.8F, -0.45F, -2.85F, 5.6F, 0.9F, 5.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ball.addChild("ball_vertical_ribbon",
                ModelPartBuilder.create()
                        .uv(48, 102).cuboid(-0.45F, -2.8F, -2.85F, 0.9F, 5.6F, 5.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ball.addChild("ball_top_knot",
                ModelPartBuilder.create().uv(72, 102).cuboid(-0.8F, -3.2F, -0.8F, 1.6F, 1.4F, 1.6F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData tail = root.addChild("curled_tail",
                ModelPartBuilder.create()
                        .uv(86, 102).cuboid(-1.3F, -1.3F, 0.0F, 2.6F, 2.6F, 6.5F),
                ModelTransform.of(0.0F, 12.3F, 7.3F, -0.45F, 0.0F, 0.0F));
        tail.addChild("tail_ball",
                ModelPartBuilder.create()
                        .uv(102, 102).cuboid(-2.1F, -2.1F, 4.8F, 4.2F, 4.2F, 4.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        tail.addChild("tail_curl",
                ModelPartBuilder.create()
                        .uv(86, 114).cuboid(-1.4F, -4.2F, 6.2F, 2.8F, 2.8F, 2.8F)
                        .uv(100, 114).cuboid(-1.0F, -5.2F, 5.6F, 2.0F, 2.0F, 2.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(StoneLionGuardianEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.42F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.45F;
        this.mane.pitch = this.head.pitch * 0.2F;
        this.mane.yaw = this.head.yaw * 0.18F;

        float heavyStep = MathHelper.cos(limbAngle * 0.48F) * limbDistance;
        float counterStep = MathHelper.cos(limbAngle * 0.48F + MathHelper.PI) * limbDistance;
        this.leftFrontPaw.pitch = counterStep * 0.32F;
        this.rightBackLeg.pitch = counterStep * 0.28F;
        this.rightFrontPaw.pitch = heavyStep * 0.32F;
        this.leftBackLeg.pitch = heavyStep * 0.28F;
        this.body.roll = MathHelper.sin(limbAngle * 0.48F) * limbDistance * 0.025F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.08F) * 0.07F;
        this.ball.yaw = MathHelper.sin(animationProgress * 0.06F) * 0.025F;

        if (entity.handSwingProgress > 0.0F) {
            float slam = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.head.pitch -= 0.12F * slam;
            this.leftFrontPaw.pitch -= 0.58F * slam;
            this.rightFrontPaw.pitch -= 0.58F * slam;
            this.body.pitch = -0.08F * slam;
            this.ball.pitch = 0.12F * slam;
        } else {
            this.body.pitch = 0.0F;
            this.ball.pitch = 0.0F;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 1.6F) * 0.08F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 22.0F);
            this.body.roll = 0.9F * fall;
            this.head.pitch += 0.35F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
