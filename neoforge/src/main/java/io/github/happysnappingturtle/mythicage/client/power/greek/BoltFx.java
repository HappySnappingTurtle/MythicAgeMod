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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 雷击的表现（03 号分镜 GP-GR-01；用户 2026-10-17 要求「很高的云直接劈下来」）：
 * 锁定后目标脚下金白落点环收拢；头顶 16 格高空乌云由小到大聚成扁平的一大片（半径 6 格），云底电花越闪越密、最后几刻云底乱窜细弧（15 刻，
 * 跟随目标）→ 命中：一道从云底直贯到目标的分叉长雷束（头两刻闪烁、第 4 刻回闪一次）+ 天空闪白 + 落点短闪、
 * 电花与地面碎屑迸散、冲击环外扩 → 淡紫余弧约 0.35 秒衰减，乌云慢慢散开。
 * 低特效（原版「粒子：最少」）只留落点环、少量云与主束。
 */
public final class BoltFx implements PowerFxRenderer {
    static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/bolt_ring.png");
    static final int WARNING = 15;
    /** 云底离目标脚底的高度。 */
    static final double CLOUD_HEIGHT = 16;

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        switch (p.phase()) {
            case COMMIT -> {
                if (!p.targets().isEmpty()) {
                    ClientPowerFx.add(new Warning(p.castId(), p.targets().getFirst(), p.origin(), level.getGameTime()));
                }
            }
            case HIT -> {
                ClientPowerFx.removeIf(fx -> fx instanceof Warning w && w.castId.equals(p.castId()));
                Entity t = p.targets().isEmpty() ? null : level.getEntity(p.targets().getFirst());
                Vec3 bottom = t != null ? t.position() : p.origin();
                double h = t != null ? t.getBbHeight() : 1.8;
                ClientPowerFx.add(new Strike(bottom, bottom.add(0, CLOUD_HEIGHT - 0.6, 0), p.seed(), level.getGameTime()));
                level.setSkyFlashTime(3);
                level.addParticle(ParticleTypes.FLASH, bottom.x, bottom.y + h * 0.5, bottom.z, 0, 0, 0);
                RandomSource r = RandomSource.create(p.seed());
                for (int i = 0; i < 40; i++) {
                    level.addParticle(PowerRegistries.SPARK.get(), bottom.x, bottom.y + 0.1, bottom.z,
                            (r.nextDouble() - 0.5) * 0.7, r.nextDouble() * 0.45, (r.nextDouble() - 0.5) * 0.7);
                }
                // 落点的地面碎屑
                BlockState ground = level.getBlockState(BlockPos.containing(bottom).below());
                if (!ground.isAir() && !minimal()) {
                    for (int i = 0; i < 24; i++) {
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                                bottom.x + (r.nextDouble() - 0.5) * 1.2, bottom.y + 0.1, bottom.z + (r.nextDouble() - 0.5) * 1.2,
                                (r.nextDouble() - 0.5) * 0.5, 0.25 + r.nextDouble() * 0.35, (r.nextDouble() - 0.5) * 0.5);
                    }
                }
            }
            case END -> ClientPowerFx.removeIf(fx -> fx instanceof Warning w && w.castId.equals(p.castId()));
            default -> {
            }
        }
    }

    static boolean minimal() {
        return Minecraft.getInstance().options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL;
    }

    /** 预警：落点环 + 跟随目标的高空乌云。 */
    static final class Warning implements ActiveFx {
        final String castId;
        final int target;
        final long start;
        Vec3 at;

        Warning(String castId, int target, Vec3 at, long start) {
            this.castId = castId;
            this.target = target;
            this.at = at;
            this.start = start;
        }

        @Override
        public String castId() {
            return castId;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            long age = now - start;
            Entity e = level.getEntity(target);
            if (e != null) {
                at = e.position();
            }
            RandomSource r = level.random;
            Vec3 cloud = at.add(0, CLOUD_HEIGHT, 0);
            // 乌云由小到大聚起：扁平的一大片，半径 2 → 6 格
            double spread = 2 + 4 * Math.min(1, age / 8.0);
            int puffs = minimal() ? 3 : 16;
            for (int i = 0; i < puffs; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(r.nextDouble()) * spread;
                // 高空的云离镜头常超过 32 格，普通粒子会被剔除
                level.addAlwaysVisibleParticle(PowerRegistries.STORM_CLOUD.get(), cloud.x + Math.cos(a) * d,
                        cloud.y + 0.3 + r.nextDouble() * 0.6 * (1 - d / spread * 0.6), cloud.z + Math.sin(a) * d, 0, 0, 0);
            }
            // 云底蓄亮：越接近落雷电花越密
            int sparks = minimal() ? 0 : 1 + (int) (age * 6 / WARNING);
            for (int i = 0; i < sparks; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(r.nextDouble()) * spread * 0.8;
                level.addAlwaysVisibleParticle(PowerRegistries.SPARK.get(), cloud.x + Math.cos(a) * d, cloud.y + 0.1,
                        cloud.z + Math.sin(a) * d, 0, -0.03, 0);
            }
            // 服务端没发命中（丢包）时最多多留 1 秒
            return age < WARNING + 20;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float age = now - start + pt;
            float k = Math.min(1, age / WARNING);
            float r = 1.8f - 0.9f * k;
            FxDraw.flat(pose, buffers, RING, at.add(0, 0.03, 0), r, age * 9f, FxDraw.argb(0.6f + 0.4f * k, 0xFFF4C8));
            // 最后 6 刻云底乱窜细弧，预告落雷
            if (age > WARNING - 6 && age < WARNING && !minimal()) {
                Vec3 base = at.add(0, CLOUD_HEIGHT, 0);
                for (int i = 0; i < 3; i++) {
                    FxDraw.arc(pose, buffers, base.add((i - 1) * 1.2, 0, (i % 2) * 0.8), (long) age * 13 + i, 2.4f, 0.8f);
                }
            }
        }
    }

    /** 命中：长雷束闪烁 + 回闪，冲击环外扩，余弧衰减。 */
    static final class Strike implements ActiveFx {
        final Vec3 bottom;
        final Vec3 top;
        final long seed;
        final long start;

        Strike(Vec3 bottom, Vec3 top, long seed, long start) {
            this.bottom = bottom;
            this.top = top;
            this.seed = seed;
            this.start = start;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            return now - start < 12;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float age = now - start + pt;
            // 0–2 刻全亮且每刻换折点（闪烁）；第 3 刻暗下去；第 4 刻沿另一条路径回闪；之后衰减到第 8 刻
            if (age < 3) {
                FxDraw.bolt(pose, buffers, top, bottom, seed + (long) age, 1f);
            } else if (age < 4) {
                FxDraw.bolt(pose, buffers, top, bottom, seed + 2, 0.35f);
            } else if (age < 8) {
                FxDraw.bolt(pose, buffers, top, bottom, seed + 7, 0.9f * (1 - (age - 4) / 4f));
            }
            // 冲击环：0.5 → 3.5 格外扩并淡出
            if (age < 9) {
                float t = age / 9f;
                FxDraw.flat(pose, buffers, RING, bottom.add(0, 0.05, 0), 0.5f + 3f * t, age * 4,
                        FxDraw.argb(0.9f * (1 - t), 0xFFE9A8));
            }
            if (age >= 2 && !minimal()) {
                // 余弧：贴着落点向四周乱跳，约 0.35 秒衰减完
                float k = Math.max(0, 1f - (age - 2) / 7f);
                for (int i = 0; i < 4; i++) {
                    FxDraw.arc(pose, buffers, bottom.add(0, 0.15, 0), seed * 31 + i * 7 + (long) age, 1.3f, k);
                }
            }
        }
    }
}
