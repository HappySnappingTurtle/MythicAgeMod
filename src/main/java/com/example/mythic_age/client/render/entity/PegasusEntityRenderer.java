package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.PegasusModel;
import com.example.mythic_age.entity.PegasusEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class PegasusEntityRenderer extends MobEntityRenderer<PegasusEntity, PegasusModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/pegasus/pegasus.png");

    public PegasusEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new PegasusModel(context.getPart(ModModelLayers.PEGASUS)), 0.7F);
    }

    @Override
    protected void scale(PegasusEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.18F, 1.18F, 1.18F);
    }

    @Override
    public Identifier getTexture(PegasusEntity entity) {
        return TEXTURE;
    }
}
