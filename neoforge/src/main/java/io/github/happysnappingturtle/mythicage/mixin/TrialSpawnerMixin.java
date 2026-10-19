package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.power.SpawnerStall;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 试炼刷怪器停摆（T13 瘟疫）：停摆时跳过这一刻。它的冷却结束与下次生成是绝对时刻（游戏刻），
 * 不往后推的话暂停期间冷却照样走完，所以每停一刻两者各后推一刻。
 */
@Mixin(TrialSpawner.class)
public abstract class TrialSpawnerMixin {
    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true)
    private void mythicAge$stall(ServerLevel level, BlockPos pos, boolean ominous, CallbackInfo ci) {
        if (SpawnerStall.stalled(level, pos)) {
            TrialSpawnerDataAccessor d = (TrialSpawnerDataAccessor) ((TrialSpawner) (Object) this).getData();
            long now = level.getGameTime();
            if (d.mythicAge$getCooldownEndsAt() > now) {
                d.mythicAge$setCooldownEndsAt(d.mythicAge$getCooldownEndsAt() + 1);
            }
            if (d.mythicAge$getNextMobSpawnsAt() > now) {
                d.mythicAge$setNextMobSpawnsAt(d.mythicAge$getNextMobSpawnsAt() + 1);
            }
            ci.cancel();
        }
    }
}
