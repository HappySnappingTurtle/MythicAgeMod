package io.github.happysnappingturtle.mythicage.content.greek.cyclops;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 马洛涅烈酒（T25a §4.3，D9、D10；《奥德赛》9.196–211：阿波罗祭司马戎所赠、甜如蜜、未兑水的红酒）：
 * 像雪球一样扔出，落地变成一只酒碗（{@link WineBowl}），16 格内的独眼巨人会走过去喝。
 */
public class MaroneanWineItem extends Item {
    public static final int COOLDOWN = 10;

    public MaroneanWineItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5F, 0.6F);
        if (!level.isClientSide) {
            ThrownWine wine = new ThrownWine(level, player);
            wine.setItem(stack.copyWithCount(1));
            wine.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.9F, 1.0F);
            level.addFreshEntity(wine);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
