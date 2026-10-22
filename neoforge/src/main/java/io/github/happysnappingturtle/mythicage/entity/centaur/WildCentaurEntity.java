package io.github.happysnappingturtle.mythicage.entity.centaur;

import io.github.happysnappingturtle.mythicage.core.wild.GroupAggro;
import io.github.happysnappingturtle.mythicage.summon.MythSummon;
import io.github.happysnappingturtle.mythicage.summon.SummonUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 野外半人马（W1，T15 设计文档 §2）：中立群居。平时跟着首领在营地附近巡游，夜里与雨天回营火边卧下休息；
 * 有人打群里的一只（玩家、玩家的召唤单位、玩家的投射物），全群找他算账，追出 32 格或 60 秒后放弃；
 * 首领（1.1 倍、名牌「喀戎的后裔」）死了，其余成员放下仇恨、远离凶手。敌视狼；对村民友好。
 */
public class WildCentaurEntity extends AbstractCentaurEntity {
    private static final EntityDataAccessor<Boolean> LEADER = SynchedEntityData.defineId(WildCentaurEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RESTING = SynchedEntityData.defineId(WildCentaurEntity.class, EntityDataSerializers.BOOLEAN);
    public static final float LEADER_SCALE = 1.1F;
    public static final int ANGER_TICKS = 1200;
    public static final double ANGER_RANGE = 32;
    public static final int FLEE_TICKS = 200;
    public static final double CAMP_RADIUS = 24;

    @Nullable
    private UUID groupId;
    @Nullable
    private BlockPos camp;
    @Nullable
    private UUID angerAt;
    private long angerUntil;
    @Nullable
    private UUID fleeFrom;
    private long fleeUntil;

    public WildCentaurEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createWildAttributes() {
        return createCentaurAttributes();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEADER, false);
        builder.define(RESTING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new Flee());
        goalSelector.addGoal(3, new RestAtCamp());
        goalSelector.addGoal(4, new Roam());
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HoldGrudge());
        targetSelector.addGoal(1, new Retaliate());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Wolf.class, 10, true, false, w -> !((Wolf) w).isTame()));
    }

    /** 群体、营地、首领（营地生成时调用）。 */
    public void joinGroup(UUID group, BlockPos campCenter, boolean leader) {
        this.groupId = group;
        this.camp = campCenter.immutable();
        entityData.set(LEADER, leader);
        if (leader) {
            setCustomName(Component.translatable("entity.mythic_age.centaur.leader"));
        }
        refreshDimensions();
    }

    public void applyWildStats() {
        applyCentaurStats(SummonUnits.statsFile("greek", "wild_centaur"), 40, 0.3);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, type, data);
        applyWildStats();
        return d;
    }

    public boolean leader() {
        return entityData.get(LEADER);
    }

    @Nullable
    public UUID groupId() {
        return groupId;
    }

    @Nullable
    public BlockPos camp() {
        return camp;
    }

    @Override
    public String variant() {
        return leader() ? "leader" : "";
    }

    @Override
    public float variantScale() {
        return leader() ? LEADER_SCALE : 1.0F;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(variantScale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (LEADER.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    protected boolean resting() {
        return entityData.get(RESTING);
    }

    // ───────────── 仇恨 ─────────────

    /** 能打：记仇的对象（在 32 格内、没过期），未被驯服的狼。不打村民。 */
    @Override
    protected boolean mayAttack(LivingEntity t) {
        if (t instanceof AbstractVillager || !t.isAlive()) {
            return false;
        }
        if (t instanceof Wolf w) {
            return !w.isTame();
        }
        return t.getUUID().equals(angerAt) && level().getGameTime() < angerUntil;
    }

    public boolean angryAt(Entity e) {
        return e.getUUID().equals(angerAt) && level().getGameTime() < angerUntil;
    }

    /** 伤害来源归到谁身上：玩家本人、玩家的召唤单位、玩家的投射物 → 玩家；别的生物 → 它自己。 */
    static GroupAggro.Source classify(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity cause = source.getEntity();
        if (cause instanceof Player) {
            return direct instanceof Projectile ? GroupAggro.Source.PLAYER_PROJECTILE : GroupAggro.Source.PLAYER;
        }
        if (cause instanceof MythSummon s && s.link().owner != null) {
            return GroupAggro.Source.PLAYER_SUMMON;
        }
        return cause instanceof LivingEntity ? GroupAggro.Source.OTHER_MOB : GroupAggro.Source.NONE;
    }

    @Nullable
    private LivingEntity culprit(DamageSource source) {
        Entity cause = source.getEntity();
        if (cause instanceof MythSummon s && s.link().owner != null && level() instanceof ServerLevel sl) {
            Player owner = sl.getPlayerByUUID(s.link().owner);
            if (owner != null) {
                return owner;
            }
        }
        return cause instanceof LivingEntity le ? le : null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        // 这一下打死了自己：不再招呼同伴（首领之死另有处理，见 die）
        if (hurt && !level().isClientSide && !isDeadOrDying()) {
            GroupAggro.Source kind = classify(source);
            LivingEntity who = culprit(source);
            if (who != null && GroupAggro.alertsGroup(kind)) {
                for (WildCentaurEntity m : groupMembers()) {
                    m.anger(who);
                }
            } else if (who != null && GroupAggro.retaliates(kind)) {
                anger(who);
            }
        }
        return hurt;
    }

    public void anger(LivingEntity who) {
        angerAt = who.getUUID();
        angerUntil = level().getGameTime() + ANGER_TICKS;
        fleeFrom = null;
        entityData.set(RESTING, false);
        setTarget(who);
    }

    /** 野性的半人马不让拴绳。 */
    @Override
    public boolean canBeLeashed() {
        return false;
    }

    /** 同群（含自己）、48 格内的半人马。 */
    public List<WildCentaurEntity> groupMembers() {
        if (groupId == null) {
            return List.of(this);
        }
        return level().getEntitiesOfClass(WildCentaurEntity.class, new AABB(blockPosition()).inflate(48),
                e -> e.isAlive() && groupId.equals(e.groupId));
    }

    /** 首领死了：其余成员放下仇恨、远离凶手（07 号边界用例）。 */
    @Override
    public void die(DamageSource source) {
        if (leader() && !level().isClientSide) {
            LivingEntity killer = culprit(source);
            for (WildCentaurEntity m : groupMembers()) {
                if (m != this) {
                    m.angerAt = null;
                    m.setTarget(null);
                    if (killer != null) {
                        m.fleeFrom = killer.getUUID();
                        m.fleeUntil = level().getGameTime() + FLEE_TICKS;
                    }
                }
            }
        }
        super.die(source);
    }

    public boolean fleeing() {
        return fleeFrom != null && level().getGameTime() < fleeUntil;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && resting() && (getTarget() != null || !wantsRest())) {
            entityData.set(RESTING, false);
        }
    }

    private boolean wantsRest() {
        return level().isNight() || level().isRaining();
    }

    /** 记仇的对象还在 32 格内、没过期，就把他设为目标；否则放下仇恨。 */
    private final class HoldGrudge extends TargetGoal {
        HoldGrudge() {
            super(WildCentaurEntity.this, false);
        }

        @Override
        public boolean canUse() {
            if (angerAt == null) {
                return false;
            }
            Entity e = level() instanceof ServerLevel sl ? sl.getEntity(angerAt) : null;
            if (!(e instanceof LivingEntity le) || !le.isAlive() || level().getGameTime() >= angerUntil
                    || distanceToSqr(le) > ANGER_RANGE * ANGER_RANGE) {
                angerAt = null;
                if (getTarget() != null && e != null && getTarget() == e) {
                    setTarget(null);
                }
                return false;
            }
            targetMob = le;
            return getTarget() != le;
        }

        @Override
        public void start() {
            mob.setTarget(targetMob);
            super.start();
        }
    }

    /** 首领死后远离凶手。 */
    private final class Flee extends Goal {
        Flee() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return fleeing() && !busy();
        }

        @Override
        public void tick() {
            if (getNavigation().isDone() && level() instanceof ServerLevel sl) {
                Entity from = sl.getEntity(fleeFrom);
                if (from == null) {
                    return;
                }
                Vec3 to = DefaultRandomPos.getPosAway(WildCentaurEntity.this, 24, 7, from.position());
                if (to != null) {
                    getNavigation().moveTo(to.x, to.y, to.z, 1.35);
                }
            }
        }
    }

    /** 夜里与雨天回营火边卧下。 */
    private final class RestAtCamp extends Goal {
        RestAtCamp() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return camp != null && wantsRest() && getTarget() == null && !fleeing();
        }

        private int tries;

        private Vec3 seat;

        @Override
        public void start() {
            tries = 0;
            seat = seat();
        }

        /**
         * 围火的位置（半径 3.5）：按 UUID 在群里排个序，均匀分在 -10°～290° 之间，
         * 空出东北边皮帐所在的那一块（营地 build 里帐子在约 -37° 方向），免得挤成一团或钻进帐子。
         */
        private Vec3 seat() {
            List<WildCentaurEntity> ms = new ArrayList<>(groupMembers());
            ms.sort(Comparator.comparing(Entity::getUUID));
            int n = Math.max(1, ms.size());
            int i = Math.max(0, ms.indexOf(WildCentaurEntity.this));
            double a = Math.toRadians(-10 + 300.0 * (i + 0.5) / n);
            return new Vec3(camp.getX() + 0.5 + Math.cos(a) * 3.5, camp.getY(), camp.getZ() + 0.5 + Math.sin(a) * 3.5);
        }

        @Override
        public void tick() {
            double dx = getX() - seat.x;
            double dz = getZ() - seat.z;
            if (dx * dx + dz * dz > 1.5 * 1.5 && tries < 200) {
                tries++;
                entityData.set(RESTING, false);
                if (getNavigation().isDone()) {
                    getNavigation().moveTo(seat.x, seat.y, seat.z, 1.0);
                }
            } else if (getNavigation().isDone()) {
                entityData.set(RESTING, true);
                getLookControl().setLookAt(Vec3.atCenterOf(camp));
                Vec3 c = Vec3.atBottomCenterOf(camp);
                float yaw = (float) (Math.atan2(c.z - getZ(), c.x - getX()) * 180 / Math.PI) - 90;
                setYRot(yaw);
                yBodyRot = yaw;
            }
        }

        @Override
        public void stop() {
            entityData.set(RESTING, false);
        }
    }

    /** 白天在营地附近巡游，跟着首领走。 */
    private final class Roam extends Goal {
        private int cooldown;

        Roam() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || fleeing() || resting() || --cooldown > 0) {
                return false;
            }
            cooldown = 80 + random.nextInt(120);
            return true;
        }

        @Override
        public void start() {
            Vec3 to = null;
            WildCentaurEntity lead = leader() ? null : groupMembers().stream().filter(WildCentaurEntity::leader).findFirst().orElse(null);
            if (lead != null && distanceToSqr(lead) > 8 * 8) {
                to = lead.position().add(random.nextGaussian() * 3, 0, random.nextGaussian() * 3);
            } else if (camp != null && distanceToSqr(Vec3.atBottomCenterOf(camp)) > CAMP_RADIUS * CAMP_RADIUS) {
                to = Vec3.atBottomCenterOf(camp);
            } else {
                to = DefaultRandomPos.getPos(WildCentaurEntity.this, 10, 4);
            }
            if (to != null) {
                getNavigation().moveTo(to.x, to.y, to.z, 0.8);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !getNavigation().isDone() && getTarget() == null && !fleeing();
        }
    }

    // ───────────── 存档 ─────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (groupId != null) {
            tag.putUUID("Group", groupId);
        }
        if (camp != null) {
            tag.putLong("Camp", camp.asLong());
        }
        tag.putBoolean("Leader", leader());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        groupId = tag.hasUUID("Group") ? tag.getUUID("Group") : null;
        camp = tag.contains("Camp") ? BlockPos.of(tag.getLong("Camp")) : null;
        entityData.set(LEADER, tag.getBoolean("Leader"));
        refreshDimensions();
    }
}
