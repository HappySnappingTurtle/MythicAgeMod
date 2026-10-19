package io.github.happysnappingturtle.mythicage.client.power;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.happysnappingturtle.mythicage.core.power.ThrowRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 投掷物飞行（T13 设计文档 §3.1）：收到投掷型神力的 {@code COMMIT} 后，从施法者手边按 {@link ThrowRules} 的抛物线
 * 画一个翻滚的物品飞到落点，身后拖一串粒子。只是表现：落地效果由服务端在同一时刻执行。
 */
public final class ThrowFx implements ActiveFx {
    private final ItemStack item;
    private final Vec3 from;
    private final Vec3 to;
    private final long start;
    private final int ticks;
    private final double distance;
    @Nullable
    private final ParticleOptions trail;

    public ThrowFx(ClientLevel level, int casterId, Vec3 to, ItemStack item, @Nullable ParticleOptions trail) {
        Entity caster = level.getEntity(casterId);
        this.from = caster != null ? caster.getEyePosition().add(caster.getLookAngle().scale(0.6)).add(0, -0.3, 0) : to.add(0, 2, 0);
        this.to = to;
        this.item = item;
        this.trail = trail;
        this.start = level.getGameTime();
        this.distance = caster != null ? caster.getEyePosition().distanceTo(to) : 2;
        this.ticks = ThrowRules.flightTicks(distance);
    }

    Vec3 at(float t) {
        return from.lerp(to, t).add(0, ThrowRules.arc(distance, t), 0);
    }

    @Override
    public boolean tick(ClientLevel level, long now) {
        float t = (now - start) / (float) ticks;
        if (trail != null && t <= 1) {
            Vec3 p = at(Math.min(1, t));
            level.addParticle(trail, p.x, p.y, p.z, 0, 0.01, 0);
        }
        return t < 1;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
        float age = now - start + pt;
        float t = Math.min(1, age / ticks);
        Vec3 p = at(t);
        pose.pushPose();
        pose.translate(p.x, p.y, p.z);
        pose.mulPose(Axis.YP.rotationDegrees(age * 25));
        pose.mulPose(Axis.XP.rotationDegrees(age * 40));
        pose.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers, Minecraft.getInstance().level, 0);
        pose.popPose();
    }
}
