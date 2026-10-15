package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.guardian.GreekGuardianEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * 祭司的占位渲染（T9a）：借用原版村民模型 + 牧师长袍贴图，直到 T9b 的 GeckoLib 祭司模型完成。
 * 只为能在实机里看见、点中这个实体，不代表最终外观。
 */
public class GuardianPlaceholderRenderer extends MobRenderer<GreekGuardianEntity, VillagerModel<GreekGuardianEntity>> {
    private static final ResourceLocation BASE = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    private static final ResourceLocation ROBE = ResourceLocation.withDefaultNamespace("textures/entity/villager/profession/cleric.png");

    public GuardianPlaceholderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerModel<>(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        addLayer(new RobeLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(GreekGuardianEntity entity) {
        return BASE;
    }

    private static final class RobeLayer extends RenderLayer<GreekGuardianEntity, VillagerModel<GreekGuardianEntity>> {
        RobeLayer(RenderLayerParent<GreekGuardianEntity, VillagerModel<GreekGuardianEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, GreekGuardianEntity entity, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) {
                return;
            }
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ROBE)), light,
                    LivingEntityRenderer.getOverlayCoords(entity, 0), -1);
        }
    }
}
