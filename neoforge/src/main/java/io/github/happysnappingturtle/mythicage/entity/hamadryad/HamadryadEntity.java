package io.github.happysnappingturtle.mythicage.entity.hamadryad;

import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.summon.Disguised;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import io.github.happysnappingturtle.mythicage.wild.greek.GreekWild;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 树仙女（01_希腊 W5、T15 设计文档 §6、制作卡 10）：绑定一棵本命橡木（存树根坐标）的中立宁芙。
 * <ul>
 *   <li>白天少女形态，在本命树 12 格内漫步；夜里走到树旁化树（碰撞像原木、能站上去），天亮现形。实现 {@link Disguised}：天马天眼会标出她。</li>
 *   <li>从不主动攻击。本命树 16 格内有玩家砍原木：第 1、2 次树液 + 叹息警告；第 3 次现形，对他唤根刺（裂纹 16 刻后出刺 6 点），
 *       之后每 60 刻一次，持续 60 秒或他离开本命树 24 格。直接打她也会这样还手。</li>
 *   <li>树根那格不再是本命橡木：她随之死去；是玩家毁的（40 刻内的破坏记录）就给他挖掘疲劳 I 2400 刻，爆炸等不给。</li>
 *   <li>对本命橡木用骨粉 3 次 → 赠宁芙之露；每只每 3 个 MC 日一次。</li>
 * </ul>
 */
public class HamadryadEntity extends PathfinderMob implements GeoEntity, MythVariantHolder, Disguised {
    public static final int WARNINGS = 2;
    public static final double PROTECT_RADIUS = 16;
    public static final double CHASE_RADIUS = 24;
    public static final int TRANSFORM_TICKS = 20;
    public static final int FATIGUE_TICKS = 2400;
    public static final int DEW_COOLDOWN = 72000;
    public static final int DEW_MEALS = 3;

    public enum Form { MAIDEN, TREE }

    public enum Act { NONE, TO_TREE, TO_MAIDEN, WARN, CAST, BLESS }

    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(HamadryadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACT = SynchedEntityData.defineId(HamadryadEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation TREE = RawAnimation.begin().thenLoop("tree");
    private static final RawAnimation TO_TREE = RawAnimation.begin().thenPlayAndHold("to_tree");
    private static final RawAnimation TO_MAIDEN = RawAnimation.begin().thenPlayAndHold("to_maiden");
    private static final RawAnimation WARN_TREE = RawAnimation.begin().thenPlayAndHold("warn_tree");
    private static final RawAnimation SIGH = RawAnimation.begin().thenPlayAndHold("sigh");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlayAndHold("cast");
    private static final RawAnimation BLESS = RawAnimation.begin().thenPlayAndHold("bless");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private BlockPos treeRoot;
    private long actEnd;
    private final Map<UUID, Integer> strikes = new HashMap<>();
    @Nullable
    private UUID angryAt;
    private long angryUntil;
    private long nextCastAt;
    @Nullable
    private UUID rootBreaker;
    /** 同一名玩家的引用（GameTest 的假玩家不在世界的玩家列表里，按 UUID 找不到）。 */
    private java.lang.ref.WeakReference<Player> rootBreakerRef = new java.lang.ref.WeakReference<>(null);
    private long rootBrokenAt = -100;
    private int dewMeals;
    private long dewReadyAt;
    private long revealedUntil;

    private float spikeDamage = 6;
    private int castCooldown = 60;
    private int angerTicks = 1200;

    public HamadryadEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, Form.MAIDEN.ordinal());
        builder.define(ACT, Act.NONE.ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SummonUnits.Stats s = SummonUnits.statsFile("greek", "wild_hamadryad");
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", 24));
        setHealth(getMaxHealth());
        spikeDamage = (float) s.specialDouble("spike_damage", 6);
        castCooldown = s.specialInt("cast_cooldown", 60);
        angerTicks = s.specialInt("anger_ticks", 1200);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** 绑定本命树（树根那一格的坐标），活动范围限制在它 12 格内。 */
    public void bindTree(BlockPos root) {
        treeRoot = root.immutable();
        restrictTo(treeRoot, 12);
        setPersistenceRequired();
    }

    // ───────────── 状态 ─────────────

    @Nullable
    public BlockPos treeRoot() {
        return treeRoot;
    }

    public Form form() {
        return Form.values()[Math.floorMod(entityData.get(FORM), Form.values().length)];
    }

    public Act act() {
        return Act.values()[Math.floorMod(entityData.get(ACT), Act.values().length)];
    }

    public boolean angry() {
        return angryAt != null && level().getGameTime() < angryUntil;
    }

    public int strikes(Player p) {
        return strikes.getOrDefault(p.getUUID(), 0);
    }

    public long dewReadyAt() {
        return dewReadyAt;
    }

    public boolean revealedTo() {
        return level().getGameTime() < revealedUntil;
    }

    private void setForm(Form f) {
        entityData.set(FORM, f.ordinal());
        refreshDimensions();
    }

    private void setAct(Act a, long end) {
        entityData.set(ACT, a.ordinal());
        actEnd = end;
    }

    public static boolean night(Level level) {
        long t = level.getDayTime() % 24000L;
        return t >= 13000 && t < 23000;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return form() == Form.TREE ? EntityDimensions.fixed(0.7F, 2.9F) : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key)) {
            refreshDimensions();
        }
    }

