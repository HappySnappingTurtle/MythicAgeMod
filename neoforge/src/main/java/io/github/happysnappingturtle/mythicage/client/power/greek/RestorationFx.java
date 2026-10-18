package io.github.happysnappingturtle.mythicage.client.power.greek;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.client.power.ActiveFx;
import io.github.happysnappingturtle.mythicage.client.power.ClientPowerFx;
import io.github.happysnappingturtle.mythicage.client.power.FxDraw;
import io.github.happysnappingturtle.mythicage.client.power.PowerFxRenderer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 复原的表现（03 号分镜 GP-GR-04；用户 2026-10-17 要求更丰富）：
 * <ol>
 *   <li>瞄准：脚下半径 12 的橄榄叶环；</li>
 *   <li>降临：施法者脚下展开发光的雅典娜纹章（回纹、橄榄环、八芒星，缓慢转动，5 秒后淡去），
 *       同时一圈叶浪从施法者推到 12 格，浪头上扬起橄榄叶；</li>
 *   <li>每个受益者：一圈光从脚扫到头顶；之后 5 秒里身边不断升起修复光线（6 道错开的细光向上流）、
 *       橄榄叶绕身旋转、光点上浮；</li>
 *   <li>每次补血：脚下闪一圈光环外扩 + 一簇光点；清除了状态的头顶出净化图标，修了装备的出铁砧图标；</li>
 *   <li>第 5 秒：施法处大片散叶。</li>
 * </ol>
 */
public final class RestorationFx implements PowerFxRenderer {
    static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/olive_ring.png");
    static final ResourceLocation SWEEP = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/heal_ring.png");
    static final ResourceLocation SIGIL = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/athena_sigil.png");
    static final int OLIVE = 0xB8D46A;
    static final int LIGHT = 0xA8E070;
    static final int DURATION = 100;
    static final float RADIUS = 12;

    @Override
    public AimStyle aimStyle() {
        return new AimStyle(RING, OLIVE);
    }

