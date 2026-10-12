package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.SunFalconModel;
import com.example.mythic_age.entity.SunFalconEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SunFalconEntityRenderer extends MobEntityRenderer<SunFalconEntity, SunFalconModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/sun_falcon/sun_falcon.png");

    public SunFalconEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SunFalconModel(context.getPart(ModModelLayers.SUN_FALCON)), 0.46F);
    }

    @Override
    protected void scale(SunFalconEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.04F, 1.04F, 1.04F);
    }

    @Override
    public Identifier getTexture(SunFalconEntity entity) {
        return TEXTURE;
    }
}
