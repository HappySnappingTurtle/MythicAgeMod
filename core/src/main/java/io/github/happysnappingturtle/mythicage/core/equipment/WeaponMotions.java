package io.github.happysnappingturtle.mythicage.core.equipment;

/** GA-8：第一、第三人称共用的动作曲线，进度来自原版挥击；不依赖游戏类。 */
public final class WeaponMotions {
    private WeaponMotions() {
    }

    public static float smooth(float value) {
        float x = Float.isNaN(value) ? 0 : Math.max(0, Math.min(1, value));
        return x * x * (3 - 2 * x);
    }

    /** 前推距离（格）：先收半拍，0.6 达到最远，再归位。正值向前，负值向后。 */
    public static float thrust(float progress) {
        float t = Float.isNaN(progress) ? 0 : Math.max(0, Math.min(1, progress));
        if (t < 0.3F) {
            return -0.10F * smooth(t / 0.3F);
        }
        if (t < 0.6F) {
            return -0.10F + 0.55F * smooth((t - 0.3F) / 0.3F);
        }
        return 0.45F * (1 - smooth((t - 0.6F) / 0.4F));
    }

    /** 举刀角度（度）：举过头顶 → 快速下劈 → 收回；首尾为零。 */
    public static float cleave(float progress) {
        float t = Float.isNaN(progress) ? 0 : Math.max(0, Math.min(1, progress));
        if (t < 0.35F) {
            return 100 * smooth(t / 0.35F);
        }
        if (t < 0.65F) {
            return 100 - 155 * smooth((t - 0.35F) / 0.3F);
        }
        return -55 * (1 - smooth((t - 0.65F) / 0.35F));
    }

    /** 起止平滑接入持械姿势，避免挥击开始时突然转向。 */
    public static float envelope(float progress) {
        return smooth(progress / 0.15F) * (1 - smooth((progress - 0.8F) / 0.2F));
    }
}
