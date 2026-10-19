package io.github.happysnappingturtle.mythicage.client.power.greek;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.client.power.ActiveFx;
import io.github.happysnappingturtle.mythicage.client.power.ClientPowerFx;
import io.github.happysnappingturtle.mythicage.client.power.FxDraw;
import io.github.happysnappingturtle.mythicage.client.power.PowerFxRenderer;
import io.github.happysnappingturtle.mythicage.content.greek.power.GreekPowers;
import io.github.happysnappingturtle.mythicage.content.greek.power.LureStoneBlock;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.power.LureRules;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import io.github.happysnappingturtle.mythicage.power.PowerZones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 诱兽石的表现（T13 设计文档 §4.1；05 号分镜 GP-GR-02）：
 * <ul>
 *   <li>瞄准：两格高的放置框（放得下为海蓝、放不下为红）+ 半径 48 的贝纹浪边界；</li>
 *   <li>立起（{@code rise}）：地面炸开碎土与石屑、一团水花，几点海光升起；随后边界浪从石脚 2 秒扩到 48 格再淡去；</li>
 *   <li>持续：石旁的刻纹上不时升起海蓝光点（只在玩家附近）；</li>
 *   <li>每 5 秒（{@code pulse}）：一圈贝纹浪从石脚向外推到 10 格并淡去；</li>
 *   <li>新吸引的动物（{@code attract}）：身上冒出 2 秒的引路光点，飘向石头；</li>
 *   <li>结束（{@code sink}）：石屑与尘土落下、水花，随后方块复原。</li>
 * </ul>
 * 设计里的「方块 10 刻升起动画」没有做成方块实体：临时方块不该带方块实体（{@code TempBlocks} 拒绝有方块实体的格子），
 * 改由立起时的碎土与水花表现。
 */
public final class LureFx implements PowerFxRenderer {
    static final ResourceLocation BAND = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/lure_band.png");
    static final int SEA = 0x58B9C2;
    static final float[] OK = {0.35f, 0.78f, 0.82f};
    static final float[] BAD = {0.9f, 0.25f, 0.2f};

    // ───────────── 瞄准 ─────────────

    /** 与 {@code LurePower.validate} 相同的判定（客户端只作提示，以服务端为准）。 */
    static boolean fits(ClientLevel level, BlockPos place) {
        BlockPos below = place.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        for (BlockPos p : new BlockPos[]{place, place.above()}) {
            if (!level.getBlockState(p).canBeReplaced() || level.getBlockEntity(p) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean renderPlacement(PoseStack pose, MultiBufferSource buffers, ClientLevel level, BlockPos place, long gameTime,
                                   float partialTick) {
        float[] c = fits(level, place) ? OK : BAD;
        FxDraw.band(pose, buffers, BAND, Vec3.atBottomCenterOf(place).add(0, 0.06, 0), LureRules.RADIUS, 1.2f, 3f,
                FxDraw.argb(0.75f, 0xFFFFFF));
        // 画过贴图后换了缓冲，线框缓冲要重新取
        AABB box = new AABB(place).expandTowards(0, 1, 0).inflate(0.002);
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, c[0], c[1], c[2], 0.9f);
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box.deflate(0.15), c[0], c[1], c[2], 0.35f);
        return true;
    }

    // ───────────── 事件 ─────────────

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        long now = level.getGameTime();
        switch (p.phase()) {
            case HIT -> {
                Double radius = PowerZones.radiusOf(p.tag());
                if (radius != null) {
                    ClientPowerFx.removeIf(fx -> fx instanceof Zone z && z.castId.equals(p.castId()));
                    ClientPowerFx.add(new Zone(p.castId(), p.origin(), radius.floatValue(), now, p.duration()));
                } else if ("rise".equals(p.tag())) {
                    rise(level, p.origin(), p.seed());
                } else if ("pulse".equals(p.tag())) {
                    ClientPowerFx.add(new Ripple(p.castId(), p.origin(), now));
                } else if ("attract".equals(p.tag())) {
                    for (int id : p.targets()) {
                        ClientPowerFx.add(new Lead(p.castId(), id, p.origin(), now));
                    }
                }
            }
            case END -> {
                ClientPowerFx.removeIf(fx -> fx.castId().equals(p.castId()));
                sink(level, p.origin(), p.seed());
            }
            default -> {
            }
        }
    }

    static boolean minimal() {
        return Minecraft.getInstance().options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL;
    }

    static BlockParticleOption stoneChip() {
        BlockState s = GreekPowers.LURE_STONE.get().defaultBlockState().setValue(LureStoneBlock.HALF, DoubleBlockHalf.UPPER);
        return new BlockParticleOption(ParticleTypes.BLOCK, s);
    }

