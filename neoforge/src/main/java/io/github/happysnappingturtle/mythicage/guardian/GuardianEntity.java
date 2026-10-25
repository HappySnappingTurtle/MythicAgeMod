package io.github.happysnappingturtle.mythicage.guardian;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
 * 祭司的通用部分（T9 设计文档 §7；T10b 设计文档 §4；19 号 U2：每个文明都有祭司，名字不同、作用相同）。
 * 有固定身份 guardianId、居所 home 与所属文明；白天在居所 8 格内走动，夜里留在 3 格内；
 * 有玩家打开交易界面时停步并面向该玩家。死亡后由 {@link GuardianSpawner} 按注册表在居所复原。
 * 模型、动画与手势时长由各文明的子类提供。
 */
public abstract class GuardianEntity extends PathfinderMob {
    public static final int DAY_RADIUS = 8;
    public static final int NIGHT_RADIUS = 3;

    private static final EntityDataAccessor<Byte> GESTURE = SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> GESTURE_START = SynchedEntityData.defineId(GuardianEntity.class, EntityDataSerializers.INT);

    private int gestureEndTick;
    @Nullable
    private UUID guardianId;
    @Nullable
    private BlockPos home;
    private Civilization civ;

    protected GuardianEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.civ = GuardianTypes.civOf(type);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GESTURE, (byte) GuardianGesture.NONE.ordinal());
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
        goalSelector.addGoal(2, new ErrandGoal());
        goalSelector.addGoal(2, new PanicGoal(this, 0.7));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // ───────────── 身份、居所与文明 ─────────────

    @Nullable
    public UUID guardianId() {
        return guardianId;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public Civilization civ() {
        return civ;
    }

    /** 由生成方设定所属文明（测试文明可能复用别的文明的实体类型）。 */
    public void setCiv(Civilization civ) {
        this.civ = civ;
    }

    /** 由生成方（村庄规则、复原、刷怪蛋）设定身份与居所。 */
    public void assign(UUID guardianId, BlockPos home) {
        this.guardianId = guardianId;
        this.home = home.immutable();
        updateRestriction();
    }

    private void updateRestriction() {
        if (errand != null) {
            return;
        }
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
        if (gesture() != GuardianGesture.NONE && tickCount >= gestureEndTick) {
            entityData.set(GESTURE, (byte) GuardianGesture.NONE.ordinal());
        }
    }

    // ───────────── 手势 ─────────────

    /** 手势时长（刻）；由各文明按自己的动画给出。 */
    protected abstract int gestureTicks(GuardianGesture g);

    public GuardianGesture gesture() {
        return GuardianGesture.byOrdinal(entityData.get(GESTURE));
    }

    /** 当前手势开始的刻（客户端据此判断是否需要从头播放）。 */
    public int gestureStart() {
        return entityData.get(GESTURE_START);
    }

    /** 服务端播放一次手势；同一手势再次触发会从头播放。 */
    public void playGesture(GuardianGesture g) {
        playGesture(g, gestureTicks(g));
    }

    /** 播放手势并指定时长（差事：坐诊一整天、听橡 200 刻）。 */
    public void playGesture(GuardianGesture g, int ticks) {
        entityData.set(GESTURE, (byte) g.ordinal());
        entityData.set(GESTURE_START, tickCount);
        gestureEndTick = tickCount + Math.max(1, ticks);
    }

    // ───────────── 差事（T19 §1.10 J4） ─────────────

    /**
     * 一趟差事：依次走到 {@code stops}，每处播 {@code gesture} {@code ticks} 刻（到达时调用 {@code onArrive}）；
     * {@code until} > 0 时最后一处一直待到这一刻（世界刻）。差事期间不受居所半径限制，做完回居所。不存档（纯表现）。
     */
    public record Errand(String tag, java.util.List<BlockPos> stops, GuardianGesture gesture, int ticks, long until,
                         @Nullable java.util.function.BiConsumer<GuardianEntity, BlockPos> onArrive) {
    }

    @Nullable
    private Errand errand;
    private int errandStop;
    private int errandWait = -1;
    private int errandTicks;
    private int errandRepath;

    @Nullable
    public Errand errand() {
        return errand;
    }

    /** 正在某差事的目的地上（如神谕坐诊中）。 */
    public boolean atErrand(String tag) {
        return errand != null && errand.tag().equals(tag) && errandWait >= 0;
    }

    public void startErrand(Errand e) {
        errand = e;
        errandStop = 0;
        errandWait = -1;
        errandTicks = 0;
        errandRepath = 0;
        clearRestriction();
        getNavigation().stop();
    }

    public void endErrand() {
        errand = null;
        errandWait = -1;
        if (gesture() != GuardianGesture.NONE) {
            entityData.set(GESTURE, (byte) GuardianGesture.NONE.ordinal());
        }
        updateRestriction();
    }

    /** 只供 GameTest：立刻到达当前目的地。 */
    public void arriveForTest() {
        if (errand != null && errandStop < errand.stops().size()) {
            BlockPos t = errand.stops().get(errandStop);
            teleportTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
        }
    }

    private boolean near(BlockPos t) {
        double dx = getX() - (t.getX() + 0.5);
        double dz = getZ() - (t.getZ() + 0.5);
        return dx * dx + dz * dz <= 2.5 * 2.5 && Math.abs(getY() - t.getY()) <= 3;
    }

    private void tickErrand() {
        Errand e = errand;
        if (e == null) {
            return;
        }
        if (errandStop >= e.stops().size()) {
            endErrand();
            return;
        }
        BlockPos t = e.stops().get(errandStop);
        boolean last = errandStop == e.stops().size() - 1;
        if (errandWait < 0) {
            if (near(t)) {
                getNavigation().stop();
                int ticks = e.ticks();
                if (last && e.until() > 0) {
                    ticks = (int) Math.max(ticks, e.until() - level().getGameTime());
                }
                errandWait = ticks;
                // 循环手势（坐诊、听橡）一直做到走；一次性手势（奠酒）做完就站着待到时候
                playGesture(e.gesture(), e.gesture().loops() ? ticks : e.ticks());
                if (e.onArrive() != null) {
                    e.onArrive().accept(this, t);
                }
                return;
            }
            // 走不到（20 秒没到）：附近没有玩家就直接到位，有玩家在看就跳过这一处
            if (++errandTicks > 400) {
                errandTicks = 0;
                if (level().getNearestPlayer(this, 16) == null) {
                    teleportTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
                } else {
                    errandStop++;
                }
                return;
            }
            if (--errandRepath <= 0 || getNavigation().isDone()) {
                errandRepath = 40;
                getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 1.2);
            }
            return;
        }
        getLookControl().setLookAt(t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5);
        if (--errandWait <= 0) {
            errandStop++;
            errandWait = -1;
            errandTicks = 0;
            if (errandStop >= e.stops().size()) {
                endErrand();
            }
        }
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
        tag.putString("Civ", civ.id());
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
        // T9 的实体没有 Civ：按实体类型
        civ = Civilization.byId(tag.getString("Civ")).orElse(GuardianTypes.civOf(getType()));
    }

    public GlobalPos globalHome() {
        return GlobalPos.of(level().dimension(), home != null ? home : blockPosition());
    }

    /** 差事：走过去、做手势、回居所。 */
    private final class ErrandGoal extends Goal {
        ErrandGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return errand != null;
        }

        @Override
        public boolean canContinueToUse() {
            return errand != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            tickErrand();
        }
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
            viewer = TradeService.nearestViewer(GuardianEntity.this);
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
