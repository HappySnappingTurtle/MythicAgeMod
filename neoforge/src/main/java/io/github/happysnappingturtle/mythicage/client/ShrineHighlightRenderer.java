package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.shrine.inspect.ShrineSurvey;
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
import org.joml.Matrix4f;

/**
 * 神坛检视的「高亮缺失格」（T8a 设计文档 §4；T24 §5.1、§7.3）：只在客户端画 10 秒，不生成方块。
 * 白框是扫描范围；各组按形状区分（不只靠颜色）：带对角叉的框 = 缺 / 挡，虚线框 = 不合格，细高柱 = 整列（如没有屋顶），
 * 贴地薄片 = 位置（祈祷位），普通框 = 已计入。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class ShrineHighlightRenderer {
    public static final int DURATION_TICKS = 200;
    private static ShrineSurvey shown;
    private static long until;

    private ShrineHighlightRenderer() {
    }

    public static void show(ShrineSurvey payload) {
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
        ShrineSurvey p = shown;
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        LevelRenderer.renderLineBox(pose, lines, new AABB(Vec3.atLowerCornerOf(p.scanMin()),
                Vec3.atLowerCornerOf(p.scanMax()).add(1, 1, 1)), 1f, 1f, 1f, 0.6f);
        for (ShrineSurvey.Highlight h : p.highlights()) {
            float r = ((h.color() >> 16) & 0xFF) / 255f;
            float g = ((h.color() >> 8) & 0xFF) / 255f;
            float b = (h.color() & 0xFF) / 255f;
            for (BlockPos c : h.cells()) {
                switch (h.shape()) {
                    case CELL -> LevelRenderer.renderLineBox(pose, lines, new AABB(c).inflate(0.02), r, g, b, 1f);
                    case CROSS -> {
                        AABB box = new AABB(c).inflate(0.02);
                        LevelRenderer.renderLineBox(pose, lines, box, r, g, b, 1f);
                        cross(pose, lines, box, r, g, b);
                    }
                    case DASHED -> dashed(pose, lines, new AABB(c).inflate(0.02), r, g, b);
                    case COLUMN -> LevelRenderer.renderLineBox(pose, lines, new AABB(c.getX() + 0.3, c.getY(), c.getZ() + 0.3,
                            c.getX() + 0.7, p.scanMax().getY() + 1, c.getZ() + 0.7), r, g, b, 0.9f);
                    case FLAT -> LevelRenderer.renderLineBox(pose, lines, new AABB(c.getX() + 0.1, c.getY(), c.getZ() + 0.1,
                            c.getX() + 0.9, c.getY() + 0.05, c.getZ() + 0.9), r, g, b, 1f);
                }
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /** 四个竖面各画一条对角线。 */
    private static void cross(PoseStack pose, VertexConsumer lines, AABB b, float r, float g, float bl) {
        line(pose, lines, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ, r, g, bl);
        line(pose, lines, b.minX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ, r, g, bl);
        line(pose, lines, b.minX, b.minY, b.minZ, b.minX, b.maxY, b.maxZ, r, g, bl);
        line(pose, lines, b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, r, g, bl);
    }

    /** 虚线框：12 条棱各画两小段。 */
    private static void dashed(PoseStack pose, VertexConsumer lines, AABB b, float r, float g, float bl) {
        double[][] corners = {{b.minX, b.minY, b.minZ}, {b.maxX, b.minY, b.minZ}, {b.maxX, b.minY, b.maxZ}, {b.minX, b.minY, b.maxZ},
                {b.minX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.maxZ}, {b.minX, b.maxY, b.maxZ}};
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            double[] a = corners[e[0]];
            double[] z = corners[e[1]];
            for (double t0 : new double[]{0, 0.6}) {
                double t1 = t0 + 0.3;
                line(pose, lines, a[0] + (z[0] - a[0]) * t0, a[1] + (z[1] - a[1]) * t0, a[2] + (z[2] - a[2]) * t0,
                        a[0] + (z[0] - a[0]) * t1, a[1] + (z[1] - a[1]) * t1, a[2] + (z[2] - a[2]) * t1, r, g, bl);
            }
        }
    }

    private static void line(PoseStack pose, VertexConsumer lines, double x1, double y1, double z1, double x2, double y2, double z2,
                             float r, float g, float b) {
        Matrix4f m = pose.last().pose();
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) {
            return;
        }
        lines.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, 1f).setNormal(pose.last(), nx / len, ny / len, nz / len);
        lines.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, 1f).setNormal(pose.last(), nx / len, ny / len, nz / len);
    }
}
