package io.github.happysnappingturtle.mythicage.content.greek.village.soldier;

import io.github.happysnappingturtle.mythicage.content.Content;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

import static io.github.happysnappingturtle.mythicage.core.player.Civilization.GREEK;

/**
 * 希腊士兵的兵装与投矛（T20 设计文档 §10.4）：村库里的库存单位，不能穿、不能用（玩家可以捐给村库）。
 */
public final class GreekSoldierItems {
    public static final DeferredItem<SoldierGearItem> HOPLITE_KIT = gear("hoplite_kit", 16,
            "盾矛兵装", "Hoplite Gear", "重装歩兵の兵装");
    public static final DeferredItem<SoldierGearItem> HOPLITE_KIT_SIMPLE = gear("hoplite_kit_simple", 16,
            "简易盾矛兵装", "Simple Hoplite Gear", "簡易な重装歩兵の兵装");
    public static final DeferredItem<SoldierGearItem> HOPLITE_KIT_BROKEN = gear("hoplite_kit_broken", 16,
            "破损的盾矛兵装", "Damaged Hoplite Gear", "傷んだ重装歩兵の兵装");
    public static final DeferredItem<SoldierGearItem> JAVELINEER_KIT = gear("javelineer_kit", 16,
            "投矛兵装", "Javelineer Gear", "投槍兵の兵装");
    public static final DeferredItem<SoldierGearItem> JAVELINEER_KIT_SIMPLE = gear("javelineer_kit_simple", 16,
            "简易投矛兵装", "Simple Javelineer Gear", "簡易な投槍兵の兵装");
    public static final DeferredItem<SoldierGearItem> JAVELINEER_KIT_BROKEN = gear("javelineer_kit_broken", 16,
            "破损的投矛兵装", "Damaged Javelineer Gear", "傷んだ投槍兵の兵装");
    public static final DeferredItem<SoldierGearItem> JAVELIN = gear("javelin", 64, "投矛", "Javelin", "投げ槍");
    public static final DeferredItem<SoldierGearItem> ROUGH_JAVELIN = gear("rough_javelin", 64, "粗投矛", "Rough Javelin", "粗い投げ槍");

    private GreekSoldierItems() {
    }

    private static DeferredItem<SoldierGearItem> gear(String id, int stack, String zh, String en, String ja) {
        return Content.item(id, GREEK, SoldierGearItem::new).properties(p -> p.stacksTo(stack)).lang(zh, en, ja).register();
    }

    public static void init() {
    }

    /** 兵装：说明行写「供本村士兵使用」。 */
    public static final class SoldierGearItem extends Item {
        public SoldierGearItem(Properties p) {
            super(p);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable("item.mythic_age.soldier_gear.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }
}
