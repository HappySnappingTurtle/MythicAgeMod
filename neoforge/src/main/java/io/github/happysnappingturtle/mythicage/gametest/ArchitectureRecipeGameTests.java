package io.github.happysnappingturtle.mythicage.gametest;

import com.mojang.authlib.GameProfile;
import io.github.happysnappingturtle.mythicage.content.greek.GreekArchitecture;
import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.player.MythData;
import io.github.happysnappingturtle.mythicage.recipe.AgeGatedRecipe;
import io.github.happysnappingturtle.mythicage.recipe.CraftingContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** T8b 希腊建筑套件配方（TC-T8b-20～23）：六种切石产物、柱头时代门槛及屋顶／马赛克产量。 */
@GameTestHolder(MythicAgeIds.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchitectureRecipeGameTests {
    private static final String BATCH = "architecture_recipes";

    private ArchitectureRecipeGameTests() {
    }

    /** TC-T8b-20：平滑大理石有六条指定的切石配方，每条各产出一个构件。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void smooth_marble_stonecutting(GameTestHelper helper) {
        SingleRecipeInput input = new SingleRecipeInput(new ItemStack(GreekMaterials.SMOOTH_MARBLE));
        var matches = helper.getLevel().getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, input,
                helper.getLevel());
        List<ItemLike> outputs = List.of(GreekArchitecture.MARBLE_COLUMN, GreekArchitecture.MARBLE_COLUMN_BASE,
                GreekArchitecture.MARBLE_ARCHITRAVE, GreekArchitecture.MARBLE_CORNICE,
                GreekArchitecture.TRIGLYPH_FRIEZE, GreekArchitecture.DORIC_CAPITAL);
        for (ItemLike output : outputs) {
            String name = BuiltInRegistries.ITEM.getKey(output.asItem()).getPath();
            ResourceLocation recipeId = id(name + "_from_smooth_marble_stonecutting");
            // 同一原料有多个切石产物，按配方 id 精确选择而非取第一个匹配项。
            var recipes = matches.stream().filter(r -> r.id().equals(recipeId)).toList();
            helper.assertTrue(recipes.size() == 1, recipeId + " 应存在且唯一");
            assertStack(helper, recipes.getFirst().value().assemble(input, helper.getLevel().registryAccess()),
                    output, 1, "切石机 " + recipeId);
        }
        helper.succeed();
    }

    /** TC-T8b-21：爱奥尼柱头要求希腊古典，产物一个；远古玩家无法匹配。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void ionic_capital_requires_classical(GameTestHelper helper) {
        CraftingInput input = input(3, 2,
                GreekMaterials.SMOOTH_MARBLE, Items.AIR, GreekMaterials.SMOOTH_MARBLE,
                Items.AIR, GreekMaterials.MARBLE_ASHLAR, Items.AIR);
        CraftingRecipe recipe = assertCraft(helper, player(helper, Age.CLASSICAL), input,
                GreekArchitecture.IONIC_CAPITAL, 1);
        assertGate(helper, recipe, Age.CLASSICAL);
        assertRejected(helper, player(helper, Age.ARCHAIC), input, "远古玩家不能合成爱奥尼柱头");
        helper.succeed();
    }

    /** TC-T8b-22：科林斯柱头要求希腊英雄，金粒配方产物一个；古典玩家仍无法匹配。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void corinthian_capital_requires_heroic(GameTestHelper helper) {
        CraftingInput input = input(3, 2,
                GreekMaterials.SMOOTH_MARBLE, Items.GOLD_NUGGET, GreekMaterials.SMOOTH_MARBLE,
                Items.AIR, GreekMaterials.MARBLE_ASHLAR, Items.AIR);
        CraftingRecipe recipe = assertCraft(helper, player(helper, Age.HEROIC), input,
                GreekArchitecture.CORINTHIAN_CAPITAL, 1);
        assertGate(helper, recipe, Age.HEROIC);
        assertRejected(helper, player(helper, Age.CLASSICAL), input, "古典玩家不能合成科林斯柱头");
        helper.succeed();
    }

    /** TC-T8b-23：远古屋顶与马赛克配方产量依次为 4、4、6、2、4、4，坡面／平面配方组正确。 */
    @GameTest(template = GameTestTemplates.EMPTY_3X3, batch = BATCH)
    public static void roof_and_mosaic_recipes(GameTestHelper helper) {
        FakePlayer player = player(helper, Age.ARCHAIC);
        ItemLike ceramic = GreekMaterials.CERAMIC_COMPONENT;
        ItemLike tile = GreekArchitecture.TERRACOTTA_ROOF_TILES;
        ItemLike air = Items.AIR;
        CraftingRecipe tiles = assertCraft(helper, player, input(2, 2, ceramic, ceramic, ceramic, ceramic),
                GreekArchitecture.TERRACOTTA_ROOF_TILES, 4);
        CraftingRecipe slope = assertCraft(helper, player, input(3, 3,
                tile, air, air, tile, tile, air, tile, tile, tile), GreekArchitecture.TERRACOTTA_ROOF_STAIRS, 4);
        CraftingRecipe slab = assertCraft(helper, player, input(3, 1, tile, tile, tile),
                GreekArchitecture.TERRACOTTA_ROOF_SLAB, 6);
        CraftingRecipe ridge = assertCraft(helper, player, input(3, 2, air, tile, air, tile, air, tile),
                GreekArchitecture.TERRACOTTA_RIDGE, 2);
        CraftingRecipe antefix = assertCraft(helper, player, input(3, 1, ceramic, Items.BLACK_DYE, ceramic),
                GreekArchitecture.TERRACOTTA_ANTEFIX, 4);
        CraftingRecipe mosaic = assertCraft(helper, player,
                input(2, 2, Items.DIORITE, Items.BLACKSTONE, Items.BLACKSTONE, Items.DIORITE),
                GreekArchitecture.PEBBLE_MOSAIC, 4);
        for (CraftingRecipe recipe : List.of(tiles, slope, slab, ridge, antefix, mosaic)) {
            helper.assertTrue(!(recipe instanceof AgeGatedRecipe), "屋顶与马赛克配方不应包装时代限制");
        }
        for (CraftingRecipe recipe : List.of(tiles, slope, slab, ridge, mosaic)) {
            helper.assertTrue(recipe instanceof ShapedRecipe, "赤陶瓦、坡面、平面、屋脊和马赛克应为有序配方");
        }
        helper.assertTrue(antefix instanceof ShapelessRecipe, "瓦当应为无序配方");
        helper.assertTrue(slope.getGroup().equals("terracotta_roof") && slab.getGroup().equals("terracotta_roof"),
                "赤陶瓦坡面与平面应同属 terracotta_roof 配方组");
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper, Age greekAge) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "t8b_" + greekAge.id()));
        player.getAbilities().instabuild = false;
        MythData.get(player).civ(Civilization.GREEK).setAge(greekAge);
        return player;
    }

    private static CraftingInput input(int width, int height, ItemLike... items) {
        return CraftingInput.of(width, height, Arrays.stream(items)
                .map(item -> item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item)).toList());
    }

    private static CraftingRecipe assertCraft(GameTestHelper helper, FakePlayer player, CraftingInput input,
                                               ItemLike output, int count) {
        ResourceLocation expectedId = BuiltInRegistries.ITEM.getKey(output.asItem());
        CraftingContext.enter(player);
        try {
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent(), expectedId + " 应匹配指定原料与玩家时代");
            var holder = recipe.orElseThrow();
            helper.assertTrue(holder.id().equals(expectedId), "应匹配 " + expectedId + "，实际 " + holder.id());
            assertStack(helper, holder.value().assemble(input, helper.getLevel().registryAccess()), output, count,
                    "合成 " + expectedId);
            return holder.value();
        } finally {
            CraftingContext.exit();
        }
    }

    private static void assertGate(GameTestHelper helper, CraftingRecipe recipe, Age age) {
        helper.assertTrue(recipe instanceof AgeGatedRecipe, "柱头配方应为 AgeGatedRecipe");
        AgeGatedRecipe gated = (AgeGatedRecipe) recipe;
        helper.assertTrue(gated.civilization() == Civilization.GREEK, "柱头配方应要求希腊文明");
        helper.assertTrue(gated.age() == age, "柱头配方应要求 " + age.id() + " 时代");
        helper.assertTrue(gated.inner() instanceof ShapedRecipe, "柱头内部应为有序合成配方");
    }

    private static void assertRejected(GameTestHelper helper, FakePlayer player, CraftingInput input, String message) {
        CraftingContext.enter(player);
        try {
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input,
                    helper.getLevel()).isEmpty(), message);
        } finally {
            CraftingContext.exit();
        }
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, ItemLike item, int count, String context) {
        helper.assertTrue(stack.is(item.asItem()) && stack.getCount() == count,
                context + " 应产出 " + count + " × " + BuiltInRegistries.ITEM.getKey(item.asItem()) + "，实际 " + stack);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }
}
