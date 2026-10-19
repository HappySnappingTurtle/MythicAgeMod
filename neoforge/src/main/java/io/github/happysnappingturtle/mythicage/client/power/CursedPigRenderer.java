package io.github.happysnappingturtle.mythicage.client.power;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.happysnappingturtle.mythicage.content.greek.power.CursedPig;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 被诅咒的猪：原版猪模型 + 玫瑰粉金斑贴图，头顶常驻一枚上下浮动的金苹果印记（朝向镜头），一眼认出不是普通猪。
 */
public class CursedPigRenderer extends MobRenderer<CursedPig, PigModel<CursedPig>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID,
            "textures/entity/cursed_pig.png");
    static final ResourceLocation MARK = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/item/gp_gr_curse.png");

    public CursedPigRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PigModel<>(ctx.bakeLayer(ModelLayers.PIG)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(CursedPig pig) {
        return TEXTURE;
    }

    @Override
    public void render(CursedPig pig, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(pig, yaw, pt, pose, buffers, light);
        float t = pig.tickCount + pt;
        pose.pushPose();
        pose.translate(0, pig.getBbHeight() + 0.45 + Mth.sin(t * 0.12f) * 0.06, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float s = 0.22f;
        pose.scale(s, s, s);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(MARK));
        PoseStack.Pose last = pose.last();
        float[][] q = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (float[] v : q) {
            vc.addVertex(last, v[0], v[1], 0).setColor(0xF0FFFFFF).setUv(v[2], v[3]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT).setNormal(last, 0, 0, 1);
        }
        pose.popPose();
    }
}
