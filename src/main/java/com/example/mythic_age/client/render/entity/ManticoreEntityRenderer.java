package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ManticoreModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.ManticoreEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ManticoreEntityRenderer extends MobEntityRenderer<ManticoreEntity, ManticoreModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/manticore/manticore.png");

    public ManticoreEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ManticoreModel(context.getPart(ModModelLayers.MANTICORE)), 0.75F);
    }

    @Override
    protected void scale(ManticoreEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.04F, 1.04F, 1.04F);
    }

    @Override
    public Identifier getTexture(ManticoreEntity entity) {
        return TEXTURE;
    }
}