    static boolean minimal() {
        return Minecraft.getInstance().options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL;
    }

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        RandomSource r = RandomSource.create(p.seed() ^ p.tag().hashCode());
        long now = level.getGameTime();
        switch (p.phase()) {
            case COMMIT -> ClientPowerFx.add(new Sigil(p.origin(), now, p.duration() > 0 ? p.duration() : DURATION));
            case HIT -> {
                for (int id : p.targets()) {
                    Entity e = level.getEntity(id);
                    if (e == null) {
                        continue;
                    }
                    switch (p.tag()) {
                        case "sweep" -> ClientPowerFx.add(new Blessing(id, now, r.nextLong()));
                        case "pulse" -> {
                            ClientPowerFx.add(new Pulse(id, now));
                            for (int i = 0; i < 8; i++) {
                                double a = i * Math.PI / 4 + r.nextDouble() * 0.4;
                                double rad = e.getBbWidth() * 0.6 + 0.2;
                                level.addParticle(PowerRegistries.HEAL_MOTE.get(), e.getX() + Math.cos(a) * rad,
                                        e.getY() + 0.1 + r.nextDouble() * 0.5, e.getZ() + Math.sin(a) * rad, 0, 0.03, 0);
                            }
                        }
                        case "cleanse" -> level.addParticle(PowerRegistries.CLEANSE.get(), e.getX(),
                                e.getY() + e.getBbHeight() + 0.45, e.getZ(), 0, 0, 0);
                        case "repair" -> level.addParticle(PowerRegistries.REPAIR.get(), e.getX() + 0.35,
                                e.getY() + e.getBbHeight() + 0.25, e.getZ(), 0, 0, 0);
                        default -> {
                        }
                    }
                }
            }
            case END -> {
                Vec3 o = p.origin();
                int n = minimal() ? 12 : 70;
                for (int i = 0; i < n; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double rad = r.nextDouble() * 5;
                    level.addParticle(PowerRegistries.OLIVE_LEAF.get(), o.x + Math.cos(a) * rad, o.y + 1 + r.nextDouble() * 2,
                            o.z + Math.sin(a) * rad, Math.cos(a) * 0.05, 0.04 + r.nextDouble() * 0.04, Math.sin(a) * 0.05);
                }
            }
            default -> {
            }
        }
    }

    /** 施法者脚下的纹章 + 外推的叶浪。 */
    static final class Sigil implements ActiveFx {
        static final int WAVE = 16;
        final Vec3 at;
        final long start;
        final int duration;

        Sigil(Vec3 at, long start, int duration) {
            this.at = at;
            this.start = start;
            this.duration = duration;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            long age = now - start;
            if (age <= WAVE && !minimal()) {
                // 叶浪浪头扬起叶子
                float rad = waveRadius(age);
                for (int i = 0; i < 10; i++) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    level.addParticle(PowerRegistries.OLIVE_LEAF.get(), at.x + Math.cos(a) * rad, at.y + 0.2,
                            at.z + Math.sin(a) * rad, Math.cos(a) * 0.06, 0.08, Math.sin(a) * 0.06);
                }
            }
            return age < duration;
        }

        static float waveRadius(float age) {
            return RADIUS * (float) Math.pow(Math.min(1, age / WAVE), 0.7);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float age = now - start + pt;
            float in = Math.min(1, age / 8f);
            float out = Math.min(1, (duration - age) / 25f);
            float alpha = 0.95f * Math.max(0, Math.min(in, out));
            float grow = 1.6f + 1.2f * (float) Math.sin(Math.min(1, age / 8f) * Math.PI / 2);
            FxDraw.flat(pose, buffers, SIGIL, at.add(0, 0.04, 0), grow, age * 0.8f, FxDraw.argb(alpha, 0xFFFFFF));
            if (age <= WAVE + 2) {
                float t = Math.min(1, age / WAVE);
                FxDraw.flat(pose, buffers, RING, at.add(0, 0.06, 0), Math.max(0.3f, waveRadius(age)), age * 2,
                        FxDraw.argb(1 - t * 0.8f, 0xFFFFFF));
            }
        }
    }

    /** 受益者身上 5 秒的祝福：开头扫光，之后修复光线上流、叶子绕身、光点上浮。 */
    static final class Blessing implements ActiveFx {
        static final int SWEEP_TICKS = 12;
        static final int STREAKS = 6;
        static final int CYCLE = 24;
        final int target;
        final long start;
        final float[] angles = new float[STREAKS];
        final float[] phases = new float[STREAKS];

        Blessing(int target, long start, long seed) {
            this.target = target;
            this.start = start;
            RandomSource r = RandomSource.create(seed);
            for (int i = 0; i < STREAKS; i++) {
                angles[i] = (float) (i * Math.PI * 2 / STREAKS + r.nextDouble() * 0.6);
                phases[i] = r.nextFloat() * CYCLE;
            }
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            Entity e = level.getEntity(target);
            long age = now - start;
            if (e == null || age > DURATION) {
                return false;
            }
            double w = e.getBbWidth() * 0.6 + 0.3;
            if (age <= SWEEP_TICKS) {
                double y = e.getY() + e.getBbHeight() * age / SWEEP_TICKS;
                for (int i = 0; i < 3; i++) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    level.addParticle(PowerRegistries.HEAL_MOTE.get(), e.getX() + Math.cos(a) * w, y, e.getZ() + Math.sin(a) * w,
                            0, 0.01, 0);
                }
            }
            if (!minimal()) {
                // 叶子沿切向飞出，看起来绕身旋转
                if (age % 3 == 0) {
                    double a = age * 0.35;
                    double y = e.getY() + 0.3 + (age % 12) / 12.0 * e.getBbHeight();
                    level.addParticle(PowerRegistries.OLIVE_LEAF.get(), e.getX() + Math.cos(a) * w, y, e.getZ() + Math.sin(a) * w,
                            -Math.sin(a) * 0.08, 0.015, Math.cos(a) * 0.08);
                }
                if (age % 2 == 0) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    level.addParticle(PowerRegistries.HEAL_MOTE.get(), e.getX() + Math.cos(a) * w * 0.8, e.getY() + 0.2,
                            e.getZ() + Math.sin(a) * w * 0.8, 0, 0.035, 0);
                }
            }
            return true;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level == null ? null : mc.level.getEntity(target);
            if (e == null) {
                return;
            }
            float age = now - start + pt;
            Vec3 feet = e.getPosition(pt);
            float h = e.getBbHeight();
            float w = e.getBbWidth() * 0.6f + 0.3f;
            if (age <= SWEEP_TICKS + 2) {
                float t = Math.min(1, age / SWEEP_TICKS);
                float alpha = t < 0.8f ? 0.95f : 0.95f * (1 - t) / 0.2f;
                FxDraw.flat(pose, buffers, SWEEP, feet.add(0, h * t, 0), w + 0.1f, age * 8, FxDraw.argb(alpha, LIGHT));
            }
            // 修复光线：每道 0.6 格长，从脚下流到头顶上方，循环；整体在最后 15 刻淡出
            float fade = Math.min(1, Math.min(age / 6f, (DURATION - age) / 15f));
            if (fade <= 0) {
                return;
            }
            for (int i = 0; i < STREAKS; i++) {
                float p = ((age + phases[i]) % CYCLE) / CYCLE;
                float y0 = -0.2f + p * (h + 0.8f);
                float a = Mth.sin(p * Mth.PI) * 0.8f * fade;
                double x = Math.cos(angles[i] + age * 0.03) * w;
                double z = Math.sin(angles[i] + age * 0.03) * w;
                FxDraw.streak(pose, buffers, feet.add(x, y0, z), feet.add(x, y0 + 0.6, z), 0.025f, LIGHT, a);
            }
        }
    }

    /** 一次补血：脚下光环外扩。 */
    static final class Pulse implements ActiveFx {
        final int target;
        final long start;

        Pulse(int target, long start) {
            this.target = target;
            this.start = start;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            return now - start < 10 && level.getEntity(target) != null;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level == null ? null : mc.level.getEntity(target);
            if (e == null) {
                return;
            }
            float t = Math.min(1, (now - start + pt) / 10f);
            FxDraw.flat(pose, buffers, SWEEP, e.getPosition(pt).add(0, 0.05, 0), 0.3f + (e.getBbWidth() + 0.9f) * t, 0,
                    FxDraw.argb(0.9f * (1 - t), LIGHT));
        }
    }
}
