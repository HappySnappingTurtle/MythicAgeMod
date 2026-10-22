package io.github.happysnappingturtle.mythicage.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.happysnappingturtle.mythicage.summon.greek.SummonPegasusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** 天马的眼睛金光（_glowmask）：只在放出天眼后的 1 秒里画（T14 §11.4），平时不发光。 */
public class SkyEyeGlowLayer extends AutoGlowingGeoLayer<SummonPegasusEntity> {
    public SkyEyeGlowLayer(GeoRenderer<SummonPegasusEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, SummonPegasusEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.eyeGlowing()) {
            super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        }
    }
}
