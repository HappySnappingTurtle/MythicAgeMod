package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.FenrirWolfModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.FenrirWolfEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class FenrirWolfEntityRenderer extends MobEntityRenderer<FenrirWolfEntity, FenrirWolfModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/fenrir_wolf/fenrir_wolf.png");

    public FenrirWolfEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new FenrirWolfModel(context.getPart(ModModelLayers.FENRIR_WOLF)), 0.78F);
    }

    @Override
    protected void scale(FenrirWolfEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(FenrirWolfEntity entity) {
        return TEXTURE;
    }
}
