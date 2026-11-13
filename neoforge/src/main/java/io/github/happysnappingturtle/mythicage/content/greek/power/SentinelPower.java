package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.power.PowerReason;
import io.github.happysnappingturtle.mythicage.core.power.SentinelRules;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.TransientEntities;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import io.github.happysnappingturtle.mythicage.shrine.ShrineRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 哨兵石像 GP-GR-03（T13 设计文档 §4.2；05 号）：吟唱结束时在落点四角 (±2, ±2) 各找一个站位（高度 0、+1、−1、+2、−2，
 * 1×2 空间 + 下方实心）；**4 个都找到才提交**，否则失败、不消耗。寿命在提交时定：落点 32 格内有施法者自己的希腊神坛
 * → 3 个 MC 日，否则 1 个 MC 日。4 尊作为一组产物登记，新的一组替换旧的（旧组逐尊碎化）。
 */
public final class SentinelPower implements GodPower {
    @Nullable
    private static volatile Long lifetimeOverride;

    @Override
    public String id() {
        return GreekPowers.SENTINEL;
    }

    static BlockPos center(CastContext ctx) {
        return BlockPos.containing(ctx.origin());
    }

    /** 一个角能不能站：脚下结实、身体两格没有碰撞、不是液体。服务端判定与客户端瞄准预览共用。 */
    static boolean standable(Level level, BlockPos p) {
        return level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && level.getFluidState(p).isEmpty() && level.getFluidState(p.above()).isEmpty();
    }

    /** 每个角的站位（按 {@link SentinelRules#CORNERS} 的顺序）；找不到的角为空。 */
    public static List<Optional<BlockPos>> cornerSpots(Level level, BlockPos center) {
        List<Optional<BlockPos>> out = new ArrayList<>();
        for (int[] c : SentinelRules.CORNERS) {
            BlockPos found = null;
            for (int dy : SentinelRules.HEIGHTS) {
                BlockPos p = center.offset(c[0], dy, c[1]);
                if (standable(level, p)) {
                    found = p;
                    break;
                }
            }
            out.add(Optional.ofNullable(found));
        }
        return out;
    }

    /** 四角站位；有一个角找不到就为空。 */
    public static Optional<List<BlockPos>> spots(Level level, BlockPos center) {
        List<Optional<BlockPos>> corners = cornerSpots(level, center);
        return corners.stream().allMatch(Optional::isPresent)
                ? Optional.of(corners.stream().map(Optional::get).toList()) : Optional.empty();
    }

    @Override
    public Optional<PowerReason> validate(CastContext ctx) {
        return spots(ctx.level(), center(ctx)).isPresent() ? Optional.empty() : Optional.of(PowerReason.PLACEMENT_INVALID);
    }

    /** 落点到施法者最近一座希腊神坛（GA-13：供奉的村庄神庙，及旧的玩家神坛；同维度）的距离；没有时为无穷远。 */
    static double nearestOwnShrine(ServerPlayer caster, ServerLevel level, Vec3 at) {
        double best = Double.POSITIVE_INFINITY;
        List<net.minecraft.core.GlobalPos> altars = new java.util.ArrayList<>(
                io.github.happysnappingturtle.mythicage.village.temple.VillageTemples.devotedAltars(level.getServer(), caster.getUUID(),
                        Civilization.GREEK));
        ShrineRegistry.get(level.getServer()).ownedBy(caster.getUUID(), Civilization.GREEK).forEach(e -> altars.add(e.pos()));
        for (net.minecraft.core.GlobalPos g : altars) {
            if (g.dimension() == level.dimension()) {
                best = Math.min(best, Vec3.atBottomCenterOf(g.pos()).distanceTo(at));
            }
        }
        return best;
    }

    @Override
    public void execute(CastContext ctx) {
        ServerLevel level = ctx.level();
        ServerPlayer caster = ctx.caster();
        BlockPos c = center(ctx);
        List<BlockPos> spots = spots(level, c).orElse(List.of());
        if (spots.isEmpty()) {
            return;
        }
        Long o = lifetimeOverride;
        long lifetime = o != null ? o : SentinelRules.lifetime(nearestOwnShrine(caster, level, ctx.origin()));
        long expires = WorldClock.now(level.getServer()) + lifetime;
        List<SentinelEntity> made = new ArrayList<>();
        for (BlockPos p : spots) {
            SentinelEntity s = GreekPowers.SENTINEL_ENTITY.get().create(level);
            if (s == null) {
                continue;
            }
            // 背对中心，面朝外
            float yaw = (float) (Mth.atan2(p.getZ() - c.getZ(), p.getX() - c.getX()) * Mth.RAD_TO_DEG) - 90f;
            s.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0);
            s.setYHeadRot(yaw);
            s.setYBodyRot(yaw);
            s.setup(caster.getUUID(), expires);
            net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(s, level, level.getCurrentDifficultyAt(p), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(s);
            made.add(s);
        }
        TransientEntities.registerProducts(caster, ctx.spec().id(), made);
        GreekPowers.play(level, ctx.origin(), "gp.gr.sentinel.rise", 1f, 1f);
        PowerFx.emitAll(ctx, PowerFxPayload.Phase.HIT, made, "rise");
    }

    /** 载入时已过期、或碎化到一半的哨兵不再加入世界。 */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof SentinelEntity s
                && rejectOnLoad(s, WorldClock.now(level.getServer()))) {
            event.setCanceled(true);
        }
    }

    /** 从存档载入时是否丢弃：已过期，或碎化到一半。 */
    public static boolean rejectOnLoad(SentinelEntity s, long now) {
        return s.retiring() || s.expiresAt() <= now;
    }

    /** 只供 GameTest：之后施放的哨兵用给定寿命；传 null 恢复。 */
    public static void lifetimeOverrideForTest(@Nullable Long ticks) {
        lifetimeOverride = ticks;
    }

    /** 只供 GameTest：寿命规则（不改寿命覆盖）。 */
    public static long lifetimeFor(ServerPlayer caster, ServerLevel level, Vec3 at) {
        return SentinelRules.lifetime(nearestOwnShrine(caster, level, at));
    }
}
