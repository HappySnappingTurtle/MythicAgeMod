package com.example.mythic_age.item.spell.atlantean;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.particle.ModParticles;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class AbyssalRiftSpell extends AbstractSpell {
    public AbyssalRiftSpell() { super(SpellType.ABYSSAL_RIFT, MythologyType.ATLANTEAN, SpellCategory.OFFENSIVE, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        Vec3d riftCenter = Vec3d.ofCenter(targetPos);
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(16.0, 8.0, 16.0), entity -> entity.isAlive());

        if (enemies.isEmpty()) {
            sendMessage(caster, "mythic_age.spellbook.message.abyssal_rift.no_targets");
        }

        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().magic(), 10.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 180, 0));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 360, 2));

            Vec3d pull = riftCenter.subtract(enemy.getPos());
            if (pull.lengthSquared() > 0.01D) {
                enemy.setVelocity(enemy.getVelocity().add(pull.normalize().multiply(0.38D)));
                enemy.velocityModified = true;
            }
        }

        MythicParticleEffects.spawnUltimate(world, ParticleTypes.SQUID_INK,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            110, 6.0, 2.4, 6.0, 0.08);
        MythicParticleEffects.spawnMajor(world, ParticleTypes.PORTAL,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            120, 4.0, 1.8, 4.0, 0.18);
        MythicParticleEffects.spawnMajor(world, ModParticles.SHOCKWAVE,
            targetPos.getX() + 0.5, targetPos.getY() + 0.2, targetPos.getZ() + 0.5,
            26, 5.0, 0.1, 5.0, 0.02);

        world.playSound(null, targetPos, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.PLAYERS, 1.7f, 0.55f);
        world.playSound(null, targetPos, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.3f, 0.85f);
        sendMessage(caster, "mythic_age.spellbook.message.abyssal_rift.success");
        return true;
    }
}
