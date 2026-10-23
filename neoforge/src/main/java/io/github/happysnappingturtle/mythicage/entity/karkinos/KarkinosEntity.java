package io.github.happysnappingturtle.mythicage.entity.karkinos;

import io.github.happysnappingturtle.mythicage.core.wild.WildCombatRules;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;

/**
 * 卡尔基诺斯（01_希腊 W4、T15 设计文档 §5、制作卡 09）：独居的领地型巨蟹。
 * <ul>
 *   <li>领地 = 出生点周围 8 格：玩家（创造 / 旁观不算）或鸡、鱼进入就攻击；追击不出 16 格，之后回领地中心。</li>
 *   <li>面朝目标横着靠近，转身慢（每刻 6°），所以能绕到它侧后；张钳 14 刻后夹击 9 点（目标要在前方 120° 内、3.2 格内）。</li>
 *   <li>正面铁壳：伤害来源在前方 120° 内 −80%（{@link WildCombatRules#frontalMultiplier}）。</li>
 *   <li>掀翻：玩家满蓄力攻击时暴击、疾跑或用斧头（{@link WildCombatRules#flips}）→ 翻倒 60 刻，受伤 ×2、不攻击。</li>
 *   <li>白天没有目标时回领地中心埋进泥沙，只露眼柄；被踩到或受伤就钻出来。</li>
 * </ul>
 */
public class KarkinosEntity extends Monster implements GeoEntity, MythVariantHolder {
    public static final int WINDUP_TICKS = 14;
    public static final int PINCH_TICKS = 8;
    public static final int EMERGE_TICKS = 10;
    public static final float TURN_PER_TICK = 6.0F;
    public static final double FRONT_ARC = 120.0;
    public static final double FRONT_REDUCTION = 0.8;

