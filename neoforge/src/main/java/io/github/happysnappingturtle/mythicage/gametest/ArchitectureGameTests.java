package io.github.happysnappingturtle.mythicage.gametest;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.content.greek.GreekArchitecture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.AxisBlock;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.ColumnShaftBlock;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.MosaicBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.greek.temple.ColumnCounter;
import io.github.happysnappingturtle.mythicage.core.scan.Grid3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** T8b 希腊建筑套件的集成测试（TC-T8b-01～12）。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchitectureGameTests {
    private static final String BATCH = "architecture";
    private static final List<String> IDS = List.of("marble_column_base", "marble_column", "doric_capital", "ionic_capital",
            "corinthian_capital", "marble_architrave", "triglyph_frieze", "marble_cornice", "terracotta_roof_tiles",
            "terracotta_roof_stairs", "terracotta_roof_slab", "terracotta_ridge", "terracotta_antefix", "pebble_mosaic");

    private ArchitectureGameTests() {
    }

    /** TC-T8b-01：14 个方块已声明、三语完整、有物品。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void kit_registered(GameTestHelper h) {
        for (String id : IDS) {
            List<ContentEntry> e = Content.entries().stream().filter(c -> c.id().equals(id)).toList();
            h.assertTrue(e.size() == 1, id + " 应在 Content 中恰好声明一次");
            ContentEntry c = e.getFirst();
            h.assertTrue(c.block().isPresent() && c.item().isPresent(), id + " 应有方块和物品");
            h.assertTrue(!c.name().zh().isBlank() && !c.name().en().isBlank() && !c.name().ja().isBlank(), id + " 三语不完整");
        }
        h.succeed();
    }

    private static ColumnShaftBlock.Part part(GameTestHelper h, int y) {
        return h.getBlockState(new BlockPos(1, y, 1)).getValue(ColumnShaftBlock.PART);
    }

    /** 模拟玩家放置（触发 getStateForPlacement 与邻居更新）。 */
    private static void place(GameTestHelper h, Block block, BlockPos below, float yaw) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ma_builder"));
        p.setYRot(yaw);
        ItemStack stack = new ItemStack(block);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(below);
        stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
    }

    /** TC-T8b-02～04：柱身自动分段，柱础与柱头不影响分段。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void column_parts(GameTestHelper h) {
        h.setBlock(1, 0, 1, Blocks.STONE);
        Block shaft = GreekArchitecture.MARBLE_COLUMN.get();
        place(h, shaft, new BlockPos(1, 0, 1), 0);
        h.assertTrue(part(h, 1) == ColumnShaftBlock.Part.SINGLE, "单段应为 single");
        place(h, shaft, new BlockPos(1, 1, 1), 0);
        h.assertTrue(part(h, 1) == ColumnShaftBlock.Part.BOTTOM && part(h, 2) == ColumnShaftBlock.Part.TOP, "两段应为 bottom + top");
        place(h, shaft, new BlockPos(1, 2, 1), 0);
        h.assertTrue(part(h, 1) == ColumnShaftBlock.Part.BOTTOM && part(h, 2) == ColumnShaftBlock.Part.MIDDLE
                && part(h, 3) == ColumnShaftBlock.Part.TOP, "三段应为 bottom + middle + top");
        h.getLevel().destroyBlock(h.absolutePos(new BlockPos(1, 2, 1)), false);
        h.assertTrue(part(h, 1) == ColumnShaftBlock.Part.SINGLE && part(h, 3) == ColumnShaftBlock.Part.SINGLE,
                "拆掉中段后上下应各自变回 single");
        place(h, GreekArchitecture.DORIC_CAPITAL.get(), new BlockPos(1, 3, 1), 0);
        h.assertTrue(part(h, 3) == ColumnShaftBlock.Part.SINGLE, "上方的柱头不影响分段");
        h.succeed();
    }

    /** TC-T8b-05、06：柱础 + 柱身 + 柱头（3 格）在神庙判定中算一根合格柱；套件的柱构件都在 greek_column。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void composed_column_counts(GameTestHelper h) {
        h.setBlock(1, 0, 1, GreekArchitecture.MARBLE_COLUMN_BASE.get());
        h.setBlock(1, 1, 1, GreekArchitecture.MARBLE_COLUMN.get());
        h.setBlock(1, 2, 1, GreekArchitecture.CORINTHIAN_CAPITAL.get());
        Grid3 g = new Grid3(3, 3, 3);
        for (int y = 0; y < 3; y++) {
            g.set(1, y, 1, h.getBlockState(new BlockPos(1, y, 1)).is(GreekTemple.GREEK_COLUMN));
        }
        h.assertTrue(ColumnCounter.count(g, 3).count() == 1, "柱础 + 柱身 + 柱头应计为 1 根柱");
        for (Block b : List.of(GreekArchitecture.MARBLE_COLUMN_BASE.get(), GreekArchitecture.MARBLE_COLUMN.get(),
                GreekArchitecture.DORIC_CAPITAL.get(), GreekArchitecture.IONIC_CAPITAL.get(),
                GreekArchitecture.CORINTHIAN_CAPITAL.get())) {
            BlockState s = b.defaultBlockState();
            h.assertTrue(s.is(GreekTemple.GREEK_COLUMN) && s.is(GreekMaterials.MARBLE_FAMILY), b + " 应在 greek_column 与 marble_family");
        }
        h.succeed();
    }

    /** TC-T8b-07：三陇板饰带沿 x 连续放置时三陇板与陇间板交替。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void frieze_alternates(GameTestHelper h) {
        boolean[] seen = new boolean[4];
        for (int x = 0; x < 4; x++) {
            h.setBlock(x + 1, 0, 1, Blocks.STONE);
            place(h, GreekArchitecture.TRIGLYPH_FRIEZE.get(), new BlockPos(x + 1, 0, 1), 0);
            seen[x] = h.getBlockState(new BlockPos(x + 1, 1, 1)).getValue(FacingShapedBlock.ALT);
        }
        h.assertTrue(seen[0] != seen[1] && seen[1] != seen[2] && seen[2] != seen[3], "相邻两块应交替");
        h.succeed();
    }

    /** TC-T8b-08：檐口是原版楼梯形状，转角自动衔接。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void cornice_corners(GameTestHelper h) {
        BlockState c = GreekArchitecture.MARBLE_CORNICE.get().defaultBlockState();
        h.assertTrue(c.getBlock() instanceof StairBlock, "檐口应是楼梯形状");
        h.setBlock(1, 0, 1, c.setValue(StairBlock.FACING, Direction.NORTH));
        h.setBlock(1, 0, 0, c.setValue(StairBlock.FACING, Direction.EAST));
        BlockState shaped = h.getBlockState(new BlockPos(1, 0, 1)).updateShape(Direction.NORTH,
                h.getBlockState(new BlockPos(1, 0, 0)), h.getLevel(), h.absolutePos(new BlockPos(1, 0, 1)),
                h.absolutePos(new BlockPos(1, 0, 0)));
        h.assertTrue(shaped.getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT, "相邻转向的檐口应形成转角");
        h.succeed();
    }

    /** TC-T8b-09：马赛克象限由坐标决定，平移 2 格重复。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void mosaic_quadrants(GameTestHelper h) {
        h.assertTrue(MosaicBlock.quadrantAt(new BlockPos(0, 0, 0)) == 0 && MosaicBlock.quadrantAt(new BlockPos(1, 0, 0)) == 1
                && MosaicBlock.quadrantAt(new BlockPos(0, 0, 1)) == 2 && MosaicBlock.quadrantAt(new BlockPos(1, 0, 1)) == 3,
                "象限应为 0/1/2/3");
        h.assertTrue(MosaicBlock.quadrantAt(new BlockPos(-3, 5, 7)) == MosaicBlock.quadrantAt(new BlockPos(-1, 0, 9)),
                "平移 2 格应重复（含负坐标）");
        h.succeed();
    }

    /** TC-T8b-10：玩家朝南放置爱奥尼柱头，axis = z（涡卷面朝玩家）。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void ionic_axis(GameTestHelper h) {
        h.setBlock(1, 0, 1, Blocks.STONE);
        place(h, GreekArchitecture.IONIC_CAPITAL.get(), new BlockPos(1, 0, 1), 0);
        h.assertTrue(h.getBlockState(new BlockPos(1, 1, 1)).getValue(AxisBlock.AXIS) == Direction.Axis.Z, "朝南放置应为 z 轴");
        h.succeed();
    }

    /** TC-T8b-11、12（GA-6 修订）：瓦当具有贴檐碰撞；柱身顶面不是完整面。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void shapes(GameTestHelper h) {
        h.setBlock(1, 0, 1, GreekArchitecture.TERRACOTTA_ANTEFIX.get());
        BlockPos a = h.absolutePos(new BlockPos(1, 0, 1));
        h.assertTrue(!h.getLevel().getBlockState(a).getCollisionShape(h.getLevel(), a).isEmpty(), "瓦当应有贴檐碰撞");
        h.setBlock(1, 1, 1, GreekArchitecture.MARBLE_COLUMN.get());
        BlockPos c = h.absolutePos(new BlockPos(1, 1, 1));
        h.assertTrue(!h.getLevel().getBlockState(c).isFaceSturdy(h.getLevel(), c, Direction.UP), "柱身顶面不应是完整面");
        h.succeed();
    }

    /** 修订 02：13 格宽的缓坡山花（13、9、5、1 四行）逐块放置后，形状为 L、LH、M…M、RH、R，顶端 SINGLE。 */
    @GameTest(template = GameTestTemplates.EMPTY_17X16X20, batch = BATCH)
    public static void pediment_low_pitch_shapes(GameTestHelper h) {
        int[] widths = {13, 9, 5, 1};
        BlockState base = io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_PEDIMENT.get()
                .defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        for (int row = 0; row < widths.length; row++) {
            int start = 2 + row * 2;
            for (int i = 0; i < widths[row]; i++) {
                h.setBlock(start + i, 1 + row, 5, base);
            }
        }
        var P = io.github.happysnappingturtle.mythicage.content.greek.temple.PedimentBlock.SHAPE;
        for (int row = 0; row < 3; row++) {
            int start = 2 + row * 2;
            int w = widths[row];
            var shapes = new java.util.ArrayList<String>();
            for (int i = 0; i < w; i++) {
                shapes.add(h.getBlockState(new BlockPos(start + i, 1 + row, 5)).getValue(P).getSerializedName());
            }
            // 两端是外坡、次外是内坡，且同一端的外坡与内坡属于同一侧（与结构旋转无关）
            String a0 = shapes.get(0);
            String a1 = shapes.get(1);
            String b0 = shapes.get(w - 1);
            String b1 = shapes.get(w - 2);
            h.assertTrue(java.util.Set.of(a0, b0).equals(java.util.Set.of("left", "right"))
                    && a1.equals(a0 + "_high") && b1.equals(b0 + "_high"), "第 " + row + " 行两端应为外坡与内坡：" + shapes);
            for (int i = 2; i < w - 2; i++) {
                h.assertTrue(shapes.get(i).equals("middle"), "第 " + row + " 行中段应为 middle：" + shapes);
            }
        }
        h.assertTrue(h.getBlockState(new BlockPos(8, 4, 5)).getValue(P).getSerializedName().equals("single"), "顶端应为 single");
        h.succeed();
    }
}
