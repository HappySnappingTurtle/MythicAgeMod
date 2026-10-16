package io.github.happysnappingturtle.mythicage.content;

import io.github.happysnappingturtle.mythicage.content.greek.GreekMaterials;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.registry.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 每个文明一个创造模式物品栏（T6 设计文档 §2.3），条目顺序为 {@link Content} 中的声明顺序。
 * 一个文明在 {@link #ICONS} 里有图标才注册物品栏；新文明上线时只需在这里加一行图标。
 */
public final class ModCreativeTabs {
    private static final Map<Civilization, Supplier<ItemStack>> ICONS = new EnumMap<>(Map.of(
            Civilization.GREEK, () -> new ItemStack(GreekMaterials.TIN_INGOT.get())));
    private static final Map<Civilization, DeferredHolder<CreativeModeTab, CreativeModeTab>> TABS =
            new EnumMap<>(Civilization.class);

    static {
        ICONS.forEach((civ, icon) -> TABS.put(civ, ModRegistries.TABS.register(civ.id(), () -> CreativeModeTab.builder()
                .title(Component.translatable(tabKey(civ)))
                .icon(icon)
                .displayItems((params, output) -> Content.entries(civ)
                        .forEach(e -> e.item().ifPresent(i -> output.accept(i.get()))))
                .build())));
    }

    private ModCreativeTabs() {
    }

    public static String tabKey(Civilization civ) {
        return "itemGroup.mythic_age." + civ.id();
    }

    public static Map<Civilization, DeferredHolder<CreativeModeTab, CreativeModeTab>> tabs() {
        return Collections.unmodifiableMap(TABS);
    }

    public static void init() {
    }
}
