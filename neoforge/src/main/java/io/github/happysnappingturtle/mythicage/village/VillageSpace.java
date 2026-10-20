package io.github.happysnappingturtle.mythicage.village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

/**
 * 「某点在哪个村」（V0 §5.1、W4，T13b §3.3）：有规划数据的村庄按已建范围判断；
 * 没有规划数据的（原版村庄、升级前登记的）退回钟周围 {@link VillageService#RANGE} 格。
 */
public final class VillageSpace {
    /** 离开判定：已建范围外这么多格内仍算在村里（进村标题不反复弹）。 */
    public static final int LEAVE_MARGIN = 16;

    private VillageSpace() {
    }

    public static Optional<VillageRegistry.Village> villageAt(ServerLevel level, BlockPos pos) {
        VillagePlans plans = VillagePlans.get(level.getServer());
        VillageRegistry reg = VillageRegistry.get(level.getServer());
        Optional<UUID> planned = plans.at(level.dimension(), pos.getX(), pos.getZ());
        if (planned.isPresent()) {
            Optional<VillageRegistry.Village> v = reg.byId(planned.get());
            if (v.isPresent()) {
                return v;
            }
        }
        return VillageService.villageAt(level, pos, VillageService.RANGE, v -> !plans.has(v.id()));
    }

    /** 玩家还算在这个村里吗（离开判定）。 */
    public static boolean stillInside(ServerLevel level, VillageRegistry.Village v, BlockPos pos) {
        VillagePlans plans = VillagePlans.get(level.getServer());
        if (plans.has(v.id())) {
            return plans.within(v.id(), pos.getX(), pos.getZ(), LEAVE_MARGIN);
        }
        double dx = v.bell().pos().getX() - pos.getX();
        double dz = v.bell().pos().getZ() - pos.getZ();
        return dx * dx + dz * dz <= (double) VillageService.LEAVE_RANGE * VillageService.LEAVE_RANGE;
    }
}
