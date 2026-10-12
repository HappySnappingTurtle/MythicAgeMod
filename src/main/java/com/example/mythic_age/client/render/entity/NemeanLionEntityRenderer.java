package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.NemeanLionModel;
import com.example.mythic_age.entity.NemeanLionEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class NemeanLionEntityRenderer extends MobEntityRenderer<NemeanLionEntity, NemeanLionModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/nemean_lion/nemean_lion.png");

    public NemeanLionEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new NemeanLionModel(context.getPart(ModModelLayers.NEMEAN_LION)), 0.72F);
    }

    @Override
    protected void scale(NemeanLionEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(NemeanLionEntity entity) {
        return TEXTURE;
    }
}
