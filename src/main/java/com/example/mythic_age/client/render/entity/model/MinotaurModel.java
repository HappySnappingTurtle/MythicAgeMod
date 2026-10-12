package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.MinotaurEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class MinotaurModel extends SinglePartEntityModel<MinotaurEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart muzzle;
    private final ModelPart heavyBrow;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftNostril;
    private final ModelPart rightNostril;
    private final ModelPart mouth;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart body;
    private final ModelPart chestHarness;
    private final ModelPart leftChestMuscle;
    private final ModelPart rightChestMuscle;
    private final ModelPart waistBelt;
    private final ModelPart loinclothFront;
    private final ModelPart neck;
    private final ModelPart tail;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart leftFist;
    private final ModelPart leftFingerA;
    private final ModelPart leftFingerB;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart rightFist;
    private final ModelPart rightFingerA;
    private final ModelPart rightFingerB;
    private final ModelPart labrysAxe;
    private final ModelPart labrysHandle;
    private final ModelPart labrysLeftBlade;
    private final ModelPart labrysRightBlade;
    private final ModelPart labrysCenterSocket;
    private final ModelPart labrysCounterweight;
    private final ModelPart leftLeg;
    private final ModelPart leftHoof;
    private final ModelPart leftHoofClaw;
    private final ModelPart rightLeg;
    private final ModelPart rightHoof;
    private final ModelPart rightHoofClaw;

    public MinotaurModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.muzzle = this.head.getChild("muzzle");
        this.heavyBrow = this.head.getChild("heavy_brow");
        this.leftEye = this.head.getChild("left_eye");
        this.rightEye = this.head.getChild("right_eye");
        this.leftNostril = this.head.getChild("left_nostril");
        this.rightNostril = this.head.getChild("right_nostril");
        this.mouth = this.head.getChild("mouth");
        this.leftHorn = this.head.getChild("left_horn");
        this.rightHorn = this.head.getChild("right_horn");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.body = root.getChild("body");
        this.chestHarness = this.body.getChild("chest_harness");
        this.leftChestMuscle = this.body.getChild("left_chest_muscle");
        this.rightChestMuscle = this.body.getChild("right_chest_muscle");
        this.waistBelt = this.body.getChild("waist_belt");
        this.loinclothFront = this.body.getChild("loincloth_front");
        this.neck = this.body.getChild("neck");
        this.tail = this.body.getChild("tail");
        this.leftArm = root.getChild("left_arm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.leftFist = this.leftArm.getChild("left_fist");
        this.leftFingerA = this.leftFist.getChild("left_finger_a");
        this.leftFingerB = this.leftFist.getChild("left_finger_b");
        this.rightArm = root.getChild("right_arm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.rightFist = this.rightArm.getChild("right_fist");
        this.rightFingerA = this.rightFist.getChild("right_finger_a");
        this.rightFingerB = this.rightFist.getChild("right_finger_b");
        this.labrysAxe = this.rightArm.getChild("labrys_axe");
        this.labrysHandle = this.labrysAxe.getChild("labrys_handle");
        this.labrysLeftBlade = this.labrysAxe.getChild("labrys_left_blade");
        this.labrysRightBlade = this.labrysAxe.getChild("labrys_right_blade");
        this.labrysCenterSocket = this.labrysAxe.getChild("labrys_center_socket");
        this.labrysCounterweight = this.labrysAxe.getChild("labrys_counterweight");
        this.leftLeg = root.getChild("left_leg");
        this.leftHoof = this.leftLeg.getChild("left_hoof");
        this.leftHoofClaw = this.leftHoof.getChild("left_hoof_claw");
        this.rightLeg = root.getChild("right_leg");
        this.rightHoof = this.rightLeg.getChild("right_hoof");
        this.rightHoofClaw = this.rightHoof.getChild("right_hoof_claw");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.35F, -10.2F, -4.45F, 8.7F, 10.2F, 8.7F),
                ModelTransform.pivot(0.0F, -2.0F, 0.0F));
        head.addChild("muzzle",
                ModelPartBuilder.create().uv(44, 0).cuboid(-2.8F, -4.6F, -8.7F, 5.6F, 4.6F, 4.25F),
                ModelTransform.NONE);
        head.addChild("heavy_brow",
                ModelPartBuilder.create().uv(70, 0).cuboid(-3.6F, -6.75F, -5.45F, 7.2F, 1.6F, 1.1F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(106, 0).cuboid(-2.9F, -5.65F, -5.95F, 1.1F, 1.1F, 0.9F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(112, 0).cuboid(1.8F, -5.65F, -5.95F, 1.1F, 1.1F, 0.9F),
                ModelTransform.NONE);
        head.addChild("left_nostril",
                ModelPartBuilder.create().uv(118, 0).cuboid(-1.7F, -2.8F, -9.25F, 1.0F, 0.9F, 0.9F),
                ModelTransform.NONE);
        head.addChild("right_nostril",
                ModelPartBuilder.create().uv(122, 0).cuboid(0.7F, -2.8F, -9.25F, 1.0F, 0.9F, 0.9F),
                ModelTransform.NONE);
        head.addChild("mouth",
                ModelPartBuilder.create().uv(106, 6).cuboid(-2.0F, -1.1F, -9.15F, 4.0F, 0.9F, 0.9F),
                ModelTransform.NONE);
        head.addChild("left_horn",
                ModelPartBuilder.create().uv(78, 0).cuboid(2.75F, -14.7F, -2.1F, 5.0F, 5.8F, 4.2F),
                ModelTransform.NONE);
        head.addChild("right_horn",
                ModelPartBuilder.create().uv(100, 0).mirrored().cuboid(-7.75F, -14.7F, -2.1F, 5.0F, 5.8F, 4.2F),
                ModelTransform.NONE);
        head.addChild("left_ear",
                ModelPartBuilder.create().uv(84, 14).cuboid(3.35F, -8.9F, -3.4F, 2.0F, 3.6F, 2.5F),
                ModelTransform.NONE);
        head.addChild("right_ear",
                ModelPartBuilder.create().uv(94, 14).mirrored().cuboid(-5.35F, -8.9F, -3.4F, 2.0F, 3.6F, 2.5F),
                ModelTransform.NONE);

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(40, 0).cuboid(-6.0F, -11.0F, -4.0F, 12.0F, 16.0F, 8.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        body.addChild("chest_harness",
                ModelPartBuilder.create().uv(0, 30).cuboid(-5.2F, -9.0F, -4.8F, 10.4F, 10.5F, 1.0F),
                ModelTransform.NONE);
        body.addChild("left_chest_muscle",
                ModelPartBuilder.create().uv(24, 30).cuboid(0.8F, -7.8F, -5.2F, 4.0F, 5.6F, 1.0F),
                ModelTransform.NONE);
        body.addChild("right_chest_muscle",
                ModelPartBuilder.create().uv(36, 30).cuboid(-4.8F, -7.8F, -5.2F, 4.0F, 5.6F, 1.0F),
                ModelTransform.NONE);
        body.addChild("waist_belt",
                ModelPartBuilder.create().uv(0, 42).cuboid(-5.6F, 2.0F, -4.8F, 11.2F, 2.2F, 1.0F),
                ModelTransform.NONE);
        body.addChild("loincloth_front",
                ModelPartBuilder.create().uv(24, 42).cuboid(-2.4F, 3.8F, -5.0F, 4.8F, 6.7F, 1.2F),
                ModelTransform.NONE);
        body.addChild("neck",
                ModelPartBuilder.create().uv(0, 24).cuboid(-3.2F, -14.0F, -3.2F, 6.4F, 6.0F, 6.4F),
                ModelTransform.NONE);
        body.addChild("tail",
                ModelPartBuilder.create().uv(0, 50).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 13.0F, 2.0F),
                ModelTransform.of(0.0F, 2.0F, 3.5F, 0.35F, 0.0F, 0.0F));

        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create().uv(68, 24).mirrored().cuboid(-1.0F, -9.0F, -3.2F, 5.0F, 12.8F, 6.4F),
                ModelTransform.pivot(6.0F, 2.0F, 0.0F));
        leftArm.addChild("left_forearm",
                ModelPartBuilder.create().uv(68, 48).mirrored().cuboid(-0.8F, 3.4F, -3.0F, 4.8F, 7.8F, 6.0F),
                ModelTransform.NONE);
        ModelPartData leftFist = leftArm.addChild("left_fist",
                ModelPartBuilder.create().uv(68, 62).mirrored().cuboid(-1.2F, 10.4F, -3.4F, 5.6F, 4.0F, 6.8F),
                ModelTransform.NONE);
        leftFist.addChild("left_finger_a",
                ModelPartBuilder.create().uv(86, 62).cuboid(-1.0F, 13.4F, -4.0F, 1.8F, 3.0F, 2.0F),
                ModelTransform.NONE);
        leftFist.addChild("left_finger_b",
                ModelPartBuilder.create().uv(94, 62).cuboid(2.0F, 13.4F, -4.0F, 1.8F, 3.0F, 2.0F),
                ModelTransform.NONE);

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create().uv(94, 24).cuboid(-4.0F, -9.0F, -3.2F, 5.0F, 12.8F, 6.4F),
                ModelTransform.pivot(-6.0F, 2.0F, 0.0F));
        rightArm.addChild("right_forearm",
                ModelPartBuilder.create().uv(94, 48).cuboid(-4.8F, 3.4F, -3.0F, 4.8F, 7.8F, 6.0F),
                ModelTransform.NONE);
        ModelPartData rightFist = rightArm.addChild("right_fist",
                ModelPartBuilder.create().uv(102, 62).cuboid(-5.2F, 10.4F, -3.4F, 5.6F, 4.0F, 6.8F),
                ModelTransform.NONE);
        rightFist.addChild("right_finger_a",
                ModelPartBuilder.create().uv(112, 62).cuboid(-4.8F, 13.4F, -4.0F, 1.8F, 3.0F, 2.0F),
                ModelTransform.NONE);
        rightFist.addChild("right_finger_b",
                ModelPartBuilder.create().uv(120, 62).cuboid(-1.8F, 13.4F, -4.0F, 1.8F, 3.0F, 2.0F),
                ModelTransform.NONE);
        ModelPartData axe = rightArm.addChild("labrys_axe", ModelPartBuilder.create(), ModelTransform.NONE);
        axe.addChild("labrys_handle",
                ModelPartBuilder.create().uv(88, 64).cuboid(-7.0F, -9.0F, -1.0F, 2.0F, 24.0F, 2.0F),
                ModelTransform.NONE);
        axe.addChild("labrys_left_blade",
                ModelPartBuilder.create().uv(74, 86).cuboid(-12.0F, -12.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                ModelTransform.NONE);
        axe.addChild("labrys_right_blade",
                ModelPartBuilder.create().uv(100, 86).cuboid(-6.0F, -12.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                ModelTransform.NONE);
        axe.addChild("labrys_center_socket",
                ModelPartBuilder.create().uv(88, 86).cuboid(-7.5F, -11.0F, -2.2F, 3.0F, 4.0F, 4.4F),
                ModelTransform.NONE);
        axe.addChild("labrys_counterweight",
                ModelPartBuilder.create().uv(74, 104).cuboid(-7.8F, 13.0F, -2.0F, 3.6F, 4.0F, 4.0F),
                ModelTransform.NONE);

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create().uv(0, 64).mirrored().cuboid(-1.4F, 0.0F, -3.0F, 3.8F, 18.0F, 6.0F),
                ModelTransform.pivot(2.8F, 5.0F, 0.0F));
        ModelPartData leftHoof = leftLeg.addChild("left_hoof",
                ModelPartBuilder.create().uv(40, 64).mirrored().cuboid(-2.0F, 16.0F, -4.5F, 4.8F, 3.0F, 8.0F),
                ModelTransform.NONE);
        leftHoof.addChild("left_hoof_claw",
                ModelPartBuilder.create().uv(40, 78).cuboid(-2.3F, 18.3F, -5.7F, 5.3F, 1.4F, 1.5F),
                ModelTransform.NONE);
        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create().uv(20, 64).cuboid(-2.4F, 0.0F, -3.0F, 3.8F, 18.0F, 6.0F),
                ModelTransform.pivot(-2.8F, 5.0F, 0.0F));
        ModelPartData rightHoof = rightLeg.addChild("right_hoof",
                ModelPartBuilder.create().uv(64, 64).cuboid(-2.8F, 16.0F, -4.5F, 4.8F, 3.0F, 8.0F),
                ModelTransform.NONE);
        rightHoof.addChild("right_hoof_claw",
                ModelPartBuilder.create().uv(64, 78).cuboid(-3.0F, 18.3F, -5.7F, 5.3F, 1.4F, 1.5F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(MinotaurEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F);
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F);
        resetFaceAndBodyParts();

        this.rightArm.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 1.35F * limbDistance;
        this.leftArm.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.35F * limbDistance;
        this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + MathHelper.PI) * 1.4F * limbDistance;

        this.rightArm.roll = 0.08F;
        this.leftArm.roll = -0.08F;
        resetArmChildren();
        resetLegChildren();
        this.labrysAxe.visible = true;
        this.labrysHandle.pitch = 0.0F;
        this.labrysLeftBlade.pitch = 0.0F;
        this.labrysRightBlade.pitch = 0.0F;
        this.labrysCenterSocket.pitch = 0.0F;
        this.labrysCounterweight.pitch = 0.0F;

        float charge = entity.handSwingProgress > 0.0F ? MathHelper.sin(entity.handSwingProgress * MathHelper.PI) : 0.0F;
        this.body.pitch = -0.18F * charge;
        this.head.pitch -= 0.35F * charge;
        this.labrysAxe.roll = -0.18F * charge;
        this.body.yaw = MathHelper.sin(limbAngle * 0.3331F) * 0.08F * limbDistance;
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.4F) * 0.08F;
            this.body.roll = recoil;
            this.head.roll = -recoil * 0.8F;
            this.labrysAxe.roll -= recoil * 0.6F;
        } else {
            this.body.roll = 0.0F;
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.15F * fall;
            this.head.pitch += 0.35F * fall;
            this.leftArm.pitch = -0.35F * fall;
            this.rightArm.pitch = -0.85F * fall;
            this.labrysAxe.roll = -0.55F * fall;
        }
    }

    private void resetFaceAndBodyParts() {
        this.muzzle.pitch = 0.0F;
        this.heavyBrow.pitch = 0.0F;
        this.leftEye.pitch = 0.0F;
        this.rightEye.pitch = 0.0F;
        this.leftNostril.pitch = 0.0F;
        this.rightNostril.pitch = 0.0F;
        this.mouth.pitch = 0.0F;
        this.leftHorn.pitch = 0.0F;
        this.rightHorn.pitch = 0.0F;
        this.leftEar.pitch = 0.0F;
        this.rightEar.pitch = 0.0F;
        this.chestHarness.pitch = 0.0F;
        this.leftChestMuscle.pitch = 0.0F;
        this.rightChestMuscle.pitch = 0.0F;
        this.waistBelt.pitch = 0.0F;
        this.loinclothFront.pitch = 0.0F;
        this.neck.pitch = 0.0F;
        this.tail.pitch = 0.35F + MathHelper.sin(this.head.yaw * 2.0F) * 0.04F;
    }

    private void resetArmChildren() {
        this.leftForearm.pitch = 0.0F;
        this.leftFist.pitch = 0.0F;
        this.leftFingerA.pitch = 0.0F;
        this.leftFingerB.pitch = 0.0F;
        this.rightForearm.pitch = 0.0F;
        this.rightFist.pitch = 0.0F;
        this.rightFingerA.pitch = 0.0F;
        this.rightFingerB.pitch = 0.0F;
    }

    private void resetLegChildren() {
        this.leftHoof.pitch = 0.0F;
        this.leftHoofClaw.pitch = 0.0F;
        this.rightHoof.pitch = 0.0F;
        this.rightHoofClaw.pitch = 0.0F;
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
