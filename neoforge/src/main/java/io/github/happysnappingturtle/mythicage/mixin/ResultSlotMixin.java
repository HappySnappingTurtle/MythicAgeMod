package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.recipe.CraftingContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 取出合成结果时也要有玩家上下文：原版在这一步重新查找配方以计算剩余物，
 * 如果此时受限配方因为「没有玩家」而不匹配，原版会把原料原样留在格子里，等于免费合成（T6 设计文档 §7.2）。
 */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Shadow
    @Final
    private Player player;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void mythicAge$enter(Player taker, ItemStack stack, CallbackInfo ci) {
        CraftingContext.enter(player);
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void mythicAge$exit(Player taker, ItemStack stack, CallbackInfo ci) {
        CraftingContext.exit();
    }
}
