package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class UnderworldPassageSpell extends AbstractSpell {
    public UnderworldPassageSpell() { super(SpellType.UNDERWORLD_PASSAGE, MythologyType.GREEK, SpellCategory.TRANSPORT, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        BlockPos tp = targetPos;
        while (tp.getY() > world.getBottomY() && !world.getBlockState(tp.down()).isSolidBlock(world, tp.down())) {
            tp = tp.down();
        }
        double prevX = caster.getX();
        double prevY = caster.getY();
        double prevZ = caster.getZ();
        caster.teleport(tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5);
        MythicParticleEffects.spawnMajor(world, ParticleTypes.PORTAL, prevX, prevY + 1, prevZ, 30, 1.0, 1.0, 1.0, 0.5);
        MythicParticleEffects.spawnMajor(world, ParticleTypes.PORTAL,
            tp.getX() + 0.5, tp.getY() + 1, tp.getZ() + 0.5, 30, 1.0, 1.0, 1.0, 0.5);
        world.playSound(null, tp, SoundEvents.BLOCK_PORTAL_TRAVEL, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
