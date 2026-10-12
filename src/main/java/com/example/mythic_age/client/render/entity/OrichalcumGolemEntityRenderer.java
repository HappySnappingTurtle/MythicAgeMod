package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.OrichalcumGolemModel;
import com.example.mythic_age.entity.OrichalcumGolemEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class OrichalcumGolemEntityRenderer extends MobEntityRenderer<OrichalcumGolemEntity, OrichalcumGolemModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/orichalcum_golem/orichalcum_golem.png");

    public OrichalcumGolemEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new OrichalcumGolemModel(context.getPart(ModModelLayers.ORICHALCUM_GOLEM)), 0.72F);
    }

    @Override
    protected void scale(OrichalcumGolemEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.12F, 1.12F, 1.12F);
    }

    @Override
    public Identifier getTexture(OrichalcumGolemEntity entity) {
        return TEXTURE;
    }
}
