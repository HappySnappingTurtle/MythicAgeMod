package io.github.happysnappingturtle.mythicage.agent;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 截图：只从游戏自己的帧缓冲读取（{@link Screenshot#takeScreenshot}），物理上不可能截到桌面或其他窗口。
 * <p>
 * 用「请求 → 下一次 RenderFrameEvent.Post 时截取并落盘」两步：tick 发生在渲染之前，
 * 同一 tick 内直接截会拿到上一帧的画面。
 */
public final class FrameCapture {
    /** 一次截图的结果。 */
    public record Result(int width, int height) {
    }

    private static volatile Path pendingTarget;
    private static volatile Result lastResult;
    private static volatile String lastError;

    private FrameCapture() {
    }

    public static void request(Path target) {
        lastResult = null;
        lastError = null;
        pendingTarget = target;
    }

    /** @return 截图结果；尚未完成时为 null（失败时看 {@link #error()}）。 */
    public static Result result() {
        return lastResult;
    }

    public static String error() {
        return lastError;
    }

    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        Path target = pendingTarget;
        if (target == null) {
            return;
        }
        pendingTarget = null;
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            Files.createDirectories(target.getParent());
            image.writeToFile(target);
            lastResult = new Result(image.getWidth(), image.getHeight());
        } catch (Exception e) {
            lastError = "截图失败：" + e;
            AgentEntrypoint.LOG.error("[agent] 截图失败：{}", target, e);
        }
    }
}
