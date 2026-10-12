package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.SunFalconEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class SunFalconModel extends SinglePartEntityModel<SunFalconEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart sunDisk;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;

    public SunFalconModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.sunDisk = this.head.getChild("sun_disk");
        this.body = root.getChild("body");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.tail = root.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 26).cuboid(-3.8F, -4.4F, -5.1F, 7.6F, 8.6F, 10.8F),
                ModelTransform.pivot(0.0F, 13.6F, 1.0F));
        body.addChild("belly",
                ModelPartBuilder.create().uv(36, 28).cuboid(-2.7F, 3.2F, -3.0F, 5.4F, 2.0F, 6.0F),
                ModelTransform.NONE);
        body.addChild("left_talon",
                ModelPartBuilder.create().uv(78, 62).cuboid(1.2F, 3.7F, -3.0F, 1.4F, 1.8F, 2.0F),
                ModelTransform.NONE);
        body.addChild("right_talon",
                ModelPartBuilder.create().uv(84, 62).cuboid(-2.6F, 3.7F, -3.0F, 1.4F, 1.8F, 2.0F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-2.6F, -4.6F, -3.9F, 5.2F, 5.4F, 5.6F),
                ModelTransform.pivot(0.0F, 8.8F, -4.6F));
        head.addChild("beak",
                ModelPartBuilder.create().uv(26, 0).cuboid(-1.35F, -2.0F, -7.45F, 2.7F, 2.15F, 3.65F),
                ModelTransform.NONE);
        head.addChild("beak_tip",
                ModelPartBuilder.create().uv(40, 0).cuboid(-0.75F, -1.75F, -9.6F, 1.5F, 1.1F, 2.25F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(88, 0).cuboid(-2.0F, -3.2F, -4.2F, 0.85F, 0.85F, 0.55F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(94, 0).cuboid(1.15F, -3.2F, -4.2F, 0.85F, 0.85F, 0.55F),
                ModelTransform.NONE);
        head.addChild("left_brow",
                ModelPartBuilder.create().uv(80, 0).cuboid(-2.35F, -4.1F, -4.35F, 1.3F, 0.65F, 0.65F),
                ModelTransform.NONE);
        head.addChild("right_brow",
                ModelPartBuilder.create().uv(84, 0).cuboid(1.05F, -4.1F, -4.35F, 1.3F, 0.65F, 0.65F),
                ModelTransform.NONE);

        ModelPartData sunDisk = head.addChild("sun_disk",
                ModelPartBuilder.create()
                        .uv(44, 0).cuboid(-4.3F, -8.8F, -0.6F, 8.6F, 8.6F, 1.2F),
                ModelTransform.pivot(0.0F, -3.8F, 0.8F));
        sunDisk.addChild("sun_disk_top_ray",
                ModelPartBuilder.create().uv(66, 0).cuboid(-1.0F, -10.2F, -0.7F, 2.0F, 1.8F, 1.4F),
                ModelTransform.NONE);
        sunDisk.addChild("sun_disk_bottom_ray",
                ModelPartBuilder.create().uv(74, 0).cuboid(-1.0F, -0.2F, -0.7F, 2.0F, 1.8F, 1.4F),
                ModelTransform.NONE);
        sunDisk.addChild("sun_disk_left_ray",
                ModelPartBuilder.create().uv(88, 8).cuboid(-5.8F, -5.4F, -0.7F, 2.2F, 1.6F, 1.4F),
                ModelTransform.NONE);
        sunDisk.addChild("sun_disk_right_ray",
                ModelPartBuilder.create().uv(98, 8).cuboid(3.6F, -5.4F, -0.7F, 2.2F, 1.6F, 1.4F),
                ModelTransform.NONE);

        ModelPartData leftWing = root.addChild("left_wing",
                ModelPartBuilder.create()
                        .uv(0, 48).cuboid(0.0F, -1.0F, -3.0F, 11.0F, 1.4F, 8.0F)
                        .uv(34, 48).cuboid(8.0F, -0.2F, 3.0F, 8.0F, 1.2F, 5.0F),
                ModelTransform.of(3.5F, 11.0F, -1.6F, 0.0F, 0.0F, 0.2F));
        leftWing.addChild("left_primary_feather_a",
                ModelPartBuilder.create().uv(0, 60).cuboid(2.0F, 0.4F, 5.0F, 2.0F, 1.0F, 6.0F),
                ModelTransform.NONE);
        leftWing.addChild("left_primary_feather_b",
                ModelPartBuilder.create().uv(10, 60).cuboid(5.5F, 0.4F, 5.6F, 2.0F, 1.0F, 6.4F),
                ModelTransform.NONE);
        leftWing.addChild("left_primary_feather_c",
                ModelPartBuilder.create().uv(20, 60).cuboid(9.0F, 0.4F, 6.0F, 2.0F, 1.0F, 6.6F),
                ModelTransform.NONE);

        ModelPartData rightWing = root.addChild("right_wing",
                ModelPartBuilder.create()
                        .uv(0, 48).mirrored().cuboid(-11.0F, -1.0F, -3.0F, 11.0F, 1.4F, 8.0F)
                        .uv(34, 48).mirrored().cuboid(-16.0F, -0.2F, 3.0F, 8.0F, 1.2F, 5.0F),
                ModelTransform.of(-3.5F, 11.0F, -1.6F, 0.0F, 0.0F, -0.2F));
        rightWing.addChild("right_primary_feather_a",
                ModelPartBuilder.create().uv(38, 60).mirrored().cuboid(-4.0F, 0.4F, 5.0F, 2.0F, 1.0F, 6.0F),
                ModelTransform.NONE);
        rightWing.addChild("right_primary_feather_b",
                ModelPartBuilder.create().uv(48, 60).mirrored().cuboid(-7.5F, 0.4F, 5.6F, 2.0F, 1.0F, 6.4F),
                ModelTransform.NONE);
        rightWing.addChild("right_primary_feather_c",
                ModelPartBuilder.create().uv(58, 60).mirrored().cuboid(-11.0F, 0.4F, 6.0F, 2.0F, 1.0F, 6.6F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(72, 34).cuboid(-3.0F, -0.8F, 0.0F, 6.0F, 1.6F, 8.0F),
                ModelTransform.pivot(0.0F, 14.2F, 6.0F));
        tail.addChild("tail_tip",
                ModelPartBuilder.create().uv(92, 34).cuboid(-1.0F, -0.9F, 6.0F, 2.0F, 1.8F, 4.0F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(SunFalconEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.65F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.65F;
        this.sunDisk.pitch = -this.head.pitch * 0.25F;

        float flapSpeed = entity.isFlying() ? 0.75F : 0.28F;
        float flapAmount = entity.isFlying() ? 0.82F : 0.2F;
        float flap = MathHelper.sin(animationProgress * flapSpeed) * flapAmount;
        this.leftWing.roll = 0.18F + flap;
        this.rightWing.roll = -0.18F - flap;
        this.leftWing.pitch = -0.12F + MathHelper.cos(animationProgress * flapSpeed) * 0.14F;
        this.rightWing.pitch = this.leftWing.pitch;
        this.tail.pitch = 0.22F + MathHelper.sin(animationProgress * 0.12F) * 0.08F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.15F) * 0.1F;
        this.body.pitch = entity.handSwingProgress > 0.0F ? -0.28F : 0.0F;
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.4F) * 0.08F;
            this.head.roll = recoil;
            this.body.roll = recoil * 0.5F;
            this.leftWing.roll -= recoil;
            this.rightWing.roll -= recoil;
        } else {
            this.head.roll = 0.0F;
            this.body.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.0F * fall;
            this.head.pitch += 0.22F * fall;
            this.leftWing.roll = -0.8F * fall;
            this.rightWing.roll = 0.8F * fall;
            this.tail.pitch -= 0.28F * fall;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
