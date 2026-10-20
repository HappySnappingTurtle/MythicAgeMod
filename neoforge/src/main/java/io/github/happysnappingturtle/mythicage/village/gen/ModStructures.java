package io.github.happysnappingturtle.mythicage.village.gen;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 村庄生成的结构类型与片段类型（T13b §2.1）。 */
public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MythicAgeIds.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MythicAgeIds.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<VanillaGateStructure>> VANILLA_GATE =
            TYPES.register("vanilla_gate", () -> () -> VanillaGateStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<CivVillageStructure>> CIV_VILLAGE =
            TYPES.register("civ_village", () -> () -> CivVillageStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> BUILDING_PIECE =
            PIECES.register("village_building", () -> (StructurePieceType.StructureTemplateType) VillageBuildingPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> INFO_PIECE =
            PIECES.register("village_info", () -> VillageInfoPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PATH_PIECE =
            PIECES.register("village_path", () -> VillagePathPiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PLOT_PIECE =
            PIECES.register("village_plot", () -> VillagePlotPiece::new);

    private ModStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }
}
