package io.github.happysnappingturtle.mythicage.core.anim;

/** 头部视线叠加的角度处理（GA-5），单位为度，不依赖游戏运行时。 */
public final class HeadLook {
    private HeadLook() {
    }

    /** 先归一到 (−180, 180]，再限幅，避免跨整圈时头骨跳转。 */
    public static float yaw(float netHeadYaw, float limit) {
        if (!Float.isFinite(netHeadYaw)) {
            return 0;
        }
        float wrapped = netHeadYaw % 360.0F;
        if (wrapped > 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped <= -180.0F) {
            wrapped += 360.0F;
        }
        return Math.clamp(wrapped, -limit, limit);
    }

    /** 俯仰只限幅；异常角度不传给骨骼。 */
    public static float pitch(float headPitch, float limit) {
        return Float.isFinite(headPitch) ? Math.clamp(headPitch, -limit, limit) : 0;
    }
}
