package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class BronzeSpell extends AbstractSpell {
    public BronzeSpell() { super(SpellType.BRONZE, MythologyType.GREEK, SpellCategory.BUFF, 200); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1800, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1800, 0));
        world.getEntitiesByClass(PlayerEntity.class, caster.getBoundingBox().expand(15.0), p -> p != caster)
            .forEach(p -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1800, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1800, 0));
            });
        MythicParticleEffects.spawnNormal(world, ParticleTypes.CRIT,
            caster.getX(), caster.getY() + 1, caster.getZ(), 25, 1.0, 1.0, 1.0, 0.2);
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.8f, 1.2f);
        return true;
    }
}
