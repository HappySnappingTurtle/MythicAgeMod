package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.AzureDragonModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.AzureDragonEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class AzureDragonEntityRenderer extends MobEntityRenderer<AzureDragonEntity, AzureDragonModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/azure_dragon/azure_dragon.png");

    public AzureDragonEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new AzureDragonModel(context.getPart(ModModelLayers.AZURE_DRAGON)), 0.62F);
    }

    @Override
    protected void scale(AzureDragonEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(AzureDragonEntity entity) {
        return TEXTURE;
    }
}
