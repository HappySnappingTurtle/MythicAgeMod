package io.github.happysnappingturtle.mythicage.village;

import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.VillagePlan;
import io.github.happysnappingturtle.mythicage.guardian.MythDays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 预留地块被玩家占用（V0 §5.3、W5）：整平后的地面之上出现非自然方块（玩家放的东西）→ 地块 blocked 并记下坐标；
 * 清空后恢复。不拆玩家方块。每个 MC 日对附近有玩家的村庄检查一次；采样（每 2 格一列），不逐格扫全地块。
 */
public final class PlotWatch {
    public static final int INTERVAL = 200;
    public static final int PLAYER_RANGE = 128;
    private static final Map<UUID, Long> CHECKED = new HashMap<>();

    private PlotWatch() {
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        long today = MythDays.today(server);
        VillagePlans plans = VillagePlans.get(server);
        for (VillagePlans.Plan p : plans.all()) {
            ServerLevel level = server.getLevel(p.dim());
            if (level == null || CHECKED.getOrDefault(p.village(), -1L) == today) {
                continue;
            }
            BlockPos c = VillagePlansUtil.center(p);
            boolean near = level.players().stream().anyMatch(pl -> pl.blockPosition().closerThan(c, PLAYER_RANGE));
            if (near) {
                check(level, p);
                CHECKED.put(p.village(), today);
            }
        }
    }

    /** 检查全部预留地块（GameTest 直接调用）。返回被占用的数量。 */
    public static int check(ServerLevel level, VillagePlans.Plan p) {
        VillagePlans plans = VillagePlans.get(level.getServer());
        int n = 0;
        for (VillagePlan.Plot plot : p.info().plots()) {
            Optional<BlockPos> conflict = conflict(level, plot.box());
            plans.setBlocked(p.village(), plot.id(), conflict);
            n += conflict.isPresent() ? 1 : 0;
        }
        return n;
    }

    static Optional<BlockPos> conflict(ServerLevel level, Box b) {
        int ground = b.minY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = b.minX(); x <= b.maxX(); x += 2) {
            for (int z = b.minZ(); z <= b.maxZ(); z += 2) {
                if (!level.isLoaded(m.set(x, ground, z))) {
                    continue;
                }
                boolean corner = (x == b.minX() || x == b.maxX()) && (z == b.minZ() || z == b.maxZ());
                for (int y = ground; y <= ground + 3; y++) {
                    BlockState s = level.getBlockState(m.set(x, y, z));
                    if (corner && y == ground) {
                        continue; // 界桩
                    }
                    if (!s.isAir() && !natural(s)) {
                        return Optional.of(m.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** 自然变化不算占用：草、花、雪、树叶、树苗、自然地面。 */
    private static boolean natural(BlockState s) {
        return s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.DIRT) || s.is(BlockTags.LOGS);
    }

    public static void clearForTest() {
        CHECKED.clear();
    }
}
