package io.github.happysnappingturtle.mythicage.client.power;

import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 通用表现（T12 设计文档 §7）：没有专属实现的神力用它。吟唱中施法者周身金色粒子环（逐刻，由
 * {@link ClientPowerFx} 调 {@link #channelTick}）；提交在原点与目标上闪光；命中冒暴击粒子；预警冒红尘；取消冒烟。
 */
public final class GenericPowerFx implements PowerFxRenderer {
    public static final GenericPowerFx INSTANCE = new GenericPowerFx();
    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.82f, 0.25f), 1.0f);
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.9f, 0.15f, 0.1f), 1.2f);

    private GenericPowerFx() {
    }

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        RandomSource r = RandomSource.create(p.seed());
        switch (p.phase()) {
            case COMMIT -> {
                Vec3 o = p.origin();
                level.addParticle(ParticleTypes.FLASH, o.x, o.y + 0.5, o.z, 0, 0, 0);
                burst(level, ParticleTypes.END_ROD, o.add(0, 0.5, 0), 16, 0.15, r);
                for (int id : p.targets()) {
                    Entity e = level.getEntity(id);
                    if (e != null) {
                        burst(level, ParticleTypes.END_ROD, e.position().add(0, e.getBbHeight() / 2, 0), 10, 0.1, r);
                    }
                }
            }
            case HIT -> {
                for (int id : p.targets()) {
                    Entity e = level.getEntity(id);
                    if (e != null) {
                        burst(level, ParticleTypes.CRIT, e.position().add(0, e.getBbHeight() / 2, 0), 8, 0.3, r);
                    }
                }
            }
            case WARNING -> burst(level, RED, p.origin().add(0, 0.2, 0), 12, 0.05, r);
            case CHANNEL_CANCEL -> {
                Entity c = level.getEntity(p.caster());
                Vec3 at = c != null ? c.position().add(0, 1, 0) : p.origin();
                burst(level, ParticleTypes.SMOKE, at, 10, 0.05, r);
            }
            default -> {
            }
        }
    }

    /** 吟唱光环：绕施法者转的两粒金尘，进度越高转得越高。 */
    static void channelTick(ClientLevel level, Entity caster, long age, double progress) {
        double y = caster.getY() + 0.2 + progress * caster.getBbHeight();
        for (int i = 0; i < 2; i++) {
            double a = age * 0.45 + i * Math.PI;
            level.addParticle(GOLD, caster.getX() + Math.cos(a) * 0.8, y, caster.getZ() + Math.sin(a) * 0.8, 0, 0, 0);
        }
    }

    private static void burst(ClientLevel level, net.minecraft.core.particles.ParticleOptions type, Vec3 at, int n,
                              double speed, RandomSource r) {
        for (int i = 0; i < n; i++) {
            level.addParticle(type, at.x, at.y, at.z, (r.nextDouble() - 0.5) * speed * 4, (r.nextDouble() - 0.2) * speed * 4,
                    (r.nextDouble() - 0.5) * speed * 4);
        }
    }
}
