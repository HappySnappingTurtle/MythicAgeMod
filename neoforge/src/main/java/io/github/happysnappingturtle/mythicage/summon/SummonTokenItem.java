package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonUnitCard;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 召唤符（总纲 §3.6、T14 设计文档 §2.1）：一次性；按住右键吟唱 1 秒，在面前召出单位。上限已满时不吟唱、不消耗；
 * 吟唱中受伤会被打断（{@link SummonService#onPlayerHurt}），召唤符不消耗。谁用谁是主人。
 */
public class SummonTokenItem extends Item {
    public static final int CAST_TICKS = 20;
    private final String card;

    public SummonTokenItem(String card, Properties props) {
        super(props.stacksTo(16));
        this.card = card;
    }

    public String card() {
        return card;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CAST_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            Optional<SummonUnitCard> c = SummonUnits.card(card);
            if (c.isEmpty()) {
                return InteractionResultHolder.fail(stack);
            }
            if (!SummonService.fits(sp, c.get())) {
                sp.displayClientMessage(Component.translatable("mythic_age.summon.cap_full", SummonService.occupied(sp),
                        SummonService.cap(sp)), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level instanceof ServerLevel sl && remaining % 4 == 0) {
            sl.sendParticles(ParticleTypes.ENCHANT, user.getX(), user.getY() + 1.2, user.getZ(), 6, 0.6, 0.6, 0.6, 0.4);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer sp) {
            SummonUnits.card(card).ifPresent(c -> {
                SummonService.Result r = SummonService.summon(sp, c, SummonService.frontOf(sp));
                if (r == SummonService.Result.OK) {
                    if (!sp.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                } else if (r == SummonService.Result.CAP_FULL) {
                    sp.displayClientMessage(Component.translatable("mythic_age.summon.cap_full", SummonService.occupied(sp),
                            SummonService.cap(sp)), true);
                }
            });
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String key = getDescriptionId() + ".tip";
        if (net.minecraft.locale.Language.getInstance().has(key)) {
            lines.add(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("mythic_age.summon.token_tip").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
