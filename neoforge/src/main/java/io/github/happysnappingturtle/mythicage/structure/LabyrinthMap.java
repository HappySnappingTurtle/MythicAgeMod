package io.github.happysnappingturtle.mythicage.structure;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.List;
import java.util.Optional;

/**
 * 克诺索斯地图（T24 设计文档 §8.2，D12）：守护者出售，指向最近的迷宫（结构标签 {@code mythic_age:labyrinth/greek}，
 * 含近出生点的那座）。同原版探险家地图：比例 1:4、预渲染群系、红叉标记。找不到不成交。
 */
public final class LabyrinthMap {
    public static final TagKey<Structure> TAG = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "labyrinth/greek"));
    /** 搜索半径（区块），同原版探险家地图。 */
    public static final int RADIUS_CHUNKS = 100;

    private LabyrinthMap() {
    }

    public static Optional<BlockPos> find(ServerLevel level, BlockPos from) {
        return Optional.ofNullable(level.findNearestMapStructure(TAG, from, RADIUS_CHUNKS, false));
    }

    /** 兑换页上的样品。 */
    public static ItemStack displayStack() {
        ItemStack s = new ItemStack(Items.FILLED_MAP);
        s.set(DataComponents.ITEM_NAME, Component.translatable("mythic_age.knossos_map.offer"));
        return s;
    }

    public static ItemStack map(ServerLevel level, BlockPos from, BlockPos target) {
        ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, target, "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.ITEM_NAME, Component.translatable("mythic_age.knossos_map.name"));
        int dist = (int) Math.round(Math.hypot(target.getX() - from.getX(), target.getZ() - from.getZ()) / 10.0) * 10;
        map.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("mythic_age.knossos_map.lore", dist))));
        return map;
    }
}
