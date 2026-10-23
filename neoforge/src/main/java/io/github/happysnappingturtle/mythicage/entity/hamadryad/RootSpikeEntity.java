package io.github.happysnappingturtle.mythicage.entity.hamadryad;

import io.github.happysnappingturtle.mythicage.entity.MythVariantHolder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 根刺（树仙女的攻击，01_希腊 W5、T15 §6）：地面裂纹 16 刻（预警）→ 三根橡树根冲出，半径 1 格内的生物受 6 点伤害并被顶起
 * （树仙女自己不受）→ 12 刻后缩回消失。
 */
public class RootSpikeEntity extends Entity implements GeoEntity, MythVariantHolder {
    public static final int CRACK_TICKS = 16;
    public static final int BURST_TICKS = 8;
    public static final int RETRACT_TICKS = 12;
    public static final double RADIUS = 1.0;

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(RootSpikeEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation CRACK = RawAnimation.begin().thenPlayAndHold("crack");
    private static final RawAnimation BURST = RawAnimation.begin().thenPlayAndHold("burst");
    private static final RawAnimation RETRACT = RawAnimation.begin().thenPlayAndHold("retract");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID owner;
    private float damage = 6;
    private int age;

    public RootSpikeEntity(EntityType<? extends RootSpikeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setup(@Nullable LivingEntity owner, float damage) {
        this.owner = owner == null ? null : owner.getUUID();
        this.damage = damage;
    }

    /** 0 裂纹、1 出刺、2 缩回。 */
    public int phase() {
        return entityData.get(PHASE);
    }

    public int age() {
        return age;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        age++;
        ServerLevel sl = (ServerLevel) level();
        if (age < CRACK_TICKS) {
            if (age % 3 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(blockPosition().below())),
                        getX(), getY() + 0.05, getZ(), 6, 0.6, 0.02, 0.6, 0.02);
            }
            if (age == 1) {
                playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.6F);
            }
        } else if (age == CRACK_TICKS) {
            entityData.set(PHASE, 1);
            playSound(SoundEvents.MANGROVE_ROOTS_BREAK, 1.4F, 0.6F);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(getX() - RADIUS, getY(), getZ() - RADIUS, getX() + RADIUS, getY() + 1.5, getZ() + RADIUS),
                    e -> e.isAlive() && !(e instanceof HamadryadEntity) && !e.getUUID().equals(owner))) {
                Entity src = owner == null ? null : sl.getEntity(owner);
                e.hurt(src instanceof LivingEntity le ? damageSources().mobAttack(le) : damageSources().magic(), damage);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.45, 0));
                e.hurtMarked = true;
            }
        } else if (age == CRACK_TICKS + BURST_TICKS) {
            entityData.set(PHASE, 2);
        } else if (age >= CRACK_TICKS + BURST_TICKS + RETRACT_TICKS) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
        damage = tag.contains("Damage") ? tag.getFloat("Damage") : 6;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putFloat("Damage", damage);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(switch (phase()) {
            case 1 -> BURST;
            case 2 -> RETRACT;
            default -> CRACK;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