    /** 立起：碎土四溅、石屑、一团水花、几点海光升起。 */
    static void rise(ClientLevel level, Vec3 at, long seed) {
        RandomSource r = RandomSource.create(seed);
        BlockState ground = level.getBlockState(BlockPos.containing(at).below());
        BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, ground.isAir() ? GreekPowers.LURE_STONE.get()
                .defaultBlockState() : ground);
        for (int i = 0; i < 36; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 0.5 + r.nextDouble() * 0.4;
            level.addParticle(dirt, at.x + Math.cos(a) * d, at.y + 0.1, at.z + Math.sin(a) * d, Math.cos(a) * 0.25,
                    0.2 + r.nextDouble() * 0.3, Math.sin(a) * 0.25);
        }
        for (int i = 0; i < 16; i++) {
            level.addParticle(stoneChip(), at.x + (r.nextDouble() - 0.5), at.y + r.nextDouble() * 2, at.z + (r.nextDouble() - 0.5),
                    (r.nextDouble() - 0.5) * 0.2, 0.1, (r.nextDouble() - 0.5) * 0.2);
        }
        for (int i = 0; i < 24; i++) {
            level.addParticle(ParticleTypes.SPLASH, at.x + (r.nextDouble() - 0.5) * 1.4, at.y + 0.2 + r.nextDouble() * 1.6,
                    at.z + (r.nextDouble() - 0.5) * 1.4, 0, 0.2, 0);
        }
        for (int i = 0; i < 10; i++) {
            level.addParticle(PowerRegistries.SEA_MOTE.get(), at.x + (r.nextDouble() - 0.5) * 0.9, at.y + 0.3 + r.nextDouble() * 1.4,
                    at.z + (r.nextDouble() - 0.5) * 0.9, 0, 0.02, 0);
        }
    }

    /** 收回：石屑与尘土落下、水花。 */
    static void sink(ClientLevel level, Vec3 at, long seed) {
        RandomSource r = RandomSource.create(seed + 1);
        for (int i = 0; i < 28; i++) {
            level.addParticle(stoneChip(), at.x + (r.nextDouble() - 0.5) * 0.9, at.y + r.nextDouble() * 2, at.z + (r.nextDouble() - 0.5) * 0.9,
                    (r.nextDouble() - 0.5) * 0.1, -0.05, (r.nextDouble() - 0.5) * 0.1);
        }
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.POOF, at.x + (r.nextDouble() - 0.5) * 1.2, at.y + 0.2 + r.nextDouble() * 0.8,
                    at.z + (r.nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
        }
        for (int i = 0; i < 14; i++) {
            level.addParticle(ParticleTypes.SPLASH, at.x + (r.nextDouble() - 0.5) * 1.2, at.y + 0.2, at.z + (r.nextDouble() - 0.5) * 1.2,
                    0, 0.15, 0);
        }
    }

    // ───────────── 持续表现 ─────────────

    /** 区域：开头 2 秒边界浪从石脚扩到半径并淡去；之后刻纹上不时升起海光（只在玩家附近）。 */
    static final class Zone implements ActiveFx {
        static final int ANNOUNCE = 40;
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
            if (age % 6 == 0 && !minimal() && player != null && player.position().distanceTo(center) < radius + 16) {
                RandomSource r = level.random;
                // 南北两面刻纹的位置：石面在中心前后约 0.3 格，高 0.5–1.8 格
                double side = r.nextBoolean() ? 0.36 : -0.36;
                level.addParticle(PowerRegistries.SEA_MOTE.get(), center.x + 0.06 + (r.nextDouble() - 0.5) * 0.4,
                        center.y + 0.5 + r.nextDouble() * 1.3, center.z + side, 0, 0.01, side * 0.02);
            }
            return age < duration;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float age = now - start + pt;
            if (age >= ANNOUNCE) {
                return;
            }
            float t = age / ANNOUNCE;
            float r = 1.0f + (radius - 1.0f) * (float) Math.sqrt(t);
            float alpha = t < 0.7f ? 0.95f : 0.95f * (1 - (t - 0.7f) / 0.3f);
            FxDraw.band(pose, buffers, BAND, center.add(0, 0.06, 0), r, 1.6f, 3f, FxDraw.argb(alpha, 0xFFFFFF));
        }
    }

    /** 一圈贝纹浪：2.5 秒从石脚推到 10 格并淡去；起始时一圈海光向外散。 */
    static final class Ripple implements ActiveFx {
        static final int LIFE = 50;
        static final float REACH = 10f;
        final String castId;
        final Vec3 center;
        final long start;

        Ripple(String castId, Vec3 center, long start) {
            this.castId = castId;
            this.center = center;
            this.start = start;
        }

        @Override
        public String castId() {
            return castId;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            if (now == start && !minimal()) {
                for (int i = 0; i < 12; i++) {
                    double a = i / 12.0 * Math.PI * 2;
                    level.addParticle(PowerRegistries.SEA_MOTE.get(), center.x + Math.cos(a) * 1.2, center.y + 0.2,
                            center.z + Math.sin(a) * 1.2, Math.cos(a) * 0.08, 0.01, Math.sin(a) * 0.08);
                }
            }
            return now - start < LIFE;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float t = Math.min(1, (now - start + pt) / LIFE);
            float ease = 1 - (1 - t) * (1 - t);
            FxDraw.band(pose, buffers, BAND, center.add(0, 0.07, 0), 1.2f + (REACH - 1.2f) * ease, 1.0f, 3f,
                    FxDraw.argb(0.9f * (1 - t), 0xFFFFFF));
        }
    }

    /** 被吸引的动物：2 秒内每 5 刻冒一点海光，飘向石头。 */
    static final class Lead implements ActiveFx {
        static final int LIFE = 40;
        final String castId;
        final int entityId;
        final Vec3 stone;
        final long start;

        Lead(String castId, int entityId, Vec3 stone, long start) {
            this.castId = castId;
            this.entityId = entityId;
            this.stone = stone;
            this.start = start;
        }

        @Override
        public String castId() {
            return castId;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            Entity e = level.getEntity(entityId);
            if (e == null) {
                return false;
            }
            if ((now - start) % 5 == 0 && !minimal()) {
                Vec3 at = e.position().add(0, e.getBbHeight() * 0.8, 0);
                Vec3 dir = stone.add(0, 1, 0).subtract(at).normalize().scale(0.07);
                level.addParticle(PowerRegistries.SEA_MOTE.get(), at.x, at.y, at.z, dir.x, dir.y, dir.z);
            }
            return now - start < LIFE;
        }
    }
}
