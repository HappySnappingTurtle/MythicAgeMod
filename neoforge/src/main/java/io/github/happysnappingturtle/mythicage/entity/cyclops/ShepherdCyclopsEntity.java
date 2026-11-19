package io.github.happysnappingturtle.mythicage.entity.cyclops;

import io.github.happysnappingturtle.mythicage.advancement.MythEvents;
import io.github.happysnappingturtle.mythicage.combat.DamagePipeline;
import io.github.happysnappingturtle.mythicage.content.greek.cyclops.ThrownBoulder;
import io.github.happysnappingturtle.mythicage.content.greek.cyclops.WineBowl;
import io.github.happysnappingturtle.mythicage.core.combat.DamageZones;
import io.github.happysnappingturtle.mythicage.core.lair.LairRules;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.wild.DrunkRules;
import io.github.happysnappingturtle.mythicage.entity.anim.MythAnimState;
import io.github.happysnappingturtle.mythicage.entity.centaur.WildCentaurEntity;
import io.github.happysnappingturtle.mythicage.lair.EliteLairs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 精英「独眼巨人·牧人」（EL-GR-02，T25a §4）：
 * <ul>
 *   <li>巢穴（⑤）：家与睡处由登记写入；白天在家 {@value #GRAZE_RADIUS} 格内放羊（洞里的羊跟着他走），傍晚回睡处睡觉；
 *       离家 {@link LairRules#LEASH} 格放下目标回家、到家回满血；</li>
 *   <li>远处投石：目标在 {@value #ROCK_MIN}–{@value #ROCK_MAX} 格、看得见 → 左手举石 1.2 秒，落点地面有红色标记 →
 *       沿抛线投出，落地碎裂（{@value #ROCK_DAMAGE}，{@link ThrownBoulder#RADIUS} 格内），不改地形；</li>
 *   <li>近身挥棒：举棒蓄力 0.8 秒、第 {@value #SMASH_HIT} 刻砸下（{@value #SMASH_REACH} 格内）；</li>
 *   <li>奥德修斯之计（§4.3，core {@link DrunkRules}）：16 格内有酒碗就走过去喝 2 秒，醉倒 8 秒（仰面躺倒），醉倒期间受伤 ×3
 *       （T25.1 窗口区，效果组 {@code cyclops_drunk}）；醒后 30 秒对酒没兴趣；喝的途中挨打就中断。醉倒中被玩家打死 →
 *       进度「我的名字叫『无人』」；</li>
 *   <li>敌视玩家与 16 格内的野生半人马。</li>
 * </ul>
 */
public class ShepherdCyclopsEntity extends CyclopsEntity implements Enemy, DamagePipeline.Targeted {
    public static final int GRAZE_RADIUS = 24;
    public static final double INSIDE = 6.0;
    public static final int STUCK_TICKS = 600;
    private static final double NOBODY_NEAR = 24.0;

    public static final int ROCK_WINDUP = 24;
    public static final int ROCK_COOLDOWN = 140;
    public static final double ROCK_MIN = 7.0;
    public static final double ROCK_MAX = 26.0;
    public static final float ROCK_DAMAGE = 12.0F;
    public static final int SMASH_HIT = 18;
    public static final int SMASH_TICKS = 24;
    public static final int SMASH_COOLDOWN = 30;
    public static final double SMASH_REACH = 4.5;
    public static final double CENTAUR_RANGE = 16.0;
    /** 进度事件：醉倒时被玩家击杀。 */
    public static final String NOBODY_EVENT = "cyclops_nobody";

    @Nullable
    private BlockPos home;
    @Nullable
    private BlockPos sleep;
    private String lairKey = "";
    private boolean returning;

    private DrunkRules.State drunk = DrunkRules.State.FRESH;
    @Nullable
    private UUID bowl;
    private long rockReadyAt;
    private long smashReadyAt;

    public ShepherdCyclopsEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createShepherdAttributes() {
        return createAttributes()
                .add(Attributes.MAX_HEALTH, 160.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new StuporGoal());
        goalSelector.addGoal(1, new ReturnHomeGoal());
        goalSelector.addGoal(1, new WineGoal());
        goalSelector.addGoal(2, new RockGoal());
        goalSelector.addGoal(3, new SmashGoal());
        goalSelector.addGoal(4, new SleepGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, WildCentaurEntity.class, 10, true, false,
                e -> distanceToSqr(e) <= CENTAUR_RANGE * CENTAUR_RANGE));
    }

    // ───────────── 巢穴 ─────────────

    public void bindLair(String key, BlockPos home, BlockPos sleep) {
        this.lairKey = key;
        this.home = home.immutable();
        this.sleep = sleep.immutable();
        restrictTo(home, GRAZE_RADIUS);
    }

    public String lairKey() {
        return lairKey;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public boolean inside() {
        return sleep != null && distanceToSqr(Vec3.atBottomCenterOf(sleep)) <= INSIDE * INSIDE;
    }

    public boolean returning() {
        return returning;
    }

    private boolean night() {
        return LairRules.sealTime(level().getDayTime());
    }

    @Override
    public DamageZones.Target damageTarget() {
        return DamageZones.Target.ELITE;
    }

    // ───────────── 醉倒 ─────────────

    public DrunkRules.Phase drunkPhase() {
        return drunk.phase();
    }

    public boolean drunk() {
        return drunk.phase() == DrunkRules.Phase.DRUNK;
    }

    private boolean stupor() {
        return drunk.phase() == DrunkRules.Phase.DRINK || drunk.phase() == DrunkRules.Phase.DRUNK;
    }

    /** 伤害乘区（T25.1）：醉倒时窗口区 ×3（D12；精英不受 Boss 窗口封顶）。 */
    public static void collect(DamagePipeline.Hit hit, List<DamageZones.Mod> out) {
        if (hit.victim() instanceof ShepherdCyclopsEntity s && s.drunk()) {
            out.add(DamageZones.Mod.of(DamageZones.Zone.WINDOW, "cyclops_drunk", DrunkRules.DRUNK_MULTIPLIER));
        }
    }

    private void tickDrink(long now) {
        DrunkRules.State before = drunk;
        if (drunk.phase() == DrunkRules.Phase.SOBER && tickCount % 10 == 0) {
            WineBowl b = nearestBowl();
            if (b != null) {
                drunk = DrunkRules.notice(drunk, now, distanceToSqr(b));
                if (drunk.phase() == DrunkRules.Phase.APPROACH) {
                    bowl = b.getUUID();
                    setTarget(null);
                }
            }
        }
        if (drunk.phase() == DrunkRules.Phase.APPROACH && bowlEntity() == null) {
            drunk = DrunkRules.bowlGone(drunk, now);
        }
        drunk = DrunkRules.tick(drunk, now);
        if (DrunkRules.finishedDrinking(before, drunk)) {
            WineBowl b = bowlEntity();
            if (b != null) {
                b.discard();
            }
            bowl = null;
        }
        if (before.phase() != drunk.phase()) {
            switch (drunk.phase()) {
                case DRINK -> {
                    setAnimState(MythAnimState.DRINK);
                    playSound(SoundEvents.GENERIC_DRINK, 1.2F, 0.6F);
                }
                case DRUNK -> {
                    setAnimState(MythAnimState.DRUNK);
                    playSound(SoundEvents.PLAYER_BURP, 1.5F, 0.5F);
                }
                default -> {
                    if (before.phase() == DrunkRules.Phase.DRINK || before.phase() == DrunkRules.Phase.DRUNK) {
                        setAnimState(MythAnimState.AUTO);
                    }
                }
            }
        }
        if (drunk() && tickCount % 20 == 0 && level() instanceof ServerLevel sl) {
            // 醉意：头顶冒出几个小泡
            sl.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 1.2, getZ(), 4, 0.6, 0.2, 0.6, 0.02);
        }
    }

    @Nullable
    private WineBowl nearestBowl() {
        List<WineBowl> bowls = level().getEntitiesOfClass(WineBowl.class, getBoundingBox().inflate(DrunkRules.NOTICE_RANGE),
                b -> b.isAlive() && distanceToSqr(b) <= DrunkRules.NOTICE_RANGE * DrunkRules.NOTICE_RANGE);
        WineBowl best = null;
        for (WineBowl b : bowls) {
            if (best == null || distanceToSqr(b) < distanceToSqr(best)) {
                best = b;
            }
        }
        return best;
    }

    @Nullable
    private WineBowl bowlEntity() {
        return bowl != null && level() instanceof ServerLevel sl && sl.getEntity(bowl) instanceof WineBowl b && b.isAlive() ? b : null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && !level().isClientSide) {
            DrunkRules.State before = drunk;
            drunk = DrunkRules.hurt(drunk, level().getGameTime());
            if (before.phase() == DrunkRules.Phase.DRINK && drunk.phase() == DrunkRules.Phase.SOBER) {
                setAnimState(MythAnimState.AUTO);
            }
            if (animState() == MythAnimState.SLEEP) {
                setAnimState(MythAnimState.AUTO);
            }
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && drunk() && source.getEntity() instanceof ServerPlayer p) {
            MythEvents.fire(p, NOBODY_EVENT, Civilization.GREEK, 1);
        }
        super.die(source);
    }

    // ───────────── 每刻 ─────────────

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long now = level().getGameTime();
        tickDrink(now);
        if (stupor()) {
            setTarget(null);
        }
        if (home != null && !returning && LairRules.mustReturn(distanceToSqr(Vec3.atBottomCenterOf(home)))) {
            returning = true;
            setTarget(null);
        }
        if (tickCount % 40 == 0 && !lairKey.isEmpty()) {
            herd();
        }
    }

    /** 放羊：洞里登记的羊跟着他（离得远的往他身边走）。 */
    private void herd() {
        for (Sheep s : level().getEntitiesOfClass(Sheep.class, getBoundingBox().inflate(LairRules.LEASH),
                e -> e.isAlive() && lairKey.equals(e.getPersistentData().getString(EliteLairs.LAIR_KEY)))) {
            if (s.distanceToSqr(this) > 7 * 7 && !s.isLeashed()) {
                s.getNavigation().moveTo(this, 1.0);
            }
        }
    }

    // ───────────── 目标 ─────────────

    /** 醉倒、喝酒时不动、不转头、不打架。 */
    private final class StuporGoal extends Goal {
        StuporGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return stupor();
        }

        @Override
        public void tick() {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** 闻到酒：走到酒碗边。 */
    private final class WineGoal extends Goal {
        WineGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return drunk.phase() == DrunkRules.Phase.APPROACH;
        }

        @Override
        public void tick() {
            WineBowl b = bowlEntity();
            if (b == null) {
                return;
            }
            getLookControl().setLookAt(b);
            if (distanceToSqr(b) <= 2.6 * 2.6) {
                getNavigation().stop();
                drunk = DrunkRules.arrive(drunk, level().getGameTime());
                setAnimState(MythAnimState.DRINK);
                playSound(SoundEvents.GENERIC_DRINK, 1.2F, 0.6F);
            } else if (getNavigation().isDone() || tickCount % 10 == 0) {
                getNavigation().moveTo(b, 1.0);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private final class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return returning && home != null && !stupor();
        }

        @Override
        public void tick() {
            setTarget(null);
            Vec3 h = Vec3.atBottomCenterOf(home);
            if (distanceToSqr(h) < 4 * 4) {
                returning = false;
                setHealth(getMaxHealth());
                getNavigation().stop();
                return;
            }
            if (getNavigation().isDone() || tickCount % 40 == 0) {
                if (!getNavigation().moveTo(h.x, h.y, h.z, 1.1) && distanceToSqr(h) > 64 * 64) {
                    teleportTo(h.x, h.y, h.z);
                }
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** 远处投石：举石 1.2 秒（落点标记）→ 投出。 */
    private final class RockGoal extends Goal {
        private int timer;
        private Vec3 mark = Vec3.ZERO;

        RockGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive() || stupor() || level().getGameTime() < rockReadyAt) {
                return false;
            }
            double d = distanceTo(t);
            return d >= ROCK_MIN && d <= ROCK_MAX && getSensing().hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return timer > 0 && !stupor();
        }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            timer = ROCK_WINDUP + 12;
            mark = ground(t.position());
            getNavigation().stop();
            playOnce(MythAnimState.LIFT_ROCK, ROCK_WINDUP + 1);
            playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(mark.x, mark.y + 1, mark.z);
            Vec3 to = mark.subtract(position());
            float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
            setYRot(yaw);
            yBodyRot = yaw;
            int age = ROCK_WINDUP + 12 - timer;
            if (age < ROCK_WINDUP && age % 3 == 0 && level() instanceof ServerLevel sl) {
                marker(sl, mark);
            }
            if (age == ROCK_WINDUP && level() instanceof ServerLevel sl) {
                double yawR = Math.toRadians(yaw);
                // 出手点：左手举在头顶左侧（身体朝向的左边 0.7 格、离地 4.6 格）
                Vec3 hand = position().add(Math.cos(yawR) * 0.7, 4.6, Math.sin(yawR) * 0.7);
                ThrownBoulder.launch(sl, ShepherdCyclopsEntity.this, hand, mark, ROCK_DAMAGE);
                playOnce(MythAnimState.THROW_ROCK, 12);
                rockReadyAt = level().getGameTime() + ROCK_COOLDOWN;
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F);
            }
            timer--;
        }

        @Override
        public void stop() {
            timer = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** 落点：脚下的地面（往下找到实心方块），没找到就原样。 */
    private Vec3 ground(Vec3 at) {
        BlockPos.MutableBlockPos p = BlockPos.containing(at).mutable();
        for (int i = 0; i < 6 && level().getBlockState(p.below()).getCollisionShape(level(), p.below()).isEmpty(); i++) {
            p.move(0, -1, 0);
        }
        return new Vec3(at.x, p.getY(), at.z);
    }

    /** 地面落点标记：一圈红色尘点 + 中心的小碎石。 */
    private static void marker(ServerLevel sl, Vec3 c) {
        DustParticleOptions red = new DustParticleOptions(new Vector3f(0.85F, 0.15F, 0.1F), 1.4F);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16;
            sl.sendParticles(red, c.x + Math.cos(a) * ThrownBoulder.RADIUS, c.y + 0.1, c.z + Math.sin(a) * ThrownBoulder.RADIUS,
                    1, 0, 0, 0, 0);
        }
        sl.sendParticles(ParticleTypes.SMOKE, c.x, c.y + 0.1, c.z, 2, 0.2, 0.0, 0.2, 0.0);
    }

    /** 追上去、挥棒：举棒 0.8 秒后砸下。 */
    private final class SmashGoal extends Goal {
        private int swing;

        SmashGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && !stupor();
        }

        @Override
        public boolean canContinueToUse() {
            return (swing > 0 || canUse()) && !stupor();
        }

        @Override
        public void start() {
            swing = 0;
        }

        @Override
        public void stop() {
            swing = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (swing > 0) {
                swing++;
                if (t != null) {
                    getLookControl().setLookAt(t, 30, 30);
                }
                if (swing == SMASH_HIT && t != null && t.isAlive() && distanceTo(t) <= SMASH_REACH + 0.5) {
                    t.hurt(damageSources().mobAttack(ShepherdCyclopsEntity.this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                    Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
                    if (d.lengthSqr() > 1.0E-4) {
                        t.knockback(1.2, -d.x, -d.z);
                    }
                    playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1.0F, 0.6F);
                }
                if (swing >= SMASH_TICKS) {
                    swing = 0;
                    smashReadyAt = level().getGameTime() + SMASH_COOLDOWN;
                }
                return;
            }
            if (t == null) {
                return;
            }
            getLookControl().setLookAt(t, 30, 30);
            if (distanceTo(t) <= SMASH_REACH && level().getGameTime() >= smashReadyAt) {
                getNavigation().stop();
                swing = 1;
                playOnce(MythAnimState.CLUB_SMASH, SMASH_TICKS);
            } else if (getNavigation().isDone() || tickCount % 10 == 0) {
                getNavigation().moveTo(t, 1.15);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** 夜里回睡处睡觉（巢穴在他进洞后封洞）。 */
    private final class SleepGoal extends Goal {
        private int stuck;

        SleepGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return sleep != null && night() && getTarget() == null && !stupor();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            stuck = 0;
        }

        @Override
        public void stop() {
            if (animState() == MythAnimState.SLEEP) {
                setAnimState(MythAnimState.AUTO);
            }
        }

        @Override
        public void tick() {
            if (distanceToSqr(Vec3.atBottomCenterOf(sleep)) > 2.5 * 2.5) {
                if (animState() == MythAnimState.SLEEP) {
                    setAnimState(MythAnimState.AUTO);
                }
                if (getNavigation().isDone() || tickCount % 40 == 0) {
                    getNavigation().moveTo(sleep.getX() + 0.5, sleep.getY(), sleep.getZ() + 0.5, 0.8);
                }
                if (++stuck > STUCK_TICKS && level().getNearestPlayer(ShepherdCyclopsEntity.this, NOBODY_NEAR) == null) {
                    teleportTo(sleep.getX() + 0.5, sleep.getY(), sleep.getZ() + 0.5);
                    getNavigation().stop();
                    stuck = 0;
                }
            } else {
                getNavigation().stop();
                stuck = 0;
                if (animState() != MythAnimState.SLEEP) {
                    setAnimState(MythAnimState.SLEEP);
                }
                if (tickCount % 60 == 0) {
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.FOX_SLEEP, SoundSource.HOSTILE, 1.0F, 0.4F);
                }
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Lair", lairKey);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        if (sleep != null) {
            tag.put("Sleep", NbtUtils.writeBlockPos(sleep));
        }
        tag.putBoolean("Returning", returning);
        tag.putLong("RockReadyAt", rockReadyAt);
        tag.putLong("WokeAt", drunk.wokeAt());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lairKey = tag.getString("Lair");
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        sleep = NbtUtils.readBlockPos(tag, "Sleep").orElse(null);
        returning = tag.getBoolean("Returning");
        rockReadyAt = tag.getLong("RockReadyAt");
        // 读档后醒着（醉倒、喝酒不存档）
        drunk = new DrunkRules.State(DrunkRules.Phase.SOBER, 0, tag.contains("WokeAt") ? tag.getLong("WokeAt") : Long.MIN_VALUE / 4);
        if (home != null) {
            restrictTo(home, GRAZE_RADIUS);
        }
    }
}
