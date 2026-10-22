package io.github.happysnappingturtle.mythicage.summon.greek;

import io.github.happysnappingturtle.mythicage.summon.SummonTargets;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 半人马掷出的松树干（T14 §11.5「掷松」）：沿直线飞（略下坠），每个目标只打一次，最多穿过 {@code pierce} 个目标；
 * 只打主人能打的目标（不误伤村民、中立动物与自己人）；撞到方块就碎成木屑。最多飞 40 刻。
 */
public class CentaurPineEntity extends Projectile {
    public static final double SPEED = 1.2;
    private static final int LIFETIME = 40;

    private double damage = 12;
    private int pierce = 3;
    private final Set<UUID> hit = new HashSet<>();
    private final List<UUID> hitOrder = new ArrayList<>();
    private int age;

    public CentaurPineEntity(EntityType<? extends CentaurPineEntity> type, Level level) {
        super(type, level);
    }

    public void configure(double damage, int pierce) {
        this.damage = damage;
        this.pierce = pierce;
    }

    /** 已命中的目标（按先后，GameTest 读）。 */
    public List<UUID> hits() {
        return List.copyOf(hitOrder);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        Vec3 from = position();
        Vec3 to = from.add(v);
        if (!level().isClientSide) {
            if (++age > LIFETIME) {
                shatter();
                return;
            }
            HitResult block = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (block.getType() != HitResult.Type.MISS) {
                to = block.getLocation();
            }
            Entity owner = getOwner();
            AABB swept = getBoundingBox().expandTowards(to.subtract(from)).inflate(0.4);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, swept,
                    e -> e.isAlive() && e != owner && !hit.contains(e.getUUID()))) {
                if (owner != null && !SummonTargets.canAttack(owner, e)) {
                    continue;
                }
                hit.add(e.getUUID());
                hitOrder.add(e.getUUID());
                e.hurt(damageSources().thrown(this, owner), (float) damage);
                Vec3 push = v.multiply(1, 0, 1).normalize();
                e.knockback(0.8, -push.x, -push.z);
                playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.8F, 0.8F);
                if (hitOrder.size() >= pierce) {
                    shatter();
                    return;
                }
            }
            if (block.getType() != HitResult.Type.MISS) {
                shatter();
                return;
            }
        }
        setPos(to.x, to.y, to.z);
        setDeltaMovement(v.x, v.y - 0.02, v.z);
        // 朝向跟着速度（渲染器据此摆树干）
        double flat = v.horizontalDistance();
        setYRot((float) (Math.toDegrees(Math.atan2(v.x, v.z))));
        setXRot((float) (Math.toDegrees(Math.atan2(v.y, flat))));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    private void shatter() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LOG.defaultBlockState()),
                    getX(), getY(), getZ(), 24, 0.6, 0.4, 0.6, 0.1);
            sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, getSoundSource(), 1.0F, 0.8F);
        }
        discard();
    }

    public int age() {
        return age;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("Damage", damage);
        tag.putInt("Pierce", pierce);
        tag.putInt("Age", age);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getDouble("Damage");
        pierce = tag.getInt("Pierce");
        age = tag.getInt("Age");
    }
}
