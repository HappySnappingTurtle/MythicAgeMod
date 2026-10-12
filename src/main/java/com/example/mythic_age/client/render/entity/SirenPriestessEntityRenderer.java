package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.SirenPriestessModel;
import com.example.mythic_age.entity.SirenPriestessEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SirenPriestessEntityRenderer extends MobEntityRenderer<SirenPriestessEntity, SirenPriestessModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/siren_priestess/siren_priestess.png");

    public SirenPriestessEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SirenPriestessModel(context.getPart(ModModelLayers.SIREN_PRIESTESS)), 0.42F);
    }

    @Override
    protected void scale(SirenPriestessEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.04F, 1.04F, 1.04F);
    }

    @Override
    public Identifier getTexture(SirenPriestessEntity entity) {
        return TEXTURE;
    }
}
