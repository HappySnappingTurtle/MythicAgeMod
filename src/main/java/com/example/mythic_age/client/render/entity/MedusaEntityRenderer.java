package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.MedusaModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.MedusaEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class MedusaEntityRenderer extends MobEntityRenderer<MedusaEntity, MedusaModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/medusa/medusa.png");

    public MedusaEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MedusaModel(context.getPart(ModModelLayers.MEDUSA)), 0.55F);
    }

    @Override
    public Identifier getTexture(MedusaEntity entity) {
        return TEXTURE;
    }
}
