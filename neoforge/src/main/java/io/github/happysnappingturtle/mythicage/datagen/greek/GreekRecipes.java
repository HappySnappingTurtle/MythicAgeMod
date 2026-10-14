package io.github.happysnappingturtle.mythicage.datagen.greek;

import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.datagen.AgeGatedOutput;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import static io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.BRONZE_INGOT;
import static io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.BRONZE_MIX;
import static io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.RAW_TIN;
import static io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.TIN_INGOT;

/**
 * 希腊配方（08 号 §2 与 {@code 材料物产与配方提案.json} 的 recipe_contract_examples）。
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

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path);
    }

    public static net.minecraft.advancements.Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }
}
