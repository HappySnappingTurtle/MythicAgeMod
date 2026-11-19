package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.content.greek.cyclops.WineBowl;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 地上的酒碗（T25a ⑦）：按实体朝向平放着画 3D 碗模型（{@code models/item/wine_bowl.json}）。
 * 原版投掷物渲染器会让模型始终正对镜头，平面图标没问题，立体的碗从上往下看就成了一条细线。
 */
public class WineBowlRenderer extends EntityRenderer<WineBowl> {
    private final ItemRenderer items;

    public WineBowlRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.items = ctx.getItemRenderer();
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(WineBowl bowl, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // 物品模型以方块中心为原点：抬半格，碗底正好落在地面
        pose.translate(0, 0.5, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-bowl.getYRot()));
        items.renderStatic(bowl.getItem(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, bowl.level(),
                bowl.getId());
        pose.popPose();
        super.render(bowl, yaw, partialTick, pose, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(WineBowl bowl) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
