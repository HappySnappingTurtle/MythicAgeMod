package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.core.player.Age;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.recipe.AgeGatedRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

/**
 * 把普通合成配方包成 {@code mythic_age:age_gated} 再输出（T6 设计文档 §7）。
 * 用法：{@code ShapelessRecipeBuilder...save(AgeGatedOutput.of(out, GREEK, CLASSICAL), id)}。
 */
public record AgeGatedOutput(RecipeOutput parent, Civilization civ, Age age) implements RecipeOutput {

    public static RecipeOutput of(RecipeOutput parent, Civilization civ, Age age) {
        return new AgeGatedOutput(parent, civ, age);
    }

    @Override
    public Advancement.Builder advancement() {
        return parent.advancement();
    }

    @Override
    public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                       ICondition... conditions) {
        if (!(recipe instanceof CraftingRecipe crafting)) {
            throw new IllegalArgumentException(id + "：只有合成配方可以设置时代限制");
        }
        parent.accept(id, new AgeGatedRecipe(civ, age, crafting), advancement, conditions);
    }
}
