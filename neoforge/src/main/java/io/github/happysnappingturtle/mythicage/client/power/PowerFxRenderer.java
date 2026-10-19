package io.github.happysnappingturtle.mythicage.client.power;

import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 某个神力的专属表现（T12 设计文档 §2.7）：T13 起各神力自己登记；没有登记的只画通用效果（{@link GenericPowerFx}）。
 * 吟唱光环与吟唱条是框架统一画的，这里只管各阶段的一次性表现。
 */
public interface PowerFxRenderer {
    void play(PowerFxPayload payload, ClientLevel level);

    /** 瞄准时范围圈的样式（贴图 + 颜色）；为空时用通用的金色线圈。 */
    @javax.annotation.Nullable
    default AimStyle aimStyle() {
        return null;
    }

    /**
     * 放置型神力瞄准时的专属预览（放置格 {@code place} 已确定）：画了就返回 true，框架不再画默认的单格框。
     * 坐标为世界坐标（已减去相机位置的 PoseStack）。
     */
    default boolean renderPlacement(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                                    ClientLevel level, net.minecraft.core.BlockPos place, long gameTime, float partialTick) {
        return false;
    }

    /** 范围圈样式：一张贴地的圈贴图（圈在贴图外缘）与调色。 */
    record AimStyle(net.minecraft.resources.ResourceLocation ring, int rgb) {
    }

    /** 专属表现注册表（按神力 id）。 */
    final class Registry {
        private static final Map<String, PowerFxRenderer> ALL = new ConcurrentHashMap<>();

        private Registry() {
        }

        public static void register(String powerId, PowerFxRenderer renderer) {
            if (ALL.putIfAbsent(powerId, renderer) != null) {
                throw new IllegalStateException("重复的神力表现：" + powerId);
            }
        }

        public static Optional<PowerFxRenderer> get(String powerId) {
            return Optional.ofNullable(ALL.get(powerId));
        }
    }
}
