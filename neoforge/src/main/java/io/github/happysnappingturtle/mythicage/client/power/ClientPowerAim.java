package io.github.happysnappingturtle.mythicage.client.power;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.PowerAimPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nullable;

/**
 * 瞄准预览（T12 设计文档 §7）：几何每帧在客户端算，合法性用服务端发来的悬停判定。
 * 选实体：给准星下的实体画框（金 = 合法，红 = 不合法，白 = 服务端还没判）；
 * 选点：在落点（以自身为中心时在脚下）画范围圈，没有半径时画小标记，超出距离为灰色；选方块：在放置格画框。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class ClientPowerAim {
    private static final float[] GOLD = {1f, 0.8f, 0.2f};
    private static final float[] RED = {1f, 0.2f, 0.15f};
    private static final float[] WHITE = {1f, 1f, 1f};
    private static final float[] GRAY = {0.55f, 0.55f, 0.55f};
    private static final int SEGMENTS = 48;

    @Nullable
    private static PowerAimPayload aiming;
    private static int hoverId = -1;
    private static boolean hoverValid;

    private ClientPowerAim() {
    }

    public static void accept(PowerAimPayload p) {
        switch (p.kind()) {
            case START -> {
                aiming = p;
                hoverId = -1;
                hoverValid = false;
            }
            case HOVER -> {
                if (aiming != null && aiming.castId().equals(p.castId())) {
                    hoverId = p.entityId();
                    hoverValid = p.valid();
                }
            }
            case STOP -> {
                if (aiming != null && aiming.castId().equals(p.castId())) {
                    clear();
                }
            }
        }
    }

    @Nullable
    public static PowerAimPayload aiming() {
        return aiming;
    }

    static void clear() {
        aiming = null;
        hoverId = -1;
        hoverValid = false;
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        PowerAimPayload a = aiming;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || a == null || player == null
                || mc.level == null) {
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Vec3 eye = player.getEyePosition(pt);
        Vec3 look = player.getViewVector(pt);
        Vec3 end = eye.add(look.scale(a.range()));
        BlockHitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean miss = block.getType() == HitResult.Type.MISS;
        switch (a.pick()) {
            case ENTITY -> {
                double reach = miss ? a.range() : block.getLocation().distanceTo(eye);
                EntityHitResult hit = ProjectileUtil.getEntityHitResult(mc.level, player, eye, eye.add(look.scale(reach)),
                        player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0),
                        e -> !e.isSpectator() && e.isPickable() && e != player);
                if (hit != null) {
                    Entity e = hit.getEntity();
                    AABB box = e.getBoundingBox().move(e.getPosition(pt).subtract(e.position())).inflate(0.05);
                    float[] c = hoverId != e.getId() ? WHITE : hoverValid ? GOLD : RED;
                    LevelRenderer.renderLineBox(pose, lines, box, c[0], c[1], c[2], 1f);
                }
            }
            case POINT -> {
                Vec3 center = a.selfCentered() ? player.getPosition(pt) : miss ? end : block.getLocation();
                float[] c = !a.selfCentered() && miss ? GRAY : GOLD;
                if (a.radius() > 0) {
                    PowerFxRenderer.AimStyle style = PowerFxRenderer.Registry.get(a.powerId())
                            .map(PowerFxRenderer::aimStyle).orElse(null);
                    if (style != null && c != GRAY) {
                        long t = mc.level.getGameTime();
                        FxDraw.flat(pose, buffers, style.ring(), center.add(0, 0.06, 0), a.radius(), (t + pt) * 0.4f,
                                FxDraw.argb(0.9f, 0xFFFFFF));
                        float[] sc = {(style.rgb() >> 16 & 255) / 255f, (style.rgb() >> 8 & 255) / 255f, (style.rgb() & 255) / 255f};
                        c = sc;
                    }
                    // 画过贴图后换了缓冲，线框缓冲要重新取
                    circle(pose, buffers.getBuffer(RenderType.lines()), center.add(0, 0.05, 0), a.radius(), c);
                } else {
                    LevelRenderer.renderLineBox(pose, lines, new AABB(center, center).inflate(0.25), c[0], c[1], c[2], 1f);
                }
            }
            case BLOCK -> {
                BlockPos place = block.getBlockPos().relative(block.getDirection());
                boolean custom = !miss && PowerFxRenderer.Registry.get(a.powerId())
                        .map(r -> r.renderPlacement(pose, buffers, mc.level, place, mc.level.getGameTime(), pt)).orElse(false);
                if (!miss && !custom) {
                    AABB cell = new AABB(place).inflate(0.002);
                    LevelRenderer.renderLineBox(pose, lines, cell, GOLD[0], GOLD[1], GOLD[2], 0.9f);
                    LevelRenderer.renderLineBox(pose, lines, cell.deflate(0.15), GOLD[0], GOLD[1], GOLD[2], 0.35f);
                }
            }
        }
        pose.popPose();
        buffers.endBatch();
    }

    private static void circle(PoseStack pose, VertexConsumer lines, Vec3 c, float r, float[] color) {
        PoseStack.Pose last = pose.last();
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = Math.PI * 2 * i / SEGMENTS;
            double a1 = Math.PI * 2 * (i + 1) / SEGMENTS;
            float x0 = (float) (c.x + Math.cos(a0) * r);
            float z0 = (float) (c.z + Math.sin(a0) * r);
            float x1 = (float) (c.x + Math.cos(a1) * r);
            float z1 = (float) (c.z + Math.sin(a1) * r);
            float nx = x1 - x0;
            float nz = z1 - z0;
            float len = (float) Math.sqrt(nx * nx + nz * nz);
            nx /= len;
            nz /= len;
            lines.addVertex(last, x0, (float) c.y, z0).setColor(color[0], color[1], color[2], 1f).setNormal(last, nx, 0, nz);
            lines.addVertex(last, x1, (float) c.y, z1).setColor(color[0], color[1], color[2], 1f).setNormal(last, nx, 0, nz);
        }
        // 圆心小十字
        float s = 0.3f;
        lines.addVertex(last, (float) c.x - s, (float) c.y, (float) c.z).setColor(color[0], color[1], color[2], 1f).setNormal(last, 1, 0, 0);
        lines.addVertex(last, (float) c.x + s, (float) c.y, (float) c.z).setColor(color[0], color[1], color[2], 1f).setNormal(last, 1, 0, 0);
        lines.addVertex(last, (float) c.x, (float) c.y, (float) c.z - s).setColor(color[0], color[1], color[2], 1f).setNormal(last, 0, 0, 1);
        lines.addVertex(last, (float) c.x, (float) c.y, (float) c.z + s).setColor(color[0], color[1], color[2], 1f).setNormal(last, 0, 0, 1);
    }
}
