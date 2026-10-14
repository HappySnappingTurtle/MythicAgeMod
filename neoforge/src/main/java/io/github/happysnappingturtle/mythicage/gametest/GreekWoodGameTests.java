package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.ContentEntry;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekFoods;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.GreekWood;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** T6b 橄榄木族与希腊食物的集成测试：注册、实际配方查询、方块交互及两种容器返还。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GreekWoodGameTests {
    private static final String BATCH = "greek_wood_foods";

    private GreekWoodGameTests() {
    }

    /** TC-T6b-20：21 种木族形态及额外盆栽均已注册、在 Content 声明且三语完整。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void wood_family_registered(GameTestHelper helper) {
        for (String name : List.of("olive_log", "olive_wood", "stripped_olive_log", "stripped_olive_wood",
                "olive_planks", "olive_slab", "olive_stairs", "olive_fence", "olive_fence_gate", "olive_door",
                "olive_trapdoor", "olive_button", "olive_pressure_plate", "olive_sign", "olive_wall_sign",
                "olive_hanging_sign", "olive_wall_hanging_sign", "olive_boat", "olive_chest_boat",
                "olive_sapling", "olive_leaves", "potted_olive_sapling")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, name);
            List<ContentEntry> entries = Content.entries().stream().filter(e -> e.id().equals(name)).toList();
            helper.assertTrue(entries.size() == 1, name + " 应在 Content 中恰好声明一次");
            ContentEntry entry = entries.getFirst();
            helper.assertTrue(!entry.name().zh().isBlank() && !entry.name().en().isBlank()
                    && !entry.name().ja().isBlank(), name + " 三语名称不完整");
            boolean boat = name.equals("olive_boat") || name.equals("olive_chest_boat");
            boolean noItem = name.equals("olive_wall_sign") || name.equals("olive_wall_hanging_sign")
                    || name.equals("potted_olive_sapling");
            helper.assertTrue(entry.block().isPresent() == !boat, name + " 方块声明不正确");
            helper.assertTrue(entry.item().isPresent() == !noItem, name + " 物品声明不正确");
            entry.block().ifPresent(block -> helper.assertTrue(block.isBound()
                    && BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.BLOCK.get(id) == block.get(),
                    name + " 方块未正确注册"));
            entry.item().ifPresent(item -> helper.assertTrue(item.isBound()
                    && BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.get(id) == item.get(),
                    name + " 物品未正确注册"));
        }
        helper.assertTrue(GreekWood.OLIVE_DOOR.asItem() instanceof DoubleHighBlockItem, "门应使用双格放置物品");
        helper.succeed();
    }

    /** TC-T6b-21、22：八种新形态，并兼顾测试文档中的原版木板配方与原木转换。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void wood_shape_recipes(GameTestHelper helper) {
        ItemLike w = GreekAgriculture.OLIVE_PLANKS;
        ItemLike s = Items.STICK;
        ItemLike air = Items.AIR;
        assertCraft(helper, GreekWood.OLIVE_SLAB, 6, 3, 1, w, w, w);
        assertCraft(helper, GreekWood.OLIVE_STAIRS, 4, 3, 3, w, air, air, w, w, air, w, w, w);
        assertCraft(helper, GreekWood.OLIVE_FENCE, 3, 3, 2, w, s, w, w, s, w);
        assertCraft(helper, GreekWood.OLIVE_FENCE_GATE, 1, 3, 2, s, w, s, s, w, s);
        assertCraft(helper, GreekWood.OLIVE_DOOR, 3, 2, 3, w, w, w, w, w, w);
        assertCraft(helper, GreekWood.OLIVE_TRAPDOOR, 2, 3, 2, w, w, w, w, w, w);
        assertCraft(helper, GreekWood.OLIVE_BUTTON, 1, 1, 1, w);
        assertCraft(helper, GreekWood.OLIVE_PRESSURE_PLATE, 1, 2, 1, w, w);
        assertCraft(helper, Items.STICK, 4, 1, 2, w, w);
        assertCraft(helper, Items.CRAFTING_TABLE, 1, 2, 2, w, w, w, w);
        assertCraft(helper, Items.CHEST, 1, 3, 3, w, w, w, w, air, w, w, w, w);
        for (ItemLike log : List.of(GreekAgriculture.OLIVE_LOG, GreekAgriculture.OLIVE_WOOD,
                GreekAgriculture.STRIPPED_OLIVE_LOG, GreekAgriculture.STRIPPED_OLIVE_WOOD)) {
            assertCraft(helper, w, 4, 1, 1, log);
        }
        ItemLike log = GreekAgriculture.OLIVE_LOG;
        assertCraft(helper, GreekAgriculture.OLIVE_WOOD, 3, 2, 2, log, log, log, log);
        ItemLike stripped = GreekAgriculture.STRIPPED_OLIVE_LOG;
        assertCraft(helper, GreekAgriculture.STRIPPED_OLIVE_WOOD, 3, 2, 2, stripped, stripped, stripped, stripped);
        // 公共木台阶标签应与原版数据合并，不能被橄榄木族覆盖或收窄。
        assertCraft(helper, GreekMaterials.WOODEN_PLATTER, 2, 2, 1, Items.OAK_SLAB, Items.OAK_SLAB);
        assertCraft(helper, GreekMaterials.WOODEN_PLATTER, 2, 2, 1, GreekWood.OLIVE_SLAB, GreekWood.OLIVE_SLAB);
        helper.succeed();
    }

    /** TC-T6b-24：门的上下两半实际存在，破坏下半后两半消失且仅掉落一扇门。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void door_drops_once(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        placeDoor(helper, pos);
        BlockPos absolute = helper.absolutePos(pos);
        helper.assertTrue(helper.getLevel().destroyBlock(absolute, true), "应能破坏门的下半");
        helper.assertTrue(helper.getBlockState(pos).isAir() && helper.getBlockState(pos.above()).isAir(),
                "破坏下半后门的两半都应消失");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1));
        helper.assertTrue(drops.size() == 1, "门的两半合计只能产生一个掉落实体，实际 " + drops.size());
        assertStack(helper, drops.getFirst().getItem(), GreekWood.OLIVE_DOOR, 1, "门掉落");
        helper.succeed();
    }

    /** TC-T6b-25：徒手右键能开门，木按钮输出 15 信号并在 30 tick 后复位。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void wood_block_set_behaviour(GameTestHelper helper) {
        FakePlayer player = player(helper);
        BlockPos door = new BlockPos(0, 1, 1);
        placeDoor(helper, door);
        helper.getBlockState(door).useWithoutItem(helper.getLevel(), player, hit(helper, door));
        helper.assertTrue(helper.getBlockState(door).getValue(DoorBlock.OPEN)
                && helper.getBlockState(door.above()).getValue(DoorBlock.OPEN), "右键应打开上下两半门");

        BlockPos button = new BlockPos(2, 1, 1);
        helper.setBlock(button.below(), Blocks.STONE);
        helper.setBlock(button, GreekWood.OLIVE_BUTTON.get().defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.FLOOR));
        BlockPos absolute = helper.absolutePos(button);
        helper.assertTrue(helper.getBlockState(button).getSignal(helper.getLevel(), absolute, Direction.UP) == 0,
                "未按下的按钮不应输出信号");
        helper.getBlockState(button).useWithoutItem(helper.getLevel(), player, hit(helper, button));
        helper.assertTrue(helper.getBlockState(button).getValue(ButtonBlock.POWERED), "右键应按下按钮");
        helper.assertTrue(helper.getBlockState(button).getSignal(helper.getLevel(), absolute, Direction.UP) == 15,
                "按下的按钮应输出 15 级红石信号");
        helper.runAfterDelay(31, () -> {
            helper.assertTrue(!helper.getBlockState(button).getValue(ButtonBlock.POWERED), "木按钮应在 30 tick 后复位");
            helper.assertTrue(helper.getBlockState(button).getSignal(helper.getLevel(), absolute, Direction.UP) == 0,
                    "按钮复位后不应输出信号");
            helper.succeed();
        });
    }

    /** TC-T6b-52：系数转换为实际饱和度后检查，并检查无效果、堆叠数与公共食物标签。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void food_values(GameTestHelper helper) {
        assertFood(helper, GreekFoods.OLIVE_FLATBREAD, 6, 0.6f, 64);
        assertFood(helper, GreekFoods.HONEY_CURD, 4, 0.6f, 16);
        assertFood(helper, GreekFoods.GRAPE_PLATTER, 4, 0.4f, 16);
        assertFood(helper, GreekFoods.GREEK_TRAVEL_MEAL, 8, 0.7f, 64);
        helper.succeed();
    }

    /** TC-T6b-53、54：单份食物通过 finishUsingItem 返回容器；多份时容器进入背包。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void eating_returns_containers(GameTestHelper helper) {
        assertContainer(helper, GreekFoods.HONEY_CURD, Items.BOWL);
        assertContainer(helper, GreekFoods.GRAPE_PLATTER, GreekMaterials.WOODEN_PLATTER);
        helper.succeed();
    }

    /** TC-T6b-55、56：四种食物配方、蜜酪的两种剩余物，以及两种熟鱼与生鱼反例。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void food_recipes(GameTestHelper helper) {
        assertCraft(helper, GreekFoods.OLIVE_FLATBREAD, 1, 2, 2,
                Items.WHEAT, GreekAgriculture.OLIVE, Items.WHEAT, Items.WHEAT);
        assertCraft(helper, GreekFoods.GRAPE_PLATTER, 1, 2, 2,
                GreekAgriculture.GRAPE, GreekMaterials.WOODEN_PLATTER, GreekAgriculture.GRAPE, GreekAgriculture.GRAPE);
        CraftingInput honey = input(3, 1, Items.MILK_BUCKET, Items.HONEY_BOTTLE, Items.BOWL);
        CraftingRecipe recipe = recipe(helper, honey);
        assertStack(helper, recipe.assemble(honey, helper.getLevel().registryAccess()), GreekFoods.HONEY_CURD, 1,
                "蜜酪配方");
        var remaining = recipe.getRemainingItems(honey);
        helper.assertTrue(remaining.size() == 3, "蜜酪剩余物应对应三个合成格");
        assertStack(helper, remaining.get(0), Items.BUCKET, 1, "牛奶桶合成剩余物");
        assertStack(helper, remaining.get(1), Items.GLASS_BOTTLE, 1, "蜂蜜瓶合成剩余物");
        helper.assertTrue(remaining.get(2).isEmpty(), "碗应消耗，不应在合成时返还");
        for (ItemLike fish : List.of(Items.COOKED_COD, Items.COOKED_SALMON)) {
            assertCraft(helper, GreekFoods.GREEK_TRAVEL_MEAL, 1, 2, 1, fish, GreekFoods.OLIVE_FLATBREAD);
        }
        for (ItemLike fish : List.of(Items.COD, Items.SALMON)) {
            CraftingInput raw = input(2, 1, GreekFoods.OLIVE_FLATBREAD, fish);
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, raw,
                    helper.getLevel()).isEmpty(), "生鱼不应能合成行旅餐");
        }
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gt_wood_food"));
        player.getAbilities().instabuild = false;
        player.getFoodData().setFoodLevel(0);
        return player;
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
    }

    private static void placeDoor(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, GreekWood.OLIVE_DOOR.get().defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(pos.above(), GreekWood.OLIVE_DOOR.get().defaultBlockState()
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertBlockPresent(GreekWood.OLIVE_DOOR.get(), pos);
        helper.assertBlockPresent(GreekWood.OLIVE_DOOR.get(), pos.above());
    }

    private static CraftingInput input(int width, int height, ItemLike... items) {
        return CraftingInput.of(width, height, Arrays.stream(items)
                .map(i -> i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i)).toList());
    }

    private static CraftingRecipe recipe(GameTestHelper helper, CraftingInput input) {
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "没有匹配的合成配方：" + input.items());
        return recipe.orElseThrow().value();
    }

    private static void assertCraft(GameTestHelper helper, ItemLike result, int count, int width, int height,
                                    ItemLike... ingredients) {
        CraftingInput input = input(width, height, ingredients);
        assertStack(helper, recipe(helper, input).assemble(input, helper.getLevel().registryAccess()), result, count,
                "合成 " + BuiltInRegistries.ITEM.getKey(result.asItem()));
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, ItemLike item, int count, String context) {
        helper.assertTrue(stack.is(item.asItem()) && stack.getCount() == count,
                context + "：期望 " + count + " × " + BuiltInRegistries.ITEM.getKey(item.asItem()) + "，实际 " + stack);
    }

    private static void assertFood(GameTestHelper helper, ItemLike item, int nutrition, float modifier, int stackSize) {
        ItemStack stack = new ItemStack(item);
        String id = BuiltInRegistries.ITEM.getKey(item.asItem()).toString();
        FoodProperties food = stack.get(DataComponents.FOOD);
        helper.assertTrue(food != null, id + " 缺少食物组件");
        helper.assertTrue(food.nutrition() == nutrition, id + " 营养值不正确");
        // 1.21.1 保存的是实际饱和度（营养 × 系数 × 2），不是 Builder 的系数。
        helper.assertTrue(Math.abs(food.saturation() - nutrition * modifier * 2) < 0.0001f,
                id + " 饱和度系数不正确");
        helper.assertTrue(food.effects().isEmpty(), id + " 不应带状态效果");
        helper.assertTrue(stack.getMaxStackSize() == stackSize, id + " 堆叠上限不正确");
        helper.assertTrue(stack.is(ModTags.cItem("foods")), id + " 应在 c:foods 标签中");
    }

    private static void assertContainer(GameTestHelper helper, ItemLike food, ItemLike container) {
        FakePlayer player = player(helper);
        ItemStack single = new ItemStack(food);
        assertStack(helper, single.finishUsingItem(helper.getLevel(), player), container, 1, "单份食物容器返还");
        helper.assertTrue(single.isEmpty(), "单份食物应已消耗");
        ItemStack multiple = new ItemStack(food, 2);
        assertStack(helper, multiple.finishUsingItem(helper.getLevel(), player), food, 1, "多份食物剩余数量");
        helper.assertTrue(player.getInventory().countItem(container.asItem()) == 1, "多份食物应向背包返还一个容器");
    }
}
