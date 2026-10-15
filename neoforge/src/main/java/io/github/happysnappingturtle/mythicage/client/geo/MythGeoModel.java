package io.github.happysnappingturtle.mythicage.client.geo;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 本模组生物的 GeckoLib 模型（T7 设计文档 §6.4）。资源路径：
 * <pre>
 * geo/entity/&lt;名&gt;.geo.json
 * animations/entity/&lt;名&gt;.animation.json
 * textures/entity/&lt;名&gt;/&lt;名&gt;.png            基础外观
 * textures/entity/&lt;名&gt;/&lt;名&gt;_&lt;变体&gt;.png    变体（缺失时退回基础外观）
 * </pre>
 * 头部骨骼 {@code head} 叠加实体的视线方向（机制动作期间不叠加，见 {@link MythVariantHolder#headLookLocked()}）；{@code hiddenBones} 中的骨骼默认隐藏（如只在投石时显示的石块）。
 */
public class MythGeoModel<T extends GeoAnimatable & MythVariantHolder> extends GeoModel<T> {
    private final String name;
    private final ResourceLocation geo;
    private final ResourceLocation animation;
    private final ResourceLocation baseTexture;
    private final Set<String> hiddenBones;
    private final Map<String, ResourceLocation> variantTextures = new HashMap<>();

    public MythGeoModel(String name, Set<String> hiddenBones) {
        this.name = name;
        this.geo = id("geo/entity/" + name + ".geo.json");
        this.animation = id("animations/entity/" + name + ".animation.json");
        this.baseTexture = id("textures/entity/" + name + "/" + name + ".png");
        this.hiddenBones = Set.copyOf(hiddenBones);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return geo;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animation;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String variant = animatable.variant();
        if (variant.isEmpty()) {
            return baseTexture;
        }
        return variantTextures.computeIfAbsent(variant, v -> {
            ResourceLocation tex = id("textures/entity/" + name + "/" + name + "_" + v + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent() ? tex : baseTexture;
        });
    }

    /** 资源重载后重新判断变体贴图是否存在。 */
    public void clearTextureCache() {
        variantTextures.clear();
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> state) {
        for (String bone : hiddenBones) {
            getBone(bone).ifPresent(b -> b.setHidden(true));
        }
        EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (look != null && !animatable.headLookLocked()) {
            getBone("head").ifPresent(head -> {
                head.setRotX(head.getRotX() + look.headPitch() * Mth.DEG_TO_RAD * 0.6F);
                head.setRotY(head.getRotY() + look.netHeadYaw() * Mth.DEG_TO_RAD * 0.6F);
            });
        }
    }
}
