package io.github.happysnappingturtle.mythicage.content.greek.power;

import io.github.happysnappingturtle.mythicage.core.power.SentinelRules;
import io.github.happysnappingturtle.mythicage.core.power.TargetClassifier;
import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.power.OwnedSummon;
import io.github.happysnappingturtle.mythicage.power.PowerProduct;
import io.github.happysnappingturtle.mythicage.power.PowerRegistries;
import io.github.happysnappingturtle.mythicage.power.PowerSpecLoader;
import io.github.happysnappingturtle.mythicage.power.TargetFactsCollector;
import io.github.happysnappingturtle.mythicage.power.TransientEntities;
import io.github.happysnappingturtle.mythicage.runtime.WorldClock;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 哨兵石像（GP-GR-03；T13 设计文档 §4.2）：不可移动的固定构造物，40 生命、8 护甲，免疫击退、推不动。
 * 每 1.5 秒一轮：选 16 格内最近的敌方（按主人的目标矩阵；主人离线时只打实现 {@link Enemy} 的怪），
 * 转身 → 拉弓 12 刻 → 射出 4 点、不可捡的箭 → 复位；**视线上有友方就不射**。
 * 到期（或被新的一组替换）时碎化 20 刻后移除；载入时已过期的不加入世界（{@link SentinelPower#onEntityJoin}）。
 * 实现 {@link OwnedSummon}（临时）：复原能给它回血。
 */
public class SentinelEntity extends PathfinderMob implements OwnedSummon, PowerProduct, GeoEntity, MythVariantHolder {
    /** 同步给客户端的动画状态。 */
    public enum Pose { RISE, IDLE, DRAW, SHOOT, CRUMBLE }

    private static final EntityDataAccessor<Byte> POSE = SynchedEntityData.defineId(SentinelEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> POSE_START = SynchedEntityData.defineId(SentinelEntity.class,
            EntityDataSerializers.INT);
    private static final int SHOOT_POSE_TICKS = 8;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int clientSeenPoseStart = -1;

    @Nullable
    private UUID owner;
    private long expiresAt = Long.MAX_VALUE;
    private int poseTicks;
    private int cooldown;
    /** 碎化开始前的等待（逐尊碎化）。 */
    private int retireDelay = -1;
    @Nullable
    private UUID drawTarget;
    /** 射出后还在飞的箭（服务端发拖尾符光；不存档）。 */
    private final List<Arrow> flying = new ArrayList<>();
    /** 客户端表现：上一刻看到的姿态与它持续的刻数。 */
    @Nullable
    private Pose fxPose;
    private int fxAge;

    public SentinelEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.FOLLOW_RANGE, SentinelRules.RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POSE, (byte) Pose.RISE.ordinal());
        builder.define(POSE_START, 0);
    }

    /** 由施放设定主人与到期刻。 */
    public void setup(UUID owner, long expiresAt) {
        this.owner = owner;
        this.expiresAt = expiresAt;
        setPoseState(Pose.RISE, SentinelRules.RISE_TICKS);
    }

    public long expiresAt() {
        return expiresAt;
    }

    public Pose poseState() {
        byte b = entityData.get(POSE);
        return b >= 0 && b < Pose.values().length ? Pose.values()[b] : Pose.IDLE;
    }

    protected void setPoseState(Pose p, int ticks) {
        entityData.set(POSE, (byte) p.ordinal());
        entityData.set(POSE_START, tickCount);
        poseTicks = ticks;
    }

    @Override
    public UUID summonOwner() {
        return owner;
    }

    @Override
    public boolean permanentSummon() {
        return false;
    }

    // ───────────── 不可移动 ─────────────

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity other) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
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
    protected boolean shouldDropLoot() {
        return false;
    }

    // ───────────── 逐刻 ─────────────

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        trails(server);
        if (poseState() == Pose.CRUMBLE) {
            if (--poseTicks <= 0) {
                discard();
            }
            return;
        }
        if (retireDelay >= 0) {
            if (retireDelay-- == 0) {
                crumble(server);
            }
            return;
        }
        if (WorldClock.now(server.getServer()) >= expiresAt) {
            crumble(server);
            return;
        }
        switch (poseState()) {
            case RISE -> {
                if (--poseTicks <= 0) {
                    setPoseState(Pose.IDLE, 0);
                }
            }
            case DRAW -> {
                LivingEntity t = drawTarget == null ? null : server.getEntity(drawTarget) instanceof LivingEntity l ? l : null;
                if (t != null) {
                    getLookControl().setLookAt(t, 30, 30);
                    setYRot(yHeadRot);
                }
                if (--poseTicks <= 0) {
                    if (t != null && valid(server, t) && clear(server, t)) {
                        shoot(server, t);
                        setPoseState(Pose.SHOOT, SHOOT_POSE_TICKS);
                    } else {
                        setPoseState(Pose.IDLE, 0);
                    }
                    drawTarget = null;
                }
            }
            case SHOOT -> {
                if (--poseTicks <= 0) {
                    setPoseState(Pose.IDLE, 0);
                }
            }
            default -> {
            }
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (poseState() == Pose.IDLE && cooldown <= 0) {
            LivingEntity t = pickTarget(server);
            if (t != null) {
                drawTarget = t.getUUID();
                cooldown = shotInterval();
                setPoseState(Pose.DRAW, SentinelRules.DRAW_TICKS);
                getLookControl().setLookAt(t, 30, 30);
                GreekPowers.play(this, "gp.gr.sentinel.draw");
            }
        }
    }

    // ───────────── 可覆盖的规则（遗迹守卫版 RuinSentinelEntity 改这些） ─────────────

    /** 射程（格）。 */
    protected double range() {
        return SentinelRules.RANGE;
    }

    /** 两箭之间的刻数。 */
    protected int shotInterval() {
        return SentinelRules.SHOT_INTERVAL;
    }

    /** 每箭的伤害。 */
    protected float arrowDamage() {
        return SentinelRules.ARROW_DAMAGE;
    }

    /** 该不该射它（也用来判断射线上的生物要不要避开：不该射的挡在路上就不射）。 */
    protected boolean shouldShoot(ServerLevel server, LivingEntity e) {
        ServerPlayer op = ownerPlayer(server);
        Set<UUID> own = op == null ? Set.of() : TargetFactsCollector.ownVillages(op);
        return hostile(server, e, op, own);
    }

    @Nullable
    private ServerPlayer ownerPlayer(ServerLevel server) {
        return owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
    }

    /** 是不是该射的目标（按主人的目标矩阵；主人离线时只打 Enemy 的怪）。 */
    boolean hostile(ServerLevel server, LivingEntity e, @Nullable ServerPlayer op, Set<UUID> own) {
        if (e == this || !e.isAlive() || e instanceof SentinelEntity s && owner != null && owner.equals(s.owner)) {
            return false;
        }
        if (op == null) {
            return e instanceof Enemy && !(e instanceof Player);
        }
        var spec = PowerSpecLoader.get(GreekPowers.SENTINEL).orElse(null);
        return spec != null && !TargetClassifier.NONE.equals(TargetClassifier.reaction(spec,
                TargetFactsCollector.collect(op, e, own)));
    }

    private boolean valid(ServerLevel server, LivingEntity t) {
        return t.distanceTo(this) <= range() && shouldShoot(server, t);
    }

    @Nullable
    LivingEntity pickTarget(ServerLevel server) {
        double r = range();
        List<LivingEntity> near = server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r),
                e -> e.distanceTo(this) <= r && shouldShoot(server, e));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
        for (LivingEntity e : near) {
            if (clear(server, e)) {
                return e;
            }
        }
        return null;
    }

    /** 射线没被方块挡住，且路上没有不该打的生物（友方、中立……）。 */
    boolean clear(ServerLevel server, LivingEntity target) {
        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition().add(target.position()).scale(0.5);
        if (server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                != HitResult.Type.MISS) {
            return false;
        }
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1),
                e -> e != this && e != target && e.isAlive())) {
            if (e.getBoundingBox().inflate(0.3).clip(from, to).isPresent() && !shouldShoot(server, e)) {
                return false;
            }
        }
        return true;
    }

    private void shoot(ServerLevel server, LivingEntity t) {
        Arrow arrow = new Arrow(server, this, new ItemStack(Items.ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        Vec3 from = getEyePosition().add(getViewVector(1).scale(0.6));
        arrow.setPos(from.x, from.y - 0.1, from.z);
        double dx = t.getX() - arrow.getX();
        double dy = t.getY(0.4) - arrow.getY();
        double dz = t.getZ() - arrow.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float speed = 1.6f;
        arrow.shoot(dx, dy + horizontal * 0.12, dz, speed, 0);
        // 伤害 = ceil(速度 × 基础伤害)：按出手速度换算，命中时恰好是 arrowDamage()
        arrow.setBaseDamage(arrowDamage() / speed);
        server.addFreshEntity(arrow);
        flying.add(arrow);
        GreekPowers.play(this, "gp.gr.sentinel.shoot");
    }

    /** 箭的拖尾：飞行中每刻在箭身前后各发一点暗绿符光，落地、停下或飞满 2 秒为止。 */
    private void trails(ServerLevel server) {
        flying.removeIf(a -> {
            if (a.isRemoved() || a.tickCount > 40 || a.getDeltaMovement().lengthSqr() < 0.04) {
                return true;
            }
            Vec3 v = a.getDeltaMovement();
            for (double f : new double[]{0, 0.5}) {
                server.sendParticles(PowerRegistries.RUNE_MOTE.get(), a.getX() - v.x * f, a.getY() - v.y * f, a.getZ() - v.z * f,
                        1, 0.02, 0.02, 0.02, 0);
            }
            return false;
        });
    }

    // ───────────── 客户端表现（按同步的姿态发粒子，不另发网络消息） ─────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientFx();
        }
    }

    /** 模型坐标（单位 u，16u = 1 格；+X 左、+Z 前）→ 世界坐标，按身体朝向转。 */
    private Vec3 modelPoint(double left, double up, double forward) {
        double yaw = Math.toRadians(yBodyRot);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double lx = Math.cos(yaw);
        double lz = Math.sin(yaw);
        return new Vec3(getX() + (lx * left + fx * forward) / 16, getY() + up / 16, getZ() + (lz * left + fz * forward) / 16);
    }

    private void clientFx() {
        Pose p = poseState();
        if (p != fxPose) {
            fxPose = p;
            fxAge = 0;
            if (p == Pose.SHOOT) {
                // 撒手：弓前一小团符光
                Vec3 m = modelPoint(4.5, 23, 11);
                for (int i = 0; i < 6; i++) {
                    level().addParticle(PowerRegistries.RUNE_MOTE.get(), m.x, m.y, m.z, (random.nextDouble() - 0.5) * 0.08,
                            (random.nextDouble() - 0.5) * 0.08, (random.nextDouble() - 0.5) * 0.08);
                }
            }
        } else {
            fxAge++;
        }
        switch (p) {
            case RISE -> {
                // 破土：脚下一圈碎土
                BlockState ground = level().getBlockState(blockPosition().below());
                if (!ground.isAir()) {
                    BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, ground);
                    for (int i = 0; i < 2; i++) {
                        double a = random.nextDouble() * Math.PI * 2;
                        level().addParticle(dirt, getX() + Math.cos(a) * 0.45, getY() + 0.05, getZ() + Math.sin(a) * 0.45,
                                Math.cos(a) * 0.1, 0.12, Math.sin(a) * 0.1);
                    }
                }
            }
            case DRAW -> {
                // 弓臂微光：拉弓时弓在胸前（左侧、肩高、前伸），沿上下弓臂随机冒符光
                if (fxAge % 2 == 0) {
                    Vec3 b = modelPoint(4.5, 23 + (random.nextDouble() - 0.5) * 14, 9);
                    level().addParticle(PowerRegistries.RUNE_MOTE.get(), b.x, b.y, b.z, 0, 0.005, 0);
                }
            }
            case CRUMBLE -> {
                // 碎化：自上而下掉黑石碎屑与青铜碎片
                double top = getBbHeight() * Math.max(0.1, 1 - fxAge / (double) SentinelRules.CRUMBLE_TICKS);
                BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState());
                for (int i = 0; i < 3; i++) {
                    level().addParticle(chip, getX() + (random.nextDouble() - 0.5) * 0.7, getY() + random.nextDouble() * top,
                            getZ() + (random.nextDouble() - 0.5) * 0.7, 0, -0.05, 0);
                }
                if (fxAge % 3 == 0) {
                    level().addParticle(PowerRegistries.BRONZE_FLAKE.get(), getX() + (random.nextDouble() - 0.5) * 0.6, getY() + top,
                            getZ() + (random.nextDouble() - 0.5) * 0.6, 0, -0.02, 0);
                    level().addParticle(ParticleTypes.ASH, getX(), getY() + top * 0.5, getZ(), 0, 0, 0);
                }
            }
            default -> {
            }
        }
    }

    // ───────────── 退场 ─────────────

    private void crumble(ServerLevel server) {
        if (poseState() == Pose.CRUMBLE) {
            return;
        }
        TransientEntities.untag(this);
        setPoseState(Pose.CRUMBLE, SentinelRules.CRUMBLE_TICKS);
        GreekPowers.play(this, "gp.gr.sentinel.crumble");
    }

    @Override
    public void retire(int delayTicks) {
        TransientEntities.untag(this);
        retireDelay = Math.max(0, delayTicks);
    }

    public boolean retiring() {
        return retireDelay >= 0 || poseState() == Pose.CRUMBLE;
    }

    // ───────────── 存档 ─────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) {
            tag.putUUID("SentinelOwner", owner);
        }
        tag.putLong("Expires", expiresAt);
        tag.putBoolean("Retiring", retiring());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("SentinelOwner") ? tag.getUUID("SentinelOwner") : null;
        expiresAt = tag.contains("Expires") ? tag.getLong("Expires") : Long.MAX_VALUE;
        if (tag.getBoolean("Retiring")) {
            retireDelay = 0;
        }
        if (poseState() == Pose.RISE) {
            setPoseState(Pose.IDLE, 0);
        }
    }

    // ───────────── 动画（客户端） ─────────────

    @Override
    public String variant() {
        return "";
    }

    @Override
    public float variantScale() {
        return 1.0F;
    }

    @Override
    public boolean headLookLocked() {
        return poseState() == Pose.RISE || poseState() == Pose.CRUMBLE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> {
            Pose p = poseState();
            int start = entityData.get(POSE_START);
            if (start != clientSeenPoseStart) {
                clientSeenPoseStart = start;
                state.getController().forceAnimationReset();
            }
            return state.setAndContinue(switch (p) {
                case RISE -> RawAnimation.begin().thenPlayAndHold("rise");
                case DRAW -> RawAnimation.begin().thenPlayAndHold("draw");
                case SHOOT -> RawAnimation.begin().thenPlay("shoot").thenLoop("idle");
                case CRUMBLE -> RawAnimation.begin().thenPlayAndHold("crumble");
                case IDLE -> RawAnimation.begin().thenLoop("idle");
            });
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
