package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ChimeraModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.ChimeraEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ChimeraEntityRenderer extends MobEntityRenderer<ChimeraEntity, ChimeraModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/chimera/chimera.png");

    public ChimeraEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ChimeraModel(context.getPart(ModModelLayers.CHIMERA)), 0.9F);
    }

    @Override
    protected void scale(ChimeraEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(ChimeraEntity entity) {
        return TEXTURE;
    }
}
