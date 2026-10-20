package io.github.happysnappingturtle.mythicage.village.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * 一座村庄建筑（T13b §2.1、§4.3）：放模板，再整地——
 * <ul>
 *   <li>占地每一列：地面下补土到实心地面（≤ {@link TerrainOps#MAX_FILL} 格），占地边缘的列用多边形砌石（低处露出的就是挡土墙）；</li>
 *   <li>模板顶上的山体削掉（≤ {@link TerrainOps#MAX_CUT} 格）；</li>
 *   <li>占地外一圈：高处比地面高 ≥ 2 格时砌挡土墙（≤ 5 格高，压顶方石）；低处按 1:1 培土成缓坡。</li>
 * </ul>
 * 包围盒按 {@link #MARGIN} 格外扩，保证外圈所在的区块也会调用这里。
 */
public final class VillageBuildingPiece extends TemplateStructurePiece {
    public static final int MARGIN = 2;
    public static final int MAX_WALL = 5;

    /** 体素 y = 0（地面上第一层）相对模板原点的高度（建筑数据的 floor_y）。 */
    private final int floorY;
    /** false：桥、码头——不整地，只在模板底层的实心方块下面补同种方块到水底 / 地面。 */
    private final boolean shape;

    public VillageBuildingPiece(StructureTemplateManager manager, String template, BlockPos origin, Rotation rotation,
                                int floorY, boolean shape) {
        super(ModStructures.BUILDING_PIECE.get(), 0, manager, ResourceLocation.parse(template), template,
                settings(rotation), origin);
        this.floorY = floorY;
        this.shape = shape;
    }

    public VillageBuildingPiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.BUILDING_PIECE.get(), tag, manager, rl -> settings(Rotation.valueOf(tag.getString("Rot"))));
        this.floorY = tag.getInt("Floor");
        this.shape = !tag.contains("Shape") || tag.getBoolean("Shape");
    }

    @Override
    public BoundingBox getBoundingBox() {
        BoundingBox b = super.getBoundingBox();
        return new BoundingBox(b.minX() - MARGIN, b.minY() - TerrainOps.MAX_FILL, b.minZ() - MARGIN,
                b.maxX() + MARGIN, b.maxY() + TerrainOps.MAX_CUT, b.maxZ() + MARGIN);
    }

    private static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
        tag.putInt("Floor", floorY);
        tag.putBoolean("Shape", shape);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, chunkBox, chunkPos, pivot);
        BoundingBox t = boundingBox; // 模板本身的包围盒（super 刚重算过）
        if (!shape) {
            pillars(level, chunkBox, t);
            return;
        }
        int floor = t.minY() + floorY;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int x = Math.max(t.minX() - MARGIN, chunkBox.minX()); x <= Math.min(t.maxX() + MARGIN, chunkBox.maxX()); x++) {
            for (int z = Math.max(t.minZ() - MARGIN, chunkBox.minZ()); z <= Math.min(t.maxZ() + MARGIN, chunkBox.maxZ()); z++) {
                int dist = Math.max(Math.max(t.minX() - x, x - t.maxX()), Math.max(t.minZ() - z, z - t.maxZ()));
                if (dist <= 0) {
                    boolean edge = x == t.minX() || x == t.maxX() || z == t.minZ() || z == t.maxZ();
                    // 地面下：模板底层（地基层或地面层）往下补到实心地面
                    int bottom = floorY > 0 ? t.minY() - 1 : floor - 2;
                    BlockState ground = level.getBlockState(p.set(x, floor - 1, z));
                    if (floorY == 0 && TerrainOps.fillable(ground)) {
                        level.setBlock(p, edge ? TerrainOps.polygonal() : grass, 2);
                    }
                    TerrainOps.fillDown(level, p, x, bottom, z, edge ? TerrainOps.polygonal() : dirt,
                            edge ? TerrainOps.polygonal() : dirt);
                    // 模板顶上的山体
                    for (int y = t.maxY() + 1; y <= t.maxY() + TerrainOps.MAX_CUT; y++) {
                        BlockState s = level.getBlockState(p.set(x, y, z));
                        if (s.isAir() || !TerrainOps.clearable(s)) {
                            break;
                        }
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                    continue;
                }
            }
        }
        TerrainOps.ring(level, chunkBox, t.minX(), t.minZ(), t.maxX(), t.maxZ(), floor, MARGIN, MAX_WALL);
    }

    /** 只在模板底层的实心方块下面补同种方块（桥墩、码头石堤一直砌到水底）。 */
    private static void pillars(WorldGenLevel level, BoundingBox chunkBox, BoundingBox t) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(t.minX(), chunkBox.minX()); x <= Math.min(t.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(t.minZ(), chunkBox.minZ()); z <= Math.min(t.maxZ(), chunkBox.maxZ()); z++) {
                BlockState bottom = level.getBlockState(p.set(x, t.minY(), z));
                if (bottom.isAir() || !bottom.isCollisionShapeFullBlock(level, p)) {
                    continue;
                }
                TerrainOps.fillDown(level, p, x, t.minY() - 1, z, bottom, bottom);
            }
        }
    }
}
