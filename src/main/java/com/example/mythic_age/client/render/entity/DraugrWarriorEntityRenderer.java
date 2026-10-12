package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.client.render.entity.model.DraugrWarriorModel;
import com.example.mythic_age.client.render.entity.model.ModModelLayers;
import com.example.mythic_age.entity.DraugrWarriorEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class DraugrWarriorEntityRenderer extends MobEntityRenderer<DraugrWarriorEntity, DraugrWarriorModel> {
    private static final Identifier TEXTURE = new Identifier(MythicAgeMod.MOD_ID, "textures/entity/draugr_warrior/draugr_warrior.png");

    public DraugrWarriorEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DraugrWarriorModel(context.getPart(ModModelLayers.DRAUGR_WARRIOR)), 0.48F);
    }

    @Override
    protected void scale(DraugrWarriorEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.04F, 1.04F, 1.04F);
    }

    @Override
    public Identifier getTexture(DraugrWarriorEntity entity) {
        return TEXTURE;
    }
}
