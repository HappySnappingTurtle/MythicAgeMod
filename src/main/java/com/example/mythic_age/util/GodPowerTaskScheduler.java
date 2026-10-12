package com.example.mythic_age.util;

import com.example.mythic_age.MythicAgeMod;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Main-thread tick scheduler for short delayed god-power effects.
 */
public final class GodPowerTaskScheduler {
    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    private GodPowerTaskScheduler() {
    }

    public static void schedule(ServerWorld world, int delayTicks, Runnable task) {
        TASKS.add(new ScheduledTask(
                world.getRegistryKey().getValue().toString(),
                Math.max(0, delayTicks),
                task
        ));
    }

    public static void tick(ServerWorld world) {
        if (TASKS.isEmpty()) {
            return;
        }

        String dimension = world.getRegistryKey().getValue().toString();
        Iterator<ScheduledTask> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            if (!task.dimension.equals(dimension)) {
                continue;
            }
            if (task.delayTicks > 0) {
                task.delayTicks--;
                continue;
            }

            iterator.remove();
            try {
                task.runnable.run();
            } catch (RuntimeException exception) {
                MythicAgeMod.LOGGER.warn("Delayed god-power task failed", exception);
            }
        }
    }

    private static final class ScheduledTask {
        private final String dimension;
        private final Runnable runnable;
        private int delayTicks;

        private ScheduledTask(String dimension, int delayTicks, Runnable runnable) {
            this.dimension = dimension;
            this.delayTicks = delayTicks;
            this.runnable = runnable;
        }
    }
}
