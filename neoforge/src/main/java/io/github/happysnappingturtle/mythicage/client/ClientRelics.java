package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.client.screen.GreekAltarScreen;
import io.github.happysnappingturtle.mythicage.relic.RelicPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** 客户端：最近收到的神庙供奉栏（T16）。界面打开着时直接刷新。 */
public final class ClientRelics {
    private static RelicPayloads.Shelf latest;

    private ClientRelics() {
    }

    public static void accept(RelicPayloads.Shelf shelf) {
        latest = shelf;
        if (Minecraft.getInstance().screen instanceof GreekAltarScreen s && s.pos().equals(shelf.pos())) {
            s.relicsChanged();
        }
    }

    @Nullable
    public static RelicPayloads.Shelf at(BlockPos pos) {
        return latest != null && latest.pos().equals(pos) ? latest : null;
    }
}
