package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import io.github.happysnappingturtle.mythicage.boss.TrialArena;
import io.github.happysnappingturtle.mythicage.boss.TrialArenas;
import io.github.happysnappingturtle.mythicage.boss.TrialBoss;
import io.github.happysnappingturtle.mythicage.core.anim.ChargeTimeline;
import io.github.happysnappingturtle.mythicage.core.boss.MoveScheduler;
import io.github.happysnappingturtle.mythicage.core.boss.MoveScheduler.Move;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.entity.MinotaurEntity;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 弥诺陶洛斯 · 克诺索斯迷宫之主（T23 §6）：试炼 Boss 版。共用 T7 的模型与动画（变体 boss：放大 1.4、换贴图），数值与行为单独配置，
 * 不听传令杖、不占召唤槽（卡片 MIN-01）。只在自己的竞技场大厅里活动，只打大厅里的合格玩家。
 * <p>
 * 招式由 {@link MoveScheduler} 选（同一时刻只有一招）；伤害全在服务端按有效刻判定，一次攻击每个目标只结算一次（MIN-02），
 * 锁向后不再转身（MIN-03）。阶段 ① 只有斧劈、冲锋与追击；撞柱硬直、阶段二的连冲与掷石在阶段 ②。
 */
public class MinotaurBossEntity extends MinotaurEntity implements TrialBoss {
    public static final ResourceLocation RIG = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, "minotaur");
    public static final float AXE_DAMAGE = 12;
    public static final float CHARGE_DAMAGE = 16;
    public static final int AXE_WINDUP = 16;
    public static final int AXE_LOCK_AT = 10;
    public static final int AXE_ACTIVE_END = 20;
    public static final int AXE_TOTAL = 36;
    /** 斧劈扇形：前方 90°。 */
    public static final double AXE_HALF_ANGLE = 45;
    /** 冲锋扫掠：身体包围盒外扩。 */
    public static final double SWEEP_INFLATE = 0.3;

    private String arenaId = "";
    private Move move = Move.NONE;
    private int moveTick;
    private long axeReadyAt;
    private long chargeReadyAt;
    private long throwReadyAt;
    private final Set<UUID> hitThisMove = new HashSet<>();
    private UUID target;
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
        }
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, AXE_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
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

    public Optional<TrialArena> arena() {
        if (!(level() instanceof ServerLevel sl) || arenaId.isEmpty()) {
            return Optional.empty();
        }
        return TrialArenas.get(sl.getServer()).byId(arenaId);
    }

    public Move currentMove() {
        return move;
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        arenaId = tag.getString("Arena");
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
        return (float) (Mth.atan2(e.getZ() - getZ(), e.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private void face(float yaw) {
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    // ───────────── 招式 ─────────────

    private void tickBrain() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Optional<TrialArena> arena = arena();
        if (arena.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Optional<ServerPlayer> t = currentTarget(level, arena.get());
        if (move != Move.NONE && move != Move.CHASE) {
            tickMove(level, t.orElse(null));
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
        Move next = MoveScheduler.choose(new MoveScheduler.Situation(arena.get().phase()
                == io.github.happysnappingturtle.mythicage.core.boss.ArenaPhase.FIGHT_P2, d, lineOfSight(p), false,
                now >= axeReadyAt, now >= chargeReadyAt, now >= throwReadyAt));
        switch (next) {
            case AXE -> startAxe(p);
            case CHARGE, DOUBLE_CHARGE -> startCharge(p, now);
            case THROW, CHASE -> {
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

    private double horizontalDistSqr(Entity e) {
        double dx = e.getX() - getX();
        double dz = e.getZ() - getZ();
        return dx * dx + dz * dz;
    }

    private void startAxe(ServerPlayer p) {
        move = Move.AXE;
        moveTick = 0;
        hitThisMove.clear();
        getNavigation().stop();
        face(yawTo(p));
        playOnce(MythAnimState.ATTACK_SWING, AXE_TOTAL);
        axeReadyAt = level().getGameTime() + MoveScheduler.AXE_COOLDOWN;
    }

    private void startCharge(ServerPlayer p, long now) {
        move = Move.CHARGE;
        moveTick = 0;
        hitThisMove.clear();
        face(yawTo(p));
        startCharge(Math.min(MoveScheduler.CHARGE_MAX, Math.sqrt(horizontalDistSqr(p)) + 2));
        chargeReadyAt = now + MoveScheduler.CHARGE_COOLDOWN;
    }

    private void tickMove(ServerLevel level, ServerPlayer t) {
        moveTick++;
        switch (move) {
            case AXE -> {
                if (t != null && moveTick < AXE_LOCK_AT) {
                    face(yawTo(t));
                }
                if (moveTick >= AXE_WINDUP && moveTick < AXE_ACTIVE_END) {
                    axeHits(level);
                }
                if (moveTick >= AXE_TOTAL) {
                    endMove();
                }
            }
            case CHARGE, DOUBLE_CHARGE -> {
                if (t != null && charge().phase() == ChargeTimeline.Phase.WINDUP && charge().canTurn()) {
                    chargeYaw = yawTo(t);
                    face(chargeYaw);
                }
                if (!charge().active()) {
                    endMove();
                }
            }
            default -> endMove();
        }
    }

    private void endMove() {
        move = Move.NONE;
        moveTick = 0;
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
            p.hurt(damageSources().mobAttack(this), AXE_DAMAGE);
        }
    }

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
                    p.hurt(damageSources().mobAttack(this), CHARGE_DAMAGE);
                    Vec3 push = Vec3.directionFromRotation(0, chargeYaw).scale(0.8);
                    p.push(push.x, 0.3, push.z);
                    p.hurtMarked = true;
                }
            }
        });
    }

    private static void reportHit(Move m, Entity e) {
        var h = onHitForTest;
        if (h != null) {
            h.accept(m, e.getUUID());
        }
    }

    public void pauseForTest(boolean paused) {
        pausedForTest = paused;
    }

    /** 只供 GameTest：立即出一招。 */
    public void forceMoveForTest(Move m, LivingEntity t) {
        if (t instanceof ServerPlayer p) {
            target = p.getUUID();
            if (m == Move.AXE) {
                startAxe(p);
            } else if (m == Move.CHARGE) {
                startCharge(p, level().getGameTime());
            }
        }
    }

    /** 只供 GameTest：指定目标（假玩家不会被「最近」选中时）。 */
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
