package io.github.happysnappingturtle.mythicage.content;

import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

/** 每个文明一个创造模式物品栏（T6 设计文档 §2.3），条目顺序为 {@link Content} 中的声明顺序。 */
public final class ModCreativeTabs {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GREEK = ModRegistries.TABS.register("greek",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable(tabKey(Civilization.GREEK)))
                    .icon(() -> new ItemStack(GreekMaterials.TIN_INGOT.get()))
                    .displayItems((params, output) -> Content.entries(Civilization.GREEK)
                            .forEach(e -> e.item().ifPresent(i -> output.accept(i.get()))))
                    .build());

    private ModCreativeTabs() {
    }

    public static String tabKey(Civilization civ) {
        return "itemGroup.mythic_age." + civ.id();
    }

    public static void init() {
    }
}
