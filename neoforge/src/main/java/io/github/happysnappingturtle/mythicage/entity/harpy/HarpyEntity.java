package io.github.happysnappingturtle.mythicage.entity.harpy;

import io.github.happysnappingturtle.mythicage.core.wild.GrabRules;
import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
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
 * 哈耳庇厄（01_希腊 W2、T15 设计文档 §4、制作卡 08）：群居的敌对飞行生物，随崖巢生成。
 * <ul>
 *   <li>24 格内发现玩家（创造 / 旁观不算）就在其上空 8 格绕圈；没有玩家时捕食 16 格内的鸡、兔。</li>
 *   <li>俯冲：冷却好了 → 尖啸收翅 16 刻（预警，悬停不动）→ 俯冲；同群同一目标同一时间只有一只俯冲。</li>
 *   <li>接触目标按 {@link GrabRules} 判定：举盾挡住 → 坠地眩晕 40 刻；免疫期 / 崖巢 24 格内 / 悬崖边 / 体型大 → 爪击 + 击退；
 *       否则抓起（目标骑在她脚下），升高 ≤ 3 格，20 刻后松手，目标 8 秒内免疫抓举。</li>
 *   <li>俯冲途中被打中 → 坠地眩晕 40 刻。雷雨天俯冲冷却减半（风暴精灵）。</li>
 *   <li>没有目标时回巢栖息。</li>
 * </ul>
 * 飞行不走寻路：每刻朝目标点转向（同斯廷法利斯怪鸟），只有栖息、眩晕时受重力。
 */
public class HarpyEntity extends Monster implements GeoEntity, MythVariantHolder {
    public static final int SCREECH_TICKS = 16;
    public static final int STUN_TICKS = 40;
    public static final int SWIPE_TICKS = 10;
    public static final int DIVE_MAX_TICKS = 60;
    public static final double DIVE_SPEED = 0.9;
    public static final String IMMUNE_KEY = "mythic_age.harpy_immune_until";

    public enum State { PERCH, FLY, SCREECH, DIVE, CARRY, SWIPE, STUNNED }

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(HarpyEntity.class, EntityDataSerializers.INT);

    private static final int[][] PERCH_SLOTS = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation SCREECH = RawAnimation.begin().thenPlayAndHold("screech");
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop("dive");
    private static final RawAnimation CARRY = RawAnimation.begin().thenLoop("carry");
    private static final RawAnimation SWIPE = RawAnimation.begin().thenPlayAndHold("swipe");
    private static final RawAnimation STUNNED = RawAnimation.begin().thenLoop("stunned");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID groupId;
    @Nullable
    private BlockPos nest;
    /** 没有巢时的盘旋中心：第一次 AI 刻记下的位置（命令召唤、刷怪笼生成的那种）。 */
    @Nullable
    private Vec3 anchor;
    private long stateEnd;
    private long nextAttackAt;
    private double carryTopY;

    private double clawDamage = 4;
    private int attackCooldown = 120;
    private double aggroRange = 24;
    private double flySpeed = 0.5;

