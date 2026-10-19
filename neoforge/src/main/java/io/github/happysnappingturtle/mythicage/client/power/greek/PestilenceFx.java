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
import io.github.happysnappingturtle.mythicage.power.PowerZones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 瘟疫的表现（03 号分镜 GP-GR-06）：陶瓮翻滚着飞出、拖一缕病雾 → 落地碎裂（陶片 + 一团病雾）→ 暗红病印 1 秒内
 * 扩张到 24 格边界，之后淡淡留着当边界 → 区域内贴地飘稀薄病雾（低密度、只在玩家附近生成）→ 每个被封停的刷怪器上方
 * 一个停摆符号 → 中了瘟疫的敌人头顶暗红标记（Aura）→ 到期雾散、符号熄灭。雾只是表现，不代表伤害区。
 */
public final class PestilenceFx implements PowerFxRenderer {
    static final ResourceLocation SEAL = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/plague_seal.png");
    static final ResourceLocation BAND = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/plague_band.png");
    /** 落点病印的半径（格）。 */
    static final float SEAL_RADIUS = 3.5f;

    @Override
    public AimStyle aimStyle() {
        return new AimStyle(SEAL, 0xB0352A);
    }

    static boolean minimal() {
        return Minecraft.getInstance().options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL;
    }

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        long now = level.getGameTime();
        switch (p.phase()) {
            case COMMIT -> ClientPowerFx.add(new ThrowFx(level, p.caster(), p.origin(),
                    new ItemStack(GreekPowers.PESTILENCE_ITEM.get()), PowerRegistries.PLAGUE_MIST.get()));
            case HIT -> {
                Double radius = PowerZones.radiusOf(p.tag());
                if (radius != null) {
                    ClientPowerFx.removeIf(fx -> fx instanceof Zone z && z.castId.equals(p.castId()));
                    ClientPowerFx.add(new Zone(p.castId(), p.origin(), radius.floatValue(), now, p.duration()));
                    shatter(level, p.origin(), p.seed());
                } else if ("mark".equals(p.tag())) {
                    ClientPowerFx.removeIf(fx -> fx instanceof Mark m && m.castId.equals(p.castId()) && m.at.equals(p.origin()));
                    ClientPowerFx.add(new Mark(p.castId(), p.origin(), now, p.duration()));
                }
            }
            case END -> ClientPowerFx.removeIf(fx -> (fx instanceof Zone z && z.castId.equals(p.castId()))
                    || (fx instanceof Mark m && m.castId.equals(p.castId())));
            default -> {
            }
        }
    }

    /** 陶瓮碎裂：陶片四溅 + 一团病雾。 */
    static void shatter(ClientLevel level, Vec3 at, long seed) {
        RandomSource r = RandomSource.create(seed);
        BlockParticleOption shard = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_TERRACOTTA.defaultBlockState());
        for (int i = 0; i < 24; i++) {
            level.addParticle(shard, at.x, at.y + 0.3, at.z, (r.nextDouble() - 0.5) * 0.6, r.nextDouble() * 0.4,
                    (r.nextDouble() - 0.5) * 0.6);
        }
        for (int i = 0; i < 18; i++) {
            level.addParticle(PowerRegistries.PLAGUE_MIST.get(), at.x + (r.nextDouble() - 0.5) * 2, at.y + 0.3 + r.nextDouble(),
                    at.z + (r.nextDouble() - 0.5) * 2, (r.nextDouble() - 0.5) * 0.08, 0.01, (r.nextDouble() - 0.5) * 0.08);
        }
    }

    /** 区域：病印扩张后留作边界 + 贴地病雾。 */
    static final class Zone implements ActiveFx {
        static final int EXPAND = 20;
        final String castId;
        final Vec3 center;
        final float radius;
        final long start;
        final int duration;

        Zone(String castId, Vec3 center, float radius, long start, int duration) {
            this.castId = castId;
            this.center = center;
            this.radius = radius;
            this.start = start;
            this.duration = duration;
        }

        @Override
        public String castId() {
            return castId;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            long age = now - start;
            var player = Minecraft.getInstance().player;
            // 只在玩家附近生雾，远处的区域不白费粒子
            if (player != null && !minimal() && player.position().distanceTo(center) < radius + 32) {
                RandomSource r = level.random;
                int n = age < EXPAND ? 8 : 3;
                for (int i = 0; i < n; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double d = Math.sqrt(r.nextDouble()) * radius * Math.min(1, age / (double) EXPAND);
                    double x = center.x + Math.cos(a) * d;
                    double z = center.z + Math.sin(a) * d;
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x),
                            (int) Math.floor(z));
                    double gy = Math.abs(y - center.y) <= 6 ? y : center.y;
                    level.addParticle(PowerRegistries.PLAGUE_MIST.get(), x, gy + 0.15 + r.nextDouble() * 0.4, z,
                            (r.nextDouble() - 0.5) * 0.02, 0.003, (r.nextDouble() - 0.5) * 0.02);
                }
            }
            return age < duration;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float age = now - start + pt;
            float out = Math.max(0, Math.min(1, (duration - age) / 40f));
            // 落点病印：0.4 秒张开到 3.5 格，之后一直留着、缓慢转动
            float open = Math.min(1, age / 8f);
            FxDraw.flat(pose, buffers, SEAL, center.add(0, 0.05, 0), SEAL_RADIUS * (0.5f + 0.5f * open), age * 0.3f,
                    FxDraw.argb(0.9f * open * out, 0xFFFFFF));
            // 病印外扩成边界带：1 秒扩到区域半径，之后淡淡留作边界
            float t = Math.min(1, age / EXPAND);
            float r = Math.max(SEAL_RADIUS, radius * (float) Math.sqrt(t));
            float alpha = age < EXPAND ? 0.95f : Math.max(0.5f, 0.95f - (age - EXPAND) / 30f);
            FxDraw.band(pose, buffers, BAND, center.add(0, 0.06, 0), r, age < EXPAND ? 1.6f : 1.0f, 2.5f, FxDraw.argb(alpha * out, 0xFFFFFF));
        }
    }

    /** 被封停的刷怪器上方的停摆符号（持续时间内每秒补一个图标粒子）。 */
    static final class Mark implements ActiveFx {
        final String castId;
        final Vec3 at;
        final long start;
        final int duration;

        Mark(String castId, Vec3 at, long start, int duration) {
            this.castId = castId;
            this.at = at;
            this.start = start;
            this.duration = duration;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            long age = now - start;
            if (age % 20 == 0) {
                level.addParticle(PowerRegistries.SEAL.get(), at.x, at.y + 1.1, at.z, 0, 0, 0);
            }
            if (age % 5 == 0 && !minimal()) {
                level.addParticle(PowerRegistries.PLAGUE_MIST.get(), at.x + (level.random.nextDouble() - 0.5), at.y + 0.6,
                        at.z + (level.random.nextDouble() - 0.5), 0, 0.01, 0);
            }
            return age < duration;
        }
    }
}
