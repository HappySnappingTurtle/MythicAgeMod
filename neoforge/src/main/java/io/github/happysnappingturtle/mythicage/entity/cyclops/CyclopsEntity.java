package io.github.happysnappingturtle.mythicage.entity.cyclops;

import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumMap;
import java.util.Map;

/**
 * 独眼巨人（RIG-CYCLOPS，T25a）：召唤版（阿瑞斯，阶段 ④）与精英「独眼巨人·牧人」（阶段 ⑥）共用的外壳——
 * 模型 {@code geo/entity/cyclops.geo.json}、服务端写入的动画状态、按步速调整行走动画。
 * 碰撞 1.8 × 3.8（设计文档 §9）。
 */
public abstract class CyclopsEntity extends PathfinderMob implements GeoEntity, MythVariantHolder,
        io.github.happysnappingturtle.mythicage.entity.BoneVisibility {
    public static final double WALK_THRESHOLD = 0.02;
    /** 行走动画每循环前进的距离（格）：大腿摆幅 ±22°、腿长约 25u，两步约 2.4 格。 */
    public static final double WALK_CYCLE_BLOCKS = 2.4;

    private static final EntityDataAccessor<Byte> ANIM_STATE = SynchedEntityData.defineId(CyclopsEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANIM_START = SynchedEntityData.defineId(CyclopsEntity.class, EntityDataSerializers.INT);

    private static final Map<MythAnimState, RawAnimation> ANIMATIONS = new EnumMap<>(MythAnimState.class);

    static {
        ANIMATIONS.put(MythAnimState.IDLE, RawAnimation.begin().thenLoop("idle"));
        ANIMATIONS.put(MythAnimState.WALK, RawAnimation.begin().thenLoop("walk"));
        for (MythAnimState s : new MythAnimState[] {MythAnimState.ATTACK_SWING, MythAnimState.GRAB, MythAnimState.HURL,
                MythAnimState.LIFT_ROCK, MythAnimState.THROW_ROCK, MythAnimState.CLUB_SMASH}) {
            ANIMATIONS.put(s, RawAnimation.begin().thenPlayAndHold(s.animation()));
        }
        for (MythAnimState s : new MythAnimState[] {MythAnimState.DRINK, MythAnimState.DRUNK, MythAnimState.SLEEP}) {
            ANIMATIONS.put(s, RawAnimation.begin().thenLoop(s.animation()));
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int animUntil;
    private int clientSeenAnimStart = -1;

    protected CyclopsEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 110.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM_STATE, (byte) MythAnimState.AUTO.ordinal());
        builder.define(ANIM_START, 0);
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
    public boolean headLookLocked() {
        MythAnimState s = animState();
        return s == MythAnimState.GRAB || s == MythAnimState.HURL || s == MythAnimState.DRINK || s == MythAnimState.DRUNK
                || s == MythAnimState.SLEEP;
    }

    /** 投掷巨石只在举石、投石出手前显示（模型里挂在左手上）。 */
    @Override
    public boolean showBone(String bone) {
        if (!"boulder".equals(bone)) {
            return false;
        }
        MythAnimState s = animState();
        return s == MythAnimState.LIFT_ROCK || s == MythAnimState.THROW_ROCK && animAge() < 4;
    }

    // ───────────── 动画状态 ─────────────

    public MythAnimState animState() {
        return MythAnimState.byOrdinal(entityData.get(ANIM_STATE));
    }

    /** 当前动画状态开始以来的刻数（两端都可用）。 */
    public int animAge() {
        return tickCount - entityData.get(ANIM_START);
    }

    public void setAnimState(MythAnimState state) {
        entityData.set(ANIM_STATE, (byte) state.ordinal());
        entityData.set(ANIM_START, tickCount);
        animUntil = 0;
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setAnimState(MythAnimState.AUTO);
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<CyclopsEntity> main = new AnimationController<>(this, "main", 4, state -> {
            MythAnimState s = animState();
            int start = entityData.get(ANIM_START);
            if (start != clientSeenAnimStart) {
                clientSeenAnimStart = start;
                if (s != MythAnimState.AUTO) {
                    state.getController().forceAnimationReset();
                }
            }
            if (s == MythAnimState.AUTO || !ANIMATIONS.containsKey(s)) {
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

    private double animationSpeed() {
        MythAnimState s = animState();
        if ((s == MythAnimState.AUTO || !ANIMATIONS.containsKey(s)) && horizontalSpeed() > WALK_THRESHOLD) {
            return Mth.clamp(horizontalSpeed() * 20.0 / WALK_CYCLE_BLOCKS, 0.4, 3.0);
        }
        return 1.0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
