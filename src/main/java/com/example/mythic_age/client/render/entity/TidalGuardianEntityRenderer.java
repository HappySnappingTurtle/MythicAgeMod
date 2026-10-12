package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.TidalGuardianModel;
import com.example.mythic_age.entity.TidalGuardianEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class TidalGuardianEntityRenderer extends MobEntityRenderer<TidalGuardianEntity, TidalGuardianModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/tidal_guardian/tidal_guardian.png");

    public TidalGuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new TidalGuardianModel(context.getPart(ModModelLayers.TIDAL_GUARDIAN)), 0.58F);
    }

    @Override
    protected void scale(TidalGuardianEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(TidalGuardianEntity entity) {
        return TEXTURE;
    }
}
