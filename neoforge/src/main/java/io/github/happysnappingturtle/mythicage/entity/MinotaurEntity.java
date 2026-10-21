package io.github.happysnappingturtle.mythicage.entity;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.anim.ChargeTimeline;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import io.github.happysnappingturtle.mythicage.rig.RigRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * 弥诺陶洛斯（T7 样板外壳，设计文档 §1、§6）：可以生成、站立、慢走；冲锋只由开发命令驱动，只做直线位移、不造成伤害。
 * 召唤版（T16）与 Boss 版（T23）在此基础上加行为。
 */
public class MinotaurEntity extends PathfinderMob implements GeoEntity, MythVariantHolder {
    /** 冲刺速度：卡片 §6 冲锋 20 刻至多 16 格。 */
    public static final double CHARGE_BLOCKS_PER_TICK = 0.8;
    /** 客户端判定「在走路」的水平速度阈值（格/刻）。 */
    public static final double WALK_THRESHOLD = 0.02;
    /** 行走动画每循环前进的距离（格）：大腿摆幅 ±24°、腿长 18u，两步约 1.9 格。 */
    public static final double WALK_CYCLE_BLOCKS = 1.9;

    private static final EntityDataAccessor<Byte> ANIM_STATE = SynchedEntityData.defineId(MinotaurEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANIM_START = SynchedEntityData.defineId(MinotaurEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> VARIANT = SynchedEntityData.defineId(MinotaurEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> VARIANT_SCALE = SynchedEntityData.defineId(MinotaurEntity.class, EntityDataSerializers.FLOAT);

    private static final Map<MythAnimState, RawAnimation> ANIMATIONS = new EnumMap<>(MythAnimState.class);

    static {
        for (MythAnimState s : MythAnimState.values()) {
            if (s.animation() == null) {
                continue;
            }
            RawAnimation raw = switch (s) {
                case CHARGE_WINDUP, CHARGE_RECOVER, PAW, ATTACK_SWING, TOSS -> RawAnimation.begin().thenPlayAndHold(s.animation());
                default -> RawAnimation.begin().thenLoop(s.animation());
            };
            ANIMATIONS.put(s, raw);
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** 一次性动画（挥斧、顶飞）播完后由服务端回到 AUTO 的刻数；0 = 不计时。 */
    private int animUntil;
    private final ChargeTimeline charge = new ChargeTimeline();
    private float chargeYaw;
    private int clientSeenAnimStart = -1;

    public MinotaurEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM_STATE, (byte) MythAnimState.AUTO.ordinal());
        builder.define(ANIM_START, 0);
        builder.define(VARIANT, "");
        builder.define(VARIANT_SCALE, 1.0F);
    }

    @Override
    protected void registerGoals() {
        // 冲锋期间占住移动、视线与跳跃，其他目标不运行
        goalSelector.addGoal(0, new ChargeLockGoal());
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ───────────── 变体 ─────────────

    @Override
    public String variant() {
        return entityData.get(VARIANT);
    }

    @Override
    public float variantScale() {
        return entityData.get(VARIANT_SCALE);
    }

    @Override
    public boolean headLookLocked() {
        MythAnimState s = animState();
        return s == MythAnimState.CHARGE_WINDUP || s == MythAnimState.CHARGE_RUN || s == MythAnimState.CHARGE_RECOVER;
    }

    /** 设置变体，缩放取自服务端骨架数据；空字符串回到基础外观。 */
    public void setVariant(String name) {
        double scale = 1.0;
        if (!name.isEmpty()) {
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(getType());
            scale = RigRegistry.get(id).map(r -> r.scaleOf(name)).orElseGet(() -> {
                MythicAge.LOGGER.warn("{} 没有骨架数据，变体 {} 按缩放 1 处理", id, name);
                return 1.0;
            });
        }
        applyVariant(name, (float) scale);
    }

    private void applyVariant(String name, float scale) {
        entityData.set(VARIANT, name);
        entityData.set(VARIANT_SCALE, scale);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (VARIANT_SCALE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(variantScale());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", variant());
        tag.putFloat("VariantScale", variantScale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float scale = tag.contains("VariantScale") ? tag.getFloat("VariantScale") : 1.0F;
        applyVariant(tag.getString("Variant"), scale > 0 ? scale : 1.0F);
        // 进行中的冲锋不保存：重新加载后回到空闲（TC-T7-45）
        charge.cancel();
        setAnimState(MythAnimState.AUTO);
    }

    // ───────────── 动画状态 ─────────────

    public MythAnimState animState() {
        return MythAnimState.byOrdinal(entityData.get(ANIM_STATE));
    }

    /** 服务端设置动画状态；同一状态再次设置会让客户端从头播放。 */
    public void setAnimState(MythAnimState state) {
        entityData.set(ANIM_STATE, (byte) state.ordinal());
        entityData.set(ANIM_START, tickCount);
    }

    public ChargeTimeline charge() {
        return charge;
    }

    /** 开发命令：沿当前朝向冲锋一次（只做直线位移）。 */
    public void startCharge(double distanceBlocks) {
        charge.start(ChargeTimeline.runTicksFor(distanceBlocks, CHARGE_BLOCKS_PER_TICK));
        chargeYaw = getYRot();
        getNavigation().stop();
        haltInput();
        setAnimState(MythAnimState.CHARGE_WINDUP);
    }

    private void haltInput() {
        setSpeed(0);
        setZza(0);
        setXxa(0);
        Vec3 v = getDeltaMovement();
        setDeltaMovement(0, v.y, 0);
    }

    /** 服务端：播放一次性动画，ticks 刻后回到 AUTO。 */
    public void playOnce(MythAnimState state, int ticks) {
        setAnimState(state);
        animUntil = tickCount + ticks;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (animUntil > 0 && tickCount >= animUntil) {
            animUntil = 0;
            setAnimState(MythAnimState.AUTO);
        }
        if (charge.active()) {
            haltInput();
            tickCharge();
        }
    }

    private void tickCharge() {
        ChargeTimeline.Phase before = charge.phase();
        if (before == ChargeTimeline.Phase.RUN && charge.tickInPhase() > 0 && horizontalCollision) {
            charge.stopRun();
        }
        if (!charge.canTurn()) {
            setYRot(chargeYaw);
            yBodyRot = chargeYaw;
            yHeadRot = chargeYaw;
        }
        Vec3 v = getDeltaMovement();
        if (charge.moving()) {
            double yaw = Math.toRadians(chargeYaw);
            setDeltaMovement(-Math.sin(yaw) * CHARGE_BLOCKS_PER_TICK, v.y, Math.cos(yaw) * CHARGE_BLOCKS_PER_TICK);
        } else {
            setDeltaMovement(0, v.y, 0);
        }
        ChargeTimeline.Phase after = charge.advance();
        if (after != before) {
            setAnimState(MythAnimState.of(after));
        }
        if (!charge.active()) {
            haltInput();
        }
    }

    private final class ChargeLockGoal extends Goal {
        ChargeLockGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return charge.active();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<MinotaurEntity> main = new AnimationController<>(this, "main", 4, state -> {
            MythAnimState s = animState();
            int start = entityData.get(ANIM_START);
            if (start != clientSeenAnimStart) {
                clientSeenAnimStart = start;
                if (s != MythAnimState.AUTO) {
                    state.getController().forceAnimationReset();
                }
            }
            if (s == MythAnimState.AUTO) {
                s = horizontalSpeed() > WALK_THRESHOLD ? MythAnimState.WALK : MythAnimState.IDLE;
            }
            return state.setAndContinue(ANIMATIONS.get(s));
        });
        main.setAnimationSpeedHandler(e -> e.animationSpeed());
        controllers.add(main);
    }

    private double horizontalSpeed() {
        double dx = getX() - xo;
        double dz = getZ() - zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 行走时按实际步速调整播放速度，避免滑步；其他动画按原速。 */
    private double animationSpeed() {
        if (animState() == MythAnimState.AUTO && horizontalSpeed() > WALK_THRESHOLD) {
            double scale = Math.max(variantScale(), 0.01);
            return Mth.clamp(horizontalSpeed() * 20.0 / (WALK_CYCLE_BLOCKS * scale), 0.4, 3.0);
        }
        return 1.0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
