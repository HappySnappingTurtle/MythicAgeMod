package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.NineTailedFoxModel;
import com.example.mythic_age.entity.NineTailedFoxEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class NineTailedFoxEntityRenderer extends MobEntityRenderer<NineTailedFoxEntity, NineTailedFoxModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/nine_tailed_fox/nine_tailed_fox.png");

    public NineTailedFoxEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new NineTailedFoxModel(context.getPart(ModModelLayers.NINE_TAILED_FOX)), 0.5F);
    }

    @Override
    protected void scale(NineTailedFoxEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.02F, 1.02F, 1.02F);
    }

    @Override
    public Identifier getTexture(NineTailedFoxEntity entity) {
        return TEXTURE;
    }
}
