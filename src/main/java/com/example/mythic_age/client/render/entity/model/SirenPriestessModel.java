package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.SirenPriestessEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class SirenPriestessModel extends SinglePartEntityModel<SirenPriestessEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart crown;
    private final ModelPart leftHairFin;
    private final ModelPart rightHairFin;
    private final ModelPart body;
    private final ModelPart waveChest;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart staff;
    private final ModelPart tailBase;
    private final ModelPart tailTip;
    private final ModelPart tailFin;

    public SirenPriestessModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.crown = this.head.getChild("fin_crown");
        this.leftHairFin = this.head.getChild("left_hair_fin");
        this.rightHairFin = this.head.getChild("right_hair_fin");
        this.body = root.getChild("body");
        this.waveChest = this.body.getChild("wave_chest_ornament");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.staff = this.leftArm.getChild("crystal_staff");
        this.tailBase = root.getChild("tail_base");
        this.tailTip = this.tailBase.getChild("tail_tip");
        this.tailFin = this.tailTip.getChild("tail_fin");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 26).cuboid(-3.9F, -10.4F, -2.4F, 7.8F, 11.6F, 4.8F),
                ModelTransform.pivot(0.0F, 10.5F, 0.0F));

        body.addChild("wave_chest_ornament",
                ModelPartBuilder.create()
                        .uv(26, 26).cuboid(-3.9F, -8.8F, -2.8F, 7.8F, 5.0F, 1.2F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        body.addChild("waist_shell_belt",
                ModelPartBuilder.create().uv(48, 26).cuboid(-2.4F, -3.0F, -2.9F, 4.8F, 2.6F, 1.2F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-2.85F, -6.6F, -2.9F, 5.7F, 6.6F, 5.8F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));
        head.addChild("hair_back_panel",
                ModelPartBuilder.create().uv(62, 0).cuboid(-2.5F, -4.8F, 2.35F, 5.0F, 5.9F, 1.0F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(28, 0).cuboid(-1.95F, -4.25F, -3.35F, 1.05F, 1.05F, 0.75F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(34, 0).cuboid(0.9F, -4.25F, -3.35F, 1.05F, 1.05F, 0.75F),
                ModelTransform.NONE);
        head.addChild("mouth_line",
                ModelPartBuilder.create().uv(40, 0).cuboid(-1.35F, -2.15F, -3.45F, 2.7F, 0.65F, 0.75F),
                ModelTransform.NONE);

        ModelPartData crown = head.addChild("fin_crown",
                ModelPartBuilder.create()
                        .uv(42, 0).cuboid(-4.0F, -5.4F, -1.0F, 8.0F, 5.5F, 2.0F)
                        .uv(66, 0).cuboid(-1.1F, -8.4F, -0.8F, 2.2F, 3.0F, 1.6F),
                ModelTransform.pivot(0.0F, -6.8F, 0.0F));

        crown.addChild("crown_spire",
                ModelPartBuilder.create().uv(118, 0).cuboid(-0.7F, -10.4F, -0.7F, 1.4F, 2.4F, 1.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        head.addChild("left_hair_fin",
                ModelPartBuilder.create().uv(76, 0).cuboid(0.0F, -4.8F, -0.6F, 2.1F, 8.0F, 1.2F),
                ModelTransform.of(2.55F, -4.2F, 2.35F, 0.08F, -0.18F, 0.18F));
        head.addChild("right_hair_fin",
                ModelPartBuilder.create().uv(86, 0).cuboid(-2.1F, -4.8F, -0.6F, 2.1F, 8.0F, 1.2F),
                ModelTransform.of(-2.55F, -4.2F, 2.35F, 0.08F, 0.18F, -0.18F));
        head.addChild("left_ear_fin",
                ModelPartBuilder.create().uv(96, 12).cuboid(0.0F, -2.4F, -0.45F, 1.55F, 4.8F, 0.9F),
                ModelTransform.of(2.55F, -3.6F, -0.35F, 0.0F, -0.15F, 0.18F));
        head.addChild("right_ear_fin",
                ModelPartBuilder.create().uv(104, 12).mirrored().cuboid(-1.55F, -2.4F, -0.45F, 1.55F, 4.8F, 0.9F),
                ModelTransform.of(-2.55F, -3.6F, -0.35F, 0.0F, 0.15F, -0.18F));

        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(62, 14).cuboid(-0.4F, -1.0F, -1.2F, 2.2F, 11.0F, 2.4F),
                ModelTransform.of(3.1F, 1.5F, 0.0F, -0.14F, 0.0F, -0.18F));
        leftArm.addChild("left_wrist_fin",
                ModelPartBuilder.create().uv(112, 12).cuboid(0.9F, 5.7F, -1.5F, 1.1F, 3.4F, 3.0F),
                ModelTransform.NONE);
        ModelPartData staff = leftArm.addChild("crystal_staff",
                ModelPartBuilder.create()
                        .uv(90, 0).cuboid(-0.6F, -11.5F, -0.6F, 1.2F, 21.0F, 1.2F),
                ModelTransform.of(2.4F, 4.0F, -0.2F, 0.0F, 0.0F, 0.08F));
        staff.addChild("staff_crystal",
                ModelPartBuilder.create().uv(104, 0).cuboid(-1.7F, -15.5F, -1.7F, 3.4F, 4.2F, 3.4F),
                ModelTransform.NONE);
        staff.addChild("staff_top_spire",
                ModelPartBuilder.create().uv(118, 0).cuboid(-0.8F, -17.3F, -0.8F, 1.6F, 2.0F, 1.6F),
                ModelTransform.NONE);
        staff.addChild("staff_rune_band",
                ModelPartBuilder.create().uv(90, 22).cuboid(-1.0F, -7.5F, -1.0F, 2.0F, 1.2F, 2.0F),
                ModelTransform.NONE);

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(76, 14).mirrored().cuboid(-1.8F, -1.0F, -1.2F, 2.2F, 11.0F, 2.4F),
                ModelTransform.of(-3.1F, 1.5F, 0.0F, -0.14F, 0.0F, 0.18F));
        rightArm.addChild("right_wrist_fin",
                ModelPartBuilder.create().uv(112, 20).mirrored().cuboid(-2.0F, 5.7F, -1.5F, 1.1F, 3.4F, 3.0F),
                ModelTransform.NONE);

        ModelPartData tailBase = root.addChild("tail_base",
                ModelPartBuilder.create()
                        .uv(26, 34).cuboid(-3.2F, -0.4F, -2.2F, 6.4F, 9.0F, 4.4F),
                ModelTransform.pivot(0.0F, 11.0F, 0.0F));
        tailBase.addChild("tail_scale_band_1",
                ModelPartBuilder.create().uv(0, 54).cuboid(-3.0F, 2.0F, -2.8F, 6.0F, 1.2F, 1.0F),
                ModelTransform.NONE);
        tailBase.addChild("tail_scale_band_2",
                ModelPartBuilder.create().uv(20, 54).cuboid(-2.8F, 5.1F, -2.8F, 5.6F, 1.2F, 1.0F),
                ModelTransform.NONE);
        tailBase.addChild("tail_scale_band_3",
                ModelPartBuilder.create().uv(58, 54).cuboid(-2.6F, 7.2F, -2.6F, 5.2F, 1.1F, 0.9F),
                ModelTransform.NONE);
        ModelPartData tailTip = tailBase.addChild("tail_tip",
                ModelPartBuilder.create()
                        .uv(52, 34).cuboid(-2.2F, 0.0F, -1.8F, 4.4F, 10.0F, 3.6F),
                ModelTransform.of(0.0F, 8.2F, 0.0F, 0.18F, 0.0F, 0.0F));
        tailTip.addChild("tail_tip_scale_band",
                ModelPartBuilder.create().uv(40, 54).cuboid(-2.0F, 3.4F, -2.3F, 4.0F, 1.1F, 0.9F),
                ModelTransform.NONE);
        ModelPartData tailFin = tailTip.addChild("tail_fin",
                ModelPartBuilder.create()
                        .uv(74, 34).cuboid(-6.2F, -0.4F, -1.2F, 12.4F, 4.2F, 2.4F),
                ModelTransform.of(0.0F, 9.1F, 0.0F, 0.12F, 0.0F, 0.0F));
        tailFin.addChild("tail_fin_left_edge",
                ModelPartBuilder.create().uv(96, 44).cuboid(5.5F, -0.2F, -1.4F, 1.1F, 3.6F, 2.8F),
                ModelTransform.NONE);
        tailFin.addChild("tail_fin_right_edge",
                ModelPartBuilder.create().uv(106, 44).mirrored().cuboid(-6.6F, -0.2F, -1.4F, 1.1F, 3.6F, 2.8F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(SirenPriestessEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.65F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.65F;
        this.crown.pitch = -this.head.pitch * 0.18F + MathHelper.sin(animationProgress * 0.08F) * 0.035F;
        this.leftHairFin.roll = 0.18F + MathHelper.sin(animationProgress * 0.11F) * 0.05F;
        this.rightHairFin.roll = -0.18F - MathHelper.sin(animationProgress * 0.11F) * 0.05F;

        float drift = entity.isDrifting() ? 0.16F : 0.08F;
        this.body.pitch = MathHelper.sin(animationProgress * drift) * 0.035F;
        this.waveChest.pitch = this.body.pitch * 0.35F;
        this.tailBase.pitch = 0.08F + MathHelper.sin(animationProgress * 0.14F) * 0.12F;
        this.tailTip.pitch = 0.18F + MathHelper.sin(animationProgress * 0.14F + 0.8F) * 0.16F;
        this.tailFin.roll = MathHelper.sin(animationProgress * 0.17F) * 0.18F;

        this.leftArm.pitch = -0.18F + MathHelper.sin(animationProgress * 0.12F) * 0.05F;
        this.rightArm.pitch = -0.16F + MathHelper.sin(animationProgress * 0.12F + MathHelper.PI) * 0.04F;
        this.staff.pitch = -0.08F;
        if (entity.handSwingProgress > 0.0F) {
            float cast = MathHelper.sin(entity.handSwingProgress * MathHelper.PI);
            this.leftArm.pitch = -1.45F + cast * 0.28F;
            this.staff.pitch = -0.55F;
            this.body.pitch -= 0.08F * cast;
        }

        if (entity.hurtTime > 0) {
            this.head.roll = MathHelper.sin(animationProgress * 2.0F) * 0.08F;
        } else {
            this.head.roll = 0.0F;
        }
        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 0.95F * fall;
            this.head.pitch += 0.25F * fall;
            this.leftArm.pitch = -0.25F * fall;
            this.rightArm.pitch = -0.25F * fall;
            this.tailBase.pitch -= 0.35F * fall;
            this.tailFin.roll *= 1.0F - fall * 0.6F;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
