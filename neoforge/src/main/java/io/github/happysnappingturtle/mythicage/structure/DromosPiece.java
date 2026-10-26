package io.github.happysnappingturtle.mythicage.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * 下行台阶（dromos，T22 §5.3）：从迷宫入口格的南口一路向南、每格升 1，升到地面为止；3 格宽、净高 4，两侧墙、台阶下与顶上实心
 * （地面以上不砌顶——最后几级是露天的下沉台阶，接地表残迹的中庭）。深度不固定（地表下 24 格，遇到夹取时更深），所以由程序挖而不是模板。
 *
 * @param x0     台阶最西一列
 * @param z0     第一级台阶所在的 z
 * @param y0     迷宫地面（第一级台阶的方块高度）
 * @param depth  级数（= 地面上第一层的高度 − y0）
 */
public final class DromosPiece extends StructurePiece {
    private final int x0;
    private final int z0;
    private final int y0;
    private final int depth;
    private final String stairs;
    private final String wall;
    private final String ceiling;
    private final String filler;

    public DromosPiece(int x0, int z0, int y0, int depth, String stairs, String wall, String ceiling, String filler) {
        super(MythStructures.DROMOS_PIECE.get(), 0, new BoundingBox(x0 - 1, y0 - 1, z0, x0 + 3, y0 + depth + 5, z0 + depth - 1));
        this.x0 = x0;
        this.z0 = z0;
        this.y0 = y0;
        this.depth = depth;
        this.stairs = stairs;
        this.wall = wall;
        this.ceiling = ceiling;
        this.filler = filler;
    }

    public DromosPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(MythStructures.DROMOS_PIECE.get(), tag);
        this.x0 = tag.getInt("X0");
        this.z0 = tag.getInt("Z0");
        this.y0 = tag.getInt("Y0");
        this.depth = tag.getInt("Depth");
        this.stairs = tag.getString("Stairs");
        this.wall = tag.getString("Wall");
        this.ceiling = tag.getString("Ceiling");
        this.filler = tag.getString("Filler");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("X0", x0);
        tag.putInt("Z0", z0);
        tag.putInt("Y0", y0);
        tag.putInt("Depth", depth);
        tag.putString("Stairs", stairs);
        tag.putString("Wall", wall);
        tag.putString("Ceiling", ceiling);
        tag.putString("Filler", filler);
    }

    private static BlockState state(String id) {
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        return b == Blocks.AIR ? Blocks.STONE.defaultBlockState() : b.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BlockState step = state(stairs);
        if (step.hasProperty(StairBlock.FACING)) {
            step = step.setValue(StairBlock.FACING, Direction.SOUTH);
        }
        BlockState wallState = state(wall);
        BlockState ceilState = state(ceiling);
        BlockState fill = state(filler);
        int surface = y0 + depth; // 地面上第一层
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < depth; i++) {
            int z = z0 + i;
            int y = y0 + i;
            for (int x = x0 - 1; x <= x0 + 3; x++) {
                boolean side = x == x0 - 1 || x == x0 + 3;
                for (int yy = y - 1; yy <= y + 5; yy++) {
                    if (!chunkBox.isInside(p.set(x, yy, z))) {
                        continue;
                    }
                    BlockState s;
                    if (yy == y - 1) {
                        s = fill;                                    // 台阶下
                    } else if (side) {
                        if (yy >= surface) {
                            continue;                                // 地面以上不砌墙（露天下沉台阶）
                        }
                        s = wallState;
                    } else if (yy == y) {
                        s = step;
                    } else if (yy <= y + 4) {
                        s = Blocks.AIR.defaultBlockState();
                    } else {
                        if (yy >= surface - 1) {
                            continue;                                // 地面以上不砌顶
                        }
                        s = ceilState;
                    }
                    level.setBlock(p, s, 2);
                }
            }
        }
    }
}
