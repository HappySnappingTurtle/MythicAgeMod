package io.github.happysnappingturtle.mythicage.relic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 圣物物品（总纲 §3.8）：没有耐久，堆叠上限 1。实现 {@link ICurioItem}，Curios 自动识别；
 * 能不能戴由 {@link RelicService#canWear} 判断（服务器看绑定表），效果由对账按绑定表挂上，不用 Curios 的属性修饰。
 * 说明文字：{@code item.mythic_age.<id>.worn} / {@code .offered}。
 */
public class RelicItem extends Item implements ICurioItem {
    public RelicItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean canEquip(SlotContext ctx, ItemStack stack) {
        return RelicItems.SLOT.equals(ctx.identifier()) && RelicService.canWear(ctx.entity(), ctx.index(), stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = BuiltInRegistries.ITEM.getKey(this).toLanguageKey("item");
        tooltip.add(Component.translatable("mythic_age.relic.tooltip.worn", Component.translatable(key + ".worn"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mythic_age.relic.tooltip.offered", Component.translatable(key + ".offered"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mythic_age.relic.tooltip.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}
