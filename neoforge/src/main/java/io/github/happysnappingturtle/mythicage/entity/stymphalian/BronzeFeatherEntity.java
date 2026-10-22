package io.github.happysnappingturtle.mythicage.entity.stymphalian;

import io.github.happysnappingturtle.mythicage.wild.greek.GreekWild;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 斯廷法利斯怪鸟射出的青铜羽（01_希腊 W3）：慢速直飞（不受重力，0.6 格 / 刻），打中造成 3 点弹射物伤害——
 * 伤害来源带着弹射物，所以正面举盾能挡住；不打别的怪鸟。撞到方块碎成金属屑，不能捡。最多飞 100 刻。
 */
public class BronzeFeatherEntity extends ThrowableItemProjectile {
    private static final int LIFETIME = 100;

    private double damage = 3;

    public BronzeFeatherEntity(EntityType<? extends BronzeFeatherEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public BronzeFeatherEntity(Level level, LivingEntity shooter, double damage) {
        super(GreekWild.BRONZE_FEATHER_SHOT.get(), shooter, level);
        this.damage = damage;
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return GreekWild.BRONZE_FEATHER.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return !(target instanceof StymphalianBirdEntity) && super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        hit.getEntity().hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity o ? o : null), (float) damage);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(getDefaultItem())),
                        getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.05);
            }
            playSound(SoundEvents.CHAIN_HIT, 0.6F, 1.4F);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("Damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getDouble("Damage");
        setNoGravity(true);
    }
}
