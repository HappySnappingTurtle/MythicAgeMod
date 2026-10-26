package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import io.github.happysnappingturtle.mythicage.core.boss.ThreadTrail;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 回程线的服务端记录（T23 §5.1）：线团在背包里、人在某个迷宫范围里时按 {@link ThreadTrail} 记点（每 {@value #SAMPLE} 刻采一次脚下位置），
 * 每 {@value #SEND} 刻把增量发给本人。离开迷宫范围 {@value #FORGET} 刻或下线后清空；从祭坛取线时重新开始。只存在内存里。
 */
public final class ThreadTrails {
    public static final int SAMPLE = 4;
    public static final int SEND = 10;
    public static final int FORGET = 6000;

    private static final class State {
        final ThreadTrail trail = new ThreadTrail();
        int sentEnd;
        int sentVersion;
        long lastInside;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private ThreadTrails() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            tick(p);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        STATES.remove(e.getEntity().getUUID());
    }

    public static void tick(ServerPlayer p) {
        if (p.tickCount % SAMPLE != 0) {
            return;
        }
        long now = p.serverLevel().getGameTime();
        State s = STATES.get(p.getUUID());
        boolean inside = TrialArenas.get(p.server).regionOf(p).isPresent();
        if (inside && p.getInventory().hasAnyMatching(st -> st.is(GreekKnossos.ARIADNE_THREAD.get()))) {
            if (s == null) {
                s = new State();
                STATES.put(p.getUUID(), s);
            }
            s.lastInside = now;
            s.trail.add(p.getX(), p.getY() + 0.1, p.getZ());
        } else if (s != null && now - s.lastInside >= FORGET) {
            STATES.remove(p.getUUID());
            send(p, new ThreadTrailPayload(true, List.of()));
            return;
        }
        if (s != null && p.tickCount % SEND < SAMPLE) {
            flush(p, s);
        }
    }

    private static void flush(ServerPlayer p, State s) {
        if (s.trail.version() != s.sentVersion) {
            s.sentVersion = s.trail.version();
            s.sentEnd = s.trail.end();
            send(p, new ThreadTrailPayload(true, vecs(s.trail.points())));
        } else if (s.trail.end() > s.sentEnd) {
            List<ThreadTrail.Point> add = s.trail.since(Math.max(0, s.sentEnd - 1));
            s.sentEnd = s.trail.end();
            send(p, new ThreadTrailPayload(false, vecs(add)));
        }
    }

    private static void send(ServerPlayer p, ThreadTrailPayload payload) {
        if (p.connection != null) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static List<Vec3> vecs(List<ThreadTrail.Point> pts) {
        List<Vec3> out = new ArrayList<>(pts.size());
        pts.forEach(q -> out.add(new Vec3(q.x(), q.y(), q.z())));
        return out;
    }

    /** 从祭坛取线：从当前位置重新开始。 */
    public static void restart(ServerPlayer p) {
        State s = new State();
        s.lastInside = p.serverLevel().getGameTime();
        s.trail.add(p.getX(), p.getY() + 0.1, p.getZ());
        s.sentVersion = -1;
        STATES.put(p.getUUID(), s);
    }

    /** 只供 GameTest。 */
    public static int pointsForTest(UUID player) {
        State s = STATES.get(player);
        return s == null ? 0 : s.trail.size();
    }

    public static void clearForTest(UUID player) {
        STATES.remove(player);
    }
}
