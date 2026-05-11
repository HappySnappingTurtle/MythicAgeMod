package com.example.mythic_age.client.render.entity.model;

import com.example.mythic_age.MythicAgeMod;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {
    public static final EntityModelLayer MINOTAUR = 
        new EntityModelLayer(new Identifier(MythicAgeMod.MOD_ID, "minotaur"), "main");
    public static final EntityModelLayer CYCLOPS =
        new EntityModelLayer(new Identifier(MythicAgeMod.MOD_ID, "cyclops"), "main");
    public static final EntityModelLayer CENTAUR =
        new EntityModelLayer(new Identifier(MythicAgeMod.MOD_ID, "centaur"), "main");
    public static final EntityModelLayer HYDRA =
        new EntityModelLayer(new Identifier(MythicAgeMod.MOD_ID, "hydra"), "main");
    public static final EntityModelLayer MEDUSA =
        new EntityModelLayer(new Identifier(MythicAgeMod.MOD_ID, "medusa"), "main");
    public static final EntityModelLayer TEMPLE_GUARDIAN =
        new EntityModelLayer(new Identifier(MythicAgeMod.MOD_ID, "temple_guardian"), "main");
    public static final EntityModelLayer WALKING_TREE =
        new EntityModelLayer(new Identifier(MythicAgeMod.MOD_ID, "walking_tree"), "main");
}
