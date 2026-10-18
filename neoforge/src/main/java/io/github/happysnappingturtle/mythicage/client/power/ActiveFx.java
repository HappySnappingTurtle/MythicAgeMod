package io.github.happysnappingturtle.mythicage.client.power;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * 一段持续的表现（T13 设计文档 §1.3）：由某个神力的表现实现在收到事件时加入 {@link ClientPowerFx#add}，
 * 逐刻 {@link #tick}（发粒子、推进动画），逐帧 {@link #render}（画地面圈、雷束）。{@link #tick} 返回 false 时移除。
 */
public interface ActiveFx {
    boolean tick(ClientLevel level, long gameTime);

    /** 世界坐标（已减去相机位置的 PoseStack）。 */
    default void render(PoseStack pose, MultiBufferSource buffers, long gameTime, float partialTick) {
    }

    /** 所属施放（可按 castId 提前结束）；不需要时为空串。 */
    default String castId() {
        return "";
    }
}
