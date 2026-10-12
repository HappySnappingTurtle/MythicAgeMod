package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.AnubisWarriorModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.AnubisWarriorEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class AnubisWarriorEntityRenderer extends MobEntityRenderer<AnubisWarriorEntity, AnubisWarriorModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/anubis_warrior/anubis_warrior.png");

    public AnubisWarriorEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new AnubisWarriorModel(context.getPart(ModModelLayers.ANUBIS_WARRIOR)), 0.55F);
    }

    @Override
    protected void scale(AnubisWarriorEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.08F, 1.08F, 1.08F);
    }

    @Override
    public Identifier getTexture(AnubisWarriorEntity entity) {
        return TEXTURE;
    }
}
