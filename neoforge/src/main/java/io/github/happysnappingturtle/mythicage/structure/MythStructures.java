package io.github.happysnappingturtle.mythicage.structure;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 村庄以外的结构（遗迹、迷宫、精英巢穴）的结构类型与片段类型（T22 §3.1）；村庄的在 {@code village.gen.ModStructures}。 */
public final class MythStructures {
    static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MythicAgeIds.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MythicAgeIds.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MythicAgeIds.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<RuinStructure>> RUIN =
            TYPES.register("ruin", () -> () -> RuinStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<LabyrinthStructure>> LABYRINTH =
            TYPES.register("labyrinth", () -> () -> LabyrinthStructure.CODEC);

    /** 精英巢穴（T25a）：嵌进坡面的洞。 */
    public static final DeferredHolder<StructureType<?>, StructureType<LairStructure>> LAIR =
            TYPES.register("lair", () -> () -> LairStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> LAIR_PIECE =
            PIECES.register("lair", () -> (StructurePieceType.StructureTemplateType) LairPiece::new);

    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<LairPiece.ShellProcessor>> SHELL_PROCESSOR =
            PROCESSORS.register("lair_shell", () -> () -> LairPiece.ShellProcessor.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> DROMOS_PIECE =
            PIECES.register("dromos", () -> DromosPiece::new);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TEMPLATE_PIECE =
            PIECES.register("template", () -> (StructurePieceType.StructureTemplateType) TemplatePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> INFO_PIECE =
            PIECES.register("structure_info", () -> StructureInfoPiece::new);

    private MythStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
        PROCESSORS.register(modBus);
    }
}
