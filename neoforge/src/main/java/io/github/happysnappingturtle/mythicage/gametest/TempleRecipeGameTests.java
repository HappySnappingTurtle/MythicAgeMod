package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.content.greek.temple.BrazierBlock;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.recipe.AgeGatedRecipe;
import io.github.happysnappingturtle.mythicage.recipe.CraftingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** T8a 神庙构件配方（TC-T8a-90～94）：实际配方查询、物品放置与工作台时代限制。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TempleRecipeGameTests {
    private static final String BATCH = "temple_recipes";

    private TempleRecipeGameTests() {
    }

    /** TC-T8a-90：切石机可用一块大理石制作一根大理石柱。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void marble_pillar_stonecutting(GameTestHelper helper) {
        SingleRecipeInput input = new SingleRecipeInput(new ItemStack(GreekMaterials.MARBLE));
        // 大理石有多种切石产物，不能只取第一个匹配的配方。
        var recipes = helper.getLevel().getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, input,
                helper.getLevel()).stream()
                .filter(r -> r.id().equals(id("marble_pillar_from_marble_stonecutting"))).toList();
        helper.assertTrue(recipes.size() == 1, "大理石柱切石机配方应存在且唯一");
        assertStack(helper, recipes.getFirst().value().assemble(input, helper.getLevel().registryAccess()),
                GreekTemple.MARBLE_PILLAR, 1, "大理石柱切石产物");
        helper.succeed();
    }

    /** TC-T8a-91：远古玩家用煤或木炭都可合成基础圣火盆，通过物品放下后均为熄灭状态。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void brazier_coal_and_charcoal(GameTestHelper helper) {
        FakePlayer player = player(helper, Age.ARCHAIC);
        int x = 0;
        for (ItemLike fuel : List.of(Items.COAL, Items.CHARCOAL)) {
            CraftingInput input = input(3, 3,
                    Items.COBBLESTONE_SLAB, fuel, Items.COBBLESTONE_SLAB,
                    Items.AIR, Items.COBBLESTONE_SLAB, Items.AIR,
                    Items.STICK, Items.AIR, Items.STICK);
            CraftingRecipe recipe = craftingRecipe(helper, player, "greek_brazier", input);
            helper.assertTrue(!(recipe instanceof AgeGatedRecipe), "基础圣火盆不应受时代限制");
            ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
            assertStack(helper, result, GreekTemple.GREEK_BRAZIER, 1, "基础圣火盆合成产物");

            BlockPos below = new BlockPos(x, 0, 1);
            helper.setBlock(below, Blocks.STONE);
            player.setItemInHand(InteractionHand.MAIN_HAND, result);
            BlockPos absolute = helper.absolutePos(below);
            result.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false)));
            helper.assertBlockPresent(GreekTemple.GREEK_BRAZIER.get(), below.above());
            helper.assertTrue(!helper.getBlockState(below.above()).getValue(BrazierBlock.LIT),
                    "煤／木炭配方得到的圣火盆放下时应为 lit=false");
            helper.assertTrue(result.isEmpty(), "放置应消耗一个圣火盆物品");
            x += 2;
        }
        helper.succeed();
    }

    /** TC-T8a-92：神像是希腊古典时代的受限无序配方，八份指定材料产出一尊神像。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void statue_requires_greek_classical(GameTestHelper helper) {
        CraftingInput input = statueInput();
        CraftingRecipe recipe = craftingRecipe(helper, player(helper, Age.CLASSICAL), "greek_statue", input);
        AgeGatedRecipe gated = assertClassicalGate(helper, recipe);
        helper.assertTrue(gated.inner() instanceof ShapelessRecipe, "神像内部应为无序合成配方");
        assertStack(helper, recipe.assemble(input, helper.getLevel().registryAccess()),
                GreekTemple.GREEK_STATUE, 1, "神像合成产物");
        helper.succeed();
    }

    /** TC-T8a-93：三角楣石是希腊古典时代的受限有序配方，方石三块与陶制构件一件产出两块。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void pediment_requires_greek_classical(GameTestHelper helper) {
        CraftingInput input = input(3, 2,
                GreekMaterials.MARBLE_ASHLAR, GreekMaterials.MARBLE_ASHLAR, GreekMaterials.MARBLE_ASHLAR,
                Items.AIR, GreekMaterials.CERAMIC_COMPONENT, Items.AIR);
        CraftingRecipe recipe = craftingRecipe(helper, player(helper, Age.CLASSICAL), "greek_pediment", input);
        AgeGatedRecipe gated = assertClassicalGate(helper, recipe);
        helper.assertTrue(gated.inner() instanceof ShapedRecipe, "三角楣石内部应为有序合成配方");
        assertStack(helper, recipe.assemble(input, helper.getLevel().registryAccess()),
                GreekTemple.GREEK_PEDIMENT, 2, "三角楣石合成产物");
        helper.succeed();
    }

    /** TC-T8a-94：实际工作台中远古无法合成神像且不扣材料；古典能取出一尊并正确消耗原料。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void statue_crafting_menu_age_gate(GameTestHelper helper) {
        BlockPos table = new BlockPos(1, 0, 1);
        helper.setBlock(table, Blocks.CRAFTING_TABLE);
        for (Age age : List.of(Age.ARCHAIC, Age.CLASSICAL)) {
            FakePlayer player = player(helper, age);
            CraftingMenu menu = new CraftingMenu(0, player.getInventory(),
                    ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(table)));
            try {
                CraftingInput input = statueInput();
                for (int i = 0; i < input.size(); i++) {
                    menu.getSlot(i + 1).set(input.getItem(i).copy());
                }
                ItemStack result = menu.getSlot(0).getItem();
                if (age == Age.ARCHAIC) {
                    helper.assertTrue(result.isEmpty(), "远古玩家的神像配方结果栏应为空，实际 " + result);
                    for (int i = 0; i < input.size(); i++) {
                        helper.assertTrue(ItemStack.matches(menu.getSlot(i + 1).getItem(), input.getItem(i)),
                                "远古玩家第 " + (i + 1) + " 格原料不应被扣除或改变");
                    }
                } else {
                    assertStack(helper, result, GreekTemple.GREEK_STATUE, 1, "古典玩家工作台结果");
                    ItemStack taken = menu.getSlot(0).remove(result.getCount());
                    menu.getSlot(0).onTake(player, taken);
                    assertStack(helper, taken, GreekTemple.GREEK_STATUE, 1, "古典玩家取出的神像");
                    for (int slot = 1; slot <= 9; slot++) {
                        helper.assertTrue(menu.getSlot(slot).getItem().isEmpty(),
                                "取出神像后第 " + slot + " 格原料应消耗完毕");
                    }
                }
            } finally {
                menu.removed(player);
            }
        }
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper, Age greekAge) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "t8a_" + greekAge.id()));
        player.getAbilities().instabuild = false;
        MythData.get(player).civ(Civilization.GREEK).setAge(greekAge);
        return player;
    }

    private static CraftingInput statueInput() {
        // 打乱材料位置，避免把无序配方误测成固定图案。
        return input(3, 3,
                GreekMaterials.MARBLE, Items.GOLD_INGOT, GreekMaterials.SMOOTH_MARBLE,
                GreekMaterials.MARBLE, GreekMaterials.SMOOTH_MARBLE, GreekMaterials.MARBLE,
                GreekMaterials.MARBLE, GreekMaterials.MARBLE, Items.AIR);
    }

    private static CraftingInput input(int width, int height, ItemLike... items) {
        return CraftingInput.of(width, height, Arrays.stream(items)
                .map(item -> item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item)).toList());
    }

    private static CraftingRecipe craftingRecipe(GameTestHelper helper, FakePlayer player, String name,
                                                 CraftingInput input) {
        CraftingContext.enter(player);
        try {
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent(), name + " 应能匹配指定材料与玩家时代");
            var holder = recipe.orElseThrow();
            helper.assertTrue(holder.id().equals(id(name)), "应匹配 " + name + "，实际 " + holder.id());
            return holder.value();
        } finally {
            CraftingContext.exit();
        }
    }

    private static AgeGatedRecipe assertClassicalGate(GameTestHelper helper, CraftingRecipe recipe) {
        helper.assertTrue(recipe instanceof AgeGatedRecipe, "配方应为 AgeGatedRecipe");
        AgeGatedRecipe gated = (AgeGatedRecipe) recipe;
        helper.assertTrue(gated.civilization() == Civilization.GREEK, "配方应要求希腊文明");
        helper.assertTrue(gated.age() == Age.CLASSICAL, "配方应要求古典时代");
        return gated;
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, ItemLike item, int count, String context) {
        helper.assertTrue(stack.is(item.asItem()) && stack.getCount() == count,
                context + "应为 " + count + " × " + item.asItem() + "，实际 " + stack);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }
}
