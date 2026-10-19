package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.power.CastContext;
import io.github.happysnappingturtle.mythicage.power.GodPower;
import io.github.happysnappingturtle.mythicage.power.PowerFx;
import io.github.happysnappingturtle.mythicage.power.PowerFxPayload;
import io.github.happysnappingturtle.mythicage.power.PowerSpecLoader;
import io.github.happysnappingturtle.mythicage.power.PowerThrow;
import io.github.happysnappingturtle.mythicage.power.PowerZones;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 瘟疫 GP-GR-06（T13 设计文档 §3.3）：投掷陶瓮，落地登记 5 分钟的瘟疫区域（{@link PowerZones}，存档）：
 * <ul>
 *   <li>每 20 刻给区域内反应为 {@code afflict} / {@code pvp} 的实体续虚弱 II + 缓慢 I（各 40 刻）与「瘟疫」标记（Boss 不给标记）；</li>
 *   <li>带标记的实体：开始使用弓、弩、三叉戟时取消；生物作为主人发出的弹射物、唤魔者尖牙、恼鬼不允许加入世界
 *       （玩家只拦箭与三叉戟）——不必逐个怪改 AI，新怪也自动覆盖；</li>
 *   <li>区域内刷怪笼、试炼刷怪器停摆（{@code SpawnerStall}）；怪物的自然生成与巡逻生成判失败。</li>
 * </ul>
 */
public final class PestilencePower implements GodPower {
    static final String KIND = "pestilence";
    static final int STATUS_TICKS = 40;

    @Override
    public String id() {
        return GreekPowers.PESTILENCE;
    }

    @Override
    public void execute(CastContext ctx) {
        PowerThrow.launch(ctx, () -> land(ctx));
    }

    /** 陶瓮落地：登记区域、找出区域内的刷怪器、立即生效一次。 */
    static void land(CastContext ctx) {
        ServerLevel level = ctx.level();
        long now = WorldClock.now(level.getServer());
        int duration = ctx.spec().durationTicks();
        PowerZones.Zone zone = new PowerZones.Zone(ctx.castId(), KIND, ctx.spec().id(), level.dimension(), ctx.origin(),
                ctx.spec().radius(), now + duration, ctx.caster().getUUID(), List.of());
        zone = zone.withMarks(spawners(level, zone));
        PowerZones.add(level.getServer(), zone);
        GreekPowers.play(level, ctx.origin(), "gp.gr.pestilence.shatter", 1.2f, 1f);
        PowerFx.emit(ctx, PowerFxPayload.Phase.HIT, List.of(), ctx.origin(), duration, "zone:" + zone.radius());
        for (BlockPos m : zone.marks()) {
            PowerFx.emit(ctx, PowerFxPayload.Phase.HIT, List.of(), Vec3.atCenterOf(m), duration, "mark");
            GreekPowers.play(level, Vec3.atCenterOf(m), "gp.gr.pestilence.spawner_seal", 1f, 1f);
        }
        afflict(level, zone);
    }

