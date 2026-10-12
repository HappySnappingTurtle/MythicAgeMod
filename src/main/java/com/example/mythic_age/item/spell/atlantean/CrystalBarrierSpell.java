package com.example.mythic_age.item.spell.atlantean;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.particle.ModParticles;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class CrystalBarrierSpell extends AbstractSpell {
    public CrystalBarrierSpell() { super(SpellType.CRYSTAL_BARRIER, MythologyType.ATLANTEAN, SpellCategory.UTILITY, 160); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        BlockPos center = caster.getBlockPos();
        List<PlayerEntity> allies = world.getEntitiesByClass(PlayerEntity.class,
            new Box(center).expand(10.0), entity -> entity.isAlive());

        for (PlayerEntity ally : allies) {
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 1));
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1600, 1));
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 2400, 0));
        }

        MythicParticleEffects.spawnMajor(world, ModParticles.DIVINE_SPARKLE,
            center.getX() + 0.5, center.getY() + 1.2, center.getZ() + 0.5,
            80, 4.5, 2.2, 4.5, 0.08);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.END_ROD,
            center.getX() + 0.5, center.getY() + 1.4, center.getZ() + 0.5,
            60, 3.5, 1.6, 3.5, 0.06);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.ENCHANT,
            center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
            70, 4.5, 1.0, 4.5, 0.04);

        world.playSound(null, center, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5f, 0.8f);
        world.playSound(null, center, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.9f, 1.35f);
        sendMessage(caster, "mythic_age.spellbook.message.crystal_barrier.success");
        return true;
    }
}
