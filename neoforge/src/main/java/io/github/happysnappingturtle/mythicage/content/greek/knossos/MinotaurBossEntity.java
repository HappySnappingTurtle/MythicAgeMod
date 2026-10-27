package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import com.google.gson.JsonObject;
import io.github.happysnappingturtle.mythicage.boss.TrialArena;
import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import io.github.happysnappingturtle.mythicage.boss.TrialBoss;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.anim.ChargeTimeline;
import io.github.happysnappingturtle.mythicage.core.boss.ArenaPhase;
import io.github.happysnappingturtle.mythicage.core.boss.MechanicWindows;
import io.github.happysnappingturtle.mythicage.core.boss.MoveScheduler;
import io.github.happysnappingturtle.mythicage.core.boss.MoveScheduler.Move;
import io.github.happysnappingturtle.mythicage.entity.MinotaurEntity;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 弥诺陶洛斯 · 克诺索斯迷宫之主（T23 §6）：试炼 Boss 版。共用 T7 的模型与动画（变体 boss：放大 1.4、换贴图），数值单独配置
 * （{@code entity_stats/greek/minotaur_boss.json}），不听传令杖、不占召唤槽（卡片 MIN-01）。只在自己的竞技场大厅里活动，只打大厅里的合格玩家。
 * <ul>
 *   <li>招式由 {@link MoveScheduler} 选（同一时刻只有一招）：斧劈、冲锋、阶段二的连冲与掷碎石、追击；</li>
 *   <li>伤害全在服务端按有效刻判定，一次攻击每个目标只结算一次（MIN-02）；锁向后不再转身（MIN-03）；</li>
 *   <li>冲锋撞到柱 / 墙：竞技场按 {@link MechanicWindows} 判是否硬直（柱 60 刻、墙 40 刻，受伤 ×2）；撞柱时柱碎；
 *       连冲第一段撞上就取消第二段（MIN-04）；</li>
 *   <li>血量首次 ≤ 50%：当前招式结束后怒吼 {@code roar_ticks} 刻，然后竞技场开始机关墙。</li>
 * </ul>
 */
