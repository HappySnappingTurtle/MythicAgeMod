package io.github.happysnappingturtle.mythicage.summon.greek;

import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import io.github.happysnappingturtle.mythicage.summon.SummonGoals;
import io.github.happysnappingturtle.mythicage.summon.SummonLink;
import io.github.happysnappingturtle.mythicage.summon.SummonTargets;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
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
 * 召唤版半人马（T14 设计文档 §6.3、§11.5；制作卡 06）：机动远程。
 * <ul>
 *   <li>风筝：与目标保持 10–14 格，近于 8 格就后撤，远于 16 格就追近；</li>
 *   <li>射箭：拉弓 16 刻（0.8 秒）后放箭，伤害 6；</li>
 *   <li>近身：目标进入 3 格，前身抬起 12 刻后前蹄踢击；</li>
 *   <li>特殊攻击「掷松」：蓄力 20 刻后掷出连根拔起的松树干，伤害 ×2、穿过 3 个目标，冷却 15 秒。</li>
 * </ul>
 * 上身转向由客户端模型按视线偏转腰部（{@code CentaurGeoModel}）。
 */
public class SummonCentaurEntity extends PathfinderMob implements GeoEntity, MythSummon, MythVariantHolder {
    /** 服务端写入的一次性动画；AUTO 由客户端按速度在站立 / 慢走 / 奔跑间切换。 */
    public enum Anim { AUTO, DRAW, SHOOT, HURL, KICK }

    private static final EntityDataAccessor<Byte> ANIM = SynchedEntityData.defineId(SummonCentaurEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANIM_START = SynchedEntityData.defineId(SummonCentaurEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
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
    private final SummonLink link = new SummonLink();
    private int clientSeenAnimStart = -1;

    // 数值（entity_stats/greek/summon_centaur.json）
    private double arrowDamage = 6;
    private int drawTicks = 16;
    private int shotCooldown = 20;
    private double kickDamage = 7;
    private int kickWindup = 12;
    private int kickCooldown = 40;
    private double pineDamage = 12;
    private int pineWindup = 20;
    private int pineCooldown = 300;
    private int pinePierce = 3;
    private double keepMin = 10;
    private double keepMax = 14;

    /** 正在进行的动作：0 无、1 拉弓、2 放箭收势、3 掷松、4 踢击。 */
    private int action;
    private int actionTimer;
    @Nullable
    private LivingEntity actionTarget;
    private long nextShot;
    private long nextKick;
    private long pineReadyAt;
    /** 已掷出的松树干数（GameTest 读）。 */
    private int pinesThrown;
    private int arrowsShot;

    public SummonCentaurEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createSummonAttributes() {
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

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ActionLock());
        goalSelector.addGoal(2, new Skirmish());
        goalSelector.addGoal(3, new SummonGoals.Follow(this, 1.15));
        goalSelector.addGoal(3, new SummonGoals.Guard(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new SummonGoals.Commanded(this));
        targetSelector.addGoal(1, new SummonGoals.DefendOwner(this));
        targetSelector.addGoal(2, new Retaliate());
        targetSelector.addGoal(3, new SummonGoals.NearestEnemy(this));
    }

    // ───────────── MythSummon ─────────────

    @Override
    public SummonLink link() {
        return link;
    }

    @Override
    public boolean combatUnit() {
        return true;
    }

    @Override
    public void applyStats(SummonUnits.Stats s) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(s.attr("max_health", 70));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(s.attr("movement_speed", 0.32));
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

    /** 传令杖潜行右键：立即掷松（冷却中、正在出手或目标太远时拒绝）。 */
    @Override
    public boolean requestSpecial(LivingEntity target) {
        if (level().getGameTime() < pineReadyAt || action == 3 || distanceToSqr(target) > 24 * 24) {
            return false;
        }
        startHurl(target);
        return true;
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

    public boolean busy() {
        return action != 0;
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

    private void startHurl(LivingEntity target) {
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
            return action == 0 && t != null && t.isAlive() && SummonTargets.canAttack(SummonCentaurEntity.this, t);
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
            if (sees && now >= pineReadyAt && d >= 6 && d <= 20) {
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
    private final class Retaliate extends TargetGoal {
        private int seen;

        Retaliate() {
            super(SummonCentaurEntity.this, false);
        }

        @Override
        public boolean canUse() {
            LivingEntity by = getLastHurtByMob();
            if (by == null || getLastHurtByMobTimestamp() == seen || !SummonTargets.canAttack(mob, by)) {
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

    // ───────────── 阵营、存档 ─────────────

    @Override
    public boolean isAlliedTo(Entity other) {
        if (link.owner != null && (other.getUUID().equals(link.owner)
                || (other instanceof MythSummon s && link.owner.equals(s.link().owner)))) {
            return true;
        }
        Player owner = link.owner == null ? null : level().getPlayerByUUID(link.owner);
        return owner != null && owner.isAlliedTo(other) || super.isAlliedTo(other);
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
        tag.putLong("PineReadyAt", pineReadyAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        link.load(tag);
        pineReadyAt = tag.getLong("PineReadyAt");
        action = 0;
    }

    // ───────────── GeckoLib ─────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<SummonCentaurEntity> main = new AnimationController<>(this, "main", 4, state -> {
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
