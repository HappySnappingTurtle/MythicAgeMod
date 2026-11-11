package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.GreekArchitecture;
import io.github.happysnappingturtle.mythicage.content.greek.architecture.FacingShapedBlock;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** GA-6：模型对应的碰撞、真实物品点击放置与正式卫城模板依附。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchitectureAttachmentGameTests {
    private static final String BATCH = "architecture_attachment";
    private static final String CLASSICAL = "village/greek/acropolis_classical";

    private ArchitectureAttachmentGameTests() {
    }

    private static VoxelShape expected(Direction facing) {
        return switch (facing) {
            case NORTH -> Block.box(0, 0, 14, 16, 9, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 9, 2);
            case EAST -> Block.box(0, 0, 0, 2, 9, 16);
            case WEST -> Block.box(14, 0, 0, 16, 9, 16);
            default -> throw new IllegalArgumentException("瓦当必须水平朝向");
        };
    }

    /** TC-GA6-20：朝北碰撞箱精确匹配 box(0,0,14,16,9,16)，其余朝向同步旋转。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void antefix_shapes(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var state = GreekArchitecture.TERRACOTTA_ANTEFIX.get().defaultBlockState().setValue(FacingShapedBlock.FACING, facing);
            h.getLevel().setBlock(pos, state, Block.UPDATE_ALL);
            h.assertTrue(!Shapes.joinIsNotEmpty(state.getCollisionShape(h.getLevel(), pos), expected(facing), BooleanOp.NOT_SAME),
                    facing + " 瓦当碰撞箱必须贴着背面");
            h.assertTrue(!Shapes.joinIsNotEmpty(state.getShape(h.getLevel(), pos), expected(facing), BooleanOp.NOT_SAME),
                    facing + " 瓦当选取框必须与碰撞箱一致");
        }
        h.succeed();
    }

    /** TC-GA6-21：用真实物品点击石头北面，正面朝北且背面与石头相接；补测另三面。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void antefix_clicked_face(GameTestHelper h) {
        BlockPos stone = h.absolutePos(new BlockPos(5, 2, 5));
        h.getLevel().setBlock(stone, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ga6_builder"));
        for (Direction face : Direction.Plane.HORIZONTAL) {
            // 故意使玩家水平视向与点击面不一致，防止误用玩家朝向也能通过。
            player.setYRot(face.toYRot());
            ItemStack stack = new ItemStack(GreekArchitecture.TERRACOTTA_ANTEFIX.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            Vec3 hit = Vec3.atCenterOf(stone).add(face.getStepX() * 0.5, 0, face.getStepZ() * 0.5);
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, stone, false)));
            BlockPos pos = stone.relative(face);
            var state = h.getLevel().getBlockState(pos);
            h.assertTrue(state.is(GreekArchitecture.TERRACOTTA_ANTEFIX.get()), face + " 点击面应成功放置瓦当");
            h.assertTrue(state.getValue(FacingShapedBlock.FACING) == face, face + " 正面应背离支撑石头");
            var box = state.getCollisionShape(h.getLevel(), pos).bounds().move(pos.getX(), pos.getY(), pos.getZ());
            double gap = switch (face) {
                case NORTH -> stone.getZ() - box.maxZ;
                case SOUTH -> box.minZ - (stone.getZ() + 1);
                case EAST -> box.minX - (stone.getX() + 1);
                case WEST -> stone.getX() - box.maxX;
                default -> throw new IllegalArgumentException();
            };
            h.assertTrue(Math.abs(gap) < 1.0e-9, face + " 碰撞箱应与石头相接，间距=" + gap);
        }
        h.succeed();
    }

    /** TC-GA6-22：GameTest 直接放置正式古典卫城，检查每个瓦当身后的实体支撑与共用许可表。 */
    @GameTest(template = CLASSICAL, batch = BATCH, timeoutTicks = 100)
    public static void classical_acropolis_backing(GameTestHelper h) throws IOException {
        ArchitectureAttachments rules;
        var id = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "mythic_age/architecture_attach.json");
        try (var in = h.getLevel().getServer().getResourceManager().getResourceOrThrow(id).open()) {
            rules = ArchitectureAttachments.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        var template = h.getLevel().getServer().getStructureManager().get(
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, CLASSICAL)).orElseThrow();
        var size = template.getSize();
        int count = 0;
        for (BlockPos relative : BlockPos.betweenClosed(0, 0, 0, size.getX() - 1, size.getY() - 1, size.getZ() - 1)) {
            BlockPos pos = h.absolutePos(relative);
            var state = h.getLevel().getBlockState(pos);
            if (!state.is(GreekArchitecture.TERRACOTTA_ANTEFIX.get())) {
                continue;
            }
            count++;
            Direction facing = state.getValue(FacingShapedBlock.FACING);
            BlockPos behind = pos.relative(facing.getOpposite());
            var support = h.getLevel().getBlockState(behind);
            String supportId = BuiltInRegistries.BLOCK.getKey(support.getBlock()).toString();
            h.assertTrue(!support.getCollisionShape(h.getLevel(), behind).isEmpty()
                            && (support.isFaceSturdy(h.getLevel(), behind, facing)
                            || rules.allowsSupport("mythic_age:terracotta_antefix", supportId)),
                    CLASSICAL + " " + relative + " 瓦当身后无支撑：" + supportId);
        }
        h.assertTrue(count > 0, "古典卫城必须实际包含瓦当，不能空跑");
        h.succeed();
    }
}
