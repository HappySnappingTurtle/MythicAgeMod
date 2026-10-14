package io.github.happysnappingturtle.mythicage.datagen.greek;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.datagen.AgeGatedOutput;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import static io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.*;

/**
 * 希腊配方（08 号 §2 与 {@code 材料物产与配方提案.json} 的 recipe_contract_examples；大理石族按原版石材惯例）。
 * 配方 id 统一写明，熔炉与高炉分开命名，避免同一产物的多条配方互相覆盖。
 */
public final class GreekRecipes {
    private GreekRecipes() {
    }

    public static void build(RecipeOutput out) {
        // tin_smelting：粗锡 → 锡锭（熔炉 200 tick / 高炉 100 tick，经验 0.7，与原版铁相同）
        smeltAndBlast(out, RAW_TIN, TIN_INGOT, 0.7f, "tin_ingot");

        // bronze_mix：铜锭 3 + 锡锭 1 → 青铜混合物 4（希腊古典时代解锁，T6 设计文档 §7）
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, BRONZE_MIX, 4)
                .requires(Tags.Items.INGOTS_COPPER).requires(Tags.Items.INGOTS_COPPER).requires(Tags.Items.INGOTS_COPPER)
                .requires(ModTags.cItem("ingots/tin"))
                .unlockedBy("has_tin_ingot", has(TIN_INGOT))
                .save(AgeGatedOutput.of(out, Civilization.GREEK, Age.CLASSICAL), id("bronze_mix"));

        // bronze_smelting：青铜混合物 → 青铜锭
        smeltAndBlast(out, BRONZE_MIX, BRONZE_INGOT, 0.7f, "bronze_ingot");

