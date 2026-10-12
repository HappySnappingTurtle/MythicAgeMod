package com.example.mythic_age.item.spell.nordic;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

public class FrostSpell extends AbstractSpell {
    public FrostSpell() { super(SpellType.FROST, MythologyType.NORDIC, SpellCategory.OFFENSIVE, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Create frost effects in target area
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = -1; y <= 2; y++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // Turn water into ice
                    if (state.getBlock() == Blocks.WATER) {
                        world.setBlockState(pos, Blocks.ICE.getDefaultState());
                    }
                    // Place snow on ground
                    else if (y == 0 && state.isAir() &&
                            world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        world.setBlockState(pos, Blocks.SNOW.getDefaultState());
                    }
                }
            }
        }

        // Freeze nearby hostile mobs
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(10), entity -> entity.isAlive());

        for (HostileEntity enemy : enemies) {
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 4));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 2));
            enemy.damage(world.getDamageSources().freeze(), 6.0f);
        }

        // Grant player frost resistance
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));

        MythicParticleEffects.spawnMajor(world, ParticleTypes.SNOWFLAKE,
            targetPos.getX(), targetPos.getY() + 2, targetPos.getZ(),
            100, 8.0, 3.0, 8.0, 0.1);

        world.playSound(null, targetPos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.5f, 0.8f);
        sendMessage(caster, "mythic_age.spellbook.message.frost.success");

        return true;
    }
}
