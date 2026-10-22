package io.github.happysnappingturtle.mythicage.mixin.client;

import io.github.happysnappingturtle.mythicage.client.ClientSkyEye;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 天眼标出的实体只在主人的客户端上显示描边（T14 §11.4）；不改服务端的发光状态，别的玩家看不到。 */
@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void mythicAge$skyEye(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientSkyEye.marked(entity)) {
            cir.setReturnValue(true);
        }
    }
}
