package io.github.happysnappingturtle.mythicage.client.power;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nullable;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端收表现事件（T12 设计文档 §2.7 / §7）：去重、分发给专属或通用表现、记下谁在吟唱（画光环与吟唱条）。
 * 只对一次性的阶段（吟唱开始 / 取消、提交、结束）按 castId + phase 去重；预警和命中可以多次。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class ClientPowerFx {
    private static final Set<PowerFxPayload.Phase> ONCE = Set.of(PowerFxPayload.Phase.CHANNEL_START,
            PowerFxPayload.Phase.CHANNEL_CANCEL, PowerFxPayload.Phase.COMMIT, PowerFxPayload.Phase.END);
    private static final int SEEN_MAX = 256;
    /** 吟唱条在结束后还显示多久（刻）。 */
    static final int LINGER = 10;

    /** 一段吟唱（本地游戏刻）。{@code endedAt} 为 -1 表示还在吟唱。 */
    public record Channel(String castId, String powerId, int caster, long start, int duration, long endedAt, boolean cancelled) {
        double progress(double now) {
            if (cancelled && endedAt >= 0) {
                return Math.min(1, (endedAt - start) / (double) Math.max(1, duration));
            }
            return duration <= 0 ? 1 : Math.min(1, (now - start) / duration);
        }

        Channel end(long now, boolean cancel) {
            return new Channel(castId, powerId, caster, start, duration, now, cancel);
        }
    }

    private static final Map<String, Boolean> SEEN = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > SEEN_MAX;
        }
    };
    private static final Map<Integer, Channel> CHANNELS = new ConcurrentHashMap<>();
    private static final List<ActiveFx> ACTIVE = new ArrayList<>();

    /** 加入一段持续表现（只在客户端主线程调用）。 */
    public static void add(ActiveFx fx) {
        ACTIVE.add(fx);
    }

    /** 提前结束某些持续表现（如雷击预警在命中时结束）。 */
    public static void removeIf(Predicate<ActiveFx> filter) {
        ACTIVE.removeIf(filter);
    }

    private ClientPowerFx() {
    }

    public static void accept(PowerFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !level.dimension().location().equals(p.dimension())) {
            return;
        }
        if (ONCE.contains(p.phase()) && SEEN.put(p.castId() + ":" + p.phase(), Boolean.TRUE) != null) {
            return;
        }
        long now = level.getGameTime();
        switch (p.phase()) {
            case CHANNEL_START -> CHANNELS.put(p.caster(),
                    new Channel(p.castId(), p.powerId(), p.caster(), now, p.duration(), -1, false));
            case CHANNEL_CANCEL, COMMIT -> CHANNELS.computeIfPresent(p.caster(), (k, c) -> c.castId().equals(p.castId())
                    ? c.end(now, p.phase() == PowerFxPayload.Phase.CHANNEL_CANCEL) : c);
            default -> {
            }
        }
        PowerFxRenderer.Registry.get(p.powerId()).orElse(GenericPowerFx.INSTANCE).play(p, level);
    }

    /** 本地玩家的吟唱（吟唱条用）；没有为空。 */
    @Nullable
    public static Channel local() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : CHANNELS.get(mc.player.getId());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (!ACTIVE.isEmpty() && !mc.isPaused()) {
            for (ActiveFx fx : List.copyOf(ACTIVE)) {
                boolean keep;
                try {
                    keep = fx.tick(level, now);
                } catch (RuntimeException e) {
                    io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.error("神力表现出错，已移除", e);
                    keep = false;
                }
                if (!keep) {
                    ACTIVE.remove(fx);
                }
            }
        }
        if (CHANNELS.isEmpty()) {
            return;
        }
        for (Iterator<Channel> it = CHANNELS.values().iterator(); it.hasNext(); ) {
            Channel c = it.next();
            // 结束后留一会儿给吟唱条淡出；服务端没发结束（丢包、换维度）时，超出吟唱刻 2 秒后自行清掉
            if (c.endedAt() >= 0 ? now - c.endedAt() > LINGER : now - c.start() > c.duration() + 40) {
                it.remove();
                continue;
            }
            Entity e = level.getEntity(c.caster());
            // 第一人称时不给自己画光环（粒子会糊在镜头前），自己的进度看吟唱条
            boolean ownFirstPerson = e == mc.player && mc.options.getCameraType().isFirstPerson();
            if (c.endedAt() < 0 && e != null && !ownFirstPerson && !mc.isPaused()) {
                GenericPowerFx.channelTick(level, e, now - c.start(), c.progress(now));
            }
        }
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || ACTIVE.isEmpty() || mc.level == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        for (ActiveFx fx : ACTIVE) {
            fx.render(pose, buffers, now, pt);
        }
        pose.popPose();
        buffers.endBatch();
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
        CHANNELS.clear();
        SEEN.clear();
        ClientPowerAim.clear();
    }
}
