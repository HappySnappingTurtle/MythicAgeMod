package io.github.happysnappingturtle.mythicage.power;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 神力提交后的逐刻任务（T13 设计文档 §5）：雷击的 15 刻预警、复原的 5 次补血……只在内存，服务器重启即丢弃
 * （这些效果都 ≤ 5 秒；持续几分钟的区域效果走 {@code PowerZones} 存档）。
 */
public final class PowerTasks {
    /** 一个任务；{@link #tick} 返回 false 表示结束。{@code age} 从 1 开始（提交后的第几刻）。 */
    @FunctionalInterface
    public interface Task {
        boolean tick(MinecraftServer server, int age);
    }

    private static final class Running {
        final Task task;
        int age;

        Running(Task task) {
            this.task = task;
        }
    }

    private static final List<Running> RUNNING = new ArrayList<>();
    private static final List<Running> PENDING = new ArrayList<>();

    private PowerTasks() {
    }

    /** 从下一刻起每刻调用一次。 */
    public static synchronized void schedule(Task task) {
        PENDING.add(new Running(task));
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        List<Running> now;
        synchronized (PowerTasks.class) {
            RUNNING.addAll(PENDING);
            PENDING.clear();
            now = List.copyOf(RUNNING);
        }
        List<Running> done = new ArrayList<>();
        for (Running r : now) {
            boolean more;
            try {
                more = r.task.tick(event.getServer(), ++r.age);
            } catch (RuntimeException e) {
                io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.error("神力任务出错，已终止", e);
                more = false;
            }
            if (!more) {
                done.add(r);
            }
        }
        synchronized (PowerTasks.class) {
            RUNNING.removeAll(done);
        }
    }

    public static synchronized void onServerStopped(ServerStoppedEvent event) {
        RUNNING.clear();
        PENDING.clear();
    }

    static synchronized int countForTest() {
        return RUNNING.size() + PENDING.size();
    }
}
