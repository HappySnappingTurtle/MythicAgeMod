package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.relic.TempleRelicDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 村庄神庙祭坛上方悬着的圣物（GA-13 §2.5）：最多 3 件，绕祭坛中心排开、上下轻轻浮动、慢慢转。
 * 只画服务器发来的祭坛；超过 {@link #STALE} 刻没收到更新（走远了、取光了）就不画。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class TempleRelicRenderer {
    /** 服务器每 100 刻刷新一次，留出余量。 */
    static final int STALE = 400;
    private static final double VIEW = 48;

    private record Shown(List<ItemStack> relics, long at) {
    }

    private static final Map<BlockPos, Shown> SHOWN = new HashMap<>();

    private TempleRelicRenderer() {
    }

    public static void accept(TempleRelicDisplay.Display d) {
        Minecraft mc = Minecraft.getInstance();
        if (d.relics().isEmpty()) {
            SHOWN.remove(d.altar());
        } else if (mc.level != null) {
            SHOWN.put(d.altar(), new Shown(d.relics(), mc.level.getGameTime()));
        }
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || mc.level == null || SHOWN.isEmpty()) {
            return;
        }
        long now = mc.level.getGameTime();
        SHOWN.values().removeIf(s -> now - s.at() > STALE);
        Vec3 cam = event.getCamera().getPosition();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (Map.Entry<BlockPos, Shown> e : SHOWN.entrySet()) {
            BlockPos a = e.getKey();
            if (cam.distanceToSqr(Vec3.atCenterOf(a)) > VIEW * VIEW) {
                continue;
            }
            List<ItemStack> relics = e.getValue().relics();
            float t = now + pt;
            int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, a.above());
            for (int i = 0; i < relics.size(); i++) {
                // 1 件居中；多件绕中心均匀排开、跟着慢慢绕
                double angle = relics.size() == 1 ? 0 : (Math.PI * 2 * i / relics.size()) + t * 0.01;
                double r = relics.size() == 1 ? 0 : 0.4;
                double x = a.getX() + 0.5 + Math.cos(angle) * r;
                double z = a.getZ() + 0.5 + Math.sin(angle) * r;
                double y = a.getY() + 1.15 + Math.sin(t * 0.05 + i * 2.1) * 0.06;
                pose.pushPose();
                pose.translate(x - cam.x, y - cam.y, z - cam.z);
                pose.mulPose(Axis.YP.rotationDegrees(t * 1.5F + i * 120));
                // 地面物品的显示本身缩了一半：再放大 1.2 倍，约 0.6 格见方，站在祭坛前一眼能认出
                pose.scale(1.2F, 1.2F, 1.2F);
                mc.getItemRenderer().renderStatic(relics.get(i), ItemDisplayContext.GROUND, light,
                        OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
                pose.popPose();
            }
        }
        buffers.endBatch();
    }
}
