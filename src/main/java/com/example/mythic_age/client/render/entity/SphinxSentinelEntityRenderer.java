package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.SphinxSentinelModel;
import com.example.mythic_age.entity.SphinxSentinelEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SphinxSentinelEntityRenderer extends MobEntityRenderer<SphinxSentinelEntity, SphinxSentinelModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/sphinx_sentinel/sphinx_sentinel.png");

    public SphinxSentinelEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SphinxSentinelModel(context.getPart(ModModelLayers.SPHINX_SENTINEL)), 0.72F);
    }

    @Override
    protected void scale(SphinxSentinelEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(SphinxSentinelEntity entity) {
        return TEXTURE;
    }
}
