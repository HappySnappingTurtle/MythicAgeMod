package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.CentaurEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class CentaurModel extends SinglePartEntityModel<CentaurEntity> {
    private final ModelPart root;
    private final ModelPart horseBody;
    private final ModelPart horseChest;
    private final ModelPart horseRump;
    private final ModelPart horseShoulderBlend;
    private final ModelPart horseBellyShadow;
    private final ModelPart tail;
    private final ModelPart tailTuft;
    private final ModelPart humanTorso;
    private final ModelPart humanWaist;
    private final ModelPart humanShoulders;
    private final ModelPart humanChestStrap;
    private final ModelPart maneNeck;
    private final ModelPart head;
    private final ModelPart noseBridge;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart mouth;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart hairCrown;
    private final ModelPart hairBack;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart leftHand;
    private final ModelPart leftFingerA;
    private final ModelPart leftFingerB;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart rightHand;
    private final ModelPart rightFingerA;
    private final ModelPart rightFingerB;
    private final ModelPart bow;
    private final ModelPart bowUpper;
    private final ModelPart bowLower;
    private final ModelPart bowGrip;
    private final ModelPart bowString;
    private final ModelPart arrow;
    private final ModelPart arrowTip;
    private final ModelPart quiver;
    private final ModelPart quiverStrap;
    private final ModelPart quiverArrowA;
    private final ModelPart quiverArrowB;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontLeftHoof;
    private final ModelPart frontRightLeg;
    private final ModelPart frontRightHoof;
    private final ModelPart backLeftLeg;
    private final ModelPart backLeftHoof;
    private final ModelPart backRightLeg;
    private final ModelPart backRightHoof;

    public CentaurModel(ModelPart root) {
        this.root = root;
        this.horseBody = root.getChild("horse_body");
        this.horseChest = this.horseBody.getChild("horse_chest");
        this.horseRump = this.horseBody.getChild("horse_rump");
        this.horseShoulderBlend = this.horseBody.getChild("horse_shoulder_blend");
        this.horseBellyShadow = this.horseBody.getChild("horse_belly_shadow");
        this.tail = this.horseBody.getChild("tail");
        this.tailTuft = this.tail.getChild("tail_tuft");
        this.humanTorso = root.getChild("human_torso");
        this.humanWaist = this.humanTorso.getChild("human_waist");
        this.humanShoulders = this.humanTorso.getChild("human_shoulders");
        this.humanChestStrap = this.humanTorso.getChild("human_chest_strap");
        this.maneNeck = this.humanTorso.getChild("mane_neck");
        this.head = this.humanTorso.getChild("head");
        this.noseBridge = this.head.getChild("nose_bridge");
        this.leftEye = this.head.getChild("left_eye");
        this.rightEye = this.head.getChild("right_eye");
        this.mouth = this.head.getChild("mouth");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.hairCrown = this.head.getChild("hair_crown");
        this.hairBack = this.head.getChild("hair_back");
        this.leftArm = this.humanTorso.getChild("left_arm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.leftHand = this.leftForearm.getChild("left_hand");
        this.leftFingerA = this.leftHand.getChild("left_finger_a");
        this.leftFingerB = this.leftHand.getChild("left_finger_b");
        this.rightArm = this.humanTorso.getChild("right_arm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.rightHand = this.rightForearm.getChild("right_hand");
        this.rightFingerA = this.rightHand.getChild("right_finger_a");
        this.rightFingerB = this.rightHand.getChild("right_finger_b");
        this.bow = this.leftHand.getChild("bow");
        this.bowUpper = this.bow.getChild("bow_upper");
        this.bowLower = this.bow.getChild("bow_lower");
        this.bowGrip = this.bow.getChild("bow_grip");
        this.bowString = this.bow.getChild("bow_string");
        this.arrow = this.bow.getChild("arrow");
        this.arrowTip = this.arrow.getChild("arrow_tip");
        this.quiver = this.humanTorso.getChild("quiver");
        this.quiverStrap = this.humanTorso.getChild("quiver_strap");
        this.quiverArrowA = this.quiver.getChild("quiver_arrow_a");
        this.quiverArrowB = this.quiver.getChild("quiver_arrow_b");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontLeftHoof = this.frontLeftLeg.getChild("front_left_hoof");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.frontRightHoof = this.frontRightLeg.getChild("front_right_hoof");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backLeftHoof = this.backLeftLeg.getChild("back_left_hoof");
        this.backRightLeg = root.getChild("back_right_leg");
        this.backRightHoof = this.backRightLeg.getChild("back_right_hoof");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData horseBody = root.addChild("horse_body",
                ModelPartBuilder.create().uv(0, 34).cuboid(-6.4F, -5.5F, -13.0F, 12.8F, 10.3F, 24.5F),
                ModelTransform.pivot(0.0F, 11.0F, 2.0F));
        horseBody.addChild("horse_chest",
                ModelPartBuilder.create().uv(78, 34).cuboid(-5.0F, -7.0F, -15.0F, 10.0F, 8.5F, 6.5F),
                ModelTransform.NONE);
        horseBody.addChild("horse_rump",
                ModelPartBuilder.create().uv(78, 52).cuboid(-5.8F, -4.8F, 7.5F, 11.6F, 9.6F, 6.0F),
                ModelTransform.NONE);
        horseBody.addChild("horse_shoulder_blend",
                ModelPartBuilder.create().uv(0, 72).cuboid(-4.6F, -8.4F, -11.5F, 9.2F, 5.2F, 6.1F),
                ModelTransform.NONE);
        horseBody.addChild("horse_belly_shadow",
                ModelPartBuilder.create().uv(36, 72).cuboid(-5.2F, 3.7F, -8.0F, 10.4F, 1.5F, 14.5F),
                ModelTransform.NONE);
        ModelPartData tail = horseBody.addChild("tail",
                ModelPartBuilder.create().uv(96, 34).cuboid(-1.2F, -0.5F, -1.0F, 2.4F, 10.0F, 3.0F),
                ModelTransform.of(0.0F, -2.3F, 11.6F, 0.72F, 0.0F, 0.0F));
        tail.addChild("tail_tuft",
                ModelPartBuilder.create().uv(108, 34).cuboid(-3.0F, 7.0F, 0.5F, 6.0F, 4.9F, 4.0F),
                ModelTransform.NONE);

        ModelPartData humanTorso = root.addChild("human_torso",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.4F, -16.5F, -3.0F, 8.8F, 12.0F, 5.4F),
                ModelTransform.pivot(0.0F, 7.0F, -7.5F));
        humanTorso.addChild("human_waist",
                ModelPartBuilder.create().uv(36, 0).cuboid(-3.2F, -8.0F, -3.3F, 6.4F, 4.6F, 5.0F),
                ModelTransform.NONE);
        humanTorso.addChild("human_shoulders",
                ModelPartBuilder.create().uv(28, 0).cuboid(-5.8F, -15.8F, -2.7F, 11.6F, 3.3F, 5.2F),
                ModelTransform.NONE);
        humanTorso.addChild("human_chest_strap",
                ModelPartBuilder.create().uv(64, 0).cuboid(-4.6F, -13.6F, -3.4F, 9.2F, 1.2F, 1.0F),
                ModelTransform.NONE);
        humanTorso.addChild("mane_neck",
                ModelPartBuilder.create().uv(68, 8).cuboid(-2.5F, -16.2F, 1.4F, 5.0F, 9.2F, 1.4F),
                ModelTransform.NONE);

        ModelPartData head = humanTorso.addChild("head",
                ModelPartBuilder.create().uv(62, 18).cuboid(-3.2F, -6.8F, -3.0F, 6.4F, 6.8F, 6.0F),
                ModelTransform.pivot(0.0F, -15.3F, -2.8F));
        head.addChild("nose_bridge",
                ModelPartBuilder.create().uv(90, 0).cuboid(-1.5F, -3.2F, -4.0F, 3.0F, 2.5F, 1.4F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(104, 0).cuboid(-2.4F, -4.2F, -3.7F, 1.15F, 1.2F, 0.95F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(110, 0).cuboid(1.25F, -4.2F, -3.7F, 1.15F, 1.2F, 0.95F),
                ModelTransform.NONE);
        head.addChild("mouth",
                ModelPartBuilder.create().uv(104, 6).cuboid(-2.0F, -1.3F, -3.9F, 4.0F, 0.9F, 0.9F),
                ModelTransform.NONE);
        head.addChild("left_ear",
                ModelPartBuilder.create().uv(116, 0).cuboid(2.9F, -5.7F, -0.7F, 1.3F, 3.1F, 2.7F),
                ModelTransform.NONE);
        head.addChild("right_ear",
                ModelPartBuilder.create().uv(116, 8).mirrored().cuboid(-4.2F, -5.7F, -0.7F, 1.3F, 3.1F, 2.7F),
                ModelTransform.NONE);
        head.addChild("hair_crown",
                ModelPartBuilder.create().uv(90, 12).cuboid(-3.9F, -8.1F, -0.6F, 7.8F, 2.1F, 3.4F),
                ModelTransform.NONE);
        head.addChild("hair_back",
                ModelPartBuilder.create().uv(90, 22).cuboid(-4.0F, -6.7F, 1.9F, 8.0F, 6.8F, 1.4F),
                ModelTransform.NONE);

        ModelPartData leftArm = humanTorso.addChild("left_arm",
                ModelPartBuilder.create().uv(0, 82).cuboid(-0.4F, -1.0F, -1.9F, 3.4F, 9.4F, 3.7F),
                ModelTransform.of(5.2F, -13.8F, -1.4F, -0.58F, 0.0F, -0.18F));
        ModelPartData leftForearm = leftArm.addChild("left_forearm",
                ModelPartBuilder.create().uv(18, 82).cuboid(-1.1F, -0.2F, -1.9F, 2.7F, 9.5F, 3.8F),
                ModelTransform.of(1.4F, 6.9F, -3.1F, -0.38F, 0.0F, 0.08F));
        ModelPartData leftHand = leftForearm.addChild("left_hand",
                ModelPartBuilder.create().uv(36, 82).cuboid(-1.3F, 8.2F, -2.0F, 3.1F, 3.1F, 3.5F),
                ModelTransform.NONE);
        leftHand.addChild("left_finger_a",
                ModelPartBuilder.create().uv(104, 82).cuboid(-1.2F, 10.8F, -2.4F, 1.0F, 1.8F, 1.5F),
                ModelTransform.NONE);
        leftHand.addChild("left_finger_b",
                ModelPartBuilder.create().uv(110, 82).cuboid(0.3F, 10.8F, -2.3F, 1.0F, 1.8F, 1.5F),
                ModelTransform.NONE);
        ModelPartData bow = leftHand.addChild("bow", ModelPartBuilder.create(), ModelTransform.NONE);
        bow.addChild("bow_upper",
                ModelPartBuilder.create().uv(100, 32).cuboid(1.9F, -19.4F, -5.2F, 1.4F, 12.0F, 1.5F),
                ModelTransform.NONE);
        bow.addChild("bow_lower",
                ModelPartBuilder.create().uv(108, 32).cuboid(1.9F, -7.4F, -5.2F, 1.4F, 12.8F, 1.5F),
                ModelTransform.NONE);
        bow.addChild("bow_grip",
                ModelPartBuilder.create().uv(116, 32).cuboid(1.3F, -8.6F, -5.0F, 2.4F, 2.4F, 1.6F),
                ModelTransform.NONE);
        bow.addChild("bow_string",
                ModelPartBuilder.create().uv(100, 50).cuboid(3.3F, -19.1F, -4.4F, 0.6F, 24.2F, 0.4F),
                ModelTransform.NONE);
        ModelPartData arrow = bow.addChild("arrow",
                ModelPartBuilder.create().uv(108, 50).cuboid(-3.7F, -8.5F, -5.9F, 7.7F, 1.1F, 0.7F),
                ModelTransform.NONE);
        arrow.addChild("arrow_tip",
                ModelPartBuilder.create().uv(108, 56).cuboid(3.8F, -8.8F, -6.2F, 1.2F, 1.7F, 1.5F),
                ModelTransform.NONE);

        ModelPartData rightArm = humanTorso.addChild("right_arm",
                ModelPartBuilder.create().uv(50, 82).cuboid(-3.0F, -1.0F, -1.9F, 3.4F, 9.4F, 3.7F),
                ModelTransform.of(-5.2F, -13.8F, -1.4F, -0.52F, 0.0F, 0.18F));
        ModelPartData rightForearm = rightArm.addChild("right_forearm",
                ModelPartBuilder.create().uv(68, 82).cuboid(-1.6F, -0.2F, -1.8F, 2.7F, 9.6F, 3.6F),
                ModelTransform.of(-1.4F, 6.8F, -2.9F, -0.45F, 0.0F, -0.08F));
        ModelPartData rightHand = rightForearm.addChild("right_hand",
                ModelPartBuilder.create().uv(86, 82).cuboid(-1.8F, 8.2F, -1.9F, 3.1F, 3.1F, 3.4F),
                ModelTransform.NONE);
        rightHand.addChild("right_finger_a",
                ModelPartBuilder.create().uv(116, 82).cuboid(-1.3F, 10.8F, -2.2F, 1.0F, 1.8F, 1.5F),
                ModelTransform.NONE);
        rightHand.addChild("right_finger_b",
                ModelPartBuilder.create().uv(122, 82).cuboid(0.2F, 10.8F, -2.1F, 1.0F, 1.8F, 1.5F),
                ModelTransform.NONE);

        ModelPartData quiver = humanTorso.addChild("quiver",
                ModelPartBuilder.create().uv(100, 60).cuboid(-1.5F, -4.5F, 0.0F, 3.0F, 9.0F, 2.3F),
                ModelTransform.of(-1.5F, -9.0F, 2.4F, 0.18F, 0.0F, -0.18F));
        humanTorso.addChild("quiver_strap",
                ModelPartBuilder.create().uv(96, 74).cuboid(-4.6F, -12.0F, 2.6F, 9.0F, 1.2F, 1.0F),
                ModelTransform.NONE);
        quiver.addChild("quiver_arrow_a",
                ModelPartBuilder.create().uv(118, 60).cuboid(-1.1F, -7.5F, 0.3F, 1.0F, 3.5F, 1.3F),
                ModelTransform.NONE);
        quiver.addChild("quiver_arrow_b",
                ModelPartBuilder.create().uv(120, 66).cuboid(0.3F, -7.8F, 0.3F, 1.0F, 3.8F, 1.3F),
                ModelTransform.NONE);

        addHorseLeg(root, "front_left_leg", "front_left_hoof", 3.4F, 12.0F, -6.8F, 0, 96, 16, 96);
        addHorseLeg(root, "front_right_leg", "front_right_hoof", -3.4F, 12.0F, -6.8F, 34, 96, 50, 96);
        addHorseLeg(root, "back_left_leg", "back_left_hoof", 3.6F, 12.0F, 8.8F, 68, 96, 84, 96);
        addHorseLeg(root, "back_right_leg", "back_right_hoof", -3.6F, 12.0F, 8.8F, 102, 96, 0, 114);

        return TexturedModelData.of(modelData, 128, 128);
    }

    private static void addHorseLeg(ModelPartData root, String legName, String hoofName, float x, float y, float z, int legU, int legV, int hoofU, int hoofV) {
        ModelPartData leg = root.addChild(legName,
                ModelPartBuilder.create().uv(legU, legV).cuboid(-1.45F, 0.0F, -1.65F, 2.9F, 10.7F, 3.5F),
                ModelTransform.pivot(x, y, z));
        leg.addChild(hoofName,
                ModelPartBuilder.create().uv(hoofU, hoofV).cuboid(-1.9F, 9.5F, -2.5F, 3.8F, 2.6F, 4.7F),
                ModelTransform.NONE);
    }

    @Override
    public void setAngles(CentaurEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        resetFaceDetails();

        float gait = 1.15F * limbDistance;
        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.78F + MathHelper.PI) * gait;
        this.backRightLeg.pitch = this.frontLeftLeg.pitch;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.78F) * gait;
        this.backLeftLeg.pitch = this.frontRightLeg.pitch;
        resetHooves();

        this.horseBody.pitch = MathHelper.sin(limbAngle * 0.39F) * 0.03F * limbDistance;
        this.horseChest.pitch = 0.0F;
        this.horseRump.pitch = 0.0F;
        this.horseShoulderBlend.pitch = 0.0F;
        this.horseBellyShadow.pitch = 0.0F;
        this.tail.pitch = 0.7F + MathHelper.sin(animationProgress * 0.15F) * 0.12F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.12F) * 0.1F;
        this.tailTuft.pitch = MathHelper.sin(animationProgress * 0.18F) * 0.08F;

        this.humanTorso.pitch = -0.04F * limbDistance;
        this.humanTorso.yaw = MathHelper.sin(limbAngle * 0.25F) * 0.04F * limbDistance;
        this.humanWaist.pitch = 0.0F;
        this.humanShoulders.pitch = 0.0F;
        this.humanChestStrap.visible = true;
        this.maneNeck.pitch = MathHelper.sin(animationProgress * 0.1F) * 0.025F;
        this.quiver.visible = true;
        this.quiverStrap.visible = true;
        this.quiverArrowA.visible = true;
        this.quiverArrowB.visible = true;

        resetArms();
        float draw = entity.handSwingProgress > 0.0F ? MathHelper.sin(entity.handSwingProgress * MathHelper.PI) : 0.0F;
        this.leftArm.pitch = -0.9F + draw * 0.35F;
        this.leftArm.yaw = -0.12F - draw * 0.3F;
        this.leftForearm.pitch = -0.38F - draw * 0.2F;
        this.rightArm.pitch = -0.75F - 0.65F * draw;
        this.rightArm.yaw = 0.12F + 0.55F * draw;
        this.rightForearm.pitch = -0.45F - 0.45F * draw;
        this.rightHand.yaw = -0.22F * draw;
        this.bowString.pitch = -0.12F * draw;
        this.arrow.visible = true;
        this.arrowTip.visible = this.arrow.visible;

        if (entity.hurtTime > 0) {
            this.humanTorso.roll = MathHelper.sin(animationProgress * 2.4F) * 0.07F;
            this.head.roll = this.humanTorso.roll * 0.7F;
        } else {
            this.humanTorso.roll = 0.0F;
            this.head.roll = 0.0F;
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.horseBody.roll = 1.12F * fall;
            this.humanTorso.pitch = 0.7F * fall;
            this.head.pitch += 0.45F * fall;
        } else {
            this.horseBody.roll = 0.0F;
        }
    }

    private void resetFaceDetails() {
        this.noseBridge.pitch = 0.0F;
        this.leftEye.pitch = 0.0F;
        this.rightEye.pitch = 0.0F;
        this.mouth.pitch = 0.0F;
        this.leftEar.pitch = MathHelper.sin(this.head.pitch) * 0.02F;
        this.rightEar.pitch = -this.leftEar.pitch;
        this.hairCrown.pitch = 0.0F;
        this.hairBack.pitch = 0.0F;
    }

    private void resetHooves() {
        this.frontLeftHoof.pitch = 0.0F;
        this.frontRightHoof.pitch = 0.0F;
        this.backLeftHoof.pitch = 0.0F;
        this.backRightHoof.pitch = 0.0F;
    }

    private void resetArms() {
        this.leftArm.roll = -0.18F;
        this.leftArm.yaw = 0.0F;
        this.leftForearm.pitch = -0.38F;
        this.leftForearm.yaw = 0.0F;
        this.leftHand.pitch = 0.0F;
        this.leftFingerA.pitch = 0.0F;
        this.leftFingerB.pitch = 0.0F;
        this.rightArm.roll = 0.18F;
        this.rightArm.yaw = 0.0F;
        this.rightForearm.pitch = -0.45F;
        this.rightForearm.yaw = 0.0F;
        this.rightHand.pitch = 0.0F;
        this.rightHand.yaw = 0.0F;
        this.rightFingerA.pitch = 0.0F;
        this.rightFingerB.pitch = 0.0F;
        this.bow.visible = true;
        this.bowUpper.pitch = 0.0F;
        this.bowLower.pitch = 0.0F;
        this.bowGrip.pitch = 0.0F;
        this.bowString.visible = true;
        this.bowString.pitch = 0.0F;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
