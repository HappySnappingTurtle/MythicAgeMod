package io.github.happysnappingturtle.mythicage.content.greek.knossos;

import io.github.happysnappingturtle.mythicage.content.greek.ruin.GreekRuins;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 弥诺陶洛斯掷出的碎石（T23 §6.2）：抛物线弹体，命中一个生物造成伤害后碎掉，落地也碎掉；不破坏方块。
 * 外观用大理石碎块的物品。
 */
public class ThrownRubble extends ThrowableItemProjectile {
    private float damage = 10;

    public ThrownRubble(EntityType<? extends ThrownRubble> type, Level level) {
        super(type, level);
    }

    public ThrownRubble(Level level, LivingEntity owner, float damage) {
        super(GreekKnossos.THROWN_RUBBLE.get(), owner, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return GreekRuins.MARBLE_RUBBLE.get().asItem();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity e = hit.getEntity();
        if (e != getOwner() && e instanceof LivingEntity living) {
            MinotaurBossEntity.reportThrowHit(living);
            living.hurt(damageSources().thrown(this, getOwner()), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, GreekRuins.MARBLE_RUBBLE.get().defaultBlockState()),
                    getX(), getY(), getZ(), 16, 0.3, 0.2, 0.3, 0.1);
            discard();
        }
    }
}
