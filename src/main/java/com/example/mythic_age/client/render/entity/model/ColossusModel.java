package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.ColossusEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class ColossusModel extends SinglePartEntityModel<ColossusEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart faceMask;
    private final ModelPart eyeSlit;
    private final ModelPart mouthGroove;
    private final ModelPart leftCheekPlate;
    private final ModelPart rightCheekPlate;
    private final ModelPart crownBand;
    private final ModelPart crownCenterSpike;
    private final ModelPart crownSpikeLeft;
    private final ModelPart crownSpikeRight;
    private final ModelPart body;
    private final ModelPart torsoCore;
    private final ModelPart chestPlateUpper;
    private final ModelPart chestPlateLower;
    private final ModelPart chestCenterGem;
    private final ModelPart leftChestSeam;
    private final ModelPart rightChestSeam;
    private final ModelPart backPlate;
    private final ModelPart bronzeBelt;
    private final ModelPart leftShoulderPlate;
    private final ModelPart rightShoulderPlate;
    private final ModelPart leftArm;
    private final ModelPart leftElbowRing;
    private final ModelPart leftForearm;
    private final ModelPart leftFist;
    private final ModelPart leftFingerA;
    private final ModelPart leftFingerB;
    private final ModelPart rightArm;
    private final ModelPart rightElbowRing;
    private final ModelPart rightForearm;
    private final ModelPart rightFist;
    private final ModelPart rightFingerA;
    private final ModelPart rightFingerB;
    private final ModelPart leftLeg;
    private final ModelPart leftKneeRing;
    private final ModelPart leftShinPlate;
    private final ModelPart leftFoot;
    private final ModelPart leftToePlate;
    private final ModelPart rightLeg;
    private final ModelPart rightKneeRing;
    private final ModelPart rightShinPlate;
    private final ModelPart rightFoot;
    private final ModelPart rightToePlate;
    private final ModelPart sword;

    public ColossusModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.faceMask = this.head.getChild("face_mask");
        this.eyeSlit = this.head.getChild("eye_slit");
        this.mouthGroove = this.head.getChild("mouth_groove");
        this.leftCheekPlate = this.head.getChild("left_cheek_plate");
        this.rightCheekPlate = this.head.getChild("right_cheek_plate");
        this.crownBand = this.head.getChild("crown_band");
        this.crownCenterSpike = this.head.getChild("crown_center_spike");
        this.crownSpikeLeft = this.head.getChild("crown_spike_left");
        this.crownSpikeRight = this.head.getChild("crown_spike_right");
        this.body = root.getChild("body");
        this.torsoCore = this.body.getChild("torso_core");
        this.chestPlateUpper = this.body.getChild("chest_plate_upper");
        this.chestPlateLower = this.body.getChild("chest_plate_lower");
        this.chestCenterGem = this.body.getChild("chest_center_gem");
        this.leftChestSeam = this.body.getChild("left_chest_seam");
        this.rightChestSeam = this.body.getChild("right_chest_seam");
        this.backPlate = this.body.getChild("back_plate");
        this.bronzeBelt = this.body.getChild("bronze_belt");
        this.leftShoulderPlate = this.body.getChild("left_shoulder_plate");
        this.rightShoulderPlate = this.body.getChild("right_shoulder_plate");
        this.leftArm = root.getChild("left_arm");
        this.leftElbowRing = this.leftArm.getChild("left_elbow_ring");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.leftFist = this.leftForearm.getChild("left_fist");
        this.leftFingerA = this.leftFist.getChild("left_finger_a");
        this.leftFingerB = this.leftFist.getChild("left_finger_b");
        this.rightArm = root.getChild("right_arm");
        this.rightElbowRing = this.rightArm.getChild("right_elbow_ring");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.rightFist = this.rightForearm.getChild("right_fist");
        this.rightFingerA = this.rightFist.getChild("right_finger_a");
        this.rightFingerB = this.rightFist.getChild("right_finger_b");
        this.leftLeg = root.getChild("left_leg");
        this.leftKneeRing = this.leftLeg.getChild("left_knee_ring");
        this.leftShinPlate = this.leftLeg.getChild("left_shin_plate");
        this.leftFoot = this.leftLeg.getChild("left_foot");
        this.leftToePlate = this.leftFoot.getChild("left_toe_plate");
        this.rightLeg = root.getChild("right_leg");
        this.rightKneeRing = this.rightLeg.getChild("right_knee_ring");
        this.rightShinPlate = this.rightLeg.getChild("right_shin_plate");
        this.rightFoot = this.rightLeg.getChild("right_foot");
        this.rightToePlate = this.rightFoot.getChild("right_toe_plate");
        this.sword = this.rightFist.getChild("sword");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-5.2F, -10.0F, -5.2F, 10.4F, 10.0F, 10.4F),
                ModelTransform.pivot(0.0F, -20.0F, 0.0F));
        head.addChild("face_mask",
                ModelPartBuilder.create().uv(40, 0).cuboid(-4.1F, -8.2F, -5.9F, 8.2F, 7.2F, 1.1F),
                ModelTransform.NONE);
        head.addChild("eye_slit",
                ModelPartBuilder.create().uv(60, 0).cuboid(-3.2F, -5.5F, -6.4F, 6.4F, 1.8F, 0.8F),
                ModelTransform.NONE);
        head.addChild("mouth_groove",
                ModelPartBuilder.create().uv(72, 0).cuboid(-2.5F, -2.2F, -6.2F, 5.0F, 1.0F, 0.7F),
                ModelTransform.NONE);
        head.addChild("left_cheek_plate",
                ModelPartBuilder.create().uv(80, 0).cuboid(2.9F, -6.8F, -6.1F, 1.5F, 4.8F, 1.0F),
                ModelTransform.NONE);
        head.addChild("right_cheek_plate",
                ModelPartBuilder.create().uv(86, 0).mirrored().cuboid(-4.4F, -6.8F, -6.1F, 1.5F, 4.8F, 1.0F),
                ModelTransform.NONE);
        head.addChild("crown_band",
                ModelPartBuilder.create().uv(0, 20).cuboid(-4.8F, -11.2F, -4.8F, 9.6F, 2.2F, 9.6F),
                ModelTransform.NONE);
        head.addChild("crown_center_spike",
                ModelPartBuilder.create().uv(28, 0).cuboid(-1.0F, -16.0F, -3.2F, 2.0F, 5.2F, 2.0F),
                ModelTransform.NONE);
        head.addChild("crown_spike_left",
                ModelPartBuilder.create().uv(34, 0).cuboid(2.3F, -15.0F, -3.0F, 1.9F, 4.2F, 1.9F),
                ModelTransform.NONE);
        head.addChild("crown_spike_right",
                ModelPartBuilder.create().uv(28, 8).mirrored().cuboid(-4.2F, -15.0F, -3.0F, 1.9F, 4.2F, 1.9F),
                ModelTransform.NONE);

        ModelPartData body = root.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -12.0F, 0.0F));
        body.addChild("torso_core",
                ModelPartBuilder.create().uv(42, 12).cuboid(-9.2F, -8.0F, -5.2F, 18.4F, 26.0F, 10.4F),
                ModelTransform.NONE);
        body.addChild("chest_plate_upper",
                ModelPartBuilder.create().uv(0, 34).cuboid(-8.2F, -7.2F, -6.0F, 16.4F, 7.0F, 1.2F),
                ModelTransform.NONE);
        body.addChild("chest_plate_lower",
                ModelPartBuilder.create().uv(0, 48).cuboid(-7.2F, -0.2F, -5.9F, 14.4F, 6.6F, 1.1F),
                ModelTransform.NONE);
        body.addChild("chest_center_gem",
                ModelPartBuilder.create().uv(54, 34).cuboid(-1.6F, -3.4F, -6.3F, 3.2F, 4.4F, 0.5F),
                ModelTransform.NONE);
        body.addChild("left_chest_seam",
                ModelPartBuilder.create().uv(62, 34).cuboid(6.8F, -6.8F, -6.2F, 1.3F, 12.8F, 0.9F),
                ModelTransform.NONE);
        body.addChild("right_chest_seam",
                ModelPartBuilder.create().uv(68, 34).mirrored().cuboid(-8.1F, -6.8F, -6.2F, 1.3F, 12.8F, 0.9F),
                ModelTransform.NONE);
        body.addChild("back_plate",
                ModelPartBuilder.create().uv(74, 32).cuboid(-7.8F, -5.8F, 4.8F, 15.6F, 14.6F, 1.2F),
                ModelTransform.NONE);
        body.addChild("bronze_belt",
                ModelPartBuilder.create().uv(82, 50).cuboid(-6.6F, 12.7F, -5.8F, 13.2F, 5.3F, 1.1F),
                ModelTransform.NONE);
        body.addChild("left_shoulder_plate",
                ModelPartBuilder.create().uv(32, 34).cuboid(5.4F, -7.8F, -5.8F, 5.0F, 6.4F, 1.2F),
                ModelTransform.NONE);
        body.addChild("right_shoulder_plate",
                ModelPartBuilder.create().uv(44, 34).mirrored().cuboid(-10.4F, -7.8F, -5.8F, 5.0F, 6.4F, 1.2F),
                ModelTransform.NONE);

        addArm(root, "left", 10.0F, -17.0F, false);
        addArm(root, "right", -10.0F, -17.0F, true);
        addLeg(root, "left", 4.5F, false);
        addLeg(root, "right", -4.5F, true);

        return TexturedModelData.of(modelData, 128, 128);
    }

    private static ModelPartData addArm(ModelPartData root, String side, float x, float y, boolean mirrored) {
        int upperV = mirrored ? 78 : 62;
        ModelPartData arm = root.addChild(side + "_arm",
                ModelPartBuilder.create().uv(0, upperV).mirrored(mirrored).cuboid(mirrored ? -7.2F : 0.0F, -2.0F, -3.6F, 7.2F, 14.2F, 7.2F),
                ModelTransform.pivot(x, y, 0.0F));
        arm.addChild(side + "_elbow_ring",
                ModelPartBuilder.create().uv(30, upperV).mirrored(mirrored).cuboid(mirrored ? -7.7F : -0.5F, 10.8F, -4.0F, 8.2F, 3.4F, 8.0F),
                ModelTransform.NONE);
        ModelPartData forearm = arm.addChild(side + "_forearm",
                ModelPartBuilder.create().uv(60, upperV).mirrored(mirrored).cuboid(mirrored ? -6.9F : -0.2F, 13.2F, -3.5F, 6.7F, 12.8F, 7.0F),
                ModelTransform.NONE);
        ModelPartData fist = forearm.addChild(side + "_fist",
                ModelPartBuilder.create().uv(88, upperV).mirrored(mirrored).cuboid(mirrored ? -7.4F : -1.0F, 23.0F, -4.4F, 8.1F, 6.0F, 8.8F),
                ModelTransform.NONE);
        fist.addChild(side + "_finger_a",
                ModelPartBuilder.create().uv(112, upperV).mirrored(mirrored).cuboid(mirrored ? -6.4F : -0.9F, 28.0F, -4.8F, 3.0F, 3.0F, 2.0F),
                ModelTransform.NONE);
        fist.addChild(side + "_finger_b",
                ModelPartBuilder.create().uv(112, upperV + 8).mirrored(mirrored).cuboid(mirrored ? -3.3F : 4.1F, 28.0F, -4.8F, 3.0F, 3.0F, 2.0F),
                ModelTransform.NONE);
        if (mirrored) {
            ModelPartData sword = fist.addChild("sword",
                    ModelPartBuilder.create()
                            .uv(100, 0).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                    ModelTransform.of(-3.5F, 28.0F, -6.5F, 0.1F, 0.0F, 0.25F));
            sword.addChild("sword_crossguard",
                    ModelPartBuilder.create()
                            .uv(108, 0).cuboid(-3.5F, -3.5F, -1.2F, 7.0F, 2.0F, 2.4F),
                    ModelTransform.NONE);
            sword.addChild("sword_blade",
                    ModelPartBuilder.create()
                            .uv(96, 18).cuboid(-1.5F, -30.0F, -0.75F, 3.0F, 27.0F, 1.5F),
                    ModelTransform.NONE);
            sword.addChild("sword_blade_tip",
                    ModelPartBuilder.create()
                            .uv(106, 18).cuboid(-1.0F, -34.0F, -0.5F, 2.0F, 5.0F, 1.0F),
                    ModelTransform.NONE);
            sword.addChild("sword_pommel",
                    ModelPartBuilder.create()
                            .uv(112, 10).cuboid(-1.5F, 7.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                    ModelTransform.NONE);
        }
        return arm;
    }

    private static void addLeg(ModelPartData root, String side, float x, boolean mirrored) {
        int legU = mirrored ? 36 : 0;
        int legV = mirrored ? 104 : 94;
        int kneeU = mirrored ? 72 : 34;
        int shinU = mirrored ? 0 : 64;
        int shinV = mirrored ? 104 : 94;
        int footU = 82;
        int footV = mirrored ? 111 : 94;
        int toeU = mirrored ? 22 : 0;
        ModelPartData leg = root.addChild(side + "_leg",
                ModelPartBuilder.create().uv(legU, legV).mirrored(mirrored).cuboid(mirrored ? -8.2F : 0.0F, 0.0F, -4.2F, 8.2F, 13.0F, 8.4F),
                ModelTransform.pivot(x, 0.0F, 0.0F));
        leg.addChild(side + "_knee_ring",
                ModelPartBuilder.create().uv(kneeU, legV).mirrored(mirrored).cuboid(mirrored ? -8.7F : -0.5F, 11.8F, -4.7F, 9.2F, 3.4F, 9.4F),
                ModelTransform.NONE);
        leg.addChild(side + "_shin_plate",
                ModelPartBuilder.create().uv(shinU, shinV).mirrored(mirrored).cuboid(mirrored ? -7.8F : 0.5F, 14.5F, -4.4F, 7.3F, 9.3F, 8.8F),
                ModelTransform.NONE);
        ModelPartData foot = leg.addChild(side + "_foot",
                ModelPartBuilder.create().uv(footU, footV).mirrored(mirrored).cuboid(mirrored ? -10.2F : -0.5F, 20.5F, -6.4F, 10.2F, 4.3F, 11.6F),
                ModelTransform.NONE);
        foot.addChild(side + "_toe_plate",
                ModelPartBuilder.create().uv(toeU, 122).mirrored(mirrored).cuboid(mirrored ? -9.7F : 0.0F, 23.5F, -7.2F, 9.2F, 2.5F, 2.3F),
                ModelTransform.NONE);
    }

    @Override
    public void setAngles(ColossusEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        resetHeadAndArmor();

        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.45F) * 1.05F * limbDistance;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.45F + MathHelper.PI) * 1.05F * limbDistance;
        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.45F + MathHelper.PI) * 0.45F * limbDistance - 0.16F;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.45F) * 0.45F * limbDistance - 0.16F;
        this.rightArm.roll = 0.08F;
        this.leftArm.roll = -0.08F;
        resetArmChildren();
        resetLegChildren();
        this.sword.visible = true;
        this.sword.pitch = -0.18F;
        this.sword.roll = 0.12F;
        this.body.yaw = MathHelper.sin(limbAngle * 0.22F) * 0.05F * limbDistance;

        if (entity.handSwingProgress > 0.0F) {
            float swing = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.rightArm.pitch = -2.35F + swing * 0.7F;
            this.rightForearm.pitch = -0.5F * swing;
            this.rightFist.pitch = -0.25F * swing;
            this.sword.pitch = -0.6F + swing * 0.5F;
            this.leftArm.pitch = -0.85F + swing * 0.2F;
            this.body.pitch = swing * 0.15F;
            this.chestCenterGem.pitch = -0.08F * swing;
        } else {
            this.body.pitch = 0.0F;
        }

        if (entity.hurtTime > 0) {
            float shake = MathHelper.sin(animationProgress * 1.7F) * 0.07F;
            this.head.roll = shake;
            this.body.roll = -shake * 0.4F;
        } else {
            this.head.roll = 0.0F;
            this.body.roll = 0.0F;
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 24.0F);
            this.body.pitch = 0.85F * fall;
            this.head.pitch += 0.45F * fall;
            this.leftArm.pitch = 0.95F * fall;
            this.rightArm.pitch = 0.95F * fall;
            this.crownBand.pitch = 0.08F * fall;
        }
    }

    private void resetHeadAndArmor() {
        this.faceMask.pitch = 0.0F;
        this.eyeSlit.pitch = 0.0F;
        this.mouthGroove.pitch = 0.0F;
        this.leftCheekPlate.pitch = 0.0F;
        this.rightCheekPlate.pitch = 0.0F;
        this.crownBand.pitch = 0.0F;
        this.crownCenterSpike.pitch = 0.0F;
        this.crownSpikeLeft.pitch = 0.0F;
        this.crownSpikeRight.pitch = 0.0F;
        this.torsoCore.pitch = 0.0F;
        this.chestPlateUpper.pitch = 0.0F;
        this.chestPlateLower.pitch = 0.0F;
        this.chestCenterGem.pitch = 0.0F;
        this.leftChestSeam.pitch = 0.0F;
        this.rightChestSeam.pitch = 0.0F;
        this.backPlate.pitch = 0.0F;
        this.bronzeBelt.pitch = 0.0F;
        this.leftShoulderPlate.pitch = 0.0F;
        this.rightShoulderPlate.pitch = 0.0F;
    }

    private void resetArmChildren() {
        this.leftElbowRing.pitch = 0.0F;
        this.leftForearm.pitch = 0.0F;
        this.leftFist.pitch = 0.0F;
        this.leftFingerA.pitch = 0.0F;
        this.leftFingerB.pitch = 0.0F;
        this.rightElbowRing.pitch = 0.0F;
        this.rightForearm.pitch = 0.0F;
        this.rightFist.pitch = 0.0F;
        this.rightFingerA.pitch = 0.0F;
        this.rightFingerB.pitch = 0.0F;
    }

    private void resetLegChildren() {
        this.leftKneeRing.pitch = 0.0F;
        this.leftShinPlate.pitch = 0.0F;
        this.leftFoot.pitch = 0.0F;
        this.leftToePlate.pitch = 0.0F;
        this.rightKneeRing.pitch = 0.0F;
        this.rightShinPlate.pitch = 0.0F;
        this.rightFoot.pitch = 0.0F;
        this.rightToePlate.pitch = 0.0F;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
