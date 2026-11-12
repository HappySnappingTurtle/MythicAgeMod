package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.village.build.ConstructionSitePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.*;

@EventBusSubscriber(modid = "mythic_age", value = Dist.CLIENT)
public final class ConstructionSiteRenderer {
    private record Shown(ConstructionSitePayload payload, long expires) {}
    private static final Map<UUID, Shown> SITES = new HashMap<>();
    private ConstructionSiteRenderer() {}
    public static void accept(ConstructionSitePayload p) {
        var level = Minecraft.getInstance().level;
        if (!p.active() || level == null) SITES.remove(p.owner());
        else SITES.put(p.owner(), new Shown(p, level.getGameTime() + 100));
    }
    @SubscribeEvent static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { SITES.clear(); }
    @SubscribeEvent static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || mc.level == null) return;
        SITES.values().removeIf(s -> s.expires < mc.level.getGameTime() || !s.payload.dimension().equals(mc.level.dimension().location()));
        var cam = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose(); pose.translate(-cam.x, -cam.y, -cam.z);
        for (Shown shown : SITES.values()) {
            var p = shown.payload;
            var box = new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(p.min()), net.minecraft.world.phys.Vec3.atLowerCornerOf(p.max().offset(1, 1, 1)));
            if (!box.inflate(48).contains(cam)) continue;
            LevelRenderer.renderLineBox(pose, lines, new AABB(box.minX, box.minY + .04, box.minZ, box.maxX, box.minY + .06, box.maxZ), 1, .86f, .35f, .65f);
            for (double x : new double[]{box.minX, box.maxX}) for (double z : new double[]{box.minZ, box.maxZ}) {
                pose.pushPose(); pose.translate(x - .5, box.minY, z - .5);
                mc.getBlockRenderer().renderSingleBlock(net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState(),
                        pose, buffers, net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, net.minecraft.core.BlockPos.containing(x, box.minY, z)),
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            for (var at : p.blocked()) LevelRenderer.renderLineBox(pose, lines, new AABB(at).inflate(.015), 1, .1f, .1f, 1);
        }
        pose.popPose(); buffers.endBatch(RenderType.lines());
    }
}
