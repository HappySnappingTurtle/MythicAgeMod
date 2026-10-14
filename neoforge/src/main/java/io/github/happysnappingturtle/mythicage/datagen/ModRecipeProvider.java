package io.github.happysnappingturtle.mythicage.datagen;

import io.github.happysnappingturtle.mythicage.datagen.greek.GreekRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

/** 配方汇总：每个文明一个配方类，一条配方一个方法调用，便于与 08 号 json 逐条核对。 */
public final class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        GreekRecipes.build(out);
    }
}
