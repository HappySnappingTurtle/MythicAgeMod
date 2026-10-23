package io.github.happysnappingturtle.mythicage.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/** 文明盾牌（T17）：原版盾的格挡逻辑，耐久与修复材料来自数值表与文明材料；格挡后的效果由事件（{@link CounterEvents}）处理。 */
public class ShieldWeaponItem extends ShieldItem implements CounterEvents.CounterShield {
    private final TagKey<Item> repair;
    private final int enchantability;
    private final io.github.happysnappingturtle.mythicage.core.equipment.EquipmentTable.Weapon stats;
    @Nullable
    private final String tip;

    public ShieldWeaponItem(Properties properties, io.github.happysnappingturtle.mythicage.core.equipment.EquipmentTable.Weapon stats,
                            int enchantability, TagKey<Item> repair, @Nullable String tip) {
        super(properties.durability(stats.durability()));
        this.stats = stats;
        this.repair = repair;
        this.enchantability = enchantability;
        this.tip = tip;
    }

    @Override
    public io.github.happysnappingturtle.mythicage.core.equipment.EquipmentTable.Weapon counterStats() {
        return stats;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(repair);
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (tip != null) {
            tooltip.add(Component.translatable(tip).withStyle(ChatFormatting.GRAY));
        }
    }
}
