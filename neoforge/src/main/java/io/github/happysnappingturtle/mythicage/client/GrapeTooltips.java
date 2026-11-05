package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** 葡萄插条、葡萄架、野葡萄藤的种法说明（GA-2）。 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class GrapeTooltips {
    private GrapeTooltips() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        Item item = e.getItemStack().getItem();
        List<String> keys;
        if (item == GreekAgriculture.GRAPE_CUTTING.get()) {
            keys = List.of("mythic_age.grape.tooltip.cutting", "mythic_age.grape.tooltip.cutting.source");
        } else if (item == GreekAgriculture.GRAPE_TRELLIS.get().asItem()) {
            keys = List.of("mythic_age.grape.tooltip.trellis", "mythic_age.grape.tooltip.trellis.harvest");
        } else if (item == GreekAgriculture.WILD_GRAPEVINE.get().asItem()) {
            keys = List.of("mythic_age.grape.tooltip.wild");
        } else {
            return;
        }
        // 名字在第 1 行，说明插在名字后面
        int at = Math.min(1, e.getToolTip().size());
        for (int i = keys.size() - 1; i >= 0; i--) {
            e.getToolTip().add(at, Component.translatable(keys.get(i)).withStyle(ChatFormatting.GRAY));
        }
    }
}