        pottery(out);
        marble(out);
    }

    /** 陶器与器物（08 号 json：raw_roof_tile、ceramic_firing、libation、wooden_platter）。 */
    private static void pottery(RecipeOutput out) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, RAW_ROOF_TILE, 4)
                .requires(Items.CLAY_BALL, 4)
                .unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
                .save(out, id("raw_roof_tile"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(RAW_ROOF_TILE), RecipeCategory.MISC, CERAMIC_COMPONENT, 0.1f, 200)
                .unlockedBy("has_raw_roof_tile", has(RAW_ROOF_TILE))
                .save(out, id("ceramic_component_from_smelting"));
        // 奠酒：苹果 + 水瓶 ×2 → 奠酒 ×2（2 个瓶子进、2 个瓶装奠酒出，容器守恒）
        Ingredient water = DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.WATER), Items.POTION);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, LIBATION, 2)
                .requires(Items.APPLE).requires(water).requires(water)
                .unlockedBy("has_apple", has(Items.APPLE))
                .save(out, id("libation"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, WOODEN_PLATTER, 2)
                .requires(ItemTags.WOODEN_SLABS).requires(ItemTags.WOODEN_SLABS)
                .unlockedBy("has_wooden_slab", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(ItemTags.WOODEN_SLABS)))
                .save(out, id("wooden_platter"));
    }

    /**
     * 大理石建筑套件（设计 §5.2）：原石烧制得平滑大理石；平滑 2×2 得方石；原石 2×2 得多边形砌石；
     * 方石 8 块围一颗青金石或陶制构件得回纹饰带；切石机可以切出各自的形状与磨光面。
     */
    private static void marble(RecipeOutput out) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(MARBLE), RecipeCategory.BUILDING_BLOCKS, SMOOTH_MARBLE, 0.1f, 200)
                .unlockedBy("has_marble", has(MARBLE))
                .save(out, id("smooth_marble_from_smelting"));
        square(out, SMOOTH_MARBLE, MARBLE_ASHLAR);
        square(out, MARBLE, POLYGONAL_MARBLE);
        frieze(out, BLUE_MEANDER_MARBLE, Items.LAPIS_LAZULI);
        frieze(out, RED_MEANDER_MARBLE, CERAMIC_COMPONENT);

        slab(out, MARBLE_SLAB, MARBLE);
        stairs(out, MARBLE_STAIRS, MARBLE);
        wall(out, MARBLE_WALL, MARBLE);
        slab(out, SMOOTH_MARBLE_SLAB, SMOOTH_MARBLE);
        slab(out, MARBLE_ASHLAR_SLAB, MARBLE_ASHLAR);
        stairs(out, MARBLE_ASHLAR_STAIRS, MARBLE_ASHLAR);
        wall(out, MARBLE_ASHLAR_WALL, MARBLE_ASHLAR);
        wall(out, POLYGONAL_MARBLE_WALL, POLYGONAL_MARBLE);
        slab(out, POLISHED_MARBLE_SLAB, POLISHED_MARBLE);
        stairs(out, POLISHED_MARBLE_STAIRS, POLISHED_MARBLE);

        // 切石机：原石 → 原石形状与多边形砌石；平滑 → 平滑台阶、方石系、磨光系；方石、磨光、多边形 → 各自形状
        stonecut(out, MARBLE, MARBLE_SLAB, 2);
        stonecut(out, MARBLE, MARBLE_STAIRS, 1);
        stonecut(out, MARBLE, MARBLE_WALL, 1);
        stonecut(out, MARBLE, POLYGONAL_MARBLE, 1);
        stonecut(out, MARBLE, POLYGONAL_MARBLE_WALL, 1);
        stonecut(out, POLYGONAL_MARBLE, POLYGONAL_MARBLE_WALL, 1);
        stonecut(out, SMOOTH_MARBLE, SMOOTH_MARBLE_SLAB, 2);
        for (ItemLike from : new ItemLike[]{SMOOTH_MARBLE, MARBLE_ASHLAR}) {
            if (from == SMOOTH_MARBLE) {
                stonecut(out, from, MARBLE_ASHLAR, 1);
            }
            stonecut(out, from, MARBLE_ASHLAR_SLAB, 2);
            stonecut(out, from, MARBLE_ASHLAR_STAIRS, 1);
            stonecut(out, from, MARBLE_ASHLAR_WALL, 1);
        }
        for (ItemLike from : new ItemLike[]{SMOOTH_MARBLE, POLISHED_MARBLE}) {
            if (from == SMOOTH_MARBLE) {
                stonecut(out, from, POLISHED_MARBLE, 1);
            }
            stonecut(out, from, POLISHED_MARBLE_SLAB, 2);
            stonecut(out, from, POLISHED_MARBLE_STAIRS, 1);
        }
    }

    /** 饰带：8 块大理石方石围住 1 个颜料 → 8 块饰带。 */
    private static void frieze(RecipeOutput out, ItemLike result, ItemLike pigment) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, 8)
                .pattern("###").pattern("#P#").pattern("###")
                .define('#', MARBLE_ASHLAR).define('P', pigment)
                .unlockedBy("has_marble_ashlar", has(MARBLE_ASHLAR))
                .save(out, id(name(result)));
    }

    /** 熔炉与高炉各一条：{@code <name>_from_smelting} 与 {@code <name>_from_blasting}。 */
    public static void smeltAndBlast(RecipeOutput out, ItemLike input, ItemLike result, float xp, String name) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, xp, 200)
                .unlockedBy("has_input", has(input))
                .save(out, id(name + "_from_smelting"));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, xp, 100)
                .unlockedBy("has_input", has(input))
                .save(out, id(name + "_from_blasting"));
    }

    /** 2×2 同种材料 → 4 个。 */
    public static void square(RecipeOutput out, ItemLike base, ItemLike result) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, 4)
                .pattern("##").pattern("##").define('#', base)
                .unlockedBy("has_base", has(base))
                .save(out, id(name(result)));
    }

    /** 台阶：3 个一行 → 6 个。 */
    public static void slab(RecipeOutput out, ItemLike result, ItemLike base) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, 6)
                .pattern("###").define('#', base)
                .unlockedBy("has_base", has(base))
                .save(out, id(name(result)));
    }

    /** 楼梯：6 个楼梯形 → 4 个。 */
    public static void stairs(RecipeOutput out, ItemLike result, ItemLike base) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, 4)
                .pattern("#  ").pattern("## ").pattern("###").define('#', base)
                .unlockedBy("has_base", has(base))
                .save(out, id(name(result)));
    }

    /** 墙：6 个两行 → 6 个。 */
    public static void wall(RecipeOutput out, ItemLike result, ItemLike base) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, result, 6)
                .pattern("###").pattern("###").define('#', base)
                .unlockedBy("has_base", has(base))
                .save(out, id(name(result)));
    }

    /** 切石机：{@code <产物>_from_<原料>_stonecutting}。 */
    public static void stonecut(RecipeOutput out, ItemLike input, ItemLike result, int count) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(input), RecipeCategory.BUILDING_BLOCKS, result, count)
                .unlockedBy("has_input", has(input))
                .save(out, id(name(result) + "_from_" + name(input) + "_stonecutting"));
    }

    public static String name(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }
}
