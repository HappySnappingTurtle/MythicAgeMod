package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.relic.RelicEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 赫菲斯托斯之砧（T25a §7）：铁砧算完结果后，按玩家佩戴的圣物折扣经验消耗（只在服务端改，经验值随 DataSlot 同步给客户端）。
 * 原版「过于昂贵」在算价时就判了（按折前价），这里不会把它变成能做。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow
    @Final
    private DataSlot cost;

    @Inject(method = "createResult", at = @At("RETURN"))
    private void mythicAge$discount(CallbackInfo ci) {
        if (((ItemCombinerMenuAccessor) this).mythicAge$player() instanceof ServerPlayer p) {
            int before = cost.get();
            int after = RelicEvents.anvilCost(p, before);
            if (after != before) {
                cost.set(after);
            }
        }
    }
}
