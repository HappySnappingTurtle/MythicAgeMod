package io.github.happysnappingturtle.mythicage.entity.centaur;

import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import io.github.happysnappingturtle.mythicage.summon.greek.CentaurPineEntity;
import io.github.happysnappingturtle.mythicage.summon.greek.GreekSummons;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * 半人马的共同部分（T14 §6.3、T15 §2；制作卡 06）：模型与动画、拉弓射箭、近身踢击、风筝、动作时间线、上身转向。
 * 召唤版（{@code SummonCentaurEntity}）多归属与特殊攻击「掷松」；野外版（{@code WildCentaurEntity}）是中立群居。
 * <ul>
 *   <li>风筝：与目标保持 keepMin–keepMax 格，近于 keepMin − 2 就后撤，远于 keepMax + 2 就追近；</li>
 *   <li>射箭：拉弓 drawTicks 刻后放箭；近身：目标进入 3 格，前身抬起 kickWindup 刻后前蹄踢击；</li>
 *   <li>掷松（只有 {@link #hurlEnabled()} 的子类）：蓄力后掷出松树干，穿过多个目标。</li>
 * </ul>
 * 上身转向由客户端模型按视线偏转腰部（{@code CentaurGeoModel}）。
 */
public abstract class AbstractCentaurEntity extends PathfinderMob implements GeoEntity, MythVariantHolder {
    /** 服务端写入的一次性动画；AUTO 由客户端按速度在站立 / 慢走 / 奔跑间切换。 */
    public enum Anim { AUTO, DRAW, SHOOT, HURL, KICK }

    private static final EntityDataAccessor<Byte> ANIM = SynchedEntityData.defineId(AbstractCentaurEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANIM_START = SynchedEntityData.defineId(AbstractCentaurEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation REST = RawAnimation.begin().thenLoop("rest");
    private static final Map<Anim, RawAnimation> ONCE = new EnumMap<>(Map.of(
            Anim.DRAW, RawAnimation.begin().thenPlayAndHold("draw"),
            Anim.SHOOT, RawAnimation.begin().thenPlayAndHold("shoot"),
            Anim.HURL, RawAnimation.begin().thenPlayAndHold("hurl"),
            Anim.KICK, RawAnimation.begin().thenPlayAndHold("kick")));
    private static final double WALK_THRESHOLD = 0.02;
    private static final double RUN_THRESHOLD = 0.2;
    /** 步行 / 奔跑动画每循环前进的距离（格）。 */
    private static final double WALK_CYCLE_BLOCKS = 1.8;
    private static final double RUN_CYCLE_BLOCKS = 3.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int clientSeenAnimStart = -1;

    // 数值（entity_stats/greek/summon_centaur.json）
    protected double arrowDamage = 6;
    protected int drawTicks = 16;
    protected int shotCooldown = 20;
    protected double kickDamage = 7;
    protected int kickWindup = 12;
    protected int kickCooldown = 40;
    protected double pineDamage = 12;
    protected int pineWindup = 20;
    protected int pineCooldown = 300;
    protected int pinePierce = 3;
    protected double keepMin = 10;
    protected double keepMax = 14;

    /** 正在进行的动作：0 无、1 拉弓、2 放箭收势、3 掷松、4 踢击。 */
    protected int action;
    private int actionTimer;
    @Nullable
    private LivingEntity actionTarget;
    private long nextShot;
    private long nextKick;
    protected long pineReadyAt;
    /** 已掷出的松树干数（GameTest 读）。 */
    private int pinesThrown;
    private int arrowsShot;

    protected AbstractCentaurEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createCentaurAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 70.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIM, (byte) 0);
        builder.define(ANIM_START, 0);
    }

    /** 共同的目标：漂浮、动作锁、交战（风筝 / 射箭 / 踢击 / 掷松）。子类先调这里再加自己的。 */
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ActionLock());
        goalSelector.addGoal(2, new Skirmish());
    }

    /** 能不能打这个目标（召唤版按主人的目标矩阵；野外版按仇恨）。 */
    protected abstract boolean mayAttack(LivingEntity target);

    /** 会不会掷松（召唤版的特殊攻击）。 */
    protected boolean hurlEnabled() {
        return false;
    }

    /** 正在营火边休息（野外版夜里）：播 rest 动画。 */
    protected boolean resting() {
        return false;
    }

    /** 按数值文件设置属性与时间线参数（{@code entity_stats/<civ>/*_centaur.json}）。 */
    protected void applyCentaurStats(SummonUnits.Stats s, double defaultHealth, double defaultSpeed) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", defaultHealth));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(s.attr("movement_speed", defaultSpeed));
        setHealth(getMaxHealth());
        arrowDamage = s.specialDouble("arrow_damage", arrowDamage);
        drawTicks = s.specialInt("draw_ticks", drawTicks);
        shotCooldown = s.specialInt("shot_cooldown", shotCooldown);
        kickDamage = s.specialDouble("kick_damage", kickDamage);
        kickWindup = s.specialInt("kick_windup", kickWindup);
        kickCooldown = s.specialInt("kick_cooldown", kickCooldown);
        pineDamage = s.specialDouble("pine_damage", pineDamage);
        pineWindup = s.specialInt("pine_windup", pineWindup);
        pineCooldown = s.specialInt("pine_cooldown", pineCooldown);
        pinePierce = s.specialInt("pine_pierce", pinePierce);
        keepMin = s.specialDouble("keep_min", keepMin);
        keepMax = s.specialDouble("keep_max", keepMax);
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

    /** 正在拉弓 / 掷松 / 踢击。 */
    public boolean busy() {
        return action != 0;
    }

    public Anim anim() {
        int i = entityData.get(ANIM);
        return i >= 0 && i < Anim.values().length ? Anim.values()[i] : Anim.AUTO;
    }

    private void setAnim(Anim a) {
        entityData.set(ANIM, (byte) a.ordinal());
        entityData.set(ANIM_START, tickCount);
    }

    public long pineReadyAt() {
        return pineReadyAt;
    }

    public int pinesThrown() {
        return pinesThrown;
    }

    public int arrowsShot() {
        return arrowsShot;
    }

    // ───────────── 动作时间线（服务端） ─────────────

    private void startAction(int a, int ticks, LivingEntity target, Anim anim) {
        action = a;
        actionTimer = ticks;
        actionTarget = target;
        setAnim(anim);
    }

    private void endAction() {
        action = 0;
        actionTarget = null;
        setAnim(Anim.AUTO);
    }

    protected void startHurl(LivingEntity target) {
        getNavigation().stop();
        pineReadyAt = level().getGameTime() + pineCooldown;
        startAction(3, pineWindup + 8, target, Anim.HURL);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (action == 0) {
            return;
        }
        LivingEntity t = actionTarget;
        if (t != null && t.isAlive() && t.level() == level()) {
            getLookControl().setLookAt(t, 30.0F, 30.0F);
        }
        actionTimer--;
        switch (action) {
            case 1 -> { // 拉弓：拉满时放箭
                if (t == null || !t.isAlive() || !hasLineOfSight(t)) {
                    endAction();
                } else if (actionTimer <= 0) {
                    shootArrow(t);
                    startAction(2, 10, t, Anim.SHOOT);
                }
            }
            case 2 -> { // 放箭收势
                if (actionTimer <= 0) {
                    nextShot = level().getGameTime() + shotCooldown;
                    endAction();
                }
            }
            case 3 -> { // 掷松：蓄力到点出手
                if (actionTimer == 8 && t != null && t.isAlive()) {
                    throwPine(t);
                }
                if (actionTimer <= 0) {
                    endAction();
                }
            }
            case 4 -> { // 踢击：抬身预警后判定
                if (actionTimer == 18 - kickWindup && t != null && t.isAlive() && distanceToSqr(t) < 3.5 * 3.5) {
                    kick(t);
                }
                if (actionTimer <= 0) {
                    nextKick = level().getGameTime() + kickCooldown;
                    endAction();
                }
            }
            default -> endAction();
        }
    }

    private void shootArrow(LivingEntity t) {
        Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
        double speed = 1.6;
        arrow.setBaseDamage(arrowDamage / speed);
        arrow.setCritArrow(false);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = t.getX() - getX();
        double dy = t.getY(0.3333) - arrow.getY();
        double dz = t.getZ() - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + flat * 0.15, dz, (float) speed, 2.0F);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 0.8F + random.nextFloat() * 0.2F);
        level().addFreshEntity(arrow);
        arrowsShot++;
    }

    private void throwPine(LivingEntity t) {
        CentaurPineEntity pine = new CentaurPineEntity(GreekSummons.CENTAUR_PINE.get(), level());
        pine.setOwner(this);
        pine.configure(pineDamage, pinePierce);
        Vec3 from = new Vec3(getX(), getY() + 2.4, getZ());
        pine.moveTo(from.x, from.y, from.z, getYRot(), 0);
        Vec3 dir = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(from).normalize();
        pine.setDeltaMovement(dir.scale(CentaurPineEntity.SPEED));
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        level().addFreshEntity(pine);
        pinesThrown++;
    }

    private void kick(LivingEntity t) {
        t.hurt(damageSources().mobAttack(this), (float) kickDamage);
        Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
        d = d.lengthSqr() > 1.0E-4 ? d.normalize() : getLookAngle();
        t.knockback(1.0, -d.x, -d.z);
        playSound(SoundEvents.HORSE_ANGRY, 1.0F, 0.9F);
    }

    /** 动作进行中占住移动与视线（掷松、踢击站定；拉弓时也站定，动画才清楚）。 */
    private final class ActionLock extends Goal {
        ActionLock() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return action != 0;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** 有目标时：近身就踢，否则保持 10–14 格边退边射，冷却好且目标合适时掷松。 */
    private final class Skirmish extends Goal {
        private int repath;

        Skirmish() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return action == 0 && t != null && t.isAlive() && mayAttack(t);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            // 目标选择器隔一刻才复查 canContinueToUse：刚开始的动作（如掷松）不能在下一刻被这里改成拉弓
            if (t == null || action != 0) {
                return;
            }
            getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = Math.sqrt(distanceToSqr(t));
            long now = level().getGameTime();
            boolean sees = hasLineOfSight(t);
            if (d < 3.0 && now >= nextKick) {
                getNavigation().stop();
                startAction(4, 18, t, Anim.KICK);
                return;
            }
            if (hurlEnabled() && sees && now >= pineReadyAt && d >= 6 && d <= 20) {
                startHurl(t);
                return;
            }
            if (d < keepMin - 2) {
                // 后撤：沿离开目标的方向退 6 格（撞墙就侧移）
                if (--repath <= 0) {
                    repath = 10;
                    Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
                    away = away.lengthSqr() > 1.0E-4 ? away.normalize() : getLookAngle().reverse();
                    Vec3 side = new Vec3(-away.z, 0, away.x).scale(random.nextBoolean() ? 3 : -3);
                    Vec3 to = position().add(away.scale(6)).add(horizontalCollision ? side : Vec3.ZERO);
                    getNavigation().moveTo(to.x, to.y, to.z, 1.35);
                }
            } else if (d > keepMax + 2 || !sees) {
                if (--repath <= 0) {
                    repath = 10;
                    getNavigation().moveTo(t, 1.15);
                }
            } else {
                getNavigation().stop();
                if (sees && now >= nextShot) {
                    startAction(1, drawTicks, t, Anim.DRAW);
                }
            }
        }
    }

    /** 被谁打就打谁（只打合法目标）。 */
    protected final class Retaliate extends TargetGoal {
        private int seen;

        public Retaliate() {
            super(AbstractCentaurEntity.this, false);
        }

        @Override
        public boolean canUse() {
            LivingEntity by = getLastHurtByMob();
            if (by == null || getLastHurtByMobTimestamp() == seen || !mayAttack(by)) {
                return false;
            }
            seen = getLastHurtByMobTimestamp();
            targetMob = by;
            return true;
        }

        @Override
        public void start() {
            mob.setTarget(targetMob);
            super.start();
        }
    }

    // ───────────── 存档 ─────────────

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("PineReadyAt", pineReadyAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pineReadyAt = tag.getLong("PineReadyAt");
        action = 0;
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<AbstractCentaurEntity> main = new AnimationController<>(this, "main", 4, state -> {
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
            if (resting() && h < WALK_THRESHOLD) {
                return state.setAndContinue(REST);
            }
            if (h > RUN_THRESHOLD) {
                return state.setAndContinue(RUN);
            }
            return state.setAndContinue(h > WALK_THRESHOLD ? WALK : IDLE);
        });
        main.setAnimationSpeedHandler(e -> gaitSpeed());
        controllers.add(main);
    }

    private double horizontalSpeed() {
        double dx = getX() - xo;
        double dz = getZ() - zo;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** 步行 / 奔跑按实际步速调整播放速度，避免滑步；其他动画按原速。 */
    private double gaitSpeed() {
        if (anim() != Anim.AUTO) {
            return 1.0;
        }
        double h = horizontalSpeed();
        if (h > RUN_THRESHOLD) {
            return Mth.clamp(h * 20.0 / RUN_CYCLE_BLOCKS * 0.5, 0.6, 2.5);
        }
        if (h > WALK_THRESHOLD) {
            return Mth.clamp(h * 20.0 / WALK_CYCLE_BLOCKS * 0.8, 0.4, 3.0);
        }
        return 1.0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
