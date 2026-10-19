package io.github.happysnappingturtle.mythicage.client.power.greek;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.client.power.ActiveFx;
import io.github.happysnappingturtle.mythicage.client.power.ClientPowerFx;
import io.github.happysnappingturtle.mythicage.client.power.FxDraw;
import io.github.happysnappingturtle.mythicage.client.power.PowerFxRenderer;
import io.github.happysnappingturtle.mythicage.content.greek.power.SentinelPower;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.power.SentinelRules;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 哨兵石像的表现（T13 设计文档 §4.2；05 号分镜 GP-GR-03）：
 * <ul>
 *   <li>瞄准：四角各一枚暗绿站位符（站得下）或红框（站不下，4 个都站得下才能施放）；</li>
 *   <li>吟唱：四角站位符逐个亮起（间隔 6 刻），缓慢呼吸，符上飘起符光；吟唱取消时熄灭；</li>
 *   <li>立起（{@code rise}）：站位符一闪后 1.5 秒淡去，每尊脚下碎土四溅、一圈符光散开；</li>
 *   <li>石像自身的升起尘土、拉弓时弓臂微光、碎化时的黑石碎屑由实体在客户端按姿态发出，箭的拖尾由服务端发
 *       （见 {@code SentinelEntity}）。</li>
 * </ul>
 */
public final class SentinelFx implements PowerFxRenderer {
    static final ResourceLocation RUNE = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/sentinel_rune.png");
    static final float[] BAD = {0.9f, 0.25f, 0.2f};

    // ───────────── 瞄准 ─────────────

