package io.github.happysnappingturtle.mythicage.structure;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 村庄以外的结构（遗迹、迷宫）的结构类型与片段类型（T22 §3.1）；村庄的在 {@code village.gen.ModStructures}。 */
public final class MythStructures {
    static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MythicAgeIds.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MythicAgeIds.MOD_ID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TEMPLATE_PIECE =
            PIECES.register("template", () -> (StructurePieceType.StructureTemplateType) TemplatePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> INFO_PIECE =
            PIECES.register("structure_info", () -> StructureInfoPiece::new);

    private MythStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }
}
