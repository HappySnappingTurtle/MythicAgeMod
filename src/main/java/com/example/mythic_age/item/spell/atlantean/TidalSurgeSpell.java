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

public class TidalSurgeSpell extends AbstractSpell {
    public TidalSurgeSpell() { super(SpellType.TIDAL_SURGE, MythologyType.ATLANTEAN, SpellCategory.OFFENSIVE, 120); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(12.0, 5.0, 12.0), entity -> entity.isAlive());

        if (enemies.isEmpty()) {
            sendMessage(caster, "mythic_age.spellbook.message.tidal_surge.no_targets");
        }

        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().drown(), 6.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 320, 2));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 220, 1));

            Vec3d push = enemy.getPos().subtract(Vec3d.ofCenter(targetPos));
            if (push.lengthSquared() > 0.01D) {
                enemy.setVelocity(enemy.getVelocity().add(push.normalize().multiply(0.28D).add(0.0D, 0.12D, 0.0D)));
                enemy.velocityModified = true;
            }
        }

        MythicParticleEffects.spawnMajor(world, ParticleTypes.SPLASH,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            90, 7.0, 2.5, 7.0, 0.24);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.BUBBLE_POP,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            70, 6.0, 2.2, 6.0, 0.12);
        MythicParticleEffects.spawnMajor(world, ModParticles.SHOCKWAVE,
            targetPos.getX() + 0.5, targetPos.getY() + 0.2, targetPos.getZ() + 0.5,
            18, 4.0, 0.1, 4.0, 0.02);

        world.playSound(null, targetPos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.8f, 0.7f);
        world.playSound(null, targetPos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 1.5f, 0.8f);
        sendMessage(caster, "mythic_age.spellbook.message.tidal_surge.success");
        return true;
    }
}
