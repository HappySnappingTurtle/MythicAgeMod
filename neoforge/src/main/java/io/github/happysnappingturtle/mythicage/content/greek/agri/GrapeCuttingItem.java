package io.github.happysnappingturtle.mythicage.content.greek.agri;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * 葡萄插条（GA-2）：种在葡萄架上由 {@link GrapeTrellisBlock#useItemOn} 处理；对着别的方块右键时在动作栏提示种法。
 */
public final class GrapeCuttingItem extends Item {
    public GrapeCuttingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() != null && !ctx.getLevel().isClientSide) {
            boolean trellis = ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock() instanceof GrapeTrellisBlock;
            ctx.getPlayer().displayClientMessage(Component.translatable(
                    trellis ? "mythic_age.grape.hint.planted" : "mythic_age.grape.hint.need_trellis"), true);
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }
}
