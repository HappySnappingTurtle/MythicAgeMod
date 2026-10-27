package io.github.happysnappingturtle.mythicage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端选项 {@code mythic_age-client.toml}（T24 §7.1，12 号 §5：只影响字幕、HUD、闪光、装饰粒子）。
 * <b>不影响必要的战斗预警</b>：预警粒子由服务端发出或走不读本配置的绘制路径，减少特效不能删掉预警、也不改伤害。
 * 现在模组里没有震屏效果，所以没有震屏开关；以后加震屏必须先在这里加开关。
 */
public final class ClientConfig {
    public enum Particles { FOLLOW_VANILLA, ALL, REDUCED, MINIMAL }

    public enum Anchor { BOTTOM_LEFT, TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue REDUCE_FLASHES;
    public static final ModConfigSpec.EnumValue<Particles> DECORATIVE_PARTICLES;
    public static final ModConfigSpec.BooleanValue FAVOR_HUD;
    public static final ModConfigSpec.EnumValue<Anchor> FAVOR_HUD_ANCHOR;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        REDUCE_FLASHES = b.comment("Reduce full-screen and bright flashes (lightning sky flash, statue rune pulses)")
                .translation("mythic_age.configuration.reduceFlashes").define("reduceFlashes", false);
        DECORATIVE_PARTICLES = b.comment("Decorative particles only; combat warnings are never reduced")
                .translation("mythic_age.configuration.decorativeParticles").defineEnum("decorativeParticles", Particles.FOLLOW_VANILLA);
        FAVOR_HUD = b.comment("Show the favor bar when favor changes")
                .translation("mythic_age.configuration.favorHud").define("favorHud", true);
        FAVOR_HUD_ANCHOR = b.comment("Screen corner for the favor bar")
                .translation("mythic_age.configuration.favorHudAnchor").defineEnum("favorHudAnchor", Anchor.BOTTOM_LEFT);
        SPEC = b.build();
    }

    private ClientConfig() {
    }

    /** 配置还没载入（如 GameTest 服务端）时用默认值。 */
    public static boolean reduceFlashes() {
        return SPEC.isLoaded() && REDUCE_FLASHES.get();
    }

    public static Particles decorativeParticles() {
        return SPEC.isLoaded() ? DECORATIVE_PARTICLES.get() : Particles.FOLLOW_VANILLA;
    }

    public static boolean favorHud() {
        return !SPEC.isLoaded() || FAVOR_HUD.get();
    }

    public static Anchor favorHudAnchor() {
        return SPEC.isLoaded() ? FAVOR_HUD_ANCHOR.get() : Anchor.BOTTOM_LEFT;
    }
}
