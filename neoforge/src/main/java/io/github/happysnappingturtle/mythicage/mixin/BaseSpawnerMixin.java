package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.power.SpawnerStall;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 刷怪笼停摆（T13 瘟疫）：停摆时跳过这一刻，生成倒计时不推进。 */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void mythicAge$stall(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (SpawnerStall.stalled(level, pos)) {
            ci.cancel();
        }
    }
}
