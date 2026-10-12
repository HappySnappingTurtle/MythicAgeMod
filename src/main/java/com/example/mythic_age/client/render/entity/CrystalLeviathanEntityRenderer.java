package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.CrystalLeviathanModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.CrystalLeviathanEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CrystalLeviathanEntityRenderer extends MobEntityRenderer<CrystalLeviathanEntity, CrystalLeviathanModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/crystal_leviathan/crystal_leviathan.png");

    public CrystalLeviathanEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new CrystalLeviathanModel(context.getPart(ModModelLayers.CRYSTAL_LEVIATHAN)), 0.72F);
    }

    @Override
    protected void scale(CrystalLeviathanEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.12F, 1.12F, 1.12F);
    }

    @Override
    public Identifier getTexture(CrystalLeviathanEntity entity) {
        return TEXTURE;
    }
}
