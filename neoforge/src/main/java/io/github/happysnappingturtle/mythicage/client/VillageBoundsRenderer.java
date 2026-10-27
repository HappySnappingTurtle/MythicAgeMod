package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Outline;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 「显示村界」（T24 设计文档 §4.4）：只在客户端画 30 秒，不生成方块。为了站在地面也看得清，沿边界竖一道下实上淡的光幕，
 * 光幕顶上和地面各有一条线。线型区分三类（不只靠颜色）：
 * <ul>
 *   <li>已建村界：金色连续光幕，高 {@value #BUILT_H} 格；</li>
 *   <li>未建规划地块：浅蓝虚线光幕（每 2 格一段），矮一些；</li>
 *   <li>被占地块：橙色连续光幕，地面再画对角叉。</li>
 * </ul>
 * 显示期间动作栏实时说明玩家此刻在村界内还是外（村界决定在这里伤害动物是否计入善恶）。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class VillageBoundsRenderer {
    public static final int DURATION_TICKS = 600;
    private static final double LIFT = 0.02;
    private static final float BUILT_H = 2.5f;
    private static final float PLANNED_H = 1.2f;
    private static final float OCCUPIED_H = 1.8f;
    /** 光幕只画相机周围这么远（远处的边界看不清，也省顶点）。 */
    private static final double RANGE = 128;
    private static AffairsPayloads.Bounds shown;
    /** 已建村界的竖边：z → 这一行所有竖边的 x（升序），用于判断玩家在不在村界内。 */
    private static Map<Integer, int[]> crossings = Map.of();
    private static long until;
    private static final Map<Long, Integer> GROUND = new HashMap<>();

    private VillageBoundsRenderer() {
    }

    public static void show(AffairsPayloads.Bounds payload) {
        shown = payload;
        Map<Integer, List<Integer>> rows = new HashMap<>();
        for (Outline.Edge e : payload.built()) {
            if (!e.alongX()) {
                rows.computeIfAbsent(e.z(), k -> new java.util.ArrayList<>()).add(e.x());
            }
        }
        Map<Integer, int[]> out = new HashMap<>();
        rows.forEach((z, xs) -> {
            int[] a = xs.stream().mapToInt(Integer::intValue).toArray();
            Arrays.sort(a);
            out.put(z, a);
        });
        crossings = out;
        GROUND.clear();
        until = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() + DURATION_TICKS;
    }

    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return shown != null && mc.level != null && mc.level.getGameTime() < until;
    }

    /** 点 (x, z) 在不在已建村界内：向 +x 方向数穿过的竖边，奇数为内。 */
    static boolean inside(double x, double z) {
        int[] xs = crossings.get((int) Math.floor(z));
        if (xs == null) {
            return false;
        }
        int n = 0;
        for (int ex : xs) {
            if (ex > x) {
                n++;
            }
        }
        return n % 2 == 1;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.player == null || mc.level.getGameTime() % 10 != 0) {
            return;
        }
        boolean in = inside(mc.player.getX(), mc.player.getZ());
        long left = (until - mc.level.getGameTime()) / 20;
        mc.gui.setOverlayMessage(Component.translatable(in ? "mythic_age.village.bounds.inside" : "mythic_age.village.bounds.outside",
                left), false);
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        AffairsPayloads.Bounds b = shown;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        // 光幕（两面都画；下实上淡）
        VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        for (Outline.Edge e : b.built()) {
            if (near(e, cam)) {
                curtain(level, quads, m, e, BUILT_H, 1f, 0.82f, 0.3f, 0.42f);
            }
        }
        for (int[] r : b.planned()) {
            for (Outline.Edge e : rect(r)) {
                if (dash(e) && near(e, cam)) {
                    curtain(level, quads, m, e, PLANNED_H, 0.55f, 0.8f, 1f, 0.38f);
                }
            }
        }
        for (int[] r : b.occupied()) {
            for (Outline.Edge e : rect(r)) {
                if (near(e, cam)) {
                    curtain(level, quads, m, e, OCCUPIED_H, 1f, 0.5f, 0.15f, 0.45f);
                }
            }
        }
        buffers.endBatch(RenderType.debugQuads());
        // 线：地面一条、光幕顶一条（顶线让远处也看得出边界的走向）
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Outline.Edge e : b.built()) {
            if (near(e, cam)) {
                edge(level, lines, pose, m, e, 0, 1f, 0.82f, 0.3f);
                edge(level, lines, pose, m, e, BUILT_H, 1f, 0.9f, 0.55f);
            }
        }
        for (int[] r : b.planned()) {
            for (Outline.Edge e : rect(r)) {
                if (dash(e) && near(e, cam)) {
                    edge(level, lines, pose, m, e, 0, 0.55f, 0.8f, 1f);
                    edge(level, lines, pose, m, e, PLANNED_H, 0.7f, 0.9f, 1f);
                }
            }
        }
        for (int[] r : b.occupied()) {
            for (Outline.Edge e : rect(r)) {
                edge(level, lines, pose, m, e, 0, 1f, 0.5f, 0.15f);
                edge(level, lines, pose, m, e, OCCUPIED_H, 1f, 0.65f, 0.3f);
            }
            segment(level, lines, pose, m, r[0], r[1], r[2] + 1, r[3] + 1, 1f, 0.5f, 0.15f);
            segment(level, lines, pose, m, r[2] + 1, r[1], r[0], r[3] + 1, 1f, 0.5f, 0.15f);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /** 虚线：每 2 格画 1 格。 */
    private static boolean dash(Outline.Edge e) {
        return Math.floorMod(e.alongX() ? e.x() : e.z(), 2) == 0;
    }

    private static boolean near(Outline.Edge e, Vec3 cam) {
        return Math.abs(e.x() - cam.x) < RANGE && Math.abs(e.z() - cam.z) < RANGE;
    }

    private static List<Outline.Edge> rect(int[] r) {
        return Outline.rect(new Box(r[0], 0, r[1], r[2], 0, r[3]));
    }

    /** 沿单位边竖一块高 h 的光幕：底边 alpha、顶边透明，两面都画（不受剔除影响）。 */
    private static void curtain(ClientLevel level, VertexConsumer q, Matrix4f m, Outline.Edge e, float h, float r, float g, float b,
                                float a) {
        int x2 = e.alongX() ? e.x() + 1 : e.x();
        int z2 = e.alongX() ? e.z() : e.z() + 1;
        float y1 = (float) ground(level, e.x(), e.z());
        float y2 = (float) ground(level, x2, z2);
        float ax = e.x();
        float az = e.z();
        q.addVertex(m, ax, y1, az).setColor(r, g, b, a);
        q.addVertex(m, x2, y2, z2).setColor(r, g, b, a);
        q.addVertex(m, x2, y2 + h, z2).setColor(r, g, b, 0f);
        q.addVertex(m, ax, y1 + h, az).setColor(r, g, b, 0f);
        q.addVertex(m, ax, y1 + h, az).setColor(r, g, b, 0f);
        q.addVertex(m, x2, y2 + h, z2).setColor(r, g, b, 0f);
        q.addVertex(m, x2, y2, z2).setColor(r, g, b, a);
        q.addVertex(m, ax, y1, az).setColor(r, g, b, a);
    }

    private static void edge(ClientLevel level, VertexConsumer lines, PoseStack pose, Matrix4f m, Outline.Edge e, float dy,
                             float r, float g, float b) {
        int x2 = e.alongX() ? e.x() + 1 : e.x();
        int z2 = e.alongX() ? e.z() : e.z() + 1;
        double y1 = ground(level, e.x(), e.z()) + dy;
        double y2 = ground(level, x2, z2) + dy;
        line(lines, pose, m, e.x(), y1, e.z(), x2, y2, z2, r, g, b);
    }

    /** 对角线：分成 1 格长的小段，每段贴地。 */
    private static void segment(ClientLevel level, VertexConsumer lines, PoseStack pose, Matrix4f m, int x1, int z1, int x2, int z2,
                                float r, float g, float b) {
        int steps = Math.max(1, Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1)));
        double px = x1;
        double pz = z1;
        double py = ground(level, x1, z1);
        for (int i = 1; i <= steps; i++) {
            double nx = x1 + (x2 - x1) * (double) i / steps;
            double nz = z1 + (z2 - z1) * (double) i / steps;
            double ny = ground(level, (int) Math.floor(nx), (int) Math.floor(nz));
            line(lines, pose, m, px, py, pz, nx, ny, nz, r, g, b);
            px = nx;
            py = ny;
            pz = nz;
        }
    }

    /**
     * 方块角 (x, z) 处的地面高度：取周围四列地面的最低者。边界常贴着房子走，取最高会被屋顶顶成一根尖刺；
     * 取最低时光幕立在房外的地上（遇到台阶最多往下差一格）。
     */
    private static double ground(ClientLevel level, int x, int z) {
        int h = Integer.MAX_VALUE;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                h = Math.min(h, column(level, x + dx, z + dz));
            }
        }
        return h + LIFT;
    }

    /**
     * 一列的地面：客户端只有 MOTION_BLOCKING 等高度图（树叶也算），从那里往下跳过树叶与不挡路的方块（草、花），
     * 光幕贴着真正的地面而不是树冠。区块已载入时才缓存。
     */
    private static int column(ClientLevel level, int x, int z) {
        long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
        Integer cached = GROUND.get(key);
        if (cached != null) {
            return cached;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        net.minecraft.core.BlockPos.MutableBlockPos p = new net.minecraft.core.BlockPos.MutableBlockPos(x, y - 1, z);
        for (int i = 0; i < 64 && y > level.getMinBuildHeight(); i++) {
            net.minecraft.world.level.block.state.BlockState st = level.getBlockState(p.setY(y - 1));
            if (st.blocksMotion() && !tree(st)) {
                break;
            }
            y--;
        }
        if (level.hasChunk(x >> 4, z >> 4)) {
            GROUND.put(key, y);
        }
        return y;
    }

    /** 树叶与树干（本模组的树干不在原版 logs 标签里：按「可燃的柱状方块」认）。 */
    private static boolean tree(net.minecraft.world.level.block.state.BlockState st) {
        return st.is(net.minecraft.tags.BlockTags.LEAVES) || st.is(net.minecraft.tags.BlockTags.LOGS)
                || (st.getBlock() instanceof net.minecraft.world.level.block.RotatedPillarBlock && st.ignitedByLava());
    }

    private static void line(VertexConsumer lines, PoseStack pose, Matrix4f m, double x1, double y1, double z1, double x2, double y2,
                             double z2, float r, float g, float b) {
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) {
            return;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        lines.addVertex(m, (float) x1, (float) y1, (float) z1).setColor(r, g, b, 1f).setNormal(pose.last(), nx, ny, nz);
        lines.addVertex(m, (float) x2, (float) y2, (float) z2).setColor(r, g, b, 1f).setNormal(pose.last(), nx, ny, nz);
    }
}
