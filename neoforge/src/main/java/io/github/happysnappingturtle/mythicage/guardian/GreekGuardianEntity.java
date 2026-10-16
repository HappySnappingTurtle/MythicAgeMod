package io.github.happysnappingturtle.mythicage.guardian;

import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * 希腊守护者：神庙祭司 Hiereus（T9 设计文档 §7；外观见 VI-GR-PRI-01，GeckoLib 模型与动画见 T9b 设计文档）。
 * 有固定身份 guardianId 与居所 home；白天在居所 8 格内走动，夜里留在 3 格内；
 * 有玩家打开交易界面时停步并面向该玩家。死亡后由 {@link GuardianSpawner} 按注册表在居所复原。
 */
public class GreekGuardianEntity extends PathfinderMob implements GeoEntity, MythVariantHolder {
    public static final int DAY_RADIUS = 8;
    public static final int NIGHT_RADIUS = 3;
    /** 客户端判定「在走路」的水平速度阈值（格/刻）。 */
    public static final double WALK_THRESHOLD = 0.02;
    /** 行走动画每循环前进的距离（格）：大腿摆幅 ±14°、腿长 14u，两步约 0.85 格。 */
    public static final double WALK_CYCLE_BLOCKS = 0.85;

    /** 一次性手势（T9b 设计文档 §3）：服务端触发，同步给客户端，播完回到待机/行走。 */
    public enum Gesture {
        NONE(null, 0), GREET("greet", 32), ANNOUNCE("announce", 44), POINT("point", 28);

        private final String animation;
        private final int ticks;

        Gesture(String animation, int ticks) {
            this.animation = animation;
            this.ticks = ticks;
        }

        public String animation() {
            return animation;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        static Gesture byOrdinal(int o) {
            return o >= 0 && o < values().length ? values()[o] : NONE;
        }
    }

    private static final EntityDataAccessor<Byte> GESTURE = SynchedEntityData.defineId(GreekGuardianEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> GESTURE_START = SynchedEntityData.defineId(GreekGuardianEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int gestureEndTick;
    private int clientSeenGestureStart = -1;

    @Nullable
    private UUID guardianId;
    @Nullable
    private BlockPos home;

    public GreekGuardianEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GESTURE, (byte) Gesture.NONE.ordinal());
        builder.define(GESTURE_START, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HoldForTradeGoal());
        goalSelector.addGoal(2, new PanicGoal(this, 0.7));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // ───────────── 身份与居所 ─────────────

    @Nullable
    public UUID guardianId() {
        return guardianId;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    /** 由生成方（村庄规则、复原、刷怪蛋）设定身份与居所。 */
    public void assign(UUID guardianId, BlockPos home) {
        this.guardianId = guardianId;
        this.home = home.immutable();
        updateRestriction();
    }

    private void updateRestriction() {
        if (home != null) {
            restrictTo(home, level().isNight() ? NIGHT_RADIUS : DAY_RADIUS);
        }
    }

    /** 刷怪蛋与 /summon：没有身份时以当前位置为居所登记一个新身份。 */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        if (guardianId == null) {
            GuardianSpawner.registerNew(level.getLevel(), this, blockPosition(), Optional.empty());
        }
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 100 == 0) {
            updateRestriction();
        }
        if (gesture() != Gesture.NONE && tickCount >= gestureEndTick) {
            entityData.set(GESTURE, (byte) Gesture.NONE.ordinal());
        }
    }

    // ───────────── 手势与动画 ─────────────

    public Gesture gesture() {
        return Gesture.byOrdinal(entityData.get(GESTURE));
    }

    /** 服务端播放一次手势；同一手势再次触发会从头播放。 */
    public void playGesture(Gesture g) {
        entityData.set(GESTURE, (byte) g.ordinal());
        entityData.set(GESTURE_START, tickCount);
        gestureEndTick = tickCount + g.ticks;
    }

    @Override
    public String variant() {
        return "";
    }

    @Override
    public float variantScale() {
        return 1.0F;
    }

    /** 宣告时抬头望天：不叠加视线方向。 */
    @Override
    public boolean headLookLocked() {
        return gesture() == Gesture.ANNOUNCE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<GreekGuardianEntity> main = new AnimationController<>(this, "main", 4, state -> {
            Gesture g = gesture();
            int start = entityData.get(GESTURE_START);
            if (start != clientSeenGestureStart) {
                clientSeenGestureStart = start;
                if (g != Gesture.NONE) {
                    state.getController().forceAnimationReset();
                }
            }
            if (g != Gesture.NONE) {
                return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(g.animation()));
            }
            return state.setAndContinue(horizontalSpeed() > WALK_THRESHOLD ? WALK : IDLE);
        });
        main.setAnimationSpeedHandler(e -> e.animationSpeed());
        controllers.add(main);
    }

    private double horizontalSpeed() {
        double dx = getX() - xo;
        double dz = getZ() - zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 行走时按实际步速调整播放速度，避免滑步。 */
    private double animationSpeed() {
        if (gesture() == Gesture.NONE && horizontalSpeed() > WALK_THRESHOLD) {
            return net.minecraft.util.Mth.clamp(horizontalSpeed() * 20.0 / WALK_CYCLE_BLOCKS, 0.4, 3.0);
        }
        return 1.0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // ───────────── 交互 ─────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isAlive()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp) {
            TradeService.openGuardian(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof ServerPlayer player) {
            NeoForge.EVENT_BUS.post(new GuardianEvent.Harmed(this, player));
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server && guardianId != null) {
            GuardianRegistry.get(server.getServer()).markDead(guardianId, MythDays.today(server.getServer()));
            Optional<ServerPlayer> killer = source.getEntity() instanceof ServerPlayer p ? Optional.of(p) : Optional.empty();
            NeoForge.EVENT_BUS.post(new GuardianEvent.Killed(this, killer));
            TradeService.onGuardianGone(this);
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    // ───────────── 存档 ─────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (guardianId != null) {
            tag.putUUID("GuardianId", guardianId);
        }
        if (home != null) {
            tag.putLong("Home", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("GuardianId")) {
            guardianId = tag.getUUID("GuardianId");
        }
        if (tag.contains("Home")) {
            home = BlockPos.of(tag.getLong("Home"));
            updateRestriction();
        }
    }

    public GlobalPos globalHome() {
        return GlobalPos.of(level().dimension(), home != null ? home : blockPosition());
    }

    /** 有玩家打开交易界面时：停止走动，面向最近的那位。 */
    private final class HoldForTradeGoal extends Goal {
        @Nullable
        private ServerPlayer viewer;

        HoldForTradeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            viewer = TradeService.nearestViewer(GreekGuardianEntity.this);
            return viewer != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (viewer != null) {
                getLookControl().setLookAt(viewer, 30, 30);
            }
        }
    }
}
