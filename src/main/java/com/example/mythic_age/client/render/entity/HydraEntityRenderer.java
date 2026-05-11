package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.HydraModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.HydraEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class HydraEntityRenderer extends MobEntityRenderer<HydraEntity, HydraModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/hydra/hydra.png");

    public HydraEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new HydraModel(context.getPart(ModModelLayers.HYDRA)), 1.15F);
    }

    @Override
    public Identifier getTexture(HydraEntity entity) {
        return TEXTURE;
    }
}
