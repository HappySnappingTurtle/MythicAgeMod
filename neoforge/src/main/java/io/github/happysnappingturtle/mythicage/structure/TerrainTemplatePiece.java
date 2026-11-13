package io.github.happysnappingturtle.mythicage.structure;

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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * 模板 + 整地（T13b §4.3 抽出，T22 §3.2）：放一个结构模板，再按 {@link Terrain} 处理模板周围的地形。村庄建筑与遗迹共用。
 * <ul>
 *   <li>{@link Terrain#FULL}（村庄建筑）：占地每一列补到实心地面（边缘的列用多边形砌石），削掉压在模板上的山体，
 *       外圈高处砌挡土墙、低处 1:1 培土；</li>
 *   <li>{@link Terrain#LIGHT}（遗迹）：补土与削山同上，但边缘也用土，外圈高处按 1:1 削成缓坡、不砌墙（遗迹本来半埋在地里）；</li>
 *   <li>{@link Terrain#PILLARS}（桥、码头）：只在模板底层的实心方块下面补同种方块到水底 / 地面；</li>
 *   <li>{@link Terrain#NONE}：只放模板。</li>
 * </ul>
 * 包围盒按 {@link #MARGIN} 格外扩（含补土与削山的高度），保证外圈所在的区块也会调用这里；有门数据（村庄建筑）时
 * 整地后在门前开台阶（GA-14），包围盒再外扩 {@code DoorAccess.MAX_STEPS + 1} 格。
 */
public abstract class TerrainTemplatePiece extends TemplateStructurePiece {
    public static final int MARGIN = 2;
    public static final int MAX_WALL = 5;

    public enum Terrain { FULL, LIGHT, PILLARS, NONE }

    /** 体素 y = 0（地面上第一层）相对模板原点的高度（建筑数据的 floor_y）。 */
    protected final int floorY;
    protected final Terrain terrain;
    /** 门（世界坐标、朝外方向、宽度）：整地时在门前开台阶（GA-14）；没有门数据（遗迹、旧存档）为 null。 */
    protected final io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.Door door;

    protected TerrainTemplatePiece(StructurePieceType type, StructureTemplateManager manager, String template, BlockPos origin,
                                   Rotation rotation, int floorY, Terrain terrain) {
        this(type, manager, template, origin, rotation, floorY, terrain, null);
    }

    protected TerrainTemplatePiece(StructurePieceType type, StructureTemplateManager manager, String template, BlockPos origin,
                                   Rotation rotation, int floorY, Terrain terrain,
                                   io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.Door door) {
        super(type, 0, manager, ResourceLocation.parse(template), template, settings(rotation), origin);
        this.floorY = floorY;
        this.terrain = terrain;
        this.door = terrain == Terrain.FULL ? door : null;
    }

    protected TerrainTemplatePiece(StructurePieceType type, StructureTemplateManager manager, CompoundTag tag) {
        super(type, tag, manager, rl -> settings(Rotation.valueOf(tag.getString("Rot"))));
        this.floorY = tag.getInt("Floor");
        this.terrain = readTerrain(tag);
        int[] d = tag.getIntArray("Door");
        this.door = d.length == 4 && tag.contains("DoorFacing")
                ? new io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.Door(
                new io.github.happysnappingturtle.mythicage.core.village.gen.Pos(d[0], d[1], d[2]), tag.getString("DoorFacing"), d[3])
                : null;
    }

    public io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.Door door() {
        return door;
    }

    public Terrain terrain() {
        return terrain;
    }

    /** 存档里的整地方式（子类可兼容旧字段）。 */
    protected Terrain readTerrain(CompoundTag tag) {
        return tag.contains("Terrain") ? Terrain.valueOf(tag.getString("Terrain")) : Terrain.FULL;
    }

    protected static StructurePlaceSettings settings(Rotation rotation) {
        return new StructurePlaceSettings().setRotation(rotation);
    }

    @Override
    public BoundingBox getBoundingBox() {
        BoundingBox b = super.getBoundingBox();
        if (terrain == Terrain.NONE) {
            return b;
        }
        // 有门前台阶时多外扩几格，通道所在的区块也会调用整地
        int m = MARGIN + (door == null ? 0 : io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.MAX_STEPS + 1);
        return new BoundingBox(b.minX() - m, b.minY() - TerrainOps.MAX_FILL, b.minZ() - m,
                b.maxX() + m, b.maxY() + TerrainOps.MAX_CUT, b.maxZ() + m);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
        tag.putInt("Floor", floorY);
        tag.putString("Terrain", terrain.name());
        if (door != null) {
            tag.putIntArray("Door", new int[]{door.pos().x(), door.pos().y(), door.pos().z(), door.width()});
            tag.putString("DoorFacing", door.facing());
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, chunkBox, chunkPos, pivot);
        BoundingBox t = boundingBox; // 模板本身的包围盒（super 刚重算过）
        switch (terrain) {
            case NONE -> {
            }
            case PILLARS -> pillars(level, chunkBox, t);
            case FULL, LIGHT -> shape(level, chunkBox, t, terrain == Terrain.FULL);
        }
    }

    private void shape(WorldGenLevel level, BoundingBox chunkBox, BoundingBox t, boolean walls) {
        int floor = t.minY() + floorY;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int x = Math.max(t.minX(), chunkBox.minX()); x <= Math.min(t.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(t.minZ(), chunkBox.minZ()); z <= Math.min(t.maxZ(), chunkBox.maxZ()); z++) {
                boolean edge = walls && (x == t.minX() || x == t.maxX() || z == t.minZ() || z == t.maxZ());
                // 地面下：模板底层（地基层或地面层）往下补到实心地面
                int bottom = floorY > 0 ? t.minY() - 1 : floor - 2;
                BlockState ground = level.getBlockState(p.set(x, floor - 1, z));
                if (floorY == 0 && TerrainOps.fillable(ground)) {
                    level.setBlock(p, edge ? TerrainOps.polygonal() : grass, 2);
                } else if (!walls && floorY > 0) {
                    // 遗迹：地面那一层模板只写了一部分（前庭铺地），其余没写的格子下面是坑就补土
                    TerrainOps.fillDown(level, p, x, floor - 1, z, dirt, dirt);
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
            }
        }
        TerrainOps.ring(level, chunkBox, t.minX(), t.minZ(), t.maxX(), t.maxZ(), floor, MARGIN, MAX_WALL, walls);
        if (door != null) {
            // 门前台阶（GA-14）：外圈的挡土墙不认门，门朝上坡时会把门堵死
            var box = new io.github.happysnappingturtle.mythicage.core.village.gen.Box(t.minX(), t.minY(), t.minZ(), t.maxX(), t.maxY(), t.maxZ());
            TerrainOps.doorSteps(level, chunkBox, io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.corridor(box, door,
                    io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess.MAX_STEPS), door.pos().y(),
                    q -> TerrainOps.doorClearable(level.getBlockState(q)), false);
        }
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
