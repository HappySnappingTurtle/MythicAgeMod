package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.MinotaurModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.MinotaurEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class MinotaurEntityRenderer extends MobEntityRenderer<MinotaurEntity, MinotaurModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/minotaur/minotaur.png");

    public MinotaurEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MinotaurModel(context.getPart(ModModelLayers.MINOTAUR)), 0.95f);
    }

    @Override
    public Identifier getTexture(MinotaurEntity entity) {
        return TEXTURE;
    }
}