    public HarpyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, State.FLY.ordinal());
    }

    @Override
    protected void registerGoals() {
        // 不用目标选择器与寻路：飞行与攻击都在 customServerAiStep 里按状态算
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SummonUnits.Stats s = SummonUnits.statsFile("greek", "wild_harpy");
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", 20));
        setHealth(getMaxHealth());
        flySpeed = s.attr("flying_speed", 0.5);
        clawDamage = s.specialDouble("claw_damage", 4);
        attackCooldown = s.specialInt("attack_cooldown", 120);
        aggroRange = s.specialDouble("aggro_range", 24);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** 入群：群 id、崖巢（可为空）。随结构生成的不会因玩家走远而消失。 */
    public void joinFlock(UUID group, @Nullable BlockPos nest) {
        this.groupId = group;
        this.nest = nest == null ? null : nest.immutable();
        setPersistenceRequired();
    }

    // ───────────── 状态查询（GameTest 与渲染用） ─────────────

    public State state() {
        int i = entityData.get(STATE);
        return i >= 0 && i < State.values().length ? State.values()[i] : State.FLY;
    }

    private void setState(State s, long end) {
        entityData.set(STATE, s.ordinal());
        stateEnd = end;
        boolean grounded = s == State.PERCH || s == State.STUNNED;
        setNoGravity(!grounded);
        if (!grounded && onGround()) {
            setDeltaMovement(getDeltaMovement().x, 0.3, getDeltaMovement().z);
        }
    }

    @Nullable
    public UUID groupId() {
        return groupId;
    }

    @Nullable
    public BlockPos nest() {
        return nest;
    }

    @Override
    public String variant() {
        return "";
    }

    @Override
    public float variantScale() {
        return 1.0F;
    }

    /** 目标的抓举免疫截止刻（存在目标的持久数据里，任何一只哈耳庇厄都认）。 */
    public static long immuneUntil(Entity target) {
        return target.getPersistentData().getLong(IMMUNE_KEY);
    }

    // ───────────── 群 ─────────────

    public List<HarpyEntity> flock() {
        if (groupId == null) {
            return List.of(this);
        }
        return level().getEntitiesOfClass(HarpyEntity.class, getBoundingBox().inflate(48),
                e -> e.isAlive() && groupId.equals(e.groupId));
    }

    private int rank() {
        List<HarpyEntity> fs = new ArrayList<>(flock());
        fs.sort(Comparator.comparing(Entity::getUUID));
        return Math.max(0, fs.indexOf(this));
    }

    /** 同群里有别的哈耳庇厄正在对这个目标尖啸 / 俯冲 / 抓着它。 */
    private boolean flockmateAttacking(LivingEntity t) {
        for (HarpyEntity h : flock()) {
            if (h != this && h.getTarget() == t && (h.state() == State.SCREECH || h.state() == State.DIVE || h.state() == State.CARRY)) {
                return true;
            }
        }
        return false;
    }

    /** 立刻对 t 开始尖啸预警（跳过绕圈与冷却；GameTest、开发命令用）。 */
    public void beginAttack(LivingEntity t) {
        setTarget(t);
        startScreech(level().getGameTime());
    }

    private void startScreech(long now) {
        setState(State.SCREECH, now + SCREECH_TICKS);
        playSound(SoundEvents.GOAT_SCREAMING_PREPARE_RAM, 1.6F, 1.5F + random.nextFloat() * 0.2F);
    }

    // ───────────── 伤害 ─────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof HarpyEntity) {
            return false;
        }
        boolean wasDiving = state() == State.DIVE;
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && !isDeadOrDying()) {
            if (wasDiving) {
                stun();
            }
            if (source.getEntity() instanceof LivingEntity who && getTarget() == null) {
                setTarget(who);
            }
        }
        return hurt;
    }

    /** 坠地眩晕：放开抓着的目标，取消俯冲，受重力掉下去，40 刻后再飞。 */
    public void stun() {
        ejectPassengers();
        setState(State.STUNNED, level().getGameTime() + STUN_TICKS);
        setDeltaMovement(getDeltaMovement().multiply(0.2, 0, 0.2).add(0, -0.4, 0));
        nextAttackAt = level().getGameTime() + STUN_TICKS + attackCooldown / 2;
        playSound(SoundEvents.PHANTOM_HURT, 1.0F, 1.4F);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return groupId == null && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected void doPush(Entity other) {
        if (!(other instanceof HarpyEntity)) {
            super.doPush(other);
        }
    }

    // ───────────── 抓举：目标挂在脚下 ─────────────

    /**
     * 目标挂在脚下：头顶离她的脚 0.05 格。positionRider 会再减去目标自己的骑乘挂点（尸壳 −1.3 之类），这里先加回去。
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return new Vec3(0, -passenger.getBbHeight() - 0.05 + passenger.getVehicleAttachmentPoint(this).y, 0);
    }

    /** 松手时目标从挂着的位置直接掉下去（原版会把下马的放到坐骑头顶）。 */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return passenger.position();
    }

    /** 被抓的目标（哪怕是怪物）不能操控她。 */
    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    /** 俯冲接触目标。 */
    private void contact(LivingEntity t, long now) {
        boolean prey = t instanceof Chicken || t instanceof Rabbit;
        boolean shield = t.isBlocking() && t.isDamageSourceBlocked(damageSources().mobAttack(this));
        double nestSq = nest == null ? Double.POSITIVE_INFINITY : t.blockPosition().distSqr(nest);
        GrabRules.Outcome o = prey ? GrabRules.Outcome.SWIPE
                : GrabRules.contact(shield, now, immuneUntil(t), nestSq, maxDropNear(level(), t.blockPosition()), t.getBbWidth(), t.getBbHeight());
        if (o == GrabRules.Outcome.FAIL_STUN) {
            playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.9F);
            stun();
            return;
        }
        if (o == GrabRules.Outcome.GRAB && !t.isPassenger()) {
            // 先把自己挪到「目标挂在脚下时脚还在原处」的高度，再抓；否则目标会被按进地里
            double hangY = t.getY() + t.getBbHeight() + 0.05;
            setPos(getX(), Math.max(getY(), hangY), getZ());
            setDeltaMovement(Vec3.ZERO); // 俯冲的余速会把两者一起再往下带一刻
            if (!t.startRiding(this, true)) {
                o = GrabRules.Outcome.SWIPE;
            }
        }
        if (o == GrabRules.Outcome.GRAB) {
            t.getPersistentData().putLong(IMMUNE_KEY, now + GrabRules.IMMUNE_TICKS);
            carryTopY = getY() + GrabRules.MAX_LIFT;
            setState(State.CARRY, now + GrabRules.HOLD_TICKS);
            playSound(SoundEvents.PHANTOM_BITE, 1.0F, 1.3F);
            return;
        }
        // 爪击 + 击退，然后拉起
        t.hurt(damageSources().mobAttack(this), (float) clawDamage);
        Vec3 away = t.position().subtract(position()).multiply(1, 0, 1);
        if (away.lengthSqr() > 1.0E-4) {
            t.knockback(0.8, -away.x, -away.z);
        }
        setState(State.SWIPE, now + SWIPE_TICKS);
        nextAttackAt = now + attackCooldown();
        playSound(SoundEvents.PHANTOM_BITE, 1.0F, 1.1F);
    }

    /** 松手：目标记上 3 格已下落距离（再摔 3 格 ≈ 1.5 心）。 */
    private void release(long now) {
        for (Entity p : new ArrayList<>(getPassengers())) {
            p.stopRiding();
            p.fallDistance = GrabRules.RELEASE_FALL;
        }
        setState(State.FLY, 0);
        nextAttackAt = now + attackCooldown();
        playSound(SoundEvents.PHANTOM_FLAP, 1.0F, 1.2F);
    }

    private int attackCooldown() {
        return level().isThundering() ? attackCooldown / 2 : attackCooldown;
    }

    /**
     * pos（目标脚下那格）附近 {@link GrabRules#CLIFF_RANGE} 格内的最大落差：在中心与 8 个方向 2、4、6 格处，
     * 从 pos 的高度往下找第一个挡路的方块，最多找 8 格（找不到就当 8 格）。
     */
    public static int maxDropNear(Level level, BlockPos pos) {
        int max = 0;
        int[][] dirs = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : dirs) {
            for (int r = d[0] == 0 && d[1] == 0 ? 0 : 2; r <= GrabRules.CLIFF_RANGE; r += 2) {
                BlockPos col = pos.offset(d[0] * r, 0, d[1] * r);
                int drop = 0;
                while (drop < 8 && !level.getBlockState(col.below(drop + 1)).blocksMotion()) {
                    drop++;
                }
                // 这一列本身比脚下高（墙、坡）：不算落差
                if (!level.getBlockState(col).blocksMotion()) {
                    max = Math.max(max, drop);
                }
                if (d[0] == 0 && d[1] == 0) {
                    break;
                }
            }
        }
        return max;
    }

    // ───────────── 飞行 ─────────────

    @Override
    public void travel(Vec3 input) {
        if (!isNoGravity()) {
            super.travel(input);
            return;
        }
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.91));
        calculateEntityAnimation(false);
    }

    private void steer(Vec3 dest, double speed) {
        Vec3 d = dest.subtract(position());
        double dist = d.length();
        Vec3 want = dist < 0.05 ? Vec3.ZERO : d.scale(Math.min(speed, dist * 0.25) / dist);
        Vec3 v = getDeltaMovement().lerp(want, 0.25);
        if (horizontalCollision) {
            v = v.add(0, 0.15, 0);
        }
        setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 0.002) {
            face((float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F);
        }
    }

    private void face(float yaw) {
        setYRot(Mth.approachDegrees(getYRot(), yaw, 15.0F));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void faceTarget(LivingEntity t) {
        Vec3 d = t.position().subtract(position());
        face((float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F);
    }

    private Vec3 home() {
        if (nest != null) {
            return Vec3.atBottomCenterOf(nest);
        }
        if (anchor == null) {
            anchor = position();
        }
        return anchor;
    }

    /** 巢上的落脚处：按群内排序取，跳过被挡住或脚下悬空的格子（崖边常有），都不行就落在巢中心。 */
    private Vec3 perchSlot() {
        int start = rank();
        for (int k = 0; k < PERCH_SLOTS.length; k++) {
            int[] s = PERCH_SLOTS[(start + k) % PERCH_SLOTS.length];
            BlockPos p = nest.offset(s[0], 0, s[1]);
            if (!level().getBlockState(p).blocksMotion() && !level().getBlockState(p.above()).blocksMotion()
                    && level().getBlockState(p.below()).blocksMotion()) {
                return Vec3.atBottomCenterOf(p).add(0, 0.05, 0);
            }
        }
        return Vec3.atBottomCenterOf(nest).add(0, 0.05, 0);
    }

    private double phase() {
        return (getUUID().getLeastSignificantBits() & 0xffff) / 65536.0 * Math.PI * 2;
    }

    @Nullable
    private LivingEntity findTarget() {
        Player p = level().getNearestPlayer(TargetingConditions.forCombat().range(aggroRange).ignoreLineOfSight(), this);
        if (p != null) {
            return p;
        }
        List<LivingEntity> prey = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
                e -> e.isAlive() && (e instanceof Chicken || e instanceof Rabbit));
        return prey.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private boolean validTarget(LivingEntity t) {
        return t.isAlive() && !(t instanceof Player p && (p.isCreative() || p.isSpectator())) && distanceToSqr(t) < 40 * 40;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long now = level().getGameTime();
        State s = state();
        LivingEntity t = getTarget();
        if (t != null && !validTarget(t)) {
            setTarget(null);
            t = null;
        }
        if (t == null && tickCount % 10 == 0 && s != State.STUNNED && s != State.CARRY) {
            t = findTarget();
            if (t != null) {
                setTarget(t);
            }
        }

        switch (s) {
            case STUNNED -> {
                if (now >= stateEnd) {
                    setState(State.FLY, 0);
                }
            }
            case CARRY -> {
                if (getPassengers().isEmpty() || now >= stateEnd) {
                    release(now);
                } else {
                    steer(new Vec3(getX(), carryTopY, getZ()), 0.25);
                }
            }
            case SCREECH -> {
                setDeltaMovement(getDeltaMovement().scale(0.6));
                if (t == null) {
                    setState(State.FLY, 0);
                } else {
                    faceTarget(t);
                    if (now >= stateEnd) {
                        setState(State.DIVE, now + DIVE_MAX_TICKS);
                    }
                }
            }
            case DIVE -> {
                if (t == null || now >= stateEnd || onGround()) {
                    setState(State.FLY, 0);
                    nextAttackAt = now + attackCooldown();
                } else {
                    Vec3 aim = t.position().add(0, t.getBbHeight() * 0.5, 0);
                    Vec3 d = aim.subtract(position().add(0, getBbHeight() * 0.3, 0));
                    setDeltaMovement(d.normalize().scale(Math.min(DIVE_SPEED, d.length())));
                    faceTarget(t);
                    if (getBoundingBox().inflate(0.4).intersects(t.getBoundingBox())) {
                        contact(t, now);
                    }
                }
            }
            case SWIPE -> {
                steer(position().add(0, 3, 0), flySpeed);
                if (now >= stateEnd) {
                    setState(State.FLY, 0);
                }
            }
            case PERCH -> {
                if (t != null || nest == null) {
                    setState(State.FLY, 0);
                } else {
                    setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                }
            }
            case FLY -> {
                if (t != null) {
                    double a = phase() + tickCount * 0.04;
                    steer(t.position().add(Math.cos(a) * 6, 8, Math.sin(a) * 6), flySpeed);
                    if (now >= nextAttackAt && hasLineOfSight(t) && !flockmateAttacking(t)) {
                        startScreech(now);
                    }
                } else if (nest != null) {
                    Vec3 slot = perchSlot();
                    steer(slot, flySpeed);
                    if (position().distanceToSqr(slot) < 0.6 * 0.6) {
                        setDeltaMovement(Vec3.ZERO);
                        setState(State.PERCH, 0);
                    }
                } else {
                    double a = phase() + tickCount * 0.02;
                    steer(home().add(Math.cos(a) * 6, 6, Math.sin(a) * 6), flySpeed);
                }
            }
        }
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
        tag.putBoolean("Perched", state() == State.PERCH);
        tag.putDouble("ClawDamage", clawDamage);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putDouble("AggroRange", aggroRange);
        tag.putDouble("FlySpeed", flySpeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        groupId = tag.hasUUID("Flock") ? tag.getUUID("Flock") : null;
        nest = NbtUtils.readBlockPos(tag, "Nest").orElse(null);
        setState(tag.getBoolean("Perched") ? State.PERCH : State.FLY, 0);
        if (tag.contains("ClawDamage")) {
            clawDamage = tag.getDouble("ClawDamage");
            attackCooldown = tag.getInt("AttackCooldown");
            aggroRange = tag.getDouble("AggroRange");
            flySpeed = tag.getDouble("FlySpeed");
        }
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> switch (state()) {
            case PERCH -> state.setAndContinue(IDLE);
            case SCREECH -> state.setAndContinue(SCREECH);
            case DIVE -> state.setAndContinue(DIVE);
            case CARRY -> state.setAndContinue(CARRY);
            case SWIPE -> state.setAndContinue(SWIPE);
            case STUNNED -> state.setAndContinue(STUNNED);
            case FLY -> state.setAndContinue(getY() - yo < -0.05 ? GLIDE : FLY);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
