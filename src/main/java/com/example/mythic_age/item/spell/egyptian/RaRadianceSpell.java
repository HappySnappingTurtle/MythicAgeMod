package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class RaRadianceSpell extends AbstractSpell {
    public RaRadianceSpell() { super(SpellType.RA_RADIANCE, MythologyType.EGYPTIAN, SpellCategory.OFFENSIVE, 120); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Heal allies and cleanse negative effects
        List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(10),
            entity -> entity.isAlive() && !(entity instanceof HostileEntity));

        for (LivingEntity ally : allies) {
            ally.heal(8.0f);
            // Remove negative status effects
            ally.removeStatusEffect(StatusEffects.POISON);
            ally.removeStatusEffect(StatusEffects.WITHER);
            ally.removeStatusEffect(StatusEffects.WEAKNESS);
            ally.removeStatusEffect(StatusEffects.SLOWNESS);

            // Grant sun blessing
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 3000, 1));
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));
        }

        // Damage undead creatures
        List<LivingEntity> undead = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(15),
            entity -> entity.isUndead() && entity.isAlive());

        for (LivingEntity undeadEntity : undead) {
            undeadEntity.damage(world.getDamageSources().magic(), 12.0f);
            undeadEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 6000, 2));
        }

        // Place light sources around the area
        for (int i = 0; i < 8; i++) {
            BlockPos lightPos = targetPos.add(
                world.random.nextInt(11) - 5,
                world.random.nextInt(3),
                world.random.nextInt(11) - 5
            );

            if (world.getBlockState(lightPos).isAir()) {
                world.setBlockState(lightPos, Blocks.GLOWSTONE.getDefaultState());
            }
        }

        // Golden radiance particle effect
        for (int i = 0; i < 60; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 20;
            double y = targetPos.getY() + world.random.nextDouble() * 8;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 20;
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.END_ROD, x, y, z, 1, 0, 0.1, 0, 0.1);
        }

        sendMessage(caster, "mythic_age.spellbook.message.ra_radiance.success");
        return true;
    }
}
