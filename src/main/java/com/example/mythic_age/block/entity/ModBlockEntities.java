package com.example.mythic_age.block.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.block.ModBlocks;
import com.example.mythic_age.item.GodPowerCertificateItem;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<RelicPedestalBlockEntity> RELIC_PEDESTAL_BLOCK_ENTITY;
    public static BlockEntityType<AbundanceAltarCoreBlockEntity> ABUNDANCE_ALTAR_CORE;
    public static BlockEntityType<AnimalLureTowerBlockEntity> ANIMAL_LURE_TOWER;
    public static BlockEntityType<EgyptianStatuePedestalBlockEntity> EGYPTIAN_STATUE_PEDESTAL;
    
    // 为每个文明类型创建单独的神像基座 BlockEntityType
    public static BlockEntityType<StatueBaseBlockEntity> STATUE_GREEK_BASE;
    public static BlockEntityType<StatueBaseBlockEntity> STATUE_EGYPTIAN_BASE;
    public static BlockEntityType<StatueBaseBlockEntity> STATUE_CHINESE_BASE;
    public static BlockEntityType<StatueBaseBlockEntity> STATUE_ATLANTEAN_BASE;
    public static BlockEntityType<StatueBaseBlockEntity> STATUE_NORDIC_BASE;

    public static void registerBlockEntities() {
        MythicAgeMod.LOGGER.info("Registering Block Entities for " + MythicAgeMod.MOD_ID);
        RELIC_PEDESTAL_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "relic_pedestal_block_entity"),
                FabricBlockEntityTypeBuilder.create(RelicPedestalBlockEntity::new, ModBlocks.RELIC_PEDESTAL).build()
        );
        
        ABUNDANCE_ALTAR_CORE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "abundance_altar_core"),
                FabricBlockEntityTypeBuilder.create(AbundanceAltarCoreBlockEntity::new, ModBlocks.ABUNDANCE_ALTAR_CORE).build()
        );
        
        ANIMAL_LURE_TOWER = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "animal_lure_tower"),
                FabricBlockEntityTypeBuilder.create(AnimalLureTowerBlockEntity::new, ModBlocks.ANIMAL_LURE_TOWER).build()
        );

        EGYPTIAN_STATUE_PEDESTAL = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "egyptian_statue_pedestal"),
                FabricBlockEntityTypeBuilder.create(EgyptianStatuePedestalBlockEntity::new, ModBlocks.EGYPTIAN_STATUE_PEDESTAL).build()
        );

        // 为每个文明类型注册单独的神像基座 BlockEntityType
        STATUE_GREEK_BASE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "statue_greek_base"),
                FabricBlockEntityTypeBuilder.create(
                    (pos, state) -> new StatueBaseBlockEntity(pos, state, GodPowerCertificateItem.MythologyType.GREEK),
                    ModBlocks.STATUE_GREEK_BASE
                ).build()
        );
        
        STATUE_EGYPTIAN_BASE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "statue_egyptian_base"),
                FabricBlockEntityTypeBuilder.create(
                    (pos, state) -> new StatueBaseBlockEntity(pos, state, GodPowerCertificateItem.MythologyType.EGYPTIAN),
                    ModBlocks.STATUE_EGYPTIAN_BASE
                ).build()
        );
        
        STATUE_CHINESE_BASE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "statue_chinese_base"),
                FabricBlockEntityTypeBuilder.create(
                    (pos, state) -> new StatueBaseBlockEntity(pos, state, GodPowerCertificateItem.MythologyType.CHINESE),
                    ModBlocks.STATUE_CHINESE_BASE
                ).build()
        );
        
        STATUE_ATLANTEAN_BASE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "statue_atlantean_base"),
                FabricBlockEntityTypeBuilder.create(
                    (pos, state) -> new StatueBaseBlockEntity(pos, state, GodPowerCertificateItem.MythologyType.ATLANTEAN),
                    ModBlocks.STATUE_ATLANTEAN_BASE
                ).build()
        );
        
        STATUE_NORDIC_BASE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MythicAgeMod.MOD_ID, "statue_nordic_base"),
                FabricBlockEntityTypeBuilder.create(
                    (pos, state) -> new StatueBaseBlockEntity(pos, state, GodPowerCertificateItem.MythologyType.NORDIC),
                    ModBlocks.STATUE_NORDIC_BASE
                ).build()
        );
    }
}

