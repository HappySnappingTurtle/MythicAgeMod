package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.content.greek.knossos.ThreadTrailPayload;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import io.github.happysnappingturtle.mythicage.client.power.FxDraw;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里阿德涅线团的回程线（T23 §5.1）：只画本人的线，一条贴地发光的金线（淡金光晕 + 亮芯，加色；只在客户端，不生成任何方块）。
 * 服务端每 10 刻发增量；replace 时整条重画。离线或换世界时清空。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class ThreadTrailRenderer {
    private static final List<Vec3> POINTS = new ArrayList<>();
    /** 只画离相机这么近的线段。 */
    private static final double VIEW = 48;

    private ThreadTrailRenderer() {
    }

    private static Vec3 pointer;
    private static long pointerUntil;

    /** 阶段二：线团指向 Boss（约 1 秒）。 */
    public static void pointer(io.github.happysnappingturtle.mythicage.boss.BossPointerPayload p) {
        Minecraft mc = Minecraft.getInstance();
        pointer = new Vec3(p.x(), p.y(), p.z());
        pointerUntil = mc.level == null ? 0 : mc.level.getGameTime() + 25;
    }

    public static void accept(ThreadTrailPayload p) {
        if (p.replace()) {
            POINTS.clear();
        }
        POINTS.addAll(p.points());
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        POINTS.clear();
        pointer = null;
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean showPointer = pointer != null && mc.level != null && mc.player != null && mc.level.getGameTime() < pointerUntil;
        if (POINTS.size() < 2 && !showPointer) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        float pulse = 0.8F + 0.2F * (float) Math.sin((mc.level == null ? 0 : mc.level.getGameTime()) * 0.15);
        // 金线：一道淡金的宽光晕 + 一条亮芯（加色发光，贴地 0.1 格）
        for (int i = 1; i < POINTS.size(); i++) {
            Vec3 a = POINTS.get(i - 1);
            Vec3 b = POINTS.get(i);
            if (a.distanceToSqr(cam) > VIEW * VIEW && b.distanceToSqr(cam) > VIEW * VIEW) {
                continue;
            }
            FxDraw.streak(pose, buffers, a, b, 0.11F, 0xE8A93A, 0.28F * pulse);
            FxDraw.streak(pose, buffers, a, b, 0.035F, 0xFFF1B8, 0.85F * pulse);
        }
        if (showPointer) {
            Vec3 from = mc.player.position().add(0, 0.6, 0);
            Vec3 dir = pointer.subtract(from).normalize();
            Vec3 a = from.add(dir.scale(1.0));
            Vec3 b = from.add(dir.scale(3.0));
            FxDraw.streak(pose, buffers, a, b, 0.14F, 0xFFC94A, 0.45F);
            FxDraw.streak(pose, buffers, a, b, 0.05F, 0xFFFFFF, 0.95F);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lightning());
    }
}
