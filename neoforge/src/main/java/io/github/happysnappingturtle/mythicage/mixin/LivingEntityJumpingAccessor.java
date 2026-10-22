package io.github.happysnappingturtle.mythicage.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 骑手是否按着跳跃键（天马骑乘时按住跳跃上升，T14 §11.4 R3）。 */
@Mixin(LivingEntity.class)
public interface LivingEntityJumpingAccessor {
    @Accessor("jumping")
    boolean mythicAge$isJumping();
}