    /** 区域内的刷怪笼与试炼刷怪器（遍历已载入区块的方块实体）。 */
    static List<BlockPos> spawners(ServerLevel level, PowerZones.Zone zone) {
        List<BlockPos> out = new ArrayList<>();
        int r = (int) Math.ceil(zone.radius());
        ChunkPos min = new ChunkPos(BlockPos.containing(zone.center().add(-r, 0, -r)));
        ChunkPos max = new ChunkPos(BlockPos.containing(zone.center().add(r, 0, r)));
        for (int cx = min.x; cx <= max.x; cx++) {
            for (int cz = min.z; cz <= max.z; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if ((be instanceof SpawnerBlockEntity || be instanceof TrialSpawnerBlockEntity) && zone.covers(be.getBlockPos())) {
                        out.add(be.getBlockPos().immutable());
                    }
                }
            }
        }
        return out;
    }

    /** 给区域内的敌方续状态。施法者离线时只对实现 Enemy 的生物生效。 */
    static void afflict(ServerLevel level, PowerZones.Zone zone) {
        var spec = PowerSpecLoader.get(zone.powerId()).orElse(null);
        if (spec == null) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(zone.owner());
        Set<UUID> own = owner == null ? Set.of() : TargetFactsCollector.ownVillages(owner);
        double r = zone.radius();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(zone.center(), zone.center()).inflate(r, 16, r), x -> x.isAlive() && zone.covers(x.position()))) {
            boolean hit;
            if (owner != null) {
                hit = !TargetClassifier.NONE.equals(TargetClassifier.reaction(spec,
                        TargetFactsCollector.collect(owner, e, own)));
            } else {
                hit = e instanceof Enemy && !(e instanceof Player);
            }
            if (!hit) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STATUS_TICKS, 1, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STATUS_TICKS, 0, false, true));
            if (!TargetFactsCollector.isBoss(e)) {
                e.addEffect(new MobEffectInstance(GreekPowers.PLAGUED, STATUS_TICKS, 0, false, false, true));
            }
        }
    }

    /** 区域行为：逐刻续状态；到期发结束表现。 */
    static final PowerZones.Kind ZONE = new PowerZones.Kind() {
        @Override
        public void tick(ServerLevel level, PowerZones.Zone zone, long now) {
            afflict(level, zone);
        }

        @Override
        public void end(ServerLevel level, PowerZones.Zone zone) {
            GreekPowers.play(level, zone.center(), "gp.gr.pestilence.end", 1f, 1f);
            var p = new PowerFxPayload(zone.castId(), zone.powerId(), PowerFxPayload.Phase.END, -1, level.dimension().location(),
                    zone.center(), List.of(), WorldClock.now(level.getServer()), 0, 0, "", "");
            for (ServerPlayer to : io.github.happysnappingturtle.mythicage.power.PowerFx.recipients(level, null, zone.center())) {
                io.github.happysnappingturtle.mythicage.power.PowerFx.sendRaw(to, p);
            }
        }
    };

    // ───────────── 远程封禁、自然生成 ─────────────

    static boolean plagued(Entity e) {
        return e instanceof LivingEntity l && l.hasEffect(GreekPowers.PLAGUED);
    }

    /** 带标记的实体开始使用弓 / 弩 / 三叉戟：取消（骷髅、掠夺者拉不开弓，玩家同理）。 */
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (plagued(event.getEntity()) && event.getItem().is(GreekPowers.RANGED_WEAPONS)) {
            event.setCanceled(true);
        }
    }

    /** 带标记的生物发出的弹射物、尖牙、恼鬼不加入世界；带标记的玩家只拦箭与三叉戟。 */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity e = event.getEntity();
        Entity owner = null;
        if (e instanceof Projectile p) {
            owner = p.getOwner();
        } else if (e instanceof EvokerFangs f) {
            owner = f.getOwner();
        } else if (e instanceof net.minecraft.world.entity.monster.Vex v) {
            owner = v.getOwner();
        } else if (e instanceof TraceableEntity t && !(e instanceof OwnableEntity)) {
            owner = t.getOwner();
        }
        if (owner == null || !plagued(owner)) {
            return;
        }
        if (owner instanceof Player && !(e instanceof AbstractArrow)) {
            return;
        }
        event.setCanceled(true);
    }

    /** 区域内怪物的自然生成与巡逻生成判失败（动物照常）。 */
    public static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        var type = event.getSpawnType();
        if ((type == net.minecraft.world.entity.MobSpawnType.NATURAL || type == net.minecraft.world.entity.MobSpawnType.PATROL)
                && event.getEntity().getType().getCategory() == MobCategory.MONSTER
                && event.getLevel() instanceof ServerLevel sl
                && !PowerZones.at(sl, new Vec3(event.getX(), event.getY(), event.getZ()), KIND).isEmpty()) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
