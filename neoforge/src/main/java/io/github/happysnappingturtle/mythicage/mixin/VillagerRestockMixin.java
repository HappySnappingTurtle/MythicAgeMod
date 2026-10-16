package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverEvents;
import io.github.happysnappingturtle.mythicage.core.greek.prayer.BelieverRules;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 代价 F7（T10 设计文档 §7）：当天祈祷过的有职业村民，每日补货上限从 2 次降为配置值（默认 1）。
 * 只改补货判定，不影响其他工作。上限为 2 时与原版判定完全一致。
 */
@Mixin(Villager.class)
public abstract class VillagerRestockMixin {
    @Shadow
    private int numberOfRestocksToday;
    @Shadow
    private long lastRestockGameTime;

    @Inject(method = "allowedToRestock", at = @At("HEAD"), cancellable = true, require = 0)
    private void mythicAge$prayerRestockLimit(CallbackInfoReturnable<Boolean> cir) {
        Villager self = (Villager) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        int limit = BelieverEvents.restockLimit(self);
        if (limit < BelieverRules.VANILLA_RESTOCKS) {
            cir.setReturnValue(BelieverRules.restockAllowed(numberOfRestocksToday, limit, self.level().getGameTime(),
                    lastRestockGameTime));
        }
    }
}
