package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.power.ShieldPool;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * 神力护盾（T12 设计文档 §2.6；05 号 §1.2）：大型神力提交时由框架自动登记（原点、到期 = 提交 + 持续上界），
 * 任何大型神力确认与提交时检查。只在内存：大型神力都不超过 5 分钟，重启后清空可以接受。
 */
public final class PowerShields {
    private static final ShieldPool POOL = new ShieldPool();

    private PowerShields() {
    }

    /** 还要等多少刻；0 = 可以放。 */
    public static long remaining(ServerLevel level, Vec3 origin) {
        return POOL.remaining(level.dimension().location().toString(), origin.x, origin.z, WorldClock.now(level.getServer()));
    }

    public static void add(ServerLevel level, Vec3 origin, long durationTicks) {
        POOL.add(new ShieldPool.Shield(level.dimension().location().toString(), origin.x, origin.z,
                PowerService.config().shieldRadius(), WorldClock.now(level.getServer()) + Math.max(1, durationTicks)));
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        POOL.clear();
    }

    /** 只供 GameTest。 */
    public static void clearForTest() {
        POOL.clear();
    }
}