public class MinotaurBossEntity extends MinotaurEntity implements TrialBoss {
    public static final ResourceLocation RIG = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "minotaur");
    /** 斧劈扇形：前方 90°。 */
    public static final double AXE_HALF_ANGLE = 45;
    /** 冲锋扫掠：身体包围盒外扩。 */
    public static final double SWEEP_INFLATE = 0.3;
    /** 冲刺时往前探多远找挡路的格子。 */
    public static final double PROBE = 0.6;

    // 数值（entity_stats/greek/minotaur_boss.json，缺失时用这里的默认值）
    private float axeDamage = 12;
    private float chargeDamage = 16;
    private float throwDamage = 10;
    private int axeWindup = 16;
    private int axeLockAt = 10;
    private int axeActiveEnd = 20;
    private int axeTotal = 36;
    private int doubleGap = 12;
    private int throwWindup = 20;
    private int throwTotal = 40;
    private int roarTicks = 40;
    private int stunRecover = 16;

    private String arenaId = "";
    private Move move = Move.NONE;
    private int moveTick;
    private long axeReadyAt;
    private long chargeReadyAt;
    private long throwReadyAt;
    private final Set<UUID> hitThisMove = new HashSet<>();
    private UUID target;
    /** 连冲：第二段在 moveTick 到这个值时开始；−1 = 没有第二段。 */
    private int secondSegmentAt = -1;
    private int segment;
    private int stun;
    private int roar;
    private boolean phaseTwoPending;
    private boolean devicesReady;
    private Vec3 throwAt;
    private final List<BlockPos> blocking = new ArrayList<>();

    /** 只供 GameTest：暂停选新招（当前招式照常走完）。 */
    private boolean pausedForTest;
    /** 只供 GameTest：全部 Boss 都不自己选招（用例里用 {@link #forceMoveForTest} 出招）。 */
    public static volatile boolean pauseAllForTest;
    /** 只供 GameTest：每次命中回调（招式、目标）。 */
    public static volatile java.util.function.BiConsumer<Move, UUID> onHitForTest;

    public MinotaurBossEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            setVariant("boss");
            applyStats();
        }
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    private void applyStats() {
        SummonUnits.Stats st = SummonUnits.statsFile("greek", "minotaur_boss");
        var armor = getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(st.attr("armor", 6));
        }
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(st.attr("movement_speed", 0.28));
        }
        JsonObject sp = st.special();
        axeDamage = f(sp, "axe_damage", axeDamage);
        chargeDamage = f(sp, "charge_damage", chargeDamage);
        throwDamage = f(sp, "throw_damage", throwDamage);
        axeWindup = i(sp, "axe_windup", axeWindup);
        axeLockAt = i(sp, "axe_lock_at", axeLockAt);
        axeActiveEnd = i(sp, "axe_active_end", axeActiveEnd);
        axeTotal = i(sp, "axe_total", axeTotal);
        doubleGap = i(sp, "double_charge_gap", doubleGap);
        throwWindup = i(sp, "throw_windup", throwWindup);
        throwTotal = i(sp, "throw_total", throwTotal);
        roarTicks = i(sp, "roar_ticks", roarTicks);
        stunRecover = i(sp, "stun_recover", stunRecover);
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(axeDamage);
        }
    }

    private static float f(JsonObject o, String k, float d) {
        return o.has(k) ? o.get(k).getAsFloat() : d;
    }

    private static int i(JsonObject o, String k, int d) {
        return o.has(k) ? o.get(k).getAsInt() : d;
    }

    @Override
    protected ResourceLocation rigId() {
        return RIG;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new BrainGoal());
    }

    // ───────────── 竞技场 ─────────────

    @Override
    public String arenaId() {
        return arenaId;
    }

    @Override
    public void bindArena(String id) {
        arenaId = id;
    }

    @Override
    public void onLeashed() {
        endMove();
        charge().cancel();
    }

    @Override
    public void onPhaseTwo() {
        phaseTwoPending = true;
    }

    @Override
    public boolean phaseTwoReady() {
        return devicesReady;
    }

    @Override
    public float damageTakenMultiplier() {
        return stun > 0 ? MechanicWindows.DAMAGE_MULTIPLIER : 1.0F;
    }

    public Optional<TrialArena> arena() {
        if (!(level() instanceof ServerLevel sl) || arenaId.isEmpty()) {
            return Optional.empty();
        }
        return TrialArenas.get(sl.getServer()).byId(arenaId);
    }

    public Move currentMove() {
        return move;
    }

    public int stunTicks() {
        return stun;
    }

    public int roarTicks() {
        return roar;
    }

    public int segment() {
        return segment;
    }

    // ───────────── 免疫与持久 ─────────────

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypeTags.IS_FALL) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Arena", arenaId);
        tag.putBoolean("DevicesReady", devicesReady);
        tag.putBoolean("PhaseTwoPending", phaseTwoPending);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        arenaId = tag.getString("Arena");
        devicesReady = tag.getBoolean("DevicesReady");
        phaseTwoPending = tag.getBoolean("PhaseTwoPending");
        setVariant("boss");
    }

    // ───────────── 目标 ─────────────

    private Optional<ServerPlayer> currentTarget(ServerLevel level, TrialArena a) {
        List<ServerPlayer> inHall = a.playersInHall(level);
        if (target != null) {
            for (ServerPlayer p : inHall) {
                if (p.getUUID().equals(target)) {
                    return Optional.of(p);
                }
            }
        }
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : inHall) {
            double d = distanceToSqr(p);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        target = best == null ? null : best.getUUID();
        return Optional.ofNullable(best);
    }

    private boolean lineOfSight(Entity e) {
        Vec3 from = new Vec3(getX(), getY() + 1.0, getZ());
        Vec3 to = new Vec3(e.getX(), e.getY() + 1.0, e.getZ());
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    private float yawTo(Entity e) {
        return yawTo(e.getX(), e.getZ());
    }

    private float yawTo(double x, double z) {
        return (float) (Mth.atan2(z - getZ(), x - getX()) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private void face(float yaw) {
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    private void stand() {
        getNavigation().stop();
        Vec3 v = getDeltaMovement();
        setDeltaMovement(0, v.y, 0);
    }

    private double horizontalDistSqr(Entity e) {
        double dx = e.getX() - getX();
        double dz = e.getZ() - getZ();
        return dx * dx + dz * dz;
    }

    // ───────────── 大脑 ─────────────

    private void tickBrain() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Optional<TrialArena> arena = arena();
        if (arena.isEmpty() || !arena.get().phase().fighting() || !arena.get().bossId().map(getUUID()::equals).orElse(false)) {
            // 过期的 Boss（竞技场已复原、换了一场，或区块卸载期间被复原后实体才载入）：自行消失，不留孤儿
            if (tickCount > 20) {
                io.github.happysnappingturtle.mythicage.MythicAge.LOGGER.info("过期的 Boss {}（竞技场 {}）自行消失", getUUID(), arenaId);
                discard();
            }
            return;
        }
        if (stun > 0) {
            stand();
            if (--stun == 0) {
                playOnce(MythAnimState.CHARGE_RECOVER, stunRecover);
            }
            return;
        }
        if (roar > 0) {
            stand();
            if (--roar == 0) {
                devicesReady = true;
            }
            return;
        }
        long now = level.getGameTime();
        Optional<ServerPlayer> t = currentTarget(level, arena.get());
        if (move != Move.NONE && move != Move.CHASE) {
            tickMove(level, t.orElse(null));
            return;
        }
        if (phaseTwoPending) {
            // 阶段切换放在招式之间（卡片 §7「先完成安全中断点」）
            phaseTwoPending = false;
            roar = roarTicks;
            stand();
            playOnce(MythAnimState.ROAR, roarTicks);
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.7F);
            return;
        }
        if (pausedForTest || pauseAllForTest) {
            return;
        }
        if (t.isEmpty()) {
            move = Move.NONE;
            if (position().distanceTo(arena.get().bossOrigin().getBottomCenter()) > 3) {
                getNavigation().moveTo(arena.get().bossOrigin().getX() + 0.5, arena.get().bossOrigin().getY(),
                        arena.get().bossOrigin().getZ() + 0.5, 0.8);
            }
            return;
        }
        ServerPlayer p = t.get();
        double d = Math.sqrt(horizontalDistSqr(p));
        Move next = MoveScheduler.choose(new MoveScheduler.Situation(arena.get().phase() == ArenaPhase.FIGHT_P2, d,
                lineOfSight(p), false, now >= axeReadyAt, now >= chargeReadyAt, now >= throwReadyAt));
        switch (next) {
            case AXE -> startAxe(p);
            case CHARGE -> startCharge(p, now, false);
            case DOUBLE_CHARGE -> startCharge(p, now, true);
            case THROW -> startThrow(p, now);
            case CHASE -> {
                move = Move.CHASE;
                if (tickCount % 10 == 0 || getNavigation().isDone()) {
                    getNavigation().moveTo(p, 1.0);
                }
                getLookControl().setLookAt(p);
            }
            case NONE -> {
            }
        }
    }

    private void startAxe(ServerPlayer p) {
        move = Move.AXE;
        moveTick = 0;
        hitThisMove.clear();
        stand();
        face(yawTo(p));
        playOnce(MythAnimState.ATTACK_SWING, axeTotal);
        axeReadyAt = level().getGameTime() + MoveScheduler.AXE_COOLDOWN;
    }

    private void startCharge(ServerPlayer p, long now, boolean twice) {
        move = twice ? Move.DOUBLE_CHARGE : Move.CHARGE;
        moveTick = 0;
        segment = 1;
        secondSegmentAt = -1;
        hitThisMove.clear();
        face(yawTo(p));
        startCharge(Math.min(MoveScheduler.CHARGE_MAX, Math.sqrt(horizontalDistSqr(p)) + 2));
        snort();
        chargeReadyAt = now + (twice ? MoveScheduler.DOUBLE_CHARGE_COOLDOWN : MoveScheduler.CHARGE_COOLDOWN);
    }

    private void snort() {
        if (level() instanceof ServerLevel sl) {
            Vec3 f = Vec3.directionFromRotation(0, getYRot());
            sl.sendParticles(ParticleTypes.CLOUD, getX() + f.x * 1.2, getY() + getBbHeight() * 0.8, getZ() + f.z * 1.2,
                    4, 0.1, 0.05, 0.1, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.2F, 0.6F);
        }
    }

    private void startThrow(ServerPlayer p, long now) {
        move = Move.THROW;
        moveTick = 0;
        hitThisMove.clear();
        stand();
        face(yawTo(p));
        throwAt = p.position();
        playOnce(MythAnimState.THROW, throwTotal);
        throwReadyAt = now + MoveScheduler.THROW_COOLDOWN;
    }

    private void tickMove(ServerLevel level, ServerPlayer t) {
        moveTick++;
        switch (move) {
            case AXE -> {
                if (t != null && moveTick < axeLockAt) {
                    face(yawTo(t));
                }
                if (moveTick >= axeWindup && moveTick < axeActiveEnd) {
                    axeHits(level);
                }
                if (moveTick >= axeTotal) {
                    endMove();
                }
            }
            case CHARGE, DOUBLE_CHARGE -> {
                if (t != null && charge().phase() == ChargeTimeline.Phase.WINDUP && charge().canTurn()) {
                    chargeYaw = yawTo(t);
                    face(chargeYaw);
                }
                if (!charge().active()) {
                    if (secondSegmentAt >= 0 && moveTick >= secondSegmentAt && t != null) {
                        // 连冲第二段：重新预警、重新锁向（卡片 §6）
                        secondSegmentAt = -1;
                        segment = 2;
                        hitThisMove.clear();
                        face(yawTo(t));
                        startCharge(Math.min(MoveScheduler.CHARGE_MAX, Math.sqrt(horizontalDistSqr(t)) + 2));
                        snort();
                    } else if (secondSegmentAt < 0) {
                        endMove();
                    }
                }
            }
            case THROW -> {
                if (t != null && moveTick < throwWindup - 6) {
                    face(yawTo(t));
                    throwAt = t.position();
                }
                if (moveTick >= throwWindup - 6 && moveTick < throwWindup && throwAt != null) {
                    level.sendParticles(ParticleTypes.CRIT, throwAt.x, throwAt.y + 0.1, throwAt.z, 6, 0.6, 0.0, 0.6, 0);
                }
                if (moveTick == throwWindup && throwAt != null) {
                    throwRock(level);
                }
                if (moveTick >= throwTotal) {
                    endMove();
                }
            }
            default -> endMove();
        }
    }

    private void throwRock(ServerLevel level) {
        Vec3 f = Vec3.directionFromRotation(0, getYRot());
        ThrownRubble rock = new ThrownRubble(level, this, throwDamage);
        rock.setPos(getX() - f.z * 0.6, getY() + getBbHeight() * 0.85, getZ() + f.x * 0.6);
        double dx = throwAt.x - rock.getX();
        double dz = throwAt.z - rock.getZ();
        double dy = throwAt.y + 1.0 - rock.getY();
        double dist = Math.sqrt(dx * dx + dz * dz);
        rock.shoot(dx, dy + dist * 0.18, dz, 1.3F, 0);
        level.addFreshEntity(rock);
        level.playSound(null, blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.5F, 0.4F);
    }

    private void endMove() {
        move = Move.NONE;
        moveTick = 0;
        segment = 0;
        secondSegmentAt = -1;
        hitThisMove.clear();
    }

    /** 斧劈有效刻：前方扇形、半径内、视线通的玩家各受一次伤害。 */
    private void axeHits(ServerLevel level) {
        Optional<TrialArena> a = arena();
        if (a.isEmpty()) {
            return;
        }
        double reach = MoveScheduler.AXE_RANGE + getBbWidth() / 2;
        Vec3 facing = Vec3.directionFromRotation(0, getYRot());
        for (ServerPlayer p : a.get().playersInHall(level)) {
            if (hitThisMove.contains(p.getUUID()) || horizontalDistSqr(p) > reach * reach) {
                continue;
            }
            Vec3 to = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
            if (to.lengthSqr() > 1e-6) {
                double cos = facing.dot(to.normalize());
                if (cos < Math.cos(Math.toRadians(AXE_HALF_ANGLE))) {
                    continue;
                }
            }
            if (!lineOfSight(p)) {
                continue;
            }
            hitThisMove.add(p.getUUID());
            reportHit(Move.AXE, p);
            p.hurt(damageSources().mobAttack(this), axeDamage);
        }
    }

    // ───────────── 冲锋：扫掠与撞击 ─────────────

    @Override
    protected void onChargeRunTick() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        arena().ifPresent(a -> {
            for (ServerPlayer p : a.playersInHall(level)) {
                if (!hitThisMove.contains(p.getUUID()) && getBoundingBox().inflate(SWEEP_INFLATE).intersects(p.getBoundingBox())) {
                    hitThisMove.add(p.getUUID());
                    reportHit(Move.CHARGE, p);
                    p.hurt(damageSources().mobAttack(this), chargeDamage);
                    Vec3 push = Vec3.directionFromRotation(0, chargeYaw).scale(0.8);
                    p.push(push.x, 0.3, push.z);
                    p.hurtMarked = true;
                }
            }
        });
    }

    /** 冲刺方向前方 {@value #PROBE} 格、身体宽度与高度内的实心格。 */
    private List<BlockPos> probe() {
        List<BlockPos> out = new ArrayList<>();
        Vec3 f = Vec3.directionFromRotation(0, chargeYaw);
        Vec3 side = new Vec3(-f.z, 0, f.x);
        double half = getBbWidth() / 2;
        Vec3 front = position().add(f.scale(half + PROBE));
        for (double s : new double[]{-half + 0.1, 0, half - 0.1}) {
            for (int dy = 0; dy < 3; dy++) {
                BlockPos p = BlockPos.containing(front.x + side.x * s, getY() + 0.5 + dy, front.z + side.z * s);
                var shape = level().getBlockState(p).getCollisionShape(level(), p);
                // 矮于半格的东西（碎块、台阶）跨得过去，不算挡路
                if (!out.contains(p) && !shape.isEmpty() && shape.max(Direction.Axis.Y) > 0.5) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    @Override
    protected boolean chargeBlocked() {
        blocking.clear();
        blocking.addAll(probe());
        return horizontalCollision || !blocking.isEmpty();
    }

    @Override
    protected void onChargeBlocked() {
        secondSegmentAt = -1;           // 撞上就取消连冲的第二段
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Optional<TrialArena> arena = arena();
        if (arena.isEmpty()) {
            return;
        }
        TrialArena a = arena.get();
        TrialArena.Impact impact = a.classify(level, blocking.isEmpty() ? probe() : blocking);
        MechanicWindows.Outcome out = MechanicWindows.Outcome.NONE;
        switch (impact.kind()) {
            case INTACT_PILLAR -> {
                out = a.windows().onImpact(MechanicWindows.Impact.INTACT_PILLAR, level.getGameTime(), a.allPillarsBroken());
                a.breakPillar(level, impact.pillar(), Direction.fromYRot(chargeYaw).getOpposite());
            }
            case WALL -> out = a.windows().onImpact(MechanicWindows.Impact.WALL, level.getGameTime(), a.allPillarsBroken());
            case NONE -> {
            }
        }
        if (out.stunned()) {
            charge().cancel();
            stun = out.stunTicks();
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight(), getZ(), 12, 0.4, 0.2, 0.4, 0.1);
            level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5F, 0.5F);
        }
    }

    @Override
    protected void onChargeFinished() {
        if (stun > 0) {
            playOnce(MythAnimState.STUN, stun);   // 硬直：头角卡在柱里挣扎
            endMove();
            return;
        }
        if (move == Move.DOUBLE_CHARGE && segment == 1 && blocking.isEmpty() && !horizontalCollision) {
            secondSegmentAt = moveTick + doubleGap;
        }
    }

    // ───────────── 死亡 ─────────────

    /** 死亡动画（跪倒伏地）的长度：播完再消失，不用原版的侧倒。 */
    public static final int DEATH_TICKS = 40;

    @Override
    public void die(DamageSource source) {
        super.die(source);
        charge().cancel();
        endMove();
        stun = 0;
        roar = 0;
        if (!level().isClientSide()) {
            setAnimState(MythAnimState.DEATH);
        }
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        Vec3 v = getDeltaMovement();
        setDeltaMovement(0, v.y, 0);
        if (deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    // ───────────── 测试 ─────────────

    private static void reportHit(Move m, Entity e) {
        var h = onHitForTest;
        if (h != null) {
            h.accept(m, e.getUUID());
        }
    }

    static void reportThrowHit(Entity e) {
        reportHit(Move.THROW, e);
    }

    public void pauseForTest(boolean paused) {
        pausedForTest = paused;
    }

    /** 只供 GameTest：立即出一招。 */
    public void forceMoveForTest(Move m, LivingEntity t) {
        if (t instanceof ServerPlayer p) {
            target = p.getUUID();
            long now = level().getGameTime();
            switch (m) {
                case AXE -> startAxe(p);
                case CHARGE -> startCharge(p, now, false);
                case DOUBLE_CHARGE -> startCharge(p, now, true);
                case THROW -> startThrow(p, now);
                default -> {
                }
            }
        }
    }

    /** 只供 GameTest：指定目标。 */
    public void targetForTest(Player p) {
        target = p.getUUID();
    }

    private final class BrainGoal extends Goal {
        BrainGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            tickBrain();
        }
    }
}
