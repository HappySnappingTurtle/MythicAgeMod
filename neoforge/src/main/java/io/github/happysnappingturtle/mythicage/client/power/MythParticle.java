package io.github.happysnappingturtle.mythicage.client.power;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 神力用的贴图粒子（T13 设计文档 §1.3）。各类型的行为由 {@link Style} 决定：寿命、大小、重力、上浮、是否自发光、
 * 淡入淡出、旋转、是否逐帧换贴图。贴图在 {@code textures/particle/}，由 {@code tools/texgen/fxgen.py} 生成。
 */
public class MythParticle extends TextureSheetParticle {
    /** 粒子行为参数。{@code animated} 为真时按寿命逐帧播放，否则随机取一帧。 */
    public record Style(int minLife, int maxLife, float minSize, float maxSize, float gravity, float rise, float drag,
                        boolean glow, float fadeIn, float fadeOut, float spin, boolean animated, float alpha) {
    }

    private final SpriteSet sprites;
    private final Style style;
    private final float baseAlpha;

    protected MythParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites,
                           Style style) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.style = style;
        this.xd = dx;
        this.yd = dy + style.rise();
        this.zd = dz;
        this.lifetime = style.minLife() + random.nextInt(Math.max(1, style.maxLife() - style.minLife() + 1));
        this.quadSize = Mth.lerp(random.nextFloat(), style.minSize(), style.maxSize());
        this.gravity = style.gravity();
        this.friction = style.drag();
        this.hasPhysics = false;
        this.baseAlpha = style.alpha();
        this.alpha = style.fadeIn() > 0 ? 0 : baseAlpha;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        if (style.animated()) {
            setSpriteFromAge(sprites);
        } else {
            pickSprite(sprites);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (style.animated()) {
            setSpriteFromAge(sprites);
        }
        oRoll = roll;
        roll += style.spin();
        float t = age / (float) lifetime;
        float a = baseAlpha;
        if (style.fadeIn() > 0 && t < style.fadeIn()) {
            a *= t / style.fadeIn();
        }
        if (style.fadeOut() > 0 && t > 1 - style.fadeOut()) {
            a *= (1 - t) / style.fadeOut();
        }
        alpha = Mth.clamp(a, 0, 1);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return style.glow() ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
    }

    /** 某种风格的粒子工厂。 */
    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites, Style style) {
        return (type, level, x, y, z, dx, dy, dz) -> new MythParticle(level, x, y, z, dx, dy, dz, sprites, style);
    }

}
