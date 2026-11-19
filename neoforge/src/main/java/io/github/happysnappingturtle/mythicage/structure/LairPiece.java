package io.github.happysnappingturtle.mythicage.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 巢穴模板件（T25a §5.2）：模板只写明确给出的格子（洞内空气、地面、陈设、岩壳），其余保留原地形。
 * <ul>
 *   <li>岩壳（{@link #SHELL} 里的几种石头）在世界里那一格已经是实心方块时不放——山体够厚的地方看不出人造的壳，
 *       山体不够厚（洞口上方、坡面凹处）才补成岩石；</li>
 *   <li>放完后，模板地面那一层（洞底与洞外平台）下面是空的就往下补（顶层泥土、下面石头），坡脚不悬空。</li>
 * </ul>
 */
public final class LairPiece extends TerrainTemplatePiece {
    /** 模板里作岩壳用的方块。 */
    public static final Set<Block> SHELL = Set.of(Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF, Blocks.COBBLESTONE,
            Blocks.MOSSY_COBBLESTONE);

    public LairPiece(StructureTemplateManager manager, String template, BlockPos origin, Rotation rotation, int floorY) {
        super(MythStructures.LAIR_PIECE.get(), manager, template, origin, rotation, floorY, Terrain.NONE);
        placeSettings.addProcessor(ShellProcessor.INSTANCE);
    }

    public LairPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(MythStructures.LAIR_PIECE.get(), manager, tag);
        placeSettings.addProcessor(ShellProcessor.INSTANCE);
    }

    @Override
    protected Terrain readTerrain(CompoundTag tag) {
        return Terrain.NONE;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, chunkBox, chunkPos, pivot);
        BoundingBox t = boundingBox;
        int ground = t.minY() + floorY - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = Math.max(t.minX(), chunkBox.minX()); x <= Math.min(t.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(t.minZ(), chunkBox.minZ()); z <= Math.min(t.maxZ(), chunkBox.maxZ()); z++) {
                BlockState g = level.getBlockState(p.set(x, ground, z));
                if (g.isAir() || !g.getFluidState().isEmpty()) {
                    continue;
                }
                TerrainOps.fillDown(level, p, x, ground - 1, z, dirt, stone);
            }
        }
    }

    /** 岩壳遇到实心的原地形就不放。 */
    public static final class ShellProcessor extends StructureProcessor {
        public static final ShellProcessor INSTANCE = new ShellProcessor();
        public static final MapCodec<ShellProcessor> CODEC = MapCodec.unit(INSTANCE);

        private ShellProcessor() {
        }

        @Nullable
        @Override
        public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                                  StructureTemplate.StructureBlockInfo blockInfo,
                                                                  StructureTemplate.StructureBlockInfo relative,
                                                                  StructurePlaceSettings settings) {
            if (!SHELL.contains(relative.state().getBlock())) {
                return relative;
            }
            BlockState here = level.getBlockState(relative.pos());
            if (!here.isAir() && here.getFluidState().isEmpty() && here.isSolid()) {
                return null;
            }
            return relative;
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return MythStructures.SHELL_PROCESSOR.get();
        }
    }
}
