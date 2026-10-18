package io.github.happysnappingturtle.mythicage.client.power;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** 神力表现的几何（T13 设计文档 §1.3）：贴地发光圈、分叉雷束。坐标都是世界坐标（调用方已平移相机）。 */
public final class FxDraw {
    private FxDraw() {
    }

    /**
     * 一张水平的发光贴图（圈、印、符），中心 {@code c}、半径 {@code r}、绕竖轴转 {@code angleDeg}；{@code argb} 调色与透明度。
     */
    public static void flat(PoseStack pose, MultiBufferSource buffers, ResourceLocation tex, Vec3 c, float r, float angleDeg,
                            int argb) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(tex));
        PoseStack.Pose last = pose.last();
        float a = angleDeg * Mth.DEG_TO_RAD;
        float cos = Mth.cos(a) * r;
        float sin = Mth.sin(a) * r;
        float x = (float) c.x;
        float y = (float) c.y;
        float z = (float) c.z;
        // 四个角：(-1,-1) (-1,1) (1,1) (1,-1) 旋转后
        vertex(vc, last, x + (-cos + sin), y, z + (-sin - cos), 0, 0, argb);
        vertex(vc, last, x + (-cos - sin), y, z + (-sin + cos), 0, 1, argb);
        vertex(vc, last, x + (cos - sin), y, z + (sin + cos), 1, 1, argb);
        vertex(vc, last, x + (cos + sin), y, z + (sin - cos), 1, 0, argb);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int argb) {
        vc.addVertex(p, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
    }

    public static int argb(float a, int rgb) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * 分叉雷束：从 {@code top} 到 {@code bottom} 的折线主束（每 1.1 格一折，seed 决定折点），从上段分出 3–4 条向下的
     * 分叉（分叉上还会再分一次）。四层由细到粗、由亮到淡叠加（加色混合），{@code intensity} 0–1 控制整体亮度；
     * 主束白热芯 + 金色外光，分叉淡紫。
     */
    public static void bolt(PoseStack pose, MultiBufferSource buffers, Vec3 top, Vec3 bottom, long seed, float intensity) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose last = pose.last();
        RandomSource r = RandomSource.create(seed);
        double len = top.distanceTo(bottom);
        int n = Math.max(8, (int) (len / 1.1));
        Vec3[] pts = new Vec3[n + 1];
        double ox = 0;
        double oz = 0;
        for (int i = 0; i <= n; i++) {
            Vec3 p = top.lerp(bottom, i / (double) n);
            if (i == 0 || i == n) {
                pts[i] = p;
                continue;
            }
            // 折点做随机游走（有惯性），越靠近落点越收拢，最后一段正中目标
            double pull = 1 - Math.pow(i / (double) n, 3);
            ox = (ox + (r.nextDouble() - 0.5) * 1.1) * 0.8;
            oz = (oz + (r.nextDouble() - 0.5) * 1.1) * 0.8;
            pts[i] = p.add(ox * pull, 0, oz * pull);
        }
        polyline(vc, last, pts, 0.09f, 1f, 0.78f, 0.28f, intensity);
        int forks = 3 + r.nextInt(2);
        for (int f = 0; f < forks; f++) {
            int from = 1 + r.nextInt(Math.max(1, (int) (n * 0.6)));
            Vec3[] fork = branch(pts[from], 4 + r.nextInt(3), r);
            polyline(vc, last, fork, 0.045f, 0.8f, 0.62f, 1f, intensity * 0.85f);
            Vec3[] twig = branch(fork[1 + r.nextInt(fork.length - 2)], 3, r);
            polyline(vc, last, twig, 0.03f, 0.75f, 0.58f, 1f, intensity * 0.7f);
        }
    }

    /** 一条向下斜出的分叉：每段下降约 0.9 格、沿一个随机水平方向偏 0.6 格并带抖动。 */
    private static Vec3[] branch(Vec3 start, int segments, RandomSource r) {
        Vec3[] out = new Vec3[segments + 1];
        out[0] = start;
        double a = r.nextDouble() * Math.PI * 2;
        double dx = Math.cos(a) * 0.6;
        double dz = Math.sin(a) * 0.6;
        for (int i = 1; i <= segments; i++) {
            out[i] = out[i - 1].add(dx + (r.nextDouble() - 0.5) * 0.5, -0.6 - r.nextDouble() * 0.6,
                    dz + (r.nextDouble() - 0.5) * 0.5);
        }
        return out;
    }

    /** 一道直的发光细线（加色），用于修复光线等。 */
    public static void streak(PoseStack pose, MultiBufferSource buffers, Vec3 a, Vec3 b, float width, int rgb, float alpha) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        float red = (rgb >> 16 & 255) / 255f;
        float green = (rgb >> 8 & 255) / 255f;
        float blue = (rgb & 255) / 255f;
        ribbon(vc, pose.last(), a, b, width, 0, red, green, blue, alpha);
        ribbon(vc, pose.last(), a, b, 0, width, red, green, blue, alpha);
    }

    /** 一小段余弧（淡紫，从 {@code from} 向外随机折 3 段）。 */
    public static void arc(PoseStack pose, MultiBufferSource buffers, Vec3 from, long seed, float length, float intensity) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        RandomSource r = RandomSource.create(seed);
        Vec3[] pts = new Vec3[4];
        pts[0] = from;
        double dx = (r.nextDouble() - 0.5) * 2;
        double dz = (r.nextDouble() - 0.5) * 2;
        for (int i = 1; i < 4; i++) {
            pts[i] = pts[i - 1].add(dx * length / 3 + (r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.3) * 0.25,
                    dz * length / 3 + (r.nextDouble() - 0.5) * 0.2);
        }
        polyline(vc, pose.last(), pts, 0.035f, 0.78f, 0.55f, 1f, intensity);
    }

    private static void polyline(VertexConsumer vc, PoseStack.Pose p, Vec3[] pts, float core, float red, float green, float blue,
                                 float intensity) {
        for (int layer = 0; layer < 4; layer++) {
            // 第 0 层是白热的芯，往外逐层变粗、变淡、偏向主色
            float w = core * (layer == 0 ? 0.6f : 1f + layer * 1.6f);
            float a = intensity * (layer == 0 ? 1f : 0.55f / layer);
            float lr = layer == 0 ? 1f : red;
            float lg = layer == 0 ? 0.98f : green;
            float lb = layer == 0 ? 0.9f : blue;
            for (int i = 0; i + 1 < pts.length; i++) {
                ribbon(vc, p, pts[i], pts[i + 1], w, 0, lr, lg, lb, a);
                ribbon(vc, p, pts[i], pts[i + 1], 0, w, lr, lg, lb, a);
            }
        }
    }

    /** 两点间一条带（沿 x 或 z 方向展开），正反各画一次免得被剔除。 */
    private static void ribbon(VertexConsumer vc, PoseStack.Pose p, Vec3 a, Vec3 b, float wx, float wz, float r, float g,
                               float bl, float alpha) {
        float[][] q = {
                {(float) a.x - wx, (float) a.y, (float) a.z - wz},
                {(float) a.x + wx, (float) a.y, (float) a.z + wz},
                {(float) b.x + wx, (float) b.y, (float) b.z + wz},
                {(float) b.x - wx, (float) b.y, (float) b.z - wz}};
        for (int i = 0; i < 4; i++) {
            vc.addVertex(p, q[i][0], q[i][1], q[i][2]).setColor(r, g, bl, alpha);
        }
        for (int i = 3; i >= 0; i--) {
            vc.addVertex(p, q[i][0], q[i][1], q[i][2]).setColor(r, g, bl, alpha);
        }
    }
}
