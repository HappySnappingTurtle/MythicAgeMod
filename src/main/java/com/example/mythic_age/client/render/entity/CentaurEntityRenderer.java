package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.CentaurModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.CentaurEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class CentaurEntityRenderer extends MobEntityRenderer<CentaurEntity, CentaurModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/centaur/centaur.png");

    public CentaurEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new CentaurModel(context.getPart(ModModelLayers.CENTAUR)), 0.75F);
    }

    @Override
    public Identifier getTexture(CentaurEntity entity) {
        return TEXTURE;
    }
}
