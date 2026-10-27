package io.github.happysnappingturtle.mythicage.compat.jei;

import io.github.happysnappingturtle.mythicage.recipe.AgeGatedRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;

/** 受限配方（{@code mythic_age:age_gated}）在 JEI 合成格里照内层配方摆放，右下写「需要 希腊 · 古典」（T24 §8.1）。 */
final class AgeGatedExtension implements ICraftingCategoryExtension<AgeGatedRecipe> {
    @Override
    public void setRecipe(RecipeHolder<AgeGatedRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid,
                          IFocusGroup focuses) {
        AgeGatedRecipe r = holder.value();
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        grid.createAndSetOutputs(builder, List.of(r.inner().getResultItem(level.registryAccess())));
        grid.createAndSetIngredients(builder, r.inner().getIngredients(), getWidth(holder), getHeight(holder));
    }

    @Override
    public int getWidth(RecipeHolder<AgeGatedRecipe> holder) {
        return holder.value().inner() instanceof ShapedRecipe s ? s.getWidth() : 0;
    }

    @Override
    public int getHeight(RecipeHolder<AgeGatedRecipe> holder) {
        return holder.value().inner() instanceof ShapedRecipe s ? s.getHeight() : 0;
    }

    @Override
    public void drawInfo(RecipeHolder<AgeGatedRecipe> holder, int width, int height, GuiGraphics g, double mouseX, double mouseY) {
        AgeGatedRecipe r = holder.value();
        Component need = Component.translatable("mythic_age.jei.age_gated", Component.translatable("mythic_age.civ." + r.civilization().id()),
                Component.translatable("mythic_age.age." + r.age().id()));
        var font = Minecraft.getInstance().font;
        g.drawString(font, need, width - font.width(need), height - 9, 0xFF8B5A2B, false);
    }
}
