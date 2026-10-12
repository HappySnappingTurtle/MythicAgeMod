package com.example.mythic_age.item.spell.atlantean;

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

public class AbyssalCurrentSpell extends AbstractSpell {
    public AbyssalCurrentSpell() { super(SpellType.ABYSSAL_CURRENT, MythologyType.ATLANTEAN, SpellCategory.UTILITY, 120); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        BlockPos origin = caster.getBlockPos();
        BlockPos safeTarget = targetPos;
        int attempts = 0;
        while (attempts < 8 && safeTarget.getY() > world.getBottomY()
                && !world.getBlockState(safeTarget.down()).isSolidBlock(world, safeTarget.down())) {
            safeTarget = safeTarget.down();
            attempts++;
        }
        if (!world.getBlockState(safeTarget).isAir()) {
            safeTarget = safeTarget.up();
        }

        MythicParticleEffects.spawnMajor(world, ParticleTypes.BUBBLE_POP,
            origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
            45, 1.5, 1.4, 1.5, 0.18);
        caster.teleport(safeTarget.getX() + 0.5, safeTarget.getY(), safeTarget.getZ() + 0.5);
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 600, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 900, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 360, 1));

        MythicParticleEffects.spawnMajor(world, ParticleTypes.SPLASH,
            safeTarget.getX() + 0.5, safeTarget.getY() + 1.0, safeTarget.getZ() + 0.5,
            55, 1.8, 1.5, 1.8, 0.2);
        MythicParticleEffects.spawnNormal(world, ParticleTypes.NAUTILUS,
            safeTarget.getX() + 0.5, safeTarget.getY() + 1.2, safeTarget.getZ() + 0.5,
            28, 1.3, 1.0, 1.3, 0.05);
        world.playSound(null, safeTarget, SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.5f, 0.95f);
        sendMessage(caster, "mythic_age.spellbook.message.abyssal_current.success");
        return true;
    }
}
