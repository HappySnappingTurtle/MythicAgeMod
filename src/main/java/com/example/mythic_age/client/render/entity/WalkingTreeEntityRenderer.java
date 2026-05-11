package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.WalkingTreeModel;
import com.example.mythic_age.entity.WalkingTreeEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * 行走树实体渲染器
 * 
 * 负责渲染行走树实体。
 */
public class WalkingTreeEntityRenderer extends MobEntityRenderer<WalkingTreeEntity, WalkingTreeModel> {
    public WalkingTreeEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new WalkingTreeModel(context.getPart(ModModelLayers.WALKING_TREE)), 1.25F);
    }

    @Override
    protected void scale(WalkingTreeEntity entity, MatrixStack matrices, float amount) {
        float scale = 1.2F;
        matrices.scale(scale, scale, scale);
    }
    
    @Override
    public Identifier getTexture(WalkingTreeEntity entity) {
        return new Identifier(MythicAgeMod.MOD_ID, "textures/entity/walking_tree/" + entity.getVariant().getSerializedName() + ".png");
    }
}
