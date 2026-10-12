package com.example.mythic_age.entity.ai.goal;

import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public class CentaurRangedAttackGoal extends MythicSpecialAttackGoal {

    private static final int VOLLEY_COUNT = 5;
    private static final float ARROW_DAMAGE = 6.5F;
    private static final float ARROW_SPEED = 1.85F;

    public CentaurRangedAttackGoal(HostileEntity centaur) {
        super(centaur, 180, 20.0F);
    }

    @Override
    protected void performAttack(LivingEntity target) {
        mob.playSound(ModSounds.ENTITY_CENTAUR_SPECIAL, 1.2F, 0.75F);
        mob.swingHand(Hand.MAIN_HAND);

        double baseX = target.getX() - mob.getX();
        double baseY = target.getBodyY(0.45D) - mob.getEyeY();
        double baseZ = target.getZ() - mob.getZ();
        double horizontal = Math.sqrt(baseX * baseX + baseZ * baseZ);

        for (int i = 0; i < VOLLEY_COUNT; i++) {
            ArrowEntity arrow = new ArrowEntity(mob.getWorld(), mob);
            double spread = (i - VOLLEY_COUNT / 2) * 0.06D;
            double sx = baseX + spread * baseZ / Math.max(horizontal, 0.1D);
            double sz = baseZ - spread * baseX / Math.max(horizontal, 0.1D);
            double sy = baseY + horizontal * 0.18D + (i - VOLLEY_COUNT / 2) * 0.04D;

            int difficulty = mob.getWorld().getDifficulty().getId();
            arrow.setVelocity(sx, sy, sz, ARROW_SPEED, 10.0F - difficulty * 2.5F);
            arrow.setDamage(ARROW_DAMAGE);
            arrow.setCritical(true);
            mob.getWorld().spawnEntity(arrow);
        }

        if (mob.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CRIT,
                    mob.getX(), mob.getBodyY(0.8D), mob.getZ(),
                    20, 0.5D, 0.3D, 0.5D, 0.15D);
            sw.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                    mob.getX(), mob.getBodyY(0.9D), mob.getZ(),
                    3, 0.3D, 0.1D, 0.3D, 0.0D);
        }

        mob.playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 0.9F + mob.getRandom().nextFloat() * 0.2F);
    }
}
