package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.content.greek.GreekVillageBlocks;
import io.github.happysnappingturtle.mythicage.content.greek.village.PedimentOrnamentBlock;
import io.github.happysnappingturtle.mythicage.content.greek.village.TallWorkstationBlock;
import io.github.happysnappingturtle.mythicage.content.greek.village.TreadingVatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * 希腊村庄新方块（TC-T13b-70 的多格部分）：陶窑 / 青铜熔炉两格高、踩葡萄池两格长——放置时自动补另一格、
 * 拆任一格整件移除、只掉 1 个、生火状态两半同步。地面在 y = 1。
 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageBlockGameTests {
    private static final String BATCH = "village_blocks";
    private static final String T = GameTestTemplates.EMPTY_11X11;

    private VillageBlockGameTests() {
    }

    private static FakePlayer player(GameTestHelper h) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ma_village_blocks"));
        p.setYRot(0); // 朝南看：放下的工作站正面朝北（朝向放置者）
        return p;
    }

    private static void ground(GameTestHelper h) {
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    /** 站在 below 上方用物品右键 below 的顶面。 */
    private static void place(GameTestHelper h, FakePlayer p, Block block, BlockPos below) {
        ItemStack stack = new ItemStack(block);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = h.absolutePos(below);
        stack.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
    }

    private static List<ItemStack> drops(GameTestHelper h, BlockState state) {
        return state.getDrops(new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.IRON_PICKAXE)));
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void tall_workstation_two_halves(GameTestHelper h) {
        ground(h);
        FakePlayer p = player(h);
        for (Block block : List.of(GreekVillageBlocks.POTTERY_KILN.get(), GreekVillageBlocks.BRONZE_FURNACE.get())) {
            BlockPos base = new BlockPos(block == GreekVillageBlocks.POTTERY_KILN.get() ? 3 : 7, 2, 5);
            place(h, p, block, base.below());
            BlockState lower = h.getBlockState(base);
            BlockState upper = h.getBlockState(base.above());
            h.assertTrue(lower.is(block) && lower.getValue(TallWorkstationBlock.HALF) == DoubleBlockHalf.LOWER, block + " 下半");
            h.assertTrue(upper.is(block) && upper.getValue(TallWorkstationBlock.HALF) == DoubleBlockHalf.UPPER, block + " 上半");
            h.assertTrue(lower.getValue(TallWorkstationBlock.FACING) == Direction.NORTH, block + " 应朝向放置者");
            h.assertTrue(drops(h, lower).size() == 1 && drops(h, upper).isEmpty(), block + " 只有下半掉落");
            // 改下半的生火状态，上半跟着亮
            h.setBlock(base, lower.setValue(TallWorkstationBlock.LIT, true));
            h.assertTrue(h.getBlockState(base.above()).getValue(TallWorkstationBlock.LIT), block + " 上半应同步生火");
            h.assertTrue(h.getBlockState(base.above()).getLightEmission() == 13, block + " 生火时上半发光");
            // 拆上半，下半一并移除
            h.getLevel().destroyBlock(h.absolutePos(base.above()), false);
            h.assertBlockNotPresent(block, base);
        }
        // 头顶不够两格：放不下
        h.setBlock(new BlockPos(5, 3, 8), Blocks.STONE);
        place(h, p, GreekVillageBlocks.POTTERY_KILN.get(), new BlockPos(5, 1, 8));
        h.assertBlockNotPresent(GreekVillageBlocks.POTTERY_KILN.get(), new BlockPos(5, 2, 8));
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void treading_vat_two_cells(GameTestHelper h) {
        ground(h);
        FakePlayer p = player(h);
        Block vat = GreekVillageBlocks.TREADING_VAT.get();
        BlockPos front = new BlockPos(5, 2, 3);
        place(h, p, vat, front.below());
        BlockState f = h.getBlockState(front);
        BlockState b = h.getBlockState(front.south());
        h.assertTrue(f.is(vat) && f.getValue(TreadingVatBlock.PART) == BedPart.FOOT, "点的那格是前格");
        h.assertTrue(b.is(vat) && b.getValue(TreadingVatBlock.PART) == BedPart.HEAD, "沿视线往里一格是后格");
        h.assertTrue(f.getValue(TreadingVatBlock.FACING) == Direction.NORTH && b.getValue(TreadingVatBlock.FACING) == Direction.NORTH,
                "两格朝向一致、朝放置者");
        h.assertTrue(drops(h, f).size() == 1 && drops(h, b).isEmpty(), "只有前格掉落");
        // 空心碰撞：池中间的碰撞箱顶只有池底高
        double top = f.getCollisionShape(h.getLevel(), h.absolutePos(front)).max(Direction.Axis.Y, 0.5, 0.5);
        h.assertTrue(top < 0.2, "池中间应能站进去，实际碰撞顶 " + top);
        h.getLevel().destroyBlock(h.absolutePos(front), false);
        h.assertBlockNotPresent(vat, front.south());
        // 后面被挡住：放不下
        h.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        place(h, p, vat, new BlockPos(2, 1, 3));
        h.assertBlockNotPresent(vat, new BlockPos(2, 2, 3));
        h.succeed();
    }

    @GameTest(template = T, batch = BATCH, timeoutTicks = 100)
    public static void pediment_ornament_parts(GameTestHelper h) {
        ground(h);
        Block bolt = GreekVillageBlocks.PEDIMENT_TRIDENT.get();
        BlockState south = bolt.defaultBlockState().setValue(PedimentOrnamentBlock.FACING, Direction.SOUTH);
        // 朝南：模型 +x（顺时针）是西，所以西端是 start、东端是 end
        for (int x = 3; x <= 5; x++) {
            h.setBlock(new BlockPos(x, 3, 5), south);
        }
        h.setBlock(new BlockPos(4, 4, 5), south);
        h.setBlock(new BlockPos(8, 3, 5), south);
        h.assertTrue(part(h, 5, 3, 5) == PedimentOrnamentBlock.Part.START, "西端应为 start，实际 " + part(h, 5, 3, 5));
        h.assertTrue(part(h, 4, 3, 5) == PedimentOrnamentBlock.Part.MIDDLE, "中间应为 middle");
        h.assertTrue(part(h, 3, 3, 5) == PedimentOrnamentBlock.Part.END, "东端应为 end");
        h.assertTrue(part(h, 4, 4, 5) == PedimentOrnamentBlock.Part.TOP, "叠在上面的应为 top");
        h.assertTrue(part(h, 8, 3, 5) == PedimentOrnamentBlock.Part.SINGLE, "单独一格应为 single");
        // 拆掉一端，剩下两格重新拼
        h.setBlock(new BlockPos(5, 3, 5), Blocks.AIR);
        h.assertTrue(part(h, 4, 3, 5) == PedimentOrnamentBlock.Part.START, "拆掉西端后中间那格变 start");
        // 朝向不同的不相连
        h.setBlock(new BlockPos(9, 3, 5), bolt.defaultBlockState().setValue(PedimentOrnamentBlock.FACING, Direction.NORTH));
        h.assertTrue(part(h, 8, 3, 5) == PedimentOrnamentBlock.Part.SINGLE, "朝向不同不应相连");
        h.assertTrue(h.getBlockState(new BlockPos(4, 3, 5)).getCollisionShape(h.getLevel(),
                h.absolutePos(new BlockPos(4, 3, 5))).isEmpty(), "楣饰没有碰撞");
        h.succeed();
    }

    private static PedimentOrnamentBlock.Part part(GameTestHelper h, int x, int y, int z) {
        return h.getBlockState(new BlockPos(x, y, z)).getValue(PedimentOrnamentBlock.PART);
    }
}
