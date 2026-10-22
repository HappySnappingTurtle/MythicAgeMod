package io.github.happysnappingturtle.mythicage.summon.greek;

import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.mixin.LivingEntityJumpingAccessor;
import io.github.happysnappingturtle.mythicage.summon.Disguised;
import io.github.happysnappingturtle.mythicage.summon.KerykeionItem;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import io.github.happysnappingturtle.mythicage.summon.SkyEyePayload;
import io.github.happysnappingturtle.mythicage.summon.SummonLink;
import io.github.happysnappingturtle.mythicage.summon.SummonService;
import io.github.happysnappingturtle.mythicage.summon.SummonTargets;
import io.github.happysnappingturtle.mythicage.summon.SummonTokenItem;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 召唤版天马（T14 设计文档 §6.2、§11.4）：不战斗的侦察单位。
 * <ul>
 *   <li>跟随：主人头顶开阔时在其上空 6–10 格绕圈飞；头顶有遮挡就落到主人身边步行（D7）；驻守时在驻守点上空盘旋；</li>
 *   <li>天眼：每 10 秒把 32 格内的敌方标给主人一人看 5 秒，会伪装的生物对主人现形；</li>
 *   <li>骑乘：只有主人能骑；地面像马一样走，按跳跃起飞；空中朝视线方向飞，耐力 30 秒，耗尽后只能缓降；</li>
 *   <li>死亡后由框架按单位卡在主人的希腊神坛旁重生（1 日）。</li>
 * </ul>
 */
