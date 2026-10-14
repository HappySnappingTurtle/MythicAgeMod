package io.github.happysnappingturtle.mythicage.agent.remote;

import io.github.happysnappingturtle.mythicage.agent.AgentEntrypoint;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.lang.reflect.Method;

/**
 * 打开创造物品栏并切换到指定标签页。标签页 id 依次按本 mod、原版命名空间查找，或写完整的 {@code 命名空间:路径}。
 */
final class CreativeTabs {
    private CreativeTabs() {
    }

    /** @return 失败原因；成功返回 null */
    static String open(String tabId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return "玩家为空";
        }
        CreativeModeTab tab = resolve(tabId);
        if (tab == null) {
            return "未知标签页：" + tabId;
        }
        CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(
                mc.player, mc.player.connection.enabledFeatures(), mc.options.operatorItemsTab().get());
        mc.setScreen(screen);
        try {
            Method selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            selectTab.setAccessible(true);
            selectTab.invoke(screen, tab);
        } catch (ReflectiveOperationException e) {
            AgentEntrypoint.LOG.error("[agent] 切换创造标签页失败", e);
            return "切换标签页失败：" + e;
        }
        return null;
    }

    private static CreativeModeTab resolve(String id) {
        if (id.contains(":")) {
            return BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(ResourceLocation.tryParse(id)).orElse(null);
        }
        CreativeModeTab modTab = BuiltInRegistries.CREATIVE_MODE_TAB
                .getOptional(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, id)).orElse(null);
        if (modTab != null) {
            return modTab;
        }
        return BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(ResourceLocation.withDefaultNamespace(id)).orElse(null);
    }
}
