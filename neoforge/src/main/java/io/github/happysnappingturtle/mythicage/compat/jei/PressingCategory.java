package io.github.happysnappingturtle.mythicage.compat.jei;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.recipe.PressingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/** 橄榄压榨机的 JEI 分类（T6b 留到 T24，§8.1）：原料（含个数）+ 容器 → 产物，写所需时间。 */
final class PressingCategory implements IRecipeCategory<PressingRecipe> {
    static final RecipeType<PressingRecipe> TYPE = RecipeType.create(MythicAgeIds.MOD_ID, "pressing", PressingRecipe.class);
    private final IDrawable icon;

    PressingCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(GreekAgriculture.OLIVE_PRESS.get());
    }

    @Override
    public RecipeType<PressingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("mythic_age.jei.pressing");
    }

    @Override
    public int getWidth() {
        return 116;
    }

    @Override
    public int getHeight() {
        return 44;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PressingRecipe r, IFocusGroup focuses) {
        List<ItemStack> inputs = Arrays.stream(r.ingredient().getItems()).map(s -> s.copyWithCount(r.count())).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 4).setStandardSlotBackground().addItemStacks(inputs);
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 24).setStandardSlotBackground().addIngredients(r.container());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 84, 14).setOutputSlotBackground().addItemStack(r.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, PressingRecipe r, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(Math.max(20, r.time())).setPosition(42, 14);
        builder.addText(Component.translatable("mythic_age.jei.pressing_time", String.format(java.util.Locale.ROOT, "%.1f", r.time() / 20.0)),
                60, 10).setPosition(30, 34);
    }
}
