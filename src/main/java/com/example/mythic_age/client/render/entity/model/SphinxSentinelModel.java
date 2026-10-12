package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.entity.SphinxSentinelEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class SphinxSentinelModel extends SinglePartEntityModel<SphinxSentinelEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart headdress;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public SphinxSentinelModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.headdress = this.head.getChild("headdress");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.backLeftLeg = root.getChild("back_left_leg");
        this.backRightLeg = root.getChild("back_right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create()
                        .uv(0, 34).cuboid(-6.5F, -7.0F, -13.0F, 13.0F, 9.5F, 25.0F)
                        .uv(76, 38).cuboid(-5.6F, -8.1F, -10.0F, 11.2F, 2.0F, 18.0F)
                        .uv(76, 60).cuboid(-7.2F, -5.8F, -12.8F, 14.4F, 5.8F, 8.0F),
                ModelTransform.pivot(0.0F, 14.2F, 2.2F));
        body.addChild("chest",
                ModelPartBuilder.create().uv(76, 60).cuboid(-7.2F, -5.8F, -12.8F, 14.4F, 5.8F, 8.0F),
                ModelTransform.NONE);
        body.addChild("shoulder_scarab",
                ModelPartBuilder.create().uv(116, 20).cuboid(-2.2F, -7.4F, -13.0F, 4.4F, 3.2F, 1.0F),
                ModelTransform.NONE);

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.1F, -7.0F, -3.8F, 8.2F, 8.2F, 6.5F),
                ModelTransform.pivot(0.0F, 7.0F, -11.6F));
        head.addChild("human_face",
                ModelPartBuilder.create().uv(34, 0).cuboid(-3.0F, -2.4F, -4.4F, 6.0F, 2.0F, 1.0F),
                ModelTransform.NONE);
        head.addChild("left_eye",
                ModelPartBuilder.create().uv(92, 0).cuboid(-2.8F, -4.5F, -4.2F, 1.0F, 1.0F, 0.8F),
                ModelTransform.NONE);
        head.addChild("right_eye",
                ModelPartBuilder.create().uv(98, 0).cuboid(1.8F, -4.5F, -4.2F, 1.0F, 1.0F, 0.8F),
                ModelTransform.NONE);
        head.addChild("left_pupil",
                ModelPartBuilder.create().uv(116, 0).cuboid(-2.35F, -4.1F, -4.65F, 0.45F, 0.45F, 0.5F),
                ModelTransform.NONE);
        head.addChild("right_pupil",
                ModelPartBuilder.create().uv(120, 0).cuboid(1.9F, -4.1F, -4.65F, 0.45F, 0.45F, 0.5F),
                ModelTransform.NONE);
        head.addChild("nose_bridge",
                ModelPartBuilder.create()
                        .uv(104, 0).cuboid(-0.8F, -3.4F, -4.8F, 1.6F, 2.0F, 0.8F),
                ModelTransform.NONE);
        head.addChild("mouth_line",
                ModelPartBuilder.create()
                        .uv(112, 0).cuboid(-1.8F, -1.0F, -4.9F, 3.6F, 0.6F, 0.7F),
                ModelTransform.NONE);
        head.addChild("chin_beard",
                ModelPartBuilder.create().uv(116, 8).cuboid(-1.0F, -0.2F, -4.8F, 2.0F, 3.1F, 1.0F),
                ModelTransform.NONE);

        ModelPartData headdress = head.addChild("headdress",
                ModelPartBuilder.create()
                        .uv(52, 0).cuboid(-5.8F, -8.5F, -3.6F, 11.6F, 3.2F, 6.4F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        headdress.addChild("headdress_left_panel",
                ModelPartBuilder.create().uv(52, 12).cuboid(-5.6F, -5.4F, -2.8F, 2.2F, 7.8F, 5.8F),
                ModelTransform.NONE);
        headdress.addChild("headdress_right_panel",
                ModelPartBuilder.create().uv(66, 12).cuboid(3.4F, -5.4F, -2.8F, 2.2F, 7.8F, 5.8F),
                ModelTransform.NONE);
        headdress.addChild("headdress_top_band",
                ModelPartBuilder.create().uv(52, 0).cuboid(-5.8F, -8.5F, -3.6F, 11.6F, 3.2F, 6.4F),
                ModelTransform.NONE);
        headdress.addChild("headdress_brow",
                ModelPartBuilder.create().uv(84, 12).cuboid(-3.2F, -8.8F, -4.4F, 6.4F, 1.4F, 1.0F),
                ModelTransform.NONE);

        ModelPartData tail = root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(80, 74).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 12.0F),
                ModelTransform.pivot(0.0F, 11.8F, 14.0F));
        tail.addChild("tail_tuft",
                ModelPartBuilder.create().uv(100, 74).cuboid(-2.2F, -2.0F, 10.0F, 4.4F, 4.0F, 4.4F),
                ModelTransform.NONE);

        ModelPartData frontLeftLeg = root.addChild("front_left_leg",
                ModelPartBuilder.create()
                        .uv(0, 72).cuboid(-2.4F, 0.0F, -2.2F, 4.8F, 10.0F, 4.4F)
                        .uv(38, 72).cuboid(-3.1F, 8.6F, -4.0F, 6.2F, 1.8F, 3.0F),
                ModelTransform.pivot(4.1F, 14.0F, -8.0F));
        frontLeftLeg.addChild("front_left_paw",
                ModelPartBuilder.create().uv(0, 72).cuboid(-3.2F, 8.6F, -4.0F, 6.4F, 2.0F, 3.2F),
                ModelTransform.NONE);
        frontLeftLeg.addChild("front_left_claws",
                ModelPartBuilder.create().uv(0, 82).cuboid(-3.0F, 10.4F, -4.9F, 6.0F, 1.1F, 1.2F),
                ModelTransform.NONE);
        ModelPartData frontRightLeg = root.addChild("front_right_leg",
                ModelPartBuilder.create()
                        .uv(0, 72).mirrored().cuboid(-2.4F, 0.0F, -2.2F, 4.8F, 10.0F, 4.4F)
                        .uv(38, 72).mirrored().cuboid(-3.1F, 8.6F, -4.0F, 6.2F, 1.8F, 3.0F),
                ModelTransform.pivot(-4.1F, 14.0F, -8.0F));
        frontRightLeg.addChild("front_right_paw",
                ModelPartBuilder.create().uv(18, 72).cuboid(-3.2F, 8.6F, -4.0F, 6.4F, 2.0F, 3.2F),
                ModelTransform.NONE);
        frontRightLeg.addChild("front_right_claws",
                ModelPartBuilder.create().uv(20, 82).cuboid(-3.0F, 10.4F, -4.9F, 6.0F, 1.1F, 1.2F),
                ModelTransform.NONE);
        ModelPartData backLeftLeg = root.addChild("back_left_leg",
                ModelPartBuilder.create()
                        .uv(20, 72).cuboid(-2.1F, 0.0F, -2.0F, 4.2F, 10.0F, 4.0F)
                        .uv(58, 72).cuboid(-2.5F, 8.7F, -3.1F, 5.0F, 1.6F, 2.6F),
                ModelTransform.pivot(4.2F, 14.0F, 7.6F));
        backLeftLeg.addChild("back_left_paw",
                ModelPartBuilder.create().uv(36, 72).cuboid(-2.6F, 8.7F, -3.1F, 5.2F, 1.8F, 2.8F),
                ModelTransform.NONE);
        ModelPartData backRightLeg = root.addChild("back_right_leg",
                ModelPartBuilder.create()
                        .uv(20, 72).mirrored().cuboid(-2.1F, 0.0F, -2.0F, 4.2F, 10.0F, 4.0F)
                        .uv(58, 72).mirrored().cuboid(-2.5F, 8.7F, -3.1F, 5.0F, 1.6F, 2.6F),
                ModelTransform.pivot(-4.2F, 14.0F, 7.6F));
        backRightLeg.addChild("back_right_paw",
                ModelPartBuilder.create().uv(54, 72).cuboid(-2.6F, 8.7F, -3.1F, 5.2F, 1.8F, 2.8F),
                ModelTransform.NONE);

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(SphinxSentinelEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * (MathHelper.PI / 180.0F) * 0.75F;
        this.head.yaw = headYaw * (MathHelper.PI / 180.0F) * 0.65F;
        this.headdress.yaw = this.head.yaw * 0.25F;

        this.frontLeftLeg.pitch = MathHelper.cos(limbAngle * 0.68F) * 0.8F * limbDistance;
        this.backRightLeg.pitch = this.frontLeftLeg.pitch;
        this.frontRightLeg.pitch = MathHelper.cos(limbAngle * 0.68F + MathHelper.PI) * 0.8F * limbDistance;
        this.backLeftLeg.pitch = this.frontRightLeg.pitch;
        this.tail.pitch = 0.18F + MathHelper.sin(animationProgress * 0.11F) * 0.08F;
        this.tail.yaw = MathHelper.sin(animationProgress * 0.15F) * 0.12F;
        this.body.pitch = entity.handSwingProgress > 0.0F ? -0.18F * MathHelper.sin(entity.handSwingProgress * MathHelper.PI) : 0.0F;
        if (entity.hurtTime > 0) {
            float recoil = MathHelper.sin(animationProgress * 2.0F) * 0.06F;
            this.head.roll = recoil;
            this.headdress.roll = -recoil * 0.6F;
            this.tail.yaw -= recoil;
        } else {
            this.head.roll = 0.0F;
            this.headdress.roll = 0.0F;
        }

        if (entity.deathTime > 0) {
            float fall = Math.min(1.0F, entity.deathTime / 20.0F);
            this.body.roll = 1.05F * fall;
            this.head.pitch += 0.25F * fall;
        } else {
            this.body.roll = 0.0F;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
