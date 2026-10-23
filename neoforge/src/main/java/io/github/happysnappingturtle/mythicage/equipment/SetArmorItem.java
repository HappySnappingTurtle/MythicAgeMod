package io.github.happysnappingturtle.mythicage.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/** 带套装说明的护甲件（T17）。耐久按每件绝对值填（倍率可能不是整数，10 号合同 §0）。 */
public class SetArmorItem extends ArmorItem {
    @Nullable
    private final String tip;

    public SetArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, int durability, @Nullable String tip) {
        super(material, type, properties.durability(durability));
        this.tip = tip;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (tip != null) {
            tooltip.add(Component.translatable(tip).withStyle(ChatFormatting.GRAY));
        }
    }
}
