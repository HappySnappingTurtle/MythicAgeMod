package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.HydraModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.HydraEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class HydraEntityRenderer extends MobEntityRenderer<HydraEntity, HydraModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/hydra/hydra.png");

    public HydraEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new HydraModel(context.getPart(ModModelLayers.HYDRA)), 1.35F);
    }

    @Override
    protected void scale(HydraEntity entity, MatrixStack matrices, float amount) {
        float s = 1.65F;
        matrices.translate(0.0F, -(s - 1.0F) * 1.4F, 0.0F);
        matrices.scale(s, s, s);
    }

    @Override
    public Identifier getTexture(HydraEntity entity) {
        return TEXTURE;
    }
}
