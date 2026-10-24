package io.github.happysnappingturtle.mythicage.village.resident;

import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.JobStep;
import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.village.work.VillageScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * 文明村庄的居民（T19 设计文档 §3，V1 §2）：一种实体，数据决定文明、职业与外观。
 * <ul>
 *   <li>自己只有三类目标：漂浮、逃跑回屋、执行当前工作单；派什么单由村庄调度器决定（V1 R2）；</li>
 *   <li>工作单不存进度：卸载、死亡、读档都会让当前工作单作废，托管退回（V1 §4.3）；</li>
 *   <li>不繁殖、不捡东西、死亡不掉落（手上的东西只是外观）；</li>
 *   <li>睡觉段回到床位睡下，黎明起床。</li>
 * </ul>
 */
public class ResidentEntity extends PathfinderMob implements GeoEntity, MythVariantHolder {
    public static final double REACH = 2.0;
    public static final double WALK_SPEED = 0.55;
    /** 连续这么多次算不出路（起点四周都走不通）就回床位锚点脱困。 */
    public static final int STUCK_LIMIT = 3;

    private static final EntityDataAccessor<String> CIV = SynchedEntityData.defineId(ResidentEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PROFESSION = SynchedEntityData.defineId(ResidentEntity.class, EntityDataSerializers.STRING);
    /** 正在播放的动作动画名；空串 = 按移动自动待机 / 行走。 */
    private static final EntityDataAccessor<String> ANIM = SynchedEntityData.defineId(ResidentEntity.class, EntityDataSerializers.STRING);
    /** 外观：第 0 位女装，第 1–3 位服色。 */
    private static final EntityDataAccessor<Integer> APPEARANCE = SynchedEntityData.defineId(ResidentEntity.class, EntityDataSerializers.INT);
    /** 手上搬着的东西（只是外观）。 */
    private static final EntityDataAccessor<String> CARRY = SynchedEntityData.defineId(ResidentEntity.class, EntityDataSerializers.STRING);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation CARRY_WALK = RawAnimation.begin().thenLoop("carry_walk");
    private static final RawAnimation SLEEP = RawAnimation.begin().thenLoop("sleep");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID village;
    @Nullable
    private BlockPos home;

    // 当前工作单（不存档）
    @Nullable
    private Job job;
    private int step;
    private int stepTicks;
    private long jobStart;
    private int repathIn;
    private int stuckTries;

    public ResidentEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CIV, "");
        builder.define(PROFESSION, "");
        builder.define(ANIM, "");
        builder.define(APPEARANCE, 0);
        builder.define(CARRY, "");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ResidentFleeGoal(this));
        goalSelector.addGoal(2, new ResidentJobGoal(this));
        // 导航只是「允许穿门」，真的开门要有这个目标（原版村民靠大脑行为开门）；进门后随手关上
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // ───────────── 身份 ─────────────

    /** 生成后由村庄调用一次。 */
    public void setup(UUID village, String civ, String profession, BlockPos home, String name, boolean female, int palette) {
        this.village = village;
        this.home = home.immutable();
        entityData.set(CIV, civ);
        entityData.set(PROFESSION, profession);
        entityData.set(APPEARANCE, (female ? 1 : 0) | ((palette & 7) << 1));
        setCustomName(Component.literal(name));
        setCustomNameVisible(false);
    }

    public Optional<UUID> village() {
        return Optional.ofNullable(village);
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public String civ() {
        return entityData.get(CIV);
    }

    public String profession() {
        return entityData.get(PROFESSION);
    }

    public void setProfession(String p) {
        entityData.set(PROFESSION, p);
    }

    public boolean female() {
        return (entityData.get(APPEARANCE) & 1) != 0;
    }

    public int palette() {
        return (entityData.get(APPEARANCE) >> 1) & 7;
    }

    public String anim() {
        return entityData.get(ANIM);
    }

    public String carrying() {
        return entityData.get(CARRY);
    }

    public void setCarrying(String what) {
        entityData.set(CARRY, what == null ? "" : what);
    }

    // ───────────── 工作单 ─────────────

    public boolean idle() {
        return job == null && !isSleeping();
    }

    public boolean walking() {
        return job != null && step < job.steps().size() && job.steps().get(step).kind() == JobStep.Kind.MOVE;
    }

    public Optional<Job> job() {
        return Optional.ofNullable(job);
    }

    public int step() {
        return step;
    }

    /** 调度器派单（托管已经做好）。 */
    public void assign(Job j) {
        if (isSleeping()) {
            stopSleeping();
        }
        job = j;
        step = 0;
        stepTicks = 0;
        jobStart = level().getGameTime();
        repathIn = 0;
        entityData.set(ANIM, "");
    }

    /** 当前工作单作废（打断、超时、目标失效）：托管退回。{@code countFailure}：目标记一次失败。 */
    public void abortJob(boolean countFailure) {
        if (job == null) {
            return;
        }
        Job j = job;
        clearJob();
        if (level() instanceof ServerLevel sl) {
            VillageScheduler.onJobAborted(sl, this, j, countFailure);
        }
    }

    private void clearJob() {
        job = null;
        step = 0;
        stepTicks = 0;
        entityData.set(ANIM, "");
        setCarrying("");
        getNavigation().stop();
    }

    /**
     * 没在睡觉却站在床方块里（旧存档换人、睡觉时区块卸载等）：挪到床边能站的地方。
     * 否则寻路的起点就在床里，一步都走不出去。返回是否挪动了。
     */
    public boolean unstickFromBed() {
        if (isSleeping() || !(level().getBlockState(blockPosition()).getBlock() instanceof BedBlock)) {
            return false;
        }
        BlockPos bed = blockPosition();
        Optional<net.minecraft.world.phys.Vec3> stand = BedBlock.findStandUpPosition(getType(), level(), bed,
                level().getBlockState(bed).getValue(BedBlock.FACING), getYRot());
        net.minecraft.world.phys.Vec3 to = stand.orElse(net.minecraft.world.phys.Vec3.atBottomCenterOf(bed.above()));
        moveTo(to.x, to.y, to.z, getYRot(), getXRot());
        getNavigation().stop();
        return true;
    }

    /** 卡住时回到自己的床位锚点（床位锚点是生成时就能站的地面）。 */
    void returnHome() {
        if (home != null && !blockPosition().equals(home)) {
            moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, getYRot(), getXRot());
            getNavigation().stop();
        }
    }

    /** 每刻由 {@link ResidentJobGoal} 调用。 */
    void tickJob() {
        if (job == null || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (step == 0 && stepTicks == 0) {
            unstickFromBed();
        }
        if (level().getGameTime() - jobStart > job.timeoutTicks()) {
            abortJob(true);
            return;
        }
        if (step >= job.steps().size()) {
            clearJob();
            return;
        }
        JobStep s = job.steps().get(step);
        switch (s.kind()) {
            case MOVE -> {
                BlockPos t = new BlockPos(job.target().x(), job.target().y(), job.target().z());
                if (reached(t)) {
                    getNavigation().stop();
                    next();
                } else if (--repathIn <= 0 || getNavigation().isDone()) {
                    repathIn = 40;
                    boolean moving = getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, WALK_SPEED / 0.5);
                    net.minecraft.world.level.pathfinder.Path p = getNavigation().getPath();
                    if (!moving || p == null || p.getNodeCount() <= 1) {
                        if (++stuckTries >= STUCK_LIMIT) {
                            stuckTries = 0;
                            returnHome();
                        }
                    } else {
                        stuckTries = 0;
                    }
                }
            }
            case ACT -> {
                if (stepTicks == 0) {
                    entityData.set(ANIM, s.anim());
                    getNavigation().stop();
                }
                getLookControl().setLookAt(job.target().x() + 0.5, job.target().y() + 0.5, job.target().z() + 0.5);
                if (++stepTicks >= s.ticks()) {
                    entityData.set(ANIM, "");
                    next();
                }
            }
            case SETTLE -> {
                Job j = job;
                clearJob();
                VillageScheduler.onJobSettle(sl, this, j);
            }
        }
    }

    private void next() {
        step++;
        stepTicks = 0;
    }

    public boolean reached(BlockPos t) {
        double dx = getX() - (t.getX() + 0.5);
        double dz = getZ() - (t.getZ() + 0.5);
        return dx * dx + dz * dz <= REACH * REACH && Math.abs(getY() - t.getY()) <= 2.5;
    }

    /** 只供 GameTest：把当前步骤推进到结算前（不走路、不等动作）。 */
    public void fastForwardForTest() {
        if (job != null) {
            while (step < job.steps().size() && job.steps().get(step).kind() != JobStep.Kind.SETTLE) {
                step++;
            }
            stepTicks = 0;
        }
    }

    /** 只供 GameTest：立刻执行一步。 */
    public void tickJobForTest() {
        tickJob();
    }

    /** 只供 GameTest（TC-T19-44）：让当前工作单「已经开工了 ticks 刻」，用来触发超时。 */
    public void ageJobForTest(long ticks) {
        jobStart -= ticks;
    }

    // ───────────── 睡觉 ─────────────

    /** 在床位旁就睡下（床位是床方块时用原版的睡姿）。 */
    public boolean trySleepAtHome() {
        if (home == null || isSleeping() || job != null || !reached(home)) {
            return false;
        }
        BlockState bed = level().getBlockState(home);
        if (bed.getBlock() instanceof BedBlock) {
            startSleeping(home);
        } else {
            entityData.set(ANIM, "sleep");
        }
        return true;
    }

    public void wakeUp() {
        if (isSleeping()) {
            stopSleeping();
        }
        if ("sleep".equals(anim())) {
            entityData.set(ANIM, "");
        }
    }

    public boolean resting() {
        return isSleeping() || "sleep".equals(anim());
    }

    // ───────────── 生命周期 ─────────────

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // 脱困：卡在方块里（会窒息）就回床位锚点；没睡觉却站在床里就下床
        if (tickCount % 40 == 0) {
            if (isInWall()) {
                returnHome();
            } else {
                unstickFromBed();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && job != null) {
            abortJob(false);
        }
        if (hurt) {
            wakeUp();
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        abortJob(false);
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            VillageScheduler.onResidentDied(sl, this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (job != null && level() instanceof ServerLevel) {
            abortJob(false);
        }
        super.remove(reason);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isAlive()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp) {
            VillageScheduler.onInteract(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (village != null) {
            tag.putUUID("Village", village);
        }
        if (home != null) {
            tag.putLong("Home", home.asLong());
        }
        tag.putString("Civ", civ());
        tag.putString("Profession", profession());
        tag.putInt("Appearance", entityData.get(APPEARANCE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        village = tag.hasUUID("Village") ? tag.getUUID("Village") : null;
        home = tag.contains("Home") ? BlockPos.of(tag.getLong("Home")) : null;
        entityData.set(CIV, tag.getString("Civ"));
        entityData.set(PROFESSION, tag.getString("Profession"));
        entityData.set(APPEARANCE, tag.getInt("Appearance"));
    }

    // ───────────── 外观 ─────────────

    /** 贴图变体：男 / 女 × 6 种服色（{@code resident_m0}…{@code resident_f5}，由 resident_palettes.py 生成）。 */
    @Override
    public String variant() {
        return (female() ? "f" : "m") + palette();
    }

    @Override
    public float variantScale() {
        return 1;
    }

    @Override
    public boolean headLookLocked() {
        return !anim().isEmpty();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (isSleeping() || "sleep".equals(anim())) {
                return state.setAndContinue(SLEEP);
            }
            String a = anim();
            if (!a.isEmpty()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop(ResidentAnims.resolve(a)));
            }
            if (state.isMoving()) {
                return state.setAndContinue(carrying().isEmpty() ? WALK : CARRY_WALK);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
