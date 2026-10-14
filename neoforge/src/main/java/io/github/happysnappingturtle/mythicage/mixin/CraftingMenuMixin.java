package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.recipe.CraftingContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 合成格计算结果时记下玩家（T6 设计文档 §7.2）。工作台与玩家背包的 2×2 合成格都走这个静态方法。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"))
    private static void mythicAge$enter(AbstractContainerMenu menu, Level level, Player player,
                                        CraftingContainer craftSlots, ResultContainer resultSlots,
                                        RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        CraftingContext.enter(player);
    }

    @Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
    private static void mythicAge$exit(AbstractContainerMenu menu, Level level, Player player,
                                       CraftingContainer craftSlots, ResultContainer resultSlots,
                                       RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        CraftingContext.exit();
    }
}
