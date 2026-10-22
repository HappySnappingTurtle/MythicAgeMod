package io.github.happysnappingturtle.mythicage.entity.stymphalian;

import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
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
import java.util.UUID;

/**
 * 斯廷法利斯怪鸟（01_希腊 W3、T15 设计文档 §3、制作卡 07）：群居的敌对飞鸟，只随沼泽鸟巢生成。
 * <ul>
 *   <li>白天在鸟巢上空盘旋；20 格内有玩家（或被打）就飞到目标上空 6–10 格绕圈。</li>
 *   <li>齐射：领头鸟发令，每只抖羽 16 刻后射 1 根慢速青铜羽（可盾挡）；一轮总数 ≤ 群内只数；两轮间隔 4 秒。</li>
 *   <li>惊散：24 格内敲钟或摇青铜响板 → 飞到高空聚集 4 秒，正在抖羽的取消，这 4 秒不齐射。</li>
 *   <li>领头鸟被射落：其余放下目标各自回巢（D4），2 分钟后才会再攻击；群里没有领头鸟时，UUID 最小的一只接任。</li>
 *   <li>夜里回巢栖息，不主动攻击。</li>
 * </ul>
 * 飞行不走寻路：每刻朝目标点转向（同天马），只有落在巢上时受重力。
 */
public class StymphalianBirdEntity extends Monster implements GeoEntity, MythVariantHolder {
    public static final float LEADER_SCALE = 1.15F;
    public static final int RATTLE_TICKS = 16;
    public static final int STARTLE_TICKS = 80;
    public static final double STARTLE_RADIUS = 24;
    public static final int ROUT_TICKS = 2400;
    public static final double FEATHER_SPEED = 0.6;

    public enum State { PERCH, FLY, RATTLE, SHOOT, STARTLE }

    private static final EntityDataAccessor<Boolean> LEADER = SynchedEntityData.defineId(StymphalianBirdEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(StymphalianBirdEntity.class, EntityDataSerializers.INT);

    /** 巢上的落脚点：中心与周围 8 格（按群内 UUID 排序取）。 */
    private static final int[][] PERCH_SLOTS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ROOST = RawAnimation.begin().thenLoop("roost");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation RATTLE = RawAnimation.begin().thenLoop("rattle");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlayAndHold("shoot");
    private static final RawAnimation STARTLE = RawAnimation.begin().thenLoop("startle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID groupId;
    @Nullable
    private BlockPos nest;
    private long startledUntil;
    private double startleY;
    private long routedUntil;
    private long rattleEnd;
    private long shootUntil;
    private long volleyReadyAt;
    private boolean perched;

    private double featherDamage = 3;
    private int volleyCooldown = 80;
    private double aggroRange = 20;
    private double flySpeed = 0.45;

    public StymphalianBirdEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEADER, false);
        builder.define(STATE, State.FLY.ordinal());
    }

