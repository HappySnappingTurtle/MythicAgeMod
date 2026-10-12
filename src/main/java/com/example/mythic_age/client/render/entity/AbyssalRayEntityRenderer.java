package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.AbyssalRayModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.AbyssalRayEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class AbyssalRayEntityRenderer extends MobEntityRenderer<AbyssalRayEntity, AbyssalRayModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/abyssal_ray/abyssal_ray.png");

    public AbyssalRayEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new AbyssalRayModel(context.getPart(ModModelLayers.ABYSSAL_RAY)), 0.46F);
    }

    @Override
    protected void scale(AbyssalRayEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.05F, 1.05F, 1.05F);
    }

    @Override
    public Identifier getTexture(AbyssalRayEntity entity) {
        return TEXTURE;
    }
}
