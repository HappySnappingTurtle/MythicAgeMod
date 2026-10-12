package com.example.mythic_age.item.spell.nordic;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.util.GodPowerWeatherManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.LivingEntity;
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

public class FimbulwinterSpell extends AbstractSpell {
    public FimbulwinterSpell() { super(SpellType.FIMBULWINTER, MythologyType.NORDIC, SpellCategory.OFFENSIVE, 600); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        String fimbulwinterId = "fimbulwinter_" + caster.getUuid().toString() + "_" + world.getTime();
        GodPowerWeatherManager.getInstance().startFimbulwinter(world, targetPos, fimbulwinterId);

        // Create ice and snow effects in a large area
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                for (int y = -2; y <= 5; y++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // Turn water into ice
                    if (state.getBlock() == Blocks.WATER) {
                        world.setBlockState(pos, Blocks.ICE.getDefaultState());
                    }
                    // Place snow on ground and plants
                    else if (y >= 0 && state.isAir() &&
                            world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        if (world.random.nextFloat() < 0.7f) {
                            world.setBlockState(pos, Blocks.SNOW.getDefaultState());
                        }
                    }
                    // Freeze plants
                    else if (state.getBlock() instanceof PlantBlock) {
                        if (world.random.nextFloat() < 0.3f) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
        }

        // Deal frost damage to all living entities (except the caster)
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(20), entity -> entity.isAlive() && entity != caster);

        for (LivingEntity entity : entities) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1200, 3));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 1200, 2));
            entity.damage(world.getDamageSources().freeze(), 8.0f);

            if (entity instanceof HostileEntity) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 1200, 2));
            }
        }

        // Grant player extreme cold resistance and power
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 1));

        // Massive snowflake particle effects
        MythicParticleEffects.spawnUltimate(world, ParticleTypes.SNOWFLAKE,
            targetPos.getX(), targetPos.getY() + 5, targetPos.getZ(),
            200, 20.0, 8.0, 20.0, 0.2);

        world.playSound(null, targetPos, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.WEATHER, 2.0f, 0.3f);
        sendMessage(caster, "mythic_age.spellbook.message.fimbulwinter.success");

        return true;
    }
}
