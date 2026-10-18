package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 神力表现事件的发送（T12 设计文档 §2.7 / §7）：发给追踪该位置区块的玩家，外加施法者自己。
 * 记住 {@code COMMIT} 后仍在持续的表现，玩家开始追踪那个区块时补发一次（只补表现，不补伤害）。
 * 效果代码用 {@link #emit} 发 {@code WARNING} / {@code HIT} / {@code END}。
 */
public final class PowerFx {
    private record Live(PowerFxPayload payload, ServerLevel level, long until) {
    }

    private static final Map<String, Live> LIVE = new ConcurrentHashMap<>();
    /** 只供 GameTest：最近发出的负载（不论发给谁），最多 256 条。 */
    private static final Deque<CustomPacketPayload> RECENT = new ConcurrentLinkedDeque<>();

    private PowerFx() {
    }

    /** 某个位置的表现该发给谁：追踪该区块的玩家 ∪ 施法者。 */
    public static List<ServerPlayer> recipients(ServerLevel level, @Nullable ServerPlayer caster, Vec3 at) {
        List<ServerPlayer> out = new ArrayList<>(level.getChunkSource().chunkMap.getPlayers(
                new ChunkPos(BlockPos.containing(at)), false));
        if (caster != null && !out.contains(caster)) {
            out.add(caster);
        }
        return out;
    }

    static PowerFxPayload payload(ServerPlayer caster, String castId, String powerId, PowerFxPayload.Phase phase,
                                  Vec3 origin, List<? extends Entity> targets, long seed, int duration, String cancelReason,
                                  String tag) {
        return new PowerFxPayload(castId, powerId, phase, caster.getId(), caster.level().dimension().location(), origin,
                targets.stream().map(Entity::getId).toList(), WorldClock.now(caster.server), seed, duration, cancelReason, tag);
    }

    /** 发一份表现；{@code around} 决定按哪个位置找追踪的玩家（吟唱按施法者，提交按原点）。 */
    static void send(ServerPlayer caster, PowerFxPayload p, Vec3 around) {
        for (ServerPlayer to : recipients((ServerLevel) caster.level(), caster, around)) {
            sendTo(to, p);
        }
        if (p.phase() == PowerFxPayload.Phase.COMMIT && p.duration() > 0) {
            LIVE.put(p.castId(), new Live(p, (ServerLevel) caster.level(), p.startTick() + p.duration()));
        } else if (p.phase() == PowerFxPayload.Phase.END) {
            LIVE.remove(p.castId());
        }
    }

    /** 效果代码发表现（{@code WARNING} / {@code HIT} / {@code END}），按原点找接收者。 */
    public static void emit(CastContext ctx, PowerFxPayload.Phase phase, List<? extends Entity> targets) {
        emit(ctx, phase, targets, ctx.origin(), 0, "");
    }

    /** 带子事件、原点与时长的表现（T13 §1.3）：原点可以不是施放原点（如瘟疫里每个刷怪器各发一份）。 */
    public static void emit(CastContext ctx, PowerFxPayload.Phase phase, List<? extends Entity> targets, Vec3 at, int duration,
                            String tag) {
        send(ctx.caster(), payload(ctx.caster(), ctx.castId(), ctx.spec().id(), phase, at, List.copyOf(targets), ctx.seed(),
                duration, "", tag), at);
    }

    /** 目标多于 16 个时分成几份发（同一子事件），客户端逐份处理。 */
    public static void emitAll(CastContext ctx, PowerFxPayload.Phase phase, List<? extends Entity> targets, String tag) {
        if (targets.isEmpty()) {
            return;
        }
        for (int i = 0; i < targets.size(); i += PowerFxPayload.MAX_TARGETS) {
            emit(ctx, phase, targets.subList(i, Math.min(targets.size(), i + PowerFxPayload.MAX_TARGETS)), ctx.origin(), 0, tag);
        }
    }

    static void sendTo(ServerPlayer to, CustomPacketPayload p) {
        RECENT.addLast(p);
        while (RECENT.size() > 256) {
            RECENT.pollFirst();
        }
        PacketDistributor.sendToPlayer(to, p);
    }

    // ───────────── 补播 ─────────────

    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        replay(event.getPlayer(), event.getLevel(), event.getPos());
    }

    /** 给开始追踪 {@code pos} 区块的玩家补发仍在持续的 {@code COMMIT}（持续时间换成剩余时长）；返回补发的 castId。 */
    public static List<String> replay(ServerPlayer player, ServerLevel level, ChunkPos pos) {
        if (LIVE.isEmpty()) {
            return List.of();
        }
        long now = WorldClock.now(level.getServer());
        List<String> sent = new ArrayList<>();
        for (Map.Entry<String, Live> e : LIVE.entrySet()) {
            Live l = e.getValue();
            if (l.until() <= now) {
                LIVE.remove(e.getKey(), l);
                continue;
            }
            if (l.level() == level && new ChunkPos(BlockPos.containing(l.payload().origin())).equals(pos)) {
                sendTo(player, l.payload().withDuration((int) (l.until() - now)));
                sent.add(e.getKey());
            }
        }
        return sent;
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        LIVE.clear();
        RECENT.clear();
    }

    // ───────────── 测试 ─────────────

    /** 只供 GameTest：最近发出的、属于某次施放的负载（按发送顺序）。 */
    public static List<CustomPacketPayload> sentForTest(String castId) {
        List<CustomPacketPayload> out = new ArrayList<>();
        for (CustomPacketPayload p : RECENT) {
            if (p instanceof PowerFxPayload f && f.castId().equals(castId)
                    || p instanceof PowerAimPayload a && a.castId().equals(castId)) {
                out.add(p);
            }
        }
        return out;
    }
}
