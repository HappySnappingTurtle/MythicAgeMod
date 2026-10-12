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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PestilenceSpell extends AbstractSpell {
    public PestilenceSpell() { super(SpellType.PESTILENCE, MythologyType.GREEK, SpellCategory.DEBUFF, 160); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        world.getEntitiesByClass(HostileEntity.class,
            Box.of(Vec3d.ofCenter(targetPos), 20, 20, 20), mob -> true)
            .forEach(mob -> {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 1));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 1));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 0));
            });
        MythicParticleEffects.spawnNormal(world, ParticleTypes.SQUID_INK,
            targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5, 50, 3.0, 2.0, 3.0, 0.1);
        world.playSound(null, targetPos, SoundEvents.ENTITY_WITCH_AMBIENT, SoundCategory.HOSTILE, 1.0f, 0.6f);
        return true;
    }
}
