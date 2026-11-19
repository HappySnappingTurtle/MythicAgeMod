package io.github.happysnappingturtle.mythicage.content.greek.cyclops;

import io.github.happysnappingturtle.mythicage.content.greek.power.CeasefirePower;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 扔出去的马洛涅烈酒：落地（或砸到生物脚边）变成酒碗；不伤人，往停火区里扔不算破约（I4）。 */
public class ThrownWine extends ThrowableItemProjectile implements CeasefirePower.Harmless {
    public ThrownWine(EntityType<? extends ThrownWine> type, Level level) {
        super(type, level);
    }

    public ThrownWine(Level level, LivingEntity owner) {
        super(GreekCyclops.THROWN_WINE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return GreekCyclops.MARONEAN_WINE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 at = result instanceof EntityHitResult e ? e.getEntity().position() : result.getLocation();
        WineBowl.place(sl, at);
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()), at.x, at.y + 0.2, at.z, 8, 0.2, 0.1, 0.2, 0.05);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.DECORATED_POT_INSERT, SoundSource.NEUTRAL, 0.8F, 0.8F);
        discard();
    }
}
