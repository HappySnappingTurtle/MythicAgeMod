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

import java.util.List;

public class ChronoStasisSpell extends AbstractSpell {
    public ChronoStasisSpell() { super(SpellType.CHRONO_STASIS, MythologyType.ATLANTEAN, SpellCategory.UTILITY, 400); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(20.0, 8.0, 20.0), entity -> entity.isAlive());
        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().magic(), 7.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 700, 5));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 700, 3));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 700, 2));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 500, 0));
            enemy.setVelocity(enemy.getVelocity().multiply(0.05D, 0.0D, 0.05D));
            enemy.velocityModified = true;
        }
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1600, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1600, 0));

        MythicParticleEffects.spawnUltimate(world, ModParticles.DIVINE_SPARKLE,
            targetPos.getX() + 0.5, targetPos.getY() + 1.8, targetPos.getZ() + 0.5,
            150, 9.0, 3.5, 9.0, 0.08);
        MythicParticleEffects.spawnUltimate(world, ParticleTypes.END_ROD,
            targetPos.getX() + 0.5, targetPos.getY() + 1.4, targetPos.getZ() + 0.5,
            120, 8.5, 2.8, 8.5, 0.04);
        MythicParticleEffects.spawnMajor(world, ParticleTypes.ENCHANT,
            targetPos.getX() + 0.5, targetPos.getY() + 1.2, targetPos.getZ() + 0.5,
            130, 9.0, 2.5, 9.0, 0.05);
        world.playSound(null, targetPos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.5f, 0.55f);
        world.playSound(null, targetPos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.8f, 0.55f);
        sendMessage(caster, "mythic_age.spellbook.message.chrono_stasis.success");
        return true;
    }
}
