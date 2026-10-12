package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ColossusModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.ColossusEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ColossusEntityRenderer extends MobEntityRenderer<ColossusEntity, ColossusModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/colossus/colossus.png");

    public ColossusEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ColossusModel(context.getPart(ModModelLayers.COLOSSUS)), 1.35F);
    }

    @Override
    protected void scale(ColossusEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.15F, 1.15F, 1.15F);
    }

    @Override
    public Identifier getTexture(ColossusEntity entity) {
        return TEXTURE;
    }
}