    /** 化树时像一截原木：挡路、能站上去，推不动。 */
    @Override
    public boolean canBeCollidedWith() {
        return form() == Form.TREE && isAlive();
    }

    @Override
    public boolean isPushable() {
        return form() != Form.TREE && super.isPushable();
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
    public String variant() {
        return "";
    }

    @Override
    public float variantScale() {
        return 1.0F;
    }

    @Override
    public void reveal(ServerPlayer viewer, int ticks) {
        revealedUntil = level().getGameTime() + ticks;
    }

    // ───────────── 护树（HamadryadEvents 调用） ─────────────

    /** 玩家在本命树附近砍了一块原木（不是树根那格）。返回这是第几次。 */
    public int onChop(Player p) {
        int n = strikes.merge(p.getUUID(), 1, Integer::sum);
        long now = level().getGameTime();
        if (n <= WARNINGS) {
            warn(p);
        } else {
            anger(p, now);
        }
        return n;
    }

    private void warn(Player p) {
        if (level() instanceof ServerLevel sl && treeRoot != null) {
            for (int y = 1; y <= 5; y++) {
                sl.sendParticles(ParticleTypes.DRIPPING_HONEY, treeRoot.getX() + 0.5, treeRoot.getY() + y, treeRoot.getZ() + 0.5,
                        3, 0.45, 0.3, 0.45, 0);
            }
            sl.playSound(null, treeRoot, SoundEvents.SOUL_ESCAPE.value(), net.minecraft.sounds.SoundSource.NEUTRAL, 2.0F, 0.6F);
        }
        p.displayClientMessage(Component.translatable("mythic_age.hamadryad.warn"), true);
        setAct(Act.WARN, level().getGameTime() + (form() == Form.TREE ? 12 : 24));
    }

    private void anger(LivingEntity who, long now) {
        angryAt = who.getUUID();
        angryUntil = now + angerTicks;
        setTarget(who);
        if (form() == Form.TREE) {
            setForm(Form.MAIDEN);
            setAct(Act.TO_MAIDEN, now + TRANSFORM_TICKS);
            nextCastAt = now + TRANSFORM_TICKS;
        } else {
            nextCastAt = Math.min(nextCastAt, now + 4);
        }
        if (who instanceof Player p) {
            p.displayClientMessage(Component.translatable("mythic_age.hamadryad.angry"), true);
        }
    }

    /** 有玩家亲手毁了树根那格（BreakEvent），40 刻内树根检查时归到他。 */
    public void noteRootBroken(Player p) {
        rootBreaker = p.getUUID();
        rootBreakerRef = new java.lang.ref.WeakReference<>(p);
        rootBrokenAt = level().getGameTime();
    }

    /** 对本命橡木用了一次骨粉。返回 true = 计数（或已赠露）；false = 还在冷却。 */
    public boolean onBoneMeal(Player p) {
        long now = level().getGameTime();
        if (now < dewReadyAt) {
            p.displayClientMessage(Component.translatable("mythic_age.hamadryad.dew_resting"), true);
            return false;
        }
        dewMeals++;
        if (level() instanceof ServerLevel sl && treeRoot != null) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, treeRoot.getX() + 0.5, treeRoot.getY() + 1.5, treeRoot.getZ() + 0.5,
                    8, 0.6, 1.0, 0.6, 0);
        }
        if (dewMeals >= DEW_MEALS) {
            dewMeals = 0;
            dewReadyAt = now + DEW_COOLDOWN;
            ItemStack dew = new ItemStack(GreekWild.NYMPH_DEW.get());
            if (!p.getInventory().add(dew)) {
                level().addFreshEntity(new ItemEntity(level(), p.getX(), p.getY(), p.getZ(), dew));
            }
            p.displayClientMessage(Component.translatable("mythic_age.hamadryad.dew_given"), true);
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.2F);
            if (form() == Form.MAIDEN) {
                setAct(Act.BLESS, now + 20);
            }
        }
        return true;
    }

    // ───────────── 伤害 ─────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && !isDeadOrDying() && source.getEntity() instanceof LivingEntity who
                && !(who instanceof HamadryadEntity)) {
            anger(who, level().getGameTime());
        }
        return hurt;
    }

    // ───────────── AI ─────────────

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long now = level().getGameTime();
        if (treeRoot != null && tickCount % 10 == 0 && level().isLoaded(treeRoot)
                && !level().getBlockState(treeRoot).is(GreekWild.HAMADRYAD_OAK_LOG.get())) {
            treeDestroyed(now);
            return;
        }
        if (act() != Act.NONE && now >= actEnd) {
            setAct(Act.NONE, 0);
        }
        // 化树或在做动作时站定（不能用 isImmobile：那会让原版连 AI 都不跑）
        if (form() == Form.TREE || act() != Act.NONE) {
            getNavigation().stop();
            setZza(0);
            setXxa(0);
        }
        LivingEntity t = getTarget();
        if (angry()) {
            if (t == null || !t.isAlive() || !t.getUUID().equals(angryAt) || t instanceof Player p && (p.isCreative() || p.isSpectator())
                    || (treeRoot != null && t.position().distanceToSqr(Vec3.atCenterOf(treeRoot)) > CHASE_RADIUS * CHASE_RADIUS)) {
                calm();
            } else if (act() == Act.NONE && now >= nextCastAt) {
                getNavigation().stop();
                getLookControl().setLookAt(t);
                setAct(Act.CAST, now + 16);
                RootSpikeEntity spike = GreekWild.ROOT_SPIKE.get().create(level());
                if (spike != null) {
                    spike.setup(this, spikeDamage);
                    spike.moveTo(t.getX(), Math.floor(t.getY()), t.getZ(), 0, 0);
                    level().addFreshEntity(spike);
                }
                nextCastAt = now + castCooldown;
            }
            return;
        } else if (angryAt != null) {
            calm();
        }
        // 作息：夜里走到树旁化树，天亮现形
        boolean night = night(level());
        if (form() == Form.MAIDEN && night && act() == Act.NONE) {
            Vec3 spot = treeSide();
            if (spot == null || position().distanceToSqr(spot) < 1.2 * 1.2) {
                getNavigation().stop();
                setForm(Form.TREE);
                setAct(Act.TO_TREE, now + TRANSFORM_TICKS);
                playSound(SoundEvents.AZALEA_LEAVES_PLACE, 1.0F, 0.7F);
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(spot.x, spot.y, spot.z, 0.8);
            }
        } else if (form() == Form.TREE && !night && act() == Act.NONE) {
            setForm(Form.MAIDEN);
            setAct(Act.TO_MAIDEN, now + TRANSFORM_TICKS);
            playSound(SoundEvents.AZALEA_LEAVES_BREAK, 1.0F, 0.9F);
        }
    }

    /** 树旁的落脚处：本命树南侧两格（找不到树就原地化树）。 */
    @Nullable
    private Vec3 treeSide() {
        return treeRoot == null ? null : Vec3.atBottomCenterOf(treeRoot.south(2));
    }

    private void calm() {
        angryAt = null;
        angryUntil = 0;
        setTarget(null);
        strikes.clear();
    }

    private void treeDestroyed(long now) {
        Player p = rootBreaker == null || now - rootBrokenAt > 40 ? null
                : rootBreakerRef.get() != null ? rootBreakerRef.get() : level().getPlayerByUUID(rootBreaker);
        if (p != null) {
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, FATIGUE_TICKS, 0));
            p.displayClientMessage(Component.translatable("mythic_age.hamadryad.cursed"), true);
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, getX(), getY() + 1, getZ(), 30, 0.5, 1.0, 0.5, 0);
        }
        playSound(SoundEvents.SOUL_ESCAPE.value(), 2.0F, 0.5F);
        kill();
    }

    /** GameTest 用：把她当作树被毁时的死亡判定立刻跑一次（不等 10 刻一次的检查）。 */
    public void checkTreeNow() {
        if (treeRoot != null && !level().getBlockState(treeRoot).is(GreekWild.HAMADRYAD_OAK_LOG.get())) {
            treeDestroyed(level().getGameTime());
        }
    }

    // ───────────── 存档 ─────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (treeRoot != null) {
            tag.put("TreeRoot", NbtUtils.writeBlockPos(treeRoot));
        }
        tag.putInt("Form", form().ordinal());
        tag.putInt("DewMeals", dewMeals);
        tag.putLong("DewReadyAt", dewReadyAt);
        tag.putFloat("SpikeDamage", spikeDamage);
        tag.putInt("CastCooldown", castCooldown);
        tag.putInt("AngerTicks", angerTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "TreeRoot").ifPresent(this::bindTree);
        setForm(Form.values()[Math.floorMod(tag.getInt("Form"), Form.values().length)]);
        dewMeals = tag.getInt("DewMeals");
        dewReadyAt = tag.getLong("DewReadyAt");
        if (tag.contains("SpikeDamage")) {
            spikeDamage = tag.getFloat("SpikeDamage");
            castCooldown = tag.getInt("CastCooldown");
            angerTicks = tag.getInt("AngerTicks");
        }
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> switch (act()) {
            case TO_TREE -> state.setAndContinue(TO_TREE);
            case TO_MAIDEN -> state.setAndContinue(TO_MAIDEN);
            case WARN -> state.setAndContinue(form() == Form.TREE ? WARN_TREE : SIGH);
            case CAST -> state.setAndContinue(CAST);
            case BLESS -> state.setAndContinue(BLESS);
            case NONE -> state.setAndContinue(form() == Form.TREE ? TREE : state.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