    @Override
    public boolean renderPlacement(PoseStack pose, MultiBufferSource buffers, ClientLevel level, BlockPos place, long gameTime,
                                   float partialTick) {
        List<Optional<BlockPos>> corners = SentinelPower.cornerSpots(level, place);
        float breathe = 0.75f + 0.2f * (float) Math.sin((gameTime + partialTick) * 0.15);
        List<AABB> bad = new ArrayList<>();
        for (int i = 0; i < corners.size(); i++) {
            Optional<BlockPos> spot = corners.get(i);
            if (spot.isPresent()) {
                FxDraw.flat(pose, buffers, RUNE, Vec3.atBottomCenterOf(spot.get()).add(0, 0.04, 0), 0.55f, 0,
                        FxDraw.argb(breathe, 0xFFFFFF));
            } else {
                int[] c = SentinelRules.CORNERS.get(i);
                bad.add(new AABB(place.offset(c[0], 0, c[1])).expandTowards(0, 1, 0).inflate(0.002));
            }
        }
        // 画过贴图后换了缓冲，线框缓冲要重新取
        for (AABB box : bad) {
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, BAD[0], BAD[1], BAD[2], 0.9f);
        }
        AABB center = new AABB(place).inflate(0.002);
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), center, 0.25f, 0.82f, 0.54f, 0.5f);
        return true;
    }

    // ───────────── 事件 ─────────────

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        long now = level.getGameTime();
        switch (p.phase()) {
            case CHANNEL_START -> {
                BlockPos center = BlockPos.containing(p.origin());
                List<Vec3> spots = SentinelPower.cornerSpots(level, center).stream().flatMap(Optional::stream)
                        .map(Vec3::atBottomCenterOf).toList();
                ClientPowerFx.removeIf(fx -> fx instanceof Runes r && r.castId.equals(p.castId()));
                ClientPowerFx.add(new Runes(p.castId(), spots, now, p.duration()));
            }
            case CHANNEL_CANCEL -> ClientPowerFx.removeIf(fx -> fx instanceof Runes r && r.castId.equals(p.castId()));
            case HIT -> {
                if (!"rise".equals(p.tag())) {
                    return;
                }
                List<Vec3> at = new ArrayList<>();
                for (int id : p.targets()) {
                    Entity e = level.getEntity(id);
                    if (e != null) {
                        at.add(e.position());
                    }
                }
                // 旁观者可能没收到吟唱（离得远），按石像位置补一组站位符
                boolean[] found = {false};
                ClientPowerFx.forEach(fx -> {
                    if (fx instanceof Runes r && r.castId.equals(p.castId())) {
                        r.flash(now);
                        found[0] = true;
                    }
                });
                if (!found[0] && !at.isEmpty()) {
                    Runes r = new Runes(p.castId(), at, now - 30, 0);
                    r.flash(now);
                    ClientPowerFx.add(r);
                }
                RandomSource rnd = RandomSource.create(p.seed());
                for (Vec3 v : at) {
                    burst(level, v, rnd);
                }
            }
            default -> {
            }
        }
    }

    /** 一尊石像破土：碎土四溅、一圈符光散开。 */
    static void burst(ClientLevel level, Vec3 at, RandomSource r) {
        BlockState ground = level.getBlockState(BlockPos.containing(at).below());
        if (!ground.isAir()) {
            BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, ground);
            for (int i = 0; i < 20; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                level.addParticle(dirt, at.x + Math.cos(a) * 0.45, at.y + 0.1, at.z + Math.sin(a) * 0.45, Math.cos(a) * 0.2,
                        0.15 + r.nextDouble() * 0.25, Math.sin(a) * 0.2);
            }
        }
        for (int i = 0; i < 10; i++) {
            double a = i / 10.0 * Math.PI * 2;
            level.addParticle(PowerRegistries.RUNE_MOTE.get(), at.x + Math.cos(a) * 0.5, at.y + 0.1, at.z + Math.sin(a) * 0.5,
                    Math.cos(a) * 0.06, 0.02, Math.sin(a) * 0.06);
        }
    }

    /** 四角站位符：逐个亮起、呼吸；立起时一闪后淡去。 */
    static final class Runes implements ActiveFx {
        static final int STAGGER = 6;
        static final int APPEAR = 8;
        static final int FADE = 30;
        final String castId;
        final List<Vec3> spots;
        final long start;
        final int channel;
        long flashAt = -1;

        Runes(String castId, List<Vec3> spots, long start, int channel) {
            this.castId = castId;
            this.spots = List.copyOf(spots);
            this.start = start;
            this.channel = channel;
        }

        void flash(long now) {
            flashAt = now;
        }

        @Override
        public String castId() {
            return castId;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            long age = now - start;
            if (flashAt < 0 && age % 8 == 0) {
                for (int i = 0; i < spots.size(); i++) {
                    if (age >= (long) i * STAGGER) {
                        Vec3 s = spots.get(i);
                        level.addParticle(PowerRegistries.RUNE_MOTE.get(), s.x + (level.random.nextDouble() - 0.5) * 0.6,
                                s.y + 0.1, s.z + (level.random.nextDouble() - 0.5) * 0.6, 0, 0.03, 0);
                    }
                }
            }
            // 吟唱后迟迟没有立起（服务端失败）时也要收场
            return flashAt >= 0 ? now - flashAt < FADE : age < channel + 60;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float age = now - start + pt;
            for (int i = 0; i < spots.size(); i++) {
                float local = age - i * STAGGER;
                if (local <= 0) {
                    continue;
                }
                float appear = Math.min(1, local / APPEAR);
                float alpha;
                float size = 0.55f * (0.6f + 0.4f * appear);
                if (flashAt >= 0) {
                    float f = Math.min(1, (now - flashAt + pt) / FADE);
                    alpha = 1 - f;
                    size *= 1 + 0.35f * (float) Math.sin(Math.min(1, f * 4) * Math.PI);
                } else {
                    alpha = appear * (0.7f + 0.25f * (float) Math.sin(age * 0.2 + i));
                }
                FxDraw.flat(pose, buffers, RUNE, spots.get(i).add(0, 0.04, 0), size, 0, FxDraw.argb(alpha, 0xFFFFFF));
            }
        }
    }
}