    @Override
    protected void registerGoals() {
        // 不用目标选择器与寻路：飞行与攻击都在 customServerAiStep 里按状态算
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SummonUnits.Stats s = SummonUnits.statsFile("greek", "wild_stymphalian_bird");
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", 10));
        setHealth(getMaxHealth());
        flySpeed = s.attr("flying_speed", 0.45);
        featherDamage = s.specialDouble("feather_damage", 3);
        volleyCooldown = s.specialInt("volley_cooldown", 80);
        aggroRange = s.specialDouble("aggro_range", 20);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** 入群：群 id、鸟巢（落脚处，可为空）、是否领头鸟。随结构生成的鸟不会因玩家走远而消失。 */
    public void joinFlock(UUID group, @Nullable BlockPos nest, boolean leader) {
        this.groupId = group;
        this.nest = nest == null ? null : nest.immutable();
        setLeader(leader);
        setPersistenceRequired();
    }

    // ───────────── 状态查询（GameTest 与渲染用） ─────────────

    public boolean leader() {
        return entityData.get(LEADER);
    }

    private void setLeader(boolean leader) {
        entityData.set(LEADER, leader);
        refreshDimensions();
    }

    public State state() {
        int i = entityData.get(STATE);
        return i >= 0 && i < State.values().length ? State.values()[i] : State.FLY;
    }

    @Nullable
    public UUID groupId() {
        return groupId;
    }

    @Nullable
    public BlockPos nest() {
        return nest;
    }

    public boolean perched() {
        return perched;
    }

    public boolean startled() {
        return level().getGameTime() < startledUntil;
    }

    public boolean routed() {
        return level().getGameTime() < routedUntil;
    }

    public boolean rattling() {
        return rattleEnd != 0;
    }

    /** 夜晚（按一天中的时刻算，不看天空亮度）：13000–23000 刻。 */
    public static boolean night(Level level) {
        long t = level.getDayTime() % 24000L;
        return t >= 13000 && t < 23000;
    }

    @Override
    public String variant() {
        return "";
    }

    @Override
    public float variantScale() {
        return leader() ? LEADER_SCALE : 1.0F;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(variantScale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LEADER.equals(key)) {
            refreshDimensions();
        }
    }

    // ───────────── 群 ─────────────

    /** 同群（含自己）、48 格内活着的怪鸟。 */
    public List<StymphalianBirdEntity> flock() {
        if (groupId == null) {
            return List.of(this);
        }
        return level().getEntitiesOfClass(StymphalianBirdEntity.class, getBoundingBox().inflate(48),
                e -> e.isAlive() && groupId.equals(e.groupId));
    }

    private int rank() {
        List<StymphalianBirdEntity> fs = new ArrayList<>(flock());
        fs.sort(Comparator.comparing(Entity::getUUID));
        return Math.max(0, fs.indexOf(this));
    }

    /**
     * 领头鸟发令齐射：群里每只（不在受惊、没在抖羽）都以 target 为目标开始抖羽，16 刻后各射 1 根。
     * 返回参与的只数（≤ 群内只数）；受惊、冷却中或自己不是领头鸟时返回 0。
     */
    public int startVolley(LivingEntity target) {
        long now = level().getGameTime();
        if (!leader() || startled() || now < volleyReadyAt) {
            return 0;
        }
        int n = 0;
        for (StymphalianBirdEntity b : flock()) {
            if (!b.startled() && !b.rattling() && !b.routed()) {
                b.beginRattle(target, now + RATTLE_TICKS);
                n++;
            }
        }
        volleyReadyAt = now + RATTLE_TICKS + volleyCooldown;
        return n;
    }

    private void beginRattle(LivingEntity target, long end) {
        setTarget(target);
        rattleEnd = end;
        if (perched) {
            takeOff();
        }
        playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0F, 0.7F + random.nextFloat() * 0.2F);
    }

    /** 惊散：半径内所有怪鸟飞到高空聚集 4 秒（钟、青铜响板调用）。返回受惊的只数。 */
    public static int startleAround(ServerLevel level, Vec3 at, double radius) {
        int n = 0;
        for (StymphalianBirdEntity b : level.getEntitiesOfClass(StymphalianBirdEntity.class, new AABB(at, at).inflate(radius),
                e -> e.isAlive() && e.position().distanceToSqr(at) <= radius * radius)) {
            b.startle();
            n++;
        }
        return n;
    }

    public void startle() {
        startledUntil = level().getGameTime() + STARTLE_TICKS;
        rattleEnd = 0;
        double base = nest != null ? nest.getY() : getY();
        startleY = Math.max(getY(), base) + 14;
        if (perched) {
            takeOff();
        }
        playSound(SoundEvents.PHANTOM_FLAP, 1.2F, 1.4F);
    }

    /** 领头鸟被射落：其余放下目标、取消抖羽，回巢 2 分钟。 */
    private void rout() {
        routedUntil = level().getGameTime() + ROUT_TICKS;
        rattleEnd = 0;
        setTarget(null);
    }

