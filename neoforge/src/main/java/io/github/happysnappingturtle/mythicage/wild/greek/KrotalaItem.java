package io.github.happysnappingturtle.mythicage.wild.greek;

import io.github.happysnappingturtle.mythicage.entity.stymphalian.StymphalianBirdEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/**
 * 青铜响板 Krotala（01_希腊 W3、T15 D5）：右键摇响，24 格内的斯廷法利斯怪鸟受惊高飞 4 秒；冷却 6 秒。
 * 出处：伪阿波罗多洛斯《书库》2.5.6——雅典娜从赫淮斯托斯处拿来青铜响板交给赫拉克勒斯。
 */
public class KrotalaItem extends Item {
    public static final int COOLDOWN = 120;

    public KrotalaItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_COW_BELL.value(), SoundSource.PLAYERS, 1.5F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2F, 1.8F);
        if (level instanceof ServerLevel sl) {
            StymphalianBirdEntity.startleAround(sl, player.position(), StymphalianBirdEntity.STARTLE_RADIUS);
            player.gameEvent(GameEvent.INSTRUMENT_PLAY);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythic_age.krotala.tip").withStyle(ChatFormatting.GRAY));
    }
}
