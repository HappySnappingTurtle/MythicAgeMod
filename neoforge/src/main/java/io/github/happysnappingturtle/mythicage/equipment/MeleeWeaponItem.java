package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.equipment.EquipmentTable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 文明近战武器（T17 设计文档 §3）：伤害、攻速、触及都来自装备数值表，挂在主手上；
 * 是否横扫由表里的 {@code sweep} 决定（默认不横扫）。攻击扣 1 耐久。
 */
public class MeleeWeaponItem extends Item {
    public static final ResourceLocation REACH_ID = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "weapon_reach");

    private final EquipmentTable.Weapon stats;
    private final int enchantability;
    private final TagKey<Item> repair;
    @Nullable
    private final String tip;

    public MeleeWeaponItem(Properties properties, EquipmentTable.Weapon stats, int enchantability, TagKey<Item> repair,
                           @Nullable String tip) {
        super(properties.durability(stats.durability()).attributes(attributes(stats)));
        this.stats = stats;
        this.enchantability = enchantability;
        this.repair = repair;
        this.tip = tip;
    }

    public static ItemAttributeModifiers attributes(EquipmentTable.Weapon w) {
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        EquipmentTable.damageModifier(w.damage()), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        EquipmentTable.speedModifier(w.speed()), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (w.reach() > 0) {
            b.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH_ID, w.reach(),
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        return b.build();
    }

    public EquipmentTable.Weapon stats() {
        return stats;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return stats.sweep() && ability == ItemAbilities.SWORD_SWEEP;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (tip != null) {
            tooltip.add(Component.translatable(tip).withStyle(ChatFormatting.GRAY));
        }
    }
}
