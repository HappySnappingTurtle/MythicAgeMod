package io.github.happysnappingturtle.mythicage.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** 本模组生物的渲染器：按实体变体缩放模型与阴影（Boss 版放大 1.4 倍），可选自发光层。 */
public class MythGeoRenderer<T extends Entity & GeoEntity & MythVariantHolder> extends GeoEntityRenderer<T> {
    private final float baseShadow;

    public MythGeoRenderer(EntityRendererProvider.Context ctx, MythGeoModel<T> model, float collisionWidth) {
        super(ctx, model);
        this.baseShadow = collisionWidth * 0.5F;
        this.shadowRadius = baseShadow;
    }

    /** 启用自发光层：读取与贴图同名的 {@code _glowmask.png}（如红眼），不受环境光影响。 */
    public MythGeoRenderer<T> glowing() {
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        return this;
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, T animatable, BakedGeoModel model,
                                    boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float s = animatable.variantScale();
        super.scaleModelForRender(widthScale * s, heightScale * s, poseStack, animatable, model, isReRender, partialTick, packedLight, packedOverlay);
    }

    @Override
    protected float getShadowRadius(T entity) {
        return baseShadow * entity.variantScale();
    }
}
