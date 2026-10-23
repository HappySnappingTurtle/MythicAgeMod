package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.core.equipment.DrawRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 拉弓时间不同于原版的弓（T17 §4.4）：满弓 fullTicks 刻。只改力度公式里的满弓刻数；拉满之后与原版满弓完全相同。
 * 其余逻辑照抄原版 {@code BowItem.releaseUsing}。
 */
public class DrawBowItem extends BowItem {
    private final int fullTicks;
    @Nullable
    private final String tip;

    public DrawBowItem(Properties properties, int durability, int fullTicks, @Nullable String tip) {
        super(properties.durability(durability));
        this.fullTicks = fullTicks;
        this.tip = tip;
    }

    public int fullTicks() {
        return fullTicks;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty()) {
            return;
        }
        int used = getUseDuration(stack, entity) - timeLeft;
        used = net.neoforged.neoforge.event.EventHooks.onArrowLoose(stack, level, player, used, true);
        if (used < 0) {
            return;
        }
        float power = DrawRules.power(used, fullTicks);
        if (power < 0.1) {
            return;
        }
        List<ItemStack> arrows = draw(stack, ammo, player);
        if (level instanceof ServerLevel server && !arrows.isEmpty()) {
            shoot(server, player, player.getUsedItemHand(), stack, arrows, power * 3.0F, 1.0F, power == 1.0F, null);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (tip != null) {
            tooltip.add(Component.translatable(tip).withStyle(ChatFormatting.GRAY));
        }
    }
}
