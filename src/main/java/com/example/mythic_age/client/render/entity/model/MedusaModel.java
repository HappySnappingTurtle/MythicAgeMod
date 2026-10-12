package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.MedusaEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class MedusaModel extends SinglePartEntityModel<MedusaEntity> {
    private static final String[] SNAKE_NAMES = {
            "front_snake", "left_snake", "right_snake", "back_snake", "left_outer_snake", "right_outer_snake"
    };
    private static final float[] SNAKE_PHASES = {0.0F, 0.7F, 1.4F, 2.1F, 2.8F, 3.5F};

    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart robeFront;
    private final ModelPart waistSash;
    private final ModelPart leftShoulder;
    private final ModelPart rightShoulder;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart leftHand;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart rightHand;
    private final ModelPart head;
    private final ModelPart facePlate;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftPupil;
    private final ModelPart rightPupil;
    private final ModelPart stoneGazeAura;
    private final ModelPart mouth;
    private final ModelPart noseBridge;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart snakeHair;
    private final ModelPart hairCrown;
    private final ModelPart hairBack;
    private final ModelPart[] snakeBodies = new ModelPart[SNAKE_NAMES.length];
    private final ModelPart[] snakeHeads = new ModelPart[SNAKE_NAMES.length];
    private final ModelPart[] snakeLeftEyes = new ModelPart[SNAKE_NAMES.length];
    private final ModelPart[] snakeRightEyes = new ModelPart[SNAKE_NAMES.length];
    private final ModelPart[] snakeLeftFangs = new ModelPart[SNAKE_NAMES.length];
    private final ModelPart[] snakeRightFangs = new ModelPart[SNAKE_NAMES.length];
    private final ModelPart[] snakeTongues = new ModelPart[SNAKE_NAMES.length];
    private final ModelPart coiledTail;
    private final ModelPart tailBase;
    private final ModelPart tailMid;
    private final ModelPart tailCoilOuter;
    private final ModelPart tailCoilInner;
    private final ModelPart tailTip;
    private final ModelPart tailTipSpike;

    public MedusaModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.robeFront = this.torso.getChild("robe_front");
        this.waistSash = this.torso.getChild("waist_sash");
        this.leftShoulder = this.torso.getChild("left_shoulder");
        this.rightShoulder = this.torso.getChild("right_shoulder");
        this.leftArm = this.torso.getChild("left_arm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.leftHand = this.leftForearm.getChild("left_hand");
        this.rightArm = this.torso.getChild("right_arm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.rightHand = this.rightForearm.getChild("right_hand");
        this.head = root.getChild("head");
        this.facePlate = this.head.getChild("face_plate");
        this.leftEye = this.head.getChild("left_eye");
        this.rightEye = this.head.getChild("right_eye");
        this.leftPupil = this.leftEye.getChild("left_pupil");
        this.rightPupil = this.rightEye.getChild("right_pupil");
        this.stoneGazeAura = this.head.getChild("stone_gaze_aura");
        this.mouth = this.head.getChild("mouth");
        this.noseBridge = this.head.getChild("nose_bridge");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.snakeHair = this.head.getChild("snake_hair");
        this.hairCrown = this.snakeHair.getChild("hair_crown");
        this.hairBack = this.snakeHair.getChild("hair_back");
        for (int i = 0; i < SNAKE_NAMES.length; i++) {
            String snakeName = SNAKE_NAMES[i];
            this.snakeBodies[i] = this.snakeHair.getChild(snakeName + "_body");
            this.snakeHeads[i] = this.snakeBodies[i].getChild(snakeName + "_head");
            this.snakeLeftEyes[i] = this.snakeHeads[i].getChild(snakeName + "_left_eye");
            this.snakeRightEyes[i] = this.snakeHeads[i].getChild(snakeName + "_right_eye");
            this.snakeLeftFangs[i] = this.snakeHeads[i].getChild(snakeName + "_left_fang");
            this.snakeRightFangs[i] = this.snakeHeads[i].getChild(snakeName + "_right_fang");
            this.snakeTongues[i] = this.snakeHeads[i].getChild(snakeName + "_tongue");
        }
        this.coiledTail = root.getChild("coiled_tail");
        this.tailBase = this.coiledTail.getChild("tail_base");
        this.tailMid = this.coiledTail.getChild("tail_mid");
        this.tailCoilOuter = this.coiledTail.getChild("tail_coil_outer");
        this.tailCoilInner = this.tailCoilOuter.getChild("tail_coil_inner");
        this.tailTip = this.coiledTail.getChild("tail_tip");
        this.tailTipSpike = this.tailTip.getChild("tail_tip_spike");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData torso = root.addChild("torso",
                ModelPartBuilder.create().uv(0, 18).cuboid(-4.2F, -11.4F, -2.6F, 8.4F, 11.4F, 5.2F),
                ModelTransform.pivot(0.0F, 14.4F, 0.0F));
        torso.addChild("robe_front",
                ModelPartBuilder.create().uv(28, 18).cuboid(-4.8F, -10.4F, -3.2F, 9.6F, 7.0F, 1.1F),
                ModelTransform.NONE);
        torso.addChild("waist_sash",
                ModelPartBuilder.create().uv(54, 18).cuboid(-4.5F, -3.4F, -3.2F, 9.0F, 2.0F, 1.1F),
                ModelTransform.NONE);
        torso.addChild("left_shoulder",
                ModelPartBuilder.create().uv(72, 18).cuboid(3.7F, -10.4F, -2.4F, 1.7F, 2.2F, 4.8F),
                ModelTransform.NONE);
        torso.addChild("right_shoulder",
                ModelPartBuilder.create().uv(82, 18).mirrored().cuboid(-5.4F, -10.4F, -2.4F, 1.7F, 2.2F, 4.8F),
                ModelTransform.NONE);

        ModelPartData leftArm = torso.addChild("left_arm",
                ModelPartBuilder.create().uv(48, 28).cuboid(-0.4F, -1.0F, -1.7F, 2.1F, 10.0F, 3.4F),
                ModelTransform.of(4.8F, -9.2F, 0.0F, -0.08F, 0.0F, -0.08F));
        ModelPartData leftForearm = leftArm.addChild("left_forearm",
                ModelPartBuilder.create().uv(60, 28).cuboid(-0.6F, 8.0F, -1.5F, 2.2F, 7.0F, 3.0F),
                ModelTransform.NONE);
        leftForearm.addChild("left_hand",
                ModelPartBuilder.create().uv(72, 28).cuboid(-0.8F, 14.0F, -1.6F, 2.5F, 2.4F, 3.2F),
                ModelTransform.NONE);

        ModelPartData rightArm = torso.addChild("right_arm",
                ModelPartBuilder.create().uv(84, 28).mirrored().cuboid(-1.7F, -1.0F, -1.7F, 2.1F, 10.0F, 3.4F),
                ModelTransform.of(-4.8F, -9.2F, 0.0F, -0.08F, 0.0F, 0.08F));
        ModelPartData rightForearm = rightArm.addChild("right_forearm",
                ModelPartBuilder.create().uv(96, 28).mirrored().cuboid(-1.6F, 8.0F, -1.5F, 2.2F, 7.0F, 3.0F),
                ModelTransform.NONE);
        rightForearm.addChild("right_hand",
                ModelPartBuilder.create().uv(108, 28).mirrored().cuboid(-1.7F, 14.0F, -1.6F, 2.5F, 2.4F, 3.2F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-3.05F, -7.1F, -3.25F, 6.1F, 7.3F, 6.5F),
                ModelTransform.pivot(0.0F, 3.4F, 0.0F));
        head.addChild("face_plate",
                ModelPartBuilder.create().uv(30, 0).cuboid(-2.65F, -6.0F, -3.75F, 5.3F, 5.0F, 0.7F),
                ModelTransform.NONE);
        ModelPartData leftEye = head.addChild("left_eye",
                ModelPartBuilder.create().uv(104, 0).cuboid(-2.05F, -4.85F, -4.05F, 1.1F, 1.2F, 0.9F),
                ModelTransform.NONE);
        leftEye.addChild("left_pupil",
                ModelPartBuilder.create().uv(118, 0).cuboid(-1.7F, -4.5F, -4.3F, 0.42F, 0.52F, 0.35F),
                ModelTransform.NONE);
        ModelPartData rightEye = head.addChild("right_eye",
                ModelPartBuilder.create().uv(110, 0).cuboid(0.95F, -4.85F, -4.05F, 1.1F, 1.2F, 0.9F),
                ModelTransform.NONE);
        rightEye.addChild("right_pupil",
                ModelPartBuilder.create().uv(122, 0).cuboid(1.28F, -4.5F, -4.3F, 0.42F, 0.52F, 0.35F),
                ModelTransform.NONE);
        head.addChild("stone_gaze_aura",
                ModelPartBuilder.create().uv(104, 12).cuboid(-2.65F, -5.15F, -4.35F, 5.3F, 1.85F, 0.25F),
                ModelTransform.NONE);
        head.addChild("mouth",
                ModelPartBuilder.create().uv(104, 6).cuboid(-1.55F, -2.1F, -4.05F, 3.1F, 0.85F, 0.9F),
                ModelTransform.NONE);
        head.addChild("nose_bridge",
                ModelPartBuilder.create().uv(116, 4).cuboid(-0.5F, -3.6F, -3.95F, 1.0F, 1.6F, 0.85F),
                ModelTransform.NONE);
        head.addChild("left_ear",
                ModelPartBuilder.create().uv(118, 8).cuboid(2.35F, -5.95F, -0.65F, 1.1F, 2.7F, 2.15F),
                ModelTransform.NONE);
        head.addChild("right_ear",
                ModelPartBuilder.create().uv(116, 15).mirrored().cuboid(-3.45F, -5.95F, -0.65F, 1.1F, 2.7F, 2.15F),
                ModelTransform.NONE);

        ModelPartData snakeHair = head.addChild("snake_hair", ModelPartBuilder.create(), ModelTransform.NONE);
        snakeHair.addChild("hair_crown",
                ModelPartBuilder.create().uv(28, 0).cuboid(-4.6F, -9.0F, -3.4F, 9.2F, 2.9F, 6.8F),
                ModelTransform.NONE);
        snakeHair.addChild("hair_back",
                ModelPartBuilder.create().uv(62, 0).cuboid(-4.0F, -7.3F, 2.8F, 8.0F, 8.1F, 1.4F),
                ModelTransform.NONE);
        addSnakeHair(snakeHair, "front_snake", 0.0F, -8.8F, -4.6F, -0.12F, 0.0F, 0.0F, 72, 0, 100, 10, true);
        addSnakeHair(snakeHair, "left_snake", -5.2F, -8.2F, -0.7F, -0.08F, 0.0F, 0.16F, 56, 0, 80, 0, true);
        addSnakeHair(snakeHair, "right_snake", 5.2F, -8.2F, -0.7F, -0.08F, 0.0F, -0.16F, 64, 0, 90, 0, true);
        addSnakeHair(snakeHair, "back_snake", 0.0F, -8.4F, 4.2F, -0.02F, 0.0F, 0.0F, 72, 12, 110, 10, false);
        addSnakeHair(snakeHair, "left_outer_snake", -7.2F, -7.6F, -0.3F, -0.04F, 0.0F, 0.26F, 56, 12, 82, 12, true);
        addSnakeHair(snakeHair, "right_outer_snake", 7.2F, -7.6F, -0.3F, -0.04F, 0.0F, -0.26F, 64, 12, 92, 12, true);

        ModelPartData coiledTail = root.addChild("coiled_tail", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 3.0F, 0.0F));
        coiledTail.addChild("tail_base",
                ModelPartBuilder.create().uv(0, 40).cuboid(-3.7F, 0.0F, -3.2F, 7.4F, 8.5F, 6.4F),
                ModelTransform.NONE);
        coiledTail.addChild("tail_mid",
                ModelPartBuilder.create().uv(32, 40).cuboid(-2.9F, 7.0F, -2.6F, 5.8F, 7.0F, 5.2F),
                ModelTransform.NONE);
        ModelPartData tailCoilOuter = coiledTail.addChild("tail_coil_outer",
                ModelPartBuilder.create().uv(56, 40).cuboid(-5.1F, 12.5F, -2.3F, 10.2F, 3.7F, 4.6F),
                ModelTransform.NONE);
        tailCoilOuter.addChild("tail_coil_inner",
                ModelPartBuilder.create().uv(90, 40).cuboid(-2.8F, 14.8F, -1.8F, 5.6F, 2.9F, 3.6F),
                ModelTransform.NONE);
        ModelPartData tailTip = coiledTail.addChild("tail_tip",
                ModelPartBuilder.create().uv(0, 56).cuboid(-1.5F, 14.8F, -3.2F, 3.0F, 8.4F, 3.0F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, 0.36F, 0.0F, 0.0F));
        tailTip.addChild("tail_tip_spike",
                ModelPartBuilder.create().uv(16, 56).cuboid(-0.8F, 22.5F, -3.6F, 1.6F, 2.5F, 1.6F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    private static void addSnakeHair(ModelPartData parent, String name, float x, float y, float z, float pitch, float yaw, float roll, int bodyU, int bodyV, int headU, int headV, boolean frontFacing) {
        ModelPartData body = parent.addChild(name + "_body",
                ModelPartBuilder.create().uv(bodyU, bodyV).cuboid(-0.95F, -1.0F, -0.95F, 1.9F, 9.2F, 1.9F),
                ModelTransform.of(x, y, z, pitch, yaw, roll));
        ModelPartData head = body.addChild(name + "_head",
                ModelPartBuilder.create().uv(headU, headV).cuboid(-1.35F, 7.2F, -1.25F, 2.7F, 1.9F, 2.5F),
                ModelTransform.NONE);
        float eyeZ = frontFacing ? -1.45F : 1.05F;
        float detailZ = frontFacing ? -1.75F : 1.35F;
        head.addChild(name + "_left_eye",
                ModelPartBuilder.create().uv(82, 24).cuboid(-0.9F, 7.75F, eyeZ, 0.5F, 0.5F, 0.4F),
                ModelTransform.NONE);
        head.addChild(name + "_right_eye",
                ModelPartBuilder.create().uv(88, 24).cuboid(0.4F, 7.75F, eyeZ, 0.5F, 0.5F, 0.4F),
                ModelTransform.NONE);
        head.addChild(name + "_left_fang",
                ModelPartBuilder.create().uv(94, 24).cuboid(-0.8F, 8.45F, eyeZ, 0.6F, 0.8F, 0.4F),
                ModelTransform.NONE);
        head.addChild(name + "_right_fang",
                ModelPartBuilder.create().uv(94, 24).cuboid(0.2F, 8.45F, eyeZ, 0.6F, 0.8F, 0.4F),
                ModelTransform.NONE);
        head.addChild(name + "_tongue",
                ModelPartBuilder.create().uv(100, 24).cuboid(-0.25F, 8.9F, detailZ, 0.5F, 1.1F, 0.4F),
                ModelTransform.NONE);
    }

    @Override
    public void setAngles(MedusaEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        resetFace();

        this.torso.yaw = MathHelper.sin(limbAngle * 0.45F) * 0.08F * limbDistance;
        this.robeFront.pitch = 0.0F;
        this.waistSash.pitch = 0.0F;
        this.leftShoulder.pitch = 0.0F;
        this.rightShoulder.pitch = 0.0F;
        this.coiledTail.yaw = MathHelper.sin(animationProgress * 0.18F) * 0.18F + MathHelper.sin(limbAngle * 0.55F) * 0.25F * limbDistance;
        this.coiledTail.pitch = MathHelper.sin(limbAngle * 0.35F) * 0.06F * limbDistance;
        this.tailBase.pitch = 0.0F;
        this.tailMid.pitch = MathHelper.sin(animationProgress * 0.15F) * 0.035F;
        this.tailCoilOuter.yaw = MathHelper.sin(animationProgress * 0.16F) * 0.08F;
        this.tailCoilInner.yaw = -this.tailCoilOuter.yaw * 0.7F;
        this.tailTip.yaw = MathHelper.sin(animationProgress * 0.22F) * 0.18F;
        this.tailTip.pitch = 0.38F + MathHelper.sin(animationProgress * 0.13F) * 0.05F;
        this.tailTipSpike.pitch = 0.0F;

        this.snakeHair.pitch = MathHelper.sin(animationProgress * 0.16F) * 0.06F;
        this.snakeHair.yaw = MathHelper.cos(animationProgress * 0.13F) * 0.08F;
        this.hairCrown.pitch = 0.0F;
        this.hairBack.pitch = MathHelper.sin(animationProgress * 0.11F) * 0.03F;
        for (int i = 0; i < SNAKE_NAMES.length; i++) {
            float phase = SNAKE_PHASES[i];
            this.snakeBodies[i].roll += MathHelper.sin(animationProgress * 0.18F + phase) * 0.06F;
            this.snakeBodies[i].pitch += MathHelper.cos(animationProgress * 0.16F + phase) * 0.035F;
            this.snakeHeads[i].pitch = MathHelper.sin(animationProgress * 0.22F + phase) * 0.08F;
            this.snakeHeads[i].yaw = MathHelper.cos(animationProgress * 0.2F + phase) * 0.05F;
            this.snakeLeftEyes[i].visible = true;
            this.snakeRightEyes[i].visible = true;
            this.snakeLeftFangs[i].visible = true;
            this.snakeRightFangs[i].visible = true;
            this.snakeTongues[i].visible = true;
        }

        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 0.8F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 0.8F * limbDistance;
        this.rightArm.roll = 0.08F;
        this.leftArm.roll = -0.08F;
        this.rightForearm.pitch = 0.0F;
        this.leftForearm.pitch = 0.0F;
        this.rightHand.pitch = 0.0F;
        this.leftHand.pitch = 0.0F;

        if (entity.handSwingProgress > 0.0F) {
            float cast = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -1.4F + cast * 0.35F;
            this.leftArm.pitch = -1.35F + cast * 0.3F;
            this.head.pitch -= cast * 0.2F;
            this.stoneGazeAura.visible = true;
            this.stoneGazeAura.pitch = -0.08F * cast;
            this.snakeHair.pitch -= cast * 0.25F;
            this.snakeBodies[0].pitch -= cast * 0.12F;
            this.snakeHeads[0].pitch -= cast * 0.12F;
            this.leftPupil.visible = true;
            this.rightPupil.visible = true;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 2.8F) * 0.08F;
        } else {
            this.head.roll = 0.0F;
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.torso.pitch = 0.75F * fall;
            this.coiledTail.pitch = 0.45F * fall;
            this.head.pitch += 0.45F * fall;
        } else {
            this.torso.pitch = 0.0F;
        }
    }

    private void resetFace() {
        this.facePlate.pitch = 0.0F;
        this.leftEye.pitch = 0.0F;
        this.rightEye.pitch = 0.0F;
        this.leftPupil.pitch = 0.0F;
        this.rightPupil.pitch = 0.0F;
        this.stoneGazeAura.visible = false;
        this.stoneGazeAura.pitch = 0.0F;
        this.mouth.pitch = 0.0F;
        this.noseBridge.pitch = 0.0F;
        this.leftEar.pitch = 0.0F;
        this.rightEar.pitch = 0.0F;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
