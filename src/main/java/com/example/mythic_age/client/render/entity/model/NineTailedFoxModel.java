package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.NineTailedFoxEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class NineTailedFoxModel extends SinglePartEntityModel<NineTailedFoxEntity> {
    private static final float[] BASE_TAIL_YAW = {-0.78F, -0.58F, -0.38F, -0.18F, 0.0F, 0.18F, 0.38F, 0.58F, 0.78F};
    private static final float[] BASE_TAIL_PITCH = {-0.22F, -0.28F, -0.34F, -0.42F, -0.48F, -0.42F, -0.34F, -0.28F, -0.22F};
    private static final float[] BASE_TAIL_ROLL = {-0.38F, -0.3F, -0.2F, -0.1F, 0.0F, 0.1F, 0.2F, 0.3F, 0.38F};

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart body;
    private final ModelPart chestFur;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;
    private final ModelPart[] tails = new ModelPart[9];

    public NineTailedFoxModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.body = root.getChild("fox_body");
        this.chestFur = root.getChild("chest_fur");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
        for (int i = 0; i < this.tails.length; i++) {
            this.tails[i] = root.getChild("tail_" + (i + 1));
        }
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("fox_body",
                ModelPartBuilder.create()
                        .uv(0, 28).cuboid(-4.4F, -4.2F, -7.9F, 8.8F, 7.4F, 15.4F),
                ModelTransform.pivot(0.0F, 14.6F, 0.8F));
        body.addChild("shoulder_fur",
                ModelPartBuilder.create()
                        .uv(42, 30).cuboid(-3.2F, -5.0F, -5.0F, 6.4F, 1.8F, 8.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData chestFur = root.addChild("chest_fur",
                ModelPartBuilder.create()
                        .uv(74, 30).cuboid(-3.1F, -2.8F, -1.0F, 6.2F, 6.2F, 1.2F),
                ModelTransform.of(0.0F, 12.6F, -6.7F, -0.18F, 0.0F, 0.0F));
        chestFur.addChild("chest_fur_point",
                ModelPartBuilder.create()
                        .uv(92, 30).cuboid(-2.0F, 2.0F, -1.35F, 4.0F, 2.6F, 1.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.0F, -3.95F, -4.85F, 6.0F, 5.8F, 5.6F),
                ModelTransform.pivot(0.0F, 10.2F, -7.0F));
        head.addChild("snout",
                ModelPartBuilder.create()
                        .uv(30, 0).cuboid(-1.75F, -1.25F, -8.05F, 3.5F, 2.45F, 3.5F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("left_eye",
                ModelPartBuilder.create()
                        .uv(52, 0).cuboid(-2.35F, -2.25F, -5.25F, 0.95F, 0.95F, 0.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("right_eye",
                ModelPartBuilder.create()
                        .uv(58, 0).cuboid(1.4F, -2.25F, -5.25F, 0.95F, 0.95F, 0.7F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("nose",
                ModelPartBuilder.create()
                        .uv(64, 0).cuboid(-0.7F, -0.25F, -8.42F, 1.4F, 0.8F, 0.72F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        head.addChild("mouth_line",
                ModelPartBuilder.create()
                        .uv(70, 0).cuboid(-1.2F, 0.78F, -8.22F, 2.4F, 0.42F, 0.66F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData leftEar = head.addChild("left_ear",
                ModelPartBuilder.create()
                        .uv(82, 0).cuboid(-0.9F, -2.9F, -0.7F, 1.8F, 3.1F, 1.3F),
                ModelTransform.of(2.35F, -3.9F, -3.0F, 0.08F, -0.18F, 0.28F));
        leftEar.addChild("left_ear_tip",
                ModelPartBuilder.create()
                        .uv(90, 0).cuboid(-0.55F, -4.2F, -0.55F, 1.1F, 1.8F, 1.1F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData rightEar = head.addChild("right_ear",
                ModelPartBuilder.create()
                        .uv(82, 7).cuboid(-0.9F, -2.9F, -0.7F, 1.8F, 3.1F, 1.3F),
                ModelTransform.of(-2.35F, -3.9F, -3.0F, 0.08F, 0.18F, -0.28F));
        rightEar.addChild("right_ear_tip",
                ModelPartBuilder.create()
                        .uv(90, 7).cuboid(-0.55F, -4.2F, -0.55F, 1.1F, 1.8F, 1.1F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData frontLeftLeg = root.addChild("front_left_leg",
                ModelPartBuilder.create()
                        .uv(0, 52).cuboid(-1.15F, 0.0F, -1.1F, 2.3F, 6.8F, 2.2F),
                ModelTransform.pivot(2.7F, 16.6F, -4.6F));
        frontLeftLeg.addChild("front_left_paw",
                ModelPartBuilder.create()
                        .uv(10, 52).cuboid(-1.45F, 6.2F, -1.85F, 2.9F, 1.2F, 2.6F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData frontRightLeg = root.addChild("front_right_leg",
                ModelPartBuilder.create()
                        .uv(22, 52).cuboid(-1.15F, 0.0F, -1.1F, 2.3F, 6.8F, 2.2F),
                ModelTransform.pivot(-2.7F, 16.6F, -4.6F));
        frontRightLeg.addChild("front_right_paw",
                ModelPartBuilder.create()
                        .uv(32, 52).cuboid(-1.45F, 6.2F, -1.85F, 2.9F, 1.2F, 2.6F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData backLeftLeg = root.addChild("back_left_leg",
                ModelPartBuilder.create()
                        .uv(44, 52).cuboid(-1.25F, 0.0F, -1.2F, 2.5F, 6.7F, 2.4F),
                ModelTransform.pivot(2.9F, 16.7F, 4.7F));
        backLeftLeg.addChild("back_left_paw",
                ModelPartBuilder.create()
                        .uv(56, 52).cuboid(-1.55F, 6.1F, -1.7F, 3.1F, 1.25F, 2.6F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData backRightLeg = root.addChild("back_right_leg",
                ModelPartBuilder.create()
                        .uv(70, 52).cuboid(-1.25F, 0.0F, -1.2F, 2.5F, 6.7F, 2.4F),
                ModelTransform.pivot(-2.9F, 16.7F, 4.7F));
        backRightLeg.addChild("back_right_paw",
                ModelPartBuilder.create()
                        .uv(82, 52).cuboid(-1.55F, 6.1F, -1.7F, 3.1F, 1.25F, 2.6F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        addTail(root, "tail_1", "tail_1_tip", 0, 78, -2.3F, 11.7F, BASE_TAIL_PITCH[0], BASE_TAIL_YAW[0], BASE_TAIL_ROLL[0]);
        addTail(root, "tail_2", "tail_2_tip", 30, 78, -1.7F, 10.9F, BASE_TAIL_PITCH[1], BASE_TAIL_YAW[1], BASE_TAIL_ROLL[1]);
        addTail(root, "tail_3", "tail_3_tip", 60, 78, -1.0F, 10.3F, BASE_TAIL_PITCH[2], BASE_TAIL_YAW[2], BASE_TAIL_ROLL[2]);
        addTail(root, "tail_4", "tail_4_tip", 90, 78, -0.35F, 9.9F, BASE_TAIL_PITCH[3], BASE_TAIL_YAW[3], BASE_TAIL_ROLL[3]);
        addTail(root, "tail_5", "tail_5_tip", 0, 102, 0.0F, 9.7F, BASE_TAIL_PITCH[4], BASE_TAIL_YAW[4], BASE_TAIL_ROLL[4]);
        addTail(root, "tail_6", "tail_6_tip", 30, 102, 0.35F, 9.9F, BASE_TAIL_PITCH[5], BASE_TAIL_YAW[5], BASE_TAIL_ROLL[5]);
        addTail(root, "tail_7", "tail_7_tip", 60, 102, 1.0F, 10.3F, BASE_TAIL_PITCH[6], BASE_TAIL_YAW[6], BASE_TAIL_ROLL[6]);
        addTail(root, "tail_8", "tail_8_tip", 90, 102, 1.7F, 10.9F, BASE_TAIL_PITCH[7], BASE_TAIL_YAW[7], BASE_TAIL_ROLL[7]);
        addTail(root, "tail_9", "tail_9_tip", 0, 78, 2.3F, 11.7F, BASE_TAIL_PITCH[8], BASE_TAIL_YAW[8], BASE_TAIL_ROLL[8]);

        return TexturedModelData.of(modelData, 128, 128);
    }

    private static void addTail(ModelPartData root, String name, String tipName, int uvX, int uvY, float x, float y, float pitch, float yaw, float roll) {
        ModelPartData tail = root.addChild(name,
                ModelPartBuilder.create()
                        .uv(uvX, uvY).cuboid(-0.75F, -0.75F, 0.0F, 1.5F, 1.5F, 8.8F)
                        .uv(uvX + 8, uvY).cuboid(-1.45F, -1.45F, 6.5F, 2.9F, 2.9F, 4.9F),
                ModelTransform.of(x, y, 6.0F, pitch, yaw, roll));
        tail.addChild(tipName,
                ModelPartBuilder.create()
                        .uv(uvX + 22, uvY).cuboid(-1.0F, -1.0F, 10.3F, 2.0F, 2.0F, 2.3F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
    }

    @Override
    public void setAngles(NineTailedFoxEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.72F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.72F;
        this.leftEar.roll = 0.28F + MathHelper.sin(animationProgress * 0.12F) * 0.035F;
        this.rightEar.roll = -0.28F - MathHelper.sin(animationProgress * 0.12F) * 0.035F;

        float walk = MathHelper.cos(limbAngle * 0.82F) * limbDistance;
        float counterWalk = MathHelper.cos(limbAngle * 0.82F + MathHelper.PI) * limbDistance;
        this.frontLeftLeg.pitch = counterWalk * 0.95F;
        this.backRightLeg.pitch = counterWalk * 0.9F;
        this.frontRightLeg.pitch = walk * 0.95F;
        this.backLeftLeg.pitch = walk * 0.9F;
        this.body.roll = MathHelper.sin(limbAngle * 0.82F) * limbDistance * 0.06F;
        this.chestFur.pitch = -0.18F + MathHelper.sin(animationProgress * 0.11F) * 0.025F;

        float glamour = entity.handSwingProgress > 0.0F ? MathHelper.sin(entity.handSwingProgress * MathHelper.PI) : 0.0F;
        for (int i = 0; i < this.tails.length; i++) {
            float side = i - 4.0F;
            float wave = MathHelper.sin(animationProgress * 0.13F + i * 0.55F) * 0.08F;
            this.tails[i].yaw = BASE_TAIL_YAW[i] + wave + side * 0.025F * glamour;
            this.tails[i].pitch = BASE_TAIL_PITCH[i] - 0.18F * glamour + MathHelper.cos(animationProgress * 0.1F + i) * 0.035F;
            this.tails[i].roll = BASE_TAIL_ROLL[i] + MathHelper.cos(animationProgress * 0.12F + i * 0.4F) * 0.06F;
        }

        if (glamour > 0.0F) {
            this.head.pitch -= 0.16F * glamour;
            this.chestFur.pitch -= 0.06F * glamour;
        }
        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 1.8F) * 0.1F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 18.0F);
            this.body.roll = 1.0F * fall;
            this.head.pitch += 0.35F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
