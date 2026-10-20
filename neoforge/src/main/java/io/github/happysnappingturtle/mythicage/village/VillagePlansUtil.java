package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.village.gen.VillageInfoPiece;
import net.minecraft.core.BlockPos;

/** 规划数据的小工具。 */
final class VillagePlansUtil {
    private VillagePlansUtil() {
    }

    static BlockPos center(VillagePlans.Plan p) {
        return VillageInfoPiece.block(p.info().center());
    }
}
