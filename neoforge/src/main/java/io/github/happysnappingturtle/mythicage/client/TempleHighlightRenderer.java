package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.temple.AltarSurveyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * 神坛检视的「高亮缺失格」（T8a 设计文档 §4）：只在客户端画线框，10 秒后消失，不生成任何方块。
 * 白：扫描范围；红：无屋顶的列、神坛头顶的遮挡；绿：台基上的柱；黄：高度不足或贴地的柱；橙：熄灭的圣火盆；蓝：祈祷位。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class TempleHighlightRenderer {
    public static final int DURATION_TICKS = 200;
    private static AltarSurveyPayload shown;
    private static long until;

    private TempleHighlightRenderer() {
    }

    public static void show(AltarSurveyPayload payload) {
        shown = payload;
        until = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() + DURATION_TICKS;
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return shown != null && mc.level != null && mc.level.getGameTime() < until;
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !active()) {
            return;
        }
        AltarSurveyPayload p = shown;
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        box(pose, lines, new AABB(Vec3.atLowerCornerOf(p.scanMin()), Vec3.atLowerCornerOf(p.scanMax()).add(1, 1, 1)),
                1f, 1f, 1f, 0.6f);
        // 无屋顶的列：从柱脚高度画到扫描体顶部的细竖框
        for (BlockPos r : p.uncoveredRoof()) {
            box(pose, lines, new AABB(r.getX() + 0.3, r.getY(), r.getZ() + 0.3, r.getX() + 0.7, p.scanMax().getY() + 1,
                    r.getZ() + 0.7), 1f, 0.2f, 0.2f, 0.9f);
        }
        cells(pose, lines, p.altarCover(), 1f, 0.2f, 0.2f);
        cells(pose, lines, p.countedColumns(), 0.2f, 1f, 0.3f);
        cells(pose, lines, p.shortColumns(), 1f, 0.9f, 0.1f);
        cells(pose, lines, p.unlitBraziers(), 1f, 0.55f, 0.1f);
        for (BlockPos s : p.prayerSlots()) {
            box(pose, lines, new AABB(s.getX() + 0.1, s.getY(), s.getZ() + 0.1, s.getX() + 0.9, s.getY() + 0.05,
                    s.getZ() + 0.9), 0.3f, 0.6f, 1f, 1f);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void cells(PoseStack pose, VertexConsumer lines, List<BlockPos> list, float r, float g, float b) {
        for (BlockPos c : list) {
            box(pose, lines, new AABB(c).inflate(0.02), r, g, b, 1f);
        }
    }

    private static void box(PoseStack pose, VertexConsumer lines, AABB box, float r, float g, float b, float a) {
        LevelRenderer.renderLineBox(pose, lines, box, r, g, b, a);
    }
}
