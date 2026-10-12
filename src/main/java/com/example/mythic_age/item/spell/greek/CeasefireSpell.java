package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class CeasefireSpell extends AbstractSpell {
    public CeasefireSpell() { super(SpellType.CEASEFIRE, MythologyType.GREEK, SpellCategory.UTILITY, 500); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        world.getEntitiesByClass(HostileEntity.class, caster.getBoundingBox().expand(50.0), mob -> true)
            .forEach(mob -> {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1200, 3));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 1200, 2));
                mob.setTarget(null);
            });
        MythicParticleEffects.spawnNormal(world, ParticleTypes.HEART,
            caster.getX(), caster.getY() + 2, caster.getZ(), 30, 3.0, 1.0, 3.0, 0.1);
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.NEUTRAL, 1.0f, 1.0f);
        return true;
    }
}
