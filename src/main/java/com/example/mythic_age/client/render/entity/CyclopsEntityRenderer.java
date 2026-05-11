package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.CyclopsModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.CyclopsEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CyclopsEntityRenderer extends MobEntityRenderer<CyclopsEntity, CyclopsModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/cyclops/cyclops.png");

    public CyclopsEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new CyclopsModel(context.getPart(ModModelLayers.CYCLOPS)), 0.9F);
    }

    @Override
    protected void scale(CyclopsEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.05F, 1.05F, 1.05F);
    }

    @Override
    public Identifier getTexture(CyclopsEntity entity) {
        return TEXTURE;
    }
}
