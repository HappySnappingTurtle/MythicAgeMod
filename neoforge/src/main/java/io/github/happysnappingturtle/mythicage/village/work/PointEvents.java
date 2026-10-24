package io.github.happysnappingturtle.mythicage.village.work;

import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import io.github.happysnappingturtle.mythicage.village.VillagePlans;
import io.github.happysnappingturtle.mythicage.village.VillageRegistry;
import io.github.happysnappingturtle.mythicage.village.economy.VillageEconomy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 玩家在登记采集点上挖掘，与 NPC 采集共用同一份余量（T19 设计文档 §6.2，11 号 §6）：挖掉采集点那一格时余量减一次采集量。
 */
public final class PointEvents {
    private PointEvents() {
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            consumeAt(level, event.getPos());
        }
    }

    /** 返回是否命中了某个采集点。 */
    public static boolean consumeAt(ServerLevel level, BlockPos pos) {
        Optional<UUID> v = VillagePlans.get(level.getServer()).at(level.dimension(), pos.getX(), pos.getZ());
        if (v.isEmpty()) {
            return false;
        }
        Optional<VillageEconomy.State> s = VillageEconomy.get(level.getServer()).peek(v.get());
        Optional<CivVillageData> data = VillageRegistry.get(level.getServer()).byId(v.get())
                .flatMap(x -> VillageData.civ(x.civ()));
        if (s.isEmpty() || data.isEmpty()) {
            return false;
        }
        long today = MythDays.today(level.getServer());
        for (Map.Entry<String, ResourcePoint> e : s.get().points.entrySet()) {
            ResourcePoint p = e.getValue();
            if (p.pos().x() == pos.getX() && p.pos().y() == pos.getY() && p.pos().z() == pos.getZ()) {
                CivVillageData.PointKind k = data.get().points().get(p.kind());
                int take = k == null ? 1 : k.take();
                e.setValue(p.recover(today).take(take));
                VillageEconomy.get(level.getServer()).setDirty();
                return true;
            }
        }
        return false;
    }
}
