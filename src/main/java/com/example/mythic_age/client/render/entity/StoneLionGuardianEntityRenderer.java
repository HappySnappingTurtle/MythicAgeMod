package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.client.render.entity.model.StoneLionGuardianModel;
import com.example.mythic_age.entity.StoneLionGuardianEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class StoneLionGuardianEntityRenderer extends MobEntityRenderer<StoneLionGuardianEntity, StoneLionGuardianModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/stone_lion_guardian/stone_lion_guardian.png");

    public StoneLionGuardianEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new StoneLionGuardianModel(context.getPart(ModModelLayers.STONE_LION_GUARDIAN)), 0.66F);
    }

    @Override
    protected void scale(StoneLionGuardianEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.07F, 1.07F, 1.07F);
    }

    @Override
    public Identifier getTexture(StoneLionGuardianEntity entity) {
        return TEXTURE;
    }
}
