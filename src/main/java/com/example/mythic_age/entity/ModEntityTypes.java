package com.example.mythic_age.entity;

import com.example.mythic_age.MythicAgeMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 模组实体类型注册
 */
public class ModEntityTypes {
    
    /**
     * 陨石实体类型
     * 用于陨石雨法术的下落动画效果
     */
    public static final EntityType<MeteorEntity> METEOR = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(MythicAgeMod.MOD_ID, "meteor"),
        FabricEntityTypeBuilder.<MeteorEntity>create(SpawnGroup.MISC, MeteorEntity::new)
            .dimensions(EntityDimensions.fixed(1.0f, 1.0f))
            .trackRangeBlocks(64)
            .trackedUpdateRate(1)
            .build()
    );
    
    /**
     * 注册所有实体类型
     */
    public static void registerEntityTypes() {
        MythicAgeMod.LOGGER.info("Registering Entity Types for " + MythicAgeMod.MOD_ID);
        
        // 确保陨石实体类型已经被正确注册
        // METEOR实体类型已在静态初始化块中注册，这里只需确认
        if (METEOR != null) {
            MythicAgeMod.LOGGER.info("Successfully registered Meteor entity type: " + METEOR.toString());
        } else {
            MythicAgeMod.LOGGER.error("Failed to register Meteor entity type - METEOR is null!");
        }
        
        // 注意：陨石实体不需要使用FabricDefaultAttributeRegistry，因为它不是LivingEntity
    }
}