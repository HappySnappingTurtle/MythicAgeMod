package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import io.github.happysnappingturtle.mythicage.player.MythData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 神之信物（总纲 §3.4，T23 §7.1）：试炼击败的纪念。击杀时记录已经写入玩家数据，信物本身不承载进度——丢了不影响晋升；
 * 右键只提示「你的试炼已记入」与击败次数，不重复写记录。
 */
public class TrialTokenItem extends Item {
    private final String trial;

    public TrialTokenItem(Properties properties, String trial) {
        super(properties);
        this.trial = trial;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            int kills = MythData.get(sp).trialKills().getOrDefault(trial, 0);
            sp.displayClientMessage(kills > 0 ? Component.translatable("mythic_age.trial.token.recorded", kills)
                    : Component.translatable("mythic_age.trial.token.none"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
