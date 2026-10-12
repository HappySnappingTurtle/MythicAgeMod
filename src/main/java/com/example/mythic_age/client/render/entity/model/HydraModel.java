package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.HydraEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class HydraModel extends SinglePartEntityModel<HydraEntity> {
    private static final String[] NECK_NAMES = {
            "rear_left_neck", "outer_left_neck", "left_neck", "inner_left_neck",
            "center_neck",
            "inner_right_neck", "right_neck", "outer_right_neck", "rear_right_neck"
    };
    private static final String[] HEAD_NAMES = {
            "rear_left_head", "outer_left_head", "left_head", "inner_left_head",
            "center_head",
            "inner_right_head", "right_head", "outer_right_head", "rear_right_head"
    };
    private static final float[] BASE_YAWS = {
            1.10F, 0.78F, 0.50F, 0.25F, 0.0F, -0.25F, -0.50F, -0.78F, -1.10F
    };
    private static final float[] BASE_PITCHES = {
            -0.22F, -0.26F, -0.32F, -0.38F, -0.44F, -0.38F, -0.32F, -0.26F, -0.22F
    };
    private static final float[] HEAD_PHASES = {
            0.0F, 0.7F, 1.4F, 2.1F, 2.8F, 3.5F, 4.2F, 4.9F, 5.6F
    };

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] necks = new ModelPart[NECK_NAMES.length];
    private final ModelPart[] heads = new ModelPart[HEAD_NAMES.length];
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;
    private final ModelPart tail;

    public HydraModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        for (int i = 0; i < NECK_NAMES.length; i++) {
            this.necks[i] = this.body.getChild(NECK_NAMES[i]);
            this.heads[i] = this.necks[i].getChild(HEAD_NAMES[i]);
        }
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
        this.tail = this.body.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 38).cuboid(-3.9F, -0.9F, -19.5F, 7.8F, 3.6F, 35.0F),
                ModelTransform.pivot(0.0F, 14.8F, 2.0F)
        );
        body.addChild("shoulder_saddle",
                ModelPartBuilder.create()
                        .uv(0, 76).cuboid(-12.0F, -4.2F, -18.0F, 24.0F, 2.8F, 9.4F),
                ModelTransform.NONE);
        body.addChild("front_chest_mass",
                ModelPartBuilder.create()
                        .uv(0, 88).cuboid(-6.1F, -4.7F, -20.3F, 12.2F, 4.4F, 7.2F),
                ModelTransform.NONE);
        body.addChild("neck_collar",
                ModelPartBuilder.create()
                        .uv(0, 100).cuboid(-13.0F, -6.0F, -18.8F, 26.0F, 2.4F, 4.2F),
                ModelTransform.NONE);
        body.addChild("spine_crest_a",
                ModelPartBuilder.create()
                        .uv(0, 108).cuboid(-0.9F, -6.7F, -14.0F, 1.8F, 2.6F, 4.0F),
                ModelTransform.NONE);
        body.addChild("spine_crest_b",
                ModelPartBuilder.create()
                        .uv(10, 108).cuboid(-0.9F, -6.2F, -9.0F, 1.8F, 2.4F, 4.0F),
                ModelTransform.NONE);
        body.addChild("spine_crest_c",
                ModelPartBuilder.create()
                        .uv(20, 108).cuboid(-0.9F, -5.8F, -4.0F, 1.8F, 2.4F, 6.0F),
                ModelTransform.NONE);
        body.addChild("tail",
                ModelPartBuilder.create()
                        .uv(82, 52).cuboid(-1.9F, 0.0F, 0.0F, 3.8F, 2.8F, 19.0F),
                ModelTransform.of(0.0F, -0.1F, 13.0F, 0.05F, 0.0F, 0.0F)
        )
                .addChild("tail_mid",
                        ModelPartBuilder.create()
                                .uv(82, 76).cuboid(-1.3F, 0.2F, 18.0F, 2.6F, 2.1F, 15.0F),
                        ModelTransform.NONE)
                .addChild("tail_tip",
                        ModelPartBuilder.create()
                                .uv(82, 94).cuboid(-0.8F, 0.4F, 32.0F, 1.6F, 1.4F, 12.0F),
                        ModelTransform.NONE);

        addNeck(body, "center_neck", "center_head", 0.0F, -5.8F, -17.3F, 0.0F, 0, 0, 1.05F);
        addNeck(body, "inner_left_neck", "inner_left_head", 3.5F, -5.6F, -17.0F, 0.25F, 128, 0, 0.95F);
        addNeck(body, "inner_right_neck", "inner_right_head", -3.5F, -5.6F, -17.0F, -0.25F, 128, 20, 0.95F);
        addNeck(body, "left_neck", "left_head", 6.5F, -5.3F, -16.5F, 0.50F, 64, 20, 0.90F);
        addNeck(body, "right_neck", "right_head", -6.5F, -5.3F, -16.5F, -0.50F, 64, 40, 0.90F);
        addNeck(body, "outer_left_neck", "outer_left_head", 9.5F, -5.0F, -15.8F, 0.78F, 128, 40, 0.95F);
        addNeck(body, "outer_right_neck", "outer_right_head", -9.5F, -5.0F, -15.8F, -0.78F, 128, 60, 0.95F);
        addNeck(body, "rear_left_neck", "rear_left_head", 12.8F, -4.8F, -14.5F, 1.25F, 192, 0, 0.92F);
        addNeck(body, "rear_right_neck", "rear_right_head", -12.8F, -4.8F, -14.5F, -1.25F, 192, 20, 0.92F);
        addLeg(root, "front_left_leg", 5.1F, 17.0F, -8.4F, false);
        addLeg(root, "front_right_leg", -5.1F, 17.0F, -8.4F, true);
        addLeg(root, "back_left_leg", 5.3F, 17.0F, 8.5F, false);
        addLeg(root, "back_right_leg", -5.3F, 17.0F, 8.5F, true);

        return TexturedModelData.of(modelData, 256, 256);
    }

    private static void addNeck(ModelPartData parent, String neckName, String headName, float x, float y, float z,
                                float yaw, int neckU, int neckV, float headScale) {
        ModelPartData neck = parent.addChild(neckName,
                ModelPartBuilder.create()
                        .uv(neckU, neckV).cuboid(-1.25F, -15.2F, -1.25F, 2.5F, 18.0F, 2.5F),
                ModelTransform.of(x, y, z, -0.66F, yaw, 0.0F)
        );
        neck.addChild(neckName + "_base_socket",
                ModelPartBuilder.create()
                        .uv(104, 34).cuboid(-2.55F, -0.2F, -2.8F, 5.1F, 3.8F, 5.6F)
                        .uv(104, 44).cuboid(-2.0F, -1.2F, -2.2F, 4.0F, 2.2F, 4.4F),
                ModelTransform.NONE);
        neck.addChild(neckName + "_body_overlap",
                ModelPartBuilder.create()
                        .uv(92, 110).cuboid(-2.45F, 0.6F, -3.8F, 4.9F, 4.0F, 6.6F),
                ModelTransform.NONE);
        neck.addChild(neckName + "_head_overlap",
                ModelPartBuilder.create()
                        .uv(92, 118).cuboid(-2.05F, -16.4F, -3.25F, 4.1F, 3.2F, 5.0F),
                ModelTransform.NONE);
        neck.addChild(neckName + "_throat_ridge",
                ModelPartBuilder.create()
                        .uv(100, 18).cuboid(-0.5F, -14.8F, -2.0F, 1.0F, 12.8F, 0.85F),
                ModelTransform.NONE);
        float detailScale = Math.max(0.9F, headScale);
        ModelPartData head = neck.addChild(headName,
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.5F * headScale, -7.5F * headScale, -5.7F * headScale,
                                9.0F * headScale, 8.5F * headScale, 8.8F * headScale),
                ModelTransform.pivot(0.0F, -15.2F, -2.6F)
        );
        head.addChild(headName + "_upper_jaw",
                ModelPartBuilder.create()
                        .uv(0, 18).cuboid(-3.7F * headScale, -2.8F * headScale, -10.4F * headScale,
                                7.4F * headScale, 3.5F * headScale, 5.3F * headScale),
                ModelTransform.NONE);
        head.addChild(headName + "_lower_jaw",
                ModelPartBuilder.create()
                        .uv(24, 18).cuboid(-3.2F * headScale, 0.6F * headScale, -9.8F * headScale,
                                6.4F * headScale, 2.2F * headScale, 4.6F * headScale),
                ModelTransform.NONE);
        head.addChild(headName + "_brow",
                ModelPartBuilder.create()
                        .uv(48, 18).cuboid(-4.7F * headScale, -6.2F * headScale, -6.5F * headScale,
                                9.4F * headScale, 1.4F * headScale, 1.3F * headScale),
                ModelTransform.NONE);
        head.addChild(headName + "_left_eye",
                ModelPartBuilder.create()
                        .uv(64, 96).cuboid(-3.7F * headScale, -4.9F * headScale, -6.9F * headScale,
                                1.3F * detailScale, 1.3F * detailScale, 1.0F * detailScale),
                ModelTransform.NONE);
        head.addChild(headName + "_right_eye",
                ModelPartBuilder.create()
                        .uv(70, 96).cuboid(2.4F * headScale, -4.9F * headScale, -6.9F * headScale,
                                1.3F * detailScale, 1.3F * detailScale, 1.0F * detailScale),
                ModelTransform.NONE);
        head.addChild(headName + "_left_fang",
                ModelPartBuilder.create()
                        .uv(76, 96).cuboid(-2.7F * headScale, 0.2F * headScale, -10.3F * headScale,
                                0.8F * detailScale, 2.4F * detailScale, 0.8F * detailScale),
                ModelTransform.NONE);
        head.addChild(headName + "_right_fang",
                ModelPartBuilder.create()
                        .uv(80, 96).cuboid(1.9F * headScale, 0.2F * headScale, -10.3F * headScale,
                                0.8F * detailScale, 2.4F * detailScale, 0.8F * detailScale),
                ModelTransform.NONE);
        head.addChild(headName + "_left_horn",
                ModelPartBuilder.create()
                        .uv(88, 0).cuboid(-3.8F * headScale, -9.7F * headScale, -2.4F * headScale,
                                1.6F * detailScale, 3.6F * detailScale, 1.6F * detailScale),
                ModelTransform.NONE);
        head.addChild(headName + "_right_horn",
                ModelPartBuilder.create()
                        .uv(88, 0).mirrored().cuboid(2.2F * headScale, -9.7F * headScale, -2.4F * headScale,
                                1.6F * detailScale, 3.6F * detailScale, 1.6F * detailScale),
                ModelTransform.NONE);
        head.addChild(headName + "_left_poison_gland",
                ModelPartBuilder.create()
                        .uv(100, 0).cuboid(-5.2F * headScale, -2.2F * headScale, -4.3F * headScale,
                                1.2F * headScale, 2.4F * headScale, 2.2F * headScale),
                ModelTransform.NONE);
        head.addChild(headName + "_right_poison_gland",
                ModelPartBuilder.create()
                        .uv(100, 0).mirrored().cuboid(4.0F * headScale, -2.2F * headScale, -4.3F * headScale,
                                1.2F * headScale, 2.4F * headScale, 2.2F * headScale),
                ModelTransform.NONE);
    }

    private static void addLeg(ModelPartData root, String name, float x, float y, float z, boolean mirrored) {
        root.addChild(name,
                ModelPartBuilder.create()
                        .uv(54, 76).mirrored(mirrored).cuboid(-1.6F, 0.0F, -2.3F, 3.0F, 7.0F, 4.5F)
                        .uv(70, 76).mirrored(mirrored).cuboid(-2.4F, 6.0F, -4.2F, 4.5F, 2.0F, 6.0F)
                        .uv(104, 94).mirrored(mirrored).cuboid(-2.5F, 7.3F, -5.0F, 1.0F, 0.8F, 2.0F)
                        .uv(110, 94).mirrored(mirrored).cuboid(0.8F, 7.3F, -5.0F, 1.0F, 0.8F, 2.0F),
                ModelTransform.pivot(x, y, z)
        );
    }

    @Override
    public void setAngles(HydraEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        float headYawRad = headYaw * (MathHelper.PI / 180.0F);
        float headPitchRad = headPitch * (MathHelper.PI / 180.0F);

        for (int i = 0; i < this.necks.length; i++) {
            float phase = HEAD_PHASES[i];
            float headWeight = i == 2 ? 0.36F : 0.24F;
            this.necks[i].yaw = BASE_YAWS[i] + headYawRad * headWeight
                    + MathHelper.sin(animationProgress * 0.085F + phase) * 0.06F;
            this.heads[i].pitch = headPitchRad * (i == 2 ? 0.46F : 0.34F)
                    + MathHelper.sin(animationProgress * 0.12F + phase) * 0.055F;
            this.heads[i].yaw = MathHelper.sin(animationProgress * 0.07F + phase) * 0.035F;
        }

        float crawl = 0.75F * limbDistance;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.7F) * crawl;
        this.backLeftLeg.pitch = MathHelper.cos(limbAngle * 0.7F) * crawl;
        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.7F + MathHelper.PI) * crawl;
        this.backRightLeg.pitch = MathHelper.cos(limbAngle * 0.7F + MathHelper.PI) * crawl;
        this.body.yaw = MathHelper.sin(limbAngle * 0.35F) * 0.06F * limbDistance;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.12F) * 0.25F + MathHelper.sin(limbAngle * 0.35F) * 0.18F * limbDistance;

        if (entity.handSwingProgress > 0.0F) {
            float strike = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            for (int i = 0; i < this.necks.length; i++) {
                float centerBias = i == 4 ? 0.55F : 0.4F;
                this.necks[i].pitch = BASE_PITCHES[i] - strike * centerBias;
                this.heads[i].pitch = -strike * 0.3F;
            }
        } else {
            for (int i = 0; i < this.necks.length; i++) {
                this.necks[i].pitch = BASE_PITCHES[i];
            }
        }

        if (entity.hurtTime > 0) {
            float shake = MathHelper.sin(animationProgress * 2.5F) * 0.08F;
            for (int i = 0; i < this.heads.length; i++) {
                this.heads[i].roll = (i % 2 == 0 ? shake : -shake) * (0.7F + i * 0.08F);
            }
        } else {
            for (ModelPart head : this.heads) {
                head.roll = 0.0F;
            }
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.2F * fall;
            for (ModelPart neck : this.necks) {
                neck.pitch = 0.25F * fall;
            }
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
