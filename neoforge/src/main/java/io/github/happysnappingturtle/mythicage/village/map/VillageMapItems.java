package io.github.happysnappingturtle.mythicage.village.map;

import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.VillageSpace;
import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenPayloads;
import io.github.happysnappingturtle.mythicage.village.screen.VillageScreenService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Optional;

/**
 * 原版地图上的村庄标记（GA-12 §6，GA-9 D12）：在某个村界内手持已填充的地图潜行右键，就打开村庄界面的地图页，
 * 并给这张地图加上村庄标记（与守护者指路地图同一个神庙图标，记在地图物品上，之后一直在）。原版地图上的标记没有悬停，村名写在地图物品的提示里（§10 I4）。
 */
public final class VillageMapItems {
    /** 地图物品自定义数据里记村名的键：村庄 id → 名字的文本行（主神 id，空串为无主神）。 */
    public static final String TAG = "mythic_age_villages";
    public static final String PREFIX = "mythic_age_village_";

    private VillageMapItems() {
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.FILLED_MAP) || !event.getEntity().isShiftKeyDown()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Optional<VillageRegistry.Village> v = VillageSpace.villageAt(player.serverLevel(), player.blockPosition());
        if (v.isEmpty()) {
            return;
        }
        mark(stack, v.get());
        VillageScreenService.open(player, v.get().id(), VillageScreenPayloads.TAB_MAP);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** 给地图加上这个村子的标记（已有就不重复）；返回是否新加。 */
    public static boolean mark(ItemStack map, VillageRegistry.Village v) {
        String id = PREFIX + v.id();
        CompoundTag data = map.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag villages = data.getCompound(TAG);
        if (villages.contains(v.id().toString())) {
            return false;
        }
        boolean tinted = map.has(net.minecraft.core.component.DataComponents.MAP_COLOR);
        MapItemSavedData.addTargetDecoration(map, v.bell().pos(), id, io.github.happysnappingturtle.mythicage.village.VillageGuide.GREEK_VILLAGE_ICON);
        if (!tinted) {
            // 原版给探险图染色；玩家自己的地图不改外观
            map.remove(net.minecraft.core.component.DataComponents.MAP_COLOR);
        }
        villages.putString(v.id().toString(), v.patron());
        data.put(TAG, villages);
        CustomData.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, map, data);
        return true;
    }

    /** 这张地图上标过的村子（主神 id 列表）。 */
    public static List<String> marked(ItemStack map) {
        CompoundTag villages = map.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(TAG);
        return villages.getAllKeys().stream().sorted().map(villages::getString).toList();
    }

    /** 地图物品提示：标出的村庄。 */
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(Items.FILLED_MAP)) {
            return;
        }
        for (String patron : marked(event.getItemStack())) {
            Component name = patron.isEmpty() ? Component.translatable("mythic_age.village.title.plain")
                    : Component.translatable("mythic_age.village.title", Component.translatable("mythic_age.god." + patron));
            event.getToolTip().add(Component.translatable("mythic_age.village.map.marked", name)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
