package io.github.happysnappingturtle.mythicage.mixin;

import io.github.happysnappingturtle.mythicage.entity.stymphalian.StymphalianBirdEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 钟响了（玩家敲、弹射物打中、红石触发）：24 格内的斯廷法利斯怪鸟受惊（T15 §3，赫拉克勒斯第六项功绩）。 */
@Mixin(BellBlock.class)
public abstract class BellBlockMixin {
    @Inject(method = "attemptToRing(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            at = @At("RETURN"))
    private void mythicAge$startleBirds(Entity entity, Level level, BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && level instanceof ServerLevel sl) {
            StymphalianBirdEntity.startleAround(sl, Vec3.atCenterOf(pos), StymphalianBirdEntity.STARTLE_RADIUS);
        }
    }
}
