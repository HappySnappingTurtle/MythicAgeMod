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

public class DesertStormSpell extends AbstractSpell {
    public DesertStormSpell() { super(SpellType.DESERT_STORM, MythologyType.EGYPTIAN, SpellCategory.OFFENSIVE, 160); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Create sandstorm effect in target area
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                BlockPos pos = targetPos.add(x, 0, z);

                // Randomly place sand
                if (world.getBlockState(pos).isAir() && world.random.nextFloat() < 0.3f) {
                    world.setBlockState(pos, Blocks.SAND.getDefaultState());
                }
            }
        }

        // Apply confusion effects to hostile mobs
        List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(8),
            entity -> entity instanceof HostileEntity && entity.isAlive());

        for (LivingEntity enemy : enemies) {
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 4000, 0));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 4000, 1));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 4000, 0));
            enemy.damage(world.getDamageSources().magic(), 3.0f);
        }

        // Grant desert adaptation to caster
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));

        // Sandstorm particle effect
        for (int i = 0; i < 80; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 24;
            double y = targetPos.getY() + world.random.nextDouble() * 6;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 24;
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.SMOKE, x, y, z, 1, 0, 0.2, 0, 0.1);
        }

        sendMessage(caster, "mythic_age.spellbook.message.desert_storm.success");
        return true;
    }
}