public class SummonPegasusEntity extends PathfinderMob implements GeoEntity, MythSummon, MythVariantHolder {
    /** 服务端写入的一次性动画；AUTO 由客户端按状态挑（站立 / 行走 / 室内紧收 / 扇翼 / 滑翔）。 */
    public enum Anim { AUTO, TAKEOFF, LAND, SKY_EYE }

    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(SummonPegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> INDOOR = SynchedEntityData.defineId(SummonPegasusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> ANIM = SynchedEntityData.defineId(SummonPegasusEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANIM_START = SynchedEntityData.defineId(SummonPegasusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAMINA = SynchedEntityData.defineId(SummonPegasusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAMINA_MAX = SynchedEntityData.defineId(SummonPegasusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EYE_GLOW_UNTIL = SynchedEntityData.defineId(SummonPegasusEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FOLD = RawAnimation.begin().thenLoop("fold");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("glide");
    private static final Map<Anim, RawAnimation> ONCE = new EnumMap<>(Map.of(
            Anim.TAKEOFF, RawAnimation.begin().thenPlayAndHold("takeoff"),
            Anim.LAND, RawAnimation.begin().thenPlayAndHold("land"),
            Anim.SKY_EYE, RawAnimation.begin().thenPlayAndHold("sky_eye")));
    /** 行走动画每循环前进的距离（格）：对角步一循环两步，步幅约 0.9 格。 */
    private static final double WALK_CYCLE_BLOCKS = 1.8;
    private static final double WALK_THRESHOLD = 0.02;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final SummonLink link = new SummonLink();
    private int clientSeenAnimStart = -1;
    private int animUntil;

    // 数值（entity_stats/greek/summon_pegasus.json）
    private double flySpeed = 0.5;
    private double rideFlySpeed = 0.55;
    private int eyeRadius = 32;
    private int eyePeriod = 200;
    private int eyeTicks = 100;

    /** 没人骑时的飞行状态（服务端）。 */
    private boolean aiFlying;
    private int eyeTimer;
    /** 最近一次天眼标出的实体（GameTest 读）。 */
    private final List<UUID> lastMarked = new ArrayList<>();
    /** 骑乘时由控制方（骑手的客户端）判断的飞行状态。 */
    private boolean riderFlying;
    /** 盘旋的相位：不同的天马不挤在同一点。 */
    private final float circlePhase;

    public SummonPegasusEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        circlePhase = random.nextFloat() * Mth.TWO_PI;
    }

    public static AttributeSupplier.Builder createSummonAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
        builder.define(INDOOR, false);
        builder.define(ANIM, (byte) 0);
        builder.define(ANIM_START, 0);
        builder.define(STAMINA, 600);
        builder.define(STAMINA_MAX, 600);
        builder.define(EYE_GLOW_UNTIL, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new GroundReturn());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ───────────── MythSummon ─────────────

    @Override
    public SummonLink link() {
        return link;
    }

    @Override
    public boolean combatUnit() {
        return false;
    }

    @Override
    public boolean requestSpecial(LivingEntity target) {
        return false;
    }

    @Override
    public void applyStats(SummonUnits.Stats s) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", 30));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(s.attr("movement_speed", 0.3));
        setHealth(getMaxHealth());
        flySpeed = s.specialDouble("fly_speed", flySpeed);
        rideFlySpeed = s.specialDouble("ride_fly_speed", rideFlySpeed);
        int max = s.specialInt("stamina_ticks", 600);
        entityData.set(STAMINA_MAX, max);
        entityData.set(STAMINA, max);
        eyeRadius = s.specialInt("sky_eye_radius", eyeRadius);
        eyePeriod = s.specialInt("sky_eye_period", eyePeriod);
        eyeTicks = s.specialInt("sky_eye_ticks", eyeTicks);
    }

    // ───────────── 外观状态 ─────────────

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
        return anim() == Anim.SKY_EYE;
    }

    public Anim anim() {
        int i = entityData.get(ANIM);
        return i >= 0 && i < Anim.values().length ? Anim.values()[i] : Anim.AUTO;
    }

    private void playOnce(Anim a, int ticks) {
        entityData.set(ANIM, (byte) a.ordinal());
        entityData.set(ANIM_START, tickCount);
        animUntil = tickCount + ticks;
    }

    /** 飞着（没人骑时的 AI 飞行，或骑乘时离地）。 */
    public boolean flying() {
        return entityData.get(FLYING);
    }

    public boolean indoor() {
        return entityData.get(INDOOR);
    }

    public int stamina() {
        return entityData.get(STAMINA);
    }

    public int staminaMax() {
        return entityData.get(STAMINA_MAX);
    }

    /** 天眼发光（只在放出天眼后的 1 秒里画眼睛的金光）。 */
    public boolean eyeGlowing() {
        return tickCount < entityData.get(EYE_GLOW_UNTIL);
    }

    /** 只供 GameTest：直接设耐力。 */
    public void setStaminaForTest(int v) {
        entityData.set(STAMINA, v);
    }

    public List<UUID> lastMarked() {
        return List.copyOf(lastMarked);
    }

    // ───────────── 服务端每刻 ─────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount % 10 == 0) {
            entityData.set(INDOOR, !headroom(blockPosition(), 3));
        }
        if (animUntil > 0 && tickCount >= animUntil) {
            animUntil = 0;
            entityData.set(ANIM, (byte) Anim.AUTO.ordinal());
        }
        int stamina = stamina();
        if (isVehicle()) {
            boolean air = !onGround() && !isInWater();
            entityData.set(FLYING, air);
            stamina = air ? stamina - 1 : stamina + 2;
            fallDistance = 0;
        } else {
            entityData.set(FLYING, aiFlying);
            stamina += 2;
        }
        entityData.set(STAMINA, Mth.clamp(stamina, 0, staminaMax()));
        if (link.bound() && ++eyeTimer >= eyePeriod) {
            eyeTimer = 0;
            skyEye();
        }
    }

    /** 没人骑时：决定起飞 / 降落，飞着就朝盘旋点转向。 */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isVehicle()) {
            return;
        }
        ServerPlayer owner = SummonTargets.owner(this);
        Vec3 center = null;
        boolean open = false;
        double radius = 5.0;
        if (link.mode == SummonMode.GUARD && link.guard != null) {
            center = Vec3.atBottomCenterOf(link.guard);
            open = headroom(link.guard, 6);
            radius = 4.0;
        } else if (owner != null && owner.level() == level() && !owner.isSpectator()) {
            center = owner.position();
            open = headroom(owner.blockPosition(), 6);
        }
        boolean wantFly = center != null && open;
        if (wantFly && !aiFlying) {
            takeOff();
        } else if (!wantFly && aiFlying) {
            if (owner != null && owner.level() == level() && link.mode != SummonMode.GUARD) {
                // 主人进了室内：落到主人身边
                SummonService.teleportNear(this, owner);
                land();
            } else {
                setDeltaMovement(getDeltaMovement().multiply(0.8, 0, 0.8).add(0, -0.3, 0));
                if (onGround()) {
                    land();
                }
            }
        }
        if (aiFlying && center != null) {
            double angle = tickCount * 0.035 + circlePhase;
            double height = 8.0 + 1.5 * Math.sin(tickCount * 0.02 + circlePhase);
            Vec3 target = center.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            if (position().distanceToSqr(target) > 24.0 * 24.0) {
                moveTo(target.x, target.y, target.z, getYRot(), 0);
            }
            steer(target, flySpeed);
        }
    }

