package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.JiangshiModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.JiangshiEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class JiangshiEntityRenderer extends MobEntityRenderer<JiangshiEntity, JiangshiModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/jiangshi/jiangshi.png");

    public JiangshiEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new JiangshiModel(context.getPart(ModModelLayers.JIANGSHI)), 0.44F);
    }

    @Override
    protected void scale(JiangshiEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.04F, 1.04F, 1.04F);
    }

    @Override
    public Identifier getTexture(JiangshiEntity entity) {
        return TEXTURE;
    }
}
