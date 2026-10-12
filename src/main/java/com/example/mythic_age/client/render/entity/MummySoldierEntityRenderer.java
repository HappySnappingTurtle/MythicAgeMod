package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.MummySoldierModel;
import com.example.mythic_age.entity.MummySoldierEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MummySoldierEntityRenderer extends MobEntityRenderer<MummySoldierEntity, MummySoldierModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/mummy_soldier/mummy_soldier.png");

    public MummySoldierEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MummySoldierModel(context.getPart(ModModelLayers.MUMMY_SOLDIER)), 0.5F);
    }

    @Override
    protected void scale(MummySoldierEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.02F, 1.02F, 1.02F);
    }

    @Override
    public Identifier getTexture(MummySoldierEntity entity) {
        return TEXTURE;
    }
}
