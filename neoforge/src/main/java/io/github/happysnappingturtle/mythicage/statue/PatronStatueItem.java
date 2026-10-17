package io.github.happysnappingturtle.mythicage.statue;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * 主神像道具（T11a 设计文档 §5.2）：对着地面使用，按模板在该处放下整座神像，正面朝向玩家。
 * 道具上记着文明与主神（数据组件 {@code statue_target}）。
 */
public class PatronStatueItem extends Item {
    public PatronStatueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        ItemStack stack = ctx.getItemInHand();
        StatueTarget target = stack.get(PatronStatues.TARGET.get());
        BlockPos clicked = ctx.getClickedPos();
        BlockPos at = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(ctx.getClickedFace());
        PatronStatues.PlaceResult r = target == null ? PatronStatues.PlaceResult.NO_TARGET
                : PatronStatues.place(player, level, target, at, player.getDirection().getOpposite());
        if (r == PatronStatues.PlaceResult.OK) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(Component.translatable(r.langKey()), true);
        return InteractionResult.FAIL;
    }

    @Override
    public Component getName(ItemStack stack) {
        StatueTarget t = stack.get(PatronStatues.TARGET.get());
        return t == null ? super.getName(stack)
                : Component.translatable("item.mythic_age.patron_statue.of", Component.translatable("mythic_age.god." + t.patron()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        StatueTarget t = stack.get(PatronStatues.TARGET.get());
        if (t != null) {
            tooltip.add(Component.translatable("mythic_age.civ." + t.civ().id()));
        }
        tooltip.add(Component.translatable("item.mythic_age.patron_statue.tip"));
    }
}
