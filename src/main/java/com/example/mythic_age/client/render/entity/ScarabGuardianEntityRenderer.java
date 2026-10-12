package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.ScarabGuardianModel;
import com.example.mythic_age.entity.ScarabGuardianEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ScarabGuardianEntityRenderer extends MobEntityRenderer<ScarabGuardianEntity, ScarabGuardianModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/scarab_guardian/scarab_guardian.png");

    public ScarabGuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ScarabGuardianModel(context.getPart(ModModelLayers.SCARAB_GUARDIAN)), 0.45F);
    }

    @Override
    protected void scale(ScarabGuardianEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.05F, 1.05F, 1.05F);
    }

    @Override
    public Identifier getTexture(ScarabGuardianEntity entity) {
        return TEXTURE;
    }
}
