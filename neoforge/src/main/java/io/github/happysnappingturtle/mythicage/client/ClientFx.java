package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;

/**
 * 装饰特效的统一开关（T24 §7.1）：只给装饰粒子、闪光用。战斗预警（落点环、机关墙预警、刨地蓄力）<b>不调用这里</b>，
 * 由 {@code tools/check_content.py} 静态检查预警所在文件不引用本类。
 */
public final class ClientFx {
    private ClientFx() {
    }

    /** 当前装饰粒子档位：跟随原版时按原版「粒子效果」换算。 */
    public static ClientConfig.Particles decorative() {
        ClientConfig.Particles p = ClientConfig.decorativeParticles();
        if (p != ClientConfig.Particles.FOLLOW_VANILLA) {
            return p;
        }
        ParticleStatus v = Minecraft.getInstance().options.particles().get();
        return switch (v) {
            case ALL -> ClientConfig.Particles.ALL;
            case DECREASED -> ClientConfig.Particles.REDUCED;
            case MINIMAL -> ClientConfig.Particles.MINIMAL;
        };
    }

    /** 装饰粒子是否降到最少。 */
    public static boolean decorativeMinimal() {
        return decorative() == ClientConfig.Particles.MINIMAL;
    }

    /** 装饰粒子的数量：全部 / 减半 / 0。 */
    public static int decorativeCount(int full) {
        return switch (decorative()) {
            case MINIMAL -> 0;
            case REDUCED -> (full + 1) / 2;
            default -> full;
        };
    }

    public static boolean reduceFlashes() {
        return ClientConfig.reduceFlashes();
    }
}