    // ───────────── 伤害与死亡 ─────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && !isDeadOrDying() && !routed() && source.getEntity() instanceof LivingEntity who
                && !(who instanceof StymphalianBirdEntity)) {
            for (StymphalianBirdEntity b : flock()) {
                if (!b.routed()) {
                    b.setTarget(who);
                }
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (leader() && !level().isClientSide) {
            for (StymphalianBirdEntity b : flock()) {
                if (b != this) {
                    b.rout();
                }
            }
        }
        super.die(source);
    }

    /** 怪鸟之间不互相推挤：巢上落脚点只隔 1 格，挤来挤去会把还没落下的鸟推离落脚点。 */
    @Override
    protected void doPush(Entity other) {
        if (!(other instanceof StymphalianBirdEntity)) {
            super.doPush(other);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return groupId == null && super.removeWhenFarAway(distance);
    }

    // ───────────── 飞行 ─────────────

    private void takeOff() {
        perched = false;
        setNoGravity(true);
        setDeltaMovement(getDeltaMovement().x, 0.35, getDeltaMovement().z);
    }

    private void perch() {
        perched = true;
        setNoGravity(false);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void travel(Vec3 input) {
        if (perched || !isNoGravity()) {
            super.travel(input);
            return;
        }
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.91));
        calculateEntityAnimation(false);
    }

    /** 朝目标点转向：限速，靠近时减速；撞墙就往上抬；身体朝向跟着水平速度。 */
    private void steer(Vec3 dest, double speed) {
        Vec3 d = dest.subtract(position());
        double dist = d.length();
        Vec3 want = dist < 0.05 ? Vec3.ZERO : d.scale(Math.min(speed, dist * 0.25) / dist);
        Vec3 v = getDeltaMovement().lerp(want, 0.2);
        if (horizontalCollision) {
            v = v.add(0, 0.15, 0);
        }
        setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 0.002) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 12.0F));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    private Vec3 home() {
        return nest != null ? Vec3.atBottomCenterOf(nest) : position();
    }

    private Vec3 perchSlot() {
        int[] s = PERCH_SLOTS[rank() % PERCH_SLOTS.length];
        return Vec3.atBottomCenterOf(nest).add(s[0], 0.05, s[1]);
    }

    private double phase() {
        return (getUUID().getLeastSignificantBits() & 0xffff) / 65536.0 * Math.PI * 2;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long now = level().getGameTime();
        boolean night = night(level());
        boolean startled = startled();
        boolean routed = routed();
        boolean goHome = night || routed;

        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || goHome || t instanceof Player p && (p.isCreative() || p.isSpectator())
                || distanceToSqr(t) > 40 * 40)) {
            setTarget(null);
            t = null;
        }
        if (t == null && !goHome && !startled && tickCount % 10 == 0) {
            t = level().getNearestPlayer(TargetingConditions.forCombat().range(aggroRange).ignoreLineOfSight(), this);
            if (t != null) {
                setTarget(t);
            }
        }
        if (groupId != null && !leader() && !routed && tickCount % 100 == 0) {
            List<StymphalianBirdEntity> fs = flock();
            if (fs.stream().noneMatch(StymphalianBirdEntity::leader)
                    && fs.stream().map(Entity::getUUID).min(Comparator.naturalOrder()).orElse(getUUID()).equals(getUUID())) {
                setLeader(true);
            }
        }

        // 抖羽 → 射羽
        if (rattleEnd != 0) {
            if (startled || t == null) {
                rattleEnd = 0;
            } else if (now >= rattleEnd) {
                shootFeather(t);
                rattleEnd = 0;
                shootUntil = now + 6;
            } else if ((rattleEnd - now) % 4 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WAX_OFF, getX(), getY() + 0.9 * variantScale(), getZ(), 4, 0.8, 0.2, 0.8, 0.0);
            }
        }
        if (leader() && t != null && !startled && !rattling() && now >= volleyReadyAt && distanceToSqr(t) < 20 * 20) {
            startVolley(t);
        }

        // 移动
        if (perched) {
            if (!goHome || nest == null) {
                takeOff();
            } else {
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            }
        }
        if (!perched) {
            Vec3 dest;
            double speed = flySpeed;
            if (startled) {
                Vec3 c = home();
                double a = phase();
                dest = new Vec3(c.x + Math.cos(a) * 3, startleY, c.z + Math.sin(a) * 3);
                speed = flySpeed * 1.5;
            } else if (goHome && nest != null) {
                dest = perchSlot();
                if (position().distanceToSqr(dest) < 0.6 * 0.6) {
                    perch();
                    dest = null;
                }
            } else if (rattling()) {
                dest = position();
            } else if (t != null) {
                double a = phase() + tickCount * 0.03;
                double r = 5;
                double h = 7 + rank() % 3;
                dest = t.position().add(Math.cos(a) * r, h, Math.sin(a) * r);
            } else {
                double a = phase() + tickCount * 0.02;
                dest = home().add(Math.cos(a) * 8, 10 + (rank() % 3), Math.sin(a) * 8);
            }
            if (dest != null) {
                steer(dest, speed);
            }
        }
        if (t != null && (rattling() || now < shootUntil)) {
            getLookControl().setLookAt(t);
        }

        State s = perched ? State.PERCH : startled ? State.STARTLE : rattling() ? State.RATTLE : now < shootUntil ? State.SHOOT : State.FLY;
        if (state() != s) {
            entityData.set(STATE, s.ordinal());
        }
    }

    private void shootFeather(LivingEntity t) {
        BronzeFeatherEntity f = new BronzeFeatherEntity(level(), this, featherDamage);
        Vec3 from = position().add(0, 0.5 * variantScale(), 0);
        f.setPos(from.x, from.y, from.z);
        Vec3 aim = new Vec3(t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ()).subtract(from);
        f.shoot(aim.x, aim.y, aim.z, (float) FEATHER_SPEED, 1.0F);
        level().addFreshEntity(f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 0.8F, 1.5F);
    }

    // ───────────── 存档 ─────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (groupId != null) {
            tag.putUUID("Flock", groupId);
        }
        if (nest != null) {
            tag.put("Nest", NbtUtils.writeBlockPos(nest));
        }
        tag.putBoolean("Leader", leader());
        tag.putBoolean("Perched", perched);
        tag.putLong("RoutedUntil", routedUntil);
        tag.putDouble("FeatherDamage", featherDamage);
        tag.putInt("VolleyCooldown", volleyCooldown);
        tag.putDouble("AggroRange", aggroRange);
        tag.putDouble("FlySpeed", flySpeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        groupId = tag.hasUUID("Flock") ? tag.getUUID("Flock") : null;
        nest = NbtUtils.readBlockPos(tag, "Nest").orElse(null);
        setLeader(tag.getBoolean("Leader"));
        perched = tag.getBoolean("Perched");
        setNoGravity(!perched);
        routedUntil = tag.getLong("RoutedUntil");
        if (tag.contains("FeatherDamage")) {
            featherDamage = tag.getDouble("FeatherDamage");
            volleyCooldown = tag.getInt("VolleyCooldown");
            aggroRange = tag.getDouble("AggroRange");
            flySpeed = tag.getDouble("FlySpeed");
        }
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> switch (state()) {
            case PERCH -> state.setAndContinue(night(level()) ? ROOST : IDLE);
            case RATTLE -> state.setAndContinue(RATTLE);
            case SHOOT -> state.setAndContinue(SHOOT);
            case STARTLE -> state.setAndContinue(STARTLE);
            case FLY -> state.setAndContinue(getY() - yo < -0.04 ? GLIDE : FLY);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
