package io.github.happysnappingturtle.mythicage.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 铁砧、锻造台等的使用者（赫菲斯托斯之砧的经验折扣要知道是谁在用铁砧）。 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
    @Accessor("player")
    Player mythicAge$player();
}
