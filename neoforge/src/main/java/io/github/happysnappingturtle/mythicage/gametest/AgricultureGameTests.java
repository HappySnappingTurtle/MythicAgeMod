package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.agri.GrapeTrellisBlock;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OliveLeavesBlock;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OlivePressBlock;
import io.github.happysnappingturtle.mythicage.content.greek.agri.OlivePressBlockEntity;
import io.github.happysnappingturtle.mythicage.content.wood.ModBoats;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.config.ConfigHolder;
import io.github.happysnappingturtle.mythicage.core.config.ConfigLoader;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** T6b 橄榄、葡萄、压榨器与催化效果的集成测试（T6b 测试用例文档 §1–§5、设计文档 §10.4）。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AgricultureGameTests {
    private static final String BATCH = "agriculture";
    private static final String CONFIG_BATCH = "agriculture_config";

    /** TC-T6b-62：测试用的生长拦截开关（模拟季节类模组）。 */
    private static volatile boolean blockGrowth;

    static {
        NeoForge.EVENT_BUS.addListener((CropGrowEvent.Pre e) -> {
            if (blockGrowth) {
                e.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            }
        });
    }

    private AgricultureGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gt_agri"));
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        return new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    }

    private static int dropped(GameTestHelper helper, BlockPos rel, Item item) {
        BlockPos abs = helper.absolutePos(rel);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2)).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void clearDrops(GameTestHelper helper, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3)).forEach(e -> e.discard());
    }

    /** 反复随机刻直到状态改变，返回新状态（最多 n 次）。 */
    private static BlockState tickUntilChange(GameTestHelper helper, BlockPos rel, RandomSource random, int n) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);
        BlockState before = level.getBlockState(abs);
        for (int i = 0; i < n; i++) {
            level.getBlockState(abs).randomTick(level, abs, random);
            if (!level.getBlockState(abs).equals(before)) {
                break;
            }
        }
        return level.getBlockState(abs);
    }

    // ---------------------------------------------------------------- 橄榄树

    /** TC-T6b-01：在草方块上生成橄榄树：高 5–7、冠幅 ≤7、至少 2 根侧枝。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void olive_tree_shape(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
            }
        }
        BlockPos origin = new BlockPos(5, 1, 5);
        var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(GreekAgriculture.OLIVE_TREE_CONFIGURED).value();
        helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(7),
                helper.absolutePos(origin)), "应能生成橄榄树");
        int minX = 99, maxX = -99, minZ = 99, maxZ = -99, maxY = 0, branches = 0;
        for (int x = 0; x < 11; x++) {
            for (int y = 1; y < 11; y++) {
                for (int z = 0; z < 11; z++) {
                    BlockState s = helper.getBlockState(new BlockPos(x, y, z));
                    if (s.is(GreekAgriculture.OLIVE_LOG.get()) || s.is(GreekAgriculture.OLIVE_WOOD.get())
                            || s.is(GreekAgriculture.OLIVE_LEAVES.get())) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                        maxY = Math.max(maxY, y);
                    }
                    if (s.is(GreekAgriculture.OLIVE_WOOD.get())) {
                        branches++;
                    }
                }
            }
        }
        helper.assertTrue(maxY >= 5 && maxY <= 7, "树高应为 5–7，实际 " + maxY);
        helper.assertTrue(maxX - minX + 1 <= 9 && maxZ - minZ + 1 <= 9, "冠幅过大");
        helper.assertTrue(branches >= 4, "应有至少 2 根侧枝（每根 ≥2 格），实际侧枝方块 " + branches);
        helper.succeed();
    }

    /** TC-T6b-02、TC-T6b-03：不覆盖已有方块；种在石头上长不出来。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void olive_tree_respects_space(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(GreekAgriculture.OLIVE_TREE_CONFIGURED).value();
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.assertTrue(!feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(1),
                helper.absolutePos(new BlockPos(2, 1, 2))), "石头上不应长树");
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(2), helper.absolutePos(new BlockPos(5, 1, 5)));
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 3, z));
            }
        }
        helper.succeed();
    }

    /** TC-T6b-04 至 TC-T6b-07：结果、玩家放置的树叶不结果、采摘。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void olive_leaves_fruit_and_harvest(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        BlockState natural = GreekAgriculture.OLIVE_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
        helper.setBlock(new BlockPos(1, 0, 1), GreekAgriculture.OLIVE_LOG.get());
        helper.setBlock(p, natural);
        RandomSource random = RandomSource.create(3);
        BlockState s1 = tickUntilChange(helper, p, random, 2000);
        helper.assertTrue(s1.getValue(OliveLeavesBlock.FRUIT) == 1, "应推进到青果，实际 " + s1);
        BlockState s2 = tickUntilChange(helper, p, random, 2000);
        helper.assertTrue(s2.getValue(OliveLeavesBlock.FRUIT) == 2, "应推进到熟果");
        BlockState s3 = tickUntilChange(helper, p, random, 2000);
        helper.assertTrue(s3.getValue(OliveLeavesBlock.FRUIT) == 2, "熟果后不再变化");

        FakePlayer pl = player(helper);
        helper.getBlockState(p).useWithoutItem(helper.getLevel(), pl, hit(helper, p));
        int olives = dropped(helper, p, GreekAgriculture.OLIVE.get());
        helper.assertTrue(olives >= 1 && olives <= 2, "采摘应掉 1–2 个橄榄，实际 " + olives);
        helper.assertTrue(helper.getBlockState(p).getValue(OliveLeavesBlock.FRUIT) == 0, "采摘后回到无果");

        helper.setBlock(p, natural.setValue(OliveLeavesBlock.FRUIT, 1));
        var result = helper.getBlockState(p).useWithoutItem(helper.getLevel(), pl, hit(helper, p));
        helper.assertTrue(!result.consumesAction(), "青果不能采摘");

        BlockState placed = natural.setValue(LeavesBlock.PERSISTENT, true);
        helper.setBlock(p, placed);
        helper.assertTrue(tickUntilChange(helper, p, random, 500).getValue(OliveLeavesBlock.FRUIT) == 0,
                "玩家放置的树叶不结果");
        helper.succeed();
    }

    /** TC-T6b-09：斧头去皮保持轴向。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void olive_log_stripping(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        helper.setBlock(p, GreekAgriculture.OLIVE_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        FakePlayer pl = player(helper);
        pl.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        UseOnContext ctx = new UseOnContext(helper.getLevel(), pl, InteractionHand.MAIN_HAND, pl.getMainHandItem(), hit(helper, p));
        BlockState stripped = helper.getBlockState(p).getToolModifiedState(ctx, ItemAbilities.AXE_STRIP, false);
        helper.assertTrue(stripped != null && stripped.is(GreekAgriculture.STRIPPED_OLIVE_LOG.get())
                && stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "应去皮且轴向不变，实际 " + stripped);
        helper.succeed();
    }

    /** TC-T6b-26 至 TC-T6b-28：告示牌方块实体、船、盆栽。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void signs_boats_pots(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 0), GreekAgriculture.OLIVE_SIGN.get());
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(0, 1, 0))) instanceof SignBlockEntity,
                "告示牌应有方块实体");
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 2), GreekAgriculture.OLIVE_HANGING_SIGN.get());
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 2))) instanceof HangingSignBlockEntity,
                "悬挂告示牌应有方块实体");

        Boat boat = new Boat(helper.getLevel(), 0, 0, 0);
        boat.setVariant(ModBoats.olive());
        helper.assertTrue(boat.getDropItem() == GreekAgriculture.OLIVE_BOAT.get(), "船应掉回橄榄木船");
        ChestBoat chest = new ChestBoat(helper.getLevel(), 0, 0, 0);
        chest.setVariant(ModBoats.olive());
        helper.assertTrue(chest.getDropItem() == GreekAgriculture.OLIVE_CHEST_BOAT.get(), "运输船应掉回橄榄木运输船");

        ResourceLocation sapling = GreekAgriculture.OLIVE_SAPLING.getId();
        helper.assertTrue(((FlowerPotBlock) Blocks.FLOWER_POT).getFullPotsView().get(sapling).get()
                == GreekAgriculture.POTTED_OLIVE_SAPLING.get(), "空花盆应能种下橄榄树苗");
        helper.succeed();
    }

    // ---------------------------------------------------------------- 葡萄

    /** 在 rel 处放一架完整的三格葡萄架（下方铺 soil），三格的 stage 都为 stage。 */
    private static void trellis(GameTestHelper helper, BlockPos rel, Block soil, int stage) {
        helper.setBlock(rel.below(), soil);
        BlockState s = GreekAgriculture.GRAPE_TRELLIS.get().defaultBlockState().setValue(GrapeTrellisBlock.STAGE, stage);
        for (GrapeTrellisBlock.Part part : GrapeTrellisBlock.Part.values()) {
            helper.setBlock(rel.above(part.offset), s.setValue(GrapeTrellisBlock.PART, part));
        }
    }

    /** 三格都是葡萄架，且 stage 相同。 */
    private static void assertTrellisStage(GameTestHelper helper, BlockPos bottom, int stage, String message) {
        for (GrapeTrellisBlock.Part part : GrapeTrellisBlock.Part.values()) {
            BlockState s = helper.getBlockState(bottom.above(part.offset));
            helper.assertTrue(s.is(GreekAgriculture.GRAPE_TRELLIS.get()) && s.getValue(GrapeTrellisBlock.PART) == part
                    && s.getValue(GrapeTrellisBlock.STAGE) == stage, message + "（" + part.getSerializedName() + "）");
        }
    }

    private static void assertTrellisGone(GameTestHelper helper, BlockPos bottom, String message) {
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(!helper.getBlockState(bottom.above(i)).is(GreekAgriculture.GRAPE_TRELLIS.get()), message);
        }
    }

    /** 以玩家（空手）的方式破坏 rel 处的方块，走完整的 playerWillDestroy / playerDestroy 流程。 */
    private static void breakAsPlayer(GameTestHelper helper, FakePlayer pl, BlockPos rel) {
        pl.gameMode.destroyBlock(helper.absolutePos(rel));
    }

    /** TC-T6b-37：用物品放置得到三格；上方第二格或第三格被挡住时放不下，不消耗物品。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void grape_trellis_placement(GameTestHelper helper) {
        FakePlayer pl = player(helper);
        int[] blockedAt = {0, 2, 3};
        for (int k = 0; k < blockedAt.length; k++) {
            BlockPos ground = new BlockPos(1 + 3 * k, 0, 1);
            helper.setBlock(ground, Blocks.DIRT);
            if (blockedAt[k] > 0) {
                helper.setBlock(ground.above(blockedAt[k]), Blocks.STONE);
            }
            ItemStack stack = new ItemStack(GreekAgriculture.GRAPE_TRELLIS.get(), 2);
            BlockPos abs = helper.absolutePos(ground);
            BlockPlaceContext ctx = new BlockPlaceContext(pl, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
            InteractionResult result = ((BlockItem) stack.getItem()).place(ctx);
            if (blockedAt[k] == 0) {
                helper.assertTrue(result.consumesAction() && stack.getCount() == 1, "应放下并消耗 1 个");
                assertTrellisStage(helper, ground.above(), 0, "应放出三格");
                Direction.Axis axis = helper.getBlockState(ground.above()).getValue(GrapeTrellisBlock.AXIS);
                for (int i = 2; i <= 3; i++) {
                    helper.assertTrue(helper.getBlockState(ground.above(i)).getValue(GrapeTrellisBlock.AXIS) == axis,
                            "三格的 axis 应一致");
                }
            } else {
                helper.assertTrue(!result.consumesAction() && stack.getCount() == 2, "被挡住时不应放下");
                assertTrellisGone(helper, ground.above(), "被挡住时不应有葡萄架");
            }
        }
        helper.succeed();
    }

    /** TC-T6b-31、33、34、36：对中格种下、底格生长、对顶格采摘，破坏顶格时只掉一份。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void grape_trellis_cycle(GameTestHelper helper) {
        BlockPos p = new BlockPos(5, 1, 5);
        trellis(helper, p, Blocks.DIRT, 0);
        FakePlayer pl = player(helper);
        ItemStack cuttings = new ItemStack(GreekAgriculture.GRAPE_CUTTING.get(), 3);
        pl.setItemInHand(InteractionHand.MAIN_HAND, cuttings);
        helper.getBlockState(p.above()).useItemOn(cuttings, helper.getLevel(), pl, InteractionHand.MAIN_HAND,
                hit(helper, p.above()));
        assertTrellisStage(helper, p, 1, "对中格种下后三格应为 1");
        helper.assertTrue(cuttings.getCount() == 2, "插条应 −1");

        RandomSource random = RandomSource.create(5);
        for (int stage = 2; stage <= 4; stage++) {
            tickUntilChange(helper, p, random, 3000);
            assertTrellisStage(helper, p, stage, "应推进到 " + stage);
        }
        tickUntilChange(helper, p, random, 500);
        assertTrellisStage(helper, p, 4, "成熟后不再变化");

        helper.getBlockState(p.above(2)).useWithoutItem(helper.getLevel(), pl, hit(helper, p.above(2)));
        int grapes = dropped(helper, p.above(2), GreekAgriculture.GRAPE.get());
        helper.assertTrue(grapes >= 2 && grapes <= 3, "采摘应掉 2–3 串，实际 " + grapes);
        assertTrellisStage(helper, p, 2, "采摘后回到抽藤阶段");
        clearDrops(helper, p.above());

        helper.setBlock(p, helper.getBlockState(p).setValue(GrapeTrellisBlock.STAGE, 4));
        assertTrellisStage(helper, p, 4, "改底格后上两格应同步");
        breakAsPlayer(helper, pl, p.above(2));
        assertTrellisGone(helper, p, "破坏顶格后整架应消失");
        helper.assertTrue(dropped(helper, p.above(), GreekAgriculture.GRAPE_TRELLIS.get().asItem()) == 1, "应只掉 1 个架子");
        helper.assertTrue(dropped(helper, p.above(), GreekAgriculture.GRAPE_CUTTING.get()) == 1, "应掉 1 根插条");
        helper.assertTrue(dropped(helper, p.above(), GreekAgriculture.GRAPE.get()) >= 2, "成熟时破坏另掉葡萄");
        helper.succeed();
    }

    /** TC-T6b-32：架子放在石头上时，对任意一格都种不下插条。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void grape_needs_soil(GameTestHelper helper) {
        BlockPos p = new BlockPos(5, 1, 5);
        trellis(helper, p, Blocks.STONE, 0);
        FakePlayer pl = player(helper);
        ItemStack cuttings = new ItemStack(GreekAgriculture.GRAPE_CUTTING.get(), 3);
        for (int i = 0; i < 3; i++) {
            helper.getBlockState(p.above(i)).useItemOn(cuttings, helper.getLevel(), pl, InteractionHand.MAIN_HAND,
                    hit(helper, p.above(i)));
        }
        assertTrellisStage(helper, p, 0, "石头上不应能种");
        helper.assertTrue(cuttings.getCount() == 3, "不应消耗插条");
        helper.succeed();
    }

    /** TC-T6b-38、39、41：破坏底格或中格只掉一份；创造模式不掉落；下方的土被换掉后整架掉落。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void grape_trellis_breaking(GameTestHelper helper) {
        FakePlayer pl = player(helper);
        Item trellisItem = GreekAgriculture.GRAPE_TRELLIS.get().asItem();
        for (int i = 0; i < 2; i++) {
            BlockPos p = new BlockPos(1 + 4 * i, 1, 1);
            trellis(helper, p, Blocks.DIRT, 0);
            breakAsPlayer(helper, pl, p.above(i));
            assertTrellisGone(helper, p, "破坏任意一格后整架应消失");
            helper.assertTrue(dropped(helper, p.above(), trellisItem) == 1, "应只掉 1 个架子");
            clearDrops(helper, p.above());
        }

        BlockPos c = new BlockPos(1, 1, 7);
        trellis(helper, c, Blocks.DIRT, 4);
        FakePlayer creative = player(helper);
        creative.setGameMode(GameType.CREATIVE);
        breakAsPlayer(helper, creative, c.above(2));
        assertTrellisGone(helper, c, "创造模式破坏顶格后整架应消失");
        helper.assertTrue(dropped(helper, c.above(), trellisItem) == 0
                && dropped(helper, c.above(), GreekAgriculture.GRAPE.get()) == 0, "创造模式不应掉落");

        BlockPos s = new BlockPos(8, 1, 7);
        trellis(helper, s, Blocks.DIRT, 1);
        helper.setBlock(s.below(), Blocks.STONE);
        assertTrellisGone(helper, s, "土被换成石头后整架应掉落");
        helper.assertTrue(dropped(helper, s.above(), trellisItem) == 1
                && dropped(helper, s.above(), GreekAgriculture.GRAPE_CUTTING.get()) == 1, "应掉架子与插条");
        helper.succeed();
    }

    /** TC-T6b-40：中格、顶格接收随机刻时不推进。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void grape_only_bottom_ticks(GameTestHelper helper) {
        BlockPos p = new BlockPos(5, 1, 5);
        trellis(helper, p, Blocks.DIRT, 1);
        RandomSource random = RandomSource.create(3);
        tickUntilChange(helper, p.above(), random, 3000);
        tickUntilChange(helper, p.above(2), random, 3000);
        assertTrellisStage(helper, p, 1, "只有底格生长");
        helper.succeed();
    }

    // ---------------------------------------------------------------- 压榨器

    private static OlivePressBlockEntity press(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, GreekAgriculture.OLIVE_PRESS.get());
        return (OlivePressBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
    }

    /** TC-T6b-40、TC-T6b-45：一批 40 刻；两批连续产出 2 瓶。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH, timeoutTicks = 200)
    public static void press_processes_batches(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        OlivePressBlockEntity be = press(helper, p);
        be.setItem(OlivePressBlockEntity.INPUT, new ItemStack(GreekAgriculture.OLIVE.get(), 8));
        be.setItem(OlivePressBlockEntity.CONTAINER, new ItemStack(GreekMaterials.CERAMIC_COMPONENT.get(), 2));
        helper.runAfterDelay(20, () -> helper.assertTrue(helper.getBlockState(p).getValue(OlivePressBlock.PRESSING),
                "工作时 pressing 应为 true"));
        helper.runAfterDelay(43, () -> {
            helper.assertTrue(be.getItem(OlivePressBlockEntity.OUTPUT).getCount() == 1, "第一批应产出 1 瓶，实际 "
                    + be.getItem(OlivePressBlockEntity.OUTPUT));
            helper.assertTrue(be.getItem(OlivePressBlockEntity.INPUT).getCount() == 4, "应扣 4 个橄榄");
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(be.getItem(OlivePressBlockEntity.OUTPUT).getCount() == 2, "两批应产出 2 瓶");
            helper.assertTrue(be.getItem(OlivePressBlockEntity.CONTAINER).isEmpty(), "容器应用完");
            helper.assertTrue(!helper.getBlockState(p).getValue(OlivePressBlock.PRESSING), "停止后 pressing 为 false");
            helper.succeed();
        });
    }

    /** TC-T6b-41 至 TC-T6b-44：原料不够、没有容器、产物满时暂停，进度保留。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH, timeoutTicks = 200)
    public static void press_pauses(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        OlivePressBlockEntity be = press(helper, p);
        be.setItem(OlivePressBlockEntity.INPUT, new ItemStack(GreekAgriculture.OLIVE.get(), 3));
        be.setItem(OlivePressBlockEntity.CONTAINER, new ItemStack(GreekMaterials.CERAMIC_COMPONENT.get(), 1));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.progress() == 0, "橄榄不够时不应开始");
            be.setItem(OlivePressBlockEntity.INPUT, new ItemStack(GreekAgriculture.OLIVE.get(), 4));
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(be.progress() > 0, "够料后应开始");
            be.setItem(OlivePressBlockEntity.CONTAINER, ItemStack.EMPTY);
        });
        helper.runAfterDelay(40, () -> {
            int kept = be.progress();
            helper.assertTrue(kept > 0, "暂停时进度应保留");
            be.setItem(OlivePressBlockEntity.CONTAINER, new ItemStack(GreekMaterials.CERAMIC_COMPONENT.get(), 1));
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(be.getItem(OlivePressBlockEntity.OUTPUT).getCount() == 1, "继续后应产出");
            be.setItem(OlivePressBlockEntity.OUTPUT, new ItemStack(GreekAgriculture.OLIVE_OIL.get(), 16));
            be.setItem(OlivePressBlockEntity.INPUT, new ItemStack(GreekAgriculture.OLIVE.get(), 4));
            be.setItem(OlivePressBlockEntity.CONTAINER, new ItemStack(GreekMaterials.CERAMIC_COMPONENT.get(), 1));
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(be.getItem(OlivePressBlockEntity.OUTPUT).getCount() == 16, "产物满时应暂停");
            helper.assertTrue(be.getItem(OlivePressBlockEntity.INPUT).getCount() == 4, "产物满时不应扣料");
            helper.succeed();
        });
    }

    /** TC-T6b-46：漏斗自动化：上方放原料、侧面放容器、下方取产物；原料不能进容器槽。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH, timeoutTicks = 200)
    public static void press_hoppers(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        OlivePressBlockEntity be = press(helper, p);
        helper.assertTrue(!be.canPlaceItemThroughFace(OlivePressBlockEntity.CONTAINER,
                new ItemStack(GreekAgriculture.OLIVE.get()), Direction.NORTH), "橄榄不能进容器槽");
        helper.assertTrue(!be.canTakeItemThroughFace(OlivePressBlockEntity.INPUT,
                new ItemStack(GreekAgriculture.OLIVE.get()), Direction.DOWN), "原料槽不能被取出");
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.HOPPER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.HOPPER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.HOPPER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.SOUTH));
        var top = (net.minecraft.world.level.block.entity.HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)));
        var side = (net.minecraft.world.level.block.entity.HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(0, 1, 1)));
        top.setItem(0, new ItemStack(GreekAgriculture.OLIVE.get(), 4));
        side.setItem(0, new ItemStack(GreekMaterials.CERAMIC_COMPONENT.get(), 1));
        helper.runAfterDelay(150, () -> {
            var bottom = (net.minecraft.world.level.block.entity.HopperBlockEntity) helper.getLevel()
                    .getBlockEntity(helper.absolutePos(new BlockPos(1, 0, 1)));
            int oil = 0;
            for (int i = 0; i < bottom.getContainerSize(); i++) {
                if (bottom.getItem(i).is(GreekAgriculture.OLIVE_OIL.get())) {
                    oil += bottom.getItem(i).getCount();
                }
            }
            helper.assertTrue(oil + be.getItem(OlivePressBlockEntity.OUTPUT).getCount() == 1, "全自动应产出 1 瓶油");
            helper.succeed();
        });
    }

    /** TC-T6b-47、TC-T6b-48、TC-T6b-63：NBT 往返；配方来自数据包；同一刻多次调用只计一次。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void press_nbt_data_and_tick_guard(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        OlivePressBlockEntity be = press(helper, p);
        be.setItem(OlivePressBlockEntity.INPUT, new ItemStack(GreekAgriculture.OLIVE.get(), 4));
        be.setItem(OlivePressBlockEntity.CONTAINER, new ItemStack(GreekMaterials.CERAMIC_COMPONENT.get(), 1));
        int before = be.progress();
        for (int i = 0; i < 10; i++) {
            be.tick(helper.getLevel(), helper.absolutePos(p), helper.getBlockState(p));
        }
        helper.assertTrue(be.progress() - before <= 1, "同一游戏刻内多次调用只应计一次，实际 +" + (be.progress() - before));
        var tag = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        OlivePressBlockEntity copy = new OlivePressBlockEntity(helper.absolutePos(p), helper.getBlockState(p));
        copy.loadWithComponents(tag, helper.getLevel().registryAccess());
        helper.assertTrue(copy.getItem(OlivePressBlockEntity.INPUT).getCount() == 4 && copy.progress() == be.progress(),
                "NBT 往返后槽位与进度应一致");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                MythicAgeIds.MOD_ID, "olive_oil_from_pressing")).isPresent(), "压榨配方应由数据包加载");
        helper.succeed();
    }

    /** TC-T6b-49：破坏压榨器掉落本体与槽内物品。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void press_drops_contents(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        OlivePressBlockEntity be = press(helper, p);
        be.setItem(OlivePressBlockEntity.INPUT, new ItemStack(GreekAgriculture.OLIVE.get(), 7));
        helper.getLevel().destroyBlock(helper.absolutePos(p), true);
        helper.assertTrue(dropped(helper, p, GreekAgriculture.OLIVE.get()) == 7, "应掉出槽内的橄榄");
        helper.assertTrue(dropped(helper, p, GreekAgriculture.OLIVE_PRESS.get().asItem()) == 1, "应掉压榨器本体");
        helper.succeed();
    }

    /** TC-T6b-50、TC-T6b-51：油灯亮度 12，不属于圣火盆标签。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void oil_lamp(GameTestHelper helper) {
        BlockState lamp = GreekAgriculture.GREEK_OIL_LAMP.get().defaultBlockState();
        helper.assertTrue(lamp.getLightEmission() == 12, "亮度应为 12");
        helper.assertTrue(!lamp.is(net.minecraft.tags.TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "braziers"))), "油灯不计为圣火盆");
        helper.succeed();
    }

    // ---------------------------------------------------------------- 催化效果

    /** TC-T6b-60：橄榄树叶默认不接受骨粉，也不消耗骨粉。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void bone_meal_ignored_on_leaves(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        helper.setBlock(p, GreekAgriculture.OLIVE_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 5);
        boolean used = BoneMealItem.applyBonemeal(meal, helper.getLevel(), helper.absolutePos(p), null);
        helper.assertTrue(!used && meal.getCount() == 5, "不应接受骨粉，也不应消耗");
        helper.assertTrue(helper.getBlockState(p).getValue(OliveLeavesBlock.FRUIT) == 0, "不应推进");
        helper.succeed();
    }

    /** TC-T6b-61：骨粉对葡萄架约 50% 几率推进 1 阶，从不一次推进 2 阶；施在中格也作用到整架；成熟后不接受。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = BATCH)
    public static void bone_meal_on_trellis(GameTestHelper helper) {
        BlockPos p = new BlockPos(5, 1, 5);
        GrapeTrellisBlock block = GreekAgriculture.GRAPE_TRELLIS.get();
        RandomSource random = RandomSource.create(11);
        int success = 0;
        for (int i = 0; i < 1000; i++) {
            BlockState s = block.defaultBlockState().setValue(GrapeTrellisBlock.STAGE, 1);
            if (block.isBonemealSuccess(helper.getLevel(), random, helper.absolutePos(p), s)) {
                success++;
            }
        }
        helper.assertTrue(success >= 450 && success <= 550, "骨粉成功率应约 50%，实际 " + success / 10.0 + "%");
        trellis(helper, p, Blocks.DIRT, 1);
        block.performBonemeal(helper.getLevel(), random, helper.absolutePos(p.above()), helper.getBlockState(p.above()));
        assertTrellisStage(helper, p, 2, "施在中格：每次只推进 1 阶，三格同步");
        helper.setBlock(p, helper.getBlockState(p).setValue(GrapeTrellisBlock.STAGE, 4));
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(!block.isValidBonemealTarget(helper.getLevel(), helper.absolutePos(p.above(i)),
                    helper.getBlockState(p.above(i))), "成熟后任何一格都不接受骨粉");
        }
        helper.succeed();
    }

    /** TC-T6b-62：其他模组拦截 CropGrowEvent.Pre 时，随机刻与骨粉都不推进。 */
    @GameTest(template = GameTestTemplates.EMPTY_11X11, batch = CONFIG_BATCH)
    public static void crop_grow_event_respected(GameTestHelper helper) {
        BlockPos p = new BlockPos(5, 1, 5);
        GrapeTrellisBlock block = GreekAgriculture.GRAPE_TRELLIS.get();
        trellis(helper, p, Blocks.DIRT, 1);
        blockGrowth = true;
        try {
            tickUntilChange(helper, p, RandomSource.create(1), 3000);
            block.performBonemeal(helper.getLevel(), RandomSource.create(1), helper.absolutePos(p), helper.getBlockState(p));
            assertTrellisStage(helper, p, 1, "被拦截时不应推进");
        } finally {
            blockGrowth = false;
        }
        helper.succeed();
    }

    /** TC-T6b-64：热更 growth.boneMeal.oliveLeaves=true 后橄榄树叶接受骨粉。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = CONFIG_BATCH)
    public static void bone_meal_config(GameTestHelper helper) {
        ConfigHolder<WorldRulesConfig> holder = ModConfigs.worldRules();
        WorldRulesConfig original = holder.current();
        BlockPos p = new BlockPos(1, 1, 1);
        helper.setBlock(p, GreekAgriculture.OLIVE_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1));
        try {
            WorldRulesConfig.Growth g = original.growth();
            WorldRulesConfig next = new WorldRulesConfig(original.beneficialConversions(), original.mobBlockDamage(),
                    original.blockEntityStall(), original.godPowers(), original.recipes(),
                    new WorldRulesConfig.Growth(g.oliveFruitTicks(), g.grapeStageTicks(),
                            new WorldRulesConfig.BoneMeal(g.boneMeal().grapeChance(), true)), original.temples());
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), next));
            ItemStack meal = new ItemStack(Items.BONE_MEAL, 5);
            helper.assertTrue(BoneMealItem.applyBonemeal(meal, helper.getLevel(), helper.absolutePos(p), null),
                    "开启后应接受骨粉");
            helper.assertTrue(helper.getBlockState(p).getValue(OliveLeavesBlock.FRUIT) == 1, "应推进 1 阶");
        } finally {
            holder.reload(ConfigLoader.toPrettyJson(holder.schema(), original));
        }
        helper.succeed();
    }

    /** TC-T6b-21 至 TC-T6b-23：原版木材配方与燃料通过标签直接可用。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void vanilla_wood_integration(GameTestHelper helper) {
        ItemStack planks = new ItemStack(GreekAgriculture.OLIVE_PLANKS.get());
        helper.assertTrue(planks.is(net.minecraft.tags.ItemTags.PLANKS), "木板应在 planks 标签");
        helper.assertTrue(planks.getBurnTime(null) == 300, "木板燃烧时间应同原版");
        helper.assertTrue(new ItemStack(GreekAgriculture.OLIVE_LOG.get()).getBurnTime(null) == 300, "原木燃烧时间应同原版");
        helper.assertTrue(GreekAgriculture.OLIVE_LOG.get().defaultBlockState().is(BlockTags.LOGS_THAT_BURN), "原木应可燃");
        var level = helper.getLevel();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(planks.copy(), planks.copy()));
        var sticks = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
        helper.assertTrue(sticks.is(Items.STICK), "橄榄木板应能合成木棍");
        helper.succeed();
    }
}
