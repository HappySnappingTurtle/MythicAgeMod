package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.TempleGuardianModel;
import com.example.mythic_age.entity.TempleGuardianEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/**
 * 神殿守护者实体渲染器
 * 
 * 使用共享的神殿守护者模型，并根据文明类型切换贴图。
 */
public class TempleGuardianEntityRenderer extends MobEntityRenderer<TempleGuardianEntity, TempleGuardianModel<TempleGuardianEntity>> {

    public TempleGuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new TempleGuardianModel<>(context.getPart(ModModelLayers.TEMPLE_GUARDIAN)), 0.55F);
    }

    @Override
    public Identifier getTexture(TempleGuardianEntity entity) {
        try {
            return TempleGuardianTextures.forType(entity.getMythologyType());
        } catch (Exception e) {
            MythicAgeMod.LOGGER.warn("Failed to resolve temple guardian texture for {}: {}", entity.getMythologyType(), e.getMessage());
            return TempleGuardianTextures.forType(com.example.mythic_age.item.GodPowerCertificateItem.MythologyType.GREEK);
        }
    }
}
