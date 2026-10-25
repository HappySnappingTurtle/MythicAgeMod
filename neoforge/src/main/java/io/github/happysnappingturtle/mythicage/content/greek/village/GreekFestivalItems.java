package io.github.happysnappingturtle.mythicage.content.greek.village;

import io.github.happysnappingturtle.mythicage.content.Content;
import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 节庆与神谕的两件物品（T19 设计文档 §1.10 J5、J6）：
 * <ul>
 *   <li>泛雅典娜奖瓶：城邦节当天集市商人限量出售；右键倒出橄榄油 {@value PrizeAmphoraItem#OIL}（奖品是瓶里雅典娜圣树的油，
 *       瓶身写着「来自雅典的奖品之一」）；</li>
 *   <li>神谕铅板：神谕所的答案写在说明行里（多多那出土的问卜铅板）。</li>
 * </ul>
 */
public final class GreekFestivalItems {
    public static final DeferredItem<PrizeAmphoraItem> PANATHENAIC_AMPHORA = Content.item("panathenaic_amphora", GREEK,
                    PrizeAmphoraItem::new)
            .properties(p -> p.stacksTo(4))
            .lang("泛雅典娜奖瓶", "Panathenaic Prize Amphora", "パンアテナイア賞杯アンフォラ")
            .register();

    public static final DeferredItem<Item> ORACLE_TABLET = Content.item("oracle_tablet", GREEK)
            .properties(p -> p.stacksTo(1))
            .lang("神谕铅板", "Oracle Lead Tablet", "神託の鉛板")
            .register();

    private GreekFestivalItems() {
    }

    public static void init() {
    }

    public static final class PrizeAmphoraItem extends Item {
        public static final int OIL = 4;

        public PrizeAmphoraItem(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                ItemStack oil = new ItemStack(GreekAgriculture.OLIVE_OIL.get(), OIL);
                if (!player.getInventory().add(oil)) {
                    player.drop(oil, false);
                }
                stack.consume(1, player);
                level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable("item.mythic_age.panathenaic_amphora.inscription")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(Component.translatable("item.mythic_age.panathenaic_amphora.use", OIL)
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
