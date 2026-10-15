package io.github.happysnappingturtle.mythicage.datagen.greek;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.content.greek.GreekFoods;
import io.github.happysnappingturtle.mythicage.content.greek.GreekWood;
import io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple;
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
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.recipe.PressingRecipe;
import net.minecraft.world.item.ItemStack;
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
        agriculture(out);
        woodShapes(out);
        foods(out);
        temple(out);
    }

    /** 神庙构件（T8a）：柱与基础圣火盆远古可做，神像与三角楣石要求希腊古典时代。 */
    private static void temple(RecipeOutput out) {
        // 大理石 1 → 大理石柱 1（切石机）；解锁条件由 stonecut 使用 has(MARBLE)。
        stonecut(out, MARBLE, GreekTemple.MARBLE_PILLAR, 1);

        // 圆石台阶 3 + 煤或木炭 1 + 木棍 2 → 基础圣火盆 1。
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GreekTemple.GREEK_BRAZIER)
                .pattern("SCS").pattern(" S ").pattern("I I")
                .define('S', Items.COBBLESTONE_SLAB).define('C', ItemTags.COALS).define('I', Items.STICK)
                .unlockedBy("has_cobblestone_slab", has(Items.COBBLESTONE_SLAB))
                .save(out, id("greek_brazier"));

        // 大理石 5 + 平滑大理石 2 + 金锭 1 → 神像 1（无序，希腊古典时代）。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, GreekTemple.GREEK_STATUE)
                .requires(MARBLE, 5).requires(SMOOTH_MARBLE, 2).requires(Tags.Items.INGOTS_GOLD)
                .unlockedBy("has_marble", has(MARBLE))
                .save(AgeGatedOutput.of(out, Civilization.GREEK, Age.CLASSICAL), id("greek_statue"));

        // 大理石方石 3 + 陶制构件 1 → 三角楣石 2（有序，希腊古典时代）。
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GreekTemple.GREEK_PEDIMENT, 2)
                .pattern("AAA").pattern(" C ")
                .define('A', MARBLE_ASHLAR).define('C', CERAMIC_COMPONENT)
                .unlockedBy("has_marble_ashlar", has(MARBLE_ASHLAR))
                .save(AgeGatedOutput.of(out, Civilization.GREEK, Age.CLASSICAL), id("greek_pediment"));
    }

    /** 橄榄木族其余八种形态：图案、产量与配方组均沿用原版橡木。 */
    private static void woodShapes(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GreekWood.OLIVE_SLAB, 6)
                .pattern("###").define('#', GreekAgriculture.OLIVE_PLANKS).group("wooden_slab")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_slab"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, GreekWood.OLIVE_STAIRS, 4)
                .pattern("#  ").pattern("## ").pattern("###")
                .define('#', GreekAgriculture.OLIVE_PLANKS).group("wooden_stairs")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_stairs"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GreekWood.OLIVE_FENCE, 3)
                .pattern("W#W").pattern("W#W")
                .define('W', GreekAgriculture.OLIVE_PLANKS).define('#', Items.STICK).group("wooden_fence")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_fence"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, GreekWood.OLIVE_FENCE_GATE)
                .pattern("#W#").pattern("#W#")
                .define('W', GreekAgriculture.OLIVE_PLANKS).define('#', Items.STICK).group("wooden_fence_gate")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_fence_gate"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, GreekWood.OLIVE_DOOR, 3)
                .pattern("##").pattern("##").pattern("##")
                .define('#', GreekAgriculture.OLIVE_PLANKS).group("wooden_door")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_door"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, GreekWood.OLIVE_TRAPDOOR, 2)
                .pattern("###").pattern("###")
                .define('#', GreekAgriculture.OLIVE_PLANKS).group("wooden_trapdoor")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_trapdoor"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, GreekWood.OLIVE_BUTTON)
                .requires(GreekAgriculture.OLIVE_PLANKS).group("wooden_button")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_button"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, GreekWood.OLIVE_PRESSURE_PLATE)
                .pattern("##").define('#', GreekAgriculture.OLIVE_PLANKS).group("wooden_pressure_plate")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_pressure_plate"));
    }

    /** 四种希腊食物（08 号）：蜜酪的桶与玻璃瓶由原版合成剩余物规则返还。 */
    private static void foods(RecipeOutput out) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, GreekFoods.OLIVE_FLATBREAD)
                .requires(Items.WHEAT, 3).requires(GreekAgriculture.OLIVE)
                .unlockedBy("has_olive", has(GreekAgriculture.OLIVE))
                .save(out, id("olive_flatbread"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, GreekFoods.HONEY_CURD)
                .requires(Items.MILK_BUCKET).requires(Items.HONEY_BOTTLE).requires(Items.BOWL)
                .unlockedBy("has_honey_bottle", has(Items.HONEY_BOTTLE))
                .save(out, id("honey_curd"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, GreekFoods.GRAPE_PLATTER)
                .requires(GreekAgriculture.GRAPE, 3).requires(WOODEN_PLATTER)
                .unlockedBy("has_grape", has(GreekAgriculture.GRAPE))
                .save(out, id("grape_platter"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, GreekFoods.GREEK_TRAVEL_MEAL)
                .requires(GreekFoods.OLIVE_FLATBREAD).requires(ModTags.cItem("foods/cooked_fish"))
                .unlockedBy("has_olive_flatbread", has(GreekFoods.OLIVE_FLATBREAD))
                .save(out, id("greek_travel_meal"));
    }

    /** 橄榄木（核心件）、告示牌、船、压榨器、油灯、葡萄架、压榨配方（T6b）。木材族其余形态见 woodShapes。 */
    private static void agriculture(RecipeOutput out) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, GreekAgriculture.OLIVE_PLANKS, 4)
                .requires(GreekAgriculture.OLIVE_LOGS_ITEM).group("planks")
                .unlockedBy("has_log", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(GreekAgriculture.OLIVE_LOGS_ITEM)))
                .save(out, id("olive_planks"));
        woodFromLogs(out, GreekAgriculture.OLIVE_WOOD, GreekAgriculture.OLIVE_LOG);
        woodFromLogs(out, GreekAgriculture.STRIPPED_OLIVE_WOOD, GreekAgriculture.STRIPPED_OLIVE_LOG);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GreekAgriculture.OLIVE_SIGN, 3)
                .pattern("###").pattern("###").pattern(" X ")
                .define('#', GreekAgriculture.OLIVE_PLANKS).define('X', Items.STICK).group("wooden_sign")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_sign"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GreekAgriculture.OLIVE_HANGING_SIGN, 6)
                .pattern("X X").pattern("###").pattern("###")
                .define('#', GreekAgriculture.STRIPPED_OLIVE_LOG).define('X', Items.CHAIN).group("hanging_sign")
                .unlockedBy("has_stripped_logs", has(GreekAgriculture.STRIPPED_OLIVE_LOG))
                .save(out, id("olive_hanging_sign"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, GreekAgriculture.OLIVE_BOAT)
                .pattern("# #").pattern("###").define('#', GreekAgriculture.OLIVE_PLANKS).group("boat")
                .unlockedBy("has_planks", has(GreekAgriculture.OLIVE_PLANKS))
                .save(out, id("olive_boat"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, GreekAgriculture.OLIVE_CHEST_BOAT)
                .requires(net.minecraft.world.level.block.Blocks.CHEST).requires(GreekAgriculture.OLIVE_BOAT).group("chest_boat")
                .unlockedBy("has_boat", has(GreekAgriculture.OLIVE_BOAT))
                .save(out, id("olive_chest_boat"));

        // 08 号 json：olive_press（大理石 3 + 橄榄原木 1 + 木棍 2）
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GreekAgriculture.OLIVE_PRESS)
                .pattern("SLS").pattern("MMM")
                .define('S', Items.STICK).define('L', GreekAgriculture.OLIVE_LOG).define('M', MARBLE)
                .unlockedBy("has_olive_log", has(GreekAgriculture.OLIVE_LOG))
                .save(out, id("olive_press"));
        // greek_oil_lamp：陶制构件 + 橄榄油 + 线 → 2
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, GreekAgriculture.GREEK_OIL_LAMP, 2)
                .requires(CERAMIC_COMPONENT).requires(GreekAgriculture.OLIVE_OIL).requires(Items.STRING)
                .unlockedBy("has_olive_oil", has(GreekAgriculture.OLIVE_OIL))
                .save(out, id("greek_oil_lamp"));
        // libation_oil：水瓶 ×2 + 橄榄油 → 奠酒 ×2
        Ingredient water = DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.WATER), Items.POTION);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, LIBATION, 2)
                .requires(GreekAgriculture.OLIVE_OIL).requires(water).requires(water)
                .unlockedBy("has_olive_oil", has(GreekAgriculture.OLIVE_OIL))
                .save(out, id("libation_from_olive_oil"));
        // 葡萄架：木棍 ×6 → 3
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, GreekAgriculture.GRAPE_TRELLIS, 3)
                .pattern("S S").pattern("S S").pattern("S S").define('S', Items.STICK)
                .unlockedBy("has_grape_cutting", has(GreekAgriculture.GRAPE_CUTTING))
                .save(out, id("grape_trellis"));
        // olive_oil：压榨器，橄榄 ×4 + 陶制构件 → 橄榄油，40 刻
        out.accept(id("olive_oil_from_pressing"), new PressingRecipe(Ingredient.of(GreekAgriculture.OLIVE.get()), 4,
                Ingredient.of(CERAMIC_COMPONENT.get()), new ItemStack(GreekAgriculture.OLIVE_OIL.get()), 40), null);
    }

    /** 原木 ×4（2×2）→ 木头 ×3。 */
    private static void woodFromLogs(RecipeOutput out, ItemLike wood, ItemLike log) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, wood, 3)
                .pattern("##").pattern("##").define('#', log).group("bark")
                .unlockedBy("has_log", has(log))
                .save(out, id(name(wood)));
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
