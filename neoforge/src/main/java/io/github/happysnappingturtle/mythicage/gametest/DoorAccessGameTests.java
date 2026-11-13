package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.BuildingSpec;
import io.github.happysnappingturtle.mythicage.core.village.gen.DoorAccess;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.gen.TemplateTransform;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.village.build.Construction;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildingPiece;
import io.github.happysnappingturtle.mythicage.village.gen.VillageBuildings;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/**
 * GA-14 坡地建筑门前通道（TC-GA14-20～24）。谷仓（门朝东、宽 2）原点在 (0, 2, 10)：占地 x 0..10、z 10..18，门在 x = 9、z = 14..15，
 * 门内地面站立高度 y = 3；门外（x ≥ 11）是石头土坡，顶面 y = 5（比门内地面高 2 格）。
 * 整地只写场地里的格（chunkBox 限在场地内），不碰隔壁用例。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorAccessGameTests {
    private static final String BATCH = "door_access";
    private static final String T = GameTestTemplates.EMPTY_17X16X36;
    private static final String GRANARY = "greek:granary";

    private DoorAccessGameTests() {
    }

    static void hill(GameTestHelper h, int top) {
        VillageWorkGameTests.floor(h);
        for (int x = 11; x <= 16; x++) {
            for (int z = 6; z <= 22; z++) {
                for (int y = 2; y <= top; y++) {
                    h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    static BoundingBox arena(GameTestHelper h) {
        BlockPos a = h.absolutePos(new BlockPos(0, 1, 0));
        BlockPos b = h.absolutePos(new BlockPos(16, 15, 35));
        return BoundingBox.fromCorners(a, b);
    }

    static VillageBuildingPiece piece(GameTestHelper h) {
        BuildingSpec s = VillageBuildings.all().get(GRANARY);
        BlockPos o = h.absolutePos(new BlockPos(0, 2, 10));
        Pos origin = new Pos(o.getX(), o.getY(), o.getZ());
        return new VillageBuildingPiece(h.getLevel().getStructureManager(), s.template(), o, Rotation.NONE, s.floorY(), true,
                DoorAccess.door(s, origin, Turn.R0));
    }

    static void generate(GameTestHelper h, VillageBuildingPiece p) {
        p.postProcess(h.getLevel(), h.getLevel().structureManager(), h.getLevel().getChunkSource().getGenerator(),
                h.getLevel().getRandom(), arena(h), new ChunkPos(h.absolutePos(BlockPos.ZERO)), h.absolutePos(BlockPos.ZERO));
    }

    static boolean stair(GameTestHelper h, int x, int y, int z) {
        BlockState s = h.getLevel().getBlockState(h.absolutePos(new BlockPos(x, y, z)));
        return s.is(GreekMaterials.MARBLE_ASHLAR_STAIRS.get()) && s.getValue(StairBlock.FACING) == Direction.EAST;
    }

    static boolean air(GameTestHelper h, int x, int y, int z) {
        return h.getLevel().getBlockState(h.absolutePos(new BlockPos(x, y, z))).isAir();
    }

    /** 门两格都能一级一级走出去：门内 3 → 第 1 级 4 → 第 2 级 5 → 坡顶 6，每级上方净空 2 格。 */
    static void assertWalkable(GameTestHelper h) {
        for (int z : new int[]{14, 15}) {
            h.assertTrue(stair(h, 11, 3, z) && stair(h, 12, 4, z), "两级朝东的方石台阶（z = " + z + "）");
            h.assertTrue(air(h, 11, 4, z) && air(h, 11, 5, z) && air(h, 12, 5, z) && air(h, 12, 6, z), "台阶上方净空（z = " + z + "）");
            h.assertTrue(!air(h, 13, 5, z) && air(h, 13, 6, z) && air(h, 13, 7, z), "第 3 列是坡顶，走得上去（z = " + z + "）");
        }
        h.assertTrue(!stair(h, 11, 3, 13) && !stair(h, 11, 3, 16), "通道只有门宽");
    }

    /**
     * TC-GA14-20：生成整地后，门外的挡土墙让出两级台阶，门旁的墙留着。挡土墙预先砌好：运行时世界里 {@code ring} 读的
     * WORLD_SURFACE_WG 高度图是旧的（看不见测试里刚垒的土坡），只有真正生成时才可靠。
     */
    @GameTest(template = T, batch = BATCH)
    public static void generation_cuts_steps(GameTestHelper h) {
        wall(h);
        generate(h, piece(h));
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(11, 4, 12))).is(GreekMaterials.POLYGONAL_MARBLE.get()),
                "门旁仍是挡土墙（外圈 1 格处）");
        assertWalkable(h);
        h.succeed();
    }

    /** TC-GA14-21：平地不放台阶。 */
    @GameTest(template = T, batch = BATCH)
    public static void generation_flat_no_steps(GameTestHelper h) {
        VillageWorkGameTests.floor(h);
        generate(h, piece(h));
        for (int x = 11; x <= 15; x++) {
            for (int z = 14; z <= 15; z++) {
                for (int y = 1; y <= 6; y++) {
                    h.assertTrue(!stair(h, x, y, z), "平地没有台阶");
                }
            }
        }
        h.succeed();
    }

    static BuildPlacement.Placed placeGranary(GameTestHelper h) {
        BuildingSpec s = VillageBuildings.all().get(GRANARY);
        BlockPos o = h.absolutePos(new BlockPos(0, 2, 10));
        h.getLevel().getServer().getStructureManager().get(net.minecraft.resources.ResourceLocation.parse(s.template())).orElseThrow()
                .placeInWorld(h.getLevel(), o, o, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                        h.getLevel().getRandom(), 2);
        Pos origin = new Pos(o.getX(), o.getY(), o.getZ());
        return new BuildPlacement.Placed(s.id(), origin, Turn.R0, TemplateTransform.worldBox(origin, s.sizeX(), s.sizeY(), s.sizeZ(), Turn.R0));
    }

    /** 坡地整地的样子：门外 1 格处砌挡土墙（多边形砌石，方石压顶），再往外是石头土坡。 */
    static void wall(GameTestHelper h) {
        hill(h, 5);
        for (int z = 9; z <= 19; z++) {
            h.setBlock(new BlockPos(11, 3, z), GreekMaterials.POLYGONAL_MARBLE.get());
            h.setBlock(new BlockPos(11, 4, z), GreekMaterials.POLYGONAL_MARBLE.get());
            h.setBlock(new BlockPos(11, 5, z), GreekMaterials.MARBLE_ASHLAR.get());
        }
    }

    /** TC-GA14-22：落成检查修好台阶，写一行发展记录。 */
    @GameTest(template = T, batch = BATCH)
    public static void completion_builds_steps(GameTestHelper h) {
        wall(h);
        BuildPlacement.Placed at = placeGranary(h);
        var tv = VillageWorkGameTests.village(h, "greek", Map.of(), at.box());
        try {
            var ctx = VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
            int n = Construction.doorAccess(ctx, VillageBuildings.all().get(GRANARY), at, List.of(at.box()));
            h.assertTrue(n == 4, "两级 × 两列：" + n);
            assertWalkable(h);
            h.assertTrue(ctx.state().log.stream().anyMatch(l -> l.key().equals("mythic_age.village.log.door_steps")), "发展记录");
            h.assertTrue(Construction.doorAccess(ctx, VillageBuildings.all().get(GRANARY), at, List.of(at.box())) == 0, "修过一次就不再修");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    /** TC-GA14-23：通道里有别人的方块：整条不修，什么都不动。 */
    @GameTest(template = T, batch = BATCH)
    public static void completion_blocked_leaves_all(GameTestHelper h) {
        wall(h);
        h.setBlock(new BlockPos(12, 6, 15), Blocks.OAK_PLANKS);
        BuildPlacement.Placed at = placeGranary(h);
        var tv = VillageWorkGameTests.village(h, "greek", Map.of(), at.box());
        try {
            var ctx = VillageScheduler.contextForTest(h.getLevel(), tv.id()).orElseThrow();
            int n = Construction.doorAccess(ctx, VillageBuildings.all().get(GRANARY), at, List.of(at.box()));
            h.assertTrue(n == 0, "不修：" + n);
            h.assertTrue(!stair(h, 11, 3, 14) && h.getLevel().getBlockState(h.absolutePos(new BlockPos(11, 5, 14)))
                    .is(GreekMaterials.MARBLE_ASHLAR.get()), "挡土墙原样");
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(12, 6, 15))).is(Blocks.OAK_PLANKS), "木板不动");
            h.assertTrue(ctx.state().log.stream().anyMatch(l -> l.key().equals("mythic_age.village.log.door_blocked")), "记「被挡」");
            h.succeed();
        } finally {
            VillageWorkGameTests.cleanup(h, tv);
        }
    }

    /** TC-GA14-24：生成片段存档往返；旧存档没有门数据时不开通道、包围盒不外扩。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void piece_save_roundtrip(GameTestHelper h) {
        VillageBuildingPiece p = piece(h);
        var ctx = StructurePieceSerializationContext.fromLevel(h.getLevel());
        CompoundTag tag = p.createTag(ctx);
        VillageBuildingPiece back = new VillageBuildingPiece(h.getLevel().getStructureManager(), tag);
        h.assertTrue(p.door().equals(back.door()), "门读回一致：" + back.door());
        h.assertTrue(back.door().width() == 2 && back.door().facing().equals("east"), "宽 2、朝东");
        tag.remove("Door");
        tag.remove("DoorFacing");
        VillageBuildingPiece old = new VillageBuildingPiece(h.getLevel().getStructureManager(), tag);
        h.assertTrue(old.door() == null, "旧存档没有门数据");
        BoundingBox wide = p.getBoundingBox();
        BoundingBox narrow = old.getBoundingBox();
        h.assertTrue(wide.minX() == narrow.minX() - DoorAccess.MAX_STEPS - 1, "有门时包围盒多外扩：" + wide + " / " + narrow);
        h.succeed();
    }
}