    private void takeOff() {
        aiFlying = true;
        setNoGravity(true);
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().x, 0.6, getDeltaMovement().z);
        playOnce(Anim.TAKEOFF, 12);
    }

    private void land() {
        aiFlying = false;
        setNoGravity(false);
        playOnce(Anim.LAND, 10);
    }

    /** 朝目标点转向：限速，靠近时减速；撞墙就往上抬；身体朝向跟着速度。 */
    private void steer(Vec3 target, double speed) {
        Vec3 d = target.subtract(position());
        double dist = d.length();
        Vec3 want = dist < 0.05 ? Vec3.ZERO : d.scale(Math.min(speed, dist * 0.2) / dist);
        Vec3 v = getDeltaMovement().lerp(want, 0.2);
        if (horizontalCollision) {
            v = v.add(0, 0.15, 0);
        }
        setDeltaMovement(v);
        faceVelocity(v);
    }

    private void faceVelocity(Vec3 v) {
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }
    }

    /** from 头顶往上 n 格都没有会挡住的方块。 */
    private boolean headroom(BlockPos from, int n) {
        for (int i = 1; i <= n; i++) {
            BlockPos p = from.above(i);
            if (!level().getBlockState(p).getCollisionShape(level(), p).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 地面上（没飞时）跟随主人 / 回驻守点；飞着或被骑时不动。 */
    private final class GroundReturn extends Goal {
        private int repath;

        GroundReturn() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private Vec3 goal() {
            if (link.mode == SummonMode.GUARD && link.guard != null) {
                return Vec3.atBottomCenterOf(link.guard);
            }
            ServerPlayer owner = SummonTargets.owner(SummonPegasusEntity.this);
            return owner != null && owner.level() == level() ? owner.position() : null;
        }

        @Override
        public boolean canUse() {
            Vec3 g = goal();
            return !aiFlying && !isVehicle() && g != null && position().distanceToSqr(g) > 6 * 6;
        }

        @Override
        public boolean canContinueToUse() {
            Vec3 g = goal();
            return !aiFlying && !isVehicle() && g != null && position().distanceToSqr(g) > 3 * 3;
        }

        @Override
        public void tick() {
            Vec3 g = goal();
            ServerPlayer owner = SummonTargets.owner(SummonPegasusEntity.this);
            if (owner != null && link.mode != SummonMode.GUARD && distanceToSqr(owner) > 24.0 * 24.0) {
                SummonService.teleportNear(SummonPegasusEntity.this, owner);
                return;
            }
            if (--repath <= 0 && g != null) {
                repath = 10;
                getNavigation().moveTo(g.x, g.y, g.z, 1.1);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    // ───────────── 天眼 ─────────────

    /** 标出 32 格内的敌方给主人看；会伪装的生物对主人现形。没有敌方时不放动画。 */
    public void skyEye() {
        ServerPlayer owner = SummonTargets.owner(this);
        lastMarked.clear();
        if (owner == null || owner.level() != level()) {
            return;
        }
        double r2 = (double) eyeRadius * eyeRadius;
        List<LivingEntity> found = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(eyeRadius),
                e -> e != this && e != owner && distanceToSqr(e) <= r2 && SummonTargets.isEnemyOf(owner, e));
        if (found.isEmpty()) {
            return;
        }
        int[] ids = new int[found.size()];
        for (int i = 0; i < found.size(); i++) {
            LivingEntity e = found.get(i);
            ids[i] = e.getId();
            lastMarked.add(e.getUUID());
            if (e instanceof Disguised d) {
                d.reveal(owner, eyeTicks);
            }
        }
        if (!(owner instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(owner, new SkyEyePayload(ids, eyeTicks));
        }
        entityData.set(EYE_GLOW_UNTIL, tickCount + 20);
        if (!flying() && !isVehicle()) {
            playOnce(Anim.SKY_EYE, 20);
        }
    }

    // ───────────── 骑乘 ─────────────

    /** 只有主人能骑（空手或拿着别的东西右键）；传令杖、召唤符照常给物品处理。 */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof KerykeionItem || held.getItem() instanceof SummonTokenItem || isVehicle()
                || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!canMount(player)) {
            player.displayClientMessage(Component.translatable("mythic_age.summon.not_your_mount", getDisplayName()), true);
            return InteractionResult.CONSUME;
        }
        mount(player);
        return InteractionResult.CONSUME;
    }

    /** 主人上马（服务端）：停下 AI 飞行与寻路，交给骑手控制。 */
    public boolean mount(Player player) {
        if (!canMount(player)) {
            return false;
        }
        aiFlying = false;
        setNoGravity(false);
        getNavigation().stop();
        return player.startRiding(this);
    }

    /** 只有主人能骑（R1）。 */
    public boolean canMount(Player player) {
        return link.owner != null && player.getUUID().equals(link.owner) && !isVehicle();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        riderFlying = !onGround();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && !onGround() && !isInWater()) {
            // 在空中下马：天马自己接着飞
            aiFlying = true;
            setNoGravity(true);
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        setRot(player.getYRot(), player.getXRot() * 0.5F);
        yRotO = getYRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float strafe = player.xxa * 0.5F;
        float forward = player.zza <= 0 ? player.zza * 0.25F : player.zza;
        return new Vec3(strafe, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /** 骑乘移动在骑手的客户端上算（同原版马）：地面走、按跳跃起飞、空中朝视线飞，耐力耗尽只能缓降。 */
    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance() && getControllingPassenger() instanceof Player p) {
            boolean jump = ((LivingEntityJumpingAccessor) p).mythicAge$isJumping();
            // 落进水里：收翼浮到水面（不会带着骑手沉底），在水面上恢复耐力，按跳跃再起飞
            if (isInWater()) {
                riderFlying = false;
                if (getFluidHeight(net.minecraft.tags.FluidTags.WATER) > 0.6) {
                    setDeltaMovement(getDeltaMovement().multiply(0.9, 1, 0.9).add(0, 0.06, 0));
                }
            }
            // 按跳跃起飞：不要求正踩在地上（重进存档时骑乘的天马可能悬在空中、客户端判定不在地面）
            if (!riderFlying && jump && stamina() > 0 && headroom(blockPosition(), 3)) {
                riderFlying = true;
                setDeltaMovement(getDeltaMovement().x, 0.6, getDeltaMovement().z);
            }
            // 有人骑时不会自由落体：不在飞行状态却在快速下坠，就展翼转为滑翔
            if (!riderFlying && !onGround() && getDeltaMovement().y < -0.3) {
                riderFlying = true;
            }
            if (riderFlying) {
                rideFly(p, jump);
                return;
            }
        }
        super.travel(input);
    }

    private void rideFly(Player p, boolean jump) {
        boolean tired = stamina() <= 0;
        Vec3 want = p.zza > 0 ? p.getLookAngle().scale(rideFlySpeed) : new Vec3(0, -0.06, 0);
        if (jump && !tired) {
            want = new Vec3(want.x, Math.max(want.y, 0.35), want.z);
        }
        if (tired) {
            want = new Vec3(want.x * 0.6, Math.min(want.y, -0.15), want.z * 0.6);
        }
        Vec3 v = getDeltaMovement().lerp(want, 0.25);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        fallDistance = 0;
        calculateEntityAnimation(true);
        if (onGround() && v.y <= 0.0 && !(jump && !tired)) {
            riderFlying = false;
        }
    }

    /** 骑手在客户端上是否正飞着（耐力条用）。 */
    public boolean riderFlying() {
        return riderFlying;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    // ───────────── 阵营、存档 ─────────────

    @Override
    public boolean isAlliedTo(Entity other) {
        if (link.owner != null && (other.getUUID().equals(link.owner)
                || (other instanceof MythSummon s && link.owner.equals(s.link().owner)))) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        link.save(tag);
        tag.putBoolean("AiFlying", aiFlying);
        tag.putInt("Stamina", stamina());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        link.load(tag);
        aiFlying = tag.getBoolean("AiFlying");
        setNoGravity(aiFlying);
        if (tag.contains("Stamina")) {
            entityData.set(STAMINA, tag.getInt("Stamina"));
        }
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<SummonPegasusEntity> main = new AnimationController<>(this, "main", 5, state -> {
            Anim a = anim();
            int start = entityData.get(ANIM_START);
            if (start != clientSeenAnimStart) {
                clientSeenAnimStart = start;
                if (a != Anim.AUTO) {
                    state.getController().forceAnimationReset();
                }
            }
            if (a != Anim.AUTO) {
                return state.setAndContinue(ONCE.get(a));
            }
            double h = horizontalSpeed();
            if (flying() || riderFlying) {
                return state.setAndContinue(getY() - yo > 0.02 || h > 0.3 ? FLY : GLIDE);
            }
            if (h > WALK_THRESHOLD) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(indoor() ? FOLD : IDLE);
        });
        main.setAnimationSpeedHandler(e -> walkSpeedScale());
        controllers.add(main);
    }

    private double horizontalSpeed() {
        double dx = getX() - xo;
        double dz = getZ() - zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 行走时按步速调整播放速度，避免滑步。 */
    private double walkSpeedScale() {
        if (anim() == Anim.AUTO && !flying() && !riderFlying && horizontalSpeed() > WALK_THRESHOLD) {
            return Mth.clamp(horizontalSpeed() * 20.0 / WALK_CYCLE_BLOCKS, 0.4, 3.0);
        }
        return 1.0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
