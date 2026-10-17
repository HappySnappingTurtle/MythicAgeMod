package io.github.happysnappingturtle.mythicage.power;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/**
 * 神力道具（卷轴、可放置道具、投掷物）的基类（T12 设计文档 §2.3）：右键交给 {@link PowerService}——
 * 第一次进入瞄准，第二次确认目标并开始吟唱；潜行 + 右键取消。可堆叠 16。
 */
public class GodPowerItem extends Item {
    private final String powerId;

    public GodPowerItem(String powerId, Properties props) {
        super(props.stacksTo(16));
        this.powerId = powerId;
    }

    public String powerId() {
        return powerId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            PowerService.use(sp, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
