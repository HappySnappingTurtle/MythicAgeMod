package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.ValkyrieModel;
import com.example.mythic_age.entity.ValkyrieEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ValkyrieEntityRenderer extends MobEntityRenderer<ValkyrieEntity, ValkyrieModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/valkyrie/valkyrie.png");

    public ValkyrieEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ValkyrieModel(context.getPart(ModModelLayers.VALKYRIE)), 0.5F);
    }

    @Override
    protected void scale(ValkyrieEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.05F, 1.05F, 1.05F);
    }

    @Override
    public Identifier getTexture(ValkyrieEntity entity) {
        return TEXTURE;
    }
}
