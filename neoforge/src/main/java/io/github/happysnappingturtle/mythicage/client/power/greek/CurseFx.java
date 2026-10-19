package io.github.happysnappingturtle.mythicage.client.power.greek;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.client.power.ActiveFx;
import io.github.happysnappingturtle.mythicage.client.power.ClientPowerFx;
import io.github.happysnappingturtle.mythicage.client.power.FxDraw;
import io.github.happysnappingturtle.mythicage.client.power.PowerFxRenderer;
import io.github.happysnappingturtle.mythicage.client.power.ThrowFx;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * 诅咒的表现（03 号分镜 GP-GR-09）：金苹果翻滚飞出、拖一串粉色光点 → 落地果汁四溅、玫瑰粉符圈张开并缓缓脉动 →
 * 被选中的敌人依次（每 2 刻一个）冒一团短促的玫瑰烟变成猪（烟小，不挡别的敌人）→ 诅咒猪头顶常驻金苹果印记（渲染器）→
 * 免疫的精英 / Boss 头顶弹出盾形提示 → 全部变完后符圈收拢消失。
 */
public final class CurseFx implements PowerFxRenderer {
    static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/curse_ring.png");

    @Override
    public AimStyle aimStyle() {
        return new AimStyle(RING, 0xF39BC0);
    }

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        long now = level.getGameTime();
        RandomSource r = RandomSource.create(p.seed() ^ p.tag().hashCode());
        switch (p.phase()) {
            case COMMIT -> ClientPowerFx.add(new ThrowFx(level, p.caster(), p.origin(),
                    new ItemStack(GreekPowers.CURSE_ITEM.get()), ParticleTypes.CHERRY_LEAVES));
            case HIT -> {
                if (p.tag().startsWith("ring:")) {
                    float radius = Float.parseFloat(p.tag().substring(5));
                    ClientPowerFx.add(new Ring(p.castId(), p.origin(), radius, now));
                    ItemParticleOption juice = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLDEN_APPLE));
                    for (int i = 0; i < 20; i++) {
                        level.addParticle(juice, p.origin().x, p.origin().y + 0.2, p.origin().z, (r.nextDouble() - 0.5) * 0.4,
                                r.nextDouble() * 0.3, (r.nextDouble() - 0.5) * 0.4);
                    }
                    return;
                }
                for (int id : p.targets()) {
                    Entity e = level.getEntity(id);
                    Vec3 at = e != null ? e.position() : p.origin();
                    double h = e != null ? e.getBbHeight() : 1;
                    switch (p.tag()) {
                        case "transform" -> {
                            for (int i = 0; i < 10; i++) {
                                level.addParticle(PowerRegistries.ROSE_PUFF.get(), at.x + (r.nextDouble() - 0.5) * 0.8,
                                        at.y + r.nextDouble() * h, at.z + (r.nextDouble() - 0.5) * 0.8,
                                        (r.nextDouble() - 0.5) * 0.04, 0.02, (r.nextDouble() - 0.5) * 0.04);
                            }
                            for (int i = 0; i < 8; i++) {
                                level.addParticle(ParticleTypes.HEART, at.x + (r.nextDouble() - 0.5), at.y + h + r.nextDouble() * 0.4,
                                        at.z + (r.nextDouble() - 0.5), 0, 0.05, 0);
                            }
                        }
                        case "immune" -> level.addParticle(PowerRegistries.IMMUNE.get(), at.x, at.y + h + 0.5, at.z, 0, 0, 0);
                        default -> {
                        }
                    }
                }
            }
            case END -> {
                for (ActiveFx fx : java.util.List.copyOf(ringsOf(p.castId()))) {
                    ((Ring) fx).close(now);
                }
            }
            default -> {
            }
        }
    }

    private static java.util.List<ActiveFx> ringsOf(String castId) {
        java.util.List<ActiveFx> out = new java.util.ArrayList<>();
        ClientPowerFx.forEach(fx -> {
            if (fx instanceof Ring ring && ring.castId.equals(castId)) {
                out.add(fx);
            }
        });
        return out;
    }

    /** 符圈：张开 → 脉动 → 收到结束后收拢（10 刻）。 */
    static final class Ring implements ActiveFx {
        final String castId;
        final Vec3 at;
        final float radius;
        final long start;
        long closedAt = -1;

        Ring(String castId, Vec3 at, float radius, long start) {
            this.castId = castId;
            this.at = at;
            this.radius = radius;
            this.start = start;
        }

        void close(long now) {
            if (closedAt < 0) {
                closedAt = now;
            }
        }

        @Override
        public String castId() {
            return castId;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            if (now - start < 40 && level.random.nextFloat() < 0.6f) {
                double a = level.random.nextDouble() * Math.PI * 2;
                level.addParticle(ParticleTypes.CHERRY_LEAVES, at.x + Math.cos(a) * radius, at.y + 0.3,
                        at.z + Math.sin(a) * radius, 0, 0.02, 0);
            }
            // 服务端结束事件丢失时 5 秒后自行收拢
            if (closedAt < 0 && now - start > 100) {
                closedAt = now;
            }
            return closedAt < 0 || now - closedAt < 10;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float age = now - start + pt;
            float open = Math.min(1, age / 6f);
            float r = radius * (0.6f + 0.4f * open) * (1 + 0.03f * Mth.sin(age * 0.4f));
            float alpha = 0.95f * open;
            if (closedAt >= 0) {
                float c = Math.min(1, (now - closedAt + pt) / 10f);
                r *= 1 - c;
                alpha *= 1 - c * 0.5f;
            }
            if (r > 0.05f) {
                FxDraw.flat(pose, buffers, RING, at.add(0, 0.05, 0), r, -age * 1.2f, FxDraw.argb(alpha, 0xFFFFFF));
            }
        }
    }
}
