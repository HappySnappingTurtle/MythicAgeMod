package io.github.happysnappingturtle.mythicage.content.greek.cyclops;

import io.github.happysnappingturtle.mythicage.core.power.ThrowRules;
import io.github.happysnappingturtle.mythicage.lair.EliteLairs;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 独眼巨人·牧人投出的巨石（T25a §4.2）：沿固定抛线（{@link ThrowRules}，同投掷型神力）飞到举石时标好的落点，
 * 落地碎裂：{@value #RADIUS} 格内的生物受伤（不伤投手和他的羊），不改地形。不存档（飞行不到 1 秒）。
 */
public class ThrownBoulder extends Entity implements ItemSupplier {
    public static final double RADIUS = 1.75;

    private Vec3 from = Vec3.ZERO;
    private Vec3 to = Vec3.ZERO;
    private int tick;
    private int total = 1;
    private float damage;
    @Nullable
    private LivingEntity owner;

    public ThrownBoulder(EntityType<? extends ThrownBoulder> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static ThrownBoulder launch(ServerLevel level, LivingEntity owner, Vec3 from, Vec3 to, float damage) {
        ThrownBoulder b = new ThrownBoulder(GreekCyclops.THROWN_BOULDER.get(), level);
        b.owner = owner;
        b.from = from;
        b.to = to;
        b.damage = damage;
        b.total = ThrowRules.flightTicks(from.distanceTo(to));
        b.moveTo(from.x, from.y, from.z);
        level.addFreshEntity(b);
        return b;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        tick++;
        double f = Math.min(1.0, tick / (double) total);
        double dist = from.distanceTo(to);
        Vec3 p = from.lerp(to, f).add(0, ThrowRules.arc(dist, f), 0);
        setPos(p.x, p.y, p.z);
        if (f >= 1.0) {
            land(sl);
        }
    }

    private void land(ServerLevel sl) {
        AABB area = new AABB(to, to).inflate(RADIUS + 0.6, 1.5, RADIUS + 0.6);
        String lair = owner == null ? "" : owner.getPersistentData().getString(EliteLairs.LAIR_KEY);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e != owner
                && (lair.isEmpty() || !lair.equals(e.getPersistentData().getString(EliteLairs.LAIR_KEY))))) {
            double dx = e.getX() - to.x;
            double dz = e.getZ() - to.z;
            if (dx * dx + dz * dz <= (RADIUS + 0.6) * (RADIUS + 0.6)) {
                e.hurt(damageSources().mobProjectile(this, owner), damage);
            }
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, GreekCyclops.CAVE_BOULDER.get().defaultBlockState()),
                to.x, to.y + 0.3, to.z, 40, 0.8, 0.3, 0.8, 0.15);
        sl.playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6F, 1.4F);
        sl.playSound(null, to.x, to.y, to.z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
        discard();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(GreekCyclops.CAVE_BOULDER.get());
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
