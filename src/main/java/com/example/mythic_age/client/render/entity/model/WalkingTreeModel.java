package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.WalkingTreeEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class WalkingTreeModel extends SinglePartEntityModel<WalkingTreeEntity> {
    private static final float FRONT_OUTER_ROOT_BASE_PITCH = -1.20F;
    private static final float FRONT_OUTER_ROOT_BASE_YAW = 0.20F;
    private static final float FRONT_OUTER_ROOT_BASE_ROLL = 0.78F;
    private static final float SIDE_MID_ROOT_BASE_PITCH = -0.85F;
    private static final float SIDE_MID_ROOT_BASE_ROLL = 1.18F;
    private static final float BACK_OUTER_ROOT_BASE_PITCH = 1.02F;
    private static final float BACK_OUTER_ROOT_BASE_YAW = 0.14F;
    private static final float BACK_OUTER_ROOT_BASE_ROLL = 0.28F;
    private static final float ATTACK_ROOT_HALF_WIDTH = 1.55F;
    private static final float ATTACK_ROOT_LENGTH = 28.0F;
    private static final float ATTACK_ROOT_ORIGIN_X = 11.0F;
    private static final float ATTACK_ROOT_ORIGIN_Y = 15.5F;
    private static final float ATTACK_ROOT_ORIGIN_Z = -13.6F;
    private static final float ATTACK_ROOT_REST_PITCH = -1.16F;
    private static final float ATTACK_ROOT_REST_YAW = 0.20F;
    private static final float ATTACK_ROOT_REST_ROLL = 1.00F;
    private static final float ATTACK_ROOT_SWING_PITCH = 0.78F;
    private static final float ATTACK_ROOT_SWING_YAW = 0.20F;
    private static final float ATTACK_ROOT_SWING_ROLL = 0.84F;
    private static final float ATTACK_ROOT_STRIKE_PITCH = 0.28F;
    private static final float ATTACK_ROOT_STRIKE_YAW = 0.10F;
    private static final float ATTACK_ROOT_STRIKE_ROLL = 0.30F;
    private static final float ATTACK_ROOT_LIFT_END = 0.34F;
    private static final float ATTACK_ROOT_HOLD_END = 0.48F;
    private static final float ATTACK_ROOT_SLAM_END = 0.72F;

    private final ModelPart root;
    private final ModelPart canopy;
    private final ModelPart trunk;
    private final ModelPart rootCollar;
    private final ModelPart leftAttackRoot;
    private final ModelPart rightAttackRoot;
    private final ModelPart frontLeftSupportRoot;
    private final ModelPart frontRightSupportRoot;
    private final ModelPart leftMidSupportRoot;
    private final ModelPart rightMidSupportRoot;
    private final ModelPart backLeftSupportRoot;
    private final ModelPart backRightSupportRoot;
    private float attackProgress;

    public WalkingTreeModel(ModelPart root) {
        this.root = root;
        this.canopy = root.getChild("canopy");
        this.trunk = root.getChild("trunk");
        this.rootCollar = root.getChild("root_collar");
        this.leftAttackRoot = this.rootCollar.getChild("left_attack_root");
        this.rightAttackRoot = this.rootCollar.getChild("right_attack_root");
        this.frontLeftSupportRoot = this.rootCollar.getChild("front_outer_left_root");
        this.frontRightSupportRoot = this.rootCollar.getChild("front_outer_right_root");
        this.leftMidSupportRoot = this.rootCollar.getChild("left_mid_root");
        this.rightMidSupportRoot = this.rootCollar.getChild("right_mid_root");
        this.backLeftSupportRoot = this.rootCollar.getChild("back_outer_left_root");
        this.backRightSupportRoot = this.rootCollar.getChild("back_outer_right_root");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData canopy = root.addChild("canopy",
            ModelPartBuilder.create()
                .uv(0, 0).cuboid(-16.0F, -36.0F, -16.0F, 32.0F, 5.0F, 32.0F, new Dilation(0.7F))
                .uv(0, 40).cuboid(-13.0F, -41.0F, -13.0F, 26.0F, 4.0F, 26.0F, new Dilation(0.55F))
                .uv(0, 72).cuboid(-9.0F, -45.0F, -9.0F, 18.0F, 4.0F, 18.0F, new Dilation(0.35F))
                .uv(0, 96).cuboid(-14.0F, -31.0F, -14.0F, 28.0F, 2.0F, 28.0F, new Dilation(0.25F))
                .uv(128, 0).cuboid(-20.0F, -35.0F, -8.0F, 40.0F, 5.0F, 16.0F, new Dilation(0.15F))
                .uv(128, 24).cuboid(-8.0F, -35.0F, -20.0F, 16.0F, 5.0F, 40.0F, new Dilation(0.15F)),
            ModelTransform.NONE
        );

        ModelPartData trunk = root.addChild("trunk",
            ModelPartBuilder.create()
                .uv(160, 72).cuboid(-5.0F, -34.0F, -5.0F, 10.0F, 46.0F, 10.0F, new Dilation(0.40F))
                .uv(192, 72).cuboid(-6.0F, 1.5F, -6.0F, 12.0F, 14.0F, 12.0F, new Dilation(0.06F)),
            ModelTransform.NONE
        );

        ModelPartData rootCollar = root.addChild("root_collar",
            ModelPartBuilder.create()
                .uv(128, 128).cuboid(-4.0F, 7.0F, -4.0F, 8.0F, 9.0F, 8.0F, new Dilation(0.18F))
                .uv(128, 160).cuboid(-5.0F, 13.0F, -5.0F, 10.0F, 2.0F, 10.0F, new Dilation(0.02F))
                .uv(168, 212).cuboid(-3.0F, 11.5F, -3.0F, 6.0F, 6.0F, 6.0F, new Dilation(0.06F)),
            ModelTransform.NONE
        );

        addRootPiece(rootCollar, "front_left_buttress_root", -3.1F, 10.5F, -3.2F, 11.0F, 4.0F, -0.78F, -0.06F, 0.48F);
        addRootPiece(rootCollar, "front_right_buttress_root", 3.1F, 10.5F, -3.2F, 11.0F, 4.0F, -0.78F, 0.06F, -0.48F);
        addRootPiece(rootCollar, "left_buttress_root", -3.6F, 10.8F, 0.0F, 11.0F, 4.1F, -0.08F, 0.00F, 0.96F);
        addRootPiece(rootCollar, "right_buttress_root", 3.6F, 10.8F, 0.0F, 11.0F, 4.1F, -0.08F, 0.00F, -0.96F);
        addRootPiece(rootCollar, "back_left_buttress_root", -2.7F, 10.8F, 3.2F, 10.0F, 3.6F, 0.58F, 0.04F, 0.30F);
        addRootPiece(rootCollar, "back_right_buttress_root", 2.7F, 10.8F, 3.2F, 10.0F, 3.6F, 0.58F, -0.04F, -0.30F);
        addRootPiece(rootCollar, "front_left_bridge_root", -7.3F, 12.2F, -6.8F, 10.0F, 3.2F, -0.68F, -0.10F, 0.86F);
        addRootPiece(rootCollar, "front_right_bridge_root", 7.3F, 12.2F, -6.8F, 10.0F, 3.2F, -0.68F, 0.10F, -0.86F);
        addRootPiece(rootCollar, "rear_left_bridge_root", -7.0F, 12.6F, 5.8F, 9.0F, 3.0F, 0.38F, -0.04F, 0.82F);
        addRootPiece(rootCollar, "rear_right_bridge_root", 7.0F, 12.6F, 5.8F, 9.0F, 3.0F, 0.38F, 0.04F, -0.82F);

        addRootPiece(rootCollar, "front_outer_left_root", -12.0F, 15.2F, -8.5F, 26.0F, 3.8F, FRONT_OUTER_ROOT_BASE_PITCH, -FRONT_OUTER_ROOT_BASE_YAW, FRONT_OUTER_ROOT_BASE_ROLL);
        addRootPiece(rootCollar, "front_inner_left_root", -6.0F, 14.0F, -10.5F, 13.0F, 2.6F, -1.10F, -0.08F, 0.22F);
        addRootPiece(rootCollar, "front_inner_right_root", 6.0F, 14.0F, -10.5F, 13.0F, 2.6F, -1.10F, 0.08F, -0.22F);
        addRootPiece(rootCollar, "front_outer_right_root", 12.0F, 15.2F, -8.5F, 26.0F, 3.8F, FRONT_OUTER_ROOT_BASE_PITCH, FRONT_OUTER_ROOT_BASE_YAW, -FRONT_OUTER_ROOT_BASE_ROLL);
        addRootPiece(rootCollar, "left_front_root", -12.5F, 14.0F, -6.0F, 13.0F, 2.9F, -0.25F, 0.00F, 1.08F);
        addRootPiece(rootCollar, "left_mid_root", -13.5F, 15.7F, 0.0F, 24.0F, 3.4F, SIDE_MID_ROOT_BASE_PITCH, 0.00F, SIDE_MID_ROOT_BASE_ROLL);
        addRootPiece(rootCollar, "left_back_root", -12.5F, 14.0F, 6.5F, 12.0F, 2.9F, 0.20F, -0.08F, 0.98F);
        addRootPiece(rootCollar, "right_front_root", 12.5F, 14.0F, -6.0F, 13.0F, 2.9F, -0.25F, 0.00F, -1.08F);
        addRootPiece(rootCollar, "right_mid_root", 13.5F, 15.7F, 0.0F, 24.0F, 3.4F, SIDE_MID_ROOT_BASE_PITCH, 0.00F, -SIDE_MID_ROOT_BASE_ROLL);
        addRootPiece(rootCollar, "right_back_root", 12.5F, 14.0F, 6.5F, 12.0F, 2.9F, 0.20F, 0.08F, -0.98F);
        addRootPiece(rootCollar, "back_outer_left_root", -8.0F, 14.0F, 10.5F, 18.0F, 3.2F, BACK_OUTER_ROOT_BASE_PITCH, BACK_OUTER_ROOT_BASE_YAW, BACK_OUTER_ROOT_BASE_ROLL);
        addRootPiece(rootCollar, "back_center_root", 0.0F, 14.0F, 11.8F, 10.0F, 2.6F, 0.82F, 0.00F, 0.00F);
        addRootPiece(rootCollar, "back_outer_right_root", 8.0F, 14.0F, 10.5F, 18.0F, 3.2F, BACK_OUTER_ROOT_BASE_PITCH, -BACK_OUTER_ROOT_BASE_YAW, -BACK_OUTER_ROOT_BASE_ROLL);
        addRootPiece(rootCollar, "front_secondary_left", -10.0F, 15.5F, -11.5F, 10.0F, 2.0F, -1.18F, -0.18F, 0.60F);
        addRootPiece(rootCollar, "front_secondary_right", 10.0F, 15.5F, -11.5F, 10.0F, 2.0F, -1.18F, 0.18F, -0.60F);
        addRootPiece(rootCollar, "left_secondary_root", -14.5F, 15.5F, -1.5F, 9.0F, 1.9F, 0.02F, 0.00F, 1.32F);
        addRootPiece(rootCollar, "right_secondary_root", 14.5F, 15.5F, -1.5F, 9.0F, 1.9F, 0.02F, 0.00F, -1.32F);
        addRootPiece(rootCollar, "back_secondary_left", -6.0F, 15.5F, 12.0F, 8.0F, 1.8F, 0.86F, 0.10F, 0.12F);
        addRootPiece(rootCollar, "back_secondary_right", 6.0F, 15.5F, 12.0F, 8.0F, 1.8F, 0.86F, -0.10F, -0.12F);
        addRootPiece(rootCollar, "left_hanging_root", -4.5F, 18.0F, 0.0F, 11.0F, 1.4F, 0.12F, 0.00F, 0.55F);
        addRootPiece(rootCollar, "right_hanging_root", 4.5F, 18.0F, 0.0F, 11.0F, 1.4F, 0.12F, 0.00F, -0.55F);
        addRootPiece(rootCollar, "front_hanging_root", 0.0F, 16.8F, -3.6F, 12.0F, 1.8F, -0.68F, 0.00F, 0.00F);
        addRootPiece(rootCollar, "back_hanging_root", 0.0F, 18.0F, 4.5F, 11.0F, 1.4F, 0.55F, 0.00F, 0.00F);

        rootCollar.addChild("left_attack_root",
            ModelPartBuilder.create()
                .uv(208, 112).cuboid(-ATTACK_ROOT_HALF_WIDTH, 0.0F, -ATTACK_ROOT_HALF_WIDTH, ATTACK_ROOT_HALF_WIDTH * 2.0F, ATTACK_ROOT_LENGTH, ATTACK_ROOT_HALF_WIDTH * 2.0F, new Dilation(0.10F)),
            ModelTransform.of(-ATTACK_ROOT_ORIGIN_X, ATTACK_ROOT_ORIGIN_Y, ATTACK_ROOT_ORIGIN_Z, ATTACK_ROOT_REST_PITCH, -ATTACK_ROOT_REST_YAW, ATTACK_ROOT_REST_ROLL)
        );

        rootCollar.addChild("right_attack_root",
            ModelPartBuilder.create()
                .uv(208, 112).mirrored().cuboid(-ATTACK_ROOT_HALF_WIDTH, 0.0F, -ATTACK_ROOT_HALF_WIDTH, ATTACK_ROOT_HALF_WIDTH * 2.0F, ATTACK_ROOT_LENGTH, ATTACK_ROOT_HALF_WIDTH * 2.0F, new Dilation(0.10F)),
            ModelTransform.of(ATTACK_ROOT_ORIGIN_X, ATTACK_ROOT_ORIGIN_Y, ATTACK_ROOT_ORIGIN_Z, ATTACK_ROOT_REST_PITCH, ATTACK_ROOT_REST_YAW, -ATTACK_ROOT_REST_ROLL)
        );

        return TexturedModelData.of(modelData, 256, 256);
    }

    private static void addRootPiece(ModelPartData parent, String name, float pivotX, float pivotY, float pivotZ,
                                     float height, float width, float pitch, float yaw, float roll) {
        int textureV = width >= 5.0F ? 128 : width >= 4.0F ? 148 : 174;
        parent.addChild(name,
            ModelPartBuilder.create().uv(224, textureV)
                .cuboid(-width / 2.0F, 0.0F, -width / 2.0F, width, height, width),
            ModelTransform.of(pivotX, pivotY, pivotZ, pitch, yaw, roll)
        );
    }

    private static float smoothStepRange(float value, float start, float end) {
        float t = MathHelper.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public void setAngles(WalkingTreeEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        float movement = Math.min(limbDistance, 1.0F);
        float slowSwing = limbAngle * 0.24F;
        float idleWave = MathHelper.sin(animationProgress * 0.03F) * 0.02F;
        float attackLift = smoothStepRange(this.attackProgress, 0.0F, ATTACK_ROOT_LIFT_END);
        float attackSlam = smoothStepRange(this.attackProgress, ATTACK_ROOT_HOLD_END, ATTACK_ROOT_SLAM_END);
        float attackRecover = smoothStepRange(this.attackProgress, ATTACK_ROOT_SLAM_END, 1.0F);
        float raisedAttackPose = attackLift * (1.0F - attackSlam);
        float strikeAttackPose = attackSlam * (1.0F - attackRecover);
        float attackBodyArc = Math.max(raisedAttackPose * 0.45F, strikeAttackPose);

        this.trunk.pitch = -0.08F + MathHelper.sin(slowSwing) * 0.02F * movement - raisedAttackPose * 0.035F + strikeAttackPose * 0.055F;
        this.trunk.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.05F + idleWave;

        this.canopy.pitch = this.trunk.pitch * 0.22F + MathHelper.sin(animationProgress * 0.04F) * 0.008F;
        this.canopy.yaw = this.trunk.yaw * 0.55F;

        this.rootCollar.pitch = MathHelper.sin(slowSwing) * 0.01F * movement;
        this.rootCollar.yaw = -idleWave * 0.25F;

        float leftRestPitch = ATTACK_ROOT_REST_PITCH + MathHelper.sin(animationProgress * 0.04F) * 0.035F;
        float rightRestPitch = ATTACK_ROOT_REST_PITCH + MathHelper.sin(animationProgress * 0.04F + 0.8F) * 0.035F;
        this.leftAttackRoot.pitch = leftRestPitch + raisedAttackPose * ATTACK_ROOT_SWING_PITCH - strikeAttackPose * ATTACK_ROOT_STRIKE_PITCH;
        this.rightAttackRoot.pitch = rightRestPitch + raisedAttackPose * ATTACK_ROOT_SWING_PITCH - strikeAttackPose * ATTACK_ROOT_STRIKE_PITCH;
        this.leftAttackRoot.yaw = -ATTACK_ROOT_REST_YAW - raisedAttackPose * ATTACK_ROOT_SWING_YAW + strikeAttackPose * ATTACK_ROOT_STRIKE_YAW;
        this.rightAttackRoot.yaw = ATTACK_ROOT_REST_YAW + raisedAttackPose * ATTACK_ROOT_SWING_YAW - strikeAttackPose * ATTACK_ROOT_STRIKE_YAW;
        this.leftAttackRoot.roll = ATTACK_ROOT_REST_ROLL - raisedAttackPose * ATTACK_ROOT_SWING_ROLL + strikeAttackPose * ATTACK_ROOT_STRIKE_ROLL;
        this.rightAttackRoot.roll = -ATTACK_ROOT_REST_ROLL + raisedAttackPose * ATTACK_ROOT_SWING_ROLL - strikeAttackPose * ATTACK_ROOT_STRIKE_ROLL;

        float supportSwingA = MathHelper.cos(slowSwing) * 0.12F * movement;
        float supportSwingB = MathHelper.cos(slowSwing + MathHelper.PI) * 0.12F * movement;
        float supportRoll = MathHelper.sin(slowSwing * 0.75F) * 0.08F * movement;

        this.frontLeftSupportRoot.pitch = FRONT_OUTER_ROOT_BASE_PITCH + supportSwingA;
        this.frontRightSupportRoot.pitch = FRONT_OUTER_ROOT_BASE_PITCH + supportSwingB;
        this.frontLeftSupportRoot.yaw = -FRONT_OUTER_ROOT_BASE_YAW - supportRoll * 0.35F;
        this.frontRightSupportRoot.yaw = FRONT_OUTER_ROOT_BASE_YAW + supportRoll * 0.35F;
        this.frontLeftSupportRoot.roll = FRONT_OUTER_ROOT_BASE_ROLL + supportSwingA * 0.30F;
        this.frontRightSupportRoot.roll = -FRONT_OUTER_ROOT_BASE_ROLL - supportSwingB * 0.30F;

        this.leftMidSupportRoot.pitch = SIDE_MID_ROOT_BASE_PITCH + supportSwingA * 0.45F;
        this.rightMidSupportRoot.pitch = SIDE_MID_ROOT_BASE_PITCH + supportSwingB * 0.45F;
        this.leftMidSupportRoot.roll = SIDE_MID_ROOT_BASE_ROLL + supportSwingA * 0.20F;
        this.rightMidSupportRoot.roll = -SIDE_MID_ROOT_BASE_ROLL - supportSwingB * 0.20F;

        this.backLeftSupportRoot.pitch = BACK_OUTER_ROOT_BASE_PITCH + supportSwingB * 0.80F + attackBodyArc * 0.025F;
        this.backRightSupportRoot.pitch = BACK_OUTER_ROOT_BASE_PITCH + supportSwingA * 0.80F + attackBodyArc * 0.025F;
        this.backLeftSupportRoot.yaw = BACK_OUTER_ROOT_BASE_YAW + supportRoll * 0.25F;
        this.backRightSupportRoot.yaw = -BACK_OUTER_ROOT_BASE_YAW - supportRoll * 0.25F;
        this.backLeftSupportRoot.roll = BACK_OUTER_ROOT_BASE_ROLL + supportSwingB * 0.18F;
        this.backRightSupportRoot.roll = -BACK_OUTER_ROOT_BASE_ROLL - supportSwingA * 0.18F;
    }

    @Override
    public void animateModel(WalkingTreeEntity entity, float limbAngle, float limbDistance, float tickDelta) {
        super.animateModel(entity, limbAngle, limbDistance, tickDelta);
        this.attackProgress = entity.getAttackAnimationProgress(tickDelta);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
