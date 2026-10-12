package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.FrostGiantModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.FrostGiantEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class FrostGiantEntityRenderer extends MobEntityRenderer<FrostGiantEntity, FrostGiantModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/frost_giant/frost_giant.png");

    public FrostGiantEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new FrostGiantModel(context.getPart(ModModelLayers.FROST_GIANT)), 0.88F);
    }

    @Override
    protected void scale(FrostGiantEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(FrostGiantEntity entity) {
        return TEXTURE;
    }
}
