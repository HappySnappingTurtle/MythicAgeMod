package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 「青铜之躯」（05 号 GP-GR-08）：护甲 +6、护甲韧性 +2、击退抗性 +0.3。属性修饰符 id 固定，
 * 再次施放只刷新时长、数值不叠加；随药水效果一起到期、死亡或喝牛奶时移除。
 */
public final class BronzeBodyEffect extends MobEffect {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "bronze_body_" + path);
    }

    public BronzeBodyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC8803A);
        addAttributeModifier(Attributes.ARMOR, id("armor"), 6, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, id("toughness"), 2, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("knockback"), 0.3, AttributeModifier.Operation.ADD_VALUE);
    }
}
