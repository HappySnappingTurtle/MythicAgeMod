package io.github.happysnappingturtle.mythicage.client.power.greek;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.happysnappingturtle.mythicage.client.ClientFx;
import io.github.happysnappingturtle.mythicage.client.power.ActiveFx;
import io.github.happysnappingturtle.mythicage.client.power.ClientPowerFx;
import io.github.happysnappingturtle.mythicage.client.power.FxDraw;
import io.github.happysnappingturtle.mythicage.client.power.PowerFxRenderer;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerZones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 停火的表现（T25a §2.3）：施法者脚下张开传令杖停战标记（信使金 + 天青）→ 贴着地形的边界光幕 1 秒内从施法者向外扩到 64 格，之后淡淡留着
 * → 自然到期淡出；破约时边界整圈碎成金色光屑。减少闪光（装饰从简）时只留边界线、不放光屑。
 */
public final class CeasefireFx implements PowerFxRenderer {
    static final ResourceLocation SEAL = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/ceasefire_seal.png");
    static final ResourceLocation CURTAIN = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "textures/fx/ceasefire_curtain.png");
    /** 光幕只画离镜头多少格内的那几段。 */
    static final double NEAR = 48;
    static final int GOLD = 0xE2B33C;
    static final float SEAL_RADIUS = 4.0f;

    @Override
    public AimStyle aimStyle() {
        return new AimStyle(SEAL, GOLD);
    }

    @Override
    public void play(PowerFxPayload p, ClientLevel level) {
        long now = level.getGameTime();
        switch (p.phase()) {
            case HIT -> {
                Double radius = PowerZones.radiusOf(p.tag());
                if (radius != null) {
                    ClientPowerFx.removeIf(fx -> fx instanceof Zone z && z.castId.equals(p.castId()));
                    ClientPowerFx.add(new Zone(p.castId(), p.origin(), radius.floatValue(), now, p.duration()));
                }
            }
            case END -> {
                // 先找出来再删再加：不能在 removeIf 的回调里往同一个列表里加
                java.util.List<Zone> ended = new java.util.ArrayList<>();
                ClientPowerFx.removeIf(fx -> {
                    if (fx instanceof Zone z && z.castId.equals(p.castId())) {
                        ended.add(z);
                        return true;
                    }
                    return false;
                });
                for (Zone z : ended) {
                    if ("break".equals(p.tag())) {
                        shatter(level, z);
                    } else {
                        ClientPowerFx.add(new Fade(z, now));
                    }
                }
            }
            default -> {
            }
        }
    }

    /** 破约：沿玩家附近的那段边界迸出金色光屑。 */
    static void shatter(ClientLevel level, Zone z) {
        if (ClientFx.decorativeMinimal()) {
            return;
        }
        var player = Minecraft.getInstance().player;
        RandomSource r = level.random;
        DustParticleOptions gold = new DustParticleOptions(new Vector3f(0.89f, 0.70f, 0.24f), 1.4f);
        DustParticleOptions sky = new DustParticleOptions(new Vector3f(0.49f, 0.77f, 0.88f), 1.0f);
        for (int i = 0; i < 160; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double x = z.center.x + Math.cos(a) * z.radius;
            double zz = z.center.z + Math.sin(a) * z.radius;
            if (player != null && player.distanceToSqr(x, player.getY(), zz) > 48 * 48) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(zz));
            level.addParticle(i % 3 == 0 ? sky : gold, x, y + 0.2 + r.nextDouble() * 1.5, zz,
                    (r.nextDouble() - 0.5) * 0.2, r.nextDouble() * 0.15, (r.nextDouble() - 0.5) * 0.2);
        }
    }

    /** 区域：停战标记 + 外扩后留作边界的光带。 */
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
            return now - start < duration + 40;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            draw(pose, buffers, now - start + pt, 1f);
        }

        void draw(PoseStack pose, MultiBufferSource buffers, float age, float fade) {
            float open = Math.min(1, age / 10f);
            if (!ClientFx.decorativeMinimal()) {
                FxDraw.flat(pose, buffers, SEAL, center.add(0, 0.05, 0), SEAL_RADIUS * (0.4f + 0.6f * open), age * 0.2f,
                        FxDraw.argb(0.9f * open * fade, 0xFFFFFF));
            }
            // 边界光幕：1 秒内从施法者向外扩到区域半径，每个顶点落在那里的地面上（64 格内地形起伏，平铺的光带会埋进地里）
            var mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            float t = Math.min(1, age / EXPAND);
            float r = Math.max(SEAL_RADIUS, radius * (float) Math.sqrt(t));
            float alpha = age < EXPAND ? 1f : Math.max(0.6f, 1f - (age - EXPAND) / 30f);
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            FxDraw.curtain(pose, buffers, CURTAIN, center, r, age < EXPAND ? 3.0f : 2.5f, 4f, FxDraw.argb(alpha * fade, 0xFFFFFF),
                    (x, z) -> ground(mc.level, x, z), eye, NEAR);
        }
    }

    /**
     * 地面高度：客户端没有「不含树叶」的高度图，从 MOTION_BLOCKING 往下跳过树叶与树干（最多 24 格），光幕不爬上树冠。
     */
    static double ground(ClientLevel level, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        var pos = new net.minecraft.core.BlockPos.MutableBlockPos(bx, y - 1, bz);
        for (int i = 0; i < 24 && y > level.getMinBuildHeight(); i++) {
            var st = level.getBlockState(pos.setY(y - 1));
            if (!st.is(net.minecraft.tags.BlockTags.LEAVES) && !st.is(net.minecraft.tags.BlockTags.LOGS) && !st.isAir()) {
                break;
            }
            y--;
        }
        return y;
    }

    /** 自然到期：一秒内淡出。 */
    static final class Fade implements ActiveFx {
        final Zone zone;
        final long start;

        Fade(Zone zone, long start) {
            this.zone = zone;
            this.start = start;
        }

        @Override
        public boolean tick(ClientLevel level, long now) {
            return now - start < 20;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, long now, float pt) {
            float f = 1 - Math.min(1, (now - start + pt) / 20f);
            zone.draw(pose, buffers, zone.duration, f);
        }
    }
}
