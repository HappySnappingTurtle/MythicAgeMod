package io.github.happysnappingturtle.mythicage.compat.jei;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.recipe.AgeGatedRecipe;
import io.github.happysnappingturtle.mythicage.recipe.ModRecipes;
import io.github.happysnappingturtle.mythicage.recipe.PressingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * JEI 可选插件（T24 设计文档 §8.1，D11）：受限配方在合成格旁写「需要 希腊 · 古典」；橄榄压榨机有自己的分类。
 * 只在装了 JEI 时被 JEI 发现并加载（{@link JeiPlugin}），本模组不依赖 JEI。
 */
@JeiPlugin
public final class MythicJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(AgeGatedRecipe.class, new AgeGatedExtension());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new PressingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<PressingRecipe> pressing = level.getRecipeManager().getAllRecipesFor(ModRecipes.PRESSING.get()).stream()
                .map(RecipeHolder::value).toList();
        registration.addRecipes(PressingCategory.TYPE, pressing);
        // GA-4 供奉：说明页挂在奥林匹斯祭坛与奠酒上（供品表在服务端数据包里，说明按默认表写）
        net.minecraft.network.chat.Component offering = net.minecraft.network.chat.Component.translatable("mythic_age.jei.offering");
        registration.addIngredientInfo(List.of(
                        new ItemStack(io.github.happysnappingturtle.mythicage.content.greek.temple.GreekTemple.GREEK_ALTAR.get()),
                        new ItemStack(io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials.LIBATION.get())),
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK, offering);
        // GA-2 种葡萄
        registration.addIngredientInfo(List.of(new ItemStack(GreekAgriculture.GRAPE_CUTTING.get()),
                        new ItemStack(GreekAgriculture.GRAPE_TRELLIS.get()), new ItemStack(GreekAgriculture.WILD_GRAPEVINE.get())),
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK, net.minecraft.network.chat.Component.translatable("mythic_age.jei.grape"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(GreekAgriculture.OLIVE_PRESS.get()), PressingCategory.TYPE);
    }
}