    public enum State { IDLE, WINDUP, PINCH, FLIPPED, BURIED, EMERGE }

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(KarkinosEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("pinch_windup");
    private static final RawAnimation PINCH = RawAnimation.begin().thenPlayAndHold("pinch");
    private static final RawAnimation FLIPPED = RawAnimation.begin().thenLoop("flipped");
    private static final RawAnimation BURIED = RawAnimation.begin().thenLoop("buried");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlayAndHold("emerge");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private BlockPos home;
    private long stateEnd;
    private long nextPinchAt;
    /** 掀翻预约：玩家的满蓄力暴击 / 疾跑 / 斧头攻击在 AttackEntityEvent 里记下，紧接着的这次伤害生效时翻倒。 */
    @Nullable
    private UUID flipBy;
    private long flipArmedAt = -10;

    private double clawDamage = 9;
    private double territory = 8;
    private double chase = 16;
    private int pinchCooldown = 30;
    private int flipTicks = 60;

    public KarkinosEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        moveControl = new SideStepControl(this);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, State.IDLE.ordinal());
    }

    @Override
    protected void registerGoals() {
        // 不用目标选择器：领地、追击、夹击都在 customServerAiStep 里按状态算，移动用寻路 + 横移的 MoveControl
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SummonUnits.Stats s = SummonUnits.statsFile("greek", "wild_karkinos");
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", 60));
        setHealth(getMaxHealth());
        clawDamage = s.specialDouble("claw_damage", 9);
        territory = s.specialDouble("territory", 8);
        chase = s.specialDouble("chase", 16);
        pinchCooldown = s.specialInt("pinch_cooldown", 30);
        flipTicks = s.specialInt("flip_ticks", 60);
        home = blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ───────────── 状态 ─────────────

    public State state() {
        int i = entityData.get(STATE);
        return i >= 0 && i < State.values().length ? State.values()[i] : State.IDLE;
    }

    private void setState(State s, long end) {
        State old = state();
        entityData.set(STATE, s.ordinal());
        stateEnd = end;
        if ((old == State.BURIED) != (s == State.BURIED)) {
            refreshDimensions();
        }
    }

    public BlockPos home() {
        if (home == null) {
            home = blockPosition();
        }
        return home;
    }

    public void setHome(BlockPos pos) {
        home = pos.immutable();
    }

    public boolean flipped() {
        return state() == State.FLIPPED;
    }

    public boolean buried() {
        return state() == State.BURIED;
    }

    public static boolean day(Level level) {
        long t = level.getDayTime() % 24000L;
        return t < 12500 || t >= 23500;
    }

    @Override
    public String variant() {
        return "";
    }

    @Override
    public float variantScale() {
        return 1.0F;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions d = super.getDefaultDimensions(pose);
        return buried() ? EntityDimensions.scalable(d.width(), 0.3F) : d;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            refreshDimensions();
        }
    }

    // ───────────── 伤害：正面铁壳、翻倒 ×2、掀翻 ─────────────

    /** AttackEntityEvent 里调用：这名玩家的这一下满足掀翻条件。 */
    public void armFlip(Player by) {
        flipBy = by.getUUID();
        flipArmedAt = level().getGameTime();
    }

    /** 这次伤害的倍率：翻倒 ×2；否则来源在前方 120° 内 ×0.2；没有来源位置（摔落、中毒等）×1。 */
    public double damageMultiplier(DamageSource source) {
        if (flipped()) {
            return WildCombatRules.FLIPPED_MULTIPLIER;
        }
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return 1.0;
        }
        return WildCombatRules.frontalMultiplier(yBodyRot, from.x - getX(), from.z - getZ(), FRONT_ARC, FRONT_REDUCTION);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }
        boolean wasBuried = buried();
        double mult = damageMultiplier(source);
        if (mult < 1.0) {
            playSound(SoundEvents.SHIELD_BLOCK, 0.8F, 0.6F);
        }
        boolean hurt = super.hurt(source, (float) (amount * mult));
        if (!hurt || isDeadOrDying()) {
            return hurt;
        }
        long now = level().getGameTime();
        if (source.getEntity() instanceof Player p && p.getUUID().equals(flipBy) && now - flipArmedAt <= 1 && !flipped()) {
            flipBy = null;
            setState(State.FLIPPED, now + flipTicks);
            getNavigation().stop();
            playSound(SoundEvents.TURTLE_EGG_CRACK, 1.0F, 0.6F);
        }
        if (wasBuried && !flipped()) {
            setState(State.EMERGE, now + EMERGE_TICKS);
        }
        if (source.getEntity() instanceof LivingEntity who && !(who instanceof KarkinosEntity) && getTarget() == null) {
            setTarget(who);
        }
        return hurt;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    /** 蟹在水下不会淹死（1.21 的 canBreatheUnderwater 是 final，用 NeoForge 的流体钩子）。 */
    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    // ───────────── AI ─────────────

    private boolean validTarget(LivingEntity t) {
        return t.isAlive() && !(t instanceof Player p && (p.isCreative() || p.isSpectator()))
                && t.position().distanceToSqr(Vec3.atBottomCenterOf(home())) <= chase * chase;
    }

    @Nullable
    private LivingEntity findTarget() {
        Vec3 c = Vec3.atBottomCenterOf(home());
        double r2 = territory * territory;
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(territory + 8),
                e -> e.isAlive() && e != this && (e instanceof Player || e instanceof Chicken || e instanceof AbstractFish))) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
                continue;
            }
            if (e.position().distanceToSqr(c) > r2) {
                continue;
            }
            double d = distanceToSqr(e) - (e instanceof Player ? 1000 : 0);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** 目标是否在身体前方 arc 度内。 */
    private boolean inFront(Entity t, double arc) {
        return WildCombatRules.frontalMultiplier(yBodyRot, t.getX() - getX(), t.getZ() - getZ(), arc, 1.0) == 0.0;
    }

    private void turnToward(Vec3 p) {
        float yaw = (float) (Mth.atan2(p.z - getZ(), p.x - getX()) * Mth.RAD_TO_DEG) - 90.0F;
        float y = Mth.approachDegrees(yBodyRot, yaw, TURN_PER_TICK);
        setYRot(y);
        yBodyRot = y;
        yHeadRot = y;
    }

    private boolean steppedOn() {
        return !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.1, 0.6, 0.1),
                e -> e != this && !(e instanceof KarkinosEntity) && e.onGround()).isEmpty();
    }

    private boolean buryableGround() {
        BlockState g = level().getBlockState(blockPosition().below());
        return g.is(BlockTags.SAND) || g.is(Blocks.MUD) || g.is(Blocks.CLAY) || g.is(Blocks.GRAVEL) || g.is(BlockTags.DIRT);
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

        switch (s) {
            case FLIPPED -> {
                getNavigation().stop();
                if (now >= stateEnd) {
                    setState(State.IDLE, 0);
                    nextPinchAt = now + pinchCooldown;
                }
                return;
            }
            case BURIED -> {
                getNavigation().stop();
                if (!day(level()) || steppedOn()) {
                    setState(State.EMERGE, now + EMERGE_TICKS);
                    if (getTarget() == null) {
                        LivingEntity on = level().getNearestPlayer(this, 2.0);
                        if (on != null && validTarget(on)) {
                            setTarget(on);
                        }
                    }
                }
                return;
            }
            case EMERGE -> {
                getNavigation().stop();
                if (now >= stateEnd) {
                    setState(State.IDLE, 0);
                    nextPinchAt = now + 6;
                }
                return;
            }
            case WINDUP -> {
                getNavigation().stop();
                if (t != null) {
                    turnToward(t.position());
                }
                if (now >= stateEnd) {
                    setState(State.PINCH, now + PINCH_TICKS);
                    if (t != null && distanceToSqr(t) <= 3.2 * 3.2 && inFront(t, FRONT_ARC)) {
                        t.hurt(damageSources().mobAttack(this), (float) clawDamage);
                        playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.6F);
                    } else {
                        playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 0.7F);
                    }
                }
                return;
            }
            case PINCH -> {
                if (now >= stateEnd) {
                    setState(State.IDLE, 0);
                    nextPinchAt = now + pinchCooldown;
                }
                return;
            }
            case IDLE -> {
                // 往下走
            }
        }

        if (t == null && tickCount % 10 == 0) {
            t = findTarget();
            if (t != null) {
                setTarget(t);
            }
        }
        Vec3 c = Vec3.atBottomCenterOf(home());
        if (t != null) {
            turnToward(t.position());
            double d2 = distanceToSqr(t);
            if (d2 > 2.4 * 2.4) {
                getNavigation().moveTo(t, 1.0);
            } else {
                getNavigation().stop();
                if (now >= nextPinchAt && inFront(t, 60)) {
                    setState(State.WINDUP, now + WINDUP_TICKS);
                    playSound(SoundEvents.ARMOR_EQUIP_TURTLE.value(), 1.0F, 0.6F);
                }
            }
            return;
        }
        double fromHome = position().distanceToSqr(c);
        if (fromHome > 2.0 * 2.0) {
            if (getNavigation().isDone()) {
                getNavigation().moveTo(c.x, c.y, c.z, 0.9);
            }
        } else if (day(level()) && buryableGround()) {
            getNavigation().stop();
            setState(State.BURIED, 0);
        } else if (!day(level()) && getNavigation().isDone() && random.nextInt(80) == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * territory * 0.7;
            getNavigation().moveTo(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 0.7);
        }
    }

    /**
     * 横着走：按寻路给的目标点移动，但不把身体转向移动方向——有目标时身体朝目标（customServerAiStep 里慢慢转），
     * 没目标时身体与移动方向垂直（侧行）。
     */
    static final class SideStepControl extends MoveControl {
        private final KarkinosEntity crab;

        SideStepControl(KarkinosEntity crab) {
            super(crab);
            this.crab = crab;
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                mob.setZza(0);
                mob.setXxa(0);
                return;
            }
            operation = Operation.WAIT;
            double dx = wantedX - mob.getX();
            double dz = wantedZ - mob.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.05) {
                mob.setZza(0);
                mob.setXxa(0);
                return;
            }
            if (crab.getTarget() == null) {
                float moveYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
                float side = Mth.wrapDegrees(moveYaw - crab.yBodyRot) > 0 ? 90.0F : -90.0F;
                float y = Mth.approachDegrees(crab.yBodyRot, moveYaw - side, TURN_PER_TICK * 2);
                crab.setYRot(y);
                crab.yBodyRot = y;
                crab.yHeadRot = y;
            }
            float yaw = crab.getYRot() * Mth.DEG_TO_RAD;
            // 世界方向 → 本地（zza 向前、xxa 向左）
            double fx = -Mth.sin(yaw);
            double fz = Mth.cos(yaw);
            double forward = (dx * fx + dz * fz) / dist;
            double left = (dx * fz - dz * fx) / dist;
            mob.setSpeed((float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            mob.setZza((float) forward);
            mob.setXxa((float) left);
        }
    }

    // ───────────── 存档 ─────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Home", NbtUtils.writeBlockPos(home()));
        tag.putBoolean("Buried", buried());
        tag.putDouble("ClawDamage", clawDamage);
        tag.putDouble("Territory", territory);
        tag.putDouble("Chase", chase);
        tag.putInt("PinchCooldown", pinchCooldown);
        tag.putInt("FlipTicks", flipTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        setState(tag.getBoolean("Buried") ? State.BURIED : State.IDLE, 0);
        if (tag.contains("ClawDamage")) {
            clawDamage = tag.getDouble("ClawDamage");
            territory = tag.getDouble("Territory");
            chase = tag.getDouble("Chase");
            pinchCooldown = tag.getInt("PinchCooldown");
            flipTicks = tag.getInt("FlipTicks");
        }
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> switch (state()) {
            case WINDUP -> state.setAndContinue(WINDUP);
            case PINCH -> state.setAndContinue(PINCH);
            case FLIPPED -> state.setAndContinue(FLIPPED);
            case BURIED -> state.setAndContinue(BURIED);
            case EMERGE -> state.setAndContinue(EMERGE);
            case IDLE -> state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** 测试与开发命令用：所在服务端世界。 */
    public ServerLevel serverLevel() {
        return (ServerLevel) level();
    }
}
